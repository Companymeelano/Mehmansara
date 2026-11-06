# MEHRA — مهرآ

اپلیکیشن مدیریت مهمان‌پذیر فرهنگیان اهواز (Android) — Persian-first, RTL, offline-tolerant.

> **وضعیت تحویل**
> - 🎬 **نسخه نمایشی برای معرفی به مشتری آماده است** (بدون سرور، با داده نمونه کامل):
>   - دانلود مستقیم: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/mehra-demo-debug.apk
>   - صفحه فایل: https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/mehra-demo-debug.apk
>   - فایل ثابت‌شده: `artifacts/mehra-demo-1.0.0-debug.apk` — راهنما: [`docs/DEMO.md`](docs/DEMO.md)
> - ✅ **APK نسخه عملیاتی** هم ساخته و منتشر شده است:
>   - دانلود مستقیم: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-debug.apk
>   - صفحه دانلود: https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-debug.apk
>   - انتشار: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-test
>   - فایل محلی: `artifacts/mehra-1.0.0-debug.apk` (جزئیات در `artifacts/README.md`)
> - 🖥️ **نسخهٔ نهایی ویندوز (Mehra.exe 1.0) منتشر شد** — انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-windows — برنامهٔ رومیزی کارکنان (Avalonia/.NET 8، win-x64 self-contained):
  - دانلود مستقیم: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
  - zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
  - جزئیات و صداقت در گزارش: [`docs/WINDOWS.md`](docs/WINDOWS.md)
- ✅ **تست‌های واحد سبز**: تبدیل جلالی، ارقام/پول فارسی، نویسنده XLSX.
> - ⏳ **تست زمان‌اجرا روی دستگاه و سرور واقعی انجام نشده**؛ بک‌اند ASP.NET Core هنوز
>   پیاده‌سازی نشده است و نسخه عملیاتی تا زمانی که سرور سازگار با `docs/API.md` بالا نیاید از
>   مرحله «اتصال به سرور» عبور نمی‌کند (هیچ داده نمونه‌ای نمایش داده نمی‌شود).
> جزئیات: [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md) و [`artifacts/README.md`](artifacts/README.md).

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

## 2. دو flavour: نسخه عملیاتی و نسخه نمایشی

| مورد | `prod` (عملیاتی) | `demo` (نمایشی) |
|---|---|---|
| بسته | `ir.ahvaz.mehra` (`*.debug` در بیلد دیباگ) | `ir.ahvaz.mehra.demo` |
| منبع داده | فقط سرور واقعی (REST/SignalR) | داده درون‌حافظه‌ای در `app/src/demo` |
| صفحه اتصال به سرور | لازم | ندارد؛ مستقیم به داشبورد |
| داده نمونه | **هیچ** | همه‌جا (۲۴ اتاق، ۱۲ مهمان، ۱۹ رزرو، صندوق، گزارش‌ها) |
| ساخت | `./gradlew :app:assembleProdDebug` | `./gradlew :app:assembleDemoDebug` |

نسخه نمایشی برای معرفی به مشتری و آموزش کارکنان است و می‌تواند کنار نسخه واقعی روی همان گوشی
نصب شود. کد و داده نمایشی فقط در source set مربوط به `demo` قرار دارد؛ بیلد `prod` حتی یک کلاس
نمایشی هم در dex ندارد (این موضوع با بازرسی بایت‌به‌بایت APK تأیید شده است). راهنمای کامل نمایش
و مسیر پیشنهادی ۵ دقیقه‌ای برای مشتری: [`docs/DEMO.md`](docs/DEMO.md).

## 3. پشته فنی

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

## 4. سیستم طراحی لاکچری

- **۷ پوستهٔ تم** (طلایی مدیران، ابسیدین سلطنتی، پلاتینیوم لوکس، افق نقره‌ای، یاقوت کبود، زمرد تاج، یاقوت شب) — قابل انتخاب در تنظیمات؛ تم انتخابی به‌صورت گرادیان در **همهٔ** بخش‌ها اعمال می‌شود: پس‌زمینهٔ سراسری، نوار بالای گرادیانی، کارت‌های شیشه‌ای با border گرادیانی، نوار پایین شناور و دکمه‌های سه‌بعدی.
- **آیکن‌های سه‌بعدی SVG**: فایل‌های master در `design/icons/svg/`، معادل برداری در `core/ui/icons/MehraIcons.kt` و رندر نهایی با چیپ گرادیانی تم در `Icon3D` — به‌این‌ترتیب هر آیکن (نوار پایین، کاشی‌های ماژول‌ها، آمار داشبورد، دکمه‌ها) خودکار با تم انتخابی هماهنگ است.
- **امضای سه‌بعدی سازنده**: نام «Milad Yaghoobi» به‌صورت اکسترود فلزی سه‌بعدی با رنگ فلزیِ تم انتخابی (رندر روی Canvas در `CreatorSignature`) در صفحات ورود، قفل، «بیشتر» و «دربارهٔ سامانه».
- جزئیات کامل: [docs/DESIGN.md](docs/DESIGN.md)

## 5. ساختار پروژه

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

## 6. اجرا و ساخت

بیلد در CI (بدون نیاز به نصب چیزی روی سیستم شما): هر push به هر شاخه، workflow «Android CI» را اجرا می‌کند و APK را در شاخه `ci-artifacts` می‌گذارد.

```bash
# پیش‌نیاز محلی: JDK 17، Android SDK (API 35)، Android Studio Ladybug یا جدیدتر
./gradlew :app:assembleProdDebug      # APK دیباگ نسخه عملیاتی
./gradlew :app:assembleDemoDebug      # APK نسخه نمایشی (بدون سرور)
./gradlew :app:assembleProdRelease    # APK ریلیز (نیازمند keystore.properties برای امضا)
./gradlew :app:testProdDebugUnitTest  # تست‌های واحد
```

خروجی‌ها: `app/build/outputs/apk/prod/debug/` و `app/build/outputs/apk/demo/debug/`.

راهنمای کامل: [`docs/INSTALL.md`](docs/INSTALL.md).
تنظیمات و الزامات سرور: [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md).
قرارداد API: [`docs/API.md`](docs/API.md).
گزارش تست و مشکلات شناخته‌شده: [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

## 7. نکات امنیتی پیاده‌شده

- توکن‌ها فقط در `EncryptedSharedPreferences`؛ هیچ رمز یا کلیدی در سورس نیست (`allowBackup=false`).
- لاگ‌ها هرگز توکن/رمز/اطلاعات هویتی مهمان را چاپ نمی‌کنند (interceptor فقط متادیتای امن).
- قفل محلی با PIN شش‌رقمی (PBKDF2WithHmacSHA1، ۱۲۰ هزار تکرار، salt تصادفی، مقایسه زمان‌ثابت) و قفل خودکار پس از بی‌کاری.
- دسترسی به اطلاعات هویتی مهمان فقط با مجوز `guests.pii.view` و به‌صورت پوشانده‌شده توسط پیش‌فرض.
- هر عملیات مالی/پذیرش با `Idempotency-Key` یکتا؛ تکراری‌ها در صف آفلاین با همان کلید بازپخش می‌شوند.
- HTTPS در نسخه ریلیز اجباری است؛ cleartext فقط در variant دیباگ و برای LAN مجاز است.
- مجوزها فقط «راهنمای UI» هستند؛ تصمیم نهایی همیشه سمت سرور است.

## 8. آفلاین و همگام‌سازی

- خواندن‌ها read-through: نتیجه موفق کش می‌شود و در قطعی، همان داده با علامت «کش‌شده» و زمان به‌روزرسانی نمایش داده می‌شود.
- نوشتن‌های حساس در قطعی، در `outbox` ذخیره و با همان idempotency key بازپخش می‌شوند؛ UI وضعیت «در انتظار تأیید سرور» را نشان می‌دهد.
- `SyncWorker` دوره‌ای و هنگام بازگشت شبکه اجرا می‌شود؛ خطای ۴xx (به‌جز 408/429) به‌عنوان رد قطعی علامت‌گذاری می‌شود.
