# 10 — Feature Value Assessment

| Field | Value |
|---|---|
| Document ID | RD-10 |
| Version | 0.2 — Draft for review (2026-11-21: row 9 disposition changed by Product Owner direction / D-08 decision) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager |
| Approval | Pending — Product Owner |
| Related | RD-03, RD-04, RD-07, RD-16 §5, PRD |

---

## 1. Method

Every existing user-facing feature and major subsystem is classified (**Essential, High value, Supporting, Optional, Experimental, Redundant, Low value, Not currently needed**) and rated **H/M/L** on: user value (for personas P1–P5), business value (BO-01…BO-06), development complexity (to bring to target quality), maintenance cost, technical feasibility on-device, performance impact, privacy impact (H = high risk), and dependency. Implementation status comes from RD-03/RD-04. **No feature is kept merely because it exists** (PO-REQ-1120 §5.4).

## 2. Assessment

| # | Current feature | Status today (RD-03/04) | Class | User value | Business value | Dev complexity | Maint. cost | Feasibility | Perf impact | Privacy impact | Depends on | Priority | Disposition |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Speaking attempt (record → analyse) — Speaking Lab | Partial; no transcript by default | **Essential** | H | H | H | M | H (tiers) | M | M | Speech tiers | P0 | **Rebuild** as session player (CAP-04) |
| 2 | Evidence gate & nullable scores | Implemented, tested | **Essential** | H | H | L | L | H | L | L | — | P0 | **Keep & extend** (ARC-05 §5) |
| 3 | Evaluation scores (Speaking Lab engine) | Heuristic; mislabelled | **Essential** | H | H | M | M | H | L | L | Transcript | P0 | **Rebuild** as metric catalogue (CAP-05) |
| 4 | Activity Engine scoring (length formula) | Misleading | Redundant | L | L | — | M | — | L | L | — | — | **Remove** (merge into CAP-05) |
| 5 | Live transcription (SpeechRecognizer) | Partial (default off; 1 utterance) | **Essential** | H | H | M | M | H | L | M (network fallback) | Platform | P0 | **Rebuild** (RecognitionSession, ADR-009) |
| 6 | Whisper/Moonshine TFLite ASR | Fabricated | Experimental | — | — | H | H | L (as built) | H | L | Model files | — | **Remove**; SPK-03 research R3 |
| 7 | Daily plans ×3 (routine, dashboard plan, activity plan) | Duplicated; partly broken | Essential concept / Redundant impl. | H | H | M | H (×3) | H | L | L | Content | P0 | **Merge** into one Today session (CAP-03) |
| 8 | 150-activity catalogue & 25 templates | Implemented (structure); content reusable | **High value** (content) | H | M | M | M | H | L | L | — | P0 | **Merge**: content into 5 tracks × 5 templates (CAP-06) |
| 9 | 15 curriculum modules, 12 lessons, creator knowledge base | Implemented; creator-derived | ~~Low value / legal risk~~ **High value (content basis) — superseded 2026-11-21** | H | H | M | M | H | L | L | D-08 guardrails (SRS-BR-028) | P0 | ~~Remove~~ **Keep the techniques: verify, re-express in our own words, link to the specific source video, remove branding** (Product Owner direction; D-08 decided "original wording, link out"; PRD-FR-056/057; CNT-01). Module/lesson *structure* still merges into tracks + Techniques collection (CAP-06) |
| 10 | 30/60/90 programs, 10 learning paths, "My Learning Tracks" | Partial; 6/7 uncompletable | Redundant | L | L | — | M | H | L | L | — | — | **Remove** (tracks replace) |
| 11 | Labs ×8 (pronunciation, articulation, voice, listening, grammar, public speaking, challenges, real-world tasks) | Same engine; overlapping | Redundant | M (content) | L | — | H | H | L | L | — | — | **Merge** content into tracks/templates; remove screens |
| 12 | Read-aloud / listen-and-repeat drills with TTS reference | Partial (no target alignment used) | **High value** | H (P5, P3) | M | L | L | H | L | L | TTS | P0 | **Rebuild** as TPL-READ/REPEAT with M-MATCH |
| 13 | Conversation simulator (template replies) | Template, misleading | Low value (as is) | M | L | — | M | M | L | L | — | P2 | **Remove**; replace with scripted role-play R2 |
| 14 | AI Mentor chat (template, simulated streaming) | Misleading | Low value | L | L (trust risk) | — | M | L | L | L | — | — | **Remove** (NG-02) |
| 15 | Vocabulary (20 words, flashcards, usage check) | Partial; thin | **Essential** (concept) | H | H | M | M | H | L | L | Content (≥300 words) | P0 | **Rebuild** as 5-stage lifecycle (CAP-07) |
| 16 | LPM 5-stage vocab, SRS, quiz, notebook (dead code) | Dead | High value (design) | H | M | M | M | H | L | L | — | P1 | **Reuse design/algorithms** in CAP-07 |
| 17 | Progress analytics screen | Misleading values | **Essential** (concept) | H | H | M | M | H | L | L | Valid attempts | P0 | **Rebuild** evidence-only (CAP-08) |
| 18 | Streaks ×4 | Duplicated, gameable | Essential concept / Redundant impl. | H | M | L | M | H | L | L | — | P0 | **Merge** into one derived streak |
| 19 | Onboarding (3 steps + sliders) | Implemented; misleading reuse | **Essential** (concept) | H | H | M | L | H | L | L | — | P0 | **Rebuild** with first attempt (CAP-02) |
| 20 | Google sign-in (Credential Manager) | Implemented | Supporting | M | M | L | L | H | L | M | Play services | P1 | **Keep**, make optional (CAP-01) |
| 21 | Account deletion | Dead (no UI) | **Essential** (compliance) | M | H | M | L | H | L | L (reduces risk) | Re-auth | P0 | **Rebuild & wire** (CAP-10) |
| 22 | Settings (29 controls incl. dev diagnostics, cloud AI switch, YouTube key) | Partial; cluttered | Supporting | M | M | M | M | H | L | M | — | P1 | **Rebuild** in 7 sections (CAP-10) |
| 23 | Theme light/dark/system | Implemented | Supporting | M | L | L | L | H | L | L | — | P1 | **Keep** (new palette DES-05) |
| 24 | Notification drawer (in-app, 2 stores) | Partial; duplicates | Optional (as is) | L | L | — | M | H | L | L | — | P2 | **Replace**: OS reminders (R1) + inbox (R2) |
| 25 | Reminders (OS notifications) | Absent | **High value** | H | H | M | L | H | L | L | WorkManager (ADR-005) | P1 | **Build** (CAP-09) |
| 26 | Launch animation (2.1–2.9 s) | Implemented | Not currently needed (as is) | L | L | L | L | H | M | L | — | P3 | **Simplify** to ≤ 1 s |
| 27 | Tutorial systems ×4 (24 features) | Implemented; overwhelming | Low value | L | L | — | M | H | L | L | — | — | **Remove**; contextual tips |
| 28 | Welcome-back banner & hero carousel | Partial; mostly static | Optional | M | L | L | L | H | L | L | — | P2 | **Replace** with Today greeting + session card |
| 29 | YouTube Intelligence (scraping) | Implemented; ToS risk | Low value / legal risk | L | L (risk) | — | H | L (fragile) | M | M | YouTube | — | **Remove** (ADR-011) |
| 30 | Admin console (moderation, lessons, releases) | Partial; over-broad rules | Not currently needed | — | L | H (needs backend) | H | M | L | M | Server auth | — | **Remove** (NG-10) |
| 31 | In-app APK updater & backups | Partial; install likely broken | Not currently needed | L | L (risk) | — | H | M | L | M | GitHub | — | **Remove** (ADR-004) |
| 32 | Cloud AI (Firebase AI Logic, Gemini Live) | Dormant | Not currently needed | — | — (constraint) | — | M | H | L | **H** | Cloud | — | **Remove** (ADR-003) |
| 33 | Video recording + MediaPipe body language | Non-functional (no models) | Experimental | M | L | H | H | M | H | **H** | Models | P3 | **Remove**; R3 video self-review |
| 34 | Multimodal fusion engine | Pass-through mapper | Redundant | — | — | — | L | — | L | L | — | — | **Remove** (absorbed into MetricEngine) |
| 35 | Recommendation engines ×4 | Duplicated; substring bugs | Redundant | M | M | — | M | H | L | L | — | — | **Merge** into PlanEngine |
| 36 | Firestore sync of user data | Implemented (fragmented; saves broken) | **Essential** (supporting) | M | H | M | M | H | L | M | Auth | P1 | **Rebuild** local-first (CAP-11) |
| 37 | Local JSON learning state | Implemented; unsynced | Redundant | — | — | — | M | H | M (main thread) | L | — | — | **Replace** with Room (ADR-002) |
| 38 | Firestore rules (per-user) | Strong patterns; mismatches | **Essential** (security) | — | H | M | M | H | L | L | — | P0 | **Rewrite** for target schema, keep patterns |
| 39 | CI release workflow | Implemented; no gates | Supporting | — | H | M | M | H | — | — | Secrets | P0 | **Rework** (ENG-01) |
| 40 | Unit/Robolectric test harness | Implemented; never in CI | Supporting | — | H | L | M | H | — | — | — | P0 | **Keep harness**; rewrite suites (TST-01) |

## 3. Summary by disposition

| Disposition | Features (#) |
|---|---|
| Keep (as is or minor) | 2, 20, 23 |
| Keep & extend / reuse design | 16, 40 |
| Rebuild | 1, 3, 5, 12, 15, 17, 19, 21, 22, 36, 38, 39 |
| Merge | 7, 8, 11, 18, 35 |
| Keep — verify and re-express (D-08, 2026-11-21) | 9 (techniques kept; module/lesson structure merged into tracks and the Techniques collection) |
| Build new | 25 |
| Simplify / replace | 24, 26, 28, 37 |
| Remove | 4, 6, 10, 13, 14, 27, 29, 30, 31, 32, 33, 34 |

**Result:** 13 of 40 features/subsystems removed, 5 merged, 4 replaced or simplified — the product surface shrinks from 26 routes to 4 primary destinations and ~37 screens (DES-02), while the essential value (speaking with honest feedback, daily habit, vocabulary in use, evidence-based progress, privacy) is rebuilt to production quality.
