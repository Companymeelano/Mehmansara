using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class HomePage : UserControl
{
    public HomePage(Session session)
    {
        InitializeComponent();
        DataContext = new HomeVm(session);
    }
}
