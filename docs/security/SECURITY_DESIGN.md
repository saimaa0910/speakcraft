# Security Design

| Field | Value |
|---|---|
| Document ID | SEC-01 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Security Engineer |
| Approval | Pending — Product Owner |
| Related | ENG-01 DevOps & Release Plan, TST-01 Test Strategy, ARC-01/ARC-02/ARC-03/ARC-04/ARC-05, NFR (NFR-SEC-*), FS-01 Account, FS-10 Privacy & data rights, FS-11 Sync, FS-12 Speech capability, ADR-002, ADR-003, ADR-004, ADR-005, ADR-007, ADR-008, ADR-009, ADR-013, ADR-014, ADR-015, RD-02, RD-03 (FND-SEC-*, FND-OPS-*), RD-15 (PP-2, PP-6), RD-16 (NG-01, NG-10, NG-12), PLN-04 RAID |

> **Authority.** Per GOV-02 §4 this document is the authoritative home for security controls (`SEC-REQ-nn`), threats (`THR-nn`) and the security test catalogue (`TC-SEC-nnn`). Other documents reference these IDs and do not restate them.
>
> **Evidence rule.** Labels follow GOV-02 DC-3: **VERIFIED** (seen at `path:line`), **INFERRED**, **ASSUMPTION**, **RECOMMENDATION**. Nothing in this document was built, run, deployed or tested (DC-4). No rules were compiled or deployed, and no Firebase or GitHub setting was inspected online. Every proposed target is **Proposed — requires approval** (DC-5).
>
> Paths are relative to the repository root. `pkg/` = `app/src/main/java/com/example/`.

---

## 1. Scope and assumptions

### 1.1 In scope

| Area | Included |
|---|---|
| Android app (target R0–R2) | Local storage, permissions, exported components, speech capture, logging, backup behaviour, and the authentication and sync clients |
| Firebase | Authentication (Google provider), Cloud Firestore (the existing named database), App Check, API-key restrictions, budget alerts |
| CI/CD | Secrets, signing, supply chain and release integrity. Security requirements are stated here; how they are implemented is in ENG-01 |
| Privacy | Data inventory, retention, consent, data rights, privacy-policy and data-safety inputs |

### 1.2 Out of scope

- Server-side code. None exists (`RD-02 §2.3`) and none is proposed. Any Cloud Function would need an ADR.
- Admin console and shared content collections (RD-16 NG-10). Content ships inside the app.
- Cloud AI (RD-16 NG-01; ADR-003).
- Video capture (ADR-010, deferred to R3).
- The self-updating installer (ADR-004; NG-11).

### 1.3 Assumptions

| ID | Assumption | Label | Impact if false |
|---|---|---|---|
| A-1 | ADR-002 (Room), ADR-003 (remove cloud AI), ADR-004 option B (no updater), ADR-005 (WorkManager), ADR-008, ADR-009, ADR-014 and ADR-015 are approved as proposed | ASSUMPTION | §3, §5, §6 and §7 need revision. Each section names the ADR it depends on |
| A-2 | Account is optional (D-05). The no-account mode is the full core loop and stores data only on the device | ASSUMPTION (pending D-05) | §4 and §6 simplify, but PP-6 is violated |
| A-3 | Production stays on the existing Firebase project and named database (`firebase.json:4`; `res/values/firebase_applet_config.xml:3`), so legacy signed-in data can be migrated (D-19). A separate dev project is recommended in ENG-01 (ED-01) | ASSUMPTION | The legacy migration path (§5.6) changes |
| A-4 | Target users are adults (D-01 segment). The app is not directed at children | ASSUMPTION | The privacy policy, data-safety form and Play Families policy would apply |
| A-5 | Many target devices have file-based encryption. It is mandatory for devices that launch with Android 10 or later; devices on Android 7–9 vary | INFERRED (platform policy) | The §6.6 encryption decision is revisited |
| A-6 | The repository may become public. Nothing in it may depend on secrecy | ASSUMPTION (visibility UNKNOWN) | None. This is the conservative stance |
| A-7 | No Remote Config, analytics or crash SDK exists in R1 (ADR-007 option C; ADR-013 option A) | ASSUMPTION (pending D-13, D-09) | §8.3 endpoints, §9.6 data safety and §10 logging change |

---

## 2. Current-state security assessment (VERIFIED unless labelled)

Source: static audit at HEAD `68a9cb1`. Severity uses the audit scale (Critical / High / Medium / Low). The target control column points to §12.

### 2.1 Findings

| Finding | Severity | Summary | Evidence (`path:line`) | Target control |
|---|---|---|---|---|
| FND-SEC-02 | **Critical** (if the release keystore secret is unset; secret state UNKNOWN) | Every push to `main`/`master`/`beta` builds a beta. Without `ANDROID_KEYSTORE_BASE64` it runs `assembleDebug`, which produces a debuggable APK signed with a debug keystore (store password `android`) that installs the App Check **Debug** provider. The exported launcher accepts a debug-secret intent extra in those builds | `.github/workflows/release_on_push.yml:3-10,257-262,297-328`; `app/build.gradle.kts:56-61,70-72`; `pkg/data/firebase/AppCheckConfig.kt:24-44`; `app/src/main/AndroidManifest.xml:28` | SEC-REQ-14, 27, 39 |
| FND-SEC-03 | High | Account deletion is implemented but has no UI caller and no re-authentication. A partial-deletion state is possible. There is no export. `applicationUpdateEvents` cannot be deleted | `pkg/data/firebase/SpeakCraftRepository.kt:1287-1319`; `pkg/ui/viewmodel/MainCoachViewModel.kt:474`; `firestore.rules:862` | SEC-REQ-05, 22–25 |
| FND-SEC-05 | High (privacy misrepresentation) | Transcripts of up to 15,000 characters are written to Firestore, while Settings says transcripts stay on the device | `SpeakCraftRepository.kt:806`; `firestore.rules:204,233`; `pkg/ui/screens/AnalyticsAndSettingsScreens.kt:917` | SEC-REQ-10, 36 |
| FND-SEC-06 | High (privacy) | `allowBackup="true"` with the untouched template backup and extraction rules. Recordings, chat history and learning JSON are eligible for cloud backup and device transfer | `AndroidManifest.xml:18-20`; `res/xml/backup_rules.xml:8-13`; `res/xml/data_extraction_rules.xml:6-19` | SEC-REQ-16, 17 |
| FND-SEC-07 | High (privacy) | When no on-device recogniser exists, the app falls back to the default (typically network) recogniser with no disclosure | `pkg/data/ai/local/audio/LiveTranscriptionManager.kt:99-103,165,340`; `pkg/data/media/TtsAndSpeechHelper.kt:126` | SEC-REQ-20, 21 |
| FND-OPS-04 | High | Self-updater with a user-editable source repo and "uninstall the old build" copy. Incompatible with Play. `REQUEST_INSTALL_PACKAGES` is missing | `pkg/ui/screens/AppUpdateScreen.kt:265,314-324`; `pkg/data/update/ReleaseUpdateRepository.kt:313-362`; `AndroidManifest.xml:5-8` | SEC-REQ-27, 29 (ADR-004) |
| FND-OPS-01 | High (process) | CI runs no unit tests, lint or rules tests, and does not deploy rules. Deployed rules may differ from the repo (UNKNOWN) | `release_on_push.yml:331-353` (only `assemble*`) | SEC-REQ-12; ENG-01 §4 |
| FND-SEC-01 | Medium (privacy/product) | Google sign-in is mandatory, so the app cannot be used without an account | `pkg/data/firebase/AuthHelper.kt:41-124`; `SpeakCraftRepository.kt:53-56`; `pkg/ui/AppNavigation.kt:244-261` | SEC-REQ-01, 02 |
| FND-SEC-04 | Medium | Admin model is overbroad. Any admin, including MODERATOR, can publish OTA releases and write unvalidated lessons and challenges. Admins can read every profile. Client and rules map roles differently | `firestore.rules:17-46,333-334,761-764,794-853`; `SpeakCraftRepository.kt:250,261` | SEC-REQ-11, 13 |
| FND-SEC-08 | Medium | uid, email and provider emails are written to Logcat on every Firestore error, in all build types | `pkg/data/firebase/FirestoreErrorHandler.kt:25-51` | SEC-REQ-34 |
| FND-SEC-09 | Medium | Sign-out clears nothing local. Recordings are in a shared, unpartitioned folder. The wipe helper is never called | `AuthHelper.kt:126-143`; `pkg/data/media/AudioRecorderManager.kt:57,62`; `pkg/data/learning/LearningProgressManager.kt:697` | SEC-REQ-04, 18 |
| FND-SEC-10 | Medium | Rule validation gaps: only list items 0–2 are checked, several collections have no `hasOnly`, and scores and streaks are client-writable | `firestore.rules:64-70,79-91,356-362,704-753` | SEC-REQ-08, 09 |
| FND-SEC-11 | Medium | R8 is disabled. The App Check debug library ships in release. Logs are not stripped | `app/build.gradle.kts:66,139`; `app/proguard-rules.pro:1-21` | SEC-REQ-14, 27, 34 |
| FND-OPS-03 | Medium | CI actions are pinned to mutable tags. `npm install` runs without a lockfile while the service-account secret is in the step environment. The token has `contents: write` on every push | `release_on_push.yml:40-41,55,60,66,107,112,469,483,500` | SEC-REQ-32, 33 |
| FND-OPS-02 | Medium | The Gradle wrapper is not committed (only `gradle/wrapper/gradle-wrapper.properties`). A local build fails without `google-services.json` (INFERRED) | `gradle.properties:28`; `AuthHelper.kt:53,93` | SEC-REQ-33; ENG-01 §3 |
| FND-OPS-05 | Medium | No crash reporting and no logging framework (84 raw `Log` calls) | audit §10 | ADR-007; SEC-REQ-34, 35 |
| FND-AI-10 | Low (assurance) / High if re-enabled | `firebase-ai` ships but is gated at runtime. The "network boundary" interceptor is never attached, so the test that relies on it gives false assurance | `app/build.gradle.kts:135`; `pkg/data/ai/local/LocalExecutionPolicy.kt:13-15,35-48` | SEC-REQ-28 (ADR-003) |
| FND-SEC-12 | Low | FileProvider exposes `cache-path "/"` plus unused `recordings/` and `backups/` paths | `res/xml/file_paths.xml:3-6`; `AndroidManifest.xml:38-46` | SEC-REQ-29 |
| S13 (audit) | Low | Raw exception text is shown to users at sign-in | `AuthHelper.kt:121` | SEC-REQ-06 |
| S14 (audit) | Low | Public identifiers are committed (reCAPTCHA site key, database id) | `res/values/firebase_applet_config.xml:3-4` | §8.1 (acceptable) |
| FND-OPS-06 / 07 | Low | Dependency drift; the JUnit catalog entry points at the wrong version; docs drift on versioning | `gradle/libs.versions.toml:4-5,50`; `docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:16,20` vs `release_on_push.yml:137` | ENG-01 §7, §11 |
| FND-DATA-06 | High (functional, not security) | Every speaking-session save is rejected by the rules (5 fields not in `hasOnly`), a regression in HEAD | `SpeakCraftRepository.kt:829-833` vs `firestore.rules:459-467` | Rules emulator tests in CI (SEC-REQ-12) prevent recurrence |

### 2.2 Strengths to keep (VERIFIED)

| Strength | Evidence |
|---|---|
| Global default deny; owner checks; `hasAll`/`hasOnly`; immutable `createdAt`; timestamps `<= request.time`; create-only audit collections | `firestore.rules:5-7,13-15,336-362,472` |
| No secrets in git history; keystores and `.env` ignored | `.gitignore:17-19`; audit §6.1 |
| Workflow inputs are passed through `env:`, not interpolated into scripts (injection hygiene). Credentials are cleaned up in an `always()` step | `release_on_push.yml:119-127,508-512` |
| Optional user API key encrypted with Android Keystore AES-GCM. This pattern can be reused if app-level encryption is ever needed (§6.6) | `pkg/data/preferences/AppPreferencesRepository.kt:51-110` |
| Repository filtering in Gradle settings; `FAIL_ON_PROJECT_REPOS` | `settings.gradle.kts:3-9,18` |

### 2.3 Priority containment (RECOMMENDATION — first Stage B changes, each needs approval)

Stage A changes nothing. These four items reduce live risk to current beta users before the redesign lands:

1. **Stop push-triggered distribution.** Make the existing workflow `workflow_dispatch`-only, or require the release keystore. No new debug-signed builds should reach testers (THR-04, THR-16). Implementation: ENG-01 ENG-T-26.
2. **Freeze admin-writable shared collections** in the deployed rules. Set `applicationReleases`, `adminUsers`, `lessons` and `learningChallenges` client writes to `false`. CI's Admin SDK publisher bypasses rules, so the existing release path still works (THR-18).
3. **Remove the App Check debug-token intent injection** in any build that is distributed (THR-03).
4. **Remove PII from `FirestoreErrorHandler`** logging (THR-14).

---

## 3. Data inventory and classification (TARGET design)

### 3.1 Classification scheme

| Class | Meaning | Handling baseline |
|---|---|---|
| **C1 Sensitive-voice** | Raw voice audio and speech transcripts. They can contain personal disclosures; voice is biometric-adjacent | On device only. Never uploaded by SpeakCraft. Excluded from backup and device transfer. Explicit retention. Never logged |
| **C2 Personal** | Account identifiers and profile data that identify or describe the user | Minimised. Synced only when signed in. Never logged |
| **C3 Learning** | Derived metrics, plans, vocabulary progress and learning events (personal data, lower sensitivity) | Synced when signed in. Backed up locally only through include-listed files |
| **C4 Device/Operational** | Device-scoped state: preferences, install identifier, consent records, sync watermarks, diagnostics | Local. Not backed up when device-specific |
| **C5 Public** | Bundled content pack | No personal data |

### 3.2 Inventory

Room table names follow PLAN_AND_CANON; ARC-03 is authoritative for the schema. The split of transcripts into a separate database file is a **RECOMMENDATION** for ARC-03 (it makes backup exclusion simple and testable, §6.3).

| # | Data item | Store (local) | Class | Synced to Firestore? | Retention (local / cloud) | Deletion path | Backup / D2D |
|---|---|---|---|---|---|---|---|
| 1 | Audio recording (WAV during the attempt, optionally AAC after evaluation; ADR-015) | `noBackupFilesDir/recordings/<profileId>/<attemptId>.(wav\|m4a)`; metadata in `recording_file` | C1 | **Never** | Default 30 days (D-10). Options: after feedback / 7 d / 30 d / keep | Retention job; per-attempt delete; Clear history; sign-out (Remove); delete account; delete local data | Excluded (by location and by rules) |
| 2 | Transcript text | `sensitive.db` (separate Room database), keyed by `attemptId` | C1 | **No** in R1. R2 opt-in only (D-16), to a separate `transcripts` subcollection (§7.5) | Kept with its attempt until deleted (proposed; SD-03) | Per-attempt delete; Clear history; sign-out (Remove); delete account | Excluded |
| 3 | Recognition partials (live) | Memory only | C1 | No | Discarded at the end of the attempt | Process end | n/a |
| 4 | Attempt record and metrics (template, content id and version, rule-set version, result state, duration, measured metrics, estimated metrics, feedback **codes**) | `attempt`, `attempt_metric` in `learning.db` | C3 | Yes, signed in: `users/{uid}/attempts/{attemptId}`, immutable | Until the user deletes it | Clear history (local + cloud); delete account | Included (`learning.db`) |
| 5 | "Was this accurate?" rating (ADR-013 A) | `learning.db` | C3 | No | With the attempt | As #4 | Included |
| 6 | Daily session plan and steps | `session_plan`, `session_step` | C3 | Yes: `users/{uid}/plans/{dateKey}` (mutable, LWW) | Proposed: 90 days rolling (ARC-03 to confirm) | Pruning job; Clear history; delete account | Included |
| 7 | Word progress (stage, schedule) | `word_progress` | C3 | Yes: `users/{uid}/words/{wordId}` (merge rule per ADR-014) | Until reset or deleted | Reset vocabulary; delete account | Included |
| 8 | Word review results | `word_review` | C3 | As `WORD_REVIEWED` events | Until deleted | Reset vocabulary; delete account | Included |
| 9 | Learning events (`SESSION_COMPLETED`, `WORD_STAGE_PASSED`, `WORD_REVIEWED`), idempotent | `learning_event` | C3 | Yes: `users/{uid}/events/{eventId}`, immutable | Until deleted | Clear history (session events); reset vocabulary (word events); delete account | Included |
| 10 | Profile: level (D-17), goal, daily minutes, reminder time, time zone, optional display name | `profile` | C2 | Yes: `users/{uid}` | Until deleted | Delete account; delete local data | Included |
| 11 | Account identity: Firebase uid, Google email, name, photo URL | Firebase Auth SDK storage (SDK-managed, app-private); only the uid is copied into `profile.linked_uid` | C2 | Held by Firebase Auth. **Email is not copied into Firestore documents** | Session lifetime | Sign-out; `FirebaseUser.delete()` | Excluded (not include-listed) |
| 12 | Synced learning preferences (daily word goal, review reminders, retention choice) | DataStore `settings` | C2/C4 | Yes, inside `users/{uid}.prefs` | Until changed | Delete account; delete local data | Included (`settings` file) |
| 13 | Device-only state: install `deviceId` (random UUID), T3a consent record (granted, disclosure version, timestamp, recogniser package), speech tier cache, sync watermark, deletion state machine | DataStore `device_state` | C4 | `deviceId` is written into synced documents for LWW tie-breaks. The rest is not synced | Install lifetime | Delete account; delete local data; uninstall | **Excluded.** A restored `deviceId` would collide (§6.5) |
| 14 | Sync outbox | `sync_outbox` | C3 (transient) | Source of uploads | Until acknowledged | Sign-out; delete account | Included in the file. Restored rows are safe because creates are idempotent (§6.5) |
| 15 | `users/{uid}/meta/sync` | Cloud only | C4 | Yes | Until deleted | Delete account | n/a |
| 16 | Firestore SDK offline cache | SDK files in app-private storage | C2/C3 | Mirror of cloud | SDK-managed | `terminate()` + `clearPersistence()` on sign-out and deletion | Excluded |
| 17 | Reminder, sync and retention jobs | WorkManager database | C4 | No | Until cancelled | Sign-out (Remove); delete account | Excluded |
| 18 | Diagnostic log (ADR-007 C), PII-free | `noBackupFilesDir/diagnostics/` (rolling 2 × 256 KB, 7 days; proposed) | C4 | **No.** Shared only by explicit user action | 7 days rolling | Delete account; delete local data | Excluded |
| 19 | Export file (JSON) | Written directly to a user-chosen location through the Storage Access Framework | C1–C3 | No | User-controlled | User-controlled | n/a |
| 20 | Bundled content pack v1 | APK assets plus read-only Room tables | C5 | No | App version | App update | n/a |
| 21 | Legacy local data (JSON stores, mentor history, `filesDir/recordings`, `backups/`, `updates/`) | Legacy paths | C1–C3 | No | Migrated (profile + vocabulary, D-19), then **deleted** after successful migration | Migration step; delete local data | Excluded once legacy paths are removed |
| 22 | Legacy cloud data (19 user subcollections, legacy profile fields, `applicationUpdateEvents`) | Firestore | C1–C3 | Legacy | Read + delete only during the migration window, then deleted (§5.6) | Client migration cleanup; account deletion; operator cleanup (L2) | n/a |

### 3.3 Never stored in the cloud (normative; enforced by SEC-REQ-10)

Audio (in any form), transcripts (R1; R2 only through the separate opt-in path), recognition partials, **file paths or device paths**, email address, free text derived from speech (flagged phrases, filler examples, quoted words), consent records, precise location, contacts, advertising ID, and the hardware identity of the device.

---

## 4. Authentication and App Check

### 4.1 Model (RECOMMENDATION, depends on D-05)

| Mode | Identity | Network | Data home |
|---|---|---|---|
| **No account** (default on first run) | Local `profileId` (random UUID v4) | **None** initiated by SpeakCraft or by the Firebase SDKs (SEC-REQ-02). The only exception is OS-level speech services after T3a consent | Room + DataStore on the device |
| **Signed in** (optional) | Firebase Auth uid (Google provider) | Firestore sync through `SyncWorker` (ADR-005, ADR-014) | Local source of truth plus cloud copy of C2/C3 |

- **Firebase anonymous auth is not used.** It would create cloud accounts and network traffic for users who never asked for an account. (RECOMMENDATION)
- Firebase components (Auth, Firestore, App Check) are initialised **lazily** when the user signs in, or at start-up for users already signed in, so that no-account mode makes no traffic. INFERRED that this is achievable; it must be verified by traffic capture (TC-SEC-002).

### 4.2 Google sign-in through Credential Manager

| Step | Rule |
|---|---|
| Explicit sign-in | Use `GetSignInWithGoogleOption` from the sign-in screen (SCR-ACC-01) or from Settings → Account. **No sign-in prompt appears on cold start** for users who never signed in |
| Silent restore | Use `GetGoogleIdOption(filterByAuthorizedAccounts = true, autoSelect = true)` **only** when this install has a linked uid whose Firebase session is missing |
| Token exchange | `GoogleAuthProvider.getCredential(idToken, null)` → `signInWithCredential`. The web client id is resolved at runtime, and a missing value maps to the error "sign-in unavailable in this build" (§4.6; ENG-01 §3) |
| Nonce | Set a per-request nonce on the Google ID option where Credential Manager and Firebase Auth support it end to end. Verify in Stage B. (RECOMMENDATION) |
| Firebase project | Only the **Google** provider is enabled. Email/password, phone and anonymous are disabled (SEC-REQ-03; evidence TC-SEC-049) |
| First sign-in data adoption (ADR-014 §7) | If the local profile has data and the cloud account is empty, re-key local `profileId` → uid and upload. If **both** hold data, or the local profile was previously linked to a **different** uid, ask explicitly: "Add this device's practice history to ▢▢▢@gmail.com?" with the choices *Merge* / *Keep separate on this device*. The email is masked. This prevents another person's history on a shared device being merged into an account silently (THR-02) |

### 4.3 Session

- The Firebase Auth SDK persists the session in app-private storage. ID tokens (about 1 hour) are refreshed by the SDK. INFERRED (SDK behaviour).
- One process-scoped `AuthRepository` (in the `Application`-owned `AppContainer`, ADR-001) exposes the auth state as a `StateFlow`. This fixes the current pattern, where a flow is recreated on every recomposition (`AppNavigation.kt:244`).
- If token refresh fails with "user disabled or deleted" (for example, deleted from another device), the app stops sync and shows "You've been signed out". Local data is kept as a local profile until the user chooses otherwise (§6.4).

### 4.4 Sign-out and local cleanup (RECOMMENDATION; default behaviour is decision SD-01)

Sequence:

1. Cancel the unique `SyncWorker`. If online, offer a bounded final flush (≤ 10 s; Proposed).
2. Show a dialog with two explicit actions:
   - **Remove from this device** (primary). Wipes the profile partition (§6.4 table).
   - **Keep on this device.** The data stays as a local profile marked "previously linked".
   - If `sync_outbox` is not empty, the dialog states: "N practice items haven't been backed up yet and will be lost if you remove them."
3. `credentialManager.clearCredentialState(...)`, then `FirebaseAuth.signOut()`.
4. `FirebaseFirestore.terminate()`, then `clearPersistence()`. The cloud mirror is removed in both branches.
5. Execute the chosen branch, then return to the no-account Today screen.

Data is kept only if the user chooses "Keep on this device"; otherwise it is wiped. Rationale: shared and second-hand devices (THR-02). The cost is a possible loss of unsynced items, which the warning in step 2 mitigates.

### 4.5 Re-authentication before account deletion

- Re-authentication runs **before any data is deleted**. This is the root-cause fix for the current partial-deletion risk (FND-SEC-03).
- Flow: `GetSignInWithGoogleOption` (account chooser) → `currentUser.reauthenticate(credential)` → assert that `reauthenticatedUser.uid == currentUser.uid`, otherwise abort with "That's a different account" → start the deletion state machine (§9.3).
- If `FirebaseAuthRecentLoginRequiredException` occurs at the final `delete()` step, re-authenticate again and resume. Do not restart.

### 4.6 Error mapping (raw exception text is never shown; SEC-REQ-06)

| Condition (exception) | User message (intent; final copy by UX) | Action | Log code |
|---|---|---|---|
| `GetCredentialCancellationException` | (none) | Return silently | `AUTH_CANCELLED` |
| `NoCredentialException` | "No Google account is available on this device. You can keep practising without signing in." | Link to the system account settings | `AUTH_NO_CREDENTIAL` |
| `GetCredentialInterruptedException` | "Sign-in was interrupted. Try again." | Retry | `AUTH_INTERRUPTED` |
| Provider configuration error, or web client id missing | "Sign-in isn't available in this version." | Hide the sign-in entry | `AUTH_CONFIG` |
| `FirebaseNetworkException` | "You're offline. Your practice is saved on this device." | Retry later | `AUTH_OFFLINE` |
| `FirebaseAuthInvalidCredentialsException` | "Sign-in failed. Please try again." | Retry | `AUTH_INVALID_CREDENTIAL` |
| `FirebaseAuthInvalidUserException` | "This account is no longer available." | Treat as signed out (§4.3) | `AUTH_USER_INVALID` |
| `FirebaseAuthRecentLoginRequiredException` | "Please confirm it's you." | Re-authentication flow (§4.5) | `AUTH_REAUTH_REQUIRED` |
| `FirebaseTooManyRequestsException` | "Too many attempts. Try again later." | Back off | `AUTH_RATE_LIMITED` |
| App Check token failure during sync | (non-blocking banner) "Backup is temporarily unavailable." | Exponential back-off | `APPCHECK_FAILED` |
| Anything else | "Something went wrong. Your practice on this device is safe." | Retry | `AUTH_UNKNOWN:<class simple name>` |

### 4.7 App Check (ADR-008)

| Item | Target | Label |
|---|---|---|
| Debug-token injection | **Removed.** No code reads `FIREBASE_APPCHECK_DEBUG_TOKEN` or any other extra from launch intents (current: `AppCheckConfig.kt:13,25-36`) | RECOMMENDATION |
| Debug provider | `firebase-appcheck-debug` becomes `debugImplementation`, with the provider factory in the `debug` source set. Developers register the logcat-printed debug token in the **dev** Firebase project only (ENG-01 ED-01). Debug tokens are treated as secrets and never committed | RECOMMENDATION |
| Release provider | **Play Integrity** once the app is linked in Play Console (ADR-004 B). The reCAPTCHA provider and its site-key resource (`firebase_applet_config.xml:4`) are removed; whether its Android artifact exists was UNKNOWN in the audit | RECOMMENDATION |
| Enforcement | **Not enforced** while any sideloaded channel (GitHub APKs) is active. Play Integrity is expected to fail app recognition for builds not installed from Play (INFERRED), so enforcement would lock those testers out. Enforce Firestore only when distribution is Play-only **and** console metrics show ≥ 98% verified requests over 14 consecutive days (**Proposed — requires approval**; SD-06) | RECOMMENDATION |
| Until enforcement | Per-user data protection relies on **Firebase Auth plus security rules** (§5), which are the real control. App Check reduces automated abuse (THR-06, THR-19) | VERIFIED principle (ADR-008 §3) |
| CI and emulator | Rules tests run on the Firestore emulator with a `demo-` project id, so App Check is not involved | RECOMMENDATION |

---

## 5. Authorization and user-data isolation

### 5.1 Principles

1. **Single principal.** `request.auth.uid` is the only authority. The app builds paths only from `FirebaseAuth.currentUser.uid`, never from user input or intent data.
2. **Default deny.** Only `users/{uid}/…` is reachable, and only by its owner.
3. **No shared collections and no admin role in R1–R2.** Content ships in the APK (NG-10). Custom claims grant nothing. `users` cannot be listed by anyone.
4. **No trusted aggregates in the cloud** (ADR-014 §2). Streaks, totals and trends are derived on the device from immutable facts. A user who forges facts affects only their own data. There are no leaderboards (NG-06).
5. **Allow-list schemas.** Every write is checked with `hasAll` + `hasOnly`, types, enums, ranges and string sizes. **Every** list element is validated (bounded size, every index checked, or `list.hasOnly(enum)`).
6. **Server time for server stamps.** `updatedAt` and `syncedAt` must equal `request.time`, which means the client sends `FieldValue.serverTimestamp()`. Device-observed times (`startedAt`, `occurredAt`) are bounded to `[now − 400 d, now + 5 min]` so that offline-captured facts still sync. The bounds are **Proposed**.
7. **Immutable facts.** `attempts` and `events` are create-only. Deletes are allowed for data rights.

### 5.2 Target access matrix (PROPOSED)

| Path | get / list | create | update | delete | Key notes |
|---|---|---|---|---|---|
| `users/{uid}` | owner / **denied** | owner, schema v1, server-stamped | owner; mutable keys only; `createdAt` immutable; one-time legacy migration (§5.3) | owner | No email. `displayName` ≤ 40 characters and optional |
| `users/{uid}/attempts/{attemptId}` | owner / owner | owner; UUID id == `attemptId` field; full schema | **denied** | owner | Metrics and feedback **codes** only. No transcript, audio, path or free text |
| `users/{uid}/plans/{dateKey}` | owner / owner | owner; `YYYY-MM-DD` key; ≤ 4 steps, every step validated | owner (LWW; `deviceId` + server `updatedAt`) | owner | — |
| `users/{uid}/words/{wordId}` | owner / owner | owner; slug id; SM-2 field ranges | owner | owner | Merge rule enforced on the client (ADR-014) |
| `users/{uid}/events/{eventId}` | owner / owner | owner; UUID id; type-specific payload | **denied** | owner | Idempotency key = document id |
| `users/{uid}/meta/sync` | owner | owner; only document id `sync` | owner | owner | Schema version, migration state, history-clear and deletion markers |
| `users/{uid}/transcripts/{attemptId}` (R2 only, D-16) | — | — | — | — | Not present in R1 rules (§7.5) |
| Legacy user subcollections | owner (L1 window only) | denied | denied | owner (L1 window only) | §5.6 |
| Everything else (shared, admin, release collections) | denied | denied | denied | denied | Removed at L1 / L2 |

### 5.3 Proposed rules — illustrative pseudocode

> **PROPOSED — illustrative only.** Not compiled, emulated or deployed. Field names are placeholders until ARC-03 fixes the schema, and enumerations (feedback codes, structure elements) come from ARC-05. Final rules are written in Stage B together with the TC-SEC-020…038 emulator suite.

```javascript
rules_version = '2';
// PROPOSED (SEC-01 §5.3). Illustrative; not syntax-checked or tested in Stage A.
service cloud.firestore {
  match /databases/{database}/documents {

    // 1. Default deny: anything not matched below is inaccessible.
    match /{document=**} { allow read, write: if false; }

    // ---------- helpers ----------
    function signedIn()          { return request.auth != null; }
    function isOwner(uid)        { return signedIn() && request.auth.uid == uid; }
    function d()                 { return request.resource.data; }
    function isUuid(s)           { return s is string && s.matches('^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'); }
    function isSlug(s, max)      { return s is string && s.size() >= 1 && s.size() <= max && s.matches('^[a-z0-9][a-z0-9_\\-]*$'); }
    function isDateKey(s)        { return s is string && s.matches('^20[0-9]{2}-[01][0-9]-[0-3][0-9]$'); }
    function isServerNow(t)      { return t == request.time; }            // client sends FieldValue.serverTimestamp()
    function isClientPast(t)     { return t is timestamp
                                     && t <= request.time + duration.value(5, 'm')
                                     && t >= request.time - duration.value(400, 'd'); }
    function isIntIn(v, lo, hi)  { return v is int && v >= lo && v <= hi; }
    function isNumIn(v, lo, hi)  { return (v is int || v is float) && v >= lo && v <= hi; }
    function optNum(m, k, lo, hi){ return !(k in m) || isNumIn(m[k], lo, hi); }
    function optInt(m, k, lo, hi){ return !(k in m) || isIntIn(m[k], lo, hi); }
    function unchanged(k)        { return d()[k] == resource.data[k]; }
    function keysExactly(req, opt) { return d().keys().hasAll(req) && d().keys().hasOnly(req.concat(opt)); }
    // Full list validation without loops: bounded size, and every possible index checked.
    function uuidList4(l)        { return l is list && l.size() <= 4
                                     && (l.size() < 1 || isUuid(l[0])) && (l.size() < 2 || isUuid(l[1]))
                                     && (l.size() < 3 || isUuid(l[2])) && (l.size() < 4 || isUuid(l[3])); }

    match /users/{uid} {
      allow get:    if isOwner(uid);
      allow list:   if false;                                    // no enumeration by anyone
      allow create: if isOwner(uid) && validProfile()
                    && isServerNow(d().createdAt) && isServerNow(d().updatedAt);
      allow update: if isOwner(uid) && validProfile() && isServerNow(d().updatedAt) && (
                       // (a) one-time migration of a legacy (pre-redesign) profile, which has no schemaVersion (§5.6, D-19)
                       (!('schemaVersion' in resource.data) && isServerNow(d().createdAt))
                       // (b) normal update of a v1 profile
                    || ('schemaVersion' in resource.data && unchanged('createdAt')
                        && d().diff(resource.data).affectedKeys().hasOnly(
                             ['displayName','level','goal','dailyMinutes','reminderTime','timeZone',
                              'prefs','updatedAt','deviceId'])) );
      allow delete: if isOwner(uid);

      function validProfile() {
        return keysExactly(['schemaVersion','level','goal','dailyMinutes','createdAt','updatedAt','deviceId'],
                           ['displayName','reminderTime','timeZone','prefs'])
          && d().schemaVersion == 1
          && d().level in ['L1','L2','L3','L4','L5']                          // D-17 (5 levels)
          && d().goal in ['EVERYDAY','WORK','INTERVIEW','PRESENT','CLEAR']      // RD-16 CAP-06 tracks
          && d().dailyMinutes in [5, 10, 15]
          && (!('displayName' in d())  || (d().displayName is string && d().displayName.size() <= 40))
          && (!('reminderTime' in d()) || (d().reminderTime is string && d().reminderTime.matches('^[0-2][0-9]:[0-5][0-9]$')))
          && (!('timeZone' in d())     || (d().timeZone is string && d().timeZone.size() <= 64))
          && (!('prefs' in d())        || validPrefs(d().prefs))
          && isUuid(d().deviceId);
          // No 'email', 'streakDays', score or counter fields: aggregates are never trusted in the cloud.
      }
      function validPrefs(p) {
        return p is map && p.keys().hasOnly(['dailyWordGoal','reviewReminders','recordingRetention'])
          && optInt(p, 'dailyWordGoal', 0, 20)
          && (!('reviewReminders' in p) || p.reviewReminders is bool)
          && (!('recordingRetention' in p) || p.recordingRetention in ['AFTER_FEEDBACK','D7','D30','KEEP']);
      }

      // ---------- attempts: immutable metrics-only facts ----------
      match /attempts/{attemptId} {
        allow get, list: if isOwner(uid);       // the client always queries with limit() plus a watermark (FND-DATA-05)
        allow create:    if isOwner(uid) && isUuid(attemptId) && validAttempt(attemptId);
        allow update:    if false;
        allow delete:    if isOwner(uid);       // Clear history / account deletion
      }
      function validAttempt(id) {
        return keysExactly(['schemaVersion','attemptId','templateId','contentId','contentVersion','ruleSetVersion',
                            'result','startedAt','localDate','durationMs','metrics','syncedAt','deviceId'],
                           ['estimates','feedbackCodes','planDate','stepIndex','targetWordId'])
          && d().schemaVersion == 1 && d().attemptId == id
          && d().templateId in ['TPL-READ','TPL-REPEAT','TPL-ANSWER','TPL-STRUCT','TPL-STORY','TPL-ROLE']
          && isSlug(d().contentId, 64) && isIntIn(d().contentVersion, 1, 100000)
          && d().ruleSetVersion is string && d().ruleSetVersion.matches('^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$')
          && d().result in ['EVALUATED','INCONCLUSIVE','PASS','FAIL']
          && isClientPast(d().startedAt) && isDateKey(d().localDate)
          && isIntIn(d().durationMs, 0, 600000)
          && validMetrics(d().metrics)
          && (!('estimates' in d())     || validEstimates(d().estimates))
          && (!('feedbackCodes' in d()) || (d().feedbackCodes is list && d().feedbackCodes.size() <= 4
                                            && d().feedbackCodes.hasOnly(['FB_PACE_FAST','FB_PACE_SLOW','FB_LONG_PAUSES',
                                               'FB_FILLERS','FB_TARGET_WORD_MISSING','FB_STRUCTURE_MISSING' /* …ARC-05 enum */])))
          && (!('planDate' in d())      || isDateKey(d().planDate))
          && optInt(d(), 'stepIndex', 0, 3)
          && (!('targetWordId' in d())  || isSlug(d().targetWordId, 64))
          && isServerNow(d().syncedAt) && isUuid(d().deviceId);
          // Keys such as 'transcript', 'audio', 'localMediaPath', 'filePath' and 'email' are impossible: hasOnly rejects them.
      }
      function validMetrics(m) {             // measured metrics (ARC-05)
        return m is map
          && m.keys().hasOnly(['speakingMs','speechRatio','wpm','longPauses','wordCount','targetWordUsed','referenceMatchPct'])
          && optInt(m,'speakingMs',0,600000) && optNum(m,'speechRatio',0,1) && optNum(m,'wpm',0,400)
          && optInt(m,'longPauses',0,500)    && optInt(m,'wordCount',0,5000)
          && (!('targetWordUsed' in m) || m.targetWordUsed is bool)
          && optNum(m,'referenceMatchPct',0,100);
      }
      function validEstimates(e) {           // estimated metrics; counts only, never the flagged text
        return e is map
          && e.keys().hasOnly(['fillersPerMin','lexicalVariety','grammarFlagCount','hedgeCount','structureElements'])
          && optNum(e,'fillersPerMin',0,120) && optNum(e,'lexicalVariety',0,1)
          && optInt(e,'grammarFlagCount',0,100) && optInt(e,'hedgeCount',0,200)
          && (!('structureElements' in e) || (e.structureElements is list && e.structureElements.size() <= 8
               && e.structureElements.hasOnly(['SITUATION','TASK','ACTION','RESULT','POINT','REASON','EXAMPLE','POINT_AGAIN'])));
      }

      // ---------- events: immutable facts (idempotency key = document id) ----------
      match /events/{eventId} {
        allow get, list: if isOwner(uid);
        allow create:    if isOwner(uid) && isUuid(eventId) && validEvent(eventId);
        allow update:    if false;
        allow delete:    if isOwner(uid);
      }
      function validEvent(id) {
        let e = d();
        let p = e.payload;
        return keysExactly(['schemaVersion','eventId','type','occurredAt','localDate','payload','syncedAt','deviceId'], [])
          && e.schemaVersion == 1 && e.eventId == id && p is map
          && isClientPast(e.occurredAt) && isDateKey(e.localDate)
          && isServerNow(e.syncedAt) && isUuid(e.deviceId)
          && (
               (e.type == 'SESSION_COMPLETED'
                  && p.keys().hasAll(['planDate','stepsCompleted']) && p.keys().hasOnly(['planDate','stepsCompleted','attemptIds'])
                  && isDateKey(p.planDate) && isIntIn(p.stepsCompleted, 1, 4)
                  && (!('attemptIds' in p) || uuidList4(p.attemptIds)))
            || (e.type == 'WORD_STAGE_PASSED'
                  && p.keys().hasAll(['wordId','stage']) && p.keys().hasOnly(['wordId','stage','attemptId'])
                  && isSlug(p.wordId, 64) && isIntIn(p.stage, 1, 5)
                  && (!('attemptId' in p) || isUuid(p.attemptId)))
            || (e.type == 'WORD_REVIEWED'
                  && p.keys().hasAll(['wordId','grade']) && p.keys().hasOnly(['wordId','grade','attemptId'])
                  && isSlug(p.wordId, 64) && isIntIn(p.grade, 0, 5)
                  && (!('attemptId' in p) || isUuid(p.attemptId)))
             );
      }

      // ---------- plans: mutable, last-writer-wins ----------
      match /plans/{dateKey} {
        allow get, list: if isOwner(uid);
        allow create:    if isOwner(uid) && isDateKey(dateKey) && validPlan(dateKey) && isServerNow(d().updatedAt);
        allow update:    if isOwner(uid) && validPlan(dateKey) && isServerNow(d().updatedAt) && unchanged('dateKey');
        allow delete:    if isOwner(uid);
      }
      function validPlan(key) {
        let s = d().steps;
        return keysExactly(['schemaVersion','dateKey','generatorVersion','minutes','steps','updatedAt','deviceId'], ['completedAt'])
          && d().schemaVersion == 1 && d().dateKey == key
          && d().generatorVersion is string && d().generatorVersion.size() <= 16
          && d().minutes in [5, 10, 15]
          && s is list && s.size() >= 1 && s.size() <= 4
          && validStep(s[0])
          && (s.size() < 2 || validStep(s[1])) && (s.size() < 3 || validStep(s[2])) && (s.size() < 4 || validStep(s[3]))
          && (!('completedAt' in d()) || isClientPast(d().completedAt))
          && isUuid(d().deviceId);
      }
      function validStep(st) {
        return st is map && st.keys().hasAll(['kind','contentId','status']) && st.keys().hasOnly(['kind','contentId','status','attemptId'])
          && st.kind in ['SPEAK','WORD_LEARN','WORD_REVIEW','WARMUP']
          && isSlug(st.contentId, 64) && st.status in ['PENDING','DONE','SKIPPED']
          && (!('attemptId' in st) || isUuid(st.attemptId));
      }

      // ---------- words: vocabulary item state ----------
      match /words/{wordId} {
        allow get, list:      if isOwner(uid);
        allow create, update: if isOwner(uid) && isSlug(wordId, 64) && validWord(wordId) && isServerNow(d().updatedAt);
        allow delete:         if isOwner(uid);
      }
      function validWord(id) {
        return keysExactly(['schemaVersion','wordId','stage','status','updatedAt','deviceId'],
                           ['nextReviewDate','intervalDays','ease','reviews','lapses'])
          && d().schemaVersion == 1 && d().wordId == id
          && isIntIn(d().stage, 0, 5) && d().status in ['NEW','LEARNING','REVIEW','SUSPENDED']
          && (!('nextReviewDate' in d()) || isDateKey(d().nextReviewDate))
          && optInt(d(),'intervalDays',0,3650) && optNum(d(),'ease',1.3,3.0)
          && optInt(d(),'reviews',0,100000) && optInt(d(),'lapses',0,100000)
          && isUuid(d().deviceId);
      }

      // ---------- meta/sync ----------
      match /meta/{docId} {
        allow get:            if isOwner(uid) && docId == 'sync';
        allow create, update: if isOwner(uid) && docId == 'sync' && validSyncMeta() && isServerNow(d().updatedAt);
        allow delete:         if isOwner(uid);
      }
      function validSyncMeta() {
        return keysExactly(['schemaVersion','updatedAt','deviceId'],
                           ['maxDataSchemaVersion','legacyMigration','historyClearedAt','wordsResetAt','deletionRequestedAt'])
          && d().schemaVersion == 1 && isUuid(d().deviceId)
          && optInt(d(),'maxDataSchemaVersion',1,100)
          && (!('legacyMigration' in d())     || d().legacyMigration in ['NOT_STARTED','DONE','SKIPPED'])
          && (!('historyClearedAt' in d())    || d().historyClearedAt is timestamp)
          && (!('wordsResetAt' in d())        || d().wordsResetAt is timestamp)
          && (!('deletionRequestedAt' in d()) || d().deletionRequestedAt is timestamp);
      }

      // ---------- legacy subcollections: L1 migration window only (§5.6); removed at L2 ----------
      match /{legacyCol}/{docId} {
        allow get, list, delete: if isOwner(uid) && legacyCol in [
          'speakingSessions','speechFeedback','vocabulary','dailyActivities','learningProgress','enrollments',
          'youtubeSources','extractedTechniques','conversationSessions','realWorldTasks','activityProgress',
          'activityAttempts','savedActivities','dailyPracticePlans','notifications','learningEvents',
          'vocabularyProgress','quizAttempts','learningSnapshots'];
        // create/update: not granted, so default deny applies. Rules are OR-ed, so this block does not
        // widen attempts/plans/words/events/meta, whose own blocks govern them.
      }
    }

    // ---------- legacy top-level per-user telemetry: L1 window, delete-only for the owner ----------
    match /applicationUpdateEvents/{eventId} {
      allow get, list, delete: if signedIn() && resource.data.userId == request.auth.uid;
    }
  }
}
```

**Size and volume controls.** Rules cannot count documents, so volume abuse (THR-06) is bounded by:

- per-field size limits and document-id formats (above);
- the app's own bounded queries (`limit()` plus the `updatedAt` watermark);
- App Check enforcement once Play-only (SD-06);
- Firebase API-key restrictions and GCP budget alerts (SEC-REQ-37, 38; ENG-01 §9).

The residual risk is accepted at **Medium** until App Check is enforced.

**Idempotency note for ARC-04.** A retried create of an existing immutable document is evaluated as an *update* and is therefore denied. Because one denied write fails the whole batch, `SyncWorker` must:

- write immutable facts in small batches;
- on `PERMISSION_DENIED`, `get()` the document;
- mark the outbox row synced if the stored document has the same `attemptId`/`eventId`.

**Optional hardening (SD-08).** Deny creates while `meta/sync.deletionRequestedAt` exists. This costs one extra document read per write evaluation. It is not proposed for R1.

### 5.4 App-side authorization rules (RECOMMENDATION)

- Repositories accept no uid parameter. They read it from `AuthRepository`. ViewModels never touch Firestore (ADR-002).
- No collection-group queries, and no queries outside `users/{uid}`.
- Unknown fields in downloaded documents are ignored. A document with a `schemaVersion` above what the client supports is not overwritten; the client pauses upload and asks the user to update (ENG-01 §8.4).

### 5.5 Rules test plan (emulator, runs in CI)

| Aspect | Plan |
|---|---|
| Harness | `@firebase/rules-unit-testing` with the Node built-in test runner. The existing `firestore.test.js:1-6` already uses both, but has no `package.json`, so it is not runnable (audit §4.5). Formalise it with a committed `package.json` and **lockfile**. This is test tooling; confirm it under ADR-006 scope (RECOMMENDATION) |
| Emulator | Firestore emulator through `firebase emulators:exec` with project id `demo-speakcraft`. The `demo-` prefix keeps it offline from real projects. `firebase.json:9-13` already configures the emulator on port 8085 |
| Structure | One "valid baseline" builder per collection. Generated negative cases for each field: missing required key, unknown key, wrong type, below/above range, oversize string, and an invalid element at **each** list index |
| Suites | TC-SEC-020 … TC-SEC-038 (§13) |
| Coverage | The emulator rule-coverage report is uploaded as a CI artefact. Target: every `allow` expression evaluated both true and false (**Proposed**) |
| Gate | The `rules-test` job is a **required status check** on `main` (ENG-01 §4.3). A rules change without matching tests fails review (CODEOWNERS) |
| Deploy linkage | Rules are deployed only from a commit whose `rules-test` passed. The deployed ruleset is recorded at each release (SEC-REQ-12; ENG-01 ED-06) |

### 5.6 Legacy collection retirement plan

| Phase | When | Rules change | Data actions | Notes |
|---|---|---|---|---|
| **L0 Containment** | Immediately on approval (R0) | In the **current** rules, set client writes to `false` for `applicationReleases`, `adminUsers`, `adminAnnouncements`, `adminSettings`, `lessons`, `learningChallenges`, `learningModules`, `communicationExercises`, `youtube*`, `videoTranscripts` and shared `extractedTechniques`; remove `users` `list` for admins (`firestore.rules:334`). **Do not widen** the legacy `speakingSessions` allow-list; widening would re-enable transcript upload (FND-SEC-05) | None | CI's Admin SDK publisher bypasses rules, so release publishing still works until ADR-004 retires it |
| **L1 Transition** | With R1 | Deploy §5.3: new collections, legacy user subcollections **read + delete only**, owner delete on `applicationUpdateEvents`, all shared and admin collections denied (blocks removed) | R1 client migrates profile + vocabulary (D-19) through the one-time profile migration (§5.3 (a)), then deletes the legacy subcollections it has migrated. Account deletion also covers legacy paths | **Ordering:** rules are deployed before the R1 app is released, because the rules are additive for R1 (ENG-01 §8.4). If D-15 keeps the applicationId, publish one final legacy `applicationReleases` record pointing to R1 **before** L1 removes that collection. Debug-signed legacy installs cannot update in place and must reinstall, so their local legacy data is lost (state this in the release notes) |
| **L2 Retirement** | End of the migration window (**Proposed:** R1 + 90 days; D-19) | Remove the legacy blocks and the `applicationUpdateEvents` block (default deny) | Operator cleanup with a privileged one-off script (new tooling; needs approval and a runbook): delete all shared/admin collections, `applicationUpdateEvents`, and legacy user subcollections of users who never upgraded. Revoke the release-publishing service account (ENG-01 §6) | Legacy data has no purpose after the window. Keeping it conflicts with data minimisation |
| **L3 Hygiene** | After L2 | — | Delete `firebase-blueprint.json` stale entries and replace `firestore.test.js` with the new suite | — |

---

## 6. Local data protection

### 6.1 Storage locations (RECOMMENDATION)

| Rule | Detail |
|---|---|
| App-private only | `filesDir`, `noBackupFilesDir`, `databases/`, DataStore. No external storage, MediaStore or world-readable modes. No `READ/WRITE_EXTERNAL_STORAGE` |
| Recordings | `noBackupFilesDir/recordings/<profileId>/` (ADR-015). Never in `cacheDir`, so the system cannot evict them before their retention ends |
| Sensitive text | `sensitive.db` (transcripts) is a separate Room database excluded from backup (§3.2 #2) |
| Exports | Written through the Storage Access Framework (`ACTION_CREATE_DOCUMENT`). No FileProvider and no temporary copy outside `cacheDir` |
| FileProvider | Removed together with the updater (ADR-004). If "Share diagnostics" uses `ACTION_SEND`, re-introduce it with a single path `cache-path name="diagnostics" path="diagnostics/"` (fixes FND-SEC-12) |

### 6.2 Backup decision (RECOMMENDATION; decision SD-02)

| Option | Effect | Assessment |
|---|---|---|
| **A. `allowBackup="true"` with include-only rules** (recommended) | Only `learning.db` (C2/C3) and the `settings` DataStore are backed up and transferred. Recordings, transcripts, device state, Firebase SDK stores, the Firestore cache, WorkManager and diagnostics are not | No-account users keep their progress when they change phones, which PP-6 needs. C1 data never leaves the device. Android 9+ cloud backups are end-to-end encrypted with the device screen lock (INFERRED, platform behaviour) |
| B. `allowBackup="false"` | No cloud backup | Simplest. No-account users lose all history on a device change. **Device-to-device transfer still occurs for apps targeting API 31+** unless excluded in `dataExtractionRules` (INFERRED, platform behaviour change), so the extraction rules are still required |

### 6.3 Backup and data-extraction rules specification (PROPOSED)

Include-only semantics: when any `<include>` is present, nothing else is backed up (INFERRED, platform documentation). `fullBackupContent` applies to Android 11 and earlier; `dataExtractionRules` applies to Android 12 and later. Both are required because minSdk is below 31. File names follow §3.2 and ARC-03 confirms them.

```xml
<!-- res/xml/data_extraction_rules.xml (Android 12+) — PROPOSED -->
<data-extraction-rules>
  <cloud-backup>
    <include domain="database" path="learning.db"/>
    <include domain="file" path="datastore/settings.preferences_pb"/>
  </cloud-backup>
  <device-transfer>
    <include domain="database" path="learning.db"/>
    <include domain="file" path="datastore/settings.preferences_pb"/>
  </device-transfer>
</data-extraction-rules>

<!-- res/xml/backup_rules.xml (Android 11 and lower) — PROPOSED -->
<full-backup-content>
  <include domain="database" path="learning.db"/>
  <include domain="file" path="datastore/settings.preferences_pb"/>
</full-backup-content>
```

Explicitly **not** included, and verified absent by TC-SEC-012 and TC-SEC-013:

- `sensitive.db*`
- `no_backup/**` (recordings, diagnostics)
- `datastore/device_state.preferences_pb`
- Firebase Auth, App Check and Installations shared preferences
- Firestore cache files
- the WorkManager database
- legacy paths

The SQLite `-wal`/`-shm` side files need confirming during implementation. The system stops the app before Auto Backup runs (INFERRED), and the backup must restore a consistent database (TC-SEC-013).

### 6.4 Per-profile partitioning and wipe matrix

Partitioning (SEC-REQ-18):

- Every learning row carries `profile_id`, and every DAO query is scoped to the active profile.
- The recordings folder is per profile.
- Profile-scoped preferences are namespaced.
- WorkManager work is tagged with the profile id.
- Only one profile is active at a time.

| Store | Sign-out → Remove | Sign-out → Keep | Clear history | Reset vocabulary | Delete account | Delete local data (no account) |
|---|---|---|---|---|---|---|
| `learning.db` profile rows | ✔ wipe | keep (re-key to local profile) | attempts, metrics, plans, session events | words, reviews, word events | ✔ | ✔ |
| `sensitive.db` transcripts | ✔ | keep | ✔ | — | ✔ | ✔ |
| Recordings folder | ✔ | keep | ✔ | — | ✔ | ✔ |
| `settings` (profile keys) | ✔ | keep | — | — | ✔ | ✔ |
| `device_state` | keep `deviceId`; clear watermark | clear watermark | — | — | ✔ (after the deletion state completes) | ✔ |
| Firestore cache | ✔ | ✔ | n/a | n/a | ✔ | n/a |
| Auth / Credential Manager | ✔ | ✔ | — | — | ✔ | n/a |
| WorkManager (sync, reminders) | ✔ | sync only | — | — | ✔ | ✔ |
| Diagnostics | keep (PII-free) | keep | — | — | ✔ | ✔ |
| Cloud documents | untouched | untouched | attempts, session events, plans + `historyClearedAt` marker | words, word events + `wordsResetAt` marker | **all** (§9.3) | n/a |

### 6.5 Restore-from-backup handling (RECOMMENDATION; TC-SEC-050)

If `learning.db` exists but `device_state` has no `deviceId`, the app treats this as a restore:

1. Generate a new `deviceId`.
2. Reset the sync watermark.
3. Mark the profile "sign in to resume backup".
4. Keep outbox rows. Re-uploads of immutable facts resolve as already synced (§5.3 idempotency note).

Attempts restored without transcripts or recordings show "Recording and transcript not available on this device".

### 6.6 Database encryption decision

**RECOMMENDATION: no app-level database or file encryption in R1. SQLCipher is not proposed** (it would be a new dependency; ADR-002 Risks).

| Factor | Reasoning |
|---|---|
| Existing protection | The Linux UID sandbox isolates app-private storage from other apps. File-based encryption protects data at rest on locked devices for most of the target base (A-5) |
| Threats app-level encryption would address | Forensic extraction from rooted devices, or devices without storage encryption (THR-01). On an unlocked or rooted device the app's own Keystore key is usable by an attacker running as the app, so the marginal gain is small |
| Backup exposure | Removed by design through include-only rules and `sensitive.db` exclusion (§6.3) |
| Costs | New dependency or custom crypto; performance; data-loss risk if a Keystore key is invalidated (for example after a lock-screen change); migration complexity |
| Residual risk | **Low–Medium**, accepted (THR-01). C1 exposure is time-boxed by the 30-day recording retention |
| Revisit triggers | Transcript cloud sync (D-16); entry into regulated markets; evidence that a material share of users run devices without storage encryption. The app can check this locally; nothing is sent |
| Fallback without a new dependency | AES-GCM with an Android Keystore key over recording files, reusing the existing pattern in `AppPreferencesRepository.kt:51-110` |

### 6.7 Other on-device controls

- Notifications show generic reminder text only ("Time for today's 10-minute session"), with no transcripts or feedback content. PendingIntents use `FLAG_IMMUTABLE` and explicit intents (THR-22).
- `FLAG_SECURE` is not applied. Transcripts on screen are user content; can be revisited under SD-03.
- Nothing is copied to the clipboard automatically.

---

## 7. Recordings and transcripts privacy

### 7.1 Recordings (ADR-015, referenced not restated)

ADR-015 is authoritative for location, format, the retention options, immediate deletion of discarded or failed attempts, "never uploaded" and attempt-id-only cloud references. SEC-01 adds the enforcement requirements:

- **Retention enforcement.** A daily WorkManager job (ADR-005) **and** an app-start sweep delete expired files within 24 hours of expiry (SEC-REQ-19). `recording_file.expires_at` is set when the file is created and recomputed when the user changes the retention option.
- **Orphan sweep.** Files with no `recording_file` row, or whose row has no attempt, are deleted on app start. This fixes FND-AI-14.
- **Playback** is local only (`MediaPlayer` on the app-private file). In R1–R2 there is no share or export of audio (NG-06, NG-12; SD-04).
- **Retention changes** apply retroactively when shorter, and prospectively when longer.

### 7.2 Recogniser selection (ADR-009 tiers)

| Tier | API used | Network |
|---|---|---|
| T1 / T2 | `SpeechRecognizer.createOnDeviceSpeechRecognizer(...)` only | None by design (on-device API) |
| T3a | `SpeechRecognizer.createSpeechRecognizer(...)` (default service) **only after valid consent** | Audio streamed by the **device's speech service**, not by SpeakCraft |
| T3b | No recogniser; `AudioRecord` only | None |

`EXTRA_PREFER_OFFLINE` is advisory (ADR-009). **It is never treated as a privacy control.** The only code path that can create a non-on-device recogniser is guarded by `ConsentStore.isValid(currentDisclosureVersion, recogniserPackage)` (SEC-REQ-20; TC-SEC-015).

### 7.3 Online-transcription consent (T3a): disclosure content requirements

UX writes the copy (DES-06). These elements are **mandatory**, and TC-SEC-016 asserts each one:

| # | Required element | Notes |
|---|---|---|
| 1 | **What is sent:** "Your voice while you record an answer" | Not the account, profile, history or other recordings |
| 2 | **To whom:** the name of the device's speech service, resolved at runtime from the recogniser component label | For example "Speech Services by Google". State that its provider's own privacy policy applies and that SpeakCraft does not control it |
| 3 | **When:** only while recording in a practice session, only while this setting is on, never in the background | — |
| 4 | **Why:** this device has no offline recogniser for the selected English variant | Name the language (for example en-GB) |
| 5 | **What SpeakCraft keeps:** the transcript and recording stay on this device, under the user's retention setting. SpeakCraft uploads neither | — |
| 6 | **Alternatives:** "Audio-only mode" (delivery feedback without a transcript), and "Install offline speech" when `triggerModelDownload` is available (FS-12) | The language-pack download is performed by the OS speech provider over the network. Disclose it as such |
| 7 | **Withdrawal:** Settings → Speech & privacy → "Online transcription", effective immediately | — |
| 8 | **Choice design:** two equal-weight buttons, "Use online transcription" and "Use audio-only mode". Nothing pre-selected; no countdown; no repeated nagging (at most one re-prompt per 30 days after a decline; **Proposed**) | PP-2, PP-7 |
| 9 | **Timing:** shown before the first T3a recording (onboarding SCR-ONB or SCR-PERM-01), **never mid-recording** | — |
| 10 | **Record:** granted/declined, disclosure version, timestamp and recogniser package, stored in `device_state` (local only) | Re-consent is required when the disclosure version changes materially or the default recogniser package changes |
| 11 | **Accessibility:** readable at 200% font scale; TalkBack order matches visual order (DES-07) | — |

### 7.4 Transcripts

- Stored only in `sensitive.db`. Shown on attempt detail (SCR-PROG-04) and the session result.
- Never written to logs, notifications, the clipboard, Firestore (R1), backups or diagnostics.
- Included in the user's export (§9.1), with an export-time notice that the file contains transcripts.
- Free text derived from transcripts (flagged phrases, filler examples) never leaves the device. Cloud attempts carry only codes and counts (§5.3).

### 7.5 R2 transcript sync opt-in (D-16), constraints for a future design

- A separate subcollection `users/{uid}/transcripts/{attemptId}`, so attempt rules stay transcript-free.
- A separate toggle, off by default, with its own disclosure.
- Turning it off deletes the cloud transcripts.
- Rules must check the profile's opt-in flag and a size cap.
- This triggers the privacy review procedure (§9.7) and the encryption revisit (§6.6).

---

## 8. Secrets management and network security

ENG-01 §6 holds the operational secrets inventory (storage, scope, rotation owner). This section sets the policy.

### 8.1 Policy

| Item | Policy | Label |
|---|---|---|
| Secrets in the repository | **None.** No keystores, passwords, service-account keys, tokens or `.env` values. `.gitignore` already covers `*.jks`, `debug.keystore` and `.env` (`.gitignore:17-19`). Add `google-services.json` explicitly (it is currently absent from the repo but not ignored, audit §6.1) | VERIFIED current / RECOMMENDATION |
| `google-services.json` | Treated as **confidential configuration**. It holds a restricted API key and project identifiers, not credentials. Not committed. One per environment (ENG-01 ED-01, ED-03). Supplied to CI from the environment-scoped secret for release builds only | RECOMMENDATION |
| Firebase API key | Restricted in Google Cloud: (a) to Android apps with **package name + release (and app-signing) SHA-256/SHA-1** fingerprints; (b) to the required APIs only (Identity Toolkit, Secure Token, Cloud Firestore, Firebase Installations, App Check). Evidence is recorded at each release (TC-SEC-049) | RECOMMENDATION |
| Public identifiers | The database id and project id may remain in resources. Per-environment values go in build-type resource overlays (ENG-01 §2). The reCAPTCHA site key is removed with the provider | RECOMMENDATION |
| Signing keys | App-signing and upload keys are held by named custodians, with an encrypted offline backup in two locations. CI holds only what it must, as **environment-scoped** secrets behind required reviewers (ENG-01 §5) | RECOMMENDATION |
| Release signing config | The build **fails fast** when release signing variables are missing. No `""` defaults (current: `app/build.gradle.kts:52,54`) | RECOMMENDATION |
| Debug keystore secret | **Treat `DEBUG_KEYSTORE_BASE64` as compromised.** It signed distributed builds, and anyone who holds it can produce APKs that pass the legacy updater's certificate check for debug-signed installs. Actions: (1) never sign a distributed build with it again; (2) delete the GitHub secret; (3) remove its SHA fingerprints from the **production** Firebase Android app and OAuth client, so builds signed with it cannot use Google sign-in against prod; (4) developers each use their own local debug keystore, registered in the **dev** project only. The hard-coded `debugConfig` (`app/build.gradle.kts:56-61`) is removed in favour of the AGP default debug keystore (SEC-REQ-39) | RECOMMENDATION |
| Service account `FIREBASE_SERVICE_ACCOUNT_JSON` | Retired with the publish script (ADR-004). **Revoke the key in GCP** and delete the secret. If CI ever deploys rules, use keyless Workload Identity Federation or a narrowly scoped deployer (ENG-01 ED-06) | RECOMMENDATION |
| BuildConfig | No secrets in `BuildConfig` or resources. Remove the unused secrets Gradle plugin (`app/build.gradle.kts:6,99-102`; ADR-006) | RECOMMENDATION |
| Secret scanning | GitHub secret scanning with push protection where the repository plan allows it (availability depends on visibility and plan; verify). Otherwise a pattern-scan CI step (TC-SEC-043) | RECOMMENDATION |

### 8.2 Network security configuration (PROPOSED)

```xml
<!-- res/xml/network_security_config.xml — PROPOSED -->
<network-security-config>
  <base-config cleartextTrafficPermitted="false">
    <trust-anchors><certificates src="system"/></trust-anchors>
  </base-config>
  <debug-overrides>
    <trust-anchors><certificates src="user"/></trust-anchors>  <!-- local proxy debugging in debug builds only -->
  </debug-overrides>
</network-security-config>
```

- Manifest: `android:networkSecurityConfig="@xml/network_security_config"` and `android:usesCleartextTraffic="false"`. Today cleartext is blocked only by the targetSdk default (audit §6.6, INFERRED). An explicit configuration documents the intent and is testable (TC-SEC-042).
- **No certificate pinning.** Google endpoints rotate certificates; pinning risks outages for little gain over system trust (RECOMMENDATION).

### 8.3 Endpoints after removals (target; to be confirmed by traffic capture, TC-SEC-002 / TC-SEC-048)

| Endpoint (host family) | Purpose | Contacted by | When | Label |
|---|---|---|---|---|
| `identitytoolkit.googleapis.com`, `securetoken.googleapis.com` | Firebase Auth sign-in and token refresh | Firebase Auth SDK | Signed-in mode only | INFERRED (SDK) |
| `firestore.googleapis.com` | Sync | Firestore SDK | Signed-in mode only | INFERRED |
| `firebaseinstallations.googleapis.com`, `firebaseappcheck.googleapis.com` | Installation id and App Check tokens | Firebase SDKs | Signed-in mode, once App Check is active | INFERRED |
| Google Play services (Credential Manager, Play Integrity) | Account selection, attestation | Play services (out of process) | Sign-in, App Check | INFERRED |
| Device speech-service endpoints | Online recognition (T3a) and language-pack download | **OS speech service**, not SpeakCraft | Only after consent or an explicit download | INFERRED |
| Any further Google endpoints observed | To be listed in the data-safety inputs | Firebase or Play SDKs | — | UNKNOWN; verify |

**Removed** with ADR-003, 004, 009, 010 and 011 (VERIFIED as present today): `api.github.com` and GitHub release downloads (`GitHubReleaseClient.kt:18-52`); YouTube pages and Data API (`YouTubeIntelligenceClient.kt`); Hugging Face and GCS model URLs (`OnDeviceModelManager.kt:83-139`); Firebase AI Logic (`FirebaseAiLogicService.kt`). OkHttp can then be removed (ADR-006). `INTERNET` remains for sync.

---

## 9. Privacy controls and data rights

### 9.1 Export (FS-10; SEC-REQ-22)

- **Format:** UTF-8 JSON, `format: "speakcraft-export"`, `formatVersion: 1`. Timestamps are ISO-8601 UTC. Field names are stable and documented in FS-10.
- **Delivery:** Storage Access Framework `ACTION_CREATE_DOCUMENT` (the user chooses where it goes). Works offline and without an account, because export reads local Room. For signed-in users the app runs a pull sync first when online, and states "Includes data synced up to <time>".
- **Contents:** profile, synced preferences, attempts (metrics, estimates, feedback rendered as text plus codes, **transcripts**), plans, words, events, and the export metadata.
- **Excluded:** audio (R1; SD-04), `deviceId`, tokens, consent internals and diagnostics. The export lists what is excluded.

```json
{
  "format": "speakcraft-export",
  "formatVersion": 1,
  "exportedAt": "2026-11-20T10:15:00Z",
  "app": { "versionName": "1.2.0", "contentPackVersion": 1, "ruleSetVersion": "1.0.0" },
  "profile": { "level": "L3", "goal": "WORK", "dailyMinutes": 10, "displayName": "Sam", "createdAt": "…" },
  "preferences": { "dailyWordGoal": 3, "recordingRetention": "D30" },
  "attempts": [{
    "attemptId": "…", "templateId": "TPL-ANSWER", "contentId": "work-intro-01", "startedAt": "…",
    "durationMs": 61000, "result": "EVALUATED",
    "metrics": { "wpm": 128, "longPauses": 3, "speechRatio": 0.82 },
    "estimates": { "fillersPerMin": 4.1 },
    "feedback": [{ "code": "FB_LONG_PAUSES", "text": "…" }],
    "transcript": "…",
    "recordingOnDevice": true
  }],
  "plans": [], "words": [], "events": [],
  "notIncluded": ["audio recordings", "device identifiers", "sign-in tokens", "diagnostic log"]
}
```

### 9.2 Clear history and reset vocabulary (SEC-REQ-23)

1. A confirmation dialog states the scope (§6.4) and that the action is irreversible. Recordings and transcripts are included.
2. Local deletion runs in one Room transaction, then the files are deleted.
3. When signed in: tombstone jobs are queued in the outbox and delete the matching cloud documents with paginated queries. The job then writes `meta/sync.historyClearedAt` (or `wordsResetAt`) as a server timestamp.
4. Other devices, on their next pull, see the marker and purge local items with `startedAt ≤ marker`. Their outbox does not re-upload purged items.

### 9.3 Delete account: client-side cascade with re-authentication, verification and resumable state (SEC-REQ-05, 24)

Entry point: Settings → Account → "Delete account". The screen lists what will be deleted (cloud data, data on this device, the sign-in link with SpeakCraft) and states that the action is irreversible and that other devices will be cleared on their next sync. It requires network access.

| Phase (persisted in `device_state.deletion`) | Action | Idempotent / resumable |
|---|---|---|
| `CONFIRMED` | User confirms explicitly | — |
| `REAUTHENTICATED` | §4.5. The uid must match | Re-run if the app restarts |
| `MARKER_WRITTEN` | `meta/sync.deletionRequestedAt = serverTimestamp()`. Other devices stop uploading and offer "Finish deleting account" | Yes |
| `SYNC_STOPPED` | Cancel `SyncWorker`; clear the outbox | Yes |
| `CLOUD_SUBCOLLECTIONS` | For each of `attempts`, `events`, `plans`, `words`, the L1 legacy subcollections, and (R2) `transcripts`: query `limit(400)` → batch delete → repeat until empty, recording progress per collection. Then delete the owner's `applicationUpdateEvents` (L1 window) | Yes. The query-until-empty loop is naturally resumable |
| `VERIFIED` | For each collection, `limit(1)` returns empty from the **server** (`Source.SERVER`) | Yes |
| `CLOUD_PROFILE` | Delete `meta/sync`, then `users/{uid}` | Yes |
| `AUTH_DELETED` | `currentUser.delete()`. On `RecentLoginRequired`, go back to re-authentication and resume here | Yes |
| `LOCAL_WIPED` | Full local wipe (§6.4 "Delete account" column), including `terminate()` + `clearPersistence()` and Credential Manager state | Yes |
| `DONE` | Clear the deletion state and show a confirmation. The app returns to first run | — |

- **On app start:** if a deletion state exists, show a blocking "Finishing account deletion" screen and resume. If Auth is already deleted, skip to `LOCAL_WIPED`.
- **Residual risks:**

| Residual risk | Mitigation | Rating |
|---|---|---|
| App uninstalled or device lost mid-cascade, so the local state is lost and cloud documents remain | The marker persists. The next sign-in on any device detects it and offers to resume. The web deletion route (§9.4) is the operator backstop | Low–Medium |
| An offline device holds pending writes | Before `AUTH_DELETED`, the marker stops cooperative clients. After `AUTH_DELETED`, the refresh token is invalid and writes fail (INFERRED). The optional rule hardening is SD-08 | Low |
| Google Auto Backup copies of `learning.db` remain in the user's Google account | Disclosed in the privacy policy. The user manages them in Google account settings | Low (user-controlled) |
| Firestore point-in-time recovery or scheduled backups, if enabled, retain deleted data for their window (PITR: 7 days) | Disclose the retention window; decide PITR in ENG-01 ED-10 | Low |
| A server-side cascade (for example a Cloud Function on Auth user deletion) would remove client dependence | **Not proposed.** There is no backend (EP-1). It would need an ADR if the residual risk is judged unacceptable | — |

### 9.4 Play account-deletion web requirement (dependency)

- Google Play requires apps that let users create an account to offer in-app deletion **and** a web resource where users can request account and data deletion without the app (INFERRED, Play User Data policy). Account creation here is implicit at the first Google sign-in, and is treated as in scope.
- **Dependency (PLN-04 DEP):** a public page linked from the store listing and the privacy policy. Hosting is decided in ENG-01 ED-09.
- **Operator runbook** (ENG-01 §9):
  1. Verify that the requester controls the Google account (reply from the same address).
  2. Look up the uid in the Auth console.
  3. Recursively delete `users/<uid>` in the named database (Firebase console or `firebase firestore:delete --recursive`; confirm named-database support when implementing).
  4. Delete the Auth user.
  5. Record the request id, date and uid (no other PII) in a deletion register.
  - SLA ≤ 30 days (**Proposed**).

### 9.5 Privacy policy content requirements

The policy must state, consistently with §3 and verified by SEC-REQ-36:

1. Controller identity and contact.
2. Data categories (§3.2), in plain language.
3. Purposes, and legal bases where applicable (target markets are an ASSUMPTION; D-01).
4. That account use is optional and what changes when signed in.
5. Processors and recipients: Google Firebase (Auth, Firestore, App Check / Play Integrity) and Google Play; the device speech service for T3a under its own policy.
6. What is **not** collected: no audio uploads, no transcripts in the cloud (R1), no analytics, no ads, no location, no contacts.
7. On-device storage and retention: recordings default 30 days with options; transcripts kept with attempts; backup behaviour (§6.2).
8. Cloud retention: until deletion; backup windows (ED-10).
9. Rights and how to exercise them: in-app export, clear history, delete account; the web deletion link; contact.
10. Online-transcription disclosure (mirrors §7.3).
11. Security measures summary.
12. International transfers (Google infrastructure).
13. Children: not directed at children (A-4).
14. Change notification, version and effective date.

### 9.6 Data-safety form inputs (Play) — draft, to be confirmed before any Play submission

| Play data type | Collected? | Shared? | Optional? | Purpose | Notes |
|---|---|---|---|---|---|
| Personal info: Email address | Yes, when signed in (processed by Firebase Auth through Google sign-in) | No | **Yes** (account optional) | Account management | Not copied into Firestore documents |
| Personal info: Name | Yes, if the user enters a display name and signs in | No | Yes | App functionality | Optional field |
| Personal info: User IDs | Yes (Firebase uid) when signed in | No | Yes | Account management, app functionality | — |
| App activity: Other actions (learning progress, attempt metrics) | Yes, when signed in | No | Yes | App functionality | Derived metrics only |
| Audio: Voice or sound recordings | **Not collected by SpeakCraft** (on device only) | **See SD-05** | — | — | For T3a, audio is sent to the device speech service. Conservative recommendation: declare as *shared, optional, App functionality* once T3a ships, subject to policy review |
| App info and performance: Crash logs / Diagnostics | No (local diagnostic log; shared only by explicit user action) | No | — | — | Revisit if D-13 approves Crashlytics |
| Device or other IDs | **To confirm** from Firebase's published per-SDK disclosure guidance (Auth, Firestore, App Check / Installations) | — | — | — | UNKNOWN |
| Security practices | Encrypted in transit: Yes. Deletion request: Yes (in-app and web). Independent review: No | — | — | — | — |

### 9.7 Privacy review procedure (referenced by ADR-003 §Consequences)

Any new data flow, SDK, permission, endpoint or change of default needs, before approval:

1. An update to §3 (inventory).
2. A §11 delta (threats).
3. Updated §12 requirements and §13 tests.
4. Updated user-facing disclosures (in-app text, privacy policy, data-safety inputs).
5. Product Owner approval recorded in RD-99 §8.

---

## 10. Logging and PII

### 10.1 AppLogger rules (ADR-007; SEC-REQ-34, 35)

| Rule | Detail |
|---|---|
| Single entry point | All logging goes through `AppLogger`. A CI check fails on any `android.util.Log` or `println` outside `AppLogger` (TC-SEC-041) |
| Levels in release | VERBOSE, DEBUG and INFO are no-ops. WARN and ERROR go to Logcat **after redaction**, plus the local diagnostic log. Defence in depth: R8 `-assumenosideeffects` for `Log.v/d/i` once R8 is enabled (FND-SEC-11) |
| Structured codes | Log events are enumerated codes with non-personal parameters, for example `SYNC_UPLOAD_FAILED{collection=attempts, code=PERMISSION_DENIED, attempt=3}` instead of free text |
| Never logged | Transcripts and recognition partials; audio data; email; display name; uid; Firebase ID, refresh or App Check tokens; Google ID tokens; Firestore paths containing a uid; file paths containing a `profileId`; consent details |
| Redaction filter (applied to every message and throwable message) | Email regex → `<email>`. 28-character Firebase-uid-like tokens → `<uid>`. JWT pattern `eyJ[\w-]+\.[\w-]+\.[\w-]+` → `<jwt>`. `users/[^/\s]+` → `users/<uid>`. UUIDs → `<id>`. URL query strings stripped |
| Exceptions | Log class name, code and redacted message. Stack traces only in debug builds and the local diagnostic log (redacted) |
| Replacement of `FirestoreErrorHandler` | Its JSON with `userId`/`email`/`providerInfo` (`FirestoreErrorHandler.kt:35-51`) is removed. It logs operation, collection name and error code only |

### 10.2 Diagnostic log (ADR-007 option C)

- Location `noBackupFilesDir/diagnostics/`. Rolling, 2 files × 256 KB, 7 days (**Proposed**).
- Contents: timestamp, app version, OS version and device model (disclosed), and log codes. Uncaught exceptions are recorded (redacted) by a wrapper around the default handler, which still crashes normally.
- Shared only from Settings → About → "Share diagnostics", after a preview screen that shows exactly what will be shared.
- Wiped on account deletion and on delete local data. Excluded from backup.

### 10.3 If Crashlytics is later approved (D-13)

It would be opt-in, with no custom keys or logs containing personal data, the redaction filter applied to non-fatals, a data-safety update and the §9.7 review.

---

## 11. Threat model (STRIDE)

### 11.1 Assets and trust boundaries

**Assets:**

| ID | Asset |
|---|---|
| A1 | Recordings |
| A2 | Transcripts |
| A3 | Learning data |
| A4 | Account identity (Google linkage, Firebase session) |
| A5 | Signing keys and release channel |
| A6 | Firebase project (rules, quota, billing) |
| A7 | CI secrets |
| A8 | User trust (truthful privacy claims) |
| A9 | Local profile data (no-account mode) |

**Trust boundaries:**

```
[User] ─ device UI ─ [SpeakCraft app sandbox: Room, DataStore, recordings]
                         │ Binder/intents                │ HTTPS (signed-in only)
             [Other apps / OS speech service]      [Firebase: Auth · Firestore · App Check]
                         │                                ▲ rules deploy
             [Google Play services]               [Maintainer / CI (GitHub Actions)] ─ [GitHub Releases / Play Console] ─ [Testers' devices]
```

### 11.2 STRIDE table

Likelihood (L) and Impact (I) use the scale H / M / L. "Current" is the exposure at HEAD `68a9cb1`. Residual is the risk after the mitigations.

| ID | STRIDE | Assets | Threat / scenario | Current | L | I | Mitigations | Residual |
|---|---|---|---|---|---|---|---|---|
| THR-01 | I | A1, A2, A3, A9 | Device theft or loss; forensic extraction from a rooted or unencrypted device | Recordings kept forever, unpartitioned (FND-SEC-09) | M | M | App-private storage; device encryption; 30-day retention (SEC-REQ-19); no secrets on screen; §6.6 decision | Low–Medium (accepted) |
| THR-02 | I | A2, A3, A9 | Shared or second-hand device: the next user sees or merges another person's history | No sign-out cleanup (FND-SEC-09) | M | M | Sign-out "Remove" by default (SEC-REQ-04); profile partitioning (SEC-REQ-18); merge confirmation (§4.2) | Low |
| THR-03 | T/E | A6, A3 | A malicious local app sends intents or extras to exported components (App Check debug-secret injection; spoofed actions) | Exported `MainActivity` accepts a debug token in debug builds (FND-SEC-02) | M | M | No intent extras or data processed (SEC-REQ-14, 29); only the launcher exported; immutable PendingIntents; FileProvider removed or narrowed (TC-SEC-008, 011) | Low |
| THR-04 | S/T | A5, A8 | Malicious APK through the update channel: user-editable repo, debug-key holder, "uninstall the old build" social engineering | High (FND-OPS-04, FND-SEC-02) | M | H | Updater removed (ADR-004); release-signed only (SEC-REQ-27); published SHA-256 and certificate fingerprints; debug cert retired (SEC-REQ-39); Play App Signing | Low (Medium–Low during the GitHub beta) |
| THR-05 | T | A3 | A user forges their own attempts, streaks or scores through direct API writes | Client-writable counters (FND-SEC-10) | M | L | Aggregates derived locally from facts, never trusted in the cloud (ADR-014); schema validation; immutability (SEC-REQ-08, 09) | Low (self-deception only; no leaderboards) |
| THR-06 | D | A6 | Quota or cost abuse: scripted mass writes or reads with a valid account; mass account creation | Unbounded listeners; no App Check in practice | M | M | Size and id limits; no `users` list; bounded client queries; Google-only provider; API-key restrictions; budget alerts (SEC-REQ-37, 38); App Check after Play (SEC-REQ-15) | **Medium** until App Check is enforced |
| THR-07 | I | A3 | Cross-user read or write through a rules defect | Good isolation, but untested in CI | L | H | Default deny; owner-only; negative emulator tests as a required check; rules deployed only from tested commits; no admin role (SEC-REQ-07, 11, 12) | Low |
| THR-08 | S | A4, A3 | Account takeover: Google account compromise; theft of refresh tokens from a device or backup | Firebase stores eligible for backup (template rules) | L | M | Google-managed authentication; no passwords in the app; Firebase stores excluded from backup (SEC-REQ-17); re-authentication before deletion (SEC-REQ-05) | Low |
| THR-09 | I | A1, A2, A4 | Cloud backup or device-to-device transfer leaks recordings, transcripts or tokens | High (FND-SEC-06) | H | H | `noBackupFilesDir` for recordings; include-only rules covering D2D; `sensitive.db` excluded; dynamic `bmgr` tests (SEC-REQ-16, 17; TC-SEC-012, 013) | Low |
| THR-10 | T/E | A5, A7 | Supply chain: compromised action tag, Gradle dependency, wrapper jar, or unpinned npm package | Mutable tags; npm without a lockfile next to the service-account secret (FND-OPS-03) | M | H | SHA-pinned actions; wrapper validation; locked dependencies; minimal permissions; Dependabot alerts (SEC-REQ-32, 33) | Medium–Low |
| THR-11 | I/E | A5, A7 | CI secret exfiltration through a fork PR, workflow injection or an over-privileged token | `contents: write` on every push; secrets in a push-triggered job | L | H | PR workflows hold no secrets; no `pull_request_target`; env-passed inputs (kept); release environment with required reviewer; CODEOWNERS on `.github/` (SEC-REQ-32) | Low |
| THR-12 | I (compliance) | A8 | Privacy misrepresentation: UI, policy or data-safety claims contradict behaviour | High (FND-SEC-05, FND-SEC-07, FND-AI-10) | H | H | Cloud-AI SDK removed plus a classpath check (SEC-REQ-28); no transcripts or paths in cloud documents (SEC-REQ-10); consent gating (SEC-REQ-20); a single privacy-facts source with a test (SEC-REQ-36); release checklist sign-off | Low |
| THR-13 | I | A1, A2 | Audio streamed to a network recogniser without the user knowing | High (FND-SEC-07) | H | H | On-device API for T1/T2; consent-gated factory (SEC-REQ-20, 21; TC-SEC-015, 016) | Low (the OS service's behaviour is disclosed but outside our control) |
| THR-14 | I | A4, A2 | PII in Logcat, bug reports or OEM log collectors | uid and email logged (FND-SEC-08) | M | M | AppLogger redaction and release stripping (SEC-REQ-34; TC-SEC-040, 041) | Low |
| THR-15 | R (compliance) | A3, A8 | Incomplete account deletion: the user believes the data is gone but it is not | Unreachable, partial deletion (FND-SEC-03) | M | M | Resumable verified cascade; marker; web deletion route (SEC-REQ-24, 25) | Low–Medium |
| THR-16 | E | A6, A5 | Debug or test artefacts in distributed builds: debuggable flag, App Check debug provider, `run-as` access | High (FND-SEC-02, FND-SEC-11) | M | H | Release-only CI; `debugImplementation`; artefact checks for debuggable and debug certificate (SEC-REQ-14, 27; TC-SEC-009, 010) | Low |
| THR-17 | S/D | A5 | Signing key lost or compromised | Custody undocumented | L | H | Play App Signing (resettable upload key); custodians; offline backups; rotation plan (ENG-01 §5) | Low |
| THR-18 | T | A5, A8, A3 | Abuse of legacy shared collections and admin roles during the transition (a MODERATOR edits the legacy OTA record or content) | Medium (FND-SEC-04) | L | H | L0 freeze of client writes; L1 removal; L2 deletion (§5.6; SEC-REQ-11, 13) | Low after L0 |
| THR-19 | S/D | A6 | API key extracted from the APK and used to call Identity Toolkit or Firestore directly | Key restriction state UNKNOWN | M | M | API-key restrictions; providers limited to Google; rules; App Check (SEC-REQ-03, 37, 15) | Low–Medium |
| THR-20 | I/T | A3, A4 | Network interception (MITM) on hostile Wi-Fi | Default config only | L | M | TLS through the Firebase SDKs; no cleartext; system CAs only in release (SEC-REQ-30) | Low |
| THR-21 | I | A2 | Export file leaks after the user saves it to a shared location | No export today | M | M | SAF; export notice that the file contains transcripts; no automatic sharing (SEC-REQ-22) | Low (user-controlled) |
| THR-22 | I | A2 | Lock-screen notifications or the recents screen expose content | n/a | L | L | Generic reminder text; no transcripts in notifications (§6.7) | Low |

---

## 12. Security requirements

SEC-01 is the **authoritative source** for detailed security requirements (SEC-REQ-nn); they are binding on the SRS (§10) and NFR (§4). The "Related NFR" column links each requirement to the measurable NFR it supports ("—" = no separate NFR; SEC-REQ is the requirement). Each requirement is verified by the listed TC-SEC tests (§13).

| ID | Requirement (testable) | Threats | Related NFR | Verified by |
|---|---|---|---|---|
| SEC-REQ-01 | Onboarding, sessions, feedback, Words, Progress, reminders, export and "Delete local data" work without sign-in and without network | THR-12 | NFR-REL-006 | TC-SEC-001 |
| SEC-REQ-02 | In no-account mode the app (including the Firebase SDKs) makes no network requests. OS speech services after T3a consent are excluded | THR-12, THR-13 | NFR-PRIV-002 | TC-SEC-002 |
| SEC-REQ-03 | Sign-in uses Google through Credential Manager and Firebase Auth only. Other providers are disabled in the Firebase project | THR-08, THR-19 | — | TC-SEC-049 |
| SEC-REQ-04 | Sign-out offers "Remove from this device" and "Keep on this device". Default per decision D-21 (recommended: Remove when all data is synced; if unsynced data exists, warn and default to Keep). Remove wipes every store marked ✔ in §6.4 | THR-02 | NFR-PRIV-005 | TC-SEC-003, 004 |
| SEC-REQ-05 | Account deletion re-authenticates the same uid before any data is deleted. A uid mismatch aborts with no changes | THR-08, THR-15 | NFR-REL-009 | TC-SEC-005 |
| SEC-REQ-06 | Authentication and sync errors show only mapped messages (§4.6). Raw exception text is never displayed | THR-14 | — | TC-SEC-007 |
| SEC-REQ-07 | Rules deny by default. Only `users/{uid}/…` is reachable, only when `request.auth.uid == uid`. `users` cannot be listed | THR-07 | NFR-SEC-001 | TC-SEC-020, 021, 022 |
| SEC-REQ-08 | Every create and update is validated with required and allowed key lists, types, enums, ranges, string sizes, and **every** list element | THR-05, THR-06 | NFR-SEC-001 | TC-SEC-023–025, 028, 030–033, 036 |
| SEC-REQ-09 | `attempts` and `events` are create-only. `updatedAt`/`syncedAt` equal `request.time`. Client timestamps lie within [now − 400 d, now + 5 min] | THR-05 | NFR-SEC-001 | TC-SEC-027, 029, 030 |
| SEC-REQ-10 | No cloud document contains transcripts (R1), audio, file or device paths, email, or speech-derived free text. Enforced by rules allow-lists and by repository mapper unit tests | THR-12 | NFR-PRIV-003 | TC-SEC-026, 023 |
| SEC-REQ-11 | No shared collections and no admin role in R1–R2. Users with admin custom claims gain no access | THR-07, THR-18 | NFR-SEC-001 | TC-SEC-034 |
| SEC-REQ-12 | Rules are deployed only from a commit whose emulator suite passed. The deployed ruleset matches the repository at each release (recorded evidence) | THR-07 | NFR-SEC-001 | TC-SEC-038; ENG-01 checklist |
| SEC-REQ-13 | Legacy user collections are read + delete only from L1. All legacy and shared rules are removed at L2 (§5.6) | THR-18 | — | TC-SEC-035 |
| SEC-REQ-14 | No code reads App Check debug secrets from intents. `firebase-appcheck-debug` is absent from the release runtime classpath | THR-03, THR-16 | NFR-SEC-004 | TC-SEC-008, 010 |
| SEC-REQ-15 | Release builds use Play Integrity once distributed through Play. Enforcement follows the SD-06 criteria | THR-06, THR-19 | — | TC-SEC-049 |
| SEC-REQ-16 | Recordings are written only under `noBackupFilesDir/recordings/<profileId>/`. Nothing is written to shared or external storage | THR-09, THR-01 | NFR-PRIV-006 | TC-SEC-014 |
| SEC-REQ-17 | Backup and extraction rules include only `learning.db` and `settings` for cloud backup **and** device transfer | THR-09, THR-08 | NFR-PRIV-006 | TC-SEC-012, 013 |
| SEC-REQ-18 | All learning rows carry `profile_id`. DAO queries never return another profile's rows | THR-02 | — | TC-SEC-019 |
| SEC-REQ-19 | Expired recordings are deleted within 24 h of expiry. Discarded or failed attempts' audio is deleted immediately. Orphans are swept at app start | THR-01 | NFR-PRIV-005 | TC-SEC-014 |
| SEC-REQ-20 | A non-on-device `SpeechRecognizer` is created only with a valid T3a consent (current disclosure version and recogniser package) | THR-13 | NFR-PRIV-002 | TC-SEC-015 |
| SEC-REQ-21 | The consent screen contains every §7.3 element. Consent is revocable with immediate effect. The record is stored locally | THR-12, THR-13 | NFR-PRIV-002 | TC-SEC-016 |
| SEC-REQ-22 | Export produces schema-v1 JSON through SAF with the §9.1 contents and exclusions, offline and without an account | THR-21 | NFR-PRIV-004 | TC-SEC-017 |
| SEC-REQ-23 | Clear history and Reset vocabulary delete the local items and matching cloud documents and set the marker. Other devices purge on their next sync | THR-15 | NFR-PRIV-005 | TC-SEC-018 |
| SEC-REQ-24 | Account deletion follows the §9.3 state machine, survives process death at every phase, verifies emptiness from the server, and ends with Auth deletion and a local wipe | THR-15 | NFR-REL-009 | TC-SEC-006, 037 |
| SEC-REQ-25 | A public account-deletion request URL and an operator runbook exist before any Play submission | THR-15 | — | ENG-01 checklist; TC-SEC-049 |
| SEC-REQ-26 | No secrets and no `google-services.json` are committed. A CI scan blocks known key patterns | THR-10, THR-11 | — | TC-SEC-043 |
| SEC-REQ-27 | Every distributed artefact is release-signed, non-debuggable and R8-minified. CI fails on `debuggable=true` or a debug-certificate signature | THR-04, THR-16 | NFR-SEC-003 | TC-SEC-009 |
| SEC-REQ-28 | No cloud-AI SDK (`firebase-ai`, Vertex AI or Generative AI clients) is on the release runtime classpath, and no AI endpoint strings appear in the release DEX | THR-12 | NFR-SEC-006 | **TC-SEC-010** (as referenced by ADR-003) |
| SEC-REQ-29 | Only `MainActivity` is exported. No external intent extras or data are processed. There is no FileProvider except the diagnostics-only path | THR-03 | NFR-SEC-004 | TC-SEC-008, 011 |
| SEC-REQ-30 | Cleartext is disabled through the network security configuration. Release trusts system CAs only | THR-20 | — | TC-SEC-042, 011 |
| SEC-REQ-31 | The merged manifest permissions are exactly the approved allow-list: `INTERNET`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`, plus the library-merged WorkManager permissions as reviewed. No `CAMERA` in R1–R2, never `REQUEST_INSTALL_PACKAGES`. `<queries>` for `RecognitionService` is present | THR-12, THR-03 | NFR-SEC-004 | TC-SEC-011 |
| SEC-REQ-32 | Workflow actions are pinned to full commit SHAs. Permissions are minimal per job. Release and deploy jobs run in protected environments with a required reviewer. PR workflows use no secrets | THR-10, THR-11 | NFR-SEC-005 | TC-SEC-045 |
| SEC-REQ-33 | The Gradle wrapper is committed and validated in CI. Dependency versions are locked (catalog exact versions plus Gradle locking or verification) | THR-10 | NFR-SEC-005 | TC-SEC-046, 047 |
| SEC-REQ-34 | All logging goes through AppLogger. Release builds emit no V/D/I logs. The redaction rules in §10.1 are applied | THR-14 | NFR-SEC-002 | TC-SEC-040, 041 |
| SEC-REQ-35 | The diagnostic log is PII-free, local, user-shared only after a preview, excluded from backup and wiped on deletion | THR-14 | NFR-SEC-002 | TC-SEC-040, 012 |
| SEC-REQ-36 | In-app privacy statements, the privacy policy and the data-safety inputs derive from one reviewed "privacy facts" source. An automated test plus a release sign-off checks consistency | THR-12 | NFR-PRIV-007 | TC-SEC-044 |
| SEC-REQ-37 | The Firebase API key is restricted to the app's package and certificates and to the required APIs | THR-19, THR-06 | — | TC-SEC-049 |
| SEC-REQ-38 | GCP budget alerts and Firestore usage alerts are configured with named recipients | THR-06 | — | TC-SEC-049 |
| SEC-REQ-39 | The debug-keystore secret is retired. Debug-certificate fingerprints are removed from the production Firebase app and OAuth client | THR-04 | NFR-SEC-003 | TC-SEC-049 |
| SEC-REQ-40 | A restore from backup is detected, regenerates `deviceId`, and pauses sync until sign-in | THR-08 | — | TC-SEC-050 |

---

## 13. Security test plan

TST-01 owns environments and exit criteria. The tests below are catalogued here and planned there. **None has been executed.**

Abbreviations for "Runs in": **CI** = on every PR; **REL** = release workflow; **RC** = manual on a release candidate; **UNIT** = JVM or Robolectric; **EMU** = Firestore emulator; **INST** = instrumented on a device or emulator.

| TC | Title | Type / runs in | Verifies | Pass criterion |
|---|---|---|---|---|
| TC-SEC-001 | No-account core loop offline | INST + RC (airplane mode) | SEC-REQ-01 | Every listed feature completes; no sign-in prompt |
| TC-SEC-002 | No-account network silence | RC: traffic capture on a test device (on-device VPN capture tool or proxy) | SEC-REQ-02 | Zero app-initiated connections across a full scripted session |
| TC-SEC-003 | Sign-out → Remove wipes the partition | UNIT (Robolectric) + INST | SEC-REQ-04 | Room rows, `sensitive.db` rows, the recordings folder, profile preferences, Firestore cache and work tagged with the profile are absent |
| TC-SEC-004 | Sign-out → Keep | UNIT | SEC-REQ-04 | Local data is present and re-keyed; Auth is null; sync work is cancelled; cache is cleared |
| TC-SEC-005 | Re-authentication gate | UNIT (fake auth) | SEC-REQ-05 | No delete call happens before re-auth; a uid mismatch leaves zero changes |
| TC-SEC-006 | Resumable deletion | UNIT (state machine with a fake store; kill at each phase) + EMU integration | SEC-REQ-24 | Every interruption point resumes to `DONE`; server collections are empty |
| TC-SEC-007 | Error mapping | UNIT, one case per §4.6 row | SEC-REQ-06 | Each mapped message is shown; no raw `localizedMessage` text appears |
| TC-SEC-008 | Intent extras ignored | INST (`am start` with `FIREBASE_APPCHECK_DEBUG_TOKEN` and arbitrary extras) + CI static grep | SEC-REQ-14, 29 | No preference written; no behaviour change; the grep finds no extra-reading code outside an allow-list |
| TC-SEC-009 | Release artefact checks | REL script (`apkanalyzer manifest debuggable`, `apksigner verify --print-certs`, `jarsigner -verify` for the AAB) | SEC-REQ-27 | `debuggable=false`; certificate equals the expected fingerprint; certificate is not `CN=Android Debug`; mapping file exists (R8 ran) |
| **TC-SEC-010** | **No cloud-AI SDK on the release classpath** | CI Gradle check over `releaseRuntimeClasspath`, plus a DEX string scan of the release APK | SEC-REQ-28, 14 | No `com.google.firebase:firebase-ai`, `firebase-vertexai`, `com.google.ai.client.generativeai` or `firebase-appcheck-debug`; no `firebasevertexai.googleapis.com` or `generativelanguage.googleapis.com` strings |
| TC-SEC-011 | Merged-manifest checks | CI script over the merged release manifest | SEC-REQ-29, 30, 31 | Exported components are exactly `[MainActivity]`; permissions equal the allow-list; `usesCleartextTraffic=false`; `networkSecurityConfig` set; backup attributes as decided (SD-02); `<queries>` for `RecognitionService` present |
| TC-SEC-012 | Backup rules static check | CI (XML parse) | SEC-REQ-17, 35 | `cloud-backup` and `device-transfer` include exactly the approved set; `backup_rules.xml` matches |
| TC-SEC-013 | Backup and D2D dynamic check | RC on an emulator: `bmgr` with the local transport, then the device-transfer path per Android backup testing guidance; inspect the backup set and restore | SEC-REQ-17 | Only approved files present; no recordings, `sensitive.db`, Firebase stores or `device_state`; the restored app opens a consistent database |
| TC-SEC-014 | Recording location and retention | UNIT (fake clock) + INST | SEC-REQ-16, 19 | Paths are under `noBackupFilesDir/recordings/<profileId>`; expiry deletion within 24 h; discarded audio deleted immediately; orphans swept |
| TC-SEC-015 | Recogniser consent gating | UNIT (fake recogniser factory) | SEC-REQ-20 | `createSpeechRecognizer` is never called without valid consent; a stale version or a package change forces re-consent |
| TC-SEC-016 | Consent disclosure content | UNIT (Compose UI test) | SEC-REQ-21 | All 11 §7.3 elements present; two equal-weight unselected actions; withdrawal toggle works |
| TC-SEC-017 | Export schema and content | UNIT (golden file / schema) | SEC-REQ-22 | Valid v1; includes transcripts; excludes `deviceId`, tokens and audio |
| TC-SEC-018 | Clear history cascade | EMU integration (two simulated devices) | SEC-REQ-23 | Cloud documents deleted; marker set; device B purges and does not re-upload |
| TC-SEC-019 | Profile isolation in DAOs | UNIT (in-memory Room, two profiles) | SEC-REQ-18 | No query leaks rows across profiles |
| TC-SEC-020 | Rules: unauthenticated denied | EMU / CI | SEC-REQ-07 | Every path, every operation denied |
| TC-SEC-021 | Rules: cross-user denied | EMU / CI | SEC-REQ-07 | get, list, create and delete on each subcollection of another uid denied |
| TC-SEC-022 | Rules: `users` list denied; own get allowed | EMU / CI | SEC-REQ-07 | As stated |
| TC-SEC-023 | Rules: profile create | EMU / CI | SEC-REQ-08, 10 | Valid baseline allowed; missing key, extra key (`email`, `streakDays`) and client `createdAt` denied |
| TC-SEC-024 | Rules: profile update and legacy migration | EMU / CI | SEC-REQ-08 | `createdAt` change denied; disallowed key denied; one-time legacy-to-v1 replacement allowed exactly once |
| TC-SEC-025 | Rules: attempt create | EMU / CI | SEC-REQ-08 | Valid allowed; id mismatch or non-UUID denied |
| TC-SEC-026 | Rules: forbidden content | EMU / CI | SEC-REQ-10 | Attempts or events with `transcript`, `audio`, `localMediaPath`, `filePath` or `email` denied |
| TC-SEC-027 | Rules: attempt immutability | EMU / CI | SEC-REQ-09 | Update denied; owner delete allowed |
| TC-SEC-028 | Rules: ranges, enums and lists | EMU / CI | SEC-REQ-08 | Out-of-range metric, unknown metric key, unknown feedback code and list larger than its maximum all denied |
| TC-SEC-029 | Rules: timestamps | EMU / CI | SEC-REQ-09 | `startedAt` > now + 5 min or < now − 400 d denied; client-supplied `syncedAt` denied |
| TC-SEC-030 | Rules: events | EMU / CI | SEC-REQ-08, 09 | Valid allowed per type; update denied; unknown type or payload key denied; invalid UUID at each `attemptIds` index denied |
| TC-SEC-031 | Rules: plans | EMU / CI | SEC-REQ-08 | Invalid step at each index 0–3 denied; 5 steps denied; bad `dateKey` denied |
| TC-SEC-032 | Rules: words | EMU / CI | SEC-REQ-08 | Stage, ease and interval out of range denied |
| TC-SEC-033 | Rules: meta | EMU / CI | SEC-REQ-08 | Only `sync` allowed; field validation |
| TC-SEC-034 | Rules: shared and admin collections | EMU / CI | SEC-REQ-11 | `lessons`, `applicationReleases`, `adminUsers` and `youtube*` denied, including for tokens with `admin: true` or `role` claims |
| TC-SEC-035 | Rules: legacy window | EMU / CI (L1 variant) | SEC-REQ-13 | Legacy read and delete allowed; create and update denied; another user's legacy data denied |
| TC-SEC-036 | Rules: size limits | EMU / CI | SEC-REQ-08 | Oversize strings (for example a 41-character `displayName`) denied |
| TC-SEC-037 | Rules: deletion cascade permitted | EMU / CI | SEC-REQ-24 | The owner can delete every document in every allowed collection plus the profile |
| TC-SEC-038 | Rules coverage report | EMU / CI artefact | SEC-REQ-12 | Every `allow` expression evaluated both true and false (Proposed target) |
| TC-SEC-040 | Log redaction | UNIT | SEC-REQ-34, 35 | Email, uid, JWT, `users/<uid>` and tagged transcript inputs are redacted; release logger drops V/D/I |
| TC-SEC-041 | Logging usage check | CI static script | SEC-REQ-34 | No `android.util.Log` or `println` outside AppLogger |
| TC-SEC-042 | Cleartext blocked | INST (HTTP request to a local test server fails in release-like build) + CI configuration check | SEC-REQ-30 | Request refused |
| TC-SEC-043 | Secret scan | CI (GitHub secret scanning where available, or a pattern script) | SEC-REQ-26 | No matches (private keys, `AIza…`, `ghp_`, `"private_key"`, keystore magic bytes) |
| TC-SEC-044 | Privacy-facts consistency | UNIT + review | SEC-REQ-36 | In-app strings match the PrivacyFacts source; the generated data-safety table matches the reviewed version |
| TC-SEC-045 | Workflow hardening lint | CI script | SEC-REQ-32 | Every `uses:` pinned to a 40-hex SHA; `permissions:` declared; no `pull_request_target`; secrets only in protected-environment jobs |
| TC-SEC-046 | Gradle wrapper validation | CI | SEC-REQ-33 | Wrapper checksum valid; `distributionSha256Sum` present |
| TC-SEC-047 | Dependency vulnerability review | REL checklist (Dependabot alerts) | SEC-REQ-33 | No open critical or high alerts without a recorded waiver |
| TC-SEC-048 | Manual RC security review | RC | Multiple | `run-as` fails (non-debuggable); a Logcat capture during a full session shows no PII; the endpoint list matches §8.3; intents fuzzed |
| TC-SEC-049 | Firebase/GCP configuration evidence | REL checklist (console screenshots or exports) | SEC-REQ-03, 15, 25, 37, 38, 39 | Providers, API-key restrictions, App Check status, budget alerts, SHA fingerprints and the deletion URL all match the expected state |
| TC-SEC-050 | Restore-from-backup handling | INST (restore through `bmgr`) | SEC-REQ-40 | New `deviceId`; sync paused; no duplicate cloud documents |

---

## 14. Open security decisions

### 14.1 Existing master decisions (RD-99 §8) affecting security

| Decision | Security dependency |
|---|---|
| D-03 distribution / updater | Removes THR-04's main vector. Determines when App Check enforcement and the data-safety form apply |
| D-04 cloud-AI removal | SEC-REQ-28; THR-12 |
| D-05 account optional | §4.1 model; SEC-REQ-01, 02 |
| D-10 recording retention (30 days) | §7.1; THR-01 residual risk |
| D-13 crash reporting | §10.3; data-safety inputs |
| D-14 minSdk and speech tier policy / online consent | §7.2–7.3; how many users see T3a |
| D-15 applicationId / namespace change | Firebase app registration, SHA fingerprints, legacy migration path (§5.6) |
| D-16 transcript cloud sync opt-in (R2) | §7.5; re-triggers §6.6 and §9.7 |
| D-19 legacy data migration scope | §5.6 window length and legacy rules |

### 14.2 New security decisions (consolidated into RD-99 §8 as D-21…D-26; SD-07 is owned by ENG-01 ED-06; SD-08 is an implementation detail tracked here)

| ID | Decision | Options | Recommendation |
|---|---|---|---|
| SD-01 (→ D-21) | Default sign-out behaviour | (a) **Remove** is the primary action; Keep is an explicit choice. (b) Keep by default | **(a)**. Data is kept only if the user chooses "Keep on this device"; the unsynced-items warning is mandatory |
| SD-02 (→ D-23) | Backup policy | (A) `allowBackup=true` with include-only `learning.db` + `settings`. (B) `allowBackup=false` (D2D rules still needed) | **(A)** |
| SD-03 (→ D-24) | Local transcript retention default | (a) Keep with the attempt until deleted. (b) Follow the recording retention | **(a)**, with an added option "Delete transcripts when recordings expire" |
| SD-04 (→ D-22) | Audio in data export | (a) R1 JSON only. (b) Optional ZIP with recordings | **(a)** for R1; consider (b) in R2 |
| SD-05 (→ D-25) | Data-safety declaration for T3a online transcription | (a) Declare audio as "shared, optional". (b) Declare not collected | **(a)** (conservative), subject to policy review before submission |
| SD-06 (→ D-26) | App Check enforcement criteria | Enforce when Play-only **and** ≥ 98% verified over 14 days (**Proposed**), or another threshold | Approve the proposed criteria |
| SD-07 | Rules deployment credential | See ENG-01 **ED-06** (owned there) | Manual deploy by the Release Manager with recorded evidence in R0–R1 |
| SD-08 | Rule hardening during deletion (deny creates while the marker exists) | Add (1 extra read per write) or omit | Omit in R1; revisit if TC-SEC-018 or TC-SEC-006 show races |

---

## 15. Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial draft: current-state assessment, target data inventory, authentication and App Check, proposed rules and test plan, local protection, speech privacy, data rights, logging, STRIDE model, SEC-REQ-01…40, TC-SEC catalogue, open decisions | Security Engineer |
