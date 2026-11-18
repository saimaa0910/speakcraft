# SpeakCraft — Launch Animation Implementation Guide

**Date:** 2026-11-18  
**Architecture:** Kotlin + Jetpack Compose + Material 3 + Activity-Scoped `ViewModel` + Preferences DataStore

---

## 1. Architecture Overview

The launch experience is built as a lightweight, non-blocking overlay anchored at the root of `SpeakCraftRoot` (`ui/AppNavigation.kt`), completely above `AuthScreen` and `AuthenticatedAppShell` (`NavHost`). This guarantees:
- **Zero navigation replays:** Navigating between tabs or secondary routes inside `NavHost` never touches the overlay.
- **Configuration change & background resilience:** `LaunchExperienceViewModel` retains `isOverlayVisible`, `latchedCadence`, and `currentStage` across Activity recreation (screen rotation, fold/unfold, theme switches) and background-to-foreground resumes (`onStop` -> `onStart`).
- **Non-blocking startup:** `auth.authStateFlow()`, DataStore preferences, and Firestore user listeners start immediately on frame 1 underneath the overlay.
- **Zero interfering permissions or audio hooks:** Uses pure Compose graphics/animation primitives without touching `AudioRecorderManager`, CameraX, or microphone permissions.

---

## 2. Files Added & Modified

| File | Role |
| :--- | :--- |
| `app/src/main/java/com/example/ui/components/LaunchAnimationOverlay.kt` | `LaunchCadence` (`2900ms`/`2100ms`/`600ms`), `LaunchStage`, `LaunchTimelineSpec`, `LaunchExperienceViewModel`, `SpeakCraftLaunchOverlay`, and `SpeakCraftNativeStudioCrest` |
| `app/src/main/java/com/example/ui/AppNavigation.kt` | Wires `LaunchExperienceViewModel`, DataStore `hasCompletedInitialLaunch` latching, seamless settling curtain (`launch_animation_settling_bg`), `SpeakCraftLaunchOverlay`, beta channel selection, and Editorial Studio `UiState.Loading` polish |
| `app/src/main/java/com/example/data/preferences/AppPreferencesRepository.kt` | Adds `hasCompletedInitialLaunch: Boolean` and `markInitialLaunchCompleted()` |
| `app/src/main/res/values/strings.xml` | Adds `launch_studio_eyebrow`, `launch_tagline`, and `launch_logo_content_description` |
| `app/src/main/res/values/colors.xml` | Adds Light `speakcraft_window_bg` (`#FFFAF9F6`) & `speakcraft_splash_icon_bg` (`#FF0F172A`) |
| `app/src/main/res/values-night/colors.xml` | Adds Dark `speakcraft_window_bg` (`#FF0F172A`) |
| `app/src/main/res/values/themes.xml` | Applies `android:windowBackground` to `Theme.SpeakCraft` |
| `app/src/main/res/values-v31/themes.xml` | Configures Android 12+ `windowSplashScreenBackground` & `windowSplashScreenIconBackgroundColor` for flash-free OS handoff |
| `app/src/test/java/com/example/SpeakCraftLaunchAnimationTest.kt` | Unit & Robolectric Compose tests covering deliberate 4-stage timeline progression, first vs returning launch, signed-in/out flows, Light/Dark themes, reduced motion, native crest, background resume, and phone screen sizes |
| `app/src/test/java/com/example/SpeakCraftTabletLayoutTest.kt` | Adds tablet (`w1024dp-h768dp`) launch overlay verification (`148.dp` scaled emblem & centered typography) |

---

## 3. Performance & Anti-Jank Engineering

1. **Single-Clock Timeline (`Animatable`):**
   - A single `Animatable(0f)` drives normalized `progress` (`0f..1f`) with `LinearEasing` over `cadence.durationMs`, synchronized to Compose's `MonotonicFrameClock`.
2. **Zero-Recomposition Frame Updates:**
   - Opacity, scale, and vertical translation are applied inside `Modifier.graphicsLayer { ... }` lambdas, and spotlights/calibration ticks/orbital arcs/crest geometry/15-bar harmonic sound-wave bars draw inside `Canvas { ... }`, bypassing layout and recomposition passes during the reveal.
3. **Bounded `InfiniteTransition`:**
   - Ambient radial breathing and dual-frequency harmonic equalizer motion run only while the overlay is visible and `effectiveCadence != LaunchCadence.REDUCED_MOTION`. Once `onFinished()` flips `isOverlayVisible = false`, the composable leaves the composition tree and releases all animation observers.
4. **System Splash Continuity:**
   - `values/themes.xml` and `values-v31/themes.xml` match the exact Editorial Studio surface color (`Warm Ivory` in Light mode, `Midnight Navy` in Dark mode) so the OS launch window flows seamlessly into Stage 1 without a double splash screen.
5. **Slow-Network Continuity:**
   - If Firestore profile loading (`AuthenticatedAppShell` `UiState.Loading`) exceeds the `2.90s` / `2.10s` launch reveal, a themed Editorial Studio loading surface (`"Preparing your speaking studio..."`) bridges smoothly until `UiState.Success` arrives.
