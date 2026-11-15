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
            // v3.0.0: living aurora backdrop — three huge, very slow radial glows drift
            // over the base gradient, so the whole app feels alive (pure canvas drawing).
            val aurora = rememberInfiniteTransition(label = "aurora")
            val g1 by aurora.animateFloat(0f, 1f, infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse), label = "g1")
            val g2 by aurora.animateFloat(1f, 0f, infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse), label = "g2")
            val g3 by aurora.animateFloat(0f, 1f, infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse), label = "g3")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backdrop)
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(mehraColors.accentStart.copy(alpha = 0.16f), Color.Transparent),
                            ),
                            radius = w * 0.95f,
                            center = Offset(w * (0.15f + 0.7f * g1), h * (0.10f + 0.25f * g2)),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF3B82F6).copy(alpha = 0.10f), Color.Transparent),
                            ),
                            radius = w * 0.8f,
                            center = Offset(w * (0.9f - 0.6f * g2), h * (0.55f + 0.3f * g3)),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(mehraColors.accentEnd.copy(alpha = 0.14f), Color.Transparent),
                            ),
                            radius = w * 0.85f,
                            center = Offset(w * (0.5f + 0.4f * g3), h * (0.95f - 0.35f * g1)),
                        )
                    },
            ) {
                content()
            }
        }
    }
}
