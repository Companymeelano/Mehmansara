package ir.ahvaz.mehra.domain.model

import java.time.Instant

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String? = null,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val createdAt: Instant? = null,
    val isRead: Boolean = false,
    val kind: ActivityKind = ActivityKind.SYSTEM,
)

/** Signals pushed by the backend over SignalR. */
sealed interface RealtimeEvent {
    data class Connected(val at: Instant) : RealtimeEvent
    data class Disconnected(val reason: String?) : RealtimeEvent
    data class Connecting(val attempt: Int) : RealtimeEvent

    /**
     * A hub invocation was received. Raw payload is kept so the UI can show whatever the
     * server sends without the client inventing fields.
     */
    data class Received(
        val target: String,
        val title: String,
        val body: String?,
        val kind: ActivityKind,
        val receivedAt: Instant,
        val payloadJson: String? = null,
    ) : RealtimeEvent
}

data class RealtimeStatus(
    val isConnected: Boolean = false,
    val lastEventAt: Instant? = null,
    val lastError: String? = null,
    val reconnectAttempt: Int = 0,
)
