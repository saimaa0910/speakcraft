# SpeakCraft — UI/UX & Engineering Changelog

**Revision Date:** 2026-11-18  
**Release Scope:** SpeakCraft World-Class UI/UX Transformation + P0/P1 Reliability Fixes + Universal 24-Feature Tutorial & Benefits System  

---

## 1. Visual Design System & Accessibility Foundations

* **Tokenized Color System (`ui/theme/Color.kt`, `ui/theme/Theme.kt`):**
  * Added `SpeakCraftColors` `CompositionLocal` (`MaterialTheme.speakCraftColors`) providing semantic tokens (`success`, `warning`, `info`, `accentTeal`, `accentAmber`, `streakFlame`, subtle containers, and borders) across Light and Dark themes.
  * Eliminated ad-hoc hardcoded `Color(0x...)` literals across screen composables in favor of `MaterialTheme.colorScheme` and `MaterialTheme.speakCraftColors`.
  * Renamed XML/Manifest application and activity theme from `Theme.MyApplication` to `Theme.SpeakCraft` (`res/values/themes.xml`, `AndroidManifest.xml`).
* **Typography Weight Restoration (`ui/theme/Type.kt`):**
  * Fixed font weight mapping (`ISS-015`) so `FontWeight.Normal`, `Medium`, `SemiBold`, and `Bold` render distinct visual hierarchy across bundled TTF fonts.
* **Unified Spacing & Touch Targets (`ui/theme/Spacing.kt`, `CoachComponents.kt`):**
  * Standardized `4dp`/`8dp` spacing scale (`SpeakCraftSpacing`), `maxContentWidth` adaptive constraints (`600dp`–`760dp`), and `48dp` minimum interactive component heights.
  * Added `Modifier.semantics { heading() }` on `SectionHeader`, `contentDescription` / reduced-motion checks (`rememberReducedMotionEnabled()`) on `LiveWaveformVisualizer` and `ScoreTrendSparkline`, plus reusable `ConfirmDestructiveDialog`, `CloudSyncWarningBanner`, and `StepProgressIndicator`.

---

## 2. Core Reliability & Data Integrity Fixes

* **Stage 1 Muted Video Review Studio (`ISS-001`):**
  * Added `VideoReviewPlayerCard` using native Android `VideoView` with `MediaPlayer` dynamic volume control (`volume = 0f` for Stage 1 Mute Pass, `1f` for Stage 2/3) and synchronized scrubbing/replay for recorded CameraX or uploaded `.mp4` videos.
* **Accurate Calendar Streak & Duration Math (`ISS-002`, `ISS-020`, `ISS-028`):**
  * Replaced flawed non-matching-date streak incrementing and hardcoded `+= 3` minute increments with `StreakAndAnalyticsHelper.computeUpdatedStreak` and `computeMinutesDelta`.
  * Fixed `AudioRecorderManager` recording timer to use monotonic `SystemClock.elapsedRealtime()` and verified `.m4a` duration via `MediaMetadataRetriever`.
  * Dynamic `todayDateKey()` / `todayEpochDay()` computation in `MainCoachViewModel`.
* **Cloud Sync Transparency & Upload Safety (`ISS-003`, `ISS-005`):**
  * `SpeakingAndLabsViewModel` now surfaces Firestore write failures cleanly via `cloudSyncError` + `CloudSyncWarningBanner` with a `retryCloudSync()` action instead of masking failures as `UiState.Success`.
  * Pre-checks `OpenableColumns.SIZE` against the `19 MB` multimodal inline threshold before reading uploaded audio/video bytes into memory.
* **Persistent Multi-Turn AI Mentor (`ISS-021`):**
  * Upgraded `InteractiveCoachViewModel` and `AiMentorScreen` from single Q&A boxes to scrollable multi-turn chat bubbles persisted per-user (`mentor_history_{uid}.json`) with confirmation dialog on `"Clear"`.
* **Backup Snapshot Restoration (`ISS-017`):**
  * Added `UpdateBackupManager.restoreBackupSnapshot(...)`, `UpdateAndAdminViewModel.restoreBackup(...)`, and per-snapshot `"Restore"` action + confirmation dialog in `AppUpdateScreen`.

---

## 3. Screen-by-Screen UI/UX Redesign

* **Authentication & 3-Step Guided Onboarding (`AuthAndOnboardingScreens.kt`):**
  * Added active session-checking feedback pill on `AuthScreen`.
  * Rebuilt `OnboardingAssessmentScreen` into a 3-step guided wizard (`StepProgressIndicator`, `BackHandler`, `FlowRow` skill chips, preset calibration baselines, and goal/pace selectors).
* **Action-First Home Dashboard (`DashboardScreen.kt`):**
  * Redesigned around Today's Focus recommendation, Routine Progress bar, 4 Quick Action Hubs (`Speaking Studio`, `Daily Practice`, `AI Roleplay`, `Analytics`), searchable module directory (`dashboard_module_search`) with `FlowRow` category chips, real `Weekly`/`Monthly` filtering (`ISS-018`), and quick tutorial directory access.
* **Speaking Lab, Vocabulary, Labs & Analytics (`SpeakingAndChallengesScreens.kt`, `VocabularyScreen.kt`, `LabsAndTrainingScreens.kt`, `ConversationAndMentorScreens.kt`, `AnalyticsAndSettingsScreens.kt`):**
  * Added 3-step studio clarity, Stage 1/2/3 audit chips, `ConfirmDestructiveDialog` on session delete (`SpeakingLabScreen`), `Clear History` (`SettingsScreen`), and scenario switching (`ConversationSimulatorScreen`).
  * Differentiated `articulation_lab` (`Articulation & Enunciation Lab` defaulting to `"Difficult Sounds"`) from `pronunciation` (`Phonetic & Intonation Lab`) (`ISS-018B`).
  * Added live word search, level filters, and `"Card X of Y due today"` counter with animated flip card in `VocabularyScreen`.
  * Wired real timestamp window filtering (`Daily` / `Weekly` / `Monthly`) with WPM averages and honest empty states in `AnalyticsScreen` (`ISS-018`).

---

## 4. Universal Per-Feature Tutorial & Improvement Guide System

* **All-24-Feature Tutorial Registry (`data/model/FeatureTutorialRegistry.kt`):**
  * Authored comprehensive, practical guides for all **24 routes and screens** (`auth`, `onboarding`, `dashboard`, `daily_practice`, `learning_paths`, `vocabulary`, `speaking_lab`, `challenges`, `articulation_lab`, `pronunciation`, `voice_training`, `conversation`, `listening_lab`, `grammar_speaking`, `public_speaking`, `real_world_tasks`, `module_lessons`, `lesson_detail`, `youtube_library`, `analytics`, `ai_mentor`, `settings`, `app_updates`, `admin_console`).
  * Every guide documents:
    1. **Feature Purpose Summary**
    2. **How to Use (Numbered Step-by-Step Instructions)**
    3. **What Will Improve By Doing This (Measurable Communication Outcomes)**
    4. **Coach Tip**
* **Inline & Global Access (`ui/components/FeatureTutorialComponents.kt`, `AppNavigation.kt`):**
  * Embedded `FeatureGuideStrip(featureKey)` across all 13 UI screen files (collapsible inline strip that keeps interfaces uncluttered while making instructions one tap away).
  * Added universal TopAppBar Help (`?`) button (`feature_tutorial_topbar_button`) opening `FeatureTutorialDialog` with two tabs: `"Guide Detail"` (context-aware for the active screen) and `"All Features (24)"` (searchable directory with direct `"Open"` jump buttons).
  * Added `"Explore Tutorials for All 24 Features & Benefits"` (`browse_all_tutorials_button`) on the Home Dashboard for instant discovery.

* **Per-User `TutorialStateManager` & `TutorialOverlay` (`data/preferences/TutorialState.kt`, `ui/components/TutorialOverlay.kt`, `AppNavigation.kt`):**
  * Added `TutorialState` and DataStore-backed `TutorialStateManager` (`speakcraft_tutorial_state`) keyed per `userId` (`completed_tutorials_<uid>`, `first_use_tutorials_enabled_<uid>`) so first-use `TutorialOverlay` tours and spotlights run once per feature per user without cross-account leakage.
  * Updated `TutorialOverlay` to use a sibling scrim `Box` behind the spotlight card (preserving TalkBack/Compose semantic bounds for benefits and usage panels) and side-by-side panels on wide screens (`availableWidth >= 720.dp`).
* **One-Tap Check for Updates Auto-Download (`SpeakCraftRepository.kt`, `UpdateAndAdminViewModel.kt`, `AppUpdateScreen.kt`, `AppNavigation.kt`):**
  * Added `fetchEnabledReleasesOnce()` and `checkForUpdatesManual(prefs)` with `isCheckingForUpdates` indicator so clicking Check for Updates (from Settings or `AppUpdateScreen`) fetches enabled releases from Firestore and automatically downloads & SHA-256 verifies any newer update.
* **Adaptive Tablet Responsiveness (`ui/theme/Spacing.kt`, `AppNavigation.kt`, All Screen Composables):**
  * Added `AdaptiveWindowTier` (`COMPACT`, `MEDIUM >= 600dp`, `EXPANDED >= 840dp`) via `BoxWithConstraints`, switched navigation to `NavigationRail` at `>= 600dp`, expanded container widths up to `1160.dp`, enabled 4-up metric rows + 3-column module grids, and implemented 2-pane studio / 2-column layouts across screens.

---

## 5. Verification Summary

* **`gradle :app:testDebugUnitTest --console=plain`:** `41 / 41` unit & Robolectric Compose tests passed (`0` failures, `0` skipped), including `SpeakCraftTabletLayoutTest` (`w1024dp-h768dp`).
* **`compile_applet`:** Debug APK compiled successfully after all edits.
