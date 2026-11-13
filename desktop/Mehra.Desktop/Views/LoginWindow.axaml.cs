using Avalonia.Controls;

namespace Mehra.Desktop.Views;

public partial class LoginWindow : Window
{
    public LoginWindow()
    {
        InitializeComponent();
        var s = SettingsStore.Load();
        this.FindControl<ComboBox>("ModeCombo")!.SelectedIndex = s.Mode switch { "api" => 1, "sql" => 2, _ => 0 };
        this.FindControl<TextBox>("ServerBox")!.Text = s.ServerUrl;
        this.FindControl<TextBox>("SqlServerBox")!.Text = s.SqlServer;
        this.FindControl<TextBox>("SqlDbBox")!.Text = s.SqlDb;
        this.FindControl<ComboBox>("SqlAuthCombo")!.SelectedIndex = s.SqlAuth == "sql" ? 1 : 0;
        this.FindControl<TextBox>("SqlUserBox")!.Text = s.SqlUser;
        ApplyModeVisibility();
    }

    private void OnModeChanged(object? sender, SelectionChangedEventArgs e) => ApplyModeVisibility();
    private void OnSqlAuthChanged(object? sender, SelectionChangedEventArgs e)
    {
        var win = (this.FindControl<ComboBox>("SqlAuthCombo")!.SelectedItem as ComboBoxItem)?.Tag as string == "win";
        this.FindControl<StackPanel>("SqlCredPanel")!.IsVisible = !win;
    }

    private string Mode => (this.FindControl<ComboBox>("ModeCombo")!.SelectedItem as ComboBoxItem)?.Tag as string ?? "demo";

    private void ApplyModeVisibility()
    {
        var m = Mode;
        this.FindControl<StackPanel>("ApiPanel")!.IsVisible = m == "api";
        this.FindControl<StackPanel>("SqlPanel")!.IsVisible = m == "sql";
    }

    private async void OnLogin(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var user = this.FindControl<TextBox>("UserBox")!.Text ?? "";
        var pass = this.FindControl<TextBox>("PassBox")!.Text ?? "";
        var error = this.FindControl<TextBlock>("ErrorText")!;
        error.IsVisible = false;
        var mode = Mode;

        if (mode is "api" or "sql" && (user.Trim().Length == 0 || pass.Length == 0))
        {
            error.Text = "نام کاربری و رمز عبور را وارد کنید.";
            error.IsVisible = true;
            return;
        }

        IHotelApi api;
        Result<SessionUser> result;
        if (mode == "demo")
        {
            api = new DemoApi();
            result = await api.LoginAsync("", "demo", "");
        }
        else if (mode == "api")
        {
            var server = this.FindControl<TextBox>("ServerBox")!.Text ?? "";
            api = new ApiClient();
            result = await api.LoginAsync(server, user.Trim(), pass);
            if (result.Ok) SettingsStore.Save(new AppSettings { Mode = "api", ServerUrl = server });
        }
        else
        {
            var server = this.FindControl<TextBox>("SqlServerBox")!.Text?.Trim() ?? "";
            var db = this.FindControl<TextBox>("SqlDbBox")!.Text?.Trim() ?? "Mehra";
            var winAuth = (this.FindControl<ComboBox>("SqlAuthCombo")!.SelectedItem as ComboBoxItem)?.Tag as string == "win";
            var sqlUser = this.FindControl<TextBox>("SqlUserBox")!.Text ?? "";
            var sqlPass = this.FindControl<TextBox>("SqlPassBox")!.Text ?? "";
            if (server.Length == 0) { error.Text = "نام سرور SQL Server را وارد کنید (مثال: localhost یا (localdb)\\MSSQLLocalDB)."; error.IsVisible = true; return; }
            // SQL Server 2022 may be installed as a normal instance (localhost / named instance),
            // not LocalDB — resolve against a candidate list and remember the working server.
            var resolved = await SqlServerApi.ResolveAsync(server, db, winAuth, winAuth ? null : sqlUser, winAuth ? null : sqlPass);
            if (!resolved.Ok) { error.Text = resolved.Error; error.IsVisible = true; return; }
            server = resolved.Value.ResolvedServer;
            api = resolved.Value.Api;
            result = await api.LoginAsync(server, user.Trim(), pass);
            if (result.Ok)
                SettingsStore.Save(new AppSettings { Mode = "sql", SqlServer = server, SqlDb = db, SqlAuth = winAuth ? "win" : "sql", SqlUser = sqlUser });
        }

        if (!result.Ok)
        {
            error.Text = result.Error;
            error.IsVisible = true;
            return;
        }
        OpenMain(new Session(api, result.Value!));
    }

    private void OpenMain(Session session)
    {
        var main = new MainWindow(session);
        main.Show();
        Close();
    }
}

public class AppSettings
{
    public string Mode { get; set; } = "demo";
    public string ServerUrl { get; set; } = "";
    public string SqlServer { get; set; } = "(localdb)\\MSSQLLocalDB";
    public string SqlDb { get; set; } = "Mehra";
    public string SqlAuth { get; set; } = "win";
    public string SqlUser { get; set; } = "";
}

public static class SettingsStore
{
    private static string FilePath()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "mehra");
        Directory.CreateDirectory(dir);
        return Path.Combine(dir, "settings.json");
    }

    public static AppSettings Load()
    {
        try
        {
            var f = FilePath();
            if (!File.Exists(f)) return new AppSettings();
            return System.Text.Json.JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(f)) ?? new AppSettings();
        }
        catch { return new AppSettings(); }
    }

    public static void Save(AppSettings s)
    {
        try
        {
            File.WriteAllText(FilePath(), System.Text.Json.JsonSerializer.Serialize(s));
        }
        catch { /* non-fatal */ }
    }

    public static string LoadServerUrl() => Load().ServerUrl;
    public static void SaveServerUrl(string url) => Save(new AppSettings { Mode = "api", ServerUrl = url });
}
