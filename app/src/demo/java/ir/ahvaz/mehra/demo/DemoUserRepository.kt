package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.CreateUserCommand
import ir.ahvaz.mehra.domain.model.RoleInfo
import ir.ahvaz.mehra.domain.model.UserRole
import ir.ahvaz.mehra.domain.repository.UserRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoUserRepository @Inject constructor(
    private val store: DemoStore,
) : UserRepository {

    override suspend fun users(query: String): AppResult<CachedData<List<AppUser>>> {
        delay(140)
        val q = query.trim()
        val list = store.users
            .filter { q.isEmpty() || it.fullName.contains(q) || it.username.contains(q, ignoreCase = true) }
            .sortedBy { it.fullName }
        return demoCached(list)
    }

    override suspend fun roles(): AppResult<List<RoleInfo>> {
        delay(120)
        val list = UserRole.entries.map { role ->
            RoleInfo(
                code = role.wire,
                titleFa = role.labelFa,
                permissions = role.defaultPermissions,
                userCount = store.users.count { it.roleCode == role.wire },
            )
        }
        return demoOk(list)
    }

    override suspend fun save(command: CreateUserCommand): AppResult<AppUser> {
        delay(240)
        if (command.fullName.isBlank()) return demoFailure("نام و نام خانوادگی را وارد کنید.")
        if (command.username.isBlank()) return demoFailure("نام کاربری را وارد کنید.")
        val role = UserRole.fromWire(command.roleCode) ?: return demoFailure("نقش انتخاب شده معتبر نیست.")
        val existing = command.id?.let { userId -> store.users.firstOrNull { it.id == userId } }
        val duplicate = store.users.any {
            it.username.equals(command.username.trim(), ignoreCase = true) && it.id != existing?.id
        }
        if (duplicate) return demoFailure("این نام کاربری قبلاً ثبت شده است.")

        val user = AppUser(
            id = existing?.id ?: store.nextId("user"),
            fullName = command.fullName.trim(),
            username = command.username.trim(),
            roleCode = role.wire,
            roleTitleFa = role.labelFa,
            phone = command.phone?.demoDigits()?.ifBlank { null },
            isActive = command.isActive,
            lastLoginAt = existing?.lastLoginAt,
        )
        val index = store.users.indexOfFirst { it.id == user.id }
        if (index >= 0) store.users[index] = user else store.users += user
        store.auditLogs.add(
            0,
            AuditLogEntry(
                id = store.nextId("au"),
                action = if (existing != null) "ویرایش کاربر" else "ایجاد کاربر",
                entity = "User",
                entityId = user.id,
                description = "${user.fullName} — ${user.roleTitleFa}",
                actorName = DemoStore.DEMO_OPERATOR,
                occurredAt = Instant.now(),
            ),
        )
        return demoOk(user)
    }

    override suspend fun setActive(userId: String, active: Boolean): AppResult<Unit> {
        delay(180)
        val index = store.users.indexOfFirst { it.id == userId }
        if (index < 0) return demoNotFound("کاربر مورد نظر یافت نشد.")
        val user = store.users[index]
        store.users[index] = user.copy(isActive = active)
        store.auditLogs.add(
            0,
            AuditLogEntry(
                id = store.nextId("au"),
                action = "ویرایش کاربر",
                entity = "User",
                entityId = user.id,
                description = if (active) {
                    "حساب کاربری ${user.fullName} فعال شد"
                } else {
                    "حساب کاربری ${user.fullName} غیرفعال شد"
                },
                actorName = DemoStore.DEMO_OPERATOR,
                occurredAt = Instant.now(),
            ),
        )
        return AppResult.Success(Unit)
    }

    override suspend fun resetPassword(userId: String, newPassword: String): AppResult<Unit> {
        delay(200)
        if (newPassword.length < 6) return demoFailure("رمز عبور باید حداقل ۶ کاراکتر باشد.")
        if (store.users.none { it.id == userId }) return demoNotFound("کاربر مورد نظر یافت نشد.")
        return AppResult.Success(Unit)
    }

    override suspend fun auditLogs(from: LocalDate, to: LocalDate): AppResult<List<AuditLogEntry>> {
        delay(160)
        val list = store.auditLogs
            .filter { log ->
                val date = store.localDateOf(log.occurredAt) ?: return@filter false
                !date.isBefore(from) && !date.isAfter(to)
            }
            .sortedByDescending { it.occurredAt }
        return demoOk(list)
    }
}
