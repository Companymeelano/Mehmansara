package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import ir.ahvaz.mehra.R

/**
 * Vazirmatn (SIL OFL) is bundled as five static weights so Persian shaping never depends on
 * a downloadable font provider — the app must render correctly on an isolated hotel LAN.
 */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/** Latin fallback used for digits inside mixed strings and for tabular figures. */
val VazirmatnRtl: FontFamily = Vazirmatn

private fun base(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0): TextStyle =
    TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
        textDirection = TextDirection.Content,
    )

fun mehraTypography(): Typography = Typography(
    displayLarge = base(42, 60, FontWeight.Bold),
    displayMedium = base(36, 52, FontWeight.Bold),
    displaySmall = base(30, 44, FontWeight.SemiBold),
    headlineLarge = base(26, 40, FontWeight.Bold),
    headlineMedium = base(23, 36, FontWeight.SemiBold),
    headlineSmall = base(21, 34, FontWeight.SemiBold),
    titleLarge = base(19, 32, FontWeight.SemiBold),
    titleMedium = base(17, 28, FontWeight.Medium),
    titleSmall = base(15, 26, FontWeight.Medium),
    bodyLarge = base(16, 30, FontWeight.Normal),
    bodyMedium = base(14, 26, FontWeight.Normal),
    bodySmall = base(12, 22, FontWeight.Normal),
    labelLarge = base(14, 24, FontWeight.Medium),
    labelMedium = base(12, 20, FontWeight.Medium),
    labelSmall = base(11, 18, FontWeight.Medium),
)
