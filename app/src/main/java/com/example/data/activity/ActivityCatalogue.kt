package com.example.data.activity

enum class ActivityDurationFilter(val label: String) {
    ALL("Any Duration"),
    QUICK("Quick (<= 5 min)"),
    STANDARD("Standard (6–7 min)"),
    DEEP("Deep Dive (8+ min)")
}

data class ActivitySearchFilter(
    val query: String = "",
    val moduleId: String? = null,
    val difficulty: ActivityDifficulty? = null,
    val exerciseType: ExerciseTemplateType? = null,
    val skillCategory: ActivitySkillCategory? = null,
    val durationFilter: ActivityDurationFilter = ActivityDurationFilter.ALL
)

object ActivityValidator {

    private val idRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    fun validate(activity: ActivityDefinition?): ActivityValidationResult {
        if (activity == null) {
            return ActivityValidationResult(valid = false, errors = listOf("Activity definition is null."))
        }
        val errors = mutableListOf<String>()
        if (activity.activityId.isBlank() || !idRegex.matches(activity.activityId)) {
            errors += "Invalid or empty activityId: '${activity.activityId}'."
        }
        if (activity.title.isBlank()) {
            errors += "Title must not be blank."
        }
        if (activity.description.isBlank()) {
            errors += "Description must not be blank."
        }
        if (activity.moduleId.isBlank()) {
            errors += "moduleId must not be blank."
        }
        if (activity.duration !in 1..120) {
            errors += "Duration (${activity.duration}) must be between 1 and 120 minutes."
        }
        if (activity.learningObjective.isBlank()) {
            errors += "learningObjective must not be blank."
        }
        if (activity.instructions.isEmpty() || activity.instructions.any { it.isBlank() }) {
            errors += "instructions must contain at least one non-blank step."
        }
        if (activity.examples.isEmpty() || activity.examples.any { it.isBlank() }) {
            errors += "examples must contain at least one non-blank example."
        }
        if (activity.interactionConfiguration.promptText.isBlank()) {
            errors += "interactionConfiguration.promptText must not be blank."
        }
        if (activity.scoringRubric.isEmpty()) {
            errors += "scoringRubric must contain at least one dimension."
        } else {
            val weightSum = activity.scoringRubric.sumOf { it.weightPercent }
            if (weightSum != 100) {
                errors += "scoringRubric weights must sum to 100 (was $weightSum)."
            }
        }
        if (activity.completionCriteria.passingScoreThreshold !in 1..100) {
            errors += "completionCriteria.passingScoreThreshold must be between 1 and 100."
        }
        if (activity.sourceAttribution.isBlank()) {
            errors += "sourceAttribution must not be blank."
        }
        if (activity.contentVersion < 1) {
            errors += "contentVersion must be >= 1."
        }
        return ActivityValidationResult(valid = errors.isEmpty(), errors = errors)
    }

    fun validateCatalogue(
        modules: List<ActivityModuleDefinition>,
        activities: List<ActivityDefinition>
    ): ActivityValidationResult {
        val errors = mutableListOf<String>()
        val moduleIds = modules.map { it.moduleId }.toSet()
        val seenIds = mutableSetOf<String>()
        val allIds = activities.map { it.activityId }.toSet()

        activities.forEach { act ->
            if (!seenIds.add(act.activityId)) {
                errors += "Duplicate activityId: ${act.activityId}"
            }
            if (act.moduleId !in moduleIds) {
                errors += "Activity ${act.activityId} references unknown moduleId ${act.moduleId}"
            }
            val single = validate(act)
            if (!single.valid) {
                errors += "${act.activityId}: ${single.errors.joinToString("; ")}"
            }
            act.nextActivityIds.forEach { nextId ->
                if (nextId !in allIds) {
                    errors += "Activity ${act.activityId} references missing nextActivityId '$nextId'"
                }
            }
        }
        return ActivityValidationResult(valid = errors.isEmpty(), errors = errors)
    }
}

object ActivityCatalogue {

    val modules: List<ActivityModuleDefinition> = listOf(
        ActivityModuleDefinition(
            moduleId = "mod_01_english_fluency",
            order = 1,
            title = "English Fluency",
            subtitle = "Daily spontaneous speaking, expressive reading, clarity & thinking in English",
            skillCategory = ActivitySkillCategory.ENGLISH_FLUENCY,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_02_pronunciation_articulation",
            order = 2,
            title = "Pronunciation & Articulation",
            subtitle = "Oral warm-ups, difficult sounds, tongue twisters, stress & recording comparisons",
            skillCategory = ActivitySkillCategory.PRONUNCIATION,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_03_voice_mastery",
            order = 3,
            title = "Voice Mastery",
            subtitle = "Breath support, pitch, melody, pauses, projection, filler control & vocal confidence",
            skillCategory = ActivitySkillCategory.VOICE_MASTERY,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_04_speaking_clarity",
            order = 4,
            title = "Speaking Clarity",
            subtitle = "CCC framework, analogies, concise delivery, structured answers & summarization",
            skillCategory = ActivitySkillCategory.SPEAKING_CLARITY,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_05_conversation_mastery",
            order = 5,
            title = "Conversation Mastery",
            subtitle = "FORD, High-Low-Buffalo, follow-up threads, Match & Mirror & rapport building",
            skillCategory = ActivitySkillCategory.CONVERSATION,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_06_active_listening",
            order = 6,
            title = "Active Listening",
            subtitle = "Subtext, emotional listening, reflective responses, listening games & reading the room",
            skillCategory = ActivitySkillCategory.ACTIVE_LISTENING,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_07_storytelling",
            order = 7,
            title = "Storytelling",
            subtitle = "Narrative arcs, VAKS sensory details, suspense pauses, short & long storytelling",
            skillCategory = ActivitySkillCategory.STORYTELLING,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_08_body_language",
            order = 8,
            title = "Body Language",
            subtitle = "Posture, eye contact, facial congruence, Power-Zone gestures & video-call presence",
            skillCategory = ActivitySkillCategory.BODY_LANGUAGE,
            targetActivityCount = 10
        ),
        ActivityModuleDefinition(
            moduleId = "mod_09_public_speaking",
            order = 9,
            title = "Public Speaking",
            subtitle = "Camera speaking, strong hooks, scriptless delivery, Q&A recovery & stage rehearsal",
            skillCategory = ActivitySkillCategory.PUBLIC_SPEAKING,
            targetActivityCount = 15
        ),
        ActivityModuleDefinition(
            moduleId = "mod_10_professional_communication",
            order = 10,
            title = "Professional Communication",
            subtitle = "Interviews, BLUF briefings, stakeholder alignment, feedback, negotiation & executive presence",
            skillCategory = ActivitySkillCategory.PROFESSIONAL,
            targetActivityCount = 20
        )
    )

    val allActivities: List<ActivityDefinition> by lazy {
        ActivityCatalogueModules1To3.module1Activities +
            ActivityCatalogueModules1To3.module2Activities +
            ActivityCatalogueModules1To3.module3Activities +
            ActivityCatalogueModules4To6.module4Activities +
            ActivityCatalogueModules4To6.module5Activities +
            ActivityCatalogueModules4To6.module6Activities +
            ActivityCatalogueModules7To10.module7Activities +
            ActivityCatalogueModules7To10.module8Activities +
            ActivityCatalogueModules7To10.module9Activities +
            ActivityCatalogueModules7To10.module10Activities
    }

    private val byId: Map<String, ActivityDefinition> by lazy {
        allActivities.associateBy { it.activityId }
    }

    private val modulesById: Map<String, ActivityModuleDefinition> by lazy {
        modules.associateBy { it.moduleId }
    }

    fun getActivityById(activityId: String): ActivityDefinition? = byId[activityId]

    fun getModuleById(moduleId: String): ActivityModuleDefinition? = modulesById[moduleId]

    fun getActivitiesForModule(moduleId: String): List<ActivityDefinition> =
        allActivities.filter { it.moduleId == moduleId }

    fun getValidatedActivity(activityId: String): Pair<ActivityDefinition?, ActivityValidationResult> {
        val definition = byId[activityId]
        val validation = ActivityValidator.validate(definition)
        return definition to validation
    }

    fun searchAndFilter(filter: ActivitySearchFilter): List<ActivityDefinition> {
        val q = filter.query.trim()
        return allActivities.filter { activity ->
            val matchesQuery = q.isBlank() ||
                activity.title.contains(q, ignoreCase = true) ||
                activity.description.contains(q, ignoreCase = true) ||
                activity.learningObjective.contains(q, ignoreCase = true) ||
                activity.skillCategory.label.contains(q, ignoreCase = true) ||
                activity.exerciseType.displayName.contains(q, ignoreCase = true) ||
                (modulesById[activity.moduleId]?.title?.contains(q, ignoreCase = true) == true)

            val matchesModule = filter.moduleId == null || activity.moduleId == filter.moduleId
            val matchesDifficulty = filter.difficulty == null || activity.difficulty == filter.difficulty
            val matchesType = filter.exerciseType == null || activity.exerciseType == filter.exerciseType
            val matchesSkill = filter.skillCategory == null || activity.skillCategory == filter.skillCategory
            val matchesDuration = when (filter.durationFilter) {
                ActivityDurationFilter.ALL -> true
                ActivityDurationFilter.QUICK -> activity.duration <= 5
                ActivityDurationFilter.STANDARD -> activity.duration in 6..7
                ActivityDurationFilter.DEEP -> activity.duration >= 8
            }

            matchesQuery && matchesModule && matchesDifficulty && matchesType && matchesSkill && matchesDuration
        }
    }

    fun paginate(
        items: List<ActivityDefinition>,
        page: Int,
        pageSize: Int = 20
    ): List<ActivityDefinition> {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(5, 100)
        return items.take(safePage * safeSize)
    }
}
