package com.example.data.ai

import android.content.Context
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.OnDeviceModelManager
import com.example.data.ai.local.audio.SpeechRecognitionManager
import com.example.data.evaluation.CoachEvaluationReport
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.InsufficientSpeechEvidenceException
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.util.UUID

/**
 * On-device SpeakCraft Intelligence Coordinator governed centrally by [AISettingsRepository] (ADR-003).
 * All speech analysis, transcription, coaching, conversation, and vocabulary evaluation execute
 * strictly on-device via [LocalIntelligenceOrchestrator] with evidence-based scoring.
 */
class HybridAiCoachCoordinator(
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val prefsRepo: AppPreferencesRepository? = null,
    private val appContext: Context? = null,
    private val modelManager: OnDeviceModelManager? = null,
    private val aiSettingsRepo: AISettingsRepository? = prefsRepo?.let { AISettingsRepository(it) }
) {

    val aiSettingsFlow: Flow<AISettings> =
        aiSettingsRepo?.aiSettingsFlow ?: flowOf(AISettings())

    suspend fun currentAiSettings(): AISettings {
        return runCatching { aiSettingsRepo?.aiSettingsFlow?.first() }.getOrNull() ?: AISettings()
    }

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String,
        hintPrompt: String = "",
        mediaFile: File? = null
    ): Result<String> {
        val settings = currentAiSettings()
        if (!settings.localSpeechRecognitionEnabled) {
            return Result.failure(
                IllegalStateException("On-device speech recognition is disabled in App Settings → AI & Intelligence.")
            )
        }
        val targetFile = mediaFile ?: if (audioBytes.isNotEmpty() && appContext != null) {
            File.createTempFile("sc_transcribe_", ".m4a", appContext.cacheDir).apply {
                writeBytes(audioBytes)
            }
        } else null

        if (targetFile == null || !targetFile.exists()) {
            return Result.failure(
                IllegalStateException("Record or attach an audio clip first before transcribing.")
            )
        }
        val speechManager = SpeechRecognitionManager(context = appContext, modelManager = modelManager)
        val res = speechManager.transcribe(targetFile)
        if (mediaFile == null) {
            runCatching { targetFile.delete() }
        }
        return if (res.transcript.isNotBlank()) {
            Result.success(res.transcript.trim())
        } else {
            Result.failure(
                IllegalStateException(
                    res.limitationNotice?.takeIf { it.isNotBlank() }
                        ?: "No recognizable speech detected in this audio clip."
                )
            )
        }
    }

    suspend fun evaluateSpeechWithReport(
        sessionId: String,
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = "",
        liveTranscript: String = "",
        transcriptSessionId: String? = sessionId,
        asrConfidence: Float = 0.85f,
        videoAssessmentSelected: Boolean = mimeType.startsWith("video/", ignoreCase = true)
    ): CoachEvaluationReport {
        val settings = currentAiSettings()
        return localOrchestrator.evaluateSpeechWithReport(
            sessionId = sessionId,
            mediaBytes = mediaBytes,
            mimeType = mimeType,
            moduleType = moduleType,
            promptTitle = promptTitle,
            durationSeconds = durationSeconds,
            previousSession = previousSession,
            extraInstructions = extraInstructions,
            localMediaPath = localMediaPath,
            liveTranscript = liveTranscript,
            transcriptSessionId = transcriptSessionId,
            asrConfidence = asrConfidence,
            videoAssessmentSelected = videoAssessmentSelected && settings.localVideoAnalysisEnabled,
            enableSpeechRecognition = settings.localSpeechRecognitionEnabled,
            enableAudioAnalysis = settings.localAudioAnalysisEnabled && settings.localAiEnabled,
            enableVideoAnalysis = settings.localVideoAnalysisEnabled
        )
    }

    suspend fun analyzeSpeechRecording(
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = "",
        liveTranscript: String = "",
        asrConfidence: Float = 0.85f
    ): Result<SpeakingSession> {
        val report = evaluateSpeechWithReport(
            sessionId = UUID.randomUUID().toString(),
            mediaBytes = mediaBytes,
            mimeType = mimeType,
            moduleType = moduleType,
            promptTitle = promptTitle,
            durationSeconds = durationSeconds,
            previousSession = previousSession,
            extraInstructions = extraInstructions,
            localMediaPath = localMediaPath,
            liveTranscript = liveTranscript,
            asrConfidence = asrConfidence
        )
        return if (report.evaluationState == CoachEvaluationState.COMPLETED && report.session != null) {
            Result.success(report.session)
        } else {
            Result.failure(InsufficientSpeechEvidenceException(report))
        }
    }

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback> = emptyList(),
        history: List<Pair<String, String>> = emptyList(),
        userQuestion: String,
        roleKey: String = "mentor"
    ): Flow<String> = localOrchestrator.askMentorStream(
        profile = profile,
        recentSessions = recentSessions,
        recentFeedback = recentFeedback,
        userQuestion = userQuestion,
        history = history,
        roleKey = roleKey
    )

    suspend fun generateConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> = localOrchestrator.generateConversationTurn(scenario, userLevel, historyPairs, userMessage)

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String
    ): Result<String> = localOrchestrator.generateConversationCheckIn(scenario, transcript)

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String,
        difficulty: String = "Intermediate"
    ): Result<VocabEvaluationResult> = localOrchestrator.evaluateVocabSentence(word, meaning, userSentence, difficulty)
}
