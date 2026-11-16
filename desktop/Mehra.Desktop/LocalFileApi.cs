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
            new RoomDto { Id = "r1", RoomNumber = "101", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "OCCUPIED", CurrentGuestName = "رضایی" },
            new RoomDto { Id = "r2", RoomNumber = "102", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "OCCUPIED", CurrentGuestName = "مریم احمدی" },
            new RoomDto { Id = "r3", RoomNumber = "103", Floor = 1, Capacity = 4, RoomTypeName = "چهارتخته", RatePerNightRial = 4_200_000, Status = "RESERVED" },
            new RoomDto { Id = "r4", RoomNumber = "104", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "CLEANING" },
            new RoomDto { Id = "r5", RoomNumber = "201", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
            new RoomDto { Id = "r6", RoomNumber = "202", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
            new RoomDto { Id = "r7", RoomNumber = "203", Floor = 2, Capacity = 3, RoomTypeName = "سه‌تخته", RatePerNightRial = 3_300_000, Status = "OCCUPIED", CurrentGuestName = "علی رضایی" },
            new RoomDto { Id = "r8", RoomNumber = "301", Floor = 3, Capacity = 4, RoomTypeName = "سوئیت", RatePerNightRial = 6_800_000, Status = "FREE" },
        });
        s.Guests.AddRange(new[]
        {
            new GuestDto { Id = "g1", LastName = "رضایی", Phone = "09120000001", City = "اهواز", BalanceRial = 0, TotalStays = 5, LastStayAtUtc = DateTime.UtcNow.AddDays(-2).ToString("o") },
            new GuestDto { Id = "g2", LastName = "مریم احمدی", Phone = "09120000002", City = "تهران", BalanceRial = 2_500_000, TotalStays = 3, LastStayAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") },
            new GuestDto { Id = "g3", LastName = "علی رضایی", Phone = "09120000005", City = "اهواز", BalanceRial = 0, TotalStays = 8, LastStayAtUtc = DateTime.UtcNow.AddDays(-6).ToString("o") },
            new GuestDto { Id = "g4", LastName = "سارا محمدی", Phone = "09120000003", City = "تهران", BalanceRial = 1_200_000, TotalStays = 1, LastStayAtUtc = iso },
        });
        s.Reservations.AddRange(new[]
        {
            new ReservationDto { Id = "v1", Code = "R-1421", GuestName = "رضایی", GuestPhone = "09120000001", RoomNumber = "101", CheckInDate = today.ToString("yyyy-MM-dd"), CheckOutDate = today.AddDays(2).ToString("yyyy-MM-dd"), GuestsCount = 2, TotalRial = 5_000_000, PaidRial = 5_000_000, Status = "CHECKED_IN" },
            new ReservationDto { Id = "v2", Code = "R-1422", GuestName = "مریم احمدی", GuestPhone = "09120000002", RoomNumber = "102", CheckInDate = today.ToString("yyyy-MM-dd"), CheckOutDate = today.AddDays(1).ToString("yyyy-MM-dd"), GuestsCount = 1, TotalRial = 2_500_000, PaidRial = 0, Status = "CHECKED_IN" },
            new ReservationDto { Id = "v3", Code = "R-1423", GuestName = "سارا محمدی", GuestPhone = "09120000003", RoomNumber = "103", CheckInDate = today.AddDays(1).ToString("yyyy-MM-dd"), CheckOutDate = today.AddDays(3).ToString("yyyy-MM-dd"), GuestsCount = 3, TotalRial = 8_400_000, PaidRial = 4_000_000, Status = "CONFIRMED" },
            new ReservationDto { Id = "v4", Code = "R-1424", GuestName = "حسن کریمی", GuestPhone = "09120000004", RoomNumber = "301", CheckInDate = today.AddDays(5).ToString("yyyy-MM-dd"), CheckOutDate = today.AddDays(7).ToString("yyyy-MM-dd"), GuestsCount = 2, TotalRial = 13_600_000, PaidRial = 0, Status = "PENDING" },
        });
        s.Stays.AddRange(new[]
        {
            new StayDto { Id = "s1", ReservationCode = "R-1421", GuestName = "رضایی", RoomNumber = "101", CheckInAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(3).ToString("o"), PlannedCheckOut = today.AddDays(2).ToString("yyyy-MM-dd"), GuestsCount = 2, Status = "IN_HOUSE" },
            new StayDto { Id = "s2", ReservationCode = "R-1422", GuestName = "مریم احمدی", RoomNumber = "102", CheckInAtUtc = DateTime.UtcNow.AddHours(-4).ToString("o"), PlannedCheckOut = today.AddDays(1).ToString("yyyy-MM-dd"), GuestsCount = 1, Status = "IN_HOUSE" },
            new StayDto { Id = "s3", ReservationCode = "R-1418", GuestName = "علی رضایی", RoomNumber = "203", CheckInAtUtc = DateTime.UtcNow.AddDays(-3).AddHours(2).ToString("o"), PlannedCheckOut = today.ToString("yyyy-MM-dd"), GuestsCount = 3, Status = "IN_HOUSE" },
        });
        s.Transactions.AddRange(new[]
        {
            new TransactionDto { Id = "t1", Code = "TX-9021", Type = "Receive", AmountRial = 5_000_000, Method = "Cash", Description = "پیش‌پرداخت رزرو", GuestName = "رضایی", RoomNumber = "101", OperatorName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-1).ToString("o"), IsReversed = false },
            new TransactionDto { Id = "t2", Code = "TX-9020", Type = "Receive", AmountRial = 2_500_000, Method = "Pos", Description = "تسویهٔ اقامت", GuestName = "مریم احمدی", RoomNumber = "102", OperatorName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o"), IsReversed = false },
            new TransactionDto { Id = "t3", Code = "TX-9019", Type = "Pay", AmountRial = 800_000, Method = "Cash", Description = "خرید ملزومات", OperatorName = "سید مرتضی موسوی", CreatedAtUtc = DateTime.UtcNow.AddHours(-6).ToString("o"), IsReversed = false },
            new TransactionDto { Id = "t4", Code = "TX-9018", Type = "Discount", AmountRial = 500_000, Method = "Cash", Description = "تخفیف", GuestName = "سارا محمدی", RoomNumber = "103", OperatorName = "سید مرتضی موسوی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(-2).ToString("o"), IsReversed = false },
        });
        s.Housekeeping.AddRange(new[]
        {
            new HousekeepingTaskDto { Id = "h1", RoomNumber = "104", Type = "Checkout", Status = "Pending", Notes = "پس از تخلیهٔ مهمان قبلی", CreatedAtUtc = DateTime.UtcNow.AddHours(-2).ToString("o") },
            new HousekeepingTaskDto { Id = "h2", RoomNumber = "203", Type = "Daily", Status = "InProgress", AssignedToName = "زهرا محمدی", CreatedAtUtc = DateTime.UtcNow.AddHours(-5).ToString("o") },
            new HousekeepingTaskDto { Id = "h3", RoomNumber = "101", Type = "Deep", Status = "Done", AssignedToName = "زهرا محمدی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o"), FinishedAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(-3).ToString("o") },
        });
        s.Maintenance.AddRange(new[]
        {
            new MaintenanceTicketDto { Id = "m1", Code = "MT-310", RoomNumber = "203", Title = "تعمیر سیفون دوش", Description = "نشتی آب", Priority = "High", Status = "InProgress", AssigneeName = "مامور رضائی", CostRial = 1_500_000, ReporterName = "زهرا محمدی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") },
            new MaintenanceTicketDto { Id = "m2", Code = "MT-311", RoomNumber = "104", Title = "تعویض لامپ", Description = "لامپ سوخته", Priority = "Urgent", Status = "Open", ReporterName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o") },
        });
        s.Notifications.AddRange(new[]
        {
            new NotificationDto { Id = "n1", Title = "رزرو فردا ثبت نشده", Body = "اتاق ۱۰۱", Severity = "HIGH", Kind = "RESERVATION", CreatedAtUtc = DateTime.UtcNow.AddHours(-2).ToString("o"), IsRead = false },
            new NotificationDto { Id = "n2", Title = "اتاق ۱۰۴ نیاز به نظافت دارد", Body = "اتاق ۱۰۴", Severity = "MEDIUM", Kind = "HOUSEKEEPING", CreatedAtUtc = DateTime.UtcNow.AddHours(-5).ToString("o"), IsRead = false },
            new NotificationDto { Id = "n3", Title = "بدهی مهمان مریم احمدی", Body = "اهواز", Severity = "LOW", Kind = "GUEST", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o"), IsRead = true },
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

    public LocalFileApi() { _store = LocalStore.Load(); }

    public bool IsDemo => false;
    public string ServerLabel => "فایل محلی — بدون سرور";
    public string DataFile => LocalStore.FilePath();
    public LocalStore Store => _store;

    public Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password)
    {
        var u = _store.Users.FirstOrDefault(x => x.Username.Equals(username.Trim(), StringComparison.OrdinalIgnoreCase));
        if (u is null || !u.IsActive)
            return Task.FromResult(Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است."));
        if (LocalStore.Hash(u.Salt, password) != u.PasswordHash)
            return Task.FromResult(Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است."));
        u.LastLoginAtUtc = DateTime.UtcNow.ToString("o");
        LocalStore.Save(_store);
        return Task.FromResult(Result<SessionUser>.Good(new SessionUser
        {
            FullName = u.FullName,
            Username = u.Username,
            RoleTitle = string.IsNullOrEmpty(u.RoleTitle) ? u.RoleCode : u.RoleTitle
        }));
    }

    public Task<Result<string>> HealthAsync() =>
        Task.FromResult(Result<string>.Good("فایل محلی در دسترس است — بدون سرور (" + DataFile + ")"));

    public Task<Result<List<RoomDto>>> RoomsAsync() =>
        Task.FromResult(Result<List<RoomDto>>.Good(_store.Rooms.OrderBy(r => r.RoomNumber).ToList()));

    public Task<Result<List<ReservationDto>>> ReservationsAsync() =>
        Task.FromResult(Result<List<ReservationDto>>.Good(_store.Reservations.OrderByDescending(r => r.CheckInDate).Take(200).ToList()));

    public Task<Result<List<StayDto>>> StaysAsync() =>
        Task.FromResult(Result<List<StayDto>>.Good(_store.Stays.OrderByDescending(s => s.CheckInAtUtc).ToList()));

    public Task<Result<List<GuestDto>>> GuestsAsync() =>
        Task.FromResult(Result<List<GuestDto>>.Good(_store.Guests.OrderBy(g => g.LastName).ThenBy(g => g.FirstName).ToList()));

    public Task<Result<List<HousekeepingTaskDto>>> HousekeepingAsync() =>
        Task.FromResult(Result<List<HousekeepingTaskDto>>.Good(_store.Housekeeping.OrderByDescending(h => h.CreatedAtUtc).ToList()));

    public Task<Result<List<MaintenanceTicketDto>>> MaintenanceAsync() =>
        Task.FromResult(Result<List<MaintenanceTicketDto>>.Good(_store.Maintenance.OrderByDescending(m => m.CreatedAtUtc).ToList()));

    public Task<Result<List<AppUserDto>>> UsersAsync() =>
        Task.FromResult(Result<List<AppUserDto>>.Good(_store.Users.OrderBy(u => u.FullName).Select(u => new AppUserDto
        {
            Id = u.Id, FullName = u.FullName, Username = u.Username, RoleCode = u.RoleCode,
            RoleTitle = u.RoleTitle, Phone = u.Phone, IsActive = u.IsActive, LastLoginAtUtc = u.LastLoginAtUtc
        }).ToList()));

    public Task<Result<List<NotificationDto>>> NotificationsAsync() =>
        Task.FromResult(Result<List<NotificationDto>>.Good(_store.Notifications.OrderByDescending(n => n.CreatedAtUtc).ToList()));

    public Task<Result<string>> MarkAllNotificationsReadAsync()
    {
        foreach (var n in _store.Notifications) n.IsRead = true;
        LocalStore.Save(_store);
        return Task.FromResult(Result<string>.Good("همهٔ اعلان‌ها خوانده شدند."));
    }

    public Task<Result<List<DebtorDto>>> DebtorsAsync() =>
        Task.FromResult(Result<List<DebtorDto>>.Good(_store.Guests
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

    public Task<Result<List<TransactionDto>>> TransactionsAsync(string fromIso, string toIso)
    {
        var f = DateOnly.TryParse(fromIso, out var fd) ? fd : DateOnly.FromDateTime(DateTime.Today.AddDays(-30));
        var t = DateOnly.TryParse(toIso, out var td) ? td : DateOnly.FromDateTime(DateTime.Today);
        var list = _store.Transactions
            .Where(x => DateOnly.TryParse(x.CreatedAtUtc is { Length: >= 10 } c ? c[..10] : "", out var d) && d >= f && d <= t)
            .OrderByDescending(x => x.CreatedAtUtc)
            .Take(100).ToList();
        return Task.FromResult(Result<List<TransactionDto>>.Good(list));
    }

    public Task<Result<CashBoxDto>> CashboxAsync(string dateIso)
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

    public Task<Result<DashboardDto>> DashboardAsync()
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

    public Task<Result<ReportDto>> ReportAsync(string key, string fromIso, string toIso)
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

    private static bool InRange(string? createdAtUtc, string fromIso, string toIso) =>
        createdAtUtc is { Length: >= 10 } c && string.CompareOrdinal(c[..10], fromIso) >= 0 && string.CompareOrdinal(c[..10], toIso) <= 0;
}
