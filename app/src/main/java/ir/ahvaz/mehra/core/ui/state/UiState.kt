package ir.ahvaz.mehra.core.ui.state

import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.ErrorKind
import ir.ahvaz.mehra.core.common.dataOrNull
import ir.ahvaz.mehra.core.common.errorOrNull

/**
 * One immutable state type for every screen.
 *
 * [fromCache] and [updatedAtMillis] exist so a screen can always tell the operator how old
 * the information on screen is — the app never pretends cached data is live.
 */
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val updatedAtMillis: Long? = null,
    val fromCache: Boolean = false,
    val pendingOperations: Int = 0,
    val actionInProgress: Boolean = false,
) {
    val isOffline: Boolean
        get() = fromCache || error?.kind == ErrorKind.NETWORK || error?.kind == ErrorKind.TIMEOUT

    val isEmpty: Boolean
        get() = data == null && error == null && !isLoading

    fun applyResult(result: AppResult<CachedData<T>>): UiState<T> = when (result) {
        is AppResult.Success -> copy(
            data = result.data.data,
            isLoading = false,
            isRefreshing = false,
            error = null,
            updatedAtMillis = result.data.updatedAtMillis,
            fromCache = result.data.fromCache,
        )

        is AppResult.Failure -> copy(
            isLoading = false,
            isRefreshing = false,
            // Keep whatever was already displayed; the strip shows staleness.
            error = result.error,
        )
    }

    fun applyValue(result: AppResult<T>): UiState<T> = when (result) {
        is AppResult.Success -> copy(
            data = result.data,
            isLoading = false,
            isRefreshing = false,
            error = null,
            updatedAtMillis = System.currentTimeMillis(),
            fromCache = false,
        )

        is AppResult.Failure -> copy(isLoading = false, isRefreshing = false, error = result.error)
    }

    companion object {
        fun <T> loading(): UiState<T> = UiState(isLoading = true)

        fun <T> idle(): UiState<T> = UiState()
    }
}

/** Small helpers used by the view models. */
inline fun <T> AppResult<T>.stateFrom(
    current: UiState<T>,
): UiState<T> = when (this) {
    is AppResult.Success -> current.copy(data = data, error = null, isLoading = false, isRefreshing = false)
    is AppResult.Failure -> current.copy(error = error, isLoading = false, isRefreshing = false)
}

fun <T> AppResult<T>.valueOrNull(): T? = dataOrNull()

fun <T> AppResult<T>.errorOrNullSafe(): AppError? = errorOrNull()
