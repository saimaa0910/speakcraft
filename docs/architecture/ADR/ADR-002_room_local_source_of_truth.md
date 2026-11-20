# ADR-002 — Room as the local source of truth for learning data

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** (adds a dependency to the app) |
| Date | 2026-11-20 |
| Owner (role) | Database Architect |
| Related | ARC-03 Database Design, ADR-014, RD-03 FND-DATA-01…03, FND-DATA-08, PO-REQ-1119 Phase 3 ("Use Room or the existing local persistence mechanism for offline progress") |

## Context
Learning data is spread across three mechanisms with no single source of truth (FND-DATA-01):
- Firestore collections written with `await()` on UI paths, so offline writes hang (FND-DATA-03).
- Hand-rolled JSON files (`learning_state/…json`, `notification_store/…json`, `mentor_history_…json`) written synchronously on the main thread (FND-DATA-08), never synced, lost on reinstall (FND-DATA-02).
- DataStore preferences.

Room 2.7.0 is already declared in `gradle/libs.versions.toml` and the KSP plugin is already applied, but neither is used (RD-02 §3).

## Problem
The redesigned product is offline-first and account-optional (D-05). It needs transactional, queryable, migratable local storage for attempts, metrics, plans, vocabulary progress, streak days and an idempotent event log — independent of whether the user is signed in.

## Options
| Option | Pros | Cons |
|---|---|---|
| **A. Room (SQLite) as source of truth; Firestore as sync target** | Transactions, indexes, typed queries, Flow observation, schema migrations with tests; works without an account; already in the catalog with KSP applied | New runtime dependency (+ KSP processor); migration code to maintain |
| B. Firestore offline cache as the only store | No new dependency | Requires sign-in (conflicts with D-05); cache is a cache (eviction, no guaranteed retention of unsynced reads), limited queries, writes block UI if awaited |
| C. Improve DataStore + JSON files | No new dependency | No transactions or queries; whole-file rewrites; concurrency bugs already present; poor scalability for attempt history |

## Decision (proposed)
**Option A.** Room becomes the single local source of truth for learning data. DataStore remains for preferences. Firestore becomes a sync target for signed-in users (ADR-014). All JSON-file stores are retired after migration.

## Consequences
- Add `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) using the catalog versions (version upgrade evaluated under ADR-006).
- Exported schema JSON committed under `app/schemas/` and migration tests in CI.
- Repositories expose `Flow` from Room; ViewModels never call Firestore directly.
- Local data must be partitioned by profile (local profile id → Firebase uid after sign-in) and wiped on account deletion (SEC-01).

## Risks
| Risk | Mitigation |
|---|---|
| Schema mistakes requiring migrations | Start at version 1 with a reviewed schema (ARC-03); auto-migrations where possible; migration tests |
| Database encryption expectations | Data lives in app-private storage; device encryption applies. SQLCipher is **not** proposed (another dependency); revisit only if a threat model requires it (SEC-01 §6) |

## Approval status
Pending — Product Owner (decision D-11). If rejected, fall back to Option C with a single serialized-store abstraction; offline-first guarantees weaken and account-optional mode becomes limited.
