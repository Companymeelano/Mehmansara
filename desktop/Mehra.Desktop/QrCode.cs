using System.Text;

namespace Mehra.Desktop;

/// <summary>
/// Minimal, dependency-free QR Code generator (byte mode, ECC level M, versions 1-10).
/// Ported 1:1 from the ISO/IEC 18004 algorithm and verified against OpenCV's
/// QRCodeDetector. Used for the printable guest QR sheet (LanServer /register + /qr).
/// </summary>
public static class QrCode
{
    // version -> (ecCodewordsPerBlock, [(numBlocks, dataCodewordsPerBlock), ...])
    private static readonly Dictionary<int, (int Ec, (int N, int D)[] Blocks)> EccM = new()
    {
        [1] = (10, new[] { (1, 16) }), [2] = (16, new[] { (1, 28) }), [3] = (26, new[] { (1, 44) }),
        [4] = (18, new[] { (2, 32) }), [5] = (24, new[] { (2, 43) }), [6] = (16, new[] { (4, 27) }),
        [7] = (18, new[] { (4, 31) }), [8] = (22, new[] { (2, 38), (2, 39) }),
        [9] = (22, new[] { (3, 36), (2, 37) }), [10] = (26, new[] { (4, 43), (1, 44) }),
    };

    private static readonly Dictionary<int, int[]> Align = new()
    {
        [1] = Array.Empty<int>(), [2] = new[] { 6, 18 }, [3] = new[] { 6, 22 }, [4] = new[] { 6, 26 },
        [5] = new[] { 6, 30 }, [6] = new[] { 6, 34 }, [7] = new[] { 6, 22, 38 }, [8] = new[] { 6, 24, 42 },
        [9] = new[] { 6, 26, 46 }, [10] = new[] { 6, 28, 50 },
    };

    private static readonly int[] Exp = new int[512];
    private static readonly int[] Log = new int[256];

    static QrCode()
    {
        int x = 1;
        for (int i = 0; i < 255; i++)
        {
            Exp[i] = x;
            Log[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) x ^= 0x11D;
        }
        for (int i = 255; i < 512; i++) Exp[i] = Exp[i - 255];
    }

    private static int Gmul(int a, int b) => a == 0 || b == 0 ? 0 : Exp[Log[a] + Log[b]];

    private static int[] RsEcc(int[] data, int degree)
    {
        var g = new int[degree + 1];
        g[0] = 1;
        int len = 1;
        for (int i = 0; i < degree; i++)
        {
            var ng = new int[len + 1];
            for (int j = 0; j < len; j++)
            {
                ng[j] ^= g[j];
                ng[j + 1] ^= Gmul(g[j], Exp[i]);
            }
            g = ng;
            len++;
        }
        var res = new int[degree];
        foreach (var b in data)
        {
            int f = b ^ res[0];
            Array.Copy(res, 1, res, 0, degree - 1);
            res[degree - 1] = 0;
            if (f != 0)
                for (int j = 0; j < degree; j++)
                    res[j] ^= Gmul(g[j + 1], f);
        }
        return res;
    }

    private static int BitLen(int v) { int n = 0; while (v != 0) { n++; v >>= 1; } return n; }

    private static int BchTypeInfo(int data)
    {
        int d = data << 10;
        while (BitLen(d) - BitLen(0x537) >= 0)
            d ^= 0x537 << (BitLen(d) - BitLen(0x537));
        return ((data << 10) | d) ^ 0x5412;
    }

    private static int BchTypeNumber(int data)
    {
        int d = data << 12;
        while (BitLen(d) - BitLen(0x1F25) >= 0)
            d ^= 0x1F25 << (BitLen(d) - BitLen(0x1F25));
        return (data << 12) | d;
    }

    private static bool Mask(int p, int i, int j) => p switch
    {
        0 => (i + j) % 2 == 0,
        1 => i % 2 == 0,
        2 => j % 3 == 0,
        3 => (i + j) % 3 == 0,
        4 => (i / 2 + j / 3) % 2 == 0,
        5 => (i * j) % 2 + (i * j) % 3 == 0,
        6 => ((i * j) % 2 + (i * j) % 3) % 2 == 0,
        _ => ((i * j) % 3 + (i + j) % 2) % 2 == 0,
    };

    private static int LostPoint(bool[][] m)
    {
        int size = m.Length;
        int n = 0;
        for (int i = 0; i < size; i++)
        {
            int run = 1;
            for (int j = 1; j < size; j++)
            {
                if (m[i][j] == m[i][j - 1]) run++;
                else { if (run >= 5) n += 3 + run - 5; run = 1; }
            }
            if (run >= 5) n += 3 + run - 5;
            run = 1;
            for (int j = 1; j < size; j++)
            {
                if (m[j][i] == m[j - 1][i]) run++;
                else { if (run >= 5) n += 3 + run - 5; run = 1; }
            }
            if (run >= 5) n += 3 + run - 5;
        }
        for (int i = 0; i < size - 1; i++)
            for (int j = 0; j < size - 1; j++)
                if (m[i][j] == m[i][j + 1] && m[i][j + 1] == m[i + 1][j] && m[i + 1][j] == m[i + 1][j + 1])
                    n += 3;
        bool[] pat = { true, false, true, true, true, false, true };
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size - 6; j++)
            {
                bool h = true, v = true;
                for (int k = 0; k < 7; k++)
                {
                    if (m[i][j + k] != pat[k]) h = false;
                    if (m[j + k][i] != pat[k]) v = false;
                }
                if (h) n += 40;
                if (v) n += 40;
            }
        int dark = 0;
        foreach (var row in m) foreach (var c in row) if (c) dark++;
        int pct = dark * 100 / (size * size);
        n += Math.Abs(pct - 50) / 5 * 10;
        return n;
    }

    /// <summary>Encodes <paramref name="text"/> (UTF-8 byte mode) and returns the module matrix (true = dark).</summary>
    public static bool[][] Encode(string text)
    {
        var data = Encoding.UTF8.GetBytes(text);
        int ver = 0;
        for (int v = 1; v <= 10; v++)
        {
            int countBits = v <= 9 ? 8 : 16;
            int cap = EccM[v].Blocks.Sum(b => b.N * b.D) * 8;
            if (4 + countBits + data.Length * 8 <= cap) { ver = v; break; }
        }
        if (ver == 0) throw new InvalidOperationException("Payload too large for QR (v1-10, ECC M).");
        int countBits2 = ver <= 9 ? 8 : 16;
        var (ec, blocks) = EccM[ver];
        int totalData = blocks.Sum(b => b.N * b.D);

        var bits = new List<int>(totalData * 8);
        void Put(int v, int n) { for (int i = n - 1; i >= 0; i--) bits.Add((v >> i) & 1); }
        Put(0b0100, 4);
        Put(data.Length, countBits2);
        foreach (var b in data) Put(b, 8);
        Put(0, Math.Min(4, totalData * 8 - bits.Count));
        while (bits.Count % 8 != 0) bits.Add(0);
        int[] pads = { 0xEC, 0x11 };
        int pi = 0;
        while (bits.Count < totalData * 8) { Put(pads[pi % 2], 8); pi++; }
        var cw = new List<int>(totalData);
        for (int i = 0; i < bits.Count; i += 8)
        {
            int v = 0;
            for (int k = 0; k < 8; k++) v = (v << 1) | bits[i + k];
            cw.Add(v);
        }

        var blockList = new List<int[]>();
        int idx = 0;
        foreach (var (n, d) in blocks)
            for (int k = 0; k < n; k++) { blockList.Add(cw.Skip(idx).Take(d).ToArray()); idx += d; }
        var ecc = blockList.Select(b => RsEcc(b, ec)).ToList();
        var final = new List<int>();
        int maxD = blockList.Max(b => b.Length);
        for (int i = 0; i < maxD; i++)
            foreach (var b in blockList)
                if (i < b.Length) final.Add(b[i]);
        int maxE = ecc.Max(b => b.Length);
        for (int i = 0; i < maxE; i++)
            foreach (var b in ecc)
                if (i < b.Length) final.Add(b[i]);

        int size = 17 + 4 * ver;
        var mm = new bool?[size][];
        for (int i = 0; i < size; i++) mm[i] = new bool?[size];

        void Probe(int r0, int c0)
        {
            for (int r = -1; r < 8; r++)
            {
                if (r0 + r < 0 || size <= r0 + r) continue;
                for (int c = -1; c < 8; c++)
                {
                    if (c0 + c < 0 || size <= c0 + c) continue;
                    mm[r0 + r][c0 + c] = (r >= 0 && r <= 6 && (c == 0 || c == 6)) || (c >= 0 && c <= 6 && (r == 0 || r == 6)) || (r >= 2 && r <= 4 && c >= 2 && c <= 4);
                }
            }
        }
        Probe(0, 0);
        Probe(0, size - 7);
        Probe(size - 7, 0);

        foreach (var r in Align[ver])
            foreach (var c in Align[ver])
            {
                if (mm[r][c] is not null) continue;
                for (int dr = -2; dr <= 2; dr++)
                    for (int dc = -2; dc <= 2; dc++)
                        mm[r + dr][c + dc] = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0);
            }

        for (int r = 8; r < size - 8; r++) if (mm[r][6] is null) mm[r][6] = r % 2 == 0;
        for (int c = 8; c < size - 8; c++) if (mm[6][c] is null) mm[6][c] = c % 2 == 0;

        if (ver >= 7)
        {
            int bits18 = BchTypeNumber(ver);
            for (int i = 0; i < 18; i++)
            {
                bool mod = ((bits18 >> i) & 1) == 1;
                int a = size - 11 + i % 3, b = i / 3;
                mm[a][b] = mod;
                mm[b][a] = mod;
            }
        }

        // reserve format info (test pattern first, like the reference)
        void TypeInfo(bool test, int maskPattern)
        {
            int bits15 = BchTypeInfo(maskPattern); // ECC M = 00
            for (int i = 0; i < 15; i++)
            {
                bool mod = !test && ((bits15 >> i) & 1) == 1;
                if (i < 6) mm[i][8] = mod;
                else if (i < 8) mm[i + 1][8] = mod;
                else mm[size - 15 + i][8] = mod;
            }
            for (int i = 0; i < 15; i++)
            {
                bool mod = !test && ((bits15 >> i) & 1) == 1;
                if (i < 8) mm[8][size - i - 1] = mod;
                else if (i < 9) mm[8][15 - i] = mod;
                else mm[8][15 - i - 1] = mod;
            }
            mm[size - 8][8] = !test;
        }
        TypeInfo(true, 0);

        bool?[][] MapData(bool?[][] src, int maskPattern)
        {
            var dst = src.Select(r => r.ToArray()).ToArray();
            int inc = -1, row = size - 1, bitIndex = 7, byteIndex = 0;
            for (int col = size - 1; col > 0; col -= 2)
            {
                int cc = col;
                if (cc <= 6) cc -= 1;
                while (true)
                {
                    foreach (var c in new[] { cc, cc - 1 })
                    {
                        if (dst[row][c] is null)
                        {
                            bool dark = byteIndex < final.Count && ((final[byteIndex] >> bitIndex) & 1) == 1;
                            if (Mask(maskPattern, row, c)) dark = !dark;
                            dst[row][c] = dark;
                            bitIndex--;
                            if (bitIndex == -1) { byteIndex++; bitIndex = 7; }
                        }
                    }
                    row += inc;
                    if (row < 0 || size <= row) { row -= inc; inc = -inc; break; }
                }
            }
            return dst;
        }

        bool?[][] bestMm = null;
        int bestMask = 0, bestPenalty = int.MaxValue;
        for (int mp = 0; mp < 8; mp++)
        {
            var cand = MapData(mm, mp);
            // format info for this mask
            int bits15 = BchTypeInfo(mp);
            for (int i = 0; i < 15; i++)
            {
                bool mod = ((bits15 >> i) & 1) == 1;
                if (i < 6) cand[i][8] = mod;
                else if (i < 8) cand[i + 1][8] = mod;
                else cand[size - 15 + i][8] = mod;
                if (i < 8) cand[8][size - i - 1] = mod;
                else if (i < 9) cand[8][15 - i] = mod;
                else cand[8][15 - i - 1] = mod;
            }
            cand[size - 8][8] = true;
            var solid = cand.Select(r => r.Select(x => x == true).ToArray()).ToArray();
            int p = LostPoint(solid);
            if (p < bestPenalty) { bestPenalty = p; bestMask = mp; bestMm = cand; }
        }
        _ = bestMask;
        return bestMm!.Select(r => r.Select(x => x == true).ToArray()).ToArray();
    }

    /// <summary>SVG (crisp, single color) with quiet zone; perfect for embedding in the printable sheet.</summary>
    public static string ToSvg(bool[][] m, int border = 4, string color = "#16233D")
    {
        int size = m.Length;
        int n = size + 2 * border;
        var sb = new StringBuilder();
        sb.Append($"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 {n} {n}\" shape-rendering=\"crispEdges\">");
        sb.Append($"<rect width=\"{n}\" height=\"{n}\" fill=\"#ffffff\"/>");
        sb.Append($"<g fill=\"{color}\">");
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (m[r][c])
                    sb.Append($"<rect x=\"{c + border}\" y=\"{r + border}\" width=\"1\" height=\"1\"/>");
        sb.Append("</g></svg>");
        return sb.ToString();
    }
}
