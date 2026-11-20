# ADR-011 — Remove YouTube Intelligence and the creator-derived content pipeline

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** |
| Date | 2026-11-20 (revised 2026-11-21) |
| Owner (role) | Solution Architect / Product Manager |
| Related | RD-16 NG-08, PP-8, RD-03 FND-AI-11, FND-DOM-09, D-08 |

## Context
`YouTubeIntelligenceClient` scrapes watch pages with a spoofed browser user-agent, reads undocumented caption endpoints, optionally uses a user-supplied Data API key, and publishes verbatim caption sentences to Firestore as "PUBLISHED" techniques with synthetic timestamps (FND-AI-11). A primary navigation tab is labelled with the creator's handle; the curriculum references the creator 102 times with "VERIFIED" labels pointing to the channel root (FND-DOM-09). No licence or permission is recorded.

## Decision (proposed)
Remove the YouTube client, screen, admin sync, shared collections (`youtubeChannels`, `youtubeVideos`, `videoTranscripts`, shared `extractedTechniques`), the per-user seeded copies (`users/{uid}/extractedTechniques`, `youtubeSources`) and their rules; remove creator branding from the app in R0.

**Revised 2026-11-21 after the Product Owner's direction and decision D-08 ("original wording, link out"):** this ADR now covers **ingestion and branding only**. The channel's frameworks, techniques, exercises, activities and tips are **kept as the curriculum** (PRD-FR-056): curated by hand, re-expressed in our own words and examples, each linked to its specific source video by an outbound link, with no verbatim transcript text and no creator name, likeness or branding (PRD-FR-057, SRS-BR-028). Existing items in `CurriculumCatalog.kt` are verified and migrated into the bundled content pack (CNT-01, ARC-03 §6). No scraping, caption download or YouTube API use in any form.

## Consequences
The automated (and non-compliant) content source disappears; content workstream EP-09 verifies and re-expresses the existing items and adds further techniques by hand. Removes YouTube Terms-of-Service exposure and a fragile network dependency. Residual IP risk is managed by the D-08 rules and the release content checks (TST-01).

## Approval status
D-08 decided by the Product Owner on 2026-11-21. This ADR remains **Proposed — requires explicit approval** (gate G3).
