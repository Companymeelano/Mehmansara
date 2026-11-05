package ir.ahvaz.mehra.core.common

object Constants {
    const val API_PREFIX = "/api/v1"
    const val HUB_PATH = "/hubs/notifications"

    const val CONNECT_TIMEOUT_SECONDS = 20L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    const val HEADER_IDEMPOTENCY = "Idempotency-Key"
    const val HEADER_DEVICE_ID = "X-Device-Id"
    const val HEADER_CLIENT_VERSION = "X-Client-Version"
    const val HEADER_PLATFORM = "X-Platform"

    const val PAGINATION_PAGE_SIZE = 25

    /** Refresh the access token this many seconds before it actually expires. */
    const val TOKEN_REFRESH_SKEW_SECONDS = 60L

    const val DEFAULT_CURRENCY_UNIT = "ریال"
    const val DEFAULT_LOCK_TIMEOUT_MINUTES = 5

    /** Cache lifetime used to decide whether a cached payload may still be shown offline. */
    const val CACHE_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    const val SYNC_WORK_NAME = "mehra-outbox-sync"
    const val SYNC_NOTIFICATION_ID = 4201
    const val OPS_NOTIFICATION_CHANNEL = "mehra_ops"
    const val SYNC_NOTIFICATION_CHANNEL = "mehra_sync"
}
