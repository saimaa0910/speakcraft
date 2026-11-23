# Content pack — build support (not shipped)

The curriculum ships as JSON in `app/src/main/assets/content/` (ARC-03 §6: `techniques.json`, `tips.json`,
`activities/trk-*.json`, `manifest.json`). This folder holds files used to build and review it.

| File | Purpose |
|---|---|
| `branding-blocklist.txt` | Creator names, handles and creator-coined framework names that must not appear in any displayed field (V-14). Used by `ContentPackAssetsTest` and `TipPackLoader` |
| `tools/update-manifest.ps1` | Recomputes `manifest.json` counts and SHA-256 checksums after a content edit (V-19). Use `-BumpVersion` for a change that ships |

## Status of pack version 1 (2026-11-23)

- 15 techniques, 86 activities and 111 coach tips. They were re-expressed in SpeakCraft's own words from the
  app's earlier hard-coded curriculum (`CurriculumCatalog.kt`), following D-08 (original wording, no creator
  name or branding, link out only to a specific video).
- **Every item is `PENDING_REVIEW`.** No source video id, link or timestamp was added, because none was known
  and sources are never invented. Debug builds use the pack; release builds reject it (V-10) and keep the
  built-in tips until review is complete.

## Review workflow (per item)

1. Watch the specific source video by hand (no scraping or caption download; ADR-011).
2. Check that the wording is SpeakCraft's own and that nothing displayed names the creator or a coined framework.
3. Fill `provenance.sourceVideoId`, `sourceUrl` (a `watch?v=`, `youtu.be/` or `shorts/` link to that video),
   `timestampStart` (and optionally `timestampEnd`), `verifiedBy` (pseudonymous reviewer id) and `verifiedOn`.
4. Set `licenceStatus` to `ORIGINAL_REEXPRESSION` (or `ORIGINAL` for text written from scratch; it still needs
   `verifiedBy`/`verifiedOn`).
5. Run `content/tools/update-manifest.ps1 -BumpVersion`, then `./gradlew :app:testDebugUnitTest`
   (`ContentPackAssetsTest` and `TipPackLoaderTest` enforce the rules).
