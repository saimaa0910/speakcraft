# Design System — SpeakCraft (Compose / Material 3)

| Field | Value |
|---|---|
| Document ID | DES-05 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Design System Architect |
| Approval | Pending — Product Owner (decision **D-18** brand palette) |
| Related | RD-13 Vision, RD-15 Principles (PP-1…8, UX-1…10), RD-16 Scope, DES-01 UX Audit, DES-02 Information Architecture, DES-03 Screen Inventory, DES-06 UI/UX Specifications, DES-07 Accessibility Guidelines, DES-08 Redesign Decisions, ARC-02 Technical Design, ENG-01 DevOps, CONF-033…036 (RD-05) |

> **Everything in this document is a RECOMMENDATION.** It becomes authoritative for visual tokens and components (GOV-02 §4) when the Product Owner approves it. The palette is decision **D-18**.
>
> **Stack constraint (EP-1):** Jetpack Compose with Material 3, as in the current build (Compose BOM `2024.09.00`, so `material3` 1.3.x, plus `material-icons-core` and `material-icons-extended`; `gradle/libs.versions.toml`, `app/build.gradle.kts:110-117`) [VERIFIED]. **No new libraries.** Fonts and icons are proposed as **asset** changes only.

---

## 1. Design principles

| ID | Principle | What it means for tokens and components | Source |
|---|---|---|---|
| DS-1 | **Calm by default** | One accent colour, neutral surfaces, generous spacing, no gradients or pastel decoration. Motion is short and purposeful. | UX-10 |
| DS-2 | **The task is the hero** | Every screen has one primary (filled) action. Decoration never competes with the prompt, the Record button or the feedback. | PP-3, UX-6 |
| DS-3 | **Honest by construction** | Components can't display a number without an evidence label. Empty and Inconclusive states are first-class components, not afterthoughts. | PP-1 |
| DS-4 | **Readable for B1–B2 speakers** | Plain words, comfortable type sizes, sentence case, no monospace or code-like UI text. | UX-1, RD-13 §4 |
| DS-5 | **Accessible without exceptions** | Contrast, 200 % text, 48 dp targets, colour-independent states and reduced motion are built into the components. | UX-9, DES-07 |
| DS-6 | **One component per pattern** | One Record button, one feedback card, one metric tile, one empty-state layout. Variants come from parameters, not copies. | PP-5; DES-01 UXF-12/16/34 |
| DS-7 | **Material 3 first** | Use M3 components and roles as designed. Customise through tokens, not forks. | EP-1 |
| DS-8 | **Works anywhere** | Every component has offline, loading and error variants. Nothing depends on network imagery. | PP-6, UX-7 |

## 2. Token architecture

```
Reference tokens (raw values)       →  System tokens (roles)                  →  Component usage
ScPalette.Indigo40 = #4054A6           colorScheme.primary                       Button container
ScPalette.Neutral98 = #FAF9FD          colorScheme.surface                       Screen background
ScFontWeights / Inter files            typography.titleMedium                    Card title
4 dp scale                             Spacing.lg (16 dp)                        Card padding
M3 easing values                       Motion.standard / Motion.durationMedium2  Expand / collapse
```

**Rule:** UI code may use **system tokens only** (`MaterialTheme.colorScheme.*`, `SpeakCraftTheme.semanticColors.*`, `MaterialTheme.typography.*`, `Spacing.*`, `MaterialTheme.shapes.*`, `Motion.*`). Reference tokens are `internal` to the design-system package (§13).

---

## 3. Colour

### 3.1 Palette direction (decision D-18)

The current "Violet Studio" palette (saturated violet `#6C3DF4`, 8 pastel swatch pairs, hero gradients, sidebar gradients; `Color.kt:1-120`) conflicts with UX-10. It also fails contrast in 13 light-theme pairs (DES-01 UXF-41). Three options were considered:

| Option | Description | Assessment |
|---|---|---|
| **A — Calm Indigo (recommended)** | Desaturated indigo-blue accent (hue ≈ 230°) on cool, lightly tinted neutral greys. Muted plum tertiary used sparingly. Standard semantic hues. | Keeps continuity with the current indigo/violet identity while being far calmer. Indigo is distinct from the success (green), warning (amber), streak (orange) and error (red) hues. **All pairs pass WCAG AA** (§3.4). |
| B — Deep Teal | Teal accent (hue ≈ 180°). | Fresh, but adjacent to success green. Success and info would need re-tuning to avoid semantic confusion. Not computed. |
| C — Keep Violet Studio and fix contrast | Minimum change. | The saturated violet and pastel system contradict "calm and premium". The 13 failing pairs must be fixed anyway. |

The hex values below are hand-tuned approximations of Material 3 tonal palettes (tone 40/80 accents, tone 90/30 containers, tone 98/6 surfaces). **They are the authoritative token values.** Do not regenerate them. Any change must re-run TC-A11Y-001 (DES-07 §15).

### 3.2 Material 3 `ColorScheme`: all roles

| Role | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `#4054A6` | `#B8C3FF` | Filled button, selected controls, links, focus indicator, primary chart series |
| `onPrimary` | `#FFFFFF` | `#1D2D75` | Content on `primary` |
| `primaryContainer` | `#DEE1FF` | `#33438C` | Emphasised containers (Today session card accent, chart target band) |
| `onPrimaryContainer` | `#142166` | `#DEE1FF` | Content on `primaryContainer` |
| `inversePrimary` | `#B8C3FF` | `#4054A6` | Snackbar action |
| `secondary` | `#5A5D72` | `#C3C5DD` | Low-emphasis accents |
| `onSecondary` | `#FFFFFF` | `#2C2F42` | Content on `secondary` |
| `secondaryContainer` | `#DFE1F9` | `#434659` | Navigation indicator, tonal buttons, selected chips and segments |
| `onSecondaryContainer` | `#171B2C` | `#DFE1F9` | Content on `secondaryContainer` |
| `tertiary` | `#74546F` | `#E3BADA` | Reserved: comparison chart series, sparing Words accents |
| `onTertiary` | `#FFFFFF` | `#43273F` | Content on `tertiary` |
| `tertiaryContainer` | `#FFD7F4` | `#5B3D56` | Rare accent containers |
| `onTertiaryContainer` | `#2C1229` | `#FFD7F4` | Content on `tertiaryContainer` |
| `background` | `#FAF9FD` | `#121318` | Same as `surface` |
| `onBackground` | `#1A1B21` | `#E3E1E9` | Same as `onSurface` |
| `surface` | `#FAF9FD` | `#121318` | Screen background, top app bar at rest |
| `onSurface` | `#1A1B21` | `#E3E1E9` | Primary text and icons |
| `surfaceVariant` | `#E2E1EC` | `#45464F` | Legacy M3 role; prefer `surfaceContainer*` |
| `onSurfaceVariant` | `#45464F` | `#C6C5D0` | Secondary text, inactive icons, axis labels |
| `surfaceTint` | `#4054A6` | `#B8C3FF` | Equal to `primary`. Tonal elevation is expressed through container roles (§6.1). |
| `surfaceDim` | `#DAD9DF` | `#121318` | Dimmed surface (rare) |
| `surfaceBright` | `#FAF9FD` | `#38393F` | Bright surface (rare) |
| `surfaceContainerLowest` | `#FFFFFF` | `#0D0E13` | Inputs on tinted backgrounds |
| `surfaceContainerLow` | `#F4F3F8` | `#1A1B21` | Cards, grouped lists, bottom sheets |
| `surfaceContainer` | `#EEEDF3` | `#1E1F25` | Navigation bar and rail, scrolled top app bar, bottom action area |
| `surfaceContainerHigh` | `#E9E7ED` | `#292A2F` | Dialogs, menus |
| `surfaceContainerHighest` | `#E3E2E8` | `#34343A` | Progress tracks, switch tracks, emphasised neutral areas |
| `inverseSurface` | `#2F3036` | `#E3E1E9` | Snackbar container |
| `inverseOnSurface` | `#F1F0F7` | `#2F3036` | Snackbar text |
| `outline` | `#767680` | `#90909A` | **Interactive boundaries** (text fields, outlined buttons, Estimated badge) |
| `outlineVariant` | `#C6C5D0` | `#45464F` | **Decorative** dividers and card outlines only. Never the only boundary of an interactive control. |
| `error` | `#BA1A1A` | `#FFB4AB` | Errors, destructive confirmation |
| `onError` | `#FFFFFF` | `#690005` | Content on `error` |
| `errorContainer` | `#FFDAD6` | `#93000A` | Error banners |
| `onErrorContainer` | `#410002` | `#FFDAD6` | Content on `errorContainer` |
| `scrim` | `#000000` | `#000000` | Modal scrim (alpha applied by M3 components) |

### 3.3 Extended semantic roles (`SemanticColors` via CompositionLocal)

| Role | Light | Dark | Usage rule |
|---|---|---|---|
| `success` | `#2B6A46` | `#96D5A9` | Completion only: step done, saved, word stage passed. **Never** used to judge a metric. |
| `onSuccess` | `#FFFFFF` | `#00391F` | Content on `success` |
| `successContainer` | `#C2EFCF` | `#10512F` | Completion banners |
| `onSuccessContainer` | `#00210F` | `#B2F2C4` | Content on `successContainer` |
| `warning` | `#7A5900` | `#F1C048` | Caution that needs attention (for example low storage, online recognition notice) |
| `onWarning` | `#FFFFFF` | `#402D00` | Content on `warning` |
| `warningContainer` | `#FFDFA0` | `#5C4300` | Warning banners |
| `onWarningContainer` | `#261A00` | `#FFDFA0` | Content on `warningContainer` |
| `info` | `#00658A` | `#7DD0FF` | Neutral notices (offline, sync paused) |
| `onInfo` | `#FFFFFF` | `#00344A` | Content on `info` |
| `infoContainer` | `#C4E7FF` | `#004C69` | Info banners |
| `onInfoContainer` | `#001E2C` | `#C4E7FF` | Content on `infoContainer` |
| `measuredContainer` | `#DEE1FF` | `#2C3B83` | Filled **Measured** badge |
| `onMeasuredContainer` | `#263780` | `#DEE1FF` | Text and icon in the Measured badge |
| `estimatedContent` | `#45464F` | `#C6C5D0` | Text in the **Estimated** badge (transparent container) |
| `estimatedOutline` | `#767680` | `#90909A` | 1 dp outline of the Estimated badge |
| `inconclusiveContainer` | `#ECEAF2` | `#2E2F37` | **Inconclusive** badge and state card: neutral, never alarming |
| `onInconclusiveContainer` | `#3A3B46` | `#D4D3DE` | Content on `inconclusiveContainer` |
| `streak` | `#9C4300` | `#FFB68B` | Streak icon and number on surfaces |
| `streakContainer` | `#FFDBC8` | `#6E3A12` | Practised-day cell fill |
| `onStreakContainer` | `#331200` | `#FFDBC8` | Check icon in a practised cell |
| `recording` | `#B3261E` | `#FFB4AB` | Record button **while recording** only |
| `onRecording` | `#FFFFFF` | `#690005` | Stop icon on the recording button |
| `recordingContainer` | `#F9DEDC` | `#8C1D18` | Recording status chip |
| `onRecordingContainer` | `#410E0B` | `#F9DEDC` | Content on `recordingContainer` |
| `chartPrimary` | = `primary` | = `primary` | Main data series |
| `chartComparison` | = `tertiary` | = `tertiary` | Second series (also differs by marker and dash) |
| `chartTargetBand` | = `primaryContainer` at 60 % alpha | = `primaryContainer` at 60 % alpha | Target range band (always labelled) |
| `chartGrid` | = `outlineVariant` | = `outlineVariant` | Horizontal grid lines |
| `focusIndicator` | = `primary` | = `primary` | 2 dp focus ring on custom components |

### 3.4 WCAG contrast ratios (computed)

**Method:** WCAG 2.x relative luminance (sRGB linearisation, threshold 0.04045). Ratios were computed by script in Stage A from the hex values above at 100 % opacity. Requirements: **4.5:1** for text below 24 sp, **3:1** for large text, UI-component boundaries, focus indicators and meaningful graphics. These ratios become a regression test (DES-07 TC-A11Y-001).

| Foreground / background | Light | Dark | Min | Result |
|---|---|---|---|---|
| `onSurface` / `surface` | 16.39 | 14.32 | 4.5 | Pass |
| `onSurface` / `surfaceContainerLowest` (L) | 17.18 | — | 4.5 | Pass |
| `onSurface` / `surfaceContainerHighest` | 13.34 | 9.55 | 4.5 | Pass |
| `onSurface` / `surfaceDim` (L) · `surfaceBright` (D) | 12.25 | 8.88 | 4.5 | Pass |
| `onSurfaceVariant` / `surface` | 8.94 | 10.86 | 4.5 | Pass |
| `onSurfaceVariant` / `surfaceContainerLow` | 8.48 | 10.06 | 4.5 | Pass |
| `onSurfaceVariant` / `surfaceContainer` | 8.05 | 9.62 | 4.5 | Pass |
| `onSurfaceVariant` / `surfaceContainerHighest` | 7.27 | 7.24 | 4.5 | Pass |
| `onSurfaceVariant` / `surfaceBright` (D) | — | 6.74 | 4.5 | Pass |
| `onPrimary` / `primary` | 6.92 | 7.29 | 4.5 | Pass |
| `primary` / `surface` (text button, link) | 6.60 | 10.85 | 4.5 | Pass |
| `primary` / `surfaceContainerLow` | 6.26 | 10.05 | 4.5 | Pass |
| `onPrimaryContainer` / `primaryContainer` | 11.34 | 7.03 | 4.5 | Pass |
| `onSecondary` / `secondary` | 6.47 | 7.76 | 4.5 | Pass |
| `onSecondaryContainer` / `secondaryContainer` | 13.24 | 7.20 | 4.5 | Pass |
| `onTertiary` / `tertiary` | 6.48 | 7.69 | 4.5 | Pass |
| `onTertiaryContainer` / `tertiaryContainer` | 13.28 | 7.25 | 4.5 | Pass |
| `error` / `surface` | 6.16 | 10.93 | 4.5 | Pass |
| `onError` / `error` | 6.46 | 7.72 | 4.5 | Pass |
| `onErrorContainer` / `errorContainer` | 13.26 | 7.24 | 4.5 | Pass |
| `inverseOnSurface` / `inverseSurface` (snackbar) | 11.61 | 10.15 | 4.5 | Pass |
| `inversePrimary` / `inverseSurface` (snackbar action) | 7.69 | 5.34 | 4.5 | Pass |
| `outline` / `surface` (boundary) | 4.29 | 5.87 | 3.0 | Pass |
| `outline` / `surfaceContainerLow` (field in card) | 4.07 | 5.43 | 3.0 | Pass |
| `outline` / `surfaceContainerLowest` (L) · `surfaceContainerHigh` (D) | 4.49 | 4.53 | 3.0 | Pass |
| `success` / `surface` | 6.16 | 10.94 | 4.5 | Pass |
| `success` / `surfaceContainerLow` | 5.85 | 10.13 | 4.5 | Pass |
| `onSuccess` / `success` | 6.46 | 7.72 | 4.5 | Pass |
| `onSuccessContainer` / `successContainer` | 13.51 | 7.32 | 4.5 | Pass |
| `warning` / `surface` | 6.15 | 10.93 | 4.5 | Pass |
| `onWarning` / `warning` | 6.45 | 7.77 | 4.5 | Pass |
| `onWarningContainer` / `warningContainer` | 13.26 | 7.23 | 4.5 | Pass |
| `info` / `surface` | 6.21 | 10.90 | 4.5 | Pass |
| `onInfo` / `info` | 6.50 | 7.76 | 4.5 | Pass |
| `onInfoContainer` / `infoContainer` | 13.27 | 7.25 | 4.5 | Pass |
| `onMeasuredContainer` / `measuredContainer` | 8.41 | 7.92 | 4.5 | Pass |
| `estimatedContent` / `surface` | 8.94 | 10.86 | 4.5 | Pass |
| `estimatedContent` / `surfaceContainerLow` | 8.48 | 10.06 | 4.5 | Pass |
| `estimatedOutline` / `surface` | 4.29 | 5.87 | 3.0 | Pass |
| `onInconclusiveContainer` / `inconclusiveContainer` | 9.30 | 8.98 | 4.5 | Pass |
| `streak` / `surface` | 6.23 | 10.88 | 4.5 | Pass |
| `streak` / `surfaceContainerLow` | 5.91 | 10.07 | 4.5 | Pass |
| `onStreakContainer` / `streakContainer` | 13.22 | 7.12 | 4.5 | Pass |
| `recording` / `surface` | 6.24 | 10.93 | 4.5 | Pass |
| `onRecording` / `recording` | 6.54 | 7.72 | 4.5 | Pass |
| `onRecordingContainer` / `recordingContainer` | 12.77 | 7.17 | 4.5 | Pass |
| `focusIndicator` / `surfaceContainerLow` | 6.26 | 10.05 | 3.0 | Pass |
| `chartPrimary` / `surface` | 6.60 | 10.85 | 3.0 | Pass |
| `chartComparison` / `surface` | 6.18 | 10.87 | 3.0 | Pass |

**Supplementary fills with no requirement**, because state is also carried by an icon and text: `secondaryContainer` nav indicator on `surfaceContainer` 1.11 (L); `streakContainer` cell on `surface` 1.24 (L); `chartTargetBand` on `surface` 1.23 (L); `outlineVariant` on `surface` 1.63 (L) / 1.98 (D), decorative only.

**Lowest text ratio in the system: 5.34:1** (dark snackbar action). Every text pair exceeds AA, and most main text pairs exceed AAA (7:1).

**Current palette, for comparison** (computed, `Color.kt` and `Theme.kt`): 13 light-theme failures, including `outline` 1.24:1, `warning`/`onWarning` 2.90:1, `success` 3.25:1, `onSecondary`/`secondary` 3.76:1, `error` 4.32:1, and 6 pastel ink pairs at 2.48–3.81:1. Dark `outline` is 1.36:1. Details in DES-01 UXF-41.

### 3.5 Colour usage rules

1. **One accent.** `primary` marks the single primary action, selection and focus. It is not used for decoration or large fills.
2. **Neutral canvas.** Large areas use `surface` and `surfaceContainer*` only. No gradients, pastel tiles, hero overlays or image scrims (the current hero, sidebar and pastel tokens are retired, §13.3).
3. **Semantic colours mean one thing.** `success` = completed. `error` = error or destructive. `recording` = mic is capturing. `streak` = streak. Metric judgements ("within target", "faster than target") use neutral text, never green or red. This avoids grading speakers by colour (PP-7).
4. **Colour is never the only carrier** of meaning (DES-07 A11Y-23). Every coloured state also has text and/or an icon.
5. **On-roles only.** Text on a coloured container uses its matching `on*` role.
6. **No translucent text** except the M3 disabled state (38 %). Ratios in §3.4 assume full opacity.
7. **Dark theme** uses `#121318`, not pure black, and expresses depth through lighter containers.

### 3.6 Dynamic colour (Material You) policy

**Recommendation: do not use dynamic colour.** The current app does not either (`Theme.kt:233-249`) [VERIFIED].

| Reason | Explanation |
|---|---|
| Semantic stability | Measured, Estimated, Inconclusive, success, warning, streak and recording are product signals that must look the same on every device. A wallpaper-derived primary can land on green (confused with success) or orange (confused with streak or warning). |
| Contrast assurance | §3.4 is valid only for a fixed palette. Dynamic schemes are generated per device and cannot be tested exhaustively. |
| Availability | Dynamic colour needs API 31+. With minSdk 24 (D-14 pending) the user base would see two different products. |
| Brand (D-18) | A calm, recognisable identity needs a fixed accent. |
| Harmonisation | Harmonising extended roles with a dynamic primary would need a new colour-utilities dependency, which would require an ADR (EP-1). |

Revisit only through a design decision with automated contrast tests over generated schemes. Even then, extended roles stay fixed.

---

## 4. Typography

### 4.1 Font family (asset change, not a dependency)

**Current state [VERIFIED]:** three **single-weight (400) static TTFs**: Plus Jakarta Sans, DM Sans and JetBrains Mono (`res/font/`). Bold and SemiBold are **synthesised** by the platform (`Type.kt:9-25`). `labelSmall` uses JetBrains Mono, in 103 places. `displayMedium`, `displaySmall` and `headlineSmall` are undefined and fall back to the system font (DES-01 UXF-46).

| Option | Family | Licence | Strengths | Concerns |
|---|---|---|---|---|
| **A (recommended)** | **Inter**, static weights 400 / 500 / 600 | SIL Open Font License 1.1 | Designed for screen UI; very legible at small sizes; tabular figures (`tnum`); broad script coverage useful for R3 localisation (verify the subsets needed) | Neutral character; full-charset files are large, so subset to the required scripts |
| B | Plus Jakarta Sans, static weights 400 / 500 / 600 | SIL OFL 1.1 | Continuity with current headings | More geometric personality at body sizes; verify script coverage |
| C | Source Sans 3 or IBM Plex Sans | SIL OFL 1.1 | Professional, many weights | Less distinctive |

**Recommendation:**
- Use **one family for all roles** (Option A) with **three real weights** (400 Regular, 500 Medium, 600 SemiBold).
- Do not request any weight that is not bundled, so nothing is synthesised.
- Remove DM Sans and JetBrains Mono. **No monospace anywhere in UI labels.**

| Item | Specification |
|---|---|
| Files | `res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` (static instances; subset to Latin and Latin Extended for R1) |
| Why static, not variable | Compose `FontVariation` axis settings require API 26+. minSdk is 24 (D-14 pending). Static instances render identically on every supported API level. If D-14 raises minSdk to ≥ 26, one variable file may replace the three. |
| Licence | Ship the OFL text (for example `assets/licenses/OFL-Inter.txt`) and list it under About → Open-source licences (SCR-SET-08). |
| Size | Measure the APK delta before approval; subsetting is expected to keep it small (ASSUMPTION; to be measured). |

```kotlin
// core/designsystem/theme/Type.kt (sketch)
internal val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)
```

### 4.2 Type scale (all 15 Material 3 roles)

| Role | Size / line height (sp) | Weight | Letter spacing (sp) | Typical use |
|---|---|---|---|---|
| `displayLarge` | 52 / 60 | 400 | −0.5 | Not used on compact. Expanded metric hero only. |
| `displayMedium` | 44 / 52 | 400 | −0.25 | Metric detail value (expanded) |
| `displaySmall` | 36 / 44 | 500 | −0.25 | Key metric value on metric detail and session complete (tabular figures) |
| `headlineLarge` | 30 / 38 | 600 | −0.25 | Onboarding question titles |
| `headlineMedium` | 26 / 34 | 600 | −0.15 | Result headline; session complete title |
| `headlineSmall` | 22 / 30 | 600 | 0 | Dialog titles (M3 default); Today greeting line |
| `titleLarge` | 20 / 28 | 600 | 0 | Top app bar titles; bottom sheet titles |
| `titleMedium` | 16 / 24 | 600 | 0.1 | Card titles; "What went well" / "Try this next" headings |
| `titleSmall` | 14 / 20 | 600 | 0.1 | Section headers; metric labels |
| `bodyLarge` | 16 / 24 | 400 | 0.15 | Default reading text; transcripts; feedback sentences |
| `bodyMedium` | 14 / 20 | 400 | 0.2 | Supporting text; list secondary lines |
| `bodySmall` | 12 / 16 | 400 | 0.3 | Captions, chart axis labels, helper text. Never the sole carrier of essential information. |
| `labelLarge` | 14 / 20 | 500 | 0.1 | Buttons, segmented buttons, tabs |
| `labelMedium` | 12 / 16 | 500 | 0.4 | Chips, badges, navigation labels |
| `labelSmall` | 11 / 16 | 500 | 0.4 | Timestamps (rare). Never for essential information. |

### 4.3 Extended text styles

| Style | Definition | Use |
|---|---|---|
| `promptText` | 20 / 30 sp, 400, 0 sp | Read-aloud passages, questions to answer, sentences to repeat (SCR-SESS-01 prompt state) |
| `metricValue` | `displaySmall` + `fontFeatureSettings = "tnum"` | Metric values that update or are compared |
| `timer` | `titleMedium` + `fontFeatureSettings = "tnum"` | Recording elapsed time "0:23 / 1:30" |
| `targetWordEmphasis` | `SpanStyle(fontWeight = SemiBold, textDecoration = Underline)` | Target words in prompts and transcripts. Weight plus underline, never colour alone. |

### 4.4 Font-scaling rules

| # | Rule |
|---|---|
| FS-1 | All text sizes are in `sp`. Never convert text to `dp` or cancel the user's font scale. |
| FS-2 | Layouts support system font scale up to **200 %** (Android 14+ non-linear scaling), together with the largest display size, with no loss of content or function. |
| FS-3 | **No fixed-height containers hold text.** Use `heightIn(min = …)` or `defaultMinSize`, never `height(…)` or `size(…)`, for anything containing text. (This fixes DES-01 UXF-42.) |
| FS-4 | Essential text (prompts, feedback, errors, button labels, metric values) is never truncated. `maxLines` with ellipsis is allowed only for previews whose full text is one tap away. |
| FS-5 | Buttons grow vertically, and labels may wrap to two lines. Side-by-side buttons stack vertically when they don't fit (`FlowRow` or a width check). |
| FS-6 | Metric tiles reflow: stacked full-width rows on compact width; grid tiles with a minimum width of 160 dp on medium and expanded (§11.13). |
| FS-7 | Navigation labels are single words of at most 8 characters (Today, Practice, Words, Progress) so they fit at 200 %. |
| FS-8 | Icons stay in `dp`. Icons inline with text align to the first line's centre. |
| FS-9 | Verify at font scale 1.0, 1.3, 1.5 and 2.0 on a 360 × 640 dp window (DES-07 TC-A11Y-007, TC-A11Y-023). |

### 4.5 Text rules

- **Sentence case** everywhere (titles, buttons, tabs). No all-caps transforms.
- Start-aligned. Never justified.
- Reading width is at most **640 dp** (about 70 characters at `bodyLarge`).
- Emphasis by weight 600, not colour. Underline only for links and target words.
- Numbers in metrics and timers use tabular figures.

---

## 5. Spacing and layout

### 5.1 Spacing scale (4 dp base)

Existing names `xs`…`xxl` are kept for migration (`Spacing.kt:18-24`) [VERIFIED].

| Token | dp | Typical use |
|---|---|---|
| `none` | 0 | — |
| `xxs` | 2 | Optical adjustment only (icon–text baseline). Avoid. |
| `xs` | 4 | Badge padding (vertical), icon-to-label in chips |
| `sm` | 8 | Gap between related items; inline icon–text gap |
| `md` | 12 | Gap between cards; vertical padding in list items |
| `lg` | 16 | Card padding; compact screen margin |
| `xl` | 24 | Section gap; medium and expanded margins |
| `xxl` | 32 | Large section gap (expanded); empty-state padding |
| `xxxl` | 48 | Top spacing on onboarding and state screens |
| `huge` | 64 | Rare; hero spacing on expanded |
| `minTouchTarget` | 48 | Minimum interactive size (kept) |

### 5.2 Semantic spacing

| Alias | Value | Use |
|---|---|---|
| `screenMargin` | 16 (compact) · 24 (medium, expanded) | Horizontal screen padding |
| `cardPadding` | 16 | Inside cards and sheets |
| `cardGap` | 12 | Between stacked cards |
| `sectionGap` | 24 (compact) · 32 (expanded) | Between screen sections |
| `inlineGap` | 8 | Icon–text, chip–chip, badge–value |
| `buttonGap` | 8 horizontal · 12 vertical stack | Between buttons |
| `bottomActionPadding` | 16 + navigation-bar insets | Sticky bottom action area in session and onboarding |

### 5.3 Layout grid by window class

The window class comes from the existing `AdaptiveWindowTier` and `tierForWidth` (`Spacing.kt:3-16, 31-35`) [VERIFIED]. No window-size-class library is added.

| Class | Width | Columns | Margin | Gutter | Navigation | Layout pattern | Max content width |
|---|---|---|---|---|---|---|---|
| Compact | < 600 dp | 4 | 16 dp | 8 dp | `NavigationBar` (4 items) | Single pane; push to detail | Full width |
| Medium | 600–839 dp | 8 | 24 dp | 16 dp | `NavigationRail` (4 items, labels shown) | Single pane, centred. Track and word lists as 2-column grids. | 640 dp for reading and session content; 840 dp for lists |
| Expanded | ≥ 840 dp | 12 | 24 dp | 24 dp | `NavigationRail` (4 items, labels shown) | List-detail for Practice, Words and Progress history, with a 24 dp spacer between panes. Today in 2 columns (session card + progress summary). | Session player 720 dp centred; detail pane ≥ 360 dp |
| Height-compact (landscape phone, < 480 dp tall) | any | — | 16 dp | — | `NavigationRail` instead of the bar | Session player puts the Record button beside the prompt | — |

The same four destinations appear at every width (DES-02 §2.1). No 10-item sidebar. A permanent drawer is not needed for four items.

### 5.4 Insets, edge-to-edge and keyboard

- Edge-to-edge stays on (`MainActivity.kt:17`). Every screen applies `WindowInsets.safeDrawing` through Scaffold `contentWindowInsets`.
- Sticky bottom action areas pad for navigation-bar insets.
- Screens with text input use `imePadding()` and bring the focused field into view. The current UI has 0 `imePadding` (DES-01 Appendix; UIA §6).
- Content must not be hidden behind sticky bars when a control receives focus (DES-07 A11Y-30).

---

## 6. Elevation, borders and shape

### 6.1 Elevation (tonal)

Depth comes from container colour roles, not shadows.

| Level | Role | Use | Shadow |
|---|---|---|---|
| 0 | `surface` | Screen background; top app bar at rest | None |
| 1 | `surfaceContainerLow` | Cards, grouped lists, bottom sheets | None |
| 2 | `surfaceContainer` | Navigation bar and rail; scrolled top app bar; sticky bottom action area | None |
| 3 | `surfaceContainerHigh` | Dialogs, menus (M3 defaults) | M3 component default only |
| 4 | `surfaceContainerHighest` | Progress and switch tracks; emphasised neutral fills | None |

No elevated cards and no custom shadows. No FAB in R1, because primary actions are inline buttons.

### 6.2 Borders

| Token | Width | Colour | Use |
|---|---|---|---|
| `borderHairline` | 1 dp | `outlineVariant` | Outlined cards, dividers (decorative) |
| `borderControl` | 1 dp | `outline` | Text fields (unfocused), outlined buttons, Estimated badge |
| `borderFocus` | 2 dp, 2 dp offset | `focusIndicator` | Keyboard and switch focus on custom clickable components |
| `borderSelected` | 2 dp | `primary` | Selected card in single-select lists (with a check icon) |
| `borderError` | 2 dp | `error` | Text field error (M3 default) |

### 6.3 Corner radius

The current values are small 14 / medium 20 / large 24 / extraLarge 30, with extraSmall at the default (`Theme.kt:226-231`) [VERIFIED]. New, calmer values:

| Token (`Shapes`) | Radius | Use |
|---|---|---|
| none | 0 | Full-bleed surfaces, navigation bar |
| `extraSmall` | 4 dp | Evidence badges, snackbars, tooltips, text fields (M3 default) |
| `small` | 8 dp | Chips, small containers, chart bar tops |
| `medium` | 12 dp | Cards, metric tiles, prompt card, state cards |
| `large` | 16 dp | Today session card, large feature cards |
| `extraLarge` | 28 dp | Dialogs, bottom sheets (top corners) |
| full | 50 % | Buttons, Record button, avatars, navigation indicator, segmented button ends |

---

## 7. Iconography

| Topic | Specification |
|---|---|
| Visual language | Material Symbols style: **Outlined** by default, **Filled** for selected navigation items. Weight 400, grade 0, optical size 24. |
| Implementation (R1) | Use the icons already available from `material-icons-core` and `material-icons-extended` (Outlined, Filled and AutoMirrored sets). No new dependency. |
| Missing glyphs | Download the Material Symbols SVG (Apache 2.0) and import it as a vector drawable **asset**. |
| Asset strategy | `material-icons-extended` is very large. With R8 disabled (RD-03 FND-SEC-11) it inflates the APK. **Recommendation:** in R0/R1 replace it with about 30 imported vector drawables for the icons actually used (OQ-DS-6; ARC-02). |
| Sizes | 16 dp in badges; 18 dp inside buttons; 20 dp in dense lists; **24 dp standard**; 32 dp in state cards and the Record icon; 48 dp in empty states |
| Touch area | Icon-only controls use `IconButton` (48 dp target). |
| RTL | Directional icons (back, forward, list, help, volume) use `Icons.AutoMirrored.*`. |
| Colour | `onSurfaceVariant` (inactive), `onSurface` (active), or the `on*` role of the container. |

**`contentDescription` rules** (DES-07 A11Y-02/03):

| Case | `contentDescription` |
|---|---|
| Decorative icon, or an icon next to text that says the same thing | `null` |
| Icon-only button | The **action**, e.g. "Open profile and settings", "Play recording", "Close session". Not the icon name. |
| State icon with no visible text | The state, e.g. "Practised". Prefer adding visible text instead. |
| Avatar | "Profile and settings" |

**Core icon map (Material Icons names):**

| Use | Icon |
|---|---|
| Today | `Outlined.Today` / `Filled.Today` |
| Practice | `Outlined.RecordVoiceOver` / `Filled.RecordVoiceOver` |
| Words | `AutoMirrored.Outlined.MenuBook` / `AutoMirrored.Filled.MenuBook` (verify availability in the BOM in use) |
| Progress | `Outlined.Insights` / `Filled.Insights` |
| Record / Stop | `Filled.Mic` / `Filled.Stop` |
| Play / Pause recording | `Filled.PlayArrow` / `Filled.Pause` |
| Listen (reference audio) | `AutoMirrored.Outlined.VolumeUp` |
| Back / Close | `AutoMirrored.Filled.ArrowBack` / `Filled.Close` |
| How we measured this | `Outlined.Info` |
| Measured / Inconclusive | `Outlined.Straighten` / `AutoMirrored.Outlined.HelpOutline` |
| Strength / Focus | `Outlined.CheckCircle` / `Outlined.Flag` |
| Streak | `Outlined.EventAvailable`. Calm, not a flame. |
| Offline / Error / Mic blocked | `Outlined.CloudOff` / `Outlined.ErrorOutline` / `Outlined.MicOff` |
| Accuracy rating | `Outlined.ThumbUp` / `Outlined.ThumbDown`, always with text labels |

---

## 8. Motion

### 8.1 Duration tokens (Material 3 values)

| Token | ms | Token | ms |
|---|---|---|---|
| `short1` | 50 | `medium1` | 250 |
| `short2` | 100 | `medium2` | 300 |
| `short3` | 150 | `medium3` | 350 |
| `short4` | 200 | `medium4` | 400 |
| `long1` | 450 | `long2` | 500 |
| `brandMoment` | ≤ 1000 | Launch only (RD-16 §5) | |

### 8.2 Easing tokens (cubic-bezier, Compose `CubicBezierEasing`)

| Token | Control points | Use |
|---|---|---|
| `standard` | (0.2, 0.0, 0.0, 1.0) | Most in-place changes |
| `standardDecelerate` | (0.0, 0.0, 0.0, 1.0) | Elements entering in place |
| `standardAccelerate` | (0.3, 0.0, 1.0, 1.0) | Elements leaving in place |
| `emphasizedDecelerate` | (0.05, 0.7, 0.1, 1.0) | Screen enter |
| `emphasizedAccelerate` | (0.3, 0.0, 0.8, 0.15) | Screen exit |
| `linear` | — | Real-time level meter; indeterminate progress |

### 8.3 Reduced motion

**Effective setting** = the in-app preference (Appearance → "Reduce motion": *Follow system* (default) / *On* / *Off*). With *Follow system*, it follows `Settings.Global.ANIMATOR_DURATION_SCALE == 0` ("Remove animations").

- Observe the system value **reactively** with a `ContentObserver` on `Settings.Global.getUriFor(ANIMATOR_DURATION_SCALE)`. This fixes the one-time read at `CC:111-124`.
- Persist the preference in the existing DataStore. The key `reduced_motion` already exists but is unused (`PR:39, 119, 247-251`).
- Provide the effective value as `LocalReducedMotion` at the theme root. All design-system animation helpers read it.

| Motion | Default behaviour | Reduced-motion behaviour |
|---|---|---|
| Screen transition (push within a tab) | Fade-through: exit 100 ms `emphasizedAccelerate`, enter 200 ms `emphasizedDecelerate` | Instant switch |
| Tab switch | Crossfade 150 ms | Instant |
| Session state change (prompt → recording → processing → result) | `AnimatedContent` fade 200 ms plus size 250 ms `standard` | Instant |
| Expand / collapse (transcript, "All measurements") | `expandVertically` + fade, 250 ms `standard` | Instant |
| Selection, chip or button state change | 150 ms `standard` | Instant |
| Determinate progress value change | 300 ms `standard` | Jump to value |
| Recording level meter | Real-time bars, ≤ 30 fps, `linear` | Single static level bar updated ≤ 4 Hz; no scrolling |
| Recording indicator dot | Solid (no pulse) | Solid |
| Countdown digits (optional) | 150 ms fade | Instant swap |
| Milestone celebration | Once, ≤ 800 ms (scale 0.9 → 1 + fade) | Static icon and text |
| Skeleton placeholders | **Static in all modes** (no shimmer) | Static |
| Brand moment at launch | Platform splash, ≤ 1000 ms | Platform default; no extra animation |
| M3 internal animations (sheets, dialogs, ripples) | M3 default | M3 default (short, user-initiated); not customised |

### 8.4 Motion rules

1. In-app motion is at most 400 ms, except the launch brand moment (≤ 1000 ms) and a milestone (≤ 800 ms).
2. No looping animation except the live level meter and indeterminate progress.
3. No parallax, bounce or overshoot. Nothing flashes more than 3 times per second.
4. Motion never blocks input. The user can act during a transition.
5. Animation is never used to fake progress.

---

## 9. Haptics

Use `LocalView.current.performHapticFeedback(...)`, a platform API that respects the system "touch feedback" setting. `LocalHapticFeedback` in the Compose version in use exposes only a small set of types (INFERRED from the Compose 1.7 API; verify in Stage B).

| Event | Constant | Fallback below API 30 |
|---|---|---|
| Recording started | `HapticFeedbackConstants.CONFIRM` (API 30+) | `VIRTUAL_KEY` |
| Recording stopped | `CONFIRM` | `VIRTUAL_KEY` |
| Permission denied, error or Inconclusive result | `REJECT` (API 30+) | None |
| Milestone reached | `CONFIRM` (once) | None |
| Scrolling, navigation, toggles, countdown ticks | None | None |

Rules:
- Never use continuous vibration.
- Haptics always accompany a visible or audible change; they are never the only signal.
- An in-app toggle ("Vibration feedback", default on) is an open question (OQ-DS-5).

---

## 10. Theme behaviour (dark, light, system)

| Topic | Specification |
|---|---|
| Modes | **System** (default) / **Light** / **Dark**, persisted in DataStore. The current `ThemePreference` is kept (`NAV:221-225`; `AS:537-544`) [VERIFIED]. Set in Appearance (SCR-SET-05). |
| Effective theme | Drives `MaterialTheme` colours, `SemanticColors`, system bar icon style and window background together. |
| System bar icons | Edge-to-edge with transparent bars. Bar style follows the **effective app theme**, not only the system night mode. Call `enableEdgeToEdge(statusBarStyle = …, navigationBarStyle = …)` with `SystemBarStyle.light(…)` or `SystemBarStyle.dark(…)` (activity-compose 1.10.1) whenever the effective theme changes. This fixes dark icons on dark bars when the app is forced dark on a light system (DES-01 UXF-48). |
| Window background and splash | `res/values/colors.xml` `speakcraft_window_bg` = `#FAF9FD`; `values-night` = `#121318` (the `surface` values). This removes the splash-to-Compose colour jump (CONF-035). The splash icon background = `primaryContainer` per mode. Remove the unused `purple_*` / `teal_*` template colours. |
| Forced theme at cold start | When the user selects Light or Dark in-app, also call `UiModeManager.setApplicationNightMode(…)` (API 31+, platform API) so the system splash uses matching resources. Below API 31 a brief mismatch at cold start is accepted. |
| Brand moment | Platform splash only (`windowSplashScreen*` attributes already present in `values-v31/themes.xml`). No overlay composable and no extra library. Respects reduced motion. |
| Imagery | R1 uses no photographic imagery. Any vector illustration uses theme tokens so it adapts to both modes. |
| Testing | Every screen is reviewed in both themes (DES-01 §5; DES-07 TC-A11Y-024). |

---

## 11. Components

General rules for all components:
- Built on M3 composables.
- Expose `modifier` as the first optional parameter.
- Provide correct semantics by default.
- Include Light/Dark and 200 % font `@Preview`s (`@PreviewLightDark` and `@PreviewFontScale` are available in the ui-tooling-preview version in use; verify).

### 11.1 Buttons

| Type | Composable | Emphasis | Use | Per screen |
|---|---|---|---|---|
| Filled | `Button` | Highest | **The** primary action (Start session, Try again, Continue) | Exactly 1 |
| Tonal | `FilledTonalButton` | Medium | Secondary action next to the primary (Listen again, Skip this step) | ≤ 2 |
| Outlined | `OutlinedButton` | Medium-low | Alternatives (Choose another prompt) | ≤ 2 |
| Text | `TextButton` | Low | Tertiary actions, dialog actions, "How we measured this" | As needed |
| Destructive | `TextButton` with `error` content in a confirmation dialog; `Button` with `error` container only on the final confirmation of Delete account | — | Delete history, delete account | — |
| Icon | `IconButton` / `FilledTonalIconButton` | — | Play, close, info | — |

| Size | Min height | Horizontal padding | Icon | Label style | Width rule |
|---|---|---|---|---|---|
| Standard | **48 dp** (grows with text) | 24 dp (16 dp on the icon side) | 18 dp, 8 dp gap | `labelLarge` | Wrap content |
| Large (bottom CTA) | **56 dp** | 24 dp | 24 dp | `titleMedium` | Full width on compact (minus margins); max 480 dp centred on wider windows |
| Dense (dialogs, toolbars) | 40 dp visual, 48 dp touch | 12 dp | — | `labelLarge` | Wrap content |

**States.**
- Enabled, pressed, focused and disabled use M3 state layers (disabled: 38 % content, 12 % container).
- Focused custom buttons also show `borderFocus`.
- **Loading:** the label is replaced by a 20 dp `CircularProgressIndicator` (2 dp stroke) and the button keeps its width. It is not clickable and its semantics state description is "Loading". This prevents double submission (DES-01 UXF-39).

**Labels.** Verb first, sentence case, ideally 3 words or fewer ("Start session", "Try again", "Allow microphone"). Never "OK" as a primary action. Never "AI".

### 11.2 Text inputs

| Aspect | Specification |
|---|---|
| Component | `OutlinedTextField` (boundary `outline`, 4.29:1) |
| Label | Always visible as a floating label. Never placeholder-only. |
| Supporting text | Hint or character count below (for example "Up to 100 characters" for display name) |
| Error | `isError = true`, error icon, and text that says how to fix it ("Enter a name, or skip"). Announced (DES-07 A11Y-32). |
| Size | Min height 56 dp. Multi-line fields grow and are never fixed-height. |
| Keyboard | Correct `KeyboardOptions` (capitalisation, `ImeAction.Next` / `Done`). `imePadding()`; the focused field scrolls into view. |
| Scope | Typed input is minimal by design (PP-4). R1 uses it for display name and optional custom goal text only. |

### 11.3 Cards

| Variant | Composable | Container | Border | Use |
|---|---|---|---|---|
| Filled (default) | `Card` | `surfaceContainerLow` | — | Content groups, activity cards, Today session card |
| Outlined | `OutlinedCard` | `surface` | `borderHairline` | Cards on a `surfaceContainerLow` background |
| Elevated | — | — | — | **Not used** |

- **Padding** 16 dp. **Radius** `medium` (12 dp); Today session card `large` (16 dp).
- **Content order:** title (`titleMedium`), supporting text (`bodyMedium`, `onSurfaceVariant`), meta (`labelMedium`), optional action.
- **Clickable cards** use `Card(onClick = …)`, so the whole card is the target. The card has one destination that matches its label (DES-01 UXF-10).
- **No nested tap targets**, except one trailing `IconButton` with its own label (for example "Save activity").
- The card reads as one merged phrase (DES-07 A11Y-07).

### 11.4 List items

| Aspect | Specification |
|---|---|
| Component | M3 `ListItem` |
| Min heights | One-line 56 dp, two-line 72 dp, three-line 88 dp. These are minimums; items grow with font scale. |
| Leading | 24 dp icon or 40 dp avatar |
| Trailing | Chevron for navigation; `Switch` for settings (whole row toggleable, §11.22); meta text (`labelMedium`) |
| Dividers | None by default (use gaps or group lists in cards). Inset dividers (`borderHairline`) only in dense settings lists. |

### 11.5 Dialogs

| Aspect | Specification |
|---|---|
| Component | `AlertDialog` (platform window, so focus is contained) |
| When | Destructive confirmations (delete attempt, clear history, delete account), leaving a session with an unsaved recording, irreversible choices. **Not** for information, tips, updates or permission explanations (use SCR-PERM-01). |
| Content | Title (`headlineSmall`) phrased as a question ("Delete this recording?"). Body (`bodyMedium`) of at most 3 sentences stating the consequence. |
| Actions | Up to 2 `TextButton`s: dismiss (start side), confirm (end side) with a specific verb ("Delete", "Leave"). The destructive confirm uses `error` content. |
| Dismissal | Back and outside tap always dismiss. The only exception is while an irreversible operation is in progress, which shows progress inside the dialog. |
| Prohibited | Dialogs at app launch, stacked dialogs, non-dismissable dialogs (DES-01 UXF-11) |
| Width | 280–560 dp |

### 11.6 Bottom sheets

| Aspect | Specification |
|---|---|
| Component | `ModalBottomSheet` (separate window; focus contained) |
| Use | "How we measured this" (SCR-SESS-02), metric explanations, choose a prompt, filters, recording tips |
| Anatomy | Drag handle; title (`titleLarge`); scrollable content; explicit **Close** `IconButton` in the header, so the sheet is not swipe-dependent (switch and keyboard users) |
| Shape and colour | Top corners `extraLarge` (28 dp); container `surfaceContainerLow` (M3 default) |
| Width | Max 640 dp, centred on medium and expanded |
| Behaviour | `skipPartiallyExpanded = true` for short sheets. Back dismisses. Focus returns to the invoking control. |

### 11.7 Navigation bar and rail

| Aspect | `NavigationBar` (compact) | `NavigationRail` (medium, expanded, height-compact) |
|---|---|---|
| Items | Today · Practice · Words · Progress (DES-02 §2.1) | Same |
| Icons | Outlined (unselected), Filled (selected), 24 dp (§7) | Same |
| Labels | Always shown, `labelMedium` | Always shown |
| Colours | Container `surfaceContainer`; indicator `secondaryContainer`; selected icon `onSecondaryContainer`; selected label `onSurface`; unselected `onSurfaceVariant` (M3 defaults) | Same |
| Size | 80 dp tall | 80 dp wide; items top-aligned; no FAB or header |
| Selected state | Indicator + Filled icon + `selected` semantics (not colour alone) | Same |
| Visibility | Hidden in full-screen flows (session player, onboarding, sign-in, permission education) | Same |
| Behaviour | Each tab keeps its own back stack (`saveState` / `restoreState`). Re-selecting the current tab pops to its root and scrolls to top. The tab stays highlighted on that tab's secondary screens (fixes DES-01 UXF-08). | Same |
| Badges | None in R1 (OQ-DS-4) | None |

### 11.8 Top app bars

| Context | Component | Content |
|---|---|---|
| Primary destinations | `TopAppBar` (small, 64 dp) with `pinnedScrollBehavior` (container `surface` → `surfaceContainer` on scroll) | Title (`titleLarge`): "Today", "Practice", "Words", "Progress". Trailing 40 dp initials avatar in a 48 dp `IconButton`, labelled "Profile and settings" (DES-02 §2.2). R2: bell before the avatar. |
| Detail screens | `TopAppBar` | Navigation icon `AutoMirrored.Filled.ArrowBack` ("Back"), title, at most one trailing action |
| Full-screen flows | `TopAppBar` | `Filled.Close` ("Close session"), step progress (§11.11). Confirm on close only if a recording would be lost. |
| Prohibited | — | Brand lockups, empty titles (current sidebar case), large or collapsing bars, more than 2 trailing actions |

### 11.9 Chips and segmented buttons

| Component | Use | Notes |
|---|---|---|
| `FilterChip` | Multi-select filters (Practice track filters, Words filters) | Check icon when selected. Selected state is announced. |
| `SingleChoiceSegmentedButtonRow` | Exclusive choice of up to 4 options (Progress period "Week / Month / All") | Radio semantics. Selected segment shows a check icon. |
| `AssistChip` | Contextual suggestions ("Tips for a clear recording") | Opens a sheet or performs a light action |
| Prohibited | Chips used as primary action buttons (current CM:724-726) | Use buttons |
| Size | 32 dp visual, 48 dp touch (M3 minimum interactive size kept on) | `labelMedium` (chips), `labelLarge` (segments) |

### 11.10 Snackbars

| Aspect | Specification |
|---|---|
| Use | Confirmations ("Recording deleted", with Undo) and background notices ("Couldn't sync. We'll try again later.") |
| Not for | Errors that block the current task (use an inline error state) or anything needing a decision (use a dialog) |
| Position | Above the navigation bar or sticky bottom action area. One at a time. |
| Actions | At most 1, a short verb. Colours `inverseSurface` / `inverseOnSurface` / `inversePrimary`. |
| Duration | Short for info. Long when there is an action. Never `Indefinite` unless the user must act. Keep the M3 `SnackbarHost`, which adapts duration to the system accessibility timeout (verify, DES-07 TC-A11Y-020). |
| Announcement | Announced politely to TalkBack (verify in TC-A11Y-020) |

### 11.11 Progress indicators

| Pattern | Component | Specification |
|---|---|---|
| Session step progress | Determinate segmented bar: a `Row` of segments with `progressBarRangeInfo` semantics | Always paired with text "Step 2 of 4" (`labelLarge`). Segments 4 dp tall, 4 dp gap, `primary` (done and current) / `surfaceContainerHighest` (to do). |
| Word stage progress | Stepper (§11.21) | Text labels for each stage |
| Offline speech pack download (CAP-12) | `LinearProgressIndicator` (determinate) | Shows a percentage and size remaining in text |
| Processing after recording | `CircularProgressIndicator` (indeterminate) + status text | "Checking your recording…". At **10 s**: "This is taking longer than usual" + Cancel. Never indefinite (DES-01 UXF-50). |
| Content loading | Static skeleton matching the layout | Shown only after 300 ms (avoids flicker) |
| Button loading | §11.1 | — |
| Prohibited | Progress that moves without real progress; spinners with no text after 1 s | — |

### 11.12 Record button and recording panel

The single recording control used by the session player (SCR-SESS-01) everywhere. It replaces the current 12 start labels and 7 duplicated permission-checked buttons (DES-01 UXF-16, UXF-36).

**Anatomy (top to bottom):** status line (`bodyMedium`) · level meter · Record button · button label (`labelLarge`) · timer (`timer` style, "0:23 / 1:30").

| Property | Specification |
|---|---|
| Size | 80 dp circular container (64 dp in height-compact); 32 dp icon. The touch target equals the container. |
| Shape | Full (circle) |
| Interaction | **Single tap to start, single tap to stop.** No press-and-hold. Taps are debounced for 500 ms. Hardware keyboard Enter or Space toggles when focused. |
| Auto-stop | At the template's maximum duration, with a warning 10 s before ("10 seconds left"). Silence auto-stop is per FS-04. |
| Haptics | Start and stop (§9) |

| State | Visual | Label · status | Semantics (DES-07 A11Y-06/08/26) |
|---|---|---|---|
| Ready | `primary` fill, `Filled.Mic` | "Record" · "Tap to start when you're ready" | Description "Start recording" |
| Countdown (optional, OQ-DS-8) | Button shows `Filled.Close`; digits 3–2–1 in `headlineLarge` above | "Cancel" · "Get ready" | Countdown announced **before** capture starts |
| Recording | `recording` fill, `Filled.Stop` on `onRecording`; solid 8 dp dot beside the timer | "Stop" · "Recording" | Description "Stop recording"; state "Recording, 23 seconds" (on focus; not announced continuously) |
| Near limit | As Recording | Status "10 seconds left" | Announced once |
| Processing | Button replaced by a 48 dp `CircularProgressIndicator` | "Checking your recording…" | Polite live region |
| Permission needed | Button replaced by a tonal button "Allow microphone" and an explanation | §11.19 Permission | — |
| Mic unavailable (busy, in a call) | Disabled button, `Outlined.MicOff` | "The microphone is being used by another app" + "Try again" | Announced |
| Interrupted | Session shows the interrupted state (DES-02 SCR-SESS-01) | "Recording stopped because of a call" + "Record again" | Announced |

**Level meter (waveform).**
- **Size:** 48 dp tall (it holds no text). Bars are 3 dp wide with 3 dp gaps and rounded caps.
- **Colour:** `onSurfaceVariant` when idle, `recording` while capturing.
- **Data:** the real input level (RMS) at ≤ 30 fps. **Never animate without real input.** If input is near zero for 3 s, the status reads "We can't hear anything. Check your microphone."
- **Reduced motion:** a single horizontal level bar updated at ≤ 4 Hz.
- **Accessibility:** decorative (`clearAndSetSemantics {}`); state is conveyed by the status text and timer.

### 11.13 Metric tile

Shows one measured or estimated value from an attempt or a trend.

| Part | Specification |
|---|---|
| Label | `titleSmall`, e.g. "Pace" |
| Value + unit | `metricValue` at `headlineSmall` size inside tiles (`displaySmall` on metric detail); unit in `bodyMedium`, e.g. "128" "words/min" |
| Evidence badge | **Required**: Measured or Estimated (§11.14). An Inconclusive tile replaces the value with "—" and the text "Not enough speech to measure". |
| Target (optional) | `bodyMedium` `onSurfaceVariant`: "Target 120–160", plus a status in words ("Within target", "Faster than target"). Neutral colour, no green or red. |
| Delta (optional) | Only against an attempt at the **same prompt or template**: "12 fewer than last time". Text is required; a decorative arrow icon is optional. |
| Info | Trailing `IconButton` `Outlined.Info` ("How we measured pace") → sheet (SCR-SESS-02) |
| Layout | Compact: full-width row (label and badge on the start side, value on the end side), wrapping to two lines at large font. Medium and expanded: grid tile, minimum width 160 dp. At most **3 tiles** on a result; more under "All measurements". |
| Container | `surfaceContainerLow`, radius `medium`, padding 16 dp, `heightIn(min = 72.dp)`. No fixed height. |
| Semantics | Merged into one phrase: "Pace, 128 words per minute, measured. Target 120 to 160, within target." Units are spelled out (DES-07 A11Y-34). The info button stays separately focusable. |
| Prohibited | Fallback or default values, values without a badge, composite "overall" scores for free speaking (D-06) |

### 11.14 Evidence badge

| Variant | Container | Content | Icon (16 dp) | Text |
|---|---|---|---|---|
| **Measured** | `measuredContainer` (filled) | `onMeasuredContainer` | `Outlined.Straighten` | "Measured" |
| **Estimated** | Transparent, 1 dp `estimatedOutline` | `estimatedContent` | None (the outline is the visual difference) | "Estimated" |
| **Inconclusive** | `inconclusiveContainer` (filled) | `onInconclusiveContainer` | `AutoMirrored.Outlined.HelpOutline` | "Inconclusive" |

- **Size:** height 24 dp (grows with font), horizontal padding 8 dp, `labelMedium`, radius `extraSmall` (4 dp), so it is distinguishable from interactive chips.
- **Interaction:** non-interactive (the tile's info button explains it).
- **Rule:** the **text is mandatory**; shape and colour are supplementary (DES-07 A11Y-23).

### 11.15 Feedback card (attempt result)

One component for every template (it replaces CC:1326-1842 and ETR:645-787; DES-01 UXF-34).

| Order | Section | Specification |
|---|---|---|
| 1 | Header | `headlineMedium`: "Here's how it went". For an evidence failure, the Inconclusive card (§11.16) is shown **instead** of sections 2–4. |
| 2 | Strength | `Outlined.CheckCircle` (primary) + `titleMedium` "What went well" + **one** sentence in `bodyLarge` |
| 3 | Focus | `Outlined.Flag` + `titleMedium` "Try this next" + **one** actionable sentence |
| 4 | Metrics | At most 3 metric tiles relevant to the template (ARC-05). The rest sit under the expandable "All measurements". Read aloud and Listen & repeat show "Recognised as intended: 82 % (pass mark 75 %)" with a Measured badge. A below-threshold result reads **"Not yet. Try again."** in neutral styling, never red "Fail". |
| 5 | Transcript | Collapsible; first 3 lines shown, then "Show full transcript". Heading "What we heard". Target words use `targetWordEmphasis`. A play/pause control for the local recording sits here. If recognition was unavailable, it says so plainly (CAP-12). |
| 6 | Accuracy rating | "Was this accurate?" with two outlined buttons, "Yes" and "No" (icons plus text). "No" offers optional reason chips. Stored per CAP-05. |
| 7 | Actions | One filled button (per FS-04: "Continue" inside Today, "Try again" in Practice), plus one tonal or text alternative |

Semantics: section titles are headings. When the result appears, focus moves to the header and it is announced (DES-07 A11Y-09).

### 11.16 Inconclusive state card

| Part | Specification |
|---|---|
| Container | `inconclusiveContainer`, radius `medium`, padding 16 dp |
| Icon | `AutoMirrored.Outlined.HelpOutline`, 32 dp, `onInconclusiveContainer` |
| Title | `titleMedium`: "We couldn't measure this attempt" |
| Reason (specific) | `bodyMedium`, mapped from the evidence-gate reason (ARC-05). Examples: "We heard less than 3 seconds of speech." · "It was too quiet to hear clearly." · "No words were recognised." · "Speech recognition isn't available offline on this phone." · "Recording stopped because of a call." |
| Reassurance | "This attempt doesn't count against you." Exact streak rule per SRS-BR and DES-07 OQ-A11Y-1. |
| Actions | Filled "Try again"; text button "Tips for a clear recording" (sheet) |
| Rules | Never shows partial metrics as if complete; may state measured facts ("We heard 3 seconds of speech"). No red, no "fail". Announced on appearance (polite live region). |

This preserves the fail-closed evidence gate (RD-03 FND-AI-17; DES-01 §6 K-6).

### 11.17 Streak calendar

| Variant | Where | Specification |
|---|---|---|
| Week strip | Today, Progress | 7 day columns (48 dp wide), day initial above (`labelMedium`; locale first day of week via `WeekFields.of(locale)`), 40 dp cell |
| Month grid | Progress | 7 × n grid of 40 dp cells. Month navigation `IconButton`s ("Previous month", "Next month"). |

| Cell state | Visual | Text equivalent |
|---|---|---|
| Practised | `streakContainer` fill + `Filled.Check` (`onStreakContainer`) | "practised" |
| Today, not yet | 2 dp `primary` ring + day number | "today, not practised yet" |
| Today, done | Practised + ring | "today, practised" |
| Missed | No fill, day number in `onSurfaceVariant`. **No red, no cross.** | "not practised" |
| Future | Day number at 38 % | "upcoming" |

- **Counter:** `titleSmall` "4-day streak" + `bodyMedium` "Best: 9 days" (`Outlined.EventAvailable` in `streak` colour). A new user sees **"Start your streak today"**, never a seeded 1 (DES-01 UXF-57).
- **Semantics:** the week strip is one merged node with a summary: "This week: practised Monday, Tuesday and Thursday. Today not practised yet." Month cells are traversable text.
- **Interaction:** non-interactive in R1.
- The streak definition is authoritative in SRS-BR (one valid attempt per local day; PP-7).

### 11.18 Charts (Compose `Canvas`, no library)

| Type | Use |
|---|---|
| Line chart | One metric over attempts or days (SCR-PROG-02) |
| Bar chart | Practice time per day or week, measured from recordings (CAP-08) |

| Rule | Specification |
|---|---|
| CH-1 Real data only | Plot only stored values. **Never** fallback, default, interpolated or invented points (DES-01 UXF-56). |
| CH-2 Sparse data | Fewer than 2 points: no line. Show the single value as a metric tile + "Practise again to see a trend." |
| CH-3 Gaps | Missing days are gaps: a broken line or points only. Do not connect across gaps. |
| CH-4 Axes | Y axis labelled with units. Bars start at 0. Rate metrics may use an offset baseline, labelled. 3–5 horizontal grid lines (`chartGrid`, 1 dp). Axis labels `bodySmall` `onSurfaceVariant`. |
| CH-5 Target band | `chartTargetBand` fill with a text label ("Target 120–160") |
| CH-6 Series | Maximum 2. Primary = `chartPrimary`, solid, circle markers. Comparison = `chartComparison`, dashed, square markers. **Estimated** metrics are drawn dashed and labelled "Estimated". |
| CH-7 Marks | Line 2 dp; markers 6 dp (8 dp selected); bars max 24 dp wide (60 % of slot) with 4 dp top radius |
| CH-8 Size | Height `max(160.dp, measured axis text + plot)` so axis labels scale with font. Not a fixed height. |
| CH-9 Interaction | Tapping a point shows its value and date in a label. The same data is available in a "View as list" toggle. |
| CH-10 Accessibility | `contentDescription` summary, e.g. "Pace over your last 8 attempts: from 142 to 128 words per minute; 6 of 8 within target." Plus the list alternative (DES-07 A11Y-12, A11Y-23). |
| CH-11 Motion | No draw-in animation under reduced motion; otherwise at most 300 ms. |

### 11.19 State patterns

Every screen defines all applicable states (DES-03 state matrix; UX-7).

| Pattern | When | Layout | Copy pattern (example) | Actions | Accessibility |
|---|---|---|---|---|---|
| **Empty** | No data yet (first run, filters with no results) | 48 dp icon (`onSurfaceVariant`), title `titleMedium`, body `bodyMedium`, one action; vertically centred in the content area | "Your progress will appear here" / "Finish your first session to see your pace, pauses and more." | "Start today's session" | Title is a heading |
| **Loading** | Data not yet available | Nothing for the first 300 ms, then a static skeleton matching the layout. Spinners only in buttons and processing. | — | — | Container description "Loading"; polite announcement once |
| **Error** | Load or save failed | Inline in place of the content: `Outlined.ErrorOutline`, title, plain-language cause, action. **Keeps any user input.** Never raw exception text; technical details go to logs only. | "Couldn't load your history" / "Something went wrong on this phone. Your recordings are safe." | "Try again" | Announced (assertive only for blocking errors) |
| **Offline** | Network unavailable. Core practice works offline (PP-6). | Non-blocking `infoContainer` row, shown **only** where a feature needs the network (sign-in, sync status, consented online recognition T3a) | "You're offline. Your practice is saved on this phone and will sync later." | Optional "Retry" | Polite announcement once |
| **Permission (microphone)** | Before first recording; denied; permanently denied | 1) Education screen SCR-PERM-01 (why, what stays on the phone) → system prompt. 2) Denied once: inline card in the session, "SpeakCraft needs the microphone to hear your practice." 3) Permanently denied (no rationale and not granted): same card with "Open settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`). Re-check automatically on `ON_RESUME`. | — | "Allow microphone" / "Open settings" | Card announced; focus moves to it |
| **Permission (notifications, API 33+)** | When the user turns on a reminder, never at launch | Reminder row shows "Notifications are off for SpeakCraft" | — | "Open settings" | — |
| **Capability unavailable** (CAP-12, e.g. tier T3b) | No on-device recognition | Honest explanation of what still works (audio-only metrics) and the options | "This phone can't turn speech into text offline. We can still measure your pace and pauses." | "Learn more" / "Use online recognition…" (consent flow) | — |

### 11.20 Prompt card and transcript view

| Component | Specification |
|---|---|
| Prompt card | Container `surfaceContainerLow`, radius `medium`, padding 16 dp. Template label (`labelMedium`, e.g. "Answer a question"). Prompt in `promptText`. Optional "Listen" tonal button (`AutoMirrored.Outlined.VolumeUp`) for Listen & repeat. Optional preparation tip (`bodyMedium`). Target words use `targetWordEmphasis`. Max width 640 dp. |
| Transcript view | `bodyLarge`, selectable text. Heading "What we heard". Long pauses shown as " … " with the accessibility label "pause". Target words emphasised. Caveat link "How speech recognition works" → sheet. Never edits or embellishes recognised text. |

### 11.21 Word card and stage stepper (Words)

| Component | Specification |
|---|---|
| Word card | Word in `headlineSmall`; part of speech and meaning in `bodyLarge`; example sentence in `bodyMedium`. "Listen" button (TTS). No pronunciation score (NG-03). |
| Stage stepper | Five labelled steps: **Learn · Say it · Use it · Make your own · Review** (RD-16 CAP-07; DES-02 §6). Each step has an icon state (done: check; current: ring; upcoming: neutral) **and** text. Semantics: "Stage 2 of 5, Say it, current." |

### 11.22 Selection controls (settings rows)

- `Switch`, `Checkbox` and `RadioButton` always sit in a row whose **whole row** is the target.
  - Switch rows: `Modifier.toggleable(role = Role.Switch)` on the row, with the inner control's `onCheckedChange = null`.
  - Radio groups: `Modifier.selectableGroup()` on the group.
- The label sits on the start side and the control on the end side. Supporting text explains the effect in plain words.
- This fixes the bare "Switch" announcements at `AS:624-645` and `ND:740-749`.

---

## 12. Terminology, voice and tone

### 12.1 Voice

| Trait | Means | Avoid |
|---|---|---|
| **Calm** | Even, unhurried statements | Exclamation marks (max one, milestones only), hype, urgency |
| **Plain** | CEFR B1-level English: sentences of about 20 words or fewer, common words, no idioms | Jargon, abbreviations, phrasal-verb slang |
| **Honest** | Say what was measured and what wasn't | "Perfect!", "Verified", "Objective", invented precision |
| **Encouraging, not pushy** | Notice effort and improvement; suggest one next step | Guilt ("You'll lose your streak!"), loss framing, nagging |
| **Respectful** | Treat users as capable adults; never blame | "Oops", "You failed", "Wrong" |

| Context | Tone | Example |
|---|---|---|
| Onboarding | Welcoming, brief | "Let's find the right practice for you." |
| Session prompt | Clear, direct | "Answer in about one minute. Take a moment to plan first." |
| Feedback | Specific, balanced | "Your pace stayed steady. Try this next: pause briefly between your main points." |
| Inconclusive | Reassuring, practical | "We couldn't measure this attempt. It was too quiet to hear clearly. Try again a little closer to the phone." |
| Error | Calm, actionable | "Couldn't save your settings. Try again." |
| Privacy | Precise, true | "Your recordings stay on this phone. They are deleted after 30 days unless you change this." (exact text per D-10) |
| Milestone | Warm, modest | "Seven days in a row. Nice work." |

### 12.2 Canonical product vocabulary

| Term | Meaning | Notes |
|---|---|---|
| **Today** | Primary destination showing today's session | Not "Home", "Dashboard" |
| **Today's session** | The daily plan of 3–4 steps (CAP-03) | "Session" is reserved for this (DES-02 §6) |
| **Step** | One item in today's session | — |
| **Practice** | Primary destination: the library of tracks and activities | Verb spelling per OQ-DS-3 |
| **Track** | A themed collection (Everyday conversation, Workplace, Job interviews, Presentations & storytelling, Clear speech) | Not "Path", "Program", "Module", "Hub" |
| **Activity** | One practice item in a track, based on a template | Not "Drill", "Lab", "Exercise template" |
| **Prompt** | What the user is asked to say or read | — |
| **Prepare** | Optional planning time before recording | — |
| **Record / Stop** | Recording controls | Fixed labels everywhere |
| **Recording** | The audio of one attempt (kept on the phone) | — |
| **Attempt** | One recording and its result | "Try again" creates a new attempt |
| **Feedback** | The result of an attempt | Not "AI feedback", "AI check", "Report" |
| **What went well / Try this next** | One strength and one focus | **Focus** is the noun ("Your focus: fewer long pauses") |
| **Transcript / What we heard** | Text recognised from the recording | — |
| **Measured** | Computed directly from the audio or transcript | Badge text, exactly |
| **Estimated** | A heuristic inference | Badge text, exactly |
| **Inconclusive** | Not enough evidence to measure | Badge text, exactly; never "failed" |
| **Recognised as intended** | Agreement between the transcript and the reference text | **Never** "pronunciation score" (NG-03) |
| **Pass mark / Not yet** | Threshold result for word stages and read-aloud | Not "Fail" |
| **Words** | Primary destination: vocabulary | Not "Vocabulary Builder", "Tools" |
| **Learn · Say it · Use it · Make your own · Review** | Word stages | Exact labels |
| **Review** | Spaced review of words | — |
| **Progress** | Primary destination: trends and history | Not "Analytics" |
| **Practice time** | Minutes of recorded speaking and session time, measured | Not "Practice hours" from ticks |
| **Streak** | Consecutive days with at least one valid attempt (SRS-BR) | "4-day streak" |
| **Check-in** (R2) | Repeatable benchmark task | — |
| **Goal / Level / Daily time** | Personalisation settings | One label each, everywhere (DES-01 UXF-17) |
| **Reminder** | System notification at the chosen time | **Inbox** is R2 |
| **Speech recognition** | Turning speech into text, on this phone or online with consent | — |
| **Profile / Settings** | — | — |

### 12.3 Banned and retired terms

| Banned | Why | Use instead |
|---|---|---|
| "AI", "AI Coach", "AI Mentor", "AI Check", "Smart …" for deterministic features | Misrepresents template or rule code (PP-1; NG-01/02) | "Feedback", or describe the feature |
| "Studio", "Engine", "Lab", "Hub", "Console", "Intelligence" | Internal or marketing jargon (UX-1) | Canonical terms in §12.2 |
| "@askvinh", "Vinh Giang", or any creator name; borrowed framework names ("3-Stage Self-Audit", "5 Vocal Foundations", "4-Color Communicator", "Friendly Authority") | IP risk and unexplained references (PP-8; D-08) | Plain, original names |
| Developer terms: "Firestore", "CameraX", "Photo Picker", "TFLite", "Whisper", "Moonshine", "MediaPipe", "DSP", "evaluator", "evidence gate", "LOCAL_ONLY", enum names (e.g. "INSUFFICIENT_EVIDENCE", "EXTRACTED"), byte sizes, exception messages | Not actionable for users (DES-01 UXF-53) | Plain-language cause and action |
| "Pronunciation score", "accent score" | Not measured (NG-03) | "Recognised as intended" (read and repeat only) |
| "Overall score", "fluency score", "x/100" for free speaking | No composite (D-06) | Individual labelled metrics |
| "Verified", "Objective", "Accurate" applied to app output | Unprovable claims (EP-5) | Measured / Estimated |
| "Words learned" for words merely seen; "Mastered" | Inflated progress (PP-7) | "Words at Review stage" (per FS-07) |
| "Welcome back" on a first visit | Wrong (DES-01 UXF-27) | First-run copy |
| "Fail", "Wrong", "Oops", "Don't break your streak!" | Blame or guilt (PP-7) | "Not yet. Try again." |
| "Session" for a single recording | Reserved term | "Attempt" |

### 12.4 Writing rules

1. Sentence case for all UI text. Button labels are verb-first and ideally 3 words or fewer.
2. Use "you" for the user and "we" for SpeakCraft. Contractions are fine ("can't", "we'll").
3. Write numbers as numerals with locale formatting. Visible units may be short ("words/min"); accessibility text spells them out ("words per minute").
4. Errors follow the formula **what happened + what to do** (+ reassurance about data where relevant).
5. Metric explanations follow **what it is · how we measure it · why it matters · its limits**.
6. Never put counts of content in static copy ("150 activities"). Compute them or omit them (DES-01 UXF-61).
7. Format dates, times and durations with the device locale. The first day of the week comes from the locale.
8. All strings live in `res/values/strings.xml`, with plurals in `<plurals>` (DES-07 A11Y-36).
9. Spelling standard: per OQ-DS-3 (recommendation: British English, matching the documentation set).

### 12.5 Examples

| Don't (current) | Do |
|---|---|
| "Analyze With Local AI Coach" | (Automatic after Stop) "Checking your recording…" |
| "Status: INSUFFICIENT_EVIDENCE • No score awarded" | "We couldn't measure this attempt. We heard less than 3 seconds of speech." |
| "Welcome back, Sruthi" (first visit) | "Hi Sruthi. Your first session is ready." |
| "1 Day Streak" (day 0) | "Start your streak today" |
| "Record and Review Studio: CameraX video or Photo Picker (<19MB)" | "Record your answer" |
| "Pronunciation 65/100" (free speech) | "Recognised as intended: 82 % · Measured" (read-aloud only) |

---

## 13. Implementation mapping

### 13.1 Package and file structure

The location is a package inside `:app` initially (single module, RD-03 FND-ARCH-01). Promotion to a `:core:designsystem` Gradle module is decided in ARC-02. The root package follows D-15.

| File | Contents |
|---|---|
| `core/designsystem/theme/Color.kt` | `internal object ScPalette`: raw tonal values from §3.2–3.3 (the only place with `Color(0x…)`) |
| `core/designsystem/theme/ColorScheme.kt` | `LightColors = lightColorScheme(…all §3.2 roles…)`, `DarkColors = darkColorScheme(…)` |
| `core/designsystem/theme/SemanticColors.kt` | `@Immutable data class SemanticColors(…§3.3…)`, `LightSemanticColors`, `DarkSemanticColors`, `LocalSemanticColors` |
| `core/designsystem/theme/Type.kt` | `InterFamily`, `ScTypography` (15 roles, §4.2), `ScTextStyles` (§4.3) |
| `core/designsystem/theme/Shape.kt` | `ScShapes` (§6.3), `ScBorders` (§6.2) |
| `core/designsystem/theme/Spacing.kt` | `object Spacing` (§5.1–5.2), `AdaptiveWindowTier`, `tierForWidth`, `contentMaxWidthFor`. Migrated from `ui/theme/Spacing.kt`. |
| `core/designsystem/theme/Motion.kt` | `object Motion` (durations §8.1, easings §8.2), `LocalReducedMotion`, `rememberSystemReducedMotion()` (reactive `ContentObserver`), helpers such as `Motion.tween(durationMs)` that return `snap()` when reduced |
| `core/designsystem/theme/Theme.kt` | `SpeakCraftTheme(themeMode, reduceMotionPref, content)` providing `MaterialTheme(colorScheme, typography, shapes)` + `LocalSemanticColors` + `LocalReducedMotion`; `SystemBarsEffect` (§10) |
| `core/designsystem/icon/ScIcons.kt` | Icon map (§7); later the imported vector drawables |
| `core/designsystem/component/` | `Buttons.kt` (loading-capable wrappers), `RecordButton.kt`, `LevelMeter.kt`, `MetricTile.kt`, `EvidenceBadge.kt`, `FeedbackCard.kt`, `InconclusiveCard.kt`, `StreakWeek.kt`, `StreakMonth.kt`, `LineChart.kt`, `BarChart.kt`, `StateViews.kt` (Empty / Error / Offline / Permission / Skeleton), `SectionHeader.kt` (with `heading()`), `PromptCard.kt`, `TranscriptView.kt`, `WordCard.kt`, `StageStepper.kt`, `SettingsRows.kt`, `ScTopAppBar.kt`, `ScNavigation.kt` (bar or rail by tier) |
| `core/designsystem/preview/` | Multi-preview annotations: light/dark × font 1.0/2.0 × widths 360/600/840 dp |

```kotlin
// Access pattern (sketch)
object SpeakCraftTheme {
    val semanticColors: SemanticColors
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}
// Usage: Text("Measured", color = SpeakCraftTheme.semanticColors.onMeasuredContainer)
```

### 13.2 Tests for the design system (existing stack only)

| Test | Type | Reference |
|---|---|---|
| Token contrast: compute every §3.4 pair from `ScPalette` and assert ≥ min | JVM unit test (pure Kotlin) | DES-07 TC-A11Y-001 |
| Component semantics (badge text, record button descriptions, merged tile phrase, headings) | Robolectric + Compose UI test | TC-A11Y-002…006, 009 |
| 200 % font: no visual overflow in tiles, cards, buttons | Robolectric with `Density(fontScale = 2f)` | TC-A11Y-007 |
| Touch targets ≥ 48 dp | Robolectric | TC-A11Y-008 |
| Reduced-motion variants rendered | Robolectric with `LocalReducedMotion provides true` | TC-A11Y-010 |

### 13.3 Migration from the current "Violet Studio" theme

**Strategy:** add the new design system **beside** `ui/theme` in R0. New R1 screens use **only** the new tokens. Current screens are **retired or replaced** (RD-16 §5) rather than re-skinned, because most of them are removed. Delete `ui/theme` when the last legacy screen is gone.

| Current token (`ui/theme`) | New token | Note |
|---|---|---|
| `RoyalViolet` #6C3DF4 / `LuminousViolet` #A78BFA (primary) | `primary` #4054A6 / #B8C3FF | Calmer accent (D-18) |
| `StudioCanvasLight` #F4F5FB / `MidnightIndigoBg` #100C2A | `surface` #FAF9FD / #121318 | Also the window background (§10) |
| `CrispWhite` (surface) / `ElevatedIndigoSurface` | `surfaceContainerLowest` / `surfaceContainerLow` | Cards move to `surfaceContainerLow` |
| `SoftLavenderSurface` / `IndigoSurfaceVariant` | `surfaceContainerHigh` (or `surfaceVariant`) | — |
| `DeepIndigoInk` / `SoftWhiteOnDark` | `onSurface` #1A1B21 / #E3E1E9 | — |
| `CoolSlateOnLight` / `MutedLavenderOnDark` | `onSurfaceVariant` #45464F / #C6C5D0 | — |
| `SubtleBorderLight` / `SubtleBorderDark` (as `outline`) | `outline` for boundaries; `outlineVariant` for decoration | Fixes the 1.24:1 and 1.36:1 boundaries |
| `SkyAccent` (secondary) | `info` (extended) | `secondary` becomes neutral slate |
| `MintSuccess` (tertiary and success) | `success` (extended) | `tertiary` becomes muted plum |
| `WarmAmber` (warning) | `warning` #7A5900 · `streak` #9C4300 | Split by meaning |
| `RoseCrimson` (error) / `recordingPulse` | `error` #BA1A1A · `recording` #B3261E | — |
| `StreakSurface*` | `streakContainer` | — |
| `sparklinePointCenter` | `surface` | — |
| `Sidebar*` (6), `Hero*` (6), `studioConsole*` | **Retired** | No sidebar, no hero overlays |
| `Camera*` | **Retired** | Video is out of R1; re-specify in R3 (ADR-010) |
| `Pastel*` (16 roles) | **Retired** | No decorative tiles (DS-1) |
| `AcousticTeal`, `AmberSpotlight`, `EmeraldSuccess` aliases (25 raw uses that bypass dark mode) | **Retired** | Their screens (YT, LM, DP, ADM) are removed or replaced |
| Shapes 14 / 20 / 24 / 30 dp | 4 / 8 / 12 / 16 / 28 dp | §6.3 |
| Plus Jakarta Sans, DM Sans, JetBrains Mono (single weight) | Inter 400 / 500 / 600 | §4.1; asset swap |
| `SpeakCraftSpacing` (xs…xxl) | `Spacing` (same values + semantic aliases) | §5 |
| `rememberReducedMotionEnabled` (non-reactive, in `CoachComponents.kt`) | `LocalReducedMotion` (reactive + in-app preference) | §8.3 |
| `enableEdgeToEdge()` default auto style | Theme-aware `SystemBarStyle` | §10 |
| `res/values(-night)/colors.xml` `speakcraft_window_bg` #FAF9F6 / #0F172A | #FAF9FD / #121318 | §10 |

**Steps** (to be scheduled in PLN-02):
1. R0: create `core/designsystem` with tokens, the theme and the contrast test.
2. R0: swap the font assets and add the licence.
3. R1: build design-system components with previews and tests before screens.
4. R1: the new 4-tab shell uses `SpeakCraftTheme` from `core/designsystem`.
5. R1: update window and splash colours.
6. After R1 cut-over: delete `ui/theme`, the legacy fonts and `material-icons-extended` (if OQ-DS-6 is approved).

### 13.4 CI guardrails (grep-based; no new library; owned by ENG-01)

| ID | Check | Current baseline [VERIFIED, UIA §5.2] | Target |
|---|---|---|---|
| G-1 | `Color(0x` only in `core/designsystem/theme/` | 0 outside theme | Keep 0 |
| G-2 | `RoundedCornerShape(` only in the design system | 147 | 0 in new code |
| G-3 | Literal `.sp` only in `Type.kt` | 2 | 0 |
| G-4 | `fontWeight =` overrides outside the design system | 95 | 0 in new code |
| G-5 | Literal `N.dp` outside the design system (allowlist: `0.dp`, `1.dp`) | 1,386 | Warning now; fail for new files |
| G-6 | Hardcoded UI strings (`Text("…")`, `text = "…"`) outside previews and tests | about 386 | 0 in new code (DES-07 A11Y-36) |
| G-7 | Banned terms (§12.3) in `strings.xml` (case-insensitive, with allowlist) | n/a | 0 |
| G-8 | Monospace fonts in UI (`FontFamily.Monospace`, mono font files) | 103 `labelSmall` uses | 0 |

---

## 14. Open questions and decisions

| ID | Question | Recommendation |
|---|---|---|
| OQ-DS-1 (**D-18**) | Approve the brand palette | Option A "Calm Indigo" (§3.1–3.4) |
| OQ-DS-2 | Approve the font family and an APK size budget for fonts | Inter, 3 static weights, Latin subset (§4.1) |
| OQ-DS-3 | UI spelling standard: British ("practise", "colour") or US | British English, consistent with the documentation; strings in resources allow a later `values-en-rUS` |
| OQ-DS-4 | Badges on navigation items (for example Words reviews due) | None in R1; Today surfaces due reviews |
| OQ-DS-5 | In-app "Vibration feedback" toggle | Include in Appearance (default on) |
| OQ-DS-6 | Icon strategy: keep `material-icons-extended`, or import about 30 Material Symbols vector drawables | Import and drop the artifact (APK size; ties to the R8 decision in SEC-01) |
| OQ-DS-7 | Display names for the 5 levels (D-17) | Plain descriptive names, not CEFR codes (NG-04) |
| OQ-DS-8 | Countdown before recording: default on or off | Off by default; available in Learning preferences (SCR-SET-02) |

## Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial draft. New Calm Indigo palette with computed contrast; type, spacing, motion and component specs; terminology; migration plan. | Design System Architect |
