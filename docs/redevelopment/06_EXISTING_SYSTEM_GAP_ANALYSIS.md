# 06 — Existing System Gap Analysis

| Field | Value |
|---|---|
| Document ID | RD-06 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-03, RD-04, RD-05, PRD, SRS, PLN-01, PLN-02 |

---

## 1. Purpose and method

This document lists the gaps between the **current system** (RD-03, RD-04) and (a) what the Product Owner asked for (PO-REQ-1119, PO-REQ-1120), (b) what the documentation claims (RD-05), and (c) the **target product** (PRD/SRS). Each gap has a priority and the release that closes it. **P0** gaps are unsafe, broken, legally exposed, or block the core loop. Safety, legal, data-rights and foundation P0 gaps close in **R0**; core-experience P0 gaps (GAP-10…12) close in **R1**, because they require the rebuilt core loop. The R0 exit criterion referenced by the PRD (§20) is defined in §5.

Priority: **P0** critical · **P1** high · **P2** medium · **P3** low.

## 2. Gap summary

| Category | P0 | P1 | P2 | Total |
|---|---|---|---|---|
| Safety, legal and policy | 6 | 2 | 0 | 8 |
| Core experience | 3 | 5 | 1 | 9 |
| Data and persistence | 2 | 4 | 1 | 7 |
| Engineering foundation | 4 | 3 | 2 | 9 |
| Product completeness (vs PO-REQ-1119) | 0 | 7 | 3 | 10 |
| Documentation | 1 | 1 | 1 | 3 |
| **Total** | **16** | **22** | **8** | **46** |

## 3. Gap register

### 3.1 Safety, legal and policy

| ID | Gap (current → required) | Evidence | P | Closed in | Closed by |
|---|---|---|---|---|---|
| GAP-01 | CI can distribute debuggable, debug-signed builds with the App Check debug provider → release-signed, non-debuggable builds only | FND-SEC-02 | P0 | R0 | ENG-01; ADR-004/008; PRD-FR-121 |
| GAP-02 | Creator-branded tab and creator-derived content without permission → original or licensed content only | FND-UI-03, DOM-09 | P0 | R0 (removal) / R1 (replacement) | ADR-011; D-08; EP-09 |
| GAP-03 | YouTube scraping with spoofed user-agent, captions republished → removed | FND-AI-11 | P0 | R0 | ADR-011 |
| GAP-04 | Privacy text contradicted (transcripts to Firestore; network recogniser undisclosed) → true, tested privacy statements | FND-SEC-05, SEC-07 | P0 | R0 (stop transcript upload, correct text) / R1 (tiers + consent) | SRS-PRIV-001/002; ADR-009 |
| GAP-05 | Recordings eligible for cloud backup → excluded | FND-SEC-06 | P0 | R0 | ADR-015; SEC-01 §6 |
| GAP-06 | PII logged in release → redacted | FND-SEC-08 | P0 | R0 | SEC-01 §10 |
| GAP-07 | Self-updating APK channel (Play-incompatible, likely broken install) → removed | FND-OPS-04 | P1 | R0 | ADR-004; D-03 |
| GAP-08 | Dormant cloud-AI SDK and code (no consent flow) → removed | FND-AI-10 | P1 | R0 | ADR-003; D-04 |

### 3.2 Core experience

| ID | Gap | Evidence | P | Closed in | Closed by |
|---|---|---|---|---|---|
| GAP-10 | No transcript by default; file transcription stubbed; live transcription single-utterance and duplicating → continuous transcript per tier | FND-AI-01…04 | P0 | R1 (SPK-01 in R0) | ADR-009; FS-04, FS-12 |
| GAP-11 | Mislabelled/fabricated scores (pronunciation proxy, grammar baseline 78, length-based activity scores, fallback 62) → metric catalogue with Measured/Estimated labels, nulls, no composite for free speaking | FND-AI-08, DOM-04, UI-07 | P0 | R0 (hide fabricated displays) / R1 (new pipeline) | ARC-05; SRS-BR-002…006 |
| GAP-12 | Activities complete without speaking; self-ticked plans and steps → completion only from valid attempts | FND-DOM-05, DOM-06 | P0 | R1 | SRS-BR-008/009 |
| GAP-13 | 26 routes, ~10 overlapping labs, 3 plans, dense dashboard → 4 destinations, one player, one plan | FND-UI-01/02/04, DOM-01 | P1 | R1 | DES-02; FS-03, FS-04 |
| GAP-14 | Mic-permission denial invisible; loading/error collapsed to empty → explicit states everywhere | FND-UI-05 | P1 | R1 | DES-03 state matrix |
| GAP-15 | State lost on rotation; recording leaks across screens → saved state; single recording owner | FND-UI-06 | P1 | R1 | ARC-01 §6 |
| GAP-16 | Grammar/filler rule false positives → precision-qualified rules | FND-AI-09 | P1 | R1 | SPK-02; NFR-QUAL-003 |
| GAP-17 | No interruption handling; failed recordings never cleaned → safe interruption; retention | FND-AI-14 | P1 | R1 | SRS-FR-SESS-010; ADR-015 |
| GAP-18 | Tutorial overload and developer jargon → contextual tips; plain language | FND-UI-08/11 | P2 | R1 | DES-06 §2.4; DES-05 §12 |

### 3.3 Data and persistence

| ID | Gap | Evidence | P | Closed in | Closed by |
|---|---|---|---|---|---|
| GAP-20 | Speaking-session saves denied by rules (HEAD regression) → working persistence | FND-DATA-06 | P0 | R0 (rules/payload alignment or new data path) | ARC-03; SEC-01 §5 |
| GAP-21 | Account deletion unreachable; no export; no local cleanup → full data-rights flows | FND-SEC-03, SEC-09 | P0 | R0 (wire deletion with re-auth) / R1 (export, clear, resumable) | FS-10; SRS-FR-ACC-007/008 |
| GAP-22 | Three persistence mechanisms, local JSON never synced → Room source of truth + sync | FND-DATA-01/02 | P1 | R0–R1 | ADR-002, ADR-014 |
| GAP-23 | Offline writes hang → local-first writes | FND-DATA-03 | P1 | R1 | SRS-FR-DATA-001 |
| GAP-24 | Four streaks, fabricated counters → derived, evidence-only statistics | FND-DOM-07/08 | P1 | R1 | SRS-BR-010…018 |
| GAP-25 | Absolute device paths in cloud; client-writable aggregates → none in cloud | FND-DATA-04, SEC-10 | P1 | R1 | ARC-03 §8 |
| GAP-26 | 16+ unbounded listeners → bounded watermark pulls | FND-DATA-05 | P2 | R1 | ARC-03 §8 |

### 3.4 Engineering foundation

| ID | Gap | Evidence | P | Closed in | Closed by |
|---|---|---|---|---|---|
| GAP-30 | CI runs no tests, lint or rules tests; releases on every push → gated PR + tag-driven release | FND-OPS-01 | P0 | R0 | ENG-01 §4 |
| GAP-31 | Gradle wrapper missing; local build needs undocumented `google-services.json` → reproducible builds | FND-OPS-02 | P0 | R0 | ENG-01 §3 |
| GAP-32 | DI container rebuilt on recreation; no Application → process-scoped container | FND-ARCH-02 | P0 | R0 | ADR-001; ARC-01 §2 |
| GAP-33 | No interfaces/domain layer; god objects → package-by-feature with interfaces | FND-ARCH-01/03 | P0 | R0 (structure) / R1 (features) | ADR-012; ARC-02 |
| GAP-34 | No background work → WorkManager | FND-OPS-05 | P1 | R1 | ADR-005 |
| GAP-35 | Dependency drift, unused libraries, R8 off → current, minimal, minified | FND-OPS-06, SEC-11 | P1 | R0 | ADR-006 |
| GAP-36 | No crash visibility → diagnostics log + Play vitals | FND-OPS-05 | P1 | R1 | ADR-007 |
| GAP-37 | Tests encode false assurance; core paths untested → meaningful suites | FND-TEST-02/03 | P2 | R0–R1 | TST-01 |
| GAP-38 | Supply-chain hygiene (unpinned actions, npm without lockfile) | FND-OPS-03 | P2 | R0 | ENG-01 §11 |

### 3.5 Product completeness versus PO-REQ-1119

| ID | PO-REQ-1119 phase | Current state | P | Closed in | Closed by |
|---|---|---|---|---|---|
| GAP-40 | Ph1 Welcome-back experience | One text line; "welcome back" on first visit | P2 | R1 | SRS-FR-TODAY-010 (simplified per PP-3) |
| GAP-41 | Ph2 Personalised daily plan | Three plans; goal ignored; self-ticked | P1 | R1 | FS-03 |
| GAP-42 | Ph3 Real-time dashboard | Fabricated fallbacks; inconsistent numbers | P1 | R1 | FS-03, FS-08 (reactive Room flows) |
| GAP-43 | Ph4 Streak engine | Four streaks; stale; gameable | P1 | R1 | FS-08 |
| GAP-44 | Ph5 Notification centre | In-app only; duplicates; no OS reminders | P1 | R1 (reminders) / R2 (inbox) | FS-09 |
| GAP-45 | Ph6 Profile & settings (incl. delete data) | Partial; no deletion/export | P1 | R1 | FS-10 |
| GAP-46 | Ph7 Admin console | Client-gated admin with over-broad rules | P2 | Descoped (NG-10) | — documented as missing backend requirement per PO-REQ-1119 Ph7 |
| GAP-47 | Ph8 Vocabulary builder 5 stages | Built but dead; 20 words | P1 | R1 | FS-07; EP-09 content |
| GAP-48 | Ph9 Quiz, flashcards, notebook | Quiz dead; notebook split | P2 | R2 | FS-07 R2 |
| GAP-49 | Ph10 Unified progress & events | Event model exists but unused | P1 | R0–R1 | ADR-014; FS-11 |

### 3.6 Documentation

| ID | Gap | Evidence | P | Closed in |
|---|---|---|---|---|
| GAP-50 | 59 documents with 54 conflicts; "verified" claims without evidence → one authoritative set with control | RD-01, RD-05 | P0 | Stage A (this set) + archive move after approval |
| GAP-51 | 10 documents required by PO-REQ-1119 do not exist | CONF-044 | P1 | Superseded by Stage A set (GOV-02 §6) |
| GAP-52 | No developer onboarding/setup guide | FND-OPS-02 | P2 | R0 (ENG-01 §3 → README) |

## 4. What is not a gap (keep as is)

| Item | Why |
|---|---|
| Kotlin/Compose/M3 foundation, adaptive layout tiers | Sound (RD-02 §5) |
| Credential Manager Google sign-in | Correct implementation |
| Evidence-gate design | Honest, tested (FND-AI-17) |
| Firestore per-user isolation patterns | Strong (RD-03 §8 "Preserve") |
| Tokenised colour roles | Good practice (FND-UI-09) |

## 5. R0 exit criterion

R0 is complete when GAP-01…06, GAP-10 (spike only: SPK-01 report), GAP-11 (fabricated displays removed), GAP-20, GAP-21 (deletion reachable), GAP-30…33 and GAP-50 are closed and verified (TST-01 §10).
