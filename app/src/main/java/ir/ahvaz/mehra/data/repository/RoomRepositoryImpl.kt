package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.parseLongInput
import ir.ahvaz.mehra.core.common.toLatinDigits
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.RoomsApi
import ir.ahvaz.mehra.data.remote.dto.CreateMaintenanceRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateTaskRequestDto
import ir.ahvaz.mehra.data.remote.dto.RoomDraftDto
import ir.ahvaz.mehra.data.remote.dto.RoomDto
import ir.ahvaz.mehra.data.remote.dto.RoomHistoryDto
import ir.ahvaz.mehra.data.remote.dto.RoomLookupsDto
import ir.ahvaz.mehra.data.remote.dto.RoomStatusRequestDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OperationType
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomDraft
import ir.ahvaz.mehra.domain.model.RoomFilterState
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.model.RoomLookups
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.repository.RoomRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val api: RoomsApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), RoomRepository {

    private val listType = object : TypeToken<List<RoomDto>>() {}.type
    private val historyType = object : TypeToken<List<RoomHistoryDto>>() {}.type

    override suspend fun rooms(filter: RoomFilterState): AppResult<CachedData<List<Room>>> {
        val key = "rooms:${filter.status?.wire.orEmpty()}:${filter.floor ?: ""}:${filter.query.trim()}"
        return readThrough(key, listType) {
            executor.value {
                api.list(
                    search = filter.query.trim().takeIf { it.isNotEmpty() },
                    status = filter.status?.wire,
                    floor = filter.floor,
                    page = 1,
                    pageSize = 200,
                )
            }.map { it.items.orEmpty() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun room(roomId: String): AppResult<Room> =
        executor.value { api.get(roomId) }.map { it.toDomain() }

    override suspend fun lookups(): AppResult<RoomLookups> {
        val key = "rooms:lookups"
        val result = readThrough(key, RoomLookupsDto::class.java) {
            executor.value { api.lookups() }
        }.mapCached { it.toDomain() }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun history(roomId: String): AppResult<List<RoomHistoryEntry>> {
        val result = readThrough("rooms:history:$roomId", historyType) {
            executor.value { api.history(roomId) }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun save(draft: RoomDraft): AppResult<Room> {
        val dto = draft.toDtoOrNull()
            ?: return AppResult.Failure(
                AppError.validation(
                    "اطلاعات اتاق کامل نیست.",
                    mapOf("roomNumber" to "شماره اتاق، طبقه، نوع اتاق و تعرفه الزامی است."),
                )
            )
        return if (draft.id == null) {
            executor.value { api.create(UUID.randomUUID().toString(), dto) }.map { it.toDomain() }
        } else {
            executor.value { api.update(draft.id, dto) }.map { it.toDomain() }
        }
    }

    override suspend fun changeStatus(roomId: String, status: RoomStatus, note: String?): AppResult<Unit> =
        executor.unit {
            api.changeStatus(roomId, RoomStatusRequestDto(status.wire, note))
        }

    override suspend fun requestCleaning(roomId: String, command: CreateCleaningCommand): AppResult<Unit> =
        executor.unit {
            api.requestCleaning(
                UUID.randomUUID().toString(),
                CreateTaskRequestDto(
                    roomId = roomId,
                    type = command.type.wire,
                    assigneeId = command.assigneeId,
                    notes = command.notes,
                ),
            )
        }

    override suspend fun createMaintenance(
        roomId: String,
        command: CreateMaintenanceCommand,
    ): AppResult<Unit> {
        val body = CreateMaintenanceRequestDto(
            roomId = roomId,
            title = command.title,
            description = command.description,
            priority = command.priority.wire,
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.unit { api.createMaintenance(idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.MAINTENANCE_CREATE,
                method = "POST",
                path = "/api/v1/maintenance/tickets",
                body = body,
                summaryFa = "درخواست تعمیرات اتاق ${command.title}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    private fun RoomDraft.toDtoOrNull(): RoomDraftDto? {
        val number = roomNumber.trim()
        val floorValue = parseLongInput(floor)?.toInt() ?: return null
        val capacityValue = parseLongInput(capacity)?.toInt() ?: return null
        val rate = parseLongInput(ratePerNightRial.toLatinDigits()) ?: return null
        if (number.isEmpty() || roomTypeId.isBlank() || capacityValue < 1 || rate < 0) return null
        return RoomDraftDto(
            roomNumber = number,
            floor = floorValue,
            capacity = capacityValue,
            roomTypeId = roomTypeId,
            ratePerNightRial = rate,
            amenities = amenities.toList(),
            status = status.wire,
            notes = notes.takeIf { it.isNotBlank() },
        )
    }
}
