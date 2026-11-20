# FS-08 — Progress & Streak

| Field | Value |
|---|---|
| Document ID | FS-08 |
| Version | 0.2 — Draft for review (2026-11-21, consistency review CL-76: attempt detail renders the stored tip snapshot, ARC-03 §3.4) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-08; PRD §15.8 (PRD-FR-070…075), PRD-FR-047, PRD-FR-052, PRD §14 J5; SRS §6.8 (SRS-FR-PROG-001…008), SRS-FR-EVAL-015/016, SRS-FR-WORD-014, ARC-03 §3.4, ARC-05 §8.4, §15, §16 (SRS-BR-002, -003, -006, -008, -010…-012, -018, -020…-022, -024…-026); NFR-PERF-008, NFR-REL-004/005, NFR-MAINT-005, NFR-QUAL-001/006, NFR-A11Y-*; ARC-01 §2, §4.2; ARC-03 ENT-03/04/05/07/09/11/13; ARC-05 §6, §7; ADR-002, ADR-014, ADR-015; DES-02 (SCR-PROG-01…05, SCR-SESS-01, SCR-ACC-01); DES-05 §11.13, §11.17, §11.18, §12; FS-01, FS-03, FS-04, FS-05, FS-07, FS-09, FS-10, FS-11; decisions D-06, D-10, D-16 |

---

## 1. Feature ID & purpose

**FS-08 / CAP-08 (pillar P-4).** Show people whether they are improving using **only evidence from their own valid attempts**: one streak, practice time measured from recordings, trends of measured metrics that respect noise bands, an attempt history with transcript, metrics, feedback and local replay, modest milestones and — in R2 — repeatable check-ins. Nothing on these screens is seeded, estimated from taps or stored as an untrusted counter.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "Which streak is the real one?" | Four streak computations; stale profile streak; tasks completed with one tap (FND-DOM-07; data_learning_audit §5) | All — UN-06 |
| "These minutes aren't real." | 3 minutes per ticked step; planned minutes added on tap (FND-DOM-08) | Sofia, Aarav — UN-04 |
| "My totals look wrong." | Analytics read only the latest 200 attempts; active days clamped to 5; "words learned" = words seen (audit §5) | All |
| "My streak broke although I practised." | UTC day boundary; dateKey fixed when the screen was created (FND-DOM-11; audit §5) | Aarav, Daniel |
| "I want to hear how I sounded last week." | Absolute device paths stored in the cloud; activity audio never transcribed (FND-DATA-04; audit §4.1) | Meera, Sofia — UN-03 |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | Progress overview SCR-PROG-01 (current and longest streak, week strip and month grid, practice time this week, sessions completed, words learned, word summary, trend cards); metric detail SCR-PROG-02; history SCR-PROG-03; attempt detail with local replay SCR-PROG-04; milestone celebration (3, 7, 14, 30, 60, 100 days); streak summary consumed by Today (FS-03); non-blocking backup prompt for local users (DES-02 §2.2) | Check-ins SCR-PROG-05; milestone and weekly-summary inbox messages (FS-09) |
| **Out** | Leaderboards or comparison with others (NG-06); composite or overall scores for free speaking (D-06); exam or CEFR predictions (NG-04); streak freeze, repair or upsell (PP-7); cloud recordings or cross-device replay (NG-12); export and clear history (FS-10); evaluation logic (FS-05, ARC-05) | Social sharing of progress |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Overview, history | Profile exists (FS-01); facts read from Room; works with zero attempts (empty states) |
| Trend card | ≥ 3 valid attempts in the template family with a non-null value for the metric (SRS-BR-024) |
| Replay | Attempt recorded on this device on T1 or T3b, retention not expired, file present (SRS-FR-SESS-009, ADR-015) |
| Check-in (R2) | Baseline or previous check-in exists for the track; PlanEngine offers the check-in (FS-03) |

## 5. Main workflow

### 5.1 Facts and what they count toward (SRS-FR-PROG-005)

| Fact (Room) | Streak day (SRS-BR-010) | Practice time (SRS-BR-018) | Sessions completed | Words learned | Trends |
|---|---|---|---|---|---|
| Valid attempt — Today or Practice speaking activity, onboarding first attempt (ENT-03 `VALID`) | Yes | + M-DUR | — | — | Yes, in its template family |
| Valid word-stage attempt, stages 2–4 (Pass mark or Not yet) | Yes | + M-DUR | — | — | No (word checks are not a template family — RECOMMENDATION) |
| Valid review attempt with spoken recall | Yes | + M-DUR | — | — | No |
| Inconclusive attempt | No (SRS-BR-008) | No | — | — | No; listed in history |
| Interrupted or discarded attempt | Not recorded (SRS-FR-SESS-010) | — | — | — | — |
| Learn stage, quiz answer (R2), screen visit, tap, notification tap | No | 0 | — | — | — |
| `SESSION_COMPLETED` event (ENT-09) | Not required (SRS-BR-012) | — | + 1 | — | — |
| Word with stage 4 passed (Learned / Reviewing / Mastered, FS-07 §5.1) | — | — | — | + 1 | — |
| Attempt or event pulled by sync from another device | Same rules, using its stored dateKey (SRS-BR-020/021) | Same | Same | Same | Same |

### 5.2 Streak computation (SRS-BR-010, -011, -012)

One `ProgressCalculator` computes every streak figure shown anywhere (NFR-MAINT-005). RECOMMENDATION algorithm:

1. **D** = distinct `dateKey` values of valid speaking attempts from any source.
2. **T** = today's dateKey in the device's *current* time zone (`DateKeyProvider`); **Y** = T minus one calendar day (`LocalDate.minusDays(1)`, never "now − 24 h").
3. If D is empty → current = 0, longest = 0.
4. **A** = latest date in D. If A < Y → current = 0. Otherwise current = length of the run of consecutive dates in D that ends at A. A may be *later* than T after a west-bound time-zone change; it still counts (Issue 1).
5. Longest = length of the longest run of consecutive dates in D over all history.
6. Recomputed reactively from Room whenever attempts change (new attempt, sync merge, clear history). Never stored as truth and never read from Firestore (ADR-014 §2; SEC-01 THR-05).
7. An attempt's dateKey = local date when **recording started**, in the zone in force at that moment; stored on ENT-03 and never rewritten (SRS-BR-011; RECOMMENDATION for midnight-crossing attempts, Issue 8).

**Worked examples** (2026; 21 Nov is a Saturday). "Valid" = valid speaking attempt.

| # | Scenario | Valid-attempt dateKeys | Today (T) | Current | Longest | Shown |
|---|---|---|---|---|---|---|
| S1 | Practised four days, not yet today | 17, 18, 19, 20 Nov | 21 Nov | 4 | 4 | "4-day streak"; today cell "today, not practised yet" |
| S2 | Same, after a valid attempt today | 17–21 Nov | 21 Nov | 5 | 5 | "5-day streak" |
| S3 | One missed day | 16, 17, 18, 20, 21 Nov | 21 Nov | 2 | 3 | "2-day streak · Best: 3 days"; 19 Nov shown "not practised" (no red, no cross) |
| S4 | Two or more missed days | 16, 17, 18 Nov | 21 Nov | 0 | 3 | "Start a new streak today · Best: 3 days"; Today offers recovery (FS-03, SRS-BR-014) |
| S5 | Only Inconclusive attempts today | 19, 20 Nov (21 Nov Inconclusive only) | 21 Nov | 2 | 2 | Today not counted (SRS-BR-008) |
| S6 | Three valid attempts in one day | 21 Nov ×3 | 21 Nov | 1 | 1 | A day counts once |
| S7 | Learn stage and quiz only today | 20 Nov; 21 Nov Learn only | 21 Nov | 1 | 1 | Learn/quiz are not speaking attempts |
| S8 | East-bound: New York → London | 20 Nov 21:00 New York (dateKey 20); 21 Nov 09:00 London (dateKey 21) | 21 Nov | 2 | 2 | Consecutive local dates; only 7 h apart in real time |
| S9 | West-bound across the date line: Tokyo → Los Angeles | 21 Nov 08:00 Tokyo (dateKey 21); now 20 Nov in Los Angeles | 20 Nov | 1 (A = 21 > T) | 1 | Not reset; a new attempt in Los Angeles is stored as 20 Nov and current becomes 2 (20, 21) — no double counting |
| S10 | East-bound long-haul skips a local date: Los Angeles → Tokyo | 20 Nov 06:00 Los Angeles (dateKey 20); 22 Nov 08:00 Tokyo (dateKey 22) | 22 Nov | 1 | — | 21 Nov never had a practice on the device; streak restarts (SRS-BR-011 literal; Q1) |
| S11 | DST starts, Europe/London, 29 Mar 2026 (23-hour day) | 28 Mar 23:30 GMT; 29 Mar 23:30 BST (23 h later) | 29 Mar | 2 | 2 | Calendar dates, not 24-hour periods |
| S12 | DST ends, Europe/London, 25 Oct 2026 (25-hour day) | 24 Oct 00:10; 25 Oct 23:50 (both instances of 01:30 on 25 Oct map to dateKey 25 Oct) | 25 Oct | 2 | 2 | Consecutive despite ~48 h between attempts |
| S13 | Recording crosses midnight | Started 21 Nov 23:59:40, stopped 22 Nov 00:00:30 | 22 Nov | dateKey 21 Nov | — | RECOMMENDATION: start time decides (Issue 8) |
| S14 | Device clock set back two days, then practice | dateKey from the wrong device date | — | Counted as stored | — | Accepted limitation; affects only the user (SEC-01 THR-05; Q3) |

### 5.3 Practice time (SRS-BR-018)

Practice time = sum of M-DUR (recording length) of valid attempts; non-speaking actions add 0. "This week" = dateKeys in the current week using the locale's first day of the week (`WeekFields.of(locale)`, as the week strip in DES-05 §11.17). Display = whole minutes rounded **down** (never overstated); "Less than 1 min" when 0 < total < 60 s; "0 min" when none.

Example (week of Mon 16 Nov, en-GB): Mon valid 62 s + valid 45 s; Tue Inconclusive 20 s; Wed Learn stage only; Thu valid Say it 4 s → 62 + 45 + 4 = 111 s → **"1 min"**. The bar chart on SCR-PROG-01 shows seconds per day converted to minutes, bars from 0, missing days as gaps (DES-05 §11.18 CH-1, CH-3, CH-4). Label: "Practice time" with explanation "Total length of your measured recordings" (Issue 2).

### 5.4 Other figures

| Figure | Definition |
|---|---|
| Sessions completed | Count of `SESSION_COMPLETED` events with dateKey in the current week (RECOMMENDATION; all-time total on Profile, FS-10; Issue 11) |
| Practice days this week | Count of streak days in the current week |
| Words learned | Words with status Learned, Reviewing or Mastered (FS-07 §5.1) — never words opened or reviewed once (PP-7) |
| Word summary | Counts per status (New, Learning, Learned, Reviewing, Mastered) and "Due today" (SRS-FR-PROG-007); tapping opens Words (SCR-WORD-01) |

### 5.5 Trends and noise bands (SRS-FR-PROG-002; SRS-BR-024; ARC-05 §6)

| Family (SRS-FR-PROG-002) | Metric | Label | Noise band † (ARC-05 §6) | Trend card in R1 (RECOMMENDATION) |
|---|---|---|---|---|
| Read & repeat (TPL-READ, TPL-REPEAT) | M-MATCH Recognised as intended | Measured | ±5 pts | Yes |
| | M-WPM Pace (TPL-READ only) | Measured | ±10 wpm | Yes |
| Free speaking (TPL-ANSWER, TPL-STRUCT, TPL-STORY) | M-WPM Pace | Measured | ±10 wpm | Yes |
| | M-RATIO Speech ratio | Measured | ±5 pts | Yes |
| | M-PAUSE Long pauses | Measured | ±1 | Deferred — count is not normalised by length (Issue 4) |
| | M-FILL Fillers / min, M-LEX Variety | Estimated | ±1/min, ±0.05 | No — PRD-FR-070 limits trends to measured metrics (Issue 3) |

Rules (RECOMMENDATION † unless cited):

1. Values = non-null values of the metric from valid attempts in the family during the selected period (default last 30 days †; option "All time"), oldest first. Inconclusive attempts and null metrics are excluded (SRS-BR-002, -008).
2. n < 3 → no line; the card shows "{n} of 3 valid attempts — practise {3 − n} more to see a trend" (SRS-BR-024).
3. n ≥ 3 → k = min(5, ⌊n / 2⌋); **Δ = mean(latest k) − mean(earliest k)** (Issue 5).
4. |Δ| < noise band → **"No clear change"** (SRS-BR-024). Otherwise a neutral direction: "Faster by about 16 words per minute", "Speaking more of the time by about 7 points". No green/red and no "better/worse" (DES-05 §3 rule 3; PP-7). The plain-language band and target range are shown as text (ARC-05 §6; DES-05 CH-5).
5. The chart marks where `evaluationVersion` or the capture tier changed between points (Issue 4).

| Example | Values (oldest → latest) | n, k | Δ | Card text |
|---|---|---|---|---|
| Free speaking, Pace | 98, 104, 101, 112, 118, 121 | 6, 3 | 117.0 − 101.0 = +16 (≥ 10) | "Faster by about 16 words per minute · now in the comfortable range (100–160 †)" |
| Free speaking, Speech ratio | 62, 65, 64, 66 | 4, 2 | 65.0 − 63.5 = +1.5 (< 5) | "No clear change" |
| Read & repeat, Recognised as intended | 70, 85 | 2 | — | "2 of 3 valid attempts — practise 1 more to see a trend" |

### 5.6 Progress overview (SCR-PROG-01; FLOW-07)

1. User opens Progress. `ProgressCalculator` combines Room Flows from `AttemptRepository`, `PlanRepository` and `WordRepository` (ARC-01 §4.2); no network call.
2. Order (RECOMMENDATION): streak counter "4-day streak · Best: 9 days" + week strip, expandable month grid (DES-05 §11.17) → "This week": practice time with daily bars, practice days, sessions completed → word summary → trend cards per family → **View history**.
3. Local (not signed-in) users with ≥ 3 † valid attempts see a dismissible card "Back up your progress" → SCR-ACC-01 (FS-01 A7); dismissed for 30 days † (RECOMMENDATION).
4. A trend card opens SCR-PROG-02; **View history** opens SCR-PROG-03.

### 5.7 Metric detail (SCR-PROG-02)

Line chart of the metric per attempt (DES-05 §11.18): stored values only (CH-1), target band with label (CH-5), noise-band note "Changes smaller than ±10 words per minute are normal variation", version/tier markers, period selector, **View as list** (CH-9), **How we measured this** (text from the ARC-05 catalogue, SCR-SESS-02, FS-05). Tapping a point opens that attempt (SCR-PROG-04).

### 5.8 History (SCR-PROG-03; SRS-FR-PROG-003)

Newest first, paged (≤ 50 ms per query with ≈ 3,000 attempts on RD-LOW, NFR-PERF-008). Each row: date and time (locale format, A11Y-37); activity title, or "Word · {word} · Say it"; template label; up to 3 headline metrics with value and unit (Inconclusive rows show the reason instead); status text **Valid** or **Inconclusive** (badge, DES-05 §11.14); "{n} mistakes found" from `issue_count` (ARC-03 §3.4) on valid rows; a playback icon when a recording is available. Filters (RECOMMENDATION): family (Read & repeat / Free speaking / Words), source (Today / Practice / Words), status. Attempts on retired content keep their stored title and remain readable (SRS-BR-026).

### 5.9 Attempt detail and replay (SCR-PROG-04; SRS-FR-PROG-004)

Content: date, activity and prompt, tier label (SRS-FR-SESS-006); transcript "What we heard" (DES-05 §11.20) from `sensitive.db`; all metrics with Measured / Estimated badges and "Not available — {reason}" for null values (SRS-BR-002, -003); strength and focus (FS-05); **Mistakes found** from the stored `attempt_issue` rows (codes, labels, evidence; quoted spans from `sensitive.db`) or the honest "didn't find issues" line, and **Coach tips** rendered from the tip snapshot stored with the attempt (tip IDs and the text as shown; ARC-03 §3.4, local only — consistency review, CL-76), so history shows what was shown even after a content update (SRS-FR-EVAL-015/016, SRS-FR-WORD-014; ARC-05 §8.4) — nothing is re-evaluated with newer rules or re-derived from a newer content version; evaluation-rules version inside "How we measured this"; accuracy rating ("Was this accurate?", ENT-13); playback; **Try again** → SCR-SESS-01 on the same activity (hidden for retired content); **Delete recording** (RECOMMENDATION, Issue 10).

| Condition | Replay area shows |
|---|---|
| Recording retained (T1 / T3b, within retention, file present) | Player: play/pause, position, duration (48 dp controls); one player at a time; pauses on audio-focus loss; stops when leaving the screen |
| Attempt captured on T2 / T3a | "This phone can't keep a recording while it transcribes." |
| Retention "Delete after feedback" | "Recordings are deleted after feedback (your setting)." + link to Speech & privacy (SCR-SET-04) |
| Retention expired | "Recording deleted after {30 days} (your setting)." |
| Inconclusive attempt | "Recordings of unmeasured attempts are deleted straight away." (SRS-BR-022) |
| Attempt made on another device (synced) | "Recordings and transcripts stay on the phone that made them." (NG-12, D-16). Mistakes found shows only the synced issue titles (≤ 3 codes in `feedbackCodes`), without evidence (ARC-03 §3.4) |
| File missing or unreadable | "This recording can't be played." (metadata cleaned by `CleanupWorker`) |

Transcript area: T3b → "No transcript — audio-only"; transcripts deleted by the D-24 option → "Transcript deleted."

### 5.10 Milestones (SRS-FR-PROG-006)

1. When a new streak day makes the current streak equal to 3, 7, 14, 30, 60 or 100, `ProgressCalculator` emits a milestone once.
2. The celebration appears once on the next result or Today screen (RECOMMENDATION: on the SCR-SESS-01 result after the attempt that made the streak day): ≤ 800 ms scale-and-fade, or a static icon and text under reduced motion (DES-05 §8; A11Y-21; NFR-A11Y-005). Copy: "Seven days in a row. Nice work." (DES-05 §12.1). Announced in a polite live region.
3. Celebrated milestones are recorded as `{runStartDateKey}:{n}` in the `settings` DataStore so they are not repeated after restart or restore. A new streak run may earn the same milestone again (RECOMMENDATION; Q2).
4. R1: no system notification (SRS-FR-REM-004). R2: an inbox message is also created (FS-09).

### 5.11 Evidence-only display rules (PRD-NFR-001; NFR-QUAL-001; TST-01 §9)

| Never | Instead |
|---|---|
| A seeded "1-day streak" or default values for new users | "Start your streak today" (DES-05 §11.17); empty states |
| Counters incremented by navigation, taps, ticks or notifications | Figures derived from facts on every change (SRS-FR-PROG-005) |
| Minutes from plan durations or step counts | Sum of M-DUR of valid attempts (SRS-BR-018) |
| A trend from fewer than 3 values, or "improved" inside the noise band | "{n} of 3…" / "No clear change" (SRS-BR-024) |
| 0 or a guessed value for a missing metric | "Not available — {reason}" (SRS-BR-002) |
| An overall or composite score for free speaking | Individual labelled metrics (D-06, SRS-BR-006) |
| Inconclusive attempts in totals or trends | Listed in history with the reason only |
| Streak or totals read from the cloud | Derived on the device (ADR-014 §2) |
| Comparisons with other users, ranks, streak freezes | Personal history only (NG-06, PP-7) |
| "Pronunciation" or "accent" labels | "Recognised as intended" (SRS-BR-004) |

### 5.12 R2 — Check-ins (SCR-PROG-05; SRS-FR-PROG-008) — outline

Every 14–28 days † PlanEngine offers the user's check-in activity (FS-03). The first point is the onboarding baseline (`onb-intro`, SRS-FR-ONB-009) when it was valid. Check-in attempts are ordinary attempts (they count toward streak and practice time). SCR-PROG-05 plots each measured headline metric across check-ins and applies the §5.5 noise-band rule between the latest and the previous check-in ("No clear change" or a neutral direction); this feeds KPI-07. The check-in prompt per track is not yet defined (Issue 9).

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | New user, no attempts | Empty states (§9); "Start your streak today"; trend cards explain the 3-attempt rule |
| A2 | Sync merges attempts from another device | Figures recompute; the streak may grow; synced attempts show no transcript or recording (§5.9) |
| A3 | User clears history (FS-10) | Figures recompute from what remains; streak and practice time may drop to 0; word progress is unaffected unless words are reset |
| A4 | **Try again** on an attempt detail | SCR-SESS-01 opens on the same activity; the new attempt appears at the top of history |
| A5 | Local user taps "Back up your progress" | SCR-ACC-01 (FS-01 A7), returning to SCR-PROG-01 |
| A6 | Level or track changed | No reset of history, streak or trends (SRS-BR-025) |
| A7 | Local midnight or time-zone change while Progress is open | `DateKeyProvider` emits a new T; week strip and current streak update without restart (RECOMMENDATION) |
| A8 | Expanded width | Two-column cards with a history pane (DES-02 §7) |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Reading progress data fails | "Couldn't load your progress. Your practice is saved on this phone." + Try again; no zeros shown in place of data | — (read path; SRS-ERR-005 covers writes) |
| E2 | Recording file missing or corrupt | "This recording can't be played."; metadata cleaned later | — |
| E3 | Audio focus lost during playback (call, other app) | Playback pauses; resume on user action | — |
| E4 | Saving an accuracy rating or deleting a recording fails | "Couldn't save this change. Try again." Retry once; never show success for a failed write | SRS-ERR-005 |
| E5 | Transcript store unavailable | "The transcript can't be shown right now."; metrics and feedback still shown | SRS-ERR-005 |
| E6 | Sync failure while Progress is open | No effect on Progress (local figures); sync status in Settings → Account | SRS-ERR-006 |

## 8. Business rules

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-002 / -003 | Missing metric inputs → "Not available"; every metric shows its Measured / Estimated label |
| SRS-BR-006 | No overall score for free-speaking attempts in history or detail |
| SRS-BR-008 | Inconclusive attempts never count toward streak, practice time or trends |
| SRS-BR-010 | Streak day = dateKey with ≥ 1 valid speaking attempt from any source |
| SRS-BR-011 | Current streak ends today or yesterday, else 0; longest is the maximum ever; history is never deleted by a missed day; stored dateKeys are never rewritten |
| SRS-BR-012 | Completing the daily session is not required for a streak day |
| SRS-BR-018 | Practice time = sum of recording durations of valid attempts |
| SRS-BR-020 / -021 | Synced facts are idempotent by UUID and merged per ADR-014; figures are recomputed after merge |
| SRS-BR-022 | Replay is available only while the recording is retained |
| SRS-BR-024 | Trends need ≥ 3 valid attempts in the family; changes inside the noise band are "no clear change" |
| SRS-BR-025 / -026 | Level or track changes never reset progress; retired content stays readable in history |
| RECOMMENDATION | Streak algorithm details (§5.2 steps 4 and 7), week definition and rounding (§5.3), trend metric list and Δ rule (§5.5), milestone record (§5.10), backup-card threshold (§5.6), history filters |

## 9. UI states

**SCR-PROG-01 Progress overview**

| State | What the user sees | Actions |
|---|---|---|
| Default | §5.6 content from facts | Open trend card; View history; Words summary |
| Loading | Nothing for 300 ms, then skeleton (DES-05 §11.19) | — |
| Empty (no valid attempts) | "Start your streak today"; "Your progress will appear here. Finish your first session to see your pace, pauses and more." | Start today's session |
| Error | E1 | Try again |
| Offline | Identical to Default (local data) | — |
| Permission | N/A — no permission needed | — |
| Local user | Default + "Back up your progress" card (§5.6) | Sign in; Not now |
| Streak 0 with history | "Start a new streak today · Best: {n} days" | Start today's session |

**SCR-PROG-02 Metric detail**

| State | What the user sees | Actions |
|---|---|---|
| Default (n ≥ 3) | Chart, Δ text or "No clear change", noise-band note, target band, markers | Period; View as list; How we measured this; open a point |
| Too few values (n < 3) | Single values as metric tiles; "{n} of 3 valid attempts — practise {3 − n} more to see a trend" | Practise this |
| Loading / Error | Skeleton / E1 | — / Try again |
| Empty | N/A — reached only from a card | — |
| Offline | Identical to Default | — |
| Permission | N/A | — |

**SCR-PROG-03 History**

| State | What the user sees | Actions |
|---|---|---|
| Default | Paged list newest first (§5.8) | Filter; open an attempt |
| Loading | Skeleton rows; next page shows a row-level progress indicator | — |
| Empty — no attempts | "Your attempts will appear here." | Start today's session |
| Empty — filter | "No attempts match these filters." | Clear filters |
| Error | E1 | Try again |
| Offline | Identical to Default | — |
| Permission | N/A | — |

**SCR-PROG-04 Attempt detail**

| State | What the user sees | Actions |
|---|---|---|
| Default — valid | §5.9 content | Play; Try again; Rate; Delete recording |
| Default — Inconclusive | Reason, date, activity; no metrics; "This attempt doesn't count against you." | Try again |
| Replay unavailable | Reason text per §5.9 table | Speech & privacy (when caused by a setting) |
| Playing | Player with position; announced "Playing" (A11Y-06) | Pause; seek |
| Loading / Error | Skeleton / E1, E2, E5 | — / Try again |
| Empty | N/A — always an attempt | — |
| Offline | Identical to Default | — |
| Permission | N/A — playback needs no permission | — |

**Milestone celebration** (shown on SCR-SESS-01 result or SCR-TODAY-01) — Default: animation ≤ 800 ms + copy; Reduced motion: static icon + copy; dismiss by tap or after reading; no other states. **SCR-PROG-05 Check-ins (R2)** — Default chart of check-ins with Δ text; Too few (one check-in): "Your next check-in will show the change."; Empty: "Your first check-in is your onboarding answer."; Loading/Error as above; Offline identical; Permission N/A.

## 10. Data requirements

| Entity / store | Fields needed by FS-08 (minimum; ARC-03 authoritative) |
|---|---|
| ENT-03 Attempt (ARC-03 §3.4) | `attempt_id`, `profile_id`, `date_key`, `started_at`, `source`, `task_kind`, `content_id`, `content_version`, `template_id`, tier, `result` (EVALUATED / PASS / FAIL / INCONCLUSIVE; valid ⇔ not INCONCLUSIVE), Inconclusive reason, `duration_ms` (M-DUR), `evaluation_version`, `target_word_id?`, `issue_count`, `focus_issue_ordinal`; ENT-03 (`attempt_issue`) rows for Mistakes found; `shown_tip_ids` and `shown_tips_snapshot` for Coach tips (local only, CL-76); check-in marker (R2, `source = CHECKIN`); origin device |
| ENT-04 AttemptMetric | `attemptId`, `key` (M-*), `value?`, `unit`, `label` (MEASURED / ESTIMATED), `nullReason?` |
| ENT-09 LearningEvent | `SESSION_COMPLETED {planDate}` for sessions completed |
| ENT-05 SessionPlan | Check-in offer (R2) via PlanEngine |
| ENT-07 WordProgress | `status`, `nextReviewDate` for the word summary |
| ENT-11 RecordingFile | `attemptId`, relative path, `expiresAt`, `format` — replay availability |
| ENT-13 FeedbackRating | Rating shown on attempt detail |
| `sensitive.db` | Transcript text by `attemptId` (not synced, D-16) |
| `settings` DataStore | `celebratedMilestones`, backup-card dismissal date, reduced-motion preference |
| Content pack | Activity titles by `activityId` + `contentVersion` (SRS-BR-026) |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `ProgressCalculator` | Pure functions: streak (dateKeys, today), practice time (attempts, week), trend (values, noise band), milestone detection, word-status counts |
| `AttemptRepository` | Paged history Flow, attempt detail, metrics, rating, recording deletion |
| `PlanRepository`, `WordRepository` | Session completions; word counts and due today |
| `ContentRepository` | Activity titles and templates by version |
| `DateKeyProvider` / `Clock` | Today's dateKey in the current zone; emits on local midnight and time-zone change |
| `PreferencesRepository` | Retention setting (replay messages), reduced motion, milestone record |
| `ProfileRepository` / `AccountRepository` | Local vs signed-in state for the backup card |
| `CleanupWorker` | Removes expired recordings and dangling metadata (SRS-FR-DATA-009) |
| Platform | Media playback with audio focus (ARC-02 to name the class) |

## 12. Offline behaviour

Progress is entirely local: every figure, chart, history row, transcript and recording is read from the device, so all of FS-08 works in airplane mode (NFR-REL-006). Signed-in users' attempts from other devices appear after the next sync pull (SRS-FR-DATA-004) and trigger recomputation.

## 13. Privacy requirements

- Replay plays local files only; recordings never leave the device (NG-12, SRS-PRIV-001, ADR-015).
- Transcripts are shown from `sensitive.db` and are not synced in R1 (SRS-FR-DATA-006, D-16).
- Streaks, totals and trends are derived on the device; no aggregate is uploaded or trusted from the cloud (ADR-014 §2; SEC-01 THR-05).
- No analytics (SRS-PRIV-004); no transcript text or uid in logs (NFR-SEC-002).

## 14. Accessibility notes

Week strip is one merged node with a summary ("This week: practised Monday, Tuesday and Thursday. Today not practised yet.") and month cells are traversable text (DES-05 §11.17). Charts have a content-description summary and a "View as list" alternative (A11Y-12; DES-05 CH-10). Metric tiles read as one phrase with units spelled out (A11Y-07, A11Y-34). Status, direction and practised days never rely on colour (A11Y-23, NFR-A11Y-006). Milestone animation respects reduced motion and is announced politely (A11Y-21, A11Y-08). Player controls are labelled with state ("Play recording", "Pause") and ≥ 48 dp (A11Y-19). Layouts reflow at 200 % font scale (A11Y-13); dates and numbers use locale formats (A11Y-37). Streak rule for users who cannot complete a speaking step: DES-07 OQ-A11Y-1 (Q6).

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS08-01 | A new user with no attempts | Progress is opened | "Start your streak today", "0 min" practice time and no trend line are shown; no seeded streak value appears |
| AC-FS08-02 | Valid attempts on 17–20 Nov, from Today and Practice | Progress is opened on 21 Nov before practising | Current streak 4, longest 4, today shown "not practised yet" (S1) |
| AC-FS08-03 | Valid attempts on 16, 17, 18, 20, 21 Nov | Progress is opened on 21 Nov | Current 2, longest 3, and 16–18 Nov are still shown as practised (S3) |
| AC-FS08-04 | Last valid attempt on 18 Nov | Progress is opened on 21 Nov | Current 0 with "Start a new streak today · Best: 3 days" (S4) |
| AC-FS08-05 | Only Inconclusive attempts today | Progress is opened | Today is not a streak day and adds no practice time (S5) |
| AC-FS08-06 | A valid attempt stored as 21 Nov in Tokyo | The device moves to Los Angeles (local date 20 Nov) and the user practises | The stored dateKeys are 20 and 21 Nov, unchanged; current streak 2; no day counted twice (S9) |
| AC-FS08-07 | Valid attempts on 28 Mar 23:30 GMT and 29 Mar 23:30 BST (Europe/London) | Progress is opened on 29 Mar | Current streak 2 (S11) |
| AC-FS08-08 | Any user | They open screens, tap Today steps without recording and tap a reminder | No streak, practice-time or session figure changes |
| AC-FS08-09 | Valid 62 s, 45 s and 4 s attempts and a 20 s Inconclusive attempt this week | Progress is opened | Practice time shows "1 min" |
| AC-FS08-10 | Two valid free-speaking attempts with pace values | Progress is opened | The pace card shows "2 of 3 valid attempts — practise 1 more to see a trend" and no line |
| AC-FS08-11 | Free-speaking pace values 98, 104, 101, 112, 118, 121 and speech-ratio values 62, 65, 64, 66 | Progress is opened | Pace card: "Faster by about 16 words per minute"; speech-ratio card: "No clear change"; neither uses green or red |
| AC-FS08-12 | 5 valid and 2 Inconclusive attempts | History is opened | 7 rows newest first, each with status text; Inconclusive rows show their reason and are excluded from totals |
| AC-FS08-13 | A valid attempt with one null metric and two stored issues | Attempt detail is opened | Transcript, every metric with its Measured/Estimated badge, "Not available — {reason}" for the null metric, feedback, the two stored Mistakes found entries with evidence, coach tips and the rating are shown — exactly as stored, without re-evaluation |
| AC-FS08-14 | Attempts captured on T2 and one whose recording expired | Their details are opened | No player; the matching reason text from §5.9 is shown |
| AC-FS08-15 | A retained T1 recording | The user taps Play, then a call arrives | Playback starts, pauses on focus loss and stops when the screen is left |
| AC-FS08-16 | A synced attempt from another device | History and detail are opened | It appears and counts toward the streak; transcript and recording show the "stay on the phone that made them" text |
| AC-FS08-17 | A streak reaching 7 days | The valid attempt is saved | The celebration appears once (static under reduced motion) and does not reappear after restarting the app |
| AC-FS08-18 | Words in every status, 3 due today | Progress is opened | Counts per status, "3 due today", and words learned = Learned + Reviewing + Mastered |
| AC-FS08-19 | An attempt on an activity retired in a later content version | History and detail are opened | Its title, metrics and feedback are readable; Try again is hidden |
| AC-FS08-20 | ≈ 3,000 attempts on RD-LOW | Progress and History are opened | Each local query completes in ≤ 50 ms (NFR-PERF-008) |
| AC-FS08-21 (R2) | Two check-ins with pace 118 and 131 | SCR-PROG-05 is opened | Both are plotted and the change (+13, beyond ±10) is described neutrally |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-PROG-001 | Streak with no data | unit | Empty attempt set | Compute | Current 0, longest 0 | SRS-BR-011 |
| TC-PROG-002 | Streak examples S1–S7 | unit | Table-driven fixtures, fixed clock | Compute each | Values as §5.2 | SRS-BR-010, -011, -012, -008 |
| TC-PROG-003 | Time-zone examples S8–S10 | unit | Fixed instants and zones (America/New_York, Europe/London, Asia/Tokyo, America/Los_Angeles) | Store attempts, change zone, compute | Values as §5.2; stored dateKeys unchanged | SRS-BR-011 |
| TC-PROG-004 | DST examples S11–S12 | unit | Europe/London, 29 Mar and 25 Oct 2026 | Compute | Current 2 in both | SRS-BR-011 |
| TC-PROG-005 | Midnight-crossing dateKey | unit | Recording 23:59:40–00:00:30 | Save attempt | dateKey = start date (S13) | SRS-BR-011 (RECOMMENDATION) |
| TC-PROG-006 | No counter from navigation | Robolectric UI | Fake repos with write spies | Navigate all tabs; tap a Today step; open from notification | No writes to attempts or events; figures unchanged | SRS-FR-PROG-005 |
| TC-PROG-007 | Practice time and week start | unit | §5.3 fixture; locales en-GB and en-US | Compute week totals | 111 s → "1 min"; week boundaries Monday vs Sunday | SRS-BR-018 |
| TC-PROG-008 | Trend threshold | unit | n = 0, 2, 3 | Compute trend | No trend for 0 and 2; trend for 3 | SRS-BR-024 |
| TC-PROG-009 | Trend Δ and noise band | unit | §5.5 examples | Compute | +16 → direction text; +1.5 → "No clear change" | SRS-BR-024, SRS-FR-PROG-002 |
| TC-PROG-010 | Trend excludes invalid values | unit | Mix of null metrics and Inconclusive attempts; tier change | Compute | Excluded; tier marker emitted | SRS-BR-002, -008 |
| TC-PROG-011 | Overview states | Robolectric UI | Fakes: empty, default, streak 0 with history, error, local user | Render at font 1.0 and 2.0, light and dark | Copy and actions per §9; no clipping | SRS-FR-PROG-001, NFR-A11Y-002 |
| TC-PROG-012 | Week strip semantics | Robolectric UI | Practised Mon, Tue, Thu | Read semantics | Merged summary text as §14 | NFR-A11Y-001 |
| TC-PROG-013 | Chart accessibility | Robolectric UI | 8 pace values | Inspect; toggle list | Summary description; list shows 8 rows | SRS-FR-PROG-002, NFR-A11Y-001 |
| TC-PROG-014 | History order, paging, filters | Robolectric UI | 3,000 seeded attempts | Scroll; apply filters | Newest first; pages load; filters correct; empty-filter state | SRS-FR-PROG-003 |
| TC-PROG-015 | Query performance | instrumented | RD-LOW, seeded 3,000 attempts | Time overview and history queries (median of 10) | ≤ 50 ms each | NFR-PERF-008 |
| TC-PROG-016 | Attempt detail content | Robolectric UI | Valid attempt with a null and an Estimated metric and two `attempt_issue` rows; a synced attempt with codes only | Open each | Badges, "Not available — reason", feedback, stored mistakes with evidence, tips, rating; synced attempt shows titles without evidence | SRS-FR-PROG-004, SRS-FR-EVAL-015 |
| TC-PROG-017 | Replay availability mapping | unit | Each §5.9 condition | Map to UI state | Correct message or player | SRS-FR-SESS-009, SRS-BR-022 |
| TC-PROG-018 | Playback behaviour | instrumented | Retained WAV/AAC recording | Play; trigger focus loss; navigate away | Plays; pauses; stops | SRS-FR-PROG-004 |
| TC-PROG-019 | Synced attempt from another device | unit | Merge a remote attempt | Recompute; open detail state | Counted in streak; no transcript/recording; explanatory text | SRS-BR-020, -021 |
| TC-PROG-020 | Milestone once | unit | Streak 6 → valid attempt; restart; break and rebuild to 7 | Observe emissions | One emission; none after restart; one for the new run | SRS-FR-PROG-006 |
| TC-PROG-021 | Milestone reduced motion | Robolectric UI | Animator scale 0 / in-app Reduce motion | Trigger milestone | Static icon and text; announcement | NFR-A11Y-005 |
| TC-PROG-022 | Word summary counts | unit | Words in every status, 3 due | Compute | Counts; learned excludes New and Learning | SRS-FR-PROG-007 |
| TC-PROG-023 | Retired content readable | Robolectric UI | Attempt on retired activity | Open history and detail | Title and data shown; Try again hidden | SRS-BR-026 |
| TC-PROG-024 | Clear history recomputation | instrumented | Data in all tables | Clear attempts (FS-10) | Streak and practice time recompute; word progress unchanged | SRS-FR-PROG-005 |
| TC-PROG-025 | Delete recording | instrumented | Retained recording | Delete from attempt detail | File removed; ENT-11 updated; attempt and metrics kept | SRS-BR-022 (Issue 10) |
| TC-PROG-026 | Honesty review of Progress | manual | Reference device, scripted data set incl. Inconclusive, T3b, synced attempts | Walk TST-01 §9 checklist on SCR-PROG-01…04 | No unbacked figure, label or claim | PRD-NFR-001, NFR-QUAL-001 |
| TC-PROG-027 (R2) | Check-in comparison | unit | Two check-ins | Compute | Δ vs noise band text | SRS-FR-PROG-008 |

## 17. Traceability

| PRD-FR | SRS-FR / SRS-BR | AC | TC |
|---|---|---|---|
| PRD-FR-070 | SRS-FR-PROG-001, -002; SRS-BR-024 | AC-FS08-10, -11 | TC-PROG-008, -009, -010, -013 |
| PRD-FR-071 | SRS-FR-PROG-003, -004; SRS-BR-022, -026 | AC-FS08-12…16, -19 | TC-PROG-014, -016…-019, -023, -025 |
| PRD-FR-072 | SRS-FR-PROG-001, -006; SRS-BR-010…012 | AC-FS08-01…07, -17 | TC-PROG-001…005, -011, -012, -020, -021 |
| PRD-FR-073 | SRS-FR-PROG-005; SRS-BR-008, -018 | AC-FS08-05, -08, -09 | TC-PROG-006, -007, -024, -026 |
| PRD-FR-074 | SRS-FR-PROG-007 | AC-FS08-18 | TC-PROG-022 |
| PRD-FR-075 (R2) | SRS-FR-PROG-008 | AC-FS08-21 | TC-PROG-027 |
| PRD-FR-052 (shared with FS-06) | SRS-FR-PRAC-004 | AC-FS08-02 | TC-PROG-002 |
| PRD-FR-047 (re-display; owned by FS-05) | SRS-FR-EVAL-015, -016; SRS-FR-WORD-014 | AC-FS08-13 | TC-PROG-016 |
| PRD-NFR-004 | NFR-PERF-008 | AC-FS08-20 | TC-PROG-015 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | An east-bound long-haul flight can skip a local date and break the streak (S10). Accept as SRS-BR-011 states, or allow a grace rule? | Product Owner |
| Q2 | Is a milestone celebrated once ever, or once per streak run? Recommended: once per run | Product Owner |
| Q3 | Should the app detect manual device-date changes for streaks? Recommended: no (self-deception only, THR-05) | Product Owner / Security Engineer |
| Q4 | "Sessions completed" — this week or all time on the overview? Recommended: this week (all time on Profile) | Product Owner |
| Q5 | Default trend period: last 30 days † or all time? | Product Owner / UX |
| Q6 | What streak rule applies to users who cannot complete a speaking step (DES-07 OQ-A11Y-1)? | Product Owner |
| Q7 | Is the backup prompt threshold (≥ 3 valid attempts †, re-shown after 30 days †) acceptable? | Product Owner |
| Q8 | May the accuracy rating be added or changed from attempt detail (ENT-13 is separate from the immutable attempt)? | Product Owner (with FS-05) |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | "Consecutive streak days ending today or yesterday" does not cover a latest dateKey later than today, which happens after a west-bound time-zone change (S9); a literal reading could reset a live streak | SRS-BR-011 | Add: a latest streak day later than today counts as current (§5.2 step 4) |
| 2 | Practice time has three definitions: sum of recording durations (SRS-BR-018), "shown as minutes speaking" (suggests M-SPK, voiced time only) and "recorded speaking and session time" (DES-05) | SRS-BR-018, DES-05 §12.2 | Keep recording duration; change the label and DES-05 wording |
| 3 | SRS-BR-024 requires a noise band for every trend, but ARC-05 §6 defines bands for 7 metrics only; PRD-FR-070 limits trends to measured metrics while SRS-FR-PROG-002 says "metric" | SRS-BR-024, SRS-FR-PROG-002, PRD-FR-070, ARC-05 §6 | List the trend metrics explicitly (§5.5 proposal) |
| 4 | M-PAUSE is a count per attempt, so attempts of different length in one family are not comparable; comparability of M-RATIO / M-PAUSE across capture tiers (T1 VAD vs T2 recogniser RMS callbacks) is not defined | ARC-05 §3, §6 | Define a per-minute pause rate with its band; state tier comparability |
| 5 | How a trend "change" is computed (first vs last, averages, slope) is not defined anywhere | SRS-BR-024, ARC-05 §6 | Adopt the §5.5 Δ rule or specify another |
| 6 | DES-05 CH-2 draws a line from 2 points, contradicting the 3-attempt minimum for trend lines | DES-05 §11.18 vs SRS-BR-024 | DES-05 CH-2: "fewer than 3 points: no line" |
| 7 | "One-time celebration" does not say whether a milestone can be earned again in a later streak run | SRS-FR-PROG-006 | Decide Q2 |
| 8 | dateKey is "at the time of the event", which is ambiguous for an attempt that crosses local midnight | SRS §1.2, SRS-BR-011 | State "local date when recording starts" (§5.2 step 7) |
| 9 | Check-ins use "same prompt as baseline per track", but the baseline is the single `onb-intro` activity for every track | SRS-FR-PROG-008 vs SRS-FR-ONB-009 | Define check-in activity per track, or use `onb-intro` for all |
| 10 | SEC-01 lists "per-attempt delete" for recordings and transcripts, but no SRS-FR or screen defines it; attempt detail is the natural place | SEC-01 §3.2 #1–2; SRS-FR-PROG-004 | Add an SRS-FR for per-attempt deletion |
| 11 | The period of "sessions completed", "words learned" and "this week" (week start) is not defined; DES-05 §12.3 suggests "Words at Review stage" instead of "Words learned" | SRS-FR-PROG-001, DES-05 §12.3 | State periods and the locale week; align the label with FS-07 §5.1 |
| 12 | DES-05 §11.16 defers the streak rule for users who cannot speak to DES-07 OQ-A11Y-1, but SRS-BR-010 requires a speaking attempt and no product rule exists | DES-05 §11.16, SRS-BR-010 | Decide Q6 and record it in SRS §16 |
| 13 | SRS-FR-PROG-004 promises transcript and playback in attempt detail but does not cover synced attempts from another device, which have neither (NG-12, D-16) | SRS-FR-PROG-004 | Add the "stays on the phone that made it" case |
