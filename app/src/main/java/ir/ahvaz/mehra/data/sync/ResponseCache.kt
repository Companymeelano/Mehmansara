package ir.ahvaz.mehra.data.sync

import com.google.gson.Gson
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.data.local.dao.ApiCacheDao
import ir.ahvaz.mehra.data.local.entity.ApiCacheEntity
import javax.inject.Inject
import javax.inject.Singleton

data class CachedPayload(val json: String, val updatedAtMillis: Long)

/**
 * Persists the last successful wire payload for a query key.
 * Used to keep the app usable on the internal network when the server is momentarily gone.
 */
@Singleton
class ResponseCache @Inject constructor(
    private val dao: ApiCacheDao,
    private val gson: Gson,
) {

    suspend fun put(key: String, payload: Any) {
        runCatching {
            dao.put(
                ApiCacheEntity(
                    cacheKey = key,
                    payloadJson = gson.toJson(payload),
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun get(key: String): CachedPayload? {
        val entity = runCatching { dao.get(key) }.getOrNull() ?: return null
        if (System.currentTimeMillis() - entity.updatedAtMillis > Constants.CACHE_MAX_AGE_MILLIS) return null
        return CachedPayload(entity.json = entity.payloadJson, updatedAtMillis = entity.updatedAtMillis)
    }

    suspend fun invalidate(key: String) {
        runCatching { dao.invalidate(key) }
    }

    suspend fun invalidatePrefix(prefix: String) {
        runCatching { dao.invalidatePrefix(prefix) }
    }

    suspend fun clearAll() {
        runCatching { dao.clear() }
    }

    suspend fun purgeExpired() {
        runCatching {
            dao.deleteOlderThan(System.currentTimeMillis() - Constants.CACHE_MAX_AGE_MILLIS)
        }
    }
}
