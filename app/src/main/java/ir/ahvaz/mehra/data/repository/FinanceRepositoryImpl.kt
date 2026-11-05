package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.FinanceApi
import ir.ahvaz.mehra.data.remote.dto.CashBoxDto
import ir.ahvaz.mehra.data.remote.dto.DebtorDto
import ir.ahvaz.mehra.data.remote.dto.DiscountRequestDto
import ir.ahvaz.mehra.data.remote.dto.FinanceSummaryDto
import ir.ahvaz.mehra.data.remote.dto.NewTransactionRequestDto
import ir.ahvaz.mehra.data.remote.dto.TransactionDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OperationType
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.CashBoxSummary
import ir.ahvaz.mehra.domain.model.DebtorItem
import ir.ahvaz.mehra.domain.model.DiscountCommand
import ir.ahvaz.mehra.domain.model.FinanceSummary
import ir.ahvaz.mehra.domain.model.NewTransactionCommand
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionFilter
import ir.ahvaz.mehra.domain.repository.FinanceRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val api: FinanceApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), FinanceRepository {

    private val transactionListType = object : TypeToken<List<TransactionDto>>() {}.type
    private val debtorListType = object : TypeToken<List<DebtorDto>>() {}.type

    override suspend fun transactions(filter: TransactionFilter): AppResult<CachedData<List<Transaction>>> {
        val from = IsoFormat.date(filter.from)
        val to = IsoFormat.date(filter.to)
        val key = "finance:tx:$from:$to:${filter.type?.wire.orEmpty()}:${filter.method?.wire.orEmpty()}:${filter.query.trim()}"
        return readThrough(key, transactionListType) {
            executor.value {
                api.transactions(
                    from = from,
                    to = to,
                    type = filter.type?.wire,
                    method = filter.method?.wire,
                    search = filter.query.trim().takeIf { it.isNotEmpty() },
                    page = 1,
                    pageSize = 200,
                )
            }.map { it.items.orEmpty() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun cashBox(date: LocalDate): AppResult<CachedData<CashBoxSummary>> {
        val dateParam = IsoFormat.date(date)
        return readThrough("finance:cashbox:$dateParam", CashBoxDto::class.java) {
            executor.value { api.cashBox(dateParam) }
        }.mapCached { it.toDomain() }
    }

    override suspend fun summary(from: LocalDate, to: LocalDate): AppResult<FinanceSummary> {
        val fromParam = IsoFormat.date(from)
        val toParam = IsoFormat.date(to)
        val result = readThrough("finance:summary:$fromParam:$toParam", FinanceSummaryDto::class.java) {
            executor.value { api.summary(fromParam, toParam) }
        }.mapCached { it.toDomain() }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun debtors(): AppResult<List<DebtorItem>> {
        val result = readThrough("finance:debtors", debtorListType) {
            executor.value { api.debtors() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun register(command: NewTransactionCommand): AppResult<Transaction> {
        if (command.amountRial <= 0) {
            return AppResult.Failure(
                AppError.validation("مبلغ تراکنش باید بزرگ‌تر از صفر باشد.", mapOf("amountRial" to "الزامی"))
            )
        }
        if (command.description.isBlank()) {
            return AppResult.Failure(
                AppError.validation("شرح تراکنش الزامی است.", mapOf("description" to "الزامی"))
            )
        }
        val body = NewTransactionRequestDto(
            type = command.type.wire,
            amountRial = command.amountRial,
            method = command.method.wire,
            description = command.description.trim(),
            guestId = command.guestId,
            reservationId = command.reservationId,
            reference = command.reference,
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.value { api.register(idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("finance:")
                cache.invalidatePrefix("dashboard:")
                AppResult.Success(result.data.toDomain())
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.FINANCE_TRANSACTION,
                method = "POST",
                path = "/api/v1/finance/transactions",
                body = body,
                summaryFa = "${command.type.labelFa} مبلغ برای ${command.description}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    override suspend fun applyDiscount(command: DiscountCommand): AppResult<Unit> {
        if (command.amountRial <= 0) {
            return AppResult.Failure(AppError.validation("مبلغ تخفیف باید بزرگ‌تر از صفر باشد."))
        }
        if (command.reason.isBlank()) {
            return AppResult.Failure(AppError.validation("دلیل تخفیف الزامی است."))
        }
        val body = DiscountRequestDto(
            guestId = command.guestId,
            reservationId = command.reservationId,
            amountRial = command.amountRial,
            reason = command.reason.trim(),
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.unit { api.discount(idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("finance:")
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.DISCOUNT,
                method = "POST",
                path = "/api/v1/finance/discounts",
                body = body,
                summaryFa = "ثبت تخفیف: ${command.reason}",
                idempotencyKey = idempotencyKey,
            )
        }
    }
}
