package com.example.data.learning

import com.example.data.activity.ActivityCatalogue
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathsUserState
import com.example.data.model.SpeakingSession

/**
 * Origin module category for a past recorded audio or video session across SpeakCraft.
 */
enum class RecordingSourceCategory(val label: String) {
    ALL("All"),
    SPEAKING_STUDIO("Record & Review"),
    COACH_PRACTICE("Coach Practice"),
    LEARNING_PATHS("Learning Paths"),
    ACTIVITIES("150 Activities"),
    SPEAKING_LABS("Speaking Labs")
}

/**
 * Unified presentation model for any recorded session across the entire app,
 * rendered exclusively under the Recordings tab in User Profile.
 */
data class UnifiedRecordedSession(
    val key: String,
    val title: String,
    val sourceCategory: RecordingSourceCategory,
    val sourceLabel: String,
    val timestampMs: Long,
    val durationSeconds: Int,
    val localMediaPath: String,
    val isVideo: Boolean,
    val summary: String,
    val transcript: String = "",
    val badges: List<String> = emptyList(),
    val comparisonNote: String? = null,
    val comparisonPositive: Boolean = true,
    val backedSpeakingSession: SpeakingSession? = null,
    val backedPracticeRecordingId: String? = null,
    val backedActivityAttemptId: String? = null,
    val backedLearningPathKey: Pair<String, Int>? = null
)

object UnifiedRecordingsCatalog {

    /**
     * Collects all past recorded sessions from:
     * 1) Record & Review Studio and specialized Speaking Labs ([SpeakingSession])
     * 2) Coach Practice ([SavedPracticeRecording])
     * 3) Learning Paths daily lessons ([LearningPathsUserState] day audio attachments)
     * 4) Activity Engine 150 ([LocalActivityAttempt] recorded media)
     */
    fun collectAll(
        speakingSessions: List<SpeakingSession>,
        learningSnapshot: PersistedLearningSnapshot?,
        learningPathsState: LearningPathsUserState?
    ): List<UnifiedRecordedSession> {
        val items = mutableListOf<UnifiedRecordedSession>()

        // 1. Speaking Studio & Labs
        speakingSessions.forEachIndexed { idx, sess ->
            val category = if (sess.moduleType.equals("SPEAKING_LAB", ignoreCase = true)) {
                RecordingSourceCategory.SPEAKING_STUDIO
            } else {
                RecordingSourceCategory.SPEAKING_LABS
            }
            val sourceLabel = when (sess.moduleType.uppercase()) {
                "SPEAKING_LAB" -> "Record & Review Studio"
                "CHALLENGES" -> "Speaking Challenge"
                "PRONUNCIATION" -> "Phonetic & Intonation Lab"
                "ARTICULATION" -> "Articulation Lab"
                "SHADOWING", "LISTENING_LAB" -> "Active Listening & Shadowing"
                "GRAMMAR_SPEAKING" -> "Grammar Speaking Lab"
                "PUBLIC_SPEAKING" -> "Public Speaking Lab"
                "VOICE_TRAINING" -> "Voice Training Lab"
                "LESSON_DETAIL" -> "Lesson Practice"
                else -> sess.moduleType.ifBlank { "Speaking Session" }
            }
            val pills = if (LabAttemptEvaluator.isEngineEvaluated(sess)) {
                buildList {
                    add("On-device feedback")
                    if (sess.wordsPerMinute > 0L) add("${sess.wordsPerMinute} WPM")
                    else if (sess.transcript.isBlank()) add("Audio only")
                }
            } else {
                buildList {
                    add("Fluency ${sess.fluencyScore}")
                    add(if (sess.pronunciationAssessable) "Pron ${sess.pronunciationScore}" else "Pron N/A")
                    add("Articulation ${sess.articulationScore}")
                    if (sess.wordsPerMinute > 0L) add("${sess.wordsPerMinute} WPM")
                }
            }
            val prior = speakingSessions.getOrNull(idx + 1)
            val comparable = prior != null &&
                !LabAttemptEvaluator.isEngineEvaluated(sess) &&
                !LabAttemptEvaluator.isEngineEvaluated(prior)
            val comparisonText = if (comparable) {
                val deltaFlu = sess.fluencyScore - prior!!.fluencyScore
                val deltaWpm = sess.wordsPerMinute - prior.wordsPerMinute
                "Compared with prior: Fluency ${if (deltaFlu >= 0) "+$deltaFlu" else "$deltaFlu"}, WPM $deltaWpm"
            } else null
            val deltaFlu = if (comparable) sess.fluencyScore - prior!!.fluencyScore else 0L
            val isVid = sess.mediaType.contains("video", ignoreCase = true) ||
                sess.localMediaPath.endsWith(".mp4", ignoreCase = true)
            val ts = sess.createdAt?.toDate()?.time
                ?: sess.updatedAt?.toDate()?.time
                ?: 0L

            items += UnifiedRecordedSession(
                key = "speak_${sess.id.ifBlank { "${idx}_${sess.promptTitle}_${sess.durationSeconds}" }}",
                title = sess.promptTitle.ifBlank { sourceLabel },
                sourceCategory = category,
                sourceLabel = sourceLabel,
                timestampMs = ts,
                durationSeconds = sess.durationSeconds.toInt().coerceAtLeast(0),
                localMediaPath = sess.localMediaPath,
                isVideo = isVid,
                summary = sess.overallAssessment.ifBlank { "Saved $sourceLabel recording." },
                transcript = sess.transcript,
                badges = pills,
                comparisonNote = comparisonText,
                comparisonPositive = deltaFlu >= 0L,
                backedSpeakingSession = sess
            )
        }

        // 2. Coach Practice archived recordings
        learningSnapshot?.practiceRecordings.orEmpty().forEach { rec ->
            val pills = buildList {
                if (!rec.valid) {
                    add("Inconclusive")
                } else {
                    add("On-device feedback")
                    rec.score?.let { add("Score $it%") }
                    rec.passed?.let { add(if (it) "Passed" else "Practice") }
                }
            }
            items += UnifiedRecordedSession(
                key = "coach_${rec.id}",
                title = rec.title.ifBlank { "Coach Practice" },
                sourceCategory = RecordingSourceCategory.COACH_PRACTICE,
                sourceLabel = "Coach Practice",
                timestampMs = rec.timestampMs,
                durationSeconds = rec.durationSeconds.coerceAtLeast(0),
                localMediaPath = rec.localMediaPath,
                isVideo = rec.hasVideo || rec.localMediaPath.endsWith(".mp4", ignoreCase = true),
                summary = rec.summary.ifBlank { "Completed on-device Coach Practice attempt." },
                transcript = rec.transcript,
                badges = pills,
                backedPracticeRecordingId = rec.id
            )
        }

        // 3. Learning Paths recorded speech responses
        learningPathsState?.enrollments?.forEach { (programId, enrollment) ->
            val program = LearningPathCurriculumRegistry.getProgram(programId)
            val programTitle = program?.title ?: enrollment.programTitle.ifBlank { "Learning Path" }
            enrollment.dayRecords.values.forEach { day ->
                val audioPath = day.lastAudioPath
                if (!audioPath.isNullOrBlank()) {
                    val lessonTitle = LearningPathCurriculumRegistry.getLesson(programId, day.dayNumber)?.title
                        ?: "Day ${day.dayNumber}"
                    items += UnifiedRecordedSession(
                        key = "lp_${programId}_d${day.dayNumber}",
                        title = "Day ${day.dayNumber}: $lessonTitle",
                        sourceCategory = RecordingSourceCategory.LEARNING_PATHS,
                        sourceLabel = "Learning Paths · $programTitle",
                        timestampMs = day.lastUpdatedEpochMs,
                        durationSeconds = day.audioDurationSeconds.coerceAtLeast(0),
                        localMediaPath = audioPath,
                        isVideo = audioPath.endsWith(".mp4", ignoreCase = true),
                        summary = day.coachSummary.ifBlank { "Recorded lesson practice response." },
                        transcript = day.lastSubmissionText,
                        badges = buildList {
                            if (day.lastScore > 0) add("Score ${day.lastScore}%")
                            add(if (day.completed) "Completed" else "Draft")
                        },
                        backedLearningPathKey = programId to day.dayNumber
                    )
                }
            }
        }

        // 4. 150 Activities recorded attempts
        learningSnapshot?.activityAttempts.orEmpty().forEach { att ->
            if (att.recordedMediaPath.isNotBlank()) {
                val actDef = ActivityCatalogue.getActivityById(att.activityId)
                val title = actDef?.title ?: att.activityId
                items += UnifiedRecordedSession(
                    key = "act_${att.id}",
                    title = title,
                    sourceCategory = RecordingSourceCategory.ACTIVITIES,
                    sourceLabel = "150 Activities",
                    timestampMs = att.timestampMs,
                    durationSeconds = att.durationSeconds.toInt().coerceAtLeast(0),
                    localMediaPath = att.recordedMediaPath,
                    isVideo = att.recordedMediaPath.endsWith(".mp4", ignoreCase = true),
                    summary = actDef?.learningObjective?.ifBlank { actDef.description }
                        ?: "Recorded 150 Activities speaking attempt.",
                    badges = buildList {
                        add("Score ${att.objectiveScore}%")
                        if (att.wordsPerMinute > 0) add("${att.wordsPerMinute} WPM")
                        add(if (att.passed) "Passed" else "Practiced")
                    },
                    backedActivityAttemptId = att.id
                )
            }
        }

        return items
            .sortedWith(compareByDescending<UnifiedRecordedSession> { it.timestampMs }.thenBy { it.title })
    }
}
