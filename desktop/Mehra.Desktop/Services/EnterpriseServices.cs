using System.Diagnostics;
using System.Text;
using System.Text.Json;

namespace Mehra.Desktop;

public class SmsSettings
{
    public bool Enabled { get; set; } = false;
    public string ApiKey { get; set; } = "";
    public string SenderNumber { get; set; } = "3000505";
}

public static class SmsService
{
    private static readonly string SettingsPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Mehra", "sms_settings.json");

    public static SmsSettings LoadSettings()
    {
        try
        {
            if (File.Exists(SettingsPath))
                return JsonSerializer.Deserialize<SmsSettings>(File.ReadAllText(SettingsPath)) ?? new();
        }
        catch { }
        return new();
    }

    public static void SaveSettings(SmsSettings s)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(SettingsPath)!);
            File.WriteAllText(SettingsPath, JsonSerializer.Serialize(s, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { }
    }

    public static async Task<bool> SendWelcomeSmsAsync(string phone, string guestName, string roomNumber, string code, string checkOutFa)
    {
        var s = LoadSettings();
        if (!s.Enabled || string.IsNullOrWhiteSpace(s.ApiKey) || string.IsNullOrWhiteSpace(phone))
            return false;

        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
            var msg = $"مهمان گرامی {guestName}، به مهمان‌پذیر فراسا خوش آمدید.\nاتاق: {roomNumber}\nکد قبض: {code}\nتاریخ تخلیه: {checkOutFa} (ساعت ۱۲:۰۰)";
            var url = $"https://api.kavenegar.com/v1/{Uri.EscapeDataString(s.ApiKey)}/sms/send.json?receptor={Uri.EscapeDataString(phone)}&sender={Uri.EscapeDataString(s.SenderNumber)}&message={Uri.EscapeDataString(msg)}";
            var res = await http.GetAsync(url);
            return res.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }
}

public static class ExportService
{
    public static string ExportCsv(string filePrefix, string[] headers, IEnumerable<string[]> rows)
    {
        var desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
        if (string.IsNullOrWhiteSpace(desktop) || !Directory.Exists(desktop))
            desktop = Path.GetTempPath();

        var file = Path.Combine(desktop, $"{filePrefix}-{DateTime.Now:yyyyMMdd-HHmm}.csv");
        var sb = new StringBuilder();
        sb.AppendLine(string.Join(",", headers.Select(Escape)));
        foreach (var r in rows)
            sb.AppendLine(string.Join(",", r.Select(Escape)));

        // UTF-8 with BOM so Microsoft Excel opens Persian text cleanly
        File.WriteAllText(file, sb.ToString(), new UTF8Encoding(true));
        try { Process.Start(new ProcessStartInfo(file) { UseShellExecute = true }); } catch { }
        return file;
    }

    private static string Escape(string? v)
    {
        var s = (v ?? "").Replace("\"", "\"\"");
        return $"\"{s}\"";
    }
}

public class BankAccountItem
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N")[..6];
    public string BankName { get; set; } = "";
    public string CardNumber { get; set; } = "";
    public string OwnerName { get; set; } = "";
    public bool IsDefault { get; set; }
    public override string ToString() => $"{BankName} — {CardNumber} ({OwnerName})";
}

public static class BankAccountsStore
{
    private static readonly string PathFile = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Mehra", "bank_accounts.json");

    public static List<BankAccountItem> Load()
    {
        try
        {
            if (File.Exists(PathFile))
            {
                var list = JsonSerializer.Deserialize<List<BankAccountItem>>(File.ReadAllText(PathFile));
                if (list != null && list.Count > 0) return list;
            }
        }
        catch { }
        return new List<BankAccountItem>
        {
            new() { BankName = "بانک ملی (پیش‌فرض)", CardNumber = "۶۰۳۷-۹۹۷۰-۰۰۰۰-۰۰۰۰", OwnerName = "مهمان‌پذیر فرهنگیان ناحیه ۳ اهواز", IsDefault = true }
        };
    }

    public static void Save(List<BankAccountItem> list)
    {
        try { Directory.CreateDirectory(Path.GetDirectoryName(PathFile)!); File.WriteAllText(PathFile, JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true })); } catch { }
    }
}

public class OrgContractRecord
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N")[..6];
    public string OrgName { get; set; } = "";
    public string DiscountPercent { get; set; } = "۲۰";
    public string ContactPerson { get; set; } = "";
    public string CreatedAt { get; set; } = Fa.DateLong(DateTime.Today);
}

public static class OrgContractsService
{
    private static readonly string PathFile = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Mehra", "org_contracts.json");

    public static List<OrgContractRecord> Load()
    {
        try { if (File.Exists(PathFile)) return JsonSerializer.Deserialize<List<OrgContractRecord>>(File.ReadAllText(PathFile)) ?? new(); } catch { }
        return new();
    }

    public static void Save(List<OrgContractRecord> list)
    {
        try { Directory.CreateDirectory(Path.GetDirectoryName(PathFile)!); File.WriteAllText(PathFile, JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true })); } catch { }
    }
}

public class ParkingRecord
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N")[..8];
    public string Plate { get; set; } = "";
    public string GuestName { get; set; } = "";
    public string RoomNumber { get; set; } = "";
    public string EnteredAt { get; set; } = Fa.DateLong(DateTime.Today) + " " + DateTime.Now.ToString("HH:mm");
}

public static class ParkingService
{
    private static readonly string PathFile = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Mehra", "parking.json");

    public static List<ParkingRecord> Load()
    {
        try { if (File.Exists(PathFile)) return JsonSerializer.Deserialize<List<ParkingRecord>>(File.ReadAllText(PathFile)) ?? new(); } catch { }
        return new();
    }

    public static void Save(List<ParkingRecord> list)
    {
        try { Directory.CreateDirectory(Path.GetDirectoryName(PathFile)!); File.WriteAllText(PathFile, JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true })); } catch { }
    }
}

public static class ShiftHandoverPrinter
{
    public static void PrintHandover(string operatorName, long cashRial, long posRial, string notes)
    {
        var html = $@"<!DOCTYPE html><html lang=""fa"" dir=""rtl""><head><meta charset=""utf-8"" /><title>گزارش تحویل شیفت — فراسا</title>
<style>body{{font-family:Vazirmatn,Tahoma,sans-serif;background:#F7F3E9;color:#16233D;padding:24px}}.sheet{{max-width:640px;margin:0 auto;background:#fff;border:2px solid #C9A24B;border-radius:14px;padding:24px}}table{{width:100%;border-collapse:collapse;margin:14px 0}}th,td{{border:1px solid #E4D9BE;padding:10px;text-align:right}}th{{background:#F7F3E9}}</style></head>
<body><div class=""sheet""><h2>گزارش تحویل شیفت پذیرش — فراسا</h2>
<p><strong>تحویل‌دهنده:</strong> {System.Net.WebUtility.HtmlEncode(operatorName)} | <strong>تاریخ:</strong> {Fa.DateLong(DateTime.Today)} - {Fa.Digits(DateTime.Now.ToString("HH:mm"))}</p>
<table><tr><th>موجودی صندوق نقدی</th><td>{Fa.Money(cashRial)}</td></tr>
<tr><th>جمع تراکنش‌های دستگاه پوز</th><td>{Fa.Money(posRial)}</td></tr>
<tr><th>جمع کل تحویلی</th><td><strong>{Fa.Money(cashRial + posRial)}</strong></td></tr></table>
<p><strong>توضیحات شیفت:</strong> {System.Net.WebUtility.HtmlEncode(string.IsNullOrWhiteSpace(notes) ? "—" : notes)}</p>
<button onclick=""window.print()"" style=""background:#B08D3E;color:#fff;border:none;padding:10px 20px;border-radius:8px;cursor:pointer"">چاپ گزارش شیفت</button>
</div></body></html>";
        var file = Path.Combine(Path.GetTempPath(), $"farasa-shift-{Guid.NewGuid():N}.html");
        File.WriteAllText(file, html, Encoding.UTF8);
        try { Process.Start(new ProcessStartInfo(file) { UseShellExecute = true }); } catch { }
    }
}
