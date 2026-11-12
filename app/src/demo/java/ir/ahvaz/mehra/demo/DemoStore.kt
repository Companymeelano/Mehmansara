package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.domain.model.ActivityItem
import ir.ahvaz.mehra.domain.model.ActivityKind
import ir.ahvaz.mehra.domain.model.AlertItem
import ir.ahvaz.mehra.domain.model.AlertSeverity
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.Companion as GuestCompanion
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.LookupItem
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.model.StayStatus
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory dataset that backs the `demo` flavour only.
 *
 * This file lives in `src/demo`, so it is compiled **only** into the showcase build: the
 * production app (`prod`) contains no sample data at all and renders real server responses
 * or honest empty states.
 *
 * The store starts EMPTY on purpose: no rooms, guests, reservations, stays, payments,
 * tasks, tickets, notifications or users are pre-seeded. The operator enters every piece
 * of data manually through the app forms. Only the reference catalogues (room types,
 * floors, amenities) are provided, because the forms need them as selectable options.
 */
@Singleton
class DemoStore @Inject constructor() {

    companion object {
        private val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")
        const val HOTEL_NAME = "مهمان‌پذیر فرهنگیان اهواز"
        const val DEMO_OPERATOR = "زهرا کریمی"
    }

    val today: LocalDate = LocalDate.now()

    private var sequence: Int = 4210

    /** Deterministic id generator so newly created records look like server ids. */
    fun nextId(prefix: String): String {
        sequence += 1
        return "$prefix-$sequence"
    }

    fun at(dayOffset: Long, hour: Int, minute: Int = 0): Instant =
        today.plusDays(dayOffset).atTime(hour, minute).atZone(TEHRAN).toInstant()

    fun nights(from: LocalDate, to: LocalDate): Int =
        ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(1)

    // ------------------------------------------------------------------ catalogue

    private data class TypeDef(val id: String, val title: String, val capacity: Int, val rate: Long)

    private val types = listOf(
        TypeDef("rt-single", "اتاق یک\u200cتخته", 1, 1_800_000L),
        TypeDef("rt-double", "اتاق دو تخته", 2, 2_500_000L),
        TypeDef("rt-triple", "اتاق سه تخته", 3, 3_200_000L),
        TypeDef("rt-quad", "اتاق چهار تخته", 4, 3_900_000L),
        TypeDef("rt-family", "اتاق پنج\u200cتخته", 5, 5_200_000L),
        TypeDef("rt-suite", "سوئیت شش\u200cتخته", 6, 7_800_000L),
    )

    val roomTypes: List<LookupItem> = types.map { LookupItem(it.id, it.title) }

    val floors: List<Int> = listOf(1, 2)

    val amenities: List<LookupItem> = listOf(
        LookupItem("am-ac", "کولر گازی"),
        LookupItem("am-tv", "تلویزیون"),
        LookupItem("am-fridge", "یخچال"),
        LookupItem("am-wc", "سرویس بهداشتی اختصاصی"),
        LookupItem("am-wifi", "اینترنت بی‌سیم"),
        LookupItem("am-balcony", "بالکن"),
    )

    // ------------------------------------------------------------------ rooms

    val rooms: MutableList<Room> = mutableListOf()

    fun room(roomId: String): Room? = rooms.firstOrNull { it.id == roomId }

    fun roomByNumber(number: String): Room? = rooms.firstOrNull { it.roomNumber == number }

    // ------------------------------------------------------------------ guests

    val guests: MutableList<Guest> = mutableListOf()

    fun guest(guestId: String): Guest? = guests.firstOrNull { it.id == guestId }

    fun guestByName(name: String): Guest? = guests.firstOrNull { it.fullName == name.trim() }

    // ------------------------------------------------------------------ reservations & stays

    private fun defReservation(
        id: String,
        code: String,
        guest: Guest,
        room: Room?,
        checkIn: LocalDate,
        checkOut: LocalDate,
        status: ReservationStatus,
        guestsCount: Int = 2,
        discount: Long = 0L,
        paid: Long = 0L,
        notes: String? = null,
        createdBy: String = DEMO_OPERATOR,
        companions: List<GuestCompanion> = emptyList(),
    ): Reservation {
        val nights = nights(checkIn, checkOut)
        val nightly = room?.ratePerNightRial ?: 2_500_000L
        return Reservation(
            id = id,
            code = code,
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
            totalRial = nightly * nights,
            discountRial = discount,
            paidRial = paid,
            status = status,
            notes = notes,
            createdByName = createdBy,
            createdAt = at(-nights - 2L, 11, 30),
            companions = companions,
        )
    }

    val reservations: MutableList<Reservation> = mutableListOf()

    fun reservation(reservationId: String): Reservation? =
        reservations.firstOrNull { it.id == reservationId }

    private fun defStay(
        id: String,
        reservation: Reservation,
        room: Room,
        checkInAt: Instant?,
        plannedCheckOut: LocalDate,
        status: StayStatus = StayStatus.IN_HOUSE,
        actualCheckOutAt: Instant? = null,
        total: Long,
        paid: Long,
    ): Stay = Stay(
        id = id,
        reservationId = reservation.id,
        reservationCode = reservation.code,
        guestId = reservation.guestId,
        guestName = reservation.guestName,
        guestPhone = reservation.guestPhone,
        roomId = room.id,
        roomNumber = room.roomNumber,
        checkInAt = checkInAt,
        plannedCheckOut = plannedCheckOut,
        actualCheckOutAt = actualCheckOutAt,
        status = status,
        guestsCount = reservation.guestsCount,
        companions = reservation.companions,
        totalRial = total,
        paidRial = paid,
    )

    val stays: MutableList<Stay> = mutableListOf()

    fun stay(stayId: String): Stay? = stays.firstOrNull { it.id == stayId }

    fun activeStays(): List<Stay> = stays.filter { it.status == StayStatus.IN_HOUSE }

    fun documentTypes(): List<String> = listOf(
        "کارت ملی", "شناسنامه", "گذرنامه", "کارت پایان خدمت", "گواهی اشتغال به تحصیل",
    )

    // ------------------------------------------------------------------ finance

    private fun defTransaction(
        id: String,
        code: String,
        type: TransactionType,
        amount: Long,
        method: PaymentMethod,
        description: String,
        dayOffset: Long,
        hour: Int,
        minute: Int = 0,
        guestName: String? = null,
        roomNumber: String? = null,
        operator: String = DEMO_OPERATOR,
        reference: String? = null,
        reservationId: String? = null,
    ): Transaction = Transaction(
        id = id,
        code = code,
        type = type,
        amountRial = amount,
        method = method,
        description = description,
        guestName = guestName,
        roomNumber = roomNumber,
        operatorName = operator,
        reservationId = reservationId,
        reference = reference,
        createdAt = at(dayOffset, hour, minute),
    )

    val transactions: MutableList<Transaction> = mutableListOf()

    // ------------------------------------------------------------------ housekeeping & maintenance

    val housekeepingStaff: List<StaffMember> = listOf(
        StaffMember("staff-hk-1", "فاطمه رسولی", "Housekeeping", "خدمات", "09163334455"),
        StaffMember("staff-hk-2", "رقیه نیسی", "Housekeeping", "خدمات", "09164445566"),
        StaffMember("staff-hk-3", "سمیرا حیدری", "Housekeeping", "خدمات", "09165556677"),
        StaffMember("staff-hk-4", "علی زبیدی", "Housekeeping", "خدمات", "09166667788"),
    )

    val technicians: List<StaffMember> = listOf(
        StaffMember("staff-tc-1", "مهدی نیسی", "Maintenance", "تعمیرات", "09167778899"),
        StaffMember("staff-tc-2", "علیرضا زبیدی", "Maintenance", "تعمیرات", "09168889900"),
        StaffMember("staff-tc-3", "سعید فرجی", "Maintenance", "تعمیرات", "09169990011"),
    )

    val housekeepingTasks: MutableList<HousekeepingTask> = mutableListOf()

    val maintenanceTickets: MutableList<MaintenanceTicket> = mutableListOf()

    // ------------------------------------------------------------------ notifications, users, audit

    val notifications: MutableList<NotificationItem> = mutableListOf()

    val users: MutableList<AppUser> = mutableListOf()

    val auditLogs: MutableList<AuditLogEntry> = mutableListOf()

    // ------------------------------------------------------------------ derived helpers

    fun occupiedRooms(): List<Room> = rooms.filter { it.status == RoomStatus.OCCUPIED }

    fun localDateOf(instant: Instant?): LocalDate? = instant?.atZone(TEHRAN)?.toLocalDate()

    /** How many rooms were occupied on the given business date, derived from the stays. */
    fun occupiedOn(date: LocalDate): Int = stays.count { stay ->
        val from = localDateOf(stay.checkInAt) ?: return@count false
        val to = localDateOf(stay.actualCheckOutAt) ?: stay.plannedCheckOut
        !from.isAfter(date) && to.isAfter(date)
    }

    fun checkoutOn(date: LocalDate): List<Stay> =
        stays.filter { stay ->
            stay.status == StayStatus.IN_HOUSE && stay.plannedCheckOut == date ||
                localDateOf(stay.actualCheckOutAt) == date
        }

    fun arrivalsOn(date: LocalDate): List<Reservation> =
        reservations.filter { it.checkIn == date && it.status != ReservationStatus.CANCELLED }

    fun transactionsOn(date: LocalDate): List<Transaction> =
        transactions.filter { it.createdAt?.atZone(TEHRAN)?.toLocalDate() == date }

    fun transactionsBetween(from: LocalDate, to: LocalDate): List<Transaction> =
        transactions.filter { tx ->
            val date = tx.createdAt?.atZone(TEHRAN)?.toLocalDate()
            date != null && !date.isBefore(from) && !date.isAfter(to)
        }

    fun unreadNotifications(): Int = notifications.count { !it.isRead }

    fun alerts(): List<AlertItem> {
        val result = mutableListOf<AlertItem>()
        val openTasks = housekeepingTasks.count { it.status.isOpen }
        if (openTasks > 0) {
            result += AlertItem(
                id = "alert-hk",
                title = "$openTasks درخواست نظافت باز",
                message = "کارهای خدمات در انتظار تخصیص یا تأیید هستند.",
                severity = AlertSeverity.INFO,
                createdAt = at(0, 9, 10),
            )
        }
        val urgent = maintenanceTickets.firstOrNull {
            it.status.isOpen && it.priority == MaintenancePriority.URGENT
        }
        if (urgent != null) {
            result += AlertItem(
                id = "alert-mt",
                title = "تیکت فوری تعمیرات",
                message = "اتاق ${urgent.roomNumber} — ${urgent.title}",
                severity = AlertSeverity.WARNING,
                createdAt = urgent.createdAt,
            )
        }
        val pendingReservations = reservations.count { it.status == ReservationStatus.PENDING }
        if (pendingReservations > 0) {
            result += AlertItem(
                id = "alert-res",
                title = "$pendingReservations رزرو در انتظار تأیید",
                message = "تأیید رزروهای تلفنی بر عهده سرپرست پذیرش است.",
                severity = AlertSeverity.INFO,
                createdAt = at(0, 12, 0),
            )
        }
        val blocked = rooms.count { it.status == RoomStatus.OUT_OF_SERVICE }
        if (blocked > 0) {
            result += AlertItem(
                id = "alert-room",
                title = "$blocked اتاق خارج از سرویس",
                message = "اتاق ۲۰۶ تا پایان تعمیرات قابل واگذاری نیست.",
                severity = AlertSeverity.CRITICAL,
                createdAt = at(-1, 9, 0),
            )
        }
        return result.sortedByDescending { it.severity.ordinal }
    }

    fun recentActivities(): List<ActivityItem> = auditLogs
        .sortedByDescending { it.occurredAt }
        .take(8)
        .map { log ->
            ActivityItem(
                id = log.id,
                title = log.action,
                description = log.description,
                occurredAt = log.occurredAt,
                kind = when {
                    log.action.contains("رزرو") -> ActivityKind.RESERVATION
                    log.action.contains("پرداخت") || log.action.contains("رسید") -> ActivityKind.PAYMENT
                    log.action.contains("نظافت") -> ActivityKind.HOUSEKEEPING
                    log.action.contains("تیکت") -> ActivityKind.MAINTENANCE
                    log.action.contains("اتاق") -> ActivityKind.ROOM_STATUS
                    else -> ActivityKind.SYSTEM
                },
                actorName = log.actorName,
            )
        }
}
