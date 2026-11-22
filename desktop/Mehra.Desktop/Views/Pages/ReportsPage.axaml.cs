using System.Diagnostics;
using System.Net;
using System.Text;
using Avalonia.Controls;
using Avalonia.Interactivity;

namespace Mehra.Desktop.Views.Pages;

public class ReportRowItem
{
    public string Title { get; set; } = "";
    public string Subtitle { get; set; } = "";
    public string Amount { get; set; } = "";
}

public partial class ReportsPage : UserControl
{
    private readonly Session _session;
    private List<ReportRowItem> _currentRows = new();

    public ReportsPage(Session session)
    {
        InitializeComponent();
        _session = session;
        RangeCombo.SelectedIndex = 0;
        ReportCombo.SelectedIndex = 0;
        _ = LoadAsync();
    }

    private void OnRangeChanged(object? sender, SelectionChangedEventArgs e) => _ = LoadAsync();

    private async Task LoadAsync()
    {
        if (RangeCombo == null || ReportCombo == null) return;
        var rangeTag = (RangeCombo.SelectedItem as ComboBoxItem)?.Tag?.ToString() ?? "all";
        var reportTag = (ReportCombo.SelectedItem as ComboBoxItem)?.Tag?.ToString() ?? "finance";

        var txRes = await _session.Api.TransactionsAsync();
        var resRes = await _session.Api.ReservationsAsync();
        var roomRes = await _session.Api.RoomsAsync();

        var txs = txRes.Value ?? new();
        var stays = resRes.Value ?? new();
        var rooms = roomRes.Value ?? new();

        var minDate = rangeTag switch
        {
            "today" => DateTime.Today,
            "week" => DateTime.Today.AddDays(-7),
            "month" => DateTime.Today.AddDays(-30),
            _ => DateTime.MinValue
        };

        var filteredTxs = txs.Where(t =>
            DateTime.TryParse(t.OccurredAtUtc, out var dt) ? dt.Date >= minDate : true).ToList();

        long income = filteredTxs.Where(t => t.Type == "Receive").Sum(t => t.AmountRial);
        long debt = stays.Where(s => (s.RemainingRial ?? 0) > 0).Sum(s => s.RemainingRial ?? 0);
        int occ = rooms.Count(r => string.Equals(r.Status, "Occupied", StringComparison.OrdinalIgnoreCase));

        TotalIncomeText.Text = Fa.Money(income);
        TotalDebtText.Text = Fa.Money(debt);
        TotalStaysText.Text = Fa.Num(stays.Count);
        OccupiedRoomsText.Text = $"{Fa.Num(occ)} از {Fa.Num(rooms.Count)}";

        _currentRows = reportTag switch
        {
            "debt" => stays.Where(s => (s.RemainingRial ?? 0) > 0).Select(s => new ReportRowItem
            {
                Title = $"{s.GuestName} — اتاق {Fa.Digits(s.RoomNumber)}",
                Subtitle = $"کد: {Fa.Digits(s.Code)} | ورود: {Fa.Date(s.CheckInDate)} | خروج: {Fa.Date(s.CheckOutDate)}",
                Amount = $"بدهی: {Fa.Money(s.RemainingRial ?? 0)}"
            }).ToList(),
            "stays" => stays.Select(s => new ReportRowItem
            {
                Title = $"{s.GuestName} — اتاق {Fa.Digits(s.RoomNumber)}",
                Subtitle = $"کد: {Fa.Digits(s.Code)} | {Fa.Date(s.CheckInDate)} تا {Fa.Date(s.CheckOutDate)}",
                Amount = Fa.Money(s.TotalRial ?? 0)
            }).ToList(),
            _ => filteredTxs.Select(t => new ReportRowItem
            {
                Title = $"{t.GuestName ?? "عمومی"} — {(t.Method == "Cash" ? "نقدی" : t.Method == "Transfer" ? "کارت به کارت" : "دستگاه پوز")}",
                Subtitle = $"{t.Description ?? "تراکنش"} | اتاق {Fa.Digits(t.RoomNumber ?? "—")}",
                Amount = Fa.Money(t.AmountRial)
            }).ToList()
        };

        if (_currentRows.Count == 0)
            _currentRows.Add(new ReportRowItem { Title = "موردی در این بازه زمانی یافت نشد", Subtitle = "فیلتر زمانی یا نوع گزارش را تغییر دهید", Amount = "—" });

        ReportList.ItemsSource = _currentRows;
    }

    private void OnExportExcel(object? sender, RoutedEventArgs e)
    {
        ExportService.ExportCsv("farasa-report",
            new[] { "عنوان", "جزئیات", "مبلغ" },
            _currentRows.Select(r => new[] { r.Title, r.Subtitle, r.Amount }));
    }

    private void OnPrintReport(object? sender, RoutedEventArgs e)
    {
        var rowsHtml = string.Join("", _currentRows.Select(r =>
            $"<tr><td>{WebUtility.HtmlEncode(r.Title)}</td><td>{WebUtility.HtmlEncode(r.Subtitle)}</td><td><strong>{WebUtility.HtmlEncode(r.Amount)}</strong></td></tr>"));
        var html = $@"<!DOCTYPE html><html lang=""fa"" dir=""rtl""><head><meta charset=""utf-8"" /><title>گزارش جامع — فراسا</title>
<style>body{{font-family:Vazirmatn,Tahoma,sans-serif;padding:24px;color:#111}}table{{width:100%;border-collapse:collapse;margin-top:14px}}th,td{{border:1px solid #222;padding:8px;text-align:right;font-size:12px}}th{{background:#EFEFEF}}</style></head>
<body><h2>گزارش جامع — مهمان‌پذیر فرهنگیان ناحیه ۳ اهواز</h2>
<p>تاریخ گزارش: {Fa.DateLong(DateTime.Today)} | مجری: شرکت نگین الماس برتر صیدون</p>
<table><tr><th>عنوان</th><th>جزئیات</th><th>مبلغ</th></tr>{rowsHtml}</table>
<button onclick=""window.print()"" style=""margin-top:14px;padding:8px 18px;background:#111;color:#fff;border:none;border-radius:6px;cursor:pointer"">چاپ گزارش</button>
</body></html>";
        var file = Path.Combine(Path.GetTempPath(), $"farasa-report-{Guid.NewGuid():N}.html");
        File.WriteAllText(file, html, Encoding.UTF8);
        try { Process.Start(new ProcessStartInfo(file) { UseShellExecute = true }); } catch { }
    }
}
