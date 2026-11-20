# 12 — Product Opportunity Analysis

| Field | Value |
|---|---|
| Document ID | RD-12 |
| Version | 0.2 — Draft for review (2026-11-21: OPP-15 decision superseded by Product Owner direction / D-08) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | CPO / Product Manager |
| Approval | Pending — Product Owner |
| Related | RD-07, RD-08, RD-10, RD-11, RD-13, RD-14 |

---

## 1. Purpose

To identify where SpeakCraft can create distinctive user value, given (a) what the audit says the product can realistically deliver on the existing stack, (b) what the market under-serves (RD-11), and (c) the Product Owner's constraints. Each opportunity is scored and linked to the strategy.

## 2. Opportunity scoring

Scores 1–5: **User need** (how strongly personas need it), **Market gap** (how poorly competitors serve it, RD-11), **Feasibility** (on-device, existing stack), **Fit** (with constraints and positioning), **Effort** (5 = small). Total = sum.

| ID | Opportunity | Need | Gap | Feasibility | Fit | Effort | Total | Evidence | Decision |
|---|---|---|---|---|---|---|---|---|---|
| OPP-01 | **Honest, transparent feedback** (Measured/Estimated, Inconclusive, "how we measured", no fake composite) | 5 | 5 | 5 | 5 | 4 | **24** | RD-11 D2, I3; FND-AI-17 | **Core (P-2)** |
| OPP-02 | **Verifiable privacy & offline** (on-device by default, account optional, consented exceptions, export/delete) | 4 | 5 | 4 | 5 | 3 | **21** | RD-11 D1, I1, I8 | **Foundation (F-1, F-2)** |
| OPP-03 | **Professional communication for non-native speakers** (meetings, interviews, presentations; structure checks like STAR/PREP) | 5 | 4 | 4 | 5 | 3 | **21** | RD-11 D5, I4; FND-DOM-13 | **Core content (CAP-06)** |
| OPP-04 | **Ten-minute daily session** with deterministic personalisation | 5 | 2 | 5 | 5 | 4 | **21** | PO-REQ-1119 Ph2; RD-11 table stakes | **Core (P-1)** |
| OPP-05 | **Delivery metrics that are robust on-device** (pace, pauses, fillers, length, hedging, lexical variety) | 4 | 4 | 4 | 5 | 4 | **21** | RD-11 D4 | **Core (ARC-05)** |
| OPP-06 | **Vocabulary for production** (say it, use it, make your own; spoken review) | 4 | 4 | 4 | 5 | 3 | **20** | RD-11 D8; PO-REQ-1119 Ph8 | **Pillar P-3** |
| OPP-07 | **Evidence-based progress & benchmark check-ins** | 4 | 5 | 4 | 5 | 3 | **21** | RD-11 D6, I6 | **Pillar P-4 (check-ins R2)** |
| OPP-08 | **Low-end & low-bandwidth first** (small app, offline after install) | 3 | 4 | 4 | 4 | 3 | 18 | RD-11 D7 | NFR-PERF-010, F-2 |
| OPP-09 | Scripted role-play conversations | 4 | 2 | 4 | 4 | 3 | 17 | Category norm (cloud LLM chat) — honest scripted alternative | R2 |
| OPP-10 | Video self-review (no automated scoring) | 3 | 3 | 3 | 3 | 2 | 14 | Public speaking need; FND-AI-12 | R3 (conditional) |
| OPP-11 | Transparent monetisation (e.g. one-time unlock) | 3 | 4 | 5 | 4 | 3 | 19 | RD-11 D9 | D-07 (out of scope now) |
| OPP-12 | Open-ended AI conversation partner | 4 | 1 | 1 | 1 | 1 | 8 | RD-11 §6 "should not compete" | **Rejected** (NG-01/02) |
| OPP-13 | Phoneme-level pronunciation scoring | 4 | 1 | 1 | 2 | 1 | 9 | RD-11 §6 | **Rejected** (NG-03) |
| OPP-14 | Exam score prediction | 3 | 2 | 1 | 1 | 2 | 9 | RD-11 §6 | **Rejected** (NG-04) |
| OPP-15 | Creator-based content library | 3 | 2 | 3 | 1 | 3 | 12 | FND-DOM-09 (IP) | ~~Rejected (PP-8)~~ **Superseded 2026-11-21 by Product Owner direction:** the channel's techniques form the curriculum, re-expressed in original wording with source-video links and no branding (D-08 decided; PRD-FR-056/057). The scraping/branding form scored here stays rejected |

## 3. Opportunity map

```
           High market gap
                 ▲
   OPP-02 ●      │      ● OPP-01
   OPP-07 ●      │      ● OPP-03, OPP-05
                 │      ● OPP-06
  ───────────────┼──────────────────► High feasibility/fit
   OPP-12 ●      │      ● OPP-04 (table stakes, must be excellent)
   OPP-13 ●      │      ● OPP-09
   OPP-14 ●      │
           Low market gap
```

## 4. Synthesis — the opportunity SpeakCraft should own

The intersection of OPP-01, OPP-02, OPP-03 and OPP-05 is unoccupied: **no mainstream product combines ESL-aware professional speaking practice with honest, on-device, privacy-preserving feedback** (RD-11 §4–6). SpeakCraft already holds pieces of it (evidence gate, DSP, professional content, local-only policy). The table-stakes opportunity OPP-04 (daily session) must be executed excellently but is not a differentiator on its own.

## 5. What would make the opportunity fail

| Failure mode | Mitigation |
|---|---|
| Platform recognition too poor on target devices | SPK-01 before building; tiered experience; honest audio-only mode |
| "Honest" feels "less impressive" than competitors' big scores | Make specificity and transcripts the hero; test ASM-04 in beta |
| Content too thin to sustain daily use | ≥ 15 activities per track and ≥ 300 words for R1; R2 expansion |
| Privacy claim broken by a future feature | Principles PP-2, NFR-PRIV tests in CI, ADR process |

## 6. Recommendations

1. Position on OPP-01 + OPP-02 + OPP-03 (RD-13 §7).
2. Treat OPP-04 and OPP-05 as the core loop's engine (RD-14 §4).
3. Sequence OPP-06 into R1 (Words tab) and OPP-07/09 into R2 (D-20).
4. Explicitly reject OPP-12…OPP-14 (RD-16 §4). OPP-15 is superseded: the channel's techniques form the curriculum under D-08 (decided 2026-11-21), while scraping and creator branding stay rejected.
