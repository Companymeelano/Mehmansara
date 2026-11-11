package ir.ahvaz.mehra.feature.reception

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.export.ReportExporter
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.CheckInCommand
import ir.ahvaz.mehra.domain.model.CheckOutCommand
import ir.ahvaz.mehra.domain.model.Companion
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.ReceptionRepository
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceptionUiState(
    val stays: UiState<List<Stay>> = UiState(),
    val searchQuery: String = "",
    val searchResults: List<Reservation> = emptyList(),
    val isSearching: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val canOperate: Boolean = false,
    val actionMessage: String? = null,
    val rooms: List<ir.ahvaz.mehra.domain.model.Room> = emptyList(),
)

@HiltViewModel
class ReceptionViewModel @Inject constructor(
    private val receptionRepository: ReceptionRepository,
    private val reservationRepository: ReservationRepository,
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    private val exporter: ReportExporter,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceptionUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<ReceptionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update { it.copy(canOperate = user?.can(Permissions.RECEPTION_OPERATE) == true) }
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

    fun refresh() {
        _state.update {
            it.copy(stays = it.stays.copy(isLoading = it.stays.data == null, isRefreshing = it.stays.data != null))
        }
        viewModelScope.launch {
            val result = receptionRepository.activeStays()
            _state.update { it.copy(stays = it.stays.applyResult(result)) }
        }
    }

    fun onSearchChange(value: String) {
        _state.update { it.copy(searchQuery = value) }
        if (value.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        _state.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            val result = receptionRepository.searchReservations(value)
            _state.update {
                when (result) {
                    is AppResult.Success -> it.copy(searchResults = result.data, isSearching = false)
                    is AppResult.Failure -> it.copy(isSearching = false, actionMessage = result.error.userMessageFa)
                }
            }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            when (val result = roomRepository.rooms()) {
                is AppResult.Success -> _state.update { it.copy(rooms = result.data.data) }
                is AppResult.Failure -> _state.update { it.copy(actionMessage = result.error.userMessageFa) }
            }
        }
    }

    fun extendStay(stayId: String, newCheckOut: java.time.LocalDate) {
        _state.update { it.copy(actionMessage = null) }
        viewModelScope.launch {
            val result = receptionRepository.extendStay(stayId, newCheckOut)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "تمدید اقامت ثبت شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
        }
    }

    fun moveRoom(stayId: String, newRoomId: String, reason: String?) {
        _state.update { it.copy(actionMessage = null) }
        viewModelScope.launch {
            val result = receptionRepository.moveRoom(stayId, newRoomId, reason)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "اتاق مهمان تغییر کرد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
        }
    }

    /** Output of a stay: formatted PDF receipt (with info) or raw JSON record. */
    fun exportStayOutputs(stayId: String, raw: Boolean) {
        viewModelScope.launch {
            when (val stayRes = receptionRepository.stay(stayId)) {
                is AppResult.Success -> {
                    val stay = stayRes.data
                    runCatching {
                        val receiptRes = receptionRepository.receipt(stayId)
                        val receipt = (receiptRes as? AppResult.Success)?.data
                        if (raw) {
                            val file = exporter.exportStayRawJson(stay, receipt)
                            exporter.share(file, ReportExporter.MIME_JSON, "خروجی خام اقامت ${stay.reservationCode}")
                        } else {
                            when (receiptRes) {
                                is AppResult.Success -> {
                                    val file = exporter.receiptPdf(receiptRes.data)
                                    exporter.share(file, ReportExporter.MIME_PDF, "فاکتور ${receiptRes.data.receiptNumber}")
                                }
                                is AppResult.Failure ->
                                    _state.update { it.copy(actionMessage = receiptRes.error.userMessageFa) }
                            }
                        }
                    }.onFailure {
                        _state.update { it.copy(actionMessage = "ساخت خروجی ناموفق بود: ${it.message}") }
                    }
                }
                is AppResult.Failure -> _state.update { it.copy(actionMessage = stayRes.error.userMessageFa) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}

data class CheckInUiState(
    val reservation: Reservation? = null,
    val rooms: List<ir.ahvaz.mehra.domain.model.Room> = emptyList(),
    val documentTypes: List<String> = emptyList(),
    val selectedRoomId: String? = null,
    val companions: List<Companion> = emptyList(),
    val selectedDocuments: Set<String> = emptySet(),
    val initialPayment: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val pendingConfirmation: Boolean = false,
    val completedStay: Stay? = null,
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receptionRepository: ReceptionRepository,
    private val reservationRepository: ReservationRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val reservationId: String = savedStateHandle.get<String>("reservationId").orEmpty()

    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    private var reservation: Reservation? = null

    init {
        viewModelScope.launch {
            when (val result = reservationRepository.get(reservationId)) {
                is AppResult.Success -> {
                    reservation = result.data
                    _state.update {
                        it.copy(
                            reservation = result.data,
                            selectedRoomId = result.data.roomId,
                            companions = result.data.companions,
                        )
                    }
                    loadRooms(result.data)
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.userMessageFa)
                }
            }
            when (val result = receptionRepository.documentTypes()) {
                is AppResult.Success -> _state.update { it.copy(documentTypes = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadRooms(reservation: Reservation) {
        viewModelScope.launch {
            val result = roomRepository.rooms()
            _state.update { current ->
                when (result) {
                    is AppResult.Success -> {
                        val available = result.data.data.filter {
                            it.status == ir.ahvaz.mehra.domain.model.RoomStatus.FREE ||
                                it.id == reservation.roomId
                        }
                        current.copy(rooms = available, isLoading = false)
                    }

                    is AppResult.Failure -> current.copy(isLoading = false, errorMessage = result.error.userMessageFa)
                }
            }
        }
    }

    fun selectRoom(roomId: String) {
        _state.update { it.copy(selectedRoomId = roomId) }
    }

    fun updateInitialPayment(value: String) {
        _state.update { it.copy(initialPayment = value) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun toggleDocument(document: String) {
        _state.update {
            val documents = it.selectedDocuments.toMutableSet()
            if (!documents.add(document)) documents.remove(document)
            it.copy(selectedDocuments = documents)
        }
    }

    fun addCompanion() {
        _state.update { it.copy(companions = it.companions + Companion(fullName = "")) }
    }

    fun updateCompanion(index: Int, transform: (Companion) -> Companion) {
        _state.update {
            val updated = it.companions.toMutableList()
            if (index in updated.indices) updated[index] = transform(updated[index])
            it.copy(companions = updated)
        }
    }

    fun removeCompanion(index: Int) {
        _state.update {
            val updated = it.companions.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            it.copy(companions = updated)
        }
    }

    fun updateNote(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun submit() {
        val roomId = _state.value.selectedRoomId
        if (roomId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "اتاق تخصیصی را انتخاب کنید.") }
            return
        }
        val payment = parseLongInput(_state.value.initialPayment.toLatinDigits())
        _state.update { it.copy(isSubmitting = true, errorMessage = null, pendingConfirmation = false) }
        viewModelScope.launch {
            val command = CheckInCommand(
                reservationId = reservationId,
                roomId = roomId,
                companions = _state.value.companions.filter { it.fullName.isNotBlank() },
                documentTypes = _state.value.selectedDocuments.toList(),
                initialPaymentRial = payment,
                paymentMethod = _state.value.paymentMethod,
                note = _state.value.note.takeIf { it.isNotBlank() },
            )
            when (val result = receptionRepository.checkIn(command)) {
                is AppResult.Success -> _state.update {
                    it.copy(isSubmitting = false, completedStay = result.data)
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.error.userMessageFa,
                        pendingConfirmation = result.error.kind ==
                            ir.ahvaz.mehra.core.common.ErrorKind.QUEUED_OFFLINE,
                    )
                }
            }
        }
    }
}

data class CheckOutUiState(
    val stay: Stay? = null,
    val finalPayment: String = "",
    val discount: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val moveToCleaning: Boolean = true,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val pendingConfirmation: Boolean = false,
    val receipt: Receipt? = null,
)

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receptionRepository: ReceptionRepository,
) : ViewModel() {

    private val stayId: String = savedStateHandle.get<String>("stayId").orEmpty()

    private val _state = MutableStateFlow(CheckOutUiState())
    val state: StateFlow<CheckOutUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = it.stay == null) }
        viewModelScope.launch {
            when (val result = receptionRepository.stay(stayId)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        stay = result.data,
                        isLoading = false,
                        finalPayment = result.data.balanceRial.coerceAtLeast(0).toString(),
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.userMessageFa)
                }
            }
        }
    }

    fun onFinalPaymentChange(value: String) {
        _state.update { it.copy(finalPayment = value) }
    }

    fun onDiscountChange(value: String) {
        _state.update { it.copy(discount = value) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun onMoveToCleaningChange(value: Boolean) {
        _state.update { it.copy(moveToCleaning = value) }
    }

    fun submit() {
        _state.update { it.copy(isSubmitting = true, errorMessage = null, pendingConfirmation = false) }
        viewModelScope.launch {
            val command = CheckOutCommand(
                stayId = stayId,
                finalPaymentRial = parseLongInput(_state.value.finalPayment.toLatinDigits()) ?: 0L,
                paymentMethod = _state.value.paymentMethod,
                discountRial = parseLongInput(_state.value.discount.toLatinDigits()) ?: 0L,
                note = _state.value.note.takeIf { it.isNotBlank() },
                moveRoomToCleaning = _state.value.moveToCleaning,
            )
            when (val result = receptionRepository.checkOut(command)) {
                is AppResult.Success -> _state.update {
                    it.copy(isSubmitting = false, receipt = result.data, stay = null)
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.error.userMessageFa,
                        pendingConfirmation = result.error.kind ==
                            ir.ahvaz.mehra.core.common.ErrorKind.QUEUED_OFFLINE,
                    )
                }
            }
        }
    }
}
