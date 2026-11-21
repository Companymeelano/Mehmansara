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
