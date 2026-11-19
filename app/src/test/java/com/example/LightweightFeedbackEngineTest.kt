package com.example

import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.language.ConfidenceFeedbackEngine
import com.example.data.ai.local.language.FluencyFeedbackEngine
import com.example.data.ai.local.language.GrammarFeedbackEngine
import com.example.data.ai.local.language.GrammarIssue
import com.example.data.ai.local.language.ImprovementRecommendationEngine
import com.example.data.ai.local.language.LocalFeedbackEngine
import com.example.data.ai.local.language.LocalFeedbackEvidence
import com.example.data.ai.local.language.PronunciationFeedbackEngine
import com.example.data.ai.local.language.StructureFeedbackEngine
import com.example.data.ai.local.language.VocabularyAnalysis
import com.example.data.ai.local.language.VocabularyFeedbackEngine
import com.example.data.ai.local.video.FaceAnalysisResult
import com.example.data.ai.local.video.PostureAnalysisResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightweightFeedbackEngineTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Grammar Feedback Engine Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun grammar_engine_outputs_rule_ids_and_reproducible_corrections() {
        val issue = GrammarIssue(
            originalText = "he don't",
            correctedText = "he doesn't",
            issueType = "Subject-Verb Agreement",
            explanation = "Use 'doesn't' with third-person singular subjects ('he').",
            ruleId = "SVA_001",
            confidenceLevel = 0.95f
        )

        val evidence = LocalFeedbackEvidence(
            transcript = "Yesterday he don't go to the office.",
            wordCount = 7,
            grammarIssues = listOf(issue)
        )

        val result = GrammarFeedbackEngine.evaluate(evidence)
        assertEquals(1, result.specificMistakes.size)
        assertTrue(result.specificMistakes[0].contains("[SVA_001]"))
        assertTrue(result.specificMistakes[0].contains("Subject-Verb Agreement"))

        assertEquals(1, result.correctedExamples.size)
        assertTrue(result.correctedExamples[0].contains("Instead of: \"he don't\" → Say: \"he doesn't\" [Rule: SVA_001]"))
        assertNotNull(result.recommendedTask)
    }

    @Test
    fun grammar_engine_never_invents_mistakes_for_clean_speech() {
        val evidence = LocalFeedbackEvidence(
            transcript = "I arrived at the meeting on time and delivered a clear presentation to the client.",
            wordCount = 15,
            grammarIssues = emptyList()
        )

        val result = GrammarFeedbackEngine.evaluate(evidence)
        assertTrue(result.specificMistakes.isEmpty())
        assertTrue(result.correctedExamples.isEmpty())
        assertTrue(result.strengths.isNotEmpty())
        assertTrue(result.strengths[0].contains("zero", ignoreCase = true))
    }

    @Test
    fun grammar_engine_does_not_evaluate_when_transcript_is_empty() {
        val evidence = LocalFeedbackEvidence(transcript = "", wordCount = 0)
        val result = GrammarFeedbackEngine.evaluate(evidence)
        assertTrue(result.specificMistakes.isEmpty())
        assertTrue(result.strengths.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Fluency Feedback Engine Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun fluency_engine_evaluates_pacing_and_pause_rhythm() {
        // Rushed delivery (> 175 WPM)
        val rushedEvidence = LocalFeedbackEvidence(
            transcript = "We must immediately launch this product right now without waiting for anything else.",
            wordsPerMinute = 195,
            speechDurationSeconds = 4.0
        )
        val rushedResult = FluencyFeedbackEngine.evaluate(rushedEvidence)
        assertTrue(rushedResult.specificMistakes.any { it.contains("fast") || it.contains("195 WPM") })
        assertTrue(rushedResult.suggestions.any { it.contains("Rate Contrast") })

        // Optimal delivery (120-170 WPM)
        val optimalEvidence = LocalFeedbackEvidence(
            transcript = "Clear communication is about cadence and connection with your listeners.",
            wordsPerMinute = 145,
            speechDurationSeconds = 5.0,
            pauseCount = 3,
            meanPauseDurationMs = 650.0
        )
        val optimalResult = FluencyFeedbackEngine.evaluate(optimalEvidence)
        assertTrue(optimalResult.strengths.any { it.contains("Optimal speaking pace") })
        assertTrue(optimalResult.strengths.any { it.contains("Natural pause rhythm") })

        // Fillers detected
        val fillerEvidence = LocalFeedbackEvidence(
            transcript = "So um basically we should start.",
            fillerCount = 2,
            detectedFillers = listOf("um (1)", "basically (1)")
        )
        val fillerResult = FluencyFeedbackEngine.evaluate(fillerEvidence)
        assertTrue(fillerResult.specificMistakes.any { it.contains("filler word") })
        assertTrue(fillerResult.suggestions.any { it.contains("Zero-Filler 60s Sprint") })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Vocabulary Feedback Engine Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun vocabulary_engine_evaluates_diversity_and_repeated_expressions() {
        val vocab = VocabularyAnalysis(
            targetWordsUsed = listOf("articulate", "concise"),
            repeatedWords = mapOf("very" to 4, "thing" to 3),
            vocabularyDiversityScore = 28,
            suggestions = emptyList()
        )

        val evidence = LocalFeedbackEvidence(
            transcript = "It was a very very good thing and another very good thing.",
            wordCount = 35,
            vocabularyAnalysis = vocab
        )

        val result = VocabularyFeedbackEngine.evaluate(evidence)
        assertTrue(result.strengths.any { it.contains("target vocabulary") })
        assertTrue(result.specificMistakes.any { it.contains("repeated words") })
        assertTrue(result.specificMistakes.any { it.contains("Limited lexical diversity") })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Pronunciation Feedback Engine Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun pronunciation_engine_handles_expected_prompts_and_accent_tolerance() {
        // Expected vs recognized with Indian English accent tolerance
        val evidence = LocalFeedbackEvidence(
            expectedText = "This thing is very good",
            transcript = "Dis thing is wery good",
            wordCount = 5
        )

        val result = PronunciationFeedbackEngine.evaluate(evidence)
        assertNotNull(result)
        // Accent variants like /th/ -> /d/ and /v/ <-> /w/ should be recognized intelligibly
        assertTrue(result.strengths.any { it.contains("accent variants recognized", ignoreCase = true) })
    }

    @Test
    fun pronunciation_engine_detects_clipping_in_freeform_speech() {
        val audio = AcousticFeatures(
            durationSeconds = 10.0,
            clippingDetected = true,
            speechSilenceRatio = 1.2
        )
        val evidence = LocalFeedbackEvidence(
            transcript = "Freeform speaking without reference text.",
            audioFeatures = audio
        )

        val result = PronunciationFeedbackEngine.evaluate(evidence)
        assertTrue(result.specificMistakes.any { it.contains("clipping", ignoreCase = true) })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Confidence Feedback Engine Tests (Non-Psychological Invariant)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun confidence_engine_strictly_evaluates_vocal_projection_and_posture_without_personality_claims() {
        val audio = AcousticFeatures(
            durationSeconds = 8.0,
            rmsVolumeDb = -18.5, // strong projection
            clippingDetected = false,
            pitchVariationHz = 32.0 // dynamic melody
        )
        val face = FaceAnalysisResult(
            framesAnalyzed = 20,
            faceDetectionRate = 0.90f
        )
        val posture = PostureAnalysisResult(
            framesAnalyzed = 20,
            bodyDetectedFrames = 18,
            headTiltAlert = false
        )

        val evidence = LocalFeedbackEvidence(
            audioFeatures = audio,
            faceAnalysis = face,
            postureAnalysis = posture
        )

        val result = ConfidenceFeedbackEngine.evaluate(evidence)

        // Must praise physical vocal projection and camera alignment
        assertTrue(result.strengths.any { it.contains("vocal projection", ignoreCase = true) })
        assertTrue(result.strengths.any { it.contains("camera presence", ignoreCase = true) })
        assertTrue(result.strengths.any { it.contains("Upright postural alignment", ignoreCase = true) })

        // Strictly verify that output does NOT make psychological personality claims
        val allText = (result.strengths + result.specificMistakes + result.suggestions).joinToString(" ")
        assertFalse("Must not claim to evaluate personality", allText.contains("personality", ignoreCase = true))
        assertFalse("Must not claim introversion/extroversion", allText.contains("introvert", ignoreCase = true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Structure Feedback Engine Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun structure_engine_flags_run_on_sentences_and_recognizes_signposts() {
        val runOnText = "We arrived at the facility early in the morning and we waited outside for three hours until the manager arrived and then we spoke about the project and nobody had the key so we had to call security."
        val evidence = LocalFeedbackEvidence(
            transcript = runOnText,
            wordCount = 37
        )

        val result = StructureFeedbackEngine.evaluate(evidence)
        assertTrue(result.specificMistakes.any { it.contains("run-on", ignoreCase = true) })
        assertTrue(result.suggestions.any { it.contains("PREP", ignoreCase = true) })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Master LocalFeedbackEngine Tests (All 6 Canonical Sections)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun local_feedback_engine_generates_all_six_canonical_sections_with_honesty_notice() {
        val evidence = LocalFeedbackEvidence(
            transcript = "He don't agree with the proposal because it is too expensive.",
            expectedText = null,
            wordCount = 11,
            wordsPerMinute = 130,
            fillerCount = 0,
            pauseCount = 2,
            meanPauseDurationMs = 700.0,
            speechDurationSeconds = 5.2,
            grammarIssues = listOf(
                GrammarIssue(
                    originalText = "he don't",
                    correctedText = "he doesn't",
                    issueType = "Subject-Verb Agreement",
                    explanation = "Use 'doesn't' with third-person singular subjects ('he').",
                    ruleId = "SVA_001"
                )
            ),
            exerciseTitle = "Daily Standup Practice",
            evaluatorName = "SpeakingEvaluator"
        )

        val feedback = LocalFeedbackEngine.buildFeedback(evidence)

        // Section 1: What Went Well
        assertNotNull(feedback.whatWentWell)
        assertTrue("Section 1 must not be empty", feedback.whatWentWell.isNotEmpty())

        // Section 2: Specific Mistakes
        assertNotNull(feedback.specificMistakes)
        assertTrue("Section 2 must contain the SVA rule mistake", feedback.specificMistakes.any { it.contains("[SVA_001]") })

        // Section 3: Corrected Examples
        assertNotNull(feedback.correctedExamples)
        assertTrue("Section 3 must contain corrected example", feedback.correctedExamples.any { it.contains("Instead of:") })

        // Section 4: Explanation
        assertNotNull(feedback.explanation)
        assertTrue("Section 4 must be substantive coaching rationale", feedback.explanation.length >= 30)

        // Section 5: Improvement Suggestions
        assertNotNull(feedback.improvementSuggestions)
        assertTrue("Section 5 must provide actionable drills", feedback.improvementSuggestions.isNotEmpty())

        // Section 6: Next Practice Task
        assertNotNull(feedback.nextPracticeTask)
        assertTrue("Section 6 must be non-blank next activity", feedback.nextPracticeTask.isNotBlank())

        // Honesty notice
        assertNotNull(feedback.honestyNotice)
        assertTrue(feedback.honestyNotice.contains("Zero cloud AI", ignoreCase = true) || feedback.honestyNotice.contains("No cloud AI", ignoreCase = true))
    }

    @Test
    fun audio_only_recording_evaluates_delivery_without_hallucinating_grammar_errors() {
        val audio = AcousticFeatures(
            durationSeconds = 12.0,
            rmsVolumeDb = -21.0,
            clippingDetected = false,
            speakingRateConsistencyScore = 0.82
        )
        val evidence = LocalFeedbackEvidence(
            transcript = "", // Audio-only mode: no transcript available
            wordCount = 0,
            audioFeatures = audio,
            speechDurationSeconds = 12.0,
            processingMode = "Audio-Only DSP Engine"
        )

        val feedback = LocalFeedbackEngine.buildFeedback(evidence)

        // Must NOT hallucinate grammar mistakes or quotes
        assertFalse(feedback.specificMistakes.any { it.contains("grammar", ignoreCase = true) })
        assertFalse(feedback.specificMistakes.any { it.contains("you said", ignoreCase = true) })

        // Must still provide vocal delivery strengths and actionable pacing guidance
        assertTrue(feedback.whatWentWell.isNotEmpty())
        assertTrue(feedback.improvementSuggestions.isNotEmpty())
        assertTrue(feedback.nextPracticeTask.isNotBlank())
        assertTrue(feedback.honestyNotice.contains("on-device", ignoreCase = true))
    }
}
