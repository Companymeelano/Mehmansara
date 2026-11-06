using System.Collections.ObjectModel;
using Avalonia.Media;

namespace Mehra.Desktop;

public static class OpsText
{
    private static IBrush C(string hex) => new SolidColorBrush(Color.Parse(hex));

    public static string TaskStatusFa(string? s) => s?.ToUpperInvariant() switch
    {
        "PENDING" => "در انتظار تخصیص",
        "ASSIGNED" => "تخصیص داده شده",
        "INPROGRESS" => "در حال انجام",
        "DONE" => "پایان یافته",
        "APPROVED" => "تأیید نهایی",
        "REJECTED" => "رد شده",
        _ => s ?? "—"
    };
    public static IBrush TaskStatusBrush(string? s) => s?.ToUpperInvariant() switch
    {
        "PENDING" => C("#B7791F"),
        "ASSIGNED" => C("#2F6690"),
        "INPROGRESS" => C("#B08D3E"),
        "DONE" or "APPROVED" => C("#2E7D5B"),
        "REJECTED" => C("#B3261E"),
        _ => Brushes.Gray
    };

    public static string CleaningTypeFa(string? s) => s?.ToUpperInvariant() switch
    {
        "CHECKOUT" => "نظافت پس از خروج",
        "DAILY" => "نظافت روزانه",
        "DEEP" => "نظافت اساسی",
        "TURNDOWN" => "آماده‌سازی شب",
        _ => s ?? "—"
    };

    public static string PriorityFa(string? s) => s?.ToUpperInvariant() switch
    {
        "LOW" => "کم",
        "NORMAL" => "عادی",
        "HIGH" => "بالا",
        "URGENT" => "فوری",
        _ => s ?? "—"
    };
    public static IBrush PriorityBrush(string? s) => s?.ToUpperInvariant() switch
    {
        "URGENT" => C("#B3261E"),
        "HIGH" => C("#B7791F"),
        "NORMAL" => C("#2F6690"),
        "LOW" => C("#8A93A3"),
        _ => Brushes.Gray
    };

    public static string MaintStatusFa(string? s) => s?.ToUpperInvariant() switch
    {
        "OPEN" => "ثبت شده",
        "ASSIGNED" => "ارجاع شده",
        "INPROGRESS" => "در حال تعمیر",
        "WAITINGPARTS" => "در انتظار قطعه",
        "RESOLVED" => "رفع نقص",
        "CLOSED" => "بسته شده",
        _ => s ?? "—"
    };
    public static IBrush MaintStatusBrush(string? s) => s?.ToUpperInvariant() switch
    {
        "OPEN" => C("#B7791F"),
        "ASSIGNED" => C("#2F6690"),
        "INPROGRESS" => C("#B08D3E"),
        "RESOLVED" => C("#2E7D5B"),
        "CLOSED" => C("#8A93A3"),
        _ => Brushes.Gray
    };

    public static string TxnTypeFa(string? s) => s?.ToUpperInvariant() switch
    {
        "RECEIVE" => "دریافت",
        "PAY" => "پرداخت",
        "DISCOUNT" => "تخفیف",
        "REFUND" => "بازگشت وجه",
        _ => s ?? "—"
    };
    public static IBrush TxnTypeBrush(string? s) => s?.ToUpperInvariant() switch
    {
        "RECEIVE" => C("#2E7D5B"),
        "PAY" => C("#B3261E"),
        "DISCOUNT" => C("#B7791F"),
        "REFUND" => C("#7A5EA8"),
        _ => Brushes.Gray
    };

    public static string MethodFa(string? s) => s?.ToUpperInvariant() switch
    {
        "CASH" => "نقد",
        "POS" => "کارت‌خوان",
        "CARD" => "کارت به کارت",
        "TRANSFER" => "حواله بانکی",
        "ONLINE" => "درگاه اینترنتی",
        "CHECK" => "چک",
        "OTHER" => "سایر",
        _ => s ?? "—"
    };

    public static string SeverityFa(string? s) => s?.ToUpperInvariant() switch
    {
        "CRITICAL" => "بحرانی",
        "HIGH" => "مهم",
        "MEDIUM" => "متوسط",
        "LOW" => "کم",
        _ => s ?? "—"
    };
}

public class GuestsVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<GuestRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public GuestsVm(Session session) { _session = session; _ = LoadAsync(); }

    private async Task LoadAsync()
    {
        var r = await _session.Api.GuestsAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var g in r.Value!)
        {
            var bal = g.BalanceRial ?? 0;
            Items.Add(new GuestRow
            {
                Name = ((g.FirstName ?? "") + " " + (g.LastName ?? "")).Trim(),
                PhoneFa = Fa.Digits(g.Phone ?? ""),
                City = g.City ?? "",
                BalanceFa = Fa.Money(g.BalanceRial) + (bal > 0 ? " — بدهکار" : ""),
                BalanceBrush = bal > 0
                    ? new SolidColorBrush(Color.Parse("#B3261E"))
                    : new SolidColorBrush(Color.Parse("#2E7D5B")),
                StaysFa = Fa.Num(g.TotalStays ?? 0) + " اقامت",
                LastFa = Fa.Date(g.LastStayAtUtc)
            });
        }
        Status = Fa.Num(Items.Count) + " مهمان — به‌روز شد";
    }
}

public class FinanceVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<StatCard> Cards { get; } = new();
    public ObservableCollection<TxnRow> Transactions { get; } = new();
    public ObservableCollection<DebtorRow> Debtors { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public FinanceVm(Session session) { _session = session; _ = LoadAsync(); }

    private async Task LoadAsync()
    {
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var weekAgo = DateTime.Today.AddDays(-7).ToString("yyyy-MM-dd");
        var box = await _session.Api.CashboxAsync(today);
        var txns = await _session.Api.TransactionsAsync(weekAgo, today);
        var debtors = await _session.Api.DebtorsAsync();
        if (!box.Ok) { Status = box.Error; return; }
        var b = box.Value!;
        Cards.Clear();
        void Add(string label, string value, string caption, string hex) =>
            Cards.Add(new StatCard { Label = label, Value = value, Caption = caption, Accent = new SolidColorBrush(Color.Parse(hex)) });
        Add("دریافتی امروز", Fa.Money(b.ReceivedRial), Fa.Num(b.TransactionCount) + " تراکنش", "#2E7D5B");
        Add("پرداختی امروز", Fa.Money(b.PaidRial), "", "#B3261E");
        Add("خالص صندوق امروز", Fa.Money(b.NetRial), "تخفیف: " + Fa.Money(b.DiscountsRial), "#B08D3E");
        Add("ماندهٔ صندوق", Fa.Money(b.OpenBalanceRial), "", "#2F6690");
        Transactions.Clear();
        if (txns.Ok)
            foreach (var t in txns.Value!)
                Transactions.Add(new TxnRow
                {
                    Code = Fa.Digits(t.Code ?? "—"),
                    TypeFa = OpsText.TxnTypeFa(t.Type),
                    TypeBrush = OpsText.TxnTypeBrush(t.Type),
                    AmountFa = Fa.Money(t.AmountRial),
                    MethodFa = OpsText.MethodFa(t.Method),
                    Guest = t.GuestName ?? "",
                    Room = string.IsNullOrEmpty(t.RoomNumber) ? "" : "اتاق " + Fa.Digits(t.RoomNumber),
                    Operator = t.OperatorName ?? "",
                    DateFa = Fa.Date(t.CreatedAtUtc),
                    ReversedFa = t.IsReversed == true ? "معکوس شده" : ""
                });
        Debtors.Clear();
        if (debtors.Ok)
            foreach (var d in debtors.Value!)
                Debtors.Add(new DebtorRow
                {
                    Name = d.GuestName ?? "",
                    PhoneFa = Fa.Digits(d.Phone ?? ""),
                    Room = string.IsNullOrEmpty(d.RoomNumber) ? "" : "اتاق " + Fa.Digits(d.RoomNumber),
                    BalanceFa = Fa.Money(d.BalanceRial),
                    LastFa = Fa.Date(d.LastActivityAtUtc)
                });
        Status = "به‌روز شد — بازهٔ تراکنش‌ها: " + Fa.Range(weekAgo, today);
    }
}

public class HousekeepingVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<TaskRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public HousekeepingVm(Session session) { _session = session; _ = LoadAsync(); }

    private async Task LoadAsync()
    {
        var r = await _session.Api.HousekeepingAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var t in r.Value!)
            Items.Add(new TaskRow
            {
                Room = "اتاق " + Fa.Digits(t.RoomNumber ?? "—"),
                TypeFa = OpsText.CleaningTypeFa(t.Type),
                StatusFa = OpsText.TaskStatusFa(t.Status),
                StatusBrush = OpsText.TaskStatusBrush(t.Status),
                Assignee = t.AssignedToName ?? "—",
                Notes = t.Notes ?? "",
                CreatedFa = Fa.Date(t.CreatedAtUtc)
            });
        Status = Fa.Num(Items.Count) + " کار — به‌روز شد";
    }
}

public class MaintenanceVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<TicketRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public MaintenanceVm(Session session) { _session = session; _ = LoadAsync(); }

    private async Task LoadAsync()
    {
        var r = await _session.Api.MaintenanceAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var t in r.Value!)
            Items.Add(new TicketRow
            {
                Code = Fa.Digits(t.Code ?? "—"),
                Title = t.Title ?? "",
                Room = "اتاق " + Fa.Digits(t.RoomNumber ?? "—"),
                PriorityFa = OpsText.PriorityFa(t.Priority),
                PriorityBrush = OpsText.PriorityBrush(t.Priority),
                StatusFa = OpsText.MaintStatusFa(t.Status),
                StatusBrush = OpsText.MaintStatusBrush(t.Status),
                Assignee = t.AssigneeName ?? "—",
                CostFa = Fa.Money(t.CostRial),
                CreatedFa = Fa.Date(t.CreatedAtUtc)
            });
        Status = Fa.Num(Items.Count) + " تیکت — به‌روز شد";
    }
}

public class UsersVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<UserRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public UsersVm(Session session) { _session = session; _ = LoadAsync(); }

    private async Task LoadAsync()
    {
        var r = await _session.Api.UsersAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var u in r.Value!)
        {
            var active = u.IsActive != false;
            Items.Add(new UserRow
            {
                Name = u.FullName ?? "",
                Username = u.Username ?? "",
                Role = u.RoleTitle ?? u.RoleCode ?? "",
                PhoneFa = Fa.Digits(u.Phone ?? ""),
                ActiveFa = active ? "فعال" : "غیرفعال",
                ActiveBrush = active
                    ? new SolidColorBrush(Color.Parse("#2E7D5B"))
                    : new SolidColorBrush(Color.Parse("#B3261E")),
                LastLoginFa = Fa.Date(u.LastLoginAtUtc)
            });
        }
        Status = Fa.Num(Items.Count) + " کاربر — به‌روز شد";
    }
}

public class NotificationsVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<NotifRow> Items { get; } = new();
    private string _status = "در حال بارگذاری…";
    public string Status { get => _status; set => Set(ref _status, value); }

    public NotificationsVm(Session session) { _session = session; _ = LoadAsync(); }

    public async Task LoadAsync()
    {
        var r = await _session.Api.NotificationsAsync();
        if (!r.Ok) { Status = r.Error; return; }
        Items.Clear();
        foreach (var n in r.Value!)
            Items.Add(new NotifRow
            {
                Title = n.Title ?? "",
                Body = n.Body ?? "",
                SeverityFa = OpsText.SeverityFa(n.Severity),
                Accent = StatusText.SeverityBrush(n.Severity),
                TimeFa = Fa.Time(n.CreatedAtUtc),
                ReadFa = n.IsRead == true ? "خوانده‌شده" : "خوانده‌نشده"
            });
        var unread = Items.Count(i => i.ReadFa == "خوانده‌نشده");
        Status = Fa.Num(Items.Count) + " اعلان — " + Fa.Num(unread) + " خوانده‌نشده";
    }

    public async Task MarkAllReadAsync()
    {
        Status = "در حال ارسال…";
        var r = await _session.Api.MarkAllNotificationsReadAsync();
        Status = r.Ok ? r.Value! : r.Error;
        await LoadAsync();
    }
}

public class ReportOption
{
    public string Key { get; set; } = "";
    public string Title { get; set; } = "";
}

public class ReportsVm : VmBase
{
    private readonly Session _session;
    public ObservableCollection<ReportLine> Lines { get; } = new();
    private string _title = "";
    public string Title { get => _title; set => Set(ref _title, value); }
    private string _subtitle = "";
    public string Subtitle { get => _subtitle; set => Set(ref _subtitle, value); }
    private string _period = "";
    public string Period { get => _period; set => Set(ref _period, value); }
    private string _status = "";
    public string Status { get => _status; set => Set(ref _status, value); }

    public static readonly List<ReportOption> Options = new()
    {
        new() { Key = "occupancy", Title = "گزارش اشغال اتاق‌ها" },
        new() { Key = "check-in-out", Title = "گزارش ورود و خروج" },
        new() { Key = "reservations", Title = "گزارش رزروها" },
        new() { Key = "revenue", Title = "گزارش درآمد" },
        new() { Key = "debtors", Title = "گزارش بدهکاران" },
        new() { Key = "staff-performance", Title = "گزارش عملکرد کارکنان" },
        new() { Key = "housekeeping", Title = "گزارش خدمات" },
        new() { Key = "maintenance", Title = "گزارش تعمیرات" }
    };

    public ReportsVm(Session session) { _session = session; }

    public async Task LoadReportAsync(string key)
    {
        var today = DateTime.Today;
        var (jy, jm, _) = Jalali.ToJalali(today);
        var from = Jalali.ToGregorian(jy, jm, 1).ToString("yyyy-MM-dd");
        var to = today.ToString("yyyy-MM-dd");
        Status = "در حال دریافت گزارش…";
        Lines.Clear();
        var r = await _session.Api.ReportAsync(key, from, to);
        if (!r.Ok) { Status = r.Error; return; }
        var rep = r.Value!;
        Title = rep.Title ?? "";
        Subtitle = rep.Subtitle ?? "";
        Period = "بازه: " + Fa.Range(rep.From ?? from, rep.To ?? to) + " — تاریخ تهیه: " + Fa.Date(rep.GeneratedAtUtc);
        if (rep.Columns is { Count: > 0 })
            Lines.Add(new ReportLine { Text = string.Join(" | ", rep.Columns), IsHeader = true });
        foreach (var row in rep.Rows ?? new List<List<string>>())
            Lines.Add(new ReportLine { Text = string.Join(" | ", row.Select(Fa.Digits)) });
        foreach (var t in rep.Totals ?? new List<string>())
            Lines.Add(new ReportLine { Text = Fa.Digits(t), IsTotal = true });
        foreach (var n in rep.Notes ?? new List<string>())
            Lines.Add(new ReportLine { Text = n });
        Status = Fa.Num(rep.Rows?.Count ?? 0) + " سطر";
    }
}
