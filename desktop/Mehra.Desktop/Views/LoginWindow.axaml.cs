using Avalonia.Controls;

namespace Mehra.Desktop.Views;

public partial class LoginWindow : Window
{
    public LoginWindow()
    {
        InitializeComponent();
        var saved = SettingsStore.LoadServerUrl();
        if (saved.Length > 0) this.FindControl<TextBox>("ServerBox")!.Text = saved;
    }

    private async void OnLogin(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var server = this.FindControl<TextBox>("ServerBox")!.Text ?? "";
        var user = this.FindControl<TextBox>("UserBox")!.Text ?? "";
        var pass = this.FindControl<PasswordBox>("PassBox")!.Password ?? "";
        var error = this.FindControl<TextBlock>("ErrorText")!;
        error.IsVisible = false;
        if (user.Trim().Length == 0 || pass.Length == 0)
        {
            error.Text = "نام کاربری و رمز عبور را وارد کنید.";
            error.IsVisible = true;
            return;
        }
        var api = new ApiClient();
        var result = await api.LoginAsync(server, user.Trim(), pass);
        if (!result.Ok)
        {
            error.Text = result.Error;
            error.IsVisible = true;
            return;
        }
        SettingsStore.SaveServerUrl(server);
        OpenMain(new Session(api, result.Value!));
    }

    private async void OnDemo(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var api = new DemoApi();
        var result = await api.LoginAsync("", "demo", "");
        OpenMain(new Session(api, result.Value!));
    }

    private void OpenMain(Session session)
    {
        var main = new MainWindow(session);
        main.Show();
        Close();
    }
}

public static class SettingsStore
{
    private static string FilePath()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "mehra");
        Directory.CreateDirectory(dir);
        return Path.Combine(dir, "settings.json");
    }

    public static string LoadServerUrl()
    {
        try
        {
            var f = FilePath();
            if (!File.Exists(f)) return "";
            using var doc = System.Text.Json.JsonDocument.Parse(File.ReadAllText(f));
            return doc.RootElement.TryGetProperty("serverUrl", out var el) ? el.GetString() ?? "" : "";
        }
        catch { return ""; }
    }

    public static void SaveServerUrl(string url)
    {
        try
        {
            File.WriteAllText(FilePath(),
                System.Text.Json.JsonSerializer.Serialize(new { serverUrl = url }));
        }
        catch { /* non-fatal */ }
    }
}
