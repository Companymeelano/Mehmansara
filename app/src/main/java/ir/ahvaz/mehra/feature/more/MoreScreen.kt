package ir.ahvaz.mehra.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.R
import ir.ahvaz.mehra.core.ui.components.CreatorSignature
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.SessionUser

private data class MenuEntry(
    val title: String,
    val description: String,
    @androidx.annotation.DrawableRes val iconRes: Int,
    val permission: String?,
    val onClick: () -> Unit,
)

/**
 * Simple, flat module menu for all roles.
 *
 * Design rules for novice users:
 *  - one flat grid of big tiles, no nested groups and no jargon;
 *  - plain one/two-word labels and a short description;
 *  - entries the current role cannot use are hidden (server still re-checks every permission);
 *  - a quick-guide card on top points beginners to the bottom bar for daily work.
 */
@Composable
fun MoreScreen(
    user: SessionUser?,
    onOpenDbTool: () -> Unit,
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

    val entries = listOfNotNull(
        MenuEntry("مهمانان", "پروندهٔ مهمانان", R.drawable.ic_mehra_guests, Permissions.GUESTS_VIEW, onOpenGuests)
            .takeIf { has(Permissions.GUESTS_VIEW) },
        MenuEntry("مالی", "درآمد و هزینه‌ها", R.drawable.ic_mehra_finance, Permissions.FINANCE_VIEW, onOpenFinance)
            .takeIf { has(Permissions.FINANCE_VIEW) },
        MenuEntry("نظافت", "نظافت و آماده‌سازی اتاق", R.drawable.ic_mehra_housekeeping, Permissions.HOUSEKEEPING_VIEW, onOpenHousekeeping)
            .takeIf { has(Permissions.HOUSEKEEPING_VIEW) },
        MenuEntry("تعمیرات", "ثبت خرابی و تعمیر", R.drawable.ic_mehra_maintenance, Permissions.MAINTENANCE_VIEW, onOpenMaintenance)
            .takeIf { has(Permissions.MAINTENANCE_VIEW) },
        MenuEntry("گزارش‌ها", "خروجی PDF و اکسل", R.drawable.ic_mehra_reports, Permissions.REPORTS_VIEW, onOpenReports)
            .takeIf { has(Permissions.REPORTS_VIEW) },
        MenuEntry("کاربران", "مدیریت کاربران", R.drawable.ic_mehra_users, Permissions.USERS_VIEW, onOpenUsers)
            .takeIf { has(Permissions.USERS_VIEW) },
        MenuEntry("دیتابیس (تست مستقیم)", "اتصال مستقیم به SQL Server از طریق WiFi — پورت 1433، بدون ورود", R.drawable.ic_mehra_reports, null, onOpenDbTool),
        MenuEntry("اعلان‌ها", "پیام‌ها و هشدارها", R.drawable.ic_mehra_notifications, null, onOpenNotifications),
        MenuEntry("تنظیمات", "تنظیمات برنامه", R.drawable.ic_mehra_settings, null, onOpenSettings),
    )

    MehraScreen(
        title = "بخش‌های برنامه",
        subtitle = user?.let { "${it.fullName} • ${it.roleTitleFa}" },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = MehraTheme.colors.accentMid,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "راهنمای سریع",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "کارهای روزمره از نوار پایین انجام می‌شود: خانه، اتاق‌ها، مهمانان، رزروها و پذیرش. بقیهٔ بخش‌ها همین‌جا هستند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            entries.chunked(2).forEach { rowEntries ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowEntries.forEach { entry ->
                        MenuTile(entry = entry, modifier = Modifier.weight(1f))
                    }
                    if (rowEntries.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "مهرآ — مهمان‌پذیر فرهنگیان اهواز",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            CreatorSignature(compact = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** One big, plain-language tile in the flat grid. */
@Composable
private fun MenuTile(entry: MenuEntry, modifier: Modifier = Modifier) {
    MehraCard(modifier = modifier, onClick = entry.onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val colors = MehraTheme.colors
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colors.accentMid.copy(alpha = 0.38f),
                                colors.accentEnd.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            ) {
                Icon3D(
                    icon = ImageVector.vectorResource(entry.iconRes),
                    contentDescription = null,
                    size = 48.dp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
