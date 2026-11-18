using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class HousekeepingPage : UserControl
{
    public HousekeepingPage(Session session)
    {
        InitializeComponent();
        _vm = new HousekeepingVm(session);
        DataContext = _vm;
    }
    private readonly HousekeepingVm _vm;

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("ثبت کار نظافت",
            ("شمارهٔ اتاق", ""), ("نوع (Daily/Checkout/Deep)", "Daily"), ("مسئول نظافت", ""), ("توضیحات", ""));
        dlg.Required = new[] { true, true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddTaskAsync(dlg.Values["شمارهٔ اتاق"], dlg.Values["نوع (Daily/Checkout/Deep)"], dlg.Values["مسئول نظافت"], dlg.Values["توضیحات"]);
    }
}
