package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
    @SerializedName("isRead") val isRead: Boolean? = null,
)

data class ReportDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("columns") val columns: List<String>? = null,
    @SerializedName("rows") val rows: List<List<String>>? = null,
    @SerializedName("totals") val totals: List<String>? = null,
    @SerializedName("notes") val notes: List<String>? = null,
    @SerializedName("from") val from: String? = null,
    @SerializedName("to") val to: String? = null,
    @SerializedName("generatedAtUtc") val generatedAtUtc: String? = null,
)

data class MarkReadRequestDto(
    @SerializedName("notificationIds") val notificationIds: List<String>? = null,
)
