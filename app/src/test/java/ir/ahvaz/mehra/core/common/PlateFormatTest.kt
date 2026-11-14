package ir.ahvaz.mehra.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlateFormatTest {

    @Test
    fun `normalize converts persian digits and strips separators`() {
        assertThat(normalizePlate("۱۲ب۳۴۵-۶۷")).isEqualTo("12ب34567")
        assertThat(normalizePlate("12ب34567")).isEqualTo("12ب34567")
    }

    @Test
    fun `split and join round-trip`() {
        val plate = "12ب34567"
        val seg = splitPlate(plate)
        assertThat(seg.province).isEqualTo("12")
        assertThat(seg.serial3).isEqualTo("345")
        assertThat(seg.letter).isEqualTo("ب")
        assertThat(seg.duo).isEqualTo("67")
        assertThat(joinPlate(seg)).isEqualTo(plate)
        assertThat(isPlateComplete(plate)).isTrue()
        assertThat(isPlateComplete("12ب345")).isFalse()
    }

    @Test
    fun `floor label uses hamkaf for ground floor`() {
        assertThat(0.floorLabelFa()).isEqualTo("همکف")
        assertThat((-1).floorLabelFa()).isEqualTo("همکف")
        assertThat(2.floorLabelFa()).isEqualTo("طبقه ۲")
    }
}
