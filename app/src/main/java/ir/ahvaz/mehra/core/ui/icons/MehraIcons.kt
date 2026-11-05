package ir.ahvaz.mehra.core.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ir.ahvaz.mehra.R

/**
 * MEHRA's own icon set.
 *
 * Every glyph is authored as an SVG master in the `design/icons/svg` folder (with metallic
 * gradients for the 3D look) and shipped to the app as a vector drawable in
 * `res/drawable/ic_mehra_*.xml`. At runtime the glyph is drawn by
 * [ir.ahvaz.mehra.core.ui.components.Icon3D] on top of a theme-driven gradient chip, so the
 * icon always matches the user-selected theme.
 *
 * Accessors are `@Composable` getters because `ImageVector.vectorResource` is composable.
 */
object MehraIcons {

    /** Brand mark: a Persian mihrab arch with a dot. */
    val Logo: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_logo)

    /** Dashboard: three rounded bars of different heights. */
    val Dashboard: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_dashboard)

    /** Rooms: bed with headboard, mattress and pillow. */
    val Rooms: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_rooms)

    /** Reservations: calendar with a check mark. */
    val Reservations: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_reservations)

    /** Reception: concierge bell. */
    val Reception: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_reception)

    /** More: 3x3 grid of dots. */
    val More: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_more)

    /** Guests: two travellers. */
    val Guests: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_guests)

    /** Finance: banknote with a coin. */
    val Finance: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_finance)

    /** Housekeeping: spray bottle with mist. */
    val Housekeeping: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_housekeeping)

    /** Maintenance: gear. */
    val Maintenance: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_maintenance)

    /** Reports: document with folded corner and lines. */
    val Reports: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_reports)

    /** Users: ID card with portrait and lines. */
    val Users: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_users)

    /** Notifications: bell with clapper. */
    val Notifications: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_notifications)

    /** Settings: three sliders. */
    val Settings: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_settings)

    /** Check mark used on selected theme cards. */
    val Check: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mehra_check)
}
