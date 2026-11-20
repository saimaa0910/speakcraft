# 13 — Product Vision

| Field | Value |
|---|---|
| Document ID | RD-13 |
| Version | 0.2 — Draft for review (2026-11-21: techniques curriculum and after-attempt mistakes/tips added per Product Owner direction; D-08 decided) |
| Date | 2026-11-21 |
| Status | Draft — **Recommendation**, awaiting Product Owner approval |
| Owner (role) | Chief Product Officer |
| Approval | Pending — Product Owner (decision D-01, D-02) |
| Related | RD-07 Product Audit, RD-08 Personas, RD-11 Competitor Analysis, RD-12 Opportunity Analysis, RD-14 Strategy, RD-15 Principles, RD-16 Scope, PRD, BRD |

> Everything in this document is a **RECOMMENDATION** derived from the audit evidence in RD-03…RD-12. It becomes a **DECISION** only when the Product Owner approves it (PLN-05, gate G1).

---

## 1. Why a new vision is needed (evidence summary)

| Evidence | Source |
|---|---|
| The product tries to be ~15 things at once: 23 routes, 15 curriculum modules **and** 10 activity modules (150 activities), 3 daily-practice systems, 4 streak definitions, ~10 overlapping "labs", a template chatbot labelled "AI Mentor", a YouTube scraper and an admin console. | RD-03 §4, §6; RD-10 |
| The core promise — speak and get useful feedback — does not work by default: no transcript is produced unless the user finds and enables live transcription, and then only the first utterance is captured. Feedback falls back to loudness, pauses and pitch. | RD-03 §5 (FND-AI-01…04) |
| Several scores measure something other than their label (free-speech "Pronunciation" = loudness + pitch spread; Activity scores = answer length). The app has previously shown ~78/100 for empty recordings. | RD-03 §5–6; `app/docs/COACH_EVALUATION_ROOT_CAUSE.md` |
| The product identity is borrowed from one third-party YouTube creator (a primary tab is labelled "@askvinh"; 102 references in the curriculum) without any recorded permission. | RD-03 §6.3 (FND-DOM-09) |
| The product's privacy promise ("audio, video and transcripts stay on-device") is contradicted by the code (transcripts saved to Firestore; device backup enabled; network recogniser fallback). | RD-03 §8 (FND-SEC-05…07) |
| Direction has oscillated: cloud Gemini → "zero cloud AI" → "hybrid AI coach" in three days of commits. | RD-05 history section |
| Competitors are all cloud-dependent, and users across the category distrust their scores; nobody shows confidence or declines to score bad audio. | RD-11 |

The conclusion: SpeakCraft does not need more features. It needs **one excellent, trustworthy core experience** and the discipline to say no to everything that does not serve it.

## 2. Vision statement (proposed)

> **Everyone who can speak English should be able to speak it with confidence — at work, in interviews and in everyday life.**

## 3. Mission (proposed)

> SpeakCraft gives intermediate English speakers a private, honest speaking coach in their pocket: ten minutes of real speaking practice a day, feedback grounded in evidence they can see, and progress they can measure — processed on their own phone.

## 4. Target audience

| Segment | Role in the product | Detail |
|---|---|---|
| **Primary** — Working professionals and job seekers who already speak English at an intermediate to upper-intermediate level (≈ CEFR B1–B2) and need to speak more clearly and confidently in meetings, interviews and presentations | Designed for | RD-08 persona P1 "Aarav", P2 "Meera" |
| **Secondary** — University students and early-career graduates preparing for placement interviews, group discussions and presentations | Designed for | RD-08 persona P3 "Daniel" |
| **Served, not designed for** — Advanced speakers polishing delivery; elementary learners using guided read-aloud and repeat-after-me practice | Supported by levels and templates | RD-08 P4, P5 |
| **Not targeted** — Complete beginners learning English from zero; native speakers seeking professional public-speaking training; exam candidates needing validated IELTS/TOEFL score prediction | Non-goal | RD-16 §4 |

Segment choice is **decision D-01** (see RD-14 §2 for the evidence and alternatives).

## 5. The primary problem

People with good English knowledge still freeze, ramble or sound unsure when they must **speak** — because they rarely practise speaking aloud, never hear themselves objectively, and have no safe, private way to rehearse the situations that matter to them. Existing apps either teach vocabulary and grammar (not speaking), require an internet connection and send their voice to cloud services, or give scores users do not trust.

## 6. Core value proposition

| For the user | SpeakCraft provides | Because |
|---|---|---|
| "I don't know what to practise." | **A ten-minute daily session** chosen for their goal and level | Decision fatigue was a key failure of the current 23-route product |
| "I can't tell how I sound." | **A transcript of what they said and three measured insights** (pace, pauses, filler words, answer length, vocabulary range, words recognised as intended), **the mistakes our checks found — with evidence — and coach tips**, led by one concrete focus for the next attempt (PRD-FR-047) | Users need objective, specific feedback; vague scores are distrusted (RD-11) |
| "I want proven techniques, not just practice." | **A techniques curriculum**: the communication frameworks, techniques, exercises, activities and tips taught on the @askvinh YouTube channel, explained in our own words, each with exercises to practise and a link to the original lesson (PRD-FR-056/057) | Product Owner aim (2026-11-21); D-08 decided "original wording, link out" — the app does not use the creator's name or branding |
| "I don't trust the score." | **Honest feedback**: every metric labelled *Measured* or *Estimated*; the app says "Inconclusive — try again" rather than inventing a score | The current app's mislabelled and fabricated scores destroyed trust (RD-03) |
| "I want to use better words when I speak." | **Words you can use**: each word is learned, said aloud, used in a sentence and reviewed at spaced intervals | Product Owner's explicit vocabulary-lifecycle requirement (PO-REQ-1119, Phase 8) |
| "Am I actually improving?" | **Progress from evidence**: trends of measured metrics and periodic check-ins on the same task — not lesson counts | Category "progress" usually measures activity, not ability (RD-11) |
| "I don't want my voice on someone's server." | **Private by design**: recordings are processed on the phone; nothing leaves the device without an explicit choice | Product Owner constraint; competitor gap (RD-11) |

## 7. Differentiation (proposed)

1. **Honesty as a feature.** No competitor shows measurement confidence or refuses to score bad audio. SpeakCraft makes "what we measured, how sure we are, what we could not measure" visible on every result.
2. **Private and offline-capable.** Core practice and evaluation run on-device; account sign-in is optional; recordings never leave the phone by default.
3. **Speaking for work, for non-native speakers.** Interview, meeting and presentation practice designed around the language needs of B1–B2 speakers — a combination the category splits across separate products (RD-11).
4. **Small, focused, calm.** Ten minutes a day, four tabs, no manipulative streak pressure.

What SpeakCraft deliberately does **not** compete on is listed in RD-16 §4 (phoneme-level diagnosis, open-ended AI conversation, avatars, exam-score prediction, human tutors, content volume).

## 8. Product pillars (summary — detailed in RD-14 §4)

| Pillar | Promise |
|---|---|
| **P-1 Speak every day** | A short, personal daily session that is easy to start and finish |
| **P-2 Feedback you can trust** | Evidence-based, labelled, specific feedback with one clear next step |
| **P-3 Words you can use** | Vocabulary that moves from recognition into spoken use |
| **P-4 Progress you can see** | Improvement shown from measured evidence over time |
| **Foundation F-1 Private by design** | On-device processing; explicit consent for anything else |
| **Foundation F-2 Works anywhere** | Offline-first storage; usable without network or account |

## 9. Learning philosophy

1. **Output beats input.** Speaking improves by speaking. Every session includes real speaking attempts, not just reading or tapping.
2. **One focus at a time.** Each result shows the mistakes our checks actually found (a short, evidenced list — at most three shown first) and coach tips, led by one strength and one thing to change next — never a wall of twelve problems or invented ones.
3. **Rehearse the real situation.** Practise the interview answer, the meeting update, the self-introduction — then transfer.
4. **Repetition with variation.** Retry the same prompt to feel improvement; revisit skills with new prompts to consolidate; review words at spaced intervals.
5. **Listen to yourself.** Seeing your transcript and hearing your recording is itself feedback.
6. **Measure honestly, celebrate effort fairly.** Consistency is rewarded (streaks for genuine practice), but ability claims are made only from evidence.

## 10. Long-term direction (beyond the first releases — indicative, not committed)

| Horizon | Direction | Pre-condition |
|---|---|---|
| R1 (MVP) | The core loop: Today session → speak → honest feedback → retry; Practice library (5 tracks); Words; Progress; privacy and data rights | Approval of Stage A |
| R2 | Scripted role-play conversations; progress check-ins; vocabulary notebook and quiz; in-app inbox | R1 success metrics met (BRD §8) |
| R3 (conditional) | Video self-review (record and watch, no automated body-language scoring); validated on-device recognition improvements; optional content packs; store launch | ADR-004, ADR-009, ADR-010 decisions |
| Later (explore) | Optional, consented, privacy-reviewed enhancements (e.g. on-device language models on capable devices) | Separate ADR, privacy review, Product Owner authorisation |

## 11. What success looks like (12 months, proposed)

- A new user completes a first valid speaking attempt within **3 minutes** of first opening the app.
- Users practise on **≥ 3 days a week** on average in their first month.
- **≥ 75 %** of users who rate their feedback say it was accurate (in-app "Was this accurate?" prompt).
- Users can show a measurable change in at least one metric (e.g. filler words per minute, pace stability, answer length within target) between their first and fourth check-in.
- Zero privacy incidents; data deletion and export work end-to-end.

Measures and targets are defined authoritatively in BRD §8 (`BO-nn`) and are **Proposed — requires approval**.
