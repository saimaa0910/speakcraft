# 16 — Product Scope and Non-Goals

| Field | Value |
|---|---|
| Document ID | RD-16 |
| Version | 0.2 — Draft for review (2026-11-21: technique curriculum and after-attempt mistakes/tips added per Product Owner direction; creator content disposition changed per D-08 decision) |
| Date | 2026-11-21 |
| Status | Draft — **Recommendation**, awaiting Product Owner approval |
| Owner (role) | Product Manager |
| Approval | Pending — Product Owner |
| Related | RD-10 Feature Value Assessment, RD-14 Strategy, PRD §11, PLN-01 Roadmap, PLN-02 WBS |

> This document is the **authoritative list of product capabilities (CAP-nn)** and release scope. The PRD, SRS, feature specifications, WBS and RTM reference these IDs.

---

## 1. Scope summary

| In scope (R0–R2) | Deferred (R3, conditional) | Out of scope (non-goals) |
|---|---|---|
| 12 capabilities that deliver the core loop: daily session → speak → honest feedback → retry, supported by practice content, vocabulary, progress, reminders, privacy and offline-first storage | Store launch, video self-review, recognition-model improvements, content packs, UI localisation | Cloud AI, chatbots, social, exams, beginner curriculum, web/desktop, in-app admin, monetisation (until D-07) |

## 2. Capability catalogue (authoritative)

| CAP | Capability | Pillar | Release | Feature spec | Description |
|---|---|---|---|---|---|
| CAP-01 | **Account & sign-in** | F-2 | R1 | FS-01 | Use without an account (local profile); optional Google sign-in to back up and sync; sign-out; account deletion |
| CAP-02 | **Onboarding & personalisation** | P-1 | R1 | FS-02 | Goal, level (self-selected + optional short speaking check), daily minutes, reminder time, microphone permission and privacy choices; first speaking attempt inside onboarding |
| CAP-03 | **Today — daily session** | P-1 | R1 | FS-03 | A deterministic daily session (≈5/10/15 min) of 3–4 steps built from goal, level, weakest measured skill and due vocabulary; stable within the day; gentle recovery after missed days |
| CAP-04 | **Speaking practice session** | P-2 | R1 | FS-04 | Prompt → optional preparation → record → evidence check → transcript → feedback → retry/compare; handles interruptions and permission denial |
| CAP-05 | **Honest feedback & evaluation** | P-2 | R1 | FS-05 | Measured and estimated metrics per template; one strength + one focus; **mistakes found and coach tips after every submitted attempt** (deterministic and on-device; PRD-FR-047); *Inconclusive* state; "Was this accurate?" rating; versioned rule sets |
| CAP-06 | **Practice library** | P-1 | R1 (5 tracks, 5 templates); R2 (role-play) | FS-06 | Tracks: Everyday conversation, Workplace, Interviews, Presentations & storytelling, Clear speech. Templates R1: Read aloud, Listen & repeat, Answer a question, Structured answer, Tell a story. **Techniques collection**: the frameworks, techniques, exercises and tips taught on the @askvinh channel, re-expressed in our own words with links to the specific source videos (PRD-FR-056/057, D-08). R2: Scripted role-play |
| CAP-07 | **Words — vocabulary lifecycle** | P-3 | R1 (lifecycle + spoken review); R2 (notebook, quiz) | FS-07 | Learn → Say it → Use it (given sentences) → Make your own → Review (spaced repetition); daily word goal; R2 notebook and deterministic quiz |
| CAP-08 | **Progress & streak** | P-4 | R1 (metrics, history, streak); R2 (check-ins) | FS-08 | Trends of measured metrics, attempt history with local replay, one streak definition, weekly calendar, milestones; R2 repeatable benchmark check-ins |
| CAP-09 | **Reminders & inbox** | P-1 | R1 (OS reminders); R2 (in-app inbox) | FS-09 | Daily practice reminder at the user's chosen time, review reminders, respectful frequency caps; R2 learning inbox (no software-update notifications) |
| CAP-10 | **Profile, settings, privacy & data rights** | F-1 | R1 | FS-10 | Profile, learning preferences, appearance (theme, font scale, reduced motion), speech & privacy settings, recording retention, export, clear history, delete account, about/legal |
| CAP-11 | **Offline-first storage & sync** | F-2 | R0/R1 | FS-11 | Local source of truth; idempotent learning events; background sync of progress metrics to Firestore when signed in; conflict rules; legacy-data migration |
| CAP-12 | **Speech capability management** | P-2 / F-1 | R1 | FS-12 | Detect device tier and recognition modes; offline language-pack status and download (where the platform supports it); consented online fallback; honest status display |

Platform/engineering capabilities without a user-facing feature spec (covered in ENG-01, SEC-01, ARC-01): release signing and distribution (ADR-004), CI quality gates, crash reporting (ADR-007, if approved), App Check, security rules.

## 3. Release scope

| Release | Capabilities | Not included |
|---|---|---|
| **R0 Foundation** | CAP-11 (local store, events, sync skeleton, migration design); removal flags for retired features; CI gates; signing; data-safety fixes (RD-06 P0 gaps) | New user-facing features |
| **R1 Core loop (MVP)** | CAP-01…CAP-12 at R1 level | Role-play, check-ins, notebook, quiz, inbox |
| **R2 Depth** | CAP-06 role-play; CAP-07 notebook & quiz; CAP-08 check-ins; CAP-09 inbox; content expansion | — |
| **R3 Reach (conditional)** | Store launch (ADR-004); video self-review (ADR-010); recognition improvements (ADR-009); UI localisation | Each needs a decision first |

## 4. Non-goals (explicit)

| NG | Non-goal | Reason |
|---|---|---|
| NG-01 | **Cloud AI or LLM-based evaluation, chat or generation** | Product Owner constraint (PO-REQ-1119 §"Critical architecture requirements" 3, 4, 7; PO-REQ-1120 §9.5). Any future proposal needs an ADR, privacy review and explicit authorisation |
| NG-02 | **Open-ended conversational "AI tutor" or chatbot** | Cannot be done honestly on-device today; the current template "Mentor" misrepresents itself (PP-1) |
| NG-03 | **Phoneme-level pronunciation scoring or accent rating** | Requires acoustic models not available on-device; recognition agreement must not be presented as pronunciation (SRS-BR-004) |
| NG-04 | **Exam band prediction (IELTS, TOEFL, CEFR certification)** | Requires validated scoring; would be dishonest (PP-1) |
| NG-05 | **Teaching English from zero** (grammar curriculum, alphabet, A1 vocabulary) | Different product; Duolingo-class content investment |
| NG-06 | **Social features** (leaderboards, friends, sharing recordings, communities) | Privacy positioning; manipulation risk (PP-7) |
| NG-07 | **Automated body-language / facial analysis scoring** | Models absent, heuristics weak, privacy-sensitive; video self-review without scoring is deferred to R3 |
| NG-08 | **Content scraped from third parties** (YouTube or otherwise) | Legal and fragility risk (PP-8). Hand-curated re-expression of the channel's techniques with source-video links is in scope (PRD-FR-056/057) — only automated scraping, caption download and verbatim copying are excluded |
| NG-09 | **Web, desktop or iOS clients** | Android-native constraint (PO-REQ-1119) |
| NG-10 | **In-app admin console** in R1–R2 | Content ships with the app; no server-side authorisation backend exists; insecure admin would violate PO-REQ-1119 Phase 7 rules |
| NG-11 | **Self-updating APK installer** (subject to D-03) | Incompatible with Google Play; security risk (RD-03 FND-OPS-04) |
| NG-12 | **Cloud storage of recordings / cross-device playback** | Privacy positioning; storage cost; not needed for the core loop |
| NG-13 | **Monetisation features** (paywalls, subscriptions, ads) | No evidence of business requirements; decision D-07 |
| NG-14 | **Real-time feedback while speaking** (live coaching overlays) | Distracting; recognition partials unreliable; post-attempt feedback is sufficient for R1–R2 |

## 5. Retired current features (summary — rationale per feature in RD-10)

| Current feature | Disposition | Replaced by |
|---|---|---|
| `@askvinh` tab / YouTube Intelligence (channel ingestion, per-user seeded copies) | Remove | Techniques collection in CAP-06 (SCR-PRAC-01/04) |
| Creator-derived techniques, frameworks, 30-day plan, challenges, shadowing scripts (`CurriculumCatalog.kt`) | **Keep — verify, re-express in our own words, link to the specific source video** (D-08 decided 2026-11-21; CNT-01) | CAP-06 techniques and activities; CAP-03 Today |
| AI Mentor chat | Remove | Focus tips in CAP-05; tips library in CAP-06 |
| Conversation simulator (template replies) | Remove | Scripted role-play (CAP-06, R2) |
| Speaking Lab, Challenges, Articulation, Pronunciation, Voice Training, Listening, Grammar Speaking, Public Speaking, Real-world Tasks labs | Merge | CAP-06 tracks and templates; CAP-04 session engine |
| Activity Engine (150 activities, 25 templates, 10 modules) | Merge (content reused after review) | CAP-06 |
| 15 curriculum modules, lessons, 30/60/90-day programs, 10 learning paths | Merge | CAP-06 tracks + CAP-03 Today |
| Three daily plans; four streaks; four recommenders | Merge | CAP-03; CAP-08; one plan engine |
| Feature tutorial overlay (24 features) | Remove | Contextual first-use tips (DES-06 §2.4) |
| Launch animation | Simplify | ≤ 1 s brand moment respecting reduced motion (DES-06) |
| Notification drawer (two stores) | Merge | CAP-09 reminders (R1) + inbox (R2) |
| In-app update screen and dialog | Remove (D-03) | Store updates / release notes |
| Admin console | Remove (NG-10) | Content shipped in releases |
| Cloud AI (Firebase AI Logic, Gemini Live), Whisper/Moonshine fakes | Remove | ARC-05 local pipeline |
| Video recording + MediaPipe scoring | Remove from R1 | R3 video self-review (no scoring) |

## 6. Scope change control

A capability is added, removed or moved between releases only through a PRD change approved by the Product Owner (PLN-05, gate G1 or G4), with RD-16, PRD, PLN-01, PLN-02 and GOV-01 updated in the same change.
