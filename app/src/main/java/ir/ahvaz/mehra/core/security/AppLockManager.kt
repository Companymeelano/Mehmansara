package ir.ahvaz.mehra.core.security

import ir.ahvaz.mehra.core.datastore.SettingsRepository
import ir.ahvaz.mehra.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

/**
 * Local app lock.
 *
 * The PIN never leaves the device: only a PBKDF2 verifier (120k iterations, per-install
 * salt) is stored inside [SecureStore]. The lock is triggered automatically after the
 * configurable idle timeout, on process restart, and manually from settings.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val store: SecureStore,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    @Volatile
    private var lockTimeoutMillis: Long = DEFAULT_TIMEOUT_MINUTES * 60_000L

    @Volatile
    private var backgroundedAtMillis: Long = 0L

    private val _locked = MutableStateFlow(isPinSet)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    val isPinSet: Boolean
        get() = !store.getString(SecureStore.KEY_PIN_HASH).isNullOrBlank() &&
            !store.getString(SecureStore.KEY_PIN_SALT).isNullOrBlank()

    val isPersistentStorageAvailable: Boolean get() = store.isPersistentStorageAvailable

    init {
        appScope.launch {
            settingsRepository.settings.collect { settings ->
                lockTimeoutMillis = settings.lockTimeoutMinutes.coerceAtLeast(0) * 60_000L
            }
        }
    }

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        store.putString(SecureStore.KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        store.putString(SecureStore.KEY_PIN_HASH, hashPin(pin, salt))
        _locked.value = false
    }

    fun clearPin() {
        store.remove(SecureStore.KEY_PIN_HASH)
        store.remove(SecureStore.KEY_PIN_SALT)
        _locked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = store.getString(SecureStore.KEY_PIN_HASH) ?: return false
        val saltBase64 = store.getString(SecureStore.KEY_PIN_SALT) ?: return false
        val salt = runCatching { Base64.decode(saltBase64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val candidate = hashPin(pin, salt)
        // Constant time comparison.
        if (candidate.length != storedHash.length) return false
        var diff = 0
        for (i in candidate.indices) diff = diff or (candidate[i].code xor storedHash[i].code)
        return diff == 0
    }

    fun unlock(pin: String): Boolean {
        val valid = verifyPin(pin)
        if (valid) {
            backgroundedAtMillis = 0L
            _locked.value = false
        }
        return valid
    }

    fun lock() {
        if (isPinSet) _locked.value = true
    }

    fun onAppBackgrounded() {
        backgroundedAtMillis = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (!isPinSet) return
        if (backgroundedAtMillis == 0L) {
            // Cold start with an enrolled PIN: never auto-unlock.
            _locked.value = true
            return
        }
        val elapsed = System.currentTimeMillis() - backgroundedAtMillis
        if (lockTimeoutMillis == 0L || elapsed >= lockTimeoutMillis) {
            _locked.value = true
        }
        backgroundedAtMillis = 0L
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val hash = factory.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_BYTES = 16
        const val DEFAULT_TIMEOUT_MINUTES = 5
    }
}
