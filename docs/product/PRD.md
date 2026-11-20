# Product Requirements Document (PRD) — SpeakCraft

| Field | Value |
|---|---|
| Document ID | PRD |
| Version | 0.2 — Draft for review (2026-11-21: Product Owner direction added as PRD-FR-047 feedback with mistakes and coach tips after every submission, PRD-FR-056 technique curriculum from the @askvinh channel, PRD-FR-057 D-08 decided "original wording, link out") |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval (gate G1) |
| Owner (role) | Product Manager |
| Approval | Pending — Product Owner |
| Related | BRD, RD-13…RD-16, SRS, NFR, USER_STORIES, FEATURE_SPECIFICATIONS, DES-02, GOV-01 |

> **Authority.** This PRD is the authoritative source for product functional requirements (`PRD-FR-nnn`) and product quality expectations (`PRD-NFR-nnn`). Detailed, testable behaviour lives in the SRS; workflows and UI states in the feature specifications (FS-nn). Capability IDs (`CAP-nn`) are defined in RD-16.

---

## 1. Executive summary

SpeakCraft is being redesigned from a sprawling, partly non-functional "AI communication studio" into a focused, private speaking coach. The new product delivers one core loop — **a ten-minute daily session → speak → see what you said → honest, specific feedback → try again** — supported by a practice library, a vocabulary lifecycle, evidence-based progress, reminders and complete privacy controls. It runs on-device on the existing Android stack, works offline and without an account, and makes no claim it cannot support.

## 2. Product overview

| Item | Description |
|---|---|
| Platform | Android phone first (tablet-adaptive), minSdk 24 (tier policy D-14), Kotlin/Compose |
| Primary navigation | Four destinations: **Today · Practice · Words · Progress** (DES-02) |
| Core loop | Today session → speaking attempt → feedback → retry → progress |
| Intelligence | On-device, deterministic metric and rule engines; platform speech recognition tiers (ARC-05, ADR-009) |
| Data | Local-first (Room, ADR-002); optional Google sign-in to sync metrics to Firestore (ADR-014) |
| Not included | Cloud AI, chatbots, social, exam prediction, admin console, self-updating APKs (RD-16 §4) |

## 3. Problem statement

Intermediate English speakers know enough English but freeze, ramble or sound unsure when speaking at work and in interviews, because they rarely practise aloud, cannot hear themselves objectively, and lack a private way to rehearse. The current SpeakCraft fails them because (a) it offers too many overlapping features with no clear path (RD-03 FND-UI-01/02, DOM-01), (b) its feedback does not work by default and is partly fabricated (FND-AI-01, AI-08, DOM-04), and (c) its privacy promises are not true (FND-SEC-05…07).

## 4. Product vision

"Everyone who can speak English should be able to speak it with confidence — at work, in interviews and in everyday life." (RD-13 §2)

## 5. Business objectives

BO-01 trustworthy core loop · BO-02 daily habit · BO-03 measurable improvement · BO-04 verifiable privacy and store-readiness · BO-05 reduced complexity · BO-06 no legal/policy exposure (authoritative in BRD §3).

## 6. User segments

Primary S1: working professionals and job seekers at ≈ B1–B2. Secondary S2: students and early-career graduates. Served: advanced speakers (S3), elementary learners with guided templates (S4). Not targeted: complete beginners, native public speakers, exam candidates (RD-14 §2; decision D-01).

## 7. User personas

| ID | Persona | Segment | Core need |
|---|---|---|---|
| P1 | Aarav — software engineer, 28, B2, daily stand-ups and client calls | S1 | Speak concisely and confidently in meetings |
| P2 | Meera — job seeker, 31, B1+, returning to work | S1 | Rehearse interview answers privately |
| P3 | Daniel — final-year student, 21, B1 | S2 | Placement interviews and presentations |
| P4 | Sofia — senior analyst, 38, C1 | S3 | Polish delivery: pace, pauses, fillers |
| P5 | Hoang — shop supervisor, 34, A2+ | S4 | Everyday clarity via read-aloud and repeat |

Full personas: RD-08. Personas are **evidence-informed hypotheses**, not research findings (no user research exists — ASM-01).

## 8. User needs

| ID | Need (user's words) | Personas | Addressed by |
|---|---|---|---|
| UN-01 | "Tell me what to practise today — I have ten minutes." | All | CAP-03 |
| UN-02 | "Let me rehearse the real situation." | P1, P2, P3 | CAP-06 |
| UN-03 | "Show me exactly what I said and what to fix next." | All | CAP-04, CAP-05 |
| UN-04 | "Don't give me a score I can't trust." | All | CAP-05 |
| UN-05 | "Help me use better words when I speak." | P1, P2, P3, P5 | CAP-07 |
| UN-06 | "Show me I'm improving." | All | CAP-08 |
| UN-07 | "Remind me, but don't nag me." | All | CAP-09 |
| UN-08 | "Keep my voice private." | All, esp. P2 | CAP-10, CAP-12 |
| UN-09 | "Let it work on the train without signal." | P1, P3, P5 | CAP-11 |
| UN-10 | "Let me start without signing up." | All | CAP-01 |

## 9. Product goals

| ID | Goal | Measure (BRD §8) |
|---|---|---|
| PG-01 | Make the first valid speaking attempt happen fast | KPI-01, KPI-02 |
| PG-02 | Make the daily session the obvious, finishable default | KPI-03, KPI-04 |
| PG-03 | Make feedback trusted | KPI-05, KPI-06 |
| PG-04 | Make improvement visible from evidence | KPI-07 |
| PG-05 | Keep the product private, stable and simple | KPI-08 … KPI-12 |

## 10. Success metrics

Defined authoritatively in BRD §8 (KPI-01 … KPI-12), all **Proposed — requires approval**, measured per ADR-013 / D-09.

## 11. Product scope

In scope: CAP-01 … CAP-12 at the release levels in RD-16 §3. Out of scope: RD-16 §4 non-goals NG-01 … NG-14. Retired features: RD-16 §5.

## 12. Functional capabilities (summary)

| CAP | Capability | Release | Feature spec |
|---|---|---|---|
| CAP-01 | Account & sign-in | R1 | FS-01 |
| CAP-02 | Onboarding & personalisation | R1 | FS-02 |
| CAP-03 | Today — daily session | R1 | FS-03 |
| CAP-04 | Speaking practice session | R1 | FS-04 |
| CAP-05 | Honest feedback & evaluation | R1 | FS-05 |
| CAP-06 | Practice library | R1 / R2 | FS-06 |
| CAP-07 | Words — vocabulary lifecycle | R1 / R2 | FS-07 |
| CAP-08 | Progress & streak | R1 / R2 | FS-08 |
| CAP-09 | Reminders & inbox | R1 / R2 | FS-09 |
| CAP-10 | Profile, settings, privacy & data rights | R1 | FS-10 |
| CAP-11 | Offline-first storage & sync | R0 / R1 | FS-11 |
| CAP-12 | Speech capability management | R1 | FS-12 |

## 13. Non-functional expectations

| ID | Expectation | Detailed in |
|---|---|---|
| PRD-NFR-001 | **Honesty**: no fabricated, defaulted or mislabelled outputs; every metric labelled Measured/Estimated; Inconclusive instead of guessing | NFR-QUAL-*, SRS-BR-001…006 |
| PRD-NFR-002 | **Privacy**: recordings and transcripts stay on-device by default; explicit consent for any exception; privacy text verified by tests | NFR-PRIV-*, SEC-01 |
| PRD-NFR-003 | **Offline**: every core-loop step works without network | NFR-REL-*, NFR-COMP-* |
| PRD-NFR-004 | **Responsiveness**: feedback appears quickly after recording stops; app starts fast; navigation is immediate | NFR-PERF-* |
| PRD-NFR-005 | **Reliability**: no data loss on interruptions, crashes or sync failures; idempotent progress events | NFR-REL-* |
| PRD-NFR-006 | **Accessibility**: TalkBack, 200 % font scale, contrast, 48 dp targets, reduced motion | NFR-A11Y-*, DES-07 |
| PRD-NFR-007 | **Compatibility**: Android 7.0+ with capability tiers; honest degradation | NFR-COMP-*, ADR-009 |
| PRD-NFR-008 | **Security**: per-user isolation; release-signed builds; no PII in logs | NFR-SEC-*, SEC-01 |
| PRD-NFR-009 | **Maintainability**: one engine per concern; tests and CI gates; package-by-feature | NFR-MAINT-* |
| PRD-NFR-010 | **Footprint**: small install size and battery-friendly processing | NFR-PERF-*, NFR-COMP-* |
| PRD-NFR-011 | **Localisation readiness**: all UI strings in resources (UI in English for R1–R2) | NFR-MAINT-* |

## 14. User journeys (summary)

| Journey | Steps | Flow |
|---|---|---|
| J1 First-time user | Open → value screen → goal → level → minutes → reminder → mic + privacy → speech check → first attempt "Introduce yourself" → first feedback → Today | DES-04 FLOW-01 |
| J2 Returning user, daily session | Open → Today → Start → step 1…n (speak/feedback/retry) → session complete → streak update | FLOW-02, FLOW-03 |
| J3 Rehearse a specific situation | Practice → track → activity → speak → feedback → retry → save | FLOW-04 |
| J4 Learn a word | Today word step or Words → Learn → Say it → Use it → Make your own → scheduled review | FLOW-05, FLOW-06 |
| J5 Check progress | Progress → metric trend → attempt history → replay | FLOW-07 |
| J6 Manage privacy and data | Profile → Settings → Speech & privacy / Data → export / clear / delete | FLOW-08, FLOW-09 |

## 15. Feature requirements

Priority: **P0** must have for the release · **P1** should have · **P2** could have · **P3** later. "R" = target release.

### 15.1 CAP-01 Account & sign-in

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-001 | Users can use all core features **without an account**, with data stored in a local profile | P1 | R1 | BR-014; D-05 |
| PRD-FR-002 | Users can **sign in with Google** at any time to back up and sync progress across devices | P1 | R1 | BR-014 |
| PRD-FR-003 | On first sign-in, local progress is **adopted** into the account without loss | P1 | R1 | BR-016 |
| PRD-FR-004 | Users can **sign out**, choosing whether to keep or remove local data on the device | P1 | R1 | PO-REQ-1119 Ph6 §4 |
| PRD-FR-005 | Users can **delete their account and all associated data** after confirmation and re-authentication; the result is verifiable | P0 | R1 | BR-007 |

### 15.2 CAP-02 Onboarding & personalisation

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-010 | Onboarding collects **goal/track, level (5 levels), daily minutes (5/10/15) and reminder preference** in ≤ 5 short steps with sensible defaults | P0 | R1 | BR-002; D-17 |
| PRD-FR-011 | Onboarding explains **privacy** and requests **microphone permission** in context, with a usable path if denied | P0 | R1 | BR-005 |
| PRD-FR-012 | Onboarding determines the device's **speech capability tier** and, if needed, offers offline-pack download or consented online transcription, or explains audio-only mode | P0 | R1 | ADR-009 |
| PRD-FR-013 | Onboarding ends with a **first speaking attempt** ("Introduce yourself") and its feedback, which becomes the user's baseline | P0 | R1 | PG-01 |
| PRD-FR-014 | All onboarding choices can be **changed later** in Settings | P0 | R1 | PO-REQ-1119 Ph2 §2 |

### 15.3 CAP-03 Today — daily session

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-020 | Today presents **one daily session** of 3–4 steps fitting the chosen duration, with one primary "Start" action | P0 | R1 | BR-002; PP-3 |
| PRD-FR-021 | The session is generated **deterministically** from goal, level, weakest measured skill, due words and recent history, and is **stable within the day** | P0 | R1 | PO-REQ-1119 Ph2 §3, §5 |
| PRD-FR-022 | Completed steps are preserved; if adaptation is needed, only **remaining** steps change | P0 | R1 | PO-REQ-1119 Ph2 §5 |
| PRD-FR-023 | After missed days the session is a **gentle recovery** session; no backlog accumulates and history is never erased | P1 | R1 | PO-REQ-1119 Ph2 §4 |
| PRD-FR-024 | Today shows the **streak and today's progress** compactly, and a **personal greeting** for returning users (first visit: welcome; later: welcome back with one relevant fact) | P1 | R1 | PO-REQ-1119 Ph1 (simplified) |
| PRD-FR-025 | Users can **swap** a step for an alternative of the same type | P2 | R1 | — |
| PRD-FR-026 | Users can change today's **duration** before starting | P2 | R1 | — |

### 15.4 CAP-04 Speaking practice session

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-030 | Each speaking step shows the **prompt**, an optional **preparation** timer, a single **record/stop** control with visible recording state and time limit | P0 | R1 | BR-001 |
| PRD-FR-031 | After stopping, the attempt is processed automatically and the **transcript** is shown (or the reason it is unavailable) | P0 | R1 | BR-003 |
| PRD-FR-032 | Attempts with insufficient evidence are shown as **Inconclusive** with the reason and a retry; they never count as completed or as failures | P0 | R1 | BR-003; PO-REQ-1119 Ph8C |
| PRD-FR-033 | Users can **retry** the same prompt and see a **comparison** with their previous attempt | P0 | R1 | Learning philosophy RD-13 §9 |
| PRD-FR-034 | Users can **replay their recording** where the tier and retention setting allow | P1 | R1 | RD-13 §9 (listen to yourself) |
| PRD-FR-035 | **Interruptions** (call, backgrounding, permission revoked, storage full) end or pause the attempt safely with a clear message and no corrupt data | P0 | R1 | PRD-NFR-005 |
| PRD-FR-036 | Read-aloud and repeat templates offer **reference audio** (device text-to-speech) at normal and slow speed | P1 | R1 | PO-REQ-1119 Ph8 |

### 15.5 CAP-05 Honest feedback & evaluation

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-040 | Feedback shows the **three most relevant metrics** for the template, each labelled **Measured** or **Estimated**, with a plain-language band | P0 | R1 | BR-003; PP-1 |
| PRD-FR-041 | Feedback gives **one strength and one focus** for the next attempt, anchored in the user's own words where possible | P0 | R1 | RD-13 §9 |
| PRD-FR-042 | Language observations (grammar, word choice) appear only from **high-precision rules**, phrased as "possible issue" with a suggested alternative | P1 | R1 | RSK-03 |
| PRD-FR-043 | Free-speaking attempts show **no composite overall score**; read-aloud and word-stage checks show a score against a configured pass threshold | P0 | R1 | D-06; PO-REQ-1119 Ph8C |
| PRD-FR-044 | Each result offers **"Was this accurate?"** (yes/no + optional reason), stored locally | P1 | R1 | KPI-05 |
| PRD-FR-045 | Each metric has a **"How we measured this"** explanation including its limitations | P1 | R1 | PP-1 |
| PRD-FR-046 | Every evaluation records the **evaluation-rules version** used | P0 | R1 | PO-REQ-1119 Ph8C |
| PRD-FR-047 | After **every submitted attempt** (speaking activities, technique exercises, word stages and review items), the result lists the **mistakes our checks detected**, each with its evidence and Measured/Estimated label, and gives **coach tips** linked to the relevant technique. If the checks find nothing, the result says so honestly, without claiming the attempt was perfect. An attempt that fails the evidence gate (Inconclusive) shows its specific reason and advice for a clearer recording instead, because there is no reliable evidence to report mistakes from (SRS-BR-008). Tips and mistakes come from deterministic function logic and on-device ("local intelligence") analysis; no cloud AI is required (D-04) | P0 | R1 | Product Owner 2026-11-21 |

### 15.6 CAP-06 Practice library

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-050 | The library is organised by **five tracks** (Everyday conversation, Workplace, Interviews, Presentations & storytelling, Clear speech); each activity shows template, duration and level | P0 | R1 | RD-16 CAP-06 |
| PRD-FR-051 | R1 supports **five templates**: Read aloud, Listen & repeat, Answer a question, Structured answer, Tell a story | P0 | R1 | RD-16 |
| PRD-FR-052 | Any library activity can be started outside Today and **counts toward progress and streak** | P0 | R1 | BR-011 |
| PRD-FR-053 | Content is **bundled, versioned and original** (written from scratch, or channel techniques re-expressed in our own words per PRD-FR-057); R1 ships ≥ 15 activities per track | P0 | R1 | BR-008; RSK-04 |
| PRD-FR-054 | Users can **save** activities for later | P2 | R1 | — |
| PRD-FR-055 | R2 adds **scripted role-play** (multi-turn prompts voiced by text-to-speech; user responses evaluated per turn) | P1 | R2 | RD-16 |
| PRD-FR-056 | The curriculum is built from the **communication frameworks, techniques, exercises, activities and tips** taught on the @askvinh YouTube channel. Each **technique** is a first-class content item: its idea and steps in our own words, its exercises and activities practised through the five templates, and its tips. Techniques can be browsed in Practice and are used by Today's plan | P0 | R1 | Product Owner aim 2026-11-21; D-08 |
| PRD-FR-057 | Creator-derived content follows **D-08 (decided 2026-11-21): original wording, link out**. Every technique, exercise and tip is written in our own words and examples, with no verbatim transcripts. Each item links to the **specific source video** (with timestamp) through a "Watch the original lesson" link, and the app and store listing never use the creator's name, likeness or branding | P0 | R1 | D-08; BR-008 |

### 15.7 CAP-07 Words — vocabulary lifecycle

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-060 | Each word progresses through **Learn → Say it → Use it → Make your own → Review** | P0 | R1 | PO-REQ-1119 Ph8 |
| PRD-FR-061 | A **daily new-word goal** and **due reviews** feed the Today session | P0 | R1 | PO-REQ-1119 Ph8A |
| PRD-FR-062 | Stage checks use configurable **pass thresholds** (defaults: Say it 75, Use it 75, Make your own 70) and Inconclusive handling | P0 | R1 | PO-REQ-1119 Ph8C |
| PRD-FR-063 | Reviews follow a **spaced-repetition** schedule and include **spoken recall** | P0 | R1 | PO-REQ-1119 Ph9 |
| PRD-FR-064 | Users can browse words by **status**, search and filter | P1 | R1 | PO-REQ-1119 Ph8A |
| PRD-FR-065 | R1 ships an original starter bank of **≥ 300 words** across levels and tracks | P0 | R1 | RSK-04 |
| PRD-FR-066 | R2 adds a **notebook** (save words from transcripts and feedback; notes; tags) | P1 | R2 | PO-REQ-1119 Ph9 §3 |
| PRD-FR-067 | R2 adds a deterministic **quiz**, labelled "Quiz" (not "AI") | P2 | R2 | PO-REQ-1119 Ph9 §1 |

### 15.8 CAP-08 Progress & streak

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-070 | Progress shows **trends of measured metrics** per template family (no trend until ≥ 3 valid attempts; noise band shown) | P0 | R1 | BR-011 |
| PRD-FR-071 | Progress lists **attempt history** with transcript, metrics, feedback and replay where available | P0 | R1 | — |
| PRD-FR-072 | **One streak**: consecutive local days with ≥ 1 valid speaking attempt; longest streak; weekly calendar; milestones (3, 7, 14, 30, 60, 100) | P0 | R1 | PO-REQ-1119 Ph4 |
| PRD-FR-073 | Practice time and counts are computed **only from validated events** | P0 | R1 | BR-011 |
| PRD-FR-074 | Progress summarises **word progress** by stage | P1 | R1 | — |
| PRD-FR-075 | R2 adds **check-ins**: a repeatable benchmark task every 2–4 weeks, compared over time | P1 | R2 | RD-11-I6 |

### 15.9 CAP-09 Reminders & inbox

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-080 | A **daily practice reminder** at the user's chosen time, enabled only by user choice | P1 | R1 | BR-012 |
| PRD-FR-081 | Due word reviews are mentioned in the daily reminder; **at most one reminder per day** by default | P1 | R1 | PO-REQ-1119 Ph5 |
| PRD-FR-082 | No reminder is sent if the user has already practised today | P1 | R1 | PO-REQ-1119 Ph5 |
| PRD-FR-083 | Notification permission (Android 13+) is requested **in context**; reminders degrade gracefully if denied | P1 | R1 | PO-REQ-1119 Ph5 |
| PRD-FR-084 | R2 **inbox** for learning messages (milestones, weekly summary) with read/unread and delete; never software-update notices | P2 | R2 | PO-REQ-1119 Ph5 |

### 15.10 CAP-10 Profile, settings, privacy & data rights

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-090 | **Profile** shows name, level, goal/track, daily minutes, member since and key statistics; editable | P1 | R1 | PO-REQ-1119 Ph6 §1 |
| PRD-FR-091 | **Settings** sections: Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account, About | P0 | R1 | PO-REQ-1119 Ph6 §2 |
| PRD-FR-092 | **Speech & privacy** settings: recognition mode and language, offline-pack status/download, online-transcription consent, recording retention | P0 | R1 | ADR-009, ADR-015 |
| PRD-FR-093 | **Export** all personal data as a machine-readable file | P0 | R1 | BR-007 |
| PRD-FR-094 | **Clear** learning history selectively (recordings, attempts, words, everything) with confirmation | P0 | R1 | BR-007 |
| PRD-FR-095 | An accurate **privacy information** screen explaining what is stored where; its claims are covered by automated tests | P0 | R1 | BR-005; PP-2 |
| PRD-FR-096 | **Appearance**: theme Light/Dark/System; reduce motion; text size follows system font scale | P1 | R1 | PO-REQ-1119 Ph6 |
| PRD-FR-097 | **About**: version, licences, privacy policy, terms, help, share diagnostics | P1 | R1 | ADR-007 |

### 15.11 CAP-11 Offline-first storage & sync

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-100 | All core features work **fully offline** | P0 | R1 | BR-004 |
| PRD-FR-101 | Signed-in users' progress **syncs automatically** in the background; sync status is visible; sync never blocks the UI | P1 | R1 | ADR-014 |
| PRD-FR-102 | Multi-device edits are reconciled by **defined conflict rules** without losing completed progress | P1 | R1 | PO-REQ-1119 Ph10 |
| PRD-FR-103 | Existing signed-in users' **profile and vocabulary progress are migrated** on first launch of the new version | P1 | R1 | BR-016; D-19 |

### 15.12 CAP-12 Speech capability management

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-110 | The app determines the **capability tier** (T1/T2/T3a/T3b) and shows an honest status | P0 | R1 | ADR-009; PP-1 |
| PRD-FR-111 | Where supported, users can **install the offline speech language pack** from within the app | P1 | R1 | ADR-009 |
| PRD-FR-112 | **Online transcription** requires explicit, revocable consent naming what is sent and to whom | P0 | R1 | BR-005 |
| PRD-FR-113 | When no transcription is available, **audio-only mode** gives delivery feedback and explains its limits | P0 | R1 | PP-1 |
| PRD-FR-114 | Users can choose the **recognition accent/locale** (en-US, en-GB, en-IN, en-AU) where supported | P2 | R1 | RD-03 FND-AI (existing locales) |

### 15.13 Platform and transition

| ID | Requirement | P | R | Source |
|---|---|---|---|---|
| PRD-FR-120 | Retired features (RD-16 §5) are **removed** from the app, including their data paths and permissions | P0 | R0–R1 | BO-05, BO-06 |
| PRD-FR-121 | Distribution uses **release-signed** builds only; no in-app APK installer (subject to D-03) | P0 | R0 | BR-009 |

## 16. Dependencies

| ID | Dependency | Affects |
|---|---|---|
| DEP-01 | Platform speech recognition quality and APIs per device (SPK-01) | CAP-04, CAP-05, CAP-12 |
| DEP-02 | Original content production (activities, word bank) | CAP-06, CAP-07 |
| DEP-03 | Google Play developer account and listing (if ADR-004 approved) | Distribution, App Check |
| DEP-04 | Approval of ADR-002 (Room) and ADR-005 (WorkManager) | CAP-11, CAP-09 |
| DEP-05 | Firebase project access (existing named database) | CAP-01, CAP-11 |
| DEP-06 | Privacy policy and terms text (owner-provided or drafted for approval) | CAP-10 |

## 17. Constraints

BC-01 … BC-06 (BRD §6). In addition: on-device processing only; no backend services; English-only UI for R1–R2.

## 18. Risks

Top product risks RSK-01 … RSK-08 (RD-14 §8); full register PLN-04.

## 19. Assumptions

ASM-01 … ASM-06 (RD-14 §9), validated during R1 beta.

## 20. Release strategy

| Release | Content | Entry | Exit |
|---|---|---|---|
| R0 Foundation | CAP-11 skeleton, removals (PRD-FR-120), CI gates, release signing (PRD-FR-121), data-safety fixes | Stage A approved (G1–G4, explicit "start Stage B" — PLN-05 GR-01) | RD-06 §5 R0 exit criterion met; CI gates green |
| R1 Core loop (MVP) | All P0/P1 R1 requirements | R0 exit; SPK-01 done | R1 acceptance (§21); beta KPIs measured |
| R2 Depth | R2 requirements | R1 exit | R2 acceptance |
| R3 Reach | Store launch, video self-review, recognition improvements | Decisions D-03, ADR-009/010 | Per decision |

Detailed roadmap: PLN-01.

## 21. Acceptance criteria (release level)

R1 is accepted when **all** of the following hold (detailed test cases in TST-01 and FS-nn):

1. Every P0 R1 requirement has passing acceptance tests traced in GOV-01.
2. A new user can complete onboarding and a valid first attempt offline on a Tier T1 or T2 reference device.
3. No screen shows a metric, score, status or label that is not backed by evidence (honesty review checklist, TST-01 §9).
4. Privacy tests pass: no audio/transcript network egress by default; backup excludes recordings; privacy text matches behaviour.
5. Export, clear history and account deletion work end-to-end and are verified.
6. Accessibility checks pass at 200 % font scale and with TalkBack on the core loop.
7. CI gates (unit, Robolectric, lint, rules tests) pass on the release commit; the build is release-signed.
8. Product Owner signs the UAT report (PLN-05 gate G6).
