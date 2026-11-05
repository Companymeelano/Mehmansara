package ir.ahvaz.mehra.core.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.di.RefreshClient
import ir.ahvaz.mehra.core.security.AuthTokens
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes the access token exactly once even when several parallel requests hit a 401.
 *
 * Refresh tokens are rotated by the backend: the response always carries a new pair, and a
 * revoked/expired refresh token ends the session ([SessionEvent.Expired]) instead of
 * silently looping.
 */
@Singleton
class TokenRefreshCoordinator @Inject constructor(
    @RefreshClient private val client: OkHttpClient,
    private val gson: Gson,
    private val sessionManager: SessionManager,
    private val serverUrlProvider: ServerUrlProvider,
    private val sessionEventBus: SessionEventBus,
) {

    private val lock = Any()

    fun refreshBlocking(staleAccessToken: String? = null): String? =
        synchronized(lock) { performRefresh(staleAccessToken) }

    private fun performRefresh(staleAccessToken: String?): String? {
        val currentAccess = sessionManager.accessToken
        // Another thread already refreshed while we were waiting for the lock.
        if (staleAccessToken != null && currentAccess != null && currentAccess != staleAccessToken) {
            return currentAccess
        }
        val refreshToken = sessionManager.refreshToken
        if (refreshToken.isNullOrBlank()) {
            endSession()
            return null
        }
        val url = serverUrlProvider.absoluteUrl("${Constants.API_PREFIX}/auth/refresh")
        if (url == null) {
            return null
        }

        val payload = gson.toJson(
            RefreshRequest(refreshToken = refreshToken, deviceId = sessionManager.deviceId)
        )
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON))
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string()
                if (!response.isSuccessful || raw.isNullOrBlank()) {
                    if (response.code == 400 || response.code == 401 || response.code == 403) endSession()
                    return null
                }
                val envelope = runCatching { gson.fromJson(raw, RefreshEnvelope::class.java) }.getOrNull()
                val data = envelope?.data
                val newAccess = data?.accessToken
                if (newAccess.isNullOrBlank() || data == null) {
                    endSession()
                    return null
                }
                val accessExpiry = IsoFormat.parseInstant(data.accessExpiresAtUtc)?.toEpochMilli()
                    ?: (System.currentTimeMillis() + DEFAULT_ACCESS_LIFETIME_MILLIS)
                val refreshExpiry = IsoFormat.parseInstant(data.refreshExpiresAtUtc)?.toEpochMilli()
                    ?: (System.currentTimeMillis() + DEFAULT_REFRESH_LIFETIME_MILLIS)
                sessionManager.persist(
                    AuthTokens(
                        accessToken = newAccess,
                        refreshToken = data.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
                        accessExpiresAtMillis = accessExpiry,
                        refreshExpiresAtMillis = refreshExpiry,
                    ),
                    user = null,
                )
                sessionEventBus.emit(SessionEvent.Refreshed(accessExpiry))
                newAccess
            }
        } catch (e: IOException) {
            // Offline: keep the session, the caller surfaces a network error instead.
            null
        }
    }

    private fun endSession() {
        sessionManager.clear()
        sessionEventBus.emit(SessionEvent.Expired)
    }

    private data class RefreshRequest(
        @SerializedName("refreshToken") val refreshToken: String,
        @SerializedName("deviceId") val deviceId: String,
    )

    private data class RefreshEnvelope(
        @SerializedName("success") val success: Boolean? = null,
        @SerializedName("data") val data: RefreshData? = null,
    )

    private data class RefreshData(
        @SerializedName("accessToken") val accessToken: String? = null,
        @SerializedName("refreshToken") val refreshToken: String? = null,
        @SerializedName("accessExpiresAtUtc") val accessExpiresAtUtc: String? = null,
        @SerializedName("refreshExpiresAtUtc") val refreshExpiresAtUtc: String? = null,
    )

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val DEFAULT_ACCESS_LIFETIME_MILLIS = 15L * 60 * 1000
        const val DEFAULT_REFRESH_LIFETIME_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
