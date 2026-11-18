package com.example.data.ai

import android.util.Log
import com.example.data.model.CurriculumCatalog
import com.example.data.model.ExtractedTechnique
import com.example.data.model.SpeechFeedback
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiCoachService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

data class ExtractedYouTubeBundle(
    val mainTopic: String,
    val category: String,
    val difficulty: String,
    val techniques: List<ExtractedTechnique>
)

data class VocabEvaluationResult(
    val usedCorrectly: Boolean,
    val naturalnessScore: Int,
    val feedback: String,
    val improvedExample: String
)

class GeminiCoachService {

    private val generalModel by lazy {
        Firebase.ai.generativeModel(MODEL_NAME)
    }

    private val speechAnalysisSchema = Schema.obj(
        properties = mapOf(
            "transcript" to Schema.string("Verbatim transcript of the spoken audio"),
            "overallAssessment" to Schema.string("Evidence-based holistic assessment referencing observed delivery"),
            "strengths" to Schema.array(Schema.string(), "Specific things done well with examples"),
            "mistakes" to Schema.array(Schema.string(), "Specific grammar, pacing, or clarity mistakes observed"),
            "recordingExamples" to Schema.array(Schema.string(), "Exact verbatim phrases quoted from the recording"),
            "pronunciationAssessable" to Schema.boolean("True ONLY if spoken voice is clearly audible and >= 4 seconds so pronunciation can be judged reliably"),
            "pronunciationNotes" to Schema.array(Schema.string(), "Specific words/sounds mispronounced or praised; state if audio wasn't assessable"),
            "betterAlternatives" to Schema.array(Schema.string(), "Stronger sentence rewrites of user phrasing"),
            "fillerWordsDetected" to Schema.array(Schema.string(), "Detected filler words with approximate counts"),
            "repeatedExpressions" to Schema.array(Schema.string(), "Noticeably repeated words or crutch expressions with counts"),
            "articulationClarity" to Schema.string("Evaluation of consonant crispness, enunciation, and vocal energy"),
            "thoughtOrganization" to Schema.string("Evaluation of opening hook, logical flow (e.g., PREP), and closing"),
            "improvementSuggestions" to Schema.array(Schema.string(), "Actionable next drills"),
            "recommendedExercises" to Schema.array(Schema.string(), "Recommended @askvinh exercises"),
            "nextRecommendedActivity" to Schema.string("Single best next activity in SpeakCraft based on the observed mistakes"),
            "confidenceLevel" to Schema.enumeration(listOf("High", "Medium", "Low"), "Assessment confidence based on audio clarity"),
            "audioQualityNote" to Schema.string("Note on background noise, volume, or duration sufficiency"),
            "fluencyScore" to Schema.integer("0-100 fluency score backed by evidence"),
            "pronunciationScore" to Schema.integer("0-100 pronunciation clarity score (0 if pronunciationAssessable is false)"),
            "vocabularyScore" to Schema.integer("0-100 vocabulary diversity and precision score"),
            "grammarScore" to Schema.integer("0-100 grammatical accuracy score"),
            "articulationScore" to Schema.integer("0-100 articulation and thought organization score"),
            "wordsPerMinute" to Schema.integer("Estimated speaking pace in words per minute")
        )
    )

    private val speechAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = speechAnalysisSchema
            }
        )
    }

    private val youtubeExtractionSchema = Schema.obj(
        properties = mapOf(
            "mainTopic" to Schema.string("Primary educational topic of the video"),
            "category" to Schema.enumeration(CurriculumCatalog.allCategories, "Best matching communication category"),
            "difficulty" to Schema.enumeration(listOf("Beginner", "Intermediate", "Advanced"), "Difficulty tier"),
            "techniques" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("Concise technique or framework name"),
                        "creatorAdvice" to Schema.string("What the creator actually taught in the provided title/description/transcript—never fabricated"),
                        "aiExplanation" to Schema.string("Supplementary coaching explanation clearly separated from creator words"),
                        "originalContext" to Schema.string("Context in the video where this idea was introduced"),
                        "instructions" to Schema.string("Step-by-step practice instructions grounded in the lesson"),
                        "practicalExercise" to Schema.string("Actionable speaking drill applying this lesson"),
                        "expectedOutcome" to Schema.string("Observable skill outcome after practice"),
                        "suggestedDurationMinutes" to Schema.integer("Recommended drill time in minutes (3-20)"),
                        "commonMistake" to Schema.string("Mistake discussed or implied"),
                        "correction" to Schema.string("Recommended fix"),
                        "timestampLabel" to Schema.string("Timestamp from transcript if available, otherwise 'Full video'")
                    )
                ),
                description = "1 to 3 non-duplicate techniques grounded strictly in the provided source text"
            )
        )
    )

    private val youtubeExtractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = youtubeExtractionSchema
            }
        )
    }

    private val vocabEvalSchema = Schema.obj(
        properties = mapOf(
            "usedCorrectly" to Schema.boolean("True if the target word is used naturally and accurately in context"),
            "naturalnessScore" to Schema.integer("0-100 score for contextual accuracy and tone"),
            "feedback" to Schema.string("Brief coaching explanation of usage accuracy"),
            "improvedExample" to Schema.string("Polished conversational sentence example")
        )
    )

    private val vocabEvalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = vocabEvalSchema
            }
        )
    }

    suspend fun analyzeSpeechRecording(
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = ""
    ): Result<SpeakingSession> = try {
        require(mediaBytes.isNotEmpty()) { "Recording is empty." }
        require(mediaBytes.size <= 19 * 1024 * 1024) { "Media file exceeds 19MB inline upload limit. Please keep clips under 3 minutes." }

        val comparisonContext = previousSession?.let {
            "Previous recording baseline -> Fluency: ${it.fluencyScore}, Pronunciation: ${it.pronunciationScore}, Vocabulary: ${it.vocabularyScore}, Grammar: ${it.grammarScore}, Articulation: ${it.articulationScore}, WPM: ${it.wordsPerMinute}."
        } ?: "No previous recording baseline yet."

        val prompt = """
            You are an evidence-based English communication coach grounded in Vinh Giang's (@askvinh) frameworks (3Vs, 5 Vocal Foundations: Rate, Pauses, Melody, Volume, Character; PREP; CLEAR).
            Analyze the attached spoken recording ($durationSeconds seconds) for module '$moduleType' and prompt: '$promptTitle'.
            $extraInstructions
            $comparisonContext
            CRITICAL EVIDENCE RULES:
            1. Transcribe accurately what you actually hear. Do NOT invent spoken words.
            2. If audio is silent, mostly noise, unintelligible, or too short (< 4 seconds), set pronunciationAssessable = false, pronunciationScore = 0, confidenceLevel = "Low", and explain clearly in audioQualityNote.
            3. Quote exact verbatim phrases from the recording in recordingExamples.
            4. Identify filler words, noticeably repeated expressions, articulation clarity, and thought organization.
            5. Compare delivery against the previous baseline if provided, and recommend one concrete next activity.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            speechAnalysisModel.generateContent(
                content {
                    inlineData(mediaBytes, mimeType)
                    text(prompt)
                }
            )
        }
        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty analysis.")
        val json = JSONObject(rawJson)

        val assessable = json.optBoolean("pronunciationAssessable", durationSeconds >= 4)
        val rawPron = json.optInt("pronunciationScore", if (assessable) 65 else 0).toLong().coerceIn(0L, 100L)
        val safePronScore = if (assessable) rawPron else 0L

        val session = SpeakingSession(
            moduleType = moduleType,
            promptTitle = promptTitle,
            durationSeconds = durationSeconds.toLong(),
            localMediaPath = localMediaPath,
            mediaType = mimeType,
            transcript = json.optString("transcript", ""),
            overallAssessment = json.optString("overallAssessment", "Analysis complete."),
            strengths = json.optJSONArray("strengths").toStringList(),
            mistakes = json.optJSONArray("mistakes").toStringList(),
            pronunciationNotes = buildList {
                val examples = json.optJSONArray("recordingExamples").toStringList()
                if (examples.isNotEmpty()) {
                    add("Quoted examples: ${examples.take(2).joinToString(" | ")}")
                }
                addAll(json.optJSONArray("pronunciationNotes").toStringList())
            },
            betterAlternatives = json.optJSONArray("betterAlternatives").toStringList(),
            fillerWordsDetected = json.optJSONArray("fillerWordsDetected").toStringList(),
            repeatedExpressions = json.optJSONArray("repeatedExpressions").toStringList(),
            improvementSuggestions = json.optJSONArray("improvementSuggestions").toStringList(),
            recommendedExercises = json.optJSONArray("recommendedExercises").toStringList(),
            pronunciationAssessable = assessable,
            articulationClarity = json.optString("articulationClarity", ""),
            thoughtOrganization = json.optString("thoughtOrganization", ""),
            nextRecommendedActivity = json.optString("nextRecommendedActivity", ""),
            confidenceLevel = json.optString("confidenceLevel", "Medium"),
            audioQualityNote = json.optString("audioQualityNote", ""),
            fluencyScore = json.optInt("fluencyScore", 65).toLong().coerceIn(0L, 100L),
            pronunciationScore = safePronScore,
            vocabularyScore = json.optInt("vocabularyScore", 65).toLong().coerceIn(0L, 100L),
            grammarScore = json.optInt("grammarScore", 65).toLong().coerceIn(0L, 100L),
            articulationScore = json.optInt("articulationScore", 65).toLong().coerceIn(0L, 100L),
            wordsPerMinute = json.optInt("wordsPerMinute", 120).toLong().coerceIn(0L, 400L)
        )
        Result.success(session)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Speech analysis failed", e)
        Result.failure(e)
    }

    suspend fun extractYouTubeLessons(
        videoId: String,
        videoTitle: String,
        videoUrl: String,
        description: String,
        transcriptText: String?
    ): Result<ExtractedYouTubeBundle> = try {
        val transcriptSection = if (!transcriptText.isNullOrBlank()) {
            "AVAILABLE CAPTIONS/TRANSCRIPT:\n${transcriptText.take(12000)}"
        } else {
            "NO CAPTIONS AVAILABLE. Rely ONLY on the real title and description below; explicitly note that detailed transcript timestamps are unavailable and do NOT invent spoken quotes."
        }
        val prompt = """
            Analyze this real YouTube educational video and extract structured English communication lessons.
            Video Title: $videoTitle
            Video URL: $videoUrl
            Description: ${description.take(2500)}
            $transcriptSection

            CRITICAL RULES:
            - Never fabricate video quotes or timestamps that are not in the provided text.
            - Clearly distinguish the creator's actual advice (creatorAdvice) from your supplementary coaching note (aiExplanation).
            - Include originalContext, step-by-step instructions, practicalExercise, expectedOutcome, and suggestedDurationMinutes.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            youtubeExtractionModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty extraction response.")
        val json = JSONObject(raw)
        val category = json.optString("category", "Public Speaking")
        val difficulty = json.optString("difficulty", "Intermediate")
        val mainTopic = json.optString("mainTopic", videoTitle)
        val techArr = json.optJSONArray("techniques") ?: JSONArray()
        val techniques = (0 until techArr.length()).map { idx ->
            val obj = techArr.getJSONObject(idx)
            ExtractedTechnique(
                videoId = videoId,
                title = obj.optString("title", mainTopic),
                category = category,
                difficulty = difficulty,
                creatorAdvice = obj.optString("creatorAdvice", ""),
                aiExplanation = obj.optString("aiExplanation", ""),
                originalContext = obj.optString("originalContext", ""),
                instructions = obj.optString("instructions", ""),
                practicalExercise = obj.optString("practicalExercise", ""),
                expectedOutcome = obj.optString("expectedOutcome", ""),
                suggestedDurationMinutes = obj.optInt("suggestedDurationMinutes", 5).toLong().coerceIn(2L, 30L),
                commonMistake = obj.optString("commonMistake", ""),
                correction = obj.optString("correction", ""),
                sourceVideoTitle = videoTitle,
                sourceVideoUrl = videoUrl,
                timestampLabel = obj.optString("timestampLabel", "Full video"),
                isCreatorDerived = !transcriptText.isNullOrBlank() || description.isNotBlank(),
                moderationStatus = "PUBLISHED"
            )
        }
        Result.success(ExtractedYouTubeBundle(mainTopic, category, difficulty, techniques))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "YouTube lesson extraction failed", e)
        Result.failure(e)
    }

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String
    ): Result<VocabEvaluationResult> = try {
        val prompt = """
            Evaluate if the English learner used the word "$word" (meaning: "$meaning") naturally and accurately in a real-world conversational sentence:
            Learner's sentence: "$userSentence"
            Focus on contextual appropriateness, collocations, and natural spoken English, not rote definition memorization.
        """.trimIndent()
        val response = withTimeout(AI_TIMEOUT_MS) { vocabEvalModel.generateContent(prompt) }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty evaluation."))
        Result.success(
            VocabEvaluationResult(
                usedCorrectly = json.optBoolean("usedCorrectly", false),
                naturalnessScore = json.optInt("naturalnessScore", 50).coerceIn(0, 100),
                feedback = json.optString("feedback", ""),
                improvedExample = json.optString("improvedExample", "")
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Vocab evaluation failed", e)
        Result.failure(e)
    }

    suspend fun sendConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> = try {
        val systemInstruction = """
            You are a realistic English conversation partner in the scenario: "$scenario".
            Adapt your vocabulary and speaking speed/complexity to a "$userLevel" English communicator.
            RULES:
            - Respond naturally in character (2-4 concise sentences) and ask an engaging follow-up question.
            - DO NOT interrupt the conversation with grammar corrections or scores during normal turns. Keep the dialogue flowing naturally.
        """.trimIndent()

        val history = mutableListOf<Content>(
            content("user") { text(systemInstruction) },
            content("model") { text("Understood. Let's begin the $scenario roleplay naturally.") }
        )
        historyPairs.forEach { (role, text) ->
            history.add(content(role) { text(text) })
        }

        val chat = generalModel.startChat(history = history)
        val response = withTimeout(AI_TIMEOUT_MS) { chat.sendMessage(userMessage) }
        val reply = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Partner returned empty reply.")
        Result.success(reply)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Conversation turn failed", e)
        Result.failure(e)
    }

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String
    ): Result<String> = try {
        val prompt = """
            Review this practice conversation for scenario "$scenario" using Vinh Giang's (@askvinh) communication principles (Active Listening, PREP structure, Friendly Authority, Word Choice):

            $transcript

            Provide a structured coaching debrief:
            1. Naturalness & Conversational Flow
            2. Stronger Phrasing Alternatives (quote exact user lines)
            3. Clarity & Active Listening Suggestions
            4. One Next Practice Drill
        """.trimIndent()
        val response = withTimeout(AI_TIMEOUT_MS) { generalModel.generateContent(prompt) }
        Result.success(response.text.orEmpty().ifBlank { "No feedback generated." })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Conversation check-in failed", e)
        Result.failure(e)
    }

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback> = emptyList(),
        userQuestion: String
    ): Flow<String> {
        val recentSummary = recentSessions.take(5).joinToString("\n") {
            "- ${it.promptTitle} (${it.moduleType}): Fluency ${it.fluencyScore}, Pron ${if (it.pronunciationAssessable) it.pronunciationScore else "N/A"}, Articulation ${it.articulationScore}; Mistakes: ${it.mistakes.take(2).joinToString("; ")}; Fillers: ${it.fillerWordsDetected.take(2).joinToString(", ")}"
        }.ifBlank { "No recorded sessions yet." }

        val fbSummary = recentFeedback.take(3).joinToString("\n") {
            "- Repeated: ${it.repeatedExpressions.joinToString(", ")}; Next activity: ${it.nextRecommendedActivity}"
        }

        val contextPrompt = """
            You are SpeakCraft's Personalized AI Communication Mentor, deeply trained on Vinh Giang's (@askvinh) teachings (3Vs, 5 Vocal Foundations, PREP, CLEAR, 3-Stage Video Audit, 4-Color Communicator, 4 Ways of Listening).
            Learner Profile:
            - Name: ${profile?.displayName ?: "Speaker"}, Level: ${profile?.communicationLevel ?: "Intermediate"}
            - Goal: ${profile?.primaryGoal ?: "Confident communication"}
            - Streak: ${profile?.streakDays ?: 0} days, Words Learned: ${profile?.wordsLearnedCount ?: 0}
            - Current Scores -> Fluency ${profile?.fluencyScore ?: 60}, Pronunciation ${profile?.pronunciationScore ?: 60}, Vocabulary ${profile?.vocabularyScore ?: 60}, Grammar ${profile?.grammarScore ?: 60}, Articulation ${profile?.articulationScore ?: 60}, Listening ${profile?.listeningScore ?: 60}
            Recent Speaking Sessions:
            $recentSummary
            $fbSummary

            User Request: $userQuestion

            Give tailored, motivating, concrete advice grounded in @askvinh lessons, referencing their actual weak spots and next step.
        """.trimIndent()

        return generalModel.generateContentStream(contextPrompt).map { it.text.orEmpty() }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }
    }
}
