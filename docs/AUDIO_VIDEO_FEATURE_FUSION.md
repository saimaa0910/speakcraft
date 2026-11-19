# Audio-Video Multimodal Feature Fusion Engine

**Product:** SpeakCraft — Stage 1  
**Subsystem:** Multimodal Alignment, Feature Normalization, and Evidence Aggregation  
**Location:** `com.example.data.ai.local.fusion.MultimodalFeatureFusionEngine`

---

## 1. Architectural Purpose

Human communication consists of verbal, vocal, and visual elements (the "3Vs" in Vinh Giang's curriculum). The `MultimodalFeatureFusionEngine` reconciles independently extracted signals from audio, computer vision, and linguistic pipelines into a synchronized evidence structure (`LocalFeedbackEvidence`).

Crucially, the fusion engine guarantees modality independence:
- **Audio-Only Mode:** Evaluates speech, fillers, pace, and vocal modulation even when no video exists.
- **Video-Only Mode:** Evaluates posture, framing, and camera presence even when audio is muted or absent.
- **Transcript-Only Mode:** Evaluates grammar, structure, and vocabulary even when no media files exist.
- **Combined Multimodal Mode:** Performs full synchronized alignment across all three sensory channels.

---

## 2. Feature Fusion Pipeline

```text
  AUDIO SIGNAL          VIDEO FRAMES           USER TRANSCRIPT
       |                     |                        |
       v                     v                        v
  AcousticFeatures    Visual Landmark Results    LanguageAnalysisResult
  - Duration, RMS     - Face Yaw/Pitch, Presence - Grammar Issues
  - Pitch (mean/var)  - Shoulder Alignment       - Vocab Diversity
  - WPM, Pauses       - Hand Gesture Frequency   - Repeated Words
  - Filler Counts     - Stability & Movement     - Sentence Length
       |                     |                        |
       +---------------------+------------------------+
                             |
                             v
               MultimodalFeatureFusionEngine
                             |
       1. Timestamp Alignment & Duration Validation
       2. Modality Independence Null-Handling
       3. Metric Normalization & Caveat Compilation
                             |
                             v
                   LocalFeedbackEvidence
```

---

## 3. Data Structure: LocalFeedbackEvidence

The normalized output of the fusion engine is passed directly into the feedback generators:

```kotlin
data class LocalFeedbackEvidence(
    val transcript: String,
    val expectedText: String?,
    val wordCount: Int,
    val wordsPerMinute: Int,
    val fillerCount: Int,
    val detectedFillers: Map<String, Int>,
    val pauseCount: Int,
    val meanPauseDurationMs: Long,
    val speechDurationSeconds: Double,
    val grammarIssues: List<GrammarIssue>,
    val vocabularyAnalysis: VocabularyAnalysis?,
    val audioFeatures: AcousticFeatures?,
    val faceAnalysis: FaceAnalysisResult?,
    val postureAnalysis: PostureAnalysisResult?,
    val gestureAnalysis: GestureAnalysisResult?,
    val exerciseTitle: String,
    val evaluatorName: String,
    val limitationNotices: List<String>,
    val processingMode: String
)
```

---

## 4. Evidence Grounding & Cross-Modal Scoring Rules

1. **Vocal Pacing vs. Physical Movement:**
   - When speaking pace is fast ($> 180\text{ WPM}$) and visual movement is high ($\sigma > 0.040$), the feedback highlights rushed delivery and recommends planting feet while introducing deliberate silent pauses.
2. **Filler Frequency vs. Pause Duration:**
   - When filler count is high ($> 4$) and pause count is low ($< 2$), the feedback highlights that the speaker fills thinking time with verbal crutches rather than comfortable silence.
3. **Voice Pitch vs. Posture Alignment:**
   - Slouched posture accompanied by low voice volume ($< 0.05\text{ RMS}$) generates posture alignment advice to expand the diaphragm and support vocal projection.
4. **Honest Caveat Aggregation:**
   - If audio was absent, a notice is added: *"Audio delivery was not assessed; feedback is based on text and physical cues."*
   - If video was absent, a notice is added: *"Camera presence was not assessed; record video to receive body language observations."*
   - Limitation notices are surfaced directly in the final user interface.
