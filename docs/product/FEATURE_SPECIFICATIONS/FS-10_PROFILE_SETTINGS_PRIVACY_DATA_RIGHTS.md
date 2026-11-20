# FS-10 — Profile, Settings, Privacy & Data Rights

| Field | Value |
|---|---|
| Document ID | FS-10 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Product Manager / Business Analyst |
| Approval | Pending — Product Owner |
| Related | RD-16 CAP-10; PRD §15.10 (PRD-FR-090…097), PRD-FR-014; SRS §6.10 (SRS-FR-SET-001…008), §9, §11, §15, §16; NFR-PRIV-001…007, NFR-A11Y-001…006, NFR-SEC-002; SEC-01 §3, §6.2–§6.5, §7, §9.1–§9.2, §10.2, §12 (SEC-REQ-21…23, 35, 36); ADR-007, ADR-013, ADR-015; ARC-01 §2; ARC-03 §8, §10; DES-02 (SCR-PROF-01, SCR-SET-01…09); DES-05 §12; FS-01, FS-02, FS-09, FS-11, FS-12; decisions D-10, D-16, D-22, D-23, D-24 |

> **Boundaries (FS-00 §1.1).** Sign-in, sign-out, account deletion and "Delete everything on this device" are owned by **FS-01** and only linked from here. Tier detection, offline-pack install and online-transcription consent behaviour are owned by **FS-12**; reminder scheduling by **FS-09**; sync behaviour and status rules by **FS-11**. This FS defines how those controls appear in Settings, plus export, clear history, retention, privacy information, appearance and About. Copy is draft wording (RECOMMENDATION). No test in this document has been executed (GOV-02 DC-4).

---

## 1. Feature ID & purpose

**FS-10 / CAP-10 (pillar F-1).** Give people one honest place to see who they are in the app (Profile), change every choice they made in onboarding (PRD-FR-014), control what happens to their voice, recordings and transcripts, take a complete copy of their data, delete learning history selectively, and read a privacy explanation that is generated from the same constants that drive behaviour — so the screen cannot drift from the truth.

## 2. User problem

| Problem (user's words) | Evidence (current system) | Personas / needs |
|---|---|---|
| "I can't find Settings, and half of it is for developers." | 29 controls, engine diagnostics first, dead "Cloud AI Coach" switch, plaintext API-key field (DES-01 UXF-21); Settings not on primary chrome (UXF-09); toggles with no effect (FND-AI-06) | All; Hoang and Sofia most affected |
| "The app says my transcripts stay on my phone — is that true?" | Transcripts were uploaded while Settings said they stay on the device (FND-SEC-05, ISS-03) | Meera (P2); UN-08 |
| "Let me take my data with me, or delete my history." | No export; deletion unreachable (FND-SEC-03, UXF-52) | Meera, Sofia; UN-08 |
| "My recordings pile up forever." | No retention or clean-up; failed recordings never deleted (FND-AI-14) | All; UN-08 |
| "The profile and onboarding don't agree." | 4 vs 5 levels, mismatched goal and minute options (UXF-17) | All |
| "Reduce motion doesn't do anything." | Read once, ignored by most screens (UXF-45) | Users who need reduced motion |

## 3. Scope

| | R1 | R2 |
|---|---|---|
| **In** | SCR-PROF-01 profile view/edit; SCR-SET-01 hub with the seven sections of SRS-FR-SET-002; SCR-SET-02 learning preferences; SCR-SET-03 reminder settings (behaviour FS-09); SCR-SET-04 speech & privacy presentation (behaviour FS-12) incl. recording retention and the D-24 transcript option; SCR-SET-05 appearance; SCR-SET-06 export (JSON always + D-22 recordings option) and clear history (4 options, local + cloud); SCR-SET-07 entry points to FS-01/FS-11; SCR-SET-08 About + Share diagnostics; SCR-SET-09 privacy information generated from policy constants | Transcript cloud-sync opt-in toggle (D-16; design constraints SEC-01 §7.5, sync in FS-11); notebook (ENT-15), inbox (ENT-16) and quiz (ENT-17) data added to export and clear history when those features ship |
| **Out** | Account actions (FS-01); tier/pack/consent logic (FS-12); reminder scheduling (FS-09); sync engine (FS-11); in-app font-size slider (system font scale only, SRS-FR-SET-007); cloud-AI settings (NG-01, ADR-003); update channel / GitHub repo / app updates (NG-11, ADR-004); YouTube API key (NG-08, ADR-011); tutorial replay (DES-06 §2.4); sharing recordings with other users (NG-06); cloud storage of recordings (NG-12); analytics opt-in (none in R1, SRS-PRIV-004) | — |

## 4. Preconditions

| Workflow | Preconditions |
|---|---|
| Open Profile / Settings | Onboarding complete; a Profile exists (FS-01 §5.1). Reached from the avatar on any primary destination (DES-02 §2.2) |
| Change a setting | None. Works offline and without an account |
| Export | Device storage available; a destination exists (see §5.4 step 5). No network needed; if signed in and online a pull runs first |
| Clear history | None. Cloud deletion for signed-in users is queued and does not need a connection at the time of clearing |
| Share diagnostics | An app that accepts a shared file is installed |
| Privacy information | None |

## 5. Main workflow

### 5.1 Profile (SRS-FR-SET-001)
1. User taps the avatar → SCR-PROF-01. `ProfileRepository` and `ProgressCalculator` emit the profile and statistics from Room Flows.
2. The screen shows initials avatar (RECOMMENDATION: no photo, nothing loaded from the network), display name, level, goal (track), daily minutes, "Member since {month year}" and key statistics. RECOMMENDATION for "key statistics" (SRS does not list them): current and longest streak, minutes speaking, valid attempts, words learned — each derived by `ProgressCalculator` with FS-08 definitions (SRS-FR-PROG-005).
3. **Edit name** opens an inline editor (0–40 characters †, SEC-01 §5.2). Level, goal and daily minutes rows open the **same** editors as SCR-SET-02 (one implementation, RECOMMENDATION). Save writes through `ProfileRepository` in one transaction and, when signed in, adds an ENT-10 outbox row (FS-11).
4. A **Settings** action opens SCR-SET-01.

### 5.2 Settings home (SRS-FR-SET-002)
SCR-SET-01 lists, in this order: **Learning · Reminders · Speech & privacy · Appearance · Data & privacy · Account · About**. Each row shows a one-line current value (e.g. "Speech & privacy — On-device transcription · Recordings kept 30 days"). There is **no** developer or engine-diagnostics row (DES-02 §5); diagnostics are reachable only through About → Share diagnostics (§5.9).

### 5.3 Settings catalogue — every setting, default, range and effect

Store column: ENT-01/ENT-02 are Room entities synced inside `users/{uid}` when signed in (canon R-1; ARC-03 §8 is authoritative for synced fields); `settings` = DataStore, backed up (D-23), not synced; `device_state` = not backed up, not synced (SEC-01 §3.2 #13).

| # | Screen | Setting | Default | Options / range | Effect | Store |
|---|---|---|---|---|---|---|
| 1 | SCR-PROF-01 | Display name | Empty (onboarding does not capture it — FS-02 issue) | 0–40 characters † | Used in the Today greeting (SRS-FR-TODAY-010) and Profile header; empty → greeting without a name | ENT-01 |
| 2 | SCR-SET-02 (also from Profile) | Goal (track) | Onboarding choice (SRS-FR-ONB-002) | Everyday conversation, Workplace communication, Job interviews, Presentations & storytelling, Clear speech | `PlanEngine` regenerates **incomplete** steps only (SRS-FR-TODAY-007, SRS-BR-013); history kept (SRS-BR-025) | ENT-01 |
| 3 | SCR-SET-02 | Level | Onboarding choice; default Intermediate (SRS-FR-ONB-003) | Beginner, Elementary, Intermediate, Upper-intermediate, Advanced (D-17) | As #2 | ENT-01 |
| 4 | SCR-SET-02 | Daily time | 10 min (SRS-FR-ONB-004) | 5 / 10 / 15 min | As #2; session size per FS-03 | ENT-01 |
| 5 | SCR-SET-02 | New words per day | 2 † (SRS-BR-027) | 0–5 | Number of new-word stages offered per day (FS-03, FS-07); 0 = reviews only | ENT-02 |
| 6 | SCR-SET-03 | Daily reminder | Onboarding choice (off after "Not now") | On / Off | Schedules or cancels `ReminderWorker` (SRS-FR-REM-001; behaviour FS-09). Turning on with Android 13+ may request POST_NOTIFICATIONS in context (PRD-FR-083) | `settings` (`reminder_enabled`, device-level, ARC-03 §3.2) |
| 7 | SCR-SET-03 | Reminder time | 19:00 † (SRS-FR-ONB-005) | Any local time; 22:00–07:00 only if chosen explicitly (SRS-BR-019) | Reminder posted around this time (ADR-005) | ENT-01 (`reminderTime`, SEC-01 §3.2 #10) |
| 8 | SCR-SET-03 | Notification permission (read-only) | System state | Allowed / Blocked | Blocked → "Notifications are blocked" + **Open system settings**; never re-prompts (SRS-FR-REM-005) | System |
| 9 | SCR-SET-04 | Speech recognition status (read-only) | Detected tier | T1 / T2 / T3a / T3b + pack state | Status copy from FS-12 §5.3; re-evaluated per SRS-FR-SPCH-003 | `device_state` (tier cache) |
| 10 | SCR-SET-04 | Recognition language | Device locale if one of the four, else en-US (SRS-FR-SPCH-008; fallback detail FS-12 §5.7) | English (US), (UK), (India), (Australia), filtered by support | Recogniser and reference-audio locale (SRS §8); triggers tier re-evaluation | `settings` |
| 11 | SCR-SET-04 | Offline speech pack | Detected | Installed / Download / Downloading / Not available | **Install** (API 33+, where supported) per FS-12 §5.4; row hidden when the platform cannot report it | Platform |
| 12 | SCR-SET-04 | Online transcription | **Off**; never pre-selected (SEC-01 §7.3 #8) | Off / On — row shown only on devices eligible for T3a (SRS-FR-SET-003, FS-12 §5.1) | Off→On opens the FS-12 consent disclosure; On→Off takes effect before the next attempt, tier → T3b (SRS-FR-SPCH-006, SRS-BR-023) | `device_state` (consent record) |
| 13 | SCR-SET-04 | Keep recordings | **30 days** † (SRS-BR-022, D-10, ADR-015) | Delete after feedback / 7 days / 30 days / Keep until I delete | See §5.6. Helper text when the tier keeps no recordings (T2/T3a): "On this phone, recordings aren't kept while speech is turned into text." | ENT-02 (`recordingRetention`) |
| 14 | SCR-SET-04 | Delete transcripts when recordings expire (D-24) | **Off** — transcripts kept with their attempt until deleted (D-24, SEC-01 SD-03) | Off / On | See §5.7. Disabled with explanation when #13 = Keep until I delete | ENT-02 (`delete_transcripts_with_recordings`, not synced, ARC-03 §3.3) |
| 15 | SCR-SET-05 | Theme | System (SRS-FR-SET-007) | Light / Dark / System | Applied immediately to all screens; System follows the OS setting | `settings` |
| 16 | SCR-SET-05 | Reduce motion | Off in-app; **effective** value = in-app On **or** system animator scale 0 (NFR-A11Y-005) | On / Off | Removes non-essential animation everywhere, reacting live (A11Y-21). When the system setting forces it, the switch shows On, disabled, "Turned on in your phone's settings" | `settings` |
| 17 | SCR-SET-05 | Text size (information row) | System font scale | — (no in-app control, SRS-FR-SET-007) | Row text "Text size follows your phone's font size" + **Open display settings** | System |
| 18 | SCR-SET-06 | Include my recordings (ZIP) | **Off**, chosen per export, not remembered (D-22 consolidated recommendation) | Off / On — shown only if D-22 approves the option | See §5.4 | Not stored |
| 19 | SCR-SET-06 | Keep recordings (summary row) | — | — | Read-only value; opens #13 on SCR-SET-04 (placement issue, §19 #4) | — |
| R2 | SCR-SET-06 | Back up transcripts to my account (D-16) | Off | Off / On | Defined by FS-11 and SEC-01 §7.5 when R2 is approved | ENT-02 |

SCR-SET-07 holds no settings of its own: it shows account state and sync status (FS-11 §5.5) and the FS-01 actions (Sign in / Sign out / Delete account / Delete everything on this device). SCR-SET-08 holds information and actions only (§5.9).

### 5.4 Export my data (SRS-FR-SET-004; D-22; SEC-01 §9.1)
1. SCR-SET-06 → **Export my data** opens the export sheet. "What's included" and "Not included" lists are generated from the same policy constants as SCR-SET-09 (§5.8). A notice states: "This file includes the text of what you said (your transcripts). Keep it somewhere safe." (SEC-01 §7.4).
2. If D-22 approves the option, a switch **Include my recordings (ZIP)** is shown, **off**. Turning it on shows: "Recordings contain your voice. Anyone with this file can listen to them. Only save or share it where you trust." plus "{n} recordings, about {size} MB". Alternative (SEC-01 SD-04 a): the switch is absent in R1 and audio is listed under "Not included".
3. User taps **Export**. If signed in and online, `SyncRepository` runs a pull first (SEC-01 §9.1; timeout 10 s †, RECOMMENDATION) and the file records "Includes data synced up to {time}". Offline or on timeout: "Includes the data on this phone. Changes from your other devices after {last backup time} aren't included."
4. The data layer reads Room (`learning.db`), transcripts (`sensitive.db`) and preferences off the main thread (NFR-PERF-012) and writes **schema v1 JSON** — `format: "speakcraft-export"`, `formatVersion: 1`, ISO-8601 UTC times. **ARC-03 §10 is authoritative for field names**; the SEC-01 §9.1 sample is illustrative. With the recordings option on, the output is one ZIP (RECOMMENDATION for ARC-03 §10): `export.json` (identical to the JSON-only file) + `recordings/{attemptId}.{m4a|wav}`; each attempt carries `recordingOnDevice` and, when included, the in-ZIP path.
5. Delivery — **open difference** logged in SEC-01 §9.1 for RD-99: (S) system share sheet with a FileProvider URI from app-private `exports/`, deleted after hand-off (SRS-FR-SET-004, SRS §8); or (F) Storage Access Framework `ACTION_CREATE_DOCUMENT`, written directly to the chosen location (SEC-REQ-22, SEC-REQ-29). Default file name `speakcraft-export-{YYYY-MM-DD}.json|.zip` (RECOMMENDATION). Acceptance criteria below are delivery-neutral.
6. Success: "Export saved." (F) or the share sheet closes (S). The app keeps no copy.

**Export content map (SRS-PRIV-003, NFR-PRIV-004)**

| Data (SRS §9 / canon R-1) | Included | Note |
|---|---|---|
| Profile ENT-01; learning preferences ENT-02 | Yes | — |
| Device/UI settings (theme, reduce motion, recognition language) | Yes (RECOMMENDATION) | SRS §9 groups UI prefs with profile & preferences |
| Attempts ENT-03 + metrics ENT-04 + feedback (codes and rendered text) | Yes | Measured/Estimated labels kept |
| Transcripts (`sensitive.db`) | Yes | Export-time notice (step 1) |
| "Was this accurate?" ratings ENT-13 | Yes (RECOMMENDATION) | Not named in SRS-FR-SET-004 / SEC-01 §9.1 — §19 #5 |
| Session plans ENT-05/06; word progress ENT-07; reviews ENT-08; events ENT-09 | Yes | — |
| Saved activities ENT-12 | Yes (RECOMMENDATION) | §19 #5 |
| Recordings (ENT-11 files) | Only with the D-22 option | Otherwise listed in `notIncluded` |
| Diagnostics log | No | Shared separately (§5.9); §19 #6 |
| `deviceId`, tokens, consent record, outbox, sync state, deletion state | No | Operational data (SEC-01 §9.1) |
| Notebook ENT-15 | Yes, when rows exist | The table ships in v1 and holds legacy custom words from the D-19 import (ARC-03 §3.16); UI in R2 |
| R2: inbox ENT-16, quiz ENT-17 | When shipped | — |

### 5.5 Clear history (SRS-FR-SET-005; SEC-01 §6.4, §9.2; SEC-REQ-23)
1. SCR-SET-06 → **Clear history** lists four options with their consequences. The user picks one → confirmation dialog stating scope, consequences and "This can't be undone."
2. On confirm, local deletion runs off the main thread: `learning.db` rows in one transaction, then `sensitive.db`, then files. RECOMMENDATION: a persisted "clear in progress" flag makes an interrupted clear resume on next start (two databases cannot share one transaction — §19 #7).
3. Signed in: tombstone jobs are queued in ENT-10; `SyncWorker` deletes the matching cloud documents with paginated queries and then writes the marker in `users/{uid}/meta/sync` (FS-11 §5.4). Outbox rows for deleted items are removed so they are never uploaded. Other devices purge on their next pull.
4. Derived numbers (streak, practice time, trends, word counts) recompute from what remains (SRS-FR-PROG-005).

| Option | Deleted on this phone | Deleted in the cloud (signed in) | Kept | Confirmation copy (draft) |
|---|---|---|---|---|
| **Recordings only** | All files in `noBackupFilesDir/recordings/<profileId>/` and their ENT-11 rows | Nothing (recordings are never uploaded, ADR-015) | Attempts, metrics, transcripts, words, streak | "Delete all recordings? Your attempts, transcripts and progress stay, but you won't be able to replay past attempts." |
| **Attempts and transcripts** | ENT-03, ENT-04, ENT-13, transcripts, recordings + ENT-11, plans ENT-05/06, `SESSION_COMPLETED` events (SEC-01 §6.4) | `attempts/*`, `plans/*`, `SESSION_COMPLETED` events; marker `historyClearedAt` | Word progress and word events, profile, preferences | "Delete all attempts and transcripts? Your streak, practice time and trends will start again from zero. Word progress stays." Today's plan is regenerated with no completed steps (RECOMMENDATION) |
| **Word progress** | ENT-07, ENT-08, `WORD_STAGE_PASSED` / `WORD_REVIEWED` events | `words/*`, word events; marker `wordsResetAt` | Attempts (incl. word-stage attempts), transcripts, recordings | "Reset all words? Every word goes back to New and scheduled reviews are removed. Your attempts stay." |
| **Everything** | Union of the three rows + saved activities ENT-12 and notebook entries ENT-15 (RECOMMENDATION; ARC-03 OQ-15) | Union; both markers | **Profile, preferences, account and sign-in** (distinct from FS-01 "Delete everything on this device", FS-01 Issue 6) | "Delete all learning history? This removes attempts, transcripts, recordings, plans and word progress. Your profile and settings stay." Requires ticking "I understand this can't be undone" (RECOMMENDATION) |

### 5.6 Recording retention (SRS-BR-022, D-10, ADR-015, SEC-01 §7.1)
| Option | Behaviour |
|---|---|
| Delete after feedback | The recording is deleted as soon as the evaluated attempt is stored; no playback is offered (SRS-FR-SESS-009) |
| 7 days / 30 days † | `expiresAt` = attempt time + period; `CleanupWorker` (daily) and an app-start sweep delete within 24 h of expiry (SEC-REQ-19, SRS-FR-DATA-009) |
| Keep until I delete | No expiry; selecting it shows "Recordings take space on your phone. You can delete them any time in Data & privacy." |
| Always | Discarded, interrupted and Inconclusive recordings are deleted immediately (SRS-BR-022) |

Changing the option: **shorter** applies retroactively — a one-off sweep runs immediately and a snackbar says "{n} older recordings were deleted"; **longer** applies to new recordings only (SEC-01 §7.1; interpretation — §19 #8).

### 5.7 Transcript retention option (D-24)
Default Off: transcripts stay with their attempt until the attempt is deleted (SEC-01 §3.2 #2). When On, `CleanupWorker` deletes the transcript of each attempt at the time its recording expires; for attempts without a recording (T2/T3a) the same period is applied from the attempt time (RECOMMENDATION). Attempt detail then shows "Transcript deleted" (FS-08). Turning it on shows: "Older transcripts will be deleted with your recordings. Metrics and feedback stay."

### 5.8 Privacy information (SRS-FR-SET-006, SRS-PRIV-002, NFR-PRIV-007, SEC-REQ-36)
SCR-SET-09 is reached from SCR-SET-04 and SCR-SET-06. **Content rules:** every statement is a string resource template filled from a policy constant (one reviewed "privacy facts" source, SEC-01 §12 SEC-REQ-36); no claim is hard-coded; a snapshot test compares rendered text with the constants (TC-SET-023, TC-SEC-044); wording follows DES-05 §12 (plain, precise; no "100 % private", "military-grade" or "AI").

| Section | Content (draft) | Generated from |
|---|---|---|
| On this phone | Profile, preferences, attempts and metrics, transcripts, recordings ("deleted after {retention}"), words, plans | Local data categories constant + current retention |
| Backed up to your account | Signed in: the synced categories. Local/signed out: "You're not signed in, so nothing is uploaded." | Sync allow-list used by the upload mappers (ARC-03 §8) + auth state |
| Never leaves this phone | Recordings, transcripts (R1), file paths, your speech-service consent choice | Never-uploaded constant (SEC-01 §3.3) |
| Speech recognition | Current tier sentence, e.g. T3a: "While you record, your voice is sent to {service} to turn it into text." | `SpeechCapabilityService` + FS-12 §5.3 copy |
| Phone backup | "Android backup can include your progress and settings. Recordings and transcripts are never included." | Backup policy constant mirrored in backup XML (D-23; TC-SEC-012) |
| Analytics | "SpeakCraft has no analytics and no ads." | SRS-PRIV-004 constant |
| Your choices | Links: Keep recordings, Online transcription, Export, Clear history, Account | Static navigation |
| Privacy policy | Version, effective date, **Read the full privacy policy** | Policy metadata (DEP-06, D-30) |

### 5.9 About and diagnostics (SRS-FR-SET-008, ADR-007, SEC-01 §10.2)
1. SCR-SET-08 shows version name and code, **Open-source licences**, **Privacy policy**, **Terms**, **Help & contact**, **Share diagnostics**.
2. Privacy policy and terms open the hosted page (D-30); RECOMMENDATION: the text shipped with this version is bundled so it can be read offline.
3. **Share diagnostics** opens a preview listing exactly what will be shared: app version, Android version, device model, time-stamped log codes from the PII-free rolling log (SEC-01 §10.2). Text: "This shows exactly what you'll share. It contains no recordings, transcripts, names or email." Actions: **Share** (system share sheet, diagnostics-only FileProvider path, SEC-01 §6.1) / Cancel. Nothing is sent automatically.
4. Developer diagnostics (engine state, tier internals) are **not** in the Settings list. RECOMMENDATION: a developer screen exists only in debug builds (debug source set, compile-time flag ARC-02 §9) and is absent from release builds (SRS-FR-PLAT-003).

## 6. Alternate workflows

| ID | Trigger | Behaviour |
|---|---|---|
| A1 | Level, track or minutes changed mid-day | Only incomplete steps of today's plan change; completed steps and history unchanged (SRS-BR-013, -025) |
| A2 | Export with no attempts yet | File contains profile and preferences with empty lists |
| A3 | Large export (e.g. 3,000 attempts, NFR-PERF-008 data set) | Progress indicator; Cancel deletes any partial file; UI stays responsive |
| A4 | Destination picker or share sheet cancelled | No file kept; temporary files deleted |
| A5 | Clear history while signed in and offline | Local deletion immediate; SCR-SET-06 shows "Your cloud copy will be deleted when you're online"; cloud deletion runs on reconnect |
| A6 | Retention shortened | Immediate sweep; count snackbar (§5.6) |
| A7 | Device restored from backup (D-23) | Attempts show "Recording and transcript not available on this device" (SEC-01 §6.5); export sets `recordingOnDevice: false` and omits the transcript |
| A8 | User turns Online transcription on | FS-12 consent disclosure; switch turns on only after "Use online transcription" |
| A9 | Signed out with kept data | SCR-SET-09 "Backed up" section reads "Not backed up while you're signed out" |
| A10 | Debug build | Developer screen reachable from About (§5.9 step 4) |

## 7. Error scenarios

| ID | Condition | Behaviour / copy (RECOMMENDATION) | SRS-ERR |
|---|---|---|---|
| E1 | Setting or profile write fails | Control reverts to the stored value; "Couldn't save your settings. Try again." | SRS-ERR-005 |
| E2 | Display name longer than 40 characters | Inline error "Use 40 characters or fewer", Save disabled | — |
| E3 | Export write fails (no space, destination error) | "Couldn't save your export. Check there's enough space and try again." + Retry; partial file deleted | SRS-ERR-005 (closest) |
| E4 | Not enough space for the ZIP | Checked before writing using the size estimate: "There isn't enough space for your recordings. Free up {size} MB or export without recordings." | SRS-ERR-004 (closest) |
| E5 | Pull before export fails or times out | Export continues from local data with the step-3 note | SRS-ERR-006 |
| E6 | Clear-history local transaction fails | Rolled back; "Couldn't delete your history. Nothing was changed. Try again." | SRS-ERR-005 |
| E7 | Cloud deletion after clear fails | Outbox kept with back-off; sync status "Error — Retry" (FS-11); local data already cleared | SRS-ERR-006 |
| E8 | Retention sweep cannot delete a file | Retried on next run; PII-free diagnostic code; still within SEC-REQ-19 24 h target | SRS-ERR-005 |
| E9 | Hosted policy page unreachable | Bundled copy shown with "You're offline — showing the version included in this app" | — |
| E10 | No app can receive the diagnostics file | "No app on this phone can receive the file." | — |

## 8. Business rules

| Rule | Application in this feature |
|---|---|
| SRS-BR-022 | Retention default 30 days †; four options; discarded/interrupted/Inconclusive recordings deleted immediately |
| SRS-BR-023 | Online transcription only with T3a + consent; revocation immediate — the SCR-SET-04 switch follows this |
| SRS-BR-025 | Changing level or track never resets progress or history |
| SRS-BR-013 | Preference changes regenerate only incomplete steps |
| SRS-BR-027 | New-word goal default 2 †, range 0–5 |
| SRS-BR-010, -011, -018 | Streak and practice time derive from valid attempts, so clearing attempts resets them (stated in the confirmation) |
| SRS-BR-020 | Tombstones and re-uploads are idempotent; cleared items are not re-uploaded |
| SRS-BR-019 | Reminder quiet hours (behaviour FS-09) |
| SRS-PRIV-002 / -003 / -005 | Privacy text generated from constants; export and deletion cover all categories; recordings never backed up |
| RECOMMENDATION | One editor per preference shared by Profile and SCR-SET-02; resumable clear; ZIP layout; profile statistics list; bundled policy text; debug-only developer screen |

## 9. UI states

**SCR-PROF-01 Profile**

| State | What the user sees | Actions |
|---|---|---|
| Default | Avatar initials, name (or "Add your name"), level, goal, daily time, member since, statistics | Edit name; rows open editors; Settings |
| Loading | Skeleton rows | — |
| Empty | No attempts: statistics show "0" with "Your first attempt will appear here" (no fabricated values, PRD-NFR-001) | Go to Today |
| Error | "Couldn't load your profile." | Retry |
| Offline | N/A — local data only | — |
| Permission | N/A | — |
| Editing name | Text field with counter "{n}/40", E2 error | Save; Cancel |

**SCR-SET-01 Settings home** — Default: seven rows with current values; Loading: skeleton rows; Empty: N/A (static list); Error: a row whose value cannot be read shows "—" and still opens; Offline: N/A (all local); Permission: N/A.

**SCR-SET-02 Learning**

| State | What the user sees | Actions |
|---|---|---|
| Default | Goal, Level, Daily time, New words per day with current values | Open editor; change |
| Loading / Empty | Skeleton / N/A (always has values) | — |
| Error | E1 snackbar; value reverted | Retry |
| Offline / Permission | N/A — local | — |
| Changed mid-day | Snackbar "Today's remaining steps were updated." | — |

**SCR-SET-03 Reminders** (behaviour FS-09) — Default: switch, time, permission row; Permission: "Notifications are blocked" + Open system settings (SRS-FR-REM-005); Error: E1; Loading: skeleton; Empty/Offline: N/A.

**SCR-SET-04 Speech & privacy**

| State | What the user sees | Actions |
|---|---|---|
| Default | Status block (FS-12 §5.3), Recognition language, Offline speech pack (if supported), Online transcription (if T3a-eligible), Keep recordings, Delete transcripts when recordings expire, Privacy information link | Change any control |
| Loading | "Checking speech recognition…" in the status block; other rows usable | — |
| Empty | N/A | — |
| Error | Status "We couldn't check speech recognition yet" (FS-12 E-states); E1 for writes | Retry check |
| Offline | Pack **Install** disabled "Needs a connection"; T3a status adds "Needs a connection to work" | Other controls |
| Permission | Microphone not granted: note "Microphone access is off — speaking practice is paused" + Open settings (status still shown, FS-12 A8) | Open settings |
| Pack downloading | Progress row (FS-12 §5.4) | Keep using app |
| Consent sheet open | FS-12 disclosure over the screen | Use online transcription / Use audio-only mode |

**SCR-SET-05 Appearance** — Default: Theme radio group, Reduce motion switch, Text size row; Reduce motion forced by system: switch On + disabled + explanation; Loading/Empty/Error/Offline/Permission: N/A or E1 on write failure.

**SCR-SET-06 Data & privacy**

| State | What the user sees | Actions |
|---|---|---|
| Default | Export my data, Clear history, Keep recordings (summary), Privacy information | Open |
| Loading | N/A — actions only | — |
| Empty | No history: Clear history options show "Nothing to delete" and are disabled; export still available (A2) | Export |
| Error | E3/E4/E6/E7 messages | Retry |
| Offline | Export works; signed-in note (§5.4 step 3); pending cloud deletion line (A5) | — |
| Permission | N/A (SAF/share sheet need no permission) | — |
| Exporting | Progress "Preparing your export… {n}%" (polite live region) | Cancel |
| Clear confirmation | Dialog per option (§5.5 table) | Delete …; Cancel |
| Clearing | Progress indicator; Back disabled until local deletion ends | — |

**SCR-SET-07 Account** — states owned by FS-01 §9 and the sync-status block by FS-11 §9.

**SCR-SET-08 About** — Default: version, licences, policy, terms, help, Share diagnostics; Offline: E9 for policy/terms; Diagnostics preview: content list + Share/Cancel; Error: E10; Loading/Empty/Permission: N/A.

**SCR-SET-09 Privacy information**

| State | What the user sees | Actions |
|---|---|---|
| Default | Sections of §5.8 filled for the current auth state, tier and retention | Links in "Your choices" |
| Loading | Skeleton until tier and auth state resolve (≤ 1 frame typical) | — |
| Empty / Error | N/A — generated from constants; if the tier is unknown, the speech section says "We're still checking speech recognition on this phone." | — |
| Offline | Same content; policy link uses the bundled copy (E9) | — |
| Permission | N/A | — |

## 10. Data requirements

| Entity / store | Use in FS-10 (minimum; ARC-03 authoritative) |
|---|---|
| ENT-01 Profile | `displayName?`, `level`, `goal`, `dailyMinutes`, `reminderTime`, `memberSince`, `authState` (display) |
| ENT-02 LearningPreferences | `dailyWordGoal`, `recordingRetention`, `delete_transcripts_with_recordings` (D-24, not synced); R2 transcript-sync opt-in (ARC-03 §3.3) |
| ENT-03 Attempt, ENT-04 AttemptMetric, ENT-13 FeedbackRating | Export; Clear history (attempts and transcripts, everything) |
| ENT-05 SessionPlan, ENT-06 SessionStep | Export; cleared with attempts; today's plan regenerated |
| ENT-07 WordProgress, ENT-08 WordReview, ENT-09 LearningEvent | Export; word reset; event types decide which option deletes them |
| ENT-10 SyncOutbox | Tombstone jobs for cloud deletion; removal of rows for cleared items |
| ENT-11 RecordingFile | `attemptId`, `path`, `sizeBytes`, `expiresAt` — retention, ZIP, recordings-only clear |
| ENT-14 SyncState | Last successful pull time for the export note |
| `sensitive.db` | Transcript text by `attemptId` (export, clear, D-24 expiry) |
| `settings` DataStore | Theme, reduce motion, recognition language, reminder on/off |
| ENT-12 SavedActivity, ENT-15 NotebookEntry | Export; cleared by "Everything" (RECOMMENDATION) |
| `device_state` | Consent record and tier cache (read for display; never exported) |
| Cloud | `users/{uid}` fields and `users/{uid}/meta/sync` markers `historyClearedAt`, `wordsResetAt`; synced field list: ARC-03 §8. Export schema: ARC-03 §10 |

## 11. API / repository dependencies

| Dependency | Used for |
|---|---|
| `ProfileRepository` | Profile read/edit; member since |
| `PreferencesRepository` | Fronts ENT-02 and the `settings` DataStore (canon R-1) |
| `ProgressCalculator` | Profile statistics; recompute after clear |
| `AttemptRepository`, `PlanRepository`, `WordRepository` | Export reads; clear-history deletes; plan regeneration through `PlanEngine` |
| `SyncRepository` / `SyncEngine` + `SyncWorker` | Pull before export; tombstones and markers (FS-11) |
| `SpeechCapabilityService` | Status, pack state, T3a eligibility for SCR-SET-04 and SCR-SET-09 (FS-12) |
| `CleanupWorker` | Retention and D-24 expiry; immediate sweep after a shorter retention |
| `AccountRepository` | Auth state for SCR-SET-07 and SCR-SET-09 |
| `AppLogger` | Diagnostic log source for Share diagnostics (ADR-007) |
| Export / clear orchestration | Data-layer component named in ARC-02; interface in ARC-04 §5 |
| Platform | Storage Access Framework or share sheet (§5.4 step 5), `java.util.zip`, `PackageManager` (version), `Settings` intents (notifications, display), FileProvider diagnostics path |

## 12. Offline behaviour

Every control on SCR-PROF-01 and SCR-SET-01…09 works offline (PRD-FR-100). Deferred while offline: the pull before export (export uses local data and says so), cloud deletion after Clear history (queued, A5), the hosted policy page (bundled copy, E9). Owned elsewhere: offline-pack download and online transcription (FS-12), sign-in (FS-01).

## 13. Privacy requirements

- Privacy information and the export "included / not included" lists are generated from policy constants and snapshot-tested (SRS-PRIV-002, NFR-PRIV-007, SEC-REQ-36).
- Export covers every data category (SRS-PRIV-003, NFR-PRIV-004) except those listed in `notIncluded`; it contains transcripts and says so; with the D-22 option it contains voice recordings and warns first. The app keeps no copy after hand-off.
- Each Clear history option deletes exactly its stated categories locally and in the cloud (NFR-PRIV-005, SEC-REQ-23).
- Retention is enforced within 24 h (SEC-REQ-19); recordings are never backed up (SRS-PRIV-005, NFR-PRIV-006, D-23).
- Diagnostics are PII-free, previewed and shared only by the user (SEC-REQ-35, NFR-SEC-002).
- No analytics (SRS-PRIV-004, ADR-013); recordings never shared with other users or stored in the cloud (NG-06, NG-12).

## 14. Accessibility notes

Section titles are headings (NFR-A11Y-001); switch rows merge label and state (A11Y-07, fixes UXF-43); radio groups for theme and retention expose state descriptions (A11Y-06); destructive buttons name the action ("Delete all recordings"); export and clear progress are polite live regions (A11Y-08); errors are announced and say how to fix them (A11Y-32); dialogs use real Dialogs with focus containment (A11Y-10); 200 % font scale with no clipped text (NFR-A11Y-002, A11Y-13); 48 dp targets (NFR-A11Y-004, A11Y-19); Reduce motion itself follows A11Y-21. Cross-cutting checks: TC-A11Y-007, -008, -010, -023, -024.

## 15. Acceptance criteria

| ID | Given | When | Then |
|---|---|---|---|
| AC-FS10-01 | A user with 4 valid attempts | They open Profile and set the name "Sam" | Profile shows level, goal, daily time, member since and statistics derived from the attempts, and the next Today greeting uses "Sam" |
| AC-FS10-02 | Any user | They open Settings | Exactly seven sections appear in the order Learning, Reminders, Speech & privacy, Appearance, Data & privacy, Account, About, and no developer or engine-diagnostics entry is shown |
| AC-FS10-03 | Today's plan has 1 of 3 steps complete | The user changes daily time from 10 to 15 minutes | The completed step is unchanged, only incomplete steps are regenerated, and no history is removed |
| AC-FS10-04 | SCR-SET-02 is open | The user sets New words per day | Values 0–5 are accepted, the default is 2, and no other value can be saved |
| AC-FS10-05 | Theme is System | The user selects Dark and restarts the app | Every screen uses the dark theme immediately and after restart |
| AC-FS10-06 | The phone's animator scale is 0 | The user opens Appearance | Reduce motion shows On, is disabled with the system explanation, and no non-essential animation runs |
| AC-FS10-07 | A local user in airplane mode with attempts, transcripts, words, plans, events and ratings | They export their data | A schema-v1 JSON file is produced that contains every category in the §5.4 content map marked "Yes", includes transcripts, and contains no `deviceId`, token or audio |
| AC-FS10-08 | D-22 approves the recordings option | The user opens the export sheet | "Include my recordings (ZIP)" is off; turning it on shows the voice warning and size; exporting produces a ZIP with `export.json` and one file per retained recording |
| AC-FS10-09 | A signed-in user online | They export | A pull runs first and the file states the time data was synced up to; if the pull fails the export still completes with the local-data note |
| AC-FS10-10 | 12 retained recordings | The user clears "Recordings only" | All 12 files and their ENT-11 rows are deleted, attempts and transcripts remain, and replay is no longer offered |
| AC-FS10-11 | A signed-in user online | They clear "Attempts and transcripts" | Local attempts, metrics, ratings, transcripts, recordings, plans and session events are deleted; the matching cloud documents are deleted; `meta/sync.historyClearedAt` is set; word progress is unchanged |
| AC-FS10-12 | Words in several stages | The user clears "Word progress" | Every word is New, reviews and word events are deleted locally and in the cloud, `wordsResetAt` is set, and attempts remain |
| AC-FS10-13 | A signed-in user | They clear "Everything" | All learning history is deleted but the profile, preferences and sign-in remain and onboarding does not restart |
| AC-FS10-14 | A clear-history confirmation is open | The user cancels | Nothing is deleted |
| AC-FS10-15 | A signed-in user offline | They clear "Attempts and transcripts" | Local data is deleted at once, the pending-cloud line is shown, and the cloud documents are deleted after connectivity returns without re-uploading the cleared items |
| AC-FS10-16 | Retention 30 days; recordings aged 2, 10 and 20 days | The user selects 7 days | The 10- and 20-day recordings are deleted immediately with a count snackbar and the 2-day recording remains |
| AC-FS10-17 | Retention "Delete after feedback" on a T1 device | An attempt is evaluated | No recording file remains for it and no playback control is shown |
| AC-FS10-18 | D-24 option On, retention 7 days | An attempt passes 7 days of age | Its recording and transcript are deleted within 24 h and its metrics and feedback remain |
| AC-FS10-19 | Each combination of local / signed-in, T1 / T3a and retention 7 / 30 days | The user opens Privacy information | Every statement matches the policy constants and current state, and the snapshot test for that state passes |
| AC-FS10-20 | Any user | They open About → Share diagnostics | Version and links are shown; the preview lists exactly what is shared and contains no uid, email, name, transcript or file path |
| AC-FS10-21 | Devices in each tier | The user opens Speech & privacy | The FS-12 status for the tier is shown; Online transcription appears only on T3a-eligible devices; turning it off makes the next attempt T3b |
| AC-FS10-22 | A preference write fails | The user changes a setting | The control returns to the stored value and "Couldn't save your settings. Try again." is shown |
| AC-FS10-23 | A release build | Any screen is searched | No developer-diagnostics screen or entry exists (RECOMMENDATION §5.9) |

## 16. Test cases

| TC | Title | Type | Preconditions | Steps | Expected result | Traces |
|---|---|---|---|---|---|---|
| TC-SET-001 | Profile statistics and empty state | Robolectric UI | Fake `ProgressCalculator` with 0 and 4 attempts | Render SCR-PROF-01 | Values match the fake; 0 attempts shows the empty copy, no invented numbers | SRS-FR-SET-001 |
| TC-SET-002 | Display-name validation | unit | ViewModel with fake repository | Submit 0, 40, 41 characters | 0 and 40 saved; 41 rejected with E2 | SRS-FR-SET-001 |
| TC-SET-003 | Name reaches the greeting | unit | In-memory Room; Today ViewModel | Save name; collect Today state | Greeting contains the new name | SRS-FR-SET-001, SRS-FR-TODAY-010 |
| TC-SET-004 | Settings home content | Robolectric UI | Default state | Read row labels | Seven labels in SRS order; no diagnostics row | SRS-FR-SET-002 |
| TC-SET-005 | Mid-day preference change | unit | Plan with 1 of 3 steps done | Change minutes/level/track | Completed step identical; only incomplete steps differ; no rows deleted | SRS-FR-TODAY-007, SRS-BR-013, SRS-BR-025 |
| TC-SET-006 | New-word goal bounds | unit | — | Set −1, 0, 2, 5, 6 | 0, 2, 5 stored; −1 and 6 rejected; default 2 | SRS-BR-027 |
| TC-SET-007 | Theme applies and persists | instrumented | Emulator | Select Dark; force-stop; relaunch | Dark immediately and after relaunch; System follows OS toggle | SRS-FR-SET-007 |
| TC-SET-008 | Effective reduce motion | Robolectric UI | Fake system animator scale 0/1; in-app On/Off | Render SCR-SET-05; change system value while open | Effective = in-app OR system; switch disabled when system forces it; reacts live | SRS-FR-SET-007, NFR-A11Y-005 |
| TC-SET-009 | Export golden file | unit | Seeded in-memory DBs | Run export | Output equals the golden JSON for ARC-03 §10 v1; transcripts present; no `deviceId`/tokens/audio | SRS-FR-SET-004, SEC-REQ-22 |
| TC-SET-010 | Export completeness | unit | One row in every exportable table | Run export; compare keys to tables | Every "Yes" category of §5.4 present; `notIncluded` lists the rest | SRS-PRIV-003, NFR-PRIV-004 |
| TC-SET-011 | Export offline, no account | instrumented | Local profile, airplane mode | Export to device storage | File written; no network request | SRS-FR-SET-004, SEC-REQ-01 |
| TC-SET-012 | Recordings option UI | Robolectric UI | D-22 option flag on | Open export sheet; toggle option | Off by default; warning and size shown when on; not remembered next time | SRS-FR-SET-004 (D-22) |
| TC-SET-013 | ZIP layout | unit | 3 attempts, 2 with recordings | Export with option on | ZIP has `export.json` + 2 files named by `attemptId`; JSON paths match | SRS-FR-SET-004 (D-22) |
| TC-SET-014 | Pull before export | unit | Fake `SyncRepository`: success / timeout | Export as signed-in user | Success: synced-up-to time recorded; timeout: local-data note; export completes in both | SRS-FR-SET-004, SRS-ERR-006 |
| TC-SET-015 | Clear recordings only | instrumented | 12 recording files | Clear → Recordings only | 0 files and 0 ENT-11 rows for the profile; attempts and transcripts intact | SRS-FR-SET-005, NFR-PRIV-005 |
| TC-SET-016 | Clear attempts and transcripts — local scope | unit | Seeded `learning.db` + `sensitive.db` | Run option | Deleted exactly per §5.5 row; word rows and word events untouched; outbox rows for cleared items removed | SRS-FR-SET-005, NFR-PRIV-005 |
| TC-SET-017 | Clear history cloud cascade | instrumented | Firestore emulator; signed-in user; second simulated device | Clear on device A; sync B | Cloud docs deleted; marker set; B purges and does not re-upload (see TC-SEC-018) | SRS-FR-SET-005, SEC-REQ-23 |
| TC-SET-018 | Word reset and Everything scopes | unit | Seeded DBs | Run each option | Word reset deletes only word data; Everything deletes all history and saved activities, keeps profile and preferences | SRS-FR-SET-005 |
| TC-SET-019 | Cancel and rollback | unit | Fake DAO failing on the last delete | Cancel dialog; then confirm with failure | Cancel: no change. Failure: full rollback and E6 | SRS-FR-SET-005, SRS-ERR-005 |
| TC-SET-020 | Retention change sweep | unit | Fake clock; recordings aged 2/10/20 days | Change 30 → 7; then 7 → 30 | Shorter: 10/20-day files deleted at once; longer: existing expiry unchanged, new files get 30 days | SRS-BR-022, SEC-REQ-19 |
| TC-SET-021 | Delete after feedback | instrumented | T1 device or emulator with fake recogniser | Record; reach result | No file for the attempt; no playback control | SRS-BR-022, SRS-FR-SESS-009 |
| TC-SET-022 | Transcript expiry option | unit | Fake clock; option On; attempts with and without recordings | Advance past retention; run `CleanupWorker` | Transcripts deleted with recordings and, for T2/T3a attempts, at the same age; metrics kept | D-24 (SEC-01 SD-03) |
| TC-SET-023 | Privacy information matches constants | Robolectric UI | Fakes for auth, tier, retention | Render SCR-SET-09 per state | Snapshot equals text generated from constants for each state (see TC-SEC-044) | SRS-FR-SET-006, SRS-PRIV-002, NFR-PRIV-007 |
| TC-SET-024 | Diagnostics preview is PII-free | unit | Log seeded with email, uid, `users/<uid>`, transcript-tagged text | Build preview | Redacted per SEC-01 §10.1; preview equals shared content (see TC-SEC-040) | SRS-FR-SET-008, SEC-REQ-35 |
| TC-SET-025 | About content and offline policy | Robolectric UI | Offline fake | Open About; open Privacy policy | Version name/code, licences, links shown; bundled policy with offline line | SRS-FR-SET-008 |
| TC-SET-026 | Speech & privacy rows per tier | Robolectric UI | Fake capability: T1, T2, T3a-eligible, T3b-no-recogniser, API 32 | Render SCR-SET-04 | Consent row only when T3a-eligible; pack row only when supported; retention helper text on T2/T3a | SRS-FR-SET-003 |
| TC-SET-027 | Consent off takes effect | unit | Consent granted, T3a | Toggle off; request capability for next attempt | Tier T3b; consent record declined with timestamp | SRS-FR-SPCH-006, SRS-BR-023 |
| TC-SET-028 | Write failure reverts | unit | Fake repository throws on write | Change theme and daily time | UI value reverts; E1 message emitted | SRS-ERR-005 |
| TC-SET-029 | No developer screen in release | manual | Release-signed build | Inspect About and navigation graph; tap version repeatedly | No developer screen or entry reachable | SRS-FR-PLAT-003 |
| TC-SET-030 | Settings accessibility | manual | TalkBack on; font scale 2.0 | Operate SCR-SET-04 and SCR-SET-06 end to end | All controls labelled with state; no clipped text; dialogs trap focus (see TC-A11Y-007, -023) | NFR-A11Y-001, NFR-A11Y-002 |

## 17. Traceability

| PRD-FR | SRS-FR / rule | AC | TC |
|---|---|---|---|
| PRD-FR-090 | SRS-FR-SET-001 | AC-FS10-01 | TC-SET-001, -002, -003 |
| PRD-FR-091 | SRS-FR-SET-002 | AC-FS10-02, -23 | TC-SET-004, -029 |
| PRD-FR-092 | SRS-FR-SET-003; SRS-BR-022, SRS-BR-023 | AC-FS10-16, -17, -18, -21 | TC-SET-020, -021, -022, -026, -027 |
| PRD-FR-093 | SRS-FR-SET-004; SRS-PRIV-003 | AC-FS10-07, -08, -09 | TC-SET-009 … -014 |
| PRD-FR-094 | SRS-FR-SET-005 (local-only delete: SRS-FR-ACC-009, owned by FS-01) | AC-FS10-10 … -15 | TC-SET-015 … -019 |
| PRD-FR-095 | SRS-FR-SET-006; SRS-PRIV-002 | AC-FS10-19 | TC-SET-023 |
| PRD-FR-096 | SRS-FR-SET-007 | AC-FS10-05, -06 | TC-SET-007, -008 |
| PRD-FR-097 | SRS-FR-SET-008 | AC-FS10-20 | TC-SET-024, -025 |
| PRD-FR-014 (CAP-02, realised here) | SRS-FR-SET-001, -002; SRS-BR-013, -025, -027 | AC-FS10-03, -04 | TC-SET-005, -006 |
| PRD-NFR-002 | NFR-PRIV-004 … -007 | AC-FS10-07, -19 | TC-SET-010, -023 |
| PRD-NFR-006 | NFR-A11Y-001 … -006 | AC-FS10-06 | TC-SET-008, -030 |
| — | SRS-ERR-005 | AC-FS10-22 | TC-SET-019, -028 |

## 18. Open questions

| # | Question | Owner |
|---|---|---|
| Q1 | Export delivery: share sheet (SRS) or Storage Access Framework (SEC-01)? (§19 #1) | Product Owner / Security Engineer (RD-99) |
| Q2 | D-22: optional recordings ZIP in R1, or JSON only in R1 and ZIP in R2? | Product Owner |
| Q3 | D-24: confirm the option sits under Keep recordings on SCR-SET-04 and applies the same age to attempts without recordings | Product Owner |
| Q4 | Confirm the Profile statistics list (§5.1) | Product Owner |
| Q5 | Should "Everything" also clear saved activities (recommended) — and should it ever clear learning preferences? | Product Owner |
| Q6 | Should the retention choice sync across devices (SEC-01 §3.2 #12 syncs it) when recordings are per device? | Product Owner / Security Engineer |
| Q7 | Bundle the privacy policy and terms text in the app for offline reading (DEP-06, D-30)? | Product Owner |
| Q8 | Confirmation strength for "Everything": tick box (recommended) or typed word as in FS-01? | Product Owner |

## 19. Issues found in source documents

| # | Issue | Source | Suggested resolution |
|---|---|---|---|
| 1 | Export delivery conflict: share sheet with FileProvider `exports/` (SRS-FR-SET-004, SRS §8, ARC-01 §12, WBS T-14.2) vs Storage Access Framework and "no FileProvider except diagnostics" (SEC-01 §6.1, §9.1, SEC-REQ-22, SEC-REQ-29) | SRS, ARC-01, SEC-01 | Decide in RD-99 (Q1) and amend the losing documents |
| 2 | If D-22 alternative (JSON only) is chosen, SRS-FR-SET-004 ("recordings optionally included as a ZIP") and SRS-PRIV-003 / NFR-PRIV-004 ("100 % of personal data categories", which include recordings) are not met in R1 | SRS, NFR, SEC-01 SD-04 | Record the D-22 outcome in SRS-FR-SET-004 and state any R1 exclusion in SRS-PRIV-003 |
| 3 | The D-24 option "Delete transcripts when recordings expire" exists only in SEC-01 SD-03 / canon; SRS-FR-SET-003 lists no transcript control, SRS §9 says transcripts are kept "until deleted" and SRS-BR-022 covers recordings only | SRS-FR-SET-003, SRS §9, SRS-BR-022 | Add the option to SRS-FR-SET-003 and SRS §9 if D-24 is approved |
| 4 | Retention placement: PRD-FR-092 and SRS-FR-SET-003 put the selector in Speech & privacy; DES-02 §4 lists retention under SCR-SET-06 Data & privacy | DES-02 §4 | Keep the control on SCR-SET-04 and a read-only summary on SCR-SET-06 (as specified here); update DES-02 wording |
| 5 | Export content lists (SRS-FR-SET-004, SEC-01 §9.1) omit "Was this accurate?" ratings (in SRS §9 "Attempts (metrics, feedback, rating)"), UI preferences (in SRS §9 "Profile & learning preferences … DataStore (UI prefs)") and saved activities (ENT-12, absent from SRS §9 altogether) | SRS-FR-SET-004, SRS §9, SEC-01 §9.1 | Add ENT-13, UI preferences and ENT-12 to the export list and ENT-12 to SRS §9 |
| 6 | The diagnostics log is a data category in SRS §9 (so SRS-PRIV-003 implies it is exported) but SEC-01 §9.1 excludes it; its size also differs — "rolling 1 MB †" (SRS §9) vs "2 × 256 KB, 7 days" (SEC-01 §3.2 #18, §10.2) | SRS §9, SEC-01 | Exclude diagnostics from SRS-PRIV-003 scope (shared via About) and align the size |
| 7 | SEC-01 §9.2 requires clear-history local deletion "in one Room transaction", but transcripts live in the separate `sensitive.db` (SEC-01 §3.2 #2); no persisted state makes an interrupted clear resumable (ENT-18 covers account deletion only) | SEC-01 §9.2, §3.2; canon R-1 | ARC-03 to define a resumable clear job (outbox tombstone + local flag) |
| 8 | SEC-01 §7.1 says `expires_at` is "recomputed when the user changes the retention option" and also that changes apply "prospectively when longer" — unclear whether existing recordings are extended | SEC-01 §7.1 | Confirm the interpretation used here (shorter: retroactive; longer: new recordings only) |
| 9 | SRS-FR-SET-005 offers "word progress" as a Clear-history option, while SEC-01 §6.4, §9.2 and SEC-REQ-23 describe a separate "Reset vocabulary" action; saved activities are covered by neither Clear history nor the SEC-01 wipe matrix | SRS-FR-SET-005, SEC-01 §6.4 | Use one name and one place (Clear history → Word progress) and add ENT-12 to the §6.4 matrix |
| 10 | SEC-01 §3.2 #12 stores synced learning preferences in the `settings` DataStore, whereas canon R-1, FS-00 §2.1 and ARC-03 §3.3 make them the Room entity ENT-02. SEC-01 §5.3 `validPrefs` allows `dailyWordGoal` 0–20 (SRS-BR-027 and ARC-03: 0–5). SEC-01 §5.3 and ARC-03 §3.3 both carry a `reviewReminders` preference, but no SRS requirement or setting exists for it and SRS-FR-REM-003 makes the due-review mention mandatory | SEC-01 §3.2, §5.3; ARC-03 §3.3 | Align SEC-01 to ENT-02 and SRS-BR-027; either add a reminder setting to SRS §6.9 (FS-09) or drop `reviewReminders` |
| 11 | SRS-FR-SET-001 requires "key statistics" without listing them, and an editable display name that onboarding never collects (FS-02 issue) | SRS-FR-SET-001 | Approve the §5.1 list (Q4) |
| 12 | RD-16 CAP-10 lists "font scale" as an appearance control, while PRD-FR-096 / SRS-FR-SET-007 make text size follow the system font scale; the current app has an in-app font scale 0.85–1.3 (data_learning_audit §7.1) | RD-16 §2 | Confirm that the in-app font scale is retired and update RD-16 wording |
