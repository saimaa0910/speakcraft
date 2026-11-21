package com.example.data.ai.cloud

import android.util.Log
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityEvaluationResult
import com.example.data.activity.ActivityEvaluatorStrategy
import com.example.data.activity.ActivityUserResponse
import com.example.data.activity.EvaluationDimensionScore
import com.example.data.activity.LocalSpeechMetrics
import com.example.data.ai.ExtractedYouTubeBundle
import com.example.data.ai.VocabEvaluationResult
import com.example.data.model.ExtractedTechnique
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "FirebaseAiLogicService"
private const val AI_TIMEOUT_MS = 60_000L
private const val MAX_INLINE_MEDIA_BYTES = 19 * 1024 * 1024

interface LiveVoiceHandle {
    val isActive: Boolean
    suspend fun sendTextPrompt(text: String)
    suspend fun stopAndClose()
}

@OptIn(PublicPreviewAPI::class)
private class FirebaseLiveVoiceHandle(
    private val session: LiveSession
) : LiveVoiceHandle {
    override val isActive: Boolean
        get() = !session.isClosed() && session.isAudioConversationActive()

    override suspend fun sendTextPrompt(text: String) {
        if (!session.isClosed() && text.isNotBlank()) {
            session.send(text.trim(), turnComplete = true)
        }
    }

    override suspend fun stopAndClose() {
        runCatching { session.stopAudioConversation() }
        runCatching { session.close() }
    }
}

/**
 * Cloud AI Coach powered strictly by Firebase AI Logic (`Firebase.ai`) and App Check.
 * Uses zero API keys and propagates real backend/network errors transparently.
 */
class FirebaseAiLogicService {

    private val ai by lazy { Firebase.ai }

    fun modelNameForTier(tier: String): String = when (tier.lowercase()) {
        "deep" -> "gemini-4-argon"
        "fast" -> "gemini-3.5-flash-lite"
        else -> "gemini-3.8-flash"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Cloud Audio Transcription
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String,
        hintPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (audioBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No audio bytes to transcribe."))
            }
            if (audioBytes.size > MAX_INLINE_MEDIA_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio clip exceeds 19 MB Cloud AI limit.")
                )
            }
            val model = ai.generativeModel(
                modelName = "gemini-3.8-flash",
                systemInstruction = content {
                    text(
                        "You are a verbatim speech transcriber. Return ONLY the clean verbatim spoken transcript " +
                            "of the English speech in the audio, preserving natural filler words like 'um' or 'uh' if clearly spoken. " +
                            "Do not add commentary, labels, or timestamps."
                    )
                }
            )
            val safeMime = normalizeMediaMimeType(mimeType, isVideoDefault = false)
            val prompt = content {
                inlineData(audioBytes, safeMime)
                text(
                    buildString {
                        append("Transcribe all spoken words from this clip accurately.")
                        if (hintPrompt.isNotBlank()) {
                            append(" Topic context: ").append(hintPrompt.take(200))
                        }
                    }
                )
            }
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                Result.failure(IllegalStateException("Cloud AI returned an empty transcript."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "transcribeAudio timed out", e)
            Result.failure(IllegalStateException("Cloud audio transcription timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "transcribeAudio failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Multimodal Speech / Video Recording Coaching
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun analyzeSpeechRecording(
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = "",
        liveTranscript: String = "",
        modelTier: String = "balanced"
    ): Result<SpeakingSession> = withContext(Dispatchers.IO) {
        try {
            if (mediaBytes.size > MAX_INLINE_MEDIA_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Recording exceeds 19 MB inline upload limit for Cloud AI.")
                )
            }
            if (mediaBytes.isEmpty() && liveTranscript.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Record audio/video or provide spoken transcript first.")
                )
            }

            val schema = Schema.obj(
                properties = mapOf(
                    "transcript" to Schema.string("Clean verbatim transcript of what the speaker said"),
                    "overallAssessment" to Schema.string("Specific, encouraging 2-4 sentence executive coach synthesis quoting observed habits"),
                    "strengths" to Schema.array(Schema.string(), "2-4 specific observable strengths"),
                    "mistakes" to Schema.array(Schema.string(), "2-4 concrete areas to refine with exact phrasing"),
                    "pronunciationNotes" to Schema.array(Schema.string(), "Honest diction/articulation/cadence notes"),
                    "betterAlternatives" to Schema.array(Schema.string(), "2-3 direct Original -> Improved spoken rewrites"),
                    "fillerWordsDetected" to Schema.array(Schema.string(), "Filler expressions counted, e.g. 'um (2)'"),
                    "repeatedExpressions" to Schema.array(Schema.string(), "Noticeably repeated words or phrases"),
                    "improvementSuggestions" to Schema.array(Schema.string(), "2-4 concrete next steps"),
                    "recommendedExercises" to Schema.array(Schema.string(), "1-2 short follow-up drills"),
                    "pronunciationAssessable" to Schema.boolean("True if audio or detailed speech evidence was present"),
                    "articulationClarity" to Schema.string("One crisp sentence on consonant crispness and delivery"),
                    "thoughtOrganization" to Schema.string("One crisp sentence on structure (e.g., PREP / STAR / BLUF)"),
                    "nextRecommendedActivity" to Schema.string("Single highest-leverage 60-second drill to run next"),
                    "confidenceLevel" to Schema.string("High, Medium, or Low"),
                    "fluencyScore" to Schema.integer("0 to 100"),
                    "pronunciationScore" to Schema.integer("0 to 100"),
                    "vocabularyScore" to Schema.integer("0 to 100"),
                    "grammarScore" to Schema.integer("0 to 100"),
                    "articulationScore" to Schema.integer("0 to 100"),
                    "wordsPerMinute" to Schema.integer("Estimated speaking pace in words per minute")
                )
            )

            val selectedModelName = modelNameForTier(modelTier)
            val model = ai.generativeModel(
                modelName = selectedModelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                    temperature = 0.35f
                },
                systemInstruction = content {
                    text(
                        "You are SpeakCraft's Senior Communication Coach. Give warm, grounded, specific feedback " +
                            "grounded in what the learner actually spoke or wrote. Quote real phrases in betterAlternatives " +
                            "using 'Original -> Improved' format. Never fabricate laboratory phoneme metrics if audio quality is unclear."
                    )
                }
            )

            val prevSummary = previousSession?.let {
                "Previous session '${it.promptTitle}': fluency=${it.fluencyScore}, grammar=${it.grammarScore}, WPM=${it.wordsPerMinute}."
            } ?: "No prior session baseline."

            val promptContent = content {
                if (mediaBytes.isNotEmpty()) {
                    inlineData(mediaBytes, normalizeMediaMimeType(mimeType, isVideoDefault = false))
                }
                text(
                    buildString {
                        appendLine("Module: $moduleType")
                        appendLine("Prompt / Topic: $promptTitle")
                        appendLine("Estimated duration: ${durationSeconds.coerceAtLeast(1)} seconds")
                        appendLine("Prior baseline: $prevSummary")
                        if (extraInstructions.isNotBlank()) {
                            appendLine("Additional prompt instructions: $extraInstructions")
                        }
                        if (liveTranscript.isNotBlank()) {
                            appendLine("Captured transcript: $liveTranscript")
                        }
                        appendLine("Analyze this speaking attempt and produce structured coaching feedback matching the schema.")
                    }
                )
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(promptContent)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloud AI Coach returned an empty analysis."))
            }

            val obj = JSONObject(rawJson)
            val resolvedTranscript = obj.optString("transcript").ifBlank { liveTranscript }.trim()
            val wordCount = resolvedTranscript.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
            val fallbackWpm = if (durationSeconds >= 5 && wordCount > 0) {
                ((wordCount * 60.0) / durationSeconds).roundToInt().coerceIn(40, 260)
            } else 130
            val wpm = obj.optInt("wordsPerMinute", fallbackWpm).let {
                if (it <= 0 && wordCount > 0) fallbackWpm else it.coerceIn(0, 300)
            }

            val engineTag = "Analyzed by Cloud AI • Gemini (${tierLabel(modelTier)}) • ${durationSeconds.coerceAtLeast(1)}s"
            val session = SpeakingSession(
                moduleType = moduleType,
                promptTitle = promptTitle.ifBlank { "Communication Practice" },
                durationSeconds = durationSeconds.coerceAtLeast(1).toLong(),
                localMediaPath = localMediaPath,
                mediaType = mimeType,
                transcript = resolvedTranscript.ifBlank { "[Audio/Video recording analyzed by Cloud AI]" },
                overallAssessment = obj.optString("overallAssessment"),
                strengths = obj.optStringList("strengths"),
                mistakes = obj.optStringList("mistakes"),
                pronunciationNotes = obj.optStringList("pronunciationNotes"),
                betterAlternatives = obj.optStringList("betterAlternatives"),
                fillerWordsDetected = obj.optStringList("fillerWordsDetected"),
                repeatedExpressions = obj.optStringList("repeatedExpressions"),
                improvementSuggestions = obj.optStringList("improvementSuggestions"),
                recommendedExercises = obj.optStringList("recommendedExercises"),
                pronunciationAssessable = obj.optBoolean("pronunciationAssessable", true),
                articulationClarity = obj.optString("articulationClarity"),
                thoughtOrganization = obj.optString("thoughtOrganization"),
                nextRecommendedActivity = obj.optString("nextRecommendedActivity"),
                confidenceLevel = obj.optString("confidenceLevel", "Medium"),
                audioQualityNote = engineTag,
                fluencyScore = obj.optInt("fluencyScore", 76).coerceIn(20, 100).toLong(),
                pronunciationScore = obj.optInt("pronunciationScore", 76).coerceIn(20, 100).toLong(),
                vocabularyScore = obj.optInt("vocabularyScore", 76).coerceIn(20, 100).toLong(),
                grammarScore = obj.optInt("grammarScore", 76).coerceIn(20, 100).toLong(),
                articulationScore = obj.optInt("articulationScore", 76).coerceIn(20, 100).toLong(),
                wordsPerMinute = wpm.toLong()
            )
            Result.success(session)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "analyzeSpeechRecording timed out", e)
            Result.failure(IllegalStateException("Cloud AI speech analysis timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "analyzeSpeechRecording failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Streaming Personalized AI Mentor
    // ─────────────────────────────────────────────────────────────────────────

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback>,
        history: List<Pair<String, String>>,
        userQuestion: String,
        roleKey: String = "mentor",
        modelTier: String = "balanced"
    ): Flow<String> = flow {
        val roleDirective = when (roleKey.lowercase()) {
            "analyst" -> "Act as a crisp Communication Data Analyst: highlight patterns across pace (WPM), fillers, scores, and highest-ROI drills."
            "interviewer" -> "Act as an Executive & Hiring Interview Coach: evaluate clarity, STAR/BLUF rigor, authority, and give tightened spoken rewrites."
            "quick_coach" -> "Be ultra-concise and practical: max 3 sharp bullets + 1 immediate 60-second exercise."
            else -> "Act as SpeakCraft's warm Senior Communication Mentor: encouraging, insightful, and concrete with word-for-word spoken upgrades."
        }

        val profileContext = buildString {
            val name = profile?.displayName?.ifBlank { "Learner" } ?: "Learner"
            val level = profile?.communicationLevel?.ifBlank { "Intermediate" } ?: "Intermediate"
            val goal = profile?.primaryGoal?.ifBlank { "confident communication" } ?: "confident communication"
            appendLine("Learner: $name | Level: $level | Goal: $goal")
            if (profile != null) {
                appendLine(
                    "Skill Scores: Fluency=${profile.fluencyScore}, Pronunciation=${profile.pronunciationScore}, " +
                        "Vocabulary=${profile.vocabularyScore}, Grammar=${profile.grammarScore}, " +
                        "Articulation=${profile.articulationScore}, Listening=${profile.listeningScore}"
                )
            }
            if (recentSessions.isNotEmpty()) {
                appendLine("Recent Practice Sessions:")
                recentSessions.take(4).forEach { s ->
                    appendLine(
                        "- '${s.promptTitle}' (${s.moduleType}): Fluency=${s.fluencyScore}, Grammar=${s.grammarScore}, " +
                            "Vocab=${s.vocabularyScore}, Pace=${s.wordsPerMinute} WPM, Fillers=${s.fillerWordsDetected.take(3)}"
                    )
                }
            }
            if (recentFeedback.isNotEmpty()) {
                val next = recentFeedback.firstOrNull()?.nextRecommendedActivity.orEmpty()
                if (next.isNotBlank()) appendLine("Latest recommended drill: $next")
            }
        }

        val model = ai.generativeModel(
            modelName = modelNameForTier(modelTier),
            systemInstruction = content {
                text(
                    "$roleDirective Ground every answer in the learner's real SpeakCraft data below. " +
                        "If the user pastes a sentence or draft, rewrite it verbatim into a cleaner spoken version and explain why.\n\n$profileContext"
                )
            }
        )

        val chatHistory = history.takeLast(12).map { (role, text) ->
            val geminiRole = if (role.equals("user", ignoreCase = true)) "user" else "model"
            content(role = geminiRole) { text(text) }
        }
        val chat = model.startChat(history = chatHistory)

        var emittedAny = false
        withTimeout(AI_TIMEOUT_MS) {
            chat.sendMessageStream(userQuestion).collect { chunk ->
                val text = chunk.text
                if (!text.isNullOrEmpty()) {
                    emittedAny = true
                    emit(text)
                }
            }
        }
        if (!emittedAny) {
            throw IllegalStateException("Cloud AI Mentor returned no content.")
        }
    }.flowOn(Dispatchers.IO)

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Roleplay Conversation Turn & Coaching Debrief
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String,
        modelTier: String = "balanced"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = ai.generativeModel(
                modelName = modelNameForTier(modelTier),
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 260
                },
                systemInstruction = content {
                    text(
                        "You are a realistic English conversation partner in the scenario: \"$scenario\". " +
                            "Learner proficiency: $userLevel. Stay strictly in character, respond naturally in 2-4 crisp sentences, " +
                            "ask one engaging follow-up question, and never repeat an earlier question. " +
                            "Optionally add at most one concise bracketed note at the end like [Coach tip: ...] only when a clear phrasing upgrade exists."
                    )
                }
            )
            val chatHistory = historyPairs.takeLast(16).map { (role, text) ->
                val geminiRole = if (role.equals("user", ignoreCase = true)) "user" else "model"
                content(role = geminiRole) { text(text) }
            }
            val chat = model.startChat(history = chatHistory)
            val response = withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessage(userMessage)
            }
            val reply = response.text?.trim().orEmpty()
            if (reply.isBlank()) {
                Result.failure(IllegalStateException("Cloud AI partner returned an empty reply."))
            } else {
                Result.success(reply)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "generateConversationTurn timed out", e)
            Result.failure(IllegalStateException("Cloud AI conversation partner timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateConversationTurn failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String,
        modelTier: String = "balanced"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = ai.generativeModel(
                modelName = modelNameForTier(modelTier),
                systemInstruction = content {
                    text(
                        "Provide a sharp, practical communication coaching debrief on the conversation transcript. " +
                            "Include: What Went Well (2-3 bullets), High-Leverage Upgrades with exact 'Original -> Better' rewrites (2 bullets), " +
                            "and Next Practice Drill (1 bullet)."
                    )
                }
            )
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        text("Scenario: $scenario\n\nTranscript:\n$transcript")
                    }
                )
            }
            val debrief = response.text?.trim().orEmpty()
            if (debrief.isBlank()) {
                Result.failure(IllegalStateException("Cloud AI debrief returned empty text."))
            } else {
                Result.success("Cloud AI Coaching Debrief — $scenario\n\n$debrief")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "generateConversationCheckIn timed out", e)
            Result.failure(IllegalStateException("Cloud AI debrief timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateConversationCheckIn failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Real-Time Gemini Live Voice Session (Bi-Directional Audio)
    // ─────────────────────────────────────────────────────────────────────────

    @OptIn(PublicPreviewAPI::class)
    suspend fun startLiveVoiceSession(
        scenario: String,
        userLevel: String,
        onInputTranscript: (String) -> Unit,
        onOutputTranscript: (String) -> Unit,
        onGoAway: (String) -> Unit,
        onError: (Throwable) -> Unit
    ): Result<LiveVoiceHandle> = withContext(Dispatchers.IO) {
        var liveSession: LiveSession? = null
        try {
            val liveModel = ai.liveModel(
                modelName = "gemini-3.8-flash",
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice("Aoede"))
                    inputAudioTranscription = AudioTranscriptionConfig()
                    outputAudioTranscription = AudioTranscriptionConfig()
                },
                systemInstruction = content {
                    text(
                        "You are SpeakCraft's live voice conversation coach in scenario: \"$scenario\". " +
                            "Adapt vocabulary and cadence to a $userLevel speaker. Keep turns lively, supportive, and 2-3 sentences max, " +
                            "ending with a natural follow-up question."
                    )
                }
            )
            val session = withTimeout(AI_TIMEOUT_MS) {
                liveModel.connect()
            }
            liveSession = session
            session.startAudioConversation(
                functionCallHandler = { call ->
                    FunctionResponsePart(call.name, buildJsonObject { put("status", "ok") })
                },
                transcriptHandler = { inputTranscription, outputTranscription ->
                    inputTranscription?.text?.trim()?.takeIf { it.isNotBlank() }?.let(onInputTranscript)
                    outputTranscription?.text?.trim()?.takeIf { it.isNotBlank() }?.let(onOutputTranscript)
                },
                goAwayHandler = { goAway ->
                    onGoAway("Live session closing soon (${goAway.timeLeft}). Switching cleanly when finished.")
                },
                enableInterruptions = true
            )
            Result.success(FirebaseLiveVoiceHandle(session))
        } catch (e: SecurityException) {
            runCatching { liveSession?.close() }
            Log.e(TAG, "startLiveVoiceSession denied RECORD_AUDIO permission", e)
            Result.failure(SecurityException("Microphone permission is required for live voice sessions.", e))
        } catch (e: TimeoutCancellationException) {
            runCatching { liveSession?.close() }
            Log.e(TAG, "startLiveVoiceSession timed out", e)
            Result.failure(IllegalStateException("Timed out connecting to Gemini Live Voice."))
        } catch (e: CancellationException) {
            runCatching { liveSession?.close() }
            throw e
        } catch (e: Exception) {
            runCatching { liveSession?.close() }
            Log.e(TAG, "startLiveVoiceSession failed", e)
            onError(e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Vocabulary Stage 4 Semantic & Contextual Evaluator
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String,
        difficulty: String = "Intermediate"
    ): Result<VocabEvaluationResult> = withContext(Dispatchers.IO) {
        try {
            val schema = Schema.obj(
                properties = mapOf(
                    "usedCorrectly" to Schema.boolean("True when the target word fits semantically, grammatically, and idiomatically"),
                    "naturalnessScore" to Schema.integer("Score from 0 to 100 reflecting semantic accuracy, grammar, and naturalness"),
                    "feedback" to Schema.string("Concise 2-sentence explanation citing how the word worked and any fix needed"),
                    "improvedExample" to Schema.string("More natural, executive-ready sentence keeping the learner's intended topic")
                )
            )
            val model = ai.generativeModel(
                modelName = "gemini-3.5-flash-lite",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                    temperature = 0.25f
                }
            )
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        text(
                            "Evaluate whether this $difficulty English learner used the target word naturally and accurately.\n" +
                                "Target Word: $word\n" +
                                "Meaning: $meaning\n" +
                                "Learner Sentence: \"$userSentence\""
                        )
                    }
                )
            }
            val raw = response.text?.trim().orEmpty()
            if (raw.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloud AI vocabulary evaluation returned empty response."))
            }
            val obj = JSONObject(raw)
            Result.success(
                VocabEvaluationResult(
                    usedCorrectly = obj.optBoolean("usedCorrectly", false),
                    naturalnessScore = obj.optInt("naturalnessScore", 65).coerceIn(0, 100),
                    feedback = obj.optString("feedback", "Evaluated by Cloud AI Coach."),
                    improvedExample = obj.optString("improvedExample", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "evaluateVocabularyUsage timed out", e)
            Result.failure(IllegalStateException("Cloud vocabulary check timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "evaluateVocabularyUsage failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. YouTube Lesson & Technique Extractor (Up to 5 Structured Drills)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun extractYouTubeLessons(
        videoId: String,
        videoTitle: String,
        videoUrl: String,
        description: String,
        transcriptText: String?,
        modelTier: String = "balanced"
    ): Result<ExtractedYouTubeBundle> = withContext(Dispatchers.IO) {
        try {
            val techniqueSchema = Schema.obj(
                properties = mapOf(
                    "title" to Schema.string("Clear drill title"),
                    "creatorAdvice" to Schema.string("Direct paraphrase/quote of the creator's insight"),
                    "aiExplanation" to Schema.string("Why this communication technique works psychologically and vocally"),
                    "instructions" to Schema.string("3 numbered practical steps to practice today"),
                    "practicalExercise" to Schema.string("60-second recording prompt"),
                    "expectedOutcome" to Schema.string("Measurable speaking improvement"),
                    "commonMistake" to Schema.string("Common trap speakers fall into"),
                    "correction" to Schema.string("Exact physical/verbal adjustment"),
                    "timestampLabel" to Schema.string("Timestamp like 01:15")
                )
            )
            val bundleSchema = Schema.obj(
                properties = mapOf(
                    "mainTopic" to Schema.string("Focused communication theme"),
                    "category" to Schema.string("One of: Voice Mastery, Storytelling, Body Language, Active Listening, Conversation Mastery, Pronunciation & Articulation, Professional Communication, Public Speaking, Speaking Clarity"),
                    "difficulty" to Schema.string("Beginner, Intermediate, or Advanced"),
                    "techniques" to Schema.array(techniqueSchema, "3 to 5 distinct practical drills extracted from the video")
                )
            )
            val model = ai.generativeModel(
                modelName = modelNameForTier(modelTier),
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = bundleSchema
                    temperature = 0.35f
                }
            )
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        text(
                            buildString {
                                appendLine("Extract up to 5 actionable communication coaching drills strictly attributed to this source video.")
                                appendLine("Title: $videoTitle")
                                appendLine("URL: $videoUrl")
                                if (description.isNotBlank()) appendLine("Description:\n${description.take(2500)}")
                                if (!transcriptText.isNullOrBlank()) appendLine("Transcript:\n${transcriptText.take(6500)}")
                            }
                        )
                    }
                )
            }
            val raw = response.text?.trim().orEmpty()
            if (raw.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloud AI YouTube extractor returned empty JSON."))
            }
            val obj = JSONObject(raw)
            val category = obj.optString("category", "Speaking Clarity")
            val difficulty = obj.optString("difficulty", "Intermediate")
            val mainTopic = obj.optString("mainTopic", videoTitle)
            val arr = obj.optJSONArray("techniques") ?: JSONArray()
            val techniques = (0 until arr.length()).mapNotNull { idx ->
                val item = arr.optJSONObject(idx) ?: return@mapNotNull null
                ExtractedTechnique(
                    videoId = videoId,
                    title = item.optString("title").ifBlank { "$videoTitle — Drill ${idx + 1}" },
                    category = category,
                    difficulty = difficulty,
                    creatorAdvice = item.optString("creatorAdvice"),
                    aiExplanation = item.optString("aiExplanation"),
                    originalContext = videoTitle,
                    instructions = item.optString("instructions"),
                    practicalExercise = item.optString("practicalExercise"),
                    expectedOutcome = item.optString("expectedOutcome"),
                    suggestedDurationMinutes = 5L,
                    commonMistake = item.optString("commonMistake"),
                    correction = item.optString("correction"),
                    sourceVideoTitle = videoTitle,
                    sourceVideoUrl = videoUrl,
                    timestampLabel = item.optString("timestampLabel", "00:00"),
                    isCreatorDerived = true,
                    moderationStatus = "PUBLISHED"
                )
            }
            if (techniques.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Cloud AI could not extract structured techniques from this source."))
            }
            Result.success(
                ExtractedYouTubeBundle(
                    mainTopic = mainTopic,
                    category = category,
                    difficulty = difficulty,
                    techniques = techniques
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "extractYouTubeLessons timed out", e)
            Result.failure(IllegalStateException("Cloud YouTube extraction timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "extractYouTubeLessons failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Universal Activity Engine Cloud AI Evaluator
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun evaluateUniversalActivity(
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        strategy: ActivityEvaluatorStrategy,
        metrics: LocalSpeechMetrics,
        modelTier: String = "balanced"
    ): Result<ActivityEvaluationResult> = withContext(Dispatchers.IO) {
        try {
            val dimSchema = Schema.obj(
                properties = mapOf(
                    "key" to Schema.string(),
                    "label" to Schema.string(),
                    "score" to Schema.integer("0 to 100"),
                    "note" to Schema.string("Concrete evidence-grounded justification")
                )
            )
            val resultSchema = Schema.obj(
                properties = mapOf(
                    "overallObjectiveScore" to Schema.integer("0 to 100"),
                    "dimensionScores" to Schema.array(dimSchema, "Scores matching every requested rubric dimension key"),
                    "whatWentWell" to Schema.array(Schema.string(), "2-3 specific strengths"),
                    "specificMistakes" to Schema.array(Schema.string(), "1-3 concrete growth points"),
                    "correctedExamples" to Schema.array(Schema.string(), "2 natural upgraded spoken examples quoting or adapting user's response"),
                    "explanation" to Schema.string("Clear 2-3 sentence synthesis"),
                    "improvementSuggestions" to Schema.array(Schema.string(), "2-3 actionable drills"),
                    "nextPracticeTask" to Schema.string("Single immediate follow-up drill")
                )
            )

            val model = ai.generativeModel(
                modelName = modelNameForTier(modelTier),
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = resultSchema
                    temperature = 0.3f
                }
            )

            val rubricText = activity.scoringRubric.joinToString("\n") {
                "- key=${it.key}, label=${it.label}, weight=${it.weightPercent}%, criteria=${it.criteriaDescription}"
            }
            val mediaBytes = response.mediaBytes?.takeIf { it.isNotEmpty() && it.size <= MAX_INLINE_MEDIA_BYTES }

            val aiResp = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        if (mediaBytes != null) {
                            inlineData(mediaBytes, normalizeMediaMimeType(response.mediaMimeType, isVideoDefault = false))
                        }
                        text(
                            buildString {
                                appendLine("Activity: ${activity.title} (${activity.skillCategory} • ${activity.difficulty})")
                                appendLine("Objective: ${activity.learningObjective}")
                                appendLine("Evaluator Role (${strategy.evaluatorName}): ${strategy.buildEvaluatorInstructions(activity, response)}")
                                appendLine("Rubric Dimensions:\n$rubricText")
                                appendLine("Completed Checklist Items: ${response.completedChecklistItems.joinToString(", ").ifBlank { "None" }}")
                                appendLine("Measured Local Metrics: words=${metrics.wordCount}, WPM=${metrics.wordsPerMinute}, fillers=${metrics.fillerCount} (${metrics.detectedFillers.joinToString()})")
                                appendLine("User Response / Dialogue:\n${response.combinedTextForAnalysis.take(4000)}")
                            }
                        )
                    }
                )
            }

            val raw = aiResp.text?.trim().orEmpty()
            if (raw.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Cloud Activity evaluator returned empty response."))
            }
            val obj = JSONObject(raw)
            val overallScore = obj.optInt("overallObjectiveScore", 74).coerceIn(20, 100)
            val rawDims = obj.optJSONArray("dimensionScores") ?: JSONArray()
            val dimMap = mutableMapOf<String, EvaluationDimensionScore>()
            for (i in 0 until rawDims.length()) {
                val d = rawDims.optJSONObject(i) ?: continue
                val key = d.optString("key")
                if (key.isNotBlank()) {
                    dimMap[key] = EvaluationDimensionScore(
                        key = key,
                        label = d.optString("label", key),
                        score = d.optInt("score", overallScore).coerceIn(20, 100),
                        note = d.optString("note")
                    )
                }
            }
            val finalDims = activity.scoringRubric.map { rubric ->
                dimMap[rubric.key] ?: EvaluationDimensionScore(
                    key = rubric.key,
                    label = rubric.label,
                    score = overallScore,
                    note = "Assessed by Cloud AI Coach against ${rubric.label}."
                )
            }

            Result.success(
                ActivityEvaluationResult(
                    activityId = activity.activityId,
                    evaluatorName = "${strategy.evaluatorName} • Cloud Gemini (${tierLabel(modelTier)})",
                    overallObjectiveScore = overallScore,
                    passedCriteria = overallScore >= activity.completionCriteria.passingScoreThreshold,
                    dimensionScores = finalDims,
                    whatWentWell = obj.optStringList("whatWentWell"),
                    specificMistakes = obj.optStringList("specificMistakes"),
                    correctedExamples = obj.optStringList("correctedExamples"),
                    explanation = obj.optString("explanation"),
                    improvementSuggestions = obj.optStringList("improvementSuggestions"),
                    nextPracticeTask = obj.optString("nextPracticeTask"),
                    honestyNotice = "Evaluated by Cloud AI Coach (Gemini ${tierLabel(modelTier)}). ${strategy.honestyCaveat(activity, response)}".trim(),
                    aiEvaluated = true,
                    confidenceSelfRating = response.confidenceSelfRating,
                    wordsPerMinute = metrics.wordsPerMinute,
                    fillerCount = metrics.fillerCount,
                    wordCount = metrics.wordCount,
                    recommendedNextActivityIds = activity.nextActivityIds
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "evaluateUniversalActivity timed out", e)
            Result.failure(IllegalStateException("Cloud activity evaluation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "evaluateUniversalActivity failed", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON & Media Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun tierLabel(tier: String): String = when (tier.lowercase()) {
        "deep" -> "Pro Reasoning"
        "fast" -> "Fast Lite"
        else -> "Balanced Flash"
    }

    private fun normalizeMediaMimeType(raw: String, isVideoDefault: Boolean): String {
        val clean = raw.trim().lowercase()
        return when {
            clean.startsWith("audio/") || clean.startsWith("video/") -> clean
            isVideoDefault -> "video/mp4"
            else -> "audio/mp4"
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i).trim()
                if (s.isNotBlank()) add(s)
            }
        }
    }
}
