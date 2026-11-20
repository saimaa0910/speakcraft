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
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.OnDeviceModelManager
import com.example.data.ai.local.audio.SpeechRecognitionManager
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UserProfile
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
 * Dual-engine SpeakCraft AI Coordinator:
 * - Default (Cloud AI Coach OFF): 100% private on-device [LocalIntelligenceOrchestrator].
 * - Opt-in (Cloud AI Coach ON): Firebase AI Logic via [FirebaseAiLogicService] (no API keys, App Check protected).
 *
 * Strictly preserves error transparency: Cloud AI errors propagate authentically to UI rather than silently falling back.
 */
class HybridAiCoachCoordinator(
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val cloudService: FirebaseAiLogicService = FirebaseAiLogicService(),
    private val prefsRepo: AppPreferencesRepository? = null,
    private val appContext: Context? = null,
    private val modelManager: OnDeviceModelManager? = null,
    private val cloudEnabledOverride: (() -> Boolean)? = null,
    private val modelTierOverride: (() -> String)? = null
) {

    val cloudAiEnabledFlow: Flow<Boolean> =
        prefsRepo?.preferencesFlow?.map { it.cloudAiEnabled }
            ?: flowOf(cloudEnabledOverride?.invoke() ?: false)

    val cloudAiModelTierFlow: Flow<String> =
        prefsRepo?.preferencesFlow?.map { it.cloudAiModelTier }
            ?: flowOf(modelTierOverride?.invoke() ?: "balanced")

    suspend fun isCloudAiEnabled(): Boolean {
        cloudEnabledOverride?.let { return it() }
        val prefs = runCatching { prefsRepo?.preferencesFlow?.first() }.getOrNull()
        return prefs?.cloudAiEnabled ?: false
    }

    suspend fun activeModelTier(): String {
        modelTierOverride?.let { return it() }
        val prefs = runCatching { prefsRepo?.preferencesFlow?.first() }.getOrNull()
        return prefs?.cloudAiModelTier ?: "balanced"
    }

    suspend fun setCloudAiEnabled(enabled: Boolean) {
        prefsRepo?.setCloudAiEnabled(enabled)
    }

    suspend fun setCloudAiModelTier(tier: String) {
        prefsRepo?.setCloudAiModelTier(tier)
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
                        ?: "Local file transcription needs the optional speech model or Live Speech Capture while recording. You can also enable Cloud AI Coach."
                )
            )
        }
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
        return if (isCloudAiEnabled()) {
            val tier = activeModelTier()
            cloudService.analyzeSpeechRecording(
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
        } else {
            localOrchestrator.analyzeSpeechRecording(
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
                IllegalStateException("Cloud AI Coach is disabled. Using on-device Voice Coach mode instead.")
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

    /**
     * Used as `aiClientOverride` for [com.example.data.activity.UniversalEvaluationEngine].
     * Returns null when running in Local Intelligence mode so the upgraded local rubric executes;
     * when Cloud AI Coach is enabled, throws on Cloud failure so the UI surfaces the real error instead of masking it.
     */
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
