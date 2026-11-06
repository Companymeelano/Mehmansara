using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class MaintenancePage : UserControl
{
    public MaintenancePage(Session session)
    {
        InitializeComponent();
        DataContext = new MaintenanceVm(session);
    }
}
