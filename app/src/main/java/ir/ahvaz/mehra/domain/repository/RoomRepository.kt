package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomDraft
import ir.ahvaz.mehra.domain.model.RoomFilterState
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.model.RoomLookups
import ir.ahvaz.mehra.domain.model.RoomStatus

interface RoomRepository {
    suspend fun rooms(filter: RoomFilterState = RoomFilterState()): AppResult<CachedData<List<Room>>>
    suspend fun room(roomId: String): AppResult<Room>
    suspend fun lookups(): AppResult<RoomLookups>
    suspend fun history(roomId: String): AppResult<List<RoomHistoryEntry>>
    suspend fun save(draft: RoomDraft): AppResult<Room>
    suspend fun changeStatus(roomId: String, status: RoomStatus, note: String? = null): AppResult<Unit>
    suspend fun requestCleaning(roomId: String, command: CreateCleaningCommand): AppResult<Unit>
    suspend fun createMaintenance(roomId: String, command: CreateMaintenanceCommand): AppResult<Unit>
}
