# SpeakCraft UI Layer Audit (Discovery for Redesign)

Audit date: 2026-11-20. Repository: `f:\speakcraft`, branch `main`, HEAD `68a9cb1`. Read-only; no repository files were changed.

## 0. Method, legend and path aliases

**Method.** All UI code was read by hand. That is about 22,000 lines in `ui/` (AppNavigation, 15 screen files, 9 component files, 4 theme files, 5 ViewModels), plus `MainActivity.kt`, `data/AppContainer.kt`, `res/values*` and the 5 named Robolectric/Compose tests.

- Four parallel read-only sub-audits each read a slice of the screen and component files in full. I personally re-verified the highest-impact claims (listed under "Spot-checks" below).
- All grep counts in sections 5, 6 and 9 were produced by me over `app/src/main/java/com/example/ui/`.
- Nothing could be built or run, so nothing was checked at runtime.

**Legend.**
- **[V]** VERIFIED: seen in code at the cited line(s).
- **[I]** INFERRED: a reasoned consequence that was not observed running.

Citations take the form `ALIAS:line` or `ALIAS:start-end`.

**Spot-checks re-verified personally [V]:**
- `LocalExecutionPolicy.POLICY = "LOCAL_ONLY"` (`data/ai/local/LocalExecutionPolicy.kt:13`).
- No code builds an `activity_library/<id>` route.
- Voice Training submits as `"SPEAKING_LAB"` (LM:601-602).
- `clearAnalysis()` does not clear the recorder path (SVM:594-613).
- `NotificationsCenterSheet` has zero call sites.
- Dashboard `onOpenProfile` is declared but unused (D:162).
- The mic permission callback has no denied branch (SAC:150-154).
- `ActivityScreen` reloads on every recomposition key (UA:76-78).
- The admin-role trace (REPO:236-270).
- Manifest content (no `REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS`, `configChanges` or `Application` subclass).
- The `WelcomeBackBanner` body (W:127-189) and the HorizontalPager (W:247-260).
- The plan-row self-report completion (W:614, W:639).
- The `reducedMotion` parameter is not passed to the dashboard (NAV:916-946 vs D:160).

**Path aliases** (relative to `app/src/main/java/com/example/` unless noted):

| Alias | Path | Lines |
|---|---|---|
| NAV | ui/AppNavigation.kt | 1548 |
| D | ui/screens/DashboardScreen.kt | 1038 |
| W | ui/components/DashboardStudioWidgets.kt | 1543 |
| AO | ui/screens/AuthAndOnboardingScreens.kt | 662 |
| P | ui/screens/UserProfileScreen.kt | 616 |
| SAC | ui/screens/SpeakingAndChallengesScreens.kt | 1114 |
| LAT | ui/screens/LabsAndTrainingScreens.kt | 1024 |
| LM | ui/screens/LessonAndModulesScreens.kt | 710 |
| CM | ui/screens/ConversationAndMentorScreens.kt | 878 |
| YT | ui/screens/YouTubeIntelligenceScreen.kt | 375 |
| VO | ui/screens/VocabularyScreen.kt | 555 |
| DP | ui/screens/DailyAndProgramsScreens.kt | 381 |
| ADS | ui/screens/ActivityDiscoveryScreen.kt | 682 |
| UA | ui/screens/UniversalActivityScreen.kt | 538 |
| AS | ui/screens/AnalyticsAndSettingsScreens.kt | 1089 |
| AU | ui/screens/AppUpdateScreen.kt | 660 |
| ADM | ui/screens/AdminDashboardScreen.kt | 402 |
| CC | ui/components/CoachComponents.kt | 1943 |
| ETR | ui/components/ExerciseTemplateRenderers.kt | 819 |
| FT | ui/components/FeatureTutorialComponents.kt | 456 |
| TO | ui/components/TutorialOverlay.kt | 657 |
| LA | ui/components/LaunchAnimationOverlay.kt | 774 |
| ND | ui/components/NotificationDrawer.kt | 785 |
| NC | ui/components/NotificationsCenterSheet.kt | 483 |
| UD | ui/components/UpdateAvailableDialog.kt | 332 |
| MVM | ui/viewmodel/MainCoachViewModel.kt | 487 |
| SVM | ui/viewmodel/SpeakingAndLabsViewModel.kt | 621 |
| ICVM | ui/viewmodel/InteractiveCoachViewModel.kt | 552 |
| AEVM | ui/viewmodel/ActivityEngineViewModel.kt | 589 |
| UVM | ui/viewmodel/UpdateAndAdminViewModel.kt | 432 |
| AC | data/AppContainer.kt | 128 |
| CAT | data/model/CurriculumCatalog.kt | 1124 |
| FTR | data/model/FeatureTutorialRegistry.kt | 440 |
| REPO | data/firebase/SpeakCraftRepository.kt | 1351 |
| HYB | data/ai/HybridAiCoachCoordinator.kt | 356 |
| LIO | data/ai/local/LocalIntelligenceOrchestrator.kt | 984 |
| LPM | data/learning/LearningProgressManager.kt | 920 |
| NMR | data/notifications/NotificationManagerRepository.kt | 494 |
| PR | data/preferences/AppPreferencesRepository.kt | 315 |
| YTC | data/youtube/YouTubeIntelligenceClient.kt | 519 |
| MAN | app/src/main/AndroidManifest.xml | 49 |
| CUJ / TAB / LAUNCH / HAI / AET | app/src/test/java/com/example/{SpeakCraftComposeCujTest, SpeakCraftTabletLayoutTest, SpeakCraftLaunchAnimationTest, HybridAiComposeUiTest, ActivityEngineComposeTest}.kt | 822 / 283 / 295 / 194 / 199 |

---

## Executive summary: the 20 findings that matter most for the redesign

1. **Information architecture**
   - The app registers **26 NavHost routes** (NAV:915-1236) and also has 4 non-route gates (auth, onboarding, profile-loading, profile-error) and 5 global overlays [V].
   - The phone bottom bar exposes only 5 routes (NAV:177-183). The remaining 21 are reached mostly through a **22–23-tile dashboard directory** (D:285-307) [V].
2. **The "@askvinh" bottom tab** (NAV:181) opens a YouTube channel-ingestion tool, "YouTube Channel Intelligence" (YT:63-302).
   - The user pastes a channel URL (default `https://www.youtube.com/@askvinh`, YT:79).
   - The app scrapes captions and then **heuristically fabricates "techniques"**: the first 5 sentences, invented timestamps `idx*45s`, all marked creator-derived and shown as "verified" (LIO:847-907, YT:321) [V].
   - In effect, a primary tab is devoted to a content-pipeline/admin-style tool and to one creator's brand.
3. **Navigation chrome differs by width** [V]:
   - Bottom bar below 600dp and a rail at 600–839dp both show the same 5 items (NAV:828-905).
   - A **10-item sidebar** appears at 840dp and above (NAV:1336-1348). Its labels differ from the bottom bar: "Practice" vs "Daily", "YouTube Intelligence" vs "@askvinh", and **"Tools" → Vocabulary**.
   - Sidebar destinations outside the 5 primary routes get a back arrow and pile up on the back stack (NAV:511, 595-603, 746).
4. **One orphan route:** `activity_library/{moduleId}` (NAV:1211-1222) is never navigated to [V]. `pronunciation` is reachable only from one Quick Access tile (W:1278) [V].
5. **Massive duplication of "practice" capabilities** [V]:
   - 10 screens record the user's voice and run the *same* `SpeakingAndLabsViewModel.submitForAiAnalysis` pipeline (SVM:445-556), or the separate Activity Engine pipeline (ETR/AEVM).
   - 12 different "start recording" labels and 8 different "analyze" labels exist (section 7.2).
   - `articulation_lab` and `pronunciation` render the same composable (NAV:997-1014). The default "Difficult Sounds" filter contains 1 drill (LAT:86-88).
6. **Three parallel daily-plan systems** (Daily Practice 8 steps; dashboard "Today's Practice Plan"; Activity Engine "Daily Plan"). Also two "paths" systems, two module taxonomies, two vocabulary stores and two streak sources (section 7.3) [V].
7. **The dashboard is extremely dense** [V/I]:
   - Up to 16 top-level LazyColumn items, about 21 widgets and about 70 tappable targets.
   - 25 distinct navigation targets; roughly 5–6 phone screens of scroll [I].
   - Three overlapping "today" widgets and two conflicting "weekly goal" concepts (D:766 "Weekly Goal: 5 h" vs W:937 "x / 5 days").
8. **The requested "welcome back experience" is only partially implemented** [V]:
   - `WelcomeBackBanner` is a single animated text line, "Welcome back, {name}" (W:127-189). It ignores its `learningTrack`, `learningDay`, `overallScore`, `personalizedCue` and `onAvatarClick` parameters.
   - There is no avatar. `personalizedCue` is computed but never rendered (D:214-226).
   - **Brand-new users are also greeted with "Welcome back"** [V].
   - The **HorizontalPager carousel is implemented** (W:247-260), with 4 slides. 3 of them are static and all 4 use the same image (W:284-289).
9. **Cloud AI is hard-disabled** by a compile-time constant (`LocalExecutionPolicy.kt:13`; HYB:56-58).
   - Settings nevertheless shows a permanently disabled "Cloud AI Coach" switch and dead model-tier chips (AS:658-713) [V].
   - Every "AI" label in the UI is backed by local deterministic or heuristic code [V].
10. **Permission denial is invisible** [V]:
    - On all 7 lab or lesson recorders the mic-permission callback is `if (granted) start` with no else branch (SAC:150-154 and 6 other sites).
    - There is no rationale and no settings deep link.
11. **Offline and error states are thin** [V]:
    - NAV coerces every Firestore list to `.orEmpty()` (NAV:513-521), discarding Loading and Error for every list-driven screen.
    - A profile load error is a dead-end text with no retry (NAV:490-494).
    - Analysis can spin forever offline because the save awaits `batch.commit()` before leaving Loading (SVM:509-535; REPO:1337-1350) [I, strong].
12. **The state model is fragile** [V]:
    - 0 `rememberSaveable` in screens versus 89 `remember { mutableStateOf }`.
    - 0 `SavedStateHandle` anywhere.
    - `AppContainer` is created inside a composable `remember` (NAV:216) with no `Application` subclass (MAN:17-25), so it is rebuilt on every Activity recreation.
    - The activity attempt is wiped on rotation (UA:76-78).
13. **God objects** [V]:
    - `AuthenticatedAppShell` is a **1,032-line composable** (NAV:290-1321) holding VM wiring, 20+ state collections, business logic (NAV:546-593), dialog gating and the whole NavHost.
    - `MainCoachViewModel` observes 15 Firestore flows (MVM:100-152).
    - `UpdateAndAdminViewModel` mixes OTA updates, backups, admin CMS, YouTube sync and AI extraction (UVM:30-431).
14. **Design tokens:** colors are fully tokenized (0 `Color(0x` outside `ui/theme`). Spacing, shape and type are not [V]:
    - 1,386 literal `N.dp` values in `ui/`.
    - 147 raw `RoundedCornerShape(` calls.
    - 95 `fontWeight` overrides.
    - `labelSmall` is **JetBrains Mono** and is used 103 times.
    - `headlineSmall` is used 6 times but not defined, so it falls back to the system font.
    - No dynamic color.
15. **Accessibility** [V]:
    - 127 of 195 `Icon(` calls have `contentDescription = null` (mostly decorative).
    - Only 6 `heading()` usages and 2 `Role.` usages in the entire UI.
    - Several fixed-height text containers will clip at large font scales: the hero at 252dp (W:282), the flashcard at 270dp (VO:241), the score ring at 72dp (CC:1505-1537).
    - The dashboard ignores system reduced-motion.
16. **Tutorial system: 4 overlapping mechanisms** (auto core tour, first-use spotlight, help-icon dialog plus a second dashboard instance, inline `FeatureGuideStrip` on 23 screens) [V].
    - Pressing Back or "Skip" marks the tutorial completed (NAV:1252-1268) [V].
17. **Update system** [V/I]:
    - The update dialog becomes **non-dismissable for mandatory releases** on every route (UD:75-77, 319; NAV:646-655).
    - In-app install likely cannot succeed: there is no `REQUEST_INSTALL_PACKAGES` permission (MAN:5-8). The user may be stuck with a browser sideload [I].
    - "Check for Updates" silently auto-downloads (UVM:216-226) [V].
18. **Settings mixes developer and user concerns** [V]:
    - 29 controls. Developer diagnostics come first on phones (AS:1057-1064).
    - A plaintext YouTube API key field (AS:946-953).
    - **No account deletion**: `MainCoachViewModel.deleteAccountAndAllData` has 0 callers (MVM:474).
19. **Dead and duplicate UI** [V]:
    - `NotificationsCenterSheet` (483 lines) is dead and duplicates `NotificationDrawer`.
    - `feedbackSection` is pasted 8 times; the permission-checked record button 7 times; the list markup twice per lab (phone and tablet).
    - Multiple dead VM APIs and deprecated constructors are still exercised only by tests.
20. **Tests** [V]:
    - The 5 named test files hold 44 tests (CUJ 20, TAB 7, LAUNCH 8, HAI 5, AET 4). 40 of them render a single composable with fake callbacks; the other 4 test timeline, VM or engine logic.
    - **No test exercises `SpeakCraftRoot`/NavHost**, the bottom bar, back-stack behavior, permission denial, offline mode or font scaling.
    - Tests use deprecated VM constructors that production never calls (9 `GeminiCoachService()` uses in tests, 0 in main).

---

## 1. Navigation inventory

### 1.1 Shell structure (as implemented)

```
MainActivity.onCreate (MainActivity.kt:11-21)
  configureAppCheck -> enableEdgeToEdge() -> setContent { SpeakCraftRoot() }
SpeakCraftRoot (NAV:210-286)
  AppContainer = remember(appContext) { AppContainer(...) }      NAV:216   [not process-scoped]
  theme from prefs.themeMode SYSTEM/LIGHT/DARK                     NAV:221-225
  Box {
    if (currentUser == null) AuthScreen                           NAV:249-250
    else key(uid) AuthenticatedAppShell                           NAV:252-261
    if (launchVisible) settle-bg | SpeakCraftLaunchOverlay        NAV:264-283  (drawn over everything)
  }
AuthenticatedAppShell (NAV:288-1321, 1,032 lines)
  creates 5 ViewModels (Activity-scoped, keyed by uid)            NAV:317-403
  when(profileState):
    Loading  -> "Preparing your speaking studio..."               NAV:469-489
    Error    -> "Could not load profile: …" (no retry)            NAV:490-494
    Success(null | !onboardingCompleted) -> OnboardingAssessmentScreen   NAV:497-503
    Success(onboarded) ->
      overlays/dialogs: FeatureTutorialDialog (NAV:605-615), UpdateAvailableDialog (NAV:657-675)
      BoxWithConstraints: width>=840 sidebar | 600..839 rail | <600 bottom bar   NAV:677-679
      Scaffold(topBar = back? title, bell, help, [admin], avatar; bottomBar)       NAV:692-858
      NavHost(start="dashboard", fade 180/140ms)                                  NAV:906-1237
      TutorialOverlay (NAV:1242-1301), NotificationDrawer (NAV:1303-1316)
```

Top-bar actions on every authenticated screen [V]:
- Back arrow when the route is not one of the 5 primary routes (NAV:745-751).
- Bell with an unread badge that caps at "9+" (NAV:753-780).
- Help "Feature guide & what improves" (NAV:781-792).
- Admin icon, only if `isAdmin` (NAV:793-797).
- Initials avatar → `profile` (NAV:798-821).

On the dashboard at phone width the title is the brand lockup "SpeakCraft / Communication Improvement" (NAV:705-736). On the dashboard with the sidebar it is **empty** (the NAV:705 condition is false and the NAV:737 condition is false) [V].

### 1.2 Navigation chrome per width

| Width | Chrome | Items, with exact labels |
|---|---|---|
| < 600dp | `NavigationBar` (NAV:828-857) | Home → `dashboard` · Daily → `daily_practice` · Record → `speaking_lab` · **@askvinh** → `youtube_library` · Mentor → `ai_mentor` (NAV:177-183) [V] |
| 600–839dp | `NavigationRail` (NAV:861-905) | The same 5 items and labels. No access to the sidebar-only destinations [V] |
| ≥ 840dp | `SpeakCraftIndigoSidebar`, 232dp (NAV:1329-1547) | Home → `dashboard`; "150 Activities" → `activity_library`; "Practice" → `daily_practice`; "AI Mentor" → `ai_mentor`; "Learn" → `learning_paths`; "Record & Review" → `speaking_lab`; "YouTube Intelligence" → `youtube_library`; **"Tools" → `vocabulary`**; "Progress" → `analytics`; "Settings" → `settings` (NAV:1338-1347). Plus a bottom card, "Update Ready"/"Studio Sync" with button "Install Update"/"Check Updates" → `app_updates` (NAV:1477-1544) [V] |

### 1.3 Route table

The table lists all 26 NavHost routes. Titles come from `routeTitles` (NAV:185-208) or the dynamic title logic (NAV:527-538).

| # | Route (args) | NAV line | Composable (definition) | Top-bar title | How a user reaches it (all verified call sites) |
|---|---|---|---|---|---|
| 1 | `dashboard` | 915 | `DashboardScreen` (D:143) | brand lockup | Start destination (NAV:908). Bottom bar, rail and sidebar "Home". Settings "Replay Welcome & Module Tours" → dashboard (NAV:1148-1153). LPM alert route (LPM:294) |
| 2 | `daily_practice` | 949 | `DailyPracticeScreen` (DP:59) | "Daily Practice" | Bottom/rail "Daily", sidebar "Practice". Dashboard: module grid "Daily Speaking Practice" (CAT:170), ring link "Reflect" (W:855), fallback plan row "Daily Reflection" (W:525), plan default route (W:513). Tour step 4 "Start Daily Practice" (TO:109-117). Alerts (NMR:101; LPM:137, 160). RecommendationEngine:123. DailyPracticePlanEngine:204 |
| 3 | `learning_paths` | 960 | `LearningPathsScreen` (DP:271) | "Learning Paths" | Sidebar "Learn". Dashboard tile "30/60/90 Plans" (D:297), ring link "Learn" (W:852), "My Learning Tracks › View All" (D:575). Alert (LPM:326) |
| 4 | `vocabulary` | 967 | `VocabularyScreen` (VO:69) | "Vocabulary Builder" | Sidebar **"Tools"**. Dashboard grid "Vocabulary Development" (CAT:171), Quick Access tile (W:1277), plan rows. Daily steps 1 and 3 (CAT:990, 992). Tour step 6. Alerts (LPM:148, 338). Recommendation:102. DPPE:81 |
| 5 | `speaking_lab` | 981 | `SpeakingLabScreen` (SAC:103) | "Record & Review" | Bottom/rail "Record", sidebar "Record & Review". Dashboard grid "Record and Review" (CAT:181), hero "Quick Practice" (W:213, W:236-242), ring "Practice" (W:853), "Latest Recording Review" card (D:887). Daily step 7 (CAT:996). Tour step 2. Alerts (NMR:116, 268). Recommendation:134. DPPE:165 |
| 6 | `challenges` | 989 | `ChallengesScreen` (SAC:794) | "Challenges" | Dashboard grid (CAT:182), Quick Access (W:1284), hero slide 3 (W:231). Recommendation:39. DPPE:96 |
| 7 | `articulation_lab` | 997 | `PronunciationLabScreen(initialFocus="articulation")` (LAT:75) | "Articulation & Enunciation Lab" | Dashboard grid "Articulation Lab" (CAT:168), hero slide 2 (W:221), fallback plan row (W:521). Daily step 2 (CAT:991). Recommendation:49. DPPE:122, 142 |
| 8 | `pronunciation` | 1006 | `PronunciationLabScreen(initialFocus="pronunciation")` (LAT:75) | "Phonetic & Intonation Lab" | **Only** the Quick Access tile "Pronunciation Practice" (W:1278) [V] |
| 9 | `voice_training` | 1015 | `VoiceTrainingLabScreen` (LM:523, defined in the lessons file) | "Voice Training Lab" | Dashboard grid (CAT:169), hero slide 2 secondary (W:222), fallback plan row. Daily step 4 (CAT:993). First-use spotlight. Recommendation:94. DPPE:57, 284, 294 |
| 10 | `conversation` | 1018 | `ConversationSimulatorScreen` (CM:99) | "Conversation Mastery" | Dashboard grid (CAT:172), Quick Access (W:1279), hero slide 3 (W:230). Tour step 5. DPPE:254, 274 |
| 11 | `listening_lab` | 1026 | `ListeningShadowingScreen` (LAT:332) | "Active Listening" | Dashboard grid (CAT:173), fallback plan row. Daily step 5 (CAT:994). Recommendation:110. DPPE:178 |
| 12 | `grammar_speaking` | 1034 | `GrammarSpeakingScreen` (LAT:537) | "Thinking & Articulation" | Dashboard grid "Thinking & Articulation" (CAT:178), fallback plan row "Contextual Sentence Construction" (W:522). DPPE:109, 191 |
| 13 | `public_speaking` | 1041 | `PublicSpeakingScreen` (LAT:725) | "Public Speaking" | Dashboard grid (CAT:174), Quick Access (W:1281). First-use spotlight. DPPE:264 |
| 14 | `real_world_tasks` | 1048 | `RealWorldTasksScreen` (LAT:912, in the labs file) | "Real-World Tasks" | Dashboard tile "Field Tasks" (D:298). Daily step 6 (CAT:995) |
| 15 | `module_lessons/{slug}` (String) | 1054-1066 | `ModuleLessonsScreen` (LM:81) | Module title, else "Module Lessons" (NAV:528-529) | Dashboard grid: storytelling, body_language, confidence, professional, influence (CAT:175-177, 179-180). Quick Access: storytelling, confidence, body_language (W:1280, 1282, 1283) |
| 16 | `lesson_detail/{lessonId}` (String) | 1067-1083 | `LessonDetailScreen` (LM:255) | Lesson title, else "Lesson Detail" (NAV:530-533) | Module Lessons list (NAV:1063). "Next Lesson" chain (NAV:1081). Hero slide 1 → `lesson_vinh_3stage_audit` (W:212). RecommendationEngine (59, 74) |
| 17 | `youtube_library` | 1084 | `YouTubeIntelligenceScreen` (YT:63) | "YouTube Intelligence" | Bottom/rail **"@askvinh"**, sidebar "YouTube Intelligence". Dashboard tile "@askvinh Library" (D:299). Module Lessons empty-state CTA "Open YouTube Library" (NAV:1064; LM:139-142) |
| 18 | `analytics` | 1093 | `AnalyticsScreen` (AS:88) | "Progress Analytics" | Sidebar "Progress". Dashboard tile "Analytics" (D:300), ring "Review" (W:854). Profile "Progress Analytics" (P:496). Tour step 7. Alerts (LPM:314, 842) |
| 19 | `ai_mentor` | 1101 | `AiMentorScreen` (CM:597) | "AI Mentor" | Bottom/rail "Mentor", sidebar "AI Mentor". Dashboard tile (D:301). Inline mentor card send/chips (NAV:938-945; D:558). Analytics CTA (NAV:1098). Tour step 3 |
| 20 | `app_updates` | 1110 | `AppUpdateScreen` (AU:78) | "App Updates & Backups" | Sidebar card (NAV:1522). Dashboard tile "App Updates" (D:302), update banner (D:341). Update dialog "Download & Install" (NAV:667). Settings "Check for Updates" (NAV:1136-1139). Profile "App Updates & Backups" (P:517) |
| 21 | `admin_console` | 1118 | `AdminDashboardScreen` (ADM:65) | "Admin Console" | Top-bar icon if admin (NAV:793-797). Dashboard tile "Admin Studio" if admin (D:303-304). Settings "Open Admin & Content Studio" if admin (AS:506-518; NAV:1141) |
| 22 | `settings` | 1126 | `SettingsScreen` (AS:394) | "Settings" | Sidebar "Settings". Profile "App Settings" (P:510). "AI Settings" button in labs (`onOpenAiSettings`, NAV:986, 994, 1003, 1012, 1031, 1038, 1045). LiveTranscriptPanel "Check Permissions" (CC:1199-1206, which opens in-app Settings, not Android settings). Alert (LPM:659). **Not on the phone bottom bar or the rail** |
| 23 | `profile` | 1189 | `UserProfileScreen` (P:96) | "My Profile" | Top-bar avatar (NAV:798-806). Welcome alert (NMR:82). The dashboard `onOpenProfile` parameter is declared but never invoked, and its default would go to `"settings"` (D:162) [V] |
| 24 | `activity_library` | 1204 | `ActivityDiscoveryScreen` (ADS:81) | "Activity Engine (150)" | Sidebar "150 Activities" (NAV:1339). Dashboard tile "Activity Engine" (D:296). ActivityScreen "Browse All 150 Activities" (UA:133 → NAV:1234, which pushes a new copy) |
| 25 | `activity_library/{moduleId}` (String) | 1211-1222 | `ActivityDiscoveryScreen(initialModuleId)` | "Activity Engine (150)" | **None: orphan.** No code builds `"activity_library/…"` (repo-wide grep finds only the route declaration at NAV:1212) [V] |
| 26 | `activity/{activityId}` (String) | 1223-1236 | `ActivityScreen` (UA:66) | Activity title, else "Activity Studio" (NAV:535-536) | Activity library rows (NAV:1208, 1220). "Continue Next" (NAV:1233) |

**Non-route surfaces** [V]:
- `AuthScreen` (AO:79), shown when signed out (NAV:250).
- `OnboardingAssessmentScreen` (AO:331), the onboarding gate (NAV:498).
- Profile loading and error screens (NAV:469-494).
- Global overlays: launch (LA:251), `UpdateAvailableDialog` (UD:53), `FeatureTutorialDialog` (FT:268; instances at NAV:606 **and** D:172-178), `TutorialOverlay` (TO:185), `NotificationDrawer` (ND:96).

### 1.4 Full navigation tree (phone, as experienced)

```
[Launch overlay 2.9s first / 2.1s returning / 0.6s reduced-motion]
 └─ AuthScreen (Google only) ─► Onboarding (3 steps) ─► Dashboard (+ auto 7-step tour)
Bottom bar
 ├─ Home: dashboard
 │    ├─ Update banner ─► app_updates
 │    ├─ Hero pager (4 slides): lesson_detail/lesson_vinh_3stage_audit, speaking_lab, articulation_lab,
 │    │                         voice_training, conversation, challenges, <recommended route>
 │    ├─ Today's Practice Plan rows ─► plan task routes (vocabulary, articulation_lab, grammar_speaking,
 │    │                                voice_training, listening_lab, daily_practice, conversation, …)
 │    ├─ Today's Progress ring: Learn─►learning_paths · Practice─►speaking_lab · Review─►analytics · Reflect─►daily_practice
 │    ├─ Inline AI Mentor card ─► ai_mentor
 │    ├─ Quick Access (8): vocabulary, pronunciation, conversation, module_lessons/storytelling,
 │    │                    public_speaking, module_lessons/confidence, module_lessons/body_language, challenges
 │    ├─ My Learning Tracks (4) ─► (filter only) · View All ─► learning_paths
 │    ├─ Latest Recording Review ─► speaking_lab
 │    ├─ "Explore Tutorials for All 24 Features" ─► FeatureTutorialDialog ─► (Open) any allow-listed route
 │    └─ All Communication Modules & Tools (22/23 tiles):
 │         15 content modules (CAT:168-182) ─► 9 lab routes + 5 module_lessons/{slug}
 │              module_lessons/{slug} ─► lesson_detail/{id} ─► Next Lesson ─► lesson_detail/{next} ─► …
 │                                    └─► youtube_library (empty-state CTA)
 │         7 tools: activity_library ─► activity/{id} ─► Continue Next ─► activity/{next} ─► …
 │                                                    └─► Browse All ─► activity_library (pushed again)
 │                  learning_paths, real_world_tasks, youtube_library, analytics ─► ai_mentor, ai_mentor, app_updates
 │         (+ admin_console for admins)
 ├─ Daily: daily_practice ─► 7 step routes (vocabulary ×2, articulation_lab, voice_training, listening_lab,
 │                           real_world_tasks, speaking_lab)
 ├─ Record: speaking_lab ─► settings ("AI Settings")
 ├─ @askvinh: youtube_library ─► external YouTube app/browser only
 └─ Mentor: ai_mentor
Top bar (all screens): bell ─► NotificationDrawer ─► alert routes · help ─► FeatureTutorialDialog
                       admin icon ─► admin_console · avatar ─► profile ─► analytics, settings, app_updates, drawer
```

### 1.5 Orphans, unreachable, broken links

- **Orphan route:** `activity_library/{moduleId}` (NAV:1211-1222) is never built [V]. ADS also has an `initialTab` parameter (ADS:87) that is never passed [V].
- **Near-orphan:** `pronunciation` has only one entry point, the Quick Access tile (W:1278) [V]. On rail and sidebar layouts Quick Access still exists on the dashboard (D:465, D:565), so it stays reachable [V].
- **Unreachable on rail-width devices (600–839dp)** except through the dashboard: `activity_library`, `learning_paths`, `vocabulary`, `analytics`, `settings`. The rail carries only the 5 primary items (NAV:883) [V].
- **Profile entry:** reachable only through the top-bar avatar and one alert. The dashboard's avatar callback is dead (D:162; `WelcomeBackBanner.onAvatarClick` unused, W:135) [V].
- **Broken route strings:** none among the literals. Every dashboard, catalog, recommendation, plan and notification route resolves to a NavHost destination [V] (sub-audit plus my grep of route definitions).
- **Runtime-crash risk:** announcement `targetRoute` is free text from Firestore (REPO:288). `sanitizeRoute` only blocks update routes (`data/notifications/NotificationModels.kt:154-163`). An unknown route passed to `navController.navigate` (NAV:1315) throws `IllegalArgumentException` [I].
- **Tutorial registry vs routes:**
  - `FeatureTutorialRegistry` has 24 keys, including the non-routes `auth` and `onboarding`.
  - `profile`, `activity_library`, `activity_library/{moduleId}` and `activity/{activityId}` have **no tutorial**, so the help icon silently shows the *dashboard* guide on those routes (FTR:437-438) [V].
  - `admin_console` is listed for all users in "All Features (24)" (FT:306) [V].

### 1.6 Back-stack behaviour

- `navigateTo` applies `popUpTo(start){saveState=true}` **only** to the 5 primary routes, but `launchSingleTop=true` and `restoreState=true` to every route (NAV:595-603) [V].
- **Sidebar inconsistency (≥ 840dp):** `activity_library`, `learning_paths`, `vocabulary`, `analytics` and `settings` are top-level sidebar items, but they are pushed onto the stack and show a back arrow, because `isPrimaryRoute` only knows the 5 bottom-bar routes (NAV:511, 746) [V]. Repeated sidebar taps among them build an arbitrarily deep stack [I].
- **Unbounded chains** [V]:
  - Lesson "Next Lesson" (NAV:1081).
  - Activity "Continue Next" (NAV:1233).
  - "Browse All 150 Activities" pushes a *new* `activity_library` instead of popping back (NAV:1234).
  - "Next Lesson" silently crosses modules (CAT:524) [V].
- **Selection highlighting:** a bottom or rail item is highlighted only on exact route match (NAV:837, 884). On any secondary screen no tab is highlighted, even when it was reached from that tab [V].
- **Dialog-driven navigation:**
  - The update dialog "Download & Install" navigates to `app_updates` and starts a download in the same click (NAV:665-669) [V].
  - Tutorial "Open {module}" navigates and marks the **whole core tour** completed, so the remaining steps are lost (NAV:1288-1299) [V].
- **System Back** [V]:
  - During onboarding it steps back through the wizard (AO:384-386).
  - Over `TutorialOverlay` it *dismisses and persists completion* (TO:212-214; NAV:1252-1268).
  - The drawer has its own `BackHandler` (ND:111-113).
  - The launch overlay has none (LA:251-658).

### 1.7 Duplicate entry points to the same capability

| Capability | Distinct entry points and labels (examples) |
|---|---|
| Record & Review (`speaking_lab`) | "Record" (bottom), "Record & Review" (sidebar), "Record and Review" (grid, CAT:181), "Quick Practice" (hero, W:441), "Practice" (ring, W:853), "Latest Recording Review" (D:887), "Record & Review (3-Stage Self-Audit)" (daily step, CAT:996), "Open Speaking Lab" (tour), streak and session alerts (NMR:116, 268). **≥ 10 entry points** [V] |
| Pronunciation/articulation (one composable) | "Articulation Lab" (CAT:168), "Pronunciation Practice" (W:1278), "Speak with Clarity" (hero, W:218), "Articulation & Enunciation Drill" (W:521; CAT:991). Two routes [V] |
| AI Mentor | "Mentor" (bottom), "AI Mentor" (sidebar, tile D:301, inline card W:1113), Analytics "Ask AI Mentor for Custom Weekly Plan" (AS:357-362), tour step 3 [V] |
| Daily practice | "Daily", "Practice" (sidebar), "Daily Speaking Practice" (CAT:170), "Reflect" (W:855), "Daily Reflection" (W:525), alerts [V] |
| Learning paths/programs | "Learn" (sidebar, ring), "30/60/90 Plans" (D:297), "View All" under "My Learning Tracks" (W:1444). The track cards do not navigate (D:576) [V] |
| YouTube tool | "@askvinh" (bottom), "YouTube Intelligence" (sidebar, title), "@askvinh Library" (D:299), "YouTube Channel Intelligence" (YT:108), "Open YouTube Library" (LM:139-142) [V] |
| App updates | 6 entry points (route table row 20) [V] |
| Settings | 9 entry points, but none on the phone's primary chrome (route table row 22) [V] |
| Tutorials | Auto tour, first-use spotlight, help icon, dashboard "Explore Tutorials…" button, Settings "Replay Welcome & Module Tours", inline `FeatureGuideStrip` on 23 screens [V] |

### 1.8 The "@askvinh" primary tab: exactly what it does

1. **Tab:** `PrimaryNavItem("youtube_library", "@askvinh", PlayCircle)` (NAV:181). It is one of 5 primary destinations on phones and rails [V].
2. **Screen:** `YouTubeIntelligenceScreen` (NAV:1084-1092; YT:63-302). Top bar "YouTube Intelligence" (NAV:200); header "YouTube Channel Intelligence" (YT:108) [V].
3. **Input:** a URL field pre-filled with `https://www.youtube.com/@askvinh` (YT:79). Any channel, playlist or video URL is accepted, so there is no channel lock (YT:148) [V]. An optional YouTube Data API v3 key is entered in **Settings**, not here (AS:946-958; passed via `prefs.youtubeApiKey`, NAV:1090) [V].
4. **Primary action:** "Inspect & Extract Lessons" (YT:168). While running it shows "Analyzing Feed & Captions..." (YT:161-168) [V]. The pipeline:
   - Up to 15 videos come from the RSS feed when there is no key, or from Data API search when there is one (YTC:95, 110-111, 123, 152, 310).
   - Captions are scraped from the watch-page timedtext (YTC:391-452).
   - The run is sequential and cannot be cancelled (ICVM:485-533) [V].
5. **"Intelligence":** always a local heuristic, because cloud AI is disabled (HYB:333-338; `LocalExecutionPolicy.kt:13`) [V]:
   - A keyword category (LIO:829-839).
   - The first ≤ 5 sentences of 30–240 characters (LIO:847-854).
   - Templated text with **invented timestamps `idx*45s`** (LIO:870-881), marked `isCreatorDerived = true` (LIO:882, 905).
   - Videos without captions still yield a generic template technique stamped "00:00" (LIO:886-907).
6. **Output UI** [V]:
   - "Video Extraction Audit" with raw status strings such as "EXTRACTED" and "NO_CAPTIONS" (YT:185, 198).
   - "Searchable Knowledge Base" (YT:232) with category chips from `CurriculumCatalog.allCategories` (YT:256). Four extractor categories are missing from that chip list (LIO:830-838), so those items appear only under "All".
   - Empty state "No matching techniques" (YT:270).
   - Any timestamp other than "Pending" shows as **verified** (YT:321).
7. **Playback:** no in-app player. Cards open YouTube externally through `LocalUriHandler` (YT:193, 284, 295, 365). There is no "practice this technique" action (YT:304-375) [V].
8. **Data:** results are written to `users/{uid}/youtubeSources` and `users/{uid}/extractedTechniques` (REPO:940, 964), never to lessons [V]. The list is pre-seeded with 15 "@askvinh" knowledge-base items by `seedAskVinhKnowledgeBaseIfEmpty`, which runs in MVM init and after onboarding (MVM:187-191, 230) [V].
9. **Brand saturation:** "askvinh" appears on 149 source lines across `app/` [V, sub-audit grep]. Examples:
   - Auth tagline "AI Communication Coach grounded in Vinh Giang (@askvinh) frameworks" (AO:146).
   - "Browse all 15 @askvinh modules" (D:647).
   - All 12 "verified" lessons link to the channel home page, not to specific videos (CAT:205-447).

**Assessment:** a content-ingestion and creator-brand tool occupies one of five primary tabs. Ordinary users have no clear task there and are sent out of the app. The output is presented as "verified" lessons that are in fact heuristic sentence extracts [V/I].

---

## 2. Screen inventory

**State columns:** L = loading, E = empty, Er = error, Off = offline, Perm = permission-denied. "—" means absent.

All list data from MVM reaches screens through `(state as? UiState.Success)?.data.orEmpty()` (NAV:513-521). So **no list-driven screen can tell Loading or Error apart from "empty"** [V].

| Screen (definition, span) | Route | Purpose · main content | Primary action | Secondary actions | Data source | L | E | Er | Off | Perm |
|---|---|---|---|---|---|---|---|---|---|---|
| `AuthScreen` AO:79-304 (226) | gate | Sign-in. "SpeakCraft", the @askvinh tagline (AO:146), "What's inside:" plus 4 feature rows (AO:194-218) | "Sign in with Google" (AO:268) | — | `AuthHelper` Credential Manager (AH:58-62, 98) | "Checking existing Google session..." (AO:159-183), button spinner (AO:261-266) | n/a | raw `localizedMessage` (AO:225-233; AH:121) | — | n/a |
| `OnboardingAssessmentScreen` AO:331-631 (301) | gate | 3-step wizard (section 3.2) | "Start My Coaching Plan" (AO:599-623) | "Back", "Continue" | local state → `mainVm.completeOnboardingAssessment` (NAV:500-502) | — (no submit state; double-tap possible) | n/a | — (failure snackbar has no host, section 3.2) | — | n/a |
| `DashboardScreen` D:143-676 (534) | dashboard | Home hub, about 21 widgets (section 3.4) | Hero "Start Lesson" (W:398-421) | about 70 tappables, 25 routes | params from NAV, `learningSnapshot`, `CurriculumCatalog` | shell only (NAV:469-489) | search "No matching modules" (D:362-367) | — | — | n/a |
| `DailyPracticeScreen` DP:59-214 (156) | daily_practice | "Daily Communication Practice" · "$date • x/8 activities completed" (DP:153-154), "Today's 3 Target Words" (DP:90), 8 step cards, reflection slider 1–10 | Step toggle / "Open" (DP:262) | "Save Daily Reflection" (DP:110-140) | MVM `dailyActivities`, `dailyVocabularyBatch`; `CAT.dailySteps` (CAT:989-998) | — | — | snackbar only | — | n/a |
| `LearningPathsScreen` DP:271-381 (111) | learning_paths | "Structured Learning Programs" (DP:291), 7 programs (CAT:1000-1124) | "Enroll" | Day checkboxes, "Hide Plan" | MVM `enrollments` | — | — | snackbar | — | n/a |
| `VocabularyScreen` VO:69-452 (384) | vocabulary | Tabs "Words / Flashcards / AI Quiz / Notebook" (VO:109); 20-word bank (CAT:544+) | Per tab: "Mastered Next", "Evaluate Conversational Usage", "Add to Personal Notebook" | "Review Again (1d)", "Next Word", TTS speak | MVM `vocabularyEntries`, `vocabEvalState`; `CAT.vocabularyBank` | quiz only (VO:359-384) | "No matching words" (VO:177-180) | quiz only | — | n/a |
| `SpeakingLabScreen` SAC:103-790 (688) | speaking_lab | "Record and Review Studio" (SAC:621-624); 3-step indicator; capture card; 3-stage self-audit playback; transcript; history of 10 | "Record Mic" (SAC:352) → "Analyze With Local AI Coach" (SAC:516) | "Video Clip", "Attach Video from Photo Picker", "Transcribe Audio", playback, delete, "AI Settings" | SVM (22 flows, SAC:111-133); `pastSessions`; delete via MVM | shimmer (SAC:665, 735) | "No saved recordings yet" (SAC:776-780) | retry card (SAC:678-687, 748-757) | — | **silent** mic (SAC:150-154); camera error with misleading "Try Again" that only dismisses (SAC:639-643) |
| `ChallengesScreen` SAC:794-1114 (321) | challenges | "Communication Challenges" (SAC:1001); filter ALL/DAILY/WEEKLY; timed card | "Start {N}s Timer" (SAC:943) → "Submit AI Check" (SAC:965) | "Stop Early", "Pause/Resume" | `CAT.speakingChallenges` (10) plus Firestore `sharedChallenges` merged in the composable (SAC:802-815) | shimmer | — | card **without retry** (SAC:1053, 1099) | — | silent (SAC:835-837) |
| `PronunciationLabScreen` LAT:75-329 (255) | articulation_lab, pronunciation | Two headers by focus (LAT:107-116); 8 drills (CAT:916-925); IPA, contrast, phrase | "Record" (LAT:186) → "Compare AI" (LAT:199) | "Listen 0.75x" (actually plays at 0.72, LAT:143), "Listen 1.0x" | SVM; CAT | shimmer | — | retry | — | silent (LAT:103-105) |
| `VoiceTrainingLabScreen` LM:523-710 (188) | voice_training | "@askvinh 5 Vocal Foundations…" while only 4 levers exist (LM:534-541, 635-636) | "Practice Drill" (LM:596) → "Evaluate Vocal Foundations With AI" (LM:611) | "Hear Reference", unlabeled WPM and pause sliders (LM:570-571) | SVM, but submits as `SPEAKING_LAB` (LM:602) | shimmer | — | card without retry | — | silent (LM:550-552) |
| `ConversationSimulatorScreen` CM:99-595 (497) | conversation | "Conversation Simulator", 13 scenarios (CAT:900-914), chat, voice-turn card | "Send reply" / "Speak Voice Turn" (CM:322, 410-433) | "End & Get Coach Check-In Feedback" (CM:449), scenario switch dialog | ICVM | "Partner is responding..." (CM:242) | n/a (auto greeting) | retry (CM:246-257) | — | silent (CM:124-125) |
| `ListeningShadowingScreen` LAT:332-534 (203) | listening_lab | "Active Listening & Shadowing"; 3 hardcoded segments | "Shadow & Repeat" (LAT:409) → "Score Shadow" (LAT:421) | "Play Reference" (TTS), "Watch @askvinh" (channel home) | SVM; CAT:932-952 | shimmer | — | retry | — | silent (LAT:353-355) |
| `GrammarSpeakingScreen` LAT:537-722 (186) | grammar_speaking | "Thinking & Articulation / Grammar"; 7 grammar topics, nothing on PREP/CLEAR despite the subtitle (LAT:650; CAT:960-968) | "Speak Drill" (LAT:598) → "Check Grammar" (LAT:610) | — | SVM | shimmer | — | retry | — | silent (LAT:556-558) |
| `PublicSpeakingScreen` LAT:725-909 (185) | public_speaking | "Public Speaking & Presentation Training"; 6 modules, audio only, though one is body language (CAT:976) | "Record Rehearsal" (LAT:785) → "Get Presentation AI" (LAT:797) | — | SVM | shimmer | — | retry | — | silent (LAT:744-746) |
| `RealWorldTasksScreen` LAT:912-969 (58) | real_world_tasks | "Real-World Communication Tasks"; checkbox plus reflection per task | "Save Reflection" (LAT:1020) | Checkbox autosave | MVM `realWorldTasks`; IDs are list positions `rw_task_$idx` (LAT:940, 956) | — | — | snackbar | — | n/a |
| `ModuleLessonsScreen` LM:81-209 (129) | module_lessons/{slug} | Lesson cards plus "Queued Frameworks (Transcript Verification Required)" (LM:183) | Open lesson | "Open YouTube Library" (LM:139-142), "Fetch Live Captions in YouTube Library" (LM:201) | CAT lessons plus MVM `publishedSharedLessons` (LM:90-116) | — | "No lessons published in this module yet" (LM:139-142); `confidence` has 0 lessons (CAT:497-517) | — | — | n/a |
| `LessonDetailScreen` LM:255-521 (267) | lesson_detail/{id} | Objective, source link, "Technique Breakdown", "Practice Prompt", recorder, feedback, "Completion & Takeaways" | "Record" (LM:373) → "Get AI Feedback" (LM:399) | "Mark Complete" (LM:426-459), "Next Lesson" | CAT, SVM, MVM toggle | shimmer | "Lesson not found" (LM:292) | card **without retry** (LM:411) | — | silent (LM:309-311) |
| `YouTubeIntelligenceScreen` YT:63-302 (240) | youtube_library | Section 1.8 | "Inspect & Extract Lessons" (YT:168) | Search, category chips, open video externally | ICVM; MVM sources and techniques | button spinner (YT:161-168) | "No matching techniques" (YT:270) | card (YT:173-178) | — | n/a |
| `AnalyticsScreen` AS:88-248 (161) | analytics | "Progress Analytics"; Daily/Weekly/Monthly; 4 metric tiles; 4 cards (section 8) | Period chips | "Ask AI Mentor for Custom Weekly Plan" (AS:357-362) | MVM sessions and dailies; profile | — | "No sessions in {period} window" (AS:258-265) | — | — | n/a |
| `AiMentorScreen` CM:597-878 (282) | ai_mentor | "Personalized AI Mentor"; role chips (4); 4 quick prompts; chat | "Ask mentor" icon (CM:872) | Dictate, "Clear" thread dialog | ICVM; history in a file (ICVM:156-188) | "Analyzing scores with On-Device Local AI..." (CM:798) | greeting (ICVM:80-83) | retry (CM:803-809) | — | silent (CM:617) |
| `AppUpdateScreen` AU:78-253 (176) | app_updates | Section 8.4 | "Check for Updates Now" | Channel chips, GitHub repo "Sync", backups, restore | UVM | "Checking & Downloading..." | release-less dev text (AU:373) | card whose "Try Again" only resets | — | n/a |
| `AdminDashboardScreen` ADM:65-402 (338) | admin_console | 4 tabs (section 8.5) | Per-tab publish | — | UVM | busy flags | — (no empty states for releases or telemetry) | snackbar | — | non-admin card (ADM:71-90) |
| `SettingsScreen` AS:394-1068 (675) | settings | 29 controls (section 8.7) | — | — | prefs, AI settings, tutorial state, UVM | — | — | `cloudPolicyMessage` (unreachable) | — | diagnostics text only |
| `UserProfileScreen` P:96-532 (437) | profile | Header, editor, 4 stats, 6 skill bars, 5 links (section 8.6) | "Edit Profile" → "Save Changes" | Links, "Sign Out" dialog (P:125) | params; MVM `updateUserProfile` | — | — | snackbar (MVM:258-262) | — | n/a |
| `ActivityDiscoveryScreen` ADS:81-598 (518) | activity_library(/{id}) | "SpeakCraft Universal Activity Engine"; tabs "All 150 / Daily Plan / 10 Paths / Saved & Recent / Analytics" (ADS:74-78) | Open activity | Filters, "Load More", path start, **Swap (a visible no-op, ADS:112-114)** | AEVM plus local catalog | — | ADS:312, 497 | silently falls back to local data (AEVM:129-133) | implicit local fallback | n/a |
| `ActivityScreen` UA:66-476 (411) | activity/{id} | Header, objective, template panel (ETR:80-451), confidence slider, structured feedback | "Start Exercise" → "Submit for Evaluation" → "Continue Next" (UA:322, 378, 443) | "Retry", bookmark, "Browse All 150 Activities" | AEVM | **blank while loading** (UA:139) | "Activity Unavailable" (UA:115-133) | — | — | handled with a visible message in ETR (ETR:110-124) |

**Flags: hardcoded, sample, placeholder and fabricated data** [V unless marked]

- **No `TODO` or `FIXME`** in `ui/` (grep count 0). No "coming soon" or lorem in the screens [V].
- **Dashboard fallbacks:**
  - `overallScore` 62 (D:181).
  - Pronunciation trend 65 with a single fake point next to "0 sessions" (D:238, 950).
  - Total daily tasks 6 (D:189).
  - `totalCoreLessons.coerceAtLeast(9)` (D:193).
  - Static KPI pills "Weekly Goal: 5 h", "↑ Start learning", "↑ Improve fluency", "Today's Goal" (D:766, 777, 788, 799).
  - A fixed 6-row fallback plan of 34 minutes regardless of the user's goal (W:520-525).
  - The ring draws a 28° arc even at 0 done (W:821).
  - "Local AI" badge always shown (W:1129).
- **New users see "1 Day Streak"** because the profile is seeded `streakDays = 1` (REPO:494) and the display is floored at 1 (W:703; D:435).
- **Hardcoded counts in copy** [V]:
  - "150 drills • 10 modules • 10 paths" (D:296); "All 24 Features" (D:637); "15 @askvinh modules" (D:647); "13 Conversation Simulator roles" (AO:211); "150 … 25 reusable templates • 9 AI evaluators" (ADS:150-155).
  - Copy that does not match the code: **"9-step"** routine where 8 steps exist (CAT:170 vs CAT:989-998); **"5 Vocal Foundations"** where 4 exist (LM:636 vs LM:534-541).
- **Analytics and charts:**
  - Analytics shows fallback skill numbers 62/65/60/68/58/66 as real values (AS:103-108).
  - `ScoreTrendSparkline` invents a 60 → 62 trend with fewer than 2 points, and its accessibility text reads "latest 62 out of 100" (CC:414-417).
- **Fabricated or misleading AI signals:**
  - ASR confidence is fabricated: 0.85 by default, 0.94 after transcription (SVM:129, 193), and passed to the evaluator (SVM:451, 526).
  - Lab-specific grading instructions are dropped by the local engine except an `"Expected:"` marker that no screen sends (LIO:257-260, 938-943) [V]. So "Compare AI" never compares against the target phrase.
  - YouTube timestamps are invented (LIO:881).
  - Cloud lessons are forced to `sourceVerified = true` (LM:98, 107, 276, 285).
- **Admin defaults:** the release-notes field is pre-filled with launch-animation marketing copy (ADM:272, 275); `downloadUrl "https://"` (ADM:277).
- **AppUpdate copy:** hardcoded "v1.1.0-beta.1" and "v1.0.0"; CI secret name `FIREBASE_SERVICE_ACCOUNT_JSON` shown to end users (AU:295, 304, 373).
- **Learning snapshot defaults** that are never collected in any UI: `targetLevel = ADVANCED`, `preferredPracticeTime = "Morning (09:00)"`, `interests = ["Leadership","Workplace","Storytelling"]` (`data/learning/LearningModels.kt:379-383`) [V].

---

## 3. First-time and returning user flow (as implemented)

### 3.1 Step by step

1. **Process start.**
   - `MainActivity` configures App Check, calls `enableEdgeToEdge()` with default auto-styling, then calls `setContent { SpeakCraftRoot() }` (MainActivity.kt:14-19) [V].
   - The window background is `#FAF9F6` in light mode and `#0F172A` in night mode (res/values/colors.xml:10; res/values-night/colors.xml:3). The API 31+ splash uses the same background with an icon background of `#0F172A` even in light mode (res/values-v31/themes.xml:5-6; values/colors.xml:11) [V].
   - The Compose background is `#F4F5FB` / `#100C2A` (Color.kt:6, 14), so there is a **visible color jump from splash to Compose** in both modes [V values; I visual].
   - There is no core-splashscreen API [V].
2. **Launch overlay** (LA:251-658; NAV:264-283).
   - Drawn over the auth screen or the app.
   - Before DataStore loads, a plain background box is shown (NAV:266-273).
   - Cadence: **2900 ms on first launch**, **2100 ms on returning launches**, **600 ms with reduced motion** (LA:71-75) [V].
   - First launch is marked device-wide as soon as prefs load (NAV:231-242) [V].
   - Not skippable. There is no pointer-input block, so touches likely pass through to the auth screen [I].
3. **Auth** (`currentUser == null`, NAV:249-250).
   - Google sign-in only, via Credential Manager: silent authorized-account auto-select (AH:58-62), then interactive `GetSignInWithGoogleOption` (AH:98) [V].
   - No email/password or anonymous sign-in [V].
   - The button is disabled only while `isLoading`, not while `isCheckingSession` (AO:254) [V].
4. **Shell bootstrap.** Five ViewModels are created (NAV:317-403). `recordUpdateCheck(now)` is written on every shell start, so "Checked: …" on the update screen does not mean a check happened (NAV:313-315) [V].
5. **Profile gate** (NAV:468-503):
   - Loading → spinner with "Preparing your speaking studio...".
   - Error → dead-end text.
   - `profile == null || !onboardingCompleted` → onboarding.
6. **Onboarding:** 3 steps (section 3.2).
7. **Post-onboarding.**
   - On success the user is auto-enrolled in "30_day_challenge" and the @askvinh knowledge base is seeded (MVM:228-230) [V].
   - The profile snapshot updates, and the NavHost renders the dashboard.
8. **First dashboard visit.**
   - The **auto core tour** (7 steps) shows if prefs and tutorial state have loaded, tutorials are enabled and the core key is not completed (NAV:617-622) [V].
   - The update dialog is suppressed while any overlay, help dialog or drawer is open (NAV:646-655) [V].
   - Notifications and the learning snapshot load (NAV:546-584), and a welcome alert is created (NMR:74-122) [V].
9. **First visit to 8 routes** (`speaking_lab`, `ai_mentor`, `daily_practice`, `conversation`, `vocabulary`, `voice_training`, `public_speaking`, `analytics`; TO:119-128) shows a single-step "Module Quick Guide" spotlight. The inline `FeatureGuideStrip` on the same screen repeats the same content [V].

**Returning user:**
- 2.1 s launch overlay → Firebase persisted session (no auth UI) → profile loading → dashboard.
- The core tour is not repeated.
- Greeting "Welcome back, {name}" (W:164). This is **the same greeting a brand-new user sees** right after onboarding [V].
- There is no time-of-day greeting (no `LocalTime`/`HOUR_OF_DAY` in `ui/`) and no "since your last visit" summary. The `personalizedCue` text is computed but never rendered (D:214-226) [V].

### 3.2 What onboarding collects and where it is stored

| Step (AO:413-417) | Heading | Fields (exact labels, options, defaults) |
|---|---|---|
| 1 "Identity & Speaking Level" | "Welcome to SpeakCraft" (AO:427) | "Your Name": default Google display name, else "Speaker" (AO:350, 439). "Current English Speaking Level": Beginner / Intermediate / Upper-Intermediate / Advanced, default Intermediate (AO:351, 450). Selecting a level **overwrites all 7 sliders** with presets (AO:362-382) |
| 2 "Coaching Goal & Cadence" | "Set Your Goal & Rhythm" (AO:475) | "Primary Communication Goal": multi-line, default "Master vocal variety, eliminate filler words, and speak persuasively" (AO:352). That default matches none of the 4 preset chips (AO:389-392). "Daily Practice Commitment" chips 10m/15m/25m/45m (AO:509-513) **plus** a duplicate "Daily Practice Goal" slider 5–60, default 15 (AO:524-529) |
| 3 "Baseline Calibration" | "Calibrate Your Skill Baseline" (AO:537) | 7 sliders (range 20–95 unless noted): Self-Rated Fluency 60, Pronunciation Clarity 62, Active Vocabulary 64, Spoken Grammar 68, Articulation & Structure 58, Listening Comprehension 65, Speaking Confidence (Self-Assessment) 55 with range 10–95 (AO:555-561) |

**Behavior and persistence:**
- **Validation:** none. A blank name or goal is accepted, and the repository falls back to "Speaker" / "Speak with vocal authority" (REPO:484-485). Submit has no loading or disabled state (AO:599-618) [V].
- **Skip:** none [V].
- **Storage:** NAV:500-502 → MVM:201-234 → `SpeakCraftRepository.createOrUpdateProfile` (REPO:469) → Firestore **`users/{uid}`** (`set` for a new document, `update` for an existing one; REPO:482, 486-528) [V]. Fields stored:
  - userId, displayName (≤ 100), communicationLevel, primaryGoal (≤ 200)
  - onboardingCompleted = true, dailyGoalMinutes (5–240)
  - **streakDays = 1**, lastPracticeDate, totalPracticeMinutes, wordsLearnedCount, recordingsCount, completedExercisesCount
  - fluency / pronunciation / vocabulary / grammar / articulation / listening scores, confidenceSelfRating
  - createdAt, updatedAt
- **Failure path:** the error goes to `_actionMessage` (MVM:232). The only `SnackbarHost` lives in the post-onboarding Scaffold (NAV:695), so **the error is invisible during onboarding** [V code; I effect].
- **Downstream misuse of the self-ratings:**
  - The self-ratings become the "Objective Speech Skills" bars in Analytics (AS:101-110, 280-292) and the profile "Communication Skill Profile" (P:465-478) until sessions update them. Self-assessment is therefore shown as if it were measured [V/I].
  - Confidence is collected but never shown anywhere [V].
- **Mismatches with the profile editor** [V]:
  - Onboarding offers 4 levels; the profile editor offers 5 (adds Elementary) and spells "Upper Intermediate" without the hyphen (P:75-81, 332). An onboarded "Upper-Intermediate" user therefore sees no level chip selected.
  - The goal chip sets differ (AO:389-392 vs P:83-92), and the profile has no free-text goal.
  - Minute options differ: 25 has no profile chip, and 60 is coerced (P:94, 119).

### 3.3 Prior AI-agent prompt (`public/assets/aistudio/Paste_November_19__2026_-_7_30PM_80bb0484`, 1,204 lines): was it implemented?

| Prompt requirement (prompt line) | Status | Evidence |
|---|---|---|
| Phase 1.1 welcome-back animation with avatar, track, day, score and personalized message (43-90) | **PARTIALLY_IMPLEMENTED** | `WelcomeBackBanner` is one `Text` with a 520 ms fade/slide, saveable once per session (W:127-189). The parameters `learningTrack`, `learningDay`, `overallScore`, `personalizedCue` and `onAvatarClick` are accepted but unused (W:130-135). No avatar. `personalizedCue` computed and discarded (D:214-226). The banner respects `reducedMotion`, but NAV never passes it, so it is always `false` (NAV:916-946 vs D:160) [V] |
| Phase 1.2 learning-plan carousel cards (track, day, score, title, description, Start Lesson, Quick Practice, illustration, indicator) (92-115) | **IMPLEMENTED (content mostly static)** | `SpotlightHeroCard` (W:191-476): 4 slides (W:205-243), "Score N" chip (W:368), "Start Lesson" and "Quick Practice" buttons (W:398-441), dots (W:453-471). The **same** `img_coach_hero` image appears on every slide (W:284-289). Slides 1–3 are the prompt's example copy verbatim. Only slide 4 is personalized [V] |
| Phase 1.3 swipe via HorizontalPager, dots not primary (117-128) | **IMPLEMENTED** | `HorizontalPager` + `rememberPagerState` (W:247-260); scale/alpha transition gated by reduced motion (W:264-265); dots are also tappable at 7dp (W:467-471) [V] |
| Phase 2 personalized daily plan engine (132-272) | **PARTIALLY_IMPLEMENTED (UI)** | The dashboard reads `learningSnapshot.dailyPlan` (D:186) and renders "Today's Practice Plan" (W:487-694). Completion is a **self-reported tick** (W:639), not a validated activity event, contrary to prompt lines 312-318. Target level, interests and preferred time are never collected in any UI and use hardcoded defaults (LearningModels.kt:379-383) [V] |
| Phase 3 real-time dashboard, 12 sections (274-320) | **PARTIALLY_IMPLEMENTED** | Present: welcome, today's plan, completion ring, streak, practice time KPI, recording KPI, vocabulary KPI, trend chart, recommended action (hero slide 4). Missing: "Recent achievements" (`achievements` exists in the snapshot but no UI reads it; grep), "Continue learning" (0 hits), a current-level widget beyond the hero badge, and a weekly activity *chart* (only a 5-day dot row, W:887-984) [V] |
| Phase 4 streak engine UI: current, longest, calendar, milestones (322-361) | **PARTIALLY_IMPLEMENTED** | Streak card shows current and "Best: N d" plus the latest milestone badge (W:703-760). There is no calendar beyond the weekly dots. The streak shows 1 for new users (REPO:494; W:703) [V] |
| Phase 5 notification center (363-416) | **PARTIALLY_IMPLEMENTED** | `NotificationDrawer` has bell, badge, read/unread, mark-all, dismiss, pin, filters and preferences (ND:96-400). **No OS notifications or permission** (no `POST_NOTIFICATIONS` in MAN; no NotificationCompat/WorkManager/FCM in main). It is not a "screen". A duplicate dead sheet exists (NC) [V] |
| Phase 6 profile and settings (418-546) | **PARTIALLY_IMPLEMENTED** | Profile has initials (no picture), no "member since", no completed-activities count. Settings has no account section (no password, security or **delete account**: MVM:474 has 0 callers), no export, no About/Terms/Privacy (0 hits), no font scaling or animation preferences (`setReducedMotion`/`setFontScalePreference` exist in PR:241-251 with 0 callers), and **cloud AI settings are present** although the prompt said not to add them (AS:650-713) [V] |
| Phase 7 admin console, 13 modules (548-583) | **PARTIALLY_IMPLEMENTED** | 4 tabs only: YouTube Sync, Moderation, Releases, Telemetry (ADM:108). No user statistics, audit logs or role management [V] |
| Phase 8/9 vocabulary lifecycle (stages 1–5), quiz, flashcards, notebook (585-891) | **PARTIALLY_IMPLEMENTED** | `VocabularyScreen` has 4 tabs over a 20-word bank (VO:109; CAT:544+). `LocalVocabularyQuizEngine` is referenced **nowhere** outside its own file (grep). The snapshot's `notebook`, `spacedRepetition` and `quizHistory` are not read by any UI. Only the `wordProgress` count is (D:199; P:160) [V] |
| Phase 12 Room / local DB (310, 997+) | **NOT IMPLEMENTED** | No `androidx.room` or `@Entity` anywhere (grep) [V] |

### 3.4 What the dashboard shows (phone order, D:327-674) and density

0. `UpdateBannerCard`, if an update is available → `app_updates` (D:335-345).
1. Header: `WelcomeBackBanner` "Welcome back, {name}" plus the search field "Search lessons, drills, tools..." (D:348-356, 692).
2. Search results with "Search Results (N)" or the empty card "No matching modules" / "Clear Search", shown only while typing (D:359-382).
3. `SpotlightHeroCard`: a 4-slide pager, 252dp fixed height (D:485-493; W:282).
4. `StreakHighlightCard` "{n} Day Streak" (W:740).
5. `TodaysPracticePlanCard` "Today's Practice Plan" "x / y completed" (W:570, 587).
6. KPI strip, 2×2 on phone: "Practice Hours", "Words Learned", "Recordings", "Lessons Completed x / 12" (D:512-518, 808-858).
7. "Today's Progress" ring "x / y Tasks" with links Learn / Practice / Review / Reflect, plus "Weekly Goal" "x / 5 days" (D:540-552; W:852-855, 931-937).
8. `InlineAiMentorCard` "AI Mentor": input plus 3 chips (W:1113, 1223-1225).
9. "Quick Access", 8 tiles (W:1277-1284).
10. "My Learning Tracks", 4 cards (filter only) plus "View All" (W:1435; D:571-578).
11. "Latest Recording Review", only if sessions exist (D:605-613).
12. "Pronunciation & Clarity Trend" with Weekly/Monthly chips (D:615-622, 948).
13. `FeatureGuideStrip` plus "Explore Tutorials for All 24 Features & Benefits" (D:626-640).
14. "All Communication Modules & Tools" / "Browse all 15 @askvinh modules & coaching utilities" with 5 chips (D:643-664).
15. `ModuleGrid`: 15 modules plus 7 tools, plus 1 for admins = 22 or 23 tiles (D:285-307, 667).

**Density counts:**
- Up to **16 top-level items** and **about 21 distinct widgets** [V].
- **About 70 tappable targets** [I, counted from the spans above].
- **25 distinct route strings** (24 for non-admins) covering 20 destinations [V].
- Not reachable from the dashboard: `settings`, `profile`, `activity_library/{moduleId}`, `activity/{id}` [V].
- Phone scroll estimated at about 4,000dp, roughly 5–6 screens [I].

**Redundancy:** three "today" widgets (streak, plan count, ring count), two weekly-goal notions, and four navigation surfaces (hero, Quick Access, tracks, directory). Quick Access duplicates 7 directory tiles exactly [V].

**Tablet:** EXPANDED (≥ 840dp) is a two-column dashboard with a 1280dp max width (D:384-481, 323). MEDIUM keeps the phone order with 4-up rows (D:517, 522-539, 566, 574) [V].

**Semantic mislinks** [V]:
- Hero slide 1, "The 3 Vs of Communication", opens the "3-Stage Self-Awareness Audit" lesson (W:209-212 vs CAT:233-235). The actual 3Vs seed is `vinh_3vs` (CAT:189).
- Slides 2 and 3 say "Start Lesson" but open labs (W:220-221, 229-230).

---

## 4. ViewModels

### 4.1 Inventory, construction and scoping

| ViewModel | Lines | Constructed | Scope | Responsibilities |
|---|---|---|---|---|
| `MainCoachViewModel` | 487 | `viewModel(key="main_$uid", factory=viewModelFactory{initializer{…}})` (NAV:317-330) | Activity (created outside NavHost) | Profile and onboarding, 15 Firestore observers (MVM:100-152), recommendation (MVM:154-170), daily steps, reflection, lessons, programs, vocabulary SRS (MVM:344-374), custom words, vocab AI eval, real-world tasks, session delete, history clear, account delete (dead, MVM:474), admin role (MVM:142-148), announcements, knowledge-base seeding (MVM:187-191) |
| `SpeakingAndLabsViewModel` | 621 | NAV:332-348 | Activity | Recording plus countdown, live transcription, media upload (≤ 19 MB held in memory), camera attach, playback, transcription, evidence gate plus evaluation (SVM:445-556), save/retry/delete, AI-settings mirror |
| `InteractiveCoachViewModel` | 552 | NAV:350-366, holds `appContext` | Activity | Conversation sim, voice loop, debrief plus save, mentor chat plus file persistence, YouTube audit plus Firestore (5 domains) |
| `UpdateAndAdminViewModel` | 432 | NAV:368-386 (9 dependencies) | Activity | OTA release eval, check, download, verify, install; backups and restore; admin YouTube sync plus **AI extraction**; moderation; lesson, challenge and release publishing (UVM:70-431) |
| `ActivityEngineViewModel` | 589 | NAV:388-403 | Activity | 150-activity engine: catalog, Firestore progress plus local shadow, attempt state machine, timer, recording, evaluation, daily plan, paths, bookmarks |
| `LaunchExperienceViewModel` | ~30 | `viewModel()` default (NAV:213); defined in a *component file* (LA:219-248) | Activity | Overlay visibility, latched cadence, stage (stage is read only by tests) |

**Construction pattern** [V]:
- Factories are created inline in the composable. `checkNotNull(this[APPLICATION_KEY])` is evaluated and discarded (NAV:321, 336, 354, 372, 392).
- All VMs are Activity-scoped and keyed by uid. None is scoped to a nav back-stack entry, so every VM lives for the whole session regardless of which screen is open.
- **Sign-out:** VMs for previous users are never cleared from the Activity's `ViewModelStore` [I].

**AppContainer lifecycle** [V]:
- `AppContainer` is built in `remember(context.applicationContext)` inside `SpeakCraftRoot` (NAV:216), and there is no `Application` subclass (MAN:17-25; grep).
- A configuration change recreates the Activity and composition, and with them a **new AppContainer**.
- The Activity-scoped VMs keep referencing the *first* container's singletons (repository, `AudioRecorderManager`, `LocalIntelligenceOrchestrator`, …).
- Meanwhile NAV reads `learningProgressManager`, `notificationManager`, `tutorialStateManager`, `aiSettingsRepository` and `intelligenceCapabilityManager` from the *new* container (NAV:302-303, 427-437).
- DataStore is protected by `preferencesDataStore` delegates (PR:22; TutorialState.kt:11) [V].
- Consequences: duplicated heavyweight objects, and two in-memory notification and learning stores for the same files after a rotation [I].

**State holders outside the ViewModels** [V]:
- `LearningProgressManager` and `NotificationManagerRepository` are mutated directly from composable lambdas (NAV:546-593, 934, 1308-1315).
- NAV computes "session completed" alert scores inline (NAV:561-583). This is business logic in the navigation composable.

### 4.2 State model

- **All five feature VMs expose `StateFlow`** (lines containing `MutableStateFlow` / `stateIn` / `fun` / `viewModelScope.launch`) [V]:
  - MVM: 3 / 10 / 17 / 16
  - SVM: 17 / 4 / 18 / 9
  - ICVM: 15 / 3 / 14 / 13
  - AEVM: 11 / 8 / 25 / 6
  - UVM: 7 / 7 / 18 / 15
- There is no single per-screen `UiState`. Screens collect many independent flows; `SpeakingLabScreen` collects 22 (SAC:111-133) [V].
- `SpeakingAndLabsViewModel` exposes `audioManager` publicly, and screens call it directly (SVM:45; SAC:437, 447) [V].
- **NAV collects all MVM flows at the shell level** (NAV:405-426), so all 15 Firestore listeners stay active on every screen [V code; I cost].
- **Screen-local state:** 89 `mutableStateOf` in screens and **0 `rememberSaveable` in screens** (5 in components: W ×3, FT ×2) [V]. Topic, selected drill, challenge, lesson, tab, period, admin forms, API-key draft and reflection text are all lost on rotation [V].
- **Process death:** **0 `SavedStateHandle`** in the codebase [V]. Lost on process death [V]:
  - A cloud-failed session held only in `pendingUnsavedSession` (SVM:543).
  - The download state.
  - Conversation turns (in memory only, ICVM:88).
  - Mentor history *is* persisted, to `filesDir/mentor_history_<uid>.json` (ICVM:156-188).

### 4.3 Lifecycle and coroutine problems

| Problem | Evidence |
|---|---|
| Activity attempt wiped on rotation: `LaunchedEffect(activityId){ vm.loadActivity }` resets all state; the manifest has no `configChanges` | UA:76-78; AEVM:206-233; MAN:26-30 [V] |
| Recording continues across navigation; there is no `DisposableEffect` or lifecycle stop in SAC/LAT/LM. The next lab shows "Stop" with a LIVE waveform | sub-audit [V] |
| Labs can evaluate another lab's recording, because `clearAnalysis()` leaves `audioManager.recordedFilePath` | SVM:594-613, 466 [V] |
| Voice Training and Speaking Lab share one result slot (`"SPEAKING_LAB"`) | LM:601-602; SAC:663, 732 [V] |
| Analysis job never stored, so `cancelEvaluation` cannot cancel it and stale results overwrite state | SVM:454 [V] |
| Submit buttons stay enabled during Loading, allowing duplicate evaluations and saves | SAC:508, 960; LAT:197, 419, 608, 795 [V buttons; I effect] |
| Shared `AudioRecorderManager` and `LiveTranscriptionManager` singletons are **released** in `SVM.onCleared`, while AEVM uses the same instances | SVM:615-620; NAV:340, 397; AC:63, 85 [V] |
| AEVM timer keeps running off-screen; `todayDateKey` is frozen at construction | AEVM:570-588, 100 [V] |
| MVM `dailyVocabularyBatch` is `lazy`, so it goes stale after midnight (`todayDateKey` is a getter) | MVM:77-78, 172-179 [V] |
| ICVM `onCleared` launches on the cancelled `viewModelScope` | ICVM:306-316, 548-550 [I] |
| Main-thread file I/O: ICVM history (ICVM:114, 176-188); SVM reads up to 19 MB (SVM:173, 466); notification store `commitLocked` (NMR:434-469) | [V/I] |
| UVM `adminSyncBusy` has no `finally`; the admin sync ignores write `Result`s; "check" auto-downloads | UVM:347-386, 364-384, 216-226 [V] |
| `MainCoachViewModel.cloudAiEnabled` uses `SharingStarted.Eagerly`; SVM has 4 eager derived flows | MVM:68-69; SVM:71-91 [V] |
| Activity Engine "Swap" is a visible no-op: `dailyPlan` is remembered with keys that a swap doesn't change | ADS:112-114; AEVM:504-505, 521 [V] |

### 4.4 Duplicated logic across ViewModels

| Duplication | Evidence |
|---|---|
| Two recording/evaluation pipelines | SVM `submitForAiAnalysis` vs AEVM `startAudioRecording`/evaluate (AEVM:270-276) [V] |
| Two feedback renderers | `FeedbackReportCard` CC:1326-1842 vs `ActivityStructuredFeedbackCard` ETR:645-787 [V] |
| YouTube audit in two places | ICVM:485-533 vs UVM:345-388 [V] |
| Conversation turn generation in two places | ICVM:204-230 vs AEVM:304-341 [V] |
| Session delete in two places | MVM:455 vs dead `SVM.deleteSavedSession` [V] |
| Overall score in two places | D:180-185 vs P:145-148 [V] |
| Words learned in two places | D:199-204 vs P:157-161 [V] |
| Streak calculated differently | W:703 vs P:153-156, so the two screens can disagree [V] |
| Cloud-lesson mapping duplicated within LM | LM:90-111 vs 268-288 [V] |

**Deprecated constructors** in MVM:53-62, SVM:52-61, ICVM:51-64 and UVM:49-68, plus an unused `geminiCoach` parameter in AEVM. They are **used only by tests** (9 `GeminiCoachService()` in `src/test`, 0 in `src/main`) [V].

**Dead VM API** [V]:
- `MVM.cloudAiEnabled`, `MVM.setCloudAiEnabled`, `MVM.deleteAccountAndAllData`.
- SVM: `currentSessionId`, `liveTranscriptConfidence`, `capabilityMatrix`, `cloudAiEnabled`, `setCloudAiEnabled`, `clearTranscriptionError`, `deleteSavedSession`, `onSessionCompleted`.
- ICVM: `mentorAnswer`, `cloudAiModelTier`, `setCloudAiEnabled`, `startCloudLiveVoiceCall`.

---

## 5. Design system and theming

### 5.1 Tokens defined (`ui/theme`)

- **Color.kt (120 lines):** about 85 `Color(0x…)` constants. They form the "Violet Studio" palette: canvas and surfaces, violet primary, sky, mint, amber and rose semantics, sidebar gradient, hero overlays, and **8 pastel swatch pairs × light/dark**. Aliases `AcousticTeal = SkyAccent`, `AmberSpotlight = WarmAmber`, `EmeraldSuccess = MintSuccess` (Color.kt:116-118) [V].
- **Theme.kt (249 lines):**
  - `lightColorScheme` / `darkColorScheme` define primary, secondary, tertiary (plus containers), background, surface, surfaceVariant, outline and error (Theme.kt:174-224) [V].
  - `SpeakCraftExtendedColors` holds 48 roles (success/warning/info, hero, camera, sidebar, streak, 16 pastel roles) and is provided through a CompositionLocal (Theme.kt:17-67, 167-172, 241) [V].
  - **Not defined:** `surfaceContainer*`, `outlineVariant`, `inverseSurface`, `scrim`, `surfaceTint`, `surfaceDim`/`surfaceBright`. These fall back to Material 3 baseline (purple-grey) values [V for absence; I for visual].
  - `outlineVariant` is used 3 times (ND:357, 464, 683). Two of the 4 `AlertDialog`s do not override `containerColor`, so they render on baseline `surfaceContainerHigh` [I].
- **Shapes:** small 14, medium 20, large 24, extraLarge 30dp. `extraSmall` keeps the default (Theme.kt:226-231) [V].
- **Spacing.kt:** `xs 4, sm 8, md 12, lg 16, xl 24, xxl 32, minTouchTarget 48`, max widths 840/880/1160/520, and the window tiers COMPACT / MEDIUM (≥ 600) / EXPANDED (≥ 840) (Spacing.kt:18-47) [V].
- **Type.kt** [V]:
  - Three bundled **single-weight (400) static TTFs**: Plus Jakarta Sans, DM Sans, JetBrains Mono (res/font; Type.kt:15-25). Bold and SemiBold are **synthesized** (Type.kt:9-14) [V; faux-bold quality I].
  - Defined styles: displayLarge 34, headlineLarge 26, headlineMedium 21, titleLarge/Medium/Small 18/16/14, bodyLarge/Medium/Small 16/14/12, labelLarge 14, labelMedium 12, **labelSmall 11 in JetBrains Mono** (Type.kt:27-105).
  - **Undefined:** displayMedium, displaySmall, headlineSmall, which fall back to the M3 default with `FontFamily.Default`. `headlineSmall` is used 6 times in `ui/` [V].
  - `labelSmall` (monospace) is used **103 times** (grep), so most chips, badges and meta text render in a code font [V].
- **Light / dark / system:** `ThemePreference` SYSTEM/LIGHT/DARK from DataStore (NAV:221-225), set in Settings "Appearance" (AS:537-544) [V].
- **Dynamic color (Material You): not supported.** No `dynamicLightColorScheme` (Theme.kt:233-249) [V].
- **System bars:** `enableEdgeToEdge()` with the default auto style follows the *system* night mode, not the in-app theme choice (MainActivity.kt:17). With the app forced dark on a light system, the status-bar icons will likely be dark on dark [I].
- **Resources:**
  - `strings.xml` has only 4 strings (app_name plus 3 launch strings) (res/values/strings.xml:2-5).
  - `stringResource` is used only in `LaunchAnimationOverlay` (LA:324-327).
  - About **386 hardcoded English `Text("…")`/`text = "…"` literals** in `ui/` (grep, lower bound). No localization is possible [V].
  - Leftover template colors `purple_*` / `teal_*` have 0 references (colors.xml:3-7) [V].
  - The XML theme parent is `android:Theme.DeviceDefault.NoActionBar` (themes.xml:3) [V].

### 5.2 Token consistency (grep counts over `ui/`)

| Metric | Screens (15 files) | Components (9 files) | AppNavigation | Total |
|---|---|---|---|---|
| `Color(0x` literals | 0 | 0 | 0 | **0** (85 in theme) |
| Literal `N.dp` | 682 | 656 | 48 | **1,386** |
| Literal `N.sp` | 0 | 2 (LA) | 0 | **2** (26 in theme) |
| `MaterialTheme.colorScheme.` | 416 | 357 | 19 | 792 |
| `speakCraftColors` | 38 | 41 | 3 | 82 |
| Raw palette constants (`AcousticTeal`/`AmberSpotlight`/`EmeraldSuccess`), which bypass dark mode | 25 | 0 | 0 | 25 (YT, LM, DP, ADM) |
| `SpeakCraftSpacing.` | 378 | 74 | 0 | 452 |
| Raw `RoundedCornerShape(` | 49 | 91 | 7 | **147** |
| `MaterialTheme.shapes` | 107 | 37 | — | 144 |
| `fontWeight = FontWeight.` overrides | — | — | — | 95 |
| `.height(N.dp)` | 24 | 18 | 2 | 44 |

**Per-file literal dp:** CC 163, W 154, ETR 82, AS 76, ND 62, CM 60, SAC 57, LM 57, P 51, ADS 48, TO 48, LAT 46, AU 45, FT 45, ADM 42, NC 39, AO 35, UA 35, D 34, LA 34, YT 34, VO 33, DP 29, UD 29, NAV 48 [V].

**Assessment.**
- Colors are well tokenized, apart from the 25 raw palette references.
- Spacing tokens are used for outer padding, but card internals use ad-hoc 6/10/12/14dp.
- Radii are mostly raw literals.
- Typography weights are overridden ad hoc 95 times.
- The magic `54.dp` button size appears at CM:869 and UA:317, 373 [V].

---

## 6. Accessibility

| Check | Finding |
|---|---|
| `contentDescription = null` | **127** total (screens 70, components 54, NAV 3) out of **195** `Icon(` calls and 3 `Image(`. Most are decorative icons next to text. Problems: icons that repeat adjacent text, so TalkBack reads it twice (D:1013; W:1362; CC:1064/1069, 1257, 1428, 1911, 1935); state-only icons with no text (plan circle W:623-653, weekly dots W:896, 968 "Day completed" with no day name) [V] |
| Per-file null counts | W 14, AU 12, CC 11, CM 9, SAC 9, LM 7, TO 7, ETR 6, AS 5, P 5, UA 5, FT 5, ND 5, AO 4, LAT 4, UD 3, NC 3, NAV 3, ADS 2, ADM 2, VO 2, YT 2, D 1, DP 1, LA 0 [V] |
| Headings | Only **6** `heading()` uses in all of `ui/`: AO ×4 (AO:141, 429, 477, 539), `SectionHeader` (CC:143, used about 40 times), TutorialOverlay title (TO:339). Dashboard card titles, settings card headers (AS:387), analytics card titles, drawer title (ND:205), lesson sections and activity sections have no heading semantics. On phone layouts the lab drill lists have no heading at all (LAT:307, 512, 700, 887) [V] |
| Roles and state | Only **2** `Role.` uses in `ui/`. Clickable Rows, Boxes and Surfaces have no role: plan rows (W:614) with a nested 24dp tap target (W:639), carousel dots (W:468), ring links (W:862), Quick Access tiles (W:1352), track cards (W:1471), module tiles (D:994), profile rows (P:591), ETR checklist rows with no checkbox semantics (ETR:400-417). Selected cards show selection by color only (LAT:283, 310, 488, …). FilterChips used as action buttons (mentor quick prompts, CM:724-726) are announced as toggles [V/I] |
| Labels | Settings and drawer switches are not merged with their labels via `toggleable`, so TalkBack announces a bare "Switch" (AS:624-645; ND:740-749). Sliders have no label semantics (AO:660; AS:548-553; LM:570-571; ETR:634). The loading shimmer announces nothing (CC:862-868). There is no `liveRegion` for streaming chat or errors (CM) [V/I] |
| Touch targets | Undersized: carousel dots 7×7 / 20×7dp (W:467); plan check 24dp (W:625); tutorial step dots 8dp (TO:556-566); drawer action pill about 26dp (ND:563-588); ring links about 22–26dp (W:859-863) [V sizes; I computed heights]. Top-bar actions do enforce 48dp (NAV:756, 784, 801). AdminDashboard uses `minTouchTarget` 0 times [V] |
| Fixed heights that break with font scaling | Hero `Box.height(252.dp)` holding a badge row, 2-line headlineLarge, 2-line subtitle and a 48dp button row (W:282). Flashcard `.height(270.dp)` holding displayLarge text plus meaning plus example (VO:241). Score ring `size(72.dp)` with titleLarge plus "/100" (CC:1505-1537). Today ring 96dp with 2 lines (W:805-844). Waveform 78dp with badge (CC:322). Video box 210dp with overlay text (CC:553). 64dp avatar with headlineSmall initials (P:207-216) [V code; I clipping]. No font-scale test exists |
| Reduced motion | `rememberReducedMotionEnabled` is true only when `ANIMATOR_DURATION_SCALE == 0`. It is read once and is **not reactive** (CC:111-124) [V]. Respected by the launch overlay, TutorialOverlay, FT, waveform, step bar, skill bars and shimmer. **Ignored by** the dashboard (NAV computes `reducedMotion` at NAV:227 but never passes it to `DashboardScreen`; D:160 defaults to false), `NotificationDrawer` (fixed 220/260ms tweens, ND:126-155), the score ring `animateFloatAsState` (CC:1371-1375), transcript autoscroll (CC:1029), `AnimatedThinkingDots` (CC:930), the vocabulary flashcard `AnimatedContent` (VO:249) and the onboarding `AnimatedContent` (AO:422). The in-app `reduced_motion` preference exists (PR:39, 119, 247-251) but has no UI and is never read [V] |
| Overlays and focus | `TutorialOverlay` and `NotificationDrawer` are drawn in-window, not as Dialogs, so there is no accessibility focus trap [I]. The launch overlay has no pointer block, so its semantics and touches reach the underlying UI [I]. The update dialog is a real `AlertDialog` that blocks Back when mandatory (UD:75-77) [V] |
| Text input | 0 `imePadding` in `ui/`; chat lists do not autoscroll to the newest message (CM) [V] |
| Localization and RTL | `supportsRtl="true"` (MAN:24), but almost all strings are hardcoded (section 5.1); `String.format` without a Locale (D:198; P:418) [V] |

---

## 7. UX problems

### 7.1 Overloaded screens

- **Dashboard:** section 3.4 (about 21 widgets, about 70 targets, 25 routes).
- **SpeakingLabScreen** (688-line composable): capture with mic, video, or picker upload; a 3-stage self-audit player; transcription; prompt presets; live transcript; analysis; history; delete. All on one screen [V].
- **SettingsScreen** (675 lines, 29 controls): engine diagnostics first on phones (AS:1057-1064).
- **ActivityDiscoveryScreen:** 5 sub-products behind one route (ADS:74-78).
- **AppUpdateScreen:** OTA plus backups plus history plus developer setup text [V].

### 7.2 How many ways to "start speaking practice"

**Voice-recording surfaces** (screens that capture the user's speech for evaluation), **10**:
`speaking_lab`, `challenges`, `pronunciation`, `articulation_lab`, `voice_training`, `listening_lab`, `grammar_speaking`, `public_speaking`, `lesson_detail/{id}` and `activity/{id}` (audio templates). `conversation` and `ai_mentor` add dictation-based voice input, for **12 voice surfaces** [V].

**Start-recording labels (12):**
1. "Record Mic" (SAC:352)
2. "Video Clip" (SAC:386)
3. "Start {N}s Timer" (SAC:943)
4. "Record" (LAT:186)
5. "Shadow & Repeat" (LAT:409)
6. "Speak Drill" (LAT:598)
7. "Record Rehearsal" (LAT:785)
8. "Record Again" (CC:1309, from 8 handler sites)
9. "Record Audio" (ETR:283)
10. "Open Camera Video Studio" (ETR:349)
11. "Practice Drill" (LM:596)
12. LessonDetail "Record" (LM:373)

Plus the import button "Attach Video from Photo Picker" (SAC:403) [V].

**Analyze labels for the same call (8):** "Analyze With Local AI Coach", "Submit AI Check", "Compare AI", "Score Shadow", "Check Grammar", "Get Presentation AI", "Evaluate Vocal Foundations With AI", "Get AI Feedback" [V].

**Stop labels (5):** "Stop", "Stop Early", "Stop Shadowing", "Stop Delivery", "Stop Recording" [V].

**Inconsistent controls:**
- Pause exists only in SAC and LessonDetail.
- Manual transcription only in SAC.
- Error retry only in some labs.
- Only 3 record buttons show a mic icon (SAC:350; LAT:184; LM:594) [V].

**Navigation entry points that lead directly to a recorder:** more than 30 across the bottom bar, sidebar, hero, plan rows, ring, Quick Access, directory, daily steps, tour, alerts and recommendations (section 1.7) [I, from the route table].

### 7.3 Overlapping features (lab-by-lab)

All 8 SVM-based recorders share **the same recorder** (`speakingVm.startRecording` → `audioManager.startRecording`, SVM:228) and **the same evaluator call** (`submitForAiAnalysis`, SVM:445-556) [V]. The local engine maps the module type only to an evaluator *name* used for next-task text (LIO:923-935). Scoring is effectively identical across labs [I, strong].

| Surface | What the user does | Pipeline | Distinct value |
|---|---|---|---|
| speaking_lab | Free speech on a preset or custom topic; video; upload; self-audit | SVM (`SPEAKING_LAB`) | Only screen with video, upload, pause, manual transcription and history |
| challenges | Timed free speech | SVM (`DAILY_CHALLENGE`) | Countdown, shared Firestore challenges |
| pronunciation | TTS phrase, then read aloud | SVM (`PRONUNCIATION`) | IPA content, but no target alignment |
| articulation_lab | Identical to pronunciation | SVM (`PRONUNCIATION`) | Header, guide key and default chip "Difficult Sounds" (1 drill) (LAT:86-88, 107-116) |
| listening_lab | TTS script, then repeat (not simultaneous shadowing) | SVM (`SHADOWING` → SpeakingEvaluator) | Script markers ignored by TTS (LAT:374) |
| grammar_speaking | Read rule, then speak | SVM (`GRAMMAR_SPEAKING`) | Rule, mistake and fix copy |
| public_speaking | Read principle, then speak | SVM (`PRESENTATION`) | Module copy; audio only despite the body-language module |
| voice_training | TTS script, then read | SVM (submits as `SPEAKING_LAB`) | WPM slider only changes TTS rate (LM:577) |
| lesson_detail | Speak on lesson prompt | SVM (`LESSON_PRACTICE`) | Lesson content and completion |
| activity/{id} | 25 templates (text, audio, dialogue, checklist) | AEVM + ETR, separate feedback card | Structured activity engine |
| conversation | Scenario role-play, text or one-shot dictation | ICVM | 13 scenarios; debrief |
| ai_mentor | Q&A coach | ICVM | Roles; persisted history |
| daily_practice | Self-ticked 8-step checklist linking to the labs above | MVM (Firestore `DailyActivity`) | Reflection and confidence |
| Dashboard "Today's Practice Plan" | Self-ticked plan linking to labs | LPM `PersonalizedDailyPlan` (local) | Drives the local streak |
| Activity Engine "Daily Plan" | Generated activity list | AEVM | Swap button is a no-op |
| learning_paths | 7 programs with day checkboxes; 6 of 7 can never complete (drills < days) | MVM | DP:353-368; CAT:1072-1121 |
| Activity Engine "10 Paths" | Activity sequences | AEVM / `LearningPathsEngine` | Different "paths" concept |
| Dashboard "My Learning Tracks" | 4 cards that only filter the module grid | — | Third "tracks/paths" concept (D:576) |
| module_lessons / lesson_detail | 12 verified lessons across 15 "modules"; `confidence` has 0 | MVM / CAT | Source-grounded lessons |
| real_world_tasks | Off-app tasks with reflections | MVM | — |
| youtube_library | Channel ingestion (section 1.8) | ICVM | Admin-like |

**Duplicate systems (summary)** [V]:
- **3** daily-plan systems.
- **3** "path/track" concepts.
- **2** module taxonomies: 15 content modules (CAT:167-182) vs 10 activity modules (`data/activity/ActivityCatalogue.kt:107-188`), plus 5 dashboard chip groups (D:643-664) that disagree with the hub grouping (D:139 vs D:253).
- **2** vocabulary stores: Firestore `VocabularyEntry` vs local `wordProgress` / `notebook` / `spacedRepetition`.
- **2** streak sources: profile `streakDays` vs LPM streak.
- **2** feedback cards.
- **2** notification UIs, one dead.
- **4** tutorial mechanisms.

### 7.4 Inconsistent terminology

| Concept | Names used |
|---|---|
| The YouTube tool | "@askvinh" (NAV:181), "YouTube Intelligence" (NAV:200, 1344), "@askvinh Library" (D:299), "YouTube Channel Intelligence" (YT:108), "YouTube Library" (LM:139) |
| Free-speech recorder | "Record" (tab), "Record & Review" (title, sidebar), "Record and Review Studio" (SAC:621), "Record and Review" (CAT:181), "Speaking Lab" (tour CTA "Open Speaking Lab") |
| Vocabulary | "Vocabulary Builder" (title), "Vocabulary Development" (grid, Quick Access), **"Tools"** (sidebar, NAV:1345) |
| Daily | "Daily" (tab), "Practice" (sidebar), "Daily Practice" (title), "Daily Communication Practice" (DP:153), "Daily Speaking Practice" (CAT:170), "Daily Plan" (Activity Engine), "Today's Practice Plan" (W:570) |
| Paths | "Learning Paths" (title), "Learn" (sidebar), "30/60/90 Plans" (D:297), "Structured Learning Programs" (DP:291), "10 Paths" / "10 Personalized Learning Paths" (ADS), "My Learning Tracks" (W:1435) |
| Grammar lab | "Thinking & Articulation" (title, CAT:178), "Thinking & Articulation / Grammar" (LAT:650), "Contextual Sentence Construction" (W:522) |
| Articulation | Lab title (LAT:108), part of the grammar header (LAT:650), a score pill (SAC:575) |
| Activity Engine | "Activity Engine (150)" (title), "150 Activities" (sidebar), "Activity Engine" (tile), "SpeakCraft Universal Activity Engine" (ADS:150) |
| Metrics | "Practice Hours" "x h" (D:764) vs "Practice Time" "x hrs" (P:417); "{n} Day Streak" vs "Active Streak"; "Score N" vs "Score N/100" |
| Profile fields | "Current English Speaking Level" vs "Communication Level"; "Primary Communication Goal" vs "Primary Goal" vs "Focus Goal"; "Daily Practice Commitment" / "Goal" / "Target" |
| Coach | "Local AI Coach", "SpeakCraft Coach", "AI Check", "Presentation AI", "Compare AI", "AI Partner", "SpeakCraft AI Mentor" |
| Admin | "Admin Console" (title), "Admin Studio" (tile), "Open Admin & Content Studio" (Settings) |

### 7.5 Confusing for a new user

- **Developer jargon on screen** [V]:
  - "CameraX video or Photo Picker (<19MB)" (SAC:623).
  - "Status: INSUFFICIENT_EVIDENCE • No score awarded" (CC:1268).
  - "Running SpeakingEvaluator..." (UA:399).
  - "EXTRACTED" / "NO_CAPTIONS" (YT:198).
  - "Local Whisper / Moonshine TFLite", "Acoustic Audio DSP Analysis", "MediaPipe" (AS:785-902).
  - "Checks Firestore applicationReleases…" and the CI secret name (AU).
- **Unexplained creator frameworks:** "Vinh Giang 3-Stage Self-Audit", "4 Ways of Listening", "5 Vocal Foundations", "4-Color Communicator", "Friendly Authority" (SAC:408; LAT:461; LM:636; CAT:180) [V].
- **"Welcome back"** on the first-ever visit; **"1 Day Streak"** on day 0 [V].
- **Labels that don't match behavior** [V]:
  - "Listen 0.75x" plays at 0.72.
  - "Shadow & Repeat" is not simultaneous.
  - "Watch @askvinh" opens the channel home page.
  - "Compare AI" doesn't compare.
  - "Card x of N due today" counts non-due cards (VO:224, 236).
  - The camera-error "Try Again" only dismisses (SAC:641).
  - The update-interrupted "Try Again" only dismisses (AU:131-138).
- **Analyze before recording:** labs allow it, then show an "insufficient evidence" card (SVM:472-497) [V].
- **History is mixed:** Speaking Lab history contains every lab's sessions with no module label, and deltas compare unrelated modules (SAC:538-610) [V].
- **Learning Programs** [V]:
  - The 30-day plan is shown expanded before enrolling.
  - Other programs cannot be previewed.
  - "Hide Plan" fails for the 30-day plan.
  - There is no way to unenroll.
  - Most programs cannot be completed (DP:278-352).
- **Track cards** silently filter a grid far below them, and that hidden filter also restricts search (D:313, 576) [V].
- **Phone-call metaphor** ("End Voice Call", Call icons) for a one-shot dictation (CM:368-373) [V].
- **Help shows the dashboard guide** on profile and activity routes (FTR:437-438) [V].

---

## 8. Status of special systems

### 8.1 Launch animation

**Status: IMPLEMENTED; polished but unskippable.**

- `SpeakCraftLaunchOverlay` (LA:251-658, a 408-line composable) plus a crest (LA:661-774) [V].
- Stages: INITIAL_APPEARANCE below 0.14, BRAND_REVEAL below 0.44, IDENTITY_REVEAL below 0.80, FINAL_TRANSITION from 0.80 (hold 0.80–0.88, dissolve 0.88–1.0), then COMPLETED (LA:117-180) [V].
- Durations: 2900 ms first launch, 2100 ms returning, 600 ms with reduced motion (LA:71-75) [V].
- Copy comes from string resources (LA:324-327) [V].
- The `forceVectorFallback` "fallback" only adds a test tag (LA:257, 515-519) [V].
- There is no skip, tap handler or `BackHandler`. Touches likely pass through [I].
- On rotation mid-animation, `Animatable` restarts from 0 (LA:263-266) [I].
- Every returning launch costs 2.1 s before the dashboard [V].

### 8.2 Tutorial and feature-tutorial system

**Status: IMPLEMENTED, but DUPLICATED across 4 mechanisms.**

1. **Auto core tour** with 7 steps (TO:109-117, 130-144). Steps and CTAs:
   1. "Home Coach Dashboard"
   2. "Record & Review Studio (3-Stage Audit)" → `speaking_lab`
   3. "Personalized Multi-Turn AI Mentor" → `ai_mentor`
   4. "Daily Speaking Practice Routine" → `daily_practice`
   5. "Interactive Conversation Simulator" → `conversation`
   6. "Vocabulary Builder & Spaced Repetition" → `vocabulary`
   7. "Progress Analytics & Mistake Patterns" → `analytics`

   Overlay buttons: "Skip Tour"/"Dismiss", "Back", "Next Feature"/"Finish Tour"/"Got It, Let's Practice", the module CTA, and step dots. A "Don't show first-use guides automatically (replay anytime in Settings)" checkbox disables *all* auto guides (TO:572-594) [V].
2. **First-use spotlight** on 8 routes (TO:119-128; NAV:624-641) [V].
3. **Help-icon `FeatureTutorialDialog`** (NAV:605-615), plus a **second instance on the dashboard** (D:172-178) that is not part of the `showCoreTour` guard, so it can stack with the overlay [V/I].
   - Tabs: "Guide Detail" and "All Features (24)" with search; buttons "Got It" / "Close" + "Open" / "Launch Tour".
   - The "Open" allowlist is hardcoded and duplicates the NavHost route list (FT:260-265).
   - 24 registry entries (FTR:16-429).
   - The `auth` entry promises "live Gemini AI coaching", which contradicts the local-only policy (FTR:29) [V].
4. **Inline `FeatureGuideStrip`** "How to use & what improves" on 23 call sites across 13 files (FT:70-174) [V].

**Persistence:** DataStore `speakcraft_tutorial_state` with per-user keys (TutorialState.kt:11, 27-31). Legacy duplicate keys in `speakcraft_prefs` are unused (PR:34-35, 114-115, 203-227) [V].

**Behavior gaps** [V]:
- Back or Skip marks the tour completed (NAV:1252-1268; TO:212-214).
- "Open module" marks the whole core tour completed (NAV:1288-1299).

### 8.3 Notification drawer and center

**Status: drawer PARTIALLY_IMPLEMENTED (in-app only); `NotificationsCenterSheet` is DEAD.**

- `NotificationDrawer` (ND:96-400) is a right-edge slide-over with a scrim and `BackHandler` (ND:111-141).
- Header "Notifications", "N new", "Practice alerts, coaching & streaks".
- Filters: All / Unread / Pinned / Coaching / Reminders.
- Actions: "Mark all read", "Clear read", and per alert open-route, pin, read/unread and dismiss (ND:557-651).
- 5 preference switches. The last one merges two flags, and `accountAlerts` has no toggle (ND:698-729) [V].
- **Alert sources:**
  - Welcome → `profile`; Daily → `daily_practice`; Streak → `speaking_lab` (NMR:74-122).
  - Session completed → `speaking_lab`, computed in NAV (NAV:561-583; NMR:250-273).
  - Admin announcements, with a free-text route (NMR:232-245).
  - Learning-layer notifications bridged in (NMR:182-227).
- **No system notifications:** no `POST_NOTIFICATIONS` permission, NotificationCompat, WorkManager, alarms or FCM. "Daily Practice Reminders" only creates items when the app opens [V].
- `NotificationsCenterSheet` (NC:79-269) plus 4 privates have **0 call sites**. It uses the older `data.learning` notification models, and its KDoc still claims it is opened from the bell (NC:72-76). `LearningProgressManager.updateNotificationPreferences` therefore has no UI (LPM:630) [V].

### 8.4 Update dialog and app update screen

**Status: IMPLEMENTED, but RISKY.**

- **`UpdateAvailableDialog`** (UD:53-332):
  - "Update Available: vX", channel chip, "vA → vB", "MANDATORY", "What's New" with up to 5 features, "Direct APK Download Link".
  - Confirm: "Download & Install", or "Download APK Link" when there is a signing conflict.
  - "Direct Link" and "Later". **"Later" is hidden when mandatory** (UD:319), and outside taps and Back are ignored (UD:75-77) [V].
  - Gating (NAV:646-655) re-shows it on every route except `app_updates`.
- **`AppUpdateScreen`** (AU:78-253):
  - Header "Application Updates & Backups".
  - Interrupted-update card.
  - "Installed Version" / "Update Channel" tiles.
  - `UpdateChannelCard` with "Stable" / "Beta (Early Access)", the **"GitHub Repo (owner/repo, optional)" field with a "Sync" button** (a developer control exposed to users), and "Check for Updates Now".
  - `LatestReleaseSection` state machine: Idle, Downloading, Verifying, VerifiedReady → "Launch Verified Installer", SigningConflict → "Download APK in Browser for Clean Install", Failed.
  - Backups with "Create Manual Snapshot Backup Now", up to 4 rows with raw "(N B)" sizes and "Restore". Restore only brings back theme, TTS rate and channel (UVM:325-337).
  - "Update Activity History" (8 events) [V].
- **Release sources:** Firestore `applicationReleases` merged with a GitHub repo whose `BuildConfig` default is an empty string (`app/build.gradle.kts:17-18, 41-45`; PR:138-139). The default channel in a default build is **"beta"** (build.gradle.kts:15-16) [V].
- **Stuck-user risk** [I]:
  - Mandatory releases (flag or `minSupportedVersion`) block the app until installed.
  - In-app install via `ACTION_VIEW` likely fails without `REQUEST_INSTALL_PACKAGES` (MAN:5-8; `data/update/ReleaseUpdateRepository.kt:313-330`).
  - The fallback guidance tells users to uninstall and sideload from a browser (AU:462-466; UD:206-210).

### 8.5 Admin dashboard

**Status: PARTIALLY_IMPLEMENTED.**

- **Who sees it:** `isAdmin = adminRole != NONE` (MVM:142-148). `adminRole` comes from `observeAdminRole` (REPO:236-270) [V]:
  1. NONE if the auth uid differs.
  2. Cached ID-token claims `role`, or `admin == true` → SUPER_ADMIN.
  3. Otherwise a live Firestore `adminUsers/{uid}` document with `active == true`, using its `role` (default SUPER_ADMIN).
  4. The claim takes precedence over the document.

  There is no email allowlist. The initial value is NONE, so admins briefly see the restricted card [I].
- **UI entry points:** top-bar icon, dashboard tile and Settings button, each shown only if admin (NAV:793-797; D:303-304; AS:506-518). The route itself is not guarded at navigation level; the screen renders "Admin Access Restricted" for non-admins, showing the uid and the `adminUsers/{uid}` path (ADM:71-90) [V].
- **Tabs** (ADM:108):
  - "YouTube Sync": URL pre-filled with @askvinh, "Publish immediately…", "Sync to Shared Catalog".
  - "Moderation": "AI Extraction Moderation Queue" (actually all techniques, REPO:424-435), plus "Publish Shared Lesson" and "Publish Shared Speaking Challenge" forms.
  - "Releases": "Publish New Release". The form never sets `signingCertSha256` or `fileSizeBytes` (ADM:319-332).
  - "Telemetry": two tiles plus 25 events, no empty states [V].
- **Security:** UI gating is cosmetic; enforcement is in `firestore.rules`. The rules' `isAdmin()` includes MODERATOR, so moderators can publish OTA APK releases (firestore.rules:17-32, 841-853) [V; flagged for the security audit].

### 8.6 Profile screen

**Status: IMPLEMENTED (basic).**

- **Header card** (P:180-296): gradient band, initials avatar, name, email, level pill, "Score {n}/100" pill, "Focus Goal: {goal} • {n} min/day", "Edit Profile" / "Close".
- **Editor** (P:299-398): "Display Name", "Communication Level" (5 chips), "Primary Goal" (8 chips), "Daily Practice Target" (5/10/15/20/30/45), "Cancel" / "Save Changes". The editor closes immediately with no inline feedback; a snackbar comes from MVM (MVM:258-262).
- **Stats** (P:401-446): "Active Streak" (renders "1 days", P:410), "Practice Time", "Recordings", "Words Learned".
- **"Communication Skill Profile"**: 6 bars (P:465-478).
- **Links** (P:485-529): "Progress Analytics", "Notifications & Alerts", "App Settings", "App Updates & Backups", "Sign Out" with dialog (P:125).
- **Missing:** photo, member-since, delete account, data export [V].

### 8.7 Settings screen: every setting

Header "Settings & Privacy" / "Signed in as {email} • SpeakCraft v{VERSION_NAME}" (AS:1006-1008).

**Phone card order:** AI Core → Speech → Recognition/Diagnostics → Video/Privacy → Updates → Appearance → Integrations → Privacy & Account (AS:1057-1064) [V].

| # | Card | Label | Control | Pref, default | Audience | Works? |
|---|---|---|---|---|---|---|
| 1 | AI & Intelligence | "Active Engine: On-Device Local Intelligence (Zero-Cloud-AI Policy)" | read-only chip | — | dev | info |
| 2 | 〃 | "A. Local Intelligence" | sub-header | — | dev | — |
| 3 | 〃 | "Local Intelligence Engine" | switch | `ai_local_enabled`, true | dev/user | yes |
| 4 | 〃 | "B. Cloud AI" | sub-header | — | dev | — |
| 5 | 〃 | "Cloud AI Coach" ("Unavailable • Zero-Cloud-AI Policy") | switch, **always disabled** | — | dev | **no** (AS:658-688; `LocalExecutionPolicy.kt:13`) |
| 6 | 〃 | `cloudPolicyMessage` | error text | — | — | **unreachable** (AS:690-696) |
| 7 | 〃 | "Balanced / Deep / Fast" | chips, disabled | `cloud_ai_model_tier`, "balanced" | dev | **no** (AS:697-713) |
| 8 | C. Speech & Transcription | "Live Transcription" | switch | `ai_live_transcription_enabled`, false | user | yes |
| 9 | 〃 | "On-Device Speech Recognition" | switch | `ai_local_speech_recognition_enabled`, true | dev | yes |
| 10 | 〃 | "Acoustic Audio DSP Analysis" | switch | `ai_local_audio_analysis_enabled`, true | dev | yes |
| 11 | (no header) | "Recognition Engine": "Automatic (On-Device Preferred)" / "Android SpeechRecognizer (On-Device)" / "Local Whisper / Moonshine TFLite" | chips | `ai_recognition_engine`, "AUTO" | dev | yes |
| 12 | 〃 | "Recognition Language": en-US / en-GB / en-AU / en-IN | chips | `ai_recognition_language`, "en-US" | user | yes |
| 13 | 〃 | "Speech & Transcription Diagnostics" | read-only panel | — | dev | info |
| 14 | D. Video / E. Privacy | "Camera & Body Language Analysis" | switch | `ai_local_video_analysis_enabled`, true | user | yes |
| 15 | 〃 | Privacy statement | text | — | user | info |
| 16 | Software Updates & Backups | `updateStatusSummary` | text | — | user | info |
| 17 | 〃 | "Stable Channel" / "Beta Channel" | chips, also fire a network refresh | `update_channel`, **"beta"** in a default build | dev | yes |
| 18 | 〃 | "Check for Updates" | button, navigates and **auto-downloads** | — | user | yes (UVM:216-226) |
| 19 | 〃 | "Backup Data" | button | — | user | partial (restores 3 prefs) |
| 20 | 〃 | "Open Admin & Content Studio" | button, admin only | — | admin | yes |
| 21 | Appearance | "System / Light / Dark" | chips | `theme_mode`, SYSTEM | user | yes |
| 22 | 〃 | "Reference Voice Speed: 1.00x" | slider 0.65–1.35 | `tts_rate`, 1.0 | user | yes |
| 23 | 〃 | "First-Use Feature Overlays" | switch | `first_use_tutorials_enabled_{uid}`, true | user | yes |
| 24 | 〃 | "Replay Welcome & Module Tours" | button | — | user | yes |
| 25 | Integrations & Keystore Protection | "Optional YouTube Data API v3 Key" | **plaintext** text field | — | dev | yes, with no success feedback (AS:946-953; NAV:1134) |
| 26 | 〃 | "Encrypt & Save YouTube Key" | button | `youtube_api_key` (AES-GCM) | dev | yes; silently stores "" on encryption failure (PR:76, 87) |
| 27 | Privacy & Account | "Delete Speaking Session History" | button plus confirmation dialog | — | user | yes |
| 28 | 〃 | "Sign Out" | button, **no confirmation** here (Profile has one) | — | user | yes |
| 29 | — | Delete account | **absent** (MVM:474 has 0 callers) | — | user | **missing** |

Not exposed anywhere: notification preferences (only inside the drawer), reduced-motion and font-scale prefs (PR:241-251), and the GitHub release repo (it is on AppUpdateScreen) [V].

---

## 9. Code health in the UI layer

### 9.1 File sizes (lines)

| Size | Files |
|---|---|
| > 1,500 | CC 1943, AppNavigation 1548, W 1543 |
| 1,000–1,499 | SAC 1114, AS 1089, D 1038, LAT 1024 |
| 600–999 | CM 878, ETR 819, ND 785, LA 774, LM 710, ADS 682, AO 662, AU 660, TO 657, P 616 |
| < 600 | VO 555, UA 538, NC 483 (dead), FT 456, ADM 402, DP 381, YT 375, UD 332, theme 523 total |

**ViewModels:** SVM 621, AEVM 589, ICVM 552, MVM 487, UVM 432 [V].

**File/name mismatches** [V]:
- `VoiceTrainingLabScreen` lives in `LessonAndModulesScreens.kt` (LM:523).
- `RealWorldTasksScreen` lives in `LabsAndTrainingScreens.kt` (LAT:912).
- `LaunchExperienceViewModel` lives in a component file (LA:219).
- `rememberReducedMotionEnabled` lives in `CoachComponents.kt` (CC:112).

### 9.2 Very long composables (> 250 lines)

| Composable | Lines | Location |
|---|---|---|
| `AuthenticatedAppShell` | **1,032** | NAV:290-1321 |
| `SpeakingLabScreen` | 688 | SAC:103-790 |
| `SettingsScreen` | 675 | AS:394-1068 |
| `DashboardScreen` | 534 | D:143-676 |
| `ActivityDiscoveryScreen` | 518 | ADS:81-598 |
| `FeedbackReportCard` | 517 | CC:1326-1842 |
| `ConversationSimulatorScreen` | 497 | CM:99-595 |
| `TutorialOverlay` | 473 | TO:185-657 |
| `UserProfileScreen` | 437 | P:96-532 |
| `ActivityScreen` | 411 | UA:66-476 |
| `SpeakCraftLaunchOverlay` | 408 | LA:251-658 |
| `VocabularyScreen` | 384 | VO:69-452 |
| `ExerciseTemplateInteractionPanel` | 372 | ETR:80-451 |
| `AdminDashboardScreen` | 338 | ADM:65-402 |
| `ChallengesScreen` | 321 | SAC:794-1114 |
| `NotificationDrawer` | 305 | ND:96-400 |
| `OnboardingAssessmentScreen` | 301 | AO:331-631 |
| `SpotlightHeroCard` | 286 | W:191-476 |
| `AiMentorScreen` | 282 | CM:597-878 |
| `UpdateAvailableDialog` | 280 | UD:53-332 |
| `LessonDetailScreen` | 267 | LM:255-521 |
| `PronunciationLabScreen` | 255 | LAT:75-329 |

All [V].

### 9.3 Duplicated components and markup

- `feedbackSection` block pasted **8 times**: SAC:664-696, 734-766, 1039-1058, 1085-1104; LAT:205-231, 427-453, 616-642, 803-829 [V].
- Permission-checked record button pasted **7 times**: SAC:150-154, 835-837; LAT:103-105, 353-355, 556-558, 744-746; LM:550-552 [V].
- Lab list-card markup pasted twice per lab, once for tablet and once for phone (for example LAT:282-297 vs 309-324) [V].
- Grammar, Public Speaking, Shadowing and Pronunciation are structurally the same composable with different catalog lists [V].
- `NotificationsCenterSheet` duplicates `NotificationDrawer` [V].
- Two feedback cards (CC vs ETR) [V].
- Two `FeatureTutorialDialog` instances (NAV:606; D:173) [V].
- Calculations duplicated between dashboard and profile, listed in section 4.4 [V].

### 9.4 Dead composables and dead code (verified by grep)

- **Dead composables:** `NotificationsCenterSheet` plus private `NotificationAlertRow`, `AlertPreferencesPanel`, `AlertToggleRow` and `styleForNotificationCategory` (NC:79-468). That is the only dead composable family. Every other composable in `ui/` has at least one call site [V].
- **Dead parameters and state** [V]:
  - `DashboardScreen`: `realWorldTasks`, `onOpenProfile`, `personalizedCue`, `learningTrackLabel`, `ext` (D:151, 162, 166, 209-226).
  - `WelcomeBackBanner`: `learningTrack`, `learningDay`, `overallScore`, `personalizedCue`, `onAvatarClick` (W:130-135).
  - `InlineAiMentorCard.cloudAiEnabled` (W:1062-1068).
  - `VocabularyScreen`: `cloudAiEnabled`, `onToggleCloudAi`, with NAV passing `null` (VO:69, 81-82; NAV:977-978).
  - `ActivityDiscoveryScreen.initialTab` (ADS:87).
  - `cloudAiEnabled` collected but never read (CM:113, 609; YT:73; UA:82).
  - `evaluationState` (SAC:124); `recordedPath` (SAC:826).
  - The stale `@Suppress("UNUSED_PARAMETER")` on `SettingsScreen` (AS:391).
  - `LaunchExperienceViewModel.currentStage`, read only by tests (LA:226-227).
- **Unused imports** [V]: SAC:34; CC:49; ETR:8, 11, 28, 33.
- **Dead preference API** [V]: legacy tutorial keys and functions (PR:34-35, 203-227); `setReducedMotion` / `setFontScalePreference` (PR:241-251).
- **Dead ViewModel API:** listed in section 4.4.

### 9.5 Business logic inside composables (selected)

- **NAV** [V]:
  - Session-completed alert scoring (NAV:561-583).
  - Learning and notification store loading (NAV:546-560).
  - Announcement ingestion (NAV:588-593).
  - Dialog and overlay gating rules (NAV:617-655).
  - Dynamic title resolution against catalogs (NAV:527-538).
- **D:** score averages, task counts, hours formatting, words-learned max-of-3, weekly days, track percentages, module catalog build and search (D:180-319) [V].
- **W:** O(n²) done/highlight matching (W:533-602), streak copy rules (W:703-710), icon-by-category matching (W:500-508) [V].
- **SAC:** challenge merge and filter (SAC:802-820), cross-module delta (SAC:593-604), step calculation (SAC:176-181) [V].
- **CC `FeedbackReportCard`:** composite score formula that differs from the "previous" composite formula (CC:1351-1360), pace thresholds, regex parsing of rewrites (CC:1377-1393) [V].
- **VO:** spaced-repetition "due" logic (VO:89-98, 220-225) [V].
- **LM:** lesson mapping, and AI prompt strings built in the UI (LM:90-116, 387-392, 600-606) [V].
- **AS:** analytics period filter, WPM average, composite and mistake aggregation (AS:97-130). Default parameters instantiate `IntelligenceCapabilityManager().computeMatrix` (AS:413-426) [V].
- **ADM:** builds domain objects with `System.currentTimeMillis()` IDs and parses CSV (ADM:208-333) [V].
- **CM:** the voice-loop orchestration, including permission handling, lives in `onClick` (CM:333-360) [V].
- **ADS:** VM calls inside `remember` that read non-reactive `.value` (ADS:108-117) [V].

---

## 10. Test coverage of the UI

All UI tests are Robolectric (`@Config(sdk=[36])`). Phone tests use `w412dp-h915dp` (CUJ:66; HAI:42; LAUNCH:40) and tablet tests use `w1024dp-h768dp` (TAB:44). Every test renders **one composable in isolation** with fake callbacks [V].

| Test | What it actually asserts |
|---|---|
| CUJ `dashboard renders recommended next action, update banner, and searchable modules` (81-135) | Update banner tag displayed; tapping the recommended-action button produces a non-blank route; search "Storytelling" shows `module_card_module_lessons/storytelling` |
| CUJ `feedback report card omits fabricated pronunciation bar when unassessable` (138-162) | Insight text shown; the "Pronunciation" label does not exist |
| CUJ `lesson detail displays creator attribution badge and marks lesson completed` (165-199) | "Creator Framework (@askvinh)" shown; the completion toggle reports id plus `true` |
| CUJ `admin console blocks unauthorized user` (202-229) | "Admin Access Restricted" shown |
| CUJ `app update screen renders update check, backup creation, and restore button` (232-266) | Check, backup and restore buttons exist; restore opens the confirm dialog |
| CUJ `onboarding wizard progresses across 3 steps…` (269-299) | "Step 1/2/3 of 3" shown; completion callback gets name "Taylor" and level "Advanced" |
| CUJ `speaking lab renders video review player…` (302-332) | Preset chip fills the prompt; video player card shown; "Stage 1" switch updates the label |
| CUJ `settings screen gates destructive history wipe…` (335-367) | The confirmation dialog must be accepted before the callback fires |
| CUJ `pronunciation vs articulation routes show distinct headers and filters` (370-393) | **Only** checks that "Articulation & Enunciation Lab" is displayed. Pronunciation is not rendered and filters are not checked; the test name overclaims |
| CUJ `analytics screen shows empty state when Daily window…` (396-417) | Daily chip, then the "No sessions in Daily window" text |
| CUJ `ai mentor renders persistent chat bubbles` (420-443) | `mentor_chat_bubble_0` displayed |
| CUJ `feature guide strip expands…` (446-460) | Expanding shows "How to Use" and "What Will Improve By Doing This" |
| CUJ `dashboard browse all tutorials opens searchable 24 feature dialog` (463-500) | Dialog opens; "all" tab shows the dashboard item |
| CUJ `tutorial overlay steps through speaking lab and ai mentor…` (503-551) | Step titles, try-module route `speaking_lab`, don't-show plus skip produces `dismissed && dontShow && !completed` |
| CUJ `settings screen toggles first use overlays and triggers replay tours` (554-587) | Switch reports false; replay callback fires |
| CUJ `update available dialog shows release notes, direct apk link…` (590-639) | Notes text, direct-link URL, confirm and dismiss callbacks |
| CUJ `update available dialog highlights signing key conflict…` (642-682) | Conflict notice shown; confirm opens the direct URL |
| CUJ `welcome back banner displays minimal animated username greeting` (685-695) | "Welcome back, Sruthi" with `reducedMotion = true` |
| CUJ `user profile screen displays user info and saves profile edits` (698-737) | Name shown; edit then save reports "Sruthi K" and 15 min |
| CUJ `dynamic local coach produces topic specific rewrites…` (740-…) | Pure engine logic, not UI |
| TAB ×7 (59-283) | Bounds-based side-by-side checks: tutorial overlay columns; dashboard hero beside plan and 4 KPI tiles left-to-right; update check beside backup; analytics tiles and cards in 2 columns; settings updates beside tutorial switch; speaking lab prompt beside history; launch emblem ≥ 144dp |
| LAUNCH ×8 (47-295) | Timeline math per stage; cadence latch first vs returning; no replay after completion (VM only); overlay stages, then a placeholder "Signed-in Dashboard Ready" text; AuthScreen exists after overlay completes; light/dark plus reduced motion plus vector fallback tags; emblem ≤ 108dp at 320dp and 120–128dp at 412dp |
| HAI ×5 (57-193) | Settings cloud switch is off and disabled, and the live-transcription toggle fires; Mentor, Conversation and Speaking Lab have no cloud chip; role chip changes the VM role; the voice toggle shows the call button; inline mentor card shows "Local AI" and dispatches the prompt and chip text |
| AET ×4 (51-199) | Invalid ID shows the recovery card with working back; full start → input → confidence → submit → feedback → continue flow; dialogue template in dark theme; discovery search "FORD", Daily Plan durations, Paths card, Analytics labels |
| Related: `NotificationManagementSystemTest` (173-…) | Drawer renders alerts, filter (pinned), preferences, pin, navigation |
| Related: `CentralizedAiSettingsTest` 07–10 | Settings shows 5 AI subsections; Speaking Lab, Challenges and Vocabulary have no scattered cloud toggles |

**Important flows that are NOT tested** [V, by grep of `src/test`]:
- `SpeakCraftRoot`, `AuthenticatedAppShell` and the NavHost: 0 references. Nothing covers routing, the bottom bar, rail or sidebar, back-stack behavior, the onboarding → dashboard transition, profile loading or error, or the tutorial and update-dialog gating rules.
- Microphone or camera **permission denial**; recording start, stop and evaluate end to end (all labs are rendered without recording).
- Offline and Firestore error states.
- Screens with no test: `DailyPracticeScreen`, `LearningPathsScreen`, `YouTubeIntelligenceScreen`, `ListeningShadowingScreen`, `GrammarSpeakingScreen`, `PublicSpeakingScreen`, `VoiceTrainingLabScreen`, `RealWorldTasksScreen`, `ModuleLessonsScreen`, `SpotlightHeroCard` / carousel swipe, `TodaysPracticePlanCard`.
- Font scaling, TalkBack semantics, reduced motion on the dashboard, RTL.
- Configuration change and process death (only `LaunchExperienceViewModel` is checked for no replay).
- Mandatory-update lockout; notification route navigation to an unknown route.
- **Production VM construction:** tests use the deprecated constructors (`GeminiCoachService()` ×9 in tests, 0 in main), so the factory wiring in NAV:317-403 is untested.

---

## 11. Feature status classification (code-based)

| Capability | Status | Key evidence |
|---|---|---|
| Google sign-in | DOCUMENTED_AND_IMPLEMENTED | AO:79-304; AH:58-98. Google only |
| Onboarding assessment (3 steps) | PARTIALLY_IMPLEMENTED | No validation, no skip, invisible error, self-ratings shown as "objective" (section 3.2) |
| Launch animation | DOCUMENTED_AND_IMPLEMENTED | LA:71-180. Unskippable; 2.1 s on every launch |
| Welcome-back experience | PARTIALLY_IMPLEMENTED | W:127-189 (greeting only; unused params) |
| Learning-plan carousel (HorizontalPager) | DOCUMENTED_AND_IMPLEMENTED | W:247-260. Content mostly static; same image on every slide |
| Personalized dashboard | PARTIALLY_IMPLEMENTED | Plan from the local engine; fake fallbacks; missing achievements and continue-learning (section 3.3) |
| Dashboard module directory and search | DUPLICATED | D:285-382 duplicates Quick Access, hero and sidebar |
| Daily Practice (8 steps) | DUPLICATED | DP:59-214 vs dashboard plan vs Activity Engine daily plan |
| Dashboard "Today's Practice Plan" | PARTIALLY_IMPLEMENTED | Self-reported ticks (W:639); fallback fixed plan (W:520-525) |
| Streak | PARTIALLY_IMPLEMENTED | Two sources; seeded at 1 (REPO:494; W:703; P:153-156) |
| Learning Paths (7 programs) | BROKEN | 6 of 7 cannot complete; no preview or unenroll (DP:278-368; CAT:1072-1121) |
| Module lessons / lesson detail | PARTIALLY_IMPLEMENTED | 12 lessons; `confidence` module empty; channel-root "sources" (CAT:205-517) |
| Vocabulary (words, flashcards, quiz, notebook) | PARTIALLY_IMPLEMENTED | 20-word bank; misleading "due" count; parallel unused local vocab store; `LocalVocabularyQuizEngine` unused |
| Record & Review (speaking_lab) | PARTIALLY_IMPLEMENTED | Works locally; offline spin risk; silent permission denial; cross-lab recording leak |
| Challenges | PARTIALLY_IMPLEMENTED | No retry; filter/selection mismatch (SAC:817-821, 1053) |
| Pronunciation lab | DUPLICATED | Same screen as articulation; no target comparison (LIO:938-943) |
| Articulation lab | DUPLICATED | NAV:997-1005; default filter has 1 drill |
| Voice Training lab | BROKEN (partially) | Shares the result slot with Speaking Lab (LM:602); pause slider unused; copy says 5 levers, code has 4 |
| Listening/shadowing | PARTIALLY_IMPLEMENTED | Not simultaneous; pause markers and WPM ignored; generic links |
| Grammar speaking | DUPLICATED | Same pipeline; subtitle promises PREP/CLEAR content that does not exist |
| Public speaking | DUPLICATED | Same pipeline; body-language module audio-only |
| Real-world tasks | PARTIALLY_IMPLEMENTED | Position-based IDs; no save feedback (LAT:940-1020) |
| Conversation simulator | PARTIALLY_IMPLEMENTED | Local rule-based; one-shot voice; turns not persisted; streaming voice call has no caller (ICVM:256-299) |
| AI Mentor | PARTIALLY_IMPLEMENTED | Local rule-based text with artificial chunk delay (LIO:770-774); history persisted |
| Inline dashboard mentor card | DUPLICATED | Entry to ai_mentor (NAV:938-945) |
| YouTube Intelligence ("@askvinh" tab) | PLACEHOLDER (heuristic output presented as verified) | LIO:847-907; YT:321 |
| Activity Engine (150 activities) | PARTIALLY_IMPLEMENTED | Works and is tested; Swap no-op; rotation wipe; blank loading; orphan module route |
| Activity Engine daily plan and paths | DUPLICATED | ADS:349-470 vs Daily Practice and Learning Paths |
| Progress Analytics | PARTIALLY_IMPLEMENTED | Fallback numbers; self-ratings labelled "Objective" (AS:101-110) |
| Notifications drawer | PARTIALLY_IMPLEMENTED | In-app only; no OS notifications (section 8.3) |
| Notifications center sheet | BROKEN (dead code) | NC: 0 call sites |
| Tutorial overlay and feature guides | DUPLICATED | 4 mechanisms (section 8.2) |
| Update dialog and OTA screen | PARTIALLY_IMPLEMENTED | Mandatory lockout; install likely blocked (section 8.4) |
| Backups and restore | PARTIALLY_IMPLEMENTED | Restores only 3 prefs (UVM:325-337) |
| Admin console | PARTIALLY_IMPLEMENTED | 4 tabs; moderation "queue" lists everything; release form incomplete |
| Profile | PARTIALLY_IMPLEMENTED | No photo, no delete, level and goal mismatches with onboarding |
| Settings | PARTIALLY_IMPLEMENTED | Developer-first; dead cloud controls; plaintext key; no account deletion |
| Cloud AI (toggle and tiers) | PLACEHOLDER | Hard-disabled (`LocalExecutionPolicy.kt:13`) |
| Account deletion | BROKEN (not wired) | MVM:474, 0 callers |
| Theme (system/light/dark) | DOCUMENTED_AND_IMPLEMENTED | NAV:221-225; AS:537-544. No dynamic color |
| Tablet layouts (rail and sidebar) | PARTIALLY_IMPLEMENTED | Rail lacks secondary destinations; sidebar back-stack and label inconsistencies |
| Localization | UNKNOWN / not started | 4 string resources; about 386 hardcoded literals |
