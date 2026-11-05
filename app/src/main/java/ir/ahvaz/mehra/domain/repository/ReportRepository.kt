package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType
import java.time.LocalDate

interface ReportRepository {
    suspend fun load(type: ReportType, from: LocalDate, to: LocalDate): AppResult<ReportTable>
}
