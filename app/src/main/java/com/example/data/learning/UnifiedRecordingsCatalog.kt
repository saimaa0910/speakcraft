package com.example.data.learning

import android.content.Context
import com.example.data.activity.ActivityCatalogue
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathsUserState
import com.example.data.model.SpeakingSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

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

enum class DeviceRecordingHint {
    STUDIO_AUDIO,
    STUDIO_VIDEO,
    COACH_PRACTICE
}

data class DeviceMediaFile(
    val path: String,
    val name: String,
    val lastModifiedMs: Long,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val sourceHint: DeviceRecordingHint
)

/**
 * Scans SpeakCraft's app-private audio & video recording directories off the main thread
 * so any previously recorded, offline, or un-analyzed clips are still surfaced in Profile -> Recordings.
 */
object DeviceRecordingsScanner {
    private val audioExtensions = setOf("m4a", "mp3", "wav", "aac", "3gp", "ogg")
    private val videoExtensions = setOf("mp4", "mkv", "webm")

    suspend fun scan(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<DeviceMediaFile> = withContext(dispatcher) {
        val appCtx = context.applicationContext
        scanDirectories(
            recordingsDir = File(appCtx.noBackupFilesDir, "recordings"),
            practiceDir = File(appCtx.filesDir, "practice")
        )
    }

    fun scanDirectories(
        recordingsDir: File?,
        practiceDir: File?
    ): List<DeviceMediaFile> {
        val discovered = mutableListOf<DeviceMediaFile>()

        if (recordingsDir != null && recordingsDir.isDirectory) {
            recordingsDir.listFiles().orEmpty()
                .filter { it.isFile && it.length() > 0L }
                .forEach { file ->
                    val ext = file.extension.lowercase()
                    val isVideo = ext in videoExtensions
                    val isAudio = ext in audioExtensions
                    if (isVideo || isAudio) {
                        discovered += DeviceMediaFile(
                            path = file.absolutePath,
                            name = file.name,
                            lastModifiedMs = file.lastModified(),
                            sizeBytes = file.length(),
                            isVideo = isVideo,
                            sourceHint = if (isVideo) DeviceRecordingHint.STUDIO_VIDEO else DeviceRecordingHint.STUDIO_AUDIO
                        )
                    }
                }
        }

        if (practiceDir != null && practiceDir.isDirectory) {
            practiceDir.listFiles().orEmpty().forEach { child ->
                if (child.isDirectory) {
                    val files = child.listFiles().orEmpty().filter { it.isFile && it.length() > 0L }
                    val best = files.firstOrNull { it.name.equals("replay.mp4", ignoreCase = true) }
                        ?: files.firstOrNull { it.extension.lowercase() in videoExtensions }
                        ?: files.firstOrNull { it.name.equals("audio.m4a", ignoreCase = true) }
                        ?: files.firstOrNull { it.extension.lowercase() in audioExtensions }
                    if (best != null) {
                        val isVideo = best.extension.lowercase() in videoExtensions
                        discovered += DeviceMediaFile(
                            path = best.absolutePath,
                            name = "${child.name}/${best.name}",
                            lastModifiedMs = best.lastModified(),
                            sizeBytes = best.length(),
                            isVideo = isVideo,
                            sourceHint = DeviceRecordingHint.COACH_PRACTICE
                        )
                    }
                } else if (child.isFile && child.length() > 0L) {
                    val ext = child.extension.lowercase()
                    val isVideo = ext in videoExtensions
                    val isAudio = ext in audioExtensions
                    if (isVideo || isAudio) {
                        discovered += DeviceMediaFile(
                            path = child.absolutePath,
                            name = child.name,
                            lastModifiedMs = child.lastModified(),
                            sizeBytes = child.length(),
                            isVideo = isVideo,
                            sourceHint = DeviceRecordingHint.COACH_PRACTICE
                        )
                    }
                }
            }
        }

        return discovered
            .distinctBy { normalizeMediaPath(it.path) }
            .sortedByDescending { it.lastModifiedMs }
    }

    internal fun normalizeMediaPath(path: String): String =
        path.trim().replace('\\', '/')
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
    val backedSpeakingRecordingId: String? = null,
    val backedPracticeRecordingId: String? = null,
    val backedActivityAttemptId: String? = null,
    val backedLearningPathKey: Pair<String, Int>? = null,
    val backedDeviceFilePath: String? = null
)

object UnifiedRecordingsCatalog {

    private fun normalizePath(path: String): String = path.trim().replace('\\', '/')

    private fun claimPath(path: String, claimedPaths: MutableSet<String>) {
        if (path.isBlank()) return
        claimedPaths += normalizePath(path)
        runCatching { File(path).absolutePath }.getOrNull()?.let { abs ->
            claimedPaths += normalizePath(abs)
        }
        runCatching { File(path).canonicalPath }.getOrNull()?.let { can ->
            claimedPaths += normalizePath(can)
        }
    }

    private fun isPathClaimed(path: String, claimedPaths: Set<String>): Boolean {
        if (path.isBlank()) return false
        if (normalizePath(path) in claimedPaths) return true
        val abs = runCatching { File(path).absolutePath }.getOrNull()
        if (abs != null && normalizePath(abs) in claimedPaths) return true
        val can = runCatching { File(path).canonicalPath }.getOrNull()
        return can != null && normalizePath(can) in claimedPaths
    }

    private fun sourceLabelForModule(moduleType: String): String = when (moduleType.uppercase()) {
        "SPEAKING_LAB" -> "Record & Review Studio"
        "CHALLENGES" -> "Speaking Challenge"
        "PRONUNCIATION" -> "Phonetic & Intonation Lab"
        "ARTICULATION" -> "Articulation Lab"
        "SHADOWING", "LISTENING_LAB" -> "Active Listening & Shadowing"
        "GRAMMAR_SPEAKING" -> "Grammar Speaking Lab"
        "PUBLIC_SPEAKING" -> "Public Speaking Lab"
        "VOICE_TRAINING" -> "Voice Training Lab"
        "LESSON_DETAIL", "LESSON_PRACTICE" -> "Lesson Practice"
        else -> moduleType.ifBlank { "Speaking Session" }
    }

    /**
     * Collects all past recorded sessions from:
     * 1) Record & Review Studio and specialized Speaking Labs ([SpeakingSession] enriched with local [SavedSpeakingRecording])
     * 1b) Offline / pre-analysis / un-synced local [SavedSpeakingRecording] entries
     * 2) Coach Practice ([SavedPracticeRecording])
     * 3) Learning Paths daily lessons ([LearningPathsUserState] day audio attachments)
     * 4) Activity Engine 150 ([LocalActivityAttempt] recorded media)
     * 5) Unclaimed on-device audio/video files discovered by [DeviceRecordingsScanner]
     */
    fun collectAll(
        speakingSessions: List<SpeakingSession>,
        learningSnapshot: PersistedLearningSnapshot?,
        learningPathsState: LearningPathsUserState?,
        deviceMediaFiles: List<DeviceMediaFile> = emptyList()
    ): List<UnifiedRecordedSession> {
        val items = mutableListOf<UnifiedRecordedSession>()
        val claimedPaths = mutableSetOf<String>()
        val localSpeakingList = learningSnapshot?.speakingRecordings.orEmpty()
        val matchedLocalIds = mutableSetOf<String>()

        // 1. Speaking Studio & Labs (Firestore sessions enriched with local media/transcript)
        speakingSessions.forEachIndexed { idx, sess ->
            val category = if (sess.moduleType.equals("SPEAKING_LAB", ignoreCase = true)) {
                RecordingSourceCategory.SPEAKING_STUDIO
            } else {
                RecordingSourceCategory.SPEAKING_LABS
            }
            val sourceLabel = sourceLabelForModule(sess.moduleType)
            val ts = sess.createdAt?.toDate()?.time
                ?: sess.updatedAt?.toDate()?.time
                ?: 0L

            val matchedLocal = localSpeakingList.firstOrNull { local ->
                if (local.id in matchedLocalIds) return@firstOrNull false
                when {
                    sess.id.isNotBlank() && local.id == sess.id -> true
                    sess.localMediaPath.isNotBlank() && normalizePath(local.localMediaPath) == normalizePath(sess.localMediaPath) -> true
                    sess.promptTitle.isNotBlank() &&
                        local.promptTitle == sess.promptTitle &&
                        (local.moduleType.equals(sess.moduleType, ignoreCase = true) || local.moduleType.isBlank()) &&
                        (ts == 0L || local.timestampMs == 0L || abs(local.timestampMs - ts) <= 600_000L) -> true
                    else -> false
                }
            }
            if (matchedLocal != null) {
                matchedLocalIds += matchedLocal.id
            }

            var effectiveLocalPath = sess.localMediaPath.ifBlank { matchedLocal?.localMediaPath.orEmpty() }
            val effectiveTranscript = sess.transcript.ifBlank { matchedLocal?.transcript.orEmpty() }
            val effectiveMediaType = sess.mediaType.ifBlank { matchedLocal?.mediaType.orEmpty() }
            val initialIsVid = effectiveMediaType.contains("video", ignoreCase = true) ||
                effectiveLocalPath.endsWith(".mp4", ignoreCase = true)

            // Recover stripped local file for prior Firestore sessions by timestamp proximity
            if (effectiveLocalPath.isBlank() && ts > 0L) {
                val matchedDevice = deviceMediaFiles
                    .filter { file ->
                        file.sourceHint != DeviceRecordingHint.COACH_PRACTICE &&
                            !isPathClaimed(file.path, claimedPaths) &&
                            file.isVideo == initialIsVid &&
                            abs(file.lastModifiedMs - ts) <= 180_000L
                    }
                    .minByOrNull { abs(it.lastModifiedMs - ts) }
                if (matchedDevice != null) {
                    effectiveLocalPath = matchedDevice.path
                }
            }

            claimPath(effectiveLocalPath, claimedPaths)

            val isVid = initialIsVid || effectiveLocalPath.endsWith(".mp4", ignoreCase = true)
            val pills = if (LabAttemptEvaluator.isEngineEvaluated(sess)) {
                buildList {
                    add("On-device feedback")
                    if (sess.wordsPerMinute > 0L) add("${sess.wordsPerMinute} WPM")
                    else if (effectiveTranscript.isBlank()) add(if (isVid) "Video" else "Audio only")
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

            items += UnifiedRecordedSession(
                key = "speak_${sess.id.ifBlank { "${idx}_${sess.promptTitle}_${sess.durationSeconds}" }}",
                title = sess.promptTitle.ifBlank { sourceLabel },
                sourceCategory = category,
                sourceLabel = sourceLabel,
                timestampMs = ts.takeIf { it > 0L } ?: (matchedLocal?.timestampMs ?: 0L),
                durationSeconds = sess.durationSeconds.toInt().coerceAtLeast(matchedLocal?.durationSeconds ?: 0).coerceAtLeast(0),
                localMediaPath = effectiveLocalPath,
                isVideo = isVid,
                summary = sess.overallAssessment.ifBlank { "Saved $sourceLabel recording." },
                transcript = effectiveTranscript,
                badges = pills,
                comparisonNote = comparisonText,
                comparisonPositive = deltaFlu >= 0L,
                backedSpeakingSession = sess,
                backedSpeakingRecordingId = matchedLocal?.id,
                backedDeviceFilePath = effectiveLocalPath.takeIf { it.isNotBlank() }
            )
        }

        // 1b. Local Studio / Lab recordings not yet synced or not yet analyzed
        localSpeakingList.forEach { rec ->
            if (rec.id in matchedLocalIds) return@forEach
            val alreadyClaimed = isPathClaimed(rec.localMediaPath, claimedPaths)
            if (alreadyClaimed) return@forEach
            claimPath(rec.localMediaPath, claimedPaths)

            val category = if (rec.moduleType.equals("SPEAKING_LAB", ignoreCase = true)) {
                RecordingSourceCategory.SPEAKING_STUDIO
            } else {
                RecordingSourceCategory.SPEAKING_LABS
            }
            val sourceLabel = sourceLabelForModule(rec.moduleType)
            val isVid = rec.mediaType.contains("video", ignoreCase = true) ||
                rec.localMediaPath.endsWith(".mp4", ignoreCase = true)
            val badges = if (rec.evaluated) {
                buildList {
                    add("Saved on device")
                    if (rec.fluencyScore > 0) add("Fluency ${rec.fluencyScore}")
                    if (rec.wordsPerMinute > 0) add("${rec.wordsPerMinute} WPM")
                }
            } else {
                listOf(if (isVid) "Saved Video Clip" else "Saved Audio Clip", "Ready on device")
            }

            items += UnifiedRecordedSession(
                key = "local_speak_${rec.id}",
                title = rec.promptTitle.ifBlank { if (isVid) "Recorded Video Take" else "$sourceLabel Take" },
                sourceCategory = category,
                sourceLabel = sourceLabel,
                timestampMs = rec.timestampMs,
                durationSeconds = rec.durationSeconds.coerceAtLeast(0),
                localMediaPath = rec.localMediaPath,
                isVideo = isVid,
                summary = rec.overallAssessment.ifBlank {
                    "Saved ${if (isVid) "video" else "audio"} recording from $sourceLabel."
                },
                transcript = rec.transcript,
                badges = badges,
                backedSpeakingRecordingId = rec.id,
                backedDeviceFilePath = rec.localMediaPath.takeIf { it.isNotBlank() }
            )
        }

        // 2. Coach Practice archived recordings
        learningSnapshot?.practiceRecordings.orEmpty().forEach { rec ->
            claimPath(rec.localMediaPath, claimedPaths)
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
                backedPracticeRecordingId = rec.id,
                backedDeviceFilePath = rec.localMediaPath.takeIf { it.isNotBlank() }
            )
        }

        // 3. Learning Paths recorded speech responses
        learningPathsState?.enrollments?.forEach { (programId, enrollment) ->
            val program = LearningPathCurriculumRegistry.getProgram(programId)
            val programTitle = program?.title ?: enrollment.programTitle.ifBlank { "Learning Path" }
            enrollment.dayRecords.values.forEach { day ->
                val audioPath = day.lastAudioPath
                if (!audioPath.isNullOrBlank()) {
                    claimPath(audioPath, claimedPaths)
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
                        backedLearningPathKey = programId to day.dayNumber,
                        backedDeviceFilePath = audioPath
                    )
                }
            }
        }

        // 4. 150 Activities recorded attempts
        learningSnapshot?.activityAttempts.orEmpty().forEach { att ->
            if (att.recordedMediaPath.isNotBlank()) {
                claimPath(att.recordedMediaPath, claimedPaths)
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
                    backedActivityAttemptId = att.id,
                    backedDeviceFilePath = att.recordedMediaPath
                )
            }
        }

        // 5. Unclaimed on-device audio/video files (prior recordings before indexing, offline drafts, etc.)
        deviceMediaFiles.forEachIndexed { idx, file ->
            if (isPathClaimed(file.path, claimedPaths)) return@forEachIndexed
            claimPath(file.path, claimedPaths)

            val category = when (file.sourceHint) {
                DeviceRecordingHint.COACH_PRACTICE -> RecordingSourceCategory.COACH_PRACTICE
                DeviceRecordingHint.STUDIO_AUDIO, DeviceRecordingHint.STUDIO_VIDEO -> RecordingSourceCategory.SPEAKING_STUDIO
            }
            val sourceLabel = when (file.sourceHint) {
                DeviceRecordingHint.COACH_PRACTICE -> "Coach Practice · On-Device"
                DeviceRecordingHint.STUDIO_VIDEO -> "On-Device Video"
                DeviceRecordingHint.STUDIO_AUDIO -> "On-Device Audio"
            }
            val title = when (file.sourceHint) {
                DeviceRecordingHint.COACH_PRACTICE -> "Coach Practice Clip"
                DeviceRecordingHint.STUDIO_VIDEO -> "Recorded Video Clip"
                DeviceRecordingHint.STUDIO_AUDIO -> "Recorded Audio Clip"
            }

            items += UnifiedRecordedSession(
                key = "dev_${idx}_${file.name}_${file.lastModifiedMs}",
                title = title,
                sourceCategory = category,
                sourceLabel = sourceLabel,
                timestampMs = file.lastModifiedMs,
                durationSeconds = 0,
                localMediaPath = file.path,
                isVideo = file.isVideo,
                summary = "Saved ${if (file.isVideo) "video" else "audio"} clip stored on this device.",
                badges = listOf(if (file.isVideo) "Video" else "Audio", "Saved on Device"),
                backedDeviceFilePath = file.path
            )
        }

        return items
            .sortedWith(compareByDescending<UnifiedRecordedSession> { it.timestampMs }.thenBy { it.title })
    }
}
