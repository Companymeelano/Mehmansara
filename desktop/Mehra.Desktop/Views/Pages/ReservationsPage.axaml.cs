using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReservationsPage : UserControl
{
    public ReservationsPage(Session session)
    {
        InitializeComponent();
        DataContext = new ReservationsVm(session);
    }
}
