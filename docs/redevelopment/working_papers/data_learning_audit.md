# SpeakCraft: learning/domain layer audit (read-only)

Scope: `data/activity`, `data/learning`, `data/evaluation`, `data/model`, `data/preferences`, `data/notifications`, `data/firebase/ActivityFirestoreRepository.kt`, how the UI consumes them, and the related unit tests.
Method: static reading only. No build was run because no JDK or SDK is available. All counts come from `grep` and `awk` run over the source.

**Path convention.** Unless a path is prefixed, it is relative to `app/src/main/java/com/example/`. `test/` means `app/src/test/java/com/example/`. `root/` means the repository root `f:\speakcraft`.
**Evidence tags.** **[V]** = VERIFIED, seen directly in code. **[I]** = INFERRED, reasoned from code or SDK semantics but not executed.

---

## 0. Executive summary (most important findings)

1. **Three parallel "learning systems" coexist with almost no data flow between them.** [V]
   - **(A) Activity Engine.** 150 activities, `ActivityEngine`, `UniversalEvaluationEngine`, `DailyPracticeEngine`, `LearningPathsEngine` and `ActivityProgressAnalyticsEngine`. Stored in the Firestore collections `activityProgress`, `activityAttempts`, `dailyPracticePlans` and `savedActivities`. Shown only inside the Activity Library (`ui/screens/ActivityDiscoveryScreen.kt:100-114`).
   - **(B) LearningProgressManager.** Lives in `data/learning`. Produces the dashboard daily plan, the dashboard streak, achievements and notifications. Stored only in a local JSON file (`data/learning/LearningProgressManager.kt:49-51`).
   - **(C) Legacy curriculum.** `CurriculumCatalog` with lessons, an 8-step daily routine, structured programs, a vocabulary bank and challenges. Stored in Firestore under `users/{uid}/…` (`dailyActivities`, `vocabulary`, `learningProgress`, `enrollments`, `speakingSessions`) and on `UserProfile.streakDays`.
   - The three systems compute their own streaks, minutes and plans independently.
2. **Most of LearningProgressManager is dead code.** [V] The UI calls only 3 of its 21 public operations: `loadForUser`, `completeDailyTask` and `ingestAdminAnnouncement` (`ui/AppNavigation.kt:547,590,934`). The following have no caller anywhere:
   - the 5-stage vocabulary flow, flashcards, quiz, notebook and SRS recording;
   - export and clear of learning data;
   - `mergeSnapshots`, which was meant for cloud sync;
   - `updateLearningPreferences`.

   There is no Firestore sync for this state at all, even though `root/firestore.rules:704-756` defines rules for `notifications`, `learningEvents`, `vocabularyProgress`, `quizAttempts` and `learningSnapshots`.
3. **The SM-2 `SpacedRepetitionScheduler` is not connected to the vocabulary UI.** [V] The flashcards use a separate ad hoc scheduler in `MainCoachViewModel.reviewVocabularyCard` with intervals 1/3/3n days capped at 21 (`ui/viewmodel/MainCoachViewModel.kt:344-374`). `LocalVocabularyQuizEngine` (9 question types) has zero callers, so it is DEAD.
4. **Activity scores mostly reflect text length, not speaking skill, and a wordless recording passes.** [V] Cloud AI is hard-disabled by `LocalExecutionPolicy.POLICY = "LOCAL_ONLY"` (`data/ai/local/LocalExecutionPolicy.kt:13`), so every activity is scored by the formula `56 + lengthRatio*22 + (recording?8) + checklist + discourse − fillers − grammar`, clamped to 30–96 (`data/activity/UniversalEvaluationEngine.kt:264-279`).
   - Activity audio is **never transcribed**: `ActivityUserResponse.transcript` is never set (`ui/viewmodel/ActivityEngineViewModel.kt:354-365`).
   - A wordless recording that is at least 1.5 s long and not silent scores **64 and passes** the 60 threshold.
   - A typed answer of 3 words scores 61 and passes.
   - The per-dimension scores are cosmetic: overall score +2, −1, −2 by rubric index (`UniversalEvaluationEngine.kt:281-282`).
   - Rubric weights (`weightPercent`) are ignored by local scoring.
5. **Users see conflicting streak numbers.** [V] There are four separate streak computations:
   - Firestore `UserProfile.streakDays`, updated from daily-routine step toggles (`data/firebase/SpeakCraftRepository.kt:670-684`);
   - the local LPM streak, from tapping dashboard tasks (`data/learning/StreakAndCompletionEngine.kt`);
   - the activity-attempt streak (`data/activity/ActivityProgressAnalyticsEngine.kt:34-40`);
   - `max(profile, local)` on the profile screen and in notifications (`ui/screens/UserProfileScreen.kt:153-156`, `ui/AppNavigation.kt:556-559`).

   Dashboard tasks can be marked complete with a single tap, without doing anything (`ui/components/DashboardStudioWidgets.kt:639-640`). That builds the streak and adds "practice minutes" equal to the planned duration (`LearningProgressManager.kt:373`).
6. **There are no OS notifications.** [V] The manifest has no `POST_NOTIFICATIONS`, and the codebase uses no NotificationCompat, AlarmManager or WorkManager (`app/src/main/AndroidManifest.xml:5-8`, `app/build.gradle.kts:105-151`). The "notifications" are an in-app list kept in two duplicate stores (LPM `UserNotification` and `NotificationManagerRepository` `StoredAlert`), with a one-way sync between them. This produces duplicate daily "plan ready" and "streak" alerts. `preferredReminderTime = "09:00"` is never read.
7. **Intellectual-property and attribution risk.** [V] `data/model/CurriculumCatalog.kt` contains 102 mentions of "Vinh" or "@askvinh":
   - 15 "technique" seeds attributed to Vinh Giang, 12 of them marked `"VERIFIED"`;
   - specific video titles and timestamps (for example `"01:15"`), all pointing at the channel root `https://www.youtube.com/@askvinh` rather than a specific video;
   - a program whose subtitle is "Adapted from @askvinh's 30-Day Mastery Plan" (`CurriculumCatalog.kt:1005`).

   The 150 activities carry `sourceAttribution = "SpeakCraft Core Curriculum"` but use framework names associated with the same creator (High-Low-Buffalo, VAKS, Power Zone). `root/metadata.json` says the app is "built from @askvinh YouTube lessons". No licence or permission artefact was found.

---

## 1. Domain model inventory

### 1.1 Activity domain (`data/activity/ActivityModels.kt`) [V]

| Model | Lines | Meaning |
|---|---|---|
| `ExerciseTemplateType` (25 values) | 9-38 | Exercise template, each mapped to an `InteractionFamily` |
| `InteractionFamily` (7) | 40-48 | Which UI renderer and evaluation gate to use |
| `ActivityDifficulty` (3: Beginner/Intermediate/Advanced) | 50-54 | 3-tier difficulty (the learning layer has 5 tiers, see 1.2) |
| `ActivitySkillCategory` (13) → `DailyPracticePillar` | 56-70 | Skill taxonomy used by the plan pillars |
| `DailyPracticePillar` (7) | 72-80 | Pillars for the activity daily plan |
| `EvaluationStrategyType` (9) | 82-92 | Evaluator strategy names |
| `ActivityPermission` (MIC/CAMERA) | 94-97 | Runtime permissions per activity |
| `ActivityModuleDefinition` | 99-106 | One of the 10 modules (`targetActivityCount` unused outside the catalogue, see §9) |
| `InteractionConfiguration` | 108-122 | Prompt, recording flags, listening script, dialogue opener, checklist; `targetWordCountMin` defaults to 12 (120) |
| `EvaluationConfiguration` | 124-129 | Strategy, `aiEnabled` (never read), `focusCriteria` (never read), `honestyCaveat` |
| `ScoringRubricDimension` | 131-136 | key/label/weight/criteria; the weight is used only in the cloud prompt |
| `CompletionCriteria` | 138-144 | Only `passingScoreThreshold` and `requiresChecklistCompletion` are read; `minDurationSeconds`, `minWordsOrTurns` and `requiresRecording` are never read (§9) |
| `FeedbackConfiguration` | 146-154 | Never read outside the definition (§9) |
| `ActivityDefinition` | 156-177 | Full activity |
| `ActivityUserResponse` | 189-208 | User submission; `combinedTextForAnalysis` joins transcript, typed text and the user's dialogue turns (202-207) |
| `ActivityEvaluationResult` | 217-236 | Score, feedback sections, separate confidence self-rating |
| `LearningPathDefinition` / `LearningPathMilestone` / `LearningPathProgressSummary` | 238-265 | 10 activity paths |
| `DailyPracticeBlock` / `DailyPracticePlan` | 267-284 | Activity-based daily plan |
| `ModuleCompletionStat`, `SkillTrendComparison`, `ActivityAnalyticsSnapshot` | 286-315 | Activity analytics |
| Firestore: `UserActivityProgress`, `ActivityAttemptRecord`, `SavedActivityRecord`, `DailyPracticePlanRecord` | 319-392 | Persisted under `users/{uid}/…` |

Also in this package:
- `ActivityCatalogue.kt`: `ActivityDurationFilter`, `ActivitySearchFilter`, `ActivityValidator` (19-103).
- `ActivityEngine.kt`: `LoadedActivityResult`, `ActivitySubmissionOutcome`.
- `UniversalEvaluationEngine.kt`: `LocalSpeechMetrics` (103-108) and 9 `ActivityEvaluatorStrategy` classes (19-101). These only produce prompt text and a caveat. `buildEvaluatorInstructions` is consumed only by the cloud path (`data/ai/cloud/FirebaseAiLogicService.kt:767`), which is disabled.

### 1.2 Learning domain (`data/learning/LearningModels.kt`) [V]

| Model | Lines | Meaning / status |
|---|---|---|
| `LearningLevel` (5 tiers incl. ELEMENTARY, UPPER_INTERMEDIATE) | 9-28 | Overlaps with `ActivityDifficulty` (3 tiers) and the free-text `UserProfile.communicationLevel` |
| `LearningGoal` (9) | 34-61 | Goal enum. It never reaches the plan from the profile (§3.1) |
| `LearningEventType`, `LearningEvent` | 67-93 | Idempotent event log. Only produced internally by dead vocabulary, quiz and flashcard functions |
| `DailyPracticeTask`, `PersonalizedDailyPlan` | 96-129 | Dashboard daily plan. `isCompleteForStreak` = ≥60% done, or ≥2 tasks, or ≥ half the minutes (127-128) |
| `LearningStreakState`, `AchievementRecord` | 132-150 | Local streak and badges |
| `NotificationCategory` (10), `UserNotification`, `NotificationPreferences` | 156-203 | **Duplicate** of `data/notifications` `AppAlertCategory`/`StoredAlert`/`AlertPreferences` |
| `WordLearningStage`, `VocabularyWordProgress`, `SpacedRepetitionItem`, `VocabularyNotebookItem` | 210-269 | 5-stage vocabulary model. **Duplicate** of Firestore `VocabularyEntry` (`data/model/FirestoreModels.kt:87`), which is what the UI actually uses |
| `QuizQuestionType` (9), `QuizQuestion`, `QuizAnswerReview`, `QuizSessionResult` | 272-319 | Only used by the dead `LocalVocabularyQuizEngine` and the dead `recordQuizSession` |
| `AdminRole`, `AdminAuditRecord`, `AdminAnnouncement` | 326-368 | Admin RBAC models that ended up in the learning package; used by `SpeakCraftRepository` and `MainCoachViewModel` |
| `PersistedLearningSnapshot` (schema v2) | 375-400 | Whole LPM state. `selectedGoals` defaults to a non-empty list (380), which blocks profile goal ingestion (§3.1) |

### 1.3 Evaluation domain (`data/evaluation`) [V]
- `EvaluationModels.kt` defines:
  - `CoachEvaluationState` (12 states, 9-22);
  - `DimensionAvailability` (24-29);
  - `EvaluationDimensionDetail`, where `score` is nullable (31-40);
  - `EvidenceIssueCode` (42-57);
  - `EvidenceValidationResult` (59-75);
  - `CoachEvaluationReport` (77-96);
  - `InsufficientSpeechEvidenceException` (103-107).
- `EvaluationEvidenceValidator.kt` defines `EvidenceValidationInput` and `ValidatedEvidenceBundle` (9-34).
- `EvidenceBasedScoringEngine.ScoringInput` is at `EvidenceBasedScoringEngine.kt:26-43`.

### 1.4 Legacy curriculum and profile (`data/model`) [V]
- `CurriculumCatalog.kt:3-139`: `CuratedTechniqueSeed`, `ModuleDefinition`, `LessonDefinition`, `VocabSeed`, `SpeakingChallengeItem`, `PronunciationDrill`, `ShadowingSegment`, `GrammarSpeakingLesson`, `PublicSpeakingModule`, `ProgramWeekPlan`, `StructuredProgram`, `DailyStepDefinition`.
- `FirestoreModels.kt` holds 20 Firestore classes, including:
  - `UserProfile` (17-40). It has hard-coded default skill scores of 62/65/60/68/58/66 (lines 31-36) and confidence 55.
  - `DailyActivity` (58), `UserLearningProgress` (72), `VocabularyEntry` (87), `SpeakingSession` (110).
  - `LearningModule` (294) and `CommunicationExercise` (328), which are unused (§9).
- `FeatureTutorialRegistry.kt`: 24 `FeatureTutorial` entries.

### 1.5 Duplicate and overlapping engines: who is actually used

| Pair or group | Used by the UI? (grep evidence) | Status |
|---|---|---|
| **`activity/DailyPracticeEngine`** vs **`learning/DailyPracticePlanEngine`** | `DailyPracticeEngine` is used by `ActivityEngineViewModel.buildEffectiveDailyPlan` (`ui/viewmodel/ActivityEngineViewModel.kt:469-495`), rendered in the Activity Library (`ui/screens/ActivityDiscoveryScreen.kt:112-114`). `DailyPracticePlanEngine` is used only by `LearningProgressManager.loadForUser` (`LearningProgressManager.kt:99-117`); its plan is rendered on the Dashboard through `learningSnapshot.dailyPlan` (`ui/screens/DashboardScreen.kt:186`). **A third "daily practice" exists:** `CurriculumCatalog.dailySteps` (8 steps) on the `daily_practice` route (`ui/AppNavigation.kt:949-957`, `ui/screens/DailyAndProgramsScreens.kt:76`) | **DUPLICATED ×3.** All three are live, on different screens, with different units (catalogue activities, generic route tasks, fixed steps) and different persistence (Firestore `dailyPracticePlans`, a local JSON file, Firestore `dailyActivities`) [V] |
| **`ActivityProgressAnalyticsEngine`** vs **`LearningProgressManager`** vs **`StreakAndAnalyticsHelper`** vs **`StreakAndCompletionEngine`** | `ActivityProgressAnalyticsEngine` is used by `ActivityEngineViewModel.analyticsSnapshot` (181-190) and appears in the Activity Library stats (`ActivityDiscoveryScreen.kt:164-165`). `StreakAndCompletionEngine` is used only inside LPM; its streak appears on the Dashboard (`DashboardStudioWidgets.kt:703-704`). `StreakAndAnalyticsHelper` is used by `SpeakCraftRepository.saveDailyActivity` (648, 673) for `UserProfile.streakDays`, and its `filterSessionsByPeriod` is used by the Dashboard and Analytics screens (`DashboardScreen.kt:229`, `ui/screens/AnalyticsAndSettingsScreens.kt:98`) | **DUPLICATED.** All are live, with four streak definitions (§5) [V] |
| **`UniversalEvaluationEngine`** vs **`EvidenceBasedScoringEngine`** vs **`EvaluationEvidenceValidator`** | `UniversalEvaluationEngine` scores Activity Library submissions (`data/AppContainer.kt:109-119`). `EvidenceBasedScoringEngine` scores the Speaking Lab and Challenges (`data/ai/local/LocalIntelligenceOrchestrator.kt:214,288`, `ui/viewmodel/SpeakingAndLabsViewModel.kt:482`). `UniversalEvaluationEngine` calls `EvidenceBasedScoringEngine` only to build the failure report it throws (`UniversalEvaluationEngine.kt:207-218`). `EvaluationEvidenceValidator` is a shared gate used by both | **Two separate scoring philosophies.** Speaking Lab: per-dimension acoustic and transcript scores, null when evidence is missing. Activities: a length-based heuristic with fabricated dimension spreads. They are not duplicates in code, but they are inconsistent in method [V] |
| **`CurriculumCatalog`** vs **`ActivityCatalogue`** | Both are live. `CurriculumCatalog` drives the lessons, modules, vocabulary, labs, challenges, programs and daily routine screens (consumer list in §2.2). `ActivityCatalogue` drives the Activity Library and `activity/{id}` (`ui/AppNavigation.kt:534-536`) | **DUPLICATED taxonomy.** `CurriculumCatalog.contentModules` (15 modules) and `ActivityCatalogue.modules` (10 modules) cover the same topics (storytelling, body language, public speaking, professional, and so on) with different IDs. They do not cross-reference each other [V] |
| **`LearningPathsEngine`** vs **`StructuredPrograms`** | The `learning_paths` route renders `LearningPathsScreen`, which shows `CurriculumCatalog.structuredPrograms` (7 programs with manual day toggles; `ui/AppNavigation.kt:960-966`, `DailyAndProgramsScreens.kt:272-277`). `LearningPathsEngine` (10 paths) is shown only inside the Activity Library (`ActivityDiscoveryScreen.kt:104,422`) | **DUPLICATED concept.** `LearningPathsEngine` is PARTIALLY used: `prerequisitesMet` is computed (`LearningPathsEngine.kt:313-317`) but never read in the UI (grep finds no `prerequisitesMet` in `ui/`) [V] |
| **Recommenders** | `ActivityEngine.recommendNextActivities` (`ActivityEngine.kt:141-200`); `DailyPracticeEngine.scoreCandidate`; `data/ai/RecommendationEngine.recommendNextAction` (Dashboard CTA, used by `MainCoachViewModel`); `DailyPracticePlanEngine` | Four independent recommenders with different signals [V] |
| **Vocabulary** | The UI uses Firestore `VocabularyEntry` and `MainCoachViewModel.reviewVocabularyCard`. The LPM 5-stage model, its SRS and `LocalVocabularyQuizEngine` have no callers | LPM vocabulary is **DEAD** [V] |
| **Notifications** | `NotificationManagerRepository` feeds the bell drawer (`ui/AppNavigation.kt:1303-1316`). LPM notifications reach the drawer only through `syncFromLearningNotifications` (`ui/AppNavigation.kt:585-587`). `NotificationsCenterSheet` (`ui/components/NotificationsCenterSheet.kt:79`) has **no callers** | DUPLICATED, partly DEAD [V] |
| **Tutorial state** | `TutorialStateManager` (per-user DataStore) is used by `ui/AppNavigation.kt:305,622,1146-1293`. The global keys `completed_tutorial_keys` and `first_use_tutorials_enabled` in `AppPreferencesRepository` (114-115, 203-227) are only read as a default parameter in `AnalyticsAndSettingsScreens.kt:408`, which `AppNavigation.kt:1144` overrides | Legacy keys are DEAD in production [V] |

---

## 2. Content

### 2.1 Activity catalogue (`data/activity`) [V]
- **10 modules** (`ActivityCatalogue.kt:107-188`) and **150 activities**, unique and all valid. Grep of `id = "mNN_` gives 45 + 45 + 60. Per-module counts: m01–m07 and m09 have 15 each, m08 has 10, m10 has 20. Unit tests assert the same numbers (`test/ActivityEngineUnitTest.kt:39-75`).
- All referenced `nextActivityIds` and path IDs exist; a grep cross-check found 0 missing.
- **Difficulty:** 57 Beginner, 77 Intermediate, 16 Advanced. m05 has no Advanced activities; m10 has 5.
- **Duration:** 4–10 min; 72 activities are 6 min. Total about 921 min (m10: 139 min, m08: 58 min).
- **Exercise templates:** all 25 are used, but unevenly. Examples: 16 SPEAKING_EXERCISE, 14 ROLEPLAY, and only 1 each of TONGUE_TWISTERS, WORD_ASSOCIATION and INTERVIEW_SIMULATION.
- **Skill categories:** 12 of the 13 are used; `REFLECTION` is used by none. Only 4 activities are VOCABULARY and 2 are GRAMMAR, so the plan's VOCABULARY pillar draws from 6 activities.
- **Evaluation strategies:** 31 PRESENTATION, 30 CONVERSATION, 29 SPEAKING, 15 STORYTELLING, 14 PRONUNCIATION, 13 LISTENING, 9 REFLECTION, 6 GRAMMAR, 3 VOCABULARY.
- **Media:** 88 activities support audio and 13 support video (all in m07–m10). Mic and camera permissions match the audio and video flags; an awk check found no mismatches.
- **Interaction extras:** 19 activities have listening scripts, 23 have dialogue openers, 23 have checklists, and 6 have custom honesty caveats.
- **Authoring style.** Each activity's text is hand-written, but the structure is templated:
  - every activity goes through one `buildActivity(...)` helper per file (`ActivityCatalogueModules1To3.kt:25-94` and equivalents);
  - there is **exactly one rubric per module**, with 3 dimensions each (10 rubric functions, e.g. `speakingRubric()` at `ActivityCatalogueModules1To3.kt:7-11`);
  - 147 of 150 activities have **exactly 3 instructions** and all 150 have **exactly 1 example**;
  - `sourceAttribution` is the constant "SpeakCraft Core Curriculum" (`ActivityCatalogueModules1To3.kt:92`, `ActivityCatalogueModules4To6.kt:85`, `ActivityCatalogueModules7To10.kt:90`);
  - `contentVersion` is always 1.
- **Quality issues:**
  - Duplicate titles: "Simple Explanations" appears in m01 (`ActivityCatalogueModules1To3.kt:327`) and m04 (`ActivityCatalogueModules4To6.kt:228`). "Technical Explanations" appears in m04 (`ActivityCatalogueModules4To6.kt:412`) and m10 (`ActivityCatalogueModules7To10.kt:1359`). Each pair has different descriptions.
  - Module and activity skills disagree: module 1 "English Fluency" contains VOCABULARY and GRAMMAR activities (e.g. `ActivityCatalogueModules1To3.kt:330`) that are still scored with `speakingRubric()`.
  - `CompletionCriteria.minDurationSeconds`/`minWordsOrTurns` are populated by the builder (`ActivityCatalogueModules1To3.kt:84-89`) but never enforced (§9).
  - Path difficulty labels are inconsistent: "Beginner" paths include Advanced activities (path_04 has 1, path_05 has 1), and Intermediate paths include up to 5 Advanced and 4 Beginner activities (awk join of path IDs with activity difficulty).
  - No placeholder or TODO text was found in the activity catalogue.

### 2.2 Legacy curriculum (`data/model/CurriculumCatalog.kt`) [V]

| Collection | Count | Lines | Consumer |
|---|---|---|---|
| `allCategories` | 20 strings | 143-164 | `ui/screens/YouTubeIntelligenceScreen.kt` |
| `contentModules` | 15 | 167-183 | Admin, nav, dashboard, `ui/screens/LessonAndModulesScreens.kt` |
| `askVinhKnowledgeBase` (techniques) | 15 (12 `VERIFIED`, 3 `PENDING_TRANSCRIPT`) | 187-519 | Lessons screen; also seeded into Firestore `extractedTechniques` on onboarding (`data/firebase/SpeakCraftRepository.kt:532-590`, `ui/viewmodel/MainCoachViewModel.kt:230`) |
| `lessons` (generated from the VERIFIED techniques) | 12 | 521-542 | Lessons, Dashboard, `data/ai/RecommendationEngine.kt` |
| `vocabularyBank` | **20 words**, 4 per level × 5 levels | 544-885 | `ui/screens/VocabularyScreen.kt`, `MainCoachViewModel.kt:172-179` |
| `speakingChallenges` | 10 | 887-898 | Challenges screen |
| `conversationScenarios` | 13 strings | 900-914 | Conversation screen |
| `pronunciationDrills` | 8 | 916-925 | Labs |
| `shadowingSegments` | 3 | 927-958 | Labs |
| `grammarLessons` | 7 | 960-968 | Labs |
| `publicSpeakingModules` | 6 | 970-977 | Labs |
| `realWorldTaskSeeds` | 7 strings | 979-987 | Labs |
| `dailySteps` | **8**, although the module subtitle says "Guided 9-step routine" (170) | 989-998 | Daily Practice screen |
| `structuredPrograms` | 7 programs, 14 week plans; only the 30-day program has day-level drills | 1000-1123 | `LearningPathsScreen` |

**Totals actually defined in code:**
- 10 + 15 modules;
- 150 activities;
- 12 lessons;
- 25 exercise templates;
- 20 vocabulary items;
- 10 challenges, 8 pronunciation drills, 3 shadowing segments, 7 grammar lessons, 6 public-speaking modules, 7 programs, 10 learning paths.

### 2.3 Content provenance and IP risk [V]
- **Volume of references.** "Vinh"/"askvinh" appears 102 times in `CurriculumCatalog.kt` and 22 times in `FeatureTutorialRegistry.kt`. It also appears in about 15 UI files, `DeterministicFeedbackTemplates.kt:57,210,229`, `CameraVideoRecorder.kt:192`, `RecommendationEngine.kt:59,73` and `SpeakCraftRepository.kt:575,583,1006`.
- **Content tied to the creator.** All 15 technique seeds quote "Vinh Giang teaches…" (e.g. `CurriculumCatalog.kt:195,197`) and carry `sourceVideoTitle` values that look like real video titles (204-447). Their timestamps (`"01:15"`, `"00:45"`, …; lines 206-448) are attached to `sourceVideoUrl = "https://www.youtube.com/@askvinh"`, the channel root, for every VERIFIED item (205-447). **[I]** The timestamps therefore cannot be verified and may be fabricated, while the items are still labelled "VERIFIED".
- **Derived material.** Several items are adaptations of the creator's material:
  - "Adapted from @askvinh's 30-Day Mastery Plan" (1005);
  - all 10 challenges have `sourceReference` "@askvinh — …" (888-897);
  - `chal_video_summary` asks the user to summarise Vinh Giang's 3Vs (895);
  - the shadowing scripts are attributed to @askvinh videos (931-952).
- **Activity catalogue attribution.** Module subtitles name High-Low-Buffalo, FORD, VAKS, CCC and Power-Zone (`ActivityCatalogue.kt:136,144,160,168`). A grep finds 128 framework-keyword hits across the catalogue files, while every activity's `sourceAttribution` says "SpeakCraft Core Curriculum". That is a mismatch in either direction, depending on whether the frameworks really are third-party.
- **Store listing.** `root/metadata.json` describes the app as "built from @askvinh YouTube lessons", which is a public claim of derivation.
- **Risk.** No licence, permission or creator agreement was found in code. Using the creator's name, framework names, quoted advice, video titles and an "adapted" 30-day plan without documented permission is a trademark, copyright and passing-off exposure. The "VERIFIED" labels are also not backed by real video links. This is legal judgement territory: flag it for review.

---

## 3. Learning engine logic

### 3.1 Daily plans
**(A) Activity plan: `DailyPracticeEngine.generatePlan`** (`data/activity/DailyPracticeEngine.kt:49-184`). [V]
- **Inputs:** target minutes (snapped to 10/20/30/45/60 at lines 7 and 59), profile level, progress, recent attempts, pinned IDs, and IDs completed today.
- **Level mapping** is an exact lowercase match: "beginner" or "advanced", otherwise Intermediate (60-64). As a result, "Upper-Intermediate" and "Elementary" from onboarding or profile become Intermediate [V]. The same mapping appears in `ActivityEngine.kt:185-189`.
- **Weakest skill** = lowest average attempt score; if there are no attempts, the lowest self-reported profile skill (74-79, 271-282).
- **Algorithm:**
  1. Place pinned activities first (88-103).
  2. For each pillar in the duration's pillar list (9-47), pick the highest `scoreCandidate` (106-144). The score starts at 100, then:
     - −55 if the activity is in the last 15 attempts;
     - −20 if already completed;
     - +25 / +10 / −12 for difficulty match;
     - +30 for the weakest skill;
     - +22 for a goal-keyword hit;
     - plus a deterministic `hash(activityId xor dateKey) % 13` jitter (230-268).
  3. Top up while there is room (147-165).
- **Bug [I, high confidence].** The plan is regenerated every time the attempt list changes (`ActivityDiscoveryScreen.kt:112-114`, `ActivityEngineViewModel.kt:469-495`), and today's attempts are in `recentAttempts`, which carry the −55/−20 penalties. An activity just completed is therefore likely to be replaced, so its `completed` checkmark (from `completedTodayIds`) rarely survives. Only swapped activities are persisted as pinned (`ActivityEngineViewModel.kt:509-525`). The persisted `plannedActivityIds` are never read back, and the selected minutes reset to 20 on every ViewModel start (`ActivityEngineViewModel.kt:108`).

**(B) Dashboard plan: `DailyPracticePlanEngine.generateOrAdaptPlan`** (`data/learning/DailyPracticePlanEngine.kt:31-243`). [V]
- This is a fixed template of generic "route tasks", not catalogue content:
  - vocabulary (always);
  - remediation, chosen by substring matches such as "filler"/"um", "grammar" or "pace" against recent session mistakes (87-128); **[I]** note that the substring "um" also matches words like "summary" or "number";
  - pronunciation, if it is the weakest skill or a goal;
  - a goal task;
  - a speaking recording;
  - listening, grammar and reflection.

  At most 4, 5 or 6 tasks are kept, depending on recovery mode and budget (213-221).
- **Recovery mode** applies when 2 or more days were missed (34).
- **Level** = `LearningLevel.fromLabel(profile.communicationLevel)` (`LearningProgressManager.kt:83-84`).
- **Goals.** `loadForUser` uses the profile goal only if `selectedGoals` is empty (`LearningProgressManager.kt:86-88`). The default is `[DAILY_FLUENCY, CONFIDENCE]` (`LearningModels.kt:380`), and `updateLearningPreferences` is never called. **The user's chosen goal therefore never affects the Dashboard plan.** [V]
- **Task completion** is only a manual tap through `completeDailyTask` (`ui/AppNavigation.kt:933-935`). `recordEvent` auto-completion (`LearningProgressManager.kt:261-266`) is reachable only from dead functions. [V]
- The plan text promises a "5-stage Learn → Speak → Sentence → Complete workflow" (`DailyPracticePlanEngine.kt:78`) that does not exist in the UI. The vocabulary screen tabs are Words, Flashcards, AI Quiz and Notebook (`ui/screens/VocabularyScreen.kt:109`). [V]

**(C) Daily routine (`CurriculumCatalog.dailySteps`).** [V] Eight steps the user ticks manually. Each toggle writes `dailyActivities/{date}`. Minutes are computed as `steps × 3`, with a minimum of 3 (`MainCoachViewModel.kt:275`), so the number is fabricated rather than measured.

### 3.2 Levels and goals
- **Level vocabularies across the app:**
  - Onboarding offers 4: Beginner, Intermediate, Upper-Intermediate, Advanced (`ui/screens/AuthAndOnboardingScreens.kt:450`).
  - The profile editor offers 5, including Elementary and "Upper Intermediate" (`ui/screens/UserProfileScreen.kt:76-82`).
  - Activities have 3; LearningLevel has 5. [V]
- **How level is set.** Level is self-selected. Choosing a level presets the "baseline" skill sliders, for example Beginner = 42/45/48/50 (`AuthAndOnboardingScreens.kt:363-380`, Beginner preset at 366), and the user can adjust them. There is **no placement test** and no automatic level progression anywhere in the codebase [V]. Profile skill scores are later blended at a 3:2 ratio with evaluated Speaking Lab session dimensions (`SpeakCraftRepository.kt:869-889`), but the level string never changes.

### 3.3 Learning paths
- There are 10 paths of 12–17 activities each: 136 slots covering 117 unique activities, with 17 activities appearing in more than one path. Several paths simply relist a module (path_04 = all of m02; path_08 = all of m07) (`LearningPathsEngine.kt:5-302`).
- Progress = count of completed activities. Milestones are count thresholds. Prerequisites are met at 70% of the prerequisite path (313-317).
- Nothing enforces order or prerequisites in the UI: a path card opens the next uncompleted activity (`ActivityDiscoveryScreen.kt:463-465`), and `prerequisitesMet` is unused. [V]
- An activity counts as "completed" once any attempt scored at or above 60 (`ActivityEngine.kt:92`). Combined with §4, a single 2-second recording completes it.

### 3.4 Spaced repetition
- **`SpacedRepetitionScheduler`** (`data/learning/SpacedRepetitionScheduler.kt`) is a simplified SM-2 [V]:
  - binary quality: 4 for "known", 2 for "not known" (28);
  - the SM-2 ease formula, clamped to 1.3–3.0 (29). At q=4 the ease delta is exactly 0, so ease can only fall;
  - intervals of 1, 3 and 6 days, then `prevInterval×ease` clamped to 7–60 (35-40); standard SM-2 uses 1, 6, then I×EF;
  - status MASTERED when reps ≥ 4 and interval ≥ 14 (45);
  - epoch day = UTC `ms/86400000` (`LearningProgressManager.kt:47`), not the local day. **[I]** For users east of UTC, cards roll over at the UTC midnight rather than local midnight.
- **Wiring:** it is **not connected to the vocabulary UI.** The only live call is `dueItems()` inside `loadForUser`, which is always 0 because nothing creates SRS items. As a result, the "N vocabulary words due" notification (`LearningProgressManager.kt:142-152`) never fires. [V]
- **What the UI actually uses** is `MainCoachViewModel.reviewVocabularyCard` (`ui/viewmodel/MainCoachViewModel.kt:344-374`) [V]:
  - known → reps+1 and mastery+1; unknown → reps=0 and mastery−1;
  - interval 1, then 3, then 3×reps, capped at 21 days;
  - no ease factor;
  - stored in Firestore `vocabulary/{wordId}`.

  Every word with no entry is "due" (`VocabularyScreen.kt:220-224`). The first review, even "Need review", creates the entry and increments `wordsLearnedCount` (`SpeakCraftRepository.kt:746-769`), so the "words learned" stat counts words *seen*, not words learned [V]. `dailyVocabularyBatch` is computed `by lazy` (`MainCoachViewModel.kt:172`), so it does not roll over at midnight while the ViewModel is alive [I].

### 3.5 Vocabulary quiz generation
- **`LocalVocabularyQuizEngine`** (`data/learning/LocalVocabularyQuizEngine.kt`) is **DEAD**: grep finds 0 callers in main or test. [V]
- **Quality, had it shipped:**
  - the default seed is `System.currentTimeMillis()` (21), which contradicts the "deterministic" claim at line 9;
  - with a 20-word bank and question counts up to 15, targets repeat (34-36);
  - "Correct Sentence" distractors are nonsense templates such as `"Yesterday I ${word}ed to the grocery aisle…"` (182-184), for example "articulateed", which makes the right answer trivially guessable.
- **What the UI's "AI Quiz" tab really is:** a single-sentence usage check through `evaluateVocabUsage` and `HybridAiCoachCoordinator.evaluateVocabularyUsage` (`MainCoachViewModel.kt:407-432`). It is local-only under the LOCAL_ONLY policy [V]. It is not a quiz.

---

## 4. Evaluation and scoring

### 4.1 Activity Library submissions (`UniversalEvaluationEngine`) [V]
1. **Submission gate** (`ActivityEngine.kt:46-65`): the response needs any of text, media, checklist ticks or dialogue turns.
2. **Cloud path:** `aiClientOverride` → `HybridAiCoachCoordinator.evaluateUniversalActivityIfCloud` (`data/AppContainer.kt:109-115`) returns null whenever `LocalExecutionPolicy.isLocalOnly()`. The policy constant is `"LOCAL_ONLY"` (`data/ai/local/LocalExecutionPolicy.kt:13`; `data/ai/HybridAiCoachCoordinator.kt:74-78,341-347`). **In this build, every activity is scored locally.** If cloud were enabled and the call failed, the coordinator re-throws the error (`HybridAiCoachCoordinator.kt:350-354`) and `evaluate` has no try/catch (`UniversalEvaluationEngine.kt:227-233`), so the user would see an error rather than a local fallback [V].
3. **Evidence used:**
   - `combinedTextForAnalysis`: typed text plus the user's dialogue turns. **The transcript field is never populated**, because the ViewModel omits it (`ActivityEngineViewModel.kt:354-365`) and the activity screen has no speech-to-text; the text box asks users to type a "Spoken transcript" (`ui/components/ExerciseTemplateRenderers.kt:434`).
   - `durationSeconds` is the **activity timer since Start**, not recording length (`ActivityEngineViewModel.kt:236-241,281-284`). WPM = typed words ÷ elapsed time, clamped to 30–320 (`UniversalEvaluationEngine.kt:126-130`), so it measures typing and thinking time, not speech rate.
   - Audio and video files pass only through `EvaluationEvidenceValidator` as a gate:
     - audio needs ≥1.5 s, RMS ≥0.008 and ≥0.5 s voiced (`EvaluationEvidenceValidator.kt:42-45`);
     - video counts as "valid" when the file merely exists and is non-empty, because the engine fakes `videoFramesAnalyzed = 1` and `videoAnalysisCompleted = true` (`UniversalEvaluationEngine.kt:200-201`), so no frames are analysed;
     - a video-only submission (no separate audio) also needs ≥2 typed words, because with `audioRequired=false` the validator requires a valid transcript (`EvaluationEvidenceValidator.kt:243`).
4. **Score formula** (`UniversalEvaluationEngine.kt:258-279`):
   ```
   lengthRatio = clamp(words / max(targetWordCountMin(=12), 8), 0, 1.35)
   base = 56 + int(lengthRatio*22) + (verifiedMedia ? 8 : 0) + int(checklistRatio*8)
          + discourseBonus(0..12) − min(fillers*4, 18) − min(grammarIssues*5, 16)
   checklist warm-up with <6 words: 48 + checklistRatio*38
   result clamped to 30..96; pass = base ≥ 60
   ```
   - **Per-dimension scores:** `base + 2` for the first rubric dimension, `base − idx` for the others (281-282). The rubric weights are ignored and the notes are generic.
   - **Determinism:** fully deterministic. There is no Random in the scoring path (a grep finds Random only in the quiz engine).
5. **Edge cases (computed from the formula above):**
   - Empty text, no media: error "enter at least 2 words" (221-225).
   - 2 typed words: 59, fail.
   - 3 typed words: 61, **pass**.
   - 12 words, clean: 78.
   - About 17 or more clean words: 85, and up to 96 with discourse cues.
   - **A valid recording with 0 typed words: 64, pass, and the activity is marked completed** (`ActivityEngine.kt:92`).
   - Silent or short audio: throws `InsufficientSpeechEvidenceException` carrying a null-score report (206-218), so nothing is saved.
6. **Misleading honesty text.**
   - Pronunciation activities with audio get the caveat "Audio review assesses intelligibility, rhythm, and audible word clarity honestly…" (`UniversalEvaluationEngine.kt:35-40`), and catalogue caveats say "audio review checks resulting spoken clarity" (`ActivityCatalogueModules1To3.kt:465,491,517,639`). **No audio feature contributes to the score** apart from the validity gate and the +8.
   - Dimensions named "Articulation Crispness", "Word & Sentence Stress" and "Controlled Pace" (`ActivityCatalogueModules1To3.kt:13-17`) are scored from typed text length. **This is pronunciation scoring from text.** [V]
7. **Cloud parser (unreachable).** It defaults a missing overall score to **74** and clamps every score to at least 20, and it fills missing dimensions with the overall score (`data/ai/cloud/FirebaseAiLogicService.kt:783,793-805`). That is fabricated filler if it is ever re-enabled [V].

### 4.2 Speaking Lab and Challenges (`EvaluationEvidenceValidator` + `EvidenceBasedScoringEngine`) [V]
- **Pipeline** (`LocalIntelligenceOrchestrator.kt:171-306`): decode audio → (optional) on-device ASR → validate → language analysis → transcript-versus-expected-text alignment → fuse features → score.
- **Fail-closed behaviour.** Invalid evidence produces `overallScore = null`, with every dimension marked unavailable (`EvidenceBasedScoringEngine.kt:49-75`). If no dimension can be evaluated, the result is `ANALYSIS_FAILED` (146-160). Overall = unweighted mean of the evaluated dimensions, flagged as partial when any are missing (165-175). This design is good.
- **What each dimension measures:**

  | Dimension | Lines | Formula and evidence |
  |---|---|---|
  | Fluency | 354-372 | `38 + speechRatio*34 + continuity*20 + rateConsistency*12`, minus fillers, ±WPM band, minus long pauses; clamped to 10–98. Acoustic VAD, reasonable |
  | Pronunciation (with expected text) | 411-426 | ASR transcript compared with the expected text (`PronunciationAnalysisEngine.analyze(transcript, expected)`; `data/ai/local/audio/PronunciationAnalysisEngine.kt:119`). **ASR word match, not phonemes** |
  | Pronunciation (free speech) | 428-453 | Loudness, pitch variation, continuity, noise and clipping. **Not pronunciation at all**: it scores signal quality and prosody but is labelled "Pronunciation & Clarity" |
  | Delivery | 474-487 | Loudness band + pitch spread − noise − clipping |
  | Grammar | 533-543 | Starts from a **fixed baseline of 78**, + length bonus (up to 16) − rule-issue penalties. Contradicts the file header's "Zero baseline … never returns 78/100" (lines 17-18); a clean 35-word answer gets 94 |
  | Vocabulary | 593-598 | Type-token diversity + target words − repetition |
  | Video presence | 631-638 | Face and posture detection rates |

- **Confidence values** are hard-coded constants (0.88, 0.72, 0.82, 0.52, 0.85 and so on; e.g. 384, 421, 445), not computed.
- **Trust assessment.** The relative signals are meaningful (silence, fillers, loudness, diversity). The absolute 0–100 numbers have arbitrary offsets, and two dimensions measure something other than their labels.

### 4.3 How much can a user trust the numbers? [I]
- **Activity Library:** very little. Scores rank essay length, the presence of a recording and typed filler words. Nearly any honest attempt passes, and the activity is then "completed" and counted towards paths and milestones.
- **Speaking Lab:** moderate for fluency, delivery and filler counts. Low for "Pronunciation" (text match or acoustic proxy) and for Grammar (baseline 78).
- **Rendering issue [I].** Partial evaluations may display as full after a reload. `SpeakingSession.isPartialEvaluation` is an `is`-prefixed Kotlin boolean (`data/model/FirestoreModels.kt:143`) written under the key `"isPartialEvaluation"` (`SpeakCraftRepository.kt:830`). Firestore's bean mapper exposes that getter as the property `partialEvaluation`, so the field should read back as `false`. The activity models avoid this pattern on purpose (`ActivityModels.kt:317`).

---

## 5. Progress, streaks and analytics

| Metric | Source / computation | Store | Where shown |
|---|---|---|---|
| Streak #1 `UserProfile.streakDays` | `StreakAndAnalyticsHelper.computeUpdatedStreak` in `saveDailyActivity` (`SpeakCraftRepository.kt:670-684`). Runs whenever a daily-routine step is toggled or a reflection saved | Firestore `users/{uid}` | Fallback on Dashboard; used in profile max; notifications |
| Streak #2 LPM `LearningStreakState` | `StreakAndCompletionEngine.recordDailyCompletion` when the dashboard plan meets `isCompleteForStreak` (`LearningProgressManager.kt:364-368`); milestones at 3/7/14/30/60/100 (`StreakAndCompletionEngine.kt:21`) | Local JSON only | Dashboard streak card (`DashboardStudioWidgets.kt:703-704`) |
| Streak #3 activity consistency | Days with any activity attempt, pass or fail (`ActivityProgressAnalyticsEngine.kt:34-40`) | Derived from Firestore `activityAttempts` (≤200 most recent) | Activity Library badge (`ActivityDiscoveryScreen.kt:165`) |
| Streak #4 | `max(#1, #2)` | — | Profile (`UserProfileScreen.kt:153-156`); NMR "Protect your N-day streak" (`ui/AppNavigation.kt:556-559`) |
| Practice minutes | `UserProfile.totalPracticeMinutes`: 3 min per routine step plus session duration (`MainCoachViewModel.kt:275`, `SpeakCraftRepository.kt:686-688,880`). LPM `totalPracticeMinutes`: planned task minutes on a tap (`LearningProgressManager.kt:373`). Activity analytics: the activity timer (`ActivityProgressAnalyticsEngine.kt:18-19`) | Firestore / local / Firestore | Dashboard and Profile show `max(profile, LPM)` (`DashboardScreen.kt:194-197`, `UserProfileScreen.kt:149-152`); Activity Library shows its own figure |
| Words learned | `max(vocab entries, profile.wordsLearnedCount, LPM mastered (always 0))` (`DashboardScreen.kt:200-204`) | Firestore | Dashboard / Profile |
| `completedExercisesCount` | Incremented on **every** daily-step toggle, including un-ticking (`SpeakCraftRepository.kt:683`), and on every speaking session (879) | Firestore | Analytics |
| Active days this week | LPM weekly map, else count of routine days **clamped to 5** (`DashboardScreen.kt:205-207`) | — | Dashboard |

**Bugs and risks:**
- **Stale Firestore streak [V].** `lastPracticeDate` is written even when `hasCompletedSteps = false` (`SpeakCraftRepository.kt:680`, condition at 673-678). Example: the streak is 10 and the last practice was 5 days ago. The user saves only a reflection today, so the streak stays at 10 while the date moves to today. Tomorrow's step then makes it 11. The streak is also never decayed for missed days until the user's next save, so the profile can show a dead streak indefinitely.
- **Gameable streaks [V].** One tap per dashboard task is enough (`DashboardStudioWidgets.kt:639-640`): two taps give a streak day. Ticking routine steps (#1) is equally unverified.
- **Undercounting [V].** Activity analytics use only the latest 200 attempts (`data/firebase/ActivityFirestoreRepository.kt:85`), so lifetime totals, averages and trends are understated for heavy users.
- **Cross-system blindness [V].** Activity completions do not feed the LPM streak or plan, and Speaking Lab sessions do not auto-complete dashboard tasks (`recordEvent` has no external callers).
- **Main-thread disk I/O [I].** LPM and NMR write JSON synchronously in `commitState`/`commitLocked` (`LearningProgressManager.kt:884-887`, `data/notifications/NotificationManagerRepository.kt:434-437`), and are called from a Compose `LaunchedEffect`, which runs on the Main dispatcher (`ui/AppNavigation.kt:546-560`).
- **Unbounded growth [V].** `processedEventKeys` is never trimmed (`LearningProgressManager.kt:280`), whereas `eventLog` is capped at 300.
- **Day boundaries.** `ActivityEngineViewModel.todayDateKey` is fixed when the ViewModel is created (`ActivityEngineViewModel.kt:100`) [V], so a session that crosses midnight is attributed to the wrong day [I].

---

## 6. Notifications
- **No OS notifications exist.** [V]
  - The manifest permissions are only INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO and CAMERA (`app/src/main/AndroidManifest.xml:5-8`).
  - A grep for `NotificationCompat|NotificationChannel|POST_NOTIFICATIONS|AlarmManager|WorkManager|FirebaseMessaging|BroadcastReceiver` returns nothing.
  - `app/build.gradle.kts:105-151` has no WorkManager or FCM dependency.
- **Store 1: `NotificationManagerRepository`** (in-app). It persists to `filesDir/notification_store/alerts_<uid>.json` (35-36, 439-443), with a cap of 150 alerts (473). It is the only store the bell drawer renders (`ui/AppNavigation.kt:1303-1316`). Triggers:
  1. A welcome alert on the first load (74-88).
  2. "Today's N-min speaking plan is ready", once per day, generated when the app opens (90-106).
  3. "Protect your N-day streak" when the streak is at least 2 (108-122).
  4. A session-completed alert when a new COMPLETED speaking session appears (`ui/AppNavigation.kt:561-583` → 250-273).
  5. Admin announcements (232-245).
  6. A bridge from LPM notifications (182-227).

  All triggers run only while the app is open; nothing is scheduled. `UpdateSystemIsolationGuard` filters update-related text and routes (`data/notifications/NotificationModels.kt:113-164`).
- **Store 2: LPM `UserNotification`** (`LearningProgressManager.kt:131-164, 288-342, 848-870`). Generates "plan ready", "vocab due" (never fires, §3.4), "keep your streak alive", goal-met, milestone, lesson-completed (dead path), quiz (dead path), achievements and announcements. Its own UI (`NotificationsCenterSheet`) is **dead**, and its preferences cannot be changed because `updateNotificationPreferences` has no caller.
- **Duplicate alerts [V].** Both stores create a daily "plan ready" alert with different dedup keys (`daily_plan_ready_<date>` at `LearningProgressManager.kt:138` and `daily_plan_<date>` at `NotificationManagerRepository.kt:102`), and both create a streak alert (`streak_reminder_` and `streak_protect_`). After the sync, the user sees two of each per day, possibly with different streak numbers.
- **Dismissed alerts come back [I].** The sync deduplicates only against alerts currently stored (`NotificationManagerRepository.kt:186,194`). After the user dismisses or clears a bridged alert, the next change to `learningSnapshot.notifications` re-adds every LPM notification.
- **Unused fields [V].** `NotificationPreferences.preferredReminderTime` (`LearningModels.kt:191`) and `preferredPracticeTime` are never read.

---

## 7. Preferences

### 7.1 `AppPreferencesRepository`, DataStore `speakcraft_prefs` (`data/preferences/AppPreferencesRepository.kt:22`) [V]

| Key (line) | Type | Default / validation |
|---|---|---|
| `theme_mode` (107) | string | SYSTEM (SYSTEM/LIGHT/DARK) |
| `tts_rate` (108) | float | 1.0, clamped 0.6–1.4 on write (177) |
| `youtube_api_key` (109) | string | "" (AES-GCM AndroidKeyStore, `enc:` prefix; plaintext legacy read is allowed (92)) |
| `update_channel` (110) | string | `BuildConfig.BUILD_CHANNEL` or "stable" (stable/beta) |
| `last_update_check_ms` (111) | long | 0 |
| `dismissed_release_version` (112) | string | "" |
| `last_verified_version_code` (113) | long | 0 |
| `completed_tutorial_keys` (114) | string set | ∅ (legacy; superseded by TutorialStateManager) |
| `first_use_tutorials_enabled` (115) | bool | true (legacy) |
| `initial_launch_completed` (116) | bool | false (launch animation, `ui/AppNavigation.kt:233-239`) |
| `github_release_repo` (117) | string | `BuildConfig.GITHUB_RELEASE_REPO` |
| `font_scale_preference` (118) | float | 1.0, clamped 0.85–1.3 |
| `reduced_motion` (119) | bool | false |
| `cloud_ai_enabled` (120) | bool | false (forced false under LOCAL_ONLY) |
| `cloud_ai_model_tier` (121) | string | "balanced" (balanced/deep/fast) |
| `voice_auto_speak_enabled` (122) | bool | true |
| `ai_local_enabled` (123) | bool | true |
| `ai_live_transcription_enabled` (124) | bool | false |
| `ai_local_speech_recognition_enabled` (125) | bool | true |
| `ai_local_audio_analysis_enabled` (126) | bool | true |
| `ai_local_video_analysis_enabled` (127) | bool | true |
| `ai_recognition_language` (128) | string | "en-US" |
| `ai_recognition_engine` (129) | string | "AUTO" (AUTO/ANDROID_OFFLINE/LOCAL_WHISPER) |

### 7.2 `AISettingsRepository` (`data/preferences/AISettingsRepository.kt`) [V]
- It is a facade over the same DataStore, defined by the `AISettings` data class (21-37).
- **Defaults:** local AI on, cloud off, tier "balanced", live transcription off, local speech recognition, audio analysis and video analysis on, "en-US", AUTO.
- `cloudAiSupportedInBuild = !LocalExecutionPolicy.isLocalOnly()`, which is false, so `effectiveCloudAiEnabled` is always false (33-34).
- `setCloudAiEnabled(true)` persists false and returns a failure (74-89).
- `audioVideoUploadedToCloud = false` is a constant (36).

### 7.3 `TutorialState` / `TutorialStateManager` (`data/preferences/TutorialState.kt`) [V]
- Separate DataStore `speakcraft_tutorial_state` (11).
- Per-user keys `completed_tutorials_<uid>` (string set) and `first_use_tutorials_enabled_<uid>` (bool, default true) (28, 31).
- `shouldShowTutorial = enabled && key not completed` (21-22).
- Operations: mark one, mark many, toggle, reset (48-82).

### 7.4 Settings that live elsewhere
- **LPM `PersistedLearningSnapshot` settings** (local JSON): currentLevel, targetLevel (unused), selectedGoals, dailyTargetMinutes (15), preferredPracticeTime (unused), interests (unused) and notificationPreferences (`LearningModels.kt:377-392`).
- **NMR `AlertPreferences`:** 7 booleans, all true by default (`NotificationModels.kt:57-75`).
- **UserProfile in Firestore:** level, goal, dailyGoalMinutes.
- **Clamp ranges disagree** for daily goal minutes: 5–240 in the repository (`SpeakCraftRepository.kt:493`), 10–90 in LPM (`LearningProgressManager.kt:85`), 5–90 in NMR (`NotificationManagerRepository.kt:72`), and snapped to 10/20/30/45/60 for the activity plan. [V]

---

## 8. Persistence map

| Data | Where stored | Source of truth | Sync / conflicts |
|---|---|---|---|
| Profile (name, level, goal, minutes, skill scores, streak, counters) | Firestore `users/{uid}` (`SpeakCraftRepository.kt:469-530`) | Firestore | Firestore offline cache only. Read-modify-write without transactions (e.g. 668-688) [V] |
| Level / goals for the Dashboard plan | LPM local JSON (`currentLevel`, `selectedGoals`) | Level is re-derived from the profile on every load; goals are stuck at the defaults (§3.1) | Not synced. Diverges from the profile goal [V] |
| Activity progress / attempts / saved / plan | Firestore `activityProgress`, `activityAttempts`, `savedActivities`, `dailyPracticePlans` (`ActivityFirestoreRepository.kt:63-125`) plus an in-memory ViewModel shadow (`ActivityEngineViewModel.kt:103-106`) | Firestore; local shadow wins when remote is empty | **[I]** Firestore `set/update().await()` completes only when the server acknowledges. Offline, `submitActiveResponse` stays at "Evaluating" until reconnect (`ActivityEngineViewModel.kt:408-426`), so the "Saved locally" banner shows only on errors. `isUpdate` uses `update()`, which fails if the document is missing (`ActivityFirestoreRepository.kt:151`) |
| Speaking sessions, feedback, conversations | Firestore `speakingSessions`, `speechFeedback`, `conversationSessions` | Firestore | — |
| Vocabulary (UI) | Firestore `vocabulary/{wordId}` (`SpeakCraftRepository.kt:729-780`) | Firestore | — |
| Vocabulary (LPM 5-stage / SRS / notebook / quiz) | LPM local JSON | n/a (never written) | DEAD |
| Daily routine | Firestore `dailyActivities/{date}` | Firestore | — |
| Lessons / programs | Firestore `learningProgress`, `enrollments` | Firestore | — |
| Dashboard plan, streak, achievements, points, minutes, LPM notifications | `filesDir/learning_state/learning_snapshot_<uid>.json` (`LearningProgressManager.kt:49-51, 889-919`) | Local file | **Never synced.** `mergeSnapshots` (713-784) is unused, and it would not merge SRS, quiz history, notifications or the plan anyway. Lost on reinstall or device change, even though the tutorial copy promises streaks "across reinstalls or devices" (`data/model/FeatureTutorialRegistry.kt:28`) [V] |
| In-app alerts | `filesDir/notification_store/alerts_<uid>.json` | Local file | Not synced |
| Recordings | `filesDir/recordings/*.m4a|*.mp4` (`data/media/AudioRecorderManager.kt:57-62`, `data/media/CameraVideoRecorder.kt:201-202`). **Absolute device paths** stored in Firestore (`SpeakingSession.localMediaPath`, `ActivityAttemptRecord.recordedMediaPath` at `ActivityFirestoreRepository.kt:201`) | Device | References dangle on another device. Account deletion removes files only for documents with a `localMediaPath` field (`SpeakCraftRepository.kt:1307`), **not** activity recordings (`recordedMediaPath`) [V] |
| Settings | DataStore `speakcraft_prefs`, `speakcraft_tutorial_state` | Local | Not synced |
| Account deletion | Deletes the Firestore subcollections listed at `SpeakCraftRepository.kt:1295-1299` | — | Does **not** delete the LPM JSON or NMR JSON files; `wipeAllLocalDataForUser` has no caller [V] |

---

## 9. Dead code, hard-coded values, mock data, TODO

**Dead (0 production callers, checked by grep):**
- Whole classes:
  - `LocalVocabularyQuizEngine`;
  - `SpacedRepetitionScheduler`, except `dueItems` inside LPM, which always returns an empty result;
  - `NotificationsCenterSheet`;
  - Firestore `LearningModule` and `CommunicationExercise`.
- `LearningProgressManager` functions:
  - `updateLearningPreferences`;
  - `recordVocabularyStageProgress`, `recordFlashcardReview`, `recordQuizSession`;
  - `upsertNotebookItem`, `toggleNotebookFavorite`, `deleteNotebookItem`;
  - `unreadNotificationCount`, `markNotificationRead`, `markAllNotificationsRead`, `deleteNotification`, `updateNotificationPreferences`;
  - `addAccountSecurityNotice`;
  - `exportUserLearningDataJson`, `clearSelectedLearningData`, `wipeAllLocalDataForUser`, `mergeSnapshots`.
  - `recordEvent` is only called from these dead functions.
- `NotificationManagerRepository.clearAllAlerts` and `postAlert`: `postAlert` has no external caller.
- Model fields that are never read:
  - `CompletionCriteria.minDurationSeconds`, `minWordsOrTurns`, `requiresRecording`;
  - `FeedbackConfiguration` (all fields);
  - `EvaluationConfiguration.aiEnabled`, `focusCriteria`;
  - `ActivityModuleDefinition.targetActivityCount`;
  - `LearningPathMilestone.rewardSummary`;
  - `LearningPathProgressSummary.prerequisitesMet`;
  - `PersistedLearningSnapshot.targetLevel`, `preferredPracticeTime`, `interests`;
  - `NotificationPreferences.preferredReminderTime`.
- Unreachable or unused code paths:
  - the cloud evaluation path in `FirebaseAiLogicService.evaluateUniversalActivity` (714-845), unreachable under LOCAL_ONLY;
  - the `AppPreferences` legacy tutorial keys.
- Unused imports in `UniversalEvaluationEngine.kt:3-8`: `Log`, `TAG`, `CancellationException`, `TimeoutCancellationException`, `withTimeout`, `JSONArray`, `JSONObject`.

**Hard-coded and baseline values [V]:**
- Default profile skill scores 62/65/60/68/58/66 and confidence 55 (`FirestoreModels.kt:31-37`). These feed the plans' "weakest skill" until real scores arrive.
- Onboarding level presets (`AuthAndOnboardingScreens.kt:363-379`).
- Activity base score 56 and clamp 30–96 (`UniversalEvaluationEngine.kt:274-279`).
- Grammar baseline 78 (`EvidenceBasedScoringEngine.kt:543`).
- Cloud fallback 74 and floor 20 (`FirebaseAiLogicService.kt:783`).
- Hard-coded confidence constants in scoring.
- 3 minutes per routine step (`MainCoachViewModel.kt:275`).
- Points table (`LearningProgressManager.kt:237-249`).
- "activeDaysThisWeek … coerceIn(0, 5)" (`DashboardScreen.kt:207`).
- Milestone text "Full 150-Activity Catalogue Master" (`ActivityProgressAnalyticsEngine.kt:98`).

**Mock or seeded data [V]:**
- Onboarding seeds 15 @askvinh techniques into the user's Firestore `extractedTechniques` and auto-enrols the user in the 30-day program (`MainCoachViewModel.kt:229-230`).
- Three "PENDING_TRANSCRIPT" technique seeds whose instructional text is placeholder wording asking for verification (`CurriculumCatalog.kt:454-518`); they are excluded from lessons.

**Markers:**
- No TODO, FIXME or HACK markers in scope.
- A stray reasoning comment remains: `// … Wait: if audio fails when audioRequired -> INVALID immediately.` (`EvaluationEvidenceValidator.kt:240`). It points to unreviewed, generated code.

---

## 10. Test coverage of this layer

| Test file | Tests | What it meaningfully checks | Gaps |
|---|---|---|---|
| `test/ActivityEngineUnitTest.kt` | 9 | Catalogue counts and validity, 25 templates used, unique IDs (39-97). Validator rejects bad input (98-118). Search, filter and pagination (120-140). Confidence kept separate from the objective score (143-178). Daily plan has no duplicates and avoids recent activities (213-252). Paths reference valid IDs and milestones (255-276). Analytics deltas and streak (279-358) | Score assertions are only `in 1..100` (204). Nothing pins down the formula, the zero-word audio pass, the 3-word pass, or the cosmetic dimension spread. No test that completed activities survive plan regeneration. Prerequisites not tested |
| `test/CurriculumAndRecommendationTest.kt` | 4 | 15 content modules; pending seeds not creator-derived (31-46). RecommendationEngine routing (48-92) | Asserts only that `sourceVideoUrl` starts with `https://www.youtube.com/` (43), so channel-root URLs pass as "verified" |
| `test/StreakAnalyticsAndBackupTest.kt` | 7 | `StreakAndAnalyticsHelper` arithmetic (27-69), minutes delta, period filter, tutorial registry and TutorialStateManager isolation | Does not test `saveDailyActivity`'s `lastPracticeDate` behaviour (the stale-streak bug) |
| `test/NotificationManagementSystemTest.kt` | 4 | NMR persistence, dedup, pinning, update-payload blocking, preferences, drawer rendering | No test of `syncFromLearningNotifications`: duplicates and dismissed alerts reappearing are untested |
| `test/CoachEvaluationEvidenceTest.kt` | 15 | Thorough on the validator and `EvidenceBasedScoringEngine` fail-closed rules: missing, empty, short, silent or corrupt audio; partial evaluation; stale transcript; inference failure (92-445) | Does not check that dimension values are meaningful (e.g. the grammar baseline, or pronunciation measured from acoustics) |
| `test/CentralizedAiSettingsTest.kt` | 10 | AISettings persistence, LOCAL_ONLY lock (91-103), capability statuses, settings UI composition | — |

**Completely untested (grep finds 0 references in `test/`):**
- `LearningProgressManager`, `DailyPracticePlanEngine`, `StreakAndCompletionEngine`, `SpacedRepetitionScheduler`, `LocalVocabularyQuizEngine`;
- `ActivityFirestoreRepository`;
- `mergeSnapshots`;
- `MainCoachViewModel.reviewVocabularyCard`.

The domain layer has no androidTest coverage either: only `app/src/androidTest/java` exists.

---

## 11. Feature status classification

| Capability | Status | Key evidence |
|---|---|---|
| 150-activity catalogue (browse, search, filter) | DOCUMENTED_AND_IMPLEMENTED | `ActivityCatalogue.kt:105-258`; tests |
| Activity submission and evaluation | PARTIALLY_IMPLEMENTED (works, but scores are not trustworthy; audio never transcribed; video not analysed) | `UniversalEvaluationEngine.kt:200-201,258-282`; `ActivityEngineViewModel.kt:354-365` |
| Cloud (Gemini) activity evaluation | DEAD (policy-disabled) | `LocalExecutionPolicy.kt:13`; `HybridAiCoachCoordinator.kt:74-78` |
| 9 evaluator strategies | PLACEHOLDER (caveat or prompt text only; no local behaviour differs, except the checklist gate) | `UniversalEvaluationEngine.kt:19-101` |
| Activity rubric weights and dimension scores | PLACEHOLDER (fabricated spread) | `UniversalEvaluationEngine.kt:281-282` |
| Speaking Lab evidence validation (fail-closed) | DOCUMENTED_AND_IMPLEMENTED | `EvaluationEvidenceValidator.kt`; 15 tests |
| Speaking Lab dimension scoring | PARTIALLY_IMPLEMENTED (pronunciation is a proxy; grammar has a baseline) | `EvidenceBasedScoringEngine.kt:428-453,543` |
| Activity daily plan (10–60 min) | PARTIALLY_IMPLEMENTED / BROKEN (completion state lost on regeneration; minutes not persisted) | `DailyPracticeEngine.kt:241-242`; `ActivityDiscoveryScreen.kt:112-114` |
| Dashboard personalised daily plan | PARTIALLY_IMPLEMENTED (goal ignored; completion only by self-report) | `LearningProgressManager.kt:86-88`; `DashboardStudioWidgets.kt:639-640` |
| Daily 8-step routine | PARTIALLY_IMPLEMENTED (self-report; fabricated minutes; "9-step" label) | `MainCoachViewModel.kt:275`; `CurriculumCatalog.kt:170,989-998` |
| Daily plans overall | DUPLICATED (×3) | §1.5 |
| Learning paths (10, activity-based) | PARTIALLY_IMPLEMENTED (no prerequisite or order enforcement) | `LearningPathsEngine.kt:313-317` |
| Structured programs (30/60/90-day, etc.) | PARTIALLY_IMPLEMENTED (manual day toggles; only the 30-day program has day-level content) | `CurriculumCatalog.kt:1000-1123`; `SpeakCraftRepository.kt:619-633` |
| Level determination | PLACEHOLDER (self-selected; no placement or progression) | `AuthAndOnboardingScreens.kt:363-379,450` |
| Spaced repetition (SM-2) | DEAD | §3.4 |
| Flashcards in the UI (ad hoc SRS) | PARTIALLY_IMPLEMENTED | `MainCoachViewModel.kt:344-374` |
| 5-stage vocabulary builder | DEAD (promised by plan text) | `LearningProgressManager.kt:394-491`; `DailyPracticePlanEngine.kt:78` |
| Local vocabulary quiz (9 types) | DEAD | `LocalVocabularyQuizEngine.kt` (0 callers) |
| Vocabulary usage check ("AI Quiz" tab) | DOCUMENTED_AND_IMPLEMENTED (local) | `MainCoachViewModel.kt:407-440` |
| Vocabulary notebook | Firestore-backed: IMPLEMENTED. LPM notebook: DEAD | `VocabularyScreen.kt:401-442` |
| Streaks | DUPLICATED (×4) and BROKEN (stale profile streak; gameable) | §5 |
| Achievements and milestones | DUPLICATED (activity milestones vs LPM badges), PARTIALLY_IMPLEMENTED | `ActivityProgressAnalyticsEngine.kt:92-104`; `LearningProgressManager.kt:808-824` |
| Activity analytics snapshot | PARTIALLY_IMPLEMENTED (200-attempt window) | `ActivityFirestoreRepository.kt:85` |
| OS push and local notifications / reminders | PLACEHOLDER / not implemented | §6 |
| In-app notification drawer | DOCUMENTED_AND_IMPLEMENTED, with DUPLICATED alerts | `NotificationManagerRepository.kt` |
| LPM notification centre | DEAD | `NotificationsCenterSheet.kt` (0 callers) |
| Learning-state cloud sync / conflict merge | DEAD (rules exist; no code) | `LearningProgressManager.kt:713`; `firestore.rules:704-756` |
| Learning-data export / selective clear | DEAD | `LearningProgressManager.kt:670-695` |
| AI settings centralisation | DOCUMENTED_AND_IMPLEMENTED | `AISettingsRepository.kt`; tests |
| Per-user tutorial state | DOCUMENTED_AND_IMPLEMENTED (legacy global keys DEAD) | `TutorialState.kt` |
| Recording references across devices | BROKEN by design (local absolute paths stored in the cloud) | `ActivityFirestoreRepository.kt:201` |
| Content provenance labelling ("VERIFIED" @askvinh) | UNKNOWN / at risk (unverifiable timestamps; channel-root URLs) | `CurriculumCatalog.kt:204-449` |
