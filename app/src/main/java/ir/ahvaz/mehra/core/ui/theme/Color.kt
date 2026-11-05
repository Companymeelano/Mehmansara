package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Semantic colours that Material 3 does not cover (statuses, charts, brand gold). */
@Immutable
data class MehraColors(
    val isDark: Boolean,
    val gold: Color,
    val goldSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val statusFree: Color,
    val statusReserved: Color,
    val statusOccupied: Color,
    val statusCleaning: Color,
    val statusMaintenance: Color,
    val statusOutOfService: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartGrid: Color,
    val cardBorder: Color,
)

// ---------------------------------------------------------------------------
// Palette building blocks
// ---------------------------------------------------------------------------
private val NavyDeep = Color(0xFF0E1524)
private val NavyMid = Color(0xFF1B2740)
private val Champagne = Color(0xFFC8A96A)
private val ChampagneSoft = Color(0xFFE8D6AE)
private val WarmWhite = Color(0xFFFBF7EF)
private val WarmSurface = Color(0xFFFFFFFF)
private val WarmOutline = Color(0xFFE3D9C6)
private val Ink = Color(0xFF17202F)
private val MutedInk = Color(0xFF5A6474)

private val ObsidianBlack = Color(0xFF090C12)
private val ObsidianSurface = Color(0xFF12161F)
private val ObsidianCard = Color(0xFF171C27)
private val ObsidianOutline = Color(0xFF2C3341)
private val SilverAccent = Color(0xFFC9CFD8)
private val SilverSoft = Color(0xFFE6EAEF)
private val ObsidianText = Color(0xFFEDEFF3)

private val PearlWhite = Color(0xFFF7F6F3)
private val PearlSurface = Color(0xFFFFFFFF)
private val PearlGray = Color(0xFFEDECE8)
private val PearlOutline = Color(0xFFDDDBD4)
private val MatteGold = Color(0xFFA98F5C)
private val MatteGoldSoft = Color(0xFFE4D9C2)
private val PearlInk = Color(0xFF1F2126)

private val HorizonWhite = Color(0xFFF8FAFC)
private val HorizonSurface = Color(0xFFFFFFFF)
private val SlateBlue = Color(0xFF4A6B8A)
private val SlateBlueDeep = Color(0xFF2C4359)
private val SlateSoft = Color(0xFFDFE7EF)
private val SilverLine = Color(0xFFD8E0E8)
private val HorizonInk = Color(0xFF1B2836)

private val Success = Color(0xFF2E7D5B)
private val SuccessSoft = Color(0xFFE1F1E9)
private val Warning = Color(0xFFB7791F)
private val WarningSoft = Color(0xFFFBEFD8)
private val Danger = Color(0xFFB3261E)
private val DangerSoft = Color(0xFFF9E2E0)
private val Info = Color(0xFF2F6690)
private val InfoSoft = Color(0xFFE3EEF7)

private val SuccessDark = Color(0xFF57C79B)
private val WarningDark = Color(0xFFE0B155)
private val DangerDark = Color(0xFFE5837C)
private val InfoDark = Color(0xFF7FB7DC)

// ---------------------------------------------------------------------------
// Color schemes per preset
// ---------------------------------------------------------------------------
fun ThemePreset.colorScheme(): ColorScheme = when (this) {
    ThemePreset.GOLDEN_EXECUTIVE -> lightColorScheme(
        primary = NavyDeep,
        onPrimary = Color.White,
        primaryContainer = NavyMid,
        onPrimaryContainer = ChampagneSoft,
        secondary = Champagne,
        onSecondary = NavyDeep,
        secondaryContainer = ChampagneSoft,
        onSecondaryContainer = NavyDeep,
        tertiary = Color(0xFF5C6B82),
        onTertiary = Color.White,
        background = WarmWhite,
        onBackground = Ink,
        surface = WarmSurface,
        onSurface = Ink,
        surfaceVariant = Color(0xFFF3EDE1),
        onSurfaceVariant = MutedInk,
        outline = WarmOutline,
        outlineVariant = Color(0xFFEFE7D8),
        error = Danger,
        onError = Color.White,
        errorContainer = DangerSoft,
        onErrorContainer = Danger,
        scrim = Color(0x99000000),
    )

    ThemePreset.OBSIDIAN_ROYAL -> darkColorScheme(
        primary = SilverSoft,
        onPrimary = ObsidianBlack,
        primaryContainer = Color(0xFF2A3140),
        onPrimaryContainer = SilverSoft,
        secondary = Champagne,
        onSecondary = ObsidianBlack,
        secondaryContainer = Color(0xFF33291A),
        onSecondaryContainer = ChampagneSoft,
        tertiary = SilverAccent,
        onTertiary = ObsidianBlack,
        background = ObsidianBlack,
        onBackground = ObsidianText,
        surface = ObsidianSurface,
        onSurface = ObsidianText,
        surfaceVariant = ObsidianCard,
        onSurfaceVariant = Color(0xFFA9B1BE),
        outline = ObsidianOutline,
        outlineVariant = Color(0xFF232936),
        error = DangerDark,
        onError = ObsidianBlack,
        errorContainer = Color(0xFF3A1F1D),
        onErrorContainer = DangerDark,
        scrim = Color(0xCC000000),
    )

    ThemePreset.PLATINUM_LUXURY -> lightColorScheme(
        primary = PearlInk,
        onPrimary = Color.White,
        primaryContainer = PearlGray,
        onPrimaryContainer = PearlInk,
        secondary = MatteGold,
        onSecondary = Color.White,
        secondaryContainer = MatteGoldSoft,
        onSecondaryContainer = Color(0xFF3B2F17),
        tertiary = Color(0xFF7A7568),
        onTertiary = Color.White,
        background = PearlWhite,
        onBackground = PearlInk,
        surface = PearlSurface,
        onSurface = PearlInk,
        surfaceVariant = Color(0xFFF1F0EC),
        onSurfaceVariant = Color(0xFF6B675E),
        outline = PearlOutline,
        outlineVariant = Color(0xFFEAE8E2),
        error = Danger,
        onError = Color.White,
        errorContainer = DangerSoft,
        onErrorContainer = Danger,
        scrim = Color(0x99000000),
    )

    ThemePreset.SILVER_HORIZON -> lightColorScheme(
        primary = SlateBlueDeep,
        onPrimary = Color.White,
        primaryContainer = SlateSoft,
        onPrimaryContainer = SlateBlueDeep,
        secondary = SlateBlue,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8EFF6),
        onSecondaryContainer = SlateBlueDeep,
        tertiary = Color(0xFF7D8FA3),
        onTertiary = Color.White,
        background = HorizonWhite,
        onBackground = HorizonInk,
        surface = HorizonSurface,
        onSurface = HorizonInk,
        surfaceVariant = Color(0xFFEEF3F8),
        onSurfaceVariant = Color(0xFF5B6B7C),
        outline = SilverLine,
        outlineVariant = Color(0xFFE9EFF5),
        error = Danger,
        onError = Color.White,
        errorContainer = DangerSoft,
        onErrorContainer = Danger,
        scrim = Color(0x99000000),
    )
}

fun ThemePreset.mehraColors(): MehraColors = when (this) {
    ThemePreset.GOLDEN_EXECUTIVE -> MehraColors(
        isDark = false,
        gold = Champagne,
        goldSoft = ChampagneSoft,
        success = Success,
        successSoft = SuccessSoft,
        warning = Warning,
        warningSoft = WarningSoft,
        info = Info,
        infoSoft = InfoSoft,
        danger = Danger,
        dangerSoft = DangerSoft,
        statusFree = Color(0xFF2E7D5B),
        statusReserved = Color(0xFF2F6690),
        statusOccupied = Color(0xFF8A6A2F),
        statusCleaning = Color(0xFF7A5EA8),
        statusMaintenance = Color(0xFFB7791F),
        statusOutOfService = Color(0xFF6B7280),
        chartPrimary = NavyDeep,
        chartSecondary = Champagne,
        chartTertiary = Color(0xFF9FAFC4),
        chartGrid = WarmOutline,
        cardBorder = Color(0xFFEDE4D3),
    )

    ThemePreset.OBSIDIAN_ROYAL -> MehraColors(
        isDark = true,
        gold = Champagne,
        goldSoft = Color(0xFF4A3B22),
        success = SuccessDark,
        successSoft = Color(0xFF17342A),
        warning = WarningDark,
        warningSoft = Color(0xFF3A2E15),
        info = InfoDark,
        infoSoft = Color(0xFF16283A),
        danger = DangerDark,
        dangerSoft = Color(0xFF3A1F1D),
        statusFree = SuccessDark,
        statusReserved = InfoDark,
        statusOccupied = Champagne,
        statusCleaning = Color(0xFFB49BE0),
        statusMaintenance = WarningDark,
        statusOutOfService = Color(0xFF98A1AF),
        chartPrimary = SilverSoft,
        chartSecondary = Champagne,
        chartTertiary = Color(0xFF5A6474),
        chartGrid = ObsidianOutline,
        cardBorder = Color(0xFF222836),
    )

    ThemePreset.PLATINUM_LUXURY -> MehraColors(
        isDark = false,
        gold = MatteGold,
        goldSoft = MatteGoldSoft,
        success = Success,
        successSoft = SuccessSoft,
        warning = Warning,
        warningSoft = WarningSoft,
        info = Info,
        infoSoft = InfoSoft,
        danger = Danger,
        dangerSoft = DangerSoft,
        statusFree = Success,
        statusReserved = Color(0xFF4A6B8A),
        statusOccupied = Color(0xFF8A7038),
        statusCleaning = Color(0xFF6E5A93),
        statusMaintenance = Warning,
        statusOutOfService = Color(0xFF75726A),
        chartPrimary = PearlInk,
        chartSecondary = MatteGold,
        chartTertiary = Color(0xFFBDBAB1),
        chartGrid = PearlOutline,
        cardBorder = Color(0xFFE9E7E1),
    )

    ThemePreset.SILVER_HORIZON -> MehraColors(
        isDark = false,
        gold = Color(0xFF8E9BA8),
        goldSoft = Color(0xFFE3E9EF),
        success = Success,
        successSoft = SuccessSoft,
        warning = Warning,
        warningSoft = WarningSoft,
        info = Info,
        infoSoft = InfoSoft,
        danger = Danger,
        dangerSoft = DangerSoft,
        statusFree = Success,
        statusReserved = SlateBlue,
        statusOccupied = Color(0xFF3B5E7D),
        statusCleaning = Color(0xFF6B6FA8),
        statusMaintenance = Warning,
        statusOutOfService = Color(0xFF6B7A88),
        chartPrimary = SlateBlueDeep,
        chartSecondary = SlateBlue,
        chartTertiary = Color(0xFFAFC3D4),
        chartGrid = SilverLine,
        cardBorder = Color(0xFFE4EBF2),
    )
}
