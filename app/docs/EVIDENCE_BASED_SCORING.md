# Evidence-Based Scoring Policy

## Explicit 12-State Evaluation Lifecycle (`CoachEvaluationState`)
1. `NOT_STARTED`
2. `WAITING_FOR_AUDIO`
3. `WAITING_FOR_VIDEO`
4. `RECORDING`
5. `PROCESSING`
6. `VALIDATING_EVIDENCE`
7. `EVALUATING`
8. `COMPLETED` — only reachable after `EvaluationEvidenceValidator.validate()` succeeds and at least one dimension evaluates with verified evidence.
9. `INSUFFICIENT_EVIDENCE`
10. `RECORDING_FAILED`
11. `ANALYSIS_FAILED`
12. `CANCELLED`

## Evidence Thresholds (`EvaluationEvidenceValidator`)
| Modality | Required When | Validation Criteria | Failure State |
|---|---|---|---|
| **Audio** | `audioRequired == true` | File or bytes exist, length > 0, PCM decode succeeds, duration >= `1.5s`, RMS energy >= `0.008f`, voiced speech >= `0.5s` | `INSUFFICIENT_EVIDENCE` (missing/empty/short/silent) or `ANALYSIS_FAILED` (corrupt/decode error) |
| **Video** | `videoAssessmentSelected == true` ONLY | Video exists, frames extracted > 0, MediaPipe analysis completed without error | Partial (`video_presence = null`) or `ANALYSIS_FAILED` if video-only |
| **Transcript** | Optional for audio-only; required when `transcriptRequired == true` | Non-blank, `transcriptSessionId == sessionId` (rejects stale text), >= 2 meaningful words | `grammar` & `vocabulary` set to `null` (`UNAVAILABLE_MISSING_EVIDENCE`) |

Audio-only and voice-only drills **never** require video frames (`videoRequired == false`).

## Scoring & Aggregation Rules (`EvidenceBasedScoringEngine`)
- **Zero Baseline Points on Missing Evidence**: If `isValidForEvaluation == false`, `overallScore = null` and every dimension has `score = null`.
- **Per-Dimension Metadata**: Every `EvaluationDimensionDetail` records `key`, `label`, `score: Int?`, `evidenceSource`, `confidence`, `availabilityStatus` (`EVALUATED`, `UNAVAILABLE_MISSING_EVIDENCE`, `UNAVAILABLE_ANALYSIS_FAILED`, `NOT_REQUIRED`), `evaluationTimestampMs`, and `note`.
- **No Silent Weight Redistribution**: When only a subset of dimensions has verified evidence (e.g., audio DSP succeeded while transcription or video frames were unavailable), `overallScore` averages only the evaluated dimensions, `isPartialEvaluation = true`, and `partialReason` explicitly lists unavailable dimensions and states that partial scores must not be compared directly to complete multimodal scores.
- **Failed Inference Protection**: When linguistic or visual inference fails (`languageInferenceFailed = true` or `videoAnalysisFailed = true`), affected dimensions receive `score = null` and `DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED`.

## User-Facing Recovery Messaging (`CoachEvaluationStatusCard`)
- **Missing audio**: `"Your speaking recording is missing."` / `"Please record your response before requesting feedback."` + **Record Again** & **Cancel** actions.
- **Silent audio**: `"We couldn't detect speech in this recording."` / `"Try speaking clearly for a few seconds and submit again."`
- **Audio processing failure**: `"We couldn't analyze this recording. Your response has not been scored."`
