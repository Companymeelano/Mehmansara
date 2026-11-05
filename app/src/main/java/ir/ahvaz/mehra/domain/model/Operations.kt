package ir.ahvaz.mehra.domain.model

import java.time.Instant

data class StaffMember(
    val id: String,
    val fullName: String,
    val roleCode: String? = null,
    val roleTitleFa: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
)

data class HousekeepingTask(
    val id: String,
    val roomId: String,
    val roomNumber: String,
    val type: CleaningType,
    val status: HousekeepingTaskStatus,
    val assignedToId: String? = null,
    val assignedToName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val approvedAt: Instant? = null,
    val approvedByName: String? = null,
)

data class CreateCleaningCommand(
    val roomId: String,
    val type: CleaningType = CleaningType.CHECKOUT,
    val assigneeId: String? = null,
    val notes: String? = null,
)

data class HousekeepingPerformance(
    val employeeId: String,
    val employeeName: String,
    val completedCount: Int,
    val averageMinutes: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
) {
    val approvalRatePercent: Int
        get() {
            val total = approvedCount + rejectedCount
            return if (total == 0) 0 else (approvedCount * 100) / total
        }
}

data class MaintenanceTicket(
    val id: String,
    val code: String,
    val roomId: String,
    val roomNumber: String,
    val title: String,
    val description: String? = null,
    val priority: MaintenancePriority,
    val status: MaintenanceStatus,
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val costRial: Long = 0L,
    val imageUrls: List<String> = emptyList(),
    val reporterName: String? = null,
    val createdAt: Instant? = null,
    val resolvedAt: Instant? = null,
    val roomReleased: Boolean = false,
)

data class CreateMaintenanceCommand(
    val roomId: String,
    val title: String,
    val description: String,
    val priority: MaintenancePriority,
    /** Local content:// or file:// URIs captured/selected on the device, uploaded after create. */
    val localImageUris: List<String> = emptyList(),
)
