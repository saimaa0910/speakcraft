package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityEvaluationResult
import com.example.data.activity.ActivityEvaluatorStrategy
import com.example.data.activity.ActivityUserResponse
import com.example.data.activity.LocalSpeechMetrics
import com.example.data.ai.cloud.FirebaseAiLogicService
import com.example.data.ai.cloud.LiveVoiceHandle
import com.example.data.ai.local.LocalExecutionPolicy
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File

private const val TAG = "HybridAiCoordinator"

/**
 * Dual-engine SpeakCraft AI Coordinator governed centrally by [AISettingsRepository]:
 * - Stage 1 Zero-Cloud-AI Policy ([LocalExecutionPolicy.isLocalOnly] == true):
 *   Cloud AI is strictly UNAVAILABLE and cannot be activated or called silently anywhere.
 * - All speech evaluation runs through [LocalIntelligenceOrchestrator.evaluateSpeechWithReport]
 *   with strict evidence validation and no fabricated scores.
 */
class HybridAiCoachCoordinator(
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val cloudService: FirebaseAiLogicService = FirebaseAiLogicService(),
    private val prefsRepo: AppPreferencesRepository? = null,
    private val appContext: Context? = null,
    private val modelManager: OnDeviceModelManager? = null,
    private val aiSettingsRepo: AISettingsRepository? = prefsRepo?.let { AISettingsRepository(it) },
    private val cloudEnabledOverride: (() -> Boolean)? = null,
    private val modelTierOverride: (() -> String)? = null
) {

    val aiSettingsFlow: Flow<AISettings> =
        aiSettingsRepo?.aiSettingsFlow ?: flowOf(AISettings())

    val cloudAiEnabledFlow: Flow<Boolean> =
        if (LocalExecutionPolicy.isLocalOnly()) {
            flowOf(false)
        } else {
            aiSettingsRepo?.aiSettingsFlow?.map { it.effectiveCloudAiEnabled }
                ?: prefsRepo?.preferencesFlow?.map { it.cloudAiEnabled }
                ?: flowOf(cloudEnabledOverride?.invoke() ?: false)
        }

    val cloudAiModelTierFlow: Flow<String> =
        aiSettingsRepo?.aiSettingsFlow?.map { it.cloudAiModelTier }
            ?: prefsRepo?.preferencesFlow?.map { it.cloudAiModelTier }
            ?: flowOf(modelTierOverride?.invoke() ?: "balanced")

    suspend fun currentAiSettings(): AISettings {
        return runCatching { aiSettingsRepo?.aiSettingsFlow?.first() }.getOrNull() ?: AISettings()
    }

    suspend fun isCloudAiEnabled(): Boolean {
        if (LocalExecutionPolicy.isLocalOnly()) return false
        cloudEnabledOverride?.let { return it() }
        return currentAiSettings().effectiveCloudAiEnabled
    }

    suspend fun activeModelTier(): String {
        modelTierOverride?.let { return it() }
        val prefs = runCatching { prefsRepo?.preferencesFlow?.first() }.getOrNull()
        return prefs?.cloudAiModelTier ?: "balanced"
    }

    suspend fun setCloudAiEnabled(enabled: Boolean): Result<Boolean> {
        val repo = aiSettingsRepo
        if (repo != null) return repo.setCloudAiEnabled(enabled)
        if (LocalExecutionPolicy.isLocalOnly()) {
            prefsRepo?.setCloudAiEnabled(false)
            return if (enabled) {
                Result.failure(
                    IllegalStateException("Cloud AI Coach is unavailable in Stage 1 Zero-Cloud-AI Policy.")
                )
            } else {
                Result.success(false)
            }
        }
        prefsRepo?.setCloudAiEnabled(enabled)
        return Result.success(enabled)
    }

    suspend fun setCloudAiModelTier(tier: String) {
        aiSettingsRepo?.setCloudAiModelTier(tier) ?: prefsRepo?.setCloudAiModelTier(tier)
    }

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String,
        hintPrompt: String = "",
        mediaFile: File? = null
    ): Result<String> {
        if (isCloudAiEnabled()) {
            return cloudService.transcribeAudio(audioBytes, mimeType, hintPrompt)
        }
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
        if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            return cloudService.analyzeSpeechRecording(
                mediaBytes = mediaBytes,
                mimeType = mimeType,
                moduleType = moduleType,
                promptTitle = promptTitle,
                durationSeconds = durationSeconds,
                previousSession = previousSession,
                extraInstructions = extraInstructions,
                localMediaPath = localMediaPath,
                liveTranscript = liveTranscript,
                modelTier = tier
            )
        }
        val report = evaluateSpeechWithReport(
            sessionId = java.util.UUID.randomUUID().toString(),
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
    ): Flow<String> = flow {
        if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            cloudService.askMentorStream(
                profile = profile,
                recentSessions = recentSessions,
                recentFeedback = recentFeedback,
                history = history,
                userQuestion = userQuestion,
                roleKey = roleKey,
                modelTier = tier
            ).collect { emit(it) }
        } else {
            localOrchestrator.askMentorStream(
                profile = profile,
                recentSessions = recentSessions,
                recentFeedback = recentFeedback,
                userQuestion = userQuestion,
                history = history,
                roleKey = roleKey
            ).collect { emit(it) }
        }
    }

    suspend fun generateConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> {
        return if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            cloudService.generateConversationTurn(scenario, userLevel, historyPairs, userMessage, tier)
        } else {
            localOrchestrator.generateConversationTurn(scenario, userLevel, historyPairs, userMessage)
        }
    }

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String
    ): Result<String> {
        return if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            cloudService.generateConversationCheckIn(scenario, transcript, tier)
        } else {
            localOrchestrator.generateConversationCheckIn(scenario, transcript)
        }
    }

    suspend fun startLiveVoiceSession(
        scenario: String,
        userLevel: String,
        onInputTranscript: (String) -> Unit,
        onOutputTranscript: (String) -> Unit,
        onGoAway: (String) -> Unit,
        onError: (Throwable) -> Unit
    ): Result<LiveVoiceHandle> {
        if (!isCloudAiEnabled()) {
            return Result.failure(
                IllegalStateException("Cloud AI Coach is unavailable. Live voice runs offline via on-device speech mode.")
            )
        }
        return cloudService.startLiveVoiceSession(
            scenario = scenario,
            userLevel = userLevel,
            onInputTranscript = onInputTranscript,
            onOutputTranscript = onOutputTranscript,
            onGoAway = onGoAway,
            onError = onError
        )
    }

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String,
        difficulty: String = "Intermediate"
    ): Result<VocabEvaluationResult> {
        return if (isCloudAiEnabled()) {
            cloudService.evaluateVocabularyUsage(word, meaning, userSentence, difficulty)
        } else {
            localOrchestrator.evaluateVocabSentence(word, meaning, userSentence, difficulty)
        }
    }

    suspend fun extractYouTubeLessons(
        videoId: String,
        videoTitle: String,
        videoUrl: String,
        description: String,
        transcriptText: String?
    ): Result<ExtractedYouTubeBundle> {
        return if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            cloudService.extractYouTubeLessons(videoId, videoTitle, videoUrl, description, transcriptText, tier)
        } else {
            localOrchestrator.extractYouTubeLessons(videoId, videoTitle, videoUrl, description, transcriptText)
        }
    }

    suspend fun evaluateUniversalActivityIfCloud(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        metrics: LocalSpeechMetrics
    ): ActivityEvaluationResult? {
        if (!isCloudAiEnabled()) return null
        val tier = activeModelTier()
        val result = cloudService.evaluateUniversalActivity(activity, response, strategy, metrics, tier)
        return result.getOrElse { err ->
            if (err is CancellationException) throw err
            Log.e(TAG, "Cloud activity evaluation error", err)
            throw err
        }
    }
}
