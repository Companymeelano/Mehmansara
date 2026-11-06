namespace Mehra.Desktop;

/// <summary>
/// Gregorian &lt;-&gt; Jalali conversion, ported 1:1 from the Android client
/// (app/src/main/java/ir/ahvaz/mehra/core/date/JalaliConverter.kt, jalaali-js algorithm).
/// </summary>
public static class Jalali
{
    private static readonly int[] Breaks = { -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178 };
    private const int UnixEpochJdn = 2440588;

    public static (int y, int m, int d) ToJalali(DateTime gregorian)
    {
        var jdn = (int)(gregorian.Date - new DateTime(1970, 1, 1)).TotalDays + UnixEpochJdn;
        return JdnToJalali(jdn);
    }

    public static DateTime ToGregorian(int jy, int jm, int jd)
    {
        var (gy, gm, gd) = JdnToGregorian(JalaliToJdn(jy, jm, jd));
        return new DateTime(gy, gm, gd);
    }

    private static int JalaliToJdn(int jy, int jm, int jd)
    {
        var r = JalCal(jy, true);
        return GregorianToJdn(jy + 621, 3, r.march) + (jm - 1) * 31 - jm / 7 * (jm - 7) + jd - 1;
    }

    private static (int gy, int gm, int gd) JdnToGregorian(int jdn)
    {
        var j = 4 * jdn + 139361631;
        j += Div(4 * jdn + 183187720, 146097) * 3 * 4 - 3908;
        var i = Div(j % 1461, 4) * 5 + 308;
        var gd = Div(i % 153, 5) + 1;
        var gm = Div(i / 153 % 12, 1) + 1;
        var gy = Div(j / 1461, 1) - 100100 + (8 - gm) / 6;
        return (gy, gm, gd);
    }

    private static int GregorianToJdn(int gy, int gm, int gd)
    {
        var d = Div((gy + Div(gm - 8, 6) + 100100) * 1461, 4)
              + Div(153 * Mod(gm + 9, 12) + 2, 5)
              + gd - 34840408;
        d = d - Div(Div(gy + 100100 + Div(gm - 8, 6), 100) * 3, 4) + 752;
        return d;
    }

    private static (int leap, int gy, int march) JalCal(int jy, bool withoutLeap = false)
    {
        var bl = Breaks.Length;
        var gy = jy + 621;
        var leapJ = -14;
        var jp = Breaks[0];
        int jm = 0, jump = 0, leapG = 0, n = 0, i = 0;
        if (jy < jp || jy >= Breaks[bl - 1])
            throw new ArgumentOutOfRangeException(nameof(jy), "Jalali year out of supported range");
        for (i = 1; i < bl; i++)
        {
            jm = Breaks[i];
            jump = jm - jp;
            if (jy < jm) break;
            leapJ += Div(jump, 33) * 8 + Div(Mod(jump, 33), 4);
            jp = jm;
        }
        n = jy - jp;
        leapJ += Div(n, 33) * 8 + Div(Mod(n, 33) + 3, 4);
        if (Mod(jump, 33) == 4 && jump - n == 4) leapJ++;
        var leapG = Div(gy, 4) - Div((Div(gy, 100) + 1) * 3, 4) - 150;
        var march = 20 + leapJ - leapG;
        if (!withoutLeap)
        {
            if (jump - n < 6) n = n - jump + Div(jump + 4, 33) * 33;
            leapJ = (Mod(Mod(n + 1, 33) - 1, 4) == 0) ? 1 : 0;
            if (leapJ == 0 && Mod(jump, 33) == 4) leapJ = -1;
        }
        var leap = leapJ;
        return (leap, gy, march);
    }

    private static (int y, int m, int d) JdnToJalali(int jdn)
    {
        var (gy, gm, gd) = JdnToGregorian(jdn);
        var jy = gy - 621;
        var r = JalCal(jy);
        var jdn1F = GregorianToJdn(gy, 3, r.march);
        var k = jdn - jdn1F;
        if (k >= 0)
        {
            if (k <= 185) return (jy, 1 + k / 31, k % 31 + 1);
            k -= 186;
        }
        else
        {
            jy--;
            k += 179;
            if (r.leap == 1) k += 1;
        }
        return (jy, 7 + k / 30, k % 30 + 1);
    }

    private static int Div(int a, int b) => a / b; // C# int division truncates toward zero, same as the Kotlin source
    private static int Mod(int a, int b) => ((a % b) + b) % b;
}
