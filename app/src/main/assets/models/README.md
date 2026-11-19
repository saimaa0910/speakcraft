# SpeakCraft On-Device AI Models (Stage 1.2 Lightweight Catalog)

Bundled MediaPipe models (or placed in assets):
- `face_landmarker.task` (~1 MB) - Download from https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
- `pose_landmarker_lite.task` (~5 MB) - Download from https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task  
- `hand_landmarker.task` (~2 MB) - Download from https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task

Optional User-Downloaded Speech Models (stored in app files dir, NOT in assets):
- `whisper-tiny-en.tflite` (~39 MB) - Optional on-device speech recognizer
- `moonshine-tiny-int8.tflite` (~26 MB) - Alternative lightweight speech recognizer

**CRITICAL STAGE 1.2 INVARIANT:**
Zero large language models (LLMs like Phi-3.5 Mini, Gemma, Qwen) are required or supported.
All language, grammar, fluency, structure, confidence, and coaching feedback run deterministically with 0 MB downloaded models.
