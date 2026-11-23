# Local Intelligence Engine (EP-05/06/07, EP-29) — Increment Report

| Field | Value |
|---|---|
| Document ID | RPT-EP07-01 |
| Version | 0.2 |
| Date | 2026-11-23 |
| Status | Draft — for Product Owner review (epic review G5) |
| Owner (role) | Android Technical Lead / AI/ML Engineer |
| Branch | `feature/local-intelligence-engine` |
| Related | ARC-05 v0.2 §10.1, ARC-07, ADR-017 (D-42), PLN-02 EP-05/06/07/29, SRS-FR-EVAL-*, SRS-FR-VID-*, SRS-BR-001/002/028, TST-01 §7/§9 |

## 1. Scope

The Product Owner asked (2026-11-23) to "start building local intelligence engine", then to add an **audio and video analyser** and complete all tasks, stories and epics related to the engine. Decisions taken in the session: **D-42** video analysis with local replay; verify with a local Android SDK and CI on pushed increments; bundle the official MediaPipe face and pose models.

## 2. What was found and fixed in the existing engine

| # | Finding | Rule | Fix |
|---|---|---|---|
| F-1 | Audio-only attempts reported "0 fillers / 0 hedges / 0 grammar issues", earning false strengths | SRS-BR-002, PP-1 | Transcript metrics omitted without a transcript |
| F-2 | Grammar rules labelled "verified ≥ 0.90" with no measurement; false positives ("does she have") | SRS-FR-EVAL-006 | Candidate rules + SPK-02 harness + registry; corpus SPK02-v1 measured (§3) |
| F-3 | A default loudness (0.05) replaced missing evidence | SRS-BR-002 | No default; level check only with evidence |
| F-4 | No acoustic analysis stage | ARC-05 §4.1 | `StreamingFeatureExtractor`, `RmsSeriesFeatureExtractor` |
| F-5 | Technique issue overrides missing | CL-60 | SUPPRESS overrides |
| F-6 | Metrics applied to templates they don't belong to (a failed read-aloud praised for "few fillers"; "pace too slow" on a one-word stage) — found by golden fixtures | ARC-05 §6 | `MetricEngine.isApplicable` |
| F-7 | Duplicate issues (same code twice) | SRS-FR-EVAL-015 | One issue per code (grammar spans excepted) |
| F-8 | Word-stage headlines showed pace for a single word | ARC-05 §7 | Removed |
| F-9 | Four coach tips linked to **invented** YouTube URLs (e.g. `watch?v=speech_consonants_drill`) | SRS-BR-028, ARC-03 V-12 | Removed; a test rejects any non-specific video link |
| F-10 | The noise floor landed on speech when < 10 % of a recording was silent → "no speech" | ARC-05 §5 | Noise floor also uses the quietest 200 ms stretch |

## 3. SPK-02 grammar-rule qualification (corpus SPK02-v1)

31 positive and 31 negative recogniser-style examples per rule, including correct sentences that look wrong without punctuation.

| Rule | TP | FP | FN | TN | Precision | Recall | Qualified |
|---|---|---|---|---|---|---|---|
| R-AGREE-01 he/she/it don't | 31 | 0 | 0 | 31 | 1.000 | 1.000 | Yes |
| R-AGREE-02 he/she/it have | 31 | 1 | 0 | 30 | 0.969 | 1.000 | Yes |
| R-COMP-01 double comparative | 31 | 2 | 0 | 29 | 0.939 | 1.000 | Yes |
| R-PAST-01 didn't + past | 31 | 0 | 0 | 31 | 1.000 | 1.000 | Yes |
| R-PART-01 have went | 31 | 2 | 0 | 29 | 0.939 | 1.000 | Yes |
| R-ARTICLE-01 a + vowel word | 31 | 2 | 0 | 29 | 0.939 | 1.000 | Yes |

`Spk02QualificationTest` re-measures on every build and fails if the registry differs. The corpus was written by the development agent; a QA / Product Owner review of the examples is recommended.

## 4. New components

| Area | Components | Tested |
|---|---|---|
| Acoustic features (CMP-30) | `StreamingFeatureExtractor`, `RmsSeriesFeatureExtractor`, `AcousticFeatures` | JVM (15) |
| Session layer (EP-06, CMP-26…29) | `CaptureController` (stop reasons, 10 s warning ≥ 30 s limits), `RecordingArbiter`, `RecognitionSession` (T1 segmented; T2/T3a restart + stitch; error classes; partial fallback), `SpeechAttemptRunner` (all tiers + camera) | JVM (15) |
| Android adapters | `AudioRecordSource`, `WavFileSink`, `RecognizerPipe`, `PlatformRecognizerEngine`, `LanguagePackManager`, `OnlineTranscriptionConsentStore`; `AppContainer` wiring | Compile only (device check in SPK-01) |
| Video analyser (EP-29, ARC-07) | `VideoAnalyzer`, `HeadPose`, video models; issue codes 11–13 and tips; self-check pruning | JVM (13 + 4 pipeline) |
| Video Android adapters | `MediaPipeFrameAnalyzer`, `VideoCaptureController` (Preview + Analysis + video-only capture, fallback), `AudioTranscoder` (WAV → AAC, ADR-015), `ReplayMuxer` | Compile only (device check in SPK-06) |
| Quality | `GoldenFixturesTest` (8 fixtures pinned at 1.3.0), `LocalIntelligenceHonestyTest` (7), `Spk02QualificationTest` (2) | JVM |
| Models | `face_landmarker.task`, `pose_landmarker_lite.task` with checksums (`assets/models/MODELS_PROVENANCE.md`) | — |

`evaluationVersion` is now **1.3.0** (changelog in `LocalIntelligencePipeline`).

## 5. Verification

| Level | How | Result |
|---|---|---|
| Local JVM harness (engine packages) | IntelliJ Kotlin compiler + minimal JUnit runner | **114 tests, 114 passed** |
| Local Gradle, debug unit tests (JDK 21, as CI) | `:app:testDebugUnitTest` | 316 tests ran; 314 passed; 2 workflow-policy tests failed only because of CRLF line endings on Windows (they pass on CI's Linux runner) — tests fixed to normalise line endings |
| Local Gradle, full CI task set | `:app:assembleDebug :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug` | See §5.1 |
| CI (GitHub Actions) | push to `feature/local-intelligence-engine` | See §5.1 |
| Devices | SPK-01 (speech), SPK-06 (video) | **Not run** — needs reference devices |

### 5.1 Build results
_Filled in after the runs; see the session summary._

## 6. Open items

| Item | Owner | When |
|---|---|---|
| Device checks: piped audio and segmented sessions per recogniser (SPK-01); MediaPipe accuracy, battery and thermal across lighting, skin tones and glasses (SPK-06) | AI/ML Engineer | Reference devices |
| Tune † thresholds (VAD, noise floor, video bands) on consented recordings | AI/ML Engineer | SPK-01 / SPK-06 |
| Review corpus SPK02-v1 | QA / Product Owner | Before beta |
| Session-player UI that drives `SpeechAttemptRunner`, camera preview and replay screen | UI | EP-08 |
| REBAND overrides; tips from the content pack | Android Tech Lead | EP-09 |
| Privacy information and data-safety text for local video replay | Product Owner / Security | Before release |
