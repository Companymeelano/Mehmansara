package ir.ahvaz.mehra.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.date.toJalali
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.domain.model.CashBoxSummary
import ir.ahvaz.mehra.domain.model.DebtorItem
import ir.ahvaz.mehra.domain.model.DiscountCommand
import ir.ahvaz.mehra.domain.model.FinanceSummary
import ir.ahvaz.mehra.domain.model.NewTransactionCommand
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionFilter
import ir.ahvaz.mehra.domain.model.TransactionType
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceUiState(
    val from: JalaliDate = JalaliDate.today().let { JalaliDate(it.year, it.month, 1) },
    val to: JalaliDate = JalaliDate.today(),
    val transactions: UiState<List<Transaction>> = UiState(),
    val cashBox: UiState<CashBoxSummary> = UiState(),
    val summary: UiState<FinanceSummary> = UiState(),
    val debtors: List<DebtorItem> = emptyList(),
    val typeFilter: TransactionType? = null,
    val query: String = "",
    val canView: Boolean = false,
    val canOperate: Boolean = false,
    val pendingOperations: Int = 0,
    val isOnline: Boolean = true,
    val actionMessage: String? = null,
    val isRegistering: Boolean = false,
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val authRepository: AuthRepository,
    private val offlineQueue: OfflineQueue,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<FinanceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update {
                    it.copy(
                        canView = user?.can(Permissions.FINANCE_VIEW) == true,
                        canOperate = user?.can(Permissions.FINANCE_OPERATE) == true,
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

    fun onRangeChange(from: JalaliDate, to: JalaliDate) {
        if (to < from) {
            _state.update { it.copy(actionMessage = "بازه تاریخ نامعتبر است.") }
            return
        }
        _state.update { it.copy(from = from, to = to) }
        refresh()
    }

    fun onTypeFilterChange(type: TransactionType?) {
        _state.update { it.copy(typeFilter = type) }
        refreshTransactions()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        refreshTransactions()
    }

    fun refresh() {
        refreshTransactions()
        refreshCashBox()
        refreshSummary()
        refreshDebtors()
    }

    private fun refreshTransactions() {
        _state.update {
            it.copy(
                transactions = it.transactions.copy(
                    isLoading = it.transactions.data == null,
                    isRefreshing = it.transactions.data != null,
                )
            )
        }
        viewModelScope.launch {
            val current = _state.value
            val result = financeRepository.transactions(
                TransactionFilter(
                    from = current.from.toGregorian(),
                    to = current.to.toGregorian(),
                    type = current.typeFilter,
                    query = current.query,
                )
            )
            _state.update { it.copy(transactions = it.transactions.applyResult(result)) }
        }
    }

    private fun refreshCashBox() {
        viewModelScope.launch {
            val result = financeRepository.cashBox(JalaliDate.today().toGregorian())
            _state.update { state ->
                when (result) {
                    is AppResult.Success -> state.copy(cashBox = state.cashBox.applyResult(result))
                    is AppResult.Failure -> state.copy(
                        cashBox = state.cashBox.copy(error = result.error, isLoading = false)
                    )
                }
            }
        }
    }

    private fun refreshSummary() {
        viewModelScope.launch {
            val current = _state.value
            val result = financeRepository.summary(current.from.toGregorian(), current.to.toGregorian())
            _state.update { it.copy(summary = it.summary.applyValue(result)) }
        }
    }

    private fun refreshDebtors() {
        viewModelScope.launch {
            when (val result = financeRepository.debtors()) {
                is AppResult.Success -> _state.update { it.copy(debtors = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun registerTransaction(
        type: TransactionType,
        amount: String,
        method: PaymentMethod,
        description: String,
        guestName: String?,
        reference: String?,
    ) {
        val parsed = parseLongInput(amount.toLatinDigits())
        if (parsed == null || parsed <= 0L) {
            _state.update { it.copy(actionMessage = "مبلغ تراکنش را به‌درستی وارد کنید.") }
            return
        }
        if (description.isBlank()) {
            _state.update { it.copy(actionMessage = "شرح تراکنش الزامی است.") }
            return
        }
        _state.update { it.copy(isRegistering = true, actionMessage = null) }
        viewModelScope.launch {
            val result = financeRepository.register(
                NewTransactionCommand(
                    type = type,
                    amountRial = parsed,
                    method = method,
                    description = description,
                    guestName = guestName?.takeIf { it.isNotBlank() },
                    reference = reference?.takeIf { it.isNotBlank() },
                )
            )
            _state.update {
                it.copy(
                    isRegistering = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "تراکنش با کد ${result.data.code} ثبت شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            refresh()
        }
    }

    fun applyDiscount(amount: String, reason: String, guestId: String?) {
        val parsed = parseLongInput(amount.toLatinDigits())
        if (parsed == null || parsed <= 0L) {
            _state.update { it.copy(actionMessage = "مبلغ تخفیف را وارد کنید.") }
            return
        }
        if (reason.isBlank()) {
            _state.update { it.copy(actionMessage = "دلیل تخفیف الزامی است.") }
            return
        }
        viewModelScope.launch {
            val result = financeRepository.applyDiscount(
                DiscountCommand(
                    guestId = guestId?.takeIf { it.isNotBlank() },
                    amountRial = parsed,
                    reason = reason,
                )
            )
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "تخفیف ثبت شد."
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
