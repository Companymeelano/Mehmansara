package ir.ahvaz.mehra.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.datastore.SettingsRepository
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val dashboard: UiState<DashboardSnapshot> = UiState(),
    val currencyUnit: CurrencyUnit = CurrencyUnit.RIAL,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val user: SessionUser? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(currencyUnit = settings.currencyUnit) }
            }
        }
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            offlineQueue.observePendingCount().collect { count ->
                _state.update { it.copy(pendingOperations = count) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(dashboard = it.dashboard.copy(isLoading = it.dashboard.data == null, isRefreshing = it.dashboard.data != null)) }
        viewModelScope.launch {
            val result = dashboardRepository.load()
            _state.update { it.copy(dashboard = it.dashboard.applyResult(result)) }
        }
    }
}
