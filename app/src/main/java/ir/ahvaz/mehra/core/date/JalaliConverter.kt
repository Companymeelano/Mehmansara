package ir.ahvaz.mehra.core.date

import java.time.LocalDate

/**
 * Solar Hijri <-> Gregorian conversion.
 *
 * Ported from `jalaali-js` (MIT) which implements the Borkowski/Jalali arithmetic with the
 * 33-year break table. All integer divisions intentionally truncate toward zero, exactly
 * like the reference implementation.
 */
object JalaliConverter {

    /** Break years of the 33-year cycle (Borkowski). */
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    /** Julian Day Number of the Gregorian epoch offset (1970-01-01 == JDN 2440588). */
    private const val UNIX_EPOCH_JDN = 2440588

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int, withoutLeap: Boolean = false): JalCal {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        require(jy >= BREAKS[0] && jy < BREAKS[bl - 1]) { "سال شمسی خارج از محدوده پشتیبانی: $jy" }

        var jump = 0
        for (i in 1 until bl) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ = leapJ + (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ = leapJ + (n / 33) * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1

        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG

        var leap = 0
        if (!withoutLeap) {
            if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
            leap = ((n + 1) % 33 - 1) % 4
            if (leap == -1) leap = 4
        }
        return JalCal(leap = leap, gy = gy, march = march)
    }

    fun isLeapYear(jy: Int): Boolean = jalCal(jy, withoutLeap = false).leap == 0

    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Int {
        var d = (gy + (gm - 8) / 6 + 100100) * 1461 / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 +
            gd - 34840408
        d = d - ((gy + 100100 + (gm - 8) / 6) / 100) * 3 / 4 + 752
        return d
    }

    fun jdnToGregorian(jdn: Int): LocalDate {
        var j = 4 * jdn + 139361631
        j = j + ((4 * jdn + 183187720) / 146097) * 3 / 4 * 4 - 3908
        val i = (j % 1461 / 4) * 5 + 308
        val gd = i % 153 / 5 + 1
        val gm = i / 153 % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return LocalDate.of(gy, gm, gd)
    }

    fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy, withoutLeap = true)
        return gregorianToJdn(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    fun jdnToJalali(jdn: Int): JalaliDate {
        val gy = jdnToGregorian(jdn).year
        var jy = gy - 621
        val r = jalCal(jy, withoutLeap = false)
        val jdn1f = gregorianToJdn(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun localDateToJalali(date: LocalDate): JalaliDate =
        jdnToJalali(date.toEpochDay().toInt() + UNIX_EPOCH_JDN)

    fun jalaliToLocalDate(jalali: JalaliDate): LocalDate =
        jdnToGregorian(jalaliToJdn(jalali.year, jalali.month, jalali.day))
}
