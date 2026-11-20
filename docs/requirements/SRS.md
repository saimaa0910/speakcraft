# Software Requirements Specification (SRS) — SpeakCraft

| Field | Value |
|---|---|
| Document ID | SRS |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval (gate G1) |
| Owner (role) | Business Analyst / Solution Architect |
| Approval | Pending — Product Owner |
| Related | PRD, NFR, USE_CASES, FEATURE_SPECIFICATIONS, ARC-01…ARC-05, SEC-01, TST-01, GOV-01 |

> **Authority.** The SRS is the authoritative source for testable software functional requirements (`SRS-FR-<AREA>-nnn`) and business rules (`SRS-BR-nnn`). Measurable quality requirements are in NFR (`NFR-<CAT>-nnn`). Each SRS requirement traces to a PRD requirement. "Shall" = mandatory for the stated release; numeric defaults marked † are **Proposed — requires approval**.

---

## 1. Introduction

### 1.1 Purpose
To specify the behaviour of the redesigned SpeakCraft Android application precisely enough to design, implement and test it.

### 1.2 Definitions

| Term | Definition |
|---|---|
| **Activity** | A content item (prompt + template + metadata) from the bundled content pack |
| **Template** | Interaction type: TPL-READ (read aloud), TPL-REPEAT (listen & repeat), TPL-ANSWER (answer a question), TPL-STRUCT (structured answer), TPL-STORY (tell a story); R2: TPL-ROLE (scripted role-play) |
| **Attempt** | One recording of the user responding to an activity or word stage, plus its evaluation |
| **Valid attempt** | An attempt that passed the evidence gate (SRS-BR-001) |
| **Inconclusive** | Evaluation outcome when evidence is insufficient; not a pass, not a fail |
| **Session** | Today's ordered set of steps (activities and word tasks) for a local date |
| **Step** | One unit of a session (a speaking activity, a word stage, or a review set) |
| **Metric** | A quantity computed from an attempt; labelled **Measured** or **Estimated** (ARC-05 §6) |
| **Tier** | Device speech capability: T1, T2, T3a, T3b (ADR-009) |
| **Local profile** | On-device user identity used before or without sign-in |
| **dateKey** | Local calendar date `YYYY-MM-DD` in the device time zone at the time of the event |
| **Event** | Immutable learning fact with a UUID (idempotency key) |

### 1.3 References
PRD; BRD; RD-16 (CAP-nn); ADR-001…ADR-015; ARC-03 (entities `ENT-nn`); ARC-05 (metrics, rules); SEC-01; DES-02/03 (screens `SCR-*`).

## 2. Scope

The SRS covers releases R0–R2 of the Android application: capabilities CAP-01…CAP-12. R3 items are excluded. Server-side components: none (Firebase Auth and Firestore are used as managed services).

## 3. System overview

```
┌──────────────────────── Android app (single :app module) ─────────────────────────┐
│ feature/* (Compose UI + ViewModels)                                                │
│   onboarding · today · session · practice · words · progress · profile/settings   │
│ speech/  capture (AudioRecord) → recognition (SpeechRecognizer tiers)             │
│          → metrics (DSP + transcript) → evaluation (gate, rules, feedback)         │
│ data/    Room (source of truth) · DataStore (prefs) · content pack (assets)        │
│          sync (WorkManager → Firestore) · auth (Credential Manager → Firebase Auth)│
└──────────────────────────────────────────────────────────────────────────────────┘
           │ HTTPS (only when signed in and online)
           ▼
   Firebase Auth · Cloud Firestore (named database, per-user documents)
```
Architecture detail: ARC-01.

## 4. User classes

| Class | Description | Access |
|---|---|---|
| UC-L Local user | Uses the app without an account | All core features; data on device only |
| UC-S Signed-in user | Signed in with Google | All features + backup/sync |
| UC-B Beta tester | Signed-in or local user in the beta programme | Same as above + diagnostics sharing |
| (none) Administrator | No administrator class in R1–R2 (BR-017) | — |

## 5. Operating environment

| Item | Requirement |
|---|---|
| OS | Android 7.0 (API 24) to Android 16 (API 36); feature tiers by API level (ADR-009) — subject to D-14 |
| Devices | Phones (primary); tablets and foldables (adaptive layout) |
| Hardware | Microphone required for speaking features; speaker/headphones for reference audio |
| Memory | Must run on devices with 2 GB RAM † (NFR-COMP-003) |
| Network | Optional; required only for sign-in, sync, offline-pack download and consented online transcription |
| Services | Google Play services for Google sign-in (optional path); a device speech-recognition service for T1–T3a |

## 6. Functional requirements

### 6.1 Account & sign-in (ACC) — CAP-01, FS-01

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-ACC-001 | On first launch the system shall create a **local profile** (UUID) without requiring sign-in | PRD-FR-001 |
| SRS-FR-ACC-002 | The system shall offer Google sign-in via Credential Manager from onboarding (optional step) and from Settings → Account | PRD-FR-002 |
| SRS-FR-ACC-003 | On successful first sign-in, the system shall re-key all local data from the local profile ID to the Firebase UID in one transaction and enqueue upload of all syncable records | PRD-FR-003 |
| SRS-FR-ACC-004 | If the signed-in account already has cloud data, the system shall merge per ADR-014 rules and shall not discard any completed attempt or passed word stage | PRD-FR-003, PRD-FR-102 |
| SRS-FR-ACC-005 | Sign-in cancellation shall return the user to the previous screen without error text; other sign-in failures shall show a user-safe message and a retry | PRD-FR-002 |
| SRS-FR-ACC-006 | On sign-out the system shall offer "Remove my practice data from this device" and "Keep it on this device"; the default follows decision D-21 (recommended: Remove when all data is synced; if unsynced data exists, warn and default to Keep); credential state is cleared in both cases | PRD-FR-004; SEC-REQ-04 |
| SRS-FR-ACC-007 | Account deletion shall: (1) explain what will be deleted; (2) require typed or explicit confirmation; (3) require recent re-authentication; (4) delete all Firestore documents under `users/{uid}`; (5) delete the Firebase Auth user; (6) delete all local data and recordings for the profile; (7) show a completion confirmation | PRD-FR-005 |
| SRS-FR-ACC-008 | Account deletion shall be **resumable**: if interrupted, a persisted deletion state shall cause the system to resume deletion at next launch before any other screen | PRD-FR-005 |
| SRS-FR-ACC-009 | A local user shall be able to delete all local data ("Delete everything on this device") | PRD-FR-094 |

### 6.2 Onboarding & personalisation (ONB) — CAP-02, FS-02

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-ONB-001 | Onboarding shall consist of: value screen → goal/track → level → daily minutes → reminder → privacy & microphone → speech check → first attempt → first feedback; each choice step shall have a pre-selected default and a Continue action | PRD-FR-010 |
| SRS-FR-ONB-002 | Goal options shall map to tracks: Everyday conversation, Workplace communication, Job interviews, Presentations & storytelling, Clear speech | PRD-FR-010 |
| SRS-FR-ONB-003 | Level options shall be Beginner, Elementary, Intermediate, Upper-intermediate, Advanced, each with a one-line "can do" description; default Intermediate | PRD-FR-010; D-17 |
| SRS-FR-ONB-004 | Daily minutes options shall be 5, 10 (default) and 15 | PRD-FR-010 |
| SRS-FR-ONB-005 | The reminder step shall offer "Remind me at [time]" (default 19:00 †) or "Not now"; choosing a reminder on Android 13+ shall trigger the notification permission request | PRD-FR-080, PRD-FR-083 |
| SRS-FR-ONB-006 | The privacy step shall state in plain language that recordings and transcripts stay on the device, then request RECORD_AUDIO | PRD-FR-011 |
| SRS-FR-ONB-007 | If microphone permission is denied, the system shall explain the impact, offer "Try again" (or "Open settings" when permanently denied), and allow continuing to Today with speaking steps disabled | PRD-FR-011 |
| SRS-FR-ONB-008 | The speech check shall determine the tier (SRS-FR-SPCH-001) and present the tier-specific choice (offline pack download, online-transcription consent, or audio-only explanation) | PRD-FR-012 |
| SRS-FR-ONB-009 | The first attempt shall use activity `onb-intro` (TPL-ANSWER, "Introduce yourself", 60 s max) and shall be evaluated and stored like any attempt; its metrics are the user's baseline | PRD-FR-013 |
| SRS-FR-ONB-010 | Onboarding progress shall survive process death; on relaunch the user resumes at the last incomplete step | PRD-NFR-005 |
| SRS-FR-ONB-011 | The first-attempt step shall be skippable ("Do this later"); skipping shall not block reaching Today | PRD-FR-013 |

### 6.3 Today — daily session (TODAY) — CAP-03, FS-03

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-TODAY-001 | Today shall display: greeting, today's session card (steps, estimated minutes, one primary action), streak summary and today's progress | PRD-FR-020, PRD-FR-024 |
| SRS-FR-TODAY-002 | The plan engine shall generate a session for the current dateKey when Today is first shown that day, using inputs: track, level, daily minutes, measured weakest metric family, due reviews, new-word goal, last 14 days of attempts, content version | PRD-FR-021 |
| SRS-FR-TODAY-003 | Plan generation shall be deterministic: identical inputs and dateKey shall yield an identical plan | PRD-FR-021 |
| SRS-FR-TODAY-004 | A 10-minute session shall contain: 1 warm-up (TPL-READ or TPL-REPEAT), 1 main speaking task (track-matched TPL-ANSWER/STRUCT/STORY), 1 word task (new word stage or review set), optionally 1 retry/focus task; 5- and 15-minute sessions scale per FS-03 table | PRD-FR-020 |
| SRS-FR-TODAY-005 | The engine shall not select an activity completed in the previous 7 days if an unpractised alternative of the same template and track exists | PRD-FR-021 |
| SRS-FR-TODAY-006 | Plans shall be persisted; completed steps shall never be removed or reordered (SRS-BR-013) | PRD-FR-022 |
| SRS-FR-TODAY-007 | Changing duration or preferences mid-day shall regenerate only incomplete steps | PRD-FR-022, PRD-FR-026 |
| SRS-FR-TODAY-008 | Recovery mode (SRS-BR-014) shall apply when the last valid attempt was ≥ 2 local days ago | PRD-FR-023 |
| SRS-FR-TODAY-009 | "Swap" on a step shall replace it with the next-best candidate of the same template family; at most 3 swaps per day † | PRD-FR-025 |
| SRS-FR-TODAY-010 | Greeting: first ever visit "Welcome, {name}"; subsequent days "Good {morning/afternoon/evening}, {name}" plus one factual line (e.g. streak, last focus); never "welcome back" on the first visit | PRD-FR-024 |
| SRS-FR-TODAY-011 | When all steps are complete Today shall show a completion state with what was practised and the next suggested optional activity | PRD-FR-020 |

### 6.4 Speaking practice session (SESS) — CAP-04, FS-04

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-SESS-001 | The session player shall present states: Prompt → (Prepare) → Ready → Recording → Processing → Result, plus Inconclusive, Error and Interrupted | PRD-FR-030 |
| SRS-FR-SESS-002 | Preparation time per template: TPL-ANSWER 15 s, TPL-STRUCT 30 s, TPL-STORY 30 s, others 0 †; users can skip preparation | PRD-FR-030 |
| SRS-FR-SESS-003 | Maximum recording length per template: TPL-READ/REPEAT 30 s, TPL-ANSWER 90 s, TPL-STRUCT 120 s, TPL-STORY 120 s †; recording stops automatically at the limit with a 10 s warning | PRD-FR-030 |
| SRS-FR-SESS-004 | While recording the UI shall show elapsed time, remaining time, an input-level indicator and a single Stop control; TalkBack shall announce recording start/stop | PRD-FR-030 |
| SRS-FR-SESS-005 | The system shall capture audio per the device tier (SRS-FR-SPCH-002) | PRD-FR-031 |
| SRS-FR-SESS-006 | After Stop, processing shall run automatically; the result shall show the transcript (if any) with a tier label ("On-device", "Online (consented)", or "No transcript — audio-only") | PRD-FR-031 |
| SRS-FR-SESS-007 | If the evidence gate fails, the result shall be Inconclusive with the specific reason (too short, too quiet, no speech detected, transcript unavailable for a transcript-required template) and actions Retry / Skip | PRD-FR-032 |
| SRS-FR-SESS-008 | "Try again" shall start a new attempt on the same activity; the result shall show a side-by-side comparison of the 3 headline metrics with the previous valid attempt | PRD-FR-033 |
| SRS-FR-SESS-009 | Where a recording is retained (T1, T3b, and retention ≠ "delete after feedback"), the result shall offer playback | PRD-FR-034 |
| SRS-FR-SESS-010 | On phone call, audio-focus loss, app backgrounding, permission revocation or storage-full during recording, the system shall stop capture, discard partial audio, record no attempt, and show the Interrupted state with Retry | PRD-FR-035 |
| SRS-FR-SESS-011 | Leaving the player mid-attempt shall ask for confirmation and discard the in-progress attempt | PRD-FR-035 |
| SRS-FR-SESS-012 | TPL-READ and TPL-REPEAT shall offer reference audio via TextToSpeech at 1.0× and 0.75× using the selected English locale | PRD-FR-036 |
| SRS-FR-SESS-013 | Player state (current step, prepared prompt) shall survive configuration changes; in-progress recording shall survive rotation | PRD-NFR-005 |
| SRS-FR-SESS-014 | Only one recording may be active app-wide; navigation away from the player stops it | PRD-FR-035 |

### 6.5 Evaluation & feedback (EVAL) — CAP-05, FS-05

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-EVAL-001 | Every attempt shall pass through the evidence gate (SRS-BR-001) before any metric is shown | PRD-FR-032 |
| SRS-FR-EVAL-002 | The system shall compute the metrics defined for the template in ARC-05 §6; a metric whose inputs are unavailable shall be `null` and displayed "Not available — {reason}" | PRD-FR-040 |
| SRS-FR-EVAL-003 | Each displayed metric shall carry its label Measured or Estimated per ARC-05 §6 (SRS-BR-003) | PRD-FR-040 |
| SRS-FR-EVAL-004 | The result shall show the 3 headline metrics for the template (ARC-05 §7), each with value, unit, plain-language band and target range | PRD-FR-040 |
| SRS-FR-EVAL-005 | The feedback composer shall select exactly one **strength** and one **focus** from deterministic rules ranked by priority (ARC-05 §8); where possible the focus quotes a span from the transcript | PRD-FR-041 |
| SRS-FR-EVAL-006 | Language observations shall come only from rules whose measured precision on the evaluation corpus is ≥ 0.90 † (NFR-QUAL-003); at most 2 per attempt; phrased "Possible issue" with a suggestion | PRD-FR-042 |
| SRS-FR-EVAL-007 | Free-speaking templates (ANSWER, STRUCT, STORY) shall not display an overall score (SRS-BR-006) | PRD-FR-043 |
| SRS-FR-EVAL-008 | TPL-READ and TPL-REPEAT shall display "Recognised as intended: N %" with pass/fail against the threshold (SRS-BR-007) and the list of words not recognised as intended | PRD-FR-043 |
| SRS-FR-EVAL-009 | The result shall offer "Was this accurate?" (Yes / No + optional reason: transcript wrong, metric wrong, tip not useful); stored with the attempt | PRD-FR-044 |
| SRS-FR-EVAL-010 | Each metric shall provide "How we measured this" text including limitations (e.g. filler detection depends on the recogniser keeping "um") | PRD-FR-045 |
| SRS-FR-EVAL-011 | Each attempt shall store `evaluationVersion` (rule-set semantic version) and `contentVersion` | PRD-FR-046 |
| SRS-FR-EVAL-012 | Recognition confidence shall be shown only if supplied by the recogniser; otherwise not shown (SRS-BR-005) | PRD-NFR-001 |
| SRS-FR-EVAL-013 | Evaluation shall complete within the NFR-PERF-005 budget; if it exceeds 2× the budget, the system shall show the Error state with Retry evaluation (audio retained until retry or exit) | PRD-NFR-004 |
| SRS-FR-EVAL-014 | Structured-answer evaluation shall report which framework elements were detected (e.g. STAR: Situation, Task, Action, Result) as Estimated, with the cue phrases that triggered detection | PRD-FR-040 |

### 6.6 Practice library (PRAC) — CAP-06, FS-06

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-PRAC-001 | The library shall list the 5 tracks with activity counts; selecting a track lists its activities filterable by template and level | PRD-FR-050 |
| SRS-FR-PRAC-002 | Each activity card shall show title, template, estimated minutes, level and "Practised" status | PRD-FR-050 |
| SRS-FR-PRAC-003 | Activity detail shall show goal, prompt preview, template explanation and Start | PRD-FR-050 |
| SRS-FR-PRAC-004 | Attempts started from the library shall be recorded identically to Today attempts and count toward progress and streak | PRD-FR-052 |
| SRS-FR-PRAC-005 | Content shall be loaded from a bundled, versioned content pack (JSON in assets) validated at build time (schema, unique IDs, required fields) | PRD-FR-053 |
| SRS-FR-PRAC-006 | Users shall be able to save/unsave an activity; saved activities appear in a "Saved" filter | PRD-FR-054 |
| SRS-FR-PRAC-007 (R2) | TPL-ROLE shall present a scripted multi-turn scenario: each partner line is shown and spoken via TTS; each user turn is recorded and evaluated as TPL-ANSWER with turn-specific expected elements | PRD-FR-055 |

### 6.7 Words (WORD) — CAP-07, FS-07

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-WORD-001 | Each word shall have status New → Learning (stage 1–4) → Learned → Reviewing → Mastered (SRS-BR-017) | PRD-FR-060 |
| SRS-FR-WORD-002 | Stage 1 **Learn** shall show word, IPA (if available in content), part of speech, simple meaning, 2 examples, common collocations, and play TTS pronunciation; marking "I've read this" completes the stage | PRD-FR-060 |
| SRS-FR-WORD-003 | Stage 2 **Say it** shall record the word (max 5 s) and pass if the recogniser output contains the word or an accepted inflection with match score ≥ threshold (default 75 †) | PRD-FR-062 |
| SRS-FR-WORD-004 | Stage 3 **Use it** shall present 2 sentences containing the word; the user reads each; pass if recognised-as-intended ≥ threshold (default 75 †) for both | PRD-FR-062 |
| SRS-FR-WORD-005 | Stage 4 **Make your own** shall ask for an original spoken sentence; pass requires: target word (or inflection) present, ≥ 5 words, ≤ 1 high-precision grammar observation, and composite ≥ 70 † (ARC-05 §9) | PRD-FR-062 |
| SRS-FR-WORD-006 | Stage checks without usable recognition output shall be Inconclusive (SRS-BR-008), not failed | PRD-FR-062 |
| SRS-FR-WORD-007 | After 3 failed attempts on a stage, the system shall offer an easier variant (shorter sentence, slower reference) and allow continuing later | PRD-FR-062 |
| SRS-FR-WORD-008 | Passing stage 4 shall mark the word Learned and schedule the first review per SRS-BR-016 | PRD-FR-063 |
| SRS-FR-WORD-009 | A review set shall contain due words (max 10 †) mixing spoken recall ("Say the word that means …") and sentence use; outcomes update the schedule | PRD-FR-063 |
| SRS-FR-WORD-010 | The word list shall support filters (New, Learning, Learned, Due, Mastered), text search, and level/track filters | PRD-FR-064 |
| SRS-FR-WORD-011 | The word bank shall be bundled in the content pack with ≥ 300 entries for R1, each with level, track tags, meaning, examples and licensing provenance | PRD-FR-065 |
| SRS-FR-WORD-012 (R2) | Notebook: save a word from a transcript or feedback, add notes and tags, edit, delete, favourite, search | PRD-FR-066 |
| SRS-FR-WORD-013 (R2) | Quiz: generate deterministic question sets (meaning, fill-in-the-blank, sentence choice, listening recognition) from the bank using a seeded generator; labelled "Quiz" | PRD-FR-067 |

### 6.8 Progress & streak (PROG) — CAP-08, FS-08

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-PROG-001 | Progress overview shall show: current and longest streak, weekly calendar (7 days), practice time this week, sessions completed, words learned, and metric trend cards | PRD-FR-070, PRD-FR-072 |
| SRS-FR-PROG-002 | Trend cards shall be per template family (Read/Repeat; Answer/Structured/Story) and metric; a trend shall display only with ≥ 3 valid attempts (SRS-BR-024) and show the noise band | PRD-FR-070 |
| SRS-FR-PROG-003 | History shall list attempts newest first with date, activity, template, headline metrics, status (Valid/Inconclusive) and playback availability | PRD-FR-071 |
| SRS-FR-PROG-004 | Attempt detail shall show transcript, all metrics with labels, feedback, accuracy rating and playback (if retained) | PRD-FR-071 |
| SRS-FR-PROG-005 | Streak and statistics shall be derived from attempts and events in the local database (SRS-BR-010…012, -018); no counter shall be incremented by navigation or taps | PRD-FR-073 |
| SRS-FR-PROG-006 | Milestones (3, 7, 14, 30, 60, 100 days) shall trigger a one-time celebration (reduced-motion aware) and, in R2, an inbox message | PRD-FR-072 |
| SRS-FR-PROG-007 | Word progress summary shall show counts by status and words due today | PRD-FR-074 |
| SRS-FR-PROG-008 (R2) | Check-in: every 14–28 days † the plan offers the user's check-in activity (same prompt as baseline per track); results are plotted against prior check-ins | PRD-FR-075 |

### 6.9 Reminders & inbox (REM) — CAP-09, FS-09

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-REM-001 | When reminders are enabled, the system shall schedule one daily reminder near the chosen local time (ADR-005) | PRD-FR-080 |
| SRS-FR-REM-002 | The reminder shall not be posted if a valid attempt exists for today's dateKey (SRS-BR-019) | PRD-FR-082 |
| SRS-FR-REM-003 | Reminder text shall mention due word reviews when present; tapping it opens Today | PRD-FR-081 |
| SRS-FR-REM-004 | Reminders shall use a dedicated notification channel "Practice reminders"; the system shall not post any other notification category in R1 | PRD-FR-080 |
| SRS-FR-REM-005 | If POST_NOTIFICATIONS is denied, Settings → Reminders shall show the state and a link to system settings; no repeated prompts | PRD-FR-083 |
| SRS-FR-REM-006 | Reminders shall be rescheduled after reboot, time-zone change and preference change | PRD-FR-080 |
| SRS-FR-REM-007 (R2) | Inbox shall list learning messages (milestones, weekly summary) with read/unread, mark all read, delete; unread badge on Today's top bar; no software-update messages | PRD-FR-084 |

### 6.10 Profile, settings, privacy & data rights (SET) — CAP-10, FS-10

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-SET-001 | Profile shall show and allow editing of display name, level, track/goal, daily minutes; and show member-since date and key statistics | PRD-FR-090 |
| SRS-FR-SET-002 | Settings shall contain sections: Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account, About | PRD-FR-091 |
| SRS-FR-SET-003 | Speech & privacy shall show: tier and status, recognition language, offline pack status/install (where supported), online-transcription consent toggle (T3 devices), recording retention selector, link to privacy information | PRD-FR-092 |
| SRS-FR-SET-004 | Export shall produce a JSON file (schema in ARC-03 §10) containing profile, preferences, attempts (metrics, transcripts if present), word progress, plans and events, shared via the system share sheet; recordings optionally included as a ZIP | PRD-FR-093 |
| SRS-FR-SET-005 | Clear history shall offer: recordings only; attempts and transcripts; word progress; everything; each with confirmation stating consequences; signed-in deletions propagate to Firestore | PRD-FR-094 |
| SRS-FR-SET-006 | Privacy information shall describe exactly what is stored locally, what syncs (if signed in), what never leaves the device, and the current tier's behaviour; content shall be generated from the same configuration constants that drive behaviour | PRD-FR-095 |
| SRS-FR-SET-007 | Appearance shall offer Light / Dark / System (default System) and Reduce motion (default follows system); text size follows system font scale | PRD-FR-096 |
| SRS-FR-SET-008 | About shall show version name/code, open-source licences, privacy policy, terms, help/contact, and "Share diagnostics" (PII-free log, ADR-007) | PRD-FR-097 |

### 6.11 Data storage & sync (DATA) — CAP-11, FS-11

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-DATA-001 | All learning data shall be written to the local database first, in a transaction; UI shall never wait for network writes | PRD-FR-100 |
| SRS-FR-DATA-002 | Every fact (attempt, stage pass, review result, session completion) shall be an immutable record with a client-generated UUID; re-processing the same UUID shall have no effect (SRS-BR-020) | PRD-FR-102 |
| SRS-FR-DATA-003 | For signed-in users, each syncable change shall create an outbox entry; a background job shall upload outbox entries when online, with exponential back-off | PRD-FR-101 |
| SRS-FR-DATA-004 | On sign-in and on app start (when online), the system shall download documents changed since the last pull watermark and merge per ADR-014 | PRD-FR-102 |
| SRS-FR-DATA-005 | Sync status (Up to date / Syncing / Waiting for connection / Error with retry) shall be visible in Settings → Account | PRD-FR-101 |
| SRS-FR-DATA-006 | Audio, file paths and (by default) transcripts shall never be included in uploaded documents | PRD-NFR-002 |
| SRS-FR-DATA-007 | On first launch of the redesigned version, for a signed-in legacy user the system shall import profile (name, level, goal, daily minutes) and vocabulary progress from legacy collections, mark migration complete, and leave legacy documents untouched (D-19) | PRD-FR-103 |
| SRS-FR-DATA-008 | Legacy migration failures shall not block use; the system shall retry on next launch and log a PII-free diagnostic | PRD-FR-103 |
| SRS-FR-DATA-009 | Retained recordings shall be stored under `noBackupFilesDir/recordings/<profileId>/` and deleted per retention (SRS-BR-022) by a daily cleanup job | ADR-015 |

### 6.12 Speech capability (SPCH) — CAP-12, FS-12

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-SPCH-001 | The capability service shall determine the tier: T1 (API ≥ 33, on-device recogniser supports locale, audio-source input verified), T2 (API ≥ 31 on-device recogniser available), T3a (no on-device recogniser, online consent given, a recogniser exists), T3b (otherwise) | PRD-FR-110 |
| SRS-FR-SPCH-002 | Capture: T1 — app-owned AudioRecord 16 kHz mono PCM16 written to WAV and piped to the recogniser; T2/T3a — recogniser owns the microphone with segment restart; T3b — AudioRecord only | PRD-FR-110 |
| SRS-FR-SPCH-003 | Status shall be re-evaluated on app start, locale change, consent change and after an offline-pack installation; status text shall reflect verified state only | PRD-FR-110 |
| SRS-FR-SPCH-004 | Where `checkRecognitionSupport`/`triggerModelDownload` are available, Settings and onboarding shall show offline-pack status and an Install action with progress/failure states | PRD-FR-111 |
| SRS-FR-SPCH-005 | The online-transcription consent screen shall state: audio is sent to the device's speech service provider (named where determinable) for transcription only; SpeakCraft does not store it remotely; consent can be withdrawn in Settings | PRD-FR-112 |
| SRS-FR-SPCH-006 | Withdrawing consent shall take effect before the next attempt and change the tier to T3b | PRD-FR-112 |
| SRS-FR-SPCH-007 | In T3b, transcript-dependent templates/steps shall be adapted: TPL-READ/REPEAT and word stages show delivery metrics only and are marked "Transcript needed for this check"; the plan engine prefers delivery-oriented activities | PRD-FR-113 |
| SRS-FR-SPCH-008 | Recognition locale options: en-US, en-GB, en-IN, en-AU, filtered by recogniser support; default from device locale if English, else en-US | PRD-FR-114 |
| SRS-FR-SPCH-009 | Continuous recognition shall capture the full attempt: segmented session (T1) or automatic restart on end-of-segment with stitching (T2/T3a); duplicate text on stop shall be prevented | PRD-FR-031 |
| SRS-FR-SPCH-010 | Recogniser errors shall be classified (no match, network, busy, audio, permission, unavailable); "no match" shall lead to Inconclusive, not to a capability failure state | PRD-FR-110 |

### 6.13 Platform (PLAT) — transition

| ID | Requirement | Trace |
|---|---|---|
| SRS-FR-PLAT-001 | The build shall not include: Firebase AI Logic, MediaPipe, TensorFlow Lite, CameraX, the YouTube client, the self-updater, the admin console, the AI Mentor, the conversation simulator, the tutorial overlay system, or creator-branded content (subject to ADR approvals) | PRD-FR-120 |
| SRS-FR-PLAT-002 | The manifest shall declare only permissions used: INTERNET, RECORD_AUDIO, POST_NOTIFICATIONS (Android 13+); plus `<queries>` for `android.speech.RecognitionService` | PRD-FR-120 |
| SRS-FR-PLAT-003 | Distributed builds shall be release builds, R8-minified, release-signed; debug-only code (App Check debug provider) shall be in the debug source set | PRD-FR-121 |

## 7. Non-functional requirements

Measurable NFRs are specified in `docs/requirements/NFR.md` (NFR-PERF, NFR-REL, NFR-SEC, NFR-PRIV, NFR-A11Y, NFR-COMP, NFR-MAINT, NFR-QUAL). They are binding on this SRS.

## 8. Interface requirements

| Interface | Requirement |
|---|---|
| User interface | Per DES-02 (IA), DES-03 (screens), DES-05 (design system), DES-06 (specs), DES-07 (accessibility) |
| Microphone | `AudioRecord` (16 kHz mono PCM16) in T1/T3b; via `SpeechRecognizer` in T2/T3a |
| Speech recognition | `android.speech.SpeechRecognizer` (`createOnDeviceSpeechRecognizer` API 31+; `EXTRA_AUDIO_SOURCE`, `EXTRA_SEGMENTED_SESSION`, `checkRecognitionSupport`, `triggerModelDownload` API 33+) |
| Text-to-speech | `android.speech.tts.TextToSpeech`, locale-matched to recognition locale |
| Authentication | Credential Manager (Google ID) → Firebase Auth |
| Cloud data | Cloud Firestore (existing named database), per-user documents only (ARC-04) |
| Background work | WorkManager (ADR-005) |
| Notifications | `NotificationCompat`, channel "Practice reminders" |
| Sharing | Android share sheet for export (via FileProvider limited to an `exports/` directory) |

## 9. Data requirements

| Data | Store | Retention | Sync | Ref |
|---|---|---|---|---|
| Profile & learning preferences | Room + DataStore (UI prefs) | Until deleted | Yes (signed in) | ENT-01 |
| Attempts (metrics, feedback, rating) | Room | Until deleted | Metrics yes; transcript no (default) | ENT-03 |
| Transcripts | Room | Until deleted | No (R2 opt-in, D-16) | ENT-03 |
| Recordings | `noBackupFilesDir` | Per retention (default 30 days †) | Never | ADR-015 |
| Session plans | Room | 90 days † | Yes | ENT-05 |
| Word progress & reviews | Room | Until deleted | Yes | ENT-07/08 |
| Events | Room | Until deleted | Yes | ENT-09 |
| Content pack | Assets (read-only) | App version | N/A | ARC-03 §6 |
| Diagnostics log | App-private file, rolling 1 MB † | Rolling | Only via user share | ADR-007 |

Full model: ARC-03.

## 10. Security requirements

SEC-REQ-01… in SEC-01 §12 are binding. Key points: per-user Firestore isolation; immutable fact documents; no admin roles; release-signed builds; no PII in logs; FileProvider limited to exports; backup excludes recordings and databases' sensitive content per SEC-01 §6.

## 11. Privacy requirements

| ID | Requirement |
|---|---|
| SRS-PRIV-001 | No audio or transcript shall be transmitted off-device unless tier is T3a with consent (and then only to the speech service for transcription) |
| SRS-PRIV-002 | Privacy information text shall be generated from the same policy constants that govern behaviour, and covered by tests |
| SRS-PRIV-003 | Export and deletion shall cover 100 % of personal data categories in §9 |
| SRS-PRIV-004 | No analytics SDK in R1 (ADR-013); any future analytics requires opt-in |
| SRS-PRIV-005 | Recordings shall be excluded from Android backup and device-to-device transfer |

## 12. Performance requirements

NFR-PERF-001…010 (startup, navigation, recording start latency, evaluation latency, plan generation, database queries, memory, battery, APK size).

## 13. Availability requirements

| ID | Requirement |
|---|---|
| SRS-AVL-001 | All core features shall be available offline (no service dependency) |
| SRS-AVL-002 | Firebase unavailability shall degrade only sign-in and sync, with visible status |
| SRS-AVL-003 | Speech-service unavailability shall degrade to T3b behaviour automatically with explanation |

## 14. Compatibility requirements

NFR-COMP-001…008 (API range, tier matrix, RAM, storage, screen sizes, recogniser variants, locales, 16 KB page size).

## 15. Error handling

| ID | Condition | Required behaviour |
|---|---|---|
| SRS-ERR-001 | Microphone permission denied | Explain impact; Try again / Open settings; non-speaking parts remain usable |
| SRS-ERR-002 | Recogniser unavailable / busy | Fall back to tier T3b for the attempt; tell the user transcript was not available; do not mark capability failed for "no match" |
| SRS-ERR-003 | Evidence insufficient | Inconclusive with reason and Retry (SRS-FR-SESS-007) |
| SRS-ERR-004 | Storage full | Stop recording, discard partial file, show message with link to storage settings |
| SRS-ERR-005 | Database write failure | Show non-blocking error; retry once; log PII-free diagnostic; never show success for a failed write |
| SRS-ERR-006 | Sync failure | Keep outbox; exponential back-off; status shows "Error — Retry"; never block UI |
| SRS-ERR-007 | Sign-in failure | User-safe message mapped from error class; Retry |
| SRS-ERR-008 | Content pack invalid (build-time) | Build fails (CI) — never shipped |
| SRS-ERR-009 | Account deletion partially complete | Resume at next launch (SRS-FR-ACC-008) |
| SRS-ERR-010 | TTS unavailable | Hide reference-audio controls; show "Reference audio isn't available on this device" |

All user-facing error messages shall be plain language with no exception text, error codes or internal class names.

## 16. Business rules

| ID | Rule |
|---|---|
| SRS-BR-001 | **Evidence gate.** An attempt is valid only if: recording duration ≥ 1.5 s; voiced speech ≥ 0.5 s and ≥ 25 % † of duration (where audio is available); input level above the silence threshold; for transcript-required templates, ≥ 1 recognised word (READ/REPEAT/word stages) or ≥ 8 recognised words † (ANSWER/STRUCT/STORY); transcript belongs to this attempt (session-scoped). Otherwise Inconclusive |
| SRS-BR-002 | A metric without its required inputs is null and shown as "Not available"; no default, baseline or fallback values may be substituted |
| SRS-BR-003 | Each metric has a fixed label: **Measured** (duration, speaking time, speech ratio, pace, long pauses, word count, target word used, recognised-as-intended %) or **Estimated** (filler rate, lexical variety, grammar observations, structure elements, hedging) |
| SRS-BR-004 | Recognition agreement shall be named "Recognised as intended" and shall never be called pronunciation or accent accuracy |
| SRS-BR-005 | Confidence values are displayed or used only when supplied by the recogniser; no constant confidence may be used |
| SRS-BR-006 | No overall score for TPL-ANSWER, TPL-STRUCT, TPL-STORY (D-06) |
| SRS-BR-007 | Pass thresholds (configurable, versioned): READ/REPEAT recognised-as-intended ≥ 75 % †; word stage 2 ≥ 75, stage 3 ≥ 75, stage 4 ≥ 70 † |
| SRS-BR-008 | Inconclusive attempts do not count as completion, failure, streak days or practice time |
| SRS-BR-009 | A session step is complete when it has a valid attempt (speaking or word-stage steps) or when its review set is finished |
| SRS-BR-010 | **Streak day** = a dateKey with ≥ 1 valid speaking attempt from any source (Today, Practice, Words stages 2–4, reviews with spoken recall) |
| SRS-BR-011 | **Current streak** = number of consecutive streak days ending today or yesterday; otherwise 0. **Longest streak** = maximum ever. History is never deleted by a missed day. Days use the dateKey recorded at attempt time (time-zone changes do not rewrite history) |
| SRS-BR-012 | Session completion is not required for a streak day |
| SRS-BR-013 | A plan is generated once per dateKey; regeneration changes only incomplete steps; completed steps are never removed or reordered |
| SRS-BR-014 | **Recovery**: if the last valid attempt is ≥ 2 local days old, today's session is min(chosen, 10) minutes, prioritises due reviews and a familiar template, and adds no backlog |
| SRS-BR-015 | Word stages must be passed in order; replaying a passed stage never reverts status |
| SRS-BR-016 | **Spaced repetition** (SM-2 variant): grade 4 = recall pass first try, 3 = pass after hint/retry, 1 = fail; first intervals 1, 3, 7 days, then `interval × EF` (EF start 2.5, min 1.3, updated by SM-2 formula); grade < 3 resets interval to 1; due dates computed on local dateKey |
| SRS-BR-017 | Word status: **Learned** after stage 4 passed; **Mastered** when review interval ≥ 21 days †; Mastered words return to Reviewing on a failed review |
| SRS-BR-018 | **Practice time** = sum of recording durations of valid attempts (plus 0 for non-speaking actions); shown as minutes speaking |
| SRS-BR-019 | Reminders: max 1 per day by default; not sent if a valid attempt exists today; not sent between 22:00 and 07:00 † unless the user chose a time in that window |
| SRS-BR-020 | Events and attempts are idempotent by UUID; duplicates are ignored on write and on sync |
| SRS-BR-021 | Conflict resolution per ADR-014 (facts append-only; mutable docs last-writer-wins; word state merges by most advanced stage and later schedule) |
| SRS-BR-022 | Recording retention default 30 days † (D-10); options: delete after feedback, 7 days, 30 days, keep until deleted; discarded/interrupted/inconclusive recordings are deleted immediately |
| SRS-BR-023 | Online transcription only when tier T3a and consent = true; revocation immediate |
| SRS-BR-024 | Trend lines require ≥ 3 valid attempts in the template family; changes smaller than the metric's noise band are reported as "no clear change" |
| SRS-BR-025 | Changing level or track never resets progress or history |
| SRS-BR-026 | Attempts reference `activityId` + `contentVersion`; retired content remains readable in history |
| SRS-BR-027 | New-word goal default: 2 per day † (range 0–5); review set max 10 words per session step |

## 17. Constraints

BC-01…BC-06 (BRD §6); ADR-001 stack; no new dependency without approved ADR; English UI strings in resources.

## 18. External dependencies

| Dependency | Use | Failure impact |
|---|---|---|
| Device speech recognition service (often Google) | Transcription T1–T3a | Degrade to T3b |
| Device TTS engine | Reference audio | Hide reference audio |
| Google Play services / Credential Manager | Sign-in | Local-only use continues |
| Firebase Auth + Firestore | Account, sync | Local-only use continues |
| WorkManager (ADR-005) | Reminders, sync, cleanup | Reminders/sync paused |

## 19. Acceptance criteria

1. Every SRS-FR for the release has ≥ 1 passing test case traced in GOV-01 (unit, Robolectric, instrumented or documented manual test).
2. Every SRS-BR has unit tests covering boundary values.
3. NFR targets approved for the release are met on the reference devices (TST-01 §5).
4. No open Critical/High defects (TST-01 severity scale).
5. Product Owner UAT sign-off (PLN-05 G6).
