package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CashBoxSummary
import ir.ahvaz.mehra.domain.model.DailyPoint
import ir.ahvaz.mehra.domain.model.DebtorItem
import ir.ahvaz.mehra.domain.model.DiscountCommand
import ir.ahvaz.mehra.domain.model.FinanceSummary
import ir.ahvaz.mehra.domain.model.MethodTotal
import ir.ahvaz.mehra.domain.model.NewTransactionCommand
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionFilter
import ir.ahvaz.mehra.domain.model.TransactionType
import ir.ahvaz.mehra.domain.repository.FinanceRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoFinanceRepository @Inject constructor(
    private val store: DemoStore,
) : FinanceRepository {

    override suspend fun transactions(filter: TransactionFilter): AppResult<CachedData<List<Transaction>>> {
        delay(150)
        val q = filter.query.trim()
        val digits = q.demoDigits()
        val list = store.transactionsBetween(filter.from, filter.to)
            .filter { filter.type == null || it.type == filter.type }
            .filter { filter.method == null || it.method == filter.method }
            .filter {
                q.isEmpty() ||
                    it.description.contains(q) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.guestName?.contains(q) == true ||
                    it.roomNumber?.contains(q) == true ||
                    (digits.isNotEmpty() && it.amountRial.toString().contains(digits))
            }
            .sortedByDescending { it.createdAt }
        return demoCached(list)
    }

    override suspend fun cashBox(date: LocalDate): AppResult<CachedData<CashBoxSummary>> {
        delay(150)
        val items = store.transactionsOn(date)
        val received = items.filter { it.type == TransactionType.RECEIVE }.sumOf { it.amountRial }
        val paid = items.filter { it.type == TransactionType.PAY || it.type == TransactionType.REFUND }
            .sumOf { it.amountRial }
        val discounts = items.filter { it.type == TransactionType.DISCOUNT }.sumOf { it.amountRial }
        val byMethod = items.filter { it.type == TransactionType.RECEIVE }
            .groupBy { it.method }
            .map { (method, list) -> MethodTotal(method, list.sumOf { it.amountRial }, list.size) }
            .sortedByDescending { it.amountRial }
        return demoCached(
            CashBoxSummary(
                date = date,
                receivedRial = received,
                paidRial = paid,
                discountsRial = discounts,
                netRial = received - paid,
                transactionCount = items.size,
                byMethod = byMethod,
                openBalanceRial = store.guests.sumOf { it.balanceRial },
            )
        )
    }

    override suspend fun summary(from: LocalDate, to: LocalDate): AppResult<FinanceSummary> {
        delay(180)
        val items = store.transactionsBetween(from, to)
        val received = items.filter { it.type == TransactionType.RECEIVE }.sumOf { it.amountRial }
        val paid = items.filter { it.type == TransactionType.PAY || it.type == TransactionType.REFUND }
            .sumOf { it.amountRial }
        val discounts = items.filter { it.type == TransactionType.DISCOUNT }.sumOf { it.amountRial }
        val byMethod = items.filter { it.type == TransactionType.RECEIVE }
            .groupBy { it.method }
            .map { (method, list) -> MethodTotal(method, list.sumOf { it.amountRial }, list.size) }
            .sortedByDescending { it.amountRial }
        val daily = mutableListOf<DailyPoint>()
        var day = from
        while (!day.isAfter(to)) {
            val dayItems = items.filter { store.localDateOf(it.createdAt) == day }
            daily += DailyPoint(
                date = day,
                receivedRial = dayItems.filter { it.type == TransactionType.RECEIVE }.sumOf { it.amountRial },
                paidRial = dayItems.filter {
                    it.type == TransactionType.PAY || it.type == TransactionType.REFUND
                }.sumOf { it.amountRial },
            )
            day = day.plusDays(1)
        }
        return demoOk(
            FinanceSummary(
                from = from,
                to = to,
                receivedRial = received,
                paidRial = paid,
                discountsRial = discounts,
                netRial = received - paid,
                byMethod = byMethod,
                dailySeries = daily,
            )
        )
    }

    override suspend fun debtors(): AppResult<List<DebtorItem>> {
        delay(140)
        val list = store.guests
            .filter { it.balanceRial > 0 }
            .sortedByDescending { it.balanceRial }
            .map { guest ->
                val stay = store.activeStays().firstOrNull { it.guestId == guest.id }
                DebtorItem(
                    guestId = guest.id,
                    guestName = guest.fullName,
                    phone = guest.phone,
                    roomNumber = stay?.roomNumber,
                    balanceRial = guest.balanceRial,
                    lastActivityAt = guest.lastStayAt,
                )
            }
        return demoOk(list)
    }

    override suspend fun register(command: NewTransactionCommand): AppResult<Transaction> {
        delay(260)
        if (command.amountRial <= 0L) return demoFailure("مبلغ تراکنش معتبر نیست.")
        if (command.description.isBlank()) return demoFailure("شرح تراکنش را وارد کنید.")
        val transaction = Transaction(
            id = store.nextId("tx"),
            code = "TR-${1100 + store.transactions.size}",
            type = command.type,
            amountRial = command.amountRial,
            method = command.method,
            description = command.description.trim(),
            guestName = command.guestName,
            roomNumber = store.activeStays().firstOrNull { it.guestId == command.guestId }?.roomNumber,
            operatorName = DemoStore.DEMO_OPERATOR,
            reservationId = command.reservationId,
            reference = command.reference,
            createdAt = Instant.now(),
        )
        store.transactions.add(0, transaction)
        command.guestId?.let { guestId ->
            val index = store.guests.indexOfFirst { it.id == guestId }
            if (index >= 0) {
                val guest = store.guests[index]
                val delta = when (command.type) {
                    TransactionType.RECEIVE -> -command.amountRial
                    TransactionType.PAY, TransactionType.REFUND -> command.amountRial
                    TransactionType.DISCOUNT -> -command.amountRial
                }
                store.guests[index] = guest.copy(balanceRial = (guest.balanceRial + delta).coerceAtLeast(0L))
            }
        }
        return demoOk(transaction)
    }

    override suspend fun applyDiscount(command: DiscountCommand): AppResult<Unit> {
        delay(220)
        if (command.amountRial <= 0L) return demoFailure("مبلغ تخفیف معتبر نیست.")
        if (command.reason.isBlank()) return demoFailure("دلیل تخفیف را وارد کنید.")
        val guest = command.guestId?.let { store.guest(it) }
        store.transactions.add(
            0,
            Transaction(
                id = store.nextId("tx"),
                code = "TR-${1100 + store.transactions.size}",
                type = TransactionType.DISCOUNT,
                amountRial = command.amountRial,
                method = ir.ahvaz.mehra.domain.model.PaymentMethod.CASH,
                description = "تخفیف — ${command.reason.trim()}",
                guestName = guest?.fullName,
                operatorName = DemoStore.DEMO_OPERATOR,
                reservationId = command.reservationId,
                createdAt = Instant.now(),
            ),
        )
        return AppResult.Success(Unit)
    }
}
