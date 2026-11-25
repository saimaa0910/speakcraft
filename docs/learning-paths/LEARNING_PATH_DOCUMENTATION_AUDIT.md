# SPEAKCRAFT — LEARNING PATHS DOCUMENTATION & SOURCE CODE AUDIT

**Branch:** `feature/complete-seven-learning-paths`  
**Audit Scope:** Full recursive inspection of `docs/`, `app/src/main/assets/content/`, `firestore.rules`, `.github/workflows/ci.yml`, and Kotlin source/test files under `app/src/`.

---

## 1. Executive Summary

SpeakCraft currently defines **seven Structured Learning Programs** in `CurriculumCatalog.structuredPrograms` (exposed on navigation route `learning_paths` via `LearningPathsScreen` in `DailyAndProgramsScreens.kt`):

1. **30-Day Communication Challenge** (`30_day_challenge`, 30 days)
2. **60-Day Fluency Program** (`60_day_fluency`, 60 days)
3. **90-Day Advanced Communication Program** (`90_day_advanced`, 90 days)
4. **Daily Vocabulary Challenge** (`vocab_challenge`, 21 days)
5. **Articulation & Pronunciation Bootcamp** (`pronunciation_program`, 28 days)
6. **Public Speaking & Storytelling Program** (`public_speaking_program`, 30 days)
7. **Professional & Influence Program** (`interview_program`, 21 days)

Total curriculum footprint across all seven programs: **280 daily lessons**.

### Current Pre-Implementation Reality
- **Program #1 (`30_day_challenge`)** was partially specified (4 weeks, 28 drill strings where Days 28–30 were collapsed into `"Day 28-30: ..."`), lacked learning objectives, structured modules/lessons, audio/written/quiz workflows, interactive assessments, evidence-based scoring/feedback, pre-enroll preview, and rich local persistence.
- **Programs #2 through #7** were incomplete stubs containing only **2 to 6 summary drill strings** for **21, 28, 30, 60, or 90 days**, making true day-by-day learning and program completion impossible (`DailyAndProgramsScreens.kt` could only toggle checkboxes on flat text labels without lesson content, practice exercises, audio/transcript feedback, or assessments).
- **Important Coexisting Systems** (must remain intact and connected):
  - **`LearningPathsEngine` (10 Activity Paths):** 10 guided paths mapping the 150-activity `ActivityCatalogue` (`m01_*`..`m10_*`), asserted by `ActivityEngineUnitTest` (`allPaths.size == 10`).
  - **`YouTubeRemovalAndBrandingPolicyTest.kt` (line 92):** explicitly asserts `CurriculumCatalog.structuredPrograms.size == 7`.

---

## 2. Recorded Documentation & Code Conflicts

| Conflict ID | Source A | Source B | Recorded Conflict | Resolution Strategy |
|---|---|---|---|---|
| **CONF-LP-01** | `docs/05_COMPLETE_FEATURE_DOCUMENTATION.md` (`FEAT-007`..`FEAT-010`), `docs/01_UI_UX_DESIGN_DOCUMENTATION.md` | `CurriculumCatalog.kt` (`structuredPrograms`), `YouTubeRemovalAndBrandingPolicyTest.kt` | Legacy docs emphasize 15 static curriculum modules and generic 30/60/90-day plans, without detailing the 4 specialized 21-day and 28-day programs (`vocab_challenge`, `pronunciation_program`, `public_speaking_program`, `interview_program`). | Preserve all 7 exact program IDs and titles in `CurriculumCatalog.structuredPrograms` (`size == 7`), while completing all 7 with structured modules, objectives, lessons, and assessments. |
| **CONF-LP-02** | `docs/07_DATABASE_DESIGN_AND_DOCUMENTATION.md` (SQL draft schema) | `CurriculumCatalog.kt`, `firestore.rules` | Legacy SQL draft defines `CHECK (duration_days IN (30,60,90,180))`, omitting 21-day and 28-day durations; live `firestore.rules` allows `currentDay in 1..180` and `completedDays.size <= 180`. | Follow live Firestore constraints (`1..180`) and support exact durations: `30, 60, 90, 21, 28, 30, 21` days (280 total days). |
| **CONF-LP-03** | `docs/LEARNING_PATHS_SPECIFICATION.md` (`LearningPathsEngine` — 10 paths) | `CurriculumCatalog.structuredPrograms` (7 programs on `learning_paths` route) | Two independent features use "Learning Paths" terminology: the 10 activity-catalogue paths (`ActivityDiscoveryScreen` `"10 Paths"` tab) and the 7 time-bound programs (`LearningPathsScreen` `learning_paths` route). | Keep both intact: preserve `LearningPathsEngine.allPaths.size == 10`, upgrade the 7 programs on `learning_paths`, and bidirectionally cross-link every lesson to verified `ActivityCatalogue` IDs (`m01_*`..`m10_*`). |
| **CONF-LP-04** | `docs/redevelopment/*` (`FS-06_PRACTICE_LIBRARY.md`, `REDESIGN_DECISIONS.md RDD-17`) | `YouTubeRemovalAndBrandingPolicyTest.kt`, live navigation (`learning_paths`) | Redevelopment planning docs proposed consolidating fixed-day challenges into 5 situation tracks (`TRK-*`), whereas policy tests and active routes enforce the 7 structured programs. | Record the track consolidation proposal transparently while delivering the complete 7-program implementation as requested and preserving existing contracts. |
| **CONF-LP-05** | `content/branding-blocklist.txt`, `YouTubeRemovalAndBrandingPolicyTest.kt`, `.github/workflows/ci.yml` | Legacy activity/transcript names in older docs | Policy tests and CI strictly ban creator names (`vinh\|giang\|askvinh\|YouTubeIntelligence*`) and creator-coined framework names listed in `branding-blocklist.txt` (`3Vs`, `5 Vocal Foundations`, `4-Color Communicator`, `Power Zone`, `4 Ways of Listening`, `High Low Buffalo`, `3-2-1 Conversation`, `Relive vs Report`, `Friendly Authority`, `3-Stage Audit`). | Author all 7 new Learning Path curricula (280 lessons + 28 assessments) with original, pedagogically grounded communication terminology free of all banned terms. |
| **CONF-LP-06** | `firestore.rules` (`enrollments` write rules) | Extended lesson/assessment persistence needs | `firestore.rules` strictly enforces `hasOnly(['userId', 'programId', 'programTitle', 'currentDay', 'completedDays', 'active', 'createdAt', 'updatedAt'])` on create and `affectedKeys().hasOnly(['currentDay', 'completedDays', 'active', 'updatedAt'])` on update. | Sync core enrollment status (`currentDay`, `completedDays`, `active`) to Firestore strictly within the allowlist, and persist rich local lesson scores, quiz answers, audio/transcript feedback, assessments, notes, and timestamps offline-first in `filesDir/learning_paths/paths_<uid>.json`. |

---

## 3. Complete Inventory of the Seven Learning Programs

### Program 1: `30_day_challenge` — **30-Day Communication Challenge**
- **Description (Code & Legacy Baseline):** Foundational daily communication transformation building vocal energy, expressive clarity, structured delivery, conversational confidence, storytelling, and real-world execution over 30 days.
- **Intended Audience:** Beginners to developing communicators establishing a consistent daily speaking habit.
- **Required Skill Level:** Beginner -> Developing (Level 1–2).
- **Dependencies / Prerequisites:** None (recommended entry point).
- **Duration:** 30 days (formerly 4 weeks with 28 strings; Days 28–30 collapsed).
- **Target Structure:** 4 Modules (Week 1: Awareness & Vocal Baseline [Days 1–7]; Week 2: Clarity, Framing & Presence [Days 8–14]; Week 3: Conversation, Listening & Stories [Days 15–21]; Week 4: High-Stakes Influence & Capstone [Days 22–30]) = **30 individual daily lessons + 3 module checkpoints + 1 Week 4 Capstone**.
- **Required Activities & Cross-Links:** `m01_*` (Baseline speaking/clarity), `m02_*` (Articulation), `m03_*` (Vocal Foundations), `m04_*` (Structure/Clarity), `m05_*` (Conversations), `m06_*` (Listening), `m07_*` (Storytelling), `m08_*` (Presence), `m09_*` (Presentation), `m10_*` (Workplace scenarios).
- **Exercise Types:** Audio speaking drills, self-recording reflections, pacing/pause reading aloud, 3-part thought structuring, mini-stories, active listening summaries, quiz checks, capstone presentation.
- **Assessment & Scoring:** Lesson rubric evaluation (Completeness, Clarity, Vocal/Structural focus, Key Concept alignment, >= 60% pass) + 4 graded Module Assessments (quiz + practical task, >= 70% pass). Separate `confidenceSelfRating` (1–5) tracked independently from objective score.
- **Completion Criteria:** All 30 daily lessons completed AND all 4 module/capstone assessments passed.
- **Pre-Implementation Status:** Partially drafted (Week 1–4 headers + 28 strings, Days 28–30 collapsed; no interactive exercises, assessments, or scoring).

### Program 2: `60_day_fluency` — **60-Day Fluency Program**
- **Description:** Extended fluency builder targeting spontaneous English formulation, hesitation/filler reduction, natural phrasing, grammatical precision under time pressure, and adaptive real-time discourse.
- **Intended Audience:** Developing to intermediate speakers looking to move from scripted comfort to spontaneous fluency.
- **Required Skill Level:** Developing -> Intermediate (Level 2–3).
- **Dependencies / Prerequisites:** Recommended completion of `30_day_challenge` (advisory, non-blocking).
- **Duration:** 60 days.
- **Target Structure:** 4 Phases x 15 days = **60 individual daily lessons + 3 phase checkpoints + 1 Phase 4 Capstone** (Phase 1: Smooth Initiation & Core Syntax [Days 1–15]; Phase 2: Connected Thought & Discourse Flow [Days 16–30]; Phase 3: Adaptive Conversation & Storytelling [Days 31–45]; Phase 4: High-Pressure Fluency & Capstone [Days 46–60]).
- **Required Activities & Cross-Links:** Mapped across `m01_*`, `m03_*`, `m04_*`, `m05_*`, `m06_*`, `m07_*`, `m09_*`, `m10_*`.
- **Exercise Types:** Timed impromptu responses, sentence expansion, connective rewriting, filler-free monologues, paraphrase drills, branching scenario turns, spontaneous Q&A.
- **Completion Criteria:** 60/60 daily lessons completed + 4/4 phase/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (1 block header with 3 summary drill strings for 60 days).

### Program 3: `90_day_advanced` — **90-Day Advanced Communication Program**
- **Description:** Comprehensive mastery track for executive clarity, persuasive argumentation, nuanced storytelling, diplomatic negotiation, crisis recovery, and strategic leadership speaking.
- **Intended Audience:** Intermediate to advanced speakers targeting leadership, high-stakes briefings, and persuasive communication mastery.
- **Required Skill Level:** Intermediate -> Advanced (Level 3–4).
- **Dependencies / Prerequisites:** Recommended completion of `60_day_fluency` or `public_speaking_program` (advisory).
- **Duration:** 90 days.
- **Target Structure:** 6 Modules x 15 days = **90 individual daily lessons + 5 module checkpoints + 1 Month 3 Executive Capstone** (Days 1–15 Cognitive Framing & Concision; Days 16–30 Vocal Command & Prosody; Days 31–45 Persuasive Structure & Rhetoric; Days 46–60 Strategic Storytelling; Days 61–75 Conflict, Negotiation & Executive Dialogue; Days 76–90 Executive Presence & Capstone).
- **Completion Criteria:** 90/90 daily lessons completed + 6/6 module/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (3 monthly headers with 6 summary strings total for 90 days).

### Program 4: `vocab_challenge` — **Daily Vocabulary Challenge**
- **Description:** 21-day applied lexical precision bootcamp turning passive vocabulary into confident, idiomatic, spoken collocations across professional, analytical, persuasive, and narrative contexts.
- **Intended Audience:** All learners seeking sharper word choice, fewer vague fillers ("good", "stuff", "thing"), and natural collocation fluency.
- **Required Skill Level:** Beginner -> Intermediate (Level 1–3).
- **Dependencies / Prerequisites:** None.
- **Duration:** 21 days.
- **Target Structure:** 3 Modules x 7 days = **21 individual daily lessons + 2 weekly checkpoints + 1 Final Capstone** (Week 1: High-Impact Verbs, Precision Nouns & Clarifiers [Days 1–7]; Week 2: Analytical, Persuasive & Nuanced Connectors [Days 8–14]; Week 3: Narrative Vividness, Synthesis & Capstone [Days 15–21]).
- **Completion Criteria:** 21/21 daily lessons completed + 3/3 module/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (1 module header with 2 summary strings for 21 days).

### Program 5: `pronunciation_program` — **Articulation & Pronunciation Bootcamp**
- **Description:** Focused 28-day articulatory mechanics program covering breath-supported phonation, vowel openness, crisp consonant endings, difficult minimal pairs, word/sentence stress, connected linking, and natural rhythm.
- **Intended Audience:** Speakers aiming for crisp intelligibility, reduced mumbling, accurate stress timing, and clearer phone/microphone delivery.
- **Required Skill Level:** All Levels (Level 1–3).
- **Dependencies / Prerequisites:** None.
- **Duration:** 28 days.
- **Target Structure:** 4 Modules x 7 days = **28 individual daily lessons + 3 weekly checkpoints + 1 Articulation Capstone** (Week 1: Breath, Jaw & Consonant Precision [Days 1–7]; Week 2: Vowel Clarity & Minimal Pair Contrast [Days 8–14]; Week 3: Word Stress, Rhythm & Reductions [Days 15–21]; Week 4: Connected Speech, Intonation & Capstone [Days 22–28]).
- **Completion Criteria:** 28/28 daily lessons completed + 4/4 module/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (1 module header with 2 summary strings for 28 days).

### Program 6: `public_speaking_program` — **Public Speaking & Storytelling Program**
- **Description:** 30-day stage, camera, and presentation craft curriculum developing composure under spotlight, memorable openings, structured main points, sensory storytelling, visual/vocal contrast, Q&A agility, and mistake recovery.
- **Intended Audience:** Speakers preparing for talks, team briefings, demos, keynotes, webinars, or recorded presentations.
- **Required Skill Level:** Developing -> Advanced (Level 2–4).
- **Dependencies / Prerequisites:** Helpful after `30_day_challenge` or `pronunciation_program` (advisory).
- **Duration:** 30 days.
- **Target Structure:** 4 Modules (Days 1–7 Stage Grounding, Openings & Anchor Presence; Days 8–15 Core Structure & Audience Framing; Days 16–23 Narrative Arcs & Expressive Delivery; Days 24–30 Q&A Agility, Recovery & Keynote Capstone) = **30 individual daily lessons + 3 module checkpoints + 1 Keynote Capstone**.
- **Completion Criteria:** 30/30 daily lessons completed + 4/4 module/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (1 module header with 2 summary strings for 30 days).

### Program 7: `interview_program` — **Professional & Influence Program**
- **Description:** 21-day high-stakes workplace and interview readiness program covering concise self-introductions, behavioral STAR/CAR narratives, technical concept translation, confident objection handling, feedback conversations, and executive synthesis.
- **Intended Audience:** Job candidates, engineers, managers, and professionals preparing for interviews, stakeholder reviews, or leadership meetings.
- **Required Skill Level:** Developing -> Advanced (Level 2–4).
- **Dependencies / Prerequisites:** Helpful after `30_day_challenge` or `vocab_challenge` (advisory).
- **Duration:** 21 days.
- **Target Structure:** 3 Modules x 7 days = **21 individual daily lessons + 2 module checkpoints + 1 Mock Interview & Executive Influence Capstone** (Week 1: Positioning, Self-Intro & Structured Answers [Days 1–7]; Week 2: Behavioral Stories & Analytical Clarity [Days 8–14]; Week 3: Objections, Negotiation, Feedback & Capstone [Days 15–21]).
- **Completion Criteria:** 21/21 daily lessons completed + 3/3 module/capstone assessments passed.
- **Pre-Implementation Status:** Stub only (1 module header with 2 summary strings for 21 days).

---

## 4. Source Code Audit Summary

### UI & Navigation Layer
- `app/src/main/java/com/example/ui/screens/DailyAndProgramsScreens.kt` (`LearningPathsScreen`): renders all `CurriculumCatalog.structuredPrograms` on a flat card list; lacks program preview before enrollment, unenroll/pause action, search/filter, module hierarchy, lesson detail, interactive practice, transcript mistake highlighting, or assessments.
- `app/src/main/java/com/example/ui/AppNavigation.kt`: routes `"learning_paths"` to `LearningPathsScreen` using only `enrollments`, `onEnroll`, and `onToggleDay` from `MainCoachViewModel`. Needs new parameterized routes for `learning_path_detail/{programId}`, `learning_path_lesson/{programId}/{dayNumber}`, and `learning_path_assessment/{programId}/{moduleId}` wired to a dedicated `LearningPathsViewModel`.
- Reusable UI & media components ready for direct reuse:
  - `TranscriptDisplayCard` (`com.example.ui.components`) + `TranscriptAnalyzer` (`com.example.speech.pipeline`) for transcript mistake/warning/tip highlights.
  - `AudioRecorderManager` & `LiveTranscriptionManager` for offline recording & transcription.
  - `TtsAndSpeechHelper` for offline TTS model-example playback.
  - `UniversalActivityScreen` (`activity/{activityId}`) for seamless `"Practice in Full Studio"` escalation.

### Domain, Evaluation & Persistence Layer
- `CurriculumCatalog.kt`: defines `structuredPrograms` (`List<StructuredProgram>`). Must derive cleanly from a comprehensive curriculum registry so `structuredPrograms.size == 7` remains guaranteed while expanding every program from 2–28 strings to full daily curricula (`280` lessons + `28` assessments).
- Existing local intelligence analyzers (`LocalLanguageAnalysisEngine`, `DynamicLocalCoachEngine`, `LocalResponseAnalyzer`, `TranscriptAnalyzer`, `EvaluationEvidenceValidator`) work 100% offline/on-device without any cloud AI dependency.
- `SpeakCraftRepository.kt`: Firestore `enrollments` collection strictly allows `userId`, `programId`, `programTitle`, `currentDay`, `completedDays`, `active`, `createdAt`, `updatedAt`. `toggleProgramDay` lacks safe handling if the enrollment doc does not yet exist and lacks pause/resume (`active`) updates.
- Dedicated `LearningPathPersistenceRepository` storing full per-user state atomically in `filesDir/learning_paths/paths_<uid>.json` satisfies 100% offline capability while syncing enrollment progress cleanly with Firestore and cross-marking linked `ActivityCatalogue` progress.
