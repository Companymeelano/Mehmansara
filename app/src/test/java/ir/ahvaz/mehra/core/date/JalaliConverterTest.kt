package ir.ahvaz.mehra.core.date

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/**
 * Anchors come from widely published Iranian calendar dates (Nowruz and 22 Bahman).
 * The round-trip test guards against asymmetric conversions across a 150-year span.
 */
class JalaliConverterTest {

    @Test
    fun `converts known anchor dates`() {
        assertThat(JalaliConverter.localDateToJalali(LocalDate.of(2024, 3, 20)))
            .isEqualTo(JalaliDate(1403, 1, 1))
        assertThat(JalaliConverter.localDateToJalali(LocalDate.of(2021, 3, 21)))
            .isEqualTo(JalaliDate(1400, 1, 1))
        assertThat(JalaliConverter.localDateToJalali(LocalDate.of(1979, 2, 11)))
            .isEqualTo(JalaliDate(1357, 11, 22))
        assertThat(JalaliConverter.jalaliToLocalDate(JalaliDate(1403, 1, 1)))
            .isEqualTo(LocalDate.of(2024, 3, 20))
    }

    @Test
    fun `last day of leap year maps to expected gregorian day`() {
        assertThat(JalaliConverter.jalaliToLocalDate(JalaliDate(1399, 12, 30)))
            .isEqualTo(LocalDate.of(2021, 3, 20))
        assertThat(JalaliDate(1399, 1, 1).isLeapYear).isTrue()
        assertThat(JalaliDate(1400, 1, 1).isLeapYear).isFalse()
    }

    @Test
    fun `round trips every day of a 150 year window`() {
        var date = LocalDate.of(1960, 1, 1)
        val end = LocalDate.of(2110, 1, 1)
        while (date.isBefore(end)) {
            val jalali = JalaliConverter.localDateToJalali(date)
            val back = JalaliConverter.jalaliToLocalDate(jalali)
            assertThat(back).isEqualTo(date)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `nights between never returns negative`() {
        val checkIn = LocalDate.of(2024, 3, 20)
        assertThat(nightsBetween(checkIn, checkIn.plusDays(3))).isEqualTo(3)
        assertThat(nightsBetween(checkIn, checkIn)).isEqualTo(0)
        assertThat(nightsBetween(checkIn.plusDays(2), checkIn)).isEqualTo(0)
    }

    @Test
    fun `parses persian digits and separators`() {
        assertThat(JalaliDate.parse("۱۴۰۳/۰۱/۰۱")).isEqualTo(JalaliDate(1403, 1, 1))
        assertThat(JalaliDate.parse("1403-01-01")).isEqualTo(JalaliDate(1403, 1, 1))
        assertThat(JalaliDate.parse("باطل")).isNull()
    }
}
