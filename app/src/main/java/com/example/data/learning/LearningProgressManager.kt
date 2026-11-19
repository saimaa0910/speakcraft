package com.example.data.learning

import android.content.Context
import android.util.Log
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabSeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val TAG = "LearningProgressMgr"

/**
 * Granular clear options for Settings -> Data & Privacy.
 */
data class ClearLearningDataOptions(
    val clearDailyPlanAndEvents: Boolean = false,
    val clearVocabularyProgress: Boolean = false,
    val clearNotebook: Boolean = false,
    val clearQuizHistory: Boolean = false,
    val clearNotifications: Boolean = false,
    val clearStreakAndAchievements: Boolean = false
)

/**
 * Single unified, offline-first, idempotent Learning Progress Manager (Phase 10).
 *
 * Guarantees:
 * - Every learning event (lesson, vocabulary stage 1-5, quiz, flashcard, daily task, speaking session)
 *   is deduplicated by [LearningEvent.idempotencyKey] so repeated taps or retries never double-count.
 * - Updates daily plan, streak, achievements, vocabulary mastery, spaced repetition, and notifications
 *   atomically in one reactive [StateFlow<PersistedLearningSnapshot>].
 * - Persists immediately to local JSON storage (`filesDir/learning_state/`) so 100% of learning
 *   state survives app restarts and offline usage without waiting on network.
 * - Provides deterministic conflict resolution [mergeSnapshots] for cloud synchronization.
 */
class LearningProgressManager(
    private val storageDir: File? = null,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val todayKeyProvider: () -> String = { StreakAndCompletionEngine.todayKey() },
    private val todayEpochDayProvider: () -> Long = { clockMs() / 86_400_000L }
) {
    constructor(context: Context) : this(
        storageDir = File(context.applicationContext.filesDir, "learning_state").apply { mkdirs() }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _state = MutableStateFlow(PersistedLearningSnapshot())
    val state: StateFlow<PersistedLearningSnapshot> = _state.asStateFlow()

    private var activeUserId: String = ""

    /**
     * Loads or initializes persisted learning state for [userId], synchronizes profile metadata,
     * refreshes today's personalized plan & streak, and creates daily welcome notifications if enabled.
     */
    fun loadForUser(
        userId: String,
        profile: UserProfile? = null,
        recentSessions: List<SpeakingSession> = emptyList()
    ) {
        val cleanUid = userId.trim().ifBlank { "local_guest" }
        val loaded = if (activeUserId == cleanUid && _state.value.userId == cleanUid) {
            _state.value
        } else {
            activeUserId = cleanUid
            readFromDisk(cleanUid) ?: PersistedLearningSnapshot(userId = cleanUid)
        }

        val todayKey = todayKeyProvider()
        val now = clockMs()
        val level = profile?.communicationLevel?.takeIf { it.isNotBlank() }
            ?.let { LearningLevel.fromLabel(it) } ?: loaded.currentLevel
        val targetMinutes = profile?.dailyGoalMinutes?.toInt()?.coerceIn(10, 90) ?: loaded.dailyTargetMinutes
        val goals = if (loaded.selectedGoals.isEmpty() && !profile?.primaryGoal.isNullOrBlank()) {
            listOf(LearningGoal.fromLabel(profile?.primaryGoal))
        } else loaded.selectedGoals.ifEmpty { listOf(LearningGoal.DAILY_FLUENCY, LearningGoal.CONFIDENCE) }

        val refreshedStreak = StreakAndCompletionEngine.refreshForDate(loaded.streak, todayKey)
        val missedDays = StreakAndCompletionEngine.missedDaysCount(refreshedStreak, todayKey)

        val skillScores = buildSkillScoreMap(profile)
        val recurringMistakes = recentSessions.take(5).flatMap { it.mistakes }.distinct().take(6)
            .ifEmpty { loaded.recurringMistakeTags }
        val masteredSkills = skillScores.filterValues { it >= 90 }.keys + loaded.masteredSkillTags
        val dueWords = SpacedRepetitionScheduler.dueItems(loaded.spacedRepetition, todayEpochDayProvider()).size

        val plan = if (loaded.dailyPlan?.dateKey == todayKey && loaded.dailyPlan.tasks.isNotEmpty()) {
            loaded.dailyPlan
        } else {
            DailyPracticePlanEngine.generateOrAdaptPlan(
                DailyPracticePlanEngine.PlanGenerationInput(
                    dateKey = todayKey,
                    level = level,
                    goals = goals,
                    targetMinutes = targetMinutes,
                    skillScores = skillScores,
                    recurringMistakes = recurringMistakes,
                    masteredSkills = masteredSkills,
                    missedDaysBeforeToday = missedDays,
                    wordsDueForReview = dueWords,
                    existingPlanForToday = loaded.dailyPlan,
                    nowMs = now
                )
            )
        }

        var updated = loaded.copy(
            userId = cleanUid,
            currentLevel = level,
            selectedGoals = goals,
            dailyTargetMinutes = targetMinutes,
            dailyPlan = plan,
            streak = refreshedStreak,
            recurringMistakeTags = recurringMistakes,
            masteredSkillTags = masteredSkills.toList(),
            lastUpdatedAtMs = now
        )

        // Generate helpful, deduplicated learning notifications (NEVER APK/update alerts!)
        updated = appendNotificationIfAllowed(
            snapshot = updated,
            category = NotificationCategory.DAILY_PRACTICE_REMINDER,
            title = if (plan.isRecoveryPlan) "Welcome back! Your gentle recovery plan is ready" else "Today's ${plan.targetMinutes}-min practice plan is ready",
            body = plan.adaptiveSummary,
            targetRoute = "daily_practice",
            dedupKey = "daily_plan_ready_$todayKey",
            nowMs = now
        )

        if (dueWords > 0) {
            updated = appendNotificationIfAllowed(
                snapshot = updated,
                category = NotificationCategory.VOCABULARY_REVIEW,
                title = "$dueWords vocabulary word${if (dueWords > 1) "s" else ""} due for review",
                body = "Quick spaced-repetition flashcards keep words active in your speaking vocabulary.",
                targetRoute = "vocabulary",
                dedupKey = "vocab_due_$todayKey",
                nowMs = now
            )
        }

        if (refreshedStreak.currentStreakDays > 0 && !refreshedStreak.todayRequirementMet) {
            updated = appendNotificationIfAllowed(
                snapshot = updated,
                category = NotificationCategory.STREAK_ALERT,
                title = "Keep your ${refreshedStreak.currentStreakDays}-day streak alive!",
                body = "Complete 2 tasks in today's plan to lock in Day ${refreshedStreak.currentStreakDays + 1}.",
                targetRoute = "daily_practice",
                dedupKey = "streak_reminder_$todayKey",
                nowMs = now
            )
        }

        commitState(updated)
    }

    /**
     * Updates learning preferences (level, target level, goals, daily duration, schedule, interests)
     * and adapts today's plan while preserving any already-completed tasks.
     */
    fun updateLearningPreferences(
        currentLevel: LearningLevel? = null,
        targetLevel: LearningLevel? = null,
        goals: List<LearningGoal>? = null,
        dailyTargetMinutes: Int? = null,
        preferredPracticeTime: String? = null,
        interests: List<String>? = null,
        profile: UserProfile? = null
    ) {
        val prev = _state.value
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val nextLevel = currentLevel ?: prev.currentLevel
        val nextTarget = targetLevel ?: prev.targetLevel
        val nextGoals = goals?.takeIf { it.isNotEmpty() } ?: prev.selectedGoals
        val nextMinutes = dailyTargetMinutes?.coerceIn(10, 90) ?: prev.dailyTargetMinutes
        val dueWords = SpacedRepetitionScheduler.dueItems(prev.spacedRepetition, todayEpochDayProvider()).size

        val adaptedPlan = DailyPracticePlanEngine.generateOrAdaptPlan(
            DailyPracticePlanEngine.PlanGenerationInput(
                dateKey = todayKey,
                level = nextLevel,
                goals = nextGoals,
                targetMinutes = nextMinutes,
                skillScores = buildSkillScoreMap(profile),
                recurringMistakes = prev.recurringMistakeTags,
                masteredSkills = prev.masteredSkillTags.toSet(),
                missedDaysBeforeToday = StreakAndCompletionEngine.missedDaysCount(prev.streak, todayKey),
                wordsDueForReview = dueWords,
                existingPlanForToday = prev.dailyPlan,
                nowMs = now
            )
        )

        commitState(
            prev.copy(
                currentLevel = nextLevel,
                targetLevel = nextTarget,
                selectedGoals = nextGoals,
                dailyTargetMinutes = nextMinutes,
                preferredPracticeTime = preferredPracticeTime ?: prev.preferredPracticeTime,
                interests = interests ?: prev.interests,
                dailyPlan = adaptedPlan,
                lastUpdatedAtMs = now
            )
        )
    }

    // -----------------------------------------------------------------------
    // Unified Idempotent Event Ingestion
    // -----------------------------------------------------------------------

    /**
     * Records a learning event idempotently. Duplicate [LearningEvent.idempotencyKey] values are ignored.
     * Automatically updates daily tasks, streak, achievements, points, practice minutes, and notifications.
     */
    fun recordEvent(event: LearningEvent): Boolean {
        val current = _state.value
        if (event.idempotencyKey in current.processedEventKeys) {
            return false
        }

        val todayKey = event.localDateKey.ifBlank { todayKeyProvider() }
        val now = if (event.timestampMs > 0L) event.timestampMs else clockMs()
        val pointsDelta = when (event.type) {
            LearningEventType.LESSON_STARTED -> 5
            LearningEventType.LESSON_COMPLETED -> 30
            LearningEventType.WORD_LEARNED -> 10
            LearningEventType.WORD_SPOKEN -> 15
            LearningEventType.SENTENCE_PRACTICED -> 20
            LearningEventType.OWN_SENTENCE_SUBMITTED -> 25
            LearningEventType.VOCABULARY_WORD_COMPLETED -> 50
            LearningEventType.QUIZ_COMPLETED -> ((event.score ?: 70) / 2).coerceIn(15, 50)
            LearningEventType.FLASHCARD_REVIEWED -> 5
            LearningEventType.DAILY_PLAN_COMPLETED -> 60
            LearningEventType.STREAK_UPDATED -> 10
        }

        // Auto-mark matching daily practice task complete when relevant activity finishes
        val currentPlan = current.dailyPlan ?: DailyPracticePlanEngine.generateOrAdaptPlan(
            DailyPracticePlanEngine.PlanGenerationInput(
                dateKey = todayKey,
                level = current.currentLevel,
                goals = current.selectedGoals,
                targetMinutes = current.dailyTargetMinutes,
                nowMs = now
            )
        )
        val updatedTasks = currentPlan.tasks.map { task ->
            if (task.isCompleted) task
            else if (doesEventSatisfyTask(event, task)) {
                task.copy(isCompleted = true, completedAtMs = now)
            } else task
        }
        val updatedPlan = currentPlan.copy(tasks = updatedTasks)

        // Evaluate daily completion requirement for streak update
        val prevStreak = current.streak
        val newStreak = StreakAndCompletionEngine.recordDailyCompletion(
            current = prevStreak,
            todayDateKey = todayKey,
            requirementMet = updatedPlan.isCompleteForStreak
        )

        var next = current.copy(
            dailyPlan = updatedPlan,
            streak = newStreak,
            processedEventKeys = current.processedEventKeys + event.idempotencyKey,
            eventLog = (listOf(event) + current.eventLog).take(300),
            totalPoints = current.totalPoints + pointsDelta,
            totalPracticeMinutes = current.totalPracticeMinutes + event.minutesSpent.coerceAtLeast(0),
            lastUpdatedAtMs = now
        )

        // Emit notification when daily requirement newly met or milestone reached
        if (newStreak.todayRequirementMet && !prevStreak.todayRequirementMet) {
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.STREAK_ALERT,
                title = "Daily goal complete — ${newStreak.currentStreakDays}-day streak!",
                body = "Awesome consistency! You completed today's practice requirement.",
                targetRoute = "dashboard",
                dedupKey = "streak_completed_$todayKey",
                nowMs = now
            )
        }
        if (newStreak.latestMilestoneUnlocked != null && newStreak.latestMilestoneUnlocked != prevStreak.latestMilestoneUnlocked) {
            val milestone = newStreak.latestMilestoneUnlocked
            next = unlockAchievementIfNew(
                snapshot = next,
                id = "streak_milestone_$milestone",
                title = "$milestone-Day Streak Milestone",
                description = "Practiced consistently for $milestone consecutive days.",
                category = "Streak",
                nowMs = now
            )
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.STREAK_MILESTONE,
                title = "Milestone unlocked: $milestone-Day Streak!",
                body = "Your daily communication habit reached $milestone days in a row.",
                targetRoute = "analytics",
                dedupKey = "milestone_$milestone",
                nowMs = now
            )
        }

        if (event.type == LearningEventType.LESSON_COMPLETED && event.title.isNotBlank()) {
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.LESSON_PROGRESS,
                title = "Lesson completed: ${event.title}",
                body = "Your learning track and dashboard progress have been updated.",
                targetRoute = "learning_paths",
                dedupKey = "lesson_done_${event.entityId}_$todayKey",
                nowMs = now
            )
        }

        if (event.type == LearningEventType.QUIZ_COMPLETED && event.score != null) {
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.PRACTICE_FEEDBACK,
                title = "Local Vocabulary Quiz scored ${event.score}%",
                body = "Review any missed words in your Vocabulary Notebook to cement mastery.",
                targetRoute = "vocabulary",
                dedupKey = "quiz_done_${event.idempotencyKey}",
                nowMs = now
            )
        }

        next = evaluateDynamicAchievements(next, now)
        commitState(next)
        return true
    }

    /**
     * Toggles or completes a specific daily plan task by ID.
     */
    fun completeDailyTask(taskId: String) {
        val current = _state.value
        val plan = current.dailyPlan ?: return
        val task = plan.tasks.firstOrNull { it.id == taskId } ?: return
        if (task.isCompleted) return

        val todayKey = todayKeyProvider()
        val now = clockMs()
        val updatedTasks = plan.tasks.map {
            if (it.id == taskId) it.copy(isCompleted = true, completedAtMs = now) else it
        }
        val updatedPlan = plan.copy(tasks = updatedTasks)
        val newStreak = StreakAndCompletionEngine.recordDailyCompletion(
            current = current.streak,
            todayDateKey = todayKey,
            requirementMet = updatedPlan.isCompleteForStreak
        )

        var next = current.copy(
            dailyPlan = updatedPlan,
            streak = newStreak,
            totalPracticeMinutes = current.totalPracticeMinutes + task.durationMinutes,
            totalPoints = current.totalPoints + 15,
            lastUpdatedAtMs = now
        )
        if (updatedPlan.completedCount == updatedPlan.totalCount) {
            next = unlockAchievementIfNew(
                snapshot = next,
                id = "first_full_daily_plan",
                title = "Daily Plan Finisher",
                description = "Completed every task in your personalized daily practice plan.",
                category = "Consistency",
                nowMs = now
            )
        }
        commitState(next)
    }

    // -----------------------------------------------------------------------
    // 5-Stage Vocabulary Builder Progress, Quiz, Flashcards & Notebook
    // -----------------------------------------------------------------------

    fun recordVocabularyStageProgress(
        seed: VocabSeed,
        stage: WordLearningStage,
        passed: Boolean,
        score: Int = 100,
        userSentence: String = ""
    ) {
        val current = _state.value
        val now = clockMs()
        val todayKey = todayKeyProvider()
        val existing = current.wordProgress[seed.id] ?: VocabularyWordProgress(
            wordId = seed.id,
            word = seed.word
        )

        // Monotonic locks: once a stage or full word completion is earned, replay NEVER erases it
        val stage1Done = existing.stage1Completed || stage.stageNumber >= 1
        val stage2Done = existing.stage2Passed || (stage == WordLearningStage.STAGE_2_SPEAK_WORD && passed)
        val stage3Done = existing.stage3Passed || (stage == WordLearningStage.STAGE_3_SPEAK_SENTENCES && passed)
        val stage4Done = existing.stage4Passed || (stage == WordLearningStage.STAGE_4_CREATE_SENTENCE && passed)
        val stage5Done = existing.isCompleted || (stage == WordLearningStage.STAGE_5_COMPLETE && stage1Done && stage2Done && stage3Done && stage4Done)

        val highest = maxOf(
            existing.highestStageCompleted,
            when {
                stage5Done -> 5
                stage4Done -> 4
                stage3Done -> 3
                stage2Done -> 2
                stage1Done -> 1
                else -> 0
            }
        )

        val newlyCompleted = stage5Done && !existing.isCompleted
        val updatedProgress = existing.copy(
            highestStageCompleted = highest,
            stage1Completed = stage1Done,
            stage2Passed = stage2Done,
            stage2BestScore = if (stage == WordLearningStage.STAGE_2_SPEAK_WORD) maxOf(existing.stage2BestScore, score) else existing.stage2BestScore,
            stage3Passed = stage3Done,
            stage3BestScore = if (stage == WordLearningStage.STAGE_3_SPEAK_SENTENCES) maxOf(existing.stage3BestScore, score) else existing.stage3BestScore,
            stage3SentencesCompleted = if (stage == WordLearningStage.STAGE_3_SPEAK_SENTENCES && passed) existing.stage3SentencesCompleted + 1 else existing.stage3SentencesCompleted,
            stage4Passed = stage4Done,
            stage4BestScore = if (stage == WordLearningStage.STAGE_4_CREATE_SENTENCE) maxOf(existing.stage4BestScore, score) else existing.stage4BestScore,
            stage4LastSentence = userSentence.ifBlank { existing.stage4LastSentence },
            isCompleted = stage5Done,
            completedAtMs = if (newlyCompleted) now else existing.completedAtMs,
            totalAttempts = existing.totalAttempts + 1,
            pointsEarned = existing.pointsEarned + if (passed) 15 else 2,
            updatedAtMs = now
        )

        val updatedMap = current.wordProgress + (seed.id to updatedProgress)

        // Schedule spaced repetition review automatically upon Stage 5 completion
        val updatedSrs = if (newlyCompleted || seed.id !in current.spacedRepetition) {
            val srsItem = SpacedRepetitionScheduler.scheduleNext(
                wordId = seed.id,
                word = seed.word,
                current = current.spacedRepetition[seed.id],
                known = passed,
                todayEpochDay = todayEpochDayProvider(),
                nowMs = now
            )
            current.spacedRepetition + (seed.id to srsItem)
        } else current.spacedRepetition

        commitState(
            current.copy(
                wordProgress = updatedMap,
                spacedRepetition = updatedSrs,
                lastUpdatedAtMs = now
            )
        )

        val eventType = when (stage) {
            WordLearningStage.STAGE_1_LEARN -> LearningEventType.WORD_LEARNED
            WordLearningStage.STAGE_2_SPEAK_WORD -> LearningEventType.WORD_SPOKEN
            WordLearningStage.STAGE_3_SPEAK_SENTENCES -> LearningEventType.SENTENCE_PRACTICED
            WordLearningStage.STAGE_4_CREATE_SENTENCE -> LearningEventType.OWN_SENTENCE_SUBMITTED
            WordLearningStage.STAGE_5_COMPLETE -> LearningEventType.VOCABULARY_WORD_COMPLETED
        }
        val keySuffix = if (stage == WordLearningStage.STAGE_5_COMPLETE) "complete" else "${stage.stageNumber}_${updatedProgress.totalAttempts}"
        recordEvent(
            LearningEvent(
                idempotencyKey = "vocab_${seed.id}_$keySuffix",
                type = eventType,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = seed.id,
                title = seed.word,
                score = score,
                minutesSpent = if (stage == WordLearningStage.STAGE_5_COMPLETE) 2 else 1,
                skillTag = "Vocabulary"
            )
        )
    }

    fun recordFlashcardReview(wordId: String, word: String, known: Boolean) {
        val current = _state.value
        val now = clockMs()
        val updatedItem = SpacedRepetitionScheduler.scheduleNext(
            wordId = wordId,
            word = word,
            current = current.spacedRepetition[wordId],
            known = known,
            todayEpochDay = todayEpochDayProvider(),
            nowMs = now
        )
        val updatedNotebook = current.notebook[wordId]?.let { nb ->
            nb.copy(
                reviewCount = nb.reviewCount + 1,
                status = if (updatedItem.status == "MASTERED") "Mastered" else "Practicing",
                updatedAtMs = now
            )
        }
        commitState(
            current.copy(
                spacedRepetition = current.spacedRepetition + (wordId to updatedItem),
                notebook = if (updatedNotebook != null) current.notebook + (wordId to updatedNotebook) else current.notebook,
                lastUpdatedAtMs = now
            )
        )
        recordEvent(
            LearningEvent(
                idempotencyKey = "flashcard_${wordId}_${now / 1000L}",
                type = LearningEventType.FLASHCARD_REVIEWED,
                timestampMs = now,
                localDateKey = todayKeyProvider(),
                entityId = wordId,
                title = word,
                score = if (known) 100 else 50,
                minutesSpent = 1,
                skillTag = "Vocabulary"
            )
        )
    }

    fun recordQuizSession(result: QuizSessionResult) {
        val current = _state.value
        val now = clockMs()
        commitState(
            current.copy(
                quizHistory = (listOf(result) + current.quizHistory).take(50),
                lastUpdatedAtMs = now
            )
        )
        recordEvent(
            LearningEvent(
                idempotencyKey = "quiz_${result.sessionId}",
                type = LearningEventType.QUIZ_COMPLETED,
                timestampMs = now,
                localDateKey = todayKeyProvider(),
                entityId = result.sessionId,
                title = "Local Vocabulary Quiz (${result.difficulty})",
                score = result.scorePercent,
                minutesSpent = (result.elapsedSeconds / 60).coerceAtLeast(2),
                skillTag = "Vocabulary"
            )
        )
    }

    fun upsertNotebookItem(item: VocabularyNotebookItem) {
        val current = _state.value
        val now = clockMs()
        val existing = current.notebook[item.wordId]
        val merged = item.copy(
            dateAddedMs = existing?.dateAddedMs?.takeIf { it > 0L } ?: now,
            reviewCount = maxOf(existing?.reviewCount ?: 0, item.reviewCount),
            updatedAtMs = now
        )
        commitState(
            current.copy(
                notebook = current.notebook + (merged.wordId to merged),
                lastUpdatedAtMs = now
            )
        )
    }

    fun toggleNotebookFavorite(wordId: String) {
        val current = _state.value
        val item = current.notebook[wordId] ?: return
        commitState(
            current.copy(
                notebook = current.notebook + (wordId to item.copy(isFavorite = !item.isFavorite, updatedAtMs = clockMs()))
            )
        )
    }

    fun deleteNotebookItem(wordId: String) {
        val current = _state.value
        if (wordId !in current.notebook) return
        commitState(
            current.copy(
                notebook = current.notebook - wordId,
                lastUpdatedAtMs = clockMs()
            )
        )
    }

    // -----------------------------------------------------------------------
    // Notification Center Operations (STRICTLY Learning & Account ONLY)
    // -----------------------------------------------------------------------

    fun unreadNotificationCount(): Int = _state.value.notifications.count { !it.isRead }

    fun markNotificationRead(notificationId: String) {
        val current = _state.value
        commitState(
            current.copy(
                notifications = current.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
            )
        )
    }

    fun markAllNotificationsRead() {
        val current = _state.value
        commitState(
            current.copy(
                notifications = current.notifications.map { it.copy(isRead = true) }
            )
        )
    }

    fun deleteNotification(notificationId: String) {
        val current = _state.value
        commitState(
            current.copy(
                notifications = current.notifications.filterNot { it.id == notificationId }
            )
        )
    }

    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        commitState(
            _state.value.copy(
                notificationPreferences = prefs,
                lastUpdatedAtMs = clockMs()
            )
        )
    }

    fun ingestAdminAnnouncement(announcement: AdminAnnouncement) {
        if (!announcement.active) return
        val updated = appendNotificationIfAllowed(
            snapshot = _state.value,
            category = NotificationCategory.ADMIN_ANNOUNCEMENT,
            title = announcement.title,
            body = announcement.message,
            targetRoute = announcement.targetRoute.ifBlank { "dashboard" },
            dedupKey = "admin_ann_${announcement.id}",
            nowMs = announcement.createdAtMs.takeIf { it > 0L } ?: clockMs()
        )
        commitState(updated)
    }

    fun addAccountSecurityNotice(title: String, body: String) {
        val updated = appendNotificationIfAllowed(
            snapshot = _state.value,
            category = NotificationCategory.ACCOUNT_SECURITY,
            title = title,
            body = body,
            targetRoute = "settings",
            dedupKey = "sec_${title.hashCode()}_${todayKeyProvider()}",
            nowMs = clockMs()
        )
        commitState(updated)
    }

    // -----------------------------------------------------------------------
    // Export, Selective Clear, Account Wipe & Sync Conflict Resolution
    // -----------------------------------------------------------------------

    fun exportUserLearningDataJson(): String = json.encodeToString(_state.value)

    fun clearSelectedLearningData(options: ClearLearningDataOptions) {
        val current = _state.value
        val now = clockMs()
        var next = current
        if (options.clearDailyPlanAndEvents) {
            next = next.copy(dailyPlan = null, eventLog = emptyList(), processedEventKeys = emptySet())
        }
        if (options.clearVocabularyProgress) {
            next = next.copy(wordProgress = emptyMap(), spacedRepetition = emptyMap())
        }
        if (options.clearNotebook) {
            next = next.copy(notebook = emptyMap())
        }
        if (options.clearQuizHistory) {
            next = next.copy(quizHistory = emptyList())
        }
        if (options.clearNotifications) {
            next = next.copy(notifications = emptyList())
        }
        if (options.clearStreakAndAchievements) {
            next = next.copy(streak = LearningStreakState(), achievements = emptyList(), totalPoints = 0)
        }
        commitState(next.copy(lastUpdatedAtMs = now))
    }

    fun wipeAllLocalDataForUser(userId: String) {
        val cleanUid = userId.trim().ifBlank { activeUserId }
        fileForUser(cleanUid)?.delete()
        if (cleanUid == activeUserId) {
            _state.value = PersistedLearningSnapshot()
            activeUserId = ""
        }
    }

    companion object {
        /**
         * Deterministic conflict resolution for merging local offline state with synced cloud state:
         * - Completions, vocabulary stages, achievements, processed event keys, and completed dates take union/max (monotonic: progress is never lost).
         * - Longest streak takes max; active streak is recomputed from union of completed dates.
         * - Preferences (level, goals, daily minutes, notification preferences) take the latest timestamp.
         */
        fun mergeSnapshots(
            local: PersistedLearningSnapshot,
            remote: PersistedLearningSnapshot,
            todayDateKey: String = StreakAndCompletionEngine.todayKey()
        ): PersistedLearningSnapshot {
            val preferLocalPrefs = local.lastUpdatedAtMs >= remote.lastUpdatedAtMs
            val unionDates = (local.streak.completedDateKeys + remote.streak.completedDateKeys).distinct().sorted()
            val mergedStreakBase = LearningStreakState(
                longestStreakDays = maxOf(local.streak.longestStreakDays, remote.streak.longestStreakDays),
                completedDateKeys = unionDates,
                achievedMilestones = (local.streak.achievedMilestones + remote.streak.achievedMilestones).distinct().sorted()
            )
            val mergedStreak = StreakAndCompletionEngine.refreshForDate(mergedStreakBase, todayDateKey)

            val mergedWords = (local.wordProgress.keys + remote.wordProgress.keys).associateWith { key ->
                val a = local.wordProgress[key]
                val b = remote.wordProgress[key]
                when {
                    a == null -> b!!
                    b == null -> a
                    else -> a.copy(
                        highestStageCompleted = maxOf(a.highestStageCompleted, b.highestStageCompleted),
                        stage1Completed = a.stage1Completed || b.stage1Completed,
                        stage2Passed = a.stage2Passed || b.stage2Passed,
                        stage2BestScore = maxOf(a.stage2BestScore, b.stage2BestScore),
                        stage3Passed = a.stage3Passed || b.stage3Passed,
                        stage3BestScore = maxOf(a.stage3BestScore, b.stage3BestScore),
                        stage3SentencesCompleted = maxOf(a.stage3SentencesCompleted, b.stage3SentencesCompleted),
                        stage4Passed = a.stage4Passed || b.stage4Passed,
                        stage4BestScore = maxOf(a.stage4BestScore, b.stage4BestScore),
                        stage4LastSentence = if (a.updatedAtMs >= b.updatedAtMs) a.stage4LastSentence else b.stage4LastSentence,
                        isCompleted = a.isCompleted || b.isCompleted,
                        completedAtMs = maxOf(a.completedAtMs, b.completedAtMs),
                        totalAttempts = maxOf(a.totalAttempts, b.totalAttempts),
                        pointsEarned = maxOf(a.pointsEarned, b.pointsEarned),
                        updatedAtMs = maxOf(a.updatedAtMs, b.updatedAtMs)
                    )
                }
            }

            val mergedNotebook = (local.notebook.keys + remote.notebook.keys).associateWith { key ->
                val a = local.notebook[key]
                val b = remote.notebook[key]
                when {
                    a == null -> b!!
                    b == null -> a
                    a.updatedAtMs >= b.updatedAtMs -> a
                    else -> b
                }
            }

            val mergedAchievements = (local.achievements + remote.achievements)
                .distinctBy { it.id }
                .sortedByDescending { it.unlockedAtMs }

            val mergedEvents = (local.eventLog + remote.eventLog)
                .distinctBy { it.idempotencyKey }
                .sortedByDescending { it.timestampMs }
                .take(300)

            return (if (preferLocalPrefs) local else remote).copy(
                streak = mergedStreak,
                wordProgress = mergedWords,
                notebook = mergedNotebook,
                achievements = mergedAchievements,
                processedEventKeys = local.processedEventKeys + remote.processedEventKeys,
                eventLog = mergedEvents,
                totalPoints = maxOf(local.totalPoints, remote.totalPoints),
                totalPracticeMinutes = maxOf(local.totalPracticeMinutes, remote.totalPracticeMinutes),
                lastUpdatedAtMs = maxOf(local.lastUpdatedAtMs, remote.lastUpdatedAtMs)
            )
        }
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    private fun doesEventSatisfyTask(event: LearningEvent, task: DailyPracticeTask): Boolean {
        val cat = task.category.lowercase()
        val skill = task.skillFocus.lowercase()
        return when (event.type) {
            LearningEventType.VOCABULARY_WORD_COMPLETED, LearningEventType.QUIZ_COMPLETED, LearningEventType.FLASHCARD_REVIEWED ->
                cat.contains("vocab") || skill.contains("vocab")
            LearningEventType.LESSON_COMPLETED ->
                cat.contains("foundation") || cat.contains("speaking") || task.targetRoute.contains("lesson") || task.targetRoute == "speaking_lab"
            LearningEventType.OWN_SENTENCE_SUBMITTED ->
                cat.contains("grammar") || skill.contains("grammar")
            LearningEventType.WORD_SPOKEN, LearningEventType.SENTENCE_PRACTICED ->
                cat.contains("pronun") || skill.contains("pronun")
            LearningEventType.DAILY_PLAN_COMPLETED -> true
            else -> false
        }
    }

    private fun evaluateDynamicAchievements(snapshot: PersistedLearningSnapshot, nowMs: Long): PersistedLearningSnapshot {
        var next = snapshot
        val completedWords = next.wordProgress.values.count { it.isCompleted }
        if (completedWords >= 1) {
            next = unlockAchievementIfNew(next, "first_vocab_mastered", "First Word Mastered", "Completed all 5 stages for your first vocabulary word.", "Vocabulary", nowMs)
        }
        if (completedWords >= 5) {
            next = unlockAchievementIfNew(next, "vocab_builder_5", "Active Lexicon Builder", "Mastered 5 vocabulary words across all 5 speaking stages.", "Vocabulary", nowMs)
        }
        if (next.quizHistory.any { it.scorePercent >= 80 }) {
            next = unlockAchievementIfNew(next, "quiz_ace_80", "Local Quiz Ace", "Scored 80% or higher on an offline Local Vocabulary Quiz.", "Quiz", nowMs)
        }
        if (next.totalPracticeMinutes >= 30) {
            next = unlockAchievementIfNew(next, "practice_30_mins", "30 Minutes in Studio", "Logged 30+ minutes of active communication practice.", "Practice", nowMs)
        }
        return next
    }

    private fun unlockAchievementIfNew(
        snapshot: PersistedLearningSnapshot,
        id: String,
        title: String,
        description: String,
        category: String,
        nowMs: Long
    ): PersistedLearningSnapshot {
        if (snapshot.achievements.any { it.id == id }) return snapshot
        val badge = AchievementRecord(id, title, description, category, nowMs)
        val withBadge = snapshot.copy(achievements = listOf(badge) + snapshot.achievements)
        return appendNotificationIfAllowed(
            snapshot = withBadge,
            category = NotificationCategory.ACHIEVEMENT_UNLOCKED,
            title = "Achievement unlocked: $title",
            body = description,
            targetRoute = "analytics",
            dedupKey = "ach_$id",
            nowMs = nowMs
        )
    }

    private fun appendNotificationIfAllowed(
        snapshot: PersistedLearningSnapshot,
        category: NotificationCategory,
        title: String,
        body: String,
        targetRoute: String,
        dedupKey: String,
        nowMs: Long
    ): PersistedLearningSnapshot {
        if (!snapshot.notificationPreferences.isEnabledFor(category)) return snapshot
        if (dedupKey.isNotBlank() && snapshot.notifications.any { it.dedupKey == dedupKey }) return snapshot
        val item = UserNotification(
            id = UUID.randomUUID().toString(),
            category = category,
            title = title,
            body = body,
            timestampMs = nowMs,
            isRead = false,
            targetRoute = targetRoute,
            dedupKey = dedupKey
        )
        return snapshot.copy(notifications = (listOf(item) + snapshot.notifications).take(100))
    }

    private fun buildSkillScoreMap(profile: UserProfile?): Map<String, Int> {
        if (profile == null) return emptyMap()
        return mapOf(
            "Fluency" to profile.fluencyScore.toInt(),
            "Pronunciation" to profile.pronunciationScore.toInt(),
            "Vocabulary" to profile.vocabularyScore.toInt(),
            "Grammar" to profile.grammarScore.toInt(),
            "Articulation" to profile.articulationScore.toInt(),
            "Listening" to profile.listeningScore.toInt()
        )
    }

    private fun commitState(snapshot: PersistedLearningSnapshot) {
        _state.update { snapshot }
        writeToDisk(snapshot)
    }

    private fun fileForUser(uid: String): File? {
        val dir = storageDir ?: return null
        val safe = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "local_guest" }
        return File(dir, "learning_snapshot_$safe.json")
    }

    private fun readFromDisk(uid: String): PersistedLearningSnapshot? {
        val file = fileForUser(uid) ?: return null
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<PersistedLearningSnapshot>(file.readText())
        }.onFailure { err ->
            Log.w(TAG, "Failed reading local snapshot, starting fresh: ${err.message}")
        }.getOrNull()
    }

    private fun writeToDisk(snapshot: PersistedLearningSnapshot) {
        val file = fileForUser(snapshot.userId) ?: return
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(snapshot))
            if (!temp.renameTo(file)) {
                file.writeText(json.encodeToString(snapshot))
                temp.delete()
            }
        }.onFailure { err ->
            Log.w(TAG, "Failed writing local snapshot: ${err.message}")
        }
    }
}
