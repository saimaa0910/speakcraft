# UI/UX Specifications — SpeakCraft (Proposed)

| Field | Value |
|---|---|
| Document ID | DES-06 |
| Version | 0.2 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UI/UX Designer |
| Approval | Pending — Product Owner |
| Related | DES-02 Information Architecture, DES-03 Screen Inventory, DES-04 User Flows, DES-05 Design System, DES-07 Accessibility Guidelines, DES-08 Redesign Decisions, DES-01 UX Audit, FS-01…FS-12, PRD, SRS §6.2–6.10 and §15, ARC-05 Local Intelligence Architecture, RD-13…RD-16 |

> **Everything in this document is a RECOMMENDATION** (GOV-02 DC-3). It becomes authoritative for screen-level experience when the Product Owner approves it.
>
> **What this document owns and what it does not.** DES-06 owns the experience of each screen: layout, hierarchy, interaction, responsive behaviour, state presentation and the design acceptance criteria `AC-DES-nn`. It does **not** redefine:
> - screen IDs and navigation (DES-02 §4 is authoritative; only those `SCR-*` IDs are used here);
> - tokens, components, motion and terminology (DES-05; token and component names are used exactly as defined there, C-5);
> - system behaviour and business rules (SRS `SRS-FR-*`, `SRS-BR-*`; cited, not restated);
> - metric definitions and Inconclusive reasons (ARC-05 §5–§9);
> - accessibility rules and test cases (DES-07 `A11Y-nn`, `TC-A11Y-nnn`; cited by ID);
> - state matrices and flows (DES-03, DES-04 `FLOW-nn`).
>
> No Compose code is specified. Sketches show intent, not implementation.

---

## 1. Experience overview and visual direction

### 1.1 The experience in one sentence

> Open the app, see one session, speak, see what you said and one thing to try next, try again — in a calm space that never pretends to know more than it does.

The redesign replaces a 26-route "studio" (DES-01 §3.2, UXF-06; RD-03 FND-UI-01, FND-UI-02) with four destinations and one task-focused session player.

**Curriculum basis (Product Owner direction, 2026-11-21; PRD-FR-056/057).** The curriculum is built from the frameworks, techniques, tasks, exercises, activities and tips taught on the @askvinh YouTube channel. In the interface this becomes a first-class content type, the **technique** (technique → explanation in our words → exercises and activities → tips), browsed in Practice (SCR-PRAC-01 Techniques collection, SCR-PRAC-04 Technique detail; §3.4), used by Today's plan, practised in the same session player with the same honest metrics (§3.3), and linked from every result's coach tips. **D-08 is DECIDED (2026-11-21): "original wording, link out"** (SRS-BR-028): techniques, exercises and tips are written in our own words and examples with no verbatim transcripts; each links to its **specific** source video (with timestamp) through "Watch the original lesson"; and the creator's name, likeness and branding never appear in the UI, content, notifications or store listing. YouTube scraping, caption download and API ingestion stay removed (YouTube Terms of Service; ADR-011; RD-16 NG-08). The screen designs that implement this are RECOMMENDATIONS pending design approval.

**Feedback after every attempt (Product Owner direction, 2026-11-21; PRD-FR-047).** Every submitted attempt — speaking activity, technique exercise, word stage or review item — ends with **Mistakes found** (evidence-backed, labelled Measured or Estimated, honest when empty) and **Coach tips** linked to techniques, produced by deterministic on-device logic (ARC-05 §8.4; no cloud AI, D-04).

Every screen in §3 is judged against three questions:

1. **Is the next step obvious?** (PP-3, UX-6)
2. **Is every number backed by evidence and labelled?** (PP-1, UX-3)
3. **Does it work offline, at 200 % text, with TalkBack and with reduced motion?** (PP-6, UX-7, UX-9)

### 1.2 Experience goals by persona

| Persona | What they need from the interface | Where it shows up |
|---|---|---|
| **Aarav** (P1, B2, software engineer) | Start in two taps on the commute; structure and filler feedback he can act on before a stand-up | Today single primary action (§3.2); Structured answer result and mistakes found (§3.3.5, §3.3.6) |
| **Meera** (P2, B1+, job seeker) | Privacy she can see; retry the same interview answer and see the change | Privacy step (§3.1, SCR-ONB-06); Try again with comparison (§3.3.9) |
| **Daniel** (P3, B1, student) | Works offline on an older mid-range phone; short sessions; streaks that motivate without guilt | Offline-identical core screens (§2.2.4); 5-minute option; calm streak (§3.2) |
| **Sofia** (P4, C1, senior analyst) | Precise, honest metrics with their limits; trends over weeks | "How we measured this" (§3.3.12, SCR-SESS-02); metric detail (§3.6, SCR-PROG-02) |
| **Hoang** (P5, A2+, shop supervisor) | Plain words; slow reference audio; gentle "Not yet" instead of "Fail" | Copy rules (§2.5); Read aloud and Listen & repeat results (§3.3.7) |

### 1.3 Visual direction: calm, premium, minimal, professional

| Quality | What it means on screen | Design-system mechanism (DES-05) |
|---|---|---|
| **Calm** | One accent colour used only for the primary action, selection and focus. Neutral canvas. Nothing moves unless the user caused it or it carries live information (the level meter). | `primary` reserved by §3.5 rule 1; `surface` / `surfaceContainer*` canvas; motion rules §8.4 |
| **Premium** | Generous, consistent spacing; real font weights; precise alignment; no visual noise. Quality comes from restraint and craft, not decoration. | Spacing scale §5.1, `screenMargin`, `sectionGap`; Inter 400/500/600 (§4.1, OQ-DS-2); radius scale §6.3 |
| **Minimal** | One primary (filled) button per task screen. Progressive disclosure: detail is one tap away ("All measurements", "How we measured this", "Show full transcript"). | Button rules §11.1; bottom sheets §11.6 |
| **Professional** | Plain, adult, B1-level English. Labels say what things do. No mascots, no hype, no exclamation marks except one on a milestone. | Voice §12.1; banned terms §12.3 |
| **Restrained colour** | Semantic colours mean one thing each (`success` = completed, `recording` = mic capturing, `streak` = streak). Metric judgements are neutral text, never green or red. | §3.3, §3.5 rule 3 |
| **Generous spacing** | Compact screens use a 16 dp margin and 24 dp between sections; content never touches edges; reading width ≤ 640 dp. | `screenMargin`, `sectionGap`, `cardGap`; §4.5 reading width |
| **Purposeful motion** | Motion explains a change of state (prompt → recording → result) and is ≤ 400 ms; it is instant under reduced motion. | `Motion.*` durations §8.1; reduced-motion table §8.3 |

### 1.4 What we avoid

| Avoid | Current example (evidence) | Instead |
|---|---|---|
| **Neon gradients, pastel decoration, hero overlays** | Saturated violet `#6C3DF4`, 8 pastel swatch pairs, sidebar and hero gradients (DES-05 §3.1; ui_audit §5.1); 252 dp hero pager with the same image on every slide (UXF-42; ui_audit §3.3) | Neutral containers; no imagery in R1 (DES-05 §10 Imagery); the session card is the only emphasised surface (`primaryContainer` accent, §3.2) |
| **Dense dashboards** | ~21 widgets, ~70 targets, 25 routes on the home screen (UXF-19; FND-UI-02; ui_audit §3.4) | Today has a greeting, one session card with one button, a week strip and a short progress note (§3.2) |
| **Gamified pressure** | "1 Day Streak" before practising (UXF-57); self-ticked plans counting as practice (UXF-24); loss-framed copy risk (PP-7) | One honest streak; no flames, no freezes, no countdowns to "lose" a streak; celebration only for real milestones (§2.6) |
| **Fake precision** | Fallback score 62 and a single-point "trend" (UXF-56); "Objective Speech Skills" from self-ratings (UXF-58) | No number without evidence; Measured / Estimated / Inconclusive badges in text (DES-05 §11.14) |
| **Developer language** | "INSUFFICIENT_EVIDENCE", "CameraX", "Local Whisper / Moonshine TFLite" (UXF-53) | Canonical vocabulary (DES-05 §12.2); banned-terms guardrail G-7 (DES-05 §13.4) |
| **Unexplained creator branding; scraped "verified" content** | A primary tab labelled "@askvinh" that opens a caption-scraping tool; framework names shown without explanation; heuristic extracts labelled "verified"; links to the channel home page (UXF-07, UXF-55, UXF-59; FND-UI-03) | Techniques explained in our own plain words inside Practice (SCR-PRAC-04), each with steps, exercises and tips; a "Watch the original lesson" link to the specific video; no creator name, likeness or branding anywhere (D-08 decided, SRS-BR-028); no scraping and no "verified" labels (§3.4) |
| **Interruptions** | 2.1–2.9 s launch overlay (UXF-02), mandatory update dialog (UXF-11), auto tour (UXF-30) | ≤ 1 s platform splash (§2.3); no dialog at launch; contextual tips (§2.4) |

### 1.5 How to read §3

Each screen section has the same eight parts:

| Part | Content |
|---|---|
| (a) Current problem | What is wrong today, with evidence: `UXF-nn` (DES-01), `FND-UI-nn` (RD-03) and ui_audit section references |
| (b) Proposed experience | What the user experiences, in plain words |
| (c) Wireframe | ASCII sketch at **compact width (360 dp)**, with notes for medium and expanded |
| (d) Interactions | What each control does |
| (e) Information hierarchy | Priority order of content, top to bottom |
| (f) Responsive behaviour | Compact / medium / expanded / height-compact |
| (g) Loading, error, offline (and permission) states | Screen-specific use of the §2.2 templates; the full state matrix is DES-03 |
| (h) Acceptance criteria | `AC-DES-nn`, testable; each screen includes 200 % font scale and TalkBack criteria referencing DES-07 |

### 1.6 Wireframe legend

| Notation | Meaning (DES-05 component) |
|---|---|
| `[[ Label ]]` | Filled `Button`: **the** primary action (Large 56 dp when it is a bottom CTA) |
| `[ Label ]` | `FilledTonalButton` or `OutlinedButton` (secondary) |
| `( Label )` | `TextButton` (tertiary) |
| `(o)` | Record button, 80 dp circle (§11.12). `(■)` = Record button while recording (Stop) |
| `<-` / `(x)` | Back / Close `IconButton` |
| `(AV)` | 40 dp initials avatar in a 48 dp `IconButton` ("Profile and settings") |
| `(i)` | Info `IconButton` → "How we measured this" |
| `{Measured}` `{Estimated}` `{Inconclusive}` | Evidence badge (§11.14) |
| `(*)` / `( )` | Selected / unselected radio row (whole row is the target, §11.22) |
| `[on]` / `[off]` | Switch row |
| `###` | Static skeleton block |
| `====` | Top edge of the sticky bottom action area (`bottomActionPadding`) |
| `|||||` | Level meter (§11.12) |

Widths: compact sketches are ~42 characters wide, representing a 360 dp window with the 16 dp `screenMargin`.

---

## 2. Cross-cutting patterns

### 2.1 App shell and navigation per window class

**Current problem.** Navigation differs by width: 5 bottom items (including "@askvinh" and "Mentor") below 840 dp, a 10-item sidebar with different labels above it, and Settings is not on phone chrome at all (UXF-06, UXF-09; FND-UI-01; ui_audit §1.2). Secondary screens highlight no tab and stacks grow without bound (UXF-08; ui_audit §1.6).

**Proposed shell.** Four destinations — **Today · Practice · Words · Progress** — identical in order, icons and labels at every width (DES-02 §2.1; DES-05 §11.7).

| Window class (DES-05 §5.3) | Navigation | Top app bar | Content layout | Bottom area |
|---|---|---|---|---|
| Compact (< 600 dp) | `NavigationBar`, 4 items, labels always shown | Small `TopAppBar`, title + trailing avatar `(AV)` | Single pane, push to detail | Snackbar host above the bar |
| Medium (600–839 dp) | `NavigationRail`, 4 items, labels shown | Same | Single pane centred; lists as 2-column grids; reading max 640 dp, lists max 840 dp | Snackbar host bottom-centre |
| Expanded (≥ 840 dp) | `NavigationRail`, 4 items | Same, per pane | List-detail in Practice, Words and Progress history; Today in 2 columns | Snackbar host bottom-centre of content |
| Height-compact (< 480 dp tall) | `NavigationRail` instead of the bar | Same | Session player places the Record button beside the prompt | — |

```
COMPACT (360 dp)                          MEDIUM (600–839 dp)
+------------------------------------------+   +---+---------------------------------+
| Today                              (AV) |   |   | Today                      (AV) |
+------------------------------------------+   | T |                                 |
|                                          |   | P |   +-------------------------+   |
|   screen content (single pane)           |   | W |   | content centred,        |   |
|                                          |   | G |   | max 640 dp (reading)    |   |
|                                          |   |   |   +-------------------------+   |
+------------------------------------------+   |   |                                 |
| [Today] Practice   Words   Progress      |   +---+---------------------------------+
+------------------------------------------+    rail: Today/Practice/Words/Progress

EXPANDED (>= 840 dp), e.g. Practice
+---+------------------------+---+---------------------------+
|   | Practice          (AV) |   |  <detail pane title>      |
| T | list pane (>= 360 dp)  |24 |  detail pane              |
| P |  Track / activity list | dp|  (activity detail)        |
| W |                        |   |                           |
| G |                        |   |                           |
+---+------------------------+---+---------------------------+
```

**Shell rules.**

| # | Rule | Source |
|---|---|---|
| SH-1 | Primary destinations show a small `TopAppBar` with the destination title (`titleLarge`) and the avatar. The avatar opens Profile (SCR-PROF-01). In R2 a bell appears before the avatar on Today only; it is **not** shown in R1. | DES-02 §2.2; DES-05 §11.8 |
| SH-2 | Detail screens show Back (`AutoMirrored.Filled.ArrowBack`, "Back"), a title and at most one trailing action. | DES-05 §11.8 |
| SH-3 | Full-screen flows hide the bar or rail: SCR-SESS-01, SCR-ONB-01…08, SCR-ACC-01, SCR-ACC-02, SCR-PERM-01, SCR-SYS-01. Each shows Close or Back, except SCR-SYS-01 while deletion is running. | DES-02 §2.3 principle 2 |
| SH-4 | Each tab keeps its own back stack. Re-selecting the current tab pops to its root and scrolls to top. The tab stays highlighted on its secondary screens. System Back pops one level; from a tab root it goes to Today; from Today it leaves the app. | DES-02 §2.1, §2.3; DES-05 §11.7 |
| SH-5 | Maximum depth 3 from a primary destination (e.g. Practice → Track → Activity → Session player). | DES-02 §2.3 principle 1 |
| SH-6 | No badges on navigation items in R1. Due word reviews are surfaced on Today and Words instead. | DES-05 OQ-DS-4 |
| SH-7 | Edge-to-edge with `WindowInsets.safeDrawing`; sticky bottom areas pad for navigation-bar insets; text-input screens use `imePadding()`. | DES-05 §5.4 |
| SH-8 | One snackbar at a time, above the bar or sticky action area. | DES-05 §11.10 |

### 2.2 State templates

Every screen in §3 uses these templates and only these. DES-03 lists, per screen, which states apply (UX-7). Components are DES-05 §11.19 (`StateViews`).

**Current problem.** Loading and Error render as "empty" on every list (`orEmpty()`, NAV:513-521; UXF-49), offline analysis can spin forever (UXF-50), errors are dead ends or raw exception text (UXF-51, UXF-53), and mic denial is silent (UXF-36). RD-03 FND-UI-05.

#### 2.2.1 Loading (skeletons)

| Aspect | Specification |
|---|---|
| When | Data is not yet available from the local database or content pack |
| First 300 ms | Show nothing new (keeps the previous frame or blank canvas). Most local reads finish here, so most users never see a skeleton. |
| After 300 ms | A **static** skeleton that matches the final layout: blocks in `surfaceContainerHighest` with the target component's shape (`medium` for cards and tiles, `small` for text lines, full for buttons). Text-line blocks are `bodyLarge` line height. |
| Motion | **No shimmer in any mode** (DES-05 §8.3). |
| Spinners | Only inside buttons (§11.1 loading) and in the session Processing state (§11.11). Never a full-screen spinner. |
| Accessibility | The skeleton container has the description "Loading" and is announced politely once; skeleton blocks are hidden from semantics. |
| Timeout | If loading exceeds 10 s, replace the skeleton with the Error template (a local read that slow is a fault). |

```
+------------------------------------------+
| Today                              (AV) |
+------------------------------------------+
|  ######################                  |   <- greeting line block
|  +------------------------------------+  |
|  | ##############                     |  |   <- session card block (shape large)
|  | ########################           |  |
|  | ########################           |  |
|  | [##########################]       |  |   <- button-shaped block (not clickable)
|  +------------------------------------+  |
|  ####  ####  ####  ####  ####  ####  ### |   <- week strip blocks
+------------------------------------------+
```

#### 2.2.2 Empty

| Aspect | Specification |
|---|---|
| When | No data yet (new user), or filters/search return nothing |
| Layout | 48 dp icon (`onSurfaceVariant`), title `titleMedium` (heading), one or two lines `bodyMedium`, one action; vertically centred in the content area; padding `xxl` |
| Rule | An empty state is **honest**: it never shows zeros as achievements, placeholder charts, sample data or seeded values (HRC-31, HRC-32). It always offers a next step (DES-02 §2.3 principle 4). |
| Filter / search empty | Title "No activities match these filters" + `( Clear filters )`. Search: "No words match "{query}"" + `( Clear search )`. |

#### 2.2.3 Error

| Aspect | Specification |
|---|---|
| When | A read, write or processing step failed |
| Placement | **Inline, in place of the content that failed** — never a dialog, never a toast-only message. Unaffected parts of the screen keep working. |
| Layout | `Outlined.ErrorOutline` (32 dp), title `titleMedium`, cause and fix in `bodyMedium`, reassurance about data where relevant, one action |
| Copy formula | **What happened + what to do (+ what is safe)** (DES-05 §12.4 rule 4). No exception text, error codes, class names or library names (SRS §15 closing rule). |
| Input | Any user input or recording is kept (DES-05 §11.19). |
| Announcement | Announced when it appears; assertive only if it blocks the current task (DES-07 A11Y-32). |
| Retry | The action actually retries (fixes "Try Again" buttons that only dismissed, UXF-39). |

Example: "Couldn't load your history" / "Something went wrong on this phone. Your recordings are safe." / `[ Try again ]`.

#### 2.2.4 Offline

Core practice works offline (PP-6; PRD-FR-100). Offline is therefore **not a state of core screens**.

| Screen group | Offline presentation |
|---|---|
| Today, session player, Practice, Words, Progress, Profile | **Identical to online.** No banner, no icon, no degraded copy. |
| Features that need the network: sign-in (SCR-ACC-01), sync status (SCR-SET-07), offline speech-pack download and online-transcription consent (SCR-SET-04, SCR-ONB-07), account deletion start (SCR-ACC-02), Share diagnostics/Help links (SCR-SET-08), "Watch the original lesson" on a technique or coach tip (SCR-PRAC-04, SCR-SESS-01 result; PRD-FR-057) | A non-blocking `infoContainer` row with `Outlined.CloudOff`: "You're offline. {What this means here}." plus optional `( Retry )`. Shown only next to the affected control. |
| T3a (consented online transcription) while offline | The attempt continues audio-only for that attempt (ARC-05 §4.2); the result's tier line says "Transcript wasn't available this time — you were offline." No blocking. |

Copy example: "You're offline. Your practice is saved on this phone and will sync later." (DES-05 §11.19).

#### 2.2.5 Permission

| Permission | When requested | Presentation |
|---|---|---|
| Microphone (RECORD_AUDIO) | In onboarding (SCR-ONB-06) or, if skipped, before the first recording (SCR-PERM-01) — never at launch | 1) Education (what we use it for, what stays on the phone) → system dialog. 2) Denied once: an inline card replaces the Record button: "SpeakCraft needs the microphone to hear your practice." `[ Allow microphone ]`. 3) Permanently denied: same card with `[ Open settings ]`. Permission is re-checked on `ON_RESUME` (DES-05 §11.19; SRS-ERR-001). |
| Notifications (API 33+) | Only when the user turns on a reminder (SCR-ONB-05 or SCR-SET-03) | If denied: the reminder row shows "Notifications are off for SpeakCraft" + `( Open settings )`. No repeated prompts (SRS-FR-REM-005). |

#### 2.2.6 Capability unavailable (speech tier T3b)

Honest explanation of what still works, using the DES-05 §11.19 copy pattern: "This phone can't turn speech into text offline. We can still measure your pace and pauses." with `( Learn more )` and, where eligible, `( Use online recognition… )` → consent flow (FS-12). Shown in onboarding (SCR-ONB-07), Speech & privacy (SCR-SET-04) and on the result's transcript section (§3.3.8). Not shown as an error.

#### 2.2.7 "Not available" metric

A metric whose inputs are missing is never defaulted (SRS-BR-002). In a Metric tile it shows "—" and the reason in text: "Not available — needs a transcript" (SRS-FR-EVAL-002).

#### 2.2.8 Copy guidelines per state

| State | Do | Don't |
|---|---|---|
| Loading | (no copy; "Loading" for screen readers) | "Preparing your speaking studio…" (ui_audit §3.1) |
| Empty | "Your progress will appear here. Finish your first session to see your pace, pauses and more." | "0 sessions", charts with a fake point, "No data" |
| Error | "Couldn't save your settings. Try again." | "Error: SQLiteConstraintException", "Oops!", "Something went wrong." with no action |
| Offline | "You're offline. Sign-in needs a connection. You can keep practising." | "No internet!", a full-screen block |
| Permission | "SpeakCraft needs the microphone to hear your practice. Your recordings stay on this phone." | A silent no-op (UXF-36); "Permission denied" |
| Capability | "This phone can't turn speech into text offline. You'll still get feedback on speaking time and pauses." | "Recognizer ERROR_CLIENT", "Whisper model missing" |

### 2.3 Brand launch moment (≤ 1 s, skipped under reduced motion, never blocks input)

**Current problem.** A 408-line launch overlay composable plays on every launch: 2.9 s first, 2.1 s returning, 0.6 s with reduced motion; it has no skip, no pointer block and restarts on rotation (UXF-02; ui_audit §8.1; LA:71-75). The splash background `#FAF9F6` / `#0F172A` jumps to a different Compose canvas (UXF-48; CONF-035).

**Proposed experience.** The brand moment is the **platform launch window only** — the SpeakCraft icon centred on a calm background for as long as the app genuinely needs to draw its first screen, capped at 1 second. There is no second, app-drawn launch animation.

| Aspect | Specification | Source |
|---|---|---|
| Mechanism | Platform splash (`windowSplashScreen*` attributes on API 31+; the themed window background below API 31). **No overlay composable, no new library** (EP-1). The current `SpeakCraftLaunchOverlay` is removed. | DES-05 §10 |
| Background | Window background = `surface` per mode (`#FAF9FD` light, `#121318` dark), so the first Compose frame has the same colour (no jump). Splash icon background = `primaryContainer` per mode. | DES-05 §10 |
| Icon | **Static** icon (RECOMMENDATION: no animated vector). A static icon behaves identically with or without reduced motion, which also covers the case where the in-app "Reduce motion" preference cannot be read before app code runs. | DES-05 §8.3 brand moment row |
| Duration | The splash is shown only until the first frame of the start destination (Onboarding or Today) is ready. If a keep-on-screen condition is used (to avoid a flash of an empty Today), it is **capped at `brandMoment` (1000 ms)**; after the cap the start destination is shown with its loading skeleton (§2.2.1). | NFR-PERF-001, NFR-PERF-002; DES-05 §8.1 `brandMoment` |
| Reduced motion | With `ANIMATOR_DURATION_SCALE == 0` or in-app "Reduce motion: On", no animation occurs in the launch sequence: no icon animation, no exit animation; the first screen appears with an instant switch. | DES-05 §8.3; DES-07 A11Y-21 |
| Never blocks input | No app-drawn layer sits above the first screen. The first interactive frame accepts taps immediately. There is no "hold for branding" beyond readiness. | DES-05 §8.4 rule 4 |
| What follows | Nothing modal: no dialog, sheet, tip, tour or permission request may appear at launch (DES-05 §11.5; HRC-10). Account-deletion resume (SCR-SYS-01) is the only screen that may precede Today, because it is required by SRS-FR-ACC-008. | — |
| Sound / haptics | None. | — |

```
t = 0 ms          t <= first frame (cap 1000 ms)         t = first frame
+-------------+   +-------------+                        +------------------+
|             |   |             |                        | Good morning,    |
|    [icon]   |-->|    [icon]   |---- instant switch --->| Aarav.           |
|             |   |             |   (no app animation)   | [[ Start session ]]
| bg: surface |   | bg: surface |                        | bg: surface      |
+-------------+   +-------------+                        +------------------+
```

### 2.4 Contextual first-use tips (replaces the four current tutorial systems)

**Current problem.** Four overlapping help mechanisms repeat the same content (UXF-14; FND-UI-11; ui_audit §8.2):
1. an auto 7-step core tour after onboarding (UXF-30), whose Back or Skip marks the whole tour completed;
2. a first-use spotlight on 8 routes;
3. a help-icon `FeatureTutorialDialog`, with a second instance on the dashboard that can stack;
4. an inline `FeatureGuideStrip` on 23 screens.
The help icon also shows the *dashboard* guide on four routes (UXF-10), and the sign-in guide promises "live Gemini AI coaching" (FTR:29).

**Proposed experience.** Screens explain themselves through clear titles, labels and empty states. Where one fact is genuinely non-obvious, the screen shows **one short, dismissible tip, in place, the first time it matters** — and never again for that profile.

| Rule | Specification |
|---|---|
| TP-1 One per screen | At most **one** tip per screen, and at most one tip visible anywhere at a time. |
| TP-2 Never blocking | Tips are part of the page: no scrim, no dimming, no spotlight cut-out, no focus trap, no dialog or sheet. Every control on the screen works while a tip is visible. |
| TP-3 Dismissible | `( Got it )` and a close `IconButton` ("Dismiss tip"), both 48 dp targets. Performing the action the tip describes also dismisses it. |
| TP-4 Stored per profile | Dismissal is stored in the `settings` DataStore **keyed by profile ID** (device-local, not synced; R-1). A second profile on the same device sees the tips fresh. Deleting the profile deletes its tip state. |
| TP-5 Never again | A dismissed tip never reappears unless the user chooses **About → Show tips again** (SCR-SET-08), which resets that profile's tip state (HRC-28). |
| TP-6 Right moment | A tip appears only when its trigger condition is met (table below), never at launch and never on the first frame after the splash. |
| TP-7 Quiet zones | No tips in onboarding (SCR-ONB-*), in SCR-PERM-01, or in the session player while it is in Prepare, Ready, Recording or Processing. Tips never cover or sit adjacent to the Record button. |
| TP-8 Content | One sentence, ≤ 20 words, B1 English, states a fact or a capability — not marketing. No "AI". |
| TP-9 Placement | Directly under the screen's first heading or next to the element it describes, in reading order. |
| TP-10 Motion | Appear and disappear with a 150 ms fade (`short3`); instant under reduced motion. |

**Composition** (no new token names): an `OutlinedCard` (DES-05 §11.3, `borderHairline`, radius `medium`, `cardPadding`) containing `Outlined.Info` (24 dp, `onSurfaceVariant`), the tip text in `bodyMedium`, and `( Got it )`; trailing close `IconButton`. See §5 issue 12 (DES-05 to name this pattern).

```
+------------------------------------------+
| (i) Your session changes every day,  (x) |
|     based on your goal and recent        |
|     results.                  ( Got it ) |
+------------------------------------------+
```

**Tip catalogue (R1, complete list — 6 tips replace 24 tutorial entries).**

| ID | Screen | Trigger | Text |
|---|---|---|---|
| TIP-01 | SCR-TODAY-01 | First visit to Today after onboarding | "Your session changes every day, based on your goal and recent results." |
| TIP-02 | SCR-SESS-01 Result | First valid result ever | "Measured means we counted it from your recording. Estimated is our best guess. Tap (i) to see how." |
| TIP-03 | SCR-PRAC-01 | First visit to Practice | "Everything you practise here counts towards your streak and progress." |
| TIP-04 | SCR-WORD-01 | First visit to Words | "Each word has five stages. Saying it aloud moves it forward." |
| TIP-05 | SCR-PROG-01 | First visit with at least one valid attempt | "Trends appear after three valid attempts of the same kind." |
| TIP-06 | SCR-PROG-04 | First attempt detail opened | "You can still tell us whether this feedback was accurate." |

Accessibility: the tip is ordinary content in reading order (not a live region, so it does not interrupt TalkBack); the card merges into one phrase with separately focusable actions (DES-07 A11Y-07); text wraps at 200 % (A11Y-13).

### 2.5 Microcopy and tone

DES-05 §12 is the authority for voice, vocabulary and banned terms. This section adds screen-level rules.

**2.5.1 Fixed action labels (use exactly these).**

| Action | Label | Where |
|---|---|---|
| Begin today's session | "Start session" / "Continue session" (after ≥ 1 step done) | SCR-TODAY-01 |
| Recording | "Record" / "Stop" | Record button everywhere (§11.12) |
| Retry an attempt | "Try again" | Result, Inconclusive, attempt detail |
| Re-record after interruption | "Record again" | Interrupted state |
| Move to next step | "Continue" | Result inside Today; onboarding |
| Skip a step | "Skip this step" | Session player (Today context) |
| Reference audio | "Listen" / "Listen slowly" | Read aloud, Listen & repeat, word card |
| Start a word review | "Start review" | SCR-WORD-01 |
| Finish onboarding attempt later | "Do this later" | SCR-ONB-08 |
| Grant microphone | "Allow microphone" / "Open settings" | Permission states |
| Explanation | "How we measured this" | Result, attempt detail |
| Accuracy | "Was this accurate?" → "Yes" / "No" | Result, attempt detail |
| Return after session complete | "Done" | SCR-TODAY-02 |

Never: "OK" as a primary action, "Submit", "Analyze", "AI …", "Let's go!", "Got it, Let's Practice" (DES-05 §11.1, §12.3).

**2.5.2 Writing rules for this product's users.**

| Rule | Why (persona) | Example |
|---|---|---|
| ≤ 20 words per sentence; one idea per sentence | Hoang (A2+) and Daniel (B1) read in their second language | "Answer in about one minute. Take a moment to plan first." |
| Concrete over abstract | Aarav wants to act on it | "Pause silently instead of saying 'basically'." not "Improve your fluency." |
| Facts before judgement | Sofia distrusts vague scores | "You used 'um' 6 times in 1 minute." then the band |
| Neutral metric words | PP-7; DES-05 §3.5 rule 3 | "Within target", "Faster than target" — never "Good!", "Bad" |
| No guilt or loss framing | Meera's anxiety; PP-7 | "Welcome back. Here's a shorter session to ease back in." not "You broke your streak" |
| Privacy statements are generated from policy constants | PP-2; SRS-PRIV-002 | "Your recordings stay on this phone. They're deleted after 30 days unless you change this." |
| Numbers as numerals, locale-formatted; units spelled out for screen readers | DES-07 A11Y-34, A11Y-37 | Visible "128 words/min"; spoken "128 words per minute" |
| Plurals via resources | DES-07 A11Y-36 | "1 step" / "3 steps" |
| British spelling (OQ-DS-3 recommendation): "Practice" (noun, tab), "practise" (verb) | Consistency with documentation | "Practise more" |
| Technique names, explanations, exercises and tips are written in our own plain words, never verbatim from transcripts; the creator's name, likeness and branding never appear (D-08 decided; SRS-BR-028) | PP-8; legal exposure (RSK-05); B1 readability | A technique name that says what to do, with "Why it works" in one sentence — not an unexplained framework label |
| Mistakes are stated as evidence, not judgement | PP-1, PP-7; Meera's anxiety | "'basically' ×5" under "Filler words above the usual range" — never "You made 5 mistakes!" |

### 2.6 Celebrations (milestones only, reduced-motion aware)

| Aspect | Specification | Source |
|---|---|---|
| What counts | **Streak milestones only**: 3, 7, 14, 30, 60 and 100 days. Nothing else in R1 is celebrated (no confetti for a single attempt, a word stage pass or opening the app). | PRD-FR-072; SRS-FR-PROG-006 |
| Frequency | Once per milestone per profile, ever. If the streak resets and is rebuilt, the milestone is not celebrated again; the longest streak keeps the record. | RECOMMENDATION |
| Where | Inline, in the place the user already is: the result screen of the attempt that earned it, and SCR-TODAY-02 if the session completes the same day. **Never a dialog**, never at launch. | DES-05 §11.5 |
| Visual | `Outlined.EventAvailable` (48 dp, `streak`) + `titleMedium` text, e.g. "Seven days in a row. Nice work." (the only place an exclamation mark is allowed, and it is not used here). | DES-05 §7, §12.1 |
| Motion | Once, ≤ 800 ms: scale 0.9 → 1 + fade, `emphasizedDecelerate`. **Reduced motion: static icon and text.** No looping, no particles, nothing flashing more than 3 times per second. | DES-05 §8.3, §8.4 |
| Haptic | `CONFIRM` once (§2.7). | DES-05 §9 |
| Sound / sharing | None. No share button (NG-06). | RD-16 NG-06 |
| Accessibility | The milestone text is part of the result and is announced with it (polite). | DES-07 A11Y-08 |

### 2.7 Haptics

DES-05 §9 defines constants; this table maps them to screens. All haptics use `performHapticFeedback`, which respects the system touch-feedback setting.

| Event | Screen | Constant (fallback < API 30) |
|---|---|---|
| Recording started | SCR-SESS-01 Recording | `CONFIRM` (`VIRTUAL_KEY`) |
| Recording stopped (by user or auto-stop) | SCR-SESS-01 | `CONFIRM` (`VIRTUAL_KEY`) |
| Inconclusive result, Interrupted, Error, permission denied | SCR-SESS-01 | `REJECT` (none) |
| Milestone reached | SCR-SESS-01 Result, SCR-TODAY-02 | `CONFIRM` once (none) |
| Word stage passed | SCR-SESS-01 word-stage result | None (visual completion only — not a milestone) |
| Navigation, scrolling, toggles, prep timer ticks, countdown | All | None |

Rules: never continuous; always paired with a visible change; never the only signal. An in-app "Vibration feedback" toggle in Appearance (SCR-SET-05) depends on DES-05 OQ-DS-5.

### 2.8 Confirmation dialog inventory (R1, complete)

Dialogs are used only for destructive or work-losing actions (DES-05 §11.5). This is the complete R1 list; any new dialog needs a DES-06 change.

| Trigger | Title | Body (≤ 3 sentences) | Actions (dismiss · confirm) |
|---|---|---|---|
| Close session player while Recording or Processing | "Leave this attempt?" | "Your recording will be discarded." | "Keep recording" · "Leave" |
| Delete one attempt (SCR-PROG-04) | "Delete this attempt?" | "Its recording, transcript and feedback will be removed from this phone{ and your account}." | "Cancel" · "Delete" (error content) |
| Clear history option (SCR-SET-06) | "Delete {option}?" | Consequence per option (§3.7.4) | "Cancel" · "Delete" |
| Shorter recording retention (SCR-SET-04) | "Delete older recordings now?" | "{n} recordings are older than {period} and will be deleted now." | "Cancel" · "Delete and change" |
| Sign out (SCR-SET-07) | "Sign out?" | Keep / Remove choice per D-21 (FS-01 §5.3) | "Cancel" · "Sign out" |
| Delete everything on this device (SCR-SET-07) | "Delete everything on this device?" | "All your practice, words and recordings on this phone will be deleted. This can't be undone." | "Cancel" · "Delete" |

Account deletion is a dedicated flow (SCR-ACC-02, §3.7.6), not a dialog.

### 2.9 Cross-cutting acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-01 | At 360, 600 and 840 dp widths and in a 480 dp-tall landscape window, the app shows exactly four destinations in the order Today, Practice, Words, Progress, with the same labels and icons; the selected item shows the indicator, the Filled icon and `selected` semantics, not colour alone (DES-07 A11Y-23). | Robolectric UI test per width |
| AC-DES-02 | Each tab keeps its own back stack; re-selecting the current tab returns to its root scrolled to top; the tab stays highlighted on its secondary screens; Back from a tab root opens Today and Back from Today leaves the app. | Robolectric navigation test |
| AC-DES-03 | No list-driven screen shows its Empty state while data is loading or after a load failure: for the first 300 ms nothing new is drawn, then a static skeleton (no shimmer in any motion mode); a load failure shows the inline Error template with a working retry, keeps any user input, contains no exception text, code or class name, and is announced (DES-07 A11Y-32). | Robolectric with fake repositories (slow, failing) |
| AC-DES-04 | With the device in airplane mode, Today, the session player, Practice, Words, Progress and Profile render and complete their tasks with no offline banner; an offline row appears only beside network-dependent controls listed in §2.2.4. | Instrumented test, airplane mode |
| AC-DES-05 | The build contains no app-drawn launch overlay. On cold start the platform splash is followed directly by the start destination; the splash is visible no longer than 1000 ms beyond process start on RD-MID (P90); window background equals `surface` in both themes; with animator duration scale 0 no launch animation runs; a tap on the first frame of Today is handled (NFR-PERF-001/002). | Instrumented timing + UI test |
| AC-DES-06 | No dialog, bottom sheet, tip, tour or permission request appears at launch or on the first frame after the splash, on any start destination (HRC-10). | UI test on cold start (new and returning profile) |
| AC-DES-07 | First-use tips: at most one visible at a time; none in SCR-ONB-*, SCR-PERM-01, or SCR-SESS-01 Prepare/Ready/Recording/Processing; each has "Got it" and a "Dismiss tip" button of ≥ 48 dp; while visible, every other control remains operable and TalkBack focus is not trapped; once dismissed it never reappears for that profile after restart, appears fresh for a second profile, and returns only after About → Show tips again. | Robolectric + DataStore test |
| AC-DES-08 | `strings.xml` contains zero banned terms from DES-05 §12.3 (CI guardrail G-7) — including the creator's name and handle in UI strings and content-pack text (D-08 decided, SRS-BR-028); no UI text is hard-coded outside resources (DES-07 A11Y-36); numbers, dates and times are locale-formatted (A11Y-37). | CI grep + lint |
| AC-DES-09 | A celebration appears only when the current streak first reaches 3, 7, 14, 30, 60 or 100 days for that profile; it is inline (not a dialog), lasts ≤ 800 ms, plays once, is a static icon and text under reduced motion (DES-07 A11Y-21, TC-A11Y-010), and has no sound or share action. | Unit test (milestone logic) + Robolectric |
| AC-DES-10 | `CONFIRM` haptics fire on recording start, stop and milestone; `REJECT` on Inconclusive, Interrupted, Error and permission denial; no haptic fires on navigation, scrolling, toggles or timers; with system touch feedback off, none fire. | Instrumented test with haptic spy |
| AC-DES-11 | Every screen in §3, in light and dark themes, at font scale 1.0, 1.3, 1.5 and 2.0 on a 360 × 640 dp window, shows no clipped, truncated or overlapping essential text (DES-07 A11Y-13; TC-A11Y-007, TC-A11Y-023, TC-A11Y-024) and every interactive element has a ≥ 48 dp target (A11Y-19; TC-A11Y-008). | Robolectric screenshot/semantics matrix |

---

## 3. Screen redesign proposals

### 3.0 Coverage

| Section | Screens (DES-02 §4 IDs) | Feature spec | Flows (DES-04) | ACs |
|---|---|---|---|---|
| 3.1 Onboarding | SCR-ONB-01…08 | FS-02 (FS-01 sign-in entry, FS-12 speech check) | FLOW-01 | AC-DES-12…21 |
| 3.2 Today | SCR-TODAY-01, SCR-TODAY-02 | FS-03, FS-08 (streak) | FLOW-02, FLOW-03 | AC-DES-22…33 |
| 3.3 Session player | SCR-SESS-01 (all states), SCR-SESS-02 | FS-04, FS-05, FS-12 | FLOW-02…FLOW-06 | AC-DES-34…57 |
| 3.4 Practice (incl. Techniques collection; PRD-FR-056/057) | SCR-PRAC-01, -02, -03, and SCR-PRAC-04 Technique detail (DES-02 v0.2) | FS-06 | FLOW-04 | AC-DES-58…65 |
| 3.5 Words | SCR-WORD-01, -02, -03, -04 | FS-07 | FLOW-05, FLOW-06 | AC-DES-66…74 |
| 3.6 Progress | SCR-PROG-01, -02, -03, -04 | FS-08 | FLOW-07 | AC-DES-75…83 |
| 3.7 Profile, settings, account | SCR-PROF-01, SCR-SET-01, SCR-SET-04, SCR-SET-06, SCR-SET-08, SCR-ACC-02 (summary of SCR-SET-02/03/05/07/09) | FS-10, FS-01, FS-09, FS-11, FS-12 | FLOW-08, FLOW-09 | AC-DES-84…99 |

Screens not specified in detail here (SCR-ACC-01, SCR-PERM-01, SCR-SYS-01, SCR-SET-02/03/05/07/09; R2 screens SCR-WORD-05/06, SCR-PROG-05, SCR-INBOX-01) follow the patterns in §2 and their feature specifications; SCR-SET-02/03/05/07/09 are summarised in §3.7.7.

---

### 3.1 Onboarding — SCR-ONB-01…08

#### (a) Current problem

| Problem | Evidence |
|---|---|
| Google sign-in is mandatory before any use; failures show raw exception text | UXF-01, UXF-53; AO:79-304, AO:225-233; FND-SEC-01 |
| Three form steps (name, 4 levels, free-text goal, minute chips **and** a duplicate slider) and **7 self-rating sliders**; no speaking at all | UXF-03, UXF-29; ui_audit §3.2 (AO:413-561) |
| Choosing a level silently overwrites all 7 sliders; the default goal text matches no chip; confidence is collected and never used | UXF-29; AO:352, 362-382 |
| No validation, no skip, no submit state; a save failure is invisible because the only snackbar host lives after onboarding | UXF-28; AO:599-623; NAV:695 |
| Onboarding offers 4 levels, the profile 5; minute options differ | UXF-17; ui_audit §3.2 |
| Self-ratings later appear as "Objective Speech Skills" | UXF-58; FND-UI-07 |
| A 7-step tour starts immediately after; first visit says "Welcome back" and "1 Day Streak" | UXF-27, UXF-30, UXF-57; FND-UI-11 |
| 9 taps to the first recording and 11 to first feedback, after sign-in, 3 form screens, 2.9 s of animation and a tour | DES-01 §4 |

#### (b) Proposed experience

Eight short screens with **no account, no typing and no network required**. Every choice has a sensible default, so a user who just taps Continue reaches their first spoken attempt in about a minute (INFERRED; KPI-01 measures it). Privacy is explained **before** the microphone is requested. The speech check tells the truth about what this phone can do. Onboarding ends with a real attempt — "Introduce yourself" — and real feedback, then Today. Workflow, alternates and errors: FS-02 §5–§7; flow: DES-04 FLOW-01.

| Step | Screen | Question / content | Default | Primary action |
|---|---|---|---|---|
| — | SCR-ONB-01 Welcome | Three true value lines | — | "Get started" |
| 1 of 7 | SCR-ONB-02 Goal | "What do you want to practise for?" (5 tracks) | Workplace communication (FS-02 Q1, pending) | "Continue" |
| 2 of 7 | SCR-ONB-03 Level | "How would you describe your spoken English?" (5 levels + "can do" line) | Intermediate | "Continue" |
| 3 of 7 | SCR-ONB-04 Daily time | "How much time do you have most days?" | 10 minutes | "Continue" |
| 4 of 7 | SCR-ONB-05 Reminder | "Would a daily reminder help?" | Remind me at 19:00 † (SRS-FR-ONB-005; see §5 issue 15) | "Continue" |
| 5 of 7 | SCR-ONB-06 Privacy & microphone | Three privacy statements from policy constants | — | "Allow microphone" |
| 6 of 7 | SCR-ONB-07 Speech check | Tier-specific outcome (FS-02 §5.1) | — | Per outcome |
| 7 of 7 | SCR-ONB-08 First attempt intro | "Introduce yourself" preview | — | "Start" → SCR-SESS-01 |

**Minimum path (INFERRED from the design):** Get started → Continue ×4 → Allow microphone + system Allow → Continue → Start → Record = 10 taps (11 on Android 13+ if the reminder stays on, because of the system notification dialog). No text input, no sign-in round-trip, no tour.

#### (c) Wireframes (compact, 360 dp)

```
SCR-ONB-01 Welcome                        SCR-ONB-02 Goal
+------------------------------------------+   +------------------------------------------+
|                                          |   | <-   Step 1 of 7                         |
|   [app icon]                             |   |  [===]--------------------------------   |
|                                          |   |                                          |
|   Speak English with                     |   |  What do you want to practise for?       |
|   confidence.                            |   |  You can change this any time.           |
|                                          |   |                                          |
|   (mic)  Ten minutes of speaking a day   |   |  ( ) Everyday conversation               |
|                                          |   |      Small talk, calls, daily situations |
|   (text) See what you said and one       |   |  (*) Workplace communication             |
|          thing to try next               |   |      Meetings, updates, explaining work  |
|                                          |   |  ( ) Job interviews                      |
|   (lock) Your recordings stay on this    |   |      "Tell me about yourself" and more   |
|          phone                           |   |  ( ) Presentations & storytelling        |
|                                          |   |      Presenting ideas, telling stories   |
|                                          |   |  ( ) Clear speech                        |
|==========================================|   |      Saying words and phrases clearly    |
|  [[          Get started           ]]    |   |==========================================|
|  ( Already use SpeakCraft? Sign in )     |   |  [[            Continue            ]]    |
+------------------------------------------+   +------------------------------------------+

SCR-ONB-03 Level                          SCR-ONB-04 Daily time
+------------------------------------------+   +------------------------------------------+
| <-   Step 2 of 7                         |   | <-   Step 3 of 7                         |
|  [=======]----------------------------   |   |  [===========]------------------------   |
|  How would you describe your             |   |  How much time do you have most days?    |
|  spoken English?                         |   |                                          |
|  This sets where you start. It never     |   |  ( ) 5 minutes                           |
|  limits what you can practise.           |   |      A short session                     |
|  ( ) Beginner                            |   |  (*) 10 minutes                          |
|      I can say simple phrases about      |   |      Recommended for most people         |
|      myself, slowly.                     |   |  ( ) 15 minutes                          |
|  ( ) Elementary                          |   |      A longer session                    |
|      I can manage short everyday ...     |   |                                          |
|  (*) Intermediate                        |   |  You can change this before any          |
|      I can talk about work and daily     |   |  session.                                |
|      life, but I hesitate or search ...  |   |                                          |
|  ( ) Upper-intermediate  ...             |   |                                          |
|  ( ) Advanced  ...                       |   |                                          |
|==========================================|   |==========================================|
|  [[            Continue            ]]    |   |  [[            Continue            ]]    |
+------------------------------------------+   +------------------------------------------+

SCR-ONB-05 Reminder                       SCR-ONB-06 Privacy & microphone
+------------------------------------------+   +------------------------------------------+
| <-   Step 4 of 7                         |   | <-   Step 5 of 7                         |
|  [================]-------------------   |   |  [=====================]--------------   |
|  Would a daily reminder help?            |   |  Your practice stays private             |
|                                          |   |                                          |
|  (*) Remind me at 19:00   ( Change time )|   |  (lock) Your recordings stay on this     |
|  ( ) Not now                             |   |         phone.                           |
|                                          |   |  (text) What you said (the transcript)   |
|  At most one reminder a day, and none    |   |         stays on this phone.             |
|  on days you've already practised.       |   |  (time) Recordings are deleted after     |
|                                          |   |         30 days. You can change this.    |
|                                          |   |  ( Read the privacy details )            |
|                                          |   |                                          |
|                                          |   |  SpeakCraft needs the microphone to hear |
|                                          |   |  you practise.                           |
|==========================================|   |==========================================|
|  [[            Continue            ]]    |   |  [[        Allow microphone        ]]    |
+------------------------------------------+   +------------------------------------------+

SCR-ONB-06, denied once                    SCR-ONB-07 Speech check (on-device outcome)
+------------------------------------------+   +------------------------------------------+
|  ...privacy statements as above...       |   | <-   Step 6 of 7                         |
|  +------------------------------------+  |   |  [==========================]---------   |
|  | (mic-off) You can explore           |  |   |  Checking speech recognition            |
|  | SpeakCraft, but speaking practice   |  |   |                                          |
|  | needs the microphone.               |  |   |  +------------------------------------+  |
|  +------------------------------------+  |   |  | (check) Speech recognition works on|  |
|==========================================|   |  | this phone without internet.       |  |
|  [[        Allow microphone        ]]    |   |  +------------------------------------+  |
|  ( Continue without microphone )         |   |  Recognition language                    |
+------------------------------------------+   |  English (India)        ( Change )       |
 Permanently denied: [[ Open settings ]]       |==========================================|
                                               |  [[            Continue            ]]    |
                                               +------------------------------------------+

SCR-ONB-07, online-consent outcome (T3a candidate)   SCR-ONB-08 First attempt intro
+------------------------------------------+   +------------------------------------------+
| <-   Step 6 of 7                         |   | <-   Step 7 of 7                         |
|  This phone can turn speech into text    |   |  [==================================]   |
|  only online.                            |   |  Let's hear you speak                    |
|                                          |   |  +------------------------------------+  |
|  If you allow it, your voice is sent to  |   |  | Answer a question                  |  |
|  {speech service provider} to make the   |   |  | Introduce yourself: who you are,   |  |
|  transcript. SpeakCraft doesn't store    |   |  | what you do, and one thing you     |  |
|  it anywhere else. You can turn this     |   |  | enjoy.                             |  |
|  off in Settings.                        |   |  +------------------------------------+  |
|                                          |   |  About one minute. You'll have 15        |
|  Without it, you still get feedback on   |   |  seconds to plan first.                  |
|  speaking time and pauses.               |   |  Your recording stays on this phone.     |
|==========================================|   |==========================================|
|  [ Allow online transcription ]          |   |  [[              Start             ]]    |
|  [ Not now                    ]          |   |  ( Do this later )                       |
+------------------------------------------+   +------------------------------------------+
 Equal-weight outlined buttons: consent is never   Mic not granted: card "Microphone needed"
 the visually preferred choice (PP-2 > PP-3).       + [[ Allow microphone ]] ( Do this later )
```

Other SCR-ONB-07 outcomes reuse the same layout: **T3b** ("This phone can't turn speech into text for SpeakCraft. You'll still get feedback on speaking time and pauses. Some checks need a transcript." + `[[ Continue ]]`); **pack installable** (`[[ Download ]]`, `( Use online transcription instead )`, `( Continue without transcripts )`; while downloading a determinate `LinearProgressIndicator` with "45 % · 12 MB left" in text, `( Cancel download )`, and Continue stays available, FS-02 A9). Copy is FS-02 §5.1.

**Medium / expanded:** content column centred, max 640 dp; the sticky action area spans the same 640 dp column; the progress bar sits above the title inside the column. **Height-compact:** content scrolls; the sticky Continue stays visible.

#### (d) Interactions

| Control | Behaviour |
|---|---|
| Get started | Opens SCR-ONB-02. Creates nothing remote; the local profile already exists (SRS-FR-ACC-001). |
| Already use SpeakCraft? Sign in | Opens SCR-ACC-01 (FS-02 A8). Cancel returns here without error text (SRS-FR-ACC-005). On success, imported choices pre-fill steps 1–4, which are skipped; steps 5–6 always run. |
| Option row | Whole row selects (radio). Selection does not advance automatically (users can read all options). |
| Continue | Persists the step, then advances (SRS-FR-ONB-010). On a failed write: inline Error under the options, stays on the step (FS-02 E5). Loading state per DES-05 §11.1 prevents double taps (UXF-28). |
| Back (top bar or system) | Previous step with the choice preserved. On SCR-ONB-01 system Back leaves the app. |
| Change time | Opens a `ModalBottomSheet` with the M3 time input (locale 12/24 h); "Save" / Close. |
| Allow microphone | Shows the system RECORD_AUDIO dialog. Granted → SCR-ONB-07. Denied → denied card (above). Permanently denied → "Open settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`); re-checked on return. |
| Continue without microphone | Continues to SCR-ONB-07; SCR-ONB-08 then shows "Microphone needed" (FS-02 A4). |
| Read the privacy details | Pushes SCR-SET-09 (Privacy information); Back returns here. |
| Speech-check actions | Per FS-02 §5.1; consent is recorded only on "Allow online transcription". |
| Recognition language → Change | `ModalBottomSheet` radio list of supported locales (en-US, en-GB, en-IN, en-AU, filtered by support; SRS-FR-SPCH-008). |
| Start | Opens SCR-SESS-01 with activity `onb-intro` (SRS-FR-ONB-009). |
| Do this later | Completes onboarding; opens Today (SRS-FR-ONB-011). Today then offers "Introduce yourself" as an optional card (FS-02 A1; Q2 pending). |
| First result → Go to Today | The onboarding result shows mistakes found and coach tips like every result (PRD-FR-047); its filled button is "Go to Today" (there is a next step, DES-05 §11.15 v0.2 action rule) and "Try again" is the tonal action (consistent with FS-02 §9; §5 issue 20 resolved). A valid first attempt is the baseline; the result adds one line: "This is your starting point. Progress compares against it." |

#### (e) Information hierarchy

1. Step position (progress bar + "Step n of 7"), 2. the question (`headlineLarge`), 3. one-line reassurance ("You can change this any time"), 4. options with plain descriptions, 5. the single primary action in the sticky area, 6. secondary text action.

#### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | As sketched; content scrolls above the sticky action area. |
| Medium / expanded | Centred 640 dp column; same order; no side illustration (R1 has no imagery). |
| Height-compact | Progress bar and title scroll with content; sticky action area remains; option rows keep 56 dp minimum height. |

#### (g) Loading, error, offline and permission states

| Screen | Loading | Error | Offline | Permission |
|---|---|---|---|---|
| SCR-ONB-01…05 | None (local, < 50 ms) | Inline save error + Retry; stays on step (FS-02 E5) | Identical to online | SCR-ONB-05 → system notification dialog after Continue on API 33+ if "Remind me" is selected |
| SCR-ONB-06 | None | — | Identical | Denied / permanently denied variants (§2.2.5) |
| SCR-ONB-07 | "Checking speech recognition on this phone…" ≤ 2 s †; then provisional T3b copy "We'll check again later" (FS-02 E7) | Download failure: "Download didn't finish. Check your connection and try again." + Retry | Download and online-consent options show "Needs a connection"; on-device and audio-only outcomes unaffected | None needed |
| SCR-ONB-08 | None | — | Identical | "Microphone needed" variant |

#### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-12 | SCR-ONB-01 shows three value lines, one filled "Get started" and the text button "Already use SpeakCraft? Sign in"; completing onboarding never requires an account, a network connection or typed input (D-05; PRD-FR-001). | Robolectric UI test, airplane mode |
| AC-DES-13 | SCR-ONB-02…05 each show exactly one pre-selected option in a radio group whose whole row (≥ 56 dp) is the target, one filled "Continue", and the text "Step n of 7"; Back returns to the previous step with its choice still selected. | Robolectric UI test |
| AC-DES-14 | Goal options are the five track names, level options the five D-17 levels with their "can do" lines, and daily time 5 / 10 / 15 minutes — read from the same options source used by SCR-SET-02, so a value chosen here always appears selected there (UXF-17). | Unit test on options source + UI test |
| AC-DES-15 | Killing the process on any of SCR-ONB-02…07 and relaunching resumes at that step with earlier choices preserved (SRS-FR-ONB-010; FS-02 AC-FS02-11). | Instrumented test |
| AC-DES-16 | On Android 13+, continuing with "Not now" never shows the notification permission dialog; continuing with "Remind me" shows it exactly once; denial keeps the preference and onboarding continues with no second prompt. | Instrumented test |
| AC-DES-17 | SCR-ONB-06 displays the three privacy statements before the system microphone dialog; their text equals the policy-constant snapshot (SRS-PRIV-002); after one denial it shows "Allow microphone" and "Continue without microphone"; after permanent denial "Open settings"; returning from system settings with the permission granted advances without another tap on "Allow". | Unit snapshot + instrumented test |
| AC-DES-18 | SCR-ONB-07 shows "Checking speech recognition on this phone…" for ≤ 2 s, then exactly one FS-02 §5.1 outcome; no "works" or "ready" text appears unless the capability was verified; the online-consent outcome shows "Allow online transcription" and "Not now" as equal-weight outlined buttons and no audio leaves the device before "Allow"; download progress shows a percentage and size in text. | Robolectric with fake capability service + network spy |
| AC-DES-19 | "Start" on SCR-ONB-08 opens SCR-SESS-01 with `onb-intro`; a valid first result shows mistakes found and coach tips, with "Go to Today" as its filled button (completes onboarding) and "Try again" as the tonal action; an Inconclusive first result shows its reason, "Try again" and "Do this later" and stores no baseline; "Do this later" opens Today. | Robolectric navigation test |
| AC-DES-20 | Neither onboarding nor the first Today visit contains self-rating sliders, free-text goal or name fields, a tour, a spotlight or an overlay (UXF-03, UXF-30). | UI test + design review |
| AC-DES-21 | With TalkBack, each step title is a heading, "Step n of 7" is announced on arrival, and each option is announced as one phrase with its description, selected state and position; at font scale 2.0 the "can do" lines wrap fully, Continue stays reachable, and the focused option is never hidden behind the sticky action area (DES-07 A11Y-07, A11Y-13, A11Y-30); under reduced motion step changes are instant (A11Y-21). | Manual TalkBack script + Robolectric fontScale 2.0 |

---

### 3.2 Today — SCR-TODAY-01 (including the greeting) and SCR-TODAY-02

#### 3.2.1 SCR-TODAY-01 Today

##### (a) Current problem

| Problem | Evidence |
|---|---|
| The home screen has up to 16 top-level items, ~21 widgets, ~70 targets and 25 routes, about 5–6 phone screens of scroll; three "today" widgets and two conflicting weekly goals | UXF-19; FND-UI-02; ui_audit §3.4 |
| Three daily-plan systems; the dashboard plan is completed by self-reported ticks with a nested 24 dp target | UXF-05, UXF-13, UXF-24, UXF-44; W:614, 639 |
| First-time users are greeted "Welcome back, {name}"; the personalised cue is computed and never shown; no time-of-day greeting; the banner ignores its track, day, score and avatar parameters | UXF-27; FND-UI-12; ui_audit §3.1 (returning user), §3.3 (W:127-189; D:214-226) |
| Fallback numbers shown as data (overall 62, single-point trend 65 next to "0 sessions"); "1 Day Streak" on day 0 | UXF-56, UXF-57; FND-UI-07; D:181, 238; REPO:494 |
| 252 dp fixed-height hero pager clips at large font; the dashboard ignores reduced motion | UXF-42, UXF-45; W:282; NAV:227 vs D:160 |
| Avatar callback is dead; an update banner and an inline "AI Mentor" card compete for attention | UXF-09, UXF-11, UXF-54; D:162; D:335-345; W:1113 |

##### (b) Proposed experience

Today answers one question — "What should I practise now?" (DES-02 §2.1) — with **one session and one button**. Aarav opens the app on the train, reads one line of greeting, sees three steps that take about ten minutes, and taps **Start session**. Below the session card he sees his week at a glance and a one-line note of today's progress. Nothing else competes.

The session is generated by the plan engine (FS-03; SRS-FR-TODAY-002…009). Steps may come from tracks or from techniques (§3.4; PRD-FR-056, SRS-FR-PRAC-008); either way a step is shown by its template and title.

**Greeting (SRS-FR-TODAY-010; GOV-02 §6.1 places the "welcome experience" here).** Static text, no animation, `headlineSmall` for line 1 and `bodyLarge` `onSurfaceVariant` for line 2.

| Situation | Line 1 | Line 2 (at most one factual line, first matching row) |
|---|---|---|
| First ever visit to Today | "Welcome, {name}." — without a name: "Welcome to SpeakCraft." | "Your first session is ready." |
| Later visits | "Good morning / afternoon / evening, {name}." — without a name: "Good morning." etc. | 1. Recovery mode (SRS-BR-014): "Here's a shorter session to ease back in." 2. Session already done today: "Today's session is done." 3. Current streak ≥ 2: "Your streak is {n} days." 4. A focus from the last valid attempt exists: "Last time, your focus was: {focus noun phrase}." 5. Otherwise no second line. |

Rules: never "Welcome back" on the first visit (UXF-27); never a placeholder name such as "Speaker" (ui_audit §3.2); never a missed-day count or loss framing (PP-7); every fact comes from the local database. Time of day uses the device's local time: morning 05:00–11:59, afternoon 12:00–17:59, evening 18:00–04:59 (RECOMMENDATION). The name is the profile display name, which is optional (FS-02 Q3; §5 issue 14).

##### (c) Wireframe (compact, 360 dp; default state, one step done)

```
+------------------------------------------+
| Today                              (AV) |
+------------------------------------------+
|  Good morning, Aarav.                    |  headlineSmall (heading)
|  Your streak is 4 days.                  |  bodyLarge, onSurfaceVariant
|                                          |
|  +------------------------------------+  |  Card, surfaceContainerLow, shape large
|  | Today's session         ( Adjust ) |  |  titleMedium
|  | 3 steps · about 10 minutes         |  |  bodyMedium
|  |                                    |  |
|  | (v) Warm-up · Read aloud     2 min |  |  Done (success check + "Done")
|  |     A short project update   Done  |  |
|  |+----------------------------------+|  |  "Up next" row: primaryContainer accent
|  || (2) Main task · Structured  4 min ||  |
|  ||     answer              Up next  ||  |
|  ||     Tell me about a time you     ||  |
|  ||     fixed a problem at work      ||  |
|  |+----------------------------------+|  |
|  | (3) Words · Review 6 words   3 min |  |
|  |                                    |  |
|  | [[       Continue session       ]] |  |  the only filled button
|  +------------------------------------+  |
|                                          |
|  This week               4-day streak    |  titleSmall + EventAvailable (streak)
|   M    T    W    T    F    S    S        |
|  [v]  [v]  [v]  (o)   .    .    .        |  week strip (StreakWeek)
|  Best: 9 days                            |
|                                          |
|  Today: 1 of 3 steps · 2 min speaking    |  bodyMedium
+------------------------------------------+
| [Today]  Practice   Words   Progress     |
+------------------------------------------+
```

**Variants (same layout):**

| Variant | Change |
|---|---|
| New user, first visit | Line 2 "Your first session is ready."; streak area shows "Start your streak today" with an empty week (no seeded 1, DES-05 §11.17); TIP-01 under the greeting; button "Start session". |
| Onboarding attempt skipped (FS-02 A1) | An `OutlinedCard` below the session card: "Introduce yourself — about a minute. This sets your starting point." `[ Start ]` (tonal). |
| Recovery mode (SRS-BR-014) | Card subtitle "A shorter session to ease back in · about 5 minutes"; no reference to missed days. |
| Microphone not granted | Speaking step rows show "Needs microphone"; the card's filled button becomes `[[ Allow microphone ]]`; a text button "Learn today's words" starts the non-speaking Learn stage (SRS-FR-ONB-007). |
| Session done | Card title "Today's session is done", all rows "Done" (or "Skipped"); line "Suggested: {activity} · {track} · {n} min"; filled button `[[ Practise more ]]` opens that activity's SCR-PRAC-03 (SRS-FR-TODAY-011). |

**Medium:** single column, centred, max 640 dp. **Expanded:** two columns (DES-02 §7): left = greeting + session card; right = streak (week strip + "Best") and a progress summary ("This week: 3 days · 18 min speaking") with `( See progress )` → SCR-PROG-01.

##### (d) Interactions

| Control | Behaviour |
|---|---|
| Start session / Continue session | Opens SCR-SESS-01 at the first incomplete step, with step progress "Step n of N" (§3.3). The first step of a 10-minute session is a warm-up without preparation (SRS-FR-TODAY-004), so a returning user reaches recording in 2 taps: Start session → Record (UX-2). |
| Adjust | Opens a `ModalBottomSheet` "Adjust today's session": duration as a `SingleChoiceSegmentedButtonRow` 5 / 10 / 15 min; each incomplete step with `( Swap )`; text "{n} swaps left today" (max 3, SRS-FR-TODAY-009). Changes regenerate only incomplete steps (SRS-FR-TODAY-007). Completed steps are listed as read-only. |
| Step rows | Not separately clickable in R1 (one entry point; avoids nested targets, UXF-40). |
| Allow microphone | Permission flow §2.2.5; on grant the button returns to "Start session" without leaving Today. |
| Practise more | Opens SCR-PRAC-03 of the suggested activity. |
| Introduce yourself → Start | Opens SCR-SESS-01 with `onb-intro`. |
| Avatar | Opens SCR-PROF-01. |
| Week strip | Non-interactive (DES-05 §11.17). |
| Tab re-select | Scrolls to top. |
| Day change | A new dateKey generates a new plan the next time Today is shown (SRS-FR-TODAY-002); an attempt recorded across midnight keeps the dateKey from attempt time (SRS-BR-011). |

##### (e) Information hierarchy

1. Greeting (who / when, one fact) → 2. **Session card with the single primary action** → 3. Streak week → 4. Today's progress note → (5. optional "Introduce yourself" card).

##### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | Single column. At font scale 1.0 on 360 × 640 dp the primary button is above the fold (HRC-18). At 2.0 the card grows and the page scrolls; nothing is truncated. |
| Medium | Centred 640 dp column. |
| Expanded | Two columns as above; the session card column is ≥ 360 dp. |
| Height-compact | Rail; content scrolls; the button stays inside the card. |

##### (g) Loading, error, offline and permission states

| State | Presentation |
|---|---|
| Loading | §2.2.1 skeleton (greeting line, card, week strip) after 300 ms. Plan generation must meet its NFR budget; no spinner. |
| Error (plan cannot be generated or read) | Inline Error in place of the card: "Couldn't prepare today's session" / "Something went wrong on this phone. Your progress is safe." `[ Try again ]` `( Go to Practice )`. Greeting and streak still render if their data loaded. |
| Empty | Not applicable: a session always exists once content is bundled (content validity is a build gate, SRS-ERR-008). |
| Offline | Identical to online. |
| Permission | Microphone variant above; notifications are not requested here. |

##### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-22 | In every Today state exactly one filled button is present ("Start session", "Continue session", "Allow microphone" or "Practise more"), and at font scale 1.0 on a 360 × 640 dp window it is fully visible without scrolling (HRC-18, HRC-22). | Robolectric UI test per state |
| AC-DES-23 | On the first ever visit line 1 reads "Welcome, {name}." (or "Welcome to SpeakCraft." without a name) and never contains "Welcome back"; on later visits it reads "Good morning/afternoon/evening, {name}." per the local-time boundaries; the greeting has no animation; no placeholder name appears (SRS-FR-TODAY-010). | Unit test (greeting builder, clock fake) + UI test |
| AC-DES-24 | Line 2 shows at most one fact, chosen by the priority table, and each fact matches the local database; recovery copy contains no count of missed days and no loss-framed words from DES-05 §12.3 (HRC-34). | Unit test with fixtures |
| AC-DES-25 | The session card lists 3–4 steps (SRS-FR-TODAY-004), each with template label, title, estimated minutes and a text status ("Done", "Up next", "Skipped" or none); after Adjust or Swap, completed steps keep their position and content (SRS-BR-013). | Robolectric + plan-engine fake |
| AC-DES-26 | From a cold start with an existing plan, a returning user reaches the Recording state in at most 2 taps (Start session, Record) (UX-2; TC-UX-002; HRC-20). | Instrumented test |
| AC-DES-27 | The Adjust sheet offers 5 / 10 / 15 minutes and Swap only on incomplete steps; after 3 swaps in a day, Swap is disabled and the text reads "No swaps left today"; the sheet has a Close button and focus returns to "Adjust" when it closes (DES-07 A11Y-10). | Robolectric UI test |
| AC-DES-28 | A new user sees "Start your streak today" and no streak number; the streak number shown on Today equals the one on SCR-PROG-01 and SCR-PROF-01 for the same data (one streak, SRS-BR-011). | Robolectric with shared fixture |
| AC-DES-29 | Without microphone permission the filled button is "Allow microphone" and speaking steps read "Needs microphone"; granting the permission in system settings and returning restores "Start session" without navigation (re-check on resume). | Instrumented test |
| AC-DES-30 | When plan generation fails, the card area shows the inline Error with a working "Try again" and "Go to Practice"; while loading, only the static skeleton appears after 300 ms; in airplane mode Today is identical to online. | Robolectric with failing/slow fakes |
| AC-DES-31 | With TalkBack, the greeting is a heading; the session card is read as one summary ("Today's session, 3 steps, about 10 minutes, 1 done") followed by each step row, and the filled button is separately focusable (DES-07 A11Y-07); the week strip is one node with a text summary (A11Y-23); at font scale 2.0 step titles wrap without truncation and no container has a fixed height (A11Y-13). | Manual TalkBack script + Robolectric fontScale 2.0 |

#### 3.2.2 SCR-TODAY-02 Session complete

##### (a) Current problem

There is no real completion moment: the daily plan is "completed" by ticking rows and returning from seven different screens (UXF-05, UXF-24; DP:262). "Practice Hours" are computed as 3 minutes per ticked step (UXF-23; FND-DOM-08), so the summary would not be truthful even if shown.

##### (b) Proposed experience

After the last step's result, **Continue** leads to a short, calm summary: what was done, how long the user actually spoke, the streak, and the one focus to carry into tomorrow. It is the final screen of the session flow (full screen, navigation hidden), and **Done** returns to Today, which now shows its "session done" state.

##### (c) Wireframe (compact)

```
+------------------------------------------+
|                                     (x)  |
|                                          |
|  Today's session is done                 |  headlineMedium (heading, focused)
|  3 of 3 steps · 6 min speaking           |  bodyLarge
|                                          |
|  (event) 5-day streak                    |  streak colour icon + titleSmall
|          Best: 9 days                    |
|  +------------------------------------+  |
|  | (event) Seven days in a row.       |  |  milestone block: only on milestone
|  |         Nice work.                 |  |  days (static if already shown)
|  +------------------------------------+  |
|                                          |
|  Your focus for next time                |  titleMedium + Outlined.Flag
|  Pause briefly between your main         |  bodyLarge
|  points.                                 |
|                                          |
|  Words: 2 moved forward · 6 reviewed     |  bodyMedium
|==========================================|
|  [[               Done              ]]   |
|  ( Practise more )                       |
+------------------------------------------+
```

**Medium / expanded:** centred column, max 640 dp (session-flow width up to 720 dp, DES-02 §7).

##### (d)–(g) Interactions, hierarchy, responsive, states

| Part | Specification |
|---|---|
| Interactions | Done and Close → Today root (done state). Practise more → Practice (SCR-PRAC-01). System Back = Done. |
| Hierarchy | 1. Completion title, 2. counts and measured speaking time, 3. streak (and milestone), 4. focus for next time, 5. word progress, 6. Done. |
| Content rules | Speaking time = sum of recording durations of valid attempts (SRS-BR-018), rounded to whole minutes; under one minute reads "Less than 1 minute speaking". Skipped steps read "2 of 3 steps done · 1 skipped" (§5 issue 10). The focus line comes from the last valid speaking attempt's focus and is omitted if none exists. A first-ever streak day reads "You started a streak today." |
| Responsive | Content scrolls above the sticky action area at large font sizes. |
| Loading / error / offline | Data is already in memory from the session; no loading state. If the summary cannot be read, show only the title and Done (never invented figures). Offline identical. |

##### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-32 | SCR-TODAY-02 appears after Continue on the last step's result (or after the last step is skipped); it shows done and skipped counts, speaking time equal to the sum of valid recording durations, and the streak; the focus line appears only when a valid speaking attempt exists in the session; its only filled button is "Done", which returns to the Today root showing the done state. | Robolectric with session fixture |
| AC-DES-33 | On a milestone day the celebration animates only if it was not already shown on that day's result, and is static under reduced motion (DES-07 A11Y-21); on appearance TalkBack focus moves to the title and it is announced (as A11Y-09 requires for results); at font scale 2.0 nothing is clipped and Done remains reachable (A11Y-13, A11Y-30). | Robolectric + manual TalkBack |

---

### 3.3 Session player — SCR-SESS-01 (all states) and SCR-SESS-02

#### (a) Current problem

| Problem | Evidence |
|---|---|
| 10 recording surfaces (12 with dictation) run the same pipeline with different behaviour; ≥ 30 entry points lead to a recorder | UXF-12; FND-UI-04; ui_audit §7.2, §7.3 |
| 12 start labels, 8 "analyse" labels, 5 stop labels; only 3 record buttons show a mic icon | UXF-16; ui_audit §7.2 |
| Recording and analysis are separate steps; "Analyze" can be pressed before recording | UXF-04; SVM:472-497 |
| No transcript by default; free-speech "Pronunciation" derived from loudness and pitch; feedback ignores the task | UXF-31, UXF-32, UXF-33; FND-AI-01, FND-AI-08 |
| Two feedback cards with different composite formulas; many items, no single focus | UXF-34; CC:1326-1842 vs ETR:645-787 |
| Insufficient evidence shown as "Status: INSUFFICIENT_EVIDENCE • No score awarded" | UXF-35; CC:1268 |
| Mic denial silent on 7 recorders; recording leaks across screens; one lab can evaluate another's recording; work lost on rotation | UXF-36, UXF-37, UXF-38; FND-UI-05, FND-UI-06 |
| Offline analysis can spin forever; buttons stay enabled while loading | UXF-50, UXF-39 |
| 688-line "Record and Review Studio" packs video, upload, self-audit, transcription and history into one screen | UXF-20; SAC:103-790 |

#### (b) Proposed experience

**One session player** for every spoken task — Today steps, Practice activities (including technique exercises, PRD-FR-056), word stages, word reviews and the onboarding attempt (DES-02 §2.2; FS-04). It moves through clear, sequential states, and at every moment there is **one obvious control**: the Record button before and during recording, then **one filled button** on the result — "Continue" inside a Today session, "Try again" in Practice and Words (DES-05 §11.15). Stopping a recording starts the check automatically. After **every** submitted attempt the result shows, in this order: **what we heard → up to three labelled headline metrics → what went well → mistakes found → coach tips → actions → "Was this accurate?"** (Product Owner direction 2026-11-21; DES-05 §11.15 v0.2; PRD-FR-047; SRS-FR-EVAL-015/016; ARC-05 §8.4) — and **never a composite overall score for free speaking** (D-06; SRS-BR-006). Mistakes are evidence-backed and labelled, and when the checks find nothing the result says so honestly without claiming perfection. When the evidence is not good enough, the player says so plainly and offers another try (Inconclusive).

**State model** (SRS-FR-SESS-001; ARC-05 §2, §5):

```
            +--------+   prep template   +---------+   timer ends /    +-------+
 open ----->| Prompt |------------------>| Prepare |-- Skip prep ----->| Ready |
            +--------+   (auto-starts)   +---------+                   +-------+
                |  no-prep template (Read aloud, Listen & repeat,           |
                |  word stages): Prompt already shows the Ready control     |
                +--------------------------+--------------------------------+
                                           | tap Record (also skips prep)
                                           v
     call, audio-focus loss,         +-----------+
     app left, mic permission  <-----| Recording |  10 s warning; auto-stop at max
     revoked, storage full           +-----------+
            |                              | tap Stop / auto-stop
            v                              v
     +-------------+               +------------+
     | Interrupted |               | Processing |  "Checking your recording…"
     +-------------+               +------------+
     Record again -> Ready      gate fails |   valid |    failure / timeout |
                                           v         v                      v
                                 +--------------+ +--------+          +-------+
                                 | Inconclusive | | Result |          | Error |
                                 +--------------+ +--------+          +-------+
                          Try again -> Ready   Try again -> Ready (new attempt)
                          Skip this step       Continue -> next step / SCR-TODAY-02
```

**Contexts** (same states, different chrome and actions):

| Context | Top bar title | Step progress | Result primary / secondary |
|---|---|---|---|
| Today step | Step role ("Warm-up", "Main task", "Words") | "Step n of N" + segmented bar | `[[ Continue ]]` / `[ Try again ]` (tonal) |
| Practice activity (incl. technique exercise) | Activity title | None | `[[ Try again ]]` / `( Done )` |
| Word stage (from SCR-WORD-03) | "{word} · Say it" etc. | "Stage n of 5" text (stepper on SCR-WORD-02/03) | Not yet: `[[ Try again ]]` / `( Continue later )`; Passed: `[[ Continue ]]` / `( Practise again )` |
| Word review (from SCR-WORD-04) | "Review" | "Word n of N" + segmented bar | Not recalled: `[[ Try again ]]` / `( Next word )`; recalled: `[[ Next word ]]` |
| Onboarding first attempt | "Introduce yourself" | None | `[[ Try again ]]` / `[ Go to Today ]` (tonal) |

Every result has exactly one filled button (DES-05 §11.15): **"Continue" inside a Today session, "Try again" in Practice and Words**; the other action is tonal or text (consistency review, CL-59). Within Words, a **passed** threshold check — a passed word stage or a recalled review item — makes moving on the expected next step, so "Continue" or "Next word" is primary (RECOMMENDATION; §5 issue 22).

**Common frame (compact).** The prompt or result content scrolls; the recording panel (DES-05 §11.12) is a sticky bottom area in `surfaceContainer` with `bottomActionPadding`. Close (`Filled.Close`, "Close session") is top-start.

```
+------------------------------------------+
| (x)  Main task             Step 2 of 4   |  TopAppBar: Close, title, step text
|  [=======][=======][-------][-------]    |  segmented step progress (4 dp)
+------------------------------------------+
|                                          |
|   scrolling content:                     |
|   prompt card  /  result (feedback card) |
|                                          |
|==========================================|  sticky recording panel or actions
|   status line                            |
|   |||||||||||| level meter ||||||||||||  |
|                  (o)                     |
|                 Record                   |
|               0:00 / 1:30                |
+------------------------------------------+
```

#### (c) Wireframes per state (compact, 360 dp)

##### 3.3.1 Prompt (Read aloud warm-up, no preparation)

```
+------------------------------------------+
| (x)  Warm-up               Step 1 of 3   |
|  [==========][----------][----------]    |
+------------------------------------------+
|  +------------------------------------+  |  PromptCard
|  | Read aloud                         |  |  labelMedium (template label)
|  | "This week we finished the login   |  |  promptText 20/30 sp
|  | screen. Next, we'll test it with   |  |
|  | five customers and fix the issues  |  |
|  | they find."                        |  |
|  |                                    |  |
|  | [ Listen ]  [ Listen slowly ]      |  |  tonal; 1.0x and 0.75x (SRS-FR-SESS-012)
|  +------------------------------------+  |
|  Read at a comfortable pace.             |  bodyMedium tip from content
|==========================================|
|  Tap Record when you're ready.           |  status (Ready)
|  ||||||||||||||||||||||||||||||||||||||  |  idle meter, onSurfaceVariant
|                  (o)                     |  primary fill, Filled.Mic
|                 Record                   |
|               0:00 / 0:30                |
+------------------------------------------+
```

Listen & repeat uses the same layout with the instruction "Listen, then repeat:" above the sentence; reference audio never plays automatically. Activities linked to a technique (SRS-FR-PRAC-008) add one line under the prompt card: `Outlined.Info` + "Technique: {technique name}. Remember: {one-line idea}." (`bodyMedium`), which is static technique content written in our own words (SRS-BR-028), not feedback.

##### 3.3.2 Prepare (Structured answer, 30 s)

```
+------------------------------------------+
| (x)  Main task             Step 2 of 3   |
+------------------------------------------+
|  +------------------------------------+  |
|  | Structured answer                  |  |
|  | "Tell me about a time you fixed a  |  |
|  | problem at work."                  |  |
|  | Use: Situation · Task · Action ·   |  |
|  | Result                             |  |
|  +------------------------------------+  |
|  Plan your answer                0:24    |  titleSmall + timer style
|  [=================-------------------]  |  determinate LinearProgressIndicator
|  ( Skip preparation )                    |
|==========================================|
|  You can start recording any time.       |
|                  (o)                     |
|                 Record                   |
|               0:00 / 2:00                |
+------------------------------------------+
```

Preparation starts automatically for Answer a question (15 s †), Structured answer (30 s †) and Tell a story (30 s †) (SRS-FR-SESS-002); `onb-intro` uses 15 s (FS-02). Tapping Record skips the remaining time. When the timer ends the state becomes **Ready** (status "Time to answer. Tap Record when you're ready.") — recording never starts by itself. The timer is not a time limit with a consequence, so it meets WCAG timing guidance without an extension control. Under reduced motion the progress bar jumps per second instead of animating.

##### 3.3.3 Ready, Recording and near-limit

```
READY                                       RECORDING (0:23)                     NEAR LIMIT
|==========================================|==========================================|=================
| Tap Record when you're ready.            | (*) Recording                            | 10 seconds left
| |||||||||||||||||||||||||||||||||||||||| | |||||| ||||||||| |||||| ||||||||  ||||| | |||| ||||| ||||
|                  (o)                     |                  (■)                     |       (■)
|                 Record                   |                  Stop                    |       Stop
|               0:00 / 1:30                |            (*) 0:23 / 1:30               |  (*) 1:20 / 1:30
+------------------------------------------+------------------------------------------+-----------------
 primary fill, Filled.Mic                    recording fill, Filled.Stop on onRecording;
                                             recordingContainer chip "Recording";
                                             solid 8 dp dot by the timer; meter in recording colour
```

| Element | Ready | Recording | Notes |
|---|---|---|---|
| Status line (`bodyMedium`) | "Tap Record when you're ready." | Chip "Recording" (`recordingContainer`) | "10 seconds left" at max − 10 s, announced once; "We can't hear anything. Check your microphone." if input is near zero for 3 s (DES-05 §11.12) |
| Level meter | Idle bars `onSurfaceVariant` | Real input level, `recording` colour, ≤ 30 fps | Reduced motion: single level bar, ≤ 4 Hz. Hidden from semantics. Never animated without input. |
| Record button | `primary`, `Filled.Mic`, label "Record" | `recording`, `Filled.Stop`, label "Stop" | 80 dp; single tap; 500 ms debounce; Enter/Space when focused |
| Timer (`timer` style) | "0:00 / {max}" | Elapsed / max with tabular figures | Max per template: Read aloud and Listen & repeat 30 s, Answer 90 s, Structured answer and Tell a story 120 s † (SRS-FR-SESS-003); `onb-intro` 60 s |
| Prompt card | Visible | Still visible and scrollable (users read while speaking) | — |
| Countdown 3-2-1 | Off by default; Learning preference (DES-05 OQ-DS-8) | — | If on, announced before capture starts |

##### 3.3.4 Processing

```
|==========================================|
|  Checking your recording…                |  polite live region
|                  ( ◌ )                   |  48 dp CircularProgressIndicator replaces Record
|                                          |
+------------------------------------------+
 After 10 s:  "This is taking longer than usual."   ( Cancel )
```

Processing starts automatically on Stop or auto-stop — there is no Analyze button (UXF-04). Target: result visible within NFR-PERF-005 (≤ 2.5 s RD-MID for a 60 s attempt, excluding recogniser finalisation). At 10 s after Stop the delay message and Cancel appear (DES-05 §11.11). Cancel returns to Ready, discards the attempt and shows the snackbar "Your recording wasn't checked." If the evaluation step itself exceeds 2× its NFR-PERF-005 budget, the Error state is shown (SRS-FR-EVAL-013; reconciliation of the two timers is §5 issue 5).

##### 3.3.5 Result — common structure (Product Owner direction 2026-11-21; PRD-FR-047; SRS-FR-EVAL-015/016)

After **every submitted attempt** — speaking activity, technique exercise, word stage or review item — the result shows the mistakes our checks found and coach tips for the next try. The order is fixed for every template:

| # | Section | Content | Component |
|---|---|---|---|
| — | Header | `headlineMedium` "Here's how it went" + tier line (`bodyMedium`, `onSurfaceVariant`) using the ARC-05 §3 user-facing labels: "On-device transcription", "On-device transcription (no replay)", "Online transcription (you agreed)" or "Audio-only feedback" (§5 issue 4) | DES-05 §11.15 header |
| 1 | **What we heard** | Transcript (first 3 lines, "Show full transcript"), "Play your recording" when retained, "How speech recognition works" | Transcript view §11.20 |
| 2 | **Headline metrics** | Up to 3 Metric tiles with Measured / Estimated badges (ARC-05 §7, table below), then `( All measurements (n) )` and `( How we measured this )` | Metric tile §11.13, Evidence badge §11.14 |
| 3 | **What went well** | One sentence (strength, ARC-05 §8.2) | Feedback card strength row §11.15 |
| 4 | **Mistakes found** | Each issue detected by deterministic checks, with evidence (quoted span, count or value) and its Measured / Estimated badge; at most 3 shown in ARC-05 §8.2 priority order, then `( Show all (n) )`; honest empty state when nothing is found | **Mistake list** — proposed new DES-05 pattern (§5 issue 19) |
| 5 | **Coach tips** | The focus tip ("Try this next", SRS-FR-EVAL-005) plus up to 2 more, chosen deterministically from the content pack's technique tips by trigger (issue code, metric band, template or technique); each links to its technique (SCR-PRAC-04) and to the source video. Followed, only for techniques that define one, by a **Self-check** (below) | **Coach tip card** — proposed new DES-05 pattern (§5 issue 19) |
| 6 | Actions | **One filled button: "Continue" inside a Today session, "Try again" in Practice and Words** (DES-05 §11.15); the other action is tonal or text (table in (b)) | Buttons §11.1 |
| 7 | **Was this accurate?** | "Yes" / "No" + optional reasons | Feedback card accuracy row §11.15 |

This order is the DES-05 §11.15 v0.2 order (transcript → metrics → strength → mistakes found → coach tips → actions → "Was this accurate?"); see §5 issue 20.

**Mistake list (proposed pattern).** Section title `titleMedium` "Mistakes found" with the count in text ("Mistakes found (2)"), marked as a heading. Items sit in one `Card` (`surfaceContainerLow`, radius `medium`) separated by inset `borderHairline` dividers. Each item: what was found (`titleSmall`), the evidence (`bodyMedium`; quoted words with `targetWordEmphasis` where relevant), the Evidence badge, and for metric-based items a trailing info `IconButton` → SCR-SESS-02. **Neutral styling only**: no error colour, no warning icon, no "wrong" or "fail" wording, no total "score" for mistakes (PP-7; DES-05 §3.5 rule 3). The content is authoritative in **ARC-05 §8.4**: an issue is raised only when its source metric is non-null and outside the target band by more than the metric's noise band, so borderline values are never shown as mistakes. Display mapping (copy is RECOMMENDATION; codes, evidence and labels are ARC-05 §8.4):

| ARC-05 §8.4 issue code | Item title (UI) | Evidence shown (example) | Badge |
|---|---|---|---|
| ISSUE_WORD_NOT_RECOGNISED | "Not recognised as intended: '{word}'" | The word in context: "…with five **customers** and…" | Measured |
| ISSUE_WRONG_FORM | "Different word recognised" | "We heard '{heard}' for '{word}'." | Measured |
| ISSUE_TARGET_WORD_MISSING | "Target word missing" | "'{word}' wasn't in what we heard." | Measured |
| ISSUE_TOO_SHORT | "Shorter than the target" / "Sentence too short" | "31 s · target 45–90 s" / "4 words · needs at least 5" | Measured |
| ISSUE_STRUCT_ELEMENT_MISSING | "No {Result} part found" | Missing element; cue phrases found for the others | Estimated |
| ISSUE_FILLERS_HIGH | "Filler words above the usual range" | "'basically' ×5, 'um' ×2" | Estimated |
| ISSUE_PAUSES_LONG | "{n} long pauses" | Count; longest pause and where it was: "2.4 s after '…the login screen'" | Measured |
| ISSUE_PACE_FAST / ISSUE_PACE_SLOW | "Faster than the target pace" / "Slower than the target pace" | "171 words/min · target 120–160" | Measured |
| ISSUE_HEDGING_HIGH | "Many hedging phrases" | "'I think maybe' ×3, 'sort of' ×2" | Estimated |
| ISSUE_LOW_VARIETY | "Some words repeated often" | "'thing' ×6, 'good' ×4" | Estimated |
| ISSUE_GRAMMAR_POSSIBLE (≤ 2, SRS-FR-EVAL-006) | "Possible grammar issue" | "'he don't' → try 'he doesn't'" | Estimated |
| ISSUE_TOO_QUIET | "Recording was quiet or uneven" | "Parts of the recording were hard to hear." | Measured (quality) |

**Self-check (ARC-05 §8.4; RECOMMENDATION for presentation).** When the activity's technique has aspects that audio cannot show (for example eye contact, gestures, facial expression — NG-07), a short "Self-check" block appears after Coach tips: title `titleMedium` "Self-check", the line "Only you can judge these. Tick what you did.", and up to 3 `Checkbox` rows (whole row is the target, DES-05 §11.22). Answers are stored as **self-reported**: they carry **no** Measured or Estimated badge, never feed metrics, trends or the streak, and are shown back only as "You ticked: {item}" (§5 issue 21).

**Honest empty state.** "Our checks didn't find issues in this attempt." followed by one line that names the checks that ran, generated from the same list (never "Perfect" or "No mistakes"): e.g. "We checked pace, pauses, filler words, length and a small set of grammar patterns. We don't check pronunciation or accent." On T3b: "We checked speaking time, pauses and speech ratio. Without a transcript we can't check words or grammar."

**Coach tip card (proposed pattern).** Up to 3 `Card`s (`surfaceContainerLow`, radius `medium`, `cardPadding`, `cardGap` between). The first is the focus: `Outlined.Flag` + `titleMedium` "Try this next"; the others carry `titleMedium` "Tip". Body: one actionable sentence in `bodyLarge`. Footer: `( Technique: {technique name} )` → SCR-PRAC-04, and `( Watch the original lesson )` with the supporting text "Opens outside SpeakCraft" → the specific source video at its timestamp through an outbound intent (SRS-FR-PRAC-009). Selection and ranking are ARC-05 §8.4 (trigger specificity issue code > metric band > technique > template, then tip priority, then a stable hash so wording varies but stays reproducible); the focus tip is the top tip for the highest-priority issue. If no issue is found, the strength is shown with one general tip for the activity's technique (ARC-05 §8.4 step 4). Tips are content written in our own words (SRS-BR-028); no generative or cloud model is involved (D-04). Calm rules: at most 3 cards, no icons other than the focus flag, no "AI".

Headline metrics per template are exactly ARC-05 §7:

| Template | Tile 1 | Tile 2 | Tile 3 | Threshold |
|---|---|---|---|---|
| Read aloud | Recognised as intended `{Measured}` | Pace `{Measured}` | Long pauses `{Measured}` | Pass mark 75 % † |
| Listen & repeat | Recognised as intended `{Measured}` | Speaking time vs reference `{Measured}` | Words `{Measured}` | Pass mark 75 % † |
| Answer a question | Pace `{Measured}` | Filler words / min `{Estimated}` | Length vs target `{Measured}` | None (D-06) |
| Structured answer | Structure elements `{Estimated}` | Pace `{Measured}` | Filler words / min `{Estimated}` | None |
| Tell a story | Length vs target `{Measured}` | Long pauses `{Measured}` | Vocabulary variety `{Estimated}` | None |
| Any template on T3b | Speaking time `{Measured}` | Long pauses `{Measured}` | Speech ratio `{Measured}` | None; transcript checks "Transcript needed for this check" |

> Technique names, tips and quoted speech in the wireframes below are **illustrative placeholders** written for this document. Real content comes from the content pack under SRS-BR-028.

##### 3.3.6 Result — free speaking (Answer a question; Today context)

```
+------------------------------------------+
| (x)  Main task             Step 2 of 3   |
+------------------------------------------+
|  Here's how it went                      |  headlineMedium (focus lands here)
|  On-device transcription                 |  tier line
|                                          |
|  What we heard                           |  1  titleMedium heading
|  [ (>) Play your recording ]   0:58      |     only if the recording is retained
|  "So I'm Aarav, I work as a software     |     bodyLarge, selectable, first 3 lines
|  engineer… basically I lead the …"       |
|  ( Show full transcript )                |
|                                          |
|  +------------------------------------+  |  2  MetricTile rows (compact)
|  | Pace  {Measured}        128    (i) |  |
|  | words/min · Target 120–160         |  |
|  | Within target                      |  |     neutral text, never green/red
|  +------------------------------------+  |
|  | Filler words  {Estimated} 4.1  (i) |  |
|  | per min · Usual range under {x} †  |  |
|  +------------------------------------+  |
|  | Length  {Measured}      58 s   (i) |  |
|  | Target 45–90 s · Within target     |  |
|  +------------------------------------+  |
|  ( All measurements (4) )                |
|  ( How we measured this )                |
|                                          |
|  (check) What went well                  |  3  titleMedium + Outlined.CheckCircle
|  Your pace stayed steady, around 128     |     one sentence
|  words a minute.                         |
|                                          |
|  Mistakes found (2)                      |  4  Mistake list (proposed pattern)
|  +------------------------------------+  |
|  | Filler words above the usual range |  |     titleSmall
|  | "basically" x5, "um" x2 - 4.1/min  |  |     bodyMedium evidence
|  | {Estimated}                    (i) |  |
|  |  - - - - - - - - - - - - - - - - - |  |     inset hairline divider
|  | Possible grammar issue             |  |
|  | "he don't" -> try "he doesn't"     |  |
|  | {Estimated}                        |  |
|  +------------------------------------+  |
|                                          |
|  Coach tips                              |  5  Coach tip cards (proposed pattern)
|  +------------------------------------+  |
|  | (flag) Try this next               |  |     the focus (SRS-FR-EVAL-005)
|  | Pause silently instead of saying   |  |
|  | "basically".                       |  |
|  | ( Technique: {technique name} )    |  |     -> SCR-PRAC-04
|  | ( Watch the original lesson )      |  |     -> specific video + timestamp
|  |   Opens outside SpeakCraft         |  |
|  +------------------------------------+  |
|  | Tip                                |  |
|  | Say your main point first, then    |  |
|  | give one example.                  |  |
|  | ( Technique: {technique name} )    |  |
|  +------------------------------------+  |
|                                          |
|  [[             Continue           ]]    |  6  the only filled button (Today session)
|  [ Try again ]                           |     tonal; Practice: [[ Try again ]] + ( Done )
|                                          |
|  Was this accurate?                      |  7
|  [ (thumb-up) Yes ]  [ (thumb-down) No ] |     outlined, icon + text
+------------------------------------------+
```

- **Structured answer**: tile 1 reads "Structure (STAR) `{Estimated}` — 3 of 4 parts found" with "Situation: found · Task: found · Action: found · Result: not found" (text, not icons alone); the missing part also appears in Mistakes found, and the focus usually names it ("Add the Result: what changed because of what you did?", ARC-05 §8.2).
- **Tell a story**: Vocabulary variety needs ≥ 25 words; otherwise the tile shows "—" and "Not available — needs at least 25 words" (§2.2.7).
- **TIP-02** (first-use tip) appears under the headline metrics on the first valid result ever.

##### 3.3.7 Result — Read aloud and Listen & repeat (threshold templates)

```
|  Here's how it went                      |
|  On-device transcription                 |
|  What we heard                           |
|  "This week we finished the login screen |
|  next we'll test it with five costumers" |
|  +------------------------------------+  |
|  | Recognised as intended {Measured}  |  |
|  |                         82 %   (i) |  |
|  | Pass mark 75 % · Passed            |  |   below mark: "Not yet. Try again."
|  +------------------------------------+  |   in onSurface — no error colour, no "Fail"
|  | Pace {Measured} ... | Long pauses ...|  |
|  (check) What went well                  |
|  You read the whole passage at a steady  |
|  pace.                                   |
|  Mistakes found (2)                      |
|  | Not recognised as intended:        |  |
|  | "customers"  We heard "costumers". |  |
|  | {Measured}                         |  |
|  | Not recognised as intended:        |  |
|  | "issues"  No match in what we heard|  |
|  Coach tips                              |
|  | (flag) Try this next               |  |
|  | Try these words again: "customers",|  |
|  | "issues". Listen to the slow       |  |
|  | version first.                     |  |
|  | ( Technique: {technique name} )    |  |
|  [[ Continue ]]  [ Try again ]           |  Today; Practice: [[ Try again ]] ( Done )
```

"Recognised as intended" is never called pronunciation or accent (SRS-BR-004; NG-03). The "How we measured this" text includes the ARC-05 §9 caveat: speech recognition may recognise a word even when it is not pronounced clearly, and may "correct" a word you said differently.

##### 3.3.8 Result — T3b (audio-only) variant

```
|  Here's how it went                      |
|  Audio-only feedback                     |  tier line
|  What we heard                           |
|  No transcript — audio-only feedback.    |  replaces the transcript box (no empty box)
|  This phone can't turn speech into text  |
|  offline, so checks on words aren't      |
|  available.                              |
|  [ (>) Play your recording ]   0:58      |  T3b keeps audio (ARC-05 §3)
|  ( Learn more )  ( Use online recognition… )   second link only where T3a is possible
|  +------------------------------------+  |
|  | Speaking time {Measured}   41 s    |  |
|  | Long pauses   {Measured}   3       |  |
|  | Speech ratio  {Measured}   71 %    |  |
|  +------------------------------------+  |
|  (check) What went well   (audio metrics only)
|  Mistakes found (1)                      |
|  | 3 long pauses  {Measured}          |  |
|  Coach tips  (focus + up to 2, by audio triggers)
|  [[ Continue ]]  [ Try again ]           |  Today; Practice: [[ Try again ]] ( Done )
```

On Read aloud and Listen & repeat in T3b, the Recognised-as-intended tile shows "—" and "Transcript needed for this check"; no pass or "Not yet" is shown (SRS-FR-SPCH-007), and the Mistakes empty-state note says which checks need a transcript. When a T3a attempt loses the network, the tier line reads "Transcript wasn't available this time — you were offline." and the same audio-only layout applies.

##### 3.3.9 Try again and comparison

"Try again" starts a new attempt on the same activity (SRS-FR-SESS-008). On the new result, each headline tile adds a comparison line against the previous **valid attempt at the same activity**: "Last time 142 · 14 fewer" (text required; a decorative arrow is optional). The strength may name the improvement ("Fewer filler words than last time: 6 → 2", ARC-05 §8.2). No comparison is shown for a first attempt, across different activities, or against an Inconclusive attempt. **Medium/expanded:** tiles show two value columns, "This attempt" and "Last attempt".

##### 3.3.10 Inconclusive

```
+------------------------------------------+
| (x)  Main task             Step 2 of 3   |
+------------------------------------------+
|  +------------------------------------+  |  Inconclusive state card
|  | (?) We couldn't measure this       |  |  inconclusiveContainer; HelpOutline 32 dp
|  |     attempt                        |  |  titleMedium
|  |                                    |  |
|  | It was too quiet to hear clearly.  |  |  reason (bodyMedium)
|  | Hold the phone a little closer, or |  |  advice
|  | move somewhere quieter.            |  |
|  |                                    |  |
|  | This attempt doesn't count against |  |  reassurance
|  | you.                               |  |
|  | ( Tips for a clear recording )     |  |  -> tips sheet
|  +------------------------------------+  |
|  What we heard                           |  only if any words were recognised
|  "my name is"                            |
|==========================================|
|  [[             Try again          ]]    |
|  ( Skip this step )                      |  Today context only
+------------------------------------------+
```

Reason copy for the ARC-05 §5 gate outcomes (RECOMMENDATION; copy states measured facts, never thresholds that may change):

| Gate reason | Reason sentence | Measured fact (when available) | Advice |
|---|---|---|---|
| `TOO_SHORT` | "The recording was too short to check." | "We heard about {n} second(s) of speech." | "Try again and keep speaking until you've finished your answer." |
| `TOO_QUIET` | "It was too quiet to hear clearly." | — | "Hold the phone a little closer, or move somewhere quieter." |
| `NO_SPEECH` | "We didn't hear any speech." | — | "Check that nothing is covering the microphone, then try again." |
| `NO_TRANSCRIPT` | "Speech recognition didn't return any words this time." | — | "Speak a little more slowly and clearly, then try again." |
| `TOO_FEW_WORDS` | "We recognised only {n} words. This activity needs a longer answer to give feedback." | "{n} words" | "Try to say two or three full sentences." |

No metric values, no red, no "fail". The attempt counts as neither completion, failure, streak day nor practice time (SRS-BR-008). Announced politely; `REJECT` haptic. The recording is deleted immediately (SRS-BR-022).

##### 3.3.11 Interrupted, Error, Permission and microphone busy

```
INTERRUPTED                                ERROR (check failed)
+------------------------------------------+   +------------------------------------------+
|  (mic-off) Recording stopped             |   |  (!) We couldn't check this recording    |
|  The recording stopped because of a      |   |  Something went wrong on this phone.     |
|  phone call. Nothing was saved from      |   |  Your recording is still here.           |
|  this attempt.                           |   |                                          |
|==========================================|   |==========================================|
|  [[          Record again          ]]    |   |  [[          Check again           ]]    |
+------------------------------------------+   |  ( Record again )                        |
 neutral surfaceContainerLow card               +------------------------------------------+
```

| Situation | Cause text ("The recording stopped because …") | Action | Source |
|---|---|---|---|
| Phone call | "… of a phone call." | `[[ Record again ]]` | SRS-FR-SESS-010 |
| Audio focus lost (another app played sound) | "… another app started playing sound." | `[[ Record again ]]` | ARC-05 §4.1 |
| App left during recording | "… you left SpeakCraft." | `[[ Record again ]]` | SRS-FR-SESS-010 |
| Microphone permission revoked | "… microphone access was turned off." | `[[ Open settings ]]` | SRS-FR-SESS-010 |
| Storage full | "… your phone is out of storage." | `[[ Open storage settings ]]` + `( Record again )` | SRS-ERR-004 |
| Check failed or too slow | (Error card) | `[[ Check again ]]` re-runs on the retained audio; `( Record again )` | SRS-FR-EVAL-013 |
| Save failed | "Couldn't save this attempt. Try again." — no result is shown as saved | `[[ Try again ]]` (retries the save) | SRS-ERR-005 |
| Microphone not yet granted | Recording panel shows the Permission card (§2.2.5); first time → SCR-PERM-01 | `[[ Allow microphone ]]` / `[[ Open settings ]]` | SRS-ERR-001 |
| Microphone busy (another app, a call) | Disabled Record button with `Outlined.MicOff`; "The microphone is being used by another app." | `( Try again )` | DES-05 §11.12 |
| Reference audio unavailable | Listen buttons hidden; "Reference audio isn't available on this device." | — | SRS-ERR-010 |

Interrupted attempts create no attempt record and partial audio is discarded (SRS-FR-SESS-010).

#### (d) Interactions

| Control | Behaviour |
|---|---|
| Record / Stop | The single control before and during recording (one `RecordButton` component everywhere). One tap starts (after the optional countdown), one tap stops; taps within 500 ms are ignored. |
| Listen / Listen slowly | Text-to-speech of the reference at 1.0× and 0.75× (SRS-FR-SESS-012); pressing again restarts; recording cannot start while reference audio plays (the Record tap stops playback first). |
| Skip preparation | Prepare → Ready. |
| Close (x) | In Prompt, Prepare, Ready, Result, Inconclusive, Interrupted or Error: closes immediately (results are already saved). In Recording or Processing: dialog "Leave this attempt?" — "Keep recording" / "Leave" (§2.8; SRS-FR-SESS-011). In a Today session, progress is kept and Today shows "Continue session". |
| System Back | Same as Close. |
| Continue (Today, filled) | Next incomplete step; after the last, SCR-TODAY-02. The filled button on a result inside a Today session (DES-05 §11.15). |
| Try again (filled when there is no next step; tonal otherwise) | New attempt on the same activity → Ready (prep templates return to Prepare). Filled on Practice/Techniques results and failed or Inconclusive word stages; tonal in a Today session, in onboarding and after a passed word stage or review item, where "Continue" is filled (DES-05 §11.15 v0.2). |
| Show all (n) (Mistakes found) | Expands the remaining issues in place, in ARC-05 §8.4 order; becomes "Show fewer". Instant under reduced motion. |
| Technique: {name} (coach tip) | Opens SCR-PRAC-04 for that technique; Back returns to the result (the result stays saved). |
| Watch the original lesson (coach tip) | Opens the specific source video at its timestamp in the YouTube app or browser through an outbound intent (SRS-FR-PRAC-009). Offline: snackbar "You're offline. The original lesson needs a connection." |
| Self-check rows | Toggle the checkbox; stored as self-reported only (ARC-05 §8.4). |
| Skip this step (Today, Inconclusive or Prompt overflow) | Marks the step Skipped and moves on; a skipped step is not complete (SRS-BR-009; §5 issue 10). |
| Info (i) on a tile / "How we measured this" | Opens SCR-SESS-02 at that metric / at the overview. |
| All measurements | Expands in place (250 ms `standard`; instant under reduced motion). |
| Show full transcript | Expands in place. |
| Play your recording | Plays the local recording; becomes "Pause". Only one audio source plays at a time. |
| Was this accurate? | "Yes" → replaced by "Thanks for telling us." "No" → three optional `FilterChip`s ("The transcript was wrong", "A measurement was wrong", "The tip wasn't useful") and `( Send )`; stored locally in ENT-13, never synced (SRS-FR-EVAL-009). |
| Tips for a clear recording | `ModalBottomSheet` with 4 tips (quiet place, phone 20–30 cm away, speak at normal volume, nothing covering the microphone). |
| Rotation / process | State survives configuration changes, including an in-progress recording (SRS-FR-SESS-013). Navigation away or backgrounding stops capture (SRS-FR-SESS-014). |

#### (e) Information hierarchy

- **Before recording:** 1. what to say (prompt card), 2. how long you have (timer), 3. the Record button, 4. reference audio and preparation.
- **While recording:** 1. that it is recording (chip, button, dot — text plus shape), 2. time left, 3. the prompt.
- **Result:** header and tier line, then 1. what we heard, 2. up to three labelled headline metrics, 3. one strength, 4. mistakes found (three first), 5. coach tips (focus first), 6. actions (one filled button: "Continue" when there is a next step — Today session, onboarding, passed word stage or review item; otherwise "Try again"), 7. accuracy question (Product Owner order, 2026-11-21; DES-05 §11.15 v0.2). Detail (other metrics, remaining mistakes, explanations, full transcript) is one tap away (UX-6).

#### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | As sketched. During Recording at font scale 2.0 on 360 × 640 dp the status line, Record button and timer stay visible together in the sticky panel; the prompt scrolls above. |
| Result actions | Inline, directly after Coach tips (and Self-check) and before "Was this accurate?", in the Product Owner order; they are not sticky, so reading order and visual order match and nothing is hidden behind a sticky bar at large font sizes (DES-07 A11Y-30). The Close button stays available in the top bar. |
| Medium | Full screen, content centred, max 640 dp; tiles as a grid (min 160 dp each); comparison in two columns. |
| Expanded | Full screen, centred, max 720 dp (DES-05 §5.3). |
| Height-compact (landscape phone) | Two panes: prompt (scrolling) at start, recording panel at end with a 64 dp Record button; result single column. |

#### (g) Loading, error, offline states

| State | Presentation |
|---|---|
| Loading | Activity content is bundled and read locally; no skeleton expected. If the activity cannot be found (retired content), the Error template "This activity isn't available any more." + `[ Go back ]`. |
| Error | §3.3.11. |
| Offline | Identical for T1, T2 and T3b. T3a continues audio-only for the attempt with the tier line explaining why (§2.2.4). |
| Permission | §3.3.11 and §2.2.5. |

#### 3.3.12 SCR-SESS-02 Feedback detail sheet ("How we measured this")

**(a) Current problem.** Metrics had no explanation of method or limits; confidence values were invented constants (0.85, 0.94) and passed to the evaluator (UXF-59; SVM:129, 193); the help system showed unrelated guides (UXF-10).

**(b) Proposed experience.** A `ModalBottomSheet` (DES-05 §11.6) that explains any metric in four short parts — **what it is · how we measure it · why it matters · its limits** (DES-05 §12.4 rule 5). Text is generated from the ARC-05 metric catalogue, the single source (ARC-05 §10). Three entry modes: one metric (from a tile's info button), overview of all metrics for this attempt (from "How we measured this"), and "How speech recognition works" (from the transcript section: tier, what stays on the phone, and that recognition can miss or "correct" words).

```
+------------------------------------------+
|                 -----                    |  drag handle
|  How we measured pace               (x)  |  titleLarge + Close IconButton
|  128 words/min   {Measured}              |  metricValue + badge
|  Target 120–160 · Within target          |
|                                          |
|  What it is                              |  titleSmall (heading)
|  How many words you said for each        |  bodyMedium
|  minute you were speaking.               |
|  How we measure it                       |
|  We count the words in the transcript    |
|  and divide by your speaking time.       |
|  Pauses don't count as speaking time.    |
|  Why it matters                          |
|  A steady, moderate pace is easier for   |
|  listeners to follow.                    |
|  Its limits                              |
|  If speech recognition misses words,     |
|  pace reads lower. Differences under     |
|  about 10 words/min aren't meaningful.   |  noise band (ARC-05 §6)
|                                          |
|  ( How speech recognition works )        |
+------------------------------------------+
```

Metric-specific content: **Filler words** lists what was counted ("um ×3, basically ×2") and the caveat that some recognisers drop "um" and "uh" (SRS-FR-EVAL-010); **Structure elements** lists each element found with the cue phrase that triggered it ("Situation — 'last year at my company'") (SRS-FR-EVAL-014); **Possible issues** show the span and suggestion; **Recognised as intended** shows the reference with unrecognised words marked by weight and underline plus the text "not recognised" (not colour alone). Recognition confidence appears only if the recogniser supplied it (SRS-BR-005).

**(f) Responsive:** max width 640 dp, centred on medium and expanded; `skipPartiallyExpanded = true`; content scrolls. **(g) States:** no loading (data in memory); offline identical.

#### (h) Acceptance criteria — SCR-SESS-01 and SCR-SESS-02

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-34 | Every speaking context (Today, Practice, technique exercises, word stages, reviews, onboarding) uses the same Record button component with the labels exactly "Record" and "Stop"; one tap starts and one tap stops; a second tap within 500 ms is ignored; no "Analyze", "Submit" or "Check" button exists, and processing starts automatically after Stop (UXF-04, UXF-16). | Robolectric UI test per context + code search |
| AC-DES-35 | The Prompt state shows the template label and the prompt in `promptText`; Read aloud and Listen & repeat show "Listen" and "Listen slowly" (1.0× and 0.75×) and never autoplay; when text-to-speech is unavailable both are hidden and "Reference audio isn't available on this device" is shown; activities linked to a technique show the technique reminder line labelled as a technique, not as feedback (SRS-FR-PRAC-008). | Robolectric with TTS fake |
| AC-DES-36 | Preparation starts automatically for Answer, Structured answer and Tell a story with the configured duration, shows remaining time in text, and ends in Ready; tapping Record during preparation starts recording at once; "Skip preparation" goes to Ready; recording never starts without a tap. | Robolectric with clock fake |
| AC-DES-37 | While recording, the timer shows elapsed and maximum time with tabular figures; the state is conveyed by the "Recording" chip, the Stop icon and text, not colour alone (DES-07 A11Y-23); the level meter moves only with real input; "10 seconds left" appears and is announced once; at the maximum the recording stops and processing begins; after 3 s of near-silence the status reads "We can't hear anything. Check your microphone." | Instrumented test with audio fixtures |
| AC-DES-38 | With TalkBack, the Record button is announced "Start recording" or "Stop recording"; recording start and stop are announced; focusing the button reads "Recording, 23 seconds" without continuous announcements; the level meter is excluded from semantics (DES-07 A11Y-03, A11Y-06, A11Y-08, A11Y-26). | Semantics test + manual TalkBack |
| AC-DES-39 | At font scale 2.0 on 360 × 640 dp during Recording, the status line, Record button and timer are visible together without scrolling; in a landscape window under 480 dp tall the Record button sits beside the prompt; under reduced motion all state changes are instant and the level meter is a single bar updated ≤ 4 Hz (A11Y-13, A11Y-21). | Robolectric fontScale/size matrix |
| AC-DES-40 | Processing shows a progress indicator with "Checking your recording…" in a polite live region; at 10 s it adds "This is taking longer than usual." and Cancel; Cancel returns to Ready with the snackbar "Your recording wasn't checked."; the indicator is never shown without a terminal state (result, Inconclusive or Error) or Cancel (HRC-02). | Robolectric with slow evaluator fake |
| AC-DES-41 | For every valid attempt (speaking activity, technique exercise, word stage, review item, onboarding) the result follows the order header → What we heard → headline metrics → What went well → Mistakes found → Coach tips → actions → Was this accurate? (PRD-FR-047); it contains exactly one strength sentence; it shows at most 3 metric tiles, which are the ARC-05 §7 headline metrics for the template and tier (including the T3b row), each with value, unit and the badge text "Measured" or "Estimated"; a metric without inputs shows "—" and "Not available — {reason}", never a default (SRS-BR-002, SRS-BR-003; HRC-30, HRC-31). | Parameterised Robolectric test over templates × tiers × contexts |
| AC-DES-42 | Coach tips show the focus tip titled "Try this next" first and at most 2 further tips; for a fixed attempt and content version the same tips appear in the same order on every render (ARC-05 §8.4 deterministic selection); each tip has "Technique: {name}" opening SCR-PRAC-04 and, where provenance exists, "Watch the original lesson" with "Opens outside SpeakCraft"; when no issue is found one general tip for the activity's technique is shown; no tip text mentions "AI" or a creator name (SRS-FR-EVAL-016, SRS-BR-028). | Unit test on tip selector + Robolectric |
| AC-DES-43 | For Answer a question, Structured answer and Tell a story, no overall score, "x/100", ring, grade, star rating or colour-coded judgement appears on the result, in SCR-SESS-02 or in attempt detail (D-06; SRS-BR-006); metric status words use neutral text colour. | Semantics-tree assertion + design review |
| AC-DES-44 | Read aloud and Listen & repeat show "Recognised as intended: N %" with "Pass mark 75 %" and "Passed" or "Not yet. Try again." in `onSurface` (no error colour, no "Fail"); words not recognised as intended are listed as text; the word "pronunciation" appears nowhere in the session player (SRS-BR-004, SRS-BR-007). | Robolectric + string search |
| AC-DES-45 | "What we heard" shows the first 3 lines with "Show full transcript", target words in `targetWordEmphasis`, long pauses as " … " announced as "pause", and text identical to the recogniser output; the tier line uses an ARC-05 §3 user-facing label; "Play your recording" appears only when the recording is retained (T1 or T3b, and retention is not "delete after feedback", SRS-FR-SESS-009), and on T2 and T3a the section states "Replay isn't available on this phone." | Robolectric with transcript, tier and retention fakes |
| AC-DES-46 | On a T3b attempt the tier line reads "Audio-only feedback", the tiles are Speaking time, Long pauses and Speech ratio, the transcript section explains why there is no transcript (no empty box), transcript-based checks read "Transcript needed for this check", and "Use online recognition…" appears only where T3a is possible. | Robolectric with tier fake |
| AC-DES-47 | "Mistakes found" lists only issues raised by the ARC-05 §8.4 catalogue for the attempt, each with its evidence (quoted words, count or value against target) and its Measured or Estimated badge; the first 3 appear in ARC-05 §8.2 priority order with "Show all (n)" for the rest; no error colour, warning icon, "wrong" or "fail" wording appears; with no issues the section reads "Our checks didn't find issues in this attempt." plus a one-line note naming the checks that ran (never "Perfect"); an Inconclusive attempt shows its gate reason instead; self-check answers carry no evidence badge and never change metrics, trends or the streak (SRS-FR-EVAL-015; SRS-FR-WORD-014). | Parameterised Robolectric over issue fixtures + unit test |
| AC-DES-48 | After Try again, each headline tile shows the previous valid attempt's value at the same activity and the difference as text; no comparison appears for a first attempt, a different activity or a previous Inconclusive attempt (SRS-FR-SESS-008). | Robolectric with attempt history fixture |
| AC-DES-49 | "Was this accurate?" offers outlined "Yes" and "No" buttons with icon and text; "No" reveals three optional reasons; the answer is stored locally (ENT-13), is never included in sync payloads, and is replaced by "Thanks for telling us."; it can be changed later on SCR-PROG-04. | Robolectric + sync payload unit test |
| AC-DES-50 | Every result has exactly one filled button (DES-05 §11.15): "Continue" inside a Today session, "Try again" in Practice, Words and onboarding, with the other action as a tonal or text button (Today "Try again", Practice "Done", onboarding "Go to Today", not-yet word stage "Continue later", unrecalled review item "Next word"); only a passed word stage ("Continue") or a recalled review item ("Next word") makes the moving-on action primary; the actions sit inline after Coach tips and before "Was this accurate?", are reachable by scrolling at font scale 2.0, and no focused element is hidden behind a sticky bar (DES-07 A11Y-30). | Robolectric per context at fontScale 1.0 and 2.0 |
| AC-DES-51 | When a result, Inconclusive, Interrupted or Error state appears, TalkBack focus moves to its header and it is announced (DES-07 A11Y-09); section titles are headings; each tile is one merged phrase with units spelled out (A11Y-07, A11Y-34), and its info button is separately focusable and labelled "How we measured {metric}". | Semantics test + manual TalkBack |
| AC-DES-52 | For each of TOO_SHORT, TOO_QUIET, NO_SPEECH, NO_TRANSCRIPT and TOO_FEW_WORDS, the Inconclusive card replaces strength, focus and metrics, shows the title "We couldn't measure this attempt", the reason, advice and "This attempt doesn't count against you.", and offers "Try again" (filled), "Tips for a clear recording" and, in Today, "Skip this step"; it shows no metric value, no error colour and no "fail"; the attempt adds no completion, streak day or practice time (SRS-BR-008; HRC-26). | Parameterised Robolectric + unit test |
| AC-DES-53 | A phone call, audio-focus loss, leaving the app, permission revocation or storage-full during recording stops capture, creates no attempt record, discards partial audio and shows the Interrupted state with the matching cause text and action (SRS-FR-SESS-010). | Instrumented test per cause |
| AC-DES-54 | An evaluation failure or timeout shows "We couldn't check this recording" with "Check again", which re-runs evaluation on the retained audio; a database save failure never displays a result as saved and offers a retry (SRS-FR-EVAL-013, SRS-ERR-005); no message contains exception text or codes. | Robolectric with failing fakes |
| AC-DES-55 | Without microphone permission the Record button is never enabled: the first request goes through SCR-PERM-01, a denial shows "Allow microphone", a permanent denial shows "Open settings", and permission is re-checked on resume; when another app holds the microphone the Record button is disabled with the busy message and "Try again" (SRS-ERR-001). | Instrumented permission test |
| AC-DES-56 | Close or Back during Recording or Processing shows "Leave this attempt?" with "Keep recording" and "Leave"; in other states the player closes without a dialog; leaving or backgrounding stops capture; only one recording can be active app-wide; an in-progress recording survives rotation (SRS-FR-SESS-011, -013, -014). | Instrumented test |
| AC-DES-57 | Every tile info button and "How we measured this" opens SCR-SESS-02 at the right metric or overview; content follows what / how / why / limits from the metric catalogue, Estimated metrics state their limits, structure lists elements with cue phrases, and at most 2 "Possible issue" items appear; the sheet has a Close button, contains focus and returns it to the invoking control (DES-07 A11Y-10), and scrolls fully at font scale 2.0 (A11Y-13). | Robolectric + manual TalkBack |

---

### 3.4 Practice — SCR-PRAC-01, SCR-PRAC-02, SCR-PRAC-03 and SCR-PRAC-04 Technique detail

> Technique names, tips and texts in these wireframes are **illustrative placeholders** written for this document. Real content is written in our own words from the @askvinh channel's techniques and ships only with verified provenance (PRD-FR-056/057; SRS-BR-028).

#### 3.4.1 SCR-PRAC-01 Practice home, SCR-PRAC-02 Track, SCR-PRAC-03 Activity detail

##### (a) Current problem

| Problem | Evidence |
|---|---|
| Content is split across 15 curriculum modules, 10 activity modules, 10 "paths", 7 programs and 9 labs; 3 different "path/track" concepts | UXF-13, UXF-25; ui_audit §7.3 |
| The activity library hides 5 sub-products behind one route ("All 150 / Daily Plan / 10 Paths / Saved & Recent / Analytics"); "Swap" is a visible no-op | UXF-22, UXF-39; ADS:74-78, 112-114 |
| Track cards silently filter a grid far below and restrict search; links open different lessons from the ones they name; "Next Lesson" crosses modules | UXF-08, UXF-10; D:313, 576; W:209-212 |
| Hard-coded counts ("150 drills • 10 modules • 10 paths") | UXF-61; D:296 |
| The creator's techniques reach users only through a creator-named primary tab that runs a YouTube caption-scraping tool: "techniques" are fabricated from the first five caption sentences, given invented timestamps and labelled "verified"; lessons link to the channel home page; framework names appear without explanation; there is no way to practise a technique | UXF-07, UXF-55, UXF-59; FND-UI-03; ui_audit §1.8 |
| 6 of 7 programs can never be completed; no unenrol | UXF-25; DP:278-368 |

##### (b) Proposed experience

Practice answers "I want to rehearse a specific situation" (DES-02 §2.1). SCR-PRAC-01 offers **two collections side by side**, switched with a `SingleChoiceSegmentedButtonRow` (DES-05 §11.9):

1. **Tracks** — the five situations (Everyday conversation, Workplace communication, Job interviews, Presentations & storytelling, Clear speech). Tracks replace modules, paths and programs (DES-02 §5). Under the tracks sits **Saved** (PRD-FR-054).
2. **Techniques** — the curriculum's core methods (PRD-FR-056; SRS-FR-PRAC-008). Each technique opens **SCR-PRAC-04** (§3.4.2), whose linked exercises open SCR-PRAC-03 and then the session player. Navigation: Practice → Technique → Activity → Session player (DES-02 v0.2 §3), depth 3.

Activities can belong to a track and to one or more techniques; SCR-PRAC-03 shows both.

##### (c) Wireframes (compact)

```
SCR-PRAC-01 Practice home (Tracks)        SCR-PRAC-01 Practice home (Techniques)
+------------------------------------------+   +------------------------------------------+
| Practice                           (AV) |   | Practice                           (AV) |
+------------------------------------------+   +------------------------------------------+
| (i) Everything you practise here    (x)  |   |  [  Tracks   |(v) Techniques ]           |
|     counts towards your streak and       |   |                                          |
|     progress.               ( Got it )   |   |  Short methods to speak more clearly,    |
|                                          |   |  each with exercises.                    |
|  [(v) Tracks   |  Techniques   ]         |   |  [All] [Workplace] [Interviews]          |
|                                          |   |  [Everyday] [Presentations] [Clear]      |
| +--------------------------------------+ |   | +--------------------------------------+ |
| | Workplace communication    Your goal | |   | | {Technique name}                   > | |
| | Meetings, updates, explaining work   | |   | | {One-line idea}                      | |
| | 18 activities · 5 practised        > | |   | | 3 exercises · 1 practised            | |
| +--------------------------------------+ |   | +--------------------------------------+ |
| | Job interviews                     > | |   | | {Technique name}                   > | |
| | Everyday conversation              > | |   | | {One-line idea}                      | |
| | Presentations & storytelling       > | |   | | 2 exercises                          | |
| | Clear speech                       > | |   | +--------------------------------------+ |
| +--------------------------------------+ |   |  ...                                     |
|                                          |   +------------------------------------------+
| Saved                                    |   | Today  [Practice]  Words   Progress      |
| +--------------------------------------+ |   +------------------------------------------+
| | Saved activities · 3               > | |
| +--------------------------------------+ |
+------------------------------------------+
| Today  [Practice]  Words   Progress      |
+------------------------------------------+

SCR-PRAC-02 Track                          SCR-PRAC-03 Activity detail
+------------------------------------------+   +------------------------------------------+
| <-  Workplace communication              |   | <-  Give a short project update     (*)  |
+------------------------------------------+   +------------------------------------------+
|  Meetings, updates and explaining your   |   |  Workplace communication · Intermediate  |
|  work.                                   |   |                                          |
|  [Read aloud] [Listen & repeat]          |   |  Goal                                    |
|  [Answer a question] [Structured answer] |   |  Give a clear one-minute update your     |
|  [Tell a story]        ( Level: Any )    |   |  team can follow.                        |
|  18 activities                           |   |                                          |
|  +------------------------------------+  |   |  The prompt                              |
|  | Give a short project update   (*)  |  |   |  +------------------------------------+  |
|  | Answer a question · 3 min ·        |  |   |  | "Give a short update on a project: |  |
|  | Intermediate · Practised           |  |   |  | what's done, what's next, and any  |  |
|  +------------------------------------+  |   |  | blockers."                         |  |
|  | Explain a technical problem   ( )  |  |   |  +------------------------------------+  |
|  | simply                             |  |   |  How it works                            |
|  | Structured answer · 4 min · Upper- |  |   |  Answer a question. You'll have 15       |
|  | intermediate                       |  |   |  seconds to plan, then up to 1:30 to     |
|  +------------------------------------+  |   |  speak.                                  |
|  ...                                     |   |  What we'll measure                      |
+------------------------------------------+   |  Pace {Measured} · Filler words          |
 (*) / ( ) = Save IconButton (filled = saved)   |  {Estimated} · Length {Measured}         |
                                               |  After each try you'll also see mistakes |
                                               |  found and coach tips.                   |
                                               |  Your last attempt · 12 Nov              |
                                               |  Pace 142 · Filler words 5.0 per min     |
                                               |  ( See all attempts )                    |
                                               |  Technique                               |
                                               |  {Technique name}                      > |
                                               |==========================================|
                                               |  [[              Start             ]]    |
                                               +------------------------------------------+
```

**Medium:** track and technique lists as 2-column grids (max 840 dp); detail centred 640 dp. **Expanded:** list-detail — the SCR-PRAC-01/02 list at start (≥ 360 dp), SCR-PRAC-03 or SCR-PRAC-04 in the detail pane with a 24 dp spacer.

##### (d) Interactions

| Control | Behaviour |
|---|---|
| Tracks / Techniques segments | Switch the collection in place (instant under reduced motion); the selection is remembered for the session (saved state). |
| Track card | Opens SCR-PRAC-02 for that track only (one labelled destination, UXF-10). |
| Technique card | Opens SCR-PRAC-04 (§3.4.2). |
| Track chips (Techniques) | `FilterChip`s filter techniques by track tag; "All" clears. |
| Saved activities | Opens SCR-PRAC-02 in Saved mode (all tracks; DES-02 §3). |
| Template chips / Level | `FilterChip`s, multi-select, wrap onto new lines (`FlowRow`); "Level: Any" `AssistChip` opens a sheet with Any + the five levels; filters combine and the count text updates ("6 activities"). |
| Activity card | Whole card opens SCR-PRAC-03. |
| Save `IconButton` | Toggles saved ("Save activity" / "Remove from saved"); snackbar with Undo; works offline. |
| Start | Opens SCR-SESS-01 in Practice context. Library attempts count toward progress and streak like Today attempts (SRS-FR-PRAC-004), and each result shows mistakes found and coach tips (PRD-FR-047). |
| Technique row (on SCR-PRAC-03) | Opens SCR-PRAC-04. |
| See all attempts | Opens SCR-PROG-03 filtered to this activity. |

##### (e) Information hierarchy

- **SCR-PRAC-01:** 1. collection switch, 2. Tracks (goal track first) or Techniques, 3. Saved (Tracks view).
- **SCR-PRAC-03:** 1. title and context, 2. goal, 3. the prompt, 4. how it works (template, preparation and maximum time from configuration), 5. what we'll measure, 6. last attempt, 7. technique link, 8. Start.

Browse screens (SCR-PRAC-01, SCR-PRAC-02) have **no filled button**: the cards are the actions (§5 issue 11).

##### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | Push navigation; detail screens have the sticky primary action. |
| Medium | 2-column card grids; detail centred. |
| Expanded | List-detail; selecting a card updates the detail pane without pushing. |
| Height-compact | Rail; detail scrolls; sticky action remains. |

##### (g) Loading, error, offline states

| State | Presentation |
|---|---|
| Loading | Content pack is local; skeleton only if the first read exceeds 300 ms (card-shaped blocks). |
| Empty | Saved mode with nothing saved: "No saved activities yet" / "Tap the save icon on any activity to find it here." + `( Browse tracks )`. Filters with no match: "No activities match these filters" + `( Clear filters )`. |
| Error | Content cannot be read: inline Error "Couldn't load activities" + `[ Try again ]`. |
| Offline | Identical. |

##### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-58 | SCR-PRAC-01 offers "Tracks" and "Techniques" in a segmented control with radio semantics; Tracks lists the five tracks with canonical names, the user's goal track first with the text "Your goal", counts computed from data (no hard-coded numbers, UXF-61) and a Saved row; Techniques lists every shippable technique with its one-line idea and computed exercise counts, filterable by track; every card opens exactly the destination its label names (UXF-10). | Robolectric with content fixture |
| AC-DES-59 | SCR-PRAC-02 shows only the chosen track's activities; template and level filters combine and the count text updates; selected chips are announced as selected; no matches shows the empty state with "Clear filters"; Saved mode lists saved activities from all tracks and has its own empty state. | Robolectric UI test |
| AC-DES-60 | Each activity card shows title, template, minutes, level and "Practised" status in text; the whole card is one target read as one phrase (DES-07 A11Y-07); Save is a separate 48 dp `IconButton` labelled "Save activity" or "Remove from saved" with its state announced; saving works offline. | Semantics test + Robolectric |
| AC-DES-61 | SCR-PRAC-03 shows goal, prompt, a template explanation whose preparation and maximum times come from configuration, the ARC-05 §7 headline metrics with badges under "What we'll measure", a last-attempt summary only when a valid attempt exists, a link to each linked technique (SCR-PRAC-04), and one filled "Start" that opens SCR-SESS-01 in Practice context; the attempt counts toward progress and streak exactly like a Today attempt (SRS-FR-PRAC-004). | Robolectric + repository test |
| AC-DES-62 | On expanded width Practice shows list and detail side by side; on medium, 2-column grids; at font scale 2.0 card titles wrap (full text always available on the detail screen) and filter chips wrap with no horizontal scrolling of essential controls; with TalkBack, section titles are headings and the segmented control announces "Tracks, selected, 1 of 2" (DES-07 A11Y-13). | Robolectric size/fontScale matrix + manual TalkBack |

#### 3.4.2 SCR-PRAC-04 Technique detail (PRD-FR-056/057; SRS-FR-PRAC-008/009; DES-02 v0.2)

##### (a) Current problem

The channel's techniques are the intended curriculum, but today they appear only as heuristic caption extracts in "YouTube Intelligence": the first five caption sentences with invented `idx*45s` timestamps, labelled "verified", grouped under category chips that miss four categories, opened externally with no way to practise (FND-UI-03; UXF-07, UXF-59; ui_audit §1.8; LIO:847-907, YT:321). Lessons marked "VERIFIED" link to the channel home page (UXF-59; CAT:205-447). Framework names appear without explanation (UXF-55).

##### (b) Proposed experience

A calm, readable page that teaches one technique in our own words and gets the user speaking: **name, one-line idea, why it works, steps, linked exercises, tips**, a single **"Practise it"** action, and **"Watch the original lesson"** for users who want the source. Hoang reads three short steps and taps Practise it; Sofia opens the original lesson to go deeper. The page never names or shows the creator (D-08 decided; SRS-BR-028) and never claims to measure the technique itself — exercises are measured with the ordinary ARC-05 metrics, and aspects audio cannot show become a self-check on the result (ARC-05 §8.4).

**Content fields** (owner ARC-03 §6; provenance SRS-FR-PRAC-009):

| Field | Shown as | Rule |
|---|---|---|
| Name | Title (`headlineMedium`) | Our wording; says what to do; the "name as taught" is stored as provenance only and not shown |
| One-line idea | Subtitle (`bodyLarge`) | ≤ 15 words, B1 English |
| Why it works | Section | 1–3 sentences, no unverifiable claims (EP-5) |
| Steps | Numbered list | 3–5 steps, verb first |
| Exercises | List of linked activities | Each on an R1 template, with template, minutes and "Practised" status; each opens SCR-PRAC-03 |
| Tips | Bulleted list | 2–4 short tips (the same tips feed result coach tips, ARC-05 §8.4) |
| Source video | "Watch the original lesson" | The **specific** video and timestamp, verified at content review (never the channel root); items without verified provenance do not ship (SRS-BR-028) |

##### (c) Wireframe (compact)

```
+------------------------------------------+
| <-  Technique                            |
+------------------------------------------+
|  {Technique name}                        |  headlineMedium (heading)
|  {One-line idea, e.g. "Silence before a  |  bodyLarge
|  key point makes people listen."}        |
|                                          |
|  Why it works                            |  titleMedium (heading)
|  {1–3 sentences in our own words.}       |  bodyLarge
|                                          |
|  How to do it                            |  titleMedium (heading)
|  1. {Step one, verb first.}              |  numbered list, bodyLarge
|  2. {Step two.}                          |
|  3. {Step three.}                        |
|                                          |
|  Exercises                               |  titleMedium (heading)
|  +------------------------------------+  |  Card with ListItem rows
|  | 1 {Exercise title}               > |  |
|  |   Read aloud · 2 min · Practised   |  |
|  |  - - - - - - - - - - - - - - - - - |  |
|  | 2 {Exercise title}               > |  |
|  |   Answer a question · 3 min        |  |
|  +------------------------------------+  |
|                                          |
|  Tips                                    |  titleMedium (heading)
|  • {Tip one.}                            |  bodyLarge bullets
|  • {Tip two.}                            |
|                                          |
|  [ Watch the original lesson ]           |  OutlinedButton + external-link icon
|  Opens YouTube, outside SpeakCraft       |  bodyMedium, onSurfaceVariant
|==========================================|
|  [[            Practise it           ]]  |  the only filled button
+------------------------------------------+
```

**Medium:** centred column, max 640 dp. **Expanded:** shown in the detail pane next to the Techniques list; choosing an exercise pushes SCR-PRAC-03 inside the detail pane.

##### (d) Interactions

| Control | Behaviour |
|---|---|
| Practise it | Opens **SCR-PRAC-03** for the technique's next unpractised exercise (the first if all are practised), whose Start opens SCR-SESS-01 — the DES-02 v0.2 path Practice → Technique → Activity → Session player. |
| Exercise row | Opens that exercise's SCR-PRAC-03. |
| Watch the original lesson | Opens the specific source video at its timestamp in the YouTube app or the browser through an outbound intent; no API key, no scraping, no embedded player or download (SRS-FR-PRAC-009; ADR-011). Offline: snackbar "You're offline. The original lesson needs a connection." |
| Back | Returns to where the technique was opened (Practice, an activity, or a result's coach tip). |

##### (e) Information hierarchy

1. Name and one-line idea → 2. why it works → 3. how to do it → 4. exercises → 5. tips → 6. original lesson link → 7. **Practise it** (sticky).

##### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | Single column; sticky Practise it. |
| Medium | Centred 640 dp column. |
| Expanded | Detail pane of the Techniques list-detail. |
| Height-compact | Content scrolls; sticky action remains. |

##### (g) Loading, error, offline states

| State | Presentation |
|---|---|
| Loading | Local content; skeleton (title line, three text blocks, list card) only after 300 ms. |
| Empty | Not applicable: a technique without exercises or verified provenance fails content validation and never ships (SRS-BR-028; SRS-ERR-008). |
| Error | "Couldn't load this technique" + `[ Try again ]`. A technique referenced from history but removed from the current content pack: "This technique is no longer available." + `( Go to Practice )`. |
| Offline | Identical, except the original-lesson link (snackbar on tap). |

##### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-63 | SCR-PRAC-04 shows, in order, the technique name, one-line idea, "Why it works", numbered steps, the linked exercises (each with template, minutes and practised status, each opening SCR-PRAC-03), tips, "Watch the original lesson" and one filled "Practise it", which opens SCR-PRAC-03 of the next unpractised exercise; exercise results are the standard result with mistakes found and coach tips (§3.3), and the page claims no measurement of the technique beyond its exercises' ARC-05 metrics (SRS-FR-PRAC-008). | Robolectric with technique fixture + honesty review (TST-01 §9) |
| AC-DES-64 | "Watch the original lesson" opens the item's specific video URL at its timestamp through an outbound intent (YouTube app or browser), states "Opens YouTube, outside SpeakCraft", and offline shows the snackbar instead; no screen embeds a YouTube player, scrapes captions or shows a "verified" label; no UI string, content-pack text or notification contains the creator's name, handle or likeness, and every shipped technique, exercise and tip has `licenceStatus = ORIGINAL_REEXPRESSION` and specific-video provenance (D-08 decided; SRS-BR-028, SRS-FR-PRAC-009; ADR-011). | Intent test + content-pack build validation + G-7 grep |
| AC-DES-65 | With TalkBack, the name and each section title are headings, the steps are announced as a numbered list ("1 of 3"), each exercise row is one phrase, and the link is announced "Watch the original lesson, opens YouTube outside SpeakCraft" (DES-07 A11Y-02, A11Y-07); at font scale 2.0 all text wraps without truncation and Practise it remains reachable without hiding focused content (A11Y-13, A11Y-30); on expanded width the page appears in the detail pane beside the Techniques list. | Manual TalkBack + Robolectric fontScale/size matrix |

---

### 3.5 Words — SCR-WORD-01, SCR-WORD-02, SCR-WORD-03, SCR-WORD-04

#### (a) Current problem

| Problem | Evidence |
|---|---|
| "Vocabulary Builder" with tabs "Words / Flashcards / AI Quiz / Notebook" over a 20-word bank; the quiz engine is referenced nowhere; notebook and spaced-repetition data are never read by the UI | ui_audit §3.3 (VO:109; CAT:544+), §2 |
| Called "Tools" in the sidebar; "AI Quiz" for a deterministic feature | UXF-15, UXF-54 |
| "Card x of N due today" counts non-due cards; "Mastered Next" button | UXF-18; VO:224, 236 |
| Flashcard is a 270 dp fixed-height box that clips at large font | UXF-42; VO:241 |
| "Words Learned" counts words merely seen; two vocabulary stores disagree | UXF-13, UXF-23; FND-DOM-08 |

#### (b) Proposed experience

Words answers "Which words am I learning and what's due?" (DES-02 §2.1). Each word moves through five stages — **Learn · Say it · Use it · Make your own · Review** (PRD-FR-060; DES-05 §12.2) — and every stage after Learn is spoken, so vocabulary turns into speech (P-3). Words home puts what is due first; word detail shows where a word is; speaking stages and reviews run in the same session player. Status counts never inflate: a word is "Learned" only after Make your own is passed (SRS-BR-017).

#### (c) Wireframes (compact)

```
SCR-WORD-01 Words home                    SCR-WORD-02 Word detail
+------------------------------------------+   +------------------------------------------+
| Words                              (AV) |   | <-  delegate                             |
+------------------------------------------+   +------------------------------------------+
| (i) Each word has five stages.      (x)  |   |  +------------------------------------+  |  WordCard (no fixed height)
|     Saying it aloud moves it forward.    |   |  | delegate                           |  |  headlineSmall
|                                          |   |  | /ˈdel.ɪ.ɡeɪt/ · verb               |  |  IPA only if in content
| +--------------------------------------+ |   |  | To give part of your work to       |  |  bodyLarge
| | Today                                | |   |  | someone else.                      |  |
| | 6 reviews due · 2 new words          | |   |  | "I delegated the report to Priya." |  |  bodyMedium
| | [[         Start review         ]]   | |   |  | Often used with: delegate tasks,   |  |
| +--------------------------------------+ |   |  | delegate to someone                |  |
|                                          |   |  | [ Listen ]  [ Listen slowly ]      |  |
| Your words                               |   |  +------------------------------------+  |
| Learning 12 · Learned 30 · Due 6         |   |  Your progress                           |
| +--------------------------------------+ |   |  (v) Learn          Passed 12 Nov        |  StageStepper (vertical
| | (search) Search words                | |   |  (v) Say it         Passed 12 Nov        |  on compact)
| +--------------------------------------+ |   |  (o) Use it         Current              |
| [New] [Learning] [Learned] [Due]         |   |  ( ) Make your own  Not started          |
| ( Level and track )                      |   |  ( ) Review         After Make your own  |
| +--------------------------------------+ |   |  Stage 3 of 5 · Use it                   |
| | delegate                   Use it  > | |   |==========================================|
| | verb · give work to someone else     | |   |  [[        Continue: Use it        ]]    |
| +--------------------------------------+ |   +------------------------------------------+
| | concise                   Learned  > | |    Learned word: no filled button; Review row
| | adjective · short and clear          | |    shows "Next review 18 Nov".
| +--------------------------------------+ |
+------------------------------------------+
| Today   Practice  [Words]  Progress      |
+------------------------------------------+

SCR-WORD-03 Word stage flow: Learn         SCR-WORD-03 Say it intro
+------------------------------------------+   +------------------------------------------+
| (x)  delegate · Learn     Stage 1 of 5   |   | (x)  delegate · Say it    Stage 2 of 5   |
+------------------------------------------+   +------------------------------------------+
|  (word card as on SCR-WORD-02)           |   |  Say the word clearly.                   |
|                                          |   |  We check that speech recognition        |
|  Read the meaning and examples. Listen   |   |  understood you, not your accent.        |
|  if it helps.                            |   |  [ Listen ]  [ Listen slowly ]           |
|                                          |   |                                          |
|==========================================|   |==========================================|
|  [[         I've read this         ]]    |   |  [[              Start             ]]    |
+------------------------------------------+   +------------------------------------------+
                                                Start -> SCR-SESS-01 word-stage mode (5 s max)

SCR-SESS-01 word-stage result (Make your own, Not yet) — SRS-FR-WORD-014; ARC-05 §8.4
+------------------------------------------+
| (x)  delegate · Make your own            |
+------------------------------------------+
|  Not yet. Try again.                     |  headlineMedium, neutral (pass / Not yet /
|  Score 64 · pass mark 70  {Estimated}    |  Inconclusive stated first, SRS-FR-WORD-014)
|                                          |
|  What we heard                           |
|  "my manager he don't delegate tasks"    |
|                                          |
|  Mistakes found (2)                      |  Mistake list (proposed pattern)
|  +------------------------------------+  |
|  | Sentence too short                 |  |  ISSUE_TOO_SHORT
|  | 4 words · needs at least 5         |  |
|  | {Measured}                         |  |
|  |  - - - - - - - - - - - - - - - - - |  |
|  | Possible grammar issue             |  |  ISSUE_GRAMMAR_POSSIBLE
|  | "he don't" -> try "he doesn't"     |  |
|  | {Estimated}                        |  |
|  +------------------------------------+  |
|                                          |
|  Coach tip                               |  one tip for word stages (SRS-FR-WORD-014)
|  +------------------------------------+  |
|  | (flag) Try this next               |  |
|  | Add who or what: "My manager       |  |
|  | delegates tasks to the team."      |  |
|  | ( Technique: {technique name} )    |  |  where the word's content links one
|  +------------------------------------+  |
|                                          |
|  [[             Try again          ]]    |
|  ( Continue later )                      |
|                                          |
|  Was this accurate?                      |
|  [ (thumb-up) Yes ]  [ (thumb-down) No ] |
+------------------------------------------+
 Say it with a different word recognised: Mistakes found lists ISSUE_WRONG_FORM
 "Different word recognised — We heard 'delicate' for 'delegate'." {Measured}.
 Passed: "Passed" (success colour + check + text), Mistakes found shows any issues
 or the honest empty state, then [[ Continue ]] ( Practise again ).

SCR-WORD-04 Review session (summary)
+------------------------------------------+
| (x)  Review                              |
+------------------------------------------+
|  6 words reviewed                        |  headlineMedium
|  5 recalled first time · 1 after a hint  |
|                                          |
|  Next reviews                            |
|  Tomorrow: 2 words                       |
|  In 3 days: 4 words                      |
|==========================================|
|  [[               Done              ]]   |
+------------------------------------------+
 Items run in SCR-SESS-01 review context ("Word 2 of 6"):
 "Say the word that means: to give part of your work to someone else."
 ( Show a hint ) before recording -> first letter and part of speech.
 Each item's result (compact): "Recalled" / "Not recalled" / Inconclusive,
 What we heard, Mistakes found (e.g. ISSUE_WRONG_FORM, ISSUE_TARGET_WORD_MISSING),
 one coach tip, then [[ Next word ]] or [[ Try again ]] ( Next word ) (SRS-FR-WORD-014).
```

**Medium:** Words home list as a 2-column grid; word detail stepper becomes horizontal (five labelled steps). **Expanded:** list-detail (word list at start, word detail at end).

#### (d) Interactions

| Control | Behaviour |
|---|---|
| Today card primary | "Start review" when reviews are due → SCR-WORD-04 (≤ 10 words, SRS-FR-WORD-009). Otherwise "Learn next word" when the daily new-word goal is not met (default 2, SRS-BR-027) → SCR-WORD-03 at Learn for the next word. Otherwise no filled button and the text "You're done with words for today." |
| Search field | `OutlinedTextField` with visible label "Search words"; filters as you type; `imePadding()`. |
| Status chips | `FilterChip`s New, Learning, Learned, Due (combine with search). "Level and track" opens a filter sheet. |
| Word row | Opens SCR-WORD-02. |
| Listen / Listen slowly | Text-to-speech of the word; hidden if TTS is unavailable (SRS-ERR-010). |
| Continue: {stage} | Opens SCR-WORD-03 at the current stage. Passed stages can be replayed from the stepper rows ("Practise again"), and replaying never lowers status (SRS-BR-015). |
| I've read this | Completes Learn (SRS-FR-WORD-002) and shows the Say it intro. |
| Start (speaking stages) | Opens SCR-SESS-01 in word-stage mode: Say it (≤ 5 s), Use it (two sentences, "Sentence 1 of 2"), Make your own (one original sentence). |
| Easier version | After 3 "Not yet" results on a stage: "Want an easier version? We'll use a shorter sentence and slower audio." `[[ Try the easier version ]]` `( Continue later )` (SRS-FR-WORD-007). |
| Show a hint (review) | Shows first letter and part of speech; a recall after a hint is graded 3 (SRS-BR-016). |
| Next word / Done | Moves through review items; Done returns to Words home. |

#### (e) Information hierarchy

- **SCR-WORD-01:** 1. what is due today and the one action, 2. status summary, 3. search and filters, 4. the list.
- **SCR-WORD-02:** 1. the word and its meaning, 2. examples and listening, 3. stage progress, 4. next stage action.
- **Stage results (SRS-FR-WORD-014):** 1. Passed / Not yet / Inconclusive, 2. score against the pass mark with evidence badge, 3. what we heard, 4. Mistakes found (ARC-05 §8.4 codes, e.g. ISSUE_WRONG_FORM, ISSUE_TARGET_WORD_MISSING, ISSUE_TOO_SHORT, ISSUE_GRAMMAR_POSSIBLE) or the honest empty state, 5. one coach tip, 6. action ("Try again" unless passed), 7. Was this accurate?

Score labels for word stages (RECOMMENDATION, ARC-05 §9 to confirm): Say it and Use it scores derive from recognition and are `{Measured}`; Make your own includes a grammar-rule component and is `{Estimated}`. Scores are allowed here because word stages are threshold checks (PRD-FR-043). "Passed" uses `success` colour with a check icon and text; "Not yet. Try again." uses neutral text.

#### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | Push navigation; vertical stage stepper. |
| Medium | 2-column word grid; horizontal stepper with labels below each step. |
| Expanded | List-detail. |
| Height-compact | Word card and stepper scroll; sticky action remains. |

#### (g) Loading, error, offline states

| State | Presentation |
|---|---|
| Loading | Skeleton rows after 300 ms. |
| Empty | No words match a filter or search: empty template with "Clear filters" / "Clear search". New user: the list shows the starter words for their level as "New" (never "Learned"). |
| Error | Inline Error "Couldn't load your words" + `[ Try again ]`. |
| Offline | Identical. |
| T3b device | Speaking stages show "Transcript needed for this check" (SRS-FR-SPCH-007) and offer `( Learn more )`; Learn still works. |

#### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-66 | SCR-WORD-01 shows "Start review" when at least one review is due, otherwise "Learn next word" when the daily goal is not met, otherwise no filled button and "You're done with words for today."; the due count includes only words whose due date is today or earlier (fixes UXF-18). | Robolectric with schedule fixtures |
| AC-DES-67 | Status chips read New, Learning, Learned and Due; counts are computed from word progress; no "Mastered" label and no "words learned" count of words that have not passed Make your own appear (DES-05 §12.3; §5 issue 8); stage names are exactly Learn, Say it, Use it, Make your own, Review. | Robolectric + string search |
| AC-DES-68 | Search filters the list as the user types, combines with status chips, keeps the field visible above the keyboard, works offline, and shows the empty state with "Clear search" when nothing matches. | Robolectric UI test |
| AC-DES-69 | SCR-WORD-02 shows the word card with no fixed-height container (all text visible at font scale 2.0, fixing VO:241), IPA only when the content provides it, "Listen" and "Listen slowly", and no pronunciation score; the stage stepper shows all five stages with an icon state and text, and TalkBack reads "Stage 3 of 5, Use it, current" (DES-07 A11Y-13, A11Y-23). | Robolectric fontScale 2.0 + semantics test |
| AC-DES-70 | For New and Learning words the filled button is "Continue: {next stage}"; Learned words that are not due have no filled button and show the next review date; replaying a passed stage never lowers the word's status (SRS-BR-015). | Robolectric + unit test |
| AC-DES-71 | Learn completes only on "I've read this"; Say it, Use it and Make your own run in SCR-SESS-01 word-stage mode; each result shows "Passed" (success colour, check icon and text) or "Not yet. Try again." (neutral), the score with its pass mark (defaults 75 / 75 / 70, read from configuration) and an evidence badge; after the status, score and what we heard, every stage result lists the mistakes detected with evidence and badge (for example "We heard 'delicate' for 'delegate'", "4 words · needs at least 5") or "Our checks didn't find issues in this attempt.", followed by exactly one coach tip; the filled button is "Try again" unless the stage passed ("Continue"); an Inconclusive stage attempt shows its reason and is never shown as "Not yet"; Say it shows the recognition caveat (ARC-05 §9; SRS-FR-WORD-014). | Parameterised Robolectric test |
| AC-DES-72 | After three "Not yet" results on the same stage, the easier-version offer appears with "Try the easier version" and "Continue later" (SRS-FR-WORD-007). | Robolectric UI test |
| AC-DES-73 | SCR-WORD-04 reviews at most 10 due words, shows "Word n of N", requires a spoken answer for each item, offers "Show a hint" before recording, shows after each item "Recalled", "Not recalled" or the Inconclusive reason with mistakes found and one coach tip (SRS-FR-WORD-014), and ends with a summary of words reviewed, first-time versus after-hint recalls and next review dates in the device locale; Inconclusive items can be retried and are not graded (SRS-BR-016). | Robolectric + scheduler unit test |
| AC-DES-74 | With TalkBack each word row is read as one phrase ("delegate, verb, give work to someone else, Use it stage") (DES-07 A11Y-07); the search field has a spoken label; chips announce their selected state; in a review item the prompt is read before the Record button; at font scale 2.0 the stepper rows and word card wrap without clipping (A11Y-13). | Manual TalkBack + Robolectric fontScale 2.0 |

---

### 3.6 Progress — SCR-PROG-01, SCR-PROG-02, SCR-PROG-03, SCR-PROG-04

#### (a) Current problem

| Problem | Evidence |
|---|---|
| Progress counts activity, not ability: "Lessons Completed x / 12", "Practice Hours" from 3 minutes per ticked step, "Words Learned" for words merely seen | UXF-23; FND-DOM-08; D:512-518, 808-858 |
| Fallback numbers shown as data: analytics skills 62/65/60/68/58/66; a single-point "trend"; a sparkline that invents 60 → 62 and tells TalkBack "latest 62 out of 100" | UXF-56; FND-UI-07; AS:103-108; CC:414-417 |
| Self-ratings from onboarding shown as "Objective Speech Skills" | UXF-58; AS:101-110, 280-292 |
| Four streak definitions; the streak differs between dashboard and profile; new users see "1 Day Streak" | UXF-13, UXF-26, UXF-57; W:703 vs P:153-156; REPO:494 |
| History mixes all labs with no label; deltas compare unrelated modules | UXF-26; SAC:538-610 |
| Loading and errors render as "No sessions in {period} window" | UXF-49; NAV:513-521; AS:258-265 |

#### (b) Proposed experience

Progress answers "Am I improving?" (DES-02 §2.1) **from evidence only** (PP-7; UX-4). It shows one streak, measured practice time, trends of measured and estimated metrics once there is enough data to mean something, word progress, and a full history where every attempt can be replayed, re-read and retried. Sofia sees her filler-word rate over four weeks with the noise band and a plain "No clear change" when the movement is within it. A new user sees an honest empty state, not zeros dressed as achievements.

#### (c) Wireframes (compact)

```
SCR-PROG-01 Progress overview             SCR-PROG-02 Metric detail
+------------------------------------------+   +------------------------------------------+
| Progress                           (AV) |   | <-  Pace                                 |
+------------------------------------------+   +------------------------------------------+
|  [(v) Week |  Month  |  All  ]           |   |  Answers and stories · {Measured}        |
|                                          |   |  [(v) Week |  Month  |  All  ]           |
|  (event) 4-day streak    Best: 9 days    |   |  Latest: 128 words/min                   |
|   M    T    W    T    F    S    S        |   |  Target 120–160 · Within target          |
|  [v]  [v]  [v]  (o)   .    .    .        |   |  +------------------------------------+  |
|  ( Show month )                          |   |  |170|                                |  |
|                                          |   |  |160|=========== target band ========|  |
|  Practice time                           |   |  |140|  o     o                       |  |
|  18 min speaking this week               |   |  |120|============== o ===== o =======|  |
|  +------------------------------------+  |   |  |   | 12   14    16    18 Nov       |  |
|  |  |   |   |   .   .   .   .          |  |   |  +------------------------------------+  |
|  |  M   T   W   T   F   S   S          |  |   |  Down 14 words/min over 6 attempts.     |
|  +------------------------------------+  |   |  6 of 6 within target.                   |
|  Sessions completed: 3                   |   |  ( View as list )                        |
|                                          |   |                                          |
|  Trends                                  |   |  About this measure                      |
|  +------------------------------------+  |   |  What it is · How we measure it ·        |
|  | Pace {Measured}                  > |  |   |  Why it matters · Its limits             |
|  | Answers and stories                |  |   |  (same text as SCR-SESS-02)              |
|  | 142 -> 128 words/min, 6 attempts   |  |   +------------------------------------------+
|  | Within target                      |  |    Estimated metrics: dashed line + "Estimated"
|  +------------------------------------+  |    label on the chart (CH-6).
|  | Filler words {Estimated}         > |  |
|  | No clear change over 5 attempts    |  |   SCR-PROG-03 History
|  +------------------------------------+  |   +------------------------------------------+
|  | Recognised as intended           > |  |   | <-  History                              |
|  | Read aloud and repeat              |  |   +------------------------------------------+
|  | Practise 1 more time to see a trend|  |   |  ( All templates )  ( All tracks )       |
|  +------------------------------------+  |   |  Today                                   |  date group (heading)
|                                          |   |  +------------------------------------+  |
|  Words                                   |   |  | Give a short project update      > |  |
|  Learning 12 · Learned 30 · Due today 6 >|   |  | Answer a question · 09:12          |  |
|                                          |   |  | Pace 128 · Filler words 4.1/min ·  |  |
|  [ See all attempts ]                    |   |  | 58 s · Recording kept              |  |
+------------------------------------------+   |  +------------------------------------+  |
| Today  Practice  Words  [Progress]       |   |  | Read it with the pause           > |  |
+------------------------------------------+   |  | Read aloud · 09:05 {Inconclusive}  |  |
                                               |  | Too quiet to measure               |  |
                                               |  +------------------------------------+  |
                                               +------------------------------------------+

SCR-PROG-04 Attempt detail (same section order as the session result, §3.3.5)
+------------------------------------------+
| <-  12 Nov, 09:12                  (:)   |  overflow: "Delete attempt"
+------------------------------------------+
|  Give a short project update             |  titleLarge
|  Answer a question · On-device           |
|  transcription                           |
|  What we heard   [ (>) Play ]  0:58      |  full transcript (not truncated)
|  "So I'm Aarav, I work as …"             |
|  Metrics (all, with badges)              |  all Metric tiles, not only headline
|  What went well · Mistakes found ·       |  as recorded at evaluation time
|  Coach tips                              |
|  [[             Try again          ]]    |  hidden if the activity was retired
|  Was this accurate?  You said: Yes       |
|  ( Change answer )                       |
+------------------------------------------+
```

**New-user empty state (SCR-PROG-01):** `Outlined.Insights` (48 dp), "Your progress will appear here", "Finish your first session to see your pace, pauses and more.", `[[ Start today's session ]]` → SCR-SESS-01 via Today's plan. No zero charts, no "0-day streak" (HRC-32).

**Sign-in prompt (local users; DES-02 §2.2):** after at least 3 valid attempts, a dismissible `OutlinedCard` at the end of SCR-PROG-01: "Keep your progress if you change phones. Sign in to back it up." `[ Sign in ]` `( Not now )`. Not shown again for 30 days after "Not now" (RECOMMENDATION). Never blocks, never on first visit (PP-7; D-05).

**Medium:** overview cards in 2 columns. **Expanded:** 2-column cards plus a history pane (DES-02 §7); SCR-PROG-04 opens in the history pane's detail area.

#### (d) Interactions

| Control | Behaviour |
|---|---|
| Week / Month / All | `SingleChoiceSegmentedButtonRow`; recomputes practice time, sessions and trend windows. |
| Show month | Expands the month grid (DES-05 §11.17) with "Previous month" / "Next month" `IconButton`s. Non-interactive cells. |
| Trend card | Opens SCR-PROG-02 for that metric and template family. |
| Words row | Opens SCR-WORD-01. |
| See all attempts | Opens SCR-PROG-03. |
| History filters | `AssistChip`s opening sheets (templates; tracks). |
| History row | Opens SCR-PROG-04. |
| Chart point (SCR-PROG-02) | Tap shows value and date in a label (CH-9); "View as list" toggles a table of the same data. |
| Play | Local playback when retained. |
| Try again (SCR-PROG-04) | Opens SCR-SESS-01 for the same activity in Practice context; the new result compares with this attempt if it was the last valid one. |
| Change answer | Re-opens "Was this accurate?" (ENT-13 is separate from the immutable attempt, R-1). |
| Delete attempt | Dialog "Delete this attempt?" (§2.8); on confirm, the attempt, its recording and transcript are deleted (and synced metrics for signed-in users); trends and practice time recompute; snackbar "Attempt deleted". |

#### (e) Information hierarchy

- **SCR-PROG-01:** 1. streak, 2. practice time and sessions, 3. trends, 4. words, 5. history entry.
- **SCR-PROG-02:** 1. latest value with badge and target, 2. chart with target band, 3. plain-language change statement, 4. list alternative, 5. explanation.
- **SCR-PROG-04:** the session-result order (§3.3.5), with all metrics instead of three.

**Content rules (all from SRS-FR-PROG-001…007, SRS-BR-010…012, -018, -024):** practice time = sum of valid recording durations, labelled "min speaking"; trend cards appear per template family (Read/Repeat; Answer/Structured/Story) only with ≥ 3 valid attempts, otherwise "Practise {n} more time(s) to see a trend" (computed); changes inside the metric's noise band read "No clear change"; change statements are neutral ("Down 14 words/min"), never "better/worse" in colour; Inconclusive attempts appear in history with their reason but never in trends, streak or practice time. Attempt detail shows mistakes and tips as recorded at evaluation time (`evaluationVersion`, `contentVersion`; §5 issue 23).

#### (f) Responsive behaviour

| Class | Behaviour |
|---|---|
| Compact | Cards stacked; charts full width, height `max(160 dp, axis text + plot)` (CH-8). |
| Medium | Overview in 2 columns; history single list centred. |
| Expanded | Overview + history pane; attempt detail in the detail area. |
| Height-compact | Rail; charts keep minimum height and scroll with content. |

#### (g) Loading, error, offline states

| State | Presentation |
|---|---|
| Loading | Skeleton (streak strip, card blocks) after 300 ms; never "No sessions". |
| Empty | New user: empty state above. Filtered history with no match: "No attempts match these filters" + `( Clear filters )`. Period with no attempts: "No practice in this period" (streak and history remain). |
| Error | Inline Error per card ("Couldn't load your trends" + `[ Try again ]`); other cards still render. |
| Offline | Identical; sign-in prompt's button shows the offline row on tap. |

#### (h) Acceptance criteria

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-75 | A profile with no valid attempts sees the SCR-PROG-01 empty state with "Start today's session" as its only filled button, and no chart, zero value, trend or streak number (HRC-31, HRC-32). | Robolectric with empty fixture |
| AC-DES-76 | Current and longest streak follow SRS-BR-011, and the same values appear on Today, Progress and Profile for the same data; missed days show no fill, no red and no cross; Inconclusive attempts never create a streak day (SRS-BR-008). | Unit test + Robolectric shared fixture |
| AC-DES-77 | Practice time equals the sum of valid recording durations in the selected period and is labelled in minutes of speaking; the bar chart plots only stored days, starts at 0 and leaves gaps for days without practice (DES-05 CH-1, CH-3, CH-4). | Unit test + Robolectric |
| AC-DES-78 | A trend card appears only for a template family with ≥ 3 valid attempts; otherwise it reads "Practise {n} more time(s) to see a trend" with the computed number; a change inside the noise band reads "No clear change"; Estimated metrics are labelled "Estimated" and drawn dashed (SRS-BR-024; DES-05 CH-6). | Parameterised unit + Robolectric |
| AC-DES-79 | SCR-PROG-02 shows the latest value with its badge and target, a chart with only stored points, gaps not joined, a labelled target band, at most 2 series, a "View as list" alternative with the same data, and a plain-language change statement; the chart has a content description summarising the series (DES-07 A11Y-12). | Robolectric + semantics test |
| AC-DES-80 | SCR-PROG-03 lists attempts newest first with locale-formatted date and time, activity, template, either a headline-metric summary or the "Inconclusive" badge with its reason, and playback availability in text; template and track filters work; loading never shows the empty state. | Robolectric with paging fixture |
| AC-DES-81 | SCR-PROG-04 shows the full transcript, every metric with its badge, the strength, mistakes found and coach tips as recorded at evaluation time, and the accuracy answer with "Change answer"; "Play" appears only for retained recordings; "Try again" (filled) is shown only while the activity is in the current content pack, otherwise "This activity is no longer available." (SRS-BR-026). | Robolectric with history fixture |
| AC-DES-82 | "Delete attempt" asks "Delete this attempt?" with the consequence; on confirm the attempt, recording and transcript are removed (and the synced metrics for a signed-in user), trends and practice time recompute, and "Attempt deleted" is announced. | Instrumented test |
| AC-DES-83 | With TalkBack, card titles and date groups are headings, each history row and trend card is one phrase with units spelled out (DES-07 A11Y-07, A11Y-34), and the segmented period control announces its selection; at font scale 2.0 cards stack and chart axis labels scale without clipping (A11Y-13); on expanded width the overview and history pane appear side by side. | Manual TalkBack + Robolectric fontScale/size matrix |

---

### 3.7 Profile, settings and account — SCR-PROF-01, SCR-SET-01, SCR-SET-04, SCR-SET-06, SCR-SET-08, SCR-ACC-02

#### 3.7.1 SCR-PROF-01 Profile

**(a) Current problem.** A gradient header with a "Score {n}/100" pill; a "Communication Skill Profile" of six bars filled from onboarding self-ratings; "Active Streak: 1 days"; links to "App Updates & Backups"; the editor offers 5 levels while onboarding offered 4 and closes without feedback; no member-since date, no export, no delete account (UXF-17, UXF-56, UXF-57, UXF-58; FND-UI-07, FND-UI-14; ui_audit §8.6; P:180-529). The dashboard avatar callback is dead (UXF-09).

**(b) Proposed experience.** A calm page about *you*: your name (optional), how you are learning (goal, level, daily time — editable in place), what you have actually done (measured), and the way into Settings. No scores, no self-ratings, no skill bars.

**(c) Wireframe (compact).**

```
+------------------------------------------+
| <-  Profile                              |
+------------------------------------------+
|  (AA)  Aarav             ( Edit name )   |  initials avatar, heightIn (no fixed size)
|        Member since 3 Nov 2026           |  bodyMedium
|        Not signed in · Saved on this     |  account status -> SCR-SET-07
|        phone only                      > |
|                                          |
|  Your learning                           |  titleSmall (heading)
|  Goal        Workplace communication   > |  ListItem rows -> edit sheets
|  Level       Intermediate              > |
|  Daily time  10 minutes                > |
|                                          |
|  Your practice                           |
|  4-day streak · Best 9 days              |  same values as Progress
|  2 h 10 min speaking · 41 attempts       |
|  30 words learned                        |  Learned = passed Make your own
|                                          |
|  Settings                              > |
+------------------------------------------+
 Without a name: "Add your name" text button; the avatar shows a person icon.
```

**(d) Interactions.** Goal, Level and Daily time rows open a `ModalBottomSheet` with a radio list from the **shared options source** (same as SCR-ONB-02…04 and SCR-SET-02) and "Save"; saving regenerates only today's incomplete steps (SRS-FR-TODAY-007) and never resets progress (SRS-BR-025). "Edit name" opens a sheet with an `OutlinedTextField` labelled "Display name", supporting text "Optional · up to 100 characters", `imePadding()`, "Save". The account row opens SCR-SET-07; Settings opens SCR-SET-01. **(e) Hierarchy:** identity → learning choices → measured practice → Settings. **(f) Responsive:** centred 640 dp column on medium and expanded. **(g) States:** loading skeleton after 300 ms; inline Error "Couldn't load your profile" + `[ Try again ]` (fixes the dead-end text, UXF-51); save failure keeps the sheet open with "Couldn't save. Try again."; offline identical.

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-84 | SCR-PROF-01 opens from the avatar on all four primary destinations (avatar labelled "Profile and settings") and shows display name or "Add your name", member-since date, account status, goal, level, daily time, and streak, speaking time and attempts identical to SCR-PROG-01 for the same data; it shows no score, skill bars, self-ratings or "1 days" (UXF-56…58). | Robolectric with shared fixture |
| AC-DES-85 | Editing goal, level or daily time offers exactly the shared options with the current value selected; saving keeps all history and changes only today's incomplete steps; the name field is optional, limited to 100 characters with an inline, announced error (DES-07 A11Y-32) and stays visible above the keyboard; at font scale 2.0 rows wrap and sheets scroll (A11Y-13). | Robolectric + unit test |

#### 3.7.2 SCR-SET-01 Settings home

**(a) Current problem.** 29 controls on one screen, with engine diagnostics first on phones, a permanently disabled "Cloud AI Coach" switch, dead model-tier chips, a plaintext YouTube API key field, update channels and an auto-downloading "Check for Updates", an admin button, no export, no account deletion and no About (UXF-21; FND-UI-14; ui_audit §8.7). Settings is not on phone chrome (UXF-09).

**(b) Proposed experience.** Seven plain groups, in the order of PRD-FR-091 / SRS-FR-SET-002, each with a one-line summary of its current state. Nothing developer-facing.

```
+------------------------------------------+
| <-  Settings                             |
+------------------------------------------+
|  (school) Learning                     > |
|           Workplace · Intermediate ·     |
|           10 min                         |
|  (bell)   Reminders                    > |
|           Every day at 19:00             |
|  (mic)    Speech & privacy             > |
|           On-device transcription ·      |
|           recordings kept 30 days        |
|  (palette) Appearance                  > |
|           System theme                   |
|  (lock)   Data & privacy               > |
|           Export or clear your data      |
|  (person) Account                      > |
|           Not signed in                  |
|  (info)   About                        > |
|           Version 2.0.0                  |
+------------------------------------------+
```

**Where the current 29 settings go:**

| Current (ui_audit §8.7 row) | New home |
|---|---|
| AI & Intelligence chips and switches, Cloud AI Coach, model tiers (1–7) | **Removed** (no cloud AI, D-04; the local pipeline is always on) |
| Live Transcription switch (8) | **Removed as a toggle**: a transcript is produced for every attempt the tier allows (UXF-31) |
| On-device recognition, Acoustic DSP, Recognition engine incl. "Whisper / Moonshine" (9–11) | **Removed**; tier status is shown in SCR-SET-04 |
| Recognition language (12) | SCR-SET-04 |
| Speech & Transcription Diagnostics panel (13) | **Removed from Settings**; About → Share diagnostics (PII-free) |
| Camera & Body Language Analysis (14) | **Removed** (video out of R1, RD-16 §5) |
| Privacy statement (15) | SCR-SET-09, generated from policy constants (SRS-FR-SET-006) |
| Updates: status, channels, Check for Updates (16–18) | **Removed** (D-03; no update notifications) |
| Backup Data (19) | **Removed**; Android backup per D-23, sync when signed in, export in SCR-SET-06 |
| Open Admin & Content Studio (20) | **Removed** (NG-10) |
| Theme (21) | SCR-SET-05 |
| Reference Voice Speed slider (22) | Replaced by "Listen" / "Listen slowly" in the session player (SRS-FR-SESS-012) |
| First-Use Feature Overlays switch, Replay Tours (23–24) | About → "Show tips again" (§2.4) |
| YouTube API key field and button (25–26) | **Removed** (ADR-011) |
| Delete Speaking Session History (27) | SCR-SET-06 → Clear history |
| Sign Out (28) | SCR-SET-07 (dialog per D-21) |
| Delete account (29, missing) | SCR-SET-07 → SCR-ACC-02 |

**(d)–(g).** Each row opens its screen; rows are `ListItem`s with a leading 24 dp icon, title, summary and chevron. Hierarchy follows PRD-FR-091. Medium/expanded: centred 640 dp list; on expanded, list-detail with the group screen in the detail pane. Loading: summaries render as text skeletons after 300 ms; offline identical.

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-86 | SCR-SET-01 shows exactly seven rows in the order Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account, About, each with a summary of its current state; no settings screen contains engine or library names, diagnostics panels, API-key fields, cloud AI controls, update channels or "Check for updates" (UXF-21; FND-UI-14). | Robolectric + string search |
| AC-DES-87 | On every settings screen (SCR-SET-01…09) each row's whole area is the target (≥ 48 dp); switch rows announce label, supporting text, "switch" and on/off; radio groups announce selection and position; group titles are headings (DES-07 A11Y-07, A11Y-19); at font scale 2.0 titles and summaries wrap without truncation (A11Y-13). | Semantics test + Robolectric fontScale 2.0 |

#### 3.7.3 SCR-SET-04 Speech & privacy

**(a) Current problem.** Capability shown as "ready" or "verified" without checks; a privacy statement contradicted by the code; developer chips for recognition engines; live transcription off by default (UXF-31, UXF-60; FND-AI-05, FND-AI-06, FND-SEC-05; ui_audit §8.7 rows 8–15).

**(b) Proposed experience.** One honest place for how speech is handled: the verified recognition status, the recognition language, the offline pack, the online-transcription consent (only where relevant), how long recordings are kept, and a link to exactly what stays on the phone.

```
+------------------------------------------+
| <-  Speech & privacy                     |
+------------------------------------------+
|  Speech recognition                      |  heading
|  +------------------------------------+  |
|  | On-device transcription            |  |  ARC-05 §3 label, verified only
|  | Works without internet on this     |  |  ("Checking speech recognition…"
|  | phone.                             |  |   while unverified)
|  +------------------------------------+  |
|  Recognition language                  > |
|  English (India)                         |
|  Offline speech pack                     |  API 33+ where supported
|  Installed · English (India)             |  or [ Download ] / progress / Retry
|  Online transcription            [off]   |  T3-eligible devices only
|  Sends your voice to {provider} to make  |
|  a transcript. Off unless you allow it.  |
|                                          |
|  Recordings                              |  heading
|  Keep recordings for                     |
|  ( ) Delete after feedback               |
|  ( ) 7 days                              |
|  (*) 30 days                             |  default (D-10)
|  ( ) Until I delete them                 |
|  Delete transcripts when recordings      |
|  expire                          [off]   |  D-24 option
|                                          |
|  ( What stays on this phone )            |  -> SCR-SET-09
+------------------------------------------+
```

**(d) Interactions.** Recognition language → sheet with supported locales (SRS-FR-SPCH-008). Download → determinate progress with percentage and size; failure "Download didn't finish. Check your connection and try again." + Retry. Turning **on** online transcription opens a `ModalBottomSheet` with the SRS-FR-SPCH-005 consent text and two equal-weight outlined buttons, "Allow online transcription" and "Cancel"; the switch turns on only after Allow. Turning **off** applies before the next attempt (SRS-FR-SPCH-006) with the snackbar "Online transcription is off. Your next attempt will use audio-only feedback." Choosing a shorter retention shows the §2.8 dialog with the number of recordings that will be deleted now. **(e)** status → language and pack → consent → retention → privacy link. **(f)** centred 640 dp. **(g)** Offline: download and consent rows show "Needs a connection"; status unaffected.

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-88 | The status card shows only an ARC-05 §3 label that matches the verified tier ("Checking speech recognition…" until verified, re-evaluated per SRS-FR-SPCH-003); the language list contains only locales the recogniser supports. | Robolectric with capability fakes |
| AC-DES-89 | The online-transcription row appears only on T3-eligible devices; enabling it requires "Allow online transcription" in a consent sheet whose text matches SRS-FR-SPCH-005 and whose two buttons have equal visual weight; disabling it takes effect before the next attempt; offline-pack download shows progress, failure and Retry, and offline shows "Needs a connection" (SRS-FR-SPCH-004…006). | Robolectric + network spy |
| AC-DES-90 | Retention offers four options with 30 days pre-selected for a new profile; a shorter choice first shows the dialog with the count of recordings to delete; "Delete transcripts when recordings expire" defaults to off; the privacy link opens SCR-SET-09, whose text matches the policy-constant snapshot (SRS-BR-022, SRS-PRIV-002; D-10, D-24). | Robolectric + snapshot test |

#### 3.7.4 SCR-SET-06 Data & privacy

**(a) Current problem.** No export exists; account deletion has no caller; "Backup Data" restores only three preferences (UXF-52; FND-SEC-03; ui_audit §8.4, §8.7 rows 19, 27, 29).

**(b) Proposed experience.** Two clear tasks — take your data with you, and clear what you no longer want — plus a link to the retention control (which lives in SCR-SET-04, §5 issue 2).

```
+------------------------------------------+
| <-  Data & privacy                       |
+------------------------------------------+
|  Export my data                        > |  -> export sheet
|  A file with your profile, practice,     |
|  words and history.                      |
|  Clear history                         > |  -> options
|  Recordings are kept for 30 days       > |  link row -> SCR-SET-04
|  ( What stays on this phone )            |  -> SCR-SET-09
+------------------------------------------+

Export sheet                               Clear history
+------------------------------------------+   +------------------------------------------+
|  Export my data                     (x)  |   | <-  Clear history                        |
|  We'll create a file with your profile,  |   |  Recordings only                       > |
|  settings, attempts, transcripts, words  |   |  Attempts and transcripts              > |
|  and plans that you can save or share.   |   |  Word progress                         > |
|  Include my recordings (ZIP)     [off]   |   |  Everything                            > |
|  Recordings contain your voice. Only     |   |  Each row shows what it deletes and      |
|  share this file with people you trust.  |   |  opens the §2.8 confirmation dialog.     |
|  [[            Export            ]]      |   +------------------------------------------+
+------------------------------------------+
```

**(d) Interactions.** Export builds the JSON file (and optional ZIP, D-22) locally, shows the loading button state, then opens the system share sheet (SRS-FR-SET-004); failure: "Couldn't create the export. Check your storage and try again." Each Clear history option opens a dialog stating exactly what is deleted and what stays (consequences owned by FS-10; §5 issue 24); for signed-in users the body adds "This also deletes the copies saved to your account." Success: snackbar "{Option} deleted". "Everything" differs from Account → "Delete everything on this device" as defined by FS-01 (issue 6 there). **(e)** export → clear → retention link → privacy information. **(f)** centred 640 dp; the export sheet max 640 dp. **(g)** Export and clearing work offline; signed-in cloud deletions are queued for sync (SRS-FR-SET-005).

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-91 | Export always produces the JSON file; "Include my recordings (ZIP)" is off by default and shows the voice warning; the Export button shows its loading state and cannot be pressed twice; success opens the share sheet, failure shows the storage message with retry; export works offline (SRS-FR-SET-004; D-22). | Instrumented test |
| AC-DES-92 | Each of the four Clear history options shows a confirmation naming what will be deleted and what stays, plus the account note for signed-in users; after confirming, only that data is removed and a snackbar confirms it; the retention row links to SCR-SET-04 and is not a second control (SRS-FR-SET-005). | Instrumented test per option |

#### 3.7.5 SCR-SET-08 About

**(a) Current problem.** There is no About, Terms or Privacy policy (ui_audit §3.3 Phase 6); developer diagnostics and update controls sit in Settings; an "App Updates & Backups" screen shows CI secret names and channel switches (UXF-21, UXF-53; FND-UI-13; ui_audit §8.4).

**(b) Proposed experience.** Version, help, legal and licences, a transparent note about where content comes from, and the two maintenance actions users may need: share diagnostics and show tips again. No update checks, channels or update notifications (D-03).

```
+------------------------------------------+
| <-  About                                |
+------------------------------------------+
|  SpeakCraft                              |
|  Version 2.0.0 (200)                     |  versionName (versionCode)
|                                          |
|  Help and contact                      > |
|  About our content                     > |  "Techniques are written in our own
|                                          |   words. Each links to its original
|                                          |   lesson." (no creator name, D-08)
|  Privacy policy                        > |  hosted page (D-30)
|  Terms of use                          > |
|  Open-source licences                  > |  incl. Inter OFL (DES-05 §4.1)
|  Share diagnostics                     > |
|  ( Show tips again )                     |
+------------------------------------------+

Share diagnostics
+------------------------------------------+
| <-  Share diagnostics                    |
|  This file helps us fix problems. It     |
|  includes: app version, phone model,     |
|  Android version, speech recognition     |
|  status and recent error codes.          |
|  It never includes recordings,           |
|  transcripts, your name or your email.   |
|  ( Preview the file )                    |
|==========================================|
|  [[              Share             ]]    |
+------------------------------------------+
```

**(d) Interactions.** Links to hosted pages open the browser (offline: "You're offline. This page needs a connection."). Share diagnostics → share sheet with the PII-free log (ADR-007); Preview shows the exact text. "Show tips again" resets this profile's tips (§2.4) with the snackbar "Tips will appear again." **(f)** centred 640 dp. **(g)** Licences and content note are bundled and work offline.

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-93 | SCR-SET-08 shows version name and code, Help and contact, About our content (without any creator name), Privacy policy, Terms of use, Open-source licences, Share diagnostics and "Show tips again"; no screen in the app offers update checks, release channels or update notifications, and the app posts no notification other than practice reminders (D-03; SRS-FR-REM-004). | Robolectric + manifest/notification audit |
| AC-DES-94 | Share diagnostics lists what the file contains, offers a preview whose content contains no recording, transcript, display name or email (unit-tested against fixtures), and shares through the system share sheet; "Show tips again" makes the first-use tips eligible again for this profile only (§2.4). | Unit + Robolectric test |

#### 3.7.6 SCR-ACC-02 Delete account (explain → confirm → re-authenticate → progress → done)

**(a) Current problem.** `deleteAccountAndAllData` exists but has no caller, so users cannot exercise their data rights (UXF-52; FND-SEC-03; MVM:474).

**(b) Proposed experience.** A short, serious, unambiguous flow (FS-01 §5.4; SRS-FR-ACC-007/008): explain what will be deleted where and offer export first; confirm by typing DELETE; re-authenticate with the system sign-in; show each deletion step; confirm completion and start fresh.

```
EXPLAIN                                    CONFIRM
+------------------------------------------+   +------------------------------------------+
| (x)  Delete account                      |   | <-   Delete account                      |
+------------------------------------------+   +------------------------------------------+
|  Delete your account?                    |   |  Type DELETE to confirm                  |
|  This deletes:                           |   |  +------------------------------------+  |
|  • your progress saved to your account   |   |  | Confirmation          DELETE       |  |  OutlinedTextField
|  • your SpeakCraft sign-in               |   |  +------------------------------------+  |
|  • everything on this phone: practice,   |   |  Type the word in capital letters.       |
|    words, recordings and settings        |   |                                          |
|  This can't be undone.                   |   |==========================================|
|  ( Export my data first )                |   |  [[        Delete my account       ]]    |  error container;
|==========================================|   |  ( Cancel )                              |  enabled only for
|  [[             Continue           ]]    |   +------------------------------------------+  exactly "DELETE"
|  ( Cancel )                              |    -> system re-authentication (Credential
+------------------------------------------+       Manager); cancel = nothing deleted

PROGRESS                                   DONE
+------------------------------------------+   +------------------------------------------+
|  Deleting your account…                  |   |  Your account and all your SpeakCraft    |
|  (v) Data saved to your account          |   |  data have been deleted.                 |
|  (o) Your sign-in                        |   |                                          |
|  ( ) Data on this phone                  |   |==========================================|
|  Please keep the app open.               |   |  [[             Continue           ]]    |  -> onboarding
+------------------------------------------+   +------------------------------------------+
 No Close while deleting; if the app is closed, SCR-SYS-01 resumes on next launch (SRS-FR-ACC-008).
```

**(d)–(g).** Export link opens SCR-SET-06's export sheet and returns here. Offline at Explain: offline row "Connect to the internet to delete your account." and Continue disabled with that reason (FS-01 E8). Re-auth cancelled or failed: return to Confirm with "Sign-in didn't finish. Nothing was deleted." (FS-01 E6). A step failure: inline Error with Retry from the failed step. Medium/expanded: centred 560 dp column.

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-95 | The flow runs Explain → Confirm → re-authentication → Progress → Done; Explain lists what is deleted in the account and on the phone, says it cannot be undone and offers "Export my data first"; "Delete my account" is enabled only when the field equals exactly "DELETE"; nothing is deleted before re-authentication succeeds; Done's "Continue" starts onboarding with a new local profile (SRS-FR-ACC-007; FS-01 AC-FS01-09). | Robolectric + Firebase emulator instrumented test |
| AC-DES-96 | Offline, Continue on Explain is disabled with "Connect to the internet to delete your account."; cancelling re-authentication returns to Confirm with "Sign-in didn't finish. Nothing was deleted."; an interrupted deletion resumes on next launch in SCR-SYS-01 before any other screen (SRS-FR-ACC-008). | Instrumented test |
| AC-DES-97 | With TalkBack, the destructive button is announced "Delete my account" with its disabled or enabled state, the confirmation field has a visible label and an announced error, and each progress step is announced in a polite live region (DES-07 A11Y-08, A11Y-32); at font scale 2.0 the itemised list wraps and the buttons stack (A11Y-13). | Manual TalkBack + Robolectric fontScale 2.0 |

#### 3.7.7 Other settings screens (summary)

| Screen | Content (owner spec) | Key design rules |
|---|---|---|
| SCR-SET-02 Learning preferences | Goal, level, daily time, new words per day (0–5, default 2), countdown before recording (off, OQ-DS-8) (FS-10; SRS-BR-027) | Same options source as onboarding and Profile; changes regenerate only incomplete steps |
| SCR-SET-03 Reminders | Daily reminder on/off and time; "Notifications are off for SpeakCraft" + "Open settings" when blocked (FS-09; SRS-FR-REM-005) | Permission requested only when turning on; no repeated prompts; copy states "at most one a day, none if you've practised" |
| SCR-SET-05 Appearance | Theme System / Light / Dark; Reduce motion Follow system / On / Off; text size follows system (with a link to system settings); Vibration feedback (OQ-DS-5) (SRS-FR-SET-007; DES-05 §8.3, §10) | Reduce motion applies immediately and reactively; system bar icons follow the effective theme |
| SCR-SET-07 Account | States per FS-01 §9: local, signed in (name, email, sync status), signed out with kept data; Sign out (D-21 dialog), Delete account → SCR-ACC-02, Delete everything on this device | Sync status text: "Up to date", "Syncing…", "Waiting for connection", "Couldn't sync — Retry" (SRS-FR-DATA-005) |
| SCR-SET-09 Privacy information | What is stored on this phone, what syncs when signed in, what never leaves the phone, the current tier's behaviour (SRS-FR-SET-006) | Text generated from policy constants and snapshot-tested; reachable from onboarding, SCR-SET-04 and SCR-SET-06 |

| ID | Acceptance criterion | Verify by |
|---|---|---|
| AC-DES-98 | SCR-SET-02 uses the shared options source; SCR-SET-03 requests notification permission only when the reminder is turned on and shows the blocked state with "Open settings"; SCR-SET-05 changes theme and reduce-motion immediately, with system bar icons matching the effective theme and reduced motion reacting to the system setting without restart (DES-07 A11Y-21; DES-05 §8.3, §10). | Robolectric + instrumented test |
| AC-DES-99 | SCR-SET-07 shows the FS-01 state for local, signed-in and signed-out-with-data profiles, with sync status in plain words; SCR-SET-09 text equals the policy-constant snapshot and describes the current tier (SRS-FR-SET-006, SRS-PRIV-002). | Robolectric + snapshot test |

---

## 4. Traceability of the binding design rules

| Rule (brief / Product Owner) | Where specified | Verified by |
|---|---|---|
| Today has one primary action | §3.2.1 | AC-DES-22, AC-DES-26 |
| The session player has one obvious control | §3.3 (b), §3.3.3 | AC-DES-34, AC-DES-39 |
| Results: what we heard, ≤ 3 headline metrics with Measured/Estimated badges, one strength, mistakes found, coach tips, actions (one filled button: "Continue" in Today, "Try again" in Practice and Words), "Was this accurate?" (PRD-FR-047; DES-05 §11.15) | §3.3.5–3.3.8, §3.5 stage results | AC-DES-41, AC-DES-42, AC-DES-45, AC-DES-47, AC-DES-49, AC-DES-50, AC-DES-71, AC-DES-73 |
| No composite overall score for free speaking (D-06) | §3.3.5 | AC-DES-43 |
| Inconclusive instead of guessed results | §3.3.10 | AC-DES-52 |
| Brand launch moment ≤ 1 s, skipped under reduced motion, never blocks input | §2.3 | AC-DES-05, AC-DES-06 |
| Contextual first-use tips replace the four tutorial systems | §2.4 | AC-DES-07, AC-DES-94 |
| Techniques collection and technique detail (PRD-FR-056; SRS-FR-PRAC-008/009) | §3.4.1, §3.4.2 (SCR-PRAC-04) | AC-DES-58, AC-DES-63, AC-DES-64, AC-DES-65 |
| D-08 decided: own words, link out, no creator name, likeness or branding (PRD-FR-057; SRS-BR-028) | §1.1, §2.5.2, §3.4.2, §3.7.5 | AC-DES-08, AC-DES-63, AC-DES-64, AC-DES-93 |
| 200 % font scale and TalkBack on every screen | §2.9 and each screen's (h) | AC-DES-11 + per-screen criteria |
| No update notifications (D-03) | §3.7.2, §3.7.5 | AC-DES-93 |
| Account optional (D-05) | §3.1 | AC-DES-12 |

---

## 5. Issues found

Issues found while writing this specification. DES-06 does not modify other documents (GOV-02 DC-1, DC-2); each issue names the owner who should resolve it. "Interim" states what DES-06 assumes until then.

| # | Issue | Source(s) | Interim in DES-06 | Owner |
|---|---|---|---|---|
| 1 | The IA summary in the working paper PLAN_AND_CANON (non-binding part) lists SCR-ONB-01…06, SCR-SET-01…07 and different SCR-WORD numbering; DES-02 §4 lists SCR-ONB-01…08, SCR-SET-01…09, SCR-WORD-02 detail / -03 stage flow / -04 review, and (v0.2) SCR-PRAC-04 | PLAN_AND_CANON "IA (DES-02)"; DES-02 §4 | DES-02 IDs used throughout | Project Manager (working paper) |
| 2 | Recording retention appears in two homes: DES-02 describes SCR-SET-06 as "export, clear, retention", while PRD-FR-092 and SRS-FR-SET-003 put retention in Speech & privacy | DES-02 §4; PRD-FR-092; SRS-FR-SET-003 | Control in SCR-SET-04; SCR-SET-06 has a link row only | UX Strategist (DES-02) |
| 3 | DES-05 §11.16 lists "Recording stopped because of a call" as an Inconclusive reason, but SRS-FR-SESS-010 says an interruption records no attempt and shows an Interrupted state | DES-05 §11.16; SRS-FR-SESS-010 | Interrupted is a separate state (§3.3.11) | Design System Architect |
| 4 | Tier labels differ: SRS-FR-SESS-006 ("On-device", "Online (consented)", "No transcript — audio-only") vs ARC-05 §3 user-facing labels | SRS-FR-SESS-006; ARC-05 §3 | ARC-05 labels (plainer, B1); SRS to reference ARC-05 | BA (SRS) |
| 5 | Two processing timers: DES-05 §11.11 shows "taking longer" + Cancel at 10 s; SRS-FR-EVAL-013 shows Error at 2× NFR-PERF-005 (5 s RD-MID, 10 s RD-LOW) | DES-05 §11.11; SRS-FR-EVAL-013; NFR-PERF-005 | Both applied (§3.3.4); needs one rule per device class | Design System Architect + BA |
| 6 | DES-05 §11.16 example "We heard less than 3 seconds of speech" does not match SRS-BR-001 (1.5 s duration, 0.5 s voiced) | DES-05 §11.16; SRS-BR-001 | Copy states the measured fact, never a threshold (§3.3.10) | Design System Architect |
| 7 | DES-05 CH-2 draws a line from 2 points; SRS-FR-PROG-002 / SRS-BR-024 require ≥ 3 valid attempts for a trend | DES-05 §11.18; SRS-BR-024 | SRS rule (§3.6) | Design System Architect |
| 8 | SRS-FR-WORD-001/-010 and SRS-BR-017 define a "Mastered" status and filter; DES-05 §12.3 bans "Mastered" | SRS §6.7, §16; DES-05 §12.3 | No "Mastered" label; filters New / Learning / Learned / Due. The internal MASTERED status is shown as "Long-term review" (consistency review, CL-62; FS-07) | Product Manager (decide label) |
| 9 | DES-05 §12.3 now states the D-08 naming rule (creator and borrowed framework names banned; plain original names, STAR and PREP allowed; "Watch the original lesson" the only pointer), but §12.2 still lacks "Technique", "Mistakes found", "Coach tips" and "Self-check", and §12.3 bans "AI Coach" without saying "Coach tips" is allowed; RD-16 §5 and DES-01 UXF-07/UXF-55 responses still say creator-derived content is removed rather than re-expressed | DES-05 §12.2–12.3; RD-16 §5; DES-01; PRD-FR-056/057 | Terms used as in PRD/SRS; creator name banned (D-08) | Design System Architect; Product Manager (RD-16) |
| 10 | No requirement defines "Skip this step" or how a session completes when steps are skipped | SRS-FR-SESS-007, SRS-BR-009; DES-07 OQ-A11Y-1 | "Skipped" status; session complete when all steps are done or skipped; skipped steps never count (§3.2, §3.3) | Product Manager (FS-03) |
| 11 | HRC-22 and DS-2 require exactly one filled action per screen; browse screens (Practice home, track list, Profile, Settings, History) have card or row actions only | DES-01 HRC-22; DES-05 DS-2 | Exactly one on task screens, none on browse screens | UX Researcher (DES-01) |
| 12 | DES-05 has no "first-use tip" component | DES-05 §11 | Composed from `OutlinedCard` + `TextButton` (§2.4) | Design System Architect |
| 13 | FS-02 §14 announces "Step 3 of 8" while its §5 indicator covers 7 steps (SCR-ONB-02…08) | FS-02 §5, §14 | "Step n of 7" | Product Manager (FS-02) |
| 14 | The greeting uses {name}, but onboarding collects no name and local users have none | SRS-FR-TODAY-010; FS-02 issue 4, Q3 | Name-less greeting variants; optional name in Profile | Product Owner |
| 15 | SRS-FR-ONB-001 requires a pre-selected default on every step; pre-selecting "Remind me" may read as opt-out against PRD-FR-080 ("enabled only by user choice") | SRS-FR-ONB-001/-005; PRD-FR-080 | "Remind me at 19:00" pre-selected per SRS; validate in usability test, fallback "Not now" | Product Owner |
| 16 | NFR-PERF-002 says the brand moment is "skipped when reduced motion is on", but the system splash runs before the in-app preference can be read | NFR-PERF-002; DES-05 §8.3 | Static splash icon, so behaviour is identical in both modes (§2.3) | Solution Architect (NFR) |
| 17 | Generic issues can contradict a technique being practised (for example a technique that teaches a deliberate pause vs ISSUE_PAUSES_LONG and the "plan your next sentence" focus) | ARC-05 §8.2, §8.4; PRD-FR-056 | Not resolved in UI; needs technique- or exercise-level suppression of conflicting issues and tips | AI/ML Engineer (ARC-05) |
| 18 | DES-02 v0.2 shows Practice → Technique → Activity, but no edge from a result's coach tip (inside the full-screen session player) to SCR-PRAC-04, nor its Back behaviour | DES-02 §3; PRD-FR-047 | The link opens SCR-PRAC-04 above the result; Back returns to the saved result (§3.3 (d)) | UX Strategist (DES-02, DES-04) |
| 19 | DES-05 has no **Mistake list**, **Coach tip card** or external-link pattern, and its icon map has no external-link icon | DES-05 §7, §11 | Proposed patterns in §3.3.5 built from `Card`, `ListItem`, Evidence badge and `TextButton` | Design System Architect |
| 20 | The Product Owner result order (transcript → metrics → strength → mistakes → tips → Try again → accuracy) and "Try again" as the single primary supersede DES-05 §11.15 order, FS-04's "Continue inside Today" and FS-02 §9's "Go to Today" primary | DES-05 §11.15; FS-04; FS-02 §9; PRD-FR-047 | Resolved in the consistency review (CL-38, CL-59): order = DES-05 §11.15 v0.2; primary per DES-05 §11.15 v0.2 — "Continue" when there is a next step (Today session, onboarding, passed word stage/review item), otherwise "Try again". Onboarding now matches FS-02 §9 ("Go to Today" filled) | Design System Architect; Product Manager (FS-02, FS-04) |
| 21 | Self-check answers (ARC-05 §8.4) need a distinct, badge-free treatment, storage and export entry | ARC-05 §8.4; DES-05 §11.14; ARC-03 | "You ticked: {item}", no badge, excluded from metrics (§3.3.5) | Design System Architect; Database Architect |
| 22 | "Try again" as the Words primary (DES-05 §11.15) is awkward after a passed word stage or recalled review item | Product Owner direction 2026-11-21 | Resolved: DES-05 §11.15 v0.2 makes "Continue" / "Next word" primary when passed (§3.3 (b)) | — |
| 23 | Attempt detail must show the mistakes and tips "as recorded"; if only codes are stored and content changes, historical feedback would change | ARC-05 §8.4; ARC-03 (ENT-03/ENT-04) | Specified as recorded at evaluation time (§3.6) | Database Architect |
| 24 | Clear-history consequences are undefined for derived data (does deleting attempts change streak history and practice time, which SRS-FR-PROG-005 derives from attempts and events?) | SRS-FR-SET-005; SRS-FR-PROG-005 | Dialog states consequences "as defined by FS-10" | Product Manager (FS-10) |
| 25 | Word-stage score labels are not defined (Say it and Use it from recognition; Make your own includes an Estimated grammar component) | ARC-05 §9 | Say it / Use it `{Measured}`, Make your own `{Estimated}` (§3.5) | AI/ML Engineer |
| 26 | Listen & repeat's headline "speaking time vs reference duration" has no defined reference-duration source | ARC-05 §7 | Shown as "{n} s · reference {m} s" once defined | AI/ML Engineer |
| 27 | Whether a review completed from Words also completes Today's word-review step is undefined | SRS-BR-009; FS-03; FS-07 | Not assumed | Product Manager |
| 28 | DES-03, DES-04 and DES-07 are being written in parallel: DES-06 cites only A11Y IDs fixed by canon R-3, TC-A11Y IDs from R-3, and FLOW-01…09 from PRD §14 | Canon R-3, R-4 | Re-check citations when those documents are issued | UI/UX Designer |
| 29 | DES-05 OQ-DS-5 (vibration toggle) and OQ-DS-8 (countdown) are still open and affect SCR-SET-02, SCR-SET-05 and the session player | DES-05 §14 | Countdown off; vibration toggle shown only if approved | Product Owner |
| 30 | SRS-FR-PRAC-009 gives activities and tips their own provenance, but DES-02 places "Watch the original lesson" only on SCR-PRAC-04 | SRS-FR-PRAC-009; DES-02 §4 | Link on SCR-PRAC-04 and on coach tips; not on SCR-PRAC-03 (keeps it focused on speaking) | UX Strategist |
| 31 | Preparation starts automatically (SRS-FR-SESS-002 times, skippable), while PRD-FR-030 calls the timer "optional" | PRD-FR-030; SRS-FR-SESS-002 | Auto-start with "Skip preparation"; Record skips (§3.3.2); validate in usability testing | Product Owner / UX Researcher |
| 32 | D-08 also covers notifications and the store listing; reminder copy (FS-09) and store text (ENG-01) are outside DES-06 but must be checked against SRS-BR-028 | SRS-BR-028; FS-09; ENG-01 | DES-06 strings comply; G-7 should also scan notification strings | Product Manager; DevOps / Release Engineer |

---

## Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-21 | Initial draft: experience overview and visual direction; cross-cutting patterns (shell, states, ≤ 1 s launch moment, contextual tips, microcopy, celebrations, haptics, dialog inventory); screen proposals for onboarding, Today, session player, Practice (including SCR-PRAC-04 Technique detail), Words, Progress, Profile, Settings and account deletion; AC-DES-01…99. Includes the Product Owner directions of 2026-11-21: techniques curriculum (PRD-FR-056), D-08 decided "original wording, link out" (PRD-FR-057, SRS-BR-028), and mistakes found + coach tips after every attempt (PRD-FR-047, SRS-FR-EVAL-015/016, SRS-FR-WORD-014, ARC-05 §8.4). | UI/UX Designer |
| 0.2 | 2026-11-21 | Consistency review: CL-59 (result primary per DES-05 §11.15 — "Continue" in Today, "Try again" in Practice and Words; §3.3 (b), §3.3.5–3.3.8, (d), (e), AC-DES-50, §4, §5 issues 20/22), CL-38 (order note = DES-05 §11.15 v0.2), CL-62 (§5 issue 8: MASTERED shown as "Long-term review") | Technical Writer (consistency review) |

