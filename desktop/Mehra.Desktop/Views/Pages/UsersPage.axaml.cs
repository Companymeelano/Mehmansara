using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class UsersPage : UserControl
{
    public UsersPage(Session session)
    {
        InitializeComponent();
        DataContext = new UsersVm(session);
    }
}
