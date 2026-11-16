using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Mehra.Desktop;

/// <summary>
/// Tiny in-process LAN server (raw TcpListener — no admin rights needed, no extra packages)
/// that exposes the local file store over HTTP following docs/API.md (envelope
/// {success,data,error}). The Android app connects by pointing its «سرور» setting at
/// http://&lt;PC-IP&gt;:5050 while the phone is on the same network.
/// Reads serve the real local data; writes answer with a clear Persian NOT_SUPPORTED envelope.
/// </summary>
public class LanServer : IDisposable
{
    public const int DefaultPort = 5050;

    private static readonly JsonSerializerOptions SerOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    private readonly LocalFileApi _api;
    private TcpListener? _listener;
    private CancellationTokenSource _cts = new();
    private volatile bool _running;
    private string? _currentToken;

    public int Port { get; private set; }
    public string LanUrl { get; private set; } = "";
    public string LocalUrl => $"http://localhost:{Port}";

    private LanServer(LocalFileApi api) { _api = api; }

    public static LanServer? Start(LocalFileApi api)
    {
        for (var port = DefaultPort; port < DefaultPort + 10; port++)
        {
            try
            {
                var s = new LanServer(api) { Port = port };
                s._listener = new TcpListener(IPAddress.Any, port);
                s._listener.Start();
                s._running = true;
                var ip = FirstLanIPv4();
                s.LanUrl = $"http://{ip}:{port}";
                _ = Task.Run(s.AcceptLoop);
                return s;
            }
            catch { /* port busy — try next */ }
        }
        return null;
    }

    private static string FirstLanIPv4()
    {
        try
        {
            foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (ni.OperationalStatus != OperationalStatus.Up) continue;
                if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback) continue;
                foreach (var ua in ni.GetIPProperties().UnicastAddresses)
                {
                    if (ua.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ua.Address))
                        return ua.Address.ToString();
                }
            }
        }
        catch { }
        return "localhost";
    }

    private async Task AcceptLoop()
    {
        while (_running)
        {
            TcpClient client;
            try { client = await _listener!.AcceptTcpClientAsync(_cts.Token); }
            catch { break; }
            _ = Task.Run(() => HandleClient(client));
        }
    }

    private async Task HandleClient(TcpClient client)
    {
        try
        {
            using (client)
            {
                client.ReceiveTimeout = 15000;
                client.SendTimeout = 15000;
                var stream = client.GetStream();
                var head = await ReadHeaders(stream);
                if (head is null) return;
                var parts = head.Value.RequestLine.Split(' ', 3);
                if (parts.Length < 2) return;
                var method = parts[0].ToUpperInvariant();
                var rawPath = parts[1];
                var body = await ReadBody(stream, head.Value.ContentLength);
                var (status, payload) = await Route(method, rawPath, head.Value.Headers, body);
                await WriteResponse(stream, status, payload);
            }
        }
        catch { /* client went away */ }
    }

    private static async Task<(string RequestLine, Dictionary<string, string> Headers, int ContentLength)?> ReadHeaders(NetworkStream s)
    {
        var buf = new List<byte>();
        var one = new byte[1];
        var sb = new StringBuilder();
        while (sb.ToString().EndsWith("\r\n\r\n") == false)
        {
            int n;
            try { n = await s.ReadAsync(one, 0, 1); } catch { return null; }
            if (n <= 0) return null;
            buf.Add(one[0]);
            sb.Append((char)one[0]);
            if (buf.Count > 64 * 1024) return null;
        }
        var text = sb.ToString();
        var lines = text.Split("\r\n");
        var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var line in lines.Skip(1))
        {
            var idx = line.IndexOf(':');
            if (idx > 0) headers[line[..idx].Trim()] = line[(idx + 1)..].Trim();
        }
        var cl = headers.TryGetValue("Content-Length", out var clv) && int.TryParse(clv, out var cli) ? cli : 0;
        return (lines[0], headers, cl);
    }

    private static async Task<string> ReadBody(NetworkStream s, int contentLength)
    {
        if (contentLength <= 0) return "";
        var buf = new byte[contentLength];
        var off = 0;
        while (off < contentLength)
        {
            int n;
            try { n = await s.ReadAsync(buf, off, contentLength - off); } catch { break; }
            if (n <= 0) break;
            off += n;
        }
        return Encoding.UTF8.GetString(buf, 0, off);
    }

    private static async Task WriteResponse(NetworkStream s, int status, object payload)
    {
        string contentType;
        byte[] bytes;
        if (payload is RawHtml raw)
        {
            contentType = "text/html; charset=utf-8";
            bytes = Encoding.UTF8.GetBytes(raw.Html);
        }
        else
        {
            contentType = "application/json; charset=utf-8";
            bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(payload, SerOpts));
        }
        var reason = status switch { 200 => "OK", 204 => "No Content", 400 => "Bad Request", 401 => "Unauthorized", 404 => "Not Found", _ => "OK" };
        var head = $"HTTP/1.1 {status} {reason}\r\nContent-Type: {contentType}\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: *\r\nAccess-Control-Allow-Methods: *\r\nContent-Length: {bytes.Length}\r\nConnection: close\r\n\r\n";
        var headBytes = Encoding.UTF8.GetBytes(head);
        await s.WriteAsync(headBytes, 0, headBytes.Length);
        await s.WriteAsync(bytes, 0, bytes.Length);
        await s.FlushAsync();
    }

    private static object EnvelopeOk(object? data) => new
    {
        success = true,
        data,
        error = (object?)null,
        traceId = Guid.NewGuid().ToString("N")[..12]
    };

    private static object EnvelopeErr(string code, string message) => new
    {
        success = false,
        data = (object?)null,
        error = new { code, message, details = (object?)null },
        traceId = Guid.NewGuid().ToString("N")[..12]
    };

    private static int QueryInt(string? v, int def) => int.TryParse(v, out var x) ? x : def;

    private static string? Query(string rawPath, string name)
    {
        var q = rawPath.IndexOf('?');
        if (q < 0) return null;
        foreach (var pair in rawPath[(q + 1)..].Split('&', StringSplitOptions.RemoveEmptyEntries))
        {
            var kv = pair.Split('=', 2);
            if (kv.Length == 2 && Uri.UnescapeDataString(kv[0]) == name)
                return Uri.UnescapeDataString(kv[1].Replace('+', ' '));
        }
        return null;
    }

    private static object Paged<T>(List<T> items, int page, int pageSize)
    {
        var total = items.Count;
        var pageItems = items.Skip((page - 1) * pageSize).Take(pageSize).ToList();
        return new { items = pageItems, page, pageSize, totalCount = total };
    }

    private async Task<(int, object)> Route(string method, string rawPath, Dictionary<string, string> headers, string body)
    {
        var path = rawPath.Split('?')[0].TrimEnd('/');
        if (path.Length == 0) path = "/";
        var seg = path.Split('/', StringSplitOptions.RemoveEmptyEntries);

        // CORS preflight
        if (method == "OPTIONS") return (204, EnvelopeOk(null));

        // Browser-friendly status page
        if (method == "GET" && path == "/")
        {
            var html = "<html dir='rtl'><head><meta charset='utf-8'><title>مهرآ — سرور محلی شبکه</title></head>" +
                       "<body style='font-family:Tahoma;background:#16233D;color:#E8D6AE;padding:40px'>" +
                       "<h1>مهرآ — فایل محلی</h1>" +
                       $"<p>نشانی شبکهٔ این کامپیوتر: <b dir='ltr'>{LanUrl}</b></p>" +
                       "<p>در اپ اندروید، در تنظیمات «نشانی سرور» همین مقدار را وارد کنید و اتصال را تست کنید.</p>" +
                       "<p>ورود: <b dir='ltr'>admin / Admin@1234</b></p>" +
                       "<p style='color:#9AA6B8'>نسخهٔ ویندوز ۱.۲.۰ — داده‌ها در فایل محلی این PC ذخیره می‌شوند (بدون SQL Server).</p>" +
                       "</body></html>";
            return (200, new RawHtml(html));
        }

        // ---- auth ----
        if (path == "/api/v1/auth/login" && method == "POST")
        {
            string? username = null, password = null;
            try
            {
                using var doc = JsonDocument.Parse(body.Length > 0 ? body : "{}");
                username = doc.RootElement.TryGetProperty("username", out var u) ? u.GetString() : null;
                password = doc.RootElement.TryGetProperty("password", out var p) ? p.GetString() : null;
            }
            catch { }
            var login = await _api.LoginAsync("", username ?? "", password ?? "");
            if (!login.Ok) return (401, EnvelopeErr("INVALID_CREDENTIALS", login.Error));
            _currentToken = "mehra-lan-" + Guid.NewGuid().ToString("N")[..16];
            var user = _api.Store.Users.FirstOrDefault(x => x.Username.Equals(login.Value!.Username, StringComparison.OrdinalIgnoreCase));
            return (200, EnvelopeOk(new
            {
                accessToken = _currentToken,
                refreshToken = _currentToken,
                accessExpiresAtUtc = DateTime.UtcNow.AddYears(1).ToString("o"),
                refreshExpiresAtUtc = DateTime.UtcNow.AddYears(1).ToString("o"),
                user = new
                {
                    id = user?.Id, fullName = user?.FullName, username = user?.Username,
                    role = user?.RoleCode, roleTitle = user?.RoleTitle, permissions = new[] { "*" }
                }
            }));
        }
        if (path == "/api/v1/auth/refresh" && method == "POST")
            return (200, EnvelopeOk(new
            {
                accessToken = _currentToken ?? "mehra-lan-local",
                refreshToken = _currentToken ?? "mehra-lan-local",
                accessExpiresAtUtc = DateTime.UtcNow.AddYears(1).ToString("o"),
                refreshExpiresAtUtc = DateTime.UtcNow.AddYears(1).ToString("o"),
                user = CurrentUserPayload()
            }));
        if (path == "/api/v1/auth/logout" && method == "POST") return (200, EnvelopeOk(null));
        if (path == "/api/v1/auth/me" && method == "GET")
        {
            if (_currentToken is null) return (401, EnvelopeErr("UNAUTHORIZED", "ابتدا وارد شوید."));
            return (200, EnvelopeOk(CurrentUserPayload()));
        }

        // ---- system ----
        if (path == "/api/v1/system/health" && method == "GET")
            return (200, EnvelopeOk(new
            {
                serverVersion = "1.2.0",
                apiVersion = "v1",
                serverTimeUtc = DateTime.UtcNow.ToString("o"),
                databaseConnected = true
            }));

        // ---- dashboard ----
        if (path == "/api/v1/dashboard/summary" && method == "GET")
        {
            var d = await _api.DashboardAsync();
            if (!d.Ok) return (200, EnvelopeErr("SERVER_ERROR", d.Error));
            var v = d.Value!;
            return (200, EnvelopeOk(new
            {
                v.BusinessDate, v.TotalRooms, v.FreeRooms, v.ReservedRooms, v.OccupiedRooms,
                v.CleaningRooms, v.MaintenanceRooms, v.OutOfServiceRooms, v.TodayCheckIns, v.TodayCheckOuts,
                v.RevenueTodayRial,
                occupancyPercent = (int)Math.Round(v.OccupancyPercent),
                occupancySeries = v.OccupancySeries,
                recentActivities = v.RecentActivities?.Select(a => new
                {
                    id = Guid.NewGuid().ToString("N"),
                    a.Title,
                    description = a.Title,
                    occurredAtUtc = a.CreatedAtUtc,
                    createdAtUtc = a.CreatedAtUtc,
                    kind = "INFO",
                    actorName = "سیستم"
                }),
                alerts = v.Alerts?.Select(a => new
                {
                    id = Guid.NewGuid().ToString("N"),
                    a.Title,
                    message = a.Message,
                    a.Severity,
                    createdAtUtc = DateTime.UtcNow.ToString("o")
                })
            }));
        }

        // ---- rooms ----
        if (path == "/api/v1/rooms/lookups" && method == "GET")
        {
            var types = _api.Store.Rooms.Select(r => r.RoomTypeName ?? "").Distinct()
                .Select((t, i) => new { id = "rt" + i, title = t }).ToList();
            var floors = _api.Store.Rooms.Select(r => r.Floor).Distinct().OrderBy(f => f).ToList();
            return (200, EnvelopeOk(new { roomTypes = types, amenities = new List<object>(), floors }));
        }
        if (path == "/api/v1/rooms" && method == "GET")
        {
            var rooms = _api.Store.Rooms.ToList();
            var search = Query(rawPath, "search");
            var status = Query(rawPath, "status");
            var floor = Query(rawPath, "floor");
            if (!string.IsNullOrWhiteSpace(search))
                rooms = rooms.Where(r => (r.RoomNumber ?? "").Contains(search) || (r.RoomTypeName ?? "").Contains(search) || (r.CurrentGuestName ?? "").Contains(search)).ToList();
            if (!string.IsNullOrWhiteSpace(status))
                rooms = rooms.Where(r => string.Equals(r.Status, status, StringComparison.OrdinalIgnoreCase)).ToList();
            if (int.TryParse(floor, out var fl))
                rooms = rooms.Where(r => r.Floor == fl).ToList();
            return (200, EnvelopeOk(Paged(rooms.OrderBy(r => r.RoomNumber).ToList(), QueryInt(Query(rawPath, "page"), 1), QueryInt(Query(rawPath, "pageSize"), 50))));
        }
        if (seg.Length == 4 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "rooms" && method == "GET")
        {
            var room = _api.Store.Rooms.FirstOrDefault(r => r.Id == seg[3] || r.RoomNumber == seg[3]);
            return room is null ? (404, EnvelopeErr("NOT_FOUND", "اتاق یافت نشد.")) : (200, EnvelopeOk(room));
        }

        // ---- guests ----
        if (path == "/api/v1/guests" && method == "GET")
        {
            var guests = _api.Store.Guests.ToList();
            var search = Query(rawPath, "search");
            if (!string.IsNullOrWhiteSpace(search))
                guests = guests.Where(g => ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Contains(search) || (g.Phone ?? "").Contains(search) || (g.City ?? "").Contains(search)).ToList();
            return (200, EnvelopeOk(Paged(guests.OrderBy(g => g.LastName).ToList(), QueryInt(Query(rawPath, "page"), 1), QueryInt(Query(rawPath, "pageSize"), 50))));
        }
        if (seg.Length == 4 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "guests" && method == "GET")
        {
            var g = _api.Store.Guests.FirstOrDefault(x => x.Id == seg[3]);
            if (g is null) return (404, EnvelopeErr("NOT_FOUND", "مهمان یافت نشد."));
            var name = ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim();
            return (200, EnvelopeOk(new
            {
                guest = g,
                stays = _api.Store.Stays.Where(s => s.GuestName == name).Select(s => new
                {
                    id = s.Id, guestName = s.GuestName, checkInDate = s.CheckInAtUtc is { Length: >= 10 } c ? c[..10] : null,
                    checkOutDate = s.PlannedCheckOut, totalRial = (long?)null, s.Status
                }).ToList(),
                reservations = _api.Store.Reservations.Where(r => r.GuestName == name).Select(r => new
                {
                    r.Id, r.Code, r.CheckInDate, r.CheckOutDate, r.Status, r.TotalRial, r.RoomNumber
                }).ToList(),
                ledger = _api.Store.Transactions.Where(t => t.GuestName == name).ToList()
            }));
        }

        // ---- reservations ----
        if (path == "/api/v1/reservations/availability" && method == "GET")
        {
            var ci = Query(rawPath, "checkIn") ?? DateTime.Today.ToString("yyyy-MM-dd");
            var co = Query(rawPath, "checkOut") ?? DateTime.Today.AddDays(1).ToString("yyyy-MM-dd");
            var gc = QueryInt(Query(rawPath, "guestsCount"), 1);
            var free = _api.Store.Rooms.Where(r =>
                r.Status == "FREE" && r.Capacity >= gc &&
                !_api.Store.Reservations.Any(x => x.RoomNumber == r.RoomNumber &&
                    (x.Status == "CONFIRMED" || x.Status == "CHECKED_IN" || x.Status == "PENDING") &&
                    string.CompareOrdinal(x.CheckInDate, co) < 0 && string.CompareOrdinal(x.CheckOutDate, ci) > 0))
                .Select(r => new
                {
                    roomId = r.Id, r.RoomNumber, r.Floor, r.RoomTypeName, r.Capacity, r.RatePerNightRial
                }).ToList();
            return (200, EnvelopeOk(free));
        }
        if (path == "/api/v1/reservations/calendar" && method == "GET")
        {
            var from = Query(rawPath, "from") ?? DateTime.Today.AddDays(-7).ToString("yyyy-MM-dd");
            var to = Query(rawPath, "to") ?? DateTime.Today.AddDays(30).ToString("yyyy-MM-dd");
            var entries = new List<object>();
            foreach (var r in _api.Store.Reservations.Where(x =>
                string.CompareOrdinal(x.CheckInDate, to) <= 0 && string.CompareOrdinal(x.CheckOutDate, from) > 0))
            {
                entries.Add(new { reservationId = r.Id, date = r.CheckInDate, roomId = (string?)null, r.RoomNumber, r.GuestName, r.Status, isArrival = true, isDeparture = false });
                entries.Add(new { reservationId = r.Id, date = r.CheckOutDate, roomId = (string?)null, r.RoomNumber, r.GuestName, r.Status, isArrival = false, isDeparture = true });
            }
            return (200, EnvelopeOk(entries));
        }
        if (path == "/api/v1/reservations/waitlist" && method == "GET")
            return (200, EnvelopeOk(new List<object>()));
        if (path == "/api/v1/reservations" && method == "GET")
        {
            var list = _api.Store.Reservations.ToList();
            var search = Query(rawPath, "search");
            var status = Query(rawPath, "status");
            var from = Query(rawPath, "from");
            var to = Query(rawPath, "to");
            if (!string.IsNullOrWhiteSpace(search))
                list = list.Where(r => (r.Code ?? "").Contains(search) || (r.GuestName ?? "").Contains(search) || (r.RoomNumber ?? "").Contains(search) || (r.GuestPhone ?? "").Contains(search)).ToList();
            if (!string.IsNullOrWhiteSpace(status))
                list = list.Where(r => string.Equals(r.Status, status, StringComparison.OrdinalIgnoreCase)).ToList();
            if (!string.IsNullOrWhiteSpace(from))
                list = list.Where(r => string.CompareOrdinal(r.CheckInDate, from) >= 0).ToList();
            if (!string.IsNullOrWhiteSpace(to))
                list = list.Where(r => string.CompareOrdinal(r.CheckInDate, to) <= 0).ToList();
            return (200, EnvelopeOk(Paged(list.OrderByDescending(r => r.CheckInDate).ToList(), QueryInt(Query(rawPath, "page"), 1), QueryInt(Query(rawPath, "pageSize"), 50))));
        }
        if (seg.Length == 4 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "reservations" && method == "GET")
        {
            var r = _api.Store.Reservations.FirstOrDefault(x => x.Id == seg[3]);
            return r is null ? (404, EnvelopeErr("NOT_FOUND", "رزرو یافت نشد.")) : (200, EnvelopeOk(r));
        }

        // ---- reception ----
        if (path == "/api/v1/reception/reservations" && method == "GET")
        {
            var q = Query(rawPath, "query") ?? "";
            var list = _api.Store.Reservations.Where(r =>
                (r.Status == "CONFIRMED" || r.Status == "PENDING" || r.Status == "CHECKED_IN") &&
                (q.Length == 0 || (r.Code ?? "").Contains(q) || (r.GuestName ?? "").Contains(q) || (r.RoomNumber ?? "").Contains(q)))
                .OrderBy(r => r.CheckInDate).ToList();
            return (200, EnvelopeOk(list));
        }
        if (path == "/api/v1/reception/stays" && method == "GET")
        {
            var q = Query(rawPath, "query") ?? "";
            var list = _api.Store.Stays.Where(s => s.Status == "IN_HOUSE" &&
                (q.Length == 0 || (s.GuestName ?? "").Contains(q) || (s.RoomNumber ?? "").Contains(q) || (s.ReservationCode ?? "").Contains(q)))
                .ToList();
            return (200, EnvelopeOk(list));
        }
        if (seg.Length == 5 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "reception" && seg[3] == "stays" && method == "GET")
        {
            var s = _api.Store.Stays.FirstOrDefault(x => x.Id == seg[4]);
            return s is null ? (404, EnvelopeErr("NOT_FOUND", "اقامت یافت نشد.")) : (200, EnvelopeOk(s));
        }
        if (path == "/api/v1/reception/document-types" && method == "GET")
            return (200, EnvelopeOk(new List<string> { "کارت ملی", "گذرنامه", "سایر" }));

        // ---- notifications ----
        if (path == "/api/v1/notifications" && method == "GET")
        {
            var unread = Query(rawPath, "unreadOnly") == "true";
            var list = _api.Store.Notifications.OrderByDescending(n => n.CreatedAtUtc).ToList();
            if (unread) list = list.Where(n => n.IsRead != true).ToList();
            return (200, EnvelopeOk(list));
        }
        if (path == "/api/v1/notifications/read-all" && method == "POST")
        {
            await _api.MarkAllNotificationsReadAsync();
            return (200, EnvelopeOk(null));
        }
        if (seg.Length == 5 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "notifications" && seg[4] == "read" && method == "POST")
        {
            var n = _api.Store.Notifications.FirstOrDefault(x => x.Id == seg[3]);
            if (n != null) { n.IsRead = true; LocalStore.Save(_api.Store); }
            return (200, EnvelopeOk(null));
        }

        // ---- housekeeping / maintenance ----
        if (path == "/api/v1/housekeeping/tasks" && method == "GET")
        {
            var status = Query(rawPath, "status");
            var list = _api.Store.Housekeeping.OrderByDescending(h => h.CreatedAtUtc).ToList();
            if (!string.IsNullOrWhiteSpace(status))
                list = list.Where(h => string.Equals(h.Status, status, StringComparison.OrdinalIgnoreCase)).ToList();
            return (200, EnvelopeOk(list));
        }
        if (path == "/api/v1/housekeeping/staff" && method == "GET" || path == "/api/v1/maintenance/technicians" && method == "GET")
            return (200, EnvelopeOk(StaffList()));
        if (path == "/api/v1/housekeeping/performance" && method == "GET")
            return (200, EnvelopeOk(new List<object>()));
        if (path == "/api/v1/maintenance/tickets" && method == "GET")
        {
            var status = Query(rawPath, "status");
            var prio = Query(rawPath, "priority");
            var list = _api.Store.Maintenance.OrderByDescending(m => m.CreatedAtUtc).ToList();
            if (!string.IsNullOrWhiteSpace(status))
                list = list.Where(m => string.Equals(m.Status, status, StringComparison.OrdinalIgnoreCase)).ToList();
            if (!string.IsNullOrWhiteSpace(prio))
                list = list.Where(m => string.Equals(m.Priority, prio, StringComparison.OrdinalIgnoreCase)).ToList();
            return (200, EnvelopeOk(list));
        }
        if (seg.Length == 5 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "maintenance" && seg[3] == "tickets" && method == "GET")
        {
            var m = _api.Store.Maintenance.FirstOrDefault(x => x.Id == seg[4]);
            return m is null ? (404, EnvelopeErr("NOT_FOUND", "تیکت یافت نشد.")) : (200, EnvelopeOk(m));
        }

        // ---- users / roles ----
        if (path == "/api/v1/users" && method == "GET")
        {
            var list = _api.Store.Users.ToList();
            var search = Query(rawPath, "search");
            if (!string.IsNullOrWhiteSpace(search))
                list = list.Where(u => u.FullName.Contains(search) || u.Username.Contains(search)).ToList();
            var items = list.Select(u => new
            {
                u.Id, u.FullName, u.Username, u.RoleCode, u.RoleTitle, u.Phone, u.IsActive, u.LastLoginAtUtc
            }).ToList();
            return (200, EnvelopeOk(Paged(items, 1, QueryInt(Query(rawPath, "pageSize"), 100))));
        }
        if (path == "/api/v1/roles" && method == "GET")
            return (200, EnvelopeOk(new[]
            {
                new { code = "ADMIN", titleFa = "مدیر کل", permissions = new[] { "*" }, userCount = _api.Store.Users.Count(u => u.RoleCode == "ADMIN") },
                new { code = "RECEPTION", titleFa = "پذیرش", permissions = new[] { "reservations", "reception" }, userCount = _api.Store.Users.Count(u => u.RoleCode == "RECEPTION") },
                new { code = "HOUSEKEEPING", titleFa = "مسئول خدمات", permissions = new[] { "housekeeping" }, userCount = 0 },
                new { code = "FINANCE", titleFa = "مدیر مالی", permissions = new[] { "finance" }, userCount = 0 },
            }));
        if (path == "/api/v1/audit-logs" && method == "GET")
            return (200, EnvelopeOk(new List<object>()));

        // ---- finance ----
        if (path == "/api/v1/finance/transactions" && method == "GET")
        {
            var from = Query(rawPath, "from") ?? DateTime.Today.AddDays(-30).ToString("yyyy-MM-dd");
            var to = Query(rawPath, "to") ?? DateTime.Today.ToString("yyyy-MM-dd");
            var r = await _api.TransactionsAsync(from, to);
            var list = r.Ok ? r.Value! : new List<TransactionDto>();
            var type = Query(rawPath, "type");
            var meth = Query(rawPath, "method");
            var search = Query(rawPath, "search");
            if (!string.IsNullOrWhiteSpace(type)) list = list.Where(t => string.Equals(t.Type, type, StringComparison.OrdinalIgnoreCase)).ToList();
            if (!string.IsNullOrWhiteSpace(meth)) list = list.Where(t => string.Equals(t.Method, meth, StringComparison.OrdinalIgnoreCase)).ToList();
            if (!string.IsNullOrWhiteSpace(search))
                list = list.Where(t => (t.Code ?? "").Contains(search) || (t.GuestName ?? "").Contains(search) || (t.Description ?? "").Contains(search)).ToList();
            return (200, EnvelopeOk(Paged(list, QueryInt(Query(rawPath, "page"), 1), QueryInt(Query(rawPath, "pageSize"), 50))));
        }
        if (path == "/api/v1/finance/cashbox" && method == "GET")
        {
            var r = await _api.CashboxAsync(Query(rawPath, "date") ?? DateTime.Today.ToString("yyyy-MM-dd"));
            return r.Ok ? (200, EnvelopeOk(r.Value)) : (200, EnvelopeErr("SERVER_ERROR", r.Error));
        }
        if (path == "/api/v1/finance/summary" && method == "GET")
        {
            var from = Query(rawPath, "from") ?? DateTime.Today.AddDays(-30).ToString("yyyy-MM-dd");
            var to = Query(rawPath, "to") ?? DateTime.Today.ToString("yyyy-MM-dd");
            var r = await _api.TransactionsAsync(from, to);
            var txns = (r.Ok ? r.Value! : new List<TransactionDto>()).Where(t => t.IsReversed != true).ToList();
            var daily = txns.GroupBy(t => t.CreatedAtUtc is { Length: >= 10 } c ? c[..10] : "")
                .OrderBy(g => g.Key)
                .Select(g => new { date = g.Key, receivedRial = g.Where(t => t.Type is "Receive" or "Discount").Sum(t => t.AmountRial ?? 0), paidRial = g.Where(t => t.Type == "Pay").Sum(t => t.AmountRial ?? 0) })
                .ToList();
            var summary = new
            {
                from, to,
                receivedRial = txns.Where(t => t.Type == "Receive").Sum(t => t.AmountRial ?? 0),
                paidRial = txns.Where(t => t.Type == "Pay").Sum(t => t.AmountRial ?? 0),
                discountsRial = txns.Where(t => t.Type == "Discount").Sum(t => t.AmountRial ?? 0),
                netRial = txns.Sum(t => t.Type is "Receive" or "Discount" ? (t.AmountRial ?? 0) : -(t.AmountRial ?? 0)),
                byMethod = txns.Where(t => t.Type is "Receive" or "Discount").GroupBy(t => t.Method ?? "")
                    .Select(g => new { method = g.Key, amountRial = g.Sum(t => t.AmountRial ?? 0), count = g.Count() }).ToList(),
                dailySeries = daily
            };
            return (200, EnvelopeOk(summary));
        }
        if (path == "/api/v1/finance/debtors" && method == "GET")
        {
            var r = await _api.DebtorsAsync();
            return (200, EnvelopeOk(r.Ok ? r.Value : new List<DebtorDto>()));
        }

        // ---- reports ----
        if (seg.Length == 4 && seg[0] == "api" && seg[1] == "v1" && seg[2] == "reports" && method == "GET")
        {
            var r = await _api.ReportAsync(seg[3], Query(rawPath, "from") ?? DateTime.Today.AddDays(-30).ToString("yyyy-MM-dd"), Query(rawPath, "to") ?? DateTime.Today.ToString("yyyy-MM-dd"));
            return r.Ok ? (200, EnvelopeOk(r.Value)) : (200, EnvelopeErr("SERVER_ERROR", r.Error));
        }

        // ---- everything else (writes / unknown) ----
        if (method is "POST" or "PUT" or "PATCH" or "DELETE")
            return (200, EnvelopeErr("NOT_SUPPORTED",
                "نسخهٔ «فایل محلی» فقط‌خوانی است — برای ثبت دادهٔ جدید از خود برنامهٔ ویندوز استفاده کنید."));
        return (404, EnvelopeErr("NOT_FOUND", "مسیر یافت نشد: " + method + " " + path));
    }

    private object? CurrentUserPayload()
    {
        var u = _api.Store.Users.FirstOrDefault(x => x.Username == "admin") ?? _api.Store.Users.FirstOrDefault();
        return u is null ? null : new
        {
            u.Id, u.FullName, u.Username, role = u.RoleCode, u.RoleTitle, permissions = new[] { "*" }
        };
    }

    private object StaffList() => _api.Store.Users.Where(u => u.IsActive).Select(u => new
    {
        u.Id, u.FullName, u.RoleCode, u.RoleTitle, u.Phone, u.IsActive
    }).ToList();

    public void Dispose()
    {
        _running = false;
        try { _cts.Cancel(); } catch { }
        try { _listener?.Stop(); } catch { }
    }

    /// <summary>Marker so the root page can be served as text/html instead of JSON.</summary>
    private sealed class RawHtml
    {
        public string Html { get; }
        public RawHtml(string html) { Html = html; }
    }
}
