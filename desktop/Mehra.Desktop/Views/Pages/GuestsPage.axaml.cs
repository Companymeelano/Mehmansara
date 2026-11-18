using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class GuestsPage : UserControl
{
    private readonly GuestsVm _vm;
    private readonly Session _session;

    public GuestsPage(Session session)
    {
        InitializeComponent();
        _session = session;
        _vm = new GuestsVm(session);
        DataContext = _vm;
    }

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("معرفی و ثبت مهمان جدید",
            ("نام", ""), ("نام خانوادگی", ""), ("نام پدر", ""),
            ("شماره شناسنامه", ""), ("کد ملی", ""), ("تاریخ تولد", ""),
            ("تلفن همراه", ""), ("محل سکونت فعلی", ""),
            ("نوع خودرو", ""), ("شماره پلاک", ""), ("تعداد مهمان", "1"),
            ("فرهنگی یا آزاد", "فرهنگی"), ("نسبت با فرهنگی (در صورت انتخاب)", ""), ("توضیحات", ""));
        dlg.Required = new[] { false, true, false, false, false, false, false, false, false, false, false, false, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            await _vm.AddGuestAsync(dlg.Values["نام"], dlg.Values["نام خانوادگی"], dlg.Values["تلفن همراه"], dlg.Values["محل سکونت فعلی"]);
            var roomDlg = new InputDialog("انتخاب اتاق پس از ثبت اولیه (اختیاری)",
                ("شمارهٔ اتاق (خالی = بدون اتاق)", ""),
                ("تاریخ خروج (yyyy-MM-dd)", DateTime.Today.AddDays(1).ToString("yyyy-MM-dd")),
                ("مبلغ کل (ریال)", "2500000"));
            roomDlg.Required = new[] { false, false, false };
            if (await roomDlg.ShowDialog<bool?>(win) == true && !string.IsNullOrWhiteSpace(roomDlg.Values["شمارهٔ اتاق (خالی = بدون اتاق)"]))
            {
                var fullName = (dlg.Values["نام"] + " " + dlg.Values["نام خانوادگی"]).Trim();
                await _session.Api.AddReservationAsync(fullName, dlg.Values["تلفن همراه"],
                    roomDlg.Values["شمارهٔ اتاق (خالی = بدون اتاق)"],
                    DateTime.Today.ToString("yyyy-MM-dd"), roomDlg.Values["تاریخ خروج (yyyy-MM-dd)"],
                    int.TryParse(dlg.Values["تعداد مهمان"], out var gc) ? gc : 1,
                    long.TryParse(roomDlg.Values["مبلغ کل (ریال)"], out var tr) ? tr : 0, 0);
            }
        }
    }

    private async void OnEdit(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: GuestRow row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("ویرایش مهمان",
            ("نام", row.FirstName), ("نام خانوادگی", row.LastName), ("تلفن", row.RawPhone), ("شهر", row.RawCity));
        dlg.Required = new[] { false, true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.UpdateGuestAsync(row.Id, dlg.Values["نام"], dlg.Values["نام خانوادگی"], dlg.Values["تلفن"], dlg.Values["شهر"]);
    }

    private async void OnDelete(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: GuestRow row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("حذف مهمان", $"آیا از حذف مهمان «{row.Name}» مطمئن هستید؟");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.DeleteGuestAsync(row.Id);
    }
}
