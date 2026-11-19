package com.example.data.ai.local.language

import com.example.data.ai.local.audio.AlignmentType
import com.example.data.ai.local.audio.PronunciationAnalysisEngine
import kotlin.math.roundToInt

/**
 * Output bundle produced by each specialized deterministic sub-engine.
 */
data class SubEngineFeedback(
    val strengths: List<String> = emptyList(),
    val specificMistakes: List<String> = emptyList(),
    val correctedExamples: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val recommendedTask: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Grammar Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

object GrammarFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        if (evidence.transcript.isBlank() && evidence.wordCount <= 0) {
            return SubEngineFeedback()
        }

        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val examples = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        if (evidence.grammarIssues.isEmpty()) {
            if (evidence.wordCount >= 10 || evidence.transcript.split(Regex("\\s+")).count { it.isNotBlank() } >= 10) {
                strengths += "Clean grammatical control — zero agreement, article, or tense issues detected."
            }
        } else {
            for (issue in evidence.grammarIssues.take(5)) {
                mistakes += "[${issue.ruleId}] ${issue.issueType}: ${issue.explanation}"
                examples += "Instead of: \"${issue.originalText}\" → Say: \"${issue.correctedText}\" [Rule: ${issue.ruleId}]"
            }
            val primaryType = evidence.grammarIssues.first().issueType
            suggestions += "Re-record this response applying the corrected $primaryType phrasing shown above."
            task = "Complete a Sentence Construction drill focusing on ${primaryType.lowercase()}."
        }

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = examples,
            suggestions = suggestions,
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Fluency Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

object FluencyFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val examples = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        // Speaking rate (WPM)
        when {
            evidence.wordsPerMinute in 120..170 -> {
                strengths += "Optimal speaking pace (${evidence.wordsPerMinute} WPM) within the ideal 120–170 WPM conversational range."
            }
            evidence.wordsPerMinute > 175 -> {
                mistakes += "Speaking pace is fast (${evidence.wordsPerMinute} WPM). Slow down slightly so listeners can absorb your key message."
                suggestions += "Practice Rate Contrast: deliver your setup briskly, then intentionally slow down 20% on your main conclusion."
                task = "Practice the 5 Vocal Foundations: Rate of Speech drill in Voice Training Lab."
            }
            evidence.wordsPerMinute in 1..99 && evidence.speechDurationSeconds > 8.0 -> {
                mistakes += "Speaking pace is slow (${evidence.wordsPerMinute} WPM). Link phrase boundaries more smoothly to maintain momentum."
                suggestions += "Practice shadowing a conversational clip to build steady cadence and rhythm."
                task = "Run an Impromptu Speaking warm-up to build fluid phrase linking."
            }
        }

        // Pause rhythm
        if (evidence.pauseCount in 2..8 && evidence.meanPauseDurationMs < 1500.0) {
            strengths += "Natural pause rhythm — used ${evidence.pauseCount} intentional pauses to frame key points."
        } else if (evidence.pauseCount < 2 && evidence.speechDurationSeconds > 15.0) {
            mistakes += "Very few pauses detected across ${evidence.speechDurationSeconds.roundToInt()}s of speech. Insert deliberate 1-second silent pauses between main ideas."
            suggestions += "Hold a clean 1-second silent pause between your opening hook and supporting explanation."
        } else if (evidence.meanPauseDurationMs > 2500.0) {
            val avgSec = ((evidence.meanPauseDurationMs / 100.0).roundToInt() / 10.0)
            mistakes += "Some pauses averaged ${avgSec}s. Keep intentional pauses between 0.5s and 1.5s to sustain listener engagement."
            suggestions += "Use a brief breath pause (0.7–1.2s) instead of long silent gaps while organizing your next sentence."
        }

        // Filler words
        if (evidence.fillerCount > 0) {
            val detail = if (evidence.detectedFillers.isNotEmpty()) {
                " (${evidence.detectedFillers.take(4).joinToString(", ")})"
            } else ""
            mistakes += "${evidence.fillerCount} filler word(s) detected$detail. Close your lips and hold a 1-second silent pause instead."
            val firstFiller = evidence.detectedFillers.firstOrNull()
                ?.replace(Regex("\\s*\\(\\d+\\)"), "")
                ?.trim()
                ?.ifBlank { "um" } ?: "um"
            examples += "Instead of: \"...so, $firstFiller, what we found was...\" → Say: \"...so [pause 1s] what we found was...\""
            suggestions += "Run the Zero-Filler 60s Sprint drill: close your lips whenever a filler word begins to form."
            if (task == null) {
                task = "Run the Zero-Filler 60s Sprint in Communication Challenges."
            }
        } else if (evidence.wordCount >= 15) {
            strengths += "Clean vocal delivery with zero filler words detected."
        }

        // Audio-only acoustic pacing consistency (when transcript is absent)
        if (evidence.transcript.isBlank()) {
            evidence.audioFeatures?.let { audio ->
                if (audio.speakingRateConsistencyScore >= 0.75) {
                    strengths += "Steady vocal pacing consistency (${(audio.speakingRateConsistencyScore * 100).roundToInt()}%) across the recording."
                } else {
                    suggestions += "Focus on keeping a steady breathing and speaking cadence from opening to closing."
                }
            }
        }

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = examples,
            suggestions = suggestions,
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Vocabulary Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

object VocabularyFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        val vocab = evidence.vocabularyAnalysis ?: return SubEngineFeedback()

        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val examples = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        if (vocab.targetWordsUsed.isNotEmpty()) {
            strengths += "Successfully incorporated target vocabulary: ${vocab.targetWordsUsed.joinToString(", ")}."
        }

        if (vocab.vocabularyDiversityScore >= 50) {
            strengths += "Strong lexical diversity (${vocab.vocabularyDiversityScore}% unique words)."
        } else if (vocab.vocabularyDiversityScore in 1..39 && evidence.wordCount >= 20) {
            mistakes += "Limited lexical diversity (${vocab.vocabularyDiversityScore}% unique words). Expand word choice for sharper precision."
            suggestions += "Replace general adjectives and verbs with specific synonyms from the SpeakCraft Vocabulary Bank."
            task = "Explore the Vocabulary Development module and practice 3 new power words in context."
        }

        if (vocab.repeatedWords.isNotEmpty()) {
            val topRepeated = vocab.repeatedWords.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "\"${it.key}\" (${it.value}x)" }
            mistakes += "Noticeably repeated words: $topRepeated. Vary phrasing to keep listeners engaged."
            suggestions += "Substitute repeated words with concise alternatives on your next recording."
            if (task == null) {
                task = "Complete a Word Association & Synonym drill in Vocabulary Development."
            }
        }

        suggestions.addAll(vocab.suggestions.take(2))

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = examples,
            suggestions = suggestions.distinct(),
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Pronunciation Feedback Engine (Accent-Tolerant & Honest)
// ─────────────────────────────────────────────────────────────────────────────

object PronunciationFeedbackEngine {

    private val alignmentEngine = PronunciationAnalysisEngine()

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val examples = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        // Prompted word-alignment analysis (transcript-based, accent-tolerant)
        if (!evidence.expectedText.isNullOrBlank() && evidence.transcript.isNotBlank()) {
            val result = alignmentEngine.analyze(
                recognizedTranscript = evidence.transcript,
                expectedText = evidence.expectedText
            )
            val accentVariantCount = result.alignmentOperations.count { it.type == AlignmentType.ACCENT_VARIANT }

            if (accentVariantCount > 0) {
                strengths += "High intelligibility (${(result.wordAccuracy * 100).roundToInt()}% word accuracy) with $accentVariantCount understandable accent variants recognized fairly."
            } else if (result.wordAccuracy >= 0.88f) {
                strengths += "Clear word alignment (${(result.wordAccuracy * 100).roundToInt()}% match to the reference prompt)."
            }

            if (result.likelyMispronounced.isNotEmpty()) {
                val flagged = result.likelyMispronounced.take(4).joinToString(", ")
                mistakes += "Transcript alignment flagged words to articulate more crisply: $flagged."
                suggestions += "Slow down and over-articulate the flagged words ($flagged) twice before speaking at full speed."
                task = "Practice consonant and vowel clarity drills in Pronunciation & Articulation."
            }
        }

        // Acoustic signal integrity (applies to both prompted and freeform speech)
        evidence.audioFeatures?.let { audio ->
            if (audio.clippingDetected) {
                mistakes += "Audio clipping detected — peak volume distorted microphone capture. Step back 10–15 cm or lower speaking volume slightly."
                suggestions += "Keep a consistent 15–20 cm microphone distance to prevent audio clipping."
            } else if (audio.speechSilenceRatio in 0.4..4.5 && audio.durationSeconds >= 3.0) {
                strengths += "Clean microphone capture with no audio clipping detected."
            }

            if (audio.backgroundNoiseLevel > 0.06f) {
                mistakes += "Elevated background noise was detected; a quieter environment will sharpen vocal clarity."
            }
        }

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            correctedExamples = examples,
            suggestions = suggestions,
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Confidence / Physical Delivery Presence Engine (Non-Psychological Invariant)
// ─────────────────────────────────────────────────────────────────────────────

object ConfidenceFeedbackEngine {

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        evidence.audioFeatures?.let { audio ->
            val volumeDb = if (audio.rmsVolumeDb > -90.0) audio.rmsVolumeDb else audio.relativeLoudness
            val pitchVar = maxOf(audio.pitchVariation, audio.pitchVariationHz)

            if (volumeDb in -30.0..-6.0 && !audio.clippingDetected) {
                strengths += "Strong vocal projection (${volumeDb.roundToInt()} dBFS) with clean dynamic headroom."
            } else if (volumeDb < -35.0 && volumeDb > -89.0) {
                mistakes += "Soft vocal projection (${volumeDb.roundToInt()} dBFS). Support your breath so sentence endings remain clearly audible."
                suggestions += "Project your voice toward the far wall of the room to lift vocal volume comfortably."
                task = "Practice the Volume Control & Voice Projection drill in Voice Mastery."
            }

            if (pitchVar >= 25.0) {
                strengths += "Dynamic vocal melody: pitch variation (${pitchVar.roundToInt()} Hz range) avoided monotonous delivery."
            } else if (pitchVar in 0.1..10.0 && audio.durationSeconds >= 6.0) {
                mistakes += "Narrow pitch range (${pitchVar.roundToInt()} Hz). Vary vocal inflection to highlight key ideas."
                suggestions += "Practice Pitch Variation: lift tone slightly on transitions and inflect downward on conclusions."
            }
        }

        evidence.faceAnalysis?.let { face ->
            if (face.framesAnalyzed > 0) {
                if (face.faceDetectionRate >= 0.8f) {
                    strengths += "Consistent camera presence — face stayed centered in ${(face.faceDetectionRate * 100).roundToInt()}% of analyzed frames."
                } else if (face.faceDetectionRate < 0.5f) {
                    mistakes += "Face was visible in only ${(face.faceDetectionRate * 100).roundToInt()}% of frames. Position the camera at eye level with balanced lighting."
                    suggestions += "Keep your eyes aligned with the lens during opening and closing statements."
                }
            }
        }

        evidence.postureAnalysis?.let { posture ->
            if (posture.framesAnalyzed > 0 && posture.bodyDetectedFrames > 0) {
                if (!posture.headTiltAlert) {
                    strengths += "Upright postural alignment observed across ${posture.bodyDetectedFrames}/${posture.framesAnalyzed} frames."
                } else {
                    mistakes += "Frequent head tilt observed in video frames. Keep shoulders relaxed and chin level."
                    suggestions += "Reset shoulder posture before recording: roll shoulders back and center your upper body."
                }
            }
        }

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            suggestions = suggestions,
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Structure Feedback Engine
// ─────────────────────────────────────────────────────────────────────────────

object StructureFeedbackEngine {

    private val SIGNPOSTS = listOf(
        "first", "second", "third", "finally", "in summary", "for example",
        "because", "therefore", "however", "the main point", "to conclude"
    )

    fun evaluate(evidence: LocalFeedbackEvidence): SubEngineFeedback {
        if (evidence.transcript.isBlank()) {
            return SubEngineFeedback()
        }

        val strengths = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var task: String? = null

        val sentences = evidence.transcript
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val maxSentenceWords = sentences.maxOfOrNull { s ->
            s.split(Regex("\\s+")).count { it.isNotBlank() }
        } ?: 0

        if (maxSentenceWords >= 30) {
            mistakes += "Detected a run-on sentence ($maxSentenceWords words without a full stop). Break complex thoughts into crisp 12–18 word statements."
            suggestions += "Use the PREP framework (Point → Reason → Example → Point) to keep every idea modular and easy to follow."
            task = "Practice the PREP Impromptu Framework in Speaking Clarity."
        } else if (sentences.size >= 2 && evidence.wordCount >= 20) {
            strengths += "Well-paced sentence structure averaging ${evidence.wordCount / sentences.size} words per sentence."
        }

        val lower = evidence.transcript.lowercase()
        val matchedSignposts = SIGNPOSTS.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }
        if (matchedSignposts.isNotEmpty()) {
            strengths += "Used clear structural signposts (${matchedSignposts.take(3).joinToString(", ")}) to guide listener comprehension."
        } else if (evidence.wordCount >= 25) {
            suggestions += "Add explicit verbal signposts (e.g., 'First...', 'For example...', 'In summary...') using the PREP structure."
        }

        return SubEngineFeedback(
            strengths = strengths,
            specificMistakes = mistakes,
            suggestions = suggestions,
            recommendedTask = task
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Improvement Recommendation Engine
// ─────────────────────────────────────────────────────────────────────────────

object ImprovementRecommendationEngine {

    fun recommendNextTask(
        evidence: LocalFeedbackEvidence,
        subResults: List<SubEngineFeedback>
    ): String {
        subResults.firstNotNullOfOrNull { it.recommendedTask }?.let { return it }

        return when (evidence.evaluatorName) {
            "PronunciationEvaluator" -> "Practice minimal pairs in Pronunciation & Articulation to sharpen consonant and vowel contrasts."
            "GrammarEvaluator"       -> "Complete a Sentence Construction drill focusing on subject-verb agreement and concise phrasing."
            "VocabularyEvaluator"    -> "Explore Vocabulary Development and apply 3 new power words in spoken sentences."
            "ConversationEvaluator"  -> "Complete a 3-turn Conversation Simulation drill focusing on open-ended follow-up questions."
            "PresentationEvaluator"  -> "Practice the PREP Impromptu Framework (Point, Reason, Example, Point) in under 60 seconds."
            "StorytellingEvaluator"  -> "Deliver a 60-second personal story using vivid concrete details and intentional pauses."
            "ListeningEvaluator"     -> "Complete an Active Listening exercise focusing on factual and emotional recall."
            "ReflectionEvaluator"    -> "Write a 2-sentence reflection on one speaking habit you want to strengthen this week."
            else -> {
                when {
                    evidence.fillerCount > 2 -> "Run the Zero-Filler 60s Sprint in Communication Challenges."
                    evidence.wordsPerMinute > 175 -> "Practice Rate of Speech contrast in Voice Mastery."
                    evidence.transcript.isBlank() -> "Practice vocal projection and 1-second pause control in Voice Mastery."
                    else -> "Advance to the next recommended exercise in your Daily Practice plan."
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Master Local Feedback Engine (All 6 Canonical Sections + Honesty Notice)
// ─────────────────────────────────────────────────────────────────────────────

object LocalFeedbackEngine {

    fun buildFeedback(evidence: LocalFeedbackEvidence): StructuredFeedback {
        val grammar = GrammarFeedbackEngine.evaluate(evidence)
        val fluency = FluencyFeedbackEngine.evaluate(evidence)
        val vocabulary = VocabularyFeedbackEngine.evaluate(evidence)
        val pronunciation = PronunciationFeedbackEngine.evaluate(evidence)
        val confidence = ConfidenceFeedbackEngine.evaluate(evidence)
        val structure = StructureFeedbackEngine.evaluate(evidence)

        val allSubs = listOf(grammar, fluency, vocabulary, pronunciation, confidence, structure)

        val strengths = allSubs.flatMap { it.strengths }.distinct().toMutableList()
        if (strengths.isEmpty()) {
            if (evidence.wordCount >= 20) {
                strengths += "Delivered a complete response (${evidence.wordCount} words) for structured coaching review."
            } else {
                strengths += "Completed the practice recording and captured on-device delivery metrics."
            }
        }

        val mistakes = allSubs.flatMap { it.specificMistakes }.distinct().toMutableList()
        if (mistakes.isEmpty()) {
            mistakes += "No major structural or delivery blockers detected in this attempt."
        }

        val correctedExamples = allSubs.flatMap { it.correctedExamples }.distinct().toMutableList()
        if (correctedExamples.isEmpty() && evidence.transcript.isNotBlank()) {
            correctedExamples += "Model phrasing: \"The core point is simple: [1s pause] when we lead with clarity, listeners follow effortlessly.\""
        }

        val suggestions = allSubs.flatMap { it.suggestions }.distinct().toMutableList()
        if (suggestions.isEmpty()) {
            if (evidence.transcript.isBlank()) {
                suggestions += "Repeat the recording focusing on steady breath support, clear volume projection, and 1-second silent pauses."
                suggestions += "Review your audio playback to verify vocal melody and steady pacing from start to finish."
            } else {
                suggestions += "Repeat the exercise focusing on vocal melody: lift tone slightly on transitions and land downward on conclusions."
                suggestions += "Review your recording using the 3-Stage Audit: audio-only for vocal control, and muted video for visual presence."
            }
        }

        val explanation = buildExplanation(evidence, mistakes)
        val nextTask = ImprovementRecommendationEngine.recommendNextTask(evidence, allSubs)
        val honestyNotice = buildHonestyNotice(evidence)

        return StructuredFeedback(
            whatWentWell = strengths,
            specificMistakes = mistakes,
            correctedExamples = correctedExamples,
            explanation = explanation,
            improvementSuggestions = suggestions,
            nextPracticeTask = nextTask,
            honestyNotice = honestyNotice
        )
    }

    private fun buildExplanation(evidence: LocalFeedbackEvidence, mistakes: List<String>): String = buildString {
        if (evidence.transcript.isBlank()) {
            append("Audio-only acoustic coaching measures measurable delivery signals such as projection, clipping, pause cadence, and pacing consistency. ")
            append("Strengthening breath support and deliberate pauses creates immediate vocal clarity even without a transcript.")
        } else {
            append("Effective spoken communication combines clear sentence structure, controlled speaking pace, and intentional vocal delivery. ")
            if (evidence.grammarIssues.isNotEmpty()) {
                append("Applying targeted rule-based corrections sharpens precision and builds listener trust. ")
            }
            if (evidence.fillerCount > 0) {
                append("Replacing filler words with brief silent pauses signals composure and lets listeners absorb each key point. ")
            }
            if (evidence.wordsPerMinute > 175) {
                append("Moderating speaking rate by 15% gives your audience room to process complex ideas. ")
            }
            if (mistakes.size == 1 && mistakes[0].startsWith("No major")) {
                append("Your foundational control is solid; experiment with vocal melody and rate contrast to elevate expressiveness.")
            }
        }
    }.trim()

    private fun buildHonestyNotice(evidence: LocalFeedbackEvidence): String = buildString {
        append("Evaluated 100% on-device (${evidence.processingMode}). Zero cloud AI involved. ")
        if (evidence.limitationNotices.isNotEmpty()) {
            append(evidence.limitationNotices.joinToString(" "))
        } else if (evidence.transcript.isBlank()) {
            append("Audio-only mode: feedback reflects acoustic delivery metrics only; linguistic and grammar checks were skipped without a transcript.")
        } else {
            append("Metrics and coaching notes are generated deterministically from local speech, acoustic, and visual analysis.")
        }
    }.trim()
}
