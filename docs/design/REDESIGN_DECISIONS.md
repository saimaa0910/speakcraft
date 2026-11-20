# Redesign Decisions — SpeakCraft (Decision Log)

| Field | Value |
|---|---|
| Document ID | DES-08 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UX Strategist |
| Approval | Pending — Product Owner |
| Related | RD-13 Product Vision, RD-14 Product Strategy, RD-15 Product Principles, RD-16 Scope and Non-Goals, DES-01 UX Audit, DES-02 Information Architecture, DES-03 Screen Inventory, DES-04 User Flows, DES-05 Design System, DES-06 UI/UX Specifications, DES-07 Accessibility Guidelines; PRD, SRS, ARC-05 |

> **Status of everything in this log.** Each entry (`RDD-nn`) is a **RECOMMENDATION** (GOV-02 DC-3) with approval status **Pending — Product Owner**. Where an entry depends on a Product Owner decision (`D-nn`, RD-99 §8), the link is stated with that decision's current status. A decided `D-nn` does not by itself approve the design that implements it: the RDD stays Pending until the redesign is approved (PLN-05 gate G2).
>
> **What this log owns.** The *why* of the redesign: the problem, the options considered, the recommended design direction and its consequences for requirements. Screen-level detail is in DES-06; navigation and screen IDs in DES-02; tokens and components in DES-05; requirements in the PRD and SRS. This log references those documents and does not restate them (GOV-02 DC-2).

---

## 1. How to read an entry

| Field | Meaning |
|---|---|
| Context / problem | What is wrong today, with evidence: `UXF-nn` (DES-01), `FND-*` (RD-03), ui_audit sections, or a Product Owner direction |
| Options considered | The realistic alternatives, including the status quo where relevant |
| Decision (RECOMMENDATION) | The recommended direction |
| Rationale | Why this option beats the others |
| Principles served | RD-15 `PP-n` (product) and `UX-n` (experience). Lower-numbered PP principles outrank higher ones (RD-15 §1) |
| Impact on requirements | `PRD-FR` / `SRS-FR` / `SRS-BR` / `NFR` IDs that implement or are changed by the decision |
| Linked decision | `D-nn` it depends on, with its status |
| Specified in / verified by | DES-06 sections and `AC-DES-nn` acceptance criteria |
| Approval status | Pending — Product Owner (all entries) |

## 2. Decision summary

| ID | Decision (short) | Linked D-nn (status) | Principles |
|---|---|---|---|
| RDD-01 | Four destinations — Today, Practice, Words, Progress — identical at every width | D-20 (Pending) | UX-8, UX-1, PP-3, PP-5 |
| RDD-02 | Replace the creator-named tab (a YouTube scraping tool) with technique content inside Practice (Techniques collection, SCR-PRAC-04) | **D-08 DECIDED 2026-11-21 (Option B)** | PP-8, PP-1, PP-5, UX-1, UX-8 |
| RDD-03 | Profile and Settings behind the avatar on every primary destination, not a tab | — | UX-8, PP-3, PP-2 |
| RDD-04 | One session player for every spoken task | — | PP-5, UX-1, UX-6, PP-3 |
| RDD-05 | Single-tap Record / Stop with automatic checking (no "Analyze" step) | — | UX-2, UX-6, PP-4, UX-9 |
| RDD-06 | One result structure, in a fixed order, with "Try again" as its single primary action | D-06 (Pending) | PP-3, UX-3, UX-6, PP-1 |
| RDD-07 | Mistakes found and coach tips after every attempt | D-04 (Pending), D-08 (DECIDED) | PP-1, UX-3, PP-3, UX-6, PP-7, UX-5 |
| RDD-08 | No composite overall score for free speaking | D-06 (Pending) | PP-1, PP-7, UX-3 |
| RDD-09 | Every metric labelled Measured or Estimated, in text | D-02 (Pending) | PP-1, UX-3, UX-9 |
| RDD-10 | Inconclusive state instead of guessed scores | D-02 (Pending) | PP-1, PP-7, UX-3 |
| RDD-11 | Today shows one session with one primary action | — | PP-3, UX-6, UX-2, PP-4, UX-10 |
| RDD-12 | Account-optional onboarding | D-05 (Pending) | PP-6, PP-2, UX-2 |
| RDD-13 | First speaking attempt inside onboarding | — | PP-4, UX-2, UX-5, PP-1 |
| RDD-14 | Contextual first-use tips instead of tours | — | UX-6, UX-10, UX-1, UX-9 |
| RDD-15 | Brand launch moment ≤ 1 s, using the platform splash | — | UX-2, UX-10, UX-9 |
| RDD-16 | Remove the "AI Mentor" chat and the conversation simulator | D-04 (Pending) | PP-1, PP-5 |
| RDD-17 | Tracks and techniques replace modules, paths and programs | D-08 (DECIDED) | PP-5, UX-1, PP-3, PP-8 |
| RDD-18 | Words as a primary destination in R1 | D-20 (Pending) | PP-4, UX-8, UX-4 |
| RDD-19 | Evidence-only progress and a single streak | — | PP-7, PP-1, UX-4, PP-5 |
| RDD-20 | Calm Indigo palette | D-18 (Pending) | UX-10, UX-9 |
| RDD-21 | No dynamic colour | D-18 (Pending) | PP-1, UX-9, UX-10 |
| RDD-22 | No update notifications and no in-app updater UI | D-03 (Pending) | PP-5, UX-6, UX-10 |
| RDD-23 | Settings reorganised into seven groups; developer diagnostics removed from the main settings list | — | UX-1, UX-6, PP-2, PP-1 |
| RDD-24 | Microphone permission explained in context, with recoverable denial | — | UX-7, PP-2, PP-6, UX-9 |

---

## 3. Decision log

### RDD-01 Four destinations: Today, Practice, Words, Progress

| Field | Content |
|---|---|
| Context / problem | The app registers 26 routes. Phones show 5 tabs (Home, Daily, Record, "@askvinh", Mentor); widths ≥ 840 dp show a 10-item sidebar with different labels ("Tools" = Vocabulary); the 600–839 dp rail cannot reach five destinations; most features are reached through a 22–23-tile directory (UXF-06; FND-UI-01; ui_audit §1.2–§1.5). |
| Options considered | (a) Keep 5 tabs, relabelled. (b) **Four purpose-led destinations**, each answering one user question. (c) Three tabs (Today, Practice, Progress) with Words inside Practice. (d) A navigation drawer with all features. |
| Decision (RECOMMENDATION) | (b): Today ("What should I practise now?"), Practice ("I want to rehearse a situation"), Words ("Which words am I learning?"), Progress ("Am I improving?"), in the same order, with the same labels and icons on bar and rail at every width. No directory, no drawer. |
| Rationale | Each destination maps to a pillar (P-1…P-4) and a user question, so features have one obvious home. Four single-word labels fit at 200 % text (DES-05 FS-7). A drawer would hide features again; (c) buries the vocabulary lifecycle the Product Owner asked for (see RDD-18). |
| Principles served | UX-8 consistent navigation; UX-1 simple to understand; PP-3 one clear next step; PP-5 fewer, better features |
| Impact on requirements | PRD §2 (primary navigation); DES-02 §2.1 (authoritative); SRS §8 UI interface; NFR-PERF-003 (navigation speed). No new requirement. |
| Linked decision | D-20 "R1 includes the Words tab" — Pending (see RDD-18) |
| Specified in / verified by | DES-06 §2.1; AC-DES-01, AC-DES-02 |
| Approval status | Pending — Product Owner |

### RDD-02 Replace the creator-named tab with technique content inside Practice

| Field | Content |
|---|---|
| Context / problem | One of five primary tabs is labelled "@askvinh". It opens "YouTube Channel Intelligence": the user pastes a channel URL, the app scrapes captions, takes the first five sentences of each video as "techniques", invents `idx*45s` timestamps, labels the results "verified", and sends users out to YouTube with no way to practise (UXF-07, UXF-59; FND-UI-03; ui_audit §1.8; LIO:847-907, YT:321). Creator branding appears on 149 source lines and framework names appear unexplained (UXF-55). **Product Owner direction (2026-11-21):** the curriculum is to be built from the frameworks, techniques, tasks, exercises, activities and tips taught on the @askvinh channel (PRD-FR-056). |
| Options considered | All options **remove the scraping tool** (YouTube Terms of Service; ADR-011; RD-16 NG-08). (a) **Licensed and co-branded**: techniques presented with the creator's branding and attribution as a licence allows. (b) **Unbranded, own words, link out**: each technique re-expressed in original wording and examples, with a "Watch the original lesson" link to its specific source video, and no creator name, likeness or branding. (c) **Generic curriculum**: original techniques with no relationship to the channel. |
| Decision (RECOMMENDATION) | Replace the creator-named tab with **technique content inside Practice**: a **Techniques collection on SCR-PRAC-01** and **SCR-PRAC-04 Technique detail** (name, one-line idea, why it works, steps, linked exercises with a single "Practise it" action, tips, "Watch the original lesson"), reachable as Practice → Technique → Activity → Session player (DES-02 v0.2). Keep the techniques as the **core curriculum**; techniques are practised in the one session player with honest metrics and are linked from every result's coach tips (RDD-07). Branding follows D-08, which the Product Owner has now **decided as Option B**: own words, no verbatim transcripts, a link to the specific source video (with timestamp), and no creator name, likeness or branding in the UI, content, notifications or store listing. |
| Rationale | It keeps the Product Owner's curriculum aim while removing a non-learning, legally risky tool from a primary slot. Practice is where users go to rehearse, so techniques sit next to tracks rather than in a fifth tab, keeping RDD-01 intact. Provenance becomes honest: a specific video and timestamp instead of a channel home page, and no "verified" labels on heuristic extracts. Option B reduces IP exposure (RSK-05) without a licence negotiation, and gives users who want more a path to the source. |
| Principles served | PP-8 own our content (original re-expression with provenance); PP-1 honest before impressive (no fabricated "verified" content); PP-5 fewer, better features; UX-1 plain techniques instead of unexplained framework names; UX-8 four destinations kept |
| Impact on requirements | PRD-FR-056 (technique curriculum), PRD-FR-057 (D-08 rules), PRD-FR-120 (removal of the scraper); SRS-FR-PRAC-008 (Techniques collection and detail), SRS-FR-PRAC-009 (provenance, outbound link only), SRS-BR-028 (licence status, no creator branding, build-time validation), SRS-FR-PLAT-001 (no YouTube client). DES-02 v0.2 adds SCR-PRAC-04. RD-16 §5, RD-14 §3 and DES-01 UXF-07/UXF-55 responses still describe creator-derived content as removed and need alignment (issue I-1). |
| Linked decision | **D-08 — DECIDED 2026-11-21 (Option B: original wording, link out).** This RDD remains a RECOMMENDATION pending design approval. |
| Specified in / verified by | DES-06 §1.1, §3.4.1, §3.4.2 (SCR-PRAC-04), §3.3.5 (coach-tip links); AC-DES-58, AC-DES-63, AC-DES-64, AC-DES-65, AC-DES-08 |
| Approval status | Pending — Product Owner (design) |

### RDD-03 Profile and Settings behind the avatar, not a tab

| Field | Content |
|---|---|
| Context / problem | Settings has 9 entry points but none on the phone's primary chrome; the dashboard avatar callback is dead; profile is reachable only through the top-bar avatar and one alert (UXF-09; ui_audit §1.5). |
| Options considered | (a) Settings as a fifth tab. (b) **Avatar on the top bar of every primary destination → Profile → Settings.** (c) An overflow menu on each screen. |
| Decision (RECOMMENDATION) | (b). The avatar (labelled "Profile and settings") is always one tap away; Settings is one level below Profile. |
| Rationale | Settings are visited rarely and do not merit a primary slot; the avatar is a widely understood pattern and keeps privacy controls two taps from anywhere. An overflow menu hides the entry. |
| Principles served | UX-8; PP-3 (tabs stay task-focused); PP-2 (privacy controls are easy to reach) |
| Impact on requirements | PRD-FR-090, PRD-FR-091; SRS-FR-SET-001, SRS-FR-SET-002; DES-02 §2.2 |
| Linked decision | — |
| Specified in / verified by | DES-06 §2.1 (SH-1), §3.7.1; AC-DES-84 |
| Approval status | Pending — Product Owner |

### RDD-04 One session player for every spoken task

| Field | Content |
|---|---|
| Context / problem | Ten screens record speech (twelve with dictation) through the same pipeline but with different controls, labels and behaviours; more than 30 navigation paths lead to a recorder; a recording continues across screens and one lab can evaluate another lab's recording; work is lost on rotation (UXF-12, UXF-37, UXF-38; FND-UI-04, FND-UI-06; ui_audit §7.2–§7.3). |
| Options considered | (a) Keep the labs and share one recorder component. (b) **One full-screen session player** with sequential states, used from Today, Practice, techniques, Words and onboarding. (c) An inline recorder embedded in each content screen. |
| Decision (RECOMMENDATION) | (b): one player with the states Prompt → Prepare → Ready → Recording → Processing → Result, plus Inconclusive, Interrupted and Error; only the top-bar title, step progress and result actions vary by context. Labs become tracks, techniques and templates. |
| Rationale | One state machine per attempt fixes the leaks and wrong-recording bugs by construction, users learn one interaction once, and one implementation can be tested thoroughly (EP-3). Option (a) keeps ten screens to learn; (c) keeps state in many places. |
| Principles served | PP-5; UX-1; UX-6; PP-3 |
| Impact on requirements | PRD-FR-030…036; SRS-FR-SESS-001…014; PRD-FR-120 (retired labs) |
| Linked decision | — |
| Specified in / verified by | DES-06 §3.3; AC-DES-34…AC-DES-57 |
| Approval status | Pending — Product Owner |

### RDD-05 Single-tap Record / Stop with automatic checking

| Field | Content |
|---|---|
| Context / problem | Recording and analysis are separate steps with eight different "analyse" labels; "Analyze" can be pressed before anything is recorded; submit buttons stay enabled while loading (UXF-04, UXF-16, UXF-39). |
| Options considered | (a) Press-and-hold to record. (b) Tap to start, tap to stop, then a separate "Analyze" button. (c) **Tap to start, tap to stop, and checking starts automatically.** |
| Decision (RECOMMENDATION) | (c), with fixed labels "Record" and "Stop", a 500 ms debounce, auto-stop at the template maximum with a 10-second warning, and a processing state that never spins indefinitely. |
| Rationale | Removes a tap from every attempt and makes the wrong order impossible. Press-and-hold is hard for motor-impaired users and for long answers. |
| Principles served | UX-2 easy to start; UX-6 minimal cognitive load; PP-4 speaking first; UX-9 accessible |
| Impact on requirements | PRD-FR-030, PRD-FR-031; SRS-FR-SESS-003, -004, -006; SRS-FR-EVAL-013 |
| Linked decision | — |
| Specified in / verified by | DES-06 §3.3.3, §3.3.4; AC-DES-34, AC-DES-37, AC-DES-40 |
| Approval status | Pending — Product Owner |

### RDD-06 One result structure, in a fixed order, with "Try again" as its single primary action

| Field | Content |
|---|---|
| Context / problem | Two feedback cards use different structures and different composite formulas; results list many items with no single focus; there is no transcript by default (UXF-31, UXF-34; CC:1326-1842 vs ETR:645-787). |
| Options considered | (a) Keep per-template result cards. (b) **One result structure for every template**: what we heard, up to three labelled headline metrics, one strength, mistakes found, coach tips, Try again, "Was this accurate?". (c) A full metrics dashboard after each attempt. |
| Decision (RECOMMENDATION) | (b), in the Product Owner's order (2026-11-21): **what we heard → headline metrics (≤ 3, Measured/Estimated) → what went well → mistakes found → coach tips → Try again → "Was this accurate?"**, with "Try again" as the single filled action (the moving-on action is primary only after a passed word stage or a recalled review item). Other metrics are one tap away ("All measurements", "How we measured this"). |
| Rationale | One predictable structure lets users read any result in seconds; the transcript first answers "what did I say?"; three metrics avoid overload; "Try again" puts repetition — the core learning mechanism (RD-13 §9) — one tap away. A dashboard per attempt recreates the overload problem. |
| Principles served | PP-3 one clear next step; UX-3 feedback users can trust; UX-6 progressive disclosure; PP-1 |
| Impact on requirements | PRD-FR-040, -041, -044, -045, -047; SRS-FR-EVAL-004, -005, -009, -010, -015, -016. Supersedes the result order in DES-05 §11.15 and FS-04's "Continue" primary inside Today and FS-02 §9's "Go to Today" primary (DES-06 §5 issue 20). |
| Linked decision | D-06 (no composite score) — Pending |
| Specified in / verified by | DES-06 §3.3.5–§3.3.8; AC-DES-41, AC-DES-45, AC-DES-49, AC-DES-50 |
| Approval status | Pending — Product Owner |

### RDD-07 Mistakes found and coach tips after every attempt

| Field | Content |
|---|---|
| Context / problem | **Product Owner direction (2026-11-21; PRD-FR-047):** after every submitted attempt users must see which mistakes were detected and how to improve. Today feedback ignores the task (lab instructions are dropped, so "Compare AI" never compares, UXF-33), "coaching" comes from template replies labelled "AI Mentor" (UXF-54), and results are not connected to any learning content. |
| Options considered | (a) One focus only (the earlier design). (b) **Mistakes found** — each issue with evidence and its Measured/Estimated label, three shown first, "Show all", an honest empty state — **plus Coach tips** — the focus tip and up to two more, chosen deterministically from technique tips and linked to the technique and its source video. (c) Every detected issue with severity colours and a mistake score. (d) Generated coaching text from a language model. |
| Decision (RECOMMENDATION) | (b), shown after every speaking activity, technique exercise, word stage and review item, with the content rules of ARC-05 §8.4: an issue is raised only when its metric is outside the target band by more than the noise band; the empty state reads "Our checks didn't find issues in this attempt" with a note on what the checks cover and never claims perfection; aspects audio cannot show become a self-check stored as self-reported, never Measured or Estimated. Presentation is calm: neutral colours, no error styling, no mistake count as a score, "Try again" the only primary action. |
| Rationale | Specific, evidence-backed issues tell users exactly what to change, and tips linked to techniques turn feedback into learning. Deterministic selection is testable and reproducible (EP-3). Option (c) grades users by colour and invites anxiety (PP-7); option (d) needs cloud AI (NG-01, D-04) or unvalidated on-device generation, and cannot guarantee honest claims. Limiting to three issues first respects one-clear-next-step without hiding anything. |
| Principles served | **PP-1** honest before impressive (evidence and labels on every issue; honest empty state); **UX-3** feedback users can trust; PP-3 and UX-6 (three first, one primary); PP-7 (no shaming); UX-5 personal (tips match the attempt and technique) |
| Impact on requirements | PRD-FR-047; SRS-FR-EVAL-015, SRS-FR-EVAL-016, SRS-FR-WORD-014; ARC-05 §8.4 (issue catalogue, tip selection, self-check); content pack tips with triggers (ARC-03 §6); new DES-05 patterns "Mistake list" and "Coach tip card" (DES-06 §5 issue 19); FS-04, FS-05, FS-07 workflows |
| Linked decision | D-04 cloud AI removal — Pending (tips and mistakes use no cloud model); D-08 — DECIDED (tips in our own words, linked to specific source videos) |
| Specified in / verified by | DES-06 §3.3.5–§3.3.8, §3.5 stage and review results, §3.6 attempt detail; AC-DES-41, AC-DES-42, AC-DES-47, AC-DES-71, AC-DES-73, AC-DES-81 |
| Approval status | Pending — Product Owner |

### RDD-08 No composite overall score for free speaking

| Field | Content |
|---|---|
| Context / problem | Free-speech "Pronunciation" is computed from loudness, pitch and voicing; activity scores come from answer length; a fallback overall score of 62 is shown to new users; the app has shown ~78/100 for empty recordings (UXF-32, UXF-56; FND-AI-08, FND-DOM-04; RD-13 §1). |
| Options considered | (a) Keep an overall score with a new formula. (b) **No overall score for Answer a question, Structured answer and Tell a story**; labelled metrics, mistakes and tips instead; threshold scores only where a reference exists (Read aloud, Listen & repeat, word stages). (c) A confidence-weighted score with caveats. |
| Decision (RECOMMENDATION) | (b). No overall, "x/100", ring, grade or colour judgement for free speaking. |
| Rationale | No on-device method can validly combine pace, fillers, structure and vocabulary into one ability number; any such number would invite trust it does not deserve. Thresholds are honest where a reference text exists. Risk: users may feel feedback is "less complete" (ASM-04), validated in beta through the accuracy rating. |
| Principles served | PP-1; PP-7; UX-3 |
| Impact on requirements | PRD-FR-043; SRS-FR-EVAL-007, SRS-FR-EVAL-008; SRS-BR-006, SRS-BR-007 |
| Linked decision | D-06 — Pending |
| Specified in / verified by | DES-06 §3.3.5; AC-DES-43, AC-DES-44 |
| Approval status | Pending — Product Owner |

### RDD-09 Every metric labelled Measured or Estimated, in text

| Field | Content |
|---|---|
| Context / problem | Scores measure something other than their label; recognition confidence is a constant (0.85, 0.94) passed off as real; capability states show "ready" without checks (UXF-32, UXF-59, UXF-60). |
| Options considered | (a) No labels. (b) Confidence percentages on each metric. (c) **Two plain labels, "Measured" and "Estimated", always in text**, with "How we measured this" one tap away. |
| Decision (RECOMMENDATION) | (c), using the DES-05 Evidence badge; recognition confidence shown only if the recogniser supplies it. |
| Rationale | Two labels are understandable at B1 and honest about the difference between counting and inferring; percentages imply precision we do not have. Text labels work for colour-blind and TalkBack users. |
| Principles served | PP-1; UX-3; UX-9 (colour is never the only carrier) |
| Impact on requirements | PRD-FR-040, PRD-FR-045; SRS-FR-EVAL-003, -010, -012; SRS-BR-003, SRS-BR-005 |
| Linked decision | D-02 evaluation philosophy — Pending |
| Specified in / verified by | DES-06 §3.3.5, §3.3.12 (SCR-SESS-02); AC-DES-41, AC-DES-57 |
| Approval status | Pending — Product Owner |

### RDD-10 Inconclusive state instead of guessed scores

| Field | Content |
|---|---|
| Context / problem | Insufficient evidence is shown as "Status: INSUFFICIENT_EVIDENCE • No score awarded" with no guidance (UXF-35). The fail-closed evidence gate itself is a strength worth keeping (DES-01 K-6; FND-AI-17). |
| Options considered | (a) Score anyway, with a warning. (b) A generic error message. (c) **An Inconclusive card**: the specific reason, practical advice, "Try again", and the assurance that it does not count against the user. |
| Decision (RECOMMENDATION) | (c), covering the five gate reasons (too short, too quiet, no speech, no transcript, too few words); interruptions are a separate Interrupted state with no attempt recorded. |
| Rationale | It keeps the honest gate and turns a dead end into a quick recovery, without blaming the user or inventing a number. |
| Principles served | PP-1; PP-7; UX-3 |
| Impact on requirements | PRD-FR-032; SRS-FR-SESS-007, SRS-FR-SESS-010; SRS-BR-001, SRS-BR-008 |
| Linked decision | D-02 — Pending |
| Specified in / verified by | DES-06 §3.3.10, §3.3.11; AC-DES-52, AC-DES-53 |
| Approval status | Pending — Product Owner |

### RDD-11 Today shows one session with one primary action

| Field | Content |
|---|---|
| Context / problem | The home screen has up to 16 top-level items, ~21 widgets, ~70 targets and 25 routes; there are three daily plans; plan steps are completed by self-reported ticks; new users are greeted "Welcome back" (UXF-05, UXF-13, UXF-19, UXF-24, UXF-27; FND-UI-02, FND-UI-12). |
| Options considered | (a) A simplified dashboard with several entry cards. (b) **One session card with one "Start session" button**, a greeting, a week strip and a one-line progress note. (c) Open the app straight into the session player. |
| Decision (RECOMMENDATION) | (b). One filled button in every state; steps complete only with valid attempts; the greeting follows SRS-FR-TODAY-010. |
| Rationale | It removes decision fatigue and makes "start speaking" the fastest path (two taps to recording). Option (c) removes orientation and any chance to adjust the session. |
| Principles served | PP-3; UX-6; UX-2; PP-4; UX-10 |
| Impact on requirements | PRD-FR-020…026; SRS-FR-TODAY-001, -010, -011; SRS-BR-009, SRS-BR-013 |
| Linked decision | — |
| Specified in / verified by | DES-06 §3.2; AC-DES-22…AC-DES-31 |
| Approval status | Pending — Product Owner |

### RDD-12 Account-optional onboarding

| Field | Content |
|---|---|
| Context / problem | Google sign-in is mandatory before any use; sign-in failures show raw exception text (UXF-01, UXF-53; FND-SEC-01). |
| Options considered | (a) Keep mandatory sign-in. (b) Anonymous Firebase authentication. (c) **A local profile with optional Google sign-in later** (onboarding secondary link, Settings → Account, a non-blocking Progress prompt). |
| Decision (RECOMMENDATION) | (c). Onboarding never requires an account, network or typed input. |
| Rationale | Removes the first-run wall and supports offline, privacy-minded users (Meera, Daniel); sign-in remains available for backup and sync. Anonymous auth would still need the network at first launch. |
| Principles served | PP-6 works anywhere; PP-2 private by default; UX-2 |
| Impact on requirements | PRD-FR-001…004; SRS-FR-ACC-001…006; SRS-FR-ONB-001 (sign-in entry point, FS-02 issue 1) |
| Linked decision | D-05 account optional — Pending |
| Specified in / verified by | DES-06 §3.1; AC-DES-12 |
| Approval status | Pending — Product Owner |

### RDD-13 First speaking attempt inside onboarding

| Field | Content |
|---|---|
| Context / problem | Onboarding is three form screens with 7 self-rating sliders and no speaking, followed by an automatic 7-step tour; 9 taps to the first recording, 11 to the first feedback; self-ratings later appear as "Objective Speech Skills" (UXF-03, UXF-29, UXF-30, UXF-58; DES-01 §4). |
| Options considered | (a) Forms, then a tour. (b) **Short choices with defaults, then a real first attempt ("Introduce yourself") with feedback inside onboarding**, skippable with "Do this later". (c) No onboarding; defaults only. |
| Decision (RECOMMENDATION) | (b). The first valid attempt becomes the baseline; no self-rating sliders exist. |
| Rationale | Users experience the core value within about a minute; the baseline comes from measurement, not self-assessment. Option (c) loses goal and level, which drive the plan. |
| Principles served | PP-4 speaking first; UX-2; UX-5 personal; PP-1 |
| Impact on requirements | PRD-FR-010…014; SRS-FR-ONB-001…011 |
| Linked decision | — |
| Specified in / verified by | DES-06 §3.1; AC-DES-12…AC-DES-21 |
| Approval status | Pending — Product Owner |

### RDD-14 Contextual first-use tips instead of tours

| Field | Content |
|---|---|
| Context / problem | Four overlapping help mechanisms repeat the same content (auto tour, first-use spotlight, a help dialog that can appear twice, an inline guide strip on 23 screens); Back or Skip marks the whole tour completed; the help icon shows the wrong guide on four routes; one guide promises "live Gemini AI coaching" (UXF-14, UXF-30, UXF-10; FND-UI-11; ui_audit §8.2). |
| Options considered | (a) Keep the tour, make it skippable. (b) A help centre only. (c) **Self-explanatory screens plus at most one short, dismissible, in-place tip per screen**, stored per profile and replayable from About. |
| Decision (RECOMMENDATION) | (c): six tips in R1, never modal, never during recording, never at launch. |
| Rationale | Tips appear when they are relevant and cost one tap to dismiss; tours interrupt at peak motivation and are dismissed unread. In-page tips avoid focus traps for TalkBack users. |
| Principles served | UX-6; UX-10; UX-1; UX-9 |
| Impact on requirements | PRD-FR-120 and SRS-FR-PLAT-001 (tutorial overlay removed); a "first-use tip" pattern to add to DES-05 (DES-06 §5 issue 12) |
| Linked decision | — |
| Specified in / verified by | DES-06 §2.4; AC-DES-07, AC-DES-94 |
| Approval status | Pending — Product Owner |

### RDD-15 Brand launch moment ≤ 1 s, using the platform splash

| Field | Content |
|---|---|
| Context / problem | An unskippable launch overlay plays on every launch (2.9 s first, 2.1 s returning), lets touches through, restarts on rotation, and follows a splash of a different colour (UXF-02, UXF-48; ui_audit §8.1; CONF-035). |
| Options considered | (a) Keep the overlay but make it skippable. (b) A shortened 1-second app-drawn animation. (c) **The platform splash only**, with a static icon, shown until the first frame is ready and capped at 1 s; no app-drawn overlay. |
| Decision (RECOMMENDATION) | (c). The window background matches the app surface, so there is no colour jump; no dialog or tip appears at launch. |
| Rationale | It removes 2 s from every launch, cannot block input, behaves identically under reduced motion, and needs no new library. |
| Principles served | UX-2; UX-10; UX-9 (reduced motion) |
| Impact on requirements | NFR-PERF-001, NFR-PERF-002; DES-05 §10 |
| Linked decision | — |
| Specified in / verified by | DES-06 §2.3; AC-DES-05, AC-DES-06 |
| Approval status | Pending — Product Owner |

### RDD-16 Remove the "AI Mentor" chat and the conversation simulator

| Field | Content |
|---|---|
| Context / problem | "AI Mentor" occupies a primary tab but replies from templates; cloud AI is hard-disabled; a permanently disabled "Cloud AI Coach" switch sits in Settings; the conversation simulator uses template replies and a phone-call metaphor for one-shot dictation (UXF-40, UXF-54; FND-AI-16; ui_audit §7.3, §8.7). |
| Options considered | (a) Keep the chat with an honest label. (b) Replace it with a cloud language model (rejected: NG-01, NG-02, D-04). (c) **Remove both**; coaching moves to result coach tips linked to techniques (RDD-07), and conversation practice returns in R2 as scripted role-play. |
| Decision (RECOMMENDATION) | (c). No "AI" labels for deterministic features anywhere. |
| Rationale | A chat that cannot understand the user misrepresents itself; deterministic tips tied to measured issues are more useful and honest. |
| Principles served | PP-1; PP-5 |
| Impact on requirements | PRD-FR-120; SRS-FR-PLAT-001; PRD-FR-047 (coach tips); PRD-FR-055 / SRS-FR-PRAC-007 (R2 role-play) |
| Linked decision | D-04 cloud AI removal — Pending |
| Specified in / verified by | DES-06 §2.1, §2.5.1, §3.7.2; AC-DES-01, AC-DES-08 |
| Approval status | Pending — Product Owner |

### RDD-17 Tracks and techniques replace modules, paths and programs

| Field | Content |
|---|---|
| Context / problem | Content is spread across 15 curriculum modules, 10 activity modules, 10 paths, 7 programs (6 of which can never be completed, with no unenrol) and three different "track/path" concepts; the activity library hides five sub-products behind one route (UXF-13, UXF-22, UXF-25; ui_audit §7.3). |
| Options considered | (a) Keep modules and programs, tidied. (b) **Five tracks by situation plus techniques as a content type**, with no fixed-day programs; Today adapts daily. (c) One flat library with search. |
| Decision (RECOMMENDATION) | (b): Everyday conversation, Workplace communication, Job interviews, Presentations & storytelling, Clear speech; techniques (RDD-02) as the second collection in Practice; activities can belong to both. |
| Rationale | Users think in situations ("my interview") and methods ("how do I pause well"); both map to one activity model. Fixed-day programs guaranteed failure; a flat list offers no guidance. |
| Principles served | PP-5; UX-1; PP-3; PP-8 |
| Impact on requirements | PRD-FR-050…054, PRD-FR-056; SRS-FR-PRAC-001…006, SRS-FR-PRAC-008 |
| Linked decision | D-08 — DECIDED (technique content in own words) |
| Specified in / verified by | DES-06 §3.4; AC-DES-58…AC-DES-65 |
| Approval status | Pending — Product Owner |

### RDD-18 Words as a primary destination in R1

| Field | Content |
|---|---|
| Context / problem | Vocabulary is called "Tools" in the sidebar, holds a 20-word bank, offers an "AI Quiz" whose engine is never called, counts non-due cards as due, and counts words merely seen as "learned" (UXF-15, UXF-18, UXF-23; ui_audit §3.3). The Product Owner requires a full vocabulary lifecycle (PO-REQ-1119). |
| Options considered | (a) Words inside Practice. (b) Words inside Progress. (c) **Words as the third primary destination in R1.** |
| Decision (RECOMMENDATION) | (c): Words home (due reviews first), word detail with the five-stage stepper, spoken stages in the session player, spaced reviews. |
| Rationale | Daily reviews need a stable home; the lifecycle is a pillar (P-3) and spoken stages make vocabulary practice speaking practice. Nesting it hides due reviews. |
| Principles served | PP-4 (every stage after Learn is spoken); UX-8; UX-4 visible progress |
| Impact on requirements | PRD-FR-060…065; SRS-FR-WORD-001…011, SRS-FR-WORD-014 |
| Linked decision | D-20 R1 includes the Words tab — Pending |
| Specified in / verified by | DES-06 §3.5; AC-DES-66…AC-DES-74 |
| Approval status | Pending — Product Owner |

### RDD-19 Evidence-only progress and a single streak

| Field | Content |
|---|---|
| Context / problem | Progress counts activity, not ability ("Lessons Completed x/12", "Practice Hours" from 3 minutes per ticked step, "Words Learned" for words seen); fallback numbers and an invented trend are shown as data, including to TalkBack; four streak definitions disagree; new users see "1 Day Streak" (UXF-23, UXF-24, UXF-26, UXF-56, UXF-57; FND-UI-07, FND-DOM-07/08). |
| Options considered | (a) Keep activity counts, labelled honestly. (b) **Trends of measured metrics (only with ≥ 3 valid attempts, with noise bands), practice time from recording durations, and one streak** (a day with at least one valid speaking attempt). (c) No streak at all. |
| Decision (RECOMMENDATION) | (b): calm streak visuals (no flames, no red for missed days, no freezes), "Start your streak today" for new users, milestone celebrations only (3, 7, 14, 30, 60, 100 days). |
| Rationale | Users can see real improvement and trust every number. A single honest streak motivates consistency (Daniel responds to streaks) without manipulation; removing it would lose a proven motivator. |
| Principles served | PP-7; PP-1; UX-4; PP-5 |
| Impact on requirements | PRD-FR-070…074; SRS-FR-PROG-001…007; SRS-BR-010…012, -018, -024 |
| Linked decision | — |
| Specified in / verified by | DES-06 §2.6, §3.2, §3.6; AC-DES-09, AC-DES-28, AC-DES-75…AC-DES-83 |
| Approval status | Pending — Product Owner |

### RDD-20 Calm Indigo palette

| Field | Content |
|---|---|
| Context / problem | The "Violet Studio" palette (saturated violet, 8 pastel swatch pairs, hero and sidebar gradients) conflicts with "calm and premium" and fails WCAG contrast in 13 light-theme pairs, including outlines at 1.24:1 (UXF-41; DES-05 §3.1). |
| Options considered | (A) **Calm Indigo**: desaturated indigo accent on cool neutrals; all pairs pass AA. (B) Deep Teal: adjacent to success green. (C) Keep Violet Studio and fix contrast. |
| Decision (RECOMMENDATION) | (A), as specified with computed ratios in DES-05 §3. |
| Rationale | Keeps continuity with the indigo/violet identity while removing decoration; every text pair passes AA and most exceed AAA; semantic hues stay distinct. |
| Principles served | UX-10 calm and premium; UX-9 accessible |
| Impact on requirements | NFR-A11Y-003; DES-05 §3 (authoritative tokens) |
| Linked decision | D-18 brand palette — Pending |
| Specified in / verified by | DES-05 §3.4; DES-07 TC-A11Y-001; DES-06 §1.3 |
| Approval status | Pending — Product Owner |

### RDD-21 No dynamic colour

| Field | Content |
|---|---|
| Context / problem | Dynamic colour (Material You) is available from API 31; the current app does not use it (ui_audit §5.1). Measured, Estimated, Inconclusive, success, recording and streak are product signals that must look the same on every device. |
| Options considered | (a) Dynamic colour for all roles. (b) Dynamic primary with fixed semantic roles. (c) **A fixed palette.** |
| Decision (RECOMMENDATION) | (c). Revisit only through a design decision with automated contrast tests over generated schemes. |
| Rationale | A wallpaper-derived primary can collide with success or streak colours, contrast cannot be guaranteed per device, and users below API 31 would see a different product. |
| Principles served | PP-1 (stable evidence signals); UX-9; UX-10 |
| Impact on requirements | NFR-A11Y-003; DES-05 §3.6 |
| Linked decision | D-18 — Pending |
| Specified in / verified by | DES-05 §3.6; TC-A11Y-001 |
| Approval status | Pending — Product Owner |

### RDD-22 No update notifications and no in-app updater UI

| Field | Content |
|---|---|
| Context / problem | A mandatory update dialog blocks every route with no dismissal; in-app install likely fails and fallback advice tells users to uninstall and sideload; "Check for Updates" downloads without asking; developer controls (release channel, GitHub repo field, CI secret name) are shown to users (UXF-11, UXF-53; FND-UI-13, FND-OPS-04; ui_audit §8.4). |
| Options considered | (a) Keep the in-app updater with a dismissible dialog. (b) Adopt a store in-app update API (a new dependency, needs an ADR). (c) **No in-app update UI or update notifications**; updates arrive through the distribution channel. |
| Decision (RECOMMENDATION) | (c). The only notification channel is "Practice reminders"; the R2 inbox never carries software-update messages; About shows the version only. |
| Rationale | Removes a lock-out and data-loss risk and a security risk; distribution channels already handle updates. |
| Principles served | PP-5; UX-6; UX-10 |
| Impact on requirements | PRD-FR-121, PRD-FR-084; SRS-FR-REM-004, SRS-FR-REM-007; SRS-FR-PLAT-001; RD-16 NG-11 |
| Linked decision | D-03 distribution / updater (ADR-004) — Pending |
| Specified in / verified by | DES-06 §3.7.2, §3.7.5; AC-DES-93 |
| Approval status | Pending — Product Owner |

### RDD-23 Settings reorganised into seven groups; developer diagnostics removed from the main list

| Field | Content |
|---|---|
| Context / problem | Settings has 29 controls on one screen; engine diagnostics come first on phones; there is a permanently disabled "Cloud AI Coach" switch, dead tier chips, a plaintext YouTube API key field, update channels and an admin button, but no export, no account deletion and no About (UXF-21, UXF-52; FND-UI-14; ui_audit §8.7). |
| Options considered | (a) Reorder the existing screen. (b) **Seven groups** — Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account, About — with dead and developer controls removed and diagnostics available only as a PII-free "Share diagnostics" in About. (c) One long list with search. |
| Decision (RECOMMENDATION) | (b), with each of the 29 current controls mapped to a new home or removed (DES-06 §3.7.2). |
| Rationale | Users find privacy and learning controls quickly; nothing implies hidden features; diagnostics stay available for support without exposing engine names. |
| Principles served | UX-1; UX-6; PP-2 (privacy controls easy to find); PP-1 (no dead switches) |
| Impact on requirements | PRD-FR-091…097; SRS-FR-SET-002…008 |
| Linked decision | — |
| Specified in / verified by | DES-06 §3.7.2–§3.7.7; AC-DES-86…AC-DES-94, AC-DES-98, AC-DES-99 |
| Approval status | Pending — Product Owner |

### RDD-24 Microphone permission explained in context, with recoverable denial

| Field | Content |
|---|---|
| Context / problem | On seven recorders the permission callback has no denied branch: after one denial nothing happens, with no explanation and no way forward (UXF-36; FND-UI-05; SAC:150-154). |
| Options considered | (a) Ask at launch. (b) Ask at the first recording without explanation. (c) **Explain first (onboarding or SCR-PERM-01), then request; inline denied and permanently-denied states with "Allow microphone" or "Open settings"; re-check on resume; the app stays usable without the microphone.** |
| Decision (RECOMMENDATION) | (c). |
| Rationale | Users understand why the microphone is needed and what stays on the phone before deciding, and can always recover. |
| Principles served | UX-7 (every state defined); PP-2; PP-6; UX-9 |
| Impact on requirements | PRD-FR-011; SRS-FR-ONB-006, SRS-FR-ONB-007; SRS-ERR-001 |
| Linked decision | — |
| Specified in / verified by | DES-06 §2.2.5, §3.1, §3.3.11; AC-DES-17, AC-DES-55 |
| Approval status | Pending — Product Owner |

---

## 4. Product Owner decisions this log depends on

| D-nn | Decision | Status (2026-11-21) | RDD entries |
|---|---|---|---|
| D-02 | Evaluation philosophy: measured-first, labelled, Inconclusive over guessing | Pending | RDD-09, RDD-10 |
| D-03 | Distribution channel; remove the in-app updater (ADR-004) | Pending | RDD-22 |
| D-04 | Remove cloud AI (ADR-003) | Pending | RDD-07, RDD-16 |
| D-05 | Account optional | Pending | RDD-12 |
| D-06 | No composite score for free speaking | Pending | RDD-06, RDD-08 |
| D-08 | Creator-derived content | **DECIDED 2026-11-21 — Option B, "original wording, link out"** (PRD-FR-057; SRS-BR-028) | RDD-02, RDD-07, RDD-17 |
| D-18 | Brand palette | Pending | RDD-20, RDD-21 |
| D-20 | R1 includes the Words tab | Pending | RDD-01, RDD-18 |

If the Product Owner rejects a pending decision, the dependent RDD entries must be revisited before Stage B UI build, and the DES-06 sections and AC-DES items they cite updated in the same change (GOV-02 §7).

## 5. Issues found

| # | Issue | Source(s) | Suggested resolution | Owner |
|---|---|---|---|---|
| I-1 | RD-14 §3 ("Removed: @askvinh branding and derived content"), RD-16 §5 ("creator knowledge base → Remove") and DES-01 UXF-07 / UXF-55 responses ("Remove … D-08") still describe creator-derived content as removed; the Product Owner direction and D-08 Option B keep the techniques, re-expressed in our own words with links to the source videos | RD-14 §3; RD-16 §5; DES-01 §3.2, §3.12; PRD-FR-056/057 | Update the disposition to "Scraping tool and creator branding removed; techniques kept as re-expressed content (D-08 Option B)" | CPO / Product Manager; UX Researcher |
| I-2 | DES-05 §12.3 (updated 2026-11-21) now carries the D-08 naming rule, but §12.2 lacks "Technique", "Mistakes found", "Coach tips" and "Self-check", and guardrail G-7 scans only `strings.xml`, not content-pack text or notification strings, which SRS-BR-028 also covers | DES-05 §12.2, §13.4; SRS-BR-028 | Add the terms; extend G-7 to the content pack and notification strings | Design System Architect |
| I-3 | RDD-06 and RDD-07 change the result order and the primary action defined in DES-05 §11.15, FS-04 and FS-02 §9, and need two new DES-05 patterns (Mistake list, Coach tip card) plus a badge-free "self-reported" treatment | DES-05 §11.15; FS-02 §9; FS-04; ARC-05 §8.4 | Update DES-05 §11 and the feature specifications to the Product Owner order | Design System Architect; Product Manager |
| I-4 | D-08 is decided, but the RD-99 §8 decision list may still show it as open | RD-99 §8; PRD-FR-057 | Record D-08 as decided (2026-11-21, Option B) in RD-99 and PLN-04 | CPO / Project Manager |
| I-5 | RDD-07 adds a pending Product Owner question: should "Try again" stay primary after a passed word stage or recalled review item (DES-06 recommends the moving-on action there) | PRD-FR-047; DES-06 §5 issue 22 | Confirm the exception | Product Owner |
| I-6 | DES-02 v0.2 raises R1 to 38 screens; KPI-11 (≤ 30 distinct layouts) still holds only if SCR-PRAC-04 shares layout patterns with SCR-PRAC-03 | DES-02 §4; BRD KPI-11 | Track in DES-03's layout count | UI/UX Designer (DES-03) |
| I-7 | The validation of RDD-08 depends on ASM-04 (users find labelled metrics without a composite useful) and RDD-07's calm tone has not been tested with users; no user research exists (DES-01 L-3) | RD-14 §9; DES-01 §1.2 | Include both in the moderated usability sessions recommended in DES-01 OQ-UX-1 | UX Researcher |

---

## Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-21 | Initial decision log RDD-01…RDD-24, including the Product Owner directions of 2026-11-21: techniques kept as the core curriculum inside Practice (SCR-PRAC-04), D-08 decided as Option B, and mistakes found + coach tips after every attempt (PRD-FR-047). | UX Strategist |
