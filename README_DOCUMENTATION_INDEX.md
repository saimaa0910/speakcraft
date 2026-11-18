# SpeakCraft Technical Documentation & Architecture Audit Index

**Revision Date:** 2026-11-18  
**Audited Application:** SpeakCraft (`com.aistudio.speakcraft.qxmvkp`, v1.0.0)  
**Repository State:** Read-only architecture and codebase audit complete across all 7 technical pillars. Zero application source files, database rules, or configurations were modified during this audit.

---

## Critical Platform Alignment Notice

Before reading the detailed specifications below, engineering and product leadership should note a key platform discrepancy between the audit brief and the repository contents:

* **Actual Codebase Reality (Verified):** The repository implements **SpeakCraft as a native Android application** written in **Kotlin + Jetpack Compose (Material 3)**, powered by serverless **Firebase Authentication (Google Credential Manager)**, **Cloud Firestore Enterprise** (named DB `ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11`), **Firebase App Check**, and **Firebase AI Logic (`gemini-3.8-flash`)**.
* **Prompt Target Context (Not Implemented / Proposed):** There are **zero** Electron, React, TypeScript, Windows Desktop, Web SPA, REST HTTP backend server, or SQL/PostgreSQL files in this repository.
* **Documentation Standard Applied:** Every document strictly separates **Verified Current Implementation** (anchored to exact file paths, line numbers, and symbols) from **Proposed Target Architecture** (for Electron + React Windows Desktop/Web parity, dedicated API Gateway, and PostgreSQL migration).

---

## Documentation Directory

| # | Document | Primary Audience | Scope Summary |
| :-: | :--- | :--- | :--- |
| **01** | [`docs/01_UI_UX_DESIGN_DOCUMENTATION.md`](docs/01_UI_UX_DESIGN_DOCUMENTATION.md) | Product, UI/UX Designers, Frontend Engineers | Audit of all 22 Compose routes, Onboarding/Auth gates, Editorial Studio design system, adaptive Bar/Rail navigation, accessibility, UI defects (`ISS-001`, `ISS-014`–`ISS-019`), and Electron/Web redesign blueprint. |
| **02** | [`docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`](docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md) | Backend & Cloud Architects, Security Engineers | Audit of serverless Firebase/OkHttp topology (`OP-001`..`OP-015`), Credential Manager + App Check, YouTube scraping pipeline, APK verification updater, and proposed REST/WebSocket Backend API (`API-001`..`API-014`). |
| **03** | [`docs/03_USER_SCALABILITY_DOCUMENTATION.md`](docs/03_USER_SCALABILITY_DOCUMENTATION.md) | Principal Architects, SRE, FinOps | Baseline concurrency (`15–16` Firestore listeners/user), bottleneck analysis (`ISS-010`, `ISS-005`, `ISS-007`), and 7-tier growth projections (`100` to `1,000,000` users) separating estimates from unverified load capacity. |
| **04** | [`docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md`](docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md) | Engineering Leads, QA, Maintainers | Full 17-field register of 28 confirmed bugs, incomplete features, security/integrity risks, and tech debt (`ISS-001`..`ISS-028`) with file/line evidence and P0–P3 remediation roadmap. |
| **05** | [`docs/05_COMPLETE_FEATURE_DOCUMENTATION.md`](docs/05_COMPLETE_FEATURE_DOCUMENTATION.md) | Product Managers, QA, Full-Stack Engineers | Exhaustive inventory of 40 feature domains (`FEAT-001`..`FEAT-040`) with strict status tagging (`Implemented`, `Partially implemented`, `Broken`, `Not implemented`) and MVP-to-V2.0 roadmap. |
| **06** | [`docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md`](docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md) | AI/ML Engineers, Prompt Architects | Deep dive into `GeminiCoachService` (`gemini-3.8-flash`), 3 JSON schemas, `RecommendationEngine`, Capabilities A–L (13-subsection specs), guardrails, `19 MB` payload cap, and Vertex/File API evolution. |
| **07** | [`docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md`](docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md) | Database Architects, Security Engineers | Audit of all 22 Firestore collections (`firebase-blueprint.json` + `firestore.rules`), DataStore/Keystore local storage, ER diagrams, and proposed 28-table PostgreSQL migration schema. |

---

## Recommended Reading Order

1. **Product & Executive Overview:** Start with [`05_COMPLETE_FEATURE_DOCUMENTATION.md`](docs/05_COMPLETE_FEATURE_DOCUMENTATION.md) -> [`04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md`](docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md).
2. **Design & Experience Leads:** Read [`01_UI_UX_DESIGN_DOCUMENTATION.md`](docs/01_UI_UX_DESIGN_DOCUMENTATION.md) -> [`05_COMPLETE_FEATURE_DOCUMENTATION.md`](docs/05_COMPLETE_FEATURE_DOCUMENTATION.md).
3. **Backend, AI & Database Architects:** Read [`02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`](docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md) -> [`07_DATABASE_DESIGN_AND_DOCUMENTATION.md`](docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md) -> [`06_AI_FEATURES_AND_AI_ARCHITECTURE.md`](docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md) -> [`03_USER_SCALABILITY_DOCUMENTATION.md`](docs/03_USER_SCALABILITY_DOCUMENTATION.md).

---

## Audit Completion Status

| Deliverable | Status | Cross-Reference IDs Verified |
| :--- | :--- | :--- |
| `docs/01_UI_UX_DESIGN_DOCUMENTATION.md` | **Complete** | `ISS-001`, `ISS-003`, `ISS-014`–`ISS-019`, `ISS-021`, `ISS-024` |
| `docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md` | **Complete** | `OP-001`–`OP-015`, `API-001`–`API-014`, `ISS-002`–`ISS-012` |
| `docs/03_USER_SCALABILITY_DOCUMENTATION.md` | **Complete** | 7-tier matrix (`100`..`1M`), `ISS-004`–`ISS-010` |
| `docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md` | **Complete** | `ISS-001`–`ISS-028` (28 issues, 17-field template) |
| `docs/05_COMPLETE_FEATURE_DOCUMENTATION.md` | **Complete** | `FEAT-001`–`FEAT-040` |
| `docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md` | **Complete** | Capabilities `A`–`L`, `ISS-003`, `ISS-005`–`ISS-007`, `ISS-021` |
| `docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md` | **Complete** | 22 Firestore collections, 28 PG tables, `ISS-002`, `ISS-008`–`ISS-012`, `ISS-027` |
| `README_DOCUMENTATION_INDEX.md` | **Complete** | All 7 documents indexed |

---

## Key Verified Findings Snapshot

* **Top Confirmed Defects Requiring P0 Action:**
  * `ISS-001`: Stage 1 "Mute Pass" in `SpeakingLabScreen` calls `AudioRecorderManager` (`MediaPlayer` audio-only) with no video player surface for CameraX/uploaded `.mp4` clips.
  * `ISS-002`: `SpeakCraftRepository.saveDailyActivity` increments `streakDays` on any non-matching date without checking consecutive days, and hardcodes `totalPracticeMinutes += 3L`.
  * `ISS-003`: `SpeakingAndLabsViewModel.submitForAiAnalysis` emits `UiState.Success` even if Firestore persistence fails.
  * `ISS-018`: `Daily`/`Weekly`/`Monthly` period filter chips on Analytics & Dashboard change header strings without filtering `sessions`.
  * `ISS-019`: Destructive session/history deletion runs without confirmation dialogs.

---

## Outstanding Information Gaps & Open Questions

1. **Target Platform Strategy (`ISS-024`):** Confirm whether SpeakCraft will remain Android-first (expanding to tablets/ChromeOS) or initiate companion **Electron (Windows Desktop)** and **React (Web)** repositories backed by a unified API Gateway.
2. **Media Storage Policy (`ISS-004`):** Decide between Firebase Cloud Storage vs. GCS/S3 signed URLs for cross-device audio/video sync and define raw `.mp4` retention limits.
3. **Production App Check & Play Compliance:** Confirm `recaptcha_site_key` provisioning for release Android builds (`AppCheckConfig.kt:46`) and whether an in-app `"Delete Account & Cloud Data"` flow is required alongside `"Delete Speaking Session History"`.
4. **Load & AI Cost Budgets:** Establish per-user daily AI quotas (`analyzeSpeechRecording`, `sendConversationTurn`, `youtubeInspect`) and execute formal load benchmarks prior to scaling beyond Pilot/Early Beta (`>1,000` users).
