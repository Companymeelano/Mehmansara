package ir.ahvaz.mehra.feature.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.ui.components.CreatorSignature
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.icons.MehraIcons
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.SessionUser

private data class MenuEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permission: String?,
    val onClick: () -> Unit,
)

/**
 * Grouped module menu. Entries the current role cannot use are hidden; the server still
 * re-checks every permission, so this is only a UX hint.
 */
@Composable
fun MoreScreen(
    user: SessionUser?,
    onOpenGuests: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenHousekeeping: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    fun has(permission: String?): Boolean = permission == null || user?.can(permission) == true

    val frontOffice = listOfNotNull(
        MenuEntry("مهمانان", "پرونده مهمان، همراهان و سوابق", MehraIcons.Guests, Permissions.GUESTS_VIEW, onOpenGuests)
            .takeIf { has(Permissions.GUESTS_VIEW) },
        MenuEntry("مالی", "صندوق، تراکنش‌ها و بدهکاران", MehraIcons.Finance, Permissions.FINANCE_VIEW, onOpenFinance)
            .takeIf { has(Permissions.FINANCE_VIEW) },
    )

    val operations = listOfNotNull(
        MenuEntry("خدمات و نظافت", "درخواست نظافت و تأیید آماده‌سازی", MehraIcons.Housekeeping, Permissions.HOUSEKEEPING_VIEW, onOpenHousekeeping)
            .takeIf { has(Permissions.HOUSEKEEPING_VIEW) },
        MenuEntry("تعمیرات", "ثبت خرابی، ارجاع و هزینه", MehraIcons.Maintenance, Permissions.MAINTENANCE_VIEW, onOpenMaintenance)
            .takeIf { has(Permissions.MAINTENANCE_VIEW) },
    )

    val management = listOfNotNull(
        MenuEntry("گزارش‌ها", "خروجی PDF و اکسل با تاریخ شمسی", MehraIcons.Reports, Permissions.REPORTS_VIEW, onOpenReports)
            .takeIf { has(Permissions.REPORTS_VIEW) },
        MenuEntry("کاربران و نقش‌ها", "مدیریت حساب‌ها و مجوزها", MehraIcons.Users, Permissions.USERS_VIEW, onOpenUsers)
            .takeIf { has(Permissions.USERS_VIEW) },
    )

    val system = listOf(
        MenuEntry("اعلان‌ها", "رویدادها و هشدارهای سامانه", MehraIcons.Notifications, null, onOpenNotifications),
        MenuEntry("تنظیمات", "سرور، پوسته، قفل و حساب کاربری", MehraIcons.Settings, null, onOpenSettings),
    )

    MehraScreen(
        title = "ماژول‌ها",
        subtitle = user?.let { "${it.fullName} • ${it.roleTitleFa}" },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (frontOffice.isNotEmpty()) {
                SectionHeader(title = "پذیرش و مهمان")
                frontOffice.forEach { MenuTile(it) }
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (operations.isNotEmpty()) {
                SectionHeader(title = "عملیات روزانه")
                operations.forEach { MenuTile(it) }
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (management.isNotEmpty()) {
                SectionHeader(title = "مدیریت")
                management.forEach { MenuTile(it) }
                Spacer(modifier = Modifier.height(10.dp))
            }
            SectionHeader(title = "سامانه")
            system.forEach { MenuTile(it) }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "مهرآ — مهمان‌پذیر فرهنگیان اهواز",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            CreatorSignature(compact = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MenuTile(entry: MenuEntry) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = entry.onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3D(
                icon = entry.icon,
                contentDescription = null,
                size = 42.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
