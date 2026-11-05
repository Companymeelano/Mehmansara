package ir.ahvaz.mehra.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeLineCap
import androidx.compose.ui.graphics.StrokeLineJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.unit.dp

/**
 * MEHRA's own icon set.
 *
 * Every glyph is authored as an SVG master in the `design/icons/svg` folder (with metallic
 * gradients for the 3D look) and mirrored here as an Android vector drawable. At runtime the
 * glyph is drawn by [ir.ahvaz.mehra.core.ui.components.Icon3D] on top of a theme-driven
 * gradient chip, so the icon always matches the user-selected theme. All paths are simple
 * strokes on a 24dp viewport with round caps — crisp at any density and safely tintable.
 */
object MehraIcons {

    private val Glyph: SolidColor = SolidColor(Color.White)

    private fun builder(name: String): Builder =
        ImageVector.Builder(name, 24.dp, 24.dp, 24.dp, 24.dp)

    private fun Builder.stroked(
        width: Float = 1.7f,
        pathBuilder: PathBuilder.() -> Unit,
    ): Builder = path(
        stroke = Glyph,
        strokeAlpha = 1f,
        strokeLineWidth = width,
        strokeLineCap = StrokeLineCap.Round,
        strokeLineJoin = StrokeLineJoin.Round,
        pathBuilder = pathBuilder,
    )

    private fun Builder.filled(
        width: Float = 1.4f,
        pathBuilder: PathBuilder.() -> Unit,
    ): Builder = path(
        fill = Glyph,
        stroke = Glyph,
        strokeLineWidth = width,
        strokeLineJoin = StrokeLineJoin.Round,
        pathBuilder = pathBuilder,
    )

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        val k = 0.5523f * r
        moveTo(cx + r, cy)
        curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        close()
    }

    /** Brand mark: a Persian mihrab arch with a dot — also used as the 3D logo chip. */
    val Logo: ImageVector = builder("mehra_logo").apply {
        stroked(1.9f) {
            moveTo(7f, 20f)
            lineTo(7f, 11.5f)
            curveTo(7f, 7f, 9f, 4.8f, 12f, 3.5f)
            curveTo(15f, 4.8f, 17f, 7f, 17f, 11.5f)
            lineTo(17f, 20f)
        }
        stroked(1.3f) {
            moveTo(9.8f, 20f)
            lineTo(9.8f, 13f)
            curveTo(9.8f, 10.6f, 10.8f, 9.4f, 12f, 8.6f)
            curveTo(13.2f, 9.4f, 14.2f, 10.6f, 14.2f, 13f)
            lineTo(14.2f, 20f)
        }
        stroked(1.9f) {
            moveTo(4.5f, 20f)
            lineTo(19.5f, 20f)
        }
        filled(width = 0.8f) { circle(12f, 2.1f, 0.95f) }
    }.build()

    /** Dashboard: three rounded bars of different heights. */
    val Dashboard: ImageVector = builder("mehra_dashboard").apply {
        filled { moveTo(5f, 12f); lineTo(8f, 12f); lineTo(8f, 19f); lineTo(5f, 19f); close() }
        filled { moveTo(10.5f, 6.5f); lineTo(13.5f, 6.5f); lineTo(13.5f, 19f); lineTo(10.5f, 19f); close() }
        filled { moveTo(16f, 9.5f); lineTo(19f, 9.5f); lineTo(19f, 19f); lineTo(16f, 19f); close() }
    }.build()

    /** Rooms: bed with headboard, mattress and pillow. */
    val Rooms: ImageVector = builder("mehra_rooms").apply {
        stroked {
            moveTo(3.5f, 19f)
            lineTo(3.5f, 8f)
        }
        stroked {
            moveTo(3.5f, 15f)
            lineTo(20.5f, 15f)
            lineTo(20.5f, 19f)
        }
        stroked {
            moveTo(6.5f, 13f)
            lineTo(6.5f, 11.4f)
            curveTo(6.5f, 10.6f, 7f, 10.1f, 7.8f, 10.1f)
            lineTo(10f, 10.1f)
            curveTo(10.8f, 10.1f, 11.3f, 10.6f, 11.3f, 11.4f)
            lineTo(11.3f, 13f)
        }
    }.build()

    /** Reservations: calendar with a check mark. */
    val Reservations: ImageVector = builder("mehra_reservations").apply {
        stroked {
            moveTo(4.5f, 6f)
            lineTo(19.5f, 6f)
            lineTo(19.5f, 19.5f)
            lineTo(4.5f, 19.5f)
            close()
        }
        stroked(1.4f) {
            moveTo(4.5f, 10f)
            lineTo(19.5f, 10f)
        }
        stroked {
            moveTo(8.5f, 3.5f)
            lineTo(8.5f, 7f)
        }
        stroked {
            moveTo(15.5f, 3.5f)
            lineTo(15.5f, 7f)
        }
        stroked(1.9f) {
            moveTo(9f, 15.2f)
            lineTo(11.1f, 17.3f)
            lineTo(15.2f, 13f)
        }
    }.build()

    /** Reception: concierge bell. */
    val Reception: ImageVector = builder("mehra_reception").apply {
        stroked {
            moveTo(5f, 16.5f)
            curveTo(5f, 12f, 8f, 9.8f, 12f, 9.8f)
            curveTo(16f, 9.8f, 19f, 12f, 19f, 16.5f)
        }
        stroked {
            moveTo(3.5f, 16.5f)
            lineTo(20.5f, 16.5f)
        }
        stroked {
            moveTo(5.5f, 19.5f)
            lineTo(18.5f, 19.5f)
        }
        stroked(1.5f) {
            moveTo(12f, 9.8f)
            lineTo(12f, 7.4f)
        }
        filled(width = 0.8f) { circle(12f, 6.1f, 1.05f) }
    }.build()

    /** More: 3x3 grid of dots. */
    val More: ImageVector = builder("mehra_more").apply {
        for (gx in floatArrayOf(6f, 12f, 18f)) {
            for (gy in floatArrayOf(6f, 12f, 18f)) {
                filled(width = 0.8f) { circle(gx, gy, 1.35f) }
            }
        }
    }.build()

    /** Guests: two travellers. */
    val Guests: ImageVector = builder("mehra_guests").apply {
        stroked { circle(9f, 8.2f, 2.6f) }
        stroked {
            moveTo(4f, 19.5f)
            curveTo(4f, 16.2f, 6.3f, 14f, 9f, 14f)
            curveTo(11.7f, 14f, 14f, 16.2f, 14f, 19.5f)
        }
        stroked(1.5f) { circle(16.8f, 9f, 2.1f) }
        stroked(1.5f) {
            moveTo(15.6f, 14.6f)
            curveTo(18.6f, 15f, 20.5f, 17f, 20.5f, 19.5f)
        }
    }.build()

    /** Finance: banknote with a coin. */
    val Finance: ImageVector = builder("mehra_finance").apply {
        stroked {
            moveTo(3f, 7.5f)
            lineTo(21f, 7.5f)
            lineTo(21f, 17f)
            lineTo(3f, 17f)
            close()
        }
        stroked(1.5f) { circle(12f, 12.2f, 2.6f) }
        stroked(1.5f) {
            moveTo(6.2f, 12.2f)
            lineTo(7.6f, 12.2f)
        }
        stroked(1.5f) {
            moveTo(16.4f, 12.2f)
            lineTo(17.8f, 12.2f)
        }
    }.build()

    /** Housekeeping: spray bottle with mist. */
    val Housekeeping: ImageVector = builder("mehra_housekeeping").apply {
        stroked {
            moveTo(9f, 10.5f)
            lineTo(15f, 10.5f)
            lineTo(15f, 20f)
            lineTo(9f, 20f)
            close()
        }
        stroked {
            moveTo(10.5f, 10.5f)
            lineTo(10.5f, 8f)
            lineTo(13.5f, 8f)
            lineTo(13.5f, 10.5f)
        }
        stroked(1.5f) {
            moveTo(13.5f, 8f)
            lineTo(16.7f, 8f)
        }
        stroked(1.4f) {
            moveTo(18.6f, 5.4f)
            lineTo(20.4f, 4.6f)
        }
        stroked(1.4f) {
            moveTo(19.2f, 8f)
            lineTo(21.2f, 8f)
        }
        stroked(1.4f) {
            moveTo(18.6f, 10.6f)
            lineTo(20.4f, 11.4f)
        }
    }.build()

    /** Maintenance: gear (circle with eight teeth). */
    val Maintenance: ImageVector = builder("mehra_maintenance").apply {
        stroked(1.5f) { circle(12f, 12f, 3f) }
        stroked(2.1f) { circle(12f, 12f, 6.4f) }
        stroked(2.1f) {
            // 8 spokes from r=6.4 to r=8.6 at 45° steps
            moveTo(18.4f, 12f); lineTo(20.6f, 12f)
            moveTo(16.53f, 16.53f); lineTo(18.08f, 18.08f)
            moveTo(12f, 18.4f); lineTo(12f, 20.6f)
            moveTo(7.47f, 16.53f); lineTo(5.92f, 18.08f)
            moveTo(5.6f, 12f); lineTo(3.4f, 12f)
            moveTo(7.47f, 7.47f); lineTo(5.92f, 5.92f)
            moveTo(12f, 5.6f); lineTo(12f, 3.4f)
            moveTo(16.53f, 7.47f); lineTo(18.08f, 5.92f)
        }
    }.build()

    /** Reports: document with folded corner and lines. */
    val Reports: ImageVector = builder("mehra_reports").apply {
        stroked {
            moveTo(7f, 3.5f)
            lineTo(13.5f, 3.5f)
            lineTo(18f, 8f)
            lineTo(18f, 20.5f)
            lineTo(7f, 20.5f)
            close()
        }
        stroked(1.4f) {
            moveTo(13.5f, 3.5f)
            lineTo(13.5f, 8f)
            lineTo(18f, 8f)
        }
        stroked(1.4f) {
            moveTo(10f, 12.5f)
            lineTo(15f, 12.5f)
        }
        stroked(1.4f) {
            moveTo(10f, 16f)
            lineTo(15f, 16f)
        }
        stroked(1.4f) {
            moveTo(10f, 9f)
            lineTo(12f, 9f)
        }
    }.build()

    /** Users: ID card with portrait and lines. */
    val Users: ImageVector = builder("mehra_users").apply {
        stroked {
            moveTo(3.5f, 5.5f)
            lineTo(20.5f, 5.5f)
            lineTo(20.5f, 18.5f)
            lineTo(3.5f, 18.5f)
            close()
        }
        stroked(1.5f) { circle(8f, 10.5f, 1.9f) }
        stroked(1.4f) {
            moveTo(5.4f, 16.2f)
            curveTo(5.8f, 14.4f, 6.8f, 13.6f, 8f, 13.6f)
            curveTo(9.2f, 13.6f, 10.2f, 14.4f, 10.6f, 16.2f)
        }
        stroked(1.4f) {
            moveTo(14f, 9.5f)
            lineTo(18f, 9.5f)
        }
        stroked(1.4f) {
            moveTo(14f, 12.5f)
            lineTo(18f, 12.5f)
        }
        stroked(1.4f) {
            moveTo(14f, 15.5f)
            lineTo(16.5f, 15.5f)
        }
    }.build()

    /** Notifications: bell with clapper. */
    val Notifications: ImageVector = builder("mehra_notifications").apply {
        stroked {
            moveTo(6f, 16f)
            lineTo(6f, 10.8f)
            curveTo(6f, 7.6f, 8.6f, 5.5f, 12f, 5.5f)
            curveTo(15.4f, 5.5f, 18f, 7.6f, 18f, 10.8f)
            lineTo(18f, 16f)
            lineTo(19.5f, 18.5f)
            lineTo(4.5f, 18.5f)
            close()
        }
        stroked(1.5f) {
            moveTo(10.2f, 20.9f)
            curveTo(10.7f, 21.7f, 13.3f, 21.7f, 13.8f, 20.9f)
        }
    }.build()

    /** Settings: three sliders. */
    val Settings: ImageVector = builder("mehra_settings").apply {
        stroked(1.5f) {
            moveTo(4f, 7f); lineTo(7.1f, 7f)
            moveTo(10.9f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(13.1f, 12f)
            moveTo(16.9f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(5.1f, 17f)
            moveTo(8.9f, 17f); lineTo(20f, 17f)
        }
        stroked(1.5f) {
            circle(9f, 7f, 1.9f)
            circle(15f, 12f, 1.9f)
            circle(7f, 17f, 1.9f)
        }
    }.build()

    /** Check mark used on selected theme cards. */
    val Check: ImageVector = builder("mehra_check").apply {
        stroked(2.1f) {
            moveTo(5f, 12.5f)
            lineTo(9.7f, 17.2f)
            lineTo(19f, 7.5f)
        }
    }.build()
}
