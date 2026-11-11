# نسخهٔ ویندوز ۱.۱.۰ — نصب‌گر مهرآSetup.exe و برنامهٔ مهرآ.exe

## فایل‌های قابل دانلود (نسخهٔ ۱.۱.۰)

- انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows
- **نصب‌گر کامل (پیشنهادی):** https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe
- لاگ ساخت نصب‌گر (Inno Setup): https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/setup-build.log
- exe خام (بدون نصب‌گر): https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- مرور فایل‌های: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/setup و https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe

## هش اندازهٔ باینری‌ها (اجرای CI `42718935759`)

- `setup/MehraSetup.exe`: 31,440,831 بایت — sha256 `b7063efa12249b90c20502b04dc788cf875ce6c9f30afc308033fcd84baa4e12`
- `exe/Mehra.exe`: 94,110,600 بایت — sha256 `9103a64d34c6238098f9553f8e6ca124d9029c5f7b84af978ec04cc1c297e12a`
- تأیید بایت-be-بایت: MZ/PE mo'tabar; brand dar نصب‌گر be-surat UTF-16/UTF-8 موجود ast: `www.meelano.ir`, `09120219593`, `Milad Yaghoobi`, «میلاد یعقوبی», «مهرآ», نسخهٔ ۱.۱.۰.

## نصب با MehraSetup.exe (فایل کامل)

1. اگر SQL Server نصب شده (از 2008 به تا bālā — LocalDB ya Express ya نسخهٔ-ye kāmil), نصب‌گر kār mikonad; اسکریپت-e جداول داخل exe nist و به‌صورت-جداگانه فایل `db\schema.sql` نصب mishavad.
2. `MehraSetup.exe` rā اجرا کنید; همهٔ-ye مراحل-ye نصب با فارسی راست‌به‌چاپ ast, به‌ فونت وزیرمتن و رنگ‌های لوکس (سرمهای/طلایی).
3. Dar صفحهٔ-ye «پایگاه داده SQL Server» نام سرور (مثالاً `.\SQLEXPRESS` ya `(localdb)\MSSQLLocalDB`) و نام پایگاه داده rā وارد کنید; اتصال تست زنده mishavad و dar صورت-ye خطا, پیام فارسی دقیق نمایش داده می‌شود.
4. به‌ tik-e «ایجاد/به‌روزرانی جداول‌ها», پس از-ye کپی-ye فایل‌های, اسکریپت-e `db\schema.sql` batch-be-batch (GO) ejrā mishavad و 9 جداول ساخته mishavad; درصد پیشرفت فارسی نمایش داده می‌شود.
5. بررسی-haye هوشمند: حداقل 300MB فضای آزاد دیسک, Windows 10 به bālā, خطا-e اتصال SQL Server — hame به‌ پیام فارسی.
6. پس از-ye نصب, تنظیمات-e اتصال dar `%AppData%\mehra\settings.json` نوشته می‌شود mishavad و برنامهٔ آمادهٔ اجرا ast.


## فونت‌ها (Vazirmatn)

- نصب‌گر **6 وزن فونت وزیرمتن** (Regular, Medium, SemiBold, Bold, ExtraBold, Black — نسخهٔ 33.003) را به‌صورت دائمی نصب می‌کند: اگر ادمین باشید در پوشهٔ فونت‌های ویندوز (`{fonts}`), وگرنه به‌صورت per-user (`{userfonts}` — ویندوز 10 نسخهٔ 1803 به بالا) و همزمان session با AddFontResourceW ثبت می‌شوند.
- خود نصب‌گر (همهٔ برچسب‌ها و صفحات) هم با فونت وزیرمتن نمایش داده می‌شود (ثبت خصوصی FR_PRIVATE).
- خود برنامهٔ مهرآ فونت regular/bold را به‌صورت embedded (avares) داخل exe دارد, پس **حتی بدون نصب فونت هم در زمان اجرا با زیباترین شکل نمایش داده می‌شود** و حجم exe به‌خاطر فونت‌های اضافی بالا نرفته است.

ورود avaliye: کاربر `admin` / رمز `Admin@1234` (dar `db\schema.sql` seed shode; dar حالت-e SQL Server هیچ دادهٔ نمونه‌ای-i وجود ندارد).

## حالت-ye ورود برنامهٔ

- «ورود نمایشی»: آفلاین, به‌ دادهٔ نمونه‌ای کامل (فقط همین حالت demo ast).
- «ورود به‌ API»: اتصال به سرور web (envelope `{success,data,error}` + Bearer — docs/API.md).
- «ورود به‌ SQL Server»: اتصال مستقیم به Microsoft SQL Server bā System.Data.SqlClient; جداول‌ها bā `db\schema.sql` ya دکمهٔ-ye «راه‌اندازی/به‌روزرانی جداول‌ها» dar تنظیمات ساخته mishavand.

طراح و توسعه‌دهنده: Milad Yaghoobi (میلاد یعقوبی) — www.meelano.ir — 09120219593

## صداقت dar گزارش

- Bāynari-hā فقط توسط GitHub Actions (windows-latest) کامپایل و منتشر shode-and و بایت-be-بایت بررسی shode-and (MZ/PE, size, sha256, brand/نسخهٔ); **ru-ye Windows-e واقعی ejrā نشده-and**.
- نصب‌گر و exe امضا نشده-and (unsigned); Windows ممکن ast SmartScreen نشان داده.
- حالت-e SQL Server فقط به‌ کامپایل sabz-e CI + review-e script-e `schema.sql` taeed shode و hich-gah bar-xord SQL Server-e زنده test nashode; اگر لازم shod, az دکمهٔ-ye «راه‌اندازی-ye جداول‌ها» dar تنظیمات دوباره تلاش konid.
- دادهٔ نمونه‌ای فقط dar «ورود نمایشی» وجود dārad.
