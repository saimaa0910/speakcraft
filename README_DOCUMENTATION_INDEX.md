# SpeakCraft Technical Documentation & Architecture Audit Index

**Revision Date:** 2026-11-18  
**Application:** SpeakCraft (`com.aistudio.speakcraft.qxmvkp`, v1.0.0)  
**Repository State:** Architecture audit (`01`–`07`) plus full Material 3 UI/UX transformation, P0/P1 reliability fixes, universal 24-feature interactive tutorial system, and verified test/build pass (`30 / 30` unit & Robolectric CUJ tests passing; `compile_applet` build succeeded).

---

## Critical Platform Alignment Notice

Before reading the detailed specifications below, engineering and product leadership should note a key platform context point:

* **Actual Codebase Reality (Verified):** The repository implements **SpeakCraft as a native Android application** written in **Kotlin + Jetpack Compose (Material 3)**, powered by **Firebase Authentication (Google Credential Manager)**, **Cloud Firestore** (named DB `ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11`), **Firebase App Check**, and **Firebase AI Logic (`gemini-3.8-flash`)**.
* **Historical Audit Docs (`01`–`07`):** Docs `01`–`07` were produced during the initial read-only baseline audit prior to the UI/UX transformation and defect remediation. See Docs `08`–`17` below for the implemented redesign specifications, verification results, and changelog.

---

## Technical Architecture & Audit Directory (`01`–`07`)

| # | Document | Primary Audience | Scope Summary |
| :-: | :--- | :--- | :--- |
| **01** | [`docs/01_UI_UX_DESIGN_DOCUMENTATION.md`](docs/01_UI_UX_DESIGN_DOCUMENTATION.md) | Product, UI/UX Designers, Frontend Engineers | Baseline audit of all 22 Compose routes, design system, navigation, accessibility, and pre-redesign UI defects. |
| **02** | [`docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md`](docs/02_BACKEND_ARCHITECTURE_DOCUMENTATION.md) | Backend & Cloud Architects, Security Engineers | Serverless Firebase/OkHttp topology (`OP-001`..`OP-015`), Credential Manager + App Check, YouTube scraping pipeline, APK verification updater. |
| **03** | [`docs/03_USER_SCALABILITY_DOCUMENTATION.md`](docs/03_USER_SCALABILITY_DOCUMENTATION.md) | Principal Architects, SRE, FinOps | Baseline concurrency, bottleneck analysis, and 7-tier growth projections (`100` to `1,000,000` users). |
| **04** | [`docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md`](docs/04_ISSUES_BUGS_AND_TECHNICAL_DEBT.md) | Engineering Leads, QA, Maintainers | Full register of 28 baseline bugs and tech debt (`ISS-001`..`ISS-028`) with file/line evidence and P0–P3 remediation roadmap. |
| **05** | [`docs/05_COMPLETE_FEATURE_DOCUMENTATION.md`](docs/05_COMPLETE_FEATURE_DOCUMENTATION.md) | Product Managers, QA, Full-Stack Engineers | Exhaustive inventory of 40 feature domains (`FEAT-001`..`FEAT-040`) and MVP-to-V2.0 roadmap. |
| **06** | [`docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md`](docs/06_AI_FEATURES_AND_AI_ARCHITECTURE.md) | AI/ML Engineers, Prompt Architects | Deep dive into `GeminiCoachService` (`gemini-3.8-flash`), JSON schemas, `RecommendationEngine`, guardrails, and payload limits. |
| **07** | [`docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md`](docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md) | Database Architects, Security Engineers | Audit of all 22 Firestore collections (`firebase-blueprint.json` + `firestore.rules`), DataStore/Keystore local storage, and relational schema proposals. |

---

## UI/UX Redesign, Tutorial System & Verification Directory (`08`–`17`)

| # | Document | Scope Summary |
| :-: | :--- | :--- |
| **08** | [`docs/UI_UX_CURRENT_STATE_AUDIT.md`](docs/UI_UX_CURRENT_STATE_AUDIT.md) | Pre-redesign UX friction, visual hierarchy, and component-level audit. |
| **09** | [`docs/COMPETITIVE_UX_ANALYSIS.md`](docs/COMPETITIVE_UX_ANALYSIS.md) | Benchmarking against leading communication, language, and coaching experiences. |
| **10** | [`docs/SPEAKCRAFT_NEW_INFORMATION_ARCHITECTURE.md`](docs/SPEAKCRAFT_NEW_INFORMATION_ARCHITECTURE.md) | Streamlined 4-hub navigation hierarchy, 24-route structure, and discoverability architecture. |
| **11** | [`docs/SPEAKCRAFT_DESIGN_SYSTEM.md`](docs/SPEAKCRAFT_DESIGN_SYSTEM.md) | `SpeakCraftColors` tokens, `SpeakCraftSpacing` (`4dp`/`8dp` grid), typography hierarchy, and theme specification. |
| **12** | [`docs/SCREEN_BY_SCREEN_REDESIGN_SPECIFICATION.md`](docs/SCREEN_BY_SCREEN_REDESIGN_SPECIFICATION.md) | Screen-by-screen blueprint covering Onboarding, Dashboard, Speaking Studio, Labs, Vocabulary, Mentor, Analytics, Settings, and Updates. |
| **13** | [`docs/INTERACTION_AND_MOTION_GUIDELINES.md`](docs/INTERACTION_AND_MOTION_GUIDELINES.md) | Transitions, animated card flips, waveform motion, and `rememberReducedMotionEnabled()` compliance. |
| **14** | [`docs/ACCESSIBILITY_AUDIT.md`](docs/ACCESSIBILITY_AUDIT.md) | TalkBack `heading()` semantics, `48dp` touch targets, color contrast tokens, and content descriptions. |
| **15** | [`docs/UI_UX_IMPLEMENTATION_ROADMAP.md`](docs/UI_UX_IMPLEMENTATION_ROADMAP.md) | 5-stage implementation plan mapping P0/P1 fixes and UI upgrades across codebase layers. |
| **16** | [`docs/UI_UX_TESTING_AND_VALIDATION_REPORT.md`](docs/UI_UX_TESTING_AND_VALIDATION_REPORT.md) | Verified `30 / 30` unit & Robolectric Compose CUJ test results, `compile_applet` build check, and manual device verification checklist. |
| **17** | [`docs/UI_UX_CHANGELOG.md`](docs/UI_UX_CHANGELOG.md) | Summary of design system tokenization, defect resolutions (`ISS-001`..`ISS-028`), and the 24-feature tutorial system. |

---

## Verified Key Improvements & Outstanding Items

* **Resolved Defects in Current Codebase:**
  * `ISS-001`: `VideoReviewPlayerCard` wired into `SpeakingLabScreen` with real `VideoView` playback and Stage 1 muted (`volume = 0f`) vs. Stage 2/3 review controls.
  * `ISS-002`, `ISS-020`, `ISS-028`: Consecutive-day calendar streak validation, `SystemClock.elapsedRealtime()` + `MediaMetadataRetriever` audio duration, and dynamic `todayDateKey()` calculation.
  * `ISS-003` & `ISS-005`: Honest Firestore failure surfacing via `CloudSyncWarningBanner` + `retryCloudSync()`, plus `OpenableColumns.SIZE` pre-check against the `19 MB` multimodal cap.
  * `ISS-014` & `ISS-015`: Tokenized `MaterialTheme.speakCraftColors`, unified `Theme.SpeakCraft` manifest/resource styling, and differentiated font weights.
  * `ISS-017`, `ISS-018`, `ISS-018B`, `ISS-019`, `ISS-021`: Backup snapshot restore UI, true timestamp-window filtering (`Daily`/`Weekly`/`Monthly`), specialized `articulation_lab` vs. `pronunciation` lab defaults, confirmation dialogs on destructive actions, and persisted multi-turn AI Mentor chat bubbles.
  * **Per-Feature Interactive Tutorials:** `FeatureTutorialRegistry` (24 features), inline collapsible `FeatureGuideStrip`, and searchable `FeatureTutorialDialog` accessible from both the TopAppBar (`?`) and the Dashboard.
* **Remaining Hardware / Platform Considerations:**
  * **Live Emulator Visual & Hardware Checks:** Execute camera/microphone capture, Google Credential Manager sign-in, and TalkBack sweeps on a physical Android device or streaming emulator.
  * **Long-Term Platform Decisions:** Companion Desktop/Web parity (`ISS-024`), cloud media storage retention policy (`ISS-004`), and release `recaptcha_site_key` provisioning (`AppCheckConfig.kt`).
