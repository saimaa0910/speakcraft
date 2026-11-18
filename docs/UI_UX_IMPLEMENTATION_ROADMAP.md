# SpeakCraft — UI/UX Implementation Roadmap

**Revision Date:** 2026-11-18  
**Execution Principle:** Work incrementally in verified stages—preserve all 22 navigation routes, Firebase Auth/Firestore security rules, Gemini AI schemas, and existing Robolectric tests (`SpeakCraftComposeCujTest`) while fixing verified defects and elevating every screen.

---

## Stage 1 — Design System, Typography & Shared Primitives (`Critical` / `Medium priority`)
* **Files:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/theme/Spacing.kt` (new), `res/values/themes.xml`, `AndroidManifest.xml`, `ui/components/CoachComponents.kt`.
* **Deliverables:**
  1. Add `SpeakCraftColors` `CompositionLocal` (`MaterialTheme.speakCraftColors`) and eliminate hardcoded hex `Color(...)` literals across screens (`ISS-014`).
  2. Rename `Theme.MyApplication` -> `Theme.SpeakCraft` (`ISS-014`).
  3. Fix `Type.kt` `FontFamily` definitions to restore Bold/SemiBold weight differentiation (`ISS-015`).
  4. Add `Spacing.kt` (`4/8dp` scale + `48.dp` min touch target).
  5. Upgrade `CoachComponents.kt`: TalkBack `heading()` on `SectionHeader`, `semantics` + `ReducedMotion` support on `LiveWaveformVisualizer` & `ScoreTrendSparkline`, add `ConfirmDestructiveDialog`, `CloudSyncWarningBanner`, `StepProgressIndicator`, and `VideoReviewPlayerCard` (real `VideoView` playback for `.mp4` Stage 1/2/3 audit — `ISS-001`).

## Stage 2 — Data Integrity, Streak Math, Recorder & ViewModel Reliability (`Critical` / `High priority`)
* **Files:** `data/firebase/SpeakCraftRepository.kt`, `data/media/AudioRecorderManager.kt`, `data/update/UpdateBackupManager.kt`, `ui/viewmodel/MainCoachViewModel.kt`, `ui/viewmodel/SpeakingAndLabsViewModel.kt`, `ui/viewmodel/InteractiveCoachViewModel.kt`, `ui/viewmodel/UpdateAndAdminViewModel.kt`.
* **Deliverables:**
  1. Fix `SpeakCraftRepository.saveDailyActivity` consecutive-day calendar streak validation and real `minutesDelta` computation (`ISS-002`).
  2. Fix `AudioRecorderManager` stopwatch using `SystemClock.elapsedRealtime()` + `MediaMetadataRetriever` actual `.m4a` duration on stop (`ISS-020`).
  3. Fix `SpeakingAndLabsViewModel`: pre-check `OpenableColumns.SIZE` before `readBytes()` (`ISS-005`), track `cloudSyncError` + `unsavedSession` with `retryCloudSync()` (`ISS-003`), and play/pause video/audio cleanly.
  4. Add `UpdateBackupManager.restoreBackupSnapshot` + `UpdateAndAdminViewModel.restoreBackup(...)` (`ISS-017`).
  5. Upgrade `InteractiveCoachViewModel` with scrollable multi-turn persisted `mentorMessages` (`mentor_history_{uid}.json`) + multi-turn context (`ISS-021`).
  6. Make `MainCoachViewModel` compute `currentTodayDateKey()` dynamically (`ISS-028`).

## Stage 3 — Auth, 3-Step Onboarding & Action-First Home Dashboard (`High priority`)
* **Files:** `ui/screens/AuthAndOnboardingScreens.kt`, `ui/screens/DashboardScreen.kt`, `ui/AppNavigation.kt`.
* **Deliverables:**
  1. Visible session-check indicator on `AuthScreen`.
  2. 3-Step Guided Onboarding Wizard (`Step 1: Identity & Level`, `Step 2: Goal & Pace`, `Step 3: Skill Baseline`) with `FlowRow`, `BackHandler`, and smart level presets (`ISS-019`).
  3. Decluttered, searchable Home `DashboardScreen` with 4 Core Hub cards, real `Weekly`/`Monthly` timestamp filtering (`ISS-018`), and categorized module tabs.

## Stage 4 — Studio Video Review, Labs Specialization, Mentor Chat & Confirmations (`Critical` / `High priority`)
* **Files:** `SpeakingAndChallengesScreens.kt`, `LabsAndTrainingScreens.kt`, `VocabularyScreen.kt`, `ConversationAndMentorScreens.kt`, `AnalyticsAndSettingsScreens.kt`, `AppUpdateScreen.kt`, `CameraVideoRecorder.kt`.
* **Deliverables:**
  1. Wire `VideoReviewPlayerCard` into `SpeakingLabScreen` for Stage 1 Muted Video Audit + Stage 2 Eyes-Closed Vocal Pass + Stage 3 Full AV Review (`ISS-001`), plus `CloudSyncWarningBanner` (`ISS-003`) and `ConfirmDestructiveDialog` on session delete (`ISS-019`).
  2. Differentiate `articulation_lab` (`Articulation & Enunciation Lab`) vs `pronunciation` (`Phonetic & Intonation Lab`) in `PronunciationLabScreen` (`ISS-018B`) and add 3-step guided drill strips + `FlowRow`.
  3. Add word search & due-card counter to `VocabularyScreen`.
  4. Upgrade `AiMentorScreen` to multi-turn chat bubbles + thread persistence (`ISS-021`), and add scenario-switch guard in `ConversationSimulatorScreen`.
  5. Wire real `Daily`/`Weekly`/`Monthly` timestamp filtering in `AnalyticsScreen` (`ISS-018`), confirmation dialog on `SettingsScreen` Clear History (`ISS-019`), and Backup Restore button + dialog in `AppUpdateScreen` (`ISS-017`).

## Stage 5 — Verification & Final Reporting (`Critical`)
* Expand unit + Robolectric Compose CUJ tests covering streak math, period filtering, backup restore, onboarding 3-step wizard, destructive dialogs, video review card, cloud-sync retry, and multi-turn mentor UI.
* Run `gradle :app:testDebugUnitTest` and `compile_applet`.
* Document verified test results in `UI_UX_TESTING_AND_VALIDATION_REPORT.md` and `UI_UX_CHANGELOG.md`, and update `README_DOCUMENTATION_INDEX.md`.
