package com.example.data.learningpaths

/**
 * Shared rubric factories and concise lesson/module builders for the 7 Learning Path programs.
 */
internal object LearningPathCatalogHelpers {

    fun rubricForType(type: LessonExerciseType): List<PathRubricCriterion> = when (type) {
        LessonExerciseType.SPEAKING_DRILL -> listOf(
            PathRubricCriterion("clarity", "Vocal Clarity & Pacing", "Measured speed with intentional pauses instead of rushing.", 35),
            PathRubricCriterion("structure", "Thought Structure", "Clear opening point supported by concrete reasoning.", 35),
            PathRubricCriterion("alignment", "Lesson Technique Focus", "Applies today's core vocal or delivery concepts.", 30)
        )
        LessonExerciseType.LISTENING_SHADOW -> listOf(
            PathRubricCriterion("prosody", "Rhythm & Phrasing Match", "Mirrors breath groups, pauses, and rising/falling intonation.", 40),
            PathRubricCriterion("comprehension", "Accurate Paraphrase", "Captures underlying intent and key facts accurately.", 35),
            PathRubricCriterion("precision", "Clean Language", "Avoids filler words and fragments while summarizing.", 25)
        )
        LessonExerciseType.VOCAB_USAGE -> listOf(
            PathRubricCriterion("collocation", "Target Word Usage", "Uses target words accurately inside natural spoken phrases.", 45),
            PathRubricCriterion("specificity", "Lexical Precision", "Replaces vague words ('thing', 'stuff', 'good') with exact terms.", 30),
            PathRubricCriterion("flow", "Sentence Grammar & Flow", "Integrates elevated vocabulary without stiffness.", 25)
        )
        LessonExerciseType.GRAMMAR_REWRITE -> listOf(
            PathRubricCriterion("accuracy", "Grammatical Accuracy", "Subject-verb agreement, clean tense, and complete clauses.", 45),
            PathRubricCriterion("concision", "Direct Phrasing", "Cuts weak qualifiers and redundant modifiers.", 30),
            PathRubricCriterion("naturalness", "Spoken Naturalness", "Sounds idiomatic when delivered aloud.", 25)
        )
        LessonExerciseType.STORY_BUILDING -> listOf(
            PathRubricCriterion("arc", "Narrative Arc", "Includes setup, challenge/turning point, and resolution/insight.", 40),
            PathRubricCriterion("detail", "Sensory Specificity", "Uses concrete moments, quotes, or visual details rather than summary.", 35),
            PathRubricCriterion("takeaway", "Clear Lesson Takeaway", "Closes with a crisp message relevant to the listener.", 25)
        )
        LessonExerciseType.PRONUNCIATION_DRILL -> listOf(
            PathRubricCriterion("articulation", "Sound & Consonant Crispness", "Articulates final stops, clusters, and target contrast pairs clearly.", 45),
            PathRubricCriterion("stress", "Syllable & Sentence Stress", "Places emphasis on stressed syllables and core content words.", 30),
            PathRubricCriterion("steady_pace", "Breath & Pace Stability", "Maintains steady breath support across full phrases.", 25)
        )
        LessonExerciseType.SCENARIO_ROLEPLAY -> listOf(
            PathRubricCriterion("tact", "Tone & Composure", "Balances warmth with grounded conviction under pressure.", 35),
            PathRubricCriterion("actionable", "Practical Response Structure", "Addresses the stakeholder question or objection directly.", 40),
            PathRubricCriterion("alignment", "Key Elements Applied", "Includes target phrasing and clear next step.", 25)
        )
        LessonExerciseType.REFLECTION_AUDIT -> listOf(
            PathRubricCriterion("specificity", "Concrete Observation", "Identifies measurable habits (pace, fillers, posture, syntax).", 40),
            PathRubricCriterion("diagnosis", "Root-Cause Insight", "Connects observed habit to why listeners lose clarity or trust.", 35),
            PathRubricCriterion("action_plan", "Actionable Adjustment", "States a specific repeatable tweak for the next session.", 25)
        )
    }

    fun assessmentRubric(isCapstone: Boolean): List<PathRubricCriterion> = if (isCapstone) {
        listOf(
            PathRubricCriterion("cap_structure", "Executive Structure & Coherence", "Delivers compelling opening, logical progression, and memorable closing.", 35),
            PathRubricCriterion("cap_language", "Language Precision & Fluency", "Uses clean syntax, purposeful vocabulary, and minimal fillers/hedges.", 35),
            PathRubricCriterion("cap_elements", "Module Mastery Synthesis", "Demonstrates multiple program techniques in one unified performance.", 30)
        )
    } else {
        listOf(
            PathRubricCriterion("mod_application", "Practical Technique Application", "Transfers module concepts cleanly into spoken/written output.", 45),
            PathRubricCriterion("mod_clarity", "Clarity & Concision", "Direct, coherent phrasing without drifting off-topic.", 30),
            PathRubricCriterion("mod_accuracy", "Scenario & Concept Recall", "Demonstrates sound judgment across knowledge checks.", 25)
        )
    }

    /**
     * Compact specification used by program catalogs to construct rich, complete [LearningPathLesson] items.
     */
    data class DaySpec(
        val day: Int,
        val title: String,
        val subtitle: String,
        val type: LessonExerciseType,
        val activityId: String,
        val labRoute: String? = null,
        val focus: String,
        val weakExample: String,
        val strongExample: String,
        val whyContrastWorks: String,
        val prompt: String,
        val modelScript: String,
        val keyTerms: List<String>,
        val quizPrompt: String,
        val quizOptions: List<String>,
        val quizCorrectIndex: Int,
        val quizExplanation: String,
        val estimatedMinutes: Int = 8,
        val minWords: Int = 22
    )

    fun toLesson(
        programId: String,
        moduleId: String,
        spec: DaySpec
    ): LearningPathLesson {
        val lessonId = "${programId}_day_${spec.day.toString().padStart(2, '0')}"
        val checklist = when (spec.type) {
            LessonExerciseType.PRONUNCIATION_DRILL -> listOf(
                "Listen to the model cadence twice before recording",
                "Articulate stressed syllables and final consonants cleanly",
                "Keep a steady breath column through the closing phrase"
            )
            LessonExerciseType.VOCAB_USAGE -> listOf(
                "Review the exact meaning and typical collocation of each keyword",
                "Use at least two target words naturally in spoken sentences",
                "Replace vague filler adjectives ('good', 'nice', 'things') with precise choices"
            )
            LessonExerciseType.STORY_BUILDING -> listOf(
                "Anchor the opening with a concrete moment in time or place",
                "Show one clear challenge or decision in the middle",
                "Land a concise single-sentence insight at the close"
            )
            LessonExerciseType.SCENARIO_ROLEPLAY -> listOf(
                "Acknowledge the listener's point calmly before pivoting",
                "State your core position in one direct sentence",
                "Propose a specific, realistic next action"
            )
            else -> listOf(
                "Read the core concept and Before/After contrast first",
                "Deliver your own spoken or written response around ${spec.minWords}+ words",
                "Run evaluation and apply at least one rewrite tip before advancing"
            )
        }

        val conceptBody = "${spec.focus} When speakers skip this step, listeners work harder to decode intent or tune out due to uneven pacing, vague phrasing, or weak endings. Practicing this intentionally builds repeatable muscle memory under live pressure."

        val takeaways = listOf(
            "Core Focus: ${spec.subtitle}",
            "Key Signal: Anchor your delivery with ${spec.keyTerms.take(3).joinToString(", ") { "'$it'" }}",
            "Quality Check: Compare your draft against the Before/After contrast and remove hesitation phrases."
        )

        return LearningPathLesson(
            lessonId = lessonId,
            programId = programId,
            moduleId = moduleId,
            dayNumber = spec.day,
            title = spec.title,
            subtitle = spec.subtitle,
            learningObjective = "Day ${spec.day}: Apply ${spec.subtitle.lowercase()} through structured speaking, diagnostic review, and targeted refinement.",
            coreConceptExplanation = conceptBody,
            keyTakeaways = takeaways,
            exampleContrast = BeforeAfterContrast(
                beforeLabel = "Typical Unstructured Draft",
                weakExample = spec.weakExample,
                afterLabel = "Focused Delivery",
                strongExample = spec.strongExample,
                whyItWorks = spec.whyContrastWorks
            ),
            focusKeywords = spec.keyTerms,
            exerciseType = spec.type,
            exercisePrompt = spec.prompt,
            practiceChecklist = checklist,
            ttsModelScript = spec.modelScript,
            targetWordCountMin = spec.minWords,
            estimatedMinutes = spec.estimatedMinutes,
            passingScoreThreshold = 60,
            linkedActivityId = spec.activityId,
            linkedLabRoute = spec.labRoute,
            quizQuestions = listOf(
                PathQuizQuestion(
                    questionId = "${lessonId}_q1",
                    prompt = spec.quizPrompt,
                    options = spec.quizOptions,
                    correctIndex = spec.quizCorrectIndex,
                    explanation = spec.quizExplanation
                )
            ),
            assessmentRubric = rubricForType(spec.type),
            expectedKeyTerms = spec.keyTerms
        )
    }

    fun buildModule(
        programId: String,
        orderIndex: Int,
        title: String,
        subtitle: String,
        objectives: List<String>,
        concepts: List<String>,
        daySpecs: List<DaySpec>,
        assessmentTitle: String,
        assessmentOverview: String,
        practicalTaskPrompt: String,
        expectedElements: List<String>,
        scenarioQuestions: List<PathQuizQuestion>,
        isCapstone: Boolean = false
    ): LearningPathModule {
        val moduleId = "${programId}_mod_${orderIndex}"
        val lessons = daySpecs.sortedBy { it.day }.map { toLesson(programId, moduleId, it) }
        val assessmentSpec = ModuleAssessmentSpec(
            assessmentId = "${moduleId}_assessment",
            moduleId = moduleId,
            programId = programId,
            isCapstone = isCapstone,
            title = assessmentTitle,
            overview = assessmentOverview,
            passingScoreThreshold = 70,
            scenarioQuestions = scenarioQuestions,
            practicalTaskPrompt = practicalTaskPrompt,
            expectedElements = expectedElements,
            minWords = if (isCapstone) 45 else 32,
            suggestedMinutes = if (isCapstone) 8 else 5,
            rubric = assessmentRubric(isCapstone)
        )
        return LearningPathModule(
            moduleId = moduleId,
            programId = programId,
            orderIndex = orderIndex,
            title = title,
            subtitle = subtitle,
            learningObjectives = objectives,
            keyConcepts = concepts,
            dayStart = lessons.first().dayNumber,
            dayEnd = lessons.last().dayNumber,
            lessons = lessons,
            assessment = assessmentSpec
        )
    }
}
