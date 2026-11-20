# Project Governance and Approvals

| Field | Value |
|---|---|
| Document ID | PLN-05 |
| Version | 0.2 — Draft for review (2026-11-21: first decision recorded in §7 — D-08) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Project Manager |
| Approval | Pending — Product Owner |
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
| G1 Product approval | — | Pending | |
| G2 UX approval | — | Pending | |
| G3 Architecture & security approval | — | Pending | |
| G4 Implementation approval | — | Pending | |
| D-08 Creator-derived content | 2026-11-21 | **Approved — Option "original wording, link out"** | Product Owner's answer in the Stage A session. The curriculum is built from the @askvinh channel's frameworks, techniques, exercises, activities and tips, re-expressed in our own words, each linked to its specific source video, with no creator name, likeness or branding (PRD-FR-056/057, SRS-BR-028). Recorded together with the Product Owner's direction that every submitted attempt gets mistakes found + coach tips (PRD-FR-047). Under GR-06 this changes RD-16 §4/§5 and RD-14; the affected documents were updated on 2026-11-21 |
| D-01…D-07, D-09…D-36 | — | Pending | See RD-99 §8 (gate per decision in RD-99 §10) |

## 8. Reporting cadence (Stage B)

| Report | Frequency | Content |
|---|---|---|
| Sprint review note | Every 2 weeks | Completed stories with evidence, demo, metrics, RAID changes |
| Epic review pack | Per epic (G5) | PRs, CI report, docs diff, open defects |
| Release report | Per release (G6/G7) | Test summary, checklist, KPIs (beta) |
