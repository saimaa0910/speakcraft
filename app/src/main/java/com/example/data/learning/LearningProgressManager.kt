package com.example.data.learning

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.LearningPathsEngine
import com.example.data.ai.local.language.LocalVocabStageEvaluation
import com.example.data.model.CurriculumCatalog
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabSeed
import com.example.data.model.VocabularyEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val TAG = "LearningProgressMgr"
private const val MAX_TASK_HISTORY = 200
private const val MAX_ACTIVITY_ATTEMPTS = 300
private const val MAX_SPEAKING_RECORDINGS = 120

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
 * - Every learning event (lesson, vocabulary stage 1-5, quiz, flashcard, daily task, speaking session,
 *   Activity Engine attempt, Coach Practice attempt) is deduplicated by [LearningEvent.idempotencyKey] so
 *   repeated taps or retries never double-count.
 * - Updates the daily task set, daily record, learning-path progress (activities, techniques, tasks), streak,
 *   achievements, vocabulary mastery, spaced repetition, and notifications atomically in one reactive
 *   [StateFlow<PersistedLearningSnapshot>].
 * - Daily task sets are completion-driven: finishing every task of the current set starts the next set right
 *   away ([DailySetRotation]); a new day starts a fresh set.
 * - Persists immediately to local JSON storage (`filesDir/learning_state/`) so 100% of learning
 *   state survives app restarts and offline usage without waiting on network.
 * - Provides deterministic conflict resolution [mergeSnapshots] for cloud synchronization.
 */
class LearningProgressManager(
    private val storageDir: File? = null,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val todayKeyProvider: () -> String = { StreakAndCompletionEngine.todayKey() },
    private val todayEpochDayProvider: () -> Long = { clockMs() / 86_400_000L },
    private val persistScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    constructor(context: Context) : this(
        storageDir = File(context.applicationContext.filesDir, "learning_state").apply { mkdirs() }
    )

    constructor(
        context: Context,
        persistScope: CoroutineScope?,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        storageDir = File(context.applicationContext.filesDir, "learning_state").apply { mkdirs() },
        persistScope = persistScope,
        ioDispatcher = ioDispatcher
    )

    private val ioLock = Any()
    @Volatile
    private var pendingPersistSnapshot: PersistedLearningSnapshot? = null
    @Volatile
    private var lastAsyncSyncSignature: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _state = MutableStateFlow(PersistedLearningSnapshot())
    val state: StateFlow<PersistedLearningSnapshot> = _state.asStateFlow()

    private val _dailyPackage = MutableStateFlow<DailyAdaptiveCurriculumPackage?>(null)

    /** Words, sentences, drills, technique and mission of the current daily set (null before loading). */
    val dailyPackage: StateFlow<DailyAdaptiveCurriculumPackage?> = _dailyPackage.asStateFlow()
    private var dailyPackageKey: List<Any?>? = null

    private var activeUserId: String = ""

    /**
     * Loads or initializes persisted learning state for [userId], synchronizes profile metadata,
     * refreshes today's daily task set & streak, and creates daily welcome notifications if enabled.
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
        val updated = buildRefreshedUserSnapshot(
            loaded = loaded,
            cleanUid = cleanUid,
            profile = profile,
            recentSessions = recentSessions
        )
        lastAsyncSyncSignature = buildSyncSignature(cleanUid, profile, recentSessions)
        commitState(updated)
    }

    /**
     * Background/IO-safe counterpart of [loadForUser] for Compose / UI callers.
     * Deduplicates repeated unchanged invocations so tab switches and profile updates
     * do not re-read or re-serialize JSON on the main thread.
     */
    suspend fun loadForUserAsync(
        userId: String,
        profile: UserProfile? = null,
        recentSessions: List<SpeakingSession> = emptyList()
    ) {
        val cleanUid = userId.trim().ifBlank { "local_guest" }
        val signature = buildSyncSignature(cleanUid, profile, recentSessions)
        if (lastAsyncSyncSignature == signature &&
            activeUserId == cleanUid &&
            _state.value.userId == cleanUid
        ) {
            return
        }

        val updated = withContext(ioDispatcher) {
            val loaded = synchronized(ioLock) {
                if (activeUserId == cleanUid && _state.value.userId == cleanUid) {
                    _state.value
                } else {
                    activeUserId = cleanUid
                    readFromDisk(cleanUid) ?: PersistedLearningSnapshot(userId = cleanUid)
                }
            }
            buildRefreshedUserSnapshot(
                loaded = loaded,
                cleanUid = cleanUid,
                profile = profile,
                recentSessions = recentSessions
            )
        }
        lastAsyncSyncSignature = signature
        commitState(updated, flushAsyncToDisk = true)
    }

    private fun buildSyncSignature(
        cleanUid: String,
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>
    ): String {
        val recentMistakeHash = recentSessions.take(5).flatMap { it.mistakes }.distinct().take(6).hashCode()
        return listOf(
            cleanUid,
            todayKeyProvider(),
            profile?.communicationLevel.orEmpty(),
            profile?.dailyGoalMinutes ?: -1L,
            profile?.primaryGoal.orEmpty(),
            profile?.fluencyScore?.toInt() ?: -1,
            profile?.pronunciationScore?.toInt() ?: -1,
            profile?.vocabularyScore?.toInt() ?: -1,
            recentSessions.size,
            recentMistakeHash
        ).joinToString("|")
    }

    private fun buildRefreshedUserSnapshot(
        loaded: PersistedLearningSnapshot,
        cleanUid: String,
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>
    ): PersistedLearningSnapshot {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val level = profile?.communicationLevel?.takeIf { it.isNotBlank() }
            ?.let { LearningLevel.fromLabel(it) } ?: loaded.currentLevel
        val targetMinutes = profile?.dailyGoalMinutes?.toInt()?.coerceIn(10, 90) ?: loaded.dailyTargetMinutes
        val goals = if (loaded.selectedGoals.isEmpty() && !profile?.primaryGoal.isNullOrBlank()) {
            listOf(LearningGoal.fromLabel(profile?.primaryGoal))
        } else loaded.selectedGoals.ifEmpty { listOf(LearningGoal.DAILY_FLUENCY, LearningGoal.CONFIDENCE) }

        val refreshedStreak = StreakAndCompletionEngine.refreshForDate(loaded.streak, todayKey)
        val todayEpoch = todayEpochDayProvider()

        val skillScores = buildSkillScoreMap(profile)
        val recurringMistakes = recentSessions.take(5).flatMap { it.mistakes }.distinct().take(6)
            .ifEmpty { loaded.recurringMistakeTags }
        val masteredSkills = skillScores.filterValues { it >= 90 }.keys + loaded.masteredSkillTags
        val dueWords = SpacedRepetitionScheduler.dueItems(loaded.spacedRepetition, todayEpoch).size

        var updated = ensureCurrentSet(
            snapshot = loaded.copy(
                userId = cleanUid,
                currentLevel = level,
                selectedGoals = goals,
                dailyTargetMinutes = targetMinutes,
                streak = refreshedStreak,
                recurringMistakeTags = recurringMistakes,
                masteredSkillTags = masteredSkills.toList(),
                lastUpdatedAtMs = now
            ),
            todayKey = todayKey,
            nowMs = now
        )
        updated = ensureDailyVocabularyState(updated, todayKey, todayEpoch)
        val plan = updated.dailyPlan

        val weeklyEligibility = VocabularyMasteryEngine.computeWeeklyEligibility(
            learnedLog = updated.learnedVocabularyLog,
            wordProgress = updated.wordProgress,
            weeklyAssessments = updated.weeklyAssessments,
            firstVocabEpochDay = updated.firstVocabularyEpochDay,
            vocabularyActiveDateKeys = updated.vocabularyActiveDateKeys,
            todayEpochDay = todayEpoch
        )
        if (weeklyEligibility.isDueNow) {
            updated = appendNotificationIfAllowed(
                snapshot = updated,
                category = NotificationCategory.WEEKLY_SUMMARY,
                title = "7-Day AI Vocabulary Quiz Ready!",
                body = "Speak and review the ${weeklyEligibility.learnedWordIdsInCycle.size} words you learnt over the past 7 days.",
                targetRoute = "vocabulary",
                dedupKey = "weekly_vocab_quiz_c${weeklyEligibility.cycleNumber}_$todayKey",
                nowMs = now
            )
        }

        // Generate helpful, deduplicated learning notifications (NEVER APK/update alerts!)
        if (plan != null) {
            updated = appendNotificationIfAllowed(
                snapshot = updated,
                category = NotificationCategory.DAILY_PRACTICE_REMINDER,
                title = if (plan.isRecoveryPlan) "Welcome back! Your gentle recovery set is ready" else "Today's daily practice set is ready",
                body = plan.adaptiveSummary,
                targetRoute = "daily_practice",
                dedupKey = "daily_plan_ready_$todayKey",
                nowMs = now
            )
        }

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
                body = "Complete 2 tasks in today's set to lock in Day ${refreshedStreak.currentStreakDays + 1}.",
                targetRoute = "daily_practice",
                dedupKey = "streak_reminder_$todayKey",
                nowMs = now
            )
        }
        return updated
    }

    /**
     * Updates learning preferences (level, target level, goals, daily duration, schedule, interests).
     * The current daily set is kept once any of its tasks is done; an untouched set is re-chosen for a new level.
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
        val nextGoals = goals?.takeIf { it.isNotEmpty() } ?: prev.selectedGoals
        val nextMinutes = dailyTargetMinutes?.coerceIn(10, 90) ?: prev.dailyTargetMinutes

        var next = prev.copy(
            currentLevel = nextLevel,
            targetLevel = targetLevel ?: prev.targetLevel,
            selectedGoals = nextGoals,
            dailyTargetMinutes = nextMinutes,
            preferredPracticeTime = preferredPracticeTime ?: prev.preferredPracticeTime,
            interests = interests ?: prev.interests,
            lastUpdatedAtMs = now
        )
        val untouched = prev.dailyPlan?.tasks?.none { it.isCompleted } ?: true
        if (untouched && nextLevel != prev.currentLevel) {
            // Re-choose the untouched set at the new level (same sequence position).
            next = next.copy(dailyRotation = next.dailyRotation.copy(items = null), dailyPlan = null)
        }
        next = ensureCurrentSet(next, todayKey, now)
        next = ensureDailyVocabularyState(next, todayKey, todayEpochDayProvider())
        commitState(next)
    }

    // -----------------------------------------------------------------------
    // Unified Idempotent Event Ingestion
    // -----------------------------------------------------------------------

    /**
     * Records a learning event idempotently. Duplicate [LearningEvent.idempotencyKey] values are ignored.
     * Automatically updates daily tasks (and starts the next set when the current one is finished), the daily
     * record, streak, achievements, points, practice minutes, and notifications.
     */
    fun recordEvent(event: LearningEvent): Boolean = recordEventWithRecords(event, defaultRecordsFor(event))

    private fun recordEventWithRecords(event: LearningEvent, records: List<DailyRecordEntry>): Boolean {
        val next = ingestEvent(_state.value, event, records)
        if (next == null) {
            // Already-counted event (e.g. a lesson passed again): keep the new record entries, count nothing twice.
            if (records.isNotEmpty()) appendRecords(records)
            return false
        }
        commitState(next)
        return true
    }

    /** Adds entries to the daily record without counting an event (idempotent by entry id). */
    private fun appendRecords(records: List<DailyRecordEntry>) {
        val current = _state.value
        val updated = DailyRecordBook.appendAll(current.dailyRecords, records)
        if (updated != current.dailyRecords) commitState(current.copy(dailyRecords = updated, lastUpdatedAtMs = clockMs()))
    }

    // -----------------------------------------------------------------------
    // Learning-path programs (lessons, checkpoints, day ticks)
    // -----------------------------------------------------------------------

    /**
     * Records one evaluated learning-path lesson attempt in today's record (score / pass as evaluated).
     * A passed lesson also counts once as a completed lesson (points, streak, daily tasks).
     */
    fun recordLearningPathLessonAttempt(
        programId: String,
        programTitle: String,
        lessonId: String,
        dayNumber: Int,
        lessonTitle: String,
        score: Int,
        passed: Boolean,
        attemptNumber: Int,
        minutesSpent: Int,
        skillTag: String
    ): Boolean {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val record = DailyRecordEntry(
            id = "lp_attempt_${lessonId}_${attemptNumber}_$now",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "lp:$programId:day$dayNumber",
            groupTitle = "$programTitle — Day $dayNumber: $lessonTitle",
            kind = DailyRecordKind.LESSON,
            stepId = "$lessonId#$attemptNumber",
            stepTitle = "Lesson attempt $attemptNumber evaluated",
            outcome = if (passed) DailyRecordOutcome.PASSED else DailyRecordOutcome.NOT_PASSED,
            score = score
        )
        if (!passed) {
            appendRecords(listOf(record))
            return false
        }
        return recordEventWithRecords(
            LearningEvent(
                idempotencyKey = "lp_$lessonId",
                type = LearningEventType.LESSON_COMPLETED,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = lessonId,
                title = lessonTitle,
                score = score,
                minutesSpent = minutesSpent,
                skillTag = skillTag
            ),
            listOf(record)
        )
    }

    /** Records one graded module checkpoint / capstone attempt; a pass counts once as a completed lesson. */
    fun recordLearningPathAssessmentAttempt(
        programId: String,
        programTitle: String,
        assessmentId: String,
        moduleId: String,
        title: String,
        isCapstone: Boolean,
        combinedScore: Int,
        passed: Boolean,
        minutesSpent: Int,
        skillTag: String
    ): Boolean {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val record = DailyRecordEntry(
            id = "lp_assess_attempt_${assessmentId}_$now",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "lp:$programId:assessment:$moduleId",
            groupTitle = "$programTitle — $title",
            kind = DailyRecordKind.LESSON,
            stepId = assessmentId,
            stepTitle = if (isCapstone) "Capstone attempt graded" else "Checkpoint attempt graded",
            outcome = if (passed) DailyRecordOutcome.PASSED else DailyRecordOutcome.NOT_PASSED,
            score = combinedScore
        )
        if (!passed) {
            appendRecords(listOf(record))
            return false
        }
        return recordEventWithRecords(
            LearningEvent(
                idempotencyKey = "lp_assess_$assessmentId",
                type = LearningEventType.LESSON_COMPLETED,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = assessmentId,
                title = title,
                score = combinedScore,
                minutesSpent = minutesSpent,
                skillTag = skillTag
            ),
            listOf(record)
        )
    }

    /** A learning-path day was ticked done by hand (self-reported, no score). */
    fun recordLearningPathDayMarked(programId: String, programTitle: String, dayNumber: Int, lessonTitle: String) {
        val todayKey = todayKeyProvider()
        appendRecords(
            listOf(
                DailyRecordEntry(
                    id = "lp_day_${programId}_${dayNumber}_$todayKey",
                    dateKey = todayKey,
                    timestampMs = clockMs(),
                    groupId = "lp:$programId:day$dayNumber",
                    groupTitle = "$programTitle — Day $dayNumber: $lessonTitle",
                    kind = DailyRecordKind.LESSON,
                    stepId = "day$dayNumber",
                    stepTitle = "Day marked done",
                    outcome = DailyRecordOutcome.SELF_REPORTED,
                    detail = "Marked done by you"
                )
            )
        )
    }

    /**
     * Marks a daily task done by the user (self-reported) and records it in today's record.
     * Completing the last open task of the set starts the next set immediately.
     */
    fun completeDailyTask(taskId: String) {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val current = ensureCurrentSet(_state.value, todayKey, now)
        val plan = current.dailyPlan ?: return
        val task = plan.tasks.firstOrNull { it.id == taskId } ?: return
        if (task.isCompleted) return

        val prevStreak = current.streak
        var next = completeTasks(current, listOf(task), now, todayKey, evidence = null)
        next = next.copy(
            totalPracticeMinutes = next.totalPracticeMinutes + task.durationMinutes,
            totalPoints = next.totalPoints + 15,
            lastUpdatedAtMs = now
        )
        next = applyStreak(prevStreak, next, todayKey, now)
        next = advanceSetIfComplete(next, todayKey, now)
        next = evaluateDynamicAchievements(next, now)
        commitState(next)
    }

    // -----------------------------------------------------------------------
    // Activity Engine, Coach Practice, routine, lessons & real-world tasks
    // -----------------------------------------------------------------------

    /**
     * Records one evaluated Activity Engine attempt: activity progress (activity + exercise pillars), the
     * attempt log, today's record (score / pass exactly as evaluated) and the matching daily task.
     * Returns false when [attemptId] was already recorded.
     */
    fun recordActivityAttempt(
        activity: ActivityDefinition,
        attempt: ActivityAttemptRecord,
        attemptId: String = attempt.id.ifBlank { "${activity.activityId}_${clockMs()}" }
    ): Boolean {
        val start = _state.value
        if (start.activityAttempts.any { it.id == attemptId }) return false
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val score = attempt.objectiveScore.toInt().coerceIn(0, 100)
        val prev = start.activityProgress[activity.activityId]
        val progress = LocalActivityProgress(
            activityId = activity.activityId,
            moduleId = activity.moduleId,
            skillCategory = activity.skillCategory.name,
            completed = (prev?.completed == true) || attempt.passed,
            attemptsCount = (prev?.attemptsCount ?: 0) + 1,
            bestObjectiveScore = maxOf(prev?.bestObjectiveScore ?: 0, score),
            latestObjectiveScore = score,
            latestConfidenceSelfRating = attempt.confidenceSelfRating.toInt().coerceIn(0, 100),
            totalPracticeSeconds = (prev?.totalPracticeSeconds ?: 0L) + attempt.durationSeconds.coerceAtLeast(0L),
            lastPracticedDateKey = attempt.dateKey.ifBlank { todayKey },
            updatedAtMs = now
        )
        val local = LocalActivityAttempt(
            id = attemptId,
            activityId = activity.activityId,
            moduleId = activity.moduleId,
            skillCategory = activity.skillCategory.name,
            exerciseType = activity.exerciseType.name,
            evaluatorName = attempt.evaluatorName,
            dateKey = attempt.dateKey.ifBlank { todayKey },
            timestampMs = now,
            durationSeconds = attempt.durationSeconds.coerceAtLeast(0L),
            objectiveScore = score,
            confidenceSelfRating = attempt.confidenceSelfRating.toInt().coerceIn(0, 100),
            passed = attempt.passed,
            aiEvaluated = attempt.aiEvaluated,
            wordsPerMinute = attempt.wordsPerMinute.toInt(),
            fillerCount = attempt.fillerCount.toInt(),
            recordedMediaPath = attempt.recordedMediaPath
        )
        val withProgress = start.copy(
            activityProgress = start.activityProgress + (activity.activityId to progress),
            activityAttempts = (listOf(local) + start.activityAttempts).take(MAX_ACTIVITY_ATTEMPTS)
        )
        val techniqueIds = LearningPathsEngine.activityToTechniques[activity.activityId].orEmpty()
        val record = DailyRecordEntry(
            id = "activity_$attemptId",
            dateKey = todayKey,
            timestampMs = now,
            groupId = activityGroupId(activity.activityId),
            groupTitle = activity.title,
            kind = DailyRecordKind.ACTIVITY,
            stepId = attemptId,
            stepTitle = "Attempt ${progress.attemptsCount} evaluated (${activity.exerciseType.displayName})",
            outcome = if (attempt.passed) DailyRecordOutcome.PASSED else DailyRecordOutcome.NOT_PASSED,
            score = score,
            detail = attempt.evaluatorName,
            techniqueIds = techniqueIds
        )
        val event = LearningEvent(
            idempotencyKey = "activity_attempt_$attemptId",
            type = LearningEventType.ACTIVITY_ATTEMPTED,
            timestampMs = now,
            localDateKey = todayKey,
            entityId = activity.activityId,
            title = activity.title,
            score = score,
            minutesSpent = (attempt.durationSeconds / 60L).toInt(),
            skillTag = activity.skillCategory.label,
            metadata = mapOf(
                "passed" to attempt.passed.toString(),
                "techniqueIds" to techniqueIds.joinToString(",")
            )
        )
        commitState(ingestEvent(withProgress, event, listOf(record)) ?: withProgress)
        return true
    }

    /**
     * Records one finished Coach Practice attempt: technique practice evidence (techniques pillar), today's
     * record (valid / inconclusive, strength, issues, score/pass only if the engine reported them) and, for a
     * valid attempt, the matching technique task. Returns false when the attempt was already recorded.
     */
    fun recordPracticeCompletion(completion: PracticeCompletion): Boolean {
        val start = _state.value
        val eventKey = "coach_attempt_${completion.attemptId}"
        if (eventKey in start.processedEventKeys) return false
        val todayKey = todayKeyProvider()
        val now = completion.timestampMs.takeIf { it > 0L } ?: clockMs()

        val updatedPractice = completion.techniqueIds.filter { it.isNotBlank() }.distinct().fold(start.techniquePractice) { acc, techId ->
            val prev = acc[techId] ?: TechniquePracticeStat(techniqueId = techId)
            acc + (techId to prev.copy(
                attempts = prev.attempts + 1,
                validAttempts = prev.validAttempts + if (completion.valid) 1 else 0,
                inconclusiveAttempts = prev.inconclusiveAttempts + if (completion.valid) 0 else 1,
                judgedAttempts = prev.judgedAttempts + if (completion.valid && completion.passed != null) 1 else 0,
                passedAttempts = prev.passedAttempts + if (completion.valid && completion.passed == true) 1 else 0,
                bestScore = if (completion.valid && completion.score != null) maxOf(prev.bestScore ?: 0, completion.score) else prev.bestScore,
                lastPracticedDateKey = todayKey,
                lastPracticedAtMs = now
            ))
        }
        val outcome = when {
            !completion.valid -> DailyRecordOutcome.INCONCLUSIVE
            completion.passed == true -> DailyRecordOutcome.PASSED
            completion.passed == false -> DailyRecordOutcome.NOT_PASSED
            else -> DailyRecordOutcome.EVALUATED
        }
        val detail = if (!completion.valid) {
            completion.inconclusiveReason.orEmpty()
        } else {
            listOfNotNull(
                completion.strengthCode?.let { "Strength: $it" },
                completion.issueCodes.takeIf { it.isNotEmpty() }?.let { "Focus: ${it.joinToString(", ")}" }
            ).joinToString(" • ")
        }
        val record = DailyRecordEntry(
            id = "coach_${completion.attemptId}",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "coach:${completion.activityId}",
            groupTitle = completion.title,
            kind = DailyRecordKind.COACH_PRACTICE,
            stepId = completion.attemptId,
            stepTitle = "Coach Practice attempt (${completion.templateId})",
            outcome = outcome,
            score = if (completion.valid) completion.score else null,
            detail = detail,
            techniqueIds = completion.techniqueIds,
            evaluationVersion = completion.evaluationVersion
        )
        val event = LearningEvent(
            idempotencyKey = eventKey,
            type = LearningEventType.TECHNIQUE_PRACTICED,
            timestampMs = now,
            localDateKey = todayKey,
            entityId = completion.activityId,
            title = completion.title,
            score = if (completion.valid) completion.score else null,
            skillTag = "Technique",
            metadata = mapOf(
                "valid" to completion.valid.toString(),
                "passed" to completion.passed?.toString().orEmpty(),
                "techniqueIds" to completion.techniqueIds.joinToString(","),
                "templateId" to completion.templateId,
                "evaluationVersion" to completion.evaluationVersion
            )
        )
        val savedRec = completion.localMediaPath.takeIf { it.isNotBlank() }?.let { mediaPath ->
            SavedPracticeRecording(
                id = completion.attemptId,
                activityId = completion.activityId,
                title = completion.title,
                templateId = completion.templateId,
                timestampMs = now,
                durationSeconds = completion.durationSeconds,
                localMediaPath = mediaPath,
                hasVideo = completion.hasVideo,
                valid = completion.valid,
                score = completion.score,
                passed = completion.passed,
                summary = completion.summary.ifBlank { detail },
                transcript = completion.transcript
            )
        }
        val withPractice = start.copy(
            techniquePractice = updatedPractice,
            practiceRecordings = if (savedRec != null) {
                (listOf(savedRec) + start.practiceRecordings.filterNot { it.id == savedRec.id }).take(100)
            } else {
                start.practiceRecordings
            }
        )
        commitState(ingestEvent(withPractice, event, listOf(record)) ?: withPractice)
        return true
    }

    /** Removes a Coach Practice recording reference from local state. */
    fun deletePracticeRecording(recordingId: String) {
        val current = _state.value
        val updated = current.practiceRecordings.filterNot { it.id == recordingId }
        if (updated.size != current.practiceRecordings.size) {
            commitState(current.copy(practiceRecordings = updated, lastUpdatedAtMs = clockMs()))
        }
    }

    /** Clears the local recorded media reference from an Activity Engine attempt. */
    fun deleteActivityAttemptMedia(attemptId: String) {
        val current = _state.value
        var changed = false
        val updated = current.activityAttempts.map { att ->
            if (att.id == attemptId && att.recordedMediaPath.isNotBlank()) {
                changed = true
                att.copy(recordedMediaPath = "")
            } else {
                att
            }
        }
        if (changed) {
            commitState(current.copy(activityAttempts = updated, lastUpdatedAtMs = clockMs()))
        }
    }

    /**
     * Archives or updates a Studio / Speaking Lab audio or video recording locally so it is always
     * playable under Profile -> Recordings, even offline or before/without AI analysis.
     */
    fun recordSpeakingRecording(recording: SavedSpeakingRecording): Boolean {
        if (recording.id.isBlank() && recording.localMediaPath.isBlank()) return false
        val current = _state.value
        val now = recording.timestampMs.takeIf { it > 0L } ?: clockMs()
        val existing = current.speakingRecordings.firstOrNull { item ->
            (recording.id.isNotBlank() && item.id == recording.id) ||
                (recording.localMediaPath.isNotBlank() && item.localMediaPath == recording.localMediaPath)
        }
        val merged = SavedSpeakingRecording(
            id = recording.id.ifBlank { existing?.id ?: "speak_${now}" },
            moduleType = recording.moduleType.ifBlank { existing?.moduleType ?: "SPEAKING_LAB" },
            promptTitle = recording.promptTitle.ifBlank { existing?.promptTitle.orEmpty() },
            timestampMs = existing?.timestampMs?.takeIf { it > 0L && recording.timestampMs <= 0L } ?: now,
            durationSeconds = maxOf(recording.durationSeconds, existing?.durationSeconds ?: 0),
            localMediaPath = recording.localMediaPath.ifBlank { existing?.localMediaPath.orEmpty() },
            mediaType = recording.mediaType.ifBlank { existing?.mediaType ?: "audio/mp4" },
            transcript = recording.transcript.ifBlank { existing?.transcript.orEmpty() },
            overallAssessment = recording.overallAssessment.ifBlank { existing?.overallAssessment.orEmpty() },
            evaluated = recording.evaluated || (existing?.evaluated == true),
            fluencyScore = if (recording.evaluated) recording.fluencyScore else maxOf(recording.fluencyScore, existing?.fluencyScore ?: 0),
            pronunciationScore = if (recording.evaluated) recording.pronunciationScore else maxOf(recording.pronunciationScore, existing?.pronunciationScore ?: 0),
            pronunciationAssessable = if (recording.evaluated) recording.pronunciationAssessable else (existing?.pronunciationAssessable == true),
            articulationScore = if (recording.evaluated) recording.articulationScore else maxOf(recording.articulationScore, existing?.articulationScore ?: 0),
            wordsPerMinute = if (recording.evaluated) recording.wordsPerMinute else maxOf(recording.wordsPerMinute, existing?.wordsPerMinute ?: 0)
        )
        val remaining = current.speakingRecordings.filterNot { item ->
            (merged.id.isNotBlank() && item.id == merged.id) ||
                (merged.localMediaPath.isNotBlank() && item.localMediaPath == merged.localMediaPath)
        }
        val updatedList = (listOf(merged) + remaining)
            .sortedByDescending { it.timestampMs }
            .take(MAX_SPEAKING_RECORDINGS)
        commitState(current.copy(speakingRecordings = updatedList, lastUpdatedAtMs = now))
        return true
    }

    /** Removes a Studio / Speaking Lab recording entry matching [idOrPath] from local state. */
    fun deleteSpeakingRecording(idOrPath: String): Boolean {
        if (idOrPath.isBlank()) return false
        val current = _state.value
        val updated = current.speakingRecordings.filterNot { item ->
            item.id == idOrPath || (item.localMediaPath.isNotBlank() && item.localMediaPath == idOrPath)
        }
        if (updated.size == current.speakingRecordings.size) return false
        commitState(current.copy(speakingRecordings = updated, lastUpdatedAtMs = clockMs()))
        return true
    }

    /** Clears locally archived Studio / Speaking Lab recordings. */
    fun clearSpeakingRecordings() {
        val current = _state.value
        if (current.speakingRecordings.isNotEmpty()) {
            commitState(current.copy(speakingRecordings = emptyList(), lastUpdatedAtMs = clockMs()))
        }
    }

    /** A step of the daily routine checklist was ticked (self-reported; once per step per day). */
    fun recordDailyRoutineStep(stepId: String, title: String): Boolean {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val record = DailyRecordEntry(
            id = "routine_${todayKey}_$stepId",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "daily_routine",
            groupTitle = "Daily routine checklist",
            kind = DailyRecordKind.DAILY_ROUTINE,
            stepId = stepId,
            stepTitle = title,
            outcome = DailyRecordOutcome.SELF_REPORTED
        )
        return recordEventWithRecords(
            LearningEvent(
                idempotencyKey = "routine_${todayKey}_$stepId",
                type = LearningEventType.ROUTINE_STEP_COMPLETED,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = stepId,
                title = title,
                skillTag = "Routine"
            ),
            listOf(record)
        )
    }

    /** A lesson was marked completed (once per lesson per day). [score] only when the lesson recorded one. */
    fun recordLessonCompleted(lessonId: String, title: String, score: Int? = null): Boolean {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val record = DailyRecordEntry(
            id = "lesson_${todayKey}_$lessonId",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "lesson:$lessonId",
            groupTitle = title.ifBlank { lessonId },
            kind = DailyRecordKind.LESSON,
            stepId = lessonId,
            stepTitle = "Lesson completed",
            outcome = DailyRecordOutcome.COMPLETED,
            score = score
        )
        return recordEventWithRecords(
            LearningEvent(
                idempotencyKey = "lesson_done_${lessonId}_$todayKey",
                type = LearningEventType.LESSON_COMPLETED,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = lessonId,
                title = title,
                score = score,
                minutesSpent = 5,
                skillTag = "Lesson"
            ),
            listOf(record)
        )
    }

    /** A real-world task was completed (self-reported); it also completes today's mission task. */
    fun recordRealWorldTask(taskId: String, title: String): Boolean {
        val todayKey = todayKeyProvider()
        val now = clockMs()
        val record = DailyRecordEntry(
            id = "realworld_${todayKey}_$taskId",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "realworld:$taskId",
            groupTitle = title.ifBlank { taskId },
            kind = DailyRecordKind.REAL_WORLD,
            stepId = taskId,
            stepTitle = "Real-world task completed",
            outcome = DailyRecordOutcome.SELF_REPORTED
        )
        return recordEventWithRecords(
            LearningEvent(
                idempotencyKey = "realworld_${taskId}_$todayKey",
                type = LearningEventType.REAL_WORLD_TASK_COMPLETED,
                timestampMs = now,
                localDateKey = todayKey,
                entityId = taskId,
                title = title,
                skillTag = "Mission"
            ),
            listOf(record)
        )
    }

    /** Ensures today's 3 level-calibrated non-repeating words are active for [level]. */
    fun ensureDailyVocabulary(level: LearningLevel = _state.value.currentLevel) {
        val todayKey = todayKeyProvider()
        val todayEpoch = todayEpochDayProvider()
        val current = _state.value
        val withLevel = if (current.currentLevel != level) current.copy(currentLevel = level) else current
        val updated = ensureDailyVocabularyState(withLevel, todayKey, todayEpoch)
        if (updated != current) {
            commitState(updated.copy(lastUpdatedAtMs = clockMs()))
        }
    }

    /**
     * Evaluates a user's spoken attempt for [seed] in Vocabulary Builder.
     * ONLY marks the word Done when the spoken assessment passes!
     */
    fun recordVocabularyWordSpokenAssessment(
        seed: VocabSeed,
        recognizedText: String,
        alternatives: List<String> = emptyList(),
        asrConfidence: Float = 0.85f,
        consecutiveFailures: Int = 0
    ): LocalVocabStageEvaluation {
        val eval = VocabularyMasteryEngine.evaluateSpokenWordAttempt(
            seed = seed,
            transcript = recognizedText,
            alternatives = alternatives,
            asrConfidence = asrConfidence,
            consecutiveFailures = consecutiveFailures
        )
        val todayKey = todayKeyProvider()
        val todayEpoch = todayEpochDayProvider()
        val now = clockMs()
        val base = ensureDailyVocabularyState(_state.value, todayKey, todayEpoch)

        val currentAssign = base.dailyVocabulary ?: DailyVocabularyAssignment(
            dateKey = todayKey,
            level = base.currentLevel,
            wordIds = listOf(seed.id)
        )
        val updatedAssign = currentAssign.copy(
            completedWordIds = if (eval.passed) currentAssign.completedWordIds + seed.id else currentAssign.completedWordIds,
            wordScores = currentAssign.wordScores + (seed.id to eval.score),
            lastTranscripts = if (eval.recognizedText.isNotBlank()) {
                currentAssign.lastTranscripts + (seed.id to eval.recognizedText)
            } else {
                currentAssign.lastTranscripts
            }
        )

        val prevWp = base.wordProgress[seed.id] ?: VocabularyWordProgress(wordId = seed.id, word = seed.word)
        val updatedWp = if (eval.passed) {
            prevWp.copy(
                highestStageCompleted = maxOf(prevWp.highestStageCompleted, 2),
                stage1Completed = true,
                stage2Passed = true,
                stage2BestScore = maxOf(prevWp.stage2BestScore, eval.score),
                isCompleted = true,
                completedAtMs = prevWp.completedAtMs.takeIf { it > 0L } ?: now,
                totalAttempts = prevWp.totalAttempts + 1,
                pointsEarned = prevWp.pointsEarned + 15,
                updatedAtMs = now
            )
        } else {
            prevWp.copy(
                stage1Completed = true,
                totalAttempts = prevWp.totalAttempts + 1,
                updatedAtMs = now
            )
        }

        val updatedLog = if (eval.passed) {
            val newRecord = LearnedWordRecord(
                wordId = seed.id,
                word = seed.word,
                level = seed.level.ifBlank { base.currentLevel.label },
                learnedDateKey = todayKey,
                learnedEpochDay = todayEpoch,
                spokenAssessmentScore = eval.score,
                spokenTranscript = eval.recognizedText,
                markedDoneAtMs = now
            )
            listOf(newRecord) + base.learnedVocabularyLog.filterNot { it.wordId == seed.id }
        } else {
            base.learnedVocabularyLog
        }

        val updatedSrs = if (eval.passed || seed.id !in base.spacedRepetition) {
            val srsItem = SpacedRepetitionScheduler.scheduleNext(
                wordId = seed.id,
                word = seed.word,
                current = base.spacedRepetition[seed.id],
                known = eval.passed,
                todayEpochDay = todayEpoch,
                nowMs = now
            )
            base.spacedRepetition + (seed.id to srsItem)
        } else {
            base.spacedRepetition
        }

        val stateWithVocab = base.copy(
            dailyVocabulary = updatedAssign,
            learnedVocabularyLog = updatedLog,
            wordProgress = base.wordProgress + (seed.id to updatedWp),
            spacedRepetition = updatedSrs,
            vocabularyActiveDateKeys = (base.vocabularyActiveDateKeys + todayKey).distinct().takeLast(90),
            firstVocabularyEpochDay = base.firstVocabularyEpochDay.takeIf { it > 0L } ?: todayEpoch,
            lastUpdatedAtMs = now
        )

        val recordOutcome = when {
            eval.passed -> DailyRecordOutcome.PASSED
            eval.isInconclusive -> DailyRecordOutcome.INCONCLUSIVE
            else -> DailyRecordOutcome.NOT_PASSED
        }
        val record = DailyRecordEntry(
            id = "vocab_speak_${seed.id}_$now",
            dateKey = todayKey,
            timestampMs = now,
            groupId = vocabGroupId(seed.id),
            groupTitle = "Vocabulary: ${seed.word}",
            kind = DailyRecordKind.VOCABULARY,
            stepId = "speak_$now",
            stepTitle = "Spoken word assessment",
            outcome = recordOutcome,
            score = if (eval.isInconclusive) null else eval.score,
            detail = eval.recognizedText
        )

        if (!eval.passed) {
            commitState(stateWithVocab.copy(dailyRecords = DailyRecordBook.append(stateWithVocab.dailyRecords, record)))
            return eval
        }

        val allDailyWordsDone = updatedAssign.wordIds.isNotEmpty() && updatedAssign.allDone
        val eventType = if (allDailyWordsDone) LearningEventType.VOCABULARY_WORD_COMPLETED else LearningEventType.WORD_SPOKEN
        val eventKey = if (allDailyWordsDone) "vocab_daily_done_${todayKey}_${seed.id}" else "vocab_spoken_${seed.id}_${updatedWp.totalAttempts}"
        val event = LearningEvent(
            idempotencyKey = eventKey,
            type = eventType,
            timestampMs = now,
            localDateKey = todayKey,
            entityId = seed.id,
            title = seed.word,
            score = eval.score,
            minutesSpent = 1,
            skillTag = "Vocabulary",
            metadata = mapOf("passed" to "true", "stage" to "Speak Word Assessment")
        )
        val afterEvent = ingestEvent(stateWithVocab, event, listOf(record)) ?: stateWithVocab
        commitState(afterEvent)
        return eval
    }

    fun getWeeklyAssessmentEligibility(
        extraEntries: List<VocabularyEntry> = emptyList()
    ): WeeklyAssessmentEligibility {
        val current = _state.value
        return VocabularyMasteryEngine.computeWeeklyEligibility(
            learnedLog = current.learnedVocabularyLog,
            wordProgress = current.wordProgress,
            weeklyAssessments = current.weeklyAssessments,
            firstVocabEpochDay = current.firstVocabularyEpochDay,
            vocabularyActiveDateKeys = current.vocabularyActiveDateKeys,
            todayEpochDay = todayEpochDayProvider(),
            fallbackEntries = extraEntries
        )
    }

    fun recordWeeklyVocabularyAssessment(
        result: WeeklyVocabularyAssessmentRecord,
        wordOutcomes: Map<String, Boolean>
    ) {
        val current = _state.value
        val now = clockMs()
        val todayKey = todayKeyProvider()
        val todayEpoch = todayEpochDayProvider()

        var updatedSrs = current.spacedRepetition
        wordOutcomes.forEach { (wordId, passed) ->
            val wordName = current.learnedVocabularyLog.firstOrNull { it.wordId == wordId }?.word
                ?: CurriculumCatalog.vocabularyBank.firstOrNull { it.id == wordId }?.word
                ?: wordId
            updatedSrs = updatedSrs + (
                wordId to SpacedRepetitionScheduler.scheduleNext(
                    wordId = wordId,
                    word = wordName,
                    current = updatedSrs[wordId],
                    known = passed,
                    todayEpochDay = todayEpoch,
                    nowMs = now
                )
            )
        }

        val finalizedResult = result.copy(
            completedDateKey = result.completedDateKey.ifBlank { todayKey },
            completedEpochDay = result.completedEpochDay.takeIf { it > 0L } ?: todayEpoch,
            completedAtMs = result.completedAtMs.takeIf { it > 0L } ?: now
        )

        var next = current.copy(
            weeklyAssessments = (listOf(finalizedResult) + current.weeklyAssessments).take(52),
            spacedRepetition = updatedSrs,
            lastUpdatedAtMs = now
        )
        if (finalizedResult.scorePercent >= 60) {
            next = unlockAchievementIfNew(
                snapshot = next,
                id = "weekly_vocab_7day",
                title = "7-Day Lexicon Master",
                description = "Completed a 7-day cumulative spoken vocabulary assessment in AI Quiz.",
                category = "Vocabulary",
                nowMs = now
            )
        }

        val record = DailyRecordEntry(
            id = "weekly_quiz_${finalizedResult.id}",
            dateKey = todayKey,
            timestampMs = now,
            groupId = "quiz:weekly_${finalizedResult.cycleNumber}",
            groupTitle = "7-Day Spoken AI Quiz (Cycle ${finalizedResult.cycleNumber})",
            kind = DailyRecordKind.QUIZ,
            stepId = finalizedResult.id,
            stepTitle = "Weekly assessment (${finalizedResult.passedWordIds.size}/${finalizedResult.assessedWordIds.size} words)",
            outcome = if (finalizedResult.scorePercent >= 60) DailyRecordOutcome.PASSED else DailyRecordOutcome.COMPLETED,
            score = finalizedResult.scorePercent
        )
        val event = LearningEvent(
            idempotencyKey = "weekly_vocab_quiz_${finalizedResult.id}",
            type = LearningEventType.QUIZ_COMPLETED,
            timestampMs = now,
            localDateKey = todayKey,
            entityId = finalizedResult.id,
            title = "7-Day AI Vocabulary Quiz (${finalizedResult.scorePercent}%)",
            score = finalizedResult.scorePercent,
            minutesSpent = 5,
            skillTag = "Vocabulary"
        )
        commitState(ingestEvent(next, event, listOf(record)) ?: next)
    }

    /** A vocabulary sentence-usage check finished; result and score exactly as the evaluator returned them. */
    fun recordVocabularyUsageCheck(wordId: String, word: String, usedCorrectly: Boolean, score: Int?): Boolean {
        val todayKey = todayKeyProvider()
        val todayEpoch = todayEpochDayProvider()
        val now = clockMs()
        val current = _state.value
        val stateWithLog = if (usedCorrectly) {
            val existingSeed = CurriculumCatalog.vocabularyBank.firstOrNull { it.id == wordId }
            val recordItem = LearnedWordRecord(
                wordId = wordId,
                word = word,
                level = existingSeed?.level ?: current.currentLevel.label,
                learnedDateKey = todayKey,
                learnedEpochDay = todayEpoch,
                spokenAssessmentScore = score ?: 80,
                markedDoneAtMs = now
            )
            current.copy(
                learnedVocabularyLog = listOf(recordItem) + current.learnedVocabularyLog.filterNot { it.wordId == wordId },
                vocabularyActiveDateKeys = (current.vocabularyActiveDateKeys + todayKey).distinct().takeLast(90),
                firstVocabularyEpochDay = current.firstVocabularyEpochDay.takeIf { it > 0L } ?: todayEpoch
            )
        } else current
        val record = DailyRecordEntry(
            id = "vocab_usage_${wordId}_$now",
            dateKey = todayKey,
            timestampMs = now,
            groupId = vocabGroupId(wordId),
            groupTitle = "Vocabulary: $word",
            kind = DailyRecordKind.VOCABULARY,
            stepId = "usage_$now",
            stepTitle = "Sentence usage check",
            outcome = if (usedCorrectly) DailyRecordOutcome.PASSED else DailyRecordOutcome.NOT_PASSED,
            score = score
        )
        val event = LearningEvent(
            idempotencyKey = "vocab_usage_${wordId}_$now",
            type = LearningEventType.OWN_SENTENCE_SUBMITTED,
            timestampMs = now,
            localDateKey = todayKey,
            entityId = wordId,
            title = word,
            score = score,
            minutesSpent = 1,
            skillTag = "Vocabulary",
            metadata = mapOf("passed" to usedCorrectly.toString())
        )
        val next = ingestEvent(stateWithLog, event, listOf(record))
        if (next != null) {
            commitState(next)
            return true
        }
        if (stateWithLog != current) {
            commitState(stateWithLog)
        }
        return false
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
                skillTag = "Vocabulary",
                metadata = mapOf("passed" to passed.toString(), "stage" to stage.label)
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
                skillTag = "Vocabulary",
                metadata = mapOf("known" to known.toString())
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
            next = next.copy(
                dailyPlan = null,
                eventLog = emptyList(),
                processedEventKeys = emptySet(),
                dailyRecords = emptyMap(),
                completedDailyTasks = emptyList(),
                dailyRotation = DailyRotationState()
            )
        }
        if (options.clearVocabularyProgress) {
            next = next.copy(
                wordProgress = emptyMap(),
                spacedRepetition = emptyMap(),
                shownVocabularyWordIds = emptyList(),
                dailyVocabulary = null,
                learnedVocabularyLog = emptyList(),
                vocabularyActiveDateKeys = emptyList(),
                firstVocabularyEpochDay = 0L
            )
        }
        if (options.clearNotebook) {
            next = next.copy(notebook = emptyMap())
        }
        if (options.clearQuizHistory) {
            next = next.copy(quizHistory = emptyList(), weeklyAssessments = emptyList())
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
            _dailyPackage.value = null
            dailyPackageKey = null
            activeUserId = ""
        }
    }

    companion object {
        /**
         * Deterministic conflict resolution for merging local offline state with synced cloud state:
         * - Completions, vocabulary stages, achievements, processed event keys, completed dates, daily records,
         *   activity / technique progress take union/max (monotonic: progress is never lost).
         * - Longest streak takes max; active streak is recomputed from union of completed dates.
         * - Preferences (level, goals, daily minutes, notification preferences) take the latest timestamp.
         * - The daily-set position takes the furthest sequence position.
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

            val mergedRecords = DailyRecordBook.appendAll(
                local.dailyRecords,
                remote.dailyRecords.values.flatten()
            )

            val mergedActivityProgress = (local.activityProgress.keys + remote.activityProgress.keys).associateWith { key ->
                val a = local.activityProgress[key]
                val b = remote.activityProgress[key]
                when {
                    a == null -> b!!
                    b == null -> a
                    else -> {
                        val newer = if (a.updatedAtMs >= b.updatedAtMs) a else b
                        newer.copy(
                            completed = a.completed || b.completed,
                            attemptsCount = maxOf(a.attemptsCount, b.attemptsCount),
                            bestObjectiveScore = maxOf(a.bestObjectiveScore, b.bestObjectiveScore),
                            totalPracticeSeconds = maxOf(a.totalPracticeSeconds, b.totalPracticeSeconds)
                        )
                    }
                }
            }

            val mergedPractice = (local.techniquePractice.keys + remote.techniquePractice.keys).associateWith { key ->
                val a = local.techniquePractice[key]
                val b = remote.techniquePractice[key]
                when {
                    a == null -> b!!
                    b == null -> a
                    else -> TechniquePracticeStat(
                        techniqueId = key,
                        attempts = maxOf(a.attempts, b.attempts),
                        validAttempts = maxOf(a.validAttempts, b.validAttempts),
                        inconclusiveAttempts = maxOf(a.inconclusiveAttempts, b.inconclusiveAttempts),
                        judgedAttempts = maxOf(a.judgedAttempts, b.judgedAttempts),
                        passedAttempts = maxOf(a.passedAttempts, b.passedAttempts),
                        bestScore = listOfNotNull(a.bestScore, b.bestScore).maxOrNull(),
                        lastPracticedDateKey = maxOf(a.lastPracticedDateKey, b.lastPracticedDateKey),
                        lastPracticedAtMs = maxOf(a.lastPracticedAtMs, b.lastPracticedAtMs)
                    )
                }
            }

            val furthestRotation = if (local.dailyRotation.sequencePosition >= remote.dailyRotation.sequencePosition) {
                local.dailyRotation
            } else remote.dailyRotation

            return (if (preferLocalPrefs) local else remote).copy(
                streak = mergedStreak,
                wordProgress = mergedWords,
                notebook = mergedNotebook,
                achievements = mergedAchievements,
                processedEventKeys = local.processedEventKeys + remote.processedEventKeys,
                eventLog = mergedEvents,
                totalPoints = maxOf(local.totalPoints, remote.totalPoints),
                totalPracticeMinutes = maxOf(local.totalPracticeMinutes, remote.totalPracticeMinutes),
                lastUpdatedAtMs = maxOf(local.lastUpdatedAtMs, remote.lastUpdatedAtMs),
                dailyRecords = mergedRecords,
                completedDailyTasks = (local.completedDailyTasks + remote.completedDailyTasks)
                    .distinctBy { it.id }
                    .sortedByDescending { it.completedAtMs }
                    .take(MAX_TASK_HISTORY),
                activityProgress = mergedActivityProgress,
                activityAttempts = (local.activityAttempts + remote.activityAttempts)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestampMs }
                    .take(MAX_ACTIVITY_ATTEMPTS),
                techniquePractice = mergedPractice,
                dailyRotation = furthestRotation.copy(
                    recentItemIds = DailySetRotation.withRecent(remote.dailyRotation.recentItemIds, local.dailyRotation.recentItemIds)
                ),
                dailyPlan = if (furthestRotation === local.dailyRotation) local.dailyPlan else remote.dailyPlan,
                firstVocabularyEpochDay = listOf(local.firstVocabularyEpochDay, remote.firstVocabularyEpochDay).filter { it > 0L }.minOrNull() ?: 0L,
                vocabularyActiveDateKeys = (local.vocabularyActiveDateKeys + remote.vocabularyActiveDateKeys).distinct().sorted().takeLast(90),
                shownVocabularyWordIds = (local.shownVocabularyWordIds + remote.shownVocabularyWordIds).distinct(),
                dailyVocabulary = if ((local.dailyVocabulary?.dateKey.orEmpty()) >= (remote.dailyVocabulary?.dateKey.orEmpty())) {
                    local.dailyVocabulary ?: remote.dailyVocabulary
                } else {
                    remote.dailyVocabulary
                },
                learnedVocabularyLog = (local.learnedVocabularyLog + remote.learnedVocabularyLog)
                    .sortedByDescending { it.markedDoneAtMs }
                    .distinctBy { it.wordId },
                weeklyAssessments = (local.weeklyAssessments + remote.weeklyAssessments)
                    .distinctBy { it.id }
                    .sortedByDescending { it.completedAtMs },
                practiceRecordings = (local.practiceRecordings + remote.practiceRecordings)
                    .sortedByDescending { it.timestampMs }
                    .distinctBy { it.id }
                    .take(100),
                speakingRecordings = (local.speakingRecordings + remote.speakingRecordings)
                    .sortedByDescending { it.timestampMs }
                    .distinctBy { it.id }
                    .take(MAX_SPEAKING_RECORDINGS)
            )
        }
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    /**
     * Applies one event: completes the daily tasks it satisfies, appends [records] to today's record, updates
     * points, minutes, streak and notifications, and starts the next set once the current one is finished.
     * Returns null when the event was already processed.
     */
    private fun ingestEvent(
        start: PersistedLearningSnapshot,
        event: LearningEvent,
        records: List<DailyRecordEntry>
    ): PersistedLearningSnapshot? {
        if (event.idempotencyKey in start.processedEventKeys) return null

        val todayKey = event.localDateKey.ifBlank { todayKeyProvider() }
        // The daily set always follows the device's today, whatever date an event carries.
        val setDay = todayKeyProvider()
        val now = if (event.timestampMs > 0L) event.timestampMs else clockMs()
        val pointsDelta = pointsFor(event)

        val current = ensureCurrentSet(start, setDay, now)
        val satisfied = current.dailyPlan?.tasks.orEmpty().filter { !it.isCompleted && doesEventSatisfyTask(event, it) }
        var next = completeTasks(current, satisfied, now, setDay, evidence = event.title.ifBlank { null })
        next = next.copy(
            processedEventKeys = next.processedEventKeys + event.idempotencyKey,
            eventLog = (listOf(event) + next.eventLog).take(300),
            totalPoints = next.totalPoints + pointsDelta,
            totalPracticeMinutes = next.totalPracticeMinutes + event.minutesSpent.coerceAtLeast(0),
            dailyRecords = DailyRecordBook.appendAll(next.dailyRecords, records),
            lastUpdatedAtMs = now
        )
        next = applyStreak(start.streak, next, todayKey, now)

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

        next = advanceSetIfComplete(next, setDay, now)
        return evaluateDynamicAchievements(next, now)
    }

    private fun pointsFor(event: LearningEvent): Int = when (event.type) {
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
        LearningEventType.ACTIVITY_ATTEMPTED -> if (event.metadata["passed"] == "true") 30 else 20
        LearningEventType.TECHNIQUE_PRACTICED -> if (event.metadata["valid"] == "true") 15 else 0
        LearningEventType.DAILY_SET_COMPLETED -> 40
        LearningEventType.ROUTINE_STEP_COMPLETED -> 5
        LearningEventType.REAL_WORLD_TASK_COMPLETED -> 20
    }

    /**
     * Makes sure the snapshot holds today's daily set and its task checklist (new day -> fresh set; same day ->
     * the stored set, rebuilt from its item ids). Completed tasks of a replaced plan are kept in the history.
     */
    private fun ensureCurrentSet(
        snapshot: PersistedLearningSnapshot,
        todayKey: String,
        nowMs: Long
    ): PersistedLearningSnapshot {
        val (rotation, pkg) = DailySetRotation.ensureSetForDate(
            rotation = snapshot.dailyRotation,
            todayKey = todayKey,
            level = snapshot.currentLevel,
            targetMinutes = snapshot.dailyTargetMinutes
        )
        val existingPlan = snapshot.dailyPlan
        val planMatches = existingPlan != null &&
            existingPlan.dateKey == todayKey &&
            existingPlan.tasks.map { it.id } == pkg.dailyTasks.map { it.id }
        if (planMatches && rotation == snapshot.dailyRotation) return snapshot

        val plan = if (planMatches) existingPlan!! else DailySetRotation.planFrom(
            pkg = pkg,
            goals = snapshot.selectedGoals,
            targetMinutes = snapshot.dailyTargetMinutes,
            missedDaysBeforeToday = StreakAndCompletionEngine.missedDaysCount(snapshot.streak, todayKey),
            nowMs = nowMs
        )
        val carriedOver = if (planMatches) emptyList() else existingPlan?.tasks?.filter { it.isCompleted }.orEmpty()
        return snapshot.copy(
            dailyRotation = rotation,
            dailyPlan = plan,
            completedDailyTasks = (snapshot.completedDailyTasks + carriedOver)
                .distinctBy { it.id }
                .sortedByDescending { it.completedAtMs }
                .take(MAX_TASK_HISTORY)
        )
    }

    /**
     * Marks [tasks] of the current plan done, records each in today's record (self-reported when [evidence] is
     * null, otherwise completed by that evidence), keeps them in the task history and marks their content items
     * as recently completed (avoided by the next sets).
     */
    private fun completeTasks(
        snapshot: PersistedLearningSnapshot,
        tasks: List<DailyPracticeTask>,
        nowMs: Long,
        todayKey: String,
        evidence: String?
    ): PersistedLearningSnapshot {
        val plan = snapshot.dailyPlan ?: return snapshot
        val ids = tasks.filter { !it.isCompleted }.map { it.id }.toSet()
        if (ids.isEmpty()) return snapshot
        val updatedTasks = plan.tasks.map { if (it.id in ids) it.copy(isCompleted = true, completedAtMs = nowMs) else it }
        val newlyDone = updatedTasks.filter { it.id in ids }
        val records = newlyDone.map { taskRecord(it, todayKey, nowMs, evidence) }
        return snapshot.copy(
            dailyPlan = plan.copy(tasks = updatedTasks),
            completedDailyTasks = (newlyDone + snapshot.completedDailyTasks).distinctBy { it.id }.take(MAX_TASK_HISTORY),
            dailyRotation = snapshot.dailyRotation.copy(
                recentItemIds = DailySetRotation.withRecent(snapshot.dailyRotation.recentItemIds, newlyDone.flatMap { it.itemIds })
            ),
            dailyRecords = DailyRecordBook.appendAll(snapshot.dailyRecords, records)
        )
    }

    private fun taskRecord(task: DailyPracticeTask, todayKey: String, nowMs: Long, evidence: String?): DailyRecordEntry {
        val (groupId, groupTitle) = when {
            task.linkedActivityId.isNotBlank() -> activityGroupId(task.linkedActivityId) to
                (ActivityCatalogue.getActivityById(task.linkedActivityId)?.title ?: task.title.removePrefix("Drill: "))
            task.linkedTechniqueIds.isNotEmpty() -> {
                val techId = task.linkedTechniqueIds.first()
                "technique:$techId" to "Technique: ${LearningPathsEngine.techniquesCatalog[techId]?.name ?: techId}"
            }
            task.category.equals("Vocabulary", true) -> "daily_vocabulary" to "Daily vocabulary"
            task.category.equals("Speaking", true) -> "daily_sentences" to "Spoken sentences"
            task.category.equals("Mission", true) -> "mission:${task.itemIds.firstOrNull() ?: task.id}" to task.title
            else -> "daily_task:${task.category.lowercase()}" to task.title
        }
        return DailyRecordEntry(
            id = "task_${task.id}",
            dateKey = todayKey,
            timestampMs = nowMs,
            groupId = groupId,
            groupTitle = groupTitle,
            kind = DailyRecordKind.DAILY_TASK,
            stepId = task.id,
            stepTitle = "Daily task: ${task.title}",
            outcome = if (evidence == null) DailyRecordOutcome.SELF_REPORTED else DailyRecordOutcome.COMPLETED,
            detail = if (evidence == null) "Marked done by you" else "Completed by: $evidence",
            techniqueIds = task.linkedTechniqueIds
        )
    }

    private fun applyStreak(
        prevStreak: LearningStreakState,
        snapshot: PersistedLearningSnapshot,
        todayKey: String,
        nowMs: Long
    ): PersistedLearningSnapshot {
        val newStreak = StreakAndCompletionEngine.recordDailyCompletion(
            current = snapshot.streak,
            todayDateKey = todayKey,
            requirementMet = snapshot.dailyPlan?.isCompleteForStreak == true
        )
        var next = snapshot.copy(streak = newStreak)

        // Emit notification when daily requirement newly met or milestone reached
        if (newStreak.todayRequirementMet && !prevStreak.todayRequirementMet) {
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.STREAK_ALERT,
                title = "Daily goal complete — ${newStreak.currentStreakDays}-day streak!",
                body = "Awesome consistency! You completed today's practice requirement.",
                targetRoute = "dashboard",
                dedupKey = "streak_completed_$todayKey",
                nowMs = nowMs
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
                nowMs = nowMs
            )
            next = appendNotificationIfAllowed(
                snapshot = next,
                category = NotificationCategory.STREAK_MILESTONE,
                title = "Milestone unlocked: $milestone-Day Streak!",
                body = "Your daily communication habit reached $milestone days in a row.",
                targetRoute = "analytics",
                dedupKey = "milestone_$milestone",
                nowMs = nowMs
            )
        }
        return next
    }

    /** When every task of today's set is done: record it, advance the sequence and start the next set. */
    private fun advanceSetIfComplete(
        snapshot: PersistedLearningSnapshot,
        todayKey: String,
        nowMs: Long
    ): PersistedLearningSnapshot {
        val plan = snapshot.dailyPlan ?: return snapshot
        if (plan.dateKey != todayKey || plan.tasks.isEmpty() || plan.tasks.any { !it.isCompleted }) return snapshot

        val finished = snapshot.dailyRotation
        val finishedSetKey = DailyAdaptiveCurriculumEngine.setKeyFor(plan.dateKey, finished.setIndexToday)
        val finishedNumber = finished.setIndexToday + 1
        val (rotation, pkg) = DailySetRotation.advance(
            rotation = finished,
            todayKey = todayKey,
            level = snapshot.currentLevel,
            targetMinutes = snapshot.dailyTargetMinutes,
            nowMs = nowMs
        )
        val nextPlan = DailySetRotation.planFrom(pkg, snapshot.selectedGoals, snapshot.dailyTargetMinutes, 0, nowMs)
        val setEvent = LearningEvent(
            idempotencyKey = "daily_set_done_$finishedSetKey",
            type = LearningEventType.DAILY_SET_COMPLETED,
            timestampMs = nowMs,
            localDateKey = todayKey,
            entityId = finishedSetKey,
            title = "Daily set $finishedNumber completed",
            skillTag = "Daily"
        )
        val record = DailyRecordEntry(
            id = "set_done_$finishedSetKey",
            dateKey = todayKey,
            timestampMs = nowMs,
            groupId = "daily_sets",
            groupTitle = "Daily task sets",
            kind = DailyRecordKind.DAILY_TASK,
            stepId = finishedSetKey,
            stepTitle = "Set $finishedNumber completed (${plan.totalCount} tasks) — set ${finishedNumber + 1} started",
            outcome = DailyRecordOutcome.COMPLETED
        )
        var next = snapshot.copy(
            dailyPlan = nextPlan,
            dailyRotation = rotation,
            dailyRecords = DailyRecordBook.append(snapshot.dailyRecords, record),
            processedEventKeys = snapshot.processedEventKeys + setEvent.idempotencyKey,
            eventLog = (listOf(setEvent) + snapshot.eventLog).take(300),
            totalPoints = snapshot.totalPoints + pointsFor(setEvent)
        )
        next = unlockAchievementIfNew(
            snapshot = next,
            id = "first_full_daily_plan",
            title = "Daily Plan Finisher",
            description = "Completed every task in your personalized daily practice plan.",
            category = "Consistency",
            nowMs = nowMs
        )
        return appendNotificationIfAllowed(
            snapshot = next,
            category = NotificationCategory.DAILY_PRACTICE_REMINDER,
            title = "Set $finishedNumber complete — your next set is ready",
            body = nextPlan.adaptiveSummary,
            targetRoute = "daily_practice",
            dedupKey = "daily_set_done_$finishedSetKey",
            nowMs = nowMs
        )
    }

    private fun doesEventSatisfyTask(event: LearningEvent, task: DailyPracticeTask): Boolean {
        if (event.type == LearningEventType.DAILY_PLAN_COMPLETED) return true
        val eventTechniques = event.metadata["techniqueIds"].orEmpty().split(",").filter { it.isNotBlank() }
        if (task.linkedActivityId.isNotBlank()) {
            return event.type == LearningEventType.ACTIVITY_ATTEMPTED && event.entityId == task.linkedActivityId
        }
        if (task.linkedTechniqueIds.isNotEmpty()) {
            return when (event.type) {
                LearningEventType.TECHNIQUE_PRACTICED ->
                    event.metadata["valid"] == "true" && eventTechniques.any { it in task.linkedTechniqueIds }
                LearningEventType.ACTIVITY_ATTEMPTED -> eventTechniques.any { it in task.linkedTechniqueIds }
                else -> false
            }
        }
        val cat = task.category.lowercase()
        val skill = task.skillFocus.lowercase()
        val vocabTask = cat.contains("vocab") || skill.contains("vocab")
        return when (event.type) {
            LearningEventType.VOCABULARY_WORD_COMPLETED, LearningEventType.QUIZ_COMPLETED, LearningEventType.FLASHCARD_REVIEWED ->
                vocabTask
            LearningEventType.LESSON_COMPLETED ->
                cat.contains("foundation") || cat.contains("speaking") || task.targetRoute.contains("lesson") || task.targetRoute == "speaking_lab"
            LearningEventType.OWN_SENTENCE_SUBMITTED ->
                cat.contains("grammar") || skill.contains("grammar") || (vocabTask && event.skillTag.equals("Vocabulary", true))
            LearningEventType.WORD_SPOKEN, LearningEventType.SENTENCE_PRACTICED ->
                cat.contains("pronun") || skill.contains("pronun")
            LearningEventType.REAL_WORLD_TASK_COMPLETED -> cat.contains("mission")
            else -> false
        }
    }

    /** Record entries for events coming through [recordEvent] (vocabulary stages, quizzes, lessons, flashcards). */
    private fun defaultRecordsFor(event: LearningEvent): List<DailyRecordEntry> {
        val dateKey = event.localDateKey.ifBlank { todayKeyProvider() }
        val ts = if (event.timestampMs > 0L) event.timestampMs else clockMs()
        val passed = event.metadata["passed"]
        fun base(kind: DailyRecordKind, groupId: String, groupTitle: String, stepTitle: String, outcome: DailyRecordOutcome, score: Int? = null, detail: String = "") =
            DailyRecordEntry(
                id = "event_${event.idempotencyKey}",
                dateKey = dateKey,
                timestampMs = ts,
                groupId = groupId,
                groupTitle = groupTitle,
                kind = kind,
                stepId = event.idempotencyKey,
                stepTitle = stepTitle,
                outcome = outcome,
                score = score,
                detail = detail
            )
        val stageOutcome = when (passed) {
            "true" -> DailyRecordOutcome.PASSED
            "false" -> DailyRecordOutcome.NOT_PASSED
            else -> DailyRecordOutcome.COMPLETED
        }
        return when (event.type) {
            LearningEventType.WORD_LEARNED, LearningEventType.WORD_SPOKEN, LearningEventType.SENTENCE_PRACTICED,
            LearningEventType.OWN_SENTENCE_SUBMITTED, LearningEventType.VOCABULARY_WORD_COMPLETED -> listOf(
                base(
                    kind = DailyRecordKind.VOCABULARY,
                    groupId = vocabGroupId(event.entityId),
                    groupTitle = "Vocabulary: ${event.title.ifBlank { event.entityId }}",
                    stepTitle = event.metadata["stage"]?.let { "Stage: $it" } ?: event.type.name.lowercase().replace('_', ' '),
                    outcome = stageOutcome
                )
            )
            LearningEventType.FLASHCARD_REVIEWED -> listOf(
                base(
                    kind = DailyRecordKind.VOCABULARY,
                    groupId = vocabGroupId(event.entityId),
                    groupTitle = "Vocabulary: ${event.title.ifBlank { event.entityId }}",
                    stepTitle = "Flashcard review",
                    outcome = DailyRecordOutcome.SELF_REPORTED,
                    detail = when (event.metadata["known"]) {
                        "true" -> "Knew it"
                        "false" -> "Needs another review"
                        else -> ""
                    }
                )
            )
            LearningEventType.QUIZ_COMPLETED -> listOf(
                base(
                    kind = DailyRecordKind.QUIZ,
                    groupId = "quiz:${event.entityId}",
                    groupTitle = event.title.ifBlank { "Vocabulary quiz" },
                    stepTitle = "Quiz finished",
                    outcome = DailyRecordOutcome.COMPLETED,
                    score = event.score
                )
            )
            LearningEventType.LESSON_COMPLETED -> listOf(
                base(
                    kind = DailyRecordKind.LESSON,
                    groupId = "lesson:${event.entityId}",
                    groupTitle = event.title.ifBlank { event.entityId },
                    stepTitle = "Lesson completed",
                    outcome = DailyRecordOutcome.COMPLETED,
                    score = event.score
                )
            )
            else -> emptyList()
        }
    }

    private fun activityGroupId(activityId: String) = "activity:$activityId"
    private fun vocabGroupId(wordId: String) = "vocab:$wordId"

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

    private fun ensureDailyVocabularyState(
        snapshot: PersistedLearningSnapshot,
        todayKey: String,
        todayEpochDay: Long
    ): PersistedLearningSnapshot {
        val completedAllTime = snapshot.learnedVocabularyLog.map { it.wordId }.toSet() +
            snapshot.wordProgress.filterValues { it.isCompleted || it.stage2Passed }.keys
        val (assignment, updatedShown) = VocabularyMasteryEngine.ensureDailyVocabularyWords(
            bank = CurriculumCatalog.vocabularyBank,
            level = snapshot.currentLevel,
            todayDateKey = todayKey,
            shownWordIds = snapshot.shownVocabularyWordIds,
            completedAllTimeIds = completedAllTime,
            existingAssignment = snapshot.dailyVocabulary,
            previousDaysCount = snapshot.vocabularyActiveDateKeys.size
        )
        val updatedActiveDays = (snapshot.vocabularyActiveDateKeys + todayKey).distinct().takeLast(90)
        val firstEpoch = snapshot.firstVocabularyEpochDay.takeIf { it > 0L } ?: todayEpochDay
        return snapshot.copy(
            dailyVocabulary = assignment,
            shownVocabularyWordIds = updatedShown,
            vocabularyActiveDateKeys = updatedActiveDays,
            firstVocabularyEpochDay = firstEpoch
        )
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

    private suspend fun commitState(snapshot: PersistedLearningSnapshot, flushAsyncToDisk: Boolean) {
        _state.update { snapshot }
        refreshDailyPackage(snapshot)
        if (flushAsyncToDisk) {
            pendingPersistSnapshot = snapshot
            withContext(ioDispatcher) {
                val latest = pendingPersistSnapshot ?: snapshot
                synchronized(ioLock) { writeToDisk(latest) }
            }
        } else {
            commitState(snapshot)
        }
    }

    private fun commitState(snapshot: PersistedLearningSnapshot) {
        _state.update { snapshot }
        refreshDailyPackage(snapshot)
        val scope = persistScope
        if (scope != null) {
            pendingPersistSnapshot = snapshot
            scope.launch(ioDispatcher) {
                val latest = pendingPersistSnapshot ?: snapshot
                synchronized(ioLock) { writeToDisk(latest) }
            }
        } else {
            synchronized(ioLock) { writeToDisk(snapshot) }
        }
    }

    /** Rebuilds [dailyPackage] only when the current set changed. */
    private fun refreshDailyPackage(snapshot: PersistedLearningSnapshot) {
        val rotation = snapshot.dailyRotation
        val key = listOf(rotation.dateKey, rotation.items, rotation.setIndexToday, rotation.sequencePosition, snapshot.dailyTargetMinutes)
        if (key == dailyPackageKey) return
        dailyPackageKey = key
        _dailyPackage.value = rotation.items?.let { items ->
            DailyAdaptiveCurriculumEngine.rebuildPackage(
                dateKey = rotation.dateKey,
                items = items,
                setIndexToday = rotation.setIndexToday,
                sequencePosition = rotation.sequencePosition,
                targetMinutes = snapshot.dailyTargetMinutes
            )
        }
    }

    private fun fileForUser(uid: String): File? {
        val dir = storageDir ?: return null
        val safe = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "local_guest" }
        return File(dir, "learning_snapshot_$safe.json")
    }

    private fun readFromDisk(uid: String): PersistedLearningSnapshot? = synchronized(ioLock) {
        val file = fileForUser(uid) ?: return null
        if (!file.exists()) return null
        return try {
            json.decodeFromString<PersistedLearningSnapshot>(file.readText())
        } catch (err: Throwable) {
            AppLogger.w(TAG, "Failed reading local snapshot, starting fresh: ${err.message}")
            null
        }
    }

    private fun writeToDisk(snapshot: PersistedLearningSnapshot) {
        val file = fileForUser(snapshot.userId) ?: return
        runCatching {
            file.parentFile?.mkdirs()
            val encoded = json.encodeToString(snapshot)
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(encoded)
            if (!temp.renameTo(file)) {
                file.writeText(encoded)
                temp.delete()
            }
        }.onFailure { err ->
            AppLogger.w(TAG, "Failed writing local snapshot: ${err.message}")
        }
    }
}
