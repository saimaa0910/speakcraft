# SpeakCraft — Interaction & Motion Guidelines

**Revision Date:** 2026-11-18  
**Design Principle:** *Tactile Clarity* — animations communicate state transitions (recording, pause, step completion, card reveal), never delay speech capture, and automatically respect system **Reduced Motion** settings.

---

## 1. Motion Timing & Easing Tokens

| Motion Token | Duration | Easing / Spec | Application |
| :--- | :---: | :--- | :--- |
| `QuickFeedbackMs` | `150ms` | `FastOutSlowInEasing` | Filter chip selection, toggle checks, card border highlights |
| `StandardTransitionMs` | `260ms` | `FastOutSlowInEasing` | Onboarding step slide/fade transitions, expandable program plans, confirmation dialogs |
| `CardFlipMs` | `320ms` | `FastOutSlowInEasing` | Vocabulary flashcard reveal transition (`AnimatedContent`) |
| `WaveformPulseMs` | `60fps` frame-driven | `withFrameMillis` (`amplitude` smoothed) | `LiveWaveformVisualizer` active mic metering (pauses cleanly when `isPaused = true`) |
| `SkeletonShimmerMs` | `850ms` | `LinearEasing` reverse loop | `ShimmerSkeletonCard` during Gemini multimodal analysis |

---

## 2. Reduced Motion Accessibility Support (`Proposed redesign`)

* Read `Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)` via a reusable `rememberReducedMotionEnabled()` helper in `CoachComponents.kt`.
* When `reducedMotion == true` (`scale == 0f`):
  * `ShimmerSkeletonCard` renders a static low-contrast placeholder card instead of pulsing opacity loops.
  * `LiveWaveformVisualizer` renders static amplitude bars updated cleanly without sinusoidal phase sweeping.
  * Screen and onboarding step transitions collapse to `tween(0)` / instant crossfade.

---

## 3. Micro-Interactions & Haptic Feedback

Using Compose `LocalHapticFeedback.current` (`HapticFeedbackType.LongPress` / `TextHandleMove`):
1. **Start / Stop Mic Recording:** Subtle haptic pulse confirming audio capture began or finalized without forcing eyes onto the timer.
2. **Pause / Resume Recording:** Crisp button color shift (`AmberSpotlight` <-> `AcousticTeal`) + waveform freezes at low uniform baseline so paused state is unmistakable.
3. **Onboarding Step Advance & Lesson Completion:** Progress bar animates smoothly via `animateFloatAsState` (`260ms`) + light haptic confirmation when marking a lesson or daily step complete.
4. **Vocabulary Flashcard Flip:** `AnimatedContent` transition between prompt side (word + IPA) and meaning/example side, paired with `TTS` speak button.

---

## 4. Microcopy & Error Tone Rules

* **Actionable, Non-Blaming Errors:** Never show raw exception class names to learners. Explain *what happened* and *what they can tap next* (e.g., `"Analyzed locally, but cloud sync failed. Tap Retry Cloud Sync so your report isn't lost."`).
* **Honest Empty Windows:** When `Daily (24h)` analytics has 0 sessions, state `"No recordings logged in the last 24 hours"` with a 1-tap CTA to open `Record & Review`, rather than showing stale all-time scores under a `"Daily"` heading (`ISS-018`).
* **Safety Friction Only on Permanent Loss:** Confirm before deleting speaking history, deleting a session, restoring a backup over current preferences, or resetting an active multi-turn conversation; keep harmless actions (navigating, checking updates, toggling daily checklist) instant.
