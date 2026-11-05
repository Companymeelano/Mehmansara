package ir.ahvaz.mehra.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ir.ahvaz.mehra.BuildConfig
import ir.ahvaz.mehra.core.common.CurrencyUnit
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.relativeTimeFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.InfoRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.SecondaryButton
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.SwitchRow
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.ThemePreset

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    MehraScreen(title = "تنظیمات", subtitle = "پیکربندی دستگاه و حساب") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state.message != null) {
                InlineMessage(text = state.message!!, isError = false)
                Spacer(modifier = Modifier.height(8.dp))
            }

            SectionHeader(title = "اتصال به سرور")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                MehraTextField(
                    label = "نشانی سرور",
                    value = state.serverUrl,
                    onValueChange = viewModel::onServerUrlChange,
                    placeholder = "https://mehra.example.ir",
                    hint = "برای شبکه داخلی می‌توانید http://192.168.x.x:5000 وارد کنید.",
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(
                        text = "ذخیره نشانی",
                        onClick = viewModel::saveServerUrl,
                        enabled = !state.isSavingServer,
                        modifier = Modifier.weight(1f),
                    )
                    CompactButton(
                        text = if (state.isVerifyingServer) "در حال بررسی…" else "بررسی اتصال",
                        onClick = viewModel::testServer,
                        enabled = !state.isVerifyingServer,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = if (state.isOnline) "شبکه در دسترس" else "بدون شبکه",
                        tint = if (state.isOnline) MehraTheme.colors.success else MehraTheme.colors.danger,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusPill(
                        text = if (state.realtime.isConnected) "رویداد زنده متصل" else "رویداد زنده قطع",
                        tint = if (state.realtime.isConnected) MehraTheme.colors.info else MehraTheme.colors.warning,
                    )
                }
                if (state.realtime.lastEventAt != null) {
                    Text(
                        text = "آخرین رویداد: ${relativeTimeFa(state.realtime.lastEventAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.serverVersion?.let { version ->
                    Text(
                        text = "نسخه سرور: ${(version).toPersianDigits()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "ظاهر")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "پوسته سازمانی",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                ThemePreset.entries.forEach { preset ->
                    SwitchRow(
                        title = preset.titleFa,
                        description = preset.descriptionFa,
                        checked = state.settings.themeId == preset.id,
                        onCheckedChange = { checked -> if (checked) viewModel.setTheme(preset) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                DropdownField(
                    label = "واحد نمایش مبالغ",
                    options = CurrencyUnit.entries.toList(),
                    selected = state.settings.currencyUnit,
                    onSelect = viewModel::setCurrency,
                    optionLabel = { it.labelFa },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "اعلان‌ها و همگام‌سازی")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                SwitchRow(
                    title = "اعلان‌های سامانه",
                    description = "نمایش اعلان رزرو، پذیرش، پرداخت و هشدارهای مدیریتی",
                    checked = state.settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotifications,
                )
                SwitchRow(
                    title = "دریافت رویدادهای زنده",
                    description = "اتصال SignalR برای به‌روزرسانی لحظه‌ای",
                    checked = state.settings.realtimeEnabled,
                    onCheckedChange = viewModel::setRealtime,
                )
                InfoRow("عملیات در انتظار ارسال", (state.pendingOperations.toString()).toPersianDigits())
                InfoRow("آخرین همگام‌سازی", state.settings.lastSyncMillis?.let {
                    relativeTimeFa(java.time.Instant.ofEpochMilli(it))
                } ?: "—")
                if (state.pendingOperations > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactButton(
                        text = "ارسال عملیات در انتظار",
                        onClick = viewModel::retryPendingOperations,
                        icon = Icons.Rounded.CloudSync,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isOnline,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "قفل صفحه")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(
                    "وضعیت قفل",
                    if (state.isPinSet) "فعال — رمز ۶ رقمی" else "غیرفعال",
                )
                InfoRow(
                    "قفل خودکار پس از",
                    "${(state.settings.lockTimeoutMinutes.toString()).toPersianDigits()} دقیقه بی‌کاری",
                )
                DropdownField(
                    label = "مدت بی‌کاری تا قفل",
                    options = listOf(1, 2, 5, 10, 15, 30, 60),
                    selected = state.settings.lockTimeoutMinutes,
                    onSelect = viewModel::setLockTimeout,
                    optionLabel = { "${(it.toString()).toPersianDigits()} دقیقه" },
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(
                        text = if (state.isPinSet) "تغییر رمز قفل" else "تنظیم رمز قفل",
                        onClick = { showPinDialog = true },
                        icon = Icons.Rounded.Lock,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isPinSet) {
                        CompactButton(
                            text = "غیرفعال‌سازی",
                            onClick = { showClearPinDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "حساب کاربری")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow("نام", state.session?.fullName ?: "—")
                InfoRow("نام کاربری", state.session?.username?.let { "@$it" } ?: "—")
                InfoRow("نقش", state.session?.roleTitleFa ?: "—")
                Spacer(modifier = Modifier.height(8.dp))
                CompactButton(
                    text = "تغییر رمز عبور",
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "درباره سامانه")
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow("نام سامانه", "مهرآ — مهمان‌پذیر فرهنگیان اهواز")
                InfoRow("نسخه اپلیکیشن", (BuildConfig.VERSION_NAME).toPersianDigits())
                InfoRow("نسخه ساخت", (BuildConfig.VERSION_CODE.toString()).toPersianDigits())
                InfoRow("آدرس API", "api/v1")
            }

            Spacer(modifier = Modifier.height(18.dp))
            SecondaryButton(
                text = "خروج امن از حساب",
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoggingOut,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
            },
        )
    }

    if (showClearPinDialog) {
        PinDialog(
            title = "غیرفعال‌سازی قفل صفحه",
            confirmLabel = "غیرفعال کن",
            onDismiss = { showClearPinDialog = false },
            onConfirm = { pin ->
                viewModel.clearPin(pin)
                showClearPinDialog = false
            },
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new, confirm ->
                viewModel.changePassword(current, new, confirm)
                showPasswordDialog = false
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("خروج از حساب") },
            text = {
                Text(
                    "با خروج، توکن‌های دسترسی از دستگاه پاک می‌شوند و عملیات در انتظار ارسال تا ورود بعدی باقی می‌مانند."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("خروج") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun PinDialog(
    title: String = "تنظیم رمز قفل",
    confirmLabel: String = "ذخیره",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            MehraTextField(
                label = "رمز ۶ رقمی",
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                numeric = true,
                password = true,
                hint = "این رمز برای بازگشایی برنامه پس از بی‌کاری استفاده می‌شود.",
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(pin) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر رمز عبور") },
        text = {
            Column {
                MehraTextField(
                    label = "رمز فعلی",
                    value = current,
                    onValueChange = { current = it },
                    password = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(
                    label = "رمز جدید",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    password = true,
                    hint = "حداقل ۸ کاراکتر",
                )
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(
                    label = "تکرار رمز جدید",
                    value = confirm,
                    onValueChange = { confirm = it },
                    password = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current, newPassword, confirm) }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
