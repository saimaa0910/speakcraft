# Work Breakdown Structure (WBS)

| Field | Value |
|---|---|
| Document ID | PLN-02 |
| Version | 0.2 — Draft for review (2026-11-21: T-04.1.1 narrowed; T-07.8, T-09.6…T-09.8, T-10.2 added for the Product Owner direction — totals in §2 not yet re-summed, see note in §2) |
| Date | 2026-11-21 |
| Status | Draft — **estimates preliminary**, awaiting engineering review and Product Owner approval (G4) |
| Owner (role) | Project Manager / Android Technical Lead |
| Approval | Pending — Product Owner |
| Related | PLN-01 Roadmap, PLN-03 Sprint Plan, PLN-04 RAID, PRD, SRS, FS-00…12, ENG-01 §12, TST-01, GOV-01 |

> **Estimates are preliminary** ideal engineering days (d) for one developer working with AI assistance, before any code spike. They include implementation, unit/UI tests and documentation updates for the item. Size bands: **S** ≤ 2 d · **M** 3–5 d · **L** 6–10 d · **XL** > 10 d (must be split before scheduling). Re-estimate at G4 and after SPK-01.

## 1. Structure and ID scheme

`IN-n` initiative → `EP-nn` epic → `F-nn.n` feature → `T-nn.n.n` task. Task types: **[ENG]** engineering, **[TST]** test, **[DOC]** documentation, **[CNT]** content, **[DES]** design asset, **[OPS]** DevOps. Each feature lists the requirements it delivers; story mapping (US-nnn) is maintained in GOV-01.

## 2. Initiatives and epics

| Initiative | Epic | Release | Priority | Estimate | Depends on |
|---|---|---|---|---|---|
| **IN-1 Foundation** | EP-01 Build, CI/CD & release hardening | R0 | P0 | 8–11 d | G4 |
| | EP-02 Architecture restructure & app shell | R0 | P0 | 9–13 d | G4 |
| | EP-03 Data foundation (Room, repositories, events, outbox) | R0 | P0 | 8–12 d | EP-02, ADR-002 |
| | EP-04 Safety, privacy fixes & feature removals | R0 | P0 | 7–10 d | G4 |
| | EP-05 Speech & rules spikes (SPK-01, SPK-02) | R0 | P0 | 6–9 d | DEP-07 |
| **IN-2 Core loop (MVP)** | EP-06 Speech pipeline | R1 | P0 | 10–15 d | EP-05 |
| | EP-07 Evaluation & feedback | R1 | P0 | 10–14 d | EP-03, EP-06 |
| | EP-08 Session player & Today | R1 | P0 | 10–14 d | EP-07, EP-17, EP-09 |
| | EP-09 Content pack & content production | R1 | P0 | 12–18 d | D-08, DEP-02 |
| | EP-10 Practice library | R1 | P0 | 4–6 d | EP-08, EP-09 |
| | EP-11 Words lifecycle | R1 | P0 | 10–13 d | EP-07, EP-09 |
| | EP-12 Progress & streak | R1 | P0 | 6–9 d | EP-03, EP-08 |
| | EP-13 Onboarding & account | R1 | P0 | 7–10 d | EP-06, EP-08 |
| | EP-14 Settings, privacy & data rights | R1 | P0 | 7–10 d | EP-03, EP-13 |
| | EP-15 Reminders | R1 | P1 | 3–5 d | ADR-005 |
| | EP-16 Sync & legacy migration | R1 | P1 | 7–10 d | EP-03, EP-13 |
| | EP-17 Design system & accessibility foundation | R1 | P0 | 6–9 d | DES-05, DEP-08 |
| | EP-18 Quality, beta & release readiness | R1 | P0 | 9–13 d | all R1 epics |
| **IN-3 Depth** | EP-19 Scripted role-play | R2 | P1 | 6–9 d | R1 |
| | EP-20 Progress check-ins | R2 | P1 | 3–5 d | R1 |
| | EP-21 Vocabulary notebook & quiz | R2 | P2 | 6–9 d | R1 |
| | EP-22 Inbox | R2 | P2 | 3–4 d | R1 |
| | EP-23 Content expansion | R2 | P1 | 8–12 d | R1 |
| | EP-24 R2 quality & release | R2 | P0 | 5–7 d | R2 epics |
| **IN-4 Reach (conditional)** | EP-25 Store launch | R3 | — | 5–8 d | D-03, DEP-03 |
| | EP-26 Video self-review | R3 | — | 8–12 d | ADR-010 revisit |
| | EP-27 Recognition research (SPK-03) | R3 | — | 6–10 d | ADR-009 |

**Totals (preliminary):** R0 ≈ 38–55 d · R1 ≈ 101–146 d · R2 ≈ 31–46 d.

> **Note (2026-11-21).** The Product Owner's direction adds work that these totals do not yet include: T-07.8 issue detection and tip selection (M ≈ 2–3 d), T-10.2 Techniques collection and SCR-PRAC-04 (M ≈ 2–3 d), T-09.7 technique records and tips (M ≈ 2–4 d), and the content-verification tasks T-09.6 and T-09.8. Engineering additions are ≈ +6–10 d on R1 (Proposed). **Content verification (T-09.6) is ≈ 38 person-days (CNT-01 §6.3)**, mostly the Product Owner watching source videos and approving rewrites. It runs in parallel with engineering but is on the R1 critical path for content freeze (RSK-04, RSK-17). Cataloguing further videos (T-09.8) is sized from the CNT-01 template once the Product Owner chooses the R1 technique set. EP-09 depends on D-08 (decided) and DEP-10.

## 3. Detailed breakdown — R0 Foundation

### EP-01 Build, CI/CD & release hardening (ENG-01)

| ID | Item | Type | Size | Delivers |
|---|---|---|---|---|
| F-01.1 | Reproducible builds | | | GAP-31 |
| T-01.1.1 | Commit Gradle wrapper; CI wrapper validation | OPS | S | ENG-01 §3 |
| T-01.1.2 | Dev setup guide incl. `google-services.json` handling; make unit tests independent of it | OPS/DOC | S | ISS-06 |
| F-01.2 | PR quality gates | | | GAP-30 |
| T-01.2.1 | PR workflow: build, unit + Robolectric tests, lint, artefact | OPS | M | ENG-01 §4 |
| T-01.2.2 | Firestore rules emulator tests in CI (`package.json`, lockfile) | OPS/TST | M | FND-TEST-04 |
| T-01.2.3 | Branch protection & required checks | OPS | S | GR-03 |
| F-01.3 | Release pipeline | | | GAP-01 |
| T-01.3.1 | Disable release-on-every-push; tag-driven release only; never debug builds | OPS | S | ISS-02 |
| T-01.3.2 | Release signing; R8 enabled with keep rules; AAB + APK | OPS | M | ADR-004, FND-SEC-11 |
| T-01.3.3 | Pin actions by SHA; least-privilege permissions; lockfiles | OPS | S | GAP-38 |
| F-01.4 | Dependency currency (ADR-006) | | | GAP-35 |
| T-01.4.1 | Remove unused catalog entries & plugins; fix JUnit ref; `debugImplementation` App Check debug | ENG | S | RD-02 §3 |
| T-01.4.2 | Upgrade Compose BOM, lifecycle, navigation, activity (separate PRs) | ENG/TST | M | RSK-11 |
| T-01.5 | Update release docs; retire `AUTO_UPDATE_GITHUB_CI_SETUP.md` to archive | DOC | S | GOV-02 |

### EP-02 Architecture restructure & app shell

| ID | Item | Type | Size | Delivers |
|---|---|---|---|---|
| F-02.1 | Application & DI | | | GAP-32 |
| T-02.1.1 | `SpeakCraftApplication` + process-scoped `AppContainer`; ViewModel factories | ENG | M | ARC-01 §2, §7 |
| T-02.1.2 | `Clock`/`DateKeyProvider`, `DispatcherProvider`, `AppLogger` (PII-free) | ENG/TST | S | ARC-01 §2 |
| F-02.2 | Package-by-feature skeleton (ADR-012) | | | GAP-33 |
| T-02.2.1 | Create `core/domain/speech/data/feature` packages; move theme to `core/designsystem` | ENG | M | ADR-012 |
| T-02.2.2 | Repository interfaces + fakes for tests | ENG/TST | M | ARC-04 §5 |
| T-02.2.3 | CI import-boundary check (simple script) | OPS | S | NFR-MAINT-001 |
| F-02.3 | New navigation shell | | | GAP-13 |
| T-02.3.1 | Type-safe routes; 4-tab graph with saved state; full-screen routes | ENG | M | DES-02 |
| T-02.3.2 | Compile-time feature flags to hide retired/unfinished features | ENG | S | ARC-02 §9 |
| T-02.3.3 | Navigation tests (Robolectric) | TST | S | TST-01 |
| T-02.4 | Namespace/applicationId change if D-15 approved | ENG | M | D-15 |

### EP-03 Data foundation

| ID | Item | Type | Size | Delivers |
|---|---|---|---|---|
| F-03.1 | Room database v1 (ADR-002) | | | GAP-22 |
| T-03.1.1 | Entities ENT-01…14, 18; DAOs; schema export | ENG | L | ARC-03 §3–5 |
| T-03.1.2 | Migration test harness | TST | S | ARC-03 §9 |
| F-03.2 | Repositories | | | SRS-FR-DATA-001 |
| T-03.2.1 | Profile, Attempt, Plan, Word, Preferences repositories (Room-backed, Flow APIs) | ENG/TST | L | ARC-04 §5 |
| T-03.2.2 | Event log with idempotency; outbox writes in transactions | ENG/TST | M | SRS-BR-020, ADR-014 |
| F-03.3 | Local profile & partitioning | | | SRS-FR-ACC-001 |
| T-03.3.1 | Local profile creation; per-profile file roots | ENG/TST | S | ADR-015 |

### EP-04 Safety, privacy fixes & feature removals

| ID | Item | Type | Size | Delivers |
|---|---|---|---|---|
| F-04.1 | Removals (behind flags first, then deletion) | | | PRD-FR-120 |
| T-04.1.1 | Remove YouTube Intelligence (ingestion, per-user seeded technique copies) and creator branding; keep the catalogue content for re-expression in EP-09 (D-08) | ENG | M | ADR-011, GAP-02/03 |
| T-04.1.2 | Remove cloud AI (firebase-ai, services, settings keys, false-assurance tests) | ENG/TST | S | ADR-003, GAP-08 |
| T-04.1.3 | Remove updater, update screens, release collections & publish script | ENG | S | ADR-004, GAP-07 |
| T-04.1.4 | Remove admin console, MediaPipe/TFLite/CameraX, mentor, conversation sim, tutorial systems | ENG | M | ADR-010, RD-16 §5 |
| F-04.2 | Privacy & data safety | | | GAP-04…06, GAP-20/21 |
| T-04.2.1 | Stop transcript upload; correct privacy text | ENG | S | ISS-03 |
| T-04.2.2 | Backup/data-extraction rules; recordings to `noBackupFilesDir` | ENG/TST | S | ADR-015 |
| T-04.2.3 | Redact PII from logs (AppLogger) | ENG/TST | S | GAP-06 |
| T-04.2.4 | Fix or retire `speakingSessions` write path (rules/payload) | ENG/TST | S | ISS-01 |
| T-04.2.5 | Wire account deletion with re-auth (interim, before FS-10 full flow) | ENG/TST | M | ISS-04 |
| T-04.2.6 | Remove fabricated dashboard displays (fallback scores, fake trends, day-0 streak) for any build distributed during R0 | ENG | S | GAP-11 |
| T-04.3 | Archive superseded docs to `docs/archive/` (separate commit) | DOC | S | ISS-07 |

### EP-05 Speech & rules spikes

| ID | Item | Type | Size | Delivers |
|---|---|---|---|---|
| T-05.1 | SPK-01 prototype: AudioRecord + on-device recogniser (audio-source piping, segmented session, restart stitching) on ≥ 4 devices | ENG | L | ADR-009, RSK-01 |
| T-05.2 | SPK-01 test set (40 utterances, consented recordings, accents) and WER/latency report | TST/DOC | M | ASM-05 |
| T-05.3 | SPK-02 rule corpus (≥ 30 pos/neg per candidate rule) and precision harness | TST | M | NFR-QUAL-003 |
| T-05.4 | Update ARC-05 tiers, NFR targets from spike results | DOC | S | NFR §10 |

## 4. Detailed breakdown — R1 Core loop

### EP-06 Speech pipeline (FS-04, FS-12; ARC-05 §3–4)

| ID | Item | Type | Size |
|---|---|---|---|
| T-06.1 | `SpeechCapabilityService` (tier detection, status, consent-aware) + tests | ENG/TST | M |
| T-06.2 | `CaptureController` (AudioRecord, PcmTee, WavWriter, focus/lifecycle interrupts) | ENG/TST | M |
| T-06.3 | `RecognitionSession` T1 (audio-source pipe, segmented) | ENG | M |
| T-06.4 | `RecognitionSession` T2/T3a (restart + stitching, de-dup) | ENG/TST | M |
| T-06.5 | Language-pack status & install (API 33+) | ENG | S |
| T-06.6 | Online-transcription consent storage & enforcement | ENG/TST | S |
| T-06.7 | `StreamingFeatureExtractor` (RMS, adaptive VAD, pauses) with fixtures | ENG/TST | M |
| T-06.8 | Manifest `<queries>`, permission flows | ENG | S |

### EP-07 Evaluation & feedback (FS-05; ARC-05 §5–10)

| ID | Item | Type | Size |
|---|---|---|---|
| T-07.1 | `EvidenceGate` (port + extend existing validator and its tests) | ENG/TST | S |
| T-07.2 | `MetricEngine` — catalogue M-* incl. MATTR, WPM, pauses, length, match | ENG/TST | M |
| T-07.3 | `ReferenceMatcher` (port alignment; relabel) | ENG/TST | S |
| T-07.4 | `RuleEngine` with qualified rules only (from SPK-02) | ENG/TST | M |
| T-07.5 | `FeedbackComposer` (priority table, strength/focus, quoting) | ENG/TST | M |
| T-07.8 | Issue detection + `TipSelector`: ISSUE_* catalogue, deterministic tip selection, empty state, self-check (ARC-05 §8.4; PRD-FR-047) with golden tests | ENG/TST | M |
| T-07.6 | Versioning, golden fixtures, "how we measured" text generation | ENG/TST | S |
| T-07.7 | `WordStageEvaluator` (stages 2–4) | ENG/TST | M |

### EP-08 Session player & Today (FS-03, FS-04)

| ID | Item | Type | Size |
|---|---|---|---|
| T-08.1 | `PlanEngine` (deterministic, recovery, swaps) + exhaustive unit tests | ENG/TST | M |
| T-08.2 | Today screen (greeting, session card, streak/progress summary, states) | ENG/TST | M |
| T-08.3 | Session player UI (all states), retry/compare, playback | ENG/TST | L |
| T-08.4 | Result & feedback detail sheet; accuracy rating | ENG/TST | M |
| T-08.5 | Session complete screen | ENG | S |

### EP-09 Content pack & production

| ID | Item | Type | Size |
|---|---|---|---|
| T-09.1 | Content-pack schema, loader, build-time validator | ENG/TST | M |
| T-09.2 | Review & de-brand 150 activities; map to 5 tracks × 5 templates; provenance | CNT | L |
| T-09.3 | Write ≥ 15 activities per track (gap fill) incl. onboarding `onb-intro` | CNT | M |
| T-09.4 | Word bank ≥ 300 entries (meaning, examples, inflections, level, track, provenance) | CNT | L |
| T-09.5 | Content QA (duplicates, level consistency, licence check) | CNT/TST | S |
| T-09.6 | Verify every creator-derived item in the current app against its specific video; re-express in our own words; record provenance; set `licenceStatus` (CNT-01 workflow; D-08). CNT-01 found 307 items, none with specific-video provenance; programmes are retired and their day content reused | CNT (Product Owner verifies and acts as reviewer; agent drafts rewrites) | ≈ 38 person-days † (CNT-01 §6.3, ≈ 300 h) |
| T-09.7 | Technique records: idea, steps, tips with triggers (≥ 1 tip per ISSUE_* code), linked activities | CNT | M |
| T-09.8 | Catalogue further channel videos with the CNT-01 template (R1: highest-value techniques; rest in R2 content updates — RSK-17) | CNT | L † |

### EP-10 Practice library (FS-06)

| T-10.1 | Practice home, track list with filters, activity detail, saved | ENG/TST | M |
| T-10.2 | Techniques collection and technique detail SCR-PRAC-04 (SRS-FR-PRAC-008/009; outbound video link) | ENG/TST | M |

### EP-11 Words lifecycle (FS-07)

| ID | Item | Type | Size |
|---|---|---|---|
| T-11.1 | Words home (today's words, due, list, filters, search) | ENG/TST | M |
| T-11.2 | Word stage flow (Learn, Say it, Use it, Make your own) incl. easier variant | ENG/TST | L |
| T-11.3 | `ReviewScheduler` (SM-2 variant) + review session | ENG/TST | M |
| T-11.4 | Today integration (new-word goal, due reviews) | ENG/TST | S |

### EP-12 Progress & streak (FS-08)

| T-12.1 | `ProgressCalculator` (streak, practice time, trends with noise bands) + boundary tests (time zones) | ENG/TST | M |
| T-12.2 | Progress overview, metric detail, history, attempt detail with replay | ENG/TST | M |
| T-12.3 | Milestone celebration (reduced-motion aware) | ENG | S |

### EP-13 Onboarding & account (FS-01, FS-02)

| T-13.1 | Onboarding steps SCR-ONB-01…07 with resumable state | ENG/TST | M |
| T-13.2 | First attempt in onboarding (`onb-intro`) | ENG/TST | S |
| T-13.3 | Optional Google sign-in; adoption of local data (re-key, merge) | ENG/TST | M |
| T-13.4 | Sign-out keep/remove; resumable account deletion full flow | ENG/TST | M |

### EP-14 Settings, privacy & data rights (FS-10)

| T-14.1 | Profile + 7 settings sections | ENG/TST | M |
| T-14.2 | Export (JSON; recordings ZIP subject to D-22) through the system "save to" dialog (SAF, SRS-FR-SET-004) | ENG/TST | M |
| T-14.3 | Clear history options (local + cloud) | ENG/TST | S |
| T-14.4 | Privacy information generated from policy constants + snapshot tests | ENG/TST | S |
| T-14.5 | About, licences, diagnostics log share (ADR-007) | ENG | S |

### EP-15 Reminders (FS-09)

| T-15.1 | `ReminderWorker`, channel, permission flow, suppression, reschedule on reboot/TZ | ENG/TST | M |

### EP-16 Sync & legacy migration (FS-11)

| T-16.1 | `SyncEngine` upload/pull/merge (ADR-014) + emulator tests | ENG/TST | L |
| T-16.2 | Target Firestore rules + rules tests (SEC-01 §5) | ENG/TST | M |
| T-16.3 | Legacy import (profile + vocabulary) with tests on a legacy snapshot | ENG/TST | M |
| T-16.4 | `CleanupWorker` (retention) | ENG/TST | S |

### EP-17 Design system & accessibility foundation (DES-05, DES-07)

| T-17.1 | Tokens (colour, type, shape, spacing, motion) and fonts | DES/ENG | M |
| T-17.2 | Core components (buttons, cards, MetricTile, FeedbackCard, InconclusiveCard, RecordButton, MistakeList, CoachTipCard, SelfCheckList, state templates — DES-05 §11) | ENG/TST | L |
| T-17.3 | Accessibility test utilities (font scale 2.0, semantics checks) | TST | S |

### EP-18 Quality, beta & release readiness (TST-01, ENG-01)

| T-18.1 | End-to-end instrumented core-loop tests on reference devices | TST | M |
| T-18.2 | Performance baseline vs NFR-PERF; fixes | TST/ENG | M |
| T-18.3 | Privacy/network-egress tests; honesty review | TST | S |
| T-18.4 | Accessibility manual pass (TalkBack, 200 %) | TST | S |
| T-18.5 | Beta programme setup (cohort, feedback form, "share beta summary") | OPS/DOC | S |
| T-18.6 | Release checklist, privacy policy page, store/GitHub release notes | OPS/DOC | S |
| T-18.7 | UAT, defect fixing buffer | TST/ENG | M |

## 5. R2 and R3 (summary)

| Epic | Main tasks |
|---|---|
| EP-19 Role-play | TPL-ROLE content schema, turn engine, TTS partner lines, per-turn evaluation, 10+ scenarios |
| EP-20 Check-ins | Check-in scheduling in PlanEngine, comparison UI, noise-band logic |
| EP-21 Notebook & quiz | ENT-15/17, notebook UI, deterministic quiz generator, tests |
| EP-22 Inbox | ENT-16, inbox UI, milestone/summary messages |
| EP-23 Content | +10 activities per track, +300 words |
| EP-24 R2 quality | Regression, beta, release |
| EP-25 Store launch | Play listing, data-safety form, account-deletion web page, Play Integrity (ADR-008) |
| EP-26 Video self-review | CameraX capture, playback, self-assessment checklist (no scoring) |
| EP-27 Recognition research | SPK-03 Whisper-class evaluation |

## 6. Prioritisation rationale

| Priority | Rule | Examples |
|---|---|---|
| P0 | Unsafe, legally exposed, broken core, or blocks other work | EP-01, EP-04, EP-05, EP-06, EP-07 |
| P1 | Needed for the MVP to be useful/compliant but can follow P0 | EP-15, EP-16 |
| P2 | Valuable depth, not required for MVP | EP-21, EP-22 |
| P3 | Optional | — |

Ordering within a priority uses dependency (PLN-04 §5), risk reduction (spikes first), user value (core loop before breadth) and effort.
