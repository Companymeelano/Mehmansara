package ir.ahvaz.mehra.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoyaltyTest {

    @Test
    fun `tier thresholds`() {
        assertThat(LoyaltyTier.tierOf(0)).isEqualTo(LoyaltyTier.BRONZE)
        assertThat(LoyaltyTier.tierOf(2)).isEqualTo(LoyaltyTier.BRONZE)
        assertThat(LoyaltyTier.tierOf(3)).isEqualTo(LoyaltyTier.SILVER)
        assertThat(LoyaltyTier.tierOf(5)).isEqualTo(LoyaltyTier.GOLD)
        assertThat(LoyaltyTier.tierOf(9)).isEqualTo(LoyaltyTier.GOLD)
        assertThat(LoyaltyTier.tierOf(10)).isEqualTo(LoyaltyTier.PLATINUM)
        assertThat(LoyaltyTier.tierOf(99)).isEqualTo(LoyaltyTier.PLATINUM)
    }

    @Test
    fun `next tier and progress math`() {
        assertThat(LoyaltyTier.nextTier(0)).isEqualTo(LoyaltyTier.SILVER)
        assertThat(LoyaltyTier.nextTier(4)).isEqualTo(LoyaltyTier.GOLD)
        assertThat(LoyaltyTier.nextTier(10)).isNull()
        // a guest with 4 stays is 4/5 of the way to GOLD
        val stays = 4
        val next = LoyaltyTier.nextTier(stays)!!
        assertThat(next).isEqualTo(LoyaltyTier.GOLD)
        assertThat(stays.toFloat() / next.minStays).isLessThan(1f)
    }
}
