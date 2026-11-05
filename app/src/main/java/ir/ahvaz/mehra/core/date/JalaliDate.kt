package ir.ahvaz.mehra.core.date

import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.common.toPersianDigits
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A Solar Hijri (Jalali) calendar date.
 *
 * Conversion is delegated to [JalaliConverter], a faithful port of the well tested
 * `jalaali-js` arithmetic (Borkowski algorithm with the 33-year break table) — never a
 * hand written "30-day months" approximation.
 *
 * The server always stores and transports ISO-8601 Gregorian dates/timestamps in UTC;
 * Jalali is a *presentation* concern only, but it is modelled explicitly because hotel
 * business dates (arrival / departure / report ranges) are Jalali days in Tehran.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    fun toGregorian(): LocalDate = JalaliConverter.jdnToGregorian(JalaliConverter.jalaliToJdn(year, month, day))

    val monthNameFa: String
        get() = if (month in 1..12) MONTH_NAMES_FA[month - 1] else ""

    val isLeapYear: Boolean get() = JalaliConverter.isLeapYear(year)

    val lengthOfMonth: Int
        get() = when {
            month <= 6 -> 31
            month <= 11 -> 30
            isLeapYear -> 30
            else -> 29
        }

    /** 0 = Saturday … 6 = Friday. */
    val dayOfWeek: Int
        get() {
            val dow = toGregorian().dayOfWeek
            return (dow.value + 1) % 7
        }

    val dayOfWeekNameFa: String get() = WEEKDAY_NAMES_FA[dayOfWeek]

    /** "۱۴۰۳/۱۲/۰۵" */
    fun formatShortFa(): String =
        "${year.toString().padStart(4, '0')}/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
            .toPersianDigits()

    /** "۱۴۰۳/۱۲/۰۵" (Latin digits, for machine readable export columns) */
    fun formatShortLatin(): String =
        "${year.toString().padStart(4, '0')}/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"

    /** "۵ اسفند ۱۴۰۳" */
    fun formatLongFa(): String = "${day.toString().toPersianDigits()} $monthNameFa ${year.toString().toPersianDigits()}"

    /** "پنجشنبه ۵ اسفند ۱۴۰۳" */
    fun formatWithWeekdayFa(): String = "$dayOfWeekNameFa ${formatLongFa()}"

    fun plusDays(days: Long): JalaliDate = from(toGregorian().plusDays(days))

    fun plusMonths(months: Long): JalaliDate = from(toGregorian().plusMonths(months))

    fun firstDayOfMonth(): JalaliDate = JalaliDate(year, month, 1)

    fun firstDayOfNextMonth(): JalaliDate =
        if (month == 12) JalaliDate(year + 1, 1, 1) else JalaliDate(year, month + 1, 1)

    /** Number of days between this date and [other] (positive when [other] is later). */
    fun daysUntil(other: JalaliDate): Long =
        toGregorian().toEpochDay() - other.toGregorian().toEpochDay()

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    override fun toString(): String = formatShortLatin()

    companion object {
        val MONTH_NAMES_FA = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )

        /** Persian and Afghan names are both accepted by the converters. */
        val MONTH_NAMES_AF = listOf(
            "حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله",
            "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت",
        )

        val WEEKDAY_NAMES_FA = listOf(
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه",
        )

        init {
            // The break table is only valid for its declared range.
            require(MONTH_NAMES_FA.size == 12 && WEEKDAY_NAMES_FA.size == 7)
        }

        fun from(date: LocalDate): JalaliDate = JalaliConverter.jdnToJalali(date.toEpochDay().toInt() + 2440588)

        fun today(): JalaliDate = from(TehranTime.today())

        fun isValid(year: Int, month: Int, day: Int): Boolean {
            if (month !in 1..12 || day < 1) return false
            if (year < 1200 || year > 3177) return false
            return day <= JalaliDate(year, month, 1).lengthOfMonth
        }

        /**
         * Parses user typed input. Accepts "1403/12/05", "1403-12-5", "۱۴۰۳/۱۲/۰۵"
         * and "14031205". Returns null when the input is not a valid Jalali date.
         */
        fun parse(raw: String?): JalaliDate? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.toLatinDigits().trim()
            val parts = cleaned.split('/', '-', '.', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                val d = parts[2].toIntOrNull() ?: return null
                return if (isValid(y, m, d)) JalaliDate(y, m, d) else null
            }
            if (cleaned.length == 8 && cleaned.all { it.isDigit() }) {
                val y = cleaned.substring(0, 4).toIntOrNull() ?: return null
                val m = cleaned.substring(4, 6).toIntOrNull() ?: return null
                val d = cleaned.substring(6, 8).toIntOrNull() ?: return null
                return if (isValid(y, m, d)) JalaliDate(y, m, d) else null
            }
            return null
        }
    }
}

/** Persian weekday index 0 = Saturday. */
fun DayOfWeek.toPersianIndex(): Int = (value + 1) % 7
