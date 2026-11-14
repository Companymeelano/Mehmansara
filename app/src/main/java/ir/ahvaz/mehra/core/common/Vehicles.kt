package ir.ahvaz.mehra.core.common

/**
 * Vehicle pickers + Iranian-plate helpers (v2.2.0).
 *
 * A normalised plate is exactly 8 characters, laid out like a real Iranian plate
 * (left to right): province code (2 digits) + serial (3 digits) + letter (1) + duo (2 digits).
 */

/** Letters allowed on Iranian plates. */
val PLATE_LETTERS: List<String> = listOf(
    "ب", "ج", "د", "س", "ص", "ط", "ع", "ق", "ل", "م", "ن", "و", "ه", "ی",
)

const val PLATE_LENGTH = 8

/** Popular vehicle models for the guest-form picker (searchable). */
val VEHICLE_TYPES: List<String> = listOf(
    "پراید", "پژو ۲۰۶", "پژو ۲۰۷", "پژو ۲۰۸", "پژو پارس", "سمند", "دنا", "تیبا", "ساینا",
    "کوییک", "رانا", "هایما", "جک", "تیگو", "خاور", "وانت پراید", "وانت", "سایر",
)

/** Common vehicle colours. */
val VEHICLE_COLORS: List<String> = listOf(
    "سفید", "مشکی", "خاکستری", "نقره‌ای", "آبی", "قرمز", "سبز", "زرد", "قهوه‌ای", "سایر",
)

/** Strips everything except digits and plate letters, and converts Persian digits to Latin. */
fun normalizePlate(raw: String): String {
    val latin = raw.toLatinDigits()
    return buildString {
        for (ch in latin) {
            if (ch.isDigit() || PLATE_LETTERS.contains(ch.toString())) append(ch)
        }
    }
}

/** The four visual sections of a plate: province (2), serial (3), letter (1), duo (2). */
data class PlateSegments(
    val province: String,
    val serial3: String,
    val letter: String,
    val duo: String,
)

/** Splits a (possibly partial) normalised plate into its display segments. */
fun splitPlate(plate: String): PlateSegments {
    val s = normalizePlate(plate).padEnd(PLATE_LENGTH, ' ')
    return PlateSegments(
        province = s.substring(0, 2).trim(),
        serial3 = s.substring(2, 5).trim(),
        letter = s.substring(5, 6).trim(),
        duo = s.substring(6, 8).trim(),
    )
}

/** Joins segments back into the canonical 8-char plate string. */
fun joinPlate(segments: PlateSegments): String =
    (segments.province + segments.serial3 + segments.letter + segments.duo)
        .let { normalizePlate(it) }

/** True when the plate has all 8 positions filled. */
fun isPlateComplete(plate: String): Boolean = normalizePlate(plate).length == PLATE_LENGTH

/** "همکف" for floor 0 and below, otherwise "طبقه N" with Persian digits. */
fun Int.floorLabelFa(): String =
    if (this <= 0) "همکف" else "طبقه " + toString().toPersianDigits()
