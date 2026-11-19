package com.example.data.ai.local.fusion

import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.FillerWordDetectionEngine
import com.example.data.ai.local.language.LanguageAnalysisResult
import com.example.data.ai.local.language.LocalFeedbackEvidence
import com.example.data.ai.local.video.FaceAnalysisResult
import com.example.data.ai.local.video.GestureAnalysisResult
import com.example.data.ai.local.video.PostureAnalysisResult

/**
 * Multimodal Feature Fusion Engine.
 *
 * Integrates independent sensory feature extractors (acoustic, video face, pose, hand gestures,
 * and linguistic analysis) into a coherent [LocalFeedbackEvidence] bundle.
 * Handles missing modalities gracefully by applying conservative fallbacks and collecting
 * explicit limitation notices.
 */
object MultimodalFeatureFusionEngine {

    /**
     * Fuses all available sensory and linguistic features into a unified [LocalFeedbackEvidence].
     *
     * @param audio          Acoustic features from [AudioFeatureExtractionEngine] (or null).
     * @param face           Face landmarks result from [FaceLandmarkAnalysisEngine] (or null).
     * @param posture        Posture analysis from [BodyLanguageAnalysisEngine] (or null).
     * @param gesture        Hand gesture analysis from [GestureAnalysisEngine] (or null).
     * @param language       Linguistic analysis from [LocalLanguageAnalysisEngine] (or null).
     * @param transcript     Spoken or written transcript text.
     * @param expectedText   Optional reference text (for reading aloud or pronunciation).
     * @param evaluatorName  Name of the active evaluator strategy (e.g. "SpeakingEvaluator").
     * @param exerciseTitle  Title of the exercise being practiced.
     * @return Fully populated [LocalFeedbackEvidence] ready for feedback generation.
     */
    fun fuse(
        audio: AcousticFeatures?,
        face: FaceAnalysisResult?,
        posture: PostureAnalysisResult?,
        gesture: GestureAnalysisResult?,
        language: LanguageAnalysisResult?,
        transcript: String,
        expectedText: String?,
        evaluatorName: String,
        exerciseTitle: String
    ): LocalFeedbackEvidence {
        val limitationNotices = mutableListOf<String>()

        // ── 1. Audio and Acoustic Features ─────────────────────────────────────
        val speechDuration = audio?.speechDurationSeconds ?: audio?.durationSeconds ?: 0.0
        val pauseCount = audio?.pauseCount ?: 0
        val meanPauseMs = audio?.meanPauseDurationMs ?: 0.0

        if (audio == null) {
            limitationNotices += "Acoustic delivery metrics were not recorded or could not be decoded."
        } else {
            if (audio.clippingDetected) {
                limitationNotices += "Microphone clipping was detected; lowering input volume will improve accuracy."
            }
            if (audio.backgroundNoiseLevel > 0.06f) {
                limitationNotices += "Background noise was present during recording; quieter environments yield better clarity."
            }
        }

        // ── 2. Linguistic and Transcript Analysis ──────────────────────────────
        val cleanTranscript = transcript.trim()
        val fillerResult = if (cleanTranscript.isNotBlank()) {
            FillerWordDetectionEngine.detect(cleanTranscript)
        } else {
            null
        }

        val wordCount = when {
            language != null -> cleanTranscript.split(Regex("\\s+")).count { it.isNotBlank() }
            audio != null && audio.wordCount > 0 -> audio.wordCount
            fillerResult != null -> fillerResult.wordCount
            cleanTranscript.isNotBlank() -> cleanTranscript.split(Regex("\\s+")).count { it.isNotBlank() }
            else -> 0
        }

        val wordsPerMinute = when {
            audio != null && audio.wordsPerMinute > 0 -> audio.wordsPerMinute.toInt()
            speechDuration >= 5.0 && wordCount > 0 -> ((wordCount * 60.0) / speechDuration).toInt().coerceIn(30, 320)
            else -> 0
        }

        val fillerCount = fillerResult?.totalFillerCount ?: 0
        val detectedFillers = fillerResult?.detectedFillers?.map { "${it.key} (${it.value})" } ?: emptyList()

        if (cleanTranscript.isBlank()) {
            limitationNotices += "No speech transcript available; linguistic and grammar analysis was skipped."
        }

        // ── 3. Visual Multimodal Ingestion ─────────────────────────────────────
        val hasVideo = face != null || posture != null || gesture != null

        if (face != null) {
            limitationNotices.addAll(face.limitations)
        }
        if (posture != null) {
            limitationNotices.addAll(posture.limitations)
        }
        if (gesture != null) {
            limitationNotices.addAll(gesture.limitations)
        }

        if (!hasVideo) {
            limitationNotices += "Video analysis unavailable (audio-only submission)."
        }

        // ── 4. Determine Processing Mode ───────────────────────────────────────
        val processingMode = when {
            hasVideo && audio != null -> "MULTIMODAL_AUDIO_VIDEO_LOCAL"
            audio != null             -> "LOCAL_ACOUSTIC_LINGUISTIC"
            cleanTranscript.isNotBlank() -> "LOCAL_LINGUISTIC_ONLY"
            else                      -> "LOCAL_MINIMAL_EVIDENCE"
        }

        return LocalFeedbackEvidence(
            transcript = cleanTranscript,
            expectedText = expectedText,
            wordCount = wordCount,
            wordsPerMinute = wordsPerMinute,
            fillerCount = fillerCount,
            detectedFillers = detectedFillers,
            pauseCount = pauseCount,
            meanPauseDurationMs = meanPauseMs,
            speechDurationSeconds = speechDuration,
            grammarIssues = language?.grammarIssues ?: emptyList(),
            vocabularyAnalysis = language?.vocabularyAnalysis,
            audioFeatures = audio,
            faceAnalysis = face,
            postureAnalysis = posture,
            gestureAnalysis = gesture,
            exerciseTitle = exerciseTitle,
            evaluatorName = evaluatorName,
            limitationNotices = limitationNotices.distinct(),
            processingMode = processingMode
        )
    }
}
