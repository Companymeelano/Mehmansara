using System.Diagnostics;
using System.Net;

namespace Mehra.Desktop;

/// <summary>
/// Generates a luxury A5 Black-and-White print-ready Persian RTL HTML invoice/receipt (v1.2.6)
/// with proper LTR-isolated Jalali dates, conditional discount & deposit rows, and company branding.
/// </summary>
public static class ReceiptPrinter
{
    public static void PrintInvoice(
        string code,
        string guestName,
        string phone,
        string roomNumber,
        string checkInFa,
        string checkOutFa,
        int guestsCount,
        int nights,
        string totalFa,
        string paidFa,
        string remainingFa,
        string? notes = null,
        long baseAmountRial = 0,
        long discountRial = 0,
        long depositRial = 0)
    {
        var codeFa = Fa.Digits(code);
        var phoneFa = Fa.Digits(phone);
        var roomFa = Fa.Digits(roomNumber);
        var checkInDisplay = Fa.Digits(checkInFa);
        var checkOutDisplay = Fa.Digits(checkOutFa);
        var issueDateDisplay = Fa.Digits(Fa.Date(DateTime.Today));

        var paymentRows = discountRial > 0
            ? $@"
    <tr><th>مبلغ اولیه اتاق / سوئیت</th><td>{WebUtility.HtmlEncode(Fa.Money(baseAmountRial))}</td><th>مبلغ تخفیف</th><td><strong>{WebUtility.HtmlEncode(Fa.Money(discountRial))}</strong></td></tr>
    <tr><th>مبلغ نهایی پس از تخفیف</th><td><strong>{WebUtility.HtmlEncode(totalFa)}</strong></td><th>مبلغ پرداخت‌شده</th><td><strong>{WebUtility.HtmlEncode(paidFa)}</strong></td></tr>"
            : $@"
    <tr><th>مبلغ اتاق / سوئیت</th><td><strong>{WebUtility.HtmlEncode(totalFa)}</strong></td><th>مبلغ پرداخت‌شده</th><td><strong>{WebUtility.HtmlEncode(paidFa)}</strong></td></tr>";

        var depositRow = depositRial > 0
            ? $@"
    <tr><th>بیعانه پرداخت‌شده</th><td><strong>{WebUtility.HtmlEncode(Fa.Money(depositRial))}</strong></td><th>مانده حساب</th><td><strong>{WebUtility.HtmlEncode(remainingFa)}</strong></td></tr>"
            : $@"
    <tr><th>مانده حساب</th><td colspan=""3""><strong>{WebUtility.HtmlEncode(remainingFa)}</strong></td></tr>";

        var html = $@"<!DOCTYPE html>
<html lang=""fa"" dir=""rtl"">
<head>
<meta charset=""utf-8"" />
<title>فاکتور اقامت — فراسا ({WebUtility.HtmlEncode(codeFa)})</title>
<style>
  @page {{ size: A5 portrait; margin: 7mm; }}
  * {{ box-sizing: border-box; }}
  body {{ font-family: Vazirmatn, Tahoma, sans-serif; background: #fff; color: #111; margin: 0; padding: 10px; font-size: 11px; }}
  .sheet {{ max-width: 148mm; margin: 0 auto; background: #fff; border: 2px solid #111; border-radius: 8px; padding: 14px; box-shadow: 4px 4px 0 #111; }}
  .head {{ display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #111; padding-bottom: 8px; margin-bottom: 10px; }}
  .brand-wrap {{ display: flex; align-items: center; gap: 8px; }}
  .emblem {{ width: 34px; height: 34px; border: 2px solid #111; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 16px; box-shadow: 2px 2px 0 #111; background: #f2f2f2; }}
  .brand {{ font-size: 18px; font-weight: 900; color: #111; line-height: 1.2; }}
  .sub {{ font-size: 10px; color: #333; font-weight: 600; }}
  .meta {{ text-align: left; font-size: 10px; line-height: 1.7; }}
  table {{ width: 100%; border-collapse: collapse; margin: 8px 0; }}
  th, td {{ border: 1px solid #222; padding: 6px 8px; text-align: right; font-size: 11px; }}
  th {{ background: #EFEFEF; color: #111; font-weight: 700; width: 24%; }}
  .ltr {{ direction: ltr; unicode-bidi: embed; display: inline-block; font-weight: 700; }}
  .rules {{ background: #F7F7F7; border: 1px solid #222; border-radius: 6px; padding: 8px 10px; font-size: 10px; line-height: 1.7; margin-top: 8px; }}
  .foot {{ margin-top: 10px; padding-top: 8px; border-top: 1.5px dashed #111; font-size: 10px; color: #111; line-height: 1.7; }}
  .company {{ text-align: center; margin-top: 6px; font-size: 9.5px; font-weight: 700; border-top: 1px solid #ccc; padding-top: 4px; }}
  @media print {{ body {{ padding: 0; }} .sheet {{ box-shadow: none; border-width: 1.5px; }} .no-print {{ display: none; }} }}
</style>
</head>
<body>
<div class=""sheet"">
  <div class=""head"">
    <div class=""brand-wrap"">
      <div class=""emblem"">◆</div>
      <div>
        <div class=""brand"">فراسا</div>
        <div class=""sub"">مهمان‌پذیر فرهنگیان ناحیه ۳ اهواز — قبض رسمی اقامت</div>
      </div>
    </div>
    <div class=""meta"">
      <div><strong>شماره قبض:</strong> {WebUtility.HtmlEncode(codeFa)}</div>
      <div><strong>تاریخ صدور:</strong> <span class=""ltr"">{WebUtility.HtmlEncode(issueDateDisplay)}</span></div>
    </div>
  </div>
  <table>
    <tr><th>نام مهمان</th><td>{WebUtility.HtmlEncode(guestName)}</td><th>تلفن همراه</th><td><span class=""ltr"">{WebUtility.HtmlEncode(phoneFa)}</span></td></tr>
    <tr><th>شماره اتاق</th><td><strong>{WebUtility.HtmlEncode(roomFa)}</strong></td><th>تعداد نفرات</th><td>{WebUtility.HtmlEncode(Fa.Num(guestsCount))} نفر</td></tr>
    <tr><th>تاریخ ورود</th><td><span class=""ltr"">{WebUtility.HtmlEncode(checkInDisplay)}</span></td><th>تاریخ خروج</th><td><span class=""ltr"">{WebUtility.HtmlEncode(checkOutDisplay)}</span></td></tr>
    <tr><th>مدت اقامت</th><td colspan=""3"">{WebUtility.HtmlEncode(Fa.Num(nights))} شب</td></tr>
    {paymentRows}
    {depositRow}
  </table>
  {(string.IsNullOrWhiteSpace(notes) ? "" : $"<p style=\"margin:6px 0;font-size:10.5px\"><strong>توضیحات:</strong> {WebUtility.HtmlEncode(notes)}</p>")}
  <div class=""rules"">
    <strong>قوانین و مقررات مهمان‌پذیر:</strong><br/>
    ۱. ساعت تحویل اتاق ۱۴:۰۰ و ساعت تخلیه ۱۲:۰۰ ظهر می‌باشد.
    ۲. ارائه کارت ملی و مدارک شناسایی معتبر هنگام پذیرش الزامی است.
  </div>
  <div class=""foot"">
    <div><strong>نشانی:</strong> اهواز ، فاز ۵ پادادشهر ، بلوار هاتف ، خیابان بشارت ۲ ( مرکز آموزش وفرهنگیان ناحیه ۳ )</div>
    <div><strong>تلفن تماس:</strong> <span class=""ltr"">۰۶۱۳۵۵۹۰۷۲۰</span></div>
    <div class=""company"">مجری و پشتیبانی سامانه: شرکت نگین الماس برتر صیدون</div>
  </div>
  <div class=""no-print"" style=""margin-top:10px;text-align:left"">
    <button onclick=""window.print()"" style=""background:#111;color:#fff;border:none;padding:8px 18px;border-radius:6px;cursor:pointer;font-family:inherit"">چاپ فاکتور (A5)</button>
  </div>
</div>
</body>
</html>";

        var file = Path.Combine(Path.GetTempPath(), $"farasa-invoice-{Guid.NewGuid():N}.html");
        File.WriteAllText(file, html, System.Text.Encoding.UTF8);
        Process.Start(new ProcessStartInfo(file) { UseShellExecute = true });
    }
}
