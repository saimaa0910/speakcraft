# Sprint Plan

| Field | Value |
|---|---|
| Document ID | PLN-03 |
| Version | 0.2 — Draft for review (2026-11-21: T-04.1.1 scope aligned to the D-08 decision) |
| Date | 2026-11-21 |
| Status | Draft — **preliminary**; first sprints planned in detail, later sprints indicative |
| Owner (role) | Project Manager |
| Approval | Pending — Product Owner (G4) |
| Related | PLN-01, PLN-02, PLN-05 §6 (Definition of Done), TST-01 |

---

## 1. Cadence and capacity

| Item | Value |
|---|---|
| Sprint length | 2 weeks |
| Capacity assumption | ~8 focus engineering days per sprint (one developer with AI assistance); 20 % reserved for defects/review — **planned load ≤ 6.5 d** |
| Ceremonies | Sprint planning (agent proposes, Product Owner confirms scope); mid-sprint check; sprint review with evidence (PLN-05 §8); retrospective note |
| Commitment rule | Only stories whose dependencies and decisions are resolved may enter a sprint; no sprint is planned above capacity |

## 2. Definition of Ready (story enters a sprint)

1. Linked SRS-FR/SRS-BR and FS acceptance criteria exist.
2. Required ADRs/decisions approved.
3. Design available for UI work (DES-03/06).
4. Test approach known (TST-01 level, fixtures).
5. Estimated ≤ 5 d (else split).

## 3. Definition of Done

As PLN-05 §6 (tests alongside code, CI green, acceptance demonstrated with evidence, accessibility and honesty checks, docs and traceability updated, no new Critical/High defects).

## 4. Sprint 0 — Stage B set-up (after G4)

| Objective | Confirm environment and access; prepare the backlog |
|---|---|
| Planned items | Confirm Firebase project/console access (ASM-08); confirm CI secrets inventory (ENG-01 §6); device inventory (ASM-07); create backlog from PLN-02 R0 items; record decisions in PLN-05 §7 |
| Dependencies | G4 |
| Acceptance | Backlog ordered; access confirmed; RAID updated |
| Testing responsibilities | QA Architect: device inventory and test-environment readiness |

## 5. R0 sprints (detailed)

### Sprint 1 — Safe pipeline and first removals

| Story / task | WBS | Est. | Status | Acceptance criteria |
|---|---|---|---|---|
| Disable release-on-push; tag-driven, release-signed only | T-01.3.1 | 1 d | Done | A push to `main` produces no release; a tag produces a release-signed build (`release.yml`, `ReleaseWorkflowPolicyTest`) |
| Commit wrapper; dev-setup guide | T-01.1.1, T-01.1.2 | 1.5 d | Done | Clean clone builds with documented steps (`CONTRIBUTING.md`, `minSdk = 26` per D-14); unit tests run without production `google-services.json` (`AuthHelper`, `TestFirebaseInit`) |
| PR workflow: build + unit + Robolectric + lint | T-01.2.1 | 2 d | Done | PR workflow `.github/workflows/ci.yml` runs `assembleDebug`, `compileReleaseKotlin`, `testDebugUnitTest`, `lintDebug`, and `security-static`; CL-20 `junit` fix applied (`CiWorkflowPolicyTest`) |
| Remove cloud AI | T-04.1.2 | 1 d | Done | `firebase-ai`, `FirebaseAiLogicService`, `GeminiCoachService`, cloud branches, and Settings Cloud AI toggle removed; CI `security-static` + `ZeroCloudAiNetworkBoundaryTest` enforce absence (ADR-003) |
| Stop transcript upload; correct privacy text | T-04.2.1 | 1 d | Done | Saved session/feedback/conversation/attempt payloads omit `transcript`, `transcriptPreview`, `localMediaPath`, and `recordedMediaPath`; Settings privacy copy aligned (`PrivacyAndTranscriptUploadTest`) |
| **Planned load** | | **6.5 d** | **Complete** | |

Dependencies: G4; DEP-05. Testing responsibility: every task adds or updates tests; QA reviews CI evidence.

### Sprint 2 — Safety, privacy and legal removals

| Story / task | WBS | Est. | Status | Acceptance criteria |
|---|---|---|---|---|
| Remove YouTube Intelligence (ingestion, seeded per-user copies) & creator branding; keep catalogue content for re-expression (D-08) | T-04.1.1 | 2.5 d | Done | Zero creator branding or YouTube Intelligence symbols in `app/src/main/` (CI `security-static` Check #6 + `YouTubeRemovalAndBrandingPolicyTest`); tab, ingestion, admin sync, API key UI/prefs, and per-user seeding removed; all 15 bundled catalogue items preserved for EP-09 |
| Remove updater and release collections | T-04.1.3 | 1 d | Todo | No install intents; no GitHub API calls |
| Backup rules; recordings to `noBackupFilesDir` | T-04.2.2 | 1 d | Todo | Backup rules test passes; files located under no-backup dir |
| PII redaction (AppLogger) | T-04.2.3 | 1 d | Todo | Log tests show no uid/email |
| Fix/retire `speakingSessions` write path | T-04.2.4 | 1 d | Todo | Rules emulator test for the save passes |
| **Planned load** | | **6.5 d** | **In progress (1/5)** | |

### Sprint 3 — Architecture shell and speech spike

| Story / task | WBS | Est. | Acceptance criteria |
|---|---|---|---|
| Application + process-scoped AppContainer | T-02.1.1 | 2 d | Rotation does not recreate container (test) |
| Clock/Dispatcher/Logger utilities | T-02.1.2 | 1 d | Unit tests |
| SPK-01 prototype on devices (part 1) | T-05.1 | 3.5 d | Prototype runs on ≥ 2 devices; interim findings logged |
| **Planned load** | | **6.5 d** | |

### Sprint 4 — Packages, navigation, spike completion

| Story / task | WBS | Est. | Acceptance criteria |
|---|---|---|---|
| Package skeleton; repository interfaces + fakes | T-02.2.1, T-02.2.2 | 3 d | Boundary check passes in CI |
| SPK-01 (part 2) + test set + report | T-05.1, T-05.2 | 3.5 d | Report with tier matrix, WER, latency; go/no-go recorded |
| **Planned load** | | **6.5 d** | |

### Sprint 5 — Data foundation and remaining removals

| Story / task | WBS | Est. | Acceptance criteria |
|---|---|---|---|
| Room v1 entities/DAOs (part) | T-03.1.1 | 3 d | Schema exported; DAO tests |
| Remove admin/MediaPipe/CameraX/mentor/conversation/tutorials | T-04.1.4 | 2 d | Dependencies removed; app builds; flags removed |
| Interim account deletion with re-auth | T-04.2.5 | 1.5 d | Emulator test: no docs remain; auth user deleted |
| **Planned load** | | **6.5 d** | |

Note: remaining R0 items (T-02.3.x nav shell, T-03.2.x repositories, T-03.3.1, T-05.3 SPK-02, T-01.4.x upgrades, T-01.2.2 rules CI) spill into Sprints 6–7 per actual velocity; PLN-01 shows R0 ≈ 5 sprints at the optimistic end and up to 7 at the conservative end.

## 6. R1 sprints (indicative)

| Sprint | Objective | Main WBS items | Exit evidence |
|---|---|---|---|
| 6–7 | Finish R0 spill-over; design-system tokens & core components | T-02.3.x, T-03.2.x, T-17.1, T-17.2 | Nav tests; component tests |
| 8–9 | Speech pipeline | T-06.1…T-06.8 | Fixture tests; device smoke |
| 9–11 | Evaluation & feedback; content pack loader; content review starts | T-07.x, T-09.1, T-09.2 | Golden fixtures; validator in CI |
| 11–13 | Plan engine, Today, session player | T-08.x | First end-to-end attempt (M5) |
| 13–15 | Words lifecycle; Practice library | T-11.x, T-10.1 | FS-07/06 acceptance |
| 15–16 | Progress & streak; onboarding | T-12.x, T-13.1, T-13.2 | FS-08/02 acceptance |
| 16–18 | Account, settings, privacy, reminders, sync, migration | T-13.3/4, T-14.x, T-15.1, T-16.x | FS-01/09/10/11 acceptance |
| 18–20 | Quality, performance, accessibility, beta, UAT | T-18.x | G6 pack |

Content tasks (T-09.2…T-09.5) run continuously from Sprint 4 at ~1–2 d per sprint.

## 7. Testing responsibilities per sprint

| Role | Responsibility |
|---|---|
| Developer (agent) | Unit/Robolectric tests with every task; keep CI green |
| QA Architect (agent) | Review test adequacy; maintain fixtures; run device tests at epic end; honesty checklist |
| Product Owner | Review sprint evidence; accept stories; participate in UAT |
