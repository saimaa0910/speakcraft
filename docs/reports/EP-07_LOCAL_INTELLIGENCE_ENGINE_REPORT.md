# Local Intelligence Engine (EP-05/06/07, EP-29) — Increment Report

| Field | Value |
|---|---|
| Document ID | RPT-EP07-01 |
| Version | 0.5 |
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
| Quality | `GoldenFixturesTest` (8 fixtures pinned at 1.4.0), `LocalIntelligenceHonestyTest` (7), `Spk02QualificationTest` (2) | JVM |
| Models | `face_landmarker.task`, `pose_landmarker_lite.task` with checksums (`assets/models/MODELS_PROVENANCE.md`) | — |
| Issue overrides (CL-60) | `TargetBands`, `IssueOverrides` (SUPPRESS + REBAND, ARC-03 §6.8 precedence, V-22 validation); applied band recorded on each metric | JVM (9) |
| Content-pack tips (SRS-FR-EVAL-016) | `TipPackLoader` (strict `tips.json`, structured triggers, V-01…V-18), `TipSelector` catalogue parameter, `AssetTipCatalog` with built-in fallback | JVM (12) |

`evaluationVersion` is now **1.4.0** (changelog in `LocalIntelligencePipeline`). With no overrides and the built-in tips, every golden fixture is unchanged from 1.3.0.

### 4.1 Curriculum content pack v1 (Product Owner request 2026-11-23: "upgrade all activity or techniques … in parallel by agent")

The app's earlier hard-coded curriculum (`CurriculumCatalog.kt`: 15 technique seeds, speaking challenges, shadowing segments, public-speaking modules, real-world tasks, grammar lessons and pronunciation drills) was re-expressed into the ARC-03 §6 pack format by four content agents working in parallel from one brief, then merged and validated.

| Item | Count | Notes |
|---|---|---|
| Techniques (`techniques.json`) | 15 | Plain original names; steps, why-it-works, target metrics only where honest, self-check items for what audio cannot measure |
| Activities (`activities/trk-*.json`) | 86 | TPL-ANSWER 48, TPL-READ 18, TPL-STORY 11, TPL-STRUCT (PREP) 6, TPL-REPEAT 3; 8 general activities (grammar lessons, openings) |
| Coach tips (`tips.json`) | 111 | 16 global issue tips (one per issue code), global and per-template general tips, and technique tips that fire only for the practised technique; technique tips rank before global tips for the same issue |
| Issue overrides | 2 techniques; 15 entries on 14 activities | The deliberate-pause technique suppresses `ISSUE_PAUSES_LONG`; the explaining technique re-bands `ISSUE_LOW_VARIETY` (repeating one key term is intended; flagged for reviewer); slow read-aloud and articulation drills re-band `ISSUE_PACE_SLOW` |

D-08 safeguards: original wording (agents checked for 5-word runs shared with the seed text), no creator name or handle, creator-coined framework names only in the internal `provenance.techniqueNameAsTaught`, `attributionText = null`, and **no source links**: none was known, and sources are never invented. Every item is therefore `PENDING_REVIEW`. Debug builds use the pack; release builds reject it (V-10) and keep the built-in tips until a reviewer adds each specific source video and signs off (workflow in `content/README.md`). `ContentPackAssetsTest` (7 cases) enforces V-01…V-22 on the shipped files, including checksums in `manifest.json`, and checks end to end that each technique's overrides and tips reach feedback.

### 4.2 Engine upgrade 2026-11-24 (evaluationVersion 1.6.0)

Product Owner request: "upgrade local intelligence engine". Two Product Owner commits had landed on `main` first: `d6c349a` (1.5.0, inline transcript highlights and transcript/video overlay UI) and `3ca904f` (device-adaptive model installer), after which CI on `main` failed.

**Review of the two commits and fixes**

| Finding | Rule | Fix |
|---|---|---|
| Transcript highlights fell back to unqualified grammar rules when none was qualified | SRS-FR-EVAL-006 | Qualified rules only |
| Every grammar match was highlighted, while feedback shows at most two | SRS-FR-EVAL-006 | Capped at two; the same spans as the grammar issues |
| Every filler and hedge was a warning even when no issue was raised, and highlights ignored technique overrides | ARC-05 §8.4, CL-60 | Warning only when the issue was raised, otherwise a tip; suppressed codes not highlighted |
| Offsets came from a lowercased copy, which can change the text length (e.g. "İ") | — | Matched on the original text, case-insensitive |
| Installer downloaded Whisper/Moonshine models whose SHA-256 was blank, and the integrity check passed blank hashes | AI-SEC-19, THR-37, TC-AI-021 | Unpinned models are never downloaded; blank hash fails the check; downloads verified and deleted on mismatch; bundled MediaPipe hashes pinned |
| Installer downloaded on any network, at first launch | Data cost | Unmetered networks only |
| Installer switched the recogniser to local Whisper whether or not the model was installed (the local Whisper engine is still the fabricated path of FND-AI-03) | ARC-05 §12, SPK-03 gate | Switched only when the model is installed and verified (none can be yet) |
| Installer re-applied settings (video/audio analysis on) on later launches, overriding user choices | — | Defaults applied once, on the first provisioning |
| Installer marked the launch screen's first-launch flag as done in the background, which can skip the first-launch experience on a fresh install | — | Own `model_provisioning_completed` preference |

**New: candidate vocal-delivery metrics (ARC-05 §8).** `PitchTracker` (YIN) and `VocalDeliveryAnalyzer` compute pitch range, volume variation, syllable rate, pace variation and rising sentence endings from PCM, for the vocal techniques of the content pack (vocal variety, warm-and-assured, calm speaking). They are returned as **candidates** with `VocalMetricValidation` status; nothing is shown or used for feedback until SPK-01 validates them with `VocalMetricValidationHarness`.

Tests added: `VocalDeliveryTest` (10), `TranscriptHighlightConsistencyTest` (6), `ModelDownloadIntegrityTest` (3).

### 4.3 Coach Practice on the phone (EP-08 slice, 2026-11-24)

Product Owner request: "upgrade and make it user can use it on phone". Until now no screen used the engine. **Coach Practice** (bottom-bar item "Coach", sidebar and a dashboard tile; route `coach_practice`) is the first end-to-end path:

| Step | What the user does / sees | Built on |
|---|---|---|
| Choose | Quick practice (5 activities written from scratch, available in every build) and the 15 techniques of the content pack with their activities; pack items show "Pending review" and appear in debug builds only (V-10) | `PracticeCatalog`, `PracticeCatalogParser`, `AssetPracticeCatalog` |
| Prepare | Prompt, structure chips (PREP/STAR), target length, reference text (with "Play the model" for listen-and-repeat), optional camera with live preview, online-transcription consent only when the phone has no offline recogniser | `CoachPracticeScreens`, `OnlineTranscriptionConsentStore` |
| Record | Microphone permission on first use; timer with the activity's limit, a warning 10 s before it, automatic stop; discard option | `CoachPracticeViewModel`, `PhonePracticeEngine` → `SpeechAttemptRunner` (tier T1/T2/T3a/T3b chosen per phone) |
| Feedback | Strength, focus, mistakes with evidence and Measured/Estimated labels, coach tips (content-pack or built-in; "Watch the original lesson" only when a specific video link exists), transcript with highlights, body-language metrics when the camera was on, self-check items of the technique, replay (audio, or video with sound when both were recorded) | `LocalIntelligencePipeline`, technique `issueOverrides` resolved per activity |

Device builds need the project's Firebase configuration (`google-services.json`, gitignored and supplied locally as before); a build without it stops at launch ("Default FirebaseApp is not initialized").

Device behaviour: when a phone's on-device recogniser does not accept piped audio (T1), the attempt is evaluated honestly, the phone is switched to T2 and the user is asked to record again (RSK-09). Replays stay in app-private storage; only the three most recent attempts are kept. Attempts are not yet saved to history (EP-03).

Tests added: `PracticeCatalogParserTest` (5), `CoachPracticeViewModelTest` (8), `CoachPracticeComposeTest` (2).

## 5. Verification

| Level | How | Result |
|---|---|---|
| Local JVM harness (engine packages) | IntelliJ Kotlin compiler + minimal JUnit runner | **114 tests, 114 passed** |
| Local Gradle, debug unit tests (JDK 21, as CI) | `:app:testDebugUnitTest` | 316 tests ran; 314 passed; 2 workflow-policy tests failed only because of CRLF line endings on Windows (they pass on CI's Linux runner) — tests fixed to normalise line endings |
| Local Gradle, full CI task set | `:app:assembleDebug :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug` | See §5.1 |
| CI (GitHub Actions) | push to `feature/local-intelligence-engine` | See §5.1 |
| Devices | SPK-01 (speech), SPK-06 (video) | **Not run** — needs reference devices |

### 5.1 Build results

| Commit | Local (Temurin JDK 21, Android SDK 36) | CI (GitHub Actions) |
|---|---|---|
| `41b5192` acoustic features, SPK-02, session layer, video | `assembleDebug` and `compileReleaseKotlin` passed; `testDebugUnitTest` **316/316 passed**; `lintDebug` crashed inside lint's Kotlin analyser because source files were edited while lint was running (not a code finding) | Run 45197335962: **success** (build, unit/Robolectric tests, lint, static security checks) |
| `7b5c232` REBAND + content-pack tips | `testDebugUnitTest --tests com.example.speech.*`: **148/148 passed** (20 classes) | Run 45200167132: **success** |
| `b1a9515` content pack v1 | `testDebugUnitTest --tests com.example.speech.*`: **155/155 passed** (incl. `ContentPackAssetsTest` 7/7) | Run 45203035859: **success** |

## 6. Open items

| Item | Owner | When |
|---|---|---|
| Device checks: piped audio and segmented sessions per recogniser (SPK-01); MediaPipe accuracy, battery and thermal across lighting, skin tones and glasses (SPK-06) | AI/ML Engineer | Reference devices |
| Tune † thresholds (VAD, noise floor, video bands) on consented recordings | AI/ML Engineer | SPK-01 / SPK-06 |
| Review corpus SPK02-v1 | QA / Product Owner | Before beta |
| Review content pack v1: add each source video link, verify wording, set `ORIGINAL_REEXPRESSION` (until then release builds use the built-in tips) | Content reviewer / Product Owner | Before release |
| "Pending review" badge for pack items in debug builds (V-10) | UI | EP-08 |
| Validate the five vocal-delivery candidates on reference devices (`VocalMetricValidationHarness`) and record results before any is shown | AI/ML Engineer | SPK-01 |
| Downloadable ASR models: replace the fabricated local Whisper path (FND-AI-03), pass SPK-03, pin each SHA-256, and add user opt-in before download | Android Tech Lead / Product Owner | R2+ (ARC-06) |
| Session-player UI that drives `SpeechAttemptRunner`, camera preview and replay screen | UI | EP-08 |
| `ContentRepository` that passes each activity's technique ids and resolved overrides to `AttemptRequest` | Android Tech Lead | EP-09 |
| Privacy information and data-safety text for local video replay | Product Owner / Security | Before release |
