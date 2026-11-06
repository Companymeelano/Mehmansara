using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class GuestsPage : UserControl
{
    public GuestsPage(Session session)
    {
        InitializeComponent();
        DataContext = new GuestsVm(session);
    }
}
