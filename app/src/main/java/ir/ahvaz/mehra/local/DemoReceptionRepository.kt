package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CheckInCommand
import ir.ahvaz.mehra.domain.model.CheckOutCommand
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.ReceiptLine
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.model.StayStatus
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionType
import ir.ahvaz.mehra.domain.repository.ReceptionRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoReceptionRepository @Inject constructor(
    private val store: DemoStore,
) : ReceptionRepository {

    override suspend fun searchReservations(query: String): AppResult<List<Reservation>> {
        delay(120)
        val q = query.trim()
        val digits = q.demoDigits()
        val list = store.reservations
            .filter {
                it.status == ReservationStatus.CONFIRMED ||
                    it.status == ReservationStatus.PENDING ||
                    it.status == ReservationStatus.CHECKED_IN ||
                    it.status == ReservationStatus.WAITLIST
            }
            .filter {
                q.isEmpty() ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.guestName.contains(q) ||
                    it.roomNumber?.contains(q) == true ||
                    (digits.isNotEmpty() && it.guestPhone?.contains(digits) == true)
            }
            .sortedBy { it.checkIn }
        return demoOk(list)
    }

    override suspend fun activeStays(query: String): AppResult<CachedData<List<Stay>>> {
        delay(140)
        val q = query.trim()
        val list = store.activeStays()
            .filter {
                q.isEmpty() ||
                    it.guestName.contains(q) ||
                    it.roomNumber.contains(q) ||
                    it.reservationCode.contains(q, ignoreCase = true)
            }
            .sortedBy { it.roomNumber }
        return demoCached(list)
    }

    override suspend fun stay(stayId: String): AppResult<Stay> =
        store.stay(stayId)?.let { demoOk(it) } ?: demoNotFound("اقامت مورد نظر یافت نشد.")

    override suspend fun checkIn(command: CheckInCommand): AppResult<Stay> {
        delay(320)
        val reservationIndex = store.reservations.indexOfFirst { it.id == command.reservationId }
        if (reservationIndex < 0) return demoNotFound("رزرو مورد نظر یافت نشد.")
        val reservation = store.reservations[reservationIndex]
        if (reservation.status == ReservationStatus.CHECKED_IN) {
            return demoFailure("این رزرو قبلاً پذیرش شده است.")
        }
        val room = store.room(command.roomId) ?: store.room(reservation.roomId.orEmpty())
        if (room == null) return demoNotFound("اتاق مورد نظر یافت نشد.")
        if (room.status == RoomStatus.MAINTENANCE || room.status == RoomStatus.OUT_OF_SERVICE) {
            return demoFailure("اتاق ${room.roomNumber} در وضعیت ${room.status.labelFa} است و قابل واگذاری نیست.")
        }

        val stay = Stay(
            id = store.nextId("stay"),
            reservationId = reservation.id,
            reservationCode = reservation.code,
            guestId = reservation.guestId,
            guestName = reservation.guestName,
            guestPhone = reservation.guestPhone,
            roomId = room.id,
            roomNumber = room.roomNumber,
            checkInAt = Instant.now(),
            plannedCheckOut = reservation.checkOut,
            status = StayStatus.IN_HOUSE,
            guestsCount = reservation.guestsCount,
            companions = if (command.companions.isNotEmpty()) command.companions else reservation.companions,
            totalRial = reservation.totalRial,
            paidRial = reservation.paidRial,
        )
        store.stays.add(0, stay)

        val paid = (reservation.paidRial + (command.initialPaymentRial ?: 0L))
        store.reservations[reservationIndex] = reservation.copy(
            status = ReservationStatus.CHECKED_IN,
            roomId = room.id,
            roomNumber = room.roomNumber,
            roomTypeName = room.roomTypeName,
            paidRial = paid,
            companions = stay.companions,
        )
        updateRoom(room.id) {
            it.copy(
                status = RoomStatus.OCCUPIED,
                currentGuestName = stay.guestName,
                currentStayId = stay.id,
                updatedAt = Instant.now(),
            )
        }
        (command.initialPaymentRial ?: 0L).takeIf { it > 0L }?.let { amount ->
            store.transactions.add(
                0,
                Transaction(
                    id = store.nextId("tx"),
                    code = "TR-${1100 + store.transactions.size}",
                    type = TransactionType.RECEIVE,
                    amountRial = amount,
                    method = command.paymentMethod,
                    description = "دریافت هنگام پذیرش — اتاق ${room.roomNumber}",
                    guestName = stay.guestName,
                    roomNumber = room.roomNumber,
                    operatorName = DemoStore.DEMO_OPERATOR,
                    reservationId = reservation.id,
                    createdAt = Instant.now(),
                ),
            )
        }
        return demoOk(stay)
    }

    override suspend fun checkOut(command: CheckOutCommand): AppResult<Receipt> {
        delay(320)
        val index = store.stays.indexOfFirst { it.id == command.stayId }
        if (index < 0) return demoNotFound("اقامت مورد نظر یافت نشد.")
        val stay = store.stays[index]
        if (stay.status != StayStatus.IN_HOUSE) return demoFailure("این اقامت قبلاً تسویه شده است.")

        val total = stay.totalRial
        val paid = stay.paidRial + command.finalPaymentRial - command.discountRial
        val receipt = buildReceipt(stay, total, paid, command)

        store.stays[index] = stay.copy(
            status = StayStatus.CHECKED_OUT,
            actualCheckOutAt = Instant.now(),
            totalRial = total,
            paidRial = paid,
        )
        store.reservations.indexOfFirst { it.id == stay.reservationId }.takeIf { it >= 0 }?.let { rIndex ->
            store.reservations[rIndex] = store.reservations[rIndex].copy(
                status = ReservationStatus.CHECKED_OUT,
                paidRial = paid,
            )
        }
        updateRoom(stay.roomId) {
            it.copy(
                status = if (command.moveRoomToCleaning) RoomStatus.CLEANING else RoomStatus.FREE,
                currentGuestName = null,
                currentStayId = null,
                updatedAt = Instant.now(),
            )
        }
        if (command.moveRoomToCleaning) {
            store.housekeepingTasks.add(
                0,
                ir.ahvaz.mehra.domain.model.HousekeepingTask(
                    id = store.nextId("hk"),
                    roomId = stay.roomId,
                    roomNumber = stay.roomNumber,
                    type = ir.ahvaz.mehra.domain.model.CleaningType.CHECKOUT,
                    status = ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus.PENDING,
                    notes = "نظافت پس از خروج ${stay.guestName}",
                    createdAt = Instant.now(),
                ),
            )
        }
        if (command.finalPaymentRial > 0L) {
            store.transactions.add(
                0,
                Transaction(
                    id = store.nextId("tx"),
                    code = "TR-${1100 + store.transactions.size}",
                    type = TransactionType.RECEIVE,
                    amountRial = command.finalPaymentRial,
                    method = command.paymentMethod,
                    description = "تسویه حساب خروج — اتاق ${stay.roomNumber}",
                    guestName = stay.guestName,
                    roomNumber = stay.roomNumber,
                    operatorName = DemoStore.DEMO_OPERATOR,
                    reservationId = stay.reservationId,
                    createdAt = Instant.now(),
                ),
            )
        }
        return demoOk(receipt)
    }

    override suspend fun extendStay(stayId: String, newCheckOut: LocalDate): AppResult<Unit> {
        delay(220)
        val index = store.stays.indexOfFirst { it.id == stayId }
        if (index < 0) return demoNotFound("اقامت مورد نظر یافت نشد.")
        val stay = store.stays[index]
        if (!newCheckOut.isAfter(stay.plannedCheckOut)) {
            return demoFailure("تاریخ جدید باید بعد از تاریخ خروج فعلی باشد.")
        }
        val room = store.room(stay.roomId)
        val nightly = if (room != null && stay.totalRial > 0 && stay.guestsCount > 0) {
            room.ratePerNightRial
        } else {
            0L
        }
        val extraNights = store.nights(stay.plannedCheckOut, newCheckOut)
        store.stays[index] = stay.copy(
            plannedCheckOut = newCheckOut,
            totalRial = stay.totalRial + nightly * extraNights,
        )
        store.reservations.indexOfFirst { it.id == stay.reservationId }.takeIf { it >= 0 }?.let { rIndex ->
            store.reservations[rIndex] = store.reservations[rIndex].copy(checkOut = newCheckOut)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun moveRoom(stayId: String, newRoomId: String, reason: String?): AppResult<Unit> {
        delay(260)
        val index = store.stays.indexOfFirst { it.id == stayId }
        if (index < 0) return demoNotFound("اقامت مورد نظر یافت نشد.")
        val stay = store.stays[index]
        val target = store.room(newRoomId) ?: return demoNotFound("اتاق مقصد یافت نشد.")
        if (target.status != RoomStatus.FREE && target.status != RoomStatus.CLEANING) {
            return demoFailure("اتاق ${target.roomNumber} آزاد نیست.")
        }
        val previousRoomId = stay.roomId
        store.stays[index] = stay.copy(roomId = target.id, roomNumber = target.roomNumber)
        updateRoom(previousRoomId) {
            it.copy(
                status = RoomStatus.CLEANING,
                currentGuestName = null,
                currentStayId = null,
                notes = reason?.ifBlank { null },
                updatedAt = Instant.now(),
            )
        }
        updateRoom(target.id) {
            it.copy(
                status = RoomStatus.OCCUPIED,
                currentGuestName = stay.guestName,
                currentStayId = stay.id,
                updatedAt = Instant.now(),
            )
        }
        store.reservations.indexOfFirst { it.id == stay.reservationId }.takeIf { it >= 0 }?.let { rIndex ->
            store.reservations[rIndex] = store.reservations[rIndex].copy(
                roomId = target.id,
                roomNumber = target.roomNumber,
                roomTypeName = target.roomTypeName,
            )
        }
        return AppResult.Success(Unit)
    }

    override suspend fun receipt(stayId: String): AppResult<Receipt> {
        delay(140)
        val stay = store.stay(stayId) ?: return demoNotFound("اقامت مورد نظر یافت نشد.")
        return demoOk(
            buildReceipt(
                stay = stay,
                total = stay.totalRial,
                paid = stay.paidRial,
                command = null,
            )
        )
    }

    override suspend fun documentTypes(): AppResult<List<String>> = demoOk(store.documentTypes())

    private fun buildReceipt(
        stay: Stay,
        total: Long,
        paid: Long,
        command: CheckOutCommand?,
    ): Receipt {
        val nights = store.nights(store.localDateOf(stay.checkInAt) ?: store.today, stay.plannedCheckOut)
        val lines = mutableListOf(
            ReceiptLine("اجاره اتاق ${stay.roomNumber} — $nights شب", total, isCredit = true),
            ReceiptLine("پرداخت‌های ثبت‌شده", stay.paidRial, isCredit = false),
        )
        if (command != null && command.discountRial > 0) {
            lines += ReceiptLine("تخفیف هنگام خروج", command.discountRial, isCredit = false)
        }
        if (command != null && command.finalPaymentRial > 0) {
            lines += ReceiptLine("پرداخت نهایی (${command.paymentMethod.labelFa})", command.finalPaymentRial, isCredit = false)
        }
        return Receipt(
            receiptNumber = "RC-${1100 + store.stays.size}",
            issuedAt = Instant.now(),
            guestName = stay.guestName,
            roomNumber = stay.roomNumber,
            checkInAt = stay.checkInAt,
            checkOutAt = store.localDateOf(stay.actualCheckOutAt)?.let { stay.actualCheckOutAt } ?: Instant.now(),
            plannedCheckOut = stay.plannedCheckOut,
            nights = nights,
            guestsCount = stay.guestsCount,
            lines = lines,
            totalRial = total,
            paidRial = paid,
            balanceRial = (total - paid).coerceAtLeast(0L),
            paymentMethod = command?.paymentMethod,
            operatorName = DemoStore.DEMO_OPERATOR,
            hotelName = DemoStore.HOTEL_NAME,
        )
    }

    private fun updateRoom(roomId: String, transform: (Room) -> Room) {
        val index = store.rooms.indexOfFirst { it.id == roomId }
        if (index >= 0) store.rooms[index] = transform(store.rooms[index])
    }
}
