using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class NotificationsPage : UserControl
{
    private readonly NotificationsVm _vm;

    public NotificationsPage(Session session)
    {
        InitializeComponent();
        _vm = new NotificationsVm(session);
        DataContext = _vm;
    }

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("اعلان جدید",
            ("عنوان اعلان", ""), ("متن اعلان", ""), ("اهمیت (HIGH/MEDIUM/LOW)", "MEDIUM"));
        dlg.Required = new[] { true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddNotificationAsync(dlg.Values["عنوان اعلان"], dlg.Values["متن اعلان"], dlg.Values["اهمیت (HIGH/MEDIUM/LOW)"]);
    }

    private async void OnMarkAll(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.MarkAllReadAsync();
}
