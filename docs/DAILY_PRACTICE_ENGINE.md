# Daily Practice Engine Specification

## 1. Purpose
`DailyPracticeEngine` (`app/src/main/java/com/example/data/activity/DailyPracticeEngine.kt`) dynamically assembles balanced, non-repetitive daily communication workouts across **5 selectable time budgets**: **10, 20, 30, 45, and 60 minutes**.

## 2. Multi-Pillar Routine Composition
| Duration | Included Practice Pillars | Typical Activities |
|---|---|---|
| **10 min** | Pronunciation, Speaking | 2 high-impact drills |
| **20 min** | Pronunciation, Voice, Speaking, Listening | 3–4 balanced drills |
| **30 min** | Pronunciation, Voice, Vocabulary, Speaking, Conversation | 4–5 comprehensive drills |
| **45 min** | Pronunciation, Voice, Vocabulary, Speaking, Listening, Conversation, Reflection | 6–7 full-spectrum drills |
| **60 min** | All 7 pillars + extended Speaking & Conversation mastery blocks | 8–9 deep-practice blocks |

## 3. Personalization & Anti-Repetition Scoring Algorithm
For every pillar slot, `DailyPracticeEngine` scores all eligible activities from the 150-activity catalogue:
- **Anti-Repetition Penalty**: `-55` points if the activity appears in the user's last 15 attempts (`recentAttempts`); `-20` points if already completed—guaranteeing fresh variety day after day.
- **Proficiency Alignment**: `+25` points for exact difficulty match with `UserProfile.communicationLevel` (`Beginner` / `Intermediate` / `Advanced`), `+10` for ±1 tier, `-12` for 2-tier jump.
- **Weakest-Skill Priority**: `+30` points for activities targeting the user's lowest historical objective score (or lowest profile baseline score when new).
- **Goal Alignment**: `+22` points when the activity matches keywords in `UserProfile.primaryGoal`.
- **Deterministic Daily Rotation**: Uses `(activityId.hashCode() xor dateKey.hashCode()) % 13` so each calendar day rotates predictably without flaky randomness.

## 4. User Customization & Cloud Persistence
- **Duration Switching**: Users switch instantaneously between `10m`, `20m`, `30m`, `45m`, and `60m` inside `ActivityDiscoveryScreen` (`Daily Plan` tab).
- **Swap Any Block**: `DailyPracticeEngine.swapBlockInPlan(currentPlan, activityIdToReplace, recentAttempts)` replaces any block with the highest-ranked fresh activity from the same pillar, marks it `pinned = true`, and recalculates `totalPlannedMinutes`.
- **Cloud Sync**: Custom plans and completion state persist to `/users/{userId}/dailyPracticePlans/{dateKey}` via `ActivityFirestoreRepository.saveDailyPracticePlan(...)`.
