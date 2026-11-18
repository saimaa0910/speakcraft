# 04 — Issues, Bugs, and Technical Debt Audit

**Revision Date:** 2026-11-18  
**Product:** SpeakCraft — AI Communication Skills Coach  
**Audit Method:** Static read-only inspection of source files, Gradle config, AndroidManifest, Firestore security rules, and test suite.  
**Verification Notice:** Existing unit/Robolectric tests (`17/17` verified prior to audit) pass on mocked/local paths; issues below were uncovered via line-by-line architectural and behavioral inspection.

---

## Table of Contents

1. [Audit Summary & Classification Key](#audit-summary--classification-key)
2. [Issue Register by Category (Items 1–20)](#issue-register-by-category)
   - [Category A: Broken Functionality & Confirmed Logic Bugs (`ISS-001`..`ISS-003`, `ISS-020`)](#category-a-broken-functionality--confirmed-logic-bugs)
   - [Category B: Architecture, Cloud Storage & Platform Gaps (`ISS-004`, `ISS-023`, `ISS-024`)](#category-b-architecture-cloud-storage--platform-gaps)
   - [Category C: AI & External Integration Risks (`ISS-005`..`ISS-007`)](#category-c-ai--external-integration-risks)
   - [Category D: Security, Privacy & Firestore Rules (`ISS-008`, `ISS-009`, `ISS-026`)](#category-d-security-privacy--firestore-rules)
   - [Category E: Performance, Memory & Query Scaling (`ISS-010`, `ISS-011`)](#category-e-performance-memory--query-scaling)
   - [Category F: Build, Dependencies, i18n & Theme Debt (`ISS-012`..`ISS-015`)](#category-f-build-dependencies-i18n--theme-debt)
   - [Category G: Incomplete Features, UX & Navigation (`ISS-016`..`ISS-019`, `ISS-021`, `ISS-022`)](#category-g-incomplete-features-ux--navigation)
   - [Category H: Testing, Telemetry & DevOps Gaps (`ISS-025`, `ISS-027`, `ISS-028`)](#category-h-testing-telemetry--devops-gaps)
3. [Prioritized Bug-Fixing & Tech-Debt Roadmap](#prioritized-bug-fixing--tech-debt-roadmap)
4. [Cross-References & Open Questions](#cross-references--open-questions)

---

## Audit Summary & Classification Key

* **Confirmed Bug:** Verified flaw in existing executed code paths where runtime behavior contradicts specifications or fails silently.
* **Incomplete Feature:** UI/data hooks exist, but essential execution logic is missing.
* **Security / Integrity Risk:** Vulnerability or missing server-side enforcement.
* **Technical Debt:** Architectural coupling, unused libraries, hardcoded strings/colors, or missing indexes.
* **Platform Gap / Improvement:** Missing target capability (e.g., Electron + React Windows/Web support, REST API, SQL DB).

| Severity | Count | Issue IDs |
| :--- | :---: | :--- |
| **Critical** | 1 | `ISS-001` |
| **High** | 7 | `ISS-002`, `ISS-003`, `ISS-004`, `ISS-005`, `ISS-006`, `ISS-007`, `ISS-024` |
| **Medium** | 16 | `ISS-008`..`ISS-021`, `ISS-025`, `ISS-026` |
| **Low** | 4 | `ISS-022`, `ISS-023`, `ISS-027`, `ISS-028` |

---

## Issue Register by Category

### Category A: Broken Functionality & Confirmed Logic Bugs

#### `ISS-001` — 3-Stage Self-Audit "Mute Pass" Video Playback Has No Video Surface
* **Issue ID:** `ISS-001`
* **Title:** Stage 1 Visual/Posture Audit plays audio-only `MediaPlayer` without rendering video frames
* **Severity:** Critical
* **Category:** Broken Functionality / UI-UX / Media Processing
* **Affected Component:** `SpeakingLabScreen.kt` (lines 256–291), `SpeakingAndLabsViewModel.kt` (lines 157–171), `AudioRecorderManager.kt`
* **File Path:** `/app/src/main/java/com/example/ui/screens/SpeakingAndChallengesScreens.kt`, `/app/src/main/java/com/example/data/media/AudioRecorderManager.kt`
* **Description:** `SpeakingLabScreen` advertises Vinh Giang's 3-Stage Self-Audit (`Stage 1: Mute Pass`, `Stage 2: Vocal (Eyes Closed)`). However, clicking `"Play Current"` routes both audio (`.m4a`) and video (`.mp4` from CameraX or Photo Picker) into `AudioRecorderManager.playFile(target, muted)`, which wraps Android `MediaPlayer` with no `SurfaceHolder`/`VideoView`/`ExoPlayer` UI surface. Selecting Stage 1 plays muted audio with zero video visuals.
* **Root Cause:** `AudioRecorderManager` was reused for video playback without adding a Media3/ExoPlayer composable video viewport.
* **Impact:** Users cannot visually review posture, eye contact, or gestures in-app, breaking a core `@askvinh` coaching pillar.
* **Steps to Reproduce:** Open `Record & Review` -> tap `Video Clip` (or `Attach Video from Photo Picker`) -> select `Stage 1: Mute Pass` -> tap `Play Current`.
* **Current Behavior:** Audio plays muted; no video frame appears on screen.
* **Expected Behavior:** Video renders inside an inline player viewport (muted in Stage 1, hidden/curtained in Stage 2, full AV in Stage 3).
* **Recommended Fix:** Integrate Jetpack Media3 `ExoPlayer` + `AndroidView(PlayerView)` inside `SpeakingLabScreen` when `mediaType.startsWith("video/")`.
* **Verification Plan:** Robolectric/device test verifying `PlayerView` mounts for `.mp4` URIs and `exoPlayer.volume == 0f` when Stage 1 is active.
* **Dependencies:** Media3 ExoPlayer (`androidx.media3:media3-exoplayer`, `media3-ui`).
* **Status:** Confirmed Bug (Open)

#### `ISS-002` — Streak Inflation Bug & Hardcoded `3` Minutes in `saveDailyActivity`
* **Issue ID:** `ISS-002`
* **Title:** `streakDays` increments unconditionally across arbitrary date gaps and ignores actual `minutesSpent`
* **Severity:** High
* **Category:** Confirmed Logic Bug / Progress Analytics
* **Affected Component:** `SpeakCraftRepository.saveDailyActivity`
* **File Path:** `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt` (lines 553–569)
* **Description:** Line 558 evaluates `if (prevLast != safeDate && cleanSteps.isNotEmpty()) prevStreak + 1L` without parsing `prevLast` (`yyyy-MM-dd`) to check if it equals yesterday. Line 565 hardcodes `"totalPracticeMinutes" to FieldValue.increment(3L)` regardless of the `minutesSpent` parameter (`5..600`) or prior increments on the same day.
* **Root Cause:** Missing date-difference check (`ChronoUnit.DAYS.between`) and hardcoded constant `3L`.
* **Impact:** Missing 14 days of practice still increments a user's streak; toggling 5 steps in one day adds `15` minutes (`5 * 3L`) instead of delta minutes.
* **Steps to Reproduce:** Call `saveDailyActivity` on `2026-11-01`, skip to `2026-11-18`, and complete one daily step.
* **Current Behavior:** `streakDays` increases by `+1` instead of resetting to `1`; `totalPracticeMinutes` adds `+3` per toggle.
* **Expected Behavior:** Streak increments only when `prevLast` is exactly `yesterday`; resets to `1` if older; adds delta `(newMinutesSpent - oldMinutesSpent)` only.
* **Recommended Fix:** Parse `prevLast` and `safeDate` with `SimpleDateFormat`/`LocalDate`, compute consecutive-day logic, and store `minutesDelta = (minutesSpent - oldMinutes).coerceAtLeast(0L)` inside a Firestore transaction (or server Cloud Function).
* **Verification Plan:** Unit test covering consecutive day, same day, and >1 day gap scenarios.
* **Dependencies:** None (or `ISS-008` server aggregation).
* **Status:** Confirmed Bug (Open)

#### `ISS-003` — Silent Firestore Write Failure Masked as `UiState.Success`
* **Issue ID:** `ISS-003`
* **Title:** `submitForAiAnalysis` emits `UiState.Success` even when `saveSpeakingSession` fails
* **Severity:** High
* **Category:** Confirmed Logic Bug / Error Handling
* **Affected Component:** `SpeakingAndLabsViewModel.submitForAiAnalysis`
* **File Path:** `/app/src/main/java/com/example/ui/viewmodel/SpeakingAndLabsViewModel.kt` (lines 212–218)
* **Description:** After Gemini returns a valid `SpeakingSession`, `repository.saveSpeakingSession(session)` is called. If `saveRes.isFailure` (e.g., network offline, permission error, rule constraint rejection), line 217 still emits `_analysisState.value = UiState.Success(session)` with empty `id` and no warning banner.
* **Root Cause:** Deliberate/accidental fallback ignoring `saveRes.exceptionOrNull()`.
* **Impact:** User sees full feedback report and believes the session was persisted to history/profile scores when Firestore rejected or failed the write.
* **Steps to Reproduce:** Trigger AI analysis while Firestore write fails rule check or lacks connectivity.
* **Current Behavior:** Feedback renders normally with no persistence warning; disappears after restart.
* **Expected Behavior:** Surface a non-blocking warning banner ("Analyzed locally, failed to sync to cloud — Retry Save") alongside the generated report.
* **Recommended Fix:** Add a `cloudSaved: Boolean` / `saveWarning: String?` wrapper or emit a Snackbar retry trigger when `saveRes.isFailure`.
* **Verification Plan:** Unit test with mocked failing `SpeakCraftRepository.saveSpeakingSession`.
* **Dependencies:** None.
* **Status:** Confirmed Bug (Open)

#### `ISS-020` — Paused Recording Elapsed Time Drift in `AudioRecorderManager`
* **Issue ID:** `ISS-020`
* **Title:** Pausing and immediately stopping audio recording skips duration accumulation when active segment is short
* **Severity:** Medium
* **Category:** Confirmed Bug / Audio Processing
* **Affected Component:** `AudioRecorderManager.kt`
* **File Path:** `/app/src/main/java/com/example/data/media/AudioRecorderManager.kt`
* **Description:** During recording, `elapsedSeconds` relies on a `1000ms` coroutine ticker (`if (!isPaused) ...`). If a user records `< 1s`, pauses, and hits Stop, or pauses/resumes rapidly across `1000ms` boundaries, elapsed duration skews relative to `MediaMetadataRetriever` actual `.m4a` duration.
* **Root Cause:** Timer relies on `delay(1000L)` ticks instead of summing monotonic `SystemClock.elapsedRealtime()` active intervals.
* **Impact:** Incorrect `durationSeconds` saved to Firestore and passed into Gemini WPM/brevity prompts.
* **Steps to Reproduce:** Start recording -> pause after `0.8s` -> stop.
* **Current Behavior:** Timer may remain `0s` and fall back to `.coerceAtLeast(1)`.
* **Expected Behavior:** Measure active milliseconds via `SystemClock.elapsedRealtime()` and reconcile final duration from the saved `.m4a` header.
* **Recommended Fix:** Track `activeDurationMs` across pause/resume transitions and read actual duration with `inspectMediaDurationSeconds` on stop.
* **Verification Plan:** Unit test for pause/resume stopwatch math.
* **Dependencies:** None.
* **Status:** Confirmed Bug (Open)

---

### Category B: Architecture, Cloud Storage & Platform Gaps

#### `ISS-004` — Device-Local Media Paths Stored in Cloud (`localMediaPath`)
* **Issue ID:** `ISS-004`
* **Title:** Audio/video recordings never upload to cloud storage; cross-device playback fails
* **Severity:** High
* **Category:** Architecture / Cloud Storage / Missing Feature
* **Affected Component:** `SpeakingSession.localMediaPath`, `SpeakingAndLabsViewModel.kt`, `SpeakCraftRepository.kt`
* **File Path:** `/app/src/main/java/com/example/data/model/FirestoreModels.kt`, `/app/src/main/java/com/example/ui/viewmodel/SpeakingAndLabsViewModel.kt` (lines 157–170)
* **Description:** Media files are saved exclusively to `context.filesDir/recordings/`. Firestore stores the local filesystem path (`/data/user/0/...`), so signing in on another Android device (or future Electron/Web client) yields `"Local media file is no longer present on this device."`
* **Root Cause:** Absence of Firebase Cloud Storage or S3/GCS media bucket integration.
* **Impact:** Recordings are lost permanently on uninstall/device switch; Web/Windows clients cannot play mobile recordings.
* **Steps to Reproduce:** Record a session on Device A -> log into the same Google account on Device B -> tap Play on past session.
* **Current Behavior:** Playback fails with local-file-missing note.
* **Expected Behavior:** Upload compressed `.m4a`/`.mp4` to object storage (`mediaObjectKey`) with signed URL playback and local disk cache.
* **Recommended Fix:** Provision Firebase Cloud Storage (or GCS/S3 via backend signed URLs), add `cloudMediaUrl`/`storagePath` to `SpeakingSession`, and stream remotely when local cache misses.
* **Verification Plan:** Integration test verifying upload, remote streaming, and delete cascade.
* **Dependencies:** Cloud Storage provisioning + security rules.
* **Status:** Incomplete Feature / Architecture Gap (Open)

#### `ISS-023` — Monolithic Repository & Overloaded `MainCoachViewModel`
* **Issue ID:** `ISS-023`
* **Title:** `SpeakCraftRepository` (1,118 LOC) and `MainCoachViewModel` violate single-responsibility modularity
* **Severity:** Low
* **Category:** Technical Debt / Code Organization
* **Affected Component:** `SpeakCraftRepository.kt`, `MainCoachViewModel.kt`
* **File Path:** `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt` (lines 1–1118)
* **Description:** One repository class combines user profile, daily tracking, vocabulary, speaking sessions, YouTube sources, shared catalog, release updates, and admin moderation mutations.
* **Root Cause:** Rapid feature expansion inside a single repository file.
* **Impact:** Harder unit testing, merge conflicts, and exceeding the 500-line modularity guideline.
* **Steps to Reproduce:** Inspect `SpeakCraftRepository.kt` (`1,118` lines).
* **Current Behavior:** Single class handles 22 Firestore paths.
* **Expected Behavior:** Split into `UserProgressRepository`, `SpeakingRepository`, `CatalogRepository`, `AdminReleaseRepository`.
* **Recommended Fix:** Refactor by domain aggregate (after audit phase, when implementation resumes).
* **Verification Plan:** Unit test suite per domain repository.
* **Dependencies:** None.
* **Status:** Technical Debt (Open)

#### `ISS-024` — Platform Discrepancy: Missing Electron + React (Windows Desktop & Web) Implementation
* **Issue ID:** `ISS-024`
* **Title:** Repository contains Android Kotlin/Compose only; requested Electron + React Desktop & Web targets do not exist
* **Severity:** High
* **Category:** Missing Platform / Architecture Alignment
* **Affected Component:** Entire Repository
* **File Path:** `/` (Repository root)
* **Description:** While product/audit documentation requests mention Electron + React on Windows Desktop and Web (with REST APIs and SQL schemas), 100% of implemented code is native Android Kotlin/Compose + serverless Firebase.
* **Root Cause:** App was initialized and built as an Android Studio / AI Studio Android project.
* **Impact:** Cannot ship Windows `.exe`/MSI or Web SPA from this codebase without building a multi-platform monorepo and backend API gateway.
* **Steps to Reproduce:** List root workspace files (`app/build.gradle.kts`, `settings.gradle.kts`).
* **Current Behavior:** Builds Android APK/AAB only.
* **Expected Behavior:** Clarify whether this repo stays Android-native or spawns companion Web/Electron + API repositories.
* **Recommended Fix:** See `02_BACKEND_ARCHITECTURE_DOCUMENTATION.md` and `07_DATABASE_DESIGN_AND_DOCUMENTATION.md` for target migration blueprints.
* **Verification Plan:** Architecture decision record (ADR) sign-off.
* **Dependencies:** Product roadmap decision.
* **Status:** Missing Implementation / Open Question

---

### Category C: AI & External Integration Risks

#### `ISS-005` — Full-File `readBytes()` Heap Allocation & 19 MB Inline Gemini Cap
* **Issue ID:** `ISS-005`
* **Title:** Loading raw video/audio into JVM `ByteArray` risks OOM and caps uploads at 19 MB
* **Severity:** High
* **Category:** Performance / Stability / AI Architecture
* **Affected Component:** `SpeakingAndLabsViewModel.kt` (lines 106, 139, 184), `GeminiCoachService.kt` (lines 150, 172)
* **File Path:** `/app/src/main/java/com/example/ui/viewmodel/SpeakingAndLabsViewModel.kt`
* **Description:** `attachUploadedMedia` calls `resolver.openInputStream(uri)?.use { it.readBytes() }` *before* checking file size (`OpenableColumns.SIZE`), allocating potentially 100+ MB on heap if a user picks a large video from Photo Picker! Furthermore, `inlineData(mediaBytes, mimeType)` enforces a 19 MB payload cap (`~2–3 min` SD clip).
* **Root Cause:** Synchronous byte-array read instead of `AssetFileDescriptor.length` / `OpenableColumns.SIZE` pre-check and cloud File API URI upload.
* **Impact:** `OutOfMemoryError` crash on 2–4 GB RAM devices when selecting large gallery videos; failed analyses for longer speeches.
* **Steps to Reproduce:** Select a >200 MB 4K video via Photo Picker in `SpeakingLabScreen`.
* **Current Behavior:** Streams entire file into `ByteArray` in memory before failing the `19 * 1024 * 1024` size check.
* **Expected Behavior:** Query `OpenableColumns.SIZE` before reading; reject `> 19 MB` immediately (or stream to cloud storage + Gemini File API).
* **Recommended Fix:** Add upfront `ContentResolver` size check and audio-track extraction (`MediaExtractor`/`MediaMuxer`) for long speeches.
* **Verification Plan:** Unit/Robolectric test with oversized mock `Cursor` size metadata.
* **Dependencies:** Cloud Storage / Gemini File API for >19 MB support.
* **Status:** Confirmed Defect (Open)

#### `ISS-006` — Unthrottled Client AI Calls, Prompt Injection Exposure & Missing Release AppCheck Fallback
* **Issue ID:** `ISS-006`
* **Title:** Raw user/YouTube text interpolated into Gemini prompts without backend quotas, sanitization, or guaranteed release App Check
* **Severity:** High
* **Category:** Security / AI Cost Control
* **Affected Component:** `GeminiCoachService.kt`, `AppCheckConfig.kt` (lines 45–60)
* **File Path:** `/app/src/main/java/com/example/data/ai/GeminiCoachService.kt` (lines 156–167, 239–250, 391–405)
* **Description:** (1) Video titles, descriptions, transcripts, and user questions are interpolated directly into LLM prompts without delimiter isolation or length/control-character sanitization. (2) No per-user rate limit or token budget exists. (3) If `R.string.recaptcha_site_key` is absent in a release build, `AppCheckConfig` logs an error and silently proceeds without App Check protection.
* **Root Cause:** Direct client-to-Gemini SDK usage without an intermediate API gateway or quota store.
* **Impact:** Prompt injection via malicious YouTube descriptions/user inputs; unbounded Gemini API billing risk under spam; missing App Check enforcement in misconfigured release builds.
* **Steps to Reproduce:** Inspect `GeminiCoachService.kt:241-244` and `AppCheckConfig.kt:46-59`.
* **Current Behavior:** Raw interpolation; zero per-user request throttling.
* **Expected Behavior:** Move prompts server-side, wrap untrusted text in XML/JSON boundaries with explicit instruction hierarchy, enforce per-user Redis quotas, and fail-closed or verify `recaptcha_site_key` at CI build time.
* **Recommended Fix:** Introduce backend AI proxy + per-user rate limit counters + CI check for release App Check key.
* **Verification Plan:** Security review + automated rate-limit tests (`429 Too Many Requests`).
* **Dependencies:** Backend API Gateway (`02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`).
* **Status:** Security / Reliability Risk (Open)

#### `ISS-007` — Fragile Client-Side YouTube HTML/Regex Scraping
* **Issue ID:** `ISS-007`
* **Title:** Caption and duration extraction relies on regex scraping mobile YouTube watch HTML
* **Severity:** High
* **Category:** Reliability / External Integration
* **Affected Component:** `YouTubeIntelligenceClient.kt`
* **File Path:** `/app/src/main/java/com/example/data/youtube/YouTubeIntelligenceClient.kt`
* **Description:** Without a Data API key, channel resolution and caption retrieval scrape HTML (`"channelId"`, `"captionTracks"`, `"lengthSeconds"`) using hardcoded mobile Safari User-Agent strings. No shared backend cache exists, so every user repeats identical HTTP + Gemini work.
* **Root Cause:** Client-only architecture without an official server-side YouTube ingestion worker.
* **Impact:** High breakage risk whenever YouTube alters inline `ytInitialPlayerResponse` structure, shows EU cookie/consent walls, or rate-limits mobile/ISP IPs.
* **Steps to Reproduce:** Inspect `YouTubeIntelligenceClient.kt` watch page parser.
* **Current Behavior:** Scrapes directly from user device on every audit click.
* **Expected Behavior:** Dedicated server worker using official YouTube Data API v3 + authorized caption/transcript pipeline with shared database caching.
* **Recommended Fix:** Move ingestion to scheduled backend service (`Admin Sync` + cached catalog) so regular learners query cached extractions.
* **Verification Plan:** Contract tests with recorded HTML/RSS fixtures + graceful fallback monitoring.
* **Dependencies:** Backend ingestion worker.
* **Status:** Architectural Fragility (Open)

---

### Category D: Security, Privacy & Firestore Rules

#### `ISS-008` — Client-Writable Aggregate Scores & Counters in `users/{uid}`
* **Issue ID:** `ISS-008`
* **Title:** Clients directly compute and write `fluencyScore`, `streakDays`, `recordingsCount`, and `totalPracticeMinutes`
* **Severity:** Medium
* **Category:** Security / Data Integrity
* **Affected Component:** `firestore.rules` (lines 270–283), `SpeakCraftRepository.kt` (lines 553–569, 730–757)
* **File Path:** `/firestore.rules`, `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt`
* **Description:** `firestore.rules` allows the owning user to `update` all score and streak fields directly on `/users/{uid}` as long as values fall within `0..100` or `>= 0`.
* **Root Cause:** Absence of Cloud Functions / server-side transactional triggers.
* **Impact:** Compromised clients can fabricate `100` scores or `36500` streak days; partial client failures between session creation and profile update can desynchronize counters (`recordingsCount` vs actual `speakingSessions.size`).
* **Steps to Reproduce:** Inspect `firestore.rules:276-282`.
* **Current Behavior:** Client updates aggregate profile fields directly.
* **Expected Behavior:** Make score/streak/count fields read-only to clients in `firestore.rules` and maintain them via server triggers/transactions.
* **Recommended Fix:** Deploy Cloud Function / backend worker on `speakingSessions` & `dailyActivities` writes; restrict client profile `update` keys to `displayName`, `communicationLevel`, `primaryGoal`, `dailyGoalMinutes`.
* **Verification Plan:** Firestore emulator rules test asserting client score-tampering `update` fails.
* **Dependencies:** Cloud Functions or Backend API.
* **Status:** Security / Integrity Debt (Open)

#### `ISS-009` — Incomplete List Element Validation in `isBoundedStringList`
* **Issue ID:** `ISS-009`
* **Title:** Firestore rules only validate types and lengths for list indices `0..2`
* **Severity:** Medium
* **Category:** Security / Schema Validation
* **Affected Component:** `isBoundedStringList` in `firestore.rules`
* **File Path:** `/firestore.rules` (lines 39–45, 74–75)
* **Description:** `isBoundedStringList(arr, maxLen)` checks `arr[0]`, `arr[1]`, and `arr[2]` for `is string && size() <= 500`, leaving indices `3..24` unchecked. Similarly, `isValidEnrollment` only checks `completedDays[0] is int`.
* **Root Cause:** Firestore Security Rules CEL lacks `for`/`all` array iteration predicates.
* **Impact:** Malicious clients could store oversized strings (>500 chars) or nested maps inside array elements at index >= 3.
* **Steps to Reproduce:** Write a `SpeakingSession` with `mistakes = ["a", "b", "c", {nested: true}]` in `firestore.test.js`.
* **Current Behavior:** Passes `isBoundedStringList` validation.
* **Expected Behavior:** Either validate via backend API schema (Zod/Pydantic/Kotlinx) before write, or store normalized child records/sub-objects.
* **Recommended Fix:** Enforce strict array validation in proposed backend API gateway (`API-004`/`API-007`) and document CEL limits.
* **Verification Plan:** Add negative test case in `firestore.test.js` / API unit tests.
* **Dependencies:** Backend API gateway for full enforcement.
* **Status:** Known Platform Limitation / Security Debt (Open)

#### `ISS-026` — Unfiltered Admin Queries Exposed in Repository Without Local Role Guard
* **Issue ID:** `ISS-026`
* **Title:** `observeAdminReleases`, `observePendingModerationTechniques`, and `observeAllUpdateEventsForAdmin` rely solely on UI gating and Firestore error swallowing
* **Severity:** Medium
* **Category:** Security / Performance
* **Affected Component:** `SpeakCraftRepository.kt` (lines 298–353), `UpdateAndAdminViewModel.kt` (lines 43–54)
* **File Path:** `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt`
* **Description:** `observePendingModerationTechniques()` queries `extractedTechniques` without filtering `moderationStatus == "PUBLISHED"`, and `.catch { emit(emptyList()) }` silently swallows `PERMISSION_DENIED` errors if a non-admin screen ever collects it.
* **Root Cause:** Silent `.catch { emit(emptyList()) }` on admin flows.
* **Impact:** Masked permission errors during debugging; if non-admin UI accidentally collects `moderationTechniques`, Firestore rejects the listener silently.
* **Steps to Reproduce:** Collect `updateAdminVm.moderationTechniques` as a non-admin user.
* **Current Behavior:** Emits empty list silently after Firestore permission denial.
* **Expected Behavior:** Gate admin flows cleanly via `isAdmin.flatMapLatest { ... }` and log non-cancellation rule denials.
* **Recommended Fix:** Combine `observeIsAdmin(uid)` before attaching admin snapshot listeners.
* **Verification Plan:** Unit test confirming zero Firestore admin queries attach when `isAdmin == false`.
* **Dependencies:** None.
* **Status:** Confirmed Technical Debt (Open)

---

### Category E: Performance, Memory & Query Scaling

#### `ISS-010` — Unbounded Real-Time Firestore Listeners on 10 User Subcollections
* **Issue ID:** `ISS-010`
* **Title:** Repository streams entire subcollections without `.limit()` or pagination while UI truncates to 10 items
* **Severity:** Medium
* **Category:** Performance / Scalability / Cost
* **Affected Component:** `SpeakCraftRepository.kt` (lines 92–222), `SpeakingLabScreen.kt` (line 335)
* **File Path:** `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt`
* **Description:** `observeSpeakingSessions`, `observeSpeechFeedback`, `observeVocabulary`, `observeDailyActivities`, `observeLearningProgress`, `observeExtractedTechniques`, `observeConversationSessions`, and `observeRealWorldTasks` stream unbounded `.snapshots()`. Meanwhile `SpeakingLabScreen` renders only `pastSessions.take(10)` with no "View All / Load More" pagination.
* **Root Cause:** Simplicity of initial prototype streams.
* **Impact:** Linear growth in Firestore read costs, memory usage, and deserialization latency per active user over months of use.
* **Steps to Reproduce:** Seed 500 `speakingSessions` (with 15KB transcripts) and inspect `observeSpeakingSessions`.
* **Current Behavior:** Downloads all 500 docs on every session launch; displays only 10 in `SpeakingLabScreen`.
* **Expected Behavior:** Query recent page (e.g., `.limit(25)`) with cursor-based "Load More" pagination and dedicated session history browser.
* **Recommended Fix:** Add paginated query APIs + `limit(25)` default on `speakingSessions` and `speechFeedback`.
* **Verification Plan:** Benchmark memory & read count with 500 seeded documents.
* **Dependencies:** Firestore composite indexes if combining `where` + `orderBy`.
* **Status:** Confirmed Performance Debt (Open)

#### `ISS-011` — Duplicate `seedAskVinhKnowledgeBaseIfEmpty()` Calls on Launch & Onboarding
* **Issue ID:** `ISS-011`
* **Title:** Knowledge base seeding runs on every `MainCoachViewModel` init and again on onboarding completion
* **Severity:** Medium
* **Category:** Performance / Startup Overhead
* **Affected Component:** `MainCoachViewModel.kt` (lines 150–154, 193)
* **File Path:** `/app/src/main/java/com/example/ui/viewmodel/MainCoachViewModel.kt`
* **Description:** `init` launches `seedAskVinhKnowledgeBaseIfEmpty()`, executing a Firestore `limit(1).get()` on every authenticated cold launch, and `completeOnboardingAssessment()` calls it a second time. Also seeds into every user's private subcollection instead of referencing a shared read-only catalog.
* **Root Cause:** Per-user duplication of static `@askvinh` catalog data.
* **Impact:** Extra Firestore read on every launch + N copies of identical seed documents across N users' storage.
* **Steps to Reproduce:** Inspect `MainCoachViewModel.kt:150-154` and `SpeakCraftRepository.kt:420-484`.
* **Current Behavior:** Duplicates static `CurriculumCatalog.askVinhKnowledgeBase` into `/users/{uid}/extractedTechniques` and `youtubeSources`.
* **Expected Behavior:** Serve base catalog from shared read-only catalog / `CurriculumCatalog`, storing only *user-discovered* YouTube audits in user subcollections.
* **Recommended Fix:** Gate seeding with a local/profile `seededCatalogVersion` flag or serve shared baseline globally.
* **Verification Plan:** Unit test verifying zero seeding writes on repeat launches.
* **Dependencies:** None.
* **Status:** Confirmed Technical Debt (Open)

---

### Category F: Build, Dependencies, i18n & Theme Debt

#### `ISS-012` — Unused Dependencies & Active Room KSP Compiler
* **Issue ID:** `ISS-012`
* **Title:** Unused Retrofit, Moshi, OkHttp Logging Interceptor, and Room KSP processor bloat build & APK
* **Severity:** Medium
* **Category:** Build / Technical Debt
* **Affected Component:** `app/build.gradle.kts`, `gradle/libs.versions.toml`
* **File Path:** `/app/build.gradle.kts`
* **Description:** `app/build.gradle.kts` actively includes `retrofit`, `converter-moshi`, `moshi-kotlin`, `ksp(moshi-kotlin-codegen)`, `logging-interceptor`, and `ksp(androidx.room.compiler)`, even though Room dependencies are commented out and all networking uses `OkHttp` + `org.json.JSONObject` directly.
* **Root Cause:** Template dependencies left enabled after pivoting to OkHttp + Firestore.
* **Impact:** Slower KSP/Gradle compilation and unnecessary DEX bytecode inside APK.
* **Steps to Reproduce:** Grep `retrofit`, `moshi`, `androidx.room` across `/app/src/main`.
* **Current Behavior:** 0 usages in `src/main`, yet libraries are compiled into the app.
* **Expected Behavior:** Comment out or remove unused dependencies and KSP processors.
* **Recommended Fix:** Prune unused Gradle entries (post-audit).
* **Verification Plan:** `compile_applet` + APK size comparison.
* **Dependencies:** None.
* **Status:** Confirmed Technical Debt (Open)

#### `ISS-013` — Hardcoded UI Strings & Non-Externalized Curriculum Text
* **Issue ID:** `ISS-013`
* **Title:** Nearly all UI labels, errors, and curriculum text are hardcoded in Kotlin instead of `strings.xml`
* **Severity:** Medium
* **Category:** Technical Debt / Internationalization (i18n)
* **Affected Component:** All screens in `/app/src/main/java/com/example/ui/` & `CurriculumCatalog.kt`
* **File Path:** `/app/src/main/res/values/strings.xml` (4 lines total)
* **Description:** `strings.xml` contains only `app_name = SpeakCraft`. Every screen title, button label, error message, and educational copy is hardcoded in English Kotlin literals.
* **Root Cause:** Rapid prototyping directly in Compose code.
* **Impact:** Blocks localization, accessibility string reuse, and consistent copy management.
* **Steps to Reproduce:** Open `app/src/main/res/values/strings.xml`.
* **Current Behavior:** Only `app_name` is externalized.
* **Expected Behavior:** Extract UI chrome strings to `strings.xml` / `stringResource(R.string.*)` and externalize curriculum JSON/CMS.
* **Recommended Fix:** Incremental string resource extraction + Lint `HardcodedText` checks.
* **Verification Plan:** Android Lint string verification.
* **Dependencies:** None.
* **Status:** Confirmed Technical Debt (Open)

#### `ISS-014` — Hardcoded Colors & Legacy Manifest Theme Name
* **Issue ID:** `ISS-014`
* **Title:** `AndroidManifest.xml` uses `@style/Theme.MyApplication`; Composables hardcode hex colors
* **Severity:** Medium
* **Category:** UI/UX / Theme Consistency
* **Affected Component:** `AndroidManifest.xml` (line 25), `DashboardScreen.kt` (lines 255, 270), `CoachComponents.kt` (line 255), `CameraVideoRecorder.kt`
* **File Path:** `/app/src/main/AndroidManifest.xml`, `/app/src/main/java/com/example/ui/screens/DashboardScreen.kt`
* **Description:** Manifest splash/window theme retains template name `Theme.MyApplication`. `DashboardScreen` uses `Color(0xFFE2E8F0)` and `Color.White` instead of semantic theme colors (`MaterialTheme.colorScheme.onPrimary` / custom surface tokens).
* **Root Cause:** Template XML left intact; inline colors used on dark hero card.
* **Impact:** Breaks strict centralized theming and risks contrast issues if palette changes.
* **Steps to Reproduce:** Inspect `AndroidManifest.xml:25` and `DashboardScreen.kt:255`.
* **Current Behavior:** Hardcoded hex colors in UI composables.
* **Expected Behavior:** Rename XML style to `Theme.SpeakCraft` and replace inline `Color(...)` calls with `SpeakCraftTheme` semantic colors.
* **Recommended Fix:** Update `themes.xml`/`AndroidManifest.xml` and add `onDarkSurfaceMuted` semantic token.
* **Verification Plan:** Visual review in Light and Dark mode.
* **Dependencies:** None.
* **Status:** Confirmed Technical Debt (Open)

#### `ISS-015` — Single-Weight Font File Mapping in `Type.kt`
* **Issue ID:** `ISS-015`
* **Title:** `Normal`, `Medium`, `SemiBold`, and `Bold` weights map to the same `.ttf` file per font family
* **Severity:** Medium
* **Category:** Typography / UI Polish
* **Affected Component:** `Type.kt`
* **File Path:** `/app/src/main/java/com/example/ui/theme/Type.kt`
* **Description:** Each `FontFamily` (`PlusJakartaSansFamily`, `DmSansFamily`, `JetBrainsMonoFamily`) maps 3–4 `FontWeight`s to the identical single `R.font.*` resource without `variationSettings` or separate static weight `.ttf` files.
* **Root Cause:** Single font file downloaded per family via `font-util`.
* **Impact:** Weak visual hierarchy between `titleLarge`, `labelLarge`, and `bodyMedium` unless synthetic bolding occurs.
* **Steps to Reproduce:** Inspect `ui/theme/Type.kt`.
* **Current Behavior:** Identical `R.font.*` ID across weights.
* **Expected Behavior:** Bundle distinct weight `.ttf` files (`-Regular`, `-Medium`, `-SemiBold`, `-Bold`) or configure variable-font `FontVariation.weight(...)` if files are variable fonts.
* **Recommended Fix:** Check font files and supply distinct static weights via `font-util`.
* **Verification Plan:** Visual hierarchy audit on Dashboard & Feedback cards.
* **Dependencies:** None.
* **Status:** Confirmed Defect (Open)

---

### Category G: Incomplete Features, UX & Navigation

#### `ISS-016` — Missing Post-Install & Rollback Telemetry Events (`INSTALLED`, `ROLLED_BACK`)
* **Issue ID:** `ISS-016`
* **Title:** Updater never emits `DOWNLOADED`, `INSTALLED`, or `ROLLED_BACK` telemetry events despite schema support
* **Severity:** Medium
* **Category:** Incomplete Feature / Telemetry
* **Affected Component:** `UpdateAndAdminViewModel.kt` (lines 111–155), `AppUpdateScreen.kt`, `AdminDashboardScreen.kt` (line 353)
* **File Path:** `/app/src/main/java/com/example/ui/viewmodel/UpdateAndAdminViewModel.kt`
* **Description:** `ApplicationUpdateEvent` rules permit `CHECKED`, `DOWNLOADED`, `VERIFIED`, `INSTALL_LAUNCHED`, `INSTALLED`, `FAILED`, `ROLLED_BACK`. However, `UpdateAndAdminViewModel` only logs `CHECKED`, `VERIFIED`, `FAILED`, and `INSTALL_LAUNCHED`. On relaunch after an APK upgrade, there is no `PackageInfo` version-change check logging `INSTALLED` (or `ROLLED_BACK` if recovery triggers).
* **Root Cause:** Post-install startup reconciliation logic was not wired into `AuthenticatedAppShell` / `UpdateAndAdminViewModel`.
* **Impact:** Admin Telemetry `"Verified Installs"` counts `INSTALL_LAUNCHED` (intent fired) rather than confirmed post-restart upgrades; true install completion rate is unknown.
* **Steps to Reproduce:** Inspect `UpdateAndAdminViewModel.kt` for `"INSTALLED"` or `"ROLLED_BACK"`.
* **Current Behavior:** Never logged.
* **Expected Behavior:** Store `pendingTargetVersion` before `ACTION_VIEW` install; on next app launch compare `BuildConfig.VERSION_NAME` and log `INSTALLED` or `FAILED`/`ROLLED_BACK`.
* **Recommended Fix:** Track last known installed version in `AppPreferencesRepository` and emit `INSTALLED` on version bump.
* **Verification Plan:** Unit test simulating `1.0.0 -> 1.1.0` preference transition.
* **Dependencies:** None.
* **Status:** Incomplete Feature (Open)

#### `ISS-017` — Missing Backup Snapshot Restore UI
* **Issue ID:** `ISS-017`
* **Title:** `AppUpdateScreen` creates and lists JSON backups but provides no UI flow to restore one
* **Severity:** Medium
* **Category:** Incomplete Feature / Reliability
* **Affected Component:** `AppUpdateScreen.kt` (lines 258–286), `UpdateBackupManager.kt`
* **File Path:** `/app/src/main/java/com/example/ui/screens/AppUpdateScreen.kt`
* **Description:** Users can create manual snapshots (`speakcraft_backup_*.json`) and view up to 4 filenames, but there is no `"Restore"` button to re-apply saved preferences (`themeMode`, `ttsRate`, `updateChannel`, encrypted key) or inspect backed-up session summaries.
* **Root Cause:** Only backup creation and interrupted-update detection were wired to UI.
* **Impact:** Backups are write-only from a user perspective unless manually extracted from app storage.
* **Steps to Reproduce:** Open `App Updates & Backups` -> tap `"Create Manual Snapshot Backup Now"` -> observe listed backups.
* **Current Behavior:** Informational text only; non-interactive.
* **Expected Behavior:** Add a `"Restore"` action with confirmation dialog and restoration feedback.
* **Recommended Fix:** Add `restoreBackup(fileName)` in `UpdateBackupManager` + `UpdateAndAdminViewModel` + button in `AppUpdateScreen`.
* **Verification Plan:** Unit test verifying `AppPreferences` round-trip restore.
* **Dependencies:** None.
* **Status:** Incomplete Feature (Open)

#### `ISS-018` — Non-Functional Analytics Period Filters & Duplicate Route Alias
* **Issue ID:** `ISS-018`
* **Title:** `Daily`/`Weekly`/`Monthly` chips do not filter session data; `articulation_lab` and `pronunciation` share identical screen
* **Severity:** Medium
* **Category:** Broken UI Control / Navigation Debt
* **Affected Component:** `AnalyticsAndSettingsScreens.kt` (lines 63, 80–104), `DashboardScreen.kt` (lines 334–341), `AppNavigation.kt` (lines 478–483)
* **File Path:** `/app/src/main/java/com/example/ui/screens/AnalyticsAndSettingsScreens.kt`, `/app/src/main/java/com/example/ui/AppNavigation.kt`
* **Description:** (1) Changing `period` (`Daily`/`Weekly`/`Monthly`) on `AnalyticsScreen` or `Weekly`/`Monthly` on `DashboardScreen` changes only the card subtitle string — `trendScores` still computes `sessions.take(10).reversed()` regardless of `createdAt` timestamps! (2) `AppNavigation.kt` maps both `"articulation_lab"` and `"pronunciation"` to `PronunciationLabScreen(...)`, making two dashboard cards lead to the same screen.
* **Root Cause:** UI filter chips wired to display label only; shared lab screen reused for two module slugs.
* **Impact:** Misleading analytics charts and user confusion when tapping two distinct dashboard tiles that open the identical view.
* **Steps to Reproduce:** Open `Progress Analytics` -> tap `Daily`, `Weekly`, `Monthly` -> observe sparkline points remain identical.
* **Current Behavior:** Chart never changes.
* **Expected Behavior:** Filter `sessions` by `createdAt` epoch window (`1d`, `7d`, `30d`), and differentiate Articulation Lab (jaw/tongue mechanics, facial warmups) or consolidate tiles.
* **Recommended Fix:** Implement `filterSessionsByPeriod(sessions, period)` pure function with unit tests, and specialize `ArticulationLabScreen`.
* **Verification Plan:** Unit test on date-window filtering.
* **Dependencies:** None.
* **Status:** Confirmed Bug / Technical Debt (Open)

#### `ISS-019` — Destructive History Deletion Without Confirmation & Non-Wrapping Onboarding Chips
* **Issue ID:** `ISS-019`
* **Title:** Deleting speaking session(s) executes immediately with zero confirmation; onboarding level chips lack `FlowRow`
* **Severity:** Medium
* **Category:** UX Defect / Accessibility
* **Affected Component:** `AnalyticsAndSettingsScreens.kt` (lines 315–320), `SpeakingAndChallengesScreens.kt` (lines 364–369), `AuthAndOnboardingScreens.kt` (line 229)
* **File Path:** `/app/src/main/java/com/example/ui/screens/AnalyticsAndSettingsScreens.kt`
* **Description:** Tapping `"Delete Speaking Session History"` in Settings or the trash icon in `SpeakingLabScreen` permanently deletes Firestore records and local `.m4a`/`.mp4` files with zero confirmation `AlertDialog` or undo snackbar. On Onboarding, 4 level `FilterChip`s sit in a non-wrapping `Row`, risking clipping at large font scales or narrow widths.
* **Root Cause:** Direct callback invocation without confirmation state; `Row` used instead of `FlowRow`.
* **Impact:** Accidental permanent data loss on mis-tap; clipped text on accessible font sizes.
* **Steps to Reproduce:** Settings -> tap `"Delete Speaking Session History"`.
* **Current Behavior:** Deletes all sessions immediately.
* **Expected Behavior:** Show Material 3 confirmation `AlertDialog` before destructive deletion; wrap onboarding chips with `ExperimentalLayoutApi` `FlowRow`.
* **Recommended Fix:** Add confirmation modal state + `FlowRow` + `Modifier.semantics` on `Canvas` charts (`CoachComponents.kt:175,213`).
* **Verification Plan:** Compose Robolectric test verifying confirmation dialog blocks accidental deletion until confirmed.
* **Dependencies:** None.
* **Status:** Confirmed UX Defect (Open)

#### `ISS-021` — Ephemeral AI Mentor & Unsaved Conversation Simulator State
* **Issue ID:** `ISS-021`
* **Title:** AI Mentor retains only the latest single answer in memory; Conversation Simulator discards history unless debrief is tapped
* **Severity:** Medium
* **Category:** Incomplete Feature / UX
* **Affected Component:** `InteractiveCoachViewModel.kt` (lines 41–53, 94–114), `ConversationAndMentorScreens.kt`
* **File Path:** `/app/src/main/java/com/example/ui/viewmodel/InteractiveCoachViewModel.kt`
* **Description:** `AiMentorScreen` stores a single `UiState<String>` (`_mentorAnswer`), overwriting previous Q&A pairs on each question and losing all state when the ViewModel clears. `ConversationSimulatorScreen` only saves a 4,000-char `transcriptPreview` when the user explicitly clicks `"End & Get Coach Check-In Feedback"`.
* **Root Cause:** In-memory `MutableStateFlow` without Firestore thread persistence.
* **Impact:** Users cannot revisit past AI Mentor coaching plans or resume an interrupted conversation roleplay.
* **Steps to Reproduce:** Ask 2 questions in AI Mentor -> sign out/restart app -> reopen AI Mentor.
* **Current Behavior:** Resets to default greeting string.
* **Expected Behavior:** Persist mentor conversation threads and auto-save active conversation simulator turns.
* **Recommended Fix:** Add `/users/{uid}/mentorMessages` (or `mentorThreads`) collection and auto-save simulator turns.
* **Verification Plan:** Repository + ViewModel persistence test.
* **Dependencies:** Firestore rule update for new collection.
* **Status:** Incomplete Feature (Open)

#### `ISS-022` — Profile Update Score Overwrite & Vocabulary Content Immutability
* **Issue ID:** `ISS-022`
* **Title:** `createOrUpdateProfile` overwrites rolling scores if called on an existing profile; `upsertVocabularyEntry` ignores edits to word/meaning/example
* **Severity:** Low
* **Category:** Logic Defect / Feature Completeness
* **Affected Component:** `SpeakCraftRepository.kt` (lines 399–417, 649–659)
* **File Path:** `/app/src/main/java/com/example/data/firebase/SpeakCraftRepository.kt`
* **Description:** If `createOrUpdateProfile` is invoked for an existing profile, it overwrites all 6 skill scores rather than updating profile preferences (`displayName`, `level`, `primaryGoal`, `dailyGoalMinutes`). Conversely, `upsertVocabularyEntry` omits `meaning`, `exampleSentence`, and `level` on updates (matching `firestore.rules:368`), so users cannot edit a custom vocabulary word after saving.
* **Root Cause:** Coarse-grained repository methods.
* **Impact:** Prevents future "Edit Profile" or "Edit Custom Word" flows from reusing existing methods safely.
* **Steps to Reproduce:** Inspect `SpeakCraftRepository.kt:400-416` and `650-658`.
* **Current Behavior:** As described above.
* **Expected Behavior:** Split `updateProfilePreferences()` from `createProfile()`; allow updating user-authored `meaning`/`exampleSentence` on custom vocabulary entries.
* **Recommended Fix:** Refactor repository update paths & rules `affectedKeys()`.
* **Verification Plan:** Firestore rules + unit tests.
* **Dependencies:** `firestore.rules` update if expanding vocabulary editable fields.
* **Status:** Confirmed Tech Debt (Open)

---

### Category H: Testing, Telemetry & DevOps Gaps

#### `ISS-025` — Zero Live End-to-End Integration Tests for AI, CameraX & YouTube Network Parsing
* **Issue ID:** `ISS-025`
* **Title:** Test suite covers pure unit/Robolectric smoke UI and Firestore rules, but lacks live AI contract, CameraX, and HTTP mock-server tests
* **Severity:** Medium
* **Category:** Testing / QA Gap
* **Affected Component:** `/app/src/test/java/com/example/`
* **File Path:** `/app/src/test/java/com/example/SpeakCraftComposeCujTest.kt`
* **Description:** Existing 17 tests verify SemVer, RecommendationEngine, YouTube XML/HTML string parsing, Robolectric CUJ navigation, and Firestore rules (`firestore.test.js`), while `ExampleInstrumentedTest.kt` cannot execute without an emulator. There are no `MockWebServer` tests for `YouTubeIntelligenceClient` HTTP error handling or `ReleaseUpdateRepository` streaming checksum validation, nor JSON schema fuzz tests for `GeminiCoachService`.
* **Root Cause:** Rapid MVP delivery without HTTP mock server or JSON fixture harness.
* **Impact:** Regressions in YouTube HTTP fallback paths, APK download corruption handling, or Gemini malformed JSON handling can reach production undetected.
* **Steps to Reproduce:** List `/app/src/test/java/com/example/`.
* **Current Behavior:** Only 5 JVM test files exist (`ExampleUnitTest`, `ExampleRobolectricTest`, `SemVerAndUpdateTest`, `CurriculumAndRecommendationTest`, `YouTubeParsingTest`, `SpeakCraftComposeCujTest`).
* **Expected Behavior:** Add OkHttp `MockWebServer` tests for updater/YouTube HTTP edge cases and JSON parser tests for `GeminiCoachService`.
* **Recommended Fix:** Introduce `okhttp3:mockwebserver` test dependency and expand JVM test coverage.
* **Verification Plan:** `gradle :app:testDebugUnitTest` passing with >80% data-layer branch coverage.
* **Dependencies:** None.
* **Status:** Testing Gap (Open)

#### `ISS-027` — Missing `firestore.indexes.json` & CI/CD Pipeline Configuration
* **Issue ID:** `ISS-027`
* **Title:** No CI workflow (`.github/workflows`), automated test gate, or `firestore.indexes.json` checked into repository
* **Severity:** Low
* **Category:** DevOps / Build Automation
* **Affected Component:** Project Root (`/firebase.json`, missing `.github/`)
* **File Path:** `/firebase.json`
* **Description:** `firebase.json` defines Firestore database and rules path, but no `firestore.indexes.json` or automated CI pipeline exists to run `gradle :app:testDebugUnitTest`, `npm test` (`firestore.test.js`), and signed APK checksum generation.
* **Root Cause:** Cloud IDE interactive deployment workflow.
* **Impact:** Adding `.orderBy()` to filtered Firestore queries (`ISS-010`) or shipping verified APK releases (`AdminDashboardScreen`) currently requires manual steps.
* **Steps to Reproduce:** Inspect root directory listing.
* **Current Behavior:** Manual build & rules deploy only.
* **Expected Behavior:** Add GitHub Actions CI running unit tests, Robolectric CUJs, Firestore emulator tests, and automated SHA-256 release manifest generation.
* **Recommended Fix:** Create CI workflow and `firestore.indexes.json` once composite queries are added.
* **Verification Plan:** Green CI check on PR.
* **Dependencies:** None.
* **Status:** Missing DevOps Automation (Open)

#### `ISS-028` — Hardcoded `Locale.US` & Non-Configurable Reference Voice Engine
* **Issue ID:** `ISS-028`
* **Title:** `TtsAndSpeechHelper` hardcodes `Locale.US` and `SimpleDateFormat(..., Locale.US)` date keys without device timezone boundary safeguards
* **Severity:** Low
* **Category:** Localization / Edge Case Bug
* **Affected Component:** `TtsAndSpeechHelper.kt`, `MainCoachViewModel.kt` (line 51)
* **File Path:** `/app/src/main/java/com/example/data/media/TtsAndSpeechHelper.kt`, `/app/src/main/java/com/example/ui/viewmodel/MainCoachViewModel.kt`
* **Description:** `todayDateKey` in `MainCoachViewModel` is initialized once as a property (`val todayDateKey = SimpleDateFormat(...).format(Date())`). If a user keeps the app open across midnight, `todayDateKey` remains yesterday's date until ViewModel recreation.
* **Root Cause:** Property initialized once instead of computed function `currentDateKey()`.
* **Impact:** Late-night practice after midnight logs to the previous day's `dailyActivities/{dateId}` document.
* **Steps to Reproduce:** Open app at 23:55, toggle a daily step at 00:05 without killing the app.
* **Current Behavior:** Writes to yesterday's date key.
* **Expected Behavior:** Compute date key dynamically on each daily action using `java.time` / current timestamp.
* **Recommended Fix:** Replace `val todayDateKey` with `fun currentTodayDateKey()` evaluated at action time.
* **Verification Plan:** Unit test with injectable `Clock`.
* **Dependencies:** None.
* **Status:** Confirmed Minor Bug (Open)

---

## Prioritized Bug-Fixing & Tech-Debt Roadmap

| Priority | Sprint / Phase | Target Issues | Concrete Deliverables |
| :--- | :--- | :--- | :--- |
| **P0 (Immediate)** | Phase 1: Correctness & Core UX | `ISS-001`, `ISS-002`, `ISS-003`, `ISS-019`, `ISS-020`, `ISS-028` | Embed Media3 `PlayerView` for 3-Stage video audit; fix consecutive-day streak & delta minutes math; surface cloud-save failure retry; add confirmation dialogs on history/session delete; fix paused duration stopwatch; dynamic midnight date key. |
| **P1 (Short-Term)** | Phase 2: Stability, Performance & Analytics | `ISS-005`, `ISS-010`, `ISS-011`, `ISS-016`, `ISS-017`, `ISS-018`, `ISS-026` | Check `OpenableColumns.SIZE` before `readBytes()`; paginate Firestore streams (`limit(25)`) + add `firestore.indexes.json`; wire `Daily/Weekly/Monthly` period date filtering; add Backup Restore UI & `INSTALLED` telemetry; guard admin flows. |
| **P2 (Medium-Term)** | Phase 3: Backend Gateway, Storage & Security | `ISS-004`, `ISS-006`, `ISS-007`, `ISS-008`, `ISS-009`, `ISS-021` | Deploy Backend API Gateway + GCS/S3 signed media uploads; migrate Gemini prompts, rate limits & YouTube ingestion server-side; enforce server-computed scores/streaks; persist AI Mentor threads. |
| **P3 (Long-Term)** | Phase 4: Multi-Platform & Polish | `ISS-012`..`ISS-015`, `ISS-022`..`ISS-025`, `ISS-027` | Prune unused Retrofit/Moshi/Room KSP deps; externalize strings & fix font weights; refactor God repository; add `MockWebServer` tests & CI pipeline; build Electron + React Desktop/Web clients (`ISS-024`). |

---

## Cross-References & Open Questions

* **Cross-References:** `01_UI_UX_DESIGN_DOCUMENTATION.md` (`ISS-001`, `ISS-014`–`ISS-019`), `02_BACKEND_ARCHITECTURE_DOCUMENTATION.md` (`ISS-004`–`ISS-009`, `ISS-024`), `06_AI_FEATURES_AND_AI_ARCHITECTURE.md` (`ISS-005`, `ISS-006`), `07_DATABASE_DESIGN_AND_DOCUMENTATION.md` (`ISS-008`–`ISS-011`).
* **Open Questions:**
  1. Should `P0` fixes (`ISS-001`, `ISS-002`, `ISS-003`, `ISS-019`) be patched directly in the Android codebase prior to any Desktop/Web expansion?
  2. For `ISS-004`, should existing local recordings on a user's Android device automatically backfill to Cloud Storage once cloud media sync is enabled?
