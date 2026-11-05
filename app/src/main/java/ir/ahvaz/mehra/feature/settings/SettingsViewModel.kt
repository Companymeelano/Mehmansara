package ir.ahvaz.mehra.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.datastore.AppSettings
import ir.ahvaz.mehra.core.datastore.SettingsRepository
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.security.AppLockManager
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.core.ui.theme.ThemePreset
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OutboxProcessor
import ir.ahvaz.mehra.domain.model.RealtimeStatus
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val serverUrl: String = "",
    val session: SessionUser? = null,
    val isOnline: Boolean = true,
    val realtime: RealtimeStatus = RealtimeStatus(),
    val pendingOperations: Int = 0,
    val isPinSet: Boolean = false,
    val isVerifyingServer: Boolean = false,
    val isSavingServer: Boolean = false,
    val message: String? = null,
    val serverSaved: Boolean = false,
    val isLoggingOut: Boolean = false,
    val loggedOut: Boolean = false,
    val serverVersion: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val sessionManager: SessionManager,
    private val appLockManager: AppLockManager,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val offlineQueue: OfflineQueue,
    private val outboxProcessor: OutboxProcessor,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            serverUrl = serverUrlProvider.current(),
            session = sessionManager.currentUser,
            isOnline = networkMonitor.isCurrentlyOnline(),
            isPinSet = appLockManager.isPinSet,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            authRepository.observeSession().collect { user -> _state.update { it.copy(session = user) } }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _state.update { it.copy(isOnline = online) } }
        }
        viewModelScope.launch {
            notificationRepository.realtimeStatus().collect { status ->
                _state.update { it.copy(realtime = status) }
            }
        }
        viewModelScope.launch {
            offlineQueue.observePendingCount().collect { count ->
                _state.update { it.copy(pendingOperations = count) }
            }
        }
    }

    fun setTheme(preset: ThemePreset) {
        viewModelScope.launch { settingsRepository.setTheme(preset.id) }
    }

    fun setCurrency(unit: CurrencyUnit) {
        viewModelScope.launch { settingsRepository.setCurrency(unit) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setRealtime(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRealtimeEnabled(enabled)
            if (enabled) notificationRepository.startRealtime() else notificationRepository.stopRealtime()
        }
    }

    fun setLockTimeout(minutes: Int) {
        viewModelScope.launch { settingsRepository.setLockTimeoutMinutes(minutes) }
    }

    fun setPin(pin: String) {
        if (pin.length != 6 || pin.any { !it.isDigit() }) {
            _state.update { it.copy(message = "رمز قفل باید ۶ رقم باشد.") }
            return
        }
        appLockManager.setPin(pin)
        _state.update { it.copy(isPinSet = true, message = "رمز قفل تنظیم شد.") }
    }

    fun clearPin(currentPin: String) {
        if (!appLockManager.verifyPin(currentPin)) {
            _state.update { it.copy(message = "رمز قفل فعلی درست نیست.") }
            return
        }
        appLockManager.clearPin()
        _state.update { it.copy(isPinSet = false, message = "قفل صفحه غیرفعال شد.") }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        when {
            newPassword.length < 8 -> {
                _state.update { it.copy(message = "رمز جدید باید حداقل ۸ کاراکتر باشد.") }
                return
            }

            newPassword != confirmPassword -> {
                _state.update { it.copy(message = "تکرار رمز جدید مطابقت ندارد.") }
                return
            }
        }
        viewModelScope.launch {
            val result = authRepository.changePassword(currentPassword, newPassword)
            _state.update {
                it.copy(
                    message = when (result) {
                        is AppResult.Success -> "رمز عبور تغییر کرد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
        }
    }

    fun onServerUrlChange(value: String) {
        _state.update { it.copy(serverUrl = value, serverSaved = false) }
    }

    fun testServer() {
        _state.update { it.copy(isVerifyingServer = true, message = null) }
        viewModelScope.launch {
            val result = authRepository.serverInfo(_state.value.serverUrl)
            _state.update {
                it.copy(
                    isVerifyingServer = false,
                    serverVersion = (result as? AppResult.Success)?.data?.serverVersion,
                    message = when (result) {
                        is AppResult.Success -> "اتصال به سرور برقرار است."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
        }
    }

    fun saveServerUrl() {
        _state.update { it.copy(isSavingServer = true, message = null) }
        val normalized = serverUrlProvider.save(_state.value.serverUrl)
        _state.update {
            it.copy(
                isSavingServer = false,
                serverUrl = normalized ?: it.serverUrl,
                serverSaved = normalized != null,
                message = if (normalized == null) {
                    "نشانی سرور نامعتبر است. نمونه صحیح: https://mehra.example.ir یا http://192.168.1.10:5000"
                } else {
                    "نشانی سرور ذخیره شد."
                },
            )
        }
    }

    fun retryPendingOperations() {
        viewModelScope.launch {
            val result = outboxProcessor.process()
            _state.update {
                it.copy(
                    message = when {
                        result.sent > 0 && result.remaining == 0 -> "همه عملیات در انتظار با موفقیت ارسال شد."
                        result.sent > 0 -> "بخشی از عملیات ارسال شد؛ ${result.remaining} عملیات باقی مانده است."
                        else -> "ارسال عملیات ناموفق بود؛ اتصال به سرور را بررسی کنید."
                    }
                )
            }
        }
    }

    fun logout() {
        _state.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            notificationRepository.stopRealtime()
            authRepository.logout()
            _state.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
