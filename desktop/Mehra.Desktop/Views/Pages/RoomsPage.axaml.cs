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
        var dlg = new RoomStatusDialog();
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.SetRoomStatusAsync(dlg.RoomNumber, dlg.NewStatus);
    }

    private async void OnAddRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new RoomDialog("افزودن اتاق جدید");
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.AddRoomAsync(dlg.RoomNumber, dlg.Floor.ToString(), dlg.Capacity.ToString(),
                dlg.RoomTypeName, dlg.RatePerNightRial.ToString(), dlg.Amenities);
    }

    private async void OnEditRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: RoomCard row }) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var rawNum = row.Number.Replace("اتاق ", "").Trim();
        var dlg = new RoomDialog("ویرایش " + row.Number, rawNum, row.Floor, row.Capacity, row.TypeName, row.RateRial, row.Amenities);
        if (await dlg.ShowDialog<bool?>(win) == true)
            await _vm.UpdateRoomAsync(row.Id, dlg.RoomNumber, dlg.Floor.ToString(), dlg.Capacity.ToString(),
                dlg.RoomTypeName, dlg.RatePerNightRial.ToString(), dlg.Amenities);
    }
}
