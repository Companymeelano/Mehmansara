using System.Globalization;
using Avalonia.Controls;
using Avalonia.Media;

namespace Mehra.Desktop.Views.Pages;

public partial class ReceptionPage : UserControl
{
    private readonly Session _session;
    private List<GuestRow> _allGuests = new();
    private GuestRow? _selectedGuest;
    private RoomCard? _selectedRoom;
    private int _nights = 1;
    private int _guestsCount = 1;
    private long _totalRial;

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
            Name = ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim(),
            PhoneFa = Fa.Digits(g.Phone ?? ""),
            City = g.City ?? ""
        }).ToList();
        FilterGuests(GuestSearchBox.Text ?? "");
    }

    private void FilterGuests(string q)
    {
        q = q.Trim();
        GuestList.ItemsSource = string.IsNullOrEmpty(q)
            ? _allGuests
            : _allGuests.Where(g =>
                g.Name.Contains(q, StringComparison.OrdinalIgnoreCase) ||
                g.RawPhone.Contains(q, StringComparison.OrdinalIgnoreCase) ||
                g.RawCity.Contains(q, StringComparison.OrdinalIgnoreCase)).ToList();
    }

    private void OnGuestSearch(object? sender, TextChangedEventArgs e) =>
        FilterGuests(GuestSearchBox.Text ?? "");

    private async void OnAddNewGuest(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (TopLevel.GetTopLevel(this) is not Window win) return;
        var dlg = new InputDialog("معرفی و ثبت مهمان جدید",
            ("نام", ""), ("نام خانوادگی", ""), ("نام پدر", ""),
            ("شماره شناسنامه", ""), ("کد ملی", ""), ("تاریخ تولد", ""),
            ("تلفن همراه", ""), ("محل سکونت فعلی", ""),
            ("نوع خودرو", ""), ("شماره پلاک", ""), ("تعداد مهمان", "1"),
            ("فرهنگی یا آزاد", "فرهنگی"), ("نسبت با فرهنگی (در صورت انتخاب)", ""), ("توضیحات", ""));
        dlg.Required = new[] { false, true, false, false, false, false, false, false, false, false, false, false, false, false };
        if (await dlg.ShowDialog<bool?>(win) == true)
        {
            var r = await _session.Api.AddGuestAsync(
                dlg.Values["نام"], dlg.Values["نام خانوادگی"],
                string.IsNullOrWhiteSpace(dlg.Values["تلفن همراه"]) ? null : dlg.Values["تلفن همراه"],
                string.IsNullOrWhiteSpace(dlg.Values["محل سکونت فعلی"]) ? null : dlg.Values["محل سکونت فعلی"]);
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
        FreeRoomList.ItemsSource = rr.Value
            .Where(rm => rm.Status == "FREE")
            .Select(rm => new RoomCard
            {
                Id = rm.Id ?? rm.RoomNumber ?? "",
                Floor = rm.Floor,
                Capacity = rm.Capacity,
                RateRial = rm.RatePerNightRial ?? 0,
                Amenities = rm.Amenities ?? "",
                Number = "اتاق " + Fa.Digits(rm.RoomNumber ?? "—"),
                TypeName = rm.RoomTypeName ?? "",
                Details = (rm.Floor == 0 ? "همکف" : "طبقهٔ " + Fa.Num(rm.Floor)) + " — " + Fa.Num(rm.Capacity) + " نفر",
                RateFa = Fa.Money(rm.RatePerNightRial) + " / شب",
                StatusFa = StatusText.RoomFa(rm.Status),
                StatusBrush = StatusText.RoomBrush(rm.Status)
            }).ToList();
        Step1Panel.IsVisible = false;
        Step2Panel.IsVisible = true;
        Step3Panel.IsVisible = false;
        BackBtn.IsVisible = true;
    }

    private async void OnSelectRoom(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (sender is not Button { Tag: RoomCard room } || _selectedGuest is null) return;
        _selectedRoom = room;
        _nights = int.TryParse(NightsBox.Text, out var n) && n > 0 ? n : 1;
        _guestsCount = int.TryParse(GuestsCountBox.Text, out var gc) && gc > 0 ? gc : 1;
        _totalRial = room.RateRial * _nights;
        SummaryBanner.Text = $"مهمان: {_selectedGuest.Name} | {room.Number} | {Fa.Num(_nights)} شب | مبلغ کل: {Fa.Money(_totalRial)}";
        PayAmountBox.Text = _totalRial.ToString(CultureInfo.InvariantCulture);
        var accts = await _session.Api.BankAccountsAsync();
        if (accts.Ok && accts.Value is { Count: > 0 })
            ManagerAcctBox.Text = $"{accts.Value[0].BankName} ({accts.Value[0].AccountNumber})";
        Step1Panel.IsVisible = false;
        Step2Panel.IsVisible = false;
        Step3Panel.IsVisible = true;
        BackBtn.IsVisible = true;
    }

    private async void OnCompleteStay(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (_selectedGuest is null || _selectedRoom is null) return;
        var rawRoom = _selectedRoom.Number.Replace("اتاق ", "").Trim();
        var paidDigits = new string((PayAmountBox.Text ?? "").Where(char.IsDigit).ToArray());
        var paid = long.TryParse(paidDigits, out var p) ? p : 0;
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var checkOut = DateTime.Today.AddDays(_nights).ToString("yyyy-MM-dd");

        var res = await _session.Api.AddReservationAsync(
            _selectedGuest.Name, _selectedGuest.RawPhone, rawRoom, today, checkOut, _guestsCount, _totalRial, paid);
        if (!res.Ok || res.Value is null) { WizardStatus.Text = res.Error; return; }

        var ci = await _session.Api.CheckInAsync(res.Value.Code ?? "");
        if (!ci.Ok) { WizardStatus.Text = ci.Error; return; }

        if (paid > 0)
        {
            var method = PayMethodCombo.SelectedIndex == 1 ? "Pos" : (PayMethodCombo.SelectedIndex == 2 ? "Transfer" : "Cash");
            var desc = $"پذیرش {rawRoom} — حساب: {ManagerAcctBox.Text} — {PayNoteBox.Text}".Trim(' ', '—');
            await _session.Api.AddTransactionAsync("Receive", paid, method, desc, _selectedGuest.Name);
        }

        ReceiptPrinter.PrintInvoice(
            res.Value.Code ?? "—", _selectedGuest.Name, _selectedGuest.PhoneFa, _selectedRoom.Number,
            Fa.Date(today), Fa.Date(checkOut), _guestsCount, _nights,
            Fa.Money(_totalRial), Fa.Money(paid), Fa.Money(Math.Max(0, _totalRial - paid)),
            PayNoteBox.Text);

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
