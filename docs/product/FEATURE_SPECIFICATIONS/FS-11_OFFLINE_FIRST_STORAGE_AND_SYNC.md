# FS-11 — Offline-first Storage & Sync

| Field | Value |
|---|---|
| Document ID | FS-11 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-11; PRD §15.11 (PRD-FR-100…103), PRD-NFR-002, PRD-NFR-005; SRS §6.11 (SRS-FR-DATA-001…009), SRS-FR-ACC-003/004, §9, §13 (SRS-AVL-001/002), §15 (SRS-ERR-005/006), §16 (SRS-BR-011, -013, -020, -021, -022, -026); NFR-REL-004…007, NFR-PRIV-003, NFR-PRIV-006, NFR-PERF-008, NFR-PERF-012, NFR-SEC-001; ADR-002, ADR-005, ADR-014, ADR-015; ARC-01 §2, §8, §9, §10; ARC-03 §7–§9; ARC-04 §5; SEC-01 §3.2, §5, §6.2–§6.5, §9.2, §9.3; DES-02 (SCR-SET-07, SCR-SYS-01); FS-01, FS-02, FS-08, FS-10; decisions D-11, D-12, D-16, D-19, D-23 |

> **Authority.** ARC-03 §8 is the authoritative list of synced fields and ARC-03 §9 of migrations; this FS names documents and entities but does not define field lists. Sign-in UI, re-key transaction and account deletion are owned by **FS-01**; export and clear-history UI by **FS-10**. Detail beyond the SRS is labelled RECOMMENDATION; † marks proposed values. No test in this document has been executed (GOV-02 DC-4).

---

## 1. Feature ID & purpose

**FS-11 / CAP-11 (pillar F-2; R0 skeleton, R1 complete).** Make the phone the system of record: every learning change is written to the local database first and is immediately visible; for signed-in users a background job backs up and merges progress through Firestore using idempotent facts and defined conflict rules; the user always knows the backup state; nothing is lost when offline, interrupted, restored to a new phone or upgrading from the legacy app.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "It should work on the train with no signal." | Firestore writes awaited on UI paths hang offline; analysis spins forever (FND-DATA-03, UXF-50) | Aarav, Daniel, Hoang; UN-09 |
| "I lost my streak when I changed phones." | Learning state in local JSON, never synced, lost on reinstall (FND-DATA-02) | Aarav, Daniel |
| "The numbers don't match between screens." | Three persistence mechanisms, no single source of truth (FND-DATA-01, UXF-13) | All; UN-06 |
| "My practice didn't save." | Speaking-session writes rejected by rules (FND-DATA-06, ISS-01) | All |
| "Can I trust the streak?" | Client-writable counters and streaks (FND-SEC-10) | All; UN-04 |
| "Start without signing up, back up later." | Sign-in mandatory (FND-SEC-01) | All; UN-10 |

## 3. Scope

| | R0 | R1 | R2 |
|---|---|---|---|
| **In** | Room schema v1 (`learning.db`, `sensitive.db`), outbox ENT-10, idempotent events ENT-09, sync skeleton, migration design (RD-16 §3) | Local-first write path; `SyncWorker` upload, pull and merge (ADR-014); sync status on SCR-SET-07; first-sign-in hand-off with FS-01; clear-history tombstones and markers; legacy migration profile + vocabulary (D-19); backup rules and restore handling (D-23); `CleanupWorker` retention (SRS-FR-DATA-009) | Transcript sync opt-in to a separate `transcripts` subcollection (D-16, SEC-01 §7.5); sync of R2 entities (ENT-15…17) once specified |
| **Out** | Real-time snapshot listeners (RECOMMENDATION; fixes FND-DATA-05 cost); server-side functions (no backend); cloud recordings and cross-device playback (NG-12); shared or admin collections (NG-10); multiple local profiles; sign-in/out/deletion UI (FS-01); export/clear UI (FS-10) | — | — |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Local write | A Profile exists (FS-01 §5.1). No network, no account |
| Upload | Signed in; network available; outbox has PENDING rows; no account deletion in progress (ENT-18); uploads not paused (§5.4 "update needed") |
| Pull and merge | Signed in and online; triggered on sign-in and app start (SRS-FR-DATA-004) and inside every `SyncWorker` run (RECOMMENDATION) |
| Legacy migration | Signed-in legacy account (persisted Firebase session, FS-01 A6, or first sign-in); legacy profile without `schemaVersion`; `meta/sync.legacyMigration` absent or `NOT_STARTED` |
| Restore handling | `learning.db` restored by Android backup and `device_state` has no `deviceId` (SEC-01 §6.5) |

## 5. Main workflow

### 5.1 Local-first write path (SRS-FR-DATA-001, -002; ADR-002, ADR-014)
1. A feature calls its repository (e.g. `AttemptRepository.saveAttempt`). The repository writes all rows of the change **in one Room transaction** and returns; it never awaits a network call.
2. Facts get a client-generated UUID (idempotency key); re-processing the same UUID has no effect (SRS-BR-020).
3. The same transaction writes the ENT-10 outbox row(s). SRS-FR-DATA-003 requires this for signed-in users; ARC-03 §3.11 writes the row for every profile and drains it only while signed in, so changes made while local or signed out upload after a later sign-in (resolves FS-01 Issue 2; §19 #11). An outbox row is a **pointer** (one pending operation per document); the payload is built from Room at upload time, so several pending changes to one mutable document collapse into one row (ARC-03 §3.11).
4. The transaction commits → Room Flows emit → Today, Progress and Words update reactively (ARC-01 §4.2). Then `SyncWorker` is enqueued (§5.3).

| Local change | Rows in the transaction | Outbox operation | Cloud document |
|---|---|---|---|
| Attempt evaluated | ENT-03, ENT-04, ENT-06 step status, ENT-11 metadata (if retained); transcript to `sensitive.db` | CREATE attempt; UPSERT plan | `users/{uid}/attempts/{attemptId}`; `/plans/{dateKey}` |
| Session completed | ENT-09 `SESSION_COMPLETED`, ENT-05 | CREATE event; UPSERT plan | `/events/{eventId}`; `/plans/{dateKey}` |
| Word stage passed | ENT-07, ENT-09 `WORD_STAGE_PASSED` | UPSERT word; CREATE event | `/words/{wordId}`; `/events/{eventId}` |
| Word reviewed | ENT-08, ENT-07 schedule, ENT-09 `WORD_REVIEWED` | UPSERT word; CREATE event | `/words/{wordId}`; `/events/{eventId}` |
| Plan generated / step swapped | ENT-05, ENT-06 | UPSERT plan | `/plans/{dateKey}` |
| Profile or learning preference changed | ENT-01 / ENT-02 | UPSERT profile | `users/{uid}` |
| Clear history / word reset (FS-10 §5.5) | Local deletes | PURGE_HISTORY / PURGE_WORDS (with marker time) | Collections + `/meta/sync` |
| Never synced (R1) | ENT-11 files, transcripts, ENT-13 ratings, ENT-14, ENT-18, `device_state`; ENT-12 and ENT-15 not synced in R1 (ARC-03 §3.1, OQ-04/OQ-05) | — | — |

### 5.2 Outbox (ENT-10)
ARC-03 §3.11 is authoritative: key (`profile_id`, `collection`, `doc_id`) — one pending operation per document; operations CREATE / UPSERT / DELETE / PURGE_HISTORY / PURGE_WORDS; states PENDING / IN_FLIGHT / FAILED / DEAD; a `generation` counter so an acknowledgement removes the row only if no newer change arrived; purge `cursor` for resumable purges. Rows are deleted on acknowledgement; restored rows are safe to re-send (SEC-01 §6.5).

### 5.3 SyncWorker behaviour and constraints (ADR-005, ARC-01 §9–§10)

| Aspect | Behaviour |
|---|---|
| Work type | One-time unique work `sync`, tagged with the uid, `ExistingWorkPolicy.KEEP`; before finishing, the worker re-reads the outbox and loops while PENDING rows remain (RECOMMENDATION) |
| Constraints | `NetworkType.CONNECTED`; no charging or battery constraint (small payloads; RECOMMENDATION) |
| Back-off | Exponential, initial 30 s † (WorkManager caps the delay at 5 h) |
| Triggers | Local write that added outbox rows (debounced 5 s †); app start when signed in; sign-in / adoption complete (FS-01); connectivity regained; **Retry** on SCR-SET-07 |
| Run order | (1) Read `meta/sync`, apply markers and checks (§5.4); (2) execute DELETE and PURGE operations and write markers; (3) pull and merge per collection; (4) upload CREATE rows in small batches (≤ 20 †) then UPSERT rows; (5) advance watermarks and status in ENT-14. Tombstones run before the pull so cleared items cannot be resurrected; upserts run after the pull so merge rules see the newest cloud state (RECOMMENDATION) |
| Idempotent creates | A retried create of an existing immutable document is denied by the rules; on `PERMISSION_DENIED` the worker `get()`s the document and marks the row DONE if the stored id matches (SEC-01 §5.3 note; SRS-BR-020) |
| Payload | Built by mappers from the ARC-03 §8 allow-list; never transcript, audio, file path or email (SRS-FR-DATA-006, SEC-REQ-10); server timestamps for `updatedAt`/`syncedAt`; `deviceId` from `device_state` |
| Dead letter | A row failing 10 † consecutive times with a non-network error becomes DEAD; status shows Error — Retry; **Retry** resets DEAD rows to PENDING (ARC-01 §10) |
| Interruption | On stop or process death, IN_FLIGHT rows return to PENDING at the next run |
| Never | Awaited from UI; snapshot listeners; collection-group queries or reads outside `users/{uid}` (SEC-01 §5.4) |
| Cancelled by | Sign-out (FS-01 §5.3); account deletion and the deletion marker (FS-01 §5.4, SEC-01 §9.3) |

### 5.4 Pull and merge (SRS-FR-DATA-004, SRS-BR-021, ADR-014)
Each collection is pulled with a bounded, paged query (`> watermark`, ordered, `limit(200)` †) on a server-set timestamp — `updatedAt` for mutable documents and the server stamp of immutable facts (`syncedAt` in SEC-01 §5.3; field naming §19 #3). The watermark per collection scope is stored in ENT-14 (`sensitive.db`, ARC-03 §3.15) and advanced only after the Room merge transaction commits. First sign-in uses watermark 0 (full paged pull). Unknown fields are ignored (SEC-01 §5.4).

| Path | Kind | Local | Merge rule |
|---|---|---|---|
| `users/{uid}` | Mutable | ENT-01, ENT-02 | Last-writer-wins per document (ADR-014: `updatedAt` + `deviceId`; whether the compared clock is the server `updatedAt` or the client `editedAt` proposed in ARC-03 I-04 is fixed in ARC-03 §8). A document with a PENDING local UPSERT keeps the local value until it uploads. First sign-in exception: §5.6 |
| `users/{uid}/attempts/{attemptId}` | Immutable fact | ENT-03, ENT-04 | Insert if `attemptId` unknown; ignore duplicates; never update. Removal only through markers |
| `users/{uid}/events/{eventId}` | Immutable fact | ENT-09 | Insert if unknown; `ProgressCalculator` recomputes streak and totals (SRS-BR-010, -018) |
| `users/{uid}/plans/{dateKey}` | Mutable | ENT-05, ENT-06 | LWW per document (ADR-014) **plus** RECOMMENDATION: a step that is DONE on either side stays DONE with its `attemptId` (SRS-BR-013; §19 #1) |
| `users/{uid}/words/{wordId}` | Mutable, merged | ENT-07 | More advanced stage wins; later review schedule wins (SRS-BR-021). RECOMMENDATION: interval and ease travel with the chosen schedule; `reviews`/`lapses` take the maximum; status re-derived (SRS-BR-017) (§19 #2) |
| `users/{uid}/meta/sync` | Control | ENT-14 | `historyClearedAt` / `wordsResetAt`: purge local rows with `startedAt`/`occurredAt` ≤ marker and drop their outbox rows (SEC-01 §9.2); applied markers are remembered. `deletionRequestedAt`: stop uploads and offer "Finish deleting account" (SEC-01 §9.3, FS-01). Data schema newer than supported: pause uploads, status "Update needed" (SEC-01 §5.4). `legacyMigration`: §5.7 |

**Conflict examples**

| # | Situation | Result |
|---|---|---|
| C1 | Aarav's phone uploads attempt `a1`, the connection drops before the acknowledgement, and the batch is retried | The second create is denied; `get()` finds `a1`; the row is DONE. One cloud document; no duplicate in history |
| C2 | On 2026-11-21 phone A (offline) completes step 2 of the plan; tablet B (online) swaps step 3 later the same day | B's plan document is newer, so plain LWW would show step 2 as PENDING on A after the pull. With DONE-wins, step 2 stays DONE (its attempt `a7` exists on both after sync) and step 3 shows B's swap |
| C3 | Word `negotiate`: phone A passes stage 3 (Use it); phone B, offline, is still at stage 2 | Merged stage 3 on both; B's stage-2 progress is not lost because stage 3 includes it |
| C4 | Both devices hold `negotiate` as Learned. A reviews it successfully (next review 2026-12-01); B reviews it and fails (interval reset, next review 2026-11-22) | Later schedule wins → 2026-12-01 on both; B's `WORD_REVIEWED` event is kept as a fact, so the failure stays in history (limitation noted in §19 #2) |
| C5 | Phone A changes daily time to 15 at 10:00; phone B changes level at 10:05; both upload later, B last | Per-document LWW keeps B's whole profile; A's daily-time change is lost (Q1) |
| C6 | Meera clears "Attempts and transcripts" on phone A; phone B is offline with 3 un-uploaded attempts made before the clear and 1 after | On B's next run the marker purges the 3 older attempts and drops their outbox rows; the newer attempt is kept and uploaded |
| C7 | Phone B's clock is 10 minutes fast | An attempt whose `startedAt` is more than 5 min in the future is rejected by the rules (SEC-REQ-09) → E3. With server `updatedAt` as the LWW clock the fast clock cannot win a conflict; with a client `editedAt` clock (ARC-03 I-04) it could — one reason for the ARC-03 §8 decision |

### 5.5 Sync status (SRS-FR-DATA-005) — SCR-SET-07
Status is derived from ENT-14, the outbox count and WorkManager state; it is shown only on SCR-SET-07, never as a modal, and never blocks a task (PRD-FR-101, SRS-AVL-002). The user-facing word is "back up" (plain language, DES-05 §12; RECOMMENDATION). Rules are evaluated top-down.

| Status | Condition | Copy (draft) | Action |
|---|---|---|---|
| Not signed in | Local profile never signed in | "Using SpeakCraft without an account. Your progress is only on this phone." (FS-01) | Sign in with Google |
| Signed out | `authState = SIGNED_OUT` | "Signed out. Your data is on this phone only." (FS-01) | Sign in with Google |
| Sign in to resume backup (R) | Linked profile without a Firebase session (restore, SEC-01 §6.5) | "Sign in to resume backup. Your practice is saved on this phone." | Sign in |
| Update needed (R) | Cloud schema newer than this app supports | "Update SpeakCraft to keep backing up. Your practice is saved on this phone." | — |
| Error — Retry | Last run ended with a non-network error, App Check failure or DEAD rows | "Backup paused. Your practice is saved on this phone." | Retry |
| Syncing | Worker running | "Backing up…" | — |
| Waiting for connection | PENDING rows and no network | "Waiting for a connection · {n} changes to back up" | — |
| Up to date | Outbox empty and last run succeeded | "Up to date · Last backed up {relative time}" | — |

(R) = RECOMMENDATION state — not in SRS-FR-DATA-005 (§19 #7). The states map to the ENT-14 `status` values of ARC-03 §3.15: `UP_TO_DATE`, `SYNCING`, `WAITING_FOR_NETWORK`, `ERROR`, `PAUSED_RESTORE` (Sign in to resume backup), `PAUSED_SCHEMA` (Update needed) and `PAUSED_DELETION` (§9 deletion row).

### 5.6 First-sign-in adoption — hand-off with FS-01 (SRS-FR-ACC-003, -004)

| Step | Owner | Action |
|---|---|---|
| 1 | FS-01 | Sign-in UI, Credential Manager, Firebase sign-in |
| 2 | FS-01 | Guard: adoption only from a `LOCAL` profile or the same uid (FS-01 A3/A4, Issue 1) |
| 3 | FS-01 + FS-11 | One Room transaction: FS-01 re-keys rows `profileId` → uid (outbox rows written while local follow the re-key, ARC-03 §3.11); FS-11 ensures one outbox row per syncable record (§5.1 table), adding any missing (SRS-FR-ACC-003); none for recordings, transcripts or ratings |
| 4 | FS-11 | Enqueue `SyncWorker`; preflight reads `meta/sync` (deletion marker, schema, `legacyMigration`) |
| 5 | FS-11 | Full pull (watermark 0) and merge **before** uploading upserts. RECOMMENDATION: if `users/{uid}` already exists, the cloud profile is kept and the local onboarding values are discarded (the outbox profile row is dropped); facts are unioned; no completed attempt or passed word stage is discarded (SRS-FR-ACC-004) (§19 #8) |
| 6 | FS-11 | Upload adopted facts and remaining upserts; legacy migration if detected (§5.7) |
| 7 | FS-01 | Success message and merge line (FS-01 A2); SCR-SET-07 status per §5.5 |

### 5.7 Legacy-data migration (SRS-FR-DATA-007, -008; D-19)
1. Trigger per §4. Runs inside `SyncWorker` after the preflight; never on the UI path.
2. Read the legacy `users/{uid}` profile and `users/{uid}/vocabulary/*` (the flashcard store the current UI uses, data_learning_audit §8; the dead local 5-stage store is not a source, FND-DOM-02). Legacy reads are allowed in the L1 window (SEC-01 §5.6).
3. Map (RECOMMENDATION; ARC-03 §9 authoritative) and write Room in one transaction. Re-running produces the same rows (keys = uid, `wordId`).

| Legacy data | Target | Mapping rule (RECOMMENDATION) |
|---|---|---|
| Name | ENT-01 `displayName` | Trim to 40 characters |
| Level (free text; 4 labels in onboarding, 5 in profile, FND-DOM-12) | ENT-01 level (D-17) | Case- and hyphen-insensitive match to the five levels; otherwise Intermediate, flagged "Please check your level" on Profile |
| Goal (free text, UXF-03) | ENT-01 goal (track) | Keywords: interview → Job interviews; present / talk / story → Presentations & storytelling; work / meeting → Workplace communication; pronunciation / clear / accent → Clear speech; otherwise Everyday conversation, flagged |
| Daily goal minutes (5–240) | ENT-01 `dailyMinutes` | ≤ 7 → 5; 8–12 → 10; ≥ 13 → 15 † |
| Vocabulary entries (flashcard reps/mastery, interval ≤ 21 days) | ENT-07 | Match to a content-pack word by normalised lemma. Matched words with ≥ 1 review: stage 1 (Learn) complete, status Learning, no review scheduled — flashcards never tested speaking, so Say it / Use it are not credited. Unmatched (custom) words and personal notes go to ENT-15 NotebookEntry, whose table ships in v1 with the UI in R2 (ARC-03 §3.16, I-19) |
| Sessions, feedback, streaks, minutes, scores, plans, programmes, notifications, local JSON stores, legacy recordings | — | Not migrated (D-19) |

4. Upload: the profile through the one-time legacy-to-v1 replacement (SEC-01 §5.3 (a)), words as v1 documents, then `meta/sync.legacyMigration = DONE` (`SKIPPED` when there is nothing to import).
5. Legacy cloud documents are left untouched (SRS-FR-DATA-007; conflicts with SEC-01 §5.6 L1 — §19 #5).
6. A one-time snackbar on SCR-TODAY-01: "We brought over your profile and {n} words from the previous version."
7. Failure: the transaction rolls back, nothing partial remains, a PII-free diagnostic `LEGACY_MIGRATION_FAILED{step}` is logged, the app stays usable and the migration retries at the next launch (SRS-FR-DATA-008).
8. Legacy local stores and `filesDir/recordings` are not migrated and are deleted after a successful migration (SEC-01 §3.2 #21); RECOMMENDATION: release notes say that old recordings are not carried over.

### 5.8 Backup and restore (D-23; SEC-01 §6.2–§6.5; SEC-REQ-17, -40)

| Item | Android cloud backup and device transfer | Reason |
|---|---|---|
| `learning.db` (profile, preferences incl. the D-24 option, attempts and metrics, plans, words, events, outbox) | Included | Progress survives a phone change without an account (PP-6) |
| `settings` DataStore | Included | Theme, motion, recognition language, reminder on/off |
| `sensitive.db` (transcripts; ENT-14 sync state and ENT-18 deletion state, ARC-03 §3.0) | Excluded | C1 sensitive-voice (SEC-01 §3.1); a restored watermark or deletion state would be wrong on a new phone |
| Recordings in `noBackupFilesDir/recordings/<profileId>/` | Excluded | C1; SRS-PRIV-005 |
| `device_state` (`deviceId`, consent record, tier cache) | Excluded | Device-specific; a restored `deviceId` would collide |
| Firebase Auth / App Check / Installations stores, Firestore cache, WorkManager database, diagnostics | Excluded | SEC-01 §6.3 |

On first start after a restore (`learning.db` present, no `deviceId`): generate a new `deviceId`; start with empty watermarks (ENT-14 is not restored); keep outbox rows; set status `PAUSED_RESTORE`; for a linked profile show "Sign in to resume backup" (the Firebase session is not restored); re-enqueue `CleanupWorker`, reminders (FS-09) and `SyncWorker` because the WorkManager database is not restored (RECOMMENDATION); re-detect the speech tier and require consent again (FS-12). Restored attempts show "Recording and transcript not available on this device". Signing in with the same uid resumes sync without re-keying; a different uid follows FS-01 A4.

### 5.9 Retention clean-up (SRS-FR-DATA-009; ADR-015; SEC-REQ-19)
`CleanupWorker` runs as unique daily periodic work and as an app-start sweep: deletes recordings whose ENT-11 `expiresAt` has passed (within 24 h of expiry); deletes orphan files (no ENT-11 row, or a row without an attempt) and rows without files; applies the D-24 transcript expiry (FS-10 §5.7); prunes plans older than 90 days † locally and, when signed in, queues deletion of the matching cloud plan documents (RECOMMENDATION; ARC-03 to confirm, §19 #12).

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | Process killed during an upload | IN_FLIGHT rows return to PENDING; re-sending is idempotent (NFR-REL-005) |
| A2 | Sign-out during a run | FS-01 cancels the worker; an in-flight batch may commit; nothing further is sent |
| A3 | Account deletion started on another device | `deletionRequestedAt` seen → uploads stop; SCR-SYS-01 offers "Finish deleting account" (FS-01, SEC-01 §9.3). After the Auth user is deleted, token refresh fails → "You've been signed out" and data stays as a local profile (SEC-01 §4.3) |
| A4 | Cloud data schema newer than the app | Uploads paused; status "Update needed"; pulls keep ignoring unknown fields (SEC-01 §5.4) |
| A5 | 7 days offline with 50 attempts | Everything uploads within 2 min of connectivity on RD-MID and local and cloud states match (NFR-REL-007) |
| A6 | Pulled attempt references an activity or `contentVersion` this app version does not have | Stored and listed in history with "This activity isn't available in this version of SpeakCraft" (SRS-BR-026; RECOMMENDATION) |
| A7 | Time-zone change between devices | dateKeys keep the value recorded at event time; merge never rewrites them (SRS-BR-011) |
| A8 | Both devices generate a plan for the same dateKey before syncing | C2 rule (LWW + DONE-wins) |
| A9 | Legacy account with no legacy data | `legacyMigration = SKIPPED`; no snackbar |
| A10 | Same account signs in again after *Keep* sign-out | Outbox rows written while signed out are still pending (ARC-03 §3.11) and upload on the first run after sign-in (FS-01 A3; §19 #11) |
| A11 | Device restored from backup | §5.8 |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Room write fails | Transaction rolled back; retried once; non-blocking "Couldn't save that. Try again."; success is never shown for a failed write; PII-free diagnostic | SRS-ERR-005 |
| E2 | No network or connection drops mid-batch | Rows stay PENDING; exponential back-off; status "Waiting for a connection" | SRS-ERR-006 |
| E3 | Create rejected and `get()` finds no document (validation failure, e.g. clock more than 5 min fast, SEC-REQ-09) | Row DEAD after classification (no endless retry); diagnostic `SYNC_REJECTED{collection,reason}`; status Error — Retry; when the cause is a future timestamp the status adds "Check your phone's date and time" | SRS-ERR-006 |
| E4 | Token refresh fails because the user is disabled or deleted | Sync stopped; "You've been signed out"; local data kept (SEC-01 §4.3, §4.6) | SRS-ERR-007 |
| E5 | App Check token failure | Status Error with "Backup is temporarily unavailable."; back-off (SEC-01 §4.6) | SRS-ERR-006 |
| E6 | Rate limited / quota exceeded | Back-off; Error after the dead-letter threshold | SRS-ERR-006 |
| E7 | Malformed cloud document | Skipped; unknown fields ignored; diagnostic; the rest of the pull continues | SRS-ERR-006 |
| E8 | Legacy migration fails | §5.7 step 7; never blocks use | SRS-ERR-005 (SRS-FR-DATA-008) |
| E9 | Device storage full | Local write fails per E1; the feature shows its storage message (SRS-ERR-004 for recordings) | SRS-ERR-004, -005 |
| E10 | Room schema migration fails after an app update | No destructive fallback in release builds; blocking message "SpeakCraft couldn't finish updating your data. Your data has not been deleted." with Share diagnostics (RECOMMENDATION; ARC-03 §9, RSK-08) | SRS-ERR-005 |

## 8. Business rules

| Rule | Application in this feature |
|---|---|
| SRS-BR-020 | Attempts and events are idempotent by UUID on write and on sync (C1) |
| SRS-BR-021 | Conflicts per ADR-014: facts append-only; mutable documents LWW; word state by most advanced stage and later schedule |
| SRS-BR-013 | Completed plan steps are never removed or reordered — basis for the DONE-wins recommendation |
| SRS-BR-011 | dateKeys are recorded at attempt time; merge never rewrites them |
| SRS-BR-010, -018 | Streak and practice time are derived from facts; no counter is synced as truth (ADR-014 §2) |
| SRS-BR-022 | Retention enforced by `CleanupWorker` |
| SRS-BR-026 | Attempts keep `activityId` + `contentVersion`; unknown content stays readable (A6) |
| SRS-PRIV-001, -005; SRS-AVL-001, -002 | No audio or transcript upload; recordings never backed up; all core features offline; Firebase outages degrade only sign-in and sync |
| RECOMMENDATION | Run order; DONE-wins per plan step; cloud profile kept on first sign-in; dead letter after 10 †; batches ≤ 20 †; pull pages of 200 †; debounce 5 s †; initial back-off 30 s †; legacy mapping table |

## 9. UI states

Sync never blocks a screen, so no screen other than those below has a sync state (N/A by design, PRD-FR-101).

**SCR-SET-07 Account — backup-status block** (the rest of the screen: FS-01 §9)

| State | What the user sees | Actions |
|---|---|---|
| Default | "Up to date · Last backed up {relative time}" | — |
| Loading | Status line skeleton until ENT-14 is read | — |
| Empty | Not signed in / signed out copy (§5.5) | Sign in with Google |
| Error | "Backup paused. Your practice is saved on this phone." (+ date-and-time hint for E3) | Retry |
| Offline | "Waiting for a connection · {n} changes to back up" | — |
| Permission | N/A — no runtime permission | — |
| Syncing | "Backing up…" with an indeterminate indicator | — |
| Update needed | §5.5 copy | — |
| Sign in to resume backup | §5.5 copy | Sign in |
| Deletion requested on another device | "This account is being deleted. Finish deleting it on this phone?" (SEC-01 §9.3) | Finish deleting account (FS-01) |

**SCR-TODAY-01 Today — migration notice only** (the rest: FS-03)

| State | What the user sees | Actions |
|---|---|---|
| Default / Loading / Empty / Offline / Permission | No migration UI | — |
| Migration done | One-time snackbar (§5.7 step 6) | Dismiss |
| Migration failed | Nothing shown; silent retry next launch (SRS-FR-DATA-008) | — |
| Fields flagged | Profile shows "Please check your level" / "Please check your goal" (SCR-PROF-01, FS-10) | Edit |

**SCR-PROG-03 / SCR-PROG-04 History and attempt detail — sync-related rows only** (the rest: FS-08)

| State | What the user sees |
|---|---|
| Attempt from another device | Metrics and feedback; "Recorded on another device — the recording and transcript stay on that device" |
| Attempt restored from backup | "Recording and transcript not available on this device" (SEC-01 §6.5) |
| Unknown activity version | A6 copy; metrics still shown |

## 10. Data requirements

| Entity | Synced to | Notes (field lists: ARC-03 §8) |
|---|---|---|
| ENT-01 Profile, ENT-02 LearningPreferences | `users/{uid}` | Mutable, LWW |
| ENT-03 Attempt, ENT-04 AttemptMetric | `users/{uid}/attempts/{attemptId}` | Immutable; metrics and feedback codes only — no transcript, audio or path |
| ENT-05 SessionPlan, ENT-06 SessionStep | `users/{uid}/plans/{dateKey}` | Mutable; DONE-wins recommendation |
| ENT-07 WordProgress | `users/{uid}/words/{wordId}` | Mutable, merged |
| ENT-08 WordReview, ENT-09 LearningEvent | `users/{uid}/events/{eventId}` | Immutable `WORD_STAGE_PASSED`, `WORD_REVIEWED`, `SESSION_COMPLETED` |
| ENT-10 SyncOutbox | — | §5.2 |
| ENT-14 SyncState | — (reads/writes `users/{uid}/meta/sync`) | In `sensitive.db` (not backed up): per-scope watermark, last pull/push, status, error code, failure count, applied markers, legacy-migration state (ARC-03 §3.15) |
| ENT-11 RecordingFile, ENT-13 FeedbackRating, ENT-18 DeletionState | Never | Local only; ENT-18 in `sensitive.db` (ARC-03 §3.0) |
| ENT-12 SavedActivity, ENT-15 NotebookEntry | Not synced in R1 | ARC-03 §3.1 (OQ-04, OQ-05); §19 #13 |
| Transcripts (`sensitive.db`) | Never in R1; R2 opt-in to `users/{uid}/transcripts/{attemptId}` (D-16) | — |
| Local stores | `learning.db` (backed up), `sensitive.db` (not backed up), `settings` (backed up), `device_state` (not backed up), recordings in `noBackupFilesDir/recordings/<profileId>/` | Canon R-1; SEC-01 §3.2 |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `SyncRepository`, `SyncEngine`, `SyncWorker` | Outbox drain, pull, merge, markers, status Flow |
| `ProfileRepository`, `AttemptRepository`, `PlanRepository`, `WordRepository`, `PreferencesRepository` | Transactional writes with outbox rows; merge targets |
| `AccountRepository` | Auth state and uid (repositories never take a uid parameter, SEC-01 §5.4) |
| `ProgressCalculator` | Recompute derived numbers after merges and purges |
| `CleanupWorker` | §5.9 |
| `Clock` / `DateKeyProvider`, `AppLogger` | Testable time; PII-free codes |
| Platform | Room (ADR-002), WorkManager (ADR-005), Cloud Firestore (named database; SDK offline persistence decision in ARC-03 §8), Firebase Auth, Android backup rules XML |

## 12. Offline behaviour

| Capability | Offline |
|---|---|
| Onboarding, Today, Practice, Words, Progress, Settings | Fully functional (PRD-FR-100, ARC-01 §8) |
| Saving attempts, plans, words, events | Immediate, local |
| Backup | Deferred in the outbox; status "Waiting for a connection" |
| Pull from other devices | Deferred until online |
| Clear history cloud part | Deferred (FS-10 A5) |
| Legacy migration | Deferred; retried |

## 13. Privacy requirements

- Uploaded documents contain only ARC-03 §8 fields; never audio, file paths, email or (by default) transcripts (SRS-FR-DATA-006, NFR-PRIV-003, SEC-REQ-10).
- A user who never signs in causes no Firebase traffic (SEC-REQ-02).
- Every query is scoped to `users/{uid}` from the auth state (SEC-REQ-07, SEC-REQ-18).
- Backup includes only `learning.db` and `settings` (D-23, SEC-REQ-17, NFR-PRIV-006); restore handling per SEC-REQ-40.
- Sync logs use codes, never uids, paths with uids or payloads (NFR-SEC-002, SEC-01 §10.1).
- R2 transcript sync requires its own disclosure and opt-in (D-16, SEC-01 §7.5).

## 14. Accessibility notes

The status line is text, never colour alone (A11Y-23, NFR-A11Y-006); a status change is announced politely only after a user-initiated Retry or when it becomes Error (A11Y-08) to avoid chatter; Retry is a 48 dp button (A11Y-19); copy reflows at 200 % font scale (NFR-A11Y-002); the migration snackbar follows TC-A11Y-020.

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS11-01 | A local user in airplane mode | They complete an attempt | It appears in Today and Progress without any network request |
| AC-FS11-02 | A repository write in which the last insert fails | The transaction runs | No row of the change remains and no success is shown |
| AC-FS11-03 | A signed-in user | They complete an attempt that finishes a session | Outbox rows exist for the attempt, the event and the plan, and none for the recording, transcript or rating |
| AC-FS11-04 | Any upload | The payload is built | It contains only ARC-03 §8 fields and no transcript, audio, path or email |
| AC-FS11-05 | An attempt whose upload was committed but not acknowledged | The batch is retried | Exactly one cloud document exists and the outbox row is DONE |
| AC-FS11-06 | 50 attempts made over 7 days offline on RD-MID | Connectivity returns | All are uploaded within 2 minutes and local and cloud sets match |
| AC-FS11-07 | Cloud attempts from another device, some already local | A pull runs | Missing attempts are inserted, duplicates ignored, and streak and totals recompute |
| AC-FS11-08 | Conflict C2 | Both devices sync | Step 2 is DONE on both and step 3 shows the later swap |
| AC-FS11-09 | Conflicts C3 and C4 | Both devices sync | The merged word has the most advanced stage and the later schedule, and both review events exist |
| AC-FS11-10 | A profile changed on two devices | Both sync | The later server write wins; a document with a pending local change keeps the local value until it uploads |
| AC-FS11-11 | `historyClearedAt` set by another device | This device pulls | Local items at or before the marker are deleted and are not re-uploaded |
| AC-FS11-12 | Each §5.5 condition | SCR-SET-07 is open | The matching status copy and action appear and no screen is blocked |
| AC-FS11-13 | A local user with history signs in to an account that already has a cloud profile and attempts | Adoption completes | All local and cloud attempts and passed stages exist, and the cloud profile values are kept (RECOMMENDATION) |
| AC-FS11-14 | A signed-in legacy user with a legacy profile and 12 vocabulary entries | R1 starts for the first time | Profile and matched words are imported per §5.7, legacy documents are unchanged, `legacyMigration = DONE`, and a second run changes nothing |
| AC-FS11-15 | Legacy migration fails mid-way | The user keeps using the app | No partial data is visible and the migration retries at next launch |
| AC-FS11-16 | A release build | Backup rules are inspected | Cloud backup and device transfer include exactly `learning.db` and `settings` |
| AC-FS11-17 | A device restored from backup with pending outbox rows | The app starts | A new `deviceId` is generated, watermarks are reset, "Sign in to resume backup" is shown for a linked profile, and after sign-in no duplicate cloud document is created |
| AC-FS11-18 | Recordings past their expiry and orphan files | `CleanupWorker` runs | All are deleted within 24 h of expiry and valid recordings remain |
| AC-FS11-19 | A create rejected by validation | The worker classifies it | The row becomes DEAD without endless retries, status shows Error — Retry, and Retry re-attempts it |
| AC-FS11-20 | Sync running | The user signs out, or a deletion marker is pulled | Uploads stop at once and no further write is sent |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-DATA-001 | Offline save, no network for local user | instrumented | Local profile; airplane mode; network monitor | Complete an attempt; open Progress | Attempt visible; 0 requests | SRS-FR-DATA-001, SRS-AVL-001 |
| TC-DATA-002 | Write atomicity | unit | In-memory Room; DAO failing on last insert | Save attempt | No partial rows; error result; no success event | SRS-FR-DATA-001, SRS-ERR-005 |
| TC-DATA-003 | No main-thread I/O | instrumented | Debug build with StrictMode penalty death | Run core loop and a sync | 0 violations | NFR-PERF-012 |
| TC-DATA-004 | Outbox rows per change | unit | Fake auth: local and signed-in | Perform each §5.1 change | Expected rows only (written in both states, drained only when signed in); none for recordings, transcripts, ratings | SRS-FR-DATA-003 |
| TC-DATA-005 | Outbox coalescing | unit | — | Change profile 3 times offline | One PENDING profile row; payload equals latest Room state | SRS-FR-DATA-003 (RECOMMENDATION) |
| TC-DATA-006 | Payload allow-list | unit | Mapper with entities containing transcript and paths | Map each collection | Only ARC-03 §8 keys; forbidden keys absent (see TC-SEC-026) | SRS-FR-DATA-006, NFR-PRIV-003, SEC-REQ-10 |
| TC-DATA-007 | Idempotent re-create | instrumented | Firestore emulator with draft rules | Upload attempt; replay the same create | Second denied → `get()` → row DONE; one document | SRS-FR-DATA-002, SRS-BR-020, NFR-REL-005 |
| TC-DATA-008 | Worker configuration | unit | `work-testing` | Enqueue from each trigger | Unique work; CONNECTED constraint; exponential back-off; KEEP policy | SRS-FR-DATA-003, SRS-ERR-006 |
| TC-DATA-009 | Kill during upload | instrumented | Emulator; 20 PENDING rows | Kill process mid-batch; relaunch | All uploaded once; no IN_FLIGHT left | NFR-REL-004, NFR-REL-005 |
| TC-DATA-010 | Long-offline recovery | instrumented | RD-MID; 50 attempts over 7 days (fake clock) | Restore network; time it | ≤ 2 min; local and cloud sets equal | NFR-REL-007 |
| TC-DATA-011 | Fact merge | unit | Local and cloud sets with overlap | Merge | Union by id; duplicates ignored; aggregates recomputed | SRS-FR-DATA-004, SRS-BR-020 |
| TC-DATA-012 | Plan merge DONE-wins | unit | Fixture C2 | Merge both directions | Step 2 DONE; step 3 from newer doc | SRS-BR-013, SRS-BR-021 |
| TC-DATA-013 | Word merge | unit | Fixtures C3, C4 and equal-stage ties | Merge both directions | Max stage; later schedule with its interval/ease; counters max | SRS-BR-021 |
| TC-DATA-014 | Profile LWW | unit | Older/newer server stamps; pending local row | Merge | Newer wins; pending local kept | SRS-BR-021 |
| TC-DATA-015 | Marker purge | unit | Local items before/after marker; outbox rows | Apply marker | Older purged and outbox rows dropped; newer kept (see TC-SEC-018) | SEC-REQ-23 |
| TC-DATA-016 | Run order | unit | Fake remote recording call order | Run worker with tombstone, creates, upserts | Order: meta → tombstones → pull → creates → upserts → watermark | ADR-014 (RECOMMENDATION) |
| TC-DATA-017 | Status derivation | unit | Fakes for ENT-14, outbox, WorkInfo, auth | Evaluate every §5.5 condition | Correct status for each; top-down precedence | SRS-FR-DATA-005 |
| TC-DATA-018 | Status UI | Robolectric UI | Each status; font scale 2.0 | Render SCR-SET-07 block | Copy and action per §5.5; no clipping; no modal | SRS-FR-DATA-005, SRS-AVL-002 |
| TC-DATA-019 | Adoption end to end | instrumented | Emulator; cloud profile + attempts; local history | Sign in (fake credential) | Union of facts; cloud profile kept; outbox empty after run | SRS-FR-ACC-003, SRS-FR-ACC-004 |
| TC-DATA-020 | Legacy mapping | unit | Legacy fixtures: 4/5 level labels, free-text goals, 5–240 minutes, vocabulary entries | Run mapper twice | §5.7 table results; second run identical | SRS-FR-DATA-007 |
| TC-DATA-021 | Legacy migration on emulator | instrumented | Emulator seeded with a legacy profile and vocabulary | First R1 launch | v1 profile and words written; legacy docs unchanged; `legacyMigration = DONE` (see TC-SEC-024) | SRS-FR-DATA-007 |
| TC-DATA-022 | Legacy failure is non-blocking | unit | Fake remote failing at step 3 | Run migration; then rerun with success | No partial rows; diagnostic code; success on rerun | SRS-FR-DATA-008 |
| TC-DATA-023 | Backup rules | unit | Parse both XML files | Compare to D-23 set | Exactly `learning.db` + `settings` (see TC-SEC-012) | SRS-PRIV-005, SEC-REQ-17 |
| TC-DATA-024 | Restore handling | instrumented | `bmgr` local transport; signed-in profile with outbox rows | Back up, clear, restore, launch | New `deviceId`; watermarks reset; "Sign in to resume backup"; no duplicates after sign-in (see TC-SEC-050) | SEC-REQ-40 |
| TC-DATA-025 | Retention and orphan sweep | unit | Fake clock; expired, valid and orphan files | Run `CleanupWorker` | Expired and orphans deleted; valid kept | SRS-FR-DATA-009, SEC-REQ-19 |
| TC-DATA-026 | Dead letter and Retry | unit | Fake remote returns validation denial | Run 10 times; then Retry | Row DEAD; status Error; Retry resets to PENDING | SRS-ERR-006 |
| TC-DATA-027 | Stop conditions | unit | Running worker | Sign out; deliver deletion marker; invalidate token | No write after each event; correct status | SRS-FR-ACC-006, SRS-FR-ACC-008 |
| TC-DATA-028 | Airplane-mode core loop | manual | Reference devices RD-LOW/MID/HIGH | Run the TST-01 offline script | All core-loop flows pass offline | PRD-FR-100, NFR-REL-006 |

Room schema migration tests are TC-MIG-nnn, owned by TST-01 §16 (ARC-03 §9).

## 17. Traceability

| PRD-FR / NFR | SRS-FR / rule | AC | TC |
|---|---|---|---|
| PRD-FR-100 | SRS-FR-DATA-001; SRS-AVL-001 | AC-FS11-01, -02 | TC-DATA-001, -002, -003, -028 |
| PRD-FR-101 | SRS-FR-DATA-003, -005, -006; SRS-AVL-002 | AC-FS11-03, -04, -05, -06, -12, -19, -20 | TC-DATA-004 … -010, -017, -018, -026, -027 |
| PRD-FR-102 | SRS-FR-DATA-002, -004; SRS-FR-ACC-004; SRS-BR-020, -021 | AC-FS11-07 … -11, -13 | TC-DATA-011 … -016, -019 |
| PRD-FR-103 | SRS-FR-DATA-007, -008 | AC-FS11-14, -15 | TC-DATA-020, -021, -022 |
| PRD-NFR-005 | NFR-REL-004, -005, -007 | AC-FS11-05, -06 | TC-DATA-007, -009, -010 |
| PRD-NFR-002 | NFR-PRIV-003, -006; SRS-PRIV-005; SRS-FR-DATA-009 (ADR-015) | AC-FS11-04, -16, -17, -18 | TC-DATA-006, -023, -024, -025 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Keep per-document LWW for `users/{uid}` (ADR-014) or move to per-field LWW so C5 loses nothing? | Database Architect / Product Owner |
| Q2 | Approve DONE-wins for plan steps (§5.4, §19 #1) | Product Owner |
| Q3 | On first sign-in with existing cloud data: keep the cloud profile (recommended), apply LWW, or ask as in SEC-01 §4.2? | Product Owner / Security Engineer |
| Q4 | Legacy words that match the bank are credited with stage 1 only (§5.7) — acceptable to legacy users, or should flashcard reviews earn more? | Product Owner (D-19) |
| Q5 | Is pull on sign-in and app start enough (SRS-FR-DATA-004), or should a running app also pull periodically for multi-device users? | Product Owner |
| Q6 | Delete migrated legacy subcollections in R1 (SEC-01 L1) or leave them untouched until L2 (SRS)? | Product Owner / Security Engineer |
| Q7 | Should saved activities (ENT-12) sync in a later release (ARC-03 OQ-04)? | Product Owner (FS-06) |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | Per-document LWW on `plans/{dateKey}` can turn a step completed on one device back to PENDING (C2), contradicting "completed steps are never removed" | ADR-014 §3, SRS-BR-021 vs SRS-BR-013 | Add a step-level DONE-wins rule to ADR-014 / ARC-03 §8 |
| 2 | Word merge "more advanced stage and later schedule" does not say which interval, ease and counters go with the chosen schedule, or what happens when the later schedule belongs to the less advanced copy; a failed review on one device is overridden (C4) | SRS-BR-021, ADR-014 §3 | ARC-03 §8 to define the field-level merge |
| 3 | The pull uses "`updatedAt > lastPull`" for all documents, but immutable attempts and events have no `updatedAt` in the SEC-01 rules (they carry `syncedAt`). ARC-03 §3.15 already watermarks on "`syncedAt`/`updatedAt`" per scope | ADR-014 §5, SRS-FR-DATA-004 vs SEC-01 §5.3 | Align the ADR-014 and SRS wording to ARC-03 |
| 4 | SEC-01 §3.2 lists "per-attempt delete" for attempts and transcripts. An outbox DELETE (ARC-03 §3.11) removes the cloud document, but devices that already pulled it are never told (the pull reads only changed documents and there is no per-document tombstone) | SEC-01 §3.2 #1–#4, §9.2; ARC-03 §3.11 | Define per-document tombstones in ARC-03 §8 or remove per-attempt delete |
| 5 | SRS-FR-DATA-007 says legacy documents are left untouched; SEC-01 §5.6 (L1) says the R1 client deletes the legacy subcollections it has migrated | SRS-FR-DATA-007 vs SEC-01 §5.6, §3.2 #22 | Decide (Q6) and align both |
| 6 | SRS-FR-DATA-007 does not define how legacy free-text level and goal, minutes 5–240 or flashcard vocabulary (reps/mastery, no stages) map to the 5 levels, 5 tracks, 5/10/15 minutes and the 5-stage lifecycle | SRS-FR-DATA-007, D-19 | ARC-03 §9 to adopt or replace the §5.7 mapping |
| 7 | SRS-FR-DATA-005 lists four statuses, but SEC-01 requires further user-visible states: update required (§5.4), "sign in to resume backup" (§6.5), App Check unavailability (§4.6) and deletion requested elsewhere (§9.3) | SRS-FR-DATA-005 | Extend SRS-FR-DATA-005 with the §5.5 states |
| 8 | First sign-in with data on both sides: SEC-01 §4.2 asks "Merge / Keep separate"; SRS-FR-ACC-003/004 and FS-01 A2 merge automatically; "enqueue upload of all syncable records" plus LWW can overwrite an existing cloud profile with fresh local onboarding defaults | SEC-01 §4.2 vs SRS-FR-ACC-003/004, ADR-014 §7 | Decide Q3; state the profile rule in SRS-FR-ACC-004 |
| 9 | Three documents place the sync watermark and deletion state differently: canon R-1 describes `sensitive.db` as the transcript store only; SEC-01 §3.2 #13 and §9.3 keep both in `device_state`; ARC-03 §3.0 puts ENT-14 and ENT-18 in `sensitive.db`. All three keep them out of backup, so behaviour is the same, but the store names disagree | Canon R-1, SEC-01 §3.2 #13, §9.3, ARC-03 §3.0 | Align canon R-1 and SEC-01 to ARC-03 §3.0 |
| 10 | ARC-01 §9 leaves Firestore SDK offline persistence "disabled for app data paths or left default"; SEC-01 §3.2 #16 assumes an SDK cache that must be cleared on sign-out | ARC-01 §9, SEC-01 §3.2 #16 | Decide in ARC-03 §8 (recommend disabled) |
| 11 | SRS-FR-DATA-003 creates outbox rows only "for signed-in users", so changes made while signed out would never upload after the same account signs in again (FS-01 Issue 2). ARC-03 §3.11 writes them for every profile and drains them only while signed in | SRS-FR-DATA-003 vs ARC-03 §3.11 | Align SRS-FR-DATA-003 to ARC-03 |
| 12 | Session plans have a 90-day † local retention (SRS §9), but cloud plan retention and pruning are undefined ("ARC-03 to confirm", SEC-01 §3.2 #6) | SRS §9, SEC-01 §3.2 #6 | ARC-03 to state cloud plan retention |
| 13 | Saved activities (ENT-12) are absent from SRS §9; ARC-03 §3.1 keeps them local in R1 (OQ-04), while FS-06 left their sync undefined | SRS §9, ARC-03 §3.1 | Add ENT-12 to SRS §9 as "local only (R1)"; decide Q7 |
