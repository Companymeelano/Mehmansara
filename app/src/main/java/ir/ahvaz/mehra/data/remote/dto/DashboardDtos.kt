package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardSummaryDto(
    @SerializedName("businessDate") val businessDate: String? = null,
    @SerializedName("totalRooms") val totalRooms: Int? = null,
    @SerializedName("freeRooms") val freeRooms: Int? = null,
    @SerializedName("reservedRooms") val reservedRooms: Int? = null,
    @SerializedName("occupiedRooms") val occupiedRooms: Int? = null,
    @SerializedName("cleaningRooms") val cleaningRooms: Int? = null,
    @SerializedName("maintenanceRooms") val maintenanceRooms: Int? = null,
    @SerializedName("outOfServiceRooms") val outOfServiceRooms: Int? = null,
    @SerializedName("todayCheckIns") val todayCheckIns: Int? = null,
    @SerializedName("todayCheckOuts") val todayCheckOuts: Int? = null,
    @SerializedName("revenueTodayRial") val revenueTodayRial: Long? = null,
    @SerializedName("occupancyPercent") val occupancyPercent: Int? = null,
    @SerializedName("occupancySeries") val occupancySeries: List<OccupancyPointDto>? = null,
    @SerializedName("recentActivities") val recentActivities: List<ActivityDto>? = null,
    @SerializedName("alerts") val alerts: List<AlertDto>? = null,
)

data class OccupancyPointDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("occupied") val occupied: Int? = null,
    @SerializedName("total") val total: Int? = null,
)

data class ActivityDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("occurredAtUtc") val occurredAtUtc: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("actorName") val actorName: String? = null,
)

data class AlertDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
)
