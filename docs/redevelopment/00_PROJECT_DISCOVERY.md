# 00 — Project Discovery

| Field | Value |
|---|---|
| Document ID | RD-00 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Solution Architect |
| Approval | Pending — Product Owner |
| Related | RD-01…RD-06, RD-99, GOV-02 |

---

## 1. Purpose

This is the entry point to the Stage A discovery: what SpeakCraft is today, how it got here, how the discovery was performed, and the headline conclusions. Details are in RD-01 (documents), RD-02 (stack), RD-03 (code), RD-04 (feature matrix), RD-05 (conflicts) and RD-06 (gaps).

## 2. The project at a glance (VERIFIED)

| Item | Fact | Evidence |
|---|---|---|
| Product | SpeakCraft — Android app for spoken English and communication coaching | `metadata.json`; app strings |
| Repository | `f:\speakcraft`, remote `github.com/saimaa0910/speakcraft`, branch `main`, clean | `git status`, `git remote -v` |
| History | 26 commits, 2026-11-18 → 2026-11-20 (three days) | `git log` |
| Origin | Scaffolded in Google AI Studio (applicationId `com.aistudio.speakcraft.qxmvkp`; "compile_applet" references; `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` in `metadata.json`) | `app/build.gradle.kts:28`; `metadata.json` |
| Stack | Kotlin 2.2.10, Jetpack Compose (BOM 2024.09) + Material 3, Navigation Compose, manual DI, DataStore, Firebase Auth/Firestore/App Check/AI Logic, CameraX, MediaPipe, TFLite; AGP 9.1.1; single module | RD-02 |
| Size | 106 main Kotlin files, 48,894 lines; 25 test files (161 + 1 `@Test`) | RD-03 §2 |
| Documentation | 59 pre-existing documents (48 in `docs/`, 6 in `app/docs/`, root index, models README, metadata, blueprint, one prior Product Owner request) | RD-01 |
| Distribution | GitHub Releases APKs + in-app self-updater; no Play Store listing | RD-03 FND-OPS-04 |
| Users | Unknown; no analytics, no store listing — ASSUMPTION: the Product Owner and a small number of testers | — |

## 3. How the product evolved (reconstructed from git and docs; detail in RD-05 §3)

| Phase | What happened | Effect |
|---|---|---|
| 1 | AI Studio scaffold: cloud-first Gemini "AI communication coach" built on one YouTube creator's (@askvinh) frameworks | Creator-centred identity and content |
| 2 | Baseline audit docs (01–07) framed against desktop/web targets never built | Documentation drift begins |
| 3 | UI/UX redesign, beta channel, then an undocumented switch to a Violet palette | Design docs stale |
| 4 | 150-activity engine added with nine cloud evaluators | Second learning system |
| 5 | Pivot to "zero cloud AI"; on-device LLM added and removed in the same commit | Local heuristic engines replace cloud |
| 6 | Product Owner request PO-REQ-1119: Android-only, no LLM, no cloud AI, full learning experience (13 phases) | Third learning system (`LearningProgressManager`), mostly unwired |
| 7 | "Hybrid AI coach" re-adds cloud AI one day later | Contradicts PO-REQ-1119 |
| 8 | Latest commit locks cloud off with a constant; dependency remains; fixes a fabricated-score bug; breaks speaking-session saves via rules mismatch | Current state |

**Pattern:** each round added features and self-reported "verified/passing" documentation without CI evidence; nothing was removed; direction on AI changed four times in about 48 hours.

## 4. Discovery method

| Step | What was done | Output |
|---|---|---|
| Document discovery | Read all 59 documents in full; git dates; spot-checks against code | RD-01, RD-05 |
| Source discovery | Four parallel static audits (UI/navigation; learning/activity/evaluation; AI/speech/media; platform/security/build/tests) with `path:line` evidence | RD-03 (and four working papers) |
| Stack inventory | Build files, version catalog, imports | RD-02 |
| Documentation vs code | Feature traceability classification | RD-04 |
| Market research | Public sources on 9+ competitors (accessed 2026-11-20); no project details in queries | RD-11 |
| Product analysis | Audit, personas, journeys, feature value, opportunities | RD-07…RD-12 |

**Limitations.** No build, run, device test or test execution was possible (no JDK/Android SDK; Gradle wrapper not committed). No user research, analytics or support data exists. Firebase console state (deployed rules, App Check enforcement, secrets) could not be inspected. Runtime behaviours are therefore marked INFERRED.

## 5. Source identifiers used across the documentation

| Alias | Meaning |
|---|---|
| **PO-REQ-1119** | The Product Owner's 19-Nov-2026 request (`public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484`), inventoried as **DOC-059** in RD-01 |
| **PO-REQ-1120** | The Product Owner's 20-Nov-2026 Stage A brief that commissioned this documentation |
| DOC-nnn | Pre-existing documents (RD-01) |
| CONF-nnn | Documentation conflicts (RD-05) |
| FND-*-nn | Code-audit findings (RD-03) |

## 6. Verified facts, assumptions, recommendations

### 6.1 Most important verified facts
1. The core loop — speak and receive useful feedback — **does not work by default**: no transcript unless live transcription is enabled, then only the first utterance (FND-AI-01…04).
2. Several displayed scores **measure something other than their label** or are length formulas (FND-AI-08, FND-DOM-04); fabricated fallbacks appear on the dashboard (FND-UI-07).
3. The product is **three parallel learning systems** with 26 routes, 3 daily plans, 4 streaks and ~10 overlapping labs (FND-DOM-01, FND-UI-01/04).
4. A primary tab and most curriculum content are built around a **third-party creator without recorded permission** (FND-UI-03, FND-DOM-09).
5. **Privacy claims are contradicted** by code (FND-SEC-05…07); account deletion is unreachable (FND-SEC-03).
6. **Speaking-session saves are currently rejected** by the Firestore rules (FND-DATA-06).
7. CI publishes a release on every push, can ship debug builds, and runs no tests (FND-SEC-02, FND-OPS-01).
8. There is valuable, reusable work: the evidence gate, DSP building blocks, alignment algorithm, SM-2 and event models, 150 activity texts, Firestore rule patterns, test harness (RD-03 §13).

### 6.2 Key assumptions (to validate — PLN-04)
ASM-01 primary segment fit; ASM-02 privacy matters to users; ASM-03 ten-minute sessions; ASM-04 measured insights without a composite score are acceptable; ASM-05 platform recognition is accurate enough; ASM-06 optional sign-in is understood; ASM-07 reference devices available for testing.

### 6.3 Headline recommendation
Restructure SpeakCraft on its existing stack into a focused, private, honest speaking coach with one core loop and four primary destinations (RD-13…RD-16), delivered in increments R0 → R1 → R2 after approval (PLN-01). No product code is changed in Stage A.

## 7. Stage A deliverables map

| Phase | Documents |
|---|---|
| 0 Discovery | RD-00…RD-06 |
| 1 Product audit | RD-07…RD-12 |
| 2 Direction | RD-13…RD-16 |
| 3 Requirements | PRD, BRD, USER_STORIES, FEATURE_SPECIFICATIONS (FS-00…FS-12), SRS, USE_CASES |
| 4 UX/UI | DES-01…DES-08 |
| 5 Architecture | ARC-01…ARC-05, ADR-001…ADR-015, SEC-01 |
| 6 NFR | NFR |
| 7 Quality | TST-01 |
| 8 DevOps | ENG-01 |
| 9 Planning | PLN-01…PLN-05 |
| 10 Governance | GOV-01, GOV-02 |
| 11 Review | RD-99 |

Index and status: GOV-02.
