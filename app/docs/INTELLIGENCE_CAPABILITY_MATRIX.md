# Intelligence Capability Matrix

`IntelligenceCapabilityManager` computes an authoritative, real-time `IntelligenceCapabilityMatrix` by combining `AISettingsRepository.aiSettingsFlow`, runtime permissions (`RECORD_AUDIO`, `CAMERA`), Android SpeechRecognizer availability, installed on-device ASR models (`OnDeviceModelManager`), MediaPipe availability (`OfflineAIAvailabilityManager`), and recognizer initialization errors.

## Honest Capability Statuses (`CapabilityStatus`)
- `ENABLED` — User enabled the feature AND all required permissions, engines, and models are verified ready.
- `AVAILABLE` — Capability is ready for on-demand use (e.g., Video Intelligence when camera permission & MediaPipe tasks are ready).
- `DISABLED` — Explicitly turned off by the user in Settings.
- `UNAVAILABLE` — Blocked by policy (e.g., Cloud AI under Stage 1 Zero-Cloud-AI Policy) or missing platform service.
- `PERMISSION_REQUIRED` — Feature is enabled in Settings, but `RECORD_AUDIO` or `CAMERA` has not been granted.
- `MODEL_REQUIRED` — Selected local ASR model (`LOCAL_WHISPER`) or bundled vision task is not installed/ready.
- `INITIALIZATION_FAILED` — Speech recognizer failed at runtime initialization.

## Capability Evaluation Rules
| Capability | Offline Capable | Conditions for `ENABLED` / `AVAILABLE` |
|---|---|---|
| **Local Intelligence** | Yes | `settings.localAiEnabled == true` (works without any LLM) |
| **Cloud AI Coach** | No | Always `UNAVAILABLE` while `LocalExecutionPolicy.isLocalOnly() == true` |
| **Live Transcription** | Device-dependent (`offlineSpeechAvailable`) | `liveTranscriptionEnabled == true` AND `RECORD_AUDIO` granted AND recognizer available AND no init failure AND (if `LOCAL_WHISPER`, local ASR model installed) |
| **On-Device Speech Recognition** | Yes when verified | `localSpeechRecognitionEnabled == true` AND (`whisper`/`moonshine` installed OR Android API 31+ `isOnDeviceRecognitionAvailable`) |
| **Local Audio Analysis (DSP)** | Yes | `localAudioAnalysisEnabled == true` AND `localAiEnabled == true` |
| **Video Intelligence (Camera/Pose)** | Yes | `localVideoAnalysisEnabled == true` AND `CAMERA` granted AND bundled MediaPipe tasks ready |

## Diagnostics Displayed in App Settings → AI & Intelligence
- **Recognition Engine**: Active resolved engine label (`Android On-Device SpeechRecognizer`, `Local Whisper Tiny INT8`, etc.)
- **Offline Availability**: `Verified Offline` vs `Unavailable / Unverified`
- **Microphone Permission**: `Granted` vs `Required (Grant in Recording)`
- **Recognition Status**: Exact `CapabilityStatus` + diagnostic reason
- **Model Availability**: Whisper/Moonshine pack installation state
- **Camera & MediaPipe Status**: Camera permission + bundled MediaPipe landmarker readiness
