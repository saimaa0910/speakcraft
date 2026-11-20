# FS-09 — Reminders & Inbox

| Field | Value |
|---|---|
| Document ID | FS-09 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-09; PRD §15.9 (PRD-FR-080…084); BRD BR-012; SRS §6.9 (SRS-FR-REM-001…007), SRS-FR-ONB-005, SRS-FR-PLAT-002, SRS §8 (notifications, background work), §15, §16 (SRS-BR-008, -010, -016, -019); NFR-SEC-004, NFR-PRIV-*, NFR-REL-006, NFR-A11Y-*; ARC-01 §2, §5; ARC-03 ENT-02/03/07/16/18; ADR-005, ADR-014; SEC-01 §3.2, §6.7, SEC-REQ-31; DES-02 (SCR-SET-03, SCR-PERM-01, SCR-INBOX-01, SCR-TODAY-01); DES-05 §11.19, §12; FS-01, FS-02, FS-03, FS-07, FS-08, FS-10; decision D-12 |

---

## 1. Feature ID & purpose

**FS-09 / CAP-09 (pillar P-1).** Help people keep a short daily habit with **one respectful system reminder** at a time they choose: only if they ask for it, never after they have already practised that day, never at night unless they picked a night-time, and never with guilt or sensitive content. Reminders are scheduled on the device with WorkManager (ADR-005) and rescheduled after reboot, time-zone and clock changes. R2 adds an in-app **inbox** for learning messages (milestones, weekly summary) — never software-update notices.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "I set a reminder time and nothing ever arrives." | No system notifications: no POST_NOTIFICATIONS, WorkManager or AlarmManager; `preferredReminderTime` is never read (FND-DOM-14; data_learning_audit §6) | All — UN-07 |
| "I get the same alert twice, and the ones I dismiss come back." | Two in-app alert stores with different dedup keys; dismissed alerts re-added by the sync bridge (FND-DOM-14; audit §6) | All |
| "Don't guilt me into practising." | "Protect your N-day streak" alerts, possibly with conflicting streak numbers (audit §6) | Sofia, Meera — UN-07, PP-7 |
| "Update notices aren't learning messages." | Update content had to be filtered out of the alert system (audit §6; ADR-004) | All — BR-012 |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | Daily reminder on/off and time in Settings → Reminders (SCR-SET-03) and onboarding (SCR-ONB-05, FS-02); WorkManager scheduling; suppression rules (SRS-BR-019); due-review mention; channel "Practice reminders"; POST_NOTIFICATIONS on Android 13+ with SCR-PERM-01 education, denied and permanently denied states; rescheduling after reboot, time-zone change, clock change and preference change; tap opens Today | Inbox SCR-INBOX-01 (ENT-16); bell with unread badge on Today; milestone and weekly-summary messages; read/unread, mark all read, delete |
| **Out** | Software-update notices (BR-012, PRD-FR-084, ADR-004); push, FCM or server-sent messages (ADR-005 option C); exact alarms (`SCHEDULE_EXACT_ALARM`); more than one reminder per day; streak-loss or guilt wording (PP-7); marketing; notification action buttons (Q3); any other notification category in R1 (SRS-FR-REM-004); the milestone celebration itself (FS-08) | Inbox messages as system notifications (Issue 8) |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Reminder delivery | `reminderEnabled = true`; notifications can be shown (Android 13+: POST_NOTIFICATIONS granted; any version: app notifications and channel enabled); WorkManager initialised in `SpeakCraftApplication` (ARC-01 §2); app opened at least once since install or force-stop |
| Suppression check | Room readable (attempts, word progress) |
| Inbox (R2) | R2 build flag on (ARC-02 §9); facts available from `ProgressCalculator` |

## 5. Main workflow

### 5.1 Turn reminders on (SCR-SET-03)

1. User opens Settings → Reminders: toggle **Daily reminder**, time row **Around 19:00 †** (SRS-FR-ONB-005 default; "around" per ADR-005), toggle **Mention words due for review** (RECOMMENDATION, Issue 2) and the line "At most one reminder a day. We won't remind you if you've already practised today."
2. Toggle on → `PreferencesRepository` stores the preference.
3. Permission check (§5.6). Android 13+ and never asked → SCR-PERM-01 (notification variant) → system dialog. Granted → step 4. Denied → the preference stays on and the row shows "Notifications are off for SpeakCraft" (same behaviour as FS-02 A5).
4. The scheduling helper of `ReminderWorker` (class names per ARC-02) computes the next trigger (§5.2) and enqueues unique work; snackbar "Reminder set for around 19:00." (announced).
5. Changing the time (Material 3 time picker with keyboard input) replaces the scheduled work. Turning the toggle off cancels the work and removes any reminder already shown today.

### 5.2 Scheduling (SRS-FR-REM-001; ADR-005)

1. Inputs: `reminderEnabled`, `reminderTime` (local HH:mm), the device's **current** `ZoneId`, now (`Clock`).
2. Candidate = today at `reminderTime` in that zone (`ZonedDateTime`; a time inside a DST gap moves forward by the gap; in an overlap the earlier offset is used).
3. If the candidate is not after now, or a reminder was already posted for today's dateKey → candidate = tomorrow at `reminderTime`.
4. Enqueue a `OneTimeWorkRequest<ReminderWorker>` with `initialDelay = candidate − now`, unique name `daily_reminder`, `ExistingWorkPolicy.REPLACE`, no constraints, input `{scheduledFor, reminderTime, zoneId}`. Each run enqueues the next one (chained one-time work instead of 24-hour periodic work, so DST and zone changes cannot drift — RECOMMENDATION, Issue 7).
5. `scheduledFor` is stored in `device_state` for the app-start check and diagnostics.

| # | Situation (Europe/London unless stated; reminder 19:00 †) | Now | Next trigger | Delay |
|---|---|---|---|---|
| W1 | Enabled in the morning | Sat 21 Nov 10:00 GMT | Sat 21 Nov 19:00 GMT | 9 h |
| W2 | Enabled after the time | Sat 21 Nov 19:30 | Sun 22 Nov 19:00 | 23 h 30 min |
| W3 | Re-enqueued across the end of DST | Sat 24 Oct 19:00 BST (UTC+1) | Sun 25 Oct 19:00 GMT (UTC+0) | 25 h |
| W4 | Re-enqueued across the start of DST | Sat 28 Mar 19:00 GMT | Sun 29 Mar 19:00 BST | 23 h |
| W5 | Time zone changes London → New York before the trigger | Work set for Sun 22 Nov 19:00 GMT (14:00 New York); zone changes Sun 08:00 New York | Sun 22 Nov 19:00 EST (00:00 GMT Mon) | Replaced on ACTION_TIMEZONE_CHANGED; if the broadcast is missed, the worker runs at 14:00 New York, finds it is early (§5.3 step 2) and re-enqueues |
| W6 | User chose 02:30 (inside quiet hours) on the DST-start night | Sat 28 Mar 21:00 GMT | Sun 29 Mar 03:30 BST (gap shifted) | 5 h 30 min |

### 5.3 At the trigger — `ReminderWorker` (SRS-FR-REM-002, -003; SRS-BR-019)

Checks run in order. The first failing check skips posting, writes a PII-free reason code to the diagnostic log (ADR-007) and — except step 1 — re-enqueues the next trigger. The worker always returns success (a WorkManager retry would post late).

| Step | Check | If it fails |
|---|---|---|
| 1 | Reminders enabled; a profile exists; no account deletion in progress (ENT-18 step ≠ DONE) | Stop; no re-enqueue (FS-01 re-arms after deletion) |
| 2 | Local time ≥ `reminderTime` − 5 min † on today's dateKey (guards zone and clock changes) | `EARLY` — re-enqueue for the correct trigger, no post |
| 3 | Local time ≤ `reminderTime` + 2 h † ("near the chosen time", SRS-FR-REM-001) | `STALE` — skip |
| 4 | No reminder already posted for today's dateKey (max 1 per day) | `ALREADY_SENT` — skip |
| 5 | No **valid** attempt with today's dateKey, from any source (SRS-BR-010); Inconclusive attempts do not count (SRS-BR-008) | `PRACTISED` — skip |
| 6 | Local time outside 22:00–07:00 †, unless `reminderTime` itself is inside that window (SRS-BR-019) | `QUIET_HOURS` — skip |
| 7 | Notifications can be shown: `areNotificationsEnabled()`, channel importance ≠ NONE, and on API ≥ 33 POST_NOTIFICATIONS granted | `BLOCKED` — skip |
| 8 | Build content (§5.4); due count = `WordRepository.dueCount(today)` when the mention toggle is on (FS-07 §5.1 "Due") | — |
| 9 | Post with a fixed notification ID; store `lastReminderDateKey = today` | Exception → log, no retry today |
| 10 | Enqueue the next trigger (§5.2) | — |

RECOMMENDATION except where a rule is cited. Examples:

| # | Situation | Outcome |
|---|---|---|
| R1 | Valid attempt at 08:15 today; reminder 19:00 | Skipped — `PRACTISED` |
| R2 | Only an Inconclusive attempt today | Posted |
| R3 | Only a Learn stage or a quiz today (no speaking attempt) | Posted |
| R4 | Reminder 21:30; Doze delays the worker to 22:20 | Skipped — `QUIET_HOURS`; next tomorrow 21:30 |
| R5 | Reminder 23:00 (chosen inside the window); runs 23:04 | Posted |
| R6 | Reminder 23:00; worker delayed to 00:30 the next day | `EARLY` on the new dateKey → re-enqueued for 23:00 tonight; the missed reminder is not sent late |
| R7 | Reminder 19:00; battery saver delays the worker to 21:45 | Skipped — `STALE` |
| R8 | 3 words due, mention on | Body mentions "3 words are due for review" |
| R9 | Permission revoked in system settings | Skipped — `BLOCKED`; SCR-SET-03 shows the off state on the next visit |

### 5.4 Notification content and deep link (SRS-FR-REM-003, -004; SEC-01 §6.7)

| Element | Specification (copy is RECOMMENDATION) |
|---|---|
| Channel | ID `practice_reminders`; name "Practice reminders"; description "One reminder a day at the time you choose."; importance DEFAULT; created idempotently at app start and when reminders are first enabled (API 26+) |
| Title | "Time to practise" |
| Body — no due words, or mention off | "Today's session is ready when you are." |
| Body — n due words | "Today's session is ready. {n} words are due for review." (plural resources; "1 word is due for review") |
| Never included | Transcripts, feedback, metrics, the words themselves, streak numbers, loss framing ("Don't break your streak!"), "AI", creator names or branding, update notices (SEC-01 §6.7 / THR-22; DES-05 §12.1, §12.3; PP-7; SRS-BR-028; BR-012) |
| Category / visibility | `CATEGORY_REMINDER`; `VISIBILITY_PRIVATE` with a public version carrying the same generic text |
| Tap action | Explicit intent to `MainActivity` with the type-safe Today route (ARC-01 §5); `PendingIntent.FLAG_IMMUTABLE`; auto-cancel; no extras that change security state (NFR-SEC-004) |
| Action buttons | None in R1 (Q3) |
| After practice | When a valid attempt is saved, a reminder already shown today is removed |

Tap behaviour: **cold start** follows the normal start order — deletion resume SCR-SYS-01 if pending (FS-01) → onboarding if incomplete (FS-02) → Today. **App running** → Today tab root; if the session player is open, the app is only brought to the front, so a recording is never stopped by navigation (SRS-FR-SESS-014). Opening from a reminder never counts as practice (SRS-FR-PROG-005).

### 5.5 Rescheduling (SRS-FR-REM-006)

| Trigger | Mechanism | Action |
|---|---|---|
| App start | `SpeakCraftApplication` calls an idempotent `ensureScheduled()` | If enabled and there is no pending work, or pending `scheduledFor` ≠ computed next trigger → replace |
| Preference change (enable, time) | `PreferencesRepository` Flow | Replace; disable → cancel work and remove today's shown reminder |
| Reboot | WorkManager keeps work and reschedules it after boot (ADR-005); app start also checks | No app action needed |
| Time-zone change | Non-exported receiver for `ACTION_TIMEZONE_CHANGED` (delivery to confirm in ARC-02, Issue 9) + worker step 2 + app start | Recompute in the new zone; replace |
| Manual time or date change | Same receiver for `ACTION_TIME_CHANGED` (Issue 4) | Recompute; replace |
| DST change | None needed — the trigger is computed with zone rules (W3, W4) | — |
| App update | WorkManager keeps work; app start checks | — |
| Force-stop by the user | Android cancels pending work until the next launch; WorkManager reschedules on launch | Help text on SCR-SET-03: "If you force-stop SpeakCraft, reminders restart when you next open it." |
| Permission granted later in system settings | Re-checked on SCR-SET-03 `ON_RESUME` and app start | `ensureScheduled()` |
| Sign-out — Keep | Reminders continue (SEC-01 §4.4) | — |
| Sign-out — Remove; account deletion; delete everything on this device | FS-01 flows cancel WorkManager jobs | Cancel work; remove a shown reminder; the new local profile starts with reminders off |
| Restore from backup on a new device | WorkManager database is not backed up (SEC-01 §3.2 #17) | App start re-creates the schedule; permission state re-checked |

### 5.6 Notification permission states (PRD-FR-083; SRS-FR-REM-005; DES-05 §11.19)

| State | Detection | SCR-SET-03 shows | Action |
|---|---|---|---|
| Allowed (or API < 33 with notifications enabled) | Permission granted and `areNotificationsEnabled()` | Toggle and time, no warning | — |
| Android 13+, never asked | `notificationPromptCount = 0` | — | Turning the toggle on opens SCR-PERM-01, then the system dialog |
| Denied, can ask again | Denied and `shouldShowRequestPermissionRationale` = true | "Notifications are off for SpeakCraft. Reminders won't appear." | **Allow notifications** — user-initiated only; shows the system dialog once more (Issue 3) |
| Permanently denied | Denied, rationale = false and `notificationPromptCount` ≥ 1 | Same text | **Open settings** → `ACTION_APP_NOTIFICATION_SETTINGS` |
| App notifications blocked (any API) | `areNotificationsEnabled()` = false | Same text | Open settings |
| Channel turned off (API 26+) | Channel importance = NONE | "Practice reminders are turned off in system settings." | Open settings → `ACTION_CHANNEL_NOTIFICATION_SETTINGS` |

The app never requests the permission at launch, on a schedule or from the worker (SRS-FR-REM-005; ADR-005). State is re-checked on `ON_RESUME`. During onboarding the system dialog is shown directly from SCR-ONB-05, which serves as the explanation (FS-02). **SCR-PERM-01 notification variant** (copy RECOMMENDATION): "Get a daily reminder?" — "We'll send one reminder a day around 19:00, only if you haven't practised yet. You can change this any time." — **Allow notifications** / **Not now** (Not now: toggle returns to off, no system dialog).

### 5.7 R2 — Inbox (SCR-INBOX-01; SRS-FR-REM-007; ENT-16) — outline

| Item | Behaviour |
|---|---|
| Entry | Bell in Today's top bar with an unread badge (DES-02 §2.2); hidden in R1 |
| Message types | Closed list: `MILESTONE` (from FS-08 §5.10) and `WEEKLY_SUMMARY`. No software-update, promotional or system messages (PRD-FR-084, BR-012); a unit test fails the build if another type is added |
| Weekly summary | Created on the first app open on or after the first day of a new locale week, from the previous week's facts only: "Last week: 4 practice days, 23 min of practice, 2 new words learned." No message for a week without valid attempts (PP-7) — RECOMMENDATION |
| Deduplication | Unique key per message (`MILESTONE:{runStartDateKey}:{n}`, `WEEKLY:{weekStartDateKey}`); a deleted message keeps a tombstone so it is never re-created (fixes FND-DOM-14) |
| Actions | Open (marks read); Mark all read; Delete (swipe plus a visible Delete button, A11Y-20); the badge follows the unread count |
| Delivery | In-app only; no system notification (Issue 8) |
| Retention / sync | RECOMMENDATION: keep 90 days † or 100 messages †; local only, regenerable from facts (Issue 8) |

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | Reminder chosen during onboarding (SCR-ONB-05) | FS-02 flow; scheduling per §5.2 |
| A2 | Permission denied after choosing a reminder | Preference kept, off state shown, no automatic re-prompt (FS-02 A5) |
| A3 | User practises after the reminder was shown | The shown reminder is removed when the valid attempt is saved (§5.4) |
| A4 | Signed-in user with two devices | Each device schedules from its own state and checks only its local attempts; a reminder can appear on device B after practice on device A until the next sync (Issues 6, 10) |
| A5 | Tap while the session player is open | App brought to the front; no navigation (§5.4) |
| A6 | Channel disabled in system settings | Worker skips `BLOCKED`; SCR-SET-03 shows the channel-off state |
| A7 | Several days without practice | Reminder text is unchanged — no recovery or guilt wording (PP-7) |
| A8 | R2 milestone reached | Inbox message created; badge updates |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Saving the reminder preference fails | "Couldn't save your settings. Try again." Toggle reverts; retry once | SRS-ERR-005 |
| E2 | Enqueuing work fails | "Couldn't set the reminder. Try again."; retried at next app start; PII-free log | — |
| E3 | Worker cannot read Room (suppression unknown) | Skip posting (fail closed, so a practised user is never reminded); log `DB_UNAVAILABLE` | SRS-ERR-005 |
| E4 | Posting throws (permission revoked between check and post) | Caught; skip; log | — |
| E5 | Inbox fails to load (R2) | "Couldn't load your messages." + Try again | — |

## 8. Business rules

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-019 | At most 1 reminder per day by default; none if a valid attempt exists today; none between 22:00 and 07:00 † unless the chosen time is in that window |
| SRS-BR-010 | "Valid attempt today" means a valid speaking attempt from any source (Today, Practice, word stages 2–4, reviews) |
| SRS-BR-008 | Inconclusive attempts do not suppress the reminder |
| SRS-BR-016 / -017 | Due words for the mention are counted on the local dateKey (FS-07) |
| RECOMMENDATION | Early and stale guards (5 min †, 2 h †), skip codes, chained one-time work, removal after practice, due-mention toggle, inbox types, dedup, retention |

## 9. UI states

**SCR-SET-03 Reminders**

| State | What the user sees | Actions |
|---|---|---|
| Default — off | Toggle off; explanation line | Turn on |
| Default — on | Toggle on; "Around 19:00"; mention toggle; force-stop help text | Change time; turn off; mention on/off |
| Loading | N/A — local preference read is immediate | — |
| Empty | N/A — settings always have values | — |
| Error | E1 / E2 inline | Try again |
| Offline | Identical — no network involved | — |
| Permission — denied | "Notifications are off for SpeakCraft. Reminders won't appear." | Allow notifications |
| Permission — permanently denied / blocked | Same text, or channel-off text | Open settings |

**SCR-PERM-01 Permission education — notification variant**

| State | What the user sees | Actions |
|---|---|---|
| Default | Title, one-reminder promise, "change any time" (§5.6) | Allow notifications; Not now |
| Permission | System dialog over the screen | Allow / Don't allow |
| Loading / Empty / Error / Offline | N/A — static screen, no data or network | — |

**Reminder notification (system surface)** — Posted (generic text, §5.4); Replaced (same ID, never two); Removed (after practice or when reminders are turned off). Locked screen: generic public version.

**SCR-TODAY-01 bell (R2)** — Hidden in R1; no badge when nothing is unread; badge with count (99+ cap) and content description "{n} unread messages".

**SCR-INBOX-01 Inbox (R2)**

| State | What the user sees | Actions |
|---|---|---|
| Default | Newest first; unread rows marked with the text "Unread" and an icon | Open; Mark all read; Delete |
| Loading | Skeleton rows | — |
| Empty | "Messages about your milestones and weekly progress will appear here." | Back to Today |
| Error | E5 | Try again |
| Offline | Identical — local data | — |
| Permission | N/A — in-app list, no permission | — |

## 10. Data requirements

| Entity / store | Fields needed by FS-09 (minimum; ARC-03 authoritative) |
|---|---|
| Reminder preferences | `reminderEnabled`, `reminderTime` ("HH:mm", default 19:00 †), `reviewReminders` (mention due words; SEC-01 prefs key), `notificationPromptCount` (FS-02 `notificationPromptShown`). RECOMMENDATION: `reminderTime` in ENT-02 LearningPreferences (synced per ARC-03 §8); `reminderEnabled` and the prompt count device-only in the `settings` DataStore (Issue 6) |
| `device_state` DataStore | `scheduledFor`, `lastReminderDateKey`, `lastSkipReason` — not backed up, so a restore cannot suppress or duplicate a reminder |
| WorkManager database | Unique work `daily_reminder` (excluded from backup, SEC-01 §3.2 #17) |
| ENT-03 Attempt | `dateKey`, `result` (suppression) |
| ENT-07 WordProgress | `status`, `nextReviewDate` (due count) |
| ENT-18 DeletionState | `step` (no reminders while deletion is in progress) |
| ENT-16 InboxMessage (R2) | `messageId`, `dedupKey` (unique), `type`, `createdAt`, `dateKey`, `params` (numbers only), `readAt?`, `deletedAt?` (tombstone) |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `ReminderWorker` + scheduling helper (`feature/reminders`; names per ARC-02) | Compute, enqueue, replace, cancel; run-time checks (§5.3) |
| WorkManager (ADR-005) | Unique one-time work, persistence across reboot |
| `PreferencesRepository` | Reminder preferences Flow |
| `AttemptRepository` | `hasValidAttempt(dateKey)` |
| `WordRepository` | `dueCount(dateKey)` |
| `ProfileRepository` / `AccountRepository` | Profile presence, deletion state, cancellation on sign-out Remove and deletion |
| `DateKeyProvider` / `Clock` | Today's dateKey and local time in the current zone |
| `ProgressCalculator` (R2) | Milestone and weekly facts for inbox messages |
| `AppLogger` | PII-free skip reasons (ADR-007) |
| Platform | `NotificationManagerCompat`, `NotificationChannel`, `ActivityResultContracts.RequestPermission` (POST_NOTIFICATIONS), Settings intents, `BroadcastReceiver` (`ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED`) |

## 12. Offline behaviour

Reminders are entirely local: scheduling, checks and posting need no network (NFR-REL-006). The inbox is built from local facts. The only network-related limitation is multi-device suppression, which depends on when the other device's attempts are pulled (A4).

## 13. Privacy requirements

- Notification text is generic: no transcripts, feedback, metrics or words (SEC-01 §6.7, THR-22); the lock-screen version is identical.
- No data leaves the device for reminders — no FCM or server scheduling (ADR-005).
- Reminder time syncs only if listed in ARC-03 §8; skip-reason logs contain no identifiers (NFR-SEC-002); no analytics (SRS-PRIV-004).
- The permission is requested only after an explicit user action (SRS-FR-REM-005).

## 14. Accessibility notes

The reminder setting reads as one labelled control with state ("Daily reminder, on, around 7 PM") (A11Y-06, A11Y-07); the time picker offers keyboard input and uses the locale's 12/24-hour format (A11Y-37). The permission status is text, never colour alone (A11Y-23); errors are announced and say how to fix them (A11Y-32). Inbox rows are merged nodes with "Unread" in text (A11Y-07); swipe-to-delete has a visible button alternative (A11Y-20); the badge has a content description with plurals (A11Y-02, A11Y-36). Targets ≥ 48 dp (A11Y-19); layouts reflow at 200 % font scale (A11Y-13).

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS09-01 | A new profile that chose "Not now" in onboarding | Any time passes | No reminder work is scheduled and no notification is posted |
| AC-FS09-02 | Notifications allowed, 10:00 Europe/London | The user enables a 19:00 reminder | Unique work `daily_reminder` is enqueued with a 9-hour delay and "Reminder set for around 19:00." is announced |
| AC-FS09-03 | Reminder 19:00, no attempt today | The worker runs at 19:00 | One notification "Time to practise" is posted on channel "Practice reminders"; tapping it opens Today |
| AC-FS09-04 | A valid attempt at 08:15 today | The worker runs at 19:00 | No notification; the next run is tomorrow 19:00 |
| AC-FS09-05 | Only an Inconclusive attempt today | The worker runs | The notification is posted |
| AC-FS09-06 | 3 words due and the mention toggle on | The reminder is posted | The body says "3 words are due for review" and names no word |
| AC-FS09-07 | A reminder already posted today | The worker runs again the same day (e.g. after a time change) | No second notification is posted |
| AC-FS09-08 | Reminder 21:30, worker delayed to 22:20; another user with reminder 23:00, worker at 23:04 | Each worker runs | The first is skipped (quiet hours); the second is posted |
| AC-FS09-09 | Android 13+, permission never requested | The user turns reminders on in SCR-SET-03 | SCR-PERM-01 appears, then the system dialog; choosing Not now turns the toggle off with no system dialog |
| AC-FS09-10 | Permission denied once | The user reopens the app and SCR-SET-03 | No dialog appears on its own; the row shows "Notifications are off for SpeakCraft" with Allow notifications |
| AC-FS09-11 | Permission permanently denied | The user taps Open settings, allows notifications and returns | App notification settings open; on return the warning disappears and the reminder is scheduled without further action |
| AC-FS09-12 | Reminders on | The device reboots and the app is not opened | The next reminder is still posted near the chosen time |
| AC-FS09-13 | Reminder 19:00 set in London | The device time zone changes to America/New_York before the trigger | The reminder is posted near 19:00 New York time, not 14:00 |
| AC-FS09-14 | Reminder 19:00 on Sat 24 Oct 2026 (Europe/London) | DST ends overnight | Sunday's reminder is posted near 19:00 GMT |
| AC-FS09-15 | Reminders on | The user signs out choosing Remove, deletes the account or deletes everything on the device | Reminder work is cancelled and no further reminder is posted |
| AC-FS09-16 | Any posted reminder | Its content and intent are inspected | No transcript, feedback, metric, word, streak number or "AI" appears; the PendingIntent is immutable and explicit |
| AC-FS09-17 | An R1 build | All notification-posting code paths are inspected and exercised | Only the "Practice reminders" channel is ever used |
| AC-FS09-18 | A reminder is shown | The user completes a valid attempt | The shown reminder is removed |
| AC-FS09-19 (R2) | A 7-day milestone and a completed week with practice | The user opens the inbox | Two messages appear with an unread badge; Mark all read clears the badge; a deleted message is not re-created; no update message type exists |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-REM-001 | Next-trigger computation | unit | Fixed `Clock`; zones Europe/London, America/New_York | Compute W1–W4, W6 | Triggers and delays as §5.2 | SRS-FR-REM-001 |
| TC-REM-002 | Enqueue, replace, cancel | unit | Fake work-scheduler port | Enable; change time; disable | One unique work replaced, then cancelled | SRS-FR-REM-001, -006 |
| TC-REM-003 | Worker decision table | unit | Fakes for prefs, attempts, words, permission, clock | Run R1–R9 | Outcomes and skip codes as §5.3 | SRS-FR-REM-002, SRS-BR-019 |
| TC-REM-004 | One per day | unit | `lastReminderDateKey` = today | Run worker | `ALREADY_SENT`; no post | SRS-BR-019 |
| TC-REM-005 | Inconclusive does not suppress | unit | Only Inconclusive attempt today | Run worker | Posted | SRS-BR-008, -019 |
| TC-REM-006 | Quiet hours | unit | Times 21:59, 22:00, 06:59, 07:00; chosen 21:30 and 23:00 | Run worker | Boundaries per SRS-BR-019 incl. the chosen-time exception | SRS-BR-019 |
| TC-REM-007 | Early and stale guards | unit | W5, R6, R7 clocks | Run worker | Re-enqueue / skip as §5.3 | SRS-FR-REM-001 (RECOMMENDATION) |
| TC-REM-008 | Content builder | unit | n = 0, 1, 3; mention off | Build notification | Correct plurals; forbidden content absent; category and visibility set | SRS-FR-REM-003, SEC-01 §6.7 |
| TC-REM-009 | Post and open Today | instrumented | API 33 emulator, permission granted, WorkManager test driver | Enable; meet the initial delay; tap the notification | Posted on "Practice reminders"; Today shown | SRS-FR-REM-003, -004 |
| TC-REM-010 | Tap during a recording | instrumented | Session player recording | Tap the reminder | App to front; recording continues; no navigation | SRS-FR-SESS-014 |
| TC-REM-011 | Cold start with deletion pending | instrumented | ENT-18 step = AUTH_USER | Tap the reminder | SCR-SYS-01 shown first (FS-01) | SRS-FR-ACC-008 |
| TC-REM-012 | Never-asked permission flow | instrumented | API 33, fresh install past onboarding with "Not now" | Turn reminders on | SCR-PERM-01, then system dialog; Allow → scheduled | SRS-FR-REM-005, PRD-FR-083 |
| TC-REM-013 | Not now on education | Robolectric UI | Fake permission state never-asked | Tap Not now | Toggle off; no permission request | SRS-FR-REM-005 |
| TC-REM-014 | Denied and permanently denied states | Robolectric UI | Fake permission states per §5.6 | Render SCR-SET-03 | Text and action per state; correct settings intent | SRS-FR-REM-005 |
| TC-REM-015 | No automatic re-prompt | instrumented | API 33; deny once | Relaunch; open screens; run worker | No permission dialog without a user tap | SRS-FR-REM-005 |
| TC-REM-016 | Granted later in system settings | manual | API 34 device, permanently denied | Open settings; allow; return | Warning cleared; reminder posted at next trigger | PRD-FR-083 |
| TC-REM-017 | Reboot persistence | manual | Reference devices RD-LOW, RD-HIGH | Enable for +15 min; reboot; do not open app | Reminder posted near the time | SRS-FR-REM-006 |
| TC-REM-018 | Time-zone change | manual | RD-MID, reminder 19:00 London | Change zone to New York in system settings at 08:00 local | Posted near 19:00 New York | SRS-FR-REM-006 |
| TC-REM-019 | Manual clock change | manual | RD-MID | Move the clock forward 3 h past the trigger, then back | No late or duplicate reminder; next trigger correct | SRS-FR-REM-006 (Issue 4) |
| TC-REM-020 | Channel or app notifications blocked | instrumented | API 33, permission granted | Block channel in settings; run worker; open SCR-SET-03 | `BLOCKED`; channel-off text and action | PRD-FR-083 |
| TC-REM-021 | Cancellation on sign-out Remove and deletion | instrumented | Reminders on, signed in | Sign out → Remove; separately delete account | No pending `daily_reminder` work | SRS-FR-ACC-006, -007 |
| TC-REM-022 | Single channel in R1 | unit | Notification builder registry | Enumerate channels and posting paths | Only `practice_reminders` | SRS-FR-REM-004 |
| TC-REM-023 | Delivery under battery saver | manual | RD-LOW, battery saver on, 3 days | Record delivery times | Each reminder within the window or skipped `STALE`; never late | SRS-FR-REM-001, ADR-005 |
| TC-REM-024 | Removal after practice | instrumented | Reminder shown | Save a valid attempt | Notification removed | §5.4 (RECOMMENDATION) |
| TC-REM-025 | Reminders screen accessibility | Robolectric UI | Font scale 2.0, dark theme | Render; read semantics | No clipping; labelled toggle with state; time in locale format | NFR-A11Y-001, -002 |
| TC-REM-026 (R2) | Inbox generation and dedup | unit | Milestone and week facts; delete one message | Regenerate | One message per key; tombstoned message not re-created; no message for an empty week | SRS-FR-REM-007 |
| TC-REM-027 (R2) | Inbox UI and badge | Robolectric UI | Messages read/unread | Open, Mark all read, Delete via button | States per §9; badge count updates | SRS-FR-REM-007 |
| TC-REM-028 (R2) | No update message type | unit | Message type enum | Assert closed list | Only MILESTONE and WEEKLY_SUMMARY | PRD-FR-084, BR-012 |

## 17. Traceability

| PRD-FR | SRS-FR / SRS-BR | AC | TC |
|---|---|---|---|
| PRD-FR-080 | SRS-FR-REM-001, -004, -006; SRS-FR-ONB-005 | AC-FS09-01, -02, -03, -12…-15, -17 | TC-REM-001, -002, -009, -017…-019, -021…-023 |
| PRD-FR-081 | SRS-FR-REM-003; SRS-BR-019 (max 1 per day) | AC-FS09-06, -07 | TC-REM-004, -008 |
| PRD-FR-082 | SRS-FR-REM-002; SRS-BR-008, -019 | AC-FS09-04, -05, -08, -18 | TC-REM-003, -005, -006, -007, -024 |
| PRD-FR-083 | SRS-FR-REM-005 | AC-FS09-09, -10, -11 | TC-REM-012…-016, -020, -025 |
| PRD-FR-084 (R2) | SRS-FR-REM-007 | AC-FS09-19 | TC-REM-026, -027, -028 |
| PRD-NFR-002 / -008 | SEC-01 §6.7; NFR-SEC-004 | AC-FS09-16 | TC-REM-008, -010, -011 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Confirm the proposed values: default time 19:00 †, quiet hours 22:00–07:00 †, early guard 5 min † and stale limit 2 h † | Product Owner |
| Q2 | Should the due-review mention have its own toggle (SEC-01 `reviewReminders`) or always be on? | Product Owner |
| Q3 | Should the notification offer actions (e.g. "Start now")? Recommended: none in R1 | Product Owner / UX |
| Q4 | Should reminders pause after several ignored days ("respectful frequency caps", RD-16 CAP-09)? | Product Owner |
| Q5 | Should the reminder preference follow the user to a second signed-in device (two devices may both remind)? | Product Owner / Database Architect |
| Q6 | R2: should inbox messages also raise a system notification? Recommended: no | Product Owner |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | SRS-FR-PLAT-002 says the manifest declares only INTERNET, RECORD_AUDIO and POST_NOTIFICATIONS, but WorkManager merges RECEIVE_BOOT_COMPLETED, WAKE_LOCK, ACCESS_NETWORK_STATE and FOREGROUND_SERVICE (SEC-REQ-31 already allows "library-merged WorkManager permissions as reviewed") | SRS-FR-PLAT-002 vs SEC-01 SEC-REQ-31 | Align SRS-FR-PLAT-002 with SEC-REQ-31 and list the reviewed merged permissions |
| 2 | PRD-FR-081 says "at most one reminder per day **by default**", RD-16 and ADR-005 mention "review reminders" and "respectful frequency caps", and SEC-01 syncs a `reviewReminders` preference — but no SRS requirement defines a separate review reminder, a configurable cap or the preference | PRD-FR-081, RD-16 CAP-09, ADR-005, SEC-01 §5 | Define in SRS: one reminder per day; `reviewReminders` = mention due words (§5.1); decide Q4 |
| 3 | "No repeated prompts" does not say whether a user-initiated retry is allowed while Android can still show the dialog | SRS-FR-REM-005 | Allow one user-initiated retry; never prompt automatically (§5.6) |
| 4 | Rescheduling lists reboot, time-zone and preference changes only — not manual clock changes, app updates or force-stop; DST is not mentioned | SRS-FR-REM-006 | Add the triggers in §5.5 |
| 5 | "Near the chosen local time" has no bound, so a delayed run can post hours late or after the dateKey rolls over; the interaction with the quiet-hours rule is undefined | SRS-FR-REM-001, SRS-BR-019 | Adopt the early/stale guards (§5.3 steps 2–3) |
| 6 | Where reminder preferences live and whether they sync is inconsistent: SEC-01 puts `reminderTime` and `timeZone` in the synced profile and synced preferences in the `settings` DataStore; the resume canon defines ENT-02 LearningPreferences in Room; FS-02 lists `reminderEnabled` / `reminderTime` in DataStore. Whether `reminderEnabled` syncs (double reminders on two devices) is undefined | SEC-01 §3.2 #10, #12; PLAN_AND_CANON R-1; FS-02 §10 | ARC-03 §7–§8 to decide; recommendation in §10 |
| 7 | ADR-005 describes "one **periodic** ReminderWorker (re-enqueued…)"; 24-hour periodic work drifts across DST and time-zone changes | ADR-005 | Reword to chained one-time work (§5.2 step 4) |
| 8 | Inbox: no sync scope for ENT-16, no retention, and SRS-FR-REM-004 forbids other notification categories only in R1, so whether R2 inbox messages raise system notifications is undefined | SRS-FR-REM-004, -007; ARC-03 §8 | State local-only, retention, and no system notification (Q6) |
| 9 | NFR-SEC-004 allows only `MainActivity` to be exported; time-zone and clock-change receivers must therefore be non-exported, and delivery of these system broadcasts to non-exported receivers must be confirmed | NFR-SEC-004, SRS-FR-REM-006 | ARC-02 to confirm; worker guard and app-start check are the fallback |
| 10 | Suppression uses only local attempts; a signed-in user who practised on another device can still be reminded, because cloud changes are pulled only on sign-in and app start | SRS-FR-REM-002, SRS-FR-DATA-004 | Accept and document, or allow the worker a bounded pull |
| 11 | The denied-state copy differs: FS-02 A5 "Notifications are blocked" vs DES-05 §11.19 "Notifications are off for SpeakCraft" | FS-02 §6 A5, DES-05 §11.19 | Use the DES-05 wording in both |
