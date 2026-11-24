package com.example.data.activity

import com.example.data.learning.DailyPracticeTask

data class TechniqueMetadata(
    val id: String,
    val name: String,
    val category: String,
    val summary: String,
    val coreRule: String
)

object LearningPathsEngine {

    val techniquesCatalog: Map<String, TechniqueMetadata> = listOf(
        TechniqueMetadata(
            id = "active-listening",
            name = "Listen fully, then reflect back",
            category = "Active Listening",
            summary = "Give someone full attention, catch their main point, and play it back in one sentence before sharing your view.",
            coreRule = "Park your reply, listen for feeling, and summarize their point in your own words."
        ),
        TechniqueMetadata(
            id = "adapt-to-listener-style",
            name = "Adapt your message to your listener",
            category = "Communication Psychology",
            summary = "Adjust pace, structure, warmth, and detail level to match your listener's communication preferences.",
            coreRule = "Observe listener body language and style; calibrate detail and tempo accordingly."
        ),
        TechniqueMetadata(
            id = "articulation-warmup",
            name = "Articulation warm-up for crisp words",
            category = "Pronunciation & Articulation",
            summary = "Condition jaw, lips, and tongue with exaggerated movements for clean, crisp consonants.",
            coreRule = "Slow down to half-speed; finish each final consonant cleanly before the next word."
        ),
        TechniqueMetadata(
            id = "calm-clear-speaking",
            name = "Calm, clear speaking habits",
            category = "Voice Mastery",
            summary = "Build diaphragmatic breath grounding, steady unhurried cadence, and calm nerve recovery.",
            coreRule = "Breathe low into the abdomen and let silence settle between thoughts."
        ),
        TechniqueMetadata(
            id = "conversation-threads",
            name = "Follow the conversation thread",
            category = "Conversation Mastery",
            summary = "Pick up keywords, interests, and emotional cues to branch natural, engaging conversational threads.",
            coreRule = "Listen for topics the other person is excited about; ask 3-layer follow-up questions."
        ),
        TechniqueMetadata(
            id = "explain-complex-ideas",
            name = "Explain Complex Ideas in Plain Words",
            category = "Speaking Clarity",
            summary = "Break down complex technical concepts using BLUF, plain language, and concrete analogies.",
            coreRule = "State the headline first, provide one intuitive comparison, and summarize the key takeaway."
        ),
        TechniqueMetadata(
            id = "pause-not-filler",
            name = "Pause instead of filler words",
            category = "Voice Mastery",
            summary = "Eliminate 'um', 'uh', and 'like' by embracing deliberate, silent pauses when formulating ideas.",
            coreRule = "When reaching for your next sentence, close your lips and pause in silence."
        ),
        TechniqueMetadata(
            id = "prep-answer",
            name = "Answer With PREP: Point, Reason, Example, Point",
            category = "Speaking Clarity",
            summary = "Deliver structured, decisive answers in meetings, interviews, and impromptu speaking sprints.",
            coreRule = "Point (core stance), Reason (why it matters), Example (concrete evidence), Point (summary)."
        ),
        TechniqueMetadata(
            id = "purposeful-gestures",
            name = "Purposeful hand gestures",
            category = "Body Language",
            summary = "Keep gestures inside the Power Zone (navel to chest) with open palms and descriptive shapes.",
            coreRule = "Anchor hands at rest; gesture intentionally on key verbs, contrasts, and outcomes."
        ),
        TechniqueMetadata(
            id = "record-and-review",
            name = "Record Yourself and Review in Passes",
            category = "Self-Coaching",
            summary = "Separate self-critique into isolated passes: Visual (muted), Vocal (audio-only), Verbal (transcript).",
            coreRule = "Study your recordings like game film without emotional self-judgment."
        ),
        TechniqueMetadata(
            id = "relive-the-story",
            name = "Tell the story from inside the scene",
            category = "Storytelling",
            summary = "Transport audiences by reliving sensory details, character dialogue, and dramatic turning points.",
            coreRule = "Relive rather than report: describe sensory sights, sounds, and verbatim moments."
        ),
        TechniqueMetadata(
            id = "share-highs-and-lows",
            name = "Share your highs, lows and surprises",
            category = "Conversation Mastery",
            summary = "Deepen rapport and authenticity by sharing positive wins, candid hurdles, and unexpected insights.",
            coreRule = "Pair personal accomplishments with vulnerable lessons learned (High-Low-Buffalo)."
        ),
        TechniqueMetadata(
            id = "three-channels",
            name = "Balance Body, Voice and Words",
            category = "Public Speaking",
            summary = "Synchronize Visual Image (posture, eye contact), Vocal Image (tone, melody), and Verbal Words.",
            coreRule = "Ensure posture and tone carry the same conviction as your spoken arguments."
        ),
        TechniqueMetadata(
            id = "vocal-variety",
            name = "Vary Your Voice to Hold Attention",
            category = "Voice Mastery",
            summary = "Modulate the 5 Vocal Foundations: Rate, Pause, Melody, Volume, and Character.",
            coreRule = "Vary your pitch melody and speed contrast to keep listeners leaning in."
        ),
        TechniqueMetadata(
            id = "warm-and-assured",
            name = "Warm and assured voice",
            category = "Professional Communication",
            summary = "Project unshakeable authority balanced with personal warmth and grounded downward inflection.",
            coreRule = "Avoid upward inflection on declarative statements; end with decisive warmth."
        )
    ).associateBy { it.id }

    /**
     * Complete mapping of catalogue activities across Modules 1-10 to their core communication techniques.
     */
    val activityToTechniques: Map<String, List<String>> = mapOf(
        // Module 1: English Fluency
        "m01_daily_english_speaking" to listOf("calm-clear-speaking", "warm-and-assured"),
        "m01_reading_aloud" to listOf("articulation-warmup", "calm-clear-speaking"),
        "m01_sentence_construction" to listOf("prep-answer"),
        "m01_complete_thought_speaking" to listOf("prep-answer", "calm-clear-speaking"),
        "m01_simple_explanations" to listOf("explain-complex-ideas"),
        "m01_daily_activity_descriptions" to listOf("relive-the-story"),
        "m01_english_recording" to listOf("record-and-review"),
        "m01_everyday_conversations" to listOf("conversation-threads"),
        "m01_impromptu_speaking" to listOf("prep-answer", "pause-not-filler"),
        "m01_random_topic_speaking" to listOf("prep-answer", "pause-not-filler"),
        "m01_expressive_reading" to listOf("vocal-variety", "warm-and-assured"),
        "m01_spontaneous_speaking" to listOf("calm-clear-speaking"),
        "m01_speech_transcription" to listOf("record-and-review"),
        "m01_sentence_clarity" to listOf("explain-complex-ideas"),
        "m01_english_thinking_practice" to listOf("calm-clear-speaking"),

        // Module 2: Pronunciation & Articulation
        "m02_mouth_movement_exercises" to listOf("articulation-warmup"),
        "m02_jaw_relaxation" to listOf("articulation-warmup", "calm-clear-speaking"),
        "m02_lip_trills" to listOf("articulation-warmup"),
        "m02_tongue_warmups" to listOf("articulation-warmup"),
        "m02_mouth_opening_exercises" to listOf("articulation-warmup"),
        "m02_pronunciation_drills" to listOf("articulation-warmup"),
        "m02_articulation_exercises" to listOf("articulation-warmup"),
        "m02_difficult_sound_practice" to listOf("articulation-warmup"),
        "m02_tongue_twisters" to listOf("articulation-warmup"),
        "m02_consonant_clarity" to listOf("articulation-warmup"),
        "m02_vowel_pronunciation" to listOf("articulation-warmup"),
        "m02_word_stress" to listOf("vocal-variety"),
        "m02_sentence_stress" to listOf("vocal-variety"),
        "m02_accent_awareness" to listOf("record-and-review"),
        "m02_recording_comparisons" to listOf("record-and-review"),

        // Module 3: Voice Mastery
        "m03_vocal_warmups" to listOf("vocal-variety", "warm-and-assured"),
        "m03_breathing_practice" to listOf("calm-clear-speaking", "warm-and-assured"),
        "m03_slow_speaking" to listOf("calm-clear-speaking", "pause-not-filler"),
        "m03_pause_practice" to listOf("pause-not-filler"),
        "m03_sentence_endings" to listOf("warm-and-assured"),
        "m03_filler_word_reduction" to listOf("pause-not-filler"),
        "m03_volume_control" to listOf("vocal-variety", "three-channels"),
        "m03_vocal_image" to listOf("three-channels", "warm-and-assured"),
        "m03_voice_confidence" to listOf("warm-and-assured"),
        "m03_expressive_delivery" to listOf("vocal-variety"),

        // Module 4: Speaking Clarity
        "m04_intentional_pauses" to listOf("pause-not-filler"),
        "m04_ccc_framework" to listOf("explain-complex-ideas"),
        "m04_organizing_thoughts" to listOf("prep-answer"),
        "m04_structured_answers" to listOf("prep-answer"),
        "m04_technical_explanations" to listOf("explain-complex-ideas"),
        "m04_declarative_statements" to listOf("warm-and-assured"),
        "m04_concise_speaking" to listOf("prep-answer", "explain-complex-ideas"),

        // Module 5: Conversation Mastery
        "m05_first_impressions" to listOf("three-channels", "warm-and-assured"),
        "m05_small_talk" to listOf("conversation-threads"),
        "m05_entering_conversations" to listOf("conversation-threads"),
        "m05_ford_framework" to listOf("conversation-threads"),
        "m05_open_ended_questions" to listOf("conversation-threads", "active-listening"),
        "m05_follow_up_questions" to listOf("conversation-threads", "active-listening"),
        "m05_three_follow_up_questions" to listOf("conversation-threads"),
        "m05_high_low_buffalo" to listOf("share-highs-and-lows"),
        "m05_common_ground_discovery" to listOf("conversation-threads"),
        "m05_match_and_mirror" to listOf("adapt-to-listener-style"),
        "m05_ending_conversations" to listOf("conversation-threads"),
        "m05_rapport_building" to listOf("adapt-to-listener-style", "active-listening"),

        // Module 6: Active Listening
        "m06_listen_and_summarize" to listOf("active-listening"),
        "m06_active_listening" to listOf("active-listening"),
        "m06_reflective_responses" to listOf("active-listening"),
        "m06_reading_the_room" to listOf("adapt-to-listener-style", "three-channels"),
        "m06_listen_and_respond" to listOf("active-listening"),

        // Module 7: Storytelling
        "m07_story_structure" to listOf("relive-the-story"),
        "m07_situation_and_context" to listOf("relive-the-story"),
        "m07_challenge_and_conflict" to listOf("relive-the-story"),
        "m07_story_progression" to listOf("relive-the-story"),
        "m07_outcome_and_lesson" to listOf("relive-the-story"),
        "m07_everyday_stories" to listOf("relive-the-story", "share-highs-and-lows"),
        "m07_vaks_framework" to listOf("relive-the-story"),
        "m07_concrete_descriptions" to listOf("relive-the-story"),
        "m07_vocal_melody_storytelling" to listOf("vocal-variety", "relive-the-story"),
        "m07_suspense_and_pauses" to listOf("pause-not-filler", "relive-the-story"),
        "m07_facial_expression_storytelling" to listOf("three-channels", "purposeful-gestures"),
        "m07_gesture_storytelling" to listOf("purposeful-gestures", "three-channels"),
        "m07_audience_connection" to listOf("adapt-to-listener-style"),
        "m07_short_storytelling" to listOf("relive-the-story", "prep-answer"),
        "m07_long_storytelling" to listOf("relive-the-story"),

        // Module 8: Body Language
        "m08_posture" to listOf("three-channels"),
        "m08_hand_gestures" to listOf("purposeful-gestures", "three-channels"),
        "m08_physical_presence" to listOf("three-channels", "purposeful-gestures"),
        "m08_gesture_control" to listOf("purposeful-gestures"),
        "m08_video_call_presence" to listOf("three-channels", "purposeful-gestures"),

        // Module 9: Public Speaking
        "m09_nervousness_management" to listOf("calm-clear-speaking", "warm-and-assured"),
        "m09_camera_speaking" to listOf("three-channels", "record-and-review"),
        "m09_strong_openings" to listOf("prep-answer", "vocal-variety"),
        "m09_main_message_structure" to listOf("prep-answer", "explain-complex-ideas"),
        "m09_memorable_conclusions" to listOf("prep-answer", "warm-and-assured"),
        "m09_speaking_without_scripts" to listOf("prep-answer", "calm-clear-speaking"),
        "m09_pause_emphasis" to listOf("pause-not-filler", "vocal-variety"),
        "m09_vocal_volume" to listOf("vocal-variety", "three-channels"),
        "m09_group_speaking" to listOf("three-channels", "adapt-to-listener-style"),
        "m09_presentation_storytelling" to listOf("relive-the-story", "prep-answer"),
        "m09_unexpected_questions" to listOf("prep-answer", "calm-clear-speaking"),
        "m09_recovering_after_mistakes" to listOf("calm-clear-speaking"),
        "m09_stage_rehearsal" to listOf("three-channels", "record-and-review"),

        // Module 10: Professional Communication
        "m10_self_introduction" to listOf("three-channels", "prep-answer", "warm-and-assured"),
        "m10_workplace_presentations" to listOf("prep-answer", "explain-complex-ideas"),
        "m10_meeting_participation" to listOf("prep-answer", "pause-not-filler"),
        "m10_team_discussions" to listOf("active-listening", "conversation-threads"),
        "m10_manager_communication" to listOf("explain-complex-ideas", "prep-answer"),
        "m10_colleague_communication" to listOf("conversation-threads", "adapt-to-listener-style"),
        "m10_remote_communication" to listOf("explain-complex-ideas", "warm-and-assured"),
        "m10_virtual_meetings" to listOf("three-channels", "explain-complex-ideas"),
        "m10_customer_communication" to listOf("active-listening", "adapt-to-listener-style"),
        "m10_technical_explanations" to listOf("explain-complex-ideas"),
        "m10_giving_feedback" to listOf("adapt-to-listener-style", "warm-and-assured"),
        "m10_receiving_criticism" to listOf("calm-clear-speaking", "active-listening"),
        "m10_objection_handling" to listOf("prep-answer", "active-listening"),
        "m10_negotiation" to listOf("adapt-to-listener-style", "warm-and-assured"),
        "m10_difficult_conversations" to listOf("calm-clear-speaking", "active-listening"),
        "m10_assertive_communication" to listOf("warm-and-assured", "three-channels"),
        "m10_executive_presence" to listOf("three-channels", "warm-and-assured", "pause-not-filler"),
        "m10_explaining_work_experience" to listOf("prep-answer", "relive-the-story"),
        "m10_interview_preparation" to listOf("prep-answer", "record-and-review"),
        "m10_networking" to listOf("conversation-threads", "warm-and-assured")
    )

    /**
     * Primary techniques anchored to each learning path.
     */
    val pathPrimaryTechniques: Map<String, List<String>> = mapOf(
        "path_01_beginner_english_fluency" to listOf("calm-clear-speaking", "articulation-warmup", "conversation-threads", "pause-not-filler"),
        "path_02_intermediate_english_fluency" to listOf("prep-answer", "explain-complex-ideas", "pause-not-filler", "active-listening"),
        "path_03_advanced_communication" to listOf("three-channels", "purposeful-gestures", "relive-the-story", "warm-and-assured"),
        "path_04_pronunciation_improvement" to listOf("articulation-warmup", "vocal-variety", "record-and-review"),
        "path_05_confident_speaking" to listOf("calm-clear-speaking", "pause-not-filler", "warm-and-assured", "three-channels"),
        "path_06_conversation_mastery" to listOf("conversation-threads", "active-listening", "share-highs-and-lows", "adapt-to-listener-style"),
        "path_07_public_speaking" to listOf("three-channels", "purposeful-gestures", "vocal-variety", "prep-answer"),
        "path_08_storytelling" to listOf("relive-the-story", "vocal-variety", "pause-not-filler", "purposeful-gestures"),
        "path_09_professional_communication" to listOf("explain-complex-ideas", "prep-answer", "warm-and-assured", "adapt-to-listener-style"),
        "path_10_interview_preparation" to listOf("prep-answer", "relive-the-story", "record-and-review", "calm-clear-speaking")
    )

    /**
     * Real-world mission tasks paired with each learning path.
     */
    val pathRealWorldMissions: Map<String, List<Pair<String, String>>> = mapOf(
        "path_01_beginner_english_fluency" to listOf(
            "Daily Narrative Mission" to "Describe 3 activities of your day in fluent English aloud for 60 seconds.",
            "Real-World Conversation Opener" to "Initiate a friendly conversational check-in using the FORD framework with a friend or colleague."
        ),
        "path_02_intermediate_english_fluency" to listOf(
            "Impromptu PREP Challenge" to "Give an unscripted 60-second explanation of an everyday topic using Point, Reason, Example, Point.",
            "Filler-Free Meeting Sprint" to "Hold silent deliberate pauses instead of saying 'um' or 'like' during a live discussion."
        ),
        "path_03_advanced_communication" to listOf(
            "Room-Reading & Subtext Alignment" to "Audit nonverbal listener cues in a live meeting and mirror tone before proposing a solution.",
            "Executive Presence Capstone" to "Deliver a 2-minute strategic presentation with grounded posture and zero apologetic hedges."
        ),
        "path_04_pronunciation_improvement" to listOf(
            "Vocal Articulator Workout" to "Complete 3 minutes of lip trills, jaw releases, and tongue twisters before important calls.",
            "Audio Self-Comparison Audit" to "Record and compare a 60-second pronunciation drill against native phonetic cadence."
        ),
        "path_05_confident_speaking" to listOf(
            "Diaphragmatic Breath Reset" to "Perform a 4-7-8 breathing composure cycle immediately before answering unexpected questions.",
            "Zero-Hedge Declarative Pitch" to "Deliver 3 clear declarative statements with downward inflection and zero uptalk."
        ),
        "path_06_conversation_mastery" to listOf(
            "Deep 3-Layer Questioning" to "Ask three consecutive layers of follow-up questions in an organic conversation without interrupting.",
            "High-Low-Buffalo Check-in" to "Share one high, one low, and one surprising observation during your next social exchange."
        ),
        "path_07_public_speaking" to listOf(
            "Scriptless Hook Delivery" to "Open a speech or presentation with a magnetic 30-second sensory story without looking at notes.",
            "Power-Zone Gesture Drill" to "Record a 90-second talk maintaining confident, open hand gestures in the navel-to-chest zone."
        ),
        "path_08_storytelling" to listOf(
            "Everyday Anecdote Reliving" to "Narrate an everyday mishap or triumph to someone using sensory VAKS details and dialogue.",
            "Suspense Pause Integration" to "Hold a full 2-second silent pause right before delivering the climactic punchline of a story."
        ),
        "path_09_professional_communication" to listOf(
            "BLUF Executive Briefing" to "Brief a manager or stakeholder delivering the Bottom Line Up Front within 45 seconds.",
            "Constructive SBI Feedback" to "Deliver situation-behavior-impact feedback warmly and assertively in a workplace context."
        ),
        "path_10_interview_preparation" to listOf(
            "60-Second Elevator Pitch" to "Deliver a crisp, impact-focused self-introduction highlighting your signature strengths.",
            "STAR Behavioral Story Delivery" to "Answer a behavioral challenge question ('Tell me about a time...') in under 90 seconds using STAR."
        )
    )

    val allPaths: List<LearningPathDefinition> = listOf(
        LearningPathDefinition(
            pathId = "path_01_beginner_english_fluency",
            title = "Beginner English Fluency",
            subtitle = "Build daily speaking confidence, clear sentences, and smooth everyday conversations.",
            targetAudience = "Learners establishing consistent daily spoken English habits.",
            difficulty = ActivityDifficulty.BEGINNER,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m01_daily_english_speaking",
                "m01_reading_aloud",
                "m01_sentence_construction",
                "m01_complete_thought_speaking",
                "m01_simple_explanations",
                "m01_daily_activity_descriptions",
                "m01_english_recording",
                "m01_everyday_conversations",
                "m02_mouth_movement_exercises",
                "m03_breathing_practice",
                "m04_intentional_pauses",
                "m05_small_talk"
            ),
            milestones = listOf(
                LearningPathMilestone("bp_m1", "First Fluency Streak", 4, "Completed core daily speaking & sentence builders"),
                LearningPathMilestone("bp_m2", "Everyday Conversationalist", 8, "Recorded baseline fluency and practiced live dialogue"),
                LearningPathMilestone("bp_m3", "Beginner Fluency Graduate", 12, "Mastered all 12 beginner fluency foundations")
            ),
            completionCriteriaSummary = "Complete all 12 core beginner fluency activities with objective score >= 60."
        ),
        LearningPathDefinition(
            pathId = "path_02_intermediate_english_fluency",
            title = "Intermediate English Fluency",
            subtitle = "Think directly in English, speak spontaneously, and organize multi-part explanations.",
            targetAudience = "Speakers comfortable with basics ready for spontaneous clarity.",
            difficulty = ActivityDifficulty.INTERMEDIATE,
            prerequisitePathIds = listOf("path_01_beginner_english_fluency"),
            orderedActivityIds = listOf(
                "m01_impromptu_speaking",
                "m01_random_topic_speaking",
                "m01_expressive_reading",
                "m01_spontaneous_speaking",
                "m01_speech_transcription",
                "m01_sentence_clarity",
                "m01_english_thinking_practice",
                "m03_filler_word_reduction",
                "m04_ccc_framework",
                "m04_organizing_thoughts",
                "m04_structured_answers",
                "m06_listen_and_summarize"
            ),
            milestones = listOf(
                LearningPathMilestone("ip_m1", "Spontaneous Thinker", 4, "Handled impromptu & random topics fluently"),
                LearningPathMilestone("ip_m2", "Structured Speaker", 8, "Cut fillers and sharpened sentence clarity"),
                LearningPathMilestone("ip_m3", "Intermediate Fluency Mastery", 12, "Delivered structured CCC & PREP explanations")
            ),
            completionCriteriaSummary = "Complete all 12 intermediate fluency & clarity activities."
        ),
        LearningPathDefinition(
            pathId = "path_03_advanced_communication",
            title = "Advanced Communication",
            subtitle = "Synthesize executive clarity, subtext listening, persuasive stories, and poise under pressure.",
            targetAudience = "Experienced communicators elevating influence and high-stakes mastery.",
            difficulty = ActivityDifficulty.ADVANCED,
            prerequisitePathIds = listOf("path_02_intermediate_english_fluency"),
            orderedActivityIds = listOf(
                "m02_recording_comparisons",
                "m03_expressive_delivery",
                "m03_voice_confidence",
                "m04_technical_explanations",
                "m06_reading_the_room",
                "m06_listen_and_respond",
                "m07_long_storytelling",
                "m08_gesture_control",
                "m09_unexpected_questions",
                "m09_stage_rehearsal",
                "m10_difficult_conversations",
                "m10_executive_presence"
            ),
            milestones = listOf(
                LearningPathMilestone("ac_m1", "Vocal & Technical Authority", 4, "Refined vocal power & technical framing"),
                LearningPathMilestone("ac_m2", "High-EQ Narrative Leader", 8, "Combined room-reading, story arc & gesture control"),
                LearningPathMilestone("ac_m3", "Master Communicator", 12, "Completed executive presence & stage capstones")
            ),
            completionCriteriaSummary = "Complete all 12 advanced cross-discipline communication capstones."
        ),
        LearningPathDefinition(
            pathId = "path_04_pronunciation_improvement",
            title = "Pronunciation Improvement",
            subtitle = "Unlock crisp consonants, clear vowel contrasts, natural stress, and global intelligibility.",
            targetAudience = "Speakers who want every word heard cleanly on the first try.",
            difficulty = ActivityDifficulty.BEGINNER,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m02_mouth_movement_exercises",
                "m02_jaw_relaxation",
                "m02_lip_trills",
                "m02_tongue_warmups",
                "m02_mouth_opening_exercises",
                "m02_pronunciation_drills",
                "m02_articulation_exercises",
                "m02_difficult_sound_practice",
                "m02_tongue_twisters",
                "m02_consonant_clarity",
                "m02_vowel_pronunciation",
                "m02_word_stress",
                "m02_sentence_stress",
                "m02_accent_awareness",
                "m02_recording_comparisons"
            ),
            milestones = listOf(
                LearningPathMilestone("pr_m1", "Articulator Warm-Up Ready", 5, "Unlocked jaw, lip, and tongue mobility"),
                LearningPathMilestone("pr_m2", "Crisp Sound Precision", 10, "Practiced difficult sounds, twisters, and endings"),
                LearningPathMilestone("pr_m3", "Intelligibility Champion", 15, "Completed all 15 pronunciation & comparison drills")
            ),
            completionCriteriaSummary = "Complete all 15 Module 2 Pronunciation & Articulation activities."
        ),
        LearningPathDefinition(
            pathId = "path_05_confident_speaking",
            title = "Confident Speaking",
            subtitle = "Build breath-grounded tone, unhurried pace, decisive endings, and calm nerve management.",
            targetAudience = "Speakers overcoming rushing, trailing endings, fillers, or speaking anxiety.",
            difficulty = ActivityDifficulty.BEGINNER,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m03_vocal_warmups",
                "m03_breathing_practice",
                "m03_slow_speaking",
                "m03_pause_practice",
                "m03_sentence_endings",
                "m03_filler_word_reduction",
                "m03_volume_control",
                "m03_vocal_image",
                "m03_voice_confidence",
                "m04_declarative_statements",
                "m08_posture",
                "m09_nervousness_management"
            ),
            milestones = listOf(
                LearningPathMilestone("cs_m1", "Calm Grounded Voice", 4, "Mastered breath support, pace, and silent pauses"),
                LearningPathMilestone("cs_m2", "Zero-Hedge Authority", 8, "Eliminated uptalk and apologetic fillers"),
                LearningPathMilestone("cs_m3", "Unshakeable Speaker", 12, "Integrated physical posture & calm nerve ritual")
            ),
            completionCriteriaSummary = "Complete all 12 voice grounding, clarity, and composure activities."
        ),
        LearningPathDefinition(
            pathId = "path_06_conversation_mastery",
            title = "Conversation Mastery",
            subtitle = "Enter chats naturally, ask memorable follow-ups, listen deeply, and build effortless rapport.",
            targetAudience = "Anyone wanting warmer, deeper social and professional conversations.",
            difficulty = ActivityDifficulty.BEGINNER,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m05_first_impressions",
                "m05_small_talk",
                "m05_entering_conversations",
                "m05_ford_framework",
                "m05_open_ended_questions",
                "m05_follow_up_questions",
                "m05_three_follow_up_questions",
                "m05_high_low_buffalo",
                "m05_common_ground_discovery",
                "m05_match_and_mirror",
                "m06_active_listening",
                "m06_reflective_responses",
                "m05_ending_conversations",
                "m05_rapport_building"
            ),
            milestones = listOf(
                LearningPathMilestone("cm_m1", "Effortless Opener", 5, "Mastered warm intros, FORD, and open questions"),
                LearningPathMilestone("cm_m2", "Deep Thread Explorer", 10, "Practiced 3-layer follow-ups and Match & Mirror"),
                LearningPathMilestone("cm_m3", "Connector & Listener", 14, "Completed active listening and real-world rapport")
            ),
            completionCriteriaSummary = "Complete all 14 social fluency and active listening activities."
        ),
        LearningPathDefinition(
            pathId = "path_07_public_speaking",
            title = "Public Speaking",
            subtitle = "Design magnetic openings, deliver scriptless talks, command the lens/stage, and handle Q&A.",
            targetAudience = "Presenters, team leads, creators, and speakers preparing for live audiences.",
            difficulty = ActivityDifficulty.INTERMEDIATE,
            prerequisitePathIds = listOf("path_05_confident_speaking"),
            orderedActivityIds = listOf(
                "m09_nervousness_management",
                "m09_camera_speaking",
                "m09_strong_openings",
                "m09_main_message_structure",
                "m09_memorable_conclusions",
                "m09_speaking_without_scripts",
                "m09_pause_emphasis",
                "m09_vocal_volume",
                "m08_hand_gestures",
                "m08_physical_presence",
                "m09_group_speaking",
                "m09_presentation_storytelling",
                "m09_unexpected_questions",
                "m09_recovering_after_mistakes",
                "m09_stage_rehearsal"
            ),
            milestones = listOf(
                LearningPathMilestone("ps_m1", "Magnetic Opening & Structure", 5, "Built hook, thesis, and memorable closing"),
                LearningPathMilestone("ps_m2", "Stage & Camera Poise", 10, "Mastered pauses, projection, and Power-Zone gestures"),
                LearningPathMilestone("ps_m3", "Keynote Ready", 15, "Passed Q&A, recovery, and full stage dress rehearsal")
            ),
            completionCriteriaSummary = "Complete all 15 Public Speaking and stage presence activities."
        ),
        LearningPathDefinition(
            pathId = "path_08_storytelling",
            title = "Storytelling",
            subtitle = "Transform everyday experiences into unforgettable sensory stories with suspense and impact.",
            targetAudience = "Speakers wanting people to lean in and remember every message.",
            difficulty = ActivityDifficulty.INTERMEDIATE,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m07_story_structure",
                "m07_situation_and_context",
                "m07_challenge_and_conflict",
                "m07_story_progression",
                "m07_outcome_and_lesson",
                "m07_everyday_stories",
                "m07_vaks_framework",
                "m07_concrete_descriptions",
                "m07_vocal_melody_storytelling",
                "m07_suspense_and_pauses",
                "m07_facial_expression_storytelling",
                "m07_gesture_storytelling",
                "m07_audience_connection",
                "m07_short_storytelling",
                "m07_long_storytelling"
            ),
            milestones = listOf(
                LearningPathMilestone("st_m1", "Story Architect", 5, "Mastered Context -> Conflict -> Turn -> Lesson"),
                LearningPathMilestone("st_m2", "Sensory Storyteller", 10, "Added VAKS imagery, melody, and suspense pauses"),
                LearningPathMilestone("st_m3", "Signature Storyteller", 15, "Delivered both 30s micro-stories and 2-min keynotes")
            ),
            completionCriteriaSummary = "Complete all 15 Module 7 Storytelling activities."
        ),
        LearningPathDefinition(
            pathId = "path_09_professional_communication",
            title = "Professional Communication",
            subtitle = "Lead crisp meetings, manage stakeholders, give SBI feedback, negotiate, and project gravitas.",
            targetAudience = "Professionals, managers, engineers, and consultants advancing workplace influence.",
            difficulty = ActivityDifficulty.INTERMEDIATE,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m10_self_introduction",
                "m10_workplace_presentations",
                "m10_meeting_participation",
                "m10_team_discussions",
                "m10_manager_communication",
                "m10_colleague_communication",
                "m10_remote_communication",
                "m10_virtual_meetings",
                "m10_customer_communication",
                "m10_technical_explanations",
                "m10_giving_feedback",
                "m10_receiving_criticism",
                "m10_objection_handling",
                "m10_negotiation",
                "m10_difficult_conversations",
                "m10_assertive_communication",
                "m10_executive_presence"
            ),
            milestones = listOf(
                LearningPathMilestone("pc_m1", "Meeting & Async Clarity", 6, "Mastered BLUF briefings, meetings, and peer alignment"),
                LearningPathMilestone("pc_m2", "High-Trust Collaborator", 12, "Practiced SBI feedback, criticism poise, and customer care"),
                LearningPathMilestone("pc_m3", "Executive Communicator", 17, "Mastered negotiation, boundaries, and executive presence")
            ),
            completionCriteriaSummary = "Complete all 17 workplace and executive communication activities."
        ),
        LearningPathDefinition(
            pathId = "path_10_interview_preparation",
            title = "Interview Preparation",
            subtitle = "Nail self-introductions, STAR behavioral stories, technical walkthroughs, and calm Q&A.",
            targetAudience = "Job seekers and promotion candidates preparing for upcoming interviews.",
            difficulty = ActivityDifficulty.INTERMEDIATE,
            prerequisitePathIds = emptyList(),
            orderedActivityIds = listOf(
                "m10_self_introduction",
                "m04_structured_answers",
                "m07_short_storytelling",
                "m10_explaining_work_experience",
                "m10_interview_preparation",
                "m04_concise_speaking",
                "m10_technical_explanations",
                "m08_video_call_presence",
                "m09_unexpected_questions",
                "m10_negotiation",
                "m10_networking",
                "m09_nervousness_management"
            ),
            milestones = listOf(
                LearningPathMilestone("iv_m1", "Pitch & STAR Ready", 4, "Polished 60s intro, PREP, and work experience stories"),
                LearningPathMilestone("iv_m2", "Live Interview Composure", 8, "Passed interview simulation, conciseness & video presence"),
                LearningPathMilestone("iv_m3", "Offer-Ready Communicator", 12, "Mastered tough Q&A, negotiation, and pre-interview calm")
            ),
            completionCriteriaSummary = "Complete all 12 focused interview communication activities."
        )
    )

    /**
     * Evaluates comprehensive learning progress for a specific learning path across all 4 pillars:
     * Activity, Exercise, Techniques, and Tasks.
     */
    fun evaluatePathProgress(
        path: LearningPathDefinition,
        progressList: List<UserActivityProgress>,
        attemptRecords: List<ActivityAttemptRecord> = emptyList(),
        dailyTasks: List<DailyPracticeTask> = emptyList()
    ): LearningPathProgressSummary {
        val progressMap = progressList.associateBy { it.activityId }
        val attemptsMap = attemptRecords.groupBy { it.activityId }

        // 1. ACTIVITY PILLAR
        val activityItems: List<PathActivityItemProgress> = path.orderedActivityIds.map { actId ->
            val def = ActivityCatalogue.getActivityById(actId)
            val prog = progressMap[actId]
            val atts = attemptsMap[actId].orEmpty()

            val isCompleted = prog?.completed == true || atts.any { it.passed }
            val attemptsCount = maxOf(prog?.attemptsCount?.toInt() ?: 0, atts.size)
            val bestScore = maxOf(
                prog?.bestObjectiveScore?.toInt() ?: 0,
                atts.maxOfOrNull { it.objectiveScore.toInt() } ?: 0
            )
            val latestScore = prog?.latestObjectiveScore?.toInt()
                ?: atts.lastOrNull()?.objectiveScore?.toInt()
                ?: 0
            val latestConfidence = prog?.latestConfidenceSelfRating?.toInt()
                ?: atts.lastOrNull()?.confidenceSelfRating?.toInt()
                ?: 0
            val practiceSecs = maxOf(
                prog?.totalPracticeSeconds ?: 0L,
                atts.sumOf { it.durationSeconds }
            )
            val lastDate = prog?.lastPracticedDateKey?.ifBlank { atts.lastOrNull()?.dateKey.orEmpty() }
                ?: atts.lastOrNull()?.dateKey.orEmpty()

            PathActivityItemProgress(
                activityId = actId,
                title = def?.title ?: actId,
                moduleId = def?.moduleId ?: "general",
                skillCategory = def?.skillCategory ?: ActivitySkillCategory.ENGLISH_FLUENCY,
                exerciseType = def?.exerciseType ?: ExerciseTemplateType.SPEAKING_EXERCISE,
                difficulty = def?.difficulty ?: path.difficulty,
                durationMinutes = def?.duration ?: 5,
                isCompleted = isCompleted,
                attemptsCount = attemptsCount,
                bestObjectiveScore = bestScore,
                latestObjectiveScore = latestScore,
                latestConfidenceRating = latestConfidence,
                totalPracticeSeconds = practiceSecs,
                lastPracticedDateKey = lastDate
            )
        }

        val totalActivities = activityItems.size.coerceAtLeast(1)
        val completedActivities = activityItems.count { it.isCompleted }
        val inProgressActivities = activityItems.count { !it.isCompleted && it.attemptsCount > 0 }
        val activityPercent = ((completedActivities * 100f) / totalActivities).toInt().coerceIn(0, 100)
        val avgScore = activityItems.filter { it.isCompleted }.map { it.bestObjectiveScore }
            .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        val bestScore = activityItems.maxOfOrNull { it.bestObjectiveScore } ?: 0
        val totalAttempts = activityItems.sumOf { it.attemptsCount }
        val totalPracticeMins = (activityItems.sumOf { it.totalPracticeSeconds } / 60).toInt()

        val activityProgress = ActivityPathProgress(
            totalActivities = path.orderedActivityIds.size,
            completedActivities = completedActivities,
            inProgressActivities = inProgressActivities,
            percentComplete = activityPercent,
            averageScore = avgScore,
            bestScore = bestScore,
            totalAttempts = totalAttempts,
            totalPracticeMinutes = totalPracticeMins,
            activities = activityItems
        )

        // 2. EXERCISE PILLAR
        val exerciseGroups = activityItems.groupBy { it.exerciseType }
        val exerciseTypeBreakdown: List<ExerciseTypeProgress> = exerciseGroups.map { (type, items) ->
            val typeTotal = items.size
            val typeCompleted = items.count { it.isCompleted }
            val typePercent = if (typeTotal > 0) ((typeCompleted * 100f) / typeTotal).toInt().coerceIn(0, 100) else 0
            val typeAvgScore = items.filter { it.isCompleted }.map { it.bestObjectiveScore }
                .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0

            ExerciseTypeProgress(
                exerciseType = type,
                typeName = type.displayName,
                family = type.interactionFamily,
                totalCount = typeTotal,
                completedCount = typeCompleted,
                percentComplete = typePercent,
                averageScore = typeAvgScore,
                associatedActivityIds = items.map { it.activityId }
            )
        }.sortedByDescending { it.totalCount }

        val totalExercises = activityItems.size
        val completedExercises = completedActivities
        val exercisePercent = activityPercent
        val distinctTypes = exerciseTypeBreakdown.size
        val completedTypes = exerciseTypeBreakdown.count { it.completedCount == it.totalCount && it.totalCount > 0 }

        val exerciseProgress = ExercisePathProgress(
            totalExercises = totalExercises,
            completedExercises = completedExercises,
            percentComplete = exercisePercent,
            distinctExerciseTypesCount = distinctTypes,
            completedExerciseTypesCount = completedTypes,
            exerciseTypeBreakdown = exerciseTypeBreakdown
        )

        // 3. TECHNIQUES PILLAR
        val pathTechniqueIds = (
            pathPrimaryTechniques[path.pathId].orEmpty() +
            activityItems.flatMap { activityToTechniques[it.activityId].orEmpty() }
        ).distinct()

        val techniqueItems: List<PathTechniqueItemProgress> = pathTechniqueIds.map { techId ->
            val meta = techniquesCatalog[techId]
            val associated = activityItems.filter { it.activityId in (activityToTechniques.filterValues { techId in it }.keys) }
                .map { it.activityId }
                .ifEmpty {
                    // Fallback to primary activities if direct mapping is sparse
                    activityItems.take(2).map { it.activityId }
                }

            val totalForTech = associated.size.coerceAtLeast(1)
            val completedForTech = associated.count { actId ->
                activityItems.firstOrNull { it.activityId == actId }?.isCompleted == true
            }
            val techAvgScore = associated.mapNotNull { actId ->
                activityItems.firstOrNull { it.activityId == actId }?.takeIf { it.isCompleted }?.bestObjectiveScore
            }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0

            val isPracticed = completedForTech > 0 || associated.any { actId ->
                (activityItems.firstOrNull { it.activityId == actId }?.attemptsCount ?: 0) > 0
            }
            val isMastered = completedForTech > 0 && (techAvgScore >= 70 || completedForTech == totalForTech)
            val techPct = ((completedForTech * 100f) / totalForTech).toInt().coerceIn(0, 100)

            PathTechniqueItemProgress(
                techniqueId = techId,
                title = meta?.name ?: techId,
                summary = meta?.summary ?: "Master this core communication framework.",
                coreRule = meta?.coreRule ?: "Practice deliberate delivery and consistent structure.",
                category = meta?.category ?: "Communication",
                associatedActivityIds = associated,
                totalActivitiesCount = totalForTech,
                completedActivitiesCount = completedForTech,
                percentComplete = techPct,
                averageScore = techAvgScore,
                isPracticed = isPracticed,
                isMastered = isMastered
            )
        }

        val totalTechniques = techniqueItems.size
        val practicedTechniques = techniqueItems.count { it.isPracticed }
        val masteredTechniques = techniqueItems.count { it.isMastered }
        val techniquePercent = if (totalTechniques > 0) {
            ((masteredTechniques * 100f) / totalTechniques).toInt().coerceIn(0, 100)
        } else 0

        val techniqueProgress = TechniquePathProgress(
            totalTechniques = totalTechniques,
            practicedTechniques = practicedTechniques,
            masteredTechniques = masteredTechniques,
            percentComplete = techniquePercent,
            techniques = techniqueItems
        )

        // 4. TASKS PILLAR
        val tasksList = mutableListOf<PathTaskItemProgress>()

        // 4a. Activity execution tasks
        activityItems.forEach { act ->
            tasksList += PathTaskItemProgress(
                taskId = "task_act_${path.pathId}_${act.activityId}",
                title = "Drill: ${act.title}",
                description = "Complete the ${act.exerciseType.displayName} practice drill (${act.durationMinutes} min).",
                category = PathTaskCategory.ACTIVITY_TASK,
                isCompleted = act.isCompleted,
                sourceActivityId = act.activityId,
                requiredThreshold = 1,
                currentProgress = if (act.isCompleted) 1 else 0
            )
        }

        // 4b. Milestone checkpoint tasks
        path.milestones.forEach { milestone ->
            val unlocked = completedActivities >= milestone.requiredCompletedCount
            tasksList += PathTaskItemProgress(
                taskId = "task_mile_${path.pathId}_${milestone.milestoneId}",
                title = "Milestone: ${milestone.title}",
                description = "${milestone.rewardSummary} (Requires ${milestone.requiredCompletedCount} activities).",
                category = PathTaskCategory.MILESTONE_CHECKPOINT,
                isCompleted = unlocked,
                requiredThreshold = milestone.requiredCompletedCount,
                currentProgress = completedActivities.coerceAtMost(milestone.requiredCompletedCount)
            )
        }

        // 4c. Real-World Application Missions
        val missions = pathRealWorldMissions[path.pathId].orEmpty()
        missions.forEachIndexed { idx, (mTitle, mDesc) ->
            // A mission completes when learner finishes at least half the path or passes relevant activities
            val threshold = if (idx == 0) (totalActivities / 3).coerceAtLeast(1) else (totalActivities * 2 / 3).coerceAtLeast(2)
            val done = completedActivities >= threshold
            tasksList += PathTaskItemProgress(
                taskId = "task_mission_${path.pathId}_$idx",
                title = mTitle,
                description = mDesc,
                category = PathTaskCategory.REAL_WORLD_APPLICATION,
                isCompleted = done,
                requiredThreshold = threshold,
                currentProgress = completedActivities.coerceAtMost(threshold)
            )
        }

        // 4d. Matching Daily Practice Tasks
        val relevantDailyTasks = dailyTasks.filter { dt ->
            dt.targetRoute.contains(path.pathId) ||
            activityItems.any { it.activityId == dt.targetRoute } ||
            techniqueItems.any { it.title.contains(dt.skillFocus, ignoreCase = true) }
        }
        relevantDailyTasks.forEach { dt ->
            tasksList += PathTaskItemProgress(
                taskId = "task_daily_${path.pathId}_${dt.id}",
                title = "Daily: ${dt.title}",
                description = dt.description,
                category = PathTaskCategory.PRACTICE_DRILL,
                isCompleted = dt.isCompleted,
                sourceActivityId = dt.targetRoute.takeIf { tr -> activityItems.any { it.activityId == tr } }
            )
        }

        val totalTasks = tasksList.size
        val completedTasks = tasksList.count { it.isCompleted }
        val taskPercent = if (totalTasks > 0) ((completedTasks * 100f) / totalTasks).toInt().coerceIn(0, 100) else 0
        val tasksByCategory = tasksList.groupBy { it.category }.mapValues { (_, list) -> list.count { it.isCompleted } }

        val taskProgress = TaskPathProgress(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            percentComplete = taskPercent,
            tasksByCategory = tasksByCategory,
            tasks = tasksList
        )

        // 5. UNIFIED PROGRESS & METRICS
        val prereqsMet = path.prerequisitePathIds.all { prereqId ->
            val prereq = allPaths.firstOrNull { it.pathId == prereqId } ?: return@all true
            val prereqDone = prereq.orderedActivityIds.count { id -> progressMap[id]?.completed == true }
            prereqDone >= (prereq.orderedActivityIds.size * 0.7).toInt().coerceAtLeast(1)
        }

        val nextActivity = path.orderedActivityIds.firstOrNull { id ->
            activityItems.firstOrNull { it.activityId == id }?.isCompleted != true
        }
        val unlockedMilestones = path.milestones.filter { completedActivities >= it.requiredCompletedCount }

        val overallWeighted = (
            (activityPercent * 0.35f) +
            (exercisePercent * 0.25f) +
            (techniquePercent * 0.20f) +
            (taskPercent * 0.20f)
        ).toInt().coerceIn(0, 100)

        val pathStatus = when {
            !prereqsMet -> LearningPathStatus.LOCKED
            completedActivities == path.orderedActivityIds.size && taskPercent >= 90 -> LearningPathStatus.COMPLETED
            completedActivities > 0 || totalAttempts > 0 -> LearningPathStatus.IN_PROGRESS
            else -> LearningPathStatus.NOT_STARTED
        }

        val nextAction = when {
            !prereqsMet -> {
                val prereqTitle = allPaths.firstOrNull { it.pathId in path.prerequisitePathIds }?.title ?: "Prerequisite path"
                "Complete at least 70% of $prereqTitle to unlock this learning path."
            }
            nextActivity != null -> {
                val nextDef = ActivityCatalogue.getActivityById(nextActivity)
                val techName = activityToTechniques[nextActivity]?.firstOrNull()?.let { techniquesCatalog[it]?.name }
                if (techName != null) {
                    "Next: Complete '${nextDef?.title ?: nextActivity}' to master the '$techName' technique."
                } else {
                    "Next: Start '${nextDef?.title ?: nextActivity}' (${nextDef?.exerciseType?.displayName ?: "Practice Drill"})."
                }
            }
            masteredTechniques < totalTechniques -> {
                val unmastered = techniqueItems.firstOrNull { !it.isMastered }?.title ?: "core techniques"
                "Refine '$unmastered' with an objective score >= 70 to achieve full path mastery."
            }
            else -> "All 4 pillars mastered! Ready for advanced communication challenges."
        }

        return LearningPathProgressSummary(
            path = path,
            completedCount = completedActivities,
            totalCount = path.orderedActivityIds.size,
            percentComplete = activityPercent,
            prerequisitesMet = prereqsMet,
            nextRecommendedActivityId = nextActivity,
            unlockedMilestones = unlockedMilestones,
            activityProgress = activityProgress,
            exerciseProgress = exerciseProgress,
            techniqueProgress = techniqueProgress,
            taskProgress = taskProgress,
            overallWeightedProgressPercent = overallWeighted,
            status = pathStatus,
            nextActionRecommendation = nextAction
        )
    }

    fun evaluateAllPaths(
        progressList: List<UserActivityProgress>,
        attemptRecords: List<ActivityAttemptRecord> = emptyList(),
        dailyTasks: List<DailyPracticeTask> = emptyList()
    ): List<LearningPathProgressSummary> =
        allPaths.map { evaluatePathProgress(it, progressList, attemptRecords, dailyTasks) }
}
