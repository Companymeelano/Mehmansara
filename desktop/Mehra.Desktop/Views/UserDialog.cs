using System.Text.Json;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

public static class UserPermissionsStore
{
    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Mehra", "user_permissions.json");

    public static readonly (string Tag, string Label, bool IsAdminOnly)[] AllPages =
    {
        ("home", "🏠 خانه", false),
        ("rooms", "🛏 اتاق‌ها (مدیریتی)", true),
        ("reservations", "📅 رزروها", false),
        ("reception", "🔑 پذیرش", false),
        ("guests", "👥 مهمانان", false),
        ("finance", "💳 مالی (مدیریتی)", true),
        ("housekeeping", "🧹 نظافت", false),
        ("maintenance", "🔧 تعمیرات", false),
        ("reports", "📊 گزارش‌ها (مدیریتی)", true),
        ("users", "👤 کاربران (مدیریتی)", true),
        ("notifications", "🔔 اعلان‌ها", false),
        ("settings", "⚙ تنظیمات (مدیریتی)", true)
    };

    public static Dictionary<string, List<string>> LoadAll()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var json = File.ReadAllText(FilePath);
                return JsonSerializer.Deserialize<Dictionary<string, List<string>>>(json) ?? new();
            }
        }
        catch { }
        return new();
    }

    public static void SaveUserPermissions(string username, List<string> allowedPages)
    {
        try
        {
            var dir = Path.GetDirectoryName(FilePath)!;
            Directory.CreateDirectory(dir);
            var all = LoadAll();
            all[username.Trim().ToLowerInvariant()] = allowedPages;
            File.WriteAllText(FilePath, JsonSerializer.Serialize(all, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { }
    }

    public static HashSet<string> GetAllowedPages(string? username, string? roleCode)
    {
        var key = (username ?? "").Trim().ToLowerInvariant();
        if (key == "admin" || string.Equals(roleCode, "admin", StringComparison.OrdinalIgnoreCase))
            return AllPages.Select(p => p.Tag).ToHashSet();

        var all = LoadAll();
        if (all.TryGetValue(key, out var list) && list.Count > 0)
            return list.ToHashSet();

        return (roleCode ?? "").ToLowerInvariant() switch
        {
            "reception" => new HashSet<string> { "home", "reservations", "reception", "guests", "notifications" },
            "finance" => new HashSet<string> { "home", "reservations", "finance", "reports", "notifications" },
            "housekeeping" => new HashSet<string> { "home", "housekeeping", "notifications" },
            "maintenance" => new HashSet<string> { "home", "maintenance", "notifications" },
            _ => new HashSet<string> { "home", "reservations", "reception", "guests", "notifications" }
        };
    }
}

public class UserDialog : Window
{
    private readonly TextBox _fullNameBox = new() { Watermark = "نام و نام خانوادگی" };
    private readonly TextBox _usernameBox = new() { Watermark = "نام کاربری (انگلیسی)" };
    private readonly TextBox _passwordBox = new() { PasswordChar = '•', Watermark = "رمز عبور" };
    private readonly TextBox _phoneBox = new() { Watermark = "شماره تماس" };
    private readonly ComboBox _roleCombo = new() { HorizontalAlignment = HorizontalAlignment.Stretch };
    private readonly Dictionary<string, CheckBox> _pageChecks = new();
    private readonly TextBlock _errText = new() { Foreground = Brush.Parse("#B3261E"), IsVisible = false };

    public string FullName => _fullNameBox.Text?.Trim() ?? "";
    public string Username => _usernameBox.Text?.Trim() ?? "";
    public string Password => _passwordBox.Text ?? "";
    public string Phone => _phoneBox.Text?.Trim() ?? "";
    public string RoleTitle => (_roleCombo.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "پذیرش";
    public List<string> AllowedPages => _pageChecks.Where(kv => kv.Value.IsChecked == true).Select(kv => kv.Key).ToList();

    public UserDialog()
    {
        Title = "افزودن کاربر و تعیین سطح دسترسی";
        Width = 540;
        Height = 620;
        FlowDirection = FlowDirection.RightToLeft;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        Background = Brush.Parse("#F7F3E9");

        _roleCombo.Items.Add(new ComboBoxItem { Content = "پذیرش (دسترسی محدود)", Tag = "reception" });
        _roleCombo.Items.Add(new ComboBoxItem { Content = "حسابدار (امور مالی و گزارش)", Tag = "finance" });
        _roleCombo.Items.Add(new ComboBoxItem { Content = "خانه‌داری (نظافت)", Tag = "housekeeping" });
        _roleCombo.Items.Add(new ComboBoxItem { Content = "تأسیسات (تعمیرات)", Tag = "maintenance" });
        _roleCombo.Items.Add(new ComboBoxItem { Content = "مدیر کل (دسترسی کامل)", Tag = "admin" });
        _roleCombo.Items.Add(new ComboBoxItem { Content = "سفارشی", Tag = "custom" });
        _roleCombo.SelectedIndex = 0;
        _roleCombo.SelectionChanged += (_, _) => ApplyRolePreset();

        var root = new StackPanel { Margin = new Thickness(20), Spacing = 10 };
        root.Children.Add(new TextBlock { Text = "مشخصات کاربر و سطح دسترسی", FontSize = 18, FontWeight = FontWeight.Bold, Foreground = Brush.Parse("#16233D") });

        var grid = new Grid { ColumnDefinitions = new ColumnDefinitions("*,*"), RowDefinitions = new RowDefinitions("Auto,Auto,Auto,Auto") };
        AddField(grid, 0, 0, "نام و نام خانوادگی *", _fullNameBox);
        AddField(grid, 0, 1, "نام کاربری (انگلیسی) *", _usernameBox);
        AddField(grid, 1, 0, "رمز عبور *", _passwordBox);
        AddField(grid, 1, 1, "تلفن", _phoneBox);
        root.Children.Add(grid);

        root.Children.Add(new TextBlock { Text = "نقش کاربر", FontWeight = FontWeight.SemiBold, FontSize = 12 });
        root.Children.Add(_roleCombo);

        root.Children.Add(new TextBlock { Text = "دسترسی به بخش‌های برنامه (بخش‌های مدیریتی برای کاربران عادی مخفی می‌شوند):", FontWeight = FontWeight.SemiBold, FontSize = 12, Margin = new Thickness(0, 6, 0, 0) });

        var permGrid = new Grid { ColumnDefinitions = new ColumnDefinitions("*,*") };
        for (int i = 0; i < UserPermissionsStore.AllPages.Length; i++)
        {
            permGrid.RowDefinitions.Add(new RowDefinition(GridLength.Auto));
            var (tag, label, _) = UserPermissionsStore.AllPages[i];
            var cb = new CheckBox { Content = label, Margin = new Thickness(4, 3) };
            _pageChecks[tag] = cb;
            Grid.SetRow(cb, i / 2);
            Grid.SetColumn(cb, i % 2);
            permGrid.Children.Add(cb);
        }

        var permBorder = new Border
        {
            Background = Brushes.White,
            BorderBrush = Brush.Parse("#E4D9BE"),
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(10),
            Padding = new Thickness(10),
            Child = permGrid
        };
        root.Children.Add(permBorder);
        root.Children.Add(_errText);

        var btnRow = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Spacing = 8, Margin = new Thickness(0, 8, 0, 0) };
        var cancelBtn = new Button { Content = "انصراف" };
        cancelBtn.Classes.Add("ghost");
        cancelBtn.Click += (_, _) => Close(false);

        var okBtn = new Button { Content = "ثبت کاربر و ذخیره دسترسی" };
        okBtn.Classes.Add("primary");
        okBtn.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(FullName) || string.IsNullOrWhiteSpace(Username) || string.IsNullOrWhiteSpace(Password))
            {
                _errText.Text = "نام، نام کاربری و رمز عبور الزامی است.";
                _errText.IsVisible = true;
                return;
            }
            Close(true);
        };
        btnRow.Children.Add(cancelBtn);
        btnRow.Children.Add(okBtn);
        root.Children.Add(btnRow);

        Content = new ScrollViewer { Content = root };
        ApplyRolePreset();
    }

    private static void AddField(Grid g, int row, int col, string label, Control ctrl)
    {
        var sp = new StackPanel { Spacing = 4, Margin = new Thickness(4) };
        sp.Children.Add(new TextBlock { Text = label, FontSize = 12, FontWeight = FontWeight.SemiBold });
        sp.Children.Add(ctrl);
        Grid.SetRow(sp, row);
        Grid.SetColumn(sp, col);
        g.Children.Add(sp);
    }

    private void ApplyRolePreset()
    {
        var tag = (_roleCombo.SelectedItem as ComboBoxItem)?.Tag?.ToString() ?? "reception";
        if (tag == "custom") return;
        var allowed = UserPermissionsStore.GetAllowedPages(tag == "admin" ? "admin" : "", tag);
        foreach (var kv in _pageChecks)
            kv.Value.IsChecked = allowed.Contains(kv.Key);
    }
}
