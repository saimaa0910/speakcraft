package com.example.data.learning

import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.LearningPathsEngine
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathPracticeLinker
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Completion-driven daily task sets, the per-day record and learning-path progress from the local learning
 * state (LearningProgressManager + DailySetRotation + LearningPathInputs).
 */
class LearningProgressDailySetsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private var today = "2026-11-25"
    private var tick = 1_764_000_000_000L

    private fun newManager(dir: java.io.File = tmp.root) = LearningProgressManager(
        storageDir = dir,
        clockMs = { tick += 1_000L; tick },
        todayKeyProvider = { today },
        todayEpochDayProvider = { 20_417L }
    )

    private fun LearningProgressManager.plan() = checkNotNull(state.value.dailyPlan)

    private fun LearningProgressManager.completeAllTasks() =
        plan().tasks.map { it.id }.forEach { completeDailyTask(it) }

    // -----------------------------------------------------------------------
    // Task 3: completion-driven daily sets
    // -----------------------------------------------------------------------

    @Test
    fun completing_every_task_of_the_set_starts_the_next_set_straight_away() {
        val mgr = newManager()
        mgr.loadForUser("user_a")
        val first = mgr.plan()
        assertEquals(today, first.dateKey)
        assertTrue("A set has several tasks", first.tasks.size >= 5)
        assertEquals(0, mgr.state.value.dailyRotation.sequencePosition)

        mgr.completeAllTasks()

        val second = mgr.plan()
        val rotation = mgr.state.value.dailyRotation
        assertEquals(today, second.dateKey)
        assertEquals(1, rotation.sequencePosition)
        assertEquals(1, rotation.setIndexToday)
        assertTrue("New set starts with nothing done", second.tasks.none { it.isCompleted })
        assertTrue("Task ids of the new set are new", second.tasks.map { it.id }.intersect(first.tasks.map { it.id }.toSet()).isEmpty())
        assertEquals(1, mgr.dailyPackage.value?.setIndexToday)
        // The finished set is in today's record, and its tasks stay in the history for the tasks pillar.
        val todayEntries = mgr.state.value.dailyRecords[today].orEmpty()
        assertTrue(todayEntries.any { it.groupId == "daily_sets" && it.outcome == DailyRecordOutcome.COMPLETED })
        assertTrue(first.tasks.all { t -> mgr.state.value.completedDailyTasks.any { it.id == t.id && it.isCompleted } })
    }

    @Test
    fun partial_completion_keeps_the_current_set_even_after_reload() {
        val mgr = newManager()
        mgr.loadForUser("user_b")
        val tasks = mgr.plan().tasks
        tasks.take(3).forEach { mgr.completeDailyTask(it.id) }

        assertEquals(tasks.map { it.id }, mgr.plan().tasks.map { it.id })
        assertEquals(0, mgr.state.value.dailyRotation.sequencePosition)

        mgr.loadForUser("user_b")
        val reloaded = mgr.plan()
        assertEquals(tasks.map { it.id }, reloaded.tasks.map { it.id })
        assertEquals(3, reloaded.completedCount)
    }

    @Test
    fun next_set_never_repeats_items_of_the_set_just_completed_at_any_level() {
        LearningLevel.entries.forEach { level ->
            var (rotation, pkg) = DailySetRotation.ensureSetForDate(DailyRotationState(), today, level, 20)
            repeat(3) {
                val previous = pkg.items()
                val (nextRotation, nextPkg) = DailySetRotation.advance(rotation, today, level, 20, nowMs = tick)
                val next = nextPkg.items()
                assertEquals("level stays adaptive", level, nextPkg.userLevel)
                assertTrue("$level words repeat", next.wordIds.intersect(previous.wordIds.toSet()).isEmpty())
                assertTrue("$level sentences repeat", next.sentenceIds.intersect(previous.sentenceIds.toSet()).isEmpty())
                assertTrue("$level activities repeat", next.activityIds.intersect(previous.activityIds.toSet()).isEmpty())
                assertNotEquals("$level technique repeats", previous.techniqueId, next.techniqueId)
                assertNotEquals("$level mission repeats", previous.missionId, next.missionId)
                assertEquals(3, next.wordIds.size)
                assertEquals(3, next.sentenceIds.size)
                assertEquals(3, next.activityIds.size)
                rotation = nextRotation
                pkg = nextPkg
            }
        }
    }

    @Test
    fun the_sequence_is_deterministic_for_the_same_position_and_history() {
        val a = DailySetRotation.advance(DailySetRotation.ensureSetForDate(DailyRotationState(), today, LearningLevel.INTERMEDIATE, 20).first, today, LearningLevel.INTERMEDIATE, 20, 1L)
        val b = DailySetRotation.advance(DailySetRotation.ensureSetForDate(DailyRotationState(), today, LearningLevel.INTERMEDIATE, 20).first, today, LearningLevel.INTERMEDIATE, 20, 2L)
        assertEquals(a.second.items(), b.second.items())
        assertEquals(a.second.dailyTasks.map { it.id }, b.second.dailyTasks.map { it.id })
    }

    @Test
    fun a_new_day_starts_a_fresh_set_from_the_same_sequence_position() {
        val mgr = newManager()
        mgr.loadForUser("user_c")
        val day1Set1 = mgr.state.value.dailyRotation.items!!
        mgr.completeAllTasks()
        val day1Set2 = mgr.plan()
        day1Set2.tasks.take(2).forEach { mgr.completeDailyTask(it.id) } // partial on day 1

        today = "2026-11-26"
        mgr.loadForUser("user_c")

        val rotation = mgr.state.value.dailyRotation
        val day2 = mgr.plan()
        assertEquals("2026-11-26", day2.dateKey)
        assertEquals(0, rotation.setIndexToday)
        assertEquals("position only advances on completion", 1, rotation.sequencePosition)
        assertTrue(day2.tasks.none { it.isCompleted })
        assertTrue(day2.tasks.map { it.id }.intersect(day1Set2.tasks.map { it.id }.toSet()).isEmpty())
        assertTrue(
            "completed items of day 1 are avoided",
            rotation.items!!.activityIds.intersect(day1Set1.activityIds.toSet()).isEmpty()
        )
        // Yesterday's completed tasks stay in the history and the record of that day.
        assertTrue(day1Set2.tasks.take(2).all { t -> mgr.state.value.completedDailyTasks.any { it.id == t.id } })
        assertTrue(mgr.state.value.dailyRecords["2026-11-25"].orEmpty().isNotEmpty())
    }

    @Test
    fun state_survives_a_restart_and_round_trips_through_json() {
        val dir = tmp.newFolder("restart")
        val first = newManager(dir)
        first.loadForUser("user_d")
        first.completeAllTasks()
        first.plan().tasks.take(2).forEach { first.completeDailyTask(it.id) }
        val actTask = first.plan().tasks.first { it.linkedActivityId.isNotBlank() && !it.isCompleted }
        first.recordActivityAttempt(
            activity = ActivityCatalogue.getActivityById(actTask.linkedActivityId)!!,
            attempt = ActivityAttemptRecord(activityId = actTask.linkedActivityId, objectiveScore = 74, passed = true, durationSeconds = 95, dateKey = today, evaluatorName = "SpeakingEvaluator"),
            attemptId = "local_restart_1"
        )
        val before = first.state.value

        val second = newManager(dir)
        second.loadForUser("user_d")
        val after = second.state.value

        assertEquals(before.dailyRotation, after.dailyRotation)
        assertEquals(before.dailyPlan?.tasks, after.dailyPlan?.tasks)
        assertEquals(before.dailyRecords, after.dailyRecords)
        assertEquals(before.activityProgress, after.activityProgress)
        assertEquals(before.activityAttempts, after.activityAttempts)
        assertEquals(before.completedDailyTasks, after.completedDailyTasks)
        assertEquals(before.dailyRotation.setIndexToday, second.dailyPackage.value?.setIndexToday)

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString(PersistedLearningSnapshot.serializer(), json.encodeToString(PersistedLearningSnapshot.serializer(), after))
        assertEquals(after, decoded)
    }

    // -----------------------------------------------------------------------
    // Tasks 1, 2, 4: progress and the daily record from every completion
    // -----------------------------------------------------------------------

    @Test
    fun activity_attempt_completes_its_daily_task_records_the_result_and_feeds_the_paths() {
        val mgr = newManager()
        mgr.loadForUser("user_e")
        val actTask = mgr.plan().tasks.first { it.linkedActivityId.isNotBlank() }
        val activity = ActivityCatalogue.getActivityById(actTask.linkedActivityId)!!

        assertTrue(
            mgr.recordActivityAttempt(
                activity,
                ActivityAttemptRecord(activityId = activity.activityId, objectiveScore = 81, passed = true, durationSeconds = 120, dateKey = today, evaluatorName = "SpeakingEvaluator"),
                attemptId = "local_a1"
            )
        )
        assertFalse("same attempt is not counted twice", mgr.recordActivityAttempt(activity, ActivityAttemptRecord(activityId = activity.activityId, objectiveScore = 81, passed = true), "local_a1"))

        val snapshot = mgr.state.value
        assertTrue(snapshot.dailyPlan!!.tasks.first { it.id == actTask.id }.isCompleted)
        val groups = DailyRecordBook.groupsFor(snapshot.dailyRecords, today)
        val group = groups.first { it.groupId == "activity:${activity.activityId}" }
        val attemptEntry = group.entries.first { it.kind == DailyRecordKind.ACTIVITY }
        assertEquals(81, attemptEntry.score)
        assertEquals(DailyRecordOutcome.PASSED, attemptEntry.outcome)
        assertTrue("the daily task sits under the same activity", group.entries.any { it.kind == DailyRecordKind.DAILY_TASK && it.stepId == actTask.id })

        assertEquals(1, snapshot.activityProgress[activity.activityId]?.attemptsCount)
        val inputs = LearningPathInputs.from(snapshot)
        val path = LearningPathsEngine.allPaths.firstOrNull { activity.activityId in it.orderedActivityIds }
        if (path != null) {
            val summary = LearningPathsEngine.evaluatePathProgress(path, inputs.progress, inputs.attempts, inputs.dailyTasks, inputs.techniquePractice)
            assertTrue(summary.activityProgress.activities.first { it.activityId == activity.activityId }.isCompleted)
            assertTrue(summary.taskProgress.tasks.any { it.taskId.endsWith(actTask.id) && it.isCompleted })
        }
    }

    @Test
    fun coach_practice_counts_valid_attempts_only_and_records_inconclusive_ones_honestly() {
        val mgr = newManager()
        mgr.loadForUser("user_f")
        val techTask = mgr.plan().tasks.first { it.category == "Technique" }
        val techId = techTask.linkedTechniqueIds.single()

        mgr.recordPracticeCompletion(
            PracticeCompletion(
                attemptId = "att-1", activityId = "act-x", title = "Drill X", techniqueIds = listOf(techId),
                templateId = "TPL-ANSWER", timestampMs = tick + 10, evaluationVersion = "1.6.0", valid = false,
                inconclusiveReason = "TOO_SHORT"
            )
        )
        assertFalse(mgr.plan().tasks.first { it.id == techTask.id }.isCompleted)

        mgr.recordPracticeCompletion(
            PracticeCompletion(
                attemptId = "att-2", activityId = "act-x", title = "Drill X", techniqueIds = listOf(techId),
                templateId = "TPL-ANSWER", timestampMs = tick + 20, evaluationVersion = "1.6.0", valid = true,
                issueCodes = listOf("ISSUE_FILLERS_HIGH"), strengthCode = "STR_PACE_IN_BAND"
            )
        )
        assertFalse("duplicate attempt ignored", mgr.recordPracticeCompletion(
            PracticeCompletion("att-2", "act-x", "Drill X", listOf(techId), "TPL-ANSWER", tick + 20, "1.6.0", valid = true)
        ))

        val snapshot = mgr.state.value
        assertTrue(snapshot.dailyPlan!!.tasks.first { it.id == techTask.id }.isCompleted)
        val stat = snapshot.techniquePractice.getValue(techId)
        assertEquals(2, stat.attempts)
        assertEquals(1, stat.validAttempts)
        assertEquals(1, stat.inconclusiveAttempts)
        assertNull("free speaking has no score", stat.bestScore)

        val entries = DailyRecordBook.groupsFor(snapshot.dailyRecords, today).first { it.groupId == "coach:act-x" }.entries
        assertEquals(DailyRecordOutcome.INCONCLUSIVE, entries[0].outcome)
        assertNull(entries[0].score)
        assertEquals("TOO_SHORT", entries[0].detail)
        assertEquals(DailyRecordOutcome.EVALUATED, entries[1].outcome)
        assertNull(entries[1].score)
        assertEquals("1.6.0", entries[1].evaluationVersion)

        // Techniques pillar: practised (one evaluated attempt), not mastered from practice alone.
        val path = LearningPathsEngine.allPaths.first { p -> LearningPathsEngine.pathPrimaryTechniques[p.pathId].orEmpty().contains(techId) }
        val inputs = LearningPathInputs.from(snapshot)
        val tech = LearningPathsEngine.evaluatePathProgress(path, inputs.progress, inputs.attempts, inputs.dailyTasks, inputs.techniquePractice)
            .techniqueProgress.techniques.first { it.techniqueId == techId }
        assertTrue(tech.isPracticed)
        assertFalse(tech.isMastered)
        assertEquals(1, tech.validPracticeAttempts)
        assertEquals(1, tech.inconclusivePracticeAttempts)
        assertEquals(33, tech.percentComplete)
    }

    @Test
    fun manual_completion_is_self_reported_in_the_record_without_a_score() {
        val mgr = newManager()
        mgr.loadForUser("user_g")
        val mission = mgr.plan().tasks.first { it.category == "Mission" }
        mgr.completeDailyTask(mission.id)
        val entry = mgr.state.value.dailyRecords[today].orEmpty().single { it.stepId == mission.id }
        assertEquals(DailyRecordOutcome.SELF_REPORTED, entry.outcome)
        assertNull(entry.score)
        assertEquals(DailyRecordKind.DAILY_TASK, entry.kind)
    }

    @Test
    fun learning_path_lessons_routine_steps_and_lessons_land_in_the_record() {
        val mgr = newManager()
        mgr.loadForUser("user_h")
        mgr.recordLearningPathLessonAttempt("prog", "Program", "lesson_1", 1, "Day one", score = 48, passed = false, attemptNumber = 1, minutesSpent = 5, skillTag = "Fluency")
        mgr.recordLearningPathLessonAttempt("prog", "Program", "lesson_1", 1, "Day one", score = 77, passed = true, attemptNumber = 2, minutesSpent = 5, skillTag = "Fluency")
        mgr.recordDailyRoutineStep("vocab_words", "Learn 3 Daily Vocabulary Words")
        mgr.recordDailyRoutineStep("vocab_words", "Learn 3 Daily Vocabulary Words") // once per day

        val groups = DailyRecordBook.groupsFor(mgr.state.value.dailyRecords, today)
        val lesson = groups.first { it.groupId == "lp:prog:day1" }.entries
        assertEquals(listOf(DailyRecordOutcome.NOT_PASSED, DailyRecordOutcome.PASSED), lesson.map { it.outcome })
        assertEquals(listOf(48, 77), lesson.map { it.score })
        assertEquals(1, groups.first { it.groupId == "daily_routine" }.entries.size)
        assertTrue(mgr.state.value.processedEventKeys.contains("lp_lesson_1"))
    }

    @Test
    fun daily_record_book_is_idempotent_grouped_and_bounded() {
        var records = emptyMap<String, List<DailyRecordEntry>>()
        fun entry(id: String, day: String, group: String, ts: Long) = DailyRecordEntry(
            id = id, dateKey = day, timestampMs = ts, groupId = group, groupTitle = group, kind = DailyRecordKind.ACTIVITY,
            stepId = id, stepTitle = id, outcome = DailyRecordOutcome.COMPLETED
        )
        records = DailyRecordBook.append(records, entry("a", "2026-11-25", "g1", 1))
        records = DailyRecordBook.append(records, entry("a", "2026-11-25", "g1", 1))
        records = DailyRecordBook.append(records, entry("b", "2026-11-25", "g2", 5))
        records = DailyRecordBook.append(records, entry("c", "2026-11-25", "g1", 9))
        assertEquals(3, records["2026-11-25"]!!.size)
        val groups = DailyRecordBook.groupsFor(records, "2026-11-25")
        assertEquals(listOf("g1", "g2"), groups.map { it.groupId })
        assertEquals(listOf("a", "c"), groups[0].entries.map { it.id })

        (1..70).forEach { d -> records = DailyRecordBook.append(records, entry("d$d", "2026-%02d-%02d".format(1 + d / 28, 1 + d % 28), "g", d.toLong())) }
        assertEquals(DailyRecordBook.MAX_DAYS, records.size)
        assertEquals(records.keys.sortedDescending(), DailyRecordBook.daysWithRecords(records))
    }

    @Test
    fun program_practice_evidence_links_drills_and_record_entries_to_learning_path_programs() {
        val program = LearningPathCurriculumRegistry.allPrograms.first { LearningPathPracticeLinker.linkedActivityIds(it).any { id -> ActivityCatalogue.getActivityById(id) != null } }
        val activity = ActivityCatalogue.getActivityById(LearningPathPracticeLinker.linkedActivityIds(program).first { ActivityCatalogue.getActivityById(it) != null })!!
        val mgr = newManager()
        mgr.loadForUser("user_i")
        mgr.recordActivityAttempt(activity, ActivityAttemptRecord(activityId = activity.activityId, objectiveScore = 55, passed = false, dateKey = today), "local_p1")
        mgr.recordLearningPathLessonAttempt(program.programId, program.title, "l1", 1, "Day one", 70, true, 1, 5, "Fluency")

        val evidence = LearningPathPracticeLinker.evidenceFor(program, mgr.state.value)
        assertEquals(listOf(activity.activityId), evidence.practicedActivityIds)
        assertTrue(evidence.passedActivityIds.isEmpty())
        assertTrue(evidence.hasAnyPractice)
        assertTrue(evidence.recentRecordEntries.any { it.groupId == "lp:${program.programId}:day1" })
        assertTrue(evidence.recentRecordEntries.any { it.groupId == "activity:${activity.activityId}" })
        assertNotNull(evidence.lastPracticedDateKey)
    }
}
