# Canonical finding IDs for RD-03 (already referenced in RD-13..16 — keep numbering)

Source identifiers: PO-REQ-1119 = public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484 ; PO-REQ-1120 = current Stage A brief.

## FND-AI
01 No transcript by default (live transcription default OFF)
02 Post-hoc file transcription stubbed (always "")
03 Whisper/Moonshine fake (invented vocab, wrong mel, GGML URL, no downloader, blank hashes)
04 Live transcription: single utterance, duplication on stop, no <queries>, concurrent mic
05 Capability status false positives (MediaPipe ready, offline verified, init failed on no-match)
06 Settings toggles ineffective (localAiEnabled, localAudioAnalysisEnabled)
07 Broken/constant audio metrics (noise unreachable, rate consistency 1.0, single-sample clipping, pitch Hz, ASR confidence constants)
08 Mislabelled scores (freeform pronunciation=loudness/pitch/voicing; grammar baseline 78; fluency 38)
09 Grammar regex & filler false positives; no punctuation -> run-on; TTR length bias
10 Cloud AI compiled in but gated; would send raw audio/video/profile w/o consent; interceptor not wired; ZeroCloud test false assurance
11 YouTube scraping (spoofed UA, timedtext), captions published as techniques
12 Video: models absent; framing only; head-tilt mislabel; activity video simulated
13 Memory/perf (boxed ArrayList<Short> OOM, 60 bitmaps, main-thread capability calc)
14 Interruptions not handled; failed recordings never cleaned
15 Duplication: 4 speech wrappers, 5 availability sources, dead feedback engines
16 Mentor/conversation = templates + simulated streaming + invented profile values
17 STRENGTH: Evidence gate fail-closed, well tested

## FND-DOM
01 Three parallel learning systems (Activity Engine / LearningProgressManager / CurriculumCatalog)
02 LPM mostly dead (5-stage vocab, quiz, notebook, sync merge, export, clear)
03 User goal never reaches dashboard plan
04 Two scoring philosophies; activity score = length formula; fabricated dimension spread
05 Activity completion by 3 typed words / wordless recording; transcript never set
06 Daily routine self-ticked; dashboard tasks one tap
07 Four streaks; stale profile streak bug; gameable
08 Fabricated minutes (3/step), words learned = seen, completedExercises inflated
09 @askvinh IP risk; "VERIFIED" items with channel-root URLs
10 Activity daily plan loses completed items; prerequisites not enforced
11 SM-2 scheduler not wired; UI ad hoc scheduler; UTC day boundary
12 Level vocabularies inconsistent (3/4/5 tiers); no placement or progression
13 Content: 150 activities templated structure; 20 vocab words; duplicate titles
14 Notifications in-app only; two stores; duplicates; dismissed reappear
15 isPartialEvaluation Firestore bean-mapping bug

## FND-DATA
01 Fragmented persistence (Firestore, JSON files, DataStore), no single source of truth
02 LPM state local-only, never synced, lost on reinstall
03 Firestore writes awaited -> offline hang; non-transactional read-modify-write
04 Absolute device paths stored in Firestore
05 16+ unbounded listeners at sign-in
06 speakingSessions writes denied by rules (HEAD regression); YouTube re-analysis denied
07 Rules/blueprint/code drift (dead collections)
08 Main-thread JSON disk I/O

## FND-SEC
01 Google sign-in mandatory; no account-free use
02 Debug-signed beta builds; App Check debug provider; exported activity accepts debug-token extra
03 Account deletion unreachable; no export; partial deletion; undeletable updateEvents
04 Admin model overbroad; client vs rules role mismatch; admins read all profiles
05 Transcripts uploaded to Firestore contradicting privacy text
06 allowBackup=true with template rules
07 Network recognizer fallback without disclosure
08 PII (uid/email) in Logcat in release
09 No local cleanup on sign-out; recordings not user-partitioned
10 Rules validation gaps (list idx>2, no hasOnly), client-writable scores/streaks
11 R8 disabled; debug App Check lib in release; logs not stripped
12 FileProvider over-broad paths

## FND-OPS
01 CI: no unit tests, lint, rules tests, rules deploy
02 Gradle wrapper not committed; google-services.json needed for local build/tests
03 Supply chain: unpinned actions, npm w/o lockfile with SA secret
04 Self-updater: Play policy, REQUEST_INSTALL_PACKAGES missing, user-editable repo, "uninstall" advice
05 No crash reporting/analytics/logging framework/remote config
06 Dependency drift & unused deps; JUnit catalog misref
07 Docs drift on versioning; test hard-asserts BuildConfig version

## FND-ARCH
01 Single :app module 48.9k lines; no domain layer/interfaces
02 AppContainer created in composable; no Application class
03 God files (CoachComponents 1943, AppNavigation 1548, Dashboard widgets 1543, SpeakCraftRepository 1351)
04 Duplication inventory (see §11)
05 Placeholder namespace com.example; applicationId com.aistudio.*

## FND-UI  (fill from UI audit; 02 = dashboard density reserved)
01 Navigation sprawl / overlapping entry points
02 Dashboard density / overloaded home

## FND-TEST
01 161+1 tests; 3 trivial; never run in CI
02 Untested: repos, auth, LPM, plan engines, SRS, notifications sync
03 False-assurance tests (ZeroCloud interceptor, models "bundled")
04 firestore.test.js not runnable; no speakingSessions coverage
