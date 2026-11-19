# Local Video Intelligence Engine Specification

**Product:** SpeakCraft — Stage 1  
**Subsystem:** Video Decoding, Frame Sampling, and Computer Vision Perception  
**Location:** `com.example.data.ai.local.video`

---

## 1. Architectural Overview

The Local Video Intelligence Engine processes CameraX MP4 video recordings captured in the Speaking Studio and Body Language Lab. To preserve device memory and battery life, it samples frames at a calibrated rate (1 frame per second, capped at 30–60 frames total), runs MediaPipe vision tasks on downscaled bitmaps, immediately recycles memory buffers, and outputs objective physical indicators.

```text
                  RECORDED VIDEO FILE (.mp4)
                              |
                              v
                VideoFrameProcessingEngine
      (MediaMetadataRetriever: 1 fps sampling, <= 640px)
                              |
              STREAMED BITMAPS (Immediate Recycle)
                              |
        +---------------------+---------------------+
        |                     |                     |
        v                     v                     v
FaceLandmarkEngine    BodyLanguageEngine     GestureEngine
(MediaPipe Face)      (MediaPipe Pose)       (MediaPipe Hand)
- 478 3D Landmarks    - 33 Body Landmarks    - 21 Hand Landmarks
- Head Yaw & Pitch    - Shoulder Alignment   - Hand Visibility %
- Facing Camera Est   - Postural Stability   - Wrist Movement Var
- Presence Guidance   - Movement Intensity   - Gestural Presence
        |                     |                     |
        +---------------------+---------------------+
                              |
                              v
                 VISUAL PRESENCE REPORT
```

---

## 2. Component Implementation Details

### 2.1 VideoFrameProcessingEngine
- **Native Frame Extraction:** Employs Android's `MediaMetadataRetriever` to extract keyframes at 1000ms intervals without decoding the entire video into RAM simultaneously.
- **Resolution Downscaling:** Extracted frames are rescaled to a maximum width of 640 pixels (preserving aspect ratio). This reduces bitmap memory footprint from ~33 MB (4K) to ~1.2 MB per frame.
- **Streaming Pipeline:** Provides `processFrames(...)` which invokes the vision analyzer callback and calls `bitmap.recycle()` immediately after each frame, ensuring memory consumption stays strictly bounded regardless of video duration.
- **Corrupt File Defense:** Catches missing index tables or corrupted media containers, reporting `corruptionDetected = true` with zero unhandled exceptions.

### 2.2 FaceLandmarkAnalysisEngine
- **Model:** MediaPipe Tasks Vision `FaceLandmarker` (`face_landmarker.task`, ~1 MB).
- **Head Orientation (Yaw & Pitch):**
  - Uses 3D landmark triangulation between the nose tip (landmark 1), left ear tragion (landmark 234), and right ear tragion (landmark 454) to calculate approximate head yaw (horizontal rotation).
  - Uses the forehead (landmark 10) and chin (landmark 152) axis to approximate head pitch (vertical tilt).
- **Camera Presence Guidance:**
  - $|\text{Yaw}| < 15^\circ$: *"Looking directly towards camera"*
  - $15^\circ \le |\text{Yaw}| \le 30^\circ$: *"Slightly turned from camera"*
  - $|\text{Yaw}| > 30^\circ$: *"Turned significantly away from camera"*
- **Privacy & Psychological Invariant:** **Strictly zero facial emotion or psychological inference.** Does not infer confidence, sincerity, honesty, gender, or mood from facial expressions.

### 2.3 BodyLanguageAnalysisEngine
- **Model:** MediaPipe Tasks Vision `PoseLandmarker` (`pose_landmarker_lite.task`, ~5 MB).
- **Shoulder Levelness & Alignment:**
  - Compares normalized Y coordinates of left shoulder (landmark 11) and right shoulder (landmark 12).
  - Absolute delta $> 0.05$ flags uneven posture with the suggestion to keep shoulders level and relaxed.
- **Postural Stability & Movement Level:**
  - Computes the standard deviation of the nose/torso position across analyzed frames.
  - Categorized as:
    - $\sigma < 0.015$: *"Stable, calm physical presence"*
    - $0.015 \le \sigma \le 0.040$: *"Moderate, natural movement"*
    - $\sigma > 0.040$: *"Frequent shifting or visible pacing"*
- **Upper Body Visibility:** Verifies whether both shoulders and elbows remain within the camera viewport.

### 2.4 GestureAnalysisEngine
- **Model:** MediaPipe Tasks Vision `HandLandmarker` (`hand_landmarker.task`, ~2 MB).
- **Hand Presence & Visibility Rate:** Measures the percentage of recorded frames where left and/or right hands are detected within the camera frame.
- **Gesture Dynamics:** Analyzes standard deviation of wrist coordinates across active frames to determine whether gestures are dynamic, subtle, or stationary.
- **Non-Judgmental Feedback:** Treats hand gestures as communicative options rather than scoring them linearly. Gestures are noted factually (e.g., *"Hands visible in 65% of frames"*), accompanied by Vinh Giang principles on using gestures to visually structure thoughts (box framing, open palms).
