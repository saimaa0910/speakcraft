# FS-06 — Practice Library

| Field | Value |
|---|---|
| Document ID | FS-06 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-06; PRD §15.6 (PRD-FR-050…057), §14 J3; SRS §6.6 (SRS-FR-PRAC-001…009), SRS-FR-EVAL-016, SRS-FR-WORD-011, SRS §9, SRS-ERR-008, SRS-BR-001/008/010/020/026/028; NFR-MAINT-009, NFR-PERF-001/003/008/010, NFR-A11Y-*; ARC-01 §2 (`ContentRepository`); ARC-03 §6 (content-pack schema, authoritative), ENT-03, ENT-12; ARC-05 §7, §8.4; ADR-011, ADR-014; DES-02 v0.2 (SCR-PRAC-01…04); DES-03; DES-05 §12; PLN-02 EP-09, EP-10, EP-19; FS-03, FS-04, FS-05, FS-07, FS-08, FS-10, FS-11; decision D-08 (DECISION — approved by the Product Owner 2026-11-21: "original wording, link out") |

---

## 1. Feature ID & purpose

**FS-06 / CAP-06 (pillar P-1).** Let people rehearse the situations they care about outside the daily session. The library offers **five tracks** of activities built on **five templates** (R1), a **Techniques** collection in which each communication technique is explained in our own words with its exercises and tips (PRD-FR-056), browse and filter, **save for later**, and a bundled, versioned, validated **content pack** whose every record carries provenance and a licence status (PRD-FR-057, SRS-BR-028). Attempts started here count exactly like Today attempts. R2 adds **scripted role-play** (TPL-ROLE). This FS owns the content types, minimum counts, the Techniques screens and the content-verification workflow; ARC-03 §6 owns the schema.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "Let me rehearse the real situation." | 22-tile directory, 26 routes and overlapping labs; 15 modules, 10 paths and 7 programs (FND-UI-01, FND-DOM-01) | Aarav (P1, stand-ups), Meera (P2, interviews), Daniel (P3, presentations); UN-02 |
| "Every practice screen scores the same anyway." | 10 recording screens share one evaluation that ignores lab instructions (FND-UI-04) | All |
| "The content feels repetitive and the levels are off." | 150 activities with templated structure, duplicate titles; inconsistent level vocabularies (FND-DOM-13, FND-DOM-12) | Sofia (P4), Hoang (P5) |
| "I want the named techniques I've heard about, explained clearly." | Techniques exist only as creator-attributed seeds with channel-root links and unverifiable timestamps marked "VERIFIED" (FND-DOM-09; data_learning_audit §2.2–2.3) | Sofia (P4), Daniel (P3) |
| "Is this content even allowed?" | Creator name and framework names used without a licence artefact; captions scraped (FND-DOM-09, FND-AI-11) | Business risk (BO-06) |
| "Completing an activity by typing three words is not practice." | Completion by 3 typed words or a wordless recording (FND-DOM-05) | All; PP-4 |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | SCR-PRAC-01 Practice home (5 tracks with counts, Techniques collection, Saved entry); SCR-PRAC-02 track list with template / level / Saved filters and "Practised" status; SCR-PRAC-03 activity detail with Start and Save; SCR-PRAC-04 technique detail ("Practise it", "Watch the original lesson"); start into FS-04 with `source = PRACTICE`; counting toward progress and streak (FS-08); save/unsave (ENT-12); content pack: activities, techniques, tips, provenance, `licenceStatus`, build-time validation, minimum counts; template descriptions; verification of the creator-derived items already in the app (§5.9) | TPL-ROLE scripted role-play (§5.8); content expansion |
| **Out** | Remote or downloadable content (ARC-01: `ContentRepository` must not fetch, R1–R2); YouTube scraping, caption download or API ingestion (ADR-011, SRS-BR-028, NG-08); an in-app video player (outbound link only, SRS-FR-PRAC-009); creator name, likeness or branding anywhere in app, content, notifications or store listing (SRS-BR-028); user-generated content; admin console (NG-10); open-ended chatbot role-play (NG-02); social sharing (NG-06); free-text search (not required by the SRS; Q5); saving techniques (Q1) | — |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Browse | Onboarding complete; content pack bundled and valid (build-time, SRS-ERR-008); no network needed |
| Start an activity | RECORD_AUDIO granted (otherwise SCR-PERM-01 via FS-04); speech tier known (FS-12) |
| Watch the original lesson | A YouTube app or browser able to open `https` links; a connection (handled by that app) |
| Ship content | Every record `licenceStatus` shippable and provenance verified (SRS-BR-028) |

## 5. Main workflow

### 5.1 Practice home — SCR-PRAC-01 (SRS-FR-PRAC-001, -008)

1. Practice tab opens SCR-PRAC-01. `PracticeViewModel` reads the track and technique index from `ContentRepository` and practised flags from `AttemptRepository`.
2. The user's own track is listed first, labelled "Your goal"; the other four follow in fixed order TRK-EVERYDAY, TRK-WORK, TRK-INTERVIEW, TRK-PRESENT, TRK-CLEAR (RECOMMENDATION). Each card: track name, one-line description, activity count, "{n} practised".
3. The **Techniques** collection follows the tracks (DES-02 v0.2): technique cards (name in our wording, one-line idea, "{n} exercises", "{k} of {n} practised"), grouped by track with a chip per track. A technique card opens SCR-PRAC-04.
4. A **Saved ({n})** row appears when the user has saved activities and opens SCR-PRAC-02 in Saved mode (across tracks).

### 5.2 Track — SCR-PRAC-02 (SRS-FR-PRAC-001, -002, -006)

1. Selecting a track lists its ACTIVE activities. Card: title, template label, estimated minutes, level, "Practised" when at least one **valid** attempt exists for the activity (any content version; Inconclusive never counts, SRS-BR-008) — RECOMMENDATION definition.
2. Filter chips: template (5; R2 Role-play), level (5, D-17), Saved. Chips combine with AND; the result count is shown; **Clear filters** resets.
3. Order (RECOMMENDATION): activities at the user's level first, then adjacent levels, then content order. A "Techniques for this track" row links to the track's techniques (SCR-PRAC-04).

### 5.3 Activity detail — SCR-PRAC-03 (SRS-FR-PRAC-003)

Shows goal ("Why practise this"), prompt preview, template explanation (§5.6), "15 seconds to plan, up to 90 seconds to speak" (values from SRS-FR-SESS-002/003 †, or the activity override), the headline metrics it gives feedback on (ARC-05 §7), "Part of: {technique}" → SCR-PRAC-04 when linked, a capability note for transcript-dependent checks on T3b (SRS-FR-SPCH-007), **Save** toggle and one primary **Start**.

### 5.4 Start and count (SRS-FR-PRAC-004; PRD-FR-052)

**Start** opens SCR-SESS-01 with `activityId` and `source = PRACTICE` (FS-04). The attempt is evaluated, stored and synced exactly like a Today attempt; a valid attempt counts toward streak, practice time and trends through FS-08 (SRS-BR-010, -018). If the same activity is an incomplete step of today's plan, that step is completed too (FS-03 A3).

### 5.5 Save / unsave (SRS-FR-PRAC-006; PRD-FR-054)

The Save icon on cards and on SCR-PRAC-03 toggles an ENT-12 SavedActivity row (`profileId`, `activityId`, `savedAt`). A snackbar confirms "Saved" / "Removed from saved" with **Undo**. Saved items appear under the Saved filter and the Saved row; a saved activity that has been retired shows "No longer available" with **Remove** (SRS-BR-026). Sync behaviour is undefined (Issue 1).

### 5.6 Templates (PRD-FR-051; RECOMMENDATION copy)

| Template | User-facing name and description | Prep / max † (SRS-FR-SESS-002/003) | Headline feedback (ARC-05 §7) |
|---|---|---|---|
| TPL-READ | **Read aloud** — "Read a short text aloud. We check which words were recognised as intended, your pace and pauses." | 0 s / 30 s | M-MATCH, M-WPM, M-PAUSE |
| TPL-REPEAT | **Listen & repeat** — "Listen to a sentence, then say it back. Use the slow version if you need it." | 0 s / 30 s | M-MATCH, speaking time vs reference, M-WORDS |
| TPL-ANSWER | **Answer a question** — "Answer a question in about a minute. Take a moment to plan first." | 15 s / 90 s | M-WPM, M-FILL, M-LEN |
| TPL-STRUCT | **Structured answer** — "Answer with a simple structure, such as Situation–Task–Action–Result or Point–Reason–Example–Point." | 30 s / 120 s | M-STRUCT, M-WPM, M-FILL |
| TPL-STORY | **Tell a story** — "Tell a short story with a beginning, a middle and an end." | 30 s / 120 s | M-LEN, M-PAUSE, M-LEX |
| TPL-ROLE (R2) | **Role-play** — "Practise a conversation. Your partner's lines are spoken aloud and you answer each one." | 5 s † / 45 s † per turn | Per turn as TPL-ANSWER (SRS-FR-PRAC-007) |

### 5.7 Techniques — SCR-PRAC-04 (SRS-FR-PRAC-008, -009; PRD-FR-056, -057; D-08 DECISION 2026-11-21)

1. **Content type.** A technique is a first-class content record: display name in our wording, one-line idea, why it works, steps, tips, ordered exercises (activities on the five templates), optional self-check items (ARC-05 §8.4), provenance and `licenceStatus` (§10).
2. **SCR-PRAC-04** shows, top to bottom: name; track and level; **The idea** (one line); **Why it works**; **Steps**; **Exercises** (template, minutes, Practised status — each opens SCR-PRAC-03, then SCR-SESS-01); **Tips**; **Watch the original lesson** (text button, only where provenance exists); one primary **Practise it**.
3. **Practise it** opens SCR-PRAC-03 for the first exercise without a valid attempt, or the first exercise when all are practised (RECOMMENDATION).
4. **Watch the original lesson** fires an outbound `ACTION_VIEW` intent for the specific video URL with its start timestamp, opening the YouTube app or a browser outside SpeakCraft. No API key, embedded player, scraping or download (SRS-FR-PRAC-009). The button never names the creator (SRS-BR-028).
5. **Technique → template mapping** (RECOMMENDATION examples): PREP / STAR → TPL-STRUCT (M-STRUCT elements); pausing and pace drills → TPL-READ or TPL-ANSWER (M-PAUSE, M-WPM); filler-reduction drills → TPL-ANSWER (M-FILL); shadowing → TPL-REPEAT (M-MATCH); storytelling structures → TPL-STORY (M-LEN, M-PAUSE). Evaluation uses only the labelled metrics (FS-05 §5.11); vocal-delivery aspects without a metric use the self-check (FS-05 Issue 9).
6. **Use elsewhere**: Today may select exercises by technique (FS-03 §8.4); coach tips link to their technique (FS-05 §5.7, ARC-05 §8.4).

### 5.8 R2 — Scripted role-play outline (TPL-ROLE; SRS-FR-PRAC-007; PRD-FR-055)

| Aspect | Outline (RECOMMENDATION unless cited) |
|---|---|
| Scenario record | `id`, track (TRK-EVERYDAY, TRK-WORK or TRK-INTERVIEW), level, title, context line, partner role, user role, 4–8 turns †, linked techniques, provenance, `licenceStatus` |
| Turn | Partner line shown **and** spoken by TTS (SRS-FR-PRAC-007); user records a reply (prep 5 s †, max 45 s †); turn-specific expected elements (e.g. "ask a clarifying question", "propose a time") detected by cue phrases and labelled Estimated |
| Flow | Linear script — no branching and no generated replies (NG-01, NG-02); runs in the FS-04 player loop (FS-04 A11) |
| Evaluation | Each turn evaluated as TPL-ANSWER with its expected elements (SRS-FR-PRAC-007); per-turn mistakes and focus tip (FS-05 A9); evidence gate per turn (Issue 3) |
| Inconclusive turn | Retry the turn or skip it (FS-04 Q5); skipped or Inconclusive turns never count (SRS-BR-008) |
| Summary | Per turn: transcript, expected elements found / missing, focus; up to 3 coach tips; **no overall score** (D-06) |
| Progress | Each valid turn is a valid attempt (streak and practice time via FS-08) |
| Content | ≥ 10 scenarios at R2 (PLN-02 EP-19) |

### 5.9 Content verification workflow (D-08 DECISION; SRS-BR-028; RECOMMENDATION procedure)

The content source is the creator-derived material already in the app, verified against specific videos and rewritten (D-08). Every item follows these steps; nothing is gathered by scraping, caption download or the YouTube API (ADR-011).

| Step | Action | Output / gate | Role |
|---|---|---|---|
| 1 Inventory | Create a backlog row for the legacy item with its code reference | `licenceStatus = PENDING_REVIEW`; `legacyRef` | Content editor |
| 2 Find the video | Watch the official channel by hand to locate the **specific** video and start time | Video ID, URL, timestamp; if none is found the item stays PENDING_REVIEW (Q2) | Content editor |
| 3 Rewrite | Write the idea, steps, exercises and tips in our own words and examples; no verbatim transcript text; display name in our wording | Draft record | Content editor |
| 4 Record provenance | `sourceVideoId`, `sourceUrl` (with `t=`), `timestampStart`, `techniqueNameAsTaught`, `verifiedBy`, `verifiedOn` | Provenance block | Content editor |
| 5 Review | Second person checks originality of wording, match with the source, plain B1 English, banned terms (DES-05 §12.3), level and template fit | `reviewedBy` ≠ `verifiedBy` (RECOMMENDATION) | Reviewer (WBS T-09.5) |
| 6 Mark shippable | Set `licenceStatus = ORIGINAL_REEXPRESSION` | Eligible for release | Content lead |
| 7 Validate | Build-time validator (§8.3) rejects anything incomplete | Release build passes or fails (SRS-ERR-008) | CI |

**Legacy items requiring re-verification** (VERIFIED in data_learning_audit §2.2–2.3; their timestamps point at the channel root and cannot be verified):

| Legacy item | Code reference | Count | Problem | Target content |
|---|---|---|---|---|
| Technique seeds | `CurriculumCatalog.kt:187-519` (timestamps and channel-root URLs at 204-449) | 15 (12 labelled "VERIFIED", 3 "PENDING_TRANSCRIPT") | Quotes the creator; links to the channel root | Techniques (§5.7) |
| Frameworks: 3Vs, 5 Vocal Foundations, PREP, CLEAR, High-Low-Buffalo, VAKS, Power Zone | `ActivityCatalogue.kt:136,144,160,168` and catalogue text (128 framework-keyword hits) | 7 | Origin unverified; names may be creator-associated (Issue 6) | Techniques with display names in our wording |
| 30-day plan | `CurriculumCatalog.kt:1005` ("Adapted from…") | 1 | Derived programme; programmes are retired (RD-16 §5) | Technique sequence or dropped (Q3) |
| Challenges | `CurriculumCatalog.kt:887-898` | 10 | `sourceReference` names the creator | Activities linked to techniques |
| Shadowing scripts | `CurriculumCatalog.kt:927-958` (attributions 931-952) | 3 | Attributed scripts — verbatim risk | New scripts in our words (TPL-REPEAT) with a link to the lesson |
| Activity catalogue | `ActivityCatalogueModules*.kt` | 150 | Templated structure, duplicate titles, framework names, constant attribution | Reviewed, de-branded, mapped to 5 tracks × 5 templates (WBS T-09.2) |

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | Filters give no result | Empty state "No activities match these filters" + **Clear filters** |
| A2 | Saved mode with nothing saved | "Save activities to find them here" + **Browse tracks** |
| A3 | Activity or technique retired in a newer content version | Hidden from browsing; still readable from history (SRS-BR-026); a saved retired activity shows "No longer available" + Remove |
| A4 | Technique without verified provenance (team-original technique) | "Watch the original lesson" is hidden (SRS-FR-PRAC-008 "where provenance exists") |
| A5 | No app can open the video link | Snackbar "Couldn't open the video. Check that a browser is installed." |
| A6 | Microphone not granted on Start | SCR-PERM-01 (FS-04 E1); browsing stays fully usable |
| A7 | Expanded width (≥ 840 dp) | List-detail: SCR-PRAC-02 list beside SCR-PRAC-03 / SCR-PRAC-04 (DES-02 §7) |
| A8 | T3b device | SCR-PRAC-03 notes "Transcript needed for this check" where relevant; activities remain startable (SRS-FR-SPCH-007) |
| A9 | Activity is also an incomplete step of today's plan | Valid attempt completes the Today step (FS-03 A3) |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Content pack invalid (schema, IDs, required fields, references, licence, provenance, minimums) | Build fails in CI; never shipped | SRS-ERR-008 |
| E2 | Content index fails to load at runtime (corrupt install) | "Couldn't load practice content. Restart the app." + Retry; PII-free diagnostic | SRS-ERR-005 |
| E3 | Save / unsave write fails | Retry once; then "Couldn't save this. Try again."; toggle reverts | SRS-ERR-005 |
| E4 | Activity referenced by a saved item or history is missing from the pack | Shown as "No longer available" (A3) | — |
| E5 | Outbound link cannot be handled | A5 | — |

## 8. Business rules

### 8.1 Rules applied (authoritative text in SRS §16)

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-001 | Library attempts, technique exercises and role-play turns pass the same evidence gate |
| SRS-BR-008 | "Practised" and technique progress count valid attempts only |
| SRS-BR-010 | A valid library attempt is a streak day like any other source |
| SRS-BR-020 | Attempts are idempotent by UUID |
| SRS-BR-026 | Attempts reference `activityId` + `contentVersion`; retired content stays readable |
| SRS-BR-028 | Creator-derived content is original re-expression with verified provenance to a specific video; no verbatim text, creator name, likeness or branding; PENDING_REVIEW never ships; no scraping, caption download or API ingestion (D-08 DECISION 2026-11-21) |

### 8.2 RECOMMENDATION — minimum content counts for R1 (values †)

| Item | Floor in source | Recommended minimum |
|---|---|---|
| Activities per track | ≥ 15 (PRD-FR-053) | ≥ 15, of which ≥ 8 free-speaking (TPL-ANSWER / STRUCT / STORY) for the Today main slot and ≥ 4 TPL-READ / REPEAT for warm-ups; every one of the 5 levels covered by ≥ 3 activities, counting level ranges (Issues 2, 4) |
| Templates | Five in R1 (PRD-FR-051) | Each template present in ≥ 4 of the 5 tracks |
| Techniques | Collection required (SRS-FR-PRAC-008) | ≥ 1 per track at launch; each with ≥ 2 exercises and ≥ 2 tips, one of them `general` (fallback for SRS-FR-EVAL-016) |
| Tips | — | ≥ 1 tip triggered by each ARC-05 §8.4 issue code that can occur in R1 |
| Role-play scenarios (R2) | — | ≥ 10 (PLN-02 EP-19) |
| Word bank | ≥ 300 (PRD-FR-065) | Owned by FS-07 |

### 8.3 RECOMMENDATION — build-time validation (SRS-FR-PRAC-005, SRS-FR-PRAC-009, SRS-ERR-008, NFR-MAINT-009, SRS-BR-028)

| # | Check | Fails the build when |
|---|---|---|
| V1 | Schema | Any record does not match the ARC-03 §6 schema version |
| V2 | Unique IDs | An ID repeats, or a retired ID is reused (registry of past IDs) |
| V3 | Required fields per template | TPL-READ/REPEAT without `referenceText`; TPL-STRUCT without `framework` and `expectedElements`; ANSWER/STRUCT/STORY without `targetRange`; any activity without title, goal, prompt, track, template, level, `estimatedMinutes` |
| V4 | References | `techniqueIds`, exercise lists, `tip.techniqueId` or tip triggers point to unknown records, ARC-05 §8.4 issue codes, metric keys or templates |
| V5 | Licence and provenance | `licenceStatus` missing; any PENDING_REVIEW record in a release build; a creator-derived record without `sourceVideoId`, timestamp, `verifiedBy`, `verifiedOn`; a `sourceUrl` that is a channel root (`/@…`, `/channel/`, `/c/`) instead of a specific video |
| V6 | Banned terms | Creator name or handle, "AI", or another DES-05 §12.3 term in any user-facing field |
| V7 | Review | `reviewedBy` missing or equal to `verifiedBy` for creator-derived records |
| V8 | Minimum counts | §8.2 minimums not met for the release |
| V9 | Fit | `referenceText` longer than the template time allows (e.g. > 60 words for 30 s †); duplicate titles within a track (FND-DOM-13) |
| V10 | Required items | `onb-intro` missing or `planEligible = true` |

## 9. UI states

**SCR-PRAC-01 Practice home**

| State | What the user sees | Actions |
|---|---|---|
| Default | "Your goal" track first, four more tracks with counts and "{n} practised"; Techniques collection grouped by track; Saved row if any | Open track (→ SCR-PRAC-02); open technique (→ SCR-PRAC-04); open Saved |
| Loading | Skeleton cards only if the index is not ready after 300 ms (local read, NFR-PERF-008) | — |
| Empty | N/A — validation guarantees five tracks; if no technique is shippable yet, the Techniques collection is hidden rather than empty | — |
| Error | E2 message | Retry |
| Offline | Identical to Default (bundled content) | — |
| Permission | N/A — browsing uses no permission | — |

**SCR-PRAC-02 Track (and Saved mode)**

| State | What the user sees | Actions |
|---|---|---|
| Default | Track name, description, filter chips, result count, activity cards (title, template, minutes, level, Practised, Save icon); "Techniques for this track" row | Filter; Save / unsave; open activity (→ SCR-PRAC-03); open technique (→ SCR-PRAC-04) |
| Filtered empty | A1 | Clear filters |
| Saved mode | Saved activities from all tracks; retired ones marked | Open; Remove |
| Saved empty | A2 | Browse tracks |
| Loading / Error / Offline / Permission | As SCR-PRAC-01 (skeleton after 300 ms / E2 / identical / N/A) | — |

**SCR-PRAC-03 Activity detail**

| State | What the user sees | Actions |
|---|---|---|
| Default | Title, template, minutes, level, goal, prompt preview, how it works, feedback metrics, "Part of: {technique}", last attempt date if practised | Start (→ SCR-SESS-01); Save; technique link (→ SCR-PRAC-04) |
| T3b note | "Transcript needed for this check" for transcript-based metrics | Start |
| Retired | "This activity is no longer available." | Back; Remove from saved |
| Loading / Empty / Offline | N/A (local) / Retired state above / identical | — |
| Error | E3 inline for Save | Retry |
| Permission | Microphone not granted: Start opens SCR-PERM-01 (FS-04) | Allow microphone |

**SCR-PRAC-04 Technique detail** (DES-02 v0.2; SRS-FR-PRAC-008, -009)

| State | What the user sees | Actions |
|---|---|---|
| Default | Name (our wording), track and level, The idea, Why it works, Steps, Exercises with Practised status, Tips, "Watch the original lesson" | Practise it (→ SCR-PRAC-03 of next exercise); open an exercise (→ SCR-PRAC-03 → SCR-SESS-01); Watch the original lesson (outbound intent) |
| In progress | "{k} of {n} exercises practised"; Practise it targets the next unpractised exercise | Practise it |
| All practised | "All exercises practised" | Practise it (first exercise again) |
| No provenance | Watch link hidden (A4) | Practise it |
| Link failure | A5 snackbar | — |
| Loading | N/A — local content | — |
| Empty | N/A — validation requires ≥ 2 exercises; a retired technique shows "This technique is no longer available." + Back | Back |
| Error | E2 message | Retry |
| Offline | Identical; the outbound link opens and the YouTube app or browser handles being offline | — |
| Permission | N/A on this screen (microphone is requested at Start, FS-04) | — |

## 10. Data requirements

| Entity / record | Fields needed by FS-06 (minimum; ARC-03 §6 authoritative for the content schema, ARC-03 §3 for ENT-12) |
|---|---|
| Content pack (bundled JSON, versioned) | `packVersion`, `schemaVersion`, `generatedAt`; collections: activities, techniques, tips, role-play scenarios (R2), word bank (FS-07) |
| Activity | `id`, `track`, `template`, `level` (or `levelMin`/`levelMax`, Issue 2), `title`, `goal`, `prompt`, `estimatedMinutes`, `prepSeconds?`, `maxSeconds?` (override; FS-02 Issue 7), `referenceText?`, `framework?` + `expectedElements?`, `targetRange?`, `focusFamilies` (FS-03 §8.5), `techniqueIds`, `planEligible`, `status` (ACTIVE / RETIRED), provenance, `licenceStatus` |
| Technique | `id`, `displayName` (our wording), `idea`, `whyItWorks`, `steps[]`, `exerciseIds[]` (ordered), `tipIds[]`, `trackIds[]`, `level` range, `focusFamilies`, `selfCheckItems[]` (ARC-05 §8.4), `status`, provenance, `licenceStatus` |
| Tip | `id`, `techniqueId`, `text` (our wording), triggers (`issueCodes` per ARC-05 §8.4, metric + band, `templates`, `techniqueIds`), `general`, `priority`, provenance, `licenceStatus` |
| Provenance block (SRS-FR-PRAC-009) | `sourceVideoId`, `sourceUrl` (specific video with `t=`), `timestampStart`, `techniqueNameAsTaught` (internal, not displayed — Issue 6), `verifiedBy`, `verifiedOn`, `reviewedBy`, `legacyRef?` |
| `licenceStatus` | ORIGINAL_REEXPRESSION, PENDING_REVIEW (SRS-BR-028); RECOMMENDATION: ORIGINAL for team-written items with no source (Issue 7) |
| ENT-12 SavedActivity | `profileId`, `activityId`, `savedAt`; sync and export undefined (Issue 1) |
| ENT-03 Attempt (read) | `activityId`, `result`, `source`, `dateKey` — Practised status and technique progress |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `ContentRepository` | Load and index the pack by track, template, level and technique; resolve tips; content version (read-only, no remote fetch) |
| `AttemptRepository` | Practised flags and technique progress (Flows) |
| `ProfileRepository` | User track and level (ordering, "Your goal") |
| ENT-12 operations | Save / unsave / list — owning repository not named in ARC-01 §2 (Issue 1); interface in ARC-04 §5 |
| Content validator (build tool) | §8.3 checks in CI (WBS T-09.1) |
| Platform | Outbound `Intent.ACTION_VIEW` for source videos (RECOMMENDATION: `startActivity` guarded by try/catch so no extra package-visibility declaration is needed; Issue 10) |

## 12. Offline behaviour

The whole library — browsing, filters, saved items, technique detail and starting activities — works offline because content is bundled (PRD-FR-100, ARC-01 §8). Only "Watch the original lesson" needs a connection, and the app that opens the link handles that. Saved-item changes are local; their sync is undefined (Issue 1).

## 13. Privacy requirements

- Content contains no personal data; saved activities and attempts are personal data covered by export and deletion (SRS-PRIV-003; Issue 1 for ENT-12).
- The outbound link carries only the public video URL and timestamp — no user identifiers or tracking parameters (RECOMMENDATION).
- No analytics on browsing, filters, saves or link taps (SRS-PRIV-004, ADR-013).
- No YouTube API key or SDK is bundled; no network call is made by SpeakCraft for content (ADR-011).

## 14. Accessibility notes

Track, technique and activity cards are merged nodes ("Workplace, 18 activities, 5 practised") (A11Y-07); filter chips and the Save toggle expose selected/"Saved" state (A11Y-06); Practised status is text, not colour (A11Y-23); counts use plurals (A11Y-36); section titles are headings; "Watch the original lesson" is announced "opens YouTube" (A11Y-02); targets ≥ 48 dp (A11Y-19); cards reflow at 200 % font scale with no fixed heights (A11Y-13); the snackbar with Undo meets DES-07 TC-A11Y-020 timing.

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS06-01 | An onboarded user with track TRK-INTERVIEW | Practice opens | Five tracks with activity counts are listed, Job interviews first as "Your goal" |
| AC-FS06-02 | A track with 18 activities | The user filters by Structured answer and Intermediate | Only matching activities are listed with the count; Clear filters restores 18 |
| AC-FS06-03 | One Inconclusive and no valid attempt on an activity | The track list is shown | The card shows title, template, minutes and level, and is **not** marked Practised |
| AC-FS06-04 | An activity linked to a technique | Its detail opens | Goal, prompt preview, template explanation, "Part of: {technique}" and one Start are shown |
| AC-FS06-05 | No valid attempt yet today | The user completes a library activity validly | The attempt is stored with `source = PRACTICE` and today counts as a streak day |
| AC-FS06-06 | An activity | The user saves it, restarts the app, then unsaves it | It appears under Saved after restart and disappears after unsaving |
| AC-FS06-07 | A content pack with a duplicate ID, a TPL-READ without reference text, or a tip pointing to an unknown technique | CI builds the release | The build fails naming the record |
| AC-FS06-08 | The R1 release pack | It is validated | Every track has ≥ 15 activities and all five templates are present in the library |
| AC-FS06-09 | A creator-derived record with `licenceStatus = PENDING_REVIEW`, or a provenance URL to the channel root | The release build runs | The build fails |
| AC-FS06-10 | The release pack and app strings | They are scanned | No creator name, handle or likeness reference appears in any user-facing field |
| AC-FS06-11 | An activity retired in the new content version with an attempt in history | The user opens history and the library | The attempt is readable; the activity is not listed; if saved it shows "No longer available" |
| AC-FS06-12 | Airplane mode | The user browses, filters, opens a technique and starts an activity | All succeed |
| AC-FS06-13 | Practice home | It is shown | A Techniques collection lists technique cards with "{k} of {n} practised" |
| AC-FS06-14 | A technique with verified provenance | SCR-PRAC-04 opens | Name, idea, why it works, steps, exercises, tips, Practise it and "Watch the original lesson" are shown |
| AC-FS06-15 | SCR-PRAC-04 | The user taps "Watch the original lesson" | An outbound intent opens the specific video at its timestamp outside the app; no in-app player appears |
| AC-FS06-16 | A technique with 3 exercises, the first practised | The user taps Practise it | SCR-PRAC-03 opens for the second exercise, and Start leads to SCR-SESS-01 |
| AC-FS06-17 | A team-original technique without provenance | SCR-PRAC-04 opens | The Watch link is hidden and everything else is shown |
| AC-FS06-18 | A legacy technique seed whose specific video has not been found | The release pack is built | The item remains PENDING_REVIEW and is absent from the release |
| AC-FS06-19 | The activity is an incomplete step in today's plan | It is completed validly from Practice | The Today step is marked done (FS-03 A3) |
| AC-FS06-20 | Expanded width (900 dp) | A track is opened | List and detail are shown side by side |
| AC-FS06-21 | R2: a 5-turn role-play scenario | The user completes it | Each partner line is shown and spoken; each turn is recorded and evaluated as TPL-ANSWER with its expected elements; the summary shows no overall score |

## 16. Test cases

No test case has been executed in Stage A (GOV-02 DC-4).

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-PRAC-001 | Tracks and counts | Robolectric UI | Fake `ContentRepository` with 5 tracks | Open Practice | 5 cards, counts, goal track first | SRS-FR-PRAC-001 |
| TC-PRAC-002 | Track ordering | unit | Each goal track | Build home state | Goal track first; others in fixed order | SRS-FR-PRAC-001 |
| TC-PRAC-003 | Template and level filters | unit | Track with mixed items | Apply chips singly and combined | Correct subsets and counts | SRS-FR-PRAC-001 |
| TC-PRAC-004 | Filtered empty state | Robolectric UI | Filters with no match | Render | Empty text + Clear filters | SRS-FR-PRAC-001 |
| TC-PRAC-005 | Card fields | Robolectric UI | Activities fixture | Render cards | Title, template, minutes, level, Practised | SRS-FR-PRAC-002 |
| TC-PRAC-006 | Practised from valid attempts | unit | Valid, Inconclusive and older-version attempts | Compute flags | Valid only, across content versions | SRS-FR-PRAC-002, SRS-BR-008 |
| TC-PRAC-007 | Activity detail | Robolectric UI | Linked activity | Open SCR-PRAC-03 | Goal, preview, explanation, technique link, Start | SRS-FR-PRAC-003 |
| TC-PRAC-008 | Start from library | unit | Navigation fake | Tap Start | Route to SCR-SESS-01 with `source = PRACTICE` | SRS-FR-PRAC-004 |
| TC-PRAC-009 | Library attempt counts | unit | `ProgressCalculator` with a library attempt | Compute streak and time | Counted like a Today attempt | SRS-FR-PRAC-004, SRS-BR-010 |
| TC-PRAC-010 | Save persistence | instrumented | In-device Room | Save; kill process; relaunch; unsave | Row present after relaunch; removed after unsave | SRS-FR-PRAC-006 |
| TC-PRAC-011 | Saved mode | Robolectric UI | Saved items in 3 tracks, one retired | Open Saved | All shown; retired marked with Remove | SRS-FR-PRAC-006, SRS-BR-026 |
| TC-PRAC-012 | Validator: schema and IDs | unit (CI) | Packs with schema error, duplicate ID, reused retired ID | Run validator | Each rejected with record ID | SRS-FR-PRAC-005, SRS-ERR-008 |
| TC-PRAC-013 | Validator: required fields and references | unit (CI) | Missing fields per template; dangling technique, tip and issue-code references | Run validator | Rejected | SRS-FR-PRAC-005 |
| TC-PRAC-014 | Validator: licence and provenance | unit (CI) | PENDING_REVIEW record; channel-root URL; missing timestamp; `reviewedBy = verifiedBy` | Run release validation | Rejected | SRS-FR-PRAC-009, SRS-BR-028 |
| TC-PRAC-015 | Validator: banned terms | unit (CI) | Creator name/handle and DES-05 §12.3 terms seeded into fields | Run validator | Rejected | SRS-BR-028 |
| TC-PRAC-016 | Validator: minimum counts | unit (CI) | Pack with 14 activities in one track | Run release validation | Rejected (§8.2) | PRD-FR-053 |
| TC-PRAC-017 | Retired content | unit | Attempt on activity retired in v2 | Load history and library | History readable; not listed | SRS-BR-026 |
| TC-PRAC-018 | Content load time | instrumented | RD-LOW, release pack | Cold start; open Practice | Index ready without delaying NFR-PERF-001; queries ≤ 50 ms † | NFR-PERF-001, -008 |
| TC-PRAC-019 | Offline library | instrumented | Airplane mode | Browse, filter, open technique, start | All succeed; no network request | PRD-FR-100 |
| TC-PRAC-020 | Techniques collection | Robolectric UI | Techniques in 3 tracks; one non-shippable excluded | Open Practice | Cards grouped by track; progress text | SRS-FR-PRAC-008 |
| TC-PRAC-021 | Technique detail content | Robolectric UI | Technique with provenance | Open SCR-PRAC-04 | All §5.7 sections; one Practise it | SRS-FR-PRAC-008 |
| TC-PRAC-022 | Practise it target | unit | Exercises with mixed practised state | Tap Practise it | Next unpractised exercise; first when all practised | SRS-FR-PRAC-008 |
| TC-PRAC-023 | Outbound video intent | Robolectric UI | Intent capture; no-handler variant | Tap Watch the original lesson | `ACTION_VIEW` with exact video URL and `t=`; snackbar when unhandled; no player | SRS-FR-PRAC-009 |
| TC-PRAC-024 | Hidden link without provenance | Robolectric UI | Team-original technique | Open SCR-PRAC-04 | Link absent | SRS-FR-PRAC-008 |
| TC-PRAC-025 | Accessibility | Robolectric UI | fontScale 2.0; both themes | Render SCR-PRAC-01…04 | No clipping; merged semantics; 48 dp | NFR-A11Y-001, -002, -004 |
| TC-PRAC-026 | Expanded list-detail | Robolectric UI | 900 dp width | Open track | Side-by-side panes | DES-02 §7 |
| TC-PRAC-027 | Content review | manual | Release candidate pack; verification backlog | Sample 20 % of creator-derived records against their videos; check wording, level, plain English | No verbatim text; provenance matches the video; record sign-off | SRS-BR-028, NFR-MAINT-009 |
| TC-PRAC-028 | R2 role-play loop | unit + Robolectric UI | 5-turn scenario fixture, fake TTS | Run all turns | TTS per partner line; per-turn TPL-ANSWER evaluation; summary without score | SRS-FR-PRAC-007 |

## 17. Traceability

| PRD-FR | SRS-FR | AC | TC |
|---|---|---|---|
| PRD-FR-050 | SRS-FR-PRAC-001, -002, -003 | AC-FS06-01…04, -20 | TC-PRAC-001…007, -025, -026 |
| PRD-FR-051 | SRS-FR-PRAC-005 (templates in pack); SRS §1.2 | AC-FS06-08 | TC-PRAC-013, -016 |
| PRD-FR-052 | SRS-FR-PRAC-004 | AC-FS06-05, -19 | TC-PRAC-008, -009 |
| PRD-FR-053 | SRS-FR-PRAC-005; SRS-ERR-008; SRS-BR-026 | AC-FS06-07, -08, -11 | TC-PRAC-012, -013, -016, -017, -018 |
| PRD-FR-054 | SRS-FR-PRAC-006 | AC-FS06-06 | TC-PRAC-010, -011 |
| PRD-FR-055 (R2) | SRS-FR-PRAC-007 | AC-FS06-21 | TC-PRAC-028 |
| PRD-FR-056 | SRS-FR-PRAC-008 | AC-FS06-13, -14, -16, -17 | TC-PRAC-020, -021, -022, -024 |
| PRD-FR-057 | SRS-FR-PRAC-009; SRS-BR-028 | AC-FS06-09, -10, -15, -18 | TC-PRAC-014, -015, -023, -027 |
| PRD-FR-100 (supporting) | SRS-AVL-001 | AC-FS06-12 | TC-PRAC-019 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Should users also be able to save techniques (ENT-12 covers activities only)? | Product Owner |
| Q2 | A legacy item whose specific source video cannot be found: drop it, or rewrite it as team-original (`ORIGINAL`, no link)? | Product Owner |
| Q3 | The adapted 30-day plan: turn it into an ordered technique sequence, or drop it because programmes are retired (RD-16 §5)? | Product Owner |
| Q4 | Should Practice home show Techniques before tracks for users who prefer named techniques (Sofia, P4)? | UX |
| Q5 | Is free-text search in the library needed in R1 (not in the SRS)? | Product Owner |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | Saved activities (ENT-12) are personal data, but they are absent from the "what syncs" list, the SRS §9 data table, the export contents and the SEC-01 inventory, and ARC-01 §2 names no repository that owns them | ADR-014 §6; SRS §9; SRS-FR-SET-004; SEC-01 §3.2; ARC-01 §2 | Decide sync (LWW per item or local only), add to export/deletion, assign a repository |
| 2 | Activities have one level each, the PRD floor is 15 per track, there are 5 levels and 5 templates, and the plan engine excludes activities used in the last 7 days; a user's level-appropriate main pool may be about 5 activities, so repeats come quickly | PRD-FR-053; SRS-FR-PRAC-001; SRS-FR-TODAY-005; D-17 | Allow level ranges per activity and/or raise the floor (§8.2) |
| 3 | Each role-play turn is evaluated as TPL-ANSWER, whose gate needs ≥ 8 recognised words †; natural short replies ("Yes, Thursday works for me") would be Inconclusive | SRS-FR-PRAC-007 vs SRS-BR-001 | Define a turn-specific word minimum |
| 4 | The Today main step needs track-matched free-speaking activities, but no per-template minimum exists; Clear speech centres on read-aloud and repeat (also FS-03 Issue 4) | SRS-FR-TODAY-004; PRD-FR-053 | Adopt §8.2 per-template minimums or allow READ/REPEAT main steps for Clear speech |
| 5 | Track names differ: "Workplace" / "Interviews" (PRD) vs "Workplace communication" / "Job interviews" (onboarding goals) vs "Workplace" / "Job interviews" (design system) | PRD-FR-050; SRS-FR-ONB-002; DES-05 §12.2 | One label per track (DES-02 §2.3 principle 5) |
| 6 | Technique detail shows "its name", provenance stores the "technique name as taught", and DES-05 §12.3 bans borrowed framework names ("5 Vocal Foundations", etc.); whether taught names such as High-Low-Buffalo, VAKS or Power Zone may be displayed under D-08 is not stated | SRS-FR-PRAC-008/009; DES-05 §12.3; SRS-BR-028 | State that the display name is always our wording and the taught name is internal provenance only |
| 7 | SRS-BR-028 defines only ORIGINAL_REEXPRESSION and PENDING_REVIEW, and SRS-FR-PRAC-009 requires a source video on **every** technique, activity and tip record; team-written items (e.g. `onb-intro`, everyday prompts) have no source video and no shippable status | SRS-BR-028; SRS-FR-PRAC-009 | Add an ORIGINAL status and make video provenance conditional on creator-derived items |
| 8 | RD-16 still lists the creator knowledge base as "Remove → original content in CAP-06" and its CAP-06 description has no techniques; PRD-FR-053 still says content is "original" while PRD-FR-056 makes creator-derived re-expression the curriculum source | RD-16 §2, §5; PRD-FR-053 vs PRD-FR-056/057 | Update RD-16 and reword PRD-FR-053 to "original or original re-expression (D-08)" |
| 9 | Design documents disagree on technique navigation: DES-03 routes technique cards through SCR-PRAC-02 "Techniques mode" and a technique variant of SCR-PRAC-03 (its I-17), while DES-02 v0.2 adds SCR-PRAC-04 reached from the Techniques collection on SCR-PRAC-01 | DES-03 SCR-PRAC-02/03 vs DES-02 v0.2 §3–§4 | Align DES-03 to SCR-PRAC-04 (this FS follows DES-02) |
| 10 | The manifest requirement lists only RecognitionService in `<queries>`; checking in advance whether an app can open the video link on Android 11+ would need an extra declaration | SRS-FR-PLAT-002 | Use a guarded `startActivity` (no check) or add the `<queries>` entry to SRS-FR-PLAT-002 |
