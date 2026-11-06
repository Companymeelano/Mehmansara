using System.Net.Http.Json;
using System.Text.Json;

namespace Mehra.Desktop;

/// <summary>ApiClient: the remaining read-only endpoints of docs/API.md.</summary>
public partial class ApiClient
{
    public async Task<Result<List<GuestDto>>> GuestsAsync()
    {
        var r = await GetAsync<Paged<GuestDto>>("/api/v1/guests?page=1&pageSize=100");
        return r.Ok ? Result<List<GuestDto>>.Good(r.Value?.Items ?? new List<GuestDto>()) : Result<List<GuestDto>>.Fail(r.Error);
    }

    public Task<Result<CashBoxDto>> CashboxAsync(string dateIso) =>
        GetAsync<CashBoxDto>($"/api/v1/finance/cashbox?date={dateIso}");

    public async Task<Result<List<DebtorDto>>> DebtorsAsync()
    {
        var r = await GetAsync<List<DebtorDto>>("/api/v1/finance/debtors");
        return r.Ok ? Result<List<DebtorDto>>.Good(r.Value ?? new List<DebtorDto>()) : Result<List<DebtorDto>>.Fail(r.Error);
    }

    public async Task<Result<List<TransactionDto>>> TransactionsAsync(string fromIso, string toIso)
    {
        var r = await GetAsync<Paged<TransactionDto>>($"/api/v1/finance/transactions?from={fromIso}&to={toIso}&page=1&pageSize=50");
        return r.Ok ? Result<List<TransactionDto>>.Good(r.Value?.Items ?? new List<TransactionDto>()) : Result<List<TransactionDto>>.Fail(r.Error);
    }

    public async Task<Result<List<HousekeepingTaskDto>>> HousekeepingAsync()
    {
        var r = await GetAsync<List<HousekeepingTaskDto>>("/api/v1/housekeeping/tasks");
        return r.Ok ? Result<List<HousekeepingTaskDto>>.Good(r.Value ?? new List<HousekeepingTaskDto>()) : Result<List<HousekeepingTaskDto>>.Fail(r.Error);
    }

    public async Task<Result<List<MaintenanceTicketDto>>> MaintenanceAsync()
    {
        var r = await GetAsync<List<MaintenanceTicketDto>>("/api/v1/maintenance/tickets");
        return r.Ok ? Result<List<MaintenanceTicketDto>>.Good(r.Value ?? new List<MaintenanceTicketDto>()) : Result<List<MaintenanceTicketDto>>.Fail(r.Error);
    }

    public async Task<Result<List<AppUserDto>>> UsersAsync()
    {
        var r = await GetAsync<Paged<AppUserDto>>("/api/v1/users?page=1&pageSize=100");
        return r.Ok ? Result<List<AppUserDto>>.Good(r.Value?.Items ?? new List<AppUserDto>()) : Result<List<AppUserDto>>.Fail(r.Error);
    }

    public async Task<Result<List<NotificationDto>>> NotificationsAsync()
    {
        var r = await GetAsync<List<NotificationDto>>("/api/v1/notifications?unreadOnly=false");
        return r.Ok ? Result<List<NotificationDto>>.Good(r.Value ?? new List<NotificationDto>()) : Result<List<NotificationDto>>.Fail(r.Error);
    }

    public async Task<Result<string>> MarkAllNotificationsReadAsync()
    {
        if (_server.Length == 0) return Result<string>.Fail("ابتدا وارد شوید.");
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, $"{_server}/api/v1/notifications/read-all")
            {
                Content = JsonContent.Create(new { notificationIds = Array.Empty<string>() })
            };
            req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _token);
            using var resp = await _http.SendAsync(req);
            var body = await resp.Content.ReadAsStringAsync();
            var env = JsonSerializer.Deserialize<ApiEnvelope<JsonElement>>(body, Json.Opts);
            if (!resp.IsSuccessStatusCode || env is null || !env.Success)
                return Result<string>.Fail(env?.Error?.Message ?? $"پاسخ نامعتبر از سرور (کد {(int)resp.StatusCode})");
            return Result<string>.Good("همهٔ اعلان‌ها خوانده شدند.");
        }
        catch (Exception ex)
        {
            return Result<string>.Fail("اتصال به سرور برقرار نشد. (" + ex.Message + ")");
        }
    }

    public Task<Result<ReportDto>> ReportAsync(string key, string fromIso, string toIso) =>
        GetAsync<ReportDto>($"/api/v1/reports/{key}?from={fromIso}&to={toIso}");
}


/// <summary>DemoApi: offline sample data for the remaining read-only modules.</summary>
public partial class DemoApi
{
    private static string Iso(int daysAgo = 0, int hour = 12) =>
        DateTime.Today.AddDays(-daysAgo).AddHours(hour - DateTime.Now.Hour).ToString("yyyy-MM-dd");

    public Task<Result<List<GuestDto>>> GuestsAsync() => Task.FromResult(Result<List<GuestDto>>.Good(new List<GuestDto>
    {
        new() { Id = "g1", FirstName = "", LastName = "رضایی", Phone = "09120000001", City = "اهواز", BalanceRial = 0, TotalStays = 5, LastStayAtUtc = DateTime.UtcNow.AddDays(-2).ToString("o") },
        new() { Id = "g2", FirstName = "", LastName = "مریم احمدی", Phone = "09120000002", City = "تهران", BalanceRial = 2_500_000, TotalStays = 3, LastStayAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") },
        new() { Id = "g3", FirstName = "", LastName = "علی رضایی", Phone = "09120000005", City = "اهواز", BalanceRial = 0, TotalStays = 8, LastStayAtUtc = DateTime.UtcNow.AddDays(-6).ToString("o") },
        new() { Id = "g4", FirstName = "", LastName = "سارا محمدی", Phone = "09120000003", City = "تهران", BalanceRial = 1_200_000, TotalStays = 1, LastStayAtUtc = DateTime.UtcNow.ToString("o") },
        new() { Id = "g5", FirstName = "", LastName = "حسنکریمی", Phone = "09120000004", City = "اهواز", BalanceRial = 0, TotalStays = 2, LastStayAtUtc = DateTime.UtcNow.AddDays(-20).ToString("o") }
    }));

    public Task<Result<CashBoxDto>> CashboxAsync(string dateIso) => Task.FromResult(Result<CashBoxDto>.Good(new CashBoxDto
    {
        Date = dateIso,
        ReceivedRial = 18_500_000,
        PaidRial = 3_200_000,
        DiscountsRial = 500_000,
        NetRial = 14_800_000,
        TransactionCount = 7,
        OpenBalanceRial = 42_300_000,
        ByMethod = new List<MethodTotalDto>
        {
            new() { Method = "Cash", AmountRial = 9_000_000, Count = 4 },
            new() { Method = "Pos", AmountRial = 8_500_000, Count = 2 },
            new() { Method = "Transfer", AmountRial = 1_000_000, Count = 1 }
        }
    }));

    public Task<Result<List<DebtorDto>>> DebtorsAsync() => Task.FromResult(Result<List<DebtorDto>>.Good(new List<DebtorDto>
    {
        new() { GuestId = "g2", GuestName = "مریم احمدی", Phone = "09120000002", RoomNumber = "102", BalanceRial = 2_500_000, LastActivityAtUtc = DateTime.UtcNow.AddHours(-4).ToString("o") },
        new() { GuestId = "g4", GuestName = "سارا محمدی", Phone = "09120000003", RoomNumber = "103", BalanceRial = 1_200_000, LastActivityAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") }
    }));

    public Task<Result<List<TransactionDto>>> TransactionsAsync(string fromIso, string toIso) => Task.FromResult(Result<List<TransactionDto>>.Good(new List<TransactionDto>
    {
        new() { Id = "t1", Code = "TX-9021", Type = "Receive", AmountRial = 5_000_000, Method = "Cash", Description = "پیش‌پرداخت رزرو", GuestName = "رضایی", RoomNumber = "101", OperatorName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-1).ToString("o"), IsReversed = false },
        new() { Id = "t2", Code = "TX-9020", Type = "Receive", AmountRial = 2_500_000, Method = "Pos", Description = "تسویٔ", GuestName = "مریم احمدی", RoomNumber = "102", OperatorName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o"), IsReversed = false },
        new() { Id = "t3", Code = "TX-9019", Type = "Pay", AmountRial = 800_000, Method = "Cash", Description = "خرید ملزومات", GuestName = "", RoomNumber = "", OperatorName = "سید مرتضی موسوی", CreatedAtUtc = DateTime.UtcNow.AddHours(-6).ToString("o"), IsReversed = false },
        new() { Id = "t4", Code = "TX-9018", Type = "Discount", AmountRial = 500_000, Method = "Cash", Description = "تخفیف", GuestName = "سارا محمدی", RoomNumber = "103", OperatorName = "سید مرتضی موسوی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(-2).ToString("o"), IsReversed = false },
        new() { Id = "t5", Code = "TX-9017", Type = "Receive", AmountRial = 8_400_000, Method = "Transfer", Description = "پیش‌پرداخت رزرو", GuestName = "سارا محمدی", RoomNumber = "103", OperatorName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o"), IsReversed = true }
    }));

    public Task<Result<List<HousekeepingTaskDto>>> HousekeepingAsync() => Task.FromResult(Result<List<HousekeepingTaskDto>>.Good(new List<HousekeepingTaskDto>
    {
        new() { Id = "h1", RoomNumber = "104", Type = "Checkout", Status = "Pending", AssignedToName = null, Notes = "با مهمان تماس گرفته شده", CreatedAtUtc = DateTime.UtcNow.AddHours(-2).ToString("o") },
        new() { Id = "h2", RoomNumber = "203", Type = "Daily", Status = "InProgress", AssignedToName = "زهرا محمدی", Notes = "", CreatedAtUtc = DateTime.UtcNow.AddHours(-5).ToString("o") },
        new() { Id = "h3", RoomNumber = "101", Type = "Deep", Status = "Done", AssignedToName = "زهرا محمدی", Notes = "", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o"), FinishedAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(-3).ToString("o") }
    }));

    public Task<Result<List<MaintenanceTicketDto>>> MaintenanceAsync() => Task.FromResult(Result<List<MaintenanceTicketDto>>.Good(new List<MaintenanceTicketDto>
    {
        new() { Id = "m1", Code = "MT-310", RoomNumber = "203", Title = "تعمیر سیفون دوشاب", Description = "سیفون آب تا برار نشتی", Priority = "High", Status = "InProgress", AssigneeName = "مامور رضائی", CostRial = 1_500_000, ReporterName = "زهرا محمدی", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") },
        new() { Id = "m2", Code = "MT-311", RoomNumber = "104", Title = "تعویض لامپ اتاق ۲۰۳", Description = "لامپ سوخته شده و تعویض شد", Priority = "Urgent", Status = "Open", AssigneeName = null, CostRial = null, ReporterName = "مامور رضائی", CreatedAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o") }
    }));

    public Task<Result<List<AppUserDto>>> UsersAsync() => Task.FromResult(Result<List<AppUserDto>>.Good(new List<AppUserDto>
    {
        new() { Id = "u1", FullName = "سید مرتضی موسوی", Username = "admin", RoleCode = "ADMIN", RoleTitle = "مدیر کل", Phone = "09120000010", IsActive = true, LastLoginAtUtc = DateTime.UtcNow.AddHours(-1).ToString("o") },
        new() { Id = "u2", FullName = "مامور رضائی", Username = "reception", RoleCode = "RECEPTION", RoleTitle = "پذیرش", Phone = "09120000011", IsActive = true, LastLoginAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o") },
        new() { Id = "u3", FullName = "زهرا محمدی", Username = "housekeeping", RoleCode = "HOUSEKEEPING", RoleTitle = "مسئول خدمات", Phone = "09120000012", IsActive = true, LastLoginAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o") },
        new() { Id = "u4", FullName = "حسنکریمی", Username = "finance1", RoleCode = "FINANCE", RoleTitle = "مدیر مالی", Phone = "09120000013", IsActive = false, LastLoginAtUtc = DateTime.UtcNow.AddDays(-30).ToString("o") }
    }));

    public Task<Result<List<NotificationDto>>> NotificationsAsync() => Task.FromResult(Result<List<NotificationDto>>.Good(new List<NotificationDto>
    {
        new() { Id = "n1", Title = "رزرو فردا ثبت نشده", Body = "اتاق ۱۰۱", Severity = "HIGH", Kind = "RESERVATION", CreatedAtUtc = DateTime.UtcNow.AddHours(-2).ToString("o"), IsRead = false },
        new() { Id = "n2", Title = "اتاق ۱۰۴ نیاز به نظافت دارد", Body = "اتاق ۱۰۴", Severity = "MEDIUM", Kind = "HOUSEKEEPING", CreatedAtUtc = DateTime.UtcNow.AddHours(-5).ToString("o"), IsRead = false },
        new() { Id = "n3", Title = "بدهی مهمان مریم احمدی", Body = "اهواز", Severity = "LOW", Kind = "GUEST", CreatedAtUtc = DateTime.UtcNow.AddDays(-1).ToString("o"), IsRead = true }
    }));

    public Task<Result<string>> MarkAllNotificationsReadAsync() =>
        Task.FromResult(Result<string>.Good("همهٔ اعلان‌ها خوانده شدند (نسخهٔ نمایشی)."));

    public Task<Result<ReportDto>> ReportAsync(string key, string fromIso, string toIso) => Task.FromResult(Result<ReportDto>.Good(new ReportDto
    {
        Type = key,
        Title = "گزارش نمونه — نمایشی",
        Subtitle = "",
        Columns = new List<string> { "تاریخ", "مقدار", "توضیح" },
        Rows = new List<List<string>>
        {
            new() { Fa.Date(fromIso), "۱٬۲۵۰٬۰۰۰", "دریافت مهمان" },
            new() { Fa.Date(toIso), "۹۸۰٬۰۰۰", "تسویه اقامت" }
        },
        Totals = new List<string> { "جمع: ۲٬۲۳۰٬۰۰۰ ریال" },
        Notes = new List<string> { "داده‌های نمونه است." },
        From = fromIso,
        To = toIso,
        GeneratedAtUtc = DateTime.UtcNow.ToString("o")
    }));
}
