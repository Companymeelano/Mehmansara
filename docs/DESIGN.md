# سیستم طراحی لاکچری MEHRA

From this commit the app ships a cohesive "luxury hospitality" design system. Everything is
driven by the **user-selected theme preset**, and every screen shares it — not just some
colours in some sections.

## 1. Theme presets (7 skins)

`ThemePreset` (`core/ui/theme/ThemePreset.kt`) now offers seven skins, persisted in DataStore
and switchable live from تنظیمات → ظاهر:

| Preset | Fa | Mood |
|---|---|---|
| `golden_executive` | طلایی مدیران | deep navy + champagne gold (light) |
| `obsidian_royal` | ابسیدین سلطنتی | matte black + silver (dark) |
| `platinum_luxury` | پلاتینیوم لوکس | pearl white + matte gold (light) |
| `silver_horizon` | افق نقره‌ای | white + slate blue (light) |
| `sapphire_royal` | یاقوت کبود | sapphire blue + ice white (light) |
| `emerald_crown` | زمرد تاج | emerald + gold on velvet black (dark) |
| `ruby_night` | یاقوت شب | ruby + rose gold on black (dark) |

Each preset defines a full `MehraColors` palette (`core/ui/theme/Color.kt`) including the
luxury tokens:

- `accentStart / accentMid / accentEnd` — 3-stop brand gradient used by **3D icon chips,
  primary buttons, the bottom bar, selected states and gradient headings**.
- `bgTop / bgBottom` — vertical gradient painted **behind the whole app** in `MehraTheme`, so
  the theme is visible in every section; screens sit on it with translucent glass cards.
- `topBarStart / topBarEnd / topBarText` — the gradient app bar.
- `metalLight / metalFace / metalDark / glow` — the metallic ramp for the 3D creator
  signature.

## 2. Global shell

- `MehraTheme` paints the preset backdrop gradient behind everything and forces RTL.
- `MehraScreen` / the nav `Scaffold` are transparent; the **top bar is a gradient** with the
  3D logo chip on root screens and an accent hairline underneath.
- `MehraCard` is a glass card: translucent surface + 1px **gradient border** in accent
  colours + soft shadow + 20dp radius.
- The **bottom navigation** is a floating glass pill with a gradient border; the selected tab
  gets a full 3D gradient icon chip.
- Buttons (`PrimaryButton`…) are 3D gradient tiles with shadow + ripple.

## 3. 3D icons (SVG pipeline)

- SVG masters: `design/icons/svg/*.svg` (see `design/icons/README.md`).
- Vector glyphs: `core/ui/icons/MehraIcons.kt` (mirrors the SVG path data, 24dp viewport).
- Runtime: `Icon3D` puts any glyph on a theme-accent gradient squircle with gloss + shadow —
  used in the bottom bar, module tiles (`بیشتر`), dashboard `StatTile`s, lock screen, etc.
- Launcher & splash: layered two-tone gold mihrab arch over a gradient adaptive-icon
  backdrop (`res/drawable/ic_launcher_*`, `ic_splash_logo.xml`).

## 4. Typography

Vazirmatn (bundled TTFs, 5 weights — no downloadable fonts, works on an isolated hotel LAN)
with a full M3 type scale (`core/ui/theme/Type.kt`); brand headings on auth screens use
`GradientText` with the accent gradient.

## 5. Creator signature — «Milad Yaghoobi»

`CreatorSignature` (`core/ui/components/CreatorSignature.kt`) draws **MILAD YAGHOOBI** on a
Compose `Canvas` as a true 3D metallic extrusion: stacked offset copies for depth, a vertical
metal gradient face taken from the active theme (`metalLight/metalFace/metalDark`), a
theme-coloured glow and a specular highlight on the top half of the letters — followed by
«میلاد یعقوبی — طراح و توسعه‌دهندهٔ سامانهٔ مهرآ». It appears on the **login screen, lock
screen, بیشتر footer and تنظیمات → درباره سامانه**, so the app is visibly and uniquely yours.

## 6. Build

```bash
./gradlew :app:assembleProdDebug :app:assembleDemoDebug
```

CI builds both flavours on every push (`.github/workflows/android-ci.yml`); APKs are
published to the `ci-artifacts` branch.
