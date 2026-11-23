# Video Analysis Architecture — SpeakCraft

| Field | Value |
|---|---|
| Document ID | ARC-07 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-23 |
| Status | Decision D-42 approved (2026-11-23); design Draft — thresholds † proposed until SPK-06 |
| Owner (role) | AI/ML Engineer / Android Technical Lead |
| Approval | D-42 Approved — Product Owner; ARC-07 review at the epic review (G5) |
| Related | ADR-017, ARC-05 (audio pipeline), ADR-015 (recordings), SEC-01 §7, RD-16 NG-07 (amended), EP-29 |

## 1. Purpose and scope
Adds an on-device **video analyser with local replay** to speaking attempts, as decided by the Product Owner (D-42). Video is optional per attempt. It complements the audio pipeline (ARC-05) and never replaces or blocks it.

Out of scope: any upload of video or landmarks; face recognition or identity; composite presence/confidence scores; emotion classification; eye tracking.

## 2. Principles
| ID | Principle |
|---|---|
| V-P1 | On-device only; nothing leaves the phone |
| V-P2 | Estimated labels on every video metric; no composite score |
| V-P3 | Name what is measured: head orientation is "facing the camera", not "eye contact" |
| V-P4 | A metric exists only when its inputs were detected; never a default |
| V-P5 | Only actionable findings are mistakes; style is not a mistake |
| V-P6 | Audio evaluation is independent of video |
| V-P7 | Replay follows the same retention, deletion and backup rules as audio (ADR-015) |

## 3. Components
| Component | Package | Responsibility | Status |
|---|---|---|---|
| `VideoAnalyzer` | `speech.video` | Evidence gate, metric catalogue, issues from frame observations (pure Kotlin) | Implemented, JVM-tested |
| `HeadPose` | `speech.video` | Head yaw/pitch/roll from the facial transformation matrix | Implemented, JVM-tested |
| `VideoModels` | `speech.video` | `FrameObservation`, `FaceObservation`, `PoseObservation`, `VideoEvaluation` | Implemented |
| `MediaPipeFrameAnalyzer` | `speech.video.android` | CameraX `ImageAnalysis.Analyzer` → MediaPipe Face/Pose Landmarker (LIVE_STREAM) → `FrameObservation` (mean luma, face box, blendshape smile, head matrix, shoulders, wrists) | Android increment |
| `VideoCaptureController` | `speech.video.android` | CameraX front camera: Preview + ImageAnalysis (≈ 10 fps analysed †) + VideoCapture without audio to an app-private file | Android increment |
| `ReplayMuxer` | `speech.video.android` | After evaluation, mux the H.264 video track with the attempt's AAC audio (`MediaExtractor` + `MediaMuxer`), delete intermediates | Android increment |
| `LocalIntelligencePipeline` | `speech.pipeline` | Accepts `AttemptInput.video`; merges video issues after audio issues; drops self-checks the video measured | Implemented |

Data flow: `CameraX ImageAnalysis → MediaPipeFrameAnalyzer → FrameObservation list → VideoAnalyzer.analyse → VideoEvaluation → AttemptInput.video → LocalIntelligencePipeline`. In parallel: `VideoCapture → attempt.mp4 (video only) → ReplayMuxer (+ attempt.m4a) → replay.mp4`.

## 4. Inputs per frame
`FrameObservation(timestampMs, luma, face?, pose?)`:
- `luma`: mean of the Y plane (0..255) of the analysed frame;
- `face`: normalised box centre/size, `yaw/pitch/roll` from `HeadPose.fromMatrix` (null without the matrix), `smile` = mean of `mouthSmileLeft`/`mouthSmileRight` blendshapes (null without blendshapes);
- `pose`: left/right shoulder and wrist points with visibility (MediaPipe pose landmarks 11/12 and 15/16).

## 5. Evidence gate and metric catalogue
**Gate (video only; audio unaffected):** fewer than 15 frames or < 3 s analysed → `TOO_FEW_FRAMES`; median luma < 40 → `TOO_DARK`; face in < 30 % of frames → `NO_FACE`. All †.

| Key | Metric | Computed from | Label | Band / issue † |
|---|---|---|---|---|
| V-FRAME | Face in frame | share of analysed frames with a detected face | Estimated | issue `ISSUE_FACE_OUT_OF_FRAME` below 75 % (target ≥ 80 %, noise 5) |
| V-CAMERA | Facing the camera | share of face frames with \|yaw\| ≤ 20° and \|pitch\| ≤ 20°; omitted without head angles | Estimated | issue `ISSUE_LOOKING_AWAY` below 55 % (target ≥ 60 %, noise 5) |
| V-GESTURE | Hand movement | share of time a visible wrist moves ≥ 0.15 frame widths/s (frames ≤ 250 ms apart); omitted without pose | Estimated | bands Active ≥ 15 % · Some ≥ 5 % · Little; **never an issue** |
| V-POSTURE | Shoulder tilt | mean absolute shoulder-line angle (both shoulders visible, ≥ 10 frames) | Estimated | issue `ISSUE_POSTURE_TILTED` above 12° (target ≤ 8°, noise 4°) |
| V-STEADY | Head movement | SD of face centre ÷ mean face width | Estimated | Steady ≤ 0.25 · Some ≤ 0.5 · A lot; never an issue |
| V-SMILE | Smiling | share of face frames with smile blendshape ≥ 0.5; omitted without blendshapes | Estimated | Warm ≥ 20 % · Neutral; never an issue |

## 6. Feedback integration
- Video issues have priorities 11–13, so audio issues remain the focus (ARC-05 §8.4 display priority).
- Coach tips: `TIP-VIDEO-FRAME-01`, `TIP-VIDEO-CAMERA-01`, `TIP-VIDEO-POSTURE-01` (original wording; links only after CNT-01 verification).
- Self-checks: `SC-EYE-01`, `SC-POSTURE-01`, `SC-GESTURE-01` are dropped when V-CAMERA, V-POSTURE, V-GESTURE were measured; facial-expression self-check remains.
- Evaluation version 1.3.0 introduced the video metrics.

## 7. Privacy and data
| Topic | Rule |
|---|---|
| Permission | CAMERA requested only when the user enables the camera for an attempt; denial keeps audio-only practice |
| Storage | `noBackupFilesDir/recordings/<profileId>/<attemptId>.mp4`; excluded from backup and device transfer; never uploaded or synced |
| Retention | Same options as audio (ADR-015, D-10: default 30 days †); a size cap of 1 GB † for all recordings, oldest deleted first with notice |
| Persistence | Only aggregate video metrics (ENT-04-style rows with V-* keys) are stored with the attempt; no landmarks, no frames |
| Deletion | Attempt delete, Clear history, sign-out Remove and account deletion delete replay files (SEC-01 §6.4) |
| Export | Included only in the optional recordings ZIP (D-22) |
| Disclosure | Privacy information (SRS-FR-SET-006) and data-safety form updated before release |

## 8. Performance budget †
Analysis at ≈ 10 fps with the lite pose model and the face model; ≤ 35 ms per analysed frame on RD-MID; frames dropped (not queued) when busy (`STRATEGY_KEEP_ONLY_LATEST`); recording 720p, ~2 Mbit/s; thermal throttling → analysis rate halves before stopping. Measured in SPK-06 on RD-LOW/MID/HIGH.

## 9. Testing
- JVM: `VideoAnalyzerTest` (13 cases: gate, each metric, honesty, head-pose maths), `VideoPipelineIntegrationTest` (4 cases).
- Device: SPK-06 — consented recordings across lighting, skin tones, glasses, camera angles; compare V-* to manual annotation; tune † thresholds; check battery and thermal.

## 10. Open items
REBAND support for techniques that want different video bands; landmark accuracy check (SPK-06); replay UI in the session result (EP-08).
