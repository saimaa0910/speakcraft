# FS-01 — Account & Sign-in

| Field | Value |
|---|---|
| Document ID | FS-01 |
| Version | 0.3 — Draft for review (2026-11-21: sign-out default aligned to decision D-21; Remove wipe scope aligned to SEC-01 §6.4. 0.3, consistency review CL-58/CL-79: deletion phases aligned to SEC-01 §9.3 / ARC-03 ENT-18; adoption and re-key wording aligned to ARC-03 §7.2) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-01; PRD §15.1 (PRD-FR-001…005); SRS §6.1 (SRS-FR-ACC-001…009), §6.11, §11, §15; NFR-REL-009, NFR-PRIV-*, NFR-SEC-002; ADR-014, ADR-015; ARC-01 §2; DES-02 (SCR-ACC-01/02, SCR-SET-07, SCR-SYS-01); FS-02, FS-10, FS-11; decision D-05 |

---

## 1. Feature ID & purpose

**FS-01 / CAP-01 (pillar F-2).** Let people use every core feature **without an account** on a local profile, optionally **sign in with Google** to back up and sync progress, **sign out** safely, and **delete their account and all associated data** in a way that is complete, resumable and verifiable.

## 2. User problem

| Problem | Evidence (current system) | Personas |
|---|---|---|
| "I just want to try it — don't make me sign up." | Google sign-in is mandatory before any use (FND-SEC-01) | All, esp. P2, P5 (UN-10) |
| "If I change phone I don't want to lose my progress." | Learning state partly local-only and lost on reinstall (FND-DATA-02) | P1, P3 |
| "When I leave, I want my data really gone." | Deletion unreachable, partial; undeletable documents (FND-SEC-03); no cleanup on sign-out (FND-SEC-09) | P2, P4 (UN-08) |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | Local profile on first launch; Google sign-in via Credential Manager from Settings → Account, onboarding and the Progress prompt; first-sign-in adoption of local data; merge with existing cloud data (ADR-014); sign-out with keep/remove choice; account deletion with re-auth and resume; "Delete everything on this device" for local users | No additional account scope |
| **Out** | Email/password or other identity providers; multiple local profiles or in-app account switcher; admin roles (BR-017, NG-10); sync engine internals and legacy migration (FS-11); export / clear history (FS-10); transcript sync (D-16, R2, FS-11) | — |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Local use | App installed; no network required |
| Sign-in | Network available; a Google account on the device usable through Credential Manager; Firebase project reachable (DEP-05) |
| Sign-out | User signed in |
| Account deletion | User signed in; network available; user can re-authenticate with the same Google account |
| Local delete | User not signed in (local profile or signed-out-kept data) |

## 5. Main workflow

### 5.1 First launch — local profile (SRS-FR-ACC-001)
1. App starts; `ProfileRepository` finds no ENT-01 Profile.
2. It creates a Profile with `profileId` = random UUID, `authState = LOCAL`, `memberSince` = today's dateKey. No network call is made.
3. Navigation starts onboarding (FS-02).

### 5.2 Sign in with Google (SRS-FR-ACC-002…004)
1. User taps **Sign in with Google** in SCR-SET-07 (or the onboarding / Progress entry, DES-02 §2.2).
2. SCR-ACC-01 explains, in text generated from the policy constants (SRS-PRIV-002): what backs up (profile, learning preferences, attempt metrics, plans, word progress, events), what never leaves the device (recordings, file paths) and that transcripts are not uploaded by default (SRS-FR-DATA-006).
3. User taps **Continue with Google**; the Credential Manager sheet opens; user selects an account.
4. `AccountRepository` exchanges the Google ID token for a Firebase credential and signs in.
5. For a `LOCAL` profile signing in for the first time: all profile-keyed rows are re-keyed from `profileId` to the Firebase UID and `authState = SIGNED_IN`, using the crash-safe, idempotent re-key procedure of ARC-03 §7.2 (a `rekey_pending` marker; one transaction per database — `learning.db` rows cascade, `sensitive.db` transcripts re-keyed; then the recording folder is renamed; an interrupted re-key completes at the next app start). ENT-10 SyncOutbox rows already exist for every syncable record, because they are written for every profile (ARC-03 §3.11), so the backlog uploads without an extra enqueue step (SRS-FR-ACC-003; ARC-03 §7.2 case A).
6. `SyncRepository` pulls cloud documents changed since the watermark and merges them per SRS-BR-021; no completed attempt or passed word stage is discarded (SRS-FR-ACC-004). Legacy users are migrated per SRS-FR-DATA-007 (FS-11).
7. SCR-ACC-01 shows "You're signed in as {email}. Your progress backs up when you're online." and returns to the originating screen. SCR-SET-07 shows sync status (SRS-FR-DATA-005).

### 5.3 Sign out (SRS-FR-ACC-006)
1. SCR-SET-07 → **Sign out** opens a dialog with two options: **Keep my practice data on this device** and **Remove it from this device**. The pre-selected option follows decision **D-21** (Pending — Product Owner; SRS-FR-ACC-006, SEC-REQ-04): **Remove** is pre-selected when every change has been uploaded; **Keep** is pre-selected when un-uploaded changes exist (step 2).
2. If ENT-10 holds un-uploaded entries, the dialog shows the warning "{n} changes haven't backed up yet. If you remove your data now, they will be lost." (mandatory per D-21; the wording is RECOMMENDATION).
3. On confirm, `AccountRepository` clears credential state (Credential Manager `clearCredentialState` + Firebase sign-out) and cancels `SyncWorker` — in both cases.
4. **Keep:** Profile `authState = SIGNED_OUT`; data stays on the device, keyed by the UID; no sync while signed out.
5. **Remove:** every store marked for wipe in SEC-01 §6.4 is cleared for the profile — `learning.db` rows, `sensitive.db` transcripts, profile preferences, the recording folder `noBackupFilesDir/recordings/<profileId>/`, the Firestore local cache and work tagged with the profile; a new local profile is created and onboarding starts (RECOMMENDATION).

### 5.4 Delete account (SRS-FR-ACC-007, -008)
1. SCR-SET-07 → **Delete account** opens SCR-ACC-02 *Explain*: what will be deleted in the cloud and on the device, that it cannot be undone, and a link to Export (FS-10) first.
2. *Confirm*: user types **DELETE** (RECOMMENDATION; SRS permits "typed or explicit confirmation") and taps **Delete my account** (SEC-01 §9.3 phase `CONFIRMED`).
3. *Re-authenticate*: Credential Manager re-auth for the same account; `AccountRepository` calls Firebase re-authentication (phase `REAUTHENTICATED`). Nothing is deleted before re-auth succeeds.
4. ENT-18 DeletionState (`sensitive.db`, ARC-03 §3.19) is persisted `{uid, phase, startedAt}`; the phase advances through the SEC-01 §9.3 sequence, each phase idempotent and resumable: `MARKER_WRITTEN` (`meta/sync.deletionRequestedAt` written; other devices stop uploading) → `SYNC_STOPPED` (`SyncWorker` cancelled, outbox cleared).
5. `CLOUD_SUBCOLLECTIONS` (the collections listed in SEC-01 §9.3 are emptied in pages until empty) → `VERIFIED` (each collection returns empty from the server) → `CLOUD_PROFILE` (`meta/sync`, then `users/{uid}`, deleted).
6. `AUTH_DELETED`: the Firebase Auth user is deleted.
7. `LOCAL_WIPED`: all local data in the SEC-01 §6.4 "Delete account" column is deleted — rows, preferences, the profile's recording folder, Firestore cache and credential state; workers cancelled; the ENT-18 row is kept until the next phase. `DONE`: the deletion state is cleared.
8. *Done*: "Your account and all your SpeakCraft data have been deleted." **Continue** → fresh first-launch state (new local profile, onboarding).

### 5.5 Delete everything on this device (SRS-FR-ACC-009)
1. Local or signed-out user: SCR-SET-07 → **Delete everything on this device**.
2. Confirmation lists the consequences (practice history, words, recordings, preferences; cannot be undone).
3. On confirm: local data, preferences and recordings deleted; new local profile; onboarding. No network needed.

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | User cancels the Credential Manager sheet | Return to the previous screen with no error text (SRS-FR-ACC-005) |
| A2 | Account already has cloud data from another device | Merge (step 5.2.6); confirmation line "We combined the progress on this phone with your account." |
| A3 | Same account signs in again after *Keep* sign-out | No re-key; RECOMMENDATION: enqueue every record created or changed while signed out (idempotent by UUID, SRS-BR-020). See Issue 2 |
| A4 | A **different** account signs in while another account's kept data is on the device | RECOMMENDATION: never adopt automatically; show "This phone has practice data from another account. Remove it and continue?" → Remove and continue / Cancel. See Issue 1 |
| A5 | App relaunched with ENT-18 `phase ≠ DONE` | SCR-SYS-01 "Finishing account deletion…" is shown before any other screen and resumes from the persisted phase (SRS-FR-ACC-008). If Firebase requires recent login, prompt re-auth; if the Auth user is already gone, skip to `LOCAL_WIPED` (SEC-01 §9.3) |
| A6 | Legacy signed-in user upgrades (Firebase session persists) | Profile starts `SIGNED_IN`; no adoption step; legacy migration per FS-11 |
| A7 | Sign-in started from the Progress "back up" prompt | Same as 5.2; returns to SCR-PROG-01 |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Offline when starting sign-in | "You're offline. Connect to the internet to sign in — you can keep practising without an account." | SRS-ERR-007, SRS-AVL-002 |
| E2 | No Google account / no credential provider | "No Google account was found on this phone. Add one in system settings, or keep using SpeakCraft without an account." | SRS-ERR-007 |
| E3 | Firebase sign-in failure (network drop, server, invalid token) | "We couldn't sign you in. Please try again." + Retry; mapped from error class, no codes | SRS-ERR-007 |
| E4 | Re-key transaction fails | Roll back; sign out of Firebase so local state stays `LOCAL`; "Sign-in couldn't finish. Your data is safe on this phone." + Retry | SRS-ERR-005 |
| E5 | Pull/merge fails after sign-in | Remain signed in; sync status "Error — Retry"; never blocks UI | SRS-ERR-006 |
| E6 | Re-auth cancelled or fails during deletion | Nothing deleted; return to SCR-ACC-02 *Confirm* | SRS-ERR-007 |
| E7 | Deletion interrupted (network loss, process death) | DeletionState kept; resume per A5 | SRS-ERR-009 |
| E8 | Offline when starting account deletion | "Connect to the internet to delete your account." Deletion does not start | — |
| E9 | Offline when resuming deletion | SCR-SYS-01 "Waiting for a connection to finish deleting your account" + Retry (see Open question Q3) | SRS-ERR-009 |
| E10 | Local file deletion fails | DeletionState stays `LOCAL_WIPED`; retried on next launch; PII-free diagnostic | SRS-ERR-005 |

## 8. Business rules

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-020 | Re-uploading adopted records is safe — attempts and events are idempotent by UUID |
| SRS-BR-021 | Merge on sign-in follows ADR-014 (facts append-only; mutable docs last-writer-wins; word state most-advanced stage) |
| SRS-BR-022 | Recordings for the profile are deleted on *Remove*, account deletion and local delete |
| SRS-BR-023 | Online-transcription consent is a device preference; it is not changed by sign-in or sign-out |
| SRS-PRIV-003 | Account deletion and local delete cover 100 % of the data categories in SRS §9 |
| RECOMMENDATION | Deletion order is fixed: cloud documents → Auth user → local data (cloud first, because Firestore rules require the authenticated owner) |

## 9. UI states

**SCR-ACC-01 Sign in**

| State | What the user sees | Actions |
|---|---|---|
| Default | Benefits, "what backs up / what stays on this phone" list, privacy link | Continue with Google; Not now |
| Loading | Progress indicator "Signing in…"; buttons disabled | — |
| Empty | N/A — static content | — |
| Error | E2/E3 message inline | Retry; Not now |
| Offline | E1 message; Continue disabled | Not now |
| Permission | N/A — no runtime permission | — |
| Success | "You're signed in as {email}" (+ A2 merge line) | Done |
| Account conflict (A4) | Explanation of other account's data | Remove and continue; Cancel |

**SCR-SET-07 Account**

| State | What the user sees | Actions |
|---|---|---|
| Default — local | "Using SpeakCraft without an account" + benefits of signing in | Sign in with Google; Delete everything on this device |
| Default — signed in | Name, email, sync status (FS-11) | Sign out; Delete account |
| Default — signed out (kept data) | "Signed out — your data is on this phone only" | Sign in with Google; Delete everything on this device |
| Loading | Skeleton rows while profile loads | — |
| Empty | N/A — always has a profile | — |
| Error | Sync "Error — Retry" chip (SRS-ERR-006) | Retry |
| Offline | "Waiting for connection" sync status; sign-in button shows E1 on tap | Delete everything on this device (local) |
| Permission | N/A | — |
| Sign-out dialog | Keep / Remove; pre-selection per D-21 (Remove when fully synced, Keep with the unsynced warning otherwise; 5.3.1–5.3.2) | Sign out; Cancel |

**SCR-ACC-02 Delete account**

| State | What the user sees | Actions |
|---|---|---|
| Default (Explain) | Itemised list of what is deleted where; irreversible; export link | Continue; Cancel |
| Confirm | Text field "Type DELETE to confirm" | Delete my account (enabled only when field = DELETE); Cancel |
| Re-auth | Credential Manager sheet | — |
| Loading (Progress) | Step list: Cloud data (`MARKER_WRITTEN`…`CLOUD_PROFILE`) → Account (`AUTH_DELETED`) → This phone (`LOCAL_WIPED`), current step announced | — (Back disabled; leaving is safe, resumes per A5) |
| Error | E6/E7 message | Retry; Cancel (before step 4 only) |
| Offline | E8 message; Continue disabled | Cancel |
| Empty / Permission | N/A | — |
| Done | Completion confirmation | Continue |

**SCR-SYS-01 Deletion resume** — Default "Finishing account deletion…" (progress); Offline E9 with Retry; Re-auth needed (Sign in again); Error with Retry; Done → onboarding. Empty/Permission: N/A.

## 10. Data requirements

| Entity | Fields needed by FS-01 (minimum; ARC-03 authoritative) |
|---|---|
| ENT-01 Profile | `profileId`, `authState` (LOCAL / SIGNED_IN / SIGNED_OUT), `uid?`, `displayName?`, `memberSince`, `updatedAt`, `deviceId`. The email is not stored; it is read from Firebase Auth at display time (ARC-03 §3.2, I-13) |
| ENT-10 SyncOutbox | Rows written for every syncable change on every profile (ARC-03 §3.11); drained after adoption (5.2.5) and on re-sign-in (A3) |
| ENT-14 SyncState | `lastPullWatermark`, `status`, `lastError` — reset on sign-out/remove/deletion |
| ENT-18 DeletionState | `uid`, `phase` (SEC-01 §9.3 / ARC-03 §11.21: `CONFIRMED` · `REAUTHENTICATED` · `MARKER_WRITTEN` · `SYNC_STOPPED` · `CLOUD_SUBCOLLECTIONS` · `VERIFIED` · `CLOUD_PROFILE` · `AUTH_DELETED` · `LOCAL_WIPED` · `DONE`), `startedAt`, `attemptCount`, `lastErrorCode`; stored in `sensitive.db`, not backed up (ARC-03 §3.19) |
| Re-keyed on adoption | All profile-keyed rows (ON UPDATE CASCADE in `learning.db`, plus `sensitive.db` transcripts; ARC-03 §7.2), including ENT-03, ENT-04, ENT-05, ENT-06, ENT-07, ENT-08, ENT-09, ENT-11, ENT-12 |
| Files | `noBackupFilesDir/recordings/<profileId>/` (ADR-015); renamed to `recordings/<uid>/` on re-key (ARC-03 §7.2 step 4) |
| Cloud | `users/{uid}` subtree only (ARC-04) |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `AccountRepository` | signIn, signOut(keepLocal), reauthenticate, deleteAccount (resumable), deletionState Flow |
| `ProfileRepository` | Create local profile; re-key transaction; authState Flow; onboarding gate |
| `SyncRepository` / `SyncEngine` + `SyncWorker` | Drain the outbox after adoption (ARC-03 §7.2), pull and merge, status, cancel on sign-out |
| `PreferencesRepository` | Clear preferences on Remove / deletion |
| `CleanupWorker` | Removal of recording folder (also enforced directly in deletion) |
| Platform | Credential Manager (Google ID), Firebase Auth, Cloud Firestore |

## 12. Offline behaviour

Local profile and all core features work offline (PRD-FR-100). Sign-in and account deletion need a connection and say so (E1, E8). Sign-out works offline in both modes; with *Keep*, un-uploaded changes stay and are uploaded on the next sign-in of the same account (A3). An interrupted deletion resumes when connectivity returns (E9).

## 13. Privacy requirements

- No Firebase network call is made for a user who has never signed in (RECOMMENDATION; supports SRS-PRIV-001 and PP-2).
- SCR-ACC-01 text is generated from the policy constants that drive sync (SRS-PRIV-002, NFR-PRIV-007).
- Uploaded documents never contain audio, file paths or (by default) transcripts (SRS-FR-DATA-006, NFR-PRIV-003).
- UID, email and name never appear in release logs (NFR-SEC-002).
- Deletion leaves 0 documents under `users/{uid}` and 0 local files for the profile, including after resume (NFR-REL-009, NFR-PRIV-005).

## 14. Accessibility notes

TalkBack labels on all buttons ("Delete my account, destructive"); the deletion progress list is a polite live region announcing each step (NFR-A11Y-001); the DELETE field has a visible label and error text, not colour only (NFR-A11Y-006); layouts reflow at 200 % font scale (NFR-A11Y-002); targets ≥ 48 dp (NFR-A11Y-004). Credential Manager UI is system-provided.

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS01-01 | A fresh install with no network | The app is opened | A local profile with a UUID is created, onboarding starts, and no network request is made |
| AC-FS01-02 | A local user on any screen of the core loop | They use Today, Practice, Words and Progress | Every feature works without sign-in |
| AC-FS01-03 | A local user with 5 attempts and 3 words in progress, online | They sign in with Google for the first time | All rows are re-keyed to the UID by the ARC-03 §7.2 procedure (complete, or completed at the next start after a crash) and every syncable record has an outbox entry |
| AC-FS01-04 | An account with cloud attempts from another device | The user signs in on a device with local attempts | After merge, both sets of attempts exist and no passed word stage is lost |
| AC-FS01-05 | The sign-in sheet is open | The user cancels | They return to the previous screen and no error text is shown |
| AC-FS01-06 | A sign-in fails for a network reason | The error is returned | A plain-language message and Retry are shown, with no error codes |
| AC-FS01-07 | A signed-in user | They sign out and keep data | Credentials are cleared, sync stops and their data remains visible as a signed-out user |
| AC-FS01-08 | A signed-in user | They sign out and choose Remove | All local data, preferences and recordings for the profile are deleted and onboarding starts |
| AC-FS01-09 | A signed-in user online | They complete Explain → DELETE → re-auth | Firestore `users/{uid}` is empty, the Auth user is deleted, local data and recordings are gone and Done is shown |
| AC-FS01-10 | Deletion was interrupted after cloud documents were deleted | The app is relaunched | SCR-SYS-01 is shown before any other screen and deletion completes from phase `AUTH_DELETED` |
| AC-FS01-11 | Re-auth is cancelled | During deletion | Nothing is deleted and the Confirm step is shown again |
| AC-FS01-12 | A local user | They choose Delete everything on this device and confirm | All local data and recordings are deleted without network and onboarding starts |
| AC-FS01-13 | Account B signs in on a device holding account A's kept data | Sign-in completes | A's data is not merged into B without the user explicitly removing it (RECOMMENDATION, Issue 1) |
| AC-FS01-14 | A signed-in user whose outbox is empty (everything uploaded) | They open the sign-out dialog | "Remove it from this device" is pre-selected and no unsynced warning is shown (D-21) |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-ACC-001 | Local profile created offline | instrumented | Fresh install, airplane mode, network monitor | Launch app | Profile row with UUID; onboarding shown; 0 network requests | SRS-FR-ACC-001 |
| TC-ACC-002 | Core features without account | manual | Local profile, onboarding done | Complete a Today step, a Practice activity, a word stage, open Progress | All succeed; no sign-in prompt blocks | SRS-FR-ACC-001, PRD-FR-001 |
| TC-ACC-003 | Re-key is crash-safe and idempotent (ARC-03 §7.2) | unit | Fake DBs with rows in all profile tables, transcripts and a recording folder | Run adoption; (a) inject failure inside the `learning.db` transaction; (b) inject a crash after it and restart | (a) all `learning.db` rows keep profileId; (b) `rekey_pending` re-runs steps 2–5 and all rows, transcripts and the folder carry the UID; re-running is a no-op | SRS-FR-ACC-003, SRS-ERR-005 |
| TC-ACC-004 | Outbox filled on adoption | unit | 5 attempts, 3 word states, 2 plans | Run adoption | One outbox entry per syncable record; none for recordings | SRS-FR-ACC-003 |
| TC-ACC-005 | Merge keeps all facts | unit | Local and cloud sets with overlapping and distinct UUIDs | Merge | Union by UUID; duplicates ignored; most advanced word stage kept | SRS-FR-ACC-004, SRS-BR-020, SRS-BR-021 |
| TC-ACC-006 | Cancel sign-in | Robolectric UI | Fake AccountRepository returns Cancelled | Tap Continue with Google | Back on previous screen; no error text node | SRS-FR-ACC-005 |
| TC-ACC-007 | Sign-in error mapping | unit | Fake returns network / no-credential / unknown errors | Map to UI message | Each maps to E1/E2/E3 copy; no exception text | SRS-FR-ACC-005, SRS-ERR-007 |
| TC-ACC-008 | Sign-out keep | instrumented | Signed-in user with data | Sign out → Keep | Credential cleared; SyncWorker cancelled; data visible; authState SIGNED_OUT | SRS-FR-ACC-006 |
| TC-ACC-009 | Sign-out remove | instrumented | Signed-in user with data and recordings | Sign out → Remove | DB rows, prefs and recording folder deleted; onboarding shown | SRS-FR-ACC-006, SRS-BR-022 |
| TC-ACC-010 | Unsynced warning | Robolectric UI | 3 outbox entries pending | Open sign-out dialog | Warning shows "3 changes"; Keep pre-selected | SRS-FR-ACC-006 |
| TC-ACC-011 | Delete account end-to-end | instrumented | Firebase emulator; signed-in user with data in every collection | Explain → DELETE → re-auth → wait | 0 docs under users/{uid}; Auth user gone; 0 local rows/files; Done | SRS-FR-ACC-007, NFR-REL-009 |
| TC-ACC-012 | Delete button gating | Robolectric UI | On Confirm step | Type "delete", "DELET", "DELETE" | Button enabled only for "DELETE" | SRS-FR-ACC-007 |
| TC-ACC-013 | Resume after interruption | instrumented | DeletionState phase=`AUTH_DELETED` persisted | Relaunch app | SCR-SYS-01 first; completes; onboarding | SRS-FR-ACC-008, SRS-ERR-009 |
| TC-ACC-014 | Re-auth cancel deletes nothing | unit | Fake re-auth returns Cancelled | Run deletion flow | No DeletionState persisted; no delete calls | SRS-FR-ACC-007 |
| TC-ACC-015 | Local delete everything | instrumented | Local profile with data, airplane mode | Delete everything on this device | All rows/files deleted; new profile; onboarding | SRS-FR-ACC-009 |
| TC-ACC-016 | Different-account guard | unit | Kept data for uid A; sign-in as uid B | Complete sign-in | No re-key/merge of A's rows into B; conflict state emitted | SRS-FR-ACC-003 (Issue 1) |
| TC-ACC-017 | No PII in logs | unit | Log capture | Run sign-in, sign-out, deletion flows | No uid, email or name in captured release-level logs | NFR-SEC-002 |
| TC-ACC-018 | No Firebase traffic for local user | manual | Fresh install, network capture proxy | Onboard and practise for 10 min | No requests to Firebase hosts | SRS-PRIV-001 (RECOMMENDATION §13) |
| TC-ACC-019 | Sign-out default when fully synced | Robolectric UI | 0 outbox entries pending | Open sign-out dialog | Remove pre-selected; no unsynced warning node | SRS-FR-ACC-006, SEC-REQ-04 (D-21) |

## 17. Traceability

| PRD-FR | SRS-FR | AC | TC |
|---|---|---|---|
| PRD-FR-001 | SRS-FR-ACC-001 | AC-FS01-01, -02 | TC-ACC-001, -002, -018 |
| PRD-FR-002 | SRS-FR-ACC-002, -005 | AC-FS01-05, -06 | TC-ACC-006, -007 |
| PRD-FR-003 | SRS-FR-ACC-003, -004 | AC-FS01-03, -04, -13 | TC-ACC-003, -004, -005, -016 |
| PRD-FR-004 | SRS-FR-ACC-006 | AC-FS01-07, -08, -14 | TC-ACC-008, -009, -010, -019 |
| PRD-FR-005 | SRS-FR-ACC-007, -008 | AC-FS01-09, -10, -11 | TC-ACC-011, -012, -013, -014 |
| PRD-FR-094 | SRS-FR-ACC-009 | AC-FS01-12 | TC-ACC-015 |
| PRD-NFR-008 | NFR-SEC-002 | — | TC-ACC-017 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Typed "DELETE" or an explicit checkbox for confirmation (SRS allows both)? | Product Owner |
| Q2 | After *Remove* on sign-out, restart onboarding or keep the previous learning preferences? | Product Owner |
| Q3 | If deletion resume happens offline, may the user keep using the app on a new local profile while cloud deletion is pending? | Product Owner / Security Engineer |
| Q4 | Should sign-in be offered on SCR-ONB-01 ("Already use SpeakCraft? Sign in") to restore preferences on a new phone? (see FS-02) | Product Owner |
| Q5 | Is account deletion also offered from a web page (Play policy requirement for apps with accounts) — out of app scope but may be needed for store listing (ADR-004)? | Product Owner (D-03) |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | No rule for a **different** Google account signing in on a device that holds another account's kept data (SRS-FR-ACC-006 *Keep*). Literal application of first-sign-in adoption (SRS-FR-ACC-003) could merge one person's history into another's account — a privacy defect | SRS-FR-ACC-003, -006 | Add a rule: adoption only from `LOCAL` profiles or the same UID; otherwise require removal (A4) |
| 2 | Outbox entries are created only "for signed-in users" (SRS-FR-DATA-003), so practice done while signed out (after *Keep*) is never uploaded when the same account signs in again | SRS-FR-DATA-003, SRS-FR-ACC-006 | Add: on every sign-in, enqueue all records not yet acknowledged by the server (idempotent) |
| 3 | Deletion is limited to `users/{uid}`, but the target data skeleton keeps legacy shared collections and legacy user sub-collections read-only during the migration window (D-19). The user's legacy data would survive "delete all associated data" (PRD-FR-005, BR-007, SRS-PRIV-003) | SRS-FR-ACC-007 | Extend step (4) to legacy locations, or delete legacy data at migration end and state the window |
| 4 | SRS-FR-ACC-002 offers sign-in "from onboarding (optional step)", but the onboarding sequence SRS-FR-ONB-001 contains no sign-in step | SRS-FR-ACC-002 vs SRS-FR-ONB-001 | Define the entry point (FS-02 recommends SCR-ONB-01 secondary action) |
| 5 | SRS-FR-ACC-008 requires resume "before any other screen" but does not define behaviour when the device is offline at resume, which could lock the user out indefinitely | SRS-FR-ACC-008 | Decide Q3 |
| 6 | SRS-FR-ACC-009 (local "Delete everything") and SRS-FR-SET-005 ("Clear history → everything", which propagates to Firestore for signed-in users) overlap; their placement and difference for signed-in users is not stated | SRS-FR-ACC-009, SRS-FR-SET-005 | State that ACC-009 is for non-signed-in profiles and SET-005 keeps the account |
