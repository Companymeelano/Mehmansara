package ir.ahvaz.mehra.domain.model

/**
 * Guest loyalty tiers (v1.6.0). Thresholds are stays-count based and deliberately simple —
 * they can be moved to settings later without touching the UI.
 */
enum class LoyaltyTier(val labelFa: String, val minStays: Int) {
    BRONZE("برنزی", 0),
    SILVER("نقره‌ای", 3),
    GOLD("طلایی", 5),
    PLATINUM("پلاتینیوم", 10);

    companion object {
        /** The tier reached with [stays] completed stays. */
        fun tierOf(stays: Int): LoyaltyTier = entries.filter { stays >= it.minStays }.maxBy { it.minStays }

        /** The next tier to unlock, or null when already at the top. */
        fun nextTier(stays: Int): LoyaltyTier? = entries.firstOrNull { it.minStays > stays }
    }
}
