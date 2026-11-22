# ADR-003 — Remove Firebase AI Logic (cloud AI) from the product

| Field | Value |
|---|---|
| Status | **Accepted & Implemented (2026-11-21 — Stage B Sprint 1, T-04.1.2)** |
| Date | 2026-11-20 |
| Owner (role) | AI/ML Engineer / Security Engineer |
| Related | ARC-05, SEC-01, RD-03 FND-AI-10, RD-05 (cloud-AI conflicts), PO-REQ-1119 (no LLMs, no paid cloud AI, no recordings/transcripts to cloud AI), PO-REQ-1120 §9.5 |

## Context
- `firebase-ai` is an `implementation` dependency and `FirebaseAiLogicService` (868 lines) implements Gemini calls, including raw audio/video upload (up to 19 MiB), transcripts, profile data, and Gemini Live microphone streaming (FND-AI-10).
- It is unreachable at runtime only because `LocalExecutionPolicy.POLICY` is a compile-time constant `LOCAL_ONLY`. There is no consent flow; its fallback parser invents default scores (74, floor 20).
- The "network boundary" interceptor that docs and tests cite as enforcement is not attached to any client.
- Product direction oscillated: cloud → "zero cloud" → "hybrid" within three days (RD-05).

## Problem
Shipping dormant cloud-AI code and SDKs contradicts the Product Owner's privacy constraints, enlarges the attack and review surface, confuses settings ("Cloud AI: Unavailable"), and invites accidental re-enablement.

## Options
1. **Remove** the SDK, the service, the hybrid coordinator's cloud branches, cloud settings keys and related tests/UI.
2. Keep it dormant behind the constant (status quo).
3. Re-enable behind consent.

## Decision (proposed)
**Option 1.** Remove `firebase-ai`, `FirebaseAiLogicService`, `GeminiCoachService`, cloud branches in `HybridAiCoachCoordinator` (the coordinator itself is replaced by the ARC-05 pipeline), `cloud_ai_*` preference keys, and the unused interceptor. Replace the false-assurance test with a build-level check that no cloud-AI SDK is on the runtime classpath (TST-01 TC-SEC-010).

## Consequences
- Smaller APK and simpler settings; privacy claims become verifiable.
- Any future cloud or on-device LLM feature requires a **new ADR, a privacy review (SEC-01 §9) and explicit Product Owner authorisation** (NG-01).

## Risks
Loss of a possible future capability — mitigated because the code remains in git history.

## Approval status
Approved by Product Owner (D-04, G1–G4, 2026-11-21). Implemented in Sprint 1 (`T-04.1.2`): `firebase-ai`, `FirebaseAiLogicService`, `GeminiCoachService`, cloud branches in `HybridAiCoachCoordinator`, `cloud_ai_*` keys, and Settings Cloud AI toggle removed; enforced by `ZeroCloudAiNetworkBoundaryTest` (TC-SEC-010) and `.github/workflows/ci.yml`.
