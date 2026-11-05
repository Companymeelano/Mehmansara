package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

/** Brand accent gradient of the active preset — used by chips, buttons and gradient text. */
val LocalAccentBrush = staticCompositionLocalOf<Brush> {
    Brush.linearGradient(
        listOf(
            ThemePreset.GOLDEN_EXECUTIVE.mehraColors().accentStart,
            ThemePreset.GOLDEN_EXECUTIVE.mehraColors().accentMid,
            ThemePreset.GOLDEN_EXECUTIVE.mehraColors().accentEnd,
        )
    )
}

/**
 * The whole app is Persian and right-to-left; the layout direction is forced here so no
 * screen can accidentally render mirrored.
 *
 * A vertical gradient in the preset's own colours is painted behind the entire app, so the
 * selected theme is visible in every section — screens, sheets and dialogs sit on top of it
 * with translucent glass surfaces.
 */
@Composable
fun MehraTheme(
    preset: ThemePreset,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(preset) { preset.colorScheme() }
    val mehraColors = remember(preset) { preset.mehraColors() }
    val typography = remember { mehraTypography() }
    val backdrop = remember(mehraColors) {
        Brush.verticalGradient(listOf(mehraColors.bgTop, mehraColors.bgBottom))
    }
    val accent = remember(mehraColors) {
        Brush.linearGradient(listOf(mehraColors.accentStart, mehraColors.accentMid, mehraColors.accentEnd))
    }

    CompositionLocalProvider(
        LocalMehraColors provides mehraColors,
        LocalAccentBrush provides accent,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MehraShapes,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backdrop),
            ) {
                content()
            }
        }
    }
}
