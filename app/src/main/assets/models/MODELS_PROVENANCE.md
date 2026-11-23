# Bundled model provenance

Models bundled for the on-device video analyser (ARC-07; decision D-42). Downloaded on 2026-11-23 from Google's official MediaPipe model storage, as authorised by the Product Owner. Both models are licensed under Apache 2.0 by Google (see the MediaPipe model cards). They run entirely on the device; no frame, landmark or result leaves the phone.

| File | Source URL | Size (bytes) | SHA-256 | Used by |
|---|---|---|---|---|
| `face_landmarker.task` | https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task | 3,758,596 | `64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff` | `MediaPipeFrameAnalyzer` — face box, blendshapes (smile), facial transformation matrix (head angles) |
| `pose_landmarker_lite.task` | https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task | 5,777,746 | `59929e1d1ee95287735ddd833b19cf4ac46d29bc7afddbbf6753c459690d574a` | `MediaPipeFrameAnalyzer` — shoulders and wrists (posture, hand movement) |

To update a model, replace the file, record the new URL, size and checksum here, and bump `LocalIntelligencePipeline.EVALUATION_VERSION`. Video metric thresholds may need re-tuning (ARC-07 §9).

The older `README.md` in this folder predates the redevelopment; it is kept unchanged as a historical record (GOV-02 DC-1).
