package ir.ahvaz.mehra.feature.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FilterChipRow
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraSearchField
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.SwitchRow
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.RoleInfo
import ir.ahvaz.mehra.domain.model.UserRole

@Composable
fun UsersScreen(viewModel: UsersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val users = state.users
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<AppUser?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var passwordTarget by remember { mutableStateOf<AppUser?>(null) }

    MehraScreen(
        title = "کاربران و نقش‌ها",
        subtitle = "دسترسی‌ها توسط سرور اعمال می‌شود",
        offline = users.fromCache || !state.isOnline,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canManage && tab == 0) {
                FloatingActionButton(onClick = {
                    editing = null
                    showForm = true
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "کاربر جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChipRow(
                    items = listOf(0, 1) + if (state.canViewAudit) listOf(2) else emptyList(),
                    selected = tab,
                    onSelect = { tab = it ?: 0 },
                    label = { when (it) { 0 -> "کاربران"; 1 -> "نقش‌ها"; else -> "گزارش رویدادها" } },
                    allLabel = null,
                )
                if (tab == 0) {
                    MehraSearchField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = "نام یا نام کاربری…",
                    )
                    FreshnessRow(
                        updatedAtMillis = users.updatedAtMillis,
                        isRefreshing = users.isRefreshing,
                        onRefresh = viewModel::refresh,
                    )
                }
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
            }

            when (tab) {
                0 -> when {
                    users.isLoading && users.data == null -> LoadingState()
                    users.error != null && users.data == null -> ErrorState(users.error!!, viewModel::refresh)
                    users.data.isNullOrEmpty() -> EmptyState(
                        title = "کاربری یافت نشد",
                        message = "با دکمه + کاربر جدید بسازید.",
                        icon = Icons.Rounded.Person,
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(users.data.orEmpty(), key = { it.id }) { user ->
                            UserRow(
                                user = user,
                                canManage = state.canManage,
                                onEdit = {
                                    editing = user
                                    showForm = true
                                },
                                onToggleActive = { viewModel.setActive(user, !user.isActive) },
                                onResetPassword = { passwordTarget = user },
                            )
                        }
                    }
                }

                1 -> RolesTab(roles = state.roles)
                else -> AuditTab(state = state, onLoad = viewModel::loadAuditLogs)
            }
        }
    }

    if (showForm) {
        UserFormDialog(
            user = editing,
            roles = state.roles,
            isSaving = state.isSaving,
            onDismiss = { showForm = false },
            onConfirm = { fullName, username, password, roleCode, phone, active ->
                viewModel.saveUser(editing?.id, fullName, username, password, roleCode, phone, active)
                showForm = false
            },
        )
    }

    passwordTarget?.let { user ->
        ResetPasswordDialog(
            user = user,
            onDismiss = { passwordTarget = null },
            onConfirm = { password ->
                viewModel.resetPassword(user, password)
                passwordTarget = null
            },
        )
    }
}

@Composable
private fun UserRow(
    user: AppUser,
    canManage: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onResetPassword: () -> Unit,
) {
    MehraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "@${user.username} • ${user.roleTitleFa}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = listOfNotNull(
                        user.phone,
                        "آخرین ورود: ${user.lastLoginAt.formatJalaliDateTimeFa()}",
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                text = if (user.isActive) "فعال" else "غیرفعال",
                tint = if (user.isActive) MehraTheme.colors.success else MehraTheme.colors.danger,
            )
        }
        if (canManage) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(text = "ویرایش", onClick = onEdit, modifier = Modifier.weight(1f))
                CompactButton(text = "تغییر رمز", onClick = onResetPassword, modifier = Modifier.weight(1f))
                CompactButton(
                    text = if (user.isActive) "غیرفعال‌سازی" else "فعال‌سازی",
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RolesTab(roles: List<RoleInfo>) {
    if (roles.isEmpty()) {
        EmptyState(
            title = "نقشی دریافت نشد",
            message = "فهرست نقش‌ها از سرور خوانده می‌شود؛ در صورت خطا نقش‌های پیش‌فرض نمایش داده می‌شوند.",
        )
    }
    val fallback = UserRole.entries.map { role ->
        RoleInfo(code = role.wire, titleFa = role.labelFa, permissions = role.defaultPermissions)
    }
    val items = if (roles.isNotEmpty()) roles else fallback
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.code }) { role ->
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = role.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(role.userCount.toString()).toPersianDigits()} کاربر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${(role.permissions.size.toString()).toPersianDigits()} مجوز",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AuditTab(state: UsersUiState, onLoad: (JalaliDate, JalaliDate) -> Unit) {
    var from by remember { mutableStateOf(JalaliDate.today().let { JalaliDate(it.year, it.month, 1) }) }
    var to by remember { mutableStateOf(JalaliDate.today()) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ir.ahvaz.mehra.core.ui.components.JalaliDateField(
                label = "از تاریخ",
                value = from,
                onDateSelected = { from = it },
                modifier = Modifier.weight(1f),
            )
            ir.ahvaz.mehra.core.ui.components.JalaliDateField(
                label = "تا تاریخ",
                value = to,
                onDateSelected = { to = it },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryButton(
            text = "دریافت رویدادها",
            onClick = { onLoad(from, to) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isLoadingAudit,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.auditLogs.isEmpty()) {
            Text(
                text = "رویدادی برای این بازه نمایش داده نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.auditLogs.forEach { entry ->
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Text(entry.action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = listOfNotNull(entry.actorName, entry.entity, entry.description).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.occurredAt.formatJalaliDateTimeFa(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun UserFormDialog(
    user: AppUser?,
    roles: List<RoleInfo>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, String?, Boolean) -> Unit,
) {
    var fullName by remember(user) { mutableStateOf(user?.fullName.orEmpty()) }
    var username by remember(user) { mutableStateOf(user?.username.orEmpty()) }
    var password by remember(user) { mutableStateOf("") }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var roleCode by remember(user) {
        mutableStateOf(user?.roleCode ?: roles.firstOrNull()?.code ?: UserRole.RECEPTIONIST.wire)
    }
    var active by remember(user) { mutableStateOf(user?.isActive ?: true) }

    val roleOptions = if (roles.isNotEmpty()) roles else UserRole.entries.map {
        RoleInfo(code = it.wire, titleFa = it.labelFa)
    }
    val selectedRole = roleOptions.firstOrNull { it.code == roleCode }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "کاربر جدید" else "ویرایش کاربر") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                MehraTextField(label = "نام و نام خانوادگی", value = fullName, onValueChange = { fullName = it })
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(label = "نام کاربری", value = username, onValueChange = { username = it })
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(
                    label = if (user == null) "رمز عبور" else "رمز جدید (اختیاری)",
                    value = password,
                    onValueChange = { password = it },
                    password = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownField(
                    label = "نقش",
                    options = roleOptions,
                    selected = selectedRole,
                    onSelect = { roleCode = it.code },
                    optionLabel = { it.titleFa },
                )
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(label = "تلفن", value = phone, onValueChange = { phone = it }, numeric = true)
                Spacer(modifier = Modifier.height(8.dp))
                SwitchRow(title = "حساب فعال", checked = active, onCheckedChange = { active = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onConfirm(fullName, username, password.takeIf { it.isNotBlank() }, roleCode, phone, active) },
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun ResetPasswordDialog(user: AppUser, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر رمز ${user.fullName}") },
        text = {
            MehraTextField(
                label = "رمز عبور جدید",
                value = password,
                onValueChange = { password = it },
                password = true,
                hint = "حداقل ۸ کاراکتر",
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(password) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
