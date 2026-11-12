package ir.ahvaz.mehra.core.security

import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.model.UserRole

/**
 * Since 1.4.0 the app is fully local: there is no server and no login screen. A local
 * operator session with full permissions is created silently on first launch so every
 * screen (audit actor, permission checks) works exactly as before.
 */
object LocalOperator {
    fun session(): SessionUser = SessionUser(
        id = "local-operator",
        fullName = "اپراتور دستگاه",
        username = "operator",
        roleCode = UserRole.SUPER_ADMIN.wire,
        roleTitleFa = UserRole.SUPER_ADMIN.labelFa,
        permissions = Permissions.all,
    )
}
