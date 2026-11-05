package ir.ahvaz.mehra.feature.reservations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.AvailabilityQuery
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationDraft
import ir.ahvaz.mehra.domain.model.ReservationQuote
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.WaitlistEntry
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.GuestRepository
import ir.ahvaz.mehra.domain.repository.ReservationFilter
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReservationsUiState(
    val reservations: UiState<List<Reservation>> = UiState(),
    val waitlist: List<WaitlistEntry> = emptyList(),
    val query: String = "",
    val status: ReservationStatus? = null,
    val canManage: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val actionMessage: String? = null,
)

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<ReservationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.RESERVATIONS_MANAGE) == true) }
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
        loadWaitlist()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        refresh()
    }

    fun onStatusChange(status: ReservationStatus?) {
        _state.update { it.copy(status = status) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(
                reservations = it.reservations.copy(
                    isLoading = it.reservations.data == null,
                    isRefreshing = it.reservations.data != null,
                )
            )
        }
        viewModelScope.launch {
            val current = _state.value
            val result = reservationRepository.list(
                ReservationFilter(query = current.query, status = current.status)
            )
            _state.update { it.copy(reservations = it.reservations.applyResult(result)) }
        }
    }

    private fun loadWaitlist() {
        viewModelScope.launch {
            when (val result = reservationRepository.waitlist()) {
                is AppResult.Success -> _state.update { it.copy(waitlist = result.data.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun confirm(reservation: Reservation) {
        viewModelScope.launch {
            val result = reservationRepository.confirm(reservation.id)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "رزرو ${reservation.code} تأیید شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
        }
    }

    fun cancel(reservation: Reservation, reason: String?) {
        viewModelScope.launch {
            val result = reservationRepository.cancel(reservation.id, reason)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "رزرو ${reservation.code} لغو شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}

data class NewReservationUiState(
    val draft: ReservationDraft = ReservationDraft(
        checkIn = JalaliDate.today(),
        checkOut = JalaliDate.today().plusDays(1),
    ),
    val guests: List<Guest> = emptyList(),
    val guestSearch: String = "",
    val availableRooms: List<AvailableRoom> = emptyList(),
    val quote: ReservationQuote? = null,
    val isAvailabilityLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val createdReservation: Reservation? = null,
    val pendingConfirmation: Boolean = false,
)

@HiltViewModel
class NewReservationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reservationRepository: ReservationRepository,
    private val guestRepository: GuestRepository,
) : ViewModel() {

    private val prefillGuestId: String? = savedStateHandle.get<String>("guestId")
        ?.takeIf { it.isNotBlank() && it != "null" }

    private val _state = MutableStateFlow(
        NewReservationUiState(
            draft = ReservationDraft(
                guestId = prefillGuestId,
                checkIn = JalaliDate.today(),
                checkOut = JalaliDate.today().plusDays(1),
            )
        )
    )
    val state: StateFlow<NewReservationUiState> = _state.asStateFlow()

    init {
        searchGuests("")
        refreshAvailability()
    }

    fun updateDraft(transform: (ReservationDraft) -> ReservationDraft) {
        _state.update { it.copy(draft = transform(it.draft), fieldErrors = emptyMap(), errorMessage = null) }
        refreshQuote()
    }

    fun onGuestSearchChange(value: String) {
        _state.update { it.copy(guestSearch = value) }
        searchGuests(value)
    }

    private fun searchGuests(query: String) {
        viewModelScope.launch {
            when (val result = guestRepository.quickSearch(query)) {
                is AppResult.Success -> {
                    val guests = result.data
                    _state.update { current ->
                        val prefilled = current.draft.guestId?.let { id ->
                            guests.firstOrNull { it.id == id }
                        }
                        current.copy(
                            guests = guests,
                            draft = if (prefilled != null && current.draft.guestName.isBlank()) {
                                current.draft.copy(guestName = prefilled.fullName)
                            } else {
                                current.draft
                            },
                        )
                    }
                }

                is AppResult.Failure -> Unit
            }
        }
    }

    fun selectGuest(guest: Guest) {
        _state.update { it.copy(draft = it.draft.copy(guestId = guest.id, guestName = guest.fullName)) }
        refreshQuote()
    }

    fun refreshAvailability() {
        val draft = _state.value.draft
        val checkIn = draft.checkIn?.toGregorian() ?: return
        val checkOut = draft.checkOut?.toGregorian() ?: return
        if (checkOut <= checkIn) return
        _state.update { it.copy(isAvailabilityLoading = true) }
        viewModelScope.launch {
            val result = reservationRepository.availability(
                AvailabilityQuery(
                    checkIn = checkIn,
                    checkOut = checkOut,
                    guestsCount = parseLongInput(draft.guestsCount)?.toInt() ?: 1,
                )
            )
            _state.update { current ->
                when (result) {
                    is AppResult.Success -> current.copy(
                        availableRooms = result.data,
                        isAvailabilityLoading = false,
                    )

                    is AppResult.Failure -> current.copy(
                        isAvailabilityLoading = false,
                        errorMessage = result.error.userMessageFa,
                    )
                }
            }
        }
    }

    private fun refreshQuote() {
        val current = _state.value
        val draft = current.draft
        val roomId = draft.roomId ?: return
        val checkIn = draft.checkIn?.toGregorian() ?: return
        val checkOut = draft.checkOut?.toGregorian() ?: return
        if (checkOut <= checkIn) return
        viewModelScope.launch {
            val discount = parseLongInput(draft.discountRial.toLatinDigits()) ?: 0L
            val result = reservationRepository.quote(
                roomId = roomId,
                checkIn = checkIn,
                checkOut = checkOut,
                guestsCount = parseLongInput(draft.guestsCount)?.toInt() ?: 1,
                discountRial = discount,
            )
            _state.update { state ->
                when (result) {
                    is AppResult.Success -> state.copy(quote = result.data)
                    is AppResult.Failure -> state.copy(quote = null)
                }
            }
        }
    }

    fun submit() {
        val draft = _state.value.draft
        val fields = mutableMapOf<String, String>()
        if (draft.guestId.isNullOrBlank()) fields["guestId"] = "مهمان را انتخاب کنید."
        if (draft.checkIn == null) fields["checkIn"] = "تاریخ ورود را انتخاب کنید."
        if (draft.checkOut == null) fields["checkOut"] = "تاریخ خروج را انتخاب کنید."
        if (draft.checkIn != null && draft.checkOut != null && draft.checkOut <= draft.checkIn) {
            fields["checkOut"] = "تاریخ خروج باید بعد از ورود باشد."
        }
        if (!draft.joinWaitlist && draft.roomId.isNullOrBlank()) fields["roomId"] = "اتاق را انتخاب کنید."
        if (fields.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = fields, errorMessage = "اطلاعات رزرو کامل نیست.") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = reservationRepository.create(draft)) {
                is AppResult.Success -> {
                    // Ensure we clear the search field so the next reservation starts blank.
                    _state.update { it.copy(isSaving = false, createdReservation = result.data) }
                }

                is AppResult.Failure -> {
                    val error = result.error
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.userMessageFa,
                            fieldErrors = error.fieldErrors,
                            pendingConfirmation = error.kind == ir.ahvaz.mehra.core.common.ErrorKind.QUEUED_OFFLINE,
                        )
                    }
                }
            }
        }
    }
}

data class ReservationDetailUiState(
    val reservation: UiState<Reservation> = UiState(),
    val canManage: Boolean = false,
    val actionMessage: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class ReservationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reservationRepository: ReservationRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val reservationId: String = savedStateHandle.get<String>("reservationId").orEmpty()

    private val _state = MutableStateFlow(ReservationDetailUiState())
    val state: StateFlow<ReservationDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canManage = user?.can(Permissions.RESERVATIONS_MANAGE) == true) }
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(reservation = it.reservation.copy(isLoading = it.reservation.data == null)) }
        viewModelScope.launch {
            val result = reservationRepository.get(reservationId)
            _state.update { it.copy(reservation = it.reservation.applyValue(result)) }
        }
    }

    fun confirm() {
        action { reservationRepository.confirm(reservationId) to "رزرو تأیید شد." }
    }

    fun cancel(reason: String?) {
        action { reservationRepository.cancel(reservationId, reason) to "رزرو لغو شد." }
    }

    private fun action(block: suspend () -> Pair<AppResult<Unit>, String>) {
        _state.update { it.copy(actionInProgress = true, actionMessage = null) }
        viewModelScope.launch {
            val (result, successMessage) = block()
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
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
