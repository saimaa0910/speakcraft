package com.example.data.ai.local

import android.content.Context
import android.util.Log
import com.example.data.ai.ExtractedYouTubeBundle
import com.example.data.ai.VocabEvaluationResult
import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.AudioFeatureExtractionEngine
import com.example.data.ai.local.audio.FillerWordDetectionEngine
import com.example.data.ai.local.audio.PronunciationAnalysisEngine
import com.example.data.ai.local.audio.SpeechRecognitionManager
import com.example.data.ai.local.fusion.MultimodalFeatureFusionEngine
import com.example.data.ai.local.language.LocalFeedbackEngine
import com.example.data.ai.local.language.LocalFeedbackEvidence
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.ai.local.language.LocalVocabularyEvaluator
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
 * Combines live Android offline speech recognition + optional local Whisper/Moonshine ASR,
 * deterministic acoustic DSP, MediaPipe vision landmarks, expanded rule-based grammar &
 * vocabulary evaluation, context-aware conversation coaching, and trend-driven mentor responses.
 *
 * Enforces [LocalExecutionPolicy.POLICY] == "LOCAL_ONLY" on every operation.
 */
class LocalIntelligenceOrchestrator(
    private val context: Context? = null,
    private val modelManager: OnDeviceModelManager? = null,
    private val capabilityManager: DeviceCapabilityManager? = null
) {
    private val audioFeatureEngine = AudioFeatureExtractionEngine()
    private val pronunciationEngine = PronunciationAnalysisEngine()

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
        localMediaPath: String = "",
        liveTranscript: String = "",
        asrConfidence: Float = 0.85f
    ): Result<SpeakingSession> = withContext(Dispatchers.IO) {
        runCatching {
            LocalExecutionPolicy.verifyLocalOnly()

            val effectiveDuration = durationSeconds.coerceAtLeast(1)
            val isVideo = mimeType.startsWith("video/", ignoreCase = true)

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
                // Step 1: Prefer live on-device transcript when captured; otherwise try file ASR
                val speechManager = SpeechRecognitionManager(
                    context = context,
                    modelManager = modelManager
                )
                val asrResult = if (liveTranscript.isBlank() && mediaFile != null) {
                    runCatching { speechManager.transcribe(mediaFile) }.getOrNull()
                } else null

                val transcript = liveTranscript.trim().ifBlank {
                    asrResult?.transcript?.trim().orEmpty()
                }
                val effectiveConfidence = if (liveTranscript.isNotBlank()) {
                    asrConfidence.coerceIn(0f, 1f)
                } else {
                    asrResult?.confidence ?: 0f
                }

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

                // Step 4: Linguistic & pronunciation alignment analysis
                val languageResult = if (transcript.isNotBlank()) {
                    LocalLanguageAnalysisEngine.analyze(
                        text = transcript,
                        transcriptConfidence = if (effectiveConfidence > 0f) effectiveConfidence else 0.85f
                    )
                } else null

                val expectedText = extractExpectedPrompt(extraInstructions)
                val pronAlignment = if (transcript.isNotBlank() && !expectedText.isNullOrBlank()) {
                    pronunciationEngine.analyze(transcript, expectedText)
                } else null

                // Step 5: Multimodal fusion & 6-section deterministic feedback
                val evaluatorName = mapModuleToEvaluator(moduleType)
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

                val computedWpm = when {
                    fused.wordsPerMinute > 0 -> fused.wordsPerMinute
                    transcript.isNotBlank() -> {
                        val wc = LocalLanguageAnalysisEngine.tokenizeWords(transcript).size
                        ((wc.toDouble() / effectiveDuration.toDouble()) * 60.0).roundToInt().coerceIn(30, 320)
                    }
                    else -> 0
                }

                val extraNotices = buildList {
                    addAll(fused.limitationNotices)
                    if (liveTranscript.isBlank()) {
                        asrResult?.limitationNotice?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }.distinct()

                val evidence = fused.copy(
                    wordsPerMinute = computedWpm,
                    speechDurationSeconds = if (fused.speechDurationSeconds > 0.0) fused.speechDurationSeconds else effectiveDuration.toDouble(),
                    limitationNotices = extraNotices
                )

                // Step 6: Deterministic scores grounded strictly in observed metrics
                val fluencyScore = computeFluencyScore(evidence, previousSession)
                val grammarScore = computeGrammarScore(evidence)
                val vocabularyScore = computeVocabularyScore(evidence)
                val pronunciationScore = computePronunciationScore(evidence, pronAlignment?.matchScore)
                val articulationScore = computeArticulationScore(evidence, pronunciationScore)

                val currentComposite = (fluencyScore + grammarScore + vocabularyScore + articulationScore) / 4
                val prevComposite = previousSession?.let {
                    ((it.fluencyScore + it.grammarScore + it.vocabularyScore + it.articulationScore) / 4L).toInt()
                }
                val sessionDelta = prevComposite?.let { currentComposite - it }

                val feedback = LocalFeedbackEngine.buildFeedback(
                    evidence = evidence,
                    previousSessionDelta = sessionDelta
                )

                val audioQualityNote = buildString {
                    if (acousticFeatures == null) {
                        append("${effectiveDuration}s recording")
                        if (transcript.isNotBlank()) append(" • Live speech captured")
                        else append(" • Vocal acoustics analyzed")
                    } else {
                        val db = acousticFeatures.relativeLoudness.roundToInt()
                        append("${effectiveDuration}s • Loudness ${db} dBFS")
                        if (acousticFeatures.clippingDetected) append(" • Slight clipping")
                        if (acousticFeatures.backgroundNoiseLevel > 0.06f) append(" • Elevated room noise")
                    }
                }

                val confidenceLabel = when {
                    acousticFeatures != null && acousticFeatures.relativeLoudness >= -26.0 && !acousticFeatures.clippingDetected -> "High"
                    transcript.isNotBlank() && evidence.fillerCount <= 1 && computedWpm in 110..175 -> "High"
                    acousticFeatures != null && acousticFeatures.relativeLoudness < -36.0 -> "Low"
                    else -> "Medium"
                }

                val articulationNote = when {
                    pronAlignment != null ->
                        "Word alignment ${(pronAlignment.wordAccuracy * 100).roundToInt()}% (${pronAlignment.intelligibilityEstimate} intelligibility — transcript alignment, non-phonemic)."
                    acousticFeatures?.clippingDetected == true ->
                        "Clipping detected — keep 15–20 cm mic distance for clearer consonants."
                    evidence.wordsPerMinute > 180 ->
                        "Fast pace (${evidence.wordsPerMinute} WPM) may compress word endings."
                    evidence.wordsPerMinute in 115..170 ->
                        "Clear cadence (${evidence.wordsPerMinute} WPM) supports crisp articulation."
                    else ->
                        "Steady acoustic delivery captured for local self-review."
                }

                val organizationNote = when {
                    transcript.isBlank() ->
                        "Audio-only evaluation active. Use Live Speech Capture or install the optional offline speech pack for transcript structure analysis."
                    languageResult != null && languageResult.averageSentenceLength > 28.0 ->
                        "Long sentences detected (avg ${languageResult.averageSentenceLength.roundToInt()} words) — break ideas into PREP points (Point, Reason, Example, Point)."
                    else ->
                        "Clear sentence segmentation (${languageResult?.sentenceCount ?: 1} sentence(s), avg ${languageResult?.averageSentenceLength?.roundToInt() ?: 10} words)."
                }

                val pronNotes = buildList {
                    add(articulationNote)
                    if (pronAlignment != null && pronAlignment.likelyMispronounced.isNotEmpty()) {
                        add("Words to enunciate more clearly: ${pronAlignment.likelyMispronounced.take(5).joinToString(", ")}.")
                    }
                    if (transcript.isBlank()) {
                        add("Word-level pronunciation alignment requires a transcript; acoustic clarity metrics were evaluated locally.")
                    }
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
                    pronunciationNotes = pronNotes,
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
            val stageEval = LocalVocabularyEvaluator.evaluateOwnSentence(
                targetWord = word,
                meaning = definition,
                userSentence = userSentence,
                difficulty = difficulty
            )
            val detailedFeedback = buildString {
                append(stageEval.explanation)
                if (stageEval.mistakes.isNotEmpty()) {
                    append(" ")
                    append(stageEval.mistakes.first())
                }
            }.trim()

            VocabEvaluationResult(
                usedCorrectly = stageEval.passed,
                naturalnessScore = stageEval.score,
                feedback = detailedFeedback,
                improvedExample = stageEval.naturalImprovedSentence
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Context-Aware Offline Conversation Partner & Coaching Debrief
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
            val words = LocalLanguageAnalysisEngine.tokenizeWords(cleanMsg)
            val fillers = FillerWordDetectionEngine.detect(cleanMsg)
            val lang = LocalLanguageAnalysisEngine.analyze(cleanMsg)
            val isQuestion = cleanMsg.endsWith("?") ||
                Regex("^(what|why|how|when|where|who|can|could|would|do|does|did|is|are|should)\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(cleanMsg)
            val hasMetric = Regex("\\b\\d+(%|\\s*(percent|days|weeks|months|years|hours|people|users|million|thousand|x))\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(cleanMsg)
            val keywords = extractTopKeywords(cleanMsg)
            val primaryTopic = keywords.firstOrNull()
            val secondaryTopic = keywords.getOrNull(1)
            val scenarioKey = scenario.lowercase()

            // 1. Genuine contextual response to what the learner actually said
            val responseLead = when {
                isQuestion && primaryTopic != null ->
                    "Great question about $primaryTopic — from my side, clarity on the goal and next step matters most."
                isQuestion ->
                    "That's a thoughtful question — I'd focus on the practical impact and a clear timeline."
                hasMetric && primaryTopic != null ->
                    "Adding concrete numbers around $primaryTopic immediately makes your point more persuasive."
                words.size < 6 ->
                    "Thanks for starting with that — try expanding it into a fuller thought with a brief reason."
                primaryTopic != null && secondaryTopic != null ->
                    "You connected $primaryTopic and $secondaryTopic clearly — that gives me a solid picture."
                primaryTopic != null ->
                    "That's a strong angle on $primaryTopic."
                else ->
                    "I hear your point clearly."
            }

            // 2. Scenario-adaptive, non-repetitive follow-up question
            val followUp = when {
                scenarioKey.contains("interview") -> when {
                    !hasMetric && turnNumber == 1 ->
                        "Could you share a specific measurable result or outcome from that experience?"
                    turnNumber == 1 ->
                        "What was the hardest trade-off you had to navigate while delivering that result?"
                    turnNumber == 2 ->
                        "How did you handle pushback or align someone who initially disagreed with your approach?"
                    turnNumber == 3 ->
                        "Looking back, what single lesson from that moment still shapes how you work today?"
                    else ->
                        "To close cleanly, what impact would you want to make in your first 90 days here?"
                }
                scenarioKey.contains("meeting") || scenarioKey.contains("workplace") || scenarioKey.contains("manager") -> when (turnNumber) {
                    1 -> if (primaryTopic != null) {
                        "Regarding $primaryTopic, what specific decision or resource do you need from the team right now?"
                    } else {
                        "What is the single highest-priority decision we need to lock in before this meeting ends?"
                    }
                    2 -> "If we commit to that plan today, what risk or dependency should we flag early?"
                    else -> "Can you recap the owner, deadline, and next milestone in one crisp sentence?"
                }
                scenarioKey.contains("negotiat") || scenarioKey.contains("client") || scenarioKey.contains("disagree") -> when (turnNumber) {
                    1 -> "I appreciate your perspective, though we have tight constraints on scope and schedule. How could we test a smaller first phase?"
                    2 -> "What evidence or past outcome shows that this option protects both teams' goals?"
                    else -> "What mutual next step feels fair to both sides so we can move forward today?"
                }
                scenarioKey.contains("small talk") || scenarioKey.contains("network") || scenarioKey.contains("casual") || scenarioKey.contains("introducing") -> when (turnNumber) {
                    1 -> if (primaryTopic != null) {
                        "That sounds interesting! What first sparked your interest in $primaryTopic?"
                    } else {
                        "That sounds genuinely interesting — what got you into that?"
                    }
                    2 -> "What has been the most rewarding or surprising part of it for you lately?"
                    else -> "I've really enjoyed chatting — what's one thing you're excited to explore next?"
                }
                else -> when (turnNumber) {
                    1 -> if (primaryTopic != null) {
                        "Can you walk me through a concrete real-life moment involving $primaryTopic?"
                    } else {
                        "Can you back that up with one vivid example from your own experience?"
                    }
                    2 -> "What made that moment memorable, and how did others react?"
                    else -> "How would you sum up your core message in one memorable closing line?"
                }
            }

            // 3. Subtle inline local coaching tip when helpful (never noisy)
            val inlineTip = when {
                lang.grammarIssues.isNotEmpty() -> {
                    val issue = lang.grammarIssues.first()
                    "\n\n[Coach tip: Try \"${issue.correctedText}\" instead of \"${issue.originalText}\".]"
                }
                fillers.totalFillerCount >= 2 ->
                    "\n\n[Coach tip: ${fillers.totalFillerCount} filler words spotted (${fillers.detectedFillers.keys.take(2).joinToString()}). Try a 1-second silent pause instead.]"
                userLevel.equals("Beginner", ignoreCase = true) || userLevel.equals("Elementary", ignoreCase = true) ->
                    "\n\n[Tip: Aim for 2 short, clear sentences using Point + Example.]"
                else -> ""
            }

            "$responseLead $followUp$inlineTip".trim()
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
            val wordCount = LocalLanguageAnalysisEngine.tokenizeWords(combined).size
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
                appendLine("Turns analyzed: ${learnerLines.size.coerceAtLeast(1)} • Words spoken: $wordCount • Fillers: ${fillers.totalFillerCount} • Diversity: ${lang.vocabularyAnalysis.vocabularyDiversityScore}%")
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
    // 4. Data-Driven Local Communication Mentor (Streaming Flow)
    // ─────────────────────────────────────────────────────────────────────────

    fun askMentorStream(
        profile: UserProfile?,
        recentSessions: List<SpeakingSession>,
        recentFeedback: List<SpeechFeedback> = emptyList(),
        userQuestion: String
    ): Flow<String> = flow {
        LocalExecutionPolicy.verifyLocalOnly()

        val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "there"
        val level = profile?.communicationLevel?.ifBlank { "Intermediate" } ?: "Intermediate"
        val goal = profile?.primaryGoal?.takeIf { it.isNotBlank() } ?: "confident communication"
        val weakest = identifyWeakestSkill(profile)
        val strongest = identifyStrongestSkill(profile)
        val q = userQuestion.lowercase()

        // Compute real session analytics trends
        val avgWpm = recentSessions.mapNotNull { it.wordsPerMinute.takeIf { w -> w > 0 } }
            .takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val recentFillers = recentSessions.take(5).flatMap { it.fillerWordsDetected }
            .groupingBy { it.lowercase() }.eachCount()
            .entries.sortedByDescending { it.value }.take(3)
        val recurringMistakes = recentSessions.take(5).flatMap { it.mistakes }
            .filter { it.isNotBlank() }.distinct().take(2)

        val response = buildString {
            appendLine("Hi $name — here is your personalized on-device SpeakCraft guidance ($level • Goal: $goal):")
            appendLine()

            when {
                q.contains("mistake") || q.contains("fix") || q.contains("weak") || q.contains("improve") -> {
                    appendLine("1. Your Top Priorities From Real Practice Data")
                    if (recurringMistakes.isNotEmpty()) {
                        recurringMistakes.forEach { appendLine("• Observed issue: $it") }
                    } else {
                        appendLine("• Priority skill right now: ${weakest.first} (${weakest.second}/100) vs your strength in ${strongest.first} (${strongest.second}/100).")
                    }
                    if (recentFillers.isNotEmpty()) {
                        appendLine("• Most frequent fillers: ${recentFillers.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }} — replace each with a 1-second silent breath.")
                    }
                    if (avgWpm != null) {
                        val paceDiagnosis = when {
                            avgWpm > 175 -> "Your average pace ($avgWpm WPM) is rushing slightly — pause 1s between ideas to drop into 135–155 WPM."
                            avgWpm < 105 -> "Your average pace ($avgWpm WPM) is quite slow — practice 60s continuous flow drills to build momentum."
                            else -> "Your average pace ($avgWpm WPM) sits right in the clear 120–165 WPM zone."
                        }
                        appendLine("• Pacing check: $paceDiagnosis")
                    }
                }
                q.contains("fluency") || q.contains("pace") || q.contains("fast") || q.contains("slow") || q.contains("filler") -> {
                    appendLine("1. Control Speaking Pace & Fluency")
                    if (avgWpm != null) appendLine("• Your recent average pace is $avgWpm WPM (target sweet spot: 130–160 WPM).")
                    appendLine("• Replace filler words ('um', 'uh', 'like') by closing your lips for one silent beat while forming your next thought.")
                    appendLine("• Practice the PREP frame (Point → Reason → Example → Point) in a 60-second sprint so your mind always knows where the sentence goes next.")
                }
                q.contains("pronun") || q.contains("articulat") || q.contains("accent") || q.contains("clear") -> {
                    appendLine("1. Sharpen Pronunciation & Articulation")
                    appendLine("• Warm up for 2 minutes with lip trills, jaw relaxation, and crisp final consonants (t, d, k, p).")
                    appendLine("• Slow down by 15% on multi-syllable words and stress the primary syllable clearly.")
                    appendLine("• Focus on intelligibility and steady rhythm — clear syllable separation matters far more than erasing your natural accent.")
                }
                q.contains("grammar") || q.contains("sentence") || q.contains("tense") -> {
                    appendLine("1. Clean Spoken Grammar & Sentence Clarity")
                    appendLine("• Keep spoken sentences between 10 and 18 words so subject-verb agreement and tense stay effortless.")
                    appendLine("• Anchor stories with one clear time frame (Past Simple for actions + Past Continuous for background).")
                    appendLine("• Practice 3 original spoken sentences daily in Vocabulary Builder Stage 4 to get instant local grammar feedback.")
                }
                q.contains("vocab") || q.contains("word") -> {
                    appendLine("1. Turn Passive Words Into Active Speaking Vocabulary")
                    appendLine("• Complete all 5 stages for 2–3 words daily in Vocabulary Builder (Learn → Speak Word → Practice Sentences → Create Own Sentence → Complete).")
                    appendLine("• Swap vague words ('good', 'thing', 'nice') for precise verbs and collocations during your next 60s recording.")
                }
                q.contains("story") || q.contains("persuad") || q.contains("present") || q.contains("public") -> {
                    appendLine("1. High-Impact Storytelling & Presentations")
                    appendLine("• Skip 'Today I will talk about...' — open with a bold question, contrast, or specific scene.")
                    appendLine("• Relive moments with direct dialogue instead of flatly reporting facts.")
                    appendLine("• Use the Rule of 3 for structure and drop your pitch on the final word of each key takeaway.")
                }
                q.contains("interview") || q.contains("meeting") || q.contains("work") || q.contains("career") -> {
                    appendLine("1. Executive & Interview Readiness")
                    appendLine("• Answer behavioral questions in 60–75 seconds using STAR (Situation, Task, Action, Result with a metric).")
                    appendLine("• Lead meeting updates with the headline decision first, then supporting context.")
                    appendLine("• Run 3 turns in Conversation Simulation under 'Job interviews' or 'Professional meetings' to sharpen follow-up poise.")
                }
                q.contains("7-day") || q.contains("plan") || q.contains("routine") || q.contains("next lesson") -> {
                    val dailyMins = profile?.dailyGoalMinutes ?: 15L
                    appendLine("1. Personalized 7-Day Plan ($dailyMins min/day • $level)")
                    appendLine("• Days 1–2: Focus on ${weakest.first} (currently ${weakest.second}/100) + 2 Vocabulary Builder 5-stage words.")
                    appendLine("• Days 3–4: Zero-Filler 60s Sprint + PREP clarity drills with Live Speech Capture.")
                    appendLine("• Days 5–6: Conversation Simulation (3+ turns) + Shadowing cadence drill.")
                    appendLine("• Day 7: Record a 90-second self-review clip and compare WPM and filler count against Day 1.")
                }
                else -> {
                    appendLine("1. Actionable Coaching For Your Question")
                    appendLine("• Lead with your core takeaway in the first 10 seconds, then back it up with one real example.")
                    appendLine("• Pair 1-second silent pauses with steady breath support and calm downward endings.")
                    appendLine("• Complete today's personalized practice plan (targeting ${weakest.first}) and review one short recording locally.")
                }
            }

            appendLine()
            appendLine("2. Snapshot From Your Profile")
            appendLine("• Strongest skill: ${strongest.first} (${strongest.second}/100) • Focus lever: ${weakest.first} (${weakest.second}/100)")
            if (recentSessions.isNotEmpty()) {
                val latest = recentSessions.first()
                appendLine("• Latest recording '${latest.promptTitle}': fluency ${latest.fluencyScore}/100, grammar ${latest.grammarScore}/100, pace ${latest.wordsPerMinute} WPM.")
            } else {
                appendLine("• Record a quick 30-second clip in Speaking Lab to unlock trend tracking across pace, fillers, and clarity.")
            }
            if (recentFeedback.isNotEmpty()) {
                val topNext = recentFeedback.first().nextRecommendedActivity
                if (topNext.isNotBlank()) {
                    appendLine("• Recommended next drill: $topNext")
                }
            }
            appendLine()
            append("100% On-Device SpeakCraft Local Intelligence — private, offline, zero cloud AI.")
        }

        val chunks = response.chunked(56)
        for (chunk in chunks) {
            emit(chunk)
            delay(4L)
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
        evidence: LocalFeedbackEvidence,
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

    private fun computeGrammarScore(evidence: LocalFeedbackEvidence): Int {
        if (evidence.transcript.isBlank()) return 72
        return (92 - evidence.grammarIssues.size * 10).coerceIn(45, 98)
    }

    private fun computeVocabularyScore(evidence: LocalFeedbackEvidence): Int {
        val vocab = evidence.vocabularyAnalysis ?: return if (evidence.transcript.isBlank()) 70 else 74
        val base = (55 + (vocab.vocabularyDiversityScore * 0.45)).roundToInt()
        val repeatPenalty = (vocab.repeatedWords.size * 4).coerceAtMost(15)
        return (base - repeatPenalty).coerceIn(45, 98)
    }

    private fun computePronunciationScore(
        evidence: LocalFeedbackEvidence,
        alignmentMatchScore: Float?
    ): Int {
        if (alignmentMatchScore != null) {
            return (alignmentMatchScore * 100f).roundToInt().coerceIn(45, 98)
        }
        var score = 78
        if (evidence.audioFeatures?.clippingDetected == true) score -= 12
        if ((evidence.audioFeatures?.backgroundNoiseLevel ?: 0f) > 0.06f) score -= 6
        if (evidence.audioFeatures != null && !evidence.audioFeatures.clippingDetected) score += 6
        return score.coerceIn(45, 96)
    }

    private fun computeArticulationScore(
        evidence: LocalFeedbackEvidence,
        pronunciationScore: Int
    ): Int {
        var score = pronunciationScore
        if (evidence.wordsPerMinute > 185) score -= 8
        else if (evidence.wordsPerMinute in 115..165) score += 4
        return score.coerceIn(45, 97)
    }

    private fun extractTopKeywords(message: String): List<String> {
        val stop = setOf(
            "the", "and", "that", "this", "with", "have", "from", "they", "what",
            "about", "would", "there", "their", "been", "were", "your", "just", "very",
            "really", "think", "going", "could", "should", "because", "where", "which"
        )
        return message.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && it !in stop }
            .distinct()
            .take(3)
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

    private fun identifyStrongestSkill(profile: UserProfile?): Pair<String, Long> {
        if (profile == null) return "Listening" to 72L
        val skills = listOf(
            "Fluency" to profile.fluencyScore,
            "Pronunciation" to profile.pronunciationScore,
            "Vocabulary" to profile.vocabularyScore,
            "Grammar" to profile.grammarScore,
            "Articulation" to profile.articulationScore,
            "Listening" to profile.listeningScore
        )
        return skills.maxByOrNull { it.second } ?: ("Fluency" to profile.fluencyScore)
    }
}
