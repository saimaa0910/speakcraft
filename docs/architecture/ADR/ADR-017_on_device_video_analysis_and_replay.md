# ADR-017 — On-device video analysis with local replay

| Field | Value |
|---|---|
| Status | **Accepted — Product Owner decision D-42 (2026-11-23)**; thresholds † remain proposed until SPK-06 |
| Date | 2026-11-23 |
| Owner (role) | Solution Architect / AI/ML Engineer / Security Engineer |
| Related | ARC-07, ADR-010 (partly superseded), ADR-015, RD-16 NG-07 (amended), SEC-01 §7, D-10, D-22, D-42 |

## Context
ADR-010 deferred video capture and excluded automated body-language analysis because the existing implementation had no bundled models and weak heuristics. On 2026-11-23 the Product Owner asked for "audio and video analyser within this app" and chose **analysis + local replay** over live-only analysis or self-review only.

## Problem
How to add video honestly, privately and without new technology risk, given that CameraX 1.5.0 and MediaPipe Tasks 0.10.14 are already in the build.

## Options
- **A — Self-review only** (ADR-010 R3 plan): record and replay, no automated metrics.
- **B — Live analysis, no video saved.**
- **C — Analysis plus local replay** (chosen by the Product Owner).

## Decision
Option **C**, under these rules:
1. **On-device only.** Frames are analysed on the phone with MediaPipe Face Landmarker and Pose Landmarker (models bundled, `assets/models/MODELS_PROVENANCE.md`). No frame, landmark or result is uploaded; video is never synced (NG-12 still applies).
2. **Estimated, never scored.** Video metrics (V-FRAME, V-CAMERA, V-GESTURE, V-POSTURE, V-STEADY, V-SMILE) are labelled Estimated. There is no composite "presence" or "confidence" score (D-06). "Facing the camera" is named as head orientation, not eye contact.
3. **Only actionable findings are mistakes:** face out of frame, not facing the camera, tilted shoulders. Hand movement, head movement and smiling are reported, never called mistakes.
4. **Local replay** stores one clip per attempt under `noBackupFilesDir/recordings/<profileId>/<attemptId>.mp4`, with the same retention options and deletion paths as audio recordings (ADR-015, D-10); excluded from backup. Video is captured without audio and muxed with the attempt's AAC audio after evaluation, so the microphone stays owned by the speech pipeline.
5. **Camera is optional per attempt** and needs the CAMERA permission, asked only when the user turns the camera on. Audio evaluation never depends on video; an Inconclusive video (too dark, face not visible, too few frames) does not affect the audio result.
6. **No identity processing.** No face embeddings, no recognition, no storage of landmarks — only per-attempt aggregate metrics.

## Consequences
- RD-16 NG-07 is amended: automated body-language *scoring* stays a non-goal; Estimated, labelled video metrics with a deterministic evidence gate are in scope.
- ADR-010 is superseded for video capture and analysis; its "no scoring" principle is kept.
- SRS-FR-PLAT-001's removal of CameraX and MediaPipe is withdrawn for these two libraries; TensorFlow Lite stays only as MediaPipe's runtime dependency. The fake Whisper/Moonshine engines remain removed.
- APK size grows by about 9.5 MB (models). Battery and thermal cost must stay within ARC-07 §8 budgets.
- Privacy policy, data-safety form and the export (D-22) must mention local video replay.

## Risks
Landmark accuracy varies with lighting, skin tone, glasses and camera quality (to be checked across a varied consented set in SPK-06); users may read Estimated metrics as judgements (mitigated by labels, wording and no scores); storage use from video (mitigated by retention and size caps).

## Approval status
Decision D-42 approved by the Product Owner on 2026-11-23 (PLN-05 §7). Proposed values † are tuned at SPK-06.
