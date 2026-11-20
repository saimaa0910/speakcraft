# ADR-013 — Product success measurement and analytics

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** |
| Date | 2026-11-20 |
| Owner (role) | Product Manager / Security Engineer |
| Related | BRD §8, RD-14 RSK-07, D-09, PP-2 |

## Context
No analytics exist. BRD success measures (activation, practice days, feedback-accuracy rating) cannot be observed without some measurement. The product is privacy-first.

## Options
| Option | Summary | Privacy |
|---|---|---|
| **A. No SDK; structured beta programme** — recruited testers, in-app "Was this accurate?" ratings stored locally, a user-initiated "Share my beta summary" export (aggregate counts only), interviews | No continuous data flow | Highest |
| B. Firebase Analytics, opt-in, minimal event set | Continuous metrics | New dependency; Google data processing; requires consent and disclosure |
| C. Signed-in users' own aggregate documents in Firestore read by an admin export script | Re-uses sync | Requires admin tooling and disclosure; aggregates of personal data |

## Decision (proposed)
**Option A for R1 beta.** Revisit B (strictly opt-in, no audio/transcript-derived content, documented event dictionary) before any public store launch, under a new approval.

## Approval status
Pending — Product Owner (decision D-09).
