package ir.ahvaz.mehra.feature.guests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.PLATE_LENGTH
import ir.ahvaz.mehra.core.common.normalizePlate
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.common.Validators
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.Companion
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.GuestDetail
import ir.ahvaz.mehra.domain.model.GuestDraft
import ir.ahvaz.mehra.domain.model.ReservationDraft
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.GuestRepository
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuestsUiState(
    val guests: UiState<List<Guest>> = UiState(),
    val query: String = "",
    val canManage: Boolean = false,
    val canViewPii: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
)

@HiltViewModel
class GuestsViewModel @Inject constructor(
    private val guestRepository: GuestRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(GuestsUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<GuestsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update {
                    it.copy(
                        canManage = user?.can(Permissions.GUESTS_MANAGE) == true,
                        canViewPii = user?.can(Permissions.GUESTS_PII_VIEW) == true,
                    )
                }
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

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(guests = it.guests.copy(isLoading = it.guests.data == null, isRefreshing = it.guests.data != null))
        }
        viewModelScope.launch {
            val result = guestRepository.guests(_state.value.query)
            _state.update { it.copy(guests = it.guests.applyResult(result)) }
        }
    }
}

data class GuestDetailUiState(
    val detail: UiState<GuestDetail> = UiState(),
    val canViewPii: Boolean = false,
    val revealNationalId: Boolean = false,
)

@HiltViewModel
class GuestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepository: GuestRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val guestId: String = savedStateHandle.get<String>("guestId").orEmpty()

    private val _state = MutableStateFlow(GuestDetailUiState())
    val state: StateFlow<GuestDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canViewPii = user?.can(Permissions.GUESTS_PII_VIEW) == true) }
            }
        }
        refresh()
    }

    fun toggleNationalIdVisibility() {
        if (_state.value.canViewPii) {
            _state.update { it.copy(revealNationalId = !it.revealNationalId) }
        }
    }

    fun refresh() {
        _state.update { it.copy(detail = it.detail.copy(isLoading = it.detail.data == null)) }
        viewModelScope.launch {
            val result = guestRepository.guest(guestId)
            _state.update { it.copy(detail = it.detail.applyValue(result)) }
        }
    }
}

data class GuestFormUiState(
    val draft: GuestDraft = GuestDraft(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val saved: Boolean = false,
    // v2.2.0: smart stay assignment from the guest form
    val rooms: List<Room> = emptyList(),
    val vehicleSearch: String = "",
)

@HiltViewModel
class GuestFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guestRepository: GuestRepository,
    private val roomRepository: RoomRepository,
    private val reservationRepository: ReservationRepository,
) : ViewModel() {

    private val guestId: String? = savedStateHandle.get<String>("guestId")?.takeIf { it.isNotBlank() && it != "null" }

    private val _state = MutableStateFlow(GuestFormUiState())
    val state: StateFlow<GuestFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = roomRepository.rooms()) {
                is AppResult.Success -> _state.update {
                    it.copy(rooms = result.data.data.filter { room -> room.status == RoomStatus.FREE }
                        .sortedBy { room -> room.roomNumber.toIntOrNull() ?: 999 })
                }
                is AppResult.Failure -> Unit
            }
        }
        if (guestId != null) {
            viewModelScope.launch {
                when (val result = guestRepository.guest(guestId)) {
                    is AppResult.Success -> {
                        val guest = result.data.guest
                        _state.update {
                            it.copy(
                                draft = GuestDraft(
                                    id = guest.id,
                                    firstName = guest.firstName,
                                    lastName = guest.lastName,
                                    nationalId = guest.nationalId.orEmpty(),
                                    phone = guest.phone.orEmpty(),
                                    carPlate = guest.carPlate.orEmpty(),
                                    vehicleType = guest.vehicleType.orEmpty(),
                                    vehicleColor = guest.vehicleColor.orEmpty(),
                                    city = guest.city.orEmpty(),
                                    address = guest.address.orEmpty(),
                                    notes = guest.notes.orEmpty(),
                                    companions = guest.companions,
                                )
                            )
                        }
                    }

                    is AppResult.Failure -> _state.update { it.copy(errorMessage = result.error.userMessageFa) }
                }
            }
        }
    }

    fun updateDraft(transform: (GuestDraft) -> GuestDraft) {
        _state.update { it.copy(draft = transform(it.draft), fieldErrors = emptyMap(), errorMessage = null) }
    }

    fun addCompanion() {
        _state.update {
            it.copy(draft = it.draft.copy(companions = it.draft.companions + Companion(fullName = "")))
        }
    }

    fun updateCompanion(index: Int, transform: (Companion) -> Companion) {
        _state.update {
            val updated = it.draft.companions.toMutableList()
            if (index in updated.indices) updated[index] = transform(updated[index])
            it.copy(draft = it.draft.copy(companions = updated))
        }
    }

    fun removeCompanion(index: Int) {
        _state.update {
            val updated = it.draft.companions.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            it.copy(draft = it.draft.copy(companions = updated))
        }
    }

    fun save() {
        val draft = _state.value.draft
        val fields = mutableMapOf<String, String>()
        if (draft.firstName.isBlank()) fields["firstName"] = "نام الزامی است."
        if (draft.lastName.isBlank()) fields["lastName"] = "نام خانوادگی الزامی است."
        if (draft.nationalId.isNotBlank() && !Validators.isValidIranNationalId(draft.nationalId)) {
            fields["nationalId"] = "کد ملی معتبر نیست."
        }
        val plate = normalizePlate(draft.carPlate)
        if (draft.carPlate.isNotBlank() && plate.length != PLATE_LENGTH) {
            fields["carPlate"] = "پلاک را کامل وارد کنید (۸ حرف/رقم؛ مثال: ۱۲ب۳۴۵۶۷)."
        }
        if (draft.phone.isNotBlank() && !Validators.isValidIranMobile(draft.phone)) {
            fields["phone"] = "شماره تماس معتبر نیست (مثال: ۰۹۱۲۳۴۵۶۷۸۹)."
        }
        val companions = draft.companions.filter { it.fullName.isNotBlank() }
        if (fields.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = fields, errorMessage = "اطلاعات مهمان را کامل کنید.") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = guestRepository.save(draft.copy(companions = companions, carPlate = plate))) {
                is AppResult.Success -> {
                    // v2.2.0: optional smart stay assignment (room + nights + discount)
                    val guest = result.data
                    val roomId = draft.roomId
                    val nights = draft.stayNights.toLatinDigits().toIntOrNull() ?: 0
                    if (roomId.isBlank() || nights <= 0) {
                        _state.update { it.copy(isSaving = false, saved = true) }
                    } else {
                        val checkIn = JalaliDate.today()
                        val resDraft = ReservationDraft(
                            guestId = guest.id,
                            guestName = guest.fullName,
                            roomId = roomId,
                            roomNumber = _state.value.rooms.firstOrNull { it.id == roomId }?.roomNumber,
                            checkIn = checkIn,
                            checkOut = checkIn.plusDays(nights.toLong()),
                            guestsCount = "1",
                            discountRial = draft.discountRial,
                            notes = "تخصیص اتاق از فرم معرفی مهمان",
                        )
                        when (val rr = reservationRepository.create(resDraft)) {
                            is AppResult.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                            is AppResult.Failure -> _state.update {
                                // keep the guest id on the draft so a retry UPDATES instead of duplicating
                                it.copy(
                                    isSaving = false,
                                    draft = it.draft.copy(id = guest.id),
                                    errorMessage = "مهمان ثبت شد؛ تخصیص اتاق ناموفق بود: " + rr.error.userMessageFa,
                                )
                            }
                        }
                    }
                }
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

    fun onVehicleSearchChange(value: String) {
        _state.update { it.copy(vehicleSearch = value) }
    }
}
