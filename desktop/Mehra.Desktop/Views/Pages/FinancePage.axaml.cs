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
        var defaultAcct = _vm.BankAccounts.Count > 0
            ? $"{_vm.BankAccounts[0].BankName} ({_vm.BankAccounts[0].AccountNumber})"
            : "صندوق نقدی مدیر";
        var dlg = new InputDialog("ثبت تراکنش مالی",
            ("نوع (Receive/Pay/Discount)", "Receive"),
            ("مبلغ (ریال)", "0"),
            ("روش پرداخت (نقدی / دستگاه پوز / کارت به کارت / انتقال)", "نقدی"),
            ("حساب مدیر", defaultAcct),
            ("شماره پیگیری / جزئیات", ""),
            ("نام مهمان (اختیاری)", ""));
        dlg.Required = new[] { true, true, true, false, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            var mRaw = dlg.Values["روش پرداخت (نقدی / دستگاه پوز / کارت به کارت / انتقال)"].Trim();
            var method = mRaw.Contains("پوز") ? "Pos" : (mRaw.Contains("کارت") || mRaw.Contains("انتقال") ? "Transfer" : "Cash");
            var desc = $"{mRaw} — حساب: {dlg.Values["حساب مدیر"]} — {dlg.Values["شماره پیگیری / جزئیات"]}".Trim(' ', '—');
            await _vm.AddTransactionAsync(
                dlg.Values["نوع (Receive/Pay/Discount)"], dlg.Values["مبلغ (ریال)"], method,
                desc, dlg.Values["نام مهمان (اختیاری)"]);
        }
    }

    private async void OnAddBank(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("معرفی حساب بانکی جدید",
            ("شماره حساب / کارت", ""), ("نوع حساب (جاری / قرض‌الحسنه / سپرده)", "جاری"),
            ("نام بانک", "بانک ملی"), ("نام صاحب حساب", "مدیر"));
        dlg.Required = new[] { true, true, true, true };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddBankAccountAsync(dlg.Values["شماره حساب / کارت"], dlg.Values["نوع حساب (جاری / قرض‌الحسنه / سپرده)"],
                dlg.Values["نام بانک"], dlg.Values["نام صاحب حساب"]);
    }

    private async void OnDeleteBank(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: BankAccountDto b } || b.Id is null) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("حذف حساب بانکی", $"آیا از حذف حساب «{b.BankName} — {b.AccountNumber}» مطمئن هستید؟");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.DeleteBankAccountAsync(b.Id);
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
