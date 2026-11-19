package com.example

import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.fusion.MultimodalFeatureFusionEngine
import com.example.data.ai.local.language.DeterministicFeedbackTemplates
import com.example.data.ai.local.language.LocalFeedbackEvidence
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLanguageAndOrchestratorTest {

    @Test
    fun local_language_engine_flags_common_grammar_issues_and_computes_diversity() {
        val problematicText = "He don't know about a apple because I can't not tell him. Today is good and yesterday was bad."
        val analysis = LocalLanguageAnalysisEngine.analyze(
            text = problematicText,
            targetWords = listOf("apple", "know")
        )

        assertNotNull(analysis)
        assertTrue(analysis.grammarIssues.isNotEmpty())

        val issueTypes = analysis.grammarIssues.map { it.issueType }
        assertTrue("Expected subject-verb agreement or article issue", issueTypes.isNotEmpty())

        val vocab = analysis.vocabularyAnalysis
        assertTrue(vocab.targetWordsUsed.contains("apple") || vocab.targetWordsUsed.contains("know"))
        assertTrue(vocab.vocabularyDiversityScore in 1..100)
    }

    @Test
    fun deterministic_feedback_templates_populate_all_six_sections() {
        val evidence = LocalFeedbackEvidence(
            transcript = "I think that we should like focus on this project because it is very important.",
            expectedText = null,
            wordCount = 15,
            wordsPerMinute = 140,
            fillerCount = 1,
            detectedFillers = listOf("like (1)"),
            pauseCount = 3,
            meanPauseDurationMs = 600.0,
            speechDurationSeconds = 6.5,
            grammarIssues = emptyList(),
            vocabularyAnalysis = null,
            audioFeatures = null,
            faceAnalysis = null,
            postureAnalysis = null,
            gestureAnalysis = null,
            exerciseTitle = "Impromptu Intro",
            evaluatorName = "SpeakingEvaluator",
            limitationNotices = listOf("Deterministic on-device evaluation active."),
            processingMode = "Local Rule-Based Engine"
        )

        val feedback = DeterministicFeedbackTemplates.buildFeedback(evidence)

        // All 6 canonical sections must be present and non-empty
        assertNotNull(feedback.whatWentWell)
        assertTrue("Section 1 (What Went Well) must have items", feedback.whatWentWell.isNotEmpty())

        assertNotNull(feedback.specificMistakes)
        assertTrue("Section 2 (Specific Mistakes) must have items", feedback.specificMistakes.isNotEmpty())

        assertNotNull(feedback.correctedExamples)

        assertNotNull(feedback.explanation)
        assertTrue("Section 4 (Explanation) must be populated", feedback.explanation.isNotBlank())

        assertNotNull(feedback.improvementSuggestions)
        assertTrue("Section 5 (Suggestions) must have items", feedback.improvementSuggestions.isNotEmpty())

        assertNotNull(feedback.nextPracticeTask)
        assertTrue("Section 6 (Next Practice Task) must be populated", feedback.nextPracticeTask.isNotBlank())

        // Honesty notice must be transparent
        assertTrue(feedback.honestyNotice.isNotBlank())
    }

    @Test
    fun multimodal_fusion_engine_integrates_features_safely() {
        val evidence = MultimodalFeatureFusionEngine.fuse(
            audio = null,
            face = null,
            posture = null,
            gesture = null,
            language = null,
            transcript = "Practice makes progress in communication.",
            expectedText = "Practice makes progress in communication.",
            evaluatorName = "SpeakingEvaluator",
            exerciseTitle = "Fluency Drill"
        )

        assertEquals(5, evidence.wordCount)
        assertEquals("SpeakingEvaluator", evidence.evaluatorName)
        assertEquals("Fluency Drill", evidence.exerciseTitle)
        assertTrue(evidence.limitationNotices.isNotEmpty())
    }

    @Test
    fun local_orchestrator_runs_offline_conversation_turns_and_vocab_evaluation() = runTest {
        val orchestrator = LocalIntelligenceOrchestrator()

        // 1. Vocabulary evaluation without cloud
        val vocabResult = orchestrator.evaluateVocabSentence(
            word = "articulate",
            definition = "able to express ideas clearly",
            userSentence = "She is an articulate speaker who explains concepts with clarity.",
            difficulty = "Intermediate"
        )
        assertTrue(vocabResult.isSuccess)
        val vocab = vocabResult.getOrThrow()
        assertTrue(vocab.usedCorrectly)
        assertTrue(vocab.naturalnessScore in 50..100)
        assertTrue(vocab.feedback.isNotBlank())

        // 2. Conversation turn from local script bank
        val turnResult = orchestrator.generateConversationTurn(
            scenario = "Job interviews",
            userLevel = "Intermediate",
            historyPairs = emptyList(),
            userMessage = "I have five years of experience in product management."
        )
        assertTrue(turnResult.isSuccess)
        val reply = turnResult.getOrThrow()
        assertTrue("Reply must be realistic dialogue", reply.isNotBlank())

        // 3. Mentor streaming without cloud
        val profile = UserProfile(
            displayName = "Alex",
            communicationLevel = "Intermediate",
            fluencyScore = 55, // weakest score
            pronunciationScore = 75
        )
        val chunks = orchestrator.askMentorStream(
            profile = profile,
            recentSessions = emptyList(),
            recentFeedback = emptyList(),
            userQuestion = "How do I improve my fluency and pace?"
        ).toList()

        assertTrue(chunks.isNotEmpty())
        val fullAnswer = chunks.joinToString("")
        assertTrue(fullAnswer.contains("fluency", ignoreCase = true) || fullAnswer.contains("pace", ignoreCase = true))
    }
}
