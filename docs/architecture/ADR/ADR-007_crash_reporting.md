# ADR-007 — Crash and stability reporting

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** |
| Date | 2026-11-20 |
| Owner (role) | DevOps / Release Engineer |
| Related | ENG-01 §9, NFR-REL-*, RD-03 FND-OPS-05, FND-SEC-08, ADR-004 |

## Context
There is no crash reporting, no logging framework, and 84 raw `Log.*` calls, some containing PII (FND-SEC-08). Production failures are invisible.

## Options
| Option | Pros | Cons |
|---|---|---|
| **A. Google Play Console Android vitals** (if distributed via Play, ADR-004 B) | No SDK; crash/ANR rates and stack traces; no extra data collection by the app | Requires Play distribution; less context than an SDK |
| B. Firebase Crashlytics | Rich reports, non-fatal logging, works for sideloaded builds | **New dependency**; data-processing disclosure; must scrub PII |
| C. Local diagnostic log only (user-exportable) | Private | Relies on users sending logs |

## Decision (proposed)
- **R1 beta:** Option C (a rolling, PII-free local diagnostic log the user can export from Settings → About → "Share diagnostics") plus Option A as soon as Play internal testing is used.
- **Crashlytics (B)** only if A + C prove insufficient — separate approval, opt-in, PII-scrubbed.
- Introduce a small `AppLogger` wrapper that strips PII and is a no-op for verbose levels in release.

## Approval status
Pending — Product Owner (decision D-13).
