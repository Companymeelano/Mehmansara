package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.DashboardApi
import ir.ahvaz.mehra.data.remote.dto.DashboardSummaryDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import ir.ahvaz.mehra.domain.repository.DashboardRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val api: DashboardApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), DashboardRepository {

    override suspend fun load(businessDate: LocalDate?): AppResult<CachedData<DashboardSnapshot>> {
        val dateParam = businessDate?.let { IsoFormat.date(it) }
        return readThrough("dashboard:${dateParam ?: "today"}", DashboardSummaryDto::class.java) {
            executor.value { api.summary(dateParam) }
        }.mapCached { it.toDomain() }
    }
}
