package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.ErrorKind
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import java.lang.reflect.Type

/**
 * Shared plumbing for caches and the offline outbox.
 *
 * [readThrough] implements the offline contract: a network failure falls back to the last
 * successful payload *and marks it as cached*, so the UI can show the staleness instead of
 * pretending everything is fine.
 */
abstract class BaseRepository(
    protected val executor: ApiExecutor,
    protected val cache: ResponseCache,
    protected val queue: OfflineQueue,
    protected val gson: Gson,
    protected val networkMonitor: NetworkMonitor,
) {

    protected suspend fun <T> readThrough(
        cacheKey: String,
        cacheType: Type,
        fetch: suspend () -> AppResult<T>,
    ): AppResult<CachedData<T>> {
        val result = fetch()
        return when (result) {
            is AppResult.Success -> {
                cache.put(cacheKey, result.data)
                AppResult.Success(
                    CachedData(
                        data = result.data,
                        updatedAtMillis = System.currentTimeMillis(),
                        fromCache = false,
                    )
                )
            }

            is AppResult.Failure -> {
                if (isOffline(result.error)) {
                    val cached = cache.get(cacheKey)
                    val parsed = cached?.let {
                        runCatching { gson.fromJson<T>(it.json, cacheType) }.getOrNull()
                    }
                    if (cached != null && parsed != null) {
                        return AppResult.Success(
                            CachedData(data = parsed, updatedAtMillis = cached.updatedAtMillis, fromCache = true)
                        )
                    }
                }
                result
            }
        }
    }

    /** Converts the cached wire model into the domain model without losing freshness info. */
    protected fun <D, T> AppResult<CachedData<D>>.mapCached(transform: (D) -> T): AppResult<CachedData<T>> =
        map { CachedData(data = transform(it.data), updatedAtMillis = it.updatedAtMillis, fromCache = it.fromCache) }

    protected fun isOffline(error: AppError): Boolean =
        error.kind == ErrorKind.NETWORK || error.kind == ErrorKind.TIMEOUT

    /**
     * Persists a sensitive mutation for later replay and reports
     * [ErrorKind.QUEUED_OFFLINE] — never a success.
     */
    protected suspend fun <T> queueForLater(
        error: AppError,
        operationType: String,
        method: String,
        path: String,
        body: Any?,
        summaryFa: String,
        idempotencyKey: String,
    ): AppResult<T> {
        if (!isOffline(error)) return AppResult.Failure(error)
        return runCatching {
            queue.enqueue(
                operationType = operationType,
                method = method,
                path = path,
                body = body,
                summaryFa = summaryFa,
                idempotencyKey = idempotencyKey,
            )
            AppResult.Failure(AppError.queuedOffline())
        }.getOrElse { AppResult.Failure(error) }
    }
}
