package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.date.nightsBetween
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.ReservationsApi
import ir.ahvaz.mehra.data.remote.dto.AvailableRoomDto
import ir.ahvaz.mehra.data.remote.dto.CalendarEntryDto
import ir.ahvaz.mehra.data.remote.dto.CancelReservationRequestDto
import ir.ahvaz.mehra.data.remote.dto.CompanionDto
import ir.ahvaz.mehra.data.remote.dto.CreateReservationRequestDto
import ir.ahvaz.mehra.data.remote.dto.PromoteWaitlistRequestDto
import ir.ahvaz.mehra.data.remote.dto.QuoteRequestDto
import ir.ahvaz.mehra.data.remote.dto.ReservationDto
import ir.ahvaz.mehra.data.remote.dto.WaitlistEntryDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OperationType
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.AvailabilityQuery
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationCalendarEntry
import ir.ahvaz.mehra.domain.model.ReservationDraft
import ir.ahvaz.mehra.domain.model.ReservationQuote
import ir.ahvaz.mehra.domain.model.WaitlistEntry
import ir.ahvaz.mehra.domain.repository.ReservationFilter
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepositoryImpl @Inject constructor(
    private val api: ReservationsApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), ReservationRepository {

    private val listType = object : TypeToken<List<ReservationDto>>() {}.type
    private val waitlistType = object : TypeToken<List<WaitlistEntryDto>>() {}.type
    private val availableType = object : TypeToken<List<AvailableRoomDto>>() {}.type
    private val calendarType = object : TypeToken<List<CalendarEntryDto>>() {}.type

    override suspend fun list(filter: ReservationFilter): AppResult<CachedData<List<Reservation>>> {
        val from = filter.from?.let { IsoFormat.date(it) }
        val to = filter.to?.let { IsoFormat.date(it) }
        val key = "reservations:${filter.status?.wire.orEmpty()}:$from:$to:${filter.query.trim()}"
        return readThrough(key, listType) {
            executor.value {
                api.list(
                    search = filter.query.trim().takeIf { it.isNotEmpty() },
                    status = filter.status?.wire,
                    from = from,
                    to = to,
                    page = 1,
                    pageSize = 200,
                )
            }.map { it.items.orEmpty() }
        }.mapCached { dtos -> dtos.mapNotNull { it.toDomain() } }
    }

    override suspend fun get(reservationId: String): AppResult<Reservation> =
        executor.value { api.get(reservationId) }.map { dto ->
            dto.toDomain() ?: Reservation(
                id = dto.id.orEmpty(),
                code = dto.code.orEmpty(),
                guestId = dto.guestId.orEmpty(),
                guestName = dto.guestName.orEmpty(),
                checkIn = LocalDate.now(),
                checkOut = LocalDate.now(),
            )
        }

    override suspend fun availability(query: AvailabilityQuery): AppResult<List<AvailableRoom>> =
        readThrough(
            "availability:${IsoFormat.date(query.checkIn)}:${IsoFormat.date(query.checkOut)}:${query.guestsCount}:${query.roomTypeId.orEmpty()}",
            availableType,
        ) {
            executor.value {
                api.availability(
                    checkIn = IsoFormat.date(query.checkIn),
                    checkOut = IsoFormat.date(query.checkOut),
                    guestsCount = query.guestsCount,
                    roomTypeId = query.roomTypeId,
                )
            }
        }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.data.data.map { it.toDomain() })
                is AppResult.Failure -> result
            }
        }

    override suspend fun quote(
        roomId: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guestsCount: Int,
        discountRial: Long,
    ): AppResult<ReservationQuote> =
        executor.value {
            api.quote(
                QuoteRequestDto(
                    roomId = roomId,
                    checkInDate = IsoFormat.date(checkIn),
                    checkOutDate = IsoFormat.date(checkOut),
                    guestsCount = guestsCount,
                    discountRial = discountRial,
                )
            )
        }.map { it.toDomain() }

    override suspend fun create(draft: ReservationDraft): AppResult<Reservation> {
        val checkIn = draft.checkIn?.toGregorian()
        val checkOut = draft.checkOut?.toGregorian()
        val validation = validationError(draft, checkIn, checkOut)
        if (validation != null) return AppResult.Failure(validation)

        val body = CreateReservationRequestDto(
            guestId = draft.guestId,
            roomId = draft.roomId,
            checkInDate = IsoFormat.date(checkIn!!),
            checkOutDate = IsoFormat.date(checkOut!!),
            guestsCount = parseLongInput(draft.guestsCount)?.toInt() ?: 1,
            discountRial = parseLongInput(draft.discountRial.toLatinDigits()),
            prepaidRial = parseLongInput(draft.prepaidRial.toLatinDigits()),
            paymentMethod = draft.paymentMethod.wire,
            notes = draft.notes.takeIf { it.isNotBlank() },
            joinWaitlist = draft.joinWaitlist,
            companions = draft.companionNames
                .filter { it.isNotBlank() }
                .map { CompanionDto(fullName = it.trim()) },
        )
        // The key is created before the first attempt and reused if the operation is queued.
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.value { api.create(idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                val reservation = result.data.toDomain()
                if (reservation != null) {
                    cache.invalidatePrefix("reservations:")
                    cache.invalidatePrefix("dashboard:")
                    AppResult.Success(reservation)
                } else {
                    AppResult.Failure(AppError.server("پاسخ سرور برای رزرو قابل خواندن نبود."))
                }
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.CREATE_RESERVATION,
                method = "POST",
                path = "/api/v1/reservations",
                body = body,
                summaryFa = "ثبت رزرو برای ${draft.guestName.ifBlank { "مهمان" }}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    override suspend fun confirm(reservationId: String): AppResult<Unit> =
        executor.unit { api.confirm(reservationId) }

    override suspend fun cancel(reservationId: String, reason: String?): AppResult<Unit> =
        executor.unit { api.cancel(reservationId, CancelReservationRequestDto(reason)) }

    override suspend fun waitlist(): AppResult<CachedData<List<WaitlistEntry>>> =
        readThrough("waitlist", waitlistType) {
            executor.value { api.waitlist() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }

    override suspend fun promoteWaitlist(waitlistId: String, roomId: String): AppResult<Reservation> =
        executor.value {
            api.promoteWaitlist(waitlistId, UUID.randomUUID().toString(), PromoteWaitlistRequestDto(roomId))
        }.map { dto ->
            dto.toDomain() ?: Reservation(
                id = dto.id.orEmpty(),
                code = dto.code.orEmpty(),
                guestId = dto.guestId.orEmpty(),
                guestName = dto.guestName.orEmpty(),
                checkIn = LocalDate.now(),
                checkOut = LocalDate.now(),
            )
        }

    override suspend fun calendar(from: LocalDate, to: LocalDate): AppResult<List<ReservationCalendarEntry>> =
        readThrough("calendar:${IsoFormat.date(from)}:${IsoFormat.date(to)}", calendarType) {
            executor.value { api.calendar(IsoFormat.date(from), IsoFormat.date(to)) }
        }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.data.data.mapNotNull { it.toDomain() })
                is AppResult.Failure -> result
            }
        }

    private fun validationError(
        draft: ReservationDraft,
        checkIn: LocalDate?,
        checkOut: LocalDate?,
    ): AppError? {
        val fields = mutableMapOf<String, String>()
        if (draft.guestId.isNullOrBlank()) fields["guestId"] = "انتخاب مهمان الزامی است."
        if (checkIn == null) fields["checkIn"] = "تاریخ ورود را انتخاب کنید."
        if (checkOut == null) fields["checkOut"] = "تاریخ خروج را انتخاب کنید."
        if (checkIn != null && checkOut != null && nightsBetween(checkIn, checkOut) < 1) {
            fields["checkOut"] = "تاریخ خروج باید بعد از تاریخ ورود باشد."
        }
        if (!draft.joinWaitlist && draft.roomId.isNullOrBlank()) {
            fields["roomId"] = "انتخاب اتاق الزامی است."
        }
        val guests = parseLongInput(draft.guestsCount)?.toInt() ?: 0
        if (guests < 1) fields["guestsCount"] = "تعداد نفرات باید حداقل ۱ باشد."
        return if (fields.isEmpty()) null else AppError.validation("اطلاعات رزرو کامل نیست.", fields)
    }
}
