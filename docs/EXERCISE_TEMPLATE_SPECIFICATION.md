# Exercise Template Specification (25 Templates across 7 Reusable Families)

Rather than building 150 separate screens, SpeakCraft maps all **25 `ExerciseTemplateType` values** to **7 reusable Compose interaction families** rendered inside a single `ActivityScreen(activityId)` via `ExerciseTemplateInteractionPanel` (`ExerciseTemplateRenderers.kt`).

## 1. Universal Renderer Lifecycle (`ActivityScreen`)
1. **Load & Validate**: `ActivityEngine.loadActivity(activityId)` runs `ActivityValidator.validate(...)`. Any missing or malformed configuration renders a safe `invalid_activity_card` recovery screen (zero crashes).
2. **Stage 1 — Ready**: Shows module badge, title, description, skill/difficulty/duration/template chips, bookmark toggle, learning objective, step-by-step instructions, example model(s), and **Start Exercise** button.
3. **Stage 2 — Active**: Starts live elapsed timer (`MM:SS`), dynamically mounts the appropriate interaction family panel, optional separate **Self-Confidence Slider**, validation feedback banner, and **Submit for Evaluation**.
4. **Stage 3 — Evaluating**: Displays active named evaluator (e.g., `Running PronunciationEvaluator...`) while calling Firebase AI Logic with 60s timeout & fallback.
5. **Stage 4 — Completed**: Renders all **6 structured feedback sections**, objective rubric score + per-dimension breakdown, honest caveat banner, separate confidence self-rating badge, **Retry**, and **Continue Next** personalized recommendation buttons.

## 2. All 25 Templates Mapped to 7 Interaction Families
| # | `ExerciseTemplateType` | `InteractionFamily` | Core Capabilities |
|---|---|---|---|
| 1 | `SPEAKING_EXERCISE` | `SPEAKING_AND_AUDIO` | Prompt card, live mic audio capture/playback, transcript/notes input, WPM/filler tracking |
| 2 | `AUDIO_RECORDING` | `SPEAKING_AND_AUDIO` | Dedicated audio capture, amplitude meter, self-playback review, reflection notes |
| 3 | `VIDEO_RECORDING` | `VIDEO_AND_BODY_LANGUAGE` | Runtime `CAMERA` + `RECORD_AUDIO` flow, embedded `CameraVideoRecorderSection`, checklist & notes |
| 4 | `LISTENING_COMPREHENSION` | `LISTENING_AND_GAME` | `TtsAndSpeechHelper` audio playback of reference script, optional reveal transcript, response summary |
| 5 | `PRONUNCIATION_DRILL` | `SPEAKING_AND_AUDIO` | Slow-to-fast drill cue, optional TTS model playback, mic recording + acoustic honesty notice |
| 6 | `VOCABULARY_EXERCISE` | `STRUCTURED_TEXT_AND_QA` | Target framing prompt, precision word-choice text composer, word-count tracking |
| 7 | `GRAMMAR_EXERCISE` | `STRUCTURED_TEXT_AND_QA` | Sentence editing / clarity rewrite composer with concrete grammar rubric |
| 8 | `READING_ALOUD` | `SPEAKING_AND_AUDIO` | Reference passage display, audio recording for expressive phrasing & cadence |
| 9 | `IMPROMPTU_SPEAKING` | `SPEAKING_AND_AUDIO` | Timed spontaneous prompt with zero-prep structure cues + audio/text response |
| 10 | `CONVERSATION_SIMULATION` | `DIALOGUE_AND_ROLEPLAY` | Multi-turn interactive AI partner chat (`GeminiCoachService` + offline fallback) + full transcript evaluation |
| 11 | `ROLEPLAY` | `DIALOGUE_AND_ROLEPLAY` | Persona-assigned scenario opening, back-and-forth user/partner turns, diplomacy evaluation |
| 12 | `STORYTELLING` | `SPEAKING_AND_AUDIO` | Narrative arc cue checklist + spoken/written story capture |
| 13 | `QUESTION_AND_ANSWER` | `STRUCTURED_TEXT_AND_QA` | Structured answer box (PREP/CCC cues) + response analysis |
| 14 | `SENTENCE_CONSTRUCTION` | `STRUCTURED_TEXT_AND_QA` | Clause/sentence builder input evaluated for completeness & clarity |
| 15 | `WORD_ASSOCIATION` | `STRUCTURED_TEXT_AND_QA` | Rapid verbal/written bridge builder connecting random anchor concepts |
| 16 | `VOICE_MODULATION` | `SPEAKING_AND_AUDIO` | Volume/pitch/melody/pause drill cues + mic capture + self-review |
| 17 | `BREATHING_VOCAL_WARMUP` | `GUIDED_CHECKLIST_WARMUP` | Interactive step-by-step warmup checklist (`requiresChecklistCompletion`) + physical sensation notes |
| 18 | `TONGUE_TWISTERS` | `SPEAKING_AND_AUDIO` | Crisp articulation drill line + multi-round audio capture |
| 19 | `BODY_LANGUAGE_SELF_REVIEW` | `VIDEO_AND_BODY_LANGUAGE` | Camera video capture + posture/eye-contact/gesture self-audit checklist |
| 20 | `PRESENTATION_PRACTICE` | `SPEAKING_AND_AUDIO` | Structured slide/talk cue card + spoken pitch recording & executive rubric |
| 21 | `INTERVIEW_SIMULATION` | `DIALOGUE_AND_ROLEPLAY` | Interviewer persona opening, behavioral/technical Q&A turns, STAR evaluation |
| 22 | `ACTIVE_LISTENING_GAME` | `LISTENING_AND_GAME` | TTS-spoken prompt lines + interactive game response (Last Word Last Letter / Follow-Up game) |
| 23 | `REAL_WORLD_TASK` | `REAL_WORLD_AND_REFLECTION` | Field mission checklist + post-interaction outcome log |
| 24 | `REFLECTION_SELF_ASSESSMENT` | `REAL_WORLD_AND_REFLECTION` | Guided reflection prompts + explicit separation of confidence slider from objective reflection rubric |
| 25 | `AI_EVALUATED_CHALLENGE` | `AI_CHALLENGE` | High-bar timed capstone combining audio/text capture, strict rubric weights, and deep AI coaching |

## 3. Permissions & Accessibility Compliance
- Runtime permission requests (`Manifest.permission.RECORD_AUDIO`, `Manifest.permission.CAMERA`) are requested lazily via `rememberLauncherForActivityResult` ONLY when the active template requires microphone or camera access.
- Every interactive control enforces `minHeight = 48.dp`, `contentDescription`, semantic `testTag`, and theme-token styling (`MaterialTheme.colorScheme` / `MaterialTheme.speakCraftColors`) across Light and Dark modes.
