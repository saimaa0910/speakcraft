# FS-03 — Today: Daily Session

| Field | Value |
|---|---|
| Document ID | FS-03 |
| Version | 0.2 — Draft for review (2026-11-21, consistency review CL-41/CL-42: Skip semantics, no-history plan rule, "unpractised" defined — all RECOMMENDATION) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-03; PRD §15.3 (PRD-FR-020…026), §14 J2; PRD-FR-056 (techniques used by Today), PRD-FR-061 (word goal feeds Today, owned by FS-07); SRS §6.3 (SRS-FR-TODAY-001…011), SRS-FR-PRAC-008 (plan may select by technique), SRS-BR-008…014, -018, -020, -025…028, SRS-FR-SPCH-007, SRS-ERR-005; D-08 (decided 2026-11-21); NFR-PERF-001/007/008, NFR-REL-004/006, NFR-MAINT-005, NFR-QUAL-006, NFR-A11Y-*; ARC-01 §2, §4; ARC-03 (ENT-01/02/03/04/05/06/07/09/10); ADR-014; DES-02 (SCR-TODAY-01, SCR-TODAY-02); DES-05 §12; GOV-02 §6.1; FS-02, FS-04, FS-05, FS-06, FS-07, FS-08, FS-09, FS-11, FS-12; BRD KPI-03/04 |

---

## 1. Feature ID & purpose

**FS-03 / CAP-03 (pillar P-1).** Give every user **one daily session** of 3–4 steps that fits the time they chose (5, 10 or 15 minutes), is built **deterministically** from their goal, level, weakest measured skill, due words and recent history, stays **stable for the whole local day**, recovers gently after missed days, and hands each completed day to FS-08 for streak and progress. Today is the start destination and the single place that answers "What should I practise now?" (DES-02 §2.1). This FS also covers the personal greeting (GOV-02 §6.1: replaces the requested `WELCOME_EXPERIENCE.md` and `PERSONALIZED_LEARNING_ENGINE.md`).

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "I have ten minutes — just tell me what to practise." | Three daily plans on three screens; a dashboard with 15+ sections (FND-DOM-01, FND-UI-02) | All; UN-01 |
| "The app ignores what I told it I need." | The chosen goal never reaches the dashboard plan (FND-DOM-03); "Elementary" and "Upper-intermediate" collapse to Intermediate in the activity plan (data_learning_audit §3.1) | Aarav (P1), Daniel (P3); UN-01 |
| "I finished a step and it disappeared." | The activity plan is regenerated after every attempt and completed items are replaced (FND-DOM-10) | Aarav (P1), Sofia (P4) |
| "Ticking a box is not practice." | Plan tasks and the daily routine complete with one tap; minutes are fabricated (FND-DOM-06, FND-DOM-08) | Sofia (P4); PP-4, PP-7 |
| "After a few missed days I feel behind." | Recovery exists only in a largely unused engine; four streak numbers disagree (FND-DOM-02, FND-DOM-07) | Daniel (P3), Hoang (P5); UN-06 |
| "It said 'Welcome back' the first time I opened it." | FND-UI-07, DES-01 UXF-27 | All |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | SCR-TODAY-01 (greeting, session card, streak summary, today's progress); `PlanEngine` generation once per dateKey; composition for 5/10/15 min; candidate selection and tie-break; recovery mode; swap (≤ 3 per day †); change today's duration; regeneration of incomplete steps only; launching steps into FS-04 / FS-07 flows; SCR-TODAY-02 session complete; `SESSION_COMPLETED` event; disabled speaking steps without microphone permission; T3b preference for delivery-oriented activities | Check-in offered as the main step when due (SRS-FR-PROG-008, owned by FS-08); bell with unread badge on the Today top bar (FS-09) |
| **Out** | Streak, practice-time and milestone computation (FS-08); word stages and review scheduling (FS-07); session player and interruptions (FS-04); evaluation and feedback (FS-05); content schema (FS-06); reminders (FS-09); any second plan, recommender or "AI" suggestion (NFR-MAINT-005, NG-01, NG-02); carrying missed sessions forward as backlog (PRD-FR-023); manual tick-off of steps (PP-4) | — |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Show Today | Onboarding complete (`onboardingCompletedAt` set, FS-02); ENT-01 Profile has track, level and daily minutes; content pack loaded by `ContentRepository`; current dateKey available from `Clock`/`DateKeyProvider` |
| Start a speaking step | RECORD_AUDIO granted (otherwise SCR-PERM-01, FS-04); speech tier known (FS-12) |
| Start a word step | Word bank in the content pack (FS-07); for stages 2–4, a transcript-capable tier (see Issue 3) |
| Network | None required for any Today workflow (PRD-FR-100) |

## 5. Main workflow

### 5.1 Open Today and generate the plan (SRS-FR-TODAY-001…003, -006)

1. App start or tab selection shows SCR-TODAY-01. `TodayViewModel` reads the current dateKey from `DateKeyProvider` and observes `PlanRepository.plan(dateKey)`.
2. If no ENT-05 SessionPlan exists for the dateKey, the ViewModel assembles the inputs of SRS-FR-TODAY-002: track, level, daily minutes (`ProfileRepository`); last 14 days of attempts and the dateKey of the last valid attempt (`AttemptRepository`); measured weakest metric family (`ProgressCalculator`, §8.5); due reviews, the word in progress and the new-word goal (`WordRepository`, ENT-02 via `PreferencesRepository`); content version, activity index and each activity's technique links (`ContentRepository`, SRS-FR-PRAC-008); speech tier (`SpeechCapabilityService`, needed by SRS-FR-SPCH-007). Technique progress (exercises practised per technique) is derived from the same attempts, not stored separately (RECOMMENDATION).
3. `PlanEngine.generate(inputs, dateKey)` — pure Kotlin, no I/O, no clock, no randomness — returns ordered steps per §8.2–§8.4 within 150 ms on RD-LOW (NFR-PERF-007).
4. `PlanRepository` stores the SessionPlan and its ENT-06 SessionSteps in one transaction (SRS-FR-DATA-001) and, for signed-in users, creates an ENT-10 SyncOutbox entry for the plan document (FS-11). A plan that exists for the dateKey is never regenerated (SRS-BR-013).
5. Today renders the greeting (§5.5), the session card (steps with template, title and estimated minutes; "about {n} min" total; one primary **Start**), the streak summary and today's progress ("{done} of {n} steps · {m} min speaking today", values from `ProgressCalculator`, FS-08).

### 5.2 Run the session

1. **Start** (later **Continue**) opens the first incomplete step: speaking step → SCR-SESS-01 with `activityId`, `stepId`, `source = TODAY` (FS-04); new-word stage → SCR-WORD-03; review set → SCR-WORD-04 (FS-07).
2. When the attempt passes the evidence gate (FS-05), `AttemptRepository` saves it and sets `SessionStep.completedByAttemptId` in the same transaction (SRS-BR-009, ARC-01 §4.1). An Inconclusive attempt leaves the step incomplete (SRS-BR-008).
3. RECOMMENDATION: the result's primary action is **Next step** while incomplete steps remain and **Finish** after the last one; Back always returns to SCR-TODAY-01 with progress kept. Room Flows update Today without refresh (ARC-01 §4.2).
4. When the last step completes (every step done, or done or skipped per A8), `PlanRepository` sets `SessionPlan.completedAt` and writes one ENT-09 `SESSION_COMPLETED` event (§8.6); navigation shows SCR-TODAY-02 (§5.6).

### 5.3 Swap a step (SRS-FR-TODAY-009)

1. Each incomplete speaking step has a **Swap** action (overflow menu, RECOMMENDATION).
2. `PlanEngine.nextCandidate(plan, step)` returns the next-best candidate of the same template family (Read/Repeat or Answer/Structured/Story, SRS-FR-PROG-002) using the ranking of §8.4, excluding activities already in today's plan and activities swapped out today.
3. `PlanRepository` replaces the step (same position, `swappedFrom` recorded) and increments `SessionPlan.swapsUsed`. After 3 swaps † the action is disabled with "You've used today's swaps."
4. RECOMMENDATION: word steps are not swappable (their content follows due reviews, FS-07); completed steps are never swappable.

### 5.4 Change today's duration (PRD-FR-026, SRS-FR-TODAY-007)

1. The session card shows a duration chip ("10 min"). Choosing 5, 10 or 15 sets `SessionPlan.minutesOverride` for **today only** (RECOMMENDATION; the profile default changes only in SCR-SET-02).
2. `PlanEngine.regenerate(plan, newInputs)` keeps completed steps in place and in order and recomposes only incomplete steps (SRS-BR-013). In recovery mode the effective duration stays min(chosen, 10) (SRS-BR-014; Open question Q2).

### 5.5 Greeting (SRS-FR-TODAY-010; PRD-FR-024)

| Situation | Text (RECOMMENDATION wording; structure per SRS) |
|---|---|
| First dateKey on which Today is ever shown | "Welcome, {name}." — never "welcome back" |
| Any later dateKey | "Good morning / Good afternoon / Good evening, {name}." with local-time bands 05:00–11:59 / 12:00–17:59 / 18:00–04:59 † |
| No display name (local users; FS-02 Issue 4) | Same text without the name: "Welcome." / "Good morning." |
| Factual line (later dateKeys only; first rule that applies) | 1 current streak ≥ 2: "You're on a {n}-day streak." · 2 recovery mode: "Here's a lighter session to get going again." · 3 last stored focus exists: "Last time you worked on: {focus}." · 4 reviews due: "{n} words are ready for review." · 5 none: no line |

Every factual line is read from stored data (PP-1); no line is invented when data is missing. The first-visit marker is stored once (`firstTodayDateKey`) so reinstall-restore or sync never shows "Welcome" again to an existing user (RECOMMENDATION).

### 5.6 Day completion and streak hand-off to FS-08 (SRS-FR-TODAY-011)

1. SCR-TODAY-02 shows: what was practised (each step with template and one headline result in plain words, from stored metrics), today's speaking time (SRS-BR-018, computed by FS-08), the current streak and the **next suggested optional activity** (the best-ranked main-pool candidate not in today's plan, §8.4) with **Try it** → SCR-PRAC-03 and **Done** → SCR-TODAY-01.
2. Streak and milestones are **not** computed here: `ProgressCalculator` derives them from valid attempts (SRS-BR-010, -011). A streak day is earned by the first valid speaking attempt of the dateKey from any source; session completion is not required (SRS-BR-012). If a milestone is reached, FS-08 shows its one-time celebration on SCR-TODAY-02 (SRS-FR-PROG-006).
3. For the rest of the dateKey, SCR-TODAY-01 shows a compact "Today's session is complete" card with the same suggestion (RECOMMENDATION). Further practice from Practice or Words still counts toward progress (PRD-FR-052).

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | Last valid attempt is ≥ 2 local days old (SRS-FR-TODAY-008) | Recovery plan per SRS-BR-014 and §8.2: effective minutes min(chosen, 10), review set placed first when reviews are due, main step uses the user's most frequent valid template of the last 14 days, no focus step, nothing carried over from missed days. History is untouched |
| A2 | Track, level or daily minutes changed in SCR-SET-02 during the day | Incomplete steps regenerated (SRS-FR-TODAY-007); completed steps kept; progress and history never reset (SRS-BR-025) |
| A3 | The activity of an incomplete step gets a valid attempt from Practice, or the planned word stage / review set is finished from Words, on the same dateKey | RECOMMENDATION: the step is marked complete with that attempt (counts once; idempotent by attempt UUID, SRS-BR-020) |
| A4 | Today stays open across midnight, or the app resumes on a new dateKey | A new plan is generated for the new dateKey on resume; yesterday's plan is frozen as it was (incomplete steps are not carried over); an attempt keeps the dateKey of the moment it was recorded (SRS-BR-011) |
| A5 | Device time zone changes | dateKey follows the current zone; existing attempts keep their recorded dateKey (SRS-BR-011); a plan is generated only if none exists for the new dateKey |
| A6 | RECORD_AUDIO not granted | Speaking steps shown disabled with "Microphone needed" and **Allow microphone** (SCR-PERM-01); non-speaking word content (stage 1 Learn) remains usable (SRS-FR-ONB-007) |
| A7 | Tier is T3b, or online consent is withdrawn mid-day (T3a → T3b) | RECOMMENDATION: tier is a selection input; a tier change regenerates incomplete steps. In T3b the plan prefers delivery-oriented activities (SRS-FR-SPCH-007) and substitutes the word slot (§8.2 note c; Issue 3) |
| A8 | User chooses **Skip** on an Inconclusive result (FS-04) | RECOMMENDATION (consistency review, CL-41; the SRS does not define Skip): the step is marked **skipped** (ENT-06 status `SKIPPED`, ARC-03 §11.8) and the player moves to the next incomplete step. A skipped step is not complete (SRS-BR-009) and adds no practice time, but the session can still be completed when every step is done or skipped; the streak is unaffected — a streak day comes only from a valid attempt and does not need session completion (SRS-BR-010, SRS-BR-012). See Issue 5 |
| A9 | Onboarding first attempt was skipped (FS-02 A1) | Optional "Introduce yourself" card below the session card; it is not a plan step |
| A10 | Signed-in user practises on two devices on the same dateKey | Plan document merges per ADR-014; RECOMMENDATION: step completion is re-derived from synced attempts (`stepId`, `dateKey`, `activityId`) so a last-writer-wins plan write cannot remove a completion. See Issue 8 |
| A11 | App update brings a new content version during the day | Today's plan keeps its steps (`activityId` + `contentVersion`, SRS-BR-026); the new version is used from the next dateKey (RECOMMENDATION) |
| A12 | R2: check-in due | Plan offers the check-in activity as the main step (SRS-FR-PROG-008; FS-08) |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Plan write fails | Retry once automatically; then "Couldn't prepare today's session. Try again." + Retry; no unsaved plan is shown as today's plan | SRS-ERR-005 |
| E2 | Reading plan inputs fails | Error state "Couldn't load today's session. Try again." + Retry; PII-free diagnostic | SRS-ERR-005 |
| E3 | No eligible candidate for a required slot | Relax filters in the order of §8.3 note; if still none, Empty state; diagnostic logged (should be prevented by FS-06 minimum counts) | — |
| E4 | Step-completion write fails inside the attempt transaction | Whole transaction rolled back; FS-04 shows its save error; the step stays incomplete | SRS-ERR-005 |
| E5 | Device clock set to an earlier date | Plan for that dateKey loaded if present, otherwise generated; no history is rewritten | — |
| E6 | Swap requested but no candidate remains | Swap disabled; "No other activity of this type is available today." | — |

## 8. Business rules

### 8.1 Rules applied (authoritative text in SRS §16)

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-008 | Inconclusive attempts never complete a step or count as practice time or streak days |
| SRS-BR-009 | A step is complete when it has a valid attempt, or when its review set is finished |
| SRS-BR-010, -011 | Streak day and current/longest streak — consumed from FS-08, shown on Today and SCR-TODAY-02 |
| SRS-BR-012 | A streak day does not require session completion |
| SRS-BR-013 | One plan per dateKey; regeneration touches only incomplete steps; completed steps never move |
| SRS-BR-014 | Recovery after ≥ 2 local days: ≤ 10 min, due reviews and a familiar template prioritised, no backlog |
| SRS-BR-018 | "Minutes speaking today" is the sum of valid recording durations (FS-08 computes) |
| SRS-BR-020 | Attempts and the `SESSION_COMPLETED` event are idempotent by UUID |
| SRS-BR-025 | Changing level or track never resets progress |
| SRS-BR-026 | Steps reference `activityId` + `contentVersion`; retired content stays readable |
| SRS-BR-027 | New-word goal default 2 per day † (0–5); review set ≤ 10 words per step |
| SRS-FR-SPCH-007 | In T3b the plan engine prefers delivery-oriented activities |

### 8.2 RECOMMENDATION — session composition (scales SRS-FR-TODAY-004; PRD-FR-020 "3–4 steps")

| Slot (default order) | Content | 5 min | 10 min (SRS-FR-TODAY-004) | 15 min | Recovery (SRS-BR-014) |
|---|---|---|---|---|---|
| 1 Warm-up | TPL-READ or TPL-REPEAT, own track preferred | 1 | 1 | 1 | 1 |
| 2 Main | Track-matched TPL-ANSWER / TPL-STRUCT / TPL-STORY | 1 (TPL-ANSWER preferred: shortest) | 1 | 1 (TPL-STRUCT / TPL-STORY preferred) | 1, familiar template |
| 3 Word task (a) | Review set if reviews are due, else next stage of the word in progress, else stage 1 of a new word while words started today < new-word goal | 1 (review set ≤ 5 words †) | 1 (≤ 10 words, SRS-BR-027) | 1 (≤ 10 words) | 1; placed first when reviews are due |
| 4 Focus / retry (b) | Retry of a recent activity, or an activity targeting the weakest measured family | 0 | 0–1 (only if a candidate exists and the budget allows) | 1 | 0 |
| **Steps / target** | | **3 / ≈ 5 min** | **3–4 / ≈ 10 min** | **4 / ≈ 15 min** | **3 / ≤ 10 min** |

Notes. (a) If no word task is available (goal 0 and nothing due) or the tier is T3b (Issue 3), the slot is filled by the next-best main-pool candidate of a different template where possible. (b) Focus candidate: the most recent valid main-slot attempt of the last 14 days whose stored focus maps to the weakest family (§8.5) and whose activity is not already planned → step "Try again: {title}" (FS-04 comparison); otherwise the best-ranked main-pool activity whose `focusFamilies` contains the weakest family, preferring an exercise of a technique whose tips target that family (PRD-FR-056, SRS-FR-PRAC-008); 15-min plans without a weakest family use the best-ranked main-pool activity of another template. (c) Budget: step estimates come from content `estimatedMinutes` (FS-06); word stage 2 min †, review 0.3 min per word †. If the total exceeds effective minutes × 1.25 †, drop the focus step, then prefer TPL-ANSWER for the main step, then shrink the review set; never fewer than 3 steps.

### 8.3 RECOMMENDATION — candidate filtering (hard constraints, applied per slot)

1. Activity is ACTIVE in the loaded content pack, `planEligible = true` (excludes `onb-intro`), and its template fits the slot.
2. Main and focus slots: activity track = user track (SRS-FR-TODAY-004). Warm-up: any track (own track scored higher).
3. Level within ±1 of the user's level (D-17 five-level order).
4. SRS-FR-TODAY-005: exclude an activity with a valid attempt in the previous 7 days if an **unpractised** alternative of the same template and track exists, where unpractised = no valid attempt for that `activityId` (RECOMMENDATION; consistency review, CL-42; Issue 7). The focus "Try again" retry is exempt (Open question Q1).
5. Exclude activities already placed in today's plan and activities swapped out today.
Relaxation order when a slot has no candidate (E3): drop rule 4, then widen rule 3 to ±2, then allow any track for the main slot.

### 8.4 RECOMMENDATION — scoring, ordering and tie-break (deterministic, SRS-FR-TODAY-003)

| Factor | Points † |
|---|---|
| Base | 100 |
| Level equals user level / adjacent level | +30 / +10 |
| Warm-up only: activity track = user track | +20 |
| Activity `focusFamilies` contains the weakest measured family (§8.5) | +25 |
| No valid attempt ever on this activity | +15 |
| Valid attempt in the previous 7 days (reachable only after relaxation, or when no unpractised alternative exists) | −60 |
| Any attempt (valid or Inconclusive) in the last 14 days | −10 |
| Recovery mode and template = most frequent valid template of the last 14 days | +30 |
| Tier T3b and template ∈ {TPL-ANSWER, TPL-STORY} (delivery-oriented, SRS-FR-SPCH-007) | +20 |
| Activity is the next unpractised exercise of a technique in progress (≥ 1 of its exercises has a valid attempt, ≥ 1 has none) — "select by technique" (SRS-FR-PRAC-008) | +10 |

Ordering: score descending; **tie-break by stable hash** — ascending FNV-1a 64-bit over the UTF-8 bytes of `activityId + "|" + dateKey`; final fallback `activityId` ascending. The engine must not use `Random`, the wall clock, `hashCode()` of collections or map iteration order. Same inputs + dateKey ⇒ byte-identical plan; a different dateKey reorders ties so the same equal-scoring activity does not win every day. `planEngineVersion` is stored with the plan so behaviour changes are traceable.

### 8.5 RECOMMENDATION — "measured weakest metric family" (SRS-FR-TODAY-002; see Issue 1)

Families use **Measured** metrics only (PRD-FR-021 "weakest measured skill"): PACE (M-WPM), PAUSES (M-PAUSE), LENGTH (M-LEN), CLARITY (M-MATCH). For each family with ≥ 3 † non-null values among valid attempts of the last 14 days, compute the share of values outside the metric's target band (bands per FS-05 §8.2). Weakest = highest share > 0; ties in the order PACE, PAUSES, LENGTH, CLARITY; otherwise none (no +25 bonus, no focus step in 10-min plans). Estimated metrics (M-FILL, M-STRUCT, M-LEX) are not used.

**No valid attempt yet (RECOMMENDATION; consistency review, CL-42).** A user with no valid attempt (new user, or onboarding attempt skipped) has no weakest family. The plan then uses the track's default rotation — warm-up + a level-appropriate main task (plus the word task per §8.2) — ranked by §8.4 without the weakest-family bonus, instead of a "weakest metric".

### 8.6 RECOMMENDATION — completion and idempotency

`SESSION_COMPLETED` is written once per dateKey with a name-based UUID (v5) of `profileOrUid|dateKey|SESSION_COMPLETED`, so a retry, a second device or a re-sync cannot create duplicates (SRS-BR-020). A completed plan stays completed even if a later attempt on one of its activities is Inconclusive.

## 9. UI states

**SCR-TODAY-01 Today**

| State | What the user sees | Actions |
|---|---|---|
| Default — not started | Greeting (+ factual line); session card "Today's session · {n} steps · about {m} min" with step rows (template name, title, minutes; "Part of: {technique}" when the activity belongs to a technique, technique name in our wording, no creator name — SRS-BR-028); streak summary; "0 of {n} steps" | Start; Swap (per speaking step); duration chip; avatar → SCR-PROF-01 |
| In progress | Completed rows show "Done" text and a check icon; next row highlighted; progress "{done} of {n} steps · {m} min speaking today" | Continue; Swap (incomplete speaking steps only) |
| Complete | Compact "Today's session is complete" card, what was practised, suggested optional activity (SRS-FR-TODAY-011) | Try it (→ SCR-PRAC-03); open Progress |
| Recovery | Card subtitle "A lighter session to get going again"; duration ≤ 10 min | Start |
| Loading | Skeleton card, shown only if the plan is not ready after 300 ms (generation target ≤ 150 ms, NFR-PERF-007) | — |
| Empty | "We couldn't build today's session from your current goal and level." (E3) | Change in SCR-SET-02; Browse Practice |
| Error | E1/E2 message | Retry |
| Offline | Identical to Default — no network is used; no banner | — |
| Permission | Speaking rows disabled with "Microphone needed" (A6) | Allow microphone (→ SCR-PERM-01) |
| T3b | Rows whose check needs a transcript are labelled "Transcript needed for this check" (SRS-FR-SPCH-007); word slot substituted | Start |
| First visit | "Welcome, {name}." without a factual line | Start |
| Onboarding attempt skipped | Optional "Introduce yourself" card (A9) | Start; Not now |

**SCR-TODAY-02 Session complete**

| State | What the user sees | Actions |
|---|---|---|
| Default | "Today's session is complete"; per-step one-line results; minutes speaking today; streak; next suggestion | Done (→ SCR-TODAY-01); Try it (→ SCR-PRAC-03) |
| Milestone | FS-08 one-time celebration above the summary, reduced-motion aware (SRS-FR-PROG-006) | Done |
| Loading | N/A — summary uses data already loaded by the player | — |
| Empty | N/A — reachable only after the last step completes | — |
| Error | Summary query fails: header and Done only; no placeholder numbers (PP-1) | Done |
| Offline | Identical to Default | — |
| Permission | N/A — no permission used | — |

## 10. Data requirements

| Entity / store | Fields needed by FS-03 (minimum; ARC-03 authoritative) |
|---|---|
| ENT-05 SessionPlan | `planId`, `profileId`, `dateKey` (unique per profile), `dailyMinutes` (effective), `minutesOverride?`, `recovery`, `contentVersion`, `planEngineVersion`, `inputsDigest` (for determinism checks), `swapsUsed`, `revision`, `generatedAt`, `completedAt?`, `updatedAt`, `deviceId` |
| ENT-06 SessionStep | `stepId`, `planId`, `position`, `slot` (WARM_UP / MAIN / WORD / FOCUS), `kind` (SPEAKING / RETRY / WORD_STAGE / REVIEW_SET), `activityId?`, `wordId?`, `stage?`, `reviewWordIds?`, `templateId?`, `estimatedMinutes`, `status`, `completedByAttemptId?`, `completedAt?`, `swappedFrom?` |
| ENT-01 Profile | `track`, `level`, `dailyMinutes`, `displayName?`, `firstTodayDateKey?` |
| ENT-02 LearningPreferences | `newWordGoal` (SRS-BR-027) via `PreferencesRepository` |
| ENT-03 / ENT-04 | Read: last 14 days of attempts (`activityId`, `templateId`, `dateKey`, `result`, `stepId`, `source`, stored focus code) and headline metric values with bands |
| ENT-07 / ENT-08 | Read: due reviews, word in progress and next stage, words started today (FS-07) |
| ENT-09 LearningEvent | `SESSION_COMPLETED` (`eventId` per §8.6, `dateKey`, `planId`) |
| ENT-10 SyncOutbox | Plan upserts and the event, signed-in users only (FS-11) |
| Content pack (ARC-03 §6) | Per activity: `id`, `track`, `template`, `level`, `estimatedMinutes`, `focusFamilies`, `techniqueIds`, `status`, `planEligible`, `licenceStatus` (FS-06 §10). Only shippable items are in a release pack (SRS-BR-028; D-08 decided 2026-11-21). Technique exercises pass the same filters as any activity; the only technique-specific rules are the +10 factor of §8.4 and the focus preference of §8.2 note (b) (RECOMMENDATION) |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `PlanEngine` (domain) | `generate`, `regenerate`, `nextCandidate` — pure functions, unit-tested exhaustively (WBS T-08.1) |
| `PlanRepository` | Plan Flow per dateKey; transactional create / regenerate / swap; completion; outbox |
| `AttemptRepository` | Recent attempts; step completion in the attempt transaction |
| `ProgressCalculator` | Weakest family (§8.5), streak summary, minutes speaking today (FS-08) |
| `WordRepository` (+ `ReviewScheduler`) | Due reviews, word in progress, words started today (FS-07) |
| `ContentRepository` | Activity index by track / template / level; content version |
| `ProfileRepository`, `PreferencesRepository` | Track, level, minutes, name, first-visit marker, new-word goal |
| `SpeechCapabilityService` | Current tier (T3b preference) |
| `Clock` / `DateKeyProvider` | Testable dateKey and time-of-day greeting |
| Platform | None (no network, no permission for Today itself) |

## 12. Offline behaviour

Every Today workflow works offline: generation, start, swap, duration change, completion and greeting use local data only (PRD-FR-100, NFR-REL-006). For signed-in users the plan and the `SESSION_COMPLETED` event are queued in the outbox and uploaded later (FS-11); Today never shows a sync indicator. T3a users who are offline record with audio-only feedback for that attempt (FS-04).

## 13. Privacy requirements

- Plans contain activity IDs, word IDs and status only — no transcript or audio data; the synced fields are those in ARC-03 §8 (NFR-PRIV-003).
- The display name in the greeting is read locally; it is never logged (NFR-SEC-002).
- No analytics on plan contents, swaps or greetings (SRS-PRIV-004, ADR-013).

## 14. Accessibility notes

Greeting is a heading; each step row uses merged semantics announcing position, template, title, minutes and state ("Step 2 of 4, Answer a question, about 3 minutes, done") (A11Y-07, A11Y-06); Done/next states carry text, not colour only (A11Y-23, NFR-A11Y-006); Start, Swap and the duration chip are ≥ 48 dp (A11Y-19, NFR-A11Y-004); swap and regeneration results are announced through a polite live region (A11Y-08); the card reflows at 200 % font scale without fixed heights (A11Y-13, NFR-A11Y-002); the completion celebration respects reduced motion (A11Y-21, NFR-A11Y-005); minutes are spelled out in semantics (A11Y-34) and use plurals (A11Y-36). Users who cannot complete speaking steps: DES-07 OQ-A11Y-1 (Q3).

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS03-01 | No plan exists for today; track TRK-WORK, level Intermediate, 10 minutes | Today is opened | A plan is stored with a TPL-READ/REPEAT warm-up, a TRK-WORK TPL-ANSWER/STRUCT/STORY main step, one word task and at most one focus step, with one primary Start |
| AC-FS03-02 | Identical plan inputs and dateKey | The plan is generated twice | Both plans are identical in steps, activities and order |
| AC-FS03-03 | Step 1 is complete | The user changes today's duration from 10 to 15 minutes | Step 1 keeps its position and attempt; only incomplete steps are recomposed, giving 4 steps |
| AC-FS03-04 | Activity X (TRK-WORK, TPL-ANSWER) has a valid attempt 3 days ago and another TRK-WORK TPL-ANSWER activity is unpractised (no valid attempt) | The plan is generated | X is not selected |
| AC-FS03-05 | The last valid attempt was 3 local days ago, daily minutes 15, 4 reviews due | Today is opened | The plan is a recovery plan of ≤ 10 minutes with the review set first, the user's most frequent template as main step and no steps for the missed days |
| AC-FS03-06 | An incomplete main step | The user swaps it 3 times | Each replacement is the next-best activity of the same template family and the fourth swap is not available † |
| AC-FS03-07 | A user who has never seen Today | Today is first shown | The greeting is "Welcome, {name}." and contains no "welcome back"; on the next dateKey at 09:00 it is "Good morning, {name}." with one factual line |
| AC-FS03-08 | The last incomplete step receives a valid attempt | The result is confirmed | SCR-TODAY-02 shows the summary and a suggestion, exactly one `SESSION_COMPLETED` event exists for the dateKey, and Today shows the Complete state |
| AC-FS03-09 | No valid attempt yet today | The user makes one valid attempt and stops | The streak summary counts today although the session is incomplete (SRS-BR-012) |
| AC-FS03-10 | A plan step is in progress | Its attempt is Inconclusive | The step stays incomplete and today's progress does not change |
| AC-FS03-11 | RECORD_AUDIO is not granted | Today is shown | Speaking steps are disabled with "Microphone needed" and an Allow microphone action |
| AC-FS03-12 | Today is open at 23:58 with an incomplete plan | The app is resumed at 00:05 | A new plan exists for the new dateKey and yesterday's plan is unchanged |
| AC-FS03-13 | Airplane mode | The user opens Today and starts each step | The plan is generated and every step can start |
| AC-FS03-14 | The main step's activity is incomplete in today's plan | The user completes it validly from Practice | The Today step shows Done (RECOMMENDATION A3) |
| AC-FS03-15 | Two candidates have equal scores | Plans are generated for two different dateKeys | Each order follows the stable hash of `activityId|dateKey`, and the order is reproducible |
| AC-FS03-16 | Tier T3b | The plan is generated | No word-stage step is planned and delivery-oriented templates are preferred (RECOMMENDATION; SRS-FR-SPCH-007) |
| AC-FS03-17 | A technique with 1 of its 3 exercises practised, and an unrelated main-pool activity otherwise scoring the same | The plan is generated | The technique's next unpractised exercise is selected, and its step shows "Part of: {technique}" without any creator name (RECOMMENDATION; SRS-FR-PRAC-008, SRS-BR-028) |

## 16. Test cases

No test case has been executed in Stage A (GOV-02 DC-4).

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-TODAY-001 | 10-minute composition | unit | Fake content index (all tracks/templates/levels); 10 min | Generate | Warm-up + track main + word task (+ ≤ 1 focus); 3–4 steps | SRS-FR-TODAY-004 |
| TC-TODAY-002 | 5- and 15-minute composition | unit | As 001 with 5 and 15 min | Generate each | 3 steps / 4 steps per §8.2; budget rule respected | SRS-FR-TODAY-004 |
| TC-TODAY-003 | Determinism | unit | 1,000 generated input sets | Generate each twice | Byte-identical serialised plans | SRS-FR-TODAY-003, NFR-QUAL-006 |
| TC-TODAY-004 | Stable hash tie-break | unit | Golden FNV-1a values for fixed IDs and dateKeys | Rank tied candidates | Order equals golden order; differs between dateKeys | SRS-FR-TODAY-003 |
| TC-TODAY-005 | 7-day exclusion | unit | X valid 3 days ago; unpractised alternative exists | Generate | X excluded | SRS-FR-TODAY-005 |
| TC-TODAY-006 | 7-day fallback | unit | Only X exists for the slot | Generate | X selected after relaxation | SRS-FR-TODAY-005 |
| TC-TODAY-007 | One plan per dateKey | unit | Fake `PlanRepository` with today's plan | Open Today twice; change unrelated data | Generation not called again | SRS-FR-TODAY-006, SRS-BR-013 |
| TC-TODAY-008 | Regeneration keeps completed steps | unit | Plan with steps 1–2 complete | Change minutes 10 → 5 and track | Steps 1–2 identical in position and attempt; others recomposed | SRS-FR-TODAY-007, SRS-BR-013 |
| TC-TODAY-009 | Recovery boundary | unit | Last valid attempt 1 day ago vs 2 days ago | Generate | Normal vs recovery | SRS-FR-TODAY-008, SRS-BR-014 |
| TC-TODAY-010 | Recovery composition | unit | Recovery, 15 min, reviews due | Generate | ≤ 10 min, review set first, familiar template, no focus | SRS-BR-014 |
| TC-TODAY-011 | Swap ranking and limit | unit | Plan with candidates | Swap main step 4 times | Next-best same family each time; 4th rejected † | SRS-FR-TODAY-009 |
| TC-TODAY-012 | Swap disabled in UI | Robolectric UI | `swapsUsed = 3` | Open step menu | Swap disabled with explanation text | SRS-FR-TODAY-009 |
| TC-TODAY-013 | Greeting rules | unit | Fake Clock at 04:59, 05:00, 11:59, 12:00, 18:00; first vs later dateKey | Build greeting | Correct variant; never "welcome back" on first visit | SRS-FR-TODAY-010 |
| TC-TODAY-014 | Greeting without name; factual line priority | unit | No display name; streak 4; reviews due | Build greeting | Name omitted; streak line chosen | SRS-FR-TODAY-010 |
| TC-TODAY-015 | Completion event idempotent | unit | Last step completed twice (retry and sync replay) | Complete | One `SESSION_COMPLETED` event | SRS-FR-TODAY-011, SRS-BR-020 |
| TC-TODAY-016 | Session complete screen | Robolectric UI | Completed plan with stored metrics | Show SCR-TODAY-02 | Steps, minutes, streak, suggestion shown; no placeholder values | SRS-FR-TODAY-011 |
| TC-TODAY-017 | Inconclusive does not complete | unit | Step in progress | Save Inconclusive attempt | Step incomplete; progress unchanged | SRS-BR-008, SRS-BR-009 |
| TC-TODAY-018 | Microphone not granted | Robolectric UI | Permission denied | Render Today | Speaking rows disabled; Allow microphone shown | SRS-FR-ONB-007 |
| TC-TODAY-019 | Midnight rollover | unit | Fake Clock 23:58 → 00:05 | Resume | New plan for new dateKey; old plan frozen | SRS-BR-011, SRS-BR-013 |
| TC-TODAY-020 | Today offline | instrumented | Airplane mode, RD-MID | Open Today; start each step | All succeed; no network request | PRD-FR-100, NFR-REL-006 |
| TC-TODAY-021 | Generation time | instrumented | RD-LOW, seeded DB of 3,000 attempts | Generate 10 times | P90 ≤ 150 ms † | NFR-PERF-007, NFR-PERF-008 |
| TC-TODAY-022 | Layout at 200 % font, dark theme | Robolectric UI | fontScale 2.0, 360 dp width, both themes | Render all §9 states | No clipped text; targets ≥ 48 dp | NFR-A11Y-002, -004 |
| TC-TODAY-023 | Weakest family | unit | Attempt sets with out-of-band values | Compute | Family per §8.5; none when < 3 values | SRS-FR-TODAY-002 |
| TC-TODAY-024 | Completion from Practice | unit | Planned activity attempted with `source = PRACTICE` | Save valid attempt | Step completed by that attempt | PRD-FR-052 (A3) |
| TC-TODAY-025 | T3b plan | unit | Tier T3b | Generate | No word-stage step; delivery templates preferred | SRS-FR-SPCH-007 |
| TC-TODAY-026 | Daily session end to end | manual | RD-MID, onboarded user, evidence capture | Complete a 10-minute session | Steps, completion, streak update and summary as specified | PRD-FR-020…024 |
| TC-TODAY-027 | Selection by technique | unit | Fake content with a technique (3 exercises, 1 practised) and an equal-scoring unrelated activity | Generate | Next technique exercise selected; step label carries the technique name; no creator name in any string | SRS-FR-PRAC-008, SRS-BR-028 |

## 17. Traceability

| PRD-FR | SRS-FR | AC | TC |
|---|---|---|---|
| PRD-FR-020 | SRS-FR-TODAY-001, -004, -011; SRS-BR-008/009 | AC-FS03-01, -08, -10, -11 | TC-TODAY-001, -002, -015, -016, -017, -018, -026 |
| PRD-FR-021 | SRS-FR-TODAY-002, -003, -005; SRS-FR-SPCH-007 | AC-FS03-02, -04, -15, -16 | TC-TODAY-003, -004, -005, -006, -023, -025 |
| PRD-FR-022 | SRS-FR-TODAY-006, -007 | AC-FS03-03, -12, -14 | TC-TODAY-007, -008, -019, -024 |
| PRD-FR-023 | SRS-FR-TODAY-008; SRS-BR-014 | AC-FS03-05 | TC-TODAY-009, -010 |
| PRD-FR-024 | SRS-FR-TODAY-001, -010; SRS-BR-012 | AC-FS03-07, -09 | TC-TODAY-013, -014, -016 |
| PRD-FR-025 | SRS-FR-TODAY-009 | AC-FS03-06 | TC-TODAY-011, -012 |
| PRD-FR-026 | SRS-FR-TODAY-007 | AC-FS03-03 | TC-TODAY-008 |
| PRD-FR-056 (Today part; library part in FS-06) | SRS-FR-PRAC-008 ("plan engine may select activities by technique") | AC-FS03-17 | TC-TODAY-027 |
| PRD-FR-061 (supporting; owned by FS-07) | SRS-FR-TODAY-002, -004; SRS-BR-027 | AC-FS03-01 | TC-TODAY-001, -002 |
| PRD-FR-100 (supporting) | SRS-AVL-001 | AC-FS03-13 | TC-TODAY-020 |
| PRD-NFR-004, -006 | NFR-PERF-007, NFR-A11Y-002/004 | — | TC-TODAY-021, -022 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | May the focus "Try again" step repeat an activity done in the last 7 days (it is a deliberate retry), as §8.3 rule 4 recommends? | Product Owner |
| Q2 | In recovery, if the user explicitly picks 15 minutes for today, does SRS-BR-014 still cap the session at 10? | Product Owner |
| Q3 | What completes a step for a user who cannot produce a valid speaking attempt (T3b word stages, speech impairment)? Link DES-07 OQ-A11Y-1 | Product Owner / UX |
| Q4 | Should the remaining new-word goal (2 per day †) be offered as an optional extra word step after the session, or only in Words (FS-07)? | Product Owner |
| Q5 | Are swaps also allowed for the warm-up step (same family), or only for main and focus steps? | Product Owner |
| Q6 | Plan retention is 90 days † (SRS §9); should completed plans older than that be pruned locally and in the cloud (FS-11)? | Database Architect |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | "Measured weakest metric family" is not defined: no families are listed, metrics have different units, and target bands exist only for M-WPM, so "weakest" cannot be computed reproducibly | SRS-FR-TODAY-002; PRD-FR-021; ARC-05 §6 | Define families and bands in ARC-05 (§8.5 proposes a method) |
| 2 | The new-word goal defaults to 2 per day, but a session contains exactly one word task (one stage or one review set), so the goal can never be met through Today alone | SRS-BR-027 vs SRS-FR-TODAY-004 | State where the rest of the goal is delivered (Q4) or allow one word step to cover several words |
| 3 | In T3b, word stages 2–4 cannot pass (transcript needed), yet every session must include a word task; the step either never completes or completes without progress | SRS-FR-SPCH-007, SRS-FR-WORD-003…005, SRS-BR-009, SRS-FR-TODAY-004 | Allow the plan engine to replace the word task in T3b (§8.2 note a) |
| 4 | The main task must be a track-matched free-speaking template, but no minimum exists per template within a track; Clear speech centres on read-aloud and repeat (Hoang, P5), so its main pool may be empty or tiny | SRS-FR-TODAY-004; PRD-FR-053; RD-16 CAP-06 | Set a per-template minimum per track (FS-06 §8.2) or allow TPL-READ/REPEAT main steps for Clear speech |
| 5 | Skip is offered on Inconclusive results, but a step completes only with a valid attempt; a skipped step blocks session completion for the day, and KPI-04 (session completion) would count it as abandoned | SRS-FR-SESS-007 vs SRS-BR-009, SRS-FR-TODAY-011; BRD KPI-04 | Define Skip (e.g. "skipped" counts for session completion but not for streak or practice time). RECOMMENDATION applied in A8 (consistency review, CL-41); SRS wording still needed |
| 6 | Recovery is triggered by "last valid attempt ≥ 2 days ago"; behaviour for a user with **no** valid attempt ever (first attempt skipped in onboarding) is undefined | SRS-FR-TODAY-008, SRS-BR-014; SRS-FR-ONB-011 | Recommend: no recovery until the first valid attempt exists |
| 7 | "Unpractised alternative" can mean never practised or not practised in the last 7 days; the two readings give different plans | SRS-FR-TODAY-005 | Clarify; §8.3 now uses unpractised = no valid attempt for that `activityId` (consistency review, CL-42); SRS wording still needed |
| 8 | Plans sync as mutable documents with last-writer-wins, so a write from a second device can drop a step completed on the first, contradicting "completed steps are never removed" | ADR-014 §3, SRS-BR-021 vs SRS-BR-013 | Derive step completion from synced attempts (A10) or merge steps per step |
| 9 | The greeting requires `{name}`, but onboarding never collects a name (also FS-02 Issue 4) | SRS-FR-TODAY-010 vs SRS-FR-ONB-001 | Define the name-less form (§5.5) |
