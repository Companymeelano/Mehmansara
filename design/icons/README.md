# آیکن‌های SVG — منبع طراحی آیکن‌های MEHRA

All MEHRA icons are authored here as **SVG masters** (`svg/*.svg`) with metallic
`linearGradient`/`radialGradient` fills so they read as 3D reliefs. The Android app renders
them through a two-step pipeline so they always match the user-selected theme:

1. **SVG master** (this folder) — the editable design source (open in Figma/Illustrator).
2. **Vector drawable** — the same path data mirrored in
   `app/src/main/java/ir/ahvaz/mehra/core/ui/icons/MehraIcons.kt` (24dp viewport, round
   strokes). When you edit an SVG, mirror the `d` attributes here.
3. **Runtime 3D chip** — `Icon3D` (`core/ui/components/Icon3D.kt`) paints the glyph on a
   rounded tile filled with the **active theme's accent gradient** plus a gloss highlight and
   drop shadow. That is why every icon (bottom bar, module tiles, dashboard stats, buttons)
   automatically follows the selected theme.

The launcher/splash marks live as layered vector XML (`app/src/main/res/drawable/
ic_launcher_foreground.xml`, `ic_splash_logo.xml`) with a gradient adaptive-icon backdrop
(`ic_launcher_bg_gradient.xml`), because `GradientDrawable` gradients render on every API
level (minSdk 24).

The creator badge (`svg/creator-badge.svg`) is the design source for the in-app 3D metallic
signature `MILAD YAGHOOBI` rendered by `CreatorSignature.kt` with the theme's metal ramp.
