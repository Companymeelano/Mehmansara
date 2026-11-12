package ir.ahvaz.mehra.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.datastore.SettingsRepository
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.network.SessionEvent
import ir.ahvaz.mehra.core.network.SessionEventBus
import ir.ahvaz.mehra.core.security.AppLockManager
import ir.ahvaz.mehra.core.security.LocalOperator
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.data.sync.OfflineQueue
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

data class RootUiState(
    val themeId: String = "golden_executive",
    val currencyUnit: CurrencyUnit = CurrencyUnit.RIAL,
    val notificationsEnabled: Boolean = true,
    val serverConfigured: Boolean = false,
    val session: SessionUser? = null,
    val locked: Boolean = false,
    val pinSet: Boolean = false,
    val isOnline: Boolean = true,
    val pendingOperations: Int = 0,
    val unreadNotifications: Int = 0,
    val realtime: RealtimeStatus = RealtimeStatus(),
    val sessionExpired: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val appLockManager: AppLockManager,
    private val networkMonitor: NetworkMonitor,
    private val offlineQueue: OfflineQueue,
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager,
    sessionEventBus: SessionEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RootUiState(
            serverConfigured = serverUrlProvider.isConfigured(),
            session = sessionManager.currentUser,
            locked = appLockManager.locked.value,
            pinSet = appLockManager.isPinSet,
            isOnline = networkMonitor.isCurrentlyOnline(),
        )
    )
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        // Fully local app (1.4.0): no login screen — make sure a local operator session exists.
        if (sessionManager.currentUser == null) {
            sessionManager.updateUser(LocalOperator.session())
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        themeId = settings.themeId,
                        currencyUnit = settings.currencyUnit,
                        notificationsEnabled = settings.notificationsEnabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(session = user) }
            }
        }
        viewModelScope.launch {
            serverUrlProvider.serverUrl.collect { url ->
                _state.update { it.copy(serverConfigured = url.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            appLockManager.locked.collect { locked ->
                _state.update { it.copy(locked = locked, pinSet = appLockManager.isPinSet) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            offlineQueue.observePendingCount().collect { count ->
                _state.update { it.copy(pendingOperations = count) }
            }
        }
        viewModelScope.launch {
            notificationRepository.observeUnreadCount().collect { count ->
                _state.update { it.copy(unreadNotifications = count) }
            }
        }
        viewModelScope.launch {
            notificationRepository.realtimeStatus().collect { status ->
                _state.update { it.copy(realtime = status) }
            }
        }
        viewModelScope.launch {
            sessionEventBus.events.collect { event ->
                when (event) {
                    SessionEvent.Expired -> {
                        _state.update { it.copy(sessionExpired = true, session = null) }
                        notificationRepository.stopRealtime()
                    }

                    SessionEvent.LoggedOut -> {
                        _state.update { it.copy(session = null, sessionExpired = false) }
                        notificationRepository.stopRealtime()
                    }

                    is SessionEvent.Refreshed -> Unit
                }
            }
        }
    }

    fun dismissSessionExpired() {
        _state.update { it.copy(sessionExpired = false) }
    }

    fun tryUnlock(pin: String): Boolean = appLockManager.unlock(pin)

    fun lockNow() = appLockManager.lock()

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            notificationRepository.stopRealtime()
            _state.update { it.copy(session = null) }
            onDone()
        }
    }

    fun onAuthenticationSucceeded() {
        _state.update { it.copy(sessionExpired = false) }
        notificationRepository.startRealtime()
    }

    fun settingsDefaults(): Triple<String, CurrencyUnit, Boolean> =
        Triple(_state.value.themeId, _state.value.currencyUnit, _state.value.notificationsEnabled)

}
