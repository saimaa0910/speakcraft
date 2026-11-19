# Local Audio Intelligence Engine Specification

**Product:** SpeakCraft — Stage 1  
**Subsystem:** Audio Perception, Signal Processing, and Speech Analysis  
**Location:** `com.example.data.ai.local.audio`

---

## 1. Architectural Overview

The Local Audio Intelligence Engine is responsible for processing captured microphone audio (.m4a / .wav), extracting acoustic signal properties, transcribing speech without external APIs, identifying vocal habits (fillers, pauses, pace), and analyzing pronunciation accuracy.

```text
                  RECORDED AUDIO FILE (.m4a / .wav)
                                |
                                v
                          AudioResampler
             (MediaExtractor + MediaCodec -> 16kHz PCM)
                                |
                +---------------+---------------+
                |                               |
                v                               v
  LocalSpeechRecognitionEngine      AudioFeatureExtractionEngine
    - Whisper Tiny TFLite             - RMS Energy & Loudness
    - Android Offline Dictation       - VAD (Voice Activity Detection)
                |                     - YIN Pitch Estimation (F0)
                v                     - Pause Frequency & Duration
          RAW TRANSCRIPT              - WPM & Articulation Rate
                |                               |
        +-------+-------+                       |
        |               |                       |
        v               v                       |
  FillerWordEngine  PronunciationEngine         |
  - \b word \b      - Levenshtein Word Align    |
  - Multi-word      - Mispronunciation List     |
  - Per-Min Rate    - Intelligibility Estimate  |
        |               |                       |
        +---------------+-----------------------+
                        |
                        v
              ACOUSTIC EVIDENCE BUNDLE
```

---

## 2. Component Implementation Details

### 2.1 AudioResampler
- **Purpose:** Transforms user recordings (captured at 44.1 kHz AAC by `AudioRecorderManager`) into 16,000 Hz 16-bit mono Int16 PCM required by speech recognition and pitch estimation algorithms.
- **Decoding:** Uses native Android `MediaExtractor` and `MediaCodec` without external FFmpeg or heavy native libraries.
- **Memory Safety:** Automatically bounds processed audio to a maximum duration of 30 seconds (480,000 samples) to prevent mobile JVM heap exhaustion.
- **Cleanup:** Explicit `finally` block guarantees `MediaCodec.release()` and `MediaExtractor.release()` are called even if exceptions occur.

### 2.2 AudioFeatureExtractionEngine
Performs rigorous signal processing over the decoded 16 kHz PCM array:
1. **RMS Energy & Relative Loudness:**
   $$\text{RMS} = \sqrt{\frac{1}{N} \sum_{i=1}^N x[i]^2}$$
   Normalized between `0.0f` (complete silence) and `1.0f` (full scale).
2. **Clipping Detection:** Tracks samples where $|x[i]| \ge 32,000$, flagging distorted recordings that could compromise speech clarity.
3. **Voice Activity Detection (VAD):** Slides 20ms non-overlapping frames. A frame is labeled active if its frame RMS exceeds `0.015f`. Tracks `speechDurationSeconds` vs `silenceDurationSeconds`.
4. **Fundamental Frequency ($F_0$) Estimation (YIN Approximation):**
   - Applies difference function $d_t(\tau)$ over voiced frames in the 80 Hz to 400 Hz range.
   - Computes mean pitch and pitch variation (standard deviation), allowing objective assessment of vocal monotony or dynamic expression.
5. **Pause Analytics:** Identifies continuous silence regions exceeding 300ms. Computes pause count and mean pause duration in milliseconds.
6. **Speaking Pace (WPM) & Articulation:** Calculates words per minute based on recognized transcript word count relative to active speech duration.

### 2.3 FillerWordDetectionEngine
- **Regex Boundary Matching:** Evaluates speech transcripts using word-boundary patterns (`\b`) to eliminate false substring positives (e.g., "like" inside "likely" or "umbrella" flagged as "um").
- **Detected Disfluencies:**
  - Single words: *um, uh, like, basically, actually, right*
  - Multi-word phrases: *you know, sort of, kind of, okay so, i mean*
- **Metrics:** Returns total count, breakdown map, and time-normalized frequency:
  $$\text{Frequency} = \frac{\text{Filler Count}}{\text{Speech Duration Minutes}}$$

### 2.4 PronunciationAnalysisEngine
- **Deterministic Alignment:** When a target or expected script is provided (e.g., in reading aloud or shadowing exercises), the engine calculates word-level Levenshtein edit distance against the recognized transcript.
- **Word Accuracy:**
  $$\text{Accuracy} = \frac{\text{Expected Words} - \text{Substitutions} - \text{Deletions}}{\text{Expected Words}}$$
- **Likely Mispronounced List:** Identifies expected words omitted or significantly mutated in the recognized output.
- **Mandatory Honesty Caveat:** Never fabricates phoneme-level percentages without a calibrated phonetic laboratory acoustic model. Every output contains:
  > *"Transcript comparison assesses wording match and audible clarity. Phoneme-level feedback is only provided when supported by a validated on-device acoustic model."*

### 2.5 LocalSpeechRecognitionEngine
- **Dual Pipeline Architecture:**
  1. **Primary (Installed):** Loads `whisper-tiny-en.tflite` via `org.tensorflow.lite.Interpreter`. Computes log-mel spectrograms and executes greedy token decoding into English text.
  2. **Secondary (Offline Fallback):** Checks Android `SpeechRecognizer` for on-device recognition capability and inspects media duration via `MediaPlayer`.
- **Fault Tolerance:** Gracefully handles missing models or `OutOfMemoryError` by generating transparent limitation notices without crashing the UI.
