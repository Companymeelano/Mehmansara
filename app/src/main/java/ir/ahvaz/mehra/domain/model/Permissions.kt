package ir.ahvaz.mehra.domain.model

/**
 * Permission codes enforced by the backend. Android only uses them to hide impossible
 * actions — every request is re-authorised server side, so hiding a button is never
 * treated as a security control.
 */
object Permissions {
    const val DASHBOARD_VIEW = "dashboard.view"
    const val ROOMS_VIEW = "rooms.view"
    const val ROOMS_MANAGE = "rooms.manage"
    const val GUESTS_VIEW = "guests.view"
    const val GUESTS_MANAGE = "guests.manage"
    const val GUESTS_PII_VIEW = "guests.pii.view"
    const val RESERVATIONS_VIEW = "reservations.view"
    const val RESERVATIONS_MANAGE = "reservations.manage"
    const val RECEPTION_OPERATE = "reception.operate"
    const val FINANCE_VIEW = "finance.view"
    const val FINANCE_OPERATE = "finance.operate"
    const val HOUSEKEEPING_VIEW = "housekeeping.view"
    const val HOUSEKEEPING_MANAGE = "housekeeping.manage"
    const val MAINTENANCE_VIEW = "maintenance.view"
    const val MAINTENANCE_MANAGE = "maintenance.manage"
    const val USERS_VIEW = "users.view"
    const val USERS_MANAGE = "users.manage"
    const val REPORTS_VIEW = "reports.view"
    const val REPORTS_EXPORT = "reports.export"
    const val AUDIT_VIEW = "audit.view"

    val all: Set<String> = setOf(
        DASHBOARD_VIEW, ROOMS_VIEW, ROOMS_MANAGE, GUESTS_VIEW, GUESTS_MANAGE, GUESTS_PII_VIEW,
        RESERVATIONS_VIEW, RESERVATIONS_MANAGE, RECEPTION_OPERATE, FINANCE_VIEW, FINANCE_OPERATE,
        HOUSEKEEPING_VIEW, HOUSEKEEPING_MANAGE, MAINTENANCE_VIEW, MAINTENANCE_MANAGE,
        USERS_VIEW, USERS_MANAGE, REPORTS_VIEW, REPORTS_EXPORT, AUDIT_VIEW,
    )
}

/** The nine operational roles of the hotel. */
enum class UserRole(val wire: String, val labelFa: String, val defaultPermissions: Set<String>) {
    SUPER_ADMIN(
        "SuperAdmin", "مدیر کل", Permissions.all,
    ),
    MANAGER(
        "Manager", "مدیر مجموعه",
        Permissions.all - setOf(Permissions.AUDIT_VIEW),
    ),
    FRONT_OFFICE_SUPERVISOR(
        "FrontOfficeSupervisor", "سرپرست پذیرش",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.ROOMS_VIEW, Permissions.ROOMS_MANAGE,
            Permissions.GUESTS_VIEW, Permissions.GUESTS_MANAGE, Permissions.GUESTS_PII_VIEW,
            Permissions.RESERVATIONS_VIEW, Permissions.RESERVATIONS_MANAGE,
            Permissions.RECEPTION_OPERATE, Permissions.FINANCE_VIEW,
            Permissions.HOUSEKEEPING_VIEW, Permissions.HOUSEKEEPING_MANAGE,
            Permissions.MAINTENANCE_VIEW, Permissions.MAINTENANCE_MANAGE,
            Permissions.REPORTS_VIEW, Permissions.REPORTS_EXPORT,
        ),
    ),
    RECEPTIONIST(
        "Receptionist", "پذیرشگر",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.ROOMS_VIEW,
            Permissions.GUESTS_VIEW, Permissions.GUESTS_MANAGE, Permissions.GUESTS_PII_VIEW,
            Permissions.RESERVATIONS_VIEW, Permissions.RESERVATIONS_MANAGE,
            Permissions.RECEPTION_OPERATE, Permissions.FINANCE_VIEW,
            Permissions.HOUSEKEEPING_VIEW, Permissions.MAINTENANCE_VIEW, Permissions.MAINTENANCE_MANAGE,
        ),
    ),
    ACCOUNTANT(
        "Accountant", "مالی",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.GUESTS_VIEW,
            Permissions.RESERVATIONS_VIEW, Permissions.FINANCE_VIEW, Permissions.FINANCE_OPERATE,
            Permissions.REPORTS_VIEW, Permissions.REPORTS_EXPORT,
        ),
    ),
    HOUSEKEEPING(
        "Housekeeping", "خدمات",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.ROOMS_VIEW,
            Permissions.HOUSEKEEPING_VIEW, Permissions.HOUSEKEEPING_MANAGE,
            Permissions.MAINTENANCE_VIEW, Permissions.MAINTENANCE_MANAGE,
        ),
    ),
    TECHNICIAN(
        "Maintenance", "تعمیرات",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.ROOMS_VIEW,
            Permissions.MAINTENANCE_VIEW, Permissions.MAINTENANCE_MANAGE,
        ),
    ),
    SUPERVISOR(
        "Supervisor", "ناظر",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.ROOMS_VIEW, Permissions.GUESTS_VIEW,
            Permissions.RESERVATIONS_VIEW, Permissions.HOUSEKEEPING_VIEW,
            Permissions.MAINTENANCE_VIEW, Permissions.REPORTS_VIEW,
        ),
    ),
    AUDITOR(
        "Auditor", "حسابرس",
        setOf(
            Permissions.DASHBOARD_VIEW, Permissions.FINANCE_VIEW, Permissions.REPORTS_VIEW,
            Permissions.REPORTS_EXPORT, Permissions.AUDIT_VIEW, Permissions.RESERVATIONS_VIEW,
        ),
    ),
    ;

    companion object {
        fun fromWire(value: String?): UserRole? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}
