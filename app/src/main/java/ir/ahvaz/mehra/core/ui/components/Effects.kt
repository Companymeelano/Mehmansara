package ir.ahvaz.mehra.core.ui.components
import androidx.compose.foundation.background
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * Text filled with a gradient (e.g. the brand accent gradient) instead of a flat colour —
 * used for the app name on auth screens and headings, always derived from the active theme.
 */
@Composable
fun GradientText(
    text: String,
    style: TextStyle,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        modifier = modifier.drawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        },
    )
}

/**
 * Micro-interaction: the element gently scales down (0.955) while pressed — gives every
 * card / button a tactile, "physical 3D" feel.
 *
 * IMPORTANT (bugfix 1.9.1): this must NOT use its own `pointerInput`/`detectTapGestures`.
 * A second gesture detector chained after `clickable` receives the pointer events first
 * (innermost modifier wins) and consumes the tap, so the `clickable` onClick never fired —
 * every MehraCard option was dead. The pressed state is now driven by the SAME
 * InteractionSource that the clickable uses (see MehraCard).
 */
@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier = graphicsLayer {
    val target = if (pressed) 0.955f else 1f
    scaleX = target
    scaleY = target
}

/**
 * Shimmer placeholder block (v1.6.0): a soft pulsing bar used in loading/empty states
 * instead of a dead grey rectangle.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha), RoundedCornerShape(6.dp)),
    )
}
