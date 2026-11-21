# Stage A — Status and Resume Checklist

| Field | Value |
|---|---|
| Document ID | RD-STATUS |
| Date | 2026-11-20 (paused) · 2026-11-21 (resumed) |
| Status | **Stage A Approved — Gates G1–G4 and decisions D-01…D-36 approved with recommended defaults on 2026-11-21** (recorded in `PLN-05 §7` and `RD-99 §8`). All Stage A deliverables (items 1–24 in §2), consistency review C-6 (`CL-01`–`CL-92`), PDF books under `docs/books/`, and optional R3+ AI design (`ARC-06` / `ADR-016`, decisions `D-37`–`D-41` deferred to R3) are complete. **Next: explicit `"Start Stage B"` instruction to begin R0 Sprint 1 (GR-01).** |
| Owner (role) | Project Manager |

Stage A is **documentation only**. No product code, configuration, rules or workflow has been changed. Nothing has been built, run, committed or pushed.

---

## 1. Completed deliverables (saved)

| Area | Documents |
|---|---|
| Governance | `docs/governance/DOCUMENT_CONTROL.md` (GOV-02) |
| Discovery (Phase 0) | `docs/redevelopment/00_PROJECT_DISCOVERY.md` · `01_DOCUMENT_INVENTORY.md` · `02_TECH_STACK_AUDIT.md` · `03_SOURCE_CODE_AUDIT.md` · `04_FEATURE_IMPLEMENTATION_MATRIX.md` · `05_DOCUMENT_CONFLICT_REGISTER.md` · `06_EXISTING_SYSTEM_GAP_ANALYSIS.md` |
| Product audit (Phase 1) | `07_PRODUCT_AUDIT.md` · `08_USER_PERSONAS.md` · `09_USER_JOURNEY_MAP.md` · `10_FEATURE_VALUE_ASSESSMENT.md` · `11_COMPETITOR_ANALYSIS.md` · `12_PRODUCT_OPPORTUNITY_ANALYSIS.md` |
| Direction (Phase 2) | `13_PRODUCT_VISION.md` · `14_PRODUCT_STRATEGY.md` · `15_PRODUCT_PRINCIPLES.md` · `16_PRODUCT_SCOPE_AND_NON_GOALS.md` |
| Requirements (Phase 3) | `docs/product/BRD.md` · `docs/product/PRD.md` · `docs/requirements/SRS.md` · `docs/product/FEATURE_SPECIFICATIONS/README.md` (FS-00) · `FS-01_ACCOUNT_AND_SIGN_IN.md` · `FS-02_ONBOARDING_AND_PERSONALISATION.md` |
| UX/UI (Phase 4) | `docs/design/UX_AUDIT.md` (DES-01) · `INFORMATION_ARCHITECTURE.md` (DES-02) · `DESIGN_SYSTEM.md` (DES-05) |
| Architecture (Phase 5) | `docs/architecture/SOLUTION_ARCHITECTURE.md` (ARC-01) · `LOCAL_INTELLIGENCE_ARCHITECTURE.md` (ARC-05) · `ADR/README.md` + ADR-001…ADR-015 · `docs/security/SECURITY_DESIGN.md` (SEC-01) |
| NFR (Phase 6) | `docs/requirements/NFR.md` |
| DevOps (Phase 8) | `docs/engineering/DEVOPS_AND_RELEASE_PLAN.md` (ENG-01) |
| Planning (Phase 9) | `docs/planning/PROJECT_ROADMAP.md` · `WORK_BREAKDOWN_STRUCTURE.md` · `SPRINT_PLAN.md` · `RAID_LOG.md` · `PROJECT_GOVERNANCE.md` |
| Evidence working papers | `docs/redevelopment/working_papers/` — `ui_audit.md`, `data_learning_audit.md`, `ai_media_audit.md`, `platform_security_audit.md`, `docs_claims.md`, `competitor_research.md`, `FND_ID_MAP.md` (canonical finding IDs), `PLAN_AND_CANON.md` (canonical IDs, decisions, data skeleton, remaining work) |

## 2. Not yet written (in progress when paused — regenerate on resume)

| # | Document | Path | Inputs to read first |
|---|---|---|---|
| 1 | FS-03 Today — daily session | `docs/product/FEATURE_SPECIFICATIONS/FS-03_TODAY_DAILY_SESSION.md` | PRD §15.3, SRS §6.3 + SRS-BR-013/014/027, ARC-05, FS-01 (as format example) |
| 2 | FS-04 Speaking practice session | `…/FS-04_SPEAKING_PRACTICE_SESSION.md` | SRS §6.4, ARC-05 §3–5, ADR-009/015 |
| 3 | FS-05 Honest feedback & evaluation | `…/FS-05_HONEST_FEEDBACK_AND_EVALUATION.md` | SRS §6.5, ARC-05 §5–9 |
| 4 | FS-06 Practice library | `…/FS-06_PRACTICE_LIBRARY.md` | SRS §6.6, RD-16 CAP-06 |
| 5 | FS-07 Words — vocabulary lifecycle | `…/FS-07_WORDS_VOCABULARY_LIFECYCLE.md` | SRS §6.7, ARC-05 §9, SRS-BR-015…017 |
| 6 | FS-08 Progress & streak | `…/FS-08_PROGRESS_AND_STREAK.md` | SRS §6.8, SRS-BR-010…012, -018, -024 |
| 7 | FS-09 Reminders & inbox | `…/FS-09_REMINDERS_AND_INBOX.md` | SRS §6.9, ADR-005, SRS-BR-019 |
| 8 | FS-10 Profile, settings, privacy & data rights | `…/FS-10_PROFILE_SETTINGS_PRIVACY_DATA_RIGHTS.md` | SRS §6.10, SEC-01 §9 |
| 9 | FS-11 Offline-first storage & sync | `…/FS-11_OFFLINE_FIRST_STORAGE_AND_SYNC.md` | SRS §6.11, ADR-002/014, SEC-01 §5 |
| 10 | FS-12 Speech capability management | `…/FS-12_SPEECH_CAPABILITY_MANAGEMENT.md` | SRS §6.12, ARC-05 §1, §3–4, ADR-009 |
| 11 | User stories | `docs/product/USER_STORIES.md` | PRD §7–8, §15; SRS; FS index |
| 12 | Use cases | `docs/requirements/USE_CASES.md` | SRS; FS |
| 13 | DES-03 Screen inventory | `docs/design/SCREEN_INVENTORY.md` | DES-02 §4 (screen IDs), DES-05 |
| 14 | DES-04 User flows | `docs/design/USER_FLOWS.md` | DES-02, SRS, working_papers/ui_audit.md §3 |
| 15 | DES-06 UI/UX specifications (incl. §2.4 contextual tips) | `docs/design/UI_UX_SPECIFICATIONS.md` | DES-02, DES-05, ARC-05 §7–9 |
| 16 | DES-07 Accessibility guidelines | `docs/design/ACCESSIBILITY_GUIDELINES.md` | DES-01, DES-05 (A11Y/TC-A11Y IDs already referenced there) |
| 17 | DES-08 Redesign decisions | `docs/design/REDESIGN_DECISIONS.md` | RD-13…16, DES-02 |
| 18 | ARC-02 Technical design | `docs/architecture/TECHNICAL_DESIGN.md` | ARC-01, ARC-05, ADRs |
| 19 | ARC-03 Database design (keep section numbers §1–§12 as referenced: §6 content pack, §8 sync fields, §9 migrations, §10 export schema) | `docs/architecture/DATABASE_DESIGN.md` | PLAN_AND_CANON data skeleton, ENT-01…ENT-18, SEC-01 §5 |
| 20 | ARC-04 API & integration spec | `docs/architecture/API_AND_INTEGRATION_SPEC.md` | ARC-01 §2, working_papers/platform_security_audit.md |
| 21 | TST-01 Test strategy (keep §5 devices, §9 honesty checklist, §10 entry/exit) | `docs/testing/TEST_STRATEGY.md` | SRS, NFR, SEC-01 §13, FS test-case ranges |
| 22 | GOV-01 Requirement traceability matrix | `docs/governance/REQUIREMENT_TRACEABILITY_MATRIX.md` | All of the above (written last, before RD-99) |
| 23 | RD-99 Final redevelopment review (decisions D-01…D-31 in §8) | `docs/redevelopment/FINAL_REDEVELOPMENT_REVIEW.md` | Everything; consistency review results |
| 24 | CNT-01 Technique catalogue audit (added 2026-11-21 at the Product Owner's request: "check" the creator-derived content already in the app) | `docs/product/TECHNIQUE_CATALOGUE_AUDIT.md` | `CurriculumCatalog.kt` and related code; ARC-05; SRS-BR-028 |

### 2.1 Product Owner direction received on 2026-11-21 (applied to PRD v0.2 / SRS v0.2; relayed to running writers)
- Aim: curriculum built from the techniques, frameworks, exercises, activities and tips of the @askvinh channel (PRD-FR-056).
- **D-08 DECIDED — "original wording, link out"** (PRD-FR-057, SRS-BR-028).
- Mistakes found + coach tips after every submitted attempt (PRD-FR-047; SRS-FR-EVAL-015/016, SRS-FR-WORD-014).
- **Done 2026-11-21 (v0.2 of each):** PRD (PRD-FR-047/056/057), SRS (EVAL-015/016, PRAC-008/009, WORD-014, SRS-BR-028), RD-10 row 9, RD-12 OPP-15, RD-13 §6/§9, RD-14 (scope note, RSK-04/05, D-08 row), RD-16 (CAP-05/06, NG-08, retired features), BRD §1/§2, ADR-011 (ingestion and branding only), DES-02 (Techniques collection + new **SCR-PRAC-04**; 38 R1 screens), ARC-05 §8.4 (issue catalogue, tip selection, self-check, candidate acoustic metrics), PLN-01/02/03/04/05 (T-07.8, T-09.6–9.8, T-10.2; RSK-17/18; D-08 recorded as the first approved decision).
- **Still to do:** GOV-02 index (CNT-01), FS-00 index, final consistency pass once the writers and CNT-01 finish.

## 3. Known consistency items to apply on resume

| # | Item | Action |
|---|---|---|
| C-1 | Sign-out default reconciled as decision **D-21** in SRS-FR-ACC-006 and SEC-REQ-04; FS-01 may still state the earlier default | Check FS-01 and align to D-21 |
| C-2 | Export audio: SEC-01 SD-04 recommends JSON-only for R1; SRS-FR-SET-004 / WBS T-14.2 include an optional recordings ZIP | Consolidated as **D-22**; reflect the recommendation in RD-99 and FS-10 |
| C-3 | SEC-01 §12 NFR mapping corrected to real NFR IDs (done); NFR §4 now points to SEC-01 §12 (done) | — |
| C-4 | Writer briefs referenced scratchpad paths; evidence now lives in `docs/redevelopment/working_papers/` | Use the new paths when regenerating |
| C-5 | DES-06 must align to DES-05 token/component names (DES-05 now exists) | Apply when writing DES-06 |
| C-6 | Final cross-document consistency review (PRD↔SRS↔FS↔US; IA↔screens; ARC↔DB↔SEC; tests↔requirements; roadmap↔dependencies) | Perform before writing RD-99 |

## 4. Decisions awaiting the Product Owner

D-01…D-20 (see RD-14 §10, ADR index, and `working_papers/PLAN_AND_CANON.md`), plus D-21…D-31 consolidated from SEC-01 §14 and ENG-01 §13. The full list with options and recommendations will be in RD-99 §8.

## 5. Resume order

1. Regenerate items 1–21 in §2 (parallelisable: FS-03…06 | FS-07…12 | US + UC | DES-03 + DES-04 | DES-06 + DES-07 + DES-08 | ARC-02 + ARC-03 + ARC-04 | TST-01).
2. Apply §3 consistency items.
3. Write GOV-01 (RTM), then run the full consistency review.
4. Write RD-99 and update GOV-02 statuses.
5. Present the Stage A completion summary and **stop for approval** (no implementation before G4).
