package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.core.network.SessionEvent
import ir.ahvaz.mehra.core.network.SessionEventBus
import ir.ahvaz.mehra.core.security.AuthTokens
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.model.UserRole
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.ServerInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demo session: there is no backend, so the showcase build signs the reviewer in as the
 * hotel manager straight away. The login screen still works (any username/password) so the
 * flow can be demonstrated end to end.
 */
@Singleton
class DemoAuthRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val sessionEventBus: SessionEventBus,
) : AuthRepository {

    private val manager = SessionUser(
        id = "user-01",
        fullName = "سید مرتضی موسوی",
        username = "admin",
        roleCode = UserRole.SUPER_ADMIN.wire,
        roleTitleFa = UserRole.SUPER_ADMIN.labelFa,
        permissions = Permissions.all,
    )

    init {
        if (sessionManager.currentUser == null) {
            persistSession(manager)
        }
    }

    override suspend fun login(username: String, password: String): AppResult<SessionUser> {
        delay(280)
        if (username.isBlank() || password.isBlank()) {
            return AppResult.Failure(
                ir.ahvaz.mehra.core.common.AppError.validation("نام کاربری و رمز عبور را وارد کنید.")
            )
        }
        val user = manager.copy(
            fullName = if (username.trim() == "admin") manager.fullName else "کاربر نمایشی",
            username = username.trim(),
        )
        persistSession(user)
        return AppResult.Success(user)
    }

    override suspend fun logout(): AppResult<Unit> {
        sessionManager.clear()
        sessionEventBus.emit(SessionEvent.LoggedOut)
        return AppResult.Success(Unit)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> {
        delay(200)
        if (newPassword.length < 6) {
            return demoFailure("رمز عبور جدید باید حداقل ۶ کاراکتر باشد.")
        }
        return AppResult.Success(Unit)
    }

    override suspend fun refreshSession(): AppResult<SessionUser> =
        AppResult.Success(sessionManager.currentUser ?: manager)

    override fun observeSession(): Flow<SessionUser?> = sessionManager.session

    override suspend fun currentUser(): SessionUser? = sessionManager.currentUser

    override suspend fun isAuthenticated(): Boolean = sessionManager.isLoggedIn

    override suspend fun serverInfo(baseUrlOverride: String?): AppResult<ServerInfo> {
        delay(150)
        return AppResult.Success(
            ServerInfo(
                serverVersion = "1.0.0-demo",
                apiVersion = "v1",
                serverTime = Instant.now(),
                databaseConnected = true,
            )
        )
    }

    private fun persistSession(user: SessionUser) {
        val now = System.currentTimeMillis()
        sessionManager.persist(
            AuthTokens(
                accessToken = "demo-access-token",
                refreshToken = "demo-refresh-token",
                accessExpiresAtMillis = now + 12L * 60 * 60 * 1000,
                refreshExpiresAtMillis = now + 30L * 24 * 60 * 60 * 1000,
            ),
            user,
        )
    }
}
