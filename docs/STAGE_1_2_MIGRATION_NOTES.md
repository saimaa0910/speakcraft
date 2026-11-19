# SpeakCraft Stage 1.2 Architecture Migration Notes

**Document:** `docs/STAGE_1_2_MIGRATION_NOTES.md`  
**Date:** 2026-11-19  
**Milestone:** Stage 1.2 — Lightweight, Offline-First Intelligence for Low-End Android Phones  
**Status:** Completed & Validated  

---

## 1. Executive Summary

Stage 1.2 transitions SpeakCraft from an experimental heavy on-device model architecture (which evaluated Phi-3.5 Mini Instruct Q4 at ~2.2 GB) to an ultra-efficient, deterministic, lightweight architecture tailored for budget and mid-range Android smartphones.

### Key Milestones Achieved:
1. **Zero Large Language Models Required:** Completely purged Phi-3.5 Mini and LiteRT-LM runtime dependencies.
2. **Deterministic Multi-Subengine Feedback (`LocalFeedbackEngine`):** Implemented 7 specialized sub-engines providing evidence-grounded coaching across all 6 canonical sections.
3. **Multi-Tier Speech Recognition (`SpeechRecognitionManager`):** Supports Whisper Tiny INT8 (~39 MB), Moonshine Tiny INT8 (~26 MB), Android offline ASR, and a 0 MB audio-only fallback.
4. **Streaming Audio Decoding (No 30s Truncation):** Audio processing supports arbitrary recording lengths up to 10 minutes without heap exhaustion.
5. **Layered Pronunciation Assessment:** Word alignment backtrace with Indian English accent tolerance and transparent non-phonetic labeling.
6. **Modular Grammar Rules with Rule IDs:** 11 distinct grammar categories with unique rule IDs and conservative thresholds.
7. **Strict Privacy Invariant:** Zero cloud AI calls verified. All coaching is computed 100% locally on the device.

---

## 2. File-by-File Modification Log

### Build & Dependency Configuration
- `app/build.gradle.kts`:
  - Removed `libs.litert.lm` dependency.
  - Ensured absence of `com.google.firebase:firebase-ai`.
- `gradle/libs.versions.toml`:
  - Removed `litert-lm` version and library definitions.

### Model Catalog & Device Capabilities
- `app/src/main/java/com/example/data/ai/local/OnDeviceModelManager.kt`:
  - Removed `LocalModelId.PHI35_MINI_INSTRUCT_Q4`.
  - Added `LocalModelId.MOONSHINE_TINY_INT8`.
  - Added `LightweightModelManager` type alias.
  - Implemented SHA-256 verification and `recoverCorruptModel()`.
  - Added invariant checking that no model in the catalog exceeds 100 MB.
- `app/src/main/java/com/example/data/ai/local/DeviceCapabilityManager.kt`:
  - Deprecated `canRunLocalSLM()`.
  - Added `canRunConcurrently()` (requires $\ge 3$ GB RAM; below this, audio and video analysis execute serially to minimize peak memory).
  - Added `recommendedFrameSamplingIntervalMs()` for adaptive video frame extraction.
- `app/src/main/java/com/example/data/ai/local/OfflineAIAvailabilityManager.kt`:
  - Removed SLM installation requirements.
  - Emphasized 0 MB download requirement for basic coaching.

### Audio Pipeline & Speech Recognition
- `app/src/main/java/com/example/data/ai/local/audio/AudioResampler.kt`:
  - Removed the hardcoded 30-second cap (`MAX_OUTPUT_SAMPLES = 480_000`).
  - Implemented chunked decoding (`decodeInChunks()`, `BUFFER_SIZE = 4096`).
  - Added dynamic buffer expansion supporting up to 10 minutes of audio.
- `app/src/main/java/com/example/data/ai/local/audio/AudioFeatureExtractionEngine.kt`:
  - Added `shortPauseCount` (300–750ms) and `longPauseCount` (>750ms) metrics.
  - Added `speakingRateConsistencyScore` and `averageSentenceLength`.
  - Added explicit scientific disclaimer that pitch and loudness do not measure confidence or personality traits.
- `app/src/main/java/com/example/data/ai/local/audio/LocalSpeechRecognitionEngine.kt`:
  - Implemented multi-window sliding inference (30s window sliding) to transcribe recordings of arbitrary duration without truncation.
  - Guaranteed `interpreter?.close()` in `finally` blocks to release native memory immediately.
- `app/src/main/java/com/example/data/ai/local/audio/SpeechRecognitionManager.kt`:
  - Created multi-tier speech manager with availability checks, SHA-256 validation, and audio-only fallback.
- `app/src/main/java/com/example/data/ai/local/audio/PronunciationAnalysisEngine.kt`:
  - Rewrote with a 5-layer pipeline: text normalization & contraction expansion, transcript comparison, Needleman-Wunsch/Levenshtein word alignment matrix with backtrace, Indian English accent tolerance (/th/ $\rightarrow$ /t/, /d/; /w/ $\leftrightarrow$ /v/), confidence scoring, and explicit non-phonetic transparency labeling.

### Linguistic Analysis & Structured Feedback
- `app/src/main/java/com/example/data/ai/local/language/LocalLanguageAnalysisEngine.kt`:
  - Implemented 11 modular grammar categories with unique rule IDs:
    - Subject-Verb Agreement: `SVA_001` .. `SVA_005`
    - Verb Tense: `TNS_001`, `TNS_002`
    - Articles: `ART_001`, `ART_002`
    - Prepositions: `PRP_001` .. `PRP_005`
    - Pronoun Case: `PRN_001` .. `PRN_003`
    - Number Agreement: `NUM_001` .. `NUM_003`
    - Word Choice: `WCH_001` .. `WCH_003`
    - Sentence Structure: `STR_001`, `STR_002`
    - Sentence Fragments: `FRG_001`
    - Run-on Sentences: `RUN_001`
    - Repetitions: `REP_001`
- `app/src/main/java/com/example/data/ai/local/language/LocalFeedbackEngine.kt`:
  - Created master deterministic feedback engine orchestrating 7 specialized sub-engines:
    1. `GrammarFeedbackEngine`
    2. `FluencyFeedbackEngine`
    3. `VocabularyFeedbackEngine`
    4. `PronunciationFeedbackEngine`
    5. `ConfidenceFeedbackEngine` (strictly empirical vocal projection + physical composure; no personality claims)
    6. `StructureFeedbackEngine`
    7. `ImprovementRecommendationEngine`
  - Produces all 6 canonical sections matching `StructuredFeedback`.
- `app/src/main/java/com/example/data/ai/local/language/LocalFeedbackGenerationEngine.kt`:
  - Refactored to delegate directly to `LocalFeedbackEngine`.
  - Purged reflection instantiation of LiteRT-LM.

### Orchestration & Scenarios
- `app/src/main/java/com/example/data/ai/local/LocalIntelligenceOrchestrator.kt`:
  - Integrated `SpeechRecognitionManager` for primary/secondary/audio-only transcription.
  - Integrated `LocalFeedbackEngine` for deterministic structured feedback.
  - Removed `tryGenerateConversationWithSlm` and references to `PHI35_MINI_INSTRUCT_Q4`.
  - Switched conversation simulation turns to 100% deterministic contextual responses via `ScenarioScriptBank`.

### Assets & Documentation
- `app/src/main/assets/models/README.md`: Updated model catalog documentation.
- `docs/LIGHTWEIGHT_AI_AUDIT.md`: Complete audit of memory, storage, and models.
- `docs/SPEECH_MODEL_BENCHMARK_REPORT.md`: Whisper Tiny vs Moonshine Tiny vs Android offline ASR.
- `docs/MODEL_SIZE_AND_RAM_REPORT.md`: Comprehensive RAM and storage budgets across device tiers.
- `docs/DEVICE_COMPATIBILITY_REPORT_STAGE1_2.md`: Real-device compatibility results (API 24 to 35).
- `docs/STAGE_1_2_MIGRATION_NOTES.md`: This document.

### Unit Tests
- `app/src/test/java/com/example/LocalAIFoundationTest.kt`: Updated for lightweight model catalog.
- `app/src/test/java/com/example/SpeechRecognitionManagerTest.kt`: Tests availability check, audio-only fallback, and integrity validation.
- `app/src/test/java/com/example/LightweightFeedbackEngineTest.kt`: Comprehensive tests for all 7 sub-engines, modular grammar rules, Indian English accent tolerance, and 6 canonical sections.

---

## 3. Backward Compatibility & System Integrity

1. **Activity Engine Preserved:** All 150 activities, 10 learning modules, and 25 exercise templates remain completely unchanged.
2. **Database & Firestore Models Preserved:** `SpeakingSession`, `SpeechFeedback`, and `UserProfile` schemas are 100% identical.
3. **6 Canonical Feedback Sections Preserved:**
   - `whatWentWell: List<String>`
   - `specificMistakes: List<String>`
   - `correctedExamples: List<String>`
   - `explanation: String`
   - `improvementSuggestions: List<String>`
   - `nextPracticeTask: String`
   (plus `honestyNotice: String` for transparency).
4. **Android SDK Requirements:** `minSdk = 24`, `targetSdk = 35` maintained.

---

## 4. Privacy & Zero-Cloud Invariant Verification

Verification was executed across the codebase:
- OkHttp `LocalExecutionPolicy.aiEndpointBlockingInterceptor` blocks all generative AI endpoints.
- Regex search across all Kotlin sources for cloud AI symbols:
  ```powershell
  Get-ChildItem -Path app\src\main\java -Recurse -Filter "*.kt" |
    Select-String "Firebase\.ai|firebase\.ai|generativeModel|generateContent|startChat"
  ```
  **Result:** 0 matches.
- Regex search for residual large LLM identifiers (`PHI35` / `litert.lm`):
  **Result:** 0 matches in production code.

**Conclusion:** SpeakCraft Stage 1.2 is fully verified, 100% offline-capable, and optimized for low-end Android smartphones.
