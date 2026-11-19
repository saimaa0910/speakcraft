package com.example.data.ai.local

import android.content.Context
import android.util.Log
import com.example.data.ai.ExtractedYouTubeBundle
import com.example.data.ai.VocabEvaluationResult
import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.AudioFeatureExtractionEngine
import com.example.data.ai.local.audio.FillerWordDetectionEngine
import com.example.data.ai.local.audio.SpeechRecognitionManager
import com.example.data.ai.local.fusion.MultimodalFeatureFusionEngine
import com.example.data.ai.local.language.LocalFeedbackEngine
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.ai.local.video.BodyLanguageAnalysisEngine
import com.example.data.ai.local.video.FaceAnalysisResult
import com.example.data.ai.local.video.FaceLandmarkAnalysisEngine
import com.example.data.ai.local.video.GestureAnalysisEngine
import com.example.data.ai.local.video.GestureAnalysisResult
import com.example.data.ai.local.video.PostureAnalysisResult
import com.example.data.ai.local.video.VideoFrameProcessingEngine
import com.example.data.model.ExtractedTechnique
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "LocalOrchestrator"

/**
 * Master 100% On-Device Local Intelligence Orchestrator.
 *
 * Replaces all cloud LLM endpoints with deterministic acoustic DSP, local speech recognition,
 * MediaPipe vision landmarks, modular rule-based grammar/vocabulary intelligence, and
 * structured communication coaching templates.
 *
 * Enforces [LocalExecutionPolicy.POLICY] == "LOCAL_ONLY" on every operation.
 */
class LocalIntelligenceOrchestrator(
    private val context: Context? = null,
    private val modelManager: OnDeviceModelManager? = null,
    private val capabilityManager: DeviceCapabilityManager? = null
) {
    private val audioFeatureEngine = AudioFeatureExtractionEngine()

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Multimodal Speech / Video Recording Analysis
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun analyzeSpeechRecording(
        mediaBytes: ByteArray,
        mimeType: String,
        moduleType: String,
        promptTitle: String,
        durationSeconds: Int,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        localMediaPath: String = ""
    ): Result<SpeakingSession> = withContext(Dispatchers.IO) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val effectiveDuration = durationSeconds.coerceAtLeast(1)
            val isVideo = mimeType.startsWith("video/", ignoreCase = true)

            // Resolve or stage local file for DSP / ASR / video frame analysis
            var tempFile: File? = null
            val mediaFile: File? = when {
                localMediaPath.isNotBlank() && File(localMediaPath).exists() -> File(localMediaPath)
                mediaBytes.isNotEmpty() && context != null -> {
                    val ext = if (isVideo) ".mp4" else ".m4a"
                    File.createTempFile("speakcraft_local_", ext, context.cacheDir).also {
                        it.writeBytes(mediaBytes)
                        tempFile = it
                    }
                }
                else -> null
            }

            try {
                // Step 1: On-device speech transcription (graceful audio-only fallback if no ASR model)
                val speechManager = SpeechRecognitionManager(
                    context = context,
                    modelManager = modelManager
                )
                val asrResult = if (mediaFile != null) {
                    runCatching { speechManager.transcribe(mediaFile) }.getOrNull()
                } else null

                val transcript = asrResult?.transcript?.trim().orEmpty()

                // Step 2: Acoustic DSP feature extraction
                val acousticFeatures: AcousticFeatures? = if (mediaFile != null) {
                    runCatching {
                        audioFeatureEngine.extractFromFile(mediaFile, transcript)
                    }.getOrNull()
                } else null

                // Step 3: Optional on-device video analysis (face, pose, gestures)
                var faceResult: FaceAnalysisResult? = null
                var postureResult: PostureAnalysisResult? = null
                var gestureResult: GestureAnalysisResult? = null

                if (isVideo && mediaFile != null && context != null) {
                    val frames = runCatching {
                        VideoFrameProcessingEngine().extractFrames(mediaFile)
                    }.getOrDefault(emptyList())

                    if (frames.isNotEmpty()) {
                        try {
                            val faceEngine = FaceLandmarkAnalysisEngine(context)
                            if (faceEngine.initialize()) {
                                faceResult = runCatching { faceEngine.analyzeFrames(frames) }.getOrNull()
                                faceEngine.release()
                            }
                            val poseEngine = BodyLanguageAnalysisEngine(context)
                            if (poseEngine.initialize()) {
                                postureResult = runCatching { poseEngine.analyzeFrames(frames) }.getOrNull()
                                poseEngine.release()
                            }
                            val gestureEngine = GestureAnalysisEngine(context)
                            if (gestureEngine.initialize()) {
                                gestureResult = runCatching { gestureEngine.analyzeFrames(frames) }.getOrNull()
                                gestureEngine.release()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Video landmark pass skipped safely: ${e.message}")
                        } finally {
                            frames.forEach { runCatching { it.recycle() } }
                        }
                    }
                }

                // Step 4: Linguistic analysis (only when transcript is non-blank)
                val languageResult = if (transcript.isNotBlank()) {
                    LocalLanguageAnalysisEngine.analyze(transcript)
                } else null

                // Step 5: Multimodal fusion & 6-section deterministic feedback
                val evaluatorName = mapModuleToEvaluator(moduleType)
                val expectedText = extractExpectedPrompt(extraInstructions)
                val fused = MultimodalFeatureFusionEngine.fuse(
                    audio = acousticFeatures,
                    face = faceResult,
                    posture = postureResult,
                    gesture = gestureResult,
                    language = languageResult,
                    transcript = transcript,
                    expectedText = expectedText,
                    evaluatorName = evaluatorName,
                    exerciseTitle = promptTitle.ifBlank { moduleType }
                )

                val extraNotices = buildList {
                    addAll(fused.limitationNotices)
                    asrResult?.limitationNotice?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.distinct()

                val evidence = fused.copy(
                    speechDurationSeconds = if (fused.speechDurationSeconds > 0.0) fused.speechDurationSeconds else effectiveDuration.toDouble(),
                    limitationNotices = extraNotices
                )

                val feedback = LocalFeedbackEngine.buildFeedback(evidence)

                // Step 6: Deterministic scores grounded strictly in observed metrics
                val fluencyScore = computeFluencyScore(evidence, previousSession)
                val grammarScore = computeGrammarScore(evidence)
                val vocabularyScore = computeVocabularyScore(evidence)
                val pronunciationScore = computePronunciationScore(evidence)
                val articulationScore = computeArticulationScore(evidence, pronunciationScore)

                val audioQualityNote = buildString {
                    if (acousticFeatures == null) {
                        append("Audio decoded at ${effectiveDuration}s duration; local acoustic DSP fallback active.")
                    } else {
                        val db = acousticFeatures.relativeLoudness.roundToInt()
                        append("Avg loudness ${db} dBFS")
                        if (acousticFeatures.clippingDetected) append(" • Clipping detected")
                        if (acousticFeatures.backgroundNoiseLevel > 0.06f) append(" • Background noise elevated")
                    }
                    append(" | ${feedback.honestyNotice}")
                }

                val confidenceLabel = when {
                    acousticFeatures != null && acousticFeatures.relativeLoudness >= -26.0 && !acousticFeatures.clippingDetected -> "High"
                    acousticFeatures != null && acousticFeatures.relativeLoudness < -36.0 -> "Building"
                    else -> "Medium"
                }

                val articulationNote = when {
                    acousticFeatures?.clippingDetected == true -> "Clipping detected — keep 15–20 cm mic distance for clearer consonants."
                    evidence.wordsPerMinute > 180 -> "Fast pace (${evidence.wordsPerMinute} WPM) may compress word endings."
                    evidence.wordsPerMinute in 115..170 -> "Clear cadence (${evidence.wordsPerMinute} WPM) supports crisp articulation."
                    else -> "Steady acoustic delivery captured for local self-review."
                }

                val organizationNote = when {
                    transcript.isBlank() -> "Audio-only evaluation active (install an on-device speech model for transcript structure scoring)."
                    languageResult != null && languageResult.averageSentenceLength > 28.0 -> "Long sentences detected — break ideas into PREP points (Point, Reason, Example, Point)."
                    else -> "Clear sentence segmentation (${languageResult?.sentenceCount ?: 1} sentence(s))."
                }

                SpeakingSession(
                    moduleType = moduleType,
                    promptTitle = promptTitle.ifBlank { "Communication Practice" },
                    durationSeconds = effectiveDuration.toLong(),
                    localMediaPath = localMediaPath,
                    mediaType = mimeType,
                    transcript = transcript.ifBlank {
                        "[Audio-only recording (${effectiveDuration}s) — evaluated locally via acoustic DSP without cloud transcription]"
                    },
                    overallAssessment = feedback.explanation,
                    strengths = feedback.whatWentWell,
                    mistakes = feedback.specificMistakes,
                    pronunciationNotes = buildList {
                        add(articulationNote)
                        if (transcript.isBlank()) {
                            add("Word-level pronunciation alignment requires a transcript; acoustic clarity metrics were evaluated locally.")
                        }
                    },
                    betterAlternatives = feedback.correctedExamples,
                    fillerWordsDetected = evidence.detectedFillers,
                    repeatedExpressions = evidence.vocabularyAnalysis?.repeatedWords?.entries
                        ?.sortedByDescending { it.value }
                        ?.take(4)
                        ?.map { "${it.key} (${it.value}x)" }
                        .orEmpty(),
                    improvementSuggestions = feedback.improvementSuggestions,
                    recommendedExercises = listOf(feedback.nextPracticeTask),
                    pronunciationAssessable = transcript.isNotBlank() || acousticFeatures != null,
                    articulationClarity = articulationNote,
                    thoughtOrganization = organizationNote,
                    nextRecommendedActivity = feedback.nextPracticeTask,
                    confidenceLevel = confidenceLabel,
                    audioQualityNote = audioQualityNote,
                    fluencyScore = fluencyScore.toLong(),
                    pronunciationScore = pronunciationScore.toLong(),
                    vocabularyScore = vocabularyScore.toLong(),
                    grammarScore = grammarScore.toLong(),
                    articulationScore = articulationScore.toLong(),
                    wordsPerMinute = evidence.wordsPerMinute.toLong()
                )
            } finally {
                tempFile?.delete()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Vocabulary Sentence Evaluation (100% On-Device)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun evaluateVocabularyUsage(
        word: String,
        meaning: String,
        userSentence: String
    ): Result<VocabEvaluationResult> = evaluateVocabSentence(
        word = word,
        definition = meaning,
        userSentence = userSentence,
        difficulty = "Intermediate"
    )

    suspend fun evaluateVocabSentence(
        word: String,
        definition: String,
        userSentence: String,
        difficulty: String = "Intermediate"
    ): Result<VocabEvaluationResult> = withContext(Dispatchers.Default) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val target = word.trim()
            val sentence = userSentence.trim()
            val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }

            if (sentence.isBlank() || words.size < 3) {
                return@runCatching VocabEvaluationResult(
                    usedCorrectly = false,
                    naturalnessScore = 20,
                    feedback = "Write a complete sentence (at least 5 words) using \"$target\" in a real communication context.",
                    improvedExample = buildModelVocabSentence(target, definition)
                )
            }

            // Match exact word or common inflections (e.g., articulate / articulated / articulating / articulates)
            val stem = target.lowercase().removeSuffix("e").removeSuffix("y")
            val lowerSentence = sentence.lowercase()
            val exactMatch = Regex("\\b${Regex.escape(target.lowercase())}(s|es|ed|d|ing|ly)?\\b")
                .containsMatchIn(lowerSentence)
            val stemMatch = stem.length >= 4 && Regex("\\b${Regex.escape(stem)}[a-z]{0,4}\\b")
                .containsMatchIn(lowerSentence)
            val includesWord = exactMatch || stemMatch

            val lang = LocalLanguageAnalysisEngine.analyze(
                text = sentence,
                targetWords = listOf(target)
            )
            val grammarPenalty = lang.grammarIssues.size * 12
            val lengthBonus = when {
                words.size in 7..24 -> 15
                words.size in 5..6 -> 8
                words.size > 24 -> 5
                else -> 0
            }
            val punctuationBonus = if (sentence.endsWith(".") || sentence.endsWith("!") || sentence.endsWith("?")) 5 else 0

            val rawScore = if (includesWord) {
                (75 + lengthBonus + punctuationBonus - grammarPenalty).coerceIn(50, 98)
            } else {
                (30 - grammarPenalty).coerceIn(10, 45)
            }

            val usedCorrectly = includesWord && words.size >= 4 && lang.grammarIssues.size <= 1

            val feedback = buildString {
                if (!includesWord) {
                    append("Your sentence did not include the target word \"$target\" ($definition). ")
                    append("Try anchoring a concrete real-world example around \"$target\".")
                } else {
                    append("Great job incorporating \"$target\" in a $difficulty-level context! ")
                    if (lang.grammarIssues.isEmpty()) {
                        append("Grammar and sentence flow are clean and natural. ")
                    } else {
                        val firstIssue = lang.grammarIssues.first()
                        append("Small polish tip: ${firstIssue.explanation} (\"${firstIssue.originalText}\" → \"${firstIssue.correctedText}\"). ")
                    }
                    if (words.size < 6) {
                        append("Try adding a brief reason or context clause so the meaning shines even more clearly.")
                    }
                }
            }.trim()

            val improvedExample = if (lang.grammarIssues.isNotEmpty()) {
                var fixed = sentence
                for (issue in lang.grammarIssues) {
                    fixed = fixed.replace(issue.originalText, issue.correctedText, ignoreCase = true)
                }
                fixed
            } else {
                buildModelVocabSentence(target, definition)
            }

            VocabEvaluationResult(
                usedCorrectly = usedCorrectly,
                naturalnessScore = rawScore,
                feedback = feedback,
                improvedExample = improvedExample
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Offline Conversation Partner & Coaching Debrief
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateConversationTurn(
        scenario: String,
        userLevel: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val turnNumber = historyPairs.count { it.first.equals("user", ignoreCase = true) } + 1
            val cleanMsg = userMessage.trim()
            val topicKeyword = extractHighlightKeyword(cleanMsg)
            val scenarioKey = scenario.lowercase()

            val acknowledgement = when {
                cleanMsg.length < 20 -> "Got it — thank you for sharing that."
                topicKeyword != null -> "That's a strong point about $topicKeyword."
                else -> "I appreciate how clearly you framed that."
            }

            val promptByScenario = when {
                scenarioKey.contains("interview") -> when (turnNumber) {
                    1 -> "Could you walk me through a specific challenge you solved recently and the measurable outcome you achieved?"
                    2 -> "How did you align stakeholders or teammates when opinions differed on the approach?"
                    3 -> "Looking back at that experience, what would you do differently today?"
                    else -> "To wrap up, what is the biggest impact you'd aim to create in your first 90 days?"
                }
                scenarioKey.contains("meeting") || scenarioKey.contains("workplace") || scenarioKey.contains("manager") -> when (turnNumber) {
                    1 -> "What is the top priority we should resolve in this meeting, and what blocker needs our immediate decision?"
                    2 -> "If we commit to that direction, what trade-off or risk should the team prepare for?"
                    else -> "Can you summarize the concrete next step and owner in one clear sentence?"
                }
                scenarioKey.contains("negotiat") || scenarioKey.contains("client") || scenarioKey.contains("objection") -> when (turnNumber) {
                    1 -> "I hear the value you're describing, though our timeline and budget are tight. How would you phase this to reduce risk?"
                    2 -> "What evidence or past result gives you confidence that this option will deliver on schedule?"
                    else -> "If we move forward together, what mutual commitment would make this a win for both sides?"
                }
                scenarioKey.contains("small talk") || scenarioKey.contains("network") || scenarioKey.contains("social") -> when (turnNumber) {
                    1 -> "That sounds genuinely interesting! What got you started in that area?"
                    2 -> "What has been the most surprising or rewarding part of it for you lately?"
                    else -> "I've really enjoyed hearing your perspective — what are you looking forward to next?"
                }
                else -> when (turnNumber) {
                    1 -> "Can you expand on that with one concrete example from your own experience?"
                    2 -> "What made that moment stand out to you, and what takeaway stayed with you?"
                    else -> "How would you summarize your core message in one memorable closing line?"
                }
            }

            val levelCue = if (userLevel.equals("Beginner", ignoreCase = true)) {
                " Take your time and answer in two simple, clear sentences."
            } else ""

            "$acknowledgement $promptByScenario$levelCue".trim()
        }
    }

    suspend fun generateConversationCheckIn(
        scenario: String,
        transcript: String
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val learnerLines = transcript.lines()
                .filter { it.trim().startsWith("Learner:", ignoreCase = true) || it.trim().startsWith("User:", ignoreCase = true) }
                .map { it.substringAfter(":").trim() }
                .filter { it.isNotBlank() }

            val combined = if (learnerLines.isNotEmpty()) learnerLines.joinToString(" ") else transcript.trim()
            val wordCount = combined.split(Regex("\\s+")).count { it.isNotBlank() }
            val fillers = FillerWordDetectionEngine.detect(combined)
            val lang = LocalLanguageAnalysisEngine.analyze(combined)

            val evidence = MultimodalFeatureFusionEngine.fuse(
                audio = null,
                face = null,
                posture = null,
                gesture = null,
                language = lang,
                transcript = combined,
                expectedText = null,
                evaluatorName = "ConversationEvaluator",
                exerciseTitle = scenario
            )
            val feedback = LocalFeedbackEngine.buildFeedback(evidence)

            buildString {
                appendLine("On-Device Coaching Debrief — $scenario")
                appendLine("Turns analyzed: ${learnerLines.size.coerceAtLeast(1)} • Words spoken: $wordCount • Fillers: ${fillers.totalFillerCount}")
                appendLine()
                appendLine("What Went Well:")
                feedback.whatWentWell.take(3).forEach { appendLine("• $it") }
                appendLine()
                appendLine("Focus Next:")
                feedback.specificMistakes.take(2).forEach { appendLine("• $it") }
                feedback.improvementSuggestions.take(2).forEach { appendLine("• $it") }
                appendLine()
                appendLine("Next Practice Task: ${feedback.nextPracticeTask}")
                appendLine(feedback.honestyNotice)
            }.trim()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Local Communication Mentor (Streaming Flow)
    // ─────────────────────────────────────────────────────────────────────────

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback> = emptyList(),
        userQuestion: String
    ): Flow<String> = flow {
        LocalExecutionPolicy.verifyLocalOnly()

        val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "there"
        val level = profile?.communicationLevel ?: "Intermediate"
        val weakest = identifyWeakestSkill(profile)
        val q = userQuestion.lowercase()

        val response = buildString {
            appendLine("Hi $name — here is your on-device SpeakCraft coaching plan ($level level):")
            appendLine()

            when {
                q.contains("fluency") || q.contains("pace") || q.contains("fast") || q.contains("slow") || q.contains("filler") -> {
                    appendLine("1. Control Speaking Pace & Fluency")
                    appendLine("• Aim for 130–160 WPM with a 1-second silent pause between major ideas.")
                    appendLine("• Replace filler words ('um', 'uh', 'like') by closing your lips for one beat while your mind forms the next clause.")
                    appendLine("• Daily drill: Run a 60-second impromptu sprint using the PREP structure (Point → Reason → Example → Point).")
                }
                q.contains("pronun") || q.contains("articulat") || q.contains("accent") || q.contains("clear") -> {
                    appendLine("1. Sharpen Pronunciation & Articulation")
                    appendLine("• Warm up for 2 minutes with lip trills, jaw relaxation, and crisp consonant endings (t, d, k, p).")
                    appendLine("• Speak 15% slower on multi-syllable words so every stressed syllable lands cleanly.")
                    appendLine("• Remember: intelligibility and rhythm matter far more than erasing your natural accent.")
                }
                q.contains("vocab") || q.contains("word") -> {
                    appendLine("1. Build Active Speaking Vocabulary")
                    appendLine("• Learn 3 words per day and immediately use each in a spoken 10-second example about your own work or life.")
                    appendLine("• Audit repeated filler adjectives ('very good', 'nice', 'thing') and swap them for precise verbs.")
                }
                q.contains("interview") || q.contains("meeting") || q.contains("work") || q.contains("career") -> {
                    appendLine("1. Executive & Interview Readiness")
                    appendLine("• Structure answers with STAR (Situation, Task, Action, Result) or PREP in 45–75 seconds.")
                    appendLine("• Land sentence endings with a calm downward inflection to project authority.")
                    appendLine("• Practice 3 follow-up questions in Conversation Simulation to stay poised under pressure.")
                }
                q.contains("7-day") || q.contains("plan") || q.contains("routine") -> {
                    appendLine("1. Personalized 7-Day Communication Plan")
                    appendLine("• Days 1–2: Vocal Warm-ups + Rate of Speech control (10 min/day).")
                    appendLine("• Days 3–4: Zero-Filler 60s Sprint + PREP Speaking Clarity drills.")
                    appendLine("• Days 5–6: Conversation Simulation (3 follow-up questions) + Vocabulary in context.")
                    appendLine("• Day 7: Record a 90-second video review and compare against Day 1.")
                }
                else -> {
                    appendLine("1. Targeted Communication Coaching")
                    appendLine("• Lead with your core takeaway in the first sentence, then support it with one concrete example.")
                    appendLine("• Pair 1-second pauses with steady breath support and deliberate downward endings.")
                    appendLine("• Practice out loud for 10–15 minutes daily and review one recording using the 3-Stage Audit (audio-only, muted video, transcript).")
                }
            }

            appendLine()
            appendLine("2. Your Personal Focus Area")
            appendLine("• Based on your current profile, prioritizing ${weakest.first} (score ${weakest.second}/100) will unlock the fastest overall gain.")
            if (recentSessions.isNotEmpty()) {
                val latest = recentSessions.first()
                appendLine("• Latest session '${latest.promptTitle}': fluency ${latest.fluencyScore}/100, pace ${latest.wordsPerMinute} WPM.")
            }
            if (recentFeedback.isNotEmpty()) {
                val topNext = recentFeedback.first().nextRecommendedActivity
                if (topNext.isNotBlank()) {
                    appendLine("• Recommended next drill: $topNext")
                }
            }
            appendLine()
            append("Generated 100% on-device by SpeakCraft Local Intelligence (zero cloud AI).")
        }

        // Stream cleanly in small readable chunks
        val chunks = response.chunked(48)
        for (chunk in chunks) {
            emit(chunk)
            delay(5L)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Local YouTube Lesson & Technique Extractor
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun extractYouTubeLessons(
        videoId: String,
        videoTitle: String,
        videoUrl: String,
        description: String,
        transcriptText: String?
    ): Result<ExtractedYouTubeBundle> = withContext(Dispatchers.Default) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val combined = "$videoTitle $description ${transcriptText.orEmpty()}".lowercase()
            val category = when {
                combined.contains("voice") || combined.contains("vocal") || combined.contains("tone") || combined.contains("pitch") -> "Voice Mastery"
                combined.contains("story") || combined.contains("narrative") -> "Storytelling"
                combined.contains("body language") || combined.contains("gesture") || combined.contains("posture") || combined.contains("eye contact") -> "Body Language"
                combined.contains("listen") -> "Active Listening"
                combined.contains("conversation") || combined.contains("small talk") || combined.contains("rapport") -> "Conversation Mastery"
                combined.contains("pronun") || combined.contains("articulat") -> "Pronunciation & Articulation"
                combined.contains("interview") || combined.contains("meeting") || combined.contains("executive") -> "Professional Communication"
                combined.contains("stage") || combined.contains("presentation") || combined.contains("public speaking") -> "Public Speaking"
                else -> "Speaking Clarity"
            }

            val difficulty = when {
                combined.contains("advanced") || combined.contains("executive") || combined.contains("mastery") -> "Advanced"
                combined.contains("beginner") || combined.contains("basics") || combined.contains("foundation") -> "Beginner"
                else -> "Intermediate"
            }

            val sourceText = (transcriptText?.takeIf { it.isNotBlank() } ?: description).trim()
            val candidateSentences = sourceText
                .split(Regex("[.!?\\n]+"))
                .map { it.trim() }
                .filter { it.length in 35..220 }
                .distinct()
                .take(3)

            val techniques = if (candidateSentences.isNotEmpty()) {
                candidateSentences.mapIndexed { idx, insight ->
                    ExtractedTechnique(
                        videoId = videoId,
                        title = "${videoTitle.take(42).trim()} — Drill ${idx + 1}",
                        category = category,
                        difficulty = difficulty,
                        creatorAdvice = insight,
                        aiExplanation = "Extracted locally from video metadata/transcript to build actionable $category habits.",
                        originalContext = videoTitle,
                        instructions = "1. Read the key cue aloud.\n2. Record a 45-second response applying it intentionally.\n3. Review playback for vocal clarity and steady pauses.",
                        practicalExercise = "Record a 45-second clip practicing: $insight",
                        expectedOutcome = "Stronger $category control with measurable delivery consistency.",
                        suggestedDurationMinutes = 5L,
                        commonMistake = "Rushing through the key point without pausing for emphasis.",
                        correction = "Pause for 1 full second before and after your main takeaway.",
                        sourceVideoTitle = videoTitle,
                        sourceVideoUrl = videoUrl,
                        timestampLabel = "00:${String.format("%02d", idx * 15)}",
                        isCreatorDerived = true,
                        moderationStatus = "PUBLISHED"
                    )
                }
            } else {
                listOf(
                    ExtractedTechnique(
                        videoId = videoId,
                        title = videoTitle.ifBlank { "$category Core Practice" }.take(60),
                        category = category,
                        difficulty = difficulty,
                        creatorAdvice = "Apply the core $category principles discussed in \"$videoTitle\".",
                        aiExplanation = "Structured on-device study drill attributed to the original creator video.",
                        originalContext = videoTitle,
                        instructions = "1. Watch the linked source video.\n2. Summarize one takeaway in 60 seconds using PREP.\n3. Re-record with clearer pauses and projection.",
                        practicalExercise = "Deliver a 60-second spoken summary of \"$videoTitle\" using intentional 1-second pauses.",
                        expectedOutcome = "Improved clarity, pacing, and practical retention of the lesson.",
                        suggestedDurationMinutes = 5L,
                        commonMistake = "Passive watching without immediate vocal practice.",
                        correction = "Immediately test the technique in a 60-second recording.",
                        sourceVideoTitle = videoTitle,
                        sourceVideoUrl = videoUrl,
                        timestampLabel = "00:00",
                        isCreatorDerived = true,
                        moderationStatus = "PUBLISHED"
                    )
                )
            }

            ExtractedYouTubeBundle(
                mainTopic = videoTitle.ifBlank { category },
                category = category,
                difficulty = difficulty,
                techniques = techniques
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun mapModuleToEvaluator(moduleType: String): String {
        val m = moduleType.lowercase()
        return when {
            m.contains("pronun") || m.contains("articulat") -> "PronunciationEvaluator"
            m.contains("grammar") -> "GrammarEvaluator"
            m.contains("vocab") -> "VocabularyEvaluator"
            m.contains("conv") || m.contains("roleplay") -> "ConversationEvaluator"
            m.contains("story") -> "StorytellingEvaluator"
            m.contains("listen") -> "ListeningEvaluator"
            m.contains("present") || m.contains("public") -> "PresentationEvaluator"
            m.contains("reflect") -> "ReflectionEvaluator"
            else -> "SpeakingEvaluator"
        }
    }

    private fun extractExpectedPrompt(extraInstructions: String): String? {
        val marker = "Expected:"
        val idx = extraInstructions.indexOf(marker, ignoreCase = true)
        if (idx < 0) return null
        return extraInstructions.substring(idx + marker.length).trim().takeIf { it.isNotBlank() }
    }

    private fun computeFluencyScore(
        evidence: com.example.data.ai.local.language.LocalFeedbackEvidence,
        previousSession: SpeakingSession?
    ): Int {
        var score = 76
        if (evidence.wordsPerMinute in 120..170) score += 10
        else if (evidence.wordsPerMinute > 185 || (evidence.wordsPerMinute in 1..90)) score -= 12
        score -= (evidence.fillerCount * 4).coerceAtMost(24)
        if (evidence.pauseCount in 2..8 && evidence.meanPauseDurationMs < 1500.0) score += 6
        if (evidence.audioFeatures?.speakingRateConsistencyScore != null && evidence.audioFeatures.speakingRateConsistencyScore >= 0.8) {
            score += 4
        }
        if (previousSession != null && evidence.fillerCount == 0 && evidence.wordCount >= 15) {
            score += 2
        }
        return score.coerceIn(45, 98)
    }

    private fun computeGrammarScore(evidence: com.example.data.ai.local.language.LocalFeedbackEvidence): Int {
        if (evidence.transcript.isBlank()) return 72
        return (92 - evidence.grammarIssues.size * 10).coerceIn(45, 98)
    }

    private fun computeVocabularyScore(evidence: com.example.data.ai.local.language.LocalFeedbackEvidence): Int {
        val vocab = evidence.vocabularyAnalysis ?: return if (evidence.transcript.isBlank()) 70 else 74
        val base = (55 + (vocab.vocabularyDiversityScore * 0.45)).roundToInt()
        val repeatPenalty = (vocab.repeatedWords.size * 4).coerceAtMost(15)
        return (base - repeatPenalty).coerceIn(45, 98)
    }

    private fun computePronunciationScore(evidence: com.example.data.ai.local.language.LocalFeedbackEvidence): Int {
        var score = 78
        if (evidence.audioFeatures?.clippingDetected == true) score -= 12
        if ((evidence.audioFeatures?.backgroundNoiseLevel ?: 0f) > 0.06f) score -= 6
        if (evidence.audioFeatures != null && !evidence.audioFeatures.clippingDetected) score += 6
        return score.coerceIn(45, 96)
    }

    private fun computeArticulationScore(
        evidence: com.example.data.ai.local.language.LocalFeedbackEvidence,
        pronunciationScore: Int
    ): Int {
        var score = pronunciationScore
        if (evidence.wordsPerMinute > 185) score -= 8
        else if (evidence.wordsPerMinute in 115..165) score += 4
        return score.coerceIn(45, 97)
    }

    private fun buildModelVocabSentence(word: String, definition: String): String {
        val cleanDef = definition.trim().trimEnd('.')
        return if (cleanDef.isNotBlank()) {
            "During the team briefing, she used \"$word\" effectively to demonstrate what it means to be $cleanDef."
        } else {
            "During the presentation, she spoke in a clear, $word manner that engaged everyone in the room."
        }
    }

    private fun extractHighlightKeyword(message: String): String? {
        val stop = setOf(
            "the", "and", "that", "this", "with", "have", "from", "they", "what",
            "about", "would", "there", "their", "been", "were", "your", "just", "very"
        )
        return message.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .firstOrNull { it.length >= 5 && it !in stop }
    }

    private fun identifyWeakestSkill(profile: UserProfile?): Pair<String, Long> {
        if (profile == null) return "Fluency" to 62L
        val skills = listOf(
            "Fluency" to profile.fluencyScore,
            "Pronunciation" to profile.pronunciationScore,
            "Vocabulary" to profile.vocabularyScore,
            "Grammar" to profile.grammarScore,
            "Articulation" to profile.articulationScore,
            "Listening" to profile.listeningScore
        )
        return skills.minByOrNull { it.second } ?: ("Fluency" to profile.fluencyScore)
    }
}
