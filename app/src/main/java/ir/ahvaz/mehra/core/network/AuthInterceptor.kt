package ir.ahvaz.mehra.core.network

import ir.ahvaz.mehra.BuildConfig
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the bearer token (except for login/refresh) plus client identification headers.
 * Never logs tokens: see [ir.ahvaz.mehra.core.di.NetworkModule] for the redacting logger.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isAuthEndpoint = path.endsWith("/auth/login") || path.endsWith("/auth/refresh")

        val builder = request.newBuilder()
            .header(Constants.HEADER_PLATFORM, "android")
            .header(Constants.HEADER_CLIENT_VERSION, BuildConfig.VERSION_NAME)
            .header(Constants.HEADER_DEVICE_ID, sessionManager.deviceId)
            .header("Accept-Language", "fa-IR")

        if (!isAuthEndpoint) {
            sessionManager.accessToken?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
        }
        return chain.proceed(builder.build())
    }
}
