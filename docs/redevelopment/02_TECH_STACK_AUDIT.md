# 02 — Technology Stack Audit

| Field | Value |
|---|---|
| Document ID | RD-02 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting product owner approval |
| Owner (role) | Solution Architect |
| Approval | Pending — Product Owner (final approver) |
| Related | RD-00, RD-03, ARC-01, ENG-01, ADR-001…ADR-012 |

---

## 1. Purpose and method

This document records the **actual** technology stack of SpeakCraft at commit `68a9cb1` (branch `main`, 26 commits, 2026-11-18 → 2026-11-20). Per the redevelopment constraint, **the existing stack is the default for every proposal**. Any change is raised only as an ADR with status *Proposed — requires explicit approval* (see §6).

**Method.** Static review of `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `AndroidManifest.xml`, `res/xml/*`, `.github/workflows/release_on_push.yml`, `firebase.json`, `firestore.rules`, and `grep` of imports across all 106 main Kotlin files (48,894 lines) and 24 test files.

**Limitations.** No JDK, Android SDK or Gradle installation exists in the analysis environment, and the Gradle wrapper scripts are not committed. **Nothing was compiled, run or tested.** Statements are labelled **VERIFIED** (seen at the cited location) or **INFERRED**.

## 2. Technology inventory

### 2.1 Platform and toolchain

| Area | Technology (version) | Status | Evidence |
|---|---|---|---|
| Programming language | Kotlin 2.2.10 (100% of main sources; no Java) | VERIFIED | `gradle/libs.versions.toml:11` |
| Kotlin Android integration | AGP 9 built-in Kotlin (no `kotlin-android` plugin applied) | VERIFIED / INFERRED (AGP 9 default) | `app/build.gradle.kts:1-8` |
| Android Gradle Plugin | 9.1.1 | VERIFIED | `libs.versions.toml:2` |
| Gradle | 9.3.1 (wrapper properties only; `gradlew` + `gradle-wrapper.jar` **not committed**) | VERIFIED | `gradle/wrapper/gradle-wrapper.properties:3` |
| Build scripts | Gradle Kotlin DSL + version catalog | VERIFIED | `*.gradle.kts`, `libs.versions.toml` |
| SDK levels | compileSdk 36 / targetSdk 36 / **minSdk 24** (Android 7.0) | VERIFIED | `app/build.gradle.kts:25,29,30` |
| JVM target | Java 11 source/target; CI uses Temurin 21 | VERIFIED | `app/build.gradle.kts:75-78`; workflow `:59-63` |
| Modules | Single `:app` module | VERIFIED | `settings.gradle.kts:27` |
| Namespace / applicationId | `com.example` (placeholder) / `com.aistudio.speakcraft.qxmvkp` (Google AI Studio origin) | VERIFIED | `app/build.gradle.kts:24,28` |
| Version | versionCode 2, versionName `1.1.0-beta.1`, channel `beta` (CI overrides) | VERIFIED | `app/build.gradle.kts:10-18` |

### 2.2 Application architecture

| Area | Technology | Status | Evidence |
|---|---|---|---|
| UI framework | Jetpack Compose, Compose BOM **2024.09.00** (≈ Compose UI 1.7 / Material3 1.3) | VERIFIED (version) / INFERRED (mapping) | `libs.versions.toml:12` |
| Design library | Material 3 + material-icons-core/extended | VERIFIED | `app/build.gradle.kts:114-116` |
| Architecture pattern | MVVM-style: Compose screens → `ViewModel` (StateFlow) → concrete repositories/engines. **No domain layer, no use cases, no repository interfaces.** | VERIFIED | RD-03 §3 |
| Navigation | Navigation Compose 2.8.9 with string routes, one `NavHost` in `ui/AppNavigation.kt` (1,548 lines) | VERIFIED | `AppNavigation.kt:906` |
| Dependency injection | **Manual service locator** `AppContainer` (24 `by lazy` properties), created inside a composable via `remember`. No Hilt/Dagger/Koin. No `Application` subclass. | VERIFIED | `data/AppContainer.kt:22-128`; `AppNavigation.kt:216` |
| ViewModel construction | `viewModelFactory { initializer { … } }`, keyed per uid | VERIFIED | `AppNavigation.kt:317,368,388` |
| Concurrency | Kotlin coroutines + Flow 1.10.2; `stateIn(WhileSubscribed(5000))` | VERIFIED | `MainCoachViewModel.kt:44,94-97` |
| Serialization | kotlinx-serialization-json 1.9.0 **and** `org.json` (two JSON stacks) | VERIFIED | 5 files vs 7 files |
| Lifecycle | lifecycle 2.8.7, activity-compose 1.10.1, core-ktx 1.18.0 | VERIFIED | `libs.versions.toml:3,7-10` |

### 2.3 Data, identity and backend

| Area | Technology | Status | Evidence |
|---|---|---|---|
| Local key-value storage | Jetpack DataStore Preferences 1.1.7 (two stores: `speakcraft_prefs`, `speakcraft_tutorial_state`) | VERIFIED | `AppPreferencesRepository.kt:22`; `TutorialState.kt:11` |
| Local structured storage | **Hand-rolled JSON files** in `filesDir` (`learning_state/`, `notification_store/`, `mentor_history_<uid>.json`) | VERIFIED | `LearningProgressManager.kt:50`; `NotificationManagerRepository.kt:36` |
| Local relational DB | **None.** Room 2.7.0 is declared in the catalog but **not** included in the app; KSP plugin is applied with no processor | VERIFIED | `libs.versions.toml:15-17`; `app/build.gradle.kts:5` |
| Remote database | Cloud Firestore (named database `ai-studio-android-speakcra-e1a16d03-…`), SDK default offline cache (no explicit settings) | VERIFIED | `firebase_applet_config.xml:3`; no `FirebaseFirestoreSettings` |
| Security rules | `firestore.rules` (865 lines), default-deny, owner checks, allow-lists | VERIFIED | `firestore.rules:5-7` |
| Authentication | Firebase Auth via **Google Sign-In only** (Credential Manager 1.5.0 + googleid 1.1.1). No email, phone or anonymous auth | VERIFIED | `AuthHelper.kt:41-124` |
| App attestation | Firebase App Check: Debug provider when `BuildConfig.DEBUG`, reCAPTCHA provider otherwise (not Play Integrity) | VERIFIED | `AppCheckConfig.kt:24-60` |
| File storage | App-private `filesDir/recordings/`, `updates/`, `backups/`, `ai_models/`. **No cloud storage** (no Firebase Storage) | VERIFIED | `AudioRecorderManager.kt:57` |
| Encryption at rest | Android Keystore AES-GCM for the optional YouTube API key only | VERIFIED | `AppPreferencesRepository.kt:51-110` |
| Networking | OkHttp **4.10.0** (three separate clients: GitHub, update download, YouTube). No Retrofit | VERIFIED | `GitHubReleaseClient.kt:12`; `ReleaseUpdateRepository.kt:53`; `YouTubeIntelligenceClient.kt:61` |
| Server-side code | **None** (no Cloud Functions, no backend). One Node admin script for publishing releases | VERIFIED | `scripts/publish_firestore_release.mjs` |

### 2.4 Speech, audio, video and intelligence

| Area | Technology | Status | Evidence (details in RD-03 §5) |
|---|---|---|---|
| Speech recognition | Android `SpeechRecognizer` (platform) as the working path; optional Whisper/Moonshine TFLite path | VERIFIED (platform API) / see RD-03 for TFLite status | `data/ai/local/audio/*` |
| Text-to-speech | Android `TextToSpeech` (platform) | VERIFIED | `data/media/TtsAndSpeechHelper.kt` |
| Audio capture | `MediaRecorder` (AAC/M4A) + PCM decode for DSP | VERIFIED | `data/media/AudioRecorderManager.kt` |
| Audio DSP | Hand-written Kotlin (RMS, VAD, pitch, pauses, resampling) | VERIFIED | `AudioFeatureExtractionEngine.kt`, `AudioResampler.kt` |
| Video capture | CameraX 1.5.0 (core/camera2/lifecycle/view/video) | VERIFIED | `data/media/CameraVideoRecorder.kt` |
| Vision ML | MediaPipe Tasks Vision 0.10.14 (face/pose/hand landmarkers). **Model files are not bundled** (`assets/models/` holds only README + `.gitkeep`) | VERIFIED | `assets/models/README.md` |
| Audio ML | MediaPipe Tasks Audio 0.10.14 — **included, zero imports** | VERIFIED | `app/build.gradle.kts:148` |
| On-device inference | TensorFlow Lite 2.16.1 (one file) | VERIFIED | `LocalSpeechRecognitionEngine.kt` |
| Language intelligence | Deterministic Kotlin rule engines (grammar, vocabulary, fillers, feedback templates). No LLM | VERIFIED | `data/ai/local/language/*` |
| Cloud AI | Firebase AI Logic (`firebase-ai`) **is a dependency** and `FirebaseAiLogicService` (868 lines) exists, but is runtime-blocked by a compile-time `LOCAL_ONLY` policy | VERIFIED | `app/build.gradle.kts:135`; `AISettingsRepository.kt:24`; `LocalExecutionPolicy.kt:13-15` |

### 2.5 Background work, notifications and observability

| Area | Technology | Status | Evidence |
|---|---|---|---|
| Background processing | **None** (no WorkManager, JobScheduler, AlarmManager, Services) | VERIFIED (0 usages) | grep |
| System notifications | **None** (no `NotificationCompat`/channels; in-app notification list only) | VERIFIED | grep |
| Push messaging | **None** (no FCM) | VERIFIED | grep |
| Crash reporting | **None** (no Crashlytics/Sentry) | VERIFIED | `app/build.gradle.kts:104-169` |
| Analytics | **None** | VERIFIED | — |
| Logging | `android.util.Log` (84 calls), no framework, not stripped in release; **PII in Firestore error logs** | VERIFIED | `FirestoreErrorHandler.kt:35-51` |
| Remote config / feature flags | **None** (compile-time policy constants) | VERIFIED | `LocalExecutionPolicy.kt:13` |

### 2.6 Quality, build and release

| Area | Technology | Status | Evidence |
|---|---|---|---|
| Unit tests | JUnit 4 + kotlinx-coroutines-test; **161 `@Test`** in 24 files | VERIFIED (count) | `app/src/test/**` |
| UI tests | Robolectric 4.16.1 + Compose ui-test-junit4 (JVM) | VERIFIED | `libs.versions.toml:38` |
| Instrumented tests | 1 trivial (`ExampleInstrumentedTest`) | VERIFIED | `app/src/androidTest/**` |
| Mocking | None (no MockK/Mockito); hand-written fakes | VERIFIED | — |
| Rules tests | `firestore.test.js` (4 tests, `@firebase/rules-unit-testing`) — **not runnable as committed** (no `package.json`) | VERIFIED | `firestore.test.js` |
| CI/CD | GitHub Actions, single workflow `release_on_push.yml` (512 lines): build → sign → GitHub Release → optional Firestore release record | VERIFIED | `.github/workflows/release_on_push.yml` |
| CI quality gates | **No unit tests, no lint, no rules tests, no rules deploy** in CI | VERIFIED | workflow |
| Distribution | GitHub Releases APK + **in-app self-updater** (SHA-256 + certificate check). No Play Store / AAB | VERIFIED | `data/update/*` |
| Signing | Release keystore from secrets; **beta channel falls back to debug keystore + `assembleDebug`** | VERIFIED | workflow `:257-262,297-328` |
| Code shrinking | R8 **disabled** (`isMinifyEnabled = false`); empty ProGuard rules | VERIFIED | `app/build.gradle.kts:66` |

## 3. Unused, idle and mis-declared dependencies (VERIFIED)

| Item | Problem | Recommendation |
|---|---|---|
| Room (runtime/ktx/compiler), Retrofit, converter-moshi, Moshi (+codegen), Coil, Accompanist permissions, play-services-location, logging-interceptor | Declared in catalog, not used anywhere | Remove from catalog, except Room if ADR-002 is approved |
| MediaPipe tasks-audio | Shipped in APK, zero imports | Remove (APK size) |
| `ui-tooling-preview` | Included, zero `@Preview` | Keep (useful for redesign) or remove |
| KSP plugin | Applied, no processor | Keep only if ADR-002 (Room) is approved; otherwise remove |
| Secrets Gradle plugin | Applied, `.env.example` empty, no `BuildConfig` secret usage | Remove |
| `firebase-appcheck-debug` | `implementation` → ships in release | Change to `debugImplementation` |
| `firebase-ai` | Shipped, runtime-blocked | Decide via ADR-003 |
| JUnit catalog entry | `junit` library points at `junitVersion` (1.3.0) rather than `junit` (4.13.2); `junit:junit:1.3.0` does not exist — probably masked transitively | Fix reference |
| foojay resolver | Applied, no toolchain requested | Remove or configure `jvmToolchain` |

## 4. Version currency (VERIFIED versions; INFERRED impact)

| Component | Current | Observation |
|---|---|---|
| Compose BOM | 2024.09.00 | ≈ 26 months behind the rest of the toolchain (AGP 9.1.1, Kotlin 2.2.10, Firebase BoM 34.17.0). Newer Material 3 components and adaptive APIs are unavailable. |
| Lifecycle / Navigation / Activity | 2.8.7 / 2.8.9 / 1.10.1 | Old relative to core-ktx 1.18.0 |
| OkHttp | 4.10.0 (2022) | Old; three separate clients |
| MediaPipe / TFLite | 0.10.14 / 2.16.1 | INFERRED risk of non-16 KB-aligned native libraries (Google Play requirement for apps targeting Android 15+) |

Version upgrades **within the same libraries** are not technology changes; they are planned as engineering tasks (PLN-02 EP-01) and recorded in ADR-006 for visibility.

## 5. Stack assessment

| Area | Verdict | Reason |
|---|---|---|
| Kotlin + Compose + Material 3 | **Retain** | Modern, appropriate, team has 10k+ lines of Compose UI to reuse patterns from |
| Navigation Compose | **Retain** | Adequate; restructure routes (type-safe route objects available in 2.8.x) |
| Manual DI (`AppContainer`) | **Retain, fix scoping** | Adequate for a single-developer app; move to an `Application` subclass so it is process-scoped. A DI framework is not needed. |
| DataStore | **Retain** | Correct for preferences |
| JSON files for learning state | **Replace (proposal)** | No transactions, no queries, no migrations, not user-partitioned consistently → ADR-002 proposes Room (already in the catalog) |
| Firestore + Auth (Google) | **Retain** | Works; strong per-user rules. Change write semantics for offline-first (don't `await()` server acknowledgement on UI paths). |
| App Check (reCAPTCHA / Debug) | **Retain product, change provider (proposal)** | Play Integrity is the standard Android provider → ADR-008 |
| Android SpeechRecognizer + TTS | **Retain as primary** | Only working recognition path; on-device where the device supports it |
| TFLite Whisper/Moonshine | **Defer** | Placeholder model catalogue (blank hashes, no downloader) → ADR-009 |
| MediaPipe Vision + CameraX | **Defer** | Models not bundled; heuristic value; heavy → ADR-010 |
| Firebase AI Logic | **Remove from product scope (proposal)** | Conflicts with stated privacy constraint; blocked at runtime anyway → ADR-003 |
| OkHttp | **Retain** (single shared client) | Only needed if updater/model download remain |
| GitHub self-updater | **Decision required** | Incompatible with Google Play → ADR-004 |
| Background work | **Gap** | Reminders need scheduling → ADR-005 (WorkManager vs AlarmManager) |
| Crash reporting | **Gap** | No visibility into production failures → ADR-007 (requires approval; privacy review) |

## 6. Technology decisions raised (all *Proposed — requires explicit approval*)

| ADR | Topic | Type |
|---|---|---|
| ADR-001 | Retain the existing technology stack as the baseline | Confirmation |
| ADR-002 | Room as the local source of truth for learning data (replacing JSON files) | **New dependency (catalog-declared, not used)** |
| ADR-003 | Remove Firebase AI Logic (cloud AI) from the product | Removal |
| ADR-004 | Distribution channel and the in-app APK updater | Product + platform decision |
| ADR-005 | Reminder scheduling: WorkManager vs AlarmManager | **New dependency (WorkManager)** or platform API |
| ADR-006 | Dependency currency upgrade (Compose BOM, lifecycle, navigation, OkHttp) | Version upgrade |
| ADR-007 | Crash reporting (Firebase Crashlytics) | **New dependency** |
| ADR-008 | App Check provider: Play Integrity | Provider change within Firebase |
| ADR-009 | Speech recognition strategy (platform recognizer primary; TFLite ASR deferred) | Scope |
| ADR-010 | Defer automated video/body-language analysis | Scope |
| ADR-011 | Remove YouTube Intelligence / creator scraping | Removal |
| ADR-012 | Code organisation: package-by-feature inside `:app` | Structure (no tech change) |

No technology migration is implemented in Stage A.
