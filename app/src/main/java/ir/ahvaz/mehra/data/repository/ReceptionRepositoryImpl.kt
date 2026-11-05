package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.ReceptionApi
import ir.ahvaz.mehra.data.remote.dto.CheckInRequestDto
import ir.ahvaz.mehra.data.remote.dto.CheckOutRequestDto
import ir.ahvaz.mehra.data.remote.dto.CompanionDto
import ir.ahvaz.mehra.data.remote.dto.ExtendStayRequestDto
import ir.ahvaz.mehra.data.remote.dto.MoveRoomRequestDto
import ir.ahvaz.mehra.data.remote.dto.ReservationDto
import ir.ahvaz.mehra.data.remote.dto.StayDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OperationType
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.CheckInCommand
import ir.ahvaz.mehra.domain.model.CheckOutCommand
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.repository.ReceptionRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceptionRepositoryImpl @Inject constructor(
    private val api: ReceptionApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), ReceptionRepository {

    private val stayListType = object : TypeToken<List<StayDto>>() {}.type
    private val reservationListType = object : TypeToken<List<ReservationDto>>() {}.type
    private val stringListType = object : TypeToken<List<String>>() {}.type

    override suspend fun searchReservations(query: String): AppResult<List<Reservation>> =
        readThrough("reception:search:${query.trim()}", reservationListType) {
            executor.value { api.searchReservations(query.trim()) }
        }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.data.data.mapNotNull { it.toDomain() })
                is AppResult.Failure -> result
            }
        }

    override suspend fun activeStays(query: String): AppResult<CachedData<List<Stay>>> =
        readThrough("reception:stays:${query.trim()}", stayListType) {
            executor.value { api.activeStays(query.trim().takeIf { it.isNotEmpty() }) }
        }.mapCached { dtos -> dtos.mapNotNull { it.toDomain() } }

    override suspend fun stay(stayId: String): AppResult<Stay> =
        executor.value { api.stay(stayId) }.map { dto ->
            dto.toDomain() ?: Stay(
                id = stayId,
                reservationId = "",
                reservationCode = "",
                guestId = "",
                guestName = "",
                roomId = "",
                roomNumber = "",
                plannedCheckOut = LocalDate.now(),
            )
        }

    override suspend fun checkIn(command: CheckInCommand): AppResult<Stay> {
        val body = CheckInRequestDto(
            roomId = command.roomId,
            companions = command.companions.map {
                CompanionDto(id = it.id, fullName = it.fullName, nationalId = it.nationalId, relation = it.relation)
            },
            documentTypes = command.documentTypes,
            initialPaymentRial = command.initialPaymentRial,
            paymentMethod = command.paymentMethod.wire,
            note = command.note,
        )
        if (command.roomId.isBlank()) {
            return AppResult.Failure(AppError.validation("اتاق تخصیصی را انتخاب کنید.", mapOf("roomId" to "الزامی")))
        }
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.value { api.checkIn(command.reservationId, idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("reception:")
                cache.invalidatePrefix("rooms:")
                cache.invalidatePrefix("dashboard:")
                val stay = result.data.toDomain()
                if (stay != null) AppResult.Success(stay)
                else AppResult.Failure(AppError.server("پاسخ پذیرش قابل خواندن نبود."))
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.CHECK_IN,
                method = "POST",
                path = "/api/v1/reception/${command.reservationId}/check-in",
                body = body,
                summaryFa = "پذیرش رزرو ${command.reservationId}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    override suspend fun checkOut(command: CheckOutCommand): AppResult<Receipt> {
        val body = CheckOutRequestDto(
            finalPaymentRial = command.finalPaymentRial,
            paymentMethod = command.paymentMethod.wire,
            discountRial = command.discountRial,
            note = command.note,
            moveRoomToCleaning = command.moveRoomToCleaning,
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.value { api.checkOut(command.stayId, idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("reception:")
                cache.invalidatePrefix("rooms:")
                cache.invalidatePrefix("dashboard:")
                cache.invalidatePrefix("finance:")
                AppResult.Success(result.data.toDomain())
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.CHECK_OUT,
                method = "POST",
                path = "/api/v1/reception/stays/${command.stayId}/check-out",
                body = body,
                summaryFa = "تسویه و خروج اقامت ${command.stayId}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    override suspend fun extendStay(stayId: String, newCheckOut: LocalDate): AppResult<Unit> =
        executor.unit { api.extend(stayId, ExtendStayRequestDto(IsoFormat.date(newCheckOut))) }

    override suspend fun moveRoom(stayId: String, newRoomId: String, reason: String?): AppResult<Unit> =
        executor.unit { api.moveRoom(stayId, MoveRoomRequestDto(newRoomId, reason)) }

    override suspend fun receipt(stayId: String): AppResult<Receipt> =
        executor.value { api.receipt(stayId) }.map { it.toDomain() }

    override suspend fun documentTypes(): AppResult<List<String>> =
        readThrough("reception:document-types", stringListType) {
            executor.value { api.documentTypes() }
        }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.data.data)
                is AppResult.Failure -> result
            }
        }
}
