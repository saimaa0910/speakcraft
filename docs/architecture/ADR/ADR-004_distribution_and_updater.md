# ADR-004 — Distribution channel and the in-app APK updater

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** (product + platform decision) |
| Date | 2026-11-20 |
| Owner (role) | DevOps / Release Engineer |
| Related | ENG-01, SEC-01, RD-03 FND-OPS-04, FND-SEC-02, ADR-008, PO-REQ-1119 Phase 5 ("do not include APK update alerts … automatic app update workflows" in notifications) |

## Context
- Every push to `main` publishes a GitHub Release; beta builds fall back to `assembleDebug` signed with a debug keystore when no release keystore secret exists (FND-SEC-02).
- The app contains a self-updater (GitHub Releases + Firestore `applicationReleases`) with SHA-256 and certificate checks, a **user-editable** source repository field, and copy advising users to uninstall and reinstall on signature mismatch. `REQUEST_INSTALL_PACKAGES` is missing, so installation very likely fails on API 26+ (FND-OPS-04).
- Google Play policy prohibits apps from updating themselves outside Play; Play requires AABs and non-debuggable builds.

## Problem
The release path is unsafe (debuggable builds to users), partly broken (installer) and incompatible with the most likely future distribution channel.

## Options
| Option | Summary | Trade-offs |
|---|---|---|
| A. Keep GitHub sideloading and fix the updater | Add permission, pin repo, release-sign every build | Ongoing security burden; users must allow unknown sources; Play-incompatible |
| **B. Google Play testing tracks → production; remove the updater** | Internal/closed testing for beta, Play App Signing, AAB, Play Integrity | Requires a Play developer account and store listing, privacy policy URL, data-safety form, web account-deletion URL |
| C. Build flavours (`play`, `sideload`) | Both channels | Doubles release testing; keeps the risky code |

## Decision (proposed)
**Option B.** Remove the in-app updater, GitHub release client, update screens/dialogs, `applicationReleases`/`applicationUpdateEvents` collections and the publish script. Until a Play account exists, distribute **release-signed** APKs as GitHub Release downloads with release notes, **without** any in-app installer. CI never ships debug builds.

## Consequences
- R1 beta: GitHub Releases (manual install) or Play internal testing, whichever is available first.
- R3 (or earlier): Play production with Play Integrity (ADR-008) and Play In-App Updates API (would be a new dependency — separate approval).
- OkHttp may become unnecessary once updater, YouTube client and model downloads are removed (ADR-006).

## Risks
Play account and listing are a business dependency (DEP-03); policy review time.

## Approval status
Pending — Product Owner (decision D-03).
