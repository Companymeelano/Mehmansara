using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

/// <summary>
/// Full 2-column Guest Registration/Edit dialog (v1.2.4) with ComboBox for فرهنگی/آزاد
/// and تعداد افراد خانواده فرهنگی.
/// </summary>
public class GuestDialog : Window
{
    public string FirstName { get; private set; } = "";
    public string LastName { get; private set; } = "";
    public string Phone { get; private set; } = "";
    public string City { get; private set; } = "";
    public string GuestCategory { get; private set; } = "فرهنگی";
    public int CulturalFamilyCount { get; private set; } = 1;

    public GuestDialog(string title = "معرفی و ثبت مهمان جدید")
    {
        Title = title;
        Width = 740;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var root = new StackPanel { Margin = new Thickness(20), Spacing = 12 };
        root.Children.Add(new TextBlock
        {
            Text = title, FontSize = 16, Classes = { "bold" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D"))
        });

        var grid = new UniformGrid { Columns = 2 };
        TextBox AddField(string label, string def = "")
        {
            var cell = new StackPanel { Margin = new Thickness(6, 4), Spacing = 4 };
            cell.Children.Add(new TextBlock { Text = label, FontWeight = FontWeight.Bold, FontSize = 12 });
            var tb = new TextBox { Text = def, HorizontalAlignment = HorizontalAlignment.Stretch };
            cell.Children.Add(tb);
            grid.Children.Add(cell);
            return tb;
        }

        var fnBox = AddField("نام");
        var lnBox = AddField("نام خانوادگی *");
        var fatherBox = AddField("نام پدر");
        var idNumBox = AddField("شماره شناسنامه");
        var natCodeBox = AddField("کد ملی");
        var birthBox = AddField("تاریخ تولد");
        var phoneBox = AddField("تلفن همراه");
        var cityBox = AddField("محل سکونت فعلی");
        var carTypeBox = AddField("نوع خودرو");
        var plateBox = AddField("شماره پلاک");
        var guestsBox = AddField("تعداد مهمان", "1");

        // ComboBox for فرهنگی / آزاد
        var catCell = new StackPanel { Margin = new Thickness(6, 4), Spacing = 4 };
        catCell.Children.Add(new TextBlock { Text = "نوع مهمان (فرهنگی / آزاد)", FontWeight = FontWeight.Bold, FontSize = 12 });
        var catCombo = new ComboBox
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            ItemsSource = new[] { "فرهنگی", "آزاد" },
            SelectedIndex = 0
        };
        catCell.Children.Add(catCombo);
        grid.Children.Add(catCell);

        var famBox = AddField("تعداد افراد خانواده فرهنگی", "1");
        var relBox = AddField("نسبت با فرهنگی (در صورت انتخاب)");
        var noteBox = AddField("توضیحات");

        root.Children.Add(new ScrollViewer { MaxHeight = 500, Content = grid });

        var err = new TextBlock { Foreground = Brushes.Red, IsVisible = false };
        root.Children.Add(err);

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید و ادامه", Classes = { "primary" } };
        var cancel = new Button
        {
            Content = "انصراف", Classes = { "ghost" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D")),
            BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6"))
        };
        ok.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(lnBox.Text))
            {
                err.Text = "نام خانوادگی الزامی است.";
                err.IsVisible = true;
                return;
            }
            FirstName = fnBox.Text?.Trim() ?? "";
            LastName = lnBox.Text.Trim();
            Phone = phoneBox.Text?.Trim() ?? "";
            City = cityBox.Text?.Trim() ?? "";
            GuestCategory = catCombo.SelectedIndex == 1 ? "آزاد" : "فرهنگی";
            CulturalFamilyCount = int.TryParse(famBox.Text, out var fc) && fc > 0 ? fc : 1;
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

/// <summary>
/// Bed selection dialog for عمومی rooms (shows 1..N beds, occupied beds disabled, empty beds selectable).
/// </summary>
public class BedSelectionDialog : Window
{
    public List<int> SelectedBeds { get; } = new();

    public BedSelectionDialog(string roomNumber, int capacity, HashSet<int> occupiedBeds)
    {
        Title = $"انتخاب تخت — {roomNumber}";
        Width = 460;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var root = new StackPanel { Margin = new Thickness(20), Spacing = 12 };
        root.Children.Add(new TextBlock
        {
            Text = $"انتخاب تخت در {roomNumber} ({capacity - occupiedBeds.Count} تخت خالی)",
            FontSize = 15, Classes = { "bold" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D"))
        });

        var wrap = new WrapPanel { Orientation = Orientation.Horizontal };
        var checks = new List<(int Bed, CheckBox Box)>();
        for (int i = 1; i <= capacity; i++)
        {
            var isOcc = occupiedBeds.Contains(i);
            var cb = new CheckBox
            {
                Content = isOcc ? $"🛏 تخت {Fa.Num(i)} (پر)" : $"🛏 تخت {Fa.Num(i)} (خالی)",
                IsEnabled = !isOcc,
                Margin = new Thickness(6, 4)
            };
            checks.Add((i, cb));
            wrap.Children.Add(cb);
        }
        root.Children.Add(wrap);

        var err = new TextBlock { Foreground = Brushes.Red, IsVisible = false };
        root.Children.Add(err);

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید تخت‌ها", Classes = { "primary" } };
        var cancel = new Button { Content = "انصراف", Classes = { "ghost" }, Foreground = new SolidColorBrush(Color.Parse("#16233D")), BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6")) };
        ok.Click += (_, _) =>
        {
            SelectedBeds.Clear();
            SelectedBeds.AddRange(checks.Where(x => x.Box.IsChecked == true).Select(x => x.Bed));
            if (SelectedBeds.Count == 0)
            {
                err.Text = "حداقل یک تخت خالی را انتخاب کنید.";
                err.IsVisible = true;
                return;
            }
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
