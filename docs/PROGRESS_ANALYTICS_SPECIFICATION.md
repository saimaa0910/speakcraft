# Progress & Analytics Engine Specification

`ActivityProgressAnalyticsEngine` (`app/src/main/java/com/example/data/activity/ActivityProgressAnalyticsEngine.kt`) transforms real Cloud Firestore `UserActivityProgress` and `ActivityAttemptRecord` streams into transparent, evidence-backed progress telemetry.

## 1. Strict Metric Integrity Rules
1. **Zero Fabricated Metrics**: Every counter, average, trend delta, and milestone derives exclusively from recorded user attempts and persisted progress records.
2. **Strict Separation of Objective Score vs. Self-Confidence**:
   - `averageObjectiveScore` averages `ActivityAttemptRecord.objectiveScore` (`0..100`) produced strictly by rubric + AI/local evaluation.
   - `averageConfidenceSelfRating` averages `ActivityAttemptRecord.confidenceSelfRating` (`1..100`, filtered to `> 0`) rated independently by the learner on `SeparateConfidenceSliderCard`.
   - Self-assessed confidence **never alters** `overallObjectiveScore`, passing criteria, or objective skill trend deltas.

## 2. Computed Analytics Dimensions (`ActivityAnalyticsSnapshot`)
| Metric | Computation |
|---|---|
| `totalActivitiesCompleted` | Distinct `activityId`s with `completed == true` (out of 150) |
| `totalAttempts` | Total count of persisted `ActivityAttemptRecord` entries |
| `totalPracticeMinutes` | `max(sum(attempts.durationSeconds), sum(progress.totalPracticeSeconds)) / 60` |
| `activeDaysLast7` | Count of distinct ISO dates (`YYYY-MM-DD`) in the trailing 7-day window |
| `currentConsistencyStreak` | Consecutive calendar days with at least one attempt ending today or yesterday |
| `weeklyAttemptsCount` | Attempts recorded within the trailing 7-day window |
| `averageObjectiveScore` | Mean objective score across all historical attempts (`0..100`) |
| `averageConfidenceSelfRating` | Nullable mean self-assessed confidence (`1..100`) — `null` if never rated |
| `moduleStats` | Per-module completion (`completedCount / totalCount`) and mean objective score across all 10 modules |
| `skillTrends` | Historical earliest-vs-latest comparison per `ActivitySkillCategory` (`earliestScore`, `latestScore`, `delta`, `attemptCount`) |
| `improvementAreas` | Bottom 3 `ActivitySkillCategory` entries ordered by `latestScore` |
| `recordingsCount` | Count of attempts containing a recorded audio/video media path |
| `achievedMilestones` | Dynamic badges (`First Activity Completed`, `10/25/50/100/150` milestones, `3-Day`/`7-Day` streaks, `5+ Audio/Video Self-Audits`, completed module counts) |

## 3. UI Presentation
Displayed in the `Analytics` tab of `ActivityDiscoveryScreen` with interactive 10-module progress cards that filter the catalogue to any tapped module immediately.
