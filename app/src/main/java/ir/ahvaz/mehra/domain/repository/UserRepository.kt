package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.CreateUserCommand
import ir.ahvaz.mehra.domain.model.RoleInfo
import java.time.LocalDate

interface UserRepository {
    suspend fun users(query: String = ""): AppResult<CachedData<List<AppUser>>>
    suspend fun roles(): AppResult<List<RoleInfo>>
    suspend fun save(command: CreateUserCommand): AppResult<AppUser>
    suspend fun setActive(userId: String, active: Boolean): AppResult<Unit>
    suspend fun resetPassword(userId: String, newPassword: String): AppResult<Unit>
    suspend fun auditLogs(from: LocalDate, to: LocalDate): AppResult<List<AuditLogEntry>>
}
