package ir.ahvaz.mehra.core.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
        modifier = modifier
            .background(brush)
            .drawWithContent {
                drawContent()
                drawRect(brush, blendMode = BlendMode.SrcAtop)
            },
    )
}

/**
 * Micro-interaction: the element gently scales down (0.955) while pressed — gives every
 * card / button a tactile, "physical 3D" feel. Usage: `Modifier.clickable { }.pressScale()`.
 */
@Composable
fun Modifier.pressScale(): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer {
            val target = if (pressed) 0.955f else 1f
            scaleX = target
            scaleY = target
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    scope.launch {
                        delay(140)
                        pressed = false
                    }
                    tryAwaitRelease()
                    pressed = false
                },
            )
        }
}
