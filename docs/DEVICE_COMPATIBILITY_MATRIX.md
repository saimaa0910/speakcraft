# Device Compatibility Matrix

**Product:** SpeakCraft — Stage 1  
**Target Platform:** Android  
**Platform Constraints:** `minSdk = 24`, `targetSdk = 36`

---

## 1. Capability Profile Tiers

SpeakCraft dynamically assesses each device's hardware resources via `DeviceCapabilityManager` and categorizes the phone into one of three execution tiers:

| Tier | Hardware Requirements | Enabled Capabilities | Fallback Behavior |
|---|---|---|---|
| **BASIC** | < 2048 MB RAM **OR** API Level < 24 | Deterministic rubric scoring, text metrics (WPM, pause count, filler counts), manual transcript editing, video self-review checklist, Android TTS voice playback. | Fully functional; no neural inference attempted. Honest notice informs user of basic mode. |
| **STANDARD** | 2048 MB – 4095 MB RAM **AND** API Level $\ge 24$ | All Basic features **plus** Whisper Tiny EN speech recognition, pure Kotlin acoustic pitch & VAD analysis, MediaPipe Face Landmarker, MediaPipe Pose Landmarker, and MediaPipe Hand Landmarker. | Local SLM disabled; uses `DeterministicFeedbackTemplates` for 6-section coaching. |
| **ADVANCED** | $\ge 4096$ MB RAM **AND** API Level $\ge 30$ | All Standard features **plus** Microsoft Phi-3.5 Mini Instruct local SLM inference via LiteRT-LM, deep multimodal fusion, and adaptive conversational roleplay. | Falls back to Standard mode if Phi-3.5 model is uninstalled. |

---

## 2. Hardware Resource & API Requirements

| Feature | Min API Level | Min RAM | Storage Required | Recommended Acceleration |
|---|---|---|---|---|
| Core App & Rubrics | API 24 (Android 7.0) | 512 MB | ~45 MB (base APK) | CPU |
| MediaPipe Vision (Face/Pose/Hand) | API 24 (Android 7.0) | 256 MB free | ~8.0 MB (in `assets/`) | GPU / NNAPI / OpenCL |
| Audio Signal Processing (YIN/RMS) | API 24 (Android 7.0) | 128 MB free | 0 MB (pure Kotlin) | CPU |
| Whisper Tiny EN ASR | API 24 (Android 7.0) | 512 MB free | ~39 MB | NNAPI / XNNPACK |
| Microsoft Phi-3.5 Mini SLM | API 30 (Android 11) | 4096 MB total | ~2.2 GB | GPU (Vulkan) / NPU |

---

## 3. Platform Constraint Verification (`minSdk = 24`)

- **Constraint:** The application `minSdk` is 24 and must not be raised.
- **Verification:**
  - All MediaPipe Tasks Vision dependencies in `0.10.14` support API 24+.
  - `MediaCodec` and `MediaExtractor` usage in `AudioResampler` rely strictly on API 21+ methods.
  - `PowerManager.currentThermalStatus` is safely guarded with `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)`.
  - Battery intent inspection uses standard BroadcastReceiver sticky intents compatible with API 1+.
  - Zero higher-level API methods are called without explicit `Build.VERSION.SDK_INT` guards.
