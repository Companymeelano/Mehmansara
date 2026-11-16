# نسخهٔ ویندوز ۱.۱.۷ — نصب‌گر یک‌فایلهٔ مهرآSetup.exe (برنامه + SQL Server LocalDB همراه + فونت‌ها)

> **از نسخهٔ ۱.۱.۵ به بعد، فقط یک فایل `MehraSetup.exe` کافی است** (نسخهٔ فعلی: ۱.۱.۷): نصب‌گر همچنین برنامه، **SQL Server 2016 LocalDB** (موتور واقعی SQL Server، نسخهٔ سبک حدود ۴۵ مگابایت) را هم به‌صورت silent داخل همان فایل نصب می‌کند، instance `(localdb)\MSSQLLocalDB` را می‌سازد، پایگاه دادهٔ `Mehra` و ۹ جدول را از `db\schema.sql` ایجاد می‌کند و تنظیمات اتصال را در `%AppData%\mehra\settings.json` می‌نویسد — یعنی از «صفر تا ثبت واقعی داده» فقط با اجرای یک فایل.

## فایل‌های قابل دانلود (نسخهٔ ۱.۱.۷)

- انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows
- **نصب‌گر کاملِ یک‌فایله (پیشنهادی حدود ۹۰ مگابایت):** https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe
- لاگ ساخت نصب‌گر (Inno Setup): https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/setup-build.log
- exe خام (بدون نصب‌گر): https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- مرور فایل‌ها: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/setup و https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe

## هش و اندازهٔ باینری‌ها (اجرای CI `43938463249` — نسخهٔ ۱.۱.۷)

- `setup/MehraSetup.exe`: 89,551,373 بایت — sha256 `5447dc9032603a853cce8c7a7e2d90604574ae868f754eeed64bd567e4a6fe60`
- `exe/Mehra.exe`: 94,196,616 بایت — sha256 `177644c4655492989e71465fb6ff2f3bd152380e23b252654794160f1075feac`
- `exe/Mehra-win-x64.zip`: 41,817,721 بایت — sha256 `addc3d6770bfc9ab828bbc732b96a4b33f0f581370c14a86a9661a1c53ecca47`
- تأیید بایت‌به‌بایت: MZ/PE معتبر؛ رشته‌های برند به‌صورت UTF-16/UTF-8 داخل نصب‌گر موجود است: `www.meelano.ir`، `09120219593`، `Milad Yaghoobi`، «میلاد یعقوبی»، «مهرآ»، نسخهٔ ۱.۱.۷؛ لاگ ISCC وجود `SqlLocalDB.msi` را در فایل نصب تأیید می‌کند («Compressing: ...\redist\SqlLocalDB.msi»).

## تغییرات نسخهٔ ۱.۱.۷ در نصب‌گر

- برچسب ایتالیایی-اسپانیایی «گزینه‌های Durée controladan» در صفحهٔ «امکانات نصب» به «گزینه‌های اضافی:» اصلاح شد
- صفحهٔ خوش‌آمد: نسخهٔ ناقص «۱.۱» به «۱.۱.۷» اصلاح شد، خط‌ها واقعاً می‌شکنند، و متن لاتین `(Milad Yaghoobi)` که چیدمان راست‌به‌چپ را به‌هم می‌ریخت حذف شد (همین‌طور در صفحهٔ پایان)
- صفحهٔ SQL: مثال‌های لاتین سرور (`localhost` / `.\\SQLEXPRESS` / `.\\MSSQLSERVER` / `(localdb)\\MSSQLLocalDB`) روی خط‌های جداگانه؛ پیش‌فرض نام کاربری `sa` حذف شد — نام کاربری و رمز «اختیاری» هستند و خالی یعنی احراز هویت ویندوز
- پنجرهٔ نصب‌گر قابل تغییر اندازه شد (`WizardResizable=yes`); تایپوگرافی لاکچری صفحهٔ خوش‌آمد و پایان با وزیرمتن بولد (۱۷pt / ۱۶pt)
- نسخه‌ها: AppVersion 1.1.7، AppVerName «مهرآ ۱.۱.۷ — ویندوز»، VersionInfoVersion 1.1.7.0 و csproj 1.1.7(.0)

## نصب با MehraSetup.exe (یک فایل — همه‌چیز)

1. `MehraSetup.exe` را اجرا کنید (نیاز به اجرای «به‌عنوان Administrator» دارد — UAC را تأیید کنید). تمام مراحل نصب فارسی و راست‌به‌چپ، با فونت وزیرمتن.
2. در صفحهٔ **مؤلفه‌ها**، گزینهٔ «SQL Server LocalDB 2016 همراه (~۴۵ مگابایت)» به‌صورت پیش‌فرض فعال است — با نگه‌داشتن آن، موتور پایگاه داده همراه برنامه نصب می‌شود. (اگر SQL Server دیگری روی سیستم دارید و نمی‌خواهید LocalDB نصب شود، تیک آن را بردارید.)
3. در صفحهٔ **تنظیمات اتصال SQL Server** (نسخهٔ ۱.۱.۷): پیش‌فرض سرور `(localdb)\\MSSQLLocalDB` و پایگاه دادهٔ `Mehra` کافی است؛ **نام کاربری و رمز عبور را خالی بگذارید** (احراز هویت ویندوز — توصیه‌شده). اگر SQL Server 2022 جداگانه نصب کرده‌اید و LocalDB همراه را تیک نزده‌اید، در فیلد سرور یکی از `localhost`، `.\\SQLEXPRESS` یا `.\\MSSQLSERVER` را بنویسید؛ نام کاربری/رمز فقط در حالت SQL Authentication لازم است (مثلاً `sa` و رمز آن). چون LocalDB در همین نصب ساخته می‌شود، تست زندهٔ اتصال در این صفحه رد می‌شود و خود نصب‌گر پس از کپی فایل‌ها همه‌چیز را می‌سازد.
4. هنگام نصب (مرحلهٔ post-install): ابتدا `SqlLocalDB.msi` به‌صورت silent نصب می‌شود (`msiexec /qn IACCEPTSQLLOCALDBLICENSETERMS=YES REBOOT=ReallySuppress`)، بعد `sqllocaldb create MSSQLLocalDB -s` و `start` اجرا می‌شود، سپس اسکریپت `db\schema.sql` batch‌به-batch (GO) اجرا و ۹ جدول + کاربر `admin` ساخته می‌شود، و در پایان `%AppData%\mehra\settings.json` نوشته می‌شود.
5. بررسی‌های هوشمند: حداقل ۹۰۰ مگابایت فضای آزاد برای LocalDB (در صفحهٔ مؤلفه‌ها چک می‌شود)، حداقل ۳۰۰ مگابایت در شروع، Windows 10 نسخهٔ 1607 (build 14393) / Server 2016 به بالا — فقط ۶۴ بیت، خطای اتصال SQL — همگی با پیام فارسی.

6. فونت‌های وزیرمتن (۶ وزن) نیز نصب دائمی می‌شوند و خود نصب‌گر هم با وزیرمتن نمایش داده می‌شود.
7. گزینه‌های پیشرفتهٔ نصب (صفحهٔ «امکانات نصب»): مسیر نصب پیش‌فرض `{autopf}\Mehra` با امکان تغییر (صفحهٔ مقصد نصب — با چک هوشمند فضای خالی همان درایو: ۹۰۰MB با LocalDB، ۳۵۰MB بدون آن)، «میان‌بر دسکتاپ»، «اجرای خودکار هنگام بالا آمدن ویندوز (Startup)»، «اجرای برنامه پس از پایان نصب» و «باز کردن پوشهٔ نصب پس از پایان».
8. ظاهر لاکچری: آیکون 3D اختصاصی (کلید طلایی روی کاشی سرمه‌ای — ۶ اندازه در mehra.ico)، بنرهای جادوی نصب با تم سرمه‌ای-طلایی (art_side.bmp ۱۶۴×۳۱۴ و art_top.bmp ۵۵×۵۵)، فونت وزیرمتن در کل wizard و پیشرفت درصد فارسی.
7. پس از پایان، برنامه با حالت «ورود به SQL Server» و آدرس `(localdb)\MSSQLLocalDB` آمادهٔ ورود است — ورود اولیه: کاربر `admin` / رمز `Admin@1234`. از آن لحظه همهٔ ۱۲ صفحه (اتاق‌ها، مهمانان، رزرو، پذیرش، مالی، نظافت، تعمیرات، گزارش‌ها، کاربران، اعلان‌ها، تنظیمات، خانه) **دادهٔ واقعی** را در پایگاه داده ثبت می‌کنند.

## اگر LocalDB همراه نصب نشود :
- می‌توانید بعداً «SQL Server LocalDB» را دستی از مایکروسافت نصب کنید، یا در همان صفحهٔ نصب به SQL Server موجود در شبکه (مثلاً `192.168.1.10\SQLEXPRESS` با SQL Authentication) متصل شوید — نصب‌گر همان‌جا جداول را می‌سازد.
- حذف برنامه (uninstall) **LocalDB را حذف نمی‌کند** (یک مؤلفهٔ سیستمی ویندوز است) — فقط برنامهٔ مهرآ و فونت‌ها/تنظیمات حذف می‌شوند.

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
