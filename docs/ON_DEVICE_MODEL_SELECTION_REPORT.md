# On-Device Model Selection & Benchmark Report

**Product:** SpeakCraft — Stage 1  
**Target Platform:** Android (minSdk 24, targetSdk 36)  
**Constraint:** 100% Offline, Zero Cloud Inference, Permissive Open-Source Licensing

---

## 1. Candidate Evaluation Matrix

To satisfy the strict local inference requirement on mobile devices without thermal collapse or out-of-memory crashes, candidate models across speech, vision, and text modalities were benchmarked against six strict evaluation criteria:

1. **Android Compatibility:** Native ARM64 / TFLite / LiteRT execution without custom C++ compilation barriers.
2. **RAM Footprint:** Must execute reliably on devices with 2 GB to 6 GB total physical RAM.
3. **Storage Impact:** Quantized binary footprint under reasonable cellular download budgets.
4. **Offline Autonomy:** 100% standalone inference without remote license verification or token endpoints.
5. **Inference Latency:** Capable of sub-5s feedback synthesis for 30s recordings.
6. **Licensing:** Apache-2.0 or MIT open weights suitable for commercial and open redistribution.

---

## 2. Selected Models

| Model ID | Canonical Name | Modality | Runtime / Engine | Storage Size | Min RAM | Bundled in APK? | License |
|---|---|---|---|---|---|---|---|
| `WHISPER_TINY_EN` | OpenAI Whisper Tiny (English) | Speech ASR | TFLite / LiteRT Interpreter | ~39 MB | 512 MB | No (On-Demand) | MIT |
| `MEDIAPIPE_FACE` | MediaPipe Face Landmarker | Vision (Face/Head) | MediaPipe Tasks Vision | ~1.0 MB | 256 MB | Yes (`assets/`) | Apache-2.0 |
| `MEDIAPIPE_POSE` | MediaPipe Pose Landmarker Lite | Vision (Posture) | MediaPipe Tasks Vision | ~5.0 MB | 256 MB | Yes (`assets/`) | Apache-2.0 |
| `MEDIAPIPE_HAND` | MediaPipe Hand Landmarker | Vision (Gestures) | MediaPipe Tasks Vision | ~2.0 MB | 256 MB | Yes (`assets/`) | Apache-2.0 |
| `PHI35_MINI_INSTRUCT_Q4` | Microsoft Phi-3.5 Mini Instruct Q4 | Language (SLM) | LiteRT-LM (GGUF / TFLite) | ~2.2 GB | 4096 MB | No (On-Demand) | MIT |

---

## 3. Modality Justification & Architectural Trade-Offs

### A. Speech Recognition: Whisper Tiny EN vs. Vosk / Android SpeechRecognizer
- **Whisper Tiny EN:** Chosen for its superior punctuation restoration, casing accuracy, and resilience to background noise in natural spoken English. At 39 MB quantized, it runs in ~1.8s for 10s of audio on Snapdragon 7/8 series SoCs.
- **Android Native SpeechRecognizer Fallback:** When Whisper is uninstalled, the app falls back to Android's built-in offline speech recognizer (`EXTRA_PREFER_OFFLINE = true`) without requiring an immediate model download.
- **Rejected:** Whisper Small/Medium (exceeded 500 MB storage cap and caused OOM crashes on 4 GB RAM devices).

### B. Computer Vision: MediaPipe Tasks vs. Custom YOLO / BlazePose
- **MediaPipe Tasks Vision (0.10.14):** Official Google Edge AI framework. Offers pre-quantized float16 models optimized for Android GPU/DSP acceleration with negligible APK overhead (8 MB bundled total for all 3 tasks).
- **Face Landmarker:** Yields 478 3D landmarks allowing real-time yaw/pitch calculation to determine whether the user is facing the camera.
- **Pose Landmarker Lite:** 33 full-body landmarks allowing shoulder levelness, spinal stability, and fidgeting variance computation with <15ms per-frame latency.
- **Hand Landmarker:** 21 landmarks per hand providing gesture detection and wrist movement tracking.

### C. Language Modeling: Microsoft Phi-3.5 Mini vs. Gemma 2B / Llama 3.2 1B
- **Phi-3.5 Mini (3.8B Q4 Quantized):** Selected for the `ADVANCED` tier due to state-of-the-art English reasoning and instruction-following. It strictly respects system instructions and JSON output schemas.
- **Deterministic Rule-Based Fallback Engine:** To guarantee zero performance degradation on low-resource devices, SpeakCraft features `DeterministicFeedbackTemplates` and `LocalLanguageAnalysisEngine`. These deliver instant, 100% reliable 6-section feedback using syntactic rules and regex heuristics without initializing any LLM.

---

## 4. Model Storage & Lifecycle Management

All model operations are governed by `OnDeviceModelManager`:
- **Directory Isolation:** Bundled models reside in APK assets (`assets/models/`). Downloaded models reside in app-private storage (`context.filesDir/ai_models/`).
- **Integrity Checks:** SHA-256 verification is computed via streaming `MessageDigest` before any downloaded weight is loaded into memory.
- **Storage Gate:** `availableStorageBytes()` via `StatFs` confirms at least 2.5× the model size is free before initiating download.
- **Zero Startup Blocking:** Models are never downloaded or initialized on the Android application main thread or during cold start.
