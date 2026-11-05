package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.Validators
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.UsersApi
import ir.ahvaz.mehra.data.remote.dto.AppUserDto
import ir.ahvaz.mehra.data.remote.dto.AuditLogDto
import ir.ahvaz.mehra.data.remote.dto.CreateUserRequestDto
import ir.ahvaz.mehra.data.remote.dto.ResetPasswordRequestDto
import ir.ahvaz.mehra.data.remote.dto.RoleDto
import ir.ahvaz.mehra.data.remote.dto.ToggleActiveRequestDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.CreateUserCommand
import ir.ahvaz.mehra.domain.model.RoleInfo
import ir.ahvaz.mehra.domain.repository.UserRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UsersApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), UserRepository {

    private val userListType = object : TypeToken<List<AppUserDto>>() {}.type
    private val roleListType = object : TypeToken<List<RoleDto>>() {}.type
    private val auditListType = object : TypeToken<List<AuditLogDto>>() {}.type

    override suspend fun users(query: String): AppResult<CachedData<List<AppUser>>> =
        readThrough("users:${query.trim()}", userListType) {
            executor.value { api.list(query.trim().takeIf { it.isNotEmpty() }) }.map { it.items.orEmpty() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }

    override suspend fun roles(): AppResult<List<RoleInfo>> {
        val result = readThrough("users:roles", roleListType) {
            executor.value { api.roles() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun save(command: CreateUserCommand): AppResult<AppUser> {
        val validation = validate(command)
        if (validation != null) return AppResult.Failure(validation)
        val dto = CreateUserRequestDto(
            fullName = command.fullName.trim(),
            username = command.username.trim(),
            password = command.password?.takeIf { it.isNotBlank() },
            roleCode = command.roleCode,
            phone = command.phone?.takeIf { it.isNotBlank() },
            isActive = command.isActive,
        )
        return if (command.id == null) {
            executor.value { api.create(UUID.randomUUID().toString(), dto) }.map { it.toDomain() }
        } else {
            executor.value { api.update(command.id, dto) }.map { it.toDomain() }
        }
    }

    override suspend fun setActive(userId: String, active: Boolean): AppResult<Unit> =
        executor.unit { api.setActive(userId, ToggleActiveRequestDto(active)) }

    override suspend fun resetPassword(userId: String, newPassword: String): AppResult<Unit> {
        val error = Validators.passwordError(newPassword)
        if (error != null) return AppResult.Failure(AppError.validation(error))
        return executor.unit { api.resetPassword(userId, ResetPasswordRequestDto(newPassword)) }
    }

    override suspend fun auditLogs(from: LocalDate, to: LocalDate): AppResult<List<AuditLogEntry>> {
        val result = readThrough(
            "users:audit:${IsoFormat.date(from)}:${IsoFormat.date(to)}",
            auditListType,
        ) {
            executor.value { api.auditLogs(IsoFormat.date(from), IsoFormat.date(to)) }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    private fun validate(command: CreateUserCommand): AppError? {
        val fields = mutableMapOf<String, String>()
        if (command.fullName.isBlank()) fields["fullName"] = "نام و نام خانوادگی الزامی است."
        if (!Validators.isValidUsername(command.username)) {
            fields["username"] = "نام کاربری باید ۳ تا ۳۲ کاراکتر و شامل حروف لاتین، عدد، نقطه یا خط تیره باشد."
        }
        if (command.roleCode.isBlank()) fields["roleCode"] = "نقش کاربر را انتخاب کنید."
        if (command.id == null) {
            val passwordError = Validators.passwordError(command.password)
            if (passwordError != null) fields["password"] = passwordError
        }
        if (!command.phone.isNullOrBlank() && !Validators.isValidIranMobile(command.phone)) {
            fields["phone"] = "شماره تماس معتبر نیست."
        }
        return if (fields.isEmpty()) null else AppError.validation("اطلاعات کاربر کامل نیست.", fields)
    }
}
