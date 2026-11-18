# SpeakCraft — UI/UX Testing and Validation Report

**Revision Date:** 2026-11-18  
**Target Build:** `com.aistudio.speakcraft.qxmvkp` (`debug` APK + JVM/Robolectric unit test suite)  
**Verification Commands Executed:**
1. `gradle :app:testDebugUnitTest --console=plain` -> **`BUILD SUCCESSFUL`** (`41 / 41` tests passed, `0` skipped, `0` failures, `0` errors)
2. `compile_applet` -> **`Build succeeded - the applet is compiled`**

---

## 1. Environment & Verification Scope Notice

* **Automated JVM & Robolectric UI Testing (Verified):** All Kotlin unit tests, data-helper tests, update/backup snapshot tests, YouTube RSS/TimedText parser tests, and **Jetpack Compose Robolectric Critical User Journey (CUJ) tests** compiled and executed locally against the final codebase.
* **Physical Device / Streaming Emulator Screenshots (Unavailable in Agent Container):** As noted in the build environment constraints, no live Android Emulator or `adb` bridge is accessible to the agent container. Visual device inspection, CameraX live preview framing, real microphone hardware capture, and physical screen-reader TalkBack sweeps were not executed by the agent and remain for interactive validation on the streaming emulator.

---

## 2. Automated Test Suite Results (Verified)

| Test Class | Tests Run | Passed | Failed | Skipped | Focus Verified |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `com.example.CurriculumAndRecommendationTest` | 4 | **4** | 0 | 0 | 15 modules, unique slugs, recommendation targeting, `PENDING_TRANSCRIPT` integrity |
| `com.example.ExampleRobolectricTest` | 1 | **1** | 0 | 0 | Robolectric Android context resource resolution (`app_name`) |
| `com.example.ExampleUnitTest` | 1 | **1** | 0 | 0 | Baseline JVM test runner verification |
| `com.example.SemVerAndUpdateTest` | 4 | **4** | 0 | 0 | SemVer comparison, SHA-256 verification & tamper rejection, backup recovery marker, release channel evaluation & HTTPS enforcement |
| `com.example.SpeakCraftComposeCujTest` | 15 | **15** | 0 | 0 | Compose UI CUJs across Onboarding, Dashboard, Speaking Lab, Feedback, Labs, Mentor, Analytics, Settings, Updates, Admin, 24-Feature Tutorials & `TutorialOverlay` |
| `com.example.SpeakCraftTabletLayoutTest` | 6 | **6** | 0 | 0 | Expanded tablet (`w1024dp-h768dp`) layout verification: side-by-side `TutorialOverlay`, 4-up Dashboard metrics & hero-beside-routine, 2-column `AppUpdateScreen`, `AnalyticsScreen`, `SettingsScreen`, and 2-pane `SpeakingLabScreen` studio |
| `com.example.StreakAnalyticsAndBackupTest` | 7 | **7** | 0 | 0 | Consecutive-day streak math, minutes delta, period filtering, backup restore, 24-feature `FeatureTutorialRegistry`, `TutorialOverlayCatalog`, and per-user `TutorialStateManager` DataStore isolation |
| `com.example.YouTubeParsingTest` | 3 | **3** | 0 | 0 | TimedText transcript XML parsing, YouTube URL extraction (watch/shorts/playlist/channel), RSS metadata parsing |
| **Total** | **41** | **41** | **0** | **0** | **100% Pass Rate** |

---

## 3. Detailed Critical User Journey (CUJ) & Defect Verification

### A. Unit & Business Logic Verification (`StreakAnalyticsAndBackupTest` & Existing Unit Tests)
1. **`ISS-002` Consecutive-Day Streak & Minutes Delta:** Verified `StreakAndAnalyticsHelper.computeUpdatedStreak(...)` increments only when `previousEpochDay == todayEpochDay - 1L`, preserves streak on same-day completions, resets to `1` after multi-day gaps, and `computeMinutesDelta(...)` never double-counts or returns negative values.
2. **`ISS-018` Time-Period Session Filtering:** Verified `StreakAndAnalyticsHelper.filterSessionsByPeriod(...)` accurately filters `SpeakingSession` records across `"Daily"` (1-day window), `"Weekly"` (7-day window), and `"Monthly"` (30-day window).
3. **`ISS-017` Backup Snapshot Creation & Restore:** Verified `UpdateBackupManager` creates versioned JSON snapshots and `restoreBackupSnapshot(...)` restores persisted theme, voice speed, and update channel preferences cleanly.
4. **24-Feature Interactive Tutorial Coverage (`FeatureTutorialRegistry`):** Verified all 24 routes (`auth`, `onboarding`, `dashboard`, `daily_practice`, `learning_paths`, `vocabulary`, `speaking_lab`, `challenges`, `articulation_lab`, `pronunciation`, `voice_training`, `conversation`, `listening_lab`, `grammar_speaking`, `public_speaking`, `real_world_tasks`, `module_lessons`, `lesson_detail`, `youtube_library`, `analytics`, `ai_mentor`, `settings`, `app_updates`, `admin_console`) contain non-blank summaries, numbered `howToUseSteps`, measurable `whatImproves` outcomes, and `coachTip` guidance.

### B. Jetpack Compose UI & Interaction Verification (`SpeakCraftComposeCujTest`)
1. **3-Step Guided Onboarding Wizard:** Verified progression across Step 1 (`Identity & Level`), Step 2 (`Goal & Pace`), and Step 3 (`Skill Baseline`), confirming completion callback triggers with calibrated profile state.
2. **Action-First Dashboard & Search (`dashboard_module_search`):** Verified Recommended Next Action banner, update availability card, and live module directory search filtering.
3. **3-Stage Video & Vocal Self-Audit Studio (`ISS-001`):** Verified `SpeakingLabScreen` renders `VideoReviewPlayerCard` for recorded `.mp4` clips and supports toggling between `"Stage 1: Mute Pass"`, `"Stage 2: Vocal (Eyes Closed)"`, and `"Stage 3: Full Review"`.
4. **Honest AI Feedback Integrity:** Verified `FeedbackReportCard` omits fabricated pronunciation bars when the backend returns unassessable (`-1`) metrics.
5. **Specialization of Articulation vs. Pronunciation Labs (`ISS-018B`):** Verified `PronunciationLabScreen` renders `"Articulation & Enunciation Lab"` with `"Difficult Sounds"` focused defaults when launched from `articulation_lab`, distinct from `"Phonetic & Intonation Lab"` (`pronunciation`).
6. **Multi-Turn Persistent AI Mentor (`ISS-021`):** Verified `AiMentorScreen` renders multi-turn user and assistant chat bubbles (`mentor_history_{uid}.json` backing).
7. **Period-Filtered Analytics & Honest Empty State (`ISS-018`):** Verified `AnalyticsScreen` switches between `"Weekly"` and `"Daily"` and renders an informative empty state when no sessions exist inside the `"Daily"` window.
8. **Destructive Action Guards (`ISS-017`, `ISS-019`):**
   * Verified `SettingsScreen` (`clear_history_button`) opens `ConfirmDestructiveDialog` prior to executing `clearHistory`.
   * Verified `AppUpdateScreen` renders per-snapshot `"Restore"` triggers with confirmation gating.
9. **Access Control & Lesson Attribution:** Verified `AdminDashboardScreen` blocks non-admin access and `LessonDetailScreen` displays creator attribution badge (`@askvinh`) + marks lessons completed.
10. **Per-Feature Interactive Tutorial System & Per-User `TutorialStateManager`:**
    * Verified `FeatureGuideStrip` renders collapsed by default cleanly on screens and expands on click to reveal `"How to use"` steps, `"What improves by doing this"`, and `"Coach tip"`.
    * Verified `"Explore Tutorials for All 24 Features & Benefits"` (`browse_all_tutorials_button`) on the Dashboard and the universal TopAppBar `?` help icon (`feature_tutorial_topbar_button`) open `FeatureTutorialDialog` with searchable directory across all 24 features.
    * Verified `TutorialOverlay` steps cleanly through Dashboard, Speaking Lab, and AI Mentor highlighting Benefits and Usage Steps, persists completion per `userId` via `TutorialStateManager` (`speakcraft_tutorial_state` DataStore), and shows first-use overlays once per feature per user.
11. **One-Tap Check for Updates Auto-Download & Tablet Responsiveness (`w1024dp-h768dp`):**
    * Verified `ReleaseUpdateRepository` evaluates enabled channel releases and enforces HTTPS/SHA-256 verification cleanly, and clicking `"Check for Updates Now"` on `AppUpdateScreen` triggers `UpdateAndAdminViewModel.checkForUpdatesManual(...)` to fetch enabled Firestore releases and automatically download & verify updates.
    * Verified `w1024dp-h768dp` tablet layout suite confirms side-by-side `TutorialOverlay` Benefits & Usage panels, 4-up Dashboard metrics, hero-beside-routine placement, and 2-column/2-pane layouts on `AppUpdateScreen`, `AnalyticsScreen`, `SettingsScreen`, and `SpeakingLabScreen`.

---

## 4. Build Health & Compiler Observations

* **AndroidManifest & Theme Resolution Fix:** During final build validation, `AndroidManifest.xml` line 30 still referenced `@style/Theme.MyApplication` on `.MainActivity` after `themes.xml` defined `Theme.SpeakCraft`. Updated `.MainActivity` to `@style/Theme.SpeakCraft`; `processDebugResources` passed cleanly thereafter.
* **Missing Import Fix:** Added missing `androidx.compose.foundation.layout.height` import in `AppUpdateScreen.kt` resolved during Kotlin compilation.
* **Remaining Non-Blocking Compiler Warnings (Informational):**
  * `SpeakCraftRepository.kt`: 9 unchecked cast warnings (`Map<String, Any?>` to `Map<String, Any>`) pre-existing from Firestore document reads.
  * `FeatureTutorialComponents.kt:197`: `Icons.Filled.TrendingUp` deprecation notice suggesting `Icons.AutoMirrored.Filled.TrendingUp` (non-blocking, renders correctly).

---

## 5. Manual Interactive Verification Checklist (For Streaming Emulator / Device)

Before shipping a release build, verify on a physical device or streaming Android emulator:
1. CameraX video recording (`camera_toggle_button`) + muted `.mp4` playback in `VideoReviewPlayerCard` (`Stage 1: Mute Pass` volume = `0f` vs. `Stage 3` volume = `1f`).
2. Hardware microphone waveform responsiveness (`LiveWaveformVisualizer`) and real `MediaMetadataRetriever` `.m4a` duration capture (`ISS-020`).
3. Google Credential Manager sign-in flow on `AuthScreen` and offline/retry behavior on `CloudSyncWarningBanner` (`ISS-003`).
4. TalkBack sweep across Dashboard, Speaking Lab, 3-Step Onboarding, and `FeatureTutorialDialog` verifying `heading()` semantics and minimum `48dp` interactive targets.
