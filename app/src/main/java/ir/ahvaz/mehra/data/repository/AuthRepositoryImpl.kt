package ir.ahvaz.mehra.data.repository

import android.os.Build
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.di.RefreshClient
import ir.ahvaz.mehra.core.network.ApiErrorCodes
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.SessionEvent
import ir.ahvaz.mehra.core.network.SessionEventBus
import ir.ahvaz.mehra.core.security.AuthTokens
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.AuthApi
import ir.ahvaz.mehra.data.remote.dto.ChangePasswordRequestDto
import ir.ahvaz.mehra.data.remote.dto.LoginRequestDto
import ir.ahvaz.mehra.data.remote.dto.LogoutRequestDto
import ir.ahvaz.mehra.data.remote.dto.ServerInfoDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val executor: ApiExecutor,
    private val sessionManager: SessionManager,
    private val serverUrlProvider: ServerUrlProvider,
    private val responseCache: ResponseCache,
    private val offlineQueue: OfflineQueue,
    private val sessionEventBus: SessionEventBus,
    @RefreshClient private val plainClient: OkHttpClient,
    private val gson: Gson,
) : AuthRepository {

    override suspend fun login(username: String, password: String): AppResult<SessionUser> {
        if (!serverUrlProvider.isConfigured()) {
            return AppResult.Failure(AppError.validation("ابتدا آدرس سرور را در تنظیمات وارد کنید."))
        }
        val result = executor.value {
            authApi.login(
                LoginRequestDto(
                    username = username.trim(),
                    password = password,
                    deviceId = sessionManager.deviceId,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                )
            )
        }
        return when (result) {
            is AppResult.Success -> {
                val dto = result.data
                val access = dto.accessToken
                val refresh = dto.refreshToken
                val user = dto.user?.toDomain()
                if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
                    AppResult.Failure(AppError.server("پاسخ سرور برای ورود نامعتبر است."))
                } else if (user == null) {
                    AppResult.Failure(AppError.server("اطلاعات کاربر در پاسخ سرور یافت نشد."))
                } else {
                    val accessExpiry = IsoFormat.parseInstant(dto.accessExpiresAtUtc)?.toEpochMilli()
                        ?: (System.currentTimeMillis() + 15L * 60 * 1000)
                    val refreshExpiry = IsoFormat.parseInstant(dto.refreshExpiresAtUtc)?.toEpochMilli()
                        ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                    sessionManager.persist(
                        AuthTokens(access, refresh, accessExpiry, refreshExpiry),
                        user,
                    )
                    AppResult.Success(user)
                }
            }

            is AppResult.Failure -> {
                val error = result.error
                if (error.httpStatus == 401 || error.code == ApiErrorCodes.INVALID_CREDENTIALS) {
                    AppResult.Failure(
                        error.copy(message = error.message ?: "نام کاربری یا رمز عبور نادرست است.")
                    )
                } else if (error.code == ApiErrorCodes.ACCOUNT_DISABLED) {
                    AppResult.Failure(error.copy(message = "حساب کاربری غیرفعال است. با مدیر مجموعه تماس بگیرید."))
                } else {
                    result
                }
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        // Best effort server side revocation; local cleanup happens regardless.
        runCatching {
            executor.unit {
                authApi.logout(
                    LogoutRequestDto(
                        refreshToken = sessionManager.refreshToken,
                        deviceId = sessionManager.deviceId,
                    )
                )
            }
        }
        clearLocalSession()
        return AppResult.Success(Unit)
    }

    private suspend fun clearLocalSession() {
        sessionManager.clear()
        responseCache.clearAll()
        offlineQueue.clearFinished()
        sessionEventBus.emit(SessionEvent.LoggedOut)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> =
        executor.unit {
            authApi.changePassword(ChangePasswordRequestDto(currentPassword, newPassword))
        }

    override suspend fun refreshSession(): AppResult<SessionUser> {
        val user = sessionManager.currentUser
            ?: return AppResult.Failure(AppError.unauthorized())
        if (sessionManager.refreshToken.isNullOrBlank()) {
            return AppResult.Failure(AppError.unauthorized())
        }
        return withContext(Dispatchers.IO) {
            val result = executor.value { authApi.me() }
            when (result) {
                is AppResult.Success -> {
                    val refreshed = result.data.toDomain()
                    if (refreshed != null) {
                        sessionManager.updateUser(refreshed)
                        AppResult.Success(refreshed)
                    } else {
                        AppResult.Success(user)
                    }
                }

                is AppResult.Failure -> AppResult.Failure(result.error)
            }
        }
    }

    override fun observeSession(): Flow<SessionUser?> = sessionManager.session

    override suspend fun currentUser(): SessionUser? = sessionManager.currentUser

    override suspend fun isAuthenticated(): Boolean = sessionManager.isLoggedIn

    override suspend fun serverInfo(baseUrlOverride: String?): AppResult<ServerInfo> =
        withContext(Dispatchers.IO) {
            val base = when {
                baseUrlOverride.isNullOrBlank() -> serverUrlProvider.current()
                else -> serverUrlProvider.normalize(baseUrlOverride)
            }
            if (base.isNullOrBlank()) {
                return@withContext AppResult.Failure(AppError.validation("آدرس سرور تنظیم نشده است."))
            }
            val url = base.trimEnd('/') + "/api/v1/system/health"
            val request = Request.Builder().url(url).get().build()
            try {
                plainClient.newCall(request).execute().use { response ->
                    val raw = response.body?.string()
                    if (!response.isSuccessful) {
                        return@withContext AppResult.Failure(
                            if (response.code == 404) {
                                AppError.notFound("مسیر سلامت سرور یافت نشد. آدرس پایه را بررسی کنید.")
                            } else {
                                AppError.server("سرور پاسخ معتبر نداد (کد ${response.code}).", response.code)
                            }
                        )
                    }
                    val envelope = runCatching {
                        gson.fromJson(raw, HealthEnvelope::class.java)
                    }.getOrNull()
                    val dto = envelope?.data
                    if (dto == null) {
                        return@withContext AppResult.Failure(AppError.server("پاسخ سلامت سرور قابل خواندن نبود."))
                    }
                    AppResult.Success(
                        ServerInfo(
                            serverVersion = dto.serverVersion,
                            apiVersion = dto.apiVersion,
                            serverTime = IsoFormat.parseInstant(dto.serverTimeUtc),
                            databaseConnected = dto.databaseConnected,
                        )
                    )
                }
            } catch (e: IOException) {
                AppResult.Failure(executor.transportError(e))
            } catch (e: IllegalArgumentException) {
                AppResult.Failure(AppError.validation("آدرس سرور معتبر نیست."))
            }
        }

    private data class HealthEnvelope(
        @SerializedName("success") val success: Boolean? = null,
        @SerializedName("data") val data: ServerInfoDto? = null,
    )
}
