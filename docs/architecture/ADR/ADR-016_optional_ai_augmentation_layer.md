# ADR-016 — Optional AI augmentation layer (R3+, conditional)

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval (D-37…D-41); not in R1/R2 scope** |
| Date | 2026-11-21 |
| Owner (role) | Solution Architect / AI/ML Engineer / Security Engineer |
| Related | ARC-06 (design; the "secure implementation design"), ADR-003 (D-04 remove cloud AI), ADR-009 (speech tiers, SPK-03), ADR-008 (App Check), ADR-013 (no analytics SDK), ARC-02 (CMP-35, CMP-75, §9 flags), ARC-03 (§8 sync, §10 export), ARC-04 (§1 no backend, §2 row 4), ARC-05 (§8.4, §12), SEC-01 (§7.3, §9.6, §9.7, SEC-REQ-02, SEC-REQ-28, TC-SEC-010), RD-15 (PP-1, PP-2), RD-16 (NG-01, NG-02, NG-03), NFR-SEC-006, DES-05 §12.3 |

## Context
- The Product Owner asked for "system design for future AI implementation" (2026-11-21). The Stage A brief says: "Do not make large LLMs mandatory" and "Cloud AI must not be introduced without explicit product authorization, privacy review and a secure implementation design."
- **R1 and R2 ship with no AI.** D-04 recommends removing cloud AI from R1 (ADR-003). RD-16 NG-01 (cloud AI / LLM evaluation, chat or generation), NG-02 (AI tutor or chatbot) and NG-03 (phoneme scoring) are non-goals for R1–R2. ADR-003 states that any future cloud or on-device LLM feature needs a new ADR, a privacy review (SEC-01 §9.7) and explicit authorisation. ARC-05 §8.4 and ARC-02 §3.2 say an on-device generative rewording of tips "would need its own ADR". This is that ADR, for R3 and later.
- The deterministic pipeline (ARC-05) is honest but cannot judge **content** (relevance, depth), and its transcripts depend on the platform recogniser, which is weak or online-only on many devices (RSK-01).
- Platform options exist (VERIFIED on the cited pages on 2026-11-21; re-verify at spike time): **ML Kit GenAI** runs Gemini Nano on-device through Android AICore (Summarization, Proofreading, Rewriting, Image description, Speech recognition, Prompt API); device support is limited and varies by feature; inference only in the foreground; excess requests return `BUSY`; "input, inference, and output data is processed locally"; no per-call server cost. GenAI **speech recognition** has a basic mode (traditional on-device model, API 31+) and an advanced mode (Gemini Nano devices); it is alpha. **Firebase AI Logic hybrid inference** (`firebase-ai` + `firebase-ai-ondevice`, experimental, no SLA) can prefer on-device and fall back to cloud Gemini, or the reverse; it needs App Check enforcement, a model availability check, a download and a warm-up.
- Sources: https://developers.google.com/ml-kit/genai · https://developers.google.com/ml-kit/genai/speech-recognition/android · https://firebase.google.com/docs/ai-logic/hybrid/android/get-started · https://developer.android.com/ai/hybrid

## Problem
Should SpeakCraft ever add AI, and if so in what form, so that AI (a) stays optional, (b) never changes Measured or Estimated metrics, scores, pass/fail, streaks or progress (PP-1, D-02, D-06), (c) never sends audio and sends text to the cloud only with separate consent (PP-2), (d) holds no API keys in the app, and (e) does not disturb the R1/R2 plan?

## Options

| Option | Summary | Honesty (PP-1) | Privacy (PP-2) | Security | Cost / effort | Assessment |
|---|---|---|---|---|---|---|
| **A. No AI ever** | Keep the deterministic coach only; close NG-01 permanently | Strongest | Strongest | Smallest surface | None | Safe but forgoes better transcripts (UC-AI-1) and content feedback; can be chosen at D-37 |
| **B. Optional on-device AI layer only** | ML Kit GenAI (AICore/Gemini Nano) for tip rewording (UC-AI-2) and "Possible" wording suggestions (UC-AI-3); optional on-device ASR backend (UC-AI-1) under ADR-009/SPK-03. Augmentation after `FeedbackComposer`/`TipSelector`, labelled "AI suggestion", validated, grounded | Preserved by design: separate card, validator, no numbers, no write path to metrics | No data leaves the device; model downloads are system-managed | No cloud SDK; `firebase-ai` stays banned (SEC-REQ-28); ML Kit GenAI artifacts need allow-listing in TC-SEC-010 | ≈ 37–53 d † plus spikes; reach limited to AICore devices (RSK-21) | **Recommended first** |
| **C. Optional hybrid on-device + cloud through Firebase AI Logic with enforced App Check** | B plus cloud Gemini for answer-content feedback (UC-AI-4), only for signed-in users with per-feature consent; `ONLY_ON_DEVICE` unless consent is valid | Preserved with the same controls; cloud output validated identically | Minimised text (transcript + prompt context) leaves the device after consent; audio never; data-safety update; provider "no training" terms required | Re-adds `firebase-ai`; amends SEC-REQ-28, TC-SEC-010, NFR-SEC-006; App Check (Play Integrity) enforced for AI Logic; quotas; budget alerts; kill switch | B + ≈ 10–15 d †; per-token cost (formula in ARC-06 §10.2); requires Play distribution (D-03) | **Only after** a privacy review, SPK-05, D-38 and D-39 |
| **D. Cloud through a backend proxy** | Client → SpeakCraft function (holds provider credentials, enforces quotas, strips identifiers) → model provider | As C | As C, plus a SpeakCraft server in the data path | Most server-side control, but the project's **first backend** (contradicts ARC-04 P-1, SEC-01 §1.2) | Highest: backend, CI/CD, on-call, hosting | Future alternative to C; needs its own ADR |

## Decision (proposed)
**Option B first; option C only after a privacy review (SEC-01 §9.7) and decisions D-38 and D-39. Option D is not proposed now.**

1. **Nothing changes in R1/R2.** ADR-003 stands; `firebase-ai` and the dormant cloud service are removed; NG-01/02/03 stay non-goals. Only the near-zero-cost hooks in ARC-06 §11 (a no-op `FeedbackEnhancer` seam, reserved enum values, a reserved "AI suggestion" design-system term, a purpose-keyed consent store generalised from T3a, an import rule) may be added, through normal ARC-02/03/04 and DES-05 approval. They add no dependency and no behaviour.
2. **At the R3 scope review (D-37)**, approve evidence-first spikes: SPK-03 re-scoped to include ML Kit GenAI speech recognition, and SPK-04 (on-device tip rewording quality and latency). Build only after each feature passes its AG-0 gate (ARC-06 §9.5).
3. **Architecture rules (binding if approved):**
   - The ARC-05 pipeline stays authoritative and runs first; AI output is advisory text in a separate "AI suggestion" card below Coach tips, never written to ENT-01…18 and never read by evaluation, plan, progress or pass/fail code (enforced by an import rule).
   - The default provider is `NoAiProvider`; every feature is opt-in and individually switchable; every feature falls back to the deterministic output.
   - Outputs are schema-constrained JSON, validated mechanically (grounding of every quote in the transcript, no new numbers, issue must be one the deterministic checks raised, banned terms, safety, length) and dropped on failure.
   - Audio never goes to any AI provider. Option B uses ML Kit GenAI directly (not Firebase AI Logic), so no cloud SDK enters the classpath.
   - No AI in no-account mode except on-device features the user opts into; cloud AI requires sign-in (D-41).
   - No API keys in the app; any cloud path uses Firebase AI Logic with App Check enforced from the first cloud build, or an approved proxy.
   - Model versions are recorded per suggestion; cloud models are pinned; unknown on-device model versions run a canary; a three-layer kill switch exists.
4. **Each new dependency** (ML Kit GenAI artifacts, `genai-speech-recognition` or a TFLite/Whisper-class model, `firebase-ai`, `firebase-ai-ondevice`, Remote Config) requires its **own** ADR approval before implementation.

## Consequences
- The product keeps working fully without AI on every device; AI improves the experience only where available and chosen.
- Proposed IDs (owned by ARC-06 until adopted): CMP-77…92, ENT-19/20, THR-23…38, AI-SEC-01…23, TC-AI-001…032, SPK-04/05, EP-28, RSK-19…28, D-37…D-41.
- Option B: SEC-REQ-28's wording ("Generative AI clients") and TC-SEC-010 need an allow-list for approved on-device artifacts; DES-05 gains the "AI suggestion" term and badge (and the G-7 grep allow-list); ARC-03 gains ENT-19/20 in `sensitive.db` (never synced, excluded from backup) and optional export fields (formatVersion stays 1); SEC-01 gains inventory rows, threats, requirements and tests.
- Option C (if later approved): ADR-003 is marked "amended by ADR-016 for R3+"; SEC-REQ-28, TC-SEC-010, NFR-SEC-006 and ARC-04 §2 row 4 are revised; SEC-01 §9.6 data-safety inputs declare user-generated text as shared, optional (D-25 pattern); cloud AI works only on Play-installed, attested builds (App Check with Play Integrity), so it depends on D-03/EP-25.
- Option D (if ever chosen): ARC-04 P-1 and SEC-01 §1.2 change; a backend ADR, threat model and operations plan are required.
- Capacity: EP-28 competes with EP-25…27 in R3 and with single-developer capacity (RSK-06).

## Risks

| Risk | Mitigation |
|---|---|
| AI suggestions wrong or ungrounded, eroding trust (RSK-19) | Mechanical grounding and number rules; separate labelled card; golden sets and human rubric; AG gates; kill switch |
| Users read AI text as measured results (RSK-20) | "AI suggestion" badge and caveat; no numbers; placement below Coach tips; copy testing in beta |
| Few target devices support AICore/Gemini Nano (RSK-21) | Honest "Not available on this phone"; deterministic path complete; don't build if SPK-04 coverage is negligible |
| Alpha/beta/experimental APIs change (RSK-22) | One `AiProvider` interface; flags; kill switch; no core dependency |
| Silent cloud fallback in hybrid mode (THR-33) | `ONLY_ON_DEVICE` unless a valid cloud consent exists; option B keeps cloud SDKs off the classpath |
| Cloud cost abuse (RSK-23, THR-27) | App Check enforced; signed-in only; client and server quotas; budget alerts; kill switch |
| Provider terms change (training, retention) (RSK-24) | Provider-terms checklist per release; disable on adverse change; re-consent |
| Recogniser "corrects" grammar or drops fillers, hiding mistakes (UC-AI-1) | SPK-03 error-preservation and filler-retention gates; verbatim mode only |
| AI work displaces R1/R2 quality (RSK-25) | Nothing before the R3 decision point; spikes first; one epic at a time |

## Approval status
**Proposed — requires explicit approval (D-37…D-41); not in R1/R2 scope.** Pending — Product Owner. Approval of this ADR also requires the SEC-01 §9.7 privacy review for each data flow, a data-safety update, a PRD scope change at G1 for R3 (PLN-05 GR-05) and separate dependency ADRs. Rejecting D-37 closes this ADR as "Rejected — retained for reference".
