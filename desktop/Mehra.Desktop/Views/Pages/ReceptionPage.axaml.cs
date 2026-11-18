using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReceptionPage : UserControl
{
    private readonly ReceptionVm _vm;
    private readonly Session _session;

    public ReceptionPage(Session session)
    {
        InitializeComponent();
        _session = session;
        _vm = new ReceptionVm(session);
        DataContext = _vm;
    }

    private async void OnWalkIn(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var tomorrow = DateTime.Today.AddDays(1).ToString("yyyy-MM-dd");
        var dlg = new InputDialog("معرفی و پذیرش مستقیم مهمان",
            ("نام و نام خانوادگی مهمان", ""), ("تلفن همراه", ""), ("شمارهٔ اتاق", ""),
            ("تاریخ خروج (yyyy-MM-dd)", tomorrow), ("تعداد نفرات", "1"), ("مبلغ کل (ریال)", "2500000"), ("پیش‌پرداخت (ریال)", "0"));
        dlg.Required = new[] { true, false, true, true, true, true, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            var r = await _session.Api.AddReservationAsync(
                dlg.Values["نام و نام خانوادگی مهمان"], dlg.Values["تلفن همراه"], dlg.Values["شمارهٔ اتاق"],
                today, dlg.Values["تاریخ خروج (yyyy-MM-dd)"],
                int.TryParse(dlg.Values["تعداد نفرات"], out var gc) ? gc : 1,
                long.TryParse(dlg.Values["مبلغ کل (ریال)"], out var tr) ? tr : 0,
                long.TryParse(dlg.Values["پیش‌پرداخت (ریال)"], out var pr) ? pr : 0);
            if (r.Ok && r.Value?.Code != null)
                await _vm.CheckInAsync(r.Value.Code);
            else
                _vm.Status = r.Error;
        }
    }

    private async void OnCheckIn(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("پذیرش رزرو (check-in)", ("کد رزرو (مثلاً R-1001)", ""));
        dlg.Required = new[] { true };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.CheckInAsync(dlg.Values["کد رزرو (مثلاً R-1001)"]);
    }

    private async void OnCheckOut(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("تسویهٔ اقامت (check-out)",
            ("کد اقامت/رزرو", ""), ("مبلغ نهایی (ریال)", "0"), ("روش پرداخت", "Cash"));
        dlg.Required = new[] { true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.CheckOutAsync(dlg.Values["کد اقامت/رزرو"], dlg.Values["مبلغ نهایی (ریال)"], dlg.Values["روش پرداخت"]);
    }
}
