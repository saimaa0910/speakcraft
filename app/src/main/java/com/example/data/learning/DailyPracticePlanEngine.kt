package com.example.data.learning

/**
 * Deterministic, 100% On-Device Personalized Daily Practice Plan Engine.
 *
 * Adapts daily tasks to:
 * - Current communication level (Beginner, Elementary, Intermediate, Upper Intermediate, Advanced)
 * - Selected goals (Daily Fluency, Pronunciation, Vocabulary, Confidence, Interviews, Public Speaking, etc.)
 * - Daily time commitment (minutes budget)
 * - Weakest skill scores & recurring mistakes (injects targeted remediation drills)
 * - Mastery graduation (skips mastered skills in favor of stretch challenges)
 * - Missed-day recovery (builds an achievable momentum-rebuilding plan after missed days)
 * - Mid-session immutability (preserves completed tasks when adapting remaining tasks)
 */
object DailyPracticePlanEngine {

    data class PlanGenerationInput(
        val dateKey: String,
        val level: LearningLevel,
        val goals: List<LearningGoal>,
        val targetMinutes: Int,
        val skillScores: Map<String, Int> = emptyMap(),
        val recurringMistakes: List<String> = emptyList(),
        val masteredSkills: Set<String> = emptySet(),
        val missedDaysBeforeToday: Int = 0,
        val wordsDueForReview: Int = 0,
        val existingPlanForToday: PersonalizedDailyPlan? = null,
        val nowMs: Long = System.currentTimeMillis()
    )

    fun generateOrAdaptPlan(input: PlanGenerationInput): PersonalizedDailyPlan {
        val effectiveGoals = input.goals.ifEmpty { listOf(LearningGoal.DAILY_FLUENCY) }
        val budget = input.targetMinutes.coerceIn(10, 90)
        val isRecovery = input.missedDaysBeforeToday >= 2

        // Preserve any already-completed tasks from today's existing plan (mid-session immutability)
        val preservedCompleted = if (input.existingPlanForToday?.dateKey == input.dateKey) {
            input.existingPlanForToday.tasks.filter { it.isCompleted }
        } else emptyList()
        val preservedIds = preservedCompleted.map { it.id }.toSet()

        val weakestSkill = input.skillScores
            .filterKeys { it !in input.masteredSkills }
            .minByOrNull { it.value }
            ?.key ?: effectiveGoals.first().focusSkills.firstOrNull() ?: "Fluency"

        val candidates = mutableListOf<DailyPracticeTask>()

        // 1. Recovery or Warm-up Anchor
        if (isRecovery) {
            candidates += DailyPracticeTask(
                id = "recovery_warmup_${input.dateKey}",
                title = "2-Min Confidence Reset Warm-up",
                description = "Gentle vocal reset after ${input.missedDaysBeforeToday} missed days — rebuild momentum without pressure.",
                category = "Recovery",
                durationMinutes = 3,
                targetRoute = "voice_training",
                skillFocus = "Confidence",
                reasonTag = "Missed-day recovery"
            )
        }

        // 2. Core Vocabulary Task (always scaled to level & due review count)
        val vocabCount = when {
            isRecovery -> 2
            input.level == LearningLevel.BEGINNER || input.level == LearningLevel.ELEMENTARY -> 2
            budget >= 25 -> 4
            else -> 3
        }
        val vocabTitle = if (input.wordsDueForReview > 0) {
            "Learn $vocabCount Words & Review ${input.wordsDueForReview} Due"
        } else {
            "Master $vocabCount ${input.level.label} Vocabulary Words"
        }
        candidates += DailyPracticeTask(
            id = "task_vocab_5stage",
            title = vocabTitle,
            description = "Complete the 5-stage Learn → Speak → Sentence → Complete workflow.",
            category = "Vocabulary",
            durationMinutes = if (isRecovery) 4 else 5,
            targetRoute = "vocabulary",
            skillFocus = "Vocabulary",
            reasonTag = if (input.wordsDueForReview > 0) "Spaced repetition due" else "Core daily pillar"
        )

        // 3. Targeted Remediation for Recurring Mistakes (highest adaptive priority)
        val mistakeText = input.recurringMistakes.joinToString(" ").lowercase()
        when {
            mistakeText.contains("filler") || mistakeText.contains("um") || mistakeText.contains("uh") -> {
                candidates += DailyPracticeTask(
                    id = "remed_zero_fillers",
                    title = "Zero-Filler Pause Control Sprint",
                    description = "Targeted remediation: replace fillers with calm 1-second silent pauses.",
                    category = "Remediation",
                    durationMinutes = 4,
                    targetRoute = "challenges",
                    skillFocus = "Fluency",
                    reasonTag = "Fixes recurring fillers",
                    isRemediation = true
                )
            }
            mistakeText.contains("grammar") || mistakeText.contains("tense") || mistakeText.contains("agreement") -> {
                candidates += DailyPracticeTask(
                    id = "remed_grammar_drill",
                    title = "Spoken Grammar Precision Drill",
                    description = "Targeted remediation for recurring sentence structure & verb agreement slips.",
                    category = "Remediation",
                    durationMinutes = 5,
                    targetRoute = "grammar_speaking",
                    skillFocus = "Grammar",
                    reasonTag = "Fixes recurring grammar",
                    isRemediation = true
                )
            }
            mistakeText.contains("pace") || mistakeText.contains("fast") || mistakeText.contains("pronun") -> {
                candidates += DailyPracticeTask(
                    id = "remed_articulation_pace",
                    title = "Articulation & Pacing Calibration",
                    description = "Slow down final consonants and hit the 130–160 WPM sweet spot.",
                    category = "Remediation",
                    durationMinutes = 5,
                    targetRoute = "articulation_lab",
                    skillFocus = "Pronunciation",
                    reasonTag = "Fixes pace/clarity",
                    isRemediation = true
                )
            }
        }

        // 4. Weakest Skill Targeted Task (skipping mastered skills)
        if ("Pronunciation" !in input.masteredSkills && (weakestSkill.equals("Pronunciation", true) || weakestSkill.equals("Articulation", true) || LearningGoal.PRONUNCIATION in effectiveGoals)) {
            candidates += DailyPracticeTask(
                id = "task_pronunciation",
                title = when (input.level) {
                    LearningLevel.BEGINNER, LearningLevel.ELEMENTARY -> "Clear Sound & Syllable Drill"
                    LearningLevel.INTERMEDIATE -> "Articulation & Enunciation Drill"
                    else -> "Executive Stress & Intonation Polish"
                },
                description = "Practice crisp consonants, stress placement, and downward authority endings.",
                category = "Pronunciation",
                durationMinutes = if (isRecovery) 4 else 5,
                targetRoute = "articulation_lab",
                skillFocus = "Pronunciation",
                reasonTag = "Weakest skill focus"
            )
        }

        // 5. Goal-Specific Primary Activity
        val primaryGoal = effectiveGoals.first()
        candidates += buildGoalTask(primaryGoal, input.level, isRecovery)

        // 6. Speaking Lab / Recording Task
        candidates += DailyPracticeTask(
            id = "task_speaking_record",
            title = when (input.level) {
                LearningLevel.BEGINNER -> "30-Second Guided Speaking Clip"
                LearningLevel.ELEMENTARY -> "45-Second Daily Story Recording"
                LearningLevel.INTERMEDIATE -> "60-Second PREP Speaking Recording"
                LearningLevel.UPPER_INTERMEDIATE -> "90-Second Executive Clarity Audit"
                LearningLevel.ADVANCED -> "2-Minute Persuasive Delivery Audit"
            },
            description = "Record locally with live speech capture and review your pace, grammar, and fillers.",
            category = "Speaking",
            durationMinutes = if (isRecovery) 4 else 6,
            targetRoute = "speaking_lab",
            skillFocus = "Fluency",
            reasonTag = "${input.level.label} core drill"
        )

        // 7. Listening / Shadowing or Grammar Construction (for medium/longer budgets)
        if (!isRecovery && "Listening" !in input.masteredSkills) {
            candidates += DailyPracticeTask(
                id = "task_listening_shadow",
                title = "Active Listening & Cadence Shadowing",
                description = "Mirror vocal melody, phrasing pauses, and rhythm from reference audio.",
                category = "Listening",
                durationMinutes = 5,
                targetRoute = "listening_lab",
                skillFocus = "Listening",
                reasonTag = "Rhythm & comprehension"
            )
        }

        if (!isRecovery && "Grammar" !in input.masteredSkills) {
            candidates += DailyPracticeTask(
                id = "task_sentence_craft",
                title = "Contextual Sentence Construction",
                description = "Build smooth spoken sentences with natural connectors and tense control.",
                category = "Grammar",
                durationMinutes = 4,
                targetRoute = "grammar_speaking",
                skillFocus = "Grammar",
                reasonTag = "Sentence fluency"
            )
        }

        // 8. Daily Reflection Anchor
        candidates += DailyPracticeTask(
            id = "task_daily_reflection",
            title = "Daily Communication Reflection",
            description = "Capture one win and one focus lever for tomorrow.",
            category = "Reflection",
            durationMinutes = 2,
            targetRoute = "daily_practice",
            skillFocus = "Confidence",
            reasonTag = "Reinforces habit"
        )

        // Assemble final task list respecting time budget and preserving completed tasks
        val finalTasks = mutableListOf<DailyPracticeTask>()
        finalTasks.addAll(preservedCompleted)
        var usedMinutes = preservedCompleted.sumOf { it.durationMinutes }
        val maxTasks = if (isRecovery) 4 else if (budget <= 15) 5 else 6

        for (candidate in candidates) {
            if (finalTasks.size >= maxTasks) break
            if (candidate.id in preservedIds) continue
            if (finalTasks.size >= 3 && usedMinutes + candidate.durationMinutes > budget + 5) continue
            finalTasks.add(candidate)
            usedMinutes += candidate.durationMinutes
        }

        val summary = buildString {
            if (isRecovery) {
                append("Welcome back! Built a lighter ${usedMinutes}-min recovery plan after ${input.missedDaysBeforeToday} missed days. ")
            } else {
                append("Tailored for ${input.level.label} • ${primaryGoal.label} (~${usedMinutes} min). ")
            }
            append("Prioritizing $weakestSkill.")
        }

        return PersonalizedDailyPlan(
            dateKey = input.dateKey,
            level = input.level,
            goals = effectiveGoals,
            targetMinutes = budget,
            isRecoveryPlan = isRecovery,
            missedDaysBeforeToday = input.missedDaysBeforeToday,
            adaptiveSummary = summary,
            tasks = finalTasks,
            generatedAtMs = input.nowMs
        )
    }

    private fun buildGoalTask(goal: LearningGoal, level: LearningLevel, isRecovery: Boolean): DailyPracticeTask {
        val mins = if (isRecovery) 4 else 6
        return when (goal) {
            LearningGoal.INTERVIEWS -> DailyPracticeTask(
                id = "task_goal_interview",
                title = "STAR Behavioral Interview Roleplay",
                description = "Practice structured interview answers with contextual follow-up prompts.",
                category = "Conversation",
                durationMinutes = mins,
                targetRoute = "conversation",
                skillFocus = "Fluency",
                reasonTag = "Goal: Job Interviews"
            )
            LearningGoal.PUBLIC_SPEAKING, LearningGoal.PRESENTATIONS -> DailyPracticeTask(
                id = "task_goal_presentation",
                title = "Magnetic Opening & Rule-of-3 Pitch",
                description = "Deliver a crisp structured presentation segment with vocal variety.",
                category = "Presentation",
                durationMinutes = mins,
                targetRoute = "public_speaking",
                skillFocus = "Articulation",
                reasonTag = "Goal: ${goal.label}"
            )
            LearningGoal.CONVERSATION, LearningGoal.WORKPLACE -> DailyPracticeTask(
                id = "task_goal_conversation",
                title = "Real-World ${goal.label} Simulation",
                description = "Respond smoothly across 3+ realistic turns with local coaching feedback.",
                category = "Conversation",
                durationMinutes = mins,
                targetRoute = "conversation",
                skillFocus = "Listening",
                reasonTag = "Goal: ${goal.label}"
            )
            LearningGoal.PRONUNCIATION -> DailyPracticeTask(
                id = "task_goal_voice",
                title = "5 Vocal Foundations Modulation Drill",
                description = "Practice pitch, pace, pauses, and volume contrast for ${level.label} speakers.",
                category = "Voice",
                durationMinutes = mins,
                targetRoute = "voice_training",
                skillFocus = "Pronunciation",
                reasonTag = "Goal: Pronunciation"
            )
            else -> DailyPracticeTask(
                id = "task_goal_fluency",
                title = "The 3Vs Foundation & Fluency Drill",
                description = "Combine visual poise, vocal warmth, and verbal clarity in one focused exercise.",
                category = "Foundation",
                durationMinutes = mins,
                targetRoute = "voice_training",
                skillFocus = "Fluency",
                reasonTag = "Goal: ${goal.label}"
            )
        }
    }
}
