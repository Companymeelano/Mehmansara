# نسخهٔ ویندوز ۱.۲.۰ — نصب‌گر یک‌فایلهٔ مهرآSetup.exe + حالت «فایل محلی» (بدون SQL Server) + سرور شبکهٔ محلی برای اندروید (برنامه + SQL Server LocalDB همراه + فونت‌ها)

> **از نسخهٔ ۱.۱.۵ به بعد، فقط یک فایل `MehraSetup.exe` کافی است** (نسخهٔ فعلی: ۱.۱.۷): نصب‌گر همچنین برنامه، **SQL Server 2016 LocalDB** (موتور واقعی SQL Server، نسخهٔ سبک حدود ۴۵ مگابایت) را هم به‌صورت silent داخل همان فایل نصب می‌کند، instance `(localdb)\MSSQLLocalDB` را می‌سازد، پایگاه دادهٔ `Mehra` و ۹ جدول را از `db\schema.sql` ایجاد می‌کند و تنظیمات اتصال را در `%AppData%\mehra\settings.json` می‌نویسد — یعنی از «صفر تا ثبت واقعی داده» فقط با اجرای یک فایل.

## فایل‌های قابل دانلود (نسخهٔ ۱.۲.۰)

- انتشار رسمی: https://github.com/Companymeelano/Mehmansara/releases/tag/v1.1.0-windows
- **نصب‌گر کاملِ یک‌فایله (پیشنهادی حدود ۹۰ مگابایت):** https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/setup/MehraSetup.exe
- لاگ ساخت نصب‌گر (Inno Setup): https://github.com/Companymeelano/Mehmansara/blob/ci-artifacts/setup/setup-build.log
- exe خام (بدون نصب‌گر): https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra.exe
- zip: https://github.com/Companymeelano/Mehmansara/raw/ci-artifacts/exe/Mehra-win-x64.zip
- مرور فایل‌ها: https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/setup و https://github.com/Companymeelano/Mehmansara/tree/ci-artifacts/exe

## هش و اندازهٔ باینری‌ها (اجرای CI `43972972466` — نسخهٔ ۱.۲.۰)

- `setup/MehraSetup.exe`: 89,584,984 بایت — sha256 `35bc006b2dc098ea2d44ca13fc3ea1ad17e03832b98c055eef1aa719cc8b66e9`
- `exe/Mehra.exe`: 94,294,920 بایت — sha256 `2b92b5201b8caf82a27b46e15e397ec53cb9bd820249297b9139586334c2f1d8`
- `exe/Mehra-win-x64.zip`: 41,851,303 بایت — sha256 `a7333ea4c5dcb0122dddd2e7d378369556fff583e456d2aa7e5dc6bc01dfb4a9`
- تأیید بایت‌به‌بایت: MZ/PE معتبر؛ رشته‌های برند به‌صورت UTF-16/UTF-8 داخل نصب‌گر موجود است: `www.meelano.ir`، `09120219593`، `Milad Yaghoobi`، «میلاد یعقوبی»، «مهرآ»، نسخهٔ ۱.۲.۰؛ لاگ ISCC وجود `SqlLocalDB.msi` را در فایل نصب تأیید می‌کند («Compressing: ...\redist\SqlLocalDB.msi»).

## نسخهٔ ۱.۲.۰ — «ترفند ساده»: فایل محلی، بدون SQL Server + اتصال اندروید از شبکه

- **حالت پیش‌فرض حالا «فایل محلی — بدون سرور» است:** همهٔ داده‌ها در یک فایل JSON (`%AppData%\\mehra\\mehra-local.json`) ذخیره می‌شوند؛ نیازی به SQL Server، LocalDB یا درایور OLE DB نیست و نصب‌گر روی SQL Server 2022 دیگر خطا نمی‌دهد.
- ورود پیش‌فرض: `admin` / `Admin@1234` (همان حساب قبلی، با هش SHA-256 در همان فایل).
- **سرور شبکهٔ محلی (LAN):** وقتی با حالت فایل محلی وارد می‌شوید، خود برنامه یک سرور HTTP کوچک روی `http://<آی‌پی-کامپیوتر>:5050` اجرا می‌کند (نشانی در نوار پایین برنامه نمایش داده می‌شود) و همان داده‌ها را با قرارداد `docs/API.md` (envelope `{success,data,error}`) سرو می‌کند: `auth/login`، `system/health`، `dashboard/summary`، `rooms`، `guests`، `reservations`، `reception/stays`، `notifications`، `housekeeping`، `maintenance`، `users`، `finance`، `reports`.
- **اتصال اندروید:** گوشی را به همان Wi-Fi وصل کنید؛ در اپ اندروید → تنظیمات → «نشانی سرور» مقدار `http://<آی‌پی-کامپیوتر>:5050` را بنویسید (مثلاً `http://192.168.1.20:5050`) و اتصال را تست کنید؛ ورود با `admin` / `Admin@1234`. داده‌های صفحه‌های اندروید همان دادهٔ فایل محلی PC است.
- عمليات نوشتن (ثبت رزرو/پرداخت/...) در نسخهٔ فایل محلی فقط‌خوانی است و با پیام فارسی «NOT_SUPPORTED» پاسخ داده می‌شود؛ برای ثبت دادهٔ جدید از خود برنامهٔ ویندوز استفاده کنید.
- نصب‌گر: مؤلفه‌های SQL (LocalDB/OLE DB/schema) حالا «اختیاری» و پیش‌فرض تیک‌خالی هستند؛ اگر تیک نخورند، `settings.json` با `Mode=local` نوشته می‌شود و اصلاً به SQL دست نمی‌زند.
- اگر LocalDB/SQL را می‌خواهید، مثل قبلی در صفحهٔ مؤلفه‌ها تیک بزنید — بقیهٔ کار همان است.

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
