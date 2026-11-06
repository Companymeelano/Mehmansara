# نسخهٔ ویندوز (Mehra.exe)

## فایل‌های قابل دانلود

- انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.0.0-windows
- دانلود مستقیم exe: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- فایل zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- صفحهٔ فایل‌ها: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe
- لاگ build: https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/logs/windows-build.log

## هش و تأیید باینری (نسخهٔ نهایی — اجرای CI `42276592462`، کامیت `13952c7`)

- `exe/Mehra.exe`: 92,797,563 بایت — sha256 `6a55794e74559c720ede4da08d362f263a211f1720b5fcca192fdb53a8eb27f4`
- `exe/Mehra-win-x64.zip`: 40,954,471 بایت (حاوی Mehra.exe + README.txt فارسی)
- بررسی بایت‌به‌بایت: MZ/PE معتبر، ماشین 0x8664 (x64)، PE32+، subsystem 2 (Windows GUI)،
  ressource آیکن (mehra.ico) و متادیتای نسخه (1.0.0.0، Culturalists of Ahvaz، «طراح و توسعه‌دهنده: میلاد یعقوبی») در باینری موجودند؛
  رشته‌های فارسیِ همهٔ ۱۲ صفحه (خانه، اتاق‌ها، رزروها، پذیرش، مهمانان، مالی، نظافت، تعمیرات، گزارش‌ها، کاربران، اعلانات، تنظیمات) به‌صورت UTF-16 در باینری هستند.

## نحوهٔ اجرا

1. فایل `Mehra.exe` را دانلود و اجرا کنید (win-x64 و self-contained؛ نیازی به نصب .NET نیست).
2. **ورود نمایشی:** بدون سرور — دادهٔ نمونهٔ کامل برای آشنایی با برنامه.
3. **ورود واقعی:** نشانی سرور ASP.NET Core (مطابق `docs/API.md`) + نام کاربری و رمز.

## صداقت در گزارش

- exe توسط GitHub Actions روی `windows-latest` کامپایل و منتشر می‌شود؛ در محیط توسعهٔ این مخزن SDK دات‌نت وجود ندارد، بنابراین «تست زمان‌اجرا روی ویندوز» انجام نشده و فقط سبز بودن کامپایل CI تأیید شده است.
- فایل exe امضا نشده (unsigned) است؛ ویندوز ممکن است هشدار SmartScreen نشان دهد.
- حالت واقعی نیازمند بک‌اند است که هنوز در این مخزن پیاده‌سازی نشده (`docs/SERVER_SETUP.md`).
- دادهٔ نمونه فقط در حالت نمایشی وجود دارد؛ حالت عملیاتی هیچ دادهٔ نمونه‌ای ندارد.

## جزئیات

- کد: `desktop/Mehra.Desktop/` — راهنمای کامل: `desktop/README.md`
- CI: `.github/workflows/windows-ci.yml` (شاخهٔ `ci-artifacts`، مسیر `exe/`)
- صفحات (۱۲ مورد): خانه / اتاق‌ها / رزروها / پذیرش / مهمانان / مالی / نظافت / تعمیرات / گزارش‌ها / کاربران / اعلانات / تنظیمات — کاملاً فارسی، RTL، تاریخ جلالی، فونت وزیرمتن، آیکن و متادیتای نسخهٔ 1.0.0.
- طراح و توسعه‌دهنده: میلاد یعقوبی
