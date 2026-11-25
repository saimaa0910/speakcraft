package com.example.data.ai

import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * On-device text coaching coordinator governed centrally by [AISettingsRepository] (ADR-003): conversation,
 * vocabulary and mentor features run strictly on-device via [LocalIntelligenceOrchestrator].
 *
 * Speaking feedback on recordings is not produced here: it comes from the local intelligence engine
 * ([com.example.speech.pipeline.LocalIntelligencePipeline]) — Coach Practice and the practice labs
 * ([com.example.data.evaluation.LabAttemptEvaluator]). Transcripts come only from the platform recogniser;
 * the former local Whisper/Moonshine file transcription was removed (FND-AI-03).
 */
class HybridAiCoachCoordinator(
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val prefsRepo: AppPreferencesRepository? = null,
    private val aiSettingsRepo: AISettingsRepository? = prefsRepo?.let { AISettingsRepository(it) }
) {

    val aiSettingsFlow: Flow<AISettings> =
        aiSettingsRepo?.aiSettingsFlow ?: flowOf(AISettings())

    suspend fun currentAiSettings(): AISettings {
        return runCatching { aiSettingsRepo?.aiSettingsFlow?.first() }.getOrNull() ?: AISettings()
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
