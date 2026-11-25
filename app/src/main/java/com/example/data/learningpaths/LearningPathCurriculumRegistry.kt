package com.example.data.learningpaths

import com.example.data.activity.ActivityCatalogue
import com.example.data.model.ProgramWeekPlan
import com.example.data.model.StructuredProgram

/**
 * Single authoritative registry for SpeakCraft's 7 Complete Learning Path Programs
 * (28 modules, 280 daily lessons, and 28 graded module/capstone assessments).
 */
object LearningPathCurriculumRegistry {

    val allPrograms: List<LearningPathProgram> by lazy {
        listOf(
            LearningPathContentCatalog1.build30DayChallenge(),
            LearningPathContentCatalog1.build60DayFluencyProgram(),
            LearningPathContentCatalog2.build90DayAdvancedProgram(),
            LearningPathContentCatalog3.buildVocabChallengeProgram(),
            LearningPathContentCatalog3.buildPronunciationProgram(),
            LearningPathContentCatalog3.buildPublicSpeakingProgram(),
            LearningPathContentCatalog3.buildInterviewProgram()
        )
    }

    private val programsById: Map<String, LearningPathProgram> by lazy {
        allPrograms.associateBy { it.programId }
    }

    private val lessonsById: Map<String, LearningPathLesson> by lazy {
        allPrograms.flatMap { program ->
            program.modules.flatMap { it.lessons }
        }.associateBy { it.lessonId }
    }

    private val lessonsByActivityId: Map<String, List<LearningPathLesson>> by lazy {
        allPrograms.flatMap { program ->
            program.modules.flatMap { it.lessons }
        }.groupBy { it.linkedActivityId }
    }

    fun getProgram(programId: String): LearningPathProgram? = programsById[programId]

    fun getModule(programId: String, moduleId: String): LearningPathModule? =
        programsById[programId]?.modules?.firstOrNull { it.moduleId == moduleId }

    fun getLesson(programId: String, dayNumber: Int): LearningPathLesson? =
        programsById[programId]?.modules
            ?.asSequence()
            ?.flatMap { it.lessons.asSequence() }
            ?.firstOrNull { it.dayNumber == dayNumber }

    fun getLessonById(lessonId: String): LearningPathLesson? = lessonsById[lessonId]

    fun getLessonsUsingActivity(activityId: String): List<LearningPathLesson> =
        lessonsByActivityId[activityId].orEmpty()

    /**
     * Converts the 7 full curriculum programs into compatibility [StructuredProgram] summaries
     * so legacy callers, analytics, and policy tests always receive 7 programs with all N days populated.
     */
    fun toStructuredPrograms(): List<StructuredProgram> = allPrograms.map { program ->
        StructuredProgram(
            id = program.programId,
            title = program.title,
            durationDays = program.durationDays,
            subtitle = program.subtitle,
            weeks = program.modules.map { module ->
                ProgramWeekPlan(
                    weekNumber = module.orderIndex,
                    title = module.title,
                    focusAreas = module.keyConcepts,
                    dailyDrills = module.lessons.map { lesson ->
                        "Day ${lesson.dayNumber}: ${lesson.title} — ${lesson.subtitle}"
                    }
                )
            }
        )
    }

    /**
     * Collects all learner-facing curriculum copy for auditing and search indexing.
     */
    fun collectProgramDisplayText(program: LearningPathProgram): String {
        val textParts = mutableListOf(
            program.title, program.subtitle, program.overview, program.targetAudience
        )
        textParts.addAll(program.learningObjectives)
        for (module in program.modules) {
            textParts.addAll(
                listOf(
                    module.title, module.subtitle, module.assessment.title,
                    module.assessment.overview, module.assessment.practicalTaskPrompt
                )
            )
            module.assessment.scenarioQuestions.forEach { q ->
                textParts.add(q.prompt)
                textParts.addAll(q.options)
                textParts.add(q.explanation)
            }
            for (lesson in module.lessons) {
                lesson.quizQuestions.forEach { q ->
                    textParts.add(q.prompt)
                    textParts.addAll(q.options)
                    textParts.add(q.explanation)
                }
                textParts.addAll(
                    listOf(
                        lesson.title, lesson.subtitle, lesson.learningObjective,
                        lesson.coreConceptExplanation, lesson.exampleContrast.weakExample,
                        lesson.exampleContrast.strongExample, lesson.exampleContrast.whyItWorks,
                        lesson.exercisePrompt, lesson.ttsModelScript
                    )
                )
            }
        }
        return textParts.joinToString("\n")
    }

    /**
     * Validates structural and referential integrity across all 7 programs.
     * Returns a list of errors; an empty list indicates complete integrity.
     */
    fun validateIntegrity(): List<String> {
        val errors = mutableListOf<String>()
        if (allPrograms.size != 7) {
            errors.add("Expected 7 programs, found ${allPrograms.size}")
        }
        val collapsedDayRangeRegex = Regex("""Day\s+\d+\s*[-–]\s*\d+""", RegexOption.IGNORE_CASE)

        for (program in allPrograms) {
            val lessons = program.modules.flatMap { it.lessons }
            if (lessons.size != program.durationDays) {
                errors.add("${program.programId}: expected ${program.durationDays} lessons, found ${lessons.size}")
            }
            val expectedDays = (1..program.durationDays).toList()
            val actualDays = lessons.map { it.dayNumber }.sorted()
            if (actualDays != expectedDays) {
                errors.add("${program.programId}: day numbers do not form continuous 1..${program.durationDays}")
            }
            if (program.modules.none { it.assessment.isCapstone }) {
                errors.add("${program.programId}: missing capstone module assessment")
            }

            for (module in program.modules) {
                module.assessment.scenarioQuestions.forEach { q ->
                    if (q.correctIndex !in q.options.indices) {
                        errors.add("${program.programId}/${module.moduleId}/${q.questionId}: invalid correctIndex")
                    }
                }
                for (lesson in module.lessons) {
                    if (ActivityCatalogue.getActivityById(lesson.linkedActivityId) == null) {
                        errors.add("${lesson.lessonId}: linkedActivityId '${lesson.linkedActivityId}' not found in ActivityCatalogue")
                    }
                    lesson.quizQuestions.forEach { q ->
                        if (q.correctIndex !in q.options.indices) {
                            errors.add("${lesson.lessonId}/${q.questionId}: invalid correctIndex")
                        }
                    }
                    if (collapsedDayRangeRegex.containsMatchIn(lesson.title) || collapsedDayRangeRegex.containsMatchIn(lesson.subtitle)) {
                        errors.add("${lesson.lessonId}: collapsed day range detected in title/subtitle")
                    }
                }
            }
        }
        return errors
    }
}
