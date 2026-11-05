package ir.ahvaz.mehra.core.ui.theme

/** The four approved skins of MEHRA. The selection is persisted in DataStore. */
enum class ThemePreset(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val isDark: Boolean,
) {
    GOLDEN_EXECUTIVE(
        id = "golden_executive",
        titleFa = "طلایی مدیران",
        titleEn = "Golden Executive",
        descriptionFa = "سرمه‌ای عمیق، طلایی شامپاینی و سفید گرم",
        isDark = false,
    ),
    OBSIDIAN_ROYAL(
        id = "obsidian_royal",
        titleFa = "ابسیدین سلطنتی",
        titleEn = "Obsidian Royal",
        descriptionFa = "ذغالی، مشکی مات و نقره‌ای",
        isDark = true,
    ),
    PLATINUM_LUXURY(
        id = "platinum_luxury",
        titleFa = "پلاتینیوم لوکس",
        titleEn = "Platinum Luxury",
        descriptionFa = "سفید صدفی، خاکستری روشن و طلایی مات",
        isDark = false,
    ),
    SILVER_HORIZON(
        id = "silver_horizon",
        titleFa = "افق نقره‌ای",
        titleEn = "Silver Horizon",
        descriptionFa = "سفید، آبی خاکستری و نقره‌ای",
        isDark = false,
    ),
    SAPPHIRE_ROYAL(
        id = "sapphire_royal",
        titleFa = "یاقوت کبود",
        titleEn = "Sapphire Royal",
        descriptionFa = "آبی یاقوتی عمیق، سفید یخی و نقره‌ای",
        isDark = false,
    ),
    EMERALD_CROWN(
        id = "emerald_crown",
        titleFa = "زمرد تاج",
        titleEn = "Emerald Crown",
        descriptionFa = "سبز زمردی، طلایی و مشکی مخملی",
        isDark = true,
    ),
    RUBY_NIGHT(
        id = "ruby_night",
        titleFa = "یاقوت شب",
        titleEn = "Ruby Night",
        descriptionFa = "قرمز یاقوتی، رزگلد و مشکی",
        isDark = true,
    );

    companion object {
        fun fromId(id: String?): ThemePreset = entries.firstOrNull { it.id == id } ?: GOLDEN_EXECUTIVE
    }
}
