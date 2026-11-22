using Avalonia.Controls;
using Mehra.Desktop.Views.Pages;

namespace Mehra.Desktop.Views;

public partial class MainWindow : Window
{
    private readonly Session _session;

    public MainWindow(Session session)
    {
        _session = session;
        InitializeComponent();
        this.FindControl<TextBlock>("UserText")!.Text = session.User.FullName;
        this.FindControl<TextBlock>("RoleText")!.Text =
            (session.User.RoleTitle.Length > 0 ? session.User.RoleTitle + " — " : "") + session.Api.ServerLabel;
        this.FindControl<TextBlock>("StatusText")!.Text = "";
        if (session.Api is LocalFileApi localApi)
        {
            var lan = LanServer.Start(localApi);
            if (lan != null)
                this.FindControl<TextBlock>("StatusText")!.Text =
                    "● سرور شبکهٔ محلی فعال (اندروید): " + lan.LanUrl + "   —   داده‌ها: " + localApi.DataFile;
        }
        var nav = this.FindControl<ListBox>("NavList")!;
        var allowed = UserPermissionsStore.GetAllowedPages(session.User.Username, session.User.RoleTitle);
        ListBoxItem? firstVisible = null;
        foreach (var obj in nav.Items)
        {
            if (obj is ListBoxItem li && li.Tag is string tag)
            {
                li.IsVisible = allowed.Contains(tag);
                if (li.IsVisible && firstVisible == null) firstVisible = li;
            }
        }
        if (firstVisible != null) nav.SelectedItem = firstVisible;
        Closing += async (_, _) =>
        {
            try { await _session.Api.CreateBackupAsync(); } catch { }
        };
    }

    private void OnNavChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (sender is not ListBox list) return;
        if (list.SelectedItem is not ListBoxItem item) return;
        var tag = item.Tag as string;
        var area = this.FindControl<ContentControl>("ContentArea")!;
        var status = this.FindControl<TextBlock>("StatusText")!;
        switch (tag)
        {
            case "home": area.Content = new HomePage(_session); status.Text = Fa.DateLong(DateTime.Today); break;
            case "rooms": area.Content = new RoomsPage(_session); status.Text = "فهرست اتاق‌ها"; break;
            case "reservations": area.Content = new ReservationsPage(_session); status.Text = "فهرست رزروها"; break;
            case "reception": area.Content = new ReceptionPage(_session); status.Text = "پذیرش — اقامت‌های فعال"; break;
            case "guests": area.Content = new GuestsPage(_session); status.Text = "فهرست مهمانان"; break;
            case "finance": area.Content = new FinancePage(_session); status.Text = "مالی — صندوق و تراکنش‌ها"; break;
            case "housekeeping": area.Content = new HousekeepingPage(_session); status.Text = "نظافت و خدمات"; break;
            case "maintenance": area.Content = new MaintenancePage(_session); status.Text = "تعمیرات"; break;
            case "reports": area.Content = new ReportsPage(_session); status.Text = "گزارش‌ها"; break;
            case "users": area.Content = new UsersPage(_session); status.Text = "کاربران"; break;
            case "notifications": area.Content = new NotificationsPage(_session); status.Text = "اعلان‌ها"; break;
            case "settings": area.Content = new SettingsPage(_session); status.Text = "تنظیمات"; break;
        }
    }

    private async void OnOrgContracts(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var list = OrgContractsService.Load();
        var dlg = new InputDialog("ثبت قرارداد / معرفی‌نامه سازمانی",
            ("نام اداره یا سازمان", ""), ("درصد تخفیف مصوب", "۲۰"), ("نام رابط / شماره نامه", ""));
        if (await dlg.ShowDialog<bool?>(this) == true)
        {
            list.Insert(0, new OrgContractRecord
            {
                OrgName = dlg.Values["نام اداره یا سازمان"],
                DiscountPercent = dlg.Values["درصد تخفیف مصوب"],
                ContactPerson = dlg.Values["نام رابط / شماره نامه"]
            });
            OrgContractsService.Save(list);
            this.FindControl<TextBlock>("StatusText")!.Text = $"✓ قرارداد سازمان «{dlg.Values["نام اداره یا سازمان"]}» ثبت شد (کل قراردادها: {Fa.Num(list.Count)}).";
        }
    }

    private async void OnExtendStay(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var dlg = new InputDialog("تمدید سریع اقامت مهمان",
            ("شماره اتاق یا کد قبض", ""), ("تعداد شب اضافه", "۱"), ("مبلغ مابه‌التفاوت (ریال)", "0"));
        if (await dlg.ShowDialog<bool?>(this) == true)
        {
            long.TryParse(dlg.Values["مبلغ مابه‌التفاوت (ریال)"].Replace(",", ""), out var extra);
            if (extra > 0)
                await _session.Api.AddTransactionAsync("Receive", extra, "Pos", $"تمدید اقامت ({dlg.Values["تعداد شب اضافه"]} شب)", "مهمان", dlg.Values["شماره اتاق یا کد قبض"]);
            this.FindControl<TextBlock>("StatusText")!.Text = $"✓ اقامت اتاق {Fa.Digits(dlg.Values["شماره اتاق یا کد قبض"])} به مدت {Fa.Digits(dlg.Values["تعداد شب اضافه"])} شب تمدید شد.";
        }
    }

    private async void OnGuardDesk(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var rooms = (await _session.Api.RoomsAsync()).Value ?? new();
        var occ = rooms.Where(r => string.Equals(r.Status, "Occupied", StringComparison.OrdinalIgnoreCase)).ToList();
        var cars = ParkingService.Load();
        var summary = $"اتاق‌های پر: {Fa.Num(occ.Count)} ({string.Join("، ", occ.Select(r => Fa.Digits(r.RoomNumber)))})\n" +
                      $"خودروهای پارکینگ: {Fa.Num(cars.Count)} ({string.Join("، ", cars.Take(5).Select(c => c.Plate))})";
        this.FindControl<TextBlock>("StatusText")!.Text = summary.Replace("\n", "   |   ");
    }

    private async void OnParking(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var list = ParkingService.Load();
        var dlg = new InputDialog("ثبت خودرو در پارکینگ",
            ("شماره پلاک", ""), ("نام مهمان", ""), ("شماره اتاق", ""));
        if (await dlg.ShowDialog<bool?>(this) == true)
        {
            list.Insert(0, new ParkingRecord
            {
                Plate = dlg.Values["شماره پلاک"],
                GuestName = dlg.Values["نام مهمان"],
                RoomNumber = dlg.Values["شماره اتاق"]
            });
            ParkingService.Save(list);
            this.FindControl<TextBlock>("StatusText")!.Text = $"✓ خودرو با پلاک {dlg.Values["شماره پلاک"]} در پارکینگ ثبت شد (تعداد کل: {Fa.Num(list.Count)}).";
        }
    }

    private async void OnShiftHandover(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var dlg = new InputDialog("گزارش تحویل شیفت پذیرش",
            ("موجودی صندوق نقدی (ریال)", "0"), ("جمع دستگاه پوز (ریال)", "0"), ("توضیحات شیفت", ""));
        if (await dlg.ShowDialog<bool?>(this) == true)
        {
            long.TryParse(dlg.Values["موجودی صندوق نقدی (ریال)"].Replace(",", ""), out var cash);
            long.TryParse(dlg.Values["جمع دستگاه پوز (ریال)"].Replace(",", ""), out var pos);
            ShiftHandoverPrinter.PrintHandover(_session.User.FullName, cash, pos, dlg.Values["توضیحات شیفت"]);
        }
    }

    private async void OnLockScreen(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        var lockWin = new LockScreenWindow(_session);
        await lockWin.ShowDialog(this);
    }

    private void OnLogout(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        new LoginWindow().Show();
        Close();
    }
}
