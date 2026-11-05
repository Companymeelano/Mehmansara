package ir.ahvaz.mehra.core.network

import ir.ahvaz.mehra.core.security.ServerUrlProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit is built against a placeholder host; the real host/port/prefix comes from the
 * operator configuration at runtime, so the address can be changed without restarting.
 */
@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = serverUrlProvider.current().toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val sameTarget = base.scheme == request.url.scheme &&
            base.host == request.url.host &&
            base.port == request.url.port &&
            base.encodedPath.trimEnd('/').isEmpty()

        if (sameTarget) return chain.proceed(request)

        val prefix = base.encodedPath.trimEnd('/')
        val rewritten = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(prefix + request.url.encodedPath)
            .build()

        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
