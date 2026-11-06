using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class HousekeepingPage : UserControl
{
    public HousekeepingPage(Session session)
    {
        InitializeComponent();
        DataContext = new HousekeepingVm(session);
    }
}
