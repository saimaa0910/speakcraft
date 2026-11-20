# ADR-011 — Remove YouTube Intelligence and the creator-derived content pipeline

| Field | Value |
|---|---|
| Status | **Proposed — requires explicit approval** |
| Date | 2026-11-20 |
| Owner (role) | Solution Architect / Product Manager |
| Related | RD-16 NG-08, PP-8, RD-03 FND-AI-11, FND-DOM-09, D-08 |

## Context
`YouTubeIntelligenceClient` scrapes watch pages with a spoofed browser user-agent, reads undocumented caption endpoints, optionally uses a user-supplied Data API key, and publishes verbatim caption sentences to Firestore as "PUBLISHED" techniques with synthetic timestamps (FND-AI-11). A primary navigation tab is labelled with the creator's handle; the curriculum references the creator 102 times with "VERIFIED" labels pointing to the channel root (FND-DOM-09). No licence or permission is recorded.

## Decision (proposed)
Remove the YouTube client, screen, admin sync, shared collections (`youtubeChannels`, `youtubeVideos`, `videoTranscripts`, shared `extractedTechniques`) and their rules; remove creator branding and creator-attributed content from the app in R0. Retained frameworks (e.g. generic structures such as STAR or PREP) are rewritten as original content; anything clearly derived from the creator is excluded pending legal review (D-08).

## Consequences
Loss of a curated content source; content workstream EP-09 must supply original material. Removes ToS/IP exposure and a fragile network dependency.

## Approval status
Pending — Product Owner (decision D-08).
