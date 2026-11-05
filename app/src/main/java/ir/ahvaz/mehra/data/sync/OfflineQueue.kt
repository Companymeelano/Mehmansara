package ir.ahvaz.mehra.data.sync

import com.google.gson.Gson
import ir.ahvaz.mehra.data.local.dao.PendingOperationDao
import ir.ahvaz.mehra.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Kinds of operations that may be queued while the server is unreachable. */
object OperationType {
    const val CREATE_RESERVATION = "CREATE_RESERVATION"
    const val CHECK_IN = "CHECK_IN"
    const val CHECK_OUT = "CHECK_OUT"
    const val FINANCE_TRANSACTION = "FINANCE_TRANSACTION"
    const val DISCOUNT = "DISCOUNT"
    const val MAINTENANCE_CREATE = "MAINTENANCE_CREATE"
    const val GUEST_CREATE = "GUEST_CREATE"
}

/**
 * Durable outbox.
 *
 * A queued operation is *not* a success: the UI must keep showing it as "awaiting server
 * confirmation" until [ir.ahvaz.mehra.data.sync.OutboxProcessor] gets a 2xx answer. The same
 * Idempotency-Key is reused on every attempt, so replay can never create duplicates.
 */
@Singleton
class OfflineQueue @Inject constructor(
    private val dao: PendingOperationDao,
    private val gson: Gson,
) {

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    fun observeRecent(): Flow<List<PendingOperationEntity>> = dao.observeRecent()

    suspend fun pendingCount(): Int = runCatching { dao.pendingCount() }.getOrDefault(0)

    suspend fun pending(limit: Int = 20): List<PendingOperationEntity> =
        runCatching { dao.pending(limit) }.getOrDefault(emptyList())

    /**
     * Stores an operation for later replay.
     * @param idempotencyKey the key already used for the online attempt (or a fresh one).
     */
    suspend fun enqueue(
        operationType: String,
        method: String,
        path: String,
        body: Any?,
        summaryFa: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): PendingOperationEntity {
        val entity = PendingOperationEntity(
            id = UUID.randomUUID().toString(),
            operationType = operationType,
            method = method,
            path = path,
            bodyJson = body?.let { runCatching { gson.toJson(it) }.getOrNull() },
            idempotencyKey = idempotencyKey,
            createdAtMillis = System.currentTimeMillis(),
            summaryFa = summaryFa,
        )
        dao.insert(entity)
        return entity
    }

    suspend fun markAttempt(id: String, error: String?) {
        runCatching { dao.markAttempt(id, System.currentTimeMillis(), error) }
    }

    suspend fun markSent(id: String) {
        runCatching { dao.delete(id) }
    }

    suspend fun markRejected(id: String, error: String?) {
        runCatching { dao.setState(id, PendingOperationEntity.STATE_REJECTED, error) }
    }

    suspend fun clearFinished() {
        runCatching { dao.clearFinished() }
    }
}
