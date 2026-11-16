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
        var ver = System.Reflection.Assembly.GetExecutingAssembly().GetName().Version;
        this.FindControl<TextBlock>("VersionText")!.Text =
            "مهرآ — نسخهٔ ویندوز " + Fa.Digits(ver is null ? "1.2.1" : ver.ToString(3));
    }

    private async void OnHealth(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.CheckHealthAsync();

    private async void OnEnsureSchema(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.EnsureSchemaAsync();
}
