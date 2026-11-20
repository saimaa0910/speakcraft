# Document Control — SpeakCraft Redevelopment Documentation

| Field | Value |
|---|---|
| Document ID | GOV-02 |
| Version | 0.2 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting product owner approval |
| Owner (role) | Technical Writer / Project Manager |
| Approval | Pending — Product Owner |
| Related | GOV-01 `REQUIREMENT_TRACEABILITY_MATRIX.md`, PLN-05 `PROJECT_GOVERNANCE.md`, RD-01 `01_DOCUMENT_INVENTORY.md` |

---

## 1. Purpose

This document is the single index and control standard for the SpeakCraft redevelopment documentation set produced in **Stage A (Discovery and Planning)**. It defines:

1. How documents are identified, versioned and approved.
2. Which document is **authoritative** for each kind of information (so the same requirement is never described differently in two places).
3. The full index of Stage A deliverables and their status.
4. How the 59 pre-existing documents relate to the new set.

## 2. Ground rules

| Rule | Description |
|---|---|
| DC-1 | Pre-existing documents in `docs/` (top level), `app/docs/` and `README_DOCUMENTATION_INDEX.md` are **preserved unchanged**. They are historical reference. Their reliability is graded in RD-01 and their contradictions are logged in RD-05. |
| DC-2 | Every requirement has **exactly one authoritative home** (see §4). Other documents reference the ID; they do not restate the requirement in different words. |
| DC-3 | Every statement in an audit document is labelled as one of: **VERIFIED** (seen in source/config with `path:line` evidence), **INFERRED** (reasoned from evidence but not directly observed), **ASSUMPTION** (no evidence; needs validation), **RECOMMENDATION** (proposal), or **DECISION** (approved by the Product Owner; as of 2026-11-21 one exists: D-08, recorded in PLN-05 §7). |
| DC-4 | No test result is recorded as passed unless it was executed and its output captured. In Stage A **no build or test was executed** (no JDK/Android SDK in the analysis environment). |
| DC-5 | Proposed targets (performance, thresholds, estimates) are marked **Proposed — requires approval** until the Product Owner approves them. |
| DC-6 | Technology changes are only proposed through an ADR in `docs/architecture/ADR/` with status **Proposed — requires explicit approval**. |

## 3. Identification and versioning

### 3.1 Document IDs

| Prefix | Folder | Meaning |
|---|---|---|
| RD-nn | `docs/redevelopment/` | Discovery, audit and product-direction documents (RD-00 … RD-16, RD-99 final review) |
| PRD / BRD / US | `docs/product/` | Product requirements, business requirements, user stories |
| CNT-nn | `docs/product/` | Content catalogue and content audits (CNT-01 technique catalogue audit) |
| FS-nn | `docs/product/FEATURE_SPECIFICATIONS/` | Feature specifications (one per approved capability) |
| SRS / NFR / UC | `docs/requirements/` | Software requirements, non-functional requirements, use cases |
| DES-nn | `docs/design/` | UX/UI design documents |
| ARC-nn / ADR-nnn | `docs/architecture/` | Architecture and architecture decision records |
| SEC-nn | `docs/security/` | Security design |
| TST-nn | `docs/testing/` | Test strategy |
| ENG-nn | `docs/engineering/` | DevOps and release |
| PLN-nn | `docs/planning/` | Roadmap, WBS, sprint plan, RAID, governance |
| GOV-nn | `docs/governance/` | Traceability and document control |
| DOC-nnn | (RD-01) | Identifiers assigned to the **pre-existing** documents |

### 3.2 Requirement and artefact IDs

| ID pattern | Defined in (authoritative) | Example |
|---|---|---|
| `BO-nn` business objective | BRD | BO-01 |
| `BR-nnn` business requirement | BRD | BR-004 |
| `PRD-FR-nnn` product functional requirement | PRD | PRD-FR-012 |
| `PRD-NFR-nnn` product quality expectation | PRD (detailed in NFR) | PRD-NFR-003 |
| `SRS-FR-<AREA>-nnn` software functional requirement | SRS | SRS-FR-EVAL-004 |
| `NFR-<CAT>-nnn` measurable non-functional requirement | NFR | NFR-PERF-002 |
| `SRS-BR-nnn` business rule | SRS §16 | SRS-BR-007 |
| `US-nnn` user story | USER_STORIES | US-021 |
| `UC-nn` use case | USE_CASES | UC-04 |
| `FS-nn` feature specification | FEATURE_SPECIFICATIONS | FS-03 |
| `SCR-<AREA>-nn` screen | DES-03 Screen Inventory | SCR-PRAC-02 |
| `FLOW-nn` user flow | DES-04 User Flows | FLOW-03 |
| `CMP-nn` architecture component | ARC-02 Technical Design | CMP-07 |
| `ENT-nn` data entity | ARC-03 Database Design | ENT-05 |
| `THR-nn` threat | SEC-01 | THR-04 |
| `TC-<AREA>-nnn` test case | TST-01 / FS-nn | TC-EVAL-003 |
| `IN-n / EP-nn / F-nn.n / T-nn.n.n` work items | PLN-02 WBS | EP-03, T-03.2.1 |
| `R0…R3` release milestones | PLN-01 Roadmap | R1 |
| `D-nn` open decision | RD-99 Final Review | D-04 |
| `RSK / ASM / ISS / DEP-nn` RAID items | PLN-04 RAID log | RSK-07 |
| `CONF-nnn` documentation conflict | RD-05 | CONF-003 |
| `GAP-nn` gap | RD-06 | GAP-11 |
| `FND-<AREA>-nn` audit finding | RD-03 | FND-ARCH-04 |

### 3.3 Versions and status lifecycle

`0.x Draft` → `0.9 Review` → `1.0 Approved` → `1.x Revised`.

Status values: **Draft — awaiting approval**, **In review**, **Approved**, **Superseded**, **Archived (historical)**.

All Stage A documents are issued as **v0.1 Draft — awaiting Product Owner approval**.

### 3.4 Standard header

Every new document begins with the table: Document ID, Version, Date, Status, Owner (role), Approval, Related.

## 4. Authoritative source map

| Information | Authoritative document | Others may only reference |
|---|---|---|
| Business objectives and success measures | BRD (`BO-nn`) | Vision, PRD, RTM |
| Product scope, product requirements, release scope | PRD (`PRD-FR`, `PRD-NFR`) + RD-16 scope | SRS, stories, roadmap |
| Detailed, testable system behaviour | SRS (`SRS-FR`, `SRS-BR`) | Feature specs (elaborate workflow/UI, do not redefine rules) |
| Measurable quality targets | NFR (`NFR-*`) | SRS, test strategy, architecture |
| Workflow, UI states and per-feature test cases | Feature specifications (`FS-nn`) | Design specs |
| Screens and navigation | DES-02 IA + DES-03 Screen Inventory | UI specs, flows |
| Visual tokens and components | DES-05 Design System | UI specs |
| Components, layering, data flow | ARC-01 / ARC-02 | — |
| Persistent data model | ARC-03 Database Design | SRS data requirements |
| Evaluation / intelligence behaviour | ARC-05 Local Intelligence Architecture (+ SRS-FR-EVAL) | Feature specs |
| Security controls and threat model | SEC-01 | SRS security section |
| Test levels, environments, exit criteria, defect severity | TST-01 (§11 severity) | Feature-spec test cases, ENG-01, PLN-05 |
| Evaluation issue codes, tip selection, metric bands | ARC-05 §6, §8.4 | FS-05, FS-07, DES-05/06, ARC-02/03, TST-01 |
| Content schema (techniques, tips, provenance, `licenceStatus`) | ARC-03 §6 (+ SRS-BR-028 rule) | FS-06, CNT-01 |
| Inventory of existing creator-derived content and its verification | CNT-01 Technique Catalogue Audit | PLN-02 EP-09 |
| Work items, estimates, sequence | PLN-02 WBS + PLN-01 Roadmap | Sprint plan |
| Risks, assumptions, issues, dependencies | PLN-04 RAID | All |
| Open decisions requiring approval | RD-99 Final Review §8 | All |
| Requirement-to-test linkage | GOV-01 RTM | All |

## 5. Stage A document index

| ID | Path | Title | Owner (role) | Status |
|---|---|---|---|---|
| RD-00 | `docs/redevelopment/00_PROJECT_DISCOVERY.md` | Project Discovery | Product Manager / Solution Architect | Draft |
| RD-01 | `docs/redevelopment/01_DOCUMENT_INVENTORY.md` | Document Inventory | Technical Writer / BA | Draft |
| RD-02 | `docs/redevelopment/02_TECH_STACK_AUDIT.md` | Technology Stack Audit | Solution Architect | Draft |
| RD-03 | `docs/redevelopment/03_SOURCE_CODE_AUDIT.md` | Source Code Audit | Android Tech Lead | Draft |
| RD-04 | `docs/redevelopment/04_FEATURE_IMPLEMENTATION_MATRIX.md` | Feature Implementation Matrix | Product Analyst / QA Architect | Draft |
| RD-05 | `docs/redevelopment/05_DOCUMENT_CONFLICT_REGISTER.md` | Document Conflict Register | Technical Writer / BA | Draft |
| RD-06 | `docs/redevelopment/06_EXISTING_SYSTEM_GAP_ANALYSIS.md` | Existing System Gap Analysis | Business Analyst | Draft |
| RD-07 | `docs/redevelopment/07_PRODUCT_AUDIT.md` | Product Audit | CPO | Draft |
| RD-08 | `docs/redevelopment/08_USER_PERSONAS.md` | User Personas | UX Researcher | Draft |
| RD-09 | `docs/redevelopment/09_USER_JOURNEY_MAP.md` | User Journey Map | UX Strategist | Draft |
| RD-10 | `docs/redevelopment/10_FEATURE_VALUE_ASSESSMENT.md` | Feature Value Assessment | Product Manager | Draft |
| RD-11 | `docs/redevelopment/11_COMPETITOR_ANALYSIS.md` | Competitor Analysis | Product Analyst | Draft |
| RD-12 | `docs/redevelopment/12_PRODUCT_OPPORTUNITY_ANALYSIS.md` | Product Opportunity Analysis | CPO / Product Manager | Draft |
| RD-13 | `docs/redevelopment/13_PRODUCT_VISION.md` | Product Vision | CPO | Draft |
| RD-14 | `docs/redevelopment/14_PRODUCT_STRATEGY.md` | Product Strategy | CPO | Draft |
| RD-15 | `docs/redevelopment/15_PRODUCT_PRINCIPLES.md` | Product Principles | CPO / UX Strategist | Draft |
| RD-16 | `docs/redevelopment/16_PRODUCT_SCOPE_AND_NON_GOALS.md` | Product Scope and Non-Goals | Product Manager | Draft |
| RD-99 | `docs/redevelopment/FINAL_REDEVELOPMENT_REVIEW.md` | Final Redevelopment Review | CPO / Project Manager | Draft |
| PRD | `docs/product/PRD.md` | Product Requirements Document | Product Manager | Draft |
| BRD | `docs/product/BRD.md` | Business Requirements Document | Business Analyst | Draft |
| US | `docs/product/USER_STORIES.md` | User Stories | Product Manager | Draft |
| FS-00 | `docs/product/FEATURE_SPECIFICATIONS/README.md` | Feature Specification Index | Product Manager | Draft |
| FS-01…FS-12 | `docs/product/FEATURE_SPECIFICATIONS/FS-nn_*.md` (12 files) | Feature Specifications | Product Manager / BA | Draft |
| CNT-01 | `docs/product/TECHNIQUE_CATALOGUE_AUDIT.md` | Technique Catalogue Audit (existing creator-derived content; added 2026-11-21) | Content Strategist / BA | Draft |
| SRS | `docs/requirements/SRS.md` | Software Requirements Specification | Business Analyst / Solution Architect | Draft |
| NFR | `docs/requirements/NFR.md` | Non-Functional Requirements | Solution Architect / QA Architect | Draft |
| UC | `docs/requirements/USE_CASES.md` | Use Cases | Business Analyst | Draft |
| DES-01 | `docs/design/UX_AUDIT.md` | UX Audit | UX Researcher | Draft |
| DES-02 | `docs/design/INFORMATION_ARCHITECTURE.md` | Information Architecture | UX Strategist | Draft |
| DES-03 | `docs/design/SCREEN_INVENTORY.md` | Screen Inventory | UI/UX Designer | Draft |
| DES-04 | `docs/design/USER_FLOWS.md` | User Flows | UX Strategist | Draft |
| DES-05 | `docs/design/DESIGN_SYSTEM.md` | Design System | Design System Architect | Draft |
| DES-06 | `docs/design/UI_UX_SPECIFICATIONS.md` | UI/UX Specifications | UI/UX Designer | Draft |
| DES-07 | `docs/design/ACCESSIBILITY_GUIDELINES.md` | Accessibility Guidelines | UI/UX Designer / QA | Draft |
| DES-08 | `docs/design/REDESIGN_DECISIONS.md` | Redesign Decisions | UX Strategist | Draft |
| ARC-01 | `docs/architecture/SOLUTION_ARCHITECTURE.md` | Solution Architecture | Solution Architect | Draft |
| ARC-02 | `docs/architecture/TECHNICAL_DESIGN.md` | Technical Design | Android Tech Lead | Draft |
| ARC-03 | `docs/architecture/DATABASE_DESIGN.md` | Database Design | Database Architect | Draft |
| ARC-04 | `docs/architecture/API_AND_INTEGRATION_SPEC.md` | API and Integration Specification | Solution Architect | Draft |
| ARC-05 | `docs/architecture/LOCAL_INTELLIGENCE_ARCHITECTURE.md` | Local Intelligence Architecture | AI/ML Engineer | Draft |
| ADR-nnn | `docs/architecture/ADR/` | Architecture Decision Records | Solution Architect | Proposed |
| SEC-01 | `docs/security/SECURITY_DESIGN.md` | Security Design | Security Engineer | Draft |
| TST-01 | `docs/testing/TEST_STRATEGY.md` | Test Strategy | QA Architect | Draft |
| ENG-01 | `docs/engineering/DEVOPS_AND_RELEASE_PLAN.md` | DevOps and Release Plan | DevOps / Release Engineer | Draft |
| PLN-01 | `docs/planning/PROJECT_ROADMAP.md` | Project Roadmap | Project Manager | Draft |
| PLN-02 | `docs/planning/WORK_BREAKDOWN_STRUCTURE.md` | Work Breakdown Structure | Project Manager / Tech Lead | Draft |
| PLN-03 | `docs/planning/SPRINT_PLAN.md` | Sprint Plan | Project Manager | Draft |
| PLN-04 | `docs/planning/RAID_LOG.md` | RAID Log | Project Manager | Draft |
| PLN-05 | `docs/planning/PROJECT_GOVERNANCE.md` | Project Governance | Project Manager | Draft |
| GOV-01 | `docs/governance/REQUIREMENT_TRACEABILITY_MATRIX.md` | Requirement Traceability Matrix | BA / QA Architect | Draft |
| GOV-02 | `docs/governance/DOCUMENT_CONTROL.md` | Document Control (this document) | Technical Writer | Draft |
| BOOK-01 | `docs/books/SpeakCraft_Book1_Product_Planning.pdf` (source `docs/books/src/book1_product_planning.html`) | Book 1 — Product Planning (edited compilation, Product Owner request 2026-11-21; not authoritative — the source documents win) | Technical Writer / Product Manager | Draft |
| BOOK-02 | `docs/books/SpeakCraft_Book2_System_Design_and_Delivery.pdf` (sources `docs/books/src/book2_*.html`; build `build_books.ps1`) | Book 2 — System Design & Delivery (edited compilation; not authoritative) | Technical Writer / Solution Architect | Draft |

## 6. Relationship to pre-existing documentation

The 59 pre-existing documents (48 in `docs/`, 6 in `app/docs/`, `README_DOCUMENTATION_INDEX.md`, `app/src/main/assets/models/README.md`, `metadata.json`, `firebase-blueprint.json`, and the Product Owner's 19-Nov request PO-REQ-1119 / DOC-059) are inventoried as `DOC-001…DOC-059` in RD-01, with a recommended disposition for each (Keep as reference / Superseded by / Archive). **None have been modified or deleted.** After the Product Owner approves Stage A, superseded documents should be moved to `docs/archive/` in a separate, reviewable commit, so the active documentation set contains one authoritative description per topic.

### 6.1 Documents required by PO-REQ-1119 (DOC-059)

PO-REQ-1119 required ten documents that were never produced (RD-05 CONF-044). Their intended content is covered by the Stage A set as follows; the separate files are therefore **superseded** rather than created (RD-05 register item D8):

| Required document (PO-REQ-1119) | Covered by |
|---|---|
| `docs/WELCOME_EXPERIENCE.md` | FS-03 (greeting, SRS-FR-TODAY-010), DES-06 §3 (SCR-TODAY-01) |
| `docs/PERSONALIZED_LEARNING_ENGINE.md` | FS-03, SRS §6.3 and SRS-BR-013/014/027 |
| `docs/DASHBOARD_AND_STREAK.md` | FS-03 (Today), FS-08 (progress & streak), SRS-BR-010…012 |
| `docs/NOTIFICATION_CENTER.md` | FS-09 (reminders R1, inbox R2) |
| `docs/PROFILE_AND_SETTINGS.md` | FS-10 |
| `docs/ADMIN_CONSOLE_SECURITY.md` | RD-16 NG-10 (descoped), SEC-01 (missing server-side authorisation documented) |
| `docs/VOCABULARY_BUILDER.md` | FS-07 |
| `docs/LEARNING_PROGRESS_ARCHITECTURE.md` | ADR-014, ARC-03, FS-11 |
| `docs/DATABASE_MIGRATIONS.md` | ARC-03 §9 |
| `docs/IMPLEMENTATION_AND_TEST_REPORT.md` | Produced per epic and release in Stage B (PLN-05 §8; TST-01) |

## 7. Change control

1. Changes to an **Approved** document require a version increment, a change-log entry and re-approval by the role listed in PLN-05.
2. A change to a requirement ID's meaning is not allowed. Retire the old ID (mark *Withdrawn*) and create a new one.
3. When a change affects traceability (requirement ↔ story ↔ test), GOV-01 is updated in the same change.

## 8. Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial Stage A document set index and control standard | Technical Writer |
| 0.2 | 2026-11-21 | All Stage A deliverables written; CNT-01 added (Product Owner request); authority entries for defect severity, issue codes/tip selection, content schema and content inventory; 59 pre-existing documents; DC-3 note: first DECISION recorded (D-08, PLN-05 §7) | Technical Writer |
