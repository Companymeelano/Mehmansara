package ir.ahvaz.mehra.core.common

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DailyTipsTest {

    @Test
    fun `tip is deterministic per day and always non-blank`() {
        val d = LocalDate.of(2026, 11, 14)
        assertThat(DailyTips.tipFor(d)).isEqualTo(DailyTips.tipFor(d))
        assertThat(DailyTips.tipFor(d)).isNotEmpty()
        repeat(365) { i ->
            assertThat(DailyTips.tipFor(LocalDate.of(2026, 1, 1).plusDays(i.toLong())))
                .isIn(DailyTips.tips)
        }
    }

    @Test
    fun `tips list is usable`() {
        assertThat(DailyTips.tips.size).isGreaterThan(3)
        assertThat(DailyTips.tips.all { it.isNotBlank() }).isTrue()
    }
}
