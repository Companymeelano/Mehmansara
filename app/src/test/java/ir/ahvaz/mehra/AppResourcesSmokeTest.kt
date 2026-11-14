package ir.ahvaz.mehra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric smoke test: the app manifest + resources load correctly and the brand
 * string is Persian — catches broken manifests/resource merges in CI without a device.
 */
@RunWith(RobolectricTestRunner::class)
class AppResourcesSmokeTest {

    @Test
    fun `app name resource is persian brand`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(context.getString(R.string.app_name)).isEqualTo("مهرآ")
        assertThat(context.packageName).isEqualTo("ir.ahvaz.mehra")
    }
}
