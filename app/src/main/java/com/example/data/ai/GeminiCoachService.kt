package com.example.data.ai

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

data class ExtractedYouTubeBundle(
    val mainTopic: String,
    val category: String,
    val difficulty: String,
    val techniques: List<ExtractedTechnique>
)

data class VocabEvaluationResult(
    val usedCorrectly: Boolean,
    val naturalnessScore: Int,
    val feedback: String,
    val improvedExample: String
)

@Deprecated("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
class GeminiCoachService {

    suspend fun analyzeSpeechRecording(
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = ""
    ): Result<SpeakingSession> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    suspend fun extractYouTubeLessons(
        videoId: String,
        videoTitle: String,
        videoUrl: String,
        description: String,
        transcriptText: String?
    ): Result<ExtractedYouTubeBundle> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String
    ): Result<VocabEvaluationResult> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    suspend fun sendConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    suspend fun generateConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String
    ): Result<String> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback> = emptyList(),
        userQuestion: String
    ): Flow<String> {
        throw UnsupportedOperationException("GeminiCoachService is deprecated. Use LocalIntelligenceOrchestrator instead.")
    }
}
