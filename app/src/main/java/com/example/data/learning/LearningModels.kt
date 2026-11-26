package com.example.data.learning

import kotlinx.serialization.Serializable

/**
 * Supported 5-tier communication proficiency levels.
 */
@Serializable
enum class LearningLevel(val label: String, val rank: Int) {
    BEGINNER("Beginner", 1),
    ELEMENTARY("Elementary", 2),
    INTERMEDIATE("Intermediate", 3),
    UPPER_INTERMEDIATE("Upper Intermediate", 4),
    ADVANCED("Advanced", 5);

    companion object {
        fun fromLabel(raw: String?): LearningLevel {
            val clean = raw?.trim().orEmpty().lowercase().replace("-", " ")
            return when {
                clean.contains("elementary") -> ELEMENTARY
                clean.contains("upper") -> UPPER_INTERMEDIATE
                clean.contains("advanced") -> ADVANCED
                clean.contains("intermediate") -> INTERMEDIATE
                clean.contains("beginner") -> BEGINNER
                else -> INTERMEDIATE // unknown or blank: L3 Intermediate is the default level (D-17)
            }
        }
    }
}

/**
 * Supported communication learning goals driving daily plan personalization.
 */
@Serializable
enum class LearningGoal(val label: String, val focusSkills: List<String>) {
    DAILY_FLUENCY("Daily English Fluency", listOf("Fluency", "Listening")),
    PRONUNCIATION("Pronunciation Improvement", listOf("Pronunciation", "Articulation")),
    VOCABULARY("Vocabulary Building", listOf("Vocabulary", "Grammar")),
    CONFIDENCE("Speaking Confidence", listOf("Fluency", "Articulation")),
    INTERVIEWS("Job Interviews", listOf("Fluency", "Grammar", "Vocabulary")),
    PUBLIC_SPEAKING("Public Speaking", listOf("Articulation", "Fluency")),
    CONVERSATION("Conversation Practice", listOf("Listening", "Fluency")),
    WORKPLACE("Workplace Communication", listOf("Vocabulary", "Fluency")),
    PRESENTATIONS("Presentation Skills", listOf("Articulation", "Vocabulary"));

    companion object {
        fun fromLabel(raw: String?): LearningGoal {
            val clean = raw?.trim().orEmpty().lowercase()
            return when {
                clean.contains("pronun") || clean.contains("articulat") -> PRONUNCIATION
                clean.contains("vocab") || clean.contains("word") -> VOCABULARY
                clean.contains("interview") -> INTERVIEWS
                clean.contains("public") || clean.contains("stage") -> PUBLIC_SPEAKING
                clean.contains("present") -> PRESENTATIONS
                clean.contains("work") || clean.contains("meeting") || clean.contains("career") -> WORKPLACE
                clean.contains("conv") || clean.contains("social") -> CONVERSATION
                clean.contains("confiden") || clean.contains("authority") -> CONFIDENCE
                else -> DAILY_FLUENCY
            }
        }
    }
}

/**
 * Unified idempotent learning event types powering Dashboard, Daily Plan, Streak, Vocabulary, and Notifications.
 */
@Serializable
enum class LearningEventType {
    LESSON_STARTED,
    LESSON_COMPLETED,
    WORD_LEARNED,
    WORD_SPOKEN,
    SENTENCE_PRACTICED,
    OWN_SENTENCE_SUBMITTED,
    VOCABULARY_WORD_COMPLETED,
    QUIZ_COMPLETED,
    FLASHCARD_REVIEWED,
    DAILY_PLAN_COMPLETED,
    STREAK_UPDATED,
    /** An Activity Engine attempt was evaluated (score / pass as recorded). */
    ACTIVITY_ATTEMPTED,
    /** A Coach Practice attempt finished (valid or inconclusive; see metadata `valid`). */
    TECHNIQUE_PRACTICED,
    /** Every task of the current daily set was completed; the next set was started. */
    DAILY_SET_COMPLETED,
    /** A step of the daily routine checklist was ticked. */
    ROUTINE_STEP_COMPLETED,
    /** A real-world communication task was marked completed (self-reported). */
    REAL_WORLD_TASK_COMPLETED
}

@Serializable
data class LearningEvent(
    val idempotencyKey: String,
    val type: LearningEventType,
    val timestampMs: Long,
    val localDateKey: String,
    val entityId: String = "",
    val title: String = "",
    val score: Int? = null,
    val minutesSpent: Int = 0,
    val skillTag: String = "",
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class DailyPracticeTask(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val targetRoute: String,
    val skillFocus: String,
    val reasonTag: String = "",
    val isCompleted: Boolean = false,
    val completedAtMs: Long = 0L,
    val isRemediation: Boolean = false,
    /** Catalogue activity this task drills (completed by an evaluated attempt of that activity). */
    val linkedActivityId: String = "",
    /** Techniques this task practises (completed by a valid practice attempt of one of them). */
    val linkedTechniqueIds: List<String> = emptyList(),
    /** Content items of the daily set covered by this task; avoided in the next set once completed. */
    val itemIds: List<String> = emptyList()
)

@Serializable
data class PersonalizedDailyPlan(
    val dateKey: String,
    val level: LearningLevel,
    val goals: List<LearningGoal>,
    val targetMinutes: Int,
    val isRecoveryPlan: Boolean = false,
    val missedDaysBeforeToday: Int = 0,
    val adaptiveSummary: String = "",
    val tasks: List<DailyPracticeTask> = emptyList(),
    val generatedAtMs: Long = 0L
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val totalCount: Int get() = tasks.size
    val completionPercent: Int
        get() = if (tasks.isEmpty()) 0 else ((completedCount * 100f) / tasks.size).toInt().coerceIn(0, 100)
    val completedMinutes: Int get() = tasks.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val isCompleteForStreak: Boolean
        get() = tasks.isNotEmpty() && (completionPercent >= 60 || completedCount >= 2 || completedMinutes >= (targetMinutes / 2).coerceAtLeast(5))
}

@Serializable
data class LearningStreakState(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastCompletedDateKey: String = "",
    val completedDateKeys: List<String> = emptyList(),
    val weeklyCompletionMap: Map<String, Boolean> = emptyMap(),
    val achievedMilestones: List<Int> = emptyList(),
    val latestMilestoneUnlocked: Int? = null,
    val todayRequirementMet: Boolean = false
)

@Serializable
data class AchievementRecord(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val unlockedAtMs: Long
)

/**
 * 10 Learning & Account Notification Categories (STRICTLY NO APK / APP UPDATE ALERTS).
 */
@Serializable
enum class NotificationCategory(val label: String) {
    DAILY_PRACTICE_REMINDER("Daily Practice Reminders"),
    STREAK_ALERT("Streak Alerts"),
    STREAK_MILESTONE("Streak Milestones"),
    VOCABULARY_REVIEW("Vocabulary Review Due"),
    LESSON_PROGRESS("Lesson Progress"),
    ACHIEVEMENT_UNLOCKED("Achievements"),
    WEEKLY_SUMMARY("Weekly Progress Summary"),
    PRACTICE_FEEDBACK("Practice Feedback"),
    ADMIN_ANNOUNCEMENT("Announcements"),
    ACCOUNT_SECURITY("Account & Security")
}

@Serializable
data class UserNotification(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val timestampMs: Long,
    val isRead: Boolean = false,
    val targetRoute: String = "dashboard",
    val dedupKey: String = ""
)

@Serializable
data class NotificationPreferences(
    val dailyPracticeReminders: Boolean = true,
    val streakAlerts: Boolean = true,
    val vocabularyReviewReminders: Boolean = true,
    val lessonAndFeedbackAlerts: Boolean = true,
    val achievementNotifications: Boolean = true,
    val weeklySummaryAlerts: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val accountSecurityAlerts: Boolean = true,
    val preferredReminderTime: String = "09:00"
) {
    fun isEnabledFor(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.DAILY_PRACTICE_REMINDER -> dailyPracticeReminders
        NotificationCategory.STREAK_ALERT, NotificationCategory.STREAK_MILESTONE -> streakAlerts
        NotificationCategory.VOCABULARY_REVIEW -> vocabularyReviewReminders
        NotificationCategory.LESSON_PROGRESS, NotificationCategory.PRACTICE_FEEDBACK -> lessonAndFeedbackAlerts
        NotificationCategory.ACHIEVEMENT_UNLOCKED -> achievementNotifications
        NotificationCategory.WEEKLY_SUMMARY -> weeklySummaryAlerts
        NotificationCategory.ADMIN_ANNOUNCEMENT -> announcementsEnabled
        NotificationCategory.ACCOUNT_SECURITY -> accountSecurityAlerts
    }
}

// ---------------------------------------------------------------------------
// 5-Stage Vocabulary Builder, Quiz, Flashcards & Notebook Models
// ---------------------------------------------------------------------------

@Serializable
enum class WordLearningStage(val stageNumber: Int, val label: String) {
    STAGE_1_LEARN(1, "Learn"),
    STAGE_2_SPEAK_WORD(2, "Speak Word"),
    STAGE_3_SPEAK_SENTENCES(3, "Practice Sentences"),
    STAGE_4_CREATE_SENTENCE(4, "Own Sentence"),
    STAGE_5_COMPLETE(5, "Complete")
}

@Serializable
data class VocabularyWordProgress(
    val wordId: String,
    val word: String,
    val highestStageCompleted: Int = 0, // 0..5
    val stage1Completed: Boolean = false,
    val stage2Passed: Boolean = false,
    val stage2BestScore: Int = 0,
    val stage3Passed: Boolean = false,
    val stage3BestScore: Int = 0,
    val stage3SentencesCompleted: Int = 0,
    val stage4Passed: Boolean = false,
    val stage4BestScore: Int = 0,
    val stage4LastSentence: String = "",
    val isCompleted: Boolean = false,
    val completedAtMs: Long = 0L,
    val totalAttempts: Int = 0,
    val pointsEarned: Int = 0,
    val updatedAtMs: Long = 0L
)

@Serializable
data class SpacedRepetitionItem(
    val wordId: String,
    val word: String,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
    val nextReviewEpochDay: Long = 0L,
    val lastReviewedAtMs: Long = 0L,
    val status: String = "LEARNING" // NEW, LEARNING, REVIEW_DUE, MASTERED
)

@Serializable
data class VocabularyNotebookItem(
    val wordId: String,
    val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val meaning: String = "",
    val personalExample: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val personalNotes: String = "",
    val tags: List<String> = emptyList(),
    val level: String = "Intermediate",
    val status: String = "Learning", // Learning, Practicing, Mastered
    val isFavorite: Boolean = false,
    val dateAddedMs: Long = 0L,
    val reviewCount: Int = 0,
    val updatedAtMs: Long = 0L
)

@Serializable
enum class QuizQuestionType(val label: String) {
    MEANING_SELECTION("Meaning Selection"),
    WORD_SELECTION("Word Selection"),
    FILL_IN_THE_BLANK("Fill in the Blank"),
    SYNONYM("Synonym Match"),
    ANTONYM("Antonym Match"),
    CORRECT_SENTENCE("Correct Sentence"),
    CONTEXT_USAGE("Context Usage"),
    LISTENING_RECOGNITION("Listening Recognition"),
    SPEAKING_RECALL("Speaking Recall")
}

@Serializable
data class QuizQuestion(
    val id: String,
    val wordId: String,
    val word: String,
    val type: QuizQuestionType,
    val prompt: String,
    val audioPromptText: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String
)

@Serializable
data class QuizAnswerReview(
    val questionId: String,
    val word: String,
    val type: QuizQuestionType,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String
)

@Serializable
data class QuizSessionResult(
    val sessionId: String,
    val timestampMs: Long,
    val difficulty: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercent: Int,
    val elapsedSeconds: Int,
    val reviews: List<QuizAnswerReview> = emptyList()
)

// ---------------------------------------------------------------------------
// Admin RBAC & Audit Models
// ---------------------------------------------------------------------------

@Serializable
enum class AdminRole(val label: String) {
    NONE("User"),
    MODERATOR("Moderator"),
    CONTENT_ADMIN("Content Admin"),
    SUPER_ADMIN("Super Admin");

    fun canManageRoles(): Boolean = this == SUPER_ADMIN
    fun canManageContent(): Boolean = this == SUPER_ADMIN || this == CONTENT_ADMIN
    fun canModerate(): Boolean = this == SUPER_ADMIN || this == CONTENT_ADMIN || this == MODERATOR
    fun canPublishAnnouncements(): Boolean = this == SUPER_ADMIN || this == CONTENT_ADMIN

    companion object {
        fun fromString(raw: String?): AdminRole = when (raw?.uppercase()?.trim()) {
            "SUPER_ADMIN", "SUPERADMIN", "ADMIN" -> SUPER_ADMIN
            "CONTENT_ADMIN", "CONTENT" -> CONTENT_ADMIN
            "MODERATOR", "MOD" -> MODERATOR
            else -> NONE
        }
    }
}

@Serializable
data class AdminAuditRecord(
    val id: String,
    val adminUid: String,
    val adminRole: String,
    val action: String,
    val targetPath: String,
    val detail: String,
    val timestampMs: Long
)

@Serializable
data class AdminAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val targetAudience: String = "ALL",
    val targetRoute: String = "dashboard",
    val createdBy: String = "",
    val createdAtMs: Long = 0L,
    val active: Boolean = true
)

@Serializable
data class LearnedWordRecord(
    val wordId: String,
    val word: String,
    val level: String = "Intermediate",
    val learnedDateKey: String = "",
    val learnedEpochDay: Long = 0L,
    val spokenAssessmentScore: Int = 0,
    val spokenTranscript: String = "",
    val markedDoneAtMs: Long = 0L
)

@Serializable
data class DailyVocabularyAssignment(
    val dateKey: String = "",
    val level: LearningLevel = LearningLevel.INTERMEDIATE,
    val wordIds: List<String> = emptyList(),
    val completedWordIds: Set<String> = emptySet(),
    val wordScores: Map<String, Int> = emptyMap(),
    val lastTranscripts: Map<String, String> = emptyMap(),
    val dayIndex: Int = 1
) {
    val allDone: Boolean get() = wordIds.isNotEmpty() && wordIds.all { it in completedWordIds }
}

@Serializable
data class WeeklyVocabularyAssessmentRecord(
    val id: String = "",
    val completedDateKey: String = "",
    val completedEpochDay: Long = 0L,
    val completedAtMs: Long = 0L,
    val cycleNumber: Int = 1,
    val assessedWordIds: List<String> = emptyList(),
    val passedWordIds: List<String> = emptyList(),
    val scorePercent: Int = 0
)

@Serializable
data class WeeklyAssessmentEligibility(
    val isUnlocked: Boolean = false,
    val isDueNow: Boolean = false,
    val daysElapsed: Int = 0,
    val distinctActiveDays: Int = 0,
    val daysRemaining: Int = 7,
    val cycleNumber: Int = 1,
    val learnedWordIdsInCycle: List<String> = emptyList(),
    val latestAssessment: WeeklyVocabularyAssessmentRecord? = null
)

@Serializable
data class SavedSpeakingRecording(
    val id: String,
    val moduleType: String = "SPEAKING_LAB",
    val promptTitle: String = "",
    val timestampMs: Long = 0L,
    val durationSeconds: Int = 0,
    val localMediaPath: String = "",
    val mediaType: String = "audio/mp4",
    val transcript: String = "",
    val overallAssessment: String = "",
    val evaluated: Boolean = false,
    val fluencyScore: Int = 0,
    val pronunciationScore: Int = 0,
    val pronunciationAssessable: Boolean = false,
    val articulationScore: Int = 0,
    val wordsPerMinute: Int = 0
)

// ---------------------------------------------------------------------------
// Unified Persisted Learning Snapshot (Offline-First Local Store + Sync)
// ---------------------------------------------------------------------------

@Serializable
data class PersistedLearningSnapshot(
    val schemaVersion: Int = 3,
    val userId: String = "",
    val currentLevel: LearningLevel = LearningLevel.BEGINNER,
    val targetLevel: LearningLevel = LearningLevel.ADVANCED,
    val selectedGoals: List<LearningGoal> = listOf(LearningGoal.DAILY_FLUENCY, LearningGoal.CONFIDENCE),
    val dailyTargetMinutes: Int = 15,
    val preferredPracticeTime: String = "Morning (09:00)",
    val interests: List<String> = listOf("Leadership", "Workplace", "Storytelling"),
    val dailyPlan: PersonalizedDailyPlan? = null,
    val streak: LearningStreakState = LearningStreakState(),
    val wordProgress: Map<String, VocabularyWordProgress> = emptyMap(),
    val spacedRepetition: Map<String, SpacedRepetitionItem> = emptyMap(),
    val notebook: Map<String, VocabularyNotebookItem> = emptyMap(),
    val quizHistory: List<QuizSessionResult> = emptyList(),
    val achievements: List<AchievementRecord> = emptyList(),
    val notifications: List<UserNotification> = emptyList(),
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val processedEventKeys: Set<String> = emptySet(),
    val eventLog: List<LearningEvent> = emptyList(),
    val totalPoints: Int = 0,
    val totalPracticeMinutes: Int = 0,
    val recurringMistakeTags: List<String> = emptyList(),
    val masteredSkillTags: List<String> = emptyList(),
    val lastUpdatedAtMs: Long = 0L,
    /** Position in the completion-driven daily-set sequence (current set + recently completed items). */
    val dailyRotation: DailyRotationState = DailyRotationState(),
    /** Daily record: dateKey -> completed tasks / steps of that day. */
    val dailyRecords: Map<String, List<DailyRecordEntry>> = emptyMap(),
    /** Completed daily tasks from every set (most recent first), for the learning-path tasks pillar. */
    val completedDailyTasks: List<DailyPracticeTask> = emptyList(),
    /** Activity Engine progress per catalogue activity (activity + exercise pillars). */
    val activityProgress: Map<String, LocalActivityProgress> = emptyMap(),
    /** Evaluated Activity Engine attempts, most recent first. */
    val activityAttempts: List<LocalActivityAttempt> = emptyList(),
    /** Coach Practice evidence per technique (techniques pillar). */
    val techniquePractice: Map<String, TechniquePracticeStat> = emptyMap(),
    /** Vocabulary Builder: epoch day when the user started learning vocabulary. */
    val firstVocabularyEpochDay: Long = 0L,
    /** Vocabulary Builder: distinct local dates where vocabulary words were shown or practiced. */
    val vocabularyActiveDateKeys: List<String> = emptyList(),
    /** Vocabulary Builder: ordered history of shown daily word IDs so daily 3 words never repeat. */
    val shownVocabularyWordIds: List<String> = emptyList(),
    /** Vocabulary Builder: today's 3 level-matched non-repeating words + spoken completion tracking. */
    val dailyVocabulary: DailyVocabularyAssignment? = null,
    /** Vocabulary Builder: all words passed via speaking assessment (feeds the 7-day AI Quiz). */
    val learnedVocabularyLog: List<LearnedWordRecord> = emptyList(),
    /** Vocabulary Builder: completed 7-day cumulative AI Quiz assessments. */
    val weeklyAssessments: List<WeeklyVocabularyAssessmentRecord> = emptyList(),
    /** Coach Practice recordings archived locally for User Profile -> Recordings tab. */
    val practiceRecordings: List<SavedPracticeRecording> = emptyList(),
    /** Studio & Lab audio/video recordings archived locally so clips & transcripts survive offline or Firestore stripping. */
    val speakingRecordings: List<SavedSpeakingRecording> = emptyList()
)
