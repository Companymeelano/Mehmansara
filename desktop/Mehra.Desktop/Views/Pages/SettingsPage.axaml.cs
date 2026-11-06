using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class SettingsPage : UserControl
{
    private readonly SettingsVm _vm;

    public SettingsPage(Session session)
    {
        InitializeComponent();
        _vm = new SettingsVm(session);
        DataContext = _vm;
    }

    private async void OnHealth(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.CheckHealthAsync();
}
