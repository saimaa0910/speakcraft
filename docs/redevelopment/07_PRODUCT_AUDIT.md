# 07 — Product Audit

| Field | Value |
|---|---|
| Document ID | RD-07 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Chief Product Officer |
| Approval | Pending — Product Owner |
| Related | RD-03, RD-04, RD-08…RD-12, RD-13…RD-16, DES-01 |

> Written as a CPO reviewing a product that is not delivering a satisfactory experience. Existing decisions are not defended because effort was spent on them. Labels: **VERIFIED** (code/doc evidence), **INFERRED**, **ASSUMPTION**, **RECOMMENDATION**.

---

## 1. Executive assessment

SpeakCraft today is **a demo of many features, not a product**. It has the surface area of a mature platform — 26 routes, 150 activities, 15 modules, labs, a mentor, a YouTube tool, an admin console, an OTA updater — but no single experience that reliably delivers its promise. The central loop (speak → useful feedback) does not work by default, the numbers it shows are partly invented, and its identity is borrowed from a third-party creator. The documentation describes a far more finished product than the code delivers.

The root problem is not visual design and not missing features. It is **the absence of product focus, combined with an AI-agent development process that added features and self-certified them without verification.**

## 2. Product positioning — answers to the CPO questions

| Question | Current answer (evidence) | Assessment |
|---|---|---|
| What problem does SpeakCraft solve? | Stated: "AI-powered English communication and speaking coach built from @askvinh YouTube lessons" (`metadata.json`). In practice: many practice screens plus a channel-scraping tool (FND-UI-01, UI-03) | **Unclear.** It names a solution (an AI coach) and a source (a creator), not a user problem |
| Who is the primary user? | Never defined. Onboarding accepts Beginner→Advanced and any goal; content targets workplace/interview/presentation (FND-DOM-13) | **Undefined** — the product serves "everyone" and therefore no one well |
| Who is the secondary user? | Undefined | — |
| What makes it valuable? | Claimed: multimodal AI feedback, creator-grounded pedagogy | **Not realised**: feedback is acoustic-only by default (FND-AI-01) and creator grounding is unlicensed (FND-DOM-09) |
| Why choose SpeakCraft? | No differentiator works today; the strongest latent one — on-device privacy — is contradicted by the code (FND-SEC-05…07) | **No defensible reason today** |
| Primary learning outcome? | Not stated; implied "communication mastery" | **Unmeasurable** as stated |
| Core product experience? | None; Home is a 22-tile directory (FND-UI-02) | **Missing** |
| Different from competitors? | Not in practice; competitors already provide transcript-based feedback (RD-11) | **Weaker than competitors on the core** |
| Trying to solve too many problems? | Yes: speaking, pronunciation, articulation, listening, grammar, vocabulary, public speaking, body language, conversation, mentoring, content ingestion, admin, OTA | **Yes — severe** |
| Features aligned with the central purpose? | ~40 % of the codebase serves non-core concerns (YouTube, admin, updater, cloud AI, video, tutorials) (RD-03 §2) | **Poorly aligned** |

### 2.1 Strongest potential positioning (RECOMMENDATION)

> **A private, honest speaking coach for intermediate speakers who need to communicate confidently at work and in interviews.**

**Evidence:** content already skews professional (FND-DOM-13); the Product Owner's goal list centres interviews, workplace and presentations (PO-REQ-1119); every scaled competitor is cloud-dependent and users distrust their scores (RD-11 §4–5); on-device processing is already the stated constraint and keeps marginal cost near zero (RD-11-I8).
**Assumptions:** the segment values privacy and short daily practice (ASM-01, ASM-02); platform recognition is adequate (ASM-05).
**Alternatives considered:** RD-14 §2.2.

## 3. Root causes of dissatisfaction

| # | Root cause | Symptoms (evidence) | What fixes it |
|---|---|---|---|
| RC-1 | **No product focus or target user** | 26 routes; 3 plans; 15 + 10 modules; ~10 labs; "Explore Tutorials for All 24 Features" (FND-UI-01/02/04, DOM-01) | One segment, one core loop, 4 destinations (RD-13, RD-16) |
| RC-2 | **The core promise does not work** | No transcript by default; first utterance only; stubbed transcription (FND-AI-01…04) | Tiered on-device recognition with full capture (ADR-009) |
| RC-3 | **Feedback cannot be trusted** | Mislabelled pronunciation; baseline scores; length-based activity scores; fallback 62; 4 streaks (FND-AI-08, DOM-04, UI-07, DOM-07) | Metric catalogue, Measured/Estimated, Inconclusive, evidence-only progress (ARC-05, PP-1) |
| RC-4 | **Borrowed identity** | "@askvinh" tab; 102 creator references; "VERIFIED" items linking to a channel home page (FND-UI-03, DOM-09) | Own content and voice (PP-8, ADR-011) |
| RC-5 | **Oscillating strategy** | Cloud → zero-cloud → hybrid → locked in ~48 h (RD-05 §3) | Written principles and non-goals with change control (RD-15, RD-16 §6) |
| RC-6 | **Self-certifying process** | "69/69 passed", "verified", "deployed" without CI evidence; CI runs no tests (FND-TEST-01, OPS-01) | Governance gates; CI evidence required for claims (PLN-05, ENG-01) |
| RC-7 | **Breadth over depth** | 20 vocabulary words; 12 lessons; 6 of 7 programs uncompletable; template conversation partner (FND-DOM-10/13, AI-16) | Fewer features, finished properly (PP-5) |
| RC-8 | **Fragile foundation** | No domain layer; god objects; rotation loses state; offline hangs (FND-ARCH-01…03, UI-06, DATA-03) | Restructure on the existing stack (ARC-01) |

## 4. Experience audit by journey stage (summary — detail in RD-09)

| Stage | Verdict | Key issue |
|---|---|---|
| Install & first open | Poor | 2.9 s non-skippable overlay; colour jump splash → app |
| Registration | Friction | Mandatory Google sign-in before any value (FND-SEC-01) |
| Onboarding | Weak | Self-rated sliders later displayed as "objective" skills; no speaking; errors invisible (FND-UI-07) |
| Goal & level | Inconsistent | 4 vs 5 vs 3 levels; goal never reaches the plan (FND-DOM-03, DOM-12) |
| Dashboard | Overloaded | ~21 widgets, ~70 targets; fabricated values (FND-UI-02, UI-07) |
| Daily practice | Confusing | Three different "today" systems; ticking boxes counts as practice (FND-DOM-06) |
| Speaking activity | Inconsistent | 10 recorders, 12 labels, same engine (FND-UI-04) |
| Evaluation & feedback | Untrustworthy | Acoustic-only by default; mislabelled scores (FND-AI-01, AI-08) |
| Progress | Misleading | Four streaks; invented minutes (FND-DOM-07/08) |
| Vocabulary | Thin | 20 words; 5-stage lifecycle built but unreachable (FND-DOM-02/13) |
| Returning user | Generic | Same "Welcome back" as first visit; no "what's next" beyond a hero pager |
| Notifications | Absent | No OS reminders (FND-DOM-14) |
| Long-term learning | Unsupported | No evidence-based progress; programs uncompletable |

## 5. Feature audit summary (detail in RD-10)

| Class | Count | Examples |
|---|---|---|
| Essential (rebuild) | 7 | Speaking attempt + evaluation, daily session, progress & streak, vocabulary lifecycle, onboarding, settings/privacy, sync |
| High value (rebuild/merge) | 5 | Practice content (activities), read-aloud/repeat drills, reminders, account, evidence gate |
| Supporting | 4 | Theme/appearance, profile, export/delete, diagnostics |
| Optional / deferred | 4 | Role-play, check-ins, notebook/quiz, inbox |
| Experimental (remove) | 3 | Whisper/Moonshine, MediaPipe body language, multimodal fusion |
| Redundant (merge away) | 10+ | Labs, duplicate plans/streaks/paths/programs/recommenders, notification sheet |
| Low value / not needed (remove) | 6 | YouTube intelligence, AI Mentor, conversation simulator (template), admin console, OTA updater, tutorial overlays |

## 6. Business and engineering assessment

| Dimension | Rating | Notes |
|---|---|---|
| User value delivered today | Low | Core loop non-functional by default |
| Trust and honesty | Low | Fabricated/mislabelled outputs |
| Privacy & compliance | Low | Claims contradicted; deletion unreachable; debug builds possible |
| Legal exposure | High risk | Creator content, scraping |
| Engineering maintainability | Low–Medium | Good building blocks; poor structure |
| Test confidence | Low | Never run in CI; false-assurance tests |
| Reusable assets | Medium | RD-03 §13 |

## 7. Conclusions and recommendations

1. **Stop adding features.** Re-found the product on one segment and one loop (RD-13…RD-16).
2. **Make the core work and make it honest** before anything else: tiered recognition, metric catalogue, Inconclusive, evidence-only progress.
3. **Remove** what is unsafe, unlicensed, misleading or off-strategy in R0 (RD-16 §5).
4. **Rebuild on the existing stack**, reusing algorithms and content, behind feature flags, one epic at a time (PLN-01).
5. **Change the process**: every claim needs CI evidence; every feature needs a PRD ID and a measure; every direction change needs an approved decision (PLN-05).

Decisions required: RD-99 §8 (D-01…D-20).
