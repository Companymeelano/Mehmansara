package ir.ahvaz.mehra.domain.model

import java.time.Instant
import java.time.LocalDate

data class OccupancyPoint(
    val date: LocalDate,
    val occupied: Int,
    val total: Int,
) {
    val percent: Int get() = if (total <= 0) 0 else (occupied * 100) / total
}

/** One day of received revenue (rial) — feeds the 7-day revenue chart. */
data class RevenuePoint(
    val date: LocalDate,
    val amountRial: Long,
)

enum class ActivityKind(val labelFa: String) {
    RESERVATION("رزرو"),
    CHECK_IN("پذیرش"),
    CHECK_OUT("خروج"),
    PAYMENT("پرداخت"),
    ROOM_STATUS("وضعیت اتاق"),
    MAINTENANCE("تعمیرات"),
    HOUSEKEEPING("خدمات"),
    SYSTEM("سیستم");

    companion object {
        fun fromWire(value: String?): ActivityKind = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: SYSTEM
    }
}

data class ActivityItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val occurredAt: Instant? = null,
    val kind: ActivityKind = ActivityKind.SYSTEM,
    val actorName: String? = null,
)

data class AlertItem(
    val id: String,
    val title: String,
    val message: String? = null,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val createdAt: Instant? = null,
)

data class DashboardSnapshot(
    val totalRooms: Int = 0,
    val freeRooms: Int = 0,
    val reservedRooms: Int = 0,
    val occupiedRooms: Int = 0,
    val cleaningRooms: Int = 0,
    val maintenanceRooms: Int = 0,
    val outOfServiceRooms: Int = 0,
    val todayCheckIns: Int = 0,
    val todayCheckOuts: Int = 0,
    val revenueTodayRial: Long = 0L,
    val occupancyPercent: Int = 0,
    val occupancySeries: List<OccupancyPoint> = emptyList(),
    val revenueSeries: List<RevenuePoint> = emptyList(),
    val recentActivities: List<ActivityItem> = emptyList(),
    val alerts: List<AlertItem> = emptyList(),
    val businessDate: LocalDate? = null,
) {
    val hasRooms: Boolean get() = totalRooms > 0
}
