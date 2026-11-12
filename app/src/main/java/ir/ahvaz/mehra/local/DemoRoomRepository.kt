package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomDraft
import ir.ahvaz.mehra.domain.model.RoomFilterState
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.model.RoomLookups
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.repository.RoomRepository
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoRoomRepository @Inject constructor(
    private val store: DemoStore,
) : RoomRepository {

    override suspend fun rooms(filter: RoomFilterState): AppResult<CachedData<List<Room>>> {
        delay(150)
        val query = filter.query.trim()
        val list = store.rooms.asSequence()
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.floor == null || it.floor == filter.floor }
            .filter {
                query.isEmpty() ||
                    it.roomNumber.contains(query) ||
                    it.roomTypeName.contains(query) ||
                    it.currentGuestName?.contains(query) == true
            }
            .sortedBy { it.roomNumber }
            .toList()
        return demoCached(list)
    }

    override suspend fun room(roomId: String): AppResult<Room> =
        store.room(roomId)?.let { demoOk(it) } ?: demoNotFound("اتاق مورد نظر یافت نشد.")

    override suspend fun lookups(): AppResult<RoomLookups> = demoOk(
        RoomLookups(
            roomTypes = store.roomTypes,
            amenities = store.amenities,
            floors = store.floors,
        )
    )

    override suspend fun history(roomId: String): AppResult<List<RoomHistoryEntry>> {
        store.room(roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        val entries = store.stays
            .filter { it.roomId == roomId }
            .map { stay ->
                RoomHistoryEntry(
                    id = stay.id,
                    guestName = stay.guestName,
                    checkIn = store.localDateOf(stay.checkInAt),
                    checkOut = store.localDateOf(stay.actualCheckOutAt) ?: stay.plannedCheckOut,
                    totalRial = stay.totalRial,
                    status = stay.status,
                )
            }
            .sortedByDescending { it.checkIn }
        return demoOk(entries)
    }

    override suspend fun save(draft: RoomDraft): AppResult<Room> {
        delay(240)
        val number = draft.roomNumber.trim()
        if (number.isEmpty()) return demoFailure("شماره اتاق را وارد کنید.")
        val rate = draft.ratePerNightRial.demoDigits().toLongOrNull()
        if (rate == null || rate <= 0L) return demoFailure("نرخ هر شب باید عددی بزرگ‌تر از صفر باشد.")
        val floor = draft.floor.demoDigits().toIntOrNull() ?: 1
        val capacity = draft.capacity.demoDigits().toIntOrNull() ?: 2
        val type = store.roomTypes.firstOrNull { it.id == draft.roomTypeId }

        val existing = draft.id?.let { store.room(it) }
        val room = Room(
            id = existing?.id ?: store.nextId("room"),
            roomNumber = number,
            floor = floor,
            capacity = capacity,
            roomTypeId = draft.roomTypeId,
            roomTypeName = type?.title ?: existing?.roomTypeName ?: "اتاق",
            ratePerNightRial = rate,
            status = existing?.status ?: draft.status,
            amenities = draft.amenities.toList(),
            notes = draft.notes.ifBlank { null },
            currentGuestName = existing?.currentGuestName,
            currentStayId = existing?.currentStayId,
            nextReservationAt = existing?.nextReservationAt,
            updatedAt = Instant.now(),
        )

        val index = store.rooms.indexOfFirst { it.id == room.id }
        if (index >= 0) store.rooms[index] = room else store.rooms += room
        return demoOk(room)
    }

    override suspend fun changeStatus(roomId: String, status: RoomStatus, note: String?): AppResult<Unit> {
        delay(180)
        val index = store.rooms.indexOfFirst { it.id == roomId }
        if (index < 0) return demoNotFound("اتاق مورد نظر یافت نشد.")
        val current = store.rooms[index]
        store.rooms[index] = current.copy(
            status = status,
            notes = note?.ifBlank { null } ?: current.notes,
            updatedAt = Instant.now(),
        )
        return AppResult.Success(Unit)
    }

    override suspend fun requestCleaning(
        roomId: String,
        command: CreateCleaningCommand,
    ): AppResult<Unit> {
        delay(200)
        val room = store.room(roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        val task = HousekeepingTask(
            id = store.nextId("hk"),
            roomId = room.id,
            roomNumber = room.roomNumber,
            type = command.type,
            status = if (command.assigneeId != null) HousekeepingTaskStatus.ASSIGNED else HousekeepingTaskStatus.PENDING,
            assignedToId = command.assigneeId,
            assignedToName = store.housekeepingStaff.nameOf(command.assigneeId),
            notes = command.notes,
            createdAt = Instant.now(),
        )
        store.housekeepingTasks.add(0, task)
        if (room.status == RoomStatus.FREE || room.status == RoomStatus.RESERVED) {
            val index = store.rooms.indexOfFirst { it.id == room.id }
            if (index >= 0) {
                store.rooms[index] = room.copy(status = RoomStatus.CLEANING, updatedAt = Instant.now())
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun createMaintenance(
        roomId: String,
        command: CreateMaintenanceCommand,
    ): AppResult<Unit> {
        delay(220)
        val room = store.room(roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        if (command.title.isBlank()) return demoFailure("عنوان خرابی را وارد کنید.")
        val ticket = MaintenanceTicket(
            id = store.nextId("mt"),
            code = "MT-${2210 + store.maintenanceTickets.size}",
            roomId = room.id,
            roomNumber = room.roomNumber,
            title = command.title.trim(),
            description = command.description.ifBlank { null },
            priority = command.priority,
            status = MaintenanceStatus.OPEN,
            reporterName = DemoStore.DEMO_OPERATOR,
            createdAt = Instant.now(),
            imageUrls = command.localImageUris,
        )
        store.maintenanceTickets.add(0, ticket)
        val index = store.rooms.indexOfFirst { it.id == room.id }
        if (index >= 0 && room.status != RoomStatus.OCCUPIED) {
            store.rooms[index] = room.copy(
                status = RoomStatus.MAINTENANCE,
                notes = "تیکت ${ticket.code}: ${ticket.title}",
                updatedAt = Instant.now(),
            )
        }
        return AppResult.Success(Unit)
    }

    @Suppress("unused")
    private fun defaultPriority(): MaintenancePriority = MaintenancePriority.NORMAL
}
