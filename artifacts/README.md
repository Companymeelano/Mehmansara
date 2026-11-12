# MEHRA — فایل‌های APK تحویلی

دو نسخه در این پوشه قرار دارد. هر دو با debug keystore امضا شده‌اند و روی گوشی تستی نصب می‌شوند
(برای انتشار در فروشگاه باید نسخه release با کلید انتشار ساخته شود).

| نسخه | فایل | کاربرد | بسته |
|---|---|---|---|
| **نمایشی** (برای معرفی به مشتری) | `artifacts/mehra-demo-1.0.0-debug.apk` | بدون سرور، با داده نمونه کامل | `ir.ahvaz.mehra.demo.debug` |
| عملیاتی | `artifacts/mehra-1.0.0-debug.apk` | نسخه واقعی؛ به سرور مجموعه وصل می‌شود | `ir.ahvaz.mehra.debug` |

## لینک‌های دانلود (مخزن عمومی است، نیازی به ورود نیست)

| نسخه | دانلود مستقیم | صفحه فایل |
|---|---|---|
| **نمایشی** | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/mehra-demo-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/mehra-demo-debug.apk |
| نمایشی (نسخه ثابت‌شده) | https://github.com/Companymeelano/Mehmansara/raw/arena/276c0f62-mehmansara/artifacts/mehra-demo-1.0.0-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/arena/276c0f62-mehmansara/artifacts/mehra-demo-1.0.0-debug.apk |
| عملیاتی | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-debug.apk |
| صفحه انتشار | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-test | — |
| نصب‌گر ویندوز ۱.۱.۳ (یک‌فایله با LocalDB+OLEDB) | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/MehraSetup.exe |
| اندروید ۱.۳.۱ — عملیاتی debug (v1.3.1) | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-debug.apk |
| اندروید ۱.۳.۱ — نمایشی demo (v1.3.1) | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/demo/mehra-demo-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/demo/mehra-demo-debug.apk |
| **اندروید ۱.۳.۱ — نهایی/واقعی (prod RELEASE، امضاشده v1+v2+v3)** ⭐ | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-release.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-release.apk |
| انتشار اندروید ۱.۳.۱ | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.3.0-android | — |
| انتشار نسخهٔ ویندوز | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows | — |
| انتشار نسخهٔ ویندوز | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-windows | — |
| اجرای CI و آرتیفکت‌ها | https://github.com/Companymeelano/Mehmansara/actions | — |

## نسخهٔ ویندوز (Mehra.exe)

برنامهٔ رومیزی ویندوز (win-x64، self-contained single-file) در همان شاخهٔ `ci-artifacts` منتشر می‌شود
(فایل ۹۲ مگابایتی به‌خاطر حجم، در این پوشه pin نشده و فقط هش/مشخصاتش اینجا ثبت شده است):

| فایل | دانلود مستقیم | حجم | SHA-256 |
|---|---|---|---|
| `exe/Mehra.exe` | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe | 92,797,563 بایت | `6a55794e74559c720ede4da08d362f263a211f1720b5fcca192fdb53a8eb27f4` |
| `exe/Mehra-win-x64.zip` | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip | 40,954,471 بایت | — |
| لاگ build | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/logs/windows-build.log | — | — |

بررسی بایت‌به‌بایت (اجرای CI `43284139706` final ویندوز / `43289800270` final اندروید): سرآیند MZ/PE معتبر، ماشین 0x8664 (x64)،
PE32+ با subsystem 2 (Windows GUI) — یعنی یک فایل اجرایی واقعی ویندوز است؛ رشته‌های فارسی
رابط کاربری (مهرآ، ورود نمایشی، …) به‌صورت UTF-16 در باینری وجود دارند.
جزئیات و صداقت در گزارش: [`docs/WINDOWS.md`](docs/WINDOWS.md).

> شاخه `ci-artifacts` همیشه **آخرین بیلد موفق** را می‌دهد و با هر push بازسازی می‌شود (به همین
> دلیل هش فایل با هر بیلد تغییر می‌کند). فایل‌های همین پوشه، نسخه ثابت‌شدهٔ همین تحویل هستند.

## مشخصات

| مورد | نمایشی | عملیاتی |
|---|---|---|
| بسته | `ir.ahvaz.mehra.demo.debug` | `ir.ahvaz.mehra.debug` |
| نام روی گوشی | مهرآ نمایشی | مهرآ |
| نسخه | 1.0.0-demo-debug (versionCode 1) | 1.0.0-debug (versionCode 1) |
| حجم | 21,002,934 بایت | 20,904,606 بایت |
| SHA-256 | `b392c419f8412fc665b65d9a44478cc1caef6567de4df409e80f96810578dfb2` | `3743f241f3bcdcea1b3a3a2cde637b3a71450bd517d5e636cc7b868afe324699` |
| minSdk / targetSdk | 24 / 35 | 24 / 35 |
| امضا | APK Signature Scheme v2 (debug keystore) | همان |

هر دو APK از یک کامیت ساخته شده‌اند و می‌توانند همزمان روی یک گوشی نصب شوند.

## نصب

```bash
adb install -r mehra-demo-1.0.0-debug.apk    # نسخه نمایشی
adb install -r mehra-1.0.0-debug.apk         # نسخه عملیاتی
```

یا انتقال فایل به گوشی → لمس فایل → اجازه «نصب از منابع ناشناس».

## نسخه نمایشی — اولین اجرا

1. پیام «نسخه نمایشی مهرآ» نمایش داده می‌شود؛ «شروع نمایش» را بزنید.
2. مستقیم وارد داشبورد می‌شوید (بدون صفحه اتصال به سرور، بدون ورود).
3. اگر از برنامه خارج شدید، در صفحه ورود **هر نام کاربری و رمزی** پذیرفته می‌شود
   (نمونه: `admin` / `123456`).
4. داده‌های نمایشی روی حافظه اپ هستند؛ با بستن کامل برنامه به حالت اولیه برمی‌گردند.

راهنمای کامل نمایش و فهرست داده‌ها: [`docs/DEMO.md`](../docs/DEMO.md).

## نسخه عملیاتی — اولین اجرا

1. صفحه «اتصال به سرور» → آدرس سرور REST (در دیباگ `http://` برای LAN مجاز است، مثلاً
   `http://192.168.1.20:5080`).
2. «بررسی اتصال» → آزمون `GET /api/v1/system/health` → «ذخیره نشانی».
3. ورود با کاربر ساخته‌شده در سرور.

**بک‌اند هنوز پیاده‌سازی نشده است**؛ نسخه عملیاتی تا بالا آمدن سروری سازگار با `docs/API.md`
در همین مرحله می‌ماند و هیچ داده نمونه‌ای نشان نمی‌دهد (این یک تصمیم طراحی است، نه نقص).

## چه چیزی در این بیلدها اثبات شده

- کامپایل کامل هر دو flavour (`assembleProdDebug` + `assembleDemoDebug`) با SDK 35 در CI.
- تست‌های واحد سبز (تبدیل جلالی، ارقام و مبلغ فارسی، اعتبارسنجی، نویسنده XLSX).
- بازرسی بایت‌به‌بایت APKها:
  - نسخه نمایشی: بسته `ir.ahvaz.mehra.demo.debug`، کلاس‌های `DemoStore`/`Demo*Repository`،
    برچسب «مهرآ نمایشی» در `resources.arsc`، بلوک امضای v2، ۱۹ dex، فونت‌های Vazirmatn.
  - نسخه عملیاتی: بسته `ir.ahvaz.mehra.debug` و **هیچ** کلاس یا داده نمایشی (`DemoStore`
    در dex وجود ندارد) — یعنی داده نمونه ساختاراً از نسخه واقعی جدا است.
- منابع و Manifest سالم (`ic_notification`، فونت‌ها، FileProvider).

## چه چیزی تست نشده

- اجرای واقعی روی گوشی/امولاتور (محیط ساخت دستگاه نداشت) — شامل چیدمان RTL در صفحه‌های
  واقعی، ثبت رزرو، پذیرش، پرداخت، خروجی PDF/Excel در نمایشگرهای خارجی.
- اتصال به سرور واقعی و SignalR (سرور هنوز وجود ندارد).
- نسخه release امضاشده با کلید انتشار (نیازمند `keystore.properties`).

فهرست کامل: [`docs/TEST_REPORT.md`](../docs/TEST_REPORT.md).
