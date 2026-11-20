using System.Globalization;
using Avalonia.Controls;

namespace Mehra.Desktop.Views.Pages;

public partial class ReceptionPage : UserControl
{
    private readonly Session _session;
    private List<GuestRow> _allGuests = new();
    private List<RoomCard> _availableRooms = new();
    private GuestRow? _selectedGuest;
    private RoomCard? _selectedRoom;
    private string _selectedBedsNote = "";
    private int _nights = 1;
    private int _guestsCount = 1;
    private long _baseTotalRial;
    private long _finalTotalRial;
    private bool _formatting;

    public ReceptionPage(Session session)
    {
        InitializeComponent();
        _session = session;
        _ = LoadGuestsAsync();
    }

    private async Task LoadGuestsAsync()
    {
        var r = await _session.Api.GuestsAsync();
        if (!r.Ok || r.Value is null) { WizardStatus.Text = r.Error; return; }
        _allGuests = r.Value.Select(g => new GuestRow
        {
            Id = g.Id ?? "",
            FirstName = g.FirstName ?? "",
            LastName = g.LastName ?? "",
            RawPhone = g.Phone ?? "",
            RawCity = g.City ?? "",
            NationalCode = g.NationalCode ?? "",
            VehiclePlate = g.VehiclePlate ?? "",
            Name = ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim(),
            PhoneFa = Fa.Digits(g.Phone ?? ""),
            City = g.City ?? ""
        }).ToList();
    }

    private void OnGuestSearch(object? sender, TextChangedEventArgs e)
    {
        var q = (GuestSearchBox.Text ?? "").Trim();
        if (string.IsNullOrEmpty(q))
        {
            GuestList.IsVisible = false;
            SearchHint.IsVisible = true;
            GuestList.ItemsSource = null;
            return;
        }
        SearchHint.IsVisible = false;
        GuestList.IsVisible = true;
        GuestList.ItemsSource = _allGuests.Where(g =>
            g.Name.Contains(q, StringComparison.OrdinalIgnoreCase) ||
            g.RawPhone.Contains(q, StringComparison.OrdinalIgnoreCase) ||
            g.RawCity.Contains(q, StringComparison.OrdinalIgnoreCase) ||
            g.NationalCode.Contains(q, StringComparison.OrdinalIgnoreCase) ||
            g.VehiclePlate.Contains(q, StringComparison.OrdinalIgnoreCase)).ToList();
    }

    private async void OnAddNewGuest(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new GuestDialog();
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            var r = await _session.Api.AddGuestAsync(
                dlg.FirstName, dlg.LastName,
                string.IsNullOrWhiteSpace(dlg.Phone) ? null : dlg.Phone,
                string.IsNullOrWhiteSpace(dlg.City) ? null : dlg.City);
            if (!r.Ok || r.Value is null) { WizardStatus.Text = r.Error; return; }
            await LoadGuestsAsync();
            var created = _allGuests.FirstOrDefault(x => x.Id == r.Value.Id) ?? _allGuests.LastOrDefault();
            if (created != null) await GoToStep2Async(created);
        }
    }

    private async void OnSelectGuest(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is Button { Tag: GuestRow g })
            await GoToStep2Async(g);
    }

    private async Task GoToStep2Async(GuestRow guest)
    {
        _selectedGuest = guest;
        SelectedGuestBanner.Text = $"مهمان انتخاب‌شده: {guest.Name} ({guest.PhoneFa})";
        var rr = await _session.Api.RoomsAsync();
        if (!rr.Ok || rr.Value is null) { WizardStatus.Text = rr.Error; return; }

        _availableRooms = rr.Value
            .Where(rm => rm.Status == "FREE" || ((rm.RoomTypeName ?? "").Contains("عمومی") && rm.Status != "MAINTENANCE" && rm.Status != "OUT_OF_SERVICE"))
            .Select(rm => new RoomCard
            {
                Id = rm.Id ?? rm.RoomNumber ?? "",
                Floor = rm.Floor,
                Capacity = rm.Capacity,
                RateRial = rm.RatePerNightRial ?? 0,
                Amenities = rm.Amenities ?? "",
                Number = "اتاق " + Fa.Digits(rm.RoomNumber ?? "—"),
                TypeName = rm.RoomTypeName ?? "",
                Details = (rm.Floor == 0 ? "همکف" : "طبقهٔ " + Fa.Num(rm.Floor)) + " — " + Fa.Num(rm.Capacity) + " تخت",
                RateFa = Fa.Money(rm.RatePerNightRial) + " / شب",
                StatusFa = StatusText.RoomFa(rm.Status),
                StatusBrush = StatusText.RoomBrush(rm.Status)
            }).ToList();

        ApplyRoomFilters();
        Step1Panel.IsVisible = false;
        Step2Panel.IsVisible = true;
        Step3Panel.IsVisible = false;
        BackBtn.IsVisible = true;
    }

    private void OnRoomSearchChanged(object? sender, TextChangedEventArgs e) => ApplyRoomFilters();
    private void OnRoomComboChanged(object? sender, SelectionChangedEventArgs e) => ApplyRoomFilters();

    private void ApplyRoomFilters()
    {
        if (FreeRoomList is null) return;
        var q = (RoomSearchBox.Text ?? "").Trim();
        var typeIdx = RoomTypeFilter.SelectedIndex;
        var floorIdx = RoomFloorFilter.SelectedIndex;

        var filtered = _availableRooms.Where(rm =>
        {
            if (!string.IsNullOrEmpty(q) &&
                !rm.Number.Contains(q, StringComparison.OrdinalIgnoreCase) &&
                !rm.Amenities.Contains(q, StringComparison.OrdinalIgnoreCase) &&
                !rm.TypeName.Contains(q, StringComparison.OrdinalIgnoreCase))
                return false;
            if (typeIdx == 1 && !rm.TypeName.Contains("سوئیت")) return false;
            if (typeIdx == 2 && !rm.TypeName.Contains("عمومی")) return false;
            if (floorIdx > 0 && rm.Floor != (floorIdx - 1)) return false;
            return true;
        }).ToList();

        FreeRoomList.ItemsSource = filtered;
    }

    private async void OnSelectRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: RoomCard room } || _selectedGuest is null) return;
        if (TopLevel.GetTopLevel(this) is not Window win) return;

        _selectedBedsNote = "";
        if (room.TypeName.Contains("عمومی"))
        {
            var bedDlg = new BedSelectionDialog(room.Number, Math.Max(1, room.Capacity), new HashSet<int>());
            if (await bedDlg.ShowDialog<bool?>(win) != true) return;
            _selectedBedsNote = "تخت‌های انتخاب‌شده: " + string.Join("، ", bedDlg.SelectedBeds.Select(Fa.Num));
            GuestsCountBox.Text = bedDlg.SelectedBeds.Count.ToString();
        }

        _selectedRoom = room;
        _nights = int.TryParse(NightsBox.Text, out var n) && n > 0 ? n : 1;
        _guestsCount = int.TryParse(GuestsCountBox.Text, out var gc) && gc > 0 ? gc : 1;
        _baseTotalRial = room.RateRial * _nights;
        _finalTotalRial = _baseTotalRial;

        SummaryBanner.Text = $"مهمان: {_selectedGuest.Name} | {room.Number} | {Fa.Num(_nights)} شب | {Fa.Num(_guestsCount)} نفر" +
                             (string.IsNullOrEmpty(_selectedBedsNote) ? "" : " | " + _selectedBedsNote);
        BaseAmountText.Text = Fa.Money(_baseTotalRial);
        DiscountBox.Text = "0";
        UpdateTotals();

        var accts = await _session.Api.BankAccountsAsync();
        if (accts.Ok && accts.Value is { Count: > 0 })
            ManagerAcctBox.Text = $"{accts.Value[0].BankName} ({accts.Value[0].AccountNumber})";

        Step1Panel.IsVisible = false;
        Step2Panel.IsVisible = false;
        Step3Panel.IsVisible = true;
        BackBtn.IsVisible = true;
    }

    private void OnDiscountChanged(object? sender, TextChangedEventArgs e)
    {
        if (_formatting) return;
        var digits = new string((DiscountBox.Text ?? "").Where(char.IsDigit).ToArray());
        var disc = long.TryParse(digits, out var d) ? d : 0;
        _formatting = true;
        var fmt = disc.ToString("N0", CultureInfo.InvariantCulture);
        if (DiscountBox.Text != fmt)
        {
            DiscountBox.Text = fmt;
            DiscountBox.CaretIndex = fmt.Length;
        }
        _formatting = false;
        UpdateTotals();
    }

    private void UpdateTotals()
    {
        var digits = new string((DiscountBox.Text ?? "").Where(char.IsDigit).ToArray());
        var disc = long.TryParse(digits, out var d) ? d : 0;
        _finalTotalRial = Math.Max(0, _baseTotalRial - disc);
        FinalTotalText.Text = Fa.Money(_finalTotalRial);

        if (PayMethodCombo.SelectedIndex == 3)
        {
            _formatting = true;
            CashSplitBox.Text = _finalTotalRial.ToString("N0", CultureInfo.InvariantCulture);
            PosSplitBox.Text = "0";
            _formatting = false;
        }
    }

    private void OnPayMethodChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (CombinedPanel is null) return;
        CombinedPanel.IsVisible = PayMethodCombo.SelectedIndex == 3;
        if (CombinedPanel.IsVisible)
        {
            _formatting = true;
            CashSplitBox.Text = _finalTotalRial.ToString("N0", CultureInfo.InvariantCulture);
            PosSplitBox.Text = "0";
            _formatting = false;
        }
    }

    private void OnCashSplitChanged(object? sender, TextChangedEventArgs e)
    {
        if (_formatting) return;
        var digits = new string((CashSplitBox.Text ?? "").Where(char.IsDigit).ToArray());
        var cash = Math.Clamp(long.TryParse(digits, out var v) ? v : 0, 0, _finalTotalRial);
        var pos = Math.Max(0, _finalTotalRial - cash);
        _formatting = true;
        var cFmt = cash.ToString("N0", CultureInfo.InvariantCulture);
        if (CashSplitBox.Text != cFmt) { CashSplitBox.Text = cFmt; CashSplitBox.CaretIndex = cFmt.Length; }
        PosSplitBox.Text = pos.ToString("N0", CultureInfo.InvariantCulture);
        _formatting = false;
    }

    private void OnPosSplitChanged(object? sender, TextChangedEventArgs e)
    {
        if (_formatting) return;
        var digits = new string((PosSplitBox.Text ?? "").Where(char.IsDigit).ToArray());
        var pos = Math.Clamp(long.TryParse(digits, out var v) ? v : 0, 0, _finalTotalRial);
        var cash = Math.Max(0, _finalTotalRial - pos);
        _formatting = true;
        var pFmt = pos.ToString("N0", CultureInfo.InvariantCulture);
        if (PosSplitBox.Text != pFmt) { PosSplitBox.Text = pFmt; PosSplitBox.CaretIndex = pFmt.Length; }
        CashSplitBox.Text = cash.ToString("N0", CultureInfo.InvariantCulture);
        _formatting = false;
    }

    private async void OnCompleteStay(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (_selectedGuest is null || _selectedRoom is null) return;
        var rawRoom = _selectedRoom.Number.Replace("اتاق ", "").Trim();
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var checkOut = DateTime.Today.AddDays(_nights).ToString("yyyy-MM-dd");

        var res = await _session.Api.AddReservationAsync(
            _selectedGuest.Name, _selectedGuest.RawPhone, rawRoom, today, checkOut, _guestsCount, _finalTotalRial, _finalTotalRial);
        if (!res.Ok || res.Value is null) { WizardStatus.Text = res.Error; return; }

        var ci = await _session.Api.CheckInAsync(res.Value.Code ?? "");
        if (!ci.Ok) { WizardStatus.Text = ci.Error; return; }

        if (_finalTotalRial > 0)
        {
            if (PayMethodCombo.SelectedIndex == 3)
            {
                var cDigits = new string((CashSplitBox.Text ?? "").Where(char.IsDigit).ToArray());
                var pDigits = new string((PosSplitBox.Text ?? "").Where(char.IsDigit).ToArray());
                var cash = long.TryParse(cDigits, out var cv) ? cv : 0;
                var pos = long.TryParse(pDigits, out var pv) ? pv : 0;
                if (cash > 0) await _session.Api.AddTransactionAsync("Receive", cash, "Cash", $"پذیرش ترکیبی (نقد) {rawRoom} — {ManagerAcctBox.Text}", _selectedGuest.Name, rawRoom);
                if (pos > 0) await _session.Api.AddTransactionAsync("Receive", pos, "Pos", $"پذیرش ترکیبی (پوز/کارت) {rawRoom} — {ManagerAcctBox.Text}", _selectedGuest.Name, rawRoom);
            }
            else
            {
                var method = PayMethodCombo.SelectedIndex == 1 ? "Transfer" : (PayMethodCombo.SelectedIndex == 2 ? "Cash" : "Pos");
                var desc = $"پذیرش {rawRoom} — حساب: {ManagerAcctBox.Text} — {PayNoteBox.Text}".Trim(' ', '—');
                await _session.Api.AddTransactionAsync("Receive", _finalTotalRial, method, desc, _selectedGuest.Name, rawRoom);
            }
        }

        var note = string.Join(" | ", new[] { _selectedBedsNote, PayNoteBox.Text }.Where(x => !string.IsNullOrWhiteSpace(x)));
        ReceiptPrinter.PrintInvoice(
            res.Value.Code ?? "—", _selectedGuest.Name, _selectedGuest.PhoneFa, _selectedRoom.Number,
            Fa.Date(today), Fa.Date(checkOut), _guestsCount, _nights,
            Fa.Money(_finalTotalRial), Fa.Money(_finalTotalRial), Fa.Money(0), note,
            _baseTotalRial, _discountRial);

        WizardStatus.Text = $"✓ اقامت {_selectedGuest.Name} در {_selectedRoom.Number} ثبت و فاکتور صادر شد.";
        Step1Panel.IsVisible = true;
        Step2Panel.IsVisible = false;
        Step3Panel.IsVisible = false;
        BackBtn.IsVisible = false;
    }

    private void OnBack(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (Step3Panel.IsVisible)
        {
            Step3Panel.IsVisible = false;
            Step2Panel.IsVisible = true;
        }
        else if (Step2Panel.IsVisible)
        {
            Step2Panel.IsVisible = false;
            Step1Panel.IsVisible = true;
            BackBtn.IsVisible = false;
        }
    }
}
