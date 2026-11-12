package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.date.JalaliConverter
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.repository.ReportRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports are generated locally from the demo dataset, so the customer can open every report,
 * change the range and export it to PDF/Excel exactly like the production app.
 */
@Singleton
class DemoReportRepository @Inject constructor(
    private val store: DemoStore,
) : ReportRepository {

    override suspend fun load(type: ReportType, from: LocalDate, to: LocalDate): AppResult<ReportTable> {
        delay(220)
        if (to.isBefore(from)) return demoFailure("بازه تاریخ نامعتبر است.")
        val table = when (type) {
            ReportType.OCCUPANCY -> occupancy(from, to)
            ReportType.CHECK_IN_OUT -> checkInOut(from, to)
            ReportType.RESERVATIONS -> reservations(from, to)
            ReportType.REVENUE -> revenue(from, to)
            ReportType.DEBTORS -> debtors()
            ReportType.STAFF_PERFORMANCE -> staffPerformance(from, to)
            ReportType.HOUSEKEEPING -> housekeeping(from, to)
            ReportType.MAINTENANCE -> maintenance(from, to)
        }
        return demoOk(table)
    }

    private fun base(type: ReportType, from: LocalDate, to: LocalDate, subtitle: String) = ReportTable(
        type = type,
        title = type.titleFa,
        subtitle = subtitle,
        columns = emptyList(),
        rows = emptyList(),
        from = from,
        to = to,
        generatedAt = Instant.now(),
    )

    private fun occupancy(from: LocalDate, to: LocalDate): ReportTable {
        val total = store.rooms.size
        val rows = mutableListOf<List<String>>()
        var day = from
        var occupiedSum = 0
        var days = 0
        while (!day.isAfter(to) && days < 62) {
            val occupied = store.occupiedOn(day)
            occupiedSum += occupied
            days += 1
            val percent = if (total == 0) 0 else occupied * 100 / total
            rows += listOf(
                JalaliConverter.localDateToJalali(day).formatShortFa(),
                jalaliWeekday(day),
                occupied.toString(),
                total.toString(),
                "$percent٪",
                (total - occupied).toString(),
            )
            day = day.plusDays(1)
        }
        val average = if (days == 0) 0 else occupiedSum / days
        val averagePercent = if (total == 0) 0 else average * 100 / total
        return base(ReportType.OCCUPANCY, from, to, "میانگین اشغال بازه: $averagePercent٪").copy(
            columns = listOf("تاریخ", "روز", "اتاق اشغال", "کل اتاق", "درصد اشغال", "اتاق آزاد"),
            rows = rows,
            totals = listOf(
                "میانگین", "", average.toString(), total.toString(), "$averagePercent٪", "",
            ),
            notes = listOf(
                "اتاق‌های در حال نظافت و تعمیر در ستون «اتاق آزاد» محاسبه نشده‌اند.",
                "مبنای محاسبه، اقامت‌های ثبت‌شده در سیستم است.",
            ),
        )
    }

    private fun checkInOut(from: LocalDate, to: LocalDate): ReportTable {
        val rows = store.stays
            .filter { stay ->
                val checkIn = store.localDateOf(stay.checkInAt)
                val checkOut = store.localDateOf(stay.actualCheckOutAt) ?: stay.plannedCheckOut
                checkIn != null && !checkIn.isAfter(to) && !checkOut.isBefore(from)
            }
            .sortedByDescending { it.checkInAt }
            .map { stay ->
                listOf(
                    stay.guestName,
                    stay.roomNumber,
                    store.localDateOf(stay.checkInAt)?.let { JalaliConverter.localDateToJalali(it).formatShortFa() } ?: "—",
                    store.localDateOf(stay.actualCheckOutAt)?.let { JalaliConverter.localDateToJalali(it).formatShortFa() } ?: "—",
                    formatMoneyFa(stay.totalRial),
                    stay.status.labelFa,
                )
            }
        return base(ReportType.CHECK_IN_OUT, from, to, "ورود و خروج‌های بازه انتخابی").copy(
            columns = listOf("مهمان", "اتاق", "ورود", "خروج", "مبلغ کل", "وضعیت"),
            rows = rows,
            notes = listOf("خروج‌های ثبت‌نشده با «—» نمایش داده می‌شوند."),
        )
    }

    private fun reservations(from: LocalDate, to: LocalDate): ReportTable {
        val items = store.reservations.filter {
            !it.checkOut.isBefore(from) && !it.checkIn.isAfter(to)
        }
        val rows = items.sortedByDescending { it.checkIn }.map { reservation ->
            listOf(
                reservation.code,
                reservation.guestName,
                reservation.roomNumber ?: "بدون اتاق",
                JalaliConverter.localDateToJalali(reservation.checkIn).formatShortFa(),
                JalaliConverter.localDateToJalali(reservation.checkOut).formatShortFa(),
                reservation.nights.toString(),
                formatMoneyFa(reservation.payableRial),
                reservation.status.labelFa,
            )
        }
        val cancelled = items.count { it.status == ir.ahvaz.mehra.domain.model.ReservationStatus.CANCELLED }
        val waitlist = items.count { it.status == ir.ahvaz.mehra.domain.model.ReservationStatus.WAITLIST }
        return base(ReportType.RESERVATIONS, from, to, "مجموع ${items.size} رزرو در بازه").copy(
            columns = listOf("کد رزرو", "مهمان", "اتاق", "ورود", "خروج", "شب", "مبلغ قابل پرداخت", "وضعیت"),
            rows = rows,
            totals = listOf(
                "جمع", "", "", "", "", "", formatMoneyFa(items.sumOf { it.payableRial }), "",
            ),
            notes = listOf(
                "رزروهای لغو‌شده: $cancelled — لیست انتظار: $waitlist",
                "مبلغ قابل پرداخت پس از کسر تخفیف محاسبه شده است.",
            ),
        )
    }

    private fun revenue(from: LocalDate, to: LocalDate): ReportTable {
        val items = store.transactionsBetween(from, to)
        val rows = mutableListOf<List<String>>()
        var day = from
        var guard = 0
        while (!day.isAfter(to) && guard < 62) {
            val dayItems = items.filter { store.localDateOf(it.createdAt) == day }
            val received = dayItems.filter { it.type == ir.ahvaz.mehra.domain.model.TransactionType.RECEIVE }
                .sumOf { it.amountRial }
            val paid = dayItems.filter {
                it.type == ir.ahvaz.mehra.domain.model.TransactionType.PAY ||
                    it.type == ir.ahvaz.mehra.domain.model.TransactionType.REFUND
            }.sumOf { it.amountRial }
            if (dayItems.isNotEmpty()) {
                rows += listOf(
                    JalaliConverter.localDateToJalali(day).formatShortFa(),
                    formatMoneyFa(received),
                    formatMoneyFa(paid),
                    formatMoneyFa(received - paid),
                    dayItems.size.toString(),
                )
            }
            day = day.plusDays(1)
            guard += 1
        }
        val receivedTotal = items.filter { it.type == ir.ahvaz.mehra.domain.model.TransactionType.RECEIVE }
            .sumOf { it.amountRial }
        val paidTotal = items.filter {
            it.type == ir.ahvaz.mehra.domain.model.TransactionType.PAY ||
                it.type == ir.ahvaz.mehra.domain.model.TransactionType.REFUND
        }.sumOf { it.amountRial }
        val byMethod = items.filter { it.type == ir.ahvaz.mehra.domain.model.TransactionType.RECEIVE }
            .groupBy { it.method }
            .map { (method, list) -> "${method.labelFa}: ${formatMoneyFa(list.sumOf { it.amountRial })}" }
        return base(ReportType.REVENUE, from, to, "دریافت‌ها، پرداخت‌ها و مانده روز").copy(
            columns = listOf("تاریخ", "دریافت", "پرداخت", "مانده روز", "تعداد تراکنش"),
            rows = rows,
            totals = listOf(
                "جمع بازه", formatMoneyFa(receivedTotal), formatMoneyFa(paidTotal),
                formatMoneyFa(receivedTotal - paidTotal), items.size.toString(),
            ),
            notes = byMethod.ifEmpty { listOf("در بازه انتخابی تراکنشی ثبت نشده است.") },
        )
    }

    private fun debtors(): ReportTable {
        val items = store.guests.filter { it.balanceRial > 0 }.sortedByDescending { it.balanceRial }
        val rows = items.map { guest ->
            val stay = store.activeStays().firstOrNull { it.guestId == guest.id }
            listOf(
                guest.fullName,
                guest.city ?: "—",
                stay?.roomNumber ?: "—",
                formatMoneyFa(guest.balanceRial),
                store.localDateOf(guest.lastStayAt)?.let {
                    JalaliConverter.localDateToJalali(it).formatShortFa()
                } ?: "—",
            )
        }
        return ReportTable(
            type = ReportType.DEBTORS,
            title = ReportType.DEBTORS.titleFa,
            subtitle = "مجموع بدهی: ${formatMoneyFa(items.sumOf { it.balanceRial })}",
            columns = listOf("مهمان", "شهر", "اتاق", "مانده حساب", "آخرین فعالیت"),
            rows = rows,
            totals = listOf("جمع", "", "", formatMoneyFa(items.sumOf { it.balanceRial }), ""),
            notes = listOf("مانده حساب از تفاوت مبلغ اقامت و پرداخت‌های ثبت‌شده محاسبه می‌شود."),
            generatedAt = Instant.now(),
        )
    }

    private fun staffPerformance(from: LocalDate, to: LocalDate): ReportTable {
        val housekeeping = store.housekeepingTasks.groupBy { it.assignedToName ?: "تخصیص نیافته" }
        val rows = housekeeping.map { (name, tasks) ->
            val done = tasks.count { it.status == ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus.DONE || it.status == ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus.APPROVED }
            val approved = tasks.count { it.status == ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus.APPROVED }
            listOf(name, "خدمات", tasks.size.toString(), done.toString(), approved.toString())
        }.sortedByDescending { it[2] }
        return base(ReportType.STAFF_PERFORMANCE, from, to, "عملکرد خدمات و تعمیرات در بازه").copy(
            columns = listOf("همکار", "واحد", "کارهای ثبت‌شده", "انجام‌شده", "تأییدشده"),
            rows = rows,
            notes = listOf("کارهای بدون مسئول در ردیف «تخصیص نیافته» جمع شده‌اند."),
        )
    }

    private fun housekeeping(from: LocalDate, to: LocalDate): ReportTable {
        val tasks = store.housekeepingTasks.filter { task ->
            val date = store.localDateOf(task.createdAt) ?: return@filter false
            !date.isBefore(from) && !date.isAfter(to)
        }
        val rows = tasks.map { task ->
            listOf(
                task.roomNumber,
                task.type.labelFa,
                task.status.labelFa,
                task.assignedToName ?: "تخصیص نیافته",
                store.localDateOf(task.createdAt)?.let {
                    JalaliConverter.localDateToJalali(it).formatShortFa()
                } ?: "—",
            )
        }
        return base(ReportType.HOUSEKEEPING, from, to, "وضعیت نظافت اتاق‌ها").copy(
            columns = listOf("اتاق", "نوع نظافت", "وضعیت", "مسئول", "تاریخ ثبت"),
            rows = rows,
            notes = listOf("کارهای باز: ${tasks.count { it.status.isOpen }}"),
        )
    }

    private fun maintenance(from: LocalDate, to: LocalDate): ReportTable {
        val tickets = store.maintenanceTickets.filter { ticket ->
            val date = store.localDateOf(ticket.createdAt) ?: return@filter false
            !date.isBefore(from) && !date.isAfter(to)
        }
        val rows = tickets.map { ticket ->
            listOf(
                ticket.code,
                ticket.roomNumber,
                ticket.title,
                ticket.priority.labelFa,
                ticket.status.labelFa,
                formatMoneyFa(ticket.costRial),
            )
        }
        return base(ReportType.MAINTENANCE, from, to, "خرابی‌ها، هزینه‌ها و وضعیت رفع نقص").copy(
            columns = listOf("کد", "اتاق", "عنوان", "اولویت", "وضعیت", "هزینه"),
            rows = rows,
            totals = listOf("جمع هزینه", "", "", "", "", formatMoneyFa(tickets.sumOf { it.costRial })),
            notes = listOf(
                "تیکت‌های باز: ${tickets.count { it.status.isOpen }}",
                "اتاق‌های در وضعیت ${RoomStatus.MAINTENANCE.labelFa}: " +
                    store.rooms.count { it.status == RoomStatus.MAINTENANCE }.toString(),
            ),
        )
    }

    private fun jalaliWeekday(date: LocalDate): String =
        JalaliConverter.localDateToJalali(date).dayOfWeekNameFa
}
