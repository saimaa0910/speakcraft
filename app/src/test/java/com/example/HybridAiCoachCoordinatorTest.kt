package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.cloud.FirebaseAiLogicService
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.model.UserProfile
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
    fun `preferences persist cloud ai coach toggle and model tier`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)

        prefsRepo.setCloudAiEnabled(false)
        prefsRepo.setCloudAiModelTier("balanced")
        val initial = prefsRepo.preferencesFlow.first()
        assertFalse(initial.cloudAiEnabled)
        assertEquals("balanced", initial.cloudAiModelTier)

        prefsRepo.setCloudAiEnabled(true)
        prefsRepo.setCloudAiModelTier("deep")
        val updated = prefsRepo.preferencesFlow.first()
        assertTrue(updated.cloudAiEnabled)
        assertEquals("deep", updated.cloudAiModelTier)

        // Reset cleanly
        prefsRepo.setCloudAiEnabled(false)
        prefsRepo.setCloudAiModelTier("balanced")
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
        prefsRepo.setCloudAiEnabled(false)

        val coordinator = HybridAiCoachCoordinator(
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            cloudService = FirebaseAiLogicService(),
            prefsRepo = prefsRepo,
            appContext = ctx
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
    fun `coordinator propagates authentic failure when cloud ai is enabled without live backend`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        prefsRepo.setCloudAiEnabled(true)

        val coordinator = HybridAiCoachCoordinator(
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            cloudService = FirebaseAiLogicService(),
            prefsRepo = prefsRepo,
            appContext = ctx
        )

        val res = coordinator.evaluateVocabularyUsage(
            word = "articulate",
            meaning = "Express ideas clearly",
            userSentence = "He can articulate complex ideas simply."
        )
        // Must NOT silently mask failure with a fake local success when Cloud AI is explicitly enabled
        assertTrue("Cloud AI call in uninitialized JVM test must return failure transparently", res.isFailure)

        prefsRepo.setCloudAiEnabled(false)
    }
}
