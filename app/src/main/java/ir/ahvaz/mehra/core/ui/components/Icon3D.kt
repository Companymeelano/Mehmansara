package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.darken
import ir.ahvaz.mehra.core.ui.theme.lighten

/**
 * A "3D" app-icon-style chip: the glyph sits on a rounded tile filled with the active theme's
 * accent gradient, a glossy highlight on top and a soft shadow underneath, so every icon in
 * the app automatically matches the user-selected theme.
 *
 * @param tone when set, the chip is built from this semantic colour instead of the brand
 *             accent (used for status-tinted tiles); the glyph stays white for contrast.
 */
@Composable
fun Icon3D(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tone: Color? = null,
    glyphScale: Float = 0.56f,
) {
    val colors = MehraTheme.colors
    val stops = if (tone != null) {
        listOf(tone.lighten(0.30f), tone, tone.darken(0.32f))
    } else {
        listOf(colors.accentStart, colors.accentMid, colors.accentEnd)
    }
    val glyphTint = if (tone != null) Color.White else colors.onAccent
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, shape, ambientColor = Color(0xFF081020).copy(alpha = 0.16f), spotColor = Color(0xFF081020).copy(alpha = 0.22f))
            .background(Brush.linearGradient(stops), shape),
    ) {
        // gloss on the upper half + slight shading at the bottom edge -> 3D relief
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.50f),
                        0.45f to Color.White.copy(alpha = 0.05f),
                        1.0f to Color(0xFF081020).copy(alpha = 0.24f),
                    ),
                    shape,
                ),
        )
        // v3.0.0 extrusion: a dark copy of the glyph shifted down-right behind the main
        // one — cheap but very convincing 3D depth on every chip.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF081020).copy(alpha = 0.42f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * glyphScale)
                .offset(x = size * 0.06f, y = size * 0.08f),
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = glyphTint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * glyphScale),
        )
    }
}
