package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.ReportsApi
import ir.ahvaz.mehra.data.remote.dto.ReportDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType
import ir.ahvaz.mehra.domain.repository.ReportRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val api: ReportsApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), ReportRepository {

    override suspend fun load(type: ReportType, from: LocalDate, to: LocalDate): AppResult<ReportTable> {
        val result = readThrough(
            "reports:${type.key}:${IsoFormat.date(from)}:${IsoFormat.date(to)}",
            ReportDto::class.java,
        ) {
            executor.value { api.report(type.key, IsoFormat.date(from), IsoFormat.date(to)) }
        }.mapCached { it.toDomain(type) }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }
}
