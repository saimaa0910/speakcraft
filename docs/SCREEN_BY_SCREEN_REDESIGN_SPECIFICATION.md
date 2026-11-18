# SpeakCraft — Screen-by-Screen Redesign Specification

**Revision Date:** 2026-11-18  
**Scope:** All 22 Compose routes + Auth/Onboarding gates. Every screen preserves existing ViewModel integrations, Firestore persistence, and core `testTag`s while resolving UX bottlenecks and verified bugs.

---

## 1. Auth & Progressive 3-Step Onboarding (`AuthAndOnboardingScreens.kt`)

| Area | `Existing implementation` | `Proposed redesign` | Priority |
| :--- | :--- | :--- | :--- |
| **AuthScreen** | Silent `attemptAutoSignIn` with no loading badge while Credential Manager checks saved Google credentials | Add subtle `"Checking Google session..."` status pill during initial check; clean hero stage framing + value bullets + 54dp `google_sign_in_button` | `Medium priority` |
| **OnboardingAssessmentScreen** | Single overwhelming scroll form with 2 text fields, non-wrapping `Row` of 4 chips (`ISS-019`), and 8 sliders | Convert into a **3-Step Guided Wizard** with `StepProgressIndicator` (`Step X of 3`), `BackHandler` to move back between steps, `FlowRow` level chips + suggested presets (`Beginner`/`Intermediate`/`Upper-Intermediate`/`Advanced` auto-calibrate defaults), and optional fine-tune sliders (`Step 3`) with `"Finish & Start 30-Day Plan"` (`complete_onboarding_button`) | `High priority` |

---

## 2. Adaptive Shell & Home Dashboard (`AppNavigation.kt`, `DashboardScreen.kt`)

* **`AuthenticatedAppShell` (`AppNavigation.kt`):**
  * Adaptive `NavigationBar` (`< 720.dp`) / `NavigationRail` (`>= 720.dp`) for the 5 primary routes (`dashboard`, `daily_practice`, `speaking_lab`, `youtube_library`, `ai_mentor`).
  * Dynamic `currentTodayDateKey()` / midnight-safe epoch calculation in `MainCoachViewModel` (`ISS-028`).
  * Compact top app bar displaying clear contextual titles and minimal right-side actions (`Updates` badge + `Settings`, plus `Admin` only when `isAdmin`).
* **`DashboardScreen`:**
  * **Zone 1 (Action-First Hero):** Update banner (conditional) -> Personalized `RecommendationEngine` stage card (`recommended_action_button` + `"Quick Record"` secondary CTA) using `MaterialTheme.speakCraftColors`.
  * **Zone 2 (Today's Momentum):** `X/9` daily routine progress bar + 4 compact `MetricTile`s (Practice Hours vs Goal, Words Learned, Recordings + Delta, Lessons & Field Tasks).
  * **Zone 3 (4 Core Hub Shortcuts):** Quick launch cards for `Learning Paths`, `Vocabulary`, `Conversation`, and `Analytics`—surfacing high-frequency destinations without scrolling past 22 tiles.
  * **Zone 4 (Filtered Performance Trend):** Latest Recording quick card + `Weekly` / `Monthly` trend card that **actually filters `sessions` by timestamp window** (`7d` vs `30d`, fixes `ISS-018`) and shows an honest empty-window note if no clips fall in the selected period.
  * **Zone 5 (Searchable Skill Studio Directory):** Search input (`dashboard_module_search`) + category filter chips (`All`, `Voice & Clarity`, `Speaking & Story`, `Workplace & Influence`, `Tools`) organizing the 15 curriculum modules + utility tools cleanly.

---

## 3. Speaking Studio, Video Review & Challenges (`SpeakingAndChallengesScreens.kt`)

* **Speaking Studio (`SpeakingLabScreen`) — Critical Fixes (`ISS-001`, `ISS-003`, `ISS-005`, `ISS-019`, `ISS-020`):**
  1. **Guided 3-Step Flow Indicator:** `1. Capture Clip` -> `2. 3-Stage Self-Audit` -> `3. AI Diagnostic & Compare`.
  2. **True In-App `.mp4` 3-Stage Self-Audit (`ISS-001`):**
     * **Stage 1: Visual Pass (Muted):** For `.mp4` video clips, renders `VideoReviewPlayerCard` (`AndroidView` wrapping `VideoView` with `MediaPlayer.setVolume(0f, 0f)`), Play/Pause/Replay controls, and Power Zone checklist (`Eye line`, `Open gestures`, `Grounded stance`).
     * **Stage 2: Vocal Pass (Eyes Closed):** Plays audio only (or dims video frame with a calm `"Eyes-Closed Vocal Pass — Listen purely to Rate, Pause, Melody, Volume & Character"` overlay) with live volume toggle.
     * **Stage 3: Full AV + Transcript Review:** Enables sound + visuals together alongside the verbatim AI transcript and `FeedbackReportCard`.
  3. **Memory-Safe Upload Pre-Check (`ISS-005`):** Check `OpenableColumns.SIZE` / `AssetFileDescriptor.length` BEFORE reading bytes (`>19 MB` guard).
  4. **Honest Cloud Sync Recovery (`ISS-003`):** If `saveSpeakingSession` fails after AI analysis succeeds, display `CloudSyncWarningBanner` (`"Analyzed locally, but cloud sync failed"`) with `"Retry Cloud Sync"` button (`retry_cloud_sync_button`) while keeping the analyzed `FeedbackReportCard` visible.
  5. **Destructive Delete Confirmation (`ISS-019`):** Tapping delete on a saved session opens `ConfirmDestructiveDialog` before removing Firestore docs and local media.
* **Challenges (`ChallengesScreen`):**
  * Add category tabs (`ALL`, `DAILY`, `WEEKLY`) in `FlowRow`/chip row, live countdown ring/bar alongside waveform, and Cloud Sync retry banner support on submission.

---

## 4. Specialized Labs, Curriculum & Vocabulary (`LabsAndTrainingScreens.kt`, `LessonAndModulesScreens.kt`, `VocabularyScreen.kt`)

* **Differentiated Articulation Lab (`articulation_lab`) vs Pronunciation Lab (`pronunciation`) (`ISS-018B`):**
  * `articulation_lab` initializes on Articulation-specific categories (`Consonant Clusters`, `Final Consonant Release`, `Word Linking`) with dedicated header `"Articulation & Enunciation Lab"` and crisp jaw/tongue-release coaching cues.
  * `pronunciation` opens `"Phonetic & Intonation Lab"` focusing on IPA minimal pairs, syllable stress, and pitch intonation (`Vowel Pairs`, `Sentence Stress`).
* **Guided 3-Step Drill Loop:** Each Lab (`PronunciationLabScreen`, `VoiceTrainingLabScreen`, `ListeningShadowingScreen`, `GrammarSpeakingScreen`, `PublicSpeakingScreen`) adds numbered step pills (`1. Model` -> `2. Record` -> `3. AI Score`), active-selection visual border highlight, and `FlowRow` category filters.
* **Lesson Detail (`LessonDetailScreen`):** Preserves `@askvinh` attribution badge, YouTube deep link, inline mic capture + AI evaluation, takeaway notes, and `lesson_complete_toggle` + `next_lesson_button`.
* **Vocabulary (`VocabularyScreen`):** Adds instant keyword search in `Words` tab, `FlowRow` level chips, `"Card X of Y due"` counter + visual flip affordance in `Flashcards` tab, and clear feedback cards in `AI Quiz`.

---

## 5. Conversation Simulator & Multi-Turn AI Mentor (`ConversationAndMentorScreens.kt`)

* **Conversation Simulator (`conversation`):**
  * Guard scenario switching with a lightweight confirmation dialog if `turns.size >= 2` so active practice turns aren't erased accidentally.
  * Add `"Partner Speaking"` toggle / replay speaker icon per partner bubble, plus dictation status pill and `"End & Get Coach Check-In"` (`conversation_checkin_button`).
* **Personalized AI Mentor (`ai_mentor` — Fixes `ISS-021`):**
  * Replace single-card overwriting with a **scrollable multi-turn chat thread (`LazyColumn`)** showing learner prompts + Mentor replies + streaming indicator.
  * Persist Mentor thread history locally per user (`mentor_history_{uid}.json` in `filesDir`) so recent coaching advice and follow-up context survive tab switches and app restarts, with a `"Clear Thread"` confirmation option.
  * Pass recent conversation turns into `askMentorStream` so users can ask natural follow-ups (e.g., *"Break down Day 2 of that plan"*).

---

## 6. Analytics, Updates, Settings & Admin (`AnalyticsAndSettingsScreens.kt`, `AppUpdateScreen.kt`, `AdminDashboardScreen.kt`)

* **Analytics (`AnalyticsScreen` — Fixes `ISS-018`):**
  * `Daily` (24h), `Weekly` (7d), and `Monthly` (30d) chips actually filter `sessions` by `createdAt?.toDate()?.time` and recalculate composite trend scores, WPM averages, and recurring mistakes for the active window, displaying an explicit empty-window state when zero recordings fall inside `Daily` / `Weekly`.
* **App Updates & Backups (`AppUpdateScreen` — Fixes `ISS-017`):**
  * Add **"Restore"** button on each `BackupSnapshotInfo` row with `ConfirmDestructiveDialog` that restores `themeMode`, `ttsRate`, and `updateChannel` preferences from the JSON snapshot via `UpdateBackupManager.restoreBackupSnapshot(...)`.
* **Settings (`SettingsScreen` — Fixes `ISS-019`):**
  * Wrap `"Delete Speaking Session History"` (`clear_history_button`) inside `ConfirmDestructiveDialog` explaining that Firestore sessions, feedback, and local audio clips will be permanently deleted.
* **Admin Console (`AdminDashboardScreen`):**
  * Maintain strict Firestore RBAC restriction screen (`"Admin Access Restricted"`), use `FlowRow` where needed, and polish release form validation feedback.
