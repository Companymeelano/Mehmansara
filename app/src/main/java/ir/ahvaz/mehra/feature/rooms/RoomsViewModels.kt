package ir.ahvaz.mehra.feature.rooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.DefaultRoomInventory
import ir.ahvaz.mehra.domain.model.LookupItem
import ir.ahvaz.mehra.domain.model.RoomDraft
import ir.ahvaz.mehra.domain.model.RoomFilterState
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.model.RoomLookups
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomsUiState(
    val rooms: UiState<List<Room>> = UiState(),
    val query: String = "",
    val status: RoomStatus? = null,
    val floor: Int? = null,
    val floors: List<Int> = emptyList(),
    val roomTypes: List<LookupItem> = emptyList(),
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val canManage: Boolean = false,
    val actionMessage: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomsUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<RoomsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.ROOMS_MANAGE) == true) }
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
        loadLookups()
        refresh()
    }

    private fun loadLookups() {
        viewModelScope.launch {
            when (val result = roomRepository.lookups()) {
                is AppResult.Success -> _state.update {
                    it.copy(floors = result.data.floors, roomTypes = result.data.roomTypes)
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun onStatusChange(status: RoomStatus?) {
        _state.update { it.copy(status = status) }
        refresh()
    }

    fun onFloorChange(floor: Int?) {
        _state.update { it.copy(floor = floor) }
        refresh()
    }

    fun applySearch() = refresh()

    /** One-tap status change from the floor board (v1.5.0). */
    fun changeStatus(room: Room, status: RoomStatus) {
        if (!_state.value.canManage) return
        viewModelScope.launch {
            _state.update { it.copy(actionInProgress = true) }
            when (val result = roomRepository.changeStatus(room.id, status)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            actionInProgress = false,
                            actionMessage = "وضعیت اتاق ${room.roomNumber.toPersianDigits()} به «${status.labelFa}» تغییر کرد.",
                        )
                    }
                    refresh()
                }
                is AppResult.Failure -> _state.update {
                    it.copy(actionInProgress = false, actionMessage = "تغییر وضعیت انجام نشد.")
                }
            }
        }
    }

    fun refresh() {
        val current = _state.value
        _state.update {
            it.copy(
                rooms = it.rooms.copy(
                    isLoading = it.rooms.data == null,
                    isRefreshing = it.rooms.data != null,
                )
            )
        }
        viewModelScope.launch {
            val result = roomRepository.rooms(
                RoomFilterState(query = current.query, status = current.status, floor = current.floor)
            )
            _state.update { it.copy(rooms = it.rooms.applyResult(result)) }
        }
    }

    /**
     * One-tap seed of the operator's own room inventory (DefaultRoomInventory) through the
     * real create API — no sample data, these are the hotel's actual rooms.
     */
    fun seedDefaultRooms() {
        if (!_state.value.canManage) {
            _state.update { it.copy(actionMessage = "برای ثبت اتاق دسترسی ندارید.") }
            return
        }
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val lookups = roomRepository.lookups()
            val types = (lookups as? AppResult.Success)?.data?.roomTypes.orEmpty()
            var created = 0
            for ((number, beds) in DefaultRoomInventory.rooms) {
                val digitsFa = beds.toString().toPersianDigits()
                val typeId = types.firstOrNull { it.title.contains("$digitsFa\u200cتخته") || it.title.contains("$beds تخته") }?.id
                    ?: types.firstOrNull()?.id.orEmpty()
                val draft = RoomDraft(
                    roomNumber = number,
                    floor = number.take(1),
                    capacity = beds.toString(),
                    roomTypeId = typeId,
                    ratePerNightRial = "3000000",
                    status = RoomStatus.FREE,
                )
                when (roomRepository.save(draft)) {
                    is AppResult.Success -> created++
                    is AppResult.Failure -> Unit
                }
            }
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = "${created.toString().toPersianDigits()} اتاق از فهرست پیش‌فرض مجموعه ثبت شد.",
                )
            }
            refresh()
        }
    }

    fun changeStatus(room: Room, status: RoomStatus) {
        if (!_state.value.canManage) {
            _state.update { it.copy(actionMessage = "برای تغییر وضعیت اتاق دسترسی ندارید.") }
            return
        }
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val result = roomRepository.changeStatus(room.id, status)
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "وضعیت اتاق ${room.roomNumber} به «${status.labelFa}» تغییر کرد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            if (result is AppResult.Success) refresh()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}

data class RoomDetailUiState(
    val room: UiState<Room> = UiState(),
    val history: UiState<List<RoomHistoryEntry>> = UiState(),
    val lookups: RoomLookups = RoomLookups(),
    val canManage: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionMessage: String? = null,
)

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId").orEmpty()

    private val _state = MutableStateFlow(RoomDetailUiState())
    val state: StateFlow<RoomDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.ROOMS_MANAGE) == true) }
            }
        }
        viewModelScope.launch {
            when (val result = roomRepository.lookups()) {
                is AppResult.Success -> _state.update { it.copy(lookups = result.data) }
                is AppResult.Failure -> Unit
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(
                room = it.room.copy(isLoading = it.room.data == null, isRefreshing = it.room.data != null),
                history = it.history.copy(isLoading = it.history.data == null),
            )
        }
        viewModelScope.launch {
            val roomResult = roomRepository.room(roomId)
            _state.update { it.copy(room = it.room.applyValue(roomResult)) }
        }
        viewModelScope.launch {
            val historyResult = roomRepository.history(roomId)
            _state.update { it.copy(history = it.history.applyValue(historyResult)) }
        }
    }

    fun changeStatus(status: RoomStatus) {
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val result = roomRepository.changeStatus(roomId, status)
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "وضعیت اتاق به «${status.labelFa}» تغییر کرد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            if (result is AppResult.Success) refresh()
        }
    }

    fun requestCleaning(type: CleaningType, notes: String?) {
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val result = roomRepository.requestCleaning(
                roomId,
                CreateCleaningCommand(roomId = roomId, type = type, notes = notes),
            )
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "درخواست نظافت ثبت و به واحد خدمات ارجاع شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
        }
    }

    fun reportMaintenance(title: String, description: String, priority: MaintenancePriority) {
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val result = roomRepository.createMaintenance(
                roomId,
                CreateMaintenanceCommand(
                    roomId = roomId,
                    title = title,
                    description = description,
                    priority = priority,
                ),
            )
            _state.update {
                it.copy(
                    actionInProgress = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "درخواست تعمیرات ثبت شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}

data class RoomFormUiState(
    val draft: RoomDraft = RoomDraft(),
    val lookups: RoomLookups = RoomLookups(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val saved: Boolean = false,
)

@HiltViewModel
class RoomFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val roomId: String? = savedStateHandle.get<String>("roomId")?.takeIf { it.isNotBlank() && it != "null" }

    private val _state = MutableStateFlow(RoomFormUiState())
    val state: StateFlow<RoomFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = roomRepository.lookups()) {
                is AppResult.Success -> _state.update { it.copy(lookups = result.data, isLoading = false) }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.userMessageFa)
                }
            }
            if (roomId != null) {
                when (val result = roomRepository.room(roomId)) {
                    is AppResult.Success -> _state.update {
                        it.copy(
                            draft = RoomDraft(
                                id = result.data.id,
                                roomNumber = result.data.roomNumber,
                                floor = result.data.floor.toString(),
                                capacity = result.data.capacity.toString(),
                                roomTypeId = result.data.roomTypeId,
                                ratePerNightRial = result.data.ratePerNightRial.toString(),
                                amenities = result.data.amenities.toSet(),
                                status = result.data.status,
                                notes = result.data.notes.orEmpty(),
                            )
                        )
                    }

                    is AppResult.Failure -> _state.update { it.copy(errorMessage = result.error.userMessageFa) }
                }
            }
        }
    }

    fun updateDraft(transform: (RoomDraft) -> RoomDraft) {
        _state.update { it.copy(draft = transform(it.draft), fieldErrors = emptyMap(), errorMessage = null) }
    }

    fun toggleAmenity(id: String) {
        _state.update {
            val amenities = it.draft.amenities.toMutableSet()
            if (!amenities.add(id)) amenities.remove(id)
            it.copy(draft = it.draft.copy(amenities = amenities))
        }
    }

    fun save() {
        val draft = _state.value.draft
        val fields = mutableMapOf<String, String>()
        if (draft.roomNumber.isBlank()) fields["roomNumber"] = "شماره اتاق الزامی است."
        if (parseLongInput(draft.floor) == null) fields["floor"] = "طبقه را وارد کنید."
        val capacity = parseLongInput(draft.capacity)?.toInt() ?: 0
        if (capacity < 1) fields["capacity"] = "ظرفیت باید حداقل ۱ باشد."
        if (draft.roomTypeId.isBlank()) fields["roomTypeId"] = "نوع اتاق را انتخاب کنید."
        if (parseLongInput(draft.ratePerNightRial.toLatinDigits()) == null) {
            fields["rate"] = "تعرفه شبانه را وارد کنید."
        }
        if (fields.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = fields, errorMessage = "اطلاعات اتاق را کامل کنید.") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = roomRepository.save(draft)) {
                is AppResult.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.error.userMessageFa,
                        fieldErrors = result.error.fieldErrors,
                    )
                }
            }
        }
    }
}
