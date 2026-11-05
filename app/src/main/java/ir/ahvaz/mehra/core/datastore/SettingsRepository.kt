package ir.ahvaz.mehra.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mehraSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mehra_settings")

/** Non-sensitive operator preferences (the server address lives in [ir.ahvaz.mehra.core.security.SecureStore]). */
data class AppSettings(
    val themeId: String = ThemePreset.GOLDEN_EXECUTIVE.id,
    val currencyUnit: CurrencyUnit = CurrencyUnit.RIAL,
    val lockTimeoutMinutes: Int = Constants.DEFAULT_LOCK_TIMEOUT_MINUTES,
    val notificationsEnabled: Boolean = true,
    val realtimeEnabled: Boolean = true,
    val lastSyncMillis: Long? = null,
) {
    val theme: ThemePreset get() = ThemePreset.fromId(themeId)
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_id")
        val CURRENCY = stringPreferencesKey("currency_unit")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout_minutes")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val REALTIME = booleanPreferencesKey("realtime_enabled")
        val LAST_SYNC = longPreferencesKey("last_sync_millis")
    }

    val settings: Flow<AppSettings> = context.mehraSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeId = prefs[Keys.THEME] ?: ThemePreset.GOLDEN_EXECUTIVE.id,
            currencyUnit = CurrencyUnit.fromCode(prefs[Keys.CURRENCY]),
            lockTimeoutMinutes = prefs[Keys.LOCK_TIMEOUT] ?: Constants.DEFAULT_LOCK_TIMEOUT_MINUTES,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            realtimeEnabled = prefs[Keys.REALTIME] ?: true,
            lastSyncMillis = prefs[Keys.LAST_SYNC],
        )
    }

    suspend fun setTheme(themeId: String) {
        context.mehraSettingsDataStore.edit { it[Keys.THEME] = themeId }
    }

    suspend fun setCurrency(unit: CurrencyUnit) {
        context.mehraSettingsDataStore.edit { it[Keys.CURRENCY] = unit.code }
    }

    suspend fun setLockTimeoutMinutes(minutes: Int) {
        context.mehraSettingsDataStore.edit { it[Keys.LOCK_TIMEOUT] = minutes.coerceIn(0, 120) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.mehraSettingsDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setRealtimeEnabled(enabled: Boolean) {
        context.mehraSettingsDataStore.edit { it[Keys.REALTIME] = enabled }
    }

    suspend fun markSynced(atMillis: Long) {
        context.mehraSettingsDataStore.edit { it[Keys.LAST_SYNC] = atMillis }
    }
}
