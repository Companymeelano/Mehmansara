package ir.ahvaz.mehra.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PersianTextTest {

    @Test
    fun `converts digits both ways`() {
        assertThat("09123456789".toPersianDigits()).isEqualTo("۰۹۱۲۳۴۵۶۷۸۹")
        assertThat("۰۹۱۲۳۴۵۶۷۸۹".toLatinDigits()).isEqualTo("09123456789")
        assertThat("٠٩١٢".toLatinDigits()).isEqualTo("0912")
        assertThat(42.toPersianDigits()).isEqualTo("۴۲")
    }

    @Test
    fun `formats money with persian thousands separator`() {
        assertThat(formatMoneyFa(1_250_000L)).isEqualTo("۱٬۲۵۰٬۰۰۰ ریال")
        assertThat(formatMoneyFa(1_250_000L, CurrencyUnit.TOMAN)).isEqualTo("۱۲۵٬۰۰۰ تومان")
    }

    @Test
    fun `parses user amount input`() {
        assertThat(parseLongInput("۱٬۲۵۰٬۰۰۰")).isEqualTo(1_250_000L)
        assertThat(parseLongInput("1250000")).isEqualTo(1_250_000L)
        assertThat(parseLongInput("")).isNull()
        assertThat(parseLongInput("abc")).isNull()
    }

    @Test
    fun `validates iranian national id check digit`() {
        assertThat(Validators.isValidIranNationalId("0013542419")).isTrue()
        assertThat(Validators.isValidIranNationalId("0013542418")).isFalse()
        assertThat(Validators.isValidIranMobile("09123456789")).isTrue()
        assertThat(Validators.isValidIranMobile("۰۹۱۲۳۴۵۶۷۸۹")).isTrue()
        assertThat(Validators.isValidIranMobile("12345")).isFalse()
    }
}
