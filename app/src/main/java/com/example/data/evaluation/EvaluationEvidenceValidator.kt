package com.example.data.evaluation

import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.AudioFeatureExtractionEngine
import com.example.data.ai.local.audio.AudioResampler
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import java.io.File

data class EvidenceValidationInput(
    val sessionId: String,
    val audioRequired: Boolean = true,
    val audioBytes: ByteArray = ByteArray(0),
    val audioFile: File? = null,
    val mimeType: String = "audio/mp4",
    val reportedDurationSeconds: Int = 0,
    val preExtractedAudio: AcousticFeatures? = null,
    val audioAnalysisFailed: Boolean = false,
    val videoAssessmentSelected: Boolean = false,
    val videoFile: File? = null,
    val videoFramesAnalyzed: Int = 0,
    val videoAnalysisCompleted: Boolean = false,
    val videoAnalysisFailed: Boolean = false,
    val transcriptRequired: Boolean = false,
    val transcript: String = "",
    val transcriptSessionId: String? = null
)

data class ValidatedEvidenceBundle(
    val validation: EvidenceValidationResult,
    val acousticFeatures: AcousticFeatures? = null,
    val effectiveDurationSeconds: Double = 0.0,
    val validatedTranscript: String = "",
    val meaningfulWordCount: Int = 0
)

/**
 * Validates that every requested assessment modality has genuine, non-empty,
 * non-silent, session-scoped evidence before any score can be calculated.
 */
object EvaluationEvidenceValidator {

    const val MIN_SPEECH_DURATION_SECONDS = 1.5
    const val MIN_SPEECH_RMS_ENERGY = 0.008f
    const val MIN_VOICED_SPEECH_SECONDS = 0.5
    const val MIN_TRANSCRIPT_MEANINGFUL_WORDS = 2

    private val audioEngine = AudioFeatureExtractionEngine()

    fun validate(input: EvidenceValidationInput): ValidatedEvidenceBundle {
        val issues = mutableListOf<String>()
        var audioValid = false
        var measuredDuration = 0.0
        var measuredRms = 0f
        var voicedSeconds = 0.0
        var extractedAudio: AcousticFeatures? = input.preExtractedAudio
        var decodeError: String? = null

        // ─── 1. Transcript session & lexical validation ──────────────────────────
        val trimmedTranscript = input.transcript.trim()
        val isStaleTranscript = trimmedTranscript.isNotBlank() &&
            !input.transcriptSessionId.isNullOrBlank() &&
            input.transcriptSessionId != input.sessionId

        val meaningfulWords = if (trimmedTranscript.isNotBlank() && !isStaleTranscript) {
            LocalLanguageAnalysisEngine.tokenizeWords(trimmedTranscript)
                .count { token -> token.count { it.isLetter() } >= 2 || token.length >= 2 }
        } else 0

        val transcriptValid = trimmedTranscript.isNotBlank() &&
            !isStaleTranscript &&
            meaningfulWords >= MIN_TRANSCRIPT_MEANINGFUL_WORDS

        if (isStaleTranscript) {
            issues += "Ignored transcript from a previous recording session."
        }

        // ─── 2. Audio Evidence Validation ────────────────────────────────────────
        var audioIssueCode: EvidenceIssueCode? = null
        var audioFailureState: CoachEvaluationState? = null
        var audioTitle = ""
        var audioMessage = ""

        if (input.audioRequired) {
            val fileExists = input.audioFile != null && input.audioFile.exists()
            val fileLen = if (fileExists) input.audioFile!!.length() else 0L
            val hasBytes = input.audioBytes.isNotEmpty() || fileLen > 0L

            when {
                input.audioAnalysisFailed -> {
                    audioIssueCode = EvidenceIssueCode.AUDIO_ANALYSIS_FAILED
                    audioFailureState = CoachEvaluationState.ANALYSIS_FAILED
                    audioTitle = "Audio Analysis Failed"
                    audioMessage = "We couldn't analyze this recording. Your response has not been scored."
                    issues += audioMessage
                }

                !hasBytes && extractedAudio == null -> {
                    audioIssueCode = if (input.audioFile != null && fileExists && fileLen == 0L) {
                        EvidenceIssueCode.EMPTY_AUDIO_FILE
                    } else {
                        EvidenceIssueCode.MISSING_AUDIO
                    }
                    audioFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                    audioTitle = "Your speaking recording is missing."
                    audioMessage = "Please record your response before requesting feedback."
                    issues += audioMessage
                }

                extractedAudio == null -> {
                    val decoded = decodeAudioFromInput(input)
                    decoded.onFailure { err ->
                        decodeError = err.message ?: "Unsupported or corrupt audio format"
                        audioIssueCode = EvidenceIssueCode.UNSUPPORTED_OR_CORRUPT_AUDIO
                        audioFailureState = CoachEvaluationState.ANALYSIS_FAILED
                        audioTitle = "Audio Processing Failed"
                        audioMessage = "We couldn't analyze this recording. Your response has not been scored."
                        issues += (decodeError ?: audioMessage)
                    }.onSuccess { pcm ->
                        if (pcm.samples.isEmpty()) {
                            decodeError = "Audio stream contained zero readable samples."
                            audioIssueCode = EvidenceIssueCode.EMPTY_AUDIO_FILE
                            audioFailureState = CoachEvaluationState.ANALYSIS_FAILED
                            audioTitle = "Audio Processing Failed"
                            audioMessage = "We couldn't analyze this recording. Your response has not been scored."
                            issues += audioMessage
                        } else {
                            val cleanTranscript = if (transcriptValid) trimmedTranscript else ""
                            extractedAudio = audioEngine.extract(
                                audioSamples = pcm.samples,
                                sampleRate = pcm.sampleRate,
                                transcript = cleanTranscript
                            )
                        }
                    }
                }
            }

            extractedAudio?.let { feats ->
                measuredDuration = feats.durationSeconds
                measuredRms = feats.rmsEnergy
                voicedSeconds = feats.speechDurationSeconds

                when {
                    measuredDuration < MIN_SPEECH_DURATION_SECONDS -> {
                        audioIssueCode = EvidenceIssueCode.DURATION_TOO_SHORT
                        audioFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                        audioTitle = "We couldn't detect enough speech to evaluate this session."
                        audioMessage = "Recording duration (${String.format("%.1f", measuredDuration)}s) is under the ${MIN_SPEECH_DURATION_SECONDS}s minimum. Try speaking clearly for a few seconds and submit again."
                        issues += audioMessage
                    }

                    measuredRms < MIN_SPEECH_RMS_ENERGY || voicedSeconds < MIN_VOICED_SPEECH_SECONDS -> {
                        audioIssueCode = EvidenceIssueCode.SILENT_AUDIO
                        audioFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                        audioTitle = "We couldn't detect speech in this recording."
                        audioMessage = "Try speaking clearly for a few seconds and submit again."
                        issues += audioMessage
                    }

                    else -> {
                        audioValid = true
                    }
                }
            }
        }

        // ─── 3. Video Evidence Validation (ONLY if video assessment is selected) ─
        var videoValid = true
        var videoIssueCode: EvidenceIssueCode? = null
        var videoFailureState: CoachEvaluationState? = null
        var videoTitle = ""
        var videoMessage = ""

        if (input.videoAssessmentSelected) {
            val videoFileOk = input.videoFile == null || (input.videoFile.exists() && input.videoFile.length() > 0L)
            when {
                input.videoAnalysisFailed -> {
                    videoValid = false
                    videoIssueCode = EvidenceIssueCode.VIDEO_ANALYSIS_FAILED
                    videoFailureState = CoachEvaluationState.ANALYSIS_FAILED
                    videoTitle = "Video Analysis Failed"
                    videoMessage = "We couldn't analyze the video frames for this session."
                    issues += videoMessage
                }
                !videoFileOk || (input.videoFile == null && !input.videoAnalysisCompleted && input.videoFramesAnalyzed <= 0) -> {
                    videoValid = false
                    videoIssueCode = EvidenceIssueCode.MISSING_VIDEO
                    videoFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                    videoTitle = "Video recording is missing."
                    videoMessage = "Record video or switch to Audio-Only mode before submitting."
                    issues += videoMessage
                }
                input.videoAnalysisCompleted && input.videoFramesAnalyzed <= 0 -> {
                    videoValid = false
                    videoIssueCode = EvidenceIssueCode.UNREADABLE_VIDEO_FRAMES
                    videoFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                    videoTitle = "No usable video frames detected."
                    videoMessage = "Ensure adequate lighting and camera framing before submitting."
                    issues += videoMessage
                }
                else -> {
                    videoValid = input.videoFramesAnalyzed > 0 && input.videoAnalysisCompleted
                    if (!videoValid) {
                        videoIssueCode = EvidenceIssueCode.UNREADABLE_VIDEO_FRAMES
                        videoFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                        videoTitle = "Video frames could not be assessed."
                        videoMessage = "Frame extraction produced zero analyzable frames."
                        issues += videoMessage
                    }
                }
            }
        }

        // ─── 4. Required Transcript Validation (for transcript-required activities)
        var transcriptFailureState: CoachEvaluationState? = null
        var transcriptIssueCode: EvidenceIssueCode? = null
        var transcriptTitle = ""
        var transcriptMessage = ""

        if (input.transcriptRequired && !transcriptValid) {
            transcriptFailureState = CoachEvaluationState.INSUFFICIENT_EVIDENCE
            transcriptIssueCode = when {
                isStaleTranscript -> EvidenceIssueCode.STALE_TRANSCRIPT
                trimmedTranscript.isBlank() -> EvidenceIssueCode.MISSING_TRANSCRIPT
                else -> EvidenceIssueCode.MEANINGLESS_TRANSCRIPT
            }
            transcriptTitle = "Speech transcript is unavailable."
            transcriptMessage = when {
                isStaleTranscript -> "Previous session transcript cannot be reused. Record or enter a new response."
                trimmedTranscript.isBlank() -> "This transcript-based exercise needs recognized speech or a written response."
                else -> "Speak or enter at least a complete short sentence before requesting evaluation."
            }
            issues += transcriptMessage
        }

        // Determine final validity:
        // Primary gate: if audioRequired -> audioValid MUST be true.
        // If transcriptRequired (or audio NOT required, e.g. written drill) -> transcriptValid MUST be true.
        // Note: if videoAssessmentSelected AND audioValid == true AND videoFailed == false -> allow partial evaluation
        // unless zero modalities succeeded. Wait: if audio fails when audioRequired -> INVALID immediately.
        val terminalFailure = when {
            input.audioRequired && !audioValid -> audioFailureState ?: CoachEvaluationState.INSUFFICIENT_EVIDENCE
            !input.audioRequired && !transcriptValid -> transcriptFailureState ?: CoachEvaluationState.INSUFFICIENT_EVIDENCE
            input.transcriptRequired && !transcriptValid && !audioValid ->
                transcriptFailureState ?: CoachEvaluationState.INSUFFICIENT_EVIDENCE
            else -> null
        }

        val isValid = terminalFailure == null &&
            (!input.audioRequired || audioValid) &&
            (!input.transcriptRequired || transcriptValid || audioValid)

        val primaryCode = audioIssueCode ?: transcriptIssueCode ?: videoIssueCode
        val headline = when {
            audioTitle.isNotBlank() -> audioTitle
            transcriptTitle.isNotBlank() -> transcriptTitle
            videoTitle.isNotBlank() -> videoTitle
            else -> "Ready"
        }
        val detailMessage = when {
            audioMessage.isNotBlank() -> audioMessage
            transcriptMessage.isNotBlank() -> transcriptMessage
            videoMessage.isNotBlank() -> videoMessage
            else -> ""
        }

        val validation = EvidenceValidationResult(
            isValidForEvaluation = isValid,
            terminalFailureState = terminalFailure,
            primaryIssueCode = primaryCode,
            userTitle = headline,
            userMessage = detailMessage,
            audioValid = audioValid,
            videoValid = videoValid,
            videoRequired = input.videoAssessmentSelected,
            transcriptValid = transcriptValid,
            transcriptRequired = input.transcriptRequired,
            measuredDurationSeconds = measuredDuration,
            measuredRmsEnergy = measuredRms,
            voicedSeconds = voicedSeconds,
            audioDecodeError = decodeError,
            issues = issues
        )

        return ValidatedEvidenceBundle(
            validation = validation,
            acousticFeatures = if (audioValid) extractedAudio else null,
            effectiveDurationSeconds = measuredDuration,
            validatedTranscript = if (transcriptValid) trimmedTranscript else "",
            meaningfulWordCount = if (transcriptValid) meaningfulWords else 0
        )
    }

    private fun decodeAudioFromInput(input: EvidenceValidationInput): Result<AudioResampler.DecodedPcmAudio> {
        // 1. Direct memory bytes (supports WAV direct header in both Android and JVM tests)
        if (input.audioBytes.isNotEmpty()) {
            val temp = kotlin.runCatching {
                val ext = if (input.mimeType.contains("wav", ignoreCase = true)) ".wav" else ".m4a"
                File.createTempFile("speakcraft_val_", ext).apply {
                    writeBytes(input.audioBytes)
                    deleteOnExit()
                }
            }
            temp.getOrNull()?.let { f ->
                try {
                    val res = AudioResampler.decodeToPcm16Mono16k(f)
                    if (res.isSuccess) return res
                    if (input.audioFile == null) return res
                } finally {
                    f.delete()
                }
            }
        }
        // 2. Existing file
        val file = input.audioFile
        if (file != null && file.exists()) {
            return AudioResampler.decodeToPcm16Mono16k(file)
        }
        return Result.failure(IllegalArgumentException("No readable audio source available."))
    }
}
