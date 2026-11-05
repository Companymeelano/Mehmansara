package ir.ahvaz.mehra.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard OkHttp 401 recovery: refresh once and replay the original request.
 * Gives up after two attempts so a broken token cannot cause an infinite loop.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val refreshCoordinator: TokenRefreshCoordinator,
    private val sessionEventBus: SessionEventBus,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val authorization = response.request.header("Authorization") ?: return null
        if (attemptCount(response) >= 2) {
            sessionEventBus.emit(SessionEvent.Expired)
            return null
        }
        val staleToken = authorization.removePrefix("Bearer").trim()
        val newToken = refreshCoordinator.refreshBlocking(staleToken) ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun attemptCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}
