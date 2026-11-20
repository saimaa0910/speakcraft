# ADR-006 — Dependency currency upgrade and clean-up

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** (version upgrades and removals within the existing stack) |
| Date | 2026-11-20 |
| Owner (role) | Android Technical Lead |
| Related | RD-02 §3–4, RD-03 FND-OPS-06, ENG-01 |

## Context
Compose BOM 2024.09.00, lifecycle 2.8.7, navigation 2.8.9 and OkHttp 4.10.0 lag the toolchain (AGP 9.1.1, Kotlin 2.2.10, Firebase BoM 34.17.0). Eleven catalog libraries are unused; MediaPipe tasks-audio ships with zero imports; KSP and the secrets plugin do nothing; `firebase-appcheck-debug` ships in release; the JUnit catalog entry points at a non-existent version; the Gradle wrapper scripts are not committed (FND-OPS-02, FND-OPS-06).

## Decision (proposed)
1. **Upgrade** Compose BOM, lifecycle, navigation and activity-compose to current stable versions verified compatible with AGP 9.1.1 / Kotlin 2.2.10 at implementation time (exact versions recorded in the PR; no versions are prescribed here because compatibility must be verified by building).
2. **Remove** unused catalog entries (Retrofit, Moshi, Coil, Accompanist, play-services-location, logging-interceptor) and, subject to ADR-003/004/010/011: `firebase-ai`, MediaPipe vision/audio, TensorFlow Lite, CameraX, OkHttp, secrets plugin.
3. **Fix** the JUnit catalog reference; change `firebase-appcheck-debug` to `debugImplementation`.
4. **Commit** the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) and validate it in CI.
5. **Retain** KSP only if ADR-002 is approved.

## Consequences
Smaller APK, smaller attack surface, access to current Material 3 components. Upgrades happen in R0 (EP-01) before UI work, each in its own PR with full test run.

## Risks
Behaviour changes in Compose/Material 3 — mitigated by Robolectric UI tests and manual smoke tests per PR.

## Approval status
Pending — Product Owner.
