using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class UsersPage : UserControl
{
    public UsersPage(Session session)
    {
        InitializeComponent();
        _vm = new UsersVm(session);
        DataContext = _vm;
    }
    private readonly UsersVm _vm;

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("افزودن کاربر جدید",
            ("نام و نام خانوادگی", ""), ("نام کاربری (انگلیسی)", ""), ("رمز عبور", ""), ("سمت (مثلاً پذیرش)", "پذیرش"), ("تلفن", ""));
        dlg.Required = new[] { true, true, true, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddUserAsync(dlg.Values["نام و نام خانوادگی"], dlg.Values["نام کاربری (انگلیسی)"], dlg.Values["رمز عبور"], dlg.Values["سمت (مثلاً پذیرش)"], dlg.Values["تلفن"]);
    }
}
