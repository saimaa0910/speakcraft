package com.example.data.activity

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "UniversalEvalEngine"

interface ActivityEvaluatorStrategy {
    val strategyType: EvaluationStrategyType
    val evaluatorName: String get() = strategyType.evaluatorName
    fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse): String
    fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse): String
}

class SpeakingEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.SPEAKING
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate spoken fluency, pacing, natural phrasing, filler control, and thought completeness."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse): String =
        if (response.mediaBytes == null && response.recordedAudioPath.isBlank()) {
            "Evaluated from your written/transcribed response. Record audio next time for pacing and vocal delivery observations."
        } else {
            activity.evaluationConfiguration.honestyCaveat
        }
}

class PronunciationEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.PRONUNCIATION
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate intelligibility, stress placement, consonant clarity, and pacing. NEVER invent exact phoneme accuracy percentages when acoustic data is insufficient."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse): String =
        if (response.mediaBytes == null) {
            "No audio waveform was attached—feedback covers wording, stress cues, and self-practice steps without claiming acoustic phoneme accuracy."
        } else {
            "Audio review assesses intelligibility, rhythm, and audible word clarity honestly without fabricating laboratory phoneme percentages."
        }
}

class GrammarEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.GRAMMAR
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate sentence clarity, active voice, tense consistency, concise structure, and natural spoken English grammar."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        activity.evaluationConfiguration.honestyCaveat
}

class VocabularyEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.VOCABULARY
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate word precision, everyday simplicity vs unnecessary jargon, collocations, and concrete imagery."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        activity.evaluationConfiguration.honestyCaveat
}

class ListeningEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.LISTENING
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Compare the learner's response strictly against the reference listening script ('${activity.interactionConfiguration.listeningScript.take(600)}'). Check factual recall, emotional/subtext awareness, and reflective paraphrasing."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        activity.evaluationConfiguration.honestyCaveat
}

class ConversationEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.CONVERSATION
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate conversational curiosity, open-ended follow-up questions, warmth, reciprocity, diplomacy, and natural flow across the dialogue turns."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        activity.evaluationConfiguration.honestyCaveat
}

class StorytellingEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.STORYTELLING
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate narrative arc (setup, conflict, turn, lesson), concrete sensory details (VAKS), pacing/suspense cues, and audience relevance."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        activity.evaluationConfiguration.honestyCaveat
}

class PresentationEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.PRESENTATION
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate opening hook, core message architecture (BLUF/pillars), closing call to action, executive poise, and checklist alignment."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse): String =
        if (activity.interactionConfiguration.supportsVideoRecording) {
            "Visual posture/eye-contact cues rely on your honest self-review checklist combined with verbal delivery analysis."
        } else {
            activity.evaluationConfiguration.honestyCaveat
        }
}

class ReflectionEvaluator : ActivityEvaluatorStrategy {
    override val strategyType = EvaluationStrategyType.REFLECTION
    override fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Evaluate depth of self-awareness, specificity of observed habits, and practicality of the learner's next real-world adjustment. Keep self-reported confidence separate from objective reflection quality."
    override fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse) =
        "Confidence self-rating is tracked separately from your objective reflection rubric score."
}

data class LocalSpeechMetrics(
    val wordCount: Int,
    val wordsPerMinute: Int,
    val fillerCount: Int,
    val detectedFillers: List<String>
)

object LocalResponseAnalyzer {
    private val fillerPatterns = listOf(
        "um" to Regex("\\bum+\\b", RegexOption.IGNORE_CASE),
        "uh" to Regex("\\buh+\\b", RegexOption.IGNORE_CASE),
        "like" to Regex("\\blike\\b", RegexOption.IGNORE_CASE),
        "basically" to Regex("\\bbasically\\b", RegexOption.IGNORE_CASE),
        "actually" to Regex("\\bactually\\b", RegexOption.IGNORE_CASE),
        "you know" to Regex("\\byou know\\b", RegexOption.IGNORE_CASE),
        "sort of" to Regex("\\bsort of\\b", RegexOption.IGNORE_CASE),
        "kind of" to Regex("\\bkind of\\b", RegexOption.IGNORE_CASE)
    )

    fun analyze(response: ActivityUserResponse): LocalSpeechMetrics {
        val text = response.combinedTextForAnalysis
        val words = text.split(Regex("\\s+")).filter { it.any(Char::isLetterOrDigit) }
        val wordCount = words.size
        val wpm = if (response.durationSeconds >= 5 && wordCount > 0) {
            ((wordCount * 60.0) / response.durationSeconds).toInt().coerceIn(30, 320)
        } else {
            0
        }
        val detected = mutableListOf<String>()
        var totalFillers = 0
        fillerPatterns.forEach { (label, regex) ->
            val count = regex.findAll(text).count()
            if (count > 0) {
                totalFillers += count
                detected += "$label ($count)"
            }
        }
        return LocalSpeechMetrics(
            wordCount = wordCount,
            wordsPerMinute = wpm,
            fillerCount = totalFillers,
            detectedFillers = detected
        )
    }
}

class UniversalEvaluationEngine(
    private val aiClientOverride: (suspend (ActivityDefinition, ActivityUserResponse, ActivityEvaluatorStrategy, LocalSpeechMetrics) -> ActivityEvaluationResult?)? = null
) {

    private val strategies: Map<EvaluationStrategyType, ActivityEvaluatorStrategy> = listOf(
        SpeakingEvaluator(),
        PronunciationEvaluator(),
        GrammarEvaluator(),
        VocabularyEvaluator(),
        ListeningEvaluator(),
        ConversationEvaluator(),
        StorytellingEvaluator(),
        PresentationEvaluator(),
        ReflectionEvaluator()
    ).associateBy { it.strategyType }

    fun strategyFor(type: EvaluationStrategyType): ActivityEvaluatorStrategy =
        strategies[type] ?: SpeakingEvaluator()

    fun allRegisteredStrategies(): List<ActivityEvaluatorStrategy> = strategies.values.toList()

    suspend fun evaluate(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        recommendedNextIds: List<String> = emptyList()
    ): ActivityEvaluationResult {
        val strategy = strategyFor(activity.evaluationConfiguration.strategy)
        val caveat = strategy.honestyCaveat(activity, response)
        val localMetrics = LocalResponseAnalyzer.analyze(response)
        val effectiveNextIds = recommendedNextIds.ifEmpty { activity.nextActivityIds }

        if (aiClientOverride != null) {
            val custom = aiClientOverride.invoke(activity, response, strategy, localMetrics)
            return custom?.copy(
                confidenceSelfRating = response.confidenceSelfRating,
                recommendedNextActivityIds = effectiveNextIds
            ) ?: buildLocalFallbackResult(activity, response, strategy, localMetrics, caveat, effectiveNextIds)
        }

        return buildLocalFallbackResult(activity, response, strategy, localMetrics, caveat, effectiveNextIds)
    }

    internal fun buildDeterministicRubricFallback(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        metrics: LocalSpeechMetrics,
        caveat: String,
        recommendedNextIds: List<String>
    ): ActivityEvaluationResult {
        val textForAnalysis = response.combinedTextForAnalysis.trim()
        val lang = if (textForAnalysis.isNotBlank()) {
            com.example.data.ai.local.language.LocalLanguageAnalysisEngine.analyze(textForAnalysis)
        } else null
        val draftInsights = if (textForAnalysis.isNotBlank()) {
            com.example.data.ai.local.language.DynamicLocalCoachEngine.analyzeUserDraft(
                text = textForAnalysis,
                topicHint = activity.title
            )
        } else null

        val checklistTotal = activity.interactionConfiguration.checklistItems.size
        val checklistRatio = if (checklistTotal > 0) {
            response.completedChecklistItems.size.toFloat() / checklistTotal.toFloat()
        } else 0f
        val hasAudioOrVideo = response.recordedAudioPath.isNotBlank() || response.recordedVideoPath.isNotBlank()
        val lengthRatio = (metrics.wordCount.toFloat() / activity.interactionConfiguration.targetWordCountMin.coerceAtLeast(8)).coerceIn(0f, 1.35f)
        val fillerPenalty = (metrics.fillerCount * 4).coerceAtMost(18)
        val grammarPenalty = ((lang?.grammarIssues?.size ?: 0) * 5).coerceAtMost(16)
        val discourseBonus = listOfNotNull(
            draftInsights?.discourse?.hasOpeningAnchor?.takeIf { it }?.let { 3 },
            draftInsights?.discourse?.hasConcreteExample?.takeIf { it }?.let { 4 },
            draftInsights?.discourse?.hasTransitionSignals?.takeIf { it }?.let { 2 },
            draftInsights?.discourse?.hasStrongClosing?.takeIf { it }?.let { 3 }
        ).sum()

        val baseScore = when {
            metrics.wordCount == 0 && !hasAudioOrVideo && checklistTotal == 0 -> 35
            checklistTotal > 0 && metrics.wordCount < 6 -> (50 + (checklistRatio * 36)).toInt()
            else -> (56 + (lengthRatio * 22).toInt() + (if (hasAudioOrVideo) 8 else 0) + (checklistRatio * 8).toInt() + discourseBonus - fillerPenalty - grammarPenalty)
        }.coerceIn(30, 96)

        val dimensions = activity.scoringRubric.mapIndexed { idx, dim ->
            val dimScore = (baseScore + (if (idx == 0) 2 else -idx)).coerceIn(30, 96)
            val dimNote = when {
                dim.key.contains("grammar", ignoreCase = true) && (lang?.grammarIssues?.isNotEmpty() == true) ->
                    "${lang.grammarIssues.size} phrasing polish item(s) spotted in local check."
                dim.key.contains("vocab", ignoreCase = true) && lang != null ->
                    "Lexical diversity ${lang.vocabularyAnalysis.vocabularyDiversityScore}% across ${metrics.wordCount} words."
                dim.key.contains("pace", ignoreCase = true) || dim.key.contains("fluency", ignoreCase = true) ->
                    "${metrics.wordsPerMinute} WPM • ${metrics.fillerCount} filler(s) • ${response.durationSeconds}s."
                else ->
                    "Local Intelligence rubric assessment (${metrics.wordCount} words, ${response.durationSeconds}s)."
            }
            EvaluationDimensionScore(
                key = dim.key,
                label = dim.label,
                score = dimScore,
                note = dimNote
            )
        }

        val wells = buildList {
            if (metrics.wordCount >= activity.interactionConfiguration.targetWordCountMin) {
                add("Met target response depth (${metrics.wordCount} words) aligned with the exercise objective.")
            }
            draftInsights?.strengths?.take(2)?.forEach { add(it) }
            if (hasAudioOrVideo) {
                add("Captured a real media recording (${response.durationSeconds}s) for self-review.")
            }
            if (response.completedChecklistItems.isNotEmpty()) {
                add("Completed ${response.completedChecklistItems.size}/$checklistTotal guided practice cues.")
            }
            if (isEmpty()) {
                add("Completed the exercise attempt cleanly.")
            }
        }.distinct().take(4)

        val mistakes = buildList {
            if (metrics.wordCount < activity.interactionConfiguration.targetWordCountMin && checklistTotal == 0) {
                add("Response is brief (${metrics.wordCount} words); expand with one concrete example or clearer takeaway.")
            }
            if (metrics.fillerCount > 0) {
                add("Detected ${metrics.fillerCount} filler expression(s): ${metrics.detectedFillers.joinToString(", ")}.")
            }
            lang?.grammarIssues?.take(2)?.forEach { issue ->
                add("${issue.issueType}: \"${issue.originalText}\" -> \"${issue.correctedText}\" (${issue.explanation})")
            }
            draftInsights?.focusAreas?.take(2)?.forEach { add(it) }
            if (metrics.wordsPerMinute > 175) {
                add("Estimated pace (${metrics.wordsPerMinute} WPM) is fast; insert 1-second silent pauses between thoughts.")
            }
            if (isEmpty()) {
                add("Clean run! Next step: push vocal contrast and slower emphasis on your final line.")
            }
        }.distinct().take(4)

        val rewrites = draftInsights?.rewrites?.map { "\"${it.original}\" -> \"${it.improved}\" (${it.why})" }
            .orEmpty()
            .ifEmpty { activity.examples.take(2) }

        val localNotice = buildString {
            append("Evaluated by SpeakCraft Local Intelligence.")
            if (caveat.isNotBlank()) append(" ").append(caveat)
        }.trim()

        val explanation = draftInsights?.narrativeSummary?.takeIf { it.isNotBlank() && metrics.wordCount >= 6 }
            ?: "Local Intelligence checked structure, filler frequency, pacing, grammar rules, and cue completion against ${activity.title}'s rubric."

        val suggestions = buildList {
            draftInsights?.suggestions?.take(2)?.forEach { add(it) }
            if (isEmpty()) {
                add("Review the model example and repeat once focusing on ${activity.scoringRubric.first().label.lowercase()}.")
                add(if (metrics.fillerCount > 0) "Replace each filler word with a calm 1-second silent pause." else "Keep your sentences crisp and land downward on final words.")
            }
        }

        val nextDrill = draftInsights?.nextMicroDrill?.takeIf { it.isNotBlank() }
            ?: "Run one 45-second retake emphasizing downward sentence endings, or advance to the recommended next activity."

        return ActivityEvaluationResult(
            activityId = activity.activityId,
            evaluatorName = strategy.evaluatorName,
            overallObjectiveScore = baseScore,
            passedCriteria = baseScore >= activity.completionCriteria.passingScoreThreshold,
            dimensionScores = dimensions,
            whatWentWell = wells,
            specificMistakes = mistakes,
            correctedExamples = rewrites,
            explanation = explanation,
            improvementSuggestions = suggestions,
            nextPracticeTask = nextDrill,
            honestyNotice = localNotice,
            aiEvaluated = false,
            confidenceSelfRating = response.confidenceSelfRating,
            wordsPerMinute = metrics.wordsPerMinute,
            fillerCount = metrics.fillerCount,
            wordCount = metrics.wordCount,
            recommendedNextActivityIds = recommendedNextIds
        )
    }

    internal fun buildLocalFallbackResult(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        metrics: LocalSpeechMetrics,
        caveat: String,
        recommendedNextIds: List<String>
    ): ActivityEvaluationResult = buildDeterministicRubricFallback(activity, response, strategy, metrics, caveat, recommendedNextIds)
}
