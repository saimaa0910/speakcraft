package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.time.DateKeyProvider
import com.example.core.logging.AppLogger
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.UserActivityProgress
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.ai.local.audio.LiveTranscriptionState
import com.example.data.firebase.ActivityFirestoreRepository
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.learning.LearningGoal
import com.example.data.learning.LearningLevel
import com.example.data.learning.LearningProgressManager
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathEvaluationEngine
import com.example.data.learningpaths.LearningPathFeedback
import com.example.data.learningpaths.LearningPathLesson
import com.example.data.learningpaths.LearningPathPersistenceRepository
import com.example.data.learningpaths.LearningPathPracticeLinker
import com.example.data.learningpaths.LearningPathProgram
import com.example.data.learningpaths.LearningPathRecommendationEngine
import com.example.data.learningpaths.LearningPathsUserState
import com.example.data.learningpaths.LessonSubmissionRequest
import com.example.data.learningpaths.ModuleAssessmentEvaluation
import com.example.data.learningpaths.ModuleAssessmentSpec
import com.example.data.learningpaths.ModuleAssessmentSubmission
import com.example.data.learningpaths.ProgramPracticeEvidence
import com.example.data.learningpaths.ProgramRecommendation
import com.example.data.learningpaths.ProgramUserStats
import com.example.data.media.AudioRecorderManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.ProgramEnrollment
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import com.example.speech.pipeline.TranscriptAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "LearningPathsVM"

data class LearningPathsUiState(
    val userId: String = "guest",
    val programs: List<LearningPathProgram> = LearningPathCurriculumRegistry.allPrograms,
    val userState: LearningPathsUserState = LearningPathsUserState(),
    val programStats: Map<String, ProgramUserStats> = emptyMap(),
    val recommendations: List<ProgramRecommendation> = emptyList(),
    val selectedCategoryFilter: String = "All",
    val searchQuery: String = "",
    val previewProgramId: String? = null,
    // Lesson Interactive Workspace
    val activeLessonProgramId: String? = null,
    val activeLessonDayNumber: Int? = null,
    val draftResponseText: String = "",
    val draftQuizAnswers: Map<String, Int> = emptyMap(),
    val draftChecklistIndices: Set<Int> = emptySet(),
    val draftConfidenceRating: Int = 3,
    val draftReflectionNote: String = "",
    val activeFeedback: LearningPathFeedback? = null,
    val lessonSessionStartEpochMs: Long = 0L,
    val isEvaluating: Boolean = false,
    // Audio & Live Transcript
    val isRecordingAudio: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingSeconds: Int = 0,
    val recordingAmplitude: Float = 0f,
    val attachedAudioPath: String? = null,
    val isPlayingAudio: Boolean = false,
    val liveTranscriptionState: LiveTranscriptionState = LiveTranscriptionState(),
    // Module / Capstone Assessment
    val activeAssessmentProgramId: String? = null,
    val activeAssessmentModuleId: String? = null,
    val assessmentScenarioAnswers: Map<String, Int> = emptyMap(),
    val assessmentPracticalText: String = "",
    val assessmentConfidenceRating: Int = 4,
    val assessmentSessionStartEpochMs: Long = 0L,
    val activeAssessmentResult: ModuleAssessmentEvaluation? = null,
    val isGradingAssessment: Boolean = false,
    // Transient notification message
    val bannerMessage: String? = null,
    /** Practice outside lessons that counts toward each program (drills, Coach Practice, daily tasks). */
    val practiceEvidence: Map<String, ProgramPracticeEvidence> = emptyMap()
)

class LearningPathsViewModel(
    private val persistenceRepository: LearningPathPersistenceRepository,
    private val speakCraftRepository: SpeakCraftRepository? = null,
    private val activityRepository: ActivityFirestoreRepository? = null,
    private val learningProgressManager: LearningProgressManager? = null,
    private val audioManager: AudioRecorderManager? = null,
    private val liveTranscriptionManager: LiveTranscriptionManager? = null,
    private val ttsHelper: TtsAndSpeechHelper? = null,
    private val aiSettingsRepository: AISettingsRepository? = null,
    private val dateKeyProvider: DateKeyProvider? = null,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningPathsUiState())
    val uiState: StateFlow<LearningPathsUiState> = _uiState.asStateFlow()

    private var latestAiSettings: AISettings = AISettings()
    private var latestProfile: UserProfile? = null
    private var lastSyncedAutoTranscript: String = ""
    private var lastSyncedSignature: String? = null

    init {
        learningProgressManager?.let { manager ->
            viewModelScope.launch {
                manager.state.collectLatest { snapshot ->
                    val evidence = withContext(computeDispatcher) {
                        LearningPathPracticeLinker.evidenceForAll(LearningPathCurriculumRegistry.allPrograms, snapshot)
                    }
                    _uiState.update { it.copy(practiceEvidence = evidence) }
                }
            }
        }
        aiSettingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.aiSettingsFlow.collectLatest { settings ->
                    latestAiSettings = settings
                    liveTranscriptionManager?.refreshFromSettings(settings)
                }
            }
        }
        audioManager?.let { recorder ->
            viewModelScope.launch {
                recorder.isRecording.collectLatest { rec ->
                    _uiState.update { it.copy(isRecordingAudio = rec) }
                }
            }
            viewModelScope.launch {
                recorder.isPaused.collectLatest { paused ->
                    _uiState.update { it.copy(isRecordingPaused = paused) }
                }
            }
            viewModelScope.launch {
                recorder.elapsedSeconds.collectLatest { sec ->
                    _uiState.update { it.copy(recordingSeconds = sec) }
                }
            }
            viewModelScope.launch {
                recorder.amplitude.collectLatest { amp ->
                    _uiState.update { it.copy(recordingAmplitude = amp) }
                }
            }
            viewModelScope.launch {
                recorder.isPlaying.collectLatest { playing ->
                    _uiState.update { it.copy(isPlayingAudio = playing) }
                }
            }
        }
        liveTranscriptionManager?.let { manager ->
            viewModelScope.launch {
                manager.state.collectLatest { st ->
                    _uiState.update { current ->
                        val recognized = st.displayText.trim()
                        val shouldMergeTranscript = current.isRecordingAudio &&
                            recognized.isNotBlank() &&
                            (current.draftResponseText.isBlank() || current.draftResponseText == lastSyncedAutoTranscript)
                        if (shouldMergeTranscript) {
                            lastSyncedAutoTranscript = recognized
                        }
                        current.copy(
                            liveTranscriptionState = st,
                            draftResponseText = if (shouldMergeTranscript) recognized else current.draftResponseText
                        )
                    }
                }
            }
        }
    }

    fun syncAccountAndEnrollments(
        userId: String,
        profile: UserProfile?,
        remoteEnrollments: List<ProgramEnrollment>
    ) {
        latestProfile = profile
        val cleanUid = userId.ifBlank { "guest" }
        val signature = buildSyncSignature(cleanUid, profile, remoteEnrollments)
        if (lastSyncedSignature == signature &&
            _uiState.value.userId == cleanUid &&
            _uiState.value.programStats.isNotEmpty()
        ) {
            return
        }
        viewModelScope.launch {
            val syncedState = if (remoteEnrollments.isNotEmpty()) {
                persistenceRepository.mergeRemoteEnrollments(cleanUid, remoteEnrollments)
            } else {
                persistenceRepository.loadUserState(cleanUid)
            }
            lastSyncedSignature = signature
            publishUserState(cleanUid, syncedState, profile)
        }
    }

    private fun buildSyncSignature(
        cleanUid: String,
        profile: UserProfile?,
        remoteEnrollments: List<ProgramEnrollment>
    ): String {
        val enrollmentHash = remoteEnrollments.map {
            "${it.programId}:${it.currentDay}:${it.completedDays.size}:${it.active}"
        }.hashCode()
        return listOf(
            cleanUid,
            profile?.communicationLevel.orEmpty(),
            profile?.primaryGoal.orEmpty(),
            profile?.fluencyScore?.toInt() ?: -1,
            profile?.pronunciationScore?.toInt() ?: -1,
            profile?.vocabularyScore?.toInt() ?: -1,
            profile?.grammarScore?.toInt() ?: -1,
            profile?.articulationScore?.toInt() ?: -1,
            enrollmentHash
        ).joinToString("|")
    }

    fun setCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openPreview(programId: String?) {
        _uiState.update { it.copy(previewProgramId = programId) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun clearDayAudioRecording(programId: String, dayNumber: Int) {
        val uid = _uiState.value.userId
        val existingPath = _uiState.value.userState.enrollments[programId]?.dayRecords?.get(dayNumber)?.lastAudioPath
        viewModelScope.launch {
            if (!existingPath.isNullOrBlank()) {
                audioManager?.deleteFile(existingPath)
            }
            val updated = persistenceRepository.clearDayAudioRecording(uid, programId, dayNumber)
            publishUserState(uid, updated, latestProfile)
        }
    }

    fun enrollInProgram(programId: String) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val uid = _uiState.value.userId
        viewModelScope.launch {
            val updated = persistenceRepository.enroll(uid, program)
            publishUserState(uid, updated, latestProfile)
            _uiState.update {
                it.copy(
                    previewProgramId = null,
                    bannerMessage = "Enrolled in '${program.title}'. Day 1 is ready!"
                )
            }
            speakCraftRepository?.let { repo ->
                runCatching { repo.enrollInProgram(program.programId, program.title) }
                    .onFailure { AppLogger.w(TAG, "Remote enroll sync deferred offline", it) }
            }
        }
    }

    fun togglePauseProgram(programId: String) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val uid = _uiState.value.userId
        val currentlyPaused = _uiState.value.userState.enrollments[programId]?.paused == true
        val targetPaused = !currentlyPaused
        viewModelScope.launch {
            val updated = persistenceRepository.setPaused(uid, programId, targetPaused)
            publishUserState(uid, updated, latestProfile)
            _uiState.update {
                it.copy(
                    bannerMessage = if (targetPaused) {
                        "Paused '${program.title}'. Your progress stays saved."
                    } else {
                        "Resumed '${program.title}'."
                    }
                )
            }
            speakCraftRepository?.let { repo ->
                runCatching { repo.setProgramActive(programId, !targetPaused) }
                    .onFailure { AppLogger.w(TAG, "Remote pause sync deferred offline", it) }
            }
        }
    }

    fun unenrollProgram(programId: String) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val uid = _uiState.value.userId
        viewModelScope.launch {
            val updated = persistenceRepository.unenroll(uid, programId)
            publishUserState(uid, updated, latestProfile)
            _uiState.update {
                it.copy(
                    previewProgramId = null,
                    bannerMessage = "Removed '${program.title}' from active programs."
                )
            }
            speakCraftRepository?.let { repo ->
                runCatching { repo.setProgramActive(programId, false) }
                    .onFailure { AppLogger.w(TAG, "Remote unenroll sync deferred offline", it) }
            }
        }
    }

    fun toggleDayCompletionQuick(programId: String, dayNumber: Int) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val uid = _uiState.value.userId
        viewModelScope.launch {
            val updated = persistenceRepository.toggleDayCompletionManual(uid, program, dayNumber)
            publishUserState(uid, updated, latestProfile)
            val record = updated.enrollments[programId]
            if (record?.completedDays?.contains(dayNumber) == true) {
                val lessonTitle = LearningPathCurriculumRegistry.getLesson(programId, dayNumber)?.title.orEmpty()
                learningProgressManager?.recordLearningPathDayMarked(program.programId, program.title, dayNumber, lessonTitle)
            }
            speakCraftRepository?.let { repo ->
                runCatching {
                    repo.syncProgramEnrollment(
                        programId = program.programId,
                        programTitle = program.title,
                        currentDay = (record?.currentDay ?: 1).toLong(),
                        completedDays = record?.completedDays?.map { it.toLong() }.orEmpty(),
                        active = !(record?.paused ?: false)
                    )
                }.onFailure { AppLogger.w(TAG, "Remote day toggle sync deferred offline", it) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interactive Lesson Workspace
    // -------------------------------------------------------------------------

    fun prepareLessonSession(programId: String, dayNumber: Int) {
        val current = _uiState.value
        if (current.activeLessonProgramId == programId && current.activeLessonDayNumber == dayNumber) {
            return
        }
        val lesson = LearningPathCurriculumRegistry.getLesson(programId, dayNumber) ?: return
        val existingDay = current.userState.enrollments[programId]?.dayRecords?.get(dayNumber)
        lastSyncedAutoTranscript = ""

        val restoredFeedback = if (existingDay != null && existingDay.attempts > 0) {
            val insights = TranscriptAnalyzer.analyze(existingDay.lastSubmissionText)
            LearningPathFeedback(
                overallScore = existingDay.lastScore,
                passed = existingDay.completed,
                confidenceSelfRating = existingDay.confidenceSelfRating.coerceIn(1, 5),
                quizScorePercent = existingDay.quizScorePercent,
                quizCorrectCount = lesson.quizQuestions.count { q ->
                    existingDay.completedQuizAnswers[q.questionId] == q.correctIndex
                },
                quizTotalCount = lesson.quizQuestions.size,
                whatWentWell = existingDay.whatWentWell,
                mistakesFound = existingDay.mistakesFound,
                correctedExamples = existingDay.correctedExamples,
                explanation = existingDay.coachSummary,
                coachTips = existingDay.coachTips,
                transcriptSummary = TranscriptAnalyzer.composeSummary(insights),
                transcriptInsights = insights,
                rubricBreakdown = emptyList(),
                recommendedNextStep = existingDay.recommendedNextStep,
                evidenceNote = "Restored from your saved session."
            )
        } else null

        liveTranscriptionManager?.prepareNewSession(UUID.randomUUID().toString(), latestAiSettings)

        _uiState.update {
            it.copy(
                activeLessonProgramId = programId,
                activeLessonDayNumber = dayNumber,
                draftResponseText = existingDay?.lastSubmissionText.orEmpty(),
                draftQuizAnswers = existingDay?.completedQuizAnswers.orEmpty(),
                draftChecklistIndices = existingDay?.completedChecklistIndices?.toSet().orEmpty(),
                draftConfidenceRating = existingDay?.confidenceSelfRating?.takeIf { r -> r in 1..5 } ?: 3,
                draftReflectionNote = existingDay?.reflectionNote.orEmpty(),
                activeFeedback = restoredFeedback,
                lessonSessionStartEpochMs = System.currentTimeMillis(),
                isEvaluating = false,
                attachedAudioPath = existingDay?.lastAudioPath
            )
        }
    }

    fun updateDraftResponseText(text: String) {
        _uiState.update { it.copy(draftResponseText = text) }
    }

    fun selectQuizOption(questionId: String, optionIndex: Int) {
        _uiState.update { current ->
            current.copy(draftQuizAnswers = current.draftQuizAnswers + (questionId to optionIndex))
        }
    }

    fun toggleChecklistItem(index: Int) {
        _uiState.update { current ->
            val updated = if (current.draftChecklistIndices.contains(index)) {
                current.draftChecklistIndices - index
            } else {
                current.draftChecklistIndices + index
            }
            current.copy(draftChecklistIndices = updated)
        }
    }

    fun setConfidenceRating(rating: Int) {
        _uiState.update { it.copy(draftConfidenceRating = rating.coerceIn(1, 5)) }
    }

    fun updateReflectionNote(note: String) {
        _uiState.update { it.copy(draftReflectionNote = note) }
    }

    fun saveReflectionNoteNow(programId: String, dayNumber: Int) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val lesson = LearningPathCurriculumRegistry.getLesson(programId, dayNumber) ?: return
        val note = _uiState.value.draftReflectionNote
        val uid = _uiState.value.userId
        viewModelScope.launch {
            val updated = persistenceRepository.saveLessonReflectionNote(uid, program, lesson, note)
            publishUserState(uid, updated, latestProfile)
            _uiState.update { it.copy(bannerMessage = "Reflection note saved locally.") }
        }
    }

    fun playModelScript(script: String) {
        ttsHelper?.speak(script, rate = 0.96f)
    }

    fun stopModelAudio() {
        ttsHelper?.stopSpeaking()
    }

    fun startOrResumeLessonRecording() {
        val recorder = audioManager ?: return
        if (recorder.isPaused.value) {
            recorder.resumeRecording()
            liveTranscriptionManager?.startTranscription(settings = latestAiSettings)
            return
        }
        val sessionId = UUID.randomUUID().toString()
        lastSyncedAutoTranscript = ""
        val started = recorder.startRecording(viewModelScope)
        if (started) {
            liveTranscriptionManager?.prepareNewSession(sessionId, latestAiSettings)
            liveTranscriptionManager?.startTranscription(sessionId, latestAiSettings)
        } else {
            _uiState.update {
                it.copy(bannerMessage = "Microphone unavailable right now—type or paste your response below.")
            }
        }
    }

    fun pauseLessonRecording() {
        audioManager?.pauseRecording()
        liveTranscriptionManager?.stopTranscription()
    }

    fun stopLessonRecording() {
        liveTranscriptionManager?.stopTranscription()
        val recordedFile = audioManager?.stopRecording()
        val sttText = liveTranscriptionManager?.state?.value?.displayText?.trim().orEmpty()
        _uiState.update { current ->
            val updatedDraft = if (sttText.isNotBlank() && current.draftResponseText.isBlank()) {
                sttText
            } else current.draftResponseText
            current.copy(
                attachedAudioPath = recordedFile?.absolutePath ?: current.attachedAudioPath,
                draftResponseText = updatedDraft
            )
        }
    }

    fun toggleRecordedPlayback() {
        val recorder = audioManager ?: return
        if (recorder.isPlaying.value) {
            recorder.stopPlayback()
        } else {
            val path = _uiState.value.attachedAudioPath
            if (!path.isNullOrBlank()) {
                recorder.playFile(path)
            } else {
                recorder.startPlayback()
            }
        }
    }

    fun submitAndEvaluateLesson(programId: String, dayNumber: Int) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val lesson = LearningPathCurriculumRegistry.getLesson(programId, dayNumber) ?: return
        val current = _uiState.value
        if (current.isEvaluating) return

        // Stop active recording cleanly if user hit Evaluate directly
        if (current.isRecordingAudio) {
            stopLessonRecording()
        }

        val snapshot = _uiState.value
        val elapsedSec = ((System.currentTimeMillis() - snapshot.lessonSessionStartEpochMs) / 1000L)
            .toInt()
            .coerceIn(snapshot.recordingSeconds, 3600)

        val request = LessonSubmissionRequest(
            programId = programId,
            dayNumber = dayNumber,
            spokenOrWrittenText = snapshot.draftResponseText,
            audioFilePath = snapshot.attachedAudioPath,
            audioDurationSeconds = snapshot.recordingSeconds,
            quizAnswers = snapshot.draftQuizAnswers,
            completedChecklistIndices = snapshot.draftChecklistIndices,
            confidenceSelfRating = snapshot.draftConfidenceRating,
            reflectionNote = snapshot.draftReflectionNote,
            practiceDurationSeconds = elapsedSec
        )

        _uiState.update { it.copy(isEvaluating = true) }

        viewModelScope.launch {
            val feedback = withContext(computeDispatcher) {
                LearningPathEvaluationEngine.evaluateLesson(lesson, request)
            }
            val uid = _uiState.value.userId
            val updatedState = persistenceRepository.recordLessonSubmission(
                userId = uid,
                program = program,
                lesson = lesson,
                request = request,
                feedback = feedback
            )
            publishUserState(uid, updatedState, latestProfile)

            _uiState.update {
                it.copy(
                    isEvaluating = false,
                    activeFeedback = feedback,
                    bannerMessage = if (feedback.passed) {
                        "Day ${lesson.dayNumber} completed (${feedback.overallScore}%)!"
                    } else {
                        "Feedback ready (${feedback.overallScore}%). Refine and retry to unlock Day ${lesson.dayNumber}."
                    }
                )
            }

            syncAfterLessonEvaluation(program, lesson, feedback, updatedState)
        }
    }

    // -------------------------------------------------------------------------
    // Module & Capstone Assessments
    // -------------------------------------------------------------------------

    fun prepareModuleAssessment(programId: String, moduleId: String) {
        val current = _uiState.value
        if (current.activeAssessmentProgramId == programId && current.activeAssessmentModuleId == moduleId) {
            return
        }
        val existingRecord = current.userState.enrollments[programId]?.moduleAssessments?.get(moduleId)
        _uiState.update {
            it.copy(
                activeAssessmentProgramId = programId,
                activeAssessmentModuleId = moduleId,
                assessmentScenarioAnswers = emptyMap(),
                assessmentPracticalText = existingRecord?.lastResponseText.orEmpty(),
                assessmentConfidenceRating = 4,
                assessmentSessionStartEpochMs = System.currentTimeMillis(),
                activeAssessmentResult = null,
                isGradingAssessment = false
            )
        }
    }

    fun selectAssessmentOption(questionId: String, optionIndex: Int) {
        _uiState.update { current ->
            current.copy(assessmentScenarioAnswers = current.assessmentScenarioAnswers + (questionId to optionIndex))
        }
    }

    fun updateAssessmentPracticalText(text: String) {
        _uiState.update { it.copy(assessmentPracticalText = text) }
    }

    fun setAssessmentConfidence(rating: Int) {
        _uiState.update { it.copy(assessmentConfidenceRating = rating.coerceIn(1, 5)) }
    }

    fun submitModuleAssessment(programId: String, moduleId: String) {
        val program = LearningPathCurriculumRegistry.getProgram(programId) ?: return
        val module = LearningPathCurriculumRegistry.getModule(programId, moduleId) ?: return
        val spec = module.assessment
        val snapshot = _uiState.value
        if (snapshot.isGradingAssessment) return

        val elapsedSec = ((System.currentTimeMillis() - snapshot.assessmentSessionStartEpochMs) / 1000L)
            .toInt()
            .coerceIn(5, 3600)

        val submission = ModuleAssessmentSubmission(
            programId = programId,
            moduleId = moduleId,
            scenarioAnswers = snapshot.assessmentScenarioAnswers,
            practicalResponseText = snapshot.assessmentPracticalText,
            confidenceSelfRating = snapshot.assessmentConfidenceRating,
            practiceDurationSeconds = elapsedSec
        )

        _uiState.update { it.copy(isGradingAssessment = true) }

        viewModelScope.launch {
            val evaluation = withContext(computeDispatcher) {
                LearningPathEvaluationEngine.evaluateModuleAssessment(spec, submission)
            }
            val uid = _uiState.value.userId
            val updated = persistenceRepository.recordModuleAssessment(
                userId = uid,
                program = program,
                spec = spec,
                evaluation = evaluation,
                submission = submission
            )
            publishUserState(uid, updated, latestProfile)

            _uiState.update {
                it.copy(
                    isGradingAssessment = false,
                    activeAssessmentResult = evaluation,
                    bannerMessage = if (evaluation.passed) {
                        if (spec.isCapstone) {
                            "Capstone passed (${evaluation.combinedScore}%)! Program mastery unlocked."
                        } else {
                            "${module.title} passed (${evaluation.combinedScore}%)!"
                        }
                    } else {
                        "Checkpoint scored ${evaluation.combinedScore}% (need ${spec.passingScoreThreshold}%). Review tips & retake."
                    }
                )
            }

            // Daily record gets every graded attempt; a pass also counts once toward points, streak and tasks.
            learningProgressManager?.recordLearningPathAssessmentAttempt(
                programId = program.programId,
                programTitle = program.title,
                assessmentId = spec.assessmentId,
                moduleId = spec.moduleId,
                title = spec.title,
                isCapstone = spec.isCapstone,
                combinedScore = evaluation.combinedScore,
                passed = evaluation.passed,
                minutesSpent = (elapsedSec / 60).coerceAtLeast(2),
                skillTag = program.primarySkillFocus.firstOrNull() ?: "Fluency"
            )
        }
    }

    private suspend fun syncAfterLessonEvaluation(
        program: LearningPathProgram,
        lesson: LearningPathLesson,
        feedback: LearningPathFeedback,
        updatedState: LearningPathsUserState
    ) {
        val enrollmentRecord = updatedState.enrollments[program.programId]
        if (enrollmentRecord != null) {
            speakCraftRepository?.let { repo ->
                runCatching {
                    repo.syncProgramEnrollment(
                        programId = program.programId,
                        programTitle = program.title,
                        currentDay = enrollmentRecord.currentDay.toLong(),
                        completedDays = enrollmentRecord.completedDays.map { it.toLong() },
                        active = !enrollmentRecord.paused
                    )
                }.onFailure { AppLogger.w(TAG, "Remote enrollment sync deferred offline", it) }
            }
        }

        // Daily record gets every evaluated attempt; a pass also counts once toward points, streak and tasks.
        learningProgressManager?.recordLearningPathLessonAttempt(
            programId = program.programId,
            programTitle = program.title,
            lessonId = lesson.lessonId,
            dayNumber = lesson.dayNumber,
            lessonTitle = lesson.title,
            score = feedback.overallScore,
            passed = feedback.passed,
            attemptNumber = enrollmentRecord?.dayRecords?.get(lesson.dayNumber)?.attempts ?: 1,
            minutesSpent = lesson.estimatedMinutes,
            skillTag = program.primarySkillFocus.firstOrNull() ?: "Fluency"
        )

        if (feedback.passed) {
            // Bidirectionally cross-update linked ActivityCatalogue progress cleanly
            val linkedActivity = ActivityCatalogue.getActivityById(lesson.linkedActivityId)
            val actRepo = activityRepository
            if (linkedActivity != null && actRepo != null) {
                runCatching {
                    val uid = _uiState.value.userId
                    actRepo.saveActivityProgress(
                        progress = UserActivityProgress(
                            id = linkedActivity.activityId,
                            userId = uid,
                            activityId = linkedActivity.activityId,
                            moduleId = linkedActivity.moduleId,
                            skillCategory = linkedActivity.skillCategory.label,
                            completed = true,
                            attemptsCount = 1L,
                            bestObjectiveScore = feedback.overallScore.toLong(),
                            latestObjectiveScore = feedback.overallScore.toLong(),
                            latestConfidenceSelfRating = feedback.confidenceSelfRating.toLong()
                        ),
                        isUpdate = false
                    )
                }.onFailure {
                    AppLogger.w(TAG, "Activity progress cross-sync deferred offline", it)
                }
            }
        }
    }

    private suspend fun publishUserState(
        userId: String,
        userState: LearningPathsUserState,
        profile: UserProfile?
    ) {
        val (statsMap, recs) = withContext(computeDispatcher) {
            val programs = LearningPathCurriculumRegistry.allPrograms
            val stats = programs.associate { program ->
                program.programId to LearningPathRecommendationEngine.computeProgramStats(
                    program = program,
                    enrollment = userState.enrollments[program.programId]
                )
            }

            val level = LearningLevel.fromLabel(profile?.communicationLevel)
            val goal = LearningGoal.fromLabel(profile?.primaryGoal)
            val weakestSkill = profile?.let { p ->
                listOf(
                    "Fluency" to p.fluencyScore,
                    "Pronunciation" to p.pronunciationScore,
                    "Vocabulary" to p.vocabularyScore,
                    "Grammar" to p.grammarScore,
                    "Articulation" to p.articulationScore
                ).minByOrNull { it.second }?.first
            }

            val recommendations = LearningPathRecommendationEngine.recommendPrograms(
                programs = programs,
                userLevel = level,
                userGoal = goal,
                userState = userState,
                weakestSkillHint = weakestSkill
            )
            stats to recommendations
        }

        _uiState.update {
            it.copy(
                userId = userId,
                userState = userState,
                programStats = statsMap,
                recommendations = recs
            )
        }
    }
}
