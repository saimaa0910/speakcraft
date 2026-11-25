package com.example.data.learning

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Daily record: what was done each day, per exercise / activity
// ---------------------------------------------------------------------------

/** Where a daily-record entry came from. */
@Serializable
enum class DailyRecordKind(val label: String) {
    ACTIVITY("Activity drill"),
    COACH_PRACTICE("Coach Practice"),
    TECHNIQUE("Technique"),
    DAILY_TASK("Daily task"),
    DAILY_ROUTINE("Daily routine"),
    VOCABULARY("Vocabulary"),
    LESSON("Lesson"),
    QUIZ("Quiz"),
    REAL_WORLD("Real-world task")
}

/**
 * The result of one recorded step, exactly as it was recorded — never inferred.
 * [EVALUATED] means the engine evaluated the attempt but the exercise has no pass mark (free speaking).
 */
@Serializable
enum class DailyRecordOutcome(val label: String) {
    PASSED("Passed"),
    NOT_PASSED("Not passed yet"),
    EVALUATED("Evaluated"),
    INCONCLUSIVE("Inconclusive"),
    COMPLETED("Completed"),
    SELF_REPORTED("Marked done")
}

/** One completed task or step, kept in the local learning state under its day. */
@Serializable
data class DailyRecordEntry(
    /** Idempotency key: recording the same entry twice keeps one copy. */
    val id: String,
    val dateKey: String,
    val timestampMs: Long,
    /** The exercise / activity the step belongs to (e.g. `activity:m01_reading_aloud`). */
    val groupId: String,
    val groupTitle: String,
    val kind: DailyRecordKind,
    val stepId: String,
    val stepTitle: String,
    val outcome: DailyRecordOutcome,
    /** Score only when one was recorded (0..100); null otherwise. */
    val score: Int? = null,
    val detail: String = "",
    val techniqueIds: List<String> = emptyList(),
    val evaluationVersion: String = ""
)

/** Entries of one exercise / activity on one day, oldest step first. */
data class DailyRecordGroup(
    val groupId: String,
    val groupTitle: String,
    val kind: DailyRecordKind,
    val entries: List<DailyRecordEntry>
) {
    val latestTimestampMs: Long get() = entries.maxOfOrNull { it.timestampMs } ?: 0L
}

/** Pure helpers for the per-day record (bounded, idempotent, grouped by exercise / activity). */
object DailyRecordBook {
    const val MAX_DAYS = 60
    const val MAX_ENTRIES_PER_DAY = 300

    fun append(
        records: Map<String, List<DailyRecordEntry>>,
        entry: DailyRecordEntry
    ): Map<String, List<DailyRecordEntry>> {
        val day = records[entry.dateKey].orEmpty()
        if (day.any { it.id == entry.id }) return records
        val updatedDay = (day + entry).sortedBy { it.timestampMs }.takeLast(MAX_ENTRIES_PER_DAY)
        val updated = records + (entry.dateKey to updatedDay)
        return if (updated.size <= MAX_DAYS) updated
        else updated.toSortedMap().entries.toList().takeLast(MAX_DAYS).associate { it.key to it.value }
    }

    fun appendAll(
        records: Map<String, List<DailyRecordEntry>>,
        entries: List<DailyRecordEntry>
    ): Map<String, List<DailyRecordEntry>> = entries.fold(records) { acc, e -> append(acc, e) }

    /** Groups one day's entries by exercise / activity, most recently active group first. */
    fun groupsFor(records: Map<String, List<DailyRecordEntry>>, dateKey: String): List<DailyRecordGroup> =
        records[dateKey].orEmpty()
            .groupBy { it.groupId }
            .map { (groupId, entries) ->
                val sorted = entries.sortedBy { it.timestampMs }
                DailyRecordGroup(
                    groupId = groupId,
                    groupTitle = sorted.last().groupTitle,
                    kind = sorted.first().kind,
                    entries = sorted
                )
            }
            .sortedByDescending { it.latestTimestampMs }

    /** Days that have at least one entry, newest first. */
    fun daysWithRecords(records: Map<String, List<DailyRecordEntry>>): List<String> =
        records.filterValues { it.isNotEmpty() }.keys.sortedDescending()

    /** Short, honest result text for one entry (e.g. "Passed • score 82", "Inconclusive — TOO_SHORT"). */
    fun resultText(entry: DailyRecordEntry): String = buildString {
        append(entry.outcome.label)
        if (entry.score != null) append(" • score ${entry.score}")
        if (entry.outcome == DailyRecordOutcome.INCONCLUSIVE && entry.detail.isNotBlank()) append(" — ${entry.detail}")
    }
}

// ---------------------------------------------------------------------------
// Completion-driven daily task sets
// ---------------------------------------------------------------------------

/** The content items of one daily set, so the same set can be rebuilt after a restart. */
@Serializable
data class DailySetItems(
    val level: LearningLevel = LearningLevel.INTERMEDIATE,
    val wordIds: List<String> = emptyList(),
    val sentenceIds: List<String> = emptyList(),
    val activityIds: List<String> = emptyList(),
    val techniqueId: String = "",
    val missionId: String = ""
) {
    fun allIds(): List<String> =
        wordIds + sentenceIds + activityIds + listOf(techniqueId, missionId).filter { it.isNotBlank() }
}

/**
 * Persisted, per-user position in the daily-set sequence.
 * [sequencePosition] advances each time the user completes a whole set; a new day starts a fresh set
 * ([setIndexToday] = 0) from the same position. [recentItemIds] (most recent first) are avoided when the
 * next set is chosen.
 */
@Serializable
data class DailyRotationState(
    val dateKey: String = "",
    val sequencePosition: Int = 0,
    val setIndexToday: Int = 0,
    val items: DailySetItems? = null,
    val recentItemIds: List<String> = emptyList(),
    val lastSetCompletedAtMs: Long = 0L
) {
    companion object {
        const val MAX_RECENT_ITEMS = 60
    }
}

// ---------------------------------------------------------------------------
// Locally persisted activity and technique progress (feeds the learning paths)
// ---------------------------------------------------------------------------

/** Activity Engine progress for one catalogue activity, kept locally so it survives restarts offline. */
@Serializable
data class LocalActivityProgress(
    val activityId: String,
    val moduleId: String = "",
    val skillCategory: String = "",
    val completed: Boolean = false,
    val attemptsCount: Int = 0,
    val bestObjectiveScore: Int = 0,
    val latestObjectiveScore: Int = 0,
    val latestConfidenceSelfRating: Int = 0,
    val totalPracticeSeconds: Long = 0L,
    val lastPracticedDateKey: String = "",
    val updatedAtMs: Long = 0L
)

/** One evaluated Activity Engine attempt, as recorded (most recent first in the snapshot). */
@Serializable
data class LocalActivityAttempt(
    val id: String,
    val activityId: String,
    val moduleId: String = "",
    val skillCategory: String = "",
    val exerciseType: String = "",
    val evaluatorName: String = "",
    val dateKey: String = "",
    val timestampMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val objectiveScore: Int = 0,
    val confidenceSelfRating: Int = 0,
    val passed: Boolean = false,
    val aiEvaluated: Boolean = false,
    val wordsPerMinute: Int = 0,
    val fillerCount: Int = 0
)

/** Coach Practice evidence for one technique (techniques pillar). Counts only what the engine reported. */
@Serializable
data class TechniquePracticeStat(
    val techniqueId: String,
    val attempts: Int = 0,
    val validAttempts: Int = 0,
    val inconclusiveAttempts: Int = 0,
    /** Valid attempts whose template has a pass mark. */
    val judgedAttempts: Int = 0,
    val passedAttempts: Int = 0,
    val bestScore: Int? = null,
    val lastPracticedDateKey: String = "",
    val lastPracticedAtMs: Long = 0L
)
