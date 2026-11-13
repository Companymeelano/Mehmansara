package ir.ahvaz.mehra.feature.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaletteNormalizeTest {

    @Test
    fun `unifies arabic and persian yeh and kaf`() {
        assertThat(normalizeSearchText("علي")).isEqualTo("علی")
        assertThat(normalizeSearchText("كتاب")).isEqualTo("کتاب")
    }

    @Test
    fun `converts persian digits to latin`() {
        assertThat(normalizeSearchText("اتاق ۲011")).isEqualTo("اتاق 2011")
        assertThat(normalizeSearchText("VIP ۱۲۳")).isEqualTo("vip 123")
    }

    @Test
    fun `strips zwnj and lowercases`() {
        assertThat(normalizeSearchText("می\u200cشود")).isEqualTo("می شود")
        assertThat(normalizeSearchText("  VIP  ")).isEqualTo("vip")
    }
}
