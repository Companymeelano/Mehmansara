package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle

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
