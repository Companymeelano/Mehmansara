using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class RoomsPage : UserControl
{
    private readonly RoomsVm _vm;

    public RoomsPage(Session session)
    {
        InitializeComponent();
        _vm = new RoomsVm(session);
        DataContext = _vm;
    }

    private async void OnStatus(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("تغییر وضعیت اتاق",
            ("شمارهٔ اتاق", ""), ("وضعیت جدید (FREE/CLEANING/MAINTENANCE/OUT_OF_SERVICE)", "FREE"));
        dlg.Required = new[] { true, true };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.SetRoomStatusAsync(dlg.Values["شمارهٔ اتاق"], dlg.Values["وضعیت جدید (FREE/CLEANING/MAINTENANCE/OUT_OF_SERVICE)"]);
    }
}
