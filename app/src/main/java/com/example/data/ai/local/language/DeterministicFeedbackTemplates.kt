package com.example.data.ai.local.language

import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.video.FaceAnalysisResult
import com.example.data.ai.local.video.GestureAnalysisResult
import com.example.data.ai.local.video.PostureAnalysisResult

// ─────────────────────────────────────────────────────────────────────────────
// Feedback Data Classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Structured bundle of local metrics and observations fed into the feedback generator.
 */
data class LocalFeedbackEvidence(
    val transcript: String = "",
    val expectedText: String? = null,
    val wordCount: Int = 0,
    val wordsPerMinute: Int = 0,
    val fillerCount: Int = 0,
    val detectedFillers: List<String> = emptyList(),
    val pauseCount: Int = 0,
    val meanPauseDurationMs: Double = 0.0,
    val speechDurationSeconds: Double = 0.0,
    val grammarIssues: List<GrammarIssue> = emptyList(),
    val vocabularyAnalysis: VocabularyAnalysis? = null,
    val audioFeatures: AcousticFeatures? = null,
    val faceAnalysis: FaceAnalysisResult? = null,
    val postureAnalysis: PostureAnalysisResult? = null,
    val gestureAnalysis: GestureAnalysisResult? = null,
    val exerciseTitle: String = "",
    val evaluatorName: String = "",
    val limitationNotices: List<String> = emptyList(),
    val processingMode: String = "LOCAL_DETERMINISTIC"
)

/**
 * The 6 canonical feedback sections produced by the SpeakCraft coaching engine.
 */
data class StructuredFeedback(
    val whatWentWell: List<String>,
    val specificMistakes: List<String>,
    val correctedExamples: List<String>,
    val explanation: String,
    val improvementSuggestions: List<String>,
    val nextPracticeTask: String,
    val honestyNotice: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Deterministic Feedback Templates
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Generates all six structured feedback sections from evidence without an LLM.
 *
 * Grounded in Vinh Giang's (@askvinh) vocal & communication frameworks:
 * 5 Vocal Foundations (Rate, Pause, Melody, Volume, Character), PREP, and CLEAR.
 */
object DeterministicFeedbackTemplates {

    fun buildFeedback(evidence: LocalFeedbackEvidence): StructuredFeedback {
        val wells = buildWhatWentWell(evidence)
        val mistakes = buildSpecificMistakes(evidence)
        val correctedExamples = buildCorrectedExamples(evidence)
        val explanation = buildExplanation(evidence, mistakes)
        val suggestions = buildImprovementSuggestions(evidence)
        val nextTask = buildNextPracticeTask(evidence)
        val honesty = buildHonestyNotice(evidence)

        return StructuredFeedback(
            whatWentWell = wells,
            specificMistakes = mistakes,
            correctedExamples = correctedExamples,
            explanation = explanation,
            improvementSuggestions = suggestions,
            nextPracticeTask = nextTask,
            honestyNotice = honesty
        )
    }

    // ── Section 1: What Went Well ─────────────────────────────────────────────

    private fun buildWhatWentWell(e: LocalFeedbackEvidence): List<String> = buildList {
        if (e.wordCount >= 50) {
            add("Met target response depth (${e.wordCount} words) showing structured thought development.")
        } else if (e.wordCount >= 20) {
            add("Delivered a clear concise response (${e.wordCount} words).")
        }

        if (e.wordsPerMinute in 120..170) {
            add("Good speaking pace (${e.wordsPerMinute} WPM) within the optimal conversational range (120–170 WPM).")
        }

        if (e.pauseCount in 2..8 && e.meanPauseDurationMs < 1500.0) {
            add("Natural pause rhythm — used ${e.pauseCount} intentional pauses to frame key ideas.")
        }

        if (e.fillerCount == 0 && e.wordCount >= 15) {
            add("Clean vocal delivery with zero filler words detected.")
        }

        if (e.grammarIssues.isEmpty() && e.wordCount >= 15) {
            add("Solid grammatical accuracy with no agreement, article, or tense issues.")
        }

        e.vocabularyAnalysis?.let { vocab ->
            if (vocab.vocabularyDiversityScore >= 50) {
                add("Strong vocabulary diversity (${vocab.vocabularyDiversityScore}% unique words).")
            }
            if (vocab.targetWordsUsed.isNotEmpty()) {
                add("Successfully incorporated target vocabulary: ${vocab.targetWordsUsed.joinToString(", ")}.")
            }
        }

        e.faceAnalysis?.let { face ->
            if (face.faceDetectionRate >= 0.8f) {
                add("Consistent camera presence — face remained centered throughout the recording.")
            }
        }

        e.postureAnalysis?.let { posture ->
            if (posture.coachingSuggestions.isEmpty() || posture.bodyDetectedFrames > 0) {
                add("Upper body was positioned well in frame for visual presence.")
            }
        }

        if (isEmpty()) {
            add("Completed the practice exercise and captured audio/video for self-reflection.")
        }
    }

    // ── Section 2: Specific Mistakes ──────────────────────────────────────────

    private fun buildSpecificMistakes(e: LocalFeedbackEvidence): List<String> = buildList {
        // Grammar issues
        for (issue in e.grammarIssues.take(3)) {
            add("${issue.issueType}: ${issue.explanation}")
        }

        // Filler words
        if (e.fillerCount > 0) {
            val fillerBreakdown = if (e.detectedFillers.isNotEmpty()) {
                " (${e.detectedFillers.take(4).joinToString(", ")})"
            } else ""
            add("${e.fillerCount} filler word(s) detected$fillerBreakdown. Try closing your lips and holding 1-second silence instead.")
        }

        // Pacing issues
        if (e.wordsPerMinute > 180) {
            add("Speaking pace is fast (${e.wordsPerMinute} WPM). High speed makes it harder for listeners to absorb your main points.")
        } else if (e.wordsPerMinute in 1..99 && e.speechDurationSeconds > 8.0) {
            add("Speaking pace is slow (${e.wordsPerMinute} WPM). Practice linking phrases more smoothly to maintain momentum.")
        }

        // Pause issues
        if (e.pauseCount < 2 && e.speechDurationSeconds > 15.0) {
            add("Very few pauses detected. Insert deliberate 1-second silent pauses between major ideas.")
        } else if (e.meanPauseDurationMs > 2500.0) {
            add("Some pauses exceeded 2.5 seconds. Keep intentional pauses between 0.5s and 1.5s to sustain engagement.")
        }

        // Vocabulary issues
        e.vocabularyAnalysis?.let { vocab ->
            if (vocab.repeatedWords.isNotEmpty()) {
                val topRepeated = vocab.repeatedWords.entries.sortedByDescending { it.value }.take(2)
                add("Noticeably repeated words: ${topRepeated.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }}.")
            }
        }

        // Face / Video presence
        e.faceAnalysis?.let { face ->
            if (face.framesAnalyzed > 0 && face.faceDetectionRate < 0.5f) {
                add("Face was visible in only ${(face.faceDetectionRate * 100).toInt()}% of video frames. Position yourself centrally with adequate lighting.")
            }
        }

        if (isEmpty()) {
            add("No major structural or delivery blockers detected in this attempt.")
        }
    }

    // ── Section 3: Corrected Examples ─────────────────────────────────────────

    private fun buildCorrectedExamples(e: LocalFeedbackEvidence): List<String> = buildList {
        // Take up to 2 grammar issues first
        for (issue in e.grammarIssues.take(2)) {
            add("Instead of: \"${issue.originalText}\" → Say: \"${issue.correctedText}\"")
        }

        // If we still need examples, add filler or phrasing examples
        if (size < 2 && e.fillerCount > 0 && e.detectedFillers.isNotEmpty()) {
            val firstFiller = e.detectedFillers.first().replace(Regex("\\s*\\(\\d+\\)"), "")
            add("Instead of: \"...so, $firstFiller, what we found was...\" → Say: \"...so [pause 1s] what we found was...\"")
        }

        if (isEmpty()) {
            add("Model phrasing: \"The core point is simple: [1s pause] when we clarify our message first, the audience follows effortlessly.\"")
        }
    }

    // ── Section 4: Explanation ────────────────────────────────────────────────

    private fun buildExplanation(e: LocalFeedbackEvidence, mistakes: List<String>): String = buildString {
        append("Effective spoken communication relies on clarity, cadence, and composure. ")
        if (e.grammarIssues.isNotEmpty()) {
            append("Correcting sentence structure ensures your credibility remains high with every listener. ")
        }
        if (e.fillerCount > 0) {
            append("Vinh Giang teaches that fillers occur when the mouth moves faster than the mind; replacing fillers with silent poise signals confidence. ")
        }
        if (e.wordsPerMinute > 175) {
            append("Slowing down by 15% creates breathing room for listeners to process and retain your key points. ")
        }
        if (mistakes.isEmpty() || (mistakes.size == 1 && mistakes[0].startsWith("No major"))) {
            append("Your delivery demonstrates strong foundational control. Continue varying your vocal melody and rate to add character.")
        }
    }.trim()

    // ── Section 5: Improvement Suggestions ────────────────────────────────────

    private fun buildImprovementSuggestions(e: LocalFeedbackEvidence): List<String> = buildList {
        if (e.fillerCount > 0) {
            add("Run the Zero-Filler 60s Sprint drill: close your lips whenever you sense a filler word forming.")
        }
        if (e.wordsPerMinute > 175) {
            add("Practice rate contrast: speak your setup briskly, then intentionally slow down 20% on the core conclusion.")
        } else if (e.wordsPerMinute in 1..99) {
            add("Practice shadowing an @askvinh lesson clip to build conversational cadence and rhythm.")
        }
        if (e.grammarIssues.isNotEmpty()) {
            add("Re-record this response once, explicitly using the corrected phrasing shown above.")
        }
        if (e.pauseCount < 2 && e.speechDurationSeconds > 10.0) {
            add("Hold a full 1-second silent pause between your opening hook and your main explanation.")
        }
        e.vocabularyAnalysis?.let { vocab ->
            if (vocab.repeatedWords.isNotEmpty()) {
                add("Substitute repeated words with synonyms from the SpeakCraft Vocabulary Bank.")
            }
        }
        if (isEmpty()) {
            add("Repeat the exercise focusing on vocal melody: pitch up slightly on questions, and inflect downward on conclusions.")
            add("Practice with the 3-Stage Audit: review muted for visual presence, audio-only for vocal delivery.")
        }
    }

    // ── Section 6: Next Practice Task ─────────────────────────────────────────

    private fun buildNextPracticeTask(e: LocalFeedbackEvidence): String {
        return when (e.evaluatorName) {
            "PronunciationEvaluator" -> "Practice minimal pairs in the Articulation Lab to sharpen consonant and vowel contrasts."
            "GrammarEvaluator"       -> "Complete a Sentence Construction drill focusing on subject-verb agreement and concise phrasing."
            "VocabularyEvaluator"    -> "Explore the Vocabulary Development module and practice incorporating 3 new power words."
            "ConversationEvaluator"  -> "Engage in a 3-turn Conversation Simulation drill focusing on active listening and follow-up questions."
            "PresentationEvaluator"  -> "Practice the PREP Impromptu Framework (Point, Reason, Example, Point) in under 60 seconds."
            "StorytellingEvaluator"  -> "Relive a 60-second personal story using direct dialogue instead of third-person summary."
            "ListeningEvaluator"     -> "Complete an Active Listening Game exercise focusing on factual and emotional recall."
            "ReflectionEvaluator"    -> "Write a 2-sentence reflection on one vocal habit you want to improve this week."
            else -> {
                if (e.fillerCount > 2) {
                    "Run the Zero-Filler 60s Sprint in Communication Challenges."
                } else if (e.wordsPerMinute > 175) {
                    "Practice the 5 Vocal Foundations: Rate of Speech drill in Voice Training Lab."
                } else {
                    "Advance to the next scheduled exercise in your Daily Practice plan."
                }
            }
        }
    }

    // ── Honesty Notice ────────────────────────────────────────────────────────

    private fun buildHonestyNotice(e: LocalFeedbackEvidence): String = buildString {
        append("Evaluated on-device using local intelligence (${e.processingMode}). ")
        if (e.limitationNotices.isNotEmpty()) {
            append(e.limitationNotices.joinToString(" "))
        } else {
            append("Metrics derived deterministically from speech audio and transcript. No cloud AI involved.")
        }
    }.trim()
}
