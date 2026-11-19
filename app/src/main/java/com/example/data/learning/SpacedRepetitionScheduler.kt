package com.example.data.learning

import kotlin.math.roundToInt

/**
 * Local SM-2 Spaced Repetition Scheduler for Vocabulary Flashcards & Review Queue.
 * Deduplicates entries by wordId and supports 100% offline scheduling.
 */
object SpacedRepetitionScheduler {

    /**
     * Reviews [current] card with quality score:
     * - [known] == true -> quality 4 (Known / Good)
     * - [known] == false -> quality 2 (Need Review / Hard)
     */
    fun scheduleNext(
        wordId: String,
        word: String,
        current: SpacedRepetitionItem?,
        known: Boolean,
        todayEpochDay: Long,
        nowMs: Long = System.currentTimeMillis()
    ): SpacedRepetitionItem {
        val prevEase = current?.easeFactor ?: 2.5
        val prevInterval = current?.intervalDays ?: 0
        val prevReps = current?.repetitions ?: 0

        val quality = if (known) 4 else 2
        val newEase = (prevEase + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))).coerceIn(1.3, 3.0)

        val (newReps, newInterval) = if (!known) {
            0 to 1
        } else {
            val nextReps = prevReps + 1
            val interval = when (nextReps) {
                1 -> 1
                2 -> 3
                3 -> 6
                else -> (prevInterval * newEase).roundToInt().coerceIn(7, 60)
            }
            nextReps to interval
        }

        val status = when {
            newReps >= 4 && newInterval >= 14 -> "MASTERED"
            known -> "LEARNING"
            else -> "REVIEW_DUE"
        }

        return SpacedRepetitionItem(
            wordId = wordId,
            word = word,
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = newReps,
            nextReviewEpochDay = todayEpochDay + newInterval.toLong(),
            lastReviewedAtMs = nowMs,
            status = status
        )
    }

    /**
     * Returns deduplicated due wordIds ordered by most overdue first.
     */
    fun dueItems(
        items: Map<String, SpacedRepetitionItem>,
        todayEpochDay: Long
    ): List<SpacedRepetitionItem> =
        items.values
            .filter { it.nextReviewEpochDay <= todayEpochDay }
            .sortedBy { it.nextReviewEpochDay }
}
