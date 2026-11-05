package ir.ahvaz.mehra.core.ui.components

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

private const val SIGNATURE_LATIN = "MILAD YAGHOOBI"

/**
 * The creator's signature rendered as a 3D metallic extrusion: the Latin name is drawn many
 * times with a growing offset (the "depth"), then the face is filled with a vertical metal
 * gradient taken from the active theme (`MehraTheme.colors.metal*`), a soft theme-coloured
 * glow behind it and a specular highlight across the top half of the letters. Underneath, the
 * Persian name and role are shown in Vazirmatn.
 *
 * Everything is drawn on a [Canvas] with plain android.graphics paints via `drawIntoCanvas`,
 * so it works on every API level (minSdk 24) without any SVG runtime dependency.
 */
@Composable
fun CreatorSignature(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MehraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 38.dp else 56.dp),
        ) {
            val text = SIGNATURE_LATIN
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.045f
                textSize = size.height * 0.46f
            }
            // shrink until the name fits the available width
            val maxWidth = size.width * 0.96f
            val measured = paint.measureText(text)
            if (measured > maxWidth && measured > 0f) {
                paint.textSize *= maxWidth / measured
            }
            val textWidth = paint.measureText(text)
            val x = center.x - textWidth / 2f
            val metrics = paint.fontMetrics
            val baseline = center.y - (metrics.ascent + metrics.descent) / 2f

            val light = colors.metalLight.toArgb()
            val face = colors.metalFace.toArgb()
            val dark = colors.metalDark.toArgb()

            run {
                val canvas = drawContext.canvas.nativeCanvas
                // --- extrusion: stacked copies going down-right, getting darker ---
                val depth = (size.height * 0.17f).coerceAtLeast(2.5f)
                val steps = depth.toInt().coerceIn(3, 24)
                for (i in steps downTo 1) {
                    val t = i.toFloat() / steps
                    paint.shader = null
                    paint.setShadowLayer(0f, 0f, 0f, 0)
                    paint.color = ColorUtils.blendARGB(dark, face, t * 0.65f)
                    canvas.drawText(text, x + i * 0.55f, baseline + i * 0.55f, paint)
                }

                // --- metallic face with glow ---
                paint.shader = LinearGradient(
                    x, baseline + metrics.ascent,
                    x, baseline + metrics.descent,
                    intArrayOf(light, face, dark),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
                paint.setShadowLayer(
                    size.height * 0.22f, 0f, 0f,
                    colors.glow.copy(alpha = 0.65f).toArgb(),
                )
                canvas.drawText(text, x, baseline, paint)

                // --- specular highlight across the top half of the letters ---
                paint.setShadowLayer(0f, 0f, 0f, 0)
                canvas.save()
                canvas.clipRect(0f, baseline + metrics.ascent, size.width, center.y + 1f)
                paint.shader = LinearGradient(
                    0f, baseline + metrics.ascent,
                    0f, center.y,
                    intArrayOf(
                        Color.White.copy(alpha = 0.54f).toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawText(text, x, baseline, paint)
                canvas.restore()
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "میلاد یعقوبی",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.accentMid,
        )
        Text(
            text = "طراح و توسعه‌دهندهٔ سامانهٔ مهرآ",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
    }
}