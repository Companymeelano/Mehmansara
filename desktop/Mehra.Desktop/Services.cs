using System.Net.Http.Json;
using System.Text.Json;
using Avalonia.Media;

namespace Mehra.Desktop;

public class Result<T>
{
    public bool Ok { get; private init; }
    public T? Value { get; private init; }
    public string Error { get; private init; } = "";
    public static Result<T> Good(T value) => new() { Ok = true, Value = value };
    public static Result<T> Fail(string error) => new() { Ok = false, Error = error };
}

/// <summary>The hotel backend surface the desktop app needs. Real mode talks REST; demo mode is offline sample data.</summary>
public interface IHotelApi
{
    bool IsDemo { get; }
    string ServerLabel { get; }
    Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password);
    Task<Result<DashboardDto>> DashboardAsync();
    Task<Result<List<RoomDto>>> RoomsAsync();
    Task<Result<List<ReservationDto>>> ReservationsAsync();
    Task<Result<List<StayDto>>> StaysAsync();
    Task<Result<string>> HealthAsync();
}

public static class Json
{
    public static readonly JsonSerializerOptions Opts = new() { PropertyNameCaseInsensitive = true };
}

public static class StatusText
{
    public static string RoomFa(string? status) => status switch
    {
        "FREE" => "خالی",
        "RESERVED" => "رزرو شده",
        "OCCUPIED" => "پر / مهمان دارد",
        "CLEANING" => "در حال نظافت",
        "MAINTENANCE" => "در حال تعمیر",
        "OUT_OF_SERVICE" => "خارج از سرویس",
        _ => status ?? "—"
    };

    public static IBrush RoomBrush(string? status) => status switch
    {
        "FREE" => new SolidColorBrush(Color.Parse("#2E7D5B")),
        "RESERVED" => new SolidColorBrush(Color.Parse("#2F6690")),
        "OCCUPIED" => new SolidColorBrush(Color.Parse("#B08D3E")),
        "CLEANING" => new SolidColorBrush(Color.Parse("#7A5EA8")),
        "MAINTENANCE" => new SolidColorBrush(Color.Parse("#B7791F")),
        "OUT_OF_SERVICE" => new SolidColorBrush(Color.Parse("#8A93A3")),
        _ => Brushes.Gray
    };

    public static string ReservationFa(string? status) => status switch
    {
        "PENDING" => "در انتظار",
        "CONFIRMED" => "تأیید شده",
        "CHECKED_IN" => "تحویل گرفته",
        "CHECKED_OUT" => "تسویه شده",
        "CANCELLED" => "لغو شده",
        "NO_SHOW" => "عدم حضور",
        "WAITLIST" => "لیست انتظار",
        _ => status ?? "—"
    };

    public static IBrush ReservationBrush(string? status) => status switch
    {
        "CONFIRMED" => new SolidColorBrush(Color.Parse("#2F6690")),
        "CHECKED_IN" => new SolidColorBrush(Color.Parse("#2E7D5B")),
        "CHECKED_OUT" => new SolidColorBrush(Color.Parse("#8A93A3")),
        "CANCELLED" or "NO_SHOW" => new SolidColorBrush(Color.Parse("#B3261E")),
        "PENDING" or "WAITLIST" => new SolidColorBrush(Color.Parse("#B7791F")),
        _ => Brushes.Gray
    };

    public static string StayFa(string? status) => status switch
    {
        "IN_HOUSE" => "در مهمان‌پذیر",
        "CHECKED_OUT" => "تسویه شده",
        "MOVED" => "جابجا شده",
        _ => status ?? "—"
    };

    public static IBrush StayBrush(string? status) => status switch
    {
        "IN_HOUSE" => new SolidColorBrush(Color.Parse("#2E7D5B")),
        "CHECKED_OUT" => new SolidColorBrush(Color.Parse("#8A93A3")),
        "MOVED" => new SolidColorBrush(Color.Parse("#2F6690")),
        _ => Brushes.Gray
    };

    public static IBrush SeverityBrush(string? severity) => (severity ?? "").ToUpperInvariant() switch
    {
        "CRITICAL" or "HIGH" => new SolidColorBrush(Color.Parse("#B3261E")),
        "MEDIUM" => new SolidColorBrush(Color.Parse("#B7791F")),
        _ => new SolidColorBrush(Color.Parse("#2F6690"))
    };
}

/// <summary>REST client for the MEHRA backend (see docs/API.md): envelope {success,data,error}, Bearer auth.</summary>
public class ApiClient : IHotelApi
{
    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(30) };
    private string _server = "";
    private string _token = "";

    public bool IsDemo => false;
    public string ServerLabel => string.IsNullOrWhiteSpace(_server) ? "سرور" : _server;

    public async Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password)
    {
        try
        {
            var server = serverUrl.Trim().TrimEnd('/');
            if (server.Length == 0) return Result<SessionUser>.Fail("نشانی سرور را وارد کنید.");
            var resp = await _http.PostAsJsonAsync($"{server}/api/v1/auth/login", new
            {
                username,
                password,
                deviceId = DeviceId(),
                deviceName = "MEHRA Windows Desktop"
            });
            var body = await resp.Content.ReadAsStringAsync();
            var env = JsonSerializer.Deserialize<ApiEnvelope<LoginResponseDto>>(body, Json.Opts);
            if (!resp.IsSuccessStatusCode || env is null || !env.Success || string.IsNullOrEmpty(env.Data?.AccessToken))
                return Result<SessionUser>.Fail(env?.Error?.Message ?? $"پاسخ نامعتبر از سرور (کد {(int)resp.StatusCode})");
            _server = server;
            _token = env.Data!.AccessToken!;
            var u = env.Data.User;
            return Result<SessionUser>.Good(new SessionUser
            {
                FullName = string.IsNullOrWhiteSpace(u?.FullName) ? username : u!.FullName!,
                Username = string.IsNullOrWhiteSpace(u?.Username) ? username : u!.Username!,
                RoleTitle = u?.RoleTitle ?? u?.Role ?? ""
            });
        }
        catch (Exception ex)
        {
            return Result<SessionUser>.Fail("اتصال به سرور برقرار نشد. نشانی سرور و شبکه را بررسی کنید. (" + ex.Message + ")");
        }
    }

    public Task<Result<DashboardDto>> DashboardAsync() => GetAsync<DashboardDto>("/api/v1/dashboard/summary");

    public async Task<Result<List<RoomDto>>> RoomsAsync()
    {
        var r = await GetAsync<Paged<RoomDto>>("/api/v1/rooms?page=1&pageSize=200");
        return r.Ok ? Result<List<RoomDto>>.Good(r.Value?.Items ?? new List<RoomDto>()) : Result<List<RoomDto>>.Fail(r.Error);
    }

    public async Task<Result<List<ReservationDto>>> ReservationsAsync()
    {
        var r = await GetAsync<Paged<ReservationDto>>("/api/v1/reservations?page=1&pageSize=200");
        return r.Ok ? Result<List<ReservationDto>>.Good(r.Value?.Items ?? new List<ReservationDto>()) : Result<List<ReservationDto>>.Fail(r.Error);
    }

    public async Task<Result<List<StayDto>>> StaysAsync()
    {
        var r = await GetAsync<List<StayDto>>("/api/v1/reception/stays");
        return r.Ok ? Result<List<StayDto>>.Good(r.Value ?? new List<StayDto>()) : Result<List<StayDto>>.Fail(r.Error);
    }

    public async Task<Result<string>> HealthAsync()
    {
        var server = _server.Length > 0 ? _server : "https://localhost:5000";
        var r = await GetRawAsync<JsonElement>(server, "/api/v1/system/health");
        return r.Ok ? Result<string>.Good("سرور در دسترس است") : Result<string>.Fail(r.Error);
    }

    private async Task<Result<T>> GetAsync<T>(string path)
    {
        if (_server.Length == 0) return Result<T>.Fail("ابتدا وارد شوید.");
        return await GetRawAsync<T>(_server, path);
    }

    private async Task<Result<T>> GetRawAsync<T>(string server, string path)
    {
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, $"{server}{path}");
            if (_token.Length > 0) req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _token);
            using var resp = await _http.SendAsync(req);
            var body = await resp.Content.ReadAsStringAsync();
            var env = JsonSerializer.Deserialize<ApiEnvelope<T>>(body, Json.Opts);
            if (!resp.IsSuccessStatusCode || env is null || !env.Success)
                return Result<T>.Fail(env?.Error?.Message ?? $"پاسخ نامعتبر از سرور (کد {(int)resp.StatusCode})");
            return Result<T>.Good(env.Data is null ? default! : env.Data);
        }
        catch (Exception ex)
        {
            return Result<T>.Fail("اتصال به سرور برقرار نشد. (" + ex.Message + ")");
        }
    }

    private static string DeviceId()
    {
        try
        {
            var path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "mehra", "device-id.txt");
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            if (File.Exists(path)) return File.ReadAllText(path).Trim();
            var id = Guid.NewGuid().ToString();
            File.WriteAllText(path, id);
            return id;
        }
        catch { return Guid.NewGuid().ToString(); }
    }
}


/// <summary>
/// Offline demo mode: in-memory sample data, clearly labeled, so the app can be explored
/// without the backend. Mirrors the Android demo flavour - never used in production mode.
/// </summary>
public class DemoApi : IHotelApi
{
    private static readonly DateTime Today = DateTime.Today;

    public bool IsDemo => true;
    public string ServerLabel => "نسخهٔ نمایشی — دادهٔ نمونه (بدون سرور)";

    public Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password) =>
        Task.FromResult(Result<SessionUser>.Good(new SessionUser
        {
            FullName = "سید مرتضی موسوی",
            Username = string.IsNullOrWhiteSpace(username) ? "demo" : username,
            RoleTitle = "مدیر کل"
        }));

    public Task<Result<string>> HealthAsync() =>
        Task.FromResult(Result<string>.Good("نسخهٔ نمایشی — بدون سرور"));

    public Task<Result<DashboardDto>> DashboardAsync() => Task.FromResult(Result<DashboardDto>.Good(new DashboardDto
    {
        BusinessDate = Today.ToString("yyyy-MM-dd"),
        TotalRooms = 8,
        FreeRooms = 3,
        ReservedRooms = 1,
        OccupiedRooms = 3,
        CleaningRooms = 1,
        MaintenanceRooms = 0,
        OutOfServiceRooms = 0,
        TodayCheckIns = 2,
        TodayCheckOuts = 1,
        RevenueTodayRial = 18_500_000,
        OccupancyPercent = 37.5,
        OccupancySeries = Enumerable.Range(0, 7).Select(i => new OccupancyPointDto
        {
            Date = Today.AddDays(i - 6).ToString("yyyy-MM-dd"),
            Occupied = 2 + (i % 4),
            Total = 8
        }).ToList(),
        Alerts = new List<DashboardAlertDto>
        {
            new() { Title = "اتاق ۱۰۴ نیاز به نظافت دارد", Message = "پس از تخلیهٔ مهمان قبلی", Severity = "MEDIUM" },
            new() { Title = "رزرو فردا برای اتاق ۱۰۱ ثبت نشده", Message = "با مهمان تماس بگیرید", Severity = "HIGH" }
        },
        RecentActivities = new List<DashboardActivityDto>
        {
            new() { Title = "ورود مهمان به اتاق ۱۰۱ — رزرو ۱۴۲۱", CreatedAtUtc = DateTime.UtcNow.AddHours(-1).ToString("o") },
            new() { Title = "ثبت رزرو جدید برای اتاق ۲۰۳", CreatedAtUtc = DateTime.UtcNow.AddHours(-3).ToString("o") },
            new() { Title = "تسویٔ مبلغ نقدی", CreatedAtUtc = DateTime.UtcNow.AddHours(-5).ToString("o") }
        }
    }));

    public Task<Result<List<RoomDto>>> RoomsAsync() => Task.FromResult(Result<List<RoomDto>>.Good(new List<RoomDto>
    {
        new() { Id = "r1", RoomNumber = "101", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "OCCUPIED", CurrentGuestName = "رضایی" },
        new() { Id = "r2", RoomNumber = "102", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "OCCUPIED", CurrentGuestName = "مریم احمدی" },
        new() { Id = "r3", RoomNumber = "103", Floor = 1, Capacity = 4, RoomTypeName = "چهارتخته", RatePerNightRial = 4_200_000, Status = "RESERVED" },
        new() { Id = "r4", RoomNumber = "104", Floor = 1, Capacity = 2, RoomTypeName = "دوتخته", RatePerNightRial = 2_500_000, Status = "CLEANING" },
        new() { Id = "r5", RoomNumber = "201", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
        new() { Id = "r6", RoomNumber = "202", Floor = 2, Capacity = 2, RoomTypeName = "یک‌تخته", RatePerNightRial = 1_800_000, Status = "FREE" },
        new() { Id = "r7", RoomNumber = "203", Floor = 2, Capacity = 3, RoomTypeName = "سه‌تخته", RatePerNightRial = 3_300_000, Status = "OCCUPIED", CurrentGuestName = "علی رضایی" },
        new() { Id = "r8", RoomNumber = "301", Floor = 3, Capacity = 4, RoomTypeName = "سوئیت", RatePerNightRial = 6_800_000, Status = "FREE" }
    }));

    public Task<Result<List<ReservationDto>>> ReservationsAsync() => Task.FromResult(Result<List<ReservationDto>>.Good(new List<ReservationDto>
    {
        new() { Id = "v1", Code = "R-1421", GuestName = "رضایی", GuestPhone = "09120000001", RoomNumber = "101", CheckInDate = Today.ToString("yyyy-MM-dd"), CheckOutDate = Today.AddDays(2).ToString("yyyy-MM-dd"), GuestsCount = 2, TotalRial = 5_000_000, PaidRial = 5_000_000, Status = "CHECKED_IN" },
        new() { Id = "v2", Code = "R-1422", GuestName = "مریم احمدی", GuestPhone = "09120000002", RoomNumber = "102", CheckInDate = Today.ToString("yyyy-MM-dd"), CheckOutDate = Today.AddDays(1).ToString("yyyy-MM-dd"), GuestsCount = 1, TotalRial = 2_500_000, PaidRial = 0, Status = "CHECKED_IN" },
        new() { Id = "v3", Code = "R-1423", GuestName = "سارا محمدی", GuestPhone = "09120000003", RoomNumber = "103", CheckInDate = Today.AddDays(1).ToString("yyyy-MM-dd"), CheckOutDate = Today.AddDays(3).ToString("yyyy-MM-dd"), GuestsCount = 3, TotalRial = 8_400_000, PaidRial = 4_000_000, Status = "CONFIRMED" },
        new() { Id = "v4", Code = "R-1424", GuestName = "حسنکریمی", GuestPhone = "09120000004", RoomNumber = "301", CheckInDate = Today.AddDays(5).ToString("yyyy-MM-dd"), CheckOutDate = Today.AddDays(7).ToString("yyyy-MM-dd"), GuestsCount = 2, TotalRial = 13_600_000, PaidRial = 0, Status = "PENDING" }
    }));

    public Task<Result<List<StayDto>>> StaysAsync() => Task.FromResult(Result<List<StayDto>>.Good(new List<StayDto>
    {
        new() { Id = "s1", ReservationCode = "R-1421", GuestName = "رضایی", RoomNumber = "101", CheckInAtUtc = DateTime.UtcNow.AddDays(-1).AddHours(3).ToString("o"), PlannedCheckOut = Today.AddDays(2).ToString("yyyy-MM-dd"), GuestsCount = 2, Status = "IN_HOUSE" },
        new() { Id = "s2", ReservationCode = "R-1422", GuestName = "مریم احمدی", RoomNumber = "102", CheckInAtUtc = DateTime.UtcNow.AddHours(-4).ToString("o"), PlannedCheckOut = Today.AddDays(1).ToString("yyyy-MM-dd"), GuestsCount = 1, Status = "IN_HOUSE" },
        new() { Id = "s3", ReservationCode = "R-1418", GuestName = "علی رضایی", RoomNumber = "203", CheckInAtUtc = DateTime.UtcNow.AddDays(-3).AddHours(2).ToString("o"), PlannedCheckOut = Today.ToString("yyyy-MM-dd"), GuestsCount = 3, Status = "IN_HOUSE" }
    }));
}
