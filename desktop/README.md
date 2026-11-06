# MEHRA — نسخهٔ ویندوز (Desktop)

برنامهٔ رومیزی کارکنان مهمان‌پذیر برای ویندوز، متصل به همان API نسخهٔ اندروید (`docs/API.md`).

- **فریم‌ورک:** Avalonia UI 11.3 روی .NET 8 — خروجی build یک فایل اجرایی واقعی `Mehra.exe` است (win-x64، self-contained single-file).
- **ظاهر:** کاملاً فارسی و راست‌به‌چپ، فونت وزیرمتن، تاریخ جلالی، ارقام و پول فارسی (ریال)، تم لوکس سرمه‌ای/طلایی مثل نسخهٔ اندروید.
- **دو حالت:** اتصال به سرور واقعی (نشانی سرور + نام کاربری/رمز) و «ورود نمایشی» کاملاً آفلاین با دادهٔ نمونه (مشابه flavour demo اندروید — در حالت عملیاتی هیچ دادهٔ نمونه‌ای وجود ندارد).
- **صفحات (۱۲ مورد):** خانه، اتاق‌ها، رزروها، پذیرش، مهمانان، مالی، نظافت، تعمیرات، گزارش‌ها، کاربران، اعلانات، تنظیمات.
- **طراح و توسعه‌دهنده:** میلاد یعقوبی

## ساخت محلی (روی ویندوز، با نصب .NET 8 SDK)

```powershell
dotnet publish desktop/Mehra.Desktop/Mehra.Desktop.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o publish
# خروجی: publish\Mehra.exe
```

## ساخت خودکار (CI)

Workflow `.github/workflows/windows-ci.yml` روی `windows-latest` اجرا می‌شود و
`Mehra.exe` و `Mehra-win-x64.zip` را روی شاخهٔ `ci-artifacts` (مسیر `exe/`) منتشر می‌کند؛
ورکفلواو اندروئید APKها را با merge (نه force-push) به همان شاخه اضافه می‌کند تا فایل exe پاک نشود.

## ساختار پوشه‌ها

```
desktop/Mehra.Desktop/
  Program.cs / App.axaml(.cs)     نقطهٔ ورود و تم
  Styles/AppStyles.axaml           تم (سرمه‌ای/طلایی، کارت‌های گرد)
  Assets/Fonts/vazirmatn_*.ttf     فونت وزیرمتن
  Jalali.cs                        تبدیل جلالی (port 1:1 از کلاینت اندروید)
  PersianText.cs                   ارقام/پول/تاریخ فارسی
  Models.cs                        DTOهای API + مدل‌های نمایشی
  Services.cs                      IHotelApi + ApiClient (REST) + DemoApi (نمونهٔ آفلاین)
  ViewModels.cs                    منطق صفحات
  Views/                           LoginWindow، MainWindow، Pages/* (12 صفحه)
  Assets/mehra.ico                 آیکن برنامه (Archive sizes 256..16)
```
