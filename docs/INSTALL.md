# راهنمای نصب و ساخت — MEHRA

## 1. پیش‌نیازها

| ابزار | نسخه |
|---|---|
| JDK | 17 (Temurin/Microsoft/OpenJDK) — `JAVA_HOME` تنظیم شود |
| Android Studio | Ladybug (2024.2) یا جدیدتر |
| Android SDK Platform | 35 |
| Build-Tools | 35.0.0 |
| Gradle | از طریق wrapper (8.11.1) — نیازی به نصب جداگانه نیست |
| دستگاه/امولاتور | Android 7.0 (API 24) یا بالاتر |

`local.properties` در ریشه پروژه (ساخته نمی‌شود؛ باید دستی ایجاد شود):

```properties
sdk.dir=/path/to/Android/Sdk
```

## 2. ساخت

```bash
./gradlew :app:assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

```bash
./gradlew :app:assembleRelease
# بدون keystore.properties خروجی unsigned است
```

## 3. امضای نسخه ریلیز (اختیاری)

فایل `keystore.properties` در ریشه پروژه:

```properties
storeFile=mehra-release.jks
storePassword=********
keyAlias=mehra
keyPassword=********
```

فایل `.jks` و `keystore.properties` **نباید** در گیت ثبت شوند (در `.gitignore` قرار دارند).

## 4. نصب روی دستگاه

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 5. پیکربندی اولین اجرا

1. اپ باز می‌شود و صفحه «اتصال به سرور» نمایش داده می‌شود.
2. نشانی سرور را وارد کنید:
   - شبکه داخلی (LAN): `http://192.168.1.20:5080` — فقط در نسخه دیباگ HTTP مجاز است.
   - اینترنت/VPN: `https://mehra.example.ir`
3. «بررسی اتصال» وضعیت `GET /api/v1/system/health` را نشان می‌دهد و سپس «ذخیره نشانی».
4. ورود با نام کاربری و رمز صادرشده توسط مدیر سامانه.
5. در «تنظیمات» می‌توانید پوسته، واحد نمایش، قفل صفحه و قفل خودکار را تعیین کنید.

## 6. تنظیمات مهم نسخه

- `applicationId` = `ir.ahvaz.mehra` (دیباگ: `ir.ahvaz.mehra.debug`)
- `versionName` = `1.0.0`
- `allowBackup=false` و `dataExtractionRules` فعال — داده مهمان در بکاپ ابری قرار نمی‌گیرد.
- cleartext فقط در variant `debug` (فایل `app/src/debug/res/xml/network_security_config.xml`).

## 7. عیب‌یابی

| نشانه | علت محتمل | راه‌حل |
|---|---|---|
| «اتصال به سرور برقرار نشد» | آدرس اشتباه/فایروال/سرور خاموش | با `curl https://<host>/api/v1/system/health` تست کنید |
| خطای TLS در ریلیز | گواهی self-signed | گواهی معتبر نصب کنید یا CA داخلی را در trust store دستگاه قرار دهید |
| «دسترسی مجاز نیست» | نقش کاربر مجوز ندارد | مجوز را در سرور به نقش بدهید (کلاینت فقط دکمه را پنهان می‌کند) |
| عملیات در انتظار ارسال باقی می‌ماند | شبکه قطع یا توکن منقضی | در تنظیمات «ارسال عملیات در انتظار» را بزنید و دوباره وارد شوید |
| تصاویر خرابی نمایش داده نمی‌شوند | URL تصویر نسبت به سرور نادرست است | باید `imageUrls` مطلق یا نسبی به میزبان سرور برگردد |
