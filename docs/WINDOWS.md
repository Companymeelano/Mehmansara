# نسخهٔ ویندوز ۱.۱.۵ — نصب‌گر یک‌فایلهٔ مهرآSetup.exe (برنامه + SQL Server LocalDB همراه + فونت‌ها)

> **از نسخهٔ ۱.۱.۵ به بعد، فقط یک فایل `MehraSetup.exe` کافی است:** نصب‌گر همچنین برنامه، **SQL Server 2016 LocalDB** (موتور واقعی SQL Server، نسخهٔ سبک حدود ۴۵ مگابایت) را هم به‌صورت silent داخل همان فایل نصب می‌کند، instance `(localdb)\MSSQLLocalDB` را می‌سازد، پایگاه دادهٔ `Mehra` و ۹ جدول را از `db\schema.sql` ایجاد می‌کند و تنظیمات اتصال را در `%AppData%\mehra\settings.json` می‌نویسد — یعنی از «صفر تا ثبت واقعی داده» فقط با اجرای یک فایل.

## فایل‌های قابل دانلود (نسخهٔ ۱.۱.۵)

- انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows
- **نصب‌گر کاملِ یک‌فایله (پیشنهادی —viron ۹۲ مگابایت):** https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe
- لاگ ساخت نصب‌گر (Inno Setup): https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/setup-build.log
- exe خام (بدون نصب‌گر): https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- مرور فایل‌ها: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/setup و https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe

## هش و اندازهٔ باینری‌ها (اجرای CI `43301411566`)

- `setup/MehraSetup.exe`: 89,558,970 بایت — sha256 `a69e7d6438fd8fe63beafb588be53fa6b63deb31d27d1564b5b1ef5fe8dab2cb`
- `exe/Mehra.exe`: 94,192,520 بایت — sha256 `3e07c8fdf9d7649efc97b827f449eacde28185b1dc0f682cf9ee85ba5eab2c1c`
- `exe/Mehra-win-x64.zip`: 41,815,275 بایت — sha256 `ebca0d83517dae418a83b5824ce62b587fa2ef475ad969be6523635a73cf5fe5`
- تأیید بایت‌به‌بایت: MZ/PE معتبر؛ رشته‌های برند به‌صورت UTF-16/UTF-8 داخل نصب‌گر موجود است: `www.meelano.ir`، `09120219593`، `Milad Yaghoobi`، «میلاد یعقوبی»، «مهرآ»، نسخهٔ ۱.۱.۵؛ لاگ ISCC وجود `SqlLocalDB.msi` را در فایل نصب تأیید می‌کند («Compressing: ...\redist\SqlLocalDB.msi»).

## نصب با MehraSetup.exe (یک فایل — همه‌چیز)

1. `MehraSetup.exe` را اجرا کنید (نیاز به اجرای «به‌عنوان Administrator» دارد — UAC را تأیید کنید). تمام مراحل نصب فارسی و راست‌به‌چپ، با فونت وزیرمتن.
2. در صفحهٔ **مؤلفه‌ها**، گزینهٔ «SQL Server LocalDB 2016 همراه (~۴۵ مگابایت)» به‌صورت پیش‌فرض فعال است — با نگه‌داشتن آن، موتور پایگاه داده همراه برنامه نصب می‌شود. (اگر SQL Server دیگری روی سیستم دارید و نمی‌خواهید LocalDB نصب شود، تیک آن را بردارید.)
3. در صفحهٔ **پایگاه دادهٔ SQL Server** پیش‌فرض‌ها کافی است: سرور `(localdb)\MSSQLLocalDB`، پایگاه دادهٔ `Mehra`، احراز هویت ویندوز. چون LocalDB در همین نصب ساخته می‌شود، تست زندهٔ اتصال در این صفحه رد می‌شود و خود نصب‌گر پس از کپی فایل‌ها همه‌چیز را می‌سازد.
4. هنگام نصب (مرحلهٔ post-install): ابتدا `SqlLocalDB.msi` به‌صورت silent نصب می‌شود (`msiexec /qn IACCEPTSQLLOCALDBLICENSETERMS=YES REBOOT=ReallySuppress`)، بعد `sqllocaldb create MSSQLLocalDB -s` و `start` اجرا می‌شود، سپس اسکریپت `db\schema.sql` batch‌به-batch (GO) اجرا و ۹ جدول + کاربر `admin` ساخته می‌شود، و در پایان `%AppData%\mehra\settings.json` نوشته می‌شود.
5. بررسی‌های هوشمند: حداقل ۹۰۰ مگابایت فضای آزاد برای LocalDB (در صفحهٔ مؤلفه‌ها چک می‌شود)، حداقل ۳۰۰ مگابایت در شروع، Windows 10 نسخهٔ 1607 (build 14393) / Server 2016 به بالا — فقط ۶۴ بیت، خطای اتصال SQL — همگی با پیام فارسی.

6. فونت‌های وزیرمتن (۶ وزن) نیز نصب دائمی می‌شوند و خود نصب‌گر هم با وزیرمتن نمایش داده می‌شود.
7. گزینه‌های پیشرفتهٔ نصب (صفحهٔ «امکانات نصب»): مسیر نصب پیش‌فرض `{autopf}\Mehra` با امکان تغییر (صفحهٔ مقصد نصب — با چک هوشمند فضای خالی همان درایو: ۹۰۰MB با LocalDB، ۳۵۰MB بدون آن)، «میان‌بر دسکتاپ»، «اجرای خودکار هنگام بالا آمدن ویندوز (Startup)»، «اجرای برنامه پس از پایان نصب» و «باز کردن پوشهٔ نصب پس از پایان».
8. ظاهر لاکچری: آیکون 3D اختصاصی (کلید طلایی روی کاشی سرمه‌ای — ۶ اندازه در mehra.ico)، بنرهای جادوی نصب با تم سرمه‌ای-طلایی (art_side.bmp ۱۶۴×۳۱۴ و art_top.bmp ۵۵×۵۵)، فونت وزیرمتن در کل wizard و پیشرفت درصد فارسی.
7. پس از پایان، برنامه با حالت «ورود به SQL Server» و آدرس `(localdb)\MSSQLLocalDB` آمادهٔ ورود است — ورود اولیه: کاربر `admin` / رمز `Admin@1234`. از آن لحظه همهٔ ۱۲ صفحه (اتاق‌ها، مهمانان، رزرو، پذیرش، مالی، نظافت، تعمیرات، گزارش‌ها، کاربران، اعلان‌ها، تنظیمات، خانه) **دادهٔ واقعی** را در پایگاه داده ثبت می‌کنند.

## اگر LocalDB همراه نصب نشود (مثلاً règles derechos): 
- می‌توانید بعداً «SQL Server LocalDB» را دستی از مایکروسافت نصب کنید، یا در همان صفحهٔ نصب به SQL Server موجود در شبکه (مثلاً `192.168.1.10\SQLEXPRESS` با SQL Authentication) متصل شوید — نصب‌گر همان‌جا جداول را می‌سازد.
- حذف برنامه (uninstall) **LocalDB را حذف نمی‌کند** (یک مؤلفهٔ sistمی ویندوز است) — فقط برنامهٔ مهرآ و فونت‌ها/تنظیمات حذف می‌شوند.

## حالت‌های ورود برنامه

- «ورود نمایشی»: آفلاین، با دادهٔ نمونهٔ کامل (فقط دادهٔ نمایشی — روی دیسک ثبت نمی‌شود).
- «ورود به API»: اتصال به سرور web (envelope `{success,data,error}` + Bearer — docs/API.md).
- «ورود به SQL Server»: اتصال مستقیم به Microsoft SQL Server با System.Data.SqlClient؛ اگر جداول ساخته نشده باشند، دکمهٔ «راه‌اندازی/به‌روزرسانی جداول» در تنظیمات با `db\schema.sql` آن‌ها را می‌سازد.

طراح و توسعه‌دهنده: Milad Yaghoobi (میلاد یعقوبی) — www.meelano.ir — 09120219593

## صداقت در گزارش

- باینری‌ها فقط توسط GitHub Actions (windows-latest) کامپایل و منتشر شده‌اند و بایت‌به‌بایت بررسی شده‌اند (MZ/PE، اندازه، sha256، برند/نسخهٔ، و وجود SqlLocalDB.msi داخل نصب‌گر)؛ **روی یک PC ویندوز واقعی با SQL Server زنده اجرا و تست نشده‌اند**.
- نصب‌گر و exe امضا نشده‌اند (unsigned) — ممکن است SmartScreen نمایش دهد («More info → Run anyway»).
- LocalDB یک مؤلفهٔ رسمی مایکروسافت با مجوز قابل‌توزیع (redistributable) است؛ نصب silent آن با `IACCEPTSQLLOCALDBLICENSETERMS=YES` پذیرش license را می‌پذیرد.
- دادهٔ نمونه فقط در «ورود نمایشی» وجود دارد؛ حالت SQL Server هیچ دادهٔ نمونه‌ای ندارد.
