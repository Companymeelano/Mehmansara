package ir.ahvaz.mehra.core.common

/** Coarse classification of failures; drives UI messaging and retry policy. */
enum class ErrorKind {
    /** No connectivity / connection refused / DNS failure. */
    NETWORK,
    /** Socket or call timeout. */
    TIMEOUT,
    /** TLS handshake or certificate validation failed. */
    TLS,

    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    /** Business conflict, e.g. the room is already reserved for that range. */
    CONFLICT,
    /** Field level validation rejected by the server (HTTP 400/422). */
    VALIDATION,
    SERVER,
    /** Operation persisted locally and will be replayed once the server is reachable. */
    QUEUED_OFFLINE,
    UNKNOWN,
}

/**
 * A single, immutable description of a failed operation.
 *
 * [message] is the server-provided (Persian) message when available; otherwise the UI
 * falls back to [userMessageFa].
 */
data class AppError(
    val kind: ErrorKind,
    val message: String? = null,
    val code: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val httpStatus: Int? = null,
    val traceId: String? = null,
    val cause: Throwable? = null,
) {
    val userMessageFa: String
        get() = message?.takeIf { it.isNotBlank() } ?: defaultMessageFa(kind)

    /** True when retrying the very same request may succeed. */
    val isRetryable: Boolean
        get() = kind == ErrorKind.NETWORK || kind == ErrorKind.TIMEOUT || kind == ErrorKind.SERVER

    fun withFieldErrors(errors: Map<String, String>): AppError = copy(fieldErrors = errors)

    companion object {
        fun network(cause: Throwable? = null): AppError =
            AppError(ErrorKind.NETWORK, cause = cause)

        fun timeout(cause: Throwable? = null): AppError =
            AppError(ErrorKind.TIMEOUT, cause = cause)

        fun tls(cause: Throwable? = null): AppError =
            AppError(ErrorKind.TLS, cause = cause)

        fun unauthorized(message: String? = null): AppError =
            AppError(ErrorKind.UNAUTHORIZED, message = message, httpStatus = 401)

        fun forbidden(message: String? = null): AppError =
            AppError(ErrorKind.FORBIDDEN, message = message, httpStatus = 403)

        fun server(message: String? = null, httpStatus: Int? = null): AppError =
            AppError(ErrorKind.SERVER, message = message, httpStatus = httpStatus)

        fun validation(message: String? = null, fields: Map<String, String> = emptyMap()): AppError =
            AppError(ErrorKind.VALIDATION, message = message, fieldErrors = fields, httpStatus = 422)

        fun conflict(message: String? = null, code: String? = null): AppError =
            AppError(ErrorKind.CONFLICT, message = message, code = code, httpStatus = 409)

        fun notFound(message: String? = null): AppError =
            AppError(ErrorKind.NOT_FOUND, message = message, httpStatus = 404)

        fun queuedOffline(): AppError = AppError(ErrorKind.QUEUED_OFFLINE)

        fun unknown(cause: Throwable? = null): AppError =
            AppError(ErrorKind.UNKNOWN, cause = cause)
    }
}

fun defaultMessageFa(kind: ErrorKind): String = when (kind) {
    ErrorKind.NETWORK -> "ارتباط با سرور برقرار نشد. اتصال شبکه داخلی مجموعه را بررسی کنید."
    ErrorKind.TIMEOUT -> "پاسخی از سرور دریافت نشد (اتمام زمان انتظار). دوباره تلاش کنید."
    ErrorKind.TLS -> "گواهی امنیتی سرور معتبر نیست. با مدیر شبکه هماهنگ کنید."
    ErrorKind.UNAUTHORIZED -> "نشست شما منقضی شده است. دوباره وارد شوید."
    ErrorKind.FORBIDDEN -> "برای این عملیات دسترسی ندارید."
    ErrorKind.NOT_FOUND -> "مورد درخواستی یافت نشد."
    ErrorKind.CONFLICT -> "این عملیات با وضعیت فعلی سیستم تعارض دارد."
    ErrorKind.VALIDATION -> "اطلاعات وارد شده معتبر نیست."
    ErrorKind.SERVER -> "خطای سرور. لطفاً بعداً دوباره تلاش کنید."
    ErrorKind.QUEUED_OFFLINE -> "اتصال قطع است؛ عملیات ذخیره شد و پس از برقراری اتصال به سرور ارسال می‌شود. تا تأیید سرور، این عملیات قطعی نیست."
    ErrorKind.UNKNOWN -> "خطای پیش‌بینی‌نشده رخ داد."
}
