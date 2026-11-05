package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CashBoxSummary
import ir.ahvaz.mehra.domain.model.DebtorItem
import ir.ahvaz.mehra.domain.model.DiscountCommand
import ir.ahvaz.mehra.domain.model.FinanceSummary
import ir.ahvaz.mehra.domain.model.NewTransactionCommand
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionFilter
import java.time.LocalDate

interface FinanceRepository {
    suspend fun transactions(filter: TransactionFilter): AppResult<CachedData<List<Transaction>>>
    suspend fun cashBox(date: LocalDate): AppResult<CachedData<CashBoxSummary>>
    suspend fun summary(from: LocalDate, to: LocalDate): AppResult<FinanceSummary>
    suspend fun debtors(): AppResult<List<DebtorItem>>
    suspend fun register(command: NewTransactionCommand): AppResult<Transaction>
    suspend fun applyDiscount(command: DiscountCommand): AppResult<Unit>
}
