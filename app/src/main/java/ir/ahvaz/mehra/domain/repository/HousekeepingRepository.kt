package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.HousekeepingPerformance
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import java.time.LocalDate

interface HousekeepingRepository {
    suspend fun tasks(status: HousekeepingTaskStatus? = null): AppResult<CachedData<List<HousekeepingTask>>>
    suspend fun staff(): AppResult<List<StaffMember>>
    suspend fun create(command: CreateCleaningCommand): AppResult<HousekeepingTask>
    suspend fun assign(taskId: String, staffId: String): AppResult<Unit>
    suspend fun start(taskId: String): AppResult<Unit>
    suspend fun complete(taskId: String, note: String?): AppResult<Unit>
    suspend fun approve(taskId: String): AppResult<Unit>
    suspend fun performance(from: LocalDate, to: LocalDate): AppResult<List<HousekeepingPerformance>>
}
