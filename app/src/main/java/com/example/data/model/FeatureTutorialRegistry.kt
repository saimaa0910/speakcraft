package com.example.data.model

data class FeatureTutorial(
    val featureKey: String,
    val title: String,
    val categoryBadge: String,
    val estimatedTime: String,
    val summary: String,
    val howToUseSteps: List<String>,
    val whatImproves: List<String>,
    val coachTip: String
)

object FeatureTutorialRegistry {

    val allTutorials: List<FeatureTutorial> = listOf(
        FeatureTutorial(
            featureKey = "auth",
            title = "Google Sign-In & Cloud Sync",
            categoryBadge = "Account & Privacy",
            estimatedTime = "30 sec",
            summary = "Connect your Google account so recordings metadata, streaks, vocabulary mastery, and AI feedback sync securely across sessions.",
            howToUseSteps = listOf(
                "Tap 'Sign in with Google' to authenticate via Android Credential Manager.",
                "If you signed in previously, SpeakCraft automatically checks your session at launch."
            ),
            whatImproves = listOf(
                "Preserves your daily streaks, speaking scores, and vocabulary spaced-repetition schedule across reinstalls or devices.",
                "Unlocks live Gemini AI coaching and personal Firestore progress logs."
            ),
            coachTip = "Your audio/video files stay private on-device; only your scores, transcripts, and structured study logs sync to Firestore."
        ),
        FeatureTutorial(
            featureKey = "onboarding",
            title = "3-Step Baseline Calibration",
            categoryBadge = "Getting Started",
            estimatedTime = "1–2 min",
            summary = "Calibrate your speaking level, target goal, and daily time budget so SpeakCraft builds a realistic starting coaching plan.",
            howToUseSteps = listOf(
                "Step 1: Enter your name and tap your current English speaking level to load realistic baseline defaults.",
                "Step 2: Pick a suggested communication goal (or write your own) and select how many minutes per day you can practice.",
                "Step 3: Fine-tune the 7 skill sliders if needed, then tap 'Start My Coaching Plan'."
            ),
            whatImproves = listOf(
                "Tailors your AI Mentor advice, Conversation Simulator difficulty, and Next Step recommendation from Day 1.",
                "Enrolls you immediately into the 30-Day Communication Challenge."
            ),
            coachTip = "Be honest rather than optimistic on your baseline—SpeakCraft updates your objective scores automatically once you record real speech."
        ),
        FeatureTutorial(
            featureKey = "dashboard",
            title = "Home Coach Dashboard",
            categoryBadge = "Command Center",
            estimatedTime = "1 min overview",
            summary = "Your daily starting point showing your highest-impact Next Step, routine momentum, filtered trends, and all 15 speaking modules.",
            howToUseSteps = listOf(
                "Tap the top 'Next Step' hero button for the single best exercise recommended for your lowest or overdue skill.",
                "Check 'Today's Routine Progress' or jump into 30/60/90 Plans, Vocabulary, Conversation, or Analytics from the 4 hub shortcuts.",
                "Use Weekly / Monthly chips on the trend card and the search bar or category chips below to locate any of the 15 training modules."
            ),
            whatImproves = listOf(
                "Decision clarity: eliminates guesswork about what to practice each day.",
                "Habit momentum: makes daily step completion, streak continuity, and vocal clarity gains immediately visible."
            ),
            coachTip = "Always glance at the hero recommendation first—even 10 focused minutes on your weakest skill beats random browsing."
        ),
        FeatureTutorial(
            featureKey = "daily_practice",
            title = "Daily Speaking Practice Routine",
            categoryBadge = "Core Habit",
            estimatedTime = "10–15 min/day",
            summary = "A structured 9-step daily checklist combining breathing, vocal drills, 3 rotating target words, impromptu speaking, and reflection.",
            howToUseSteps = listOf(
                "Review Today's 3 Target Words at the top and weave at least two into your spoken drills.",
                "Tap the circle beside each step as you complete it, or tap 'Open' to launch that step's lab directly.",
                "Set your 1–10 daily confidence slider, write a quick reflection takeaway, and tap 'Save Daily Reflection' to extend your streak."
            ),
            whatImproves = listOf(
                "Speaking consistency & consecutive-day streak discipline.",
                "Spontaneous word retrieval by forcing daily active usage of fresh vocabulary.",
                "Self-awareness through structured end-of-session reflection."
            ),
            coachTip = "Completing steps on consecutive calendar days builds your streak—skipping 2+ days resets it cleanly to 1."
        ),
        FeatureTutorial(
            featureKey = "learning_paths",
            title = "30 / 60 / 90-Day Structured Programs",
            categoryBadge = "Curriculum Tracks",
            estimatedTime = "5–15 min/day",
            summary = "Multi-week progressive roadmaps designed around communication foundations, workplace clarity, and executive presence.",
            howToUseSteps = listOf(
                "Browse the 30-Day, 60-Day, or 90-Day tracks and tap 'Enroll' on your chosen commitment.",
                "Expand the active program to see weekly focus themes and daily drills.",
                "Check off each day's drill as you finish it to watch your program completion bar advance."
            ),
            whatImproves = listOf(
                "Long-term skill compounding—sequences vocal mechanics before storytelling, persuasion, and stage delivery.",
                "Accountability through visible milestone completion across weeks."
            ),
            coachTip = "Start with the 30-Day Challenge before adding the 60- or 90-day tracks so your daily routine stays sustainable."
        ),
        FeatureTutorial(
            featureKey = "vocabulary",
            title = "Vocabulary Builder & Spaced Repetition",
            categoryBadge = "Verbal Power",
            estimatedTime = "5–8 min",
            summary = "A 4-tab active vocabulary engine with IPA phonetics, collocations, spaced-repetition flashcards, AI sentence evaluation, and a personal notebook.",
            howToUseSteps = listOf(
                "Words tab: Filter by level or search keywords, and tap the speaker icon to hear pronunciation + context sentence.",
                "Flashcards tab: Tap the card to reveal meaning/example, then pick 'Review Again (1d)' or 'Mastered Next' to schedule spaced review.",
                "AI Quiz tab: Write a realistic sentence using the target word and tap 'Evaluate Conversational Usage' for naturalness scoring.",
                "Notebook tab: Save custom phrases you hear at work or in practice lessons with personal usage notes."
            ),
            whatImproves = listOf(
                "Active spoken vocabulary precision (moving words from passive recognition into real sentences).",
                "Natural collocation choice instead of awkward literal translations.",
                "Long-term retention via spaced repetition intervals."
            ),
            coachTip = "Do not just memorize definitions—always test a word in the AI Quiz tab inside a sentence you would actually say in a meeting."
        ),
        FeatureTutorial(
            featureKey = "speaking_lab",
            title = "Record & Review Studio (3-Stage Audit)",
            categoryBadge = "Core Practice",
            estimatedTime = "3–7 min",
            summary = "Capture mic audio (with pause/resume) or CameraX/Photo Picker video (<19MB), perform the 3-Stage Self-Audit, and get coaching diagnostics.",
            howToUseSteps = listOf(
                "Enter or customize your speaking prompt at the top.",
                "Tap 'Record Mic' (supports Pause/Resume), 'Video Clip' (in-app camera), or 'Attach Video from Photo Picker'.",
                "Step 2 Audit: Review your clip using the 3 stage chips—Stage 1 (Muted visual posture), Stage 2 (Audio-only eyes closed for vocal pace/fillers), Stage 3 (Full AV).",
                "Tap 'Analyze With AI Coach' for transcript analysis, WPM pacing, filler word detection, and actionable drills."
            ),
            whatImproves = listOf(
                "Eliminates filler words ('um', 'uh', 'like') by exposing them in Stage 2 audio review and transcripts.",
                "Upgrades posture, eye line, and hand gestures via Stage 1 muted visual playback.",
                "Calibrates speaking speed (target 120–155 WPM) and vocal variety."
            ),
            coachTip = "Start with Stage 1 (Muted) first because hearing your own voice distracts you from spotting distracting body tension."
        ),
        FeatureTutorial(
            featureKey = "challenges",
            title = "Timed Communication Challenges",
            categoryBadge = "Impromptu Sprints",
            estimatedTime = "60–90 sec",
            summary = "Daily and weekly countdown speaking sprints built around core techniques like PREP, hooks, and intentional pausing.",
            howToUseSteps = listOf(
                "Filter between ALL, DAILY, or WEEKLY challenges and tap any challenge card to load its timer & framework hint.",
                "Tap 'Start [N]s Timer' to record under a realistic live countdown (pause/resume supported).",
                "When the timer finishes (or after tapping 'Stop Early'), tap 'Submit AI Check' to grade adherence to the framework."
            ),
            whatImproves = listOf(
                "Impromptu clarity under time pressure—stops rambling and teaches crisp endings.",
                "Mental agility when organizing thoughts into Point-Reason-Example-Point in seconds."
            ),
            coachTip = "Treat the final 5 seconds as your landing runway: finish your last sentence downward and hold 1 second of poised silence."
        ),
        FeatureTutorial(
            featureKey = "articulation_lab",
            title = "Articulation & Enunciation Lab",
            categoryBadge = "Speech Clarity",
            estimatedTime = "5 min",
            summary = "Focused tongue-placement, crisp consonant contrast (/r/ vs /l/, /θ/ vs /s/, /v/ vs /w/), and connected-speech drills to eliminate mumbling.",
            howToUseSteps = listOf(
                "Select a drill category (defaults to Difficult Sounds for consonant clarity) and read the physical articulation tip.",
                "Tap 'Listen 0.75x' first to hear clean mouth mechanics, then 'Listen 1.0x' for natural speed.",
                "Tap 'Record', speak the practice phrase clearly, then tap 'Compare AI' for phonetic accuracy notes."
            ),
            whatImproves = listOf(
                "Eliminates mumbling and swallowed word endings so listeners catch every key word the first time.",
                "Builds muscle memory for tricky English consonant clusters and word-boundary linking."
            ),
            coachTip = "Exaggerate jaw opening and consonant crispness slightly during 0.75x practice—normal speed will then sound effortlessly clear."
        ),
        FeatureTutorial(
            featureKey = "pronunciation",
            title = "Phonetic & Intonation Lab",
            categoryBadge = "Sound & Stress",
            estimatedTime = "5 min",
            summary = "Master English vowel minimal pairs (ship/sheep), multi-syllable word stress, sentence emphasis, and downward authority inflection.",
            howToUseSteps = listOf(
                "Browse Minimal Pairs, Word Stress, Sentence Stress, or Intonation chips at the top.",
                "Listen to both 0.75x and 1.0x audio models to hear vowel length and pitch drops.",
                "Record your voice repeating the phrase and tap 'Compare AI' to audit stress placement and upspeak."
            ),
            whatImproves = listOf(
                "Vowel distinction accuracy so similar-sounding English words never confuse listeners.",
                "Removes accidental 'upspeak' at the end of statements, projecting immediate certainty and authority."
            ),
            coachTip = "In English, stressed syllables are louder, slightly higher in pitch, and longer—unstressed vowels relax."
        ),
        FeatureTutorial(
            featureKey = "voice_training",
            title = "Voice Training Lab (5 Vocal Foundations)",
            categoryBadge = "Vocal Presence",
            estimatedTime = "5–8 min",
            summary = "Train the 5 vocal levers—Rate, Pause, Melody, Volume, and Character—using customizable WPM and pause-beat targets.",
            howToUseSteps = listOf(
                "Pick one of the 4 vocal lever cards below (e.g., Rate & Intentional Pauses or Melody & Pitch Range).",
                "Adjust the Target Pace (WPM) and Pause Hold sliders, then tap 'Hear Reference' to hear the cadence.",
                "Tap 'Practice Drill' to record yourself reading the contrast script, then run 'Evaluate Vocal Foundations With AI'."
            ),
            whatImproves = listOf(
                "Breaks monotonous flat delivery by teaching deliberate speed contrast and pitch melody.",
                "Builds comfort with 1–2 second silent pauses so key takeaways land powerfully."
            ),
            coachTip = "If you feel awkward pausing for a full second, it usually sounds poised and confident to your audience."
        ),
        FeatureTutorial(
            featureKey = "conversation",
            title = "Interactive Conversation Simulator",
            categoryBadge = "Dialogue Mastery",
            estimatedTime = "4–8 min",
            summary = "Practice 13 real-world dialogue scenarios (interviews, manager 1-on-1s, disagreements) by speaking or typing without mid-chat interruptions.",
            howToUseSteps = listOf(
                "Select one of the 13 scenario chips at the top (confirms before discarding an active dialogue).",
                "Tap the Mic button to dictate your spoken reply—or type naturally—and send to receive spoken & written partner responses.",
                "After 2+ exchanges, tap 'End & Get Coach Check-In Feedback' for a full dialogue critique saved to your history."
            ),
            whatImproves = listOf(
                "Real-time listening comprehension and diplomatic turn-taking in high-stakes workplace dialogues.",
                "Confidence handling tricky interview questions, pushback, and disagreements smoothly."
            ),
            coachTip = "Use the 4 Ways of Listening mindset: briefly paraphrase your partner's concern before stating your counterpoint."
        ),
        FeatureTutorial(
            featureKey = "listening_lab",
            title = "Active Listening & Shadowing Lab",
            categoryBadge = "Cadence & Rhythm",
            estimatedTime = "5–7 min",
            summary = "Shadow curated clips marked with pitch arrows and [PAUSE] cues to internalize natural pacing, emphasis, and rhythm.",
            howToUseSteps = listOf(
                "Select a shadowing segment and study the stress CAPS, pitch arrows [↗/↘], and [PAUSE] markers.",
                "Tap 'Play Reference' to absorb the speaker's cadence and breathing points.",
                "Tap 'Shadow & Repeat' to record yourself matching the rhythm and WPM target, then tap 'Score Shadow'."
            ),
            whatImproves = listOf(
                "Natural English rhythm, chunking, and pause placement without sounding robotic or rushed.",
                "Trains your ear to hear subtle vocal tone shifts (The 4 Ways of Listening)."
            ),
            coachTip = "Match the speaker's exact silence duration at every [PAUSE] marker—rhythm matters just as much as individual words."
        ),
        FeatureTutorial(
            featureKey = "grammar_speaking",
            title = "Thinking & Articulation / Spoken Grammar",
            categoryBadge = "Structure & Flow",
            estimatedTime = "5 min",
            summary = "Turn grammar rules into spoken reflexes alongside PREP & CLEAR thought-organization drills for spontaneous clarity.",
            howToUseSteps = listOf(
                "Pick a conversational grammar/structure topic (e.g., Storytelling Tenses, Indirect Questions, Conditionals).",
                "Compare the highlighted 'Common Mistake' vs 'Natural Fix' aloud twice.",
                "Tap 'Speak Drill' to answer the speaking prompt aloud, then tap 'Check Grammar' for AI syntax & clarity feedback."
            ),
            whatImproves = listOf(
                "Reduces hesitation from translating grammar rules mentally mid-sentence.",
                "Improves spontaneous sentence signposting ('Although...', 'Because...', 'For example...') under pressure."
            ),
            coachTip = "Practice grammar aloud rather than on paper—spoken fluency comes from ear-and-tongue familiarity."
        ),
        FeatureTutorial(
            featureKey = "public_speaking",
            title = "Public Speaking & Presentation Studio",
            categoryBadge = "Stage & Impact",
            estimatedTime = "6–10 min",
            summary = "Rehearse 6 core presentation phases—Audience Calibration, Magnetic Hooks, Rule of 3, Reliving Stories, Projection, and Downward Close.",
            howToUseSteps = listOf(
                "Select one of the 6 presentation phase cards below to load its speaking framework and assignment.",
                "Tap 'Record Rehearsal' to deliver the assigned 15–60 second segment standing up with Power Zone gestures.",
                "Tap 'Get Presentation AI' to critique your opening hook, vocal modulation, and closing impact."
            ),
            whatImproves = listOf(
                "Transforms boring factual updates into memorable talks with bold openings and direct-dialogue stories.",
                "Eliminates rushed weak endings by training poised, downward-inflected closes."
            ),
            coachTip = "Never script a whole talk word-for-word—memorize your opening hook, 3 core pillars, and closing line so you stay conversational."
        ),
        FeatureTutorial(
            featureKey = "real_world_tasks",
            title = "Real-World Field Tasks",
            categoryBadge = "Real-Life Transfer",
            estimatedTime = "Outside app + 2 min log",
            summary = "Offline missions that move your skills out of the app into real meetings, coffee chats, and team updates—paired with reflection logging.",
            howToUseSteps = listOf(
                "Choose a real-world mission (such as explaining your work via CLEAR or asking 3 open-ended questions in a meeting).",
                "Perform the task in your actual day, then check the box and record what happened and what to refine next.",
                "Tap 'Save Reflection' to update your pending task counter on the Dashboard."
            ),
            whatImproves = listOf(
                "Bridges the gap between safe solo app practice and real social/workplace courage.",
                "Locks in behavioral insights right after real conversations."
            ),
            coachTip = "Pick just ONE micro-habit per real meeting (e.g., downward inflection on your recommendation) rather than trying to fix everything at once."
        ),
        FeatureTutorial(
            featureKey = "module_lessons",
            title = "Module Curriculum Directory",
            categoryBadge = "Structured Lessons",
            estimatedTime = "3–10 min/lesson",
            summary = "Browse structured lessons inside Storytelling, Body Language, Confidence, Professional Communication, or Influence.",
            howToUseSteps = listOf(
                "Review the framework badges to distinguish core speaking frameworks from editorial drafts.",
                "Tap any lesson card to read the breakdown, review the common mistake vs correction, and record a targeted practice attempt.",
                "Save personal takeaways after each drill to track your mastery across modules."
            ),
            whatImproves = listOf(
                "Deep mastery of specific pillars like Power Zone gestures, Reliving vs Reporting stories, and Friendly Authority.",
                "Clear, step-by-step practice progression from concept to spoken execution."
            ),
            coachTip = "Work through lessons inside your weakest module sequentially so each technique builds on the last."
        ),
        FeatureTutorial(
            featureKey = "lesson_detail",
            title = "Interactive Lesson & Practice Check",
            categoryBadge = "Learn + Apply",
            estimatedTime = "5–8 min",
            summary = "Study the technique breakdown, review the before-and-after example, record your attempt inline, and log personal takeaways.",
            howToUseSteps = listOf(
                "Read the Technique Breakdown, Step-by-Step Instructions, and Before/After example.",
                "Scroll to 'Practice Prompt', tap 'Record', speak your attempt, and tap 'Get AI Feedback' for lesson-specific grading.",
                "Jot down your takeaway note and tap 'Mark Complete', then 'Next Lesson' to continue."
            ),
            whatImproves = listOf(
                "Turns passive reading into immediate muscle memory through 60-second applied speaking.",
                "Tracks completed lessons across your Dashboard and Personalized AI Mentor."
            ),
            coachTip = "Write one concrete action in the takeaway box before marking complete—writing forces recall."
        ),
        FeatureTutorial(
            featureKey = "analytics",
            title = "Progress Analytics & Mistake Patterns",
            categoryBadge = "Measurable Growth",
            estimatedTime = "2 min check",
            summary = "Track objective speech metrics (Fluency, Pronunciation, WPM Pace, Articulation) filtered by Daily/Weekly/Monthly windows separately from self-rated confidence.",
            howToUseSteps = listOf(
                "Toggle Daily, Weekly, or Monthly chips at the top to filter trends, session counts, WPM averages, and recurring mistakes.",
                "Compare your Strongest vs Weakest objective skills and inspect recurring filler words or phrasing patterns.",
                "Tap 'Ask AI Mentor for Custom Weekly Plan' to turn your analytics into a targeted 7-day drill schedule."
            ),
            whatImproves = listOf(
                "Makes vocal and verbal improvement objective and measurable over weeks.",
                "Targets your #1 bottleneck instead of over-practicing strengths."
            ),
            coachTip = "Don't compare objective AI speech scores directly to self-rated confidence—watching confidence lag or lead reveals self-perception gaps."
        ),
        FeatureTutorial(
            featureKey = "ai_mentor",
            title = "Personalized Multi-Turn AI Mentor",
            categoryBadge = "Personal Coach",
            estimatedTime = "2–5 min chat",
            summary = "A persistent, multi-turn AI communication coach aware of your recent recording scores, recurring mistakes, and speaking curriculum.",
            howToUseSteps = listOf(
                "Tap any quick-prompt chip at the top (e.g., '7-day improvement plan') or ask any follow-up question in your own words.",
                "Ask follow-ups naturally—your conversation thread persists locally per account so you can continue anytime.",
                "Use 'Clear' in the header (with confirmation) whenever you want to start a fresh coaching topic."
            ),
            whatImproves = listOf(
                "Gives tailored troubleshooting when you feel stuck on filler words, interview anxiety, or vocal monotony.",
                "Connects your exact WPM/fluency stats directly to the right practice exercise."
            ),
            coachTip = "Ask specific follow-ups like: 'Turn my top 2 mistakes into a 5-minute morning warm-up I can do before standup.'"
        ),
        FeatureTutorial(
            featureKey = "settings",
            title = "Settings, Voice Speed & Privacy",
            categoryBadge = "Preferences",
            estimatedTime = "1 min",
            summary = "Customize light/dark theme, reference TTS voice speed, on-device AI controls, and local/cloud history settings.",
            howToUseSteps = listOf(
                "Choose System, Light, or Dark appearance and drag 'Reference Voice Speed' (0.65x–1.35x) to match your listening comfort.",
                "Manage on-device speech recognition, audio/video analysis toggles, and first-use feature tutorials.",
                "Review on-device privacy guarantees and manage your speaking history or account sign-out safely."
            ),
            whatImproves = listOf(
                "Makes listening models easier to shadow by tuning TTS speed to your current comprehension level.",
                "Keeps practice recordings private on-device and prevents accidental history loss with confirmation dialogs."
            ),
            coachTip = "Lower Reference Voice Speed to 0.85x while mastering tricky intonation patterns, then raise it back to 1.0x–1.15x."
        ),
        FeatureTutorial(
            featureKey = "admin_console",
            title = "Admin & Content Studio (RBAC)",
            categoryBadge = "Administration",
            estimatedTime = "Admin workflow",
            summary = "Role-restricted studio for publishing curated shared lessons and speaking challenges to the community catalog.",
            howToUseSteps = listOf(
                "Publish curated shared lessons linked to any curriculum module slug.",
                "Publish daily speaking challenges with prompts and framework hints for learners."
            ),
            whatImproves = listOf(
                "Ensures shared community lessons and challenges stay curated, accurate, and high-quality.",
                "Enforces Firestore RBAC security so only authorized adminUsers/<uid> records can publish global changes."
            ),
            coachTip = "Double-check lesson instructions and practice prompts before publishing globally."
        )
    )

    fun forRoute(route: String): FeatureTutorial {
        val normalized = when {
            route.startsWith("module_lessons") -> "module_lessons"
            route.startsWith("lesson_detail") -> "lesson_detail"
            else -> route
        }
        return allTutorials.firstOrNull { it.featureKey == normalized }
            ?: allTutorials.first { it.featureKey == "dashboard" }
    }
}
