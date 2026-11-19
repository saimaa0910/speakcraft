# SpeakCraft Universal AI Evaluation Architecture

## 1. Design Principles
- **Firebase AI Logic First**: Uses `Firebase.ai.generativeModel("gemini-3.8-flash")` with strict structured JSON output (`Schema.obj(...)`)—zero hardcoded API keys and zero client-side key exposure.
- **Resilience & Coroutine Hygiene**: All AI calls execute inside `withTimeout(60_000L)`, preserve coroutine cancellation by rethrowing `CancellationException` (except `TimeoutCancellationException`), and fall back gracefully to deterministic local rubric scoring when offline or timed out.
- **Strict Honesty Guarantee**: Evaluators never invent quotes, never fabricate laboratory phoneme accuracy percentages without acoustic verification, and always attach explicit `honestyNotice` caveats when evaluating text-only or limited-audio submissions.
- **Strict Objective vs. Confidence Separation**: `confidenceSelfRating` (`1..100`) is stored independently on `ActivityEvaluationResult`, `ActivityAttemptRecord`, and `UserActivityProgress` and **never modifies `overallObjectiveScore`**.

## 2. The 9 Named Strategy Evaluators (`UniversalEvaluationEngine.kt`)
| # | Evaluator Class | `EvaluationStrategyType` | Primary Rubric & Coaching Focus |
|---|---|---|---|
| 1 | `SpeakingEvaluator` | `SPEAKING` | Fluency, pacing (WPM), natural phrasing, filler control, complete thoughts |
| 2 | `PronunciationEvaluator` | `PRONUNCIATION` | Intelligibility, stress placement, consonant/vowel clarity, explicit anti-fabrication caveat |
| 3 | `GrammarEvaluator` | `GRAMMAR` | Active voice, tense consistency, concise structure, spoken English clarity |
| 4 | `VocabularyEvaluator` | `VOCABULARY` | Word precision, everyday simplicity vs. jargon, collocations, sensory imagery |
| 5 | `ListeningEvaluator` | `LISTENING` | Grounded strictly against `listeningScript` recall, subtext, and reflective paraphrasing |
| 6 | `ConversationEvaluator` | `CONVERSATION` | Open-ended curiosity, follow-up thread depth, warmth, reciprocity, diplomacy |
| 7 | `StorytellingEvaluator` | `STORYTELLING` | Setup/conflict/turn/lesson arc, VAKS sensory cues, suspense pauses, audience resonance |
| 8 | `PresentationEvaluator` | `PRESENTATION` | Strong hook, single core message (CCC/BLUF), executive authority, memorable close |
| 9 | `ReflectionEvaluator` | `REFLECTION` | Depth of self-awareness, specific habit observations, actionable next real-world shift |

## 3. Local Pre-Analysis (`LocalResponseAnalyzer`)
Before invoking Gemini (and during offline fallback), SpeakCraft computes deterministic local telemetry:
- `wordCount` — Tokenized alphanumeric words across transcript, written response, and user dialogue turns
- `wordsPerMinute` — Computed when `durationSeconds >= 5` and `wordCount > 0` (clamped `30..320`)
- `fillerCount` & `detectedFillers` — Regex scan for `"um"`, `"uh"`, `"like"`, `"basically"`, `"actually"`, `"you know"`, `"sort of"`, `"kind of"`

## 4. Mandatory 6-Section Structured Feedback Schema
Every evaluation—whether returned by Gemini `Schema.obj` or the deterministic rubric fallback—populates all 6 learner-facing sections in `ActivityEvaluationResult`:
1. `whatWentWell` — 2–3 observed strengths grounded in the actual submission
2. `specificMistakes` — Concrete mistakes, omissions, or filler/pacing habits
3. `correctedExamples` — Sharper rewritten phrasing or stronger delivery alternatives
4. `explanation` — Coaching rationale explaining why the adjustment increases clarity/impact
5. `improvementSuggestions` — 2–3 immediately actionable practice habits
6. `nextPracticeTask` — Targeted single next drill focus

## 5. Multimodal Support & Testability
- When audio/video bytes (`<= 15 MB`) and MIME type are attached on `ActivityUserResponse`, `UniversalEvaluationEngine` sends `content { inlineData(bytes, mimeType); text(prompt) }`.
- Constructor supports `aiClientOverride` hook so JVM unit tests and Robolectric Compose UI tests verify all 9 evaluators, 6 feedback sections, honesty caveats, and objective/confidence separation deterministically without live network dependency.
