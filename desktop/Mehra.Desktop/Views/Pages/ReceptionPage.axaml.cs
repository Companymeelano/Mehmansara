using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReceptionPage : UserControl
{
    public ReceptionPage(Session session)
    {
        InitializeComponent();
        DataContext = new ReceptionVm(session);
    }
}
