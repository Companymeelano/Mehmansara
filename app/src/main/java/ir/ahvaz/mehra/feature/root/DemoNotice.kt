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
                        "همه اطلاعات (اتاق‌ها، مهمان‌ها، رزروها، صندوق و گزارش‌ها) نمونه هستند " +
                        "و هیچ ارتباطی با سرور واقعی برقرار نمی‌شود."
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "می‌توانید آزادانه رزرو ثبت کنید، پذیرش و تسویه انجام دهید، وضعیت اتاق‌ها را " +
                        "تغییر دهید و گزارش‌ها را خروجی بگیرید. با بستن و باز کردن مجدد برنامه، " +
                        "اطلاعات نمایشی به حالت اولیه برمی‌گردد."
                )
                Spacer(Modifier.height(8.dp))
                Text("ورود: هر نام کاربری و رمزی پذیرفته می‌شود (نمونه: admin / 123456).")
            }
        },
        confirmButton = {
            TextButton(onClick = { visible = false }) { Text("شروع نمایش") }
        },
    )
}
