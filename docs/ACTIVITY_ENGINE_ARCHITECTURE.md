# SpeakCraft Universal Communication Activity Engine — Architecture

## 1. Architectural Vision
SpeakCraft's Universal Communication Activity Engine powers **150 structured communication and English-learning activities** across **10 curriculum modules** using a **single configuration-driven execution pipeline** instead of 150 separate hardcoded screens.

```
ActivityCatalogue (10 Modules, 150 Validated Configs)
        │
        ▼
ActivityValidator (Pre-render schema & rubric guard)
        │
        ▼
ActivityEngine (Load → Validate Submission → Evaluate → Recommend Next)
        │
        ▼
Universal ActivityScreen(activityId) + ExerciseTemplateInteractionPanel (25 Templates / 7 Families)
        │
        ▼
UniversalEvaluationEngine (9 Firebase AI Logic Gemini 3.8 Flash Evaluators + Deterministic Fallback)
        │
        ▼
Structured 6-Section Feedback Engine + Separate Confidence Self-Rating
        │
        ▼
ActivityFirestoreRepository (4 User-Scoped Subcollections) → ActivityProgressAnalyticsEngine
        │
        ▼
DailyPracticeEngine (10/20/30/45/60m) & LearningPathsEngine (10 Guided Paths)
```

## 2. Core Layer Separation
| Layer | Implementation File(s) | Responsibility |
|---|---|---|
| Domain Models & Config Schema | `app/src/main/java/com/example/data/activity/ActivityModels.kt` | Standardized `ActivityDefinition` (20 properties), 25 `ExerciseTemplateType`s, `ActivityUserResponse`, `ActivityEvaluationResult`, Firestore DTOs |
| Activity Catalogue | `ActivityCatalogue.kt`, `ActivityCatalogueModules1To3.kt`, `ActivityCatalogueModules4To6.kt`, `ActivityCatalogueModules7To10.kt` | 10 curriculum modules, 150 validated core activities, search/filter/pagination |
| Pre-Render Validation | `ActivityValidator` (`ActivityCatalogue.kt`) | Ensures non-blank fields, valid regex IDs, rubric weights summing to 100%, valid `nextActivityIds`, and safe crash-free recovery |
| Orchestration Engine | `ActivityEngine.kt` | Loads & validates activities, validates user submissions, coordinates evaluation, updates progress/attempt DTOs, computes personalized next-activity recommendations |
| AI Evaluation Engine | `UniversalEvaluationEngine.kt` | Dispatches to 9 named strategy evaluators (`Firebase.ai` + `gemini-3.8-flash` + `Schema.obj`), 60s timeout, `CancellationException` propagation, honest caveats, deterministic offline rubric fallback |
| Daily Practice Engine | `DailyPracticeEngine.kt` | Generates personalized 10/20/30/45/60-min routines avoiding recent repeats and supporting per-block customization (`swapBlockInPlan`) |
| Learning Paths Engine | `LearningPathsEngine.kt` | Defines 10 ordered paths with prerequisites, milestones, completion criteria, and non-linear jump support |
| Progress & Analytics Engine | `ActivityProgressAnalyticsEngine.kt` | Computes real aggregate stats, 7-day consistency, streaks, 10-module completion, earliest-vs-latest skill deltas, and keeps confidence self-ratings strictly separate from objective scores |
| Cloud Persistence | `app/src/main/java/com/example/data/firebase/ActivityFirestoreRepository.kt` | Real-time `snapshots()` streams (`ServerTimestampBehavior.ESTIMATE`) and crash-free `Result<T>` writes across 4 user-scoped subcollections |
| ViewModel | `app/src/main/java/com/example/ui/viewmodel/ActivityEngineViewModel.kt` | Account-scoped state orchestration (`key = "activity_engine_$userId"`), live timer, media recording hooks, dialogue turns, local shadow state |
| Universal Renderer UI | `UniversalActivityScreen.kt`, `ExerciseTemplateRenderers.kt`, `ActivityDiscoveryScreen.kt` | Single `ActivityScreen(activityId)` composable, 7 reusable interaction family panels covering all 25 templates, 5-tab discovery hub |

## 3. Standardized `ActivityDefinition` Contract (20 Properties)
Every activity in SpeakCraft defines:
1. `activityId` — Unique stable slug (`^[a-zA-Z0-9_\-]+$`)
2. `title` — Concise learner-facing title
3. `description` — Actionable summary
4. `moduleId` — Parent module slug (`mod_01_*` .. `mod_10_*`)
5. `skillCategory` — Typed `ActivitySkillCategory`
6. `exerciseType` — One of 25 `ExerciseTemplateType` values
7. `difficulty` — `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
8. `duration` — Target duration in minutes (`1..120`)
9. `learningObjective` — Measurable skill outcome
10. `instructions` — Step-by-step practice sequence
11. `examples` — Concrete model answers / phrasing
12. `requiredPermissions` — Least-privilege hardware list (`NONE`, `MICROPHONE`, `CAMERA`)
13. `interactionConfiguration` — Prompt, placeholder, audio/video/TTS/checklist/dialogue config
14. `evaluationConfiguration` — Strategy enum, AI toggle, focus criteria, honesty caveat
15. `scoringRubric` — Weighted `ScoringRubricDimension` list summing strictly to `100`
16. `completionCriteria` — Duration, minimum words/turns, checklist/recording rules, passing threshold
17. `feedbackConfiguration` — 6 structured feedback section flags & coach tone
18. `nextActivityIds` — Validated progression pointers
19. `sourceAttribution` — Authorship / curriculum provenance
20. `contentVersion` — Monotonic integer (`>= 1`)

## 4. Navigation & Legacy Preservation
- Wired in `AppNavigation.kt` via routes `activity_library`, `activity_library/{moduleId}`, and `activity/{activityId}`.
- Entry points added to the Indigo Studio Sidebar (`150 Activities`) and `DashboardScreen` (`Activity Engine` tool tile).
- All pre-existing SpeakCraft modules, labs, YouTube Intelligence, AI Mentor, OTA Updater, themes (Violet Studio Light / Midnight Indigo Dark), and Firestore user collections remain 100% preserved and compatible.
