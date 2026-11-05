package ir.ahvaz.mehra.core.security

import com.google.gson.Gson
import ir.ahvaz.mehra.domain.model.SessionUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtMillis: Long,
    val refreshExpiresAtMillis: Long,
)

/**
 * Owns the session: tokens (encrypted at rest), the signed-in user and the device identity.
 * The access token is additionally cached in memory so the OkHttp interceptor never blocks.
 */
@Singleton
class SessionManager @Inject constructor(
    private val store: SecureStore,
    private val gson: Gson,
) {

    private val _session = MutableStateFlow(readUser())
    val session: StateFlow<SessionUser?> = _session.asStateFlow()

    @Volatile
    private var accessTokenCache: String? = store.getString(SecureStore.KEY_ACCESS_TOKEN)

    @Volatile
    private var refreshTokenCache: String? = store.getString(SecureStore.KEY_REFRESH_TOKEN)

    val accessToken: String? get() = accessTokenCache

    val refreshToken: String? get() = refreshTokenCache

    val accessExpiresAtMillis: Long get() = store.getLong(SecureStore.KEY_ACCESS_EXPIRES_AT)

    val refreshExpiresAtMillis: Long get() = store.getLong(SecureStore.KEY_REFRESH_EXPIRES_AT)

    val isLoggedIn: Boolean get() = !refreshTokenCache.isNullOrBlank() && _session.value != null

    val currentUser: SessionUser? get() = _session.value

    /** Stable per-install identifier sent to the server for session revocation. */
    val deviceId: String by lazy {
        store.getString(SecureStore.KEY_DEVICE_ID) ?: UUID.randomUUID().toString().also {
            store.putString(SecureStore.KEY_DEVICE_ID, it)
        }
    }

    fun currentTokens(): AuthTokens? {
        val access = accessTokenCache ?: return null
        val refresh = refreshTokenCache ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            accessExpiresAtMillis = accessExpiresAtMillis,
            refreshExpiresAtMillis = refreshExpiresAtMillis,
        )
    }

    fun persist(tokens: AuthTokens, user: SessionUser?) {
        accessTokenCache = tokens.accessToken
        refreshTokenCache = tokens.refreshToken
        store.putString(SecureStore.KEY_ACCESS_TOKEN, tokens.accessToken)
        store.putString(SecureStore.KEY_REFRESH_TOKEN, tokens.refreshToken)
        store.putLong(SecureStore.KEY_ACCESS_EXPIRES_AT, tokens.accessExpiresAtMillis)
        store.putLong(SecureStore.KEY_REFRESH_EXPIRES_AT, tokens.refreshExpiresAtMillis)
        if (user != null) updateUser(user)
    }

    fun updateUser(user: SessionUser) {
        store.putString(SecureStore.KEY_USER_JSON, gson.toJson(user))
        _session.value = user
    }

    fun clear() {
        accessTokenCache = null
        refreshTokenCache = null
        store.clearSessionKeys(SecureStore.SESSION_KEYS)
        _session.value = null
    }

    fun hasPermission(permission: String): Boolean = _session.value?.can(permission) == true

    private fun readUser(): SessionUser? {
        val json = store.getString(SecureStore.KEY_USER_JSON) ?: return null
        return runCatching { gson.fromJson(json, SessionUser::class.java) }.getOrNull()
    }
}
