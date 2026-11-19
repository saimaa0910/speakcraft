# SpeakCraft Database Changes — Activity Engine Cloud Firestore Architecture

All existing SpeakCraft user data, profile documents, speaking sessions, vocabulary, daily activities, YouTube intelligence, and RBAC collections remain **100% preserved and backward compatible**. The Universal Activity Engine adds **4 strictly user-scoped Firestore subcollections** under `/users/{userId}` on the named Firestore database `ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11`.

## 1. New User-Scoped Collections
| Collection Path | Document ID | Kotlin DTO (`ActivityModels.kt`) | Purpose |
|---|---|---|---|
| `/users/{userId}/activityProgress/{activityId}` | `activityId` | `UserActivityProgress` | Best/latest objective score, separate confidence rating, attempts count, completion status, cumulative practice seconds |
| `/users/{userId}/activityAttempts/{attemptId}` | Auto-ID | `ActivityAttemptRecord` | Immutable chronological audit log of every attempt (evaluator, objective score, separate confidence rating, WPM, fillers, 6-section feedback summary, dateKey) |
| `/users/{userId}/savedActivities/{activityId}` | `activityId` | `SavedActivityRecord` | Bookmarked favorite activities for quick access |
| `/users/{userId}/dailyPracticePlans/{dateKey}` | `YYYY-MM-DD` | `DailyPracticePlanRecord` | User's daily target duration (10/20/30/45/60m), ordered activity IDs, pinned customizations, and completed IDs |

## 2. Serialization & Safety Conventions
- Every Firestore DTO uses `@Keep @IgnoreExtraProperties`, `@DocumentId val id: String = ""`, `@ServerTimestamp val createdAt: Timestamp? = null`, and zero `is`-prefixed Boolean fields (`completed`, not `isCompleted`).
- `ActivityFirestoreRepository` uses `snapshots()` with `ServerTimestampBehavior.ESTIMATE`, unwraps `FirebaseFirestoreException` via `generateSequence(err) { it.cause }`, preserves `CancellationException`, and wraps all mutations in crash-free `Result<T>`.

## 3. Zero-Trust Security Rules (`firestore.rules`) & Blueprint (`firebase-blueprint.json`)
All 4 new subcollections enforce strict owner-only access (`isOwner(userId)`), exact field allowlists (`hasOnly(...)`), immutable `userId` and `createdAt` on updates, and numeric range bounds:
- `isValidActivityProgress`: validates `bestObjectiveScore` (`0..100`), `latestObjectiveScore` (`0..100`), `latestConfidenceSelfRating` (`0..100`), `attemptsCount` (`0..100000`), `totalPracticeSeconds` (`0..10000000`).
- `isValidActivityAttempt`: validates `objectiveScore` (`0..100`), `confidenceSelfRating` (`0..100`), `durationSeconds` (`0..14400`), `wordsPerMinute` (`0..400`), `fillerCount` (`0..2000`), immutable `activityId` / `createdAt`.
- `isValidSavedActivity`: validates `activityId`, `moduleId`, `title`, `skillCategory`, `difficulty`, `durationMinutes` (`1..120`).
- `isValidDailyPracticePlan`: validates `dateKey`, `targetMinutes` (`10..60`), list bounds (`<= 25` items) for `activityIds`, `pinnedActivityIds`, `completedActivityIds`.

## 4. Verification & Deployment
- Synchronized 1:1 across `ActivityModels.kt`, `ActivityFirestoreRepository.kt`, `firebase-blueprint.json`, `firestore.rules`, and `firestore.test.js`.
- Verified against the live local Firestore Emulator (`FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js` — **4/4 suites passed, 0 failures**).
- Deployed live to Firebase via `deploy_firebase`.
