# SpeakCraft — Document Inventory

| Field | Value |
|---|---|
| Document ID | RD-01 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting product owner approval |
| Owner (role) | Technical Writer / Business Analyst |
| Approval | Pending |
| Related | docs/redevelopment/00_PROJECT_DISCOVERY.md, 04_FEATURE_IMPLEMENTATION_MATRIX.md |

---

## 1. Purpose

This inventory lists every documentation artefact in the SpeakCraft repository. For each one it records what the document is, when it was added, whether it still describes the product, and how far its claims can be trusted. The project was built through many AI coding-agent prompts in two days (2026-11-18 to 2026-11-20). As a result its documentation is large, overlapping and often contradictory.

The inventory is an input to the product rediscovery. It does **not** decide which product direction is correct. Contradictions are recorded in RD-05 (`05_DOCUMENT_CONFLICT_REGISTER.md`), and each one is cross-referenced here by a `CONF-nnn` ID.

No existing file was modified, moved or deleted.

## 2. Scope and method

### 2.1 Documents in scope (59)

| Location | Count | Notes |
|---|---:|---|
| `docs/*.md` (top level) | 48 | `docs/redevelopment/` is excluded because it is new output. |
| `app/docs/*.md` | 6 | Added in the latest commit (`68a9cb1`). |
| `README_DOCUMENTATION_INDEX.md` (repo root) | 1 | The only root-level Markdown file. The original AI Studio template `README.md` was deleted in `330ca3e`. |
| `app/src/main/assets/models/README.md` | 1 | Model catalogue note. |
| `metadata.json`, `firebase-blueprint.json` | 2 | Platform metadata and Firestore schema blueprint. The blueprint was skimmed: entity list and paths only. |
| `public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484` | 1 | A product-request prompt given to an AI coding agent. It is treated as a requirements document, not as instructions. |

Related artefact, not inventoried: `public/assets/aistudio/image_bf3fea26.png` (2.1 MB, added in `08972b7`). The Paste prompt calls it "the supplied screenshot" and uses it as the visual reference for the welcome carousel. It is a binary image and was not reviewed.

### 2.2 How the evidence was gathered

1. **Every in-scope file was read in full**, except `firebase-blueprint.json`, which was skimmed as agreed.
2. **Dates** come from `git log --follow --format='%h %ad %s' --date=short -- <file>`. "First added" is the oldest commit and "Last modified" is the newest. The whole repository has 26 commits dated 2026-11-18 to 2026-11-20. Nearly every document has exactly one commit, so the in-document "Revision Date" headers are often meaningless. For example, the index says "2026-11-18" but was last changed on 2026-11-19.
3. **Static spot checks against source code** at HEAD (`68a9cb1`, 2026-11-20). These used `grep`/`read` only and are not an exhaustive code audit; other analysts cover that. Checks performed:
   - `app/build.gradle.kts` and `gradle/libs.versions.toml`: version, SDK levels, AI dependencies (`firebase-ai`, LiteRT, TFLite, MediaPipe, Retrofit, Room).
   - Cloud-AI call sites: `Firebase.ai`, `generativeModel`, `generateContent`, `startChat`, `gemini-`. Also the gating in `HybridAiCoachCoordinator` and `LocalExecutionPolicy`, and whether the OkHttp interceptor is attached to any client.
   - `AppNavigation.kt`: bottom-bar items, `composable()` destinations and layout breakpoints.
   - `@Test` annotations per test class at HEAD. The count was also replayed for 14 historical commits by reading old file versions with `git show`.
   - Activity catalogue IDs per module, `ExerciseTemplateType` values, evaluator classes, learning-path IDs and the 15-module `CurriculumCatalog`.
   - Theme tokens (`Color.kt`, `Theme.kt`, `Type.kt`, `Spacing.kt`) and window colours (`res/values*/colors.xml`).
   - `AndroidManifest.xml`: activities and permissions.
   - `assets/models/` contents, `OnDeviceModelManager` model specs, `DeviceCapabilityManager` thresholds, `AudioResampler` limits and video frame scaling.
   - `firestore.rules` match paths and blueprint paths.
   - `.github/workflows/release_on_push.yml`: keystore source, secrets and whether a test step exists. Also `git tag`.
   - Whether classes named in the docs exist (for example `VideoReviewPlayerCard`, `CloudSyncWarningBanner`, `ScenarioScriptBank`, `EvaluationEvidenceValidator`).
4. **No code was executed.** There was no build, no unit test run, no emulator and no Firestore emulator. Every statement in the documents that something "passed", "built", "deployed" or was "verified on device" was therefore **not reproduced**. Where a claim gives a count, the count was compared statically with the code at HEAD or at the commit that introduced the document.

### 2.3 Status definitions

| Status | Meaning |
|---|---|
| **Current** | The most recent document on its topic; no later document replaces it. Accuracy is assessed separately under *Reliability*, so a Current document can still be contradicted by code. |
| **Outdated** | No later document replaces it, but later commits changed the code, so material statements no longer describe the repository. |
| **Superseded** | A later document covers the same topic and replaces it. This is often within the same commit; see Stage 1 vs Stage 1.2. |
| **Draft** | A proposal or plan whose implementation is incomplete or not evidenced. |
| **Unknown** | Status cannot be determined. *(No document needed this value.)* |

### 2.4 Reliability definitions

| Reliability | Meaning |
|---|---|
| **Verified** | The key checkable claims were spot-checked and match HEAD. |
| **Partially verified** | Some key claims match HEAD. Others are unverifiable or show minor mismatches. |
| **Unverified** | The claims could not be checked against the repository. Examples: benchmarks, device runs, deployments, market analysis. |
| **Contradicted by code** | One or more core claims are contradicted by code or configuration at HEAD. |

---

## 3. Inventory table

Dates are shown as `YYYY-MM-DD hash`. Where only one commit touched a file, "first" and "last" are the same commit. CONF IDs refer to RD-05.

| Doc ID | File path | Document title (actual H1) | Type | Version (as stated) | First added / Last modified | Status | Reliability | Dependencies | Conflicts |
|---|---|---|---|---|---|---|---|---|---|
| DOC-001 | `docs/01_UI_UX_DESIGN_DOCUMENTATION.md` | 01 — UI and UX Design Documentation | Audit (baseline UI/UX) | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Superseded | Contradicted by code | DOC-004, 005, 008, 011 | CONF-007, 024, 025, 026, 033, 036, 047, 049, 054 |
| DOC-002 | `docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md` | 02 — Backend Architecture Documentation | Architecture (baseline audit) | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Outdated | Partially verified | DOC-003, 004, 006, 007 | CONF-007, 043, 048, 049, 050 |
| DOC-003 | `docs/03_USER_SCALABILITY_DOCUMENTATION.md` | 03 — User Scalability Documentation | Architecture / capacity estimate | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Outdated | Unverified | DOC-002, 004, 007 | CONF-007, 031, 049 |
| DOC-004 | `docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md` | 04 — Issues, Bugs, and Technical Debt Audit | Audit (issue register ISS-001..028) + roadmap | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Outdated | Contradicted by code | DOC-001, 002, 006, 007, 017 | CONF-028, 029, 038, 042, 047, 049, 054 |
| DOC-005 | `docs/05_COMPLETE_FEATURE_DOCUMENTATION.md` | 05 — Complete Feature Documentation | Spec / feature inventory (FEAT-001..040) + roadmap | Rev. 2026-11-18; "Current Baseline (v1.0.0)" | 2026-11-18 `1f77749` (only) | Outdated | Partially verified | DOC-001, 004, 006 | CONF-007, 026, 027, 032, 037, 042, 043, 049 |
| DOC-006 | `docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md` | 06 — AI Features and AI Architecture | Architecture (cloud AI) | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Outdated | Contradicted by code | DOC-002, 004, 005 | CONF-007, 011, 050 |
| DOC-007 | `docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md` | 07 — Database Design and Documentation | Architecture / data model audit | Rev. 2026-11-18 | 2026-11-18 `1f77749` (only) | Outdated | Partially verified | DOC-002, 029, 058 | CONF-030, 042, 048, 049 |
| DOC-008 | `docs/UI_UX_CURRENT_STATE_AUDIT.md` | SpeakCraft — UI/UX Current State Audit | Audit (pre-redesign UX) | Audit date 2026-11-18; app "v1.0.0" | 2026-11-18 `7ed7a24` (only) | Superseded | Partially verified | DOC-001, 004, 010 | CONF-007, 024, 037, 054 |
| DOC-009 | `docs/COMPETITIVE_UX_ANALYSIS.md` | SpeakCraft — Competitive UX Analysis & Differentiation Strategy | Analysis / positioning | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Current | Unverified | DOC-008, 011 | CONF-033 |
| DOC-010 | `docs/SPEAKCRAFT_NEW_INFORMATION_ARCHITECTURE.md` | SpeakCraft — New Information Architecture (IA) | Spec (IA proposal) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Draft | Partially verified | DOC-008, 012 | CONF-021, 022, 023, 024, 025 |
| DOC-011 | `docs/SPEAKCRAFT_DESIGN_SYSTEM.md` | SpeakCraft Design System Specification | Spec (design system) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Outdated | Contradicted by code | DOC-001, 014, 017 | CONF-033, 034, 036 |
| DOC-012 | `docs/SCREEN_BY_SCREEN_REDESIGN_SPECIFICATION.md` | SpeakCraft — Screen-by-Screen Redesign Specification | Spec (screen redesign) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Outdated | Partially verified | DOC-010, 011, 015 | CONF-023, 024, 025, 047 |
| DOC-013 | `docs/INTERACTION_AND_MOTION_GUIDELINES.md` | SpeakCraft — Interaction & Motion Guidelines | Guideline (motion/microcopy) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Current | Partially verified | DOC-011, 014 | — |
| DOC-014 | `docs/ACCESSIBILITY_AUDIT.md` | SpeakCraft — Accessibility Audit & Remediation Plan | Audit (accessibility) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Outdated | Partially verified | DOC-011, 013 | CONF-034 |
| DOC-015 | `docs/UI_UX_IMPLEMENTATION_ROADMAP.md` | SpeakCraft — UI/UX Implementation Roadmap | Roadmap (5 stages) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Superseded | Partially verified | DOC-004, 012, 016 | CONF-025, 047 |
| DOC-016 | `docs/UI_UX_TESTING_AND_VALIDATION_REPORT.md` | SpeakCraft — UI/UX Testing and Validation Report | Report (test) | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Superseded | Unverified | DOC-015, 017 | CONF-025, 028, 029 |
| DOC-017 | `docs/UI_UX_CHANGELOG.md` | SpeakCraft — UI/UX & Engineering Changelog | Changelog | Rev. 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Outdated | Partially verified | DOC-004, 015, 016 | CONF-023, 024, 025, 028, 047 |
| DOC-018 | `docs/SPEAKCRAFT_LAUNCH_ANIMATION_DESIGN.md` | SpeakCraft — Launch Animation Design Specification | Spec (design) | Release `v1.1.0-beta.1` (Build 2) | 2026-11-18 `7ed7a24` (only) | Current | Partially verified | DOC-011, 019 | CONF-033, 035, 037 |
| DOC-019 | `docs/SPEAKCRAFT_LAUNCH_ANIMATION_IMPLEMENTATION.md` | SpeakCraft — Launch Animation Implementation Guide | Architecture / implementation guide | Dated 2026-11-18 | 2026-11-18 `7ed7a24` (only) | Current | Partially verified | DOC-018, 020 | CONF-035 |
| DOC-020 | `docs/SPEAKCRAFT_LAUNCH_ANIMATION_TEST_REPORT.md` | SpeakCraft — Launch Animation & Beta Update Test Report | Report (test) | `v1.1.0-beta.1` (`versionCode = 2`) | 2026-11-18 `7ed7a24` (only) | Superseded | Partially verified | DOC-019, 021 | CONF-028, 029, 037, 039 |
| DOC-021 | `docs/AUTO_UPDATE_GITHUB_CI_SETUP.md` | SpeakCraft — Automatic Git-Push OTA Update Pipeline | Runbook (CI/OTA) | Not stated | 2026-11-18 `7ed7a24` (only) | Outdated | Contradicted by code | DOC-020 | CONF-037, 038 |
| DOC-022 | `docs/ACTIVITY_ENGINE_ARCHITECTURE.md` | SpeakCraft Universal Communication Activity Engine — Architecture | Architecture | Not stated | 2026-11-19 `7538b18` (only) | Outdated | Partially verified | DOC-023 to DOC-031 | CONF-002, 024, 026, 033 |
| DOC-023 | `docs/ACTIVITY_CATALOGUE.md` | SpeakCraft Complete Activity Catalogue (10 Modules, 150 Core Activities) | Spec (curriculum catalogue) | Not stated | 2026-11-19 `7538b18` (only) | Current | Verified | DOC-022, 024, 027 | CONF-026 |
| DOC-024 | `docs/EXERCISE_TEMPLATE_SPECIFICATION.md` | Exercise Template Specification (25 Templates across 7 Reusable Families) | Spec | Not stated | 2026-11-19 `7538b18` (only) | Outdated | Partially verified | DOC-022, 025 | CONF-002 |
| DOC-025 | `docs/AI_EVALUATION_ARCHITECTURE.md` | SpeakCraft Universal AI Evaluation Architecture | Architecture (cloud evaluators) | Not stated | 2026-11-19 `7538b18` (only) | Superseded | Contradicted by code | DOC-022, 039, 053 | CONF-002, 010, 015, 050 |
| DOC-026 | `docs/DAILY_PRACTICE_ENGINE.md` | Daily Practice Engine Specification | Spec | Not stated | 2026-11-19 `7538b18` (only) | Current | Partially verified | DOC-022, 023, 029 | CONF-045 |
| DOC-027 | `docs/LEARNING_PATHS_SPECIFICATION.md` | SpeakCraft Personalized Learning Paths Specification (10 Guided Paths) | Spec | Not stated | 2026-11-19 `7538b18` (only) | Current | Verified | DOC-023 | — |
| DOC-028 | `docs/PROGRESS_ANALYTICS_SPECIFICATION.md` | Progress & Analytics Engine Specification | Spec | Not stated | 2026-11-19 `7538b18` (only) | Current | Partially verified | DOC-022, 029 | CONF-010, 046 |
| DOC-029 | `docs/DATABASE_CHANGES.md` | SpeakCraft Database Changes — Activity Engine Cloud Firestore Architecture | Spec / change record (data) | Not stated | 2026-11-19 `7538b18` (only) | Outdated | Partially verified | DOC-007, 058 | CONF-030, 039 |
| DOC-030 | `docs/IMPLEMENTATION_ROADMAP.md` | SpeakCraft Universal Activity Engine — Implementation Roadmap | Roadmap (completed phases + future) | Not stated | 2026-11-19 `7538b18` (only) | Outdated | Partially verified | DOC-022, 031 | CONF-002 |
| DOC-031 | `docs/TESTING_AND_VALIDATION_REPORT.md` | SpeakCraft Universal Activity Engine — Testing & Validation Report | Report (test) | Not stated | 2026-11-19 `7538b18` (only) | Superseded | Partially verified | DOC-022, 029 | CONF-010, 028, 039 |
| DOC-032 | `docs/STAGE_1_LOCAL_AI_ARCHITECTURE.md` | Stage 1 — Local AI Architecture Specification | Architecture (local AI, with LLM) | "Stage 1 Implementation" | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-033 to DOC-043 | CONF-008, 051 |
| DOC-033 | `docs/STAGE_1_IMPLEMENTATION_CHANGELOG.md` | Stage 1 Implementation Changelog | Changelog | Release tag `1.1.0-local-ai`; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-032, 034 | CONF-001, 008, 037 |
| DOC-034 | `docs/CLOUD_AI_REMOVAL_AND_NETWORK_AUDIT.md` | Cloud AI Removal & Network Audit Report | Audit / report | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Outdated | Contradicted by code | DOC-033, 043, 050 | CONF-001, 005 |
| DOC-035 | `docs/LOCAL_AUDIO_INTELLIGENCE_ENGINE.md` | Local Audio Intelligence Engine Specification | Spec (audio pipeline) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-032, 046 | CONF-013, 015 |
| DOC-036 | `docs/LOCAL_VIDEO_INTELLIGENCE_ENGINE.md` | Local Video Intelligence Engine Specification | Spec (video pipeline) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Current | Partially verified | DOC-032, 049 | CONF-017, 019 |
| DOC-037 | `docs/AUDIO_VIDEO_FEATURE_FUSION.md` | Audio-Video Multimodal Feature Fusion Engine | Spec | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Current | Partially verified | DOC-035, 036 | — |
| DOC-038 | `docs/LOCAL_LANGUAGE_MODEL_INTEGRATION.md` | Local Language Model Integration Specification | Spec (on-device LLM) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-032, 044 | CONF-008 |
| DOC-039 | `docs/NINE_LOCAL_EVALUATORS_IMPLEMENTATION.md` | Nine Local Evaluators Implementation | Spec / implementation note | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Current | Partially verified | DOC-025, 053 | CONF-002, 010 |
| DOC-040 | `docs/ON_DEVICE_MODEL_SELECTION_REPORT.md` | On-Device Model Selection & Benchmark Report | Report (model selection) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-041, 047 | CONF-008, 012, 014, 017, 018 |
| DOC-041 | `docs/DEVICE_COMPATIBILITY_MATRIX.md` | Device Compatibility Matrix | Spec (device tiers) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Superseded | Partially verified | DOC-040, 048 | CONF-008, 016, 017 |
| DOC-042 | `docs/PERFORMANCE_AND_MEMORY_BENCHMARKS.md` | Performance & Memory Benchmarks | Report (benchmark) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Superseded | Contradicted by code | DOC-047 | CONF-008, 013, 014, 019 |
| DOC-043 | `docs/OFFLINE_TESTING_REPORT.md` | Offline Testing & Validation Report | Report (test) | "Stage 1" | 2026-11-19 `36e2bf1` (only) | Current | Partially verified | DOC-034, 051 | CONF-039 |
| DOC-044 | `docs/LIGHTWEIGHT_AI_AUDIT.md` | SpeakCraft Stage 1.2 — Lightweight AI Architecture & Dependency Audit | Audit + action plan | Stage 1.2; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Superseded | Partially verified | DOC-032, 045 | CONF-005, 008, 014 |
| DOC-045 | `docs/STAGE_1_2_MIGRATION_NOTES.md` | SpeakCraft Stage 1.2 Architecture Migration Notes | Changelog / migration notes | Stage 1.2; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Outdated | Partially verified | DOC-044, 046 to 049 | CONF-001, 008, 012, 020, 052 |
| DOC-046 | `docs/SPEECH_MODEL_BENCHMARK_REPORT.md` | SpeakCraft Speech Recognition Model Benchmark Report (Stage 1.2) | Report (benchmark) | Stage 1.2; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Current | Unverified | DOC-045, 055 | CONF-011, 014, 018 |
| DOC-047 | `docs/MODEL_SIZE_AND_RAM_REPORT.md` | SpeakCraft Model Size & Runtime RAM Consumption Report (Stage 1.2) | Report (resource budget) | Stage 1.2; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Current | Contradicted by code | DOC-040, 048 | CONF-009, 013, 016, 017, 018, 019 |
| DOC-048 | `docs/DEVICE_COMPATIBILITY_REPORT_STAGE1_2.md` | SpeakCraft Stage 1.2 Device Compatibility Report | Report (device test) | Stage 1.2; 2026-11-19 | 2026-11-19 `36e2bf1` (only) | Current | Unverified | DOC-041, 047 | CONF-014, 016, 039, 040 |
| DOC-049 | `app/src/main/assets/models/README.md` | SpeakCraft On-Device AI Models (Stage 1.2 Lightweight Catalog) | Reference note (model catalogue) | Stage 1.2 | 2026-11-19 `36e2bf1` (only) | Current | Partially verified | DOC-040, 047 | CONF-009, 017 |
| DOC-050 | `app/docs/CENTRALIZED_AI_SETTINGS.md` | Centralized AI & Intelligence Settings | Spec (settings / AI policy) | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Verified | DOC-034, 054 | CONF-003, 004, 011, 053 |
| DOC-051 | `app/docs/COACH_AND_TRANSCRIPTION_TEST_REPORT.md` | Coach Evaluation, Live Transcription & Centralized AI Settings — Test Report | Report (test) | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Partially verified | DOC-050, 052 to 055 | CONF-028, 039 |
| DOC-052 | `app/docs/COACH_EVALUATION_ROOT_CAUSE.md` | SpeakCraft Coach Evaluation — Root Cause Analysis (78/100 False Score Bug) | Report (root-cause analysis) | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Partially verified | DOC-053 | CONF-010 |
| DOC-053 | `app/docs/EVIDENCE_BASED_SCORING.md` | Evidence-Based Scoring Policy | Spec (scoring policy) | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Verified | DOC-052, 039 | CONF-010 |
| DOC-054 | `app/docs/INTELLIGENCE_CAPABILITY_MATRIX.md` | Intelligence Capability Matrix | Spec | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Partially verified | DOC-050, 055 | CONF-003 |
| DOC-055 | `app/docs/LIVE_TRANSCRIPTION_IMPLEMENTATION.md` | Live Transcription Implementation | Spec / implementation note | Not stated | 2026-11-20 `68a9cb1` (only) | Current | Verified | DOC-050, 054 | CONF-011 |
| DOC-056 | `README_DOCUMENTATION_INDEX.md` | SpeakCraft Technical Documentation & Architecture Audit Index | Index | Rev. 2026-11-18; app "v1.0.0" | 2026-11-18 `1f77749` / 2026-11-19 `7538b18` (3 commits) | Outdated | Contradicted by code | DOC-001 to DOC-031 | CONF-002, 021, 025, 027, 028, 037, 041 |
| DOC-057 | `metadata.json` | *(JSON — no H1; `"name": "SpeakCraft"`)* | Platform metadata (AI Studio) | Not stated | 2026-11-18 `330ca3e` (only) | Outdated | Partially verified | DOC-034, 050 | CONF-006, 009 |
| DOC-058 | `firebase-blueprint.json` | *(JSON — no H1; `entities` + `firestore` paths)* | Schema blueprint | Not stated | 2026-11-18 `330ca3e` / 2026-11-19 `7538b18` (2 commits) | Outdated | Partially verified | DOC-007, 029 | CONF-030, 032 |
| DOC-059 | `public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484` | *(Plain text — first line: "SpeakCraft — Complete Learning Experience, Dashboard, Personalization & Vocabulary Builder Upgrade")* | Prompt / product request | Dated in filename 2026-11-19 19:30 | 2026-11-19 `4d6b917` (only) | Current | Unverified (request; partial implementation seen in code) | DOC-026, 050 | CONF-004, 032, 043, 044, 045, 046, 048, 049 |

---

## 4. Per-document notes (grouped by theme)

### 4.1 Baseline audit 01–07 (commit `1f77749`, 2026-11-18)

These seven documents are a read-only audit of the first scaffold (`330ca3e`). They were written against a request that apparently asked for an Electron + React desktop/web product, which explains why "Platform Gap ISS-024" runs through all of them. They are internally detailed, with file:line evidence, but they describe a codebase that was then heavily changed. The index itself labels them "Historical Audit Docs".

- **DOC-001 (UI/UX).** Inventories 22 routes, the 5-tab bar (Home/Daily/Record/@askvinh/Mentor), the "Editorial Studio" palette (navy primary), typography and 9 UI defects. *Superseded / Contradicted:* the rail breakpoint (720dp), colour tokens, type scale and most defects (ISS-001, 014, 015, 017–019, 021) no longer match code. The 5-tab bar is still accurate.
- **DOC-002 (Backend).** Serverless Firebase + OkHttp topology, operations OP-001..015, proposed API gateway. *Outdated / Partially verified:* "no custom backend" and Firebase Auth/Firestore/OkHttp YouTube/updater still hold. The Gemini call path (OP-010..012) via `GeminiCoachService` is now a deprecated stub, and the unused Retrofit/Moshi dependencies (ISS-012) have been removed.
- **DOC-003 (Scalability).** Seven-tier capacity matrix from 100 to 1M users. It states plainly that "all throughput/capacity figures below are engineering estimates". *Unverified* by design. It is internally inconsistent on listener counts (CONF-031), and its Gemini cost model is obsolete.
- **DOC-004 (Issues).** A 28-issue register with severities and a P0–P3 roadmap. *Contradicted by code:* about 14 issues marked "Open" show fixes in code (see CONF-042), and ISS-027 ("no CI workflow") is false since `7ed7a24`. Its test claim of 17 tests matches the code at `1f77749`.
- **DOC-005 (Features).** Forty feature domains with statuses and an MVP → V2.0 roadmap. *Partially verified:* most features still exist. Statuses for FEAT-019, 031, 033, 034 and 036 have changed (notifications now exist in-app). It omits all later engines: activities, local AI, learning-progress manager, profile.
- **DOC-006 (AI).** Architecture for `GeminiCoachService` (gemini-3.8-flash), its schemas and guardrails. *Contradicted by code:* `GeminiCoachService` now throws `UnsupportedOperationException`. A separate `FirebaseAiLogicService` exists but is gated off. Still useful if cloud AI is re-enabled.
- **DOC-007 (Database).** Twenty-two Firestore collections, local DataStore/Keystore, and a proposed 28-table PostgreSQL schema. *Partially verified:* the original 22 paths still exist, but `firestore.rules` now has 34 paths and 865 lines (the document says 587). Room is still unused.

### 4.2 UI/UX redesign (commit `7ed7a24`, 2026-11-18; the index labels these 08–17)

All ten were committed together with the code they describe, together with the launch-animation and CI docs. The theme was later rewritten in `dc7c9cb` (Violet/Indigo palette) with **no accompanying documentation**, so every colour statement in this group is obsolete.

- **DOC-008 (Current-state audit).** Pre-redesign friction list with severities. *Superseded* by the redesign. Rates ISS-002 and ISS-003 "Critical" where DOC-004 rates them "High" (CONF-054).
- **DOC-009 (Competitive UX).** Archetype benchmarks (Orai, ELSA, Duolingo, Speak and others) and differentiation themes. *Current* as strategy but *Unverified*, with no research sources. Its palette references are obsolete.
- **DOC-010 (Information architecture).** Proposes relabelled tabs (Practice/Studio/Curriculum), a 4-card hub grid and keeping 22 routes. *Draft:* the relabel was never implemented (code still uses Daily/Record/@askvinh), although route stability was preserved. The index misdescribes it as "4-hub ... 24-route" (CONF-021).
- **DOC-011 (Design system).** Amber-primary Editorial Studio tokens, `SpeakCraftColors`, a type scale and spacing. *Contradicted by code* on colour: code uses `RoyalViolet #6C3DF4` and `LuminousViolet #A78BFA`. The type scale (34/26/21sp) and `Spacing.kt` (4–32dp) do match code.
- **DOC-012 (Screen-by-screen).** Redesign blueprint covering the onboarding wizard, dashboard zones, video review, labs, mentor and analytics. *Partially verified:* named components exist (`VideoReviewPlayerCard`, `ConfirmDestructiveDialog`, `StepProgressIndicator`, `mentor_history_{uid}.json`). The 720dp breakpoint is wrong.
- **DOC-013 (Motion).** Timing tokens, reduced-motion rules and microcopy tone. *Current, Partially verified* (`rememberReducedMotionEnabled` exists in 5 files). No conflicts found.
- **DOC-014 (Accessibility).** A11Y-01..07 and contrast ratios. *Outdated:* its contrast maths applies to the retired navy/ivory palette (CONF-034). The structural findings (semantics, `FlowRow`, reduced motion) appear to be addressed.
- **DOC-015 (UI/UX roadmap).** A five-stage plan. *Superseded:* DOC-017 reports it as done.
- **DOC-016 (UI/UX test report).** Claims "41 / 41" tests. *Unverified:* no commit ever contained 41 tests. The commit that added the report (`7ed7a24`) contained 53, which matches DOC-020 (CONF-028).
- **DOC-017 (UI/UX changelog).** Tokenisation, P0/P1 fixes, the 24-feature tutorial system, tablet tiers (rail at 600dp, expanded at 840dp). *Partially verified:* most named classes exist and the breakpoints match code. Its "4 Quick Action Hubs" list differs from DOC-010/012, and the colour work was overwritten by `dc7c9cb`.

### 4.3 Launch animation (commit `7ed7a24`)

- **DOC-018 (Design).** A four-stage choreography with cadences of 2900/2100/600 ms and a theme colour mapping. *Partially verified:* the cadences match `LaunchAnimationOverlay.kt:71-73`. The colour tables (Warm Ivory/Midnight Navy) match only the OS window background, not the current Compose theme (CONF-035).
- **DOC-019 (Implementation).** Files, single-clock timeline and anti-jank notes. *Partially verified:* `LaunchExperienceViewModel` and the overlay exist.
- **DOC-020 (Test report).** 53/53 tests with a per-suite breakdown. *Partially verified:* the per-suite counts exactly match the code at `7ed7a24`. Pass status is not reproducible. It explicitly states that no device or emulator was available.

### 4.4 Update and CI (commit `7ed7a24`)

- **DOC-021 (OTA pipeline).** Explains why v1.0.0 saw no updates and describes the push → GitHub Release → Firestore pipeline. *Contradicted by code:* the workflow now restores the debug keystore from a `DEBUG_KEYSTORE_BASE64` secret, not from a file in the repository (`debug.keystore` is git-ignored). It also requires a `GOOGLE_SERVICES_JSON` secret and refuses stable signing without `ANDROID_KEYSTORE_BASE64`. The workflow has no test step (CONF-038).

### 4.5 Activity engine (commit `7538b18`, 2026-11-19; the index labels these 18–27)

- **DOC-022 (Architecture).** The config-driven pipeline: 20-property `ActivityDefinition`, routes, and "9 Firebase AI Logic Gemini 3.8 Flash Evaluators". *Outdated:* the evaluators became local one commit later. It is the only document that mentions the "Violet Studio Light / Midnight Indigo Dark" theme and the "Indigo Studio Sidebar", which matches code.
- **DOC-023 (Catalogue).** 150 activities across 10 modules (15/15/15/15/15/15/15/10/15/20). **Verified:** 150 unique IDs with exactly that per-module distribution in `ActivityCatalogueModules*.kt`.
- **DOC-024 (Templates).** 25 templates mapped to 7 interaction families. *Partially verified:* the 25-value `ExerciseTemplateType` enum is confirmed. Its stage 3 still says "calling Firebase AI Logic".
- **DOC-025 (AI evaluation).** Nine cloud evaluators, a 15 MB multimodal cap and an 8-word filler list. *Superseded* by DOC-039 and DOC-052/053. Contradicted by code: no Firebase calls remain in `UniversalEvaluationEngine`.
- **DOC-026 (Daily practice).** 10/20/30/45/60-minute plans and scoring weights. *Partially verified:* the file exists. A second, undocumented engine (`data/learning/DailyPracticePlanEngine.kt`, added in `4d6b917`) now runs in parallel (CONF-045).
- **DOC-027 (Learning paths).** Ten paths with prerequisites at 70%. **Verified:** 10 unique `path_NN_*` IDs in `LearningPathsEngine.kt`.
- **DOC-028 (Progress analytics).** Metric definitions and a "Zero Fabricated Metrics" rule. *Partially verified.* Its honesty rule is undermined by the later root-cause finding of fabricated baselines elsewhere (CONF-010).
- **DOC-029 (Database changes).** Four new subcollections, rules and blueprint. "Deployed live". *Partially verified:* the four paths exist in rules and blueprint. Deployment is not verifiable, and eight further collections have since been added without documentation (CONF-030).
- **DOC-030 (Roadmap).** Five completed phases and four future items. *Outdated* (cloud evaluators).
- **DOC-031 (Test report).** 69/69 tests ("56 pre-existing") and 4/4 emulator suites. *Partially verified:* the code had 69 tests at `7538b18` and 56 at `dc7c9cb`. Pass and deploy status are not reproducible.

### 4.6 Local AI — Stage 1 and Stage 1.2 (all in commit `36e2bf1`, 2026-11-19)

All 17 documents (DOC-032 to DOC-048), plus DOC-049, landed in **one commit**. That commit's message says it both introduced Stage 1, which included an on-device LLM (Phi-3.5 Mini via LiteRT-LM), and purged that LLM in Stage 1.2. The Stage 1 documents were therefore superseded on arrival. The zero-cloud claims were then partly reversed by `38d52f2`.

- **DOC-032 (Stage 1 architecture).** Includes LiteRT-LM/Phi-3.5 in the pipeline and claims "15 modular components" while listing 18. *Superseded* by DOC-044/045.
- **DOC-033 (Stage 1 changelog).** Adds `litert-lm` and Phi-3.5, removes `firebase-ai`, and gives release tag "1.1.0-local-ai", which does not exist in git. *Superseded*; contradicted by code.
- **DOC-034 (Cloud AI removal).** Says "ZERO CLOUD AI CALLS VERIFIED", `firebase-ai` "completely purged", and a regex scan finding "0 matches". *Contradicted by code:* `firebase-ai` is back at `app/build.gradle.kts:135` and `FirebaseAiLogicService.kt` calls `Firebase.ai`. The interceptor it describes is not attached to any client (CONF-001, CONF-005).
- **DOC-035 (Audio engine).** Resampler, DSP, fillers, pronunciation, Whisper. *Superseded* on the 30 s cap; code now allows 10 minutes. The DSP formulas are still useful.
- **DOC-036 (Video engine).** One frame per second, 640px downscale, MediaPipe face/pose/hand, and an explicit "zero facial emotion or psychological inference" rule. *Partially verified:* 640px matches code. The MediaPipe `.task` files are not in the repository (CONF-017).
- **DOC-037 (Fusion).** Modality independence, the `LocalFeedbackEvidence` structure and cross-modal rules. *Partially verified* (the class exists). No conflicts found.
- **DOC-038 (Local LLM integration).** A two-tier design: Phi-3.5 versus deterministic rules. *Superseded*; contradicted by Stage 1.2 and code.
- **DOC-039 (Nine local evaluators).** The nine strategies migrated to local evaluation. *Partially verified:* nine evaluator classes exist with no cloud calls. The scoring internals were later reworked by DOC-052/053.
- **DOC-040 (Model selection).** Whisper, three MediaPipe models and Phi-3.5. Claims MediaPipe is "Bundled in APK" and that SHA-256 is verified. *Contradicted by code:* no `.task` files are present, the Whisper SHA-256 is an empty string, and the Whisper URL points to a GGML `.bin` file (CONF-012, CONF-017).
- **DOC-041 (Device matrix).** BASIC/STANDARD/ADVANCED tiers, with the ADVANCED tier tied to Phi-3.5. *Superseded*, but its thresholds (2048/4096 MB, API 30) **match** `DeviceCapabilityManager.kt:146-148`. Its BASIC condition "API < 24" is impossible given minSdk 24.
- **DOC-042 (Performance).** Latency and memory table including Phi-3.5, and a 30 s audio cap. *Superseded*; contradicted on the audio cap. The numbers are unverifiable.
- **DOC-043 (Offline testing).** Ten offline scenarios and unit-test lists. *Partially verified:* the listed test classes and counts (5/3/4/3) match code. The "offline execution" claims cannot be reproduced.
- **DOC-044 (Lightweight audit).** The Stage 1.2 audit and action plan. Its checklist shows only the audit item ticked. *Superseded* by DOC-045, which reports completion.
- **DOC-045 (Stage 1.2 migration).** File-by-file changes, 11 grammar categories, 7 feedback sub-engines and multi-tier ASR. *Outdated / Partially verified:* most named classes exist. It states `targetSdk = 35` (code is 36), cites a non-existent `ScenarioScriptBank`, and its "Ensured absence of firebase-ai" has been reversed.
- **DOC-046 (Speech benchmark).** WER figures of 8.4/12.8/18.2% and real-time factors for Whisper, Moonshine and Android ASR. *Unverified:* there is no benchmark harness or data in the repository, and it says "simulated and physical". It usefully documents the cloud-fallback risk of Android `SpeechRecognizer`.
- **DOC-047 (Model size and RAM).** Size and RAM tables, tier budgets, the "98.3% reduction" and the "Zero LLMs" invariant. *Contradicted by code* on tier thresholds (<3000 MB), frame size (256×256), the <2 MB audio buffer and bundled vision assets.
- **DOC-048 (Device report 1.2).** Three hardware tiers marked "PASS (100% Stable)", with battery and thermal figures. *Unverified:* Profile 1 is "Simulated" and no device artefacts exist.
- **DOC-049 (Models README).** Lists MediaPipe assets ("Bundled ... (or placed in assets)") with download links, optional ASR models, and the invariant that zero LLMs are "required or supported". *Partially verified:* consistent with the empty assets folder, but the LLM invariant conflicts with the dormant Gemini integration (CONF-009).

### 4.7 app/docs (commit `68a9cb1`, 2026-11-20 — the newest documents)

These describe the current HEAD. They are concise, and spot checks largely confirm them.

- **DOC-050 (Centralized AI settings).** A single `AISettingsRepository`; cloud AI "Locked OFF under Stage 1 Zero-Cloud-AI Policy"; per-screen cloud chips removed. **Verified:** `HybridAiCoachCoordinator.isCloudAiEnabled()` returns false while `LocalExecutionPolicy.isLocalOnly()` is a constant true, and no `*_cloud_toggle_chip` remains. However, the cloud code and dependency still ship (CONF-003), and the Gemini Live view-model path is dormant rather than removed (CONF-053).
- **DOC-051 (Test report).** 24 suites, 161/161 tests. *Partially verified:* static `@Test` counts match **every** per-suite figure at HEAD. Pass status is not reproducible, and CI runs no tests.
- **DOC-052 (Root cause, "78/100").** Fabricated baselines (78/72/35), duration coercion, failure-to-success masking, and the fix. *Partially verified:* the fix classes exist (`EvaluationModels`, `EvaluationEvidenceValidator`, `EvidenceBasedScoringEngine`).
- **DOC-053 (Evidence-based scoring).** A 12-state lifecycle, thresholds of 1.5 s / RMS 0.008 / 0.5 s voiced, and nullable scores. **Verified:** `EvaluationEvidenceValidator.kt:42-44` and the 12-value `CoachEvaluationState`.
- **DOC-054 (Capability matrix).** `CapabilityStatus` values and enablement rules. *Partially verified* (`IntelligenceCapabilityManager` exists).
- **DOC-055 (Live transcription).** State machine, session isolation, and honest offline reporting (on-device recogniser only on API 31+). **Verified** against `LiveTranscriptionManager.kt:82,100-102,165`.

### 4.8 Root-level and other artefacts

- **DOC-056 (Documentation index).** Indexes only DOC-001..031 and calls Firebase AI Logic the "Actual Codebase Reality (Verified)". It also states v1.0.0 and "30 / 30" tests. It omits 28 documents: the 17 Stage 1/1.2 documents, the 3 launch-animation documents, the OTA runbook, the models README and all 6 `app/docs` files. Its "Revision Date: 2026-11-18" predates its own last change (`7538b18`, 2026-11-19). *Contradicted by code* (CONF-041).
- **DOC-057 (`metadata.json`).** AI Studio metadata. The description "built from @askvinh YouTube lessons" and the capability `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` reflect the original cloud design. *Outdated* (CONF-006).
- **DOC-058 (`firebase-blueprint.json`).** 25 entities and 26 Firestore paths (the original 22 plus 4 activity-engine paths). *Outdated:* rules define 8 more paths (notifications, learningEvents, vocabularyProgress, quizAttempts, learningSnapshots, adminAnnouncements, adminAuditLogs, adminSettings). The `communicationLevel` enum has 4 values while code supports 5 (CONF-032).
- **DOC-059 (Paste_November_19 prompt).** The latest explicit product request. It asks for 13 phases (welcome-back carousel through testing), sets hard constraints (Android-only; no LLM; no paid cloud AI; offline core; no cloud AI settings), and names 10 required documentation deliverables. *Current* as intent. Several features appear in code (pager, `LearningProgressManager`, streak engine, custom-claims admin, account deletion), but **none of the 10 required documents exist** (CONF-044), and the hybrid cloud commit that followed contradicts its constraints (CONF-004).

---

## 5. Documentation health summary

### 5.1 Counts

| Status | Count | | Reliability | Count |
|---|---:|---|---|---:|
| Current | 23 | | Verified | 5 |
| Outdated | 20 | | Partially verified | 33 |
| Superseded | 15 | | Unverified | 6 |
| Draft | 1 | | Contradicted by code | 15 |
| Unknown | 0 | | | |
| **Total** | **59** | | **Total** | **59** |

Observations:

- Only 5 of 59 documents (8%) could be fully verified: DOC-023, 027, 050, 053 and 055. Four of the five are short, narrowly scoped and recent.
- 35 of 59 (59%) are Outdated or Superseded. The whole repository history covers only three days (2026-11-18 to 2026-11-20). Seven Stage 1 documents (DOC-032, 033, 035, 038, 040, 041, 042) were superseded **in the same commit that introduced them** (`36e2bf1`), because that commit contained both the Stage 1 design (with an on-device LLM) and the Stage 1.2 design (without one).
- The documentation index (DOC-056) is the most misleading artefact. It is the natural entry point, yet it presents the abandoned cloud architecture as verified and omits 28 documents: 21 in `docs/`, all 6 in `app/docs/`, and the models README.

### 5.2 Duplicated or overlapping topics

| Topic | Documents | Problem |
|---|---|---|
| Design system / palette | DOC-001 §19–28, DOC-011, DOC-014 §2, DOC-017 §1, DOC-018 §2.1, DOC-022 §4 | Three different palettes are described: navy-primary Editorial Studio (DOC-001), amber-primary Editorial Studio (DOC-011/014/018) and Violet/Indigo (only mentioned in DOC-022). Code uses Violet/Indigo, which **no specification describes**. |
| Roadmaps | DOC-003 (S1–S3), DOC-004 (P0–P3), DOC-005 (v1.0 → v2.0), DOC-015 (UI stages 1–5), DOC-030 (activity phases + 4 futures), DOC-044 (checklist), DOC-059 (18-step order) | Seven roadmaps with incompatible scopes. DOC-003/004/005 assume an Electron/React/PostgreSQL future that DOC-059 explicitly rules out. |
| Test reports and test counts | DOC-004 (17), DOC-056 (30), DOC-016/017 (41), DOC-020 (53), DOC-031 (69), DOC-043 (offline), DOC-051 (161) | Seven different "all passing" figures. Only 17, 53, 69 and 161 correspond to committed code. None can be reproduced from the repository, because CI has no test step. |
| Changelogs | DOC-017, DOC-033, DOC-045, plus commit messages | DOC-033 and DOC-045 describe opposite LLM decisions in the same commit. The `dc7c9cb` theme rewrite and everything after `36e2bf1` has no changelog. |
| AI architecture | DOC-006, 025, 032, 038, 039, 044, 045, 050, 054 | Four successive architectures: cloud Gemini; on-device with LLM; on-device without LLM; hybrid with cloud locked off. Only DOC-050/054 reflect HEAD. |
| Device and model requirements | DOC-040, 041, 042, 046, 047, 048, 049 | Three sets of tier thresholds, two frame sizes, two audio caps and inconsistent model sizes and latencies (CONF-013, 014, 016, 018, 019). |
| Data model | DOC-007, 029, 058 (+ `firestore.rules`) | Counts of 22, 26 and 34 collections. The blueprint lags the rules. |
| Feature inventories | DOC-005 (40 domains), DOC-016/017 (24 tutorial "features"), DOC-056 summary | There is no single feature list. RD-04 (`04_FEATURE_IMPLEMENTATION_MATRIX.md`) should become the single source. |
| Daily practice | DOC-001/005 (9-step checklist), DOC-026 (10–60-minute engine), DOC-059 Phase 2 (`DailyPracticePlanEngine`) | Three daily-practice concepts; two engines in code (CONF-045). |
| Streaks | DOC-004/017 (`StreakAndAnalyticsHelper`), DOC-028 (consistency streak), DOC-059 Phase 4 (`StreakAndCompletionEngine`, undocumented) | Three definitions (CONF-046). |
| Navigation and information architecture | DOC-001, 008, 010, 012, 017, 022, 056 | Different claims about tab labels, hub contents, breakpoints and route counts (CONF-021 to 025). |

### 5.3 Claims whose numbers cannot be verified from the repository

- **Test pass results:** 17/17, 30/30, 41/41, 53/53, 69/69, 161/161, and "4/4 Firestore emulator suites". Static counts confirm only how many tests exist, not whether they pass. The 30 and 41 figures match no committed state.
- **Build and deploy claims:** "`compile_applet` Build succeeded" (an AI Studio tool), "Deployed live to Firebase via `deploy_firebase`", and the release tags `v1.1.0-beta.7` to `beta.23` (the tags prove builds were tagged, not what was deployed or installed).
- **Speech benchmarks (DOC-046):** WER of 8.4/12.8/18.2% and RTF of 0.14/0.11/0.08. They cite "IndicTTS / Common Voice Indian English", but no dataset or harness is present.
- **Latency and RAM (DOC-040, 042, 044, 047, 048):** for example Whisper at 1.4 s, 1.8 s or 2.8 s per 10 s of audio; peak RAM of 88, 116 or 128 MB; "98.3% storage reduction"; "94.0% memory reduction".
- **Device stability (DOC-048):** "PASS (100% Stable)" across three tiers; "< 1.5% battery per 10 sessions"; "+3.2 °C".
- **Scalability (DOC-003):** all capacity tiers. These are explicitly labelled estimates, which is good practice.
- **Contrast ratios (DOC-014):** computable, but they apply to a palette no longer in use.
- **APK size (DOC-041/047):** "~45 MB base APK" was not measured.

### 5.4 Documentation gaps (code areas with no documentation)

From the git log and spot checks, these areas exist in code but have no document, or only a passing mention:

1. **Violet/Indigo theme and the sidebar shell** (`dc7c9cb`): `Color.kt`, `Theme.kt`, sidebar at ≥840dp.
2. **Welcome and dashboard studio widgets** (`DashboardStudioWidgets.kt`, `HorizontalPager`), which DOC-059 Phase 1 requested.
3. **The `data/learning` package** (`4d6b917`): `LearningProgressManager`, `DailyPracticePlanEngine`, `StreakAndCompletionEngine`, `SpacedRepetitionScheduler`, `LocalVocabularyQuizEngine`, and the five-stage vocabulary models.
4. **`LocalVocabularyEvaluator` and `DynamicLocalCoachEngine`** (`4d6b917`, `13bffe0`).
5. **`UserProfileScreen`** and account deletion (`SpeakCraftRepository.kt:1318`).
6. **The notification system** (`NotificationManagerRepository`, `NotificationDrawer`, `NotificationsCenterSheet`; `13bffe0`, `d8e2a05`).
7. **`HybridAiCoachCoordinator` and `FirebaseAiLogicService`** (868 lines, with Gemini model tiers and Live voice). DOC-050 mentions them only as "locked off".
8. **Admin authorisation via custom claims plus `adminUsers`** (`SpeakCraftRepository.kt:236-249`) and the admin collections (`adminAnnouncements`, `adminAuditLogs`, `adminSettings`).
9. **Eight Firestore collections added after DOC-029.**
10. **Update-integrity work** (`08972b7`): APK signature compatibility and `UpdateAvailableDialog`. Also `GitHubReleaseClient` (only briefly mentioned in DOC-021).
11. **The ten documents DOC-059 required**: `WELCOME_EXPERIENCE.md`, `PERSONALIZED_LEARNING_ENGINE.md`, `DASHBOARD_AND_STREAK.md`, `NOTIFICATION_CENTER.md`, `PROFILE_AND_SETTINGS.md`, `ADMIN_CONSOLE_SECURITY.md`, `VOCABULARY_BUILDER.md`, `LEARNING_PROGRESS_ARCHITECTURE.md`, `DATABASE_MIGRATIONS.md`, `IMPLEMENTATION_AND_TEST_REPORT.md`. None of them exist.

### 5.5 Recommended disposition in the new documentation structure

Nothing is to be deleted. "Archive" means: move later, if approved, to an archive folder with a banner stating it is historical. "Superseded by" names the redevelopment document that takes over the topic. Where the replacement does not yet exist it is marked *(to be authored)*.

| Doc ID | Recommended disposition | Rationale |
|---|---|---|
| DOC-001 | Archive | Historical UI baseline; current UI captured in RD-00 and RD-04. |
| DOC-002 | Keep as reference (label "baseline 2026-11-18") | Serverless topology, auth and updater descriptions still largely valid. |
| DOC-003 | Keep as reference | Explicitly estimated scaling options; useful if cloud or backend is reconsidered. |
| DOC-004 | Superseded by 04_FEATURE_IMPLEMENTATION_MATRIX.md and a re-baselined issue register *(to be authored)* | About half the "Open" issues are fixed. |
| DOC-005 | Superseded by 04_FEATURE_IMPLEMENTATION_MATRIX.md | Feature statuses obsolete. |
| DOC-006 | Archive | Describes the removed `GeminiCoachService`; retain in case cloud AI is reinstated. |
| DOC-007 | Keep as reference (needs update) | Firestore audit still mostly valid; update to 34 paths. |
| DOC-008 | Archive | Pre-redesign audit. |
| DOC-009 | Keep as reference | Positioning input for the product owner. |
| DOC-010 | Archive | Unimplemented proposal; current navigation recorded in RD-00/RD-04. |
| DOC-011 | Superseded by a new design-system specification derived from code *(to be authored)* | Palette obsolete. |
| DOC-012 | Archive | Implemented or obsolete blueprint. |
| DOC-013 | Keep as reference | Still-valid motion and microcopy guidance. |
| DOC-014 | Superseded by a re-run accessibility audit on the current palette *(to be authored)* | Contrast figures invalid. |
| DOC-015 | Archive | Completed plan. |
| DOC-016 | Archive | Superseded test snapshot. |
| DOC-017 | Archive | Historical changelog. |
| DOC-018 | Keep as reference (update colour table) | Only launch-animation specification. |
| DOC-019 | Keep as reference | Implementation guide still matches files. |
| DOC-020 | Archive | Superseded test snapshot. |
| DOC-021 | Keep as reference (needs update to the current workflow) | Only OTA runbook. |
| DOC-022 | Keep as reference (annotate evaluator layer) | Core engine architecture still valid. |
| DOC-023 | Keep as reference | Verified catalogue. |
| DOC-024 | Keep as reference | Template mapping valid. |
| DOC-025 | Archive (superseded by DOC-039 and DOC-053) | Cloud evaluators removed. |
| DOC-026 | Keep as reference (reconcile with `DailyPracticePlanEngine`) | Active engine. |
| DOC-027 | Keep as reference | Verified. |
| DOC-028 | Keep as reference | Active engine. |
| DOC-029 | Keep as reference (feed into an updated data-model document) | Partially valid. |
| DOC-030 | Archive | Completed roadmap. |
| DOC-031 | Archive | Superseded test snapshot. |
| DOC-032 | Archive | Superseded by DOC-045. |
| DOC-033 | Archive | Superseded by DOC-045. |
| DOC-034 | Archive (retain as evidence of zero-cloud intent) | Contradicted at HEAD. |
| DOC-035 | Keep as reference (annotate that the 30 s cap is superseded) | DSP detail still useful. |
| DOC-036 | Keep as reference | Valid; note missing model assets. |
| DOC-037 | Keep as reference | Valid. |
| DOC-038 | Archive | LLM path purged. |
| DOC-039 | Keep as reference | Current evaluator description. |
| DOC-040 | Archive | Superseded by DOC-046/047. |
| DOC-041 | Archive (thresholds still match code; cite in RD-04) | Superseded by DOC-048. |
| DOC-042 | Archive | Superseded; unverifiable numbers. |
| DOC-043 | Keep as reference | Test inventory matches code. |
| DOC-044 | Archive | Pre-implementation audit. |
| DOC-045 | Keep as reference (annotate firebase-ai reversal) | Latest local-AI change log. |
| DOC-046 | Keep as reference (mark benchmarks unverified) | Only ASR comparison. |
| DOC-047 | Keep as reference (annotate contradictions) | Only resource-budget document. |
| DOC-048 | Keep as reference (mark unverified) | Only device report. |
| DOC-049 | Keep as reference | Operational note on model files. |
| DOC-050 | Keep as reference | Current. |
| DOC-051 | Keep as reference | Current test inventory. |
| DOC-052 | Keep as reference | Current. |
| DOC-053 | Keep as reference | Current, verified. |
| DOC-054 | Keep as reference | Current. |
| DOC-055 | Keep as reference | Current, verified. |
| DOC-056 | Superseded by 00_PROJECT_DISCOVERY.md and 01_DOCUMENT_INVENTORY.md | Misleading index. |
| DOC-057 | Keep as reference (update when AI direction is decided) | Platform metadata. |
| DOC-058 | Keep as reference (update to match `firestore.rules`) | Schema blueprint. |
| DOC-059 | Keep as reference (treat as the latest requirements input) | Latest product intent. |

Totals: **Keep as reference: 34**; **Archive: 20**; **Superseded by a redevelopment or new document: 5** (DOC-004, 005, 011, 014, 056).

### 5.6 Immediate recommendations

1. **Decide the AI policy** (zero-cloud vs hybrid) before any further documentation work. Every AI document depends on it (RD-05, CONF-001 to CONF-005).
2. **Replace the index** (DOC-056) with RD-00 and this inventory, and add a "historical" banner to archived documents when they are moved.
3. **Write a design-system specification from the code** (`Color.kt`, `Theme.kt`, `Type.kt`, `Spacing.kt`), and re-run the contrast checks.
4. **Re-baseline test evidence** by adding a test step to CI. Until then, treat every "N/N passed" statement as unverified.
5. **Produce the missing DOC-059 deliverables**, or record in RD-04 that they were descoped.
