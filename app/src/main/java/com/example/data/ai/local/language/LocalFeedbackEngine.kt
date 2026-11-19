package com.example.data.ai.local.language

import com.example.data.ai.local.audio.AlignmentType
import com.example.data.ai.local.audio.PronunciationAnalysisEngine
import com.example.data.ai.local.audio.WordAlignmentOp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Sub-Engine Evaluation Output
// ─────────────────────────────────────────────────────────────────────────────

data class SubEngineResult(
    val strengths: List<String> = emptyList(),
    val specificMistakes: List<String> = emptyList(),
    val correctedExamples: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val explanationSnippets: List<String> = emptyList(),
    val recommendedTask: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Grammar Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Evaluates grammar issues detected deterministically by [LocalLanguageAnalysisEngine].
 * Strictly ties every mistake to a Rule ID and evidence; never invents an error.
 */
object GrammarFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        // If transcript is blank (e.g. audio-only mode), do not evaluate grammar
        if (evidence.transcript.isBlank()) {
            return SubEngineResult()
        }

        val issues = evidence.grammarIssues
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val corrections = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        if (issues.isNotEmpty()) {
            for (issue in issues.take(3)) {
                mistakes.add("[${issue.ruleId}] ${issue.issueType}: ${issue.explanation}")
                corrections.add("Instead of: \"${issue.originalText}\" → Say: \"${issue.correctedText}\" [Rule: ${issue.ruleId}]")
            }
            suggestions.add("Practice re-recording this prompt using the exact corrected phrasing shown above.")
            explanations.add("Clean grammatical agreement builds immediate professional credibility with your listener.")
        } else if (evidence.wordCount >= 12) {
            strengths.add("Strong grammatical accuracy: zero subject-verb, tense, article, or preposition errors detected across ${evidence.wordCount} words.")
        }

        val recommendedTask = if (issues.isNotEmpty()) {
            "Complete a Sentence Construction drill focusing on ${issues.first().issueType}."
        } else null

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = corrections,
            suggestions = suggestions,
            explanationSnippets = explanations,
            recommendedTask = recommendedTask
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Fluency Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Evaluates speaking rate, pause distribution (short vs long), speech continuity,
 * and filler word frequency using DSP metrics and disfluency detectors.
 */
object FluencyFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val corrections = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val wpm = evidence.wordsPerMinute
        val duration = evidence.speechDurationSeconds
        val fillers = evidence.fillerCount
        val audio = evidence.audioFeatures

        // 1. Speaking Rate (Optimal: 120–170 WPM)
        if (wpm in 120..170) {
            strengths.add("Optimal speaking pace ($wpm WPM) within the standard conversational bracket (120–170 WPM).")
        } else if (wpm > 175) {
            mistakes.add("Speaking pace is fast ($wpm WPM). Speaking over 175 WPM makes it difficult for listeners to retain complex ideas.")
            suggestions.add("Practice Rate Contrast: deliver your context briskly, then slow down 20% on the core conclusion.")
            explanations.add("Vinh Giang emphasizes that rate variation commands attention; slowing down on key takeaways signals weight and authority.")
        } else if (wpm in 1..99 && duration >= 6.0) {
            mistakes.add("Speaking pace is slow ($wpm WPM). A rate under 100 WPM can cause listener engagement to drop.")
            suggestions.add("Practice phrase-linking drills: group 3–5 words together in a single breath before pausing.")
            explanations.add("Conversational momentum depends on rhythmic phrasing rather than isolated single-word delivery.")
        }

        // 2. Pause Control & Distribution
        val longPauses = audio?.longPauseCount ?: if (evidence.meanPauseDurationMs > 1500.0) evidence.pauseCount else 0
        val shortPauses = audio?.shortPauseCount ?: (evidence.pauseCount - longPauses).coerceAtLeast(0)

        if (evidence.pauseCount in 2..8 && evidence.meanPauseDurationMs in 400.0..1800.0) {
            strengths.add("Natural pause rhythm: used intentional pauses (mean: ${(evidence.meanPauseDurationMs / 1000.0).roundToOneDecimal()}s) to frame thoughts.")
        } else if (longPauses >= 4) {
            mistakes.add("Detected $longPauses prolonged hesitation pauses (>750ms). Hesitation pauses interrupt the listener's mental flow.")
            suggestions.add("Replace hesitation stalls with deliberate 1-second silent breaths: think during the pause, speak with confidence.")
        } else if (evidence.pauseCount < 2 && duration >= 12.0) {
            mistakes.add("Very few pauses detected across ${duration.roundToInt()}s of speech. Continuous unpaused delivery reduces listener comprehension.")
            suggestions.add("Adopt the 'Period Pause' rule: whenever a sentence ends, hold a 1-second silent pause before starting the next.")
        }

        // 3. Filler Word Discipline
        if (fillers > 0) {
            val breakdown = if (evidence.detectedFillers.isNotEmpty()) {
                " (${evidence.detectedFillers.take(3).joinToString(", ")})"
            } else ""
            mistakes.add("$fillers filler word(s) detected$breakdown.")
            val firstFiller = evidence.detectedFillers.firstOrNull()?.replace(Regex("\\s*\\(\\d+\\)"), "") ?: "um"
            corrections.add("Instead of: \"...so, $firstFiller, what we found was...\" → Say: \"...so [1-second silent pause] what we found was...\"")
            suggestions.add("Run the Zero-Filler 60s Sprint: whenever you feel a filler forming, gently touch your lips together and pause silently.")
            explanations.add("Replacing filler vocalizations with silent pauses transforms anxious energy into executive presence.")
        } else if (evidence.wordCount >= 15) {
            strengths.add("Clean vocal delivery: zero filler words detected throughout your response.")
        }

        // 4. Syllable Flow / Continuity
        if (audio != null && audio.speakingRateConsistencyScore >= 0.75) {
            strengths.add("Steady syllable cadence: speaking rate consistency scored ${(audio.speakingRateConsistencyScore * 100).roundToInt()}%.")
        }

        val recommendedTask = when {
            fillers >= 3 -> "Run the Zero-Filler 60s Sprint in Communication Challenges."
            wpm > 175    -> "Practice the 5 Vocal Foundations: Rate of Speech drill in Voice Training Lab."
            else         -> null
        }

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = corrections,
            suggestions = suggestions,
            explanationSnippets = explanations,
            recommendedTask = recommendedTask
        )
    }

    private fun Double.roundToOneDecimal(): String = "%.1f".format(this)
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Vocabulary Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Evaluates lexical variety, target word usage, and repetitive verbal habits.
 */
object VocabularyFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        if (evidence.transcript.isBlank()) return SubEngineResult()

        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val vocab = evidence.vocabularyAnalysis

        if (vocab != null) {
            // Lexical Diversity
            if (vocab.vocabularyDiversityScore >= 50) {
                strengths.add("Rich vocabulary diversity (${vocab.vocabularyDiversityScore}% unique words) showing varied lexical expression.")
            } else if (vocab.vocabularyDiversityScore < 32 && evidence.wordCount >= 30) {
                mistakes.add("Limited lexical diversity (${vocab.vocabularyDiversityScore}% unique words). Several base words were heavily recycled.")
                suggestions.add("Incorporate descriptive power verbs and sensory adjectives to elevate repetitive phrasing.")
            }

            // Target Vocabulary
            if (vocab.targetWordsUsed.isNotEmpty()) {
                strengths.add("Successfully incorporated target vocabulary: ${vocab.targetWordsUsed.joinToString(", ")}.")
            }

            // Repeated Words
            if (vocab.repeatedWords.isNotEmpty()) {
                val topRepeated = vocab.repeatedWords.entries.sortedByDescending { it.value }.take(2)
                mistakes.add("Frequently repeated words: ${topRepeated.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }}.")
                suggestions.add("Substitute frequent words like \"${topRepeated.first().key}\" with synonyms from the SpeakCraft Word Bank.")
                explanations.add("Expanding your active vocabulary keeps listeners engaged and prevents rhetorical fatigue.")
            }
        }

        val recommendedTask = if (vocab != null && vocab.repeatedWords.isNotEmpty()) {
            "Explore the Vocabulary Expansion module and practice swapping 3 repeated words."
        } else null

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            suggestions = suggestions,
            explanationSnippets = explanations,
            recommendedTask = recommendedTask
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Pronunciation Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Layered pronunciation and articulation evaluation.
 *
 * Implements strict scientific transparency:
 * - When expected text is present: runs word alignment matrix, identifies substitutions/deletions,
 *   and applies Indian English accent tolerance without punitive scoring.
 * - When freeform: evaluates acoustic clarity (clipping, speech/silence ratio) and clearly
 *   labels as transcript-based intelligibility rather than acoustic phoneme accuracy.
 */
object PronunciationFeedbackEngine {

    private val pronunciationEngine = PronunciationAnalysisEngine()

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val corrections = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val expected = evidence.expectedText
        val transcript = evidence.transcript
        val audio = evidence.audioFeatures

        if (!expected.isNullOrBlank() && transcript.isNotBlank()) {
            // Layered word alignment analysis
            val result = pronunciationEngine.analyze(expected, transcript)

            if (result.matchScore >= 0.85f) {
                strengths.add("High articulation alignment (${(result.matchScore * 100).roundToInt()}% word accuracy) with the prompt text.")
            }

            // Accent tolerance notification
            val accentVariants = result.alignmentOperations.filter { it.type == AlignmentType.ACCENT_VARIANT }
            if (accentVariants.isNotEmpty()) {
                val sample = accentVariants.take(2).mapNotNull { it.recognizedWord }.joinToString(", ")
                strengths.add("Spoken accent variants recognized clearly ($sample) — natural intelligibility preserved.")
            }

            // Mispronounced / Substituted words
            if (result.likelyMispronounced.isNotEmpty()) {
                mistakes.add("Words requiring clearer articulation: ${result.likelyMispronounced.take(3).joinToString(", ")}.")
                for (word in result.likelyMispronounced.take(2)) {
                    corrections.add("Focus drill: enunciate both starting and ending consonants clearly for \"$word\".")
                }
                suggestions.add("Practice minimal pairs in the Articulation Lab to sharpen consonant contrasts.")
            }

            explanations.add("Clear consonant enunciation ensures your message is immediately understood across diverse listener backgrounds.")
        } else {
            // Freeform acoustic clarity assessment
            if (audio != null) {
                if (audio.clippingDetected) {
                    mistakes.add("Audio clipping detected: microphone input peaked and distorted during louder passages.")
                    suggestions.add("Position your microphone 6–8 inches from your mouth and maintain an even speaking distance.")
                } else if (audio.speechSilenceRatio in 0.8..3.0) {
                    strengths.add("Good acoustic voice projection with balanced speech-to-silence ratio (${audio.speechSilenceRatio.roundToOneDecimal()}).")
                }
            }
        }

        val recommendedTask = if (!expected.isNullOrBlank()) {
            "Practice minimal pairs in the Articulation Lab to refine consonant articulation."
        } else null

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = corrections,
            suggestions = suggestions,
            explanationSnippets = explanations,
            recommendedTask = recommendedTask
        )
    }

    private fun Double.roundToOneDecimal(): String = "%.1f".format(this)
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Confidence Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Evaluates vocal projection and physical composure strictly using objective evidence.
 *
 * **STRICT INVARIANT:** Does NOT claim pitch, volume, or facial expressions directly measure
 * internal psychological confidence or personality. Uses objective physical terms:
 * "Vocal projection", "Audibility", "Camera alignment", and "Physical composure".
 */
object ConfidenceFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val audio = evidence.audioFeatures
        val face = evidence.faceAnalysis
        val posture = evidence.postureAnalysis
        val gesture = evidence.gestureAnalysis

        // 1. Objective Vocal Projection (RMS Energy & Dynamic Range)
        if (audio != null) {
            val rms = audio.rmsVolumeDb
            if (rms > -22.0 && !audio.clippingDetected) {
                strengths.add("Strong vocal projection (${rms.roundToInt()} dB RMS) with steady diaphragmatic breath support.")
            } else if (rms < -34.0) {
                mistakes.add("Low vocal projection (average ${rms.roundToInt()} dB). Soft delivery makes it difficult for listeners to follow in larger spaces.")
                suggestions.add("Support your voice from the diaphragm and project toward the back of the room.")
                explanations.add("Vocal projection is a learned physical habit; diaphragmatic breathing gives your voice resonance without straining.")
            }

            if (audio.pitchVariationHz >= 25.0) {
                strengths.add("Dynamic vocal melody: pitch variation (${audio.pitchVariationHz.roundToInt()} Hz range) avoided monotonous delivery.")
            }
        }

        // 2. Physical Composure & Camera Presence (if video is analyzed)
        if (face != null && face.framesAnalyzed > 0) {
            if (face.faceDetectionRate >= 0.8f) {
                strengths.add("Consistent camera presence: face remained well-centered across ${(face.faceDetectionRate * 100).roundToInt()}% of frames.")
            } else if (face.faceDetectionRate < 0.55f) {
                mistakes.add("Face moved out of central frame in ${((1.0f - face.faceDetectionRate) * 100).roundToInt()}% of video frames.")
                suggestions.add("Position your camera at eye level and sit squarely facing the lens.")
            }
        }

        if (posture != null && posture.bodyDetectedFrames > 0) {
            if (posture.headTiltAlert) {
                mistakes.add("Noticeable lateral head tilt during delivery. Keeping your head level communicates composure.")
                suggestions.add("Practice with a level chin: imagine balancing a book to establish neutral neck posture.")
            } else {
                strengths.add("Upright postural alignment maintained throughout the recording.")
            }
        }

        if (gesture != null && gesture.framesAnalyzed > 5) {
            if (gesture.handMovementDetected) {
                strengths.add("Active hand gestures used to visually underline verbal emphasis.")
            } else {
                suggestions.add("Bring your hands into the 'speaker box' (between waist and chest) to visually punctuate key points.")
            }
        }

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            suggestions = suggestions,
            explanationSnippets = explanations
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Structure Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Evaluates structural coherence, sentence length calibration, and rhetorical frameworks
 * (e.g. PREP: Point, Reason, Example, Point).
 */
object StructureFeedbackEngine {

    private val TRANSITION_KEYWORDS = listOf(
        "first", "second", "third", "furthermore", "however", "therefore",
        "for example", "for instance", "in conclusion", "to summarize",
        "on the other hand", "the reason is", "specifically", "as a result"
    )

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineResult {
        if (evidence.transcript.isBlank()) return SubEngineResult()

        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        val explanations = mutableListOf<String>()

        val text = evidence.transcript
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = text.split(Regex("[.!?]+\\s*")).filter { it.isNotBlank() }
        val sentenceCount = sentences.size.coerceAtLeast(1)
        val avgSentenceLength = words.size.toDouble() / sentenceCount

        // Sentence Calibration
        if (avgSentenceLength in 10.0..22.0 && sentenceCount >= 2) {
            strengths.add("Well-calibrated sentence length (average ${avgSentenceLength.roundToInt()} words/sentence) facilitating clear listener comprehension.")
        } else if (avgSentenceLength > 28.0) {
            mistakes.add("Sentences tended toward run-on length (average ${avgSentenceLength.roundToInt()} words/sentence). Complex long sentences risk losing listener attention.")
            suggestions.add("Break extended thoughts into shorter, punchy sentences: aim for 12–18 words per sentence.")
            explanations.add("Vinh Giang's CLEAR framework relies on concise thought packets that let the audience process one idea at a time.")
        }

        // Structural Signposting
        val detectedTransitions = TRANSITION_KEYWORDS.filter { keyword ->
            text.contains(Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE))
        }

        if (detectedTransitions.isNotEmpty()) {
            strengths.add("Effective logical signposting: utilized transition words (${detectedTransitions.take(3).joinToString(", ")}).")
        } else if (words.size >= 45) {
            suggestions.add("Organize your response using explicit signposts: 'First...', 'The main reason is...', and 'In summary...'.")
        }

        val recommendedTask = if (avgSentenceLength > 28.0 || detectedTransitions.isEmpty() && words.size >= 40) {
            "Practice the PREP Impromptu Framework (Point, Reason, Example, Point) in under 60 seconds."
        } else null

        return SubEngineResult(
            strengths = strengths,
            specificMistakes = mistakes,
            suggestions = suggestions,
            explanationSnippets = explanations,
            recommendedTask = recommendedTask
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Improvement Recommendation Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Aggregates findings from all sub-engines, prioritizes the highest-leverage developmental
 * areas, and maps them to concrete practice drills in the SpeakCraft curriculum.
 */
object ImprovementRecommendationEngine {

    fun synthesize(
        evidence: LocalFeedbackEvidence,
        subResults: List<SubEngineResult>
    ): Pair<List<String>, String> {
        // Collect all suggested drills
        val allSuggestions = mutableListOf<String>()
        for (res in subResults) {
            allSuggestions.addAll(res.suggestions)
        }

        // Select top 2-3 unique actionable suggestions
        val prioritizedSuggestions = allSuggestions.distinct().take(3).toMutableList()

        if (prioritizedSuggestions.isEmpty()) {
            prioritizedSuggestions.add("Repeat this exercise focusing on vocal melody: inflect upward slightly on questions and downward on conclusions.")
            prioritizedSuggestions.add("Perform a 3-Stage Audit: review muted for visual presence, audio-only for vocal delivery.")
        }

        // Determine highest-priority next practice task
        val explicitTask = subResults.mapNotNull { it.recommendedTask }.firstOrNull()

        val nextTask = explicitTask ?: when (evidence.evaluatorName) {
            "PronunciationEvaluator" -> "Practice minimal pairs in the Articulation Lab to sharpen consonant contrasts."
            "GrammarEvaluator"       -> "Complete a Sentence Construction drill focusing on subject-verb agreement."
            "VocabularyEvaluator"    -> "Explore the Vocabulary Expansion module and practice incorporating 3 power words."
            "ConversationEvaluator"  -> "Engage in a 3-turn Conversation Simulation focusing on active listening questions."
            "PresentationEvaluator"  -> "Practice the PREP Impromptu Framework (Point, Reason, Example, Point) in under 60s."
            "StorytellingEvaluator"  -> "Relive a 60-second personal story using direct dialogue instead of indirect summary."
            "ListeningEvaluator"     -> "Complete an Active Listening Game exercise focusing on factual recall."
            "ReflectionEvaluator"    -> "Write a 2-sentence reflection on one vocal habit you want to improve this week."
            else -> {
                if (evidence.fillerCount >= 3) {
                    "Run the Zero-Filler 60s Sprint in Communication Challenges."
                } else if (evidence.wordsPerMinute > 175) {
                    "Practice the 5 Vocal Foundations: Rate of Speech drill in Voice Training Lab."
                } else {
                    "Advance to the next scheduled exercise in your Daily Practice plan."
                }
            }
        }

        return Pair(prioritizedSuggestions, nextTask)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Master Local Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reusable, deterministic local feedback engine for SpeakCraft.
 *
 * Orchestrates 7 modular sub-engines:
 * 1. [GrammarFeedbackEngine]
 * 2. [FluencyFeedbackEngine]
 * 3. [VocabularyFeedbackEngine]
 * 4. [PronunciationFeedbackEngine]
 * 5. [ConfidenceFeedbackEngine]
 * 6. [StructureFeedbackEngine]
 * 7. [ImprovementRecommendationEngine]
 *
 * **Stage 1.2 Guarantees:**
 * - Zero LLM runtime dependencies (no Phi-3.5, Gemma, or Qwen).
 * - 100% deterministic, evidence-based feedback synthesis.
 * - Never hallucinates or invents an error without empirical proof.
 * - Outputs all 6 canonical sections matching [StructuredFeedback].
 */
object LocalFeedbackEngine {

    fun buildFeedback(evidence: LocalFeedbackEvidence): StructuredFeedback {
        // Run all 6 evaluation sub-engines
        val grammarRes = GrammarFeedbackEngine.evaluate(evidence)
        val fluencyRes = FluencyFeedbackEngine.evaluate(evidence)
        val vocabRes = VocabularyFeedbackEngine.evaluate(evidence)
        val pronRes = PronunciationFeedbackEngine.evaluate(evidence)
        val confRes = ConfidenceFeedbackEngine.evaluate(evidence)
        val structRes = StructureFeedbackEngine.evaluate(evidence)

        val subResults = listOf(grammarRes, fluencyRes, vocabRes, pronRes, confRes, structRes)

        // 1. What Went Well (Strengths)
        val whatWentWell = buildList {
            if (evidence.wordCount >= 45) {
                add("Met target response depth (${evidence.wordCount} words) showing structured thought development.")
            } else if (evidence.wordCount >= 20) {
                add("Delivered a clear, concise response (${evidence.wordCount} words).")
            }
            for (res in subResults) {
                addAll(res.strengths)
            }
            if (isEmpty()) {
                add("Completed the practice exercise and captured audio/video for self-reflection.")
            }
        }.distinct().take(4)

        // 2. Specific Mistakes (Empirical only)
        val specificMistakes = buildList {
            for (res in subResults) {
                addAll(res.specificMistakes)
            }
            if (isEmpty()) {
                add("No major structural or delivery blockers detected in this attempt.")
            }
        }.distinct().take(3)

        // 3. Corrected Examples
        val correctedExamples = buildList {
            for (res in subResults) {
                addAll(res.correctedExamples)
            }
            if (isEmpty()) {
                add("Model phrasing: \"The core point is simple: [1s pause] when we clarify our message first, the audience follows effortlessly.\"")
            }
        }.distinct().take(2)

        // 4. Synthesize Recommendations & Next Task
        val (suggestions, nextTask) = ImprovementRecommendationEngine.synthesize(evidence, subResults)

        // 5. Cohesive Coaching Explanation
        val explanation = buildCoachingExplanation(evidence, specificMistakes, subResults)

        // 6. Non-cloud Honesty Notice
        val honestyNotice = buildHonestyNotice(evidence)

        return StructuredFeedback(
            whatWentWell = whatWentWell,
            specificMistakes = specificMistakes,
            correctedExamples = correctedExamples,
            explanation = explanation,
            improvementSuggestions = suggestions,
            nextPracticeTask = nextTask,
            honestyNotice = honestyNotice
        )
    }

    private fun buildCoachingExplanation(
        evidence: LocalFeedbackEvidence,
        mistakes: List<String>,
        subResults: List<SubEngineResult>
    ): String = buildString {
        append("Effective spoken communication relies on clarity, cadence, and composure. ")
        for (res in subResults) {
            for (snippet in res.explanationSnippets) {
                append("$snippet ")
            }
        }
        if (mistakes.isEmpty() || (mistakes.size == 1 && mistakes[0].startsWith("No major"))) {
            append("Your delivery demonstrates solid foundational control. Continue varying your vocal melody and rate to add character.")
        }
    }.trim()

    private fun buildHonestyNotice(evidence: LocalFeedbackEvidence): String = buildString {
        append("Evaluated 100% on-device using deterministic local intelligence (${evidence.processingMode}). ")
        if (evidence.limitationNotices.isNotEmpty()) {
            append(evidence.limitationNotices.joinToString(" "))
        } else {
            append("Derived from speech audio, acoustic DSP, and local rule analysis. Zero cloud AI involved.")
        }
    }.trim()
}
