# Stage A plan & canonical decisions (for me + writer agents)

## Done
GOV-02 DOCUMENT_CONTROL, RD-01 (agent), RD-02, RD-05 (agent, decisions remapped), RD-11 (agent), RD-13, RD-14, RD-15, RD-16, ADR README + ADR-001..015.

## Evidence files (scratch/findings)
platform_security_audit.md, ai_media_audit.md, data_learning_audit.md, ui_audit.md, docs_claims.md, competitor_research.md, FND_ID_MAP.md

## Canonical IDs
Capabilities CAP-01..12 (RD-16 §2). FS-01..FS-12 = one per CAP:
FS-01 Account & sign-in; FS-02 Onboarding & personalisation; FS-03 Today daily session; FS-04 Speaking practice session; FS-05 Honest feedback & evaluation; FS-06 Practice library; FS-07 Words vocabulary lifecycle; FS-08 Progress & streak; FS-09 Reminders & inbox; FS-10 Profile/settings/privacy/data rights; FS-11 Offline-first storage & sync; FS-12 Speech capability management.
Pillars P-1..P-4, F-1, F-2. Principles PP-1..PP-8, UX-1..10, EP-1..6.
Releases R0 Foundation, R1 Core loop MVP, R2 Depth, R3 Reach.
Decisions D-01 segment; D-02 evaluation philosophy; D-03 distribution/updater; D-04 cloud AI removal; D-05 account optional; D-06 no composite score (free speaking); D-07 monetisation out of scope; D-08 creator content removal/legal; D-09 success measurement; D-10 recording retention default 30d; D-11 Room; D-12 WorkManager; D-13 crash reporting; D-14 minSdk & speech tier policy/online consent; D-15 applicationId/namespace change; D-16 transcript cloud sync opt-in (R2); D-17 level taxonomy 5 levels; D-18 brand palette; D-19 legacy data migration scope (minimal: profile + vocab); D-20 R1 includes Words tab (sequencing).
Speech tiers T1 (API33+ on-device + EXTRA_AUDIO_SOURCE), T2 (API31-32 on-device recognizer owns mic), T3a (consented online), T3b (audio-only). Spikes SPK-01 device recognition, SPK-02 grammar-rule precision corpus, SPK-03 Whisper TFLite research (R3).
Source aliases: PO-REQ-1119 = DOC-059 paste; PO-REQ-1120 = Stage A brief.

## IA (DES-02) — 4 tabs
Today (SCR-TODAY-01) | Practice (SCR-PRAC-01 library, -02 track detail, -03 activity detail) | Words (SCR-WORD-01 home, -02 word learn flow, -03 review session; R2 -04 notebook, -05 quiz) | Progress (SCR-PROG-01 overview, -02 metric detail, -03 history list, -04 attempt detail/replay; R2 -05 check-in)
Full-screen flows: Session player SCR-SESS-01 (prompt/prepare/record/processing/result/retry), Onboarding SCR-ONB-01..06, Profile SCR-PROF-01, Settings SCR-SET-01..07 (learning, reminders, speech & privacy, appearance, data & privacy, account, about), Sign-in SCR-ACC-01, Inbox SCR-INBOX-01 (R2), Permission education SCR-PERM-01.
Top bar on Today: avatar->Profile; bell (R2 inbox).

## Templates R1
TPL-READ Read aloud; TPL-REPEAT Listen & repeat; TPL-ANSWER Answer a question; TPL-STRUCT Structured answer (STAR/PREP); TPL-STORY Tell a story. R2 TPL-ROLE scripted role-play.
Tracks: TRK-EVERYDAY, TRK-WORK, TRK-INTERVIEW, TRK-PRESENT, TRK-CLEAR.

## Metrics (ARC-05) measured vs estimated
Measured: duration, speaking time, speech ratio, WPM (words/speaking time), long pauses count, words count, target-word used, reference match % (read/repeat only, labelled "recognised as intended").
Estimated: fillers/min (caveat ASR drops um/uh), lexical variety (MATTR), grammar flags (high-precision rules only), structure elements (STAR/PREP cue detection), hedging.
No composite overall for free speaking (D-06). Word stages & read-aloud: pass/fail thresholds (75/75/70 per PO-REQ-1119) — "Inconclusive" when evidence insufficient.

## Target data skeleton
Room (ADR-002): profile, attempt, attempt_metric, session_plan, session_step, word_progress, word_review, learning_event (idempotent), sync_outbox, recording_file; content from bundled versioned JSON assets (content_pack v1) loaded to memory/Room read-only tables.
Firestore (named DB retained): users/{uid} (profile); users/{uid}/attempts/{attemptId} (immutable metrics only, no transcript, no paths); users/{uid}/plans/{dateKey}; users/{uid}/words/{wordId}; users/{uid}/events/{eventId} (immutable facts: WORD_STAGE_PASSED, WORD_REVIEWED, SESSION_COMPLETED); users/{uid}/meta/sync. Retire all shared collections + legacy user subcollections (kept read-only until migration window ends).

## Remaining deliverables & owner
Me: RD-03, RD-00, RD-06, RD-07, RD-08, RD-09, RD-10, RD-12, BRD, PRD, SRS, NFR, DES-02 IA, ARC-01, ARC-05, PLN-01..05, GOV-01, RD-99.
Agents wave1: W1 DES-01, DES-05, DES-07 ; W2 SEC-01, ENG-01 ; W3 RD-04.
Agents wave2 (after PRD/SRS/IA): W4 FS-01..12 + US + UC ; W5 DES-03, DES-04, DES-06, DES-08 ; W6 ARC-02, ARC-03, ARC-04 ; W7 TST-01.

## Added decisions (consolidated from SEC-01 / ENG-01)
D-21 sign-out default (SD-01): Remove when all synced; if unsynced, warn & default Keep (SRS-FR-ACC-006 + SEC-REQ-04 updated)
D-22 audio in export (SD-04): SEC rec JSON-only R1; SRS-FR-SET-004 says optional ZIP -> consolidated rec: optional ZIP in R1 (portability)
D-23 backup policy (SD-02): allowBackup=true include-only learning.db + settings
D-24 local transcript retention (SD-03): keep with attempt + option to delete with recordings
D-25 data-safety declaration for T3a (SD-05): declare audio shared, optional
D-26 App Check enforcement criteria (SD-06)
D-27 Firebase projects dev+prod (ED-01); D-28 signing key strategy (ED-04); D-29 rules deploy manual R0–R1 (ED-06); D-30 privacy/deletion page hosting GitHub Pages (ED-09); D-31 Firestore PITR (ED-10)
Done since: RD-00, 03, 04(agent), 06, 07, 08, 09, 10, 12, BRD, PRD, SRS, NFR, DES-02, ARC-01, ARC-05, SEC-01(agent), ENG-01(agent), PLN-01..05.
Remaining: GOV-01 RTM (after FS/US), RD-99, consistency review.

---

## Resume canon (2026-11-21) — binding for every writer regenerating the remaining documents

Work paused on 2026-11-20 and resumed on 2026-11-21. New documents are dated **2026-11-21**. Evidence lives in `docs/redevelopment/working_papers/` (not in any scratchpad path).

### R-1 Entity IDs (ARC-03 §3 defines them; everyone else uses these names)
ENT-01 Profile · ENT-02 LearningPreferences (Room, profile-scoped, synced inside `users/{uid}`; device/UI-only preferences stay in the `settings` DataStore; `PreferencesRepository` fronts both) · ENT-03 Attempt (immutable once evaluated; transcript text is NOT in this table — it lives in `sensitive.db`, SEC-01 §3.2 #2) · ENT-04 AttemptMetric · ENT-05 SessionPlan · ENT-06 SessionStep · ENT-07 WordProgress · ENT-08 WordReview · ENT-09 LearningEvent · ENT-10 SyncOutbox · ENT-11 RecordingFile · ENT-12 SavedActivity · ENT-13 FeedbackRating ("Was this accurate?"; separate table so Attempt stays immutable; local only, not synced — SEC-01 §3.2 #5) · ENT-14 SyncState · ENT-15 NotebookEntry (R2) · ENT-16 InboxMessage (R2) · ENT-17 QuizAttempt (R2) · ENT-18 DeletionState.
Local stores (SEC-01 §3.2, §6): `learning.db` (Room, backed up include-only), `sensitive.db` (Room, transcripts, excluded from backup), `settings` DataStore (backed up), `device_state` (not backed up), recordings in `noBackupFilesDir/recordings/<profileId>/`. Content pack = bundled versioned JSON assets (ARC-03 §6), referenced by name, no ENT ID.

### R-2 Section numbers already cited by finished documents — keep exactly
| Document | Section | Must contain |
|---|---|---|
| ARC-02 | §2 | Package / class map (ADR-012), CMP-nn IDs |
| ARC-02 | §9 | Configuration and compile-time feature flags (WBS T-02.3.2) |
| ARC-03 | §3–§5 | Entities, relationships/ER, indexes & constraints (WBS T-03.1.1) |
| ARC-03 | §6 | Content pack schema (SRS §9, FS-00) |
| ARC-03 | §7–§8 | §7 local persistence; §8 Firestore target schema + sync fields — the authoritative "what syncs" list (NFR-PRIV-003, ADR-014, ARC-01 §9, GAP-25/26) |
| ARC-03 | §9 | Migrations incl. legacy import (D-19), migration test harness (WBS T-03.1.2, RSK-08) |
| ARC-03 | §10 | Export file schema (SRS-FR-SET-004) |
| ARC-04 | §5 | Internal module (repository/service) interfaces (WBS T-02.2.2, T-03.2.1) |
| DES-06 | §2.3 | Brand launch moment ≤ 1 s |
| DES-06 | §2.4 | Contextual first-use tips (replaces the four tutorial systems) |
| DES-06 | §3 | Screen redesign proposals; SCR-TODAY-01 includes the greeting (GOV-02 §6.1) |
| DES-07 | §3 | TalkBack / semantics rules A11Y-02…A11Y-12 |
| DES-07 | §15 | Test cases TC-A11Y-nnn |
| TST-01 | §5 | Test device matrix (NFR §1 RD-LOW/MID/HIGH) |
| TST-01 | §9 | Honesty review checklist (PRD acceptance, PLN-05 DoD) |
| TST-01 | §10 | Entry/exit criteria per level and release; R0 exit = RD-06 §5 |

### R-3 DES-07 IDs already used by DES-05 / DES-01 (meanings are fixed)
A11Y-02/03 contentDescription rules (meaningful vs decorative/duplicate) · A11Y-06/08/26 state semantics (state descriptions, live regions, record-button states) · A11Y-07 merged semantics for cards/tiles/rows · A11Y-09 focus moves to result header and it is announced · A11Y-10 modal content in real Dialog/ModalBottomSheet with focus containment · A11Y-12 charts have a text summary and list alternative · A11Y-13 200 % font, no fixed-height text containers · A11Y-19 48 dp targets · A11Y-20 pager dots/gestures have 48 dp targets and button alternatives · A11Y-21 reduced motion (system setting + in-app preference) · A11Y-23 colour is never the only carrier · A11Y-25 labelled speech-rate control (Listen & repeat) · A11Y-30 focused content not hidden by sticky bars · A11Y-32 errors announced and say how to fix · A11Y-34 units spelled out in semantics · A11Y-36 all strings in resources, plurals · A11Y-37 locale-aware formatting. §3 holds A11Y-02…12. Free IDs may be assigned to other rules.
TC-A11Y-001 token contrast (JVM) · 002…006 and 009 component semantics · 007 200 % font no overflow · 008 touch targets · 010 reduced motion · 020 snackbar duration/announcement · 023 font scale 1.0/1.3/1.5/2.0 on 360×640 dp · 024 both themes. OQ-A11Y-1 = streak/step rule for users who cannot complete a speaking step.

### R-4 Other ID owners
CMP-nn → ARC-02 · FLOW-01…14 → DES-04 · US-nnn → USER_STORIES · UC-nn → USE_CASES · AC-DES-nn → DES-06 · RDD-nn → DES-08 · TC-PERF/REL/PRIV/COMP/REG/MIG/CONT-nnn → TST-01 §16 (TC-CONT = D-08 content compliance, added 2026-11-21) · TC-SEC (SEC-01), TC-A11Y (DES-07), TC-UX (RD-15) are referenced, never redefined. FS file names: FS-07_WORDS_VOCABULARY_LIFECYCLE.md, FS-08_PROGRESS_AND_STREAK.md, FS-09_REMINDERS_AND_INBOX.md, FS-10_PROFILE_SETTINGS_PRIVACY_DATA_RIGHTS.md, FS-11_OFFLINE_FIRST_STORAGE_AND_SYNC.md, FS-12_SPEECH_CAPABILITY_MANAGEMENT.md. Every FS follows the 19-section template in FS-00 §2.

### R-5 Consolidated recommendations for decisions writers must reflect (all still Pending — Product Owner)
- **D-21 sign-out default:** "Remove from this device" is pre-selected when every change is synced; if un-uploaded changes exist, show the count warning and pre-select "Keep on this device".
- **D-22 audio in export:** consolidated recommendation = JSON export always; a separate, off-by-default "Include my recordings (ZIP)" option in R1, with a warning that recordings contain the user's voice (SRS-FR-SET-004, PRD-FR-093 "all personal data"). Alternative (SEC-01 SD-04 a): JSON only in R1, ZIP in R2.
- **D-23 backup:** `allowBackup=true` with include-only rules (`learning.db` + `settings`).
- **D-24 local transcripts:** kept with the attempt until deleted; option "Delete transcripts when recordings expire".
- **D-10 recording retention:** 30 days default (ADR-015).
- **D-05 account optional; D-06 no composite score for free speaking; D-16 transcript sync is an R2 opt-in; D-19 legacy migration = profile + vocabulary only.**

### R-6 Product Owner aim on creator content (stated 2026-11-21) — supersedes earlier "remove creator content" wording
The Product Owner's aim: **the app's curriculum is built from the frameworks, techniques, tasks, exercises, activities and tips taught across the @askvinh YouTube channel.** Consequences (relayed to the running writers on 2026-11-21):
- **D-08 DECIDED by the Product Owner on 2026-11-21: Option B "original wording, link out"** — techniques, exercises and tips re-expressed in our own words and examples; each item links to its specific source video + timestamp; no creator name, likeness or branding in the app, content, notifications or store listing. (Option A, licensed partnership, was not chosen.) This is the first approved DECISION of Stage A (GOV-02 DC-3).
- **Content source:** the creator-derived material the Product Owner already added to the current app, checked and re-verified (CNT-01 `docs/product/TECHNIQUE_CATALOGUE_AUDIT.md`, in preparation).
- **Feedback after every submission (Product Owner, 2026-11-21):** mistakes found + coach tips after every submitted attempt, by function logic / local intelligence (cloud AI not required; D-04). New IDs: PRD-FR-047, PRD-FR-056, PRD-FR-057; SRS-FR-EVAL-015, SRS-FR-EVAL-016, SRS-FR-PRAC-008, SRS-FR-PRAC-009, SRS-FR-WORD-014; SRS-BR-028.
- **Unchanged in every option:** no YouTube scraping, caption download or channel ingestion (YouTube Terms of Service; ADR-011 stands for ingestion); content curated by hand into the bundled content pack; per-item provenance (specific video + timestamp, technique name as taught, verified-by/date) and `licenceStatus` (ORIGINAL | ORIGINAL_REEXPRESSION | LICENSED (reserved) | PENDING_REVIEW — SRS-BR-028); PENDING_REVIEW never ships; no verbatim transcripts or creator name/likeness unless licensed; outbound links to official videos allowed.
- **Product shape (RECOMMENDATION):** "Technique" is a first-class content type (technique → explanation → exercises/activities → tips) inside Practice (tracks + "Techniques" collection), practised through the existing templates and evaluated only with honest labelled metrics; 4-tab IA unchanged; the creator-branded tab is replaced by this content.
- **Existing creator-derived items** (15 techniques, named frameworks, 30-day plan, 10 challenges, shadowing scripts in `CurriculumCatalog.kt`) are re-verified against specific videos and migrated into the pack — not deleted. Per-user Firestore seeding (`extractedTechniques`, `youtubeSources`) is still retired.
- **Documents to revise after the writers finish:** RD-13, RD-14 (D-08), RD-16 (retired features, NG), BRD (BO-06, BR-008), PRD (content requirements; possible new PRD-FR for technique content), ADR-011 (scope narrowed to ingestion), SEC-01 (IP note), PLN-02/04 (content-cataloguing effort, legal risk), DES-02 (Techniques collection), ARC-05 §6 (possible labelled acoustic metrics for vocal-delivery techniques).
