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
        var dlg = new UserDialog();
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            UserPermissionsStore.SaveUserPermissions(dlg.Username, dlg.AllowedPages);
            await _vm.AddUserAsync(dlg.FullName, dlg.Username, dlg.Password, dlg.RoleTitle, dlg.Phone);
        }
    }
}
