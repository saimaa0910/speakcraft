# Local Language Model Integration Specification

**Product:** SpeakCraft — Stage 1  
**Subsystem:** On-Device Natural Language Processing, Local SLM Inference, and Deterministic Coaching  
**Location:** `com.example.data.ai.local.language`

---

## 1. Architectural Strategy: Two-Tier Language Intelligence

To ensure that SpeakCraft functions instantaneously on all Android devices (from budget smartphones with 2 GB RAM up to flagship devices with NPUs), the language subsystem employs a strict two-tier strategy:

```text
                  EVALUATION EVIDENCE INPUT
                              |
                              v
                LocalFeedbackGenerationEngine
                              |
                [Is Phi-3.5 Installed & ADVANCED Profile?]
                     /                         \
                   YES                          NO
                   /                             \
                  v                               v
         LiteRT-LM Inference            Deterministic Engine
        - Microsoft Phi-3.5 Mini       - LocalLanguageAnalysisEngine
        - Quantized Q4 (GGUF/TFLite)   - Regex Grammar Rules
        - Grounded Context Prompt      - DeterministicFeedbackTemplates
        - Strict JSON Schema           - Zero RAM / CPU Overhead
                  \                               /
                   \                             /
                    +-------------+-------------+
                                  |
                                  v
                    Strict Validation & Fallback
                     [All 6 Canonical Sections]
```

---

## 2. Component Details

### 2.1 LocalLanguageAnalysisEngine
- **Grammar Diagnostics:** Implements high-precision deterministic rules for spoken English flaws:
  - Subject-Verb Agreement: flags *"he don't"*, *"she don't"*, *"I has"*, *"they was"*, *"we was"*.
  - Double Negatives: flags *"can't not"*, *"don't not"*, *"won't not"*, *"ain't no"*.
  - Indefinite Article Mismatches: flags *"a apple"*, *"a hour"*, *"an university"*, *"an unit"*.
  - Run-On Sentences: flags unsegmented compound clauses exceeding 60 words without punctuation.
- **Vocabulary Analytics:**
  - Tokenizes transcripts, computes unique vs. total word count to yield a `vocabularyDiversityScore` ($0–100\%$).
  - Detects repeated crutch words used $>3$ times in short recordings.
  - Matches target vocabulary words from curriculum exercises.

### 2.2 LocalFeedbackGenerationEngine (LiteRT-LM Integration)
- **Model Choice:** Microsoft Phi-3.5 Mini Instruct Q4 (3.8B parameters quantized to ~2.2 GB).
- **Runtime:** `com.google.ai.edge.litert:litert-lm` using on-device GPU/NPU acceleration.
- **Strict Grounding:** The SLM prompt supplies only the observed metrics (`LocalFeedbackEvidence` fields: WPM, filler counts, pause durations, grammar issues). It explicitly prohibits hallucinating quotes not found in the verbatim transcript.
- **Resilience:** If the model file is missing, device RAM is constrained, or the generated output fails JSON validation, the engine seamlessly falls back to `DeterministicFeedbackTemplates` with zero user disruption.

### 2.3 DeterministicFeedbackTemplates
When local SLM inference is unavailable, this hardened rule engine guarantees that learners receive comprehensive, pedagogical feedback structured across all 6 canonical sections:
1. **What Went Well:** Highlights concrete achievements (e.g., target length met, WPM within 120–170 sweet spot, calm pause rhythm, stable framing).
2. **Specific Mistakes:** Cites exact grammatical discrepancies, detected filler word counts, or pacing imbalances.
3. **Corrected Examples:** Directly presents before-and-after rewrites (e.g., replacing filler words with silent pauses or fixing article agreement).
4. **Explanation:** Explains the underlying communication principle (e.g., how downward inflection projects friendly authority).
5. **Improvement Suggestions:** Two to three actionable drills for the next practice session.
6. **Next Practice Task:** Dynamically recommends the next specific curriculum exercise based on the weakest evaluated dimension.
