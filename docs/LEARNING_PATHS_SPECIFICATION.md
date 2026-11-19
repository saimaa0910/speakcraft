# SpeakCraft Personalized Learning Paths Specification (10 Guided Paths)

`LearningPathsEngine` (`app/src/main/java/com/example/data/activity/LearningPathsEngine.kt`) organizes the 150-activity catalogue into **10 goal-driven learning paths** with ordered curricula, optional prerequisites, unlockable milestones, completion criteria, and **non-linear jump access** so learners can follow sequentially or jump directly into any activity.

## 1. All 10 Learning Paths
| # | Path ID | Title | Difficulty | Prerequisite Path | Ordered Activities | Milestones |
|---|---|---|---:|---|---:|---:|
| 1 | `path_01_beginner_english_fluency` | Beginner English Fluency | Beginner | None | 12 | 3 (at 4, 8, 12) |
| 2 | `path_02_intermediate_english_fluency` | Intermediate English Fluency | Intermediate | `path_01_beginner_english_fluency` | 12 | 3 (at 4, 8, 12) |
| 3 | `path_03_advanced_communication` | Advanced Communication | Advanced | `path_02_intermediate_english_fluency` | 12 | 3 (at 4, 8, 12) |
| 4 | `path_04_pronunciation_improvement` | Pronunciation Improvement | Beginner | None | 15 | 3 (at 5, 10, 15) |
| 5 | `path_05_confident_speaking` | Confident Speaking | Beginner | None | 12 | 3 (at 4, 8, 12) |
| 6 | `path_06_conversation_mastery` | Conversation Mastery | Beginner | None | 12 | 3 (at 4, 8, 12) |
| 7 | `path_07_public_speaking` | Public Speaking | Intermediate | None | 12 | 3 (at 4, 8, 12) |
| 8 | `path_08_storytelling` | Storytelling | Intermediate | None | 12 | 3 (at 4, 8, 12) |
| 9 | `path_09_professional_communication` | Professional Communication | Intermediate | None | 12 | 3 (at 4, 8, 12) |
| 10 | `path_10_interview_preparation` | Interview Preparation | Intermediate | None | 12 | 3 (at 4, 8, 12) |

## 2. Prerequisite & Progress Evaluation Rules
- **Completion Tracking**: `LearningPathsEngine.evaluatePathProgress(path, progressList)` checks each activity ID against the user's completed `UserActivityProgress` documents.
- **Prerequisite Threshold**: A prerequisite path is considered satisfied when the learner completes at least **70%** of its ordered activities (`prereqDone >= (size * 0.7).toInt()`).
- **Non-Linear Freedom**: Even when `prerequisitesMet == false`, SpeakCraft surfaces a clear advisory badge while still allowing the learner to launch any activity inside the path (`nextRecommendedActivityId` points to the earliest incomplete step).
- **Referential Integrity**: Automated unit tests assert that **100%** of `orderedActivityIds` across all 10 paths resolve to validated activities in `ActivityCatalogue`.
