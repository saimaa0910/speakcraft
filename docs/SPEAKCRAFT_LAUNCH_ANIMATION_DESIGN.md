# SpeakCraft — Launch Animation Design Specification

**Date:** 2026-11-18  
**Release:** `v1.1.0-beta.1` (Build `2` • `beta` channel)  
**Design System:** SpeakCraft Editorial Studio  
**Brand Tagline:** *"Speak with Confidence. Communicate with Impact."*

---

## 1. Design Philosophy & Observation Boundary

### 1.1 Modern Native Jetpack Compose Identity
SpeakCraft's opening experience is authored **100% natively in Jetpack Compose (`Canvas` + `graphicsLayer` + `Animatable`)** with zero dependency on static raster splash stickers. The visual language blends high-precision acoustic engineering with warm editorial polish:
- **Zero Raster Artifacts:** Every stroke, orbital arc, concentric voice wave, specular sheen, and equalizer capsule renders at native display density across compact phones, standard handhelds, foldables, and tablets.
- **No Unverified Reference Fabrication:** No third-party screen recording or proprietary asset is copied; all choreography and brand geometry belong exclusively to SpeakCraft's **Editorial Studio** palette and typography.

### 1.2 Non-Blocking Startup Architecture
Rather than blocking `MainActivity` behind a secondary splash activity:
- Android's system window background (`@color/speakcraft_window_bg`) and Android 12+ splash background (`values-v31/themes.xml`) match SpeakCraft's exact **Warm Ivory (`#FAF9F6`)** Light surface and **Midnight Navy (`#0F172A`)** Dark surface.
- `SpeakCraftLaunchOverlay` renders above the live `SpeakCraftRoot` composition tree. While the overlay runs, Firebase App Check, `authStateFlow()`, DataStore preferences, and Firestore user listeners initialize immediately underneath.
- Completion latches in an Activity-scoped `LaunchExperienceViewModel`, guaranteeing **zero replay** on screen navigation, device rotation/multi-window resize, or returning from background.

---

## 2. Visual Composition & Theme Adaptation

### 2.1 Light & Dark Theme Token Mapping
All colors resolve from `MaterialTheme.colorScheme` and `MaterialTheme.speakCraftColors`:

| Element | Light Theme (`Warm Ivory`) | Dark Theme (`Midnight Navy`) |
| :--- | :--- | :--- |
| **Canvas Backdrop** | Warm Ivory (`#FAF9F6`) | Midnight Navy (`#0F172A`) |
| **Window / OS Splash Surface** | `@color/speakcraft_window_bg` (`#FFFAF9F6`) | `@color/speakcraft_window_bg` (`#FF0F172A`) |
| **Upper Stage Spotlight** | Amber Container (`#FEF3C7` @ 85%) | Amber Gold Bright (`#F59E0B` @ 22%) |
| **Lower Acoustic Halo** | Teal Container (`#CCFBF1` @ 62%) | Acoustic Teal Bright (`#2DD4BF` @ 16%) |
| **Studio Calibration Ticks** | Subtle Border Light (`#E2E8F0` @ 45%) | Subtle Border Dark (`#334155` @ 28%) |
| **Medallion Core Surface** | Deep Midnight Studio (`HeroOverlayEnd`) | Deep Midnight Studio (`HeroOverlayEnd`) |
| **Primary Crest / Bezel / Eyebrow** | Amber Spotlight (`#D97706`) | Amber Gold Bright (`#F59E0B`) |
| **Secondary Arc / Equalizer Blend** | Acoustic Teal Accessible (`#0F766E`) | Acoustic Teal Bright (`#2DD4BF`) |
| **Wordmark (`"SpeakCraft"`)** | Midnight Navy (`#0F172A`) | Warm Ivory (`#FAF9F6`) |
| **Tagline** | Muted Slate On Light (`#475569`) | Slate Muted Light (`#CBD5E1`) |

### 2.2 Responsive Dimensions & Safe Areas
Layout geometry adapts dynamically via `BoxWithConstraints` and `WindowInsets.safeDrawing`:

| Device Class | Constraint | Medallion Size | Equalizer Width | Max Content Lockup |
| :--- | :--- | :---: | :---: | :---: |
| **Compact Phone** | `min(W, H) < 360.dp` | `104.dp` | `176.dp..280.dp` | `560.dp` |
| **Standard Phone** | `360.dp .. 599.dp` | `124.dp` | `176.dp..280.dp` | `560.dp` |
| **Tablet / Unfolded** | `min(W, H) >= 600.dp` | `148.dp` | `280.dp` | `560.dp` |

---

## 3. Deliberate Four-Stage Choreography

Previous timings (`1550 ms` / `900 ms`) were replaced with unhurried, studio-paced cadences so every visual beat breathes cleanly without feeling rushed:

| Cadence | Duration | Trigger Condition |
| :--- | :---: | :--- |
| **`FIRST_LAUNCH`** | `2900 ms` | First cold launch (`hasCompletedInitialLaunch == false`) |
| **`RETURNING_LAUNCH`** | `2100 ms` | Subsequent cold starts (`hasCompletedInitialLaunch == true`) |
| **`REDUCED_MOTION`** | `600 ms` | System `ANIMATOR_DURATION_SCALE == 0f` |

### 3.1 Stage Progression (`progress` `0.00 -> 1.00`)
1. **Stage 1 — Studio Atmosphere (`0.00 .. 0.14` | `0..405 ms` first launch)**
   - Volumetric radial spotlights fade in smoothly (`LinearOutSlowInEasing`) alongside subtle 36-tick studio aperture calibration lines.
2. **Stage 2 — Acoustic Aperture & Living Voice Mark (`0.10 .. 0.48` | `290..1390 ms`)**
   - Medallion rises (`scale 0.78f -> 1.00f`) with studio spring overshoot (`CubicBezierEasing(0.18f, 1.10f, 0.30f, 1f)`).
   - Dual counter-rotating sweep-gradient orbital rings (`225°` and `245°` arcs) with an orbital satellite bead rotate gracefully around the crest.
   - Inside the medallion, triple directional acoustic arcs draw sequentially (`arcSweepFraction`), flanking a 5-bar waveform core, followed by a diagonal glass specular sheen sweep (`0.30..0.64`).
   - Below the medallion, a **15-bar Living Harmonic Voice Equalizer** (`launch_sound_wave`) blooms outward from the center with dual-frequency harmonic shimmer and Amber-to-Teal interpolation.
3. **Stage 3 — Editorial Typography Lockup (`0.42 .. 0.80` | `1220..2320 ms`)**
   - Uppercase eyebrow `"VOICE & COMMUNICATION STUDIO"` fades in crisply (`0.42..0.60`).
   - `"SpeakCraft"` wordmark (`displayLarge`, Bold) rises (`18.dp -> 0.dp`) with **letter-spacing settle** (`2.4.sp -> -0.5.sp`).
   - Centered `112.dp` Amber-to-Teal horizon rule sweeps outward from a glowing center node (`0.54..0.76`).
   - Tagline `"Speak with Confidence. Communicate with Impact."` glides gently (`12.dp -> 0.dp`, `0.60..0.80`).
4. **Stage 4 — Editorial Hold & Seamless Dissolve (`0.80 .. 1.00` | `2320..2900 ms`)**
   - Full composition holds steadily (`0.80..0.88`, `~230 ms`) for crisp brand recognition.
   - Final dissolve (`0.88..1.00`): overlay alpha eases `1f -> 0f` while the lockup lifts forward (`scale 1.0f -> 1.04f`), revealing the live `AuthScreen` or `DashboardScreen` underneath.

### 3.2 Accessibility & Reduced Motion
- Under `REDUCED_MOTION` (`600 ms`), orbital rotation, scaling travel, letter-spacing contraction, sheen sweep, and `InfiniteTransition` waves are disabled; the crisp native crest, equalizer, wordmark, and tagline perform a calm fade-in hold and crossfade out.
