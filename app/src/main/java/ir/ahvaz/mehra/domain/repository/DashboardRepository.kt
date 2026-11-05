package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import java.time.LocalDate

interface DashboardRepository {
    suspend fun load(businessDate: LocalDate? = null): AppResult<CachedData<DashboardSnapshot>>
}
