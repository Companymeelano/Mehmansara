package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.AvailabilityQuery
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationCalendarEntry
import ir.ahvaz.mehra.domain.model.ReservationDraft
import ir.ahvaz.mehra.domain.model.ReservationQuote
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.WaitlistEntry
import ir.ahvaz.mehra.domain.repository.ReservationFilter
import ir.ahvaz.mehra.domain.repository.ReservationRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoReservationRepository @Inject constructor(
    private val store: DemoStore,
) : ReservationRepository {

    private val blockingStatuses = setOf(
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        ReservationStatus.CHECKED_IN,
    )

    override suspend fun list(filter: ReservationFilter): AppResult<CachedData<List<Reservation>>> {
        delay(150)
        val q = filter.query.trim()
        val status = filter.status
        val from = filter.from
        val to = filter.to
        val list = store.reservations.asSequence()
            .filter { status == null || it.status == status }
            .filter { from == null || !it.checkOut.isBefore(from) }
            .filter { to == null || !it.checkIn.isAfter(to) }
            .filter {
                q.isEmpty() ||
                    it.guestName.contains(q) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.roomNumber?.contains(q) == true ||
                    it.guestPhone?.contains(q.demoDigits()) == true
            }
            .sortedByDescending { it.checkIn }
            .toList()
        return demoCached(list)
    }

    override suspend fun get(reservationId: String): AppResult<Reservation> =
        store.reservation(reservationId)?.let { demoOk(it) }
            ?: demoNotFound("رزرو مورد نظر یافت نشد.")

    override suspend fun availability(query: AvailabilityQuery): AppResult<List<AvailableRoom>> {
        delay(180)
        if (!query.checkOut.isAfter(query.checkIn)) {
            return demoFailure("تاریخ خروج باید بعد از تاریخ ورود باشد.")
        }
        val roomTypeId = query.roomTypeId
        val list = store.rooms.asSequence()
            .filter { it.status == RoomStatus.FREE || it.status == RoomStatus.RESERVED }
            .filter { it.capacity >= query.guestsCount }
            .filter { roomTypeId == null || it.roomTypeId == roomTypeId }
            .filter { !isBlocked(it.id, query.checkIn, query.checkOut) }
            .sortedBy { it.roomNumber }
            .map { room ->
                AvailableRoom(
                    roomId = room.id,
                    roomNumber = room.roomNumber,
                    floor = room.floor,
                    roomTypeName = room.roomTypeName,
                    capacity = room.capacity,
                    ratePerNightRial = room.ratePerNightRial,
                )
            }
            .toList()
        return demoOk(list)
    }

    override suspend fun quote(
        roomId: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guestsCount: Int,
        discountRial: Long,
    ): AppResult<ReservationQuote> {
        val room = store.room(roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        if (!checkOut.isAfter(checkIn)) return demoFailure("تاریخ خروج باید بعد از تاریخ ورود باشد.")
        val nights = store.nights(checkIn, checkOut)
        val roomTotal = room.ratePerNightRial * nights
        val discount = discountRial.coerceIn(0L, roomTotal)
        return demoOk(
            ReservationQuote(
                nights = nights,
                nightlyRateRial = room.ratePerNightRial,
                roomTotalRial = roomTotal,
                discountRial = discount,
                totalRial = roomTotal - discount,
                prepaidRial = 0L,
                remainingRial = roomTotal - discount,
            )
        )
    }

    override suspend fun create(draft: ReservationDraft): AppResult<Reservation> {
        delay(300)
        val guestName = draft.guestName.trim()
        if (guestName.isEmpty()) return demoFailure("نام مهمان را وارد کنید.")
        val checkIn = draft.checkIn?.toGregorian() ?: return demoFailure("تاریخ ورود را انتخاب کنید.")
        val checkOut = draft.checkOut?.toGregorian() ?: return demoFailure("تاریخ خروج را انتخاب کنید.")
        if (!checkOut.isAfter(checkIn)) return demoFailure("تاریخ خروج باید بعد از تاریخ ورود باشد.")
        val guestsCount = draft.guestsCount.demoDigits().toIntOrNull() ?: 1
        if (guestsCount <= 0) return demoFailure("تعداد مهمان معتبر نیست.")
        val discount = draft.discountRial.demoDigits().toLongOrNull() ?: 0L
        val prepaid = draft.prepaidRial.demoDigits().toLongOrNull() ?: 0L

        val room = draft.roomId?.let { store.room(it) }
        if (room != null && !draft.joinWaitlist && isBlocked(room.id, checkIn, checkOut)) {
            return AppResult.Failure(
                AppError.conflict("اتاق ${room.roomNumber} در این بازه رزرو شده است. اتاق دیگری انتخاب کنید.")
            )
        }
        if (room != null && room.capacity < guestsCount) {
            return demoFailure("ظرفیت اتاق ${room.roomNumber} برای $guestsCount مهمان کافی نیست.")
        }

        val guest = draft.guestId?.let { store.guest(it) }
            ?: store.guestByName(guestName)
            ?: newGuest(guestName).also { store.guests += it }

        val nights = store.nights(checkIn, checkOut)
        val nightly = room?.ratePerNightRial ?: 2_500_000L
        val total = nightly * nights
        val status = when {
            draft.joinWaitlist -> ReservationStatus.WAITLIST
            prepaid > 0 -> ReservationStatus.CONFIRMED
            else -> ReservationStatus.PENDING
        }
        val reservation = Reservation(
            id = store.nextId("res"),
            code = "MR-${34200 + store.reservations.size}",
            guestId = guest.id,
            guestName = guest.fullName,
            guestPhone = guest.phone,
            roomId = room?.id,
            roomNumber = room?.roomNumber,
            roomTypeName = room?.roomTypeName,
            checkIn = checkIn,
            checkOut = checkOut,
            guestsCount = guestsCount,
            nightlyRateRial = nightly,
            totalRial = total,
            discountRial = discount,
            paidRial = prepaid,
            status = status,
            notes = draft.notes.trim().ifBlank { null },
            createdByName = DemoStore.DEMO_OPERATOR,
            createdAt = Instant.now(),
            companions = draft.companionNames.filter { it.isNotBlank() }
                .map { ir.ahvaz.mehra.domain.model.Companion(fullName = it.trim()) },
        )
        store.reservations.add(0, reservation)

        if (prepaid > 0) {
            store.transactions.add(
                0,
                ir.ahvaz.mehra.domain.model.Transaction(
                    id = store.nextId("tx"),
                    code = "TR-${1100 + store.transactions.size}",
                    type = ir.ahvaz.mehra.domain.model.TransactionType.RECEIVE,
                    amountRial = prepaid,
                    method = draft.paymentMethod,
                    description = "پیش‌پرداخت رزرو ${reservation.code}",
                    guestName = reservation.guestName,
                    roomNumber = reservation.roomNumber,
                    operatorName = DemoStore.DEMO_OPERATOR,
                    reservationId = reservation.id,
                    createdAt = Instant.now(),
                ),
            )
        }
        if (room != null && status == ReservationStatus.CONFIRMED && room.status == RoomStatus.FREE) {
            val index = store.rooms.indexOfFirst { it.id == room.id }
            if (index >= 0) {
                store.rooms[index] = room.copy(
                    status = RoomStatus.RESERVED,
                    nextReservationAt = checkIn,
                    updatedAt = Instant.now(),
                )
            }
        }
        return demoOk(reservation)
    }

    override suspend fun confirm(reservationId: String): AppResult<Unit> {
        delay(200)
        val index = store.reservations.indexOfFirst { it.id == reservationId }
        if (index < 0) return demoNotFound("رزرو مورد نظر یافت نشد.")
        store.reservations[index] = store.reservations[index].copy(status = ReservationStatus.CONFIRMED)
        return AppResult.Success(Unit)
    }

    override suspend fun cancel(reservationId: String, reason: String?): AppResult<Unit> {
        delay(220)
        val index = store.reservations.indexOfFirst { it.id == reservationId }
        if (index < 0) return demoNotFound("رزرو مورد نظر یافت نشد.")
        val reservation = store.reservations[index]
        store.reservations[index] = reservation.copy(
            status = ReservationStatus.CANCELLED,
            notes = reason?.ifBlank { null } ?: reservation.notes,
        )
        reservation.roomId?.let { roomId ->
            val roomIndex = store.rooms.indexOfFirst { it.id == roomId }
            if (roomIndex >= 0 && store.rooms[roomIndex].status == RoomStatus.RESERVED) {
                store.rooms[roomIndex] = store.rooms[roomIndex].copy(
                    status = RoomStatus.FREE,
                    nextReservationAt = null,
                    updatedAt = Instant.now(),
                )
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun waitlist(): AppResult<CachedData<List<WaitlistEntry>>> {
        delay(140)
        val entries = store.reservations
            .filter { it.status == ReservationStatus.WAITLIST }
            .map { reservation ->
                WaitlistEntry(
                    id = reservation.id,
                    guestName = reservation.guestName,
                    guestPhone = reservation.guestPhone,
                    requestedCheckIn = reservation.checkIn,
                    requestedCheckOut = reservation.checkOut,
                    guestsCount = reservation.guestsCount,
                    roomTypeName = reservation.roomTypeName,
                    notes = reservation.notes,
                    createdAt = reservation.createdAt,
                )
            }
        return demoCached(entries)
    }

    override suspend fun promoteWaitlist(waitlistId: String, roomId: String): AppResult<Reservation> {
        delay(240)
        val index = store.reservations.indexOfFirst { it.id == waitlistId }
        if (index < 0) return demoNotFound("درخواست لیست انتظار یافت نشد.")
        val room = store.room(roomId) ?: return demoNotFound("اتاق انتخاب شده یافت نشد.")
        val current = store.reservations[index]
        if (isBlocked(room.id, current.checkIn, current.checkOut)) {
            return AppResult.Failure(AppError.conflict("اتاق ${room.roomNumber} در این بازه آزاد نیست."))
        }
        val promoted = current.copy(
            roomId = room.id,
            roomNumber = room.roomNumber,
            roomTypeName = room.roomTypeName,
            nightlyRateRial = room.ratePerNightRial,
            totalRial = room.ratePerNightRial * store.nights(current.checkIn, current.checkOut),
            status = ReservationStatus.CONFIRMED,
        )
        store.reservations[index] = promoted
        return demoOk(promoted)
    }

    override suspend fun calendar(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<ReservationCalendarEntry>> {
        delay(160)
        val entries = mutableListOf<ReservationCalendarEntry>()
        store.reservations
            .filter { it.status != ReservationStatus.CANCELLED }
            .forEach { reservation ->
                var day = if (reservation.checkIn.isBefore(from)) from else reservation.checkIn
                while (!day.isAfter(to) && day.isBefore(reservation.checkOut)) {
                    entries += ReservationCalendarEntry(
                        reservationId = reservation.id,
                        date = day,
                        roomId = reservation.roomId,
                        roomNumber = reservation.roomNumber,
                        guestName = reservation.guestName,
                        status = reservation.status,
                        isArrival = day == reservation.checkIn,
                        isDeparture = day.plusDays(1) == reservation.checkOut,
                    )
                    day = day.plusDays(1)
                }
            }
        return demoOk(entries.sortedBy { it.date })
    }

    private fun isBlocked(roomId: String, from: LocalDate, to: LocalDate, ignoreId: String? = null): Boolean =
        store.reservations.any { reservation ->
            reservation.id != ignoreId &&
                reservation.roomId == roomId &&
                reservation.status in blockingStatuses &&
                reservation.checkIn.isBefore(to) &&
                reservation.checkOut.isAfter(from)
        }

    private fun newGuest(fullName: String): Guest {
        val parts = fullName.split(" ").filter { it.isNotBlank() }
        return Guest(
            id = store.nextId("guest"),
            firstName = parts.firstOrNull().orEmpty(),
            lastName = parts.drop(1).joinToString(" "),
            phone = null,
            city = "اهواز",
            totalStays = 0,
        )
    }

    @Suppress("unused")
    private fun methodLabel(method: PaymentMethod): String = method.labelFa
}
