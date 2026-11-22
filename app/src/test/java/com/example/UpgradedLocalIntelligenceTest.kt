package com.example

import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityUserResponse
import com.example.data.activity.UniversalEvaluationEngine
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.language.DynamicLocalCoachEngine
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.ai.local.language.LocalVocabularyEvaluator
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpgradedLocalIntelligenceTest {

    @Test
    fun `expanded spoken grammar rules catch high-frequency spoken English errors`() {
        val sample = "I am agree with the plan despite of the delay. " +
            "He suggest me to review all informations and I look forward to meet the team."
        val analysis = LocalLanguageAnalysisEngine.analyze(sample)
        val ruleIds = analysis.grammarIssues.map { it.ruleId }.toSet()

        assertTrue("Should detect 'I am agree' (SPK_001), got $ruleIds", ruleIds.contains("SPK_001"))
        assertTrue("Should detect 'despite of' (SPK_002), got $ruleIds", ruleIds.contains("SPK_002"))
        assertTrue("Should detect 'suggest me to' (SPK_003), got $ruleIds", ruleIds.contains("SPK_003"))
        assertTrue("Should detect 'look forward to meet' (SPK_004), got $ruleIds", ruleIds.contains("SPK_004"))
        assertTrue("Should detect 'informations' (SPK_005), got $ruleIds", ruleIds.contains("SPK_005"))
    }

    @Test
    fun `dynamic local coach analyzes arbitrary pasted user draft with concrete rewrites`() {
        val draft = "I think maybe we can kind of improve the onboarding flow because I am agree that users feel confused."
        val insights = DynamicLocalCoachEngine.analyzeUserDraft(draft, "Product Pitch Draft")

        assertTrue("Draft analysis should provide concrete rewrites", insights.rewrites.isNotEmpty())
        assertTrue("Draft analysis should produce a substantive narrative summary", insights.narrativeSummary.length > 40)
        assertTrue("Draft analysis should recommend a next micro-drill", insights.nextMicroDrill.isNotBlank())
    }

    @Test
    fun `local vocabulary evaluator rewards rich discourse connectors and returns personalized rewrite`() {
        val strong = LocalVocabularyEvaluator.evaluateOwnSentence(
            targetWord = "concise",
            meaning = "Giving a lot of information clearly and in a few words",
            userSentence = "Although the quarterly audit covered twelve systems, our team delivered a concise briefing so that executives could decide immediately."
        )
        assertTrue("Rich contextual sentence should pass", strong.passed)
        assertTrue("Rich contextual sentence should score >= 80, got ${strong.score}", strong.score >= 80)
        assertTrue("Corrected/polished example must be non-blank", strong.naturalImprovedSentence.isNotBlank())
    }

    @Test
    fun `local orchestrator adapts mentor output by role and rewrites pasted drafts`() = runBlocking {
        val orchestrator = LocalIntelligenceOrchestrator()
        val profile = UserProfile(
            userId = "u1",
            displayName = "Sruthi",
            communicationLevel = "Intermediate",
            fluencyScore = 74L,
            pronunciationScore = 72L,
            grammarScore = 78L
        )
        val sessions = listOf(
            SpeakingSession(id = "s2", promptTitle = "Status Update", fluencyScore = 80L, grammarScore = 82L, wordsPerMinute = 142L),
            SpeakingSession(id = "s1", promptTitle = "Intro Pitch", fluencyScore = 70L, grammarScore = 74L, wordsPerMinute = 168L)
        )

        val rewriteReply = orchestrator.askMentorStream(
            profile = profile,
            recentSessions = sessions,
            recentFeedback = emptyList(),
            userQuestion = "Please rewrite this sentence: \"I am agree that despite of the budget we maybe need more time.\"",
            roleKey = "mentor"
        ).toList().joinToString("")

        assertTrue("Mentor draft rewrite should address grammar/hedging", rewriteReply.contains("Rewrite", ignoreCase = true) || rewriteReply.contains("→"))

        val interviewReply = orchestrator.askMentorStream(
            profile = profile,
            recentSessions = sessions,
            recentFeedback = emptyList(),
            userQuestion = "Help me answer why I want this role",
            roleKey = "interviewer"
        ).toList().joinToString("")
        assertTrue("Interviewer role should shape response", interviewReply.isNotBlank())
    }

    @Test
    fun `local conversation turn uses thread memory and avoids repeating prior partner questions`() = runBlocking {
        val orchestrator = LocalIntelligenceOrchestrator()
        val firstTurn = orchestrator.generateConversationTurn(
            scenario = "Job interviews",
            userLevel = "Intermediate",
            historyPairs = emptyList(),
            userMessage = "I redesigned our billing pipeline and cut latency in half."
        ).getOrThrow()

        val secondTurn = orchestrator.generateConversationTurn(
            scenario = "Job interviews",
            userLevel = "Intermediate",
            historyPairs = listOf(
                "user" to "I redesigned our billing pipeline and cut latency in half.",
                "model" to firstTurn
            ),
            userMessage = "We aligned product and engineering on a phased rollout with automated alerts."
        ).getOrThrow()

        assertTrue("First turn should be substantive", firstTurn.length > 20)
        assertTrue("Second turn should differ from first turn", firstTurn != secondTurn)
    }

    @Test
    fun `universal evaluation engine local mode returns rich dynamic rewrites with aiEvaluated false`() = runBlocking {
        val activity = ActivityCatalogue.allActivities.first()
        val engine = UniversalEvaluationEngine(aiClientOverride = null)
        val response = ActivityUserResponse(
            activityId = activity.activityId,
            textResponse = "I am agree that despite of tight deadlines, our engineering team delivered the core milestone because we prioritized clarity and daily standups.",
            durationSeconds = 35
        )
        val result = engine.evaluate(activity, response)
        assertFalse("Local evaluation must set aiEvaluated = false", result.aiEvaluated)
        assertTrue("Overall score should be valid 0..100", result.overallObjectiveScore in 1..100)
        assertTrue("Local evaluation must include corrected examples", result.correctedExamples.isNotEmpty())
        assertTrue("Local evaluation must include explanation", result.explanation.length > 30)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
