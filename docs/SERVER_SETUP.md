# پیکربندی و الزامات سرور — MEHRA

> این سند **الزامات و قرارداد** سمت سرور است. در این مخزن کد بک‌اند پیاده‌سازی نشده است؛ کلاینت اندروید بر اساس این قرارداد نوشته شده و آماده اتصال به سرویس ASP.NET Core 10 + PostgreSQL است.

## 1. پشته پیشنهادی

- ASP.NET Core 10 (Web API، Controller-based یا Minimal API)
- PostgreSQL 16 + EF Core 9 (Npgsql)
- احراز هویت JWT (HS256 یا RS256) + Refresh Token چرخشی و قابل ابطال
- SignalR برای رویدادهای بلادرنگ
- Serilog → PostgreSQL/فایل برای Audit Log

## 2. الزامات امنیتی

| مورد | الزام |
|---|---|
| HTTPS | در Production اجباری؛ HSTS فعال؛ `UseHttpsRedirection` |
| JWT | عمر کوتاه access (۱۰–۱۵ دقیقه)، refresh چرخشی با ابطال توکن قبلی |
| دستگاه | اتصال refresh token به `X-Device-Id` و IP/UA برای امکان ابطال نشست |
| رمز عبور | PBKDF2/BCrypt/Argon2 با salt یکتا؛ هرگز plaintext |
| مجوزها | Authorization بر اساس Permission (نه فقط Role) روی هر endpoint |
| Audit | ثبت کاربر، زمان، IP، اقدام، موجودیت و شناسه برای همه عملیات حساس |
| Idempotency | ذخیره کلید و پاسخ برای عملیات مالی/پذیرش (حداقل ۷۲ ساعت) |
| Rate limit | محدودیت روی `/auth/login` و `/auth/refresh` (مثلاً ۱۰ درخواست در دقیقه به ازای IP) |
| CORS | فقط دامنه‌های لازم؛ اپ موبایل نیازی به CORS ندارد |
| لاگ | بدون توکن، رمز و اطلاعات هویتی مهمان؛ در صورت نیاز ماسک شود |
| دسترسی شبکه | سرویس بیرونی فقط از طریق VPN؛ پورت سرور در اینترنت عمومی باز نشود |

## 3. نقش‌ها و مجوزها

نقش‌ها: مدیر کل، مدیر مجموعه، سرپرست پذیرش، پذیرشگر، مالی، خدمات، تعمیرات، ناظر، حسابرس.

کدهای مجوز که کلاینت می‌شناسد (باید در `user.permissions` برگردند):

```
dashboard.view, rooms.view, rooms.manage, guests.view, guests.manage, guests.pii.view,
reservations.view, reservations.manage, reception.operate, finance.view, finance.operate,
housekeeping.view, housekeeping.manage, maintenance.view, maintenance.manage,
users.view, users.manage, reports.view, reports.export, audit.view
```

سرور مرجع نهایی است؛ کلاینت تنها برای پنهان‌کردن دکمه‌های غیرمجاز از همین فهرست استفاده می‌کند.

## 4. مدل داده پیشنهادی (خلاصه)

- `Users(Id, FullName, Username, PasswordHash, RoleId, Phone, IsActive, LastLoginAtUtc, DeviceIds)`
- `Roles(Id, Code, TitleFa)` + `RolePermissions(RoleId, Permission)`
- `RefreshTokens(Id, UserId, DeviceId, TokenHash, ExpiresAtUtc, RevokedAtUtc, ReplacedByTokenId)`
- `Rooms(Id, RoomNumber, Floor, Capacity, RoomTypeId, RatePerNightRial, Status, Notes, UpdatedAtUtc)`
- `RoomTypes`, `Amenities`, `RoomAmenities`
- `Guests(Id, FirstName, LastName, NationalId, Phone, City, Address, Notes, CreatedAtUtc)`
- `Companions(Id, GuestId, FullName, NationalId, Relation)`
- `Reservations(Id, Code, GuestId, RoomId, CheckInDate, CheckOutDate, GuestsCount, NightlyRateRial, TotalRial, DiscountRial, PaidRial, Status, Notes, CreatedByUserId, CreatedAtUtc)`
- `ReservationNights` یا قید EXCLUDE برای **جلوگیری قطعی از Double booking**
- `Stays(Id, ReservationId, RoomId, CheckInAtUtc, PlannedCheckOutDate, ActualCheckOutAtUtc, Status)`
- `Transactions(Id, Code, Type, AmountRial, Method, GuestId, ReservationId, StayId, Description, Reference, CreatedByUserId, CreatedAtUtc, IsReversed)`
- `Discounts`, `HousekeepingTasks`, `MaintenanceTickets`, `MaintenancePhotos`, `AuditLogs`, `IdempotencyRecords`

### جلوگیری از رزرو تکراری (توصیه قوی)

در PostgreSQL با افزونه `btree_gist`:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE "Reservations" ADD CONSTRAINT "no_overlap"
  EXCLUDE USING gist ("RoomId" WITH =, daterange("CheckInDate","CheckOutDate") WITH &&)
  WHERE ("Status" IN ('Pending','Confirmed','CheckedIn'));
```

قید پایگاه‌داده تضمین نهایی است؛ بررسی در لایه سرویس به‌تنهایی کافی نیست.

## 5. رفتار مورد انتظار برای قواعد کاری

| قاعده | انتظار سرور |
|---|---|
| رزرو | `checkOut > checkIn`، اتاق آزاد در بازه، ظرفیت اتاق ≥ نفرات |
| تأیید | فقط از `Pending` → `Confirmed`؛ در غیر این صورت `409 INVALID_STATE` |
| پذیرش | فقط رزرو `Confirmed`/`Pending` با اتاق تخصیص‌یافته؛ اتاق باید `Free`/`Reserved` باشد |
| خروج | اقامت فعال، تسویه ثبت‌شده؛ اتاق به `Cleaning` برود |
| جابه‌جایی اتاق | اتاق مقصد آزاد و هم‌بازه نبودن با رزرو دیگر |
| تمدید | بازه جدید با رزروهای اتاق تعارض نداشته باشد |
| تخفیف | ثبت در تراکنش از نوع `Discount` با کاربر و دلیل |
| پرداخت | هر پرداخت یک تراکنش با کد یکتا؛ برگشت، تراکنش `Refund` جداگانه |
| نظافت | چرخه `Pending → Assigned → InProgress → Done → Approved` و آزادسازی اتاق پس از تأیید |
| تعمیرات | آزادسازی اتاق فقط پس از `Resolved` و با تأیید مجاز |

## 6. SignalR

- Hub: `/hubs/notifications`، احراز هویت با همان JWT (query `access_token`).
- نام‌های پیشنهادی رویداد (کلاینت هر پیامی را بدون فرض ساختار نمایش می‌دهد): `reservation.created`, `reservation.cancelled`, `reception.checkedIn`, `reception.checkedOut`, `room.statusChanged`, `finance.payment`, `maintenance.alert`, `admin.alert`.
- هر پیام حداقل `{ title, body, kind, at }` داشته باشد.

## 7. پذیرش (Health)

`GET /api/v1/system/health` →

```json
{ "success": true, "data": { "serverVersion": "1.0.0", "apiVersion": "v1", "serverTime": "2025-03-05T10:15:30Z", "databaseConnected": true } }
```

اپلیکیشن از همین endpoint برای «بررسی اتصال» در اولین اجرا استفاده می‌کند.
