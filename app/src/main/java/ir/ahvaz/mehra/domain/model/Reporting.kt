package ir.ahvaz.mehra.domain.model

import ir.ahvaz.mehra.core.date.JalaliDate
import java.time.Instant
import java.time.LocalDate

enum class ReportType(
    val key: String,
    val titleFa: String,
    val descriptionFa: String,
) {
    OCCUPANCY("occupancy", "گزارش اشغال اتاق‌ها", "درصد اشغال، اتاق‌های آزاد و اشغال روزانه"),
    CHECK_IN_OUT("check-in-out", "گزارش ورود و خروج", "فهرست ورودها و خروج‌های بازه انتخابی"),
    RESERVATIONS("reservations", "گزارش رزروها", "رزروهای ثبت‌شده، لغو‌شده و لیست انتظار"),
    REVENUE("revenue", "گزارش درآمد", "دریافت‌ها، پرداخت‌ها و تفکیک روش پرداخت"),
    DEBTORS("debtors", "گزارش بدهکاران", "مهمانان دارای مانده حساب"),
    STAFF_PERFORMANCE("staff-performance", "گزارش عملکرد کارکنان", "عملکرد پذیرش، خدمات و تعمیرات"),
    HOUSEKEEPING("housekeeping", "گزارش خدمات", "وضعیت نظافت و زمان انجام کارها"),
    MAINTENANCE("maintenance", "گزارش تعمیرات", "خرابی‌ها، هزینه‌ها و زمان رفع نقص");

    companion object {
        fun fromKey(value: String?): ReportType? = entries.firstOrNull { it.key == value }
    }
}

data class ReportDateRange(
    val from: JalaliDate,
    val to: JalaliDate,
) {
    val fromGregorian: LocalDate get() = from.toGregorian()
    val toGregorian: LocalDate get() = to.toGregorian()
    val isValid: Boolean get() = from <= to
}

/** A fully rendered, presentation ready report (columns + string rows). */
data class ReportTable(
    val type: ReportType,
    val title: String,
    val subtitle: String? = null,
    val columns: List<String>,
    val rows: List<List<String>>,
    val totals: List<String>? = null,
    val notes: List<String> = emptyList(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val generatedAt: Instant = Instant.now(),
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
