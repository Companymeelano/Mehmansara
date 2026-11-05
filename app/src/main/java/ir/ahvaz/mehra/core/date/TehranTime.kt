package ir.ahvaz.mehra.core.date

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * All business dates of the hotel belong to the Asia/Tehran time zone.
 * The backend stores UTC instants; only this object decides what "today" means.
 */
object TehranTime {

    val zone: ZoneId = ZoneId.of("Asia/Tehran")

    fun today(): LocalDate = LocalDate.now(zone)

    fun now(): ZonedDateTime = ZonedDateTime.now(zone)

    fun nowInstant(): Instant = Instant.now()

    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    fun endOfDay(date: LocalDate): Instant = date.atTime(LocalTime.MAX).atZone(zone).toInstant()

    fun toTehran(instant: Instant): ZonedDateTime = instant.atZone(zone)

    fun toJalali(instant: Instant): JalaliDate = JalaliDate.from(instant.atZone(zone).toLocalDate())
}

object IsoFormat {

    const val PATTERN_DATE = "yyyy-MM-dd"
    const val PATTERN_TIME = "HH:mm"

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN_DATE)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN_TIME)

    fun date(value: LocalDate): String = value.format(dateFormatter)

    fun dateOrNull(value: LocalDate?): String? = value?.format(dateFormatter)

    fun timestamp(value: Instant): String = DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.SECONDS))

    fun time(value: Instant): String = value.atZone(TehranTime.zone).format(timeFormatter)

    /**
     * Tolerant parser for values coming from the API: accepts ISO instants
     * ("2025-03-05T10:15:30Z"), offsets ("2025-03-05T10:15:30+03:30"), local
     * date-times without a zone (assumed Tehran) and plain business dates.
     */
    fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        runCatching { return Instant.parse(value) }
        runCatching { return java.time.OffsetDateTime.parse(value).toInstant() }
        runCatching {
            return LocalDateTime.parse(value).atZone(TehranTime.zone).toInstant()
        }
        runCatching {
            return LocalDate.parse(value, dateFormatter).atStartOfDay(TehranTime.zone).toInstant()
        }
        // Some backends emit a trailing 'Z' on local dates.
        runCatching {
            return LocalDate.parse(value.removeSuffix("Z"), dateFormatter).atStartOfDay(TehranTime.zone).toInstant()
        }
        return null
    }

    /** Business date (yyyy-MM-dd) -> LocalDate, interpreted in Tehran. */
    fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        runCatching { return LocalDate.parse(value, dateFormatter) }
        return parseInstant(value)?.atZone(TehranTime.zone)?.toLocalDate()
    }
}

fun LocalDate.toJalali(): JalaliDate = JalaliDate.from(this)

fun LocalDate.formatJalaliShortFa(): String = toJalali().formatShortFa()

fun LocalDate.formatJalaliLongFa(): String = toJalali().formatLongFa()

fun LocalDate?.formatJalaliFaOrDash(): String = this?.toJalali()?.formatShortFa() ?: "\u2014"

fun LocalDate?.formatJalaliLongFaOrDash(): String = this?.toJalali()?.formatLongFa() ?: "\u2014"

/** "۱۴۰۳/۱۲/۰۵ — ۱۴:۳۰" in Tehran time. */
fun Instant?.formatJalaliDateTimeFa(): String {
    if (this == null) return "\u2014"
    val zoned = atZone(TehranTime.zone)
    val date = JalaliDate.from(zoned.toLocalDate()).formatShortFa()
    val time = zoned.format(DateTimeFormatter.ofPattern(PATTERN_TIME)).let { ir.ahvaz.mehra.core.common.toPersianDigits(it) }
    return "$date \u2014 $time"
}

fun Instant?.formatTimeFa(): String {
    if (this == null) return "\u2014"
    val time = atZone(TehranTime.zone).format(DateTimeFormatter.ofPattern(PATTERN_TIME))
    return ir.ahvaz.mehra.core.common.toPersianDigits(time)
}

fun Long?.formatEpochDateTimeFa(): String = this?.let { Instant.ofEpochMilli(it) }?.formatJalaliDateTimeFa() ?: "\u2014"

fun Long?.formatEpochTimeFa(): String = this?.let { Instant.ofEpochMilli(it) }?.formatTimeFa() ?: "\u2014"

/** Whole nights between two business dates; always >= 0. */
fun nightsBetween(checkIn: LocalDate, checkOut: LocalDate): Int {
    val nights = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
    return if (nights < 0) 0 else nights
}

/** "۳ شب" / "۴ روز" style duration used on reservation summaries. */
fun nightsLabelFa(nights: Int): String = "${ir.ahvaz.mehra.core.common.toPersianDigits(nights.toString())} شب"

/** "۳ دقیقه پیش" — used for activity feeds and cache freshness. */
fun relativeTimeFa(instant: Instant?, now: Instant = Instant.now()): String {
    if (instant == null) return "\u2014"
    val seconds = ChronoUnit.SECONDS.between(instant, now)
    return when {
        seconds < 60 -> "همین حالا"
        seconds < 3600 -> "${ir.ahvaz.mehra.core.common.toPersianDigits((seconds / 60).toString())} دقیقه پیش"
        seconds < 86_400 -> "${ir.ahvaz.mehra.core.common.toPersianDigits((seconds / 3600).toString())} ساعت پیش"
        seconds < 2_592_000 -> "${ir.ahvaz.mehra.core.common.toPersianDigits((seconds / 86_400).toString())} روز پیش"
        else -> JalaliDate.from(instant.atZone(TehranTime.zone).toLocalDate()).formatShortFa()
    }
}

fun epochMillisToInstantOrNull(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
