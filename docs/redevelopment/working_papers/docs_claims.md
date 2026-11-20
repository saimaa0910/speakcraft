# SpeakCraft — Documentation Claims Extract (scratch, for analysts)

Prepared by: Technical Writer / BA (documentation inventory workstream). Date: 2026-11-20.
Source IDs: `DOC-nnn` as defined in `f:\speakcraft\docs\redevelopment\01_DOCUMENT_INVENTORY.md` (RD-01). Contradictions are listed in `f:\speakcraft\docs\redevelopment\05_DOCUMENT_CONFLICT_REGISTER.md` (RD-05, `CONF-nnn`).

**How to read the "Claimed status" column.** It records what the *document* says, not what is true. "Implemented", "Fixed", "Verified" and "Passing" are all self-reported by AI agents. Code spot-checks are noted only where I made them, marked **[chk ✓]** (matches HEAD `68a9cb1`), **[chk ✗]** (contradicted) or **[chk ~]** (partial). Code auditors should treat everything else as a hypothesis.

Quick DOC key:
- 001–007: baseline audit (11-18)
- 008–017: UI/UX redesign
- 018–020: launch animation
- 021: OTA/CI runbook
- 022–031: activity engine
- 032–043: Stage 1 local AI
- 044–049: Stage 1.2
- 050–055: app/docs (11-20)
- 056: index
- 057: metadata.json
- 058: firebase-blueprint.json
- 059: Paste_November_19 prompt (a product request, not a status report)

---

## 1. Feature and capability claims by domain

### 1.1 Onboarding and authentication

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Google Sign-In via Credential Manager with silent auto-sign-in (`GetGoogleIdOption`) and interactive `GetSignInWithGoogleOption`. Anonymous auth is prohibited. | 001, 002, 005 (FEAT-001) | Implemented | — |
| 2 | "Checking Google session..." indicator during silent sign-in. | 008 (proposed), 012, 017 | Fixed (017) | — |
| 3 | Onboarding: name, level, goal, daily-goal slider and 7 skill sliders on one screen; auto-enrolls the user in the 30-day challenge. | 001, 005 (FEAT-002) | Implemented (baseline) | 8 sliders; daily goal 5–60 min; 4 levels |
| 4 | Onboarding rebuilt as a 3-step wizard (Identity & Level / Goal & Pace / Skill Baseline) with `StepProgressIndicator`, `FlowRow` chips and presets. | 012, 015, 016, 017 | Fixed / Verified (016) [chk ~: `StepProgressIndicator` exists] | 3 steps |
| 5 | The sign-in flow is not blocked by the launch animation (it plays as an overlay above `AuthScreen`). | 018, 019, 020 | Implemented / Pass | — |
| 6 | (Request) Personalised welcome-back greeting with avatar, track, day, score and message. Respects reduced motion and does not replay on recomposition. | 059 Phase 1 | Requested | — |
| 7 | (Request) Sign-out with confirmation, clearing the session without deleting history. | 059 Phase 6 | Requested | — |

### 1.2 Dashboard and home

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Baseline home: hero card with `RecommendationEngine` next action, 4 KPI tiles, sparkline, and a "wall" of 15 modules plus 7 tools. | 001, 005 (FEAT-005), 008 | Implemented (baseline); criticised as a "22-tile directory wall" | 22 tiles, 5 summary cards |
| 2 | Redesigned home: action-first hero; routine progress (X/9); 4 hub shortcuts; trend card with Weekly/Monthly filtering; searchable and filterable module directory (`dashboard_module_search`). | 010, 012, 017 | Fixed / Verified (016) | 4 hubs. Contents differ: Learning Paths/Vocabulary/Conversation/Analytics (010/012) vs Speaking Studio/Daily Practice/AI Roleplay/Analytics (017) — CONF-023 |
| 3 | Tablet layouts: 4-up metrics, 3-column module grid, hero beside routine, containers up to 1160dp. | 016, 017 | Implemented / Verified | Breakpoints 600/840dp [chk ✓ AppNavigation.kt:678-679] |
| 4 | "Activity Engine" tool tile on the dashboard; "150 Activities" sidebar entry. | 022, 030 | Implemented | 150 |
| 5 | (Request) Live dashboard with 12 sections: welcome, today's plan, completion %, streak, practice time, speaking performance, vocabulary progress, level, weekly chart, recommended next, achievements, continue learning. Updates only after a validated completion event; idempotent. | 059 Phase 3, Phase 11 | Requested [chk ~: `HorizontalPager` exists in `DashboardStudioWidgets.kt`] | 12 sections; 8-item layout order |
| 6 | (Request) Swipeable learning-plan carousel (HorizontalPager, snap, dots as indicators only). Example cards: "The 3 Vs of Communication", "Speak with Clarity", "Speak with Confidence". | 059 Phase 1 | Requested | 3 example cards |

### 1.3 Daily practice

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Legacy daily checklist: 9 steps, 3 rotated daily words, reflection, confidence slider 1–10. | 001, 005 (FEAT-024) | Implemented | 9 steps; 3 words; 1–10 |
| 2 | Streak bug: increments across date gaps; hardcoded +3 minutes per toggle. | 004 (ISS-002) | Confirmed bug (baseline) → Fixed (017, `StreakAndAnalyticsHelper`) | +3L |
| 3 | `DailyPracticeEngine`: personalised 10/20/30/45/60-minute routines from the 150-activity catalogue; anti-repetition (−55 if in the last 15 attempts, −20 if completed); level match +25 / ±1 tier +10 / 2-tier jump −12; weakest skill +30; goal keyword +22; daily rotation by `hashCode xor dateKey % 13`; block swap (`swapBlockInPlan`); persisted to `dailyPracticePlans/{dateKey}`. | 026, 029, 031 | Implemented / Tested | 5 budgets; up to 7 pillars; 8–9 blocks at 60 min |
| 4 | (Request) `DailyPracticePlanEngine` driven by goals, current and target level, scores, mistakes, completion, vocabulary performance, frequency, time available and weakest skills. Adaptive difficulty; recovery plan after a missed day; versioned plans that never change mid-session. Example beginner plan of 20 min and intermediate plan of 23 min. | 059 Phase 2 | Requested [chk ~: `data/learning/DailyPracticePlanEngine.kt` exists, undocumented; parallel to #3 — CONF-045] | Beginner 5+3+3+5+4 min; Intermediate 5+5+5+5+3 min |

### 1.4 Activities and curriculum

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Legacy curriculum: 15 communication modules, 20 categories, lessons (bundled plus admin-published), 30/60/90-day programmes. | 001, 002, 005 (FEAT-007..010) | Implemented [chk ✓ 15 modules in CurriculumCatalog.kt:167-183] | 15 modules; 20 categories; 30/60/90 days |
| 2 | @askvinh pedagogy: 3Vs, 5 Vocal Foundations, 3-Stage Self-Audit, PREP, CLEAR, 4 Ways of Listening. Creator advice is kept separate from AI supplements (`LessonAttributionBadge`). | 001, 005, 006, 009 | Implemented | — |
| 3 | Universal Activity Engine: a config-driven pipeline with a 20-property `ActivityDefinition`, `ActivityValidator`, and one `ActivityScreen(activityId)` renderer instead of 150 screens. | 022, 024, 030 | Implemented | 20 properties |
| 4 | Catalogue: 150 activities in 10 modules (15/15/15/15/15/15/15/10/15/20), covering all 25 templates. | 023, 031 | Implemented / Tested [chk ✓ 150 unique IDs with exact distribution] | 150; 10 modules |
| 5 | 25 exercise templates mapped to 7 interaction families; 4 lifecycle stages (Ready / Active / Evaluating / Completed). | 024 | Implemented [chk ✓ 25 enum values] | 25 / 7 / 4 |
| 6 | 10 learning paths with prerequisites (70% threshold), milestones and non-linear jumps. | 027, 031 | Implemented [chk ✓ 10 path IDs] | 10 paths; 12–15 activities each; 3 milestones each |
| 7 | 5-tab discovery hub (All 150 / Daily Plan / 10 Paths / Saved & Recent / Analytics); routes `activity_library`, `activity_library/{moduleId}`, `activity/{activityId}`. | 022, 030 | Implemented [chk ✓ routes exist] | 5 tabs; 3 routes |
| 8 | Future: remote curriculum content packs, pitch-contour visuals, spaced-repetition review of activities scoring < 70, coach-assigned playlists. | 030 | Proposed | review at 3/7/14 days |

### 1.5 Speaking labs and recording

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Microphone recording: AAC .m4a, 44.1 kHz, 96 kbps, pause/resume, 30-bar live waveform. | 001, 002, 005 (FEAT-016) | Implemented | 44.1 kHz; 96 kbps; 30 bars |
| 2 | In-app CameraX video (SD .mp4); Photo Picker attachment capped at 19 MB. | 001, 002, 005 (FEAT-017/018) | Implemented | 19 MB |
| 3 | 3-Stage Self-Audit (Mute Pass / Vocal Eyes-Closed / Full Review): broken at baseline (no video surface). | 001, 004 (ISS-001), 005 (FEAT-019) | Broken (baseline) → Fixed via `VideoReviewPlayerCard` (017) [chk ✓ class exists] | 3 stages |
| 4 | Cloud-sync failure banner with "Retry Cloud Sync" instead of a false success. | 004 (ISS-003), 012, 017 | Fixed [chk ✓ `CloudSyncWarningBanner` exists] | — |
| 5 | Size checked before reading bytes (19 MB guard). | 004 (ISS-005), 017 | Fixed | 19 MB |
| 6 | Monotonic recording timer plus actual duration from `MediaMetadataRetriever`. | 004 (ISS-020), 017 | Fixed | — |
| 7 | Labs: Pronunciation (IPA, minimal pairs, 0.75x/1.0x TTS), Voice Training (5 Vocal Foundations), Listening & Shadowing, Grammar (PREP/CLEAR), Public Speaking, Real-World Tasks, Challenges (daily/weekly timed). | 001, 005 (FEAT-025..029, 032) | Implemented | 5 drills; 0.75x/1.0x |
| 8 | `articulation_lab` vs `pronunciation` differentiated ("Articulation & Enunciation Lab" vs "Phonetic & Intonation Lab"). | 004 (ISS-018), 008 (ISS-018B), 012, 017 | Fixed / Verified (016) | — |
| 9 | Delete-confirmation dialogs on session and history deletion. | 004 (ISS-019), 017 | Fixed [chk ✓ `ConfirmDestructiveDialog` in 6 files] | — |
| 10 | `LiveTranscriptPanel` in Speaking, Challenges, Pronunciation, Listening, Grammar and Public Speaking screens. | 055 | Implemented | 6 screens |

### 1.6 Evaluation and scoring (and on-device AI)

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Baseline: Gemini multimodal speech analysis (`speechAnalysisSchema`, 21 fields); short or noisy audio forces `pronunciationAssessable=false`; 60 s timeout; rolling 3:2 score blend. | 002, 006 | Implemented (baseline) [chk ✗ `GeminiCoachService` is now a deprecated stub] | 21 fields; < 4 s rule; 60 s; 3:2 |
| 2 | Nine evaluators (Speaking, Pronunciation, Grammar, Vocabulary, Listening, Conversation, Storytelling, Presentation, Reflection) on **Firebase AI Logic gemini-3.8-flash** with a deterministic fallback. | 022, 025, 030, 056 | Implemented (11-19) [chk ✗ — CONF-002] | 9; 60 s; ≤ 15 MB media |
| 3 | The same nine evaluators **migrated to local** deterministic and multimodal evaluation. | 039, 033, 043 | Implemented [chk ✓ 9 classes, no Firebase] | 9 |
| 4 | Six mandatory feedback sections (whatWentWell, specificMistakes, correctedExamples, explanation, improvementSuggestions, nextPracticeTask) plus `honestyNotice`. | 025, 039, 045 | Implemented | 6 |
| 5 | Objective score and confidence self-rating are kept strictly separate (self-rating never changes the objective score). | 025, 028, 031, 001 | Implemented / Tested | confidence 1..100 |
| 6 | Local pre-analysis: word count; WPM (if ≥ 5 s, clamped to 30–320); regex filler scan. | 025 | Implemented | 8 fillers (025) vs 11 (035) — CONF-015 |
| 7 | Stage 1.2 local stack: `LocalFeedbackEngine` with 7 sub-engines; 11 grammar categories with rule IDs (SVA_001… etc.); 5-layer pronunciation assessment with "Indian English accent tolerance" (/th/→/t/,/d/; /w/↔/v/); no phoneme-level claims. | 044, 045 | Implemented [chk ~: sub-engines exist; SVA_ IDs present] | 7; 11 categories; 5 layers |
| 8 | Audio DSP: RMS, clipping (≥ 32,000), VAD (20 ms frames, RMS > 0.015), YIN pitch 80–400 Hz, pauses > 300 ms, short/long pauses at 300–750 / > 750 ms. | 035, 045, 048 | Implemented | as stated |
| 9 | Video: 1 fps (0.5 fps on low-end), ≤ 640px frames, MediaPipe Face (478 landmarks), Pose Lite (33), Hand (21); yaw thresholds 15°/30°; shoulder delta > 0.05; stability σ 0.015/0.040; **no emotion or psychological inference**. | 036, 041, 047 | Implemented [chk ~: 640px ✓; models **not bundled** — CONF-017] | as stated |
| 10 | Multimodal fusion with modality independence (audio-only, video-only, transcript-only, combined) and cross-modal rules (> 180 WPM with high movement; > 4 fillers with < 2 pauses; RMS < 0.05 with slouch). | 037 | Implemented | as stated |
| 11 | Device tiers BASIC / STANDARD / ADVANCED; concurrency at ≥ 3 GB RAM; serial execution below that. | 041, 045, 047, 048 | Implemented [chk ✓ code thresholds 2048/4096 MB, API 30; concurrency ≥ 3072 MB — CONF-016] | conflicting tier tables |
| 12 | Audio up to 10 minutes (the 30 s cap was removed). | 044, 045, 047 | Implemented [chk ✓ 600 s / 9.6M samples] (035/042 still say 30 s) | 10 min |
| 13 | Earlier fabricated scoring: baseline 78 (grammar), 72 (fluency), 35 floor; duration coerced to ≥ 10/15 s; "~78/100" on empty input. | 052 | Root-caused and fixed | 78/72/35 |
| 14 | Evidence gate: audio must exist, decode, last ≥ 1.5 s, have RMS ≥ 0.008 and ≥ 0.5 s voiced; video only when selected; transcript scoped to the session with ≥ 2 words; nullable dimension scores; no weight redistribution; partial-evaluation label; 12-state `CoachEvaluationState`. | 052, 053, 051 | Implemented / Tested [chk ✓ thresholds at EvaluationEvidenceValidator.kt:42-44; 12 states] | 1.5 s; 0.008; 0.5 s; 12 states; 15 tests |
| 15 | User-facing recovery copy: "Your speaking recording is missing." / "We couldn't detect speech in this recording." / "We couldn't analyze this recording. Your response has not been scored." | 053 | Implemented | — |
| 16 | (Request) Vocabulary-stage pass thresholds: speak word 75, sentences 75, own sentence 70. Configurable; low ASR confidence gives "inconclusive", not a failure. | 059 Phase 8C | Requested | 75 / 75 / 70 |

### 1.7 AI coach, mentor and conversation simulator

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | AI Mentor streaming (Gemini) with profile, last 5 sessions and 3 feedback records; single in-memory answer. | 001, 005 (FEAT-031), 006 | Partially implemented (baseline) | 5 / 3; 4 quick prompts |
| 2 | Multi-turn mentor thread persisted locally in `mentor_history_{uid}.json`, with a "Clear" confirmation. | 012, 015, 017 | Fixed [chk ✓ InteractiveCoachViewModel.kt:159] | — |
| 3 | Conversation simulator: 13 scenarios, voice dictation via `SpeechRecognizer`, TTS partner, end-of-session debrief saved to `conversationSessions`. | 001, 005 (FEAT-030), 006 | Implemented (baseline, Gemini) | 13 scenarios |
| 4 | Guard against switching scenario when there are ≥ 2 turns. | 012, 017 | Fixed | ≥ 2 turns |
| 5 | Conversation turns are 100% deterministic via `ScenarioScriptBank`. | 045 | Implemented [chk ✗ class absent — CONF-052] | — |
| 6 | `DynamicLocalCoachEngine`: session score deltas used for dynamic coaching feedback. | commit `13bffe0` (no doc) | Undocumented | — |
| 7 | Hybrid AI coach (`HybridAiCoachCoordinator` + `FirebaseAiLogicService`): Gemini tiers balanced/fast/deep and Gemini Live voice. | commit `38d52f2` (no doc); 050 | Implemented then **locked off** by `LOCAL_ONLY` [chk ✓ gated] | tiers: gemini-3.8-flash / 3.5-flash-lite / 4-argon |
| 8 | Inline AI Mentor card on the dashboard shows a read-only "Local AI" badge. | 050 | Implemented | — |
| 9 | Future: Gemini Live low-latency voice; posture fields in the schema; weekly AI narrative digest; embedding search. | 006 (Capabilities I–L) | Proposed | — |

### 1.8 Vocabulary

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | SM-2-style spaced repetition (mastery 0..5; intervals of 1 d → 3 d → 3n…21 d); TTS at 0.75x/1.0x; custom words; AI sentence evaluation (`vocabEvalSchema`, 4 fields). | 001, 005 (FEAT-026), 006 (Capability D) | Implemented (baseline) | 0..5; 4 fields |
| 2 | 4 tabs: Words, Flashcards, AI Quiz, Notebook. | 008 | Existing | 4 tabs |
| 3 | Word search, level filters, a "Card X of Y due today" counter, animated flip. | 012, 017 | Fixed | — |
| 4 | Custom words cannot be edited after saving (rules restrict updates). | 004 (ISS-022), 007 | Tech debt (baseline) | — |
| 5 | (Request) 5-stage word lifecycle: Learn → Speak word → Speak sentences → Own sentence → Complete. Offline evaluation with no phoneme claims; attempt history; easier example on repeated failure. | 059 Phase 8 | Requested [chk ~: `WORD_SPOKEN`, `OWN_SENTENCE` and stage fields exist in LearningModels.kt] | 5 stages; thresholds 75/75/70 |
| 6 | (Request) Quiz types (meaning, word, fill-in, synonym, antonym, sentence, context, listening, speaking recall); call it "AI Quiz" only if it really uses local intelligence; flashcards with SRS; notebook with tags, edit, export. | 059 Phase 9 | Requested [chk ~: `LocalVocabularyQuizEngine`, `SpacedRepetitionScheduler` exist] | 9 quiz types |
| 7 | (Request) Vocabulary home with daily, new, in-progress, review, completed and mastered words; daily goal; vocabulary streak. | 059 Phase 8A | Requested | — |

### 1.9 Progress and analytics

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Analytics: strongest and weakest skills, skill bars, recurring mistakes, separate subjective-confidence card. Period chips were non-functional at baseline. | 001, 004 (ISS-018), 005 (FEAT-033) | Partially implemented (baseline) → Fixed (017: Daily 24 h / Weekly 7 d / Monthly 30 d, honest empty state) | 1/7/30 days |
| 2 | `ActivityProgressAnalyticsEngine` snapshot: total completed (out of 150), attempts, minutes, active days in the last 7, consistency streak, weekly attempts, average objective score, average confidence, module stats, earliest-vs-latest skill deltas, bottom-3 improvement areas, recordings, milestones (1/10/25/50/100/150; 3-/7-day streaks; 5+ self-audits). | 028, 031 | Implemented / Tested | as stated |
| 3 | "Zero fabricated metrics". | 028 | Claimed (undermined by 052 — CONF-010) | — |
| 4 | (Request) Streak = consecutive qualifying days after the daily completion requirement; same-day duplicates count once; timezone-safe; longest streak; milestones at 3/7/14/30/60/100. | 059 Phase 4 | Requested [chk ~: `StreakAndCompletionEngine` exists — CONF-046] | 6 milestones |
| 5 | (Request) `LearningProgressManager` with idempotent events (LESSON_STARTED … STREAK_UPDATED), local-first persistence, sync and conflict rules. | 059 Phase 10 | Requested [chk ~: `LearningProgressManager.kt` exists, 919 lines in `4d6b917`] | 11 event types |

### 1.10 Notifications

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | No push notifications, no `POST_NOTIFICATIONS`, no WorkManager, no FCM. | 002, 005 (FEAT-034) | Not implemented (baseline) [chk ✓ still no permission or WorkManager] | — |
| 2 | (Request) Bell icon with unread badge, read/unread, mark-all, delete, categories, timestamps, empty state, preferences. Covers 10 learning and account categories. **No APK/update alerts.** | 059 Phase 5 | Requested [chk ~: in-app `NotificationManagerRepository` and `NotificationDrawer`; 8 `AppAlertCategory` values; models explicitly exclude update alerts] | 10 requested vs 8 implemented categories |
| 3 | Notification decoupled from learning-progress state. | commit `d8e2a05` (no doc) | Undocumented | — |
| 4 | A false "Session Completed" alert fired for unvalidated sessions; now fixed. | 052 | Fixed | — |

### 1.11 YouTube library (@askvinh intelligence)

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Inspect a channel, playlist or video URL; RSS, Data API v3 or HTML scraping; timedtext captions; Gemini extracts 1–3 techniques per video with creator/AI separation; statuses EXTRACTED / NO_CAPTIONS / INACCESSIBLE. | 001, 002, 005 (FEAT-011..014), 006 (Capability A) | Implemented (baseline, Gemini) | 12,000-character transcript; 2,500-character description; 1–3 techniques |
| 2 | Pre-seeded @askvinh knowledge base copied per user (duplicate seeding). | 004 (ISS-011), 005 (FEAT-014) | Implemented / tech debt | ~20+ documents |
| 3 | YouTube lesson extraction replaced by `LocalIntelligenceOrchestrator.extractYouTubeLessons`. | 034, 033 | Implemented (local) | — |
| 4 | Fragile client-side scraping; no shared cache. | 003, 004 (ISS-007) | Risk (open) | — |
| 5 | A scheduled multi-channel sync worker. | 005 (FEAT-015), 003 | Not implemented / Proposed | 6-hour cron (proposed) |

### 1.12 App updates (OTA)

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Firestore `applicationReleases` (stable/beta); SemVer and `minSupportedVersion`; HTTPS download; 64-hex SHA-256 checksum and signing-certificate check; pre-update backup snapshot; interrupted-update recovery; `ACTION_VIEW` install. | 002, 005 (FEAT-035/036), 007 | Partially implemented (baseline) | 64-hex |
| 2 | Backup restore UI and `restoreBackupSnapshot`. | 004 (ISS-017), 017 | Fixed [chk ✓ exists] | — |
| 3 | `INSTALLED` / `ROLLED_BACK` telemetry never emitted. | 004 (ISS-016) | Open (baseline); no later doc | — |
| 4 | One-tap "Check for Updates" automatically downloads and verifies. | 016, 017 | Implemented | — |
| 5 | GitHub Releases checked directly as well as Firestore (`GITHUB_RELEASE_REPO`); 1.0.0 → 1.1.0-beta.1 path. | 020, 021 | Implemented / Pass | versionCode 2 |
| 6 | APK signature-compatibility verification; update-available dialog (mandatory or optional). | commit `08972b7` (no doc) | Undocumented | — |
| 7 | Notification centre must not carry update alerts. | 059 Phase 5 | Requested [chk ✓ excluded in models] | — |

### 1.13 Admin

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | RBAC admin console with 4 tabs (YouTube Sync, Moderation, Releases, Telemetry). Admin status from `/adminUsers/{uid}` with `active == true`; client writes are denied. | 001, 002, 005 (FEAT-037), 007 | Implemented (baseline) | 4 tabs; latest 25 events |
| 2 | Admin queries are not locally gated; permission errors are swallowed. | 004 (ISS-026) | Tech debt (baseline) | — |
| 3 | (Request) Custom claims or trusted authorisation; server-side checks; hide the entry for non-admins; 13 modules (stats, active users, content management, announcements, moderation, health, audit logs, role management); no default access to recordings. If no secure backend exists, document the gap instead. | 059 Phase 7 | Requested [chk ~: claims plus `adminUsers` in SpeakCraftRepository.kt:236-249; rules for `adminAnnouncements`, `adminAuditLogs`, `adminSettings`] | 13 modules |
| 4 | "Robust multi-factor AdminRole observation". | commit `4d6b917` (no doc) | Undocumented | — |

### 1.14 Settings and profile

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Settings (baseline): update channel, backup, admin entry, theme (System/Light/Dark), TTS speed 0.65–1.35x, Keystore-encrypted YouTube API key (AES-256-GCM), clear history, sign out. | 001, 002, 005 (FEAT-006/038), 007 | Implemented; profile editing partial (ISS-022) | 0.65–1.35x |
| 2 | Centralised "AI & Intelligence" settings: localAi, cloudAi (locked off), cloudAiModelTier, liveTranscription (off by default), local ASR, audio DSP, video analysis, language (en-US/GB/AU/IN), engine (AUTO / ANDROID_OFFLINE / LOCAL_WHISPER). Per-screen cloud chips removed. | 050, 051 | Implemented / Tested [chk ✓ no `*_cloud_toggle_chip` remains] | 9 settings; 4 languages; 10 tests |
| 3 | Capability diagnostics (engine, offline availability, microphone, status, models, camera/MediaPipe) with 7 `CapabilityStatus` values. | 054 | Implemented | 7 statuses |
| 4 | Self-service account deletion absent at baseline (only history clearing). | 007 (open question) | Gap (baseline) [chk: `currentUser.delete()` now at SpeakCraftRepository.kt:1318; undocumented] | — |
| 5 | (Request) Profile (picture, name, email, level, goals, member since, streak, time, activities, vocabulary). Settings sections: Account, Learning preferences, Appearance (accent, font scale, animation), AI & Intelligence (no cloud settings), Integrations (real only), Data & Privacy (export, clear selected, delete account with re-authentication), Notifications, About. | 059 Phase 6 | Requested [chk ~: `UserProfileScreen.kt` exists] | 8 sections |

### 1.15 Accessibility

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | A11Y-01..07: semantics on waveform and sparkline; `FlowRow` chips; 48dp targets; reduced motion; font weights; `heading()` semantics. | 014, 001, 008 | Verified defects → Implemented/Proposed (014); Fixed (017) [chk ~: `rememberReducedMotionEnabled` exists] | 7 findings; 48dp |
| 2 | Contrast ratios: 17.8:1, 7.5:1, 13.2:1, 9.3:1, 8.4:1, 8.6:1 (AAA). White on #D97706 is borderline. | 014 | Verified (for the old palette) — invalid for the current palette (CONF-034) | as stated |
| 3 | WCAG 2.1 AA as the target standard. | 014 | Target | 4.5:1 body; 3:1 large |
| 4 | TalkBack sweep still pending on a real device. | 016 | Not done | — |

### 1.16 Theming and design system

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | "Editorial Studio" palette, navy-primary (MidnightNavy #0F172A, WarmIvory #FAF9F6, AmberSpotlight #D97706, AcousticTeal #0D9488). | 001 | Implemented (baseline) | hex as stated |
| 2 | Editorial Studio, amber-primary (#D97706) with `SpeakCraftColors` extended tokens (success, warning, info, hero, camera). | 011, 017 | Fixed / Implemented [chk ✗ code is Violet/Indigo — CONF-033] | hex as stated |
| 3 | "Violet Studio Light / Midnight Indigo Dark"; "Indigo Studio Sidebar". | 022 (passing mention) | Existing [chk ✓ RoyalViolet #6C3DF4; MidnightIndigoBg #100C2A] | — |
| 4 | Fonts: Plus Jakarta Sans, DM Sans, JetBrains Mono. Weight fix (ISS-015). Scale: displayLarge 34sp, headlineMedium 21sp. | 011, 017 (001 says 32/22sp) | Fixed [chk ✓ 34/21sp] | — |
| 5 | Spacing scale xs–xxl = 4/8/12/16/24/32dp; 48dp minimum target; shapes 10/16/24/32dp. | 011, 017 | Implemented [chk ✓ Spacing.kt:19-24] | as stated |
| 6 | Motion tokens: 150 / 260 / 320 / 850 ms; haptics; microcopy rules. | 013 | Guideline | as stated |
| 7 | (Request) Premium, elegant, minimal look with subtle gradients; not overcrowded; strong contrast. | 059 Phase 11 | Requested | — |

### 1.17 Launch animation

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Native Compose overlay above the root; non-blocking; latched in `LaunchExperienceViewModel` (no replay on rotation or background). | 018, 019 | Implemented [chk ✓ classes exist] | — |
| 2 | Cadences: first launch 2900 ms, returning 2100 ms, reduced motion 600 ms; 4 stages; 15-bar equalizer; 36 ticks; medallion 104/124/148dp. | 018, 019, 020 | Implemented / Pass [chk ✓ LaunchAnimationOverlay.kt:71-73] | as stated |
| 3 | Tagline "Speak with Confidence. Communicate with Impact."; eyebrow "VOICE & COMMUNICATION STUDIO". | 018 | Implemented | — |
| 4 | Splash and window background match the app surface (flash-free). | 018, 019 | Implemented [chk ✗ window is Ivory/Navy but the theme is now Violet/Indigo — CONF-035] | — |

### 1.18 Tutorials

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | `FeatureTutorialRegistry` covering 24 features/routes, each with summary, how-to steps, "what improves" and a coach tip. | 016, 017, 056 | Implemented / Verified [chk ✓ 24 `featureKey` values] | 24 |
| 2 | Inline `FeatureGuideStrip` across all 13 screen files; TopAppBar "?" `FeatureTutorialDialog` with search; dashboard "Explore Tutorials for All 24 Features". | 017 | Implemented [chk ✓ `FeatureGuideStrip` referenced in 13 files] | 13 files |
| 3 | First-use `TutorialOverlay` per feature per user (DataStore `speakcraft_tutorial_state`). | 016, 017 | Implemented | — |

### 1.19 Testing

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | Tests pass (self-reported). | 004 (17/17), 056 (30/30), 016 & 017 (41/41), 020 (53/53), 031 (69/69, "56 pre-existing"), 051 (161/161, 24 suites) | Passing (self-reported) [chk: static @Test counts at commits are 17 / 53 / 56 / 69 / 161 — 30 and 41 never existed — CONF-028] | as stated |
| 2 | Firestore emulator rules tests: 4/4 suites. | 029, 031 | Passed | 4 |
| 3 | Offline test matrix covering 10 scenarios; unit-test suites LocalAIFoundation (5), LocalAudioPipeline (3), LocalLanguageAndOrchestrator (4), ZeroCloud (3). | 043 | Verified [chk ✓ test counts match] | 10 scenarios |
| 4 | Gaps: no MockWebServer tests, no AI contract tests, no CameraX tests, no load tests; an instrumented test needs an emulator. | 002, 003, 004 (ISS-025) | Gap (baseline) | — |
| 5 | Manual device checklist (CameraX, microphone, Credential Manager, TalkBack) still pending. | 016, 020, 056 | Not done | — |
| 6 | (Request) Tests for every workflow: welcome, plan, dashboard, streak, notifications, profile and security, vocabulary; low-, mid- and high-end device testing. | 059 Phase 13 | Requested | — |

### 1.20 CI and release

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | No CI workflow and no `firestore.indexes.json`. | 004 (ISS-027), 007 | Gap (baseline) [chk ✗ workflow exists since `7ed7a24`; ✓ `firestore.indexes.json` still absent from repo root] | — |
| 2 | Push to main/master/beta builds the beta channel `1.1.0-beta.<run>`; a `vX.Y.Z` tag builds stable. Uploads to GitHub Releases plus `.sha256`; publishes to Firestore via `publish_firestore_release.mjs`. | 021 | Implemented [chk ~: tags v1.1.0-beta.7…beta.23 exist] | — |
| 3 | Signing falls back to `debug.keystore.base64` from the repository. | 021 | Implemented [chk ✗ uses the `DEBUG_KEYSTORE_BASE64` secret; keystore git-ignored — CONF-038] | — |
| 4 | No unit-test step in CI. | (observation, not claimed in docs) | [chk ✓ only assembleRelease/assembleDebug] | — |
| 5 | Firebase project `gen-lang-client-0651259232`; named Firestore DB `ai-studio-android-speakcra-e1a16d03-…`. | 021, 002, 007, 029 | Configured | — |

### 1.21 Security and privacy

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | App Check: debug provider in debug builds, reCAPTCHA in release. A missing site key fails open (ISS-006). | 002, 004 | Risk (baseline); later docs silent | — |
| 2 | Client-writable aggregate scores and streaks (ISS-008); list validation in rules only checks indices 0..2 (ISS-009). | 004, 007 | Open (baseline) | indices 0..2 |
| 3 | Prompt-injection exposure; no rate limits (ISS-006). | 004, 006 | Open (baseline; moot while cloud is off) | — |
| 4 | Zero cloud AI; `LOCAL_ONLY` policy; network interceptor blocks 6 AI domains. | 032, 034, 043, 044, 045, 050 | Verified [chk ✗ dependency present; interceptor not attached — CONF-001/005] | 6 domains |
| 5 | Media never uploaded; recordings stay in `filesDir`; Firestore holds scores and progress only. | 004 (ISS-004, as a gap), 032, 050 | Implemented | — |
| 6 | Live transcripts stay on-device. | 050 | Guaranteed [chk ~: not on devices without on-device recognition — CONF-011] | API 31+ |
| 7 | Zero-trust rules for activity collections (owner-only, `hasOnly`, immutable fields, numeric bounds). | 029 | Implemented / Deployed | bounds as stated |
| 8 | Keystore AES-256-GCM for the optional YouTube API key. | 002, 005, 007 | Implemented | 256-bit |
| 9 | No facial emotion or personality inference; pitch and loudness are not treated as confidence. | 036, 045 | Policy | — |
| 10 | Model integrity checked by SHA-256 before load. | 040, 045 | Implemented [chk ✗ empty hashes — CONF-012] | — |
| 11 | (Request) Delete account and data with re-authentication; reliable and verifiable deletion; no access to recordings for admins by default. | 059 Phase 6/7 | Requested | — |

### 1.22 Scalability and performance

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | 7-tier capacity projection from 100 to 1M users (DAU at 25%, peak at 10%); breakpoints include unbounded listeners, inline 19 MB payloads, scraping, client-trust scores. | 003 | Estimated (explicitly) | Firestore reads/day: 5k → 450M+; AI requests/day: 250 → 2.5M |
| 2 | Unbounded `.snapshots()` with no `.limit()` (ISS-010). | 003, 004 | Open (baseline) | 10 or 14 or 15–16 listeners (CONF-031) |
| 3 | Proposed: API gateway, Redis, GCS/S3 signed uploads, BullMQ/Cloud Tasks, PostgreSQL (28 tables), multi-region. | 002, 003, 007 | Proposed (out of scope per 059 — CONF-049) | 28 tables; 14 REST APIs |
| 4 | Peak RAM < 120 MB; base APK ~45 MB; storage reduced by 98.3%; memory by 94%. | 044, 047 | Claimed (unverified) | as stated |
| 5 | Latency per stage (resample 142 ms/30 s; DSP 48 ms; Whisper 1.4 s/10 s; MediaPipe 12–15 ms per frame; synthesis 4–18 ms; ~5 s end-to-end on 2 GB devices). | 042, 046, 048 | Claimed (unverified) | as stated |
| 6 | Battery < 1.5% per 10 sessions; +3.2 °C; 30 cycles at ~3.4%. | 048 | Claimed (unverified, "Simulated") | as stated |

### 1.23 Data model and persistence (supporting domain)

| # | Claim | Source | Claimed status | Numbers |
|---|---|---|---|---|
| 1 | 22 Firestore collections (11 user-scoped, 11 shared); rules 587 lines; Room unused; DataStore preferences; local recordings and backups. | 007 | Verified (baseline) [chk ~: now 34 paths, 865 lines] | 22; 587 |
| 2 | +4 activity collections (`activityProgress`, `activityAttempts`, `savedActivities`, `dailyPracticePlans`). | 029, 058 | Implemented / Deployed [chk ✓ in rules and blueprint] | 4 |
| 3 | Undocumented collections: `notifications`, `learningEvents`, `vocabularyProgress`, `quizAttempts`, `learningSnapshots`, `adminAnnouncements`, `adminAuditLogs`, `adminSettings`. | (none) | [chk ✓ firestore.rules:704-786] | 8 |
| 4 | (Request) 18 candidate entities, including UserLearningPreferences, ActivityCompletionEvent, VocabularyAttempt, UserNotification and AdminAuditEvent. Stable IDs, schema versioning, idempotency keys, migrations, indexes, rules. "Do not duplicate existing collections or create parallel progress systems." | 059 Phase 12 | Requested | 18 entities |
| 5 | (Request) "Use Room or the existing local persistence mechanism for offline progress." | 059 Phase 3 | Requested [chk: no Room in code] | — |

---

## 2. Documented product intent

### 2.1 Who the users are (as stated or implied)

- **English learners improving spoken communication**, from Beginner to Advanced. Level taxonomies differ: 4 levels in onboarding and the blueprint (DOC-005, 058), 5 in DOC-059 (adds Elementary), and 3 difficulty values in the activity engine.
- **Aspiring professionals:** "aspiring leaders, engineers, and presenters" (DOC-009). Example goals include interviews, workplace communication, presentations and public speaking (DOC-023 Module 10; DOC-027 paths 9–10; DOC-059 goal list).
- **Budget-device users in developing markets:** "budget and mid-range Android devices (2 GB to 4 GB RAM, Cortex-A53/A55)" (DOC-044); "developing-market Android smartphones" (DOC-047); "low-end Android devices worldwide" (DOC-048). Android Go is explicitly considered.
- **Indian English speakers** as a notable segment: "Indian English accent tolerance" (DOC-045, commit `36e2bf1`), Indian English WER benchmarks (DOC-046), `en-IN` recognition language (DOC-050).
- **Administrators and content moderators** who curate @askvinh content, publish lessons, challenges and releases, and (requested) manage users and announcements (DOC-001, 005, 059 Phase 7).

### 2.2 Goals and value proposition

- Metadata: "AI-powered English communication and speaking coach built from @askvinh YouTube lessons with speech analysis, pronunciation lab, conversation practice, and progress analytics" (DOC-057).
- Information-architecture goal: answer "Where am I? How am I improving? What should I do next?" within 2 seconds on every primary screen (DOC-010).
- Acceptance vision: "SpeakCraft should feel like a complete personalized communication-learning platform", with "one connected learning ecosystem" and no "fake statistics, buttons that do nothing, or progress values that are not backed by actual completed learning activities" (DOC-059).
- Launch tagline: "Speak with Confidence. Communicate with Impact." Eyebrow: "VOICE & COMMUNICATION STUDIO" (DOC-018).

### 2.3 Positioning (DOC-009 and others)

- Differentiators the documents claim:
  1. Creator-grounded pedagogy (@askvinh / Vinh Giang) with transparent separation of creator content and AI supplements.
  2. A 3-channel self-audit (visual, vocal, verbal) as a signature workflow.
  3. "Calm, executive-grade visual clarity", explicitly avoiding "neon 'AI-slop' gradients". The current Violet/Indigo theme may sit uneasily with this.
  4. Transparent, evidence-based scoring, such as withholding pronunciation scores on short or noisy clips.
- Benchmarked against: Orai, Yoodli, Poised (speech telemetry); ELSA Speak, BoldVoice (pronunciation); Duolingo, Babbel (habit); Speak, ChatGPT Voice (roleplay); Calm, Notion (calm studio). Each is "adapted ... without copying proprietary UI or branding".
- **Open positioning question:** the product is heavily branded around a third-party creator. There is an "@askvinh" bottom tab, a default `https://www.youtube.com/@askvinh` source, and "Vinh Giang" frameworks throughout. No document discusses licensing or permission. DOC-059 does not mention @askvinh at all, which may signal a shift to a generic communication curriculum. The product owner should decide.

### 2.4 Learning philosophy

- Frameworks used throughout: 3Vs (verbal, vocal, visual); 5 Vocal Foundations (rate, pause, melody, volume, character); 3-Stage Self-Audit; PREP; CLEAR; CCC; BLUF; FORD; High-Low-Buffalo; VAKS; STAR; 4 Ways of Listening; Power Zone gestures; "Friendly Authority"; 4-Color Communicator (DOC-001, 005, 006, 023, 025, 039).
- Practice-first: record → self-review → feedback → next task. The 6-section feedback ends with a "next practice task" (DOC-025, 039).
- Honesty and evidence: no fabricated quotes or phoneme scores; withhold scores when there is insufficient evidence; partial evaluations are labelled and not comparable (DOC-006, 025, 036, 052, 053, 059).
- Objective performance is kept separate from self-reported confidence (DOC-001, 025, 028, 039).
- Non-manipulative habit design: "honest consecutive-day streak logic ... without manipulative guilt mechanics" (DOC-009); "Do not punish the user with misleading progress resets" (DOC-059).
- Personalisation: weakest-skill priority, goal alignment, anti-repetition, adaptive difficulty, recovery after missed days (DOC-026, 059).
- Non-linear freedom: prerequisites are advisory, and learners may jump into any activity (DOC-027).

### 2.5 Stated non-goals and constraints

| Constraint | Source(s) | Status in code (spot check) |
|---|---|---|
| Android-native Kotlin/Compose only; no React, Electron or web; no unnecessary backend rewrite | DOC-059 (reverses DOC-001–007's desktop/web target) | Android-only ✓ |
| No large language models (on-device) | DOC-044, 045, 047, 049, 059 | No on-device LLM ✓; cloud Gemini integration present but dormant ✗ |
| No paid cloud AI dependencies; no cloud AI settings | DOC-059; DOC-034, 050 (zero-cloud) | `firebase-ai` dependency and cloud settings present (locked off) ✗ |
| Recordings, transcripts and video never sent to cloud AI; media stays on device | DOC-032, 050, 059 | Holds under the current policy flag; live ASR caveat (CONF-011) |
| Core learning, evaluation and feedback must work offline; app usable with 0 MB of downloaded models | DOC-032, 044, 047, 059 | Plausible; vision models not bundled (CONF-017) |
| Firebase Auth and Firestore only for identity and progress sync | DOC-032, 050, 059 | ✓ (plus the dormant AI SDK) |
| minSdk 24 must not be raised | DOC-041, 048 | minSdk 24 ✓ |
| No facial emotion or psychological inference; no personality claims from voice | DOC-036, 045 | Not checked |
| No fake integrations or non-functional buttons; no unbacked statistics | DOC-059 | Not checked (code audit) |
| Admin must not rely on a client-side boolean; no admin credentials in the APK | DOC-059 | Claims plus `adminUsers` doc ✓ (security review pending) |
| Do not create parallel progress systems or duplicate collections | DOC-059 | Two daily-plan engines and three streak implementations ✗ (CONF-045/046) |
| Small, medium and large screens supported | DOC-017, 059 | 3-tier adaptive shell ✓ |

### 2.6 Major requests in the Paste_November_19 prompt (DOC-059)

The prompt was committed in `4d6b917` on 2026-11-19 at about 19:30 (per its filename).

0. **Ground rules.** Inspect before changing. Do not rebuild. Extend MVVM and repositories. Follow 10 critical architecture requirements (see §2.5).
1. **Welcome back.** Personalised animated greeting with avatar, track, day and score. A swipeable learning-plan carousel (HorizontalPager, snap, indicator dots, Start Lesson / Quick Practice buttons) based on the supplied screenshot (`public/assets/aistudio/image_bf3fea26.png`).
2. **Personalised daily plan.** A `UserLearningProfile` (goals, current and target level, weaknesses, duration, schedule, interests, history). Five levels with manual correction. A deterministic `DailyPracticePlanEngine` with adaptive and recovery behaviour. Versioned, stable plans (`DailyPracticePlan`, `DailyPracticeTask`, `LearningGoal`, `SkillProgress`, `ActivityCompletion`).
3. **Real-time dashboard.** Twelve sections; reactive StateFlow; Firestore sync; local offline persistence; updates only on validated, de-duplicated completion events.
4. **Streak and daily completion engine.** An explicit daily requirement; one count per day; timezone-safe; current and longest streak; calendar; milestones at 3/7/14/30/60/100.
5. **Notification centre.** Bell and badge; 10 learning and account categories; read/unread, delete, preferences; offline-created items; cross-device sync; **no software-update notifications**.
6. **Profile and settings.** Profile stats; Account, Learning, Appearance, AI & Intelligence (status, model management, storage, integrity, CPU/RAM, privacy; **no cloud AI settings**), Integrations (real only), Data & Privacy (export, clear selected, delete account with re-authentication and a verifiable cleanup), Notifications, About; sign out.
7. **Admin console.** Custom claims or trusted authorisation, enforced server-side and by rules; 13 modules; role-based; no default access to private recordings; if no secure backend exists, document it rather than build insecure admin.
8. **Vocabulary builder.** A home view; a 5-stage word lifecycle (Learn; Speak word; Speak sentences; Own sentence; Complete); offline evidence-based evaluation; configurable pass scores of 75/75/70; inconclusive results on low ASR confidence; retry behaviour.
9. **Quiz, flashcards and notebook.** Nine quiz types (labelled "AI Quiz" only if it is truly intelligence-backed); SRS flashcards; a notebook with tags, edit and export.
10. **Unified learning progress.** `LearningProgressManager`; idempotent events (11 types); local-first sync; multi-device conflict rules.
11. **UI/UX design system.** Premium and minimal; consistent across modules; accessible; responsive; a defined dashboard hierarchy.
12. **Database.** Inspect before extending; 18 candidate entities; stable IDs, timestamps, schema versions, ownership, offline, sync status, idempotency keys, migrations, indexes, rules; no parallel systems.
13. **Testing.** Per-workflow tests and device-tier testing.
14. **Implementation order** (18 steps) and **required deliverables**: 10 documents under `docs/`, none of which exist (CONF-044). Also a final report covering what was implemented, files, database changes, screens, offline verification, rules, tests passed and failed, remaining work, limitations and screenshots. No such report exists in the repository.

### 2.7 Open questions surfaced by the documents (for RD-00)

These remain unanswered in any document:

1. Should Electron/web clients reuse the Firebase project or a new backend? (DOC-002) — probably moot under DOC-059.
2. What is the retention policy for raw video and transcripts? (DOC-003, 007)
3. What is the per-user AI budget or quota? (DOC-003) — depends on the cloud decision.
4. Should `streakDays` be computed server-side? (DOC-002)
5. Is a self-service "Delete My Account & All Cloud Data" flow required? (DOC-007) — partly answered by DOC-059.
6. Should `articulation_lab` and `pronunciation` be merged or split? (DOC-001) — answered as split in DOC-017.
7. Should desktop and web reach visual parity? (DOC-001) — moot.
8. Does the @askvinh content need a licence or permission? (not raised in any document; see §2.3)
