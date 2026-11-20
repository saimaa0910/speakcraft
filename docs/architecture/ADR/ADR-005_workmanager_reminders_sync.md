# ADR-005 — WorkManager for reminders and background sync

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** (adds a dependency) |
| Date | 2026-11-20 |
| Owner (role) | Android Technical Lead |
| Related | FS-09 Reminders, FS-11 Sync, ADR-014, RD-03 FND-DOM-14, FND-OPS-05 |

## Context
No background processing exists (no WorkManager, AlarmManager, services or system notifications). "Reminders" are in-app list items created only while the app is open (FND-DOM-14). The redesign requires (a) a daily practice reminder at a user-chosen time, (b) vocabulary review reminders, and (c) opportunistic sync of local learning data when connectivity returns.

## Options
| Option | Pros | Cons |
|---|---|---|
| **A. WorkManager (`androidx.work`)** | Survives reboot and process death; Doze-aware; network constraints for sync; testable with `work-testing` | New dependency; daily timing is inexact (acceptable for reminders, typically within the flex window) |
| B. AlarmManager + BroadcastReceiver (platform) | No new dependency | Must handle reboot (`RECEIVE_BOOT_COMPLETED`), Doze, exact-alarm restrictions (`SCHEDULE_EXACT_ALARM` is restricted); no network-constraint scheduling for sync — sync would need separate logic |
| C. FCM push | Server-driven | Requires a backend; sends schedule data off-device; overkill |

## Decision (proposed)
**Option A.** One periodic `ReminderWorker` (re-enqueued for the next local reminder time) and one `SyncWorker` (unique, network-constrained, triggered after local writes and on app start). Notifications use `NotificationCompat` with channels; `POST_NOTIFICATIONS` is requested on Android 13+ only when the user turns reminders on.

## Consequences
Add `androidx.work:work-runtime-ktx` (+ `work-testing` for tests). Reminder delivery is "around" the chosen time, which the UI states honestly.

## Risks
OEM battery optimisations can delay work; mitigated by honest copy and no reliance on exactness.

## Approval status
Pending — Product Owner (decision D-12). If rejected, Option B for reminders and sync-on-foreground only.
