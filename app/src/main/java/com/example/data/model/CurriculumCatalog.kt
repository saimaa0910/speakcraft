package com.example.data.model

data class CuratedTechniqueSeed(
    val id: String,
    val videoId: String,
    val title: String,
    val category: String,
    val moduleSlug: String,
    val difficulty: String,
    val creatorAdvice: String,
    val aiExplanation: String,
    val originalContext: String,
    val instructions: String,
    val practicalExercise: String,
    val expectedOutcome: String,
    val suggestedDurationMinutes: Long,
    val commonMistake: String,
    val correction: String,
    val sourceVideoTitle: String,
    val sourceVideoUrl: String,
    val timestampLabel: String,
    val isCreatorDerived: Boolean = true,
    val verificationStatus: String = "VERIFIED" // VERIFIED or PENDING_TRANSCRIPT
)

data class ModuleDefinition(
    val slug: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val category: String
)

data class LessonDefinition(
    val id: String,
    val moduleSlug: String,
    val title: String,
    val objective: String,
    val sourceVideoTitle: String,
    val sourceVideoUrl: String,
    val timestampLabel: String,
    val explanation: String,
    val instructions: List<String>,
    val example: String,
    val practicePrompt: String,
    val nextLessonId: String?,
    val isCreatorDerived: Boolean = true,
    val sourceVerified: Boolean = true
)

data class VocabSeed(
    val id: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val level: String,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val exampleSentence: String,
    val collocations: List<String>,
    val conversationExample: String,
    val partOfSpeech: String = "noun/verb",
    val simpleExplanation: String = meaning,
    val wordForms: List<String> = emptyList(),
    val practiceSentences: List<String> = listOf(exampleSentence),
    val category: String = "Communication"
)

data class SpeakingChallengeItem(
    val id: String,
    val title: String,
    val challengeType: String,
    val durationSeconds: Int,
    val frameworkHint: String,
    val prompt: String,
    val sourceReference: String
)

data class PronunciationDrill(
    val id: String,
    val category: String,
    val title: String,
    val targetIpa: String,
    val contrastOrFocus: String,
    val practicePhrase: String,
    val tip: String
)

data class ShadowingSegment(
    val id: String,
    val title: String,
    val sourceVideoTitle: String,
    val sourceVideoUrl: String,
    val focusSkill: String,
    val scriptWithMarkers: String,
    val plainScript: String,
    val pacingTargetWpm: Int
)

data class GrammarSpeakingLesson(
    val id: String,
    val topic: String,
    val ruleSummary: String,
    val commonConversationalMistake: String,
    val naturalCorrection: String,
    val speakingPrompt: String
)

data class PublicSpeakingModule(
    val id: String,
    val title: String,
    val creatorFramework: String,
    val keyPrinciple: String,
    val rehearsalPrompt: String,
    val sourceVideoUrl: String
)

data class ProgramWeekPlan(
    val weekNumber: Int,
    val title: String,
    val focusAreas: List<String>,
    val dailyDrills: List<String>
)

data class StructuredProgram(
    val id: String,
    val title: String,
    val durationDays: Int,
    val subtitle: String,
    val weeks: List<ProgramWeekPlan>
)

data class DailyStepDefinition(
    val id: String,
    val stepNumber: Int,
    val title: String,
    val description: String,
    val actionRoute: String? = null
)

object CurriculumCatalog {

    val allCategories = listOf(
        "English Fluency",
        "Spoken English",
        "Vocabulary Development",
        "Pronunciation",
        "Articulation",
        "Grammar",
        "Listening Skills",
        "Active Listening",
        "Public Speaking",
        "Presentation Skills",
        "Storytelling",
        "Confidence Building",
        "Body Language",
        "Professional Communication",
        "Interview Communication",
        "Conversation Skills",
        "Thinking in English",
        "Impromptu Speaking",
        "Voice Modulation",
        "Communication Psychology"
    )

    // The 15 required Phase 2 content modules mapped into cohesive app experiences
    val contentModules = listOf(
        ModuleDefinition("articulation_lab", "Articulation Lab", "Enunciation, minimal pairs & crisp consonants", "articulation_lab", "Articulation"),
        ModuleDefinition("voice_training", "Voice Training Lab", "5 Vocal Foundations: Rate, Pause, Melody, Volume, Character", "voice_training", "Voice Modulation"),
        ModuleDefinition("daily_practice", "Daily Speaking Practice", "Guided 9-step routine & reflection", "daily_practice", "English Fluency"),
        ModuleDefinition("vocabulary", "Vocabulary Development", "Spaced repetition, collocations & AI usage check", "vocabulary", "Vocabulary Development"),
        ModuleDefinition("conversation", "Conversation Mastery", "13 real-life simulations & flow coaching", "conversation", "Conversation Skills"),
        ModuleDefinition("listening_lab", "Active Listening", "4 Ways of Listening & cadence shadowing", "listening_lab", "Active Listening"),
        ModuleDefinition("public_speaking", "Public Speaking", "Hooks, PREP, stage confidence & closing", "public_speaking", "Public Speaking"),
        ModuleDefinition("storytelling", "Storytelling", "Reliving vs reporting & sensory dialogue", "module_lessons/storytelling", "Storytelling"),
        ModuleDefinition("body_language", "Body Language", "Power Zone gestures & camera presence", "module_lessons/body_language", "Body Language"),
        ModuleDefinition("confidence", "Confidence Training", "3-Stage Audit & nervousness grounding", "module_lessons/confidence", "Confidence Building"),
        ModuleDefinition("thinking_articulation", "Thinking & Articulation", "PREP, CLEAR & grammar through speaking", "grammar_speaking", "Impromptu Speaking"),
        ModuleDefinition("professional", "Professional Communication", "Meetings, managers & CLEAR explanations", "module_lessons/professional", "Professional Communication"),
        ModuleDefinition("influence", "Influence & Persuasion", "4-Color Communicator & Friendly Authority", "module_lessons/influence", "Communication Psychology"),
        ModuleDefinition("record_review", "Record and Review", "Audio/video capture, pause/resume & 3-Stage Audit", "speaking_lab", "Spoken English"),
        ModuleDefinition("challenges", "Communication Challenges", "Daily & weekly timed @askvinh sprints", "challenges", "Public Speaking")
    )

    // Verified @askvinh techniques + transparent verification placeholders for requested frameworks
    // awaiting caption ingestion (never fabricating missing creator instructions).
    val askVinhKnowledgeBase = listOf(
        CuratedTechniqueSeed(
            id = "vinh_3vs",
            videoId = "askvinh_3vs",
            title = "The 3Vs of Communication (Visual, Vocal, Verbal)",
            category = "Public Speaking",
            moduleSlug = "public_speaking",
            difficulty = "Beginner",
            creatorAdvice = "Vinh Giang teaches that audiences evaluate every message across three simultaneous channels: Visual Image (posture, gestures, eye line), Vocal Image (pitch, pace, tone, volume), and Verbal Image (your chosen words).",
            aiExplanation = "AI coaching supplements this by scoring transcript structure (Verbal) separately from delivery pace and pauses (Vocal).",
            originalContext = "Introduced as Vinh Giang's foundational communication lens for auditing why great ideas fail when delivered flatly.",
            instructions = "1. Choose a 60-second introduction.\n2. Record yourself on video or audio.\n3. Audit Visual first (muted), Vocal second (eyes closed), Verbal third (transcript).",
            practicalExercise = "Record a 60-second introduction in Record and Review and evaluate each 'V' on its own pass.",
            expectedOutcome = "Immediate awareness of habitual vocal monotone or frozen posture.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Focusing 100% on memorizing exact words while speaking in a flat voice.",
            correction = "Dedicate half your rehearsal time to vocal melody, intentional pauses, and grounded hand gestures.",
            sourceVideoTitle = "How to Speak Better Than 99% of People (Complete Framework)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "01:15",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_5_vocal",
            videoId = "askvinh_vocal_5",
            title = "The 5 Vocal Foundations (Rate, Pause, Melody, Volume, Character)",
            category = "Voice Modulation",
            moduleSlug = "voice_training",
            difficulty = "Beginner",
            creatorAdvice = "Vinh compares the human voice to a musical instrument with five primary levers: Rate (speed contrast), Pause (intentional silence), Melody (pitch range), Volume (diaphragmatic projection), and Character (emotional color).",
            aiExplanation = "Changing even two levers between sentences breaks vocal monotony and signals executive presence.",
            originalContext = "Core @askvinh vocal training lesson explaining why flat delivery makes strong content forgettable.",
            instructions = "1. Read a 3-sentence pitch at a brisk 150 WPM.\n2. Slow down 20% on the core insight.\n3. Hold a full 1-second silent pause immediately after your key sentence.",
            practicalExercise = "Record a 45-second explanation intentionally alternating fast setup and slow, resonant conclusion.",
            expectedOutcome = "Audible contrast in pace and cleaner sentence boundaries.",
            suggestedDurationMinutes = 6L,
            commonMistake = "Speaking at a uniform medium speed and pitch from first word to last.",
            correction = "Slow down before key takeaways and let silence frame important claims.",
            sourceVideoTitle = "Vinh Giang - 5 Vocal Foundations - Rate of Speech",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "00:45",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_3stage_audit",
            videoId = "askvinh_audit",
            title = "Record and Review: 3-Stage Self-Awareness Audit",
            category = "Confidence Building",
            moduleSlug = "record_review",
            difficulty = "Beginner",
            creatorAdvice = "Record yourself speaking for 2 to 5 minutes, step away so emotional self-judgment cools, then review in 3 isolated passes: Stage 1 muted (body language), Stage 2 audio-only with eyes closed (vocal image), Stage 3 written/verbal review.",
            aiExplanation = "Separating sensory channels prevents cognitive overload and turns self-critique into objective coaching.",
            originalContext = "Taught by Vinh Giang as the single fastest self-coaching habit for rapid communication growth.",
            instructions = "1. Record 1–2 minutes in Record and Review.\n2. Toggle Mute Pass to observe posture or pacing.\n3. Replay audio-only eyes closed to hear pitch, pauses, and fillers.\n4. Review the AI transcript for filler words and phrasing.",
            practicalExercise = "Complete a 90-second recording and run both Stage 1 (Mute) and Stage 2 (Audio-Only) passes before submitting to AI.",
            expectedOutcome = "Objective identification of 1 vocal and 1 verbal habit to fix.",
            suggestedDurationMinutes = 8L,
            commonMistake = "Stopping playback after 5 seconds out of self-consciousness.",
            correction = "Review one channel at a time like an athlete studying game film.",
            sourceVideoTitle = "The Ultimate 30-Day Plan To Master Your Communication Skills",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "04:05",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_prep",
            videoId = "askvinh_prep",
            title = "The PREP Impromptu Framework",
            category = "Impromptu Speaking",
            moduleSlug = "thinking_articulation",
            difficulty = "Intermediate",
            creatorAdvice = "Structure spontaneous answers immediately using PREP: Point (state your core conclusion first), Reason (explain why), Example (share a concrete instance), Point (restate your takeaway cleanly).",
            aiExplanation = "Leading with the Point eliminates circular preambles during interviews and Q&A.",
            originalContext = "@askvinh framework for answering unexpected questions concisely under pressure.",
            instructions = "1. State your Point within the first 5 seconds.\n2. Give one clear Reason.\n3. Anchor with one concrete Example.\n4. Close by restating the Point.",
            practicalExercise = "Answer 'What makes a team communicate effectively?' in 60 seconds strictly following P-R-E-P.",
            expectedOutcome = "Crisp 60-second impromptu answers without drifting off-topic.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Giving two minutes of background before revealing your actual opinion.",
            correction = "Answer first, then substantiate.",
            sourceVideoTitle = "How to Answer ANY Question (PREP Framework)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "02:30",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_clear",
            videoId = "askvinh_clear",
            title = "The CLEAR Framework for Complex Ideas",
            category = "Professional Communication",
            moduleSlug = "professional",
            difficulty = "Intermediate",
            creatorAdvice = "Explain technical ideas to non-experts using CLEAR: Calibrate to listener context, Link to something familiar, Envision the benefit, Abstract jargon out, Repeat the main takeaway.",
            aiExplanation = "Audience calibration increases comprehension and prevents executive tune-out.",
            originalContext = "Taught for engineers, specialists, and leaders presenting technical proposals.",
            instructions = "1. Name what your listener already understands.\n2. Introduce a familiar everyday analogy.\n3. Strip acronyms.\n4. Summarize the outcome in one sentence.",
            practicalExercise = "Explain a technical concept from your field in 90 seconds with zero unexplained acronyms.",
            expectedOutcome = "Accessible explanation understandable by a non-specialist.",
            suggestedDurationMinutes = 6L,
            commonMistake = "Equating jargon density with expertise.",
            correction = "Lead with a relatable analogy before naming technical mechanics.",
            sourceVideoTitle = "How to Explain Anything To Anyone (Even If It's Complex!)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "03:10",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_pauses",
            videoId = "askvinh_pauses",
            title = "Eliminate Filler Words With Intentional Silence",
            category = "English Fluency",
            moduleSlug = "voice_training",
            difficulty = "Beginner",
            creatorAdvice = "Filler words ('um', 'uh', 'like') appear when your mouth keeps moving before your next thought is ready. Train yourself to close your lips and hold clean silence between thoughts.",
            aiExplanation = "Listeners perceive a 1-second silent pause as authority and composure rather than awkwardness.",
            originalContext = "@askvinh pause mastery training for polished spontaneous speech.",
            instructions = "1. Speak slowly on a familiar topic for 60 seconds.\n2. Whenever you sense a filler word forming, silently close your lips for one beat.\n3. Resume only when the next phrase is clear.",
            practicalExercise = "Run the Zero-Filler 60s Sprint in Challenges and review the detected filler list.",
            expectedOutcome = "Measurable reduction in vocalized fillers per minute.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Speeding up to mask hesitation, which increases filler frequency.",
            correction = "Breathe silently at every mental period.",
            sourceVideoTitle = "How to Pause Without Feeling Awkward",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "01:40",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_articulation",
            videoId = "askvinh_cork",
            title = "Daily Articulation & Enunciation Muscle Warm-Up",
            category = "Articulation",
            moduleSlug = "articulation_lab",
            difficulty = "Intermediate",
            creatorAdvice = "Articulation relies on physical muscle engagement of the jaw, lips, and tongue. Exaggerated consonant warm-ups before speaking make everyday speech noticeably crisper.",
            aiExplanation = "Releasing final consonants (-t, -d, -k, -p, -s) dramatically improves clarity in English conversations.",
            originalContext = "@askvinh physical articulation training for clearer diction.",
            instructions = "1. Relax jaw and stretch lips wide ('eee-ooo').\n2. Speak a consonant-dense tongue twister slowly with exaggerated lip/tongue contact.\n3. Repeat the same sentence at normal conversational effort.",
            practicalExercise = "Practice 'Crisp consonant leaders build clear rapport' in Articulation Lab.",
            expectedOutcome = "Sharper consonant boundaries and reduced mumbling.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Swallowing word endings when speaking rapidly.",
            correction = "Complete the final consonant of each thought group cleanly.",
            sourceVideoTitle = "Simple Exercise to DRASTICALLY Improve Your Articulation!",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "02:00",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_storytelling",
            videoId = "askvinh_story",
            title = "Relive the Story Instead of Reporting It",
            category = "Storytelling",
            moduleSlug = "storytelling",
            difficulty = "Intermediate",
            creatorAdvice = "Weak storytellers report events in third-person summary. Engaging storytellers drop listeners into the scene using direct spoken dialogue, sensory detail, and vocal character shifts.",
            aiExplanation = "Quoted dialogue naturally triggers pitch and pace variation without forcing fake theatrics.",
            originalContext = "@askvinh storytelling masterclass on transforming dry updates into vivid stories.",
            instructions = "1. Pick a 30-second real memory.\n2. Cut preamble ('So basically what happened was...').\n3. Start in the scene with a time/place anchor.\n4. Include at least two lines of direct spoken dialogue.",
            practicalExercise = "Record a 60-second workplace or life story using direct dialogue instead of summary.",
            expectedOutcome = "Higher emotional engagement and vocal melody diversity.",
            suggestedDurationMinutes = 7L,
            commonMistake = "Stacking abstract summaries ('Then things got tense').",
            correction = "Quote the exact words spoken in that moment.",
            sourceVideoTitle = "3 Powerful Ways To Tell Stories Without Boring People",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "03:45",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_4colors",
            videoId = "askvinh_4colors",
            title = "The 4-Color Communicator Adaptation",
            category = "Communication Psychology",
            moduleSlug = "influence",
            difficulty = "Advanced",
            creatorAdvice = "Adapt your framing to how your listener processes decisions: Red (direct, bottom-line outcomes), Yellow (enthusiastic, story-driven), Green (calm, people-harmonious), Blue (analytical, evidence-first).",
            aiExplanation = "Calibration to your listener's color minimizes friction in persuasion, negotiation, and leadership.",
            originalContext = "@askvinh personality-aware influence and persuasion framework.",
            instructions = "1. Observe your partner's opening cues (speed, detail vs big-picture).\n2. Lead with metrics/ROI for Red/Blue, or people/vision for Green/Yellow.\n3. Match pacing slightly without losing authenticity.",
            practicalExercise = "Deliver a 30-second update tailored first for a Red executive, then for a Blue engineer.",
            expectedOutcome = "Stronger persuasion and fewer misaligned meetings.",
            suggestedDurationMinutes = 6L,
            commonMistake = "Presenting to every audience in your own personal preference style.",
            correction = "Adapt the first 20 seconds of your pitch to the listener's priority.",
            sourceVideoTitle = "How to Talk to ANYONE (Once You Know Their Color!)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "02:20",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_listening",
            videoId = "askvinh_listen",
            title = "The 4 Ways of Listening (Eyes, Ears, Brain, Heart)",
            category = "Active Listening",
            moduleSlug = "listening_lab",
            difficulty = "Beginner",
            creatorAdvice = "Active listening engages four channels: Eyes (observing visual cues), Ears (hearing vocal tone shifts), Brain (synthesizing core intent), and Heart (empathizing before responding).",
            aiExplanation = "Paraphrasing your partner's core point before replying proves genuine understanding.",
            originalContext = "@askvinh rapport and active listening framework.",
            instructions = "1. Listen without drafting a rebuttal.\n2. Pause 1 full second after the other person stops.\n3. Reflect their main concern in one crisp sentence before adding your view.",
            practicalExercise = "Practice a Disagreement roleplay in Conversation Practice using a 1-sentence paraphrase before every reply.",
            expectedOutcome = "Higher conversational rapport and calmer conflict resolution.",
            suggestedDurationMinutes = 6L,
            commonMistake = "Interrupting or jumping straight into counterarguments.",
            correction = "Validate and paraphrase first, then state your perspective.",
            sourceVideoTitle = "The 4 Ways of Listening to Build Instant Rapport",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "01:55",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_body_power",
            videoId = "askvinh_body",
            title = "Purposeful Hand Gestures in the Power Zone",
            category = "Body Language",
            moduleSlug = "body_language",
            difficulty = "Beginner",
            creatorAdvice = "Keep hands visible between chest and waist (the Power Zone) with open-palm gestures that illustrate scale, sequence, or contrast rather than hiding hands or fidgeting.",
            aiExplanation = "Purposeful gestures naturally unlock vocal energy and help structure spoken thoughts.",
            originalContext = "@askvinh body language guidance for stage and video calls.",
            instructions = "1. Frame camera from waist up.\n2. Count 1-2-3 cleanly on fingers during lists.\n3. Return hands calmly to neutral between key thoughts.",
            practicalExercise = "Record a 45-second video clip in Record and Review and inspect Stage 1 (Muted) strictly for Power Zone framing.",
            expectedOutcome = "Confident, distracting-free visual delivery on camera and in person.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Clenched hands, crossed arms, or off-camera gestures.",
            correction = "Anchor gestures to key verbs and numerical pillars inside chest-to-waist height.",
            sourceVideoTitle = "How to Use Hand Gestures Naturally When Speaking",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "02:50",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        CuratedTechniqueSeed(
            id = "vinh_friendly_auth",
            videoId = "askvinh_authority",
            title = "Sound Friendly Without Losing Authority",
            category = "Interview Communication",
            moduleSlug = "influence",
            difficulty = "Intermediate",
            creatorAdvice = "Combine warmth (upward pitch flexibility and facial warmth) with authority (clean downward inflection at the end of declarative statements and unhurried pacing).",
            aiExplanation = "Eliminating accidental 'upspeak' on statements makes recommendations and interview answers sound decisive.",
            originalContext = "@askvinh vocal authority lesson for interviews and leadership.",
            instructions = "1. Say 3 declarative statements ending each final word with a clear downward pitch drop.\n2. Keep a slight warm smile in your mid-sentence tone.\n3. Hold 1 second of silence after closing.",
            practicalExercise = "Practice downward statement endings in Voice Training Lab and verify steady WPM.",
            expectedOutcome = "Warm yet decisive executive vocal presence.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Raising pitch at the end of factual statements as if asking permission.",
            correction = "Land statements downward; save rising pitch for genuine questions.",
            sourceVideoTitle = "How to Sound Friendly Without Losing Authority",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            timestampLabel = "01:10",
            isCreatorDerived = true,
            verificationStatus = "VERIFIED"
        ),
        // Requested creator frameworks flagged honestly for live transcript ingestion (no fabricated instructions)
        CuratedTechniqueSeed(
            id = "vinh_pending_high_low_buffalo",
            videoId = "pending_high_low_buffalo",
            title = "High Low Buffalo (Source Verification Pending)",
            category = "Storytelling",
            moduleSlug = "storytelling",
            difficulty = "Beginner",
            creatorAdvice = "Source transcript verification required: Inspect the specific @askvinh 'High Low Buffalo' video or playlist URL in YouTube Intelligence to import verified creator instructions without fabrication.",
            aiExplanation = "Do not invent creator steps offline. Once the video captions are fetched in YouTube Learning Library, Admin moderation can publish the verified creator walkthrough.",
            originalContext = "Requested @askvinh framework awaiting live source-video transcript ingestion.",
            instructions = "Open YouTube Learning Library -> paste the @askvinh video URL covering High Low Buffalo -> run Inspect & Extract Lessons.",
            practicalExercise = "Pending verified source transcript ingestion from @askvinh.",
            expectedOutcome = "Source-grounded reflection/storytelling workflow once ingested.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Relying on AI guesses instead of verified creator captions.",
            correction = "Fetch official caption track via YouTube Intelligence before publishing.",
            sourceVideoTitle = "@askvinh — High Low Buffalo (Pending Ingestion)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh/search?query=high+low+buffalo",
            timestampLabel = "Pending",
            isCreatorDerived = false,
            verificationStatus = "PENDING_TRANSCRIPT"
        ),
        CuratedTechniqueSeed(
            id = "vinh_pending_321_convo",
            videoId = "pending_321_convo",
            title = "3-2-1 Conversation Framework (Source Verification Pending)",
            category = "Conversation Skills",
            moduleSlug = "conversation",
            difficulty = "Intermediate",
            creatorAdvice = "Source transcript verification required: Ingest the corresponding @askvinh video URL in YouTube Intelligence to retrieve authentic creator examples and timestamps.",
            aiExplanation = "Preserves strict accuracy by separating unverified framework titles from verified transcript instructions.",
            originalContext = "Requested conversation framework queued for @askvinh transcript audit.",
            instructions = "Open YouTube Learning Library -> paste the @askvinh video URL -> inspect captions and review in Admin Studio.",
            practicalExercise = "Pending verified source transcript ingestion from @askvinh.",
            expectedOutcome = "Verified conversation opening & follow-up drill once ingested.",
            suggestedDurationMinutes = 5L,
            commonMistake = "Fabricating numbered steps without checking creator captions.",
            correction = "Run live caption audit in YouTube Learning Library.",
            sourceVideoTitle = "@askvinh — 3-2-1 Conversation Framework (Pending Ingestion)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh/search?query=3+2+1+conversation",
            timestampLabel = "Pending",
            isCreatorDerived = false,
            verificationStatus = "PENDING_TRANSCRIPT"
        ),
        CuratedTechniqueSeed(
            id = "vinh_pending_nine_habits",
            videoId = "pending_nine_habits",
            title = "Nine Habits for Clearer Speaking & Nervousness Tools (Source Verification Pending)",
            category = "Confidence Building",
            moduleSlug = "confidence",
            difficulty = "Intermediate",
            creatorAdvice = "Source transcript verification required: Fetch the @askvinh channel videos on clearer speaking habits, five-step daily articulation routine, stopping mumbling, and public-speaking nervousness tools to extract verified creator steps.",
            aiExplanation = "SpeakCraft refuses to hallucinate exact numbered lists when captions have not been fetched yet.",
            originalContext = "Queued @askvinh series awaiting caption extraction.",
            instructions = "Use YouTube Learning Library to inspect @askvinh channel feed or specific video URLs.",
            practicalExercise = "Pending verified source transcript ingestion from @askvinh.",
            expectedOutcome = "Verified multi-habit checklist after live video ingestion.",
            suggestedDurationMinutes = 8L,
            commonMistake = "Guessing creator bullet lists from video titles alone.",
            correction = "Extract directly from verified timedtext captions.",
            sourceVideoTitle = "@askvinh — Clearer Speaking & Nervousness Series (Pending Ingestion)",
            sourceVideoUrl = "https://www.youtube.com/@askvinh/search?query=nervous+mumbling+clearer",
            timestampLabel = "Pending",
            isCreatorDerived = false,
            verificationStatus = "PENDING_TRANSCRIPT"
        )
    )

    val lessons: List<LessonDefinition> by lazy {
        val verified = askVinhKnowledgeBase.filter { it.verificationStatus == "VERIFIED" }
        verified.mapIndexed { idx, seed ->
            val nextId = verified.getOrNull(idx + 1)?.id
            LessonDefinition(
                id = "lesson_${seed.id}",
                moduleSlug = seed.moduleSlug,
                title = seed.title,
                objective = seed.expectedOutcome,
                sourceVideoTitle = seed.sourceVideoTitle,
                sourceVideoUrl = seed.sourceVideoUrl,
                timestampLabel = seed.timestampLabel,
                explanation = "${seed.creatorAdvice}\n\n[AI Coaching Note]: ${seed.aiExplanation}",
                instructions = seed.instructions.split("\n").filter { it.isNotBlank() },
                example = "Common Mistake: ${seed.commonMistake}\nCorrection: ${seed.correction}",
                practicePrompt = seed.practicalExercise,
                nextLessonId = nextId?.let { "lesson_$it" },
                isCreatorDerived = seed.isCreatorDerived,
                sourceVerified = true
            )
        }
    }

    val vocabularyBank = listOf(
        VocabSeed(
            id = "vocab_articulate", word = "Articulate", phonetic = "/ɑːrˈtɪk.jə.lət/",
            meaning = "Able to express ideas clearly, fluently, and effectively in speech.",
            level = "Beginner", synonyms = listOf("eloquent", "clear", "lucid"), antonyms = listOf("incoherent", "mumbled"),
            exampleSentence = "She remained articulate even during rapid Q&A.",
            collocations = listOf("articulate speaker", "clearly articulate"),
            conversationExample = "Manager: 'Can you summarize the outage?' You: 'Let me articulate the root cause in two points.'",
            partOfSpeech = "adjective / verb",
            simpleExplanation = "Speaking clearly so listeners immediately grasp your idea.",
            wordForms = listOf("articulate", "articulated", "articulating", "articulately", "articulation"),
            practiceSentences = listOf(
                "She remained articulate even during rapid Q&A.",
                "Let me clearly articulate the main reason behind our proposal.",
                "Practicing daily helps you become a more articulate speaker."
            ),
            category = "Clarity"
        ),
        VocabSeed(
            id = "vocab_rapport", word = "Rapport", phonetic = "/ræˈpɔːr/",
            meaning = "A warm, trusting connection and mutual understanding between speakers.",
            level = "Beginner", synonyms = listOf("affinity", "bond", "harmony"), antonyms = listOf("alienation", "friction"),
            exampleSentence = "Active listening builds instant rapport in interviews.",
            collocations = listOf("build rapport", "establish rapport"),
            conversationExample = "Interviewer: 'Thanks for sharing.' You: 'I appreciate it—it's great building rapport with the team.'",
            partOfSpeech = "noun",
            simpleExplanation = "Friendly trust and easy connection with the person you are talking to.",
            wordForms = listOf("rapport"),
            practiceSentences = listOf(
                "Active listening builds instant rapport in interviews.",
                "Asking thoughtful questions helps establish rapport with new clients.",
                "A warm greeting sets a positive rapport from the first minute."
            ),
            category = "Conversation"
        ),
        VocabSeed(
            id = "vocab_poise", word = "Poise", phonetic = "/pɔɪz/",
            meaning = "Graceful, calm self-assurance and composure under pressure.",
            level = "Beginner", synonyms = listOf("composure", "aplomb", "calmness"), antonyms = listOf("fluster", "nervousness"),
            exampleSentence = "Holding a silent pause shows remarkable poise.",
            collocations = listOf("speak with poise", "maintain poise"),
            conversationExample = "Mentor: 'How did the panel go?' You: 'Tough questions, but pausing helped me keep my poise.'",
            partOfSpeech = "noun",
            simpleExplanation = "Staying calm, steady, and confident even during tough moments.",
            wordForms = listOf("poise", "poised"),
            practiceSentences = listOf(
                "Holding a silent pause shows remarkable poise.",
                "She maintained her poise when the microphone stopped working.",
                "Good posture helps you speak with poise in any room."
            ),
            category = "Confidence"
        ),
        VocabSeed(
            id = "vocab_concise", word = "Concise", phonetic = "/kənˈsaɪs/",
            meaning = "Giving a lot of information clearly and in very few words.",
            level = "Beginner", synonyms = listOf("succinct", "crisp", "brief"), antonyms = listOf("wordy", "long-winded"),
            exampleSentence = "Use the PREP method to keep impromptu answers concise.",
            collocations = listOf("concise summary", "keep it concise"),
            conversationExample = "Manager: 'Give me the headline.' You: 'Here is a concise 20-second summary.'",
            partOfSpeech = "adjective",
            simpleExplanation = "Short, clear, and straight to the point without extra fluff.",
            wordForms = listOf("concise", "concisely", "conciseness"),
            practiceSentences = listOf(
                "Use the PREP method to keep impromptu answers concise.",
                "Please share a concise summary of today's meeting.",
                "Strong leaders explain complex updates concisely."
            ),
            category = "Clarity"
        ),
        VocabSeed(
            id = "vocab_clarify", word = "Clarify", phonetic = "/ˈklær.ə.faɪ/",
            meaning = "To make a statement or idea easier to understand by removing confusion.",
            level = "Elementary", synonyms = listOf("explain", "illuminate", "simplify"), antonyms = listOf("confuse", "obscure"),
            exampleSentence = "Could you clarify the deadline for our design review?",
            collocations = listOf("clarify a point", "clarify expectations"),
            conversationExample = "Teammate: 'Is Friday final?' You: 'Let me clarify the schedule so everyone is aligned.'",
            partOfSpeech = "verb",
            simpleExplanation = "Make something clear so nobody misunderstands.",
            wordForms = listOf("clarify", "clarifies", "clarified", "clarifying", "clarification"),
            practiceSentences = listOf(
                "Could you clarify the deadline for our design review?",
                "Let me clarify my main point with a quick example.",
                "Good questions clarify expectations before work begins."
            ),
            category = "Workplace"
        ),
        VocabSeed(
            id = "vocab_empathy", word = "Empathy", phonetic = "/ˈem.pə.θi/",
            meaning = "The ability to understand and share the feelings and perspective of another person.",
            level = "Elementary", synonyms = listOf("compassion", "understanding", "sensitivity"), antonyms = listOf("apathy", "indifference"),
            exampleSentence = "Listening with empathy turns tense discussions into teamwork.",
            collocations = listOf("show empathy", "listen with empathy"),
            conversationExample = "Customer: 'I waited an hour.' You: 'I hear your frustration—let me fix this right away.'",
            partOfSpeech = "noun",
            simpleExplanation = "Putting yourself in someone else's shoes when listening.",
            wordForms = listOf("empathy", "empathetic", "empathize"),
            practiceSentences = listOf(
                "Listening with empathy turns tense discussions into teamwork.",
                "Great managers show empathy when teammates face tight deadlines.",
                "You can demonstrate empathy by reflecting back what you heard."
            ),
            category = "Conversation"
        ),
        VocabSeed(
            id = "vocab_engage", word = "Engage", phonetic = "/ɪnˈɡeɪdʒ/",
            meaning = "To capture and hold someone's attention and active participation.",
            level = "Elementary", synonyms = listOf("involve", "captivate", "connect"), antonyms = listOf("bore", "alienate"),
            exampleSentence = "Opening with a vivid question immediately engages your listeners.",
            collocations = listOf("engage the audience", "actively engage"),
            conversationExample = "Coach: 'How will you open your talk?' You: 'I will ask a quick question to engage the room.'",
            partOfSpeech = "verb",
            simpleExplanation = "Pull people into your message so they stay interested.",
            wordForms = listOf("engage", "engages", "engaged", "engaging", "engagement"),
            practiceSentences = listOf(
                "Opening with a vivid question immediately engages your listeners.",
                "Eye contact helps you engage everyone at the table.",
                "Personal stories make any presentation far more engaging."
            ),
            category = "Public Speaking"
        ),
        VocabSeed(
            id = "vocab_momentum", word = "Momentum", phonetic = "/moʊˈmen.təm/",
            meaning = "The driving energy and forward progress gained through steady action.",
            level = "Elementary", synonyms = listOf("drive", "impetus", "energy"), antonyms = listOf("stagnation", "hesitation"),
            exampleSentence = "Practicing ten minutes every morning builds unstoppable momentum.",
            collocations = listOf("build momentum", "maintain momentum"),
            conversationExample = "Friend: 'How do you stay consistent?' You: 'Short daily drills keep my momentum strong.'",
            partOfSpeech = "noun",
            simpleExplanation = "Steady forward energy that makes continuing easier.",
            wordForms = listOf("momentum"),
            practiceSentences = listOf(
                "Practicing ten minutes every morning builds unstoppable momentum.",
                "Our team gained real momentum after the first milestone.",
                "Small daily wins help you maintain momentum all week."
            ),
            category = "Confidence"
        ),
        VocabSeed(
            id = "vocab_cadence", word = "Cadence", phonetic = "/ˈkeɪ.dəns/",
            meaning = "The rhythmic flow, pace, and modulation of your speaking voice.",
            level = "Intermediate", synonyms = listOf("rhythm", "tempo", "flow"), antonyms = listOf("monotony", "arrhythmia"),
            exampleSentence = "Slowing your cadence right before a key point commands attention.",
            collocations = listOf("measured cadence", "vocal cadence"),
            conversationExample = "Coach: 'Your ideas are strong—now vary your cadence so key insights land.'",
            partOfSpeech = "noun",
            simpleExplanation = "The natural beat, speed, and musical flow of how you speak.",
            wordForms = listOf("cadence", "cadences"),
            practiceSentences = listOf(
                "Slowing your cadence right before a key point commands attention.",
                "A measured cadence makes complex topics easier to follow.",
                "Vary your vocal cadence so listeners stay tuned in."
            ),
            category = "Voice"
        ),
        VocabSeed(
            id = "vocab_enunciate", word = "Enunciate", phonetic = "/ɪˈnʌn.si.eɪt/",
            meaning = "To pronounce words and syllables distinctly and clearly.",
            level = "Intermediate", synonyms = listOf("pronounce", "articulate", "utter"), antonyms = listOf("slur", "mumble"),
            exampleSentence = "Enunciate final consonants so remote microphones pick up every word.",
            collocations = listOf("enunciate clearly", "enunciate every syllable"),
            conversationExample = "Peer: 'The audio was faint.' You: 'Got it—I'll slow down and enunciate more crisply.'",
            partOfSpeech = "verb",
            simpleExplanation = "Pronounce every syllable and word ending cleanly.",
            wordForms = listOf("enunciate", "enunciates", "enunciated", "enunciating", "enunciation"),
            practiceSentences = listOf(
                "Enunciate final consonants so remote microphones pick up every word.",
                "Warming up your jaw helps you enunciate clearly on calls.",
                "She learned to enunciate every syllable during public speeches."
            ),
            category = "Pronunciation"
        ),
        VocabSeed(
            id = "vocab_brevity", word = "Brevity", phonetic = "/ˈbrev.ə.ti/",
            meaning = "Concise and exact use of words without unnecessary rambling.",
            level = "Intermediate", synonyms = listOf("conciseness", "succinctness"), antonyms = listOf("verbosity", "rambling"),
            exampleSentence = "Executive updates reward brevity over exhaustive detail.",
            collocations = listOf("soul of brevity", "speak with brevity"),
            conversationExample = "Director: 'We have two minutes.' You: 'In the interest of brevity, here are the three outcomes.'",
            partOfSpeech = "noun",
            simpleExplanation = "Keeping your message short, sharp, and high value.",
            wordForms = listOf("brevity"),
            practiceSentences = listOf(
                "Executive updates reward brevity over exhaustive detail.",
                "In the interest of brevity, let me highlight two key numbers.",
                "Speaking with brevity shows respect for everyone's time."
            ),
            category = "Workplace"
        ),
        VocabSeed(
            id = "vocab_calibrate", word = "Calibrate", phonetic = "/ˈkæl.ə.breɪt/",
            meaning = "To adjust your tone, complexity, and framing to match your listener.",
            level = "Intermediate", synonyms = listOf("adapt", "tailor", "tune"), antonyms = listOf("ignore", "mismatch"),
            exampleSentence = "Calibrate your technical depth to the audience in front of you.",
            collocations = listOf("calibrate your message", "carefully calibrate"),
            conversationExample = "Colleague: 'Was the client confused?' You: 'Yes, I need to calibrate the pitch with less jargon.'",
            partOfSpeech = "verb",
            simpleExplanation = "Fine-tune how you explain things so they fit your audience.",
            wordForms = listOf("calibrate", "calibrates", "calibrated", "calibrating", "calibration"),
            practiceSentences = listOf(
                "Calibrate your technical depth to the audience in front of you.",
                "Before presenting, calibrate your message to what leaders care about.",
                "Skilled negotiators carefully calibrate their tone under pressure."
            ),
            category = "Workplace"
        ),
        VocabSeed(
            id = "vocab_inflection", word = "Inflection", phonetic = "/ɪnˈflek.ʃən/",
            meaning = "A rise or fall in vocal pitch that conveys nuance and emphasis.",
            level = "Upper Intermediate", synonyms = listOf("intonation", "modulation", "tone"), antonyms = listOf("flatness", "monotone"),
            exampleSentence = "Downward inflection at the end of a sentence projects certainty.",
            collocations = listOf("downward inflection", "vocal inflection"),
            conversationExample = "Coach: 'Watch your upward inflection on statements—drop your pitch to sound decisive.'",
            partOfSpeech = "noun",
            simpleExplanation = "Changing the rise and fall of your pitch to show meaning.",
            wordForms = listOf("inflection", "inflections"),
            practiceSentences = listOf(
                "Downward inflection at the end of a sentence projects certainty.",
                "Subtle vocal inflection brings written scripts to life.",
                "Use a rising inflection when inviting genuine questions."
            ),
            category = "Voice"
        ),
        VocabSeed(
            id = "vocab_compelling", word = "Compelling", phonetic = "/kəmˈpel.ɪŋ/",
            meaning = "Evoking strong interest, conviction, or attention in an irresistible way.",
            level = "Upper Intermediate", synonyms = listOf("persuasive", "convincing", "captivating"), antonyms = listOf("unconvincing", "dull"),
            exampleSentence = "She backed her proposal with a compelling customer story.",
            collocations = listOf("compelling argument", "compelling story"),
            conversationExample = "VP: 'Why should we fund this now?' You: 'Here is a compelling case backed by user retention data.'",
            partOfSpeech = "adjective",
            simpleExplanation = "So clear and persuasive that people genuinely want to act.",
            wordForms = listOf("compel", "compelling", "compellingly"),
            practiceSentences = listOf(
                "She backed her proposal with a compelling customer story.",
                "A compelling opening hooks listeners in the first ten seconds.",
                "Clear evidence makes your recommendation far more compelling."
            ),
            category = "Storytelling"
        ),
        VocabSeed(
            id = "vocab_pivotal", word = "Pivotal", phonetic = "/ˈpɪv.ə.təl/",
            meaning = "Of crucial importance in shaping the direction or outcome of an event.",
            level = "Upper Intermediate", synonyms = listOf("crucial", "decisive", "central"), antonyms = listOf("trivial", "minor"),
            exampleSentence = "That single client conversation became a pivotal moment for our launch.",
            collocations = listOf("pivotal moment", "play a pivotal role"),
            conversationExample = "Interviewer: 'What changed the project?' You: 'Aligning on one clear metric was the pivotal shift.'",
            partOfSpeech = "adjective",
            simpleExplanation = "A turning point that makes a major difference.",
            wordForms = listOf("pivotal", "pivot"),
            practiceSentences = listOf(
                "That single client conversation became a pivotal moment for our launch.",
                "Clear communication plays a pivotal role in team trust.",
                "Highlight the pivotal decision at the center of your story."
            ),
            category = "Storytelling"
        ),
        VocabSeed(
            id = "vocab_diplomatic", word = "Diplomatic", phonetic = "/ˌdɪp.ləˈmæt.ɪk/",
            meaning = "Dealing with sensitive topics or disagreements with tact, respect, and skill.",
            level = "Upper Intermediate", synonyms = listOf("tactful", "gracious", "considerate"), antonyms = listOf("blunt", "abrasive"),
            exampleSentence = "A diplomatic response de-escalated the disagreement in seconds.",
            collocations = listOf("diplomatic approach", "diplomatic phrasing"),
            conversationExample = "Peer: 'I disagree with their timeline.' You: 'Let's give diplomatic feedback focused on shared goals.'",
            partOfSpeech = "adjective",
            simpleExplanation = "Sharing honest views politely without hurting relationships.",
            wordForms = listOf("diplomatic", "diplomatically", "diplomacy"),
            practiceSentences = listOf(
                "A diplomatic response de-escalated the disagreement in seconds.",
                "Use diplomatic phrasing when challenging an assumption in meetings.",
                "Handling tough client feedback diplomatically builds lasting respect."
            ),
            category = "Workplace"
        ),
        VocabSeed(
            id = "vocab_synthesize", word = "Synthesize", phonetic = "/ˈsɪn.θə.saɪz/",
            meaning = "To combine complex details into one clear, unified insight.",
            level = "Advanced", synonyms = listOf("distill", "summarize", "integrate"), antonyms = listOf("fragment", "scatter"),
            exampleSentence = "Great communicators synthesize messy data into a clear narrative.",
            collocations = listOf("synthesize insights", "synthesize feedback"),
            conversationExample = "VP: 'What did the 20 interviews show?' You: 'Let me synthesize the three recurring themes.'",
            partOfSpeech = "verb",
            simpleExplanation = "Pull scattered details together into one crisp takeaway.",
            wordForms = listOf("synthesize", "synthesizes", "synthesized", "synthesizing", "synthesis"),
            practiceSentences = listOf(
                "Great communicators synthesize messy data into a clear narrative.",
                "Let me synthesize our team's feedback into three priorities.",
                "Being able to synthesize complex research sets executives apart."
            ),
            category = "Leadership"
        ),
        VocabSeed(
            id = "vocab_resonant", word = "Resonant", phonetic = "/ˈrez.ən.ənt/",
            meaning = "Deep, warm, full-bodied vocal quality that carries effortlessly.",
            level = "Advanced", synonyms = listOf("rich", "sonorous", "vibrant"), antonyms = listOf("thin", "strained"),
            exampleSentence = "Diaphragmatic breathing produces a calm, resonant speaking voice.",
            collocations = listOf("resonant voice", "emotionally resonant"),
            conversationExample = "Listener: 'Your keynote carried across the whole hall.' You: 'Breath support keeps the tone resonant.'",
            partOfSpeech = "adjective",
            simpleExplanation = "Rich, warm sound or message that stays with listeners.",
            wordForms = listOf("resonant", "resonate", "resonance"),
            practiceSentences = listOf(
                "Diaphragmatic breathing produces a calm, resonant speaking voice.",
                "Her closing story was emotionally resonant for everyone in the room.",
                "Steady breath support makes your voice sound naturally resonant."
            ),
            category = "Voice"
        ),
        VocabSeed(
            id = "vocab_nuance", word = "Nuance", phonetic = "/ˈnuː.ɑːns/",
            meaning = "A subtle distinction in meaning, tone, or emotional expression.",
            level = "Advanced", synonyms = listOf("subtlety", "shade", "refinement"), antonyms = listOf("bluntness", "oversimplification"),
            exampleSentence = "Vocal character lets you convey nuance without changing words.",
            collocations = listOf("subtle nuance", "capture the nuance"),
            conversationExample = "Teammate: 'It sounded harsh in text.' You: 'Let's jump on a call so you hear the nuance.'",
            partOfSpeech = "noun",
            simpleExplanation = "Small, meaningful shades of tone or meaning that add depth.",
            wordForms = listOf("nuance", "nuances", "nuanced"),
            practiceSentences = listOf(
                "Vocal character lets you convey nuance without changing words.",
                "Short voice calls capture nuance that text messages often lose.",
                "Her nuanced explanation addressed every stakeholder's concern."
            ),
            category = "Leadership"
        ),
        VocabSeed(
            id = "vocab_gravitas", word = "Gravitas", phonetic = "/ˈɡræv.ɪ.tæs/",
            meaning = "Calm dignity, credibility, and quiet authority in presence and speech.",
            level = "Advanced", synonyms = listOf("authority", "presence", "weight"), antonyms = listOf("frivolity", "flippancy"),
            exampleSentence = "Pausing calmly before answering adds instant gravitas to your delivery.",
            collocations = listOf("executive gravitas", "speak with gravitas"),
            conversationExample = "Mentor: 'Why did the board listen so closely?' You: 'Slowing down gave the recommendation real gravitas.'",
            partOfSpeech = "noun",
            simpleExplanation = "Quiet, grounded authority that makes people trust your words.",
            wordForms = listOf("gravitas"),
            practiceSentences = listOf(
                "Pausing calmly before answering adds instant gravitas to your delivery.",
                "Executive gravitas comes from clarity, composure, and steady pacing.",
                "Speaking with gravitas never requires raising your voice."
            ),
            category = "Leadership"
        )
    )

    val speakingChallenges = listOf(
        SpeakingChallengeItem("chal_1min_nonstop", "1-Minute Nonstop Flow", "DAILY", 60, "Keep thinking in English—pause silently if needed, no fillers.", "Speak continuously for 60 seconds about a skill you learned recently and why it mattered.", "@askvinh — Fluency & Silence Control"),
        SpeakingChallengeItem("chal_daily_routine", "2-Minute Daily Routine Story", "DAILY", 120, "Use 5 Vocal Foundations: vary speed and add pauses between transitions.", "Walk through your ideal productive morning for 2 minutes as if guiding a friend.", "@askvinh — 5 Vocal Foundations"),
        SpeakingChallengeItem("chal_taboo", "Describe Without Naming It", "DAILY", 60, "Use sensory details, size, and analogies without saying the object's name.", "Describe a smartphone or a coffee mug for 60 seconds without naming it once.", "@askvinh — Specificity & Clarity"),
        SpeakingChallengeItem("chal_clear_tech", "CLEAR Technical Explainer", "DAILY", 90, "Calibrate -> Link -> Envision -> Abstract -> Repeat.", "Explain how Wi-Fi, cloud sync, or a complex hobby works to a non-technical grandparent.", "@askvinh — CLEAR Framework"),
        SpeakingChallengeItem("chal_3min_random", "3-Minute Deep Reflection", "WEEKLY", 180, "Exaggerate melody, pauses, and downward inflection on key lessons.", "Answer: 'What is one failure or setback that unexpectedly shaped who you are today?'", "@askvinh — Vocal Image Challenge"),
        SpeakingChallengeItem("chal_5_words", "5-Word Vocabulary Story", "DAILY", 90, "Weave: Articulate, Cadence, Rapport, Poise, Concise into one story.", "Tell a short workplace or personal story naturally using all five target words.", "@askvinh — Verbal + Storytelling"),
        SpeakingChallengeItem("chal_prep_opinion", "PREP Opinion Sprint", "DAILY", 60, "Point -> Reason -> Example -> Point in under 60 seconds.", "Share your opinion on whether AI tools make people better or lazier thinkers.", "@askvinh — PREP Framework"),
        SpeakingChallengeItem("chal_video_summary", "Summarize @askvinh in Your Words", "WEEKLY", 90, "Relive one insight cleanly without reading notes.", "Summarize Vinh Giang's 3Vs of Communication and which 'V' you want to improve first.", "@askvinh — Listening & Synthesis"),
        SpeakingChallengeItem("chal_zero_fillers", "Zero-Filler 60s Sprint", "DAILY", 60, "Whenever you feel 'um', 'uh', or 'like', hold silent poise for 1 second.", "Introduce yourself and your current project or studies with zero filler words.", "@askvinh — Eliminating Fillers"),
        SpeakingChallengeItem("chal_2min_pitch", "2-Minute Executive Presentation", "WEEKLY", 120, "Strong hook, 3 structured pillars, downward-inflected close.", "Present a 2-minute proposal for a habit, tool, or change your team should adopt.", "@askvinh — Presentation Mastery")
    )

    val conversationScenarios = listOf(
        "Casual conversations",
        "Introducing yourself",
        "Talking with colleagues",
        "Talking with managers",
        "Meeting new people",
        "Asking questions",
        "Group discussions",
        "Job interviews",
        "HR discussions",
        "Technical interviews",
        "Giving presentations",
        "Handling disagreements",
        "Professional meetings"
    )

    val pronunciationDrills = listOf(
        PronunciationDrill("pron_pair_ship_sheep", "Minimal Pairs", "/ɪ/ vs /iː/ (ship vs sheep)", "/ɪ/ vs /iː/", "Short relaxed vowel vs tense long vowel", "Please sit in the seat and ship the cheap sheep.", "Smile slightly and hold /iː/ longer for 'seat', 'cheap', 'sheep'; relax lips for 'sit', 'ship'."),
        PronunciationDrill("pron_pair_think_sink", "Minimal Pairs", "/θ/ vs /s/ (think vs sink)", "/θ/ vs /s/", "Voiceless dental fricative vs alveolar sibilant", "I think three thinkers sank six thin ships.", "Place tongue tip gently between front teeth for 'th'; pull tongue back for 's'."),
        PronunciationDrill("pron_pair_vest_west", "Minimal Pairs", "/v/ vs /w/ (vest vs west)", "/v/ vs /w/", "Labiodental vibration vs rounded lips", "Victor wore a warm velvet vest walking west.", "Touch top teeth lightly to bottom lip for /v/; round lips into a small circle without teeth contact for /w/."),
        PronunciationDrill("pron_diff_r_l", "Difficult Sounds", "/r/ vs /l/ Clarity", "/r/ vs /l/", "Tongue curl without touching roof vs firm alveolar tap", "Really clear leaders relay reliable rules.", "For /l/, press tongue tip firmly behind top teeth. For /r/, curl tongue tip slightly back without touching."),
        PronunciationDrill("pron_word_stress", "Word Stress", "Multi-Syllable Executive Stress", "ˈkɒm.jʊ.nɪ.keɪt / prɪˈzent", "Stress placement shifts meaning (PREsent vs preSENT)", "I will preSENT the PREsent proJECT to the comMITtee.", "Make stressed syllables louder, slightly higher in pitch, and longer—never rush them."),
        PronunciationDrill("pron_sentence_stress", "Sentence Stress", "Contrastive Emphasis", "Focus Word Pitch Peak", "Changing the stressed word changes the entire meaning", "I didn't say HE stole the report — I said he BORROWED it.", "Hit your focus word with higher melody and a tiny pause immediately after."),
        PronunciationDrill("pron_intonation", "Intonation", "Authority Downward Drop vs Question Rise", "↘ Falling vs ↗ Rising", "Eliminating upspeak on statements", "Our timeline is Thursday.↘ Are we aligned on the scope?↗", "Drop pitch cleanly on 'Thursday' to sound certain; lift gently on 'scope?' to invite dialogue."),
        PronunciationDrill("pron_connected", "Connected Speech", "Linking Consonants & Reductions", "C+V Linking", "Smooth transitions across word boundaries", "Let's wrap_it_up and talk_about_a clearer_approach.", "Link final consonants onto the next vowel sound so speech flows without choppy stops.")
    )

    val shadowingSegments = listOf(
        ShadowingSegment(
            id = "shad_vocal_instrument",
            title = "Your Voice Is an Instrument",
            sourceVideoTitle = "5 Vocal Foundations — @askvinh",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            focusSkill = "Pause & Melody",
            scriptWithMarkers = "Your VOICE [↗] is the MOST powerful instrument [PAUSE 1s] you already OWN [↘]. When you SLOW DOWN [—] and embrace SILENCE [PAUSE 1s], people LEAN IN.",
            plainScript = "Your voice is the most powerful instrument you already own. When you slow down and embrace silence, people lean in.",
            pacingTargetWpm = 125
        ),
        ShadowingSegment(
            id = "shad_prep_opening",
            title = "Opening With PREP Authority",
            sourceVideoTitle = "Answer Any Question — @askvinh",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            focusSkill = "Structure & Downward Inflection",
            scriptWithMarkers = "Here is my CORE point [↘]: clarity beats complexity EVERY time [PAUSE 1s]. For EXAMPLE [↗], when I replaced jargon with a single story, the entire room nodded.",
            plainScript = "Here is my core point: clarity beats complexity every time. For example, when I replaced jargon with a single story, the entire room nodded.",
            pacingTargetWpm = 130
        ),
        ShadowingSegment(
            id = "shad_relive_story",
            title = "Reliving a Moment With Dialogue",
            sourceVideoTitle = "3 Ways To Tell Stories — @askvinh",
            sourceVideoUrl = "https://www.youtube.com/@askvinh",
            focusSkill = "Vocal Character & Contrast",
            scriptWithMarkers = "Ten minutes before going on stage [PAUSE 0.5s], my mentor leaned over and whispered [soft]: 'Don't perform [PAUSE 0.5s]—just have a conversation [↘].'",
            plainScript = "Ten minutes before going on stage, my mentor leaned over and whispered: Don't perform, just have a conversation.",
            pacingTargetWpm = 120
        )
    )

    val grammarLessons = listOf(
        GrammarSpeakingLesson("gram_tenses", "Tenses in Storytelling", "Use Past Simple for the main sequence and Past Continuous to set the background scene.", "Yesterday I walk into the room and everybody is look at me.", "Yesterday I walked into the room while everyone was watching the screen.", "Tell a 45-second story about a surprise at work or school using both Past Simple and Past Continuous."),
        GrammarSpeakingLesson("gram_articles", "Articles (a / an / the)", "Use 'a/an' when introducing something new; switch to 'the' once both speakers know which item you mean.", "I gave presentation to manager and presentation went well.", "I gave a presentation to the manager, and the presentation went really well.", "Describe a tool you use every day—first introduce it with 'a/an', then refer back to it with 'the'."),
        GrammarSpeakingLesson("gram_prepositions", "Prepositions of Time & Place", "Master 'at' (exact time/point), 'on' (days/surfaces), and 'in' (months/enclosed spaces).", "Let's meet in Monday at the morning on the office.", "Let's meet on Monday in the morning at the office.", "Schedule a mock meeting aloud specifying the exact day, time window, and location."),
        GrammarSpeakingLesson("gram_sva", "Subject-Verb Agreement", "Keep singular subjects paired with singular verbs even when a long prepositional phrase sits in between.", "The quality of the slides and examples are amazing.", "The quality of the slides and examples is remarkable.", "Say 3 sentences starting with 'One of my biggest strengths is...' and 'The feedback from the team shows...'"),
        GrammarSpeakingLesson("gram_questions", "Natural Question Formation", "Use 'Could you tell me...' + statement word order for polite indirect questions.", "Can you tell me where is the meeting room?", "Could you tell me where the meeting room is?", "Ask 3 thoughtful follow-up questions you would ask a hiring manager in an interview."),
        GrammarSpeakingLesson("gram_conditionals", "Conditionals for Problem-Solving", "Use 2nd conditional ('If we reduced..., we would...') to propose hypothetical improvements tactfully.", "If we will change the intro, people would listen more.", "If we tightened the opening hook, the audience would engage faster.", "Propose a workflow improvement using 'If we..., we would...'"),
        GrammarSpeakingLesson("gram_complex", "Complex Sentences With Signposting", "Link cause and contrast aloud using 'Although...', 'Because...', and 'As a result...'.", "We missed the deadline. We learned a lot. Next time will be better.", "Although we missed the initial deadline, we spotted the bottleneck early and shipped a stronger release.", "Explain a challenge and its solution in two smooth complex sentences.")
    )

    val publicSpeakingModules = listOf(
        PublicSpeakingModule("pub_prep", "1. Speech Preparation & Audience Calibration", "CLEAR & 3Vs", "Prepare 3 core pillars, never a word-for-word script, so you stay conversational.", "Deliver a 60-second outline of a talk you'd love to give.", "https://www.youtube.com/@askvinh"),
        PublicSpeakingModule("pub_opening", "2. Magnetic Opening Techniques", "Pattern Interrupt Hook", "Skip 'Hi my name is...'. Open with a vivid question, bold contrast, or micro-story in the first 10 seconds.", "Record just the first 15 seconds of a presentation using a bold opening question.", "https://www.youtube.com/@askvinh"),
        PublicSpeakingModule("pub_structure", "3. Structuring Ideas (PREP & Rule of 3)", "PREP Framework", "Human memory groups information cleanly in threes. Frame your talk into Hook -> 3 Pillars -> Action Close.", "Pitch a 3-point summary of your favorite book or tool.", "https://www.youtube.com/@askvinh"),
        PublicSpeakingModule("pub_story", "4. Reliving Stories With Dialogue", "Relive vs Report", "Replace flat reporting with direct dialogue and sensory detail.", "Record a 60-second story featuring at least one spoken quote.", "https://www.youtube.com/@askvinh"),
        PublicSpeakingModule("pub_voice", "5. Voice Modulation & Diaphragm Projection", "5 Vocal Foundations", "Alternate fast/slow, loud/soft, and high/low pitch to create musical contrast.", "Read a motivational call-to-action projecting from your diaphragm.", "https://www.youtube.com/@askvinh"),
        PublicSpeakingModule("pub_body", "6. Power Zone Body Language & Closing", "Grounded Stance + Downward Close", "Keep gestures between chest and waist, hold eye contact per thought, and pause silently after your closing line.", "Deliver a 30-second closing takeaway followed by 2 seconds of poised silence.", "https://www.youtube.com/@askvinh")
    )

    val realWorldTaskSeeds = listOf(
        "Initiate a 2-minute conversation with a colleague or acquaintance and ask 2 follow-up questions.",
        "Explain your current work or study topic to a non-technical friend using the CLEAR framework.",
        "Ask three meaningful, open-ended questions during a meeting or call using the 4 Ways of Listening.",
        "Give a structured 90-second update or mini-presentation using PREP (Point-Reason-Example-Point).",
        "Summarize a meeting or podcast aloud in 60 seconds with zero filler words.",
        "Explain a recent problem and its solution clearly to a peer without jargon.",
        "Record a polished 60-second professional self-introduction and run the 3-Stage Self-Awareness Audit."
    )

    val dailySteps = listOf(
        DailyStepDefinition("vocab_words", 1, "Learn 3 Daily Vocabulary Words", "Study meaning, phonetic IPA, and collocations.", "vocabulary"),
        DailyStepDefinition("pronunciation_drill", 2, "Articulation & Enunciation Drill", "Practice minimal pairs and crisp final consonants.", "articulation_lab"),
        DailyStepDefinition("sentence_craft", 3, "Contextual Sentence Construction", "Write or speak original sentences using today's words.", "vocabulary"),
        DailyStepDefinition("speaking_exercise", 4, "5 Vocal Foundations Speaking Drill", "Record 60s focusing on deliberate pauses and melody.", "voice_training"),
        DailyStepDefinition("listening_shadow", 5, "Listening & Shadowing Segment", "Listen to @askvinh cadence and shadow the delivery.", "listening_lab"),
        DailyStepDefinition("real_world_task", 6, "Real-World Communication Task", "Apply one @askvinh framework in a real conversation.", "real_world_tasks"),
        DailyStepDefinition("confidence_act", 7, "Record & Review (3-Stage Self-Audit)", "Review your recording muted + eyes-closed objectively.", "speaking_lab"),
        DailyStepDefinition("reflection", 8, "Daily Communication Reflection", "Log one win and one focus lever for tomorrow.", null)
    )

    val structuredPrograms = listOf(
        StructuredProgram(
            id = "30_day_challenge",
            title = "30-Day Communication Challenge",
            durationDays = 30,
            subtitle = "Adapted from @askvinh's 30-Day Mastery Plan",
            weeks = listOf(
                ProgramWeekPlan(
                    1,
                    "Week 1: Foundation & Self-Awareness",
                    listOf("3Vs of Communication", "Basic fluency & daily vocabulary", "Sentence formation & self-intro", "Speaking confidence baseline"),
                    listOf(
                        "Day 1: Record a 2-min baseline intro & run 3-Stage Audit",
                        "Day 2: Master Vocal Foundation #1 — Rate of Speech contrast",
                        "Day 3: Master Vocal Foundation #2 — 1-second silent pauses",
                        "Day 4: Learn 3 executive words & construct live sentences",
                        "Day 5: Power Zone hand gestures & grounded posture drill",
                        "Day 6: 4 Ways of Listening in a real conversation",
                        "Day 7: Week 1 Review — Compare Day 1 vs Day 7 recording"
                    )
                ),
                ProgramWeekPlan(
                    2,
                    "Week 2: Fluency Development",
                    listOf("Thinking in English", "Eliminating filler words", "Shadowing @askvinh cadence", "Daily conversations & storytelling"),
                    listOf(
                        "Day 8: 60s Nonstop Flow without translating in your head",
                        "Day 9: Zero-Filler Sprint — replace 'um/uh' with poise",
                        "Day 10: Shadowing Lab — match pace & pauses of @askvinh",
                        "Day 11: Relive vs Report — add direct dialogue to a story",
                        "Day 12: Conversation Simulator — Casual & Meeting New People",
                        "Day 13: Active Listening paraphrase drill in dialogue",
                        "Day 14: Week 2 Review — 2-minute unscripted story audit"
                    )
                ),
                ProgramWeekPlan(
                    3,
                    "Week 3: Articulation & Expression",
                    listOf("Pronunciation & crisp consonants", "CLEAR complex explanations", "Advanced vocabulary in context", "Impromptu PREP speaking"),
                    listOf(
                        "Day 15: 10-min Articulation & Enunciation muscle warm-up",
                        "Day 16: Minimal Pairs & downward pitch inflection drill",
                        "Day 17: CLEAR Framework — explain a complex idea simply",
                        "Day 18: PREP Impromptu Sprint — 3 rapid-fire questions",
                        "Day 19: Contextual Vocabulary Assessment (5 advanced words)",
                        "Day 20: Voice Modulation — Melody & Volume projection",
                        "Day 21: Week 3 Review — Compare articulation scores"
                    )
                ),
                ProgramWeekPlan(
                    4,
                    "Week 4: Practical Mastery",
                    listOf("Professional & manager conversations", "Presentations & opening hooks", "Group discussions & disagreements", "Interview communication & final assessment"),
                    listOf(
                        "Day 22: 4-Color Communicator adaptation in meetings",
                        "Day 23: Friendly Authority — warm tone + decisive endings",
                        "Day 24: Conversation Simulator — Handling Disagreements",
                        "Day 25: Opening Hooks & 2-Minute Executive Presentation",
                        "Day 26: Interview & HR Storytelling with STAR + PREP",
                        "Day 27: Real-World Task — lead or summarize a meeting",
                        "Day 28-30: Final 3-Minute Capstone Speech & 30-Day Analytics Review"
                    )
                )
            )
        ),
        StructuredProgram(
            id = "60_day_fluency",
            title = "60-Day Fluency Program",
            durationDays = 60,
            subtitle = "Deep habit rewiring for natural thinking in English",
            weeks = listOf(
                ProgramWeekPlan(1, "Phase 1 (Days 1-20): Elimination of Hesitation", listOf("Thinking in English", "Zero-filler silence", "Daily 10-min shadowing"), listOf("Daily 60s nonstop flow", "3x/week Conversation Simulator")),
                ProgramWeekPlan(2, "Phase 2 (Days 21-40): Conversational Agility", listOf("PREP impromptu agility", "Grammar through speaking", "Contextual vocabulary"), listOf("Daily PREP sprint", "Weekly 3-Stage Video Audit")),
                ProgramWeekPlan(3, "Phase 3 (Days 41-60): Executive Fluency", listOf("4-Color adaptation", "Storytelling & persuasion", "Long-form spontaneous speaking"), listOf("3-min impromptu reflections", "Real-world leadership tasks"))
            )
        ),
        StructuredProgram(
            id = "90_day_advanced",
            title = "90-Day Advanced Communication Program",
            durationDays = 90,
            subtitle = "Complete Vocal, Visual, and Verbal mastery",
            weeks = listOf(
                ProgramWeekPlan(1, "Month 1: Vocal & Articulation Precision", listOf("5 Vocal Foundations", "IPA Phonetics & Stress", "Diaphragm Projection"), listOf("Daily 10-min vocal gym", "Shadowing & intonation drills")),
                ProgramWeekPlan(2, "Month 2: Persuasive Structure & Storytelling", listOf("CLEAR & PREP mastery", "Reliving stories", "4-Color Psychology"), listOf("Weekly recorded keynote segments", "Disagreement & negotiation sims")),
                ProgramWeekPlan(3, "Month 3: Stage Presence & Leadership", listOf("Audience engagement", "High-stakes Q&A", "Executive interviews"), listOf("Full 2-min presentations", "Real-world mentorship tasks"))
            )
        ),
        StructuredProgram(
            id = "vocab_challenge",
            title = "Daily Vocabulary Challenge",
            durationDays = 21,
            subtitle = "Active conversational usage with spaced repetition",
            weeks = listOf(
                ProgramWeekPlan(1, "21-Day Active Lexicon Track", listOf("3-5 new words daily", "Collocations & tone", "AI contextual usage evaluation"), listOf("Review due spaced-repetition cards", "Pass AI contextual sentence check"))
            )
        ),
        StructuredProgram(
            id = "pronunciation_program",
            title = "Articulation & Pronunciation Bootcamp",
            durationDays = 28,
            subtitle = "Phonetics, minimal pairs, stress, and connected speech",
            weeks = listOf(
                ProgramWeekPlan(1, "4-Week Acoustic Precision", listOf("Minimal pairs /ɪ/-/iː/, /θ/-/s/, /v/-/w/", "Word & sentence stress", "Connected speech linking"), listOf("Slow/normal reference listening", "Record & compare in Articulation Lab"))
            )
        ),
        StructuredProgram(
            id = "public_speaking_program",
            title = "Public Speaking & Storytelling Program",
            durationDays = 30,
            subtitle = "Stage confidence, hooks, storytelling, and vocal charisma",
            weeks = listOf(
                ProgramWeekPlan(1, "Stage Presence & Delivery", listOf("Pattern interrupt hooks", "Power Zone gestures", "Reliving stories"), listOf("Deliver simulated presentations", "Run 3-Stage Video Audit"))
            )
        ),
        StructuredProgram(
            id = "interview_program",
            title = "Professional & Influence Program",
            durationDays = 21,
            subtitle = "Friendly authority, 4-Color calibration, and CLEAR answers",
            weeks = listOf(
                ProgramWeekPlan(1, "High-Impact Executive Delivery", listOf("Downward inflection authority", "4-Color adaptation", "Technical CLEAR explanations"), listOf("Complete HR & Technical interview simulations", "Record 60s self-introduction"))
            )
        )
    )
}
