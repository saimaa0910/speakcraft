# 14 — Product Strategy

| Field | Value |
|---|---|
| Document ID | RD-14 |
| Version | 0.2 — Draft for review (2026-11-21: Product Owner direction on creator-derived content applied — D-08 decided) |
| Date | 2026-11-21 |
| Status | Draft — **Recommendation**, awaiting Product Owner approval |
| Owner (role) | Chief Product Officer |
| Approval | Pending — Product Owner (decisions D-01…D-06) |
| Related | RD-13 Vision, RD-15 Principles, RD-16 Scope, RD-10 Feature Value Assessment, RD-12 Opportunity Analysis, PRD, BRD, PLN-01 Roadmap |

> **Source identifiers used in this document.** `PO-REQ-1119` = the Product Owner's 19-Nov-2026 product request (`public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484`, inventoried in RD-01). `PO-REQ-1120` = the Product Owner's 20-Nov-2026 Stage A brief (this redevelopment). Both are the authoritative statements of Product Owner intent available in the repository.

---

## 1. Strategic choices at a glance

| # | Choice | Recommendation | Decision ID |
|---|---|---|---|
| 1 | Who we design for | Intermediate–upper-intermediate (B1–B2) professionals and job seekers; students second | D-01 |
| 2 | What the core experience is | A ten-minute daily speaking session with honest, evidence-based feedback | — (follows from D-01) |
| 3 | How we evaluate speech | On-device, measured-first; no composite "overall score" for free speaking; *Inconclusive* instead of guessed scores | D-02, D-06 |
| 4 | Account model | Account optional; sign in to back up and sync | D-05 |
| 5 | Cloud AI | Not part of the product; remove Firebase AI Logic (ADR-003) | D-04 |
| 6 | Distribution | Google Play testing tracks replace the in-app APK updater (ADR-004) | D-03 |
| 7 | Scope discipline | 12 capabilities; everything else removed or deferred (RD-16) | — |

## 2. Primary and secondary user segments

### 2.1 Recommendation

| Segment | Priority | Evidence | Assumption to validate |
|---|---|---|---|
| **S1 Working professionals and job seekers (B1–B2)** — need interview, meeting, presentation and workplace conversation confidence | **Primary** | Existing content is dominated by workplace, interview, presentation and storytelling modules (RD-03 §6.2); PO-REQ-1119 goal list ("speak confidently in interviews", "workplace communication", "presentation skills"); recognition locales include `en-IN`, `en-GB`, `en-AU` (RD-03 §5); category gap for non-native professional communication (RD-11-I4) | Target users will practise ~10 min/day on a phone and value privacy (ASM-01, ASM-02 in RAID) |
| **S2 University students and early-career graduates** — placement interviews, group discussions, presentations | **Secondary** | Same content fit; price-sensitive markets underserved (RD-11) | Students accept the same core loop with different scenario packs |
| S3 Advanced speakers polishing delivery | Served | Delivery metrics (pace, pauses, fillers) apply at any level | — |
| S4 Elementary learners | Served with guided templates only | PO-REQ-1119 requires Beginner→Advanced levels | Free-speaking feedback is weak at A1–A2 because speech recognition and sentence production are both unreliable; guided read-aloud and repeat-after-me work better |

### 2.2 Alternatives considered

| Alternative | Why not recommended |
|---|---|
| "Everyone who wants to improve English" (current implicit target) | Produces the current sprawl; no persona is served well (RD-07 §2) |
| Beginners as primary | Requires a full grammar/vocabulary curriculum and robust low-level ASR; competes head-on with Duolingo; free-speaking feedback is least reliable at this level |
| Native-speaker public speaking | Different needs (rhetoric, stage presence, video); crowded by Orai/Yoodli-type products; current video analysis is not functional |
| Exam preparation (IELTS/TOEFL speaking) | Requires validated band prediction SpeakCraft cannot provide honestly on-device (RD-11 §6, "should not compete on") |

## 3. Core, supporting and deferred capabilities

The canonical capability list (CAP-nn) is defined authoritatively in RD-16 §2. Strategic classification:

| Class | Capabilities | Rationale |
|---|---|---|
| **Core** — the product fails without these | CAP-03 Today session · CAP-04 Speaking practice session · CAP-05 Honest feedback · CAP-12 Speech capability management | They are the core loop and its technical precondition |
| **Supporting** — make the core valuable and sustainable | CAP-02 Onboarding · CAP-06 Practice library · CAP-07 Words · CAP-08 Progress & streak · CAP-09 Reminders · CAP-10 Profile, settings, privacy & data rights · CAP-11 Offline-first storage & sync · CAP-01 Account | Personalisation, content, motivation, trust, compliance |
| **Deferred** (R2/R3, conditional) | Scripted role-play (CAP-06 extension, R2) · Progress check-ins (CAP-08 extension, R2) · Vocabulary notebook & quiz (CAP-07 extension, R2) · In-app inbox (CAP-09 extension, R2) · Video self-review (R3) · Store launch (R3, ADR-004) | Valuable but not needed to prove the core loop |
| **Removed** | YouTube Intelligence (channel ingestion/scraping) · @askvinh branding (the channel's techniques themselves are **kept** and re-expressed in our own words — D-08 decided, see note below) · "AI Mentor" chat · free conversation simulator · cloud AI · fake Whisper/Moonshine engines · automated body-language scoring · in-app admin console · in-app APK updater (subject to D-03) · 24-feature tutorial overlay · duplicate labs, plans, streaks, paths, programs | RD-10 per-feature rationale |

> **Content basis and feedback (Product Owner direction, 2026-11-21 — DECISION).** The product's curriculum is built from the communication frameworks, techniques, tasks, exercises, activities and tips taught on the @askvinh YouTube channel (PRD-FR-056). Under **D-08 ("original wording, link out")** every item is re-expressed in our own words and examples, links to its specific source video, and the app never uses the creator's name, likeness or branding (PRD-FR-057, SRS-BR-028). The starting point is the material already in the current app, verified item by item (CNT-01). After every submitted attempt the user sees the **mistakes our checks detected and coach tips** linked to techniques, produced by deterministic logic and on-device analysis (PRD-FR-047). This supersedes the earlier recommendation in this document and in RD-10/RD-12 to remove creator-derived content; their analysis of the legal risk remains valid and is what the D-08 guardrails address.

## 4. Product pillars

Validated against the audit (the candidate list in PO-REQ-1120 §6.3 was: personalised learning, speaking practice, communication intelligence, vocabulary development, adaptive evaluation, learning progress, daily consistency).

| Pillar | Merges candidate pillars | Promise to the user | Measured by (BRD §8) | Capabilities |
|---|---|---|---|---|
| **P-1 Speak every day** | Speaking practice + daily consistency + personalised learning | "I always know what to practise, and it takes ten minutes." | Activation, weekly practice days, session completion | CAP-02, CAP-03, CAP-06, CAP-09 |
| **P-2 Feedback you can trust** | Communication intelligence + adaptive evaluation | "I can see exactly what I said and what to improve, and the app is honest about what it can't tell." | Feedback accuracy rating, inconclusive rate, retry rate | CAP-04, CAP-05, CAP-12 |
| **P-3 Words you can use** | Vocabulary development | "New words become words I actually say." | Words reaching *Used* stage, review adherence | CAP-07 |
| **P-4 Progress you can see** | Learning progress | "I can see that I'm improving, based on evidence." | Users with measurable metric change between check-ins | CAP-08 |
| **F-1 Private by design** (foundation) | — (Product Owner constraint) | "My voice stays on my phone unless I choose otherwise." | Zero privacy incidents; data-rights flows verified | CAP-10, CAP-11, CAP-12 |
| **F-2 Works anywhere** (foundation) | — (Product Owner constraint) | "It works on the train, without signal, without an account." | Offline task success | CAP-01, CAP-11 |

Why "communication intelligence" and "adaptive evaluation" are not separate pillars: to the user they are one promise (trustworthy feedback). Splitting them would recreate the current problem of several evaluation engines with different philosophies (RD-03 FND-DOM-04).

## 5. Business model position

There is **no evidence** in the repository of a monetisation model, pricing, target revenue or commercial stakeholders (RD-00 §6). This strategy therefore:

- sets **no revenue requirements**;
- treats cost-to-serve as a design constraint (on-device processing keeps marginal cost near zero, which preserves future options such as a low-price or free tier in price-sensitive markets — an *option*, not a decision);
- records monetisation as **open decision D-07** (out of Stage A scope).

## 6. Go-to-market and release strategy (summary)

| Release | Theme | Exit criterion (summary) |
|---|---|---|
| **R0 Foundation** | Make the codebase safe to build on: CI quality gates, release signing, data-safety fixes, dependency clean-up, package structure, local database (if ADR-002 approved), feature flags to retire removed features | RD-06 §5 R0 exit criterion met; CI runs unit tests + lint + rules tests on every PR |
| **R1 Core loop (MVP)** | Onboarding → Today → speak → honest feedback → retry; Practice library (5 tracks, 5 templates); Words (lifecycle + review); Progress & streak; reminders; settings, privacy, export, deletion | BRD §8 R1 targets met in closed beta |
| **R2 Depth** | Role-play, check-ins, notebook & quiz, inbox, more content | R2 targets |
| **R3 Reach (conditional)** | Store launch, video self-review, recognition improvements | ADR decisions |

Detailed roadmap: PLN-01. Distribution path depends on **D-03 / ADR-004**.

## 7. Non-goals (summary — authoritative list in RD-16 §4)

No cloud AI; no open-ended chatbot; no phoneme-level pronunciation claims; no exam-score prediction; no social features; no teaching English from zero; no web/desktop clients; no in-app admin console in R1–R2; no monetisation work in Stage B scope until D-07.

## 8. Product risks (top 8 — full list in PLN-04 RAID)

| ID | Risk | Impact | Mitigation |
|---|---|---|---|
| RSK-01 | On-device speech recognition is unavailable or poor on many target devices (minSdk 24; on-device recogniser needs API 31+) | Core loop degraded | Device-tier strategy (ARC-05 §4); explicit consented online fallback; device spike SPK-01 before R1 build |
| RSK-02 | Recognition "auto-corrects" mispronunciations, so word-level checks overstate accuracy | Users mistrust or are misled | Never label recognition match as pronunciation; show *Recognised as intended* with caveat (SRS-BR-004) |
| RSK-03 | Heuristic grammar rules produce false positives | Trust damage | Ship only rules that pass a precision threshold on a labelled corpus (NFR-QUAL-003) |
| RSK-04 | Content volume too thin (only 20 vocabulary words exist today; @askvinh-derived items need verification and rewriting before they can ship — CNT-01) | Empty-feeling product | Content workstream EP-09; verify and re-express existing items first, then extend from further videos |
| RSK-05 | IP exposure from creator-derived content and naming | Legal | D-08 decided: original wording and examples, no verbatim transcripts, no creator name/likeness/branding, specific-video links (SRS-BR-028); remove branding in R0; build-time and release checks (TST-01). Residual risk: a specific framework could still be considered distinctive — an optional legal opinion remains advisable before a public store launch |
| RSK-06 | Single-developer capacity vs scope | Delays | Strict scope (RD-16), incremental releases, preliminary estimates flagged |
| RSK-07 | Without analytics, success metrics cannot be measured | Blind decisions | ADR-013 opt-in minimal analytics or beta survey programme (D-09) |
| RSK-08 | Migration of existing users' data | Data loss | Minimal, tested migration (ARC-03 §9); legacy data retained read-only |

## 9. Key assumptions (validated in R1 beta — tracked in PLN-04)

| ID | Assumption | Validation method |
|---|---|---|
| ASM-01 | B1–B2 professionals are the segment with the strongest need and fit | Beta recruitment screener + interviews (5–8 users) |
| ASM-02 | Privacy/offline is a meaningful reason to choose SpeakCraft for this segment | Beta interview question; onboarding consent-choice data |
| ASM-03 | Ten minutes a day is the right default session length | Session length distribution in beta |
| ASM-04 | Measured insights without a composite score feel useful, not "less complete" | Feedback-accuracy rating; A/B not feasible — qualitative interviews |
| ASM-05 | Android's platform recogniser is accurate enough for B1–B2 accented English on mid-range devices | SPK-01 device spike with recorded test set |
| ASM-06 | Users will accept optional sign-in and understand local-only data | Onboarding completion and later sign-in rate |

## 10. Decisions requested from the Product Owner (this document)

| ID | Decision | Recommendation |
|---|---|---|
| D-01 | Primary and secondary segment | S1 primary, S2 secondary (§2) |
| D-02 | Evaluation philosophy | Measured-first, labelled, *Inconclusive* over guessing (ARC-05) |
| D-03 | Distribution channel / in-app updater | Play testing tracks; remove updater (ADR-004) |
| D-04 | Cloud AI | Remove from product (ADR-003) |
| D-05 | Account model | Account optional (FS-01) |
| D-06 | Composite "overall score" for free speaking | Do not show one in R1; show measured insights + focus (FS-05) |
| D-07 | Monetisation | Out of scope until after R1 |
| D-08 | Creator-derived content | **DECIDED 2026-11-21 by the Product Owner: "original wording, link out."** The curriculum is built from the @askvinh channel's frameworks, techniques, exercises, activities and tips (PRD-FR-056), re-expressed in our own words with links to the specific source videos and no creator branding (PRD-FR-057, SRS-BR-028) |
| D-09 | Success measurement method | Opt-in minimal analytics (ADR-013) or structured beta survey |

The consolidated decision list, with all other open decisions, is RD-99 §8.
