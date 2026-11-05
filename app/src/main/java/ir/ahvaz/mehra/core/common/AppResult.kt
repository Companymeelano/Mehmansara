package ir.ahvaz.mehra.core.common

/**
 * Result of a repository operation.
 *
 * Failures are always typed ([AppError]) so the UI layer can distinguish a real server
 * rejection from a degraded-but-cached answer. Nothing is ever marked as successful
 * unless the backend acknowledged it — see [ErrorKind.QUEUED_OFFLINE].
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

val AppResult<*>.isSuccess: Boolean get() = this is AppResult.Success

fun <T> AppResult<T>.dataOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** Data plus its freshness metadata, so screens can honestly show "offline / last updated". */
data class CachedData<T>(
    val data: T,
    val updatedAtMillis: Long,
    val fromCache: Boolean,
)
