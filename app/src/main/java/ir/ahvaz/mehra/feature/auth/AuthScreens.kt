package ir.ahvaz.mehra.feature.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.ui.components.CreatorSignature
import ir.ahvaz.mehra.core.ui.components.GradientText
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.SecondaryButton
import ir.ahvaz.mehra.core.ui.icons.MehraIcons
import ir.ahvaz.mehra.core.ui.theme.LocalAccentBrush

/**
 * First run: the operator points the app at the hotel server (LAN address or the verified
 * VPN endpoint) and verifies reachability before anything else happens.
 */
@Composable
fun ServerSetupScreen(
    onSaved: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MehraScreen(title = "اتصال به سرور", subtitle = "پیکربندی آدرس سرور MEHRA") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon3D(icon = MehraIcons.Logo, contentDescription = null, size = 72.dp)
                Spacer(modifier = Modifier.height(10.dp))
                GradientText(
                    text = "مهرآ",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    brush = LocalAccentBrush.current,
                )
            }
            Text(
                text = "مهمان‌پذیر فرهنگیان اهواز",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(28.dp))

            MehraTextField(
                label = "آدرس سرور",
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                placeholder = "https://mehra.example.ir/",
                hint = "در شبکه داخلی می‌توانید آدرس سرور محلی را وارد کنید. نسخه عملیاتی فقط HTTPS را می‌پذیرد.",
                error = state.errorMessage,
                singleLine = true,
                imeAction = ImeAction.Done,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = if (state.probe is ConnectionProbe.Testing) "در حال بررسی…" else "بررسی اتصال",
                    onClick = viewModel::testConnection,
                    modifier = Modifier.weight(1f),
                    loading = state.probe is ConnectionProbe.Testing,
                )
                PrimaryButton(
                    text = "ذخیره و ادامه",
                    onClick = { viewModel.saveServerUrl(onSaved) },
                    modifier = Modifier.weight(1f),
                    enabled = state.serverUrl.isNotBlank(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            when (val probe = state.probe) {
                is ConnectionProbe.Success -> {
                    Column {
                        InlineMessage(text = "اتصال برقرار شد.", isError = false)
                        Text(
                            text = "نسخه سرور: ${probe.info.serverVersion ?: "نامشخص"} — نسخه API: ${probe.info.apiVersion ?: "نامشخص"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (probe.info.databaseConnected == false) {
                            InlineMessage(text = "پایگاه‌داده سرور در دسترس نیست.")
                        }
                    }
                }

                is ConnectionProbe.Failure -> InlineMessage(text = probe.message)

                else -> Unit
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "نکته امنیتی: آدرس سرور و توکن‌های نشست به‌صورت رمزنگاری‌شده روی دستگاه ذخیره می‌شوند و اتصال مستقیم به پایگاه‌داده در هیچ شرایطی انجام نمی‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onOpenServerSettings: () -> Unit,
    sessionExpired: Boolean,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MehraScreen(
        title = "ورود به مهرآ",
        subtitle = state.savedServerUrl,
        actions = {
            IconButton(onClick = onOpenServerSettings) {
                Icon(Icons.Rounded.Badge, contentDescription = "تنظیمات سرور")
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (sessionExpired) {
                InlineMessage(text = "نشست شما منقضی شده است. لطفاً دوباره وارد شوید.")
                Spacer(modifier = Modifier.height(12.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon3D(icon = MehraIcons.Logo, contentDescription = null, size = 64.dp)
                Spacer(modifier = Modifier.height(8.dp))
                GradientText(
                    text = "مهرآ",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    brush = LocalAccentBrush.current,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "سامانه مدیریت مهمان‌پذیر فرهنگیان اهواز",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(24.dp))

            MehraTextField(
                label = "نام کاربری",
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                error = state.fieldErrors["username"],
                leadingIcon = Icons.Rounded.Person,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MehraTextField(
                label = "رمز عبور",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                error = state.fieldErrors["password"],
                password = true,
                leadingIcon = Icons.Rounded.Lock,
                imeAction = ImeAction.Done,
            )

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "ورود",
                onClick = { viewModel.login(onLoggedIn) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.isLoading,
                enabled = state.username.isNotBlank() && state.password.isNotBlank(),
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                InlineMessage(text = state.errorMessage!!)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "دسترسی‌ها در سرور کنترل می‌شود؛ نقش شما تعیین‌کننده امکانات قابل استفاده است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            CreatorSignature(compact = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
