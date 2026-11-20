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
            "فراسا — نسخهٔ ویندوز " + Fa.Digits(ver is null ? "1.2.4" : ver.ToString(3));
    }

    private async void OnHealth(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.CheckHealthAsync();

    private async void OnEnsureSchema(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.EnsureSchemaAsync();

    private async void OnBackup(object? sender, Avalonia.Interactivity.RoutedEventArgs e) =>
        await _vm.CreateBackupAsync();

    private async void OnRestore(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("بازیابی پشتیبان", "داده‌های فعلی با آخرین نسخهٔ پشتیبان جایگزین می‌شوند. ادامه می‌دهید؟", "بازیابی");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.RestoreBackupAsync();
    }

    private async void OnResetData(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("پاک‌سازی کامل داده‌ها",
            "تمام مهمانان، رزروها، اقامت‌ها، تراکنش‌های مالی، نظافت، تعمیرات و اعلان‌ها پاک می‌شوند و اتاق‌ها آزاد می‌شوند. ادامه می‌دهید؟",
            "پاک‌سازی کامل");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.ResetDataAsync();
    }
}
