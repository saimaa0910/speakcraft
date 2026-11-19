package com.example.data.activity

internal object ActivityCatalogueModules4To6 {

    private val defaultFeedback = FeedbackConfiguration()

    private fun clarityRubric() = listOf(
        ScoringRubricDimension("structure", "Logical Structure", 40, "Clear opening, ordered points, and crisp conclusion."),
        ScoringRubricDimension("conciseness", "Conciseness", 30, "Zero fluff, minimal filler, tight sentences."),
        ScoringRubricDimension("accessibility", "Listener Clarity", 30, "Plain wording and concrete illustrations.")
    )

    private fun conversationRubric() = listOf(
        ScoringRubricDimension("curiosity", "Curiosity & Questions", 35, "Open-ended, thread-deepening follow-ups."),
        ScoringRubricDimension("rapport", "Warmth & Rapport", 35, "Natural reciprocity, empathy, and matching."),
        ScoringRubricDimension("flow", "Conversational Flow", 30, "Smooth transitions and appropriate length.")
    )

    private fun listeningRubric() = listOf(
        ScoringRubricDimension("accuracy", "Comprehension Accuracy", 40, "Captures core facts and key details accurately."),
        ScoringRubricDimension("subtext", "Tone & Emotion Reading", 30, "Identifies underlying feelings, intent, or cues."),
        ScoringRubricDimension("reflection", "Reflective Response", 30, "Paraphrases clearly before responding.")
    )

    private fun buildActivity(
        id: String,
        title: String,
        description: String,
        moduleId: String,
        skill: ActivitySkillCategory,
        type: ExerciseTemplateType,
        difficulty: ActivityDifficulty,
        durationMin: Int,
        objective: String,
        instructions: List<String>,
        examples: List<String>,
        promptText: String,
        strategy: EvaluationStrategyType,
        rubric: List<ScoringRubricDimension>,
        nextIds: List<String>,
        permissions: List<ActivityPermission> = emptyList(),
        supportsAudio: Boolean = false,
        listeningScript: String = "",
        targetPhrase: String = "",
        dialogueOpening: String = "",
        dialogueRole: String = "",
        checklist: List<String> = emptyList()
    ): ActivityDefinition = ActivityDefinition(
        activityId = id,
        title = title,
        description = description,
        moduleId = moduleId,
        skillCategory = skill,
        exerciseType = type,
        difficulty = difficulty,
        duration = durationMin,
        learningObjective = objective,
        instructions = instructions,
        examples = examples,
        requiredPermissions = permissions,
        interactionConfiguration = InteractionConfiguration(
            promptText = promptText,
            supportsAudioRecording = supportsAudio,
            supportsVideoRecording = false,
            supportsTextInput = true,
            listeningScript = listeningScript,
            targetWordOrPhrase = targetPhrase,
            dialogueOpeningLine = dialogueOpening,
            dialoguePartnerRole = dialogueRole,
            checklistItems = checklist
        ),
        evaluationConfiguration = EvaluationConfiguration(
            strategy = strategy,
            aiEnabled = true,
            focusCriteria = rubric.map { it.label }
        ),
        scoringRubric = rubric,
        completionCriteria = CompletionCriteria(
            minDurationSeconds = if (supportsAudio) 10 else 5,
            minWordsOrTurns = 8,
            passingScoreThreshold = 60
        ),
        feedbackConfiguration = defaultFeedback,
        nextActivityIds = nextIds,
        sourceAttribution = "SpeakCraft Core Curriculum",
        contentVersion = 1
    )

    val module4Activities: List<ActivityDefinition> = listOf(
        buildActivity(
            id = "m04_intentional_pauses",
            title = "Intentional Pauses",
            description = "Use strategic silence before key conclusions so listeners lock onto your core message.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Frame three key takeaways using clear pre-point and post-point pauses.",
            instructions = listOf(
                "Introduce your main point, pause 1.5 seconds, then deliver your single headline sentence.",
                "Pause again before sharing one supporting example.",
                "Record or transcribe marking each [PAUSE]."
            ),
            examples = listOf("There is only one reason projects stall. [PAUSE] Nobody owns the final decision. [PAUSE]"),
            promptText = "Explain one habit that improves focus using at least three intentional pauses.",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_speaking_slowly_emphasis", "m04_declarative_statements"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_speaking_slowly_emphasis",
            title = "Speaking Slowly for Emphasis",
            description = "Slow down specifically on numbers, names, and decisions so crucial facts stand out.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.VOICE_MODULATION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Contrast normal conversational pace with deliberate slow-motion emphasis on the headline takeaway.",
            instructions = listOf(
                "Deliver background context at normal conversational speed.",
                "Drop tempo by ~25% on your single most important recommendation.",
                "Return to normal cadence on the next step."
            ),
            examples = listOf("Normal: 'We reviewed all four options.' Slow: 'Option Two saves twelve hours every single week.'"),
            promptText = "Share a 40-second recommendation and noticeably slow down on your key number or decision.",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_declarative_statements", "m09_pause_emphasis"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_declarative_statements",
            title = "Declarative Statements",
            description = "Replace tentative qualifiers with direct, accountable declarative sentences.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SENTENCE_CONSTRUCTION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Convert four hedged sentences into crisp, confident declarative statements.",
            instructions = listOf(
                "Remove weak qualifiers ('just', 'sort of', 'I guess', 'maybe we could possibly').",
                "Start with a clear subject and strong verb.",
                "Speak each revised sentence with grounded downward intonation."
            ),
            examples = listOf("Weak: 'I just feel like maybe we should delay.' -> Declarative: 'We should move the launch to Friday so testing is complete.'"),
            promptText = "Rewrite into 4 crisp declarative sentences: 1) I just think maybe we need more time. 2) Sort of feels like users are confused. 3) I guess I can lead that. 4) Sorry to bother you, could we possibly align?",
            strategy = EvaluationStrategyType.GRAMMAR,
            rubric = clarityRubric(),
            nextIds = listOf("m04_complete_thought_practice", "m10_assertive_communication")
        ),
        buildActivity(
            id = "m04_complete_thought_practice",
            title = "Complete-Thought Practice",
            description = "Prevent rambling by finishing one self-contained idea cleanly before pivoting.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Deliver a 45-second explanation using strictly complete, non-run-on sentences.",
            instructions = listOf(
                "Avoid chaining clauses with 'and then... so... which basically...'.",
                "Stop at every full period with a brief breath.",
                "Verify each sentence expresses one clear thought."
            ),
            examples = listOf("Clear notes save meeting time. Every attendee leaves with one owner and one deadline."),
            promptText = "Explain how to run an effective 15-minute standup using 5 clean, complete sentences.",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_ccc_framework", "m04_concise_speaking"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_ccc_framework",
            title = "CCC Framework",
            description = "Master the Clear, Concise, Compelling (CCC) speaking formula for memorable updates.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Deliver a 60-second message satisfying Clear (one core point), Concise (tight words), and Compelling (human impact).",
            instructions = listOf(
                "CLEAR: State your headline in sentence one.",
                "CONCISE: Give two crisp supporting facts without detours.",
                "COMPELLING: Close with why it matters to the listener."
            ),
            examples = listOf("Clear: Let's move sprint demos to Thursday. Concise: It gives QA 24 hours before release. Compelling: Nobody spends Friday night fixing urgent bugs."),
            promptText = "Pitch one workflow improvement to your team or classmates using the CCC (Clear, Concise, Compelling) framework.",
            strategy = EvaluationStrategyType.PRESENTATION,
            rubric = clarityRubric(),
            nextIds = listOf("m04_analogies", "m04_structured_answers"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_analogies",
            title = "Analogies",
            description = "Turn abstract ideas into instant 'aha!' moments using relatable everyday analogies.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.VOCABULARY,
            type = ExerciseTemplateType.VOCABULARY_EXERCISE,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Craft two original analogies linking a technical or abstract idea to an everyday physical experience.",
            instructions = listOf(
                "Pick two topics: e.g., Technical Debt, Compound Habit Growth, Active Listening, or API Integration.",
                "Use the bridge 'Think of [concept] like [familiar object] because...'",
                "Keep each analogy under 35 words."
            ),
            examples = listOf("Think of an API like a restaurant waiter: you don't enter the kitchen—you give a clear order and receive the finished dish."),
            promptText = "Create and speak two clear analogies: one for 'habit consistency' and one for a concept from your work or study.",
            strategy = EvaluationStrategyType.VOCABULARY,
            rubric = clarityRubric(),
            nextIds = listOf("m04_simple_explanations", "m04_technical_explanations"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_simple_explanations",
            title = "Simple Explanations",
            description = "Distill a multi-step process into three crystal-clear steps anyone can follow on first listen.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Explain a 3-step process using plain verbs and concrete nouns.",
            instructions = listOf(
                "Open by stating what the listener will be able to do.",
                "Walk through Step 1, Step 2, and Step 3 in one sentence each.",
                "Close with one quick check-for-understanding question."
            ),
            examples = listOf("Step 1: Open the checklist. Step 2: Pick one top priority. Step 3: Block 25 uninterrupted minutes."),
            promptText = "Explain in 3 simple steps how a beginner can prepare for a 5-minute presentation.",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_concise_speaking", "m04_logical_explanations"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_concise_speaking",
            title = "Concise Speaking",
            description = "Deliver full value in 30 seconds by trimming setup throat-clearing and redundant modifiers.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.AI_EVALUATED_CHALLENGE,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 5,
            objective = "Answer a broad question in 30 seconds (~60–75 words) with complete clarity.",
            instructions = listOf(
                "Skip preambles like 'That's a great question, let me think...'",
                "Deliver your headline in the first 5 seconds.",
                "Stop cleanly at 30 seconds."
            ),
            examples = listOf("The most underrated career skill is writing crisp summaries. It saves leaders hours and makes your work visible immediately."),
            promptText = "30-Second Precision Challenge: What is the single most valuable skill in your field, and why?",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_removing_unnecessary_words", "m04_summarization"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_removing_unnecessary_words",
            title = "Removing Unnecessary Words",
            description = "Edit wordy corporate or academic phrasing down to energetic, razor-sharp spoken English.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.GRAMMAR,
            type = ExerciseTemplateType.GRAMMAR_EXERCISE,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Cut word count by at least 40% while preserving 100% of the core meaning.",
            instructions = listOf(
                "Eliminate phrases like 'in order to', 'at this point in time', 'due to the fact that', and 'make a decision'.",
                "Replace passive voice with active verbs.",
                "Read your trimmed version aloud."
            ),
            examples = listOf("'At this point in time we need to make a decision in order to move forward.' -> 'We need to decide now so we can move forward.'"),
            promptText = "Trim by 40%+: 'At this point in time, in order to make sure that everyone on the team is completely aligned due to the fact that timelines have shifted, we are going to be holding a short meeting to make a decision on priorities.'",
            strategy = EvaluationStrategyType.GRAMMAR,
            rubric = clarityRubric(),
            nextIds = listOf("m04_organizing_thoughts", "m04_summarization")
        ),
        buildActivity(
            id = "m04_organizing_thoughts",
            title = "Organizing Thoughts",
            description = "Turn scattered ideas into an organized 3-bucket outline before you open your mouth.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.IMPROMPTU_SPEAKING,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Organize an unscripted response using Past -> Present -> Future or Problem -> Solution -> Benefit.",
            instructions = listOf(
                "Pick one mental blueprint: Problem -> Solution -> Benefit.",
                "Spend 15 seconds on each bucket.",
                "Use crisp signposts so listeners never lose the thread."
            ),
            examples = listOf("Problem: meetings run over. Solution: share a 3-bullet agenda 2 hours ahead. Benefit: we reclaim 3 hours weekly."),
            promptText = "Speak for 60 seconds about remote work or online learning using Problem -> Solution -> Benefit.",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_logical_explanations", "m04_structured_answers"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_logical_explanations",
            title = "Logical Explanations",
            description = "Connect claims to evidence cleanly using cause-and-effect reasoning.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Present a clear claim supported by two logically linked reasons and concrete proof.",
            instructions = listOf(
                "State your conclusion clearly first.",
                "Give Reason 1 and Reason 2 linked by 'First... because...' and 'Second... which means...'.",
                "Verify there are no logical leaps."
            ),
            examples = listOf("Short daily practice outperforms weekend cramming because memory consolidates during sleep and habit friction stays low."),
            promptText = "Answer logically: Why does practicing speaking for 10 minutes daily work better than 2 hours once a month?",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_strong_openings_conclusions", "m04_structured_answers"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_strong_openings_conclusions",
            title = "Strong Openings and Conclusions",
            description = "Frame every talk or answer with a hook that earns attention and a close that drives action.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Craft and deliver a compelling 1-sentence hook and a decisive 1-sentence close.",
            instructions = listOf(
                "Open with a crisp question, surprising stat, or vivid contrast.",
                "Share one 20-second core insight in the middle.",
                "Close with a clear callback or specific next action—never trail off with 'so yeah, that's it'."
            ),
            examples = listOf("Opening: 'What costs teams more time than bad code? Unclear handoffs.' Close: 'Start tomorrow's handoff with a 3-line summary.'"),
            promptText = "Record a 45-second message with a deliberate hook opening and decisive closing line (no 'that's all').",
            strategy = EvaluationStrategyType.PRESENTATION,
            rubric = clarityRubric(),
            nextIds = listOf("m09_strong_openings", "m09_memorable_conclusions"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_structured_answers",
            title = "Structured Answers",
            description = "Apply the PREP framework (Point, Reason, Example, Point) to answer tough questions smoothly.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Deliver a complete 60-second PREP answer without losing thread under pressure.",
            instructions = listOf(
                "P (Point): Give your direct answer in one sentence.",
                "R (Reason): Explain why in one sentence.",
                "E (Example): Share one brief real story or data point.",
                "P (Point): Restate your conclusion cleanly."
            ),
            examples = listOf("Point: Feedback works best within 24 hours. Reason: Details are fresh. Example: After Tuesday's demo... Point: Timely feedback accelerates growth."),
            promptText = "Use PREP to answer: 'Should teams prioritize speed or thorough documentation when launching a new feature?'",
            strategy = EvaluationStrategyType.SPEAKING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_summarization", "m10_interview_preparation"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m04_summarization",
            title = "Summarization",
            description = "Distill a dense briefing into a razor-sharp 3-bullet executive summary.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Listen to a multi-detail update and summarize the decision, risk, and next step in under 30 seconds.",
            instructions = listOf(
                "Play the audio briefing once and note the main decision, key risk, and deadline.",
                "Deliver a crisp 3-sentence summary covering only what a busy leader must know.",
                "Avoid repeating minor background details."
            ),
            examples = listOf("Bottom line: We launch Beta on May 12. Key risk: onboarding latency. Next step: DevOps runs load tests Thursday."),
            promptText = "Listen to the briefing and deliver a crisp 3-sentence executive summary.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = clarityRubric(),
            nextIds = listOf("m04_technical_explanations", "m06_listen_and_summarize"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true,
            listeningScript = "Team update: After testing three onboarding flows with forty new users this week, Flow B reduced drop-off by twenty-eight percent compared to our current screen. However, two Android tablets showed minor layout clipping on the confirmation button. Design will deliver the spacing fix tomorrow morning, engineering will verify by Wednesday afternoon, and we are green-lit to ship Flow B to all users on Friday morning."
        ),
        buildActivity(
            id = "m04_technical_explanations",
            title = "Technical Explanations",
            description = "Translate complex technical or specialist concepts into clear, actionable insight for non-experts.",
            moduleId = "mod_04_speaking_clarity",
            skill = ActivitySkillCategory.SPEAKING_CLARITY,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.ADVANCED,
            durationMin = 8,
            objective = "Explain a technical workflow using a 3-layer bridge: Outcome -> Plain Analogy -> Key Trade-Off.",
            instructions = listOf(
                "Start with what the technology achieves for the user or business.",
                "Explain how it works using one everyday analogy and zero unexplained acronyms.",
                "Close with one honest trade-off or constraint."
            ),
            examples = listOf("Encryption protects private messages like sealing a letter inside a locked box where only your recipient holds the key."),
            promptText = "Explain a technical concept from your domain (or two-factor authentication / caching) to a non-technical stakeholder in 60 seconds.",
            strategy = EvaluationStrategyType.PRESENTATION,
            rubric = clarityRubric(),
            nextIds = listOf("m10_technical_explanations", "m05_ford_framework"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        )
    )

    val module5Activities: List<ActivityDefinition> = listOf(
        buildActivity(
            id = "m05_ford_framework",
            title = "FORD Framework",
            description = "Never run out of natural conversation topics using Family, Occupation, Recreation, and Dreams.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 7,
            objective = "Guide a natural 4-turn conversation smoothly through at least two FORD pillars.",
            instructions = listOf(
                "Start with a light Recreation or Occupation question.",
                "Listen to the partner's answer and branch naturally toward Dreams or Family without sounding like an interview.",
                "Share a brief personal detail of your own for balanced reciprocity."
            ),
            examples = listOf("That weekend trail sounds awesome—what first got you into hiking, and is there a dream trail on your bucket list?"),
            promptText = "Practice a warm networking chat using the FORD (Family, Occupation, Recreation, Dreams) pillars.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_high_low_buffalo", "m05_open_ended_questions"),
            dialogueOpening = "Hey! Great event tonight. I'm Sam—I just wrapped up a super busy week leading product ops. How's your week been treating you?",
            dialogueRole = "Friendly attendee at a community mixer"
        ),
        buildActivity(
            id = "m05_high_low_buffalo",
            title = "High-Low-Buffalo Framework",
            description = "Spark engaging, authentic conversations with one High (win), one Low (challenge), and one Buffalo (random surprise).",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Share a memorable 60-second High-Low-Buffalo update that invites genuine follow-up questions.",
            instructions = listOf(
                "HIGH: Share one genuine highlight from your week.",
                "LOW: Share one honest minor hurdle (relatable, not heavy).",
                "BUFFALO: Share one unexpected or funny detail nobody would guess."
            ),
            examples = listOf("High: nailed my client demo. Low: spilled espresso on my notes. Buffalo: accidentally joined a street parade walking home!"),
            promptText = "Share your real High, Low, and Buffalo from this week in 60 seconds, then invite your listener to share theirs.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_personal_experience_sharing", "m05_appropriate_vulnerability"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m05_open_ended_questions",
            title = "Open-Ended Questions",
            description = "Upgrade dead-end yes/no questions into 'What' and 'How' prompts that unlock stories.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.SENTENCE_CONSTRUCTION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Transform five closed yes/no questions into warm, story-inviting open-ended questions.",
            instructions = listOf(
                "Replace 'Did you like...?' or 'Are you busy?' with 'What stood out...?' or 'How did you get into...?'.",
                "Keep questions short and easy to answer.",
                "Write and read aloud your 5 upgraded questions."
            ),
            examples = listOf("Closed: 'Did you enjoy your trip?' -> Open: 'What was the most unexpected moment from your trip?'"),
            promptText = "Upgrade these 5 closed questions into warm open-ended prompts: 1) Do you like your job? 2) Was your weekend good? 3) Did you enjoy the talk? 4) Are you from here? 5) Is the project going well?",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_follow_up_questions", "m05_three_follow_up_questions")
        ),
        buildActivity(
            id = "m05_follow_up_questions",
            title = "Follow-Up Questions",
            description = "Build depth by latching onto emotional keywords your partner drops instead of changing subjects.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Respond directly to your partner's last statement with tailored follow-up curiosity across 3 turns.",
            instructions = listOf(
                "Spot the most interesting noun or feeling in your partner's message.",
                "Acknowledge it briefly before asking a curious follow-up.",
                "Avoid pivoting prematurely to your own unrelated story."
            ),
            examples = listOf("Switching careers into robotics sounds bold—what moment convinced you to take that leap?"),
            promptText = "Practice active follow-up questioning in a dialogue with a colleague sharing a career pivot.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_three_follow_up_questions", "m05_conversation_thread_exploration"),
            dialogueOpening = "I actually just signed up for an evening pottery class after ten years of staring at spreadsheets all day. It felt weirdly intimidating on day one!",
            dialogueRole = "Colleague sharing a new hobby"
        ),
        buildActivity(
            id = "m05_three_follow_up_questions",
            title = "Three-Follow-Up-Questions Exercise",
            description = "Stay on one topic for three thoughtful layers deep to move effortlessly past surface small talk.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Explore a single conversational thread across 3 consecutive follow-up layers (What -> Why -> Impact).",
            instructions = listOf(
                "Turn 1: Ask a 'What' detail question about their topic.",
                "Turn 2: Ask a 'How/Why' motivation question.",
                "Turn 3: Ask about what changed for them or what comes next."
            ),
            examples = listOf("Layer 1: What book? Layer 2: What idea stuck with you most? Layer 3: How have you applied it this week?"),
            promptText = "Stay on ONE thread for 3 consecutive turns using layered follow-up questions.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_conversation_thread_exploration", "m06_follow_up_listening_game"),
            dialogueOpening = "Last month I took a solo train trip across the coast without checking work email for five days—it completely reset my perspective.",
            dialogueRole = "Friend returning from a trip"
        ),
        buildActivity(
            id = "m05_conversation_thread_exploration",
            title = "Conversation-Thread Exploration",
            description = "Spot multiple 'hooks' inside a single sentence and choose the richest thread to explore.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Identify 3 distinct conversational threads in one statement and craft an inviting follow-up for each.",
            instructions = listOf(
                "Read the multi-hook statement carefully.",
                "List 3 separate threads (e.g., location, skill, emotion).",
                "Write one natural response + follow-up for each thread."
            ),
            examples = listOf("Thread 1 (Tokyo): What neighborhood surprised you? Thread 2 (marathon): How did you train? Thread 3 (sister): Do you two run together often?"),
            promptText = "Statement: 'I just moved to Seattle last month to join a climate startup, and I'm trying to learn sailing on weekends even though I'm terrified of cold water.' Identify 3 threads and write a natural follow-up for each.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_common_ground_discovery", "m05_rapport_building")
        ),
        buildActivity(
            id = "m05_personal_experience_sharing",
            title = "Personal Experience Sharing",
            description = "Contribute warm, concise personal stories without hijacking the spotlight ('me-too' balancing).",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Share a 30-second relatable micro-story and gracefully hand the spotlight back to your listener.",
            instructions = listOf(
                "Validate your partner's point in one sentence first.",
                "Share a crisp 20-second personal parallel.",
                "End by passing the conversational baton back with a question."
            ),
            examples = listOf("I felt that exact nervousness before my first conference talk—writing 3 bullet anchors saved me. What helps you settle in?"),
            promptText = "A friend says: 'I always freeze up when meeting senior leaders.' Validate, share a 25s personal experience, and pass the floor back.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_appropriate_vulnerability", "m07_everyday_stories"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m05_appropriate_vulnerability",
            title = "Appropriate Vulnerability",
            description = "Build rapid trust by sharing real, resolved lessons ('scar, not open wound') with calm composure.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.REFLECTION_SELF_ASSESSMENT,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Share one honest challenge paired with the practical growth lesson you gained from it.",
            instructions = listOf(
                "Pick a real challenge you've already gained perspective on.",
                "Share briefly: what felt hard -> what you learned -> how it helps you now.",
                "Keep tone warm, grounded, and constructive."
            ),
            examples = listOf("Early in my career I hesitated to ask clarifying questions because I worried about looking slow. Asking early actually built far more trust."),
            promptText = "Speak or write a 45-second appropriately vulnerable reflection sharing a past hurdle and what it taught you.",
            strategy = EvaluationStrategyType.REFLECTION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_rapport_building", "m07_outcome_and_lesson"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m05_common_ground_discovery",
            title = "Common-Ground Discovery",
            description = "Find genuine shared interests, values, or experiences quickly with someone from a different background.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Uncover at least two authentic shared themes within 4 conversational turns.",
            instructions = listOf(
                "Ask curious exploratory questions across routines, music, learning, or travel.",
                "Highlight shared feelings or values even when specifics differ.",
                "Bridge naturally onto the shared discovery."
            ),
            examples = listOf("Even though you design bridges and I build apps, we both obsess over making complex systems feel effortless!"),
            promptText = "Chat with a stranger from a completely different industry and discover two genuine areas of common ground.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_match_and_mirror", "m05_rapport_building"),
            dialogueOpening = "Hi there! I'm Maya, a marine biologist visiting for the conference. Most people here seem to be in tech—what brings you out tonight?",
            dialogueRole = "Marine biologist at a cross-disciplinary meetup"
        ),
        buildActivity(
            id = "m05_match_and_mirror",
            title = "Match and Mirror",
            description = "Establish immediate psychological comfort by calibrating energy, pace, and key phrasing.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.ROLEPLAY,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Subtly mirror your partner's pacing, energy level, and 2–3 key emotional words without mimicking.",
            instructions = listOf(
                "Notice whether your partner sounds calm/reflective or upbeat/energetic.",
                "Reflect back their exact key phrase ('stretched thin', 'excited to experiment') naturally.",
                "Gradually guide the energy constructively once rapport is established."
            ),
            examples = listOf("Partner: 'It's been a whirlwind week.' You: 'A whirlwind week—what's been pulling the most of your focus?'"),
            promptText = "Roleplay with a thoughtful, slightly stressed teammate using subtle vocal/verbal Match & Mirror.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m08_body_language_mirroring", "m05_rapport_building"),
            dialogueOpening = "Honestly, I'm feeling a bit stretched thin juggling two launches at once, and I'm worried quality might slip.",
            dialogueRole = "Stretched teammate before a milestone"
        ),
        buildActivity(
            id = "m05_entering_conversations",
            title = "Entering Conversations",
            description = "Join individual or small-group conversations smoothly without awkwardness or interruption.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.ROLEPLAY,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Deliver three natural, low-friction openers for joining a pair, a group, or a solo attendee.",
            instructions = listOf(
                "Use situational observations ('How are you finding the sessions so far?') or warm permission ('Mind if I join you?').",
                "Introduce your name naturally after the first exchange.",
                "Practice entering the networking group below."
            ),
            examples = listOf("Hi, I couldn't help noticing you mentioned design systems—mind if I join in? I'm Jordan."),
            promptText = "Practice entering a friendly conversation at a workshop break using warmth and situational curiosity.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_ending_conversations", "m05_small_talk"),
            dialogueOpening = "(Two attendees are chatting near the refreshments table about the keynote speaker's opening story and pause with open body language.)",
            dialogueRole = "Two approachable workshop attendees"
        ),
        buildActivity(
            id = "m05_ending_conversations",
            title = "Ending Conversations",
            description = "Exit conversations warmly, gracefully, and memorably without abrupt excuses.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.ROLEPLAY,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Close a chat using the 3-part graceful exit: Appreciation + Specific Callback + Warm Next Step.",
            instructions = listOf(
                "Wait for a natural pause or your own speaking turn.",
                "Thank them referencing something specific they shared.",
                "State your reason cleanly and leave the door warmly open."
            ),
            examples = listOf("I've really enjoyed hearing about your podcast project, Alex. I want to say hello to one more colleague before the panel starts—let's connect on LinkedIn!"),
            promptText = "Practice exiting a pleasant 5-minute networking chat gracefully using Appreciation + Callback + Next Step.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_first_impressions", "m10_networking"),
            dialogueOpening = "Yeah, that workshop gave me so many ideas for next quarter! Anyway, what else are you hoping to catch today?",
            dialogueRole = "Friendly networking peer"
        ),
        buildActivity(
            id = "m05_small_talk",
            title = "Small Talk",
            description = "Turn everyday small talk into an enjoyable warm-up that opens the door to meaningful rapport.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Pivot effortlessly from light context (weather, venue, coffee) to an engaging personal curiosity question.",
            instructions = listOf(
                "Start with one shared observation in the moment.",
                "Add a tiny personal flavor instead of a one-word answer.",
                "Ask a breezy follow-up that invites a fun story."
            ),
            examples = listOf("This rain definitely makes hot tea mandatory—what's your go-to rainy day ritual?"),
            promptText = "Turn an elevator or waiting-room small-talk opening into a genuinely pleasant 3-turn exchange.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_first_impressions", "m05_rapport_building"),
            dialogueOpening = "Looks like the rain finally cleared up just in time for the afternoon! Busy day ahead?",
            dialogueRole = "Colleague waiting for a meeting room"
        ),
        buildActivity(
            id = "m05_first_impressions",
            title = "First Impressions",
            description = "Project immediate warmth and competence in the first 15 seconds of meeting someone new.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.SPEAKING_EXERCISE,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Deliver a crisp 15-second greeting combining eye contact cue, smile warmth, clear name, and curiosity.",
            instructions = listOf(
                "Pause half a second before saying your name clearly so they actually remember it.",
                "Share one human-friendly line about what you do (impact, not jargon title).",
                "Immediately ask an easy, welcoming question."
            ),
            examples = listOf("Hi, I'm Priya. I help engineering teams turn messy ideas into simple mobile tools. What brings you here today?"),
            promptText = "Record a warm, memorable 15-second first-impression introduction for a new colleague or event guest.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = conversationRubric(),
            nextIds = listOf("m05_rapport_building", "m10_self_introduction"),
            permissions = listOf(ActivityPermission.MICROPHONE),
            supportsAudio = true
        ),
        buildActivity(
            id = "m05_rapport_building",
            title = "Rapport Building",
            description = "Combine active curiosity, name usage, validation, and shared humor into lasting rapport.",
            moduleId = "mod_05_conversation_mastery",
            skill = ActivitySkillCategory.CONVERSATION,
            type = ExerciseTemplateType.REAL_WORLD_TASK,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 10,
            objective = "Apply 3 deliberate rapport behaviors in a real conversation today and log what shifted.",
            instructions = listOf(
                "Have one intentional 5-minute conversation with a friend, colleague, or neighbor.",
                "Apply: 1) Use their name warmly, 2) Ask 2 open follow-up questions, 3) Reflect one emotion they expressed.",
                "Return and log what happened and what felt most natural."
            ),
            examples = listOf("When I reflected my teammate's pride in solving the bug, the entire conversation opened up for ten minutes."),
            promptText = "Complete the 3-step real-world rapport mission and reflect on how the other person responded.",
            strategy = EvaluationStrategyType.REFLECTION,
            rubric = conversationRubric(),
            nextIds = listOf("m06_active_listening", "m10_networking"),
            checklist = listOf("Used listener's name warmly", "Asked at least 2 open-ended follow-ups", "Validated one emotion or perspective", "Logged real-world reflection")
        )
    )

    val module6Activities: List<ActivityDefinition> = listOf(
        buildActivity(
            id = "m06_active_listening",
            title = "Active Listening",
            description = "Practice full-presence listening by capturing key facts, intent, and confirming understanding before advising.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Listen attentively to a spoken scenario and restate both content and priority accurately.",
            instructions = listOf(
                "Press Play Audio and listen without multitasking.",
                "Identify the speaker's main concern, constraint, and desired outcome.",
                "Write or speak a concise active-listening response."
            ),
            examples = listOf("It sounds like you're proud of the design quality, yet worried the tight Friday deadline could force shortcuts."),
            promptText = "Listen carefully and summarize the speaker's concern, constraint, and core need.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_subtext_listening", "m06_reflective_responses"),
            listeningScript = "I love working with this product team, really I do. It's just that the last three Fridays we added 'one tiny feature' at 3 PM, and I ended up working Saturday mornings to stabilize the release. I want our users to love the app, but I need predictable scope freeze by Thursday noon so I can recharge."
        ),
        buildActivity(
            id = "m06_subtext_listening",
            title = "Subtext Listening",
            description = "Read between the lines to catch unspoken concerns, hesitation, or hidden priorities.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 7,
            objective = "Distinguish literal surface words from the speaker's underlying unspoken concern.",
            instructions = listOf(
                "Play the audio clip and listen for qualifiers, pauses, or polite deflection.",
                "State: 1) What was literally said, 2) What the likely subtext is, 3) A gentle clarifying question.",
                "Avoid accusing—frame your check-in with curiosity."
            ),
            examples = listOf("Subtext: They aren't sure the plan is realistic. Gentle check: 'What part of this timeline feels riskiest to you?'"),
            promptText = "Listen to the speaker's update and identify the literal words, likely subtext, and one curious check-in question.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_emotional_listening", "m06_conversational_intent"),
            listeningScript = "Sure, I guess we can try launching both dashboards next week if leadership really wants it. I mean, technically the APIs exist, though we haven't had a single real customer test the billing permissions yet. Anyway, whatever you think is best."
        ),
        buildActivity(
            id = "m06_visual_listening",
            title = "Visual Listening",
            description = "Notice posture shifts, facial micro-reactions, and eye gaze cues while others speak.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Interpret three common visual non-verbal cues thoughtfully without jumping to rigid stereotypes.",
            instructions = listOf(
                "Read the visual scene description carefully.",
                "Note two plausible interpretations (context matters—crossed arms can mean cold OR guarded).",
                "Write one calm verbal check-in to confirm rather than assume."
            ),
            examples = listOf("Rather than assuming hostility when someone glances at their watch, check: 'Want me to give you the 60-second headline first?'"),
            promptText = "During a pitch, a listener leans back, stops taking notes, and furrows their brow when pricing appears. What might this signal, and what respectful check-in should you ask?",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_reading_the_room", "m08_reading_body_language")
        ),
        buildActivity(
            id = "m06_auditory_listening",
            title = "Auditory Listening",
            description = "Train precision recall for names, numbers, dates, and exact constraints spoken aloud.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 6,
            objective = "Recall at least 4 of 5 specific factual details after a single focused listen.",
            instructions = listOf(
                "Listen once without pausing if possible.",
                "Write down: 1) Speaker's name, 2) Venue/city, 3) Time, 4) Headcount, 5) Special requirement.",
                "Submit to check accuracy."
            ),
            examples = listOf("Anchor numbers visually in your mind as you hear them."),
            promptText = "Play the audio once and capture the 5 key logistics details accurately.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_listen_and_summarize", "m06_vocal_tone_recognition"),
            listeningScript = "Hi, this is Daniel from Northstar Events. We confirmed the communication workshop in Austin for Thursday at 10:30 AM in Room 4B. We are expecting 35 participants, and 6 of them will join remotely via video call, so we need a directional tabletop microphone tested by 10:00 AM."
        ),
        buildActivity(
            id = "m06_emotional_listening",
            title = "Emotional Listening",
            description = "Listen beyond facts to name and validate the core feeling driving the speaker's message.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Identify the primary emotion accurately and craft an empathetic validation response before problem-solving.",
            instructions = listOf(
                "Listen to the speaker's story focusing on emotional cues.",
                "Name the core feeling gently ('It sounds frustrating / overwhelming / exciting...').",
                "Resist jumping immediately into unsolicited advice."
            ),
            examples = listOf("It makes total sense you feel drained after carrying that project alone—thank you for telling me openly."),
            promptText = "Listen to the audio and respond by naming the underlying emotion and validating before offering solutions.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_emotion_identification", "m06_reflective_responses"),
            listeningScript = "I spent three weeks preparing the customer research deck, and right before the meeting the agenda changed and my slides were skipped completely. Nobody even asked if I had insights to add. I know timelines are tight, it just makes me wonder if the research even mattered."
        ),
        buildActivity(
            id = "m06_last_word_last_letter_game",
            title = "Last Word, Last Letter Game",
            description = "Sharpen real-time listening focus by starting your reply with the final word or letter of the previous speaker's sentence.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.ACTIVE_LISTENING_GAME,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 5,
            objective = "Play 4 rapid conversational links starting each response with the partner's exact final word.",
            instructions = listOf(
                "Read/listen all the way to the very last word of your partner's line—do not rehearse early!",
                "Begin your next sentence with that exact final word (or its final letter for hard mode).",
                "Keep the response natural and connected to the story."
            ),
            examples = listOf("Partner: '...we discovered a hidden garden.' -> You: 'Garden paths always make me slow down and notice details.'"),
            promptText = "Play 4 rounds of Last Word / Last Letter starting from the cue line: 'Great conversations start with genuine curiosity.'",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_follow_up_listening_game", "m06_listening_without_interruption"),
            listeningScript = "Great conversations start with genuine curiosity.",
            targetPhrase = "curiosity"
        ),
        buildActivity(
            id = "m06_follow_up_listening_game",
            title = "Follow-Up Listening Game",
            description = "Train yourself to listen fully and craft a laser-focused follow-up based strictly on the speaker's final detail.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.ACTIVE_LISTENING_GAME,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Deliver three sequential follow-ups anchored strictly in newly heard details from the audio cue.",
            instructions = listOf(
                "Play the audio cue completely before typing or speaking.",
                "Pick one concrete detail from the second half of the clip.",
                "Ask a curious follow-up plus a second deeper follow-up."
            ),
            examples = listOf("You mentioned teaching your grandfather to use video calls—what was his reaction the first time it worked?"),
            promptText = "Listen to the short story and craft 2 natural follow-up questions anchored in specific details from the clip.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_listening_without_interruption", "m05_three_follow_up_questions"),
            listeningScript = "Last Sunday I visited my hometown bakery that's been open for forty years. The owner remembered my favorite cinnamon roll, and he actually let me step behind the counter to watch how they braid the dough by hand before sunrise."
        ),
        buildActivity(
            id = "m06_listening_without_interruption",
            title = "Listening Without Interruption",
            description = "Build patience and presence by holding a full 2-second breath after someone finishes before you speak.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.REAL_WORLD_TASK,
            difficulty = ActivityDifficulty.BEGINNER,
            durationMin = 8,
            objective = "Complete one real conversation holding a calm 2-second beat after every turn without interrupting or finishing sentences.",
            instructions = listOf(
                "During your next conversation or call, consciously let the other person finish every thought completely.",
                "Count 'one, two' silently before speaking your reply.",
                "Log what extra details the speaker added during those quiet moments."
            ),
            examples = listOf("When I waited two extra seconds, my colleague shared the real root cause they hadn't mentioned yet."),
            promptText = "Practice zero-interruption listening in a real chat and record what shifted when you waited 2 full seconds.",
            strategy = EvaluationStrategyType.REFLECTION,
            rubric = listeningRubric(),
            nextIds = listOf("m06_reflective_responses", "m06_reading_the_room"),
            checklist = listOf("Zero interruptions during a 5-min conversation", "Held a 2-second pause after partner finished", "Resisted finishing their sentences", "Captured reflection notes")
        ),
        buildActivity(
            id = "m06_reflective_responses",
            title = "Reflective Responses",
            description = "Paraphrase meaning in your own fresh words so speakers feel genuinely understood.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Write and speak a natural reflective paraphrase without parroting robotic identical words.",
            instructions = listOf(
                "Listen to the speaker's dilemma.",
                "Reflect both the core goal and the tension in your own warm phrasing ('What I'm hearing is...').",
                "End with a quick accuracy check ('Did I capture that right?')."
            ),
            examples = listOf("It sounds like you're torn between taking the promotion now and protecting time for your certification—did I hear that right?"),
            promptText = "Listen to the dilemma and craft a warm, non-robotic reflective response ending with a gentle check.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_listen_and_respond", "m10_difficult_conversations"),
            listeningScript = "I got offered a leadership role on the new client account, which is super exciting for my career. At the same time, my current team is in the middle of a migration and I hate the idea of leaving them hanging right before launch."
        ),
        buildActivity(
            id = "m06_reading_the_room",
            title = "Reading the Room",
            description = "Sense group energy, engagement, and alignment shifts quickly so you can adapt mid-conversation.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.ADVANCED,
            durationMin = 6,
            objective = "Diagnose room-energy cues and propose a tactful verbal reset to re-engage the group.",
            instructions = listOf(
                "Analyze the meeting scenario below.",
                "Identify what the room's silence and side-glances likely indicate.",
                "Script one 20-second facilitative reset to invite honest voices back in."
            ),
            examples = listOf("I notice the room got quiet after Slide 4—let's pause for 60 seconds. What's one concern we haven't surfaced yet?"),
            promptText = "Scenario: Ten minutes into a strategy meeting, two senior stakeholders stop contributing, check laptops, and give clipped 'fine' answers. How do you read the room and reset constructively?",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = listeningRubric(),
            nextIds = listOf("m06_conversational_intent", "m10_meeting_participation")
        ),
        buildActivity(
            id = "m06_emotion_identification",
            title = "Emotion Identification",
            description = "Sharpen emotional granularity beyond 'happy/mad/sad' to recognize pride, relief, apprehension, or burnout.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Pinpoint precise nuanced emotions from spoken phrasing and choose an attuned reply.",
            instructions = listOf(
                "Listen to the three short spoken lines.",
                "Label the nuanced emotion for each (e.g., apprehensive, quietly proud, stretched thin).",
                "Write one empathetic opening sentence for each."
            ),
            examples = listOf("Line 1 signals cautious optimism mixed with apprehension—acknowledge both."),
            promptText = "Play the 3 short clips and identify the exact nuanced emotion + tailored empathetic reply for each.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_vocal_tone_recognition", "m06_listen_and_respond"),
            listeningScript = "Clip one: We finally passed the security audit on the first try—I honestly didn't think we'd pull it off. Clip two: I'm excited to present to the board, though I've never answered live investor questions before. Clip three: Sure, add it to my plate, I'll figure out when to fit it in."
        ),
        buildActivity(
            id = "m06_vocal_tone_recognition",
            title = "Vocal Tone Recognition",
            description = "Notice how speed, hesitation, and phrasing reveal confidence, reluctance, or urgency.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Compare literal wording against tone cues to determine what support the speaker actually needs.",
            instructions = listOf(
                "Play the spoken message closely.",
                "Note cues of hesitation, urgency, or warmth.",
                "Explain how you would respond differently than if you only read a plain text chat."
            ),
            examples = listOf("Fast cadence with repeated 'real quick' signals high time pressure—lead with your 10-second answer first."),
            promptText = "Listen to the speaker's tone and explain what urgency/hesitation cues you noticed and how you should respond.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_conversational_intent", "m06_listen_and_respond"),
            listeningScript = "Hey, real quick before my 2 o'clock starts—do we actually have final sign-off from legal on the partner announcement, or are we still waiting on their redlines? Because marketing is about to hit send in twenty minutes."
        ),
        buildActivity(
            id = "m06_conversational_intent",
            title = "Conversational Intent",
            description = "Identify whether a speaker wants empathy ('vent'), brainstorming ('explore'), or a direct fix ('solve').",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.QUESTION_AND_ANSWER,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Classify conversational intent (Vent vs Explore vs Solve) and ask the clarifying intent question naturally.",
            instructions = listOf(
                "Read the two scenarios below.",
                "Identify whether each speaker likely wants Empathy, Brainstorming, or Immediate Action.",
                "Write a warm clarifying prompt ('Do you want me to just listen right now, or brainstorm options together?')."
            ),
            examples = listOf("Asking 'Would it help most to vent for a minute or map out next steps?' prevents mismatched responses."),
            promptText = "Scenario A: A friend recounts an exhausting commute and rude driver. Scenario B: A teammate says 'The build is red 1 hour before release—any ideas?' Classify intent and script both responses.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_listen_and_summarize", "m06_listen_and_respond")
        ),
        buildActivity(
            id = "m06_listen_and_summarize",
            title = "Listen and Summarize",
            description = "Distill a spoken 45-second narrative into a crisp, accurate 2-sentence recap.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.LISTENING_COMPREHENSION,
            difficulty = ActivityDifficulty.INTERMEDIATE,
            durationMin = 6,
            objective = "Capture all 3 main takeaways from a spoken briefing in exactly 2 clear sentences.",
            instructions = listOf(
                "Listen attentively without writing verbatim.",
                "Synthesize the core goal, obstacle, and action into 2 clean sentences.",
                "Speak or type your summary."
            ),
            examples = listOf("Your team is shifting customer onboarding to interactive checklists to cut setup time in half, starting with a 20-user pilot next Monday."),
            promptText = "Play the audio clip and summarize all core points in 2 crisp sentences.",
            strategy = EvaluationStrategyType.LISTENING,
            rubric = listeningRubric(),
            nextIds = listOf("m06_listen_and_respond", "m07_story_structure"),
            listeningScript = "Over the past quarter our customer support tickets rose mostly because new users couldn't locate export settings. Instead of writing a longer PDF manual, we are launching a 90-second interactive walkthrough inside the dashboard next Monday for twenty percent of new accounts and measuring ticket drop over two weeks."
        ),
        buildActivity(
            id = "m06_listen_and_respond",
            title = "Listen and Respond",
            description = "Close the active listening loop: Hear -> Paraphrase -> Add Value -> Confirm Next Step.",
            moduleId = "mod_06_active_listening",
            skill = ActivitySkillCategory.ACTIVE_LISTENING,
            type = ExerciseTemplateType.CONVERSATION_SIMULATION,
            difficulty = ActivityDifficulty.ADVANCED,
            durationMin = 7,
            objective = "Complete a 3-turn listening-first dialogue applying Hear -> Paraphrase -> Respond cleanly.",
            instructions = listOf(
                "In Turn 1, paraphrase your partner's core point before sharing your view.",
                "In Turn 2, ask one clarifying question that uncovers their hidden priority.",
                "In Turn 3, propose a mutually aligned next step."
            ),
            examples = listOf("So your top priority is keeping onboarding under 3 minutes while still capturing goals. What if we ask just two questions upfront?"),
            promptText = "Practice a Listen-and-Respond dialogue with a stakeholder raising scope concerns.",
            strategy = EvaluationStrategyType.CONVERSATION,
            rubric = listeningRubric(),
            nextIds = listOf("m07_story_structure", "m10_customer_communication"),
            dialogueOpening = "I like the new feature set, honestly, but our field reps only have two minutes between client visits. If logging a note takes six taps, they'll just go back to sticky notes.",
            dialogueRole = "Operations lead representing field reps"
        )
    )
}
