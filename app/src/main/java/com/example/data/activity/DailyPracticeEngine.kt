package com.example.data.activity

import com.example.data.model.UserProfile

object DailyPracticeEngine {

    val supportedDurationsMinutes: List<Int> = listOf(10, 20, 30, 45, 60)

    private fun pillarsForDuration(targetMinutes: Int): List<DailyPracticePillar> = when {
        targetMinutes <= 10 -> listOf(
            DailyPracticePillar.PRONUNCIATION,
            DailyPracticePillar.SPEAKING
        )
        targetMinutes <= 20 -> listOf(
            DailyPracticePillar.PRONUNCIATION,
            DailyPracticePillar.VOICE,
            DailyPracticePillar.SPEAKING,
            DailyPracticePillar.LISTENING
        )
        targetMinutes <= 30 -> listOf(
            DailyPracticePillar.PRONUNCIATION,
            DailyPracticePillar.VOICE,
            DailyPracticePillar.VOCABULARY,
            DailyPracticePillar.SPEAKING,
            DailyPracticePillar.CONVERSATION
        )
        targetMinutes <= 45 -> listOf(
            DailyPracticePillar.PRONUNCIATION,
            DailyPracticePillar.VOICE,
            DailyPracticePillar.VOCABULARY,
            DailyPracticePillar.SPEAKING,
            DailyPracticePillar.LISTENING,
            DailyPracticePillar.CONVERSATION,
            DailyPracticePillar.REFLECTION
        )
        else -> listOf(
            DailyPracticePillar.PRONUNCIATION,
            DailyPracticePillar.VOICE,
            DailyPracticePillar.VOCABULARY,
            DailyPracticePillar.SPEAKING,
            DailyPracticePillar.LISTENING,
            DailyPracticePillar.CONVERSATION,
            DailyPracticePillar.SPEAKING,
            DailyPracticePillar.CONVERSATION,
            DailyPracticePillar.REFLECTION
        )
    }

    fun generatePlan(
        dateKey: String,
        targetMinutes: Int,
        profile: UserProfile?,
        progressList: List<UserActivityProgress>,
        recentAttempts: List<ActivityAttemptRecord>,
        pinnedActivityIds: List<String> = emptyList(),
        completedTodayIds: Set<String> = emptySet(),
        catalog: List<ActivityDefinition> = ActivityCatalogue.allActivities
    ): DailyPracticePlan {
        val safeTarget = supportedDurationsMinutes.minByOrNull { kotlin.math.abs(it - targetMinutes) } ?: 20
        val desiredDifficulty = when (profile?.communicationLevel?.lowercase()) {
            "beginner" -> ActivityDifficulty.BEGINNER
            "advanced" -> ActivityDifficulty.ADVANCED
            else -> ActivityDifficulty.INTERMEDIATE
        }

        val recentlyPracticedIds = recentAttempts
            .take(15)
            .map { it.activityId }
            .toSet()

        val completedIds = progressList.filter { it.completed }.map { it.activityId }.toSet()

        // Identify weakest skills from historical objective attempts & profile baseline
        val skillAverages = recentAttempts
            .groupBy { it.skillCategory }
            .mapValues { (_, list) -> list.map { it.objectiveScore.toInt() }.average() }

        val weakestSkillName = skillAverages.minByOrNull { it.value }?.key
            ?: lowestProfileSkill(profile)

        val goalKeywords = profile?.primaryGoal.orEmpty().lowercase()

        val selected = mutableListOf<DailyPracticeBlock>()
        val usedIds = mutableSetOf<String>()
        var accumulatedMinutes = 0

        // 1. Honor user-pinned activities first (user customization)
        pinnedActivityIds.forEach { pinnedId ->
            val act = catalog.firstOrNull { it.activityId == pinnedId }
            if (act != null && usedIds.add(act.activityId) && accumulatedMinutes + act.duration <= safeTarget + 5) {
                selected += DailyPracticeBlock(
                    activityId = act.activityId,
                    title = act.title,
                    pillar = act.skillCategory.dailyPillar,
                    durationMinutes = act.duration,
                    difficulty = act.difficulty,
                    completed = act.activityId in completedTodayIds,
                    pinned = true,
                    reason = "Pinned to your custom routine"
                )
                accumulatedMinutes += act.duration
            }
        }

        // 2. Fill balanced pillars up to targetMinutes avoiding unnecessary repetition
        val pillarSequence = pillarsForDuration(safeTarget)
        for (pillar in pillarSequence) {
            if (accumulatedMinutes >= safeTarget) break
            val remainingBudget = (safeTarget - accumulatedMinutes).coerceAtLeast(4)
            val candidates = catalog.filter { act ->
                act.activityId !in usedIds &&
                    matchesPillar(act, pillar) &&
                    act.duration <= remainingBudget + 3
            }.ifEmpty {
                catalog.filter { it.activityId !in usedIds && it.duration <= remainingBudget + 3 }
            }

            val best = candidates.maxByOrNull { act ->
                scoreCandidate(
                    activity = act,
                    desiredDifficulty = desiredDifficulty,
                    weakestSkillName = weakestSkillName,
                    goalKeywords = goalKeywords,
                    recentlyPracticedIds = recentlyPracticedIds,
                    completedIds = completedIds,
                    dateKey = dateKey
                )
            }

            if (best != null && usedIds.add(best.activityId)) {
                val reason = buildReason(best, weakestSkillName, goalKeywords, best.activityId !in completedIds)
                selected += DailyPracticeBlock(
                    activityId = best.activityId,
                    title = best.title,
                    pillar = pillar,
                    durationMinutes = best.duration,
                    difficulty = best.difficulty,
                    completed = best.activityId in completedTodayIds,
                    pinned = false,
                    reason = reason
                )
                accumulatedMinutes += best.duration
            }
        }

        // 3. Top up if long session (45/60m) still has room for another meaningful drill
        while (accumulatedMinutes + 5 <= safeTarget) {
            val nextExtra = catalog
                .filter { it.activityId !in usedIds && it.duration <= (safeTarget - accumulatedMinutes) + 2 }
                .maxByOrNull { act ->
                    scoreCandidate(act, desiredDifficulty, weakestSkillName, goalKeywords, recentlyPracticedIds, completedIds, dateKey)
                } ?: break
            usedIds.add(nextExtra.activityId)
            selected += DailyPracticeBlock(
                activityId = nextExtra.activityId,
                title = nextExtra.title,
                pillar = nextExtra.skillCategory.dailyPillar,
                durationMinutes = nextExtra.duration,
                difficulty = nextExtra.difficulty,
                completed = nextExtra.activityId in completedTodayIds,
                pinned = false,
                reason = "Extended deep-practice block for ${nextExtra.skillCategory.label}"
            )
            accumulatedMinutes += nextExtra.duration
        }

        val summary = buildString {
            append("${safeTarget}-min balanced plan (${selected.size} activities)")
            if (!weakestSkillName.isNullOrBlank()) {
                append(" • prioritizing ${weakestSkillName.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)}")
            }
            if (goalKeywords.isNotBlank()) {
                append(" • aligned with '${profile?.primaryGoal}'")
            }
        }

        return DailyPracticePlan(
            dateKey = dateKey,
            targetMinutes = safeTarget,
            blocks = selected,
            totalPlannedMinutes = accumulatedMinutes,
            personalizationSummary = summary
        )
    }

    fun swapBlockInPlan(
        currentPlan: DailyPracticePlan,
        activityIdToReplace: String,
        recentAttempts: List<ActivityAttemptRecord>,
        catalog: List<ActivityDefinition> = ActivityCatalogue.allActivities
    ): DailyPracticePlan {
        val idx = currentPlan.blocks.indexOfFirst { it.activityId == activityIdToReplace }
        if (idx < 0) return currentPlan
        val oldBlock = currentPlan.blocks[idx]
        val existingIds = currentPlan.blocks.map { it.activityId }.toSet()
        val recentIds = recentAttempts.take(10).map { it.activityId }.toSet()

        val replacement = catalog
            .filter { it.activityId !in existingIds && matchesPillar(it, oldBlock.pillar) }
            .sortedBy { if (it.activityId in recentIds) 1 else 0 }
            .firstOrNull()
            ?: catalog.firstOrNull { it.activityId !in existingIds }
            ?: return currentPlan

        val updatedBlocks = currentPlan.blocks.toMutableList()
        updatedBlocks[idx] = DailyPracticeBlock(
            activityId = replacement.activityId,
            title = replacement.title,
            pillar = oldBlock.pillar,
            durationMinutes = replacement.duration,
            difficulty = replacement.difficulty,
            completed = false,
            pinned = true,
            reason = "Swapped into your custom routine"
        )
        return currentPlan.copy(
            blocks = updatedBlocks,
            totalPlannedMinutes = updatedBlocks.sumOf { it.durationMinutes }
        )
    }

    private fun matchesPillar(act: ActivityDefinition, pillar: DailyPracticePillar): Boolean = when (pillar) {
        DailyPracticePillar.REFLECTION ->
            act.exerciseType == ExerciseTemplateType.REFLECTION_SELF_ASSESSMENT ||
                act.exerciseType == ExerciseTemplateType.REAL_WORLD_TASK ||
                act.skillCategory == ActivitySkillCategory.REFLECTION
        else -> act.skillCategory.dailyPillar == pillar
    }

    private fun scoreCandidate(
        activity: ActivityDefinition,
        desiredDifficulty: ActivityDifficulty,
        weakestSkillName: String?,
        goalKeywords: String,
        recentlyPracticedIds: Set<String>,
        completedIds: Set<String>,
        dateKey: String
    ): Int {
        var score = 100
        // Strongly avoid recent repetition
        if (activity.activityId in recentlyPracticedIds) score -= 55
        if (activity.activityId in completedIds) score -= 20

        // Match proficiency smoothly (allow +/- 1 tier)
        val diffGap = kotlin.math.abs(activity.difficulty.rank - desiredDifficulty.rank)
        score += when (diffGap) {
            0 -> 25
            1 -> 10
            else -> -12
        }

        // Prioritize weak skill area
        if (!weakestSkillName.isNullOrBlank() && activity.skillCategory.name.equals(weakestSkillName, ignoreCase = true)) {
            score += 30
        }

        // Goal alignment bonus
        if (goalKeywords.isNotBlank()) {
            val haystack = "${activity.title} ${activity.description} ${activity.skillCategory.label}".lowercase()
            val words = goalKeywords.split(Regex("\\W+")).filter { it.length >= 4 }
            if (words.any { haystack.contains(it) }) {
                score += 22
            }
        }

        // Deterministic daily rotation jitter so each day feels fresh without random flakiness
        val dailyJitter = ((activity.activityId.hashCode() xor dateKey.hashCode()) and 0x7FFFFFFF) % 13
        return score + dailyJitter
    }

    private fun lowestProfileSkill(profile: UserProfile?): String? {
        if (profile == null) return null
        val pairs = listOf(
            ActivitySkillCategory.ENGLISH_FLUENCY.name to profile.fluencyScore,
            ActivitySkillCategory.PRONUNCIATION.name to profile.pronunciationScore,
            ActivitySkillCategory.VOCABULARY.name to profile.vocabularyScore,
            ActivitySkillCategory.GRAMMAR.name to profile.grammarScore,
            ActivitySkillCategory.VOICE_MASTERY.name to profile.articulationScore,
            ActivitySkillCategory.ACTIVE_LISTENING.name to profile.listeningScore
        )
        return pairs.minByOrNull { it.second }?.first
    }

    private fun buildReason(
        activity: ActivityDefinition,
        weakestSkillName: String?,
        goalKeywords: String,
        isNew: Boolean
    ): String = when {
        !weakestSkillName.isNullOrBlank() && activity.skillCategory.name.equals(weakestSkillName, ignoreCase = true) ->
            "Targets your current growth focus in ${activity.skillCategory.label}"
        goalKeywords.isNotBlank() && activity.title.lowercase().split(" ").any { it.length > 3 && goalKeywords.contains(it) } ->
            "Directly supports your communication goal"
        isNew ->
            "Fresh ${activity.difficulty.label.lowercase()} drill for ${activity.skillCategory.label}"
        else ->
            "Reinforces core ${activity.skillCategory.label} mastery"
    }
}
