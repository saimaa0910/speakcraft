# User Stories — SpeakCraft

| Field | Value |
|---|---|
| Document ID | US |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager |
| Approval | Pending — Product Owner |
| Related | PRD, SRS, FS-00…FS-12, GOV-01 |

> **Authority (GOV-02 §3.2, §4).** This document is the authoritative home of story IDs `US-nnn`. Stories describe behaviour from the user's point of view. They **reference** product requirements (`PRD-FR`), software requirements (`SRS-FR`), business rules (`SRS-BR`) and errors (`SRS-ERR`) by ID and do not redefine them. If the SRS and a story disagree, the SRS wins and the gap is logged in §6. Acceptance criteria marked **RECOMMENDATION** go beyond the current SRS and become binding only when the Product Owner approves them. Values marked † are **Proposed — requires approval** (GOV-02 DC-5).

---

## 1. Purpose and scope

1. Express every product functional requirement in PRD §15 (PRD-FR-001…121) as one or more user stories that can be estimated, planned (PLN-02 WBS) and accepted (PLN-05 Definition of Done).
2. Give each story testable Given/When/Then acceptance criteria consistent with SRS §16 business rules, so that FS test cases (`TC-<AREA>-nnn`) and GOV-01 can link to them.
3. Cover releases R0 and R1 in full and R2 briefly. R3 is out of scope (RD-16 §3).

**Product Owner direction (2026-11-21), applied here.**

1. **Curriculum.** The curriculum is built from the frameworks, techniques, exercises, activities and tips taught on the @askvinh YouTube channel (PRD-FR-056, SRS-FR-PRAC-008). See US-035.
2. **D-08 — DECISION (Product Owner, 2026-11-21): "original wording, link out".** Content is re-expressed in SpeakCraft's own words and examples, with no verbatim transcripts. Each item links to its specific source video and timestamp, and the creator's name, likeness and branding never appear in the app, content, notifications or store listing. Content is gathered by hand into the bundled content pack, with no scraping, caption download or API ingestion. Every item carries provenance and `licenceStatus`, and unverified items never ship (PRD-FR-057, SRS-FR-PRAC-009, SRS-BR-028, ADR-011). See US-034, US-036 and US-074. This is the first DECISION recorded in Stage A (GOV-02 DC-3).
3. **Feedback after every submission.** Every submitted attempt shows the mistakes detected, with evidence, and coach tips. These come from deterministic, on-device logic, with no cloud AI (PRD-FR-047, SRS-FR-EVAL-015/016, SRS-FR-WORD-014; D-04). See US-026 and US-042.

The product shape inside Practice (a Techniques collection next to the tracks, the existing four tabs, the creator-branded tab replaced by technique content) follows canon R-6 and SRS-FR-PRAC-008. Detail that goes beyond the SRS is labelled RECOMMENDATION. Gaps are listed in Issues 1, 4 and 27–31.

## 2. Story template and conventions

### 2.1 Template

```text
### US-nnn — <short title in the user's words>
| Field | Value |
| Story | As <persona name or user class>, I want <capability>, so that <benefit>. |
| Persona(s) · user class | <names (P-ID)> · <Local user | Signed-in user | Beta tester> |
| Priority · Release | <P0…P3, inherited from the PRD requirement> · <R0 | R1 | R2> |
| Business value | <High | Medium | Low> — <why>, (<BO-nn>; <KPI-nn>) |
| Dependencies | <US-nnn>, <ADR-nnn>, <SPK-0n>, <D-nn decision> |
| Traces | <PRD-FR-nnn> · <SRS-FR-…> · <FS-nn> |
Acceptance criteria: AC1…AC5 as Given / When / Then
Edge cases: 2–4, each pointing at the governing rule, error or open issue
```

### 2.2 Conventions

| Convention | Rule |
|---|---|
| Story ID | `US-nnn`, three digits, never reused. A retired story is marked *Withdrawn* (GOV-02 §7) |
| Acceptance-criterion reference | `US-nnn AC<n>` (for example US-020 AC3). GOV-01 links FS criteria `AC-FSnn-nn` and test cases to these |
| Personas | Named by name: Aarav (P1), Meera (P2), Daniel (P3), Sofia (P4), Hoang (P5) (PRD §7, RD-08). They are hypotheses, not research findings (ASM-01). This document refers to a persona by name or as "they" |
| User classes | SRS §4: **Local user**, **Signed-in user**, **Beta tester**. The SRS abbreviates them UC-L/UC-S/UC-B. This document spells them out so they cannot be confused with use-case IDs `UC-nn` (Issue 16) |
| Priority | Inherited from the traced PRD requirement (PRD §15: P0 must, P1 should, P2 could, P3 later). Where a story traces several PRD-FRs, it takes the highest |
| Release | From the PRD requirement: R0 Foundation, R1 Core loop MVP, R2 Depth |
| Business value | **High**: needed for the core loop, a P0 compliance obligation, or a KPI that gates R1. **Medium**: improves a KPI or reduces a known risk. **Low**: convenience. Always linked to BO-nn (BRD §3) |
| Dependencies | Other stories that must be done first, ADRs that must be approved, spikes (SPK-01 device recognition, SPK-02 grammar-rule precision corpus) and open decisions (D-nn, RD-99 §8) |
| Screen names | DES-02 §4 IDs (`SCR-*`) |
| Decision defaults | Where a decision is pending, acceptance criteria follow the consolidated recommendation in the Stage A canon (D-21 sign-out, D-22 export audio, D-23 backup, D-24 transcripts, D-10 retention) and say so. **D-08 is a DECISION** (Product Owner, 2026-11-21: "original wording, link out") and is applied as binding through PRD-FR-057 and SRS-BR-028 |

### 2.3 Definition of Ready and Done (summary — PLN-05 is authoritative)

| Ready | Done |
|---|---|
| Traces to ≥ 1 PRD-FR, ≥ 1 SRS-FR and its FS; acceptance criteria agreed; dependencies approved or stubbed; open decisions affecting it are listed | Acceptance criteria pass as automated or documented manual tests traced in GOV-01; honesty checklist (TST-01 §9) passes for any user-visible metric or status; accessibility checks (DES-07) pass; no P0/P1 defects open |

## 3. Epic-to-story map

One epic per capability (RD-16 §2) plus a transition/platform group. Each epic has one feature specification (FS-00 §1).

| Epic | Capability | FS | Stories | PRD-FRs covered | Release |
|---|---|---|---|---|---|
| E-01 | CAP-01 Account & sign-in | FS-01 | US-001…US-007 | 001–005, 094 (local delete) | R1 |
| E-02 | CAP-02 Onboarding & personalisation | FS-02 | US-008…US-011 | 010–014 | R1 |
| E-03 | CAP-03 Today — daily session | FS-03 | US-012…US-017 | 020–026 | R1 |
| E-04 | CAP-04 Speaking practice session | FS-04 | US-018…US-023 | 030–036 | R1 |
| E-05 | CAP-05 Honest feedback & evaluation (incl. mistakes and coach tips) | FS-05 | US-024…US-030 | 040–047 | R1 |
| E-06 | CAP-06 Practice library (incl. technique curriculum, D-08) | FS-06 | US-031…US-038 | 050–057 | R1 / R2 |
| E-07 | CAP-07 Words — vocabulary lifecycle | FS-07 | US-039…US-047 | 060–067, 047 (word results) | R1 / R2 |
| E-08 | CAP-08 Progress & streak | FS-08 | US-048…US-053 | 070–075 | R1 / R2 |
| E-09 | CAP-09 Reminders & inbox | FS-09 | US-054…US-056 | 080–084 | R1 / R2 |
| E-10 | CAP-10 Profile, settings, privacy & data rights | FS-10 | US-057…US-064 | 090–097 | R1 |
| E-11 | CAP-11 Offline-first storage & sync | FS-11 | US-065…US-068 | 100–103 | R1 |
| E-12 | CAP-12 Speech capability management | FS-12 | US-069…US-073 | 110–114 | R1 |
| E-13 | Transition & platform (no FS; ENG-01, SEC-01) | — | US-074, US-075 | 120, 121 | R0–R1 |

**Totals:** 75 stories: 71 in R0/R1 and 4 in R2 (US-038, US-047, US-053, US-056). All 78 defined PRD-FRs are covered (§5), including PRD-FR-047, -056 and -057, which were added to PRD v0.2 on 2026-11-21.

### 3.1 Story index

| ID | Title | P | R | FS |
|---|---|---|---|---|
| US-001 | Practise without an account | P1 | R1 | FS-01 |
| US-002 | Sign in with Google | P1 | R1 | FS-01 |
| US-003 | Keep my local progress when I first sign in | P1 | R1 | FS-01 |
| US-004 | Sign out and choose what stays on the device | P1 | R1 | FS-01 |
| US-005 | Delete my account and all my data | P0 | R1 | FS-01 |
| US-006 | Finish an interrupted account deletion | P0 | R1 | FS-01 |
| US-007 | Delete everything on this device without an account | P0 | R1 | FS-01 |
| US-008 | Personalise SpeakCraft in a few quick steps | P0 | R1 | FS-02 |
| US-009 | Understand privacy before granting the microphone | P0 | R1 | FS-02 |
| US-010 | Find out what speech support my phone has | P0 | R1 | FS-02 |
| US-011 | Make my first attempt during onboarding | P0 | R1 | FS-02 |
| US-012 | Start one clear daily session | P0 | R1 | FS-03 |
| US-013 | Get a plan built for me that stays stable today | P0 | R1 | FS-03 |
| US-014 | Keep completed steps when the plan adapts | P0 | R1 | FS-03 |
| US-015 | Come back gently after missed days | P1 | R1 | FS-03 |
| US-016 | See a greeting, my streak and today's progress | P1 | R1 | FS-03 |
| US-017 | Swap a step or change today's duration | P2 | R1 | FS-03 |
| US-018 | Prepare and record my answer with clear controls | P0 | R1 | FS-04 |
| US-019 | See what I said after I stop | P0 | R1 | FS-04 |
| US-020 | Be told honestly when an attempt is inconclusive | P0 | R1 | FS-04/05 |
| US-021 | Try again and compare with my last attempt | P0 | R1 | FS-04 |
| US-022 | Listen to my own recording and to a reference reading | P1 | R1 | FS-04 |
| US-023 | Stay safe when a recording is interrupted | P0 | R1 | FS-04 |
| US-024 | See three relevant metrics, each labelled Measured or Estimated | P0 | R1 | FS-05 |
| US-025 | Get one strength and one focus | P0 | R1 | FS-05 |
| US-026 | See what went wrong and a coach tip after every attempt | P0 | R1 | FS-05 |
| US-027 | See only trustworthy language observations | P1 | R1 | FS-05 |
| US-028 | No made-up overall score | P0 | R1 | FS-05 |
| US-029 | Understand and rate my feedback | P1 | R1 | FS-05 |
| US-030 | Know which rules evaluated an attempt | P0 | R1 | FS-05 |
| US-031 | Browse tracks and find the right activity | P0 | R1 | FS-06 |
| US-032 | Practise with the five R1 templates | P0 | R1 | FS-06 |
| US-033 | Count library practice toward progress and streak | P0 | R1 | FS-06 |
| US-034 | Use curated, sourced content that works offline | P0 | R1 | FS-06 |
| US-035 | Learn a named speaking technique and practise it | P0 | R1 | FS-06 |
| US-036 | Watch the original lesson a technique comes from | P0 | R1 | FS-06 |
| US-037 | Save activities for later | P2 | R1 | FS-06 |
| US-038 | (R2) Rehearse a scripted role-play | P1 | R2 | FS-06 |
| US-039 | Learn a new word | P0 | R1 | FS-07 |
| US-040 | Say it and use it in given sentences | P0 | R1 | FS-07 |
| US-041 | Make my own sentence and complete the word | P0 | R1 | FS-07 |
| US-042 | Know what went wrong in a word attempt and how to fix it | P0 | R1 | FS-07 |
| US-043 | Get new words and due reviews in Today | P0 | R1 | FS-07/03 |
| US-044 | Review words by speaking on a spaced schedule | P0 | R1 | FS-07 |
| US-045 | Browse, search and filter my words | P1 | R1 | FS-07 |
| US-046 | Start with a ready-made word bank | P0 | R1 | FS-07 |
| US-047 | (R2) Keep a word notebook and take a quiz | P1 / P2 | R2 | FS-07 |
| US-048 | See trends in my measured metrics | P0 | R1 | FS-08 |
| US-049 | Review past attempts and replay them | P0 | R1 | FS-08 |
| US-050 | Keep one honest streak | P0 | R1 | FS-08 |
| US-051 | Trust my practice statistics | P0 | R1 | FS-08 |
| US-052 | See my word progress at a glance | P1 | R1 | FS-08 |
| US-053 | (R2) Compare check-ins over time | P1 | R2 | FS-08 |
| US-054 | Turn on one daily reminder, asked for permission in context | P1 | R1 | FS-09 |
| US-055 | Receive reminders that respect me | P1 | R1 | FS-09 |
| US-056 | (R2) Read learning messages in an inbox | P2 | R2 | FS-09 |
| US-057 | View and edit my profile | P1 | R1 | FS-10 |
| US-058 | Find every setting in a predictable place | P0 | R1 | FS-10 |
| US-059 | Control speech and recording privacy | P0 | R1 | FS-10/12 |
| US-060 | Export all my data | P0 | R1 | FS-10 |
| US-061 | Clear selected learning history | P0 | R1 | FS-10 |
| US-062 | Read accurate privacy information | P0 | R1 | FS-10 |
| US-063 | Choose theme and motion | P1 | R1 | FS-10 |
| US-064 | See app information and share diagnostics | P1 | R1 | FS-10 |
| US-065 | Practise fully offline | P0 | R1 | FS-11 |
| US-066 | Back up progress automatically | P1 | R1 | FS-11 |
| US-067 | Use two devices without losing progress | P1 | R1 | FS-11 |
| US-068 | Keep my profile and words from the old app | P1 | R1 | FS-11 |
| US-069 | See an honest speech-capability status | P0 | R1 | FS-12 |
| US-070 | Install the offline speech pack from the app | P1 | R1 | FS-12 |
| US-071 | Decide about online transcription | P0 | R1 | FS-12 |
| US-072 | Get useful feedback without a transcript | P0 | R1 | FS-12 |
| US-073 | Choose my recognition accent | P2 | R1 | FS-12 |
| US-074 | Use an app without retired features or unused permissions | P0 | R0–R1 | — |
| US-075 | Install only trusted, release-signed builds | P0 | R0 | — |

---

## 4. User stories

### 4.1 E-01 — CAP-01 Account & sign-in (FS-01)

#### US-001 — Practise without an account

| Field | Value |
|---|---|
| Story | As Meera, I want to use every core feature without creating an account, so that I can find out privately whether SpeakCraft helps me before I share anything. |
| Persona(s) · user class | Meera (P2), Hoang (P5) · Local user |
| Priority · Release | P1 · R1 |
| Business value | High — removes the sign-up barrier on the way to the first valid attempt (BO-02, BO-04; KPI-01, KPI-02) |
| Dependencies | US-065; ADR-002; decision D-05 |
| Traces | PRD-FR-001 · SRS-FR-ACC-001 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A fresh install and no network | They open the app for the first time | A local profile with a unique ID is created and onboarding starts, with no sign-in step and no network request |
| AC2 | A local user who has finished onboarding | They use Today, Practice, Words and Progress | Every feature works and no screen requires sign-in |
| AC3 | A local user | They practise | Their data is stored on the device only (SRS §4 Local user) |

**Edge cases:** (1) The phone is restored from an Android backup: learning data returns (D-23 include-only backup), but recordings and transcripts do not, and history says so (SEC-01 §6.5, RECOMMENDATION). (2) Google Play services are missing: local use is unaffected (SRS §18). (3) A user who never signs in causes no Firebase traffic (FS-01 §13, RECOMMENDATION).

#### US-002 — Sign in with Google

| Field | Value |
|---|---|
| Story | As Aarav, who uses both a work phone and a personal phone, I want to sign in with Google whenever I choose, so that my progress is backed up and available on both. |
| Persona(s) · user class | Aarav (P1), Daniel (P3) · Local user becoming Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — protects progress across device changes without forcing an account (BO-02, BO-04) |
| Dependencies | US-001, US-066; ADR-001, ADR-014 |
| Traces | PRD-FR-002 · SRS-FR-ACC-002, SRS-FR-ACC-005 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A local user who is online | They choose Sign in with Google in Settings → Account | The sign-in screen explains what backs up and what never leaves the device before the Google account sheet opens |
| AC2 | The Google account sheet is open | They cancel | They return to the previous screen and no error text is shown |
| AC3 | Sign-in fails for any reason other than cancellation | The failure is returned | A plain-language message and Retry are shown, with no error codes (SRS-ERR-007) |
| AC4 | Sign-in succeeds | They look at Settings → Account | Their account and the sync status are shown (SRS-FR-DATA-005) |

**Edge cases:** (1) Offline when starting: they are told a connection is needed and that they can keep practising without an account (FS-01 E1). (2) No Google account on the device (FS-01 E2). (3) The SRS says sign-in is offered "from onboarding", but the onboarding sequence has no sign-in step (FS-01 Issue 4). (4) Sign-in started from the Progress backup prompt returns to Progress (DES-02 §2.2).

#### US-003 — Keep my local progress when I first sign in

| Field | Value |
|---|---|
| Story | As Daniel, who has practised for two weeks without an account, I want my existing progress to move into my account when I sign in, so that I lose nothing. |
| Persona(s) · user class | Daniel (P3), Meera (P2) · Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | High — losing data at sign-in would break trust and the streak (BO-01, BO-02) |
| Dependencies | US-002, US-066, US-067; ADR-002, ADR-014 |
| Traces | PRD-FR-003, PRD-FR-102 · SRS-FR-ACC-003, SRS-FR-ACC-004 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A local user with attempts and words in progress | They sign in for the first time | All local data is re-keyed to their account in one transaction and every syncable record is queued for upload |
| AC2 | The account already has cloud data from another device | Sign-in completes | The data is merged by the ADR-014 rules and no completed attempt or passed word stage is discarded (SRS-BR-020, SRS-BR-021) |
| AC3 | The re-key transaction fails | The failure occurs | No record is left partly re-keyed and they are told their data is safe on the phone (SRS-ERR-005) |

**Edge cases:** (1) A different Google account signs in on a phone that holds another account's kept data: not defined by the SRS (FS-01 Issue 1). (2) Recordings are never queued for upload (SRS-FR-DATA-006). (3) The pull after sign-in fails: they stay signed in and the status shows "Error — Retry" (SRS-ERR-006).

#### US-004 — Sign out and choose what stays on the device

| Field | Value |
|---|---|
| Story | As Meera, who sometimes lends the phone to family members, I want to choose whether my practice data stays on the phone when I sign out, so that nobody else sees my recordings or history. |
| Persona(s) · user class | Meera (P2) · Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — protects privacy on shared and second-hand phones (BO-04) |
| Dependencies | US-002, US-066; decision D-21 |
| Traces | PRD-FR-004 · SRS-FR-ACC-006 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A signed-in user whose changes are all uploaded | They choose Sign out | The dialog offers "Remove my practice data from this device" and "Keep it on this device", with Remove pre-selected (D-21 recommendation) |
| AC2 | Changes exist that have not been uploaded | The sign-out dialog opens | It states how many changes have not been backed up and pre-selects Keep (D-21 recommendation) |
| AC3 | Either option is confirmed | Sign-out completes | Credential state is cleared and background sync stops |
| AC4 | Remove is confirmed | Sign-out completes | The profile's data, preferences and recordings are deleted from the phone (SRS-BR-022) |

**Edge cases:** (1) Sign-out works offline in both branches (FS-01 §12). (2) The same account signs in again after Keep: changes made while signed out are not queued under SRS-FR-DATA-003 (FS-01 Issue 2). (3) Online-transcription consent is a device preference and does not change (SRS-BR-023). (4) FS-01 v0.2 and SEC-01 v0.2 now follow D-21, but they still describe the *Keep* branch's data differently (Issue 8).

#### US-005 — Delete my account and all my data

| Field | Value |
|---|---|
| Story | As Sofia, who is careful about personal data, I want to delete my account and everything linked to it after confirming, so that I can be sure nothing of mine remains. |
| Persona(s) · user class | Sofia (P4), Meera (P2) · Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — a legal and store-policy obligation and a core privacy promise (BO-04, BO-06; KPI-09) |
| Dependencies | US-002, US-006, US-060 (export offered first); ADR-014, ADR-015 |
| Traces | PRD-FR-005 · SRS-FR-ACC-007 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A signed-in user who is online | They open Delete account | The app explains what will be deleted in the cloud and on the phone and that it cannot be undone |
| AC2 | They have confirmed explicitly | Re-authentication is cancelled or fails | Nothing is deleted (SRS-ERR-007) |
| AC3 | They confirm and re-authenticate | Deletion runs | All Firestore documents under `users/{uid}` are deleted, then the Firebase Auth user, then all local data and recordings for the profile |
| AC4 | Deletion has finished | The result is shown | A completion confirmation is shown and the app returns to its first-launch state |

**Edge cases:** (1) Offline at the start: deletion does not begin (FS-01 E8). (2) The re-authenticated account is not the signed-in account: abort (SEC-01 §4.5, RECOMMENDATION). (3) Legacy collections outside `users/{uid}` (FS-01 Issue 3). (4) "Recent login required" at the last step: re-authenticate and resume rather than restart (SEC-01 §4.5).

#### US-006 — Finish an interrupted account deletion

| Field | Value |
|---|---|
| Story | As Sofia, I want an interrupted account deletion to finish the next time I open the app, so that I am never left with half-deleted data. |
| Persona(s) · user class | Sofia (P4) · Signed-in user with a deletion in progress |
| Priority · Release | P0 · R1 |
| Business value | High — partial deletion is a current defect (FND-SEC-03, ISS-04); completeness must be verifiable (BO-04; KPI-09) |
| Dependencies | US-005; ENT-18 DeletionState (ARC-03) |
| Traces | PRD-FR-005 · SRS-FR-ACC-008 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A deletion was interrupted after it started | The app is next launched | The deletion-resume screen (SCR-SYS-01) appears before any other screen and deletion continues from the saved step (SRS-ERR-009) |
| AC2 | The Auth user was already deleted before the interruption | Deletion resumes | It continues with local deletion |
| AC3 | The resumed deletion completes | It finishes | No documents remain under `users/{uid}`, no local data or files remain for the profile (NFR-REL-009) and onboarding starts |

**Edge cases:** (1) Offline at resume: not defined by the SRS (FS-01 Q3, Issue 5). (2) The process is killed again during resume. (3) Re-authentication needed at resume. (4) App uninstalled mid-deletion: SEC-01 §9.3 proposes a cloud marker (RECOMMENDATION).

#### US-007 — Delete everything on this device without an account

| Field | Value |
|---|---|
| Story | As Hoang, who uses SpeakCraft without an account on a phone shared at home, I want to delete all my SpeakCraft data from this phone, so that I can start again or pass the phone on safely. |
| Persona(s) · user class | Hoang (P5) · Local user (or signed-out user with kept data) |
| Priority · Release | P0 · R1 |
| Business value | Medium — data rights for people without an account (BO-04; KPI-09) |
| Dependencies | US-001; ADR-015 |
| Traces | PRD-FR-094 · SRS-FR-ACC-009 · FS-01 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A local or signed-out user | They choose Delete everything on this device | A confirmation lists what will be lost and that it cannot be undone |
| AC2 | They confirm | Deletion completes | All local data, preferences and recordings are deleted without network, and onboarding starts on a new local profile |

**Edge cases:** (1) Overlap with Clear history → Everything (SRS-FR-SET-005) is not explained (FS-01 Issue 6). (2) A file deletion fails partway: it is retried and success is never shown for a failed write (SRS-ERR-005). (3) A reminder scheduled before deletion must not fire afterwards (SEC-01 §6.4, RECOMMENDATION).

### 4.2 E-02 — CAP-02 Onboarding & personalisation (FS-02)

#### US-008 — Personalise SpeakCraft in a few quick steps

| Field | Value |
|---|---|
| Story | As Aarav, I want to set my goal, level, daily time and reminder in a few quick steps with sensible defaults, and change them later, so that practice fits me from the first day. |
| Persona(s) · user class | Aarav (P1), Daniel (P3); all personas · Local user |
| Priority · Release | P0 · R1 |
| Business value | High — a relevant first session and fast activation (BO-02; KPI-02) |
| Dependencies | US-054; decision D-17 |
| Traces | PRD-FR-010, PRD-FR-014 · SRS-FR-ONB-001, SRS-FR-ONB-002, SRS-FR-ONB-003, SRS-FR-ONB-004, SRS-FR-ONB-005, SRS-FR-ONB-010, SRS-FR-SET-001, SRS-FR-SET-002 · FS-02 (FS-10 for later edits) |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A new user | Onboarding starts | The steps follow SRS-FR-ONB-001 and every choice step has a pre-selected default and Continue |
| AC2 | The level and daily-time steps | They continue without changing anything | Intermediate and 10 minutes are stored (SRS-FR-ONB-003, -004) |
| AC3 | They select a goal | They continue | The profile's track is the mapped track (SRS-FR-ONB-002) |
| AC4 | The app is killed during onboarding | It is relaunched | They resume at the last incomplete step with their earlier choices kept |
| AC5 | Onboarding is complete | They change goal, level, daily time or reminder in Profile or Settings | The change is saved and no progress or history is reset (SRS-BR-025) |

**Edge cases:** (1) No default goal is defined (FS-02 Issue 3). (2) "≤ 5 short steps" versus nine stages (FS-02 Issue 6). (3) Saving a choice fails: they stay on the step (SRS-ERR-005). (4) Whether migrated legacy users see onboarding is not defined (FS-02 Issue 5).

#### US-009 — Understand privacy before granting the microphone

| Field | Value |
|---|---|
| Story | As Meera, I want to read in plain language where my voice goes before I am asked for the microphone, and still use the app if I say no, so that I can decide with confidence. |
| Persona(s) · user class | Meera (P2), Sofia (P4) · Local user |
| Priority · Release | P0 · R1 |
| Business value | High — informed consent underpins the privacy position and trust (BO-04, BO-01) |
| Dependencies | US-008, US-062 |
| Traces | PRD-FR-011 · SRS-FR-ONB-006, SRS-FR-ONB-007 · FS-02 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | The privacy step | It is shown | It states that recordings and transcripts stay on the phone before RECORD_AUDIO is requested |
| AC2 | They deny microphone permission | The denial returns | The impact is explained with Try again, and they can continue to Today with speaking steps disabled (SRS-ERR-001) |
| AC3 | Permission is permanently denied | The step is shown | Open settings replaces Try again; coming back with permission granted enables speaking |

**Edge cases:** (1) Granting permission never starts a recording (NFR-PRIV-001). (2) Permission revoked later in system settings: handled by US-023. (3) The privacy statement must match the tier actually in use, because T3a sends audio (SRS-FR-SET-006).

#### US-010 — Find out what speech support my phone has

| Field | Value |
|---|---|
| Story | As Hoang, who has an older phone, I want SpeakCraft to check what speech recognition my phone supports and tell me my options honestly, so that I know what feedback I will get. |
| Persona(s) · user class | Hoang (P5), Daniel (P3) · Local user |
| Priority · Release | P0 · R1 |
| Business value | High — honest capability status replaces today's false positives (FND-AI-05) (BO-01; KPI-06) |
| Dependencies | US-069, US-070, US-071, US-072; ADR-009; SPK-01; decision D-14 |
| Traces | PRD-FR-012 · SRS-FR-ONB-008, SRS-FR-SPCH-001 · FS-02, FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | The speech-check step | Tier detection completes | The tier-specific choice is shown: offline-pack download (where installable), online-transcription consent (no on-device recogniser), or the audio-only explanation |
| AC2 | Any tier | The result is shown | No status that has not been verified is displayed (SRS-FR-SPCH-003) |
| AC3 | They are offline | The check completes | Download and online options are labelled as needing a connection; the other outcomes are unaffected |

**Edge cases:** (1) The pack download is still running when they continue: the tier is re-evaluated after installation (SRS-FR-SPCH-003). (2) The download fails (FS-02 E6). (3) T1 audio-source piping is verified only at first use and may demote the phone to T2 (ARC-05 §3).

#### US-011 — Make my first attempt during onboarding

| Field | Value |
|---|---|
| Story | As Daniel, I want to record a short "Introduce yourself" answer at the end of onboarding and see real feedback, so that I know straight away whether SpeakCraft can help me. |
| Persona(s) · user class | Daniel (P3); all personas · Local user |
| Priority · Release | P0 · R1 |
| Business value | High — the first valid attempt is the activation event (BO-01; KPI-01, KPI-02) |
| Dependencies | US-009, US-010, US-018, US-019, US-024; SPK-01 |
| Traces | PRD-FR-013 · SRS-FR-ONB-009, SRS-FR-ONB-011 · FS-02 (FS-04, FS-05) |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Microphone granted, a T1 or T2 phone, no network | They record a valid introduction | A transcript and feedback are shown; the attempt is stored like any attempt and its metrics become the baseline |
| AC2 | The first attempt is Inconclusive | The result is shown | The reason and Retry are shown and no baseline is stored (SRS-BR-008) |
| AC3 | The first-attempt step | They choose Do this later | Onboarding completes and Today opens |
| AC4 | The first attempt is valid | They reach Today | Today counts as a streak day (SRS-BR-010) |

**Edge cases:** (1) How the baseline is set when the step is skipped is not defined (FS-02 Issue 8). (2) `onb-intro` allows 60 s while TPL-ANSWER allows 90 s † (FS-02 Issue 7). (3) Storage full (SRS-ERR-004). (4) The recogniser is busy: the attempt continues as T3b (SRS-ERR-002).

### 4.3 E-03 — CAP-03 Today — daily session (FS-03)

#### US-012 — Start one clear daily session

| Field | Value |
|---|---|
| Story | As Aarav, who has ten minutes before stand-up, I want Today to show one session with one Start button, so that I do not have to decide what to practise. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — an obvious, finishable default builds the habit (BO-02; KPI-03, KPI-04) |
| Dependencies | US-013, US-018, US-039, US-044 |
| Traces | PRD-FR-020 · SRS-FR-TODAY-001, SRS-FR-TODAY-004, SRS-FR-TODAY-011 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An onboarded user | They open Today | They see the greeting, one session card (steps, estimated minutes, one primary action), a streak summary and today's progress |
| AC2 | A 10-minute preference | The session is built | It has one warm-up (Read aloud or Listen & repeat), one track-matched main speaking task, one word task, and optionally one retry/focus task |
| AC3 | A step has a valid attempt, or its review set is finished | They return to Today | The step shows as complete; an Inconclusive attempt does not complete it (SRS-BR-009, SRS-BR-008) |
| AC4 | All steps are complete | Today is shown | A completion state lists what was practised and suggests one optional activity (SCR-TODAY-02) |

**Edge cases:** (1) Microphone not granted: speaking steps are disabled with a way to grant it (SRS-FR-ONB-007). (2) A T3b phone cannot pass word tasks (Issue 11). (3) What "Skip" means for step completion is not defined (FS-00 §6, FS-03).

#### US-013 — Get a plan built for me that stays stable today

| Field | Value |
|---|---|
| Story | As Daniel, I want today's session chosen from my goal, level, weakest measured skill, due words and recent practice, and to stay the same when I reopen the app, so that I can trust it and pick up where I left off. |
| Persona(s) · user class | Daniel (P3), Aarav (P1) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — relevant, predictable plans (BO-02, BO-03) |
| Dependencies | US-008, US-044, US-048; NFR-PERF-007 |
| Traces | PRD-FR-021 · SRS-FR-TODAY-002, SRS-FR-TODAY-003, SRS-FR-TODAY-005 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Today is first shown on a date | The plan engine runs | It builds the session for that dateKey from track, level, daily minutes, weakest measured metric family, due reviews, new-word goal, the last 14 days of attempts and the content version |
| AC2 | The same inputs and dateKey | The plan is built again | It is identical |
| AC3 | An unpractised alternative of the same template and track exists | Activities are chosen | No activity completed in the previous 7 days is chosen |
| AC4 | A plan exists for today | They close and reopen the app | The same plan is shown (SRS-BR-013) |

**Edge cases:** (1) A new user has no measured metrics, so "weakest" is undefined (FS-00 §6). (2) Time-zone change during the day: dateKey rules apply. (3) Content updated by an app update: attempts keep `activityId` + `contentVersion` (SRS-BR-026).

#### US-014 — Keep completed steps when the plan adapts

| Field | Value |
|---|---|
| Story | As Aarav, I want steps I have finished to stay finished when I change preferences during the day, so that my effort is never lost. |
| Persona(s) · user class | Aarav (P1), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — trust in progress (BO-03) |
| Dependencies | US-013, US-017 |
| Traces | PRD-FR-022 · SRS-FR-TODAY-006, SRS-FR-TODAY-007 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Two of four steps are complete | They change duration or a learning preference | Only the incomplete steps are regenerated |
| AC2 | A saved plan | It is regenerated for any reason | Completed steps are never removed or reordered (SRS-BR-013) |

**Edge cases:** (1) Changing level during the day resets nothing (SRS-BR-025). (2) Process death between a completion and the plan save (NFR-REL-004). (3) The new duration is shorter than the time already practised.

#### US-015 — Come back gently after missed days

| Field | Value |
|---|---|
| Story | As Meera, who sometimes misses several days because of interviews, I want a short, gentle session when I come back, so that I am not faced with a backlog. |
| Persona(s) · user class | Meera (P2), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — re-engagement after a lapse (BO-02; KPI-03) |
| Dependencies | US-013, US-044 |
| Traces | PRD-FR-023 · SRS-FR-TODAY-008 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Their last valid attempt was 2 or more local days ago | Today's plan is built | The session is min(chosen, 10) minutes, puts due reviews and a familiar template first, and adds no backlog (SRS-BR-014) |
| AC2 | They missed days | They open Progress | No history has been erased and the longest streak is kept (SRS-BR-011) |

**Edge cases:** (1) Only Inconclusive attempts in between do not reset the clock (SRS-BR-008). (2) Many overdue reviews: the set is capped at 10 (SRS-BR-027). (3) A 5-minute preference stays at 5.

#### US-016 — See a greeting, my streak and today's progress

| Field | Value |
|---|---|
| Story | As Hoang, I want a personal greeting and a compact view of my streak and today's progress when I open the app, so that I feel welcome and know where I stand. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — motivation and orientation (BO-02) |
| Dependencies | US-050, US-057 |
| Traces | PRD-FR-024 · SRS-FR-TODAY-001, SRS-FR-TODAY-010 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Their first ever visit to Today | It loads | The greeting is "Welcome, {name}" and never "welcome back" |
| AC2 | A later day | Today loads | The greeting is "Good morning/afternoon/evening, {name}" with one factual line, such as the streak or the last focus |
| AC3 | Any visit | Today loads | The streak and today's progress are shown compactly and come only from validated events (SRS-FR-PROG-005) |

**Edge cases:** (1) Onboarding never asks for a name (FS-02 Issue 4). (2) No history yet for the factual line. (3) Morning/afternoon/evening follows the device time zone.

#### US-017 — Swap a step or change today's duration

| Field | Value |
|---|---|
| Story | As Sofia, I want to swap a step for a similar one or change today's length, so that the session fits my time and energy. |
| Persona(s) · user class | Sofia (P4), Aarav (P1) · Local user, Signed-in user |
| Priority · Release | P2 · R1 |
| Business value | Low — convenience that can help completion (BO-02; KPI-04) |
| Dependencies | US-013, US-014 |
| Traces | PRD-FR-025, PRD-FR-026 · SRS-FR-TODAY-009, SRS-FR-TODAY-007 · FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An incomplete step | They tap Swap | It is replaced by the next-best candidate of the same template family |
| AC2 | They have swapped 3 times † today | They look at a step | No further swap is offered |
| AC3 | They change today's duration | The plan is regenerated | Only incomplete steps change |

**Edge cases:** (1) No alternative candidate exists. (2) PRD-FR-026 says "before starting", while SRS-FR-TODAY-007 also allows mid-day changes. The SRS wins. (3) A swap after the plan has been synced to another device (ADR-014 last-writer-wins for plans).

### 4.4 E-04 — CAP-04 Speaking practice session (FS-04)

#### US-018 — Prepare and record my answer with clear controls

| Field | Value |
|---|---|
| Story | As Meera, I want to see the prompt, take a moment to prepare, and record with one clear control that shows the time left, so that I can concentrate on speaking. |
| Persona(s) · user class | Meera (P2); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — recording is the heart of the core loop (BO-01) |
| Dependencies | US-009, US-069; SPK-01; NFR-PERF-004 |
| Traces | PRD-FR-030 · SRS-FR-SESS-001, SRS-FR-SESS-002, SRS-FR-SESS-003, SRS-FR-SESS-004 · FS-04 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A speaking step | The session player opens | It moves through Prompt → (Prepare) → Ready → Recording → Processing → Result |
| AC2 | An Answer-a-question prompt | Preparation starts | A 15 s † preparation timer runs and can be skipped (30 s † for Structured answer and Tell a story; none for the other templates) |
| AC3 | They are recording | The screen is shown | Elapsed time, remaining time, an input-level indicator and a single Stop control are visible, and TalkBack announces start and stop |
| AC4 | The template's maximum length is near (for example 90 s † for Answer a question) | 10 s remain | A warning appears and recording stops automatically at the limit |

**Edge cases:** (1) Microphone permission missing: the permission education screen (SCR-PERM-01) is shown. (2) Another recording is already active: only one may run app-wide (SRS-FR-SESS-014). (3) At 200 % font scale the Stop control stays reachable (NFR-A11Y-002).

#### US-019 — See what I said after I stop

| Field | Value |
|---|---|
| Story | As Aarav, I want my attempt processed automatically when I stop, and to see the transcript or the reason there is none, so that I can review exactly what I said. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — "see what you said" is the core promise (BO-01; KPI-05) |
| Dependencies | US-018, US-069; ADR-009; SPK-01; NFR-PERF-005 |
| Traces | PRD-FR-031 · SRS-FR-SESS-005, SRS-FR-SESS-006, SRS-FR-SPCH-009, SRS-FR-EVAL-013 · FS-04, FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | They tap Stop | Processing completes | The result shows the transcript with its tier label: "On-device", "Online (consented)" or "No transcript — audio-only" |
| AC2 | An attempt up to the template's maximum length | Recognition runs | The whole attempt is captured across recogniser segments and no text is duplicated on stop |
| AC3 | Evaluation takes longer than twice the NFR-PERF-005 budget | That limit is reached | The Error state offers Retry evaluation, and the audio is kept until they retry or leave |

**Edge cases:** (1) Recogniser busy or unavailable: the attempt falls back to T3b and they are told no transcript was available (SRS-ERR-002). (2) The database write fails: success is never shown (SRS-ERR-005). (3) A T3a network drop mid-attempt has no defined outcome (FS-00 §6, FS-04).

#### US-020 — Be told honestly when an attempt is inconclusive

| Field | Value |
|---|---|
| Story | As Hoang, I want to be told clearly when my attempt could not be evaluated and why, with a quick retry, so that I am never given a fake result or marked as failing because of a technical problem. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — honesty is the product's main differentiator (BO-01; KPI-06) |
| Dependencies | US-019; ARC-05 §5; SPK-01 |
| Traces | PRD-FR-032 · SRS-FR-SESS-007, SRS-FR-EVAL-001 · FS-04, FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An attempt shorter than 1.5 s, with too little voiced speech, or below the silence threshold | It is evaluated | The result is Inconclusive with the specific reason and Retry / Skip (SRS-BR-001, SRS-ERR-003) |
| AC2 | A transcript-required template with fewer recognised words than required (≥ 1 for Read/Repeat/word stages; ≥ 8 † for free speaking) | It is evaluated | The result is Inconclusive and no metric is shown |
| AC3 | An Inconclusive attempt | Progress is calculated | It counts as neither completion, failure, streak day nor practice time (SRS-BR-008) |
| AC4 | An Inconclusive attempt | Its result is shown | Its recording is deleted at once (SRS-BR-022), and the Inconclusive reason replaces the "Mistakes found" section (SRS-FR-EVAL-015) |

**Edge cases:** (1) "No match" from the recogniser gives Inconclusive and is not a capability failure (SRS-FR-SPCH-010). (2) The SRS lists four reasons; ARC-05 §5 adds TOO_FEW_WORDS (Issue 13). (3) How T3b handles transcript-required templates (FS-00 §6, FS-04).

#### US-021 — Try again and compare with my last attempt

| Field | Value |
|---|---|
| Story | As Daniel, I want to retry the same prompt and see my new result next to my previous one, so that I can tell whether the change I made worked. |
| Persona(s) · user class | Daniel (P3), Aarav (P1) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — retry-and-compare is the learning loop (BO-01, BO-03) |
| Dependencies | US-019, US-024 |
| Traces | PRD-FR-033 · SRS-FR-SESS-008 · FS-04 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A result | They tap Try again | A new attempt starts on the same activity |
| AC2 | The new attempt is valid and an earlier valid attempt exists | The result is shown | The three headline metrics are shown side by side with the previous valid attempt |
| AC3 | The most recent earlier attempt was Inconclusive | The comparison is built | It uses the last valid attempt only |

**Edge cases:** (1) No earlier valid attempt: no comparison is shown. (2) A metric is null in one of the two attempts: "Not available" (SRS-BR-002). (3) Attempts evaluated under different `evaluationVersion` values: comparison rules are not defined (Issue 14).

#### US-022 — Listen to my own recording and to a reference reading

| Field | Value |
|---|---|
| Story | As Sofia, I want to replay my recording after an attempt and, for read-aloud and repeat tasks, hear a reference reading at normal and slow speed, so that I can compare how I sound with how it should sound. |
| Persona(s) · user class | Sofia (P4), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — listening to yourself and to a model deepens feedback, especially for elementary learners (BO-01) |
| Dependencies | US-032, US-059, US-073; ADR-009, ADR-015; SPK-01 |
| Traces | PRD-FR-034, PRD-FR-036 · SRS-FR-SESS-009, SRS-FR-SESS-012 · FS-04 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A T1 or T3b phone and retention not set to "Delete after feedback" | The result is shown | Playback is offered |
| AC2 | A T2 or T3a phone | The result is shown | Playback is not offered and the reason is stated (ARC-05 §3) |
| AC3 | The retention period has passed | They open the attempt | Playback is not offered |
| AC4 | A Read aloud or Listen & repeat activity | The prompt is shown | Reference audio is offered at 1.0× and 0.75× in the selected English locale |
| AC5 | No text-to-speech engine is available | The prompt is shown | Reference-audio controls are hidden and "Reference audio isn't available on this device" is shown (SRS-ERR-010) |

**Edge cases:** (1) A T1 piping failure demotes the phone to T2, so no replay (ARC-05 §3, RSK-09). (2) The cleanup job deletes the file during playback. (3) After a backup restore the recording is not available (SEC-01 §6.5). (4) The speed control has a label for TalkBack (DES-07 A11Y-25).

#### US-023 — Stay safe when a recording is interrupted

| Field | Value |
|---|---|
| Story | As Aarav, whose phone sometimes rings during practice, I want an interrupted recording to stop safely with a clear message and a retry, so that I never get corrupt data or a wrong result. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — reliability and no data loss (BO-01; KPI-08) |
| Dependencies | US-018; NFR-REL-003 |
| Traces | PRD-FR-035 · SRS-FR-SESS-010, SRS-FR-SESS-011, SRS-FR-SESS-013, SRS-FR-SESS-014 · FS-04 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Recording is in progress | A phone call, audio-focus loss, backgrounding, permission revocation or full storage occurs | Capture stops, partial audio is discarded, no attempt is recorded, and the Interrupted state with Retry is shown |
| AC2 | Recording is in progress | They try to leave the player | They are asked to confirm, and the in-progress attempt is discarded if they leave |
| AC3 | Recording is in progress | The phone is rotated | Recording continues and the player state is kept |
| AC4 | Storage is full | Recording stops | The message links to storage settings (SRS-ERR-004) |

**Edge cases:** (1) The process dies during recording: no attempt exists on relaunch, and an orphan sweep removes the partial file (SEC-01 §7.1, RECOMMENDATION). (2) Navigating away from the player stops the recording (SRS-FR-SESS-014). (3) Audio-route changes such as a headset disconnecting are not listed in SRS-FR-SESS-010 (Issue 15).

### 4.5 E-05 — CAP-05 Honest feedback & evaluation (FS-05)

#### US-024 — See three relevant metrics, each labelled Measured or Estimated

| Field | Value |
|---|---|
| Story | As Sofia, I want to see the three metrics that matter for this kind of task, each labelled Measured or Estimated and with a plain-language band, so that I know how far to trust each number. |
| Persona(s) · user class | Sofia (P4); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — labelled, evidence-based metrics are the core of trusted feedback (BO-01; KPI-05) |
| Dependencies | US-019, US-020; ARC-05 §6–§7; SPK-01 |
| Traces | PRD-FR-040 · SRS-FR-EVAL-002, SRS-FR-EVAL-003, SRS-FR-EVAL-004, SRS-FR-EVAL-012, SRS-FR-EVAL-014 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A valid attempt | The result is shown | Exactly the three headline metrics for its template (ARC-05 §7) are shown, each with value, unit, plain-language band and target range |
| AC2 | Any displayed metric | It is rendered | It carries its fixed Measured or Estimated label (SRS-BR-003) |
| AC3 | A metric whose inputs are missing | The result is shown | It reads "Not available — {reason}" and no default value is substituted (SRS-BR-002) |
| AC4 | A Structured answer | It is evaluated | The framework elements detected (for example STAR Situation, Task, Action, Result) are shown as Estimated, with the cue phrases that triggered them |
| AC5 | The recogniser supplies no confidence values | The result is shown | No confidence is shown (SRS-BR-005) |

**Edge cases:** (1) T3b shows speaking time, long pauses and speech ratio (ARC-05 §7). (2) Plain-language bands are defined only for pace (FS-00 §6, FS-05). (3) Filler counts depend on the recogniser keeping "um" and "uh" (ARC-05 §6).

#### US-025 — Get one strength and one focus

| Field | Value |
|---|---|
| Story | As Aarav, I want one specific strength and one thing to focus on next time, quoting my own words where possible, so that I know exactly what to change. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — specific, actionable feedback (BO-01, BO-03) |
| Dependencies | US-024; ARC-05 §8 |
| Traces | PRD-FR-041 · SRS-FR-EVAL-005 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A valid attempt | Feedback is composed | Exactly one strength and one focus are shown, chosen by deterministic priority rules |
| AC2 | The focus concerns part of the transcript | It is shown | It quotes that part |
| AC3 | The same attempt inputs and evaluation version | Feedback is composed twice | It is identical (NFR-QUAL-006) |

**Edge cases:** (1) No transcript (T3b): the focus comes from delivery metrics. (2) A metric that is null, or Estimated with little evidence, is never praised (ARC-05 §8.2). (3) An Inconclusive attempt gets reason-specific retry advice instead (ARC-05 §8.2 priority 1). (4) The focus is also the first coach tip (SRS-FR-EVAL-016; US-026).

#### US-026 — See what went wrong and a coach tip after every attempt

| Field | Value |
|---|---|
| Story | As Aarav, I want to see exactly what went wrong in my answer, with the evidence, and a coach tip for fixing it after every attempt, so that I know precisely what to change next time. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — Product Owner direction (2026-11-21): concrete, evidence-backed correction after every submission is the heart of the coaching promise (BO-01, BO-03; KPI-05) |
| Dependencies | US-024, US-025, US-027, US-035 (technique tips in the content pack); SPK-02; decision D-04 |
| Traces | PRD-FR-047 · SRS-FR-EVAL-015, SRS-FR-EVAL-016 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A valid attempt of any activity type, including a technique exercise | The result is shown | A "Mistakes found" section lists each issue found by deterministic checks, each with its evidence (quoted span, count or value) and its Measured/Estimated label |
| AC2 | More than 3 issues were detected | The section is shown | The first 3 appear in ARC-05 §8.2 priority order, with "Show all" for the rest |
| AC3 | No issue was detected | The result is shown | The section says "Our checks didn't find issues in this attempt" with a one-line note on what the checks cover, and never claims the attempt was perfect |
| AC4 | A valid attempt | Coach tips are shown | They are the focus tip plus up to 2 more tips chosen deterministically from the content pack's technique tips whose trigger (issue code, metric band, template or technique) matches. Each links to its technique and, where provenance exists, to the source video. With no matching trigger, a general tip for the activity's technique is shown |
| AC5 | An Inconclusive attempt | The result is shown | The Inconclusive reason replaces "Mistakes found" (SRS-BR-008) |

**Edge cases:** (1) On T3b only delivery-based issues (pauses, speaking time) can be found; transcript-based checks say "Transcript needed for this check" (SRS-FR-SPCH-007). (2) Language issues come only from rules that pass SPK-02, at most 2 (SRS-FR-EVAL-006), and Estimated entries must not read as certain errors (Issue 28). (3) The same inputs always give the same mistakes and tips (NFR-QUAL-006); no generative or cloud model is used (D-04). (4) Issues are raised only beyond the metric's noise band and are shown in the ARC-05 §8.4 priority order. An activity that references no technique has no defined fallback tip (Issue 27).

#### US-027 — See only trustworthy language observations

| Field | Value |
|---|---|
| Story | As Meera, I want grammar or word-choice comments only when the app is very likely to be right, phrased as a possible issue with a suggestion, so that I am not misled by false corrections. |
| Persona(s) · user class | Meera (P2), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — avoids trust damage from false positives (BO-01; KPI-05; RSK-03) |
| Dependencies | US-024; SPK-02; NFR-QUAL-003 |
| Traces | PRD-FR-042 · SRS-FR-EVAL-006 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A valid attempt with a transcript | Language rules run | Only rules with measured precision ≥ 0.90 † on the evaluation corpus can produce observations |
| AC2 | Several rules fire | The result is shown | At most 2 observations appear, each phrased "Possible issue" with a suggested alternative |

**Edge cases:** (1) No rule passes SPK-02: no observations are shown, and this is not an error. (2) The rules must not depend on punctuation (ARC-05 §8.3). (3) T3b: no observations. (4) Observations also appear as entries under "Mistakes found" (SRS-FR-EVAL-015; Issue 28).

#### US-028 — No made-up overall score

| Field | Value |
|---|---|
| Story | As Daniel, I want no overall score on free-speaking answers, and a clear pass or fail against a stated threshold only for read-aloud and repeat, so that every number I see is real. |
| Persona(s) · user class | Daniel (P3); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — removes the fabricated scores found in the audit (FND-AI-08) (BO-01) |
| Dependencies | US-024; decision D-06 |
| Traces | PRD-FR-043 · SRS-FR-EVAL-007, SRS-FR-EVAL-008 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An Answer a question, Structured answer or Tell a story attempt | The result is shown | No overall score is displayed (SRS-BR-006) |
| AC2 | A Read aloud or Listen & repeat attempt | The result is shown | "Recognised as intended: N %" is shown with pass/fail against the 75 % † threshold (SRS-BR-007) and the list of words not recognised as intended |
| AC3 | Any result | Recognition agreement is shown | It is never called pronunciation or accent accuracy (SRS-BR-004) |

**Edge cases:** (1) N = 75 passes (≥). (2) T3b: "Transcript needed for this check" (SRS-FR-SPCH-007). (3) Threshold values are configurable and versioned (SRS-BR-007).

#### US-029 — Understand and rate my feedback

| Field | Value |
|---|---|
| Story | As Sofia, I want a "How we measured this" explanation for every metric, and to say whether a result was accurate and, if not, why, so that I can judge the feedback for myself and help check its quality. |
| Persona(s) · user class | Sofia (P4), Aarav (P1) · Local user, Signed-in user, Beta tester |
| Priority · Release | P1 · R1 |
| Business value | Medium — transparency builds trust; the rating feeds KPI-05 feedback accuracy (BO-01) |
| Dependencies | US-024; ARC-05 §10; ENT-13 FeedbackRating; decision D-09 / ADR-013 |
| Traces | PRD-FR-044, PRD-FR-045 · SRS-FR-EVAL-009, SRS-FR-EVAL-010 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A metric on a result | They open "How we measured this" | The feedback detail sheet (SCR-SESS-02) explains the method and its limits, for example that filler detection depends on the recogniser keeping "um" |
| AC2 | The metric catalogue has changed | Explanations are shown | They match the current catalogue entries (ARC-05 §10 single source) |
| AC3 | A result | They answer "Was this accurate?" | Yes or No is stored locally, with an optional reason: transcript wrong, metric wrong, or tip not useful |
| AC4 | They have rated a result | They reopen the attempt in history | Their rating is shown (SRS-FR-PROG-004) |

**Edge cases:** (1) Changing a rating later must leave the attempt unchanged; the canon stores ratings in a separate table, but the SRS says "stored with the attempt" (Issue 9). (2) Ratings are local only, so how KPI-05 is measured is unclear (Issue 10). (3) A metric that is Not available explains why. (4) TalkBack reads units in full (DES-07 A11Y-34).

#### US-030 — Know which rules evaluated an attempt

| Field | Value |
|---|---|
| Story | As Sofia, I want each attempt to record which version of the evaluation rules and content was used, so that comparisons over time stay fair and explainable. |
| Persona(s) · user class | Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | Medium — reproducible, explainable results (BO-03; NFR-QUAL-006) |
| Dependencies | US-024; ARC-05 §10 |
| Traces | PRD-FR-046 · SRS-FR-EVAL-011 · FS-05 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Any attempt | It is stored | `evaluationVersion` and `contentVersion` are stored with it |
| AC2 | An attempt evaluated under an earlier rule-set version | It is shown in history | Its stored results are shown unchanged (ENT-03 is immutable once evaluated) |

**Edge cases:** (1) Retired content stays readable in history (SRS-BR-026). (2) Whether trends and comparisons mix evaluation versions is not defined (Issue 14). (3) A content-pack update that changes technique tips does not change tips already shown for stored attempts.

### 4.6 E-06 — CAP-06 Practice library and technique curriculum (FS-06)

> **D-08 — DECISION (Product Owner, 2026-11-21): "original wording, link out".** Creator-derived techniques, exercises and tips are written in SpeakCraft's own words and examples. There are no verbatim transcripts and no creator name, likeness or branding. Each item links to its specific source video and timestamp ("Watch the original lesson"). Content is gathered by hand; there is no scraping, caption download or API ingestion (PRD-FR-056, PRD-FR-057, SRS-FR-PRAC-008, SRS-FR-PRAC-009, SRS-BR-028, ADR-011).

#### US-031 — Browse tracks and find the right activity

| Field | Value |
|---|---|
| Story | As Meera, I want to browse the practice library by track and filter by template and level, so that I can rehearse the situation I am preparing for. |
| Persona(s) · user class | Meera (P2), Aarav (P1), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — "let me rehearse the real situation" (UN-02) (BO-01) |
| Dependencies | US-034 |
| Traces | PRD-FR-050 · SRS-FR-PRAC-001, SRS-FR-PRAC-002, SRS-FR-PRAC-003 · FS-06 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Practice | It opens | The five tracks are listed with activity counts, and the Techniques collection can be reached (SRS-FR-PRAC-008) |
| AC2 | A track | It is opened | Activities can be filtered by template and level, and each card shows title, template, estimated minutes, level and "Practised" status |
| AC3 | An activity | It is opened | The detail shows its goal, a prompt preview, an explanation of the template, and Start |

**Edge cases:** (1) A filter that matches nothing shows an empty state with an action (DES-02 §2.3). (2) Per-activity level versus the minimum of 15 per track (FS-00 §6, FS-06). (3) Wide screens show list and detail side by side (DES-02 §7).

#### US-032 — Practise with the five R1 templates

| Field | Value |
|---|---|
| Story | As Daniel, I want different kinds of speaking task — read aloud, listen & repeat, answer a question, structured answer and tell a story — so that I build both clarity and fluency. |
| Persona(s) · user class | Daniel (P3), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the template set covers every track and technique exercise in R1 (BO-01) |
| Dependencies | US-018, US-022, US-024 |
| Traces | PRD-FR-051 · SRS-FR-SESS-002, SRS-FR-SESS-003, SRS-FR-EVAL-004 · FS-06 (FS-04, FS-05) |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An activity using any of the five templates | It starts | The player applies that template's preparation time and maximum recording length (SRS-FR-SESS-002, -003) |
| AC2 | A valid attempt | The result is shown | The headline metrics are the ones defined for its template (ARC-05 §7) |
| AC3 | A Read aloud or Listen & repeat attempt versus the other three templates | The result is shown | Pass/fail applies only to the first two; the other three show no overall score (SRS-BR-006, SRS-BR-007) |

**Edge cases:** (1) Role-play is not available in R1. (2) T3b adapts transcript-dependent templates (SRS-FR-SPCH-007). (3) There is no per-activity override for maximum length (FS-02 Issue 7).

#### US-033 — Count library practice toward progress and streak

| Field | Value |
|---|---|
| Story | As Aarav, I want practice I do from the library to count toward my progress and streak, so that extra practice is never wasted. |
| Persona(s) · user class | Aarav (P1), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — rewards real practice wherever it happens (BO-02, BO-03) |
| Dependencies | US-050, US-051 |
| Traces | PRD-FR-052 · SRS-FR-PRAC-004 · FS-06 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A library or technique activity | They complete a valid attempt | It is recorded exactly like a Today attempt |
| AC2 | That valid attempt is their first today | They open Progress | Today is a streak day and practice time includes it (SRS-BR-010, SRS-BR-018) |

**Edge cases:** (1) Inconclusive library attempts do not count (SRS-BR-008). (2) The Today session does not need to be finished for the streak (SRS-BR-012). (3) Whether a library attempt on an activity that is also in today's plan completes that Today step is not stated (Issue 12).

#### US-034 — Use curated, sourced content that works offline

| Field | Value |
|---|---|
| Story | As Hoang, I want all practice content built into the app, checked by hand and with its source recorded, so that it works without signal and is safe to use. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — offline use plus no legal exposure from unlicensed or unverifiable content (BO-04, BO-06; KPI-12) |
| Dependencies | DEP-02 content production; CNT-01 technique catalogue audit (in preparation); decision D-08 (DECISION 2026-11-21); ADR-011; ARC-03 §6 |
| Traces | PRD-FR-053, PRD-FR-057 · SRS-FR-PRAC-005, SRS-FR-PRAC-009 · FS-06. Business rule SRS-BR-028 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | The app is installed | The library loads offline | All activities and techniques load from the bundled, versioned content pack |
| AC2 | A content pack with schema errors, duplicate IDs or missing required fields | The build runs | The build fails and the pack is never shipped (SRS-ERR-008) |
| AC3 | The R1 content pack | Activities are counted | Each of the five tracks has at least 15 activities |
| AC4 | A technique, activity or tip record | The build validates the pack | It carries provenance (specific source video ID/URL and timestamp, technique name as taught, verified-by, verified-on) and `licenceStatus`; a creator-derived item with `licenceStatus = PENDING_REVIEW`, without verified provenance, or with provenance pointing at the channel root fails the build (SRS-FR-PRAC-009, SRS-BR-028) |
| AC5 | Creator-derived content in the pack | It is reviewed before release | It is original re-expression (`ORIGINAL_REEXPRESSION`): own wording and examples, no verbatim transcript text, and no creator name, likeness or branding (SRS-BR-028) |

**Edge cases:** (1) The 15 techniques, named frameworks, 30-day plan, 10 challenges and shadowing scripts already in the code point at the channel root. Each is re-verified against a specific video and timestamp and migrated into the pack, not deleted; items that fail stay out (canon R-6; WBS T-09.6; CNT-01). (2) Content is curated by hand; there is no scraping or caption download (ADR-011, NG-08). (3) Retired activities stay readable in history (SRS-BR-026). (4) Content written entirely in-house has no source video, so it has no link (SRS-FR-PRAC-008 "where provenance exists"; Issue 30).

#### US-035 — Learn a named speaking technique and practise it

| Field | Value |
|---|---|
| Story | As Meera, I want to learn a named speaking technique and practise it with an exercise, so that I can apply it in my interviews and at work. |
| Persona(s) · user class | Meera (P2), Aarav (P1), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the Product Owner's stated basis for the curriculum (PRD-FR-056), delivered through the honest core loop (BO-01) under the decided D-08 rules (BO-06) |
| Dependencies | US-031, US-032, US-034; decision D-08 (DECISION 2026-11-21); DEP-02 |
| Traces | PRD-FR-056 (with PRD-FR-050, -052, -054) · SRS-FR-PRAC-008 (with SRS-FR-PRAC-004, -006) · FS-06 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Practice (SCR-PRAC-01) | They open the Techniques collection and choose a technique | Technique detail (SCR-PRAC-04) shows its name, one-line idea, why it works, steps, tips, the linked exercises/activities with a single "Practise it" primary action, and "Watch the original lesson" where provenance exists (SRS-FR-PRAC-008) |
| AC2 | A technique's exercise | They tap "Practise it" | It runs through one of the five templates and is evaluated only with that template's labelled metrics, mistakes and coach tips; no technique-specific score is invented (SRS-BR-002, SRS-BR-003, SRS-BR-006; US-026) |
| AC3 | A valid attempt on a technique exercise | Progress is calculated | It counts toward progress and streak like any library attempt (SRS-FR-PRAC-004) |
| AC4 | Any technique screen | It is shown | It uses SpeakCraft's own wording and shows no creator name, likeness, branding or verbatim transcript (SRS-BR-028) |
| AC5 | A technique exercise | They save it | It appears under Saved (SRS-FR-PRAC-006) |

**Edge cases:** (1) Offline: the technique and its exercises work; only the video link needs a connection (US-036). (2) Frameworks such as STAR or PREP use Structured-answer detection, labelled Estimated (SRS-FR-EVAL-014). (3) PRD-FR-056 says techniques "are used by Today's plan", while SRS-FR-PRAC-008 says the plan engine "may" select by technique (Issue 29). (4) Delivery techniques (for example pausing for emphasis) can only be judged with the existing metrics. ARC-05 §8.4 lists candidate acoustic metrics that are not in R1 until validated, plus a self-reported self-check for aspects audio cannot show (Issue 27).

#### US-036 — Watch the original lesson a technique comes from

| Field | Value |
|---|---|
| Story | As Daniel, I want to watch the original lesson a technique comes from, so that I can see it taught in context and learn more. |
| Persona(s) · user class | Daniel (P3), Meera (P2) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | Medium — gives context and credit through a link-out, which is the decided lawful basis for using creator material (BO-06; KPI-12) |
| Dependencies | US-034, US-035; decision D-08 (DECISION 2026-11-21); ADR-011 |
| Traces | PRD-FR-057 · SRS-FR-PRAC-009 · FS-06. Business rule SRS-BR-028 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A technique, exercise or tip with verified provenance | It is shown | A "Watch the original lesson" link is offered |
| AC2 | They tap the link | It opens | The specific source video opens at its timestamp in the YouTube app or the browser through an outbound intent, with no API key, scraping or embedded download (SRS-FR-PRAC-009) |
| AC3 | The link and its surroundings | They are shown | No creator name, likeness or branding and no verbatim transcript text appear (SRS-BR-028) |

**Edge cases:** (1) Offline: the link cannot load; the expected message is not defined (Issue 30). (2) No app on the phone can open the link (Issue 30). (3) The video is later removed or made private; how often provenance is re-checked is not defined (Issue 30). (4) Checking for a handler before opening the link needs package-visibility `<queries>`, which SRS-FR-PLAT-002 does not list (Issue 30).

#### US-037 — Save activities for later

| Field | Value |
|---|---|
| Story | As Meera, I want to save activities I plan to rehearse before an interview, so that I can find them quickly later. |
| Persona(s) · user class | Meera (P2) · Local user, Signed-in user |
| Priority · Release | P2 · R1 |
| Business value | Low — convenience (BO-02) |
| Dependencies | US-031; ENT-12 SavedActivity |
| Traces | PRD-FR-054 · SRS-FR-PRAC-006 · FS-06 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An activity | They tap Save | It appears under the "Saved" filter; tapping again removes it |
| AC2 | Saved activities | The app restarts offline | The saved list is unchanged |

**Edge cases:** (1) Whether saved activities sync is not defined (FS-00 §6, FS-06). (2) Saved activities are missing from the SRS §9 data categories that export and deletion must cover (Issue 9). (3) A saved activity is retired by a content update (SRS-BR-026).

#### US-038 — (R2) Rehearse a scripted role-play

| Field | Value |
|---|---|
| Story | As Meera, I want to rehearse a multi-turn interview where the other person's lines are spoken aloud and each of my replies is evaluated, so that I can practise real back-and-forth. |
| Persona(s) · user class | Meera (P2), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P1 · R2 |
| Business value | Medium — replaces the retired conversation simulator honestly (BO-01) |
| Dependencies | US-022, US-024; R1 exit |
| Traces | PRD-FR-055 · SRS-FR-PRAC-007 · FS-06 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A role-play activity | It runs | Each partner line is shown and spoken via TTS, and each user turn is recorded and evaluated as an Answer-a-question attempt with turn-specific expected elements |
| AC2 | No TTS engine | Role-play runs | Partner lines are shown as text only (SRS-ERR-010) |

**Edge cases:** (1) Short replies versus the 8-word evidence gate (FS-00 §6, FS-06). (2) An interruption in the middle of a scenario (SRS-FR-SESS-010).

### 4.7 E-07 — CAP-07 Words — vocabulary lifecycle (FS-07)

#### US-039 — Learn a new word

| Field | Value |
|---|---|
| Story | As Daniel, I want to see a new word's meaning, examples and pronunciation in one place, so that I understand it before I try to say it. |
| Persona(s) · user class | Daniel (P3), Meera (P2), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the entry point of the vocabulary lifecycle (BR-010) (BO-02, BO-03) |
| Dependencies | US-046, US-022 |
| Traces | PRD-FR-060 · SRS-FR-WORD-001, SRS-FR-WORD-002 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A new word | They open Learn | The word, IPA (if the content has it), part of speech, simple meaning, 2 examples and common collocations are shown, with TTS pronunciation |
| AC2 | Learn | They tap "I've read this" | Stage 1 is complete and the word's status is Learning (SRS-FR-WORD-001) |

**Edge cases:** (1) TTS unavailable (SRS-ERR-010). (2) A word without IPA. (3) Stages must be passed in order (SRS-BR-015).

#### US-040 — Say it and use it in given sentences

| Field | Value |
|---|---|
| Story | As Hoang, I want to say a new word and then read it in two example sentences, with a clear result each time, so that I can use the word aloud with confidence. |
| Persona(s) · user class | Hoang (P5), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — moves words into spoken use (BO-02, BO-03) |
| Dependencies | US-039, US-019, US-020, US-042; SPK-01 |
| Traces | PRD-FR-060, PRD-FR-062 · SRS-FR-WORD-003, SRS-FR-WORD-004, SRS-FR-WORD-006, SRS-FR-WORD-007 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Stage 2 Say it | They record the word (5 s maximum) | The stage passes if the recogniser output contains the word or an accepted inflection with a match score ≥ 75 † (SRS-BR-007) |
| AC2 | Stage 3 Use it | They read both sentences | The stage passes if recognised-as-intended is ≥ 75 † for both |
| AC3 | There is no usable recognition output | A stage check is evaluated | The result is Inconclusive, not a failure (SRS-BR-008) |
| AC4 | They have failed a stage 3 times | The next attempt is offered | An easier variant (shorter sentence, slower reference) is offered and they can continue later |

**Edge cases:** (1) T3b phones cannot pass these stages (Issue 11). (2) The caveat "Say it checks that you were understood, not your accent" is shown (ARC-05 §9). (3) Replaying a passed stage never lowers the word's status (SRS-BR-015).

#### US-041 — Make my own sentence and complete the word

| Field | Value |
|---|---|
| Story | As Aarav, I want to say my own sentence using the word and have the word marked as learned when I succeed, so that it becomes part of my active vocabulary. |
| Persona(s) · user class | Aarav (P1), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the step that turns a known word into a used word (BO-03) |
| Dependencies | US-040, US-042, US-044, US-027; SPK-02 |
| Traces | PRD-FR-060, PRD-FR-062, PRD-FR-063 · SRS-FR-WORD-005, SRS-FR-WORD-008 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Stage 4 Make your own | They record an original sentence | It passes only if the target word (or an inflection) is present, the sentence has ≥ 5 words, there is ≤ 1 high-precision grammar observation and the composite is ≥ 70 † (ARC-05 §9) |
| AC2 | Stage 4 passes | The result is stored | The word is marked Learned and its first review is scheduled per SRS-BR-016 (first interval 1 day) |
| AC3 | A valid stage-4 attempt | The streak is calculated | It counts as a speaking attempt for a streak day (SRS-BR-010) |

**Edge cases:** (1) An Inconclusive stage-4 attempt is not a failure (SRS-FR-WORD-006). (2) The word is used, but in a 4-word fragment. (3) A pass on the easier variant offered after 3 failures (SRS-FR-WORD-007).

#### US-042 — Know what went wrong in a word attempt and how to fix it

| Field | Value |
|---|---|
| Story | As Hoang, I want every word attempt to tell me whether I passed, what exactly went wrong and one tip for the next try, so that I can fix it quickly. |
| Persona(s) · user class | Hoang (P5), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — PRD-FR-047 applies to word stages and reviews; precise correction speeds the lifecycle (BO-03) |
| Dependencies | US-040, US-041, US-044, US-026 |
| Traces | PRD-FR-047, PRD-FR-062 · SRS-FR-WORD-014 (applies SRS-FR-EVAL-015, -016) · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A submitted word-stage or review attempt | The result is shown | It states pass, fail or Inconclusive |
| AC2 | Mistakes were detected | The result is shown | The specific mistakes are listed with evidence, for example word not recognised, wrong form recognised as "{heard}", target word missing, sentence too short, possible grammar issue |
| AC3 | Any submitted word attempt that is not Inconclusive | The result is shown | One coach tip for the next try is given |
| AC4 | An Inconclusive word attempt | The result is shown | The Inconclusive reason is shown and it is not counted as a fail (SRS-BR-008) |

**Edge cases:** (1) "Recognised as '{heard}'" quotes the recogniser, which may itself be wrong; the "understood, not your accent" caveat applies (ARC-05 §9, SRS-BR-004). (2) A stage-4 pass with one grammar observation still lists it. (3) SRS-FR-WORD-014 gives "one coach tip", while SRS-FR-EVAL-016, which it says applies, allows up to three (Issue 31). (4) T3b: word checks need a transcript (Issue 11).

#### US-043 — Get new words and due reviews in Today

| Field | Value |
|---|---|
| Story | As Meera, I want my daily new-word goal and any due reviews to appear in today's session, so that vocabulary practice happens without extra planning. |
| Persona(s) · user class | Meera (P2); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — joins vocabulary to the daily habit (BO-02) |
| Dependencies | US-012, US-013, US-044 |
| Traces | PRD-FR-061 · SRS-FR-TODAY-002, SRS-FR-TODAY-004 · FS-07, FS-03 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Reviews are due | Today's plan is built | The word task includes a review set of up to 10 words (SRS-BR-027) |
| AC2 | No reviews are due and the new-word goal is above 0 | The plan is built | The word task is a new-word stage |
| AC3 | Default settings | The new-word goal is applied | It is 2 per day † (range 0–5; SRS-BR-027) |

**Edge cases:** (1) A goal of 2 words a day versus one word task per session (FS-00 §6, FS-03). (2) Goal set to 0. (3) No unstarted words left for the user's level and track. (4) No SRS-FR lets the user change the goal (Issue 7).

#### US-044 — Review words by speaking on a spaced schedule

| Field | Value |
|---|---|
| Story | As Daniel, I want to review learned words by recalling them aloud at growing intervals, so that I remember them when I need them. |
| Persona(s) · user class | Daniel (P3), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — spaced spoken recall makes learning last (BR-010) (BO-03) |
| Dependencies | US-041, US-042, US-043 |
| Traces | PRD-FR-063 · SRS-FR-WORD-009, SRS-FR-WORD-008 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Words are due | They start a review set | It holds up to 10 † due words, mixing spoken recall ("Say the word that means …") and sentence use |
| AC2 | A review outcome | It is saved | The schedule updates per SRS-BR-016: grade 4 first-try pass, 3 pass after hint or retry, 1 fail; a fail resets the interval to 1 day |
| AC3 | A word's interval reaches ≥ 21 days † | The schedule updates | The word is Mastered; a failed review returns a Mastered word to Reviewing (SRS-BR-017) |
| AC4 | They finish the review set | They return to Today | The word step is complete (SRS-BR-009) |

**Edge cases:** (1) An Inconclusive recall is not graded as a fail (SRS-BR-008); the grade it should receive is not defined (Issue 25). (2) Due dates use the local dateKey, so a time-zone change shifts nothing already recorded (SRS-BR-016). (3) Leaving in the middle of a set: whether partial outcomes are kept is not defined (Issue 25). (4) Each review item shows its result, mistakes and tip (US-042).

#### US-045 — Browse, search and filter my words

| Field | Value |
|---|---|
| Story | As Aarav, I want to browse my words by status and search or filter them, so that I can find a word and see where it is in its lifecycle. |
| Persona(s) · user class | Aarav (P1), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — visibility of vocabulary progress (BO-03) |
| Dependencies | US-046 |
| Traces | PRD-FR-064 · SRS-FR-WORD-010 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Words | They choose a filter | New, Learning, Learned, Due and Mastered each show the matching words |
| AC2 | The search field | They type | Matching words are listed, and level and track filters narrow the list |

**Edge cases:** (1) No matches shows an empty state with an action. (2) The filter list leaves out the "Reviewing" status defined in SRS-FR-WORD-001 (Issue 23). (3) Long lists must meet NFR-PERF-008.

#### US-046 — Start with a ready-made word bank

| Field | Value |
|---|---|
| Story | As Hoang, I want a ready set of useful words for my level and goal, so that I can start learning straight away. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the lifecycle needs content from day one, and that content must be lawful (BO-02, BO-06; KPI-12) |
| Dependencies | DEP-02; decision D-08 |
| Traces | PRD-FR-065 · SRS-FR-WORD-011 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | The R1 content pack | Entries are counted | There are ≥ 300 words, each with level, track tags, meaning, examples and licensing provenance |
| AC2 | An entry without required fields or provenance | The build validates content | The build fails (SRS-ERR-008) |

**Edge cases:** (1) Stage checks need explicit inflection lists in the content (ARC-05 §9). (2) Uneven coverage across levels and tracks. (3) Word examples drawn from creator material follow D-08 and SRS-BR-028 (own wording, specific-video provenance).

#### US-047 — (R2) Keep a word notebook and take a quiz

| Field | Value |
|---|---|
| Story | As Sofia, I want to save words from my transcripts and feedback into a notebook with notes and tags, and test myself with a quick quiz clearly labelled as a quiz, so that I build and check my own vocabulary. |
| Persona(s) · user class | Sofia (P4), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P1 (notebook, PRD-FR-066) · P2 (quiz, PRD-FR-067) · R2 |
| Business value | Medium — personal vocabulary from real speech, plus self-testing (BO-03) |
| Dependencies | US-019, US-046; ENT-15 NotebookEntry, ENT-17 QuizAttempt; R1 exit |
| Traces | PRD-FR-066, PRD-FR-067 · SRS-FR-WORD-012, SRS-FR-WORD-013 · FS-07 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A transcript or feedback item | They save a word | It is added to the notebook, where they can add notes and tags, edit, delete, favourite and search |
| AC2 | No network | They edit the notebook | Changes are saved locally (SRS-FR-DATA-001) |
| AC3 | The word bank and a seed | A quiz is generated | The same seed gives the same question set (meaning, fill-in-the-blank, sentence choice, listening recognition) |
| AC4 | The quiz screen | It is shown | It is labelled "Quiz" and never "AI" (BRU-01) |

**Edge cases:** (1) Notebook entries taken from transcripts are sensitive; whether they sync is not defined (D-16). (2) Listening questions on a phone without TTS (SRS-ERR-010). (3) Whether quiz results affect the review schedule is not defined.

### 4.8 E-08 — CAP-08 Progress & streak (FS-08)

#### US-048 — See trends in my measured metrics

| Field | Value |
|---|---|
| Story | As Sofia, I want to see how my measured metrics change over time, with an honest indication of noise, so that I know whether I am really improving. |
| Persona(s) · user class | Sofia (P4), Aarav (P1) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — evidence-based improvement (BO-03; KPI-07) |
| Dependencies | US-024, US-051 |
| Traces | PRD-FR-070 · SRS-FR-PROG-001, SRS-FR-PROG-002 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Fewer than 3 valid attempts in a template family | They open Progress | No trend is shown for that family and metric (SRS-BR-024) |
| AC2 | 3 or more valid attempts | A trend card is shown | It is per template family (Read/Repeat; Answer/Structured/Story) and metric, with the noise band |
| AC3 | A change smaller than the noise band | The trend is summarised | It says "no clear change" (SRS-BR-024) |
| AC4 | A trend chart | It is used with TalkBack | A text summary and a list alternative are available (DES-07 A11Y-12) |

**Edge cases:** (1) Inconclusive attempts are excluded. (2) Some attempts have null metrics (T3b). (3) Mixing evaluation versions (Issue 14).

#### US-049 — Review past attempts and replay them

| Field | Value |
|---|---|
| Story | As Meera, I want to look back at earlier attempts with their transcript, metrics, feedback and recording, so that I can see how my answers have changed. |
| Persona(s) · user class | Meera (P2), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — visible evidence of change (BO-03, BO-01) |
| Dependencies | US-022, US-059 |
| Traces | PRD-FR-071 · SRS-FR-PROG-003, SRS-FR-PROG-004 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | History | It opens | Attempts are listed newest first with date, activity, template, headline metrics, status (Valid/Inconclusive) and whether playback is available |
| AC2 | An attempt | It is opened | Transcript, all metrics with labels, feedback, accuracy rating and playback (if the recording is kept) are shown |
| AC3 | An attempt whose recording has expired or been cleared | It is opened | Playback is not offered and the other details remain |

**Edge cases:** (1) Attempts synced from another device have no transcript or recording (SRS-FR-DATA-006) and say so. (2) Retired content stays readable (SRS-BR-026). (3) "Try again" from attempt detail opens the session player (DES-02 §3).

#### US-050 — Keep one honest streak

| Field | Value |
|---|---|
| Story | As Daniel, I want one streak that counts the days I actually spoke, plus my longest streak, a weekly calendar and milestones, so that I stay motivated by real effort. |
| Persona(s) · user class | Daniel (P3); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — replaces four inconsistent streaks with one honest one (BO-02, BO-05; KPI-03) |
| Dependencies | US-051 |
| Traces | PRD-FR-072 · SRS-FR-PROG-001, SRS-FR-PROG-006 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A dateKey with ≥ 1 valid speaking attempt from any source (Today, Practice, word stages 2–4, spoken reviews) | The streak is calculated | That date is a streak day (SRS-BR-010) |
| AC2 | The last streak day was before yesterday | The streak is calculated | The current streak is 0 and the longest streak is unchanged (SRS-BR-011) |
| AC3 | They complete a valid attempt but not the whole session | The streak is calculated | The day still counts (SRS-BR-012) |
| AC4 | The current streak reaches 3, 7, 14, 30, 60 or 100 days | That happens | A one-time celebration is shown that respects reduced motion |

**Edge cases:** (1) Time-zone change: days use the dateKey recorded at attempt time (SRS-BR-011). (2) A user who cannot complete speaking steps (DES-07 OQ-A11Y-1). (3) Facts from two devices are combined before the streak is calculated (ADR-014).

#### US-051 — Trust my practice statistics

| Field | Value |
|---|---|
| Story | As Sofia, I want practice time and counts calculated only from real, valid practice, so that the numbers mean something. |
| Persona(s) · user class | Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — statistics from validated events only (BR-011) (BO-03) |
| Dependencies | US-065; ADR-014 |
| Traces | PRD-FR-073 · SRS-FR-PROG-005 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Any navigation, screen visit or tap | Statistics are calculated | No counter changes (SRS-FR-PROG-005) |
| AC2 | Valid attempts today | Practice time is shown | It equals the sum of their recording durations, shown as minutes speaking (SRS-BR-018) |
| AC3 | The same attempt or event arrives twice (for example through sync) | Statistics are calculated | It is counted once (SRS-BR-020) |

**Edge cases:** (1) Inconclusive attempts are excluded (SRS-BR-008). (2) After Clear history, statistics are recalculated from what remains. (3) Cloud totals are never trusted; aggregates are calculated locally (ADR-014).

#### US-052 — See my word progress at a glance

| Field | Value |
|---|---|
| Story | As Hoang, I want to see how many words are at each stage and how many are due today, so that I know where my vocabulary stands. |
| Persona(s) · user class | Hoang (P5), Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — vocabulary progress made visible (BO-03) |
| Dependencies | US-044 |
| Traces | PRD-FR-074 · SRS-FR-PROG-007 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Word progress exists | They open Progress | Counts by status and words due today are shown |
| AC2 | A word changes status | They next open Progress | The counts reflect the change |

**Edge cases:** (1) No words started yet. (2) Word progress has just been cleared (SRS-FR-SET-005).

#### US-053 — (R2) Compare check-ins over time

| Field | Value |
|---|---|
| Story | As Sofia, I want to repeat the same benchmark task every few weeks and compare the results, so that I can see long-term improvement. |
| Persona(s) · user class | Sofia (P4), Meera (P2) · Local user, Signed-in user |
| Priority · Release | P1 · R2 |
| Business value | Medium — the measure behind KPI-07 (BO-03) |
| Dependencies | US-011, US-048; R1 exit |
| Traces | PRD-FR-075 · SRS-FR-PROG-008 · FS-08 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | 14–28 days † have passed since the last check-in | Today's plan is built | The check-in activity (the same prompt as the baseline for the track) is offered |
| AC2 | Two or more check-ins | They are viewed | Results are plotted against earlier check-ins |

**Edge cases:** (1) The track changed after the baseline. (2) The baseline was skipped (FS-02 Issue 8).

### 4.9 E-09 — CAP-09 Reminders & inbox (FS-09)

#### US-054 — Turn on one daily reminder, asked for permission in context

| Field | Value |
|---|---|
| Story | As Aarav, I want one practice reminder at a time I choose, only if I turn it on, and to be asked for notification permission only when I do, so that I remember to practise without being nagged. |
| Persona(s) · user class | Aarav (P1), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — supports practice frequency while respecting control (BO-02; KPI-03) |
| Dependencies | US-008; ADR-005; decision D-12 |
| Traces | PRD-FR-080, PRD-FR-083 · SRS-FR-REM-001, SRS-FR-REM-004, SRS-FR-REM-005, SRS-FR-REM-006, SRS-FR-ONB-005 · FS-09 (FS-02) |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Android 13+ | They choose "Remind me at [time]" (default 19:00 †) | The notification permission request is shown; choosing "Not now" shows no request and posts no reminders |
| AC2 | Reminders are on and permission is granted | The chosen time arrives | One reminder is posted near that local time on the "Practice reminders" channel |
| AC3 | Permission is denied | They open Settings → Reminders | The blocked state and a link to system settings are shown, and they are not asked again |
| AC4 | The phone restarts, the time zone changes or the preference changes | Scheduling is checked | The reminder is rescheduled |
| AC5 | R1 | Any notification is posted | It is a practice reminder; no other category, including update notices, is posted (SRS-FR-REM-004) |

**Edge cases:** (1) Quiet hours 22:00–07:00 † apply unless they chose a time in that window (SRS-BR-019). (2) Battery optimisation delays background work. (3) After sign-out with Remove, reminders are cancelled (SEC-01 §6.4, RECOMMENDATION). (4) On Android below 13 there is no runtime permission prompt.

#### US-055 — Receive reminders that respect me

| Field | Value |
|---|---|
| Story | As Meera, I want the reminder to mention due word reviews, to come at most once a day, and not to come at all if I have already practised, so that every reminder is worth reading. |
| Persona(s) · user class | Meera (P2); all personas · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — "remind me, but don't nag me" (UN-07) (BO-02) |
| Dependencies | US-054, US-044 |
| Traces | PRD-FR-081, PRD-FR-082 · SRS-FR-REM-002, SRS-FR-REM-003 · FS-09 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A valid attempt exists for today's dateKey | The reminder time arrives | No reminder is posted (SRS-BR-019) |
| AC2 | Word reviews are due | The reminder is posted | Its text mentions them |
| AC3 | A reminder | They tap it | Today opens |
| AC4 | Default settings | A day passes | At most one reminder has been posted (SRS-BR-019) |

**Edge cases:** (1) Only Inconclusive attempts today: the reminder is still posted (SRS-BR-008). (2) "At most one per day by default" implies a non-default option that is not defined (Issue 19). (3) Practice on another device that has not synced yet.

#### US-056 — (R2) Read learning messages in an inbox

| Field | Value |
|---|---|
| Story | As Daniel, I want milestone and weekly-summary messages kept in an inbox I can mark as read or delete, so that I can look back at what I have achieved. |
| Persona(s) · user class | Daniel (P3) · Local user, Signed-in user |
| Priority · Release | P2 · R2 |
| Business value | Low — reinforcement of milestones (BO-02) |
| Dependencies | US-050; ENT-16 InboxMessage; R1 exit |
| Traces | PRD-FR-084 · SRS-FR-REM-007, SRS-FR-PROG-006 · FS-09 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Learning messages exist | They open the inbox from Today's bell | Messages show read/unread state and can be marked all read or deleted; Today's top bar shows an unread badge |
| AC2 | Any inbox message | It is listed | It is never a software-update message |

**Edge cases:** (1) A milestone reached on another device. (2) Deleting a message does not affect the streak.

### 4.10 E-10 — CAP-10 Profile, settings, privacy & data rights (FS-10)

#### US-057 — View and edit my profile

| Field | Value |
|---|---|
| Story | As Aarav, I want to see and edit my name, level, goal and daily minutes and see my key statistics, so that my profile reflects me. |
| Persona(s) · user class | Aarav (P1); all personas · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — personal relevance and control (BO-02) |
| Dependencies | US-008, US-051 |
| Traces | PRD-FR-090 · SRS-FR-SET-001 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Profile | It opens | Display name, level, track/goal, daily minutes, member-since date and key statistics are shown |
| AC2 | They change level or track | They save | No progress or history is reset (SRS-BR-025), and only today's incomplete steps may be regenerated (SRS-FR-TODAY-007) |

**Edge cases:** (1) "Key statistics" are not defined (Issue 22). (2) An empty display name. (3) Signed-in edits on two devices: last writer wins (SRS-BR-021).

#### US-058 — Find every setting in a predictable place

| Field | Value |
|---|---|
| Story | As Hoang, I want settings grouped into clear sections, so that I can find what I need quickly. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | Medium — simplicity after the sprawl of the current app (BO-05) |
| Dependencies | US-057 |
| Traces | PRD-FR-091 · SRS-FR-SET-002 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Settings | It opens | The sections are Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account and About |
| AC2 | Any primary destination | They tap the avatar | Profile opens with a route to Settings (DES-02 §2.2) |

**Edge cases:** (1) What the Learning section contains is not defined in the SRS (Issue 7). (2) At 200 % font scale, section rows wrap without clipping (NFR-A11Y-002).

#### US-059 — Control speech and recording privacy

| Field | Value |
|---|---|
| Story | As Meera, I want to see and control how my speech is recognised and how long my recordings are kept, so that my voice is handled the way I choose. |
| Persona(s) · user class | Meera (P2), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — "keep my voice private" (UN-08) (BO-04) |
| Dependencies | US-069, US-070, US-071, US-073; ADR-009, ADR-015; decisions D-10, D-24 |
| Traces | PRD-FR-092 · SRS-FR-SET-003, SRS-FR-DATA-009 · FS-10, FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Speech & privacy | It opens | It shows tier and status, recognition language, offline-pack status and install (where supported), the online-transcription consent toggle (T3 devices), the recording-retention selector and a link to privacy information |
| AC2 | The retention selector | It opens | The options are Delete after feedback, 7 days, 30 days (default †, D-10) and Keep until I delete (SRS-BR-022) |
| AC3 | Kept recordings | The daily cleanup job runs | Recordings older than the retention period are deleted from `noBackupFilesDir/recordings/<profileId>/` |

**Edge cases:** (1) A shorter retention applies to existing recordings, a longer one only to new recordings (SEC-01 §7.1, RECOMMENDATION). (2) The D-24 option "Delete transcripts when recordings expire" has no SRS-FR (Issue 18). (3) The phone is off for days: an app-start sweep catches up (SEC-01 §7.1, RECOMMENDATION).

#### US-060 — Export all my data

| Field | Value |
|---|---|
| Story | As Sofia, I want to export all my personal data as a machine-readable file, so that I have my own copy and can take it elsewhere. |
| Persona(s) · user class | Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — a data-rights obligation (BR-007) (BO-04; KPI-09) |
| Dependencies | ARC-03 §10; decision D-22 |
| Traces | PRD-FR-093 · SRS-FR-SET-004 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Data & privacy | They choose Export | A JSON file (ARC-03 §10 schema) with profile, preferences, attempts (metrics, and transcripts where present), word progress, plans and events is produced and shared through the system share sheet (SRS-FR-SET-004) |
| AC2 | The export screen | It is shown | "Include my recordings (ZIP)" is a separate option, off by default, with a warning that recordings contain their voice (D-22 consolidated recommendation; SRS-FR-SET-004 "optionally") |
| AC3 | No account and no network | They export | The export completes from local data |
| AC4 | The exported file | It is checked against SRS §9 | It covers 100 % of the personal-data categories (SRS-PRIV-003, NFR-PRIV-004) |

**Edge cases:** (1) The D-22 alternative (SEC-01 SD-04: JSON only in R1) still has to be ruled out by the Product Owner, and the share-sheet versus file-picker difference is still open (Issue 17). (2) Signed-in and online: SEC-01 §9.1 proposes a pull first and a "synced up to" line (RECOMMENDATION). (3) Not enough storage to write the file: no SRS-ERR covers this (Issue 17). (4) The export warns that it contains transcripts (SEC-01 §7.4).

#### US-061 — Clear selected learning history

| Field | Value |
|---|---|
| Story | As Meera, I want to clear just my recordings, my attempts, my word progress or everything, after a clear warning, so that I decide what is kept. |
| Persona(s) · user class | Meera (P2), Sofia (P4) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — a data-rights obligation (BR-007) (BO-04; KPI-09) |
| Dependencies | US-066; ADR-014, ADR-015 |
| Traces | PRD-FR-094 · SRS-FR-SET-005 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Data & privacy → Clear history | It opens | The options are recordings only; attempts and transcripts; word progress; everything, each with a confirmation stating the consequences |
| AC2 | A signed-in user confirms an option | Deletion completes | The matching cloud data is deleted as well (SRS-FR-SET-005) |
| AC3 | They clear attempts | They open Progress | Streak and statistics are recalculated from the remaining data (SRS-FR-PROG-005) |

**Edge cases:** (1) Signed in and offline: the cloud deletion follows when online (SEC-01 §9.2, RECOMMENDATION). (2) Other devices remove the same items on their next pull (SEC-01 §9.2, RECOMMENDATION). (3) Overlap with "Delete everything on this device" (FS-01 Issue 6).

#### US-062 — Read accurate privacy information

| Field | Value |
|---|---|
| Story | As Meera, I want a privacy screen that says exactly what is stored where and what never leaves the phone, so that I can trust the app's promises. |
| Persona(s) · user class | Meera (P2); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — the current privacy text is false (FND-SEC-05, ISS-03); this makes privacy verifiable (BO-04; KPI-09) |
| Dependencies | US-059; decision D-23 |
| Traces | PRD-FR-095 · SRS-FR-SET-006 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Privacy information (SCR-SET-09) | It opens | It describes what is stored locally, what syncs when signed in, what never leaves the phone, and how the current tier behaves |
| AC2 | The policy constants that drive behaviour change | The screen is rendered | The text changes with them, and automated tests verify it (SRS-PRIV-002, NFR-PRIV-007) |

**Edge cases:** (1) On T3a, audio goes to the device's speech service and the text must say so (SRS-PRIV-001). (2) Backup behaviour follows D-23 (include-only `learning.db` + `settings`). (3) Signed-out state with kept data.

#### US-063 — Choose theme and motion

| Field | Value |
|---|---|
| Story | As Sofia, I want to choose a light, dark or system theme and reduce motion, with text that follows my system font size, so that the app is comfortable to use. |
| Persona(s) · user class | Sofia (P4); all personas · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Low — comfort and accessibility (BR-013) (BO-01) |
| Dependencies | DES-05, DES-07 |
| Traces | PRD-FR-096 · SRS-FR-SET-007 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Appearance | It opens | Light / Dark / System (default System) and Reduce motion (default follows the system) are offered |
| AC2 | System font scale at 200 % | Any screen is shown | Text follows it without clipping (NFR-A11Y-002) |

**Edge cases:** (1) Reduce motion also applies to milestone celebrations (SRS-FR-PROG-006). (2) The theme changes while the session player is open.

#### US-064 — See app information and share diagnostics

| Field | Value |
|---|---|
| Story | As a beta tester, I want to see the app version, licences and legal documents and share a diagnostic log without personal data, so that I can report problems safely. |
| Persona(s) · user class | Aarav (P1) taking part in the beta · Beta tester, Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Low — supports beta feedback and support (BO-05) |
| Dependencies | ADR-007; decision D-13 |
| Traces | PRD-FR-097 · SRS-FR-SET-008 · FS-10 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | About | It opens | Version name and code, open-source licences, privacy policy, terms, help/contact and Share diagnostics are shown |
| AC2 | Share diagnostics | They use it | The shared log contains no personal data: no uid, email, name or transcript (NFR-SEC-002) |

**Edge cases:** (1) SRS §4 limits diagnostics sharing to the Beta tester class, while SRS-FR-SET-008 offers it to everyone (Issue 21). (2) Offline: the hosted privacy policy page cannot open (D-30).

### 4.11 E-11 — CAP-11 Offline-first storage & sync (FS-11)

#### US-065 — Practise fully offline

| Field | Value |
|---|---|
| Story | As Aarav, who practises on the train without signal, I want every core feature to work offline, so that I can practise anywhere. |
| Persona(s) · user class | Aarav (P1), Daniel (P3), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — "let it work on the train" (UN-09) (BR-004) (BO-04) |
| Dependencies | ADR-002, ADR-009 |
| Traces | PRD-FR-100 · SRS-FR-DATA-001 · FS-11 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | No network on a T1, T2 or T3b phone | They complete onboarding, a Today session, a library activity and a word stage, and open Progress | Each works fully (SRS-AVL-001) |
| AC2 | Any change to learning data | It is saved | It is written to the local database first, in a transaction, and the UI never waits for the network |
| AC3 | Firebase is unreachable | They use the app | Only sign-in and sync are affected, and their status is visible (SRS-AVL-002) |

**Edge cases:** (1) T3a needs a network; without one, the attempt falls back to T3b (SRS-AVL-003). (2) Downloading the offline pack needs a network. (3) A local write failure is never shown as success (SRS-ERR-005).

#### US-066 — Back up progress automatically

| Field | Value |
|---|---|
| Story | As Daniel, I want my progress to back up in the background when I am signed in, and to see the sync status, so that I never worry about losing it. |
| Persona(s) · user class | Daniel (P3), Aarav (P1) · Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — backup without friction (BO-02, BO-04) |
| Dependencies | US-002; ADR-005, ADR-014; decision D-16 |
| Traces | PRD-FR-101 · SRS-FR-DATA-003, SRS-FR-DATA-005, SRS-FR-DATA-006 · FS-11 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A signed-in user makes a syncable change | It is saved | An outbox entry is created and a background job uploads it when online, with exponential back-off |
| AC2 | Settings → Account | It is viewed | Sync status shows Up to date, Syncing, Waiting for connection, or Error with Retry |
| AC3 | Any uploaded document | It is inspected | It contains no audio, no file paths and, by default, no transcript |
| AC4 | Sync fails | The failure occurs | The outbox is kept and the UI is never blocked (SRS-ERR-006) |

**Edge cases:** (1) Changes made while signed out are never queued (FS-01 Issue 2). (2) The account was deleted from another device: sync stops and the user is told they have been signed out (SEC-01 §4.3, RECOMMENDATION). (3) Transcript sync is an R2 opt-in (D-16).

#### US-067 — Use two devices without losing progress

| Field | Value |
|---|---|
| Story | As Aarav, I want practice from my two phones combined by clear rules, so that no completed work is lost. |
| Persona(s) · user class | Aarav (P1) · Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — no lost progress across devices (BO-03) |
| Dependencies | US-066; ADR-014 |
| Traces | PRD-FR-102 · SRS-FR-DATA-002, SRS-FR-DATA-004 · FS-11 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Both phones recorded attempts offline | Both sync | Every attempt exists on both phones exactly once (union by UUID; SRS-BR-020) |
| AC2 | The profile was edited on both phones | The edits merge | The later edit wins for that document (SRS-BR-021) |
| AC3 | A word advanced on one phone and was reviewed on the other | The data merges | The more advanced stage and the later review schedule are kept (SRS-BR-021) |
| AC4 | The app starts online or the user signs in | The pull runs | Documents changed since the last watermark are downloaded and merged |

**Edge cases:** (1) Clock differences between phones affect last-writer-wins. (2) History cleared on one phone (SEC-01 §9.2 markers, RECOMMENDATION). (3) The streak is recalculated locally from the merged facts (ADR-014).

#### US-068 — Keep my profile and words from the old app

| Field | Value |
|---|---|
| Story | As Meera, an existing signed-in user, I want my profile and vocabulary progress carried over when I update to the new version, so that I do not start from scratch. |
| Persona(s) · user class | Meera (P2) · Signed-in user (legacy) |
| Priority · Release | P1 · R1 |
| Business value | Medium — preserves essential user data through the transition (BR-016) (BO-05) |
| Dependencies | US-067; ARC-03 §9; decisions D-19, D-15 |
| Traces | PRD-FR-103 · SRS-FR-DATA-007, SRS-FR-DATA-008 · FS-11 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A signed-in legacy user | They first launch the redesigned version | Profile (name, level, goal, daily minutes) and vocabulary progress are imported from the legacy collections and migration is marked complete |
| AC2 | Migration fails | The failure occurs | The app stays usable, migration is retried at the next launch and a diagnostic without personal data is logged |
| AC3 | Migration has completed | The app launches again | Migration does not run again |

**Edge cases:** (1) The SRS says legacy documents stay untouched, while SEC-01 §5.6 has the client delete them after migration (Issue 20). (2) Debug-signed legacy installs cannot update in place (SEC-01 §5.6). (3) How legacy values map to the five D-17 levels is left to ARC-03 §9. (4) Whether migrated users see onboarding (FS-02 Issue 5).

### 4.12 E-12 — CAP-12 Speech capability management (FS-12)

#### US-069 — See an honest speech-capability status

| Field | Value |
|---|---|
| Story | As Hoang, I want the app to show what my phone can actually do for speech recognition, so that I am never told something works when it does not. |
| Persona(s) · user class | Hoang (P5); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — honest status (PP-1) replaces false capability claims (BO-01) |
| Dependencies | ADR-009; SPK-01; decision D-14 |
| Traces | PRD-FR-110 · SRS-FR-SPCH-001, SRS-FR-SPCH-002, SRS-FR-SPCH-003, SRS-FR-SPCH-010 · FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | App start, a locale change, a consent change or an offline-pack installation | The capability service runs | The tier is set to T1, T2, T3a or T3b per SRS-FR-SPCH-001 and the status text reflects verified state only |
| AC2 | The current tier | They record | Capture follows the tier: T1 app-owned audio piped to the recogniser; T2/T3a the recogniser owns the microphone; T3b audio only (SRS-FR-SPCH-002) |
| AC3 | The recogniser returns "no match" | The error is classified | The attempt is Inconclusive and the capability status does not change |

**Edge cases:** (1) A T1 piping failure demotes the phone to T2, and this is remembered (ARC-05 §3). (2) Recogniser busy: T3b for that attempt (SRS-ERR-002). (3) Speech service unavailable: T3b with an explanation (SRS-AVL-003).

#### US-070 — Install the offline speech pack from the app

| Field | Value |
|---|---|
| Story | As Daniel, I want to install the English offline speech pack from inside SpeakCraft, so that transcription works without internet and stays on my phone. |
| Persona(s) · user class | Daniel (P3), Meera (P2) · Local user, Signed-in user |
| Priority · Release | P1 · R1 |
| Business value | Medium — moves users from consented online or audio-only modes to private on-device transcription (BO-04, BO-01) |
| Dependencies | US-069; ADR-009; SPK-01 |
| Traces | PRD-FR-111 · SRS-FR-SPCH-004 · FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | An Android 13+ phone whose platform supports pack management, with the pack not installed | They open Speech & privacy or the speech check | Pack status and an Install action are shown |
| AC2 | Install | The download runs | Progress is shown, a failure offers Retry, and the tier is re-evaluated after installation (SRS-FR-SPCH-003) |

**Edge cases:** (1) No network. (2) The download is done by the OS speech provider and is disclosed as such (SEC-01 §7.3 #6). (3) Pack pending installation.

#### US-071 — Decide about online transcription

| Field | Value |
|---|---|
| Story | As Meera, whose phone has no on-device recogniser, I want to be told exactly what online transcription sends and to whom before I agree, and to withdraw at any time, so that my voice leaves my phone only with my permission. |
| Persona(s) · user class | Meera (P2), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — explicit, revocable consent (BR-005) (BO-04) |
| Dependencies | US-069; decisions D-14, D-25 |
| Traces | PRD-FR-112 · SRS-FR-SPCH-005, SRS-FR-SPCH-006 · FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A phone without an on-device recogniser, where a recogniser exists | The consent screen is shown | It states that audio is sent to the device's speech service provider (named where it can be determined) for transcription only, that SpeakCraft does not store it remotely, and that consent can be withdrawn in Settings |
| AC2 | They have not consented | They record | No audio or transcript leaves the phone (SRS-BR-023, SRS-PRIV-001) |
| AC3 | They withdraw consent | The next attempt starts | The tier is T3b (SRS-FR-SPCH-006) |

**Edge cases:** (1) The consent screen is never shown mid-recording (SEC-01 §7.3 #9). (2) Re-consent when the default recogniser package changes (SEC-01 §7.3 #10, RECOMMENDATION). (3) Network lost on T3a (FS-00 §6, FS-04).

#### US-072 — Get useful feedback without a transcript

| Field | Value |
|---|---|
| Story | As Hoang, whose phone cannot turn speech into text, I want feedback on my speaking time and pauses with a clear explanation of what is missing, so that I can still practise honestly. |
| Persona(s) · user class | Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P0 · R1 |
| Business value | High — honest degradation keeps T3b phones useful (PRD-NFR-007) (BO-01) |
| Dependencies | US-069, US-024 |
| Traces | PRD-FR-113 · SRS-FR-SPCH-007 · FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Tier T3b | A result is shown | The headline metrics are speaking time, long pauses and speech ratio, and transcript-based checks say "Transcript needed for this check" |
| AC2 | Tier T3b | Today's plan is built | Delivery-oriented activities are preferred |
| AC3 | A Read aloud, Listen & repeat or word-stage step on T3b | It is completed | Only delivery metrics are shown and the step is marked "Transcript needed for this check" |

**Edge cases:** (1) Word stages 2–4 can never pass on T3b, so words cannot progress (Issue 11). (2) KPI-06 is measured on T1/T2 phones only.

#### US-073 — Choose my recognition accent

| Field | Value |
|---|---|
| Story | As Aarav, I want to choose the English variant used for recognition (US, UK, Indian or Australian), so that recognition matches how I speak. |
| Persona(s) · user class | Aarav (P1), Hoang (P5) · Local user, Signed-in user |
| Priority · Release | P2 · R1 |
| Business value | Low — may improve recognition for some speakers (BO-01) |
| Dependencies | US-069 |
| Traces | PRD-FR-114 · SRS-FR-SPCH-008 · FS-12 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | Speech & privacy | They open recognition language | en-US, en-GB, en-IN and en-AU are offered, filtered by what the recogniser supports |
| AC2 | First launch | The default is set | It is the device locale if that is English, otherwise en-US |
| AC3 | They change the locale | They save | The tier is re-evaluated (SRS-FR-SPCH-003) and reference audio uses the new locale (SRS-FR-SESS-012) |

**Edge cases:** (1) The pack for the new locale is not installed, so the phone may drop from T1. (2) The recogniser supports none of the four locales.

### 4.13 E-13 — Transition & platform (no FS; ENG-01, SEC-01)

#### US-074 — Use an app without retired features or unused permissions

| Field | Value |
|---|---|
| Story | As Sofia, I want the app to contain only features that work and to ask only for the permissions it uses, so that it is simple and trustworthy. |
| Persona(s) · user class | Sofia (P4); all personas · Local user, Signed-in user |
| Priority · Release | P0 · R0–R1 |
| Business value | High — less complexity and no policy exposure (BO-05, BO-06; KPI-11, KPI-12) |
| Dependencies | ADR-003, ADR-004, ADR-010, ADR-011; decisions D-04, D-08 (DECISION 2026-11-21) |
| Traces | PRD-FR-120, PRD-FR-057 · SRS-FR-PLAT-001, SRS-FR-PLAT-002 · no FS (ENG-01, SEC-01). Business rule SRS-BR-028 |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A release build | Its contents are inspected | Firebase AI Logic, MediaPipe, TensorFlow Lite, CameraX, the YouTube client and any YouTube ingestion or caption download, the self-updater, the admin console, the AI Mentor, the conversation simulator, the tutorial overlay system and creator-branded content are absent (SRS-FR-PLAT-001, subject to ADR approvals) |
| AC2 | A release build | Creator-derived content is inspected | It appears only as curated, provenance-tagged content-pack entries in original wording (US-034, US-035), with no creator name, likeness or branding in the UI, content, notifications or store listing (SRS-BR-028; D-08 DECISION) |
| AC3 | The merged manifest | It is inspected | Only INTERNET, RECORD_AUDIO and POST_NOTIFICATIONS (Android 13+) are declared, plus `<queries>` for `android.speech.RecognitionService` (SRS-FR-PLAT-002) |

**Edge cases:** (1) The creator-branded tab is retired. Its place is taken by the Techniques collection and track content inside Practice, keeping the four-tab IA (SRS-FR-PRAC-008; canon R-6). (2) The existing creator-derived items are re-verified and migrated into the pack, not deleted; per-user Firestore seeding (`extractedTechniques`, `youtubeSources`) is still retired (canon R-6; Issue 4). (3) Data paths of retired features (legacy Firestore collections) are retired in phases (SEC-01 §5.6). (4) Release notes tell users about removed features.

#### US-075 — Install only trusted, release-signed builds

| Field | Value |
|---|---|
| Story | As Daniel, I want every build I install to be release-signed and updated through a trusted channel, so that the app on my phone has not been tampered with. |
| Persona(s) · user class | Daniel (P3); all personas · Local user, Signed-in user, Beta tester |
| Priority · Release | P0 · R0 |
| Business value | High — removes a critical security and policy risk (ISS-02) (BO-04, BO-06) |
| Dependencies | ADR-004; decisions D-03, D-28 |
| Traces | PRD-FR-121 · SRS-FR-PLAT-003 · no FS (ENG-01) |

| AC | Given | When | Then |
|---|---|---|---|
| AC1 | A distributed build | It is verified | It is a release build, R8-minified, release-signed and not debuggable (NFR-SEC-003) |
| AC2 | The running app | Its screens are reviewed | There is no in-app APK installer or update screen (subject to D-03) |
| AC3 | Debug-only code (App Check debug provider) | The release build is inspected | It is absent (SRS-FR-PLAT-003) |

**Edge cases:** (1) Debug-signed legacy installs cannot update in place and must reinstall (SEC-01 §5.6). (2) The distribution channel depends on D-03 / ADR-004.

---

## 5. Coverage — PRD-FR → user stories

Every PRD functional requirement defined in PRD §15 (PRD v0.2, 2026-11-21) appears below. The PRD numbers requirements in blocks per capability and leaves gaps; undefined numbers are listed as gap rows (Issue 24).

| PRD-FR | P | R | Stories | Note |
|---|---|---|---|---|
| PRD-FR-001 | P1 | R1 | US-001 | — |
| PRD-FR-002 | P1 | R1 | US-002 | — |
| PRD-FR-003 | P1 | R1 | US-003 | — |
| PRD-FR-004 | P1 | R1 | US-004 | D-21 default |
| PRD-FR-005 | P0 | R1 | US-005, US-006 | — |
| PRD-FR-006…009 | — | — | — | Not defined in the PRD (numbering gap): no story |
| PRD-FR-010 | P0 | R1 | US-008 | — |
| PRD-FR-011 | P0 | R1 | US-009 | — |
| PRD-FR-012 | P0 | R1 | US-010 | — |
| PRD-FR-013 | P0 | R1 | US-011 | — |
| PRD-FR-014 | P0 | R1 | US-008 (AC5); also US-057, US-059, US-054 | Issue 7 |
| PRD-FR-015…019 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-020 | P0 | R1 | US-012 | — |
| PRD-FR-021 | P0 | R1 | US-013 | — |
| PRD-FR-022 | P0 | R1 | US-014 | — |
| PRD-FR-023 | P1 | R1 | US-015 | — |
| PRD-FR-024 | P1 | R1 | US-016 | — |
| PRD-FR-025 | P2 | R1 | US-017 | — |
| PRD-FR-026 | P2 | R1 | US-017 | Issue 26 |
| PRD-FR-027…029 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-030 | P0 | R1 | US-018 | — |
| PRD-FR-031 | P0 | R1 | US-019 | — |
| PRD-FR-032 | P0 | R1 | US-020 | — |
| PRD-FR-033 | P0 | R1 | US-021 | — |
| PRD-FR-034 | P1 | R1 | US-022 (also US-049) | — |
| PRD-FR-035 | P0 | R1 | US-023 | — |
| PRD-FR-036 | P1 | R1 | US-022 | — |
| PRD-FR-037…039 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-040 | P0 | R1 | US-024 | — |
| PRD-FR-041 | P0 | R1 | US-025 | — |
| PRD-FR-042 | P1 | R1 | US-027 | SPK-02 |
| PRD-FR-043 | P0 | R1 | US-028 | D-06 |
| PRD-FR-044 | P1 | R1 | US-029 | Issues 9, 10 |
| PRD-FR-045 | P1 | R1 | US-029 | — |
| PRD-FR-046 | P0 | R1 | US-030 | — |
| PRD-FR-047 | P0 | R1 | US-026, US-042 (also US-020 AC4) | Added in PRD v0.2; Issues 27, 28, 31 |
| PRD-FR-048…049 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-050 | P0 | R1 | US-031, US-035 | — |
| PRD-FR-051 | P0 | R1 | US-032 | — |
| PRD-FR-052 | P0 | R1 | US-033, US-035 | — |
| PRD-FR-053 | P0 | R1 | US-034 | — |
| PRD-FR-054 | P2 | R1 | US-037 (also US-035 AC5) | — |
| PRD-FR-055 | P1 | R2 | US-038 | — |
| PRD-FR-056 | P0 | R1 | US-035 (also US-031 AC1, US-026 AC4) | Added in PRD v0.2; Issue 29 |
| PRD-FR-057 | P0 | R1 | US-036, US-034 (AC4, AC5), US-074 (AC2) | Added in PRD v0.2; D-08 DECISION; Issue 30 |
| PRD-FR-058…059 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-060 | P0 | R1 | US-039, US-040, US-041 | — |
| PRD-FR-061 | P0 | R1 | US-043 | — |
| PRD-FR-062 | P0 | R1 | US-040, US-041, US-042 | — |
| PRD-FR-063 | P0 | R1 | US-041, US-044 | — |
| PRD-FR-064 | P1 | R1 | US-045 | — |
| PRD-FR-065 | P0 | R1 | US-046 | — |
| PRD-FR-066 | P1 | R2 | US-047 | — |
| PRD-FR-067 | P2 | R2 | US-047 | — |
| PRD-FR-068…069 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-070 | P0 | R1 | US-048 | — |
| PRD-FR-071 | P0 | R1 | US-049 | — |
| PRD-FR-072 | P0 | R1 | US-050 | — |
| PRD-FR-073 | P0 | R1 | US-051 | — |
| PRD-FR-074 | P1 | R1 | US-052 | — |
| PRD-FR-075 | P1 | R2 | US-053 | — |
| PRD-FR-076…079 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-080 | P1 | R1 | US-054 | — |
| PRD-FR-081 | P1 | R1 | US-055 | Issue 19 |
| PRD-FR-082 | P1 | R1 | US-055 | — |
| PRD-FR-083 | P1 | R1 | US-054 | — |
| PRD-FR-084 | P2 | R2 | US-056 | — |
| PRD-FR-085…089 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-090 | P1 | R1 | US-057 | Issue 22 |
| PRD-FR-091 | P0 | R1 | US-058 | — |
| PRD-FR-092 | P0 | R1 | US-059 | D-10, D-24 |
| PRD-FR-093 | P0 | R1 | US-060 | D-22; Issue 17 |
| PRD-FR-094 | P0 | R1 | US-061, US-007 | — |
| PRD-FR-095 | P0 | R1 | US-062 | — |
| PRD-FR-096 | P1 | R1 | US-063 | — |
| PRD-FR-097 | P1 | R1 | US-064 | Issue 21 |
| PRD-FR-098…099 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-100 | P0 | R1 | US-065 | — |
| PRD-FR-101 | P1 | R1 | US-066 | — |
| PRD-FR-102 | P1 | R1 | US-067, US-003 | — |
| PRD-FR-103 | P1 | R1 | US-068 | D-19; Issue 20 |
| PRD-FR-104…109 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-110 | P0 | R1 | US-069 | SPK-01 |
| PRD-FR-111 | P1 | R1 | US-070 | — |
| PRD-FR-112 | P0 | R1 | US-071 | — |
| PRD-FR-113 | P0 | R1 | US-072 | Issue 11 |
| PRD-FR-114 | P2 | R1 | US-073 | — |
| PRD-FR-115…119 | — | — | — | Not defined (numbering gap): no story |
| PRD-FR-120 | P0 | R0–R1 | US-074 | Issue 4. Platform work is in ENG-01 / SEC-01 |
| PRD-FR-121 | P0 | R0 | US-075 | D-03. Platform work is in ENG-01 |

**Product quality expectations (PRD-NFR-001…011)** are covered by NFR/platform: NFR.md is their authoritative home, and TST-01 holds the cross-cutting suites. Stories carry the user-visible parts as acceptance criteria. Honesty (PRD-NFR-001) appears in US-020, US-024, US-026, US-028 and US-069. Privacy (PRD-NFR-002) appears in US-009, US-062, US-066 and US-071. Offline (PRD-NFR-003) appears in US-065. Reliability (PRD-NFR-005) appears in US-008 AC4 and US-023. Accessibility (PRD-NFR-006) appears in US-018, US-048 and US-063. Security (PRD-NFR-008) appears in US-064 and US-075.

**Summary:** 78 of 78 defined PRD-FRs have at least one story; no defined PRD-FR is left without a story. Every story traces to at least one PRD-FR, at least one SRS-FR and an FS, except US-074 and US-075, which by design have no FS (RD-16 §2 platform capabilities). All 129 SRS-FRs in SRS v0.2 §6 appear in at least one story, including SRS-FR-EVAL-015/016 (US-026), SRS-FR-PRAC-008 (US-035), SRS-FR-PRAC-009 (US-034, US-036) and SRS-FR-WORD-014 (US-042). SRS-BR-028 is applied in US-034, US-035, US-036 and US-074.

## 6. Issues found in source documents

Issues already logged in FS-01 §19 and FS-02 §19, and summarised in FS-00 §6, are referenced by the stories and not repeated here. The items below are new or are raised from the story perspective. Each goes to the owner of the source document; it is not fixed here (GOV-02 DC-2). Items that PRD v0.2, SRS v0.2, FS-01 v0.2 or SEC-01 v0.2 (all 2026-11-21) have since resolved keep their number and are marked **Resolved**.

| # | Issue | Source | Suggested resolution | Affects |
|---|---|---|---|---|
| 1 | **Technique content: gaps that remain.** PRD-FR-056/057, SRS-FR-PRAC-008/009 and SRS-BR-028 now give technique content a requirement home; DES-02 v0.2 adds SCR-PRAC-04; RD-16 v0.2 adds the Techniques collection to CAP-06. Still missing: ARC-03 §6 (cited by SRS-FR-EVAL-016 and ARC-05 §8.4) does not yet define the technique record, provenance fields, `licenceStatus`, activity→technique references or tip triggers. Canon R-6 lists `licenceStatus` as LICENSED, ORIGINAL_REEXPRESSION or PENDING_REVIEW, but SRS-BR-028 names only the last two, and under D-08 LICENSED is unused for creator material | ARC-03 §6; SRS-BR-028; canon R-6 | ARC-03 §6 defines the schema; the SRS or ARC-03 defines the `licenceStatus` values and when each applies; FS-06 elaborates | US-031, US-034, US-035, US-036 |
| 2 | **Resolved (D-08 DECISION, 2026-11-21).** PRD-FR-053's "original" is consistent with original re-expression (SRS-BR-028) and with BR-008 | PRD-FR-053; BR-008 | None | — |
| 3 | **Resolved (PLN-02 / PLN-04 revisions, 2026-11-21).** The re-verification of existing creator-derived items with channel-root timestamps is now planned as WBS T-09.6 (with T-09.7 and T-09.8), RAID RSK-04, RSK-17 and DEP-10, using the CNT-01 workflow; SRS-BR-028 blocks unverified items at build time | — | None | US-034 |
| 4 | **Partly resolved.** RD-16 §5 and NG-08 (v0.2), ADR-011 (narrowed to ingestion and branding) and BRD (stakeholders) now reflect D-08. Still open: BRD KPI-12 ("unlicensed third-party content items = 0") does not say how re-expressed items (`ORIGINAL_REEXPRESSION`) and items still in `PENDING_REVIEW` are counted | BRD KPI-12 | Define the KPI-12 counting rule against `licenceStatus` | US-074, US-034 |
| 5 | **Resolved (SRS v0.2).** SRS-FR-SESS-002/003 now trace PRD-FR-051 | — | None | — |
| 6 | **Resolved (SRS v0.2).** SRS-FR-TODAY-002/004 now trace PRD-FR-061 | — | None | — |
| 7 | **Partly resolved (SRS v0.2:** SRS-FR-SET-001 now traces PRD-FR-014). Still open: the contents of Settings → Learning (SCR-SET-02) are not specified, and SRS-BR-027 gives a new-word goal range of 0–5, which implies a user setting that no SRS-FR provides | SRS-FR-SET-002; SRS-BR-027 | Add an SRS-FR listing the Learning settings (track, level, daily minutes, new-word goal) | US-008, US-043, US-058 |
| 8 | **Partly resolved (FS-01 v0.2, SEC-01 v0.2** now follow the D-21 pre-selection). Still open: the *Keep* branch data model differs. FS-01 §5.3 keeps data keyed by the UID as SIGNED_OUT; SEC-01 §4.4/§6.4 re-keys it to a local profile marked "previously linked" | FS-01 §5.3; SEC-01 §4.4, §6.4 | Choose one model | US-004 |
| 9 | Ratings and saved activities. SRS-FR-EVAL-009 stores the accuracy rating "with the attempt", while the canon and FS-00 §2.1 keep ENT-13 FeedbackRating in a separate local-only table so attempts stay immutable (SRS-FR-DATA-002). SRS §9 has no row for ENT-12 SavedActivity or ENT-13, so the scope of export and deletion completeness (SRS-PRIV-003) is ambiguous | SRS-FR-EVAL-009; SRS §9 | Reword SRS-FR-EVAL-009; add ENT-12 and ENT-13 to SRS §9 with sync and export status | US-029, US-037, US-060 |
| 10 | KPI-05 (feedback accuracy) depends on "Was this accurate?" answers, but these are local only, not synced, and there are no analytics in R1 (SRS-PRIV-004, ADR-013). No path to compute KPI-05 is defined | BRD KPI-05; SRS-PRIV-004; D-09 | Define the beta measurement method under D-09 | US-029 |
| 11 | On T3b, SRS-FR-SPCH-007 shows delivery metrics for Read/Repeat and word stages, but SRS-FR-WORD-006 makes word-stage checks without recognition output Inconclusive. So a T3b user can never pass stages 2–4, never complete a word task (SRS-BR-009) and never learn a word | SRS-FR-SPCH-007 vs SRS-FR-WORD-006, SRS-BR-009; FS-00 §6 (FS-03) | Define the T3b word path | US-012, US-040, US-042, US-072 |
| 12 | Whether a valid library attempt on an activity that is also in today's plan completes that Today step is not stated | SRS-FR-PRAC-004; SRS-BR-009 | State the rule in SRS-BR-009 | US-033 |
| 13 | SRS-FR-SESS-007 lists four Inconclusive reasons; ARC-05 §5 defines five (adds TOO_FEW_WORDS) | SRS-FR-SESS-007 vs ARC-05 §5 | Align the lists | US-020 |
| 14 | Retry comparison (SRS-FR-SESS-008) and trends (SRS-FR-PROG-002) do not say how attempts evaluated under different `evaluationVersion` values are compared | SRS-FR-SESS-008, SRS-FR-PROG-002, SRS-FR-EVAL-011 | Add a rule | US-021, US-030, US-048 |
| 15 | SRS-FR-SESS-010 does not list audio-route changes (headset or Bluetooth disconnect) or process death among the interruptions | SRS-FR-SESS-010 | Extend the list or map them to existing triggers | US-023 |
| 16 | SRS §4 abbreviates user classes as UC-L, UC-S, UC-B, which collides with the use-case ID namespace `UC-nn` (GOV-02 §3.2) | SRS §4; GOV-02 §3.2 | Rename the classes | All stories (naming) |
| 17 | **Partly resolved (SEC-01 v0.2** §7.1/§9.1 now follow the D-22 recommendation on audio). Still open: delivery. SRS-FR-SET-004 and SRS §8 use the share sheet with a FileProvider limited to `exports/`; SEC-01 §6.1/§9.1 recommend the Storage Access Framework with no FileProvider (logged for RD-99). No SRS-ERR covers running out of storage while writing an export | SRS-FR-SET-004, SRS §8, SRS §15 vs SEC-01 §6.1, §9.1 | Decide one delivery mechanism; add an export-storage error | US-060 |
| 18 | The D-24 recommendation ("Delete transcripts when recordings expire") has no SRS-FR. The rule that a shorter retention applies to existing recordings exists only in SEC-01 §7.1. SRS §9 says transcripts are kept in "Room", while the canon puts them in `sensitive.db` | D-24; SRS-FR-SET-003; SRS §9; SEC-01 §7.1 | Add the option and the retention-change rule to the SRS; name `sensitive.db` | US-059 |
| 19 | PRD-FR-081 and SRS-BR-019 say "at most one reminder per day **by default**", but no non-default option is defined | PRD-FR-081; SRS-BR-019 | Remove "by default" or define the option | US-055 |
| 20 | Legacy migration. SRS-FR-DATA-007 leaves legacy documents untouched, while SEC-01 §5.6 L1 has the client delete the legacy subcollections it migrated. BR-016 includes learning preferences, but SRS-FR-DATA-007 imports only name, level, goal and daily minutes. The mapping to the five D-17 levels and five tracks is not in the SRS | SRS-FR-DATA-007 vs SEC-01 §5.6; BR-016; D-17, D-19 | Align both documents; define the mapping in ARC-03 §9 | US-068 |
| 21 | SRS §4 gives "diagnostics sharing" only to the Beta tester class, while SRS-FR-SET-008 offers Share diagnostics to all users | SRS §4 vs SRS-FR-SET-008 | Align | US-064 |
| 22 | "Key statistics" on Profile are not defined | PRD-FR-090; SRS-FR-SET-001 | List them | US-057 |
| 23 | The word-list filters leave out the "Reviewing" status defined in SRS-FR-WORD-001 | SRS-FR-WORD-010 vs SRS-FR-WORD-001 | Add Reviewing or state where it falls | US-045 |
| 24 | PRD-FR numbering leaves undefined blocks without saying they are reserved | PRD §15 | State that gaps are reserved per capability | §5 coverage |
| 25 | Review sets: SRS-BR-016 defines grades 4/3/1 only, so the grade (or non-grade) for an Inconclusive recall is not stated. Whether outcomes are kept when a set is left part-way is not stated either | SRS-BR-016; SRS-FR-WORD-009 | Add both rules | US-044 |
| 26 | PRD-FR-026 lets users change today's duration "before starting", while SRS-FR-TODAY-007 also allows changes mid-day | PRD-FR-026 vs SRS-FR-TODAY-007 | Align wording | US-017 |
| 27 | **Coach tips: partly resolved.** ARC-05 v0.2 §8.4 now defines the issue catalogue (ISSUE_* codes), display priority and tip ranking. Still open: (a) the tip-trigger schema in ARC-03 §6 does not exist yet. (b) The fallback "a general tip for the activity's technique" has no rule for activities that reference no technique (for example `onb-intro`, read-aloud warm-ups, word stages). (c) ARC-05 §8.4 adds a user-ticked "self-check" checklist (stored as self-reported) and candidate acoustic metrics (pitch range, volume variation), and no PRD-FR or SRS-FR covers either | SRS-FR-EVAL-016; ARC-05 §8.4; ARC-03 §6 | Define the tip schema in ARC-03 §6; add a no-technique fallback; add requirements for the self-check (or mark it RECOMMENDATION) and keep the acoustic metrics out of R1 until SPK-01 validates them | US-026, US-035 |
| 28 | **Honesty of the "Mistakes found" label.** PRD-FR-047 and SRS-FR-EVAL-015 call every detected issue a "mistake", including Estimated ones (fillers, grammar observations), while SRS-FR-EVAL-006 requires "Possible issue" wording and PP-1 / SRS-BR-003 require Estimated items to read as estimates | PRD-FR-047; SRS-FR-EVAL-015 vs SRS-FR-EVAL-006, SRS-BR-003 | State that Estimated entries keep "Possible issue" wording inside the section; add this to the TST-01 §9 honesty checklist | US-026, US-027 |
| 29 | PRD-FR-056 says techniques "are used by Today's plan" (mandatory), but SRS-FR-PRAC-008 says the plan engine "may" select activities by technique, and SRS-FR-TODAY-002's plan inputs do not include technique | PRD-FR-056 vs SRS-FR-PRAC-008, SRS-FR-TODAY-002 | Decide; if mandatory, add technique to the plan inputs (FS-03) | US-035, US-013 |
| 30 | **"Watch the original lesson" link: behaviour not specified.** No defined behaviour when offline, when no app can handle the link, or when the video is later removed or made private, and no re-verification cadence. Checking for a handler needs package-visibility `<queries>`, which SRS-FR-PLAT-002 does not list. SRS-FR-PRAC-009 requires provenance on *all* technique, activity and tip records, while SRS-FR-PRAC-008 and SRS-BR-028 imply provenance only for creator-derived items, which leaves wholly in-house content unclear | SRS-FR-PRAC-008, -009, SRS-BR-028, SRS-FR-PLAT-002 | Clarify the provenance scope; add link-failure behaviour and a re-verification cadence; extend SRS-FR-PLAT-002 if needed | US-034, US-036 |
| 31 | SRS-FR-WORD-014 gives "one coach tip for the next try" for word results, yet says SRS-FR-EVAL-015/016 apply, and EVAL-016 gives the focus tip plus up to 2 more | SRS-FR-WORD-014 vs SRS-FR-EVAL-016 | State the tip count for word results | US-042 |

## 7. Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-21 | Initial draft: 75 stories across 12 capability epics and the transition/platform group; coverage of all 78 PRD-FRs in PRD v0.2; 31 source issues (4 resolved and 5 partly resolved by the 2026-11-21 v0.2 revisions of PRD, SRS, FS-01, SEC-01, ARC-05, RD-16, ADR-011, PLN-02 and PLN-04). The Product Owner direction of 2026-11-21 is applied: D-08 DECISION "original wording, link out" (§1, §4.6, US-034, US-035, US-036, US-046, US-074); mistakes and coach tips after every submission (US-026, US-042); technique curriculum (US-035) and source-video link-out (US-036). To stay within 75 stories, replay and reference audio (US-022), "How we measured this" and "Was this accurate?" (US-029), and the R2 notebook and quiz (US-047) were each combined into one story | Product Manager |
