using System.Globalization;
using System.Text;

namespace Mehra.Desktop;

/// <summary>Persian presentation helpers: digits, money (Rial), Jalali dates, weekday names.</summary>
public static class Fa
{
    private static readonly string[] Weekdays =
    {
        "\u06CC\u06A9\u0634\u0646\u0628\u0647",
        "\u062F\u0648\u0634\u0646\u0628\u0647",
        "\u0633\u0647\u200c\u0634\u0646\u0628\u0647",
        "\u0686\u0647\u0627\u0631\u0634\u0646\u0628\u0647",
        "\u067E\u0646\u062C\u0634\u0646\u0628\u0647",
        "\u062C\u0645\u0639\u0647",
        "\u0634\u0646\u0628\u0647"
    };

    public static string Digits(string? s)
    {
        if (s is null) return "";
        var sb = new StringBuilder(s.Length);
        foreach (var c in s)
            sb.Append(c is >= '0' and <= '9' ? (char)('\u06F0' + (c - '0')) : c);
        return sb.ToString();
    }

    public static string Num(long n) =>
        Digits(n.ToString("#,##0", CultureInfo.InvariantCulture));

    public static string Num(int n) => Num((long)n);

    public static string Money(long? rial) =>
        rial is null ? "\u2014" : Num(rial.Value) + " \u0631\u06CC\u0627\u0644";

    public static string Date(DateTime gregorian)
    {
        var (y, m, d) = Jalali.ToJalali(gregorian);
        return Digits($"{y}/{m:D2}/{d:D2}");
    }

    public static string DateLong(DateTime gregorian)
    {
        var (y, m, d) = Jalali.ToJalali(gregorian);
        return Digits($"{Weekdays[(int)gregorian.DayOfWeek]} {y}/{m:D2}/{d:D2}");
    }

    /// <summary>Parses an ISO date/datetime coming from the API and renders it as a Jalali date.</summary>
    public static string Date(string? iso) =>
        DateTime.TryParse(iso, CultureInfo.InvariantCulture, DateTimeStyles.None, out var dt)
            ? Date(dt.Date)
            : "\u2014";

    public static string Time(string? isoUtc) =>
        DateTime.TryParse(isoUtc, CultureInfo.InvariantCulture, DateTimeStyles.AdjustToUniversal, out var dt)
            ? Digits($"{dt.Hour:D2}:{dt.Minute:D2}")
            : "\u2014";

    public static string Range(string? fromIso, string? toIso) =>
        $"{Date(fromIso)} \u062A\u0627 {Date(toIso)}";
}
