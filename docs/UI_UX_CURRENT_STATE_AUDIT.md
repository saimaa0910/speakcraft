# SpeakCraft — UI/UX Current State Audit

**Audit Date:** 2026-11-18  
**Target Platform:** Native Android (Kotlin, Jetpack Compose, Material 3, Navigation Compose)  
**Package / Build:** `com.aistudio.speakcraft.qxmvkp` (`v1.0.0`, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36.1`)  
**Status Legend:** `Existing implementation` | `Verified defect` | `Proposed redesign`

---

## 1. Platform & Repository Scope Check

* **Existing implementation:** SpeakCraft is an Android-only native application (`app/src/main/java/com/example/...`) using Jetpack Compose, Material 3, Firebase Auth (`CredentialManager` Google Sign-In), Cloud Firestore, Firebase App Check, Firebase AI Logic (`gemini-3.8-flash`), CameraX, MediaRecorder/MediaPlayer, TextToSpeech/SpeechRecognizer, and DataStore Preferences.
* **Verified platform gap (`ISS-024`):** There are zero Electron, React, TypeScript, Windows Desktop, or Web SPA files in this repository. Desktop/Web specs in prior audit docs represent future companion targets, not an existing codebase.

---

## 2. Executive UX Diagnosis

The existing SpeakCraft UI demonstrates strong engineering fundamentals—real Google Sign-In, live Firestore listeners, Multimodal Gemini audio/video analysis, YouTube `@askvinh` ingestion, and an Editorial Studio palette (`MidnightNavy`, `WarmIvory`, `AmberSpotlight`, `AcousticTeal`). However, from a user's perspective, the current interface exhibits **high cognitive load, hidden high-frequency workflows, inconsistent visual hierarchy, and several critical interaction defects**:

| Severity | Finding ID | Summary | Source Evidence | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Critical** | `ISS-001` | 3-Stage Self-Audit Stage 1 Mute Pass plays audio-only `MediaPlayer` with no video viewport for CameraX/uploaded `.mp4` clips. | `SpeakingAndChallengesScreens.kt:256-291`, `AudioRecorderManager.kt:170-196` | `Verified defect` |
| **Critical** | `ISS-002` | `saveDailyActivity` increments `streakDays` unconditionally across multi-day gaps and hardcodes `+3L` practice minutes instead of actual delta. | `SpeakCraftRepository.kt:555-568` | `Verified defect` |
| **Critical** | `ISS-003` | `submitForAiAnalysis` emits `UiState.Success` even if Firestore `saveSpeakingSession` fails, hiding unsaved state. | `SpeakingAndLabsViewModel.kt:212-218` | `Verified defect` |
| **High priority** | `UI-IA-01` | `DashboardScreen` acts as a 22-tile directory wall (15 module cards + 7 utility cards) below 5 summary cards, overwhelming users on first launch. | `DashboardScreen.kt:158-370` | `Verified defect` |
| **High priority** | `UI-ONB-01` | `OnboardingAssessmentScreen` dumps 8 raw `Slider`s, 2 text fields, and a non-wrapping `Row` of 4 `FilterChip`s on one scroll screen (`ISS-019`). | `AuthAndOnboardingScreens.kt:205-280` | `Verified defect` |
| **High priority** | `ISS-018` | `AnalyticsScreen` (`Daily`/`Weekly`/`Monthly`) and `DashboardScreen` (`Weekly`/`Monthly`) date chips only mutate header text without filtering `sessions`. | `AnalyticsAndSettingsScreens.kt:63-124`, `DashboardScreen.kt:131,328-341` | `Verified defect` |
| **High priority** | `ISS-018B` | Both `articulation_lab` and `pronunciation` routes render `PronunciationLabScreen` identically, confusing learners expecting specialized Articulation/Enunciation vs Phonetic drills. | `AppNavigation.kt:478-483` | `Verified defect` |
| **High priority** | `ISS-019` | Destructive actions (`Delete Speaking Session History`, single session delete) execute immediately with zero confirmation dialog. | `AnalyticsAndSettingsScreens.kt:315-320`, `SpeakingAndChallengesScreens.kt:364-369` | `Verified defect` |
| **High priority** | `ISS-021` | `AiMentorScreen` displays only the latest single Q&A card (`mentorAnswer`) without multi-turn thread history or persistence across restarts. | `InteractiveCoachViewModel.kt:50-53,116-146`, `ConversationAndMentorScreens.kt:231-339` | `Verified defect` |
| **Medium priority** | `ISS-014` | Hardcoded hex colors (`Color(0xFFE2E8F0)`, `Color.White`, `Color(0xFFF87171)`) appear in Composables; `AndroidManifest.xml` & `themes.xml` still use `Theme.MyApplication`. | `DashboardScreen.kt:215,235,249`, `CameraVideoRecorder.kt:159`, `themes.xml:4` | `Verified defect` |
| **Medium priority** | `ISS-015` | `Type.kt` maps `Normal`, `SemiBold`, and `Bold` to single-weight (`400`) `.ttf` files, suppressing synthetic bold contrast in Compose. | `Type.kt:11-26` | `Verified defect` |
| **Medium priority** | `ISS-017` | `AppUpdateScreen` creates backups and lists them, but provides no Restore action in UI or `UpdateBackupManager`. | `AppUpdateScreen.kt:258-286`, `UpdateBackupManager.kt` | `Verified defect` |
| **Medium priority** | `ISS-020` | `AudioRecorderManager` stopwatch relies on coroutine delay/millis drift without reading actual `.m4a` duration via `MediaMetadataRetriever` on stop; `attachUploadedMedia` loads full stream bytes before checking `>19MB` (`ISS-005`). | `AudioRecorderManager.kt:89-163`, `SpeakingAndLabsViewModel.kt:104-124` | `Verified defect` |
| **Low priority** | `UI-AUTH-01` | `AuthScreen` runs silent `attemptAutoSignIn` without showing checking progress state to the user. | `AuthAndOnboardingScreens.kt:63-71` | `Proposed redesign` |
| **Low priority** | `UI-A11Y-01` | `LiveWaveformVisualizer` and `ScoreTrendSparkline` `Canvas` widgets lack accessibility semantics/descriptions; `reduced motion` preference is unchecked. | `CoachComponents.kt:147-258` | `Verified defect` |

---

## 3. Screen-by-Screen Friction Analysis (Verified Against Code)

### 3.1 Authentication & Onboarding (`AuthAndOnboardingScreens.kt`)
* **Strengths (`Existing implementation`):** Clean hero stage art (`img_onboarding_stage`), clear feature summary card, real `CredentialManager` Google Sign-In.
* **Friction (`Verified defect` / `Proposed redesign`):**
  1. Silent auto-sign-in gives no visual indicator (`Checking saved Google session...`).
  2. Onboarding is an intimidating single-screen wall of 8 sliders (`fluency`, `pronunciation`, `vocabulary`, `grammar`, `articulation`, `listening`, `confidence`, `dailyMins`) with no step progress, no level presets, and a horizontal `Row` of 4 `FilterChip`s that can clip on narrow `320dp` displays.

### 3.2 Navigation Shell (`AppNavigation.kt`)
* **Strengths (`Existing implementation`):** Adaptive `BoxWithConstraints` switches between `NavigationBar` (`<720dp`) and `NavigationRail` (`>=720dp`). Deep-linked routes (`module_lessons/{slug}`, `lesson_detail/{lessonId}`) work cleanly.
* **Friction (`Proposed redesign`):**
  1. Primary tabs are `[Home, Daily, Record, @askvinh, Mentor]`. Core daily workflows like **Learn / Modules** (`learning_paths`, `vocabulary`, 15 modules) and **Progress / Analytics** are buried inside the Home scroll instead of clean top-level hubs.
  2. Top bar lacks quick search / jump-to-skill affordance and stacks 2–3 right-side icon buttons (`Admin`, `Updates`, `Settings`) even when secondary screens are open.

### 3.3 Home Dashboard (`DashboardScreen.kt`)
* **Strengths (`Existing implementation`):** Action-first Recommendation hero banner (`RecommendationEngine`), update banner, 4 compact `MetricTile`s, sparkline trend card.
* **Friction (`Verified defect` / `Proposed redesign`):**
  1. Below the sparkline, all 15 communication modules + 7 utility tools render as a dense 22-tile wall with identical visual weight, forcing users to scan 22 cards to distinguish core speaking labs from system settings.
  2. `Weekly` / `Monthly` chips (`DashboardScreen.kt:334`) do not filter `sessions` by timestamp.

### 3.4 Speaking Studio & 3-Stage Audit (`SpeakingAndChallengesScreens.kt`, `CameraVideoRecorder.kt`)
* **Strengths (`Existing implementation`):** Pause/Resume mic recording, live amplitude waveform, in-app CameraX recording (`InAppCameraVideoDialog`), Photo Picker (`PickVisualMedia`), side-by-side prior session comparison, `FeedbackReportCard` withholding pronunciation when unassessable.
* **Friction (`Verified defect`):**
  1. **`ISS-001`:** When users record a CameraX video or pick an `.mp4`, selecting `Stage 1: Mute Pass` only plays audio muted via `MediaPlayer`—there is **no video surface** to inspect posture, facial expression, or gestures.
  2. **`ISS-003`:** If Firestore fails to save a freshly analyzed session, the screen shows `UiState.Success` silently without warning or `"Retry Cloud Sync"`.
  3. **`ISS-019`:** Tapping delete on a past recording deletes it and its Firestore document immediately with no confirmation dialog.
  4. **`ISS-005` / `ISS-020`:** `attachUploadedMedia` reads the entire `InputStream` into memory before checking size; `stopRecording` relies on loop ticks rather than inspecting actual file duration.

### 3.5 Specialized Labs (`LabsAndTrainingScreens.kt`, `LessonAndModulesScreens.kt`)
* **Strengths (`Existing implementation`):** IPA minimal pairs, TTS `0.75x`/`1.0x`, shadowing scripts with pause markers, PREP/CLEAR grammar drills, public speaking prompts, inline recording inside `LessonDetailScreen`, creator attribution badges (`LessonAttributionBadge`).
* **Friction (`Proposed redesign`):**
  1. `articulation_lab` and `pronunciation` open identical screens (`PronunciationLabScreen`) with no Articulation/Enunciation specialization parameter or filter preset (`ISS-018B`).
  2. No quick step guidance showing "1. Listen -> 2. Record -> 3. Compare AI" workflow clarity inside each lab card.

### 3.6 Vocabulary Builder (`VocabularyScreen.kt`)
* **Strengths (`Existing implementation`):** 4 clean tabs (`Words`, `Flashcards`, `AI Quiz`, `Notebook`), IPA + TTS pronunciation, collocations, spaced repetition due filtering.
* **Friction (`Proposed redesign`):**
  1. Needs search bar in `Words` tab, `FlowRow` level chips, flashcard progress indicator (`Card X of Y due`), and haptic/animated flip feedback on `vocab_flashcard`.

### 3.7 Conversation Simulator & AI Mentor (`ConversationAndMentorScreens.kt`)
* **Strengths (`Existing implementation`):** 13 realistic scenarios, uninterrupted natural conversation flow, on-demand end-of-session coaching debrief, voice dictation + TTS partner playback.
* **Friction (`Verified defect` / `Proposed redesign`):**
  1. **`ISS-021`:** `AiMentorScreen` overwrites a single response card every time the user asks a question—no scrollable chat thread, no follow-up context continuity, and no local persistence when navigating away.
  2. Changing scenario chip mid-conversation immediately resets `chatTurns` without warning when active user turns exist.

### 3.8 Analytics, Settings, App Updates & Admin (`AnalyticsAndSettingsScreens.kt`, `AppUpdateScreen.kt`, `AdminDashboardScreen.kt`)
* **Strengths (`Existing implementation`):** Separates objective speech metrics from subjective self-confidence rating; Keystore AES-GCM encryption for optional YouTube key; SemVer + SHA-256 APK verifier; RBAC-gated Admin Console.
* **Friction (`Verified defect`):**
  1. **`ISS-018`:** `Daily`/`Weekly`/`Monthly` chips on Analytics don't filter `sessions` by `createdAt`.
  2. **`ISS-019`:** `Delete Speaking Session History` wipes all Firestore sessions and local audio files on a single tap without confirmation.
  3. **`ISS-017`:** Backup snapshots are listed as raw text lines without a `"Restore Preferences"` action.
