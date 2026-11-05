package ir.ahvaz.mehra.core.common

private const val FA_ZERO = '\u06F0'
private const val AR_ZERO = '\u0660'

/** Converts Latin digits to Persian (۰-۹) digits. */
fun String.toPersianDigits(): String {
    if (isEmpty()) return this
    val sb = StringBuilder(length)
    for (ch in this) {
        sb.append(
            when (ch) {
                in '0'..'9' -> (FA_ZERO + (ch - '0'))
                else -> ch
            }
        )
    }
    return sb.toString()
}

/** Numeric overloads so call sites do not need manual `toString()` conversions. */
fun Int.toPersianDigits(): String = toString().toPersianDigits()

fun Long.toPersianDigits(): String = toString().toPersianDigits()

fun Int.toLatinDigits(): String = toString().toLatinDigits()

fun Long.toLatinDigits(): String = toString().toLatinDigits()

/** Converts Persian (۰-۹) and Arabic-Indic (٠-٩) digits to Latin digits. */
fun String.toLatinDigits(): String {
    if (isEmpty()) return this
    val sb = StringBuilder(length)
    for (ch in this) {
        sb.append(
            when (ch) {
                in '\u06F0'..'\u06F9' -> ('0' + (ch - FA_ZERO))
                in AR_ZERO..(AR_ZERO + 9) -> ('0' + (ch - AR_ZERO))
                else -> ch
            }
        )
    }
    return sb.toString()
}

/** Arabic Yeh/Kaf are normalised so search and stored national ids compare correctly. */
fun String.normalizePersian(): String = this
    .replace('\u064A', '\u06CC') // ي -> ی
    .replace('\u0643', '\u06A9') // ك -> ک
    .replace('\u200F', ' ')
    .replace('\u200E', ' ')
    .replace('\u00A0', ' ')
    .trim()

fun String.digitsOnly(): String = ().toLatinDigits().filter { it.isDigit() }

/** 9123456789 / 09123456789 / +989123456789 -> 09123456789 */
fun normalizeIranMobile(raw: String): String {
    var v = raw.digitsOnly()
    if (v.startsWith("0098")) v = v.removePrefix("0098")
    if (v.startsWith("98") && v.length == 12) v = "0" + v.removePrefix("98")
    if (v.length == 10 && v.startsWith("9")) v = "0$v"
    return v
}

/** Formats 1250000 into "۱٫۲۵۰٫۰۰۰". */
fun formatThousands(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val sb = StringBuilder()
    for ((index, ch) in digits.withIndex()) {
        if (index > 0 && (digits.length - index) % 3 == 0) sb.append('\u066C')
        sb.append(ch)
    }
    val body = sb.toString().toPersianDigits()
    return if (negative) "\u2212$body" else body
}

/** Parses user input that may contain Persian digits, separators or spaces. */
fun parseLongInput(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.toLatinDigits()
        .replace(",", "")
        .replace("\u066C", "")
        .replace(" ", "")
        .replace("\u00A0", "")
    if (cleaned.isEmpty() || cleaned == "-") return null
    return cleaned.toLongOrNull()
}

fun parseBigDecimalInput(raw: String?): java.math.BigDecimal? {
    val cleaned = raw?.toLatinDigits()?.replace(",", "")?.replace("\u066C", "")?.trim() ?: return null
    if (cleaned.isEmpty()) return null
    return cleaned.toBigDecimalOrNull()
}
