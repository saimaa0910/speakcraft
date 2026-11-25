package com.example.data.learningpaths

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.data.model.ProgramEnrollment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val TAG = "LearningPathRepo"

/**
 * Thread-safe, atomic, offline-first persistence repository for Learning Path progress,
 * detailed lesson scores/feedback/notes, and graded module assessments.
 */
class LearningPathPersistenceRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val storageDir = File(context.applicationContext.filesDir, "learning_paths").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val mutex = Mutex()

    suspend fun loadUserState(userId: String): LearningPathsUserState = withContext(ioDispatcher) {
        mutex.withLock {
            readUnsafe(userId)
        }
    }

    suspend fun enroll(userId: String, program: LearningPathProgram): LearningPathsUserState =
        mutateUserState(userId) { current ->
            val now = System.currentTimeMillis()
            val existing = current.enrollments[program.programId]
            val updated = if (existing != null) {
                existing.copy(active = true, paused = false, updatedAtEpochMs = now)
            } else {
                PathEnrollmentRecord(
                    programId = program.programId,
                    programTitle = program.title,
                    active = true,
                    paused = false,
                    enrolledAtEpochMs = now,
                    updatedAtEpochMs = now,
                    currentDay = 1
                )
            }
            current.copy(
                enrollments = current.enrollments + (program.programId to updated),
                lastUpdatedEpochMs = now
            )
        }

    suspend fun setPaused(
        userId: String,
        programId: String,
        paused: Boolean
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        val existing = current.enrollments[programId] ?: return@mutateUserState current
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            paused = paused,
            active = !paused,
            updatedAtEpochMs = now
        )
        current.copy(
            enrollments = current.enrollments + (programId to updated),
            lastUpdatedEpochMs = now
        )
    }

    suspend fun unenroll(
        userId: String,
        programId: String
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        if (!current.enrollments.containsKey(programId)) return@mutateUserState current
        val now = System.currentTimeMillis()
        current.copy(
            enrollments = current.enrollments - programId,
            lastUpdatedEpochMs = now
        )
    }

    suspend fun recordLessonSubmission(
        userId: String,
        program: LearningPathProgram,
        lesson: LearningPathLesson,
        request: LessonSubmissionRequest,
        feedback: LearningPathFeedback
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        val now = System.currentTimeMillis()
        val existing = current.enrollments[program.programId] ?: PathEnrollmentRecord(
            programId = program.programId,
            programTitle = program.title,
            active = true,
            enrolledAtEpochMs = now
        )
        val prevDay = existing.dayRecords[lesson.dayNumber]
        val isNowCompleted = (prevDay?.completed == true) || feedback.passed

        val updatedDayRecord = DayLessonProgressRecord(
            lessonId = lesson.lessonId,
            dayNumber = lesson.dayNumber,
            completed = isNowCompleted,
            bestScore = maxOf(prevDay?.bestScore ?: 0, feedback.overallScore),
            lastScore = feedback.overallScore,
            attempts = (prevDay?.attempts ?: 0) + 1,
            confidenceSelfRating = feedback.confidenceSelfRating,
            quizScorePercent = feedback.quizScorePercent,
            lastSubmissionText = request.spokenOrWrittenText.trim().take(2000),
            lastAudioPath = request.audioFilePath ?: prevDay?.lastAudioPath,
            audioDurationSeconds = maxOf(prevDay?.audioDurationSeconds ?: 0, request.audioDurationSeconds),
            practiceDurationSeconds = (prevDay?.practiceDurationSeconds ?: 0) +
                request.practiceDurationSeconds.coerceAtLeast(request.audioDurationSeconds),
            reflectionNote = request.reflectionNote.ifBlank { prevDay?.reflectionNote.orEmpty() }.take(1500),
            whatWentWell = feedback.whatWentWell,
            mistakesFound = feedback.mistakesFound,
            correctedExamples = feedback.correctedExamples,
            coachSummary = feedback.explanation,
            coachTips = feedback.coachTips,
            recommendedNextStep = feedback.recommendedNextStep,
            completedQuizAnswers = request.quizAnswers.ifEmpty { prevDay?.completedQuizAnswers.orEmpty() },
            completedChecklistIndices = request.completedChecklistIndices.ifEmpty {
                prevDay?.completedChecklistIndices.orEmpty()
            }.sorted(),
            lastUpdatedEpochMs = now
        )

        val updatedCompleted = if (isNowCompleted) {
            (existing.completedDays + lesson.dayNumber).distinct().sorted()
        } else {
            existing.completedDays
        }

        val nextDay = (1..program.durationDays).firstOrNull { !updatedCompleted.contains(it) }
            ?: program.durationDays

        val updatedEnrollment = existing.copy(
            active = true,
            paused = false,
            updatedAtEpochMs = now,
            currentDay = nextDay,
            completedDays = updatedCompleted,
            dayRecords = existing.dayRecords + (lesson.dayNumber to updatedDayRecord)
        )

        current.copy(
            enrollments = current.enrollments + (program.programId to updatedEnrollment),
            lastUpdatedEpochMs = now
        )
    }

    suspend fun saveLessonReflectionNote(
        userId: String,
        program: LearningPathProgram,
        lesson: LearningPathLesson,
        note: String
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        val now = System.currentTimeMillis()
        val existing = current.enrollments[program.programId] ?: PathEnrollmentRecord(
            programId = program.programId,
            programTitle = program.title,
            active = true,
            enrolledAtEpochMs = now
        )
        val prevDay = existing.dayRecords[lesson.dayNumber] ?: DayLessonProgressRecord(
            lessonId = lesson.lessonId,
            dayNumber = lesson.dayNumber
        )
        val updatedDay = prevDay.copy(
            reflectionNote = note.take(1500),
            lastUpdatedEpochMs = now
        )
        val updatedEnrollment = existing.copy(
            dayRecords = existing.dayRecords + (lesson.dayNumber to updatedDay),
            updatedAtEpochMs = now
        )
        current.copy(
            enrollments = current.enrollments + (program.programId to updatedEnrollment),
            lastUpdatedEpochMs = now
        )
    }

    suspend fun toggleDayCompletionManual(
        userId: String,
        program: LearningPathProgram,
        dayNumber: Int
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        if (dayNumber !in 1..program.durationDays) return@mutateUserState current
        val now = System.currentTimeMillis()
        val existing = current.enrollments[program.programId] ?: PathEnrollmentRecord(
            programId = program.programId,
            programTitle = program.title,
            active = true,
            enrolledAtEpochMs = now
        )
        val lesson = LearningPathCurriculumRegistry.getLesson(program.programId, dayNumber)
        val currentlyCompleted = existing.completedDays.contains(dayNumber)
        val updatedDays = if (currentlyCompleted) {
            existing.completedDays - dayNumber
        } else {
            (existing.completedDays + dayNumber).distinct().sorted()
        }

        val prevRecord = existing.dayRecords[dayNumber]
        val updatedRecord = (prevRecord ?: DayLessonProgressRecord(
            lessonId = lesson?.lessonId ?: "${program.programId}_day_$dayNumber",
            dayNumber = dayNumber
        )).copy(
            completed = !currentlyCompleted,
            lastUpdatedEpochMs = now
        )

        val nextDay = (1..program.durationDays).firstOrNull { !updatedDays.contains(it) }
            ?: program.durationDays

        val updatedEnrollment = existing.copy(
            active = true,
            paused = false,
            updatedAtEpochMs = now,
            currentDay = nextDay,
            completedDays = updatedDays,
            dayRecords = existing.dayRecords + (dayNumber to updatedRecord)
        )
        current.copy(
            enrollments = current.enrollments + (program.programId to updatedEnrollment),
            lastUpdatedEpochMs = now
        )
    }

    suspend fun recordModuleAssessment(
        userId: String,
        program: LearningPathProgram,
        spec: ModuleAssessmentSpec,
        evaluation: ModuleAssessmentEvaluation,
        submission: ModuleAssessmentSubmission
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        val now = System.currentTimeMillis()
        val existing = current.enrollments[program.programId] ?: PathEnrollmentRecord(
            programId = program.programId,
            programTitle = program.title,
            active = true,
            enrolledAtEpochMs = now
        )
        val prevAssessment = existing.moduleAssessments[spec.moduleId]
        val updatedAssessment = ModuleAssessmentRecord(
            assessmentId = spec.assessmentId,
            moduleId = spec.moduleId,
            isCapstone = spec.isCapstone,
            passed = (prevAssessment?.passed == true) || evaluation.passed,
            bestCombinedScore = maxOf(prevAssessment?.bestCombinedScore ?: 0, evaluation.combinedScore),
            lastCombinedScore = evaluation.combinedScore,
            quizScorePercent = evaluation.quizScorePercent,
            practicalScorePercent = evaluation.practicalScorePercent,
            attempts = (prevAssessment?.attempts ?: 0) + 1,
            lastResponseText = submission.practicalResponseText.trim().take(2500),
            summaryFeedback = evaluation.feedback.explanation,
            coachTips = evaluation.feedback.coachTips,
            completedAtEpochMs = now
        )

        val updatedEnrollment = existing.copy(
            active = true,
            updatedAtEpochMs = now,
            moduleAssessments = existing.moduleAssessments + (spec.moduleId to updatedAssessment)
        )

        current.copy(
            enrollments = current.enrollments + (program.programId to updatedEnrollment),
            lastUpdatedEpochMs = now
        )
    }

    /**
     * Merges remote Firestore [ProgramEnrollment] documents carefully with richer local state
     * so neither remote days nor rich local day scores/assessments are overwritten.
     */
    suspend fun mergeRemoteEnrollments(
        userId: String,
        remoteEnrollments: List<ProgramEnrollment>,
        registry: LearningPathCurriculumRegistry = LearningPathCurriculumRegistry
    ): LearningPathsUserState = mutateUserState(userId) { current ->
        if (remoteEnrollments.isEmpty()) return@mutateUserState current
        val now = System.currentTimeMillis()
        val merged = current.enrollments.toMutableMap()

        for (remote in remoteEnrollments) {
            val program = registry.getProgram(remote.programId) ?: continue
            val local = merged[remote.programId]
            val remoteDays = remote.completedDays.map { it.toInt() }.filter { it in 1..program.durationDays }
            if (local == null) {
                val cleanDays = remoteDays.distinct().sorted()
                val nextDay = (1..program.durationDays).firstOrNull { !cleanDays.contains(it) } ?: program.durationDays
                merged[remote.programId] = PathEnrollmentRecord(
                    programId = remote.programId,
                    programTitle = program.title,
                    active = remote.active,
                    paused = !remote.active,
                    enrolledAtEpochMs = now,
                    updatedAtEpochMs = now,
                    currentDay = nextDay,
                    completedDays = cleanDays
                )
            } else {
                // Local timestamps remain source-of-truth for pause & detailed scores; union completedDays cleanly
                val unionDays = (local.completedDays + remoteDays).distinct().sorted()
                val nextDay = (1..program.durationDays).firstOrNull { !unionDays.contains(it) } ?: program.durationDays
                merged[remote.programId] = local.copy(
                    completedDays = unionDays,
                    currentDay = nextDay
                )
            }
        }
        current.copy(enrollments = merged, lastUpdatedEpochMs = now)
    }

    suspend fun clearUserData(userId: String) = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                fileFor(userId).delete()
                File(fileFor(userId).parentFile, "${fileFor(userId).name}.tmp").delete()
            }.onFailure { AppLogger.w(TAG, "Failed deleting user learning paths state", it) }
        }
    }

    suspend fun clearAllData() = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                storageDir.listFiles()?.forEach { it.deleteRecursively() }
            }.onFailure { AppLogger.w(TAG, "Failed clearing learning paths directory", it) }
        }
    }

    private suspend fun mutateUserState(
        userId: String,
        transform: (LearningPathsUserState) -> LearningPathsUserState
    ): LearningPathsUserState = withContext(ioDispatcher) {
        mutex.withLock {
            val current = readUnsafe(userId)
            val next = transform(current)
            if (next != current) {
                writeUnsafe(userId, next)
            }
            next
        }
    }

    private fun readUnsafe(userId: String): LearningPathsUserState {
        val file = fileFor(userId)
        if (!file.exists()) {
            return LearningPathsUserState(userId = sanitizeUid(userId))
        }
        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            json.decodeFromString<LearningPathsUserState>(raw)
        }.onFailure {
            AppLogger.w(TAG, "Corrupt learning paths state; resetting cleanly for user", it)
        }.getOrElse {
            LearningPathsUserState(userId = sanitizeUid(userId))
        }
    }

    private fun writeUnsafe(userId: String, state: LearningPathsUserState) {
        val target = fileFor(userId)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        val encoded = json.encodeToString(LearningPathsUserState.serializer(), state)
        tmp.writeText(encoded, Charsets.UTF_8)
        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Throwable) {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun fileFor(userId: String): File {
        val clean = sanitizeUid(userId)
        return File(storageDir, "paths_$clean.json")
    }

    private fun sanitizeUid(userId: String): String {
        val trimmed = userId.trim().ifBlank { "guest" }
        return trimmed.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
    }
}
