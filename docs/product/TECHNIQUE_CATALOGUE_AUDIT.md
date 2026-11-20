# Technique Catalogue Audit — SpeakCraft

| Field | Value |
|---|---|
| Document ID | CNT-01 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Content Strategist / Business Analyst |
| Approval | Pending — Product Owner |
| Related | PRD-FR-047, PRD-FR-056, PRD-FR-057, SRS-BR-028, FS-05, FS-06, ARC-03 §6, ARC-05 |

---

> **Product Owner decisions this audit applies (2026-11-21).** (1) The curriculum is built from the frameworks, techniques, tasks, exercises, activities and tips taught on the @askvinh YouTube channel (PRD-FR-056). (2) **D-08 is DECIDED: "original wording, link out"** (PRD-FR-057, SRS-BR-028): every item is re-expressed in our own words and examples, with no verbatim transcript text; each item links to the **specific** source video and timestamp; there is no creator name, likeness or branding in the app or store listing. (3) Content is gathered by hand: no YouTube scraping, caption download or API ingestion (ADR-011). (4) After every submitted attempt the result shows the **mistakes found** (deterministic or on-device checks, with evidence, labelled Measured or Estimated) and **coach tips** linked to techniques (PRD-FR-047, SRS-FR-EVAL-015/016, SRS-FR-WORD-014). The Product Owner reports that the channel material has already been added to the present app and asked for it to be checked. This document is that check.

> **Scope warning.** This audit inventories what is **in the code**. It does **not** catalogue the channel itself and cannot say how much of the channel's teaching is covered (see §5).

## 1. Purpose and method

### 1.1 Purpose

1. Give every creator-derived or teaching item in the current app an ID, a code location, its provenance state and its risks (creator name, verbatim text).
2. Map each item to the new product shape (templates TPL-\*, tracks TRK-\*, Today eligibility; ARC-03 §6.7–6.9; FS-06 §5.7).
3. For every technique, state which mistakes our checks can honestly detect (ARC-05 §6, §8.4) and which aspects need a self-check, and propose issue codes and coach tips (PRD-FR-047).
4. Define the hand-verification and rewrite workflow and estimate its effort, so the items can move into the content pack with `licenceStatus = ORIGINAL_REEXPRESSION` (ARC-03 §6.4, §6.14).
5. List every creator-name or branding string that must be removed under D-08.

### 1.2 What was read

| Source | What was used |
|---|---|
| `docs/redevelopment/working_papers/PLAN_AND_CANON.md` R-1…R-6 | Canonical IDs, templates, tracks, metric labels, R-6 Product Owner aim |
| `working_papers/data_learning_audit.md` (item 7, §1.4, §2.1–2.3, §9–§11) | Earlier counts of creator mentions, catalogue structure, provenance risk |
| `working_papers/docs_claims.md`, `ui_audit.md` §1.8 | Claimed pedagogy; the "@askvinh" tab and the ingestion pipeline |
| `docs/architecture/LOCAL_INTELLIGENCE_ARCHITECTURE.md` (ARC-05 v0.2) | §3 tiers, §5 evidence gate, §6 metric catalogue and band table, §7 headline metrics, §8.1–8.4 rules, feedback, issue catalogue, self-check and candidate acoustic metrics, §9 word stages. ARC-05 was updated to v0.2 by its owner during this audit; this document uses v0.2 |
| `docs/architecture/DATABASE_DESIGN.md` (ARC-03) §1.4–1.5, §6.4–6.14 | Provenance block, technique and tip schema, trigger kinds, validation rules V-01…V-21 |
| `docs/architecture/TECHNICAL_DESIGN.md` (ARC-02) §3.1 | `Technique`, `Provenance`, `LicenceStatus`, `IssueCode` |
| `docs/requirements/SRS.md` | SRS-FR-EVAL-015/016, SRS-FR-PRAC-008/009, SRS-FR-WORD-014, SRS-BR-028, SRS-FR-SESS-002/003, SRS-FR-TODAY-004, template and track definitions |
| `docs/product/PRD.md`, `FEATURE_SPECIFICATIONS/FS-05`, `FS-06` | PRD-FR-047/056/057; FS-05 §5.10 and Issue 9; FS-06 §5.7 |
| Code (read in full) | `CurriculumCatalog.kt` (1,124 lines), `FeatureTutorialRegistry.kt`, `DeterministicFeedbackTemplates.kt`, `ActivityCatalogue.kt` |
| Code (read in part, by grep with context) | The three activity catalogue files (all 150 activity headers extracted by script), `LearningPathsEngine.kt`, `DailyPracticePlanEngine.kt`, `LocalFeedbackEngine.kt`, `DynamicLocalCoachEngine.kt`, `LocalIntelligenceOrchestrator.kt`, `RecommendationEngine.kt`, `SpeakCraftRepository.kt`, `FirestoreModels.kt`, `CameraVideoRecorder.kt`, `InteractiveCoachViewModel.kt` and the UI files that contain creator strings |
| Grep over the whole of `app/src/main` | Case-insensitive: `askvinh`, `vinh`, `technique`, `framework`, `exercise`, `drill`, `challenge`, `shadow`, `tip`, `PREP`, `STAR`, `CLEAR`, `3V`, `Vocal`, `Power Zone`, `VAKS`, `Buffalo`, `Mastery`, plus `FORD`, `CCC`, `BLUF`, `SBI`, `Match…Mirror`, `3-Stage`, `Relive`, `4-Color`, `Friendly Authority` |
| Repository root and tests | `metadata.json`, `firestore.rules`, `firestore.test.js`, `app/src/test/**` (creator strings only) |
| Web search (standard mode, narrow) | Used only to look for the URL of the specific video for items whose code cites a video title. No YouTube page, caption or transcript was fetched; no video text was copied. Results are recorded in §3.9 as "candidate — Product Owner to verify" |

Nothing was built or run.

### 1.3 Labels

| Label | Meaning |
|---|---|
| **VERIFIED** | Seen in code or a document at the cited `path:line` |
| **INFERRED** | A reasoned conclusion from VERIFIED facts, not observed directly |
| **ASSUMPTION** | Taken as true to proceed; needs confirmation |
| **RECOMMENDATION** | Proposed by this document; needs Product Owner approval |
| **Proposed** | An estimate or threshold that needs approval (same meaning as † in ARC-05) |

Every `path:line` citation in §3, §4 and §7 is VERIFIED unless marked otherwise. Every "what it teaches" line, coach tip and self-check item in this document is **newly written in our own words**; none is copied from the code or from any video.

### 1.4 Path aliases

Paths are relative to `app/src/main/java/com/example/` unless noted.

| Alias | Path | Alias | Path |
|---|---|---|---|
| CAT | `data/model/CurriculumCatalog.kt` | NAV | `ui/AppNavigation.kt` |
| FTR | `data/model/FeatureTutorialRegistry.kt` | CC | `ui/components/CoachComponents.kt` |
| FM | `data/model/FirestoreModels.kt` | W | `ui/components/DashboardStudioWidgets.kt` |
| ACT | `data/activity/ActivityCatalogue.kt` | ADM | `ui/screens/AdminDashboardScreen.kt` |
| A13 | `data/activity/ActivityCatalogueModules1To3.kt` | AO | `ui/screens/AuthAndOnboardingScreens.kt` |
| A46 | `data/activity/ActivityCatalogueModules4To6.kt` | CM | `ui/screens/ConversationAndMentorScreens.kt` |
| A710 | `data/activity/ActivityCatalogueModules7To10.kt` | DP | `ui/screens/DailyAndProgramsScreens.kt` |
| LPE | `data/activity/LearningPathsEngine.kt` | D | `ui/screens/DashboardScreen.kt` |
| DPPE | `data/learning/DailyPracticePlanEngine.kt` | LAT | `ui/screens/LabsAndTrainingScreens.kt` |
| DFT | `data/ai/local/language/DeterministicFeedbackTemplates.kt` | LM | `ui/screens/LessonAndModulesScreens.kt` |
| LFE | `data/ai/local/language/LocalFeedbackEngine.kt` | SAC | `ui/screens/SpeakingAndChallengesScreens.kt` |
| DLCE | `data/ai/local/language/DynamicLocalCoachEngine.kt` | YT | `ui/screens/YouTubeIntelligenceScreen.kt` |
| LLAE | `data/ai/local/language/LocalLanguageAnalysisEngine.kt` | ICVM | `ui/viewmodel/InteractiveCoachViewModel.kt` |
| LIO | `data/ai/local/LocalIntelligenceOrchestrator.kt` | MVM | `ui/viewmodel/MainCoachViewModel.kt` |
| RE | `data/ai/RecommendationEngine.kt` | REPO | `data/firebase/SpeakCraftRepository.kt` |
| CVR | `data/media/CameraVideoRecorder.kt` | (root) | `metadata.json`, `firestore.rules`, `firestore.test.js` |

### 1.5 ID scheme and field values

| Prefix | Content type | Range used |
|---|---|---|
| CNT-T-nn | Technique or named framework | T-01…T-37 |
| CNT-X-nnn | Exercise, drill, activity, prompt or conversation scenario (three digits because there are more than 99) | X-001…X-049 (CurriculumCatalog and other code), X-101…X-250 (the 150-activity catalogue) |
| CNT-C-nn | Challenge | C-01…C-10 |
| CNT-P-nn | Programme, plan, routine or module taxonomy | P-01…P-21 |
| CNT-S-nn | Shadowing or read-aloud script | S-01…S-07 |
| CNT-K-nn | Coach tip (one row per distinct idea; repeats listed as duplicates) | K-01…K-33 |

In tables the `CNT-` prefix is often dropped (for example "T-04" means CNT-T-04).

**Provenance status** (as found in code):

| Value | Meaning |
|---|---|
| SPECIFIC_VIDEO | The code cites a specific video URL or ID that a reviewer can open |
| CHANNEL_ROOT_ONLY | The code cites only the channel (root URL, channel search URL or the handle as a label) |
| TIMESTAMP_UNVERIFIABLE | The code cites a timestamp, but against the channel root, so the timestamp cannot be checked |
| NONE | No source cited (includes the activity catalogue's constant "SpeakCraft Core Curriculum") |

**Tier shorthand for §4** (ARC-05 §3): **TR** = needs a transcript (T1, T2, T3a; not T3b). **AU** = audio only (all tiers; PCM in T1/T3b, coarser RMS callbacks in T2/T3a). **TR+AU** = needs both (M-WPM; T1–T3a). **PCM** = needs app-owned PCM; ARC-05 §8.4 limits its candidate acoustic metrics to T1 (see §8 I-01).

## 2. Summary counts

### 2.1 Items by content type

| Content type | Items | Where | Of which name the creator in code or UI |
|---|---|---|---|
| Technique / named framework | **37** | 15 in `CurriculumCatalog.askVinhKnowledgeBase` (CAT:187-519; 12 labelled "VERIFIED", 3 "PENDING_TRANSCRIPT") + 22 named in the activity catalogue, labs and feedback code | 16 |
| Exercise / drill | **19** | 8 pronunciation drills, 7 spoken-grammar lessons (CAT:916-968), 4 generated micro-drills (DLCE:535-557) | 0 |
| Activity / prompt | **167** | 150 catalogue activities (A13, A46, A710), 6 public-speaking modules, 7 real-world tasks (CAT:970-987), 4 speaking-lab presets (SAC:135-141) | 13 |
| Challenge | **10** | CAT:887-898 | 10 |
| Programme / plan | **21** | 7 structured programmes, 1 daily routine (CAT:989-1123), 10 learning paths (LPE), 1 plan-task generator (DPPE), 2 module taxonomies (CAT:167-183, ACT:107-188) | 3 |
| Shadowing / read-aloud script | **7** | 3 shadowing segments (CAT:927-958), 4 voice-lever scripts (LM:534-540) | 7 |
| Coach tip (distinct ideas) | **33** | DFT, LFE, DLCE, LIO, FTR, CVR, CAT | 5 |
| Conversation scenario | **13** | CAT:900-914 with 3 opening lines (ICVM:195-200) | 0 |
| **Total** | **307** | | **54** |

Not counted as separate items (they repeat other items): the 12 generated lessons (CAT:521-542), recommendation cards (RE:30-137), dashboard slides (W:205-240), the YouTube-extraction templates (LIO:820-910) and the 14 app-usage tutorial texts in FTR (§3.8).

### 2.2 Items by provenance status

| Provenance status | Techniques | Exercises | Activities | Challenges | Programmes | Scripts | Tips | Scenarios | Total |
|---|---|---|---|---|---|---|---|---|---|
| SPECIFIC_VIDEO | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0** |
| TIMESTAMP_UNVERIFIABLE | 12 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **12** |
| CHANNEL_ROOT_ONLY | 4 | 0 | 13 | 10 | 1 | 3 | 0 | 0 | **31** |
| NONE | 21 | 19 | 154 | 0 | 20 | 4 | 33 | 13 | **264** |
| **Total** | 37 | 19 | 167 | 10 | 21 | 7 | 33 | 13 | **307** |

**Key finding (VERIFIED).** No item in the code has specific-video provenance. The 12 items labelled `"VERIFIED"` (CAT:208…450) all point at `https://www.youtube.com/@askvinh`, the channel root, so their video titles and timestamps cannot be checked. Every item must therefore be verified by hand before it can ship (SRS-BR-028; ARC-03 V-10…V-12).

### 2.3 Other risk counts

| Measure | Count | Notes |
|---|---|---|
| Items exposing the creator's name or handle | 54 of 307 | §3 "Creator name" column; strings listed in §7 |
| Source lines in `app/src/main` containing "vinh" (case-insensitive) | 171 | 102 in CAT, 22 in FTR, 47 in 18 other files. Of the 171: 101 user-visible or content text, 32 URL or handle values, 38 code identifiers or comments (§7.1) |
| Further lines outside `app/src/main` | 12 | `metadata.json:3` (store-style description) and 11 test lines (§7.4) |
| Verbatim-text risk: High | 2 items + 1 data store | S-01, S-03; plus per-user `extractedTechniques` documents whose `creatorAdvice` is a caption sentence copied as-is (LIO:869) |
| Verbatim-text risk: Medium | 15 items | T-01…T-12 (attributed "creator advice" that cannot be compared with the source without fetching it), S-02, K-02, K-10 |
| Activities linked to a technique (proposed) | 127 of 150 | §3.3 |
| Activities with a named framework or creator term in the text | 14 | CCC, FORD, High-Low-Buffalo, Match & Mirror, VAKS, Power Zone (×2), STAR, BLUF (×2), SBI, PREP, "Baseline + Cluster + Context", "5 vocal foundations" |

### 2.4 Technique feedback feasibility (detail in §4)

| Class | Techniques | Count |
|---|---|---|
| A — core mistake detectable with existing ARC-05 metrics | T-04 PREP, T-06 silence instead of fillers, T-20 STAR | **3** |
| B — some mistakes detectable; the rest needs a self-check and/or an ARC-05 extension | T-01, T-02, T-03, T-05, T-07, T-08, T-12, T-13, T-16, T-17, T-19, T-21, T-22, T-24…T-30, T-32…T-36 | **25** |
| C — core of the technique not detectable from audio or transcript; self-check only (generic checks such as length still apply) | T-09, T-10, T-11, T-18, T-23, T-31, T-37 | **7** |
| D — blocked: the technique's content is not defined in code | T-14, T-15 | **2** |

ARC-05 gaps raised: **14** (G-01…G-14, §4.3). Two of them (pitch range, volume variation) already exist in ARC-05 §8.4 as unapproved candidates; the other 12 are new.

## 3. Master inventory

How to read the tables:

- **Name as in code** is quoted exactly; it is an internal reference, not a proposed display name (ARC-03 §6.8: the display name must be our own wording).
- **Source cited in code** is quoted exactly as written (title · URL · timestamp). Candidate specific videos found by web search are kept separately in §3.9.
- **Verbatim risk:** *High* = text presented as the creator's own words or delivery; *Medium* = text presented as the creator's teaching ("creator advice"), which cannot be compared with the source without fetching it (not allowed), so it must be rewritten anyway; *Low* = generic wording with no attribution. This document quotes at most a few words of any code string.
- **Proposed mapping** = template · track(s) · Today eligibility (Warm-up = TPL-READ/REPEAT step; Main = TPL-ANSWER/STRUCT/STORY step; Practice only; R2; No). `TPL-STRUCT*` means the structure needs a framework lexicon that ARC-05 does not yet have (gap G-06); until then the activity runs as TPL-ANSWER (RECOMMENDATION).
- **Duplicates** lists other places that define or repeat the same idea.

### 3.1 Techniques and named frameworks (CNT-T)

| CNT-ID | Name as in code | Type | Location | What it teaches (our words) | Source cited in code | Provenance | Creator name exposed | Verbatim risk | Proposed mapping | Duplicates / also defined at |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | "The 3Vs of Communication (Visual, Vocal, Verbal)" (`vinh_3vs`) | Framework | CAT:188-209 | Listeners judge a message through three channels at once — how you look, how you sound and what you say — so practise and review each channel separately | "How to Speak Better Than 99% of People (Complete Framework)" · `https://www.youtube.com/@askvinh` · "01:15" (CAT:204-206) | TIMESTAMP_UNVERIFIABLE | Yes — name in advice and context (CAT:195, 197); id/videoId (CAT:189-190) | Medium — written as "[creator] teaches that…" (CAT:195) | TPL-ANSWER for sound and words + self-check for the visual channel · TRK-PRESENT, TRK-WORK · Main | W:207-213 (slide), SAC:137 (preset), DPPE:290, CAT:895 (C-08), CAT:971, 1010, 1080, A13:1067 (`m03_vocal_image`), A710:994 |
| T-02 | "The 5 Vocal Foundations (Rate, Pause, Melody, Volume, Character)" (`vinh_5_vocal`) | Framework | CAT:210-231 | Treat the voice as adjustable: vary speed, use silence, move pitch, change loudness and colour the emotion so the key idea stands out | "Vinh Giang - 5 Vocal Foundations - Rate of Speech" · `https://www.youtube.com/@askvinh` · "00:45" (CAT:226-228) | TIMESTAMP_UNVERIFIABLE | Yes — name in title, advice, context (CAT:217, 219, 226); module subtitle CAT:169; LM:636; FTR:196 | Medium — "[creator] compares the voice to…" (CAT:217) | TPL-READ (contrast scripts) and TPL-ANSWER · TRK-CLEAR, TRK-PRESENT · Warm-up / Main | CAT:169, 889, 975, 993, 1013-1014, 1045, 1082; LM:532-540; FTR:192-206; DFT:58, 264; LFE:80; RE:91-93; DPPE:280; A13:860-1136 (m03 activities), A13:1104 |
| T-03 | "Record and Review: 3-Stage Self-Awareness Audit" (`vinh_3stage_audit`) | Technique (self-review method) | CAT:232-253 | Record yourself, leave it for a while, then review in separate passes — picture only, sound only, words only — and pick one habit to change | "The Ultimate 30-Day Plan To Master Your Communication Skills" · `https://www.youtube.com/@askvinh` · "04:05" (CAT:248-250) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:241; SAC:408; FTR:126, 138; CVR:192 | Medium — "Taught by [creator] as…" (CAT:241) | Not a template: a review habit offered after any attempt with replay (SCR-PROG-04; replay needs T1/T3b) · all tracks · No (not a step) | CAT:181, 986, 996, 1012, 1072, 1111; SAC:145, 408-417, 623, 631; FTR:121-138; CVR:192; RE:131-133; DFT:244; W:212 |
| T-04 | "The PREP Impromptu Framework" (`vinh_prep`) | Framework | CAT:254-275 | Answer on the spot by giving your view first, then one reason, one concrete example, and your view again | "How to Answer ANY Question (PREP Framework)" · `https://www.youtube.com/@askvinh` · "02:30" (CAT:270-272) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:263; id `lesson_vinh_prep` used as a route (RE:59) | Medium — creator-attributed advice field | TPL-STRUCT (framework PREP) · TRK-WORK, TRK-INTERVIEW, TRK-EVERYDAY · Main | CAT:174, 178, 894 (C-07), 938-947 (S-02), 973, 983, 1038, 1043, 1058, 1072, 1083; A46:363 (`m04_structured_answers`), A46:317; RE:56-61; DPPE:158; DFT:58, 256; LFE:348-349, 359, 388; LIO:699, 732, 895; FTR:145-153, 247; LPE:58, 296. Note: PREP is also widely taught outside the channel (INFERRED; §8 Q-01) |
| T-05 | "The CLEAR Framework for Complex Ideas" (`vinh_clear`) | Framework | CAT:276-297 | Explain a complex idea by starting from what the listener knows, linking it to something familiar, showing the benefit, removing jargon and repeating the takeaway | "How to Explain Anything To Anyone (Even If It's Complex!)" · `https://www.youtube.com/@askvinh` · "03:10" (CAT:292-294) | TIMESTAMP_UNVERIFIABLE | Yes — videoId/id (CAT:277-278) | Medium | TPL-ANSWER (TPL-STRUCT* once a CLEAR lexicon exists) · TRK-WORK · Main | CAT:178-179, 891 (C-04), 971, 981, 985, 1038, 1042, 1083, 1118, 1120; FTR:247, 283; A46:204, 227, 411; A710:1358; A13:326. Name collision: a CCC step is labelled "CLEAR:" (A46:191) |
| T-06 | "Eliminate Filler Words With Intentional Silence" (`vinh_pauses`) | Technique | CAT:298-319 | When you need thinking time, stop and stay silent instead of filling the gap with "um", "uh" or "like" | "How to Pause Without Feeling Awkward" · `https://www.youtube.com/@askvinh` · "01:40" (CAT:314-316) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:307 | Medium | TPL-ANSWER · all tracks · Main | CAT:896 (C-09), 984, 1027; A13:929, 1044; DFT:146, 194, 210, 224, 262; LFE:106-114, 394; DLCE:511, 544; RE:36-41; DPPE:92; LIO:697; FTR:134 |
| T-07 | "Daily Articulation & Enunciation Muscle Warm-Up" (`vinh_articulation`, videoId `askvinh_cork`) | Technique (warm-up) | CAT:320-341 | Warm up jaw, lips and tongue with deliberately exaggerated movement so ordinary-speed speech comes out crisper | "Simple Exercise to DRASTICALLY Improve Your Articulation!" · `https://www.youtube.com/@askvinh` · "02:00" (CAT:336-338) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:329 | Medium. Also a possible content mismatch with the cited video (§3.9) | TPL-REPEAT (reference phrases) · TRK-CLEAR · Warm-up | CAT:168, 991, 1040, 1102; A13:444-806 (m02 activities); LIO:704-707; FTR:157-172; DPPE:118, 134-136; RE:46-51 |
| T-08 | "Relive the Story Instead of Reporting It" (`vinh_storytelling`) | Technique | CAT:342-363 | Put the listener inside the moment — open at a time and place, give the words people actually said, add sensory detail — instead of summarising | "3 Powerful Ways To Tell Stories Without Boring People" · `https://www.youtube.com/@askvinh` · "03:45" (CAT:358-360) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:351 | Medium | TPL-STORY · TRK-PRESENT, TRK-EVERYDAY · Main | CAT:175, 948-957 (S-03), 974, 1029, 1111; DFT:257; FTR:264, 271, 305; A710:120, 284, 330, 871 |
| T-09 | "The 4-Color Communicator Adaptation" (`vinh_4colors`) | Framework | CAT:364-385 | Notice whether a listener cares most about results, enthusiasm, harmony or detail, and shape your opening for them | "How to Talk to ANYONE (Once You Know Their Color!)" · `https://www.youtube.com/@askvinh` · "02:20" (CAT:380-382) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:373 | Medium. Possible third-party origin of the model (§8 Q-02) | TPL-ANSWER (two versions of one update) + self-check · TRK-WORK · Practice only | CAT:180, 1054, 1073, 1083, 1118-1120 |
| T-10 | "The 4 Ways of Listening (Eyes, Ears, Brain, Heart)" (`vinh_listening`) | Framework | CAT:386-407 | Listen with your eyes, ears, mind and empathy; show it by pausing and paraphrasing before you reply | "The 4 Ways of Listening to Build Instant Rapport" · `https://www.youtube.com/@askvinh` · "01:55" (CAT:402-404) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:395; FTR:223 | Medium | R2 TPL-ROLE (paraphrase turn); R1 self-check + short spoken summary (TPL-ANSWER) · TRK-EVERYDAY, TRK-WORK · R2 | CAT:173, 982, 1017, 1031; FTR:223, 238; LAT:461; RE:107-109; A46:780-1063 (m06 activities) |
| T-11 | "Purposeful Hand Gestures in the Power Zone" (`vinh_body_power`) | Technique | CAT:408-429 | Keep hands visible around the torso, use open gestures that show size, order or contrast, and rest them between points | "How to Use Hand Gestures Naturally When Speaking" · `https://www.youtube.com/@askvinh` · "02:50" (CAT:424-426) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:417; CVR:192 | Medium | Self-check only (R1 captures audio only, ARC-05 §4.1) · TRK-PRESENT · No | CAT:176, 976, 1016, 1111; A710:15, 260-283, 544-567, 614; CVR:192; FTR:267, 305; LPE:205 |
| T-12 | "Sound Friendly Without Losing Authority" (`vinh_friendly_auth`) | Technique | CAT:430-451 | Sound warm and decisive together: let statements end on a falling pitch, keep an unhurried pace, and keep rising endings for real questions | "How to Sound Friendly Without Losing Authority" · `https://www.youtube.com/@askvinh` · "01:10" (CAT:446-448) | TIMESTAMP_UNVERIFIABLE | Yes — CAT:439 | Medium | TPL-READ (statements) and TPL-ANSWER · TRK-WORK, TRK-INTERVIEW · Warm-up / Main | CAT:892, 897, 923 (X-007), 1041, 1055, 1120; A13:349, 975; A46:137, 158; A710:1450; FTR:155, 179-189, 291, 305; DLCE:437; LIO:690 |
| T-13 | "High Low Buffalo (Source Verification Pending)" (`vinh_pending_high_low_buffalo`) | Framework | CAT:453-474 (seed); A46:460-482 (full activity) | Share a recent high point, a low point and an unexpected detail, then invite the other person to share theirs | "@askvinh — High Low Buffalo (Pending Ingestion)" · `https://www.youtube.com/@askvinh/search?query=high+low+buffalo` · "Pending" (CAT:469-471) | CHANNEL_ROOT_ONLY | Yes — CAT:460-469 | Low (placeholder text). **Contradiction:** the seed says not to invent steps, yet A46:460 defines full steps under "SpeakCraft Core Curriculum" | TPL-STRUCT* (else TPL-ANSWER) · TRK-EVERYDAY · Main | ACT:144; LPE:164; A46:460. Also a widely used ice-breaker outside the channel (INFERRED; §8 Q-01) |
| T-14 | "3-2-1 Conversation Framework (Source Verification Pending)" (`vinh_pending_321_convo`) | Framework (undefined) | CAT:475-496 | Not defined in code — only the name exists | "@askvinh — 3-2-1 Conversation Framework (Pending Ingestion)" · `…/@askvinh/search?query=3+2+1+conversation` · "Pending" (CAT:491-493) | CHANNEL_ROOT_ONLY | Yes — CAT:482-491 | Low (placeholder) | Blocked until verified · — · No | None in code. A web result suggests a "3-2-1" method exists but for answering on the spot, not conversation (§3.9) |
| T-15 | "Nine Habits for Clearer Speaking & Nervousness Tools (Source Verification Pending)" (`vinh_pending_nine_habits`) | Series placeholder (undefined) | CAT:497-518 | Not defined in code — a placeholder for clearer-speaking habits, a daily articulation routine, mumbling and nervousness tools | "@askvinh — Clearer Speaking & Nervousness Series (Pending Ingestion)" · `…/@askvinh/search?query=nervous+mumbling+clearer` · "Pending" (CAT:513-515) | CHANNEL_ROOT_ONLY | Yes — CAT:504-513 | Low (placeholder) | Blocked until verified; probably several techniques · — · No | Related: A710:1010 (`m09_nervousness_management`), T-07 |
| T-16 | "CCC Framework" (`m04_ccc_framework`) | Framework | A46:181-203 | Make an update clear (one headline), concise (few words) and compelling (why it matters to people) | "SpeakCraft Core Curriculum" (A46:85) | NONE | No | Low | TPL-STRUCT* (else TPL-ANSWER) · TRK-WORK · Main | ACT:136; LPE:50, 58; A46:250 |
| T-17 | "FORD Framework" (`m05_ford_framework`) | Framework | A46:437-459 | Keep small talk going by moving between family, work, free time and hopes | "SpeakCraft Core Curriculum" | NONE | No | Low | R2 TPL-ROLE; R1 TPL-ANSWER ("say four questions you could ask") · TRK-EVERYDAY · R2 | ACT:144; LPE:160, 173; A46:709; A710:1335 |
| T-18 | "Match and Mirror" (`m05_match_and_mirror`) | Technique | A46:640-662 | Build comfort by subtly matching the other person's energy, pace and key words without copying them | "SpeakCraft Core Curriculum" | NONE | No | Low | Self-check (R2 TPL-ROLE) · TRK-EVERYDAY · No | ACT:144; LPE:166, 174; A710:568 |
| T-19 | "VAKS Framework" (`m07_vaks_framework`) | Framework | A710:212-234 | Make a scene vivid by adding something seen, heard, felt and smelled or tasted | "SpeakCraft Core Curriculum" (A710:90) | NONE | No | Low | TPL-STORY · TRK-PRESENT · Main | ACT:160; A710:9 (rubric), 353; LIO:726; LPE:224, 236 |
| T-20 | STAR (in `m10_interview_preparation`) | Framework | A710:1037-1059 | Answer a behavioural question with the situation, the task, what you did and the measurable result, spending most time on action and result | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-STRUCT (framework STAR) · TRK-INTERVIEW · Main | SAC:138; DPPE:250; LIO:687; CAT:1058; LPE:277, 296; A710:1083 |
| T-21 | BLUF (in `m10_workplace_presentations`) | Framework | A710:1106-1128 | Put the decision or request in the first sentence, then the supporting detail | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-STRUCT* (else TPL-ANSWER) · TRK-WORK · Main | ACT:184; LPE:268; A710:1175, 1221, 1473-1495 |
| T-22 | SBI (in `m10_giving_feedback`) | Framework | A710:1381-1403 | Give feedback by naming the situation, the specific behaviour and its impact, then agreeing a next step | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-STRUCT* (R2 TPL-ROLE for dialogue) · TRK-WORK · Main | LPE:244, 269 |
| T-23 | "Baseline + Cluster + Context" (in `m08_reading_body_language`) | Framework | A710:638-658 | Read body language by comparing with the person's usual behaviour, looking for several signals together and considering the setting | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-ANSWER (explain an interpretation) + self-check · TRK-WORK · Practice only | — |
| T-24 | Present → Past → Future (in `m10_self_introduction`) | Framework | A710:1060-1082 | Answer "tell me about yourself" in three moves: what you do now, how you got here, what you want next | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-STRUCT* (else TPL-ANSWER) · TRK-INTERVIEW · Main | Onboarding baseline `onb-intro` (SRS-FR-ONB-009) is a natural place to teach it |
| T-25 | Pause → Clarify/Bridge → Concise Answer (in `m09_unexpected_questions`) | Framework | A710:940-962 | Handle a surprise question by pausing, clarifying or bridging, then answering briefly | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-ANSWER · TRK-PRESENT, TRK-INTERVIEW · Main | — |
| T-26 | Validate → Clarify Root Concern → Reframe Value → Next Step (in `m10_objection_handling`) | Framework | A710:1289-1311 | Handle pushback by acknowledging it, finding the real concern, reframing the value and proposing a next step | "SpeakCraft Core Curriculum" | NONE | No | Low | R2 TPL-ROLE; R1 TPL-ANSWER · TRK-WORK · R2 | A710:1266 |
| T-27 | Hear → Paraphrase → Add Value → Confirm Next Step (in `m06_listen_and_respond`) | Framework | A46:1086-1108 | Close the listening loop: hear the whole point, paraphrase it, add your contribution, confirm the next step | "SpeakCraft Core Curriculum" | NONE | No | Low | R2 TPL-ROLE · TRK-EVERYDAY, TRK-WORK · R2 | A46:956, 1064; A46:387; A710:1427 |
| T-28 | Four-beat story arc and "but / therefore" links (`m07_story_structure`, `m07_story_progression`) | Framework | A710:96-119, 166-188 | Give a story a clear shape — setting, problem, turning point, lesson — and link events with "but" and "so" instead of "and then" | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-STORY · TRK-PRESENT · Main | A710:143, 189, 397, 420 |
| T-29 | Hook → three pillars → action close; "Rule of 3" | Framework | CAT:973 (`pub_structure`); A710:779 (`m09_main_message_structure`) | Organise a talk around one core message, three supporting points and a clear call to action | CAT:973 cites `https://www.youtube.com/@askvinh`; A710 "SpeakCraft Core Curriculum" | NONE for the technique (the module row X-018 is CHANNEL_ROOT_ONLY) | No | Low | TPL-STRUCT* (else TPL-ANSWER) · TRK-PRESENT · Main | DPPE:260; CAT:897 (C-10); A13:236; A46:294; A710:710 |
| T-30 | Opening hooks and memorable closes ("Pattern Interrupt Hook") | Technique | CAT:972 (`pub_opening`); A710:756, 802; A46:340 | Open with a question, a contrast or a short moment instead of a self-introduction, and end with a summary, a callback and a next step | CAT:972 cites the channel root; activities "SpeakCraft Core Curriculum" | NONE for the technique (module row X-017 is CHANNEL_ROOT_ONLY) | No | Low | TPL-ANSWER (short: first or last 15–30 s) · TRK-PRESENT · Main | LIO:725; FTR:264-274 |
| T-31 | Appropriate vulnerability (`m05_appropriate_vulnerability`) | Technique | A46:594-616 | Build trust by briefly and calmly sharing a past difficulty you have already resolved and what you learned | "SpeakCraft Core Curriculum" | NONE | No | Low — uses a stock phrase of unclear origin (A46:594) | TPL-ANSWER + self-check · TRK-EVERYDAY · Practice only | — |
| T-32 | "Last Word, Last Letter Game" (`m06_last_word_last_letter_game`) | Exercise-technique | A46:889-911 | Show you heard the whole sentence by starting your reply with the other person's last word (or its last letter) | "SpeakCraft Core Curriculum" | NONE | No | Low | R2 TPL-ROLE (partner line by TTS) · TRK-EVERYDAY · R2 | — |
| T-33 | Three follow-up questions / conversation threads | Technique | A46:527-549 (`m05_three_follow_up_questions`) | Go deeper by asking up to three follow-up questions on one thread before changing topic | "SpeakCraft Core Curriculum" | NONE | No | Low | R2 TPL-ROLE; R1 TPL-ANSWER ("say three follow-up questions") · TRK-EVERYDAY · R2 | A46:483, 504, 550, 912; CAT:980, 982 |
| T-34 | Speaking from anchors, not a script (`m09_speaking_without_scripts`) | Technique | A710:733-755 | Rehearse a few anchor points instead of a word-for-word script so you can speak naturally and recover if you lose your place | "SpeakCraft Core Curriculum"; CAT:971 cites the channel root | NONE | No | Low | TPL-ANSWER · TRK-PRESENT · Main | CAT:971 (X-016); FTR:274 |
| T-35 | Physiological sigh and service mindset (`m09_nervousness_management`) | Technique | A710:1010-1036 | Calm pre-speech nerves with slow breathing and by moving attention from yourself to what the listener needs | "SpeakCraft Core Curriculum" | NONE | No | Low | Self-check + TPL-READ (calm opening line) · TRK-PRESENT, TRK-INTERVIEW · Practice only | Possibly overlaps T-15 "nervousness tools" |
| T-36 | Shadowing with stress, pitch and pause markers | Technique | CAT:927-958 (scripts); LAT:332-421 | Copy a model speaker's rhythm, stress and pauses line by line to absorb natural pacing | Script sources "5 Vocal Foundations — @askvinh", "Answer Any Question — @askvinh", "3 Ways To Tell Stories — @askvinh" · channel root (CAT:931-952) | CHANNEL_ROOT_ONLY | Yes — CAT:931, 941, 951, 994, 1024, 1028; FTR:230-233; LAT:385, 461; DFT:229 | High (scripts S-01, S-03 presented as the creator's delivery) | TPL-REPEAT · TRK-CLEAR, TRK-EVERYDAY · Warm-up | DPPE:174; LFE:84; RE:107-112; CAT:994 |
| T-37 | Vent / explore / fix intent (`m06_conversational_intent`) | Technique | A46:1043-1063 | Work out whether the speaker wants to be heard, to think aloud with you or to get a solution, and respond to that need | "SpeakCraft Core Curriculum" | NONE | No | Low | TPL-ANSWER + self-check · TRK-EVERYDAY, TRK-WORK · Practice only | — |

**Findings on techniques (VERIFIED unless marked).**

1. The 12 "VERIFIED" seeds carry invented-looking video IDs (`askvinh_3vs`, `askvinh_cork`, …; CAT:190…432) that are not YouTube IDs, and channel-root URLs; the label "VERIFIED" is not supported (data_learning_audit §2.3).
2. Every seed mixes creator attribution (`creatorAdvice`, `originalContext`) with an "AI" supplement (`aiExplanation`); the generated lessons concatenate both with the tag "[AI Coaching Note]" (CAT:533). Under D-08 the attribution split goes; one original text remains.
3. Nine of the 22 frameworks from the activity catalogue are acronyms or named methods that are also taught widely outside the channel (PREP, STAR, BLUF, SBI, FORD, CCC, High-Low-Buffalo, VAKS, Match & Mirror) (INFERRED). Their correct `origin` (CHANNEL_DERIVED vs ORIGINAL) can only be set after checking whether a specific channel video is what we re-express (§8 Q-01).
4. Names that look coined by the creator and must not be displayed under D-08 (RECOMMENDATION, to be confirmed by the reviewer): "3Vs", "5 Vocal Foundations", "3-Stage Self-Awareness Audit", "Power Zone", "Friendly Authority", "Vocal Image", "4 Ways of Listening", "4-Color Communicator". ARC-03 §6.8 already keeps the taught name only in `provenance.techniqueNameAsTaught`.

### 3.2 Exercises, drills, prompts and scenarios outside the activity catalogue (CNT-X-001…049)

| CNT-ID | Name as in code | Type | Location | What it teaches (our words) | Source cited in code | Provenance | Creator name exposed | Verbatim risk | Proposed mapping | Duplicates / notes |
|---|---|---|---|---|---|---|---|---|---|---|
| X-001 | "/ɪ/ vs /iː/ (ship vs sheep)" (`pron_pair_ship_sheep`) | Drill (minimal pair) | CAT:917 | Tell the short and long "i" vowels apart in a practice sentence | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | A13:468, 760. Caveat: recognition may "correct" the word, so only "recognised as intended" can be shown (ARC-05 §9 caveat) |
| X-002 | "/θ/ vs /s/ (think vs sink)" | Drill (minimal pair) | CAT:918 | Contrast the "th" sound with "s" | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | A13:617 |
| X-003 | "/v/ vs /w/ (vest vs west)" | Drill (minimal pair) | CAT:919 | Contrast "v" (teeth on lip) with "w" (rounded lips) | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | A13:617 |
| X-004 | "/r/ vs /l/ Clarity" | Drill | CAT:920 | Contrast "r" and "l" in a short sentence | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | A13:545, 617 |
| X-005 | "Multi-Syllable Executive Stress" | Drill (word stress) | CAT:921 | Place stress on the right syllable, including words whose stress changes the meaning | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | A13:666. Stress itself is not measurable (ARC-05 §6 removed metrics) |
| X-006 | "Contrastive Emphasis" | Drill (sentence stress) | CAT:922 | Change which word you stress to change the meaning of the same sentence | — | NONE | No | Low | TPL-READ · TRK-CLEAR · Warm-up | A13:690. Emphasis not measurable |
| X-007 | "Authority Downward Drop vs Question Rise" | Drill (intonation) | CAT:923 | Let statements fall and questions rise | — | NONE | No | Low | TPL-READ · TRK-CLEAR, TRK-WORK · Warm-up | T-12; A13:975. Falling/rising endings not measurable (gap G-02) |
| X-008 | "Linking Consonants & Reductions" | Drill (connected speech) | CAT:924 | Join a final consonant to the next vowel so speech flows | — | NONE | No | Low | TPL-REPEAT · TRK-CLEAR · Warm-up | — |
| X-009 | "Tenses in Storytelling" (`gram_tenses`) | Exercise (spoken grammar) | CAT:961 | Use past simple for events and past continuous for the background | — | NONE | No | Low | TPL-STORY · TRK-EVERYDAY · Main | Feedback only if a qualified tense rule exists (ARC-05 §8.1) |
| X-010 | "Articles (a / an / the)" | Exercise | CAT:962 | Use "a/an" for something new and "the" once both people know it | — | NONE | No | Low | TPL-ANSWER · TRK-EVERYDAY · Main | Article rules are expected SPK-02 survivors (ARC-05 §8.1) |
| X-011 | "Prepositions of Time & Place" | Exercise | CAT:963 | Choose "at", "on" and "in" for times and places | — | NONE | No | Low | TPL-ANSWER · TRK-EVERYDAY · Main | Preposition rules not listed as expected survivors |
| X-012 | "Subject-Verb Agreement" | Exercise | CAT:964 | Keep the verb singular when a long phrase separates it from a singular subject | — | NONE | No | Low | TPL-ANSWER · TRK-WORK · Main | Agreement rules are expected survivors |
| X-013 | "Natural Question Formation" | Exercise | CAT:965 | Use statement word order inside polite indirect questions | — | NONE | No | Low | TPL-ANSWER · TRK-INTERVIEW · Main | — |
| X-014 | "Conditionals for Problem-Solving" | Exercise | CAT:966 | Propose an improvement tactfully with a hypothetical "if … would" sentence | — | NONE | No | Low | TPL-ANSWER · TRK-WORK · Main | — |
| X-015 | "Complex Sentences With Signposting" | Exercise | CAT:967 | Link cause and contrast with "although", "because" and "as a result" | — | NONE | No | Low | TPL-ANSWER · TRK-WORK · Main | — |
| X-016 | "1. Speech Preparation & Audience Calibration" (`pub_prep`; framework "CLEAR & 3Vs") | Activity (presentation phase) | CAT:971 | Prepare three main points instead of a word-for-word script and outline a talk aloud | `https://www.youtube.com/@askvinh` (CAT:971) | CHANNEL_ROOT_ONLY | Yes — channel URL; FTR:266 | Low | TPL-ANSWER · TRK-PRESENT · Main | T-34, T-05, T-01 |
| X-017 | "2. Magnetic Opening Techniques" (`pub_opening`; "Pattern Interrupt Hook") | Activity | CAT:972 | Record only the first 15 seconds of a talk with a bold opening question | channel root (CAT:972) | CHANNEL_ROOT_ONLY | Yes — URL | Low | TPL-ANSWER (short) · TRK-PRESENT · Main | T-30; A710:756 |
| X-018 | "3. Structuring Ideas (PREP & Rule of 3)" (`pub_structure`) | Activity | CAT:973 | Pitch a three-point summary of a book or tool | channel root (CAT:973) | CHANNEL_ROOT_ONLY | Yes — URL | Low | TPL-STRUCT* · TRK-PRESENT · Main | T-29, T-04 |
| X-019 | "4. Reliving Stories With Dialogue" (`pub_story`) | Activity | CAT:974 | Record a one-minute story that includes at least one spoken quote | channel root (CAT:974) | CHANNEL_ROOT_ONLY | Yes — URL | Low | TPL-STORY · TRK-PRESENT · Main | T-08 |
| X-020 | "5. Voice Modulation & Diaphragm Projection" (`pub_voice`) | Activity | CAT:975 | Read a call to action while alternating speed, loudness and pitch | channel root (CAT:975) | CHANNEL_ROOT_ONLY | Yes — URL; framework label names the creator's term (CAT:975) | Low | TPL-READ · TRK-PRESENT · Warm-up | T-02 |
| X-021 | "6. Power Zone Body Language & Closing" (`pub_body`) | Activity | CAT:976 | Give a 30-second closing line and then hold two seconds of silence | channel root (CAT:976) | CHANNEL_ROOT_ONLY | Yes — URL; "Power Zone" | Low | TPL-ANSWER (audio) + self-check (gestures) · TRK-PRESENT · Main | T-11, T-30 |
| X-022 | Real-world task: start a 2-minute conversation, ask 2 follow-ups | Activity (outside app) | CAT:980 | Use follow-up questions in a real conversation | Logged with channel-root URL (REPO:1006) | CHANNEL_ROOT_ONLY | Yes — LAT:927 header; REPO:1006 | Low | Outside app; R1 has no real-world task type → retire or keep as a tip (RECOMMENDATION) · TRK-EVERYDAY · No | T-33 |
| X-023 | Real-world task: explain your work with CLEAR | Activity (outside app) | CAT:981 | Explain your work to a non-expert in real life | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | as X-022 · TRK-WORK · No | T-05 |
| X-024 | Real-world task: ask three open questions using the listening framework | Activity (outside app) | CAT:982 | Practise open questions and listening in a meeting | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | as X-022 · TRK-WORK · No | T-10, T-33 |
| X-025 | Real-world task: 90-second update with PREP | Activity (outside app) | CAT:983 | Use PREP in a real update | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | as X-022 · TRK-WORK · No | T-04 |
| X-026 | Real-world task: summarise a meeting in 60 s without fillers | Activity (outside app) | CAT:984 | Summarise aloud without fillers | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | Could become an in-app TPL-ANSWER · TRK-WORK · Main (if converted) | T-06 |
| X-027 | Real-world task: explain a problem and its fix without jargon | Activity (outside app) | CAT:985 | Explain a problem and solution plainly | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | Could become TPL-ANSWER · TRK-WORK · Main (if converted) | T-05 |
| X-028 | Real-world task: record a 60-second self-introduction and audit it | Activity | CAT:986 | Record a professional introduction and review it in passes | as X-022 | CHANNEL_ROOT_ONLY | Yes | Low | TPL-ANSWER (= `onb-intro` style) · TRK-INTERVIEW · Main | T-03, T-24 |
| X-029 | "Elevator Pitch" preset | Prompt | SAC:137 | Introduce yourself and one communication strength | — (mentions "3Vs") | NONE | No | Low | TPL-ANSWER · TRK-WORK · Main | T-01 |
| X-030 | "STAR Interview" preset | Prompt | SAC:138 | Describe a project conflict and its resolution with STAR | — | NONE | No | Low | TPL-STRUCT (STAR) · TRK-INTERVIEW · Main | T-20 |
| X-031 | "Team Update" preset | Prompt | SAC:139 | Give a 45-second status update with a headline and next step | — | NONE | No | Low | TPL-STRUCT* (BLUF) · TRK-WORK · Main | T-21 |
| X-032 | "Story Hook" preset | Prompt | SAC:140 | Tell a one-minute personal story with pauses and vocal contrast | — | NONE | No | Low | TPL-STORY · TRK-PRESENT · Main | T-08, T-02 |
| X-033 | "60s Silent-Pause Retake" | Micro-drill (generated from the user's topic) | DLCE:544 | Re-explain the same point, replacing each detected filler with a calm breath | — | NONE | No | Low | Focus/retry step (SRS-FR-TODAY-004) on TPL-ANSWER · any · Main (retry) | T-06 |
| X-034 | "Slow-Landing Drill" | Micro-drill | DLCE:549 | Repeat the last two sentences more slowly with a pause before the takeaway | — | NONE | No | Low | Focus/retry step · any · Main (retry) | T-02, T-12 |
| X-035 | "Story Anchor Drill" | Micro-drill | DLCE:555 | Re-explain a topic starting from a concrete past moment | — | NONE | No | Low | Focus/retry step (TPL-STORY) · TRK-PRESENT · Main (retry) | T-08 |
| X-036 | "Vocal Melody Upgrade" | Micro-drill | DLCE:557 | Re-record with a rising opening question and a falling finish | — | NONE | No | Low | Focus/retry step · TRK-CLEAR · Main (retry) | T-12; not measurable (G-01, G-02) |
| X-037 | "Casual conversations" | Scenario | CAT:901; opener ICVM:199 (generic) | Everyday chat | — | NONE | No | Low | R2 TPL-ROLE; R1 TPL-ANSWER to a fixed opener · TRK-EVERYDAY · R2 | A13:372 |
| X-038 | "Introducing yourself" | Scenario | CAT:902; ICVM:199 | Self-introduction | — | NONE | No | Low | as X-037 · TRK-EVERYDAY, TRK-INTERVIEW · R2 | A710:1060 |
| X-039 | "Talking with colleagues" | Scenario | CAT:903; ICVM:199 | Peer coordination | — | NONE | No | Low | as X-037 · TRK-WORK · R2 | A710:1198 |
| X-040 | "Talking with managers" | Scenario | CAT:904; opener ICVM:198 | One-to-one with a manager | — | NONE | No | Low | as X-037 · TRK-WORK · R2 | A710:1175 |
| X-041 | "Meeting new people" | Scenario | CAT:905; ICVM:199 | First meetings | — | NONE | No | Low | as X-037 · TRK-EVERYDAY · R2 | A46:732 |
| X-042 | "Asking questions" | Scenario | CAT:906; ICVM:199 | Asking good questions | — | NONE | No | Low | as X-037 · TRK-EVERYDAY · R2 | A46:483 |
| X-043 | "Group discussions" | Scenario | CAT:907; ICVM:199 | Contributing in a group | — | NONE | No | Low | as X-037 · TRK-WORK · R2 | A710:1152 |
| X-044 | "Job interviews" | Scenario | CAT:908; opener ICVM:196 | Behavioural interview opening | — | NONE | No | Low | as X-037 (R1: TPL-STRUCT STAR on the opener) · TRK-INTERVIEW · R2 / Main | T-20 |
| X-045 | "HR discussions" | Scenario | CAT:909; ICVM:199 | HR conversation | — | NONE | No | Low | as X-037 · TRK-INTERVIEW, TRK-WORK · R2 | — |
| X-046 | "Technical interviews" | Scenario | CAT:910; ICVM:199 | Technical walkthrough | — | NONE | No | Low | as X-037 · TRK-INTERVIEW · R2 | T-05 |
| X-047 | "Giving presentations" | Scenario | CAT:911; ICVM:199 | Presenting | — | NONE | No | Low | as X-037 · TRK-PRESENT · R2 | T-29 |
| X-048 | "Handling disagreements" | Scenario | CAT:912; opener ICVM:197 | Disagreeing constructively | — | NONE | No | Low | as X-037 · TRK-WORK · R2 | T-10, T-26 |
| X-049 | "Professional meetings" | Scenario | CAT:913; ICVM:199 | Meeting contributions | — | NONE | No | Low | as X-037 · TRK-WORK · R2 | A710:1129 |

### 3.3 Activity catalogue (CNT-X-101…250)

All 150 activities share these values, so they are stated once instead of repeated per row (VERIFIED):

| Field | Value for every activity |
|---|---|
| Source cited in code | `sourceAttribution = "SpeakCraft Core Curriculum"` (A13:92, A46:85, A710:90); `contentVersion = 1` |
| Provenance | NONE |
| Creator name exposed | No — the activity catalogue contains no creator name or handle. It does use three creator-associated terms: "Power Zone" (rubric A710:15; activities at A710:281 and A710:546-565), "5 vocal foundations" (A13:1104) and "visual, vocal, verbal" (A710:994) |
| Verbatim risk | Low — generic, templated wording (147 of 150 have exactly 3 instructions; data_learning_audit §2.1). The origin of named frameworks is unverified (§8 Q-01) |
| What it teaches | The activity title and the linked technique (§3.1) describe it; one-line rewrites are produced in the §6 workflow, not here |

Columns: exercise type in code → proposed template; proposed track; Today eligibility; linked technique (— = general activity, `general: true` in ARC-03 §6.7); notes. Mapping rules (RECOMMENDATION): READING_ALOUD and VOICE_MODULATION → TPL-READ; PRONUNCIATION_DRILL and TONGUE_TWISTERS → TPL-REPEAT; STORYTELLING → TPL-STORY; PRESENTATION_PRACTICE and INTERVIEW_SIMULATION → TPL-STRUCT\* (TPL-ANSWER until the lexicon exists); CONVERSATION_SIMULATION, ROLEPLAY and ACTIVE_LISTENING_GAME → TPL-ROLE (R2); LISTENING_COMPREHENSION → needs a "listen, then answer" step that no R1 template offers (gap, §5); BREATHING_VOCAL_WARMUP, BODY_LANGUAGE_SELF_REVIEW and REFLECTION_SELF_ASSESSMENT → self-check (no template); VIDEO_RECORDING → self-check (R1 captures audio only), optional TPL-ANSWER for the spoken part; REAL_WORLD_TASK → outside the app; all others → TPL-ANSWER. Tracks follow the module: m01, m05, m06 → TRK-EVERYDAY; m02, m03 → TRK-CLEAR; m04, m10 → TRK-WORK (three interview activities → TRK-INTERVIEW); m07–m09 → TRK-PRESENT.

**Totals of this mapping:** TPL-ANSWER 42, TPL-ROLE (R2) 24, self-check 16, TPL-READ 14, TPL-STORY 12, TPL-STRUCT\* 11, listening gap 9, TPL-REPEAT 8, video self-check 7, TPL-STRUCT (PREP) 2, TPL-STRUCT (STAR) 2, outside app 3. By track: TRK-EVERYDAY 45, TRK-PRESENT 42, TRK-WORK 32, TRK-CLEAR 28, **TRK-INTERVIEW 3**. Today: Main 69, Warm-up 22, R2 24, No 26, No (listening gap) 9.

| CNT-ID | Activity id | Location | Title (as in code) | Exercise type → proposed template | Track | Today | Technique | Notes |
|---|---|---|---|---|---|---|---|---|
| CNT-X-101 | `m01_daily_english_speaking` | A13:98 | Daily English Speaking | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-102 | `m01_impromptu_speaking` | A13:121 | Impromptu Speaking | IMPROMPTU_SPEAKING → TPL-ANSWER | TRK-EVERYDAY | Main | T-04 |  |
| CNT-X-103 | `m01_random_topic_speaking` | A13:144 | Random-Topic Speaking | WORD_ASSOCIATION → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-104 | `m01_reading_aloud` | A13:168 | Reading Aloud | READING_ALOUD → TPL-READ | TRK-EVERYDAY | Warm-up | — |  |
| CNT-X-105 | `m01_expressive_reading` | A13:191 | Expressive Reading | READING_ALOUD → TPL-READ | TRK-EVERYDAY | Warm-up | T-02 | Pitch/pause contrast needs ARC-05 candidate metrics |
| CNT-X-106 | `m01_sentence_construction` | A13:214 | Sentence Construction | SENTENCE_CONSTRUCTION → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-107 | `m01_spontaneous_speaking` | A13:236 | Spontaneous Speaking | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | T-29 |  |
| CNT-X-108 | `m01_english_recording` | A13:259 | English Recording | AUDIO_RECORDING → TPL-ANSWER | TRK-EVERYDAY | Main | T-03 | Baseline recording |
| CNT-X-109 | `m01_speech_transcription` | A13:282 | Speech Transcription | GRAMMAR_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | T-03 | Transcript review pass |
| CNT-X-110 | `m01_sentence_clarity` | A13:305 | Sentence Clarity | GRAMMAR_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-111 | `m01_simple_explanations` | A13:326 | Simple Explanations | VOCABULARY_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | T-05 | Duplicate title with m04_simple_explanations |
| CNT-X-112 | `m01_complete_thought_speaking` | A13:349 | Complete-Thought Speaking | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | T-12 |  |
| CNT-X-113 | `m01_everyday_conversations` | A13:372 | Everyday Conversations | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | — |  |
| CNT-X-114 | `m01_daily_activity_descriptions` | A13:395 | Daily Activity Descriptions | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-115 | `m01_english_thinking_practice` | A13:418 | English Thinking Practice | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-116 | `m02_mouth_movement_exercises` | A13:444 | Mouth-Movement Exercises | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-07 | Non-speaking warm-up; self-check |
| CNT-X-117 | `m02_pronunciation_drills` | A13:468 | Pronunciation Drills | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 | Minimal pairs: recogniser may auto-correct; only "recognised as intended" |
| CNT-X-118 | `m02_articulation_exercises` | A13:494 | Articulation Exercises | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 |  |
| CNT-X-119 | `m02_lip_trills` | A13:520 | Lip Trills | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-07 | Non-speaking warm-up; self-check |
| CNT-X-120 | `m02_tongue_warmups` | A13:545 | Tongue Warm-Ups | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-07 | Non-speaking warm-up; self-check |
| CNT-X-121 | `m02_jaw_relaxation` | A13:569 | Jaw Relaxation | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-07 | Non-speaking warm-up; self-check |
| CNT-X-122 | `m02_mouth_opening_exercises` | A13:593 | Mouth-Opening Exercises | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 |  |
| CNT-X-123 | `m02_difficult_sound_practice` | A13:617 | Difficult Sound Practice | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 |  |
| CNT-X-124 | `m02_tongue_twisters` | A13:642 | Tongue Twisters | TONGUE_TWISTERS → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 |  |
| CNT-X-125 | `m02_word_stress` | A13:666 | Word Stress | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | — | Stress not measurable (ARC-05 §6 removed metric) |
| CNT-X-126 | `m02_sentence_stress` | A13:690 | Sentence Stress | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Emphasis not measurable |
| CNT-X-127 | `m02_accent_awareness` | A13:713 | Accent Awareness | REFLECTION_SELF_ASSESSMENT → Self-check (no template) | TRK-CLEAR | No | — | Reflection |
| CNT-X-128 | `m02_consonant_clarity` | A13:736 | Consonant Clarity | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | T-07 |  |
| CNT-X-129 | `m02_vowel_pronunciation` | A13:760 | Vowel Pronunciation | PRONUNCIATION_DRILL → TPL-REPEAT | TRK-CLEAR | Warm-up | — |  |
| CNT-X-130 | `m02_recording_comparisons` | A13:784 | Recording Comparisons | AUDIO_RECORDING → TPL-ANSWER | TRK-CLEAR | Main | T-03 | Before/after compare (SCR-PROG-04) |
| CNT-X-131 | `m03_vocal_warmups` | A13:810 | Vocal Warm-Ups | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-02 | Non-speaking warm-up; self-check |
| CNT-X-132 | `m03_breathing_practice` | A13:835 | Breathing Practice | BREATHING_VOCAL_WARMUP → Self-check (no template) | TRK-CLEAR | No | T-02 | Non-speaking; self-check |
| CNT-X-133 | `m03_volume_control` | A13:860 | Volume Control | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Needs candidate "volume variation" (ARC-05 §8.4) |
| CNT-X-134 | `m03_pitch_variation` | A13:883 | Pitch Variation | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Needs candidate "pitch range" (ARC-05 §8.4) |
| CNT-X-135 | `m03_vocal_melody` | A13:906 | Vocal Melody | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Needs candidate "pitch range" |
| CNT-X-136 | `m03_pause_practice` | A13:929 | Pause Practice | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-06 |  |
| CNT-X-137 | `m03_slow_speaking` | A13:952 | Slow Speaking | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Code targets 110–125 wpm |
| CNT-X-138 | `m03_sentence_endings` | A13:975 | Sentence Endings | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-12 | Falling endings not measurable (gap G-02) |
| CNT-X-139 | `m03_voice_projection` | A13:998 | Voice Projection | VOICE_MODULATION → TPL-READ | TRK-PRESENT | Warm-up | T-02 | Absolute loudness not measurable |
| CNT-X-140 | `m03_emotional_expression` | A13:1021 | Emotional Expression | VOICE_MODULATION → TPL-READ | TRK-CLEAR | Warm-up | T-02 | Self-check |
| CNT-X-141 | `m03_filler_word_reduction` | A13:1044 | Filler-Word Reduction | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-CLEAR | Main | T-06 |  |
| CNT-X-142 | `m03_vocal_image` | A13:1067 | Vocal Image | REFLECTION_SELF_ASSESSMENT → Self-check (no template) | TRK-CLEAR | No | T-01 | Reflection; self-check |
| CNT-X-143 | `m03_voice_recording_analysis` | A13:1090 | Voice Recording Analysis | AUDIO_RECORDING → TPL-ANSWER | TRK-CLEAR | Main | T-02 | Example names the creator's term "5 vocal foundations" (A13:1104) |
| CNT-X-144 | `m03_expressive_delivery` | A13:1113 | Expressive Delivery | SPEAKING_EXERCISE → TPL-ANSWER | TRK-PRESENT | Main | T-02 |  |
| CNT-X-145 | `m03_voice_confidence` | A13:1136 | Voice Confidence | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-CLEAR | Main | T-02 |  |
| CNT-X-146 | `m04_intentional_pauses` | A46:91 | Intentional Pauses | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-06 | Pause placement gap G-05 |
| CNT-X-147 | `m04_speaking_slowly_emphasis` | A46:114 | Speaking Slowly for Emphasis | VOICE_MODULATION → TPL-READ | TRK-WORK | Warm-up | T-02 | Pace contrast gap G-04 |
| CNT-X-148 | `m04_declarative_statements` | A46:137 | Declarative Statements | SENTENCE_CONSTRUCTION → TPL-ANSWER | TRK-WORK | Main | T-12 | M-HEDGE applies |
| CNT-X-149 | `m04_complete_thought_practice` | A46:158 | Complete-Thought Practice | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-12 |  |
| CNT-X-150 | `m04_ccc_framework` | A46:181 | CCC Framework | SPEAKING_EXERCISE → TPL-STRUCT* (else TPL-ANSWER) | TRK-WORK | Main | T-16 | Named framework CCC; one step is labelled "CLEAR:" (A46:191) |
| CNT-X-151 | `m04_analogies` | A46:204 | Analogies | VOCABULARY_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-05 |  |
| CNT-X-152 | `m04_simple_explanations` | A46:227 | Simple Explanations | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-05 | Duplicate title with m01_simple_explanations |
| CNT-X-153 | `m04_concise_speaking` | A46:250 | Concise Speaking | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-WORK | Main | T-16 |  |
| CNT-X-154 | `m04_removing_unnecessary_words` | A46:273 | Removing Unnecessary Words | GRAMMAR_EXERCISE → TPL-ANSWER | TRK-WORK | Main | — |  |
| CNT-X-155 | `m04_organizing_thoughts` | A46:294 | Organizing Thoughts | IMPROMPTU_SPEAKING → TPL-ANSWER | TRK-WORK | Main | T-29 |  |
| CNT-X-156 | `m04_logical_explanations` | A46:317 | Logical Explanations | QUESTION_AND_ANSWER → TPL-STRUCT (PREP) | TRK-WORK | Main | T-04 |  |
| CNT-X-157 | `m04_strong_openings_conclusions` | A46:340 | Strong Openings and Conclusions | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-30 |  |
| CNT-X-158 | `m04_structured_answers` | A46:363 | Structured Answers | QUESTION_AND_ANSWER → TPL-STRUCT (PREP) | TRK-WORK | Main | T-04 | Named framework PREP |
| CNT-X-159 | `m04_summarization` | A46:387 | Summarization | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-WORK | No (gap) | T-27 | Needs a spoken passage (TTS) |
| CNT-X-160 | `m04_technical_explanations` | A46:411 | Technical Explanations | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | T-05 | Duplicate title with m10_technical_explanations |
| CNT-X-161 | `m05_ford_framework` | A46:437 | FORD Framework | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-17 | Named framework FORD |
| CNT-X-162 | `m05_high_low_buffalo` | A46:460 | High-Low-Buffalo Framework | SPEAKING_EXERCISE → TPL-STRUCT* (else TPL-ANSWER) | TRK-EVERYDAY | Main | T-13 | Named framework; full steps here although CAT:453 marks it "pending" |
| CNT-X-163 | `m05_open_ended_questions` | A46:483 | Open-Ended Questions | SENTENCE_CONSTRUCTION → TPL-ANSWER | TRK-EVERYDAY | Main | T-33 |  |
| CNT-X-164 | `m05_follow_up_questions` | A46:504 | Follow-Up Questions | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-33 |  |
| CNT-X-165 | `m05_three_follow_up_questions` | A46:527 | Three-Follow-Up-Questions Exercise | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-33 |  |
| CNT-X-166 | `m05_conversation_thread_exploration` | A46:550 | Conversation-Thread Exploration | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-EVERYDAY | Main | T-33 |  |
| CNT-X-167 | `m05_personal_experience_sharing` | A46:571 | Personal Experience Sharing | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | — |  |
| CNT-X-168 | `m05_appropriate_vulnerability` | A46:594 | Appropriate Vulnerability | REFLECTION_SELF_ASSESSMENT → Self-check (no template) | TRK-EVERYDAY | No | T-31 | Stock phrase of unclear origin (A46:594) |
| CNT-X-169 | `m05_common_ground_discovery` | A46:617 | Common-Ground Discovery | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | — |  |
| CNT-X-170 | `m05_match_and_mirror` | A46:640 | Match and Mirror | ROLEPLAY → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-18 | Named technique "Match & Mirror" |
| CNT-X-171 | `m05_entering_conversations` | A46:663 | Entering Conversations | ROLEPLAY → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | — |  |
| CNT-X-172 | `m05_ending_conversations` | A46:686 | Ending Conversations | ROLEPLAY → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | — |  |
| CNT-X-173 | `m05_small_talk` | A46:709 | Small Talk | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-17 |  |
| CNT-X-174 | `m05_first_impressions` | A46:732 | First Impressions | SPEAKING_EXERCISE → TPL-ANSWER | TRK-EVERYDAY | Main | T-01 | Eye contact and smile: self-check |
| CNT-X-175 | `m05_rapport_building` | A46:755 | Rapport Building | REAL_WORLD_TASK → Outside app (no template) | TRK-EVERYDAY | No | T-10 | Real-world task |
| CNT-X-176 | `m06_active_listening` | A46:780 | Active Listening | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-177 | `m06_subtext_listening` | A46:802 | Subtext Listening | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-178 | `m06_visual_listening` | A46:824 | Visual Listening | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-EVERYDAY | Main | T-10 | Visual cues: self-check |
| CNT-X-179 | `m06_auditory_listening` | A46:845 | Auditory Listening | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-180 | `m06_emotional_listening` | A46:867 | Emotional Listening | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-181 | `m06_last_word_last_letter_game` | A46:889 | Last Word, Last Letter Game | ACTIVE_LISTENING_GAME → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-32 |  |
| CNT-X-182 | `m06_follow_up_listening_game` | A46:912 | Follow-Up Listening Game | ACTIVE_LISTENING_GAME → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-33 |  |
| CNT-X-183 | `m06_listening_without_interruption` | A46:934 | Listening Without Interruption | REAL_WORLD_TASK → Outside app (no template) | TRK-EVERYDAY | No | T-10 | Real-world task |
| CNT-X-184 | `m06_reflective_responses` | A46:956 | Reflective Responses | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-27 | Needs a spoken passage (TTS) |
| CNT-X-185 | `m06_reading_the_room` | A46:978 | Reading the Room | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-EVERYDAY | Main | T-10 |  |
| CNT-X-186 | `m06_emotion_identification` | A46:999 | Emotion Identification | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-187 | `m06_vocal_tone_recognition` | A46:1021 | Vocal Tone Recognition | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-10 | Needs a spoken passage (TTS) |
| CNT-X-188 | `m06_conversational_intent` | A46:1043 | Conversational Intent | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-EVERYDAY | Main | T-37 |  |
| CNT-X-189 | `m06_listen_and_summarize` | A46:1064 | Listen and Summarize | LISTENING_COMPREHENSION → TPL-ANSWER after TTS passage (gap) | TRK-EVERYDAY | No (gap) | T-27 | Needs a spoken passage (TTS) |
| CNT-X-190 | `m06_listen_and_respond` | A46:1086 | Listen and Respond | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-EVERYDAY | R2 | T-27 |  |
| CNT-X-191 | `m07_story_structure` | A710:96 | Story Structure | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-28 |  |
| CNT-X-192 | `m07_situation_and_context` | A710:120 | Situation and Context | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-08 |  |
| CNT-X-193 | `m07_challenge_and_conflict` | A710:143 | Challenge and Conflict | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-28 |  |
| CNT-X-194 | `m07_story_progression` | A710:166 | Story Progression | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-28 | "and then" chain rule = gap G-11 |
| CNT-X-195 | `m07_outcome_and_lesson` | A710:189 | Outcome and Lesson | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-28 |  |
| CNT-X-196 | `m07_vaks_framework` | A710:212 | VAKS Framework | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-19 | Named framework VAKS |
| CNT-X-197 | `m07_facial_expression_storytelling` | A710:235 | Facial-Expression Storytelling | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-01 | Video; self-check |
| CNT-X-198 | `m07_gesture_storytelling` | A710:260 | Gesture Storytelling | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-11 | Video; checklist uses "Power Zone" (A710:281) |
| CNT-X-199 | `m07_vocal_melody_storytelling` | A710:284 | Vocal Melody Storytelling | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-08 |  |
| CNT-X-200 | `m07_suspense_and_pauses` | A710:307 | Suspense and Pauses | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-02 |  |
| CNT-X-201 | `m07_everyday_stories` | A710:330 | Everyday Stories | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-08 |  |
| CNT-X-202 | `m07_concrete_descriptions` | A710:353 | Concrete Descriptions | VOCABULARY_EXERCISE → TPL-ANSWER | TRK-PRESENT | Main | T-19 |  |
| CNT-X-203 | `m07_audience_connection` | A710:374 | Audience Connection | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | — |  |
| CNT-X-204 | `m07_short_storytelling` | A710:397 | Short Storytelling | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-PRESENT | Main | T-28 |  |
| CNT-X-205 | `m07_long_storytelling` | A710:420 | Long Storytelling | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-28 | 2-minute story vs 120 s cap (SRS-FR-SESS-003) |
| CNT-X-206 | `m08_posture` | A710:447 | Posture | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-207 | `m08_eye_contact` | A710:472 | Eye Contact | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-208 | `m08_facial_expressions` | A710:496 | Facial Expressions | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-209 | `m08_facial_gestures` | A710:520 | Facial Gestures | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-210 | `m08_hand_gestures` | A710:544 | Hand Gestures | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-11 | Video; "Power Zone" in text (A710:546-565) |
| CNT-X-211 | `m08_body_language_mirroring` | A710:568 | Body-Language Mirroring | REAL_WORLD_TASK → Outside app (no template) | TRK-PRESENT | No | T-18 | Real-world task |
| CNT-X-212 | `m08_physical_presence` | A710:590 | Physical Presence | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-213 | `m08_gesture_control` | A710:614 | Gesture Control | BODY_LANGUAGE_SELF_REVIEW → Self-check (no template) | TRK-PRESENT | No | T-11 | Video; self-check only |
| CNT-X-214 | `m08_reading_body_language` | A710:638 | Reading Body Language | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-PRESENT | Main | T-23 | "Baseline + Cluster + Context" (A710:646) |
| CNT-X-215 | `m08_video_call_presence` | A710:659 | Video-Call Presence | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-01 | Video; self-check only |
| CNT-X-216 | `m09_camera_speaking` | A710:686 | Camera Speaking | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-01 | Video; self-check |
| CNT-X-217 | `m09_presentation_rehearsal` | A710:710 | Presentation Rehearsal | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-PRESENT | Main | T-29 |  |
| CNT-X-218 | `m09_speaking_without_scripts` | A710:733 | Speaking Without Scripts | IMPROMPTU_SPEAKING → TPL-ANSWER | TRK-PRESENT | Main | T-34 |  |
| CNT-X-219 | `m09_strong_openings` | A710:756 | Strong Openings | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-PRESENT | Main | T-30 |  |
| CNT-X-220 | `m09_main_message_structure` | A710:779 | Main-Message Structure | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-PRESENT | Main | T-29 |  |
| CNT-X-221 | `m09_memorable_conclusions` | A710:802 | Memorable Conclusions | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-PRESENT | Main | T-30 |  |
| CNT-X-222 | `m09_pause_emphasis` | A710:825 | Pause Emphasis | VOICE_MODULATION → TPL-READ | TRK-PRESENT | Warm-up | T-02 | Pause placement gap G-05 |
| CNT-X-223 | `m09_vocal_volume` | A710:848 | Vocal Volume | VOICE_MODULATION → TPL-READ | TRK-PRESENT | Warm-up | T-02 | Needs candidate "volume variation" |
| CNT-X-224 | `m09_presentation_storytelling` | A710:871 | Presentation Storytelling | STORYTELLING → TPL-STORY | TRK-PRESENT | Main | T-08 |  |
| CNT-X-225 | `m09_one_to_one_speaking` | A710:894 | One-to-One Speaking | ROLEPLAY → TPL-ROLE (R2) | TRK-PRESENT | R2 | — |  |
| CNT-X-226 | `m09_group_speaking` | A710:917 | Group Speaking | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-PRESENT | Main | T-01 | Room scanning: self-check |
| CNT-X-227 | `m09_unexpected_questions` | A710:940 | Unexpected Questions | QUESTION_AND_ANSWER → TPL-ANSWER | TRK-PRESENT | Main | T-25 |  |
| CNT-X-228 | `m09_recovering_after_mistakes` | A710:963 | Recovering After Mistakes | AI_EVALUATED_CHALLENGE → TPL-ANSWER | TRK-PRESENT | Main | — |  |
| CNT-X-229 | `m09_stage_rehearsal` | A710:986 | Stage Rehearsal | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-PRESENT | No | T-01 | Video; "visual, vocal, verbal" (A710:994) |
| CNT-X-230 | `m09_nervousness_management` | A710:1010 | Nervousness Management | REFLECTION_SELF_ASSESSMENT → Self-check (no template) | TRK-PRESENT | No | T-35 |  |
| CNT-X-231 | `m10_interview_preparation` | A710:1037 | Interview Preparation | INTERVIEW_SIMULATION → TPL-STRUCT (STAR) | TRK-INTERVIEW | Main | T-20 | Named framework STAR; multi-turn in code |
| CNT-X-232 | `m10_self_introduction` | A710:1060 | Self-Introduction | SPEAKING_EXERCISE → TPL-STRUCT* (else TPL-ANSWER) | TRK-INTERVIEW | Main | T-24 |  |
| CNT-X-233 | `m10_explaining_work_experience` | A710:1083 | Explaining Work Experience | QUESTION_AND_ANSWER → TPL-STRUCT (STAR) | TRK-INTERVIEW | Main | T-20 |  |
| CNT-X-234 | `m10_workplace_presentations` | A710:1106 | Workplace Presentations | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-WORK | Main | T-21 | Named framework BLUF |
| CNT-X-235 | `m10_meeting_participation` | A710:1129 | Meeting Participation | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | — |  |
| CNT-X-236 | `m10_team_discussions` | A710:1152 | Team Discussions | SPEAKING_EXERCISE → TPL-ANSWER | TRK-WORK | Main | — |  |
| CNT-X-237 | `m10_manager_communication` | A710:1175 | Manager Communication | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-21 |  |
| CNT-X-238 | `m10_colleague_communication` | A710:1198 | Colleague Communication | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | — |  |
| CNT-X-239 | `m10_remote_communication` | A710:1221 | Remote Communication | SENTENCE_CONSTRUCTION → TPL-ANSWER | TRK-WORK | Main | T-21 |  |
| CNT-X-240 | `m10_virtual_meetings` | A710:1242 | Virtual Meetings | VIDEO_RECORDING → Self-check (audio part TPL-ANSWER) | TRK-WORK | No | T-01 | Video; self-check |
| CNT-X-241 | `m10_customer_communication` | A710:1266 | Customer Communication | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-26 |  |
| CNT-X-242 | `m10_objection_handling` | A710:1289 | Objection Handling | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-26 |  |
| CNT-X-243 | `m10_negotiation` | A710:1312 | Negotiation | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | — |  |
| CNT-X-244 | `m10_networking` | A710:1335 | Networking | CONVERSATION_SIMULATION → TPL-ROLE (R2) | TRK-WORK | R2 | T-17 |  |
| CNT-X-245 | `m10_technical_explanations` | A710:1358 | Technical Explanations | PRESENTATION_PRACTICE → TPL-STRUCT* (else TPL-ANSWER) | TRK-WORK | Main | T-05 | Duplicate title with m04_technical_explanations |
| CNT-X-246 | `m10_giving_feedback` | A710:1381 | Giving Feedback | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-22 | Named framework SBI |
| CNT-X-247 | `m10_receiving_criticism` | A710:1404 | Receiving Criticism | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-10 |  |
| CNT-X-248 | `m10_difficult_conversations` | A710:1427 | Difficult Conversations | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-27 |  |
| CNT-X-249 | `m10_assertive_communication` | A710:1450 | Assertive Communication | ROLEPLAY → TPL-ROLE (R2) | TRK-WORK | R2 | T-12 |  |
| CNT-X-250 | `m10_executive_presence` | A710:1473 | Executive Presence | AI_EVALUATED_CHALLENGE → TPL-STRUCT* (else TPL-ANSWER) | TRK-WORK | Main | T-21 | BLUF framing (A710:1481) |

### 3.4 Challenges (CNT-C)

All ten are in `speakingChallenges` (CAT:887-898). Each carries a `sourceReference` label "@askvinh — …" with no URL, so provenance is CHANNEL_ROOT_ONLY and the creator's handle is exposed (shown on the Challenges screen, SAC:1001 header). Verbatim risk is Low for all (short generic prompts).

| CNT-ID | Name as in code | Location | Duration / cadence | What it teaches (our words) | Source cited in code | Proposed mapping | Duplicates / notes |
|---|---|---|---|---|---|---|---|
| C-01 | "1-Minute Nonstop Flow" (`chal_1min_nonstop`) | CAT:888 | 60 s · DAILY | Keep speaking for a minute, using silent pauses instead of fillers | "@askvinh — Fluency & Silence Control" | TPL-ANSWER · TRK-EVERYDAY · Main | T-06; CAT:1026 (Day 8); LIO:675 |
| C-02 | "2-Minute Daily Routine Story" | CAT:889 | 120 s · DAILY | Describe your ideal morning, varying speed and pausing between steps | "@askvinh — 5 Vocal Foundations" | TPL-STORY · TRK-EVERYDAY · Main | T-02 (hint names the creator's term) |
| C-03 | "Describe Without Naming It" | CAT:890 | 60 s · DAILY | Describe an everyday object by its features and comparisons without naming it | "@askvinh — Specificity & Clarity" | TPL-ANSWER · TRK-EVERYDAY · Main | T-19, T-05 |
| C-04 | "CLEAR Technical Explainer" | CAT:891 | 90 s · DAILY | Explain a technical topic to a non-technical relative | "@askvinh — CLEAR Framework" | TPL-ANSWER (TPL-STRUCT* later) · TRK-WORK · Main | T-05 |
| C-05 | "3-Minute Deep Reflection" | CAT:892 | 180 s · WEEKLY | Reflect on a setback that shaped you, with exaggerated vocal variety | "@askvinh — Vocal Image Challenge" | TPL-STORY with `maxSeconds` override (ARC-03 §6.7) or shorten to 120 s · TRK-PRESENT · Practice only | Exceeds the 120 s template cap (SRS-FR-SESS-003; §8 Q-10). T-02, T-12 |
| C-06 | "5-Word Vocabulary Story" | CAT:893 | 90 s · DAILY | Tell a short story that naturally uses five given words | "@askvinh — Verbal + Storytelling" | TPL-STORY + several target words (gap G-13) · TRK-EVERYDAY · Main | Words are from the vocabulary bank (CAT:544-885) |
| C-07 | "PREP Opinion Sprint" | CAT:894 | 60 s · DAILY | Give an opinion in under a minute using PREP | "@askvinh — PREP Framework" | TPL-STRUCT (PREP) · TRK-WORK · Main | T-04; CAT:1043 |
| C-08 | "Summarize @askvinh in Your Words" (`chal_video_summary`) | CAT:895 | 90 s · WEEKLY | (As coded) summarise the creator's three-channel model and say which channel to improve | "@askvinh — Listening & Synthesis" | **Rewrite required:** title and prompt name the creator (CAT:895). Replace with "summarise a technique you practised this week" · TPL-ANSWER · TRK-WORK · Main | T-01; §7.1 |
| C-09 | "Zero-Filler 60s Sprint" | CAT:896 | 60 s · DAILY | Introduce yourself and your current work with no fillers | "@askvinh — Eliminating Fillers" | TPL-ANSWER · all tracks · Main | T-06; RE:36-41; DFT:224, 262; LFE:112-114; CAT:1027 |
| C-10 | "2-Minute Executive Presentation" | CAT:897 | 120 s · WEEKLY | Propose a change to your team with a hook, three points and a firm close | "@askvinh — Presentation Mastery" | TPL-STRUCT* (else TPL-ANSWER) · TRK-PRESENT, TRK-WORK · Main | T-29, T-30, T-12; CAT:1057 |

### 3.5 Programmes, plans and taxonomies (CNT-P)

FS-06 and ARC-03 §1.4–1.5 record that programmes and learning paths are retired in R1 (the Today plan engine replaces them). Their day-level content is kept only as activities and tips linked to techniques (RECOMMENDATION).

| CNT-ID | Name as in code | Type | Location | What it is | Source cited in code | Provenance | Creator name exposed | Verbatim risk | Proposed mapping | Duplicates / notes |
|---|---|---|---|---|---|---|---|---|---|---|
| P-01 | "30-Day Communication Challenge" (`30_day_challenge`) | Programme (4 weeks, day-level drills) | CAT:1001-1064 | A 30-day sequence: foundations, fluency, articulation, practical situations | Subtitle "Adapted from @askvinh's 30-Day Mastery Plan" (CAT:1005) | CHANNEL_ROOT_ONLY | Yes — CAT:1005, 1024, 1028; onboarding AO:432; FTR:46 | Low–Medium: an "adapted" structure is a derivative claim. **INFERRED mismatch:** the candidate source (§3.9, T-03) appears to be a record-and-review plan, not this curriculum | Retire as a programme; reuse days as activities linked to T-01…T-12 | Days repeat T-01…T-12, T-20, C-01, C-09 (see §3.1 duplicates) |
| P-02 | "60-Day Fluency Program" | Programme (3 phases) | CAT:1066-1075 | Phase themes only; no day content | — | NONE | No | Low | Retire | T-04, T-06, T-09, T-36 |
| P-03 | "90-Day Advanced Communication Program" | Programme | CAT:1077-1086 | Month themes; subtitle echoes the three-channel model (CAT:1080) | — | NONE | No (term only) | Low | Retire | T-01, T-02, T-05, T-08, T-09 |
| P-04 | "Daily Vocabulary Challenge" (21 days) | Programme | CAT:1088-1095 | Daily new words with review | — | NONE | No | Low | Retire; Words (FS-07) covers it. Text claims "AI contextual usage evaluation" (CAT:1093), which no longer exists (D-04) | — |
| P-05 | "Articulation & Pronunciation Bootcamp" (28 days) | Programme | CAT:1097-1104 | Minimal pairs, stress, linking | — | NONE | No | Low | Retire; X-001…X-008 remain | T-07 |
| P-06 | "Public Speaking & Storytelling Program" (30 days) | Programme | CAT:1106-1113 | Hooks, gestures, reliving stories | — | NONE | No (term "Power Zone") | Low | Retire | T-08, T-11, T-30 |
| P-07 | "Professional & Influence Program" (21 days) | Programme | CAT:1115-1122 | Authority, colour adaptation, clear explanations | — | NONE | No (creator terms) | Low | Retire | T-05, T-09, T-12 |
| P-08 | Daily routine `dailySteps` (8 steps) | Routine | CAT:989-998 | Eight self-ticked steps; labelled "9-step" elsewhere (CAT:170, RE:120-121, FTR:72) | — | NONE | Yes — CAT:994, 995 | Low | Retire (ARC-03 §1.5; replaced by Today) | T-02, T-03, T-07, T-36 |
| P-09…P-18 | 10 learning paths (`path_01_beginner_english_fluency` … `path_10_interview_preparation`) | Learning paths | LPE:7, 35, 63, 91, 122, 150, 180, 211, 242, 275 | Ordered lists of catalogue activities with milestones | — | NONE | No | Low (milestones name frameworks: LPE:58, 173, 174, 205, 236, 268, 269, 296) | Retire (no path concept in R1; ARC-03 §1.5). P-18's interview activities feed TRK-INTERVIEW | Activities X-101…X-250 |
| P-19 | Plan-task set of `DailyPracticePlanEngine` | Plan generator | DPPE:53-290 | Generic daily task cards (warm-up, PREP recording, STAR role-play, "5 vocal foundations" drill, three-channel drill, shadowing, reflection) | — | NONE | No (creator terms at DPPE:280, 290) | Low | Retire (replaced by the Today plan engine) | T-01, T-02, T-04, T-06, T-20, T-29, T-36 |
| P-20 | 15 `contentModules` | Taxonomy | CAT:167-183 | Module list for the old dashboard and lessons | — | NONE | Yes — CAT:182 | Low | Retire; map to 5 tracks + Techniques collection | ACT:107-188 |
| P-21 | 10 activity modules | Taxonomy | ACT:107-188 | Module list for the Activity Library; subtitles name frameworks (ACT:136, 144, 160, 168, 184) | — | NONE | No | Low | Retire; activities move to tracks | P-20 |

### 3.6 Shadowing and read-aloud scripts (CNT-S)

| CNT-ID | Name as in code | Location | What it is (our words) | Source cited in code | Provenance | Creator name exposed | Verbatim risk | Proposed mapping | Notes |
|---|---|---|---|---|---|---|---|---|---|
| S-01 | "Your Voice Is an Instrument" (`shad_vocal_instrument`) | CAT:928-937 | Two-sentence script about slowing down and using silence, with stress, pitch and pause markers; 125 wpm target | "5 Vocal Foundations — @askvinh" · `https://www.youtube.com/@askvinh` | CHANNEL_ROOT_ONLY | Yes — CAT:931; "Watch @askvinh" button LAT:385 | **High** — presented as the creator's delivery to imitate; first-person aphorism style | Replace with a newly written script · TPL-REPEAT · TRK-CLEAR · Warm-up | ARC-03 §1.4: pack only as new original scripts |
| S-02 | "Opening With PREP Authority" (`shad_prep_opening`) | CAT:938-947 | Script stating a point and an example; 130 wpm | "Answer Any Question — @askvinh" · channel root | CHANNEL_ROOT_ONLY | Yes — CAT:941 | Medium | Replace · TPL-REPEAT · TRK-WORK · Warm-up | T-04 |
| S-03 | "Reliving a Moment With Dialogue" (`shad_relive_story`) | CAT:948-957 | Short anecdote with a quoted line from a mentor; 120 wpm | "3 Ways To Tell Stories — @askvinh" · channel root | CHANNEL_ROOT_ONLY | Yes — CAT:951 | **High** — an anecdote filed under the creator's video title may retell the creator's own story | Replace · TPL-REPEAT · TRK-PRESENT · Warm-up | T-08 |
| S-04 | "Rate & Intentional Pauses" lever script | LM:536 | Contrast script: fast list, then a slow takeaway with a pause | — | NONE | Yes — screen header LM:636 | Low | Rewrite as READ reference text · TPL-READ · TRK-CLEAR · Warm-up | T-02 |
| S-05 | "Melody & Pitch Range" lever script | LM:537 | Script about rising on questions and falling on recommendations | — | NONE | Yes — LM:636 | Low | TPL-READ · TRK-CLEAR · Warm-up | T-02, T-12; pitch not measurable (G-01, G-02) |
| S-06 | "Volume & Projection" lever script | LM:538 | Instructions on breathing low and projecting without strain | — | NONE | Yes — LM:636 | Low | Self-check + TPL-READ · TRK-PRESENT · Warm-up | T-02; volume not measurable (G-03) |
| S-07 | "Character & Warmth" lever script | LM:539 | Script moving from factual to warm tone | — | NONE | Yes — LM:636 | Low | TPL-READ + self-check · TRK-WORK · Warm-up | T-02, T-12 |

### 3.7 Coach tips (CNT-K)

One row per distinct tip idea. "Trigger in code" is the condition that shows it today. "Proposed trigger" uses ARC-05 §8.4 issue codes and ARC-03 §6.9 trigger kinds; codes marked *Proposed* are not in ARC-05 (§4.2). None of these tips has a source citation, so provenance is NONE for all; tips that name the creator are flagged.

| CNT-ID | Tip (summary of code text) | Primary location | Trigger in code | Creator name exposed | Verbatim risk | Proposed technique · trigger | Duplicates / notes |
|---|---|---|---|---|---|---|---|
| K-01 | Close your lips and hold a one-second silence instead of a filler | DFT:146 | `fillerCount > 0` | No | Low | T-06 · `ISSUE_FILLERS_HIGH` | DFT:194, 224, 262; LFE:106, 111-114, 394; DLCE:511, 544; RE:36-41; LIO:697; CAT:305-313, 896; FTR:134 |
| K-02 | Fillers happen when speech outruns thinking; silence signals confidence | DFT:210 | `fillerCount > 0` | **Yes** — "[creator] teaches that…" | Medium (attributed paraphrase) | T-06 "why it works" text, rewritten without attribution | CAT:305 |
| K-03 | Pace is fast; slow down so points land | DFT:151 (> 180 wpm), DFT:213 (> 175 wpm) | WPM thresholds 175/180 | No | Low | T-02, T-06 · `ISSUE_PACE_FAST` | LFE:80, 395; DLCE:549; LIO:675-676, 696; DFT:264. Code thresholds differ from ARC-05 band (> 160 wpm) |
| K-04 | Pace is slow; link phrases more smoothly | DFT:153 | `wpm in 1..99` and > 8 s | No | Low | T-02 · `ISSUE_PACE_SLOW` | LIO:675 |
| K-05 | Too few pauses; add a one-second pause between ideas | DFT:158, 235 | `pauseCount < 2` and > 15 s | No | Low | T-02, T-06 · *Proposed* `ISSUE_FEW_DELIBERATE_PAUSES` (no low band in ARC-05; G-05) | LFE:93-94 |
| K-06 | Pauses longer than 2.5 s; keep them between 0.5 and 1.5 s | DFT:160 | mean pause > 2.5 s | No | Low | T-34, T-02 · `ISSUE_PAUSES_LONG` | LFE:97-98 |
| K-07 | Rate contrast: brisk set-up, slower conclusion | DFT:227 | wpm > 175 | No | Low | T-02 · `ISSUE_PACE_FAST` + technique T-02 | CAT:220-225; LM:536; LFE:395 |
| K-08 | Shadow a lesson clip to build cadence | DFT:229 | `wpm in 1..99` | **Yes** — names the channel | Low | T-36 · `ISSUE_PACE_SLOW` | LFE:84 (neutral wording) |
| K-09 | Rise on questions, fall on conclusions | DFT:243, 216 | no other suggestion / no mistakes | No | Low | T-12 · technique general | LFE:281 (uses removed Hz metric); DLCE:557; CAT:923 |
| K-10 | Review a muted replay first, then audio only | DFT:244; FTR:138 | fallback | **Yes** — FTR:138 "[creator] teaches Stage 1 first because…" | Medium (attributed claim) | T-03 · technique general | FTR:130 |
| K-11 | End on a falling sentence and hold a second of silence | FTR:155 | tutorial | No | Low | T-12, T-30 · technique general | DLCE:437; LIO:690 |
| K-12 | Exaggerate at slow speed so normal speed sounds clear | FTR:172 | tutorial | No | Low | T-07 · technique general; `ISSUE_WORD_NOT_RECOGNISED` | CAT:330-331 |
| K-13 | Stressed syllables are louder, higher and longer | FTR:189 | tutorial | No | Low | X-005 activity tip (no trigger) | CAT:921 |
| K-14 | A pause that feels long to you sounds composed to listeners | FTR:206 | tutorial | No | Low | T-06, T-02 · technique general | — |
| K-15 | Paraphrase the other person's concern before your counterpoint | FTR:223 | tutorial | **Yes** — names the creator's framework | Low | T-10, T-27 · technique general | CAT:396-401 |
| K-16 | Match the model's silence at every pause marker | FTR:240 | tutorial | No | Low | T-36 · technique general (pause placement not measurable, G-05) | — |
| K-17 | Practise grammar aloud, not on paper | FTR:257 | tutorial | No | Low | Global general tip for grammar activities (X-009…X-015) | — |
| K-18 | Don't script word for word; memorise the opening, three points and the close | FTR:274 | tutorial | No | Low | T-34, T-29 · technique general; `ISSUE_PAUSES_LONG` | CAT:971; A710:733 |
| K-19 | Work on one small habit per real meeting | FTR:291 | tutorial | No | Low | T-03 · technique general | — |
| K-20 | Re-record using the corrected phrasing | DFT:232 | grammar issues present | No | Low | Global · `ISSUE_GRAMMAR_POSSIBLE` | LFE:46 |
| K-21 | Replace repeated words with alternatives | DFT:239 | repeated words | No | Low | T-19 · `ISSUE_LOW_VARIETY` | LFE:173, 176; LLAE:663 |
| K-22 | Use PREP or verbal signposts to structure ideas | LFE:348, 359 | low structure score | No | Low | T-04, T-29 · `ISSUE_STRUCT_ELEMENT_MISSING` | DFT:256; LFE:388; LIO:699, 732 |
| K-23 | Split a long sentence into two shorter beats | DLCE:434 | sentence > N words | No | Low | Retire — sentence-length rules are dropped (ARC-05 §8.1) | — |
| K-24 | Add a deliberate pause mid-thought for emphasis (rewrites the user's own sentence) | DLCE:358-371 | clean speech | No | Low | T-02 · technique general (keep as a static tip; drop the generated rewrite) | — |
| K-25 | Lead with the outcome, then STAR; drop softeners such as "I kind of think" | LIO:687-688 | mentor question mentions interview/work | No | Low | T-20, T-21 · `ISSUE_HEDGING_HIGH` | LIO:690 |
| K-26 | Short warm-up (lip trills, jaw, final consonants); slow down on long key words; clarity matters more than accent | LIO:704-707 | mentor question mentions pronunciation | No | Low | T-07 · technique general | — |
| K-27 | Aim for 10–18-word spoken sentences | LIO:712 | mentor question mentions grammar | No | Low | Retire — no supporting metric (ARC-05 §8.1) | — |
| K-28 | Don't open with "Today I will talk about…"; start with a moment; add sensory detail; pause before the punchline | LIO:725-726 | mentor question mentions story/presenting | No | Low | T-30, T-19 · technique general (opening rule = G-11) | — |
| K-29 | Frame chest-to-waist for the video audit | CVR:192 | camera overlay | **Yes** — handle and creator terms | Low | T-11 self-check item (R1 has no video) | — |
| K-30 | Breathe deeper and pause one second between key phrases | LFE:459 | quiet or rushed audio | No | Low | T-02 · technique general | — |
| K-31 | Pitch range in Hz: praise wide range or ask to vary inflection | LFE:278-281 | Hz spread | No | Low | **Retire** — uses the removed "vocal melody from Hz spread" (ARC-05 §6). Replace only if the candidate pitch-range metric is approved (G-01) | — |
| K-32 | Eight pronunciation-drill tips (mouth and tongue placement) | CAT:917-924 (`tip` field) | shown with the drill | No | Low | Activity-level tips for X-001…X-008 (no trigger) | FTR:157-189 |
| K-33 | Twelve "common mistake → correction" pairs | CAT:202-203, 224-225, 246-247, 268-269, 290-291, 312-313, 334-335, 356-357, 378-379, 400-401, 422-423, 444-445 | shown in lessons ("Common Mistake / Correction", CAT:535) | No (but part of creator-attributed records) | Medium | Rewrite as technique tips for T-01…T-12 (§4) | — |

### 3.8 Derived and duplicate structures (not counted as items)

| Structure | Location | Why not counted | Action (RECOMMENDATION) |
|---|---|---|---|
| 12 generated lessons | CAT:521-542 | Built from the 12 "VERIFIED" seeds; explanation = creator advice + "[AI Coaching Note]" (CAT:533); `sourceVerified = true` hard-coded (CAT:539) | Fold into techniques (ARC-03 §1.4) |
| Recommendation cards | RE:30-137 | Point to T-02, T-03, T-04, T-06, T-07, T-36; reason text at RE:73 names the channel | Replaced by Today plan; remove string |
| Dashboard slides | W:205-240 | The three-channel slide opens the 3-stage audit lesson (W:207-213) — wrong target | Retired with the dashboard |
| Feature tutorials (24) | FTR:16-428 | 10 pedagogical tips are K rows; 14 are app-usage texts; 22 creator mentions (§7.1) | Replaced by contextual tips (DES-06 §2.4) |
| Shared-lesson fallbacks | LM:98, 276 | Label shared lessons with the creator handle | Remove |
| YouTube extraction templates | LIO:820-910 | Generate "techniques" from scraped captions: caption sentence copied as `creatorAdvice` (LIO:869), invented timestamps `idx*45` (LIO:880), "00:00" fallback (LIO:903) | Remove (ADR-011); user copies retired (§7.3) |
| Admin publish templates | ADM:205-260 | Default channel URL and creator-labelled hint | Remove with the admin console |
| Vocabulary bank (20 words) | CAT:544-885 | Not creator-derived; owned by FS-07 | Out of scope here (seed for Words) |

### 3.9 Candidate specific videos (web search — candidate, Product Owner to verify)

Only items whose code cites a video title were searched. A "candidate" is a video ID seen on a third-party page with a matching title; **none has been opened or confirmed**, and some may belong to other channels. No caption, transcript or video text was read or copied. The reviewer must open each candidate on YouTube, confirm the channel, the technique and the timestamp, and only then record it (§6).

| CNT-ID | Title cited in code | Candidate video URL | Where the ID was seen | Confidence | Notes for the reviewer |
|---|---|---|---|---|---|
| T-01 | "How to Speak Better Than 99% of People (Complete Framework)" | `https://www.youtube.com/watch?v=ZFKHTFYXppw` | Third-party summary page with the same title (youtubesummary.com/summary/ZFKHTFYXppw); channel not named | Low | Confirm the channel; the three-channel model may be taught in a different video |
| T-02 | "Vinh Giang - 5 Vocal Foundations - Rate of Speech" | — (blank) | No matching video URL found. Third-party summaries of related talks exist, one on another channel's podcast | — | Third-party summaries list the five as rate, volume, pitch/melody, tonality and pause; code says "Character" instead of tonality — check |
| T-03 | "The Ultimate 30-Day Plan To Master Your Communication Skills" | `https://www.youtube.com/watch?v=hIFCxzdSGfU` | A channel-statistics page lists this ID under the channel with this title (vidiq.com/youtube-stats/channel/@askvinh/videos/hIFCxzdSGfU/) | Medium | The creator's site also offers a related workbook; do not copy it |
| T-04 | "How to Answer ANY Question (PREP Framework)" | `https://www.youtube.com/watch?v=2zvvQj9ezWg` | Third-party summary credits a video titled "The PREP Framework: How To Communicate Confidently In Any Situation!" to the creator (youtubesummary.com/summary/2zvvQj9ezWg) | Low–Medium | Title differs from the code |
| T-05 | "How to Explain Anything To Anyone (Even If It's Complex!)" | `https://www.youtube.com/watch?v=Sqh6rt1hN2A` | Third-party summary with the identical title, describing a "CLEAR" method (youtubesummary.com/summary/Sqh6rt1hN2A); channel not named | Low–Medium | Confirm channel |
| T-06 | "How to Pause Without Feeling Awkward" | — (blank) | Not found | — | The creator's blog has a related article on pausing (not a video) |
| T-07 | "Simple Exercise to DRASTICALLY Improve Your Articulation!" | `https://www.youtube.com/watch?v=-QI32AsePvI` | Third-party summary describing it as a Q&A episode of the channel (youtubesummary.com/summary/-QI32AsePvI) | Medium | The summary suggests a different exercise from the code's steps (and the code's videoId hints at yet another). Re-derive the steps from the video itself |
| T-08 | "3 Powerful Ways To Tell Stories Without Boring People" | — (blank) | A third-party digest confirms a video with this title (a channel Q&A episode); no URL found | — | Search the channel by title |
| T-09 | "How to Talk to ANYONE (Once You Know Their Color!)" | `https://www.youtube.com/watch?v=ikbcrpowlIs` | Third-party highlight page with the identical title (videohighlight.com/v/ikbcrpowlIs) | Medium | Summaries say the colour model comes from a third-party book (§8 Q-02) |
| T-10 | "The 4 Ways of Listening to Build Instant Rapport" | — (blank) | Not found | — | — |
| T-11 | "How to Use Hand Gestures Naturally When Speaking" | — (blank) | Not found | — | — |
| T-12 | "How to Sound Friendly Without Losing Authority" | — (blank) | Not found | — | — |
| T-13 | "High Low Buffalo" (pending) | — (blank) | Not found | — | May not be a channel technique (§8 Q-01) |
| T-14 | "3-2-1 Conversation Framework" (pending) | — (blank) | No video found; a news article describes a "3-2-1" method from the creator for answering on the spot | — | Probably not a conversation framework — re-scope after viewing |
| S-01…S-03 | Same titles as T-02, T-04, T-08 | See those rows | — | — | Scripts must be newly written whatever the source |


## 4. Technique → feedback feasibility

This section answers, for each technique: what can our deterministic checks honestly report as a "mistake found" (PRD-FR-047, SRS-FR-EVAL-015), what cannot be detected and therefore needs a self-check checklist (ARC-05 §8.4 "Self-check"; ARC-03 §6.8 `selfCheckItems`, stored as self-reported, never Measured or Estimated), and which coach tips to attach (SRS-FR-EVAL-016; ARC-03 §6.9 triggers). Rules applied:

- Only ARC-05 §6 metrics and §8.4 issue codes count as detection. No new `M-*` IDs are invented; where a technique needs a measure ARC-05 lacks, it is raised as a gap (§4.3).
- An issue is raised only outside the ARC-05 §6 band by more than the noise band (ARC-05 §8.4).
- Self-check items never claim measurement and never reference metrics (ARC-03 V-17).
- Coach tips below are **new text in our own words**, each with a trigger in ARC-03 §6.9 form: an issue code, a metric + band, a technique (general tip, empty trigger), or a template. They are drafts for the §6 rewrite step.

### 4.1 Issue codes

**Existing (ARC-05 §8.4; ARC-02 `IssueCode`)** — usable now:

| Issue code | Source metric | Label | Tier | Used by techniques |
|---|---|---|---|---|
| ISSUE_WORD_NOT_RECOGNISED | M-MATCH | Measured ("recognised as intended", never "pronunciation") | TR | T-07, T-12, T-36, X-001…X-008 |
| ISSUE_WRONG_FORM / ISSUE_TARGET_WORD_MISSING | M-TARGET / stage 2 | Measured | TR | Word stages; C-06 (with G-13) |
| ISSUE_TOO_SHORT | M-LEN | Measured | TR (words) or AU (seconds) | Almost all speaking techniques |
| ISSUE_STRUCT_ELEMENT_MISSING | M-STRUCT (STAR, PREP only) | Estimated | TR | T-04, T-20 (others need G-06) |
| ISSUE_FILLERS_HIGH | M-FILL | Estimated (recognisers often drop "um/uh") | TR | T-01, T-03, T-06, T-25, T-34 and most free speaking |
| ISSUE_PAUSES_LONG | M-PAUSE | Measured | AU | T-02, T-06, T-29, T-34 |
| ISSUE_PACE_FAST / ISSUE_PACE_SLOW | M-WPM | Measured | TR+AU | T-01, T-02, T-05, T-12, T-35, T-36 |
| ISSUE_HEDGING_HIGH | M-HEDGE | Estimated | TR | T-12, T-16, T-20, T-21 |
| ISSUE_LOW_VARIETY | M-LEX | Estimated | TR (≥ 25 words) | T-08, T-19, T-28 |
| ISSUE_GRAMMAR_POSSIBLE | M-GRAM (qualified rules only) | Estimated | TR | X-009…X-015 |
| ISSUE_TOO_QUIET | M-LEVEL | Measured (quality note) | AU | All (explains other issues) |

**Proposed (not in ARC-05; RECOMMENDATION — unusable until ARC-05 adds the measure or rule and it passes SPK-01/SPK-02):**

| Proposed issue code | Needs gap | Label if approved | For techniques |
|---|---|---|---|
| ISSUE_PITCH_NARROW | G-01 | Measured | T-01, T-02, T-08 |
| ISSUE_RISING_STATEMENT_ENDINGS | G-02 | Measured | T-12, X-007 |
| ISSUE_VOLUME_FLAT | G-03 | Measured | T-02 |
| ISSUE_NO_PACE_CONTRAST | G-04 | Measured | T-02 |
| ISSUE_FEW_DELIBERATE_PAUSES, ISSUE_PAUSE_MISSED_AT_MARK, ISSUE_NO_THINKING_PAUSE | G-05 | Measured | T-02, T-06, T-25, T-36 |
| ISSUE_STRUCT_ORDER (key element not first) | G-06 | Estimated | T-04, T-16, T-21 |
| ISSUE_NO_DIRECT_SPEECH | G-07 | Estimated | T-08 |
| ISSUE_FEW_SENSORY_DETAILS | G-08 | Estimated | T-19 |
| ISSUE_NO_QUESTION_ASKED | G-09 | Estimated | T-13, T-17, T-33 |
| ISSUE_UNEXPLAINED_JARGON | G-10 | Estimated | T-05, T-21 |
| ISSUE_WEAK_OPENING, ISSUE_TRAILING_CLOSE, ISSUE_AND_THEN_CHAIN | G-11 | Estimated | T-08, T-28, T-30 |
| ISSUE_KEY_POINT_MISSED | G-12 | Estimated | T-10, T-27 |
| ISSUE_TARGET_WORD_MISSING (several words in free speaking) | G-13 | Measured | C-06 |
| ISSUE_CHAIN_BROKEN | G-14 | Measured | T-32 |

### 4.2 Per-technique feasibility

| Technique | Mistakes our checks can detect now (issue ← metric · label · tier) | Not detectable from audio/transcript → self-check checklist (our wording) | Coach tips (trigger → tip) | ARC-05 gap |
|---|---|---|---|---|
| **T-01** Three channels (look, sound, words) — class B | Sound: ISSUE_PACE_FAST/SLOW ← M-WPM · Measured · TR+AU; ISSUE_PAUSES_LONG ← M-PAUSE · Measured · AU<br>Words: ISSUE_FILLERS_HIGH ← M-FILL · Estimated · TR; ISSUE_HEDGING_HIGH ← M-HEDGE · Estimated · TR; ISSUE_TOO_SHORT ← M-LEN | Posture, gestures, eye line, facial expression, vocal warmth.<br>☐ I sat or stood upright before I started<br>☐ My hands were free, not holding notes<br>☐ I looked at one point as if at a listener<br>☐ My face matched my message<br>☐ I listened to my replay once without looking (T1/T3b only) | *Technique (general)* → "Work on one channel at a time. Today, judge only how you sound: your pace and your pauses."<br>*ISSUE_FILLERS_HIGH* → "Your words land harder without fillers. When you feel one coming, close your mouth and wait a beat."<br>*ISSUE_PACE_FAST* → "Slower speech gives each point time to register. Aim for the range shown above." | G-01 (vocal variety); visual channel is out of scope (video removed, ARC-05 §6; NG-07) |
| **T-02** Five voice levers — class B | Rate: ISSUE_PACE_FAST/SLOW ← M-WPM · Measured · TR+AU<br>Pause: ISSUE_PAUSES_LONG ← M-PAUSE · Measured · AU (too many/long only)<br>READ scripts: ISSUE_WORD_NOT_RECOGNISED ← M-MATCH · Measured · TR | Pitch movement, loudness changes, emotional colour, speed contrast within the attempt, short deliberate pauses.<br>☐ I slowed down on my key sentence<br>☐ I paused just before my main point<br>☐ My pitch moved instead of staying on one note<br>☐ I made one sentence deliberately softer or louder<br>☐ My tone changed for the emotional part | *Technique (general)* → "Choose two levers for this attempt — for example 'slow down' and 'pause' — not all five."<br>*ISSUE_PACE_FAST + T-02* → "Say the set-up at normal speed, then slow right down for the one sentence you want remembered."<br>*ISSUE_PAUSES_LONG + T-02* → "A planned pause helps; a long search for words doesn't. Know your next sentence before you stop." | G-01, G-03 (ARC-05 §8.4 candidates, T1 only, unapproved); G-04; G-05 |
| **T-03** Record, wait, review in passes — class B | The audit is a self-review. The recording itself yields the usual issues: ISSUE_FILLERS_HIGH, ISSUE_PACE_*, ISSUE_PAUSES_LONG, ISSUE_HEDGING_HIGH, ISSUE_LOW_VARIETY. The transcript (TR) supports the "words" pass | Whether the passes were done; the visual pass (no video in R1); waiting before review. Replay exists only in T1/T3b (ARC-05 §3).<br>☐ I waited before reviewing<br>☐ I listened once with my eyes closed and noted one sound habit<br>☐ I read the transcript and noted one word habit<br>☐ I picked one habit to change next time | *Technique (general)* → "Review one thing at a time — first the sound, then the words — and choose a single habit for next time."<br>*ISSUE_FILLERS_HIGH + T-03* → "Read your transcript and mark each filler. Then answer again with silent pauses in their place."<br>*No issue + T-03* → "Compare this attempt with your first one in Progress. Look for one change, not perfection." | Replay unavailable in T2/T3a (by design); visual pass out of scope |
| **T-04** View first, reason, example, view again — class A | ISSUE_STRUCT_ELEMENT_MISSING ← M-STRUCT (PREP) · Estimated · TR (cue phrases shown as evidence)<br>ISSUE_TOO_SHORT ← M-LEN; ISSUE_FILLERS_HIGH; ISSUE_PACE_* | Whether the view came first; whether the example was concrete; whether the closing restated the view.<br>☐ My first sentence gave my view<br>☐ My example was a real, specific case<br>☐ My last sentence repeated my view in new words | *ISSUE_STRUCT_ELEMENT_MISSING + T-04* → "One part is missing ({element}). Add it in a single sentence — for an example, start with 'Last month, …'."<br>*Technique (general)* → "Answer in your first sentence; explain afterwards."<br>*ISSUE_TOO_SHORT + T-04* → "You stopped early. Add one specific example before you restate your view." | G-06 (element order: view first) |
| **T-05** Explain from the familiar — class B | ISSUE_TOO_SHORT ← M-LEN; ISSUE_PACE_FAST ← M-WPM · Measured; ISSUE_FILLERS_HIGH | Unexplained jargon or abbreviations, use of a comparison, fit to the listener, benefit stated, takeaway repeated.<br>☐ I started from something the listener already knows<br>☐ I used an everyday comparison<br>☐ I explained or avoided every abbreviation<br>☐ I said what the listener gains<br>☐ I finished by repeating the main idea in one sentence | *Technique (general)* → "Before the detail, give a picture the listener already knows: 'It works a bit like…'."<br>*ISSUE_PACE_FAST + T-05* → "New ideas need more time. Slow down whenever you introduce a term."<br>*ISSUE_TOO_SHORT + T-05* → "Add one everyday comparison so a non-expert can follow." | G-10; G-06 (CLEAR lexicon); G-11 |
| **T-06** Silence instead of fillers — class A | ISSUE_FILLERS_HIGH ← M-FILL · Estimated · TR (top fillers with counts)<br>ISSUE_PAUSES_LONG ← M-PAUSE · Measured · AU (catches silences that run too long)<br>ISSUE_PACE_FAST ← M-WPM · Measured | Fillers the recogniser dropped; whether pauses felt calm.<br>☐ When I felt a filler coming, I closed my mouth and paused<br>☐ My pauses felt calm, not panicked | *ISSUE_FILLERS_HIGH* → "You said '{top filler}' {n} times. Next time, close your lips and let a second of silence do the work."<br>*ISSUE_PACE_FAST + T-06* → "Fillers often come from rushing. Slowing down gives your next thought time to arrive."<br>*No issue + T-06* → "We found few fillers. Speech recognition can miss 'um' and 'uh', so listen back if you can." | None essential; G-05 would add a check that silent pauses were actually used |
| **T-07** Exaggerated articulation warm-up — class B | In TPL-REPEAT/READ: ISSUE_WORD_NOT_RECOGNISED ← M-MATCH · Measured · TR (which words were not recognised as intended)<br>READ: ISSUE_PACE_FAST | Mouth, jaw and tongue movement; crisp consonants; word endings (pronunciation is not assessed, NG-03); non-speaking warm-ups.<br>☐ I exaggerated my mouth movements in the slow round<br>☐ I could feel the last sound of each word<br>☐ My jaw felt loose | *ISSUE_WORD_NOT_RECOGNISED + T-07* → "These words weren't recognised as intended: {w1}, {w2}. Say them once slowly with big mouth movements, then at normal speed."<br>*Technique (general)* → "Do the slow, exaggerated round first; the normal-speed round should then feel easier."<br>*ISSUE_PACE_FAST + T-07* → "Clarity drops when you rush. Keep the second round at a comfortable pace." | None by design; show the ARC-05 §9 caveat (recognised ≠ clear) |
| **T-08** Relive, don't report — class B | ISSUE_TOO_SHORT ← M-LEN; ISSUE_PAUSES_LONG ← M-PAUSE; ISSUE_LOW_VARIETY ← M-LEX · Estimated; ISSUE_FILLERS_HIGH | Direct speech, opening inside the scene, sensory detail, character voices.<br>☐ I started at a specific time and place<br>☐ I repeated at least one thing someone actually said<br>☐ I described something you could see or hear<br>☐ I changed my voice for another person | *Technique (general)* → "Start inside the moment — 'It was 7 a.m. and the train had stopped…' — not with background."<br>*ISSUE_TOO_SHORT + T-08* → "Add the exact words someone said. It brings the scene to life."<br>*ISSUE_LOW_VARIETY + T-08* → "Swap general words like 'nice' or 'bad' for something the listener could picture." | G-07, G-11, G-01 |
| **T-09** Adapt to the listener's style — class C | ISSUE_TOO_SHORT for each version; ISSUE_HEDGING_HIGH for the results-first version (Estimated) | Listener's style, whether the framing matched, quality of adaptation.<br>☐ Version 1 led with the result or numbers<br>☐ Version 2 led with people or the story<br>☐ I changed my pace between versions<br>☐ I still sounded like myself | *Technique (general)* → "Change the first sentence for each listener; the rest of the message can stay the same."<br>*ISSUE_HEDGING_HIGH + T-09* → "For a results-focused listener, drop softeners such as 'kind of' and state the outcome." | None feasible; IP question §8 Q-02 |
| **T-10** Listen with eyes, ears, mind and empathy — class C | R1: only ISSUE_TOO_SHORT on a spoken summary | Attention, empathy, visual cues, interruptions, accuracy of the paraphrase.<br>☐ I let the other person finish completely<br>☐ I paused before replying<br>☐ I summed up their point before giving mine<br>☐ I named the feeling I noticed | *Technique (general)* → "Before you give your view, repeat the other person's main point in one sentence."<br>*ISSUE_TOO_SHORT + T-10* → "Your summary was very short. Include their main concern and one detail." | G-12; R2 TPL-ROLE |
| **T-11** Open gestures around the torso — class C | Nothing (no audio signal). Honest result: "Gestures can't be checked from audio" | ☐ My hands stayed visible between chest and waist<br>☐ I used a gesture to show a number, size or contrast<br>☐ I rested my hands between points<br>☐ I didn't fidget with objects<br>☐ I didn't cross my arms | *Technique (general)* → "Practise in front of a mirror or your phone camera: count points on your fingers and rest your hands between them."<br>*Technique (general, alternate)* → "One gesture per main point can also steady your pace." | Video out of scope (ARC-05 §6 removed video presence; NG-07) |
| **T-12** Warm and decisive; falling statement endings — class B | ISSUE_HEDGING_HIGH ← M-HEDGE · Estimated · TR; ISSUE_PACE_FAST ← M-WPM · Measured; READ statements: ISSUE_WORD_NOT_RECOGNISED | Rising vs falling endings; warmth.<br>☐ My statements ended with my voice going down<br>☐ I raised my pitch only on real questions<br>☐ I paused after my main statement<br>☐ I sounded friendly mid-sentence | *ISSUE_HEDGING_HIGH* → "You softened your point with '{hedge}' {n} times. Say it plainly: 'I recommend…'."<br>*Technique (general)* → "Let your voice drop on the last word of a statement; save the lift for questions."<br>*ISSUE_PACE_FAST + T-12* → "An unhurried pace sounds more certain. Slow down slightly on your conclusion." | G-02 |
| **T-13** High, low, surprise — class B | ISSUE_TOO_SHORT ← M-LEN. Element detection needs G-06; the closing invitation needs G-09 | ☐ I shared one good thing<br>☐ I shared one difficulty (not too heavy)<br>☐ I shared one surprising detail<br>☐ I ended by inviting the other person to share | *Technique (general)* → "Keep each part to one or two sentences, then hand over with a question."<br>*ISSUE_TOO_SHORT + T-13* → "Add a small concrete detail to each part so the listener has something to ask about." | G-06, G-09. Provenance blocked (pending in CAT) |
| **T-14** "3-2-1" (undefined) — class D | Not assessable until the technique is verified and written | — | — | Decide after verification |
| **T-15** Clearer-speaking habits and nerves series (undefined) — class D | Not assessable; likely to split into several techniques (some overlap T-07, T-35) | — | — | Decide after verification |
| **T-16** Clear, concise, compelling — class B | ISSUE_TOO_SHORT ← M-LEN; ISSUE_HEDGING_HIGH ← M-HEDGE; ISSUE_FILLERS_HIGH. Elements need G-06 | Whether the headline came first; how compelling it was.<br>☐ My first sentence was the headline<br>☐ I cut every sentence that added nothing<br>☐ I said why it matters to people | *Technique (general)* → "Headline first, one supporting fact, then the human benefit — three sentences can be enough."<br>*ISSUE_HEDGING_HIGH + T-16* → "Concise sounds confident. Remove 'just' and 'sort of'." | G-06; time-box (over-length is not a mistake in R1, §8 Q-09) |
| **T-17** Family, work, free time, hopes — class B | R1 single-turn variant: ISSUE_TOO_SHORT; question count needs G-09 | Conversation flow, rapport.<br>☐ I asked about at least two areas<br>☐ I followed up on something they said<br>☐ I shared a little about myself too | *Technique (general)* → "When the chat stalls, move to another area — work to free time, free time to plans."<br>*ISSUE_TOO_SHORT + T-17* → "Add an open question that starts with 'what' or 'how'." | G-09; R2 TPL-ROLE |
| **T-18** Subtle matching — class C | Nothing technique-specific | ☐ I matched the other person's energy a little<br>☐ I reused one of their key words<br>☐ My changes were subtle | *Technique (general)* → "Match the other person's pace a little — not exactly — and reuse one of their key words." | None (not honestly measurable) |
| **T-19** Sensory detail — class B | ISSUE_TOO_SHORT; ISSUE_LOW_VARIETY ← M-LEX · Estimated (weak proxy) | Which senses were used.<br>☐ Something seen<br>☐ Something heard<br>☐ Something felt (touch or emotion)<br>☐ Something smelled or tasted | *Technique (general)* → "Add one detail for each sense you can: a colour, a sound, a texture."<br>*ISSUE_LOW_VARIETY + T-19* → "Be specific: not 'a car' but 'a dusty red van'." | G-08 |
| **T-20** Situation, task, action, result — class A | ISSUE_STRUCT_ELEMENT_MISSING ← M-STRUCT (STAR) · Estimated · TR; ISSUE_TOO_SHORT; ISSUE_HEDGING_HIGH; ISSUE_FILLERS_HIGH | Share of time on action and result; whether the result is measurable.<br>☐ Most of my answer was about what I did and the result<br>☐ My result included a number or a clear change | *ISSUE_STRUCT_ELEMENT_MISSING + T-20* → "Add the {element}. For the result: what changed because of what you did?"<br>*Technique (general)* → "Keep the background short; the interviewer wants your actions and the outcome."<br>*ISSUE_HEDGING_HIGH + T-20* → "Own your actions: 'I led…', not 'I kind of helped…'." | G-06 (element proportions) |
| **T-21** Bottom line first — class B | ISSUE_HEDGING_HIGH; ISSUE_TOO_SHORT; ISSUE_FILLERS_HIGH. Position of the bottom line needs G-06 | ☐ My first sentence stated the decision or request<br>☐ I gave no more than three supporting points<br>☐ I ended with a clear ask | *Technique (general)* → "Lead with the decision you need, then the reasons."<br>*ISSUE_HEDGING_HIGH + T-21* → "Busy listeners need certainty. State the recommendation without softeners." | G-06 (position), G-10 |
| **T-22** Situation, behaviour, impact — class B | ISSUE_TOO_SHORT. Elements need G-06 | Tone and respect.<br>☐ I said when and where<br>☐ I described the behaviour, not the person<br>☐ I said what the impact was<br>☐ I suggested a next step together | *Technique (general)* → "Describe what you saw, not what you think the person is like." | G-06 |
| **T-23** Usual behaviour, several signals, setting — class C | ISSUE_TOO_SHORT on the spoken explanation | Correctness of the interpretation.<br>☐ I compared with how the person usually behaves<br>☐ I looked for more than one signal<br>☐ I considered the situation | *Technique (general)* → "Don't read one gesture on its own — look for two or three signals that agree." | None (knowledge content) |
| **T-24** Now, before, next — class B | ISSUE_TOO_SHORT; ISSUE_PACE_FAST; ISSUE_FILLERS_HIGH. Elements need G-06 | Relevance to the role.<br>☐ I started with what I do now<br>☐ I mentioned one or two earlier steps<br>☐ I ended with what I want next | *Technique (general)* → "Now, before, next — one or two sentences each — and finish with why this role."<br>*ISSUE_TOO_SHORT + T-24* → "Add one achievement to the 'before' part." | G-06 |
| **T-25** Pause, clarify, answer briefly — class B | ISSUE_TOO_SHORT; ISSUE_FILLERS_HIGH; ISSUE_PACE_FAST | The opening pause (M-PAUSE excludes leading silence); quality of the clarification.<br>☐ I paused before answering<br>☐ I checked or restated the question<br>☐ My answer was short and direct | *Technique (general)* → "Pausing before a surprise question looks composed, not unsure."<br>*ISSUE_FILLERS_HIGH + T-25* → "Start with silence instead of 'um' while you think." | G-05 (response pause) |
| **T-26** Acknowledge, find the concern, reframe, next step — class B | R1 single turn: ISSUE_TOO_SHORT; ISSUE_HEDGING_HIGH. Elements need G-06 | ☐ I acknowledged the concern first<br>☐ I named the real concern<br>☐ I proposed a next step | *Technique (general)* → "Acknowledge before you answer: 'That's a fair concern…'." | G-06; R2 |
| **T-27** Hear, paraphrase, add, confirm — class B | R2 role-play; ISSUE_TOO_SHORT | ☐ I paraphrased before adding my view<br>☐ I confirmed the next step | *Technique (general)* → "Check your understanding out loud: 'So the main issue is …, right?'" | G-12; R2 |
| **T-28** Story shape and "but / so" links — class B | ISSUE_TOO_SHORT; ISSUE_PAUSES_LONG; ISSUE_LOW_VARIETY; ISSUE_FILLERS_HIGH | Story elements; "and then" chains; clarity of the lesson.<br>☐ I set the scene briefly<br>☐ I made the problem clear<br>☐ There was a turning point<br>☐ I ended with a lesson | *Technique (general)* → "Link events with 'but' and 'so' instead of 'and then' — it adds tension and cause."<br>*ISSUE_TOO_SHORT + T-28* → "Add the turning point: what changed?" | G-06, G-11 |
| **T-29** One message, three points, call to action — class B | ISSUE_TOO_SHORT; ISSUE_PACE_FAST; ISSUE_FILLERS_HIGH; ISSUE_PAUSES_LONG. Elements and signposts need G-06 | ☐ I stated one core message<br>☐ I had exactly three supporting points<br>☐ I ended with a clear action | *Technique (general)* → "Signpost the three points out loud: 'First… Second… Finally…'."<br>*ISSUE_PAUSES_LONG + T-29* → "Plan your three points before you start so you don't stop to search." | G-06 |
| **T-30** Strong openings and closes — class B | ISSUE_TOO_SHORT; ISSUE_FILLERS_HIGH. Weak opener / trailing close need G-11 | ☐ I opened with a question, a contrast or a moment<br>☐ I didn't start with my name or "today I'll talk about"<br>☐ I ended with a callback or a call to action | *Technique (general)* → "Cut the warm-up sentence and start with your most interesting line." | G-11 |
| **T-31** Sharing a resolved difficulty — class C | ISSUE_TOO_SHORT | ☐ I chose a difficulty I've already worked through<br>☐ I said what I learned<br>☐ I kept it brief and calm | *Technique (general)* → "Share a setback you've already resolved, and finish with what you learned." | None |
| **T-32** Start with their last word — class B | R2 role-play; the chaining check needs G-14 | ☐ I waited for their final word before planning my reply | *Technique (general)* → "Don't plan your reply while they're talking — wait for their final word." | G-14; R2 |
| **T-33** Three follow-ups on one thread — class B | R1 ("say three follow-up questions"): ISSUE_TOO_SHORT; question count needs G-09 | ☐ My questions stayed on one topic<br>☐ Each question built on the previous answer<br>☐ I used "what" or "how" questions | *Technique (general)* → "Stay on one thread for three questions before you change topic." | G-09; R2 |
| **T-34** Anchors, not a script — class B | ISSUE_PAUSES_LONG ← M-PAUSE · Measured (stalls); ISSUE_FILLERS_HIGH; ISSUE_TOO_SHORT | Whether a script was memorised.<br>☐ I spoke from three anchor points, not memorised sentences<br>☐ When I lost a word, I moved to the next anchor | *ISSUE_PAUSES_LONG + T-34* → "Long stops suggest you were hunting for exact words. Move to your next anchor point instead."<br>*Technique (general)* → "Memorise your first line, three anchors and your last line; say the rest freely." | None |
| **T-35** Calm breathing and listener focus — class B | ISSUE_PACE_FAST on the calm opening line (READ or ANSWER) | Breathing, mindset.<br>☐ I took two slow breaths before speaking<br>☐ I thought of one useful idea for my listener<br>☐ I said my first sentence slowly | *ISSUE_PACE_FAST + T-35* → "Nerves speed us up. Say your first sentence a little slower than feels natural."<br>*Technique (general)* → "Before you start, picture one person who needs your message." | None |
| **T-36** Shadowing a model line — class B | TPL-REPEAT: ISSUE_WORD_NOT_RECOGNISED ← M-MATCH · Measured · TR; M-SPK vs reference duration is a headline (ARC-05 §7) but has no issue code; READ: ISSUE_PACE_FAST/SLOW (reference rate ±20 %) | Pauses at markers, stressed words, pitch direction.<br>☐ I paused where the script showed a pause<br>☐ I stressed the marked words<br>☐ My voice rose and fell where the arrows showed | *ISSUE_WORD_NOT_RECOGNISED + T-36* → "Play the slow reference, then repeat just the line with '{w1}'."<br>*ISSUE_PACE_FAST + T-36* → "You finished ahead of the model. Match its speed and pauses." | G-05, G-01, G-02; no issue code for M-SPK vs reference duration |
| **T-37** Hear, think together or fix? — class C | ISSUE_TOO_SHORT | ☐ I worked out what the speaker wanted<br>☐ My reply matched that need | *Technique (general)* → "If you're not sure, ask: 'Would you like ideas, or do you just want to talk it through?'" | None |

**Result.** 3 techniques are checkable at their core today (T-04, T-06, T-20); 25 are partly checkable and need a self-check for the rest; 7 are self-check only; 2 cannot be assessed until verified. 35 techniques receive a self-check checklist. Every technique has at least one general tip (needed by ARC-03 V-15).

### 4.3 Metric and rule gaps for ARC-05 §6 / §8 (raised, not resolved here)

| Gap | What is missing | Needed by | Status in ARC-05 v0.2 | Tier | Label if approved |
|---|---|---|---|---|---|
| G-01 | Pitch range as a correctly labelled acoustic measure | T-01, T-02, T-08, T-36, m03 activities | **Candidate** in §8.4 ("Pitch range"), not in the §6 catalogue until SPK-01 validation | PCM (ARC-05 says T1 only; §8 I-01) | Measured |
| G-02 | Direction of pitch at the end of phrases (rising vs falling statement endings) | T-12, X-007, S-05 | Not present | PCM + phrase timing | Measured |
| G-03 | Volume variation as a skill measure (distinct from the M-LEVEL quality signal) | T-02, m03_volume_control, m09_vocal_volume | **Candidate** in §8.4 ("Volume variation") | PCM | Measured, with microphone-distance caveat |
| G-04 | Speed contrast within one attempt (segment-level pace) | T-02, K-07, m04_speaking_slowly_emphasis | "Rate consistency" is in the removed list until implemented with transcript timing | TR+AU with segment timing (T1 segments) | Measured |
| G-05 | Pause profile: short deliberate pauses (< 1.0 s), a "too few pauses" band, pauses at reference markers, pause before answering (leading silence) | T-02, T-06, T-25, T-36, S-01…S-07, K-05, K-16 | M-PAUSE counts only silences ≥ 1.0 s inside speech, excludes leading/trailing silence, and has only an upper band | AU (markers also need TR timing) | Measured |
| G-06 | Framework lexicons beyond STAR/PREP (CCC, BLUF, SBI, High-Low-Buffalo, present-past-future, pause-bridge-answer, acknowledge-reframe-next, story arc, message + three points + close, CLEAR) and element **order/position** | T-04, T-05, T-13, T-16, T-21, T-22, T-24…T-29 | M-STRUCT and ARC-03 §6.7 `framework` support STAR and PREP only | TR | Estimated |
| G-07 | Direct-speech cue detection (reported dialogue) | T-08 | Not present | TR | Estimated |
| G-08 | Sensory-language lexicon | T-19, m07_concrete_descriptions | Not present | TR | Estimated |
| G-09 | Question detection without punctuation (ARC-05 §8.3) | T-13, T-17, T-33 | Not present | TR | Estimated |
| G-10 | Jargon and unexplained abbreviation detection | T-05, T-21 | Not present | TR | Estimated |
| G-11 | Discourse rules: preamble or self-introduction openers, trailing closes ("so yeah, that's it"), "and then" chains — qualified under SPK-02 like grammar rules | T-08, T-28, T-30, K-28 | §8.1 qualifies grammar rules only | TR | Estimated |
| G-12 | Key-point coverage against a reference list, plus a "listen, then answer" step with a TTS passage | T-10, T-27, 9 listening activities | Not present; no R1 template plays a passage before a free answer | TR | Estimated |
| G-13 | Several target words in free-speaking templates | C-06 | M-TARGET applies to word stages 2–4 only | TR | Measured |
| G-14 | Reply-chaining check (first word of reply = last word or letter of the cue) | T-32 | Not present (R2 role-play) | TR | Measured |

None of these is assigned an `M-*` ID here; ARC-05's owner decides whether and how to add them (FS-05 Issue 9 raises G-01/G-03 as well).


## 5. Gaps against the Product Owner's aim

The aim is a curriculum covering **all** the frameworks, techniques, tasks, exercises, activities and tips taught on the channel (PRD-FR-056). This audit can only compare the aim with what the **code** contains. It cannot measure coverage of the channel, because the channel has not been catalogued (that needs the §6 process) and this audit was not allowed to ingest it.

**Scale indication (INFERRED, unverified).** The code attributes 15 items to the channel, of which 12 cite a video title. A third-party statistics page seen during the §3.9 searches reported roughly 1,500 videos on the channel in November 2026 (many may be short clips or repeats). Even allowing for repetition, a dozen cited videos is very unlikely to cover "all" of the channel's techniques. The real coverage is unknown until the channel is catalogued.

| # | Gap | Evidence (VERIFIED unless marked) | Consequence | RECOMMENDATION |
|---|---|---|---|---|
| GAP-01 | No item has specific-video provenance | §2.2: SPECIFIC_VIDEO = 0; channel-root URLs at CAT:205…447 | Under SRS-BR-028 and ARC-03 V-10…V-12, **no creator-derived item can ship today** | Run §6 on every item before content freeze |
| GAP-02 | No catalogue of the channel exists (videos → techniques → timestamps) | Only 12 titles in code; 3 placeholders | Coverage of "all" techniques cannot be shown or planned | Product Owner (or a delegated reviewer) catalogues the channel by hand with the §6.4 template, newest and most-viewed teaching videos first |
| GAP-03 | Existing text is creator-attributed, not original wording | `creatorAdvice` / `originalContext` fields (CAT:195…439); "[creator] teaches" strings (DFT:210, FTR:138) | Every seed must be rewritten, not lightly edited | §6 step 3 |
| GAP-04 | "Technique" is not a content type in code; activities do not reference techniques | `CuratedTechniqueSeed` is separate from `ActivityDefinition`; no technique field on activities (A13:25-94) | Techniques cannot drive Today, tips or the Techniques collection (SRS-FR-PRAC-008; ARC-03 V-03, V-09) | Build `techniques.json`, `tips.json` and `techniqueIds` on activities (ARC-03 §6.7–6.9) using §3.3 links |
| GAP-05 | Tips are hard-coded strings with ad-hoc triggers | §3.7: thresholds 175/180 wpm (DFT:151, 213) vs ARC-05 band > 160; K-31 uses a removed metric; 5 tips name the creator | Tips would contradict the issue list and labels | Rewrite as pack tips with ARC-03 triggers; retire K-23, K-27, K-31 |
| GAP-06 | 25 exercise types vs 5 templates | §3.3 totals: 24 role-play activities wait for R2; 9 listening activities need a "listen, then answer" step no R1 template offers; 26 activities have no evaluable template (warm-ups, body language, reflection, outside-app tasks) | About 59 of 150 activities cannot run as R1 speaking attempts | Convert where possible (single-turn variants), mark the rest R2 or self-check; decide the listening question (§8 Q-05) |
| GAP-07 | Track coverage is uneven | §3.3: R1-usable activities per track — TRK-CLEAR 20 (16 warm-up, 4 main), TRK-EVERYDAY 22 (2 warm-up, 20 main), TRK-WORK 19 (1 warm-up, 18 main), TRK-PRESENT 27 (3 warm-up, 24 main), **TRK-INTERVIEW 3 (0 warm-up, 3 main)** | Fails ARC-03 §6.13 (≥ 15 per track, ≥ 3 warm-ups and ≥ 8 main tasks) for TRK-INTERVIEW, warm-ups for TRK-EVERYDAY and TRK-WORK, and main tasks for TRK-CLEAR | Fill from X-013, X-028, X-030, X-044…X-046, C-07 and new interview content; add warm-up READ texts per track |
| GAP-08 | Vocal-delivery teaching has no honest metric | §4.3 G-01…G-05; FS-05 Issue 9 | A large part of the voice teaching (pitch, volume, emphasis, pace contrast, pause placement) can only get self-check feedback in R1 | ARC-05 owner assesses G-01…G-05; until then, self-check and no improvised measures (FS-05 §5.10) |
| GAP-09 | Body language and visual presence cannot be detected | T-01 (visual), T-11, 10 m08 activities; ARC-05 §6 removed video presence; R1 captures audio only (ARC-05 §4.1) | These techniques are self-check only | Keep them in the Techniques collection as "practise in front of a mirror or camera" with a checklist (§8 Q-04) |
| GAP-10 | Suspected content mismatches with the cited sources | T-07 steps vs candidate video (§3.9); T-02 lever list vs third-party summaries; T-14 "3-2-1" scope; P-01 "adapted" plan vs candidate source | Re-expressing the code text could misrepresent what the video teaches | Re-derive steps from the video itself (§6 step 2), not from the code |
| GAP-11 | Placeholders and contradictions | T-13…T-15 "pending" (CAT:453-518); High-Low-Buffalo fully defined at A46:460 | Unclear whether these are channel techniques at all | Verify; if not found on the channel, treat as ORIGINAL or drop |
| GAP-12 | Shadowing needs new scripts and new reference audio | S-01…S-03 attributed to the creator; R1 reference audio is TTS (SRS-FR-SESS-012) | Creator audio or words cannot be used | Write original scripts (S-01…S-07) and use TTS references |
| GAP-13 | Creator-coined names appear as display names | §3.1 finding 4 | Branding exposure under D-08 | Neutral display names; taught name kept internal (ARC-03 §6.8) |
| GAP-14 | No editorial roles or capacity defined | ARC-03 §6.14 needs a reviewer and a second reviewer; no owner named | The §6 effort (≈ 38 person-days, §6.3) has no owner in the plan | Add to PLN-02 / EP-09 with named roles (pseudonymous `verifiedBy` ids) |

**Topics glimpsed incidentally in search results (unverified; not catalogued).** While looking up the cited titles, result snippets mentioned teaching on a siren-style voice warm-up, counting at even energy for vocal control, speaking faster without losing clarity, stopping "um", freezing mid-conversation, and talking to more senior people. None of these is in the code. They are listed only as leads for the GAP-02 catalogue and must be checked on the channel itself.

## 6. Verification and rewrite workflow (RECOMMENDATION)

### 6.1 Rules that apply to every step

1. **No ingestion.** No scraping, no caption or transcript download, no YouTube Data API, no automated tools that fetch video text (ADR-011; SRS-BR-028). The reviewer watches the video on YouTube like any viewer.
2. **No verbatim text.** Take notes in your own words while watching. Do not paste captions, quotes, titles or on-screen text into any user-visible field (ARC-03 V-13). Short phrases of a few words in internal `notes` are acceptable when needed to identify a moment.
3. **No branding.** No creator name, handle, likeness, channel art or coined technique name in user-visible fields (ARC-03 V-14). The taught name goes only in `provenance.techniqueNameAsTaught`.
4. **Original examples.** Every example, script and scenario is newly invented for SpeakCraft's tracks.
5. **Honest evaluation.** A technique's `targetMetrics` list only ARC-05 §6 keys; anything else becomes a self-check item (ARC-03 V-17).
6. **No match, no ship.** An item that cannot be matched to a specific video stays `PENDING_REVIEW` and is excluded from release builds (ARC-03 V-10), or is reclassified as `ORIGINAL` if it turns out to be general knowledge rather than channel-derived (§8 Q-01).

### 6.2 Steps per item

| Step | Action | Output (ARC-03 field) | Done when |
|---|---|---|---|
| 1 Find the video | Search the channel by hand, starting from the code's cited title and the §3.9 candidate | `sourceVideoId`, `sourceUrl` (`https://www.youtube.com/watch?v=<id>`) | The video is open and is on the channel |
| 2 Confirm technique and timestamp | Watch the relevant part; confirm the technique is taught there; note the start (and end) time; note any mismatch with the code | `timestampStart`, `timestampEnd`, `techniqueNameAsTaught` (internal), reviewer `notes` | Start time points at the teaching moment; mismatches recorded |
| 3 Rewrite | Write name, one-line idea, why it works, 2–7 steps and original examples in our own words, at the target level (D-17) | `name`, `summary`, `whyItWorks`, `steps` | No sentence traceable to the video's wording; reading level checked |
| 4 Record provenance | Fill the provenance block; add `&t=<seconds>s` to `sourceUrl` | `origin = CHANNEL_DERIVED`, `verifiedBy` (pseudonymous id), `verifiedOn` (YYYY-MM-DD) | ARC-03 V-11, V-12 pass |
| 5 Set licence status | Set `licenceStatus = ORIGINAL_REEXPRESSION`, `attributionText = null` | `licenceStatus`, `attributionText` | V-11 passes |
| 6 Link exercises and tips | Link ≥ 1 activity both ways; write ≥ 1 general tip and issue-triggered tips (start from §4.2); add self-check items for what audio cannot show | `relatedActivityIds`, `techniqueIds`, `tipIds`, `targetMetrics`, `selfCheckItems` | V-03, V-09, V-15, V-17 pass |
| 7 Second review | A second reviewer checks wording, examples, branding and evaluation honesty (TST-01 §9 honesty checklist) | `status = ACTIVE` | Both reviewers' ids recorded in `notes`; item passes `validateContentPack` |

### 6.3 Effort estimate (Proposed)

ASSUMPTIONS: the reviewer knows the channel; most cited videos can be found by title; durations exclude legal review, cataloguing videos not yet in the code (§6.4), translation, and any ARC-05 implementation work.

**Per class of item:**

| Class | Items | Hours per item (steps 1–7) | Total hours |
|---|---|---|---|
| Seeds with a cited title (T-01…T-12) | 12 | 4.0 base (find 0.5, confirm 0.75, rewrite 1.0, provenance 0.25, link 0.75, second review 0.5, slack 0.25), up to 5.0 where a mismatch or IP question must be resolved (per-technique table) | 53 |
| Pending seeds (T-13…T-15; T-15 may split into about 3 techniques) | 3 (≈ 5 resulting techniques) | 6.0 each; 12.0 for the T-15 series | 24 |
| Other named frameworks (T-16…T-37): decide origin first | 22 | 0.5 to decide; then +2.0 if original (2.5 in total) or +3.5 if a channel module cites it (4.0); 4.5 for T-36 (new scripts) | 63 |
| Catalogue activities (X-101…X-250): rewrite title, goal, prompt; set template, track, level, target range; link technique | 150 | 0.5 | 75 |
| Self-check checklists for non-speaking and visual activities | 33 | 0.25 | 8 |
| Exercises, prompts and scenarios (X-001…X-049) | 49 | 0.4 | 20 |
| Challenges (C-01…C-10) | 10 | 0.75 | 7.5 |
| New original scripts with markers (S-01…S-07) | 7 | 1.0 | 7 |
| Tips with triggers (≈ 3 per technique + 13 issue-code tips + 5 template general tips) | ≈ 120 | 0.2 | 24 |
| Programme content harvest (P-01 only; others retired) | 1 | 4.0 | 4 |
| Pack validation fixes and final consistency pass | — | — | 16 |
| **Total** | | | **≈ 300 h (≈ 38 person-days of 8 h)** |

**Per technique (Proposed):**

| Technique | Hours | Reason | Technique | Hours | Reason |
|---|---|---|---|---|---|
| T-01 | 4.0 | Title known; candidate found | T-20 | 2.5 | Likely general knowledge (STAR) |
| T-02 | 4.5 | Lever list to check against the video | T-21 | 2.5 | Likely general knowledge (BLUF) |
| T-03 | 4.0 | Strong candidate | T-22 | 2.5 | Likely general knowledge (SBI) |
| T-04 | 4.0 | Candidate with a different title | T-23 | 2.5 | Origin unknown |
| T-05 | 4.0 | Candidate found | T-24 | 2.5 | Origin unknown |
| T-06 | 4.5 | No candidate | T-25 | 2.5 | Origin unknown |
| T-07 | 5.0 | Content mismatch to resolve | T-26 | 2.5 | Origin unknown |
| T-08 | 4.5 | Title confirmed, no URL | T-27 | 2.5 | Origin unknown |
| T-09 | 5.0 | Third-party model to assess (§8 Q-02) | T-28 | 2.5 | Origin unknown |
| T-10 | 4.5 | No candidate | T-29 | 4.0 | Cited by a channel-root module (X-018) |
| T-11 | 4.5 | No candidate; self-check only | T-30 | 4.0 | Cited by a channel-root module (X-017) |
| T-12 | 4.5 | No candidate | T-31 | 2.5 | Stock phrase to replace |
| T-13 | 6.0 | Pending; may be general knowledge | T-32 | 2.5 | Origin unknown |
| T-14 | 6.0 | Pending; scope unclear | T-33 | 2.5 | Origin unknown |
| T-15 | 12.0 | Series placeholder; several techniques | T-34 | 4.0 | Cited by a channel-root module (X-016) |
| T-16 | 2.5 | Likely general knowledge (CCC) | T-35 | 4.0 | May be part of T-15 |
| T-17 | 2.5 | Likely general knowledge (FORD) | T-36 | 4.5 | Scripts must be newly written |
| T-18 | 2.5 | Likely general knowledge | T-37 | 2.5 | Origin unknown |
| T-19 | 2.5 | Likely general knowledge (VAKS) | | | |

Activities, exercises, challenges, scripts and tips use the per-class figures above.

**Cataloguing videos not yet in the code (§6.4):** about 1.5–3 hours per video to watch and log, plus 4 hours for each new technique found.

### 6.4 Cataloguing template (for videos not yet covered)

**Video log — one row per video watched.** Fill by hand; never paste captions or transcript text.

| Field | Fill in | Rule |
|---|---|---|
| Video ID | | 11 characters |
| Video URL | `https://www.youtube.com/watch?v=` | Specific video; never the channel, a playlist or a search URL (ARC-03 V-12) |
| Title (internal only) | | For the reviewer's reference; never shown in the app |
| Published date / length | | — |
| Reviewer id / date | | Pseudonymous id (no personal email); YYYY-MM-DD |
| Techniques taught (one line each, own words) | 1. … (start mm:ss – end mm:ss) | Own words; a few words at most from the video in `notes` |
| Already in this catalogue? | CNT-T-nn / new | Check §3.1 duplicates first |
| Exercises or activities shown | | Describe the task, then invent our own version |
| Tips given | | Rewrite as tips with a trigger (§4.1) |
| Can our checks detect the related mistakes? | ARC-05 metric / self-check / gap | Use §4.1; never invent a metric |
| Track(s) and level | TRK-… · L1–L5 | — |
| Decision | New technique / adds to CNT-T-nn / not usable | — |

**Technique record — one per technique (maps to ARC-03 §6.8 and §6.4):**

```
CNT ID / pack id        : CNT-T-__ / <slug>
Display name (ours)     : (≤ 60 characters, no creator-coined name)
Name as taught          : (internal provenance only, ≤ 80)
One-line idea           : (≤ 140)
Why it works            : (≤ 400)
Steps                   : 1. … (2–7 steps, ≤ 200 each)
Original example(s)     : …
Tracks / levels         : TRK-… / L_–L_
Target metrics          : (ARC-05 §6 keys only, or empty)
Self-check items        : (≤ 6, ≤ 120 characters each)
Tips                    : tip id · trigger (issueCode | metricKey+band | techniqueId | templateId) · text
Related activities      : (≥ 1; each lists this technique back)
Provenance              : origin = CHANNEL_DERIVED
                          sourceVideoId = ___________   sourceUrl = https://www.youtube.com/watch?v=___&t=___s
                          timestampStart = ___ s        timestampEnd = ___ s
                          verifiedBy = ______           verifiedOn = YYYY-MM-DD
licenceStatus           : ORIGINAL_REEXPRESSION     attributionText = null
Checks                  : [ ] no verbatim text  [ ] no branding  [ ] honest metrics  [ ] second reviewer: ______
```


## 7. Removal list (D-08)

Everything below must not reach the new app, its content pack, notifications or store listing (SRS-BR-028; ARC-03 V-14). Most of it disappears with the components the redevelopment replaces (ARC-02 §10; ARC-05 §2 table); this list is the checklist for confirming that. Line numbers are VERIFIED against the current code.

### 7.1 Creator name or handle in `app/src/main` (171 lines)

**Content and user-visible text (101 lines)**

| File | Lines | What |
|---|---|---|
| CAT | 182 | Module subtitle "…timed @askvinh sprints" |
| CAT | 195, 197, 217, 219, 226, 241, 263, 307, 329, 351, 373, 395, 417, 439 | Seed advice, context and one video title naming the creator or handle |
| CAT | 460, 462, 463, 464, 469, 482, 484, 485, 486, 491, 504, 506, 507, 508, 513 | Pending-seed placeholders (also instruct users to run the ingestion tool) |
| CAT | 888, 889, 890, 891, 892, 893, 894, 895, 896, 897 | Challenge `sourceReference` labels; C-08 title and prompt (895) |
| CAT | 931, 941, 951 | Shadowing source titles |
| CAT | 994, 995 | Daily-routine step descriptions |
| CAT | 1005, 1024, 1028 | "Adapted from…" programme subtitle; week-2 focus and Day 10 |
| FTR | 46, 55, 90, 112, 126, 138, 145, 196, 223, 230, 233, 266, 298, 300, 315, 317, 329, 334, 366, 374, 417, 424 | 22 tutorial strings |
| DFT | 210, 229 | Feedback explanation and suggestion |
| RE | 73 | Recommendation reason |
| CVR | 192 | Camera overlay text |
| NAV | 181 | Bottom-tab label "@askvinh" |
| CC | 778 | Badge "Creator Framework (@askvinh)" |
| ADM | 125, 251 | Admin console header; default challenge hint |
| AO | 146, 199, 432 | Sign-in tagline, feature bullet, onboarding text |
| CM | 634, 635 | Mentor quick prompts |
| DP | 128, 292 | Reflection label; programmes subtitle |
| D | 299, 647 | Dashboard tile and module subtitle |
| LAT | 385, 461, 927 | "Watch @askvinh" button; section headers |
| LM | 98, 130, 140, 276, 636 | Lesson source fallbacks; module subtitle; empty state; voice-lab subtitle |
| SAC | 408, 1001 | Audit playback label; Challenges header |
| YT | 109, 238, 271 | Ingestion screen subtitle, search hint, empty state |
| REPO | 583 | Stored status text "Verified @askvinh public framework catalog" (shown in the YT screen) |

**URL and handle values (32 lines)**

| File | Lines |
|---|---|
| CAT | 205, 227, 249, 271, 293, 315, 337, 359, 381, 403, 425, 447, 470, 492, 514, 932, 942, 952, 971, 972, 973, 974, 975, 976 |
| REPO | 575 (`channelHandle`), 1006 (real-world task `sourceVideoUrl`) |
| FM | 141, 193, 248 (model defaults) |
| ADM | 116, 216 |
| YT | 79 (default input URL) |

**Code identifiers and comments (38 lines)** — not user-visible, but they keep the brand in the codebase and in Firestore document IDs (`vinh_*`, `askvinh_*`)

| File | Lines |
|---|---|
| CAT | 185 (comment), 187, 189, 190, 211, 212, 233, 234, 255, 256, 277, 278, 299, 300, 321, 322, 343, 344, 365, 366, 387, 388, 409, 410, 431, 432, 454, 476, 498, 522 |
| DFT | 57 (KDoc) |
| RE | 59 (route `lesson_detail/lesson_vinh_prep`) |
| REPO | 532, 537 (`seedAskVinhKnowledgeBaseIfEmpty`, `askVinhKnowledgeBase`) |
| W | 212 (route `lesson_vinh_3stage_audit`) |
| LM | 113 |
| MVM | 189, 230 |

**Related branding (no name, but creator-specific):** the "@askvinh" tab's screen titles "YouTube Intelligence" / "YouTube Channel Intelligence" (NAV:200, 1344; YT:108); the badge states "Creator Framework" and "Pending Transcript Verification" (CC:777-778); creator-coined technique names used as UI labels (for example LM:636, FTR:193, SAC:408-417, CVR:192, A710:281, 546-565, A13:1104) — see §3.1 finding 4.

### 7.2 Store and metadata text

| File | Line | Text | Action |
|---|---|---|---|
| `metadata.json` | 3 | Description says the app is built from the channel's lessons (and claims server-side Gemini, line 5) | Rewrite without the creator and without cloud AI (D-04) |
| Play Store listing | — | Not in the repository (ASSUMPTION) | Check against the V-14 blocklist when written; PRD-FR-057 |

### 7.3 Per-user Firestore seeding and ingestion stores (retire)

| Item | Location | What it does | Action (ARC-03 §1.4, §8; ADR-011) |
|---|---|---|---|
| `seedAskVinhKnowledgeBaseIfEmpty` | REPO:532-597; called at MVM:189 (init) and MVM:230 (after onboarding) | Copies all 15 seeds into `users/{uid}/extractedTechniques` and writes `users/{uid}/youtubeSources` with `channelHandle "@askvinh"` (REPO:575) and the status text at REPO:583 | Remove; no new writes |
| Per-user ingestion saves | REPO:937-964 | Writes analysed videos and extracted techniques per user | Remove with the YT pipeline |
| Reads and UI flows | REPO:179-195; MVM:124-127; NAV:413-414 | Stream both collections to the UI | Remove |
| Shared catalogue | REPO:345, 430, 1106, 1137-1140 (`extractedTechniques`) | Admin-published caption-derived techniques | Retire; candidates only, caption text never migrated |
| Ingestion pipeline | LIO:820-910 (verbatim caption sentence as advice at LIO:869; invented timestamps at LIO:880, 903); YT screen; ADM YouTube sync (ADM:116-260); YouTube API key setting (ui_audit §1.8) | Scrapes captions and turns sentences into "techniques" | Remove (ADR-011) |
| Real-world task logs | REPO:1006 | Stores the channel URL on every task log | Remove field |
| Security rules | `firestore.rules`:517 (`youtubeSources`), 543 (`extractedTechniques`), 564 (`realWorldTasks`), 792 (`youtubeChannels`), 807 (shared `extractedTechniques`) | Allow these collections | Lock to read/delete only during the migration window, then remove (ARC-03 §8) |
| Account deletion list | REPO:1296 | Includes both per-user collections | Keep until existing user copies are deleted, so users can still erase them |
| Existing user documents | `users/{uid}/extractedTechniques` | May contain caption sentences copied verbatim (LIO:869) | Never migrate; delete per ARC-03 §8/§9 |

### 7.4 Tests and fixtures (go with the retired code)

`app/src/test/java/com/example/CurriculumAndRecommendationTest.kt`:32, 79 · `SpeakCraftComposeCujTest.kt`:179, 194, 197 · `YouTubeParsingTest.kt`:23, 26, 35, 46 · `firestore.test.js`:222, 237. New tests should assert the opposite: the V-14 blocklist finds no creator name, handle or coined name in any user-visible string or pack field.

**Count of creator-name strings to remove:** 171 lines in `app/src/main` (101 user-visible or content, 32 URL or handle values, 38 identifiers or comments), plus 1 line in `metadata.json` and 11 test lines — **183 lines in total**.

## 8. Open questions and issues found in source documents

### 8.1 Open questions for the Product Owner

| ID | Question | Why it matters | Suggested answer (RECOMMENDATION) |
|---|---|---|---|
| Q-01 | For frameworks that are also taught widely elsewhere (PREP, STAR, BLUF, SBI, FORD, CCC, High-Low-Buffalo, VAKS, Match & Mirror), do we record channel provenance, or treat them as `ORIGINAL` general knowledge? | Determines whether V-11 provenance is needed and whether a "Watch the original lesson" link appears | Use `CHANNEL_DERIVED` only when a specific channel video is what we actually re-express; otherwise `ORIGINAL` with no link (FS-06 A4) |
| Q-02 | Third-party summaries say the four-colour model is based on a book by another author. May we include it at all? | Possible third-party IP beyond the creator | Legal check; if kept, use neutral wording and no colour-brand names, or drop T-09 |
| Q-03 | May common acronyms (PREP, STAR, BLUF, SBI) be shown as display names? | ARC-03 §6.8 requires names "in our words" | Allow generic industry acronyms; rename creator-coined names (§3.1 finding 4) |
| Q-04 | Are self-check-only techniques (T-11, body-language activities) acceptable in the Techniques collection and in Today? | No audio signal; R1 has no video | Show them in the collection; exclude from Today unless the user picks them; link to OQ-A11Y-1 for users who cannot speak |
| Q-05 | Do we add a "listen, then answer" step (TTS passage + free answer) in R1, or move the 9 listening activities to R2? | No R1 template plays a passage before a free answer (G-12) | Move to R2 with TPL-ROLE unless FS-04 can add a TTS prompt cheaply |
| Q-06 | Confirm that the 30-day and other programmes are retired, and that only their day content is reused | P-01 claims to adapt the creator's plan; candidate source looks different | Retire (FS-06; RD-16 §5); reuse days as activities |
| Q-07 | Who are the reviewer and second reviewer, and where is the ≈ 38 person-days of effort planned? | ARC-03 §6.14 needs both; no owner today | Assign in PLN-02 / EP-09 |
| Q-08 | Should the channel be catalogued before R1 content freeze, and in what order? | GAP-02; coverage of "all" techniques cannot be shown otherwise | Catalogue teaching videos first (not shorts), using §6.4 |
| Q-09 | For time-boxed exercises (60 s sprint, 30 s update), should going over time be a "mistake"? | ARC-05 §6: above the upper bound is not a mistake in R1 | Keep as is for R1; revisit after usage data |
| Q-10 | C-05 asks for 180 s; templates cap at 120 s (SRS-FR-SESS-003) | ARC-03 §6.7 allows per-activity `maxSeconds` | Shorten to 120 s rather than override |
| Q-11 | Are any web-search candidates in §3.9 already known to the Product Owner to be correct or wrong? | Saves reviewer time | Product Owner marks them before step 1 |

### 8.2 Issues found in source documents

| ID | Document | Issue | Suggested owner action |
|---|---|---|---|
| I-01 | ARC-05 §8.4 | Candidate acoustic metrics are stated as "T1 only, because they need app-owned PCM", but ARC-05 §3 gives T3b app-owned `AudioRecord` capture too | ARC-05 owner: state whether T3b is included or why not |
| I-02 | ARC-05 §6 vs §7, §8.4 | M-PAUSE's templates column omits READ, but §7 makes it a TPL-READ headline and ISSUE_PAUSES_LONG applies to READ | Add READ to M-PAUSE's templates |
| I-03 | ARC-05 §6 band table | M-PAUSE has only an upper issue threshold; pause-based techniques cannot be told "too few pauses" | Consider a low band for pause-technique activities (G-05) |
| I-04 | ARC-05 §6; ARC-03 §6.7 | M-STRUCT and `framework` support only STAR and PREP; the catalogue has about ten further structured frameworks (G-06) | ARC-05/ARC-03 owners decide which lexicons to add |
| I-05 | ARC-02 §3.1 vs ARC-03 §6.4 | ARC-02 `LicenceStatus { ORIGINAL, ORIGINAL_REEXPRESSION, OPEN_LICENCE, PENDING_REVIEW }` differs from ARC-03 `licenceStatus { LICENSED, ORIGINAL_REEXPRESSION, PENDING_REVIEW }` plus a separate `origin { ORIGINAL, CHANNEL_DERIVED, THIRD_PARTY }` | Align ARC-02 to ARC-03 (ARC-03 owns the schema) |
| I-06 | ARC-03 | References §11.20 and §11.22 (enumerations, issue codes), which were not present when this audit was written (the document ends at §8 at the time of reading; it appears to be in progress) | ARC-03 owner: add or re-point |
| I-07 | ARC-05 §7 | TPL-REPEAT headline "M-SPK vs reference duration" has no issue code, so shadowing cannot report "too fast/slow vs the model" | Add an issue code or a band |
| I-08 | data_learning_audit §2.3 vs ui_audit §1.8 | 102 + 22 + "about 15 UI files" (data audit) vs "149 source lines" (UI audit). Both are consistent with this audit once the pattern is fixed: 147 lines match "askvinh" in `app/src`; 171 lines in `app/src/main` match "vinh" | None; this audit's counts supersede (§7) |
| I-09 | Code (for ARC-03 §1.4) | High-Low-Buffalo is "pending, do not invent" in CAT:453-474 but fully defined in A46:460-482 | Record in the T-13 verification |
| I-10 | Code thresholds vs ARC-05 | Feedback code uses 175/180 wpm "fast" (DFT:151, 213; LFE:395) and 120–170 "good" (DFT:91); ARC-05 band is 100–160 | Retired with the old engines; tips must use ARC-05 bands |
| I-11 | Code vs ARC-05 | LFE:278-281 still gives pitch feedback from the removed Hz-spread measure | Retired; do not port (K-31) |
| I-12 | ARC-03 §6.13 | Minimums per track cannot be met from current content for TRK-INTERVIEW (and warm-ups for TRK-EVERYDAY/TRK-WORK, main tasks for TRK-CLEAR) | Content plan must add items (GAP-07) |

---

*End of CNT-01.*
