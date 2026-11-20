# ADR-009 — Speech capture and recognition strategy

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval**; **must be validated by device spike SPK-01 before R1 build** |
| Date | 2026-11-20 |
| Owner (role) | AI/ML Engineer |
| Related | ARC-05 §3–4, FS-04, FS-12, NFR-PERF-004…006, RD-03 FND-AI-01…04, FND-AI-07, FND-AI-13, RSK-01, RSK-02 |

## Context
- By default the app produces **no transcript** (FND-AI-01). File transcription is stubbed (FND-AI-02). The Whisper/Moonshine TFLite path is fabricated (FND-AI-03). Live transcription uses `SpeechRecognizer`, captures one utterance, duplicates text on stop, competes with `MediaRecorder` for the microphone, and may silently use the network recogniser (FND-AI-04, FND-SEC-07).
- Audio is recorded as AAC and decoded into boxed `ArrayList<Short>` (OOM risk, FND-AI-13).
- minSdk is 24; on-device recognition APIs exist from API 31 (`createOnDeviceSpeechRecognizer`, `isOnDeviceRecognitionAvailable`); API 33 adds `EXTRA_AUDIO_SOURCE` (feed app-captured audio to the recogniser), `EXTRA_SEGMENTED_SESSION` (continuous recognition), `checkRecognitionSupport` and `triggerModelDownload` (language-pack management).

## Problem
The core loop needs a reliable transcript **and** the audio signal (for pace/pause analysis and replay) for attempts of up to ~2 minutes, on-device by default, across a wide device range.

## Options
| Option | Summary | Assessment |
|---|---|---|
| **A. Platform `SpeechRecognizer`, tiered by device capability; single microphone owner** | See tiers below | Uses existing stack; no model download by the app; quality depends on the device's recogniser |
| B. Bundle a real Whisper-class model on TFLite | Implement log-mel, tokenizer and decoder correctly; ship or download a 40–75 MB model | TFLite is already in the stack; consistent behaviour across API levels; but large effort, app size, and latency on mid-range devices are unproven |
| C. Another offline ASR library | — | New dependency; not preferred |
| D. Cloud ASR | — | Violates NG-01 / PP-2 |

## Decision (proposed): Option A, with three capability tiers

| Tier | Condition | Capture & recognition | Output |
|---|---|---|---|
| **T1 Full on-device** | API ≥ 33 and on-device recogniser supports the selected English locale (`checkRecognitionSupport`) | App owns the mic via `AudioRecord` (16 kHz mono PCM16). PCM is streamed to a WAV file **and** piped to the on-device recogniser via `EXTRA_AUDIO_SOURCE` with `EXTRA_SEGMENTED_SESSION` | Continuous transcript + full acoustic analysis + replay |
| **T2 On-device, recogniser owns mic** | API 31–32 with on-device recogniser, **or** T1 if audio-source piping fails in SPK-01 | On-device recogniser captures; segments stitched by restarting on end-of-segment; acoustic metrics from `onRmsChanged` + segment timing | Transcript + reduced acoustic metrics; replay unavailable (stated in UI) |
| **T3 No on-device recogniser** | API < 31, or no on-device support | (a) If the user consents to **online transcription** (platform speech service; audio sent to the service provider): as T2 with the default recogniser. (b) Otherwise **Audio-only mode**: `AudioRecord` capture with delivery metrics only, clearly labelled | Consented transcript, or delivery-only feedback |

Additional rules:
- The language pack can be installed from Settings where `triggerModelDownload` is available (FS-12).
- Recognition confidence is propagated only when the recogniser supplies it; otherwise *unknown* (never a constant).
- Option B is kept as research spike **SPK-03** (R3): adopt only if word-error rate and latency on reference devices beat T2/T3 on the SPK-01 test set.

## Consequences
- `AudioRecorderManager` (MediaRecorder/AAC) is replaced by a PCM capture service; audio is analysed in streaming chunks (no boxed arrays). Saved recordings use WAV during the attempt and may be transcoded to AAC for retention (ADR-015).
- The four existing recogniser wrappers and five availability sources are replaced by one `SpeechCapabilityService` and one `RecognitionSession` (ARC-05).
- `<queries>` for `android.speech.RecognitionService` is added to the manifest.

## Risks
| Risk | Mitigation |
|---|---|
| `EXTRA_AUDIO_SOURCE` support varies by recogniser implementation | SPK-01 on ≥ 4 devices; T2 fallback |
| Recogniser accuracy for accented B1–B2 speech | SPK-01 with a recorded test set; *Inconclusive* thresholds; ASM-05 |
| Recognisers "correct" mispronunciations | Never present recognition match as pronunciation (SRS-BR-004) |
| Many target devices are T3 | Consented online mode; honest audio-only mode; device matrix in NFR-COMP |

## Approval status
Pending — Product Owner (decisions D-02, D-14 "minimum Android version and tier policy").
