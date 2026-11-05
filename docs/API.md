# MEHRA REST API — قرارداد سرویس

اپلیکیشن اندروید **فقط** از طریق REST با سرور صحبت می‌کند؛ هیچ اتصال مستقیمی به PostgreSQL وجود ندارد.
این سند قرارداد مورد انتظار کلاینت است و از `data/remote/api/Apis.kt` تولید می‌شود؛ اگر سرور واقعی فیلدی متفاوت داشته باشد، DTO و Mapper سمت اپ باید مطابق پاسخ واقعی اصلاح شود (هیچ فیلدی کورکورانه فرض نشده است).

## 1. قالب پاسخ (Envelope)

```json
{
  "success": true,
  "data": { },
  "error": { "code": "RESERVATION_CONFLICT", "message": "اتاق در این بازه رزرو شده است.", "details": { "roomId": "..." } },
  "traceId": "9f1c…"
}
```

- موفق: `success=true` و `data` (ممکن است `null` باشد برای تأییدها).
- ناموفق: `success=false` و `error.code` ماشین‌خوان، `error.message` فارسیِ قابل نمایش، `error.details` می‌تواند نگاشت خطاهای فیلد باشد.
- `traceId` برای پیگیری در لاگ سرور؛ کلاینت آن را در پیام خطا نگه می‌دارد.

## 2. احراز هویت

| مورد | مقدار |
|---|---|
| هدر | `Authorization: Bearer <access_token>` |
| ورود | `POST /api/v1/auth/login` → `{ accessToken, refreshToken, accessExpiresAtUtc, refreshExpiresAtUtc, user }` |
| نوسازی | `POST /api/v1/auth/refresh` با `refreshToken`؛ **rotation**: توکن قبلی باطل می‌شود |
| ابطال | `POST /api/v1/auth/logout` توکن‌های همان دستگاه را باطل می‌کند |
| دستگاه | هدر `X-Device-Id` برای اتصال نشست به دستگاه |
| خطای ۴۰۱ | کلاینت یک‌بار refresh می‌کند؛ در صورت شکست، رویداد `SessionEvent.Expired` منتشر و کاربر به صفحه ورود هدایت می‌شود |
| خطای ۴۰۳ | پیام «دسترسی مجاز نیست» بدون تلاش مجدد |

## 3. Idempotency

عملیات مالی و پذیرش هدر `Idempotency-Key` (UUID v4) می‌فرستند. سرور باید:

1. کلید را به‌همراه پاسخ ذخیره کند (حداقل ۷۲ ساعت)،
2. در تکرار همان کلید، همان پاسخ اولیه را برگرداند (بدون اثر دوباره)،
3. در تکرار با بدنه متفاوت، `409` بدهد.

کلید **قبل از اولین تلاش** ساخته می‌شود و در بازپخش صف آفلاین دقیقاً همان مقدار ارسال می‌شود.

## 4. کدهای خطای پیشنهادی

| code | HTTP | معنا |
|---|---|---|
| `VALIDATION_FAILED` | 400/422 | خطای فیلد؛ `details` نگاشت فیلد→پیام |
| `UNAUTHORIZED` | 401 | توکن نامعتبر/منقضی |
| `FORBIDDEN` | 403 | نداشتن مجوز نقش |
| `NOT_FOUND` | 404 | موجودیت یافت نشد |
| `RESERVATION_CONFLICT` | 409 | تداخل بازه رزرو (Double booking) |
| `ROOM_NOT_AVAILABLE` | 409 | اتاق آزاد نیست |
| `INVALID_ROOM_STATE` | 409 | وضعیت اتاق اجازه عملیات نمی‌دهد |
| `STAY_NOT_ACTIVE` | 409 | اقامت فعال نیست |
| `IDEMPOTENCY_CONFLICT` | 409 | همان کلید با بدنه متفاوت |
| `RATE_LIMITED` | 429 | محدودیت نرخ درخواست |
| `TICKET_PHOTO_UNSUPPORTED` | 404/405 | سرور بارگذاری تصویر خرابی را ندارد |

کلاینت ۴۰۸/۴۲۹ و خطاهای شبکه را قابل‌تلاش‌مجدد می‌داند؛ سایر ۴xx رد قطعی و نیازمند اصلاح کاربر است.

## 5. زمان و تاریخ

- همه زمان‌ها **ISO-8601 UTC** (`2025-03-05T10:15:30Z`) و تاریخ‌های کاری `yyyy-MM-dd` در منطقه **Asia/Tehran**.
- نمایش همیشه شمسی است؛ تبدیل با پیاده‌سازی دقیق جلالی (`core/date/JalaliConverter`) و بدون وابستگی به الگوریتم‌های غیرمستند انجام می‌شود.
- مدت اقامت = تعداد شب بین ورود و خروج؛ خروج قبل از ورود در کلاینت و سرور رد می‌شود.

## 6. Realtime (SignalR)

- Hub: `/hubs/notifications` با نگوتیشن نسخه ۱ روی WebSocket.
- رویدادها: رزرو جدید/لغو، پذیرش، خروج، تغییر وضعیت اتاق، پرداخت، تعمیرات و هشدار مدیریتی.
- کلاینت پس از اتصال مجدد، داده‌های صفحه جاری را دوباره دریافت می‌کند (re-fetch) تا رویدادهای ازدست‌رفته جبران شوند.

## 7. فهرست Endpointها (تولیدشده از کد)

## AuthApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `login` | `AuthResponseDto` |
| POST | `/api/v1/auth/refresh` | `refresh` | `AuthResponseDto` |
| POST | `/api/v1/auth/logout` | `logout` | `JsonElement` |
| POST | `/api/v1/auth/change-password` | `changePassword` | `JsonElement` |
| GET | `/api/v1/auth/me` | `me` | `SessionUserDto` |

## SystemApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/system/health` | `health` | `ServerInfoDto` |
| GET | `/api/v1/notifications` | `notifications` | `List<NotificationDto>` |
| POST | `/api/v1/notifications/{id}/read` | `markRead` | `JsonElement` |
| POST | `/api/v1/notifications/read-all` | `markAllRead` | `JsonElement` |

## DashboardApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/dashboard/summary` | `summary` | `DashboardSummaryDto` |

## RoomsApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/rooms` | `list` | `PagedData<RoomDto>` |
| GET | `/api/v1/rooms/{id}` | `get` | `RoomDto` |
| GET | `/api/v1/rooms/lookups` | `lookups` | `RoomLookupsDto` |
| GET | `/api/v1/rooms/{id}/history` | `history` | `List<RoomHistoryDto>` |
| POST | `/api/v1/rooms` | `create` | `RoomDto` |
| PUT | `/api/v1/rooms/{id}` | `update` | `RoomDto` |
| PATCH | `/api/v1/rooms/{id}/status` | `changeStatus` | `RoomDto` |
| POST | `/api/v1/housekeeping/tasks` | `requestCleaning` | `HousekeepingTaskDto` |
| POST | `/api/v1/maintenance/tickets` | `createMaintenance` | `MaintenanceTicketDto` |

## GuestsApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/guests` | `list` | `PagedData<GuestDto>` |
| GET | `/api/v1/guests/{id}` | `get` | `GuestDetailDto` |
| POST | `/api/v1/guests` | `create` | `GuestDto` |
| PUT | `/api/v1/guests/{id}` | `update` | `GuestDto` |

## ReservationsApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/reservations` | `list` | `PagedData<ReservationDto>` |
| GET | `/api/v1/reservations/{id}` | `get` | `ReservationDto` |
| GET | `/api/v1/reservations/availability` | `availability` | `List<AvailableRoomDto>` |
| POST | `/api/v1/reservations/quote` | `quote` | `QuoteDto` |
| POST | `/api/v1/reservations` | `create` | `ReservationDto` |
| POST | `/api/v1/reservations/{id}/confirm` | `confirm` | `ReservationDto` |
| POST | `/api/v1/reservations/{id}/cancel` | `cancel` | `ReservationDto` |
| GET | `/api/v1/reservations/waitlist` | `waitlist` | `List<WaitlistEntryDto>` |
| POST | `/api/v1/reservations/waitlist/{id}/promote` | `promoteWaitlist` | `ReservationDto` |
| GET | `/api/v1/reservations/calendar` | `calendar` | `List<CalendarEntryDto>` |

## ReceptionApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/reception/reservations` | `searchReservations` | `List<ReservationDto>` |
| GET | `/api/v1/reception/stays` | `activeStays` | `List<StayDto>` |
| GET | `/api/v1/reception/stays/{id}` | `stay` | `StayDto` |
| POST | `/api/v1/reception/{reservationId}/check-in` | `checkIn` | `StayDto` |
| POST | `/api/v1/reception/stays/{stayId}/check-out` | `checkOut` | `ReceiptDto` |
| POST | `/api/v1/reception/stays/{stayId}/extend` | `extend` | `StayDto` |
| POST | `/api/v1/reception/stays/{stayId}/move-room` | `moveRoom` | `StayDto` |
| GET | `/api/v1/reception/stays/{stayId}/receipt` | `receipt` | `ReceiptDto` |
| GET | `/api/v1/reception/document-types` | `documentTypes` | `List<String>` |

## FinanceApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/finance/transactions` | `transactions` | `PagedData<TransactionDto>` |
| GET | `/api/v1/finance/cashbox` | `cashBox` | `CashBoxDto` |
| GET | `/api/v1/finance/summary` | `summary` | `FinanceSummaryDto` |
| GET | `/api/v1/finance/debtors` | `debtors` | `List<DebtorDto>` |
| POST | `/api/v1/finance/transactions` | `register` | `TransactionDto` |
| POST | `/api/v1/finance/discounts` | `discount` | `TransactionDto` |

## HousekeepingApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/housekeeping/tasks` | `tasks` | `List<HousekeepingTaskDto>` |
| GET | `/api/v1/housekeeping/staff` | `staff` | `List<StaffDto>` |
| POST | `/api/v1/housekeeping/tasks` | `create` | `HousekeepingTaskDto` |
| POST | `/api/v1/housekeeping/tasks/{id}/assign` | `assign` | `HousekeepingTaskDto` |
| POST | `/api/v1/housekeeping/tasks/{id}/start` | `start` | `HousekeepingTaskDto` |
| POST | `/api/v1/housekeeping/tasks/{id}/complete` | `complete` | `HousekeepingTaskDto` |
| POST | `/api/v1/housekeeping/tasks/{id}/approve` | `approve` | `HousekeepingTaskDto` |
| GET | `/api/v1/housekeeping/performance` | `performance` | `List<PerformanceDto>` |

## MaintenanceApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/maintenance/tickets` | `tickets` | `List<MaintenanceTicketDto>` |
| GET | `/api/v1/maintenance/tickets/{id}` | `ticket` | `MaintenanceTicketDto` |
| GET | `/api/v1/maintenance/technicians` | `technicians` | `List<StaffDto>` |
| POST | `/api/v1/maintenance/tickets` | `create` | `MaintenanceTicketDto` |
| POST | `/api/v1/maintenance/tickets/{id}/assign` | `assign` | `MaintenanceTicketDto` |
| PATCH | `/api/v1/maintenance/tickets/{id}/status` | `updateStatus` | `MaintenanceTicketDto` |
| POST | `/api/v1/maintenance/tickets/{id}/cost` | `registerCost` | `MaintenanceTicketDto` |
| POST | `/api/v1/maintenance/tickets/{id}/release-room` | `releaseRoom` | `MaintenanceTicketDto` |
| POST | `/api/v1/maintenance/tickets/{id}/photos` | `uploadPhotos` | `List<String>` |

## UsersApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/users` | `list` | `PagedData<AppUserDto>` |
| GET | `/api/v1/roles` | `roles` | `List<RoleDto>` |
| POST | `/api/v1/users` | `create` | `AppUserDto` |
| PUT | `/api/v1/users/{id}` | `update` | `AppUserDto` |
| PATCH | `/api/v1/users/{id}/active` | `setActive` | `AppUserDto` |
| POST | `/api/v1/users/{id}/reset-password` | `resetPassword` | `JsonElement` |
| GET | `/api/v1/audit-logs` | `auditLogs` | `List<AuditLogDto>` |

## ReportsApi

| Method | Path | Kotlin function | Response body (`data`) |
|---|---|---|---|
| GET | `/api/v1/reports/{key}` | `report` | `ReportDto` |
## 8. نکته درباره بارگذاری تصویر خرابی

`POST /api/v1/maintenance/tickets/{id}/photos` با `multipart/form-data` و فیلد تکراری `photos` است و آرایه‌ای از URL تصاویر ذخیره‌شده را برمی‌گرداند.
اگر سرور این endpoint را نداشته باشد، خودِ گزارش خرابی ثبت می‌شود و UI پیام «گزارش ثبت شد، اما بارگذاری تصاویر انجام نشد» را نشان می‌دهد (هیچ‌گاه موفقیت جعلی گزارش نمی‌شود).
