package ir.ahvaz.mehra.core.security

import ir.ahvaz.mehra.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress
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
        // Plain HTTP is allowed only for the hotel LAN (loopback/private/link-local
        // addresses, .local/.lan/.internal names) — that is how the Windows app
        // (Mehra.exe) serves the API on http://<PC-IP>:5050. Public hosts need HTTPS.
        if (url.scheme == "http" && !isLanHost(url.host)) {
            return "برای سرور خارج از شبکهٔ داخلی حتماً از HTTPS استفاده کنید (مثلاً https://mehra.example.ir)."
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
            // Bare LAN addresses (e.g. 192.168.1.20:5050) default to http; everything else to https.
            else -> "${if (BuildConfig.ALLOW_CLEARTEXT || isLanHost(lanHostOf(raw))) "http" else "https"}://$raw"
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

    companion object {
    /**
     * True for loopback, private (RFC1918), CGNAT, link-local addresses and
     * .local/.lan/.internal/.home.arpa names — i.e. the hotel LAN where the
     * Windows desktop app serves plain HTTP.
     */
    fun isLanHost(host: String): Boolean {
        val h = host.trim().trimEnd('.').lowercase()
        if (h.isEmpty()) return false
        if (h == "localhost" || h.endsWith(".localhost") ||
            h.endsWith(".local") || h.endsWith(".lan") ||
            h.endsWith(".internal") || h.endsWith(".home.arpa")) return true
        // IP literals never trigger a DNS lookup; hostnames are already covered above.
        val looksLikeIp = h.matches(Regex("[0-9.]+")) || (h.count { it == ':' } >= 2) ||
            (h.count { it == ':' } == 1 && h.substringBefore(':').matches(Regex("[0-9.]+")))
        if (!looksLikeIp) return false
        val addr = try { InetAddress.getByName(h).address } catch (e: Exception) { return false } ?: return false
        fun b(i: Int) = addr[i].toInt() and 0xFF
        return when {
            addr.size == 4 -> {
                val a = b(0); val bb = b(1)
                a == 10 ||                       // 10.0.0.0/8
                (a == 172 && bb in 16..31) ||     // 172.16.0.0/12
                (a == 192 && bb == 168) ||        // 192.168.0.0/16
                (a == 169 && bb == 254) ||        // link-local 169.254.0.0/16
                (a == 100 && bb in 64..127) ||    // CGNAT 100.64.0.0/10
                a == 127                         // loopback
            }
            else -> (b(0) and 0xFE) == 0xFC ||   // IPv6 ULA fc00::/7
                    (b(0) == 0xFE && (b(1) and 0xC0) == 0x80) || // IPv6 link-local fe80::/10
                    addr contentEquals byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1) // ::1
        }
    }

    /** Strips an optional :port from a bare "host[:port][/path]" input (IPv4 only; IPv6 keeps its colons). */
    private fun lanHostOf(raw: String): String {
        val noPath = raw.substringBefore('/')
        return if (noPath.count { it == ':' } == 1) noPath.substringBefore(':') else noPath
    }
    }
}
