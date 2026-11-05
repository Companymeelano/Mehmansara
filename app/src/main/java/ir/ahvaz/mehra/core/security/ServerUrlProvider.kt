package ir.ahvaz.mehra.core.security

import ir.ahvaz.mehra.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The backend address is operator configurable (an internal LAN address in production,
 * often plain HTTP on a lab machine while developing).
 *
 * The value lives in [SecureStore] so it can be read synchronously by the OkHttp
 * interceptor during app start-up, and is exposed as a flow for the settings screen.
 */
@Singleton
class ServerUrlProvider @Inject constructor(private val store: SecureStore) {

    private val _serverUrl = MutableStateFlow(store.getString(SecureStore.KEY_SERVER_URL).orEmpty())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun isConfigured(): Boolean = _serverUrl.value.isNotBlank()

    fun current(): String = _serverUrl.value

    /**
     * Validates and stores a user supplied address.
     * @return null on success, otherwise a Persian error message.
     */
    fun save(rawInput: String): String? {
        val normalized = normalize(rawInput) ?: return "آدرس سرور معتبر نیست. نمونه صحیح: https://mehra.example.ir/"
        val url = normalized.toHttpUrlOrNull() ?: return "آدرس سرور معتبر نیست."
        if (!BuildConfig.ALLOW_CLEARTEXT && url.scheme != "https") {
            return "در نسخه عملیاتی فقط اتصال HTTPS مجاز است."
        }
        store.putString(SecureStore.KEY_SERVER_URL, normalized)
        _serverUrl.value = normalized
        return null
    }

    /**
     * Accepts "mehra.local", "192.168.1.20:5001", "http://10.0.0.5:5000/mehra" and
     * returns a canonical base URL ending with '/'.
     */
    fun normalize(rawInput: String): String? {
        val raw = rawInput.trim().trimEnd('/')
        if (raw.isBlank()) return null
        val withScheme = when {
            raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
            else -> "${if (BuildConfig.ALLOW_CLEARTEXT) "http" else "https"}://$raw"
        }
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.host.isBlank()) return null
        return parsed.newBuilder().build().toString()
    }

    /** Absolute URL for an API path such as "/api/v1/auth/login". */
    fun absoluteUrl(path: String): String? {
        val base = current().trimEnd('/')
        if (base.isBlank()) return null
        return base + (if (path.startsWith("/")) path else "/$path")
    }
}
