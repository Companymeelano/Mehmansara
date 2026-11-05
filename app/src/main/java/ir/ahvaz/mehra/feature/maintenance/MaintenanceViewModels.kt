package ir.ahvaz.mehra.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.ErrorKind
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.MaintenanceRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceUiState(
    val tickets: UiState<List<MaintenanceTicket>> = UiState(),
    val statusFilter: MaintenanceStatus? = null,
    val priorityFilter: MaintenancePriority? = null,
    val canManage: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val actionMessage: String? = null,
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<MaintenanceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.MAINTENANCE_MANAGE) == true) }
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
    }

    fun onStatusFilterChange(status: MaintenanceStatus?) {
        _state.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun onPriorityFilterChange(priority: MaintenancePriority?) {
        _state.update { it.copy(priorityFilter = priority) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(
                tickets = it.tickets.copy(
                    isLoading = it.tickets.data == null,
                    isRefreshing = it.tickets.data != null,
                )
            )
        }
        viewModelScope.launch {
            val current = _state.value
            val result = maintenanceRepository.tickets(current.statusFilter, current.priorityFilter)
            _state.update { it.copy(tickets = it.tickets.applyResult(result)) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}

data class MaintenanceFormUiState(
    val rooms: List<Room> = emptyList(),
    val selectedRoom: Room? = null,
    val title: String = "",
    val description: String = "",
    val priority: MaintenancePriority = MaintenancePriority.NORMAL,
    val photoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val created: MaintenanceTicket? = null,
    val photoUploadFailed: Boolean = false,
    val pendingConfirmation: Boolean = false,
)

@HiltViewModel
class MaintenanceFormViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceFormUiState())
    val state: StateFlow<MaintenanceFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = roomRepository.rooms()) {
                is AppResult.Success -> _state.update { it.copy(rooms = result.data.data) }
                is AppResult.Failure -> _state.update { it.copy(errorMessage = result.error.userMessageFa) }
            }
        }
    }

    fun selectRoom(room: Room) {
        _state.update { it.copy(selectedRoom = room, fieldErrors = emptyMap()) }
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, fieldErrors = emptyMap()) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value, fieldErrors = emptyMap()) }
    }

    fun onPriorityChange(priority: MaintenancePriority) {
        _state.update { it.copy(priority = priority) }
    }

    fun addPhotos(uris: List<String>) {
        _state.update { it.copy(photoUris = (it.photoUris + uris).distinct().take(5)) }
    }

    fun removePhoto(uri: String) {
        _state.update { it.copy(photoUris = it.photoUris - uri) }
    }

    fun submit() {
        val current = _state.value
        val room = current.selectedRoom
        val fields = mutableMapOf<String, String>()
        if (room == null) fields["roomId"] = "اتاق را انتخاب کنید."
        if (current.title.isBlank()) fields["title"] = "عنوان خرابی الزامی است."
        if (current.description.isBlank()) fields["description"] = "شرح خرابی الزامی است."
        if (fields.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = fields, errorMessage = "اطلاعات خرابی را کامل کنید.") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null, photoUploadFailed = false) }
        viewModelScope.launch {
            val command = CreateMaintenanceCommand(
                roomId = room!!.id,
                title = current.title.trim(),
                description = current.description.trim(),
                priority = current.priority,
                localImageUris = current.photoUris,
            )
            when (val result = maintenanceRepository.create(command)) {
                is AppResult.Success -> {
                    val ticket = result.data
                    if (current.photoUris.isNotEmpty()) {
                        // Photos are a secondary artefact: the ticket itself is already registered
                        // server-side, so a failed upload must not be reported as a failed report.
                        when (maintenanceRepository.uploadPhotos(ticket.id, current.photoUris)) {
                            is AppResult.Success -> _state.update {
                                it.copy(isSaving = false, created = ticket, photoUploadFailed = false)
                            }

                            is AppResult.Failure -> _state.update {
                                it.copy(isSaving = false, created = ticket, photoUploadFailed = true)
                            }
                        }
                    } else {
                        _state.update { it.copy(isSaving = false, created = ticket) }
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.error.userMessageFa,
                        fieldErrors = result.error.fieldErrors,
                        pendingConfirmation = result.error.kind == ErrorKind.QUEUED_OFFLINE,
                    )
                }
            }
        }
    }
}

data class MaintenanceDetailUiState(
    val ticket: UiState<MaintenanceTicket> = UiState(),
    val technicians: List<StaffMember> = emptyList(),
    val canManage: Boolean = false,
    val actionMessage: String? = null,
    val isActing: Boolean = false,
)

@HiltViewModel
class MaintenanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val maintenanceRepository: MaintenanceRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val ticketId: String = savedStateHandle.get<String>("ticketId").orEmpty()

    private val _state = MutableStateFlow(MaintenanceDetailUiState())
    val state: StateFlow<MaintenanceDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.MAINTENANCE_MANAGE) == true) }
            }
        }
        viewModelScope.launch {
            when (val result = maintenanceRepository.technicians()) {
                is AppResult.Success -> _state.update { it.copy(technicians = result.data) }
                is AppResult.Failure -> Unit
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(ticket = it.ticket.copy(isLoading = it.ticket.data == null)) }
        viewModelScope.launch {
            val result = maintenanceRepository.ticket(ticketId)
            _state.update { it.copy(ticket = it.ticket.applyValue(result)) }
        }
    }

    fun assign(technicianId: String) = act("کارشناس تعمیرات تخصیص یافت.") {
        maintenanceRepository.assign(ticketId, technicianId)
    }

    fun updateStatus(status: MaintenanceStatus) = act("وضعیت خرابی به‌روزرسانی شد.") {
        maintenanceRepository.updateStatus(ticketId, status)
    }

    fun registerCost(amount: String, description: String?) {
        val parsed = parseLongInput(amount.toLatinDigits())
        if (parsed == null || parsed < 0) {
            _state.update { it.copy(actionMessage = "مبلغ هزینه را به‌درستی وارد کنید.") }
            return
        }
        act("هزینه تعمیرات ثبت شد.") {
            maintenanceRepository.registerCost(ticketId, parsed, description?.takeIf { it.isNotBlank() })
        }
    }

    fun releaseRoom() = act("اتاق آزاد و آماده بهره‌برداری شد.") {
        maintenanceRepository.releaseRoom(ticketId)
    }

    private fun act(successMessage: String, block: suspend () -> AppResult<Unit>) {
        _state.update { it.copy(isActing = true, actionMessage = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    isActing = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> successMessage
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            refresh()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
