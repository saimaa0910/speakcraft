package com.example.data.learningpaths

import com.example.data.activity.LearningPathsEngine
import com.example.data.learning.DailyRecordEntry
import com.example.data.learning.PersistedLearningSnapshot

/**
 * Practice done outside a program's lessons that counts toward it: Activity Engine drills linked to its
 * lessons, Coach Practice of the techniques those drills teach, and completed daily tasks for those drills.
 * Every number comes from the locally persisted learning state; nothing is estimated.
 */
data class ProgramPracticeEvidence(
    val programId: String,
    val linkedActivityIds: List<String>,
    val practicedActivityIds: List<String>,
    val passedActivityIds: List<String>,
    val techniqueIds: List<String>,
    /** Techniques with at least one evaluated (valid) Coach Practice attempt. */
    val practicedTechniqueIds: List<String>,
    val validCoachAttempts: Int,
    val inconclusiveCoachAttempts: Int,
    val completedDailyTasks: Int,
    /** Lesson / assessment / drill entries of this program in the daily record, newest first. */
    val recentRecordEntries: List<DailyRecordEntry>,
    val lastPracticedDateKey: String
) {
    val hasAnyPractice: Boolean
        get() = practicedActivityIds.isNotEmpty() || validCoachAttempts > 0 || inconclusiveCoachAttempts > 0 ||
            completedDailyTasks > 0 || recentRecordEntries.isNotEmpty()
}

/** Links the learning-progress state (activities, techniques, daily tasks, daily record) to programs. */
object LearningPathPracticeLinker {

    /** Record group id used for a program's lessons and assessments (see LearningProgressManager). */
    fun programGroupPrefix(programId: String): String = "lp:$programId:"

    fun linkedActivityIds(program: LearningPathProgram): List<String> =
        program.modules.flatMap { it.lessons }.map { it.linkedActivityId }.filter { it.isNotBlank() }.distinct()

    fun techniqueIds(program: LearningPathProgram): List<String> =
        linkedActivityIds(program).flatMap { LearningPathsEngine.activityToTechniques[it].orEmpty() }.distinct()

    fun evidenceFor(program: LearningPathProgram, snapshot: PersistedLearningSnapshot): ProgramPracticeEvidence {
        val activityIds = linkedActivityIds(program)
        val activitySet = activityIds.toSet()
        val techniques = techniqueIds(program)
        val progress = activityIds.mapNotNull { snapshot.activityProgress[it] }
        val practice = techniques.mapNotNull { snapshot.techniquePractice[it] }
        val tasks = snapshot.completedDailyTasks.filter { it.isCompleted && it.linkedActivityId in activitySet }
        val prefix = programGroupPrefix(program.programId)
        val entries = snapshot.dailyRecords.values.flatten()
            .filter { e ->
                e.groupId.startsWith(prefix) ||
                    e.groupId.removePrefix("activity:").let { it != e.groupId && it in activitySet }
            }
            .sortedByDescending { it.timestampMs }
        val lastDate = (progress.map { it.lastPracticedDateKey } + practice.map { it.lastPracticedDateKey } + entries.map { it.dateKey })
            .filter { it.isNotBlank() }
            .maxOrNull()
            .orEmpty()
        return ProgramPracticeEvidence(
            programId = program.programId,
            linkedActivityIds = activityIds,
            practicedActivityIds = progress.filter { it.attemptsCount > 0 }.map { it.activityId },
            passedActivityIds = progress.filter { it.completed }.map { it.activityId },
            techniqueIds = techniques,
            practicedTechniqueIds = practice.filter { it.validAttempts > 0 }.map { it.techniqueId },
            validCoachAttempts = practice.sumOf { it.validAttempts },
            inconclusiveCoachAttempts = practice.sumOf { it.inconclusiveAttempts },
            completedDailyTasks = tasks.size,
            recentRecordEntries = entries.take(8),
            lastPracticedDateKey = lastDate
        )
    }

    fun evidenceForAll(
        programs: List<LearningPathProgram>,
        snapshot: PersistedLearningSnapshot
    ): Map<String, ProgramPracticeEvidence> = programs.associate { it.programId to evidenceFor(it, snapshot) }
}
