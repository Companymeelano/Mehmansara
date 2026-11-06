# نسخهٔ ویندوز (Mehra.exe)

## فایل‌های قابل دانلود

- دانلود مستقیم exe: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- فایل zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- صفحهٔ فایل‌ها: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe
- لاگ build: https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/logs/windows-build.log

## هش و تأیید باینری (sha256) (اجرای CI `42267706303`، کامیت `eb2f84d`)

- `exe/Mehra.exe`: 92,138,107 بایت — sha256 `00031abe126cd446e1e364c903102b3f2aa90064f955879d5d06a3ed49a8af1c`
- `exe/Mehra-win-x64.zip`: 40,589,323 بایت (حاوی همان Mehra.exe)
- بررسی بایت‌به‌بایت: MZ/PE معتبر، ماشین 0x8664 (x64)، PE32+، subsystem 2 (Windows GUI)،
  رشته‌های فارسی UI به‌صورت UTF-16 در باینری موجودند.

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
- صفحات: خانه / اتاق‌ها / رزروها / پذیرش / تنظیمات — کاملاً فارسی، RTL، تاریخ جلالی، فونت وزیرمتن.
- طراح و توسعه‌دهنده: میلاد یعقوبی
