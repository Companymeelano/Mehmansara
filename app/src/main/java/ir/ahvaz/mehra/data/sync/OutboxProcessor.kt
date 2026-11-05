package ir.ahvaz.mehra.data.sync

import ir.ahvaz.mehra.core.di.ApiClient
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class OutboxResult(
    val sent: Int,
    val rejected: Int,
    val remaining: Int,
)

/**
 * Replays queued operations in FIFO order with their original idempotency keys.
 *
 * Replay stops at the first transport error so ordering between operations on the same
 * entity (check-in before payment) is preserved.
 */
@Singleton
class OutboxProcessor @Inject constructor(
    private val queue: OfflineQueue,
    @ApiClient private val client: OkHttpClient,
    private val serverUrlProvider: ServerUrlProvider,
    private val sessionManager: SessionManager,
) {

    suspend fun process(limit: Int = 20): OutboxResult = withContext(Dispatchers.IO) {
        val pending = queue.pending(limit)
        if (pending.isEmpty()) {
            return@withContext OutboxResult(sent = 0, rejected = 0, remaining = 0)
        }
        val token = sessionManager.accessToken
        if (token.isNullOrBlank()) {
            return@withContext OutboxResult(sent = 0, rejected = 0, remaining = queue.pendingCount())
        }

        var sent = 0
        var rejected = 0
        for (operation in pending) {
            val url = serverUrlProvider.absoluteUrl(operation.path)
            if (url == null) break
            val body = (operation.bodyJson ?: "").toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .method(operation.method, if (operation.method == "GET") null else body)
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", operation.idempotencyKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            queue.markSent(operation.id)
                            sent += 1
                        }

                        response.code in 400..499 && response.code != 408 && response.code != 429 -> {
                            // Permanent rejection: surface it instead of retrying forever.
                            val message = runCatching { response.body?.string() }.getOrNull()
                            queue.markRejected(operation.id, message ?: "HTTP ${response.code}")
                            rejected += 1
                        }

                        else -> {
                            queue.markAttempt(operation.id, "HTTP ${response.code}")
                            return@withContext OutboxResult(sent, rejected, queue.pendingCount())
                        }
                    }
                }
            } catch (e: IOException) {
                queue.markAttempt(operation.id, e.message)
                return@withContext OutboxResult(sent, rejected, queue.pendingCount())
            }
        }
        OutboxResult(sent = sent, rejected = rejected, remaining = queue.pendingCount())
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
