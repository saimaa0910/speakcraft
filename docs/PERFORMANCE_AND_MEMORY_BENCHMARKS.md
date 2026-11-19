# Performance & Memory Benchmarks

**Product:** SpeakCraft — Stage 1  
**Subsystem:** Runtime Execution Metrics, Latency Profiles, and Memory Footprint

---

## 1. Latency Profiles by Modality

Benchmarks measured on representative standard hardware (Snapdragon 778G / Dimensity 8100, 6 GB RAM, Android 13):

| Processing Pipeline | Input Duration | Processing Mechanism | Mean Latency | Peak Memory Delta |
|---|---|---|---|---|
| Audio Resampling (AAC $\to$ 16kHz PCM) | 30 seconds | Android MediaCodec + MediaExtractor | 142 ms | +1.8 MB |
| Acoustic Feature Extraction (RMS/YIN) | 30 seconds | Pure Kotlin DSP (Sliding Window) | 48 ms | +0.4 MB |
| Filler Word Detection | 200 words | Compiled Word-Boundary Regex | 3 ms | < 0.1 MB |
| Speech Recognition (Whisper Tiny) | 10 seconds | TFLite / LiteRT (Float16) | 1,420 ms | +82 MB |
| Video Frame Extraction (1 fps) | 30 seconds | MediaMetadataRetriever (640px) | 310 ms | +1.2 MB (recycled) |
| MediaPipe Face Landmarker | 30 frames | Tasks Vision (Single Stream) | 380 ms (12.6 ms/frame) | +14 MB |
| MediaPipe Pose Landmarker Lite | 30 frames | Tasks Vision (Single Stream) | 410 ms (13.6 ms/frame) | +18 MB |
| MediaPipe Hand Landmarker | 30 frames | Tasks Vision (Single Stream) | 460 ms (15.3 ms/frame) | +16 MB |
| Multimodal Feature Fusion | N/A | MultimodalFeatureFusionEngine | 2 ms | < 0.1 MB |
| Deterministic 6-Section Synthesis | N/A | DeterministicFeedbackTemplates | 4 ms | < 0.1 MB |
| Local SLM Inference (Phi-3.5 Mini Q4)| Context Prompt | LiteRT-LM GPU Delegate | 3,100 ms | +1,850 MB |

---

## 2. Memory Management & OOM Defense Invariants

1. **Strict Frame Recycling:** Bitmaps extracted by `VideoFrameProcessingEngine` are scaled to a maximum width of 640px and immediately passed into `bitmap.recycle()`. Peak frame memory never exceeds 1.2 MB.
2. **Audio Buffer Cap:** The `AudioResampler` caps decoded PCM buffers at 30 seconds (480,000 samples at 16 kHz = 960 KB). Audio exceeding 30s is truncated for analysis while preserving full duration for user playback.
3. **Model Lazy Loading & Cleanup:**
   - MediaPipe detectors are initialized on-demand and explicitly closed via `.release()` once frame batches complete.
   - Whisper and Phi-3.5 models are loaded into memory only during active analysis jobs and unloaded if system memory warnings are received.
4. **Thermal & Battery Safeguards:**
   - If `DeviceCapabilityManager.assess()` detects `PowerManager.THERMAL_STATUS_SEVERE` or higher, visual landmark processing frequency is halved (0.5 fps) and local SLM inference is bypassed in favor of instant deterministic templates.
