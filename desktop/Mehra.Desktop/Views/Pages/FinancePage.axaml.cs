using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class FinancePage : UserControl
{
    private readonly FinanceVm _vm;

    public FinancePage(Session session)
    {
        InitializeComponent();
        _vm = new FinanceVm(session);
        DataContext = _vm;
    }

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("تراکنش جدید",
            ("نوع (Receive/Pay/Discount)", "Receive"), ("مبلغ (ریال)", "0"), ("روش (Cash/Pos/Transfer)", "Cash"),
            ("توضیحات", ""), ("نام مهمان (اختیاری)", ""));
        dlg.Required = new[] { true, true, false, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddTransactionAsync(
                dlg.Values["نوع (Receive/Pay/Discount)"], dlg.Values["مبلغ (ریال)"], dlg.Values["روش (Cash/Pos/Transfer)"],
                dlg.Values["توضیحات"], dlg.Values["نام مهمان (اختیاری)"]);
    }

    private async void OnReverse(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: TxnRow row }) return;
        if (row.IsReversed) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("برگشت تراکنش", $"آیا از برگشت دادن تراکنش «{row.Code}» به مبلغ {row.AmountFa} مطمئن هستید؟", "برگشت تراکنش");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.ReverseTransactionAsync(row.Id);
    }
}
