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
