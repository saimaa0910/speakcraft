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
import com.example.data.ai.local.language.DynamicLocalCoachEngine
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
            val priorPartnerTexts = historyPairs
                .filter { !it.first.equals("user", ignoreCase = true) }
                .map { it.second.lowercase() }
            val priorTopics = historyPairs
                .filter { it.first.equals("user", ignoreCase = true) }
                .flatMap { extractTopKeywords(it.second) }
                .distinct()

            val cleanMsg = userMessage.trim()
            val words = LocalLanguageAnalysisEngine.tokenizeWords(cleanMsg)
            val fillers = FillerWordDetectionEngine.detect(cleanMsg)
            val lang = LocalLanguageAnalysisEngine.analyze(cleanMsg)
            val draftInsights = DynamicLocalCoachEngine.analyzeUserDraft(cleanMsg, scenario)
            val isQuestion = cleanMsg.endsWith("?") ||
                Regex("^(what|why|how|when|where|who|can|could|would|do|does|did|is|are|should)\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(cleanMsg)
            val hasMetric = Regex("\\b\\d+(%|\\s*(percent|days|weeks|months|years|hours|people|users|million|thousand|x))\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(cleanMsg)
            val keywords = extractTopKeywords(cleanMsg)
            val novelTopic = keywords.firstOrNull { it !in priorTopics }
            val primaryTopic = novelTopic ?: keywords.firstOrNull()
            val secondaryTopic = keywords.getOrNull(1)
            val scenarioKey = scenario.lowercase()

            // 1. Genuine contextual response referencing earlier thread & current input
            val continuityBridge = if (priorTopics.isNotEmpty() && novelTopic != null && turnNumber >= 2) {
                "Building on what you shared earlier about ${priorTopics.first()}, connecting that to $novelTopic adds helpful context. "
            } else ""

            val responseLead = when {
                isQuestion && primaryTopic != null ->
                    "${continuityBridge}Great question about $primaryTopic — from my perspective, aligning on the core outcome and timeline matters most."
                isQuestion ->
                    "${continuityBridge}That's a fair question — I'd focus on the practical impact and who owns the next step."
                hasMetric && primaryTopic != null ->
                    "${continuityBridge}Anchoring $primaryTopic with concrete numbers immediately makes your point credible."
                words.size < 6 ->
                    "${continuityBridge}I hear you — could you unpack that into two clear sentences with a quick reason?"
                primaryTopic != null && secondaryTopic != null ->
                    "${continuityBridge}You linked $primaryTopic and $secondaryTopic cleanly, which clarifies your reasoning."
                primaryTopic != null ->
                    "${continuityBridge}That's a focused angle on $primaryTopic."
                else ->
                    "${continuityBridge}I see where you're heading with that."
            }

            // 2. Candidate follow-up prompts filtered so partner never asks an already-asked question
            val candidates: List<String> = when {
                scenarioKey.contains("interview") -> listOf(
                    if (!hasMetric) "What measurable result or number best captures the impact of that work?"
                    else "What was the toughest trade-off you navigated while delivering that outcome?",
                    "How did you handle pushback or align a stakeholder who saw things differently?",
                    "Looking back, what single lesson from that experience shapes your approach today?",
                    "If you stepped into this role tomorrow, what would be your top 30-day priority?"
                )
                scenarioKey.contains("meeting") || scenarioKey.contains("workplace") || scenarioKey.contains("manager") -> listOf(
                    if (primaryTopic != null) "Regarding $primaryTopic, what concrete decision or blocker should we resolve in this meeting?"
                    else "What is the single highest-priority decision we need to lock in before this meeting wraps?",
                    "What risk or dependency could slow down that plan over the next two weeks?",
                    "If resources stay tight, what scope option would you protect first?",
                    "Can you summarize the owner, milestone, and target date in one crisp sentence?"
                )
                scenarioKey.contains("negotiat") || scenarioKey.contains("client") || scenarioKey.contains("disagree") -> listOf(
                    "I appreciate your reasoning, though timeline pressure is real on our side. How could we pilot a smaller first step?",
                    "What evidence or earlier success gives you confidence that this approach holds up?",
                    "Where do you see genuine common ground we both agree on right now?",
                    "What mutual next step feels realistic so both teams leave aligned today?"
                )
                scenarioKey.contains("small talk") || scenarioKey.contains("network") || scenarioKey.contains("casual") || scenarioKey.contains("introducing") -> listOf(
                    if (primaryTopic != null) "That sounds intriguing! What first sparked your interest in $primaryTopic?"
                    else "That sounds genuinely interesting — what first got you started with it?",
                    "What has been the most rewarding or surprising part of it for you lately?",
                    "Have you picked up any favorite books, projects, or habits around that recently?",
                    "I've really enjoyed catching up — what are you most looking forward to next month?"
                )
                else -> listOf(
                    if (primaryTopic != null) "Could you share a specific real-life moment involving $primaryTopic?"
                    else "Could you ground that in one concrete example from your week?",
                    "What shifted for you when you handled it that way?",
                    "How would someone on the outside summarize your main takeaway from that moment?",
                    "In one clear closing sentence, what is the big idea you want listeners to keep?"
                )
            }

            val followUp = candidates.firstOrNull { candidate ->
                val stem = candidate.lowercase().take(28)
                priorPartnerTexts.none { prev -> prev.contains(stem) }
            } ?: candidates[(turnNumber - 1).coerceIn(0, candidates.lastIndex)]

            // 3. Targeted inline coaching tip using LocalLanguageAnalysisEngine + DynamicLocalCoachEngine
            val inlineTip = when {
                lang.grammarIssues.isNotEmpty() -> {
                    val issue = lang.grammarIssues.first()
                    "\n\n[Coach tip: Try \"${issue.correctedText}\" instead of \"${issue.originalText}\".]"
                }
                draftInsights.discourse.hedgesDetected.isNotEmpty() -> {
                    val hedge = draftInsights.discourse.hedgesDetected.first()
                    "\n\n[Coach tip: Dropping softeners like \"$hedge\" makes your point land with more conviction.]"
                }
                fillers.totalFillerCount >= 2 ->
                    "\n\n[Coach tip: ${fillers.totalFillerCount} filler expressions spotted (${fillers.detectedFillers.keys.take(2).joinToString()}). Try a 1-second silent pause instead.]"
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
        userQuestion: String,
        history: List<Pair<String, String>> = emptyList(),
        roleKey: String = "mentor"
    ): Flow<String> = flow {
        LocalExecutionPolicy.verifyLocalOnly()

        val name = profile?.displayName?.takeIf { it.isNotBlank() } ?: "there"
        val level = profile?.communicationLevel?.ifBlank { "Intermediate" } ?: "Intermediate"
        val goal = profile?.primaryGoal?.takeIf { it.isNotBlank() } ?: "confident communication"
        val weakest = identifyWeakestSkill(profile)
        val strongest = identifyStrongestSkill(profile)
        val cleanQuestion = extractCurrentTurnText(userQuestion)
        val q = cleanQuestion.lowercase()
        val isFollowUp = history.count { it.first.equals("user", ignoreCase = true) } >= 1

        // Compute real session analytics trends
        val avgWpm = recentSessions.mapNotNull { it.wordsPerMinute.takeIf { w -> w > 0 } }
            .takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val latestSession = recentSessions.firstOrNull()
        val previousSession = recentSessions.getOrNull(1)
        val latestComposite = latestSession?.let {
            ((it.fluencyScore + it.grammarScore + it.vocabularyScore + it.articulationScore) / 4.0).roundToInt()
        }
        val prevComposite = previousSession?.let {
            ((it.fluencyScore + it.grammarScore + it.vocabularyScore + it.articulationScore) / 4.0).roundToInt()
        }
        val compositeDelta = if (latestComposite != null && prevComposite != null) latestComposite - prevComposite else null

        val recentFillers = recentSessions.take(5).flatMap { it.fillerWordsDetected }
            .groupingBy { it.lowercase() }.eachCount()
            .entries.sortedByDescending { it.value }.take(3)
        val recurringMistakes = recentSessions.take(5).flatMap { it.mistakes }
            .filter { it.isNotBlank() }.distinct().take(2)
        val recentUpgrades = recentSessions.take(3).flatMap { it.betterAlternatives }
            .filter { it.isNotBlank() }.distinct().take(2)

        // Check if the learner pasted a sentence/speech draft for direct rewriting & critique
        val pastedDraft = extractDraftForCritique(cleanQuestion)
        val isQuickCoach = roleKey.equals("quick_coach", ignoreCase = true)

        val roleHeader = when (roleKey.lowercase()) {
            "analyst" -> "SpeakCraft Data Analyst ($level • Goal: $goal)"
            "interviewer" -> "Executive & Interview Coach ($level • Goal: $goal)"
            "quick_coach" -> "Quick Action Coach ($level)"
            else -> "Personal Communication Mentor ($level • Goal: $goal)"
        }

        val response = buildString {
            if (!isFollowUp) {
                appendLine("Hi $name — $roleHeader:")
            } else {
                appendLine("Following up ($roleHeader):")
            }
            appendLine()

            if (pastedDraft != null) {
                val lang = LocalLanguageAnalysisEngine.analyze(pastedDraft)
                val draftInsights = DynamicLocalCoachEngine.analyzeUserDraft(pastedDraft, "Mentor Draft Review")
                val cleanedGrammar = LocalLanguageAnalysisEngine.applyCorrections(pastedDraft, lang.grammarIssues)

                appendLine("1. Direct Line-by-Line Upgrade For Your Draft")
                appendLine("• Intent detected: ${draftInsights.discourse.intent.label.replaceFirstChar { it.uppercase() }} (${LocalLanguageAnalysisEngine.tokenizeWords(pastedDraft).size} words)")
                if (lang.grammarIssues.isNotEmpty()) {
                    lang.grammarIssues.take(3).forEach { issue ->
                        appendLine("• Grammar polish (${issue.issueType}): \"${issue.originalText}\" -> \"${issue.correctedText}\" (${issue.explanation})")
                    }
                } else {
                    appendLine("• Grammar check: Clean subject-verb control and natural tense framing.")
                }
                if (draftInsights.rewrites.isNotEmpty()) {
                    val topRewrite = draftInsights.rewrites.first()
                    appendLine("• Executive spoken rewrite: \"${topRewrite.improved}\"")
                    appendLine("• Why this works: ${topRewrite.why}")
                } else if (!cleanedGrammar.equals(pastedDraft, ignoreCase = true)) {
                    appendLine("• Polished version: \"$cleanedGrammar\"")
                }
                if (!isQuickCoach) {
                    if (draftInsights.focusAreas.isNotEmpty()) {
                        appendLine("• Next refinement: ${draftInsights.focusAreas.first()}")
                    }
                    appendLine("• Try now: ${draftInsights.nextMicroDrill}")
                }
            } else when {
                roleKey.equals("analyst", ignoreCase = true) ||
                    q.contains("mistake") || q.contains("fix") || q.contains("weak") || q.contains("improve") || q.contains("score") -> {
                    appendLine("1. Data-Driven Priorities From Your Practice History")
                    if (latestComposite != null) {
                        val trendText = when {
                            compositeDelta != null && compositeDelta > 0 -> " (+$compositeDelta pts vs prior session)"
                            compositeDelta != null && compositeDelta < 0 -> " ($compositeDelta pts vs prior session)"
                            else -> ""
                        }
                        appendLine("• Latest composite delivery score: $latestComposite/100$trendText.")
                    }
                    if (recurringMistakes.isNotEmpty()) {
                        recurringMistakes.forEach { appendLine("• Recurring observation: $it") }
                    } else {
                        appendLine("• Biggest growth lever: ${weakest.first} (${weakest.second}/100) vs top strength in ${strongest.first} (${strongest.second}/100).")
                    }
                    if (recentFillers.isNotEmpty()) {
                        appendLine("• Most frequent fillers: ${recentFillers.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }} — replace each with a 1-second silent pause.")
                    }
                    if (avgWpm != null) {
                        val paceDiagnosis = when {
                            avgWpm > 175 -> "Your average pace ($avgWpm WPM) runs fast — pause 1s between ideas to settle near 140–155 WPM."
                            avgWpm < 105 -> "Your average pace ($avgWpm WPM) is very slow — run a 60s flow sprint to raise momentum."
                            else -> "Your average pace ($avgWpm WPM) lands squarely inside the crisp 120–165 WPM target zone."
                        }
                        appendLine("• Pacing metric: $paceDiagnosis")
                    }
                    if (!isQuickCoach && recentUpgrades.isNotEmpty()) {
                        appendLine("• Saved phrasing upgrade to rehearse: ${recentUpgrades.first()}")
                    }
                }
                roleKey.equals("interviewer", ignoreCase = true) ||
                    q.contains("interview") || q.contains("meeting") || q.contains("work") || q.contains("career") -> {
                    appendLine("1. Executive & Interview Delivery Upgrade")
                    appendLine("• Open with the headline outcome in the first 10 seconds, then unpack STAR (Situation, Task, Action, measurable Result).")
                    appendLine("• Cut softeners like 'I kind of think' or 'maybe' — lead with 'I recommend...' or 'Our data showed...'")
                    if (!isQuickCoach) {
                        appendLine("• Keep answers to 60–75 seconds cleanly ending on downward vocal inflection.")
                        appendLine("• Tip: Paste your exact interview answer here anytime and I'll tighten every sentence locally.")
                    }
                }
                q.contains("fluency") || q.contains("pace") || q.contains("fast") || q.contains("slow") || q.contains("filler") -> {
                    appendLine("1. Control Speaking Pace & Flow")
                    if (avgWpm != null) appendLine("• Your recent average pace is $avgWpm WPM (sweet spot: 130–160 WPM).")
                    appendLine("• Replace filler habits ('um', 'uh', 'like') by closing your lips for one clean silent beat between thoughts.")
                    if (!isQuickCoach) {
                        appendLine("• Practice the PREP frame (Point -> Reason -> Example -> Point) in a 60-second sprint so your next sentence is always anchored.")
                    }
                }
                q.contains("pronun") || q.contains("articulat") || q.contains("accent") || q.contains("clear") -> {
                    appendLine("1. Enunciation & Vocal Projection")
                    appendLine("• Warm up for 90 seconds with lip trills, jaw drops, and crisp final consonants (t, d, k, p).")
                    appendLine("• Slow by 15% on multi-syllable keywords and land primary stress cleanly rather than rushing endings.")
                    if (!isQuickCoach) {
                        appendLine("• Clarity and steady breath support outweigh erasing your natural accent.")
                    }
                }
                q.contains("grammar") || q.contains("sentence") || q.contains("tense") -> {
                    appendLine("1. Crisp Spoken Grammar & Phrasing")
                    appendLine("• Aim for 10–18 word spoken sentences so subject-verb agreement and tense stay effortless.")
                    appendLine("• Paste any sentence you want checked (e.g., \"Check: Despite of the rain I was agree\") for an instant rewrite and rule breakdown.")
                    if (!isQuickCoach && langExampleHint(recentUpgrades).isNotBlank()) {
                        appendLine("• From your recent practice: ${langExampleHint(recentUpgrades)}")
                    }
                }
                q.contains("vocab") || q.contains("word") -> {
                    appendLine("1. Turn Passive Vocabulary Into Active Fluency")
                    appendLine("• Complete Stages 1–5 for 2–3 words daily in Vocabulary Builder (especially Stage 4 contextual sentence crafting).")
                    appendLine("• Swap generic fillers ('very good', 'do things') for concrete verbs ('compelling', 'deliver outcomes').")
                }
                q.contains("story") || q.contains("persuad") || q.contains("present") || q.contains("public") -> {
                    appendLine("1. High-Impact Storytelling & Audience Hook")
                    appendLine("• Skip 'Today I will talk about...' — open with a vivid moment, question, or stark contrast.")
                    appendLine("• Use VAKS cues (Visual, Auditory, Kinesthetic sensory detail) and pause 1 full second right before the punchline.")
                }
                q.contains("7-day") || q.contains("plan") || q.contains("routine") || q.contains("next lesson") -> {
                    val dailyMins = profile?.dailyGoalMinutes ?: 15L
                    appendLine("1. Personalized 7-Day Plan ($dailyMins min/day • $level)")
                    appendLine("• Days 1–2: Target ${weakest.first} (${weakest.second}/100) + 2 Vocabulary Builder 5-stage cards.")
                    appendLine("• Days 3–4: Zero-Filler 60s Sprint + PREP clarity drills with Live Speech Capture.")
                    if (!isQuickCoach) {
                        appendLine("• Days 5–6: Conversation Simulator (3+ turns in '${goal.take(28)}') + Shadowing cadence drill.")
                        appendLine("• Day 7: Record a 90-second self-review clip and compare WPM and filler drops against Day 1.")
                    }
                }
                else -> {
                    val keywords = extractTopKeywords(cleanQuestion)
                    val topicHint = keywords.firstOrNull()?.let { " around \"$it\"" }.orEmpty()
                    appendLine("1. Focused Coaching Guidance$topicHint")
                    appendLine("• Lead with your single clearest takeaway in the first 10 seconds, then prove it with one concrete example.")
                    appendLine("• Target your highest-ROI lever right now: ${weakest.first} (${weakest.second}/100).")
                    if (!isQuickCoach) {
                        appendLine("• Tip: Paste any sentence or talk outline here and ask me to 'Rewrite this' for direct line-by-line coaching.")
                    }
                }
            }

            if (!isQuickCoach) {
                appendLine()
                appendLine("2. Your Live SpeakCraft Baseline")
                appendLine("• Strongest skill: ${strongest.first} (${strongest.second}/100) • Next milestone: ${weakest.first} (${weakest.second}/100)")
                if (latestSession != null) {
                    appendLine("• Latest recording '${latestSession.promptTitle}': fluency ${latestSession.fluencyScore}/100, grammar ${latestSession.grammarScore}/100, pace ${latestSession.wordsPerMinute} WPM.")
                } else {
                    appendLine("• Record one 30-second clip in Speaking Lab to unlock pace, filler, and clarity trendlines.")
                }
                if (recentFeedback.isNotEmpty()) {
                    val topNext = recentFeedback.first().nextRecommendedActivity
                    if (topNext.isNotBlank()) {
                        appendLine("• Recommended next drill: $topNext")
                    }
                }
            }
            appendLine()
            append("On-Device SpeakCraft Local Intelligence • Fast, private, deterministic coaching.")
        }

        val chunks = response.chunked(56)
        for (chunk in chunks) {
            emit(chunk)
            delay(4L)
        }
    }

    private fun extractCurrentTurnText(rawPrompt: String): String {
        val marker = "Follow-up question:"
        val idx = rawPrompt.lastIndexOf(marker, ignoreCase = true)
        return if (idx >= 0) rawPrompt.substring(idx + marker.length).trim() else rawPrompt.trim()
    }

    private fun extractDraftForCritique(question: String): String? {
        val trimmed = question.trim()
        val quoted = Regex("\"([^\"]{12,400})\"").find(trimmed)?.groupValues?.getOrNull(1)
        if (!quoted.isNullOrBlank()) return quoted.trim()

        val colonIdx = trimmed.indexOf(':')
        if (colonIdx in 2..40) {
            val prefix = trimmed.substring(0, colonIdx).lowercase()
            val after = trimmed.substring(colonIdx + 1).trim()
            if (after.length >= 12 &&
                (prefix.contains("rewrite") || prefix.contains("check") || prefix.contains("improve") ||
                    prefix.contains("correct") || prefix.contains("fix") || prefix.contains("draft") || prefix.contains("polish"))
            ) {
                return after
            }
        }
        val startsWithCommand = Regex(
            "^(rewrite|check|improve|correct|polish|edit)\\s+(?:my\\s+)?(?:sentence|speech|line|answer|intro|draft)?\\s*(.+)$",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        val commandPayload = startsWithCommand?.groupValues?.getOrNull(2)?.trim()
        if (!commandPayload.isNullOrBlank() && LocalLanguageAnalysisEngine.tokenizeWords(commandPayload).size >= 5) {
            return commandPayload
        }
        val sentenceLikeWords = LocalLanguageAnalysisEngine.tokenizeWords(trimmed).size
        val isGenericQuestion = trimmed.endsWith("?") && sentenceLikeWords <= 14
        return if (sentenceLikeWords >= 16 && !isGenericQuestion) trimmed else null
    }

    private fun langExampleHint(upgrades: List<String>): String = upgrades.firstOrNull().orEmpty()

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
                .map { it.replace(Regex("^\\s*(?:\\[?\\d{1,2}:\\d{2}\\]?\\s*[-–:]?\\s*)"), "").trim() }
                .filter { it.length in 30..240 }
                .filterNot { it.startsWith("subscribe", ignoreCase = true) || it.startsWith("http", ignoreCase = true) }
                .distinct()
                .take(5)

            val techniques = if (candidateSentences.isNotEmpty()) {
                candidateSentences.mapIndexed { idx, insight ->
                    val shortCue = insight.split(Regex("[,;:]")).firstOrNull()?.take(48)?.trim().orEmpty()
                    val drillTitle = if (shortCue.length >= 16) {
                        shortCue.replaceFirstChar { it.uppercase() }
                    } else {
                        "${videoTitle.take(36).trim()} — Drill ${idx + 1}"
                    }
                    ExtractedTechnique(
                        videoId = videoId,
                        title = drillTitle,
                        category = category,
                        difficulty = difficulty,
                        creatorAdvice = insight,
                        aiExplanation = "Creator-attributed $category habit: practicing this explicitly locks in clearer vocal phrasing and listener engagement.",
                        originalContext = videoTitle,
                        instructions = "1. Read the creator insight aloud twice.\n2. Record a 45–60s spoken take putting it into your own example.\n3. Compare your second take for deliberate pauses and energy shifts.",
                        practicalExercise = "Record a 60-second response applying: \"$insight\"",
                        expectedOutcome = "Noticeably stronger $category control and steadier phrasing.",
                        suggestedDurationMinutes = 5L,
                        commonMistake = "Knowing the concept intellectually without vocalizing it cleanly under real speaking tempo.",
                        correction = "Anchor this drill with a deliberate 1-second breath pause right before your key point.",
                        sourceVideoTitle = videoTitle,
                        sourceVideoUrl = videoUrl,
                        timestampLabel = String.format("%02d:%02d", (idx * 45) / 60, (idx * 45) % 60),
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
