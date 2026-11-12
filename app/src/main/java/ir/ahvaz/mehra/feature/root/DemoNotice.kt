package ir.ahvaz.mehra.feature.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.BuildConfig

/**
 * One-time welcome sheet for the showcase build (`demo` flavour).
 *
 * In the production flavour `BuildConfig.DEMO_MODE` is `false`, so this composable emits
 * nothing and the app behaves exactly as before.
 */
@Composable
fun DemoIntroDialog() {
    if (!BuildConfig.DEMO_MODE) return
    var visible by remember { mutableStateOf(true) }
    if (!visible) return

    AlertDialog(
        onDismissRequest = { visible = false },
        title = { Text("نسخه نمایشی مهرآ") },
        text = {
            Column {
                Text(
                    "این نسخه برای معرفی امکانات برنامه به مشتری تهیه شده است. " +
                        "هیچ دادهٔ پیش‌فرضی در آن وجود ندارد و هیچ ارتباطی با سرور واقعی برقرار نمی‌شود؛ " +
                        "همهٔ اطلاعات (اتاق‌ها، مهمان‌ها، رزروها، صندوق و گزارش‌ها) را خودتان به‌صورت دستی وارد می‌کنید."
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "می‌توانید آزادانه اتاق و مهمان تعریف کنید، رزرو ثبت کنید، پذیرش و تسویه انجام دهید، " +
                        "وضعیت اتاق‌ها را تغییر دهید و گزارش‌ها را خروجی بگیرید. با بستن و باز کردن مجدد برنامه، " +
                        "همهٔ اطلاعات واردشده پاک می‌شود."
                )
                Spacer(Modifier.height(8.dp))
                Text("ورود: نام کاربری و رمز عبور دلخواه (غیرخالی) را به‌صورت دستی وارد کنید.")
            }
        },
        confirmButton = {
            TextButton(onClick = { visible = false }) { Text("شروع نمایش") }
        },
    )
}
