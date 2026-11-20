using System.Diagnostics;
using System.Net;

namespace Mehra.Desktop;

/// <summary>
/// Generates a printable Persian RTL HTML invoice/receipt (v1.2.5) with Persian digits,
/// smart 3-part payment breakdown (base amount, discount when > 0, final amount),
/// address, and contact info, then opens it in the default browser.
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
        long discountRial = 0)
    {
        var codeFa = Fa.Digits(code);
        var phoneFa = Fa.Digits(phone);
        var roomFa = Fa.Digits(roomNumber);
        var checkInDisplay = Fa.Digits(checkInFa);
        var checkOutDisplay = Fa.Digits(checkOutFa);

        var paymentRows = discountRial > 0
            ? $@"
    <tr><th>مبلغ اولیه اتاق / سوئیت</th><td>{WebUtility.HtmlEncode(Fa.Money(baseAmountRial))}</td><th>مبلغ تخفیف</th><td style=""color:#1E7E34;font-weight:700"">{WebUtility.HtmlEncode(Fa.Money(discountRial))}</td></tr>
    <tr><th>مبلغ نهایی پس از تخفیف</th><td><strong>{WebUtility.HtmlEncode(totalFa)}</strong></td><th>مبلغ پرداخت‌شده</th><td><strong>{WebUtility.HtmlEncode(paidFa)}</strong></td></tr>
    <tr><th>مانده حساب</th><td colspan=""3""><strong>{WebUtility.HtmlEncode(remainingFa)}</strong></td></tr>"
            : $@"
    <tr><th>مبلغ اتاق / سوئیت</th><td><strong>{WebUtility.HtmlEncode(totalFa)}</strong></td><th>مبلغ پرداخت‌شده</th><td><strong>{WebUtility.HtmlEncode(paidFa)}</strong></td></tr>
    <tr><th>مانده حساب</th><td colspan=""3""><strong>{WebUtility.HtmlEncode(remainingFa)}</strong></td></tr>";

        var html = $@"<!DOCTYPE html>
<html lang=""fa"" dir=""rtl"">
<head>
<meta charset=""utf-8"" />
<title>فاکتور اقامت — فراسا ({WebUtility.HtmlEncode(codeFa)})</title>
<style>
  body {{ font-family: Vazirmatn, Tahoma, sans-serif; background: #F7F3E9; color: #16233D; margin: 0; padding: 24px; }}
  .sheet {{ max-width: 720px; margin: 0 auto; background: #fff; border: 2px solid #C9A24B; border-radius: 14px; padding: 28px; }}
  .head {{ display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #E8D6AE; padding-bottom: 14px; margin-bottom: 18px; }}
  .brand {{ font-size: 26px; font-weight: 800; color: #16233D; }}
  .sub {{ font-size: 12px; color: #5A6474; }}
  table {{ width: 100%; border-collapse: collapse; margin: 14px 0; }}
  th, td {{ border: 1px solid #E4D9BE; padding: 10px 12px; text-align: right; font-size: 13px; }}
  th {{ background: #F7F3E9; color: #16233D; }}
  .rules {{ background: #FAF7F0; border: 1px solid #E4D9BE; border-radius: 10px; padding: 12px 16px; font-size: 12px; line-height: 1.9; margin-top: 16px; }}
  .foot {{ margin-top: 18px; padding-top: 12px; border-top: 1px dashed #C9A24B; font-size: 12px; color: #5A6474; display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }}
  @media print {{ body {{ background: #fff; padding: 0; }} .no-print {{ display: none; }} }}
</style>
</head>
<body>
<div class=""sheet"">
  <div class=""head"">
    <div>
      <div class=""brand"">فراسا</div>
      <div class=""sub"">مهمان‌پذیر فرهنگیان اهواز — قبض و فاکتور رسمی اقامت</div>
    </div>
    <div>
      <div><strong>شماره قبض:</strong> {WebUtility.HtmlEncode(codeFa)}</div>
      <div class=""sub"">تاریخ صدور: {WebUtility.HtmlEncode(Fa.DateLong(DateTime.Today))}</div>
    </div>
  </div>
  <table>
    <tr><th>نام مهمان</th><td>{WebUtility.HtmlEncode(guestName)}</td><th>تلفن همراه</th><td>{WebUtility.HtmlEncode(phoneFa)}</td></tr>
    <tr><th>شماره اتاق</th><td>{WebUtility.HtmlEncode(roomFa)}</td><th>تعداد نفرات</th><td>{WebUtility.HtmlEncode(Fa.Num(guestsCount))} نفر</td></tr>
    <tr><th>تاریخ ورود</th><td>{WebUtility.HtmlEncode(checkInDisplay)}</td><th>تاریخ خروج</th><td>{WebUtility.HtmlEncode(checkOutDisplay)}</td></tr>
    <tr><th>تعداد شب‌های اقامت</th><td colspan=""3"">{WebUtility.HtmlEncode(Fa.Num(nights))} شب</td></tr>
    {paymentRows}
  </table>
  {(string.IsNullOrWhiteSpace(notes) ? "" : $"<p><strong>توضیحات:</strong> {WebUtility.HtmlEncode(notes)}</p>")}
  <div class=""rules"">
    <strong>قوانین و مقررات مهمان‌پذیر:</strong><br/>
    ۱. ساعت تحویل اتاق ۱۴:۰۰ و ساعت تخلیه ۱۲:۰۰ ظهر می‌باشد.<br/>
    ۲. ارائهٔ کارت ملی و مدارک شناسایی معتبر هنگام پذیرش الزامی است.<br/>
    ۳. حفظ آرامش و رعایت شئونات در محیط مهمان‌پذیر الزامی است.
  </div>
  <div class=""foot"">
    <div>نشانی: اهواز ، فاز ۵ پادادشهر ، بلوار هاتف ، خیابان ارشاد ۲ ( مرکز آموزش فرهنگیان ناحیه ۳ )</div>
    <div>تلفن تماس: ۰۹۱۶۸۹۱۲۲۸۴ (آقای طاهری)</div>
  </div>
  <div class=""no-print"" style=""margin-top:16px;text-align:left"">
    <button onclick=""window.print()"" style=""background:#B08D3E;color:#fff;border:none;padding:10px 20px;border-radius:8px;cursor:pointer;font-family:inherit"">چاپ فاکتور</button>
  </div>
</div>
</body>
</html>";

        var file = Path.Combine(Path.GetTempPath(), $"farasa-invoice-{Guid.NewGuid():N}.html");
        File.WriteAllText(file, html, System.Text.Encoding.UTF8);
        Process.Start(new ProcessStartInfo(file) { UseShellExecute = true });
    }
}
