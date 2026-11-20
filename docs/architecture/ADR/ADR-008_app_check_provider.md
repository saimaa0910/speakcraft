# ADR-008 — App Check provider and debug-provider hardening

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** |
| Date | 2026-11-20 |
| Owner (role) | Security Engineer |
| Related | SEC-01 §4, ADR-004, RD-03 FND-SEC-02, FND-SEC-11 |

## Context
`AppCheckConfig` installs the Debug provider when `BuildConfig.DEBUG` and writes a debug secret taken from a launch-intent extra; `MainActivity` is exported; CI may ship debug builds to users. Release builds use the reCAPTCHA provider, whose Android artifact availability could not be verified offline. `firebase-appcheck-debug` ships in release.

## Decision (proposed)
1. Remove the launch-intent debug-token injection entirely; developers register debug tokens through logcat/console as documented by Firebase.
2. `firebase-appcheck-debug` → `debugImplementation`; debug provider code moved to the `debug` source set.
3. Release provider: **Play Integrity** once distributed through Play (ADR-004 B). Until then, App Check stays **un-enforced** for Firestore, and security relies on Firebase Auth + security rules (which are the real control for per-user data).
4. CI never distributes debug builds (ENG-01).

## Consequences
Removes an injection vector and the false sense of protection. Play Integrity requires the app to be linked in Play Console.

## Approval status
Pending — Product Owner.
