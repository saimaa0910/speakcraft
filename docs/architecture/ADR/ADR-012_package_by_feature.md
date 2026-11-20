# ADR-012 — Package-by-feature structure inside the single `:app` module

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** |
| Date | 2026-11-20 |
| Owner (role) | Android Technical Lead |
| Related | ARC-01 §3, ARC-02 §2, RD-03 FND-ARCH-01…05 |

## Context
48,894 lines in one module under `com.example` (placeholder namespace) organised by layer (`data/…`, `ui/screens`, `ui/components`, `ui/viewmodel`), with god files of 1,300–1,950 lines and no domain layer or interfaces (FND-ARCH-01, FND-ARCH-03).

## Options
1. **Package-by-feature within `:app`** with enforced layering conventions.
2. Gradle multi-module split (`:core:*`, `:feature:*`).
3. Keep current layout.

## Decision (proposed)
Option 1 now; Option 2 only if build times or team size demand it (revisit at > 60k lines or > 2 contributors).

Structure (namespace `app.speakcraft` replacing `com.example`; **applicationId change is decision D-15** because it affects existing installs and Firebase registration):

```
app.speakcraft
├── app/          Application, AppContainer, MainActivity, navigation graph
├── core/         designsystem, ui (shared composables), common (time, result, logging), testing
├── data/         local (Room), remote (Firestore), prefs (DataStore), sync, content (bundled packs)
├── speech/       capture, recognition, analysis (metrics), evaluation (rules, feedback)
└── feature/      onboarding, today, session, practice, words, progress, profile, settings, reminders
```

Rules: features depend on `core`, `data` and `speech` interfaces, never on other features; composables never touch repositories; files > 600 lines require justification in review.

## Approval status
Pending — Product Owner.
