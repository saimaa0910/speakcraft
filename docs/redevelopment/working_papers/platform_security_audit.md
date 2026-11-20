# SpeakCraft: Platform, Security, Build & Release Audit

Scope: read-only static review of `f:\speakcraft` at HEAD `68a9cb1` (branch `main`, clean tree, 26 commits, 2026-11-18 to 2026-11-20).
Nothing was built, run, deployed or tested. No JDK, Android SDK, Gradle cache or Firestore emulator exists in this environment. **Nothing below should be read as "tests passed".**

Legend: **[V]** = VERIFIED (seen in code/config at the cited line). **[I]** = INFERRED (reasoned from code plus SDK/platform behaviour; not executed).
Paths are relative to `f:\speakcraft`. `pkg/` = `app/src/main/java/com/example/`.

---

## 0. Executive summary (top 12)

| # | Finding | Severity | Status |
|---|---|---|---|
| 1 | Every push to `main` produces a **debug** build (`assembleDebug`) for the beta channel unless `ANDROID_KEYSTORE_BASE64` is configured. That build is debuggable, signed with a debug keystore (store password `android`), and installs the **App Check Debug provider**. This is the production OTA channel. | Critical (conditional on secrets) | [V] workflow logic; [I] secret state unknown |
| 2 | `saveSpeakingSession` writes `evaluationStatus`, `isPartialEvaluation`, `evaluatedDimensionKeys`, `unavailableDimensionKeys`, `evidenceSummary`. None of these is in the `speakingSessions` `hasOnly` allow-list, so **every speaking-session save is rejected by firestore.rules**. Regression introduced in HEAD `68a9cb1`. | High (functional) | [V] static diff of code vs rules |
| 3 | Account deletion is implemented in the repository and ViewModel but **has no UI caller**. There is no data export. This is a Play policy and GDPR gap. | High | [V] |
| 4 | Self-updating APK mechanism (GitHub Releases + Firestore), with a **user-editable GitHub repo** field and copy that tells users to "uninstall the old build once" when signing keys differ. Google Play disallows this. | High | [V] |
| 5 | `REQUEST_INSTALL_PACKAGES` is missing from the manifest, so the in-app installer intent will most likely fail on API 26+. | High (functional) | [V] missing; [I] runtime effect |
| 6 | `android:allowBackup="true"` with untouched template backup/extraction rules. Voice/video recordings, mentor chat history and learning JSON are all eligible for cloud backup and device transfer. | High (privacy) | [V] |
| 7 | `FirestoreErrorHandler` logs uid, email and provider emails to Logcat on every Firestore error, in all build types. | Medium | [V] |
| 8 | Sign-out clears nothing local. Recordings live in a shared `filesDir/recordings` directory that is not scoped to a user. `wipeAllLocalDataForUser` has no callers. | Medium | [V] |
| 9 | CI has **no unit tests, no lint, no rules tests and no rules deploy**. Its only test is `node --test` of the publish script. | High (process) | [V] |
| 10 | Admin model: any admin role, including MODERATOR, can publish or disable app releases and write lessons and challenges with no schema validation. Client and rules map roles differently. Admins can read every user profile. | Medium | [V] |
| 11 | `isMinifyEnabled = false`. `firebase-appcheck-debug` is an `implementation` dependency, so it ships in release builds. 11 catalog libraries are unused. The KSP and secrets plugins are applied but do nothing. | Medium/Low | [V] |
| 12 | Architecture: a single `:app` module of 48,894 Kotlin lines in 106 files. No domain layer, no repository interfaces, no DI framework. `AppContainer` is created inside a composable with `remember`. | n/a | [V] |

---

## 1. Technology inventory

### 1.1 Toolchain and SDK levels
| Item | Value | Evidence |
|---|---|---|
| Language | Kotlin (100% of 106 main source files); no Java sources | [V] `git ls-files` |
| Kotlin | 2.2.10, applied via `kotlin.compose` and `kotlin.serialization` plugins | [V] `gradle/libs.versions.toml:11,109-110` |
| Kotlin Android plugin | **Not applied.** The project relies on AGP 9 built-in Kotlin | [V] `app/build.gradle.kts:1-8`; [I] AGP 9 default |
| AGP | 9.1.1 | [V] `gradle/libs.versions.toml:2` |
| Gradle | 9.3.1 (wrapper properties only; `gradlew` and `gradle-wrapper.jar` are **not committed**) | [V] `gradle/wrapper/gradle-wrapper.properties:3`; `git ls-files` |
| KSP | 2.3.5, applied, **no processor declared** (no `ksp(...)` dependency) | [V] `libs.versions.toml:13`, `app/build.gradle.kts:5,104-169` |
| compileSdk / minSdk / targetSdk | 36 / 24 / 36 | [V] `app/build.gradle.kts:25,29,30` |
| Java source/target | 11 / 11; no `jvmToolchain`, no explicit Kotlin `jvmTarget` | [V] `app/build.gradle.kts:75-78` |
| CI JDK | Temurin 21 | [V] `.github/workflows/release_on_push.yml:59-63` |
| foojay toolchain resolver | applied, but no toolchain requested (unused) | [V] `settings.gradle.kts:15` |
| Modules | `:app` only | [V] `settings.gradle.kts:27` |
| namespace / applicationId | `com.example` (placeholder) / `com.aistudio.speakcraft.qxmvkp` | [V] `app/build.gradle.kts:24,28` |
| Default version | versionCode 2, versionName `1.1.0-beta.1`, channel `beta` (overridable through Gradle properties) | [V] `app/build.gradle.kts:10-18` |
| Gradle properties | config cache on, build cache on, `googleServices.missing.passthrough=true` | [V] `gradle.properties:20-21,28` |

### 1.2 Libraries (catalog version, used in `app/build.gradle.kts`, imported in source)
| Library | Version | In app deps | Imported in main src | Evidence |
|---|---|---|---|---|
| Compose BOM | **2024.09.00** (maps to Compose UI 1.7.0 and Material3 1.3.0 [I]) | yes | yes | `libs.versions.toml:12`; `app/build.gradle.kts:110` |
| material3 | from BOM | yes | yes | `:114` |
| material-icons-core / extended | from BOM | yes | 25 files | `:115-116` |
| ui-tooling-preview | from BOM | yes | **0 `@Preview` imports** | `:113` |
| navigation-compose | 2.8.9 | yes | 1 file (`ui/AppNavigation.kt`) | `libs:14` |
| lifecycle runtime-ktx / viewmodel-compose / runtime-compose | **2.8.7** | yes | 17 files | `libs:7-9` |
| activity-compose | 1.10.1 | yes | yes | `libs:10` |
| core-ktx | 1.18.0 | yes | yes | `libs:3` |
| kotlinx-coroutines core/android | 1.10.2 | yes | 35 files | `libs:24-25` |
| kotlinx-serialization-json | 1.9.0 | yes | 5 files. `org.json` is used in 7 files as well (two JSON stacks) | `libs:26` |
| OkHttp | **4.10.0** (2022). No Retrofit. Three separate `OkHttpClient` instances | yes | 4 files | `libs:34`; `pkg/data/update/GitHubReleaseClient.kt:12`, `pkg/data/update/ReleaseUpdateRepository.kt:53`, `pkg/data/youtube/YouTubeIntelligenceClient.kt:61` |
| DataStore Preferences | 1.1.7 (two stores: `speakcraft_prefs`, `speakcraft_tutorial_state`) | yes | 2 files | `pkg/data/preferences/AppPreferencesRepository.kt:22`, `pkg/data/preferences/TutorialState.kt:11` |
| Room | 2.7.0 | **no** | 0 | `libs:15-17,65-67` |
| Firebase BoM | 34.17.0 | yes | n/a | `libs:39` |
| firebase-auth | BoM | yes | 5 files | `app/build.gradle.kts:137` |
| firebase-firestore | BoM | yes | 8 files | `:136` |
| firebase-ai (AI Logic) | BoM | yes | 1 file (`data/ai/cloud/FirebaseAiLogicService.kt`), runtime-blocked by the LOCAL_ONLY policy | `:135`; `pkg/data/preferences/AISettingsRepository.kt:24`; `pkg/data/ai/local/LocalExecutionPolicy.kt:13-15` |
| firebase-appcheck-recaptcha | BoM | yes | 1 file | `:138` |
| firebase-appcheck-debug | BoM | **yes, as `implementation` (ships in release)** | 1 file | `:139` |
| Credential Manager (`credentials`, `credentials-play-services-auth`) | 1.5.0 | yes | 3 files | `libs:42,99-100` |
| googleid | 1.1.1 | yes | 1 file | `libs:43` |
| CameraX core/camera2/lifecycle/view/video | 1.5.0 | yes | 1 file (`data/media/CameraVideoRecorder.kt`) | `libs:29-32,82-86` |
| MediaPipe tasks-vision | 0.10.14 | yes | 3 files (face, pose, hand landmarkers) | `libs:44`; `app/build.gradle.kts:145` |
| MediaPipe tasks-audio | 0.10.14 | yes | **0 imports (unused)** | `app/build.gradle.kts:148` |
| TensorFlow Lite | 2.16.1 (MediaPipe's `tensorflow-lite-api` excluded) | yes | 1 file (`data/ai/local/audio/LocalSpeechRecognitionEngine.kt`) | `libs:45`; `app/build.gradle.kts:145-151` |
| JUnit | catalog entry `junit` points at **`junitVersion` (1.3.0)**, not `junit` (4.13.2). `junit:junit:1.3.0` does not exist [I]. It is probably masked by transitive 4.13.2 from Robolectric and compose-ui-test-junit4 [I] | test | n/a | `libs.versions.toml:4-5,50` |
| kotlinx-coroutines-test | 1.10.2 | test | 12 test files | `libs:18` |
| Robolectric | 4.16.1 (tests use `@Config(sdk=[36])`) | test | yes | `libs:38` |
| androidx.test core / runner | 1.6.1 / 1.6.2 | test | yes | `libs:19-20` |
| compose ui-test-junit4 | BOM | test + androidTest | yes | `app/build.gradle.kts:160,165` |
| androidx.test.ext junit / espresso | 1.3.0 / 3.7.0 | androidTest | 1 file | `:162-163` |

### 1.3 Declared in the catalog but not used in `app/build.gradle.kts` [V]
`androidx-room-runtime`, `androidx-room-ktx`, `androidx-room-compiler`, `coil-compose` (2.7.0), `retrofit` (2.12.0), `converter-moshi` (2.12.0), `moshi-kotlin` (1.15.2), `moshi-kotlin-codegen` (1.15.2), `accompanist-permissions` (0.37.3), `play-services-location` (21.3.0), `logging-interceptor` (4.10.0), plus the version key `junit = "4.13.2"`. Evidence: `gradle/libs.versions.toml:15-17,21-23,27-28,33,35-36,65-67,74-76,80-81,87,89-90`. The `grep` of all main and test sources returns 0 imports for every one of these.

### 1.4 Applied or included but doing nothing [V]
- **KSP plugin**: applied at `app/build.gradle.kts:5` with no `ksp()` dependency.
- **Secrets Gradle plugin**: applied at `app/build.gradle.kts:6,99-102`. It reads `.env` with `.env.example` as the default, and `.env.example:1` is a single comment line, so no BuildConfig fields are generated. No `BuildConfig.<secret>` reference exists in source.
- **google-services plugin** runs with `googleServices.missing.passthrough=true` (`gradle.properties:28`). `app/google-services.json` is **not** in the repo, yet `AuthHelper.kt:53,93` reference `R.string.default_web_client_id`, which only that file generates. **A local build without the JSON will fail to compile** [I]. CI restores it from a secret (`release_on_push.yml:70-104`).
- `mediapipe tasks-audio` and `ui-tooling-preview` are included with zero imports.
- `firebase-appcheck-debug` ships in release. It is only reachable behind `BuildConfig.DEBUG` (`pkg/data/firebase/AppCheckConfig.kt:24`).

### 1.5 Version inconsistencies [V for versions, I for impact]
- Compose BOM **2024.09.00** (about 26 months old) against AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10, core-ktx 1.18.0 and Firebase BoM 34.17.0. The UI stack is two years behind the toolchain.
- lifecycle **2.8.7**, navigation **2.8.9** and activity-compose **1.10.1** are also old relative to core-ktx 1.18.0.
- OkHttp **4.10.0** (2022) is old.
- MediaPipe **0.10.14** and TFLite **2.16.1**: [I] these native-library versions are likely not 16 KB page-aligned. Google Play requires 16 KB support for apps targeting Android 15+ (targetSdk is 36).
- The JUnit catalog mis-reference is described in §1.2.
- Docs drift: `docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:16,20` says `versionCode = 1 + run_number` and "decodes `debug.keystore.base64` from the repository". The workflow actually uses `2 + RUN_NUM` (`release_on_push.yml:137`) and the `DEBUG_KEYSTORE_BASE64` secret (`:240,300-309`).

### 1.6 DI, state and data approach
- **DI: manual service locator** `AppContainer` with 24 `by lazy` properties (`pkg/data/AppContainer.kt:22-128`). No Hilt, Dagger or Koin (0 imports). There is **no `Application` subclass** (manifest `<application>` has no `android:name`, `app/src/main/AndroidManifest.xml:17-25`). The container is built inside a composable: `remember(context.applicationContext) { AppContainer(...) }` (`pkg/ui/AppNavigation.kt:216`). [I] It is therefore rebuilt whenever the Activity is recreated (rotation, process restore).
- ViewModels are built with `viewModelFactory { initializer { ... } }`, keyed per uid (`pkg/ui/AppNavigation.kt:317,368,388`). They are scoped to the Activity's ViewModelStore, so they are not cleared on sign-out [I].
- Flow/StateFlow is used throughout with `stateIn(WhileSubscribed(5000))` (`pkg/ui/viewmodel/MainCoachViewModel.kt:44,94-97`).
- Local storage is DataStore (2 stores) plus hand-rolled JSON files (§5.3). No Room or SQLite.

### 1.7 Absent capabilities [V by grep: 0 imports/usages]
Crash reporting (no Crashlytics or Sentry), analytics, Remote Config, FCM/push, logging framework (no Timber; 84 raw `android.util.Log` calls), WorkManager, JobScheduler, AlarmManager, Services/foreground services, BroadcastReceivers, system notifications (`NotificationCompat` and `NotificationChannel` absent), DI framework, Play Integrity, Play In-App Updates/Review, network security config, baseline profiles, R8 rules.

---

## 2. Authentication

| Aspect | Finding | Evidence |
|---|---|---|
| Sign-in methods | **Google only**, via Credential Manager. Silent auto sign-in uses `GetGoogleIdOption` (authorized accounts only, auto-select). Interactive sign-in uses `GetSignInWithGoogleOption`. The ID token is exchanged with `GoogleAuthProvider.getCredential`. | [V] `pkg/data/firebase/AuthHelper.kt:41-82` (auto), `:84-124` (interactive), `:71,109` |
| Anonymous / email / phone | **None.** `grep signInAnonymously\|EmailAuthProvider\|signInWithEmail` returns 0 | [V] |
| Call sites | `AuthScreen` calls `attemptAutoSignIn` in `LaunchedEffect(Unit)` and `onGoogleSignInClicked` on button press | [V] `pkg/ui/screens/AuthAndOnboardingScreens.kt:92-102,239-252` |
| Session lifecycle | Firebase Auth persists the session. The root collects `auth.authStateFlow()` (callbackFlow over `AuthStateListener`). `user == null` shows `AuthScreen`; otherwise `AuthenticatedAppShell` runs inside `key(user.uid)`. | [V] `AuthHelper.kt:33-39`; `pkg/ui/AppNavigation.kt:244-261` |
| | [I] `auth.authStateFlow()` is called without `remember`, so a new flow instance is created on each recomposition of `SpeakCraftRoot` and collection restarts. | `AppNavigation.kt:244` |
| Sign-out | Clears Credential Manager state, then `Firebase.auth.signOut()` inside `NonCancellable`. Wired from Settings and Profile. **No local data cleanup** (§5). | [V] `AuthHelper.kt:126-143`; `AppNavigation.kt:1143,1201` |
| Signed-out behaviour | The whole app is gated. Every repository call `requireUserId()` throws if unauthenticated, and shared-catalog flows emit empty lists. | [V] `pkg/data/firebase/SpeakCraftRepository.kt:53-56,272-276` |
| Account deletion | `deleteCurrentUserAccountAndData()` deletes 19 subcollections in batches of 400 (also deleting local files referenced by `localMediaPath`), then the profile doc, then `currentUser.delete()`. **It is never called from the UI.** The VM wrapper `deleteAccountAndAllData` (`pkg/ui/viewmodel/MainCoachViewModel.kt:474`) has 0 callers. There is no re-authentication flow (`reauthenticate` returns 0 hits), so a `requires-recent-login` error would leave Firestore data deleted while the Auth account survives. | [V] `SpeakCraftRepository.kt:1287-1319`; grep |
| Error handling | Cancellation is handled separately (`GetCredentialCancellationException` goes to `onAuthCancelled`). Other errors surface the raw `e.localizedMessage` to the UI. A missing `default_web_client_id` gives an explicit config error. Auto sign-in swallows all errors and falls back to unauthenticated. | [V] `AuthHelper.kt:52-57,76-80,92-97,114-122` |
| Token / claims | `observeAdminRole` reads custom claims via `getIdToken(false)` (cached; claim changes need a token refresh). | [V] `SpeakCraftRepository.kt:243-253` |

---

## 3. Firestore data model

### 3.1 Database
- **Named database** `ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11` [V] (`app/src/main/res/values/firebase_applet_config.xml:3`, `firebase.json:4`). It is obtained with `FirebaseFirestore.getInstance(dbId)` in `pkg/data/AppContainer.kt:25-27`, `SpeakCraftRepository.kt:47-51` and `pkg/data/firebase/ActivityFirestoreRepository.kt:31-35`.
- Project id `gen-lang-client-0651259232` appears in `scripts/publish_firestore_release.test.mjs:11-12` and `docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:34` [V].
- **Offline persistence**: no `FirebaseFirestoreSettings` anywhere (`grep firestoreSettings|PersistentCacheSettings` returns 0) [V]. [I] The SDK default applies: an on-disk cache (about 100 MB), unencrypted, in app-private storage. [I] Writes are `await()`ed, and a write Task only completes after server acknowledgement, so offline mutations suspend indefinitely and the UI hangs. Many mutations are also non-transactional read-modify-write sequences (`SpeakCraftRepository.kt:482-529,644-690,862-893`), so they can race.

### 3.2 Paths used in code vs blueprint vs rules
| Path | Code | `firebase-blueprint.json` | `firestore.rules` | Notes |
|---|---|---|---|---|
| `users/{uid}` | R/W `SpeakCraftRepository.kt:85,482` | `:525` | `:332-364` | OK |
| `users/{uid}/speakingSessions` | R `:99` / W `:790` | `:534` | `:453-474` | **Mismatch: 5 written fields not allowed (§4.4). Writes denied.** |
| `users/{uid}/speechFeedback` | R `:113` / W `:791` | `:535` | `:476-494` | OK, but it is in the same batch as speakingSessions, so it fails too |
| `users/{uid}/vocabulary` | `:127,744` | `:533` | `:432-451` | OK |
| `users/{uid}/dailyActivities` | `:140,644` | `:527` | `:388-408` | OK |
| `users/{uid}/learningProgress` | `:153,701` | `:528` | `:410-430` | OK |
| `users/{uid}/enrollments` | `:166,601` | `:526` | `:366-386` | OK |
| `users/{uid}/youtubeSources` | `:179,568,940` | `:537` | `:517-541` | [I] Re-analysing the same video does a `batch.set` overwrite that changes `createdAt`, `title`, `videoUrl` etc. The update rule requires `createdAt` unchanged and a restricted key set, so **re-analysis is denied** |
| `users/{uid}/extractedTechniques` | `:192,539,964` | `:538` | `:543-562` (`update: false`) | [I] Re-analysis overwrites `{vid}_t{idx}`, which is an update, so it is denied |
| `users/{uid}/conversationSessions` | R `:205` (observer has 0 callers) / W `:920` | `:536` | `:496-515` | OK |
| `users/{uid}/realWorldTasks` | `:219,1000` | `:539` | `:564-583` | OK |
| `users/{uid}/activityProgress` | `ActivityFirestoreRepository.kt:68,137` | `:529` | `:585-620` | OK |
| `users/{uid}/activityAttempts` | `:83,181` (limit 200) | `:530` | `:622-646` | OK |
| `users/{uid}/savedActivities` | `:100,225` | `:531` | `:648-669` | OK |
| `users/{uid}/dailyPracticePlans` | `:117,255` | `:532` | `:671-701` | OK |
| `users/{uid}/notifications`, `learningEvents`, `vocabularyProgress`, `quizAttempts`, `learningSnapshots` | **only in the account-deletion list** (`SpeakCraftRepository.kt:1298-1299`); the data actually lives in local JSON | **absent** | `:704-753` (weak validation) | Rules and code drift. These are dead cloud paths |
| `adminUsers/{uid}` | R `:256,332` / W `:1276` (role writer has 0 callers) | `:540` | `:758-766` | — |
| `adminAnnouncements` | R `:278` / W `:1232` (0 callers) | **absent** | `:768-775` | Missing from blueprint |
| `adminAuditLogs` | R `:305` (0 callers) / W `:1255` (callers are dead) | **absent** | `:777-784` | Missing from blueprint |
| `adminSettings` | — | absent | `:786-790` | Rules only |
| `youtubeChannels` | — | `:541` | `:792-795` | Unused by code |
| `youtubeVideos` | W `:1071` | `:542` | `:797-800` | OK |
| `videoTranscripts` | W `:1091` | `:543` | `:802-805` | OK |
| `extractedTechniques` (shared) | R `:345,430` / W `:1106,1140` | `:544` | `:807-819` | [I] Admin re-sync of the same video overwrites `createdAt`, so it is denied |
| `learningModules` | — | `:545` | `:821-824` | Unused by code |
| `lessons` | R `:359` / W `:1151` | `:546` | `:826-829` | No schema validation |
| `communicationExercises` | — | `:547` | `:831-834` | Unused by code |
| `learningChallenges` | R `:373` / W `:1174` | `:548` | `:836-839` | No schema validation |
| `applicationReleases` | R `:386,397,416` / W `:1191` and the CI script | `:549` | `:841-853` | No `hasOnly` (CI adds `downloadCount`) |
| `applicationUpdateEvents` | R `:444,458` / W `:1045` | `:550` | `:855-863` | User cannot delete; not covered by account deletion |

### 3.3 Listener usage and cost risk
- **16 snapshot listeners start as soon as the signed-in shell composes** [V] (`pkg/ui/AppNavigation.kt:405-426`, backed by `MainCoachViewModel.kt:101-151` and `UpdateAndAdminViewModel.kt:71,87`). Of these, 10 user collections have **no `limit()`**: speakingSessions, speechFeedback, vocabulary, dailyActivities, learningProgress, enrollments, realWorldTasks, youtubeSources, extractedTechniques, userUpdateEvents. Shared extractedTechniques, lessons and learningChallenges are also unbounded.
- 4 more listeners start in `ActivityEngineViewModel` (`pkg/ui/viewmodel/ActivityEngineViewModel.kt:128,146,158,170`). Only `activityAttempts` is limited (200).
- [I] Read cost on each cold start scales linearly with each user's history, because listeners re-read the full result set once the resume window has expired. `applicationUpdateEvents` grows with every manual update check (`UpdateAndAdminViewModel.kt:209-215`) and cannot be deleted.
- Admin listeners (`observeAdminReleases`, `observePendingModerationTechniques`, `observeAllUpdateEventsForAdmin`) are only collected after the client-side admin gate (`pkg/ui/screens/AdminDashboardScreen.kt:71-96`), so no cost is incurred for normal users [V].

---

## 4. Firestore rules (`firestore.rules`, 865 lines)

### 4.1 Per-user isolation (quality: good)
- Global default deny (`:5-7`). Every user subcollection gates on `isOwner(userId)` (`:13-15`), `isValidId` (`:48-50`), `hasAll`/`hasOnly` key allow-lists, `createdAt`/`updatedAt` as timestamps `<= request.time`, immutable `userId` and `createdAt`, and `diff().affectedKeys().hasOnly(...)` on update [V] (`:336-362`, for example).
- Immutable audit-style collections use `update: false`: speakingSessions `:472`, speechFeedback `:492`, user extractedTechniques `:560`, activityAttempts `:644`, learningEvents `:722`, quizAttempts `:744`.

### 4.2 Validation functions
`isValidUserProfile` `:72-92`, `isValidEnrollment` `:94-102`, `isValidDailyActivity` `:104-111`, `isValidLearningProgress` `:113-121`, `isValidActivityProgress` `:123-135`, `isValidActivityAttempt` `:137-158`, `isValidSavedActivity` `:160-166`, `isValidDailyPracticePlan` `:168-176`, `isValidVocabularyEntry` `:178-194`, `isValidSpeakingSession` `:196-227`, `isValidSpeechFeedback` `:229-245`, `isValidConversationSession` `:247-254`, `isValidYouTubeSource` `:256-272`, `isValidExtractedTechnique` `:274-292`, `isValidRealWorldTask` `:294-301`, `isValidRelease` `:303-320`, `isValidUpdateEvent` `:322-329`.

Weaknesses [V]:
- `isBoundedStringList` checks the type and length of **only elements 0-2** (`:64-70`). Elements from index 3 onward are unchecked, up to the 1 MiB document limit.
- `notifications`, `learningEvents`, `vocabularyProgress`, `quizAttempts` and `learningSnapshots` have **no `hasOnly`**. `learningSnapshots` validates only `userId` (`:751`).
- `lessons`, `learningChallenges`, `youtubeVideos`, `videoTranscripts`, `learningModules`, `communicationExercises` and `youtubeChannels` accept **any payload** from any admin (`:794-839`).
- `applicationReleases` has no `hasOnly`, and `signingCertSha256` is optional (`:316`).
- No document-count or rate limits. Gamification counters such as `streakDays` and `totalPracticeMinutes`, and all scores, are fully client-writable (`:356-362`). Users can set their own scores and streaks.

### 4.3 Admin model
- **Two sources, OR-ed** [V]: (a) custom claims `token.admin == true` or `token.role in [SUPER_ADMIN, CONTENT_ADMIN, MODERATOR]` (`:17-22`); (b) a Firestore document `adminUsers/{uid}` with `active == true` (`:24-28`). `isAdmin() = claim || doc` (`:30-32`). There is no hardcoded email or UID.
- Super admin is `token.role == 'SUPER_ADMIN'`, or a doc role in `['SUPER_ADMIN','ADMIN']` (`:34-39`). Content admin is defined at `:41-46`.
- **Bootstrapping**: `adminUsers` create/update requires `isSuperAdmin()` (`:761-764`). There are no Cloud Functions or Admin SDK claim tooling in the repo, so the first admin must be provisioned through the console or Admin SDK [I].
- **Client vs rules mismatches** [V]:
  - The client maps a bare `admin: true` claim to `SUPER_ADMIN` (`SpeakCraftRepository.kt:250`). The rules treat that claim as `isAdmin()` but **not** `isSuperAdmin()` or `isContentAdmin()`.
  - The client defaults a doc with no `role` to `SUPER_ADMIN` (`:261`). The rules require an explicit role.
  - The `adminUsers` write rule only allows `['SUPER_ADMIN','CONTENT_ADMIN','MODERATOR']` (`:763`), while `isSuperAdmin` accepts the legacy `'ADMIN'`. The rules test seeds `role: "ADMIN"` (`firestore.test.js:152`).
- **Broader access than needed** [V]:
  - Any `isAdmin()`, **including MODERATOR**, can create, update and delete `applicationReleases` (`:843-852`). That controls the OTA download URL, SHA and mandatory flag shown to every user.
  - The same applies to lessons, challenges and the video catalog (`:794-839`).
  - Admins can `get` and `list` **all user profiles** (`:333-334`). The comment at `:757` says admins have "ZERO access" to sessions, which is true, but profiles (display name, scores, goals) are exposed.
- Client-side gate: `AdminDashboardScreen` returns early when `!isAdmin` (`pkg/ui/screens/AdminDashboardScreen.kt:71-91`). The top-bar entry shows only for admins (`pkg/ui/AppNavigation.kt:793-794`). The server-side rules are the real enforcement.

### 4.4 Code-vs-rules breakages [V by static comparison]
1. **speakingSessions create**: the code payload (`SpeakCraftRepository.kt:798-836`) contains `evaluationStatus` (`:829`), `isPartialEvaluation` (`:830`), `evaluatedDimensionKeys` (`:831`), `unavailableDimensionKeys` (`:832`) and `evidenceSummary` (`:833`). None of these is in `hasOnly` at `firestore.rules:459-467`. Also, `confidenceLevel` may be `"Low (Partial)"` (code `:820`), but the rules allow only `['High','Medium','Low']` (`:218`).
   - Effect: PERMISSION_DENIED on every save, from `pkg/ui/viewmodel/SpeakingAndLabsViewModel.kt:535,575`. The batch also drops the speechFeedback document and skips the profile score update.
   - `git log -S` shows these fields were introduced in HEAD `68a9cb1`. `firestore.rules` was last changed in `4d6b917`.
2. [I] Re-analysing a YouTube video (`saveYouTubeAnalysis`, `SpeakCraftRepository.kt:934-991`) and admin re-sync (`:1062-1134`) both use `set` on existing documents. That violates the `createdAt`-immutability and `update: false` rules.
3. [I] Writes with blank required strings (for example a vocabulary `meaning` of `""`, or an activity attempt with empty `evaluatorName`) are denied, because the client does not pre-validate `size() >= 1`.

### 4.5 `firestore.test.js` coverage [V]
- 4 `test()` cases (`firestore.test.js:44,63,142,275`), using `@firebase/rules-unit-testing` against an emulator on `127.0.0.1:8085`.
- Covered: unauthenticated deny; profile create; learningProgress and speechFeedback create; cross-user read deny; ghost-field update deny; admin RBAC (self-promotion deny, release publish by a doc admin, non-HTTPS release deny, PUBLISHED-only technique reads, update-event spoof deny); activityProgress, activityAttempts, savedActivities and dailyPracticePlans create, update, immutability and isolation.
- **Not covered**: speakingSessions (the broken path), vocabulary, dailyActivities, enrollments, conversationSessions, youtubeSources, realWorldTasks, lessons, challenges, announcements, audit logs, claim-based admins, MODERATOR privileges, account-deletion deletes.
- **Not runnable as committed**: there is no `package.json` declaring `@firebase/rules-unit-testing` and it is not run in CI. It has never been executed in this audit.

---

## 5. User data rights

| Right | Status | Evidence |
|---|---|---|
| Data export | **Absent.** No export, share or "download my data" path. FileProvider is only used for APK install | [V] grep; `ReleaseUpdateRepository.kt:318` is the only `getUriForFile` |
| Account and data deletion | **DEAD** (implemented, unreachable). It also misses `applicationUpdateEvents` (top-level, rules forbid delete `:862`), local DataStore, `learning_state/`, `notification_store/`, `mentor_history_{uid}.json`, `backups/`, `updates/`, and recordings not referenced by a Firestore document. Subcollection errors are swallowed (`runCatching ... ?: continue`, `SpeakCraftRepository.kt:1302`) | [V] `SpeakCraftRepository.kt:1292-1319`; `MainCoachViewModel.kt:474` (0 callers) |
| Clear speaking history | Implemented and wired (Settings, behind a confirmation dialog per the test). Deletes session and feedback docs plus the local files they reference | [V] `SpeakCraftRepository.kt:1023-1036`; `AppNavigation.kt:1142` |
| Recording retention | Recordings are saved to `filesDir/recordings/speakcraft_rec_<ts>.m4a` and `speakcraft_vid_<ts>.mp4`, and are **never uploaded** (Firestore stores only `localMediaPath`). There is no TTL or auto-cleanup. [I] Because session saves are denied (§4.4), new recordings are orphaned and no deletion path can ever reach them | [V] `pkg/data/media/AudioRecorderManager.kt:57,62`; `pkg/data/media/CameraVideoRecorder.kt:201-202` |
| Local cleanup on sign-out | **None.** `signOut` only clears credentials and Auth (`AuthHelper.kt:126-143`). `LearningProgressManager.wipeAllLocalDataForUser` (`pkg/data/learning/LearningProgressManager.kt:697`) and `NotificationManagerRepository.clearAllAlerts` (`pkg/data/notifications/NotificationManagerRepository.kt:335`) have **0 callers**. Recordings, backups and updates are not partitioned per user | [V] |
| Local stores (5.3) | DataStore `speakcraft_prefs` (theme, update channel, GitHub repo, AI toggles, **encrypted YouTube API key**), DataStore `speakcraft_tutorial_state`, `filesDir/learning_state/learning_snapshot_<uid>.json`, `filesDir/notification_store/alerts_<uid>.json`, `filesDir/mentor_history_<uid>.json`, `filesDir/recordings/`, `filesDir/backups/`, `filesDir/updates/*.apk`, `filesDir/ai_models/`, Firestore SDK cache | [V] `AppPreferencesRepository.kt:22,26-50`; `TutorialState.kt:11`; `LearningProgressManager.kt:50,889-892`; `NotificationManagerRepository.kt:36,439-442`; `pkg/ui/viewmodel/InteractiveCoachViewModel.kt:156-160`; `UpdateBackupManager.kt:30`; `ReleaseUpdateRepository.kt:206`; `pkg/data/ai/local/OnDeviceModelManager.kt:153` |
| Backup to cloud | `allowBackup=true` with template rules (§6), so all of the above (except cache) is eligible for Google backup and device transfer | [V] |

---

## 6. Security review

### 6.1 Secrets and keys in the repo
| Item | Finding | Evidence |
|---|---|---|
| reCAPTCHA site key | `6LdE1CUuAAAAABICUa9hPzdhZ8jRz4Suo3oskJFQ` committed. Site keys are public by design; Low | [V] `res/values/firebase_applet_config.xml:4` |
| Firestore database id / GCP project id | Committed (identifiers, not secrets); Low | [V] `firebase_applet_config.xml:3`; `publish_firestore_release.test.mjs:11` |
| `google-services.json` | **Not present** on disk or in git history. Not explicitly listed in `.gitignore`; CI writes it to `app/` and deletes it at the end | [V] `find`; `git log --all --name-only`; `release_on_push.yml:80,512` |
| Keystores | `debug.keystore` and `my-upload-key.jks` are **absent** on disk and in git history. `.gitignore` covers `debug.keystore` and `*.jks` | [V] `.gitignore:18-19`; history scan |
| Secret patterns | No `AIza…`, private keys, `ghp_` or `"private_key"` in tracked files | [V] `git grep` |
| `.env` | Ignored (`.gitignore:17`). `.env.example` has no keys | [V] |
| Repo hygiene | `public/assets/aistudio/` contains a 29 KB prompt paste and a 2.1 MB screenshot, both tracked (not sensitive) | [V] |

### 6.2 Signing
- Release: `KEYSTORE_PATH` env or `rootProject/my-upload-key.jks`, alias `upload`. Passwords come from env, **defaulting to `""`** (`app/build.gradle.kts:49-55`).
- Debug: `rootProject/debug.keystore` with the hardcoded standard passwords `android`/`android` (`:56-61`). The debug build type uses it (`:70-72`).
- CI chooses between them (§8). The **beta channel defaults to debug signing** whenever `ANDROID_KEYSTORE_BASE64` is unset (`release_on_push.yml:257-262,297-328`). The docs actively recommend staying on debug signing (`docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:20,35`).

### 6.3 App Check (read carefully)
`configureAppCheck(context, intent)` is called from `MainActivity.onCreate` (`pkg/MainActivity.kt:15`).
- `if (BuildConfig.DEBUG)` (`pkg/data/firebase/AppCheckConfig.kt:24`): the value of the launch-intent extra `FIREBASE_APPCHECK_DEBUG_TOKEN` (`:13,25`) is **written synchronously into the debug provider's SharedPreferences** as `DEBUG_SECRET` (`:26-36`). Then `DebugAppCheckProviderFactory` is installed (`:41-44`).
- `else`: `RecaptchaAppCheckProviderFactory` is installed with `recaptcha_site_key` (`:45-60`). If the key is missing it **logs and continues with no provider** (`:54-59`), and the outer exception is swallowed (`:61-63`).
- **Can a launch intent enable a debug token?** Only in builds where `BuildConfig.DEBUG == true`. **But CI's beta channel ships debug builds** (`release_on_push.yml:324`). `MainActivity` is `exported="true"` (`AndroidManifest.xml:28`). So in distributed beta APKs, any app on the device can launch `MainActivity` with that extra and overwrite the stored debug secret [V code path; I exploitability]. That allows App Check denial-of-service, or installing a known secret if one is allow-listed in the console.
  - [I] On first run the debug provider also generates and logs a random secret. Unless each beta user's secret is allow-listed, App Check tokens are invalid. Either enforcement is off, in which case App Check protects nothing, or it is on, in which case beta users' Firestore and AI calls fail.
- Release provider: [UNKNOWN] whether `firebase-appcheck-recaptcha` and `RecaptchaAppCheckProviderFactory` exist as an Android artifact in BoM 34.17.0 could not be verified offline. The standard Android attestation provider is Play Integrity, which is not used.
- `firebase-appcheck-debug` is an `implementation` dependency, so it ships in release (`app/build.gradle.kts:139`).

### 6.4 Backup and FileProvider
- `android:allowBackup="true"`, `fullBackupContent=@xml/backup_rules`, `dataExtractionRules=@xml/data_extraction_rules` (`AndroidManifest.xml:18-20`). Both XML files are **untouched Android Studio templates** with all include/exclude commented out (`res/xml/backup_rules.xml:8-13`, `res/xml/data_extraction_rules.xml:6-19`).
  - [I] Everything in filesDir, DataStore and the databases is backed up: voice and video recordings, mentor chat, learning snapshots, Firestore cache, APKs.
  - [I] The Keystore-encrypted YouTube key will restore as undecryptable, because the AES key does not leave the device.
  - [I] Recordings can exceed the 25 MB backup quota, which would make the whole backup fail.
- FileProvider: `exported=false`, `grantUriPermissions=true` (`AndroidManifest.xml:38-46`). Paths are `files/updates/`, `files/backups/`, `files/recordings/` and **`cache-path path="/"` (entire cache dir)** (`res/xml/file_paths.xml:3-6`). Only `updates/` is actually used (`ReleaseUpdateRepository.kt:318-322`). The other paths are over-broad; Low.

### 6.5 Logging of PII
- **`handleFirestoreError` builds JSON containing `userId`, `email`, `emailVerified`, `tenantId` and each provider's `email`, and logs it with `Log.e` in all builds** (`pkg/data/firebase/FirestoreErrorHandler.kt:26-51`). It runs on every mutation failure (`SpeakCraftRepository.kt:1333,1348`; `ActivityFirestoreRepository.kt:174,212,245,285`) and on stream errors (`SpeakCraftRepository.kt:61-76`). [I] With §4.4, every speaking-session save logs the user's email.
- Other logs include file paths, YouTube URLs (`pkg/data/youtube/YouTubeIntelligenceClient.kt:136`) and notification titles (`NotificationManagerRepository.kt:151`).
- 84 `Log.*` calls in total, no `println`. Nothing strips logs in release (no R8, no `-assumenosideeffects`).

### 6.6 Build hardening
- `isMinifyEnabled = false` for release (`app/build.gradle.kts:66`). No shrinking or obfuscation; `material-icons-extended` and all ML libraries ship unshrunk. `proguard-rules.pro` is the empty template (`app/proguard-rules.pro:1-21`). [I] Enabling R8 later will need keep rules for Firestore `toObject` model classes (`pkg/data/model/FirestoreModels.kt`) and kotlinx-serialization.
- No `network_security_config` [V]. [I] The default for targetSdk >= 28 blocks cleartext, which is acceptable. All endpoints are HTTPS (§7).
- The cloud-AI blocking interceptor `LocalExecutionPolicy.aiEndpointBlockingInterceptor` (`pkg/data/ai/local/LocalExecutionPolicy.kt:35-48`) is **never attached to any production OkHttpClient**. It is only used in `LocalAIFoundationTest.kt:37` and `ZeroCloudAiNetworkBoundaryTest.kt:39`. The "blocked at the network layer" claim (`:9`) is false for production; this is an assurance gap.
- On-device model catalogue: download URLs for Hugging Face and GCS models with **`sha256Hash = ""` for all 5** (`OnDeviceModelManager.kt:83-139`). The integrity check returns `true` when the hash is blank (`:191`). No downloader exists in that file (`grep fun download` finds only `downloadSize`). This is a PLACEHOLDER.
- YouTube API key: user-entered, encrypted with Android Keystore AES-GCM (`AppPreferencesRepository.kt:51-110,135,182`). Good. It is sent as a URL query parameter (`YouTubeIntelligenceClient.kt:310`), which is standard for that API.

### 6.7 Exported components and permissions
- Exported: only `MainActivity` (launcher, `AndroidManifest.xml:26-36`). There is no `onNewIntent` handling.
- Declared permissions (`AndroidManifest.xml:5-8`):

| Permission | Declared | Used | Evidence |
|---|---|---|---|
| INTERNET | yes | yes (Firestore, GitHub API, YouTube, APK download) | §7 |
| ACCESS_NETWORK_STATE | yes | **not used by app code** (0 `ConnectivityManager`) [I] Firebase may use it | grep |
| RECORD_AUDIO | yes | runtime-requested (33 refs) | grep `Manifest.permission` |
| CAMERA | yes | runtime-requested (8 refs) | grep |
| POST_NOTIFICATIONS | no | **not needed**: no system notifications are posted (the notification centre is in-app only) | grep `NotificationCompat` returns 0 |
| REQUEST_INSTALL_PACKAGES | **no** | **needed** for `ACTION_VIEW` with `application/vnd.android.package-archive` on API 26+ (`ReleaseUpdateRepository.kt:323-329`). [I] Without it the system installer refuses, and `canRequestPackageInstalls()` is never checked | grep returns 0 |
| FOREGROUND_SERVICE(_MICROPHONE/_CAMERA) | no | no services exist | §11 |

- `<queries>` is absent. [I] `installIntent.resolveActivity(...)` (`ReleaseUpdateRepository.kt:327`) may return null on API 30+ due to package-visibility rules, which would silently fall back to the browser.

---

## 7. In-app update mechanism

**Sources.**
1. Firestore `applicationReleases where enabled == true` (`SpeakCraftRepository.kt:380-408`).
2. GitHub Releases API `https://api.github.com/repos/{slug}/releases?per_page=15`, unauthenticated, 60 requests per hour per IP [I] (`GitHubReleaseClient.kt:18-52`).

These are merged and de-duplicated in `ReleaseUpdateRepository.kt:155-168`. The repo slug comes from `BuildConfig.GITHUB_RELEASE_REPO`, which CI injects (`app/build.gradle.kts:41-45`; `release_on_push.yml:352`). **The user can override it through a free-text field** (`pkg/ui/screens/AppUpdateScreen.kt:265,314-324` leading to `UpdateAndAdminViewModel.kt:150-160`; `AppPreferencesRepository.kt:136-139,237`). A silent refresh runs on shell start (`AppNavigation.kt:448-451`).

**GitHub parsing.** The first `.apk` asset and the `.sha256` asset are taken. `VersionName`, `VersionCode`, `SHA256`, `SigningCertSHA256`, `MinSupportedVersion`, `Channel` and `Mandatory` are parsed by regex from the **release body** (`GitHubReleaseClient.kt:107-129,158-196`). The SHA is backfilled from the sidecar asset (`:38-50,54-64`). Releases without a 64-hex SHA or without an HTTPS URL are dropped (`:50`).

**Evaluation.** Filters are channel (beta sees all, stable sees only stable), `enabled` and `minSdk`. The newest release is chosen by SemVer, then versionCode. An update is offered if the SemVer is newer **or** the versionCode is higher (`ReleaseUpdateRepository.kt:65-153`). It becomes mandatory if the `mandatory` flag is set or the current version is below `minSupportedVersion` (`:113-116`). SemVer handles numeric pre-releases (`pkg/data/update/SemVer.kt:10-36`).

**Download and verification** (`ReleaseUpdateRepository.kt:170-311`):
1. Require HTTPS (`:175-180`).
2. Pre-flight check of the advertised cert against the installed cert (`:184-196`).
3. Create a pre-update backup (`:199-204`).
4. Stream to `files/updates/<name>.part` (`:206-244`).
5. **SHA-256 must equal the advertised SHA**, compared in constant time (`:247-252`; `ApkIntegrityVerifier.kt:43-50,203-210`).
6. Read the APK's signing certs via `getPackageArchiveInfo` and compare them with the advertised cert (`MismatchWithExpected`) and the installed cert (`MismatchWithInstalled`) (`:260-303`; `ApkIntegrityVerifier.kt:71-112,137-163`).
7. If certs are `Unknown` and no cert was advertised, the APK is still marked ready with `signatureVerified=false` (`:290-301`).

**What is actually trusted.** The SHA-256 and the expected cert come from **the same origin as the APK** (the release body or sidecar, or the admin-writable Firestore document). They protect against corruption, not against a compromised source [I]. The real protections are Android's same-signer requirement for in-place updates plus the installed-cert comparison. The weak point is the signing key itself. When CI uses the debug keystore, anyone holding the `DEBUG_KEYSTORE_BASE64` secret can sign APKs that pass every check.

**Install.** An `ACTION_VIEW` intent with a FileProvider URI and `FLAG_GRANT_READ_URI_PERMISSION` (`:313-331`). If no activity resolves, it falls back to the release page or the Play Store URL (`:332-338`). A "direct APK download link" opens in the browser (`:341-349`; `pkg/ui/components/UpdateAvailableDialog.kt:250-265`). The signing-conflict message instructs the user to "download the APK via browser, uninstall the old build once, and install the new release" (`:356-362`). Combined with a user-editable repo, this is a social-engineering vector [I].

**Backup and restore** (`pkg/data/update/UpdateBackupManager.kt`).
- The JSON snapshot stores only `themeMode`, `ttsRate`, `updateChannel`, version info and the **names** of recording files (`:33-75`). It does not store recordings, learning state or Firestore data.
- Restore returns those three preferences (`:98-116`).
- A recovery marker detects interrupted updates (`:57-65,118-132`).
- Classification: PARTIALLY_IMPLEMENTED; the name "backup" overstates it.

**Firestore publishing script** (`scripts/publish_firestore_release.mjs`).
- Validates the service account against the expected project (`:38-71`). Validates SemVer, versionCode, HTTPS URL, SHA, cert and size (`:73-121`).
- Writes `applicationReleases/rel_<ver>_<code>` through firebase-admin, bypassing the rules (`:154-196`), into the named database read from the XML (`:16-21`).
- `features` and `securityImprovements` are **hardcoded marketing strings**, not real release notes (`:130-137`).
- 3 node tests (`scripts/publish_firestore_release.test.mjs:9,19,48`) assert hardcoded project and database ids.

**Risk and Play policy.**
- Google Play's Device and Network Abuse policy forbids apps distributed on Play from updating themselves outside Play or downloading executable code (APKs).
- `REQUEST_INSTALL_PACKAGES` is a restricted permission on Play.
- Play also rejects debuggable artifacts and expects AAB uploads. CI builds APKs only (`release_on_push.yml:292-293,324-325`).
- [I] For a Play release, the updater, the GitHub client, the direct-APK link and the FileProvider `updates` path must be removed or flavor-gated, and replaced with Play In-App Updates.

---

## 8. CI/CD (`.github/workflows/release_on_push.yml`, 512 lines)

The workflow has one job, `build-and-publish-update`, on `ubuntu-latest` (`:47-49`).

| Step | What it does | Evidence |
|---|---|---|
| Triggers | Push to `main`, `master` or `beta`; tags `v*.*.*`; `workflow_dispatch` with inputs channel, version_name, mandatory, signing_mode (auto/release/debug) | `:3-38` |
| Permissions / concurrency | `contents: write`; group `ota-release-${ref}`, no cancel | `:40-45` |
| env | `FIREBASE_PUBLISH_ENABLED = secrets.FIREBASE_SERVICE_ACCOUNT_JSON != ''` | `:50-51` |
| 1 Checkout | `actions/checkout@v4`, depth 2 | `:54-57` |
| 2 JDK | Temurin 21 | `:59-63` |
| 3 Android SDK | `android-actions/setup-android@v3`, platform 36, build-tools 36.0.0 | `:65-68` |
| 4 google-services.json | From secret `GOOGLE_SERVICES_JSON` (or legacy `GOOGLESERVICES`). Writes `app/google-services.json` (chmod 600). A Python check verifies the package client and the Web OAuth client (type 3) | `:70-104` |
| 5 Gradle | `gradle/actions/setup-gradle@v4` with Gradle 9.3.1 (no wrapper in repo) | `:106-109` |
| 6 Node 20 | `actions/setup-node@v4` | `:111-114` |
| 7 Versioning | `VERSION_CODE = 2 + GITHUB_RUN_NUMBER`. Branch push gives beta `1.1.0-beta.(1+run)`. Tag gives stable `X.Y.Z` (or beta if it has a suffix). Dispatch uses the input or `1.1.<run>` / `1.1.0-beta.<1+run>`. SemVer regex check; a stable version may not have a pre-release suffix. Fails if a GitHub release for the tag already exists. Commit message is sanitised and truncated to 180 chars | `:116-228` |
| 8 Signing | `stable` or `release` mode requires the `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD` and `KEY_PASSWORD` secrets. It decodes `my-upload-key.jks`, validates alias `upload` with keytool, captures the cert SHA-256, and builds `assembleRelease`. **Otherwise (beta + auto with no keystore, or debug mode) it decodes the `DEBUG_KEYSTORE_BASE64` secret into `debug.keystore`, validates `androiddebugkey`/`android`, and builds `assembleDebug`.** Debug-signed stable builds are refused. **No keystore is generated** | `:230-329` |
| 9 Build | `gradle <task> -PversionCodeOverride -PversionNameOverride -PbuildChannelOverride -PgithubReleaseRepoOverride --no-daemon` | `:331-353` |
| 10 Verify | Copies the APK to `build-release-artifacts/speakcraft_<ver>.apk`, computes SHA-256 and the `.sha256` file, runs `apksigner verify --print-certs`, and asserts the cert matches the keystore cert. Writes a step summary | `:355-437` |
| 11 Release notes | A heredoc with `VersionCode`, `VersionName`, `Channel`, `MinSupportedVersion: 1.0.0`, `Mandatory`, `SHA256`, `SigningCertSHA256` (the format the in-app parser reads) | `:439-466` |
| 12 GitHub Release | `softprops/action-gh-release@v2`. Prerelease for beta; `make_latest` only for stable; uploads APK and .sha256 | `:468-478` |
| 13 Firestore publish | If the service account is set: `node --test scripts/publish_firestore_release.test.mjs`, then **`npm install firebase-admin@^13.0.0 --no-package-lock`** (unpinned), then `node scripts/publish_firestore_release.mjs` | `:480-501` |
| 14 Warn | A warning if the service account is missing | `:503-506` |
| 15 Cleanup | `always()`: removes keystores and `google-services.json` | `:508-512` |

**Secrets used:** `GOOGLE_SERVICES_JSON` or `GOOGLESERVICES`, `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `DEBUG_KEYSTORE_BASE64`, `FIREBASE_SERVICE_ACCOUNT_JSON`, and `github.token`.

**Quality gates present:** google-services sanity check, SemVer and channel validation, duplicate-tag guard, keystore and alias validation, apksigner cert-match, SHA computation, and node tests for the publish script.

**Missing gates** [V, all absent from the workflow]:
- No `gradle test` or `testDebugUnitTest` (the 161 unit tests never run in CI).
- No `lint`.
- No `firestore.test.js` or emulator run.
- No `firebase deploy --only firestore:rules`, so **the deployed rules may differ from the repo** [UNKNOWN].
- No PR or branch protection workflow; every push to main publishes a release.
- No AAB, no R8, no dependency or secret scanning, no SBOM, no Gradle wrapper validation.
- No staged rollout or kill switch, other than toggling `enabled` in Firestore.

**Supply-chain notes** [V]: all actions are pinned to mutable major tags (`@v4`, `@v3`, `@v2`), not SHAs. `firebase-admin@^13.0.0` is installed without a lockfile while `FIREBASE_SERVICE_ACCOUNT_JSON` is in the step env. Workflow inputs are passed through `env:` rather than inline `${{ }}` in scripts, which is good injection hygiene.

---

## 9. Tests

Nothing was executed; there is no JDK or SDK here. **No result is recorded as passed.**

Totals [V]: **161 `@Test` in `app/src/test` (24 files, 5,325 lines) + 1 in `app/src/androidTest` = 162.** Plus 4 rules tests (`firestore.test.js`) and 3 node tests (`scripts/publish_firestore_release.test.mjs`). There are no mocking libraries (no MockK or Mockito). `isReturnDefaultValues = true` (`app/build.gradle.kts:88`) makes Android stubs return defaults, so some tests can pass vacuously [I].

| File | @Test | Runner | Covers | Nature |
|---|---|---|---|---|
| ActivityEngineComposeTest.kt | 4 | Robolectric | Universal activity screen flows, discovery tabs | Real UI behaviour (Robolectric) |
| ActivityEngineUnitTest.kt | 9 | JVM | Catalogue counts (10 modules, 150 activities), validators, search, evaluators, daily plans, paths, analytics | Mix of structural count checks and real engine logic |
| CentralizedAiSettingsTest.kt | 10 | Robolectric | AI settings persistence, zero-cloud policy, capability matrix, screens without cloud toggles | Real DataStore and policy. Builds `SpeakCraftRepository(FirebaseFirestore.getInstance())` after `FirebaseApp.initializeApp` (`:52-58`); [I] fails without google-services values |
| CoachEvaluationEvidenceTest.kt | 15 | Robolectric | Evidence validation: empty, silent or corrupt audio; partial scoring; error copy | Real behaviour |
| CurriculumAndRecommendationTest.kt | 4 | JVM | Catalog modules, recommendation rules | Real but small |
| ExampleRobolectricTest.kt | 1 | Robolectric | Reads `app_name` | **Trivial smoke** |
| ExampleUnitTest.kt | 1 | JVM | `2 + 2 == 4` | **Trivial** |
| HybridAiCoachCoordinatorTest.kt | 4 | Robolectric | Cloud blocked, local routing, model tier map | Real policy routing |
| HybridAiComposeUiTest.kt | 5 | Robolectric | Screens without scattered cloud toggles | UI presence and absence assertions (smoke) |
| LightweightFeedbackEngineTest.kt | 11 | JVM | Grammar, fluency, vocabulary, pronunciation, confidence, structure engines | Real heuristic logic |
| LiveTranscriptionManagerTest.kt | 14 | Robolectric | Transcription state machine, permissions, errors, session isolation | Real (with fakes) |
| LocalAIFoundationTest.kt | 5 | JVM | Local policy, blocked domains, model catalogue, device tiers | Largely structural. Tests an interceptor that is not wired in production |
| LocalAudioPipelineTest.kt | 3 | JVM | Filler detection, pronunciation, RMS/VAD/clipping | Real DSP logic |
| LocalLanguageAndOrchestratorTest.kt | 4 | JVM | Grammar, templates, fusion, offline orchestrator | Real |
| NotificationManagementSystemTest.kt | 4 | Robolectric | Alert store dedupe/pin/clear, update-payload block, drawer UI | Real |
| SemVerAndUpdateTest.kt | 8 | Robolectric | SemVer ordering, SHA verify/tamper, backup marker, channel evaluation, non-HTTPS rejection, GitHub body parsing and merge, cert compatibility | **Real pure logic.** Gaps: no download happy path, no install intent, no `getPackageArchiveInfo` path. One test hard-asserts `BuildConfig.VERSION_CODE == 2`, `VERSION_NAME == "1.1.0-beta.1"` and `BUILD_CHANNEL == "beta"` (`:146-148`), which would break under CI overrides |
| SpeakCraftComposeCujTest.kt | 20 | Robolectric | Dashboard, feedback card, lesson, admin gate, update screen, onboarding, settings, analytics, mentor, tutorials, update dialog, profile | UI rendering with stub data (CUJ smoke). Not end-to-end with Firebase |
| SpeakCraftLaunchAnimationTest.kt | 8 | Robolectric | Launch timeline, cadence, reduced motion, sizes | Real for that feature |
| SpeakCraftTabletLayoutTest.kt | 7 | Robolectric | Tablet two-pane layouts | UI layout assertions |
| SpeechRecognitionManagerTest.kt | 4 | JVM | Fallback when no models or missing file | Smoke and negative paths |
| StreakAnalyticsAndBackupTest.kt | 7 | Robolectric | Streak math, minutes delta, period filter, backup create/restore, tutorial registry (24), tutorial DataStore per uid | Real (streak, backup, per-user tutorial state) plus structural counts |
| UpgradedLocalIntelligenceTest.kt | 7 | JVM | Spoken-grammar rules, rewrites, conversation memory, YouTube drill extraction | Real heuristic logic |
| YouTubeParsingTest.kt | 3 | Robolectric | URL extractors, RSS and timedtext parsers | Real |
| ZeroCloudAiNetworkBoundaryTest.kt | 3 | JVM | Policy constant, interceptor in isolation, deprecated Gemini throws | **False assurance:** the interceptor is not attached to any production client |
| **androidTest/** ExampleInstrumentedTest.kt | 1 | AndroidJUnit4 | Package name equals APPLICATION_ID | **Trivial**. No instrumented coverage at all |

**Untested platform code** [V by grep]: `SpeakCraftRepository` (only constructed in tests, no method exercised against an emulator), `ActivityFirestoreRepository`, `AuthHelper`, `AppCheckConfig`, `FirestoreErrorHandler`, `GitHubReleaseClient.fetchReleases` (network), `ReleaseUpdateRepository.downloadAndVerifyRelease` (only the HTTP rejection), `launchVerifiedInstallerOrReleasePage`, `deleteCurrentUserAccountAndData`, and every rules path for speakingSessions.

[I] Local unit tests cannot compile without `app/google-services.json`, because `AuthHelper.kt:53` references `R.string.default_web_client_id`.

---

## 10. Observability

| Capability | Status | Evidence |
|---|---|---|
| Crash reporting | **Absent** (no Crashlytics or Sentry plugin or dependency) | [V] `app/build.gradle.kts:104-169`; grep |
| Analytics | **Absent** | [V] |
| Logging | `android.util.Log` only (84 calls). No framework, no release stripping, PII in Firestore error logs | [V] §6.5 |
| Remote Config / feature flags | **Absent.** Policy is compile-time (`LocalExecutionPolicy.POLICY`), and releases are toggled through the Firestore `enabled` flag | [V] `LocalExecutionPolicy.kt:13` |
| Performance monitoring | **Absent** | [V] |
| Server-side telemetry | Only `applicationUpdateEvents` (update check, download, install statuses), written by the client | [V] `SpeakCraftRepository.kt:1038-1058` |

---

## 11. Background processing and lifecycle

| Capability | Status | Evidence |
|---|---|---|
| Services / foreground services | **Absent.** None in the manifest; 0 `Service`/`startForeground` | [V] `AndroidManifest.xml`; grep |
| Recording in background | [I] Not supported. `MediaRecorder` and CameraX are bound to the UI and lifecycle (`CameraVideoRecorder` uses camera-lifecycle). Recording stops or breaks when the app is backgrounded; there is no foreground-service type `microphone`/`camera` | `AudioRecorderManager.kt:59-160`; `CameraVideoRecorder.kt` |
| WorkManager / JobScheduler / AlarmManager | **Absent** | [V] |
| Background update checks | None. Checks happen on shell start (silent GitHub refresh) and manually | [V] `AppNavigation.kt:448-451`; `UpdateAndAdminViewModel.kt:190-238` |
| Reminders / streak notifications | In-app notification store only. No scheduled or system notifications | [V] §6.7 |
| Process and config resilience | No `Application`. `AppContainer` is remembered in a composable, so [I] it is rebuilt on Activity recreation, along with stateful managers such as `AudioRecorderManager` and `LiveTranscriptionManager`. `configureAppCheck` reruns on each `onCreate` | [V] `AppNavigation.kt:216`; `MainActivity.kt:15` |

---

## 12. Architecture shape

- **Modularisation**: a single `:app` module (`settings.gradle.kts:27`). It has 106 main Kotlin files and 48,894 lines [V].
- **Packages and sizes** [V]:

| Package | Files | Lines |
|---|---|---|
| `data/ai` | 28 | 9,100 |
| `data/activity` | 10 | 5,770 |
| `data/learning` | 6 | 2,089 |
| `data/model` | 4 | 2,053 |
| `data/firebase` | 5 | 1,899 |
| `data/evaluation` | 3 | 1,099 |
| `data/update` | 5 | 995 |
| `data/media` | 3 | 778 |
| `data/notifications` | 2 | 658 |
| `data/youtube` | 1 | 519 |
| `data/preferences` | 3 | 516 |
| `ui/screens` | 15 | 10,724 |
| `ui/components` | 9 | 7,792 |
| `ui/viewmodel` | 5 | 2,681 |
| `ui/theme` | 4 | 523 |

- **Domain layer**: **none**. There are no use cases (0 "UseCase" hits) and **no repository, service or client interfaces**. The only interfaces are `LiveVoiceHandle` and the sealed `UpdateDownloadState`. ViewModels call concrete Firebase repositories directly. `SpeakCraftRepository` mixes user data, the shared catalog, releases, RBAC, admin mutations and account deletion in 1,351 lines.
- **DI**: manual `AppContainer` (§1.6). `UpdateAndAdminViewModel` keeps a `@Deprecated` secondary constructor and a self-constructed fallback coordinator (`pkg/ui/viewmodel/UpdateAndAdminViewModel.kt:42-67`).
- **Navigation**: one 1,548-line `AppNavigation.kt` holding the NavHost (`:906`), plus string routes.
- **15 largest Kotlin files** [V] (`wc -l`):

| # | File | Lines |
|---|---|---|
| 1 | `ui/components/CoachComponents.kt` | 1943 |
| 2 | `ui/AppNavigation.kt` | 1548 |
| 3 | `ui/components/DashboardStudioWidgets.kt` | 1543 |
| 4 | `data/activity/ActivityCatalogueModules7To10.kt` | 1496 |
| 5 | `data/firebase/SpeakCraftRepository.kt` | 1351 |
| 6 | `data/activity/ActivityCatalogueModules1To3.kt` | 1159 |
| 7 | `data/model/CurriculumCatalog.kt` | 1124 |
| 8 | `ui/screens/SpeakingAndChallengesScreens.kt` | 1114 |
| 9 | `data/activity/ActivityCatalogueModules4To6.kt` | 1109 |
| 10 | `ui/screens/AnalyticsAndSettingsScreens.kt` | 1089 |
| 11 | `ui/screens/DashboardScreen.kt` | 1038 |
| 12 | `ui/screens/LabsAndTrainingScreens.kt` | 1024 |
| 13 | `data/ai/local/LocalIntelligenceOrchestrator.kt` | 984 |
| 14 | `data/learning/LearningProgressManager.kt` | 920 |
| 15 | `ui/screens/ConversationAndMentorScreens.kt` | 878 |

---

## 13. Capability status and prioritised weaknesses

### 13.1 Capability classification
ABSENT is used where nothing exists at all; it is outside the requested taxonomy but is stated explicitly rather than forced into PLACEHOLDER.

| Capability | Status | Key evidence |
|---|---|---|
| Google Sign-In (Credential Manager plus Firebase Auth) | IMPLEMENTED (needs google-services.json at build) | `AuthHelper.kt:41-124` |
| Silent auto sign-in | IMPLEMENTED | `AuthHelper.kt:41-82` |
| Sign-out | PARTIALLY_IMPLEMENTED (no local cleanup) | `AuthHelper.kt:126-143` |
| Account deletion | DEAD (no UI caller, no re-auth) | `SpeakCraftRepository.kt:1292`; `MainCoachViewModel.kt:474` |
| Data export | ABSENT | grep |
| Local data wipe per user | DEAD | `LearningProgressManager.kt:697` |
| Firestore profile, vocabulary, daily, progress, enrollment and task sync | IMPLEMENTED | §3.2 |
| Speaking session persistence (sessions plus feedback plus score blend) | **BROKEN** (rules reject) | `SpeakCraftRepository.kt:829-833` vs `firestore.rules:459-467` |
| Activity Engine persistence | IMPLEMENTED | `ActivityFirestoreRepository.kt` |
| YouTube analysis save, first time | IMPLEMENTED | `SpeakCraftRepository.kt:934-991` |
| YouTube re-analysis and admin re-sync | BROKEN [I] | §4.4 |
| Offline persistence | IMPLEMENTED implicitly (SDK default); offline writes hang [I] | §3.1 |
| Firestore rules: per-user isolation | IMPLEMENTED | §4.1 |
| Firestore rules: shared catalog validation | PARTIALLY_IMPLEMENTED | §4.2 |
| Admin RBAC (claims OR doc) | PARTIALLY_IMPLEMENTED (role mapping mismatch, no provisioning tooling) | §4.3 |
| Admin console: moderation, lessons, challenges, release publishing, YouTube sync | IMPLEMENTED (client) | `UpdateAndAdminViewModel.kt:345-432` |
| Admin role management, announcement publishing, audit-log viewing and writing | DEAD (0 callers) | `SpeakCraftRepository.kt:1225-1285,299-337` |
| Announcement display | IMPLEMENTED | `MainCoachViewModel.kt:151` |
| App Check (debug builds and CI beta) | BROKEN or ineffective [I] | §6.3 |
| App Check (release, reCAPTCHA) | UNKNOWN | §6.3 |
| Firebase AI Logic (cloud) | DEAD (blocked by LOCAL_ONLY policy) | `AISettingsRepository.kt:24` |
| Cloud-AI network interceptor | DEAD (tests only) | `LocalExecutionPolicy.kt:35` |
| On-device model download and integrity | PLACEHOLDER | `OnDeviceModelManager.kt:83-139,191` |
| Update discovery (Firestore plus GitHub) | IMPLEMENTED | §7 |
| APK download plus SHA-256 plus cert verification | IMPLEMENTED | `ReleaseUpdateRepository.kt:170-311` |
| APK installation | BROKEN on API 26+ [I] (no `REQUEST_INSTALL_PACKAGES`) | `AndroidManifest.xml:5-8` |
| Pre-update backup and restore | PARTIALLY_IMPLEMENTED (preferences only) | `UpdateBackupManager.kt:33-116` |
| Interrupted-update detection | IMPLEMENTED | `UpdateBackupManager.kt:118-132` |
| CI build, sign, release, Firestore publish | IMPLEMENTED (no quality gates) | §8 |
| CI unit tests, lint, rules tests, rules deploy | ABSENT | §8 |
| Rules unit tests | PARTIALLY_IMPLEMENTED (4 tests, not runnable as committed) | §4.5 |
| Instrumented tests | PLACEHOLDER (1 trivial) | `ExampleInstrumentedTest.kt` |
| Backup and data-extraction rules | PLACEHOLDER (templates) | `res/xml/*.xml` |
| R8 / minification | ABSENT (disabled) | `app/build.gradle.kts:66` |
| Secrets plugin / KSP | DEAD | §1.4 |
| Crash reporting, analytics, Remote Config, logging framework | ABSENT | §10 |
| WorkManager, services, foreground recording, system notifications | ABSENT | §11 |
| YouTube API key at-rest encryption | IMPLEMENTED | `AppPreferencesRepository.kt:51-110` |

### 13.2 Prioritised security and privacy weaknesses

| ID | Severity | Weakness | Evidence | Recommendation (for redesign) |
|---|---|---|---|---|
| S1 | **Critical** (if no release keystore secret; [UNKNOWN] whether it is configured) | Beta OTA, triggered by every push to main, ships **debuggable debug builds signed with a debug keystore** that has a public password, and installs the **App Check Debug provider**. The exported launcher accepts a debug-secret intent extra in those builds. The OTA trust chain reduces to whoever holds `DEBUG_KEYSTORE_BASE64` | `release_on_push.yml:257-262,297-328`; `app/build.gradle.kts:56-61,70-72`; `AppCheckConfig.kt:24-44`; `AndroidManifest.xml:28`; `docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:20,35` | Release-sign every distributed build. Use Play Integrity. Never ship `BuildConfig.DEBUG` builds |
| S2 | **High** | Account deletion unreachable; no data export; partial deletion state possible; `applicationUpdateEvents` undeletable and retained | `MainCoachViewModel.kt:474`; `SpeakCraftRepository.kt:1292-1319`; `firestore.rules:862` | In-app deletion plus web deletion URL (Play requirement), server-side cascade, export |
| S3 | **High** | Sideloading self-updater with a user-editable source repo and "uninstall and reinstall" guidance. Play policy violation | `AppUpdateScreen.kt:265,314-324`; `ReleaseUpdateRepository.kt:313-362` | Flavor-gate or remove; use Play In-App Updates |
| S4 | **High** | `allowBackup=true` with template rules. Voice and video recordings, transcripts in cache, and mentor chat go to cloud backup and device transfer | `AndroidManifest.xml:18-20`; `res/xml/backup_rules.xml`; `res/xml/data_extraction_rules.xml` | Exclude `recordings/`, `updates/`, `backups/`, Firestore cache and secrets; or set `allowBackup=false` |
| S5 | **Medium** | PII (uid, email, provider emails) logged to Logcat on every Firestore error, in release builds | `FirestoreErrorHandler.kt:35-51` | Redact; gate on debug; send to crash reporter without PII |
| S6 | **Medium** | No local cleanup on sign-out; recordings not user-partitioned; wipe helpers dead | `AuthHelper.kt:126-143`; `AudioRecorderManager.kt:57`; `LearningProgressManager.kt:697` | Per-user storage roots and wipe on sign-out and deletion |
| S7 | **Medium** | Over-broad admin powers: MODERATOR can publish releases (controls the OTA URL and mandatory flag), write unvalidated lessons and challenges, and read all user profiles. Client and rules role semantics differ | `firestore.rules:333-334,794-853`; `SpeakCraftRepository.kt:250,261` | Release publishing limited to super-admin or CI only; least-privilege roles; a single role source (claims) |
| S8 | **Medium** | CI supply chain: mutable action tags; unpinned `npm install` with the service-account secret in env; `contents: write` on every push; no tests or lint gate | `release_on_push.yml:41,55,66,107,469,500` | Pin SHAs, lockfile, environments with approvals, test and lint gates |
| S9 | **Medium** | Rules validation gaps: list items beyond index 2 unchecked; no `hasOnly` on several collections; client-writable scores and streaks; no rate or doc caps | `firestore.rules:64-70,704-753,826-853` | Full list validation, server-computed aggregates (Functions) |
| S10 | **Medium** | No R8; debug App Check library in release; logs not stripped | `app/build.gradle.kts:66,139` | Enable R8 with keep rules; `debugImplementation` for the debug provider |
| S11 | **Low** | Cloud-AI "network boundary" interceptor not wired (assurance gap); model integrity hashes blank | `LocalExecutionPolicy.kt:35`; `OnDeviceModelManager.kt:83-139,191` | Wire or remove; pin model hashes |
| S12 | **Low** | FileProvider `cache-path "/"` and unused `recordings/` and `backups/` paths | `res/xml/file_paths.xml:3-6` | Narrow to `updates/` (or remove with the updater) |
| S13 | **Low** | Raw exception text shown to users at sign-in | `AuthHelper.kt:121` | Map to user-safe messages |
| S14 | **Low** | Public identifiers committed (reCAPTCHA site key, project id, database id) | `firebase_applet_config.xml:3-4`; `publish_firestore_release.test.mjs:11` | Acceptable; move to build config per environment |

### 13.3 Non-security platform defects worth carrying into the redesign
1. **BROKEN**: speaking-session saves are denied by the rules (HEAD regression) [V]. See §4.4.
2. **BROKEN** [I]: APK install without `REQUEST_INSTALL_PACKAGES`; `resolveActivity` without `<queries>` on API 30+.
3. Local builds and unit tests need `app/google-services.json`, which is not in the repo and has no documented dev setup [I].
4. 16 unbounded listeners at sign-in; non-transactional read-modify-write; offline writes hang [V/I].
5. `AppContainer` is not process-scoped; no `Application` class [V].
6. Version drift (Compose BOM 2024.09, lifecycle 2.8.7, OkHttp 4.10) and the JUnit catalog mis-reference [V].
7. [I] Play 16 KB page-size risk from MediaPipe 0.10.14 and TFLite 2.16.1 native libraries. CI produces APKs, not AABs.
8. Gradle wrapper scripts and jar are not committed; CI relies on `setup-gradle` [V].
