# SpeakCraft Coach Evaluation — Root Cause Analysis (78/100 False Score Bug)

## Summary of the Defect
Submitting a Coach or Speaking Lab session without recording speech—or after recording silence, a 0-second clip, or an unreadable audio file—still produced a plausible composite score (commonly ~74–82/100, e.g., **78/100**) and persisted a `COMPLETED` session into Firestore and local history.

## Root Causes Identified Across the Pipeline

1. **Missing Pre-Evaluation Evidence Gate (`LocalIntelligenceOrchestrator` & `SpeakingAndLabsViewModel`)**
   - `SpeakingAndLabsViewModel.submitForAiAnalysis` permitted submissions with empty `mediaBytes`, stale `liveTranscript` values from prior sessions, or unverified files.
   - `LocalIntelligenceOrchestrator.analyzeSpeechRecording` proceeded straight into multimodal fusion and scoring even when `decodedAudio == null`, `rmsEnergy == 0f`, or `durationSeconds == 0`.
2. **Baseline / Floor Score Fabrication (`MultimodalFeatureFusionEngine` & `UniversalEvaluationEngine`)**
   - Scoring formulas initialized dimensions with generous baseline constants (e.g., `78` grammar baseline, `72` fluency baseline, `35` floor in `UniversalEvaluationEngine`) and awarded default points even when no voiced audio or valid transcript existed.
   - Missing dimensions received default integers rather than `null`, causing average aggregations to output ~78/100 on empty inputs.
3. **Synthetic Duration Coercion (`ActivityEngine` & `ActivityEngineViewModel`)**
   - Duration values were coerced via `.coerceAtLeast(10)` and `.coerceAtLeast(15)`, fabricating non-zero speaking durations for 0-second submissions.
4. **Silent Failure-to-Success Masking**
   - Audio decoding failures were swallowed into `null`, allowing fallback heuristic paths to return `Result.success(SpeakingSession(...))` with `evaluationStatus = "COMPLETED"`.
   - `AppNavigation` emitted a "Session Completed" notification alert for any newly inserted session regardless of whether evidence validation actually passed.

## Architectural Fix Implemented
- Added **`EvaluationModels.kt`** with an explicit 12-state lifecycle (`CoachEvaluationState`), nullable dimension scores (`score: Int?`), per-dimension provenance (`evidenceSource`, `confidence`, `availabilityStatus`, `evaluationTimestampMs`), and `InsufficientSpeechEvidenceException`.
- Added **`EvaluationEvidenceValidator.kt`** enforcing non-empty audio, readable PCM decoding, duration >= `1.5s`, RMS speech energy >= `0.008f`, voiced duration >= `0.5s`, optional video validation ONLY when `videoAssessmentSelected == true`, and session-scoped transcript validation (`transcriptSessionId == sessionId`).
- Added **`EvidenceBasedScoringEngine.kt`** with a fail-closed gate, zero fabricated baselines, nullable scores for missing/failed dimensions, and transparent partial-evaluation labeling without redistributing missing weights.
- Updated **`SpeakingAndLabsViewModel`**, **`LocalIntelligenceOrchestrator`**, **`UniversalEvaluationEngine`**, **`ActivityEngine`**, **`ActivityEngineViewModel`**, **`SpeakCraftRepository`**, and **`AppNavigation`** so failed or insufficient sessions are never scored, never saved as completed progress, and never trigger false completion alerts.
