# ADR-015 — Recording storage format and retention

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** (privacy-affecting product default) |
| Date | 2026-11-20 (revised 2026-11-21: transcode made mandatory) |
| Owner (role) | Security Engineer / AI/ML Engineer |
| Related | ADR-009, SEC-01 §7, FS-04, FS-10, RD-03 FND-AI-14, FND-SEC-06, FND-SEC-09, D-10 |

## Context
Recordings are saved to a shared `filesDir/recordings/` folder, are not partitioned per user, are eligible for Google backup (`allowBackup=true` with template rules), are never cleaned up for failed or discarded attempts, and their absolute device paths are stored in Firestore (FND-AI-14, FND-SEC-06, FND-SEC-09, FND-DATA-04).

## Decision (proposed)
1. **Location:** `noBackupFilesDir/recordings/<profileId>/<attemptId>.(wav|m4a)` — excluded from backup and device transfer by construction; backup rules also exclude it explicitly.
2. **Format:** capture as 16 kHz mono PCM16 WAV (ADR-009). After evaluation, **transcode to AAC/M4A** via the platform `MediaCodec` (≈ 10× smaller) and delete the WAV. This step is mandatory, not optional: 30 days of daily WAV recordings would take about 290 MB, far above the storage target (consistency item CL-73, ARC-02 §12). If transcoding fails, keep the WAV and retry in the daily cleanup job.
3. **Retention default:** keep recordings for **30 days** on the device so users can replay and compare; user options: *Delete after feedback*, *7 days*, *30 days*, *Keep until I delete*. A daily cleanup job enforces retention.
4. **Discarded/failed attempts** are deleted immediately.
5. **Never uploaded**; cloud records reference attempts by `attemptId` only, never by file path.
6. Account deletion and "Clear history" delete the profile's recording folder.

## Approval status
Pending — Product Owner (decision D-10: default retention period).
