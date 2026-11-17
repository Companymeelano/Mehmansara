using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReservationsPage : UserControl
{
    private readonly ReservationsVm _vm;

    public ReservationsPage(Session session)
    {
        InitializeComponent();
        _vm = new ReservationsVm(session);
        DataContext = _vm;
    }

    private async void OnAdd(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var dlg = new InputDialog("رزرو جدید",
            ("نام مهمان", ""), ("تلفن", ""), ("شمارهٔ اتاق", ""),
            ("تاریخ ورود (yyyy-MM-dd)", today), ("تاریخ خروج (yyyy-MM-dd)", DateTime.Today.AddDays(1).ToString("yyyy-MM-dd")),
            ("تعداد نفرات", "1"), ("مبلغ کل (ریال)", "0"), ("پیش‌پرداخت (ریال)", "0"));
        dlg.Required = new[] { true, false, true, true, true, true, true, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddReservationAsync(
                dlg.Values["نام مهمان"], dlg.Values["تلفن"], dlg.Values["شمارهٔ اتاق"],
                dlg.Values["تاریخ ورود (yyyy-MM-dd)"], dlg.Values["تاریخ خروج (yyyy-MM-dd)"],
                dlg.Values["تعداد نفرات"], dlg.Values["مبلغ کل (ریال)"], dlg.Values["پیش‌پرداخت (ریال)"]);
    }

    private async void OnEdit(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: ReservationRow row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("ویرایش رزرو " + row.Code,
            ("نام مهمان", row.RawGuestName), ("تلفن", row.RawPhone), ("شمارهٔ اتاق", row.RawRoomNumber),
            ("تاریخ ورود (yyyy-MM-dd)", row.CheckInIso), ("تاریخ خروج (yyyy-MM-dd)", row.CheckOutIso),
            ("تعداد نفرات", row.GuestsCount.ToString()), ("مبلغ کل (ریال)", row.TotalRial.ToString()), ("پیش‌پرداخت (ریال)", row.PaidRial.ToString()));
        dlg.Required = new[] { true, false, true, true, true, true, true, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.UpdateReservationAsync(row.Id,
                dlg.Values["نام مهمان"], dlg.Values["تلفن"], dlg.Values["شمارهٔ اتاق"],
                dlg.Values["تاریخ ورود (yyyy-MM-dd)"], dlg.Values["تاریخ خروج (yyyy-MM-dd)"],
                dlg.Values["تعداد نفرات"], dlg.Values["مبلغ کل (ریال)"], dlg.Values["پیش‌پرداخت (ریال)"]);
    }

    private async void OnDelete(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: ReservationRow row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var c = new ConfirmDialog("حذف رزرو", $"آیا از حذف رزرو «{row.Code} — {row.RawGuestName}» مطمئن هستید؟");
        if (await c.ShowDialog<bool?>(win) == true)
            await _vm.DeleteReservationAsync(row.Id);
    }
}
