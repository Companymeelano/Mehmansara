package ir.ahvaz.mehra.core.common

/**
 * Currency handling. The backend always stores amounts as integer Rial; the UI may present
 * Rial or Toman depending on the operator's setting — conversion happens only here.
 */
enum class CurrencyUnit(val code: String, val labelFa: String, val rialDivisor: Long) {
    RIAL("IRR", "ریال", 1L),
    TOMAN("IRT", "تومان", 10L);

    companion object {
        fun fromCode(code: String?): CurrencyUnit =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: RIAL
    }
}

/** Converts a stored Rial amount into the display amount of [unit]. */
fun Long.rialToDisplay(unit: CurrencyUnit): Long = this / unit.rialDivisor

/** Converts a user typed amount in [unit] back to Rial for the API. */
fun Long.displayToRial(unit: CurrencyUnit): Long = this * unit.rialDivisor

/** "۱۲٫۵۰۰٫۰۰۰ تومان" — Persian digits with thousands separators. */
fun formatMoneyFa(amountRial: Long, unit: CurrencyUnit = CurrencyUnit.RIAL): String =
    "${formatThousands(amountRial.rialToDisplay(unit))} ${unit.labelFa}"

/** Compact form for dashboard tiles: "۱۲٫۵ م" / "۱٫۲ میلیارد" (Toman based). */
fun formatMoneyCompactFa(amountRial: Long, unit: CurrencyUnit = CurrencyUnit.RIAL): String {
    val display = amountRial.rialToDisplay(unit)
    val abs = kotlin.math.abs(display)
    val sign = if (display < 0) "\u2212" else ""
    val body = when {
        abs >= 1_000_000_000L -> {
            val v = abs / 100_000_000L
            "${v / 10}.${v % 10} میلیارد"
        }
        abs >= 10_000_000L -> {
            val v = abs / 100_000L
            "${v / 10}.${v % 10} میلیون"
        }
        abs >= 1_000_000L -> {
            val v = abs / 100_000L
            "${v / 10}.${v % 10} م"
        }
        else -> formatThousands(abs)
    }
    return "$sign$body ${unit.labelFa}".toPersianDigits()
}
