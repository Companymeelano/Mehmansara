package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StaffDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("roleCode") val roleCode: String? = null,
    @SerializedName("roleTitle") val roleTitle: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
)

data class HousekeepingTaskDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("assignedToId") val assignedToId: String? = null,
    @SerializedName("assignedToName") val assignedToName: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
    @SerializedName("startedAtUtc") val startedAtUtc: String? = null,
    @SerializedName("finishedAtUtc") val finishedAtUtc: String? = null,
    @SerializedName("approvedAtUtc") val approvedAtUtc: String? = null,
    @SerializedName("approvedByName") val approvedByName: String? = null,
)

data class CreateTaskRequestDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("type") val type: String,
    @SerializedName("assigneeId") val assigneeId: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class AssignRequestDto(
    @SerializedName("assigneeId") val assigneeId: String,
)

data class CompleteTaskRequestDto(
    @SerializedName("note") val note: String? = null,
)

data class PerformanceDto(
    @SerializedName("employeeId") val employeeId: String? = null,
    @SerializedName("employeeName") val employeeName: String? = null,
    @SerializedName("completedCount") val completedCount: Int? = null,
    @SerializedName("averageMinutes") val averageMinutes: Int? = null,
    @SerializedName("approvedCount") val approvedCount: Int? = null,
    @SerializedName("rejectedCount") val rejectedCount: Int? = null,
)

data class MaintenanceTicketDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("assigneeId") val assigneeId: String? = null,
    @SerializedName("assigneeName") val assigneeName: String? = null,
    @SerializedName("costRial") val costRial: Long? = null,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("reporterName") val reporterName: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
    @SerializedName("resolvedAtUtc") val resolvedAtUtc: String? = null,
    @SerializedName("roomReleased") val roomReleased: Boolean? = null,
)

data class CreateMaintenanceRequestDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority") val priority: String,
)

data class MaintenanceStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("note") val note: String? = null,
)

data class CostRequestDto(
    @SerializedName("amountRial") val amountRial: Long,
    @SerializedName("description") val description: String? = null,
)
