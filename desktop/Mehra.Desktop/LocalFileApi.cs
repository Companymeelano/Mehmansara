using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Mehra.Desktop;

/// <summary>
/// Local-file mode ("ترفند ساده"): ALL hotel data lives in ONE JSON file
/// (%AppData%\mehra\mehra-local.json) — no SQL Server, no driver, no network needed.
/// Seeded once with sample data + the admin account; every change is saved back to the file.
/// The same store is served over LAN by <see cref="LanServer"/> so the Android app can
/// connect when the phone is on the same network.
/// </summary>
public class LocalUser
{
    [JsonPropertyName("id")] public string Id { get; set; } = "";
    [JsonPropertyName("fullName")] public string FullName { get; set; } = "";
    [JsonPropertyName("username")] public string Username { get; set; } = "";
    [JsonPropertyName("passwordHash")] public string PasswordHash { get; set; } = "";
    [JsonPropertyName("salt")] public string Salt { get; set; } = "";
    [JsonPropertyName("roleCode")] public string RoleCode { get; set; } = "";
    [JsonPropertyName("roleTitle")] public string RoleTitle { get; set; } = "";
    [JsonPropertyName("phone")] public string? Phone { get; set; }
    [JsonPropertyName("isActive")] public bool IsActive { get; set; } = true;
    [JsonPropertyName("lastLoginAtUtc")] public string? LastLoginAtUtc { get; set; }
}

public class LocalStore
{
    [JsonPropertyName("users")] public List<LocalUser> Users { get; set; } = new();
    [JsonPropertyName("rooms")] public List<RoomDto> Rooms { get; set; } = new();
    [JsonPropertyName("guests")] public List<GuestDto> Guests { get; set; } = new();
    [JsonPropertyName("reservations")] public List<ReservationDto> Reservations { get; set; } = new();
    [JsonPropertyName("stays")] public List<StayDto> Stays { get; set; } = new();
    [JsonPropertyName("transactions")] public List<TransactionDto> Transactions { get; set; } = new();
    [JsonPropertyName("housekeeping")] public List<HousekeepingTaskDto> Housekeeping { get; set; } = new();
    [JsonPropertyName("maintenance")] public List<MaintenanceTicketDto> Maintenance { get; set; } = new();
    [JsonPropertyName("notifications")] public List<NotificationDto> Notifications { get; set; } = new();

    private static readonly object IoLock = new();

    public static string FilePath()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "mehra");
        Directory.CreateDirectory(dir);
        return Path.Combine(dir, "mehra-local.json");
    }

    public static string Hash(string salt, string password) =>
        Convert.ToHexString(SHA256.HashData(Encoding.Unicode.GetBytes(salt + password))).ToLowerInvariant();

    public static LocalStore Load()
    {
        lock (IoLock)
        {
            try
            {
                var f = FilePath();
                if (File.Exists(f))
                {
                    var s = JsonSerializer.Deserialize<LocalStore>(File.ReadAllText(f));
                    if (s != null) return s;
                }
            }
            catch { /* fall through to seed */ }
            var seeded = Seed();
            Save(seeded);
            return seeded;
        }
    }

    public static void Save(LocalStore s)
    {
        lock (IoLock)
        {
            try
            {
                var f = FilePath();
                var tmp = f + ".tmp";
                File.WriteAllText(tmp, JsonSerializer.Serialize(s, new JsonSerializerOptions { WriteIndented = true }));
                File.Move(tmp, f, true);
            }
            catch { /* non-fatal */ }
        }
    }

    private static LocalStore Seed()
    {
        var today = DateTime.Today;
        var iso = DateTime.UtcNow.ToString("o");
        var s = new LocalStore();
        s.Users.Add(new LocalUser
        {
            Id = "u1", FullName = "سید مرتضی موسوی", Username = "admin",
            Salt = "mehra-local", PasswordHash = Hash("mehra-local", "Admin@1234"),
            RoleCode = "ADMIN", RoleTitle = "مدیر کل", Phone = "09120000010", IsActive = true
        });
        s.Users.Add(new LocalUser
        {
            Id = "u2", FullName = "مامور رضائی", Username = "reception",
            Salt = "mehra-local", PasswordHash = Hash("mehra-local", "Admin@1234"),
            RoleCode = "RECEPTION", RoleTitle = "پذیرش", Phone = "09120000011", IsActive = true
        });
        s.Rooms.AddRange(new[]
        {
            new RoomDto { Id = "r1", RoomNumber = "101", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "FREE" },
            new RoomDto { Id = "r2", RoomNumber = "102", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "FREE" },
            new RoomDto { Id = "r3", RoomNumber = "103", Floor = 1, Capacity = 4, RoomTypeName = "چهارتخته", RatePerNightRial = 4_200_000, Status = "FREE" },
            new RoomDto { Id = "r4", RoomNumber = "104", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "FREE" },
            new RoomDto { Id = "r5", RoomNumber = "201", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
            new RoomDto { Id = "r6", RoomNumber = "202", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
            new RoomDto { Id = "r7", RoomNumber = "203", Floor = 2, Capacity = 3, RoomTypeName = "سه‌تخته", RatePerNightRial = 3_300_000, Status = "FREE" },
            new RoomDto { Id = "r8", RoomNumber = "301", Floor = 3, Capacity = 4, RoomTypeName = "سوئیت", RatePerNightRial = 6_800_000, Status = "FREE" },
        });
        return s;
    }
}

/// <summary>
/// IHotelApi backed by the local JSON file — the Windows app needs NO SQL Server at all.
/// </summary>
public class LocalFileApi : IHotelApi
{
    private readonly LocalStore _store;
    private string _operator = "مدیر";

    /// <summary>Guards every read/mutation of the shared in-memory store (LAN server + UI can run concurrently).</summary>
    public static readonly object Gate = new();

    public LocalFileApi() { _store = LocalStore.Load(); }

    public bool IsDemo => false;
    public string ServerLabel => "فایل محلی — بدون سرور";
    public string DataFile
    {
        get
        {
            lock (Gate)
            {
                return LocalStore.FilePath();
            }
        }
    }
    public LocalStore Store => _store;

    public Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password)
    {
        lock (Gate)
        {
            var u = _store.Users.FirstOrDefault(x => x.Username.Equals(username.Trim(), StringComparison.OrdinalIgnoreCase));
            if (u is null || !u.IsActive)
                return Task.FromResult(Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است."));
            if (LocalStore.Hash(u.Salt, password) != u.PasswordHash)
                return Task.FromResult(Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است."));
            u.LastLoginAtUtc = DateTime.UtcNow.ToString("o");
            _operator = u.FullName;
            LocalStore.Save(_store);
            return Task.FromResult(Result<SessionUser>.Good(new SessionUser
            {
                FullName = u.FullName,
                Username = u.Username,
                RoleTitle = string.IsNullOrEmpty(u.RoleTitle) ? u.RoleCode : u.RoleTitle
            }));
        }
    }

    public Task<Result<string>> HealthAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<string>.Good("فایل محلی در دسترس است — بدون سرور (" + DataFile + ")"));
            }
        }

    public Task<Result<List<RoomDto>>> RoomsAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<RoomDto>>.Good(_store.Rooms.OrderBy(r => r.RoomNumber).ToList()));
            }
        }

    public Task<Result<List<ReservationDto>>> ReservationsAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<ReservationDto>>.Good(_store.Reservations.OrderByDescending(r => r.CheckInDate).Take(200).ToList()));
            }
        }

    public Task<Result<List<StayDto>>> StaysAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<StayDto>>.Good(_store.Stays.OrderByDescending(s => s.CheckInAtUtc).ToList()));
            }
        }

    public Task<Result<List<GuestDto>>> GuestsAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<GuestDto>>.Good(_store.Guests.OrderBy(g => g.LastName).ThenBy(g => g.FirstName).ToList()));
            }
        }

    public Task<Result<List<HousekeepingTaskDto>>> HousekeepingAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<HousekeepingTaskDto>>.Good(_store.Housekeeping.OrderByDescending(h => h.CreatedAtUtc).ToList()));
            }
        }

    public Task<Result<List<MaintenanceTicketDto>>> MaintenanceAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<MaintenanceTicketDto>>.Good(_store.Maintenance.OrderByDescending(m => m.CreatedAtUtc).ToList()));
            }
        }

    public Task<Result<List<AppUserDto>>> UsersAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<AppUserDto>>.Good(_store.Users.OrderBy(u => u.FullName).Select(u => new AppUserDto
                {
                Id = u.Id, FullName = u.FullName, Username = u.Username, RoleCode = u.RoleCode,
                RoleTitle = u.RoleTitle, Phone = u.Phone, IsActive = u.IsActive, LastLoginAtUtc = u.LastLoginAtUtc
                }).ToList()));
            }
        }

    public Task<Result<List<NotificationDto>>> NotificationsAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<NotificationDto>>.Good(_store.Notifications.OrderByDescending(n => n.CreatedAtUtc).ToList()));
            }
        }

    public Task<Result<string>> MarkAllNotificationsReadAsync()
    {
        lock (Gate)
        {
            foreach (var n in _store.Notifications) n.IsRead = true;
            LocalStore.Save(_store);
            return Task.FromResult(Result<string>.Good("همهٔ اعلان‌ها خوانده شدند."));
        }
    }

    public Task<Result<List<DebtorDto>>> DebtorsAsync()
        {
            lock (Gate)
            {
                return Task.FromResult(Result<List<DebtorDto>>.Good(_store.Guests
                .Where(g => (g.BalanceRial ?? 0) > 0)
                .OrderByDescending(g => g.BalanceRial)
                .Select(g => new DebtorDto
                {
                GuestId = g.Id,
                GuestName = ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim(),
                Phone = g.Phone,
                RoomNumber = _store.Stays.FirstOrDefault(s => s.Status == "IN_HOUSE" && s.GuestName == ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim())?.RoomNumber,
                BalanceRial = g.BalanceRial,
                LastActivityAtUtc = g.LastStayAtUtc
                }).ToList()));
            }
        }

    public Task<Result<List<TransactionDto>>> TransactionsAsync(string fromIso, string toIso)
    {
        lock (Gate)
        {
            var f = DateOnly.TryParse(fromIso, out var fd) ? fd : DateOnly.FromDateTime(DateTime.Today.AddDays(-30));
            var t = DateOnly.TryParse(toIso, out var td) ? td : DateOnly.FromDateTime(DateTime.Today);
            var list = _store.Transactions
                .Where(x => DateOnly.TryParse(x.CreatedAtUtc is { Length: >= 10 } c ? c[..10] : "", out var d) && d >= f && d <= t)
                .OrderByDescending(x => x.CreatedAtUtc)
                .Take(100).ToList();
            return Task.FromResult(Result<List<TransactionDto>>.Good(list));
        }
    }

    public Task<Result<CashBoxDto>> CashboxAsync(string dateIso)
    {
        lock (Gate)
        {
            var day = DateOnly.TryParse(dateIso, out var d) ? d : DateOnly.FromDateTime(DateTime.Today);
            var todays = _store.Transactions.Where(x =>
                x.CreatedAtUtc is { Length: >= 10 } c && DateOnly.TryParse(c[..10], out var dd) && dd == day && x.IsReversed != true).ToList();
            var box = new CashBoxDto
            {
                Date = dateIso,
                ReceivedRial = todays.Where(x => x.Type == "Receive").Sum(x => x.AmountRial ?? 0),
                PaidRial = todays.Where(x => x.Type == "Pay").Sum(x => x.AmountRial ?? 0),
                DiscountsRial = todays.Where(x => x.Type == "Discount").Sum(x => x.AmountRial ?? 0),
                NetRial = todays.Sum(x => x.Type is "Receive" or "Discount" ? (x.AmountRial ?? 0) : -(x.AmountRial ?? 0)),
                TransactionCount = todays.Count,
                OpenBalanceRial = _store.Guests.Sum(g => g.BalanceRial ?? 0),
                ByMethod = todays.Where(x => x.Type is "Receive" or "Discount")
                    .GroupBy(x => x.Method ?? "")
                    .Select(g => new MethodTotalDto { Method = g.Key, AmountRial = g.Sum(x => x.AmountRial ?? 0), Count = g.Count() })
                    .ToList()
            };
            return Task.FromResult(Result<CashBoxDto>.Good(box));
        }
    }

    public Task<Result<DashboardDto>> DashboardAsync()
    {
        lock (Gate)
        {
            var today = DateTime.Today;
            var todayIso = today.ToString("yyyy-MM-dd");
            var d = new DashboardDto { BusinessDate = todayIso };
            d.TotalRooms = _store.Rooms.Count;
            d.FreeRooms = _store.Rooms.Count(r => r.Status == "FREE");
            d.ReservedRooms = _store.Rooms.Count(r => r.Status == "RESERVED");
            d.OccupiedRooms = _store.Rooms.Count(r => r.Status == "OCCUPIED");
            d.CleaningRooms = _store.Rooms.Count(r => r.Status == "CLEANING");
            d.MaintenanceRooms = _store.Rooms.Count(r => r.Status == "MAINTENANCE");
            d.OutOfServiceRooms = _store.Rooms.Count(r => r.Status == "OUT_OF_SERVICE");
            d.TodayCheckIns = _store.Reservations.Count(r => r.CheckInDate == todayIso);
            d.TodayCheckOuts = _store.Reservations.Count(r => r.CheckOutDate == todayIso);
            d.RevenueTodayRial = _store.Transactions
                .Where(x => x.Type == "Receive" && x.IsReversed != true && x.CreatedAtUtc is { Length: >= 10 } c && c[..10] == todayIso)
                .Sum(x => x.AmountRial ?? 0);
            d.OccupancyPercent = d.TotalRooms == 0 ? 0 : Math.Round(100.0 * (d.OccupiedRooms + d.ReservedRooms) / d.TotalRooms, 1);
            d.OccupancySeries = new List<OccupancyPointDto>();
            for (int i = 6; i >= 0; i--)
            {
                var day = today.AddDays(-i).ToString("yyyy-MM-dd");
                var occ = _store.Reservations.Count(r =>
                    (r.Status == "CONFIRMED" || r.Status == "CHECKED_IN") &&
                    string.CompareOrdinal(r.CheckInDate, day) <= 0 && string.CompareOrdinal(r.CheckOutDate, day) > 0);
                d.OccupancySeries.Add(new OccupancyPointDto { Date = day, Occupied = occ, Total = d.TotalRooms });
            }
            d.Alerts = new List<DashboardAlertDto>();
            foreach (var r in _store.Rooms.Where(r => r.Status == "MAINTENANCE").Take(5))
                d.Alerts.Add(new DashboardAlertDto { Title = "اتاق " + Fa.Digits(r.RoomNumber ?? "") + " در حال تعمیر است", Message = "تیکت تعمیرات را بررسی کنید", Severity = "HIGH" });
            foreach (var r in _store.Rooms.Where(r => r.Status == "CLEANING").Take(5))
                d.Alerts.Add(new DashboardAlertDto { Title = "نظافت اتاق " + Fa.Digits(r.RoomNumber ?? ""), Message = "پس از اتمام، وضعیت اتاق را به «خالی» تغییر دهید", Severity = "MEDIUM" });
            d.RecentActivities = _store.Notifications.OrderByDescending(n => n.CreatedAtUtc).Take(10)
                .Select(n => new DashboardActivityDto { Title = n.Title ?? "", CreatedAtUtc = n.CreatedAtUtc }).ToList();
            return Task.FromResult(Result<DashboardDto>.Good(d));
        }
    }

    public Task<Result<ReportDto>> ReportAsync(string key, string fromIso, string toIso)
    {
        lock (Gate)
        {
            var rep = new ReportDto { Type = key, From = fromIso, To = toIso, GeneratedAtUtc = DateTime.UtcNow.ToString("o") };
            if (!DateTime.TryParse(fromIso, out var from) || !DateTime.TryParse(toIso, out var to) || from > to)
                return Task.FromResult(Result<ReportDto>.Fail("بازهٔ تاریخ نامعتبر است."));
            switch (key)
            {
                case "occupancy":
                    rep.Title = "گزارش اشغال اتاق‌ها"; rep.Columns = new() { "تاریخ", "اشغال", "کل" };
                    rep.Rows = new();
                    for (var d = from; d <= to; d = d.AddDays(1))
                    {
                        var day = d.ToString("yyyy-MM-dd");
                        var occ = _store.Reservations.Count(r =>
                            (r.Status == "CONFIRMED" || r.Status == "CHECKED_IN") &&
                            string.CompareOrdinal(r.CheckInDate, day) <= 0 && string.CompareOrdinal(r.CheckOutDate, day) > 0);
                        rep.Rows.Add(new() { day, occ.ToString(), _store.Rooms.Count.ToString() });
                    }
                    break;
                case "check-in-out":
                    rep.Title = "گزارش ورود و خروج"; rep.Columns = new() { "کد", "مهمان", "اتاق", "ورود", "خروج", "وضعیت" };
                    rep.Rows = _store.Reservations
                        .Where(r => (r.CheckInDate ?? "").CompareTo(fromIso) >= 0 && (r.CheckInDate ?? "").CompareTo(toIso) <= 0
                                 || (r.CheckOutDate ?? "").CompareTo(fromIso) >= 0 && (r.CheckOutDate ?? "").CompareTo(toIso) <= 0)
                        .OrderBy(r => r.CheckInDate)
                        .Select(r => new List<string> { r.Code ?? "", r.GuestName ?? "", r.RoomNumber ?? "", r.CheckInDate ?? "", r.CheckOutDate ?? "", r.Status ?? "" }).ToList();
                    break;
                case "reservations":
                    rep.Title = "گزارش رزروها"; rep.Columns = new() { "کد", "مهمان", "اتاق", "ورود", "خروج", "مبلغ", "وضعیت" };
                    rep.Rows = _store.Reservations.OrderByDescending(r => r.CheckInDate)
                        .Take(200)
                        .Select(r => new List<string> { r.Code ?? "", r.GuestName ?? "", r.RoomNumber ?? "", r.CheckInDate ?? "", r.CheckOutDate ?? "", (r.TotalRial ?? 0).ToString(), r.Status ?? "" }).ToList();
                    break;
                case "revenue":
                    rep.Title = "گزارش درآمد"; rep.Columns = new() { "روش پرداخت", "تعداد", "مبلغ (ریال)" };
                    rep.Rows = _store.Transactions
                        .Where(x => x.IsReversed != true && x.Type is "Receive" or "Discount" && InRange(x.CreatedAtUtc, fromIso, toIso))
                        .GroupBy(x => x.Method ?? "")
                        .Select(g => new List<string> { g.Key, g.Count().ToString(), g.Sum(x => x.AmountRial ?? 0).ToString() }).ToList();
                    rep.Totals = new() { "جمع دریافتی بازه: " + _store.Transactions
                        .Where(x => x.IsReversed != true && x.Type is "Receive" or "Discount" && InRange(x.CreatedAtUtc, fromIso, toIso))
                        .Sum(x => x.AmountRial ?? 0).ToString() };
                    break;
                case "debtors":
                    rep.Title = "گزارش بدهکاران"; rep.Columns = new() { "مهمان", "تلفن", "مانده (ریال)" };
                    rep.Rows = _store.Guests.Where(g => (g.BalanceRial ?? 0) > 0).OrderByDescending(g => g.BalanceRial)
                        .Select(g => new List<string> { ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim(), g.Phone ?? "", (g.BalanceRial ?? 0).ToString() }).ToList();
                    break;
                case "staff-performance":
                    rep.Title = "گزارش عملکرد کارکنان"; rep.Columns = new() { "کاربر", "تعداد تراکنش", "مجموع (ریال)" };
                    rep.Rows = _store.Transactions
                        .Where(x => x.IsReversed != true && InRange(x.CreatedAtUtc, fromIso, toIso))
                        .GroupBy(x => x.OperatorName ?? "")
                        .Select(g => new List<string> { g.Key, g.Count().ToString(), g.Sum(x => x.AmountRial ?? 0).ToString() }).ToList();
                    break;
                case "housekeeping":
                    rep.Title = "گزارش خدمات"; rep.Columns = new() { "اتاق", "نوع", "وضعیت", "مسئول" };
                    rep.Rows = _store.Housekeeping.OrderByDescending(h => h.CreatedAtUtc)
                        .Select(h => new List<string> { h.RoomNumber ?? "-", h.Type ?? "", h.Status ?? "", h.AssignedToName ?? "-" }).ToList();
                    break;
                case "maintenance":
                    rep.Title = "گزارش تعمیرات"; rep.Columns = new() { "کد", "عنوان", "اتاق", "اولویت", "وضعیت", "هزینه" };
                    rep.Rows = _store.Maintenance.OrderByDescending(m => m.CreatedAtUtc)
                        .Select(m => new List<string> { m.Code ?? "", m.Title ?? "", m.RoomNumber ?? "-", m.Priority ?? "", m.Status ?? "", m.CostRial?.ToString() ?? "-" }).ToList();
                    break;
                default:
                    return Task.FromResult(Result<ReportDto>.Fail("نوع گزارش ناشناخته: " + key));
            }
            return Task.FromResult(Result<ReportDto>.Good(rep));
        }
    }

    private static bool InRange(string? createdAtUtc, string fromIso, string toIso) =>
        createdAtUtc is { Length: >= 10 } c && string.CompareOrdinal(c[..10], fromIso) >= 0 && string.CompareOrdinal(c[..10], toIso) <= 0;

    // ================= write operations (v1.2.1) =================

    private string NewCode(string prefix, List<string?> codes)
    {
        var max = 1000;
        foreach (var c in codes)
            if (c != null && c.StartsWith(prefix) && int.TryParse(c[prefix.Length..], out var n) && n > max)
                max = n;
        return prefix + (max + 1);
    }

    public Task<Result<GuestDto>> AddGuestAsync(string firstName, string lastName, string? phone, string? city)
    {
        lock (Gate)
        {
            if (string.IsNullOrWhiteSpace(lastName) && string.IsNullOrWhiteSpace(firstName))
                return Task.FromResult(Result<GuestDto>.Fail("نام یا نام خانوادگی را وارد کنید."));
            var g = new GuestDto
            {
                Id = "g" + Guid.NewGuid().ToString("N")[..8],
                FirstName = firstName.Trim(),
                LastName = lastName.Trim(),
                Phone = string.IsNullOrWhiteSpace(phone) ? null : phone.Trim(),
                City = string.IsNullOrWhiteSpace(city) ? null : city.Trim(),
                BalanceRial = 0,
                TotalStays = 0,
                LastStayAtUtc = null
            };
            _store.Guests.Add(g);
            LocalStore.Save(_store);
            return Task.FromResult(Result<GuestDto>.Good(g));
        }
    }

    public Task<Result<ReservationDto>> AddReservationAsync(string guestName, string? guestPhone, string roomNumber, string checkInIso, string checkOutIso, int guestsCount, long totalRial, long paidRial)
    {
        lock (Gate)
        {
            if (string.IsNullOrWhiteSpace(guestName))
                return Task.FromResult(Result<ReservationDto>.Fail("نام مهمان را وارد کنید."));
            if (!DateTime.TryParse(checkInIso, out var ci) || !DateTime.TryParse(checkOutIso, out var co) || co <= ci)
                return Task.FromResult(Result<ReservationDto>.Fail("بازهٔ تاریخ نامعتبر است (ورود باید قبل از خروج باشد)."));
            var room = _store.Rooms.FirstOrDefault(r => r.RoomNumber == roomNumber.Trim());
            if (room is null)
                return Task.FromResult(Result<ReservationDto>.Fail("اتاق «" + roomNumber + "» یافت نشد."));
            var ciIso = ci.ToString("yyyy-MM-dd");
            var coIso = co.ToString("yyyy-MM-dd");
            var overlap = _store.Reservations.Any(x =>
                x.RoomNumber == room.RoomNumber &&
                (x.Status == "CONFIRMED" || x.Status == "CHECKED_IN" || x.Status == "PENDING") &&
                string.CompareOrdinal(x.CheckInDate, coIso) < 0 && string.CompareOrdinal(x.CheckOutDate, ciIso) > 0);
            if (overlap)
                return Task.FromResult(Result<ReservationDto>.Fail("اتاق " + roomNumber + " در این بازه رزرو شده است."));
            var r = new ReservationDto
            {
                Id = "v" + Guid.NewGuid().ToString("N")[..8],
                Code = NewCode("R-", _store.Reservations.Select(x => x.Code).ToList()),
                GuestName = guestName.Trim(),
                GuestPhone = string.IsNullOrWhiteSpace(guestPhone) ? null : guestPhone.Trim(),
                RoomNumber = room.RoomNumber,
                CheckInDate = ciIso,
                CheckOutDate = coIso,
                GuestsCount = guestsCount < 1 ? 1 : guestsCount,
                TotalRial = totalRial,
                PaidRial = paidRial,
                Status = paidRial > 0 ? "CONFIRMED" : "PENDING"
            };
            _store.Reservations.Add(r);
            if (r.Status == "CONFIRMED" && room.Status == "FREE") room.Status = "RESERVED";
            LocalStore.Save(_store);
            return Task.FromResult(Result<ReservationDto>.Good(r));
        }
    }

    public Task<Result<TransactionDto>> AddTransactionAsync(string type, long amountRial, string method, string? description, string? guestName, string? roomNumber)
    {
        lock (Gate)
        {
            if (amountRial <= 0)
                return Task.FromResult(Result<TransactionDto>.Fail("مبلغ باید بزرگ‌تر از صفر باشد."));
            var t = new TransactionDto
            {
                Id = "t" + Guid.NewGuid().ToString("N")[..8],
                Code = NewCode("TX-", _store.Transactions.Select(x => x.Code).ToList()),
                Type = string.IsNullOrWhiteSpace(type) ? "Receive" : type.Trim().ToUpperInvariant() switch { "PAY" => "Pay", "DISCOUNT" => "Discount", "REFUND" => "Refund", _ => "Receive" },
                AmountRial = amountRial,
                Method = string.IsNullOrWhiteSpace(method) ? "Cash" : method.Trim(),
                Description = description,
                GuestName = guestName,
                RoomNumber = roomNumber,
                OperatorName = _operator,
                CreatedAtUtc = DateTime.UtcNow.ToString("o"),
                IsReversed = false
            };
            _store.Transactions.Add(t);
            var gname = ((guestName ?? "").Trim());
            if (gname.Length > 0)
            {
                var g = _store.Guests.FirstOrDefault(x => ((x.FirstName ?? "") + " " + (x.LastName ?? "")).Trim().Contains(gname) || (x.LastName ?? "") == gname);
                if (g != null)
                {
                    if (t.Type == "Receive" || t.Type == "Discount") g.BalanceRial = Math.Max(0, (g.BalanceRial ?? 0) - amountRial);
                    else if (t.Type == "Pay") g.BalanceRial = (g.BalanceRial ?? 0) + amountRial;
                }
            }
            LocalStore.Save(_store);
            return Task.FromResult(Result<TransactionDto>.Good(t));
        }
    }

    public Task<Result<StayDto>> CheckInAsync(string reservationId)
    {
        lock (Gate)
        {
            var res = _store.Reservations.FirstOrDefault(r => r.Id == reservationId || r.Code == reservationId);
            if (res is null) return Task.FromResult(Result<StayDto>.Fail("رزرو یافت نشد: " + reservationId));
            if (res.Status == "CHECKED_IN") return Task.FromResult(Result<StayDto>.Fail("این رزرو قبلاً پذیرش شده است."));
            if (res.Status == "CANCELLED" || res.Status == "CHECKED_OUT") return Task.FromResult(Result<StayDto>.Fail("رزرو در وضعیت «" + res.Status + "» قابل پذیرش نیست."));
            var stay = new StayDto
            {
                Id = "s" + Guid.NewGuid().ToString("N")[..8],
                ReservationCode = res.Code,
                GuestName = res.GuestName,
                RoomNumber = res.RoomNumber,
                CheckInAtUtc = DateTime.UtcNow.ToString("o"),
                PlannedCheckOut = res.CheckOutDate,
                GuestsCount = res.GuestsCount,
                Status = "IN_HOUSE"
            };
            _store.Stays.Add(stay);
            res.Status = "CHECKED_IN";
            var room = _store.Rooms.FirstOrDefault(r => r.RoomNumber == res.RoomNumber);
            if (room != null) { room.Status = "OCCUPIED"; room.CurrentGuestName = res.GuestName; }
            LocalStore.Save(_store);
            return Task.FromResult(Result<StayDto>.Good(stay));
        }
    }

    public Task<Result<StayDto>> CheckOutAsync(string stayId, long finalPaymentRial, string method)
    {
        lock (Gate)
        {
            var stay = _store.Stays.FirstOrDefault(s => s.Id == stayId || s.ReservationCode == stayId);
            if (stay is null) return Task.FromResult(Result<StayDto>.Fail("اقامت فعال یافت نشد: " + stayId));
            if (stay.Status != "IN_HOUSE") return Task.FromResult(Result<StayDto>.Fail("این اقامت قبلاً تسویه شده است."));
            stay.Status = "CHECKED_OUT";
            var room = _store.Rooms.FirstOrDefault(r => r.RoomNumber == stay.RoomNumber);
            if (room != null) { room.Status = "CLEANING"; room.CurrentGuestName = null; }
            var res = _store.Reservations.FirstOrDefault(r => r.Code == stay.ReservationCode);
            if (res != null) res.Status = "CHECKED_OUT";
            if (finalPaymentRial > 0)
            {
                _store.Transactions.Add(new TransactionDto
                {
                    Id = "t" + Guid.NewGuid().ToString("N")[..8],
                    Code = NewCode("TX-", _store.Transactions.Select(x => x.Code).ToList()),
                    Type = "Receive",
                    AmountRial = finalPaymentRial,
                    Method = string.IsNullOrWhiteSpace(method) ? "Cash" : method.Trim(),
                    Description = "تسویهٔ نهایی اقامت " + (stay.ReservationCode ?? ""),
                    GuestName = stay.GuestName,
                    RoomNumber = stay.RoomNumber,
                    OperatorName = _operator,
                    CreatedAtUtc = DateTime.UtcNow.ToString("o"),
                    IsReversed = false
                });
                if (res != null) res.PaidRial = (res.PaidRial ?? 0) + finalPaymentRial;
            }
            LocalStore.Save(_store);
            return Task.FromResult(Result<StayDto>.Good(stay));
        }
    }

    public Task<Result<RoomDto>> SetRoomStatusAsync(string roomId, string status)
    {
        lock (Gate)
        {
            var room = _store.Rooms.FirstOrDefault(r => r.Id == roomId || r.RoomNumber == roomId);
            if (room is null) return Task.FromResult(Result<RoomDto>.Fail("اتاق یافت نشد: " + roomId));
            var allowed = new[] { "FREE", "RESERVED", "OCCUPIED", "CLEANING", "MAINTENANCE", "OUT_OF_SERVICE" };
            if (!allowed.Contains(status)) return Task.FromResult(Result<RoomDto>.Fail("وضعیت نامعتبر: " + status));
            if (status == "FREE" || status == "CLEANING") room.CurrentGuestName = null;
            room.Status = status;
            LocalStore.Save(_store);
            return Task.FromResult(Result<RoomDto>.Good(room));
        }
    }

    /// <summary>Public self-service registration from the guest QR sheet (no login): creates/finds the guest, files a PENDING reservation and notifies staff.</summary>
    public Task<Result<object>> PublicRegisterAsync(string firstName, string lastName, string phone, string? city, string? checkInIso, string? checkOutIso, int guestsCount, string? notes)
    {
        lock (Gate)
        {
            if (string.IsNullOrWhiteSpace(lastName) && string.IsNullOrWhiteSpace(firstName))
                return Task.FromResult(Result<object>.Fail("نام یا نام خانوادگی را وارد کنید."));
            if (string.IsNullOrWhiteSpace(phone))
                return Task.FromResult(Result<object>.Fail("شمارهٔ موبایل را وارد کنید."));
            var phoneN = phone.Trim();
            var guest = _store.Guests.FirstOrDefault(g => g.Phone == phoneN);
            if (guest is null)
            {
                guest = new GuestDto
                {
                    Id = "g" + Guid.NewGuid().ToString("N")[..8],
                    FirstName = firstName.Trim(),
                    LastName = lastName.Trim(),
                    Phone = phoneN,
                    City = string.IsNullOrWhiteSpace(city) ? null : city.Trim(),
                    BalanceRial = 0,
                    TotalStays = 0,
                    LastStayAtUtc = null
                };
                _store.Guests.Add(guest);
            }
            else
            {
                if (!string.IsNullOrWhiteSpace(firstName)) guest.FirstName = firstName.Trim();
                if (!string.IsNullOrWhiteSpace(lastName)) guest.LastName = lastName.Trim();
                if (!string.IsNullOrWhiteSpace(city)) guest.City = city.Trim();
            }
            string ciIso = DateTime.TryParse(checkInIso, out var ci) ? ci.ToString("yyyy-MM-dd") : DateTime.Today.ToString("yyyy-MM-dd");
            string coIso = DateTime.TryParse(checkOutIso, out var co) && co > ci ? co.ToString("yyyy-MM-dd") : DateTime.Today.AddDays(1).ToString("yyyy-MM-dd");
            var res = new ReservationDto
            {
                Id = "v" + Guid.NewGuid().ToString("N")[..8],
                Code = NewCode("R-", _store.Reservations.Select(x => x.Code).ToList()),
                GuestName = ((guest.FirstName ?? "") + " " + (guest.LastName ?? "")).Trim(),
                GuestPhone = phoneN,
                RoomNumber = null, // staff assigns the room on arrival
                CheckInDate = ciIso,
                CheckOutDate = coIso,
                GuestsCount = guestsCount < 1 ? 1 : guestsCount,
                TotalRial = 0,
                PaidRial = 0,
                Status = "PENDING"
            };
            _store.Reservations.Add(res);
            _store.Notifications.Insert(0, new NotificationDto
            {
                Id = "n" + Guid.NewGuid().ToString("N")[..8],
                Title = "درخواست اقامت جدید از QR",
                Body = res.GuestName + " — " + phoneN + " — " + ciIso + " تا " + coIso + " — " + res.GuestsCount + " نفر" + (string.IsNullOrWhiteSpace(notes) ? "" : " — " + notes),
                Severity = "INFO",
                Kind = "RESERVATION",
                CreatedAtUtc = DateTime.UtcNow.ToString("o"),
                IsRead = false
            });
            LocalStore.Save(_store);
            return Task.FromResult(Result<object>.Good(new { code = res.Code, guestId = guest.Id, status = res.Status }));
        }
    }

    // ================= edit / delete (v1.2.4) =================

    public Task<Result<GuestDto>> UpdateGuestAsync(string id, string firstName, string lastName, string? phone, string? city)
    {
        lock (Gate)
        {
            var g = _store.Guests.FirstOrDefault(x => x.Id == id);
            if (g is null) return Task.FromResult(Result<GuestDto>.Fail("مهمان یافت نشد: " + id));
            if (string.IsNullOrWhiteSpace(lastName) && string.IsNullOrWhiteSpace(firstName))
                return Task.FromResult(Result<GuestDto>.Fail("نام یا نام خانوادگی را وارد کنید."));
            if (!string.IsNullOrWhiteSpace(firstName)) g.FirstName = firstName.Trim();
            if (!string.IsNullOrWhiteSpace(lastName)) g.LastName = lastName.Trim();
            g.Phone = string.IsNullOrWhiteSpace(phone) ? null : phone.Trim();
            g.City = string.IsNullOrWhiteSpace(city) ? null : city.Trim();
            LocalStore.Save(_store);
            return Task.FromResult(Result<GuestDto>.Good(g));
        }
    }

    public Task<Result<bool>> DeleteGuestAsync(string id)
    {
        lock (Gate)
        {
            var g = _store.Guests.FirstOrDefault(x => x.Id == id);
            if (g is null) return Task.FromResult(Result<bool>.Fail("مهمان یافت نشد: " + id));
            var active = _store.Stays.Any(s => s.GuestName == ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim() && s.Status == "IN_HOUSE");
            if (active) return Task.FromResult(Result<bool>.Fail("این مهمان اقامت فعال دارد؛ابتدا تسویه کنید."));
            _store.Guests.Remove(g);
            LocalStore.Save(_store);
            return Task.FromResult(Result<bool>.Good(true));
        }
    }

    public Task<Result<ReservationDto>> UpdateReservationAsync(string id, string guestName, string? guestPhone, string? roomNumber, string checkInIso, string checkOutIso, int guestsCount, long totalRial, long paidRial)
    {
        lock (Gate)
        {
            var r = _store.Reservations.FirstOrDefault(x => x.Id == id || x.Code == id);
            if (r is null) return Task.FromResult(Result<ReservationDto>.Fail("رزرو یافت نشد: " + id));
            if (r.Status == "CHECKED_IN") return Task.FromResult(Result<ReservationDto>.Fail("رزروِ در حال اقامت را نمی‌توان ویرایش کرد؛ابتدا تسویه کنید."));
            if (string.IsNullOrWhiteSpace(guestName))
                return Task.FromResult(Result<ReservationDto>.Fail("نام مهمان را وارد کنید."));
            if (!DateTime.TryParse(checkInIso, out var ci) || !DateTime.TryParse(checkOutIso, out var co) || co <= ci)
                return Task.FromResult(Result<ReservationDto>.Fail("بازهٔ تاریخ نامعتبر است."));
            string? newRoom = string.IsNullOrWhiteSpace(roomNumber) ? null : roomNumber.Trim();
            if (newRoom != null && !_store.Rooms.Any(x => x.RoomNumber == newRoom))
                return Task.FromResult(Result<ReservationDto>.Fail("اتاق «" + newRoom + "» یافت نشد."));
            var ciIso = ci.ToString("yyyy-MM-dd");
            var coIso = co.ToString("yyyy-MM-dd");
            var overlap = _store.Reservations.Any(x => x.Id != r.Id && x.RoomNumber == newRoom &&
                (x.Status == "CONFIRMED" || x.Status == "CHECKED_IN" || x.Status == "PENDING") &&
                string.CompareOrdinal(x.CheckInDate, coIso) < 0 && string.CompareOrdinal(x.CheckOutDate, ciIso) > 0);
            if (overlap) return Task.FromResult(Result<ReservationDto>.Fail("اتاق در این بازه رزرو شده است."));
            r.GuestName = guestName.Trim();
            r.GuestPhone = string.IsNullOrWhiteSpace(guestPhone) ? null : guestPhone.Trim();
            r.RoomNumber = newRoom;
            r.CheckInDate = ciIso;
            r.CheckOutDate = coIso;
            r.GuestsCount = guestsCount < 1 ? 1 : guestsCount;
            r.TotalRial = totalRial;
            r.PaidRial = paidRial;
            if (r.Status == "PENDING" && paidRial > 0) r.Status = "CONFIRMED";
            LocalStore.Save(_store);
            return Task.FromResult(Result<ReservationDto>.Good(r));
        }
    }

    public Task<Result<bool>> DeleteReservationAsync(string id)
    {
        lock (Gate)
        {
            var r = _store.Reservations.FirstOrDefault(x => x.Id == id || x.Code == id);
            if (r is null) return Task.FromResult(Result<bool>.Fail("رزرو یافت نشد: " + id));
            if (r.Status == "CHECKED_IN")
                return Task.FromResult(Result<bool>.Fail("رزروِ در حال اقامت قابل حذف نیست؛ابتدا تسویه کنید."));
            if (_store.Stays.Any(s => s.ReservationCode == r.Code))
                return Task.FromResult(Result<bool>.Fail("برای این رزرو اقامت ثبت شده؛ابتدا آن را تسویه کنید."));
            _store.Reservations.Remove(r);
            LocalStore.Save(_store);
            return Task.FromResult(Result<bool>.Good(true));
        }
    }

    public Task<Result<TransactionDto>> ReverseTransactionAsync(string id)
    {
        lock (Gate)
        {
            var t = _store.Transactions.FirstOrDefault(x => x.Id == id || x.Code == id);
            if (t is null) return Task.FromResult(Result<TransactionDto>.Fail("تراکنش یافت نشد: " + id));
            if (t.IsReversed == true) return Task.FromResult(Result<TransactionDto>.Fail("این تراکنش قبلاً برگشت خورده است."));
            t.IsReversed = true;
            t.Description = (t.Description ?? "") + " [برگشت‌خورده]";
            LocalStore.Save(_store);
            return Task.FromResult(Result<TransactionDto>.Good(t));
        }
    }

    public Task<Result<bool>> ResetDataAsync()
    {
        lock (Gate)
        {
            _store.Guests.Clear();
            _store.Reservations.Clear();
            _store.Stays.Clear();
            _store.Transactions.Clear();
            _store.Housekeeping.Clear();
            _store.Maintenance.Clear();
            _store.Notifications.Clear();
            foreach (var r in _store.Rooms)
            {
                r.Status = "FREE";
                r.CurrentGuestName = null;
            }
            LocalStore.Save(_store);
            return Task.FromResult(Result<bool>.Good(true));
        }
    }
}
