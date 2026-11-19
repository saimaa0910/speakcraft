package com.example.data.activity

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "UniversalEvalEngine"
private const val ACTIVITY_EVAL_MODEL = "gemini-3.8-flash"
private const val EVAL_TIMEOUT_MS = 60_000L

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

    private val evaluationSchema = Schema.obj(
        properties = mapOf(
            "overallObjectiveScore" to Schema.integer("Objective rubric score 0-100 based strictly on observed evidence; never blend self-confidence rating"),
            "dimensionScores" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "key" to Schema.string("Rubric dimension key"),
                        "label" to Schema.string("Rubric dimension label"),
                        "score" to Schema.integer("0-100 score"),
                        "note" to Schema.string("Brief evidence-based observation")
                    )
                ),
                description = "Scores matching each rubric dimension"
            ),
            "whatWentWell" to Schema.array(Schema.string(), "2-3 specific strengths observed in the learner's response"),
            "specificMistakes" to Schema.array(Schema.string(), "1-3 concrete mistakes, gaps, or missed opportunities (never fabricate)"),
            "correctedExamples" to Schema.array(Schema.string(), "1-3 sharper rewritten sentences or stronger delivery phrasing"),
            "explanation" to Schema.string("Clear coaching explanation of why those adjustments raise communication impact"),
            "improvementSuggestions" to Schema.array(Schema.string(), "2-3 concrete, immediately actionable drills or habits"),
            "nextPracticeTask" to Schema.string("One specific next practice focus for the learner")
        )
    )

    private val evaluationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ACTIVITY_EVAL_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = evaluationSchema
            }
        )
    }

    suspend fun evaluate(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        recommendedNextIds: List<String> = activity.nextActivityIds
    ): ActivityEvaluationResult {
        val strategy = strategyFor(activity.evaluationConfiguration.strategy)
        val localMetrics = LocalResponseAnalyzer.analyze(response)
        val caveat = strategy.honestyCaveat(activity, response)

        if (aiClientOverride != null) {
            val custom = aiClientOverride.invoke(activity, response, strategy, localMetrics)
            return custom?.copy(
                confidenceSelfRating = response.confidenceSelfRating,
                recommendedNextActivityIds = recommendedNextIds
            ) ?: buildDeterministicRubricFallback(activity, response, strategy, localMetrics, caveat, recommendedNextIds)
        }

        if (activity.evaluationConfiguration.aiEnabled) {
            val aiResult = evaluateWithGemini(activity, response, strategy, localMetrics, caveat, recommendedNextIds)
            if (aiResult != null) return aiResult
        }

        return buildDeterministicRubricFallback(activity, response, strategy, localMetrics, caveat, recommendedNextIds)
    }

    private suspend fun evaluateWithGemini(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        localMetrics: LocalSpeechMetrics,
        caveat: String,
        recommendedNextIds: List<String>
    ): ActivityEvaluationResult? {
        return try {
            val rubricText = activity.scoringRubric.joinToString("\n") {
                "- ${it.key} (${it.label}, ${it.weightPercent}%): ${it.criteriaDescription}"
            }
            val checklistSummary = if (activity.interactionConfiguration.checklistItems.isNotEmpty()) {
                "Checklist completed (${response.completedChecklistItems.size}/${activity.interactionConfiguration.checklistItems.size}): ${response.completedChecklistItems.joinToString("; ")}"
            } else {
                "No checklist required."
            }
            val dialogueSummary = if (response.dialogueTurns.isNotEmpty()) {
                response.dialogueTurns.joinToString("\n") { "${it.role.uppercase()}: ${it.text}" }
            } else {
                ""
            }

            val prompt = """
                You are ${strategy.evaluatorName} inside SpeakCraft, an evidence-based communication coaching engine.
                Activity: ${activity.title} (${activity.exerciseType.displayName}, ${activity.difficulty.label})
                Learning Objective: ${activity.learningObjective}
                Exercise Prompt: ${activity.interactionConfiguration.promptText}
                Evaluator Focus: ${strategy.buildEvaluatorInstructions(activity, response)}
                Scoring Rubric:
                $rubricText

                Observed Local Metrics:
                - Word count: ${localMetrics.wordCount}
                - Duration seconds: ${response.durationSeconds}
                - Estimated WPM: ${if (localMetrics.wordsPerMinute > 0) localMetrics.wordsPerMinute else "N/A"}
                - Fillers detected in text: ${if (localMetrics.detectedFillers.isEmpty()) "None" else localMetrics.detectedFillers.joinToString(", ")}
                - $checklistSummary

                Learner Response Text:
                ${response.combinedTextForAnalysis.ifBlank { "(Audio/checklist submission with no written text)" }.take(4000)}
                ${if (dialogueSummary.isNotBlank()) "\nDialogue Transcript:\n${dialogueSummary.take(3000)}" else ""}

                HONESTY & QUALITY RULES:
                1. Ground EVERY strength and mistake strictly in the actual response provided. Never invent quotes or fake phoneme scores.
                2. Provide all 6 structured feedback sections: whatWentWell, specificMistakes, correctedExamples, explanation, improvementSuggestions, nextPracticeTask.
                3. Do not include PII. Score objectively 0-100 against the rubric dimensions.
            """.trimIndent()

            val aiResponse = withTimeout(EVAL_TIMEOUT_MS) {
                val bytes = response.mediaBytes
                if (bytes != null && bytes.isNotEmpty() && bytes.size <= 15 * 1024 * 1024 && response.mediaMimeType.isNotBlank()) {
                    evaluationModel.generateContent(
                        content {
                            inlineData(bytes, response.mediaMimeType)
                            text(prompt)
                        }
                    )
                } else {
                    evaluationModel.generateContent(prompt)
                }
            }

            val raw = aiResponse.text?.takeIf { it.isNotBlank() } ?: return null
            val json = JSONObject(raw)
            val dims = parseDimensions(json.optJSONArray("dimensionScores"), activity.scoringRubric)
            val overallScore = json.optInt(
                "overallObjectiveScore",
                dims.map { it.score }.average().toInt()
            ).coerceIn(0, 100)

            ActivityEvaluationResult(
                activityId = activity.activityId,
                evaluatorName = strategy.evaluatorName,
                overallObjectiveScore = overallScore,
                passedCriteria = overallScore >= activity.completionCriteria.passingScoreThreshold,
                dimensionScores = dims,
                whatWentWell = json.optJSONArray("whatWentWell").toCleanList(
                    fallback = listOf("You completed the prompt with clear engagement.")
                ),
                specificMistakes = json.optJSONArray("specificMistakes").toCleanList(
                    fallback = listOf("No major structural blockers detected; refine pacing and word precision further.")
                ),
                correctedExamples = json.optJSONArray("correctedExamples").toCleanList(
                    fallback = activity.examples.take(1)
                ),
                explanation = json.optString("explanation").ifBlank {
                    "Focused phrasing and intentional pauses make your main idea easier to remember."
                },
                improvementSuggestions = json.optJSONArray("improvementSuggestions").toCleanList(
                    fallback = listOf("Repeat the exercise once more slowing down by 10% on key phrases.")
                ),
                nextPracticeTask = json.optString("nextPracticeTask").ifBlank {
                    "Apply this technique once in a real conversation today."
                },
                honestyNotice = caveat,
                aiEvaluated = true,
                confidenceSelfRating = response.confidenceSelfRating,
                wordsPerMinute = localMetrics.wordsPerMinute,
                fillerCount = localMetrics.fillerCount,
                wordCount = localMetrics.wordCount,
                recommendedNextActivityIds = recommendedNextIds
            )
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.w(TAG, "AI evaluation timed out after ${EVAL_TIMEOUT_MS}ms for ${activity.activityId}")
                null
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI evaluation unavailable for ${activity.activityId}, falling back to local rubric check: ${e.message}")
            null
        }
    }

    internal fun buildDeterministicRubricFallback(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        metrics: LocalSpeechMetrics,
        caveat: String,
        recommendedNextIds: List<String>
    ): ActivityEvaluationResult {
        val checklistTotal = activity.interactionConfiguration.checklistItems.size
        val checklistRatio = if (checklistTotal > 0) {
            response.completedChecklistItems.size.toFloat() / checklistTotal.toFloat()
        } else 0f
        val hasAudioOrVideo = response.recordedAudioPath.isNotBlank() || response.recordedVideoPath.isNotBlank()
        val lengthRatio = (metrics.wordCount.toFloat() / activity.interactionConfiguration.targetWordCountMin.coerceAtLeast(8)).coerceIn(0f, 1.35f)
        val fillerPenalty = (metrics.fillerCount * 4).coerceAtMost(18)

        val baseScore = when {
            metrics.wordCount == 0 && !hasAudioOrVideo && checklistTotal == 0 -> 35
            checklistTotal > 0 && metrics.wordCount < 6 -> (48 + (checklistRatio * 34)).toInt()
            else -> (55 + (lengthRatio * 24).toInt() + (if (hasAudioOrVideo) 8 else 0) + (checklistRatio * 8).toInt() - fillerPenalty)
        }.coerceIn(30, 88)

        val dimensions = activity.scoringRubric.mapIndexed { idx, dim ->
            val dimScore = (baseScore + (if (idx == 0) 2 else -idx)).coerceIn(30, 90)
            EvaluationDimensionScore(
                key = dim.key,
                label = dim.label,
                score = dimScore,
                note = "Local rubric estimate (${metrics.wordCount} words, ${response.durationSeconds}s). Connect online for full Gemini coaching."
            )
        }

        val wells = buildList {
            if (metrics.wordCount >= activity.interactionConfiguration.targetWordCountMin) {
                add("Met target response depth (${metrics.wordCount} words) aligned with the exercise objective.")
            }
            if (hasAudioOrVideo) {
                add("Captured a real media recording (${response.durationSeconds}s) for self-review.")
            }
            if (response.completedChecklistItems.isNotEmpty()) {
                add("Completed ${response.completedChecklistItems.size}/$checklistTotal guided practice cues.")
            }
            if (isEmpty()) {
                add("Started the exercise and submitted an initial attempt.")
            }
        }

        val mistakes = buildList {
            if (metrics.wordCount < activity.interactionConfiguration.targetWordCountMin && checklistTotal == 0) {
                add("Response is brief (${metrics.wordCount} words); expand with one concrete example or clearer takeaway.")
            }
            if (metrics.fillerCount > 0) {
                add("Detected ${metrics.fillerCount} filler expression(s): ${metrics.detectedFillers.joinToString(", ")}.")
            }
            if (metrics.wordsPerMinute > 175) {
                add("Estimated pace (${metrics.wordsPerMinute} WPM) is fast; insert 1-second silent pauses between thoughts.")
            }
            if (isEmpty()) {
                add("Online AI semantic review was unavailable for this attempt—retry when connected for phrase-level critique.")
            }
        }

        val offlineNotice = buildString {
            append("Offline/local rubric estimate used (live Gemini AI unavailable). ")
            if (caveat.isNotBlank()) append(caveat)
        }.trim()

        return ActivityEvaluationResult(
            activityId = activity.activityId,
            evaluatorName = strategy.evaluatorName,
            overallObjectiveScore = baseScore,
            passedCriteria = baseScore >= activity.completionCriteria.passingScoreThreshold,
            dimensionScores = dimensions,
            whatWentWell = wells,
            specificMistakes = mistakes,
            correctedExamples = activity.examples.take(2),
            explanation = "Local metrics checked response completeness, filler frequency, pacing, and cue completion against ${activity.title}'s rubric.",
            improvementSuggestions = listOf(
                "Review the model example and repeat once focusing on ${activity.scoringRubric.first().label.lowercase()}.",
                if (metrics.fillerCount > 0) "Replace each filler word with a calm 1-second silent pause." else "Keep your sentences crisp and land downward on final words."
            ),
            nextPracticeTask = "Retry when online for detailed Gemini coaching or advance to the recommended follow-up activity.",
            honestyNotice = offlineNotice,
            aiEvaluated = false,
            confidenceSelfRating = response.confidenceSelfRating,
            wordsPerMinute = metrics.wordsPerMinute,
            fillerCount = metrics.fillerCount,
            wordCount = metrics.wordCount,
            recommendedNextActivityIds = recommendedNextIds
        )
    }

    private fun parseDimensions(
        arr: JSONArray?,
        rubric: List<ScoringRubricDimension>
    ): List<EvaluationDimensionScore> {
        if (arr == null || arr.length() == 0) {
            return rubric.map {
                EvaluationDimensionScore(it.key, it.label, 70, "Assessed against ${it.label}.")
            }
        }
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val fallback = rubric.getOrNull(i)
            EvaluationDimensionScore(
                key = obj.optString("key").ifBlank { fallback?.key ?: "dim_$i" },
                label = obj.optString("label").ifBlank { fallback?.label ?: "Criteria ${i + 1}" },
                score = obj.optInt("score", 70).coerceIn(0, 100),
                note = obj.optString("note").ifBlank { fallback?.criteriaDescription.orEmpty() }
            )
        }
    }

    private fun JSONArray?.toCleanList(fallback: List<String>): List<String> {
        if (this == null) return fallback
        val list = (0 until length()).mapNotNull { idx -> optString(idx).trim().takeIf { it.isNotBlank() } }
        return list.ifEmpty { fallback }
    }
}
