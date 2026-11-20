# ADR-010 — Defer video capture and automated body-language analysis

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** |
| Date | 2026-11-20 |
| Owner (role) | AI/ML Engineer |
| Related | RD-16 NG-07, RD-03 FND-AI-12, FND-AI-13 |

## Context
MediaPipe face/pose/hand engines exist, but no `.task` model files are bundled and nothing downloads them, so every landmarker fails at runtime while Settings reports "ready" (FND-AI-05, FND-AI-12). What they would compute is framing (face in frame, shoulder level), mislabelled as body language ("head tilt" from shoulder unevenness). Activity-engine video is simulated. Frame extraction holds up to 60 bitmaps (FND-AI-13).

## Decision (proposed)
Remove CameraX video recording, MediaPipe vision/audio dependencies, the three landmark engines and `VideoFrameProcessingEngine` from R1–R2. Reconsider in R3 as **video self-review without automated scoring** (record, watch back with a structured self-assessment checklist), which needs CameraX only.

## Consequences
APK size reduction (MediaPipe native libraries) and removal of a 16 KB page-alignment risk; simpler permissions (no CAMERA in R1–R2).

## Approval status
Pending — Product Owner.
