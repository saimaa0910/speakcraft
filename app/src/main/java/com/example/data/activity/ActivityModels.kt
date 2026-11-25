package com.example.data.activity

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ExerciseTemplateType(
    val displayName: String,
    val interactionFamily: InteractionFamily
) {
    SPEAKING_EXERCISE("Speaking Exercise", InteractionFamily.AUDIO_SPEAKING),
    AUDIO_RECORDING("Audio Recording Exercise", InteractionFamily.AUDIO_SPEAKING),
    VIDEO_RECORDING("Video Recording Exercise", InteractionFamily.VIDEO_PRESENCE),
    LISTENING_COMPREHENSION("Listening Comprehension", InteractionFamily.LISTENING_AUDIO),
    PRONUNCIATION_DRILL("Pronunciation Drill", InteractionFamily.AUDIO_SPEAKING),
    VOCABULARY_EXERCISE("Vocabulary Exercise", InteractionFamily.LANGUAGE_BUILDER),
    GRAMMAR_EXERCISE("Grammar Exercise", InteractionFamily.LANGUAGE_BUILDER),
    READING_ALOUD("Reading Aloud", InteractionFamily.AUDIO_SPEAKING),
    IMPROMPTU_SPEAKING("Impromptu Speaking", InteractionFamily.AUDIO_SPEAKING),
    CONVERSATION_SIMULATION("Conversation Simulation", InteractionFamily.INTERACTIVE_DIALOGUE),
    ROLEPLAY("Roleplay", InteractionFamily.INTERACTIVE_DIALOGUE),
    STORYTELLING("Storytelling", InteractionFamily.AUDIO_SPEAKING),
    QUESTION_AND_ANSWER("Question & Answer", InteractionFamily.AUDIO_SPEAKING),
    SENTENCE_CONSTRUCTION("Sentence Construction", InteractionFamily.LANGUAGE_BUILDER),
    WORD_ASSOCIATION("Word Association", InteractionFamily.LANGUAGE_BUILDER),
    VOICE_MODULATION("Voice Modulation", InteractionFamily.AUDIO_SPEAKING),
    BREATHING_VOCAL_WARMUP("Breathing & Vocal Warm-Up", InteractionFamily.GUIDED_WARMUP),
    TONGUE_TWISTERS("Tongue Twisters", InteractionFamily.AUDIO_SPEAKING),
    BODY_LANGUAGE_SELF_REVIEW("Body-Language Self-Review", InteractionFamily.VIDEO_PRESENCE),
    PRESENTATION_PRACTICE("Presentation Practice", InteractionFamily.VIDEO_PRESENCE),
    INTERVIEW_SIMULATION("Interview Simulation", InteractionFamily.INTERACTIVE_DIALOGUE),
    ACTIVE_LISTENING_GAME("Active Listening Game", InteractionFamily.LISTENING_AUDIO),
    REAL_WORLD_TASK("Real-World Communication Task", InteractionFamily.REAL_WORLD_REFLECTION),
    REFLECTION_SELF_ASSESSMENT("Reflection & Self-Assessment", InteractionFamily.REAL_WORLD_REFLECTION),
    AI_EVALUATED_CHALLENGE("AI-Evaluated Challenge", InteractionFamily.AUDIO_SPEAKING)
}

enum class InteractionFamily {
    AUDIO_SPEAKING,
    VIDEO_PRESENCE,
    LISTENING_AUDIO,
    INTERACTIVE_DIALOGUE,
    LANGUAGE_BUILDER,
    GUIDED_WARMUP,
    REAL_WORLD_REFLECTION
}

enum class ActivityDifficulty(val label: String, val rank: Int) {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 2),
    ADVANCED("Advanced", 3)
}

enum class ActivitySkillCategory(val label: String, val dailyPillar: DailyPracticePillar) {
    ENGLISH_FLUENCY("English Fluency", DailyPracticePillar.SPEAKING),
    PRONUNCIATION("Pronunciation", DailyPracticePillar.PRONUNCIATION),
    VOICE_MASTERY("Voice Mastery", DailyPracticePillar.VOICE),
    SPEAKING_CLARITY("Speaking Clarity", DailyPracticePillar.SPEAKING),
    VOCABULARY("Vocabulary", DailyPracticePillar.VOCABULARY),
    GRAMMAR("Grammar & Structure", DailyPracticePillar.VOCABULARY),
    CONVERSATION("Conversation", DailyPracticePillar.CONVERSATION),
    ACTIVE_LISTENING("Active Listening", DailyPracticePillar.LISTENING),
    STORYTELLING("Storytelling", DailyPracticePillar.SPEAKING),
    BODY_LANGUAGE("Body Language", DailyPracticePillar.VOICE),
    PUBLIC_SPEAKING("Public Speaking", DailyPracticePillar.SPEAKING),
    PROFESSIONAL("Professional Communication", DailyPracticePillar.CONVERSATION),
    REFLECTION("Reflection", DailyPracticePillar.REFLECTION)
}

enum class DailyPracticePillar(val label: String) {
    PRONUNCIATION("Pronunciation"),
    VOICE("Voice"),
    VOCABULARY("Vocabulary"),
    SPEAKING("Speaking"),
    LISTENING("Listening"),
    CONVERSATION("Conversation"),
    REFLECTION("Reflection")
}

enum class EvaluationStrategyType(val evaluatorName: String) {
    SPEAKING("SpeakingEvaluator"),
    PRONUNCIATION("PronunciationEvaluator"),
    GRAMMAR("GrammarEvaluator"),
    VOCABULARY("VocabularyEvaluator"),
    LISTENING("ListeningEvaluator"),
    CONVERSATION("ConversationEvaluator"),
    STORYTELLING("StorytellingEvaluator"),
    PRESENTATION("PresentationEvaluator"),
    REFLECTION("ReflectionEvaluator")
}

enum class ActivityPermission(val manifestPermission: String, val label: String) {
    MICROPHONE(android.Manifest.permission.RECORD_AUDIO, "Microphone"),
    CAMERA(android.Manifest.permission.CAMERA, "Camera")
}

data class ActivityModuleDefinition(
    val moduleId: String,
    val order: Int,
    val title: String,
    val subtitle: String,
    val skillCategory: ActivitySkillCategory,
    val targetActivityCount: Int
)

data class InteractionConfiguration(
    val promptText: String,
    val supportsAudioRecording: Boolean = false,
    val supportsVideoRecording: Boolean = false,
    val supportsTextInput: Boolean = true,
    val listeningScript: String = "",
    val ttsRate: Float = 1.0f,
    val targetWordOrPhrase: String = "",
    val dialogueOpeningLine: String = "",
    val dialoguePartnerRole: String = "",
    val checklistItems: List<String> = emptyList(),
    val warmupCues: List<String> = emptyList(),
    val targetWordCountMin: Int = 12,
    val allowConfidenceSelfRating: Boolean = true
)

data class EvaluationConfiguration(
    val strategy: EvaluationStrategyType,
    val aiEnabled: Boolean = true,
    val focusCriteria: List<String>,
    val honestyCaveat: String = ""
)

data class ScoringRubricDimension(
    val key: String,
    val label: String,
    val weightPercent: Int,
    val criteriaDescription: String
)

data class CompletionCriteria(
    val minDurationSeconds: Int = 10,
    val minWordsOrTurns: Int = 8,
    val requiresRecording: Boolean = false,
    val requiresChecklistCompletion: Boolean = false,
    val passingScoreThreshold: Int = 60
)

data class FeedbackConfiguration(
    val showWhatWentWell: Boolean = true,
    val showSpecificMistakes: Boolean = true,
    val showCorrectedExamples: Boolean = true,
    val showExplanation: Boolean = true,
    val showImprovementSuggestions: Boolean = true,
    val showNextPracticeTask: Boolean = true,
    val coachTone: String = "Encouraging, evidence-based, and concrete"
)

data class ActivityDefinition(
    val activityId: String,
    val title: String,
    val description: String,
    val moduleId: String,
    val skillCategory: ActivitySkillCategory,
    val exerciseType: ExerciseTemplateType,
    val difficulty: ActivityDifficulty,
    val duration: Int, // Duration in minutes
    val learningObjective: String,
    val instructions: List<String>,
    val examples: List<String>,
    val requiredPermissions: List<ActivityPermission>,
    val interactionConfiguration: InteractionConfiguration,
    val evaluationConfiguration: EvaluationConfiguration,
    val scoringRubric: List<ScoringRubricDimension>,
    val completionCriteria: CompletionCriteria,
    val feedbackConfiguration: FeedbackConfiguration,
    val nextActivityIds: List<String>,
    val sourceAttribution: String,
    val contentVersion: Int = 1
)

data class ActivityValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
)

data class DialogueTurn(
    val role: String, // "user" or "model"
    val text: String
)

data class ActivityUserResponse(
    val activityId: String,
    val textResponse: String = "",
    val transcript: String = "",
    val recordedAudioPath: String = "",
    val recordedVideoPath: String = "",
    val mediaBytes: ByteArray? = null,
    val mediaMimeType: String = "",
    val durationSeconds: Int = 0,
    val completedChecklistItems: List<String> = emptyList(),
    val dialogueTurns: List<DialogueTurn> = emptyList(),
    val confidenceSelfRating: Int? = null // 1..100 strictly separate from objective score
) {
    val combinedTextForAnalysis: String
        get() = listOf(
            transcript.trim(),
            textResponse.trim(),
            dialogueTurns.filter { it.role == "user" }.joinToString(" ") { it.text.trim() }
        ).filter { it.isNotBlank() }.joinToString("\n")
}

data class EvaluationDimensionScore(
    val key: String,
    val label: String,
    val score: Int, // 0..100
    val note: String
)

data class ActivityEvaluationResult(
    val activityId: String,
    val evaluatorName: String,
    val overallObjectiveScore: Int, // 0..100 objective rubric score (never mixed with confidence)
    val passedCriteria: Boolean,
    val dimensionScores: List<EvaluationDimensionScore>,
    val whatWentWell: List<String>,
    val specificMistakes: List<String>,
    val correctedExamples: List<String>,
    val explanation: String,
    val improvementSuggestions: List<String>,
    val nextPracticeTask: String,
    val honestyNotice: String = "",
    val aiEvaluated: Boolean = true,
    val confidenceSelfRating: Int? = null, // Stored separately, never blended into overallObjectiveScore
    val wordsPerMinute: Int = 0,
    val fillerCount: Int = 0,
    val wordCount: Int = 0,
    val recommendedNextActivityIds: List<String> = emptyList()
)

data class LearningPathMilestone(
    val milestoneId: String,
    val title: String,
    val requiredCompletedCount: Int,
    val rewardSummary: String
)

data class LearningPathDefinition(
    val pathId: String,
    val title: String,
    val subtitle: String,
    val targetAudience: String,
    val difficulty: ActivityDifficulty,
    val prerequisitePathIds: List<String>,
    val orderedActivityIds: List<String>,
    val milestones: List<LearningPathMilestone>,
    val completionCriteriaSummary: String
)

// ==================== 4-PILLAR LEARNING PATH PROGRESS MODELS ====================

data class PathActivityItemProgress(
    val activityId: String,
    val title: String,
    val moduleId: String,
    val skillCategory: ActivitySkillCategory,
    val exerciseType: ExerciseTemplateType,
    val difficulty: ActivityDifficulty,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val attemptsCount: Int,
    val bestObjectiveScore: Int,
    val latestObjectiveScore: Int,
    val latestConfidenceRating: Int,
    val totalPracticeSeconds: Long,
    val lastPracticedDateKey: String
)

data class ActivityPathProgress(
    val totalActivities: Int,
    val completedActivities: Int,
    val inProgressActivities: Int,
    val percentComplete: Int,
    val averageScore: Int,
    val bestScore: Int,
    val totalAttempts: Int,
    val totalPracticeMinutes: Int,
    val activities: List<PathActivityItemProgress>
)

data class ExerciseTypeProgress(
    val exerciseType: ExerciseTemplateType,
    val typeName: String,
    val family: InteractionFamily,
    val totalCount: Int,
    val completedCount: Int,
    val percentComplete: Int,
    val averageScore: Int,
    val associatedActivityIds: List<String>
)

data class ExercisePathProgress(
    val totalExercises: Int,
    val completedExercises: Int,
    val percentComplete: Int,
    val distinctExerciseTypesCount: Int,
    val completedExerciseTypesCount: Int,
    val exerciseTypeBreakdown: List<ExerciseTypeProgress>
)

data class PathTechniqueItemProgress(
    val techniqueId: String,
    val title: String,
    val summary: String,
    val coreRule: String,
    val category: String,
    val associatedActivityIds: List<String>,
    val totalActivitiesCount: Int,
    val completedActivitiesCount: Int,
    val percentComplete: Int,
    val averageScore: Int,
    val isPracticed: Boolean,
    val isMastered: Boolean,
    /** Coach Practice attempts of this technique (valid + inconclusive), as recorded on the device. */
    val practiceAttempts: Int = 0,
    val validPracticeAttempts: Int = 0,
    val inconclusivePracticeAttempts: Int = 0,
    /** Best engine score of a scored practice template; null when no scored attempt exists. */
    val bestPracticeScore: Int? = null,
    val lastPracticedDateKey: String = ""
)

data class TechniquePathProgress(
    val totalTechniques: Int,
    val practicedTechniques: Int,
    val masteredTechniques: Int,
    val percentComplete: Int,
    val techniques: List<PathTechniqueItemProgress>
)

enum class PathTaskCategory(val label: String) {
    ACTIVITY_TASK("Activity Drill"),
    REAL_WORLD_APPLICATION("Real-World Mission"),
    MILESTONE_CHECKPOINT("Milestone Checkpoint"),
    PRACTICE_DRILL("Practice Task")
}

data class PathTaskItemProgress(
    val taskId: String,
    val title: String,
    val description: String,
    val category: PathTaskCategory,
    val isCompleted: Boolean,
    val completedAtMs: Long = 0L,
    val sourceActivityId: String? = null,
    val requiredThreshold: Int = 1,
    val currentProgress: Int = 0
)

data class TaskPathProgress(
    val totalTasks: Int,
    val completedTasks: Int,
    val percentComplete: Int,
    val tasksByCategory: Map<PathTaskCategory, Int>,
    val tasks: List<PathTaskItemProgress>
)

enum class LearningPathStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    LOCKED("Prerequisites Needed")
}

data class LearningPathProgressSummary(
    val path: LearningPathDefinition,
    val completedCount: Int,
    val totalCount: Int,
    val percentComplete: Int,
    val prerequisitesMet: Boolean,
    val nextRecommendedActivityId: String?,
    val unlockedMilestones: List<LearningPathMilestone>,
    val activityProgress: ActivityPathProgress = ActivityPathProgress(
        totalActivities = totalCount,
        completedActivities = completedCount,
        inProgressActivities = 0,
        percentComplete = percentComplete,
        averageScore = 0,
        bestScore = 0,
        totalAttempts = 0,
        totalPracticeMinutes = 0,
        activities = emptyList()
    ),
    val exerciseProgress: ExercisePathProgress = ExercisePathProgress(
        totalExercises = totalCount,
        completedExercises = completedCount,
        percentComplete = percentComplete,
        distinctExerciseTypesCount = 0,
        completedExerciseTypesCount = 0,
        exerciseTypeBreakdown = emptyList()
    ),
    val techniqueProgress: TechniquePathProgress = TechniquePathProgress(
        totalTechniques = 0,
        practicedTechniques = 0,
        masteredTechniques = 0,
        percentComplete = 0,
        techniques = emptyList()
    ),
    val taskProgress: TaskPathProgress = TaskPathProgress(
        totalTasks = 0,
        completedTasks = 0,
        percentComplete = 0,
        tasksByCategory = emptyMap(),
        tasks = emptyList()
    ),
    val overallWeightedProgressPercent: Int = percentComplete,
    val status: LearningPathStatus = if (percentComplete >= 100) LearningPathStatus.COMPLETED else if (completedCount > 0) LearningPathStatus.IN_PROGRESS else if (!prerequisitesMet) LearningPathStatus.LOCKED else LearningPathStatus.NOT_STARTED,
    val nextActionRecommendation: String = ""
)

data class DailyPracticeBlock(
    val activityId: String,
    val title: String,
    val pillar: DailyPracticePillar,
    val durationMinutes: Int,
    val difficulty: ActivityDifficulty,
    val completed: Boolean = false,
    val pinned: Boolean = false,
    val reason: String = ""
)

data class DailyPracticePlan(
    val dateKey: String,
    val targetMinutes: Int,
    val blocks: List<DailyPracticeBlock>,
    val totalPlannedMinutes: Int,
    val personalizationSummary: String
)

data class ModuleCompletionStat(
    val module: ActivityModuleDefinition,
    val completedCount: Int,
    val totalCount: Int,
    val averageObjectiveScore: Int
)

data class SkillTrendComparison(
    val skillCategory: ActivitySkillCategory,
    val earliestScore: Int,
    val latestScore: Int,
    val delta: Int,
    val attemptCount: Int
)

data class ActivityAnalyticsSnapshot(
    val totalActivitiesCompleted: Int,
    val totalAttempts: Int,
    val totalPracticeMinutes: Int,
    val activeDaysLast7: Int,
    val currentConsistencyStreak: Int,
    val weeklyAttemptsCount: Int,
    val averageObjectiveScore: Int,
    val averageConfidenceSelfRating: Int?, // Strictly separated from objective scores
    val moduleStats: List<ModuleCompletionStat>,
    val skillTrends: List<SkillTrendComparison>,
    val improvementAreas: List<ActivitySkillCategory>,
    val achievedMilestones: List<String>,
    val recordingsCount: Int
)

// ==================== FIRESTORE MODELS (NO `is`-PREFIXED BOOLEANS) ====================

@Keep
@IgnoreExtraProperties
data class UserActivityProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityId: String = "",
    val moduleId: String = "",
    val skillCategory: String = "",
    val completed: Boolean = false,
    val attemptsCount: Long = 0L,
    val bestObjectiveScore: Long = 0L,
    val latestObjectiveScore: Long = 0L,
    val latestConfidenceSelfRating: Long = 0L,
    val totalPracticeSeconds: Long = 0L,
    val lastPracticedDateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityAttemptRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityId: String = "",
    val moduleId: String = "",
    val skillCategory: String = "",
    val exerciseType: String = "",
    val evaluatorName: String = "",
    val dateKey: String = "",
    val durationSeconds: Long = 0L,
    val objectiveScore: Long = 0L,
    val confidenceSelfRating: Long = 0L,
    val passed: Boolean = false,
    val aiEvaluated: Boolean = false,
    val responseSummary: String = "",
    val whatWentWell: List<String> = emptyList(),
    val specificMistakes: List<String> = emptyList(),
    val improvementSuggestions: List<String> = emptyList(),
    val nextPracticeTask: String = "",
    val recordedMediaPath: String = "",
    val wordsPerMinute: Long = 0L,
    val fillerCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedActivityRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityId: String = "",
    val moduleId: String = "",
    val title: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyPracticePlanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val targetMinutes: Long = 20L,
    val plannedActivityIds: List<String> = emptyList(),
    val completedActivityIds: List<String> = emptyList(),
    val pinnedActivityIds: List<String> = emptyList(),
    val personalizationSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
