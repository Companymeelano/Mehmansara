# MEHRA — APK تست (debug)

**فایل محلی:** `artifacts/mehra-1.0.0-debug.apk` (۲۰٫۸ مگابایت، امضاشده با debug keystore)

## لینک‌های دانلود (مخزن عمومی است، نیازی به ورود نیست)

| نوع | لینک |
|---|---|
| دانلود مستقیم | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-debug.apk |
| صفحه فایل با دکمه Download | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-debug.apk |
| صفحه انتشار | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-test |
| اجرای CI و آرتیفکت‌ها | https://github.com/Companymeelano/Mehmansara/actions |

> لینک شاخه `ci-artifacts` همیشه **آخرین بیلد موفق** را می‌دهد؛ با هر push در CI بازسازی می‌شود
> (به همین دلیل هش فایل با هر بیلد تغییر می‌کند). فایل همین پوشه، نسخه ثابت‌شدهٔ همین تحویل است.

## مشخصات

| مورد | مقدار |
|---|---|
| بسته | `ir.ahvaz.mehra.debug` |
| نسخه | 1.0.0 (versionCode 1) |
| حجم | 20,843,619 بایت |
| SHA-256 | `c78bbc3b40349d4cf4d6e8221b420ce60c08fc3a4d4437e1e70a654d8f7e8107` |
| minSdk / targetSdk | 24 / 35 |
| ساخت | JDK 17 • AGP 8.7.3 • Gradle 8.11.1 • GitHub Actions |
| امضا | APK Signature Scheme v2 با debug keystore (فقط تست) |

## نصب

```bash
adb install -r mehra-1.0.0-debug.apk
```

یا انتقال فایل به گوشی → لمس فایل → اجازه «نصب از منابع ناشناس».

## اولین اجرا

1. صفحه «اتصال به سرور» → آدرس سرور REST (در دیباگ `http://` برای LAN مجاز است، مثلاً `http://192.168.1.20:5080`).
2. «بررسی اتصال» → آزمون `GET /api/v1/system/health` → «ذخیره نشانی».
3. ورود با کاربر ساخته‌شده در سرور.

**بک‌اند هنوز پیاده‌سازی نشده است**؛ اپ تا بالا آمدن سروری سازگار با `docs/API.md` در همین مرحله می‌ماند و هیچ داده نمونه‌ای نشان نمی‌دهد.

## چه چیزی در این بیلد اثبات شده

- کامپایل کامل پروژه (Kotlin، Compose، Hilt/KSP، Room، WorkManager) با SDK 35.
- تست‌های واحد سبز: تبدیل جلالی↔میلادی، ارقام و مبلغ فارسی، اعتبارسنجی کد ملی/موبایل، نویسنده XLSX.
- منابع و Manifest سالم (Vazirmatn، `ic_notification`، ۱۹ dex).

## چه چیزی تست نشده

اتصال به سرور واقعی، ورود، رزرو، پذیرش، پرداخت، SignalR، صف آفلاین و چیدمان RTL روی دستگاه واقعی.
فهرست کامل: `docs/TEST_REPORT.md`.
