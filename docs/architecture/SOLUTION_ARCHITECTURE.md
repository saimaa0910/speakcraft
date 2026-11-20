# Solution Architecture — SpeakCraft (Target)

| Field | Value |
|---|---|
| Document ID | ARC-01 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — **Proposed**, awaiting approval (gate G3) |
| Owner (role) | Solution Architect |
| Approval | Pending — Product Owner; Android Tech Lead review |
| Related | RD-02, RD-03, ADR-001…ADR-015, ARC-02 Technical Design, ARC-03 Database Design, ARC-04 API & Integration, ARC-05 Local Intelligence, SEC-01, NFR |

---

## 1. Architecture overview

SpeakCraft is a **single-module, offline-first Android application** on the existing stack (ADR-001). The device is the system of record; Firebase is an optional backup/sync target for signed-in users. All speech processing is on-device (ARC-05).

```mermaid
flowchart TB
    subgraph Device[Android device]
      subgraph UI[feature/* — Compose screens + ViewModels]
        ONB[onboarding] --- TOD[today] --- SES[session] --- PRA[practice] --- WOR[words] --- PRO[progress] --- SET[profile/settings]
      end
      subgraph Domain[domain engines — pure Kotlin]
        PLAN[PlanEngine] --- STREAK[ProgressCalculator] --- SRS[ReviewScheduler] --- EVALP[Evaluation pipeline]
      end
      subgraph Speech[speech/*]
        CAP[CaptureController] --- REC[RecognitionSession] --- CAPS[SpeechCapabilityService] --- MET[MetricEngine/RuleEngine/FeedbackComposer]
      end
      subgraph Data[data/*]
        ROOM[(Room DB)] --- DS[(DataStore prefs)] --- CONTENT[Content pack\nassets JSON] --- REPO[Repositories] --- OUTBOX[Sync outbox]
      end
      WM[WorkManager:\nSyncWorker · ReminderWorker · CleanupWorker]
      OS[Platform: SpeechRecognizer · TextToSpeech · AudioRecord · NotificationManager · Credential Manager]
    end
    UI --> Domain
    UI --> Speech
    Domain --> Data
    Speech --> Data
    Speech --> OS
    WM --> Data
    WM --> FS
    REPO --> AUTH
    subgraph Cloud[Firebase — optional]
      AUTH[Firebase Auth]
      FS[(Cloud Firestore\nusers/{uid}/…)]
    end
```

### 1.1 Key architectural decisions

| Decision | Reference |
|---|---|
| Retain Kotlin/Compose/M3/Navigation/manual DI/DataStore/Firebase | ADR-001 |
| Room is the local source of truth | ADR-002 |
| No cloud AI; remove Firebase AI Logic | ADR-003 |
| Release-signed distribution; no in-app updater | ADR-004 |
| WorkManager for reminders, sync, cleanup | ADR-005 |
| Platform speech recognition in tiers T1–T3 | ADR-009 |
| Package-by-feature in `:app` | ADR-012 |
| Local-first sync with immutable facts | ADR-014 |
| Recordings in `noBackupFilesDir`, retention policy | ADR-015 |

## 2. Component responsibilities

| Layer | Component | Responsibility | Must not |
|---|---|---|---|
| App shell | `SpeakCraftApplication`, `AppContainer` | Process-scoped object graph (manual DI); WorkManager initialisation | Be created in composition (fixes FND-ARCH-02) |
| | `MainActivity`, `SpeakCraftNavHost` | Single activity; navigation graph; window-size class | Contain business logic |
| Feature UI | Screens (`@Composable`) | Render `UiState`; emit intents | Access repositories, Firestore, files |
| | ViewModels | Hold screen state (`StateFlow<UiState>`), call domain/repositories, map errors to user messages; use `SavedStateHandle` for restorable inputs | Hold Android `Context` beyond application scope |
| Domain | `PlanEngine` | Deterministic daily session generation (SRS-BR-013/014) | Do I/O |
| | `ProgressCalculator` | Streaks, practice time, trends, word-status counts from facts (SRS-BR-010…018, -024) | Store counters as truth |
| | `ReviewScheduler` | SM-2 variant scheduling (SRS-BR-016) | — |
| | `EvaluationPipeline` | Orchestrates gate → metrics → rules → feedback (ARC-05) | Fabricate values |
| Speech | `SpeechCapabilityService` | Tier detection, pack status, consent-aware status | Report unverified states |
| | `CaptureController`, `RecognitionSession` | Microphone capture, recognition, interruption handling | Run more than one at a time |
| Data | Repositories (`ProfileRepository`, `AttemptRepository`, `PlanRepository`, `WordRepository`, `ContentRepository`, `PreferencesRepository`, `AccountRepository`, `SyncRepository`) | Expose `Flow`s from Room/DataStore; transactional writes; enqueue outbox | Await network on UI paths |
| | `ContentRepository` | Load and index the bundled content pack (activities, words) by version | Fetch remote content (R1–R2) |
| | `SyncEngine` + `SyncWorker` | Upload outbox, pull changes, merge (ADR-014) | Upload audio, paths, transcripts (default) |
| | `AccountRepository` | Sign-in, sign-out, re-auth, resumable deletion | — |
| Background | `ReminderWorker`, `CleanupWorker` | Post reminders (SRS-BR-019); enforce retention (SRS-BR-022) | — |
| Cross-cutting | `AppLogger`, `Clock`/`DateKeyProvider`, `DispatcherProvider`, `FeatureFlags` (BuildConfig) | PII-free logging; testable time; testable threading; compile-time flags | — |

Detailed classes, interfaces and sequences: ARC-02.

## 3. Module boundaries and package structure

Per ADR-012 (namespace `app.speakcraft`; applicationId decision D-15):

```
app/         Application, AppContainer, MainActivity, navigation
core/        designsystem · ui (shared components, state templates) · common (Clock, Result, AppLogger, DispatcherProvider) · testing
domain/      plan · progress · review · evaluation (pipeline contracts) · model (pure Kotlin domain models)
speech/      capability · capture · recognition · analysis (features, metrics) · rules · feedback · tts
data/        local (Room: entities, DAOs, migrations) · prefs (DataStore) · content (pack loader) · remote (Firestore DTOs, mappers) · sync · account · repository
feature/     onboarding · today · session · practice · words · progress · profile · settings · reminders · inbox (R2)
```

Dependency rules (NFR-MAINT-001):

```
feature → domain, speech, data(repository interfaces), core
speech  → domain(model), data(repository interfaces), core
domain  → core (pure Kotlin; no Android imports)
data    → domain(model), core
core    → (nothing app-specific)
app     → everything (wiring only)
```

Repositories are defined as **interfaces** in `data/repository` with Room/Firestore implementations, enabling fakes in tests (fixes FND-ARCH-01).

## 4. Data flow

### 4.1 Speaking attempt (core loop)

```mermaid
sequenceDiagram
    participant U as User
    participant VM as SessionViewModel
    participant SP as speech (Capture+Recognition)
    participant EV as EvaluationPipeline
    participant AR as AttemptRepository
    participant DB as Room
    participant OB as Outbox/SyncWorker
    U->>VM: Tap Record
    VM->>SP: start(attemptId, tier, activity)
    SP-->>VM: level, elapsed, partial transcript (Flow)
    U->>VM: Tap Stop
    VM->>SP: stop()
    SP-->>VM: CaptureResult(audioFeatures, transcript, wavPath?)
    VM->>EV: evaluate(activity, captureResult)
    EV-->>VM: EvaluationResult(valid|inconclusive, metrics, feedback, version)
    VM->>AR: saveAttempt(result) (transaction)
    AR->>DB: insert attempt, metrics, event; update plan step; outbox row
    DB-->>VM: Flow emits → Today/Progress update reactively
    OB-->>OB: when signed in + online: upload attempt metrics (no audio/transcript)
```

### 4.2 Reactive state
Room `Flow`s drive all screens; there are no manually incremented counters. Today, Progress and Words observe the same tables, so a completed attempt updates every screen without refresh (PO-REQ-1119 Phase 3 "real-time dashboard", implemented locally).

## 5. Navigation

- Navigation Compose with **type-safe routes** (`@Serializable` route objects; kotlinx-serialization already in the stack).
- Graph: `OnboardingGraph` → `MainGraph` (4 tab graphs with saved state) + full-screen routes (`Session(activityId|wordStage|review)`, `SignIn`, `DeleteAccount`, `Permission`).
- Deep links: notification → Today; none from external sources in R1.
- Start destination decided from `ProfileRepository.onboardingState` before first frame (splash holds ≤ the time to read DataStore).

## 6. State management

| Concern | Approach |
|---|---|
| Screen state | `StateFlow<UiState>` per ViewModel; sealed `UiState` with Loading / Content / Empty / Error / Offline variants where applicable (fixes FND-UI-05) |
| User input surviving rotation/process death | `SavedStateHandle` and `rememberSaveable` (fixes FND-UI-06) |
| Long-running attempt | Owned by `SessionViewModel`; capture service scoped to the ViewModel; stopped on `onCleared` and lifecycle `ON_STOP` |
| One-off events (navigation, snackbars) | Channel/`SharedFlow` consumed once |
| Cross-screen data | Repositories' Flows only (no shared mutable singletons) |

## 7. Dependency management

Manual DI (ADR-001): `AppContainer` built in `SpeakCraftApplication.onCreate`; ViewModel factories receive repositories/engines via constructor; features obtain dependencies through a `CompositionLocal`-free factory passed from the nav graph. No service-locator calls from composables. Versions in `gradle/libs.versions.toml`; only ADR-approved libraries (NFR-MAINT-007).

## 8. Offline architecture

| Capability | Offline behaviour |
|---|---|
| Onboarding, Today, Practice, Words, Progress, Settings | Fully functional |
| Speech | T1/T2/T3b work offline; T3a requires network (falls back to T3b per attempt) |
| Sign-in | Requires network; local use continues |
| Sync | Deferred via outbox; status visible |
| Offline-pack install | Requires network; status shown |
| Content | Bundled with the app; no download |

## 9. Synchronisation

ADR-014 model: immutable facts (attempts, events) uploaded once (create-only); mutable documents (profile, plan, word state) last-writer-wins per document (word state merges by most advanced stage); pull by `updatedAt` watermark; outbox drained by `SyncWorker` (unique work, network constraint, exponential back-off). Sync never blocks UI and never uploads audio, file paths or transcripts (default). Firestore SDK offline persistence is **disabled for app data paths** or left default but never relied upon as the source of truth (decision recorded in ARC-03 §8).

## 10. Error handling

| Layer | Strategy |
|---|---|
| Speech | Typed errors (`CaptureError`, `RecognitionError`) → Inconclusive/Interrupted states; never crash |
| Data | `Result<T, DataError>` from repositories; transactional writes; failures surfaced as non-blocking messages (SRS-ERR-005) |
| Sync | Retries with back-off; dead-letter after N failures with user-visible "Retry" |
| UI | Error variants of `UiState` with actions; user-safe copy only (SRS §15) |
| Global | `Thread.setDefaultUncaughtExceptionHandler` records a PII-free crash entry in the diagnostic log (ADR-007), then delegates |

## 11. Observability

| Need | R1 approach |
|---|---|
| Crashes/ANRs | Play Console vitals when on Play; local diagnostic log (ADR-007) |
| Logs | `AppLogger` with levels; release build drops debug/verbose; PII redaction tested (NFR-SEC-002) |
| Product metrics | Beta programme per ADR-013 (local aggregates user can share) |
| Performance | Manual/macro measurements against NFR-PERF in each release candidate |

## 12. Security boundaries

| Boundary | Control | Reference |
|---|---|---|
| Device ↔ other apps | Only `MainActivity` exported; no security-relevant intent extras; FileProvider limited to `exports/` | SEC-01 §5–6 |
| App data at rest | App-private storage; recordings in `noBackupFilesDir`; backup rules exclude sensitive paths | ADR-015, SEC-01 §6 |
| Device ↔ Firebase | HTTPS; Firebase Auth identity; Firestore rules limit each user to `users/{uid}`; facts create-only | SEC-01 §5 |
| Device ↔ speech service | Only in T3a with consent; platform-mediated | ADR-009, SEC-01 §7 |
| Build/release | Release signing, R8, no debug providers in release | ADR-008, ENG-01 |

## 13. Architecture assessment of the current system (summary)

| Area | Current | Target | Effort |
|---|---|---|---|
| Layering | UI → concrete Firebase repos; god objects | Feature/domain/speech/data with interfaces | L |
| Persistence | Firestore + JSON files + DataStore | Room + DataStore; Firestore as sync target | L |
| Speech | 4 wrappers, fake ASR, AAC + boxed decode | One capture + recognition session, tiers | L |
| Evaluation | 2 engines, mislabelled scores | One pipeline, metric catalogue | M–L |
| Navigation | 26 string routes, 1,032-line shell | Type-safe routes, 4 tab graphs | M |
| Background | None | WorkManager | S–M |
| Release | Debug-capable OTA | Release-signed, gated CI | M |

Reuse is substantial at the **algorithm and content** level (RD-03 §13) but low at the **UI and wiring** level; the redevelopment is therefore a restructure inside the existing project, executed epic by epic behind compile-time feature flags (PLN-01).
