package ir.ahvaz.mehra.core.network.realtime

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import ir.ahvaz.mehra.core.date.TehranTime
import ir.ahvaz.mehra.core.di.ApplicationScope
import ir.ahvaz.mehra.core.di.RealtimeClient
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.domain.model.ActivityKind
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import ir.ahvaz.mehra.domain.model.RealtimeStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

/**
 * Minimal ASP.NET Core SignalR client speaking the JSON hub protocol over an OkHttp
 * WebSocket.
 *
 * Why not the official Java client? It pulls a reactive-streams stack that is awkward on
 * Android and it does not support reconnecting with our own retry/token policy. The hub
 * protocol we need is small and documented:
 *
 *   negotiate  POST {root}/hubs/notifications/negotiate?negotiateVersion=1
 *   connect    wss://{root}/hubs/notifications?id={connectionToken}   (Authorization header)
 *   handshake  {"protocol":"json","version":1}\u001E   -> {"}\u001E (or {"error":...})
 *   frame      {"type":1,"target":"paymentReceived","arguments":[{...}]}\u001E
 *   ping       {"type":6}\u001E      close {"type":7,...}\u001E
 *
 * Every frame is terminated by the record separator \u001E.
 */
@Singleton
class SignalRClient @Inject constructor(
    @RealtimeClient private val client: OkHttpClient,
    private val gson: Gson,
    private val serverUrlProvider: ServerUrlProvider,
    private val sessionManager: SessionManager,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private val _status = MutableStateFlow(RealtimeStatus())
    val status: StateFlow<RealtimeStatus> = _status.asStateFlow()

    private var connectionJob: Job? = null
    private var pingJob: Job? = null
    private var socket: WebSocket? = null
    private val handshakeCompleted = AtomicBoolean(false)

    @Volatile
    private var closedByUser = false

    fun start() {
        if (connectionJob?.isActive == true) return
        if (serverUrlProvider.current().isBlank() || !sessionManager.isLoggedIn) return
        closedByUser = false
        connectionJob = appScope.launch { connectionLoop() }
    }

    fun stop() {
        closedByUser = true
        connectionJob?.cancel()
        connectionJob = null
        pingJob?.cancel()
        pingJob = null
        socket?.close(1000, "client closing")
        socket = null
        _status.value = RealtimeStatus(isConnected = false, lastEventAt = _status.value.lastEventAt)
    }

    /** Forces an immediate reconnect (used by the "reconnect" button and after login). */
    fun restart() {
        stop()
        start()
    }

    private suspend fun connectionLoop() {
        var attempt = 0
        while (currentCoroutineContext().isActive && !closedByUser) {
            if (serverUrlProvider.current().isBlank() || !sessionManager.isLoggedIn) return
            _status.value = RealtimeStatus(
                isConnected = false,
                lastEventAt = _status.value.lastEventAt,
                lastError = null,
                reconnectAttempt = attempt,
            )
            _events.tryEmit(RealtimeEvent.Connecting(attempt))
            val connected = openConnection { reason ->
                _status.value = _status.value.copy(isConnected = false, lastError = reason)
                _events.tryEmit(RealtimeEvent.Disconnected(reason))
            }
            if (closedByUser) return
            attempt = if (connected) 1 else min(attempt + 1, 6)
            val backoff = backoffMillis(attempt)
            delay(backoff)
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        val base = min(30_000L, 1_000L * (1 shl min(attempt, 5)))
        return base + Random.nextLong(0, 750)
    }

    /**
     * Opens a single connection and suspends? No — it returns immediately after starting
     * the socket, and the caller waits on [waitForClose]. Kept simple: we block the loop
     * until the socket closes by polling the status.
     */
    private suspend fun openConnection(onClosed: (String?) -> Unit): Boolean {
        val connectionToken = negotiate() ?: run {
            _events.tryEmit(RealtimeEvent.Disconnected("مذاکره با سرور ناموفق بود"))
            return false
        }
        val wsUrl = buildWebSocketUrl(connectionToken) ?: return false

        val request = Request.Builder()
            .url(wsUrl)
            .header("Accept", "text/plain")
            .apply {
                sessionManager.accessToken?.let { header("Authorization", "Bearer $it") }
            }
            .build()

        handshakeCompleted.set(false)
        val handshake = CompletableDeferred<Boolean>()
        var connectedSuccessfully = false

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
                webSocket.send(HANDSHAKE_FRAME)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                text.split(RECORD_SEPARATOR).filter { it.isNotBlank() }.forEach { frame ->
                    handleFrame(frame) {
                        connectedSuccessfully = true
                        handshake.complete(true)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                onClosed(t.message ?: "خطای شبکه")
                handshake.complete(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                onClosed(reason.ifBlank { "اتصال بسته شد" })
                handshake.complete(false)
            }
        }

        client.newWebSocket(request, listener)
        // Suspend (never block the dispatcher) until the hub handshake completes or fails.
        withTimeoutOrNull(HANDSHAKE_TIMEOUT_MILLIS) { handshake.await() }

        if (!connectedSuccessfully) {
            socket?.cancel()
            socket = null
            return false
        }

        val now = TehranTime.nowInstant()
        _status.value = RealtimeStatus(
            isConnected = true,
            lastEventAt = now,
            lastError = null,
            reconnectAttempt = 0,
        )
        _events.tryEmit(RealtimeEvent.Connected(now))
        startPinger()

        // Suspend until the socket dies so the retry loop can pace reconnects.
        while (currentCoroutineContext().isActive && socket != null && !closedByUser) {
            delay(1_000)
        }
        pingJob?.cancel()
        return true
    }

    private fun startPinger() {
        pingJob?.cancel()
        pingJob = appScope.launch {
            while (currentCoroutineContext().isActive) {
                delay(PING_INTERVAL_MILLIS)
                val ws = socket ?: return@launch
                if (!ws.send(PING_FRAME)) return@launch
            }
        }
    }

    private fun handleFrame(frame: String, onHandshake: () -> Unit) {
        if (!handshakeCompleted.get()) {
            handshakeCompleted.set(true)
            if (frame.contains("\"error\"")) {
                _status.value = _status.value.copy(lastError = "اعتبارسنجی اتصال بلادرنگ رد شد")
                socket?.cancel()
                return
            }
            onHandshake()
            return
        }
        val element = runCatching { JsonParser.parseString(frame) }.getOrNull() ?: return
        if (!element.isJsonObject) return
        val obj = element.asJsonObject
        val type = obj.get("type")?.takeIf { it.isJsonPrimitive }?.asInt ?: return
        when (type) {
            TYPE_INVOCATION -> {
                val target = obj.get("target")?.takeIf { it.isJsonPrimitive }?.asString ?: return
                val arguments = obj.getAsJsonArray("arguments")
                val payload = arguments?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                val title = payload?.get("title")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: defaultTitleFa(target)
                val body = payload?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: payload?.get("body")?.takeIf { it.isJsonPrimitive }?.asString
                val event = RealtimeEvent.Received(
                    target = target,
                    title = title,
                    body = body,
                    kind = kindForTarget(target),
                    receivedAt = TehranTime.nowInstant(),
                    payloadJson = payload?.toString(),
                )
                _status.value = _status.value.copy(lastEventAt = event.receivedAt)
                _events.tryEmit(event)
            }

            TYPE_CLOSE -> {
                _events.tryEmit(RealtimeEvent.Disconnected("سرور اتصال را بست"))
                socket?.cancel()
            }

            TYPE_PING -> Unit
            else -> Unit
        }
    }

    private fun negotiate(): String? {
        val url = serverUrlProvider.absoluteUrl("$HUB_PATH/negotiate?negotiateVersion=1") ?: return null
        val request = Request.Builder()
            .url(url)
            .post(EMPTY_BODY)
            .apply { sessionManager.accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string() ?: return null
                val dto = runCatching { gson.fromJson(raw, NegotiateResponse::class.java) }.getOrNull()
                dto?.connectionToken ?: dto?.connectionId
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildWebSocketUrl(connectionToken: String): String? {
        val base = serverUrlProvider.current().trimEnd('/')
        if (base.isBlank()) return null
        val wsBase = when {
            base.startsWith("https://", ignoreCase = true) -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://", ignoreCase = true) -> "ws://" + base.removePrefix("http://")
            else -> "wss://$base"
        }
        return "$wsBase$HUB_PATH?id=${java.net.URLEncoder.encode(connectionToken, "UTF-8")}"
    }

    private fun defaultTitleFa(target: String): String = when (target) {
        "reservationCreated" -> "رزرو جدید ثبت شد"
        "reservationCancelled" -> "یک رزرو لغو شد"
        "guestCheckedIn" -> "پذیرش مهمان انجام شد"
        "guestCheckedOut" -> "خروج مهمان ثبت شد"
        "roomStatusChanged" -> "وضعیت اتاق تغییر کرد"
        "paymentReceived" -> "پرداخت جدید ثبت شد"
        "maintenanceRaised" -> "درخواست تعمیرات ثبت شد"
        "housekeepingTaskUpdated" -> "وضعیت کار نظافت تغییر کرد"
        "adminNotice" -> "اطلاعیه مدیریت"
        else -> "رویداد جدید"
    }

    private fun kindForTarget(target: String): ActivityKind = when (target) {
        "reservationCreated", "reservationCancelled" -> ActivityKind.RESERVATION
        "guestCheckedIn" -> ActivityKind.CHECK_IN
        "guestCheckedOut" -> ActivityKind.CHECK_OUT
        "roomStatusChanged" -> ActivityKind.ROOM_STATUS
        "paymentReceived" -> ActivityKind.PAYMENT
        "maintenanceRaised" -> ActivityKind.MAINTENANCE
        "housekeepingTaskUpdated" -> ActivityKind.HOUSEKEEPING
        else -> ActivityKind.SYSTEM
    }

    private data class NegotiateResponse(
        @SerializedName("connectionId") val connectionId: String? = null,
        @SerializedName("connectionToken") val connectionToken: String? = null,
        @SerializedName("negotiateVersion") val negotiateVersion: Int? = null,
    )

    private companion object {
        const val HUB_PATH = "/hubs/notifications"
        const val RECORD_SEPARATOR = "\u001E"
        const val HANDSHAKE_FRAME = "{\"protocol\":\"json\",\"version\":1}\u001E"
        const val PING_FRAME = "{\"type\":6}\u001E"
        const val PING_INTERVAL_MILLIS = 15_000L
        const val TYPE_INVOCATION = 1
        const val TYPE_CLOSE = 7
        const val TYPE_PING = 6
        val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        const val HANDSHAKE_TIMEOUT_MILLIS = 20_000L
    }
}
