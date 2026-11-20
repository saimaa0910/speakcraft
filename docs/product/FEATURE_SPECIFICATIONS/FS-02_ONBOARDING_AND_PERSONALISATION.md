# FS-02 — Onboarding & Personalisation

| Field | Value |
|---|---|
| Document ID | FS-02 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-02; PRD §15.2 (PRD-FR-010…014), PRD-FR-080/083; SRS §6.2 (SRS-FR-ONB-001…011), SRS-FR-SPCH-001/005/008; NFR-PERF-004/005, NFR-REL-004, NFR-A11Y-*; DES-02 (SCR-ONB-01…08, SCR-PERM-01); FS-01, FS-03, FS-04, FS-05, FS-09, FS-12; decisions D-14, D-17 |

---

## 1. Feature ID & purpose

**FS-02 / CAP-02 (pillar P-1).** Take a new user from first open to a **valid first speaking attempt with real feedback** in about three minutes (KPI-02), while collecting the minimum personalisation (goal/track, level, daily minutes, reminder), explaining privacy, obtaining microphone permission in context and resolving the device's speech capability tier.

## 2. User problem

| Problem | Evidence | Personas |
|---|---|---|
| "I want to know quickly whether this helps me." | Today there is no guided first attempt; tutorial overlay for 24 features (RD-16 §5) | All (UN-01) |
| "Why does it want my microphone and where does my voice go?" | Privacy text contradicts behaviour (FND-SEC-05…07) | P2 Meera, P4 Sofia (UN-08) |
| "The app ignores what I told it I need." | User goal never reaches the dashboard plan (FND-DOM-03) | P1 Aarav, P3 Daniel |
| "My phone is old — will it even work?" | Capability status false positives (FND-AI-05) | P5 Hoang |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | Value screen; goal/track (5); level (5, D-17); daily minutes (5/10/15); reminder choice with Android 13+ notification permission; privacy statement and RECORD_AUDIO request with denial path; speech check with tier-specific choice (FS-12); first attempt `onb-intro` evaluated like any attempt (baseline); skip of first attempt; resume after process death; all choices editable later (FS-10) | Check-in baseline reuse (FS-08, SRS-FR-PROG-008) |
| **Out** | Placement test or adaptive level assessment (see Issue 2); account creation as a required step (D-05); tutorial overlays (RD-16 §5); recognition-locale selection during onboarding (Settings only, RECOMMENDATION) | — |

## 4. Preconditions

- Profile has `onboardingCompletedAt = null` (start destination decided by `ProfileRepository` before first frame, ARC-01 §5).
- Content pack contains activity `onb-intro` (SRS-FR-ONB-009).
- No network required (PRD-FR-100) except for optional pack download / online transcription.

## 5. Main workflow

| Step | Screen | User action | System behaviour |
|---|---|---|---|
| 1 | SCR-ONB-01 Welcome | Reads three value lines; taps **Get started** | — |
| 2 | SCR-ONB-02 Goal | Picks one of 5 goals (pre-selected default, RECOMMENDATION *Workplace communication*); **Continue** | Maps goal → track TRK-EVERYDAY / TRK-WORK / TRK-INTERVIEW / TRK-PRESENT / TRK-CLEAR (SRS-FR-ONB-002); persists |
| 3 | SCR-ONB-03 Level | Picks one of 5 levels with "can do" line; default **Intermediate**; **Continue** | Persists level (SRS-FR-ONB-003) |
| 4 | SCR-ONB-04 Daily time | 5 / **10** / 15 minutes; **Continue** | Persists dailyMinutes (SRS-FR-ONB-004) |
| 5 | SCR-ONB-05 Reminder | **Remind me at 19:00 †** (time editable) or **Not now** | If reminder chosen on Android 13+, the POST_NOTIFICATIONS request is shown (SRS-FR-ONB-005); preference persisted; scheduling per FS-09 |
| 6 | SCR-ONB-06 Privacy & microphone | Reads plain-language statement; taps **Allow microphone** | Statement generated from policy constants (SRS-PRIV-002); RECORD_AUDIO requested (SRS-FR-ONB-006) |
| 7 | SCR-ONB-07 Speech check | Waits ≤ 2 s †; responds to the tier-specific choice (table 5.1) | `SpeechCapabilityService` determines tier (SRS-FR-ONB-008, SRS-FR-SPCH-001) |
| 8 | SCR-ONB-08 First attempt intro | Reads "Introduce yourself — about a minute"; taps **Start** (or **Do this later**) | Opens SCR-SESS-01 with `onb-intro` (TPL-ANSWER, 60 s max, 15 s preparation †) (SRS-FR-ONB-009) |
| 9 | SCR-SESS-01 → Result | Records, sees transcript and feedback (FS-04, FS-05) | Attempt saved like any attempt; first valid `onb-intro` attempt becomes `baselineAttemptId` |
| 10 | Result → Today | Taps **Go to Today** | `onboardingCompletedAt` set; Today generates the first plan (FS-03) |

Each Continue persists the step index before navigating (SRS-FR-ONB-010). Back returns to the previous step with the choice preserved; Back on SCR-ONB-01 exits the app. A segmented progress indicator covers steps 2–8 (RECOMMENDATION).

### 5.1 Speech-check outcomes (SCR-ONB-07) — elaborates SRS-FR-ONB-008; tier logic owned by FS-12

| Detected state | Copy (RECOMMENDATION) | Actions |
|---|---|---|
| T1 candidate (API ≥ 33, pack installed) | "Speech recognition works on this phone without internet." (piping verified at first attempt, ARC-05 §3) | Continue |
| T2 | "Speech recognition works on this phone without internet. Replaying your recordings isn't available on this phone." | Continue |
| Pack installable (API 33+, locale supported, not installed) | "Download the English speech pack so transcription works offline (needs a connection)." | Download (progress, failure + Retry); Use online transcription instead; Continue without transcripts |
| T3a candidate (recogniser exists, not on-device) | Consent text per SRS-FR-SPCH-005 (what is sent, to whom, withdrawable) | Allow online transcription (→ T3a); Not now (→ T3b) |
| T3b (no recogniser) | "This phone can't turn speech into text for SpeakCraft. You'll still get feedback on speaking time and pauses. Some checks need a transcript." | Continue |

### 5.2 Level descriptions (RECOMMENDATION copy; levels per D-17)

| Level | "Can do" line |
|---|---|
| Beginner | "I can say simple phrases about myself, slowly." |
| Elementary | "I can manage short everyday exchanges on familiar topics." |
| Intermediate (default) | "I can talk about work and daily life, but I hesitate or search for words." |
| Upper-intermediate | "I can discuss most topics clearly, with some mistakes." |
| Advanced | "I speak fluently and want to polish pace, pauses and precision." |

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | User taps **Do this later** on SCR-ONB-08 | Onboarding completes; Today opens (SRS-FR-ONB-011); no baseline yet; RECOMMENDATION: Today shows "Introduce yourself" as an optional card until done (Open question Q2) |
| A2 | Microphone denied (first time) | SCR-ONB-06 shows impact ("You can explore SpeakCraft, but speaking practice needs the microphone") + **Try again** + **Continue without microphone** (SRS-FR-ONB-007) |
| A3 | Microphone permanently denied | As A2 but **Open settings** replaces Try again; on return from settings, permission is re-checked |
| A4 | Continue without microphone | Speech check still runs; SCR-ONB-08 shows "Microphone needed" with Allow microphone / Do this later; Today opens with speaking steps disabled (SRS-FR-ONB-007) |
| A5 | Notification permission denied after choosing a reminder | Reminder preference kept; Settings → Reminders shows "Notifications are blocked" with a system-settings link; no repeated prompt (SRS-FR-REM-005) |
| A6 | Process killed mid-onboarding | Relaunch resumes at the last incomplete step with earlier choices kept (SRS-FR-ONB-010); an in-progress recording is discarded (FS-04) |
| A7 | First attempt Inconclusive | Result shows reason + Retry (FS-05); Do this later remains available; baseline set only from a valid attempt |
| A8 | User signs in from SCR-ONB-01 secondary action "Already use SpeakCraft? Sign in" (RECOMMENDATION, see Issue 1) | FS-01 sign-in; if the pulled profile has track/level/minutes, steps 2–5 are pre-filled and skipped; device-specific steps 6–7 always run |
| A9 | Offline-pack download still running at Continue | User proceeds; tier is re-evaluated after installation (SRS-FR-SPCH-003) |

## 7. Error scenarios

| ID | Condition | Behaviour | SRS-ERR |
|---|---|---|---|
| E1 | Microphone denied | A2/A3 | SRS-ERR-001 |
| E2 | Recogniser unavailable/busy during first attempt | Attempt continues as T3b where audio is app-owned; "Transcript wasn't available this time" (FS-04) | SRS-ERR-002 |
| E3 | First attempt evidence insufficient | Inconclusive + reason + Retry (A7) | SRS-ERR-003 |
| E4 | Storage full during first attempt | Recording stopped, partial discarded, storage-settings link (FS-04) | SRS-ERR-004 |
| E5 | Persisting a choice fails | Non-blocking message; retry once; stay on step (never advance on a failed write) | SRS-ERR-005 |
| E6 | Pack download fails / no network | "Download didn't finish. Check your connection and try again." + Retry; other options remain | — (FS-12) |
| E7 | Tier detection exceeds 2 s † | Show provisional T3b copy "We'll check again later"; re-evaluated on next start (SRS-FR-SPCH-003) | SRS-AVL-003 |

## 8. Business rules

| Rule | Application |
|---|---|
| SRS-BR-001 | The first attempt passes the same evidence gate as every attempt (≥ 8 recognised words for TPL-ANSWER †) |
| SRS-BR-008 | An Inconclusive first attempt is not a baseline, completion or streak day |
| SRS-BR-010 | A valid first attempt counts as a streak day |
| SRS-BR-023 | Online transcription only with T3a + consent |
| SRS-BR-025 | Changing level or track later never resets progress |
| SRS-BR-019 | Reminder window rules apply to the chosen time (FS-09) |

## 9. UI states

**SCR-ONB-01…05 (choice and value screens)** — one table, applies to each

| State | What the user sees | Actions |
|---|---|---|
| Default | Question, options with default pre-selected, progress indicator | Continue; Back |
| Loading | N/A — local reads are immediate (< 50 ms, NFR-PERF-008) | — |
| Empty | N/A — options are static | — |
| Error | E5 inline message | Retry |
| Offline | Identical to default (no network needed) | — |
| Permission (SCR-ONB-05 only) | System notification dialog after Remind me (Android 13+) | Allow / Don't allow → A5 |
| Resumed | Screen restored with saved choice (A6) | Continue |

**SCR-ONB-06 Privacy & microphone**

| State | What the user sees | Actions |
|---|---|---|
| Default | Three statements: recordings stay on this phone; transcripts stay on this phone; you control retention (text from constants) | Allow microphone |
| Permission | System RECORD_AUDIO dialog | Allow / Deny |
| Permission denied | Impact text (A2) | Try again; Continue without microphone |
| Permanently denied | Impact text (A3) | Open settings; Continue without microphone |
| Loading / Empty / Error / Offline | N/A (static; offline identical) | — |

**SCR-ONB-07 Speech check**

| State | What the user sees | Actions |
|---|---|---|
| Loading | "Checking speech recognition on this phone…" (≤ 2 s †) | — |
| Default | One of the outcomes in §5.1 | Per §5.1 |
| Downloading | Progress bar + percentage | Continue (A9); Cancel download |
| Error | E6 / E7 | Retry; other options |
| Offline | Download and online-consent options show "Needs a connection"; on-device and audio-only outcomes unaffected | Continue |
| Permission | N/A (no permission needed to detect tier) | — |
| Empty | N/A | — |

**SCR-ONB-08 First attempt intro** — Default (prompt preview, "about a minute", privacy reminder line, Start / Do this later); Permission (mic not granted: "Microphone needed" + Allow microphone / Do this later); Loading/Empty/Error/Offline: N/A (content bundled). Recording, processing, result and inconclusive states are FS-04/FS-05 (SCR-SESS-01). First-result variant: primary action **Go to Today** replaces Continue.

## 10. Data requirements

| Entity / store | Fields |
|---|---|
| ENT-01 Profile | `track`, `level`, `dailyMinutes`, `onboardingStep`, `onboardingCompletedAt`, `baselineAttemptId?`, `updatedAt` |
| Preferences (`PreferencesRepository`, DataStore) | `reminderEnabled`, `reminderTime`, `notificationPromptShown`; speech preferences `onlineConsent`, `recognitionLocale` (ARC-05 §1) |
| ENT-03 / ENT-04 | First attempt and its metrics, `source = ONBOARDING` |
| ENT-11 RecordingFile | Retained recording of the first attempt per retention (T1/T3b) |
| ENT-09 LearningEvent | No event for choice steps; attempt facts per FS-04 |
| Content pack | Activity `onb-intro` (TPL-ANSWER, `maxSeconds = 60`) |

## 11. API / repository dependencies

`ProfileRepository` (onboarding state, choices, baseline), `PreferencesRepository` (reminder, speech preferences), `SpeechCapabilityService` (tier, pack status, download), `ContentRepository` (`onb-intro`), `AttemptRepository` + `EvaluationPipeline` (first attempt via FS-04/05), `ReminderWorker` scheduling (FS-09), Android runtime permission APIs (RECORD_AUDIO, POST_NOTIFICATIONS).

## 12. Offline behaviour

The whole onboarding, including the first attempt and feedback, works offline on T1, T2 and T3b (PRD acceptance §21 item 2). Only *Download speech pack* and *Allow online transcription* need a connection; both are optional and labelled. Sign-in (A8) is optional and needs a connection.

## 13. Privacy requirements

- SCR-ONB-06 and the T3a consent text are generated from the same constants that drive behaviour and are snapshot-tested (SRS-PRIV-002, NFR-PRIV-007).
- The microphone is used only after the user taps Record (NFR-PRIV-001); asking for permission never starts recording.
- No audio or transcript leaves the device unless the user explicitly allows online transcription (SRS-PRIV-001, SRS-BR-023).
- No analytics are collected about onboarding choices (SRS-PRIV-004).

## 14. Accessibility notes

Options are radio groups with `selected` semantics and full-row 48 dp targets (NFR-A11Y-004); step titles are headings; the progress indicator announces "Step 3 of 8"; at 200 % font scale "can do" lines wrap without truncation (NFR-A11Y-002); Continue stays reachable (scrollable content, sticky button); reduced motion removes transitions (NFR-A11Y-005); the speech-check result is announced when it appears.

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS02-01 | A new user | They open the app | The onboarding sequence of SRS-FR-ONB-001 is shown and every choice step has a pre-selected default and Continue |
| AC-FS02-02 | SCR-ONB-02 | The user selects a goal | The profile track is the mapped track of SRS-FR-ONB-002 |
| AC-FS02-03 | SCR-ONB-03 / -04 | The user continues without changing anything | Level = Intermediate and daily minutes = 10 are stored |
| AC-FS02-04 | Android 13+ and SCR-ONB-05 | The user chooses "Remind me" | The notification permission request is shown once; "Not now" shows no request |
| AC-FS02-05 | SCR-ONB-06 | The screen is shown | It states recordings and transcripts stay on the device before RECORD_AUDIO is requested |
| AC-FS02-06 | Microphone denied | The user continues | They reach Today with speaking steps disabled and a way to grant the permission |
| AC-FS02-07 | Microphone permanently denied | SCR-ONB-06 is shown | "Open settings" is offered instead of "Try again" |
| AC-FS02-08 | Each tier T1/T2/T3a-candidate/T3b | The speech check completes | The matching §5.1 outcome and actions are shown, and no unverified status is displayed |
| AC-FS02-09 | Microphone granted, offline, T1 or T2 device | The user records a 40-second introduction | A valid attempt with transcript and feedback is shown and stored as baseline |
| AC-FS02-10 | SCR-ONB-08 | The user taps Do this later | Onboarding completes and Today opens |
| AC-FS02-11 | The app is killed on SCR-ONB-05 | It is relaunched | SCR-ONB-05 is shown with goal, level and minutes preserved |
| AC-FS02-12 | Onboarding completed | The user opens Settings → Learning | Goal, level, minutes and reminder are editable (PRD-FR-014) |
| AC-FS02-13 | First attempt Inconclusive | Result is shown | The reason and Retry are shown and no baseline is stored |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-ONB-001 | Sequence and defaults | Robolectric UI | Fresh profile | Walk through with Continue only | Screens in SRS order; stored track default, Intermediate, 10, reminder 19:00 † | SRS-FR-ONB-001, -003, -004 |
| TC-ONB-002 | Goal → track mapping | unit | — | Map each of 5 goals | Exact TRK-* per SRS-FR-ONB-002 | SRS-FR-ONB-002 |
| TC-ONB-003 | Level options and copy | Robolectric UI | — | Open SCR-ONB-03 | 5 levels, each with a can-do line; Intermediate selected | SRS-FR-ONB-003 |
| TC-ONB-004 | Reminder triggers notification prompt | instrumented | API 33+ emulator | Choose Remind me | POST_NOTIFICATIONS dialog shown once | SRS-FR-ONB-005 |
| TC-ONB-005 | Not now: no prompt | instrumented | API 33+ | Choose Not now | No dialog; reminderEnabled=false | SRS-FR-ONB-005 |
| TC-ONB-006 | Privacy text from constants | unit | Policy constants fixture | Render SCR-ONB-06 strings | Text matches constants snapshot | SRS-FR-ONB-006, SRS-PRIV-002 |
| TC-ONB-007 | Mic denied path | instrumented | Deny RECORD_AUDIO | Continue without microphone | Today shown; speaking steps disabled with grant action | SRS-FR-ONB-007, SRS-ERR-001 |
| TC-ONB-008 | Mic permanently denied | instrumented | Deny twice ("don't ask again") | Reopen SCR-ONB-06 state | Open settings shown; returning with grant enables Continue | SRS-FR-ONB-007 |
| TC-ONB-009 | Speech-check outcomes per tier | unit | Fake SpeechCapabilityService per state | Render ViewModel state | Correct §5.1 outcome and actions for each | SRS-FR-ONB-008 |
| TC-ONB-010 | T3a consent copy | Robolectric UI | Fake T3a candidate | Open consent | Text names what is sent, to whom, and withdrawal path | SRS-FR-SPCH-005 |
| TC-ONB-011 | First attempt offline | manual | RD-MID (T2) and RD-HIGH (T1), airplane mode | Complete onboarding with a spoken intro | Valid attempt, transcript, feedback; baselineAttemptId set | SRS-FR-ONB-009, PRD §21 item 2 |
| TC-ONB-012 | onb-intro parameters | unit | Content pack | Load `onb-intro` | TPL-ANSWER, maxSeconds 60, prep 15 s † | SRS-FR-ONB-009 |
| TC-ONB-013 | Resume after process death | instrumented | On SCR-ONB-05 | `adb shell am kill`; relaunch | SCR-ONB-05 with prior choices | SRS-FR-ONB-010 |
| TC-ONB-014 | Skip first attempt | Robolectric UI | On SCR-ONB-08 | Tap Do this later | onboardingCompletedAt set; Today shown | SRS-FR-ONB-011 |
| TC-ONB-015 | Inconclusive first attempt | unit | Gate returns TOO_SHORT | Complete attempt | No baseline stored; Retry offered | SRS-FR-ONB-009, SRS-BR-008 |
| TC-ONB-016 | Choices editable later | Robolectric UI | Onboarded profile | Change level in Settings → Learning | Level updated; history unchanged | PRD-FR-014, SRS-BR-025 |
| TC-ONB-017 | Accessibility at 200 % | Robolectric UI | fontScale 2.0, compact width | Render SCR-ONB-01…08 | No clipped text; Continue reachable; 48 dp targets | NFR-A11Y-002, -004 |
| TC-ONB-018 | Time to first attempt | manual | RD-MID, new user, scripted | Time from first open to valid result | Recorded against KPI-02 (≤ 3 min, Proposed) | PRD-FR-013 |

## 17. Traceability

| PRD-FR | SRS-FR | AC | TC |
|---|---|---|---|
| PRD-FR-010 | SRS-FR-ONB-001…004 | AC-FS02-01, -02, -03 | TC-ONB-001, -002, -003 |
| PRD-FR-011 | SRS-FR-ONB-006, -007 | AC-FS02-05, -06, -07 | TC-ONB-006, -007, -008 |
| PRD-FR-012 | SRS-FR-ONB-008 | AC-FS02-08 | TC-ONB-009, -010 |
| PRD-FR-013 | SRS-FR-ONB-009, -011 | AC-FS02-09, -10, -13 | TC-ONB-011, -012, -014, -015, -018 |
| PRD-FR-014 | (SRS-FR-SET-001/002) | AC-FS02-12 | TC-ONB-016 |
| PRD-FR-080, -083 | SRS-FR-ONB-005 | AC-FS02-04 | TC-ONB-004, -005 |
| PRD-NFR-005 | SRS-FR-ONB-010 | AC-FS02-11 | TC-ONB-013 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Default goal: *Workplace communication* (primary segment S1) or *Everyday conversation*? | Product Owner |
| Q2 | If the first attempt is skipped, how is the baseline set — first valid `onb-intro` attempt whenever done (optional Today card), or first valid TPL-ANSWER attempt? | Product Owner |
| Q3 | Should a display name be asked (optional field on SCR-ONB-01 or Profile only)? | Product Owner / UX |
| Q4 | Should the reminder step be skipped automatically on devices where notifications are disabled at system level? | UX |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | Sign-in is said to be an optional onboarding step, but the onboarding sequence has no sign-in step | SRS-FR-ACC-002 vs SRS-FR-ONB-001 | Add the A8 entry point (SCR-ONB-01 secondary action) to SRS-FR-ONB-001 |
| 2 | RD-16 CAP-02 describes level as "self-selected + optional short speaking check"; the SRS has no level-assessment requirement (first attempt is a baseline only) | RD-16 §2 vs SRS §6.2 | Confirm self-selection only for R1, or add a requirement |
| 3 | SRS-FR-ONB-001 requires a pre-selected default on every choice step but SRS-FR-ONB-002 defines no default goal | SRS-FR-ONB-001/002 | Decide Q1 |
| 4 | The greeting uses `{name}` (SRS-FR-TODAY-010) but onboarding never collects a name; local users have none | SRS-FR-TODAY-010, SRS-FR-ONB-001 | Define greeting without name, or add optional name capture |
| 5 | Legacy signed-in users migrated by SRS-FR-DATA-007 (name, level, goal, minutes imported) — whether they see onboarding is undefined | SRS-FR-DATA-007, SRS-FR-ONB-001 | Recommend: skip imported choice steps; always run steps 6–7 |
| 6 | PRD-FR-010 says "≤ 5 short steps", while SRS-FR-ONB-001 lists nine stages (four of them choices) | PRD-FR-010 vs SRS-FR-ONB-001 | Clarify that the limit applies to choice steps |
| 7 | `onb-intro` max 60 s differs from the TPL-ANSWER default of 90 s † (SRS-FR-SESS-003); the SRS has no concept of a per-activity override | SRS-FR-ONB-009 vs SRS-FR-SESS-003 | Allow content-level `maxSeconds` override (FS-06 content schema) |
| 8 | Baseline is defined only for the onboarding attempt; behaviour when it is skipped or Inconclusive is undefined | SRS-FR-ONB-009, -011 | Decide Q2 |
