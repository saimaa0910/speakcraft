# Screen Inventory — SpeakCraft (Proposed)

| Field | Value |
|---|---|
| Document ID | DES-03 |
| Version | 0.2 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UI/UX Designer |
| Approval | Pending — Product Owner |
| Related | DES-02 Information Architecture, DES-04 User Flows, DES-05 Design System, DES-06 UI/UX Specifications, DES-07 Accessibility Guidelines, DES-08 Redesign Decisions, DES-01 UX Audit, FS-01…FS-12, PRD, SRS, ARC-05 |

> **Authority (GOV-02 §4).** DES-02 §4 is the authoritative list of screen IDs. This document elaborates **every** screen in that list (DES-02 v0.2: 42 IDs — 38 R1, 4 R2) and uses no other ID. Workflow and per-feature UI states are owned by the feature specifications (FS-nn); where an FS is still being written (FS-03…FS-12), this document gives the screen-level state set the FS must cover. Layout, spacing and copy detail belong to DES-06; accessibility rules to DES-07; the rationale for each change to DES-08. Everything here is a **RECOMMENDATION** until the Product Owner approves it.

---

## 1. Purpose and scope

This inventory tells designers, engineers and testers, for each screen:

1. why it exists, who it is mainly for, and how users arrive;
2. what it shows, from top to bottom, and which DES-05 components build it;
3. its single primary action and its secondary actions;
4. its loading, empty, error, permission and offline states (UX-7; DES-01 UXF-49…52);
5. how Back, Up and the tab bar behave;
6. which feature specification owns it and which SRS requirements it serves.

It then gives a consolidated state matrix (§5), a component inventory (§6), the mapping from today's routes to their replacement or removal (§7) and the issues found while writing it (§8).

Out of scope: pixel layout, motion detail and final copy (DES-06), Compose implementation, and flows across screens (DES-04).

## 2. Conventions

### 2.1 Personas

P1 Aarav (professional, B2), P2 Meera (job seeker, B1+), P3 Daniel (student, B1), P4 Sofia (advanced, C1), P5 Hoang (elementary, A2+) — PRD §7. They are hypotheses, not research findings (ASM-01).

### 2.2 Presentation types

| Type | Meaning | Bottom bar / rail |
|---|---|---|
| **Tab root** | One of the four primary destinations (DES-02 §2.1) | Visible |
| **Detail** | Pushed onto the current tab's back stack; top bar has a Back arrow | Visible; the owning tab stays highlighted (DES-05 §11.7) |
| **Full-screen task** | A task with a clear start and end (session player, onboarding, sign-in, permission education, stage flows, destructive flows) | Hidden (DES-02 §2.3 rule 2) |
| **Sheet** | `ModalBottomSheet` over the current screen | Inherits the host |

### 2.3 Global navigation rules (apply to every screen unless the screen says otherwise)

| Rule | Behaviour | Source |
|---|---|---|
| N-1 | System Back pops one level. From a tab root it goes to Today; from Today it exits the app. | DES-02 §2.3 |
| N-2 | **Up** is the top-bar Back arrow on Detail screens. It behaves like system Back except when the screen was opened from outside the tab (for example a notification); then Up goes to the tab root. | RECOMMENDATION |
| N-3 | Re-selecting the current tab pops to the tab root and scrolls to the top. Each tab keeps its own back stack. | DES-02 §2.1 |
| N-4 | Full-screen tasks offer a visible Close. Leaving asks for confirmation **only** if a recording or typed input would be lost. | DES-01 HRC-08; SRS-FR-SESS-011 |
| N-5 | Profile (SCR-PROF-01) and Settings (SCR-SET-*) are pushed as Detail screens onto the **current** tab's stack (see §8 issue I-13). | RECOMMENDATION |

### 2.4 Global state rules

| State | Default behaviour (DES-05 §11.19) |
|---|---|
| Loading | Nothing for the first 300 ms, then a static skeleton that matches the layout. Spinners only in buttons and processing. |
| Empty | 48 dp icon, title, one sentence, one action. Never zeros or seeded values dressed as data (HRC-31, HRC-32). |
| Error | Inline in place of the content: what happened + what to do + reassurance about data. Keeps user input. Never raw exception text (SRS §15). |
| Permission | Microphone: education → system prompt → inline card → "Open settings" when permanently denied; re-checked on resume. Notifications: asked only when the user turns a reminder on. |
| Offline | Core practice works offline (PP-6). An offline row appears **only** where a feature needs the network: sign-in, sync status, speech-pack download, online transcription (T3a), account deletion. |

"N/A" in a state row always gives the reason.

### 2.5 Component names

Components are named as in DES-05 §11, shortened as follows. Anything not in DES-05 is named as the Material 3 component.

| Short name | DES-05 | Short name | DES-05 |
|---|---|---|---|
| Filled button / Large button / Tonal button / Outlined button / Text button / Icon button | §11.1 | Record button (recording panel: status line, level meter, timer) | §11.12 |
| Text field | §11.2 | Metric tile | §11.13 |
| Card / Outlined card (session card, track card, activity card) | §11.3 | Evidence badge (Measured / Estimated / Inconclusive) | §11.14 |
| List item | §11.4 | Feedback card | §11.15 |
| Dialog | §11.5 | Inconclusive card | §11.16 |
| Bottom sheet | §11.6 | Streak counter / Streak week strip / Streak month grid | §11.17 |
| Navigation bar / rail | §11.7 | Line chart / Bar chart | §11.18 |
| Top app bar (primary / detail / full-screen) | §11.8 | Empty state / Error state / Offline row / Mic permission card / Notification permission row / Capability notice / Skeleton | §11.19 |
| Filter chip / Segmented button / Assist chip | §11.9 | Prompt card / Transcript view | §11.20 |
| Snackbar | §11.10 | Word card / Stage stepper | §11.21 |
| Step progress / Linear progress / Processing indicator / Button loading | §11.11 | Switch row / Radio group | §11.22 |
| Section header | §13.1 | Avatar (initials, in the top app bar) | §11.8 |
| **MistakeList** — "Mistakes found": up to 3 entries (issue, evidence, Measured / Estimated badge), "Show all", honest empty text | §11.15a (DES-05 v0.2; I-23 resolved) | **CoachTipCard** — one tip with its trigger in plain words, "See technique" link to SCR-PRAC-04 and optional "Watch the original lesson" | §11.15b (DES-05 v0.2; I-23 resolved) |
| **SelfCheckList** — self-reported checks for technique aspects audio cannot show | §11.15c (DES-05 v0.2) | | |

### 2.6 Primary action rule

Every **task** screen has exactly one primary (filled) action (DES-05 DS-2, §11.1). Browse and settings screens have no single task; their primary action is the main selection (a card or row) and they show no filled button unless a state introduces a task (see §8 issue I-4). On the session player, exactly one primary action exists **per state**.

---

## 3. Screen summary

| ID | Screen | Rel. | Type | Owning FS | Main personas | Primary action |
|---|---|---|---|---|---|---|
| SCR-ONB-01 | Welcome / value | R1 | Full-screen task | FS-02 | All; Meera, Hoang | Get started |
| SCR-ONB-02 | Goal (track) | R1 | Full-screen task | FS-02 | All | Continue |
| SCR-ONB-03 | Level | R1 | Full-screen task | FS-02 | Hoang, Sofia | Continue |
| SCR-ONB-04 | Daily time | R1 | Full-screen task | FS-02 | Aarav, Daniel | Continue |
| SCR-ONB-05 | Reminder | R1 | Full-screen task | FS-02 (FS-09) | Daniel, Aarav | Remind me at {time} |
| SCR-ONB-06 | Privacy & microphone | R1 | Full-screen task | FS-02 | Meera, Sofia | Allow microphone |
| SCR-ONB-07 | Speech check | R1 | Full-screen task | FS-02 (FS-12) | Hoang, Meera | Continue |
| SCR-ONB-08 | First attempt intro | R1 | Full-screen task | FS-02 | All | Start |
| SCR-TODAY-01 | Today | R1 | Tab root | FS-03 | Aarav, Daniel, Hoang | Start session |
| SCR-TODAY-02 | Session complete | R1 | Full-screen task | FS-03 (FS-08) | Aarav, Daniel, Hoang | Done |
| SCR-SESS-01 | Session player | R1 | Full-screen task | FS-04 (FS-05, FS-12) | All | Per state: Record · Stop · Continue / Try again |
| SCR-SESS-02 | Feedback detail sheet | R1 | Sheet | FS-05 | Sofia, Meera | Close |
| SCR-PRAC-01 | Practice home (tracks; Techniques collection) | R1 | Tab root | FS-06 | Meera, Daniel, Aarav, Sofia | Open a track / Open a technique |
| SCR-PRAC-02 | Track (activity list, filters, saved) | R1 | Detail | FS-06 | Meera, Daniel, Aarav | Open an activity |
| SCR-PRAC-03 | Activity detail | R1 | Detail | FS-06 | Meera, Daniel, Aarav | Start |
| SCR-PRAC-04 | Technique detail | R1 | Detail | FS-06 | Sofia, Meera, Daniel | Practise it |
| SCR-WORD-01 | Words home | R1 | Tab root | FS-07 | Daniel, Meera, Hoang | Start review / Learn next word |
| SCR-WORD-02 | Word detail | R1 | Detail | FS-07 | Daniel, Hoang | Start {next stage} |
| SCR-WORD-03 | Word stage flow | R1 | Full-screen task | FS-07 | Daniel, Meera, Hoang | Per stage |
| SCR-WORD-04 | Review session | R1 | Full-screen task | FS-07 | Daniel, Aarav | Start review / Done |
| SCR-WORD-05 | Notebook | R2 | Detail | FS-07 | Sofia, Daniel | Open an entry |
| SCR-WORD-06 | Quiz | R2 | Full-screen task | FS-07 | Daniel, Hoang | Check |
| SCR-PROG-01 | Progress overview | R1 | Tab root | FS-08 | Sofia, Aarav | Open a trend |
| SCR-PROG-02 | Metric detail (trend) | R1 | Detail | FS-08 | Sofia | Open an attempt |
| SCR-PROG-03 | History | R1 | Detail | FS-08 | Sofia, Meera | Open an attempt |
| SCR-PROG-04 | Attempt detail | R1 | Detail | FS-08 (FS-04, FS-05) | Meera, Sofia | Try again |
| SCR-PROG-05 | Check-ins | R2 | Detail | FS-08 | Sofia, Aarav | Start check-in |
| SCR-PROF-01 | Profile | R1 | Detail | FS-10 | All | Edit profile |
| SCR-SET-01 | Settings home | R1 | Detail | FS-10 | All | Open a section |
| SCR-SET-02 | Learning preferences | R1 | Detail | FS-10 | Aarav, Hoang | Change a preference |
| SCR-SET-03 | Reminders | R1 | Detail | FS-09 | Daniel, Aarav | Daily reminder switch |
| SCR-SET-04 | Speech & privacy | R1 | Detail | FS-10 (FS-12) | Meera, Hoang | Install speech pack (when offered) |
| SCR-SET-05 | Appearance | R1 | Detail | FS-10 | Sofia, Hoang | Change a preference |
| SCR-SET-06 | Data & privacy | R1 | Detail | FS-10 | Meera, Sofia | Export my data |
| SCR-SET-07 | Account | R1 | Detail | FS-01 (FS-11) | Aarav, Daniel | Sign in with Google (local) |
| SCR-SET-08 | About | R1 | Detail | FS-10 | All | Open an item |
| SCR-SET-09 | Privacy information | R1 | Detail | FS-10 | Meera, Sofia | — (read) · Open privacy policy |
| SCR-ACC-01 | Sign in | R1 | Full-screen task | FS-01 | Aarav, Daniel | Continue with Google |
| SCR-ACC-02 | Delete account | R1 | Full-screen task | FS-01 | Meera, Sofia | Per step: Continue · Delete my account · Continue |
| SCR-PERM-01 | Permission education | R1 | Full-screen task | FS-04 (mic), FS-09 (notifications) | Hoang, Meera | Allow microphone / Allow notifications |
| SCR-INBOX-01 | Inbox | R2 | Detail (from Today) | FS-09 | Daniel | Open a message |
| SCR-SYS-01 | Account-deletion resume | R1 | Full-screen task (blocking) | FS-01 | — (any deleting user) | Retry (when needed) |

**Counting note (KPI-11 ≤ 30 screens).** The 38 R1 IDs use 16 layout families: explainer (ONB-01, ONB-06, ONB-07, ONB-08, PERM-01, ACC-01, SYS-01); single choice (ONB-02…05); Today; completion summary (TODAY-02); session player; detail sheet (SESS-02); card list (PRAC-01, PRAC-02, WORD-01, PROG-03); detail page (PRAC-03, PRAC-04, WORD-02, PROG-04); stage flow (WORD-03); review intro/summary (WORD-04); progress overview; chart detail (PROG-02); profile; settings list (SET-01…08); long text (SET-09); multi-step destructive flow (ACC-02). See issue I-2.

---

## 4. Screen details

### 4.1 Onboarding (full-screen tasks; owning FS-02)

Onboarding runs once, before Today (DES-02 §3 gate). Every step persists its choice before moving on, so it resumes after process death (SRS-FR-ONB-010). Step progress covers SCR-ONB-02…08 ("Step n of 7"; see issue I-15).

#### SCR-ONB-01 — Welcome / value

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | State the product promise in three lines and start set-up **without an account** (UXF-01, UXF-03). |
| Primary persona(s) | All; especially Meera (wants to try before signing up) and Hoang (needs plain, short text). |
| Entry points | First launch after the brand moment (≤ 1 s, DES-06 §2.3) when onboarding is not started · after sign-out with *Remove*, account deletion *Done*, or *Delete everything on this device* (a new local profile starts onboarding, FS-01 §5.3–5.5). |
| Main content (top → bottom) | (1) App name. No creator name, likeness or branding (D-08 decided 2026-11-21; SRS-BR-028). (2) Headline, e.g. "Practise speaking English, privately." (3) Three value lines with decorative icons: short daily sessions; see what you said and one thing to try next; recordings stay on this phone (copy generated from policy constants for the privacy line, SRS-PRIV-002). (4) Bottom action area: Get started; "Already use SpeakCraft? Sign in". |
| Components | Large button · Text button · List item ×3 (decorative icons, `contentDescription = null`) · no top app bar, no step progress |
| Primary action | **Get started** → SCR-ONB-02 |
| Secondary actions | "Already use SpeakCraft? Sign in" → SCR-ACC-01 (RECOMMENDATION from FS-02 A8; pending FS-01 Q4; see I-7). On return, imported choices pre-fill SCR-ONB-02…05; steps 06–07 always run. |
| Loading | N/A — static content; the start destination is decided before the first frame (FS-02 §4). |
| Empty | N/A — static content. |
| Error | Local profile could not be created (SRS-ERR-005): Error state "Something went wrong on this phone. Try again." + Try again. |
| Permission | N/A — no permission is requested here. |
| Offline | Identical to default. Sign-in shows its own offline message on SCR-ACC-01 (FS-01 E1). |
| Navigation | Back: exits the app (FS-02 §5). Up: none. Tab: bar hidden. |
| Owning FS | FS-02 (sign-in entry: FS-01) |
| Requirements | SRS-FR-ONB-001, SRS-FR-ONB-010, SRS-FR-ACC-001, SRS-FR-ACC-002 |

#### SCR-ONB-02 — Goal (track)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Choose what the user practises for; the goal maps to one of the five tracks and drives Today (UX-5). |
| Primary persona(s) | Aarav (Workplace communication), Meera (Job interviews), Daniel (Presentations & storytelling), Hoang (Everyday conversation, Clear speech). |
| Entry points | SCR-ONB-01 Get started · Back from SCR-ONB-03 · resume after process death. |
| Main content (top → bottom) | (1) Top bar with Back and "Step 1 of 7". (2) Question "What do you want to practise for?" (3) Five options, each with a one-line description: Everyday conversation · Workplace communication · Job interviews · Presentations & storytelling · Clear speech (SRS-FR-ONB-002). A default is pre-selected (pending FS-02 Q1). (4) "You can change this later in Settings." (5) Sticky Continue. |
| Components | Top app bar (full-screen, Back) · Step progress · Radio group (full-row targets) · Large button |
| Primary action | **Continue** → SCR-ONB-03 |
| Secondary actions | Back |
| Loading | N/A — options are static; local write < 50 ms (NFR-PERF-008). |
| Empty | N/A — options are static. |
| Error | Saving the choice fails (FS-02 E5): inline Error state + Try again; the screen does not advance. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-ONB-01, choice kept. Tab: hidden. |
| Owning FS | FS-02 |
| Requirements | SRS-FR-ONB-001, -002, -010; SRS-BR-025 |

#### SCR-ONB-03 — Level

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Self-select one of five levels (D-17) so activities and words fit the user. |
| Primary persona(s) | Hoang (Elementary) and Sofia (Advanced) — the two ends of the range. |
| Entry points | SCR-ONB-02 Continue · Back from SCR-ONB-04 · resume. |
| Main content (top → bottom) | (1) Top bar, "Step 2 of 7". (2) Question "How would you describe your spoken English?" (3) Five options with plain names and a "can do" line (FS-02 §5.2); Intermediate pre-selected (SRS-FR-ONB-003). No CEFR codes (OQ-DS-7). (4) "This only sets your starting point." (5) Continue. |
| Components | Top app bar (full-screen) · Step progress · Radio group · Large button |
| Primary action | **Continue** → SCR-ONB-04 |
| Secondary actions | Back |
| Loading | N/A — static. |
| Empty | N/A — static. |
| Error | As SCR-ONB-02. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-ONB-02. Tab: hidden. |
| Owning FS | FS-02 |
| Requirements | SRS-FR-ONB-001, -003, -010; SRS-BR-025 |

#### SCR-ONB-04 — Daily time

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Choose the length of the daily session (5, 10 or 15 minutes). |
| Primary persona(s) | Aarav (10 minutes before stand-up), Daniel. |
| Entry points | SCR-ONB-03 Continue · Back from SCR-ONB-05 · resume. |
| Main content (top → bottom) | (1) Top bar, "Step 3 of 7". (2) Question "How much time do you have each day?" (3) Options 5 · 10 (pre-selected) · 15 minutes, each with one line on what the session contains (wording from the FS-03 duration table). (4) Continue. |
| Components | Top app bar (full-screen) · Step progress · Radio group · Large button |
| Primary action | **Continue** → SCR-ONB-05 |
| Secondary actions | Back |
| Loading | N/A — static. |
| Empty | N/A — static. |
| Error | As SCR-ONB-02. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-ONB-03. Tab: hidden. |
| Owning FS | FS-02 |
| Requirements | SRS-FR-ONB-001, -004, -010 |

#### SCR-ONB-05 — Reminder

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Offer one daily reminder at a chosen time, only by the user's choice (UN-07). |
| Primary persona(s) | Daniel (builds a habit around lectures), Aarav. |
| Entry points | SCR-ONB-04 Continue · Back from SCR-ONB-06 · resume. |
| Main content (top → bottom) | (1) Top bar, "Step 4 of 7". (2) Question "Would a daily reminder help?" (3) Time row "19:00 †" with "Change time". (4) One line: "One reminder a day. None on days you've already practised." (SRS-BR-019). (5) Remind me at {time}; Not now. |
| Components | Top app bar (full-screen) · Step progress · List item (time) · M3 time picker in a Dialog · Large button · Text button |
| Primary action | **Remind me at {time}** → (Android 13+) system notification prompt → SCR-ONB-06 |
| Secondary actions | Change time · Not now (no prompt, `reminderEnabled = false`) · Back |
| Loading | N/A — static. |
| Empty | N/A — static. |
| Error | As SCR-ONB-02. |
| Permission | Android 13+: the system POST_NOTIFICATIONS dialog follows the primary action (SRS-FR-ONB-005). Denied: the preference is kept, onboarding continues without a message, and SCR-SET-03 later shows the blocked state (FS-02 A5). Below Android 13: no dialog. |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-ONB-04. Tab: hidden. |
| Owning FS | FS-02 (scheduling: FS-09) |
| Requirements | SRS-FR-ONB-005, SRS-FR-REM-001, SRS-FR-REM-005; SRS-BR-019 |

#### SCR-ONB-06 — Privacy & microphone

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Explain, truthfully and in plain words, where the user's voice goes, then ask for the microphone in context (PP-2; UXF-36). |
| Primary persona(s) | Meera (privacy is her main worry), Sofia. |
| Entry points | SCR-ONB-05 · Back from SCR-ONB-07 · resume · return from system settings. |
| Main content (top → bottom) | (1) Top bar, "Step 5 of 7". (2) Title "Your voice stays on this phone". (3) Three statements generated from policy constants: recordings stay on this phone; transcripts stay on this phone; you choose how long recordings are kept (D-10 default 30 days). (4) "More about privacy" → SCR-SET-09. (5) Allow microphone. |
| Components | Top app bar (full-screen) · Step progress · List item ×3 · Text button · Large button · Mic permission card (denied states) |
| Primary action | **Allow microphone** → system RECORD_AUDIO dialog. In the denied states it becomes **Try again**; when permanently denied, **Open settings**. |
| Secondary actions | More about privacy · Continue without microphone (denied states only) · Back |
| Loading | N/A — static. |
| Empty | N/A — static. |
| Error | N/A — no data operation (permission results are states, below). |
| Permission | Granted → SCR-ONB-07. Denied once → Mic permission card: "You can explore SpeakCraft, but speaking practice needs the microphone." + Try again + Continue without microphone (SRS-FR-ONB-007). Permanently denied → same card with Open settings; permission re-checked on return (`ON_RESUME`). |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-ONB-05. Tab: hidden. |
| Owning FS | FS-02 |
| Requirements | SRS-FR-ONB-006, -007; SRS-PRIV-002; SRS-ERR-001 |

#### SCR-ONB-07 — Speech check (tier-specific choice)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Find out what speech recognition this phone supports (T1/T2/T3a/T3b) and let the user make the one choice that applies, with no unverified status (PP-1; UXF-60). |
| Primary persona(s) | Hoang (older phone), Meera (online transcription consent). |
| Entry points | SCR-ONB-06 (granted or Continue without microphone) · Back from SCR-ONB-08 · resume. |
| Main content (top → bottom) | (1) Top bar, "Step 6 of 7". (2) While checking: "Checking speech recognition on this phone…" (≤ 2 s †). (3) One outcome (FS-02 §5.1): **On-device** (T1/T2) status line, plus "Replaying your recordings isn't available on this phone" for T2 · **Pack installable**: choice of Download the English speech pack (needs a connection) / Use online transcription / Continue without transcripts · **Online candidate** (T3a): consent text — what is sent, to whom, how to withdraw (SRS-FR-SPCH-005) — and a choice of Allow online transcription / Use audio-only feedback, **with nothing pre-selected** · **Audio-only** (T3b): Capability notice "This phone can't turn speech into text for SpeakCraft. You'll still get feedback on speaking time and pauses." (4) Continue. |
| Components | Top app bar (full-screen) · Step progress · Processing indicator (check) · Radio group (outcome choices) · Capability notice · Linear progress (download) · Large button · Text button |
| Primary action | **Continue** (enabled once a required choice is made). Starting a download happens on Continue and runs in the background (FS-02 A9). See I-14 on the radio-group pattern. |
| Secondary actions | Cancel download · Retry · Back |
| Loading | Check in progress (≤ 2 s †). If it takes longer: provisional audio-only copy "We'll check again later" (FS-02 E7). |
| Empty | N/A — there is always an outcome. |
| Error | Download failed: "Download didn't finish. Check your connection and try again." + Retry; other options stay available (FS-02 E6). |
| Permission | N/A — detecting the tier needs no permission. |
| Offline | Download and online-transcription options show "Needs a connection" and are disabled; on-device and audio-only outcomes are unaffected. |
| Navigation | Back / Up: SCR-ONB-06. Tab: hidden. DES-04 FLOW-01 proposes skipping this stop when no choice is needed (T1/T2). |
| Owning FS | FS-02 (tier logic: FS-12) |
| Requirements | SRS-FR-ONB-008; SRS-FR-SPCH-001, -003, -004, -005; SRS-BR-023 |

#### SCR-ONB-08 — First attempt intro

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Introduce the first speaking attempt ("Introduce yourself"), which becomes the user's baseline (PRD-FR-013; PP-4). The attempt itself runs in SCR-SESS-01. |
| Primary persona(s) | All. |
| Entry points | SCR-ONB-07 Continue · resume (an in-progress recording is discarded, FS-02 A6). |
| Main content (top → bottom) | (1) Top bar, "Step 7 of 7". (2) Template label "Answer a question". (3) Prompt "Introduce yourself" and "About a minute. You'll get 15 seconds to plan." (4) What happens next: "We'll show what we heard and one thing to try next." (5) Tier-specific privacy line (on-device: "This stays on your phone"; T3a: names the speech service). (6) Start; Do this later. |
| Components | Top app bar (full-screen) · Step progress · Prompt card (preview) · Large button · Text button · Mic permission card (if not granted) · Offline row (T3a only) |
| Primary action | **Start** → SCR-SESS-01 with activity `onb-intro` (first-result variant: primary action "Go to Today") |
| Secondary actions | Do this later → onboarding completes, SCR-TODAY-01 opens (SRS-FR-ONB-011) · Back |
| Loading | N/A — content is bundled. |
| Empty | N/A — `onb-intro` is guaranteed by the content-pack build check (SRS-ERR-008). |
| Error | N/A — no data operation before Start. |
| Permission | Microphone not granted: Mic permission card "Microphone needed" + Allow microphone; Do this later stays available (FS-02 A4). |
| Offline | Identical on T1, T2 and T3b. T3a: Offline row "You're offline, so this attempt gets audio-only feedback." |
| Navigation | Back / Up: SCR-ONB-07. Tab: hidden. |
| Owning FS | FS-02 |
| Requirements | SRS-FR-ONB-007, -009, -011 |

### 4.2 Today (owning FS-03)

#### SCR-TODAY-01 — Today

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Answer "What should I practise now?" with **one** session card and **one** Start button (PP-3; replaces the ~21-widget dashboard, UXF-19). |
| Primary persona(s) | Aarav (ten minutes before stand-up), Daniel, Hoang. |
| Entry points | Start destination after onboarding · Today tab · system Back from any other tab root · reminder notification tap (SRS-FR-REM-003) · Done on SCR-TODAY-02 · Close in SCR-SESS-01 mid-session · Go to Today at the end of onboarding. |
| Main content (top → bottom) | (1) Top app bar: title "Today"; R2 bell with unread badge; avatar "Profile and settings". (2) Greeting: first visit "Welcome, {name}"; later "Good morning, {name}" + one factual line, e.g. streak or last focus (SRS-FR-TODAY-010; no name → greeting without a name, FS-02 issue 4). (3) **Session card**: "Today's session", estimated minutes, 3–4 step rows (template label, title, status Done / Next / To do), recovery note when it applies ("A shorter session to ease back in", SRS-BR-014), primary button, "Change duration" (before starting). (4) Streak counter + week strip ("Start your streak today" for a new user, never a seeded 1). (5) Today's progress line: "2 of 4 steps done · 6 min speaking" (measured, SRS-BR-018). (6) At most one contextual card: "Introduce yourself" if the onboarding attempt was skipped (FS-02 A1), or the microphone notice. **Done variant** (all steps complete, SRS-FR-TODAY-011): the session card shows "Today's session is done", what was practised, and a suggested optional activity. Expanded width: two columns (session card · progress summary). |
| Components | Top app bar (primary) · Avatar · R2 M3 badge on the bell · Card (session card, large radius) · List item (steps) · Large button · Text button · Icon button (step overflow → Swap) · Bottom sheet (duration choice) · Streak counter · Streak week strip · Mic permission card · Capability notice (T3b) · Snackbar · Navigation bar / rail |
| Primary action | **Start session** → SCR-SESS-01 at step 1. Variants: **Continue session** (opens the next incomplete step) · **Practise more** in the Done variant (starts the suggested activity directly in SCR-SESS-01, so it stays within 2 taps, UX-2). |
| Secondary actions | Swap a step (≤ 3 per day †, SRS-FR-TODAY-009) · Change duration before starting (SRS-FR-TODAY-007) · Avatar → SCR-PROF-01 · R2 bell → SCR-INBOX-01 |
| Loading | Plan generation ≤ 150 ms (NFR-PERF-007); a skeleton of the session card appears only after 300 ms. |
| Empty | N/A for the session: a plan is always generated from bundled content. New-user streak shows "Start your streak today". |
| Error | Plan generation or database read fails: Error state inside the session card "Couldn't prepare today's session. Your progress is safe." + Try again (SRS-ERR-005). |
| Permission | Microphone denied: speaking steps are disabled with "Microphone needed for speaking steps" + Allow microphone (DES-04 FLOW-13); non-speaking steps (for example a word *Learn* stage) stay available (SRS-FR-ONB-007). |
| Offline | Identical to default (PP-6). No offline indicator on Today; T3a users see the offline note in the session player. |
| Navigation | Back: exits the app (start destination). Up: none (tab root). Tab: Today selected; re-selection scrolls to top. |
| Owning FS | FS-03 |
| Requirements | SRS-FR-TODAY-001…011; SRS-FR-ONB-007, -011; SRS-FR-SPCH-007; SRS-FR-PROG-005; SRS-FR-REM-003; SRS-FR-REM-007 (R2); SRS-BR-011, -013, -014 |

#### SCR-TODAY-02 — Session complete

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Close today's session with an honest summary: what was practised, measured speaking time and streak status (PP-7). |
| Primary persona(s) | Aarav, Daniel, Hoang. |
| Entry points | Automatically after the last step of today's session is complete in SCR-SESS-01 (valid attempt or finished review set, SRS-BR-009). |
| Main content (top → bottom) | (1) Full-screen top bar with Close. (2) Title "Session done" (no exclamation mark, DES-05 §12.1). (3) "What you practised": step list with outcome (Done / Skipped), no scores. (4) Metric tile "Speaking time today" with Measured badge (SRS-BR-018). (5) Streak counter + week strip with today marked practised; on a milestone day (3, 7, 14, 30, 60, 100) a one-time celebration, static under reduced motion (SRS-FR-PROG-006). (6) "Your focus" — the last focus from feedback, carried forward (RECOMMENDATION). (7) Suggested optional activity card. (8) Done. |
| Components | Top app bar (full-screen) · Metric tile · Evidence badge · Streak counter · Streak week strip · milestone celebration (DES-05 §8.3) · Card (suggestion) · Large button · Text button |
| Primary action | **Done** → SCR-TODAY-01 (Done variant) |
| Secondary actions | Practise more → suggested activity in SCR-SESS-01 |
| Loading | N/A — the summary is computed from local data (≤ 50 ms, NFR-PERF-008). |
| Empty | N/A — the screen exists only after a completed session. |
| Error | Summary read fails: the title and Done still show (completion is already saved) with "Couldn't load the summary." |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Close: same as Done (never returns into the finished player). Up: Close. Tab: hidden. |
| Owning FS | FS-03 (streak and milestones: FS-08) |
| Requirements | SRS-FR-TODAY-011; SRS-FR-PROG-006; SRS-BR-009, -010, -012, -018 |

### 4.3 Session (owning FS-04 and FS-05)

#### SCR-SESS-01 — Session player

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | The **one** place where every attempt is prepared, recorded and evaluated, for every template and word stage (replaces 10 recording surfaces and 12 start labels, UXF-12, UXF-16). |
| Primary persona(s) | All. |
| Entry points | SCR-TODAY-01 Start / Continue session / Practise more · SCR-TODAY-02 Practise more · SCR-PRAC-03 Start · SCR-WORD-03 speaking stages · SCR-WORD-04 review items · SCR-PROG-04 Try again · SCR-ONB-08 Start. |
| Main content (top → bottom) | **Always:** full-screen top bar (Close, title = activity title or "Today's session"), Step progress ("Step 2 of 4" in Today; "Word 3 of 8" in a review). **Prompt / Ready:** Prompt card (template label, prompt in `promptText`, target words emphasised, optional tip; Listen with speed 1.0× / 0.75× for Read aloud and Listen & repeat); recording panel at the bottom (status line, level meter, Record button, label, timer "0:00 / 1:30"). **Prepare:** "Plan your answer · 0:15" countdown; Record stays available and skips preparation (SRS-FR-SESS-002). **Recording:** Stop button, elapsed / limit, solid dot, live level; "10 seconds left" once; "We can't hear anything. Check your microphone." after 3 s of near-silence. **Processing:** Processing indicator + "Checking your recording…"; after the threshold "This is taking longer than usual" + Cancel (see I-16). **Result** (Feedback card, in this order — Product Owner direction 2026-11-21; see I-23): header "Here's how it went" → (a) Transcript view "What we heard" with tier label and play control → (b) ≤ 3 headline Metric tiles with Evidence badges, the comparison with the previous valid attempt after Try again (SRS-FR-SESS-008), and for Read aloud / Listen & repeat "Recognised as intended: 82 % (pass mark 75 %)" or "Not yet. Try again." → (c) Strength "What went well" (one sentence) → (d) **Mistakes found** (MistakeList): up to 3 issues by ARC-05 §8.2 priority, each with its evidence (quoted span, count or value) and Measured / Estimated badge, then "Show all"; none found → "Our checks didn't find issues in this attempt" + one line on what the checks cover (SRS-FR-EVAL-015) → (e) **Coach tips** (CoachTipCard ×1–3): the focus tip first, then up to 2 more, each linking to its technique (SCR-PRAC-04) and, where provenance exists, "Watch the original lesson" (SRS-FR-EVAL-016) → (f) actions: Try again (and Continue in Today) → (g) "Was this accurate?". **Inconclusive:** Inconclusive card with the specific reason and reassurance. **Interrupted:** "Recording stopped because of a call." **Error:** "Couldn't check your recording. It's kept until you try again." |
| Components | Top app bar (full-screen) · Step progress · Prompt card · Tonal button (Listen) · Segmented button (speed, A11Y-25) · Record button + level meter + timer · Processing indicator · Feedback card · Metric tile · Evidence badge · Transcript view · **MistakeList** and **CoachTipCard** (DES-05 §11.15a–b) · SelfCheckList (DES-05 §11.15c; technique activities only) · Icon button (play / pause) · Outlined button (Yes / No) · Filter chip (accuracy reasons) · Inconclusive card · Bottom sheet (tips; SCR-SESS-02) · Dialog (leave and discard) · Mic permission card · Capability notice (T3b) · Offline row (T3a) · Snackbar |
| Primary action | Exactly one per state: Ready / Prepare → **Record** · Recording → **Stop** · Processing → none (automatic) · Result → **Continue** inside Today, **Try again** elsewhere (DES-05 §11.15) · Inconclusive → **Try again** · Interrupted → **Record again** · Error → **Retry** (re-runs evaluation on the kept audio, SRS-FR-EVAL-013) · Permission → **Allow microphone** / **Open settings** · First onboarding result → **Go to Today** |
| Secondary actions | Listen and speed (Read aloud, Listen & repeat; hidden if text-to-speech is unavailable, SRS-ERR-010) · Try again (tonal, in Today) or Done (text, elsewhere) on Result · Show all mistakes · Open a tip's technique (opens SCR-PRAC-04 above the player; Back returns to the result, I-24) · Watch the original lesson · How we measured this → SCR-SESS-02 · Show full transcript · Play / pause recording (where retained) · Was this accurate? (Yes / No + reason chips) · Tips for a clear recording · Skip this step (Today only; see I-9) · Cancel (after the processing threshold) · Close |
| Loading | Processing is this screen's loading state (above). Before the prompt: N/A — content is bundled. |
| Empty | No mistakes detected: "Our checks didn't find issues in this attempt" + one line on what the checks cover — never "perfect" (SRS-FR-EVAL-015; PRD-FR-047). Otherwise N/A — the player is always opened with an activity. Retired content opened from history still renders from `activityId` + `contentVersion` (SRS-BR-026). |
| Error | Evaluation over budget → Error state + Retry, audio kept until retry or exit (SRS-FR-EVAL-013). Database write fails → non-blocking error, never a false success (SRS-ERR-005). Storage full → Interrupted state with a storage-settings link (SRS-ERR-004). Recogniser busy → one retry, then the attempt continues audio-only and the result says the transcript wasn't available (SRS-ERR-002). |
| Permission | Not granted → Permission state replaces the Record button (Mic permission card, DES-04 FLOW-13); first request outside onboarding goes through SCR-PERM-01. Permanently denied → Open settings; re-checked on resume. Revoked during recording → Interrupted (SRS-FR-SESS-010). Microphone busy (call, other app) → disabled Record button, "The microphone is being used by another app" + Try again. |
| Offline | T1, T2, T3b: identical (on-device). T3a: before recording, Offline row "You're offline, so this attempt gets audio-only feedback." (SRS-AVL-003; FS-04 issue on T3a network loss). |
| Tier variants | Transcript label "On-device" / "Online (consented)" / "No transcript — audio-only" (SRS-FR-SESS-006). T2 and T3a: no playback, "Replay isn't available on this phone". T3b: headline metrics speaking time, long pauses, speech ratio (ARC-05 §7); transcript-dependent checks say "Transcript needed for this check" (SRS-FR-SPCH-007). |
| Navigation | Back / Close: in Ready, Result or Inconclusive it leaves the player (Today: back to SCR-TODAY-01 with completed steps kept, SRS-BR-013; elsewhere: back to the launching screen). During Prepare, Recording or Processing: Dialog "Leave and discard this recording?" — Leave / Keep practising (SRS-FR-SESS-011). Up: Close. Tab: hidden. Rotation keeps state, including an in-progress recording (SRS-FR-SESS-013). |
| Owning FS | FS-04 (Result and Inconclusive content: FS-05; tiers: FS-12) |
| Requirements | SRS-FR-SESS-001…014; SRS-FR-EVAL-001…009, -012…016; SRS-FR-WORD-014 (word-stage and review results); SRS-FR-SPCH-002, -006, -007, -009, -010; SRS-ERR-001…005, -010; SRS-BR-001…008, -022 |

#### SCR-SESS-02 — Feedback detail sheet ("How we measured this")

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Explain each metric — what it is, how we measure it, why it matters, its limits (DES-05 §12.4 rule 5) — and list all measurements, so caveats are one tap away (UX-3; HRC-29). |
| Primary persona(s) | Sofia (wants detail), Meera (wants to trust the feedback), Aarav. |
| Entry points | Info button on any Metric tile (SCR-SESS-01 Result, SCR-PROG-02, SCR-PROG-04) · "All measurements" on a result · "How speech recognition works" in the Transcript view · the *Say it* caveat on SCR-WORD-03. |
| Main content (top → bottom) | (1) Drag handle, title (e.g. "How we measured pace"), Close. (2) Evidence badge. (3) This attempt's value, unit and target band in words. (4) Four sections: What it is · How we measure it · Why it matters · Its limits (e.g. filler counts depend on the recogniser keeping "um"). Text is generated from the ARC-05 metric catalogue (ARC-05 §10). **All-measurements variant:** every computed metric with its badge; null metrics read "Not available — {reason}" (SRS-BR-002). |
| Components | Bottom sheet (with Close icon button) · Section header · Evidence badge · Metric tile (list in the variant) · Text button (links) |
| Primary action | **Close** (the sheet is informational) |
| Secondary actions | How speech recognition works (opens the matching section) |
| Loading | N/A — text is local. |
| Empty | A metric without inputs shows "Not available — {reason}"; no default value is ever shown (SRS-BR-002). |
| Error | N/A — no data operation. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back: dismisses the sheet; focus returns to the info button (DES-05 §11.6). Up: Close. Tab: inherits the host (hidden in the player, visible in Progress). |
| Owning FS | FS-05 |
| Requirements | SRS-FR-EVAL-002, -003, -010, -011, -014; SRS-BR-002, -003, -004 |

### 4.4 Practice (owning FS-06)

> **Technique content — D-08 decided 2026-11-21: "original wording, link out" (PRD-FR-056, PRD-FR-057; SRS-FR-PRAC-008, -009; SRS-BR-028; DES-02 v0.2).** The curriculum is built from the frameworks, techniques, exercises, activities and tips taught on the @askvinh YouTube channel, **re-expressed in our own words and examples**. Each technique, exercise and tip links to its **specific** source video ("Watch the original lesson" opens the YouTube app or browser through an outbound intent). **No creator name, likeness or branding appears anywhere** in the UI. No scraping, caption download or API ingestion (the ingestion tool stays removed, ADR-011); content is curated by hand into the bundled content pack with provenance and `licenceStatus`, and items without verified provenance do not ship (SRS-ERR-008). A **technique** is a first-class content item: a **Techniques** collection on SCR-PRAC-01 leads to **SCR-PRAC-04 Technique detail** (idea, why it works, steps, tips, linked exercises, source link). Activities reference their technique(s). Path: Practice → Technique (SCR-PRAC-04) → Activity (SCR-PRAC-03) → Session player — within the maximum depth of 3 (DES-02 §2.3). The 4-tab IA is unchanged.

#### SCR-PRAC-01 — Practice home (tracks; Techniques collection)

| Field | Specification |
|---|---|
| Release | R1 (R2 role-play activities appear inside tracks) |
| Purpose | Answer "I want to rehearse a specific situation" with five plainly named tracks, and "I want to learn a technique" with the Techniques collection (replaces modules, paths, programs, the 22-tile directory and the creator-named tab, UXF-07, UXF-13, UXF-25). |
| Primary persona(s) | Meera (Job interviews), Daniel (Presentations & storytelling), Aarav (Workplace), Sofia (named techniques to polish delivery). |
| Entry points | Practice tab · Back from SCR-PRAC-02 and SCR-PRAC-04. |
| Main content (top → bottom) | (1) Top app bar: "Practice", avatar. (2) Segmented button **Tracks · Techniques** (RECOMMENDATION; DES-02 v0.2 places the collection on this screen — I-17). **Tracks view:** (3) the user's own track first, labelled "Your goal"; (4) track cards for the other four tracks: name, one-line description, activity count (computed, DES-05 §12.4 rule 6), "{n} practised"; (5) "Saved activities" row with count → SCR-PRAC-02 in Saved mode (I-6). **Techniques view:** (3) filter chips: track, Saved; (4) technique cards grouped by track: technique name in our wording, one-line idea, "{n} exercises", "Practised" status, trailing "Save" icon button. No creator name or branding (SRS-BR-028). Expanded width: list-detail with SCR-PRAC-02 or SCR-PRAC-04. |
| Components | Top app bar (primary) · Avatar · Segmented button · Card (track card, technique card; clickable) with Icon button (save) · Filter chip · List item (Saved) · Section header · Empty state · Navigation bar / rail |
| Primary action | **Open a track** (Tracks view) → SCR-PRAC-02 · **Open a technique** (Techniques view) → SCR-PRAC-04 |
| Secondary actions | Switch view · Saved activities → SCR-PRAC-02 (Saved mode) · Save / unsave a technique · Filters (Techniques view) · Avatar → SCR-PROF-01 |
| Loading | Skeleton after 300 ms (content read ≤ 50 ms). |
| Empty | N/A for tracks (bundled, ≥ 15 activities each, PRD-FR-053). "Saved activities" row reads "No saved activities yet". Techniques view with the Saved filter and nothing saved: "Save techniques to find them here" + Clear filter. |
| Error | Content cannot be read (damaged install): Error state "Couldn't load activities." + Try again. Invalid content never ships (SRS-ERR-008). |
| Permission | N/A — the microphone is requested when an attempt starts. |
| Offline | Identical to default. |
| Navigation | Back: SCR-TODAY-01 (N-1). Up: none. Tab: Practice selected; the chosen view is kept when returning. |
| Owning FS | FS-06 |
| Requirements | SRS-FR-PRAC-001, -005, -006, -008; SRS-BR-028 |

#### SCR-PRAC-02 — Track (activity list, filters, saved)

| Field | Specification |
|---|---|
| Release | R1 (R2 adds the Role-play template filter) |
| Purpose | Choose an activity in a track; filter by template and level; show saved activities across tracks. |
| Primary persona(s) | Meera, Daniel, Aarav. |
| Entry points | SCR-PRAC-01 track card · SCR-PRAC-01 "Saved activities" row (Saved mode, across tracks) · Back from SCR-PRAC-03. |
| Main content (top → bottom) | (1) Top app bar (detail): track name (or "Saved activities"). (2) One-line track description. (3) Filter chips: templates (Read aloud, Listen & repeat, Answer a question, Structured answer, Tell a story; R2 Role-play), level, Saved. (4) Result count. (5) Activity cards: title, template label, estimated minutes, level, "Technique: {name}" as plain text (not a separate target, DES-05 §11.3), "Practised" status, trailing "Save" icon button. |
| Components | Top app bar (detail) · Filter chip · Card (activity card) with Icon button (save) · Empty state · Skeleton · Snackbar ("Saved", "Removed from saved" + Undo) |
| Primary action | **Open an activity** (activity card) → SCR-PRAC-03 |
| Secondary actions | Filters · Save / unsave · Clear filters |
| Loading | Skeleton after 300 ms. |
| Empty | Filters with no results: "No activities match these filters" + Clear filters. Saved mode with none: "Save activities to find them here" + Browse tracks. |
| Error | Error state "Couldn't load activities." + Try again. Save fails: Snackbar "Couldn't save. Try again." |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-PRAC-01; filters kept in saved state. Tab: Practice highlighted. |
| Owning FS | FS-06 |
| Requirements | SRS-FR-PRAC-001, -002, -006, -007 (R2), -008 (activities reference techniques) |

#### SCR-PRAC-03 — Activity detail

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show what the activity asks and what feedback it gives, then start it. |
| Primary persona(s) | Meera (rehearses a specific interview question), Daniel, Aarav. |
| Entry points | SCR-PRAC-02 activity card · an exercise row or "Practise it" on SCR-PRAC-04 · "Open activity" on SCR-PROG-04 · Back / Done from SCR-SESS-01. |
| Main content (top → bottom) | (1) Top app bar (detail): activity title, "Save" icon button. (2) Template label · estimated minutes · level; "Technique: {name}" link → SCR-PRAC-04 when the activity belongs to a technique (cross-link, I-24). (3) "Why practise this" (goal). (4) Prompt card preview (read-only). (5) How it works, e.g. "15 seconds to plan, up to 90 seconds to speak" (values from SRS-FR-SESS-002/003 †). (6) "You'll get feedback on" — names of the headline metrics (ARC-05 §7), plus "Mistakes found and coach tips" (SRS-FR-EVAL-015/016). (7) Capability notice when a check needs a transcript on T3b (SRS-FR-SPCH-007). (8) "Last attempt: {date}" → SCR-PROG-04 if practised. (9) Start. |
| Components | Top app bar (detail) · Icon button (save) · Text button (technique link) · Prompt card · Tonal button (Listen, Read aloud and Listen & repeat only) · Capability notice · List item (last attempt) · Large button |
| Primary action | **Start** → SCR-SESS-01 |
| Secondary actions | Save · Listen · Open the technique → SCR-PRAC-04 · Open last attempt |
| Loading | N/A — bundled content (skeleton rule applies if a read is slow). |
| Empty | N/A |
| Error | Activity no longer in the content pack: "This activity isn't available any more." + Back. |
| Permission | N/A here — Start stays enabled; the player handles the microphone (one pattern, DES-05 DS-6). |
| Offline | Identical to default. |
| Navigation | Back / Up: the screen it was opened from (SCR-PRAC-02 or SCR-PRAC-04). Tab: Practice highlighted. |
| Owning FS | FS-06 (attempt: FS-04) |
| Requirements | SRS-FR-PRAC-003, -004, -006, -008; SRS-FR-SESS-012; SRS-FR-SPCH-007 |

#### SCR-PRAC-04 — Technique detail

| Field | Specification |
|---|---|
| Release | R1 (added in DES-02 v0.2, 2026-11-21) |
| Purpose | Explain one technique in our own words — idea, why it works, steps, tips — let the user practise it through its linked exercises, and link to the specific source video (PRD-FR-056, PRD-FR-057; replaces the creator-named tab and ingestion tool, UXF-07). |
| Primary persona(s) | Sofia (named techniques for pace and pauses), Meera (structured interview answers), Daniel (presentations). |
| Entry points | SCR-PRAC-01 Techniques view card · "Technique: {name}" link on SCR-PRAC-03 · a coach tip's technique link on the SCR-SESS-01 Result, SCR-WORD-03 / SCR-WORD-04 results and SCR-PROG-04 (I-24) · Back from SCR-PRAC-03. |
| Main content (top → bottom) | (1) Top app bar (detail): technique name (our wording), "Save" icon button. (2) One-line idea. (3) Track and level labels; "{n} of {m} exercises practised". (4) "Why it works" — short paragraph. (5) "Steps" — numbered list. (6) "Tips" — short list. (7) "Exercises" — the linked activities: title, template label, minutes, Practised status; each opens SCR-PRAC-03. (8) "Watch the original lesson" text button with an external-link icon, shown only where verified provenance exists; opens the specific video at its timestamp in the YouTube app or browser through an outbound intent; no in-app player, no API key, no creator name (SRS-FR-PRAC-009, SRS-BR-028). (9) Sticky bottom: Practise it. |
| Components | Top app bar (detail) · Icon button (save) · Section header · List item (steps, tips, exercises) · Text button ("Watch the original lesson") · Large button · Offline row · Snackbar |
| Primary action | **Practise it** → SCR-PRAC-03 Activity detail for the first unpractised linked exercise (when all are practised, the first exercise again), then Start → SCR-SESS-01 — DES-02 §3/§4 path Practice (SCR-PRAC-01) → SCR-PRAC-04 → SCR-PRAC-03 → SCR-SESS-01 (consistency review, CL-53; I-17) |
| Secondary actions | Open a specific exercise → SCR-PRAC-03 · Save technique · Watch the original lesson (leaves the app) |
| Loading | N/A — bundled content (skeleton rule applies if a read is slow). |
| Empty | N/A — a technique without linked exercises or verified provenance does not ship (build-time check, SRS-BR-028, SRS-ERR-008). |
| Error | Technique no longer in the content pack (opened from an old attempt's tip): Error state "This technique isn't available any more." + Back. Video link cannot open (no YouTube app or browser): Snackbar "Couldn't open the video." |
| Permission | N/A — the player handles the microphone. |
| Offline | Identical to default, except "Watch the original lesson": tapping it while offline shows the Offline row "Watching the original lesson needs a connection." |
| Navigation | Back / Up: SCR-PRAC-01 (or the screen that opened it). Tab: Practice highlighted. Opened from a coach tip inside a full-screen task: shown above it, bar hidden; Back returns to the result (I-24). |
| Owning FS | FS-06 |
| Requirements | SRS-FR-PRAC-004 (exercises count toward progress), -008, -009; SRS-BR-028 |

### 4.5 Words (owning FS-07)

#### SCR-WORD-01 — Words home

| Field | Specification |
|---|---|
| Release | R1 (R2 adds Notebook and Quiz entries) |
| Purpose | Answer "Which words am I learning and what's due?" and offer the next word task. |
| Primary persona(s) | Daniel, Meera (interview vocabulary), Hoang (everyday words), Aarav. |
| Entry points | Words tab · Back from SCR-WORD-02, SCR-WORD-05 · Close / Done from SCR-WORD-03, SCR-WORD-04, SCR-WORD-06. |
| Main content (top → bottom) | (1) Top app bar: "Words", avatar. (2) Today card: new-word goal progress ("1 of 2 new words today", SRS-BR-027 †), reviews due ("5 words to review"), primary button. (3) Search field. (4) Filter chips: New · Learning · Learned · Due · Long-term review (internal status MASTERED, see I-8) · level · track. (5) Word rows: word, short meaning, status text ("Say it next", "Review on Tuesday"). (6) R2: Notebook and Quiz rows. |
| Components | Top app bar (primary) · Avatar · Card · Large button · Text field (search) · Filter chip · List item · Empty state · Skeleton · Capability notice (T3b) · Navigation bar / rail |
| Primary action | **Start review** when reviews are due → SCR-WORD-04; otherwise **Learn next word** → SCR-WORD-03 |
| Secondary actions | Search · Filters · Open a word → SCR-WORD-02 · R2 Notebook → SCR-WORD-05 · R2 Quiz → SCR-WORD-06 |
| Loading | Skeleton after 300 ms. |
| Empty | Search or filters with no result: "No words match" + Clear. Nothing left for today: Today card reads "You're done with words for today. Next review: {date}." (no button). |
| Error | Error state "Couldn't load your words. Your progress is safe." + Try again. |
| Permission | N/A here (speaking stages handle the microphone). |
| Offline | Identical to default. T3b: Capability notice "Word checks need a transcript" → SCR-SET-04 (SRS-FR-SPCH-007; see DES-04 issue on T3b words). |
| Navigation | Back: SCR-TODAY-01. Up: none. Tab: Words selected. |
| Owning FS | FS-07 |
| Requirements | SRS-FR-WORD-001, -009, -010, -011; SRS-BR-017, -027 |

#### SCR-WORD-02 — Word detail

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show one word — meaning, examples, stage and review schedule — and continue its next stage. |
| Primary persona(s) | Daniel, Hoang. |
| Entry points | SCR-WORD-01 word row · R2 notebook entry. |
| Main content (top → bottom) | (1) Top app bar (detail): the word. (2) Word card: word, IPA if in content, part of speech, meaning, examples, Listen. (3) Stage stepper Learn · Say it · Use it · Make your own · Review, current stage marked. (4) Status line: "Learning — Say it next" or "Next review: Tuesday". (5) Common collocations. (6) Latest result per passed stage (e.g. "Use it: recognised as intended 84 %" with Measured badge). (7) Primary button. |
| Components | Top app bar (detail) · Word card · Tonal button (Listen) · Stage stepper · Evidence badge · List item · Large button · R2 Icon button ("Save to notebook") |
| Primary action | **Start {next stage}** (e.g. "Start Say it") → SCR-WORD-03. Due for review → **Review now** → SCR-WORD-04. Learned and not due → **Practise again** (replaying never lowers status, SRS-BR-015). |
| Secondary actions | Listen · R2 Save to notebook |
| Loading | N/A — bundled content and local progress. |
| Empty | N/A |
| Error | Word missing from content: Error state + Back to Words. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-WORD-01. Tab: Words highlighted. |
| Owning FS | FS-07 |
| Requirements | SRS-FR-WORD-001, -002, -008; SRS-BR-015, -016, -017 |

#### SCR-WORD-03 — Word stage flow (Learn → Say it → Use it → Make your own)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Take one word through its learning stages; speaking stages use SCR-SESS-01. |
| Primary persona(s) | Daniel, Meera, Hoang. |
| Entry points | SCR-WORD-02 primary · SCR-WORD-01 Learn next word · a word step in today's session (see DES-04 issue on hand-over). |
| Main content (top → bottom) | (1) Full-screen top bar: Close, stage stepper. (2) **Learn:** full Word card — meaning, 2 examples, collocations, Listen at 1.0× / 0.75× — and "I've read this". (3) **Say it / Use it / Make your own:** instruction ("Say the word", "Read these two sentences", "Make your own sentence with 'negotiate'") then SCR-SESS-01 in word-stage mode. (4) **Stage result:** "Passed: Say it" (success colour = completion only) or "Not yet. Try again." with "Recognised as intended 68 % (pass mark 75 %)" and Measured badge; then **Mistakes found** (MistakeList) with evidence, e.g. "Word not recognised", "We heard 'negotiation'", "Target word missing", "Sentence too short (4 words)", "Possible grammar issue: '…'"; then **one coach tip** (CoachTipCard) for the next try, linked to its technique (SRS-FR-WORD-014; SRS-FR-EVAL-015/016). Inconclusive card instead when recognition gave no usable output. Say-it caveat (ARC-05 §9). (5) After 3 failed attempts on a stage: "Try an easier version" + "Continue later" (SRS-FR-WORD-007). (6) After *Make your own* passes: "Learned. First review: tomorrow." |
| Components | Top app bar (full-screen) · Stage stepper · Word card · Tonal button (Listen) · Segmented button (speed) · Prompt card · Record button (via SCR-SESS-01) · Evidence badge · MistakeList · CoachTipCard (DES-05 §11.15a–b) · Inconclusive card · Capability notice · Large button · Text button |
| Primary action | Per stage: Learn → **I've read this** · stage passed → **Next: {stage}** · Not yet → **Try again** · Learned → **Done** |
| Secondary actions | Listen · Try an easier version (after 3 failures) · Continue later · Close |
| Loading | N/A — local. Evaluation states are SCR-SESS-01's. |
| Empty | N/A |
| Error | As SCR-SESS-01 (evaluation or save failure). |
| Permission | Microphone not granted: Mic permission card on speaking stages; *Learn* stays usable. |
| Offline | Identical to default. T3b: stages 2–4 show "Transcript needed for this check" + Continue later + link to SCR-SET-04 (SRS-FR-SPCH-007). |
| Navigation | Close / Back: leaves the flow; passed stages are kept and the word resumes at the next stage (SRS-BR-015). During a recording the player's leave dialog applies. Up: Close. Tab: hidden (full-screen task, RECOMMENDATION). |
| Owning FS | FS-07 (recording and evaluation: FS-04, FS-05) |
| Requirements | SRS-FR-WORD-002…008, -014; SRS-FR-EVAL-015, -016; SRS-BR-007, -008, -015; SRS-FR-SPCH-007 |

#### SCR-WORD-04 — Review session

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Run a spaced-review set of due words (max 10 †) with spoken recall and sentence use. |
| Primary persona(s) | Daniel, Aarav. |
| Entry points | SCR-WORD-01 Start review · SCR-WORD-02 Review now · the review step in today's session. |
| Main content (top → bottom) | **Intro:** full-screen top bar (Close), "8 words to review · about 4 minutes", Start review. **Items:** run in SCR-SESS-01 ("Word 3 of 8"; "Say the word that means …" or a sentence to use); after each item: Remembered / Not yet / Inconclusive, the mistakes found with evidence, and one coach tip (SRS-FR-WORD-014). **Summary:** count reviewed; per word "Remembered", "Remembered after a hint" or "Review again tomorrow"; next review dates; Done. |
| Components | Top app bar (full-screen) · Card · List item · Large button · Step progress, Prompt card, Record button, Inconclusive card (via SCR-SESS-01) · Empty state · Capability notice |
| Primary action | Intro → **Start review** · Summary → **Done** |
| Secondary actions | Close · item-level Hint / Try again (FS-07) |
| Loading | N/A — local. |
| Empty | No reviews due: "No reviews due. Next review: {date}." + Back to Words. |
| Error | Error state + Try again. Completed item outcomes are never lost (idempotent events, SRS-BR-020). |
| Permission | Microphone not granted: Mic permission card before the first item. |
| Offline | Identical to default. T3b: Capability notice — spoken recall needs a transcript (see DES-04 issue). |
| Navigation | Close / Back mid-set: outcomes so far are kept; remaining words stay due. Up: Close. Tab: hidden. |
| Owning FS | FS-07 |
| Requirements | SRS-FR-WORD-009, -014; SRS-BR-009, -016, -017, -020, -027 |

#### SCR-WORD-05 — Notebook (R2)

| Field | Specification |
|---|---|
| Release | R2 |
| Purpose | A personal list of saved words with notes and tags. |
| Primary persona(s) | Sofia (collects precise phrases), Daniel. |
| Entry points | SCR-WORD-01 Notebook row · "View" in the Snackbar after saving a word from a transcript or feedback. |
| Main content (top → bottom) | (1) Top app bar (detail): "Notebook". (2) Search field. (3) Filter chips: favourites, tags. (4) Entries: word, note preview, tags, favourite toggle. (5) Edit sheet for notes and tags. |
| Components | Top app bar (detail) · Text field · Filter chip · List item · Icon button (favourite) · Bottom sheet (edit) · Snackbar (delete + Undo) · Empty state · Skeleton |
| Primary action | **Open an entry** → edit sheet |
| Secondary actions | Search · Filter · Favourite · Delete (with Undo) |
| Loading | Skeleton after 300 ms. |
| Empty | "Save words from your feedback to see them here." |
| Error | Error state + Try again; edit save fails → inline error, text kept. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-WORD-01. Tab: Words highlighted. |
| Owning FS | FS-07 |
| Requirements | SRS-FR-WORD-012 |

#### SCR-WORD-06 — Quiz (R2)

| Field | Specification |
|---|---|
| Release | R2 |
| Purpose | A deterministic quiz on known words (meaning, fill-in-the-blank, sentence choice, listening), labelled "Quiz" — never "AI" (PRD-FR-067). |
| Primary persona(s) | Daniel, Hoang. |
| Entry points | SCR-WORD-01 Quiz row. |
| Main content (top → bottom) | (1) Full-screen top bar: Close, "Question 3 of 10". (2) Question; Listen for listening items. (3) Options. (4) After Check: "Correct" or "Not quite. The answer is …". (5) Summary "7 of 10" and words to revisit; Done. |
| Components | Top app bar (full-screen) · Step progress · Radio group · Tonal button (Listen) · Large button · Card · Dialog (leave) |
| Primary action | **Check** (then **Next**; summary **Done**) |
| Secondary actions | Listen · Close |
| Loading | N/A — generated locally from a seed. |
| Empty | Too few known words: "Learn a few words first." + Learn next word. |
| Error | Error state + Try again. Text-to-speech unavailable: listening questions are left out (SRS-ERR-010). |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Close / Back mid-quiz: Dialog "Leave the quiz? Your answers won't be saved." (answers are input, N-4). Up: Close. Tab: hidden. |
| Owning FS | FS-07 |
| Requirements | SRS-FR-WORD-013 |

### 4.6 Progress (owning FS-08)

#### SCR-PROG-01 — Progress overview

| Field | Specification |
|---|---|
| Release | R1 (R2 adds the Check-ins card) |
| Purpose | Answer "Am I improving?" from evidence only: one streak, measured practice time and trends of measured metrics (PP-7; replaces "Analytics" with its fallback numbers, UXF-23, UXF-56). |
| Primary persona(s) | Sofia (tracks pace and fillers), Aarav, Daniel. |
| Entry points | Progress tab · Back from SCR-PROG-02, SCR-PROG-03. |
| Main content (top → bottom) | (1) Top app bar: "Progress", avatar. (2) Streak: Streak counter (current and best) + Streak month grid. (3) This week: Metric tile "Speaking time" (Measured), sessions completed, valid attempts. (4) Segmented button Week / Month / All for the figures above. (5) Trend cards per template family and metric (e.g. "Pace · Answer, Structured, Story"): latest value with Evidence badge; a mini Line chart only with ≥ 3 valid attempts (SRS-BR-024); "No clear change" inside the noise band; otherwise "Practise {n} more times to see a trend". (6) Words summary: counts by status and words due today → Words tab. (7) "All attempts" → SCR-PROG-03. (8) Local users: dismissible card "Back up your progress" → SCR-ACC-01 (non-blocking, DES-02 §2.2). (9) R2: Check-ins card → SCR-PROG-05. |
| Components | Top app bar (primary) · Avatar · Streak counter · Streak month grid · Metric tile · Evidence badge · Segmented button · Card (trend card) with Line chart · List item · Card (sign-in prompt) with Text button · Empty state · Skeleton · Navigation bar / rail |
| Primary action | **Open a trend** (trend card) → SCR-PROG-02. Empty state → **Start today's session** → SCR-TODAY-01 / SCR-SESS-01. |
| Secondary actions | Period · All attempts · Words summary · Back up your progress · R2 Check-ins · Avatar |
| Loading | Skeleton after 300 ms (queries ≤ 50 ms, NFR-PERF-008). |
| Empty | No valid attempts yet: Empty state "Your progress will appear here" / "Finish your first session to see your pace, pauses and more." + Start today's session. Streak reads "Start your streak today". No zero-filled charts (HRC-31, HRC-32). |
| Error | Error state "Couldn't load your progress. Your practice is saved." + Try again. |
| Permission | N/A |
| Offline | Identical to default (all figures are local). The sign-in card opens SCR-ACC-01, which shows its own offline message. |
| Navigation | Back: SCR-TODAY-01. Up: none. Tab: Progress selected. |
| Owning FS | FS-08 (sign-in prompt: FS-01) |
| Requirements | SRS-FR-PROG-001, -002, -005, -006, -007; SRS-FR-ACC-002; SRS-BR-010, -011, -018, -024 |

#### SCR-PROG-02 — Metric detail (trend)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show one metric's trend across valid attempts in one template family, honestly: real points only, gaps kept, noise band and target band labelled. |
| Primary persona(s) | Sofia. |
| Entry points | SCR-PROG-01 trend card. |
| Main content (top → bottom) | (1) Top app bar (detail): metric name, e.g. "Pace". (2) Latest value as a Metric tile (`displaySmall`) with Evidence badge and info button. (3) Segmented button for the period. (4) Line chart: target band (labelled), noise band, gaps not joined; Estimated metrics drawn dashed and labelled "Estimated" (DES-05 CH-6). (5) Text summary, e.g. "From 142 to 128 words per minute over 8 attempts. No clear change." (6) "View as list" → list of attempts (date, activity, value). |
| Components | Top app bar (detail) · Metric tile · Evidence badge · Icon button (info → SCR-SESS-02) · Segmented button · Line chart · Text button · List item · Empty state · Skeleton |
| Primary action | **Open an attempt** (chart point or list row) → SCR-PROG-04 |
| Secondary actions | Period · View as list · How we measured this |
| Loading | Skeleton after 300 ms. |
| Empty | Fewer than 3 valid attempts: the single latest value + "Practise {n} more times to see a trend" (SRS-BR-024; DES-05 CH-2 says 2 — see I-18). |
| Error | Error state + Try again. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-PROG-01. Tab: Progress highlighted. |
| Owning FS | FS-08 |
| Requirements | SRS-FR-PROG-002; SRS-FR-EVAL-003, -010; SRS-BR-024 |

#### SCR-PROG-03 — History

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | List every attempt, newest first, from all sources, with clear template labels (fixes mixed history, UXF-26). |
| Primary persona(s) | Sofia, Meera (compares interview answers). |
| Entry points | SCR-PROG-01 "All attempts" · Back from SCR-PROG-04. |
| Main content (top → bottom) | (1) Top app bar (detail): "History". (2) Filter chips: template, Valid / Inconclusive. (3) Date group headers. (4) Rows: time, activity title, template label, short headline metrics, Inconclusive badge where relevant, "Recording available" or nothing. Retired content stays listed (SRS-BR-026). Expanded width: list-detail with SCR-PROG-04. |
| Components | Top app bar (detail) · Filter chip · Section header · List item · Evidence badge (Inconclusive) · Empty state · Skeleton |
| Primary action | **Open an attempt** → SCR-PROG-04 |
| Secondary actions | Filters · Clear filters |
| Loading | Skeleton after 300 ms. |
| Empty | No attempts: "No attempts yet" + Start today's session. Filters with no result: "No attempts match" + Clear filters. |
| Error | Error state "Couldn't load your history. Your recordings are safe." + Try again. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-PROG-01. Tab: Progress highlighted. |
| Owning FS | FS-08 |
| Requirements | SRS-FR-PROG-003; SRS-BR-026 |

#### SCR-PROG-04 — Attempt detail (transcript, metrics, replay)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Review one past attempt — what was heard, all metrics, the feedback, the recording — and try the same activity again. |
| Primary persona(s) | Meera, Sofia. |
| Entry points | SCR-PROG-03 row · SCR-PROG-02 point or row · SCR-PRAC-03 "Last attempt". |
| Main content (top → bottom) | (1) Top app bar (detail): activity title. (2) Date, template label, tier label. (3) Feedback (read-only, same order as the SCR-SESS-01 Result): strength, Mistakes found, Coach tips with technique links — or the Inconclusive card with its reason for Inconclusive attempts (SRS-FR-EVAL-015, -016). (4) All metrics as Metric tiles with Evidence badges. (5) Transcript view "What we heard" with play / pause where retained; otherwise "This recording was deleted after {retention}" or "Replay isn't available on this phone" (T2/T3a). (6) "Was this accurate?" — the user can set or change the rating (stored separately from the immutable attempt, ENT-13). (7) Try again. |
| Components | Top app bar (detail) · Feedback card (read-only variant) · MistakeList · CoachTipCard · Inconclusive card · Metric tile · Evidence badge · Transcript view · Icon button (play / pause) · Outlined button (Yes / No) · Large button · Dialog (delete recording) · Snackbar |
| Primary action | **Try again** → SCR-SESS-01 on the same activity; the result compares with this attempt (SRS-FR-SESS-008) |
| Secondary actions | Play / pause · Was this accurate? · How we measured this → SCR-SESS-02 · Open activity → SCR-PRAC-03 · Delete recording (RECOMMENDATION; no SRS requirement, see I-5) |
| Loading | Skeleton after 300 ms. |
| Empty | N/A — always opened with an attempt. |
| Error | Error state + Try again. Missing recording file: "Recording isn't available", other content still shown. |
| Permission | N/A (playback needs no permission). |
| Offline | Identical to default. |
| Navigation | Back / Up: the screen it was opened from. Tab: Progress highlighted (Practice when opened from SCR-PRAC-03). |
| Owning FS | FS-08 (replay and retry: FS-04; feedback: FS-05) |
| Requirements | SRS-FR-PROG-004; SRS-FR-SESS-008, -009; SRS-FR-EVAL-009, -015, -016; SRS-BR-022, -026 |

#### SCR-PROG-05 — Check-ins (R2)

| Field | Specification |
|---|---|
| Release | R2 |
| Purpose | Compare a repeatable benchmark attempt every 2–4 weeks † to show change over time (PRD-FR-075). |
| Primary persona(s) | Sofia, Aarav. |
| Entry points | SCR-PROG-01 Check-ins card · a check-in step offered in today's session (SRS-FR-PROG-008). |
| Main content (top → bottom) | (1) Top app bar (detail): "Check-ins". (2) One line: same prompt each time. (3) Next check-in date. (4) Comparison per headline metric (Line chart, points only). (5) List of past check-ins → SCR-PROG-04. (6) Start check-in when due. |
| Components | Top app bar (detail) · Card · Line chart · Metric tile · Evidence badge · List item · Large button · Empty state |
| Primary action | **Start check-in** (when due) → SCR-SESS-01; otherwise open a check-in → SCR-PROG-04 |
| Secondary actions | Open a past check-in |
| Loading | Skeleton after 300 ms. |
| Empty | "Your first check-in is your starting point." + Start check-in. |
| Error | Error state + Try again. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-PROG-01. Tab: Progress highlighted. |
| Owning FS | FS-08 |
| Requirements | SRS-FR-PROG-008 |

### 4.7 Profile and settings (Detail screens on the current tab's stack, N-5)

#### SCR-PROF-01 — Profile

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show the user's name, learning settings and key statistics, and lead to Settings (one tap from every tab, UXF-09). |
| Primary persona(s) | All. |
| Entry points | Avatar in the top app bar of every primary destination. |
| Main content (top → bottom) | (1) Top app bar (detail): "Profile". (2) Header: initials avatar, display name (or "Add your name"), "Member since {month year}". (3) Learning: Goal, Level, Daily time. (4) Statistics: current streak, total speaking time (measured), valid attempts, words at Review stage (DES-05 §12.3 term). New users see "No practice yet", not zeros. (5) Account line: "Not signed in · Back up your progress" or "Signed in as {email}". (6) Settings row. |
| Components | Top app bar (detail) · Avatar · List item · Bottom sheet (edit: Text field + Radio groups) · Section header · Streak counter |
| Primary action | **Edit profile** → edit sheet (name, goal, level, daily time) |
| Secondary actions | Settings → SCR-SET-01 · Account line → SCR-SET-07 |
| Loading | Skeleton after 300 ms. |
| Empty | N/A — a profile always exists (SRS-FR-ACC-001). |
| Error | Load fails: Error state + Try again (fixes the dead end, UXF-51). Save fails: inline error in the sheet, input kept. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: the tab screen it was opened from. Tab: the originating tab stays highlighted. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-001; SRS-BR-025 |

#### SCR-SET-01 — Settings home

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | One list of the seven user-facing settings groups (replaces the 29-control Settings with developer diagnostics first, UXF-21). |
| Primary persona(s) | All. |
| Entry points | SCR-PROF-01 Settings row. |
| Main content (top → bottom) | (1) Top app bar (detail): "Settings". (2) Seven rows, each with a one-line summary of the current value: Learning · Reminders · Speech & privacy · Appearance · Data & privacy · Account (with sync status) · About. |
| Components | Top app bar (detail) · List item (chevron) |
| Primary action | **Open a section** |
| Secondary actions | — |
| Loading | N/A — summaries are local; skeleton rule applies if slow. |
| Empty | N/A — fixed sections. |
| Error | A summary cannot be read: that row shows no summary; the section still opens. |
| Permission | N/A |
| Offline | Identical; the Account row summary reads "Waiting for connection" when sync is pending. |
| Navigation | Back / Up: SCR-PROF-01. Tab: originating tab highlighted. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-002 |

#### SCR-SET-02 — Learning preferences

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Change the onboarding choices and other learning preferences at any time (PRD-FR-014) without losing progress (SRS-BR-025). |
| Primary persona(s) | Aarav (changes daily time), Hoang (changes level). |
| Entry points | SCR-SET-01 Learning. |
| Main content (top → bottom) | (1) Top app bar (detail): "Learning". (2) Goal (track). (3) Level. (4) Daily time. (5) New words per day (0–5, default 2 †, SRS-BR-027). (6) Countdown before recording (default off, OQ-DS-8). (7) "Show tips again" (replays contextual first-use tips, DES-06 §2.4; HRC-28). |
| Components | Top app bar (detail) · List item → Bottom sheet with Radio group · Switch row · Snackbar |
| Primary action | **Change a preference** (row) — changes save immediately |
| Secondary actions | Show tips again |
| Loading | N/A — local. |
| Empty | N/A |
| Error | Save fails: Snackbar "Couldn't save your settings. Try again." and the previous value is restored (SRS-ERR-005). |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-SET-01. Tab: originating tab highlighted. After a change that affects today, Snackbar "Today's remaining steps were updated." (SRS-FR-TODAY-007). |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-001; SRS-FR-TODAY-007; SRS-BR-013, -025, -027 |

#### SCR-SET-03 — Reminders

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Turn the daily reminder on or off, set its time and see whether notifications are allowed. |
| Primary persona(s) | Daniel, Aarav. |
| Entry points | SCR-SET-01 Reminders · "Notifications are off" links elsewhere. |
| Main content (top → bottom) | (1) Top app bar (detail): "Reminders". (2) Daily reminder switch row. (3) Time row (M3 time picker in a Dialog). (4) Notification permission row when blocked: "Notifications are off for SpeakCraft" + Open settings. (5) Explanation: "One reminder a day. We won't remind you on days you've already practised." |
| Components | Top app bar (detail) · Switch row · List item · Dialog (time picker) · Notification permission row |
| Primary action | **Daily reminder** switch |
| Secondary actions | Change time · Open settings (when blocked) |
| Loading | N/A — local. |
| Empty | N/A |
| Error | Scheduling fails: Snackbar "Couldn't set your reminder. Try again."; the switch returns to off. |
| Permission | Android 13+: turning the switch on without permission shows SCR-PERM-01 (notifications) **once**, then the system prompt. Denied: switch stays on as a preference, the permission row shows, no repeated prompts (SRS-FR-REM-005). |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-SET-01. Tab: originating tab highlighted. |
| Owning FS | FS-09 |
| Requirements | SRS-FR-REM-001, -002, -005, -006; SRS-BR-019 |

#### SCR-SET-04 — Speech & privacy

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show the verified speech-recognition status and let the user manage the language, offline pack, online-transcription consent and recording retention (PP-1, PP-2). |
| Primary persona(s) | Meera (consent and retention), Hoang (offline pack on an older phone). |
| Entry points | SCR-SET-01 Speech & privacy · Capability notices ("Word checks need a transcript", "Transcript needed for this check") · SCR-SET-06 retention row. |
| Main content (top → bottom) | (1) Top app bar (detail): "Speech & privacy". (2) Status card with the verified tier label — "On-device transcription" / "On-device transcription (no replay)" / "Online transcription (you agreed)" / "Audio-only feedback" (ARC-05 §3) — and one line on what it means. (3) Recognition language (en-US, en-GB, en-IN, en-AU, filtered by support, SRS-FR-SPCH-008). (4) Offline speech pack: Installed / Not installed + Install / Downloading {n} % / Not available on this phone. (5) Online transcription switch (T3 phones only) → consent sheet with the SRS-FR-SPCH-005 text (see I-19). (6) Recording retention: Delete after feedback · 7 days · 30 days (default, D-10) · Keep until I delete (SRS-BR-022). (7) "Delete transcripts when recordings expire" switch (D-24). (8) Privacy information → SCR-SET-09. |
| Components | Top app bar (detail) · Card (status) · Capability notice · List item → Bottom sheet with Radio group · Linear progress · Switch row · Bottom sheet (consent) · Dialog (retention shortening) · Snackbar |
| Primary action | **Install speech pack** when the pack is installable; otherwise no filled button (rows) |
| Secondary actions | Change language · Online transcription on / off · Retention · Transcript deletion · Privacy information |
| Loading | Status check "Checking speech recognition…" (≤ 2 s †); shows no status until verified (SRS-FR-SPCH-003). |
| Empty | N/A |
| Error | Download fails: "Download didn't finish. Check your connection and try again." + Retry. Status check fails: "We couldn't check speech recognition." + Try again. Shortening retention that would delete existing recordings: Dialog "Delete {n} recordings older than 7 days?" (RECOMMENDATION). |
| Permission | Microphone off for SpeakCraft: the status card adds "Microphone is off for SpeakCraft" + Open settings (detection itself needs no permission). |
| Offline | Install and online-transcription consent show "Needs a connection" and are disabled; everything else works. |
| Navigation | Back / Up: SCR-SET-01 (or the screen that opened it). Tab: originating tab highlighted. |
| Owning FS | FS-10 (tier, pack and consent: FS-12) |
| Requirements | SRS-FR-SET-003; SRS-FR-SPCH-003, -004, -005, -006, -008; SRS-BR-022, -023 |

#### SCR-SET-05 — Appearance

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Theme, reduced motion and text-size guidance. |
| Primary persona(s) | Sofia (dark theme), Hoang (large text). |
| Entry points | SCR-SET-01 Appearance. |
| Main content (top → bottom) | (1) Top app bar (detail): "Appearance". (2) Theme: System (default) / Light / Dark. (3) Reduce motion: Follow system (default) / On / Off (DES-05 §8.3). (4) "Text size follows your phone's font size" + Open display settings. (5) Vibration feedback switch (pending OQ-DS-5). |
| Components | Top app bar (detail) · Radio group · List item · Switch row |
| Primary action | **Change a preference** — applies immediately |
| Secondary actions | Open display settings |
| Loading | N/A — local. |
| Empty | N/A |
| Error | Save fails: Snackbar + previous value restored. |
| Permission | N/A |
| Offline | Identical to default. |
| Navigation | Back / Up: SCR-SET-01. Tab: originating tab highlighted. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-007 |

#### SCR-SET-06 — Data & privacy (export, clear, retention)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Let users exercise their data rights: export all personal data and clear history (UXF-52). |
| Primary persona(s) | Meera, Sofia. |
| Entry points | SCR-SET-01 Data & privacy · Export link on SCR-ACC-02 *Explain*. |
| Main content (top → bottom) | (1) Top app bar (detail): "Data & privacy". (2) Export my data (with the last export date if any). (3) Clear history. (4) Recording retention summary row ("Recordings are kept for 30 days") → SCR-SET-04 (see I-3). (5) Space used by recordings (computed). (6) Privacy information → SCR-SET-09. |
| Components | Top app bar (detail) · Large button · Bottom sheet (export options: Switch row "Include my recordings (ZIP)" off by default, warning text) · Button loading · List item · Bottom sheet (clear history: Radio group) · Dialog (confirm, destructive text button) · Snackbar |
| Primary action | **Export my data** → export sheet → system share sheet (JSON always; ZIP option per D-22 consolidated recommendation; Pending) |
| Secondary actions | Clear history (Recordings only · Attempts and transcripts · Word progress · Everything) · Retention → SCR-SET-04 · Privacy information |
| Loading | Export in progress: Button loading, then the share sheet. Clearing: progress inside the confirmation Dialog (DES-05 §11.5). |
| Empty | No recordings: the ZIP option reads "No recordings to include" and is disabled. Nothing to clear: the category shows "Nothing to delete". |
| Error | Export fails (e.g. storage full): "Couldn't create the export file. Free some space and try again." + Try again. Clearing fails: Error in the Dialog + Try again; nothing reported as deleted unless it was (SRS-ERR-005). |
| Permission | N/A (FileProvider share needs no permission). |
| Offline | Export and local clearing work offline. Signed-in deletions are queued for the account: "We'll finish deleting from your account when you're online." (SRS-FR-SET-005). |
| Navigation | Back / Up: SCR-SET-01 (or SCR-ACC-02 when opened from there). Tab: originating tab highlighted. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-004, -005; SRS-PRIV-003; SRS-BR-022 |

#### SCR-SET-07 — Account (sign-in state, sync status, sign out)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Show whether the user is signed in and synced, and let them sign in, sign out, delete the account or delete everything on the device. |
| Primary persona(s) | Aarav (two devices), Daniel (new phone); Meera (deletion). |
| Entry points | SCR-SET-01 Account · SCR-PROF-01 account line. |
| Main content (top → bottom) | **Local:** "Using SpeakCraft without an account", benefits of signing in, Sign in with Google, Delete everything on this device. **Signed in:** name, email, sync status row — Up to date / Syncing / Waiting for connection / Error — Retry (SRS-FR-DATA-005) — Sign out, Delete account. **Signed out with kept data:** "Signed out — your data is on this phone only", Sign in with Google, Delete everything on this device (FS-01 §9). |
| Components | Top app bar (detail) · List item · Large button · Text button · Assist chip (sync "Retry") · Dialog (sign-out with Radio group; delete everything) · Offline row |
| Primary action | Local and signed-out → **Sign in with Google** → SCR-ACC-01. Signed in: no filled button (destructive actions are never filled, DES-05 §11.1). |
| Secondary actions | Sign out → Dialog: "Keep my practice data on this device" / "Remove it from this device", pre-selection per **D-21** (Remove when fully synced; with un-uploaded changes, the "{n} changes haven't backed up yet" warning and Keep pre-selected) · Delete account → SCR-ACC-02 · Delete everything on this device (local / signed-out) · Retry sync |
| Loading | Skeleton rows while the profile loads. |
| Empty | N/A — a profile always exists. |
| Error | Sync error: status row "Error — Retry" (SRS-ERR-006); never blocks. |
| Permission | N/A |
| Offline | Sync status "Waiting for connection". Sign in opens SCR-ACC-01 with its offline message. Delete account opens SCR-ACC-02, which blocks start while offline (FS-01 E8). |
| Navigation | Back / Up: SCR-SET-01. Tab: originating tab highlighted. After *Remove* or *Delete everything*: a new local profile and SCR-ONB-01. |
| Owning FS | FS-01 (sync status: FS-11) |
| Requirements | SRS-FR-ACC-002, -005, -006, -007, -009; SRS-FR-DATA-005 |

#### SCR-SET-08 — About

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Version, legal documents, licences, help and diagnostics sharing — without developer jargon (UXF-53). |
| Primary persona(s) | All. |
| Entry points | SCR-SET-01 About. |
| Main content (top → bottom) | (1) Top app bar (detail): "About". (2) Version name and code. (3) Open-source licences (incl. the font licence, DES-05 §4.1). (4) Privacy policy. (5) Terms. (6) Help and contact. (7) Share diagnostics (PII-free log through the share sheet, ADR-007). |
| Components | Top app bar (detail) · List item · Bottom sheet (licence text) |
| Primary action | **Open an item** |
| Secondary actions | Share diagnostics |
| Loading | N/A — local. |
| Empty | N/A |
| Error | Diagnostics file cannot be created: Snackbar "Couldn't prepare the diagnostics file. Try again." |
| Permission | N/A |
| Offline | Identical, except privacy policy, terms and help open in the browser and need a connection. |
| Navigation | Back / Up: SCR-SET-01. Tab: originating tab highlighted. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-008 |

#### SCR-SET-09 — Privacy information

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Describe exactly what is stored on the phone, what syncs when signed in, what never leaves the phone, and how the current tier behaves — generated from the same constants that drive behaviour (PP-2; UXF-60). |
| Primary persona(s) | Meera, Sofia. |
| Entry points | SCR-SET-04, SCR-SET-06 · "More about privacy" on SCR-ONB-06 · privacy link on SCR-ACC-01. |
| Main content (top → bottom) | (1) Top app bar (detail): "Privacy information". (2) On this phone: recordings (retention value), transcripts, practice history. (3) If you sign in: what backs up (metrics, plans, word progress); transcripts and recordings do not. (4) Never leaves the phone: recordings, file paths, transcripts by default. (5) Speech recognition on this phone: tier-specific statement (T3a names the speech service). (6) Device backup: what Android backup includes (D-23). (7) Links you open: "Watch the original lesson" opens YouTube, which has its own privacy policy; SpeakCraft sends nothing to YouTube beyond opening the link (SRS-FR-PRAC-009). (8) Full privacy policy (link). |
| Components | Top app bar (detail) · Section header · List item · Text button |
| Primary action | None — a reading screen; **Open privacy policy** is the only button |
| Secondary actions | — |
| Loading | N/A — generated locally. |
| Empty | N/A |
| Error | N/A — no data operation. |
| Permission | N/A |
| Offline | Identical; the policy link needs a connection. |
| Navigation | Back / Up: the screen that opened it. Tab: originating tab highlighted; hidden when opened from onboarding or sign-in. |
| Owning FS | FS-10 |
| Requirements | SRS-FR-SET-006; SRS-PRIV-001…005 |

### 4.8 Account (full-screen tasks; owning FS-01)

#### SCR-ACC-01 — Sign in

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Optional Google sign-in to back up and sync progress, with a true statement of what backs up and what stays on the phone (D-05; UXF-01). |
| Primary persona(s) | Aarav (two devices), Daniel (changing phone). |
| Entry points | SCR-SET-07 Sign in with Google · SCR-ONB-01 "Already use SpeakCraft? Sign in" (I-7) · SCR-PROG-01 "Back up your progress" card. |
| Main content (top → bottom) | (1) Full-screen top bar with Close. (2) Title "Back up your progress". (3) What backs up: profile, preferences, attempt metrics, plans, word progress. (4) What stays on this phone: recordings, and transcripts by default (text from policy constants, SRS-PRIV-002). (5) Privacy link → SCR-SET-09. (6) Continue with Google; Not now. |
| Components | Top app bar (full-screen) · List item · Text button · Large button with Button loading · Credential Manager sheet (system) · Error state · Offline row |
| Primary action | **Continue with Google** → Credential Manager → success state "You're signed in as {email}. Your progress backs up when you're online." + Done |
| Secondary actions | Not now / Close (returns to the origin) · Privacy information |
| Loading | "Signing in…"; buttons disabled (no double submission, HRC-15). |
| Empty | N/A — static content. |
| Error | No Google account (FS-01 E2) or sign-in failure (E3): plain message + Retry; never error codes (SRS-ERR-007). Cancelling the sheet returns to the previous screen with no error text (SRS-FR-ACC-005). Data adoption fails (E4): "Sign-in couldn't finish. Your data is safe on this phone." + Retry. |
| Permission | N/A — no runtime permission. |
| Offline | Offline row "You're offline. Connect to sign in — you can keep practising without an account." Continue disabled (FS-01 E1). |
| Other states | Merge line after sign-in: "We combined the progress on this phone with your account." (FS-01 A2) · Account conflict: "This phone has practice data from another account. Remove it and continue?" — Remove and continue / Cancel (FS-01 A4). |
| Navigation | Back / Close: origin screen (no sign-in). After Done: origin screen. Tab: hidden. |
| Owning FS | FS-01 |
| Requirements | SRS-FR-ACC-002, -003, -004, -005; SRS-ERR-007 |

#### SCR-ACC-02 — Delete account (explain → confirm → re-auth → progress → done)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Delete the account and all associated data, completely, resumably and verifiably (PRD-FR-005; UXF-52). |
| Primary persona(s) | Meera, Sofia. |
| Entry points | SCR-SET-07 Delete account (signed in only). |
| Main content (top → bottom) | **Explain:** what is deleted in the cloud and on this phone, that it cannot be undone, "Export your data first" link → SCR-SET-06; Continue. **Confirm:** Text field "Type DELETE to confirm" (pending FS-01 Q1), Delete my account. **Re-auth:** Credential Manager sheet. **Progress:** step list Cloud data → Account → This phone, current step announced (polite live region). **Done:** "Your account and all your SpeakCraft data have been deleted." Continue. |
| Components | Top app bar (full-screen) · List item · Text button · Text field · destructive Filled button (`error` container — the only allowed use, DES-05 §11.1) · Credential Manager sheet · Processing indicator · Large button · Error state · Offline row |
| Primary action | Per step: Explain → **Continue** · Confirm → **Delete my account** (enabled only when the field reads DELETE) · Done → **Continue** (fresh SCR-ONB-01) |
| Secondary actions | Export your data first · Cancel (before deletion starts) |
| Loading | Progress step list; Back disabled; leaving the app is safe — deletion resumes on next launch (SRS-FR-ACC-008). |
| Empty | N/A |
| Error | Re-auth cancelled or failed: nothing is deleted; back to Confirm (FS-01 E6). Interrupted: deletion state kept; resumes via SCR-SYS-01 (E7). |
| Permission | N/A |
| Offline | Explain shows "Connect to the internet to delete your account." and Continue is disabled (FS-01 E8). |
| Navigation | Back: Explain and Confirm → SCR-SET-07; Progress → disabled; Done → same as Continue. Up: Close (Explain, Confirm only). Tab: hidden. |
| Owning FS | FS-01 |
| Requirements | SRS-FR-ACC-007, -008 |

### 4.9 System

#### SCR-PERM-01 — Permission education (microphone / notifications)

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Explain why a permission is needed and what happens to the data **before** the system prompt, outside onboarding (HRC-17; UXF-36). |
| Primary persona(s) | Hoang (unfamiliar with permission prompts), Meera. |
| Entry points | **Microphone:** first request outside onboarding — e.g. Record in SCR-SESS-01 when the permission was never requested or was reset by the system. **Notifications (Android 13+):** turning the reminder on in SCR-SET-03 when not yet asked (see I-11). Never shown at launch. |
| Main content (top → bottom) | (1) Full-screen top bar with Close. (2) 48 dp icon. (3) Title: "Allow the microphone to practise speaking" / "Get one reminder a day". (4) Why, in one or two sentences. (5) What stays on the phone (microphone variant; from policy constants). (6) Primary button; Not now. |
| Components | Top app bar (full-screen) · Large button · Text button · system permission dialog |
| Primary action | **Allow microphone** / **Allow notifications** → system prompt. Permanently denied → **Open settings** (`ACTION_APPLICATION_DETAILS_SETTINGS`). |
| Secondary actions | Not now → back to the origin, which shows its inline permission card or row |
| Loading | N/A — static. |
| Empty | N/A — static. |
| Error | N/A — permission outcomes are states. |
| Permission | Granted → back to the origin, which continues (Record becomes available). Denied → origin shows the Mic permission card / Notification permission row. Permanently denied → this screen offers Open settings; re-checked on return. |
| Offline | Identical to default. |
| Navigation | Back / Close: origin screen without the permission. Tab: hidden. |
| Owning FS | FS-04 (microphone), FS-09 (notifications) |
| Requirements | SRS-FR-ONB-007 (same pattern outside onboarding); SRS-FR-SESS-010; SRS-FR-REM-005; SRS-ERR-001 |

#### SCR-INBOX-01 — Inbox (R2)

| Field | Specification |
|---|---|
| Release | R2 |
| Purpose | Learning messages only — milestones and weekly summaries; never software-update notices (PRD-FR-084). |
| Primary persona(s) | Daniel. |
| Entry points | Bell with unread badge in SCR-TODAY-01's top app bar. |
| Main content (top → bottom) | (1) Top app bar (detail): "Inbox", "Mark all as read". (2) Messages newest first: title, one line, date, unread marker (text + dot, not colour alone). (3) Message opens inline (expand) or links to the relevant screen (e.g. SCR-PROG-01). |
| Components | Top app bar (detail) · List item · M3 badge · Text button · Snackbar (delete + Undo) · Empty state · Skeleton |
| Primary action | **Open a message** |
| Secondary actions | Mark all as read · Delete (button alternative to swipe, A11Y-20) |
| Loading | Skeleton after 300 ms. |
| Empty | "No messages yet. Milestones and weekly summaries will appear here." |
| Error | Error state + Try again. |
| Permission | N/A (in-app list; not a system notification). |
| Offline | Identical to default (messages are local). |
| Navigation | Back / Up: SCR-TODAY-01. Tab: Today highlighted. |
| Owning FS | FS-09 |
| Requirements | SRS-FR-REM-007 |

#### SCR-SYS-01 — Account-deletion resume / blocking states

| Field | Specification |
|---|---|
| Release | R1 |
| Purpose | Finish an interrupted account deletion **before any other screen** (SRS-FR-ACC-008). |
| Primary persona(s) | Any user whose deletion was interrupted. |
| Entry points | App launch when the persisted deletion state is not *Done*. |
| Main content (top → bottom) | (1) Title "Finishing account deletion…". (2) Step list Cloud data → Account → This phone with the current step. (3) State-specific message and action. |
| Components | Processing indicator · List item · Large button · Offline row · Credential Manager sheet |
| Primary action | Default: none (automatic). **Retry** when offline or after an error · **Sign in again** when re-authentication is required · **Continue** when done (→ SCR-ONB-01). |
| Secondary actions | — |
| Loading | Progress through the steps (polite announcements). |
| Empty | N/A |
| Error | "Something went wrong. We'll try again." + Retry; local-file failures retry on next launch (FS-01 E10). |
| Permission | N/A |
| Offline | "Waiting for a connection to finish deleting your account." + Retry (FS-01 E9; escape route pending FS-01 Q3). |
| Navigation | Back: exits the app (no other screen is reachable until deletion finishes). Up: none. Tab: hidden. |
| Owning FS | FS-01 |
| Requirements | SRS-FR-ACC-008; SRS-ERR-009 |

---

## 5. Consolidated state matrix

**Legend.** **S** = screen-specific behaviour defined in §4 · **P** = global pattern only (§2.4) · **=** = identical to default (works offline, no indicator) · **N/A** = not applicable (reason in §4) · **Mic** / **Notif** = permission handled on this screen.

| Screen | Loading | Empty | Error | Permission | Offline | Other states |
|---|---|---|---|---|---|---|
| SCR-ONB-01 | N/A | N/A | S | N/A | = | — |
| SCR-ONB-02 | N/A | N/A | S | N/A | = | Resumed |
| SCR-ONB-03 | N/A | N/A | S | N/A | = | Resumed |
| SCR-ONB-04 | N/A | N/A | S | N/A | = | Resumed |
| SCR-ONB-05 | N/A | N/A | S | Notif (S) | = | Resumed |
| SCR-ONB-06 | N/A | N/A | N/A | Mic (S) | = | Denied · Permanently denied |
| SCR-ONB-07 | S | N/A | S | N/A | S | T1/T2 · Pack installable · T3a consent · T3b · Downloading |
| SCR-ONB-08 | N/A | N/A | N/A | Mic (S) | S (T3a) | — |
| SCR-TODAY-01 | P | N/A | S | Mic (S) | = | In progress · Done variant · Recovery |
| SCR-TODAY-02 | N/A | N/A | S | N/A | = | Milestone |
| SCR-SESS-01 | S (Processing) | S (no mistakes found) | S | Mic (S) | S (T3a) | Prompt · Prepare · Ready · Recording · Result · Inconclusive · Interrupted · Mic busy · Tier variants |
| SCR-SESS-02 | N/A | S | N/A | N/A | = | All measurements |
| SCR-PRAC-01 | P | S | S | N/A | = | Tracks view · Techniques view |
| SCR-PRAC-02 | P | S | S | N/A | = | Saved mode |
| SCR-PRAC-03 | N/A | N/A | S | N/A | = | — |
| SCR-PRAC-04 | N/A | N/A | S | N/A | S (lesson link) | All exercises practised |
| SCR-WORD-01 | P | S | S | N/A | = (T3b notice) | Done for today |
| SCR-WORD-02 | N/A | N/A | S | N/A | = | Learning · Learned · Due |
| SCR-WORD-03 | N/A | N/A | S | Mic (S) | = (T3b notice) | Learn · Passed · Not yet · Inconclusive · Easier version · Learned |
| SCR-WORD-04 | N/A | S | S | Mic (S) | = (T3b notice) | Intro · Summary |
| SCR-WORD-05 | P | S | S | N/A | = | Edit sheet |
| SCR-WORD-06 | N/A | S | S | N/A | = | Answer feedback · Summary |
| SCR-PROG-01 | P | S | S | N/A | = | Sign-in prompt |
| SCR-PROG-02 | P | S | S | N/A | = | List view |
| SCR-PROG-03 | P | S | S | N/A | = | Filtered empty |
| SCR-PROG-04 | P | N/A | S | N/A | = | Inconclusive attempt · Recording expired · No replay |
| SCR-PROG-05 | P | S | S | N/A | = | Due · Not due |
| SCR-PROF-01 | P | N/A | S | N/A | = | Edit sheet |
| SCR-SET-01 | N/A | N/A | S | N/A | S (sync summary) | — |
| SCR-SET-02 | N/A | N/A | S | N/A | = | — |
| SCR-SET-03 | N/A | N/A | S | Notif (S) | = | Blocked |
| SCR-SET-04 | S | N/A | S | Mic note (S) | S | Downloading · Consent sheet |
| SCR-SET-05 | N/A | N/A | S | N/A | = | — |
| SCR-SET-06 | S | S | S | N/A | S | Export sheet · Clear sheet |
| SCR-SET-07 | P | N/A | S | N/A | S | Local · Signed in · Signed out · Sign-out dialog |
| SCR-SET-08 | N/A | N/A | S | N/A | S (links) | — |
| SCR-SET-09 | N/A | N/A | N/A | N/A | = | Tier-specific text |
| SCR-ACC-01 | S | N/A | S | N/A | S | Success · Account conflict |
| SCR-ACC-02 | S | N/A | S | N/A | S | Explain · Confirm · Re-auth · Done |
| SCR-PERM-01 | N/A | N/A | N/A | Mic / Notif (S) | = | Permanently denied |
| SCR-INBOX-01 | P | S | S | N/A | = | Unread · Read |
| SCR-SYS-01 | S | N/A | S | N/A | S | Re-auth needed · Done |

**Coverage check.** 42 of 42 screens define all five states (UX-7; DES-01 HRC-37). Offline is "S" only on the 12 screens where a feature needs the network or a link leaves the app; every other screen works unchanged offline (PP-6).

## 6. Component inventory (component → screens)

| Component (DES-05) | Screens |
|---|---|
| Top app bar — primary | TODAY-01, PRAC-01, WORD-01, PROG-01 |
| Top app bar — detail | PRAC-02, PRAC-03, PRAC-04, WORD-02, WORD-05, PROG-02…05, PROF-01, SET-01…09, INBOX-01 |
| Top app bar — full-screen | ONB-02…08, TODAY-02, SESS-01, WORD-03, WORD-04, WORD-06, ACC-01, ACC-02, PERM-01 |
| Navigation bar / rail | Tab roots and Detail screens (hidden on Full-screen tasks) |
| Avatar | TODAY-01, PRAC-01, WORD-01, PROG-01, PROF-01 |
| Large / Filled button | ONB-01…08, TODAY-01, TODAY-02, PRAC-03, PRAC-04, WORD-01…04, WORD-06, PROG-04, PROG-05, SET-04, SET-06, SET-07, ACC-01, ACC-02, PERM-01, SYS-01 |
| Destructive filled button (`error`) | ACC-02 (final confirmation only) |
| Tonal button | SESS-01, PRAC-03, WORD-02, WORD-03, WORD-06 |
| Outlined button | SESS-01, PROG-04 ("Was this accurate?") |
| Text button | ONB-01, ONB-05…08, TODAY-01, TODAY-02, SESS-01, SESS-02, PRAC-03, PRAC-04 ("Watch the original lesson"), WORD-03, PROG-01, PROG-02, SET-07, SET-09, ACC-01, ACC-02, PERM-01, INBOX-01 |
| Icon button | TODAY-01, SESS-01, SESS-02, PRAC-01, PRAC-02, PRAC-03, PRAC-04, WORD-02, WORD-05, PROG-02, PROG-04 (+ every Metric tile info button) |
| Button loading | SET-06, ACC-01 |
| Text field | WORD-01, WORD-05, PROF-01, ACC-02 |
| Card | TODAY-01 (session card), TODAY-02, PRAC-01 (track and technique cards), PRAC-02 (activity cards), WORD-01, WORD-04, WORD-06, PROG-01, PROG-05, SET-04 |
| List item | All screens except SESS-01 and WORD-06 |
| Dialog | ONB-05 and SET-03 (time picker), SESS-01 (leave), WORD-06 (leave), PROG-04 (delete recording), SET-04 (retention), SET-06 (clear), SET-07 (sign out, delete everything) |
| Bottom sheet | TODAY-01, SESS-01 (tips), SESS-02, WORD-05, PROF-01, SET-02, SET-04 (language, retention, consent), SET-06 (export, clear), SET-08 |
| Filter chip | SESS-01 (accuracy reasons), PRAC-01 (Techniques view), PRAC-02, WORD-01, WORD-05, PROG-03 |
| Segmented button | SESS-01, WORD-03 (speech speed), PROG-01, PROG-02 (period), PRAC-01 (Tracks · Techniques) |
| Assist chip | SET-07 (Retry sync) |
| Snackbar | TODAY-01, SESS-01, PRAC-02, PRAC-03, PRAC-04, WORD-05, PROG-04, SET-02…06, SET-08, INBOX-01 |
| Step progress | ONB-02…08, SESS-01 (incl. WORD-04 items), WORD-06 |
| Linear progress | ONB-07, SET-04 (speech pack) |
| Processing indicator | ONB-07, SESS-01, ACC-02, SYS-01 |
| Skeleton | TODAY-01, PRAC-01, PRAC-02, WORD-01, WORD-05, PROG-01…05, PROF-01, SET-07, INBOX-01 |
| Record button (+ level meter, timer) | SESS-01 (also used by ONB-08, WORD-03, WORD-04 through the player) |
| Metric tile | TODAY-02, SESS-01, SESS-02, PROG-01, PROG-02, PROG-04, PROG-05 |
| Evidence badge | TODAY-02, SESS-01, SESS-02, WORD-02, WORD-03, PROG-01…05 |
| Feedback card | SESS-01; PROG-04 (read-only variant) |
| MistakeList (DES-05 §11.15a) | SESS-01, WORD-03, WORD-04 (items), PROG-04 |
| CoachTipCard (DES-05 §11.15b) | SESS-01 (1–3 tips), WORD-03, WORD-04 (1 tip per item), PROG-04 |
| SelfCheckList (DES-05 §11.15c) | SESS-01 (technique activities whose technique has aspects audio cannot show) |
| Inconclusive card | SESS-01, WORD-03, WORD-04, PROG-04 |
| Streak counter | TODAY-01, TODAY-02, PROG-01, PROF-01 |
| Streak week strip | TODAY-01, TODAY-02 |
| Streak month grid | PROG-01 |
| Line chart | PROG-01 (mini), PROG-02, PROG-05 |
| Bar chart | Not placed in R1 by this inventory; available to FS-08 if speaking time per day is added to PROG-01 |
| Empty state | PRAC-01 (Saved techniques), PRAC-02, WORD-01, WORD-04, WORD-05, WORD-06, PROG-01, PROG-02, PROG-03, PROG-05, INBOX-01 |
| Error state | Every screen whose Error row is "S" in §5 |
| Offline row | ONB-07 ("Needs a connection" labels), ONB-08, SESS-01, PRAC-04 (lesson link), SET-04, SET-07, ACC-01, ACC-02, SYS-01 |
| Mic permission card | ONB-06, ONB-08, TODAY-01, SESS-01, WORD-03, WORD-04 |
| Notification permission row | SET-03 |
| Capability notice | ONB-07, TODAY-01, SESS-01, PRAC-03, WORD-01, WORD-03, WORD-04, SET-04 |
| Prompt card | ONB-08, SESS-01, PRAC-03, WORD-03 |
| Transcript view | SESS-01, PROG-04 |
| Word card | WORD-02, WORD-03 |
| Stage stepper | WORD-02, WORD-03 |
| Switch row | SET-02, SET-03, SET-04, SET-05, SET-06 (ZIP option) |
| Radio group | ONB-02…04, ONB-07, WORD-06, PROF-01 (sheet), SET-02, SET-04, SET-05, SET-06, SET-07 |
| Section header | SESS-02, PRAC-01, PRAC-04, WORD-02, PROG-03, PROF-01, SET-09 |
| Milestone celebration (§8.3) | TODAY-02 |
| M3 time picker | ONB-05, SET-03 |
| M3 badge | TODAY-01 bell, INBOX-01 (R2) |
| System UI: permission dialog / Credential Manager / share sheet | ONB-05, ONB-06, PERM-01 / ACC-01, ACC-02, SYS-01 / SET-06, SET-08 |

## 7. Retired screens (today's routes → replacement or removal)

Based on DES-02 §5 and the route table in `docs/redevelopment/working_papers/ui_audit.md` §1.3. Removal of code happens in R0–R1 (PRD-FR-120).

| # | Current route or surface | Disposition | New home | Note |
|---|---|---|---|---|
| 1 | `dashboard` | Replaced | SCR-TODAY-01 | One session card instead of ~21 widgets (UXF-19) |
| 2 | `daily_practice` | Merged | SCR-TODAY-01 (+ SCR-SESS-01) | One plan; steps complete from valid attempts, not ticks (UXF-05, UXF-24) |
| 3 | `learning_paths` | Replaced | SCR-PRAC-01 | Tracks replace programs; no day counts (UXF-25) |
| 4 | `vocabulary` | Replaced | SCR-WORD-01…04 (R2: -05, -06) | Five-stage lifecycle; "AI Quiz" becomes "Quiz" (R2) |
| 5 | `speaking_lab` | Replaced | SCR-SESS-01; history → SCR-PROG-03 | Video and upload removed from R1 (ADR-010) |
| 6 | `challenges` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Becomes activities; shared Firestore challenges dropped |
| 7 | `articulation_lab` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Read aloud / Listen & repeat in the Clear speech track |
| 8 | `pronunciation` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Same composable as #7; no "pronunciation score" (NG-03) |
| 9 | `voice_training` | Replaced | SCR-PRAC-04 / SCR-PRAC-03 → SCR-SESS-01 | Technique content re-expressed in our own words (D-08 decided; SRS-BR-028); "5 Vocal Foundations" naming dropped |
| 10 | `conversation` | Removed | — (R2 scripted role-play in Practice, SRS-FR-PRAC-007) | Not a chatbot (NG-01) |
| 11 | `listening_lab` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Listen & repeat template |
| 12 | `grammar_speaking` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Grammar observations only from high-precision rules (SRS-FR-EVAL-006) |
| 13 | `public_speaking` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | Presentations & storytelling track |
| 14 | `real_world_tasks` | Removed | — | Self-ticked off-app tasks dropped (DES-02 §5) |
| 15 | `module_lessons/{slug}` | Replaced | SCR-PRAC-02 (tracks); SCR-PRAC-01 Techniques collection | Modules become tracks and techniques (D-08 decided) |
| 16 | `lesson_detail/{lessonId}` | Replaced | SCR-PRAC-04 Technique detail → SCR-PRAC-03 | Lessons become techniques written in our own words, each linked to its specific source video; no "Mark Complete"; no creator name (SRS-BR-028) |
| 17 | `youtube_library` (the "@askvinh" tab) | Replaced | **Replaced by the Techniques collection in Practice (D-08 decided)**: SCR-PRAC-01 Techniques view + SCR-PRAC-04 | The ingestion tool and the creator-named tab are removed (ADR-011): no scraping, caption download or API key. The techniques are kept, re-verified and re-expressed in our own words, with "Watch the original lesson" links to the specific videos (DES-02 v0.2 §5; PRD-FR-056/057). |
| 18 | `analytics` | Replaced | SCR-PROG-01…04 | Evidence only; no self-ratings as "objective" (UXF-58) |
| 19 | `ai_mentor` | Removed | — | NG-01; no "AI" labels (UXF-54) |
| 20 | `app_updates` | Removed | — | ADR-004 / D-03 |
| 21 | `admin_console` | Removed | — | NG-10 |
| 22 | `settings` | Replaced | SCR-SET-01…09 | Seven user-facing groups; diagnostics → About → Share diagnostics |
| 23 | `profile` | Replaced | SCR-PROF-01 | Reachable from every tab's avatar |
| 24 | `activity_library` | Replaced | SCR-PRAC-01, SCR-PRAC-02 | Saved → Saved mode of SCR-PRAC-02 |
| 25 | `activity_library/{moduleId}` | Removed | — (SCR-PRAC-02 covers it) | Orphan route today (never built) |
| 26 | `activity/{activityId}` | Replaced | SCR-PRAC-03 → SCR-SESS-01 | One player and one feedback card |
| — | `AuthScreen` (mandatory sign-in) | Replaced | SCR-ONB-01 (no account needed); optional SCR-ACC-01 | D-05 |
| — | `OnboardingAssessmentScreen` (3 form steps, 7 sliders) | Replaced | SCR-ONB-01…08 | First attempt inside onboarding; no self-ratings |
| — | Profile loading / error gate | Replaced | Launch decides the start destination before the first frame; errors have Try again | UXF-51 |
| — | Launch overlay (2.1–2.9 s) | Replaced | Platform splash ≤ 1 s (DES-06 §2.3) | UXF-02 |
| — | `UpdateAvailableDialog` | Removed | — | No dialogs at launch (UXF-11) |
| — | `FeatureTutorialDialog`, `TutorialOverlay`, `FeatureGuideStrip` | Replaced | Contextual first-use tips (DES-06 §2.4) | UXF-14, UXF-30 |
| — | `NotificationDrawer` (+ dead sheet) | Replaced | System reminders (SCR-SET-03) + R2 SCR-INBOX-01 | No update notices |

## 8. Issues found in source documents

Each issue is raised to the owner of the source document; none is fixed here.

| # | Issue | Source(s) | Suggested resolution |
|---|---|---|---|
| I-1 | The working-paper IA summary uses outdated screen IDs (SCR-ONB-01…06, SCR-SET-01…07, SCR-WORD-02 "learn flow", -03 "review", R2 -04/-05) that conflict with DES-02 §4. DES-03 follows DES-02. | PLAN_AND_CANON "IA (DES-02)" vs DES-02 §4 | Mark the working-paper list as superseded by DES-02 §4. |
| I-2 | DES-02 v0.2 lists 38 R1 screen IDs, while KPI-11 targets "≤ 30 screens"; DES-02 says "≤ 30 distinct layouts" but no counting rule exists. DES-03 §3 finds 16 layout families. | BRD KPI-11; DES-02 §4 | State in BRD that KPI-11 counts distinct layouts (or raise the target). |
| I-3 | Recording retention has two homes: SRS puts the selector in Speech & privacy; DES-02 puts "retention" in Data & privacy. DES-03 places the control in SCR-SET-04 and a summary row in SCR-SET-06. | SRS-FR-SET-003; DES-02 §4 (SCR-SET-06) | Confirm one control in SCR-SET-04; update DES-02 §4 label. |
| I-4 | DES-05 requires "exactly 1" filled button per screen, but browse and settings screens have no single task. | DES-05 §11.1, DS-2 | Change to "exactly one on task screens; at most one elsewhere". |
| I-5 | DES-05 specifies "Delete this recording?" and a "Recording deleted" Snackbar with Undo, but no SRS requirement covers deleting one recording; Undo for a file deletion needs deferred deletion. | DES-05 §11.5, §11.10; SRS-FR-SET-005 | Add an SRS-FR (FS-08/FS-10) or drop the per-attempt action. |
| I-6 | DES-02 shows "Saved" as a filter on SCR-PRAC-02, which is a single track, but saved activities span tracks. DES-03 uses a cross-track Saved mode of SCR-PRAC-02 opened from SCR-PRAC-01. | DES-02 §3, §4; SRS-FR-PRAC-006 | Confirm the Saved mode in DES-02 and FS-06. |
| I-7 | DES-02 lists onboarding as a sign-in entry point, but no onboarding screen holds it (also FS-01 issue 4, FS-02 issue 1). DES-03 uses a secondary action on SCR-ONB-01. | DES-02 §2.2; SRS-FR-ACC-002 vs SRS-FR-ONB-001 | Decide FS-01 Q4; update SRS-FR-ONB-001. |
| I-8 | SRS uses the status and filter "Mastered", which DES-05 bans as a UI term; SRS-FR-PROG-001 says "words learned". | SRS-FR-WORD-001, -010, SRS-BR-017, SRS-FR-PROG-001; DES-05 §12.3 | Choose user-facing labels in FS-07 (e.g. "Long-term"), keep "Mastered" internal. **Resolved (consistency review, CL-62):** visible label "Long-term review"; the internal status keeps the name MASTERED (DES-05 §12.3; FS-07). |
| I-9 | Inconclusive actions differ: SRS says "Retry / Skip"; DES-05 says "Try again" + "Tips for a clear recording". Whether a skipped step lets the session complete is open (FS-03, DES-07 OQ-A11Y-1). | SRS-FR-SESS-007, SRS-BR-009; DES-05 §11.16 | Use "Try again"; define Skip (Today only) and its effect on completion in FS-03. |
| I-10 | SRS says *Today* shows a completion state; DES-02 has a separate SCR-TODAY-02. DES-03 uses both: SCR-TODAY-02 at the end of the session, then a Done variant of SCR-TODAY-01. | SRS-FR-TODAY-011; DES-02 §4 | Confirm in FS-03. |
| I-11 | DES-02 lists SCR-PERM-01 for notifications, but onboarding requests the notification permission directly after "Remind me" and DES-05 describes no education screen. DES-03 uses SCR-PERM-01 for notifications only from Settings, once. | DES-02 §4; SRS-FR-ONB-005, SRS-FR-REM-005; DES-05 §11.19 | Confirm in FS-09. |
| I-12 | SRS lists sign-in entry points as onboarding and Settings → Account only; DES-02 and FS-01 add the Progress prompt. | SRS-FR-ACC-002; DES-02 §2.2; FS-01 A7 | Add the Progress entry to SRS-FR-ACC-002. |
| I-13 | Profile is reachable from all four tabs, but DES-02 does not say whose back stack it joins or whether the bar stays visible. DES-03 pushes it onto the current tab (N-5). | DES-02 §2.2 | Confirm in DES-02 and ARC-02 navigation graph. |
| I-14 | FS-02 presents speech-check choices as separate buttons; this breaks the one-primary-action rule and can nudge consent. DES-03 uses a radio group (no pre-selection for consent) + Continue. | FS-02 §5.1; DES-05 DS-2; PP-2 | Align FS-02 and FS-12. |
| I-15 | FS-02 says the progress indicator covers steps 2–8 (7 segments) but its accessibility note announces "Step 3 of 8". | FS-02 §5, §14 | Use "Step n of 7" throughout. |
| I-16 | Processing timeouts differ: DES-05 shows "taking longer" + Cancel at 10 s; SRS shows Error + Retry at 2× the evaluation budget (5 s RD-MID, 10 s RD-LOW). | DES-05 §11.11; SRS-FR-EVAL-013; NFR-PERF-005 | Define one rule in FS-04/FS-05 (e.g. notice at 2× budget, Error at 10 s). |
| I-17 | **Techniques placement (DES-02 v0.2).** DES-02 puts the Techniques collection "on SCR-PRAC-01" but does not say how it sits beside the tracks, where saved techniques appear, or what "Practise it" opens. DES-03 uses a Tracks · Techniques segmented view on SCR-PRAC-01 (with a Saved chip) so that Practice → Technique → Activity → Session stays within depth 3, and originally made "Practise it" start the next exercise directly in SCR-SESS-01 while exercise rows open SCR-PRAC-03. **Resolved (consistency review, CL-53):** DES-02 §3/§4 is authoritative — "Practise it" and exercise rows both open SCR-PRAC-03, then Start → SCR-SESS-01 (SCR-PRAC-04 §4.4 updated). A Saved mode on SCR-PRAC-02 that listed techniques would break depth 3. Also: the content-pack schema needs the provenance and `licenceStatus` fields of SRS-FR-PRAC-009. | DES-02 v0.2 §3–§4; SRS-FR-PRAC-008, -009; ARC-03 §6 | Confirm in DES-02 and FS-06; confirm ARC-03 §6 fields. |
| I-18 | Trend threshold differs: SRS requires ≥ 3 valid attempts; DES-05 draws a line from 2 points. | SRS-BR-024, PRD-FR-070; DES-05 §11.18 CH-2 | Align DES-05 CH-2 to 3. |
| I-19 | SRS requires an online-transcription "consent screen", but DES-02 has no ID for it. DES-03 shows it as an outcome of SCR-ONB-07 and a sheet on SCR-SET-04. | SRS-FR-SPCH-005; DES-02 §4 | Confirm the sheet approach or add an ID in DES-02. |
| I-20 | **D-08 decided 2026-11-21 ("original wording, link out").** Some documents still describe creator-derived content as removed rather than re-expressed: DES-01 UXF-07 / UXF-55 responses ("Remove", "original content"), RD-16 NG-08 and §5, RD-15 PP-8 motivation text. DES-05 §12.3 still bans creator names (consistent with SRS-BR-028) and "borrowed framework names" — this needs a note that technique names must be our own wording, while the name "as taught" is kept only in provenance (SRS-FR-PRAC-009). | DES-01 §3.2, §3.12; RD-16; RD-15 PP-8; DES-05 §12.3 | Update those items to cite D-08 (decided) and SRS-BR-028. |
| I-21 | DES-02 §2.3 lists the full-screen tasks as session player, onboarding, sign-in and permission education only. DES-03 also treats SCR-TODAY-02, SCR-WORD-03, -04, -06 and SCR-ACC-02 as full-screen tasks. | DES-02 §2.3 | Extend the DES-02 list. |
| I-22 | The R2 inbox has an unread badge on Today's top bar, but DES-05 defines no badge style (it covers only navigation badges, none in R1). | SRS-FR-REM-007; DES-05 §11.7, OQ-DS-4 | Add a top-bar badge spec to DES-05 before R2. |
| I-23 | **Mistakes found and Coach tips (SRS v0.2).** (a) DES-05 has no component for either; DES-03 proposes **MistakeList** and **CoachTipCard**. (b) The required Result order — transcript → headline metrics → strength → mistakes found → coach tips → Try again → "Was this accurate?" — differs from the DES-05 §11.15 Feedback card order (header, strength, focus, metrics, transcript, rating, actions), and the focus now opens Coach tips. (c) "Mistakes found", "Coach tips" and "Technique" are not in DES-05 §12.2; "Mistakes" sits uneasily with the "never blame" voice rule (§12.1), so each entry needs neutral wording ("We heard 'negotiation'", not "Wrong word"). (d) A result can now carry up to 3 metric tiles, 3 mistakes and 3 tips; at 200 % font this is long, so DES-06/DES-07 need a focus order and collapsed defaults. | SRS-FR-EVAL-015, -016, SRS-FR-WORD-014; PRD-FR-047; DES-05 §11.15, §12.1–12.2 | DES-05 to add both components, update §11.15 order and §12.2 terms; DES-06 to specify layout and collapse rules. **(a)–(c) resolved (consistency review, CL-37/CL-38):** DES-05 v0.2 defines `MistakeList`, `CoachTipCard` and `SelfCheckList` (§11.15a–c), the §11.15 order and the §12.2 terms. |
| I-24 | **Cross-links into SCR-PRAC-04.** Coach tips (from SCR-SESS-01, SCR-WORD-03/04, SCR-PROG-04) and the "Technique: {name}" link on SCR-PRAC-03 open SCR-PRAC-04 from outside the normal Practice path. DES-02 does not define cross-tab or above-a-full-screen-task navigation, or how to keep the depth-3 rule. DES-03 opens SCR-PRAC-04 above the current screen (Back returns there) and, from SCR-PRAC-03, returns to an existing SCR-PRAC-04 in the stack instead of pushing a second copy. Opening a technique mid-session must not lose the session (completed steps are already saved, SRS-BR-013). | DES-02 §2.3; SRS-FR-EVAL-016 | Define cross-link behaviour in DES-02 and ARC-02. |

## Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-21 | Initial inventory of all 42 DES-02 v0.2 screens (38 R1, 4 R2) with states, components, navigation, state matrix, component inventory, retired routes and source issues. Reflects the Product Owner directions of 2026-11-21: D-08 decided "original wording, link out" (ONB-01, §4.4 Practice incl. new SCR-PRAC-04, SET-08, SET-09, §7 rows 9 and 15–17, I-17, I-20); "Mistakes found" and "Coach tips" on every result (SESS-01, WORD-03, WORD-04, PROG-04, §2.5, §6, I-23, I-24). | UI/UX Designer |
| 0.2 | 2026-11-21 | Consistency review: CL-53 (SCR-PRAC-04 "Practise it" → SCR-PRAC-03 → SCR-SESS-01 per DES-02 §3/§4; SCR-PRAC-03 entry points; I-17), CL-37 (MistakeList, CoachTipCard, SelfCheckList now DES-05 §11.15a–c in §2.5, SESS-01, WORD-03, §6; I-23 resolved), CL-62 (SCR-WORD-01 filter label "Long-term review"; I-8 resolved) | Technical Writer (consistency review) |

