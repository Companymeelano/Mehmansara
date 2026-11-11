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
 * The data is entirely synthetic but internally consistent: a 24 room guesthouse in Ahvaz
 * with arrivals, in-house stays, cleaning tasks, maintenance tickets, a cash box and staff.
 * Dates are always relative to "today", so the demo never looks stale.
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

    private val defaultAmenities = listOf(
        "کولر گازی", "تلویزیون", "یخچال", "سرویس بهداشتی اختصاصی", "اینترنت بی‌سیم",
    )

    // ------------------------------------------------------------------ rooms

    private fun defRoom(
        number: String,
        capacity: Int,
        status: RoomStatus,
        guest: String? = null,
        stayId: String? = null,
        next: LocalDate? = null,
        notes: String? = null,
    ): Room {
        val type = types.first { it.capacity == capacity }
        return Room(
            id = "room-$number",
            roomNumber = number,
            floor = number.take(1).toInt(),
            capacity = capacity,
            roomTypeId = type.id,
            roomTypeName = type.title,
            ratePerNightRial = type.rate,
            status = status,
            amenities = defaultAmenities,
            notes = notes,
            currentGuestName = guest,
            currentStayId = stayId,
            nextReservationAt = next,
            updatedAt = at(0, 8, 45),
        )
    }

    val rooms: MutableList<Room> = mutableListOf(
        // موجودی اتاق‌ها بر اساس یادداشت دست‌نویس اپراتور: اتاق‌های ۱ تا ۱۷، ۲۰۷ و ۲۱۱
        // (تعداد تخت هر اتاق مطابق همان یادداشت)
        // طبقه اول
        defRoom("1", 5, RoomStatus.RESERVED, next = today.plusDays(1)),
        defRoom("2", 4, RoomStatus.OCCUPIED, guest = "علی محمدی", stayId = "stay-201"),
        defRoom("3", 3, RoomStatus.CLEANING),
        defRoom("4", 4, RoomStatus.OCCUPIED, guest = "زهرا کریمی", stayId = "stay-202"),
        defRoom("5", 5, RoomStatus.FREE),
        defRoom("6", 4, RoomStatus.OCCUPIED, guest = "مریم دهقانی", stayId = "stay-209"),
        defRoom("7", 3, RoomStatus.OCCUPIED, guest = "رضا صادقی", stayId = "stay-203"),
        defRoom("8", 3, RoomStatus.RESERVED, next = today.plusDays(2)),
        defRoom("9", 4, RoomStatus.OCCUPIED, guest = "فاطمه موسوی", stayId = "stay-204"),
        defRoom("10", 3, RoomStatus.CLEANING),
        defRoom("11", 3, RoomStatus.OCCUPIED, guest = "حسین بهرامی", stayId = "stay-205"),
        defRoom("12", 5, RoomStatus.OUT_OF_SERVICE, notes = "نشتی آب سرویس بهداشتی — تا پایان تعمیرات قابل واگذاری نیست"),
        defRoom("13", 4, RoomStatus.OCCUPIED, guest = "امیر قاسمی", stayId = "stay-210"),
        defRoom("14", 6, RoomStatus.OCCUPIED, guest = "سکینه فرجی", stayId = "stay-206"),
        defRoom("15", 3, RoomStatus.OCCUPIED, guest = "سارا محمدی", stayId = "stay-208"),
        defRoom("16", 3, RoomStatus.RESERVED, next = today.plusDays(3)),
        defRoom("17", 3, RoomStatus.MAINTENANCE, notes = "کولر گازی خراب است — تیکت MT-2201"),
        // طبقه دوم
        defRoom("207", 2, RoomStatus.CLEANING),
        defRoom("211", 1, RoomStatus.OCCUPIED, guest = "امیر قاسمی", stayId = "stay-207"),
    )

    fun room(roomId: String): Room? = rooms.firstOrNull { it.id == roomId }

    fun roomByNumber(number: String): Room? = rooms.firstOrNull { it.roomNumber == number }

    // ------------------------------------------------------------------ guests

    val guests: MutableList<Guest> = mutableListOf(
        Guest(
            id = "guest-01", firstName = "علی", lastName = "محمدی",
            nationalId = "2281945713", phone = "09161234567",
            city = "اهواز", address = "کیانپارس، خیابان ۱۰ شرقی، پلاک ۲۴",
            balanceRial = 3_500_000, totalStays = 4, lastStayAt = at(0, 14, 20),
            companions = listOf(GuestCompanion(fullName = "سمیه محمدی", relation = "همسر")),
        ),
        Guest(
            id = "guest-02", firstName = "زهرا", lastName = "کریمی",
            nationalId = "3451229871", phone = "09161112233",
            city = "دزفول", address = "خیابان شهید بهشتی، کوچه ۷",
            balanceRial = 0, totalStays = 7, lastStayAt = at(-1, 17, 40),
        ),
        Guest(
            id = "guest-03", firstName = "رضا", lastName = "صادقی",
            nationalId = "2290447781", phone = "09123334455",
            city = "آبادان", address = "امیری، خیابان ۱۵",
            balanceRial = 3_600_000, totalStays = 3, lastStayAt = at(-3, 12, 10),
        ),
        Guest(
            id = "guest-04", firstName = "فاطمه", lastName = "موسوی",
            nationalId = "2281156634", phone = "06132223344",
            city = "اهواز", address = "زیتون کارمندی، خیابان ۱۲",
            balanceRial = 6_600_000, totalStays = 6, lastStayAt = at(0, 12, 30),
            companions = listOf(
                GuestCompanion(fullName = "حسین موسوی", relation = "همسر"),
                GuestCompanion(fullName = "ریحانه موسوی", relation = "دختر"),
            ),
        ),
        Guest(
            id = "guest-05", firstName = "حسین", lastName = "بهرامی",
            nationalId = "3901445567", phone = "09167778899",
            city = "شوشتر", address = "خیابان طالقانی، پلاک ۸۸",
            balanceRial = 0, totalStays = 2, lastStayAt = at(-2, 10, 0),
        ),
        Guest(
            id = "guest-06", firstName = "مریم", lastName = "دهقانی",
            nationalId = "2298855671", phone = "09163887766",
            city = "بهبهان", address = "بلوار امام، کوچه لاله ۳",
            balanceRial = 5_600_000, totalStays = 5, lastStayAt = at(-4, 11, 15),
        ),
        Guest(
            id = "guest-07", firstName = "امیر", lastName = "قاسمی",
            nationalId = "2240112233", phone = "09145556677",
            city = "خرمشهر", address = "خیابان ۴۰ متری، پلاک ۱۲",
            balanceRial = 12_500_000, totalStays = 9, lastStayAt = at(-1, 16, 45),
            companions = listOf(
                GuestCompanion(fullName = "نرگس قاسمی", relation = "همسر"),
                GuestCompanion(fullName = "پویا قاسمی", relation = "پسر"),
            ),
        ),
        Guest(
            id = "guest-08", firstName = "سکینه", lastName = "فرجی",
            nationalId = "2285443322", phone = "09169001122",
            city = "ماهشهر", address = "خیابان امام خمینی، کوچه ۵",
            balanceRial = 0, totalStays = 4, lastStayAt = at(-1, 8, 30),
        ),
        Guest(
            id = "guest-09", firstName = "محمدجواد", lastName = "هاشمی",
            nationalId = "2282233445", phone = "09123344556",
            city = "مسجدسلیمان", address = "خیابان نفت، پلاک ۶۱",
            balanceRial = 1_200_000, totalStays = 2, lastStayAt = at(-20, 19, 0),
        ),
        Guest(
            id = "guest-10", firstName = "نگار", lastName = "شریفی",
            nationalId = "2286677889", phone = "06135566778",
            city = "اهواز", address = "گلستان، خیابان ۹ غربی",
            balanceRial = 0, totalStays = 1, lastStayAt = at(-30, 13, 20),
        ),
        Guest(
            id = "guest-11", firstName = "یاسر", lastName = "عبادی",
            nationalId = "2293344556", phone = "09169988776",
            city = "رامهرمز", address = "خیابان دانشگاه، کوچه ۲",
            balanceRial = 0, totalStays = 3, lastStayAt = at(-14, 20, 0),
        ),
        Guest(
            id = "guest-12", firstName = "سمیرا", lastName = "حیدری",
            nationalId = "2297788990", phone = "09162233445",
            city = "شادگان", address = "خیابان شهید رجایی، پلاک ۳۳",
            balanceRial = 900_000, totalStays = 2, lastStayAt = at(-6, 18, 20),
        ),
    )

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

    val reservations: MutableList<Reservation> = mutableListOf(
        // در اقامت
        defReservation(
            "res-101", "MR-34101", guest("guest-01")!!, roomByNumber("2"),
            today.minusDays(2), today.plusDays(1), ReservationStatus.CHECKED_IN,
            guestsCount = 2, paid = 4_000_000,
            companions = listOf(GuestCompanion(fullName = "سمیه محمدی", relation = "همسر")),
        ),
        defReservation(
            "res-102", "MR-34102", guest("guest-02")!!, roomByNumber("4"),
            today.minusDays(1), today.plusDays(2), ReservationStatus.CHECKED_IN,
            guestsCount = 1, paid = 7_500_000,
        ),
        defReservation(
            "res-103", "MR-34103", guest("guest-03")!!, roomByNumber("7"),
            today.minusDays(3), today, ReservationStatus.CHECKED_IN,
            guestsCount = 3, paid = 6_000_000,
        ),
        defReservation(
            "res-104", "MR-34104", guest("guest-04")!!, roomByNumber("9"),
            today, today.plusDays(3), ReservationStatus.CHECKED_IN,
            guestsCount = 3, paid = 3_000_000,
            companions = listOf(
                GuestCompanion(fullName = "حسین موسوی", relation = "همسر"),
                GuestCompanion(fullName = "ریحانه موسوی", relation = "دختر"),
            ),
        ),
        defReservation(
            "res-105", "MR-34105", guest("guest-05")!!, roomByNumber("11"),
            today.minusDays(2), today.plusDays(2), ReservationStatus.CHECKED_IN,
            guestsCount = 4, paid = 12_800_000,
        ),
        defReservation(
            "res-106", "MR-34106", guest("guest-06")!!, roomByNumber("14"),
            today.minusDays(4), today.plusDays(2), ReservationStatus.CHECKED_IN,
            guestsCount = 4, paid = 10_000_000,
        ),
        defReservation(
            "res-107", "MR-34107", guest("guest-07")!!, roomByNumber("211"),
            today.minusDays(1), today.plusDays(4), ReservationStatus.CHECKED_IN,
            guestsCount = 5, paid = 20_000_000,
            companions = listOf(
                GuestCompanion(fullName = "نرگس قاسمی", relation = "همسر"),
                GuestCompanion(fullName = "پویا قاسمی", relation = "پسر"),
            ),
        ),
        defReservation(
            "res-108", "MR-34108", guest("guest-08")!!, roomByNumber("15"),
            today.minusDays(1), today.plusDays(1), ReservationStatus.CHECKED_IN,
            guestsCount = 4, paid = 13_000_000,
        ),
        // رزروهای آینده
        defReservation(
            "res-109", "MR-34109", guest("guest-09")!!, roomByNumber("1"),
            today.plusDays(1), today.plusDays(3), ReservationStatus.CONFIRMED,
            guestsCount = 2, discount = 500_000, paid = 3_000_000,
        ),
        defReservation(
            "res-110", "MR-34110", guest("guest-10")!!, roomByNumber("8"),
            today.plusDays(2), today.plusDays(5), ReservationStatus.CONFIRMED,
            guestsCount = 3, paid = 0,
        ),
        defReservation(
            "res-111", "MR-34111", guest("guest-11")!!, roomByNumber("16"),
            today.plusDays(3), today.plusDays(6), ReservationStatus.CONFIRMED,
            guestsCount = 4, paid = 5_000_000,
        ),
        defReservation(
            "res-112", "MR-34112", guest("guest-12")!!, roomByNumber("10"),
            today.plusDays(1), today.plusDays(2), ReservationStatus.CONFIRMED,
            guestsCount = 4, paid = 0,
            notes = "درخواست اتاق رو به حیاط",
        ),
        defReservation(
            "res-113", "MR-34113", guest("guest-02")!!, roomByNumber("5"),
            today.plusDays(4), today.plusDays(7), ReservationStatus.CONFIRMED,
            guestsCount = 5, paid = 2_000_000,
        ),
        // در انتظار تأیید
        defReservation(
            "res-114", "MR-34114", guest("guest-03")!!, roomByNumber("5"),
            today.plusDays(2), today.plusDays(4), ReservationStatus.PENDING,
            guestsCount = 2, paid = 0,
        ),
        defReservation(
            "res-115", "MR-34115", guest("guest-04")!!, roomByNumber("6"),
            today.plusDays(5), today.plusDays(6), ReservationStatus.PENDING,
            guestsCount = 2, paid = 0,
            notes = "رزرو تلفنی — تأیید مدیریت لازم است",
        ),
        // لیست انتظار
        defReservation(
            "res-116", "MR-34116", guest("guest-05")!!, null,
            today.plusDays(2), today.plusDays(4), ReservationStatus.WAITLIST,
            guestsCount = 4, paid = 0,
            notes = "در انتظار آزاد شدن اتاق چهار تخته",
        ),
        // لغو شده
        defReservation(
            "res-117", "MR-34117", guest("guest-06")!!, roomByNumber("13"),
            today.minusDays(1), today.plusDays(1), ReservationStatus.CANCELLED,
            guestsCount = 3, paid = 0,
            notes = "انصراف مهمان به دلیل تغییر برنامه سفر",
        ),
        // خروج شده
        defReservation(
            "res-118", "MR-34118", guest("guest-07")!!, roomByNumber("6"),
            today.minusDays(9), today.minusDays(7), ReservationStatus.CHECKED_OUT,
            guestsCount = 4, paid = 13_000_000,
        ),
        defReservation(
            "res-119", "MR-34119", guest("guest-08")!!, roomByNumber("15"),
            today.minusDays(12), today.minusDays(10), ReservationStatus.CHECKED_OUT,
            guestsCount = 3, paid = 7_800_000,
        ),
    )

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

    val stays: MutableList<Stay> = mutableListOf(
        defStay(
            "stay-201", reservation("res-101")!!, roomByNumber("2")!!,
            at(-2, 14, 20), today.plusDays(1), total = 7_500_000, paid = 4_000_000,
        ),
        defStay(
            "stay-202", reservation("res-102")!!, roomByNumber("4")!!,
            at(-1, 17, 40), today.plusDays(2), total = 7_500_000, paid = 7_500_000,
        ),
        defStay(
            "stay-203", reservation("res-103")!!, roomByNumber("7")!!,
            at(-3, 12, 10), today, total = 9_600_000, paid = 6_000_000,
        ),
        defStay(
            "stay-204", reservation("res-104")!!, roomByNumber("9")!!,
            at(0, 12, 30), today.plusDays(3), total = 9_600_000, paid = 3_000_000,
        ),
        defStay(
            "stay-205", reservation("res-105")!!, roomByNumber("11")!!,
            at(-2, 10, 0), today.plusDays(2), total = 12_800_000, paid = 12_800_000,
        ),
        defStay(
            "stay-206", reservation("res-106")!!, roomByNumber("14")!!,
            at(-4, 11, 15), today.plusDays(2), total = 15_600_000, paid = 10_000_000,
        ),
        defStay(
            "stay-207", reservation("res-107")!!, roomByNumber("211")!!,
            at(-1, 16, 45), today.plusDays(4), total = 32_500_000, paid = 20_000_000,
        ),
        defStay(
            "stay-208", reservation("res-108")!!, roomByNumber("15")!!,
            at(-1, 8, 30), today.plusDays(1), total = 13_000_000, paid = 13_000_000,
        ),
        defStay(
            "stay-209", reservation("res-118")!!, roomByNumber("6")!!,
            at(-9, 15, 0), today.minusDays(7), StayStatus.CHECKED_OUT, at(-7, 10, 30),
            total = 13_000_000, paid = 13_000_000,
        ),
        defStay(
            "stay-210", reservation("res-119")!!, roomByNumber("15")!!,
            at(-12, 18, 20), today.minusDays(10), StayStatus.CHECKED_OUT, at(-10, 9, 50),
            total = 7_800_000, paid = 7_800_000,
        ),
    )

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

    val transactions: MutableList<Transaction> = mutableListOf(
        defTransaction(
            "tx-01", "TR-1042", TransactionType.RECEIVE, 4_000_000, PaymentMethod.CASH,
            "پیش‌پرداخت رزرو", 0, 9, 20, "علی محمدی", "102", reservationId = "res-101",
        ),
        defTransaction(
            "tx-02", "TR-1043", TransactionType.RECEIVE, 13_000_000, PaymentMethod.POS,
            "تسویه کامل اقامت", 0, 8, 40, "سکینه فرجی", "403", reservationId = "res-108",
        ),
        defTransaction(
            "tx-03", "TR-1044", TransactionType.RECEIVE, 3_000_000, PaymentMethod.POS,
            "پرداخت مرحله دوم", 0, 10, 5, "فاطمه موسوی", "203", reservationId = "res-104",
        ),
        defTransaction(
            "tx-04", "TR-1045", TransactionType.PAY, 1_850_000, PaymentMethod.CASH,
            "خرید ملزومات نظافت", 0, 11, 30, operator = "مهدی نیسی",
        ),
        defTransaction(
            "tx-05", "TR-1046", TransactionType.RECEIVE, 2_500_000, PaymentMethod.CARD,
            "بیعانه رزرو تلفنی", 0, 12, 10, "محمدجواد هاشمی", "101", reservationId = "res-109",
        ),
        defTransaction(
            "tx-06", "TR-1047", TransactionType.DISCOUNT, 500_000, PaymentMethod.CASH,
            "تخفیف فرهنگیان", 0, 12, 12, "محمدجواد هاشمی", "101", reservationId = "res-109",
        ),
        defTransaction(
            "tx-07", "TR-1048", TransactionType.RECEIVE, 6_000_000, PaymentMethod.POS,
            "تسویه مرحله اول", -1, 19, 40, "رضا صادقی", "201", reservationId = "res-103",
        ),
        defTransaction(
            "tx-08", "TR-1049", TransactionType.RECEIVE, 20_000_000, PaymentMethod.POS,
            "پیش‌پرداخت سوئیت", -1, 16, 50, "امیر قاسمی", "401", reservationId = "res-107",
        ),
        defTransaction(
            "tx-09", "TR-1050", TransactionType.RECEIVE, 7_500_000, PaymentMethod.POS,
            "تسویه کامل اقامت", -1, 17, 45, "زهرا کریمی", "104", reservationId = "res-102",
        ),
        defTransaction(
            "tx-10", "TR-1051", TransactionType.RECEIVE, 10_000_000, PaymentMethod.TRANSFER,
            "واریز حواله بانکی", -2, 13, 20, "مریم دهقانی", "302", reservationId = "res-106",
        ),
        defTransaction(
            "tx-11", "TR-1052", TransactionType.RECEIVE, 12_800_000, PaymentMethod.CASH,
            "تسویه کامل اقامت", -2, 10, 15, "حسین بهرامی", "205", reservationId = "res-105",
        ),
        defTransaction(
            "tx-12", "TR-1053", TransactionType.REFUND, 800_000, PaymentMethod.CASH,
            "بازگشت وجه رزرو لغو شده", -2, 15, 5, "مریم دهقانی", reservationId = "res-117",
        ),
        defTransaction(
            "tx-13", "TR-1054", TransactionType.PAY, 4_200_000, PaymentMethod.POS,
            "تعمیر کولر گازی اتاق ۳۰۵", -3, 14, 30, operator = "مهدی نیسی",
        ),
        defTransaction(
            "tx-14", "TR-1055", TransactionType.RECEIVE, 3_000_000, PaymentMethod.CASH,
            "پیش‌پرداخت رزرو", -3, 12, 25, "رضا صادقی", "201", reservationId = "res-103",
        ),
        defTransaction(
            "tx-15", "TR-1056", TransactionType.RECEIVE, 2_000_000, PaymentMethod.CARD,
            "بیعانه رزرو", -4, 18, 10, "نگار شریفی", "202", reservationId = "res-110",
        ),
        defTransaction(
            "tx-16", "TR-1057", TransactionType.RECEIVE, 5_000_000, PaymentMethod.POS,
            "بیعانه رزرو", -5, 11, 45, "یاسر عبادی", "304", reservationId = "res-111",
        ),
        defTransaction(
            "tx-17", "TR-1058", TransactionType.PAY, 900_000, PaymentMethod.CASH,
            "هزینه آبدارخانه", -5, 9, 30, operator = "فاطمه رسولی",
        ),
        defTransaction(
            "tx-18", "TR-1059", TransactionType.RECEIVE, 9_000_000, PaymentMethod.TRANSFER,
            "واریز حواله بانکی", -6, 16, 0, "امیر قاسمی", "401", reservationId = "res-107",
        ),
        defTransaction(
            "tx-19", "TR-1060", TransactionType.RECEIVE, 3_300_000, PaymentMethod.POS,
            "اجاره یک‌شبه", -7, 20, 15, "مهمان افتخاری", "404",
        ),
        defTransaction(
            "tx-20", "TR-1061", TransactionType.RECEIVE, 4_400_000, PaymentMethod.CASH,
            "اجاره دو شب", -8, 13, 10, "مهمان افتخاری", "303",
        ),
        defTransaction(
            "tx-21", "TR-1062", TransactionType.PAY, 2_600_000, PaymentMethod.CHECK,
            "خرید پتو و ملحفه", -9, 10, 40, operator = "محمد صالحی",
        ),
        defTransaction(
            "tx-22", "TR-1063", TransactionType.RECEIVE, 8_000_000, PaymentMethod.POS,
            "تسویه اقامت", -10, 9, 55, "مهمان افتخاری", "301",
        ),
        defTransaction(
            "tx-23", "TR-1064", TransactionType.RECEIVE, 6_200_000, PaymentMethod.CARD,
            "دریافت علی‌الحساب", -12, 17, 30, "محمدجواد هاشمی",
        ),
        defTransaction(
            "tx-24", "TR-1065", TransactionType.DISCOUNT, 400_000, PaymentMethod.CASH,
            "تخفیف ایثارگران", -13, 12, 0, "یاسر عبادی",
        ),
        defTransaction(
            "tx-25", "TR-1066", TransactionType.RECEIVE, 11_500_000, PaymentMethod.POS,
            "تسویه اقامت گروهی", -15, 15, 20, "مهمان افتخاری", "205",
        ),
    )

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

    val housekeepingTasks: MutableList<HousekeepingTask> = mutableListOf(
        HousekeepingTask(
            id = "hk-01", roomId = "room-3", roomNumber = "3",
            type = CleaningType.CHECKOUT, status = HousekeepingTaskStatus.PENDING,
            notes = "نظافت پس از خروج مهمان", createdAt = at(0, 9, 10),
        ),
        HousekeepingTask(
            id = "hk-02", roomId = "room-10", roomNumber = "10",
            type = CleaningType.DAILY, status = HousekeepingTaskStatus.ASSIGNED,
            assignedToId = "staff-hk-1", assignedToName = "فاطمه رسولی",
            createdAt = at(0, 9, 30), notes = "تعویض ملحفه و جارو",
        ),
        HousekeepingTask(
            id = "hk-03", roomId = "room-207", roomNumber = "207",
            type = CleaningType.DEEP, status = HousekeepingTaskStatus.IN_PROGRESS,
            assignedToId = "staff-hk-2", assignedToName = "رقیه نیسی",
            createdAt = at(0, 8, 0), startedAt = at(0, 10, 0),
            notes = "نظافت اساسی و شست‌وشوی پرده",
        ),
        HousekeepingTask(
            id = "hk-04", roomId = "room-14", roomNumber = "14",
            type = CleaningType.DAILY, status = HousekeepingTaskStatus.DONE,
            assignedToId = "staff-hk-1", assignedToName = "فاطمه رسولی",
            createdAt = at(0, 8, 10), startedAt = at(0, 10, 20), finishedAt = at(0, 11, 20),
            notes = "نظافت روزانه انجام شد",
        ),
        HousekeepingTask(
            id = "hk-05", roomId = "room-211", roomNumber = "211",
            type = CleaningType.TURNDOWN, status = HousekeepingTaskStatus.APPROVED,
            assignedToId = "staff-hk-3", assignedToName = "سمیرا حیدری",
            createdAt = at(-1, 18, 0), startedAt = at(-1, 18, 30), finishedAt = at(-1, 19, 0),
            approvedAt = at(-1, 19, 20), approvedByName = DEMO_OPERATOR,
            notes = "آماده‌سازی شب — تأیید سرپرست پذیرش",
        ),
        HousekeepingTask(
            id = "hk-06", roomId = "room-5", roomNumber = "5",
            type = CleaningType.CHECKOUT, status = HousekeepingTaskStatus.DONE,
            assignedToId = "staff-hk-4", assignedToName = "علی زبیدی",
            createdAt = at(-1, 11, 0), startedAt = at(-1, 11, 15), finishedAt = at(-1, 11, 55),
        ),
        HousekeepingTask(
            id = "hk-07", roomId = "room-9", roomNumber = "9",
            type = CleaningType.DAILY, status = HousekeepingTaskStatus.PENDING,
            createdAt = at(0, 12, 40), notes = "درخواست مهمان برای نظافت بعدازظهر",
        ),
    )

    val maintenanceTickets: MutableList<MaintenanceTicket> = mutableListOf(
        MaintenanceTicket(
            id = "mt-01", code = "MT-2201", roomId = "room-17", roomNumber = "17",
            title = "خرید کولر گازی",
            description = "کولر گازی اتاق روشن نمی‌شود و کمپرسور صدا می‌دهد.",
            priority = MaintenancePriority.URGENT, status = MaintenanceStatus.IN_PROGRESS,
            assigneeId = "staff-tc-1", assigneeName = "مهدی نیسی",
            costRial = 4_200_000, reporterName = DEMO_OPERATOR,
            createdAt = at(-3, 9, 15), roomReleased = false,
        ),
        MaintenanceTicket(
            id = "mt-02", code = "MT-2202", roomId = "room-12", roomNumber = "12",
            title = "نشتی آب سرویس بهداشتی",
            description = "نشتی از لوله زیر روشویی، کف سرویس خیس می‌شود.",
            priority = MaintenancePriority.HIGH, status = MaintenanceStatus.ASSIGNED,
            assigneeId = "staff-tc-2", assigneeName = "علیرضا زبیدی",
            reporterName = "رقیه نیسی", createdAt = at(-1, 8, 40),
        ),
        MaintenanceTicket(
            id = "mt-03", code = "MT-2203", roomId = "room-10", roomNumber = "10",
            title = "لامپ اتاق سوخته",
            description = "دو عدد لامپ سقفی نیاز به تعویض دارد.",
            priority = MaintenancePriority.LOW, status = MaintenanceStatus.OPEN,
            reporterName = "فاطمه رسولی", createdAt = at(0, 8, 50),
        ),
        MaintenanceTicket(
            id = "mt-04", code = "MT-2204", roomId = "room-13", roomNumber = "13",
            title = "قفل درب ورودی",
            description = "قفل درب کند باز می‌شود.",
            priority = MaintenancePriority.NORMAL, status = MaintenanceStatus.RESOLVED,
            assigneeId = "staff-tc-3", assigneeName = "سعید فرجی",
            costRial = 850_000, reporterName = DEMO_OPERATOR,
            createdAt = at(-5, 10, 0), resolvedAt = at(-4, 16, 30), roomReleased = true,
        ),
        MaintenanceTicket(
            id = "mt-05", code = "MT-2205", roomId = "room-5", roomNumber = "5",
            title = "تلویزیون روشن نمی‌شود",
            description = "برد تلویزیون خراب است؛ در انتظار قطعه یدکی.",
            priority = MaintenancePriority.NORMAL, status = MaintenanceStatus.WAITING_PARTS,
            assigneeId = "staff-tc-1", assigneeName = "مهدی نیسی",
            reporterName = "فاطمه رسولی", createdAt = at(-2, 17, 10),
        ),
    )

    // ------------------------------------------------------------------ notifications, users, audit

    val notifications: MutableList<NotificationItem> = mutableListOf(
        NotificationItem(
            "nt-01", "رزرو جدید ثبت شد",
            "رزرو MR-34113 برای زهرا کریمی (سوئیت ۴۰۵) ثبت شد.",
            AlertSeverity.INFO, at(0, 9, 22), isRead = false, kind = ActivityKind.RESERVATION,
        ),
        NotificationItem(
            "nt-02", "پرداخت دریافت شد",
            "مبلغ ۱۳٬۰۰۰٬۰۰۰ ریال برای اتاق ۴۰۳ با کارت‌خوان ثبت شد.",
            AlertSeverity.INFO, at(0, 8, 41), isRead = false, kind = ActivityKind.PAYMENT,
        ),
        NotificationItem(
            "nt-03", "درخواست نظافت",
            "اتاق ۱۰۳ برای نظافت پس از خروج در صف قرار گرفت.",
            AlertSeverity.INFO, at(0, 9, 10), isRead = false, kind = ActivityKind.HOUSEKEEPING,
        ),
        NotificationItem(
            "nt-04", "تیکت فوری تعمیرات",
            "کولر گازی اتاق ۳۰۵ در وضعیت «در حال تعمیر» است؛ اتاق از سرویس خارج است.",
            AlertSeverity.WARNING, at(0, 8, 5), isRead = false, kind = ActivityKind.MAINTENANCE,
        ),
        NotificationItem(
            "nt-05", "خروج امروز",
            "۳ مهمان امروز تسویه و خروج می‌کنند.",
            AlertSeverity.INFO, at(0, 7, 30), isRead = true, kind = ActivityKind.CHECK_OUT,
        ),
        NotificationItem(
            "nt-06", "گزارش مالی دیروز",
            "گزارش درآمد و صندوق دیروز آماده مشاهده است.",
            AlertSeverity.INFO, at(-1, 23, 50), isRead = true, kind = ActivityKind.SYSTEM,
        ),
    )

    val users: MutableList<AppUser> = mutableListOf(
        AppUser(
            "user-01", "سید مرتضی موسوی", "admin", "SuperAdmin", "مدیر کل",
            "09161112233", isActive = true, lastLoginAt = at(0, 7, 55),
        ),
        AppUser(
            "user-02", "زهرا کریمی", "z.karimi", "FrontOfficeSupervisor", "سرپرست پذیرش",
            "09162223344", isActive = true, lastLoginAt = at(0, 7, 40),
        ),
        AppUser(
            "user-03", "نگار شریفی", "n.sharifi", "Receptionist", "پذیرشگر",
            "06133334455", isActive = true, lastLoginAt = at(0, 8, 5),
        ),
        AppUser(
            "user-04", "محمد صالحی", "m.salehi", "Accountant", "مالی",
            "09164445566", isActive = true, lastLoginAt = at(-1, 20, 10),
        ),
        AppUser(
            "user-05", "فاطمه رسولی", "f.rasouli", "Housekeeping", "خدمات",
            null, isActive = true, lastLoginAt = at(0, 7, 15),
        ),
        AppUser(
            "user-06", "مهدی نیسی", "m.neisi", "Maintenance", "تعمیرات",
            "09167778899", isActive = false, lastLoginAt = at(-6, 18, 0),
        ),
    )

    val auditLogs: MutableList<AuditLogEntry> = mutableListOf(
        AuditLogEntry(
            "au-01", "ورود به سیستم", "Session", null,
            "ورود موفق از دستگاه اندروید", "زهرا کریمی", at(0, 7, 40),
        ),
        AuditLogEntry(
            "au-02", "ثبت رزرو", "Reservation", "res-113",
            "رزرو MR-34113 ثبت شد", "زهرا کریمی", at(0, 9, 22),
        ),
        AuditLogEntry(
            "au-03", "ثبت پرداخت", "Transaction", "tx-02",
            "دریافت ۱۳٬۰۰۰٬۰۰۰ ریال — کارت‌خوان", "محمد صالحی", at(0, 8, 41),
        ),
        AuditLogEntry(
            "au-04", "تغییر وضعیت اتاق", "Room", "room-3",
            "اتاق ۱۰۳ از اشغال به در حال نظافت تغییر کرد", "نگار شریفی", at(0, 8, 35),
        ),
        AuditLogEntry(
            "au-05", "ثبت درخواست نظافت", "HousekeepingTask", "hk-01",
            "نظافت پس از خروج برای اتاق ۱۰۳", "نگار شریفی", at(0, 9, 10),
        ),
        AuditLogEntry(
            "au-06", "تأیید نظافت", "HousekeepingTask", "hk-05",
            "نظافت شب اتاق ۴۰۱ تأیید شد", "زهرا کریمی", at(-1, 19, 20),
        ),
        AuditLogEntry(
            "au-07", "صدور رسید", "Stay", "stay-208",
            "رسید تسویه اقامت اتاق ۴۰۳ صادر شد", "محمد صالحی", at(0, 8, 42),
        ),
        AuditLogEntry(
            "au-08", "به‌روزرسانی تیکت", "MaintenanceTicket", "mt-01",
            "وضعیت تیکت MT-2201 به در حال تعمیر تغییر کرد", "مهدی نیسی", at(-1, 11, 0),
        ),
        AuditLogEntry(
            "au-09", "لغو رزرو", "Reservation", "res-117",
            "رزرو MR-34117 به درخواست مهمان لغو شد", "زهرا کریمی", at(-1, 10, 12),
        ),
        AuditLogEntry(
            "au-10", "ویرایش کاربر", "User", "user-06",
            "حساب کاربری مهدی نیسی موقتاً غیرفعال شد", "سید مرتضی موسوی", at(-6, 17, 55),
        ),
    )

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
