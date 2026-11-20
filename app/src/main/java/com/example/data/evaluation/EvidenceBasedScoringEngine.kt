package com.example.data.evaluation

import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.PronunciationAnalysisResult
import com.example.data.ai.local.language.LanguageAnalysisResult
import com.example.data.ai.local.language.LocalFeedbackEngine
import com.example.data.ai.local.language.LocalFeedbackEvidence
import com.example.data.ai.local.video.FaceAnalysisResult
import com.example.data.ai.local.video.GestureAnalysisResult
import com.example.data.ai.local.video.PostureAnalysisResult
import com.example.data.model.SpeakingSession
import kotlin.math.roundToInt

/**
 * Pure evidence-driven scoring policy.
 *
 * Guarantees:
 * - Zero baseline or fallback scores (never returns 78/100 or floor points without evidence).
 * - Missing or failed evidence sets the dimension's `score = null` and an explicit unavailable status.
 * - Does not silently redistribute missing dimension weights into unaffected dimensions;
 *   when only a subset of dimensions has valid evidence, the report is explicitly flagged
 *   as `isPartialEvaluation = true` with a transparent [CoachEvaluationReport.partialReason].
 */
object EvidenceBasedScoringEngine {

    data class ScoringInput(
        val sessionId: String,
        val moduleType: String,
        val promptTitle: String,
        val mimeType: String,
        val localMediaPath: String = "",
        val validated: ValidatedEvidenceBundle,
        val expectedText: String? = null,
        val languageAnalysis: LanguageAnalysisResult? = null,
        val languageInferenceFailed: Boolean = false,
        val pronunciationAlignment: PronunciationAnalysisResult? = null,
        val faceAnalysis: FaceAnalysisResult? = null,
        val postureAnalysis: PostureAnalysisResult? = null,
        val gestureAnalysis: GestureAnalysisResult? = null,
        val fusionEvidence: LocalFeedbackEvidence? = null,
        val previousSession: SpeakingSession? = null,
        val nowMs: Long = System.currentTimeMillis()
    )

    fun score(input: ScoringInput): CoachEvaluationReport {
        val v = input.validated.validation

        // 1. Fail-closed gate: invalid primary evidence must NEVER yield scores or COMPLETED status
        if (!v.isValidForEvaluation) {
            val state = v.terminalFailureState ?: CoachEvaluationState.INSUFFICIENT_EVIDENCE
            val unavailableReason = when (state) {
                CoachEvaluationState.ANALYSIS_FAILED -> DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED
                else -> DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE
            }
            val emptyDimensions = buildUnavailableDimensions(
                availability = unavailableReason,
                includeVideo = v.videoRequired,
                timestampMs = input.nowMs
            )
            return CoachEvaluationReport(
                sessionId = input.sessionId,
                evaluationState = state,
                overallScore = null,
                statusHeadline = v.userTitle.ifBlank { "Evaluation Not Scored" },
                statusMessage = v.userMessage.ifBlank {
                    "We couldn't detect enough speech evidence to evaluate this session."
                },
                isPartialEvaluation = false,
                partialReason = null,
                dimensions = emptyDimensions,
                honestyNotice = "No score was generated or saved because verified speech evidence was unavailable.",
                validation = v,
                evaluatedAtMs = input.nowMs
            )
        }

        val audio = input.validated.acousticFeatures
        val transcript = input.validated.validatedTranscript
        val hasAudio = v.audioValid && audio != null
        val hasTranscript = v.transcriptValid && transcript.isNotBlank()
        val lang = input.languageAnalysis
        val fused = input.fusionEvidence

        val dimensions = linkedMapOf<String, EvaluationDimensionDetail>()

        // ── Dimension 1: Fluency (requires real voiced audio OR written response when audio not required)
        dimensions["fluency"] = computeFluencyDimension(
            hasAudio = hasAudio,
            audio = audio,
            hasTranscript = hasTranscript,
            fused = fused,
            audioRequired = input.validated.validation.audioValid || !hasTranscript,
            timestampMs = input.nowMs
        )

        // ── Dimension 2: Pronunciation / Articulation Clarity
        dimensions["pronunciation"] = computePronunciationDimension(
            hasAudio = hasAudio,
            audio = audio,
            hasTranscript = hasTranscript,
            pronAlignment = input.pronunciationAlignment,
            timestampMs = input.nowMs
        )

        // ── Dimension 3: Vocal Delivery (projection, pitch range, audio quality)
        dimensions["delivery"] = computeDeliveryDimension(
            hasAudio = hasAudio,
            audio = audio,
            timestampMs = input.nowMs
        )

        // ── Dimension 4: Grammar (requires valid transcript + successful linguistic inference)
        dimensions["grammar"] = computeGrammarDimension(
            hasTranscript = hasTranscript,
            languageInferenceFailed = input.languageInferenceFailed,
            lang = lang,
            wordCount = input.validated.meaningfulWordCount,
            timestampMs = input.nowMs
        )

        // ── Dimension 5: Vocabulary (requires valid transcript + successful linguistic inference)
        dimensions["vocabulary"] = computeVocabularyDimension(
            hasTranscript = hasTranscript,
            languageInferenceFailed = input.languageInferenceFailed,
            lang = lang,
            wordCount = input.validated.meaningfulWordCount,
            timestampMs = input.nowMs
        )

        // ── Dimension 6: Video Presence (ONLY active/assessed when user selected video assessment)
        if (v.videoRequired) {
            dimensions["video_presence"] = computeVideoPresenceDimension(
                face = input.faceAnalysis,
                posture = input.postureAnalysis,
                gesture = input.gestureAnalysis,
                videoValid = v.videoValid,
                timestampMs = input.nowMs
            )
        }

        val evaluatedDetails = dimensions.values.filter {
            it.availabilityStatus == DimensionAvailability.EVALUATED && it.score != null
        }

        // If ALL dimensions failed or produced null, fail cleanly instead of returning a fake 0 or 78
        if (evaluatedDetails.isEmpty()) {
            return CoachEvaluationReport(
                sessionId = input.sessionId,
                evaluationState = CoachEvaluationState.ANALYSIS_FAILED,
                overallScore = null,
                statusHeadline = "Analysis Could Not Produce Any Dimension Scores",
                statusMessage = "No assessment dimensions completed with valid evidence. Your session has not been scored.",
                isPartialEvaluation = false,
                partialReason = null,
                dimensions = dimensions,
                honestyNotice = "All dimensions lacked verifiable evidence or failed inference.",
                validation = v,
                evaluatedAtMs = input.nowMs
            )
        }

        // Never extrapolate/fabricate missing dimension weights:
        // Only compute an equal-mean summary across *actually evaluated* dimensions, and flag as partial
        // whenever any expected dimension is unavailable.
        val unavailableLabels = dimensions.values
            .filter { it.availabilityStatus != DimensionAvailability.EVALUATED && it.availabilityStatus != DimensionAvailability.NOT_REQUIRED }
            .map { it.label }

        val isPartial = unavailableLabels.isNotEmpty()
        val overallScore = evaluatedDetails.mapNotNull { it.score }.average().roundToInt().coerceIn(0, 100)
        val partialReason = if (isPartial) {
            "Partial assessment (${evaluatedDetails.size} of ${dimensions.size} dimensions scored). " +
                "Unavailable without evidence: ${unavailableLabels.joinToString(", ")}. " +
                "Partial scores reflect only the evaluated channels and should not be compared directly to full multimodal scores."
        } else null

        val durationWholeSec = input.validated.effectiveDurationSeconds.roundToInt().coerceAtLeast(1)
        val fluencyVal = dimensions["fluency"]?.score ?: 0
        val pronVal = dimensions["pronunciation"]?.score ?: 0
        val grammarVal = dimensions["grammar"]?.score ?: 0
        val vocabVal = dimensions["vocabulary"]?.score ?: 0
        val deliveryVal = dimensions["delivery"]?.score ?: pronVal
        val articulationVal = ((pronVal + deliveryVal) / 2.0).roundToInt().coerceIn(0, 100)

        val prevComposite = input.previousSession?.let { prev ->
            val prevScores = listOfNotNull(
                prev.fluencyScore.takeIf { it > 0 }?.toInt(),
                prev.pronunciationScore.takeIf { it > 0 }?.toInt(),
                prev.grammarScore.takeIf { it > 0 }?.toInt(),
                prev.vocabularyScore.takeIf { it > 0 }?.toInt()
            )
            if (prevScores.isNotEmpty() && prevScores.size == evaluatedDetails.size) prevScores.average().roundToInt() else null
        }

        val evidenceForTemplates = fused ?: LocalFeedbackEvidence(
            transcript = transcript,
            expectedText = input.expectedText,
            wordCount = input.validated.meaningfulWordCount,
            wordsPerMinute = if (hasAudio && hasTranscript && durationWholeSec > 0) {
                ((input.validated.meaningfulWordCount * 60.0) / durationWholeSec).roundToInt()
            } else fused?.wordsPerMinute ?: 0,
            speechDurationSeconds = input.validated.effectiveDurationSeconds,
            audioFeatures = audio,
            faceAnalysis = input.faceAnalysis,
            postureAnalysis = input.postureAnalysis,
            gestureAnalysis = input.gestureAnalysis,
            exerciseTitle = input.promptTitle.ifBlank { input.moduleType },
            processingMode = if (hasTranscript) "On-Device Multimodal Engine" else "Audio-Only DSP Engine"
        )

        val feedback = LocalFeedbackEngine.buildFeedback(
            evidence = evidenceForTemplates,
            previousSessionDelta = prevComposite?.let { overallScore - it }
        )

        val audioQualitySummary = audio?.let {
            val db = it.relativeLoudness.roundToInt()
            buildString {
                append("${durationWholeSec}s verified speech • ${db} dBFS")
                if (it.clippingDetected) append(" • Clipping detected")
                if (it.backgroundNoiseLevel > 0.06f) append(" • Elevated room noise")
            }
        } ?: "Text-only evidence (${input.validated.meaningfulWordCount} words)"

        val confidenceBadge = when {
            audio != null && audio.relativeLoudness >= -26.0 && !audio.clippingDetected && !isPartial -> "High"
            evaluatedDetails.size >= 3 -> "Medium"
            else -> "Low (Partial)"
        }

        val evaluatedKeys = evaluatedDetails.map { it.key }
        val unavailableKeys = dimensions.values
            .filter { it.availabilityStatus != DimensionAvailability.EVALUATED && it.availabilityStatus != DimensionAvailability.NOT_REQUIRED }
            .map { it.key }

        val session = SpeakingSession(
            moduleType = input.moduleType,
            promptTitle = input.promptTitle.ifBlank { "Communication Practice" },
            durationSeconds = durationWholeSec.toLong(),
            localMediaPath = input.localMediaPath,
            mediaType = input.mimeType,
            transcript = transcript.ifBlank {
                "[Audio-only recording (${durationWholeSec}s) — evaluated locally via acoustic DSP without transcript]"
            },
            overallAssessment = buildString {
                append(feedback.explanation)
                if (partialReason != null) {
                    append("\n\n")
                    append(partialReason)
                }
            },
            strengths = feedback.whatWentWell,
            mistakes = feedback.specificMistakes,
            pronunciationNotes = listOfNotNull(
                dimensions["pronunciation"]?.note?.takeIf { it.isNotBlank() },
                dimensions["delivery"]?.note?.takeIf { it.isNotBlank() }
            ),
            betterAlternatives = feedback.correctedExamples,
            fillerWordsDetected = evidenceForTemplates.detectedFillers,
            repeatedExpressions = lang?.vocabularyAnalysis?.repeatedWords?.keys?.toList() ?: emptyList(),
            improvementSuggestions = feedback.improvementSuggestions,
            recommendedExercises = listOf(feedback.nextPracticeTask),
            pronunciationAssessable = dimensions["pronunciation"]?.score != null,
            articulationClarity = dimensions["delivery"]?.note ?: feedback.honestyNotice,
            thoughtOrganization = if (hasTranscript) {
                "Transcript structure verified across ${lang?.sentenceCount ?: 1} sentence(s)."
            } else {
                "Transcript unavailable — language structure was not scored."
            },
            nextRecommendedActivity = feedback.nextPracticeTask,
            confidenceLevel = confidenceBadge,
            audioQualityNote = audioQualitySummary,
            fluencyScore = fluencyVal.toLong(),
            pronunciationScore = pronVal.toLong(),
            vocabularyScore = vocabVal.toLong(),
            grammarScore = grammarVal.toLong(),
            articulationScore = articulationVal.toLong(),
            wordsPerMinute = evidenceForTemplates.wordsPerMinute.toLong(),
            evaluationStatus = CoachEvaluationState.COMPLETED.name,
            isPartialEvaluation = isPartial,
            evaluatedDimensionKeys = evaluatedKeys,
            unavailableDimensionKeys = unavailableKeys,
            evidenceSummary = buildString {
                if (hasAudio) append("Audio DSP (${String.format("%.1f", v.voicedSeconds)}s voiced)")
                if (hasTranscript) {
                    if (isNotEmpty()) append(" • ")
                    append("Transcript (${input.validated.meaningfulWordCount} words)")
                }
                if (v.videoRequired && v.videoValid) {
                    if (isNotEmpty()) append(" • ")
                    append("Video frames (${input.faceAnalysis?.framesAnalyzed ?: input.postureAnalysis?.framesAnalyzed ?: 0})")
                }
            }
        )

        return CoachEvaluationReport(
            sessionId = input.sessionId,
            evaluationState = CoachEvaluationState.COMPLETED,
            overallScore = overallScore,
            statusHeadline = if (isPartial) "Partial Evaluation Complete" else "Evaluation Complete",
            statusMessage = partialReason ?: "All active assessment dimensions were scored from verified session evidence.",
            isPartialEvaluation = isPartial,
            partialReason = partialReason,
            dimensions = dimensions,
            strengths = feedback.whatWentWell,
            mistakes = feedback.specificMistakes,
            correctedExamples = feedback.correctedExamples,
            explanation = session.overallAssessment,
            improvementSuggestions = feedback.improvementSuggestions,
            nextPracticeTask = feedback.nextPracticeTask,
            honestyNotice = feedback.honestyNotice,
            session = session,
            validation = v,
            evaluatedAtMs = input.nowMs
        )
    }

    private fun computeFluencyDimension(
        hasAudio: Boolean,
        audio: AcousticFeatures?,
        hasTranscript: Boolean,
        fused: LocalFeedbackEvidence?,
        audioRequired: Boolean,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (!hasAudio || audio == null) {
            return if (!audioRequired && hasTranscript && fused != null) {
                // Written exercise only — do not invent vocal pace score without audio
                EvaluationDimensionDetail(
                    key = "fluency",
                    label = "Fluency & Pacing",
                    score = null,
                    evidenceSource = "Audio Recording",
                    confidence = 0f,
                    availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                    evaluationTimestampMs = timestampMs,
                    note = "Spoken pacing requires an audio recording."
                )
            } else {
                EvaluationDimensionDetail(
                    key = "fluency",
                    label = "Fluency & Pacing",
                    score = null,
                    evidenceSource = "Acoustic DSP",
                    confidence = 0f,
                    availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                    evaluationTimestampMs = timestampMs,
                    note = "Unavailable without verified speech audio."
                )
            }
        }

        // Purely evidence-based calculation starting from observed speech-to-silence continuity
        val speechRatio = (audio.speechDurationSeconds / audio.durationSeconds.coerceAtLeast(0.1)).coerceIn(0.0, 1.0)
        var score = (38.0 + speechRatio * 34.0 + audio.speechContinuityScore * 20.0 + audio.speakingRateConsistencyScore * 12.0).roundToInt()

        val fillers = fused?.fillerCount ?: 0
        score -= (fillers * 4).coerceAtMost(24)

        val wpm = fused?.wordsPerMinute ?: audio.wordsPerMinute
        if (wpm > 0) {
            score += when (wpm) {
                in 120..165 -> 6
                in 100..119, in 166..185 -> 0
                else -> -8
            }
        }
        if (audio.longPauseCount > 3) {
            score -= ((audio.longPauseCount - 3) * 3).coerceAtMost(15)
        }

        val finalScore = score.coerceIn(10, 98)
        val note = buildString {
            append("${String.format("%.1f", audio.speechDurationSeconds)}s voiced of ${String.format("%.1f", audio.durationSeconds)}s")
            if (wpm > 0) append(" • $wpm WPM")
            if (fillers > 0) append(" • $fillers filler(s)")
        }

        return EvaluationDimensionDetail(
            key = "fluency",
            label = "Fluency & Pacing",
            score = finalScore,
            evidenceSource = if (hasTranscript) "Acoustic DSP + Transcript Timing" else "Acoustic DSP (VAD & Pause Cadence)",
            confidence = if (hasTranscript) 0.88f else 0.72f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = note
        )
    }

    private fun computePronunciationDimension(
        hasAudio: Boolean,
        audio: AcousticFeatures?,
        hasTranscript: Boolean,
        pronAlignment: PronunciationAnalysisResult?,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (!hasAudio || audio == null) {
            return EvaluationDimensionDetail(
                key = "pronunciation",
                label = "Pronunciation & Clarity",
                score = null,
                evidenceSource = "Acoustic Clarity & Alignment",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                evaluationTimestampMs = timestampMs,
                note = "Requires verified speech audio."
            )
        }

        if (hasTranscript && pronAlignment != null) {
            val base = (pronAlignment.matchScore * 100.0).roundToInt()
            val cleanBonus = if (!audio.clippingDetected && audio.relativeLoudness in -32.0..-8.0) 4 else 0
            val clipPenalty = if (audio.clippingDetected) 10 else 0
            val finalScore = (base + cleanBonus - clipPenalty).coerceIn(15, 99)
            return EvaluationDimensionDetail(
                key = "pronunciation",
                label = "Pronunciation & Clarity",
                score = finalScore,
                evidenceSource = "Prompt Transcript Alignment + Acoustic Signal",
                confidence = 0.82f,
                availabilityStatus = DimensionAvailability.EVALUATED,
                evaluationTimestampMs = timestampMs,
                note = "Word alignment ${(pronAlignment.wordAccuracy * 100).roundToInt()}% (${pronAlignment.intelligibilityEstimate})"
            )
        }

        // Acoustic clarity estimate when freeform audio-only (strictly requires real voiced audio; no 78 floor)
        val signalFactor = when {
            audio.relativeLoudness >= -24.0 -> 36
            audio.relativeLoudness >= -32.0 -> 28
            else -> 16
        }
        val pitchFactor = (audio.pitchVariationHz.coerceIn(0.0, 50.0) * 0.45).roundToInt()
        val continuityFactor = (audio.speechContinuityScore * 30.0).roundToInt()
        val noisePenalty = if (audio.backgroundNoiseLevel > 0.08f) 10 else 0
        val clipPenalty = if (audio.clippingDetected) 14 else 0
        val finalScore = (20 + signalFactor + pitchFactor + continuityFactor - noisePenalty - clipPenalty).coerceIn(15, 94)

        return EvaluationDimensionDetail(
            key = "pronunciation",
            label = "Pronunciation & Clarity",
            score = finalScore,
            evidenceSource = if (hasTranscript) "Acoustic Signal + Transcript" else "Acoustic Clarity DSP (No Phoneme Transcript)",
            confidence = if (hasTranscript) 0.68f else 0.52f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = if (audio.clippingDetected) {
                "Signal clipping reduced acoustic clarity estimate."
            } else {
                "Estimated from vocal signal clarity and cadence stability."
            }
        )
    }

    private fun computeDeliveryDimension(
        hasAudio: Boolean,
        audio: AcousticFeatures?,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (!hasAudio || audio == null) {
            return EvaluationDimensionDetail(
                key = "delivery",
                label = "Vocal Delivery",
                score = null,
                evidenceSource = "Acoustic DSP",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                evaluationTimestampMs = timestampMs,
                note = "Requires verified speech audio."
            )
        }

        val loudnessPoints = when {
            audio.relativeLoudness in -24.0..-8.0 -> 42
            audio.relativeLoudness in -32.0..-24.0 -> 32
            audio.relativeLoudness > -8.0 -> 28
            else -> 18
        }
        val melodyPoints = when {
            audio.pitchVariationHz >= 22.0 -> 28
            audio.pitchVariationHz >= 10.0 -> 20
            else -> 10
        }
        val noisePenalty = (audio.backgroundNoiseLevel * 80.0).roundToInt().coerceAtMost(18)
        val clipPenalty = if (audio.clippingDetected) 12 else 0
        val finalScore = (18 + loudnessPoints + melodyPoints - noisePenalty - clipPenalty).coerceIn(15, 98)

        return EvaluationDimensionDetail(
            key = "delivery",
            label = "Vocal Delivery",
            score = finalScore,
            evidenceSource = "Acoustic Loudness & Pitch DSP",
            confidence = 0.85f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = "Loudness ${audio.relativeLoudness.roundToInt()} dBFS • Pitch spread ${audio.pitchVariationHz.roundToInt()} Hz"
        )
    }

    private fun computeGrammarDimension(
        hasTranscript: Boolean,
        languageInferenceFailed: Boolean,
        lang: LanguageAnalysisResult?,
        wordCount: Int,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (languageInferenceFailed) {
            return EvaluationDimensionDetail(
                key = "grammar",
                label = "Grammar",
                score = null,
                evidenceSource = "Local Language Rule Engine",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED,
                evaluationTimestampMs = timestampMs,
                note = "Grammar analysis failed; no score assigned."
            )
        }
        if (!hasTranscript || lang == null || wordCount < EvaluationEvidenceValidator.MIN_TRANSCRIPT_MEANINGFUL_WORDS) {
            return EvaluationDimensionDetail(
                key = "grammar",
                label = "Grammar",
                score = null,
                evidenceSource = "Speech Transcript",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                evaluationTimestampMs = timestampMs,
                note = "Unavailable without a valid transcript."
            )
        }

        val issuePenalty = lang.grammarIssues.sumOf { issue ->
            if (issue.confidenceLevel >= 0.85f) 9 else 5
        }
        val substanceBonus = when {
            wordCount >= 35 -> 16
            wordCount >= 18 -> 10
            wordCount >= 8 -> 5
            else -> 0
        }
        val runOnPenalty = if (lang.averageSentenceLength > 30.0) 8 else 0
        val finalScore = (78 + substanceBonus - issuePenalty - runOnPenalty).coerceIn(15, 99)

        return EvaluationDimensionDetail(
            key = "grammar",
            label = "Grammar",
            score = finalScore,
            evidenceSource = "Transcript Linguistic Analysis ($wordCount words)",
            confidence = 0.86f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = if (lang.grammarIssues.isEmpty()) {
                "No rule-based grammar errors across ${lang.sentenceCount} sentence(s)."
            } else {
                "${lang.grammarIssues.size} grammar rule issue(s) flagged."
            }
        )
    }

    private fun computeVocabularyDimension(
        hasTranscript: Boolean,
        languageInferenceFailed: Boolean,
        lang: LanguageAnalysisResult?,
        wordCount: Int,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (languageInferenceFailed) {
            return EvaluationDimensionDetail(
                key = "vocabulary",
                label = "Vocabulary",
                score = null,
                evidenceSource = "Lexical Diversity Engine",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED,
                evaluationTimestampMs = timestampMs,
                note = "Vocabulary analysis failed; no score assigned."
            )
        }
        if (!hasTranscript || lang == null || wordCount < EvaluationEvidenceValidator.MIN_TRANSCRIPT_MEANINGFUL_WORDS) {
            return EvaluationDimensionDetail(
                key = "vocabulary",
                label = "Vocabulary",
                score = null,
                evidenceSource = "Speech Transcript",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                evaluationTimestampMs = timestampMs,
                note = "Unavailable without a valid transcript."
            )
        }

        val vocab = lang.vocabularyAnalysis
        val diversity = vocab.vocabularyDiversityScore.coerceIn(15, 96)
        val targetBonus = (vocab.targetWordsUsed.size * 5).coerceAtMost(15)
        val repetitionPenalty = (vocab.repeatedWords.size * 5).coerceAtMost(20)
        val lengthAdjust = if (wordCount < 8) -12 else if (wordCount >= 30) 6 else 0
        val finalScore = (diversity + targetBonus + lengthAdjust - repetitionPenalty).coerceIn(15, 98)

        return EvaluationDimensionDetail(
            key = "vocabulary",
            label = "Vocabulary",
            score = finalScore,
            evidenceSource = "Lexical Diversity Analysis ($wordCount words)",
            confidence = 0.84f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = "Diversity score $diversity • Repeated crutches: ${vocab.repeatedWords.size}"
        )
    }

    private fun computeVideoPresenceDimension(
        face: FaceAnalysisResult?,
        posture: PostureAnalysisResult?,
        gesture: GestureAnalysisResult?,
        videoValid: Boolean,
        timestampMs: Long
    ): EvaluationDimensionDetail {
        if (!videoValid || (face == null && posture == null)) {
            return EvaluationDimensionDetail(
                key = "video_presence",
                label = "Video Presence",
                score = null,
                evidenceSource = "MediaPipe Frame Analysis",
                confidence = 0f,
                availabilityStatus = DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE,
                evaluationTimestampMs = timestampMs,
                note = "Video assessment requested but no valid frames were analyzed."
            )
        }
        val faceRate = face?.faceDetectionRate ?: 0f
        val postureRate = if (posture != null && posture.framesAnalyzed > 0) {
            posture.bodyDetectedFrames.toFloat() / posture.framesAnalyzed.toFloat()
        } else 0f
        val tiltPenalty = if (posture?.headTiltAlert == true) 10 else 0
        val gestureBonus = if ((gesture?.handsDetectedFrames ?: 0) > 0) 6 else 0
        val finalScore = (30 + (faceRate * 35f).roundToInt() + (postureRate * 30f).roundToInt() + gestureBonus - tiltPenalty)
            .coerceIn(15, 98)

        return EvaluationDimensionDetail(
            key = "video_presence",
            label = "Video Presence",
            score = finalScore,
            evidenceSource = "On-Device Face/Posture/Gesture Frames (${face?.framesAnalyzed ?: posture?.framesAnalyzed ?: 0})",
            confidence = 0.78f,
            availabilityStatus = DimensionAvailability.EVALUATED,
            evaluationTimestampMs = timestampMs,
            note = "Framing ${(faceRate * 100).roundToInt()}% • Posture alignment ${(postureRate * 100).roundToInt()}%"
        )
    }

    private fun buildUnavailableDimensions(
        availability: DimensionAvailability,
        includeVideo: Boolean,
        timestampMs: Long
    ): Map<String, EvaluationDimensionDetail> {
        val map = linkedMapOf(
            "fluency" to EvaluationDimensionDetail("fluency", "Fluency & Pacing", null, "Acoustic DSP", 0f, availability, timestampMs),
            "pronunciation" to EvaluationDimensionDetail("pronunciation", "Pronunciation & Clarity", null, "Acoustic DSP + Alignment", 0f, availability, timestampMs),
            "delivery" to EvaluationDimensionDetail("delivery", "Vocal Delivery", null, "Acoustic DSP", 0f, availability, timestampMs),
            "grammar" to EvaluationDimensionDetail("grammar", "Grammar", null, "Speech Transcript", 0f, availability, timestampMs),
            "vocabulary" to EvaluationDimensionDetail("vocabulary", "Vocabulary", null, "Speech Transcript", 0f, availability, timestampMs)
        )
        if (includeVideo) {
            map["video_presence"] = EvaluationDimensionDetail(
                "video_presence", "Video Presence", null, "Video Frames", 0f, availability, timestampMs
            )
        }
        return map
    }
}
