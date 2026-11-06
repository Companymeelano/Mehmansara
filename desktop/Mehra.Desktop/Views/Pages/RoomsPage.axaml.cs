using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class RoomsPage : UserControl
{
    public RoomsPage(Session session)
    {
        InitializeComponent();
        DataContext = new RoomsVm(session);
    }
}
