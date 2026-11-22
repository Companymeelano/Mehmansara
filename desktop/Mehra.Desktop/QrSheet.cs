using System.Diagnostics;
using System.Text;

namespace Mehra.Desktop;

/// <summary>
/// Builds the printable luxury guest-QR sheet (A4/A5, RTL, navy+gold) and the
/// public self-service registration page served by the LAN server.
/// The sheet is a self-contained HTML file — print it or «Save as PDF» in the browser.
/// </summary>
public static class QrSheet
{
    private const string Navy = "#16233D";
    private const string Gold = "#C9A24B";
    private const string GoldSoft = "#E8D6AE";
    private const string Ink = "#2A3346";

    private const string Venue = "مهمان‌پذیر فرهنگیان ناحیه ۳ اهواز";

    /// <summary>Full printable sheet (A4 portrait, also prints fine on A5).</summary>
    public static string BuildSheet(string registerUrl, string appVersionFa, string managerName, string managerPhone)
    {
        var qr = QrCode.ToSvg(QrCode.Encode(registerUrl), border: 4, color: Navy);
        var sb = new StringBuilder();
        sb.Append("<!DOCTYPE html><html dir=\"rtl\" lang=\"fa\"><head><meta charset=\"utf-8\"><title>")
          .Append("فراسا — برگهٔ QR ثبت‌نام مهمان")
          .Append("</title><style>@page { size: A4 portrait; margin: 12mm; } * { box-sizing: border-box; } body { margin:0; font-family: Vazirmatn, Tahoma, sans-serif; background:#F7F4EC; color:")
          .Append(Ink).Append("; } .sheet { width: 186mm; min-height: 273mm; margin: 0 auto; background: #fff; border: 2px solid ").Append(Gold)
          .Append("; border-radius: 6mm; overflow: hidden; display: flex; flex-direction: column; } .head { background: linear-gradient(135deg, ").Append(Navy)
          .Append(" 0%, #24365C 100%); color: ").Append(GoldSoft).Append("; padding: 9mm 10mm 7mm; text-align: center; } .head h1 { margin: 0; font-size: 30pt; letter-spacing: 1px; } .head .sub { margin-top: 2mm; font-size: 11pt; color: #C7D0E0; } .qr-card { margin: 8mm auto 0; background: #fff; border: 2.5px solid ")
          .Append(Gold).Append("; border-radius: 5mm; padding: 6mm; box-shadow: 0 4px 18px rgba(22,35,61,.18); width: 74mm; text-align: center; } .qr-card svg { width: 62mm; height: 62mm; display: block; margin: 0 auto; } .qr-card .cap { margin-top: 3mm; font-size: 11pt; font-weight: bold; color: ")
          .Append(Navy).Append("; } .url { direction: ltr; margin-top: 2mm; font-size: 9.5pt; color: #5A6474; word-break: break-all; } .body { padding: 0 10mm; } h2 { font-size: 14pt; color: ").Append(Navy)
          .Append("; border-bottom: 2px solid ").Append(Gold).Append("; padding-bottom: 1.5mm; margin: 6mm 0 3mm; } ul { margin: 0; padding-right: 5mm; font-size: 10.5pt; line-height: 1.9; } .howto { display: flex; gap: 4mm; } .howto .col { flex: 1; background: #FBF8F0; border: 1px solid ")
          .Append(Gold).Append("; border-radius: 3mm; padding: 4mm; font-size: 10pt; line-height: 1.8; } .howto .col b { color: ").Append(Navy)
          .Append("; } .contact { margin: 6mm 10mm 0; background: linear-gradient(135deg, ").Append(Navy).Append(", #24365C); color: ").Append(GoldSoft)
          .Append("; border-radius: 4mm; padding: 5mm 7mm; display: flex; justify-content: space-between; align-items: center; } .contact .name { font-size: 13pt; font-weight: bold; } .contact .phone { direction: ltr; font-size: 15pt; font-weight: bold; color: #fff; letter-spacing: 1px; } .foot { margin-top: auto; padding: 4mm 10mm 6mm; text-align: center; font-size: 8.5pt; color: #8A93A3; } .foot b { color: ")
          .Append(Navy).Append("; } @media print { body { background: #fff; } .sheet { border: none; box-shadow: none; width: auto; min-height: auto; } }</style></head><body><div class=\"sheet\">");

        sb.Append("<div class=\"head\"><h1>فراسا</h1><div class=\"sub\">").Append(Venue)
          .Append("</div><div class=\"sub\">«ثبت درخواست اقامت — کد را اسکن کنید»</div></div>");

        sb.Append("<div class=\"qr-card\">").Append(qr)
          .Append("<div class=\"cap\">کد QR ثبت‌نام مهمان</div><div class=\"url\">").Append(Esc(registerUrl)).Append("</div></div>");

        sb.Append("<div class=\"body\"><h2>")
          .Append("چه اطلاعاتی در فرم ثبت‌نام می‌آید؟")
          .Append("</h2><ul><li>نام و نام خانوادگی</li><li>شمارهٔ تماس (موبایل)</li><li>شهر</li><li>تاریخ ورود و خروج</li><li>تعداد نفرات</li><li>توضیحات (اختیاری)</li></ul>")
          .Append("<h2>چطور کد را اسکن کنیم؟</h2><div class=\"howto\">")
          .Append("<div class=\"col\"><b>گوشی اندروید:</b> اپ «Camera» یا «Google Lens» را باز کنید و روی کد نگه دارید؛ با لمس لینک، صفحهٔ ثبت‌نام در مرورگر باز می‌شود. اگر باز نشد: Chrome → منوی ⋮ → «Lens».</div>")
          .Append("<div class=\"col\"><b>آیفون (iOS):</b> اپ «Camera» را باز کنید و به کد نگاه کنید؛ روی «Scan QR Code» در بالای صفحه بزنید؛ صفحهٔ ثبت‌نام در Safari باز می‌شود.</div>")
          .Append("</div></div>");

        sb.Append("<div class=\"contact\"><div><div class=\"name\">").Append(Esc(managerName))
          .Append("</div><div>مدیر مجموعه — پاسخ‌گوی درخواست‌ها</div></div><div class=\"phone\">").Append(Esc(managerPhone)).Append("</div></div>");

        sb.Append("<div class=\"foot\">سامانهٔ مدیریت مهمان‌پذیر <b>فراسا</b> — نسخهٔ ویندوز ").Append(Esc(appVersionFa))
          .Append(" — این کد را برای ثبت درخواست اقامت در این مجموعه اسکن کنید. طراح و توسعه‌دهنده: میلاد یعقوبی — www.meelano.ir — 09120219593</div>");

        sb.Append("</div></body></html>");
        return sb.ToString();
    }

    /// <summary>Public self-service registration form served at /register (no login needed).</summary>
    public static string BuildRegisterPage(string apiUrl, string appVersionFa)
    {
        var sb = new StringBuilder();
        sb.Append("<!DOCTYPE html><html dir=\"rtl\" lang=\"fa\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>")
          .Append("فراسا — ثبت درخواست اقامت")
          .Append("</title><style>body { margin:0; font-family: Vazirmatn, Tahoma, sans-serif; background: linear-gradient(160deg, #16233D 0%, #24365C 60%, #16233D 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 16px; } .card { background: #fff; border-radius: 18px; border: 3px solid #C9A24B; box-shadow: 0 12px 40px rgba(0,0,0,.35); max-width: 460px; width: 100%; padding: 24px; } h1 { margin: 0 0 4px; color: #16233D; font-size: 24px; text-align: center; } .sub { text-align: center; color: #5A6474; font-size: 13px; margin-bottom: 16px; } label { display: block; font-size: 13px; font-weight: bold; color: #16233D; margin: 10px 0 4px; } input, textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1.5px solid #D8CBA6; border-radius: 10px; font-family: inherit; font-size: 14px; background: #FBF8F0; } input:focus, textarea:focus { outline: 2px solid #C9A24B; } .row { display: flex; gap: 10px; } .row > div { flex: 1; } button { width: 100%; margin-top: 16px; padding: 13px; border: none; border-radius: 12px; background: linear-gradient(135deg, #C9A24B, #E8D6AE); color: #16233D; font-family: inherit; font-size: 16px; font-weight: bold; cursor: pointer; } button:disabled { opacity: .6; } .msg { margin-top: 12px; padding: 10px 12px; border-radius: 10px; font-size: 13.5px; display: none; } .ok { display: block; background: #E7F4EC; color: #2E7D5B; border: 1px solid #2E7D5B; } .err { display: block; background: #FBEAEA; color: #B3261E; border: 1px solid #B3261E; } .code { direction: ltr; font-weight: bold; } .foot { text-align: center; color: #9AA6B8; font-size: 11px; margin-top: 14px; }</style></head><body><div class=\"card\">");
        sb.Append("<h1>ثبت درخواست اقامت</h1><div class=\"sub\">").Append(Venue).Append("</div><form id=\"f\">")
          .Append("<label>نام *</label><input id=\"firstName\" required>")
          .Append("<label>نام خانوادگی *</label><input id=\"lastName\" required>")
          .Append("<label>شمارهٔ موبایل *</label><input id=\"phone\" inputmode=\"tel\" required>")
          .Append("<label>شهر</label><input id=\"city\">")
          .Append("<div class=\"row\"><div><label>تاریخ ورود</label><input id=\"checkIn\" type=\"date\"></div><div><label>تاریخ خروج</label><input id=\"checkOut\" type=\"date\"></div></div>")
          .Append("<label>تعداد نفرات</label><input id=\"guestsCount\" type=\"number\" min=\"1\" value=\"1\">")
          .Append("<label>توضیحات</label><textarea id=\"notes\" rows=\"2\"></textarea>")
          .Append("<button id=\"go\" type=\"submit\">ثبت درخواست</button>")
          .Append("<div id=\"msg\" class=\"msg\"></div></form>")
          .Append("<div class=\"foot\">فراسا — نسخهٔ ویندوز ").Append(Esc(appVersionFa)).Append(" — درخواست شما برای مدیر مجموعه ثبت می‌شود.</div></div>");
        sb.Append("<script>const api='").Append(Esc(apiUrl)).Append("';")
          .Append("document.getElementById('f').addEventListener('submit', async e => { e.preventDefault(); const go = document.getElementById('go'); go.disabled = true; const msg = document.getElementById('msg'); msg.className = 'msg'; try { const r = await fetch(api, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ firstName: f.firstName.value.trim(), lastName: f.lastName.value.trim(), phone: f.phone.value.trim(), city: f.city.value.trim(), checkIn: f.checkIn.value, checkOut: f.checkOut.value, guestsCount: parseInt(f.guestsCount.value || '1'), notes: f.notes.value.trim() }) }); const j = await r.json(); if (j.success) { msg.className = 'msg ok'; msg.innerHTML = 'درخواست شما با کد <span class=\"code\">' + (j.data.code || '') + '</span> ثبت شد؛ به‌زودی با شما تماس گرفته می‌شود.'; e.target.reset(); } else { msg.className = 'msg err'; msg.textContent = (j.error && j.error.message) || 'خطا در ثبت درخواست'; } } catch (err) { msg.className = 'msg err'; msg.textContent = 'اتصال به سرور برقرار نشد — هر دو دستگاه را به یک Wi-Fi وصل کنید و دوباره تلاش کنید.'; } go.disabled = false; });</script>");
        sb.Append("</body></html>");
        return sb.ToString();
    }

    /// <summary>Writes the sheet next to the local data file and opens it in the default browser (print / Save as PDF → A4/A5).</summary>
    public static string WriteAndOpen(string registerUrl, string appVersionFa, string managerName, string managerPhone)
    {
        var dir = Path.GetDirectoryName(LocalStore.FilePath())!;
        var path = Path.Combine(dir, "mehra-qr-sheet.html");
        File.WriteAllText(path, BuildSheet(registerUrl, appVersionFa, managerName, managerPhone), new UTF8Encoding(true));
        try { Process.Start(new ProcessStartInfo(path) { UseShellExecute = true }); } catch { /* user can open the file manually */ }
        return path;
    }

    private static string Esc(string s) => s.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace("\"", "&quot;");
}
