package ir.ahvaz.mehra.core.network

import com.google.gson.annotations.SerializedName

/**
 * Every MEHRA endpoint answers with this envelope (see docs/API_CONTRACT.md).
 *
 * Successful: { "success": true, "data": <payload>, "traceId": "..." }
 * Failed:     { "success": false, "error": { "code": "...", "message": "...", "details": {} } }
 */
data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: ApiErrorBody? = null,
    @SerializedName("traceId") val traceId: String? = null,
)

data class ApiErrorBody(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: Map<String, String>? = null,
)

data class PagedData<T>(
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null,
    @SerializedName("totalCount") val totalCount: Int? = null,
)

/** Well known business error codes returned by the backend. */
object ApiErrorCodes {
    const val ROOM_ALREADY_RESERVED = "ROOM_ALREADY_RESERVED"
    const val ROOM_NOT_AVAILABLE = "ROOM_NOT_AVAILABLE"
    const val ROOM_NOT_FREE = "ROOM_NOT_FREE"
    const val CAPACITY_EXCEEDED = "CAPACITY_EXCEEDED"
    const val INVALID_DATE_RANGE = "INVALID_DATE_RANGE"
    const val INVALID_STATE = "INVALID_STATE"
    const val DUPLICATE_OPERATION = "DUPLICATE_OPERATION"
    const val NATIONAL_ID_EXISTS = "NATIONAL_ID_EXISTS"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_DISABLED = "ACCOUNT_DISABLED"
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    const val TOKEN_REVOKED = "TOKEN_REVOKED"
    const val INSUFFICIENT_PERMISSION = "INSUFFICIENT_PERMISSION"
    const val OPEN_BALANCE = "OPEN_BALANCE"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val SERVER_ERROR = "SERVER_ERROR"
}
