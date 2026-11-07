using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Mehra.Desktop;

public abstract class VmBase : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;
    protected void Raise([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    protected bool Set<T>(ref T field, T value, [CallerMemberName] string name = "")
    {
        if (EqualityComparer<T>.Default.Equals(field, value)) return false;
        field = value;
        Raise(name);
        return true;
    }
}

public class HomeVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<StatCard> Cards { get; } = new();
    public ObservableCollection<AlertItem> Alerts { get; } = new();
    public ObservableCollection<ActivityItem> Activities { get; } = new();
    private string _greeting = "";
    public string Greeting { get => _greeting; set => Set(ref _greeting, value); }
    private string _todayFa = "";
    public string TodayFa { get => _todayFa; set => Set(ref _todayFa, value); }
    private string _occupancyFa = "";
    public string OccupancyFa { get => _occupancyFa; set => Set(ref _occupancyFa, value); }
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public HomeVm(Session session)
    {
        _session = session;
        Greeting = "سلام " + session.User.FullName + " عزیز";
        TodayFa = "امروز " + Fa.DateLong(DateTime.Today);
        _ = LoadAsync();
    }

    private async Task LoadAsync()
    {
        var r = await _session.Api.DashboardAsync();
        if (!r.Ok) { Status = r.Error; return; }
        var d = r.Value!;
        Cards.Clear();
        void Add(string label, string value, string caption, Avalonia.Media.IBrush accent) =>
            Cards.Add(new StatCard { Label = label, Value = value, Caption = caption, Accent = accent });
        var gold = new Avalonia.Media.SolidColorBrush(Avalonia.Media.Color.Parse("#B08D3E"));
        var green = new Avalonia.Media.SolidColorBrush(Avalonia.Media.Color.Parse("#2E7D5B"));
        var blue = new Avalonia.Media.SolidColorBrush(Avalonia.Media.Color.Parse("#2F6690"));
        var red = new Avalonia.Media.SolidColorBrush(Avalonia.Media.Color.Parse("#B3261E"));
        Add("اتاق‌ها", Fa.Num(d.TotalRooms), Fa.Num(d.FreeRooms) + " اتاق خالی", blue);
        Add("پر / اشغال", Fa.Num(d.OccupiedRooms + d.ReservedRooms), Fa.Num(d.OccupiedRooms) + " با مهمان", gold);
        Add("درصد اشغال", Fa.Digits(d.OccupancyPercent.ToString("0.#", System.Globalization.CultureInfo.InvariantCulture) + "%"), "از کل اتاق‌ها", green);
        Add("ورودی امروز", Fa.Num(d.TodayCheckIns), "خروجی: " + Fa.Num(d.TodayCheckOuts), blue);
        Add("درآمد امروز", Fa.Money(d.RevenueTodayRial), "به ریال", gold);
        Add("نظافت / تعمیر", Fa.Num(d.CleaningRooms + d.MaintenanceRooms), Fa.Num(d.CleaningRooms) + " نظافت، " + Fa.Num(d.MaintenanceRooms) + " تعمیر", red);
        Alerts.Clear();
        foreach (var a in d.Alerts ?? new List<DashboardAlertDto>())
            Alerts.Add(new AlertItem { Title = a.Title ?? "", Message = a.Message ?? "", Accent = StatusText.SeverityBrush(a.Severity) });
        Activities.Clear();
        foreach (var a in d.RecentActivities ?? new List<DashboardActivityDto>())
            Activities.Add(new ActivityItem { Title = a.Title ?? "", TimeFa = Fa.Time(a.CreatedAtUtc) });
        OccupancyFa = "هفت روز اخیر: " + string.Join("، ",
            (d.OccupancySeries ?? new List<OccupancyPointDto>()).Select(p => Fa.Date(p.Date) + ": " + Fa.Num(p.Occupied) + "/" + Fa.Num(p.Total)));
        Status = "به‌روز شد — " + _session.Api.ServerLabel;
    }
}

public class RoomsVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<RoomCard> Rooms { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public RoomsVm(Session session)
    {
        _session = session;
        _ = LoadAsync();
    }

    private async Task LoadAsync()
    {
        var r = await _session.Api.RoomsAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Rooms.Clear();
        foreach (var room in r.Value!)
            Rooms.Add(new RoomCard
            {
                Number = "اتاق " + Fa.Digits(room.RoomNumber ?? "—"),
                TypeName = room.RoomTypeName ?? "",
                Details = "طبقهٔ " + Fa.Num(room.Floor) + " — " + Fa.Num(room.Capacity) + " نفر",
                RateFa = Fa.Money(room.RatePerNightRial) + " / شب",
                StatusFa = StatusText.RoomFa(room.Status),
                StatusBrush = StatusText.RoomBrush(room.Status),
                GuestName = string.IsNullOrWhiteSpace(room.CurrentGuestName) ? "" : "مهمان: " + room.CurrentGuestName
            });
        Status = Fa.Num(Rooms.Count) + " اتاق — به‌روز شد";
    }
}

public class ReservationsVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<ReservationRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public ReservationsVm(Session session)
    {
        _session = session;
        _ = LoadAsync();
    }

    private async Task LoadAsync()
    {
        var r = await _session.Api.ReservationsAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var v in r.Value!)
            Items.Add(new ReservationRow
            {
                Code = Fa.Digits(v.Code ?? "—"),
                GuestName = v.GuestName ?? "",
                PhoneFa = Fa.Digits(v.GuestPhone ?? ""),
                RoomNumber = "اتاق " + Fa.Digits(v.RoomNumber ?? "—"),
                DatesFa = Fa.Range(v.CheckInDate, v.CheckOutDate),
                GuestsFa = Fa.Num(v.GuestsCount) + " نفر",
                AmountFa = Fa.Money(v.TotalRial),
                PaidFa = "پرداخت‌شده: " + Fa.Money(v.PaidRial),
                StatusFa = StatusText.ReservationFa(v.Status),
                StatusBrush = StatusText.ReservationBrush(v.Status)
            });
        Status = Fa.Num(Items.Count) + " رزرو — به‌روز شد";
    }
}

public class ReceptionVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<StayRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public ReceptionVm(Session session)
    {
        _session = session;
        _ = LoadAsync();
    }

    private async Task LoadAsync()
    {
        var r = await _session.Api.StaysAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var s in r.Value!)
            Items.Add(new StayRow
            {
                Code = Fa.Digits(s.ReservationCode ?? "—"),
                GuestName = s.GuestName ?? "",
                RoomNumber = "اتاق " + Fa.Digits(s.RoomNumber ?? "—"),
                SinceFa = "ورود: " + Fa.Date(s.CheckInAtUtc),
                PlannedOutFa = "تخلیهٔ برنامه‌ریزی‌شده: " + Fa.Date(s.PlannedCheckOut),
                GuestsFa = Fa.Num(s.GuestsCount) + " نفر",
                StatusFa = StatusText.StayFa(s.Status),
                StatusBrush = StatusText.StayBrush(s.Status)
            });
        Status = Fa.Num(Items.Count) + " اقامت فعال — به‌روز شد";
    }
}

public class SettingsVm : VmBase
{
    private readonly Session _session;
    private string _serverLabel;
    public string ServerLabel { get => _serverLabel; set => Set(ref _serverLabel, value); }
    private string _health = "";
    public string Health { get => _health; set => Set(ref _health, value); }
    private string _mode = "";
    public string Mode { get => _mode; set => Set(ref _mode, value); }

    private bool _isSql;
    public bool IsSql { get => _isSql; set => Set(ref _isSql, value); }
    private string _schemaStatus = "";
    public string SchemaStatus { get => _schemaStatus; set => Set(ref _schemaStatus, value); }

    public SettingsVm(Session session)
    {
        _session = session;
        _serverLabel = session.Api.ServerLabel;
        IsSql = session.Api is SqlServerApi;
        Mode = session.Api.IsDemo
            ? "حالت: نسخهٔ نمایشی (دادهٔ نمونه — بدون اتصال به سرور)"
            : IsSql ? "حالت: اتصال مستقیم به SQL Server (جدول‌ها از فایل db\schema.sql ساخته می‌شوند)"
            : "حالت: اتصال به سرور وب (API)";
    }

    public async Task EnsureSchemaAsync()
    {
        if (_session.Api is not SqlServerApi sql) return;
        SchemaStatus = "در حال اجرای اسکریپت db\schema.sql …";
        var path = Path.Combine(AppContext.BaseDirectory, "db", "schema.sql");
        var r = await SqlServerApi.EnsureSchemaAsync(sql.MasterConnectionString, path);
        SchemaStatus = r.Ok ? r.Value! : r.Error;
    }

    public async Task CheckHealthAsync()
    {
        Health = "در حال بررسی…";
        var r = await _session.Api.HealthAsync();
        Health = r.Ok ? r.Value! : r.Error;
    }
}
