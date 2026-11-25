package com.example.data.learningpaths

import com.example.data.learning.LearningGoal
import com.example.data.learning.LearningLevel
import kotlin.math.roundToInt

/**
 * Computes transparent personalized Learning Path recommendations, program statistics,
 * prerequisite advisories, and post-graduation next steps completely offline.
 */
object LearningPathRecommendationEngine {

    fun computeProgramStats(
        program: LearningPathProgram,
        enrollment: PathEnrollmentRecord?
    ): ProgramUserStats {
        if (enrollment == null) {
            return ProgramUserStats(
                programId = program.programId,
                enrolled = false,
                active = false,
                paused = false,
                completedDaysCount = 0,
                totalDays = program.durationDays,
                passedModulesCount = 0,
                totalModulesCount = program.modules.size,
                capstonePassed = false,
                programCompleted = false,
                averageLessonScore = 0,
                averageConfidenceRating = 0f,
                totalPracticeSeconds = 0,
                nextDayNumber = 1,
                nextLesson = LearningPathCurriculumRegistry.getLesson(program.programId, 1),
                completionFraction = 0f
            )
        }

        val validCompletedDays = enrollment.completedDays
            .filter { it in 1..program.durationDays }
            .distinct()
            .sorted()

        val passedModules = program.modules.count { module ->
            enrollment.moduleAssessments[module.moduleId]?.passed == true
        }
        val capstoneModule = program.modules.firstOrNull { it.assessment.isCapstone }
        val capstonePassed = capstoneModule?.let {
            enrollment.moduleAssessments[it.moduleId]?.passed == true
        } ?: false

        val scoredLessons = enrollment.dayRecords.values.filter { it.attempts > 0 && it.bestScore > 0 }
        val avgScore = if (scoredLessons.isEmpty()) 0 else {
            scoredLessons.map { it.bestScore }.average().roundToInt()
        }
        val ratedConfidence = enrollment.dayRecords.values.filter { it.confidenceSelfRating > 0 }
        val avgConfidence = if (ratedConfidence.isEmpty()) 0f else {
            ratedConfidence.map { it.confidenceSelfRating }.average().toFloat()
        }
        val totalPracticeSec = enrollment.dayRecords.values.sumOf { it.practiceDurationSeconds }

        val programCompleted = validCompletedDays.size >= program.completionCriteria.requiredDaysCount &&
            passedModules >= program.completionCriteria.requiredAssessmentsCount

        val nextDay = (1..program.durationDays).firstOrNull { day ->
            !validCompletedDays.contains(day)
        } ?: program.durationDays

        val lessonWeight = 0.80f
        val moduleWeight = 0.20f
        val dayFrac = (validCompletedDays.size.toFloat() / program.durationDays.coerceAtLeast(1)).coerceIn(0f, 1f)
        val modFrac = (passedModules.toFloat() / program.modules.size.coerceAtLeast(1)).coerceIn(0f, 1f)
        val completionFraction = (dayFrac * lessonWeight + modFrac * moduleWeight).coerceIn(0f, 1f)

        return ProgramUserStats(
            programId = program.programId,
            enrolled = true,
            active = enrollment.active && !enrollment.paused,
            paused = enrollment.paused,
            completedDaysCount = validCompletedDays.size,
            totalDays = program.durationDays,
            passedModulesCount = passedModules,
            totalModulesCount = program.modules.size,
            capstonePassed = capstonePassed,
            programCompleted = programCompleted,
            averageLessonScore = avgScore,
            averageConfidenceRating = avgConfidence,
            totalPracticeSeconds = totalPracticeSec,
            nextDayNumber = nextDay,
            nextLesson = LearningPathCurriculumRegistry.getLesson(program.programId, nextDay),
            completionFraction = completionFraction
        )
    }

    fun recommendPrograms(
        programs: List<LearningPathProgram>,
        userLevel: LearningLevel,
        userGoal: LearningGoal,
        userState: LearningPathsUserState,
        weakestSkillHint: String? = null,
        limit: Int = 3
    ): List<ProgramRecommendation> {
        val completedProgramIds = programs.filter { program ->
            computeProgramStats(program, userState.enrollments[program.programId]).programCompleted
        }.map { it.programId }.toSet()

        return programs.map { program ->
            val stats = computeProgramStats(program, userState.enrollments[program.programId])
            var score = 50
            val reasons = mutableListOf<String>()

            // Active continuation priority unless completed
            if (stats.enrolled && !stats.paused && !stats.programCompleted) {
                score += 22
                reasons += "Continue Day ${stats.nextDayNumber} of ${program.durationDays} already in progress"
            } else if (stats.programCompleted) {
                score -= 30
            }

            // Skill level fit
            when (program.difficultyTier) {
                PathDifficultyTier.FOUNDATIONAL -> {
                    if (userLevel.rank <= 2) {
                        score += 25
                        reasons += "Tailored for ${userLevel.label} foundation building"
                    } else if (userLevel.rank >= 4) {
                        score -= 10
                    }
                }
                PathDifficultyTier.ALL_LEVELS -> {
                    score += 15
                }
                PathDifficultyTier.INTERMEDIATE -> {
                    if (userLevel.rank in 2..4) {
                        score += 22
                        reasons += "Strong difficulty match for ${userLevel.label} level"
                    } else if (userLevel.rank == 1) {
                        score -= 12
                    }
                }
                PathDifficultyTier.ADVANCED -> {
                    if (userLevel.rank >= 3) {
                        score += 25
                        reasons += "Designed for advancing ${userLevel.label} executive mastery"
                    } else {
                        score -= 22
                    }
                }
            }

            // Goal alignment
            val goalBoost = when (userGoal) {
                LearningGoal.PRONUNCIATION -> if (program.programId == "pronunciation_program") 35 else 0
                LearningGoal.VOCABULARY -> if (program.programId == "vocab_challenge") 35 else 0
                LearningGoal.INTERVIEWS, LearningGoal.WORKPLACE -> if (program.programId == "interview_program") 35 else if (program.programId == "90_day_advanced") 15 else 0
                LearningGoal.PUBLIC_SPEAKING, LearningGoal.PRESENTATIONS -> if (program.programId == "public_speaking_program") 35 else if (program.programId == "90_day_advanced") 15 else 0
                LearningGoal.DAILY_FLUENCY, LearningGoal.CONVERSATION -> if (program.programId == "60_day_fluency") 30 else if (program.programId == "30_day_challenge") 22 else 0
                LearningGoal.CONFIDENCE -> if (program.programId == "30_day_challenge" || program.programId == "public_speaking_program") 28 else 0
            }
            if (goalBoost > 0) {
                score += goalBoost
                reasons += "Matches your '${userGoal.label}' focus"
            }

            // Weakest measured skill alignment
            if (!weakestSkillHint.isNullOrBlank()) {
                val lowerSkill = weakestSkillHint.lowercase()
                val matchesWeakness = when {
                    lowerSkill.contains("pronun") || lowerSkill.contains("articulat") -> program.programId == "pronunciation_program"
                    lowerSkill.contains("vocab") -> program.programId == "vocab_challenge"
                    lowerSkill.contains("grammar") || lowerSkill.contains("fluency") -> program.programId == "60_day_fluency"
                    lowerSkill.contains("story") || lowerSkill.contains("public") -> program.programId == "public_speaking_program"
                    else -> program.programId == "30_day_challenge"
                }
                if (matchesWeakness) {
                    score += 18
                    reasons += "Directly strengthens your weakest measured skill ($weakestSkillHint)"
                }
            }

            // Prerequisite readiness check
            val unmetPrereqs = program.prerequisiteProgramIds.filter { it !in completedProgramIds }
            if (unmetPrereqs.isNotEmpty()) {
                score -= 10
            } else if (program.prerequisiteProgramIds.isNotEmpty()) {
                score += 12
                reasons += "Prerequisites already completed"
            }

            val priorityTag = when {
                stats.enrolled && !stats.paused && !stats.programCompleted -> "Continue"
                goalBoost >= 30 -> "Best Goal Match"
                program.programId == "30_day_challenge" && completedProgramIds.isEmpty() -> "Best Starting Path"
                else -> program.difficultyTier.label
            }

            val reasonSummary = reasons.firstOrNull()
                ?: "${program.durationDays}-day guided track focusing on ${program.primarySkillFocus.take(2).joinToString(" & ")}"

            ProgramRecommendation(
                program = program,
                matchScore = score.coerceIn(0, 100),
                reason = reasonSummary,
                priorityTag = priorityTag
            )
        }
            .sortedByDescending { it.matchScore }
            .take(limit)
    }

    fun recommendFollowOnPrograms(
        completedProgram: LearningPathProgram,
        allPrograms: List<LearningPathProgram>,
        userState: LearningPathsUserState
    ): List<LearningPathProgram> {
        val explicitNext = completedProgram.recommendedNextProgramIds
            .mapNotNull { nextId -> allPrograms.firstOrNull { it.programId == nextId } }
            .filter { !computeProgramStats(it, userState.enrollments[it.programId]).programCompleted }
        if (explicitNext.isNotEmpty()) return explicitNext
        return allPrograms.filter {
            it.programId != completedProgram.programId &&
                !computeProgramStats(it, userState.enrollments[it.programId]).programCompleted
        }.take(2)
    }

    fun nextActionAdvice(
        program: LearningPathProgram,
        enrollment: PathEnrollmentRecord?
    ): String {
        val stats = computeProgramStats(program, enrollment)
        if (!stats.enrolled) {
            return "Preview syllabus highlights and enroll when ready—takes ~${program.estimatedDailyMinutes} min/day."
        }
        if (stats.paused) {
            return "Program paused at Day ${stats.nextDayNumber}. Resume anytime without losing progress."
        }
        if (stats.programCompleted) {
            return "Completed ${stats.totalDays}/${stats.totalDays} days and passed all ${stats.totalModulesCount} assessments! Explore a next-level program."
        }
        // Check whether a completed module is waiting strictly for its module assessment
        val pendingModuleAssessment = program.modules.firstOrNull { module ->
            val moduleDaysDone = module.lessons.all { lesson -> enrollment?.completedDays?.contains(lesson.dayNumber) == true }
            val modulePassed = enrollment?.moduleAssessments?.get(module.moduleId)?.passed == true
            moduleDaysDone && !modulePassed
        }
        if (pendingModuleAssessment != null) {
            return "All lessons in '${pendingModuleAssessment.title}' are complete—pass its Checkpoint Assessment to unlock full module mastery."
        }
        if (stats.completedDaysCount >= program.durationDays) {
            return "All ${program.durationDays} lessons checked off! Pass remaining module assessment(s) to graduate."
        }
        return "Up next: Day ${stats.nextDayNumber} — ${stats.nextLesson?.title ?: "Continue Module"}."
    }

    fun unmetPrerequisites(
        program: LearningPathProgram,
        allPrograms: List<LearningPathProgram>,
        userState: LearningPathsUserState
    ): List<LearningPathProgram> {
        return program.prerequisiteProgramIds.mapNotNull { reqId ->
            val target = allPrograms.firstOrNull { it.programId == reqId } ?: return@mapNotNull null
            val done = computeProgramStats(target, userState.enrollments[reqId]).programCompleted
            if (done) null else target
        }
    }
}
