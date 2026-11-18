# SpeakCraft Design System Specification

**Revision Date:** 2026-11-18  
**Design Aesthetic:** **Editorial Studio** — authoritative, calm, tactile, high-contrast executive speaking studio.  
**Implementation Status:** Preserves and formalizes existing palette (`Color.kt`, `Theme.kt`) while resolving `ISS-014` (hardcoded hexes & legacy `Theme.MyApplication`) and `ISS-015` (typography weight hierarchy).

---

## 1. Color System & Semantic Tokens (`Color.kt` & `Theme.kt`)

### 1.1 Core Brand & Surface Palette (`Existing` + `Extended`)
| Token | Light Mode Hex | Dark Mode Hex | Role |
| :--- | :--- | :--- | :--- |
| `primary` | `#D97706` (`AmberSpotlight`) | `#F59E0B` (`AmberGoldBright`) | Primary CTAs, active spotlight indicators, key metric accents |
| `onPrimary` | `#FFFFFF` | `#0F172A` (`MidnightNavy`) | High-contrast label/icon over primary fill |
| `primaryContainer` | `#FEF3C7` | `#451A03` | Selected filters, active step badges, spotlight banners |
| `secondary` | `#0D9488` (`AcousticTeal`) | `#2DD4BF` (`AcousticTealBright`) | Audio telemetry, `@askvinh` verified source tags, IPA phonetics |
| `background` | `#FAF9F6` (`WarmIvory`) | `#0F172A` (`MidnightNavy`) | Main canvas background |
| `surface` | `#FFFFFF` (`CrispPaper`) | `#1E293B` (`DeepSlate`) | Elevated cards, scorecards, studio stage containers |
| `surfaceVariant` | `#F1F5F9` (`SoftStone`) | `#283548` (`SlateSurface`) | Input backdrops, subtle containers, inactive waveforms |
| `outline` | `#E2E8F0` | `#334155` | 1dp crisp structural card borders |
| `error` | `#DC2626` (`CrimsonError`) | `#F87171` | Destructive actions, active recording stop state, mistake alerts |

### 1.2 Extended Semantic Tokens (`SpeakCraftColors` `CompositionLocal` — `Proposed redesign`, fixes `ISS-014`)
To eliminate all inline `Color(0x...)` literals across Composables, `Theme.kt` exposes `MaterialTheme.speakCraftColors`:
* `success` / `onSuccess` / `successContainer`: `#059669` / `#FFFFFF` / `#D1FAE5` (Light) and `#34D399` / `#0F172A` / `#064E3B` (Dark)
* `warning` / `onWarning` / `warningContainer`: `#D97706` / `#FFFFFF` / `#FEF3C7` (Light) and `#FBBF24` / `#0F172A` / `#451A03` (Dark)
* `info` / `infoContainer`: `#0D9488` / `#CCFBF1` (Light) and `#2DD4BF` / `#134E4A` (Dark)
* `heroOverlayStart` / `heroOverlayEnd`: `#330F172A` / `#EB0F172A`
* `onHeroTitle` / `onHeroSubtitle` / `onHeroPill`: `#FFFFFF` / `#E2E8F0` / `#FFFBEB`
* `cameraScrim` / `recordingIndicator`: `#A6000000` / `#F87171`

### 1.3 Android XML Theme Alignment (`themes.xml` & `AndroidManifest.xml`)
* Rename `@style/Theme.MyApplication` -> `@style/Theme.SpeakCraft` with dark/light status bar compatibility (`ISS-014`).

---

## 2. Typography System (`Type.kt` — Fixes `ISS-015`)

### 2.1 Bundled Fonts (`app/src/main/res/font/`)
* **Headlines & UI Controls:** `plus_jakarta_sans.ttf` (installed via `font-util`)
* **Reading & Coaching Body:** `dm_sans.ttf` (installed via `font-util`)
* **Telemetry, IPA, Timers & SHA-256:** `jetbrains_mono.ttf` (installed via `font-util`)

### 2.2 Weight Hierarchy Fix (`Verified defect` -> `Proposed redesign`)
* **Root Cause Verified (`ISS-015`):** Binary inspection confirmed the bundled `.ttf` assets are single-weight (`OS/2 usWeightClass = 400`) static fonts. Mapping `FontWeight.Normal`, `FontWeight.SemiBold`, and `FontWeight.Bold` all to the same resource inside `FontFamily(...)` tells Android/Compose not to synthesize bold strokes—flattening headings and body text to identical `400` stroke weight.
* **Fix:** Register `Font(R.font.<family>, FontWeight.Normal)` in each `FontFamily` so Android/Skia applies authentic weight differentiation when `FontWeight.Medium`, `FontWeight.SemiBold`, and `FontWeight.Bold` are specified, reinforced by distinct scale steps (`displayLarge 34sp`, `headlineLarge 26sp`, `headlineMedium 21sp`, `titleLarge 18sp`, `titleMedium 16sp`, `bodyLarge 16sp/24sp`, `bodyMedium 14sp/20sp`, `labelSmall 11sp` JetBrains Mono).

---

## 3. Spacing, Shape & Elevation Grid (`Spacing.kt` — `Proposed redesign`)

* **4dp / 8dp Spatial System:**
  * `xs = 4.dp` (inline icon-text micro gaps)
  * `sm = 8.dp` (chip gaps, compact stacks)
  * `md = 12.dp` (card internal spacing, list gaps)
  * `lg = 16.dp` (standard screen horizontal/vertical padding & card padding)
  * `xl = 24.dp` (section breaks, modal dialogs, auth/onboarding framing)
  * `xxl = 32.dp` (major stage separation)
* **Touch Targets:** `minTouchTarget = 48.dp` enforced on all interactive buttons, chips, and icon actions.
* **Shapes (`SpeakCraftShapes`):** `small = 10.dp`, `medium = 16.dp`, `large = 24.dp`, `extraLarge = 32.dp`.
* **Elevation:** Flat-editorial crisp `1.dp` border (`MaterialTheme.colorScheme.outline`) on content cards; `2.dp` tonal elevation for interactive tiles and hero focus cards.

---

## 4. Standardized Reusable Components (`CoachComponents.kt`)

| Component | Role | Status |
| :--- | :--- | :--- |
| `SectionHeader` | Consistent title + muted subtitle + optional trailing action | `Existing` -> polish spacing & touch target |
| `MetricTile` | Compact KPI tile with label, value, and colored delta pill | `Existing` |
| `LiveWaveformVisualizer` | Animated/paused audio amplitude visualizer + accessibility description | `Existing` -> add `semantics` + reduced-motion fallback |
| `ScoreTrendSparkline` | Canvas line + gradient area chart + accessibility summary | `Existing` -> add `semantics` + theme-safe point ring |
| `VideoReviewPlayerCard` | **NEW:** In-app `VideoView` player for `.mp4` Stage 1 Mute Pass, Stage 2 Eyes-Closed Vocal Pass & Stage 3 Full AV Review | `Proposed redesign` (fixes `ISS-001`) |
| `ConfirmDestructiveDialog` | **NEW:** Standardized M3 `AlertDialog` for permanent deletes & snapshot restores | `Proposed redesign` (fixes `ISS-019`) |
| `CloudSyncWarningBanner` | **NEW:** Surfaces Firestore persistence failure + `"Retry Cloud Sync"` CTA without losing analyzed feedback | `Proposed redesign` (fixes `ISS-003`) |
| `StepProgressIndicator` | **NEW:** Clean segmented progress bar for the 3-Step Onboarding Assessment | `Proposed redesign` |
