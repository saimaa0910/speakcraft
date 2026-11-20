# Accessibility Guidelines — SpeakCraft (Android, Compose / Material 3)

| Field | Value |
|---|---|
| Document ID | DES-07 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UI/UX Designer / QA |
| Approval | Pending — Product Owner |
| Related | DES-01 UX Audit, DES-05 Design System, DES-06 UI/UX Specifications, NFR (NFR-A11Y-001…006, NFR-MAINT-008), TST-01 Test Strategy |

> **How to read this document.**
> - Every rule (`A11Y-nn`) is a **RECOMMENDATION — Proposed, requires Product Owner approval** (GOV-02 DC-3, DC-5). Rules elaborate the measurable targets in NFR §6; they do not replace them (GOV-02 DC-2).
> - Current-state defects are cited from the UI-layer audit working paper (`docs/redevelopment/working_papers/ui_audit.md`, cited **UIA §n**) and DES-01, with their labels: **[V]** VERIFIED in code, **[I]** INFERRED.
> - **Nothing was built, installed, run or tested in Stage A** (GOV-02 DC-4; DES-01 L-1, L-2). No test case in §15 has been executed. No research with disabled users exists (DES-01 L-3). No statement here claims that anything "passes".
> - Rule IDs, test IDs and section numbers already cited by DES-05 and DES-01 keep their fixed meanings (PLAN_AND_CANON R-3). §18.2 lists every citation and the matching definition.

---

## 1. Scope and conformance target

### 1.1 Scope

| In scope | Out of scope (with reason) |
|---|---|
| All R1 screens and states in DES-02 §4 (38 R1 screens incl. SCR-PRAC-04, `SCR-*`), all design-system components in DES-05 §11, and all copy in `strings.xml` | System UI owned by Android or Google: runtime permission dialogs, Credential Manager sheets, the share sheet, notification shade rendering (we still own notification **text**) |
| R2 screens (SCR-WORD-05/06, SCR-PROG-05, SCR-INBOX-01) when they are specified: the same rules apply without exception | The Play Store listing and the web privacy / deletion page (D-30). **RECOMMENDATION:** apply WCAG 2.2 AA to that page; it is outside this app document. |
| Speech-specific behaviour: recording, reference audio, transcripts, evaluation results (§9) | Video capture and playback (out of R1; ADR-010). Captions become mandatory if video returns (R3). |

**Core-loop set** (used for the mandatory manual TalkBack script, NFR-A11Y-001 "core-loop screens"): SCR-ONB-01…08, SCR-PERM-01, SCR-TODAY-01, SCR-TODAY-02, SCR-SESS-01 (all states), SCR-SESS-02, SCR-WORD-03, SCR-WORD-04, SCR-PROG-01, SCR-PROG-04. All other R1 screens still meet every rule and are covered by the per-screen checklist (§13) and the release sweep (TC-A11Y-028).

### 1.2 Conformance target

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-01** | SpeakCraft targets **WCAG 2.2 Level A and AA, applied to a native Android app** (UX-9), plus the Android-specific requirements in this document (TalkBack, Switch Access, 200 % font scale, 48 dp targets). Where a WCAG success criterion has no direct equivalent on Android, the mapped A11Y rule in §1.3 defines the requirement. | A release may state "meets DES-07" only when the release pack TC-A11Y-039 has been executed on the TST-01 §5 devices with captured evidence and every deviation is approved at gate G6 (PLN-05). Until then the status is "target", not "conformant". This is not a legal conformance claim. | NFR-A11Y-001…006; PRD-NFR-006; RD-15 UX-9 | TC-A11Y-039 |

WCAG 2.2 is interpreted for native software in the way W3C's *Guidance on Applying WCAG to Non-Web ICT* (WCAG2ICT) describes ("page" → screen, "set of pages" → not applicable to a single app). Two AAA-level behaviours are adopted because they matter for this product: animation from interactions can be removed (A11Y-21, SC 2.3.3) and targets are 48 dp, larger than SC 2.5.8 requires.

### 1.3 WCAG 2.2 A/AA success criteria → DES-07 rules

| SC | Name (level) | Rule(s) | Note for SpeakCraft |
|---|---|---|---|
| 1.1.1 | Non-text content (A) | A11Y-02, A11Y-03, A11Y-12 | Icons, charts, level meter, streak cells |
| 1.2.1–1.2.5 | Time-based media (A/AA) | A11Y-28 | R1 has no video. Reference audio has its text on screen; the user's own recording has the transcript where recognition ran. |
| 1.3.1 | Info and relationships (A) | A11Y-04, A11Y-05, A11Y-07 | Headings, roles, merged rows |
| 1.3.2 | Meaningful sequence (A) | A11Y-11 | Traversal order |
| 1.3.3 | Sensory characteristics (A) | A11Y-24 | No "tap the red button" |
| 1.3.4 | Orientation (AA) | A11Y-14 | Portrait and landscape (NFR-COMP-005) |
| 1.3.5 | Identify input purpose (AA) | A11Y-32 | One personal field in R1 (display name); set keyboard options and autofill content type if the Compose version in use supports it (verify in Stage B) |
| 1.4.1 | Use of colour (A) | A11Y-23 | Evidence labels are text |
| 1.4.2 | Audio control (A) | A11Y-22 | No audio without a tap; pause/stop on every playback |
| 1.4.3 | Contrast minimum (AA) | A11Y-15 | Stricter: all text ≥ 4.5:1 |
| 1.4.4 | Resize text (AA) | A11Y-13 | 200 % system font scale |
| 1.4.5 | Images of text (AA) | A11Y-13 | None used (DES-05 §10, no imagery in R1) |
| 1.4.10 | Reflow (AA) | A11Y-14 | Single column at 360 dp and 200 % |
| 1.4.11 | Non-text contrast (AA) | A11Y-16 | Boundaries, focus, chart marks |
| 1.4.12 | Text spacing (AA) | A11Y-13 | Android has no user text-spacing override; layouts tolerate the larger line heights that font scaling produces |
| 1.4.13 | Content on hover or focus (AA) | A11Y-10 | Tips and tooltips are dismissible and do not cover focused content |
| 2.1.1 / 2.1.2 | Keyboard; no keyboard trap (A) | A11Y-31, A11Y-10 | Hardware keyboard and Switch Access |
| 2.1.4 | Character key shortcuts (A) | — | Not applicable: no single-key shortcuts are defined |
| 2.2.1 | Timing adjustable (A) | A11Y-29, A11Y-33 | Preparation skippable; recording limit warned; snackbar timeout |
| 2.2.2 | Pause, stop, hide (A) | A11Y-22 | Level meter moves only while capturing and reflects real input |
| 2.3.1 | Three flashes (A) | A11Y-22 | — |
| 2.4.1, 2.4.5, 3.2.3, 3.2.4 | Bypass blocks; multiple ways; consistent navigation; consistent identification | A11Y-04, A11Y-11 | Set-of-pages criteria are not directly applicable to one app; headings and the 4 fixed destinations (DES-02, UX-8) give the same benefit |
| 2.4.2 | Page titled (A) | A11Y-11 | Screen name announced on entry |
| 2.4.3 | Focus order (A) | A11Y-11 | — |
| 2.4.4 | Link purpose (A) | A11Y-02 | Card and link names match their destination (DES-01 UXF-10) |
| 2.4.6 | Headings and labels (AA) | A11Y-04, A11Y-02 | — |
| 2.4.7 | Focus visible (AA) | A11Y-31 | `borderFocus` on custom components |
| 2.4.11 | Focus not obscured (AA) | A11Y-30 | Sticky bars, snackbar, keyboard |
| 2.5.1 | Pointer gestures (A) | A11Y-20 | Single-tap alternatives |
| 2.5.2 | Pointer cancellation (A) | A11Y-27 | Activation on release (M3 default); no press-and-hold recording |
| 2.5.3 | Label in name (A) | A11Y-31 | Voice Access works by visible label |
| 2.5.4 | Motion actuation (A) | — | Not applicable: no shake or tilt features |
| 2.5.7 | Dragging movements (AA) | A11Y-20 | Sheets have Close; sliders accept taps and keys |
| 2.5.8 | Target size minimum (AA) | A11Y-19 | 48 dp (exceeds 24 px) |
| 3.1.1 / 3.1.2 | Language of page / parts | A11Y-36, A11Y-38 | R1 UI and content are both English (SRS §17); language of parts becomes relevant when the UI is localised |
| 3.2.1 / 3.2.2 | On focus; on input (A) | A11Y-29, A11Y-11 | Selecting an option never auto-advances; results never auto-advance |
| 3.2.6 | Consistent help (A) | — | Covered by DES-05 §11.13 and DES-01 HRC-29: "How we measured this" is in the same position on every metric |
| 3.3.1 / 3.3.3 | Error identification; suggestion | A11Y-32 | What happened + what to do |
| 3.3.2 | Labels or instructions (A) | A11Y-32 | Visible floating labels (DES-05 §11.2) |
| 3.3.4 | Error prevention (AA) | A11Y-32 | Confirmation for destructive actions (DES-05 §11.5) |
| 3.3.7 | Redundant entry (A) | — | Onboarding answers are never asked again (FS-02); no rule needed beyond FS-02 |
| 3.3.8 | Accessible authentication (AA) | — | Sign-in and re-authentication use the system Credential Manager (FS-01); the app asks for no password or puzzle |
| 4.1.2 | Name, role, value (A) | A11Y-02, A11Y-05, A11Y-06, A11Y-07 | — |
| 4.1.3 | Status messages (AA) | A11Y-08, A11Y-33 | — |

### 1.4 Identifiers used in this document

| Pattern | Meaning | Defined in |
|---|---|---|
| `A11Y-01…A11Y-38` | Accessibility rules | This document (§1–§12) |
| `AP-1…AP-7` | Accessibility principles | §2 |
| `C-01…C-24` | Per-screen checklist items | §13 |
| `TC-A11Y-001…039` | Accessibility test cases | §15 (TC-A11Y is owned by DES-07; other documents reference, never redefine — R-4) |
| `REM-01…REM-32` | Remediation rows (local to this document) | §16 |
| `OQ-A11Y-1…7` | Open questions for the Product Owner | §17 |

**Historical namespace warning.** The pre-existing document DOC-014 (`docs/ACCESSIBILITY_AUDIT.md`, graded *Outdated / Partially verified* in RD-01) and RD-04 FM-080 use `A11Y-01…07` with **different** meanings (for example, DOC-014 A11Y-01 is the waveform description). Those are not DES-07 rules. Cite them only as "DOC-014 A11Y-0n". Their claims are not carried over as verified.

---

## 2. Principles

| ID | Principle | What it means in practice | Source |
|---|---|---|---|
| AP-1 | **Built into components, inherited by screens** | Design-system components (DES-05 §11) ship with correct names, roles, states, targets and reduced-motion variants. Screens compose them and add only screen-level semantics (titles, order, focus moves). A screen should not need to repair a component. | DS-5, DS-6 |
| AP-2 | **Material 3 and platform semantics first** | Use M3 components as designed and Compose semantics APIs. No custom widget where an M3 one exists; no imperative announcement where a live region or state description works. | DS-7, EP-1 |
| AP-3 | **The same honest information for everyone** | Whatever a sighted user learns from a screen (Measured / Estimated / Inconclusive, target status, recording state, tier label) a TalkBack, low-vision or colour-blind user learns too, in words. | PP-1, UX-3 |
| AP-4 | **Speaking is the task, but nobody is trapped by it** | Recording works without precise touch, without sight and without colour. Text alternatives exist for all audio. The streak and step rule for users who cannot speak is an explicit Product Owner decision (OQ-A11Y-1), not an accident. | PP-4, PP-7 |
| AP-5 | **Calm announcements** | Announce what the user needs, once, politely. Never narrate timers or meters. Assertive announcements only for blocking problems. | UX-10 |
| AP-6 | **Automate what can be automated, verify the rest on devices** | Robolectric semantics tests catch regressions in CI; TalkBack, Switch Access, Voice Access and Scanner runs on real devices are still required each release. | TST-01, PLN-05 G6 |
| AP-7 | **No unverified claims** | A check is "passed" only with captured evidence (test output, screen recording, Scanner report). | GOV-02 DC-4; EP-5 |

---

## 3. TalkBack and semantics rules (A11Y-02…A11Y-12)

These rules close the semantics gaps in DES-01 UXF-43 (UIA §6 rows 1–4). They apply to every component and screen.

### 3.1 Rules

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-02** | **Meaningful names.** Every interactive element and every informative non-text element has an accessible name that states its purpose in plain words: icon-only buttons name the **action** ("Play recording", "Close session", "How we measured pace"), not the icon; a state icon without visible text names the **state** ("Practised"), although visible text is preferred; clickable cards and links name the destination their visible label promises. Names never include the role ("button") or the word "icon" — TalkBack adds the role. | 100 % of nodes with a click, toggle, select or adjust action have a non-empty name (text or `contentDescription`). Names follow the DES-05 §7 table. | NFR-A11Y-001 | TC-A11Y-009, 025, 028 |
| **A11Y-03** | **Decorative and duplicate content is silent.** Decorative icons, icons next to text that says the same thing, dividers, illustrations and the recording level meter have no accessible name (`contentDescription = null` or `clearAndSetSemantics {}`). Nothing is read twice. | No merged node contains the same phrase twice; the level meter exposes no semantics; badge and list icons have `null` descriptions. | NFR-A11Y-001 | TC-A11Y-009, 002, 003 |
| **A11Y-04** | **Headings.** The screen title and every visually distinct section title are headings (`heading()`): Today and Progress card titles, settings group titles, feedback-card sections ("Here's how it went", "What went well", "Try this next", "What we heard"), empty-state titles, sheet titles. Body text is never a heading. | Every screen exposes ≥ 1 heading (its title); each section in the DES-06 layout has exactly one heading; TalkBack heading navigation reaches every section. | NFR-A11Y-001 ("headings on section titles") | TC-A11Y-005, 011, 021, 025 |
| **A11Y-05** | **Roles and action labels.** Every custom clickable declares the right `Role` (Button, Switch, Checkbox, RadioButton, Tab; Image only for informative images). Where "Double-tap to activate" is vague, add an `onClickLabel` ("open activity", "show full transcript"). Components are used as M3 intends: FilterChip only for filtering, buttons for actions (DES-05 §11.9). Actions available only through gestures also have a `CustomAccessibilityAction`. | 0 design-system clickables without a role; 0 chips used as action buttons (design review); every gesture action also listed as a custom action. | NFR-A11Y-001 | TC-A11Y-006, 025 |
| **A11Y-06** | **State descriptions.** Any state shown visually is also exposed in semantics: selected (tabs, chips, radio options, selected card), on/off, checked, expanded/collapsed, disabled, loading ("Loading"), progress ("Step 2 of 4"; "Stage 2 of 5, Say it, current"), recording ("Recording, 23 seconds", on focus), and slider values in words with units. Built-in M3 semantics are used where they exist; `stateDescription` fills the gaps. | Each stateful component exposes its state in the semantics tree in every state listed in DES-05 §11. | NFR-A11Y-001 | TC-A11Y-003, 006, 017, 019 |
| **A11Y-07** | **Merged semantics for cards, tiles and rows.** A card, list row, metric tile, streak week strip or settings toggle row is announced as **one phrase** and activated as **one target**: `mergeDescendants`, or `toggleable` / `selectable` on the whole row with the inner control's callback set to `null` (DES-05 §11.22). At most one trailing control stays separately focusable (an info button or "Save activity"). Phrase order: title → value → qualifier → status. | One focus stop per card or row (plus ≤ 1 trailing control). The phrase matches the copy in DES-05 / DES-06, for example "Pace, 128 words per minute, measured. Target 120 to 160, within target." | NFR-A11Y-001 | TC-A11Y-004, 006, 013, 002 |
| **A11Y-08** | **Live regions for state changes.** Changes the user must know about while focus stays where it is are announced through a live region on the status text (or the M3 component's built-in announcement): recording started and stopped (SRS-FR-SESS-004), near limit (once), auto-stop, processing, result ready (with A11Y-09), Inconclusive with its reason, interrupted, microphone busy, loading after 300 ms (once), offline (once), snackbars. **Polite** by default; **Assertive** only for blocking errors and an interrupted recording. Never on continuously changing values (timer, level meter, countdown digits). | The status nodes listed in §3.3 carry the specified live-region mode; nothing else does. Manual: each event is announced exactly once. | NFR-A11Y-001 ("recording state announced") | TC-A11Y-003, 012, 034, 037, 026 |
| **A11Y-09** | **Focus moves to the result header.** When SCR-SESS-01 enters the Result state (including the Inconclusive variant, which keeps the header, DES-05 §11.15), accessibility focus moves to the result header "Here's how it went", which is a heading, and TalkBack reads it without user action. The next swipe reaches the Inconclusive card or the strength. The same pattern applies to SCR-TODAY-02 (session complete) and to the account-deletion "done" step. | After the transition the header is focused and announced; the stale "Checking your recording…" text is not re-read. Target: within 1 s of the state change (**Proposed**). | NFR-A11Y-001 | TC-A11Y-011, 025 |
| **A11Y-10** | **Modal content in a real Dialog or ModalBottomSheet.** Anything that blocks the screen is an `AlertDialog` or `ModalBottomSheet` (separate window, focus contained). Back dismisses; focus returns to the control that opened it; sheets have an explicit labelled **Close** button (not swipe-only). No in-window overlays that intercept touches or sit over content; no non-dismissable dialogs (exception: an irreversible operation in progress shows its progress inside the dialog, DES-05 §11.5). Non-modal first-use tips (DES-06 §2.4) are inline cards in the reading order with a labelled Dismiss. | 0 custom overlay composables (code review); TalkBack and keyboard cannot reach content behind an open dialog or sheet; focus returns on dismiss. | NFR-A11Y-001 | TC-A11Y-015 |
| **A11Y-11** | **Focus order and screen identity.** On entering a screen its name is announced and focus starts at the top (Back / Close, then the title). Swipe order follows the visual reading order: top app bar → content (top to bottom, start to end) → sticky action area → navigation bar. Cards and button rows are traversal groups. `traversalIndex` is used only to correct a verified mismatch. Hidden content (collapsed transcript lines, dismissed sheets, the launch splash) is not reachable. | TalkBack order matches the reading order specified for the screen in DES-06; no hidden or off-screen node is reachable. | NFR-A11Y-001 | TC-A11Y-016, 025 |
| **A11Y-12** | **Charts have a text summary and a list alternative.** Every chart (DES-05 §11.18) has (a) a summary description naming the metric, period, number of points, first and last values with units and target-band context (DES-05 CH-10), (b) a "View as list" button giving every plotted point as text (date, value with unit, Measured or Estimated), and (c) estimated series identified in words, not by dashes alone. Empty and single-point charts state why there is no line. | Summary present and complete; list rows = plotted points; toggle is a 48 dp button. | NFR-A11Y-001, NFR-A11Y-006 | TC-A11Y-014 |

### 3.2 Compose implementation notes (guidance for Stage B; verify against the Compose version in use)

The project uses Compose BOM 2024.09.00 (Compose UI 1.7 line, Material 3 1.3 line; `gradle/libs.versions.toml`) [V for the BOM; INFERRED for the library versions it maps to].

| Rule | Preferred pattern | Avoid |
|---|---|---|
| A11Y-02 | `Icon(contentDescription = stringResource(R.string.cd_play_recording))` inside `IconButton`; for a clickable row, the visible text is the name | Icon names ("mic icon"), "button" in names, hardcoded description strings |
| A11Y-03 | `contentDescription = null`; `Modifier.clearAndSetSemantics {}` for the level meter (DES-05 §11.12) | Describing decoration; repeating adjacent text |
| A11Y-04 | `Modifier.semantics { heading() }` in `SectionHeader`, top-bar titles and sheet titles | Styling text big and bold without `heading()` |
| A11Y-05 | `Modifier.clickable(role = Role.Button, onClickLabel = …)`; `customActions = listOf(CustomAccessibilityAction(label) { … })` | `pointerInput` tap detection without semantics |
| A11Y-06 | M3 `Tab(selected)`, `Switch`, `Checkbox`; `stateDescription = …`; `progressBarRangeInfo = ProgressBarRangeInfo(current, range, steps)` | Selection shown by colour only (UIA §6, LAT:283, 310, 488) |
| A11Y-07 | `Modifier.semantics(mergeDescendants = true) {}`; `Modifier.toggleable(value, role = Role.Switch, onValueChange)` on the row with `Switch(onCheckedChange = null)`; `Modifier.selectableGroup()` for radio groups | Bare `Switch` next to a separate `Text` (AS:624-645) |
| A11Y-08 | `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on the status text node | Live regions on timers or lists; imperative `announceForAccessibility` (discouraged by platform guidance and deprecated at recent API levels — verify against the compile SDK) |
| A11Y-09 | A focusable header with a `FocusRequester` requested once when the state becomes Result; the header is a heading. Whether TalkBack follows input focus on all target API levels must be confirmed on device (INFERRED). | Moving focus repeatedly; focusing a non-heading container |
| A11Y-10 | `AlertDialog`, `ModalBottomSheet` with a Close `IconButton`; restore focus with a `FocusRequester` on the invoking control | `Box` overlays drawn over the screen (current TutorialOverlay and NotificationDrawer, UIA §6) |
| A11Y-11 | Layout order equals reading order; `Modifier.semantics { isTraversalGroup = true }` on cards; `paneTitle` for full-screen flows if the screen name is not announced on entry (verify) | Visual reordering with `offset`/`zIndex` that breaks reading order |
| A11Y-12 | `Canvas` wrapped in a node with `contentDescription` = summary string; separate list composable behind a 48 dp toggle | Point-by-point descriptions on the canvas |

### 3.3 Announcement policy (what is announced, how, and when)

| Event (screen) | Mechanism | Mode | Example text (final copy in DES-06 / `strings.xml`) | Rule |
|---|---|---|---|---|
| Screen entry | Screen title / pane title | — | "Today" | A11Y-11 |
| Recording started (SCR-SESS-01) | Status text live region + button state change | Polite | "Recording" | A11Y-08, A11Y-26 |
| Near limit | Status text live region, once | Polite | "10 seconds left" | A11Y-08, A11Y-29 |
| Recording stopped / auto-stopped | Status text live region | Polite | "Recording stopped" / "Time's up. Recording stopped." | A11Y-08 |
| Processing | Status text live region | Polite | "Checking your recording…" | A11Y-08 |
| Processing > 10 s | Status text live region | Polite | "This is taking longer than usual" | A11Y-29 |
| Result ready | Focus move to header | — | "Here's how it went, heading" | A11Y-09 |
| Inconclusive | Card live region (after header focus) | Polite | "We couldn't measure this attempt. We heard less than 3 seconds of speech." | A11Y-08 |
| Interrupted | State live region | Assertive | "Recording stopped because of a call" | A11Y-08, A11Y-32 |
| Microphone busy | Status live region | Polite | "The microphone is being used by another app" | A11Y-08, A11Y-32 |
| Permission denied | Focus move to the card | — | "SpeakCraft needs the microphone to hear your practice" | A11Y-32 |
| Blocking load/save error | Error state live region | Assertive | "Couldn't load your history. Your recordings are safe." | A11Y-32 |
| Confirmation with undo | M3 Snackbar | Polite | "Recording deleted. Undo" | A11Y-33 |
| Offline (where relevant) | Info row live region, once | Polite | "You're offline. Your practice is saved on this phone." | A11Y-33 |
| Loading > 300 ms | Container description, once | Polite | "Loading" | A11Y-33 |
| Timer, level meter, countdown digits | **Not announced** (timer value available on focus) | — | — | A11Y-08 |

---

## 4. Font scaling and reflow

DES-05 §4.4 (FS-1…FS-9) holds the typographic rules; these rules make them testable.

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-13** | **Text scales to 200 %; no fixed-height text containers.** All text is in `sp` and follows the system font scale up to 200 % (Android 14+ non-linear scaling), together with the largest display size. Containers that hold text use `heightIn(min = …)` / `defaultMinSize`, never `height(…)` or `size(…)` (DES-05 FS-3). No images of text. | At font scale 2.0 on a 360 × 640 dp window, no essential text node reports visual overflow or an unintended ellipsis, and no two text nodes overlap. | NFR-A11Y-002 | TC-A11Y-007, 023 |
| **A11Y-14** | **Reflow and reachability.** Essential text (prompts, feedback, errors, button labels, metric values, evidence labels) is never truncated; `maxLines` with ellipsis only for previews whose full text is one tap away (DES-05 FS-4). Side-by-side buttons wrap or stack (FS-5); metric tiles reflow (FS-6). All content is reachable by vertical scrolling; text content never needs horizontal scrolling. Content scrolls fully clear of sticky action areas. Portrait and landscape are both supported with no orientation lock (NFR-COMP-005). | At 1.0 / 1.3 / 1.5 / 2.0 every action on the screen can be reached and activated; nothing is cut off at the end of the scroll; landscape shows the same content. | NFR-A11Y-002 | TC-A11Y-007, 023, 032 |

"Essential text" means any text whose loss changes what the user understands or can do. Decorative meta text in a preview card is not essential, but its full value must be one tap away.

---

## 5. Contrast

The authoritative colour values and the computed ratio table are in **DES-05 §3.4**. Every text pair there is ≥ 5.34:1 and every boundary pair ≥ 4.07:1 (computed in Stage A from hex values; not yet measured on a device).

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-15** | **Text contrast ≥ 4.5:1 for all text, at every size**, in light and dark themes. (Stricter than WCAG, which allows 3:1 for large text; the DES-05 palette already meets it.) Disabled controls are exempt (M3 38 % content). | Every text pair in DES-05 §3.4 computes ≥ 4.5:1; Accessibility Scanner reports 0 text-contrast findings on R1 screens except documented exemptions. | NFR-A11Y-003 | TC-A11Y-001, 028 |
| **A11Y-16** | **Non-text contrast ≥ 3:1** against adjacent colours for: boundaries needed to identify a control (outlined field, outlined button, Estimated badge outline), focus indicators, selected-state indicators, chart lines and markers, meaningful icons (icon-only buttons), and system-bar icons against the bar in the effective theme (DES-01 UXF-48). Supplementary fills listed in DES-05 §3.4 as "no requirement" are allowed only because text or an icon also carries the state (A11Y-23). | Every boundary, focus and graphic pair in DES-05 §3.4 computes ≥ 3.0:1; Scanner shows no non-text contrast findings. | NFR-A11Y-003 | TC-A11Y-001, 024, 028 |
| **A11Y-17** | **Contrast integrity.** UI code uses DES-05 system tokens only (DES-05 §2; CI guardrail G-1). Both themes are designed and tested. No dynamic colour (DES-05 §3.6). No translucent text except the M3 disabled state. No text over images or gradients. A new foreground/background pairing is added to DES-05 §3.4 and to TC-A11Y-001 **before** it is used. | TC-A11Y-001's pair list equals the DES-05 §3.4 table; G-1 = 0 violations; every screen rendered in both themes without hardcoded colours. | NFR-A11Y-003 | TC-A11Y-001, 024 |

---

## 6. Touch targets and gestures

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-18** | **One target per action; no nested targets.** A clickable card or row has one destination and at most one trailing `IconButton` with its own name (DES-05 §11.3). Touch areas of distinct actions do not overlap. (Fixes the 24 dp check inside a tappable plan row, DES-01 UXF-24/UXF-40.) | No two distinct actionable nodes have overlapping touch bounds. | NFR-A11Y-004 | TC-A11Y-008 |
| **A11Y-19** | **48 × 48 dp minimum touch target** for every interactive element. Visuals may be smaller (32 dp chips, 40 dp dense buttons, 24 dp badges are non-interactive) as long as the touch area is 48 dp. Keep the M3 minimum interactive size enforcement on (never override it to 0). The Record button is 80 dp (64 dp in height-compact). Text links inside a paragraph are avoided; where unavoidable they follow WCAG's inline exception. | Touch bounds ≥ 48 × 48 dp for every node with a click, toggle, select or adjust action; Scanner reports 0 touch-target findings. | NFR-A11Y-004 | TC-A11Y-008, 019, 028 |
| **A11Y-20** | **Pager dots and gestures have 48 dp targets and button alternatives.** Every swipe, drag or multi-finger action has a single-tap alternative: a pager has Next / Back buttons, and its dots are either ≥ 48 dp targets or non-interactive decoration (DES-01 K-5); a sheet can be closed with its Close button; a slider accepts taps, keyboard arrows and TalkBack adjust gestures; swipe-to-delete (if ever used) also has a visible button and a custom accessibility action. No long-press-only action and no press-and-hold recording. | Each gesture in DES-06 has a listed alternative; Robolectric finds the alternative button; manual Switch Access run completes every gesture flow. | NFR-A11Y-004 | TC-A11Y-008, 015, 027 |

---

## 7. Motion

DES-05 §8.3 defines the default and reduced variant of every animation; §8.4 the motion limits.

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-21** | **Reduced motion follows the system setting and an in-app preference.** Effective setting = in-app Appearance → "Reduce motion" (*Follow system* default / *On* / *Off*, SRS-FR-SET-007). *Follow system* tracks `Settings.Global.ANIMATOR_DURATION_SCALE == 0` **reactively** (content observer), not read once (fixes CC:111-124). The effective value is provided at the theme root (`LocalReducedMotion`) and read by every design-system animation helper and every screen (fixes the dashboard, drawer, score ring, flashcard and onboarding gaps, DES-01 UXF-45). | With the effective setting on, every row of DES-05 §8.3 renders its reduced variant; a change to the system setting or the in-app preference takes effect while the app is open, without restart. | NFR-A11Y-005 | TC-A11Y-010, 033 |
| **A11Y-22** | **No flashing, no unrequested movement or sound.** Nothing flashes more than 3 times per second. No looping animation except the live level meter (only while capturing, driven by real input) and indeterminate progress. No auto-advancing carousels or content. No audio starts without a user action: reference audio (TTS) and recording playback start only on tap, and every playback has a labelled Pause / Stop. No parallax, bounce or overshoot (DES-05 §8.4). | Design review of DES-06 motion specs; Robolectric shows no running infinite transition under reduced motion; no autoplay found in TC-A11Y-038. | NFR-A11Y-005 | TC-A11Y-010, 038 |

---

## 8. Colour independence

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-23** | **Colour is never the only carrier of meaning.** Every coloured state also has text and/or an icon or shape. **Measured / Estimated / Inconclusive always carry visible text** (the badge text is mandatory, DES-05 §11.14) and the word is part of the tile's merged phrase. Pass / below-threshold results use words ("Not yet. Try again."), never red or green alone; target status is neutral text ("Within target"). Streak cells add a check icon and a text equivalent; chart series differ by dash and marker as well as colour and are labelled; errors have an icon and text; selected states have a check icon or border change plus selected semantics. | In a monochrome (greyscale) walkthrough every state remains identifiable; badge text present in every evidence state. | NFR-A11Y-006 | TC-A11Y-002, 013, 014, 018, 022 |
| **A11Y-24** | **No sensory-only cues.** Instructions never depend on colour, shape, position or sound alone ("tap the red button", "the button on the right"). Haptics and sounds always accompany a visible and accessible change, never replace it (DES-05 §9). Recording state is shown by text ("Recording"), the timer, the icon change (Mic → Stop) and the dot — not by the red fill alone. | Copy review of `strings.xml` finds no sensory-only instruction; greyscale and TalkBack walkthroughs lose no information. | NFR-A11Y-006 | TC-A11Y-006, 022 |

---

## 9. Speech-specific accessibility

A speaking-practice app has accessibility problems that generic guidance does not cover: the microphone and the screen reader share the same room, the core task needs a voice, and audio content needs a text equivalent. The rules below apply to SCR-SESS-01/02, SCR-ONB-07/08, SCR-WORD-02…04 and every component that records or plays audio.

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-25** | **Labelled speech-rate control (Listen & repeat).** Reference audio for TPL-READ / TPL-REPEAT offers 1.0× and 0.75× (SRS-FR-SESS-012). The control has a visible group label ("Reference audio speed"); each option has words ("Normal", "Slower") and its semantics include the rate ("Slower, 0.75 times speed"); the selected option is announced (segmented-button or radio semantics). The label matches the real rate (fixes "Listen 0.75x" playing at 0.72, DES-01 UXF-18). If Settings keeps a continuous rate slider (current `tts_rate`, DES-01 K-15), it has a label, a value in words with unit, discrete steps, and keyboard and TalkBack adjustment. The Listen button names its action ("Listen to the sentence"). | Semantics as specified; ≥ 48 dp; the rate passed to the TTS engine equals the labelled value. | NFR-A11Y-001, NFR-A11Y-004; SRS-FR-SESS-012 | TC-A11Y-019 |
| **A11Y-26** | **Record-button states.** Each state in DES-05 §11.12 exposes the specified description, state and announcement: Ready "Start recording"; Countdown "Cancel" with the countdown announced **before** capture; Recording "Stop recording" with state "Recording, 23 seconds" on focus; Near limit announced once; Processing polite; Permission needed; Microphone unavailable announced; Interrupted announced. The button stays the **same semantics node** from Ready to Recording so focus stays on it and a second activation stops. The displayed state always matches the real capture state (never "Stop" when nothing is recording, DES-01 UXF-37). State is shown by label, icon and timer, not colour alone. | Semantics assertions for each state; manual: start and stop with two double-taps without moving focus. | NFR-A11Y-001; NFR-PRIV-001; SRS-FR-SESS-004 | TC-A11Y-003, 026, 037 |
| **A11Y-27** | **Hands-free and low-dexterity recording.** Start and stop by a single tap (no hold, no gesture), debounced 500 ms (helps users with tremor); Enter or Space toggles when focused; operable with Switch Access. Capture stops automatically at the template maximum with a 10-second warning (SRS-FR-SESS-003), and on sustained silence where FS-04 enables it, so no touch is needed to end an attempt. No step requires touching the screen during capture. **Screen-reader audio:** TalkBack speech played through the speaker can be captured by the microphone and appear in the transcript (INFERRED; acoustic coupling). RECOMMENDATION (Proposed, decision in OQ-A11Y-2): when touch exploration is on, show the tip "Use headphones so TalkBack isn't recorded" and start capture only after the start cue has finished. **Voice Access** ("Tap Record", "Tap Stop") may compete with the app for the microphone; behaviour must be observed on device (INFERRED). | Manual scripts complete start/stop with TalkBack, keyboard, Switch Access and (outcome recorded) Voice Access; keyboard toggle works in Robolectric. | NFR-A11Y-001; SRS-FR-SESS-003/004 | TC-A11Y-026, 027, 034, 035 |
| **A11Y-28** | **Text alternatives for audio; transcripts are real text.** (a) Reference audio always has its text on screen (prompt card); if a template hides the sentence for listening practice, a "Show sentence" control reveals it (OQ-A11Y-7). (b) The user's recording has the transcript as its text alternative where recognition ran, with the tier label "On-device", "Online (consented)" or "No transcript — audio-only" (SRS-FR-SESS-006). (c) The transcript is selectable text that scales, under the heading "What we heard", with long pauses read as "pause" and target words emphasised by style **and** named in the prompt text. (d) When TTS is unavailable the task stays possible from text and the screen says "Reference audio isn't available on this device" (SRS-ERR-010). | Prompt text present whenever reference audio exists; transcript semantics as specified; TTS-unavailable state shows the text. | NFR-A11Y-001; SRS-FR-SESS-006 | TC-A11Y-021, 038 |
| **A11Y-29** | **Timing under the user's control.** The preparation timer (15 / 30 s, SRS-FR-SESS-002) can be skipped and is announced when it starts, not every second. The optional pre-recording countdown (OQ-DS-8) is announced once ("Recording starts in 3 seconds") before capture. The recording maximum is warned once 10 s before and the auto-stop is explained in text. Processing longer than 10 s shows "This is taking longer than usual" and Cancel (DES-05 §11.11). Nothing advances automatically from a Result or Inconclusive state. No other time limits exist; snackbars follow the system accessibility timeout (A11Y-33). | Test clock confirms skip, single announcements, 10 s warnings and no auto-advance. | NFR-A11Y-001 (no dedicated timing NFR; OQ-A11Y-4); SRS-FR-SESS-002/003 | TC-A11Y-034 |

### 9.1 Users who cannot complete a speaking step (OQ-A11Y-1 — not decided here)

| Topic | Content |
|---|---|
| Current rules | A step is complete only with a valid attempt (SRS-BR-009). A streak day needs ≥ 1 valid speaking attempt (SRS-BR-010). Inconclusive attempts count for nothing (SRS-BR-008). |
| Who is affected (INFERRED; no research data) | Temporary voice loss (illness), speech disabilities, users whose speech the device recogniser handles poorly (repeated Inconclusive on transcript-required templates, SRS-BR-001), users who cannot speak aloud where they are. |
| Already guaranteed regardless of the decision | The user can always skip a step (DES-01 HRC-09). An Inconclusive attempt "doesn't count against you" (DES-05 §11.16). No guilt or loss-framed copy (PP-7). The audio-only path (T3b) still measures delivery without a transcript (SRS-FR-SPCH-007). |
| Decision needed | Whether and how a user who cannot complete a speaking step keeps plan progress and streak. Options are listed in §17 OQ-A11Y-1. Until decided, DES-05 §11.16 reassurance copy must not promise more than SRS-BR-008 states. |

---

## 10. Keyboard, switch and voice access; focus and sticky bars

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-30** | **Focused content is not hidden by sticky bars.** When an element receives keyboard, switch or accessibility focus, or a text field receives input focus, it is scrolled fully into view above sticky bottom action areas, the navigation bar, a visible snackbar and the on-screen keyboard (`imePadding()`, bring-into-view, window insets; DES-05 §5.4). (The current UI has 0 `imePadding`, UIA §6.) | The focused node's bounds do not intersect the sticky bar, snackbar or keyboard bounds. | NFR-A11Y-001, NFR-A11Y-002 (no dedicated NFR; OQ-A11Y-4) | TC-A11Y-029, 027 |
| **A11Y-31** | **Keyboard, Switch Access and Voice Access operability.** Every action is reachable and operable with a hardware keyboard (Tab / Shift+Tab / arrows / Enter / Space / Esc), with Switch Access scanning, and with Voice Access by its visible label. No keyboard traps (dialogs and sheets exit with Esc or Back). Every focusable element shows a visible focus indicator (M3 state layer; `borderFocus` 2 dp on custom components, contrast per A11Y-16). The accessible name contains the visible label words (WCAG 2.5.3) so that voice commands work; for the Record button (visible "Record", name "Start recording") Voice Access behaviour is checked in TC-A11Y-027 and the name is changed through DES-05 if the command fails. | Manual keyboard, Switch Access and Voice Access runs complete the core loop; every clickable node is focusable in Robolectric. | NFR-A11Y-001 (no dedicated NFR; OQ-A11Y-4) | TC-A11Y-027, 015, 035 |

---

## 11. Errors, status messages and spoken values

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-32** | **Errors are announced and say how to fix them.** Every error says what happened and what to do (DES-05 §12.4). Field errors use `isError`, the semantics error description, an icon and visible text (not colour only, DES-05 §11.2), and the user's input is kept. Blocking errors are announced assertively; where the user must act (microphone permission denied or permanently denied, DES-05 §11.19) focus moves to the error card. No raw codes, enums or exception text (DES-01 UXF-35, UXF-53). Destructive actions are confirmed (DES-05 §11.5). | Field-error semantics present with fix text; every error string follows the formula (content review); permission card focused and announced. | NFR-A11Y-001, NFR-A11Y-006 | TC-A11Y-018, 036, 037 |
| **A11Y-33** | **Status messages do not move focus.** Snackbars, offline and sync notices, loading (after 300 ms, once) and save confirmations are announced politely without moving focus. At most one snackbar at a time; never used for blocking errors. Snackbar duration adapts to the system accessibility timeout ("Time to take action") via the M3 `SnackbarHost`; a snackbar action such as Undo is also available elsewhere or stays visible long enough to reach. | Live-region / M3 announcement present; manual: duration lengthens when the system timeout is raised. | NFR-A11Y-001 | TC-A11Y-012, 020 |
| **A11Y-34** | **Units are spelled out in semantics.** Visible short units ("words/min", "s", "%", "min") are expanded in accessibility text: "128 words per minute", "23 seconds", "82 percent", "pass mark 75 percent", "10 minutes speaking". Ranges read "120 to 160"; comparisons read in words ("12 fewer words per minute than last time"). | Semantics text of metric tiles, timers, chart summaries and deltas contains the full unit words. | NFR-A11Y-001 | TC-A11Y-004, 014, 003 |
| **A11Y-35** | **Symbols, timers and non-word text have spoken equivalents.** Timer "0:23 / 1:30" → "23 seconds of 1 minute 30 seconds"; "×" → "times"; "–" in ranges → "to"; the "—" placeholder in an Inconclusive tile is not read; the " … " pause marker → "pause"; IPA is labelled "Phonetic spelling" with the Listen button beside it (TalkBack reads IPA symbols poorly — INFERRED); abbreviations such as "WPM" are not used in UI text; dates are spoken in full through the locale formatter; no emoji carries meaning. | Semantics tests on timer, transcript, tile and word card. | NFR-A11Y-001 | TC-A11Y-021, 003, 004 |

---

## 12. Localisation readiness

R1 ships an English-only UI (SRS §17). These rules make later localisation and correct number formatting possible and keep accessibility text translatable.

| ID | Requirement | Measure / pass condition | Elaborates | Tested by |
|---|---|---|---|---|
| **A11Y-36** | **All strings in resources, with plurals.** Every user-visible string — including `contentDescription`, `stateDescription`, `onClickLabel`, custom-action labels and announcement text — lives in `res/values/strings.xml`. Counts use `<plurals>` ("1 day", "4 days"; never "1 days", DES-01 UXF-57). Sentences are not built by concatenation; placeholders (`%1$s`) keep word order translatable. **Current state:** about **386** hardcoded strings and only 4 string resources (UIA §5.1; DES-01 UXF-47; DES-05 G-6) [V]. | CI guardrail G-6 = 0 in new code (DES-05 §13.4); every count string is a plural resource. | NFR-MAINT-008 (no NFR-A11Y ID) | TC-A11Y-030, 031 |
| **A11Y-37** | **Locale-aware formatting.** Numbers, percentages, durations, dates, times and the first day of the week use the device locale (`NumberFormat`, localised `DateTimeFormatter`, `WeekFields.of(locale)`, DES-05 §12.4 rule 7). No `String.format` without a `Locale` (current D:198; P:418 [V]). Accessibility text uses the same formatter as the visible text. | Unit tests in ≥ 4 locales produce locale-correct output; 0 locale-less `String.format` calls in new code (review / grep). | — (no NFR ID exists; OQ-A11Y-4) | TC-A11Y-031, 013 |
| **A11Y-38** | **RTL and text-expansion readiness.** Layouts use start/end, never left/right; directional icons use `AutoMirrored` (DES-05 §7); `supportsRtl="true"` stays (MAN:24 [V]). Layouts tolerate the expansion of pseudolocale English (XA) and the mirroring of pseudolocale Arabic (XB) without truncation. | Pseudolocale walkthrough shows no truncation or overlap and a correctly mirrored layout. | — (no NFR ID exists; OQ-A11Y-4) | TC-A11Y-032 |

---

## 13. Per-screen checklist template

Used at three points: design review of each DES-06 screen spec, the implementation PR for the screen (PLN-05 Definition of Done item 4), and the release sweep. Copy the template once per screen into the PR description or the test report. A result is recorded only with evidence (AP-7).

### 13.1 Template

| Field | Value |
|---|---|
| Screen ID / name | e.g. SCR-SESS-01 Session player |
| States checked | e.g. prompt, prepare, ready, recording, processing, result, inconclusive, interrupted, error (DES-03 state matrix) |
| Build / commit | — |
| Device or emulator / Android version | From TST-01 §5 |
| TalkBack version; font scale; display size; theme | — |
| Reviewer (role) and date | — |

| # | Check | Rules | How | Result (Pass / Fail / N/A) | Evidence |
|---|---|---|---|---|---|
| C-01 | Screen name announced on entry; focus starts at Back/Close or the title | A11Y-11 | TalkBack | | |
| C-02 | Every interactive element has a name stating its action; visible label words are in the name | A11Y-02, A11Y-31 | TalkBack; TC-A11Y-009 | | |
| C-03 | No decorative or duplicate announcements; level meter silent | A11Y-03 | TalkBack | | |
| C-04 | Title and section titles are headings; heading navigation reaches each section | A11Y-04 | TalkBack heading mode; TC-A11Y-005 | | |
| C-05 | Roles correct (button, switch, checkbox, radio, tab); chips only filter | A11Y-05 | TalkBack; review | | |
| C-06 | States announced (selected, on/off, expanded, progress, loading, recording) | A11Y-06 | TalkBack; TC-A11Y-006/017 | | |
| C-07 | Cards, rows, tiles and toggle rows read as one phrase and are one target (+ ≤ 1 trailing control) | A11Y-07, A11Y-18 | TalkBack; TC-A11Y-004/006 | | |
| C-08 | Status changes announced once, politely, without moving focus; assertive only for blocking problems | A11Y-08, A11Y-33 | TalkBack; §3.3 table | | |
| C-09 | Traversal order matches the DES-06 reading order; hidden content unreachable | A11Y-11 | TalkBack swipe-through; TC-A11Y-016 | | |
| C-10 | Modal content is a Dialog or ModalBottomSheet; Close button; focus contained and returned | A11Y-10 | TalkBack + keyboard; TC-A11Y-015 | | |
| C-11 | Charts: summary description and "View as list" (N/A if no chart) | A11Y-12 | TalkBack; TC-A11Y-014 | | |
| C-12 | Font scale 1.0 / 1.3 / 1.5 / 2.0 on 360 × 640 dp: no clipping or overlap; every action reachable; landscape works | A11Y-13, A11Y-14 | TC-A11Y-023; device spot check | | |
| C-13 | Contrast: Scanner clean in light and dark; only DES-05 tokens used | A11Y-15, A11Y-16, A11Y-17 | Accessibility Scanner; TC-A11Y-024 | | |
| C-14 | Touch targets ≥ 48 dp; no nested or overlapping targets | A11Y-18, A11Y-19 | TC-A11Y-008; Scanner | | |
| C-15 | Every gesture has a single-tap alternative | A11Y-20 | Switch Access | | |
| C-16 | Reduced motion: only reduced variants; reacts to setting changes; no flashing, autoplay or auto-advance | A11Y-21, A11Y-22 | Remove animations on/off; TC-A11Y-010 | | |
| C-17 | Greyscale: no information lost; no sensory-only instructions | A11Y-23, A11Y-24 | Monochromacy simulation; TC-A11Y-022 | | |
| C-18 | Keyboard and Switch Access reach every action; focus visible; focus never hidden by sticky bars or keyboard | A11Y-30, A11Y-31 | TC-A11Y-027, 029 | | |
| C-19 | Errors announced, say how to fix, keep input | A11Y-32 | TalkBack; TC-A11Y-018 | | |
| C-20 | Numbers, units, ranges, timers and symbols are spoken correctly | A11Y-34, A11Y-35 | TalkBack | | |
| C-21 | No hardcoded strings; plurals for counts; locale formatting | A11Y-36, A11Y-37 | G-6 grep; TC-A11Y-031 | | |
| C-22 | Pseudolocales XA / XB: no truncation; layout mirrors | A11Y-38 | TC-A11Y-032 | | |
| C-23 | Speech screens only: rate control labelled; record-button states; hands-free start/stop; text alternatives; timing | A11Y-25…A11Y-29 | TC-A11Y-003, 019, 026, 034, 038 | | |
| C-24 | Every state of the screen has been checked (Loading, Empty, Error, Offline, Permission where applicable) | All | DES-03 state matrix | | |

### 13.2 Screen-specific emphasis (R1)

| Screen(s) | Rules that need particular attention |
|---|---|
| SCR-ONB-01…08 | A11Y-20 (value pager, if used), A11Y-06 (radio options, "Step 3 of 8"), A11Y-14 (sticky Continue at 200 %), A11Y-08 (speech-check result announced) — see FS-02 §14 and TC-ONB-017 |
| SCR-PERM-01 | A11Y-32 (denied / permanently denied, focus to card), A11Y-02 ("Allow microphone", "Open settings") |
| SCR-TODAY-01/02 | A11Y-07 (session card, step rows), A11Y-07 + A11Y-23 (streak week strip), A11Y-09 (session complete) |
| SCR-SESS-01/02 | A11Y-08, A11Y-09, A11Y-25…A11Y-29, A11Y-10 ("How we measured this" sheet), A11Y-34/35 |
| SCR-PRAC-01…03 | A11Y-07, A11Y-18 (activity card + "Save activity"), A11Y-05/06 (filter chips) |
| SCR-WORD-01…04 | A11Y-06 (stage stepper "Stage 2 of 5, Say it, current"), A11Y-25 and A11Y-28 (Listen, word text), A11Y-35 (IPA) |
| SCR-PROG-01…04 | A11Y-12 (charts), A11Y-07 (history rows), A11Y-28 (attempt transcript and playback), A11Y-37 (dates) |
| SCR-PROF-01, SCR-SET-01…09 | A11Y-07 (switch and radio rows), A11Y-21 (Reduce motion preference), A11Y-25 (rate control), A11Y-30 (name field and keyboard) |
| SCR-ACC-01/02, SCR-SYS-01 | A11Y-32 (DELETE field), A11Y-08 (deletion progress list as a polite live region, FS-01 §14), A11Y-09 (done step) |

---

## 14. Test tooling within the existing stack

### 14.1 Available now (no new dependency)

| Tool | Status | Used for |
|---|---|---|
| JUnit 4 (JVM unit tests) | `testImplementation(libs.junit)` (`app/build.gradle.kts:154`) [V] | TC-A11Y-001 contrast computation in pure Kotlin; TC-A11Y-031 formatting logic |
| Robolectric 4.16.1 + Compose UI test (`ui-test-junit4`) | `app/build.gradle.kts:158-160`; existing tests use `@Config(sdk = [36])` and qualifiers such as `w412dp-h915dp` (UIA §10) [V] | Semantics assertions; component and screen rendering at 360 × 640 dp; touch-bound and text-layout checks |
| Compose UI test as instrumented tests | `androidTestImplementation(libs.androidx.compose.ui.test.junit4)` (`app/build.gradle.kts:165`) [V]; only `ExampleInstrumentedTest.kt` exists today [V] | TC-A11Y-033 (real system setting change) on an emulator |
| Compose previews (`ui-tooling`, debug) | `app/build.gradle.kts:167` [V]; `@PreviewFontScale` / `@PreviewLightDark` availability in the BOM in use to verify (DES-05 §11) | Design review at 200 % and in both themes (not a test) |

**Semantics assertions available in Compose UI test** (API names to confirm against the version in use in Stage B):

| Check | Approach |
|---|---|
| Names and duplicates | `onNodeWithContentDescription`, `onNodeWithText(useUnmergedTree = …)`, `printToLog()` to inspect the merged tree |
| Headings | `onAllNodes(isHeading())` |
| Roles and states | `SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)`, `…StateDescription`, `assertIsToggleable()`, `assertIsOn()`, `assertIsSelected()`, `…ProgressBarRangeInfo` |
| Live regions | `SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)` |
| Focus after a state change | `assertIsFocused()` on the header's focus target (input focus; TalkBack focus confirmed manually) |
| Text overflow at 200 % | Wrap content in `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f))` (DES-05 §13.2); read `SemanticsActions.GetTextLayoutResult` and assert `!hasVisualOverflow` and no unintended `isLineEllipsized`. Linear 2.0 scaling in tests is stricter for large text than Android 14's non-linear scaling (INFERRED), so it errs on the safe side. |
| Touch targets | Collect nodes with click / toggle / select / set-progress actions; compare the node's touch bounds (`touchBoundsInRoot`, or `assertTouchHeightIsEqualTo` / `assertTouchWidthIsEqualTo`) with 48 dp |
| Timing | `mainClock.autoAdvance = false`; `mainClock.advanceTimeBy(…)` |
| Keyboard | `performKeyInput { pressKey(Key.Enter) }` on the focused node |
| Reduced motion and theme | Provide `LocalReducedMotion` and the theme mode in a shared test host (DES-05 §13.1–13.2) |

A shared test host (WBS T-17.3) should expose `fontScale`, `widthDp`/`heightDp`, `theme` and `reducedMotion` parameters plus helper assertions (`assertAllTextFits()`, `assertTouchTargetsAtLeast48dp()`, `assertMergedPhrase(…)`). This is test code, not a dependency.

**Limit:** Robolectric cannot run TalkBack, Switch Access or Voice Access, cannot prove TalkBack's real traversal order, and does not reproduce device rendering exactly. Those checks stay manual (§14.2).

### 14.2 Manual tools (on the test device; not project dependencies)

| Tool | Where | Used for |
|---|---|---|
| TalkBack and Switch Access | Android Accessibility Suite on the device | TC-A11Y-011, 015, 016, 025, 026, 027 |
| Voice Access | Google app on the device | TC-A11Y-027 (label in name; microphone sharing outcome) |
| Accessibility Scanner | Google Play app on the device | TC-A11Y-024, 028 (contrast, touch target, labels) |
| Font size and display size; "Remove animations"; "Time to take action (Accessibility timeout)" | Android Settings → Accessibility / Display | TC-A11Y-020, 023, 033 |
| Developer options → Simulate colour space → Monochromacy | Android Settings | TC-A11Y-022 |
| Pseudolocales English (XA) and Arabic (XB) | Requires `isPseudoLocalesEnabled = true` on the **debug** build type — a build-configuration change owned by ENG-01, no dependency | TC-A11Y-032 |
| Hardware keyboard (USB / Bluetooth, or the emulator's host keyboard) | — | TC-A11Y-027 |

### 14.3 Requires approval before use (new dependency or version change — ADR, GOV-02 DC-6, NFR-MAINT-007)

| Candidate | What it would add | Why it needs approval | Recommendation |
|---|---|---|---|
| Accessibility Test Framework checks via `espresso-accessibility` | Automatic Google ATF checks (labels, targets, contrast) during tests | New test dependency | Defer; custom semantics assertions (§14.1) cover R0/R1 (OQ-A11Y-6) |
| Compose UI test built-in accessibility checks (newer Compose UI test versions) | ATF checks inside Compose tests | Requires a Compose BOM upgrade **and** the ATF dependency | Revisit when the BOM is upgraded for other reasons |
| Screenshot testing (for example Roborazzi or Paparazzi) | Golden-image regression at font scales and themes | New dependency | Not needed for R1; bounds and text-layout checks suffice |
| UI Automator | Cross-app instrumented flows (system Settings) | New dependency | Not needed; TC-A11Y-033 can stay manual |

### 14.4 When each check runs

| Cadence | Checks | Gate |
|---|---|---|
| Every PR touching UI | Unit and Robolectric tests, including the automated part of mixed tests: TC-A11Y-001…021, 023, 024, 029…031, 034…038 (CI); checklist §13 for changed screens | PLN-05 Definition of Done item 4 |
| End of each UI epic | Manual on one TST-01 §5 device: TC-A11Y-011, 015, 016, 020, 022, 025 for the epic's screens | Epic acceptance |
| Each release candidate | TC-A11Y-039 release pack (all automated + manual TC-A11Y-022, 025…028, 032, 033 on RD-LOW and RD-MID, TalkBack and Scanner in both themes) | PLN-05 G6; TST-01 §10 exit criteria |

---

## 15. Test cases (TC-A11Y-nnn)

None of these tests exists or has been executed yet (GOV-02 DC-4). Types: **Unit** (JVM), **Robolectric UI** (Compose UI test under Robolectric), **Instrumented** (emulator or device), **Manual** (device with assistive technology). Devices for manual tests come from TST-01 §5. "R1 screens" means DES-02 §4 screens with Release = R1.

| ID | Title | Type | Steps | Expected result | Rules |
|---|---|---|---|---|---|
| TC-A11Y-001 | Token contrast regression | Unit | For every foreground/background pair in DES-05 §3.4, light and dark, compute WCAG 2.x contrast from the `ScPalette` values at 100 % opacity. Assert the test's pair list equals the §3.4 table. | Text pairs ≥ 4.5:1; boundary, focus and graphic pairs ≥ 3.0:1; a failure names the pair and ratio; no unlisted pair exists | A11Y-15, A11Y-16, A11Y-17 |
| TC-A11Y-002 | Evidence badge semantics | Robolectric UI | Render EvidenceBadge as Measured, Estimated, Inconclusive in both themes; render one inside a MetricTile; inspect merged and unmerged trees | Visible text "Measured" / "Estimated" / "Inconclusive" present; badge icon has no description; badge has no click action; inside a tile the word is part of the tile phrase | A11Y-23, A11Y-03, A11Y-07 |
| TC-A11Y-003 | Record button state semantics | Robolectric UI | With a test clock, render the recording panel in Ready, Countdown, Recording (t = 23 s), Near limit, Processing, Microphone unavailable, Interrupted | Descriptions and states match DES-05 §11.12 ("Start recording"; "Stop recording" + "Recording, 23 seconds"); status text carries the live region from §3.3; timer has no live region; timer semantics "23 seconds of 1 minute 30 seconds"; level meter has no semantics; the button node (test tag) is the same in Ready and Recording | A11Y-26, A11Y-06, A11Y-08, A11Y-03, A11Y-34, A11Y-35 |
| TC-A11Y-004 | Metric tile merged phrase | Robolectric UI | Render MetricTile "Pace 128 words/min, Measured, Target 120–160, Within target", and an Inconclusive tile | One merged node: "Pace, 128 words per minute, measured. Target 120 to 160, within target."; info button separately focusable, named "How we measured pace"; Inconclusive tile does not read the "—" and says "Not enough speech to measure" | A11Y-07, A11Y-34, A11Y-35, A11Y-23 |
| TC-A11Y-005 | Headings | Robolectric UI | Render SectionHeader, FeedbackCard, empty-state view, sheet header, and each R1 screen's top-app-bar title | Titles and section titles have the Heading property ("Here's how it went", "What went well", "Try this next", "What we heard"); body text does not | A11Y-04 |
| TC-A11Y-006 | Selection controls and settings rows | Robolectric UI | Render Switch row, Checkbox row, radio group, FilterChip, segmented button, selected card | Each row is one node with the right Role, its label and its state; inner control not separately focusable; radio group is a selectable group; selected card exposes selected state and shows a check icon | A11Y-05, A11Y-06, A11Y-07, A11Y-24 |
| TC-A11Y-007 | Components at 200 % font | Robolectric UI | Host each DES-05 §11 component at 360 dp width with `Density(fontScale = 2f)`; read text layout results and bounds | No essential text node has visual overflow or unintended ellipsis; containers grow; buttons wrap or stack; badge grows | A11Y-13, A11Y-14 |
| TC-A11Y-008 | Touch targets | Robolectric UI | For each component and R1 screen, collect nodes with click, toggle, select or adjust actions; read touch bounds | Every touch area ≥ 48 × 48 dp; no overlapping touch areas between distinct actions; pager dots ≥ 48 dp or non-interactive with Next/Back present | A11Y-18, A11Y-19, A11Y-20 |
| TC-A11Y-009 | Icon names | Robolectric UI | Render icon-only buttons (Close, Back, Play/Pause, Info, avatar), icon + text buttons, decorative icons | Icon-only buttons are named by action, without "button" or icon names; icons beside matching text have no description; no phrase read twice | A11Y-02, A11Y-03 |
| TC-A11Y-010 | Reduced-motion variants | Robolectric UI | Provide `LocalReducedMotion` = true; render session state change, expand/collapse, level meter, milestone, chart; disable auto-advance and step one frame | Each renders its DES-05 §8.3 reduced variant (instant switch, static level bar, no draw-in); no infinite transition other than indeterminate progress | A11Y-21, A11Y-22 |
| TC-A11Y-011 | Focus moves to the result header | Robolectric UI + Manual | Drive SCR-SESS-01 Processing → Result and Processing → Inconclusive with fakes; then repeat on device with TalkBack | Header is a heading and holds focus after the transition; on device TalkBack reads "Here's how it went" (then the Inconclusive card) without user action; processing text is not re-read | A11Y-09, A11Y-04 |
| TC-A11Y-012 | Polite status announcements | Robolectric UI | Render Inconclusive card, processing status, loading (after 300 ms), offline row, blocking error state | Polite live region on Inconclusive card, processing status, loading (once) and offline row; Assertive only on the blocking error; none on lists | A11Y-08, A11Y-33 |
| TC-A11Y-013 | Streak week strip summary | Robolectric UI | Render the week strip with Monday, Tuesday, Thursday practised and today not yet, in en-GB and de-DE; render the new-user state | One merged node: "This week: practised Monday, Tuesday and Thursday. Today not practised yet."; day names and first day of week from the locale; practised cells show a check; new user hears "Start your streak today" | A11Y-07, A11Y-23, A11Y-37 |
| TC-A11Y-014 | Chart text alternative | Robolectric UI | Render LineChart with 8 points and 2 gaps plus an estimated series; with 1 point; with 0 points | Summary names metric, count, first and last values with units, target info; "View as list" is a 48 dp button; one list row per stored point (date, value + unit, Measured/Estimated); 1- and 0-point states explain in text | A11Y-12, A11Y-23, A11Y-34 |
| TC-A11Y-015 | Dialogs and sheets | Robolectric UI + Manual | Open each confirmation AlertDialog and each ModalBottomSheet; press Back; on device repeat with TalkBack and a keyboard | Sheet has a named Close button; Back and Esc dismiss; focus returns to the invoking control; TalkBack and Tab cannot reach content behind; code review finds no in-window overlay | A11Y-10, A11Y-20, A11Y-31 |
| TC-A11Y-016 | Traversal order and screen titles | Robolectric UI + Manual | For SCR-TODAY-01 and every SCR-SESS-01 state, print the semantics tree in traversal order and compare with the DES-06 reading order; swipe through on device | Order matches DES-06; screen name announced on entry; collapsed or hidden content absent. Robolectric order is approximate — the device run decides | A11Y-11 |
| TC-A11Y-017 | Progress and loading semantics | Robolectric UI | Render step progress (2 of 4), stage stepper (stage 2 current), a loading button, offline-pack download progress | "Step 2 of 4" text plus range info; "Stage 2 of 5, Say it, current"; loading button reads "Loading" and is not clickable; download progress gives percent and size in text | A11Y-06 |
| TC-A11Y-018 | Field errors | Robolectric UI | Submit an invalid display name; type a wrong word in the DELETE confirmation field (FS-01) | `isError` and an error description with the fix ("Enter a name, or skip"); label stays visible; input kept; error icon decorative | A11Y-32, A11Y-23 |
| TC-A11Y-019 | Speech-rate control | Robolectric UI + Unit | Render the Listen & repeat speed control and the Settings rate control; select "Slower"; capture the rate sent to a fake TTS | Group label "Reference audio speed"; options named in words with rate; selected state exposed; 48 dp; rate sent = 0.75; Listen button named by action | A11Y-25, A11Y-06, A11Y-19 |
| TC-A11Y-020 | Snackbar duration and announcement | Robolectric UI + Manual | Show a snackbar with an action ("Recording deleted · Undo") and one without; on device set Accessibility timeout to 2 minutes and repeat with TalkBack | Snackbar is announced politely (live region or M3 equivalent) with a dismiss action; on device it is announced once without moving focus and the snackbar with an action stays visible at least the system-recommended time; never Indefinite unless the user must act | A11Y-33 |
| TC-A11Y-021 | Transcript view semantics | Robolectric UI | Render TranscriptView with pauses and target words; render the T3b "no transcript" state | Heading "What we heard"; text selectable; pause marker read as "pause"; tier label present; no-transcript state stated plainly | A11Y-28, A11Y-35, A11Y-04 |
| TC-A11Y-022 | Colour-independence walkthrough | Manual | Enable Monochromacy simulation; walk every R1 screen in every state | Evidence labels, selection, errors, streak, recording, chart series and pass / "Not yet" all identifiable without colour; no sensory-only copy | A11Y-23, A11Y-24 |
| TC-A11Y-023 | Screens at font scale 1.0 / 1.3 / 1.5 / 2.0 on 360 × 640 dp | Robolectric UI + Manual | `@Config(qualifiers = "w360dp-h640dp")`; render every R1 screen in each state at four font scales; text-layout and bounds checks; device spot check at maximum font and display size | No essential text overflows or ellipsises; no overlapping nodes; every action reachable by scrolling; sticky action visible | A11Y-13, A11Y-14 |
| TC-A11Y-024 | Both themes | Robolectric UI + Manual | Render every R1 screen in Light and Dark; on device run Accessibility Scanner in both, including in-app forced theme on an opposite system theme | No hardcoded colours; no Scanner contrast findings beyond documented exemptions; system-bar icons legible (DES-01 UXF-48) | A11Y-16, A11Y-17 |
| TC-A11Y-025 | TalkBack core-loop script | Manual | TalkBack on, RD-MID device: first launch → SCR-ONB-01…08 → SCR-PERM-01 → Today → start session → record → stop → result → Try again → Progress → attempt detail → playback | Every step completed with TalkBack only; names, roles, states, headings, focus moves and announcements as specified; no sighted help needed | A11Y-02…A11Y-12, A11Y-26 |
| TC-A11Y-026 | Recording with TalkBack | Manual | TalkBack on; focus Record; double-tap to start; speak a known sentence; double-tap to stop without moving focus; repeat with wired/Bluetooth headphones and with the speaker | Start and stop without moving focus; both announced; with headphones the transcript contains no TalkBack speech; the speaker result is recorded as evidence for OQ-A11Y-2 | A11Y-26, A11Y-27, A11Y-08 |
| TC-A11Y-027 | Keyboard, Switch Access and Voice Access | Manual | Core loop with a hardware keyboard (Tab, Shift+Tab, arrows, Enter, Space, Esc); with Switch Access (auto-scan and two-switch); with Voice Access ("Tap Record", "Tap Stop", "Tap Continue") | Every action reachable; focus always visible and never hidden by sticky bars or keyboard; no traps; Voice Access activates controls by visible label; microphone-sharing outcome recorded | A11Y-31, A11Y-30, A11Y-20, A11Y-27 |
| TC-A11Y-028 | Accessibility Scanner sweep | Manual | Run Accessibility Scanner on every R1 screen and state, light and dark | 0 unresolved touch-target, contrast, label and clickable-item findings; any accepted finding logged with a justification | A11Y-02, A11Y-15, A11Y-16, A11Y-19 |
| TC-A11Y-029 | Focus not obscured | Robolectric UI + Manual | On SCR-PROF-01 (name field) and a long list with a sticky action: move focus to the last item and to the field; on device also open the keyboard | Focused node bounds fully above the sticky bar, snackbar and keyboard | A11Y-30 |
| TC-A11Y-030 | No hardcoded strings | Unit (CI static check, DES-05 G-6) | Run the G-6 grep guardrail over new code, including `contentDescription`, `stateDescription` and announcement arguments; check count strings | 0 hardcoded user-visible strings outside previews and tests; every count uses `<plurals>` | A11Y-36 |
| TC-A11Y-031 | Locale formatting and plurals | Unit (Robolectric for resources) | Format numbers, percentages, durations, dates and first day of week in en-GB, en-US, de-DE and hi-IN; resolve plurals for 0, 1, 2, 5 | Output follows each locale; semantics strings use the same formatter; "1 day" / "4 days" | A11Y-36, A11Y-37 |
| TC-A11Y-032 | Pseudolocale and RTL | Manual | Debug build with pseudolocales; device language English (XA), then Arabic (XB); walk the core loop at font scale 1.3 | No truncation or overlap from expansion; layout mirrors; directional icons mirror, others do not | A11Y-38, A11Y-14 |
| TC-A11Y-033 | Reduced-motion reactivity | Instrumented + Manual | With the app open, toggle system "Remove animations", then the in-app preference (Follow system / On / Off); navigate Today → session | Changes apply without restart; in-app On/Off override the system; Follow system mirrors it | A11Y-21 |
| TC-A11Y-034 | Session timing and announcements | Robolectric UI | Test clock: preparation 15 s with Skip; countdown (if enabled); record to max − 10 s and to max; processing beyond 10 s; wait on Result | Skip present and named; countdown announced once before capture; "10 seconds left" once; auto-stop explained; "This is taking longer than usual" + Cancel at 10 s; Result never auto-advances | A11Y-29, A11Y-08, A11Y-27 |
| TC-A11Y-035 | Keyboard toggle of the Record button | Robolectric UI | Focus the Record button; send Enter, then Space; send two Enter presses 200 ms apart | Enter and Space toggle start/stop; presses inside the 500 ms debounce do not double-toggle | A11Y-27, A11Y-31 |
| TC-A11Y-036 | Microphone permission states | Robolectric UI + Manual | Fake permission denied once, then permanently denied; return from Settings (ON_RESUME) | Card announced and focused; text says why and what to do; "Allow microphone" / "Open settings" ≥ 48 dp; state re-checked on resume | A11Y-32, A11Y-08, A11Y-19 |
| TC-A11Y-037 | Interrupted and microphone busy | Robolectric UI | Simulate a call interruption during capture and a busy microphone at start | Interrupted state announced assertively with "Record again"; busy state announced with "Try again"; button never shows "Stop" when not capturing | A11Y-08, A11Y-32, A11Y-26 |
| TC-A11Y-038 | Audio controls and text equivalents | Robolectric UI | Render prompt card with Listen (TPL-REPEAT), result playback, and the TTS-unavailable state | No audio starts without a tap; Play/Pause named; prompt sentence visible as text (or a "Show sentence" control per OQ-A11Y-7); TTS-unavailable shows the SRS-ERR-010 text and hides Listen | A11Y-22, A11Y-28 |
| TC-A11Y-039 | Release accessibility pack | Manual + automated | Per release candidate: all automated TC-A11Y in CI; manual TC-A11Y-022, 025…028, 032, 033 on RD-LOW and RD-MID (TST-01 §5); collect evidence | All pass, or each deviation approved and logged at G6 | A11Y-01 |

---

## 16. Remediation list (current defects → rules)

This is the remediation list DES-01 §7 points to for UXF-41…48, extended with every defect in UIA §6 and the accessibility-relevant interaction findings. Most current screens are **retired or replaced, not re-skinned** (RD-16 §5; DES-05 §13.3), so the remediation is usually "the replacement follows the rule and the test prevents recurrence", not a patch to the old screen.

Citation aliases (UIA §0; DES-01 Appendix A), relative to `app/src/main/java/com/example/`: W = `ui/components/DashboardStudioWidgets.kt`, D = `ui/screens/DashboardScreen.kt`, CC = `ui/components/CoachComponents.kt`, AO = `ui/screens/AuthAndOnboardingScreens.kt`, AS = `ui/screens/AnalyticsAndSettingsScreens.kt`, ND = `ui/components/NotificationDrawer.kt`, TO = `ui/components/TutorialOverlay.kt`, LAT = `ui/screens/LabsAndTrainingScreens.kt`, LM = `ui/screens/LessonAndModulesScreens.kt`, ETR = `ui/components/ExerciseTemplateRenderers.kt`, CM = `ui/screens/ConversationAndMentorScreens.kt`, VO = `ui/screens/VocabularyScreen.kt`, P = `ui/screens/UserProfileScreen.kt`, NAV = `ui/AppNavigation.kt`, PR = `data/preferences/AppPreferencesRepository.kt`, UD = `ui/components/UpdateAvailableDialog.kt`, LA = `ui/components/LaunchAnimationOverlay.kt`, SAC = `ui/screens/SpeakingAndChallengesScreens.kt`, SVM = `ui/viewmodel/SpeakingAndLabsViewModel.kt`, MAN = `app/src/main/AndroidManifest.xml`.

| # | Current defect | Evidence | DES-01 | Rule(s) | Remediation in the redesign | Verified by |
|---|---|---|---|---|---|---|
| REM-01 | Icons repeat adjacent text, so TalkBack reads it twice | D:1013; W:1362; CC:1064/1069, 1257, 1428, 1911, 1935 [V] | UXF-43 | A11Y-03 | Icons beside matching text have `null` descriptions (DES-05 §7) | TC-A11Y-009 |
| REM-02 | State-only icons with no text: plan circle; weekly dots "Day completed" with no day name | W:623-653; W:896, 968 [V] | UXF-43 | A11Y-02, A11Y-07, A11Y-23 | Week strip as one merged summary with day names (DES-05 §11.17) | TC-A11Y-013 |
| REM-03 | Only 6 `heading()` uses; none on dashboard card titles, settings headers, drawer title, phone lab lists | AS:387; ND:205; LAT:307, 512, 700, 887 [V] | UXF-43 | A11Y-04 | Design-system `SectionHeader` keeps `heading()` (DES-01 K-10) and is used for every section | TC-A11Y-005 |
| REM-04 | Only 2 `Role.` uses; clickable rows, boxes and surfaces have no role | W:614, 468, 862, 1352, 1471; D:994; P:591 [V] | UXF-43 | A11Y-05, A11Y-07 | `Card(onClick)`, `ListItem`, roles on every custom clickable | TC-A11Y-006, 025 |
| REM-05 | Checklist rows without checkbox semantics | ETR:400-417 [V] | UXF-43 | A11Y-05, A11Y-06, A11Y-07 | Checkbox rows per DES-05 §11.22 | TC-A11Y-006 |
| REM-06 | Selected cards show selection by colour only | LAT:283, 310, 488 [V] | UXF-43 | A11Y-06, A11Y-23, A11Y-24 | `borderSelected` + check icon + selected semantics | TC-A11Y-006, 022 |
| REM-07 | FilterChips used as action buttons (announced as toggles) | CM:724-726 [V/I] | UXF-40 | A11Y-05 | Buttons for actions; chips only filter (DES-05 §11.9) | TC-A11Y-025; design review |
| REM-08 | Settings and drawer switches not merged with labels; TalkBack says only "Switch" | AS:624-645; ND:740-749 [V] | UXF-43 | A11Y-07, A11Y-05, A11Y-06 | Whole-row `toggleable(role = Role.Switch)` (DES-05 §11.22) | TC-A11Y-006 |
| REM-09 | Sliders without label semantics | AO:660; AS:548-553; LM:570-571; ETR:634 [V] | UXF-43 | A11Y-06, A11Y-25 | Onboarding self-rating sliders removed (DES-01 UXF-03); any remaining rate slider labelled with value in words | TC-A11Y-019 |
| REM-10 | Loading shimmer announces nothing | CC:862-868 [V] | UXF-43, UXF-49 | A11Y-33 | Static skeleton; "Loading" announced once after 300 ms (DES-05 §11.19) | TC-A11Y-012 |
| REM-11 | No live regions for streaming chat or errors | CM [V/I] | UXF-43 | A11Y-08, A11Y-32 | Announcement policy §3.3; chat screens retired (RD-16) — rule prevents recurrence | TC-A11Y-012, 037 |
| REM-12 | Undersized targets: carousel dots 7×7 / 20×7 dp, tutorial dots 8 dp, drawer pill about 26 dp, ring links about 22–26 dp | W:467; TO:556-566; ND:563-588; W:859-863 [V sizes; I heights] | UXF-44 | A11Y-19, A11Y-20 | 48 dp targets; pagers get Next/Back buttons | TC-A11Y-008 |
| REM-13 | 24 dp completion check nested inside a tappable plan row | W:614, 625, 639 [V] | UXF-24, UXF-40, UXF-44 | A11Y-18, A11Y-19 | Self-reported ticks removed (completion = valid attempt, SRS-BR-009); one target per row | TC-A11Y-008 |
| REM-14 | `minTouchTarget` token used 0 times in the admin dashboard | UIA §6 [V] | UXF-44 | A11Y-19 | Admin console descoped (RD-16 NG-10); M3 minimum size kept on everywhere | TC-A11Y-008 |
| REM-15 | Fixed-height text containers: hero 252 dp, flashcard 270 dp, score ring 72 dp, Today ring 96 dp, waveform 78 dp with badge, video box 210 dp, avatar 64 dp with initials; no font-scale test exists | W:282; VO:241; CC:1505-1537; W:805-844; CC:322; CC:553; P:207-216 [V code; I clipping]; UIA §10 [V] | UXF-42 | A11Y-13, A11Y-14 | `heightIn(min = …)`; level meter holds no text (DES-05 §11.12); score rings retired | TC-A11Y-007, 023 |
| REM-16 | Reduced-motion setting read once, not reactive | CC:111-124 [V] | UXF-45 | A11Y-21 | Content observer + `LocalReducedMotion` (DES-05 §8.3) | TC-A11Y-033 |
| REM-17 | Reduced motion ignored by dashboard, drawer, score ring, transcript autoscroll, thinking dots, flashcard, onboarding | NAV:227 vs D:160; ND:126-155; CC:1371-1375; CC:1029; CC:930; VO:249; AO:422 [V] | UXF-45 | A11Y-21, A11Y-22 | Every animation helper reads `LocalReducedMotion` | TC-A11Y-010 |
| REM-18 | In-app `reduced_motion` preference exists but has no UI and is never read | PR:39, 119, 247-251 [V] | UXF-45 | A11Y-21 | Appearance → Reduce motion (SRS-FR-SET-007) | TC-A11Y-033 |
| REM-19 | TutorialOverlay and NotificationDrawer drawn in-window with no focus trap | TO; ND:96 [V code; I focus behaviour] | UXF-40, UXF-14 | A11Y-10 | Tutorials replaced by inline tips (DES-06 §2.4); inbox is a screen (R2) | TC-A11Y-015 |
| REM-20 | Launch overlay: unskippable 2.1–2.9 s; no pointer block, so its semantics and touches reach the UI underneath | LA:71-75, 251-658 [V]; pass-through [I] | UXF-02 | A11Y-11, A11Y-21 | Platform splash only, ≤ 1 s, reduced-motion aware (DES-05 §10; NFR-PERF-002) | TC-A11Y-016, 025 |
| REM-21 | Mandatory update dialog ignores Back and outside taps | UD:75-77 [V] | UXF-11 | A11Y-10 | No non-dismissable dialogs (DES-05 §11.5); updater disposition per D-03 | TC-A11Y-015 |
| REM-22 | 0 `imePadding` in `ui/`; chat lists do not scroll to the newest message | UIA §6 [V] | — | A11Y-30 | `imePadding()` and bring-into-view (DES-05 §5.4) | TC-A11Y-029 |
| REM-23 | About 386 hardcoded English strings, 4 string resources | UIA §5.1 [V] | UXF-47 | A11Y-36 | All strings in resources; G-6 guardrail | TC-A11Y-030 |
| REM-24 | `String.format` without a Locale | D:198; P:418 [V] | UXF-47 | A11Y-37 | Locale-aware formatters | TC-A11Y-031 |
| REM-25 | `supportsRtl="true"` but strings hardcoded and mirroring never checked | MAN:24 [V]; mirroring [I] | UXF-47 | A11Y-38 | AutoMirrored icons, start/end layout, pseudolocale checks | TC-A11Y-032 |
| REM-26 | Current colour tokens fail contrast (light `outline` 1.24:1, `warning` 2.90:1, `success` 3.25:1, `secondary` 3.76:1, `error` 4.32:1, pastel pairs 2.48–3.81:1; dark `outline` 1.36:1) | DES-01 UXF-41 [V, computed] | UXF-41 | A11Y-15, A11Y-16, A11Y-17 | New palette, every pair computed in DES-05 §3.4 (D-18) | TC-A11Y-001, 024, 028 |
| REM-27 | Monospace `labelSmall` in 103 places; synthesised bold; undefined display styles | Type.kt:9-25, 98-104 [V] | UXF-46 | — (readability, no WCAG-level rule) | One family with real weights (DES-05 §4.1); CI guardrail G-8; reviewed in checklist C-12 | DES-05 G-8 |
| REM-28 | System-bar icons follow the system night mode, not the in-app theme; splash-to-canvas colour jump | MainActivity.kt:17; colors.xml [V]; visual effect [I] | UXF-48 | A11Y-16, A11Y-17 | Theme-aware system-bar style; window colour = `surface` (DES-05 §10) | TC-A11Y-024 |
| REM-29 | Microphone permission denial is silent on 7 recorders | SAC:150-154, 835-837; LAT:103-105, 353-355, 556-558, 744-746; LM:550-552 [V] | UXF-36 | A11Y-32 | Denied / permanently denied cards with "Open settings", focus and announcement (DES-05 §11.19) | TC-A11Y-036 |
| REM-30 | Recording controls are inconsistent and untruthful: 12 start labels, only 3 show a mic icon; after navigation the next lab shows "Stop" with a live waveform; result shown as the raw enum "INSUFFICIENT_EVIDENCE" | UIA §7.2; SVM:594-613, 466; CC:1268 [V] | UXF-16, UXF-37, UXF-35 | A11Y-26, A11Y-08, A11Y-32 | One Record button with truthful states (DES-05 §11.12); Inconclusive card with a specific reason (§11.16) | TC-A11Y-003, 012, 037 |
| REM-31 | Buttons stay enabled while loading (duplicate submissions); an onboarding failure is invisible | SAC:508, 960; LAT:197, 419, 608, 795 [V]; AO:599-623 [V], invisible error [I] | UXF-39, UXF-28 | A11Y-06, A11Y-32, A11Y-33 | Loading button reads "Loading" and is not clickable (DES-05 §11.1); inline, announced errors | TC-A11Y-017, 018 |
| REM-32 | Processing can spin forever offline; Loading and Error look like Empty | SVM:509-535 [V code; I behaviour]; NAV:513-521 [V] | UXF-50, UXF-49 | A11Y-29, A11Y-32, A11Y-33 | 10 s "taking longer" + Cancel (DES-05 §11.11); distinct, announced states (§11.19) | TC-A11Y-034, 012 |

---

## 17. Open questions (Product Owner)

| ID | Question | Options / context | Recommendation (not a decision) | Affects |
|---|---|---|---|---|
| **OQ-A11Y-1** | **Streak and step rule for users who cannot complete a speaking step.** | (a) Keep current rules: only valid speaking attempts complete a step or a streak day (SRS-BR-008/009/010); Skip is always available but earns nothing. (b) A skipped speaking step counts toward session completion but not the streak. (c) An opt-in accessibility accommodation lets a non-speaking alternative (for example a Words review or Learn stage) count for plan progress, and optionally for the streak. (d) A user-initiated streak pause for a limited period. | No recommendation on the rule itself: it trades PP-4 (speaking is the core task) and PP-7 (honest streak) against inclusion. Request input from users with speech disabilities first (OQ-A11Y-5). Whatever is chosen must be stated in plain words on the Inconclusive card and in Progress. | SRS-BR-008…010, FS-03, FS-08, DES-05 §11.16, DES-01 HRC-09 |
| OQ-A11Y-2 | Screen-reader speech captured in recordings, and Voice Access microphone sharing | TalkBack's start announcement and the "10 seconds left" announcement can be recorded through the speaker (INFERRED). Options: (a) headphones tip only; (b) when touch exploration is on, delay capture until the start cue ends — this conflicts with NFR-PERF-004 (≤ 300 ms start latency) and needs a documented exception; (c) during capture, replace the near-limit speech with a haptic and visual cue; (d) combinations. Voice Access behaviour during capture is unknown. | (a) + (b) + (c), with the delay measured on TST-01 §5 devices in Stage B (TC-A11Y-026/027) before a value is proposed | A11Y-27, A11Y-29, NFR-PERF-004, FS-04 |
| OQ-A11Y-3 | Users who stammer or have atypical speech | Pace, long pauses and filler rate may be discouraging or misleading for these users even when measured correctly. Options: a preference to hide selected delivery metrics; neutral framing only; no change. | Decide after OQ-A11Y-5 research; keep neutral wording now (DES-05 §11.13) | FS-05, ARC-05, DES-05 §11.13 |
| OQ-A11Y-4 | NFR gaps | NFR §6 has no IDs for keyboard / switch access, status messages, time limits, focus not obscured, locale formatting or RTL. DES-07 rules A11Y-29…31, A11Y-33, A11Y-37, A11Y-38 elaborate the nearest existing ID or none. | NFR owner adds measurable IDs for these areas (this document does not invent NFR IDs) | NFR §6, GOV-01 |
| OQ-A11Y-5 | Research with disabled users | No research data exists (DES-01 L-3). | Fund moderated sessions with TalkBack users, low-vision users, motor-impaired users and people with speech disabilities on the R1 prototype, alongside OQ-UX-1 | DES-01 OQ-UX-1, PLN-01 |
| OQ-A11Y-6 | Automated accessibility-check dependency | ATF via `espresso-accessibility`, or Compose's built-in checks after a BOM upgrade (§14.3). | Defer; use the in-stack semantics assertions in R0/R1; revisit through an ADR if manual findings show gaps | NFR-MAINT-007, ENG-01 |
| OQ-A11Y-7 | Listen & repeat sentence visibility | If the template hides the sentence to train listening, deaf and hard-of-hearing users cannot do the task. | Sentence visible by default; optional "Hide text while listening" for users who want the challenge | FS-04, A11Y-28 |

---

## 18. Traceability

### 18.1 Requirements → rules → tests

| Requirement | Rules | Tests |
|---|---|---|
| NFR-A11Y-001 Screen readers | A11Y-02…A11Y-12, A11Y-25…A11Y-35 | TC-A11Y-002…006, 009, 011…021, 025…027, 029, 034…038 |
| NFR-A11Y-002 Font scaling | A11Y-13, A11Y-14, A11Y-30 | TC-A11Y-007, 023, 029, 032 |
| NFR-A11Y-003 Contrast | A11Y-15, A11Y-16, A11Y-17 | TC-A11Y-001, 024, 028 |
| NFR-A11Y-004 Touch targets | A11Y-18, A11Y-19, A11Y-20, A11Y-25 | TC-A11Y-008, 019, 028 |
| NFR-A11Y-005 Reduced motion | A11Y-21, A11Y-22 | TC-A11Y-010, 033, 038 |
| NFR-A11Y-006 Colour independence | A11Y-12, A11Y-23, A11Y-24, A11Y-32 | TC-A11Y-002, 013, 014, 018, 022 |
| NFR-MAINT-008 Strings | A11Y-36 | TC-A11Y-030, 031 |
| NFR-PRIV-001 Recording state visible | A11Y-26 | TC-A11Y-003, 026 |
| SRS-FR-SESS-002/003/004 Preparation, limit, recording announcements | A11Y-08, A11Y-26, A11Y-27, A11Y-29 | TC-A11Y-003, 026, 034 |
| SRS-FR-SESS-006 Transcript with tier label | A11Y-28 | TC-A11Y-021 |
| SRS-FR-SESS-012 Reference audio 1.0× / 0.75× | A11Y-25 | TC-A11Y-019 |
| SRS-FR-SET-007 Appearance (Reduce motion, system font scale) | A11Y-13, A11Y-21 | TC-A11Y-007, 033 |
| PRD-NFR-006; RD-15 UX-9 | A11Y-01 | TC-A11Y-039 |

### 18.2 Citations made by DES-05 and DES-01 (fixed meanings, R-3)

| Cited in | Citation | Defined here as | Match |
|---|---|---|---|
| DES-05 §7 | A11Y-02/03 contentDescription rules | A11Y-02 meaningful names; A11Y-03 decorative/duplicate silent (§3.1) | Yes |
| DES-05 §11.12 | A11Y-06/08/26 record-button semantics | A11Y-06 state descriptions; A11Y-08 live regions; A11Y-26 record-button states | Yes |
| DES-05 §11.3, §11.13 | A11Y-07 merged phrase | A11Y-07 merged semantics for cards, tiles, rows | Yes |
| DES-05 §11.15 | A11Y-09 focus to result header | A11Y-09 | Yes |
| DES-01 UXF-40 | A11Y-10 real Dialog / ModalBottomSheet | A11Y-10 | Yes |
| DES-05 §11.18 CH-10 | A11Y-12 chart summary + list | A11Y-12 | Yes |
| DES-01 UXF-42 | A11Y-13 200 %, no fixed-height text containers | A11Y-13 | Yes |
| DES-01 UXF-44, K-9 | A11Y-19 48 dp | A11Y-19 | Yes |
| DES-01 K-5 | A11Y-20 pager dots 48 dp + button alternatives | A11Y-20 | Yes |
| DES-01 UXF-45 | A11Y-21 reduced motion (system + in-app) | A11Y-21 | Yes |
| DES-05 §3.5, §11.14, §11.18 | A11Y-23 colour never the only carrier | A11Y-23 | Yes |
| DES-01 K-15 | A11Y-25 labelled speed control | A11Y-25 | Yes |
| DES-05 §5.4 | A11Y-30 focus not hidden by sticky bars | A11Y-30 | Yes |
| DES-05 §11.2 | A11Y-32 errors announced with fix | A11Y-32 | Yes |
| DES-05 §11.13 | A11Y-34 units spelled out | A11Y-34 | Yes |
| DES-05 §12.4, §13.4 G-6; DES-01 UXF-47 | A11Y-36 strings in resources, plurals | A11Y-36 | Yes |
| DES-01 UXF-47 | A11Y-37 locale-aware formatting | A11Y-37 | Yes |
| DES-01 UXF-43 | A11Y-02…12 in §3 | §3 holds A11Y-02…A11Y-12 | Yes |
| DES-05 §3.1, §3.4, §13.2 | TC-A11Y-001 token contrast (JVM) | TC-A11Y-001 (Unit) | Yes |
| DES-05 §13.2 | TC-A11Y-002…006, 009 component semantics (badge, record button, tile, headings) | 002 badge, 003 record button, 004 tile, 005 headings, 006 selection rows, 009 icon names | Yes |
| DES-05 §4.4, §13.2 | TC-A11Y-007 200 % font no overflow | TC-A11Y-007 | Yes |
| DES-05 §13.2 | TC-A11Y-008 touch targets | TC-A11Y-008 | Yes |
| DES-05 §13.2 | TC-A11Y-010 reduced motion (`LocalReducedMotion`) | TC-A11Y-010 | Yes |
| DES-05 §11.10 | TC-A11Y-020 snackbar duration and announcement | TC-A11Y-020 | Yes |
| DES-05 §4.4 FS-9 | TC-A11Y-023 font 1.0/1.3/1.5/2.0 on 360 × 640 dp | TC-A11Y-023 | Yes |
| DES-05 §10 | TC-A11Y-024 both themes | TC-A11Y-024 | Yes |
| DES-05 §11.16; DES-01 HRC-09 | OQ-A11Y-1 streak/step rule | OQ-A11Y-1 (§17) | Yes |
| DES-01 K-14 | DES-07 §15 test cases | §15 | Yes |
| DES-01 §7 | DES-07 remediation list (§16) | §16 | Yes |

---

## 19. Approval and change log

| Role | Responsibility | Status |
|---|---|---|
| Product Owner | Approve rules A11Y-01…A11Y-38 and decide OQ-A11Y-1…7 | Pending |
| Design System Architect | Confirm alignment with DES-05 | Pending |
| QA Architect | Adopt §14–§15 in TST-01 | Pending |
| Android Tech Lead | Confirm Compose feasibility of §3.2 and §14.1 | Pending |

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-21 | Initial draft: conformance target, 38 rules, announcement policy, checklist, tooling, 39 test cases, remediation list, open questions | UI/UX Designer / QA |
