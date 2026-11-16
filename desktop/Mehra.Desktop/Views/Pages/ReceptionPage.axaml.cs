using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReceptionPage : UserControl
{
    private readonly ReceptionVm _vm;

    public ReceptionPage(Session session)
    {
        InitializeComponent();
        _vm = new ReceptionVm(session);
        DataContext = _vm;
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
