# Project Governance and Approvals

| Field | Value |
|---|---|
| Document ID | PLN-05 |
| Version | 0.3 — Approved (2026-11-21: Gates G1–G4 and decisions D-01…D-36 approved with recommended defaults) |
| Date | 2026-11-21 |
| Status | **Approved — Gates G1–G4 recorded; awaiting explicit "Start Stage B" instruction (GR-01)** |
| Owner (role) | Project Manager |
| Approval | **Approved — Product Owner (2026-11-21)** |
| Related | GOV-02 Document Control, PLN-01 Roadmap, PLN-04 RAID, TST-01, ENG-01, RD-99 |

---

## 1. Governance model

SpeakCraft is an individual project. The **Product Owner is the single accountable decision-maker and final approver** for every gate. The coding agent operates the cross-functional roles (product, design, architecture, security, QA, DevOps, writing, project management) and is responsible for **preparing the review artefacts** listed for each gate; it does not approve its own work and does not proceed past a gate without the Product Owner's explicit approval.

## 2. Roles

| Role | Accountable | Responsible (prepares) | Notes |
|---|---|---|---|
| Product Owner | ✔ all gates | Business inputs, legal/content decisions, accounts (Play, Firebase) | Final approver |
| CPO / Product Manager | — | Vision, strategy, PRD, BRD, stories, scope changes | Agent |
| UX Researcher / Strategist / UI Designer / Design System Architect | — | DES-01…08 | Agent |
| Solution Architect / Android Tech Lead / Database Architect / AI-ML Engineer | — | ARC-01…05, ADRs, implementation | Agent |
| Security Engineer | — | SEC-01, security reviews | Agent |
| QA Architect / Automation Engineer | — | TST-01, test suites, test reports | Agent |
| DevOps / Release Engineer | — | ENG-01, CI/CD, release candidates | Agent |
| Technical Writer | — | Document control, docs updates | Agent |
| Project Manager | — | Roadmap, WBS, sprint plans, RAID, status reports | Agent |
| Beta testers | — | UAT feedback | Recruited (DEP-09) |

## 3. Approval gates

| Gate | Name | Stage | Approver | Artefacts to review | Approval criteria |
|---|---|---|---|---|---|
| **G1** | Product approval | A | Product Owner | RD-07…RD-16, BRD, PRD, SRS, NFR, USER_STORIES, USE_CASES, FS-00…12 | Direction, segment (D-01), scope and non-goals accepted; decisions D-01…D-09, D-17, D-19, D-20 made |
| **G2** | UX approval | A | Product Owner | DES-01…DES-08 | IA, flows, design system and key screens accepted; D-18 decided |
| **G3** | Architecture & security approval | A | Product Owner | ARC-01…05, ADR-001…015, SEC-01, NFR targets | Each ADR approved/rejected; D-10…D-16 decided |
| **G4** | Implementation approval (Stage B start) | A→B | Product Owner | PLN-01…05, ENG-01, TST-01, GOV-01, RD-99 | Plan, estimates (as preliminary) and governance accepted; explicit "start Stage B" instruction |
| **G5** | Epic review | B (per epic) | Product Owner | PR(s), test report from CI, updated docs, demo notes/screenshots | Definition of Done met (§6); no open Critical/High defects in the epic |
| **G6** | QA & UAT approval | B (per release) | Product Owner | Release-candidate test report, honesty checklist, accessibility & privacy checks, UAT summary | TST-01 exit criteria met |
| **G7** | Release approval | B (per release) | Product Owner | Release checklist (ENG-01 §8), notes, rollback plan | Checklist complete; explicit release instruction |

Gates G1–G3 may be reviewed in one sitting; each must be recorded separately in §7. The decision list per gate in the table above predates decisions D-21…D-36; **RD-99 §10 is the current mapping of decisions to gates**.

## 4. Governance rules

| ID | Rule |
|---|---|
| GR-01 | **No implementation before G4.** Stage A changes are documentation only |
| GR-02 | **One approved epic at a time** on a feature branch; small, reviewable PRs; feature flags for incomplete work |
| GR-03 | **Evidence before claims.** No document, PR or report may state that tests passed, a build succeeded, or a feature is verified without a link to the CI run or recorded output. Unexecuted tests are reported as "not run" |
| GR-04 | **Technology changes only via approved ADR** (PO-REQ-1120 §2) |
| GR-05 | **Scope changes only via PRD change** approved at G1 (or G5 for in-epic adjustments); RD-16, PRD, PLN-01/02 and GOV-01 updated together |
| GR-06 | **Direction changes are decisions.** Changes to principles (RD-15) or non-goals (RD-16 §4) require a recorded D-nn decision |
| GR-07 | **Docs move with code.** Behaviour changes update the authoritative document in the same PR |
| GR-08 | **Privacy review** for any change touching audio, transcripts, network egress or data retention (SEC-01 §9) |
| GR-09 | **No release without G6 and G7** and a release-signed build from CI |
| GR-10 | **Commit/push/release only when the Product Owner asks** (agent operating rule) |

## 5. Decision management

- Open decisions are listed in RD-99 §8 with IDs D-nn, options and a recommendation.
- The Product Owner records each decision as **Approved / Rejected / Deferred** with date in §7.
- Rejected recommendations trigger updates to the affected documents before G4.

## 6. Definition of Done (Stage B, per story/epic)

1. Code implements the referenced SRS-FR/SRS-BR and passes code review against ARC-02 conventions.
2. Unit/Robolectric tests written alongside the code; all CI gates green (build, unit, Robolectric, lint, rules tests where relevant).
3. Acceptance criteria (FS/US) demonstrated; evidence (CI link, screenshots or recordings) attached.
4. Accessibility checks for new UI (DES-07) and honesty checklist (TST-01 §9) passed.
5. No new Critical/High defects; known issues logged.
6. Documentation updated (authoritative doc + GOV-01 traceability).
7. Feature flag state documented; no dead code left behind for removed features.

## 7. Approval record

| Gate / Decision | Date | Outcome | Notes |
|---|---|---|---|
| G1 Product approval | 2026-11-21 | **Approved** | Product direction, primary segment (S1 B1–B2 professionals & job seekers; S2 secondary), scope, non-goals, and G1 decisions (`D-01`–`D-09`, `D-17`, `D-19`–`D-22`, `D-32`, `D-36`) approved with recommended defaults |
| G2 UX approval | 2026-11-21 | **Approved** | IA (4 tabs: Today, Practice, Words, Progress), Calm Indigo design system (`D-18`), flows, screen specs, accessibility guidelines, and `D-33`/`D-34` approved with recommended defaults |
| G3 Architecture & security approval | 2026-11-21 | **Approved** | Solution/technical/DB/local-intelligence architecture, `ADR-001`–`ADR-015`, `SEC-01`, and G3 decisions (`D-10`–`D-16`, `D-23`–`D-26`) approved with recommended defaults. Note: `D-15` keeps `applicationId` unchanged (`com.aistudio.speakcraft.qxmvkp`); platform signing configs preserved |
| G4 Implementation approval | 2026-11-21 | **Approved — Stage B Started** | Roadmap, WBS, sprint plan, RAID, governance, DevOps/test strategy, and G4 decisions (`D-27`–`D-31`, `D-35`) approved with recommended defaults. Product Owner issued explicit `"Start Stage B"` (incremental one-task-at-a-time execution) on 2026-11-21 |
| D-01 Primary segment | 2026-11-21 | **Approved — S1 primary, S2 secondary** | Recommended default |
| D-02 Evaluation philosophy | 2026-11-21 | **Approved — Measured-first, labelled, Inconclusive over guessing** | Recommended default |
| D-03 Distribution & in-app updater | 2026-11-21 | **Approved — Remove in-app updater; Play tracks / release-signed GitHub downloads** | Recommended default (ADR-004) |
| D-04 Cloud AI | 2026-11-21 | **Approved — Remove Cloud AI for R1–R2** | Recommended default (ADR-003); deterministic & on-device logic only |
| D-05 Account model | 2026-11-21 | **Approved — Optional account (local profile; Google Sign-In to back up)** | Recommended default |
| D-06 Composite overall score (free speaking) | 2026-11-21 | **Approved — Do not show in R1** | Recommended default; revisit after beta (RSK-14) |
| D-07 Monetisation | 2026-11-21 | **Approved — Out of scope until after R1** | Recommended default |
| D-42 Audio and video analyser | 2026-11-23 | **Approved — analysis + local replay** | Product Owner's answers in the session: on-device video analysis with local replay; verify with a local Android SDK and CI on each pushed increment; download and bundle the official MediaPipe face and pose models. Amends RD-16 NG-07 and supersedes ADR-010 in part (ADR-017, ARC-07; GR-06) |
| D-08 Creator-derived content | 2026-11-21 | **Approved — Option B ("original wording, link out")** | Techniques re-expressed in original wording with specific source video links and zero creator branding (PRD-FR-056/057, SRS-BR-028) |
| D-09 Success measurement | 2026-11-21 | **Approved — Option A (no analytics SDK in R1 beta)** | Recommended default (ADR-013) |
| D-10 Recording retention default | 2026-11-21 | **Approved — 30 days, user-changeable** | Recommended default (ADR-015) |
| D-11 Room as local source of truth | 2026-11-21 | **Approved (incl. `room-testing`)** | Recommended default (ADR-002) |
| D-12 WorkManager | 2026-11-21 | **Approved** | Recommended default (ADR-005) |
| D-13 Crash reporting | 2026-11-21 | **Approved — Local PII-free diagnostic log + Play vitals** | Recommended default (ADR-007) |
| D-14 Minimum Android version & tiers | 2026-11-21 | **Approved — Raise `minSdk` to 26; keep T1/T2/T3a/T3b tiers** | Recommended default (ADR-009) |
| D-15 `applicationId` / `namespace` | 2026-11-21 | **Approved — Keep `applicationId` `com.aistudio.speakcraft.qxmvkp`** | Recommended default (ADR-012); note platform constraint to preserve `com.example` R-class/source layout if needed |
| D-16 Transcript cloud sync | 2026-11-21 | **Approved — R2 opt-in, default off** | Recommended default |
| D-17 Level taxonomy | 2026-11-21 | **Approved — 5 plain levels with "can do" lines** | Recommended default |
| D-18 Brand palette | 2026-11-21 | **Approved — Calm Indigo (fixed, no dynamic colour)** | Recommended default (DES-05) |
| D-19 Legacy data migration | 2026-11-21 | **Approved — Profile + vocabulary only (90-day window)** | Recommended default |
| D-20 Words tab in R1 | 2026-11-21 | **Approved — Include in R1** | Recommended default |
| D-21 Sign-out default | 2026-11-21 | **Approved — Remove when synced; Keep (with warning) when unsynced** | Recommended default |
| D-22 Audio in data export | 2026-11-21 | **Approved — Optional, off-by-default recordings ZIP in R1** | Recommended default |
| D-23 Backup policy | 2026-11-21 | **Approved — Include-only (`learning.db` + `settings`)** | Recommended default (SEC-01 §6) |
| D-24 Local transcript retention | 2026-11-21 | **Approved — Keep with attempt + optional delete with recordings** | Recommended default |
| D-25 Data-safety declaration (T3a) | 2026-11-21 | **Approved — Declare "shared, optional"** | Recommended default |
| D-26 App Check enforcement | 2026-11-21 | **Approved — Enforce when Play-only and >= 98% verified for 14 days** | Recommended default (ADR-008) |
| D-27 Firebase projects | 2026-11-21 | **Approved — `dev` + `prod` (`prod` = existing project)** | Recommended default (ENG-01) |
| D-28 Signing key | 2026-11-21 | **Approved — Project-owned key uploaded to Play App Signing** | Recommended default (ENG-01) |
| D-29 Security-rules deployment | 2026-11-21 | **Approved — Manual in R0–R1** | Recommended default (ENG-01) |
| D-30 Privacy & deletion page hosting | 2026-11-21 | **Approved — GitHub Pages** | Recommended default (ENG-01) |
| D-31 Firestore PITR | 2026-11-21 | **Approved — Deferred / Product Owner choice on cost** | Disclose 7-day window if enabled |
| D-32 Word practice without transcription (T3b) | 2026-11-21 | **Approved — "Needs a transcript" state + Today swap** | Recommended default |
| D-33 TalkBack mic capture | 2026-11-21 | **Approved — Announce before capture, silent during recording, announce after Stop** | Recommended default |
| D-34 Streak accessibility | 2026-11-21 | **Approved — Keep SRS-BR-010; Skip never penalises; revisit post-beta** | Recommended default |
| D-35 Legal opinion on techniques | 2026-11-21 | **Approved — Before public store launch (R3)** | D-08 rules enforced from R0 |
| D-36 R1 technique set | 2026-11-21 | **Approved — Curated subset (~15 techniques) in R1** | Recommended default; rest in R2 content updates |
| D-37…D-41 Future AI layer (R3+) | — | Deferred to R3 scope review | Not required for G1–G4 |

## 8. Reporting cadence (Stage B)

| Report | Frequency | Content |
|---|---|---|
| Sprint review note | Every 2 weeks | Completed stories with evidence, demo, metrics, RAID changes |
| Epic review pack | Per epic (G5) | PRs, CI report, docs diff, open defects |
| Release report | Per release (G6/G7) | Test summary, checklist, KPIs (beta) |
