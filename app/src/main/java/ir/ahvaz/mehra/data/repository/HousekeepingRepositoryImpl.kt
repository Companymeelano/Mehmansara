package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.HousekeepingApi
import ir.ahvaz.mehra.data.remote.dto.AssignRequestDto
import ir.ahvaz.mehra.data.remote.dto.CompleteTaskRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateTaskRequestDto
import ir.ahvaz.mehra.data.remote.dto.HousekeepingTaskDto
import ir.ahvaz.mehra.data.remote.dto.PerformanceDto
import ir.ahvaz.mehra.data.remote.dto.StaffDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.HousekeepingPerformance
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.HousekeepingRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HousekeepingRepositoryImpl @Inject constructor(
    private val api: HousekeepingApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), HousekeepingRepository {

    private val taskListType = object : TypeToken<List<HousekeepingTaskDto>>() {}.type
    private val staffListType = object : TypeToken<List<StaffDto>>() {}.type
    private val performanceListType = object : TypeToken<List<PerformanceDto>>() {}.type

    override suspend fun tasks(status: HousekeepingTaskStatus?): AppResult<CachedData<List<HousekeepingTask>>> =
        readThrough("housekeeping:tasks:${status?.wire.orEmpty()}", taskListType) {
            executor.value { api.tasks(status?.wire) }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }

    override suspend fun staff(): AppResult<List<StaffMember>> {
        val result = readThrough("housekeeping:staff", staffListType) {
            executor.value { api.staff() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun create(command: CreateCleaningCommand): AppResult<HousekeepingTask> =
        executor.value {
            api.create(
                UUID.randomUUID().toString(),
                CreateTaskRequestDto(
                    roomId = command.roomId,
                    type = command.type.wire,
                    assigneeId = command.assigneeId,
                    notes = command.notes,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun assign(taskId: String, staffId: String): AppResult<Unit> =
        executor.unit { api.assign(taskId, AssignRequestDto(staffId)) }

    override suspend fun start(taskId: String): AppResult<Unit> =
        executor.unit { api.start(taskId) }

    override suspend fun complete(taskId: String, note: String?): AppResult<Unit> =
        executor.unit { api.complete(taskId, CompleteTaskRequestDto(note)) }

    override suspend fun approve(taskId: String): AppResult<Unit> =
        executor.unit { api.approve(taskId) }

    override suspend fun performance(from: LocalDate, to: LocalDate): AppResult<List<HousekeepingPerformance>> {
        val result = readThrough(
            "housekeeping:performance:${IsoFormat.date(from)}:${IsoFormat.date(to)}",
            performanceListType,
        ) {
            executor.value { api.performance(IsoFormat.date(from), IsoFormat.date(to)) }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }
}
