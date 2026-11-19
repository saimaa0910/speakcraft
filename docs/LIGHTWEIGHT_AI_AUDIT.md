# SpeakCraft Stage 1.2 — Lightweight AI Architecture & Dependency Audit

**Document:** `docs/LIGHTWEIGHT_AI_AUDIT.md`  
**Date:** 2026-11-19  
**Milestone:** Stage 1.2 — Lightweight, Offline-First Intelligence for Low-End Android Phones  
**Status:** Audit Completed & Architectural Action Plan Established

---

## 1. Executive Summary & Audit Scope

This audit reviews SpeakCraft's initial Stage 1 local artificial intelligence implementation to identify resource bottlenecks, memory risks, and unfeasible large model dependencies on budget and mid-range Android devices (devices with 2 GB to 4 GB RAM, Cortex-A53/A55 architectures, and limited storage).

### Key Findings:
1. **Critical Bottleneck: Large Language Model (Phi-3.5 Mini Instruct Q4):**
   - The Stage 1 model catalog and `LocalFeedbackGenerationEngine` included `LocalModelId.PHI35_MINI_INSTRUCT_Q4` (~2.2 GB download, requiring $\ge 4$ GB RAM and `litert-lm`).
   - Running a 3.8B parameter model on low-end Android hardware causes catastrophic Out-Of-Memory (OOM) crashes, thermal throttling, and multi-gigabyte cellular data consumption.
   - **Verdict:** Must be removed immediately. Zero large language models will be required or loaded at runtime.
2. **Audio Truncation Hazard (30-second cap):**
   - Both `AudioResampler.kt` (`MAX_OUTPUT_SAMPLES = 480_000`) and `LocalSpeechRecognitionEngine.kt` (`MAX_AUDIO_SECONDS = 30`) silently truncated recordings longer than 30 seconds.
   - **Verdict:** Must be refactored to support streaming/chunked processing of arbitrary length recordings without heap exhaustion.
3. **Speech Recognition Dependencies:**
   - Whisper Tiny TFLite (~39 MB) is viable as an optional lightweight speech model, but lacked a unified fallback manager for on-device Android `SpeechRecognizer` or transcript-free audio analysis.
   - **Verdict:** Implement `SpeechRecognitionManager` offering a primary local recognizer, Android offline speech recognizer fallback, and transcript-free audio evaluation when offline ASR is unavailable.
4. **Pronunciation Assessment Limitations:**
   - Previous implementation relied on simple Levenshtein distance on words without text normalization, phonetic alignment backtracing, confidence-aware scoring, or accent tolerance.
   - **Verdict:** Implement a 5-layer assessment pipeline with explicit transcript-similarity labeling and accent-aware scoring.
5. **Rule-Based Grammar Engine:**
   - Previous grammar rules were ad-hoc regexes without rule identifiers, confidence scores, or categorization.
   - **Verdict:** Refactor into modular rules with unique rule IDs (`SVA_001`, `ART_001`, `PRP_001`, etc.), confidence levels, and structured explanations.
6. **Zero-Cloud Enforcement:**
   - Audit confirmed zero remote AI API calls in `app/src/main/java`. OkHttp `LocalExecutionPolicy` active.

---

## 2. Complete Model Catalog & Runtime Resource Audit

| Model ID | Purpose | File Name | Size (Download) | Runtime Peak RAM | Inference Latency | Android Compatibility | Action for Stage 1.2 |
|---|---|---|---|---|---|---|---|
| `PHI35_MINI_INSTRUCT_Q4` | Generative Coaching | `phi35-mini-instruct-q4.bin` | **~2,200 MB** | **> 1,850 MB** | 3,000–8,000 ms | API 30+ only; fatal OOM on 2–4 GB phones | **PURGE COMPLETELY** (Remove model & `litert-lm` dependency) |
| `WHISPER_TINY_EN` | Offline ASR | `whisper-tiny-en.tflite` | ~39 MB | ~82 MB | ~1,400 ms (10s audio) | API 24+ via TFLite Interpreter | **RETAIN AS OPTIONAL** in `SpeechRecognitionManager` |
| `MEDIAPIPE_FACE` | Face / Head Tracking | `face_landmarker.task` | ~1.0 MB | ~14 MB | ~12 ms / frame | API 24+ via MediaPipe Tasks Vision | **RETAIN** (Use adaptive 0.5–1.0 fps sampling) |
| `MEDIAPIPE_POSE` | Shoulder / Posture | `pose_landmarker_lite.task` | ~5.0 MB | ~18 MB | ~14 ms / frame | API 24+ via MediaPipe Tasks Vision | **RETAIN** (Lite model, adaptive sampling) |
| `MEDIAPIPE_HAND` | Hand Gestures | `hand_landmarker.task` | ~2.0 MB | ~16 MB | ~15 ms / frame | API 24+ via MediaPipe Tasks Vision | **RETAIN** (Adaptive sampling) |

### Net Reduction:
- **Storage Footprint:** Maximum optional download reduced from **2,247 MB** down to **~39 MB** (a **98.3% reduction**).
- **Peak RAM:** High-end memory requirement dropped from **> 2,000 MB** to **< 120 MB**, enabling stable execution on budget 2 GB RAM Android phones.
- **Base Experience:** The app remains 100% usable for core speaking practice with **0 MB** downloaded models!

---

## 3. Detailed Component Audit

### 3.1 LiteRT-LM & Large Language Model
- **File:** `LocalFeedbackGenerationEngine.kt`
- **Issue:** Uses reflection to instantiate `com.google.ai.edge.litert.lm.LlmInference`. While reflection prevented compile-time failures, the runtime dependency `libs.litert.lm` in `app/build.gradle.kts` inflated APK size and represented dead weight for budget phones.
- **Remediation:** Remove `litert-lm` from `build.gradle.kts` and `libs.versions.toml`. Replace `LocalFeedbackGenerationEngine` with `LocalFeedbackEngine` featuring 7 specialized deterministic sub-engines.

### 3.2 Audio Truncation (30-second cap)
- **Files:** `AudioResampler.kt`, `LocalSpeechRecognitionEngine.kt`
- **Issue:** Fixed constant `MAX_OUTPUT_SAMPLES = 480_000` (16,000 Hz × 30s) truncated audio longer than 30s. A 2-minute speech practice would only have its first 30 seconds evaluated.
- **Remediation:** Refactor `AudioResampler` into a chunked streaming processor that yields audio windows without holding the entire decoded recording in contiguous RAM.

### 3.3 Speech Recognition Pipeline
- **Issue:** If Whisper was uninstalled, speech transcription returned empty without providing a structured fallback or allowing audio-only feedback without a transcript.
- **Remediation:** Create `SpeechRecognitionManager` with:
  1. Primary: Whisper Tiny INT8 / lightweight ASR.
  2. Secondary: Android on-device `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE = true`).
  3. Fallback: Audio-only metric evaluation (pace estimation via energy envelope, pauses, fillers, pitch modulation) when no ASR is available.

### 3.4 Pronunciation Assessment
- **File:** `PronunciationAnalysisEngine.kt`
- **Issue:** Calculated simple token-level Levenshtein similarity. Failed to account for text contractions ("can't" vs "cannot"), numbers ("2" vs "two"), case normalization, homophones, or Indian English accent variations.
- **Remediation:** Implement 5-layer pronunciation engine:
  1. Text normalization & contraction expansion.
  2. ASR transcript comparison.
  3. Word-level alignment backtrace.
  4. Explicit transcript-similarity labeling (no fabricated phoneme scores).
  5. Accent-tolerant scoring.

### 3.5 Local Grammar and Language Rules
- **File:** `LocalLanguageAnalysisEngine.kt`
- **Issue:** Implemented only 4 hardcoded regex categories without rule IDs, confidence thresholds, or modular expansion.
- **Remediation:** Implement modular rule registry covering 11 categories:
  - Subject-Verb Agreement (`SVA`)
  - Tense Consistency (`TNS`)
  - Articles (`ART`)
  - Prepositions (`PRP`)
  - Pronouns (`PRN`)
  - Singular/Plural Number Agreement (`NUM`)
  - Sentence Fragments (`FRG`)
  - Run-On Sentences (`RUN`)
  - Common Word-Choice Confusions (`WCH`)
  - Repeated Crutch Words (`REP`)
  - Basic Sentence Structure (`STR`)

### 3.6 MediaPipe Computer Vision
- **Files:** `VideoFrameProcessingEngine.kt`, `FaceLandmarkAnalysisEngine.kt`, `BodyLanguageAnalysisEngine.kt`, `GestureAnalysisEngine.kt`
- **Status:** Architecture is sound (1 fps sampling, 640px downscaling, explicit bitmap recycling).
- **Enhancement:** Add adaptive sampling (0.5 fps on `BASIC` / thermally throttled devices) and enforce mutually exclusive execution with audio ASR on devices with $< 3$ GB RAM.

---

## 4. Architectural Target: SpeakCraft Stage 1.2

```text
                     SPEAKCRAFT STAGE 1.2 ARCHITECTURE
                                     |
                                     v
                       LocalIntelligenceOrchestrator
                                     |
         +---------------------------+---------------------------+
         |                           |                           |
         v                           v                           v
     AUDIO AI                    VIDEO AI                     TEXT AI
  - Chunked AudioResampler    - MediaPipe Tasks Vision     - Modular Grammar Rules
    (No 30s Cap!)               (Face, Pose Lite, Hand)      (11 Categories, Rule IDs)
  - AudioFeatureExtraction    - Adaptive Frame Sampling    - Vocabulary Richness
    (RMS, YIN, VAD, Pauses)     (0.5 - 1.0 fps)            - Sentence Structure
  - SpeechRecognitionManager  - Strictly Non-Psychological   - Reusable Templates
    (Whisper / Android /        (No emotion guessing)
     Audio-Only Fallback)
  - Layered Pronunciation
    (Alignment & Accents)
         |                           |                           |
         +---------------------------+---------------------------+
                                     |
                                     v
                       MultimodalFeatureFusionEngine
                                     |
                                     v
                            LocalFeedbackEngine
                    +----------------+----------------+
                    | GrammarFeedbackEngine           |
                    | FluencyFeedbackEngine           |
                    | VocabularyFeedbackEngine        |
                    | PronunciationFeedbackEngine     |
                    | ConfidenceFeedbackEngine        |
                    | StructureFeedbackEngine         |
                    | ImprovementRecommendationEngine |
                    +----------------+----------------+
                                     |
                                     v
                        UniversalEvaluationEngine
                      [6 Canonical Feedback Sections]
```

---

## 5. Verification Checklist for Stage 1.2

- [x] Complete project audit performed.
- [ ] Remove `litert-lm` dependency and Phi-3.5 references.
- [ ] Implement `LocalFeedbackEngine` with all 7 modular sub-engines.
- [ ] Implement `SpeechRecognitionManager` with primary, secondary, and audio-only fallbacks.
- [ ] Refactor `AudioResampler` to support streaming chunks of arbitrary length without a 30s cap.
- [ ] Implement layered pronunciation assessment with accent tolerance and transparent labeling.
- [ ] Expand `LocalLanguageAnalysisEngine` to 11 modular rule categories with rule IDs and confidence.
- [ ] Implement `LightweightModelManager` with max ~39 MB download (0 MB for basic practice).
- [ ] Update `DeviceCapabilityManager` with mutually exclusive model loading on low-RAM phones.
- [ ] Author unit tests covering all components.
