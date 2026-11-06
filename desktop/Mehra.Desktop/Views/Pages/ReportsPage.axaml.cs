using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReportsPage : UserControl
{
    private readonly ReportsVm _vm;

    public ReportsPage(Session session)
    {
        InitializeComponent();
        _vm = new ReportsVm(session);
        DataContext = _vm;
        var combo = this.FindControl<ComboBox>("ReportCombo")!;
        foreach (var opt in ReportsVm.Options)
            combo.Items.Add(new ComboBoxItem { Content = opt.Title, Tag = opt.Key });
        combo.SelectedIndex = 0;
    }

    private async void OnReportChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (sender is not ComboBox combo) return;
        if (combo.SelectedItem is ComboBoxItem item && item.Tag is string key)
            await _vm.LoadReportAsync(key);
    }
}
