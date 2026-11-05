package ir.ahvaz.mehra.domain.model

import java.time.Instant
import java.time.LocalDate

data class Transaction(
    val id: String,
    val code: String,
    val type: TransactionType,
    val amountRial: Long,
    val method: PaymentMethod,
    val description: String,
    val guestName: String? = null,
    val roomNumber: String? = null,
    val operatorName: String? = null,
    val reservationId: String? = null,
    val reference: String? = null,
    val createdAt: Instant? = null,
    val isReversed: Boolean = false,
)

data class MethodTotal(
    val method: PaymentMethod,
    val amountRial: Long,
    val count: Int,
)

data class CashBoxSummary(
    val date: LocalDate,
    val receivedRial: Long = 0L,
    val paidRial: Long = 0L,
    val discountsRial: Long = 0L,
    val netRial: Long = 0L,
    val transactionCount: Int = 0,
    val byMethod: List<MethodTotal> = emptyList(),
    val openBalanceRial: Long = 0L,
)

data class DailyPoint(
    val date: LocalDate,
    val receivedRial: Long,
    val paidRial: Long,
)

data class FinanceSummary(
    val from: LocalDate,
    val to: LocalDate,
    val receivedRial: Long,
    val paidRial: Long,
    val discountsRial: Long,
    val netRial: Long,
    val byMethod: List<MethodTotal>,
    val dailySeries: List<DailyPoint>,
)

data class DebtorItem(
    val guestId: String,
    val guestName: String,
    val phone: String? = null,
    val roomNumber: String? = null,
    val balanceRial: Long,
    val lastActivityAt: Instant? = null,
)

data class TransactionFilter(
    val from: LocalDate,
    val to: LocalDate,
    val type: TransactionType? = null,
    val method: PaymentMethod? = null,
    val query: String = "",
)

data class NewTransactionCommand(
    val type: TransactionType,
    val amountRial: Long,
    val method: PaymentMethod,
    val description: String,
    val guestId: String? = null,
    val guestName: String? = null,
    val reservationId: String? = null,
    val reference: String? = null,
)

data class DiscountCommand(
    val guestId: String? = null,
    val reservationId: String? = null,
    val amountRial: Long,
    val reason: String,
)
