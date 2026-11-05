package ir.ahvaz.mehra.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware-backed storage for tokens, PIN verifier and the server address.
 *
 * Values are wrapped by an AES256-GCM Android Keystore master key. If the Keystore is
 * unusable on a device (corrupted keystore, exotic OEM) we deliberately fall back to an
 * in-memory map — session data then simply does not survive a restart, but secrets are
 * never written to disk in clear text.
 */
@Singleton
class SecureStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ) as SharedPreferences
    }.onFailure { Log.e(TAG, "Encrypted storage unavailable, falling back to memory only", it) }
        .getOrNull()

    private val memoryFallback = mutableMapOf<String, String>()

    val isPersistentStorageAvailable: Boolean get() = prefs != null

    fun getString(key: String): String? = prefs?.getString(key, null) ?: memoryFallback[key]

    fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }
        if (prefs != null) {
            prefs.edit().putString(key, value).apply()
        } else {
            memoryFallback[key] = value
        }
    }

    fun getLong(key: String, default: Long = 0L): Long =
        prefs?.getLong(key, default) ?: memoryFallback[key]?.toLongOrNull() ?: default

    fun putLong(key: String, value: Long) {
        if (prefs != null) prefs.edit().putLong(key, value).apply() else memoryFallback[key] = value.toString()
    }

    fun remove(key: String) {
        if (prefs != null) prefs.edit().remove(key).apply() else memoryFallback.remove(key)
    }

    /** Clears credentials but keeps the device id and server address. */
    fun clearSessionKeys(keys: Collection<String>) {
        if (prefs != null) {
            prefs.edit().apply { keys.forEach { remove(it) } }.apply()
        } else {
            keys.forEach { memoryFallback.remove(it) }
        }
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val FILE_NAME = "mehra_secure_store"

        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
        const val KEY_USER_JSON = "user_json"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_LAST_SYNC = "last_sync_at"

        val SESSION_KEYS = listOf(
            KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_ACCESS_EXPIRES_AT,
            KEY_REFRESH_EXPIRES_AT, KEY_USER_JSON,
        )
    }
}
