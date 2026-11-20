# FS-05 — Honest Feedback & Evaluation

| Field | Value |
|---|---|
| Document ID | FS-05 |
| Version | 0.2 — Draft for review (2026-11-21, consistency review CL-76: history renders the stored tip snapshot; CL-49: template general-tip fallback, "Coach tips" heading; CL-59: AC-FS05-25 actions wording) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-05; PRD §15.5 (PRD-FR-040…047), PRD-FR-032, PRD-FR-056/057 (technique tips, link out), PRD-NFR-001; SRS §6.5 (SRS-FR-EVAL-001…016), SRS-FR-WORD-014, SRS-FR-PRAC-008/009, SRS-FR-SESS-006…008, SRS-FR-SPCH-007, SRS-BR-001…008, SRS-BR-028, SRS-ERR-003; DES-02 SCR-PRAC-04; NFR-QUAL-001…006, NFR-PERF-005, NFR-PRIV-002/003, NFR-SEC-002/006, NFR-A11Y-001/006; ARC-05 §5–§10 (authoritative for evaluation behaviour, GOV-02 §4); ADR-003, ADR-009, ADR-013; SEC-01 §3.2, §3.3, §7.4; DES-02 (SCR-SESS-01 result / inconclusive, SCR-SESS-02); DES-05 §12; FS-04, FS-06, FS-07, FS-08, FS-12; decisions D-02, D-04, D-06, D-08 (decided 2026-11-21), D-16 |

---

## 1. Feature ID & purpose

**FS-05 / CAP-05 (pillar P-2).** Define what the user is shown after an attempt and the rules that make it trustworthy: the evidence-gate outcome (**Inconclusive** with a reason, never a guessed score), the **three headline metrics** for the template each labelled **Measured** or **Estimated** with a plain-language band and target range, **one strength and one focus**, a **Mistakes found** list with evidence for every issue the deterministic checks detected (or an honest "nothing found" statement), **Coach tips** linked to techniques and their source lessons, at most two high-precision language observations, **pass/fail only where a threshold exists** (read-aloud, listen-and-repeat, word stages) and **no composite score for free speaking** (D-06), a **"How we measured this"** explanation per metric, a **"Was this accurate?"** rating stored separately from the immutable attempt, and the **evaluation-rules version** on every attempt. ARC-05 is authoritative for the evaluation logic; this FS defines its presentation, states and tests.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "Don't give me a score I can't trust." | Fixed baselines (grammar 78, fluency 38), "pronunciation" computed from loudness and pitch, constant confidences (FND-AI-07, FND-AI-08); activity scores driven by text length with a fabricated dimension spread (FND-DOM-04) | All, esp. Sofia (P4), Meera (P2); UN-04 |
| "It passed me for saying nothing." | A wordless recording ≥ 1.5 s scores 64 and passes; three typed words pass (FND-DOM-05) | All; PP-1 |
| "Show me exactly what I said and what to fix next." | No transcript by default (FND-AI-01); feedback from four overlapping generators (FND-AI-15) | All; UN-03 |
| "It flagged grammar mistakes I didn't make." | Grammar and run-on rules over-fire on unpunctuated speech-recognition text (FND-AI-09) | Aarav (P1), Daniel (P3) |
| "What does 'read-aloud score' actually mean?" | Recognition agreement presented as pronunciation (FND-AI-08) | Hoang (P5); NG-03 |
| Strength to keep | The evidence gate fails closed and is well tested (FND-AI-17) | — |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | Gate outcomes and Inconclusive reasons; metric computation per ARC-05 §6 and display of the ARC-05 §7 headline set per template and tier; Measured/Estimated labels; bands and target ranges; "Not available — {reason}" for null metrics; transcript display rules; one strength and one focus; Mistakes found (SRS-FR-EVAL-015) and the self-check checklist (ARC-05 §8.4); Coach tips (SRS-FR-EVAL-016); the same sections for word-stage and review results (SRS-FR-WORD-014; screens in FS-07); ≤ 2 language observations; recognised-as-intended % with pass/fail and not-recognised words (READ/REPEAT); detected structure elements with cue phrases (STRUCT); "How we measured this" (SCR-SESS-02); "Was this accurate?" (ENT-13); evaluation and content versions; confidence only when supplied; evaluation timeout; re-display of stored results on SCR-PROG-04 (screen owned by FS-08); technique exercises evaluated with exactly the same labelled metrics (§5.11; D-08 DECISION 2026-11-21) | Per-turn feedback for TPL-ROLE (FS-06 §5.8) |
| **Out** | Overall/composite score for TPL-ANSWER/STRUCT/STORY (D-06, SRS-BR-006); pronunciation or accent scoring (NG-03); exam-band prediction (NG-04); cloud AI or LLM feedback (NG-01, ADR-003, NFR-SEC-006); real-time feedback while speaking (NG-14); technique-specific scores (e.g. a "framework score") (§5.11); generated or cloud-written tips (D-04, SRS-FR-EVAL-016); creator name, likeness or branding anywhere in results (SRS-BR-028); word-stage UX (FS-07, which consumes the evaluator outputs); trend computation (FS-08); analytics on ratings (SRS-PRIV-004) | — |

## 4. Preconditions

| Condition | Required state |
|---|---|
| Capture | A `CaptureResult` from FS-04 with `attemptId`, tier, template, duration, audio features (where audio is available) and transcript segments (T1–T3a) |
| Content | Activity data from the content pack: reference text (READ/REPEAT, stage 3), framework and expected elements (STRUCT), target range (ANSWER/STRUCT/STORY), target word and inflections (stages 2–4) (FS-06 §10) |
| Rules | Bundled, versioned rule set and metric catalogue (`evaluationVersion`), qualified rules only (NFR-QUAL-003) |
| History | Previous valid attempt on the same activity (comparison, "improvement" strengths) |
| Tips | Shippable technique tips with triggers, technique link and provenance in the content pack (FS-06 §10; SRS-BR-028) |

## 5. Main workflow

### 5.1 Evaluation pipeline (ARC-05 §2; ARC-01 §2 `EvaluationPipeline`)

1. `EvaluationPipeline.evaluate(activity, captureResult)` runs `EvidenceGate` first (SRS-FR-EVAL-001, SRS-BR-001; thresholds in ARC-05 §5 †). If it fails → `Inconclusive(reason)`; no metric is computed or shown (§5.2).
2. Valid → `MetricEngine` computes every catalogue metric defined for the template and tier (SRS-FR-EVAL-002). A metric whose inputs are missing is `null` with a reason code — never a default, baseline or fallback (SRS-BR-002, NFR-QUAL-001).
3. READ/REPEAT (and word stage 3) → `ReferenceMatcher` computes M-MATCH ("Recognised as intended", SRS-BR-004) and the list of reference words not recognised; pass/fail against the versioned threshold (75 % †, SRS-BR-007).
4. ANSWER/STRUCT/STORY (and stage 4) → `RuleEngine` applies only rules with precision ≥ 0.90 † on the SPK-02 corpus (SRS-FR-EVAL-006); results feed M-GRAM and observations.
5. `FeedbackComposer` selects exactly one strength and one focus (§5.5), builds the ordered **Mistakes found** list from the ARC-05 §8.4 issue catalogue (§5.6), and selects **Coach tips** from the content pack's technique tips by the ARC-05 §8.4 selection rules (§5.7).
6. The `EvaluationResult` (outcome, metrics with labels and bands, headline set, feedback codes and parameters, issue codes with evidence, selected tip IDs, observations, `evaluationVersion`, `contentVersion`) is returned to `SessionViewModel` and saved by `AttemptRepository` (FS-04 §5.1 step 7). Metric + rule + feedback ≤ 200 ms † (ARC-05 §11); end-to-end within NFR-PERF-005.
7. SCR-SESS-01 shows **result** (§5.3) or **inconclusive** (§5.2). Focus moves to the result header (A11Y-09).
8. The user may open "How we measured this" for any metric (SCR-SESS-02, §5.8), open a tip's technique (SCR-PRAC-04) or its source lesson (§5.7), and answer "Was this accurate?" (§5.9).

### 5.2 Inconclusive outcomes (ARC-05 §5; SRS-FR-SESS-007; copy RECOMMENDATION, DES-05 §12.1 tone)

| Reason code | Gate condition † | SRS-FR-SESS-007 reason | User copy |
|---|---|---|---|
| TOO_SHORT | Duration < 1.5 s | too short | "This attempt was too short to measure. Try speaking for a little longer." |
| TOO_QUIET | Mean active-frame level below the silence floor | too quiet | "It was too quiet to hear clearly. Try again a little closer to the phone." |
| NO_SPEECH | Voiced speech < 0.5 s or < 25 % of duration | no speech detected | "We didn't hear any speech. Check that nothing is covering the microphone, then try again." |
| NO_TRANSCRIPT | Transcript-required template, 0 recognised words (T1–T3a) — also used when segments do not carry this attempt's ID (Issue 6) | transcript unavailable | "Speech recognition didn't return any words this time, so we can't check this attempt. Try again, speaking clearly." |
| TOO_FEW_WORDS | ANSWER/STRUCT/STORY with < 8 recognised words | (not listed — Issue 6) | "We recognised only {n} words. Answers need at least 8 words to measure. Try a longer answer." |

The inconclusive state shows the **Inconclusive** badge (text), the reason line, reason-specific advice as the focus (ARC-05 §8.2 priority 1), **Retry** and **Skip** (FS-04). It shows no metric cards and no strength (Issue 7), and the reassurance line "This attempt doesn't count against you." (DES-05). The attempt is stored with `result = INCONCLUSIVE` and its reason so history can list it (SRS-FR-PROG-003); its recording is deleted immediately (SRS-BR-022); it never counts (SRS-BR-008).

### 5.3 Result content per template (headline set: ARC-05 §7, authoritative)

| Template / tier | Headline 1 | Headline 2 | Headline 3 | Pass/fail | Additional blocks |
|---|---|---|---|---|---|
| TPL-READ | M-MATCH "Recognised as intended" — Measured | M-WPM Pace — Measured | M-PAUSE Long pauses — Measured (Issue 1) | Pass mark ≥ 75 % † → "Pass mark reached" / "Not yet" (DES-05 §12.2) | Reference text with not-recognised words underlined (not colour-only); those words are Mistakes found entries |
| TPL-REPEAT | M-MATCH — Measured | Speaking time vs reference duration — Measured (Issue 2) | M-WORDS Words — Measured | As READ | As READ + Listen again |
| TPL-ANSWER | M-WPM — Measured | M-FILL Filler words per minute — Estimated | M-LEN Length vs target — Measured | None (D-06) | Observations appear in Mistakes found (≤ 2) |
| TPL-STRUCT | M-STRUCT Structure elements — Estimated | M-WPM — Measured | M-FILL — Estimated | None | Elements found / missing with the cue phrases that triggered detection (SRS-FR-EVAL-014) |
| TPL-STORY | M-LEN — Measured | M-PAUSE — Measured | M-LEX Vocabulary variety — Estimated (Issue 5) | None | — |
| T3b (any template) | M-SPK Speaking time — Measured | M-PAUSE — Measured | M-RATIO Speech ratio — Measured (Issue 1) | None; transcript-based checks show "Transcript needed for this check" (SRS-FR-SPCH-007) | No transcript, no observations; tier note "No transcript — audio-only" |

**Layout order of a valid result** (top to bottom): (1) transcript with tier label (SRS-FR-SESS-006); (2) the 3 headline metrics (+ pass/fail and comparison when it is a retry, FS-04 A1); (3) **What went well** (strength); (4) **Mistakes found** (§5.6); (5) **Coach tips**, whose first item is the focus "Try this next" (§5.7); (6) **Try again** and the step action; (7) **"Was this accurate?"** (§5.9). A **More details** link to SCR-SESS-02 lists every other computed metric (ARC-05 §7 note). Word-stage and review results use sections (4) and (5) with one tip (SRS-FR-WORD-014; screens in FS-07).

### 5.4 Metric row rules (SRS-FR-EVAL-002…004, -012)

| Element | Rule |
|---|---|
| Name and value | Canonical name (DES-05 §12.2), value and unit spelled out in semantics ("142 words per minute") |
| Label | Badge text exactly **Measured** or **Estimated** per SRS-BR-003; never colour-only |
| Band and target | Plain-language band and target range from §8.2 (RECOMMENDATION pending Issue 3) |
| Null value | "Not available — {reason}": "needs a transcript", "needs at least 25 words", "not available with this phone's speech recognition", "too little speech to measure" — never 0, a dash-as-zero or an estimate |
| Confidence | Shown only in SCR-SESS-02 and only when the recogniser supplied it; otherwise absent (SRS-FR-EVAL-012, SRS-BR-005) |
| Forbidden | Overall score, "x/100", "fluency score", "pronunciation", "accent", "AI", "accurate", "verified", "objective" (DES-05 §12.3, SRS-BR-004, SRS-BR-006) |
| Quality note | Clipping > 1 % of samples → "Parts of your recording were very loud, so some results may be less reliable." (ARC-05 §5: warning, not Inconclusive) |

### 5.5 One strength and one focus (SRS-FR-EVAL-005; ARC-05 §8.2 authoritative)

| Item | Rule |
|---|---|
| Labels | "What went well" (strength) and "Try this next" (focus) (DES-05 §12.2) |
| Position | The strength is section (3); the focus is the **first Coach tip** (SRS-FR-EVAL-016), linked to the technique of the best-matching tip for the focus issue (§5.7) |
| Focus | First matching condition in the ARC-05 §8.2 priority table: 1 Inconclusive advice · 2 READ/REPEAT not-recognised words · 3 missing STRUCT element · 4 fillers above band · 5 pace fast · 6 many long pauses · 7 length below target · 8 high-precision grammar observation. Applicability follows the template's computed metrics |
| Quoting | Where the focus concerns the transcript, it quotes the user's words ("You used 'basically' 4 times"; "Possible issue: 'he don't' → 'he doesn't'") (SRS-FR-EVAL-005) |
| Strength | The headline metric best placed within its band, or an improvement over the previous valid attempt on the same activity ("Fewer filler words than last time: 6 → 2"). Never a null metric; never an Estimated metric when its evidence is below the metric's minimum (e.g. M-LEX < 25 words) |
| No qualifying strength | RECOMMENDATION: a factual Measured line ("You spoke for 48 seconds.") rather than invented praise (Issue 7) |
| Determinism | Same inputs and rule version → same strength and focus (NFR-QUAL-006) |

### 5.6 Mistakes found (SRS-FR-EVAL-015, SRS-FR-WORD-014; PRD-FR-047)

The issue catalogue, raising rules (non-null metric outside its band by more than the noise band), evidence, labels and ordering are defined in **ARC-05 §8.4 (authoritative)**. This section specifies only how they are presented.

| Element | Presentation rule |
|---|---|
| Entry | Plain title, **evidence** exactly as ARC-05 §8.4 defines it (quoted words, count or value vs target), the **Measured** / **Estimated** badge text of that issue, and a "How we measured this" link to the source metric (SCR-SESS-02) |
| Order and count | ARC-05 §8.4 order (§8.2 priorities; see Issue 10); the first 3 are shown, then **Show all ({n})** / **Show fewer** (SRS-FR-EVAL-015) |
| Language observations | ISSUE_GRAMMAR_POSSIBLE entries, at most 2 (SRS-FR-EVAL-006), phrased "Possible issue: '{span}' → '{suggestion}'"; never in T3b |
| Forbidden | "Wrong pronunciation", "accent", "error score", "AI found" (SRS-BR-004, DES-05 §12.3) |

Example copy (RECOMMENDATION wording; codes from ARC-05 §8.4):

| Issue code | Example entry |
|---|---|
| ISSUE_WORD_NOT_RECOGNISED | "Not recognised as intended: 'schedule', 'colleague' — in 'the **schedule** for my **colleague**'" |
| ISSUE_WRONG_FORM | "We heard 'adapt', not 'adopt'." |
| ISSUE_TARGET_WORD_MISSING | "The word 'adopt' wasn't in your sentence." |
| ISSUE_TOO_SHORT | "38 seconds — this activity aims for 60–90." / "4 words — Make your own needs at least 5." |
| ISSUE_STRUCT_ELEMENT_MISSING | "No Result found. Found: Situation ('last year we…'), Task, Action." |
| ISSUE_FILLERS_HIGH | "'basically' ×4, 'um' ×3 — 5.2 per minute." |
| ISSUE_PAUSES_LONG | "6 pauses of 1 second or more; the longest (3.1 s) came after 'so the plan was'." |
| ISSUE_PACE_FAST / ISSUE_PACE_SLOW | "178 words per minute — target 100–160." |
| ISSUE_HEDGING_HIGH | "Hedges: 'sort of' ×3, 'I think maybe' ×2." |
| ISSUE_LOW_VARIETY | "Repeated often: 'really' ×6, 'thing' ×5." |
| ISSUE_GRAMMAR_POSSIBLE | "Possible issue: 'he don't' → 'he doesn't'." |
| ISSUE_TOO_QUIET | "Your recording was quiet or uneven in places." (quality note) |

**Empty state** (ARC-05 §8.4 step 4): "Our checks didn't find issues in this attempt." + one coverage line built from the checks actually run, e.g. "We checked pace, filler words and length. We don't check pronunciation or accent."; in T3b "Without a transcript we could only check speaking time and pauses." Never "perfect", "no mistakes" or "flawless" (PRD-FR-047). **Inconclusive** attempts show the gate reason instead of this section (§5.2, SRS-BR-008).

**Self-check** (ARC-05 §8.4): when the activity's technique has aspects audio cannot show (e.g. eye contact, gestures — NG-07), a short checklist "Check yourself" follows Mistakes found. Ticks are labelled **Self-reported**, never Measured or Estimated, never raise an issue and never affect metrics or progress (storage: Issue 11).

### 5.7 Coach tips (SRS-FR-EVAL-016; PRD-FR-047, -056, -057)

Candidate matching, ranking (trigger specificity, tip priority, stable hash of `tipId + attemptId`), the focus tip and the no-issue fallback are defined in **ARC-05 §8.4 (authoritative)**; tips are content records (FS-06 §10, ARC-03 §6). The section heading is **"Coach tips"** and its first card is titled **"Try this next"** (DES-05 §11.15b; consistency review, CL-49). Presentation rules:

1. **Tip 1 is the focus** — "Try this next" (§5.5): the top tip for the highest-priority issue, shown with the user's quoted evidence. With no issue it is the activity technique's general tip (ARC-05 §8.4 step 4), or the template's general tip when the activity has no technique (rule 6).
2. **Up to 2 further tips** follow, each covering the next issues or the activity's technique (ARC-05 §8.4 step 3). Only shippable tips are candidates (`licenceStatus` per SRS-BR-028; filtering in FS-06 §8.3).
3. Each tip shows its text in our own words, **Technique: {technique name}** → SCR-PRAC-04, and **Watch the original lesson** → outbound intent to the specific video and timestamp where provenance exists (SRS-FR-PRAC-009). No creator name, likeness or branding (SRS-BR-028; D-08 DECISION 2026-11-21). Tips never promise results ("this will fix…").
4. Word-stage and review results show one tip (SRS-FR-WORD-014). TPL-ROLE turns (R2) show the focus tip only; the scenario summary may show up to 3 (FS-06 §5.8).
5. Reproducibility: same transcript, metrics, activity, `attemptId` and content version → same tips; golden tests fix all five (TC-EVAL-030). No generative or cloud model is used (D-04).
6. An activity with no technique and no matching tip shows the **template's general tip** as "Try this next" (RECOMMENDATION; consistency review, CL-49; Issue 12).

### 5.8 "How we measured this" — SCR-SESS-02 content rules (SRS-FR-EVAL-010; ARC-05 §10)

| Section | Rule |
|---|---|
| Source | Generated from the metric-catalogue entry (single source); no hand-written per-screen variants |
| What it is | One plain sentence (B1 English, ≤ 20 words) |
| Label meaning | "Measured: counted directly from your recording or transcript." / "Estimated: worked out with rules that can be wrong." |
| How it is worked out | Plain formula ("words recognised ÷ minutes you were speaking") |
| What was used this time | Tier label; whether audio and transcript were available; number of words |
| Band and target | The band thresholds and this attempt's position |
| Limitations (required) | M-MATCH: "Speech recognition may recognise a word even when it isn't pronounced clearly, so this checks you were understood, not your accent." · M-FILL: "Some speech recognisers drop 'um' and 'uh', so this may count too few." · M-WPM: "Depends on how many words were recognised." · M-PAUSE: "Counts silences of 1 second or more inside your speech." · M-STRUCT: "Looks for typical phrases such as 'the result was'. You can structure an answer without them." · M-LEX: "Needs at least 25 words; short answers are not measured." · M-GRAM: "Only rules tested to be right at least 9 times in 10 are used; they can still be wrong." |
| Version | "Feedback rules version {evaluationVersion}" |
| Never | Creator names, "AI", claims of accuracy, or a technique source cited as evidence that a metric is valid (§5.11) |

### 5.9 "Was this accurate?" (SRS-FR-EVAL-009; PRD-FR-044; KPI-05)

1. The result shows **Yes** / **No**. **No** reveals optional reasons (multi-select, RECOMMENDATION): *The transcript is wrong*, *A number is wrong*, *The tip wasn't useful*.
2. `AttemptRepository` saves an ENT-13 FeedbackRating row (`attemptId`, answer, reasons, timestamps). The ENT-03 Attempt row is **not modified** (immutable once evaluated, SRS-FR-DATA-002; Issue 4).
3. One rating per attempt; changing the answer updates the same row (RECOMMENDATION). A snackbar confirms "Thanks — saved on this phone."
4. Ratings are local only and never synced (SEC-01 §3.2 #5); they are included in export (FS-10) and deleted with their attempt. Beta aggregates are shared only by the user per ADR-013.

### 5.10 Versioning and re-display (SRS-FR-EVAL-011; PRD-FR-046)

Each attempt stores `evaluationVersion` (semantic version of rules + metric definitions) and `contentVersion`. When a stored attempt is shown again (SCR-PROG-04, comparison), its stored metric values, issue codes, feedback codes and the **stored tip snapshot** (tip IDs and the text of each tip as shown, ARC-03 §3.4) are rendered, so history shows what was shown even after a content update; nothing is re-evaluated with a newer rule set or re-derived from a newer content version (RECOMMENDATION; consistency review, CL-76). The version appears in SCR-SESS-02; a comparison across versions carries a note (FS-04 A1).

### 5.11 Technique exercises (PRD-FR-056; D-08 DECISION, approved by the Product Owner 2026-11-21)

Exercises of a technique (FS-06 §5.7) use the existing templates and are evaluated **only** with the labelled metrics of §5.3 (mapping RECOMMENDATION): PREP or STAR → TPL-STRUCT with M-STRUCT elements; pausing, pace and filler drills → TPL-READ / TPL-ANSWER with M-PAUSE, M-WPM, M-FILL; shadowing → TPL-REPEAT with M-MATCH. No technique-specific score, badge or "mastery" claim is shown. Technique names appear in our own wording only, never with the creator's name (SRS-BR-028). Techniques about vocal delivery (pitch, volume, emphasis) currently have no supporting metric (Issue 9) and must not show improvised measures.

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | TPL-READ or TPL-REPEAT in T3b | Valid if the audio checks pass (FS-04 Issue 1); T3b headline set; M-MATCH row reads "Transcript needed for this check"; no pass/fail; focus from delivery metrics |
| A2 | TPL-STRUCT with no element detected | M-STRUCT "0 of 4 elements found" (Estimated); focus priority 3 names the first missing element |
| A3 | Headline metric null on a valid attempt (e.g. M-LEX with 20 words) | Row shows "Not available — needs at least 25 words"; the remaining rows display normally; strength chosen from non-null metrics |
| A4 | T2 / T3a device (recogniser owns the microphone) | Audio-dependent metrics computed from recogniser level callbacks where ARC-05 allows, otherwise null with "not available with this phone's speech recognition" (Issue 8) |
| A5 | Recogniser supplied confidence values | Shown only in SCR-SESS-02 as "Recogniser confidence: {n} %"; never used to alter metrics (SRS-BR-005) |
| A6 | Attempt reopened from history after an app update with a newer rule set | Stored values and version shown; no recomputation (§5.10) |
| A7 | User changes the accuracy answer | ENT-13 row updated; attempt unchanged |
| A8 | Word stage 2–4 or review attempt | `WordStageEvaluator` result "Pass mark reached" / "Not yet" (or Inconclusive) against SRS-BR-007 thresholds †, Mistakes found entries (e.g. "We heard 'adapt', not 'adopt'") and one coach tip (SRS-FR-WORD-014); Say-it caveat from ARC-05 §9; stage UX in FS-07 |
| A9 | R2 TPL-ROLE turn | Turn evaluated as TPL-ANSWER with turn-specific expected elements (SRS-FR-PRAC-007); compact turn result with its mistakes and focus tip; scenario summary without an overall score (FS-06 §5.8) |
| A10 | No issue raised on a valid attempt | Mistakes found shows the honest empty state with the coverage line (§5.6); the strength is shown; Coach tips show one general tip of the activity's technique as "Try this next" (ARC-05 §8.4 step 4), or the template's general tip when the activity has no technique (§5.7 rule 6) |
| A12 | The activity's technique has aspects audio cannot show | "Check yourself" checklist after Mistakes found; ticks stored as self-reported (§5.6) |
| A11 | User taps **Watch the original lesson** in a tip | Outbound `ACTION_VIEW` intent to the specific video URL with timestamp; the YouTube app or a browser opens outside SpeakCraft (SRS-FR-PRAC-009) |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Evaluation exceeds 2× the NFR-PERF-005 budget | FS-04 **error** state with Retry evaluation; audio retained until retry or exit (SRS-FR-EVAL-013) | — |
| E2 | One metric computation throws | That metric becomes null ("Not available — something went wrong measuring this"); others shown; PII-free diagnostic; never a default value | SRS-ERR-005 (logging) |
| E3 | One rule throws | Rule skipped for this attempt; no observation from it; diagnostic logged | — |
| E4 | Saving the evaluation fails | Handled by FS-04 E5; no result is shown as saved | SRS-ERR-005 |
| E5 | Saving the accuracy rating fails | Retry once; then "Couldn't save your answer. Try again."; result stays usable | SRS-ERR-005 |
| E6 | Required content data missing (e.g. no reference text, tip without technique) | Prevented by build-time validation (FS-06 §8.3) | SRS-ERR-008 |
| E7 | No app can open the source-video link | Snackbar "Couldn't open the video. Check that a browser is installed."; result unchanged | — |
| E8 | Tip selection finds no candidate and the activity has no technique | Focus tip only; no placeholder tip is invented (Issue 12) | — |

## 8. Business rules

### 8.1 Rules applied (authoritative text in SRS §16)

| Rule | Summary of application in this feature |
|---|---|
| SRS-BR-001 | Evidence gate: duration, voiced speech, level, transcript words (1 or 8 †) and session scope decide valid vs Inconclusive |
| SRS-BR-002 | Missing inputs → null and "Not available"; no substitutes |
| SRS-BR-003 | Fixed Measured / Estimated label per metric |
| SRS-BR-004 | Recognition agreement is called "Recognised as intended", never pronunciation or accent |
| SRS-BR-005 | Confidence only when supplied by the recogniser |
| SRS-BR-006 | No overall score for TPL-ANSWER, TPL-STRUCT, TPL-STORY (D-06) |
| SRS-BR-007 | Versioned pass thresholds: READ/REPEAT 75 % †; word stages 75 / 75 / 70 † |
| SRS-BR-008 | Inconclusive never counts as completion, failure, streak or practice time; it shows its reason instead of Mistakes found |
| SRS-BR-028 | Only tips with `licenceStatus = ORIGINAL_REEXPRESSION` (or team-original) and verified provenance ship; no creator name, likeness or branding in tips; no scraped or verbatim text (D-08 DECISION 2026-11-21) |

### 8.2 RECOMMENDATION — bands and target ranges for headline metrics (values †; owner ARC-05; see Issue 3)

| Metric | Unit shown | Bands (plain language) | Target shown |
|---|---|---|---|
| M-WPM | words per minute | < 100 slow · 100–160 comfortable · > 160 fast (ARC-05 §6 †) | 100–160 |
| M-MATCH | % | ≥ 75 pass mark reached · < 75 not yet (SRS-BR-007 †) | 75 % or more |
| M-PAUSE | long pauses (count; banded per minute of speaking) | ≤ 2/min few · 3–5/min some · > 5/min many † | 2 or fewer per minute |
| M-FILL | filler words per minute | ≤ 2 few · 3–5 some · > 5 many † | 2 or fewer |
| M-LEN | seconds or words | short · within range · long, against the activity's `targetRange` (FS-06 §10) | Activity target range |
| M-LEX | variety (0–1) | < 0.55 narrow · 0.55–0.70 typical · > 0.70 varied † | No target (Estimated) |
| M-STRUCT | elements | "{n} of 4 elements found" | All 4 |
| Speaking time vs reference (REPEAT) | seconds | shorter · similar (± 20 % †) · longer than the reference | ± 20 % of reference |
| M-WORDS (REPEAT) | words | fewer · same · more than the reference | Reference word count |
| M-SPK / M-RATIO (T3b) | seconds / % | M-RATIO < 50 % lots of silence · 50–85 % balanced · > 85 % few pauses † ; M-SPK vs template target | 50–85 % |

## 9. UI states

**SCR-SESS-01 — result state** (player states and actions in FS-04 §5.2)

| State | What the user sees | Actions |
|---|---|---|
| Default — free speaking | In order: transcript + tier label; 3 headline rows (§5.4); What went well; Mistakes found (first 3); Coach tips (Try this next + ≤ 2); no score | Show all; technique link (→ SCR-PRAC-04); Watch the original lesson; Try again; More details (→ SCR-SESS-02); Was this accurate?; Next step / Done |
| Mistakes — none found | "Our checks didn't find issues in this attempt." + coverage line (§5.6); one general tip (the technique's, or the template's when there is no technique) as "Try this next" | Technique link; Watch the original lesson |
| Self-check | "Check yourself" list with checkboxes, labelled "Self-reported" (A12) | Tick / untick |
| Mistakes — expanded | All entries after **Show all**; control reads "Show fewer" | Show fewer |
| Default — READ / REPEAT | Recognised as intended N %; "Pass mark reached" / "Not yet"; not-recognised words list; other headline rows | Listen again (REPEAT); Try again; More details |
| T3b | T3b headline set; "No transcript — audio-only"; transcript-based rows "Transcript needed for this check" | Try again; More details |
| Metric not available | Affected row "Not available — {reason}" | More details |
| Comparison | Previous vs current headline values with direction words (FS-04 A1) | — |
| Rated | Chosen answer shown selected; reasons if No | Change answer |
| Loading | FS-04 **processing** ("Getting your feedback…") | — |
| Empty | N/A — a result always has an outcome | — |
| Error | FS-04 **error** state (E1, E4) | Retry |
| Offline | Identical — evaluation is on-device; "Watch the original lesson" still fires the intent (the YouTube app or browser shows its own offline message) | — |
| Permission | N/A — no permission used | — |

**SCR-SESS-01 — inconclusive state**

| State | What the user sees | Actions |
|---|---|---|
| Default | "Inconclusive" badge; reason (§5.2) in place of Mistakes found; advice; "This attempt doesn't count against you."; no Coach tips (SRS-FR-EVAL-016 applies to valid attempts) | Retry; Skip; Was this accurate? (Q2) |
| Loading / Empty / Error / Offline / Permission | As the result state (processing / N/A / FS-04 error / identical / N/A) | — |

**SCR-SESS-02 — "How we measured this" sheet**

| State | What the user sees | Actions |
|---|---|---|
| Default | Per metric: what it is, label meaning, method, what was used, band, limitation, rules version; list of all other computed metrics | Close |
| Not available variant | The reason the metric could not be measured and what would make it measurable | Close |
| Confidence supplied | Additional line "Recogniser confidence: {n} %" | Close |
| Loading / Empty / Error | N/A — text generated locally from the bundled catalogue | — |
| Offline | Identical | — |
| Permission | N/A | — |

## 10. Data requirements

| Entity / store | Fields needed by FS-05 (minimum; ARC-03 authoritative) |
|---|---|
| ENT-03 Attempt | `result` (VALID / INCONCLUSIVE; PASS / FAIL where a threshold applies), `inconclusiveReason?`, `templateId`, `tier`, `evaluationVersion`, `contentVersion`, `strengthCode` + numeric params, `focusCode` + numeric params, `issues` (ARC-05 §8.4 issue code + numeric evidence: count / value, in display order), `tipIds` (selected, in order) + text snapshot of each tip (ARC-03 §3.4 `shown_tip_ids`, `shown_tips_snapshot`; local only, CL-76), `qualityWarning?`, `previousAttemptId?` — immutable once evaluated |
| ENT-04 AttemptMetric | `attemptId`, `metricKey` (M-*), `value?`, `unit`, `label` (MEASURED / ESTIMATED), `nullReason?`, `band?`, `isHeadline`, `headlineOrder?` |
| ENT-13 FeedbackRating | `ratingId`, `attemptId`, `profileId`, `accurate` (yes/no), `reasons` (TRANSCRIPT / METRIC / TIP), `createdAt`, `updatedAt`; local only, not synced |
| Self-check answers | `attemptId`, checklist item IDs ticked, `selfReported = true`, timestamp — entity not yet assigned (Issue 11); must not modify ENT-03 |
| `sensitive.db` | Transcript; text derived from speech used by feedback (quoted spans, filler examples, not-recognised words, observation spans) — never synced (SEC-01 §3.3, §7.4) |
| Bundled | Metric catalogue, band table, rule set with IDs/versions/explanations, "How we measured this" text — all versioned under `evaluationVersion` |
| Content pack | Reference text, framework and expected elements, target range, target word and inflections; technique tips (`tipId`, `techniqueId`, text, triggers, `general`, `priority`, provenance, `licenceStatus`) and technique records for links (FS-06 §10; ARC-03 §6 authoritative) |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `EvaluationPipeline` | Orchestrates gate → metrics → rules → feedback (ARC-01 §2) |
| `EvidenceGate`, `MetricEngine`, `ReferenceMatcher`, `RuleEngine`, `FeedbackComposer`, `WordStageEvaluator` | ARC-05 §2, §5–§9 components |
| `AttemptRepository` | Save attempt + metrics; previous valid attempt; save/update ENT-13 rating (owning interface to be fixed in ARC-04 §5) |
| `ContentRepository` | Reference texts, expected elements, target ranges, technique tips and technique records |
| `SessionViewModel` | Hosts result/inconclusive rendering (FS-04) |
| Platform | Outbound `Intent.ACTION_VIEW` for "Watch the original lesson" (no API key, no embedded player, SRS-FR-PRAC-009); all evaluation is on-device |

## 12. Offline behaviour

All evaluation, display, the detail sheet and the accuracy rating work offline on every tier; T3a transcription itself needs a connection (FS-04 A6, FS-12). Signed-in users' attempt metrics, feedback codes and issue codes with counts are queued for upload (FS-11); ratings and speech-derived text never are. "Watch the original lesson" needs a connection, handled by the app that opens the link.

## 13. Privacy requirements

- Evaluation runs entirely on the device; no cloud AI or LLM is used or bundled (ADR-003, NFR-SEC-006, NG-01).
- Uploaded attempt documents contain metric values and feedback **codes** only — no transcript, quoted spans, filler examples or other text derived from speech (SEC-01 §3.3, NFR-PRIV-002/003, SRS-FR-DATA-006).
- ENT-13 ratings stay on the device (SEC-01 §3.2 #5) and are covered by export and deletion (SRS-PRIV-003; FS-10).
- Transcripts and spans never appear in logs (NFR-SEC-002).
- Issue entries are synced only as codes and numbers; quoted evidence stays in `sensitive.db` (SEC-01 §3.3).
- The source-video link carries only the public video URL and timestamp — no user data or tracking parameters (RECOMMENDATION); leaving the app is visible ("Opens YouTube").

## 14. Accessibility notes

Measured / Estimated / Inconclusive / Pass mark reached / Not yet are text, not colour (NFR-A11Y-006, A11Y-23); focus moves to the result header, which announces the outcome ("Feedback. Valid attempt." / "Inconclusive. Too quiet.") (A11Y-09); each metric row is one merged node read as "Pace, 142 words per minute, Measured, comfortable, target 100 to 160" (A11Y-07, A11Y-34); not-recognised words are underlined and listed as text; comparison arrows have text equivalents; SCR-SESS-02 is a ModalBottomSheet with focus containment (A11Y-10); everything reflows at 200 % font scale (A11Y-13); Yes/No and reason chips are ≥ 48 dp with selected state (A11Y-19, A11Y-06). Mistakes found and Coach tips are headed sections; each mistake is one merged node ("Estimated. Filler words: basically 4 times, um 3 times, 5.2 per minute") (A11Y-07); Show all exposes expanded/collapsed state (A11Y-06); the source link is announced as "Watch the original lesson, opens YouTube" (A11Y-02).

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS05-01 | An attempt of 1.2 s | It is evaluated | The result is Inconclusive with the TOO_SHORT copy and Retry, no metric is shown and nothing counts |
| AC-FS05-02 | Fixtures for each reason code of §5.2 | Each is evaluated | The matching reason and copy are shown |
| AC-FS05-03 | A valid TPL-ANSWER attempt on a T1 device | The result is shown | M-WPM, M-FILL and M-LEN are shown, each with value, unit, Measured/Estimated text, band and target range |
| AC-FS05-04 | Valid TPL-ANSWER, TPL-STRUCT and TPL-STORY attempts | Results and SCR-PROG-04 are shown | No overall, composite or "x/100" score appears anywhere |
| AC-FS05-05 | A valid TPL-READ attempt with 3 reference words not recognised | The result is shown | "Recognised as intended: N %" with "Pass mark reached" or "Not yet" against 75 % †, and the 3 words listed; the word "pronunciation" does not appear |
| AC-FS05-06 | A valid TPL-STORY attempt with 20 recognised words | The result is shown | M-LEX reads "Not available — needs at least 25 words" and no value is substituted |
| AC-FS05-07 | Any valid attempt | The result is shown | Exactly one "What went well" and one "Try this next" are shown, and the focus is the first matching ARC-05 §8.2 condition |
| AC-FS05-08 | A valid answer using "basically" 4 times with M-FILL above band | The result is shown | The focus quotes "basically" and the count |
| AC-FS05-09 | A transcript triggering 3 qualified rule hits | The result is shown | At most 2 observations appear, each phrased "Possible issue: … → …" |
| AC-FS05-10 | A valid TPL-STRUCT (STAR) attempt | The result is shown | Detected and missing elements are listed with the cue phrases that triggered them, labelled Estimated |
| AC-FS05-11 | A result | The user opens "How we measured this" for M-FILL | SCR-SESS-02 shows the definition, label meaning, method, inputs used, band, the "um/uh" limitation and the rules version |
| AC-FS05-12 | The recogniser supplied no confidence values | Result and detail sheet are shown | No confidence value appears |
| AC-FS05-13 | A valid attempt | The user answers No with "The transcript is wrong" | An ENT-13 row exists, the ENT-03 row is unchanged and no rating appears in any sync payload |
| AC-FS05-14 | Any evaluated attempt | It is saved | `evaluationVersion` and `contentVersion` are stored with it |
| AC-FS05-15 | The same capture fixture and rule version | It is evaluated twice | Metrics, strength, focus and observations are identical |
| AC-FS05-16 | A valid attempt on a T3b device | The result is shown | M-SPK, M-PAUSE and M-RATIO are shown; transcript-based checks say "Transcript needed for this check"; no observations |
| AC-FS05-17 | An attempt evaluated with rules 1.0.0, app updated to 1.1.0 | The attempt is reopened from history | Its stored values and "version 1.0.0" are shown; it is not recomputed |
| AC-FS05-18 | A valid TPL-ANSWER raising ISSUE_FILLERS_HIGH, ISSUE_PACE_FAST, ISSUE_PAUSES_LONG and ISSUE_TOO_SHORT (ARC-05 §8.4) | The result is shown | Mistakes found lists the first three in ARC-05 §8.4 order, each with its evidence and Measured/Estimated label, and **Show all (4)** reveals the fourth |
| AC-FS05-19 | A valid attempt with every checked metric in band and no rule hit | The result is shown | Mistakes found says "Our checks didn't find issues in this attempt." with a coverage line naming the checks run; "perfect" or "no mistakes" does not appear |
| AC-FS05-20 | An Inconclusive attempt | The result is shown | The Inconclusive reason replaces Mistakes found and no Coach tips are shown |
| AC-FS05-21 | A valid attempt whose highest-priority issue is ISSUE_FILLERS_HIGH, with content tips triggered by that code and by the activity's technique | The result is shown, and the same fixture (same `attemptId`) is evaluated again | Coach tips show the focus first and at most 2 further tips ranked per ARC-05 §8.4, identical both times |
| AC-FS05-22 | A coach tip whose technique has verified provenance | The user taps the technique, then "Watch the original lesson" | SCR-PRAC-04 opens for that technique; the link fires an outbound intent to the specific video URL with its timestamp; no creator name appears in the tip |
| AC-FS05-23 | No tip trigger matches and the activity belongs to a technique | The result is shown | One general tip of that technique is shown after the focus |
| AC-FS05-24 | A Say it attempt where "adapt" was recognised for "adopt" | The result is shown | It states "Not yet", lists "We heard 'adapt', not 'adopt'." labelled Measured, and gives exactly one coach tip (SRS-FR-WORD-014) |
| AC-FS05-25 | Any valid result | It is rendered | Sections appear in the order transcript → headline metrics → What went well → Mistakes found → Coach tips → actions (one filled button: "Continue" in Today, "Try again" in Practice and Words; DES-05 §11.15) → "Was this accurate?" |
| AC-FS05-26 | A technique exercise whose technique defines a self-check (e.g. gestures) | The user ticks two items | The ticks are saved as Self-reported, the ENT-03 row and metrics are unchanged, and no issue or metric is derived from them |

## 16. Test cases

No test case has been executed in Stage A (GOV-02 DC-4).

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-EVAL-001 | Gate fixtures | unit | WAV/transcript fixtures: silence, noise only, < 1.5 s, valid speech | Run gate | 100 % classified as expected | SRS-FR-EVAL-001, NFR-QUAL-002 |
| TC-EVAL-002 | Gate boundaries | unit | Synthetic inputs | 1.49 / 1.50 s; voiced 0.49 / 0.50 s; 24 / 25 %; 7 / 8 words; 0 / 1 word (READ) | Inconclusive below, valid at threshold † | SRS-BR-001 |
| TC-EVAL-003 | Reason copy mapping | unit | Each reason code | Map to UI text | Exact §5.2 copy; no enum names shown | SRS-FR-SESS-007, SRS §15 |
| TC-EVAL-004 | Inconclusive shows no metrics | Robolectric UI | Inconclusive result | Render | No metric rows, no strength; Retry, Skip, reassurance present | SRS-FR-EVAL-001, SRS-BR-008 |
| TC-EVAL-005 | Headline sets | unit | Table of template × tier | Build result | Headline metrics per §5.3 | SRS-FR-EVAL-004 |
| TC-EVAL-006 | Labels | unit | All metric keys | Read label | Matches SRS-BR-003 list | SRS-FR-EVAL-003 |
| TC-EVAL-007 | Null rendering | Robolectric UI | Metrics with each null reason | Render rows | "Not available — {reason}"; no zero | SRS-FR-EVAL-002, SRS-BR-002 |
| TC-EVAL-008 | No composite score | unit + Robolectric UI | ANSWER/STRUCT/STORY results | Inspect `EvaluationResult` and UI text | No overall field; no score text | SRS-FR-EVAL-007, SRS-BR-006 |
| TC-EVAL-009 | READ threshold | unit | M-MATCH 74 % and 75 % † | Evaluate | Not yet / Pass mark reached | SRS-FR-EVAL-008, SRS-BR-007 |
| TC-EVAL-010 | Reference matching normalisation | unit | Contractions, numbers, casing | Align | Normalised matches; correct not-recognised list | SRS-FR-EVAL-008 |
| TC-EVAL-011 | Focus priority | unit | One fixture per priority 1–8 | Compose | Highest applicable focus selected | SRS-FR-EVAL-005 |
| TC-EVAL-012 | Focus quoting | unit | Transcripts with fillers / not-recognised words | Compose | Quote and count from the transcript | SRS-FR-EVAL-005 |
| TC-EVAL-013 | Strength guards | unit | Null and low-evidence Estimated metrics; improvement case | Compose | Never null or low-evidence; improvement chosen; factual fallback | SRS-FR-EVAL-005 |
| TC-EVAL-014 | Observation cap and phrasing | unit | 3 rule hits | Evaluate | 2 shown; "Possible issue" pattern | SRS-FR-EVAL-006 |
| TC-EVAL-015 | Rule precision gate | unit | SPK-02 corpus (≥ 30 positive / 30 negative per rule) | Run corpus in CI | Every shipped rule ≥ 0.90 † | SRS-FR-EVAL-006, NFR-QUAL-003 |
| TC-EVAL-016 | Structure elements | unit | STAR/PREP transcripts | Evaluate | Elements and cue phrases; Estimated | SRS-FR-EVAL-014 |
| TC-EVAL-017 | Detail text from catalogue | unit | Catalogue fixture | Generate SCR-SESS-02 text | Snapshot equals catalogue; limitation present for every headline metric | SRS-FR-EVAL-010 |
| TC-EVAL-018 | Banned terms | unit | All result/detail string resources | Scan | No "AI", "pronunciation", "accent", "accurate", "verified", "overall score", creator names | SRS-BR-004, DES-05 §12.3 |
| TC-EVAL-019 | Confidence display | unit | Recogniser with / without confidence | Build detail | Shown only when supplied | SRS-FR-EVAL-012, SRS-BR-005 |
| TC-EVAL-020 | Rating separate from attempt | unit | In-memory Room | Save, change rating | ENT-13 row upserted; ENT-03 bytes unchanged | SRS-FR-EVAL-009, SRS-FR-DATA-002 |
| TC-EVAL-021 | Rating never synced | unit | Outbox and payload mappers | Rate; build sync payloads | No rating fields; no speech-derived text | NFR-PRIV-003, SEC-01 §3.3 |
| TC-EVAL-022 | Versions stored | unit | Evaluate attempts | Read rows | `evaluationVersion`, `contentVersion` present | SRS-FR-EVAL-011 |
| TC-EVAL-023 | Golden determinism | unit | Golden fixtures per `evaluationVersion` | Evaluate twice | Identical to golden output | NFR-QUAL-006 |
| TC-EVAL-024 | Metric accuracy | unit | Annotated fixture set | Compute M-WPM, M-PAUSE | WPM within ±10 %, pauses within ±1 | NFR-QUAL-004 |
| TC-EVAL-025 | Evaluation latency | instrumented | RD-MID and RD-LOW, 60 s fixture | Stop → result 10× | P90 within NFR-PERF-005 † | NFR-PERF-005 |
| TC-EVAL-026 | Result accessibility | Robolectric UI + manual | fontScale 2.0; TalkBack on RD-MID | Render / navigate result and sheet | Header focus and announcement; merged rows; text badges; no clipping | NFR-A11Y-001, -006 |
| TC-EVAL-027 | Honesty review | manual | TST-01 §9 checklist; one attempt per template and tier | Review every result screen | No unsupported number, label or claim | PRD-NFR-001, PRD §21 item 3 |
| TC-EVAL-028 | Mistake detection and order (golden) | unit | Fixed transcripts and fixed metric values per template and tier, incl. values just inside the noise band; expected lists of ARC-05 §8.4 codes | Evaluate each fixture | Codes, evidence and order equal the golden list; null metrics and within-noise-band values raise nothing | SRS-FR-EVAL-015, ARC-05 §8.4 |
| TC-EVAL-029 | First 3 and Show all | Robolectric UI | Result with 5 issues | Render; tap Show all; tap Show fewer | 3 entries, then 5; labels as text; state announced | SRS-FR-EVAL-015 |
| TC-EVAL-030 | Coach tip selection (golden) | unit | Fixed transcripts, metrics, activity, `attemptId`, content version and a tip-catalogue fixture | Select tips twice; then change only `attemptId` | Focus first, ≤ 2 more, order equals golden and identical runs; changing `attemptId` may reorder only equally ranked tips (stable hash) | SRS-FR-EVAL-016, ARC-05 §8.4, NFR-QUAL-006 |
| TC-EVAL-031 | Tip fallbacks | unit | No issue raised; no matching trigger; activity with and without technique | Select tips | General technique tip as focus; the template's general tip when no technique (§5.7 rule 6) | SRS-FR-EVAL-016, ARC-05 §8.4 |
| TC-EVAL-032 | Honest empty state | unit + Robolectric UI | All-in-band fixtures for each template and T3b | Render | Empty copy + coverage line from checks run; no "perfect"/"no mistakes" | SRS-FR-EVAL-015, PRD-FR-047 |
| TC-EVAL-033 | Inconclusive replaces sections | Robolectric UI | Inconclusive result | Render | Reason shown; no Mistakes found or Coach tips nodes | SRS-FR-EVAL-015, SRS-BR-008 |
| TC-EVAL-034 | Source link and technique link | Robolectric UI | Tip with provenance; intent capture; no-handler variant | Tap links | `ACTION_VIEW` with exact URL + timestamp; SCR-PRAC-04 route; snackbar when unhandled | SRS-FR-PRAC-009, SRS-FR-PRAC-008 |
| TC-EVAL-035 | Word-stage mistakes and one tip | unit | Stage 2–4 and review fixtures (wrong form, missing word, too short) | Evaluate | Pass/Not yet/Inconclusive; entries with evidence; exactly one tip | SRS-FR-WORD-014 |
| TC-EVAL-036 | Section order | Robolectric UI | Valid result | Read semantics order | Order per AC-FS05-25 | SRS-FR-EVAL-015, -016 |
| TC-EVAL-037 | Tip licence filter | unit | Catalogue fixture with PENDING_REVIEW tips and a banned-name list | Select tips; scan rendered strings | PENDING_REVIEW never selected; no creator name in any output | SRS-BR-028 |
| TC-EVAL-038 | Self-check is self-reported | unit + Robolectric UI | Technique with self-check items | Tick items; inspect DB and UI | "Self-reported" label; ENT-03/ENT-04 unchanged; no issue raised | ARC-05 §8.4, PRD-FR-047 |

## 17. Traceability

| PRD-FR | SRS-FR | AC | TC |
|---|---|---|---|
| PRD-FR-032 | SRS-FR-EVAL-001; SRS-FR-SESS-007; SRS-BR-001, -008 | AC-FS05-01, -02 | TC-EVAL-001…004 |
| PRD-FR-040 | SRS-FR-EVAL-002, -003, -004, -014; SRS-BR-002, -003 | AC-FS05-03, -06, -10, -16 | TC-EVAL-005, -006, -007, -016, -024 |
| PRD-FR-041 | SRS-FR-EVAL-005 | AC-FS05-07, -08 | TC-EVAL-011, -012, -013 |
| PRD-FR-042 | SRS-FR-EVAL-006 | AC-FS05-09 | TC-EVAL-014, -015 |
| PRD-FR-043 | SRS-FR-EVAL-007, -008; SRS-BR-004, -006, -007 | AC-FS05-04, -05 | TC-EVAL-008, -009, -010 |
| PRD-FR-044 | SRS-FR-EVAL-009 | AC-FS05-13 | TC-EVAL-020, -021 |
| PRD-FR-045 | SRS-FR-EVAL-010 | AC-FS05-11 | TC-EVAL-017, -018 |
| PRD-FR-046 | SRS-FR-EVAL-011 | AC-FS05-14, -17 | TC-EVAL-022 |
| PRD-FR-047 | SRS-FR-EVAL-015, -016; SRS-FR-WORD-014 (ARC-05 §8.4) | AC-FS05-18…26 | TC-EVAL-028…036, -038 |
| PRD-FR-056 (tips part; library in FS-06) | SRS-FR-EVAL-016; SRS-FR-PRAC-008 | AC-FS05-21, -22, -23 | TC-EVAL-030, -031, -034 |
| PRD-FR-057 (tips part; content in FS-06) | SRS-FR-PRAC-009; SRS-BR-028 | AC-FS05-22 | TC-EVAL-034, -037 |
| PRD-NFR-001 | SRS-FR-EVAL-012; SRS-BR-005; NFR-QUAL-001, -006 | AC-FS05-12, -15 | TC-EVAL-019, -023, -027 |
| PRD-NFR-004, -006 | SRS-FR-EVAL-013; NFR-PERF-005; NFR-A11Y-001/006 | — | TC-EVAL-025, -026 (+ TC-SESS-023) |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Approve the band values of §8.2 or wait for SPK-01 / fixture tuning? | Product Owner / AI-ML Engineer |
| Q2 | Offer "Was this accurate?" on Inconclusive results too (useful for KPI-06)? RECOMMENDATION: yes | Product Owner |
| Q3 | Should a strength be shown at all when no metric qualifies (§5.5 fallback) — or show none? | Product Owner (D-02) |
| Q4 | Should Coach tips also appear on Inconclusive results (SRS-FR-EVAL-016 covers valid attempts only)? RECOMMENDATION: no — the reason advice is the only tip | Product Owner |
| Q5 | Should M-LEX be a headline metric for TPL-STORY given its 25-word minimum (Issue 5)? | AI-ML Engineer |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | Template applicability in the catalogue contradicts the headline table: M-PAUSE is defined for ANSWER/STRUCT/STORY only but is a TPL-READ headline; M-RATIO is defined for ANSWER/STRUCT/STORY only but is a headline for T3b "any template" (including READ/REPEAT) | ARC-05 §6 vs §7 | Extend §6 applicability or change §7 |
| 2 | TPL-REPEAT headline 2 "M-SPK vs reference duration" is not a catalogue metric (no key, label, band or method) | ARC-05 §7 vs §6 | Add a catalogue entry or use an existing metric |
| 3 | SRS requires a band and a target range for every headline metric, but ARC-05 defines a band only for M-WPM; M-PAUSE, M-FILL, M-LEN, M-LEX, M-STRUCT, M-RATIO have none. ARC-05 §8.4 raises issues only when a metric is outside its band by more than its noise band, so ISSUE_FILLERS_HIGH, ISSUE_PAUSES_LONG, ISSUE_LOW_VARIETY and ISSUE_HEDGING_HIGH cannot be raised until bands exist, and M-HEDGE has neither band nor noise band | SRS-FR-EVAL-004, ARC-05 §8.4 vs ARC-05 §6 | Add bands (and missing noise bands) to the catalogue (§8.2 proposes values †) |
| 4 | The accuracy rating is "stored with the attempt" and SRS §9 lists it in the synced Attempts row, while attempts are immutable facts and the canon/SEC-01 keep ratings in a separate, local-only table (ENT-13) | SRS-FR-EVAL-009, SRS §9 vs SRS-FR-DATA-002, SEC-01 §3.2 #5 | Reword SRS-FR-EVAL-009 to "linked to the attempt" and mark ratings local-only in SRS §9 |
| 5 | M-LEX needs ≥ 25 words but the gate accepts 8 words, so the TPL-STORY headline is often "Not available" on valid short attempts | ARC-05 §6 (M-LEX) vs SRS-BR-001; ARC-05 §7 | Choose another STORY headline or a lower M-LEX minimum |
| 6 | Inconclusive reasons differ: SRS lists 4 (no "too few words"); ARC-05 has 5 codes including TOO_FEW_WORDS; the session-scope check has no reason code | SRS-FR-SESS-007 vs ARC-05 §5 | Align the lists; map session-scope failure explicitly |
| 7 | "Exactly one strength" is required even when nothing qualifies (Inconclusive, all metrics null), which conflicts with "never praise a null or low-evidence metric" | SRS-FR-EVAL-005, ARC-05 §8.2 | Allow no strength on Inconclusive and define a factual fallback |
| 8 | M-SPK, M-RATIO and M-PAUSE need "Audio"; in T2/T3a the recogniser owns the microphone and only level callbacks exist. Whether these metrics are computed there (and at what resolution) is unspecified, so READ/STORY headlines on T2 devices are undefined | ARC-05 §6, §5; ADR-009 (T2 "reduced acoustic metrics") | State per-tier availability in the catalogue |
| 9 | PRD-FR-056 (R1, D-08 decided 2026-11-21) brings in techniques about vocal delivery (vocal variety, pitch, volume, emphasis). ARC-05 §8.4 lists pitch range and volume variation only as T1-only candidates outside the R1 catalogue, so in R1 — and on T2/T3 devices at all — such exercises get no relevant metric or mistake | PRD-FR-056 vs ARC-05 §6, §8.4 (candidate acoustic metrics) | Decide whether vocal-delivery techniques ship in R1 with self-check only, or wait for SPK-01 validation; this FS adds no metric IDs |
| 10 | Mistakes are ordered by the ARC-05 §8.2 priorities, but §8.2 has no priority for ISSUE_WRONG_FORM, ISSUE_TARGET_WORD_MISSING, ISSUE_PACE_SLOW, ISSUE_HEDGING_HIGH, ISSUE_LOW_VARIETY or ISSUE_TOO_QUIET, so their order is undefined | SRS-FR-EVAL-015, ARC-05 §8.4 vs §8.2 | Extend the §8.2 priority table to every §8.4 issue code |
| 11 | Self-check answers (ARC-05 §8.4) have no entity in the canon (ENT-01…18) and cannot be stored in ENT-03, which is immutable once evaluated; whether they sync or export is undefined | ARC-05 §8.4; PLAN_AND_CANON R-1; SRS-FR-DATA-002 | ARC-03 to assign storage (e.g. alongside ENT-13) and state sync/export |
| 12 | The fallback "a general tip for the activity's technique" is undefined when an activity has no technique (e.g. `onb-intro`, everyday prompts written by the team) | SRS-FR-EVAL-016 | State that only the focus tip is shown, or require a general tip per template. Consistency review (CL-49): fallback = the template's general tip (§5.7 rule 6); SRS-FR-EVAL-016 wording still needed |
| 13 | Terminology: DES-05 names the focus "Try this next", DES-02 §6 bans "Coach verdict", and the new section is called "Coach tips" with the focus as its first item — two names for one concept | SRS-FR-EVAL-016 vs DES-05 §12.2, DES-02 §6 | Confirm "Coach tips" as a section label and keep "Try this next" for the focus (as §5.7 does) in DES-05 §12.2. Consistency review (CL-49): heading "Coach tips", first card "Try this next" (DES-05 §11.15b) |
