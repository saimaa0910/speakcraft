# 03 — Source Code Audit

| Field | Value |
|---|---|
| Document ID | RD-03 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Android Technical Lead (with AI/ML Engineer, Security Engineer, QA Architect) |
| Approval | Pending — Product Owner |
| Related | RD-00, RD-02, RD-04, RD-05, RD-06, DES-01, SEC-01, ARC-01 |

---

## 1. Purpose, scope and method

This audit records what the SpeakCraft code **actually does** at commit `68a9cb1` (`main`, clean tree). It is the evidence base for the redesign; product documents cite its finding IDs (`FND-<AREA>-nn`).

| Item | Detail |
|---|---|
| Scope | All 106 main Kotlin files (48,894 lines), 25 test files, `AndroidManifest.xml`, resources, Gradle files, `firestore.rules` (865 lines), `firestore.test.js`, `firebase-blueprint.json`, CI workflow, scripts |
| Method | Static reading by four parallel audit streams (UI/navigation; learning/activity/evaluation domain; AI/speech/media; platform/security/build), each citing `path:line`; grep-based counts; cross-checks of high-impact claims |
| Not done | **No build, run, device test or test execution** (no JDK/Android SDK in the analysis environment; Gradle wrapper not committed). Runtime behaviours are marked INFERRED |
| Labels | **VERIFIED** = seen in code at the cited location · **INFERRED** = reasoned from code and documented platform behaviour |
| Path alias | `pkg/` = `app/src/main/java/com/example/` |

Detailed evidence (with several hundred line citations) is preserved in four working papers in `docs/redevelopment/working_papers/` (`ui_audit.md`, `data_learning_audit.md`, `ai_media_audit.md`, `platform_security_audit.md`); the findings below are the consolidated, decision-relevant subset. Canonical finding IDs: `working_papers/FND_ID_MAP.md`.

## 2. Codebase shape (VERIFIED)

| Package | Files | Lines | Notes |
|---|---|---|---|
| `data/ai` | 28 | 9,100 | Local intelligence, speech, video, cloud AI (dormant) |
| `data/activity` | 10 | 5,770 | 150-activity catalogue and engines |
| `data/learning` | 6 | 2,089 | `LearningProgressManager` and plan/streak/SRS/quiz engines |
| `data/model` | 4 | 2,053 | Curriculum catalogue, Firestore models, tutorial registry |
| `data/firebase` | 5 | 1,899 | `SpeakCraftRepository` (1,351 lines), auth, App Check |
| `data/evaluation` | 3 | 1,099 | Evidence validator and scoring engine |
| `data/update` | 5 | 995 | GitHub/Firestore self-updater |
| `data/media`, `notifications`, `youtube`, `preferences` | 9 | 2,471 | Recording, TTS, in-app alerts, YouTube scraping, DataStore |
| `ui/screens` | 15 | 10,724 | 26 routes |
| `ui/components` | 9 | 7,792 | Shared widgets, coach components, overlays |
| `ui/viewmodel` | 5 | 2,681 | 5 ViewModels |
| `ui/theme` | 4 | 523 | Colours, type, spacing |

Largest files: `CoachComponents.kt` 1,943 · `AppNavigation.kt` 1,548 · `DashboardStudioWidgets.kt` 1,543 · `ActivityCatalogueModules7To10.kt` 1,496 · `SpeakCraftRepository.kt` 1,351 · `CurriculumCatalog.kt` 1,124 · `SpeakingAndChallengesScreens.kt` 1,114.

## 3. Architecture and layering

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-ARCH-01 | Single `:app` module with no domain layer, no use cases and no repository/service interfaces. ViewModels call concrete Firebase repositories and engines directly | `settings.gradle.kts:27`; 0 "UseCase"; only interfaces are `LiveVoiceHandle` and a sealed update state | High |
| FND-ARCH-02 | The manual DI container is created inside a composable (`remember`) and there is no `Application` class, so it is rebuilt on every Activity recreation together with stateful managers (recorder, transcription) | `pkg/ui/AppNavigation.kt:216`; `AndroidManifest.xml:17-25` | High |
| FND-ARCH-03 | God objects: `AuthenticatedAppShell` is a 1,032-line composable holding ViewModel wiring, 20+ state collections, business logic, dialog gating and the NavHost; `SpeakCraftRepository` mixes user data, shared catalogue, releases, RBAC, admin and account deletion; `UpdateAndAdminViewModel` mixes OTA, backups, admin CMS, YouTube sync and AI | `AppNavigation.kt:290-1321`; `SpeakCraftRepository.kt`; `UpdateAndAdminViewModel.kt:30-431` | High |
| FND-ARCH-04 | Pervasive duplication of engines and concepts (inventory in §11) | §11 | High |
| FND-ARCH-05 | Placeholder namespace `com.example`; applicationId `com.aistudio.speakcraft.qxmvkp` from the Google AI Studio scaffold | `app/build.gradle.kts:24,28` | Low |

## 4. UI layer and navigation

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-UI-01 | **Navigation sprawl.** 26 NavHost routes, 4 non-route gates and 5 global overlays. Phones show 5 bottom tabs (Home, Daily, Record, **@askvinh**, Mentor); the other 21 routes are reached mainly through a 22–23-tile dashboard directory. The ≥ 840 dp sidebar has 10 items with different labels (e.g. "Tools" = Vocabulary); the 600–839 dp rail loses Settings, Vocabulary, Analytics, Activity Library and Learning Paths. One orphan route (`activity_library/{moduleId}`) | `AppNavigation.kt:177-183, 915-1236, 828-905, 1336-1348` | High |
| FND-UI-02 | **Overloaded home.** The dashboard has up to 16 top-level items, ~21 widgets, ~70 tap targets and 25 navigation targets (~5–6 phone screens of scroll); three overlapping "today" widgets and two conflicting weekly-goal concepts | `DashboardScreen.kt:327-674`; `DashboardStudioWidgets.kt` | High |
| FND-UI-03 | **"@askvinh" tab** opens a YouTube channel-ingestion tool, not a learning experience: paste a URL, scrape captions, fabricate "techniques" from the first five sentences with invented `idx*45s` timestamps shown as "verified"; videos open externally; no practice action | `AppNavigation.kt:181`; `YouTubeIntelligenceScreen.kt:63-302,321`; `LocalIntelligenceOrchestrator.kt:847-907` | High |
| FND-UI-04 | **Duplicated practice surfaces.** 10 screens record speech; 8 share the same recorder and the same evaluation call, and the local engine ignores lab-specific instructions, so "labs" score identically. 12 different "start recording" labels, 8 "analyse" labels, 5 "stop" labels; `articulation_lab` and `pronunciation` are one composable | `SpeakingAndLabsViewModel.kt:445-556`; `LocalIntelligenceOrchestrator.kt:257-260, 923-943` | High |
| FND-UI-05 | **Missing states.** Every list maps Loading/Error to empty (`orEmpty()`); profile-load error is a dead end; mic-permission denial does nothing visible on all 7 recorders; analysis can spin forever offline | `AppNavigation.kt:490-494, 513-521`; `SpeakingAndChallengesScreens.kt:150-154` (+6 sites) | High |
| FND-UI-06 | **Fragile state.** 0 `rememberSaveable` in screens vs 89 `remember { mutableStateOf }`; 0 `SavedStateHandle`; an activity attempt is wiped on rotation; a recording keeps running across screens and one lab can evaluate another lab's recording | `UniversalActivityScreen.kt:76-78`; grep | High |
| FND-UI-07 | **Fabricated or misleading displays.** Fallback overall score 62, single-point "trend" 65, "1 Day Streak" for new users, self-rated onboarding sliders shown as "Objective Speech Skills", sparkline inventing 60→62, "Welcome back" on the first visit, "9-step" routine with 8 steps, "5 Vocal Foundations" with 4 | `DashboardScreen.kt:181,238`; `SpeakCraftRepository.kt:494`; `AnalyticsAndSettingsScreens.kt:101-110`; `CoachComponents.kt:414-417` | High |
| FND-UI-08 | **Inconsistent terminology** for the same concepts (e.g. the daily feature has 7 names; paths have 6; the coach has 7) and developer jargon on screen ("INSUFFICIENT_EVIDENCE", "Running SpeakingEvaluator…", "CameraX", "MediaPipe", CI secret names) | DES-01 terminology table | Medium |
| FND-UI-09 | **Design tokens partly applied.** Colours fully tokenised (0 `Color(0x` outside theme) but 1,386 literal dp values, 147 raw `RoundedCornerShape`, 95 font-weight overrides; `labelSmall` is a monospace font used 103 times; `headlineSmall` undefined; single-weight fonts with synthesized bold; no dynamic colour; ~386 hard-coded English strings vs 4 string resources | `ui/theme/*`; grep counts | Medium |
| FND-UI-10 | **Accessibility gaps.** 127 of 195 icons without description (mostly decorative, some meaningful); 6 heading semantics and 2 roles in the whole UI; undersized targets (7 dp dots, 24 dp plan checks); fixed-height containers that clip at large font scales (hero 252 dp, flashcard 270 dp, score ring 72 dp); dashboard ignores reduced motion | DES-01 §accessibility | High |
| FND-UI-11 | **Tutorial overload.** Four overlapping tutorial mechanisms (auto tour, first-use spotlight, help dialog ×2, inline guide strip on 23 screens); Back/Skip marks tours completed | `AppNavigation.kt:1252-1299`; `TutorialOverlay.kt` | Medium |
| FND-UI-12 | **Prior request (PO-REQ-1119) only partly built.** Welcome-back banner is one text line ignoring avatar/track/day/score parameters; the HorizontalPager carousel exists with mostly static slides and the same image; achievements, continue-learning, delete-account UI and Room are absent | `DashboardStudioWidgets.kt:127-189, 247-289` | Medium |
| FND-UI-13 | **Update UX.** Mandatory-update dialog locks the whole app; "Check for Updates" downloads without asking; install likely fails (FND-OPS-04) | `UpdateAvailableDialog.kt:75-77`; `UpdateAndAdminViewModel.kt:216-226` | Medium |
| FND-UI-14 | **Settings mixes developer and user concerns.** 29 controls, engine diagnostics first on phones, permanently disabled "Cloud AI Coach" switch, plaintext YouTube API key field, no account deletion, no export, no About/Terms/Privacy | `AnalyticsAndSettingsScreens.kt:394-1068` | Medium |

**Preserve:** Material 3 + Compose adaptive layouts (compact/medium/expanded tiers), tokenised colour roles, the HorizontalPager implementation quality, the honest "insufficient evidence" messaging pattern, per-user tutorial state isolation.

## 5. Speech, audio and intelligence pipeline

### 5.1 Pipeline as implemented (VERIFIED unless noted)

```
mic ──MediaRecorder AAC──► .m4a ────────────────────────────────┐
mic ──SpeechRecognizer (opt-in, default OFF, 1st utterance)──► text ┤
"Transcribe Audio": Whisper path fake / Android path returns "" ─┘
        ▼
LocalIntelligenceOrchestrator → decode (boxed shorts) → DSP (RMS, VAD, pauses, pitch)
        → EvidenceValidator GATE ─fail→ "insufficient evidence", nothing saved   (strength)
        → regex grammar (if transcript) → fusion (pass-through) → heuristic scores → templates
        → Firestore save (metrics + transcript ≤15k chars + absolute file path)
Cloud Gemini: compiled in, unreachable (const LOCAL_ONLY)
```

### 5.2 Findings

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-AI-01 | **No transcript by default.** Live transcription defaults to off; without a transcript, grammar and vocabulary are null and feedback is acoustic-only | `AISettingsRepository.kt:26` | **Critical** |
| FND-AI-02 | **Post-hoc file transcription is stubbed** — the Android branch always returns `""`; UI tells users to install a model that cannot be installed | `SpeechRecognitionManager.kt:113-126`; `AnalyticsAndSettingsScreens.kt:861` | Critical |
| FND-AI-03 | **Fabricated Whisper/Moonshine path.** Invented token map (including "speak"/"craft" tokens), non-conformant naive-DFT log-mel, no decoder loop; catalogue URL is a GGML file; Moonshine never runs; no downloader exists; all model SHA-256 values blank and integrity check returns true for blank | `LocalSpeechRecognitionEngine.kt:170-197, 358-459`; `OnDeviceModelManager.kt:83-139, 191` | High |
| FND-AI-04 | **Live transcription defects.** Single utterance per session (no restart, no segmented session) so long answers are mostly lost; text duplicated on stop and scored; no `<queries>` for `RecognitionService` (often breaks recognition on API 30+, INFERRED); recorder and recogniser compete for the mic | `LiveTranscriptionManager.kt:363-381, 418-431`; `AndroidManifest.xml` | Critical |
| FND-AI-05 | **False capability states.** Settings shows "Video Intelligence: Available / MediaPipe ready" with no model files; "Offline verified" when only an unusable Whisper file exists; harmless no-match shown as "Initialization Failed" | `OnDeviceModelManager.kt:160-166`; `IntelligenceCapabilityManager.kt:113-116, 177-183, 227-294` | High |
| FND-AI-06 | **Ineffective toggles.** "Local AI off" does not stop language engines; "audio analysis off" does not stop audio analysis | `HybridAiCoachCoordinator.kt:181`; `EvaluationEvidenceValidator.kt:109-134` | Medium |
| FND-AI-07 | **Broken or constant metrics.** Background-noise thresholds unreachable (value bounded below 0.015, thresholds 0.06/0.08); speaking-rate consistency always 1.0 (constant +12 fluency and a fake "100% steady pacing" compliment); one loud sample = clipping; pitch spread in Hz (biased to higher voices); ASR confidence hard-coded 0.85/0.94 | `AudioFeatureExtractionEngine.kt:71-75, 99-108, 192-195`; `SpeakingAndLabsViewModel.kt:129,193` | High |
| FND-AI-08 | **Mislabelled scores.** Free-speech "Pronunciation & Clarity" = loudness + pitch variation + voicing ratio; with reference text it is recognition word agreement (not phonemes); grammar starts from a baseline of 78; fluency from 38; confidence values are constants | `EvidenceBasedScoringEngine.kt:354-372, 411-453, 533-543` | **Critical** (trust) |
| FND-AI-09 | **Rule quality.** ~40 regex grammar rules with frequent false positives (e.g. "tools that don't work", "if it were", "an MBA", "a two year contract", "the will to win", "that that"); unpunctuated recogniser output makes any > 45-word answer a "run-on"; filler list flags legitimate "like/right/kind of" while recognisers drop "um/uh"; type-token ratio penalises longer answers; "a lot of" → "many" rewrite is ungrammatical with uncountables | `LocalLanguageAnalysisEngine.kt:76-615, 645-650`; `FillerWordDetectionEngine.kt:33-37`; `DynamicLocalCoachEngine.kt:92` | High |
| FND-AI-10 | **Dormant cloud AI.** `firebase-ai` shipped; `FirebaseAiLogicService` would send raw audio/video (≤ 19 MiB), transcripts, profile and history, and stream live microphone audio, with no consent flow, and invents default scores (74, floor 20). Blocked only by a compile-time constant; the "network boundary" interceptor is attached to no client; the zero-cloud test gives false assurance | `FirebaseAiLogicService.kt:76-868, 783`; `LocalExecutionPolicy.kt:13-15, 35-48`; `ZeroCloudAiNetworkBoundaryTest.kt` | High |
| FND-AI-11 | **YouTube scraping.** Watch-page scraping with a spoofed user-agent, undocumented caption endpoints, verbatim captions stored and published as "PUBLISHED" techniques with synthetic timestamps; user API key placed in URL | `YouTubeIntelligenceClient.kt:199-201, 391-452, 505-510`; `LocalIntelligenceOrchestrator.kt:869-882` | High (legal) |
| FND-AI-12 | **Video analysis non-functional.** No MediaPipe models bundled or downloadable; if present, outputs would measure framing only; "head tilt" alert is shoulder unevenness; activity-engine video is simulated (`videoAnalysisCompleted = true` when a file exists) | `assets/models/` (README only); `BodyLanguageAnalysisEngine.kt:100-102,167`; `UniversalEvaluationEngine.kt:200-201` | High |
| FND-AI-13 | **Memory/performance.** Decoded PCM stored as boxed `ArrayList<Short>` (≈ 250–300 MB for a 2-min 48 kHz stereo clip, INFERRED OOM); up to 60 bitmaps held; capability matrix computed on the main thread | `AudioResampler.kt:199, 247-249`; `VideoFrameProcessingEngine.kt:100-164`; `AppNavigation.kt:435-437` | High |
| FND-AI-14 | **Lifecycle.** No handling of backgrounding, calls or audio focus during recording; failed/discarded recordings are never deleted; recording is unbounded | grep (no `AudioFocus`/`ON_STOP`); `AudioRecorderManager.kt:59-113` | Medium |
| FND-AI-15 | **Duplication.** 4 speech-recognition wrappers, 5 availability sources, 2 dead feedback generators, ≥ 5 tokenisers, 3 sentence splitters with 3 run-on thresholds | `ai_media` working paper §12 | Medium |
| FND-AI-16 | **"AI Mentor" and conversation partner** are template engines with simulated streaming (`delay(4L)` per 56-char chunk) and invented profile values when data is missing | `LocalIntelligenceOrchestrator.kt:395-513, 566-775, 960, 973` | High (honesty) |
| FND-AI-17 | **STRENGTH — evidence gate.** Fail-closed validation (duration ≥ 1.5 s, RMS ≥ 0.008, voiced ≥ 0.5 s, session-scoped transcript), nullable dimension scores, partial-evaluation labelling, 15 meaningful tests | `EvaluationEvidenceValidator.kt:42-75`; `EvidenceBasedScoringEngine.kt:49-175`; `CoachEvaluationEvidenceTest.kt` | — (keep) |

**Honest capability statement for today's scores:** they can claim relative loudness, proportion of voiced time, rough pitch spread, pause counts and, with a transcript, rule-matched error counts on this device. They cannot claim pronunciation accuracy, intelligibility, grammatical accuracy, or comparability across devices, rooms or voices.

## 6. Learning, activity and evaluation domain

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-DOM-01 | **Three parallel learning systems** with separate content, plans, streaks and storage: Activity Engine (150 activities, Firestore), `LearningProgressManager` (dashboard plan/streak/badges, local JSON), `CurriculumCatalog` (8-step routine, 12 lessons, 7 programs, 20-word vocabulary bank, Firestore) | `ActivityEngineViewModel.kt:469-495`; `LearningProgressManager.kt:99-117`; `AppNavigation.kt:949-957` | High |
| FND-DOM-02 | **`LearningProgressManager` mostly dead.** UI calls only load, complete-task and announcement ingest; the 5-stage vocabulary flow, flashcards, quiz, notebook, export/clear, sync merge and preference updates have no callers | `AppNavigation.kt:547,590,934`; data working paper §9 | High |
| FND-DOM-03 | **The user's goal never reaches the dashboard plan** (non-empty default goal list blocks profile ingestion) | `LearningProgressManager.kt:86-88`; `LearningModels.kt:380` | High |
| FND-DOM-04 | **Two scoring philosophies.** Speaking Lab: per-dimension, evidence-gated. Activity Library: `56 + length + 8 if recording`, clamped 30–96; per-dimension scores are the overall ±2; rubric weights ignored; "Articulation"/"Stress" dimensions scored from typed text | `UniversalEvaluationEngine.kt:258-282` | **Critical** (trust) |
| FND-DOM-05 | **Activity completion without speaking.** Activity audio is never transcribed (transcript field never set); a wordless 1.5 s recording scores 64 and passes; three typed words score 61 and pass; one pass completes the activity for paths and milestones | `ActivityEngineViewModel.kt:354-365`; `ActivityEngine.kt:92` | High |
| FND-DOM-06 | **Self-reported completion.** Daily routine steps and dashboard plan tasks complete with one tap, adding planned minutes | `DashboardStudioWidgets.kt:639-640`; `MainCoachViewModel.kt:275` | High |
| FND-DOM-07 | **Four streak definitions** shown in different places; the Firestore profile streak goes stale (date written without a completed step; never decays); streaks are gameable by taps | `SpeakCraftRepository.kt:670-684`; `StreakAndCompletionEngine.kt`; `ActivityProgressAnalyticsEngine.kt:34-40`; `UserProfileScreen.kt:153-156` | High |
| FND-DOM-08 | **Fabricated counters.** 3 minutes per ticked step; "words learned" increments on first review even for "Need review"; `completedExercisesCount` increments on un-ticking; activity analytics only see the last 200 attempts | `MainCoachViewModel.kt:275`; `SpeakCraftRepository.kt:683, 746-769`; `ActivityFirestoreRepository.kt:85` | High |
| FND-DOM-09 | **Third-party creator dependence.** "Vinh"/"askvinh" appears 102 times in `CurriculumCatalog.kt` (149 source lines app-wide); 12 "VERIFIED" techniques with video titles and timestamps all link to the channel home page; a program "Adapted from @askvinh's 30-Day Mastery Plan"; activities use the same named frameworks while attributed to "SpeakCraft Core Curriculum"; `metadata.json` says "built from @askvinh YouTube lessons"; **no licence or permission recorded** | `CurriculumCatalog.kt:187-519, 1005`; `metadata.json` | **Critical** (legal) |
| FND-DOM-10 | **Activity daily plan loses completed items** (regenerated on every attempt; just-completed items penalised as "recent"); persisted plan never read back; path prerequisites computed but not enforced; 6 of 7 structured programs can never be completed | `DailyPracticeEngine.kt:241-242`; `ActivityDiscoveryScreen.kt:112-114`; `LearningPathsEngine.kt:313-317` | High |
| FND-DOM-11 | **Spaced repetition not wired.** SM-2 scheduler unused by the vocabulary UI; UI uses an ad hoc 1/3/3n-day scheduler capped at 21 days; epoch day computed in UTC; "words due" notification can never fire | `SpacedRepetitionScheduler.kt`; `MainCoachViewModel.kt:344-374` | Medium |
| FND-DOM-12 | **Level taxonomy inconsistent** (onboarding 4 levels, profile 5, activities 3, learning model 5); level is self-selected, with no placement and no progression; unknown labels map to Intermediate | `AuthAndOnboardingScreens.kt:450`; `UserProfileScreen.kt:76-82`; `DailyPracticeEngine.kt:60-64` | Medium |
| FND-DOM-13 | **Content.** 150 hand-written but rigidly templated activities (one rubric per module; 147 with exactly 3 instructions; all with exactly 1 example; 2 duplicate titles); only **20 vocabulary words**; 12 lessons | data working paper §2 | Medium |
| FND-DOM-14 | **Notifications are in-app only** (no OS notifications, channels, permission or scheduling); two notification stores sync one way, producing duplicate daily alerts; dismissed alerts likely reappear; `preferredReminderTime` never read | `NotificationManagerRepository.kt`; `LearningProgressManager.kt:131-164` | High |
| FND-DOM-15 | `isPartialEvaluation` (Kotlin `is`-prefixed boolean) likely reads back as `false` through Firestore bean mapping, so partial evaluations may display as complete after reload (INFERRED) | `FirestoreModels.kt:143`; `SpeakCraftRepository.kt:830` | Low |

## 7. Data and persistence

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-DATA-01 | **No single source of truth.** Learning data spread across Firestore (≈ 20 user subcollections), local JSON files (learning snapshot, alerts, mentor history) and DataStore; daily-minute clamp ranges disagree across four places | persistence map (data working paper §8) | High |
| FND-DATA-02 | Dashboard plan, streak, achievements and local notifications live only in a local JSON file — never synced, lost on reinstall, though the tutorial promises cross-device streaks | `LearningProgressManager.kt:49-51, 713-784`; `FeatureTutorialRegistry.kt:28` | High |
| FND-DATA-03 | **Offline writes hang.** Firestore writes are awaited on UI paths (server acknowledgement), so analysis/activity submission can spin indefinitely offline; many updates are non-transactional read-modify-write sequences (race-prone) | `SpeakCraftRepository.kt:482-529, 644-690, 862-893, 1337-1350`; `ActivityEngineViewModel.kt:408-426` (INFERRED runtime) | High |
| FND-DATA-04 | Absolute device file paths stored in Firestore (`localMediaPath`, `recordedMediaPath`) — dangling on any other device | `ActivityFirestoreRepository.kt:201` | Medium |
| FND-DATA-05 | 16 Firestore snapshot listeners start at sign-in (10 user collections unbounded) plus 4 more in the activity ViewModel; read cost scales with history | `AppNavigation.kt:405-426`; `MainCoachViewModel.kt:101-151` | Medium |
| FND-DATA-06 | **Speaking-session saves are rejected by the rules (HEAD regression).** Commit `68a9cb1` added 5 fields not in the `speakingSessions` allow-list (and a disallowed confidence value), so every save fails and takes the feedback document and profile update with it; YouTube re-analysis and admin re-sync also violate immutability rules (INFERRED) | `SpeakCraftRepository.kt:798-836` vs `firestore.rules:218, 459-467` | **Critical** (functional) |
| FND-DATA-07 | Rules, blueprint and code drift: 5 rule-protected collections are written by no code; 3 written collections are absent from the blueprint | platform working paper §3.2 | Low |
| FND-DATA-08 | Local JSON stores are written synchronously on the main dispatcher | `LearningProgressManager.kt:884-887`; `NotificationManagerRepository.kt:434-437` | Medium |

## 8. Firebase, security and privacy

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-SEC-01 | Google sign-in is mandatory before any use; no account-free or local mode | `AppNavigation.kt:244-261`; `AuthHelper.kt` | Medium (product) |
| FND-SEC-02 | **Distributed beta builds can be debug builds.** CI builds `assembleDebug` (debuggable, debug keystore with public password, App Check Debug provider) for the beta channel when no release keystore secret is set; the exported launcher accepts an App Check debug-token intent extra in such builds | `release_on_push.yml:257-262, 297-328`; `app/build.gradle.kts:56-61`; `AppCheckConfig.kt:24-44` | **Critical** (conditional on secrets) |
| FND-SEC-03 | **Account deletion unreachable** (implemented, no UI caller, no re-authentication; partial-deletion state possible; update events undeletable); **no data export** | `SpeakCraftRepository.kt:1287-1319`; `MainCoachViewModel.kt:474`; `firestore.rules:862` | High |
| FND-SEC-04 | Admin model over-broad: any admin role including MODERATOR can publish OTA releases and write unvalidated lessons; admins can read all user profiles; client and rules map roles differently | `firestore.rules:333-334, 794-853`; `SpeakCraftRepository.kt:250,261` | Medium |
| FND-SEC-05 | **Privacy text contradicted.** Settings states audio, video and transcripts stay on-device, but transcripts (≤ 15k chars) are saved to Firestore | `AnalyticsAndSettingsScreens.kt:917`; `SpeakCraftRepository.kt:806` | High |
| FND-SEC-06 | `allowBackup=true` with untouched template backup/extraction rules — recordings, mentor chat and learning files eligible for cloud backup/device transfer | `AndroidManifest.xml:18-20`; `res/xml/*.xml` | High |
| FND-SEC-07 | Live transcription falls back to the default (typically network) recogniser when no on-device recogniser exists, without disclosure | `LiveTranscriptionManager.kt:99-103, 165, 340` | High |
| FND-SEC-08 | uid, email and provider emails logged to Logcat on every Firestore error, in all build types | `FirestoreErrorHandler.kt:26-51` | Medium |
| FND-SEC-09 | Sign-out clears nothing local; recordings not partitioned per user; wipe helpers have no callers | `AuthHelper.kt:126-143`; `LearningProgressManager.kt:697` | Medium |
| FND-SEC-10 | Rules gaps: list validation checks only elements 0–2; several collections lack `hasOnly`; scores, streaks and counters fully client-writable | `firestore.rules:64-70, 356-362, 704-753` | Medium |
| FND-SEC-11 | R8 disabled; App Check debug library ships in release; logs not stripped | `app/build.gradle.kts:66,139` | Medium |
| FND-SEC-12 | FileProvider exposes the entire cache dir and unused paths | `res/xml/file_paths.xml:3-6` | Low |

**Preserve:** per-user isolation quality of `firestore.rules` (default deny, owner checks, allow-lists, immutable `createdAt`); Keystore AES-GCM pattern for secrets at rest; Credential Manager sign-in.

## 9. Build, CI/CD and release

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| FND-OPS-01 | CI runs **no unit tests, no lint, no rules tests and no rules deploy**; every push to `main` publishes a release | `release_on_push.yml` | High |
| FND-OPS-02 | Gradle wrapper scripts not committed; local builds and unit tests need `google-services.json`, which is absent and undocumented | `git ls-files`; `AuthHelper.kt:53` | Medium |
| FND-OPS-03 | Supply chain: actions pinned to mutable tags; `npm install firebase-admin` without lockfile while the service-account secret is in the environment | `release_on_push.yml:55,66,107,469,500` | Medium |
| FND-OPS-04 | **Self-updating APK channel:** GitHub Releases + Firestore with a user-editable repository field, "uninstall the old build" advice on signature mismatch, missing `REQUEST_INSTALL_PACKAGES` (install likely fails on API 26+); incompatible with Google Play policy; APKs only, no AAB | `AppUpdateScreen.kt:265,314-324`; `ReleaseUpdateRepository.kt:313-362` | High |
| FND-OPS-05 | No crash reporting, analytics, logging framework or remote config | `app/build.gradle.kts:104-169` | Medium |
| FND-OPS-06 | Dependency drift and dead weight (RD-02 §3–4) | RD-02 | Medium |
| FND-OPS-07 | Documentation drift on versioning; one test hard-asserts `VERSION_CODE == 2` / `"1.1.0-beta.1"` and would fail under CI overrides | `SemVerAndUpdateTest.kt:146-148` | Low |

## 10. Tests

| ID | Finding | Evidence |
|---|---|---|
| FND-TEST-01 | 161 JVM/Robolectric `@Test` methods in 24 files + 1 trivial instrumented test; 3 trivial tests; **never executed in CI**; documented pass counts (17/30/41/53/69/161) cannot be verified | test tree; RD-05 |
| FND-TEST-02 | Untested: `SpeakCraftRepository` and `ActivityFirestoreRepository` behaviour, auth, App Check, `LearningProgressManager`, plan engines, SRS, notification sync, real M4A decode path, recorder, TTS, NavHost/app shell, permission denial, offline, rotation, font scaling | grep |
| FND-TEST-03 | False assurance: the zero-cloud test exercises an interceptor attached to no client; a model test asserts MediaPipe models are "bundled" without checking files; activity score tests assert only `in 1..100` | `ZeroCloudAiNetworkBoundaryTest.kt`; `LocalAIFoundationTest.kt:80-88`; `ActivityEngineUnitTest.kt:204` |
| FND-TEST-04 | `firestore.test.js` (4 tests) is not runnable as committed (no `package.json`) and does not cover the broken `speakingSessions` path | `firestore.test.js` |

**Preserve:** `CoachEvaluationEvidenceTest` (15), `LiveTranscriptionManagerTest` state-machine approach with fakes, `SemVerAndUpdateTest` pure-logic style, Robolectric Compose test harness.

## 11. Duplication inventory (VERIFIED)

| Concept | Implementations | Target (one) |
|---|---|---|
| Daily plan | `DailyPracticeEngine` (activities), `DailyPracticePlanEngine` (dashboard), `CurriculumCatalog.dailySteps` (8 steps) | Plan engine in FS-03 |
| Streak | Profile `streakDays`, LPM streak, activity consistency, `max()` of two | One derived streak (SRS-BR) |
| Recommenders | `ActivityEngine.recommendNextActivities`, `DailyPracticeEngine.scoreCandidate`, `RecommendationEngine`, `DailyPracticePlanEngine` | Plan engine |
| Curriculum taxonomy | 15 content modules, 10 activity modules, 5 dashboard chip groups | 5 tracks |
| Paths | 7 structured programs, 10 learning paths, 4 "learning tracks" cards | Tracks (no separate paths) |
| Scoring | `EvidenceBasedScoringEngine`, `UniversalEvaluationEngine`, cloud parser | One evaluation pipeline (ARC-05) |
| Speech recognition | `SpeechRecognitionManager`, `LocalSpeechRecognitionEngine`, `LiveTranscriptionManager`, `TtsAndSpeechHelper.startLiveSpeechCapture` | One `RecognitionSession` |
| Capability/availability | `IntelligenceCapabilityManager`, `OfflineAIAvailabilityManager`, `DeviceCapabilityManager`, `SpeechRecognitionManager.checkAvailability`, `LiveTranscriptionManager.refreshFromSettings` | One `SpeechCapabilityService` |
| Feedback generation | `LocalFeedbackEngine` + `DynamicLocalCoachEngine` (live), `DeterministicFeedbackTemplates.buildFeedback` (test-only), `LocalFeedbackGenerationEngine` (unreferenced) | One feedback composer |
| Vocabulary store | Firestore `VocabularyEntry` (live), LPM 5-stage model (dead) | One word-progress model |
| Notifications | `NotificationManagerRepository` + drawer, LPM notifications + dead sheet | Reminders + inbox (FS-09) |
| Tutorials | 4 mechanisms | Contextual tips |
| Cloud AI | `GeminiCoachService` (throws), `FirebaseAiLogicService` (gated) | None (ADR-003) |

## 12. Hard-coded values and mock data (selected, VERIFIED)

Default profile skill scores 62/65/60/68/58/66 and confidence 55; new-profile `streakDays = 1`; activity base score 56 and clamp 30–96; grammar baseline 78; fluency baseline 38; cloud fallback 74/floor 20; ASR confidence 0.85/0.94/0.78/0.55; 3 minutes per step; `activeDaysThisWeek` clamped to 5; dashboard fallbacks (score 62, trend 65, 6 tasks, ≥ 9 lessons); static KPI pills ("Weekly Goal: 5 h"); hard-coded counts in copy ("150 drills", "24 features", "15 @askvinh modules", "9 AI evaluators"); hard-coded `Locale.US` for TTS and one recogniser; a stray generated-reasoning comment in `EvaluationEvidenceValidator.kt:240`.

## 13. Assets worth preserving

| Asset | Why | Where it goes |
|---|---|---|
| Evidence gate and nullable-score model | Honest, fail-closed, well tested | ARC-05 evaluation pipeline |
| Audio DSP building blocks (RMS, VAD, pauses, resampling) | Correct in principle; need streaming rewrite and bug fixes | ARC-05 metric engine |
| Word-alignment algorithm (`PronunciationAnalysisEngine`) | Useful for read-aloud "recognised as intended" metric — relabelled | ARC-05 |
| Vocabulary evaluator stages 2 and 4 logic | Starting point for FS-07 stage checks | FS-07 |
| SM-2 scheduler (with fixes) and idempotent event model in `LearningModels` | Matches PO-REQ-1119 intent | FS-07, ADR-014 |
| Activity catalogue text (150 activities) | Hand-written prompts reusable after review and de-branding | FS-06 content |
| Firestore per-user rule patterns | Strong isolation patterns | SEC-01 target rules |
| Robolectric + Compose test harness; fakes-based tests | Fast JVM UI tests | TST-01 |
| Adaptive layout tiers and tokenised colour roles | Sound foundations | DES-05 |
| Credential Manager sign-in | Modern, correct | FS-01 |

## 14. Findings summary by severity

| Severity | Count | IDs |
|---|---|---|
| Critical | 9 | FND-AI-01, AI-02, AI-04, AI-08, DOM-04, DOM-09, DATA-06, SEC-02, (UI-level impact of AI-01: core loop non-functional) |
| High | 40+ | See tables |
| Medium | ~25 | See tables |
| Low | ~6 | See tables |

**Overall assessment.** The codebase contains capable building blocks and one genuinely good subsystem (the evidence gate), but the product it implements is fragmented into parallel systems, its central promise (useful speaking feedback) does not function by default, several displays are fabricated or mislabelled, and its release and privacy posture is not safe for wider distribution. The recommended course is a **restructure on the existing stack** — not a patch-up of the current UI and not a re-platform (RD-06, ARC-01).
