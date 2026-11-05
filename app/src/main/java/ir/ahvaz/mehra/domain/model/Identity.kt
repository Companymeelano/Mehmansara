package ir.ahvaz.mehra.domain.model

import java.time.Instant

data class SessionUser(
    val id: String,
    val fullName: String,
    val username: String,
    val roleCode: String,
    val roleTitleFa: String,
    val permissions: Set<String>,
) {
    val role: UserRole? get() = UserRole.fromWire(roleCode)

    /** Server granted permissions; falls back to the role defaults only when the server sends none. */
    fun can(permission: String): Boolean =
        if (permissions.isNotEmpty()) permissions.contains(permission)
        else role?.defaultPermissions?.contains(permission) ?: false

    fun canAny(vararg permission: String): Boolean = permission.any { can(it) }
}

data class AppUser(
    val id: String,
    val fullName: String,
    val username: String,
    val roleCode: String,
    val roleTitleFa: String,
    val phone: String? = null,
    val isActive: Boolean = true,
    val lastLoginAt: Instant? = null,
)

data class CreateUserCommand(
    val id: String? = null,
    val fullName: String,
    val username: String,
    val password: String? = null,
    val roleCode: String,
    val phone: String? = null,
    val isActive: Boolean = true,
)

data class RoleInfo(
    val code: String,
    val titleFa: String,
    val permissions: Set<String> = emptySet(),
    val userCount: Int = 0,
)

data class AuditLogEntry(
    val id: String,
    val action: String,
    val entity: String? = null,
    val entityId: String? = null,
    val description: String? = null,
    val actorName: String? = null,
    val occurredAt: Instant? = null,
)
