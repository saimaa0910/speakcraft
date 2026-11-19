package com.example.data.activity

object LearningPathsEngine {

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

    fun evaluatePathProgress(
        path: LearningPathDefinition,
        progressList: List<UserActivityProgress>
    ): LearningPathProgressSummary {
        val completedIds = progressList.filter { it.completed }.map { it.activityId }.toSet()
        val doneCount = path.orderedActivityIds.count { it in completedIds }
        val total = path.orderedActivityIds.size.coerceAtLeast(1)
        val pct = ((doneCount * 100f) / total).toInt().coerceIn(0, 100)

        val prereqsMet = path.prerequisitePathIds.all { prereqId ->
            val prereq = allPaths.firstOrNull { it.pathId == prereqId } ?: return@all true
            val prereqDone = prereq.orderedActivityIds.count { it in completedIds }
            prereqDone >= (prereq.orderedActivityIds.size * 0.7).toInt().coerceAtLeast(1)
        }

        val nextActivity = path.orderedActivityIds.firstOrNull { it !in completedIds }
        val unlocked = path.milestones.filter { doneCount >= it.requiredCompletedCount }

        return LearningPathProgressSummary(
            path = path,
            completedCount = doneCount,
            totalCount = path.orderedActivityIds.size,
            percentComplete = pct,
            prerequisitesMet = prereqsMet,
            nextRecommendedActivityId = nextActivity,
            unlockedMilestones = unlocked
        )
    }

    fun evaluateAllPaths(progressList: List<UserActivityProgress>): List<LearningPathProgressSummary> =
        allPaths.map { evaluatePathProgress(it, progressList) }
}
