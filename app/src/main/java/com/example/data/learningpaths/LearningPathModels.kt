package com.example.data.learningpaths

import com.example.speech.pipeline.TranscriptInsight
import kotlinx.serialization.Serializable

/**
 * Difficulty tier for a structured Learning Path program.
 */
enum class PathDifficultyTier(val label: String) {
    FOUNDATIONAL("Foundational"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    ALL_LEVELS("All Levels")
}

/**
 * Primary practice modality for a daily Learning Path lesson.
 */
enum class LessonExerciseType(val label: String) {
    SPEAKING_DRILL("Spoken Drill"),
    LISTENING_SHADOW("Listening & Shadowing"),
    VOCAB_USAGE("Applied Vocabulary"),
    GRAMMAR_REWRITE("Precision & Grammar Rewrite"),
    STORY_BUILDING("Story Construction"),
    PRONUNCIATION_DRILL("Articulation & Stress"),
    SCENARIO_ROLEPLAY("Scenario Response"),
    REFLECTION_AUDIT("Diagnostic Reflection")
}

/**
 * Side-by-side before/after contrast illustrating weak vs strong delivery.
 */
data class BeforeAfterContrast(
    val beforeLabel: String = "Common Unfocused Draft",
    val weakExample: String,
    val afterLabel: String = "Targeted SpeakCraft Delivery",
    val strongExample: String,
    val whyItWorks: String
)

/**
 * Knowledge-check or scenario question included in lessons and module assessments.
 */
data class PathQuizQuestion(
    val questionId: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Objective rubric criterion evaluated on each lesson or assessment submission.
 */
data class PathRubricCriterion(
    val id: String,
    val title: String,
    val description: String,
    val maxWeightPercent: Int
)

/**
 * Complete daily learning unit inside a Learning Path module.
 */
data class LearningPathLesson(
    val lessonId: String,
    val programId: String,
    val moduleId: String,
    val dayNumber: Int,
    val title: String,
    val subtitle: String,
    val learningObjective: String,
    val coreConceptExplanation: String,
    val keyTakeaways: List<String>,
    val exampleContrast: BeforeAfterContrast,
    val focusKeywords: List<String>,
    val exerciseType: LessonExerciseType,
    val exercisePrompt: String,
    val practiceChecklist: List<String>,
    val ttsModelScript: String,
    val targetWordCountMin: Int,
    val estimatedMinutes: Int,
    val passingScoreThreshold: Int = 60,
    val linkedActivityId: String,
    val linkedLabRoute: String? = null,
    val quizQuestions: List<PathQuizQuestion> = emptyList(),
    val assessmentRubric: List<PathRubricCriterion> = emptyList(),
    val expectedKeyTerms: List<String> = emptyList()
)

/**
 * Graded module checkpoint or final program capstone assessment.
 */
data class ModuleAssessmentSpec(
    val assessmentId: String,
    val moduleId: String,
    val programId: String,
    val isCapstone: Boolean,
    val title: String,
    val overview: String,
    val passingScoreThreshold: Int = 70,
    val scenarioQuestions: List<PathQuizQuestion>,
    val practicalTaskPrompt: String,
    val expectedElements: List<String>,
    val minWords: Int = 35,
    val suggestedMinutes: Int = 6,
    val rubric: List<PathRubricCriterion>
)

/**
 * Weekly or thematic phase inside a structured Learning Path program.
 */
data class LearningPathModule(
    val moduleId: String,
    val programId: String,
    val orderIndex: Int,
    val title: String,
    val subtitle: String,
    val learningObjectives: List<String>,
    val keyConcepts: List<String>,
    val dayStart: Int,
    val dayEnd: Int,
    val lessons: List<LearningPathLesson>,
    val assessment: ModuleAssessmentSpec
)

/**
 * Explicit graduation and completion gates for a Learning Path program.
 */
data class ProgramCompletionCriteria(
    val requiredDaysCount: Int,
    val requiredAssessmentsCount: Int,
    val minimumLessonAverageScore: Int = 60,
    val minimumAssessmentScore: Int = 70,
    val description: String
)

/**
 * Top-level structured Learning Path program definition.
 */
data class LearningPathProgram(
    val programId: String,
    val title: String,
    val subtitle: String,
    val overview: String,
    val targetAudience: String,
    val difficultyTier: PathDifficultyTier,
    val recommendedLevelRange: String,
    val categoryTag: String,
    val durationDays: Int,
    val estimatedDailyMinutes: Int,
    val primarySkillFocus: List<String>,
    val learningObjectives: List<String>,
    val prerequisiteProgramIds: List<String>,
    val recommendedNextProgramIds: List<String>,
    val completionCriteria: ProgramCompletionCriteria,
    val modules: List<LearningPathModule>
)

// ---------------------------------------------------------------------------
// Evaluation & Recommendation Domain Models
// ---------------------------------------------------------------------------

@Serializable
data class RubricCriterionScore(
    val criterionId: String,
    val title: String,
    val scoreOutOf100: Int,
    val note: String
)

@Serializable
data class CorrectedExampleItem(
    val original: String,
    val improved: String,
    val reason: String
)

data class LessonSubmissionRequest(
    val programId: String,
    val dayNumber: Int,
    val spokenOrWrittenText: String,
    val audioFilePath: String? = null,
    val audioDurationSeconds: Int = 0,
    val quizAnswers: Map<String, Int> = emptyMap(),
    val completedChecklistIndices: Set<Int> = emptySet(),
    val confidenceSelfRating: Int = 3,
    val reflectionNote: String = "",
    val practiceDurationSeconds: Int = 0
)

/**
 * Structured coaching evaluation output returned by [LearningPathEvaluationEngine].
 */
data class LearningPathFeedback(
    val overallScore: Int,
    val passed: Boolean,
    val confidenceSelfRating: Int,
    val quizScorePercent: Int,
    val quizCorrectCount: Int,
    val quizTotalCount: Int,
    val whatWentWell: List<String>,
    val mistakesFound: List<String>,
    val correctedExamples: List<CorrectedExampleItem>,
    val explanation: String,
    val coachTips: List<String>,
    val transcriptSummary: String,
    val transcriptInsights: List<TranscriptInsight>,
    val rubricBreakdown: List<RubricCriterionScore>,
    val recommendedNextStep: String,
    val evidenceNote: String
)

data class ModuleAssessmentSubmission(
    val programId: String,
    val moduleId: String,
    val scenarioAnswers: Map<String, Int>,
    val practicalResponseText: String,
    val audioFilePath: String? = null,
    val audioDurationSeconds: Int = 0,
    val confidenceSelfRating: Int = 4,
    val practiceDurationSeconds: Int = 0
)

data class ModuleAssessmentEvaluation(
    val assessmentId: String,
    val moduleId: String,
    val programId: String,
    val quizScorePercent: Int,
    val practicalScorePercent: Int,
    val combinedScore: Int,
    val passed: Boolean,
    val feedback: LearningPathFeedback,
    val evaluatedAtEpochMs: Long
)

data class ProgramRecommendation(
    val program: LearningPathProgram,
    val matchScore: Int,
    val reason: String,
    val priorityTag: String
)

data class ProgramUserStats(
    val programId: String,
    val enrolled: Boolean,
    val active: Boolean,
    val paused: Boolean,
    val completedDaysCount: Int,
    val totalDays: Int,
    val passedModulesCount: Int,
    val totalModulesCount: Int,
    val capstonePassed: Boolean,
    val programCompleted: Boolean,
    val averageLessonScore: Int,
    val averageConfidenceRating: Float,
    val totalPracticeSeconds: Int,
    val nextDayNumber: Int,
    val nextLesson: LearningPathLesson?,
    val completionFraction: Float
)

// ---------------------------------------------------------------------------
// Persisted Offline-First User State Models
// ---------------------------------------------------------------------------

@Serializable
data class DayLessonProgressRecord(
    val lessonId: String,
    val dayNumber: Int,
    val completed: Boolean = false,
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val attempts: Int = 0,
    val confidenceSelfRating: Int = 0,
    val quizScorePercent: Int = 0,
    val lastSubmissionText: String = "",
    val lastAudioPath: String? = null,
    val audioDurationSeconds: Int = 0,
    val practiceDurationSeconds: Int = 0,
    val reflectionNote: String = "",
    val whatWentWell: List<String> = emptyList(),
    val mistakesFound: List<String> = emptyList(),
    val correctedExamples: List<CorrectedExampleItem> = emptyList(),
    val coachSummary: String = "",
    val coachTips: List<String> = emptyList(),
    val recommendedNextStep: String = "",
    val completedQuizAnswers: Map<String, Int> = emptyMap(),
    val completedChecklistIndices: List<Int> = emptyList(),
    val lastUpdatedEpochMs: Long = 0L
)

@Serializable
data class ModuleAssessmentRecord(
    val assessmentId: String,
    val moduleId: String,
    val isCapstone: Boolean = false,
    val passed: Boolean = false,
    val bestCombinedScore: Int = 0,
    val lastCombinedScore: Int = 0,
    val quizScorePercent: Int = 0,
    val practicalScorePercent: Int = 0,
    val attempts: Int = 0,
    val lastResponseText: String = "",
    val summaryFeedback: String = "",
    val coachTips: List<String> = emptyList(),
    val completedAtEpochMs: Long = 0L
)

@Serializable
data class PathEnrollmentRecord(
    val programId: String,
    val programTitle: String,
    val active: Boolean = true,
    val paused: Boolean = false,
    val enrolledAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val currentDay: Int = 1,
    val completedDays: List<Int> = emptyList(),
    val dayRecords: Map<Int, DayLessonProgressRecord> = emptyMap(),
    val moduleAssessments: Map<String, ModuleAssessmentRecord> = emptyMap(),
    val completionCelebratedAtEpochMs: Long? = null
)

@Serializable
data class LearningPathsUserState(
    val schemaVersion: Int = 1,
    val userId: String = "guest",
    val enrollments: Map<String, PathEnrollmentRecord> = emptyMap(),
    val lastUpdatedEpochMs: Long = 0L
)
