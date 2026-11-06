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

    private async void OnMarkAll(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.MarkAllReadAsync();
}
