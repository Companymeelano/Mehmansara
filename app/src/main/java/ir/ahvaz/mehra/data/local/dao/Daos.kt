package ir.ahvaz.mehra.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.ahvaz.mehra.data.local.entity.ApiCacheEntity
import ir.ahvaz.mehra.data.local.entity.CachedNotificationEntity
import ir.ahvaz.mehra.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiCacheDao {

    @Query("SELECT * FROM api_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): ApiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: ApiCacheEntity)

    @Query("DELETE FROM api_cache WHERE cacheKey = :key")
    suspend fun invalidate(key: String)

    @Query("DELETE FROM api_cache WHERE cacheKey LIKE :prefix || '%'")
    suspend fun invalidatePrefix(prefix: String)

    @Query("DELETE FROM api_cache WHERE updatedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM api_cache")
    suspend fun clear()
}

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations WHERE state = 'PENDING' ORDER BY createdAtMillis ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE state = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun allPending(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations ORDER BY createdAtMillis DESC LIMIT 50")
    fun observeRecent(): Flow<List<PendingOperationEntity>>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE state = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query(
        "UPDATE pending_operations SET attemptCount = attemptCount + 1, " +
            "lastAttemptAtMillis = :nowMillis, lastError = :error WHERE id = :id"
    )
    suspend fun markAttempt(id: String, nowMillis: Long, error: String?)

    @Query("UPDATE pending_operations SET state = :state, lastError = :error WHERE id = :id")
    suspend fun setState(id: String, state: String, error: String?)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_operations WHERE state != 'PENDING'")
    suspend fun clearFinished()
}

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedNotificationEntity>)

    @Query("SELECT * FROM cached_notifications ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeAll(): Flow<List<CachedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM cached_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE cached_notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM cached_notifications")
    suspend fun clear()
}
