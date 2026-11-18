# SpeakCraft — Accessibility Audit & Remediation Plan

**Revision Date:** 2026-11-18  
**Standards Applied:** Material Design 3 Accessibility, Android TalkBack Semantics, WCAG 2.1 Level AA Contrast & Target Size (`>= 48.dp`).

---

## 1. Accessibility Findings (`Verified defect` -> `Implemented / Proposed`)

| ID | Priority | Component / File | Finding | Remediation |
| :--- | :--- | :--- | :--- | :--- |
| `A11Y-01` | **High priority** | `CoachComponents.kt` (`LiveWaveformVisualizer`) | `Canvas` waveform has no `semantics { contentDescription = ... }`; screen reader users receive no state cue (recording vs paused vs idle). | Add dynamic `Modifier.semantics { contentDescription = ...; stateDescription = ... }` announcing `"Microphone recording waveform, elapsed ..."` / `"Recording paused"` / `"Waveform idle"`. |
| `A11Y-02` | **High priority** | `CoachComponents.kt` (`ScoreTrendSparkline`) | `Canvas` trend chart has no `contentDescription` summarizing latest score, count, and min/max trajectory. | Add `Modifier.semantics { contentDescription = "Score trend across X sessions, from A to B out of 100" }`. |
| `A11Y-03` | **High priority** | `AuthAndOnboardingScreens.kt` (`FilterChip` row) | Horizontal `Row` of 4 skill level chips clips or crowds on `320dp` / large font scale devices (`ISS-019`). | Replace `Row` with `ExperimentalLayoutApi` `FlowRow` so chips wrap gracefully at `2.0x` font scaling. |
| `A11Y-04` | **Medium priority** | `DashboardScreen.kt`, `SpeakingAndChallengesScreens.kt`, `DailyAndProgramsScreens.kt` | Several `Button` / `IconButton` / clickable cards should explicitly guarantee `48.dp` minimum touch targets and crisp `contentDescription` labels. | Enforce `Modifier.defaultMinSize(minHeight = 48.dp)` and audit all `IconButton` / `FilterChip` touch areas; keep decorative icons `contentDescription = null`. |
| `A11Y-05` | **Medium priority** | `CoachComponents.kt` (`ShimmerSkeletonCard`, `LiveWaveformVisualizer`) | Infinite pulse/wave animations ignore Android's `ANIMATOR_DURATION_SCALE == 0f` reduced-motion preference. | Respect `rememberReducedMotionEnabled()` so pulsing/phase animations freeze cleanly when reduced motion is enabled. |
| `A11Y-06` | **Medium priority** | `Color.kt` / `Theme.kt` / `Type.kt` | Single-weight `400` font mapping flattened visual heading hierarchy (`ISS-015`), and inline hex colors bypassed high-contrast theme tokens (`ISS-014`). | Restore font weight hierarchy (`Type.kt`) and move all hero/status colors to `MaterialTheme.speakCraftColors` ensuring >= 4.5:1 body contrast & >= 3:1 large text/UI contrast in both Light (`WarmIvory`) and Dark (`MidnightNavy`) modes. |
| `A11Y-07` | **Low priority** | `LessonAndModulesScreens.kt`, `SpeakingAndChallengesScreens.kt` | Headings inside scrollable `LazyColumn` cards lack explicit `semantics { heading() }` traversal hints for TalkBack heading navigation. | Add `Modifier.semantics { heading() }` to `SectionHeader` titles so TalkBack users can jump smoothly between sections. |

---

## 2. Color Contrast Verification Summary

* **Light Mode (`WarmIvory #FAF9F6` / `CrispPaper #FFFFFF`):**
  * Primary text `#0F172A` on `#FFFFFF` = **17.8:1** (AAA)
  * Secondary muted text `#475569` on `#FFFFFF` = **7.5:1** (AAA)
  * White `#FFFFFF` on `AmberSpotlight #D97706`: Borderline for tiny text; therefore primary buttons use bold `labelLarge` (`14.sp` bold) + `52.dp` height or container tints (`#FEF3C7` with `#78350F` ink = **8.9:1**).
  * `AcousticTeal #0D9488` text vs `#0F766E` (`DarkerTeal` for small body text on white) = upgrading small light-mode teal labels or pairing with `#115E59` on `#CCFBF1` ensures >= 4.5:1 contrast everywhere!
* **Dark Mode (`MidnightNavy #0F172A` / `DeepSlate #1E293B`):**
  * Primary text `#FAF9F6` on `#1E293B` = **13.2:1** (AAA)
  * Secondary text `#CBD5E1` on `#1E293B` = **9.3:1** (AAA)
  * `AmberGoldBright #F59E0B` on `#0F172A` = **8.4:1** (AAA)
  * `AcousticTealBright #2DD4BF` on `#1E293B` = **8.6:1** (AAA)
