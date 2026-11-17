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
