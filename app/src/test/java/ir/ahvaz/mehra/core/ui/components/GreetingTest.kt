package ir.ahvaz.mehra.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GreetingTest {

    @Test
    fun `time of day greeting buckets`() {
        assertThat(timeOfDayGreetingFa(0)).isEqualTo("شب بخیر")
        assertThat(timeOfDayGreetingFa(4)).isEqualTo("شب بخیر")
        assertThat(timeOfDayGreetingFa(5)).isEqualTo("صبح بخیر")
        assertThat(timeOfDayGreetingFa(11)).isEqualTo("صبح بخیر")
        assertThat(timeOfDayGreetingFa(12)).isEqualTo("ظهر بخیر")
        assertThat(timeOfDayGreetingFa(14)).isEqualTo("ظهر بخیر")
        assertThat(timeOfDayGreetingFa(15)).isEqualTo("عصر بخیر")
        assertThat(timeOfDayGreetingFa(18)).isEqualTo("عصر بخیر")
        assertThat(timeOfDayGreetingFa(19)).isEqualTo("شب بخیر")
        assertThat(timeOfDayGreetingFa(23)).isEqualTo("شب بخیر")
    }
}
