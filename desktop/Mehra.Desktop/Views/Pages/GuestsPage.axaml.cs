using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class GuestsPage : UserControl
{
    private readonly GuestsVm _vm;

    public GuestsPage(Session session)
    {
        InitializeComponent();
        _vm = new GuestsVm(session);
        DataContext = _vm;
    }

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("مهمان جدید",
            ("نام", ""), ("نام خانوادگی", ""), ("تلفن", ""), ("شهر", ""));
        dlg.Required = new[] { false, true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddGuestAsync(dlg.Values["نام"], dlg.Values["نام خانوادگی"], dlg.Values["تلفن"], dlg.Values["شهر"]);
    }
}
