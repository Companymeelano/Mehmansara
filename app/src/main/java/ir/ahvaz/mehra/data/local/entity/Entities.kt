package ir.ahvaz.mehra.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw JSON cache of the last successful server response for a given query key.
 *
 * Storing the wire payload (instead of a normalised relational schema) keeps the offline
 * behaviour honest: what the operator sees while offline is exactly what the server last
 * returned, including its timestamp.
 */
@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String,
    val updatedAtMillis: Long,
)

/**
 * Outbox entry for an operation that must reach the server exactly once.
 *
 * [idempotencyKey] is generated when the operator submits the form and replayed unchanged,
 * so a retry after a timeout can never duplicate a payment or a check-in.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val operationType: String,
    val method: String,
    val path: String,
    val bodyJson: String?,
    val idempotencyKey: String,
    val createdAtMillis: Long,
    val attemptCount: Int = 0,
    val lastAttemptAtMillis: Long? = null,
    val lastError: String? = null,
    val state: String = STATE_PENDING,
    val summaryFa: String = "",
) {
    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_REJECTED = "REJECTED"
        const val STATE_SENT = "SENT"
    }
}

@Entity(tableName = "cached_notifications")
data class CachedNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String?,
    val severity: String,
    val kind: String,
    val createdAtMillis: Long?,
    val isRead: Boolean,
)
