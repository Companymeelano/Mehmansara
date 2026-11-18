using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class RoomsPage : UserControl
{
    private readonly RoomsVm _vm;

    public RoomsPage(Session session)
    {
        InitializeComponent();
        _vm = new RoomsVm(session);
        DataContext = _vm;
    }

    private async void OnStatus(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("تغییر وضعیت اتاق",
            ("شمارهٔ اتاق", ""), ("وضعیت جدید (FREE/CLEANING/MAINTENANCE/OUT_OF_SERVICE)", "FREE"));
        dlg.Required = new[] { true, true };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.SetRoomStatusAsync(dlg.Values["شمارهٔ اتاق"], dlg.Values["وضعیت جدید (FREE/CLEANING/MAINTENANCE/OUT_OF_SERVICE)"]);
    }

    private async void OnAddRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("افزودن اتاق جدید",
            ("شمارهٔ اتاق", ""), ("طبقه", "1"), ("ظرفیت (نفر)", "2"),
            ("نوع اتاق", "دوتخته"), ("نرخ هر شب (ریال)", "2500000"),
            ("امکانات (کولر، یخچال، تلویزیون، سرویس…)", "کولر گازی، یخچال، تلویزیون، سرویس بهداشتی"));
        dlg.Required = new[] { true, true, true, true, true, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddRoomAsync(dlg.Values["شمارهٔ اتاق"], dlg.Values["طبقه"], dlg.Values["ظرفیت (نفر)"],
                dlg.Values["نوع اتاق"], dlg.Values["نرخ هر شب (ریال)"], dlg.Values["امکانات (کولر، یخچال، تلویزیون، سرویس…)"]);
    }

    private async void OnEditRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: RoomCard row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var rawNum = row.Number.Replace("اتاق ", "").Trim();
        var dlg = new InputDialog("ویرایش " + row.Number,
            ("شمارهٔ اتاق", rawNum), ("طبقه", row.Floor.ToString()), ("ظرفیت (نفر)", row.Capacity.ToString()),
            ("نوع اتاق", row.TypeName), ("نرخ هر شب (ریال)", row.RateRial.ToString()),
            ("امکانات", row.Amenities));
        dlg.Required = new[] { true, true, true, true, true, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.UpdateRoomAsync(row.Id, dlg.Values["شمارهٔ اتاق"], dlg.Values["طبقه"], dlg.Values["ظرفیت (نفر)"],
                dlg.Values["نوع اتاق"], dlg.Values["نرخ هر شب (ریال)"], dlg.Values["امکانات"]);
    }
}
