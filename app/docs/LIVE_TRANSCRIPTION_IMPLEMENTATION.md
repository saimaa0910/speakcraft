# Live Transcription Implementation

## Architecture
SpeakCraft uses a single centralized live transcription pipeline owned by `LiveTranscriptionManager` (`com.example.data.ai.local.audio.LiveTranscriptionManager`), controlled exclusively by `AISettings.liveTranscriptionEnabled` in **App Settings → AI & Intelligence → Speech & Transcription**.

### State Machine (`StateFlow<LiveTranscriptionState>`)
- `DISABLED` — User toggle is off in centralized AI settings.
- `IDLE` — Enabled, microphone permission granted, and speech recognizer available; ready to start on recording.
- `INITIALIZING` — Engine requested to start listening.
- `LISTENING` — `RecognitionListener.onReadyForSpeech` confirmed active microphone capture.
- `PARTIAL_RESULT` — Streaming interim words emitted via `onPartialResults`.
- `FINAL_RESULT` — Confirmed recognized utterances accumulated across segments via `onResults` (empty results transition to `ERROR`, never fake success).
- `ERROR` — Recoverable recognition error (timeout, no match, audio busy, language pack missing, init exception) with **Retry Recognition** action.
- `UNAVAILABLE` — `RECORD_AUDIO` denied or no speech recognition engine available on device.

### Session Isolation & Resource Cleanup
- Every recording attempt generates a fresh `sessionId` via `prepareNewSession(sessionId, settings)`.
- Late callbacks from a previous `RecognitionListener` are dropped whenever `activeSessionId != listenerSessionId`.
- `stopTranscription()` promotes any active partial transcript to finalized text and stops the recognizer.
- `SpeakingAndLabsViewModel.onCleared()` releases `LiveTranscriptionManager` resources to prevent leaks.

### Honest Offline & Permission Reporting
- `AndroidLiveRecognizerBackend.isOfflineRecognitionAvailable()` returns `SpeechRecognizer.isOnDeviceRecognitionAvailable(context)` on Android 12+ (API 31+) and `false` on earlier versions—never falsely claiming offline verification.
- On `startTranscription()`, missing `RECORD_AUDIO` permission sets `UNAVAILABLE` with an explicit permission notice.
- Initialization failures are reported to `IntelligenceCapabilityManager.recordRecognizerInitFailure()` so Settings diagnostics reflect real runtime status.

### Coach UI Integration (`LiveTranscriptPanel`)
Integrated across `SpeakingLabScreen`, `ChallengesScreen`, `PronunciationLabScreen`, `ListeningShadowingScreen`, `GrammarSpeakingScreen`, and `PublicSpeakingScreen`:
- Displays live partial and finalized words in an auto-scrolling transcript panel.
- Shows active listening indicator, engine label, language tag, and honest offline badge (`Offline Ready` vs `Device ASR`).
- Displays **Retry Recognition** on errors and **Open AI & Transcription Settings** when disabled.
- Preserves finalized transcript after recording and permits editing only where the screen's evaluation workflow allows editable transcript notes.
