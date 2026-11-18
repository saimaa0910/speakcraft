# 05 — Complete Feature Documentation

**Revision Date:** 2026-11-18  
**Product:** SpeakCraft — AI Communication Skills Coach (`@askvinh` curriculum)  
**Scope:** Complete inventory of all 40 feature domains requested by the product specification, rigorously tagged with actual implementation status (`Implemented`, `Partially implemented`, `Broken`, `Not implemented`, `Proposed`).

---

## Table of Contents

1. [Feature Status Overview & Taxonomy](#feature-status-overview--taxonomy)
2. [Feature Matrix Across All 40 Domains (FEATS 001–040)](#feature-matrix-across-all-40-domains)
3. [Detailed Feature Specifications (15-Field Standard Template)](#detailed-feature-specifications)
   - [Domain Group 1: Auth, Onboarding, Navigation & Personalization (`FEAT-001`..`FEAT-006`)](#domain-group-1-auth-onboarding-navigation--personalization)
   - [Domain Group 2: Curriculum, Lessons, Programs & `@askvinh` YouTube Intelligence (`FEAT-007`..`FEAT-015`)](#domain-group-2-curriculum-lessons-programs--askvinh-youtube-intelligence)
   - [Domain Group 3: Speaking Studio, Audio/Video Capture & Coaching Feedback (`FEAT-016`..`FEAT-023`)](#domain-group-3-speaking-studio-audiovideo-capture--coaching-feedback)
   - [Domain Group 4: Vocabulary, Listening, Grammar, Challenges & Real-World Tasks (`FEAT-024`..`FEAT-030`)](#domain-group-4-vocabulary-listening-grammar-challenges--real-world-tasks)
   - [Domain Group 5: Analytics, Notifications, Updater, Admin & Multi-Platform (`FEAT-031`..`FEAT-040`)](#domain-group-5-analytics-notifications-updater-admin--multi-platform)
4. [Product Release Roadmap (MVP / V1 / V2 / Future)](#product-release-roadmap)
5. [Cross-References & Open Questions](#cross-references--open-questions)

---

## Feature Status Overview & Taxonomy

* **Implemented:** End-to-end UI, ViewModel, persistence/AI wiring exists in the Android repository today.
* **Partially implemented:** Core workflow works, but a key sub-capability or multi-device requirement is incomplete or broken.
* **Broken:** Implemented UI control fails its documented purpose (`ISS-001`, `ISS-018`).
* **Not implemented / Proposed:** Specified in product/target architecture (e.g., Windows Electron desktop, Web SPA, push reminders, social/peer sharing) with zero implementation in the current repository.

---

## Feature Matrix Across All 40 Domains

| Feature ID | Category / Name | Current Status | Primary Source Location |
| :--- | :--- | :--- | :--- |
| `FEAT-001` | Google Sign-In & Auto Session Restore | **Implemented** | `AuthHelper.kt`, `AuthAndOnboardingScreens.kt` |
| `FEAT-002` | Onboarding Calibration & Baseline Sliders | **Implemented** | `OnboardingAssessmentScreen` |
| `FEAT-003` | Adaptive Navigation Shell (`Bar` / `Rail`) | **Implemented** | `AppNavigation.kt:347-409` |
| `FEAT-004` | Editorial Studio Theme & Dark/Light Toggle | **Implemented** | `Theme.kt`, `Color.kt`, `SettingsScreen.kt` |
| `FEAT-005` | Next-Action Recommendation Engine | **Implemented** | `RecommendationEngine.kt`, `DashboardScreen.kt` |
| `FEAT-006` | User Profile Editing & Preferences | **Partially implemented** | `AppPreferencesRepository.kt`, `ISS-022` |
| `FEAT-007` | 15 Communication Modules Directory | **Implemented** | `CurriculumCatalog.kt`, `DashboardScreen.kt` |
| `FEAT-008` | 20 Communication Category Taxonomy | **Implemented** | `CurriculumCatalog.allCategories` |
| `FEAT-009` | Interactive Lesson Viewer & Notes | **Implemented** | `LessonAndModulesScreens.kt` |
| `FEAT-010` | 30 / 60 / 90-Day Structured Programs | **Implemented** | `DailyAndProgramsScreens.kt` |
| `FEAT-011` | YouTube Channel/Playlist/Video Audit | **Implemented** | `YouTubeIntelligenceClient.kt`, `YouTubeIntelligenceScreen.kt` |
| `FEAT-012` | Automated Transcript & Timestamp Extraction | **Implemented** | `YouTubeIntelligenceClient.kt` (`timedtext`) |
| `FEAT-013` | AI Lesson Extraction & Creator Attribution | **Implemented** | `GeminiCoachService.extractYouTubeLessons` |
| `FEAT-014` | Pre-Seeded `@askvinh` Knowledge Base | **Implemented** | `CurriculumCatalog.askVinhKnowledgeBase`, `SpeakCraftRepository:420` |
| `FEAT-015` | Multi-Channel Sync Scheduler / Background Auto-Update | **Not implemented / Proposed** | (Manual trigger only; no WorkManager/Cron) |
| `FEAT-016` | Microphone Recording (AAC, Pause/Resume, Waveform) | **Implemented** | `AudioRecorderManager.kt`, `LiveWaveformVisualizer` |
| `FEAT-017` | In-App CameraX Video Recording | **Implemented** | `CameraVideoRecorder.kt`, `SpeakingLabScreen.kt` |
| `FEAT-018` | Android Photo Picker Video Attachment (`<=19MB`) | **Implemented** | `SpeakingLabScreen.kt:120`, `SpeakingAndLabsViewModel.kt:99` |
| `FEAT-019` | Vinh Giang 3-Stage Self-Audit Playback | **Broken** (`ISS-001`) | `SpeakingLabScreen.kt:256-291` (no video view) |
| `FEAT-020` | Multimodal AI Speech & Delivery Analysis | **Implemented** | `GeminiCoachService.analyzeSpeechRecording` |
| `FEAT-021` | Evidence-Based Feedback Report Card | **Implemented** | `CoachComponents.kt:430` (`FeedbackReportCard`) |
| `FEAT-022` | Past Recordings History & Comparison | **Partially implemented** | `SpeakingLabScreen.kt:335` (`take(10)`, local-only media `ISS-004`) |
| `FEAT-023` | Cloud Media Storage & Cross-Device Playback | **Not implemented / Proposed** | `ISS-004` |
| `FEAT-024` | Daily 9-Step Practice Checklist & Reflection | **Implemented** | `DailyPracticeScreen`, `ISS-002` |
| `FEAT-025` | Timed Daily & Weekly Speaking Challenges | **Implemented** | `ChallengesScreen` |
| `FEAT-026` | Spaced-Repetition Vocabulary & AI Usage Eval | **Implemented** | `VocabularyScreen.kt`, `GeminiCoachService.evaluateVocabularyUsage` |
| `FEAT-027` | Pronunciation, IPA & 5 Vocal Foundations Labs | **Implemented** | `PronunciationLabScreen`, `VoiceTrainingLabScreen` |
| `FEAT-028` | Active Listening & Shadowing Lab | **Implemented** | `ListeningShadowingScreen` |
| `FEAT-029` | Thinking & Articulation / Grammar & Public Speaking Labs | **Implemented** | `GrammarSpeakingScreen`, `PublicSpeakingScreen` |
| `FEAT-030` | Conversation Simulator (13 Scenarios + Voice + Check-In) | **Implemented** | `ConversationSimulatorScreen` |
| `FEAT-031` | Personalized Streaming AI Mentor | **Partially implemented** | `AiMentorScreen` (no persisted history `ISS-021`) |
| `FEAT-032` | Real-World Tasks & Reflection Log | **Implemented** | `RealWorldTasksScreen` |
| `FEAT-033` | Progress Analytics & Objective vs Subjective Split | **Partially implemented** | `AnalyticsScreen` (period chips broken `ISS-018`) |
| `FEAT-034` | Practice Reminders & Push Notifications | **Not implemented / Proposed** | No `POST_NOTIFICATIONS` / FCM / WorkManager |
| `FEAT-035` | In-App SemVer Updater & SHA-256 Verification | **Partially implemented** | `AppUpdateScreen`, `ReleaseUpdateRepository` (`ISS-016`) |
| `FEAT-036` | Local Snapshot Backups & Interrupted Update Recovery | **Partially implemented** | `UpdateBackupManager.kt` (no Restore UI `ISS-017`) |
| `FEAT-037` | RBAC Admin Studio (Sync, Moderation, Releases, Telemetry) | **Implemented** | `AdminDashboardScreen.kt`, `firestore.rules` |
| `FEAT-038` | Keystore Encrypted Optional YouTube API Key | **Implemented** | `AppPreferencesRepository.kt` (AES-256-GCM) |
| `FEAT-039` | Multi-Language / Internationalization (`i18n`) | **Not implemented / Proposed** | `ISS-013` |
| `FEAT-040` | Electron (Windows Desktop) & React Web Clients | **Not implemented / Proposed** | `ISS-024` |

---

## Detailed Feature Specifications

### Domain Group 1: Auth, Onboarding, Navigation & Personalization

#### `FEAT-001` — Google Sign-In & Automatic Session Restore
* **Feature ID & Name:** `FEAT-001` — Google Sign-In & Auto Session Restore
* **Purpose:** Authenticate learners securely without passwords or anonymous guest drift.
* **User Value:** Instant one-tap entry and automatic session restoration on relaunch.
* **Trigger & Entry Points:** `SpeakCraftRoot` when `auth.currentUser == null` -> `AuthScreen` (`LaunchedEffect` silent check + `"Sign in with Google"` button).
* **User Flow:** App opens -> `attemptAutoSignIn` checks authorized accounts -> if none, user taps Sign-In -> Credential Manager sheet returns Google ID Token -> `Firebase.auth.signInWithCredential` -> routes to Onboarding or Dashboard.
* **System Behavior:** `AuthHelper.kt` manages `GetGoogleIdOption`, `GetSignInWithGoogleOption`, and `ClearCredentialStateRequest` on sign-out.
* **UI Components:** `AuthScreen` (`img_onboarding_stage`, `google_sign_in_button`, `CircularProgressIndicator`).
* **Data Models:** `FirebaseUser` (`uid`, `displayName`, `email`).
* **APIs / Operations:** `OP-001` (`CredentialManager` + Firebase Auth).
* **Permissions & Security:** `INTERNET`; Google Play Services Credential Manager; zero anonymous auth allowed in `firestore.rules`.
* **Edge Cases:** User cancels sheet (`GetCredentialCancellationException` handled gracefully without scary error text); missing `default_web_client_id` shows explicit config error.
* **Dependencies:** Google Play Services, `google-services.json`.
* **Implementation Status:** **Implemented**
* **Existing vs. Target Gaps:** Target Electron/Web clients require OAuth2 Authorization Code + PKCE browser flow (`API-001`).

#### `FEAT-002` — Initial Onboarding Calibration & 30-Day Auto-Enrollment
* **Feature ID & Name:** `FEAT-002` — Onboarding Assessment & Plan Initialization
* **Purpose:** Capture learner level, primary communication goal, daily time target, and initial 7 skill self-ratings.
* **User Value:** Personalizes AI Mentor context, Conversation Simulator difficulty, and enrolls user in the 30-Day Communication Challenge.
* **Trigger & Entry Points:** `AuthenticatedAppShell` when `profile == null || !profile.onboardingCompleted`.
* **User Flow:** Enter name -> select level (`Beginner`, `Intermediate`, `Upper-Intermediate`, `Advanced`) -> edit goal -> adjust 8 sliders -> tap `"Start My Coaching Plan"`.
* **System Behavior:** `MainCoachViewModel.completeOnboardingAssessment` writes `/users/{uid}`, enrolls `"30_day_challenge"`, and invokes `seedAskVinhKnowledgeBaseIfEmpty()`.
* **UI Components:** `OnboardingAssessmentScreen`, `AssessmentSlider`, `FilterChip`, `OutlinedTextField`.
* **Data Models:** `UserProfile`, `ProgramEnrollment`.
* **APIs / Operations:** `OP-002`, `OP-003`, `OP-008`.
* **Permissions & Security:** Owner-only Firestore `create`/`update` validated against `isValidUserProfile`.
* **Edge Cases:** Blank name defaults to `"Speaker"`; scores coerced to `0..100`; narrow screens may clip chip `Row` (`ISS-019`).
* **Dependencies:** Firestore connection.
* **Implementation Status:** **Implemented**
* **Existing vs. Target Gaps:** Recommend converting long vertical scroll into a 3-step wizard (`01_UI_UX_DESIGN_DOCUMENTATION.md`).

#### `FEAT-005` — Deterministic Next-Action Recommendation Engine
* **Feature ID & Name:** `FEAT-005` — Recommendation Engine (`RecommendationEngine`)
* **Purpose:** Compute the highest-leverage next training action dynamically on the Home Hero Card.
* **User Value:** Eliminates decision fatigue across 15 communication modules.
* **Trigger & Entry Points:** `MainCoachViewModel.recommendedNextAction` combining `userProfile`, `speakingSessions`, `learningProgress`, and today's `dailyActivities`.
* **User Flow:** User opens Dashboard -> inspects dark Hero card -> taps primary CTA (e.g., `"Complete Daily Warmup"` or `"Launch Articulation Drill"`) -> navigates straight to target route.
* **System Behavior:** Pure rule engine evaluates incomplete daily steps, zero sessions, weakest objective skill score (`fluency`, `pronunciation`, `vocabulary`, `grammar`, `articulation`), and incomplete lessons.
* **UI Components:** `DashboardScreen` hero `Card`, `recommended_action_button`.
* **Data Models:** `RecommendedNextAction(title, reason, ctaLabel, targetRoute)`.
* **APIs / Operations:** Pure in-memory Kotlin transformation (tested in `CurriculumAndRecommendationTest.kt`).
* **Permissions & Security:** N/A (read-only local inference).
* **Edge Cases:** Unloaded states fall back to safe default recommendation (`speaking_lab` / `daily_practice`).
* **Dependencies:** `MainCoachViewModel` state streams.
* **Implementation Status:** **Implemented**
* **Existing vs. Target Gaps:** Future server telemetry could incorporate spaced-repetition due counts (`vocabulary`) and scheduled challenge deadlines.

*(Compact structured coverage for `FEAT-003`, `FEAT-004`, `FEAT-006` follows the same contract: `FEAT-003` Adaptive `NavigationBar`/`NavigationRail` is Implemented at `AppNavigation.kt:347`; `FEAT-004` Light/Dark/System Theme in `Theme.kt` is Implemented with `ISS-014` XML cleanup needed; `FEAT-006` Profile/Settings in `SettingsScreen` is Partially implemented—supports theme/TTS/channel/API key, lacks UI to edit `displayName`/`primaryGoal`/`dailyGoalMinutes` post-onboarding per `ISS-022`.)*

---

### Domain Group 2: Curriculum, Lessons, Programs & `@askvinh` YouTube Intelligence

#### `FEAT-007` through `FEAT-010` — Structured Curriculum (`15 Modules`, `20 Categories`, `Lessons`, `30/60/90-Day Programs`)
* **Feature ID & Name:** `FEAT-007`..`FEAT-010` — Curriculum & Multi-Day Programs
* **Purpose:** Organize communication mastery into 15 navigable modules covering 20 categories (`English Fluency` through `Communication Psychology`), interactive lessons, and 30/60/90-day tracks.
* **User Value:** Clear progression from foundational habits to stage presentation & leadership communication.
* **Trigger & Entry Points:** Dashboard 15-Module grid -> `module_lessons/{slug}` -> `lesson_detail/{lessonId}`; Utility Grid -> `learning_paths`.
* **User Flow:** Select module -> choose lesson -> study objective, watch linked `@askvinh` video timestamp, read steps, record inline practice clip for AI review, add personal notes, mark complete -> advance to `nextLessonId`.
* **System Behavior:** Merges bundled `CurriculumCatalog` definitions with Firestore admin-published `lessons` (`status == "PUBLISHED"`). Progress persists in `/users/{uid}/learningProgress/{lessonId}` and `/users/{uid}/enrollments/{programId}`.
* **UI Components:** `ModuleGrid`, `ModuleLessonsScreen`, `LessonDetailScreen`, `LearningPathsScreen`, `LessonAttributionBadge`.
* **Data Models:** `LearningModule`, `Lesson`, `UserLearningProgress`, `ProgramEnrollment`.
* **APIs / Operations:** `OP-004`, `OP-008`, `OP-015`.
* **Permissions & Security:** Global `lessons` writable only by `isAdmin()`; user progress owner-scoped.
* **Edge Cases:** Duplicate route aliases `articulation_lab` and `pronunciation` both open `PronunciationLabScreen` (`ISS-018`).
* **Dependencies:** `CurriculumCatalog.kt`.
* **Implementation Status:** **Implemented**
* **Existing vs. Target Gaps:** Curriculum text is compiled in Kotlin (`ISS-013`); target architecture serves localized curriculum JSON from backend CMS + CDN.

#### `FEAT-011` through `FEAT-015` — YouTube `@askvinh` Intelligence & Knowledge Base Seeding
* **Feature ID & Name:** `FEAT-011`..`FEAT-015` — YouTube Channel Intelligence Pipeline
* **Purpose:** Ingest real `@askvinh` (or user-provided) channels, playlists, and videos; retrieve transcripts/captions; extract non-duplicate source-attributed communication techniques.
* **User Value:** Grounded learning directly anchored to real creator video URLs and timestamps, clearly distinguishing creator words from AI supplements.
* **Trigger & Entry Points:** Bottom/Rail tab `@askvinh` (`youtube_library`) or `AdminDashboardScreen` Tab 0 (`YouTube Sync`).
* **User Flow:** Paste URL (default `https://www.youtube.com/@askvinh`) -> tap `"Inspect & Extract Lessons"` -> progress updates per video -> browse horizontal `Video Extraction Audit` strip (status: `EXTRACTED`, `NO_CAPTIONS`, `INACCESSIBLE`) -> search & filter extracted techniques by 20 categories & 3 difficulties -> tap `"Source"` to launch YouTube video.
* **System Behavior:** `YouTubeIntelligenceClient` fetches RSS/API/watch HTML + XML captions; `GeminiCoachService.extractYouTubeLessons` structures 1–3 techniques per video; `SpeakCraftRepository.saveYouTubeAnalysis` writes user-scoped `youtubeSources` + `extractedTechniques` (or admin global catalog in `AdminDashboardScreen`).
* **UI Components:** `YouTubeIntelligenceScreen`, `LessonAttributionBadge`, `FilterChip` bars.
* **Data Models:** `YouTubeSource`, `ExtractedTechnique`, `TranscriptSegment`, `YouTubeVideo`, `VideoTranscript`.
* **APIs / Operations:** `OP-003`, `OP-004`, `OP-011`, `OP-013`, `OP-015`.
* **Permissions & Security:** `INTERNET`; optional Data API key encrypted at rest via Android Keystore (`FEAT-038`).
* **Edge Cases:** Missing captions fall back honestly to title + description (`NO_CAPTIONS`) without fabricating timestamps; HTML scraping brittle against YouTube markup shifts (`ISS-007`).
* **Dependencies:** OkHttp, Gemini 3.8 Flash.
* **Implementation Status:** `FEAT-011`..`FEAT-014` **Implemented**; `FEAT-015` (scheduled automated multi-channel sync worker) **Not implemented / Proposed**.
* **Existing vs. Target Gaps:** Move ingestion to server-side scheduled worker (`02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`) and avoid duplicating seed techniques per user (`ISS-011`).

---

### Domain Group 3: Speaking Studio, Audio/Video Capture & Coaching Feedback

#### `FEAT-016` through `FEAT-023` — Record & Review Studio, 3-Stage Audit & AI Speech Diagnostics
* **Feature ID & Name:** `FEAT-016`..`FEAT-023` — Speaking Recording & AI Analysis Suite
* **Purpose:** Capture microphone audio or video (in-app camera or gallery upload), perform Vinh Giang's 3-Stage Self-Audit, run multimodal AI evaluation, and track progress across sessions.
* **User Value:** Actionable, non-hallucinated feedback citing verbatim quotes, filler counts, WPM pace, and score deltas vs. prior recording.
* **Trigger & Entry Points:** Bottom/Rail tab `Record` (`speaking_lab`), plus embedded recording controls inside `ChallengesScreen`, `PronunciationLabScreen`, `VoiceTrainingLabScreen`, `ListeningShadowingScreen`, `GrammarSpeakingScreen`, `PublicSpeakingScreen`, and `LessonDetailScreen`.
* **User Flow:** Enter/pick prompt -> record audio (with pause/resume & live waveform) OR record CameraX `.mp4` OR attach video via Android Photo Picker (`<19 MB`) -> optional self-audit playback -> tap `"Analyze With AI Coach"` -> review `FeedbackReportCard` -> replay or delete past sessions.
* **System Behavior:** `AudioRecorderManager` (`MediaRecorder` `.m4a` AAC) + `InAppCameraVideoDialog` (CameraX `Quality.SD` `.mp4`) + `GeminiCoachService.analyzeSpeechRecording` (`speechAnalysisSchema`) -> `SpeakCraftRepository.saveSpeakingSession` (writes `speakingSessions`, `speechFeedback`, updates rolling weighted profile scores).
* **UI Components:** `SpeakingLabScreen`, `LiveWaveformVisualizer`, `InAppCameraVideoDialog`, `FeedbackReportCard`, `ShimmerSkeletonCard`.
* **Data Models:** `SpeakingSession`, `SpeechFeedback`.
* **APIs / Operations:** `OP-005`, `OP-010`.
* **Permissions & Security:** Dangerous runtime permissions `RECORD_AUDIO` and `CAMERA` requested via `ActivityResultContracts`; gallery uses zero-permission `PickVisualMedia`.
* **Edge Cases & Confirmed Bugs:**
  * `ISS-001` (**Broken**): Stage 1 "Mute Pass" lacks a video playback viewport (`AudioRecorderManager` is audio-only `MediaPlayer`).
  * `ISS-003` (**Bug**): Failed Firestore save still renders `UiState.Success`.
  * `ISS-004` (**Gap**): Cloud storage not implemented; recordings are device-local only (`FEAT-023` **Not implemented**).
  * `ISS-005` (**Stability**): `readBytes()` loads full picked video before `19 MB` guard.
  * `ISS-019` (**UX**): Single-tap delete has no confirmation modal.
* **Implementation Status:** `FEAT-016`, `FEAT-017`, `FEAT-018`, `FEAT-020`, `FEAT-021` **Implemented**; `FEAT-019` **Broken**; `FEAT-022` **Partially implemented**; `FEAT-023` **Not implemented / Proposed**.

---

### Domain Group 4: Vocabulary, Listening, Grammar, Challenges & Real-World Tasks

#### `FEAT-024` through `FEAT-030` & `FEAT-032` — Daily Practice, Challenges, Specialized Labs, Vocabulary & Conversation Simulator
* **Daily Practice (`FEAT-024`, Implemented, Bug `ISS-002`):** 9-step daily checklist, 3 rotated daily words (`todayEpochDay % bank.size`), reflection journal, 1–10 confidence rating. Streak math currently fails to reset on missed days (`ISS-002`).
* **Timed Challenges (`FEAT-025`, Implemented):** `ALL` / `DAILY` / `WEEKLY` timed drills merging `CurriculumCatalog.speakingChallenges` + Firestore admin `learningChallenges`, with auto-stop countdown timer (`SpeakingAndLabsViewModel:64`).
* **Vocabulary Builder (`FEAT-026`, Implemented):** SM-2-style spaced repetition (`masteryLevel 0..5`, `repetitions`, `nextReviewEpochDay` intervals `1d -> 3d -> 3n..21d`), `0.75x`/`1.0x` TTS playback, custom word creation, and AI contextual sentence evaluation (`vocabEvalSchema`).
* **Pronunciation & Voice Labs (`FEAT-027`, Implemented):** Minimal pairs, IPA sounds, word/sentence stress, intonation, linking, and 5 Vocal Foundations drills with TTS speed models + AI comparison.
* **Listening & Shadowing (`FEAT-028`, Implemented):** `@askvinh` script segments with pause markers, target WPM, TTS reference, YouTube link, and AI shadow scoring.
* **Grammar & Public Speaking (`FEAT-029`, Implemented):** Conversational mistake vs. natural correction drills (`PREP`, `CLEAR`), plus opening hooks and stage delivery evaluation.
* **Conversation Simulator (`FEAT-030`, Implemented):** 13 roleplay scenarios adapted to `userLevel`, non-interruptive natural turn-taking (`GeminiCoachService.sendConversationTurn`), `SpeechRecognizer` voice dictation + `TextToSpeech` partner audio, and end-of-session structured debrief saved to `conversationSessions`.
* **Real-World Tasks (`FEAT-032`, Implemented):** Checklist of real-life communication assignments with reflection logging saved to `realWorldTasks`.

---

### Domain Group 5: Analytics, Notifications, Updater, Admin & Multi-Platform

#### `FEAT-031`, `FEAT-033` through `FEAT-040` — Mentor, Analytics, Updater, Admin & Global Expansion
* **Personalized AI Mentor (`FEAT-031`, Partially implemented):** Streams grounded advice (`askMentorStream`) aware of profile scores, recent 5 sessions, and 3 feedback records; lacks persisted chat history (`ISS-021`).
* **Progress Analytics (`FEAT-033`, Partially implemented):** Displays strongest/weakest skill tiles, rolling skill breakdown bars, recurring mistakes, and strictly separates subjective confidence from objective acoustic scores. Period filter chips (`Daily`/`Weekly`/`Monthly`) are non-functional display labels (`ISS-018`).
* **Practice Reminders & Notifications (`FEAT-034`, Not implemented / Proposed):** No `POST_NOTIFICATIONS` permission, `WorkManager` local reminders, or FCM push campaigns exist.
* **In-App Updater & Backup (`FEAT-035`, `FEAT-036`, Partially implemented):**
  * Checks Firestore `applicationReleases` (`stable` / `beta`), evaluates `SemVer` + `minSupportedVersion`, downloads over HTTPS with progress bar, validates 64-char SHA-256 checksum & APK signing certificate (`ApkIntegrityVerifier`), creates pre-update local JSON snapshot (`UpdateBackupManager`), detects interrupted updates on next launch, and fires `FileProvider` `ACTION_VIEW` install intent.
  * Gaps: Missing `INSTALLED`/`ROLLED_BACK` completion telemetry (`ISS-016`) and missing `"Restore Backup"` UI (`ISS-017`).
* **RBAC Admin Studio (`FEAT-037`, Implemented):** Restricted to `/adminUsers/{uid}` active records (`firestore.rules:17`). Supports server-catalog YouTube sync, AI technique moderation (`PUBLISHED`/`APPROVED`/`REJECTED`), shared Lesson & Challenge publishing, release publishing/rollout toggling, and update funnel telemetry inspection.
* **Android Keystore Secret Storage (`FEAT-038`, Implemented):** Encrypts optional user-entered YouTube Data API v3 key via `AES/GCM/NoPadding` hardware/Keystore-backed key in `AppPreferencesRepository.kt`.
* **i18n (`FEAT-039`, Not implemented)** & **Electron Windows Desktop + React Web (`FEAT-040`, Not implemented / Proposed):** Documented in `01_UI_UX_DESIGN_DOCUMENTATION.md`, `02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`, and `07_DATABASE_DESIGN_AND_DOCUMENTATION.md`.

---

## Product Release Roadmap

| Release Milestone | Target Scope | Features & Issues Addressed |
| :--- | :--- | :--- |
| **Current Baseline (v1.0.0)** | Android MVP on Firebase | `FEAT-001`..`005`, `007`..`014`, `016`..`018`, `020`..`021`, `024`..`030`, `032`, `037`..`038` |
| **V1.1 (Android Hardening)** | Fix broken features & data bugs | Fix `FEAT-019` (`ISS-001` Video Player), `ISS-002` (Streak), `ISS-003` (Save error), `ISS-018` (Analytics period filter), `ISS-019` (Delete dialogs), `FEAT-035`/`036` (`ISS-016`–`017`) |
| **V1.5 (Cloud Media & Notifications)** | Cross-device media & retention | `FEAT-023` (Cloud Storage + streaming), `FEAT-031` (Persisted Mentor threads), `FEAT-034` (Practice reminders + FCM), `FEAT-015` (Server YouTube sync worker) |
| **V2.0 (Multi-Platform Expansion)** | Windows Desktop (Electron) & Web (React) + API Gateway + PostgreSQL | `FEAT-040` (Electron + React SPA), Unified REST/WS Backend (`02`), PostgreSQL migration (`07`), `FEAT-039` (`i18n`) |

---

## Cross-References & Open Questions

* **Cross-References:** `01_UI_UX_DESIGN_DOCUMENTATION.md` (screen layouts), `04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md` (bug details), `06_AI_FEATURES_AND_AI_ARCHITECTURE.md` (AI feature schemas).
* **Open Questions:**
  1. Should `FEAT-034` (practice reminders) prioritize local Android `WorkManager` notifications first or full cross-platform FCM + Web Push?
  2. In `V2.0`, should Windows Electron auto-updates use Squirrel/electron-updater feeding from the same `applicationReleases` schema extended with `platform` (`win32`, `android`)?
