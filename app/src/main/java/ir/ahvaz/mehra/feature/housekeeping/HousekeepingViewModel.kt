package ir.ahvaz.mehra.feature.housekeeping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.HousekeepingPerformance
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.HousekeepingRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HousekeepingUiState(
    val tasks: UiState<List<HousekeepingTask>> = UiState(),
    val cleaningRooms: List<Room> = emptyList(),
    val allRooms: List<Room> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val performance: List<HousekeepingPerformance> = emptyList(),
    val statusFilter: HousekeepingTaskStatus? = null,
    val canManage: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val actionMessage: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class HousekeepingViewModel @Inject constructor(
    private val housekeepingRepository: HousekeepingRepository,
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(HousekeepingUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<HousekeepingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.HOUSEKEEPING_MANAGE) == true) }
            }
        }
        viewModelScope.launch {
            offlineQueue.observePendingCount().collect { count ->
                _state.update { it.copy(pendingOperations = count) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _state.update { it.copy(isOnline = online) } }
        }
        refresh()
        loadStaff()
        loadCleaningRooms()
        loadPerformance()
    }

    fun onStatusFilterChange(status: HousekeepingTaskStatus?) {
        _state.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(tasks = it.tasks.copy(isLoading = it.tasks.data == null, isRefreshing = it.tasks.data != null))
        }
        viewModelScope.launch {
            val result = housekeepingRepository.tasks(_state.value.statusFilter)
            _state.update { it.copy(tasks = it.tasks.applyResult(result)) }
        }
    }

    private fun loadStaff() {
        viewModelScope.launch {
            when (val result = housekeepingRepository.staff()) {
                is AppResult.Success -> _state.update { it.copy(staff = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadCleaningRooms() {
        viewModelScope.launch {
            when (val result = roomRepository.rooms()) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        allRooms = result.data.data,
                        cleaningRooms = result.data.data.filter { room -> room.status == RoomStatus.CLEANING },
                    )
                }

                is AppResult.Failure -> Unit
            }
        }
    }

    fun loadPerformance() {
        viewModelScope.launch {
            val today = JalaliDate.today().toGregorian()
            val from = today.withDayOfMonth(1)
            when (val result = housekeepingRepository.performance(from, today)) {
                is AppResult.Success -> _state.update { it.copy(performance = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun createCleaning(roomId: String, type: CleaningType, assigneeId: String?, notes: String?) {
        viewModelScope.launch {
            val result = housekeepingRepository.create(
                CreateCleaningCommand(roomId = roomId, type = type, assigneeId = assigneeId, notes = notes)
            )
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "درخواست نظافت برای اتاق ${result.data.roomNumber} ثبت شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
            loadCleaningRooms()
        }
    }

    fun assign(task: HousekeepingTask, staffId: String) {
        action("نظافت‌کار تخصیص یافت.") { housekeepingRepository.assign(task.id, staffId) }
    }

    fun start(task: HousekeepingTask) {
        action("شروع نظافت ثبت شد.") { housekeepingRepository.start(task.id) }
    }

    fun complete(task: HousekeepingTask, note: String?) {
        action("پایان نظافت ثبت شد و در انتظار تأیید است.") { housekeepingRepository.complete(task.id, note) }
    }

    fun approve(task: HousekeepingTask) {
        action("نظافت تأیید نهایی شد.") { housekeepingRepository.approve(task.id) }
    }

    private fun action(successMessage: String, block: suspend () -> AppResult<Unit>) {
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> successMessage
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            refresh()
            loadCleaningRooms()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
