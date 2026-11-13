# MEHRA — فایل‌های تحویلی

## اندروید ۱.۴.۳ — کاملاً محلی، بدون ورود

از نسخهٔ **۱.۴.۳** برنامهٔ اندروید کاملاً آفلاین است: **صفحهٔ ورود (نام کاربری/رمز) و اتصال به سرور حذف شده‌اند**.
با نصب، اپ مستقیم وارد صفحهٔ اصلی می‌شود و همهٔ آیتم‌ها (اتاق، مهمان — با **پلاک خودرو**، رزرو، پذیرش، صندوق، گزارش) را دستی وارد می‌کنید؛ هیچ دادهٔ نمونه‌ای وجود ندارد.

| نسخه | دانلود مستقیم | صفحهٔ فایل |
|---|---|---|
| **نهایی (RELEASE، امضاشده v1+v2+v3)** ⭐ | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-release.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-release.apk |
| debug | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/apk/app-debug.apk | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/apk/app-debug.apk |
| انتشار اندروید | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.3.0-android | — |
| لاگ build | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/logs/build.log | — |
| هش‌ها (sha256 + اندازهٔ دقیق) | [`SHA256SUMS.txt`](SHA256SUMS.txt) | — |

- `applicationId` = `ir.ahvaz.mehra` (دیباگ: `ir.ahvaz.mehra.debug`)، minSdk 24 / targetSdk 35.
- **قبل از نصب نسخهٔ جدید، نسخهٔ قبلی را Uninstall کنید** (کلید امضای CI ephemeral است).
- روی Xiaomi/HyperOS اجازهٔ «نصب از منابع ناشناخته» را بدهید. اندازهٔ فایل دانلودی را با `SHA256SUMS.txt` چک کنید.

## ویندوز ۱.۱.۵

| نسخه | دانلود مستقیم | صفحهٔ فایل |
|---|---|---|
| نصب‌گر یک‌فایله (LocalDB 2016 + OLE DB 19 داخل setup) ⭐ | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/MehraSetup.exe |
| exe خام | https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe | https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/exe/Mehra.exe |
| انتشار ویندوز | https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows | — |

ورود ویندوز: `admin` / `Admin@1234` — اگر جدول‌ها موجود نباشند، ساخت خودکار انجام می‌شود (۱.۱.۵). جزئیات: [`docs/WINDOWS.md`](../docs/WINDOWS.md).

## CI

- اجرای CI و آرتیفکت‌ها: https://github.com/Companymeelano/Mehmansara/actions
- شاخهٔ `ci-artifacts` همیشه آخرین بیلد موفق را دارد؛ هش‌ها در `SHA256SUMS.txt` pin شده‌اند.
