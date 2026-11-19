# SpeakCraft Universal Activity Engine — Implementation Roadmap

## Completed Implementation Phases

### Phase 1 — Core Schema, Validation & 150-Activity Catalogue (Completed)
- Standardized 20-property `ActivityDefinition` and 25 `ExerciseTemplateType` enums in `ActivityModels.kt`.
- Authored all **150 core communication activities** across **10 curriculum modules** (`ActivityCatalogueModules1To3.kt`, `ActivityCatalogueModules4To6.kt`, `ActivityCatalogueModules7To10.kt`) covering 100% of all 25 exercise templates.
- Built `ActivityValidator` and `ActivityCatalogue` with multi-criteria search, filtering, and lazy pagination.

### Phase 2 — Universal AI Evaluation, Daily Practice, Learning Paths & Analytics Engines (Completed)
- Implemented `UniversalEvaluationEngine.kt` with all **9 named AI evaluators** backed by `Firebase.ai` (`gemini-3.8-flash`), strict `Schema.obj`, 60s timeout, honest caveats, local WPM/filler analyzer, and deterministic fallback.
- Implemented `DailyPracticeEngine.kt` supporting 10/20/30/45/60-min routines, weakest-skill prioritization, anti-repetition scoring, and per-block swapping.
- Implemented `LearningPathsEngine.kt` with all **10 guided paths**, prerequisites, milestones, and non-linear access.
- Implemented `ActivityProgressAnalyticsEngine.kt` with real historical earliest-vs-latest deltas, streaks, 10-module progress, and strict objective vs. confidence separation.
- Implemented `ActivityEngine.kt` orchestration layer.

### Phase 3 — Cloud Firestore Persistence & Zero-Trust Security Rules (Completed & Deployed)
- Implemented `ActivityFirestoreRepository.kt` and wired all dependencies in `AppContainer.kt`.
- Updated `firebase-blueprint.json`, `firestore.rules`, and `firestore.test.js` with full schema bounds and cross-user isolation tests.
- Passed all emulator security rules tests and deployed rules to Firebase.

### Phase 4 — Single Universal Compose Renderer & 5-Tab Discovery Hub (Completed)
- Built `ActivityEngineViewModel.kt`, `ExerciseTemplateRenderers.kt` (7 reusable interaction families for all 25 templates), `UniversalActivityScreen.kt` (`ActivityScreen(activityId)`), and `ActivityDiscoveryScreen.kt` (`All 150`, `Daily Plan`, `10 Paths`, `Saved & Recent`, `Analytics`).
- Wired routes (`activity_library`, `activity_library/{moduleId}`, `activity/{activityId}`), sidebar entry, and dashboard tile in `AppNavigation.kt` and `DashboardScreen.kt`.

### Phase 5 — Automated Verification & Technical Documentation (Completed)
- Added `ActivityEngineUnitTest.kt` and `ActivityEngineComposeTest.kt` alongside all 56 existing tests.
- Authored all 10 architecture, curriculum, template, AI, daily practice, paths, analytics, database, roadmap, and validation docs under `/docs/`.

## Future Post-Launch Enhancements
1. **Remote Curriculum Content Sync**: Optionally serve versioned `contentVersion >= 2` activity JSON packs from Firestore/CDN to add activities beyond 150 without an APK update.
2. **Real-Time Audio Pitch & Pause Visualization**: Augment `AudioRecorderManager` with pitch-contour graphs for Module 3 (`Voice Mastery`) drills.
3. **Spaced-Repetition Review Queue**: Auto-resurface activities where historical objective score was `< 70` after 3, 7, and 14 days.
4. **Team / Coach Shared Playlists**: Allow authenticated admins/coaches to assign custom activity bundles to cohort paths.
