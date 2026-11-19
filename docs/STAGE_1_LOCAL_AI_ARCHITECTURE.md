# Stage 1 — Local AI Architecture Specification

**Product:** SpeakCraft — On-Device AI Communication Skills Coach  
**Revision:** Stage 1 Implementation  
**Execution Policy:** `LOCAL_ONLY` (Strict Zero-Cloud AI Inference Guarantee)

---

## 1. Executive Summary

SpeakCraft Stage 1 delivers an entirely local, on-device multimodal artificial intelligence and deterministic scoring architecture. All audio processing, speech-to-text transcription, acoustic signal processing, computer vision body-language tracking, linguistic grammar checks, and pedagogical feedback generation execute locally on the user's Android smartphone without dispatching a single byte of user audio, video, transcripts, or dialogue to any cloud AI provider.

### Core Architectural Invariants
1. **Zero Cloud Inference:** No dependencies on Google Gemini Developer API, Firebase AI Logic, OpenAI, Claude, Groq, Hugging Face Inference API, or any remote speech/vision model endpoints.
2. **Offline-First:** All core features function without an active internet connection after optional model assets are installed.
3. **Data Privacy:** User recordings and transcripts remain in app-private storage (`context.filesDir`). Media is never uploaded to Firestore or external storage buckets.
4. **Graceful Tiered Degradation:** Devices run on one of three capability profiles (`BASIC`, `STANDARD`, `ADVANCED`), falling back gracefully to deterministic rule-based evaluation when specialized neural models are absent or unsupported.

---

## 2. High-Level Architectural Diagram

```text
                           SPEAKCRAFT ANDROID CLIENT
                                       |
                                       v
                         LocalIntelligenceOrchestrator
                                       |
         +-----------------------------+-----------------------------+
         |                             |                             |
         v                             v                             v
     AUDIO AI                      VIDEO AI                       TEXT AI
         |                             |                             |
  +------+------+               +------+------+               +------+------+
  |             |               |             |               |             |
  v             v               v             v               v             v
Speech ASR   Acoustic DSP   Frame Proc   MediaPipe Land    Deterministic   LiteRT-LM
(Whisper)    (YIN/RMS/VAD)  (Sampling)   (Pose/Face/Hand)  Grammar/Vocab   (Phi-3.5)
         |                             |                             |
         +-----------------------------+-----------------------------+
                                       |
                                       v
                         MultimodalFeatureFusionEngine
                                       |
                                       v
                          UniversalEvaluationEngine
                        [9 Local Evaluator Strategies]
                                       |
                                       v
                         LocalFeedbackGenerationEngine
                         [6-Section Structured Schema]
                                       |
                        +--------------+--------------+
                        |                             |
                        v                             v
               Local State & UI Flow         Firestore Progress Sync
               (Instant Response)            (Scores/Streaks Only)
```

---

## 3. Component Inventory

The local intelligence subsystem consists of 15 modular, decoupled components located under `com.example.data.ai.local`:

| Component | Class / Object | Responsibility |
|---|---|---|
| **Execution Policy** | `LocalExecutionPolicy` | Enforces `LOCAL_ONLY` policy; intercepts & blocks cloud AI HTTP calls. |
| **Model Manager** | `OnDeviceModelManager` | Manages download, cataloging, SHA-256 verification, and deletion of local models. |
| **Capability Manager** | `DeviceCapabilityManager` | Detects RAM, API level, thermal state, NPU support, and assigns `DeviceProfile`. |
| **Availability Manager** | `OfflineAIAvailabilityManager` | Aggregates hardware profiles and installed models to report live AI status. |
| **Audio Resampler** | `AudioResampler` | Decodes M4A/WAV to 16 kHz mono Int16 PCM using native MediaCodec & MediaExtractor. |
| **Speech Recognition** | `LocalSpeechRecognitionEngine` | On-device ASR via Whisper Tiny EN (TFLite) or offline SpeechRecognizer fallback. |
| **Audio Feature Engine** | `AudioFeatureExtractionEngine` | YIN pitch estimation, RMS energy, clipping detection, VAD, pause analytics, and WPM. |
| **Filler Word Engine** | `FillerWordDetectionEngine` | Regex word-boundary disfluency detection with per-minute frequency metrics. |
| **Pronunciation Engine** | `PronunciationAnalysisEngine` | Word-level Levenshtein alignment, intelligibility rating, and honest limitation notice. |
| **Video Frame Engine** | `VideoFrameProcessingEngine` | MediaMetadataRetriever-based frame sampling (1 fps) with automated bitmap recycling. |
| **Face Landmark Engine** | `FaceLandmarkAnalysisEngine` | MediaPipe Face Landmarker: yaw/pitch estimation, camera presence, front-facing consistency. |
| **Body Language Engine** | `BodyLanguageAnalysisEngine` | MediaPipe Pose Landmarker: shoulder alignment, stability, movement level, and posture cues. |
| **Gesture Engine** | `GestureAnalysisEngine` | MediaPipe Hand Landmarker: hand visibility, wrist movement variance, and consistency. |
| **Language Engine** | `LocalLanguageAnalysisEngine` | Deterministic grammar heuristics (articles, verb agreement, run-ons) & vocab diversity. |
| **Multimodal Fusion** | `MultimodalFeatureFusionEngine` | Unifies audio, visual, and linguistic metrics into `LocalFeedbackEvidence`. |
| **Feedback Engine** | `LocalFeedbackGenerationEngine` | Generates 6-section feedback using LiteRT-LM (Phi-3.5) or deterministic templates. |
| **Template Fallback** | `DeterministicFeedbackTemplates` | Hardened rule-based generator for all 6 feedback sections without neural hallucination. |
| **Central Orchestrator**| `LocalIntelligenceOrchestrator`| Primary facade replacing `GeminiCoachService` across all app ViewModels. |

---

## 4. Multimodal Data Flow

### Recording & Processing Lifecycle
1. **Capture:** Learner records speech via `AudioRecorderManager` (M4A) or video via `CameraVideoRecorder` (MP4).
2. **Decode:** Audio is extracted and resampled to 16 kHz 16-bit PCM in chunks of up to 30 seconds. Video frames are sampled at 1.0 fps and scaled down to max 640px width.
3. **Perception:**
   - Audio samples pass through `AudioFeatureExtractionEngine` (computing RMS, pitch curve, pauses) and `LocalSpeechRecognitionEngine` (yielding transcript).
   - Video frames stream sequentially through MediaPipe Face, Pose, and Hand landmarkers; bitmaps are recycled immediately.
   - Text transcript passes through `LocalLanguageAnalysisEngine` and `FillerWordDetectionEngine`.
4. **Feature Fusion:** `MultimodalFeatureFusionEngine` aligns timestamps, normalizes metrics, and attaches honest modality caveats into `LocalFeedbackEvidence`.
5. **Synthesis:** `LocalFeedbackGenerationEngine` maps evidence into the 6-section feedback schema (What Went Well, Specific Mistakes, Corrected Examples, Explanation, Improvement Suggestions, Next Practice Task).
6. **Persistence:** `ActivityEngine` stores the objective scores, duration, and completed practice cues in local state and synchronously stages Firestore progress updates (without raw media).
