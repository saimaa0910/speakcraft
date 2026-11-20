# UX Audit — Current SpeakCraft Android App

| Field | Value |
|---|---|
| Document ID | DES-01 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UX Researcher |
| Approval | Pending — Product Owner |
| Related | RD-03 Source Code Audit (FND-*), RD-13 Vision, RD-15 Principles (PP-n, UX-n), RD-16 Scope (CAP-nn, NG-nn), DES-02 Information Architecture, DES-03 Screen Inventory, DES-05 Design System, DES-06 UI/UX Specifications, DES-07 Accessibility Guidelines, DES-08 Redesign Decisions |

---

## 0. Purpose and scope

This document evaluates the user experience of the **current** SpeakCraft app (branch `main`, HEAD `68a9cb1`). It gives the redesign a shared, evidence-based list of what is wrong, how serious it is, and what the new design does instead. It also provides:

- a heuristic review checklist (§5) that is reused to review every new design (referenced by RD-15 UX-6);
- a list of current UI qualities to keep (§6).

The audit covers all user-facing UI in `app/src/main/java/com/example/ui/` plus `MainActivity.kt`, the manifest and `res/values*`. Server-side, data and intelligence defects are covered in RD-03. They appear here only where users can see them.

## 1. Method and limitations

### 1.1 Method

| Step | What was done |
|---|---|
| Evidence base | Stage A UI-layer audit working paper (cited as **UIA §n**). It hand-reads about 22,000 lines of UI code, 5 ViewModels, the manifest, resources and the 5 Compose test files. Its findings are consolidated into RD-03 as `FND-UI-*`. |
| Spot re-verification | For this document the auditor re-read these cited lines in source: `W:164`, `REPO:494`, `CC:1268`, `NAV:181`, `SAC:150-154`, `CC:111-124`, `W:282`, `VO:241`, `SAC:623`, `AS:103-108`, `MainActivity.kt:14-19`. Every one matched. The `labelSmall` count was re-run: 103 usages plus 1 definition. |
| Contrast computation | WCAG 2.x relative-luminance contrast was computed by script for the current colour tokens in `ui/theme/Color.kt` and the pairings declared in `ui/theme/Theme.kt` (UXF-41). |
| Heuristic evaluation | Nielsen's 10 usability heuristics (§2), applied by a single evaluator to the code-derived screen behaviour. |
| Categorisation | Findings are grouped into 13 categories (§3). Each has a severity, evidence, user impact and a redesign response tied to RD-15 principles and the DES-02 IA. |
| Walkthrough | A cognitive walkthrough of a new user's first five minutes, reconstructed from code paths (§4). |

### 1.2 Limitations (read before using this audit)

| # | Limitation | Consequence |
|---|---|---|
| L-1 | **Static code review only.** The app was not built, installed or run. No JDK or Android SDK was available (GOV-02 DC-4). | Visual and runtime effects such as clipping, colour jumps and spinners are **INFERRED** from code unless stated. |
| L-2 | **No device testing** with TalkBack, font scaling, Switch Access, or on low-end devices. | Accessibility findings are code-level. DES-07 defines the device tests that must confirm them. |
| L-3 | **No user research data exists.** There are no interviews, usability tests, surveys, app-store reviews, support tickets, analytics or crash data. The app has no analytics or crash reporting (RD-03 FND-OPS-05). | Severity ratings are **expert judgement**, not measured frequency. No finding claims "users said" or "x % of users". |
| L-4 | **Single evaluator.** Heuristic evaluations normally use 3–5 evaluators. | Some issues are probably missed. §5 is designed for multi-reviewer use in Stage B. |
| L-5 | Time and tap counts in §4 are counted from code paths, not measured. | They are **INFERRED** minimums. |

**RECOMMENDATION:** Before R1 UI build starts, validate the top findings and the new flows (DES-04) with moderated usability sessions: 5–6 participants from each primary persona (RD-08 P1/P2) using a clickable prototype. Also add the in-app "Was this accurate?" rating (CAP-05) as the first source of real feedback data.

### 1.3 Severity scale

| Severity | Definition |
|---|---|
| **Critical** | Blocks a core task, locks users out, causes data loss, presents fabricated performance or privacy information as fact, or creates legal or privacy exposure. |
| **High** | Causes major confusion, loss of trust or loss of work for most users; no reasonable workaround. |
| **Medium** | Noticeable friction or inconsistency; a workaround exists. |
| **Low** | Cosmetic or polish issue. |

### 1.4 Labels and citations

- **[V] VERIFIED**: seen in source at the cited `ALIAS:line`.
- **[I] INFERRED**: a reasoned consequence that was not observed running.
- Path aliases are listed in Appendix A. `UIA §n` refers to the UI audit working paper section.
- `FND-*` IDs are RD-03 findings.
- Redesign responses cite RD-15 principles (`PP-n`, `UX-n`), RD-16 capabilities and non-goals (`CAP-nn`, `NG-nn`), DES-02 screens (`SCR-*`) and open decisions (`D-nn`).

---

## 2. Heuristic evaluation (Nielsen's 10)

Rating: **Fails** (several High or Critical violations), **Partially meets** (mixed), **Meets**.

| # | Heuristic | Rating | Concrete findings (current app) | UXF refs |
|---|---|---|---|---|
| H1 | Visibility of system status | **Fails** | Mic permission denial produces no feedback (SAC:150-154) [V]. Analysis can spin forever offline (SVM:509-535) [I]. Loading and Error are collapsed into "empty" for every list (NAV:513-521) [V]. Recording keeps running after the user leaves a lab [V]. Settings shows capability "ready" without verification (FND-AI-05). *Positive:* live waveform, 3-step indicator in Record & Review, "Partner is responding…" text. | 36, 37, 49, 50, 60 |
| H2 | Match between system and the real world | **Fails** | Developer terms on screen: "CameraX video or Photo Picker (<19MB)" (SAC:623), "Status: INSUFFICIENT_EVIDENCE" (CC:1268), "Local Whisper / Moonshine TFLite" (AS:785-902) [V]. Creator frameworks shown without explanation (LM:636, SAC:408) [V]. A phone-call metaphor for one-shot dictation (CM:368-373) [V]. "AI" labels on template code (UIA §1.3) [V]. | 40, 53, 54, 55 |
| H3 | User control and freedom | **Fails** | A mandatory update dialog cannot be dismissed on any route (UD:75-77, 319) [V]. The launch overlay cannot be skipped (LA:71-75) [V]. Onboarding has no skip (UIA §3.2) [V]. Programs have no unenroll (DP:278-352) [V]. Back or Skip ends the whole tour (NAV:1252-1268) [V]. *Positive:* confirmation before wiping history (AS row 27). | 02, 11, 25, 30 |
| H4 | Consistency and standards | **Fails** | One concept carries up to 7 names (§3.4.1). There are 12 start-recording labels (UIA §7.2) [V]. Navigation chrome differs by width: 5 vs 10 items with different labels (NAV:177-183 vs 1338-1347) [V]. Two feedback cards use different formulas (CC:1326-1842 vs ETR:645-787) [V]. FilterChips act as buttons (CM:724-726) [V]. | 06, 15, 16, 34, 40 |
| H5 | Error prevention | **Fails** | "Analyze" can be pressed before recording (SVM:472-497) [V]. Submit buttons stay enabled while loading (SAC:508, 960) [V]. Blank onboarding inputs are accepted (REPO:484-485) [V]. A lab can evaluate another lab's recording (SVM:594-613) [V]. | 04, 28, 37, 39 |
| H6 | Recognition rather than recall | **Partially meets** | 26 routes, with many reachable only from a 22–23-tile directory (D:285-307) [V]. Settings is not on the phone chrome (UIA §1.3 row 22) [V]. Track cards silently filter a grid far below (D:313, 576) [V]. *Positive:* Quick Access tiles and search do exist. | 06, 09, 10 |
| H7 | Flexibility and efficiency of use | **Fails** | Every launch costs 2.1–2.9 s (LA:71-75) [V]. Daily practice sends users to 7 separate screens and back to tick (DP:262) [V]. A transcript requires a buried Settings toggle (AS row 8) [V]. *Positive:* a returning user can reach a recorder in 2 taps from the dashboard. | 02, 05, 31 |
| H8 | Aesthetic and minimalist design | **Fails** | The dashboard has about 21 widgets, about 70 targets and 25 routes (UIA §3.4) [V/I]. There are 8 pastel swatch pairs, hero gradients and an inline guide strip on 23 screens (Color.kt:72-111; FT:70-174) [V]. | 14, 19, 20, 21 |
| H9 | Help users recognise, diagnose and recover from errors | **Fails** | Raw `localizedMessage` appears on sign-in failure (AO:225-233) [V]. "Try Again" buttons only dismiss (SAC:641; AU:131-138) [V]. Profile error is a dead end (NAV:490-494) [V]. *Positive:* retry cards in some labs; the evidence gate refuses to score bad audio (FND-AI-17). | 35, 39, 51 |
| H10 | Help and documentation | **Partially meets** | Help is plentiful but duplicated across 4 mechanisms (UIA §8.2). The help icon shows the *dashboard* guide on 4 routes (FTR:437-438) [V]. The `auth` guide promises "live Gemini AI coaching", which contradicts the local-only policy (FTR:29) [V]. | 10, 14, 30 |

**Overall:** 8 of 10 heuristics **Fail**; 2 **Partially meet**. The strongest existing qualities are honesty mechanisms (the evidence gate) and the visual token discipline (§6).

---

## 3. Findings register

Columns: **Sev** = severity (§1.3). **Evidence** carries [V]/[I] labels. **Response** = what the redesign does (all responses are RECOMMENDATIONS until approved).

### 3.1 Unnecessary steps

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-01 | Google sign-in is mandatory before any use. There is no way to try the app first. | High | NAV:249-250; AO:79-304; only `GetSignInWithGoogleOption` (AH:98) [V]; FND-SEC-01 | Users without a Google account, or who are unwilling to sign in, cannot start. This is a first-run drop-off point [I]. | Account optional (D-05; CAP-01; PP-6). Sign-in is offered later from Settings → Account (SCR-SET-07, SCR-ACC-01). |
| UXF-02 | An unskippable launch overlay plays on **every** launch: 2.9 s first, 2.1 s returning. | Medium | LA:71-75; no skip, tap handler or BackHandler (LA:251-658) [V]; touches likely pass through [I] | Daily friction for returning users. Accidental taps on the screen beneath. | A brand moment of at most 1 s, using the platform splash, that respects reduced motion (RD-16 §5; DES-05 §8; UX-2). |
| UXF-03 | Onboarding asks 3 steps of form input: 4 levels, a free-text goal, minutes as chips **and** a duplicate slider, and **7 self-rating sliders**. It contains **no speaking**. | High | AO:413-417, 509-529, 555-561 [V] | Effortful, abstract self-assessment before any value. The first spoken attempt comes several screens later (§4). This contradicts PP-4. | Short onboarding (SCR-ONB-01…08): goal, level, daily time, reminder, privacy and mic, then a **first attempt inside onboarding** (CAP-02; PP-4; UX-2). |
| UXF-04 | Recording and analysis are separate steps. "Analyze" is a second button on every lab, and it can be pressed before anything is recorded. | Medium | 8 analyze labels (UIA §7.2); analyze-before-record shows an insufficient-evidence card (SVM:472-497) [V] | An extra tap on every attempt. The wrong order leads to a confusing failure. | Stop recording triggers the evidence check and feedback automatically (CAP-04; SCR-SESS-01 processing → result). |
| UXF-05 | Daily practice is an 8-step checklist that sends the user to 7 different screens and back, where they tick steps by hand. | Medium | DP:59-214, 262; CAT:989-998 [V] | Navigation overhead. Users lose their place. Completion is not tied to doing the task. | One Today session (SCR-TODAY-01) of 3–4 steps run inside a single session player, advancing automatically (CAP-03). |

### 3.2 Confusing navigation

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-06 | **26 routes.** The phone bar exposes 5. The ≥ 840 dp sidebar exposes 10 with *different* labels ("Practice" vs "Daily", **"Tools" → Vocabulary**). The rail (600–839 dp) cannot reach 5 secondary destinations except through the dashboard. | High | NAV:915-1236, 177-183, 1338-1347, 883 [V]; FND-UI-01 | The app looks different on each device. Features are hard to find and to name. | 4 persistent destinations, identical at every width: Today, Practice, Words, Progress (DES-02 §2.1; UX-8). |
| UXF-07 | One of five primary tabs is **"@askvinh"**. It opens a YouTube channel-ingestion tool that scrapes captions, shows heuristic extracts as "verified", and sends users out to YouTube. | Critical | NAV:181; YT:63-302; YT:321; LIO:847-907 [V]; FND-DOM-09, FND-AI-11 | A primary slot gives ordinary users no task to do. It carries third-party IP risk and presents false provenance. | Remove (RD-16 §5; PP-8; NG-08; D-08). Original content ships in Practice (CAP-06). |
| UXF-08 | Back-stack behaviour is unpredictable. Sidebar "top-level" items push with a back arrow. "Next Lesson" and "Continue Next" chains are unbounded, and "Next Lesson" silently crosses modules. "Browse All" pushes a duplicate. No tab is highlighted on secondary screens. | High | NAV:511, 595-603, 746, 1081, 1233-1234, 837, 884; CAT:524 [V] | Users get lost and must press Back many times. They cannot tell where they are. | Nested graph per tab with `saveState`/`restoreState`. Tab highlighted by hierarchy. Maximum depth 3. Back pops one level (DES-02 §2.3; UX-8). |
| UXF-09 | Settings and Profile are hard to find. Settings has 9 entry points, none on the phone's primary chrome. The dashboard avatar callback is dead. | Medium | UIA §1.3 rows 22–23; D:162; W:135 [V] | Privacy and appearance controls are hard to reach. | Avatar on every primary destination's top bar → Profile → Settings (DES-02 §2.2). |
| UXF-10 | Links don't go where their labels say. Hero "The 3 Vs of Communication" opens a different lesson. "Start Lesson" opens labs. Track cards only filter a grid far below and also restrict search. The help icon shows the dashboard guide on 4 routes. | Medium | W:209-212, 220-230; D:313, 576; FTR:437-438 [V] | Users lose trust in labels and feel lost. | Every card has one labelled destination (DES-05 §11.3). Content IDs come from one catalogue (CAP-06). No hidden filters. |
| UXF-11 | A **mandatory update dialog** blocks every route: "Later" is hidden, and Back and outside taps are ignored. In-app install likely cannot succeed, and the fallback tells users to uninstall and sideload. | Critical | UD:75-77, 319; NAV:646-655; MAN:5-8; AU:462-466 [V]; install failure [I]; FND-OPS-04 | Users can be **locked out** of the app. Uninstall advice risks data loss. | Remove the in-app updater (D-03; NG-11). Updates come from the store or distribution channel. No blocking dialogs at launch (DES-05 §11.5). |

### 3.3 Repeated actions and duplicate features

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-12 | **10 recording surfaces** (12 with dictation) run the same pipeline. There are ≥ 10 entry points to Record & Review alone and more than 30 paths that lead to a recorder. | High | UIA §7.2, §7.3, §1.7; all SVM-based labs call `submitForAiAnalysis` (SVM:445-556) [V] | Users must learn many screens that do the same thing. Behaviour differs per screen (pause, retry, transcription). | **One** session player (SCR-SESS-01). Labs become tracks and templates (CAP-04, CAP-06; PP-5). |
| UXF-13 | Parallel systems: 3 daily plans, 3 "path/track" concepts, 2 module taxonomies, 2 vocabulary stores, 2 streak sources, 2 feedback cards, 2 notification UIs (one dead). | High | UIA §7.3; FND-DOM-01 [V] | Numbers disagree between screens (for example streak on dashboard vs profile, W:703 vs P:153-156). Users can't tell which plan is "the" plan. | One plan (CAP-03), one track model (CAP-06), one vocabulary store (CAP-07), one streak (CAP-08), one feedback card (DES-05 §11.15). |
| UXF-14 | Four tutorial mechanisms repeat the same content: auto tour, first-use spotlight, help dialog (two instances that can stack) and an inline guide strip on 23 screens. | Medium | UIA §8.2; NAV:606 and D:172-178; FT:70-174 [V] | Interruptions and visual noise. Users learn to dismiss everything. | Self-explanatory screens plus at most one contextual first-use tip per screen (DES-06 §2.4; DES-02 §5). |

### 3.4 Inconsistent labels

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-15 | One concept, many names (see §3.4.1). | High | UIA §7.4 [V] | Users cannot build a mental model or follow instructions ("open Vocabulary" → the sidebar calls it "Tools"). | Canonical vocabulary and banned terms (DES-05 §12; DES-02 §6; UX-1). |
| UXF-16 | Action labels are inconsistent: 12 start-recording labels, 8 analyze labels and 5 stop labels. Only 3 record buttons show a mic icon. | Medium | UIA §7.2 (for example "Record Mic" SAC:352, "Speak Drill" LAT:598, "Practice Drill" LM:596) [V] | The same action looks different everywhere. | One Record button component with fixed labels (DES-05 §11.12). |
| UXF-17 | Onboarding and profile options don't match. Onboarding offers 4 levels, the profile 5. "Upper-Intermediate" vs "Upper Intermediate" means no chip is selected. Goal chips differ, and minute options differ (25 is missing; 60 is coerced). | Medium | AO:351, 389-392, 509-513; P:75-94, 119, 332 [V] | A user's own choice appears unselected or changed. Trust drops. | One level taxonomy (D-17, 5 levels) and one options source shared by onboarding and Settings (FS-02, FS-10). |
| UXF-18 | Labels contradict behaviour. "9-step" routine has 8 steps. "5 Vocal Foundations" has 4. "Listen 0.75x" plays at 0.72. "Shadow & Repeat" is not simultaneous. "Compare AI" does not compare. "Card x of N due today" counts non-due cards. "Watch @askvinh" opens the channel home page. | High | CAT:170 vs 989-998; LM:636 vs 534-541; LAT:143; LAT:374; LIO:938-943; VO:224, 236 [V] | Each mismatch erodes trust in every other claim in the app (PP-1). | Labels are written from behaviour and covered by UI tests. Content review against DES-05 §12. |

#### 3.4.1 Terminology table (current state)

| Concept | Names used today (evidence) | Canonical term (DES-05 §12) |
|---|---|---|
| Home screen | "Home", "Home Coach Dashboard", brand lockup (NAV:177, 705-736; TO:109) | **Today** |
| Daily plan | "Daily", "Practice", "Daily Practice", "Daily Communication Practice", "Daily Speaking Practice", "Daily Plan", "Today's Practice Plan", "9-step routine" (NAV:177, 1340; DP:153; CAT:170; W:570) | **Today's session** |
| Free-speech recorder | "Record", "Record & Review", "Record and Review Studio", "Record and Review", "Speaking Lab", "Quick Practice" (SAC:621; CAT:181; W:441) | **Session player** (internal); users see **Practice** → activity → **Record** |
| Vocabulary | "Vocabulary Builder", "Vocabulary Development", **"Tools"** (NAV:1345; CAT:171) | **Words** |
| Paths and programs | "Learning Paths", "Learn", "30/60/90 Plans", "Structured Learning Programs", "10 Paths", "My Learning Tracks" (DP:291; D:297; W:1435; ADS) | **Track** |
| Activities | "Activity Engine (150)", "150 Activities", "Activity Engine", "SpeakCraft Universal Activity Engine", "Activity Studio" (NAV:535, 1339; ADS:150) | **Activity** |
| Progress | "Progress", "Progress Analytics", "Analytics" (NAV:1346; D:300) | **Progress** |
| Coach and feedback | "Local AI Coach", "SpeakCraft Coach", "AI Check", "Presentation AI", "Compare AI", "AI Partner", "SpeakCraft AI Mentor", "Get AI Feedback" (UIA §7.4) | **Feedback** (no "AI") |
| Metrics | "Practice Hours" "x h" vs "Practice Time" "x hrs"; "{n} Day Streak" vs "Active Streak"; "Score N" vs "Score N/100" (D:764; P:417) | **Practice time**, **streak**; no composite score for free speaking (D-06) |
| Profile fields | "Current English Speaking Level" vs "Communication Level"; "Primary Communication Goal" vs "Primary Goal" vs "Focus Goal"; "Daily Practice Commitment" vs "Goal" vs "Target" (AO:450, 475; P:332) | **Level**, **Goal**, **Daily time** |
| Admin | "Admin Console", "Admin Studio", "Open Admin & Content Studio" (NAV:201; D:303; AS:506) | Removed (NG-10) |
| YouTube tool | "@askvinh", "YouTube Intelligence", "@askvinh Library", "YouTube Channel Intelligence", "YouTube Library" (NAV:181, 200; D:299; YT:108; LM:139) | Removed (NG-08) |

### 3.5 Overloaded screens

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-19 | The **dashboard** has up to 16 top-level items, about 21 widgets, about 70 tappable targets and 25 route strings, across roughly 5–6 phone screens of scroll. It has three "today" widgets, two conflicting weekly goals ("Weekly Goal: 5 h" vs "x / 5 days"), and Quick Access duplicates 7 directory tiles. | High | UIA §3.4; D:327-674, 766; W:937 [V]; scroll length [I]; FND-UI-02 | Decision paralysis. The next step is unclear (PP-3 violated). | Today shows **one session card, one Start button**, a streak week strip and a short progress note (SCR-TODAY-01; PP-3; UX-6). |
| UXF-20 | **Record & Review** packs mic, video and upload capture, a 3-stage self-audit player, manual transcription, prompt presets, live transcript, analysis, and history of every lab into one screen. | High | SAC:103-790 (688-line composable) [V] | Hard to understand what to do first. High chance of error. | Session player with sequential states (prompt → prepare → record → processing → result). History moves to Progress (SCR-PROG-03). Video removed from R1 (RD-16 §5). |
| UXF-21 | **Settings** has 29 controls. Engine diagnostics come first on phones. There is a permanently disabled "Cloud AI Coach" switch, dead tier chips and a plaintext API-key field. | High | AS:394-1068, 1057-1064, 658-713, 946-953 [V] | Users face developer concepts before useful settings, and privacy controls are buried. | Settings is reorganised into 7 user-facing groups (SCR-SET-02…08). Diagnostics move to About → Share diagnostics (DES-02 §5). |
| UXF-22 | The Activity library holds 5 sub-products behind one route ("All 150 / Daily Plan / 10 Paths / Saved & Recent / Analytics"). App Updates mixes OTA, backups, history and developer setup text. | Medium | ADS:74-78; AU:78-253 [V] | Duplicate concepts inside one screen. | Practice library (SCR-PRAC-01…03). The updater screen is removed (D-03). |

### 3.6 Unclear progress

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-23 | Progress counts **activity, not ability**. "Lessons Completed x / 12" is shown. "Practice Hours" is computed from 3 minutes per ticked step. "Words Learned" counts words merely seen. | High | D:512-518, 808-858; FND-DOM-08 [V] | Users can't tell whether they are improving, and the numbers can be inflated. | Progress shows trends of **measured** metrics plus practice time measured from recordings (CAP-08; PP-7; UX-4). |
| UXF-24 | Completion is **self-reported**: one-tap plan ticks with a nested 24 dp target, daily step toggles, program day checkboxes and "Mark Complete". | High | W:614, 639; DP:262; DP:353-368; LM:426-459; FND-DOM-06 [V] | Progress and streak can be gamed. Real effort and a tick look the same. | Steps complete only when a valid attempt event is recorded (CAP-03; PP-4; PP-7). |
| UXF-25 | Learning programs: 6 of 7 can never be completed (fewer drills than days). There is no preview of other programs and no unenroll. The 30-day plan shows expanded before enrolling, and "Hide Plan" fails for it. | High | DP:278-368; CAT:1072-1121 [V] | Guaranteed failure. Commitment without an exit. | Programs are replaced by tracks with no fixed day counts (CAP-06). Today adapts daily (CAP-03). |
| UXF-26 | No progress context inside the flow. Record & Review history mixes all labs without a module label, and deltas compare unrelated modules. The streak differs between dashboard and profile. | Medium | SAC:538-610, 593-604; W:703 vs P:153-156 [V] | Misleading "better/worse" signals. | Attempt history with template labels. Comparisons only between attempts at the same prompt or template (SCR-PROG-03/04; FS-08). |

### 3.7 Poor onboarding

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-27 | First-time users are greeted with **"Welcome back, {name}"**. The computed personalised cue is never rendered, and there is no avatar. | Medium | W:127-189, 164; D:214-226 [V] | Feels generic or broken on day one. | First-run copy differs from returning copy (DES-05 §12). Today greets with the day's session, not filler. |
| UXF-28 | Onboarding has no validation, no skip and no submit state, so double taps are possible. A failure is **invisible** because the only SnackbarHost lives after onboarding. | High | AO:599-623; REPO:484-485; NAV:695; MVM:232 [V]; invisible error [I] | Users may be stuck or saved with defaults ("Speaker") without knowing. | Inline validation, a loading button state and an inline error with retry (DES-05 §11.1, §11.19; FS-02). |
| UXF-29 | Onboarding inputs confuse. The default goal text matches none of the 4 chips. Choosing a level silently overwrites all 7 sliders. Confidence is collected but never shown. | Medium | AO:352, 362-382, 389-392 [V]; confidence unused [V] | Users don't know what their choices do. | Each question has a clear purpose, and choices are reflected in Today's session (UX-5). |
| UXF-30 | An auto 7-step tour starts right after onboarding. Back, Skip, or "Open module" marks the whole tour completed. | Medium | NAV:617-622, 1252-1268, 1288-1299 [V] | An interruption at peak motivation. Remaining steps are lost. | No tour. The first attempt happens inside onboarding, then Today (DES-04 FLOW-01). |

### 3.8 Unclear evaluation feedback

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-31 | **No transcript by default.** Live transcription is off by default and post-hoc file transcription is a stub. | Critical | Settings row 8 `ai_live_transcription_enabled` default false (UIA §8.7) [V]; FND-AI-01, FND-AI-02 | The core promise ("see what you said") fails silently. Feedback falls back to audio signals. | A transcript for every attempt where the device supports it, with honest tier messaging otherwise (CAP-05, CAP-12; UX-3). |
| UXF-32 | **Scores measure something other than their label.** Free-speech "Pronunciation" comes from loudness, pitch and voicing. Activity scores come from answer length. Grammar has a fixed baseline. | Critical | FND-AI-08; FND-DOM-04 [V, RD-03] | Users act on false information. This is the main trust failure in the vision (RD-13 §1). | Metrics are labelled Measured or Estimated. No composite for free speaking (D-06). Recognition agreement is called "recognised as intended" (PP-1; NG-03). |
| UXF-33 | Feedback ignores the task. Lab-specific instructions are dropped, so "Compare AI" never compares against the target phrase. | High | LIO:257-260, 938-943 [V] | Read-aloud and repeat tasks get generic feedback. | Template-specific metrics (for example reference match for Read aloud and Listen & repeat) defined in ARC-05 and FS-05. |
| UXF-34 | Two feedback cards have different structures and **different composite formulas**. Results list many items with no single focus. | High | CC:1326-1842 vs ETR:645-787; CC:1351-1360 [V] | Inconsistent results. Users don't know what to change next. | One feedback card: one strength, one focus, at most 3 metrics, transcript, "Was this accurate?" (DES-05 §11.15; PP-3). |
| UXF-35 | An insufficient-evidence result is shown as a raw enum ("Status: INSUFFICIENT_EVIDENCE • No score awarded") with no guidance. *The fail-closed behaviour itself is a strength* (FND-AI-17). | Medium | CC:1268 [V] | Users don't know why or how to fix it. | **Inconclusive** state card with a specific reason and "Try again" (DES-05 §11.16). Keep the gate (§6). |

### 3.9 Broken interaction patterns

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-36 | **Mic permission denial is silent** on 7 recorders: `if (granted) start` with no else branch, no rationale and no Settings link. | Critical | SAC:150-154, 835-837; LAT:103-105, 353-355, 556-558, 744-746; LM:550-552 [V] | After one denial the core task is impossible, and the app gives no explanation. | Permission education (SCR-PERM-01), denied and permanently-denied states with "Open settings" (DES-05 §11.19; CAP-04). |
| UXF-37 | Recording leaks across screens. It continues after navigation, the next lab shows "Stop" with a live waveform, labs can evaluate another lab's recording, and Voice Training shares Record & Review's result slot. | High | UIA §4.3; SVM:594-613, 466; LM:601-602 [V] | Wrong feedback for the wrong audio, and a confusing state. | One session state machine per attempt. Recording stops on leaving or interruption (CAP-04; FND-AI-14). |
| UXF-38 | **Work is lost** on rotation or process death. An activity attempt is wiped on rotation. There are 0 `rememberSaveable` in screens and 0 `SavedStateHandle`. | High | UA:76-78; UIA §4.2 [V] | Rotating the phone or switching apps loses an attempt or typed text. | Session and attempt state is saved (`SavedStateHandle`) and persisted locally (ARC-02; CAP-11). |
| UXF-39 | Duplicate submissions are possible because buttons stay enabled while loading. "Swap" is a visible no-op. Some "Try Again" buttons only dismiss. | Medium | SAC:508, 960; LAT:197, 419, 608, 795; ADS:112-114; SAC:641; AU:131-138 [V] | Duplicate saves. Controls that don't respond. | Loading state disables the primary action (DES-05 §11.1). Every visible control has an effect, verified by tests. |
| UXF-40 | Patterns are misused. FilterChips act as action buttons. A phone-call metaphor is used for one-shot dictation. A 24 dp tap target sits inside a tappable row. Tutorial and notification overlays are drawn in-window without a focus trap. | Medium | CM:724-726, 368-373; W:614, 639; TO:185; ND:96 [V]; focus behaviour [I] | Screen-reader and touch users get unexpected behaviour. | Standard M3 components used as intended (DES-05 §11). Modal content in real Dialogs or ModalBottomSheets (DES-07 A11Y-10). |

### 3.10 Accessibility problems (summary; rules and tests in DES-07)

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-41 | **Current colour tokens fail WCAG contrast** in the light theme. Computed values (from `Color.kt` and the `Theme.kt` pairings): `outline` #E4E6F3 on white **1.24:1** (Theme.kt:219), dark `outline` #312B5C on #19143A **1.36:1** (Theme.kt:193), `warning` #E87A1E with white **2.90:1** (Theme.kt:73-74), `success`/`tertiary` #16A363 with white **3.25:1** (Theme.kt:70-71, 209-210), `secondary` #2B84EA with white **3.76:1** (Theme.kt:205-206), `error` #E03468 with white **4.32:1** (Theme.kt:220-221), and 6 pastel ink/background pairs **2.48–3.81:1** (gold, peach, mint, coral, sky, rose; Color.kt:72-111). | High | Values [V, computed]. Whether each pair is rendered as text or boundary [I]. CONF-034: contrast never re-verified after the palette change | Low-vision users may not see field boundaries, warnings or success text. Outlined fields and buttons fall below the 3:1 non-text minimum. | New palette with every text pair ≥ 5.8:1 and boundaries ≥ 4.0:1, computed in DES-05 §3.4 (D-18). |
| UXF-42 | **Fixed-height text containers** will clip at large font scales: hero 252 dp (W:282), flashcard 270 dp (VO:241), score ring 72 dp (CC:1505-1537), Today ring 96 dp (W:805-844), waveform 78 dp with badge (CC:322), avatar 64 dp with headline initials (P:207-216). | High | Code [V]; clipping [I]; no font-scale test exists (UIA §10) | Text is cut off at 150–200 % font scale. | No fixed-height text containers. Support 200 % (DES-05 §4.4; DES-07 A11Y-13). |
| UXF-43 | **Semantics gaps.** Only 6 `heading()` and 2 `Role.` uses in all of `ui/`. Settings switches are not merged with labels. Sliders have no labels. State-only icons ("Day completed" with no day name). Icons repeat adjacent text, so it is read twice. No live regions. | High | UIA §6 rows 1–4 (for example AS:624-645; AO:660; W:896, 968; D:1013) [V] | TalkBack users cannot navigate by heading, don't know what controls do, and miss status changes. | DES-07 §3 rules (A11Y-02…12). Design-system components ship with correct semantics. |
| UXF-44 | **Undersized touch targets**: carousel dots 7×7 dp, plan check 24 dp, tutorial dots 8 dp, drawer pill about 26 dp, ring links about 22–26 dp. | Medium | W:467, 625; TO:556-566; ND:563-588; W:859-863 [V sizes; I heights] | Mis-taps, especially for motor-impaired users. | 48 dp minimum (DES-07 A11Y-19). |
| UXF-45 | **Reduced motion** is read once and does not react to changes (CC:111-124). It is ignored by the dashboard (NAV:227 is not passed to D:160), the notification drawer, the score ring, flashcards and onboarding. The in-app preference exists but has no UI (PR:247-251). | Medium | UIA §6 row "Reduced motion" [V] | Motion-sensitive users still see animation. | Reactive system setting plus an in-app preference (DES-05 §8.3; DES-07 A11Y-21). |
| UXF-46 | Typography quality. `labelSmall` uses **JetBrains Mono** (monospace) in 103 places. Bold and SemiBold are synthesised from single-weight fonts. `headlineSmall` and the display styles are undefined and fall back to the system font. | Medium | Type.kt:9-25, 98-104; UIA §5.1 [V] | Chips and meta text read like code. Headings render in mixed fonts and faux weights. | One open-licence family with real weights; no monospace in UI (DES-05 §4). |
| UXF-47 | About **386 hardcoded English strings**, with only 4 string resources. `String.format` is used without a Locale. | Medium | UIA §5.1; D:198; P:418 [V] | Localisation is impossible, and number formatting is locale-unsafe. | All strings in resources; locale-aware formatting (DES-07 A11Y-36, A11Y-37). |
| UXF-48 | System bar icons follow the *system* night mode, not the in-app theme. There is a colour jump from the splash (#FAF9F6 / #0F172A) to the Compose canvas (#F4F5FB / #100C2A). | Low | MainActivity.kt:17; res/values/colors.xml:10; values-night/colors.xml:3; Color.kt:6, 14 [V]; visual effect [I]; CONF-035 | Dark icons on a dark bar when the app is forced dark. A visible flash at launch. | System bar style follows the effective theme. Window and splash colours match `surface` (DES-05 §10). |

### 3.11 Missing states

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-49 | **Loading and Error look like Empty** on every list-driven screen (`(state as? Success)?.data.orEmpty()`). | High | NAV:513-521 [V] | "No sessions yet" while data is loading or failed to load. Users may think their data was lost. | Every screen defines Loading, Empty, Error, Offline and Permission states (DES-03 state matrix; DES-05 §11.19; UX-7). |
| UXF-50 | **Offline:** analysis can spin forever because the save awaits `batch.commit()` before leaving Loading. No screen has an offline state. | Critical | SVM:509-535; REPO:1337-1350 [V code; I behaviour, strong]; FND-DATA-03 | The core task hangs without network. | Local-first saving; sync never blocks the UI (CAP-11; PP-6). Offline pattern in DES-05 §11.19. |
| UXF-51 | Dead ends. A profile load error is text with no retry. The activity screen is blank while loading. Challenge and lesson error cards have no retry. | High | NAV:490-494; UA:139; SAC:1053, 1099; LM:411 [V] | Users must kill the app to recover. | Every error state has a recovery action (DES-02 §2.3 principle 4). |
| UXF-52 | **Account deletion and data export are unreachable.** `deleteAccountAndAllData` has 0 callers and there is no export. | Critical | MVM:474; UIA §8.6–8.7; FND-SEC-03 [V] | Users cannot exercise data rights. Store-policy and legal exposure. | Delete account (SCR-ACC-02) and export (SCR-SET-06) (CAP-10). |

### 3.12 Developer jargon in the UI

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-53 | Internal and developer terms appear on screen: "CameraX video or Photo Picker (<19MB)", "INSUFFICIENT_EVIDENCE", "Running SpeakingEvaluator…", "EXTRACTED" / "NO_CAPTIONS", "Local Whisper / Moonshine TFLite", "Acoustic Audio DSP Analysis", "MediaPipe", "Checks Firestore applicationReleases…", the CI secret name, raw "(N B)" sizes, and raw `localizedMessage` on sign-in errors. | High | SAC:623; CC:1268; UA:399; YT:198; AS:785-902; AU:295-373; AO:225-233 [V] | B1–B2 English speakers face technical English they cannot act on. | Banned-terms list and copy rules (DES-05 §12.3–12.4). Errors in plain language (DES-05 §11.19). |
| UXF-54 | **"AI" labels on deterministic or template features**: "AI Mentor", "Local AI Coach", "Compare AI", "AI Quiz", "Get Presentation AI", and an always-on "Local AI" badge. A permanently disabled "Cloud AI Coach" switch sits next to dead tier chips. | High | UIA §1.3, §7.4; W:1129; AS:658-713; `LocalExecutionPolicy.kt:13` [V]; FND-AI-16 | Overclaims capability (PP-1), then disappoints. The dead switch implies a hidden feature. | No "AI" for deterministic features. Cloud AI controls removed (NG-01, NG-02; DES-05 §12.3). |
| UXF-55 | Creator brand and unexplained frameworks run through the UI: "askvinh" on 149 source lines, the auth tagline "…grounded in Vinh Giang (@askvinh) frameworks", "Browse all 15 @askvinh modules", and named frameworks ("3-Stage Self-Audit", "5 Vocal Foundations", "4-Color Communicator", "Friendly Authority"). | High | AO:146; D:647; SAC:408; LM:636; CAT:180, 205-447 [V]; FND-DOM-09 | Confusing to users who don't know the creator. Third-party IP exposure (tracked as D-08 / RSK-05). | Original, plainly named content (PP-8; CAP-06). Creator names banned (DES-05 §12.3). |

### 3.13 False or fabricated displays

| ID | Finding | Sev | Evidence | User impact | Redesign response |
|---|---|---|---|---|---|
| UXF-56 | **Fallback numbers are shown as user data**: overall score 62 (D:181); analytics skills 62/65/60/68/58/66 (AS:103-108); a pronunciation trend of 65 with a single fake point next to "0 sessions" (D:238, 950). The sparkline invents a 60 → 62 trend, and its **accessibility text reads "latest 62 out of 100"** (CC:414-417). The ring draws a 28° arc at 0 done (W:821). There are static KPI pills ("Weekly Goal: 5 h") (D:766-799). | Critical | Cited lines [V] | New users see invented performance data as fact, including through TalkBack. | No number without evidence. Honest empty states (DES-05 §11.13, §11.18, §11.19; PP-1). Covered by checklist HRC-31. |
| UXF-57 | New users see **"1 Day Streak"** before practising. The profile is seeded `streakDays = 1` and the display is floored at 1. The profile shows "1 days". | High | REPO:494; W:703; D:435; P:410 [V] | A false achievement that also reveals sloppiness. | Streak starts at 0. "Start your streak today" (DES-05 §11.17; PP-7). |
| UXF-58 | **Self-ratings are presented as "Objective Speech Skills"** in Analytics and the profile skill bars until sessions overwrite them. | Critical | AS:101-110, 280-292; P:465-478; AO:555-561 [V] | Users believe their self-guess is a measurement. | No self-rating sliders. Only measured or estimated metrics, each labelled (PP-1; CAP-05). |
| UXF-59 | **Fabricated provenance and confidence.** YouTube timestamps are invented (`idx*45s`) and shown as "verified". Cloud lessons are forced to `sourceVerified = true`. ASR confidence uses constants (0.85, 0.94). "VERIFIED" lessons link to the channel home page. | High | LIO:870-881; YT:321; LM:98, 107, 276, 285; SVM:129, 193; CAT:205-447 [V] | Users are misled about where content comes from and how sure the app is. | No "verified" labels without real verification. Content provenance is owned (PP-8; EP-5). |
| UXF-60 | **Status and settings don't reflect reality.** Capability states show "ready" or "verified" without checks. Some toggles have no effect. The privacy statement says transcripts stay on the device while the code uploads them. | Critical | FND-AI-05, FND-AI-06, FND-SEC-05 [V, RD-03]; Settings privacy text (UIA §8.7 row 15) [V] | Users make privacy decisions based on false statements. | Status shown only after on-device verification (CAP-12). Privacy text is true and test-covered (PP-2). |
| UXF-61 | Hardcoded counts in copy go stale: "150 drills • 10 modules • 10 paths", "All 24 Features", "13 Conversation Simulator roles", "9 AI evaluators". | Low | D:296, 637; AO:211; ADS:150-155 [V] | Minor inaccuracies as content changes. | Counts are computed from content or omitted (DES-05 §12.4). |

### 3.14 Severity summary

| Category | Critical | High | Medium | Low | Total |
|---|---|---|---|---|---|
| Unnecessary steps (UXF-01…05) | 0 | 2 | 3 | 0 | 5 |
| Confusing navigation (06…11) | 2 | 2 | 2 | 0 | 6 |
| Repeated actions (12…14) | 0 | 2 | 1 | 0 | 3 |
| Inconsistent labels (15…18) | 0 | 2 | 2 | 0 | 4 |
| Overloaded screens (19…22) | 0 | 3 | 1 | 0 | 4 |
| Unclear progress (23…26) | 0 | 3 | 1 | 0 | 4 |
| Poor onboarding (27…30) | 0 | 1 | 3 | 0 | 4 |
| Unclear evaluation feedback (31…35) | 2 | 2 | 1 | 0 | 5 |
| Broken interaction patterns (36…40) | 1 | 2 | 2 | 0 | 5 |
| Accessibility (41…48) | 0 | 3 | 4 | 1 | 8 |
| Missing states (49…52) | 2 | 2 | 0 | 0 | 4 |
| Developer jargon (53…55) | 0 | 3 | 0 | 0 | 3 |
| False or fabricated displays (56…61) | 3 | 2 | 0 | 1 | 6 |
| **Total** | **10** | **29** | **20** | **2** | **61** |

---

## 4. "First five minutes": a new user today

Reconstructed from code for a first-time user from the primary segment (RD-08 P1). Times are **INFERRED** except where a coded duration exists.

| # | Moment | What the user sees and must do | Problem | UXF |
|---|---|---|---|---|
| 1 | Tap the app icon | The system splash (#FAF9F6 light / #0F172A dark) jumps to a different Compose background. A **2.9 s** launch animation plays (LA:71-75) [V]. It cannot be skipped, and taps may reach the screen underneath [I]. | Waiting; visual discontinuity | 02, 48 |
| 2 | Sign-in screen | "SpeakCraft"; tagline "AI Communication Coach grounded in Vinh Giang (@askvinh) frameworks" (AO:146); "What's inside:" with 4 feature rows. The only action is **"Sign in with Google"** [V]. On failure a raw exception message appears (AO:225-233) [V]. | No way to try first. Creator brand. Misleading "AI". | 01, 53, 54, 55 |
| 3 | Profile gate | Spinner "Preparing your speaking studio…" (NAV:469-489). On error, dead-end text with no retry (NAV:490-494) [V]. | Jargon ("studio"); dead end | 51 |
| 4 | Onboarding step 1 | Name (pre-filled); "Current English Speaking Level" with 4 options. Choosing one silently rewrites 7 sliders the user hasn't seen yet (AO:362-382) [V]. | Hidden side effects | 29 |
| 5 | Onboarding step 2 | The goal field is pre-filled with a long sentence that matches none of the 4 chips. The user then picks minutes from chips **and** a duplicate 5–60 slider (AO:352, 509-529) [V]. | Duplicate input; unclear purpose | 03, 29 |
| 6 | Onboarding step 3 | Seven self-rating sliders, for example "Pronunciation Clarity 62", **before speaking once** (AO:555-561) [V]. | Abstract and effortful. The values later show as "Objective". | 03, 58 |
| 7 | "Start My Coaching Plan" | No loading state; a double tap is possible. A failure is invisible (NAV:695) [V/I]. | Possible silent failure | 28 |
| 8 | First dashboard | **"Welcome back, {name}"** and **"1 Day Streak"** on day 0. An auto 7-step tour overlay starts at once (NAV:617-622) [V]. | False streak; wrong greeting; interruption | 27, 30, 57 |
| 9 | After the tour | About 21 widgets; fallback numbers (score 62, trend 65 beside "0 sessions") [V]. The hero "Start Lesson" opens a lesson other than the one titled (W:209-212) [V]. | Overload; fabricated data; mislink | 10, 19, 56 |
| 10 | Tap "Record" (bottom bar) | "Record and Review Studio", subtitle "Mic pause/resume, CameraX video or Photo Picker (<19MB) & 3-Stage Audit" (SAC:621-623) [V]. | Jargon; overloaded screen | 20, 53 |
| 11 | Tap "Record Mic" | The OS permission dialog appears with no prior explanation. **If denied: nothing happens, with no message and no way forward** (SAC:150-154) [V]. | Core task blocked | 36 |
| 12 | Record, then tap "Analyze With Local AI Coach" | Live transcription is off by default, so **no transcript** [V]. Feedback is based on audio signals, and "Pronunciation" is derived from loudness and pitch (FND-AI-08). Offline, analysis may spin indefinitely [I]. Short audio gives "Status: INSUFFICIENT_EVIDENCE" (CC:1268) [V]. | Core value missing; mislabelled metrics; jargon | 31, 32, 35, 50 |

**Counted minimum to first recording (INFERRED from code paths):** 9 taps. They are: Sign in (1), account chooser (1), Continue ×2 (2), Start My Coaching Plan (1), Skip Tour (1), Record tab (1), Record Mic (1), Allow (1). Add 2.9 s of animation plus network sign-in and profile load. **First feedback** takes 2 more taps (Stop, Analyze): 11 taps minimum, all after 3 form screens and a tour.

**Against the targets:** RD-13 §11 and PP-4 require a first valid speaking attempt within 3 minutes, inside onboarding. UX-2 requires at most 2 taps from app open to recording for a returning user. Today a returning user also needs 2 taps from the dashboard, but only after the 2.1 s overlay and any mandatory update dialog. The target flow is defined in DES-04 FLOW-01 and FLOW-02.

---

## 5. Heuristic review checklist (for reviewing new designs)

**Purpose.** Use this checklist in Stage B to review every new or changed screen and flow before UI build and again before release (RD-15 UX-6; TST-01 Definition of Done).

**Procedure.**
1. At least 2 reviewers (UX plus one of PM, QA or Android) review independently using the DES-06 specification or prototype, in light and dark theme, at 100 % and 200 % font.
2. Rate each applicable check: **Pass**, **Minor** (fix before release), **Major** (fix before build sign-off) or **Blocker** (design cannot proceed).
3. Record results against the screen ID (SCR-*). **Exit:** no Blocker or Major open. Minor items are logged in PLN-04.

| Check | Heuristic / principle | Pass criterion |
|---|---|---|
| HRC-01 | H1 Status | Recording state is shown by text, icon and elapsed time, never by colour alone. |
| HRC-02 | H1 Status | Any wait over 1 s shows progress with text. Over 10 s it explains the delay and offers Cancel or Retry. No indefinite spinner. |
| HRC-03 | H1 Status, UX-7 | Offline is indicated without blocking. Everything that works offline still works. |
| HRC-04 | H1 Status, PP-1 | Capability and sync status reflect verified state only. |
| HRC-05 | H2 Real world, UX-1 | Only canonical terms from DES-05 §12.2 are used. No banned term from §12.3. |
| HRC-06 | H2 Real world | No developer terms, enum names, library names, file sizes in bytes or raw exception text. |
| HRC-07 | H2 Real world | Copy is understandable at CEFR B1: short sentences, common words, no idioms. |
| HRC-08 | H3 Control | Every full-screen flow has a visible exit. Leaving asks for confirmation only if a recording or input would be lost. |
| HRC-09 | H3 Control | The user can retry any attempt and skip a step (subject to the streak decision in DES-07 OQ-A11Y-1). |
| HRC-10 | H3 Control | No dialog is non-dismissable. No dialog or overlay appears at launch. |
| HRC-11 | H3 Control | Reversible removals offer Undo. Irreversible ones are confirmed with a specific verb. |
| HRC-12 | H4 Consistency, UX-8 | The same 4 destinations, in the same order, at every window size. Back pops one level. |
| HRC-13 | H4 Consistency | One component per pattern: one Record button, one feedback card, one metric tile, one empty-state layout. |
| HRC-14 | H4 Consistency | Only DES-05 tokens are used: no raw colours, radii, sp or ad-hoc dp. |
| HRC-15 | H5 Error prevention | Primary actions are disabled while processing, so double submission is impossible. |
| HRC-16 | H5 Error prevention | Inputs validate inline before submit, and the error text says how to fix it. |
| HRC-17 | H5 Error prevention | A permission rationale is shown before the system prompt. Recording cannot start without the permission. |
| HRC-18 | H6 Recognition | The next step is always visible on Today without scrolling at 100 % font on a 360 × 640 dp screen. |
| HRC-19 | H6 Recognition | The result shows the previous attempt at the same prompt for comparison where one exists. |
| HRC-20 | H7 Efficiency, UX-2 | A returning user needs at most 2 taps from app open to recording. |
| HRC-21 | H7 Efficiency | Recording stops automatically at the maximum duration. Start and stop are single taps. |
| HRC-22 | H8 Minimalism, PP-3 | Exactly one primary (filled) action per screen. At most about 5 choices at the same level. |
| HRC-23 | H8 Minimalism, UX-10 | No decorative element competes with the task: no gradients, pastel swatches or hero images without purpose. |
| HRC-24 | H8 Minimalism, UX-10 | Celebration only for real milestones, and static under reduced motion. |
| HRC-25 | H9 Errors | Every error states what happened and what to do, in plain language, with a recovery action. No dead ends. |
| HRC-26 | H9 Errors, PP-1 | An Inconclusive result explains the specific reason and how to get a valid attempt. It is never framed as failure. |
| HRC-27 | H9 Errors | A permanently denied permission leads to an "Open settings" action. The app re-checks the permission on return. |
| HRC-28 | H10 Help | At most one contextual first-use tip per screen. It can be dismissed and replayed from Settings. |
| HRC-29 | H10 Help, UX-3 | "How we measured this" is one tap from every metric, in the same position. |
| HRC-30 | PP-1 | Every metric shows a **Measured** or **Estimated** label in text. |
| HRC-31 | PP-1 | No number, trend point or status is displayed without evidence. No defaults or fallbacks shown as data. |
| HRC-32 | PP-1, PP-7 | New-user states show an honest zero or empty state, never seeded values. |
| HRC-33 | PP-2 | Every privacy statement on screen matches actual behaviour and is covered by a test. |
| HRC-34 | PP-7 | Streak and progress count only validated attempts. No loss-framed or guilt copy. |
| HRC-35 | PP-1, UX-1 | No "AI" for deterministic features. No creator names or borrowed framework names. |
| HRC-36 | UX-9 | DES-07 checks pass: TalkBack order and labels, 200 % font, contrast, 48 dp targets, reduced motion, colour-independence. |
| HRC-37 | UX-7 | Loading, Empty, Error, Offline and Permission states are all specified (DES-03 state matrix). |
| HRC-38 | PP-4 | Every Today session includes at least one real speaking step. Speaking is the fastest path. |
| HRC-39 | PP-5 | The screen or feature traces to a CAP-nn and a PRD requirement. Nothing duplicates an existing pattern. |
| HRC-40 | UX-5 | The content shown reflects the user's goal, level and recent results, and the user can change them. |

---

## 6. What to preserve from the current UI

| # | Quality to keep | Evidence | Why it matters | How to carry it forward |
|---|---|---|---|---|
| K-1 | **Colours are fully tokenised**: 0 `Color(0x…)` literals outside `ui/theme`. | UIA §5.2 [V] | Makes the palette change (D-18) a contained edit. | Keep the rule and enforce it with a CI grep (DES-05 §13.4). |
| K-2 | Extended semantic colours provided through a `CompositionLocal` (`SpeakCraftExtendedColors`). | Theme.kt:17-67, 167-172 [V] | The correct pattern for success, warning, Measured and similar roles that M3 lacks. | Same pattern with the new role set (DES-05 §3.3, §13). |
| K-3 | Spacing tokens object and adaptive window tiers (COMPACT / MEDIUM / EXPANDED) with 452 token uses. | Spacing.kt:3-47; UIA §5.2 [V] | A ready base for the 4 dp scale and grid. | Keep the names `xs`…`xxl` and add the semantic aliases (DES-05 §5). |
| K-4 | **Adaptive layouts**: bottom bar, rail and two-column expanded layouts, with bounds-based tablet tests. | NAV:677-679; D:384-481; TAB tests (UIA §10) [V] | Tablet support exists and is tested. | Same 4 destinations at all widths. List-detail on expanded (DES-02 §7; DES-05 §5.3). |
| K-5 | **HorizontalPager quality**: `rememberPagerState`, a scale and alpha transition gated by reduced motion, and dots that are secondary to swiping. | W:247-265 [V] | A sound, accessible-leaning implementation pattern. | Reuse it wherever a pager is justified (for example onboarding value slides). Dots must get 48 dp targets and button alternatives (DES-07 A11Y-20). |
| K-6 | **Evidence-gate messaging**: the app refuses to score bad audio ("No score awarded") and fails closed; the logic is well tested. | CC:1268; FND-AI-17 [V] | The seed of the product's honesty differentiator (RD-13 §7). | Keep the gate. Re-word it as the Inconclusive state card (DES-05 §11.16). |
| K-7 | The feedback card omits an unassessable pronunciation bar instead of faking it. | CUJ test "omits fabricated pronunciation bar" (UIA §10) [V] | An honest pattern, already test-covered. | Generalise it: hide or mark Inconclusive any metric without evidence (HRC-31). |
| K-8 | Theme preference System / Light / Dark persisted in DataStore. | NAV:221-225; AS:537-544 [V] | Users expect it. It is already working. | Keep it in Appearance (SCR-SET-05) and add font and motion preferences. |
| K-9 | Top-bar actions enforce 48 dp; a `minTouchTarget` token exists. | NAV:756, 784, 801; Spacing.kt:25 [V] | Correct baseline. | Apply to all controls (DES-07 A11Y-19). |
| K-10 | `SectionHeader` carries `heading()` semantics (used about 40 times). | CC:143 [V] | One of the few correct semantics patterns. | Design-system `SectionHeader` keeps it. |
| K-11 | Reduced-motion alternatives exist for the launch overlay, waveform, step bar and shimmer. | UIA §6 [V] | The behaviours are already designed. | Make the setting reactive and apply it everywhere (DES-05 §8.3). |
| K-12 | Confirmation before destructive history deletion. | AS row 27; CUJ test [V] | Correct friction for permanent loss. | Keep it for Delete history and Delete account (DES-05 §11.5). |
| K-13 | Edge-to-edge rendering. | MainActivity.kt:17 [V] | Modern Android baseline. | Keep it, and fix the bar icon style (DES-05 §10). |
| K-14 | Robolectric and Compose UI test infrastructure with phone and tablet qualifiers (44 tests). | UIA §10 [V] | A base for semantics, font-scale and layout tests with no new libraries. | Extend with the TC-A11Y set (DES-07 §15). |
| K-15 | TTS reference playback with a user-adjustable rate. | Settings row 22 `tts_rate` (UIA §8.7) [V] | Needed for the Listen & repeat template. | Keep, with labelled speed control (DES-07 A11Y-25). |
| K-16 | A visible permission-handled message in the activity templates, and an "Activity Unavailable" recovery card with a working Back. | ETR:110-124; UA:115-133 [V] | The only correct permission and error patterns in the app. | Basis for the DES-05 §11.19 state patterns. |

---

## 7. Inputs to other documents and open questions

| Output | Consumed by |
|---|---|
| Findings UXF-01…61 and responses | DES-08 Redesign Decisions (decision log), RD-03 (FND-UI consolidation), GOV-01 RTM |
| Terminology table §3.4.1 | DES-05 §12 |
| State gaps (UXF-49…52) | DES-03 state matrix; FS-nn UI states |
| Accessibility findings (UXF-41…48) | DES-07 remediation list (§16) |
| Checklist §5 | DES-06 reviews; TST-01 Definition of Done |
| Preserve list §6 | ARC-02 migration plan; DES-05 §13 |

| # | Open question for the Product Owner |
|---|---|
| OQ-UX-1 | Approve funding of moderated usability validation (5–6 participants per primary persona) before R1 UI build (§1.2). |
| OQ-UX-2 | Confirm that the in-app "Was this accurate?" rating (CAP-05) may be stored locally and reported in aggregate as the first user-feedback data source (subject to D-09). |

---

## Appendix A — Path aliases used in citations

Paths are relative to `app/src/main/java/com/example/` unless noted.

| Alias | Path | Alias | Path |
|---|---|---|---|
| NAV | ui/AppNavigation.kt | AS | ui/screens/AnalyticsAndSettingsScreens.kt |
| D | ui/screens/DashboardScreen.kt | AU | ui/screens/AppUpdateScreen.kt |
| W | ui/components/DashboardStudioWidgets.kt | CC | ui/components/CoachComponents.kt |
| AO | ui/screens/AuthAndOnboardingScreens.kt | ETR | ui/components/ExerciseTemplateRenderers.kt |
| P | ui/screens/UserProfileScreen.kt | FT | ui/components/FeatureTutorialComponents.kt |
| SAC | ui/screens/SpeakingAndChallengesScreens.kt | TO | ui/components/TutorialOverlay.kt |
| LAT | ui/screens/LabsAndTrainingScreens.kt | LA | ui/components/LaunchAnimationOverlay.kt |
| LM | ui/screens/LessonAndModulesScreens.kt | ND | ui/components/NotificationDrawer.kt |
| CM | ui/screens/ConversationAndMentorScreens.kt | UD | ui/components/UpdateAvailableDialog.kt |
| YT | ui/screens/YouTubeIntelligenceScreen.kt | SVM | ui/viewmodel/SpeakingAndLabsViewModel.kt |
| VO | ui/screens/VocabularyScreen.kt | MVM | ui/viewmodel/MainCoachViewModel.kt |
| DP | ui/screens/DailyAndProgramsScreens.kt | REPO | data/firebase/SpeakCraftRepository.kt |
| ADS | ui/screens/ActivityDiscoveryScreen.kt | LIO | data/ai/local/LocalIntelligenceOrchestrator.kt |
| UA | ui/screens/UniversalActivityScreen.kt | CAT | data/model/CurriculumCatalog.kt |
| AH | data/firebase/AuthHelper.kt | FTR | data/model/FeatureTutorialRegistry.kt |
| PR | data/preferences/AppPreferencesRepository.kt | MAN | app/src/main/AndroidManifest.xml |
| Color.kt / Theme.kt / Type.kt / Spacing.kt | ui/theme/… | colors.xml | app/src/main/res/values(-night)/colors.xml |

## Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial draft from Stage A static code audit | UX Researcher |
