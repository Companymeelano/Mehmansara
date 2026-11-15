package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
            // v4.1.0: STATIC royal-navy aurora — three fixed radial glows (navy/sapphire/gold)
            // painted once; all looping animations removed so the app never redraws per frame.
            Box(modifier = Modifier.fillMaxSize().background(backdrop)) {
                // v4.0.0: faint Iranian geometric lattice (two diagonal families) on a
                // static layer — 4-5% opacity, no per-frame cost.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val step = 64.dp.toPx()
                            val line = mehraColors.topBarEnd.copy(alpha = 0.045f)
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(line, Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                                drawLine(line.copy(alpha = 0.03f), Offset(x, size.height), Offset(x + size.height, 0f), 1f)
                                x += step
                            }
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(mehraColors.topBarEnd.copy(alpha = 0.22f), Color.Transparent),
                            ),
                            radius = w * 0.95f,
                            center = Offset(w * 0.18f, h * 0.16f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF3D6BE0).copy(alpha = 0.15f), Color.Transparent),
                            ),
                            radius = w * 0.8f,
                            center = Offset(w * 0.85f, h * 0.62f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(mehraColors.accentMid.copy(alpha = 0.26f), Color.Transparent),
                            ),
                            radius = w * 0.85f,
                            center = Offset(w * 0.55f, h * 0.92f),
                            )
                        },
                ) {
                    content()
                }
            }
        }
    }
}
