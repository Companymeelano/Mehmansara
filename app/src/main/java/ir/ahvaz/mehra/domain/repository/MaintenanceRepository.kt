package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.StaffMember

interface MaintenanceRepository {
    suspend fun tickets(
        status: MaintenanceStatus? = null,
        priority: MaintenancePriority? = null,
    ): AppResult<CachedData<List<MaintenanceTicket>>>
    suspend fun ticket(ticketId: String): AppResult<MaintenanceTicket>
    suspend fun technicians(): AppResult<List<StaffMember>>
    suspend fun create(command: CreateMaintenanceCommand): AppResult<MaintenanceTicket>
    suspend fun assign(ticketId: String, technicianId: String): AppResult<Unit>
    suspend fun updateStatus(ticketId: String, status: MaintenanceStatus, note: String? = null): AppResult<Unit>
    suspend fun registerCost(ticketId: String, amountRial: Long, description: String?): AppResult<Unit>
    suspend fun releaseRoom(ticketId: String): AppResult<Unit>

    /**
     * Uploads locally captured fault photos (content:// URIs) for a ticket.
     * Requires the server's `POST /maintenance/tickets/{id}/photos` endpoint.
     */
    suspend fun uploadPhotos(ticketId: String, localUris: List<String>): AppResult<List<String>>
}
