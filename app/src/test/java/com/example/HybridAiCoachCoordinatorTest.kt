package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HybridAiCoachCoordinatorTest {

    @Test
    fun `centralized ai settings persist on-device transcription settings with zero cloud upload`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val aiSettingsRepo = AISettingsRepository(prefsRepo)

        aiSettingsRepo.setLiveTranscriptionEnabled(false)
        val initial = aiSettingsRepo.aiSettingsFlow.first()
        assertFalse(initial.liveTranscriptionEnabled)
        assertFalse(initial.audioVideoUploadedToCloud)

        aiSettingsRepo.setLiveTranscriptionEnabled(true)
        val updated = aiSettingsRepo.aiSettingsFlow.first()
        assertTrue("liveTranscriptionEnabled must persist via centralized AISettingsRepository", updated.liveTranscriptionEnabled)
        assertFalse(updated.audioVideoUploadedToCloud)

        // Reset cleanly
        aiSettingsRepo.setLiveTranscriptionEnabled(false)
    }

    @Test
    fun `coordinator executes conversation, vocabulary, and mentor coaching strictly on-device`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val aiSettingsRepo = AISettingsRepository(prefsRepo)

        val coordinator = HybridAiCoachCoordinator(
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            prefsRepo = prefsRepo,
            aiSettingsRepo = aiSettingsRepo
        )

        val turnRes = coordinator.generateConversationTurn(
            scenario = "Job interviews",
            userLevel = "Intermediate",
            historyPairs = emptyList(),
            userMessage = "I led a mobile architecture refactor that reduced crash rates by forty percent."
        )
        assertTrue("Local conversation turn should succeed", turnRes.isSuccess)
        assertTrue(turnRes.getOrThrow().length > 20)

        val vocabRes = coordinator.evaluateVocabularyUsage(
            word = "articulate",
            meaning = "Express ideas clearly and effectively",
            userSentence = "She was able to articulate the rollout risks clearly because she structured her update in three points."
        )
        assertTrue("Local vocabulary evaluation should succeed", vocabRes.isSuccess)
        assertTrue(vocabRes.getOrThrow().usedCorrectly)

        val chunks = coordinator.askMentorStream(
            profile = UserProfile(userId = "u1", displayName = "Sruthi", fluencyScore = 76L),
            recentSessions = emptyList(),
            recentFeedback = emptyList(),
            history = emptyList(),
            userQuestion = "Create my 7-day plan",
            roleKey = "mentor"
        ).toList()
        assertTrue("Local mentor stream must emit content", chunks.joinToString("").isNotBlank())
    }
}
