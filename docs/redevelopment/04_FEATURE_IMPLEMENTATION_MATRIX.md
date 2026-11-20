# 04 — Feature Implementation Matrix

| Field | Value |
|---|---|
| Document ID | RD-04 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Analyst / QA Architect |
| Approval | Pending — Product Owner |
| Related | RD-01 `01_DOCUMENT_INVENTORY.md`, RD-03 `03_SOURCE_CODE_AUDIT.md`, RD-05 `05_DOCUMENT_CONFLICT_REGISTER.md`, RD-06 `06_EXISTING_SYSTEM_GAP_ANALYSIS.md`, RD-10 `10_FEATURE_VALUE_ASSESSMENT.md` |

> RD-04 is the **single feature list** for the existing system. It replaces the feature statuses in DOC-004 and DOC-005 (RD-01 §5.5) and must not be re-stated elsewhere (GOV-02 DC-2). Capability IDs `CAP-nn` are defined in RD-16 §2.

---

## 1. Purpose, method and classification legend

### 1.1 Purpose

The project was built through many AI-agent prompts in three days (2026-11-18 to 2026-11-20). Each prompt left documents that self-report features as "Implemented", "Fixed" or "Verified". This matrix compares **what the documents say** with **what the code at HEAD `68a9cb1` actually does**, one feature per row, and recommends what happens to each feature in the redesign.

### 1.2 Method

| Step | What was done |
|---|---|
| 1. Claims | Every documented claim was taken from the 59 inventoried documents (`DOC-001`…`DOC-059`, RD-01), using the claims extract prepared during the RD-01 inventory. `DOC-059` is the Product Owner's 19-Nov request, also called **PO-REQ-1119**. |
| 2. Evidence | Each claim was matched against the four Stage A code audits (UI and navigation; learning/data domain; AI, speech and media; platform, security, build and release). These audits are consolidated in RD-03, and their canonical finding IDs (`FND-<AREA>-nn`) are used here. |
| 3. Spot checks | Where audits disagreed or were silent, the analyst read or grepped the code directly. Checks made for this document: the `speakingSessions` rules allow-list against the save payload; `LiveTranscriptPanel` call sites; evidence-gate recovery copy; update-telemetry statuses actually emitted; the conversation scenario-switch guard; `VideoReviewPlayerCard` and `CloudSyncWarningBanner` call sites; account-deletion callers; the 19 MB media cap; `AppAlertCategory` values; absence of `firestore.indexes.json`; and the `@Test` count at 9 historical commits (`git show`). |
| 4. Classification | Each row received exactly one status from §1.3 and one disposition from §1.4. |

**Nothing was executed.** There was no build, no unit or instrumented test run, no emulator, no Firestore emulator and no device. **No claimed test result, deployment, benchmark or device run was reproduced** (GOV-02 DC-4). Every "N/N passed" claim is listed in §5 as UNVERIFIABLE.

**Labelling (GOV-02 DC-3).** Evidence citations are **VERIFIED** (seen in source or config at the cited line) unless marked **[I]** (INFERRED: reasoned from code and platform behaviour, not observed running). The Status column is the analyst's classification derived from that evidence. The Disposition column is a **RECOMMENDATION** aligned with RD-16; no DECISION exists yet.

### 1.3 Status legend

| Status | Meaning in this matrix |
|---|---|
| **DOCUMENTED_AND_IMPLEMENTED** | The documented behaviour exists and is wired to reachable UI or a running path. Minor defects may be noted. |
| **DOCUMENTED_BUT_NOT_IMPLEMENTED** | Documented (as built, requested or proposed) but no reachable implementation exists. |
| **IMPLEMENTED_BUT_NOT_DOCUMENTED** | Reachable in code, but no document describes it (at most a passing mention or a commit message). |
| **PARTIALLY_IMPLEMENTED** | Some documented behaviour is reachable and some is missing, or the code exists but is not wired. Rows whose core code is unreachable carry the annotation **[dead]** in Notes. |
| **BROKEN** | Reachable, but does not achieve the documented outcome at runtime: it fails, is blocked (for example by security rules), or produces output that misrepresents what was measured. |
| **DEPRECATED** | Superseded or switched off; any remaining code is unreachable (**[dead]**). |
| **DUPLICATED** | Implemented more than once in parallel, contrary to a single-system intent; the copies disagree. |
| **UNKNOWN** | Cannot be determined statically. |

**Rules applied.**
1. A feature is never marked implemented without code or test evidence.
2. "Implemented" means **wired to reachable UI or a running code path**. Code that merely exists, with no production caller, is shown as PARTIALLY_IMPLEMENTED **[dead]** or DEPRECATED **[dead]**.
3. A test that exists is evidence of code, not of a passing result.
4. Where several documents describe the same feature differently, the row cites all of them and the conflict (`CONF-nnn`).

### 1.4 Disposition legend (RECOMMENDATION)

| Disposition | Meaning |
|---|---|
| **Keep** | Retain the behaviour or principle; carry it into the named capability as is. |
| **Rebuild** | The capability is needed, but the current implementation is not fit to carry forward. |
| **Merge into CAP-nn** | Fold this feature (and its duplicates) into one capability. |
| **Remove** | Retire it. Where a replacement exists it is named. |
| **Defer** | Not in R1; reconsider in the release named. |

"Cross-cutting" targets (DES-02, DES-05, DES-06, DES-07, SEC-01, ENG-01, TST-01, ARC-03) are platform or design work without a user-facing CAP (RD-16 §2).

### 1.5 Conventions

- Source paths are relative to `app/src/main/java/com/example/` and are cited by **file name** (all names are unique). §2.1 gives the folder for each file. Root files (`firestore.rules`, `firestore.test.js`, `app/build.gradle.kts`, `AndroidManifest.xml`, `.github/workflows/release_on_push.yml`, `res/…`) are named explicitly.
- "Ph*n*" means DOC-059 Phase *n*. "CUJ" means `SpeakCraftComposeCujTest.kt`.
- `D-nn` decisions, `NG-nn` non-goals, `PP-n` principles and `SPK-nn` spikes are defined in RD-99, RD-16 and RD-15.

---

## 2. Feature implementation matrix

### 2.1 File locator

| Folder (under `app/src/main/java/com/example/`) | Files cited |
|---|---|
| `ui/` | `AppNavigation.kt` |
| `ui/screens/` | `AuthAndOnboardingScreens.kt`, `DashboardScreen.kt`, `UserProfileScreen.kt`, `SpeakingAndChallengesScreens.kt`, `LabsAndTrainingScreens.kt`, `LessonAndModulesScreens.kt`, `ConversationAndMentorScreens.kt`, `YouTubeIntelligenceScreen.kt`, `VocabularyScreen.kt`, `DailyAndProgramsScreens.kt`, `ActivityDiscoveryScreen.kt`, `UniversalActivityScreen.kt`, `AnalyticsAndSettingsScreens.kt`, `AppUpdateScreen.kt`, `AdminDashboardScreen.kt` |
| `ui/components/` | `DashboardStudioWidgets.kt`, `CoachComponents.kt`, `ExerciseTemplateRenderers.kt`, `FeatureTutorialComponents.kt`, `TutorialOverlay.kt`, `LaunchAnimationOverlay.kt`, `NotificationDrawer.kt`, `NotificationsCenterSheet.kt`, `UpdateAvailableDialog.kt` |
| `ui/viewmodel/` | `MainCoachViewModel.kt`, `SpeakingAndLabsViewModel.kt`, `InteractiveCoachViewModel.kt`, `ActivityEngineViewModel.kt`, `UpdateAndAdminViewModel.kt` |
| `ui/theme/` | `Color.kt`, `Theme.kt`, `Type.kt`, `Spacing.kt` |
| `data/firebase/` | `AuthHelper.kt`, `SpeakCraftRepository.kt`, `ActivityFirestoreRepository.kt`, `AppCheckConfig.kt`, `FirestoreErrorHandler.kt` |
| `data/model/` | `CurriculumCatalog.kt`, `FirestoreModels.kt`, `FeatureTutorialRegistry.kt`, `StreakAndAnalyticsHelper.kt` |
| `data/activity/` | `ActivityModels.kt`, `ActivityCatalogue.kt`, `ActivityCatalogueModules1To3.kt` / `4To6.kt` / `7To10.kt`, `ActivityEngine.kt`, `UniversalEvaluationEngine.kt`, `DailyPracticeEngine.kt`, `LearningPathsEngine.kt`, `ActivityProgressAnalyticsEngine.kt` |
| `data/learning/` | `LearningModels.kt`, `LearningProgressManager.kt`, `DailyPracticePlanEngine.kt`, `StreakAndCompletionEngine.kt`, `SpacedRepetitionScheduler.kt`, `LocalVocabularyQuizEngine.kt` |
| `data/evaluation/` | `EvaluationModels.kt`, `EvaluationEvidenceValidator.kt`, `EvidenceBasedScoringEngine.kt` |
| `data/notifications/` | `NotificationManagerRepository.kt`, `NotificationModels.kt` |
| `data/preferences/` | `AppPreferencesRepository.kt`, `AISettingsRepository.kt`, `TutorialState.kt` |
| `data/media/` | `AudioRecorderManager.kt`, `CameraVideoRecorder.kt`, `TtsAndSpeechHelper.kt` |
| `data/ai/` | `HybridAiCoachCoordinator.kt`, `GeminiCoachService.kt`, `RecommendationEngine.kt`, `IntelligenceCapabilityManager.kt` |
| `data/ai/cloud/` | `FirebaseAiLogicService.kt` |
| `data/ai/local/` | `LocalExecutionPolicy.kt`, `LocalIntelligenceOrchestrator.kt`, `OnDeviceModelManager.kt`, `DeviceCapabilityManager.kt`, `OfflineAIAvailabilityManager.kt` |
| `data/ai/local/audio/` | `LiveTranscriptionManager.kt`, `SpeechRecognitionManager.kt`, `LocalSpeechRecognitionEngine.kt`, `AudioFeatureExtractionEngine.kt`, `AudioResampler.kt`, `PronunciationAnalysisEngine.kt`, `FillerWordDetectionEngine.kt` |
| `data/ai/local/language/` | `LocalLanguageAnalysisEngine.kt`, `LocalFeedbackEngine.kt`, `DynamicLocalCoachEngine.kt`, `LocalVocabularyEvaluator.kt` |
| `data/ai/local/video/`, `data/ai/local/fusion/` | `VideoFrameProcessingEngine.kt`, `FaceLandmarkAnalysisEngine.kt`, `BodyLanguageAnalysisEngine.kt`; `MultimodalFeatureFusionEngine.kt` |
| `data/update/`, `data/youtube/` | `ReleaseUpdateRepository.kt`, `GitHubReleaseClient.kt`, `ApkIntegrityVerifier.kt`, `UpdateBackupManager.kt`, `SemVer.kt`; `YouTubeIntelligenceClient.kt` |
| Tests (`app/src/test/java/com/example/`) | `SpeakCraftComposeCujTest.kt` (CUJ), `ActivityEngineUnitTest.kt`, `CoachEvaluationEvidenceTest.kt`, `LiveTranscriptionManagerTest.kt`, `CentralizedAiSettingsTest.kt`, `NotificationManagementSystemTest.kt`, `StreakAnalyticsAndBackupTest.kt`, `SemVerAndUpdateTest.kt`, `LocalAIFoundationTest.kt`, `ZeroCloudAiNetworkBoundaryTest.kt`, and others named in rows |

### 2.2 Authentication and account

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-001 | Auth | Google sign-in with session check | Credential Manager silent auto-sign-in plus interactive Google option; anonymous auth prohibited; "Checking Google session…" indicator · DOC-001, DOC-002, DOC-005 (FEAT-001), DOC-012, DOC-017 | `AuthHelper.kt:41-82` (silent), `:84-124` (interactive); `AuthAndOnboardingScreens.kt:92-102, 159-183, 239-252`; anonymous/email sign-in: 0 hits | DOCUMENTED_AND_IMPLEMENTED | Whole app gated behind Google (FND-SEC-01); button not disabled during session check (`AuthAndOnboardingScreens.kt:254`); raw exception text shown (`AuthHelper.kt:121`); local build needs `google-services.json` (FND-OPS-02); untested | Rebuild → CAP-01 (sign-in optional, D-05) |
| FM-002 | Auth | Sign-out | Sign out with confirmation; clears the session without deleting history · DOC-001, DOC-059 Ph6 | `AuthHelper.kt:126-143`; confirmation in Profile (`UserProfileScreen.kt:125`); Settings sign-out has none (`AppNavigation.kt:1143`) | PARTIALLY_IMPLEMENTED | No local cleanup; recordings not partitioned per user; previous user's ViewModels not cleared [I] (FND-SEC-09) | Rebuild → CAP-01 |
| FM-003 | Auth | Account and data deletion | Delete account and data with re-authentication and verifiable cleanup · DOC-059 Ph6/Ph7 (DOC-007 recorded it as a gap) | `SpeakCraftRepository.kt:1292-1319` deletes 19 subcollections, profile, then `currentUser.delete()`; wrapper `MainCoachViewModel.kt:474` has 0 callers | PARTIALLY_IMPLEMENTED | **[dead]** No UI entry; no re-auth (0 hits), so a `requires-recent-login` failure leaves data deleted but the account alive; misses local JSON stores, `applicationUpdateEvents` and unreferenced recordings; errors swallowed (`:1302`) (FND-SEC-03). Code undocumented (RD-01 §5.4) | Rebuild → CAP-10 (+ CAP-01) |

### 2.3 Onboarding and profile

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-004 | Onboarding | Onboarding wizard | 3-step wizard (Identity & Level / Goal & Pace / Skill Baseline) with step indicator; auto-enrol in the 30-day challenge · DOC-001, DOC-005 (FEAT-002), DOC-012, DOC-015–017 | `AuthAndOnboardingScreens.kt:331-631` (steps `:413-417`, indicator `:411`); enrol and seed `MainCoachViewModel.kt:228-230`; profile write `SpeakCraftRepository.kt:469-528`; CUJ `:269-299` | PARTIALLY_IMPLEMENTED | No validation, skip or submit state; failure snackbar has no host during onboarding (`AppNavigation.kt:695`); two minutes controls; self-rated sliders later shown as "Objective" skills (`AnalyticsAndSettingsScreens.kt:101-110`) | Rebuild → CAP-02 |
| FM-005 | Onboarding | Level taxonomy and level determination | 4 levels (DOC-005, DOC-058); 5 levels with manual correction and adaptive difficulty (DOC-059 Ph2) | Onboarding offers 4 (`AuthAndOnboardingScreens.kt:450`); profile editor 5 (`UserProfileScreen.kt:76-82`); `LearningModels.kt:9-28` has 5; `ActivityModels.kt:50-54` has 3; level presets only, no placement or progression (`AuthAndOnboardingScreens.kt:363-380`) | DUPLICATED | Three level vocabularies; "Upper-Intermediate" vs "Upper Intermediate"; activity plan maps unknown levels to Intermediate (`DailyPracticeEngine.kt:60-64`) (CONF-032; FND-DOM-12) | Rebuild → CAP-02 (D-17) |
| FM-006 | Onboarding | Learning profile drives personalisation | `UserLearningProfile` (goals, current and target level, weaknesses, schedule, interests) feeds the daily plan · DOC-059 Ph2 | `LearningModels.kt:375-400`; profile goal used only when `selectedGoals` is empty, but the default is non-empty (`LearningProgressManager.kt:86-88`, `LearningModels.kt:380`); target level, interests, preferred time never collected (`LearningModels.kt:379-383`); `updateLearningPreferences` 0 callers | PARTIALLY_IMPLEMENTED | The user's chosen goal never reaches the dashboard plan (FND-DOM-03) | Rebuild → CAP-02 / CAP-03 |
| FM-007 | Onboarding | Profile screen | Picture, name, email, level, goals, member since, streak, time, activities, vocabulary · DOC-059 Ph6 | `UserProfileScreen.kt:96-532` (header `:180-296`, editor `:299-398`, stats `:401-446`, links `:485-529`); CUJ `:698-737` | PARTIALLY_IMPLEMENTED | Initials only; no member-since or activity count; streak renders "1 days" (`:410`); level and goal chips differ from onboarding; reachable only from the top-bar avatar (dashboard `onOpenProfile` is dead, `DashboardScreen.kt:162`); undocumented screen (RD-01 §5.4) | Rebuild → CAP-10 |

### 2.4 Navigation and information architecture

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-008 | Nav/IA | Primary navigation (5 bottom tabs) | 5 tabs Home / Daily / Record / @askvinh / Mentor (DOC-001); 5 destinations with a proposed relabel to Practice / Studio / Curriculum (DOC-010) | `AppNavigation.kt:177-183, 828-857`; labels still "Daily / Record / @askvinh" (`:179-181`); 26 NavHost destinations (`:915-1236`) | DOCUMENTED_AND_IMPLEMENTED | Relabel never done (CONF-022); documented route counts 22/24 are wrong (CONF-021, CONF-025); one primary tab is a YouTube/creator tool; 21 routes are reached through the dashboard directory (FND-UI-01) | Rebuild → 4-tab IA (DES-02): CAP-03, CAP-06, CAP-07, CAP-08 |
| FM-009 | Nav/IA | Adaptive shell and tablet layouts | Rail at ≥600dp, expanded tier at ≥840dp; tablet 4-up metrics, 3-column grid, containers ≤1160dp (DOC-016, DOC-017; DOC-001 said 720dp) | `AppNavigation.kt:677-679`; rail `:861-905`; 10-item sidebar `:1329-1547`; dashboard expanded two-column with 1280dp max (`DashboardScreen.kt:323, 384-481`); `Spacing.kt:18-47`; `SpeakCraftTabletLayoutTest` (7) | PARTIALLY_IMPLEMENTED | Rail omits settings, vocabulary, analytics and the activity library; sidebar labels differ ("Tools" → vocabulary) and its items stack back entries (`AppNavigation.kt:511, 746`); sidebar shell undocumented except a DOC-022 mention (CONF-024) | Rebuild → cross-cutting (DES-02) |

### 2.5 Dashboard and welcome

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-010 | Dashboard | Home layout: hero, hubs, directory, search | Hero next action from `RecommendationEngine`, KPI tiles and a 22-tile module wall (DOC-001, DOC-005 FEAT-005); redesign with 4 quick-action hubs and a searchable directory (DOC-010, DOC-012, DOC-017) | Hero `DashboardStudioWidgets.kt:191-476`; recommendation `MainCoachViewModel.kt:154-170`; KPI strip `DashboardScreen.kt:808-858`; search `:348-382`; 22/23-tile directory `:285-307`; 8-tile Quick Access `DashboardStudioWidgets.kt:1277-1284`; no 4-hub grid | DUPLICATED | Up to 16 sections, ≈70 targets, 25 routes; Quick Access repeats 7 directory tiles; the 4-hub set was never built and its contents conflict between documents (CONF-023); recommender matches "um" inside other words (`RecommendationEngine.kt:34`) (FND-UI-02) | Remove → replaced by CAP-03 (Today) |
| FM-011 | Dashboard | Welcome-back greeting | Personalised greeting with avatar, track, day, score and message; respects reduced motion; no replay on recomposition · DOC-059 Ph1.1 | `DashboardStudioWidgets.kt:127-189` (one animated line "Welcome back, {name}", `:164`); `learningTrack`, `learningDay`, `overallScore`, `personalizedCue`, `onAvatarClick` unused (`:130-135`); cue computed then discarded (`DashboardScreen.kt:214-226`); CUJ `:685-695` | PARTIALLY_IMPLEMENTED | No avatar; brand-new users also see "Welcome back"; `reducedMotion` never passed (`AppNavigation.kt:916-946` vs `DashboardScreen.kt:160`) | Rebuild → CAP-03 |
| FM-012 | Dashboard | Learning-plan carousel | HorizontalPager with snap, indicator dots (not primary), Start Lesson / Quick Practice; example cards · DOC-059 Ph1.2–1.3 | Pager `DashboardStudioWidgets.kt:247-260`; 4 slides `:205-243`; buttons `:398-441`; dots `:453-471` | DOCUMENTED_AND_IMPLEMENTED | Slides 1–3 are the prompt's sample copy; the same image on every slide (`:284-289`); slide 1 "3 Vs" opens the 3-Stage Audit lesson (`:209-212`); fixed 252dp height clips at large font (`:282`); swipe untested | Remove → single session card in CAP-03 |
| FM-013 | Dashboard | Real-time dashboard (12 sections) | Welcome, plan, completion %, streak, time, performance, vocabulary, level, weekly chart, next step, achievements, continue learning; updates only on validated, de-duplicated completion events · DOC-059 Ph3, Ph11 | Plan `DashboardScreen.kt:186` + `DashboardStudioWidgets.kt:487-694`; ring `:805-855`; streak `:703-760`; trend `DashboardScreen.kt:615-622`; achievements never rendered (grep); "continue learning" 0 hits; weekly view is a 5-dot row (`DashboardStudioWidgets.kt:887-984`) | PARTIALLY_IMPLEMENTED | About 9 of 12 sections; completion is a self-reported tick (`:639`), not a validated event (FND-DOM-06); state kept in local JSON, not Room (FM-056) | Rebuild → CAP-03 + CAP-08 |
| FM-014 | Dashboard | Data-backed values only | "No fake statistics, buttons that do nothing, or progress values not backed by completed activities" · DOC-059 (acceptance) | Fallbacks: `overallScore` 62 (`DashboardScreen.kt:181`), 6 tasks (`:189`), pronunciation trend 65 (`:238`), static KPI pills (`:766-799`); fixed 34-minute fallback plan (`DashboardStudioWidgets.kt:520-525`); profile seeded `streakDays = 1` (`SpeakCraftRepository.kt:494`) and floored at 1 (`DashboardStudioWidgets.kt:703`) | BROKEN | New users see "1 Day Streak" and a score of 62 (FND-DOM-08); see FM-052 | Rebuild → CAP-08 (PP-1) |

### 2.6 Daily practice and plans

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-015 | Daily | Legacy daily checklist | 9 steps, 3 rotated daily words, reflection, confidence 1–10 · DOC-001, DOC-005 (FEAT-024) | 8 steps `CurriculumCatalog.kt:989-998` (subtitle says "9-step", `:170`); `DailyAndProgramsScreens.kt:59-214`; minutes = steps × 3 (`MainCoachViewModel.kt:275`); daily words computed `by lazy`, stale after midnight (`:172-179`) | DUPLICATED | One of three live daily-plan systems with separate stores (this one: Firestore `dailyActivities`), contrary to DOC-059 "no parallel progress systems" (CONF-045; FND-DOM-01, FND-DOM-06) | Merge into CAP-03 |
| FM-016 | Daily | Activity daily plan (`DailyPracticeEngine`) | 10/20/30/45/60-minute plans from the 150 activities; anti-repetition, level, weakest-skill and goal weights; daily rotation; block swap; persisted to `dailyPracticePlans` · DOC-026, DOC-029, DOC-031 | `DailyPracticeEngine.kt:49-184, 230-268`; called from `ActivityEngineViewModel.kt:469-525`; shown only in the Activity Library (`ActivityDiscoveryScreen.kt:112-114`); `ActivityEngineUnitTest.kt:213-252` | PARTIALLY_IMPLEMENTED | Swap is a visible no-op (`ActivityDiscoveryScreen.kt:112-114`); regeneration after each attempt replaces just-completed items [I]; persisted plan never read back; minutes reset to 20 (`ActivityEngineViewModel.kt:108`) (FND-DOM-10) | Merge into CAP-03 |
| FM-017 | Daily | Personalised daily plan (`DailyPracticePlanEngine`) | Deterministic plan from goals, levels, scores, mistakes, vocabulary and time; adaptive; recovery after a missed day; versioned and stable within a session · DOC-059 Ph2 | `DailyPracticePlanEngine.kt:31-243` (recovery `:34`, task cap `:213-221`); called only from `LearningProgressManager.kt:99-117`; rendered on the dashboard (`DashboardScreen.kt:186`) | PARTIALLY_IMPLEMENTED | Generic route tasks, not content; goal ignored (FM-006); completed by tap only; substring remediation ("um") [I]; untested; undocumented (CONF-044, CONF-045) | Rebuild → CAP-03 (one plan engine) |

### 2.7 Curriculum, lessons, programmes and paths

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-018 | Curriculum | Legacy curriculum taxonomy | 15 communication modules, 20 categories (DOC-001, DOC-002, DOC-005 FEAT-007/008); future remote content packs (DOC-030) | `CurriculumCatalog.kt:143-164` (20), `:167-183` (15); coexists with 10 activity modules (`ActivityCatalogue.kt:107-188`); no content-pack loader (grep) | DUPLICATED | Two module taxonomies for the same topics, not cross-referenced (CONF-026; FND-DOM-01); DOC-030 content packs not implemented | Merge into CAP-06 (5 tracks; content packs R3) |
| FM-019 | Curriculum | Module lessons and lesson detail | Bundled and admin-published lessons; completion; next lesson · DOC-001, DOC-005 (FEAT-009) | 12 lessons `CurriculumCatalog.kt:521-542`; `LessonAndModulesScreens.kt:81-521` ("Mark Complete" `:426-459`); `confidence` module has 0 lessons (`CurriculumCatalog.kt:497-517`); "Next Lesson" crosses modules (`:524`) | PARTIALLY_IMPLEMENTED | Admin-published lessons forced to `sourceVerified = true` (`LessonAndModulesScreens.kt:98, 107, 276, 285`); error card without retry (`:411`) | Merge into CAP-06 (original content) |
| FM-020 | Curriculum | Creator-grounded pedagogy and "VERIFIED" provenance | @askvinh frameworks (3Vs, 5 Vocal Foundations, PREP, CLEAR …); creator advice separated from AI supplements; verified lessons with source video and timestamp · DOC-001, DOC-005, DOC-006, DOC-009 | `LessonAttributionBadge` `CoachComponents.kt:771`; 15 technique seeds, 12 "VERIFIED", all linking to the channel root `youtube.com/@askvinh` (`CurriculumCatalog.kt:204-449`); "5 Vocal Foundations" screen has 4 levers (`LessonAndModulesScreens.kt:534-541, 636`); grammar lab promises PREP/CLEAR without content (`LabsAndTrainingScreens.kt:650`) | BROKEN | Verification labels and timestamps cannot be checked; the test only checks the URL prefix (`CurriculumAndRecommendationTest.kt:43`); IP and licensing exposure (FND-DOM-09) | Remove (D-08) → original content in CAP-06 |
| FM-021 | Curriculum | Structured programmes (30/60/90-day) | 30/60/90-day programmes with daily drills · DOC-001, DOC-005 (FEAT-010) | 7 programmes `CurriculumCatalog.kt:1000-1123`; `DailyAndProgramsScreens.kt:271-381`; only the 30-day programme has day-level drills | BROKEN | 6 of 7 programmes can never complete (drills fewer than days, `DailyAndProgramsScreens.kt:353-368`); no preview or unenrol; "Hide Plan" fails for the 30-day plan | Merge into CAP-06 / CAP-03 |
| FM-022 | Curriculum | Learning paths (10) | 10 paths of 12–15 activities; prerequisites at 70%; milestones; non-linear jumps · DOC-027, DOC-031 | `LearningPathsEngine.kt:5-302`, prerequisites `:313-317`; path card opens the next activity (`ActivityDiscoveryScreen.kt:463-465`); `ActivityEngineUnitTest.kt:255-276` | PARTIALLY_IMPLEMENTED | `prerequisitesMet` computed but never shown (0 hits in `ui/`); difficulty labels inconsistent; third "paths/tracks" concept beside programmes and dashboard tracks (FND-DOM-10) | Merge into CAP-06 (tracks) |

### 2.8 Activity engine

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-023 | Activity | Universal Activity Engine pipeline | Config-driven 20-property `ActivityDefinition`, `ActivityValidator`, one `ActivityScreen(activityId)` renderer · DOC-022, DOC-024, DOC-030 | `ActivityModels.kt:156-177`; `ActivityCatalogue.kt:19-103`; `UniversalActivityScreen.kt:66-476`; `ExerciseTemplateRenderers.kt:80-451`; `ActivityEngineUnitTest` (9), `ActivityEngineComposeTest` (4) | DOCUMENTED_AND_IMPLEMENTED | Attempt wiped on rotation (`UniversalActivityScreen.kt:76-78`); blank while loading (`:139`); several config fields never read (`CompletionCriteria`, `FeedbackConfiguration`) | Merge into CAP-06 (reuse the config-driven pattern for CAP-04 templates) |
| FM-024 | Activity | Activity catalogue and templates | 150 activities in 10 modules (15/15/15/15/15/15/15/10/15/20); 25 templates in 7 interaction families; 4 lifecycle stages · DOC-023, DOC-024, DOC-031 | `ActivityCatalogue.kt:107-188`; `ActivityCatalogueModules1To3.kt`, `4To6.kt`, `7To10.kt`; `ActivityModels.kt:9-48`; `ActivityEngineUnitTest.kt:39-97` | DOCUMENTED_AND_IMPLEMENTED | Templated structure (one rubric per module, 3 instructions, 1 example each); duplicate titles; framework names under "SpeakCraft Core Curriculum" attribution (FND-DOM-13, FND-DOM-09) | Merge into CAP-06 (content reused after review; 5 R1 templates) |
| FM-025 | Activity | Discovery hub and routes | 5 tabs All 150 / Daily Plan / 10 Paths / Saved & Recent / Analytics; routes `activity_library`, `activity_library/{moduleId}`, `activity/{activityId}` · DOC-022, DOC-030 | `ActivityDiscoveryScreen.kt:74-78, 81-598`; routes `AppNavigation.kt:1204-1236`; `activity_library/{moduleId}` never navigated (`:1211-1222`); `initialTab` never passed (`ActivityDiscoveryScreen.kt:87`) | DUPLICATED | Plan, paths and analytics tabs duplicate FM-015/016, FM-021/022 and FM-051; orphan route; "Browse All" pushes a new copy (`AppNavigation.kt:1234`) | Merge into CAP-06 (library only) |
| FM-026 | Activity | Activity submission, completion and persistence | Submit → evaluate → feedback → continue; progress stored in `activityProgress`, `activityAttempts`, `savedActivities`, `dailyPracticePlans` · DOC-022, DOC-029 | Gate `ActivityEngine.kt:46-65`; completed when any attempt ≥ 60 (`:92`); `ActivityFirestoreRepository.kt:63-125`; transcript never set (`ActivityEngineViewModel.kt:354-365`); absolute `recordedMediaPath` stored (`ActivityFirestoreRepository.kt:201`) | PARTIALLY_IMPLEMENTED | Activity audio never transcribed; 3 typed words or a wordless recording completes an activity (FND-DOM-05); offline submit waits for server acknowledgement [I] (`ActivityEngineViewModel.kt:408-426`; FND-DATA-03, FND-DATA-04); analytics read only the latest 200 attempts | Rebuild → CAP-04 + CAP-11 |

### 2.9 Speaking labs and recording

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-027 | Labs | Audio recording | AAC .m4a, 44.1 kHz, 96 kbps, pause/resume, live waveform; monotonic timer and actual duration from `MediaMetadataRetriever` · DOC-001, DOC-002, DOC-004 (ISS-020), DOC-005 (FEAT-016), DOC-017 | `AudioRecorderManager.kt:57-113` (format `:72-77`), duration `:158-163, 241-253` | DOCUMENTED_AND_IMPLEMENTED | No interruption or audio-focus handling; no maximum duration; failed or discarded takes never deleted; recording continues across navigation (FND-AI-14); pause only on 2 screens | Rebuild → CAP-04 |
| FM-028 | Labs | Video capture, upload and 3-stage self-audit | CameraX SD video; Photo Picker ≤19 MB with size check before reading bytes; Mute / Vocal / Full self-audit player · DOC-001, DOC-002, DOC-004 (ISS-001, ISS-005), DOC-005 (FEAT-017–019), DOC-017 | `CameraVideoRecorder.kt:94-226`; `SpeakingAndLabsViewModel.kt:40, 346-364, 415`; `VideoReviewPlayerCard` `CoachComponents.kt:494`, used at `SpeakingAndChallengesScreens.kt:429`; CUJ `:302-332` | DOCUMENTED_AND_IMPLEMENTED | Closing the camera mid-recording still attaches the clip (`CameraVideoRecorder.kt:172-176, 211-219`); file held in memory up to 19 MB; creator-named workflow | Defer → R3 video self-review without scoring (ADR-010; NG-07) |
| FM-029 | Labs | Practice labs suite | Pronunciation (IPA, minimal pairs, 0.75×/1.0× TTS) differentiated from Articulation; Voice Training (5 Vocal Foundations); Listening & Shadowing; Grammar (PREP/CLEAR); Public Speaking; Challenges (daily/weekly, timed); Real-World Tasks · DOC-001, DOC-004 (ISS-018), DOC-005 (FEAT-025–029, 032), DOC-012, DOC-017 | All recorder labs call `SpeakingAndLabsViewModel.kt:445-556`; `articulation_lab` and `pronunciation` render one composable (`AppNavigation.kt:997-1014`; `LabsAndTrainingScreens.kt:75`); "0.75×" plays at 0.72 (`:143`); Voice Training submits as `SPEAKING_LAB` (`LessonAndModulesScreens.kt:601-602`); Challenges `SpeakingAndChallengesScreens.kt:794-1114`; Real-World Tasks `LabsAndTrainingScreens.kt:912-1020` | DUPLICATED | 10 recorder surfaces with 12 "record" and 8 "analyze" labels; scoring identical across labs [I]; articulation test checks one header only (CUJ `:370-393`); a lab can evaluate another lab's recording (`SpeakingAndLabsViewModel.kt:594-613`) (FND-UI-01) | Merge into CAP-04 (session engine) + CAP-06 (tracks × templates) |
| FM-030 | Labs | Microphone and camera permission handling | Permission-checked recording; "Check Permissions" action in the transcript panel · DOC-024 (`ActivityPermission`), DOC-055 | Activity engine shows a message on denial (`ExerciseTemplateRenderers.kt:110-124`); 7 lab recorders call `if (granted) start` with no else (`SpeakingAndChallengesScreens.kt:150-154, 835-837`; `LabsAndTrainingScreens.kt:103-105, 353-355, 556-558, 744-746`; `LessonAndModulesScreens.kt:550-552`); "Check Permissions" opens in-app Settings (`CoachComponents.kt:1199-1206`) | PARTIALLY_IMPLEMENTED | Silent failure on denial; no rationale; no Android settings deep link; untested | Rebuild → CAP-04 (SCR-PERM-01) |
| FM-031 | Labs | Speaking-session save and cloud-sync banner | Sessions saved with evaluation status, partial flag and evidence summary; a failed sync shows "Retry Cloud Sync" instead of a false success · DOC-004 (ISS-003), DOC-017, DOC-052, DOC-053 | Payload adds `evaluationStatus`, `isPartialEvaluation`, `evaluatedDimensionKeys`, `unavailableDimensionKeys`, `evidenceSummary` (`SpeakCraftRepository.kt:829-833`); none is in the `speakingSessions` `hasOnly` list (`firestore.rules:459-467`); banner `CoachComponents.kt:691`, used at `LabsAndTrainingScreens.kt:207, 429, 618` | BROKEN | Every session save is denied by the rules (regression in HEAD `68a9cb1`, static diff), dropping the feedback document and the profile score blend; the banner is therefore the normal outcome; the unsaved session lives only in memory (`SpeakingAndLabsViewModel.kt:543`) (FND-DATA-06); the partial flag would read back as false anyway (FND-DOM-15) | Rebuild → CAP-11 (local-first store + outbox) |

### 2.10 Speech recognition and transcription

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-032 | ASR | Live transcription | Android SpeechRecognizer, off by default, session-isolated, honest offline reporting (on-device only on API 31+); `LiveTranscriptPanel` on 6 screens · DOC-050, DOC-051, DOC-055 | `LiveTranscriptionManager.kt:71-455` (on-device check `:82`, fallback `:99-103`, `EXTRA_PREFER_OFFLINE` `:165`); default off `AISettingsRepository.kt:26`; panels at `LabsAndTrainingScreens.kt:161, 390, 579, 766` and `SpeakingAndChallengesScreens.kt:269, 904`; `LiveTranscriptionManagerTest` (14, fake backend) | PARTIALLY_IMPLEMENTED | Captures one utterance only (`:161-166, 363-381`); text duplicated on stop (`:418-431`) [I timing]; no `<queries>` entry for RecognitionService [I]; mic shared with MediaRecorder [I]; recogniser confidence never read; second dictation wrapper hard-codes `Locale.US` and the default (network) recogniser (`TtsAndSpeechHelper.kt:126, 218`) (FND-AI-01, FND-AI-04, FND-AI-15; CONF-011) | Rebuild → CAP-12 (+ CAP-04) |
| FM-033 | ASR | Post-hoc file transcription | Multi-tier ASR Whisper → Moonshine → Android offline → audio-only ("Transcribe Audio") · DOC-044, DOC-045 | Button `SpeakingAndChallengesScreens.kt:460-475`; `SpeechRecognitionManager.kt:88-138`, Android branch returns `""` (`:113-126`) | BROKEN | Always fails; the failure message asks users to install a model that Settings cannot install (FND-AI-02) | Remove → CAP-12 recognition tiers |
| FM-034 | ASR | On-device Whisper / Moonshine models | Whisper Tiny and Moonshine TFLite; streaming SHA-256 check before load; corrupt-model recovery · DOC-035, DOC-040, DOC-044–047, DOC-049 | `LocalSpeechRecognitionEngine.kt:121-459` (invented vocabulary `:426-459`, non-standard log-mel `:342-393`, Moonshine never run `:76`); catalogue URL is a GGML `.bin` (`OnDeviceModelManager.kt:84`); every `sha256Hash = ""` (`:83-139`) and a blank hash passes (`:191`); no downloader (grep); `assets/models/` holds only `README.md` and `.gitkeep` | BROKEN | Cannot produce a transcript even if a model were present (FND-AI-03; CONF-012, CONF-018); DOC-046 benchmarks unsupported | Remove (SPK-03 research, R3) |
| FM-035 | ASR | Device capability tiers | BASIC / STANDARD / ADVANCED by RAM and API level; concurrency at ≥3 GB · DOC-041, DOC-045, DOC-047, DOC-048 | `DeviceCapabilityManager.kt:83-109`; thresholds `:146-148`; concurrency `:94` | PARTIALLY_IMPLEMENTED | **[dead]** Profile, NPU, thermal, battery and concurrency outputs are computed but never consumed (grep); KDoc contradicts the thresholds (`:21-23`) (CONF-016) | Rebuild → CAP-12 (speech tiers T1/T2/T3) |

### 2.11 Evaluation and scoring

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-036 | Evaluation | Evidence gate and evaluation lifecycle | Audio must exist, decode, last ≥1.5 s, RMS ≥0.008, ≥0.5 s voiced; transcript session-scoped with ≥2 words; nullable scores; no weight redistribution; partial label; 12-state lifecycle; recovery copy · DOC-051, DOC-052, DOC-053 | `EvaluationEvidenceValidator.kt:42-45, 49-290`, copy `:93-155`; `EvaluationModels.kt:9-22`; fail-closed `EvidenceBasedScoringEngine.kt:49-75, 146-175`; `CoachEvaluationEvidenceTest` (15) | DOCUMENTED_AND_IMPLEMENTED | Strongest component (FND-AI-17); tests use WAV, so the real M4A/AAC decode path is untested; the dormant cloud branch would bypass it (`HybridAiCoachCoordinator.kt:198-212`) | Keep → CAP-05 |
| FM-037 | Evaluation | Removal of fabricated baselines | Baselines 78 / 72 / 35, duration coercion and "~78/100 on empty input" root-caused and fixed · DOC-052 | Empty or short input now rejected (FM-036); grammar still `78 + bonus − penalties` (`EvidenceBasedScoringEngine.kt:533-543`); fluency starts at 38 (`:355`), audio-only pronunciation at 20 (`:438`), delivery at 18 (`:487`); dormant cloud parser defaults to 76/74 (`FirebaseAiLogicService.kt:283-287, 783`) | PARTIALLY_IMPLEMENTED | File header claims it "never returns 78/100" (`EvidenceBasedScoringEngine.kt:17-18`); confidence values are constants (`:384, 421, 445`) (CONF-010; FND-AI-08) | Rebuild → CAP-05 (measured vs estimated, ARC-05) |
| FM-038 | Evaluation | Pronunciation assessment | 5-layer assessment with Indian-English accent tolerance; no phoneme-level claims · DOC-044, DOC-045 | Word alignment of ASR text (`PronunciationAnalysisEngine.kt:40-41, 119-207`) runs only when instructions contain "Expected:" (`LocalIntelligenceOrchestrator.kt:257-260, 938-943`), which no screen sends; free-speech "Pronunciation & Clarity" = loudness + pitch spread + voicing (`EvidenceBasedScoringEngine.kt:428-453`); accent pairs map to non-words (`PronunciationAnalysisEngine.kt:95-113`) | BROKEN | The score label misrepresents what is measured; "Compare AI" never compares with the target phrase (FND-AI-08) | Rebuild → CAP-05 ("recognised as intended" for read/repeat only; NG-03) |
| FM-039 | Evaluation | Audio DSP features and fluency / delivery scores | RMS, clipping ≥32,000, 20 ms VAD at 0.015, pitch 80–400 Hz, pauses 300/750 ms; audio up to 10 minutes · DOC-035, DOC-044, DOC-045, DOC-048 | `AudioFeatureExtractionEngine.kt:57-223`; `AudioResampler.kt:14-15, 32, 47` (600 s); fluency `EvidenceBasedScoringEngine.kt:354-372`; delivery `:474-487` | PARTIALLY_IMPLEMENTED | Noise bounded below 0.015, so every noise check is unreachable (`AudioFeatureExtractionEngine.kt:97-108`); rate consistency always 1.0, adding +12 fluency and a fabricated "100%" strength (`LocalIntelligenceOrchestrator.kt:114`); one loud sample flags clipping (`:71, 75`); boxed `ArrayList<Short>` OOM risk (`AudioResampler.kt:199, 247-249`) (FND-AI-07, FND-AI-13; CONF-013) | Rebuild → CAP-05 (ARC-05 measured metrics) |
| FM-040 | Evaluation | Grammar rules and filler detection | 11 grammar categories with rule IDs (SVA_001 …); filler lexicon (8 words in DOC-025, 11 in DOC-035); WPM clamp 30–320 · DOC-025, DOC-035, DOC-044, DOC-045 | ≈40 regex rules `LocalLanguageAnalysisEngine.kt:76-499`; confidence gate effectively constant (`:524-527`); sentences split on `.!?` (`:683-684`); `FillerWordDetectionEngine.kt:33-37` (11 words) | PARTIALLY_IMPLEMENTED | Unpunctuated ASR text becomes one sentence, triggering run-on penalties; known false positives ("tools that don't", "a utility", "a two year contract"); "like" and "right" counted as fillers while ASR usually drops "um/uh" [I] (FND-AI-09; CONF-015) | Rebuild → CAP-05 (high-precision rules; SPK-02) |
| FM-041 | Evaluation | Activity scoring by nine local evaluators | Nine evaluators (Speaking … Reflection) migrated to local deterministic and multimodal evaluation; rubric dimension scores; strict honesty · DOC-039 (cloud versions in DOC-022, DOC-025), DOC-028 | The 9 strategy classes only produce caveat and prompt text (`UniversalEvaluationEngine.kt:19-101`); score = `56 + length + media + checklist + discourse − fillers − grammar`, clamped 30–96 (`:258-279`); dimension spread `base+2` / `base−idx` (`:281-282`); video counted as analysed when the file exists (`:200-201`) | BROKEN | A wordless valid recording scores 64 and passes; 3 typed words pass; "Articulation Crispness" is scored from typed text; rubric weights ignored; tests assert only `in 1..100` (CONF-002, CONF-010; FND-DOM-04) | Rebuild → CAP-05 |
| FM-042 | Evaluation | Structured feedback and objective/confidence separation | Six sections (what went well, mistakes, corrected examples, explanation, suggestions, next task) plus honesty notice; objective score never changed by self-rated confidence · DOC-025, DOC-028, DOC-031, DOC-039, DOC-045 | `ActivityModels.kt:217-236`; `UniversalEvaluationEngine.kt:365`; `LocalFeedbackEngine.kt:410-483`; `ActivityEngineUnitTest.kt:143-178` | DOCUMENTED_AND_IMPLEMENTED | Template text; honesty caveats promise an audio review that does not affect the score (`UniversalEvaluationEngine.kt:35-40`); two feedback renderers (`CoachComponents.kt:1326-1842`, `ExerciseTemplateRenderers.kt:645-787`); legacy Analytics shows self-ratings as "Objective" (FM-051) | Keep (principle) → CAP-05 (one strength + one focus) |
| FM-043 | Evaluation | Video analysis and multimodal fusion | 1 fps, ≤640 px, MediaPipe face/pose/hand; no emotion inference; modality-independent fusion with cross-modal rules · DOC-036, DOC-037, DOC-041, DOC-047, DOC-049 | `VideoFrameProcessingEngine.kt:141, 209` (640 px); landmarkers return false without `.task` assets (`FaceLandmarkAnalysisEngine.kt:46-55`), and the assets are absent; `headTiltAlert` measures shoulder unevenness (`BodyLanguageAnalysisEngine.kt:167`); fusion is a field mapper with no cross-modal logic (`MultimodalFeatureFusionEngine.kt:35-139`) | BROKEN | The video dimension is always null as shipped, while Settings reports it "ready" (FM-073); up to 60 bitmaps decoded before the missing model is detected (CONF-017, CONF-019; FND-AI-12) | Remove (NG-07); self-review deferred to R3 |

### 2.12 Feedback and coaching (mentor, conversation)

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-044 | Coaching | AI Mentor | Mentor using profile, last 5 sessions and 3 feedback records; multi-turn history in `mentor_history_{uid}.json` with "Clear" confirmation; inline dashboard card with "Local AI" badge · DOC-001, DOC-005 (FEAT-031), DOC-006, DOC-012, DOC-017, DOC-050 | `ConversationAndMentorScreens.kt:597-878`; template answer "streamed" with `delay(4L)` (`LocalIntelligenceOrchestrator.kt:566-775`, `:773`); invented weakest/strongest skill when no profile (`:960, 973`); history `InteractiveCoachViewModel.kt:156-188`; inline card `DashboardStudioWidgets.kt:1062-1225` | PARTIALLY_IMPLEMENTED | Deterministic templates presented as an "AI Mentor"; main-thread file I/O; history device-local only (CONF-047); inline card duplicates the route (FND-AI-16) | Remove (NG-02) → focus tips in CAP-05 |
| FM-045 | Coaching | Conversation simulator | 13 scenarios, voice dictation, TTS partner, debrief saved to `conversationSessions`; switch guard at ≥2 turns; turns 100% deterministic via `ScenarioScriptBank` · DOC-001, DOC-005 (FEAT-030), DOC-006, DOC-012, DOC-017, DOC-045 | Scenarios `CurriculumCatalog.kt:900-914`; screen `ConversationAndMentorScreens.kt:99-595`, guard `:122, 153-172, 441, 525`; replies are templates plus keyword echo (`LocalIntelligenceOrchestrator.kt:395-560`); `ScenarioScriptBank` absent (grep) | PARTIALLY_IMPLEMENTED | One-shot dictation styled as a phone call; turns kept in memory only (`InteractiveCoachViewModel.kt:88`); the claimed script bank never existed (CONF-052; FND-AI-16) | Remove → scripted role-play in CAP-06 (R2) |
| FM-046 | Coaching | Dynamic local coach rewrites | — (commit `13bffe0` only) | `DynamicLocalCoachEngine.kt:60-127` (topic and intent), hedge removal `:70-83`, "a lot of" → "many" `:92`, inserted "[1s pause]" `:359-375`; used by `LocalFeedbackEngine` and activity scoring (`UniversalEvaluationEngine.kt:247-256`) | IMPLEMENTED_BUT_NOT_DOCUMENTED | Can produce ungrammatical rewrites ("many water") and delete meaning ("what kind of car" → "what car") [I]; one test (CUJ `:740`) | Merge into CAP-05 (only after precision review) |

### 2.13 Vocabulary

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-047 | Vocabulary | Vocabulary builder, quiz and notebook | Tabs Words / Flashcards / AI Quiz / Notebook; search, level filters, "Card X of Y due today", flip; custom words; AI sentence evaluation; 9 quiz types, "AI Quiz" only if intelligence-backed; notebook with tags, edit and export · DOC-001, DOC-005 (FEAT-026), DOC-006, DOC-008, DOC-012, DOC-017, DOC-059 Ph9 | `VocabularyScreen.kt:69-452` (tabs `:109`, filters `:86-98`, notebook `:390-442`); 20-word bank (`CurriculumCatalog.kt:544-885`); "AI Quiz" is a single-sentence usage check (`MainCoachViewModel.kt:407-432`); `LocalVocabularyQuizEngine` 0 callers | PARTIALLY_IMPLEMENTED | "Due" counter counts never-seen words (`VocabularyScreen.kt:220-236`); quiz engine dead and seeded by the clock (`LocalVocabularyQuizEngine.kt:21`); no notebook tags or export; custom words not editable | Rebuild → CAP-07 (notebook and deterministic quiz in R2) |
| FM-048 | Vocabulary | Flashcards and spaced repetition | SM-2-style mastery 0–5 with intervals 1 d → 3 d → 3n … 21 d (DOC-001, DOC-005, DOC-006); SRS flashcards (DOC-059 Ph9) | UI uses an ad hoc scheduler (`MainCoachViewModel.kt:344-374`, Firestore `vocabulary/{wordId}`); `SpacedRepetitionScheduler.kt:28-45` (SM-2 variant) not wired: only `dueItems()` in LPM, always empty | DUPLICATED | Two schedulers, one dead; UTC day boundary (`LearningProgressManager.kt:47`); the first review counts as a "word learned" (`SpeakCraftRepository.kt:746-769`) (FND-DOM-11) | Merge into CAP-07 (one scheduler) |
| FM-049 | Vocabulary | Five-stage word lifecycle | Learn → Speak word → Speak sentences → Own sentence → Complete; offline evaluation; configurable pass scores 75/75/70; low ASR confidence → inconclusive; easier example after failures · DOC-059 Ph8 | Models `LearningModels.kt:210-269`; `LearningProgressManager.kt:394-491` (0 callers); stage 2 `LocalVocabularyEvaluator.kt:81-196`; stage 3 dead (`:202-316`); stage 4 `:322-480`; plan text promises the workflow (`DailyPracticePlanEngine.kt:78`) | PARTIALLY_IMPLEMENTED | **[dead]** No UI. ASR confidence is a constant, so "inconclusive" cannot work (FND-DOM-02); the inflection check accepts "convenient" for "convey" (`LocalVocabularyEvaluator.kt:524`) | Rebuild → CAP-07 |
| FM-050 | Vocabulary | Vocabulary home and vocabulary streak | Daily, new, in-progress, review, completed and mastered words; daily goal; vocabulary streak · DOC-059 Ph8A | None: the route opens the 4-tab screen (`AppNavigation.kt:967`; `VocabularyScreen.kt:109`); no vocabulary streak (grep) | DOCUMENTED_BUT_NOT_IMPLEMENTED | — | Rebuild → CAP-07 (SCR-WORD-01) |

### 2.14 Progress, streaks and analytics

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-051 | Progress | Progress analytics | Strongest/weakest skills, skill bars, recurring mistakes, separate confidence card, Daily/Weekly/Monthly filter, honest empty state (DOC-001, DOC-004, DOC-005 FEAT-033, DOC-017); activity analytics snapshot and milestones 1/10/25/50/100/150 (DOC-028, DOC-031) | `AnalyticsAndSettingsScreens.kt:88-248` (filter `:97-130`, empty state `:258-265`); `ActivityProgressAnalyticsEngine.kt:18-40, 92-104`; CUJ `:396-417`; `ActivityEngineUnitTest.kt:279-358` | PARTIALLY_IMPLEMENTED | Fallback skills 62/65/60/68/58/66 shown as real (`AnalyticsAndSettingsScreens.kt:103-108`); self-ratings labelled "Objective" (`:101-110`); confidence never shown; activity analytics capped at 200 attempts (`ActivityFirestoreRepository.kt:85`) | Rebuild → CAP-08 |
| FM-052 | Progress | "Zero fabricated metrics" | No fabricated metrics in progress analytics · DOC-028 | Minutes = 3 per ticked step (`MainCoachViewModel.kt:275`); LPM minutes = planned minutes on tap (`LearningProgressManager.kt:373`); words learned = words seen; `completedExercisesCount` also increments on un-tick (`SpeakCraftRepository.kt:683`); sparkline invents a 60 → 62 trend (`CoachComponents.kt:414-417`) | BROKEN | Session scores saved before `68a9cb1` may also contain fabricated baselines (CONF-010; FND-DOM-08) | Rebuild → CAP-08 (PP-1) |
| FM-053 | Progress | Legacy streak fix | Consecutive-day streak via `StreakAndAnalyticsHelper`, fixing date-gap increments and hard-coded +3 minutes · DOC-004 (ISS-002), DOC-017 | Used at `SpeakCraftRepository.kt:648, 670-684`; `lastPracticeDate` written even when no step is completed (`:680`); +3 minutes per step remains (`MainCoachViewModel.kt:275`); `StreakAnalyticsAndBackupTest.kt:27-69` tests arithmetic only | BROKEN | A stale streak can persist indefinitely and never decays until the next save; the save path is untested (FND-DOM-07) | Rebuild → CAP-08 |
| FM-054 | Progress | Streak and daily-completion engine | Explicit daily requirement; one count per day; timezone-safe; current and longest streak; calendar; milestones 3/7/14/30/60/100; no punitive resets · DOC-059 Ph4 | Milestones `StreakAndCompletionEngine.kt:21`; rule `LearningModels.kt:127-128`; update `LearningProgressManager.kt:364-368`; UI shows current, best and badge (`DashboardStudioWidgets.kt:703-760`) | PARTIALLY_IMPLEMENTED | Satisfied by two taps on plan rows (`:639-640`); no calendar; UTC day; local JSON only, lost on reinstall; untested (CONF-046) | Rebuild → CAP-08 (one streak rule, SRS §16) |
| FM-055 | Progress | Single streak definition | "Do not create parallel progress systems" · DOC-059 (critical requirements); streak definitions in DOC-004/017, DOC-028, DOC-059 | Four computations: profile `streakDays` (FM-053), LPM streak (FM-054), activity consistency (`ActivityProgressAnalyticsEngine.kt:34-40`), and `max()` on Profile and alerts (`UserProfileScreen.kt:153-156`; `AppNavigation.kt:556-559`) | DUPLICATED | Users can see different streak numbers on different screens (CONF-046; FND-DOM-07) | Merge into CAP-08 |
| FM-056 | Progress | Unified learning progress (`LearningProgressManager`) | Idempotent events (11 types), local-first persistence, sync and multi-device conflict rules · DOC-059 Ph10 | UI calls 3 of 21 public operations (`AppNavigation.kt:547, 590, 934`); `recordEvent` reachable only from dead functions (`LearningProgressManager.kt:261-266`); `mergeSnapshots` unused (`:713-784`); local JSON only (`:49-51, 889-919`); rules for learning collections exist but nothing writes them (`firestore.rules:704-756`) | PARTIALLY_IMPLEMENTED | **[dead]** Most of the class is unreachable; state lost on reinstall although tutorial copy promises streaks "across reinstalls or devices" (`FeatureTutorialRegistry.kt:28`); `processedEventKeys` never trimmed; untested (FND-DOM-02, FND-DATA-02) | Rebuild → CAP-11 |

### 2.15 Notifications

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-057 | Notifications | OS reminders and push | No push, `POST_NOTIFICATIONS`, WorkManager or FCM at baseline (DOC-002, DOC-005 FEAT-034); daily practice reminders that respect notification permission requested (DOC-059 Ph5) | `AndroidManifest.xml:5-8` (no permission); no WorkManager/FCM dependency (`app/build.gradle.kts:105-151`); `NotificationCompat` / `AlarmManager` / `WorkManager` 0 hits; `preferredReminderTime` never read | DOCUMENTED_BUT_NOT_IMPLEMENTED | "Reminders" are created only while the app is open (CONF-043) | Rebuild → CAP-09 (R1 OS reminders; D-12) |
| FM-058 | Notifications | In-app notification centre | Bell with unread badge, read/unread, mark-all, delete, categories, timestamps, empty state, preferences; 10 categories; offline-created; cross-device sync; no update alerts (DOC-059 Ph5); false "Session Completed" alert fixed (DOC-052) | `NotificationDrawer.kt:96-400` (actions `:557-651`, preferences `:698-729`); bell `AppNavigation.kt:753-780`; store `NotificationManagerRepository.kt:74-122, 434-469` (local JSON); 8 categories `NotificationModels.kt:23-32`; update isolation `:113-164`; session alert only for COMPLETED sessions (`AppNavigation.kt:561-583`); `NotificationManagementSystemTest` (4) | PARTIALLY_IMPLEMENTED | 8 of 10 categories; `accountAlerts` has no toggle; no sync; free-text announcement routes can crash navigation [I] (`AppNavigation.kt:1315`); admin announcements displayed but undocumented; whole feature undocumented (CONF-043, CONF-044) | Defer → CAP-09 inbox (R2) |
| FM-059 | Notifications | Single notification system | No parallel systems (DOC-059); the bell opens one notification centre | Two stores: LPM `UserNotification` and `NotificationManagerRepository`, with a one-way bridge (`AppNavigation.kt:585-587`); different dedup keys `daily_plan_ready_` / `daily_plan_` (`LearningProgressManager.kt:138`; `NotificationManagerRepository.kt:102`); `NotificationsCenterSheet.kt:79-468` has 0 callers | DUPLICATED | Two "plan ready" and two streak alerts per day; dismissed bridged alerts return [I] (`NotificationManagerRepository.kt:186, 194`) (FND-DOM-14) | Merge into CAP-09 |

### 2.16 Tutorials and launch animation

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-060 | Tutorial/Launch | Feature tutorial system | `FeatureTutorialRegistry` for 24 features; inline `FeatureGuideStrip` in 13 files; "?" dialog with search; "Explore Tutorials for All 24 Features"; first-use `TutorialOverlay` per user · DOC-016, DOC-017, DOC-056 | `FeatureTutorialRegistry.kt:16-429`; `FeatureTutorialComponents.kt:70-174, 268`; two dialog instances (`AppNavigation.kt:605-615`; `DashboardScreen.kt:172-178`); `TutorialOverlay.kt:109-144, 185-657`; `TutorialState.kt:11, 27-31` | DUPLICATED | Four overlapping mechanisms; Back or Skip marks the tour complete (`AppNavigation.kt:1252-1268`); the `auth` entry promises "live Gemini AI coaching" (`FeatureTutorialRegistry.kt:29`); profile and activity routes show the dashboard guide (`:437-438`) (CONF-027) | Remove → contextual first-use tips (DES-06) |
| FM-061 | Tutorial/Launch | Launch animation | Native overlay above the root, non-blocking, latched (no replay); 2,900 / 2,100 / 600 ms; 4 stages; tagline · DOC-018, DOC-019, DOC-020 | `LaunchAnimationOverlay.kt:71-75, 117-180, 219-248, 251-658`; strings `:324-327`; `AppNavigation.kt:264-283`; `SpeakCraftLaunchAnimationTest` (8) | DOCUMENTED_AND_IMPLEMENTED | Not skippable; touches likely pass through [I]; restarts on rotation [I]; costs 2.1 s on every launch | Rebuild (≤1 s, reduced-motion aware) → DES-06 |
| FM-062 | Tutorial/Launch | Flash-free splash hand-off | Splash and window background match the app surface (Warm Ivory / Midnight Navy) · DOC-018, DOC-019 | Window `res/values/colors.xml:10` (#FAF9F6) and `res/values-night/colors.xml:3` (#0F172A) vs Compose canvas `Color.kt:6, 14` (#F4F5FB / #100C2A) | BROKEN | Visible colour jump [I visual] (CONF-035) | Rebuild → DES-05 |

### 2.17 YouTube intelligence

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-063 | YouTube | Channel, playlist and video inspection | Inspect a URL; RSS, Data API v3 or HTML scraping; timedtext captions; statuses EXTRACTED / NO_CAPTIONS / INACCESSIBLE; Keystore AES-256-GCM API key; scheduled multi-channel sync (proposed) · DOC-001, DOC-002, DOC-005 (FEAT-011–015), DOC-006, DOC-007 | `YouTubeIntelligenceClient.kt:67-519` (spoofed user agent `:199-201, 505-510`; timedtext `:391-452`; key in query `:309-327`); `YouTubeIntelligenceScreen.kt:63-302`; key encryption `AppPreferencesRepository.kt:51-110`; no sync worker (no WorkManager) | PARTIALLY_IMPLEMENTED | Fragile scraping with Terms-of-Service risk; key typed into a plaintext field (`AnalyticsAndSettingsScreens.kt:946-953`); occupies a primary tab (FND-AI-11) | Remove (NG-08) |
| FM-064 | YouTube | Lesson extraction and seeded knowledge base | Local `extractYouTubeLessons` replaces Gemini; 1–3 techniques per video with creator/AI separation; pre-seeded @askvinh knowledge base · DOC-004 (ISS-011), DOC-005 (FEAT-014), DOC-033, DOC-034 | `LocalIntelligenceOrchestrator.kt:818-917`: first ≤5 caption sentences, synthetic timestamps `idx*45s` (`:880-881`), `isCreatorDerived = true` (`:882, 905`); shown as "verified" (`YouTubeIntelligenceScreen.kt:321`); seeding `SpeakCraftRepository.kt:532-590`, `MainCoachViewModel.kt:187-191, 230` | BROKEN | Verbatim captions published as `creatorAdvice` with fabricated timestamps; knowledge base duplicated per user (FND-AI-11, FND-DOM-09) | Remove (NG-08; D-08) |

### 2.18 Admin

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-065 | Admin | Admin console | RBAC console with 4 tabs (YouTube Sync, Moderation, Releases, Telemetry); admin = `adminUsers/{uid}` with `active == true`; client writes denied · DOC-001, DOC-002, DOC-005 (FEAT-037), DOC-007 | `AdminDashboardScreen.kt:65-402` (tabs `:108`, restricted card `:71-90`); `UpdateAndAdminViewModel.kt:345-432`; CUJ `:202-229` | PARTIALLY_IMPLEMENTED | The "moderation queue" lists every technique (`SpeakCraftRepository.kt:424-435`); release form never sets `signingCertSha256` (`AdminDashboardScreen.kt:319-332`); no empty states | Remove (NG-10) |
| FM-066 | Admin | Trusted admin authorisation and 13 modules | Custom claims or trusted authorisation enforced server-side; entry hidden for non-admins; 13 modules; no default access to recordings; document the gap if no secure backend exists · DOC-059 Ph7 | Claims OR `adminUsers` document (`SpeakCraftRepository.kt:236-270`; `firestore.rules:17-46`); client maps a bare `admin: true` claim and a role-less document to SUPER_ADMIN (`:250`, `:261`); MODERATOR can publish releases (`firestore.rules:843-852`); admins read every profile (`:333-334`); role management, announcement publishing and audit log have 0 callers (`SpeakCraftRepository.kt:1225-1285, 299-337`) | PARTIALLY_IMPLEMENTED | 4 of 13 modules; no provisioning tooling; the requested gap document was not written (CONF-048; FND-SEC-04) | Remove (NG-10); rules hardening in SEC-01 |

### 2.19 App updates and backups

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-067 | Updates | OTA discovery, download and install | Firestore `applicationReleases` and GitHub Releases; SemVer and `minSupportedVersion`; HTTPS; 64-hex SHA-256 and signing-cert check; `ACTION_VIEW` install; one-tap check auto-downloads; INSTALLED / ROLLED_BACK telemetry · DOC-002, DOC-004 (ISS-016), DOC-005 (FEAT-035/036), DOC-007, DOC-016, DOC-017, DOC-020, DOC-021 | `ReleaseUpdateRepository.kt:65-153, 170-311, 313-338`; `GitHubReleaseClient.kt:18-196`; `ApkIntegrityVerifier.kt:43-163`; `SemVer.kt:10-36`; auto-download `UpdateAndAdminViewModel.kt:216-226`; no `REQUEST_INSTALL_PACKAGES` (`AndroidManifest.xml:5-8`); INSTALLED / ROLLED_BACK accepted (`SpeakCraftRepository.kt:1046`) but never emitted (`UpdateAndAdminViewModel.kt:209-298`); `SemVerAndUpdateTest` (8) | PARTIALLY_IMPLEMENTED | Install likely fails on API 26+ [I]; user-editable GitHub source field (`AppUpdateScreen.kt:265, 314-324`); "uninstall the old build" guidance; conflicts with Google Play policy (FND-OPS-04) | Remove (D-03; NG-11) |
| FM-068 | Updates | Update-available dialog and signature compatibility | — (commit `08972b7` only) | `UpdateAvailableDialog.kt:53-332` (not dismissable when mandatory, `:75-77, 319`); gating `AppNavigation.kt:646-675`; cert comparison `ReleaseUpdateRepository.kt:184-196, 260-303` | IMPLEMENTED_BUT_NOT_DOCUMENTED | A mandatory release blocks every route while installation may be impossible (FM-067) [I] | Remove (D-03) |
| FM-069 | Updates | Pre-update backup, restore and interrupted-update recovery | Backup snapshot before update; restore UI; interrupted-update recovery · DOC-002, DOC-004 (ISS-017), DOC-005, DOC-017 | `UpdateBackupManager.kt:33-132`; restore returns only theme, TTS rate and channel (`:98-116`; `UpdateAndAdminViewModel.kt:325-337`); recovery marker `:118-132`; `StreakAnalyticsAndBackupTest` | PARTIALLY_IMPLEMENTED | "Backup" stores recording file names, not recordings or progress | Remove → CAP-11 sync replaces it |

### 2.20 Settings and AI settings

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-070 | Settings | Settings screen and section structure | Update channel, backup, admin entry, theme, TTS 0.65–1.35×, Keystore YouTube key, clear history, sign out (DOC-001, DOC-002, DOC-005 FEAT-006/038); sections Account, Learning, Appearance (accent, font scale, animation), AI (no cloud settings), Integrations (real only), Data & Privacy, Notifications, About (DOC-059 Ph6) | `AnalyticsAndSettingsScreens.kt:394-1068` (29 controls; phone order `:1057-1064`); clear history `SpeakCraftRepository.kt:1023-1036` behind a confirmation (CUJ `:335-367`); `setReducedMotion` / `setFontScalePreference` 0 callers (`AppPreferencesRepository.kt:241-251`); About / Terms / Privacy 0 hits | PARTIALLY_IMPLEMENTED | Developer diagnostics come first; no account section, account deletion, export or notification preferences; TTS clamp 0.6–1.4 vs UI 0.65–1.35 | Rebuild → CAP-10 (SCR-SET-01…07) |
| FM-071 | Settings | Centralised AI & Intelligence settings | One `AISettingsRepository`: local AI, cloud AI (locked off), model tier, live transcription (default off), local ASR, audio DSP, video, language en-US/GB/AU/IN, engine AUTO / ANDROID_OFFLINE / LOCAL_WHISPER; per-screen cloud chips removed · DOC-050, DOC-051 | `AISettingsRepository.kt:21-89`; keys `AppPreferencesRepository.kt:120-129`; UI `AnalyticsAndSettingsScreens.kt:650-917`; `CentralizedAiSettingsTest` (10), `HybridAiComposeUiTest` (5) | DOCUMENTED_AND_IMPLEMENTED | Still shows a permanently disabled cloud switch and tier chips, contrary to DOC-059 "no cloud AI settings" (CONF-004); `LOCAL_WHISPER` actually runs the Android recogniser (`LiveTranscriptionManager.kt:340, 452`) | Rebuild → CAP-12 + CAP-10 (speech and privacy settings, no cloud) |
| FM-072 | Settings | Local-AI and audio-analysis toggles | Toggles switch local intelligence and acoustic analysis on or off · DOC-050 | `localAiEnabled` only feeds `enableAudioAnalysis` (`HybridAiCoachCoordinator.kt:179-182`); with audio analysis off, the validator decodes and extracts DSP anyway (`EvaluationEvidenceValidator.kt:109-134`; `LocalIntelligenceOrchestrator.kt:194`) | BROKEN | Settings shows "Local Intelligence: Disabled" while every language engine keeps running (FND-AI-06) | Remove → CAP-12 |
| FM-073 | Settings | Capability diagnostics | Engine, offline availability, microphone, models, camera/MediaPipe with 7 `CapabilityStatus` values; never show an unavailable feature as enabled · DOC-054 | `IntelligenceCapabilityManager.kt:102-311`; MediaPipe "ready" because bundled models count as installed without a file check (`OnDeviceModelManager.kt:160-166` → `:274-294`); "Offline verified" when only a Whisper file exists (`:113, 203-205`); any recogniser error → INITIALIZATION_FAILED (`:177-183`); computed on the main thread (`AppNavigation.kt:435-437`) | BROKEN | Violates its own rule (DOC-054); a test encodes the false positive (`LocalAIFoundationTest.kt:80-88`) (FND-AI-05) | Rebuild → CAP-12 (honest status) |

### 2.21 Privacy and data rights

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-074 | Privacy | Recordings and transcripts stay on the device | "Media never uploaded; Firestore holds scores and progress only" (DOC-032, DOC-050); "audio, video and live transcripts remain strictly on-device" (DOC-050; Settings text `AnalyticsAndSettingsScreens.kt:917`) | Media stays in `filesDir/recordings` (`AudioRecorderManager.kt:57-62`); but the session payload includes the transcript (≤15,000 chars) and the absolute `localMediaPath` (`SpeakCraftRepository.kt:803, 806`); network recogniser used when no on-device one exists (`LiveTranscriptionManager.kt:99-103`; `TtsAndSpeechHelper.kt:126`); `allowBackup="true"` with template rules (`AndroidManifest.xml:18-20`) | BROKEN | At HEAD the transcript upload is blocked only because the whole save is denied (FM-031); builds before `68a9cb1` could upload it [I]; recordings are eligible for cloud backup [I] (CONF-011; FND-SEC-05, FND-SEC-06, FND-SEC-07, FND-DATA-04) | Rebuild → CAP-10 + CAP-12 (D-10, D-16) |
| FM-075 | Privacy | Data export and selective clear | Export and clear selected data · DOC-059 Ph6 | No export path (the only FileProvider use is APK install, `ReleaseUpdateRepository.kt:318`); `exportUserLearningDataJson`, `clearSelectedLearningData`, `wipeAllLocalDataForUser` have 0 callers (`LearningProgressManager.kt:670-697`) | PARTIALLY_IMPLEMENTED | **[dead]** With FM-003, a Play and GDPR gap (FND-SEC-03) | Rebuild → CAP-10 |
| FM-076 | Privacy | No emotion or personality inference | No facial emotion or psychological inference; pitch and loudness not treated as confidence · DOC-036, DOC-045 | No emotion model or label; limitation text present (`FaceLandmarkAnalysisEngine.kt:180`); but the confidence/delivery sub-engine calls dBFS −30…−6 "strong projection" and pitch SD ≥25 Hz "dynamic melody" (`LocalFeedbackEngine.kt:257-314`) | PARTIALLY_IMPLEMENTED | Delivery feedback infers confidence-like traits from loudness and pitch | Keep (principle) → CAP-05 (PP-1) |

### 2.22 Theming and accessibility

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-077 | Theme/A11y | Editorial Studio palette | Navy-primary (DOC-001), then amber-primary #D97706 with `SpeakCraftColors` (DOC-011, DOC-014, DOC-017, DOC-018) | Replaced: light primary `RoyalViolet` #6C3DF4, dark `LuminousViolet` (`Theme.kt:175, 201`; `Color.kt:22-23`); amber survives only as an alias (`Color.kt:116-118`) | DEPRECATED | Superseded by the undocumented re-skin in `dc7c9cb` (CONF-033, CONF-034) | Remove (D-18 → DES-05) |
| FM-078 | Theme/A11y | Violet Studio / Midnight Indigo theme and theme modes | Passing mention only: "Violet Studio Light / Midnight Indigo Dark", "Indigo Studio Sidebar" (DOC-022); System / Light / Dark (DOC-001, DOC-005) | ≈85 colour constants (`Color.kt`); 48 extended roles (`Theme.kt:17-67`); schemes `:174-224`; theme mode `AppNavigation.kt:221-225`, `AnalyticsAndSettingsScreens.kt:537-544` | IMPLEMENTED_BUT_NOT_DOCUMENTED | No specification and no contrast check; no dynamic colour; system bars follow the system, not the app theme [I] | Rebuild → DES-05 (D-18) |
| FM-079 | Theme/A11y | Design tokens: type, spacing, shape, motion | Plus Jakarta Sans / DM Sans / JetBrains Mono, displayLarge 34sp, headlineMedium 21sp; spacing 4–32dp and 48dp targets; shapes 10/16/24/32dp (DOC-011, DOC-017); motion 150/260/320/850 ms (DOC-013) | `Type.kt:15-105` (34/21sp match; single-weight fonts with synthesised bold; `headlineSmall` undefined); `Spacing.kt:18-47` (matches); shapes 14/20/24/30dp (`Theme.kt:226-231`) | PARTIALLY_IMPLEMENTED | 1,386 literal dp, 147 raw `RoundedCornerShape`, 95 weight overrides; monospace `labelSmall` used 103 times (CONF-036) | Rebuild → DES-05 |
| FM-080 | Theme/A11y | Accessibility | A11Y-01…07 (chart semantics, `FlowRow`, 48dp targets, reduced motion, `heading()`); WCAG 2.1 AA; contrast ratios verified · DOC-013, DOC-014, DOC-017 | `rememberReducedMotionEnabled` not reactive (`CoachComponents.kt:111-124`) and ignored by dashboard, drawer, score ring and onboarding; 6 `heading()` and 2 `Role.` uses in `ui/`; undersized targets (`DashboardStudioWidgets.kt:467, 625`); fixed heights (`:282`; `VocabularyScreen.kt:241`); 127 of 195 icons have `contentDescription = null` | PARTIALLY_IMPLEMENTED | Contrast ratios apply to the retired palette, so current compliance is UNKNOWN (CONF-034); TalkBack sweep never done (DOC-016); no font-scale test | Rebuild → DES-07 |

### 2.23 Testing and CI claims

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-081 | Test/CI | Unit and Robolectric test suite | "N/N passing": 17, 30, 41, 53, 69, 161 (see §5) · DOC-004, DOC-016, DOC-017, DOC-020, DOC-031, DOC-051, DOC-056 | 161 `@Test` in `app/src/test` (24 files) + 1 in `app/src/androidTest`; 3 trivial (`ExampleUnitTest`, `ExampleRobolectricTest`, `ExampleInstrumentedTest`); CI has no test step (`release_on_push.yml:292, 324`) | UNKNOWN | Counts verified statically; pass status not reproducible; nothing tests the NavHost, permission denial, offline behaviour, repositories or LPM (CONF-028, CONF-039; FND-TEST-01, FND-TEST-02) | Rebuild → TST-01 (CI test gate, ENG-01) |
| FM-082 | Test/CI | Firestore rules emulator tests | "4/4 suites passed" against the emulator · DOC-029, DOC-031 | `firestore.test.js:44, 63, 142, 275` (4 cases); no `package.json` for `@firebase/rules-unit-testing`; not run in CI | PARTIALLY_IMPLEMENTED | Not runnable as committed; no coverage of `speakingSessions` (the broken path), vocabulary or claims-based admins (FND-TEST-04) | Rebuild → TST-01 |
| FM-083 | Test/CI | CI release pipeline | Push to main/master/beta builds beta `1.1.0-beta.<run>`; tag builds stable; GitHub Release with `.sha256`; Firestore publish; debug signing from `debug.keystore.base64` in the repository · DOC-021 | `.github/workflows/release_on_push.yml:3-38, 116-228, 355-501`; debug keystore from the `DEBUG_KEYSTORE_BASE64` secret (`:298-309`); beta builds `assembleDebug` (`:324`); tags `v1.1.0-beta.7` … `v1.1.0-beta.23` | DOCUMENTED_AND_IMPLEMENTED | Signing description wrong (CONF-038); beta ships debuggable debug-signed builds (FND-SEC-02); no unit tests, lint, rules tests or rules deploy (FND-OPS-01); mutable action tags and lockfile-less npm install with the service-account secret (FND-OPS-03); no `firestore.indexes.json` | Rebuild → ENG-01 (ADR-004) |
| FM-084 | Test/CI | Real-device and device-tier validation | Manual checklist (CameraX, mic, Credential Manager, TalkBack) pending (DOC-016, DOC-020, DOC-056); low-, mid- and high-end device testing (DOC-059 Ph13); "PASS (100% Stable)", latency, RAM and battery figures (DOC-042, DOC-046–048) | No device logs, profiler output, benchmark harness or dataset in the repository; DOC-048 Profile 1 is "Simulated" | DOCUMENTED_BUT_NOT_IMPLEMENTED | Benchmark figures are UNKNOWN (CONF-014, CONF-040) | Rebuild → TST-01 / NFR (measured targets) |

### 2.24 Security and rules

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-085 | Security | Firestore rules: isolation and zero-trust validation | Owner-only access, `hasOnly`, immutable fields and numeric bounds for activity collections, "deployed" (DOC-029); RBAC rules for 22 collections (DOC-007) | Default deny `firestore.rules:5-7`; owner check `:13-15`; validators `:72-329`; activity collections `:585-702`; `update: false` on immutable logs; list validation checks only indices 0–2 (`:64-70`); scores and streaks client-writable (`:356-362`); shared catalogue accepts any admin payload (`:794-839`) | PARTIALLY_IMPLEMENTED | Deployed state UNKNOWN (CI never deploys rules); code/rules drift breaks session saves (FM-031) (FND-SEC-10, FND-DATA-07) | Rebuild → SEC-01 + CAP-11 |
| FM-086 | Security | Undocumented collections | — (rules only) | `notifications`, `learningEvents`, `vocabularyProgress`, `quizAttempts`, `learningSnapshots` (`firestore.rules:704-756`, no `hasOnly`), with no client writer; `adminAnnouncements`, `adminAuditLogs`, `adminSettings` (`:768-786`) | IMPLEMENTED_BUT_NOT_DOCUMENTED | 34 paths in rules vs 22/26 documented (CONF-030; FND-DATA-07) | Remove (retire) → ARC-03 |
| FM-087 | Security | App Check and build hardening | Debug provider in debug builds, reCAPTCHA in release; a missing site key fails open · DOC-002, DOC-004 (ISS-006) | `AppCheckConfig.kt:24-63`; debug secret written from a launch-intent extra (`:13, 25-36`) on the exported `MainActivity` (`AndroidManifest.xml:28`); `firebase-appcheck-debug` as `implementation` (`app/build.gradle.kts:139`); `isMinifyEnabled = false` (`:66`); uid and email logged on Firestore errors (`FirestoreErrorHandler.kt:26-51`) | PARTIALLY_IMPLEMENTED | Beta builds are debug builds, so the debug provider ships to users [I exploitability]; availability of the reCAPTCHA artifact UNKNOWN (FND-SEC-02, FND-SEC-08, FND-SEC-11) | Rebuild → SEC-01 (Play Integrity, R8) |

### 2.25 Cloud AI

| ID | Domain | Feature | Documented claim · source | Implementation evidence | Status | Notes | Disposition → CAP |
|---|---|---|---|---|---|---|---|
| FM-088 | Cloud AI | Cloud Gemini evaluation | `GeminiCoachService` multimodal analysis (21-field schema, 60 s, 3:2 blend) (DOC-002, DOC-005 FEAT-020, DOC-006); nine evaluators on Firebase AI Logic `gemini-3.8-flash` with fallback (DOC-022, DOC-024, DOC-025, DOC-030, DOC-056) | `GeminiCoachService.kt:20-87` is `@Deprecated` and every method throws; the cloud activity path returns null under `LOCAL_ONLY` (`HybridAiCoachCoordinator.kt:74-78, 341-354`); `FirebaseAiLogicService.kt:714-845` unreachable | DEPRECATED | **[dead]** Referenced only by deprecated ViewModel constructors used in tests (CONF-002, CONF-007) | Remove (D-04; NG-01) |
| FM-089 | Cloud AI | Zero-cloud enforcement | "Zero cloud AI calls verified"; `firebase-ai` purged; network interceptor blocks 6 AI domains; `LOCAL_ONLY` policy · DOC-032, DOC-034, DOC-043, DOC-044, DOC-045, DOC-050 | Runtime gate `LocalExecutionPolicy.kt:13-15` and `HybridAiCoachCoordinator.kt:56-63, 74-78`; `firebase-ai` present (`app/build.gradle.kts:135`); `Firebase.ai` call sites (`FirebaseAiLogicService.kt:78, 104-105`); interceptor `LocalExecutionPolicy.kt:35-48` attached to no client (`YouTubeIntelligenceClient.kt:61-65`, `GitHubReleaseClient.kt:12`, `ReleaseUpdateRepository.kt:53`); blocklist omits the Firebase AI host (`:18-25`) [I] | PARTIALLY_IMPLEMENTED | No cloud call can run in this build, but the guarantee is a compile-time constant, not removal or network control; `ZeroCloudAiNetworkBoundaryTest` gives false assurance (CONF-001, CONF-005, CONF-006, CONF-009; FND-AI-10, FND-TEST-03) | Remove dormant cloud code (D-04): guarantee by absence |
| FM-090 | Cloud AI | Hybrid AI coach and Gemini Live | — (commit `38d52f2`); DOC-050 says cloud is "locked off" and the Gemini Live branches were removed | `HybridAiCoachCoordinator.kt:44, 290-303`; tiers `FirebaseAiLogicService.kt:80-84`; Live `:476-534`; `InteractiveCoachViewModel.kt:255-299` has 0 callers; payloads would include raw audio/video, transcripts and profile data with no consent flow ("consent" 0 hits; `FirebaseAiLogicService.kt:90-838`) | DEPRECATED | **[dead]** Policy-gated; breaches the letter of DOC-059's constraints (CONF-003, CONF-004, CONF-053); validity of the model IDs UNKNOWN | Remove (D-04; NG-01) |

---

## 3. Summary statistics

### 3.1 Counts by status

| Status | Count | Share | FM IDs |
|---|---:|---:|---|
| DOCUMENTED_AND_IMPLEMENTED | 12 | 13% | 001, 008, 012, 023, 024, 027, 028, 036, 042, 061, 071, 083 |
| DOCUMENTED_BUT_NOT_IMPLEMENTED | 3 | 3% | 050, 057, 084 |
| IMPLEMENTED_BUT_NOT_DOCUMENTED | 4 | 4% | 046, 068, 078, 086 |
| PARTIALLY_IMPLEMENTED | 41 | 46% | 002–007 (excl. 005), 009, 011, 013, 016, 017, 019, 022, 026, 030, 032, 035, 037, 039, 040, 044, 045, 047, 049, 051, 054, 056, 058, 063, 065–067, 069, 070, 075, 076, 079, 080, 082, 085, 087, 089 |
| BROKEN | 16 | 18% | 014, 020, 021, 031, 033, 034, 038, 041, 043, 052, 053, 062, 064, 072, 073, 074 |
| DEPRECATED | 3 | 3% | 077, 088, 090 |
| DUPLICATED | 10 | 11% | 005, 010, 015, 018, 025, 029, 048, 055, 059, 060 |
| UNKNOWN | 1 | 1% | 081 |
| **Total** | **90** | 100% | |

Observations (INFERRED from the counts):
- Only **12 of 90 features (13%)** match their documentation, and most of those carry defects noted in the row.
- **57 of 90 (63%)** are PARTIALLY_IMPLEMENTED or BROKEN. In 7 rows the core code is **[dead]** (FM-003, 035, 049, 056, 075, 088, 090).
- **10 DUPLICATED** rows reflect accretion rather than replacement: three daily plans, four streaks, two module taxonomies, two schedulers, two notification stores, four tutorial mechanisms.
- The newest request (DOC-059) is the least implemented: every one of its 13 phases is partial (§4).

Recommended dispositions (RECOMMENDATION, aligned with RD-16): **Rebuild 49**, **Remove 22**, **Merge 14**, **Keep 3** (FM-036 evidence gate, FM-042 feedback structure, FM-076 no-inference principle), **Defer 2** (FM-028 video self-review, FM-058 inbox).

### 3.2 Counts by domain

Column key: **D&I** DOCUMENTED_AND_IMPLEMENTED · **D-NI** DOCUMENTED_BUT_NOT_IMPLEMENTED · **I-ND** IMPLEMENTED_BUT_NOT_DOCUMENTED · **PART** PARTIALLY_IMPLEMENTED · **BRK** BROKEN · **DEP** DEPRECATED · **DUP** DUPLICATED · **UNK** UNKNOWN. "·" = 0.

| Domain (§) | D&I | D-NI | I-ND | PART | BRK | DEP | DUP | UNK | Total |
|---|---|---|---|---|---|---|---|---|---|
| Auth & account (2.2) | 1 | · | · | 2 | · | · | · | · | 3 |
| Onboarding & profile (2.3) | · | · | · | 3 | · | · | 1 | · | 4 |
| Navigation & IA (2.4) | 1 | · | · | 1 | · | · | · | · | 2 |
| Dashboard & welcome (2.5) | 1 | · | · | 2 | 1 | · | 1 | · | 5 |
| Daily practice & plans (2.6) | · | · | · | 2 | · | · | 1 | · | 3 |
| Curriculum, lessons, programmes, paths (2.7) | · | · | · | 2 | 2 | · | 1 | · | 5 |
| Activity engine (2.8) | 2 | · | · | 1 | · | · | 1 | · | 4 |
| Speaking labs & recording (2.9) | 2 | · | · | 1 | 1 | · | 1 | · | 5 |
| Speech recognition & transcription (2.10) | · | · | · | 2 | 2 | · | · | · | 4 |
| Evaluation & scoring (2.11) | 2 | · | · | 3 | 3 | · | · | · | 8 |
| Feedback & coaching (2.12) | · | · | 1 | 2 | · | · | · | · | 3 |
| Vocabulary (2.13) | · | 1 | · | 2 | · | · | 1 | · | 4 |
| Progress, streaks & analytics (2.14) | · | · | · | 3 | 2 | · | 1 | · | 6 |
| Notifications (2.15) | · | 1 | · | 1 | · | · | 1 | · | 3 |
| Tutorials & launch animation (2.16) | 1 | · | · | · | 1 | · | 1 | · | 3 |
| YouTube intelligence (2.17) | · | · | · | 1 | 1 | · | · | · | 2 |
| Admin (2.18) | · | · | · | 2 | · | · | · | · | 2 |
| App updates & backups (2.19) | · | · | 1 | 2 | · | · | · | · | 3 |
| Settings & AI settings (2.20) | 1 | · | · | 1 | 2 | · | · | · | 4 |
| Privacy & data rights (2.21) | · | · | · | 2 | 1 | · | · | · | 3 |
| Theming & accessibility (2.22) | · | · | 1 | 2 | · | 1 | · | · | 4 |
| Testing & CI claims (2.23) | 1 | 1 | · | 1 | · | · | · | 1 | 4 |
| Security & rules (2.24) | · | · | 1 | 2 | · | · | · | · | 3 |
| Cloud AI (2.25) | · | · | · | 1 | · | 2 | · | · | 3 |
| **Total** | **12** | **3** | **4** | **41** | **16** | **3** | **10** | **1** | **90** |

Evaluation & scoring has the most BROKEN rows (3 of 8), and no domain has more than two DOCUMENTED_AND_IMPLEMENTED rows.

### 3.3 The 15 most consequential gaps between documentation and code

Ranked by user, privacy, legal and delivery impact (analyst judgement, INFERRED from the rows cited).

| Rank | Gap | Documentation says | Code does | FM IDs |
|---|---|---|---|---|
| 1 | Speaking sessions cannot be saved | Sessions are evaluated, labelled and saved (DOC-052/053) | Every save is denied by `firestore.rules` since HEAD; feedback and score blending lost | FM-031, FM-085 |
| 2 | Privacy statement is false | Audio, video and transcripts stay on the device (DOC-050, Settings) | Transcripts and device paths are in the Firestore payload; network recogniser fallback; recordings eligible for cloud backup | FM-074, FM-032 |
| 3 | "Zero cloud AI, verified" is a flag, not a fact | `firebase-ai` purged; network layer blocks AI hosts (DOC-034, DOC-050) | Gemini client, model tiers and Gemini Live ship in the APK; interceptor attached to nothing; blocked only by a constant | FM-089, FM-090, FM-088 |
| 4 | Activity scores measure text length | Nine honest local evaluators (DOC-039, DOC-028) | One length formula; dimension spread fabricated; a wordless recording passes and completes the activity | FM-041, FM-026 |
| 5 | "Pronunciation" is mislabelled | Pronunciation assessment with accent tolerance (DOC-044/045) | Free speech: loudness + pitch + voicing; reference comparison never triggered by any screen | FM-038, FM-039 |
| 6 | No working transcript pipeline | Multi-tier on-device ASR (Whisper → Moonshine → Android) (DOC-044–047) | File transcription always empty; Whisper path fake; no models; live ASR off by default and single-utterance | FM-033, FM-034, FM-032 |
| 7 | Video analysis and capability status are false | MediaPipe bundled; "never mark unavailable as enabled" (DOC-040, DOC-054) | No model files; video dimension always null; Settings says "ready" and "offline verified" | FM-043, FM-073 |
| 8 | Fabricated progress values | "Zero fabricated metrics"; "no fake statistics" (DOC-028, DOC-059) | 3 minutes per tick, words seen = learned, fallback score 62, streak 1 on day 0, invented sparkline | FM-052, FM-014, FM-051 |
| 9 | Four streaks, one stale and all gameable | Streak fixed (DOC-017); one qualifying-day rule (DOC-059 Ph4) | Four computations; stale profile streak; two taps earn a streak day | FM-053, FM-054, FM-055 |
| 10 | DOC-059 learning core is mostly dead code | Unified progress, 5-stage vocabulary, quiz, SRS, sync (DOC-059 Ph8–10) | Classes exist; 3 of 21 LPM operations used; quiz and SM-2 unwired; no sync; untested | FM-056, FM-049, FM-048, FM-047 |
| 11 | No reachable account deletion or export | Delete account with re-auth; export (DOC-059 Ph6) | Deletion code has no caller and no re-auth; no export | FM-003, FM-075 |
| 12 | Three parallel daily plans; goal ignored | One personalised plan; no parallel systems (DOC-059 Ph2) | Checklist, activity plan and dashboard plan run side by side; the user's goal never reaches the plan | FM-015, FM-016, FM-017, FM-006 |
| 13 | Unbacked "VERIFIED" creator content | Verified creator lessons with sources (DOC-005/006) | 12 "VERIFIED" items link to a channel root; YouTube extraction invents timestamps; IP exposure | FM-020, FM-064 |
| 14 | Test and release assurance is self-reported | "161/161 passed"; signing and pipeline described (DOC-051, DOC-021) | CI runs no tests; beta channel ships debug-signed debuggable builds with the App Check debug provider | FM-081, FM-083, FM-087 |
| 15 | Self-updater likely cannot install | One-tap verified update (DOC-016/017, DOC-021) | No `REQUEST_INSTALL_PACKAGES`; mandatory dialog blocks every route; Play policy conflict | FM-067, FM-068 |

---

## 4. Check of PO-REQ-1119 (DOC-059, 19-Nov request)

Base: the UI audit's prompt-implementation check, completed with the learning-domain audit. DOC-059 has 1,204 lines; line ranges are given for traceability. No phase is fully implemented.

| Phase (DOC-059 lines) | Requirement (short) | Status | Evidence | FM IDs |
|---|---|---|---|---|
| 1 Welcome back (43-131) | Animated personalised greeting with avatar, track, day, score, message; swipeable plan carousel | PARTIALLY_IMPLEMENTED | Greeting is one text line with five unused parameters (`DashboardStudioWidgets.kt:127-189`); carousel built with `HorizontalPager` (`:247-260`) but mostly static copy | FM-011, FM-012 |
| 2 Personalised daily plan (132-273) | `UserLearningProfile`; 5 levels with manual correction; deterministic, adaptive, recovery-aware, versioned plan | PARTIALLY_IMPLEMENTED | `DailyPracticePlanEngine.kt:31-243` exists; goal ignored (`LearningProgressManager.kt:86-88`); target level, interests and time never collected (`LearningModels.kt:379-383`); onboarding offers 4 levels; runs beside `DailyPracticeEngine` (CONF-045) | FM-005, FM-006, FM-017 |
| 3 Real-time dashboard (274-321) | 12 sections; reactive; offline persistence (Room or existing); updates only on validated, de-duplicated events | PARTIALLY_IMPLEMENTED | About 9 of 12 sections; achievements and "continue learning" absent; completion self-ticked (`DashboardStudioWidgets.kt:639`); no Room (`androidx.room` 0 hits) — local JSON instead | FM-013, FM-014 |
| 4 Streak engine (322-362) | Explicit daily requirement; once per day; timezone-safe; current/longest; calendar; milestones 3/7/14/30/60/100 | PARTIALLY_IMPLEMENTED | `StreakAndCompletionEngine.kt:21` milestones; current + best shown (`DashboardStudioWidgets.kt:703-760`); no calendar; UTC day; gameable; three other streaks remain | FM-054, FM-055 |
| 5 Notification centre (363-417) | Bell, badge, read/unread, delete, 10 categories, preferences, reminders, sync; no update alerts | PARTIALLY_IMPLEMENTED | Drawer implemented (`NotificationDrawer.kt:96-400`); update alerts excluded (`NotificationModels.kt:113-164`); 8 categories; no OS reminders; no sync; dead duplicate sheet | FM-057, FM-058, FM-059 |
| 6 Profile and settings (418-547) | Profile stats; Account, Learning, Appearance, AI (no cloud settings), Integrations, Data & Privacy (export, clear, delete with re-auth), Notifications, About; sign out | PARTIALLY_IMPLEMENTED | Basic profile (`UserProfileScreen.kt:96-532`); no account deletion UI (`MainCoachViewModel.kt:474`, 0 callers), export, About, font or animation settings; cloud AI controls present (`AnalyticsAndSettingsScreens.kt:650-713`) | FM-002, FM-003, FM-007, FM-070, FM-071, FM-075 |
| 7 Admin console (548-584) | Trusted authorisation enforced server-side; 13 modules; no default access to recordings; document the gap if no secure backend | PARTIALLY_IMPLEMENTED | Claims OR document role (`SpeakCraftRepository.kt:236-270`); 4 of 13 modules; MODERATOR can publish releases (`firestore.rules:843-852`); gap document not produced | FM-065, FM-066 |
| 8 Vocabulary builder (585-811) | Vocabulary home; 5-stage lifecycle; offline evidence-based evaluation; 75/75/70; inconclusive on low ASR confidence | PARTIALLY_IMPLEMENTED | **[dead]** Models and LPM functions have 0 callers (`LearningProgressManager.kt:394-491`); vocabulary home absent; UI is still the 4-tab, 20-word screen | FM-049, FM-050 |
| 9 Quiz, flashcards, notebook (812-892) | 9 quiz types ("AI Quiz" only if real); SRS flashcards; notebook with tags, edit, export | PARTIALLY_IMPLEMENTED | Quiz engine dead (`LocalVocabularyQuizEngine.kt`); SM-2 scheduler dead; ad hoc SRS live; "AI Quiz" label on a sentence check; notebook add only | FM-047, FM-048 |
| 10 Unified learning progress (893-937) | `LearningProgressManager`; 11 idempotent event types; local-first; multi-device sync and conflict rules | PARTIALLY_IMPLEMENTED | **[dead]** 3 of 21 operations called; events only from dead paths; `mergeSnapshots` unused; no Firestore sync | FM-056 |
| 11 UI/UX design system (938-996) | Premium, minimal, consistent, accessible, responsive; defined dashboard hierarchy | PARTIALLY_IMPLEMENTED | Violet/Indigo theme undocumented; tokens inconsistently used; dashboard over-dense; accessibility gaps | FM-010, FM-078, FM-079, FM-080 |
| 12 Database (997-1040) | Inspect before extending; stable IDs, schema versions, idempotency keys, migrations, indexes, rules; no parallel systems | PARTIALLY_IMPLEMENTED | Rules added for 5 learning and 3 admin collections (`firestore.rules:704-786`) but no client writes; no Room or `@Entity`; no migrations; no `firestore.indexes.json`; parallel systems created | FM-085, FM-086, FM-056 |
| 13 Testing (1041-1128) | Tests for every workflow; low/mid/high device testing | PARTIALLY_IMPLEMENTED | `@Test` grew from 99 (`4d6b917`) to 161 (`68a9cb1`), but `LearningProgressManager`, `DailyPracticePlanEngine`, `StreakAndCompletionEngine`, `SpacedRepetitionScheduler` and the quiz engine have no tests; no device testing | FM-081, FM-084 |
| *Supplementary:* critical architecture requirements (28-42) | Android-only; no LLM; no paid cloud AI dependency or cloud AI settings; offline core; no parallel systems; no client-boolean admin | PARTIALLY_IMPLEMENTED | Met: Android-only, no on-device LLM, server-checked admin. Breached: `firebase-ai` dependency and cloud settings (`app/build.gradle.kts:135`; FM-071); parallel plans, streaks and notification stores (FM-015, FM-055, FM-059) | FM-071, FM-089, FM-090 |
| *Supplementary:* required deliverables (1154-1204) | 10 documents under `docs/` plus an implementation and test report | DOCUMENTED_BUT_NOT_IMPLEMENTED | None of the 10 files exists (CONF-044); superseded by the Stage A set (GOV-02 §6) | — |

---

## 5. Verification claims

Every claim below is **UNVERIFIABLE**: nothing was built or run in Stage A, the repository holds no CI test job, test report, emulator log, deployment record or device artefact, and the AI Studio tools named in the documents (`compile_applet`, `deploy_firebase`) left no output (CONF-039). The "Static evidence" column records only what could be checked without execution.

| # | Claim | Source | Static evidence | Status |
|---|---|---|---|---|
| V-01 | "17/17 verified" tests | DOC-004 | 17 `@Test` at `1f77749` (count matches) | UNVERIFIABLE |
| V-02 | "30 / 30 unit & Robolectric CUJ tests passing" | DOC-056 | No commit ever had 30; 69 at its last edit (`7538b18`) | UNVERIFIABLE (count matches no commit) |
| V-03 | "41 / 41" tests | DOC-016, DOC-017 | 53 at `7ed7a24`, the commit that added both; no commit had 41 | UNVERIFIABLE (count matches no commit) |
| V-04 | "53 / 53" with per-suite breakdown | DOC-020 | 53 at `7ed7a24`; per-suite counts match | UNVERIFIABLE |
| V-05 | "69 / 69 (56 pre-existing)" | DOC-031 | 69 at `7538b18`; 56 at `dc7c9cb` | UNVERIFIABLE |
| V-06 | "161 / 161", 24 suites (incl. 10 AI-settings tests) | DOC-051 | 161 at `68a9cb1`; every per-suite count matches | UNVERIFIABLE |
| V-07 | "4/4 Firestore emulator suites passed" | DOC-029, DOC-031 | `firestore.test.js` has 4 cases; no `package.json`; not in CI | UNVERIFIABLE |
| V-08 | "Deployed live to Firebase via `deploy_firebase`"; "DEPLOYED" | DOC-029, DOC-031 | No deploy record; CI never deploys rules | UNVERIFIABLE |
| V-09 | "`compile_applet` → Build succeeded" | DOC-016, DOC-020 | Tool output not preserved; no Gradle wrapper committed | UNVERIFIABLE |
| V-10 | "ALL 10 OFFLINE REQUIREMENTS VERIFIED"; suites 5/3/4/3 | DOC-043 | Suite counts match code; the zero-cloud suite tests an interceptor that is never attached (FM-089) | UNVERIFIABLE |
| V-11 | "PASSED — ZERO CLOUD AI CALLS VERIFIED"; dependency scan "0 matches" | DOC-034 | Contradicted at HEAD: `firebase-ai` at `app/build.gradle.kts:135`, `Firebase.ai` at `FirebaseAiLogicService.kt:78` | UNVERIFIABLE (contradicted by code) |
| V-12 | MediaPipe "bundled in APK"; "models in assets process frames with zero network — PASS" | DOC-040, DOC-041, DOC-043 | `assets/models/` holds only `README.md` and `.gitkeep` | UNVERIFIABLE (contradicted by repository) |
| V-13 | "PASS (100% Stable)" on 3 hardware tiers; battery < 1.5% per 10 sessions; +3.2 °C | DOC-048 | Profile 1 "Simulated"; no device artefacts | UNVERIFIABLE |
| V-14 | WER 8.4 / 12.8 / 18.2%; RTF 0.14 / 0.11 / 0.08 | DOC-046 | No harness or dataset; the Whisper path cannot run (FM-034) | UNVERIFIABLE |
| V-15 | Latency and memory: Whisper ≈1.4 s per 10 s; peak RAM < 120 MB; "98.3% storage reduction"; "94% memory reduction" | DOC-042, DOC-044, DOC-047 | No profiler output; models absent | UNVERIFIABLE |
| V-16 | OTA path 1.0.0 → 1.1.0-beta.1 "Pass" | DOC-020, DOC-021 | Tags `v1.1.0-beta.7` … `v1.1.0-beta.23` exist; no install log; installer permission missing (FM-067) | UNVERIFIABLE |
| V-17 | Release tag "1.1.0-local-ai" | DOC-033 | No such git tag | UNVERIFIABLE (no tag) |
| V-18 | UI fixes "Verified" (onboarding wizard, dashboard redesign, articulation/pronunciation split) | DOC-016, DOC-017 | Named classes exist; the articulation test checks one header only (FM-029) | UNVERIFIABLE |
| V-19 | Launch animation "Implemented / Pass" | DOC-019, DOC-020 | 8 tests exist; cadences match code (FM-061) | UNVERIFIABLE |

**`@Test` count history** (unit tests in `app/src/test`, replayed with `git show`; RD-01 §2.2 and CONF-028 agree):

| Commit | Date | `@Test` | Documents claiming results at this point |
|---|---|---:|---|
| `1f77749` | 2026-11-18 | 17 | DOC-004 (17/17) |
| `7ed7a24` | 2026-11-18 | 53 | DOC-016/017 (41/41), DOC-020 (53/53) |
| `dc7c9cb` | 2026-11-19 | 56 | — (DOC-031 calls these "pre-existing") |
| `7538b18` | 2026-11-19 | 69 | DOC-031 (69/69), DOC-056 (30/30) |
| `36e2bf1` | 2026-11-19 | 99 | DOC-043 (suite counts only) |
| `4d6b917` | 2026-11-19 | 99 | — (DOC-059 committed) |
| `d8e2a05` | 2026-11-19 | 106 | — |
| `38d52f2` | 2026-11-20 | 122 | — |
| `68a9cb1` (HEAD) | 2026-11-20 | 161 (+1 instrumented) | DOC-051 (161/161) |

---

## 6. Use of this matrix

| Consumer | How it uses RD-04 |
|---|---|
| RD-06 Gap Analysis | Rows with BROKEN, DOCUMENTED_BUT_NOT_IMPLEMENTED and **[dead]** PARTIALLY_IMPLEMENTED become `GAP-nn` items. |
| RD-10 Feature Value Assessment | Uses the Disposition column as the starting recommendation for each existing feature. |
| GOV-01 RTM | Traces each FM row to its target CAP and, from there, to PRD/SRS requirements and tests. |
| RD-01 | DOC-004 and DOC-005 feature statuses are superseded by this matrix. |

Any change to a row's status or disposition after Product Owner review is made here first (GOV-02 §7).

## 7. Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial matrix: 90 features, PO-REQ-1119 phase check, verification-claims register | Product Analyst / QA Architect |
