# ADR-014 — Sync and conflict-resolution model

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** |
| Date | 2026-11-20 |
| Owner (role) | Database Architect |
| Related | ARC-03 §7–8, FS-11, ADR-002, ADR-005, RD-03 FND-DATA-02, FND-DATA-03, FND-SEC-10 |

## Context
Today, some learning state never syncs (local JSON), other state is written straight to Firestore with awaited writes that hang offline, and counters (streak, minutes, scores) are client-computed read-modify-write values that race and can be set arbitrarily (FND-DATA-03, FND-SEC-10). PO-REQ-1119 requires idempotent completion events and defined multi-device conflict rules.

## Decision (proposed)
1. **Local first.** All writes go to Room in a transaction and return immediately. A `sync_outbox` row is created for each syncable change.
2. **Append-only facts, derived aggregates.** Attempts, word-stage completions and review results are immutable records with client-generated UUIDs (idempotency keys). Streak days, totals and trends are **derived** locally from facts — never stored as authoritative counters in the cloud.
3. **Mutable documents** (profile, preferences, vocabulary item state, daily plan) use per-document `updatedAt` + `deviceId`; conflict rule **last-writer-wins per document**, except vocabulary item state, which merges by taking the more advanced stage and the later review schedule.
4. **Upload**: `SyncWorker` drains the outbox in batches when signed in and online; Firestore writes are not awaited on UI paths.
5. **Download**: on sign-in and app start, pull documents changed since the last sync watermark (`updatedAt > lastPull`) with bounded queries; merge into Room using the rules above.
6. **What syncs** by default: profile, preferences, plans, attempt *metrics*, vocabulary progress, streak facts. **What never syncs by default**: audio, transcripts (opt-in setting in R2, D-16).
7. **First sign-in** adopts local data: local profile id is re-keyed to the Firebase uid and all local facts are uploaded.

## Consequences
Security rules validate immutable fact documents (create-only, schema-checked); aggregates cannot be forged in the cloud because they are not trusted there.

## Approval status
Pending — Product Owner.
