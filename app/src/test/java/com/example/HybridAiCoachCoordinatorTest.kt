package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.cloud.FirebaseAiLogicService
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun `centralized ai settings enforce stage 1 zero cloud policy and persist transcription settings`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val aiSettingsRepo = AISettingsRepository(prefsRepo)

        aiSettingsRepo.setLiveTranscriptionEnabled(false)
        aiSettingsRepo.setCloudAiModelTier("balanced")
        val initial = aiSettingsRepo.aiSettingsFlow.first()
        assertFalse(initial.effectiveCloudAiEnabled)
        assertFalse(initial.liveTranscriptionEnabled)
        assertEquals("balanced", initial.cloudAiModelTier)

        val enableCloudResult = aiSettingsRepo.setCloudAiEnabled(true)
        assertTrue("Stage 1 Zero-Cloud-AI policy must reject enabling Cloud AI", enableCloudResult.isFailure)

        aiSettingsRepo.setLiveTranscriptionEnabled(true)
        aiSettingsRepo.setCloudAiModelTier("deep")
        val updated = aiSettingsRepo.aiSettingsFlow.first()
        assertFalse("effectiveCloudAiEnabled must stay false under Stage 1 policy", updated.effectiveCloudAiEnabled)
        assertTrue("liveTranscriptionEnabled must persist via centralized AISettingsRepository", updated.liveTranscriptionEnabled)
        assertEquals("deep", updated.cloudAiModelTier)

        // Reset cleanly
        aiSettingsRepo.setLiveTranscriptionEnabled(false)
        aiSettingsRepo.setCloudAiModelTier("balanced")
    }

    @Test
    fun `firebase ai service maps model tiers to supported gemini 3 and 4 models`() {
        val service = FirebaseAiLogicService()
        assertEquals("gemini-3.8-flash", service.modelNameForTier("balanced"))
        assertEquals("gemini-4-argon", service.modelNameForTier("deep"))
        assertEquals("gemini-3.5-flash-lite", service.modelNameForTier("fast"))
        assertEquals("gemini-3.8-flash", service.modelNameForTier("unknown"))
    }

    @Test
    fun `coordinator routes to local intelligence when cloud ai is disabled`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val aiSettingsRepo = AISettingsRepository(prefsRepo)
        aiSettingsRepo.setCloudAiEnabled(false)

        val coordinator = HybridAiCoachCoordinator(
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            cloudService = FirebaseAiLogicService(),
            prefsRepo = prefsRepo,
            appContext = ctx,
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

    @Test
    fun `coordinator blocks cloud ai activation and rejects live cloud calls under stage 1 policy`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val aiSettingsRepo = AISettingsRepository(prefsRepo)

        val coordinator = HybridAiCoachCoordinator(
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            cloudService = FirebaseAiLogicService(),
            prefsRepo = prefsRepo,
            appContext = ctx,
            aiSettingsRepo = aiSettingsRepo
        )

        val attempt = coordinator.setCloudAiEnabled(true)
        assertTrue("Enabling Cloud AI must fail transparently in Stage 1", attempt.isFailure)
        assertFalse("Cloud AI must remain disabled", coordinator.isCloudAiEnabled())

        val liveResult = coordinator.startLiveVoiceSession(
            scenario = "Job interviews",
            userLevel = "Intermediate",
            onInputTranscript = {},
            onOutputTranscript = {},
            onGoAway = {},
            onError = {}
        )
        assertTrue("Cloud Live Voice must never run silently under Stage 1 policy", liveResult.isFailure)
    }
}
