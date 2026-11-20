using System.Globalization;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

/// <summary>
/// Smart Room Add/Edit dialog (v1.2.4):
/// - Floor ComboBox including همکف (0)
/// - Category: سوئیت (default) / عمومی
/// - Bed count 1..12 تخته (auto-syncs capacity, still manually editable)
/// - 5 pre-checked amenity checkboxes
/// - Live 3-digit thousand-separator formatting on nightly rate
/// </summary>
public class RoomDialog : Window
{
    public string RoomNumber { get; private set; } = "";
    public int Floor { get; private set; }
    public int Capacity { get; private set; } = 2;
    public string RoomTypeName { get; private set; } = "سوئیت";
    public long RatePerNightRial { get; private set; }
    public string Amenities { get; private set; } = "";

    private static readonly string[] DefaultAmenities =
    {
        "کولر گازی",
        "تلویزیون",
        "میز غذاخوری",
        "یخچال",
        "سرویس بهداشتی اختصاصی"
    };

    public RoomDialog(string title, string? roomNum = null, int? floor = null, int? capacity = null,
        string? roomType = null, long? rate = null, string? amenities = null)
    {
        Title = title;
        Width = 540;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var isNew = string.IsNullOrWhiteSpace(roomNum);
        var root = new StackPanel { Margin = new Thickness(22), Spacing = 12 };
        root.Children.Add(new TextBlock
        {
            Text = title, FontSize = 16, Classes = { "bold" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D"))
        });

        // Room Number
        root.Children.Add(new TextBlock { Text = "شمارهٔ اتاق *", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var numBox = new TextBox { Text = roomNum ?? "", Watermark = "مثلاً 101" };
        root.Children.Add(numBox);

        // Floor + Category row
        var row1 = new Grid { ColumnDefinitions = new ColumnDefinitions("*,12,*") };
        var floorPanel = new StackPanel { Spacing = 4 };
        floorPanel.Children.Add(new TextBlock { Text = "طبقه", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var floorCombo = new ComboBox
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            ItemsSource = new[] { "همکف", "طبقه ۱", "طبقه ۲", "طبقه ۳", "طبقه ۴", "طبقه ۵" },
            SelectedIndex = floor.HasValue ? Math.Clamp(floor.Value, 0, 5) : 0
        };
        floorPanel.Children.Add(floorCombo);
        Grid.SetColumn(floorPanel, 0);
        row1.Children.Add(floorPanel);

        var catPanel = new StackPanel { Spacing = 4 };
        catPanel.Children.Add(new TextBlock { Text = "نوع اتاق", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var catCombo = new ComboBox
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            ItemsSource = new[] { "سوئیت", "عمومی" },
            SelectedIndex = (roomType ?? "").Contains("عمومی") ? 1 : 0
        };
        catPanel.Children.Add(catCombo);
        Grid.SetColumn(catPanel, 2);
        row1.Children.Add(catPanel);
        root.Children.Add(row1);

        // Beds (1..12) + Capacity row
        var row2 = new Grid { ColumnDefinitions = new ColumnDefinitions("*,12,*") };
        var bedPanel = new StackPanel { Spacing = 4 };
        bedPanel.Children.Add(new TextBlock { Text = "تعداد تخت", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var bedItems = Enumerable.Range(1, 12).Select(i => $"{i} تخته").ToArray();
        var initBeds = capacity.HasValue ? Math.Clamp(capacity.Value, 1, 12) : 2;
        var bedCombo = new ComboBox
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            ItemsSource = bedItems,
            SelectedIndex = initBeds - 1
        };
        bedPanel.Children.Add(bedCombo);
        Grid.SetColumn(bedPanel, 0);
        row2.Children.Add(bedPanel);

        var capPanel = new StackPanel { Spacing = 4 };
        capPanel.Children.Add(new TextBlock { Text = "ظرفیت (تعداد نفرات)", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var capBox = new TextBox { Text = (capacity ?? initBeds).ToString() };
        capPanel.Children.Add(capBox);
        Grid.SetColumn(capPanel, 2);
        row2.Children.Add(capPanel);
        root.Children.Add(row2);

        bedCombo.SelectionChanged += (_, _) =>
        {
            if (bedCombo.SelectedIndex >= 0)
                capBox.Text = (bedCombo.SelectedIndex + 1).ToString();
        };

        // Nightly Rate with live 3-digit separator
        root.Children.Add(new TextBlock { Text = "نرخ هر شب (ریال)", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var rateBox = new TextBox { Text = (rate ?? 2_500_000).ToString("N0", CultureInfo.InvariantCulture) };
        bool formatting = false;
        rateBox.TextChanged += (_, _) =>
        {
            if (formatting) return;
            var digits = new string((rateBox.Text ?? "").Where(char.IsDigit).ToArray());
            if (long.TryParse(digits, out var val))
            {
                formatting = true;
                var formatted = val.ToString("N0", CultureInfo.InvariantCulture);
                if (rateBox.Text != formatted)
                {
                    rateBox.Text = formatted;
                    rateBox.CaretIndex = formatted.Length;
                }
                formatting = false;
            }
        };
        root.Children.Add(rateBox);

        // Amenities Checkboxes
        root.Children.Add(new TextBlock { Text = "امکانات اتاق", FontWeight = FontWeight.Bold, FontSize = 12.5 });
        var amenWrap = new WrapPanel { Orientation = Orientation.Horizontal };
        var checks = new List<CheckBox>();
        foreach (var am in DefaultAmenities)
        {
            var cb = new CheckBox
            {
                Content = am,
                Margin = new Thickness(0, 2, 14, 2),
                IsChecked = isNew || (amenities ?? "").Contains(am)
            };
            checks.Add(cb);
            amenWrap.Children.Add(cb);
        }
        root.Children.Add(amenWrap);

        var err = new TextBlock { Foreground = Brushes.Red, IsVisible = false };
        root.Children.Add(err);

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید و ثبت", Classes = { "primary" } };
        var cancel = new Button
        {
            Content = "انصراف",
            Classes = { "ghost" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D")),
            BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6"))
        };
        ok.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(numBox.Text))
            {
                err.Text = "شمارهٔ اتاق را وارد کنید.";
                err.IsVisible = true;
                return;
            }
            RoomNumber = numBox.Text.Trim();
            Floor = Math.Max(0, floorCombo.SelectedIndex);
            Capacity = int.TryParse(capBox.Text, out var c) && c > 0 ? c : 1;
            var cat = catCombo.SelectedIndex == 1 ? "عمومی" : "سوئیت";
            var beds = (bedCombo.SelectedIndex + 1) + " تخته";
            RoomTypeName = $"{cat} ({beds})";
            var rawDigits = new string((rateBox.Text ?? "").Where(char.IsDigit).ToArray());
            RatePerNightRial = long.TryParse(rawDigits, out var r) ? r : 0;
            Amenities = string.Join("، ", checks.Where(x => x.IsChecked == true).Select(x => x.Content?.ToString() ?? ""));
            Close(true);
        };
        cancel.Click += (_, _) => Close(false);
        buttons.Children.Add(ok);
        buttons.Children.Add(cancel);
        root.Children.Add(buttons);

        Content = new Border
        {
            Background = new SolidColorBrush(Color.Parse("#F7F3E9")),
            BorderBrush = new SolidColorBrush(Color.Parse("#C9A24B")),
            BorderThickness = new Thickness(1),
            Child = root
        };
    }
}

public class RoomStatusDialog : Window
{
    public string RoomNumber { get; private set; } = "";
    public string NewStatus { get; private set; } = "FREE";

    private static readonly (string Code, string Label)[] Statuses =
    {
        ("FREE", "آزاد / آماده پذیرش"),
        ("CLEANING", "در حال نظافت"),
        ("MAINTENANCE", "در حال تعمیر"),
        ("OUT_OF_SERVICE", "خارج از سرویس"),
        ("OCCUPIED", "اشغال (دارای مهمان)"),
        ("RESERVED", "رزروشده")
    };

    public RoomStatusDialog(string? defaultRoom = null)
    {
        Title = "تغییر وضعیت اتاق";
        Width = 420;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var root = new StackPanel { Margin = new Thickness(20), Spacing = 12 };
        root.Children.Add(new TextBlock { Text = "شمارهٔ اتاق *", FontWeight = FontWeight.Bold });
        var numBox = new TextBox { Text = defaultRoom ?? "" };
        root.Children.Add(numBox);

        root.Children.Add(new TextBlock { Text = "وضعیت جدید", FontWeight = FontWeight.Bold });
        var combo = new ComboBox
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            ItemsSource = Statuses.Select(s => s.Label).ToArray(),
            SelectedIndex = 0
        };
        root.Children.Add(combo);

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید", Classes = { "primary" } };
        var cancel = new Button { Content = "انصراف", Classes = { "ghost" }, Foreground = new SolidColorBrush(Color.Parse("#16233D")), BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6")) };
        ok.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(numBox.Text)) return;
            RoomNumber = numBox.Text.Trim();
            NewStatus = Statuses[Math.Max(0, combo.SelectedIndex)].Code;
            Close(true);
        };
        cancel.Click += (_, _) => Close(false);
        buttons.Children.Add(ok);
        buttons.Children.Add(cancel);
        root.Children.Add(buttons);

        Content = new Border
        {
            Background = new SolidColorBrush(Color.Parse("#F7F3E9")),
            BorderBrush = new SolidColorBrush(Color.Parse("#C9A24B")),
            BorderThickness = new Thickness(1),
            Child = root
        };
    }
}
