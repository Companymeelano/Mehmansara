package ir.ahvaz.mehra.core.common

import java.time.LocalDate

/**
 * Rotating hotel-management tips (v1.9.0) shown on the dashboard hero — the selection is
 * deterministic per day so the tip is stable all day and testable.
 */
object DailyTips {

    val tips: List<String> = listOf(
        "پلاک خودروی مهمان را حتماً ثبت کنید؛ جست‌وجوی سریع با پلاک در پالت سراسری فعال است.",
        "برای هر رزرو، کد ملی و شمارهٔ تماس مهمان را کامل وارد کنید تا پیگیری آسان شود.",
        "پس از خروج، وضعیت اتاق را فوراً به «در حال نظافت» تغییر دهید تا تابلوی اتاق‌ها به‌روز بماند.",
        "از پالت جست‌وجوی سراسری (آیکون ذره‌بین در نوار پایین) می‌توانید با صدا هم جست‌وجو کنید.",
        "گزارش‌های PDF با هدر برندی از بخش گزارش‌ها دریافت می‌شوند؛ برای lưu یا ارسال آماده‌اند.",
        "ماندهٔ حساب مهمان را از پروندهٔ او ببینید؛ مقدار مثبت یعنی بدهی و مقدار منفی یعنی بستانکاری.",
        "اگر PIN قفل صفحه را تنظیم کرده‌اید، آن را در جایی امن نگه دارید.",
        "داده‌های اپ روی دستگاه و به‌صورت رمزنگاری‌شده ذخیره می‌شود و با بستن اپ پاک نمی‌شود.",
    )

    /** The tip for [date] — stable for the whole day. */
    fun tipFor(date: LocalDate): String {
        val index = ((date.dayOfYear % tips.size) + tips.size) % tips.size
        return tips[index]
    }
}
