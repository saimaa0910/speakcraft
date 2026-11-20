# Centralized AI & Intelligence Settings

## Single Source of Truth (`AISettingsRepository`)
All AI, speech recognition, live transcription, acoustic DSP, video analysis, and privacy settings are consolidated into **`AISettingsRepository`** (wrapping the single `AppPreferencesRepository` DataStore) and surfaced in **App Settings → AI & Intelligence**.

### Managed Settings (`AISettings`)
| Setting | Default | Scope |
|---|---|---|
| `localAiEnabled` | `true` | Enables deterministic on-device Local Intelligence (grammar, vocabulary, coaching, DSP) |
| `cloudAiEnabled` / `effectiveCloudAiEnabled` | `false` | Locked OFF under Stage 1 Zero-Cloud-AI Policy (`LocalExecutionPolicy.isLocalOnly() == true`) |
| `cloudAiModelTier` | `"balanced"` | Disabled while Cloud AI is unavailable |
| `liveTranscriptionEnabled` | `false` | Master switch for real-time speech transcription during Coach practice |
| `localSpeechRecognitionEnabled` | `true` | Controls on-device ASR transcription |
| `localAudioAnalysisEnabled` | `true` | Controls acoustic DSP (loudness, pitch, VAD, pause cadence) |
| `localVideoAnalysisEnabled` | `true` | Controls optional MediaPipe face/posture/gesture analysis for video drills |
| `selectedRecognitionLanguage` | `"en-US"` | BCP-47 language tag (`en-US`, `en-GB`, `en-AU`, `en-IN`) |
| `selectedRecognitionEngine` | `AUTO` | `AUTO`, `ANDROID_OFFLINE`, or `LOCAL_WHISPER` |

## Stage 1 Zero-Cloud-AI Policy Enforcement
- `AISettingsRepository.setCloudAiEnabled(true)` rejects activation with a clear failure message and persists `false`.
- `HybridAiCoachCoordinator.isCloudAiEnabled()` always returns `false` while `LocalExecutionPolicy.isLocalOnly()` is `true`.
- `SettingsScreen` renders the Cloud AI toggle as **OFF and disabled** with badge `"Unavailable • Zero-Cloud-AI Policy"`.
- **Privacy Guarantee**: Audio, video, and live transcripts remain strictly on-device. Firebase Authentication and Firestore are preserved strictly for account identity and explicit practice progress sync.

## Removal of Scattered Per-Screen AI Toggles & Prompts
Removed all scattered switches, chips, and `"Switch to Local AI"` / `"Use Local AI"` / `"Cloud Gemini"` prompts from:
- `SpeakingLabScreen` & `ChallengesScreen` (`speaking_cloud_toggle_chip` removed; replaced with `"AI Settings"` navigation link)
- `ConversationSimulatorScreen` (`conversation_cloud_toggle_chip` & Gemini Live Voice Call branches removed)
- `AiMentorScreen` (`mentor_cloud_toggle_chip` removed)
- `VocabularyScreen` (`vocabulary_cloud_toggle_chip` removed)
- `YouTubeIntelligenceScreen` (`youtube_cloud_toggle_chip` removed)
- `UniversalActivityScreen` (`activity_cloud_toggle_chip` removed)
- `InlineAiMentorCard` in `DashboardStudioWidgets` (read-only `"Local AI"` badge)
