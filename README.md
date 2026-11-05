# MEHRA — مهرآ

اپلیکیشن مدیریت مهمان‌پذیر فرهنگیان اهواز (Android) — Persian-first, RTL, offline-tolerant.

> **وضعیت تحویل — مهم**
> این مخزن شامل کد کامل اپلیکیشن اندروید، منابع، مستندات و قرارداد API است، اما **هیچ بیلدی روی این مخزن اجرا نشده است**:
> در محیطی که کد نوشته شد نه JDK، نه Android SDK و نه دسترسی به مخازن Maven (Google Maven / Maven Central) وجود داشت.
> در نتیجه هیچ APK دیباگ/ریلیز تولید نشده و هیچ تست اجرایی (unit/instrumented/emulator) اجرا نشده است.
> جزئیات دقیق در [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

---

## 1. معرفی

سامانه «مهرآ» برای مدیریت شبانه‌روزی مهمان‌پذیر فرهنگیان اهواز طراحی شده است:

- داشبورد مدیریتی (وضعیت اتاق‌ها، ورود/خروج امروز، درآمد امروز، نمودار اشغال، هشدارها، فعالیت اخیر)
- مدیریت اتاق‌ها (CRUD، طبقه/ظرفیت/نوع/تعرفه/امکانات/وضعیت، تاریخچه اشغال، درخواست نظافت و تعمیر، فیلتر و جست‌وجو)
- مدیریت مهمانان (پرونده کامل، همراهان، سوابق اقامت، رزروها، بدهی/پرداخت، دسترسی کنترل‌شده به اطلاعات هویتی)
- رزروها (ایجاد، انتخاب مهمان/اتاق، برآورد هزینه، تخفیف، پیش‌پرداخت، مانده، تأیید/لغو، لیست انتظار، جلوگیری از رزرو تکراری)
- پذیرش (جست‌وجوی رزرو، پذیرش، همراهان، تخصیص اتاق، مدارک، پرداخت اولیه، رسید، تمدید، جابه‌جایی اتاق، تسویه و خروج، انتقال به نظافت)
- مالی (صندوق، دریافت/پرداخت، پیش‌پرداخت، تسویه، بدهکاران، تخفیف، گزارش جریان نقدی و روزانه، تفکیک روش پرداخت، تاریخچه با شناسه یکتا و کاربر ثبت‌کننده)
- خدمات (فهرست نیازمند نظافت، تخصیص، شروع/پایان، تأیید آماده‌سازی، گزارش عملکرد)
- تعمیرات (ثبت خرابی، شرح، تصاویر، اولویت، کارشناس، وضعیت، هزینه، آزادسازی اتاق)
- کاربران و نقش‌ها (۹ نقش) با اعتبارسنجی سمت سرور
- گزارش‌ها (اشغال، ورود/خروج, رزرو، درآمد، بدهکاران، عملکرد کارکنان، خدمات، تعمیرات) با فیلتر تاریخ شمسی و خروجی PDF/Excel و اشتراک‌گذاری
- تنظیمات (آدرس سرور، وضعیت اتصال، پوسته، واحد پول، اعلان‌ها، مدیریت نشست، تغییر رمز، نسخه، خروج امن)

## 2. پشته فنی

| لایه | انتخاب |
|---|---|
| زبان | Kotlin 2.1.10 |
| UI | Jetpack Compose + Material 3 (BOM 2024.12.01) |
| معماری | Clean Architecture (core / domain / data / feature / navigation) + MVVM |
| DI | Hilt 2.53.1 (+ Hilt Work) |
| شبکه | Retrofit 2.11.0 + OkHttp 4.12.0 + Gson |
| همزمانی | Coroutines / Flow |
| ذخیره‌سازی | Room 2.6.1 (cache + outbox)، DataStore Preferences 1.1.1 (تنظیمات)، EncryptedSharedPreferences (توکن) |
| کارهای پس‌زمینه | WorkManager 2.10.0 (`SyncWorker` + `OutboxProcessor`) |
| Realtime | SignalR (`/hubs/notifications`) با پیاده‌سازی سبک روی OkHttp WebSocket |
| Gradle | Kotlin DSL، AGP 8.7.3، Gradle 8.11.1، JDK 17 |
| SDK | minSdk 24، compileSdk/targetSdk 35 |

نسخه‌ها در `gradle/libs.versions.toml` متمرکز شده‌اند و از مجموعه‌ای گرفته شده‌اند که با هم سازگاری اثبات‌شده دارند (Google architecture-samples).

## 3. ساختار پروژه

```
app/src/main/java/ir/ahvaz/mehra/
├── MehraApplication.kt        # Hilt + WorkManager + lifecycle + realtime
├── MainActivity.kt            # edge-to-edge, splash, RootScreen
├── core/                      # پایه‌های مشترک
│   ├── common/                # AppResult/AppError, پول, اعداد فارسی, اعتبارسنجی
│   ├── date/                  # تبدیل دقیق جلالی↔میلادی, Asia/Tehran, IsoFormat
│   ├── network/               # ApiExecutor, interceptors, refresh, realtime (SignalR)
│   ├── security/              # SecureStore, SessionManager, ServerUrlProvider, AppLockManager
│   ├── datastore/             # SettingsRepository
│   ├── notifications/         # کانال‌ها و انتشار اعلان
│   ├── media/                 # بارگذاری تصویر احرازهویت‌شده
│   ├── export/                # PDF (RTL با StaticLayout) و XLSX (بدون وابستگی)
│   ├── di/                    # Network/Database/Repository/App modules
│   └── ui/                    # theme (۴ پوسته), components (Surfaces/Inputs/Charts/States…)
├── domain/                    # مدل‌ها و اینترفیس ریپازیتوری‌ها (بدون وابستگی به اندروید)
├── data/                      # DTO، Mapper، API، Room، Sync، پیاده‌سازی ریپازیتوری‌ها
├── feature/                   # هر ماژول: ViewModel + Screen
└── navigation/                # Routes + NavHost + BottomBar
```

## 4. اجرا و ساخت

```bash
# پیش‌نیاز: JDK 17، Android SDK (API 35)، Android Studio Ladybug یا جدیدتر
./gradlew :app:assembleDebug        # APK دیباگ
./gradlew :app:assembleRelease      # APK ریلیز (نیازمند keystore.properties برای امضا)
./gradlew :app:testDebugUnitTest    # تست‌های واحد
```

راهنمای کامل: [`docs/INSTALL.md`](docs/INSTALL.md).
تنظیمات و الزامات سرور: [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md).
قرارداد API: [`docs/API.md`](docs/API.md).
گزارش تست و مشکلات شناخته‌شده: [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

## 5. نکات امنیتی پیاده‌شده

- توکن‌ها فقط در `EncryptedSharedPreferences`؛ هیچ رمز یا کلیدی در سورس نیست (`allowBackup=false`).
- لاگ‌ها هرگز توکن/رمز/اطلاعات هویتی مهمان را چاپ نمی‌کنند (interceptor فقط متادیتای امن).
- قفل محلی با PIN شش‌رقمی (PBKDF2WithHmacSHA1، ۱۲۰ هزار تکرار، salt تصادفی، مقایسه زمان‌ثابت) و قفل خودکار پس از بی‌کاری.
- دسترسی به اطلاعات هویتی مهمان فقط با مجوز `guests.pii.view` و به‌صورت پوشانده‌شده توسط پیش‌فرض.
- هر عملیات مالی/پذیرش با `Idempotency-Key` یکتا؛ تکراری‌ها در صف آفلاین با همان کلید بازپخش می‌شوند.
- HTTPS در نسخه ریلیز اجباری است؛ cleartext فقط در variant دیباگ و برای LAN مجاز است.
- مجوزها فقط «راهنمای UI» هستند؛ تصمیم نهایی همیشه سمت سرور است.

## 6. آفلاین و همگام‌سازی

- خواندن‌ها read-through: نتیجه موفق کش می‌شود و در قطعی، همان داده با علامت «کش‌شده» و زمان به‌روزرسانی نمایش داده می‌شود.
- نوشتن‌های حساس در قطعی، در `outbox` ذخیره و با همان idempotency key بازپخش می‌شوند؛ UI وضعیت «در انتظار تأیید سرور» را نشان می‌دهد.
- `SyncWorker` دوره‌ای و هنگام بازگشت شبکه اجرا می‌شود؛ خطای ۴xx (به‌جز 408/429) به‌عنوان رد قطعی علامت‌گذاری می‌شود.
