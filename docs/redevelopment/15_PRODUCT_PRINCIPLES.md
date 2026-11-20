# 15 — Product and Experience Principles

| Field | Value |
|---|---|
| Document ID | RD-15 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — **Recommendation**, awaiting Product Owner approval |
| Owner (role) | Chief Product Officer / UX Strategist |
| Approval | Pending — Product Owner |
| Related | RD-13 Vision, RD-14 Strategy, DES-05 Design System, DES-07 Accessibility, DES-08 Redesign Decisions, ARC-05 Local Intelligence, SEC-01 |

---

## 1. How to use these principles

Principles resolve disagreements. When a design, requirement or implementation choice is contested, the option that better satisfies the higher-numbered principle **loses** to the lower-numbered one (Principle 1 outranks Principle 2, and so on). Each principle states the failure in the current product that motivated it, so that the reason is never forgotten.

## 2. Product principles

### PP-1 Honest before impressive
**We never show a number, label or status that the evidence does not support.**
- Every metric is labelled **Measured** (computed directly from audio or transcript) or **Estimated** (heuristic inference).
- When evidence is insufficient, the result is **Inconclusive — try again**, never a guessed score or a failing grade.
- Capability states ("Offline speech: Ready") are shown only after verification on the device.
- Names match what is measured: recognition agreement is never called "pronunciation"; a template engine is never called "AI".

*Motivated by:* fabricated 78/100 scores; free-speech "Pronunciation" computed from loudness and pitch; "AI Mentor" built from templates; Settings showing MediaPipe "ready" with no model files (RD-03 FND-AI-05, FND-AI-08, FND-DOM-04).

### PP-2 Private by default, explicit when not
**Recordings and transcripts stay on the device unless the user makes an informed, specific choice.**
- No audio, video or transcript is sent to any server by default — including the platform's network speech service.
- If the device can only transcribe online, the app explains what is sent, to whom, and asks first (consent can be withdrawn).
- Syncing progress metrics is separate from syncing transcripts; each has its own setting.
- The privacy text in the app must be true; it is covered by automated tests.

*Motivated by:* Settings text claiming transcripts stay on-device while the code saves them to Firestore; device backup of recordings; silent network recogniser fallback (RD-03 FND-SEC-05…07).

### PP-3 One clear next step
**Every screen has one primary action, and the user always knows what to do next.**
- Today shows one session with one Start button.
- Each result shows one strength and one focus for the next attempt.
- No screen presents more than one hero action or more than ~5 choices at the same level.

*Motivated by:* a dashboard with 15+ sections, 23 routes and ~10 overlapping labs; three daily plans on three screens (RD-03 FND-UI-02, FND-DOM-01).

### PP-4 Speaking first
**Every session includes real speaking, and the fastest path in the app is "start speaking".**
- A new user speaks within the first three minutes.
- Reading, tapping and quizzes support speaking; they never replace it in the daily session.

*Motivated by:* daily routines completed by ticking boxes, and activities completed by typing three words (RD-03 FND-DOM-05, FND-DOM-06).

### PP-5 Fewer, better features
**A feature exists only if it serves a pillar, has an owner capability (CAP-nn), and has a measurable outcome.**
- Duplicates are merged, not kept "for compatibility".
- New features enter through the PRD with an ID and a success measure.

*Motivated by:* 3 daily plans, 4 streaks, 4 recommenders, 2 curricula, 4 speech-recognition wrappers, 5 availability sources (RD-03 §11).

### PP-6 Works anywhere
**Core practice works offline, on mid-range devices, without an account.**
- Local data is the source of truth; sync is opportunistic and never blocks the UI.
- Network is optional for every core-loop step.

*Motivated by:* Firestore writes awaited on UI paths that hang offline; mandatory sign-in before any use (RD-03 FND-DATA-03, FND-SEC-01).

### PP-7 Progress from evidence, motivation without manipulation
**Progress claims come from measured attempts; motivation comes from visible improvement, not guilt.**
- One streak definition, earned by completing a genuine speaking task.
- No fabricated minutes, no "words learned" for words merely seen.
- Missing a day never erases history; recovery is gentle.
- No dark patterns: no loss-framed nagging, no paywalled privacy, no streak-freeze upselling.

*Motivated by:* four streak numbers, "3 minutes per ticked step", words counted as learned on "Need review" (RD-03 FND-DOM-07, FND-DOM-08).

### PP-8 Own our content
**All learning content is original or properly licensed, versioned and shipped with the app.**

*Motivated by:* a primary tab named after a third-party creator, 102 creator references, scraped captions published as "techniques" (RD-03 FND-DOM-09, FND-AI-11).

## 3. Experience (UX) principles

| ID | Principle | What it means in practice | Verified by |
|---|---|---|---|
| UX-1 | **Simple to understand** | Plain words, no internal jargon ("Activity Engine (150)", "Studio", "Labs"); one name per concept everywhere | Terminology list in DES-05 §12; content review |
| UX-2 | **Easy to start** | ≤ 2 taps from app open to recording in a returning session | DES-04 FLOW-02; UI test TC-UX-002 |
| UX-3 | **Feedback users can trust** | Transcript visible; metrics labelled; caveats one tap away | FS-05 acceptance tests |
| UX-4 | **Visible progress** | Today shows today's progress; Progress shows trends from evidence | FS-08 |
| UX-5 | **Personal** | Session content reflects goal, level and recent results; the user can always change them | FS-02, FS-03 |
| UX-6 | **Minimal cognitive load** | One primary action per screen; progressive disclosure for detail | Heuristic review checklist (DES-01 §5) |
| UX-7 | **Offline-first** | Every core screen has a defined offline state that still works | DES-03 state matrix |
| UX-8 | **Consistent navigation** | 4 persistent destinations; predictable back behaviour; no dead ends | DES-02; navigation tests |
| UX-9 | **Accessible** | WCAG 2.2 AA-equivalent on Android: TalkBack, 200% font scale, contrast, 48 dp targets, reduced motion | DES-07; NFR-A11Y-* |
| UX-10 | **Calm and premium** | Restrained colour, generous spacing, purposeful motion; celebration reserved for real milestones | DES-05 |

## 4. Engineering principles (supporting the product principles)

| ID | Principle | Rationale |
|---|---|---|
| EP-1 | **Existing stack by default** (Kotlin, Compose, Material 3, Navigation Compose, manual DI, DataStore, Firebase Auth/Firestore); changes only via approved ADR | PO-REQ-1120 §2 |
| EP-2 | **Single source of truth per datum** — one store, one engine, one definition | Removes the duplication behind inconsistent numbers |
| EP-3 | **Deterministic, testable intelligence** — every evaluation rule has unit tests with positive and negative examples; rule sets are versioned | Enables honest claims and regression safety |
| EP-4 | **Fail closed, explain clearly** — invalid evidence never becomes a score; errors become user-safe messages | Preserves the current evidence-gate strength |
| EP-5 | **No unverified claims in docs or UI** — "tests passed" only with CI evidence; status labels backed by checks | Stops the documentation-reliability problem (RD-05) |
| EP-6 | **Small, reviewable changes** — one epic at a time, feature-flagged, with tests | PO-REQ-1120 §17 Stage B |
