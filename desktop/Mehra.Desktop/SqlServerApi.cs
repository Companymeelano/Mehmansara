using System.Data;
using System.Data.SqlClient;
using System.Security.Cryptography;
using System.Text;

namespace Mehra.Desktop;

/// <summary>
/// Direct SQL Server mode: the Windows app talks to SQL Server itself (2-tier), no web backend needed.
/// The installer (MehraSetup.exe) creates the tables; the app can also (re)create them on demand.
/// Schema: desktop/Mehra.Desktop/db/schema.sql — idempotent, works on any SQL Server version (2008+).
/// </summary>
public class SqlServerApi : IHotelApi
{
    public string Server { get; }
    public string Database { get; }
    public bool WindowsAuth { get; }
    public string? SqlUser { get; }
    private string? _sqlPass;
    private string? _tokenUser; // logged-in username (acts as the bearer identity)

    public bool IsDemo => false;
    public string ServerLabel => "SQL Server: " + Server + " / " + Database;

    private SqlServerApi(string server, string database, bool windowsAuth, string? sqlUser, string? sqlPass)
    {
        Server = server; Database = database; WindowsAuth = windowsAuth; SqlUser = sqlUser; _sqlPass = sqlPass;
    }

    public string ConnectionString => WindowsAuth
        ? $"Data Source={Server};Initial Catalog={Database};Integrated Security=True;Encrypt=False;TrustServerCertificate=True;Connection Timeout=15"
        : $"Data Source={Server};Initial Catalog={Database};User ID={SqlUser};Password={_sqlPass};Encrypt=False;TrustServerCertificate=True;Connection Timeout=15";

    /// <summary>
    /// Server names tried in order when the configured one does not answer. Covers the common
    /// SQL Server 2022 installs: a LocalDB shared instance, the default instance on localhost,
    /// or a named SQLEXPRESS instance.
    /// </summary>
    public static readonly string[] ServerCandidates =
    {
        "(localdb)\\MSSQLLocalDB", "localhost", "(local)", ".\\SQLEXPRESS", "localhost\\SQLEXPRESS", "127.0.0.1",
    };

    /// <summary>
    /// Probes the preferred server first, then every known candidate; for the first reachable
    /// server it makes sure the database exists (CREATE DATABASE if missing) and returns an api
    /// bound to that server. Used when the PC already runs SQL Server 2022 as a normal instance
    /// (not LocalDB), so «(localdb)\MSSQLLocalDB» in settings would otherwise fail.
    /// </summary>
    public static async Task<Result<(SqlServerApi Api, string ResolvedServer)>> ResolveAsync(
        string preferred, string database, bool windowsAuth, string? sqlUser, string? sqlPass)
    {
        var tried = new List<string>();
        foreach (var server in new[] { preferred }.Concat(ServerCandidates.Where(c => !c.Equals(preferred, StringComparison.OrdinalIgnoreCase))))
        {
            if (string.IsNullOrWhiteSpace(server) || tried.Contains(server)) continue;
            tried.Add(server);
            var probe = await TryCreateAsync(server, database, windowsAuth, sqlUser, sqlPass);
            if (!probe.Ok) continue;
            // Reachable — make sure the database itself exists (error 4060 otherwise).
            try
            {
                var api = probe.Value!;
                await using var con = new SqlConnection(api.MasterConnectionString);
                await con.OpenAsync();
                await using var cmd = new SqlCommand(
                    "IF DB_ID(@db) IS NULL EXEC('CREATE DATABASE [' + REPLACE(@db, ']', ']]') + ']')",
                    con);
                cmd.Parameters.AddWithValue("@db", database);
                await cmd.ExecuteNonQueryAsync();
                return Result<(SqlServerApi Api, string ResolvedServer)>.Good((api, server));
            }
            catch { /* try next candidate */ }
        }
        return Result<(SqlServerApi, string)>.Fail(
            "اتصال به هیچ‌یک از سرورهای SQL Server برقرار نشد: " + string.Join("، ", tried));
    }

    /// <summary>Tests the connection (SELECT 1) without touching the DB schema.</summary>
    public static async Task<Result<SqlServerApi>> TryCreateAsync(string server, string database, bool windowsAuth, string? sqlUser, string? sqlPass)
    {
        var api = new SqlServerApi(server, database, windowsAuth, sqlUser, sqlPass);
        try
        {
            // Test against master: the target DB may not exist yet (schema is created separately).
            var probe = new SqlServerApi(server, "master", windowsAuth, sqlUser, sqlPass);
            await using var con = new SqlConnection(probe.ConnectionString);
            await con.OpenAsync();
            await using var cmd = new SqlCommand("SELECT 1", con);
            await cmd.ExecuteScalarAsync();
            return Result<SqlServerApi>.Good(api);
        }
        catch (Exception ex)
        {
            return Result<SqlServerApi>.Fail("اتصال به SQL Server برقرار نشد؛ نام سرور (instance) و شبکه را بررسی کنید. " + ex.Message);
        }
    }

    /// <summary>Runs db/schema.sql batch-by-batch (idempotent). Used by the installer flow and the app's «راه‌اندازی پایگاه داده».</summary>
    public static async Task<Result<string>> EnsureSchemaAsync(string connectionStringMaster, string schemaPath, string dbName)
    {
        try
        {
            // schema.sql hardcodes [Mehra]; retarget it to the configured database name.
            var sql = (await File.ReadAllTextAsync(schemaPath)).Replace("Mehra", dbName);
            var batches = new List<string>();
            var sb = new StringBuilder();
            foreach (var line in sql.Split('\n'))
            {
                if (line.Trim().Equals("GO", StringComparison.OrdinalIgnoreCase))
                {
                    if (sb.Length > 0) batches.Add(sb.ToString());
                    sb.Clear();
                }
                else sb.AppendLine(line);
            }
            if (sb.Length > 0) batches.Add(sb.ToString());
            await using var con = new SqlConnection(connectionStringMaster);
            await con.OpenAsync();
            foreach (var b in batches)
            {
                if (string.IsNullOrWhiteSpace(b)) continue;
                await using var cmd = new SqlCommand(b, con) { CommandTimeout = 180 };
                await cmd.ExecuteNonQueryAsync();
            }
            return Result<string>.Good("پایگاه داده و جداول با موفقیت ایجاد/به‌روز شدند.");
        }
        catch (Exception ex)
        {
            return Result<string>.Fail("اجرای اسکریپت پایگاه داده ناموفق بود: " + ex.Message);
        }
    }

    public string MasterConnectionString => WindowsAuth
        ? $"Data Source={Server};Initial Catalog=master;Integrated Security=True;Encrypt=False;TrustServerCertificate=True;Connection Timeout=15"
        : $"Data Source={Server};User ID={SqlUser};Password={_sqlPass};Initial Catalog=master;Encrypt=False;TrustServerCertificate=True;Connection Timeout=15";

    private static string Hash(string salt, string password) =>
        Convert.ToHexString(SHA256.HashData(Encoding.Unicode.GetBytes(salt + password))).ToLowerInvariant();

    /// <summary>Authenticates against dbo.users. IHotelApi.LoginAsync repurposes serverUrl as the server name.</summary>
    public async Task<Result<SessionUser>> LoginAsync(string serverUrl, string username, string password)
    {
        var first = await TryLoginAsync(username, password);
        if (first.Ok) return first;
        // Connected, but the database/tables are missing (e.g. the installer's schema step
        // failed): create them from the bundled/embedded schema.sql and retry once.
        if (LooksLikeMissingSchema(first.Error))
        {
            var schema = await EnsureSchemaAsync(MasterConnectionString, SchemaFilePath(), Database);
            if (!schema.Ok)
                return Result<SessionUser>.Fail("ساخت خودکار جداول پایگاه داده ناموفق بود: " + schema.Error);
            return await TryLoginAsync(username, password);
        }
        return first;
    }

    private static bool LooksLikeMissingSchema(string error) =>
        error.Contains("Invalid object name") || error.Contains("Cannot open database") || error.Contains("4060");

    private static string SchemaFilePath()
    {
        var p = Path.Combine(AppContext.BaseDirectory, "db", "schema.sql");
        if (File.Exists(p)) return p;
        var asm = System.Reflection.Assembly.GetExecutingAssembly();
        using var rs = asm.GetManifestResourceStream("Mehra.Desktop.db.schema.sql");
        if (rs != null)
        {
            var tmp = Path.Combine(Path.GetTempPath(), "mehra-schema.sql");
            using (var fs = File.Create(tmp)) rs.CopyTo(fs);
            return tmp;
        }
        return p; // EnsureSchemaAsync will report the missing file itself
    }

    private async Task<Result<SessionUser>> TryLoginAsync(string username, string password)
    {
        try
        {
            await using var con = new SqlConnection(ConnectionString);
            await con.OpenAsync();
            await using var cmd = new SqlCommand(
                "SELECT TOP 1 passwordHash, salt, isActive, fullName, roleTitle, roleCode FROM dbo.users WHERE username=@u", con);
            cmd.Parameters.AddWithValue("@u", username);
            await using var r = await cmd.ExecuteReaderAsync();
            if (!await r.ReadAsync())
                return Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است.");
            var active = r.GetBoolean(2);
            if (!active) return Result<SessionUser>.Fail("این حساب کاربری غیرفعال است.");
            var hash = r.GetString(0); var salt = r.GetString(1);
            if (Hash(salt, password) != hash)
                return Result<SessionUser>.Fail("نام کاربری یا رمز عبور نادرست است.");
            _tokenUser = username;
            await using var up = new SqlCommand("UPDATE dbo.users SET lastLoginAtUtc=SYSUTCDATETIME() WHERE username=@u", con);
            up.Parameters.AddWithValue("@u", username);
            await up.ExecuteNonQueryAsync();
            return Result<SessionUser>.Good(new SessionUser
            {
                FullName = r.GetString(3),
                Username = username,
                RoleTitle = r.IsDBNull(4) ? (r.GetString(5)) : r.GetString(4)
            });
        }
        catch (SqlException ex)
        {
            return ex.Number switch
            {
                4060 => Result<SessionUser>.Fail("Cannot open database «" + Database + "» (4060) — پایگاه داده هنوز ساخته نشده است."),
                -2 or 258 => Result<SessionUser>.Fail("اتصال به SQL Server برقرار نشد؛ وضعیت instance «" + Server + "» را بررسی کنید. " + ex.Message),
                _ => Result<SessionUser>.Fail("خطا در ورود به پایگاه داده: " + ex.Message),
            };
        }
        catch (Exception ex)
        {
            return Result<SessionUser>.Fail("خطا در ورود به پایگاه داده: " + ex.Message);
        }
    }

    public Task<Result<string>> HealthAsync() => Task.FromResult(Result<string>.Good(ServerLabel + " — در دسترس"));

    private async Task<SqlConnection> OpenAsync()
    {
        var con = new SqlConnection(ConnectionString);
        await con.OpenAsync();
        return con;
    }

    private static async Task<Result<List<T>>> QueryAsync<T>(string connectionString, string sql, Func<SqlDataReader, T> map, params (string, object)[] ps)
    {
        try
        {
            await using var con = new SqlConnection(connectionString);
            await con.OpenAsync();
            await using var cmd = new SqlCommand(sql, con);
            foreach (var (n, v) in ps) cmd.Parameters.AddWithValue(n, v);
            await using var r = await cmd.ExecuteReaderAsync();
            var list = new List<T>();
            while (await r.ReadAsync()) list.Add(map(r));
            return Result<List<T>>.Good(list);
        }
        catch (Exception ex) { return Result<List<T>>.Fail("خطای پایگاه داده: " + ex.Message); }
    }

    private static string S(SqlDataReader r, int i) => r.IsDBNull(i) ? "" : r.GetString(i);
    private static long L(SqlDataReader r, int i) => r.IsDBNull(i) ? 0 : Convert.ToInt64(r.GetValue(i));
    private static int I(SqlDataReader r, int i) => r.IsDBNull(i) ? 0 : r.GetInt32(i);
    private static bool B(SqlDataReader r, int i) => !r.IsDBNull(i) && r.GetBoolean(i);
    private static string? D(SqlDataReader r, int i) => r.IsDBNull(i) ? null : r.GetDateTime(i).ToString("yyyy-MM-ddTHH:mm:ss");

    public Task<Result<List<RoomDto>>> RoomsAsync() => QueryAsync(ConnectionString,
        "SELECT id, roomNumber, floor, capacity, roomTypeName, ratePerNightRial, status, currentGuestName FROM dbo.rooms ORDER BY roomNumber",
        r => new RoomDto { Id = S(r,0), RoomNumber = S(r,1), Floor = I(r,2), Capacity = I(r,3), RoomTypeName = S(r,4), RatePerNightRial = L(r,5), Status = S(r,6), CurrentGuestName = r.IsDBNull(7) ? null : S(r,7) });

    public Task<Result<List<GuestDto>>> GuestsAsync() => QueryAsync(ConnectionString,
        "SELECT id, firstName, lastName, phone, city, balanceRial, totalStays, lastStayAtUtc FROM dbo.guests ORDER BY lastName, firstName",
        r => new GuestDto { Id = S(r,0), FirstName = S(r,1), LastName = S(r,2), Phone = r.IsDBNull(3)?null:S(r,3), City = r.IsDBNull(4)?null:S(r,4), BalanceRial = L(r,5), TotalStays = I(r,6), LastStayAtUtc = D(r,7) });

    public Task<Result<List<ReservationDto>>> ReservationsAsync() => QueryAsync(ConnectionString,
        "SELECT TOP 200 id, code, guestName, guestPhone, roomNumber, checkInDate, checkOutDate, guestsCount, totalRial, paidRial, status FROM dbo.reservations ORDER BY checkInDate DESC",
        r => new ReservationDto { Id = S(r,0), Code = S(r,1), GuestName = S(r,2), GuestPhone = r.IsDBNull(3)?null:S(r,3), RoomNumber = r.IsDBNull(4)?null:S(r,4), CheckInDate = r.GetDateTime(5).ToString("yyyy-MM-dd"), CheckOutDate = r.GetDateTime(6).ToString("yyyy-MM-dd"), GuestsCount = I(r,7), TotalRial = L(r,8), PaidRial = L(r,9), Status = S(r,10) });

    public Task<Result<List<StayDto>>> StaysAsync() => QueryAsync(ConnectionString,
        "SELECT id, reservationCode, guestName, roomNumber, checkInAtUtc, plannedCheckOut, guestsCount, status FROM dbo.stays ORDER BY checkInAtUtc DESC",
        r => new StayDto { Id = S(r,0), ReservationCode = S(r,1), GuestName = S(r,2), RoomNumber = r.IsDBNull(3)?null:S(r,3), CheckInAtUtc = D(r,4), PlannedCheckOut = r.IsDBNull(5)?null:r.GetDateTime(5).ToString("yyyy-MM-dd"), GuestsCount = I(r,6), Status = S(r,7) });

    public Task<Result<List<HousekeepingTaskDto>>> HousekeepingAsync() => QueryAsync(ConnectionString,
        "SELECT id, roomNumber, type, status, assignedToName, notes, createdAtUtc, finishedAtUtc FROM dbo.housekeeping_tasks ORDER BY createdAtUtc DESC",
        r => new HousekeepingTaskDto { Id = S(r,0), RoomNumber = r.IsDBNull(1)?null:S(r,1), Type = S(r,2), Status = S(r,3), AssignedToName = r.IsDBNull(4)?null:S(r,4), Notes = r.IsDBNull(5)?null:S(r,5), CreatedAtUtc = D(r,6), FinishedAtUtc = D(r,7) });

    public Task<Result<List<MaintenanceTicketDto>>> MaintenanceAsync() => QueryAsync(ConnectionString,
        "SELECT id, code, roomNumber, title, description, priority, status, assigneeName, costRial, reporterName, createdAtUtc, resolvedAtUtc FROM dbo.maintenance_tickets ORDER BY createdAtUtc DESC",
        r => new MaintenanceTicketDto { Id = S(r,0), Code = S(r,1), RoomNumber = r.IsDBNull(2)?null:S(r,2), Title = S(r,3), Description = r.IsDBNull(4)?null:S(r,4), Priority = S(r,5), Status = S(r,6), AssigneeName = r.IsDBNull(7)?null:S(r,7), CostRial = r.IsDBNull(8)?(long?)null:L(r,8), ReporterName = r.IsDBNull(9)?null:S(r,9), CreatedAtUtc = D(r,10), ResolvedAtUtc = D(r,11) });

    public Task<Result<List<AppUserDto>>> UsersAsync() => QueryAsync(ConnectionString,
        "SELECT id, fullName, username, roleCode, roleTitle, phone, isActive, lastLoginAtUtc FROM dbo.users ORDER BY fullName",
        r => new AppUserDto { Id = S(r,0), FullName = S(r,1), Username = S(r,2), RoleCode = S(r,3), RoleTitle = r.IsDBNull(4)?null:S(r,4), Phone = r.IsDBNull(5)?null:S(r,5), IsActive = B(r,6), LastLoginAtUtc = D(r,7) });

    public Task<Result<List<NotificationDto>>> NotificationsAsync() => QueryAsync(ConnectionString,
        "SELECT id, title, body, severity, kind, createdAtUtc, isRead FROM dbo.notifications ORDER BY createdAtUtc DESC",
        r => new NotificationDto { Id = S(r,0), Title = S(r,1), Body = r.IsDBNull(2)?null:S(r,2), Severity = S(r,3), Kind = r.IsDBNull(4)?null:S(r,4), CreatedAtUtc = D(r,5), IsRead = B(r,6) });

    public async Task<Result<string>> MarkAllNotificationsReadAsync()
    {
        try
        {
            await using var con = await OpenAsync();
            await using var cmd = new SqlCommand("UPDATE dbo.notifications SET isRead=1", con);
            await cmd.ExecuteNonQueryAsync();
            return Result<string>.Good("همهٔ اعلان‌ها خوانده شدند.");
        }
        catch (Exception ex) { return Result<string>.Fail("خطای پایگاه داده: " + ex.Message); }
    }

    public Task<Result<List<DebtorDto>>> DebtorsAsync() => QueryAsync(ConnectionString,
        "SELECT id, firstName + N' ' + lastName AS guestName, phone, NULL AS roomNumber, balanceRial, lastStayAtUtc FROM dbo.guests WHERE balanceRial > 0 ORDER BY balanceRial DESC",
        r => new DebtorDto { GuestId = S(r,0), GuestName = S(r,1), Phone = r.IsDBNull(2)?null:S(r,2), RoomNumber = null, BalanceRial = L(r,3), LastActivityAtUtc = D(r,4) });

    public Task<Result<List<TransactionDto>>> TransactionsAsync(string fromIso, string toIso) => QueryAsync(ConnectionString,
        "SELECT TOP 100 id, code, type, amountRial, method, description, guestName, roomNumber, operatorName, createdAtUtc, isReversed FROM dbo.transactions WHERE CAST(createdAtUtc AS DATE) BETWEEN @f AND @t ORDER BY createdAtUtc DESC",
        r => new TransactionDto { Id = S(r,0), Code = S(r,1), Type = S(r,2), AmountRial = L(r,3), Method = S(r,4), Description = r.IsDBNull(5)?null:S(r,5), GuestName = r.IsDBNull(6)?null:S(r,6), RoomNumber = r.IsDBNull(7)?null:S(r,7), OperatorName = r.IsDBNull(8)?null:S(r,8), CreatedAtUtc = D(r,9), IsReversed = B(r,10) },
        ("@f", fromIso), ("@t", toIso));

    public async Task<Result<CashBoxDto>> CashboxAsync(string dateIso)
    {
        try
        {
            await using var con = await OpenAsync();
            await using var cmd = new SqlCommand(@"
                SELECT
                  ISNULL(SUM(CASE WHEN type='RECEIVE' AND isReversed=0 THEN amountRial ELSE 0 END),0),
                  ISNULL(SUM(CASE WHEN type='PAY' AND isReversed=0 THEN amountRial ELSE 0 END),0),
                  ISNULL(SUM(CASE WHEN type='DISCOUNT' AND isReversed=0 THEN amountRial ELSE 0 END),0),
                  ISNULL(SUM(CASE WHEN isReversed=0 THEN CASE WHEN type IN ('RECEIVE','DISCOUNT') THEN amountRial ELSE -amountRial END ELSE 0 END),0),
                  COUNT(*)
                FROM dbo.transactions WHERE CAST(createdAtUtc AS DATE)=@d", con);
            cmd.Parameters.AddWithValue("@d", dateIso);
            await using var r = await cmd.ExecuteReaderAsync();
            var box = new CashBoxDto { Date = dateIso };
            if (await r.ReadAsync())
            {
                box.ReceivedRial = L(r, 0); box.PaidRial = L(r, 1); box.DiscountsRial = L(r, 2); box.NetRial = L(r, 3); box.TransactionCount = I(r, 4);
            }
            await using var cmd2 = new SqlCommand("SELECT ISNULL(SUM(balanceRial),0) FROM dbo.guests", con);
            box.OpenBalanceRial = Convert.ToInt64(await cmd2.ExecuteScalarAsync());
            await using var cmd3 = new SqlCommand(@"SELECT method, ISNULL(SUM(amountRial),0), COUNT(*) FROM dbo.transactions WHERE CAST(createdAtUtc AS DATE)=@d AND isReversed=0 AND type IN ('RECEIVE','DISCOUNT') GROUP BY method", con);
            cmd3.Parameters.AddWithValue("@d", dateIso);
            await using var r3 = await cmd3.ExecuteReaderAsync();
            box.ByMethod = new List<MethodTotalDto>();
            while (await r3.ReadAsync())
                box.ByMethod.Add(new MethodTotalDto { Method = S(r3, 0), AmountRial = L(r3, 1), Count = I(r3, 2) });
            return Result<CashBoxDto>.Good(box);
        }
        catch (Exception ex) { return Result<CashBoxDto>.Fail("خطای پایگاه داده: " + ex.Message); }
    }

    public async Task<Result<DashboardDto>> DashboardAsync()
    {
        try
        {
            var d = new DashboardDto { BusinessDate = DateTime.Today.ToString("yyyy-MM-dd") };
            await using var con = await OpenAsync();
            async Task<long> Scalar(string sql)
            {
                await using var c = new SqlCommand(sql, con);
                var v = await c.ExecuteScalarAsync();
                return v is null or DBNull ? 0 : Convert.ToInt64(v);
            }
            d.TotalRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms");
            d.FreeRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='FREE'");
            d.ReservedRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='RESERVED'");
            d.OccupiedRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='OCCUPIED'");
            d.CleaningRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='CLEANING'");
            d.MaintenanceRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='MAINTENANCE'");
            d.OutOfServiceRooms = (int)await Scalar("SELECT COUNT(*) FROM dbo.rooms WHERE status='OUT_OF_SERVICE'");
            d.TodayCheckIns = (int)await Scalar("SELECT COUNT(*) FROM dbo.reservations WHERE checkInDate=CAST(GETDATE() AS DATE)");
            d.TodayCheckOuts = (int)await Scalar("SELECT COUNT(*) FROM dbo.reservations WHERE checkOutDate=CAST(GETDATE() AS DATE)");
            d.RevenueTodayRial = await Scalar("SELECT ISNULL(SUM(amountRial),0) FROM dbo.transactions WHERE type='RECEIVE' AND isReversed=0 AND CAST(createdAtUtc AS DATE)=CAST(GETDATE() AS DATE)");
            d.OccupancyPercent = d.TotalRooms == 0 ? 0 : Math.Round(100.0 * (d.OccupiedRooms + d.ReservedRooms) / d.TotalRooms, 1);
            d.OccupancySeries = new List<OccupancyPointDto>();
            for (int i = 6; i >= 0; i--)
            {
                var day = DateTime.Today.AddDays(-i).ToString("yyyy-MM-dd");
                await using var oc = new SqlCommand("SELECT COUNT(DISTINCT roomNumber) FROM dbo.reservations WHERE status IN ('CONFIRMED','CHECKED_IN') AND checkInDate <= @d AND checkOutDate > @d", con);
                oc.Parameters.AddWithValue("@d", day);
                var occ = Convert.ToInt32(await oc.ExecuteScalarAsync() ?? 0);
                d.OccupancySeries.Add(new OccupancyPointDto { Date = day, Occupied = occ, Total = d.TotalRooms });
            }
            d.Alerts = new List<DashboardAlertDto>();
            await using (var c = new SqlCommand("SELECT TOP 5 roomNumber FROM dbo.rooms WHERE status='MAINTENANCE'", con))
            await using (var r = await c.ExecuteReaderAsync())
                while (await r.ReadAsync())
                    d.Alerts.Add(new DashboardAlertDto { Title = "اتاق " + Fa.Digits(S(r, 0)) + " در حال تعمیر است", Message = "تیکت تعمیرات را بررسی کنید", Severity = "HIGH" });
            await using (var c = new SqlCommand("SELECT TOP 5 roomNumber FROM dbo.rooms WHERE status='CLEANING'", con))
            await using (var r = await c.ExecuteReaderAsync())
                while (await r.ReadAsync())
                    d.Alerts.Add(new DashboardAlertDto { Title = "نظافت اتاق " + Fa.Digits(S(r, 0)), Message = "پس از اتمام، وضعیت اتاق را به «خالی» تغییر دهید", Severity = "MEDIUM" });
            d.RecentActivities = new List<DashboardActivityDto>();
            await using (var c = new SqlCommand("SELECT TOP 10 title, createdAtUtc FROM dbo.notifications ORDER BY createdAtUtc DESC", con))
            await using (var r = await c.ExecuteReaderAsync())
                while (await r.ReadAsync())
                    d.RecentActivities.Add(new DashboardActivityDto { Title = S(r, 0), CreatedAtUtc = D(r, 1) });
            return Result<DashboardDto>.Good(d);
        }
        catch (Exception ex) { return Result<DashboardDto>.Fail("خطای پایگاه داده: " + ex.Message); }
    }

    public async Task<Result<ReportDto>> ReportAsync(string key, string fromIso, string toIso)
    {
        try
        {
            await using var con = await OpenAsync();
            var rep = new ReportDto { Type = key, From = fromIso, To = toIso, GeneratedAtUtc = DateTime.UtcNow.ToString("o") };
            if (!DateTime.TryParse(fromIso, out var from) || !DateTime.TryParse(toIso, out var to) || from > to)
                return Result<ReportDto>.Fail("بازهٔ تاریخ نامعتبر است.");
            switch (key)
            {
                case "occupancy":
                    rep.Title = "گزارش اشغال اتاق‌ها"; rep.Columns = new() { "تاریخ", "اشغال", "کل" };
                    for (var d = from; d <= to; d = d.AddDays(1))
                    {
                        var day = d.ToString("yyyy-MM-dd");
                        await using var c = new SqlCommand("SELECT COUNT(DISTINCT roomNumber) FROM dbo.reservations WHERE status IN ('CONFIRMED','CHECKED_IN') AND checkInDate <= @d AND checkOutDate > @d", con);
                        c.Parameters.AddWithValue("@d", day);
                        var occ = Convert.ToInt32(await c.ExecuteScalarAsync() ?? 0);
                        await using var c2 = new SqlCommand("SELECT COUNT(*) FROM dbo.rooms", con);
                        var tot = Convert.ToInt32(await c2.ExecuteScalarAsync());
                        rep.Rows ??= new(); rep.Rows.Add(new() { day, occ.ToString(), tot.ToString() });
                    }
                    break;
                case "check-in-out":
                    rep.Title = "گزارش ورود و خروج"; rep.Columns = new() { "کد", "مهمان", "اتاق", "ورود", "خروج", "وضعیت" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT code, guestName, roomNumber, CONVERT(NVARCHAR(10), checkInDate, 111), CONVERT(NVARCHAR(10), checkOutDate, 111), status FROM dbo.reservations WHERE checkInDate BETWEEN @f AND @t OR checkOutDate BETWEEN @f AND @t ORDER BY checkInDate", r => new List<string> { S(r,0), S(r,1), S(r,2), S(r,3), S(r,4), S(r,5) }, ("@f", fromIso), ("@t", toIso))).Value ?? new();
                    break;
                case "reservations":
                    rep.Title = "گزارش رزروها"; rep.Columns = new() { "کد", "مهمان", "اتاق", "ورود", "خروج", "مبلغ", "وضعیت" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT TOP 200 code, guestName, roomNumber, CONVERT(NVARCHAR(10), checkInDate, 111), CONVERT(NVARCHAR(10), checkOutDate, 111), CAST(totalRial AS NVARCHAR(30)), status FROM dbo.reservations ORDER BY checkInDate DESC", r => new List<string> { S(r,0), S(r,1), S(r,2), S(r,3), S(r,4), S(r,5), S(r,6) })).Value ?? new();
                    break;
                case "revenue":
                    rep.Title = "گزارش درآمد"; rep.Columns = new() { "روش پرداخت", "تعداد", "مبلغ (ریال)" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT method, CAST(COUNT(*) AS NVARCHAR(10)), CAST(SUM(amountRial) AS NVARCHAR(30)) FROM dbo.transactions WHERE isReversed=0 AND CAST(createdAtUtc AS DATE) BETWEEN @f AND @t AND type IN ('RECEIVE','DISCOUNT') GROUP BY method", r => new List<string> { S(r,0), S(r,1), S(r,2) }, ("@f", fromIso), ("@t", toIso))).Value ?? new();
                    rep.Totals = new() { "جمع دریافتی بازه: " + (await ScalarStatic(ConnectionString, "SELECT ISNULL(SUM(amountRial),0) FROM dbo.transactions WHERE isReversed=0 AND CAST(createdAtUtc AS DATE) BETWEEN @f AND @t AND type IN ('RECEIVE','DISCOUNT')", ("@f", fromIso), ("@t", toIso))).ToString() };
                    break;
                case "debtors":
                    rep.Title = "گزارش بدهکاران"; rep.Columns = new() { "مهمان", "تلفن", "مانده (ریال)" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT firstName + N' ' + lastName, phone, CAST(balanceRial AS NVARCHAR(30)) FROM dbo.guests WHERE balanceRial > 0 ORDER BY balanceRial DESC", r => new List<string> { S(r,0), S(r,1), S(r,2) })).Value ?? new();
                    break;
                case "staff-performance":
                    rep.Title = "گزارش عملکرد کارکنان"; rep.Columns = new() { "کاربر", "تعداد تراکنش", "مجموع (ریال)" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT operatorName, CAST(COUNT(*) AS NVARCHAR(10)), CAST(SUM(amountRial) AS NVARCHAR(30)) FROM dbo.transactions WHERE isReversed=0 AND CAST(createdAtUtc AS DATE) BETWEEN @f AND @t GROUP BY operatorName", r => new List<string> { S(r,0), S(r,1), S(r,2) }, ("@f", fromIso), ("@t", toIso))).Value ?? new();
                    break;
                case "housekeeping":
                    rep.Title = "گزارش خدمات"; rep.Columns = new() { "اتاق", "نوع", "وضعیت", "مسئول" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT roomNumber, type, status, ISNULL(assignedToName,N'-') FROM dbo.housekeeping_tasks ORDER BY createdAtUtc DESC", r => new List<string> { S(r,0), S(r,1), S(r,2), S(r,3) })).Value ?? new();
                    break;
                case "maintenance":
                    rep.Title = "گزارش تعمیرات"; rep.Columns = new() { "کد", "عنوان", "اتاق", "اولویت", "وضعیت", "هزینه" };
                    rep.Rows = (await QueryAsync(ConnectionString, "SELECT code, title, ISNULL(roomNumber,N'-'), priority, status, ISNULL(CAST(costRial AS NVARCHAR(30)),N'-') FROM dbo.maintenance_tickets ORDER BY createdAtUtc DESC", r => new List<string> { S(r,0), S(r,1), S(r,2), S(r,3), S(r,4), S(r,5) })).Value ?? new();
                    break;
                default:
                    return Result<ReportDto>.Fail("نوع گزارش ناشناخته: " + key);
            }
            return Result<ReportDto>.Good(rep);
        }
        catch (Exception ex) { return Result<ReportDto>.Fail("خطای گزارش: " + ex.Message); }
    }

    private static async Task<long> ScalarStatic(string cs, string sql, params (string, object)[] ps)
    {
        await using var con = new SqlConnection(cs);
        await con.OpenAsync();
        await using var c = new SqlCommand(sql, con);
        foreach (var (n, val) in ps) c.Parameters.AddWithValue(n, val);
        var v = await c.ExecuteScalarAsync();
        return v is null or DBNull ? 0 : Convert.ToInt64(v);
    }
}
