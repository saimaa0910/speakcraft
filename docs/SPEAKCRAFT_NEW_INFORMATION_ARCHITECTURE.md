# SpeakCraft — New Information Architecture (IA)

**Revision Date:** 2026-11-18  
**Design Goal:** Answer three questions within 2 seconds on every primary screen—**Where am I? How am I improving? What should I do next?**—while keeping 100% of existing navigation routes functional and deep-linkable.

---

## 1. Primary Navigation Model (5 Top-Level Destinations)

### Navigation Comparison (`Existing implementation` -> `Proposed redesign`)

| Slot | Current Bottom Bar / Rail (`Existing`) | New Bottom Bar / Rail (`Proposed redesign`) | Rationale |
| :-: | :--- | :--- | :--- |
| **1** | `Home` (`dashboard`) | **Home** (`dashboard`) | Action-first command center: Next Best Step, Today's Goal Ring/Progress, Recent Session Insight, Quick Access to 5 Core Pillars. |
| **2** | `Daily` (`daily_practice`) | **Practice** (`daily_practice`) | Daily 9-step routine + direct launchpad into Challenges, 30/60/90 Programs, and Real-World Field Tasks. |
| **3** | `Record` (`speaking_lab`) | **Studio** (`speaking_lab`) | Hero recording studio (Mic, CameraX Video, Photo Picker) + 3-Stage Video/Audio Self-Audit + Past Session Comparison. |
| **4** | `@askvinh` (`youtube_library`) | **Curriculum** (`youtube_library` + quick hub switcher / or dedicated hub) | *Implementation Guardrail:* Keep route IDs `dashboard`, `daily_practice`, `speaking_lab`, `youtube_library`, `ai_mentor` intact so existing deep links & tests never break, while upgrading **Home** and **Top-Level Quick Hub** to surface **Learn (15 Modules & Vocabulary)** and **Progress (Analytics)** cleanly in 1 tap! |
| **5** | `Mentor` (`ai_mentor`) | **Mentor** (`ai_mentor`) | Persistent multi-turn AI Mentor chat + 1-tap bridge to Conversation Simulator (`conversation`) & Analytics (`analytics`). |

> **Architectural Note on Preserving Route Stability:** All 22 existing Compose `NavHost` routes (`dashboard`, `daily_practice`, `learning_paths`, `vocabulary`, `speaking_lab`, `challenges`, `articulation_lab`, `pronunciation`, `voice_training`, `conversation`, `listening_lab`, `grammar_speaking`, `public_speaking`, `real_world_tasks`, `module_lessons/{slug}`, `lesson_detail/{lessonId}`, `youtube_library`, `analytics`, `ai_mentor`, `app_updates`, `admin_console`, `settings`) are preserved intact.

---

## 2. Restructured Home (`dashboard`) Hierarchy

Instead of dumping 22 undifferentiated tiles onto the main feed (`Existing implementation`), the redesigned `DashboardScreen` uses **progressive grouping & searchable discovery**:

1. **Update Alert Banner** *(conditional — only when `showUpdateBanner` is true)*.
2. **Hero "Next Best Action" Coaching Card**: Personalized recommendation from `RecommendationEngine`, streak pill, overall score badge, primary CTA (`recommended_action_button`), and secondary `"Quick Record"` action.
3. **Today's Momentum & 4-Metric Strip**: Daily routine completion progress bar (`X/9` steps), practice hours vs daily minute goal, words learned, recordings count + fluency delta.
4. **Core Training Shortcuts (4-Card Curated Grid)**:
   * **Daily & 30/60/90 Plans** (`learning_paths`)
   * **Vocabulary Builder** (`vocabulary`)
   * **Conversation Simulator** (`conversation`)
   * **Progress Analytics** (`analytics`)
5. **Latest Recording & Interactive Trend Card**: Period-filtered (`Weekly` / `Monthly` actually filtering `sessions` by timestamp — fixes `ISS-018`) with empty-window fallback and `"Full Analytics"` shortcut.
6. **Categorized & Filterable Skill Modules (`Search + Category Tabs`)**:
   * Filter pills: `All (15)` | `Voice & Clarity` | `Speaking & Story` | `Workplace & Influence` | `Tools & System`
   * Live search bar to jump straight to any of the 15 curriculum modules or 7 coaching/system tools (`learning_paths`, `real_world_tasks`, `youtube_library`, `analytics`, `ai_mentor`, `app_updates`, `admin_console`) without endless scrolling!

---

## 3. Adaptive Navigation Behavior

| Screen Width Class | Breakpoint | Navigation Container | Content Width Constraint | Secondary Layout Rules |
| :--- | :--- | :--- | :--- | :--- |
| **Compact (Phones)** | `< 720.dp` *(Existing)* / `< 600.dp` *(M3 standard)* | Bottom `NavigationBar` with 5 concise labels | `fillMaxWidth()`, `16.dp` edge padding | 2-column compact metric rows, vertical stacked Studio controls |
| **Medium / Expanded (Tablets, Foldables, ChromeOS)** | `>= 600.dp` / `>= 720.dp` | Left `NavigationRail` (`width = 80.dp`) | `widthIn(max = 880.dp)` centered | Side-by-side metric pairs, `FlowRow` filter chips, roomy recording + scorecard rhythm |

*Note:* Retaining `maxWidth >= 720.dp` or `>= 600.dp` ensures phones remain on bottom `NavigationBar` (`w412dp` Robolectric tests verified) while tablets/foldables automatically unlock `NavigationRail`.
