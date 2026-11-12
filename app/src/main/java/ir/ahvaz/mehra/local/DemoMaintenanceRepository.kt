package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.MaintenanceRepository
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoMaintenanceRepository @Inject constructor(
    private val store: DemoStore,
) : MaintenanceRepository {

    override suspend fun tickets(
        status: MaintenanceStatus?,
        priority: MaintenancePriority?,
    ): AppResult<CachedData<List<MaintenanceTicket>>> {
        delay(140)
        val list = store.maintenanceTickets
            .filter { status == null || it.status == status }
            .filter { priority == null || it.priority == priority }
            .sortedWith(compareBy({ it.status.ordinal }, { it.priority.ordinal }))
        return demoCached(list)
    }

    override suspend fun ticket(ticketId: String): AppResult<MaintenanceTicket> =
        store.maintenanceTickets.firstOrNull { it.id == ticketId }?.let { demoOk(it) }
            ?: demoNotFound("تیکت مورد نظر یافت نشد.")

    override suspend fun technicians(): AppResult<List<StaffMember>> = demoOk(store.technicians)

    override suspend fun create(command: CreateMaintenanceCommand): AppResult<MaintenanceTicket> {
        delay(240)
        val room = store.room(command.roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        if (command.title.isBlank()) return demoFailure("عنوان خرابی را وارد کنید.")
        val ticket = MaintenanceTicket(
            id = store.nextId("mt"),
            code = "MT-${2210 + store.maintenanceTickets.size}",
            roomId = room.id,
            roomNumber = room.roomNumber,
            title = command.title.trim(),
            description = command.description.trim().ifBlank { null },
            priority = command.priority,
            status = MaintenanceStatus.OPEN,
            reporterName = DemoStore.DEMO_OPERATOR,
            imageUrls = command.localImageUris,
            createdAt = Instant.now(),
        )
        store.maintenanceTickets.add(0, ticket)
        val roomIndex = store.rooms.indexOfFirst { it.id == room.id }
        if (roomIndex >= 0 && room.status != RoomStatus.OCCUPIED) {
            store.rooms[roomIndex] = room.copy(
                status = RoomStatus.MAINTENANCE,
                notes = "تیکت ${ticket.code}: ${ticket.title}",
                updatedAt = Instant.now(),
            )
        }
        return demoOk(ticket)
    }

    override suspend fun assign(ticketId: String, technicianId: String): AppResult<Unit> {
        delay(180)
        val index = store.maintenanceTickets.indexOfFirst { it.id == ticketId }
        if (index < 0) return demoNotFound("تیکت مورد نظر یافت نشد.")
        val technician = store.technicians.firstOrNull { it.id == technicianId }
            ?: return demoNotFound("تعمیرکار انتخاب شده یافت نشد.")
        val ticket = store.maintenanceTickets[index]
        store.maintenanceTickets[index] = ticket.copy(
            status = if (ticket.status == MaintenanceStatus.OPEN) MaintenanceStatus.ASSIGNED else ticket.status,
            assigneeId = technician.id,
            assigneeName = technician.fullName,
        )
        return AppResult.Success(Unit)
    }

    override suspend fun updateStatus(
        ticketId: String,
        status: MaintenanceStatus,
        note: String?,
    ): AppResult<Unit> {
        delay(200)
        val index = store.maintenanceTickets.indexOfFirst { it.id == ticketId }
        if (index < 0) return demoNotFound("تیکت مورد نظر یافت نشد.")
        val ticket = store.maintenanceTickets[index]
        store.maintenanceTickets[index] = ticket.copy(
            status = status,
            description = note?.ifBlank { null } ?: ticket.description,
            resolvedAt = if (status == MaintenanceStatus.RESOLVED || status == MaintenanceStatus.CLOSED) {
                Instant.now()
            } else {
                ticket.resolvedAt
            },
        )
        return AppResult.Success(Unit)
    }

    override suspend fun registerCost(
        ticketId: String,
        amountRial: Long,
        description: String?,
    ): AppResult<Unit> {
        delay(200)
        if (amountRial <= 0L) return demoFailure("مبلغ هزینه معتبر نیست.")
        val index = store.maintenanceTickets.indexOfFirst { it.id == ticketId }
        if (index < 0) return demoNotFound("تیکت مورد نظر یافت نشد.")
        val ticket = store.maintenanceTickets[index]
        store.maintenanceTickets[index] = ticket.copy(
            costRial = ticket.costRial + amountRial,
            description = description?.ifBlank { null } ?: ticket.description,
        )
        return AppResult.Success(Unit)
    }

    override suspend fun releaseRoom(ticketId: String): AppResult<Unit> {
        delay(200)
        val index = store.maintenanceTickets.indexOfFirst { it.id == ticketId }
        if (index < 0) return demoNotFound("تیکت مورد نظر یافت نشد.")
        val ticket = store.maintenanceTickets[index]
        store.maintenanceTickets[index] = ticket.copy(roomReleased = true)
        val roomIndex = store.rooms.indexOfFirst { it.id == ticket.roomId }
        if (roomIndex >= 0) {
            store.rooms[roomIndex] = store.rooms[roomIndex].copy(
                status = RoomStatus.CLEANING,
                notes = null,
                updatedAt = Instant.now(),
            )
        }
        return AppResult.Success(Unit)
    }

    override suspend fun uploadPhotos(ticketId: String, localUris: List<String>): AppResult<List<String>> {
        delay(200)
        val index = store.maintenanceTickets.indexOfFirst { it.id == ticketId }
        if (index < 0) return demoNotFound("تیکت مورد نظر یافت نشد.")
        val ticket = store.maintenanceTickets[index]
        val merged = (ticket.imageUrls + localUris).distinct()
        store.maintenanceTickets[index] = ticket.copy(imageUrls = merged)
        return demoOk(merged)
    }
}
