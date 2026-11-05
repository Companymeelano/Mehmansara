package ir.ahvaz.mehra.core.network

import com.google.gson.Gson
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.ErrorKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Single choke point where HTTP responses become [AppResult] values.
 *
 * Keeping the mapping in one place is what allows every screen to distinguish
 * "server rejected" from "no connectivity" without ad-hoc try/catch blocks.
 */
@Singleton
class ApiExecutor @Inject constructor(private val gson: Gson) {

    suspend fun <T : Any> value(call: suspend () -> Response<ApiEnvelope<T>>): AppResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (!response.isSuccessful) return@withContext AppResult.Failure(errorFrom(response))
                val body = response.body()
                    ?: return@withContext AppResult.Failure(
                        AppError.server("پاسخ سرور خالی بود.", response.code())
                    )
                if (body.success == false) {
                    return@withContext AppResult.Failure(mapEnvelopeError(body, response.code()))
                }
                val data = body.data
                    ?: return@withContext AppResult.Failure(
                        AppError.server("داده‌ای از سرور دریافت نشد.", response.code())
                    )
                AppResult.Success(data)
            } catch (t: Throwable) {
                AppResult.Failure(transportError(t))
            }
        }

    /** For endpoints that acknowledge with 200/204 and no meaningful payload. */
    suspend fun <T : Any> unit(call: suspend () -> Response<ApiEnvelope<T>>): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (!response.isSuccessful) return@withContext AppResult.Failure(errorFrom(response))
                val body = response.body()
                if (body != null && body.success == false) {
                    return@withContext AppResult.Failure(mapEnvelopeError(body, response.code()))
                }
                AppResult.Success(Unit)
            } catch (t: Throwable) {
                AppResult.Failure(transportError(t))
            }
        }

    fun transportError(t: Throwable): AppError = when (t) {
        is SocketTimeoutException -> AppError.timeout(t)
        is UnknownHostException -> AppError.network(t)
        is SSLException -> AppError.tls(t)
        is IOException -> AppError.network(t)
        else -> AppError.unknown(t)
    }

    private fun mapEnvelopeError(body: ApiEnvelope<*>, status: Int): AppError {
        val error = body.error
        val code = error?.code
        val message = error?.message
        val kind = when {
            status == 401 || code == ApiErrorCodes.TOKEN_EXPIRED || code == ApiErrorCodes.TOKEN_REVOKED ->
                ErrorKind.UNAUTHORIZED
            status == 403 || code == ApiErrorCodes.INSUFFICIENT_PERMISSION -> ErrorKind.FORBIDDEN
            status == 404 -> ErrorKind.NOT_FOUND
            status == 409 || (code != null && CONFLICT_CODES.contains(code)) -> ErrorKind.CONFLICT
            status == 400 || status == 422 -> ErrorKind.VALIDATION
            status == 429 -> ErrorKind.SERVER
            else -> ErrorKind.SERVER
        }
        return AppError(
            kind = kind,
            message = message,
            code = code,
            fieldErrors = error?.details.orEmpty(),
            httpStatus = status,
            traceId = body.traceId,
        )
    }

    private fun errorFrom(response: Response<*>): AppError {
        val status = response.code()
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        val parsed: ApiEnvelope<Any>? = raw?.takeIf { it.isNotBlank() }?.let {
            runCatching { gson.fromJson(it, ApiEnvelope::class.java) as ApiEnvelope<Any> }.getOrNull()
        }
        if (parsed != null) {
            // Prefer the envelope's own error object; fall back to the status code.
            return mapEnvelopeError(parsed, status)
        }
        val kind = when (status) {
            401 -> ErrorKind.UNAUTHORIZED
            403 -> ErrorKind.FORBIDDEN
            404 -> ErrorKind.NOT_FOUND
            409 -> ErrorKind.CONFLICT
            400, 422 -> ErrorKind.VALIDATION
            else -> ErrorKind.SERVER
        }
        return AppError(kind = kind, httpStatus = status)
    }

    private companion object {
        val CONFLICT_CODES = setOf(
            ApiErrorCodes.ROOM_ALREADY_RESERVED,
            ApiErrorCodes.ROOM_NOT_AVAILABLE,
            ApiErrorCodes.ROOM_NOT_FREE,
            ApiErrorCodes.DUPLICATE_OPERATION,
            ApiErrorCodes.NATIONAL_ID_EXISTS,
            ApiErrorCodes.INVALID_STATE,
            ApiErrorCodes.OPEN_BALANCE,
            ApiErrorCodes.CAPACITY_EXCEEDED,
        )
    }
}
