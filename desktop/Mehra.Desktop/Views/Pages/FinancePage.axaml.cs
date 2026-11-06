using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class FinancePage : UserControl
{
    public FinancePage(Session session)
    {
        InitializeComponent();
        DataContext = new FinanceVm(session);
    }
}
