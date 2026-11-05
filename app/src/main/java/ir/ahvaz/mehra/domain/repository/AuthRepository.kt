package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class ServerInfo(
    val serverVersion: String? = null,
    val apiVersion: String? = null,
    val serverTime: Instant? = null,
    val databaseConnected: Boolean? = null,
)

interface AuthRepository {
    suspend fun login(username: String, password: String): AppResult<SessionUser>
    suspend fun logout(): AppResult<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit>
    suspend fun refreshSession(): AppResult<SessionUser>
    fun observeSession(): Flow<SessionUser?>
    suspend fun currentUser(): SessionUser?
    suspend fun isAuthenticated(): Boolean
    suspend fun serverInfo(baseUrlOverride: String? = null): AppResult<ServerInfo>
}
