package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalMehraColors = staticCompositionLocalOf { ThemePreset.GOLDEN_EXECUTIVE.mehraColors() }

/** Access to the extra semantic palette: `MehraTheme.colors.gold`. */
object MehraTheme {
    val colors: MehraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMehraColors.current
}

/**
 * The whole app is Persian and right-to-left; the layout direction is forced here so no
 * screen can accidentally render mirrored.
 */
@Composable
fun MehraTheme(
    preset: ThemePreset,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(preset) { preset.colorScheme() }
    val mehraColors = remember(preset) { preset.mehraColors() }
    val typography = remember { mehraTypography() }

    CompositionLocalProvider(
        LocalMehraColors provides mehraColors,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MehraShapes,
            content = content,
        )
    }
}
