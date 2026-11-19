# Nine Local Evaluators Implementation

**Product:** SpeakCraft — Stage 1  
**Subsystem:** Universal Evaluation Engine & Activity Strategy Registry  
**Location:** `com.example.data.activity.UniversalEvaluationEngine`

---

## 1. Architectural Migration Summary

All nine evaluation strategies originally routed through remote Gemini models (`gemini-3.8-flash`) have been fully migrated to on-device deterministic and local multimodal evaluation.

The strategy interface and class names have been preserved exactly:

```kotlin
interface ActivityEvaluatorStrategy {
    val strategyType: EvaluationStrategyType
    val evaluatorName: String get() = strategyType.evaluatorName
    fun buildEvaluatorInstructions(activity: ActivityDefinition, response: ActivityUserResponse): String
    fun honestyCaveat(activity: ActivityDefinition, response: ActivityUserResponse): String
}
```

---

## 2. Strategy Implementations

### 1. SpeakingEvaluator
- **Focus:** Fluency, pacing, fillers, pauses, completeness.
- **Local Implementation:** Runs `AudioFeatureExtractionEngine` and `FillerWordDetectionEngine` on audio samples/transcript. Computes WPM, pauses, and speech-to-silence ratio. Evaluates whether pace falls within optimal conversational zones (120–170 WPM).
- **Caveat:** Notes when evaluated from written text versus live media recording.

### 2. PronunciationEvaluator
- **Focus:** Intelligibility, word recognition accuracy, stress patterns, consonant crispness.
- **Local Implementation:** Uses `PronunciationAnalysisEngine` to perform Levenshtein word alignment against the reference text. Flags omitted or substituted words.
- **Honesty Rule:** Never claims phoneme-level accuracy without a validated on-device acoustic model. Transparently explains transcript-level alignment bounds.

### 3. GrammarEvaluator
- **Focus:** Sentence structure, tense consistency, subject-verb agreement, active voice.
- **Local Implementation:** Uses `LocalLanguageAnalysisEngine` rule heuristics (agreement, double negatives, articles, run-ons) combined with local feedback synthesis.

### 4. VocabularyEvaluator
- **Focus:** Target word usage, word repetition, lexical diversity, collocations.
- **Local Implementation:** Calculates lexical diversity score, identifies overused crutch expressions, and checks presence of required exercise keywords.

### 5. ListeningEvaluator
- **Focus:** Factual recall, active listening paraphrasing, subtext awareness.
- **Local Implementation:** Compares user response strictly against the curriculum activity's `listeningScript` using keyword density and semantic token overlap.

### 6. ConversationEvaluator
- **Focus:** Open-ended follow-ups, curiosity, warmth, reciprocity, conversational flow.
- **Local Implementation:** Uses `LocalIntelligenceOrchestrator` conversation turn analysis, evaluating user response depth and question frequency across turns.

### 7. StorytellingEvaluator
- **Focus:** Setup, conflict, resolution, sensory VAKS details, structural progression.
- **Local Implementation:** Analyzes response structure against narrative milestones (opening hook, conflict transition, clear takeaway).

### 8. PresentationEvaluator
- **Focus:** CCC / PREP / BLUF structural framework, vocal delivery, visual poise.
- **Local Implementation:** Evaluates point-reason-example structure via paragraph and sentence heuristics, fusing audio pitch modulation and MediaPipe visual posture cues.

### 9. ReflectionEvaluator
- **Focus:** Self-awareness, identification of speaking habits, real-world action plan.
- **Local Implementation:** Evaluates reflective depth, self-rating completion, and concrete next-step formulation.
- **Integrity Rule:** Strictly separates user-reported confidence self-ratings from objective performance metrics.

---

## 3. Preserved Six-Section Output Guarantee

Every evaluation generates an `ActivityEvaluationResult` containing all six canonical sections:
1. `whatWentWell: List<String>`
2. `specificMistakes: List<String>`
3. `correctedExamples: List<String>`
4. `explanation: String`
5. `improvementSuggestions: List<String>`
6. `nextPracticeTask: String`

Metadata fields (`honestyNotice`, `aiEvaluated`, `confidenceSelfRating`, `wordsPerMinute`, `fillerCount`, `wordCount`) are populated in every attempt.
