package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Speaker",
    val communicationLevel: String = "Intermediate",
    val primaryGoal: String = "Speak with vocal authority and clarity",
    val onboardingCompleted: Boolean = false,
    val dailyGoalMinutes: Long = 15L,
    val streakDays: Long = 0L,
    val lastPracticeDate: String = "",
    val totalPracticeMinutes: Long = 0L,
    val wordsLearnedCount: Long = 0L,
    val recordingsCount: Long = 0L,
    val completedExercisesCount: Long = 0L,
    val fluencyScore: Long = 62L,
    val pronunciationScore: Long = 65L,
    val vocabularyScore: Long = 60L,
    val grammarScore: Long = 68L,
    val articulationScore: Long = 58L,
    val listeningScore: Long = 66L,
    val confidenceSelfRating: Long = 55L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProgramEnrollment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val programId: String = "",
    val programTitle: String = "",
    val currentDay: Long = 1L,
    val completedDays: List<Long> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyActivity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val completedStepIds: List<String> = emptyList(),
    val reflectionText: String = "",
    val confidenceRating: Long = 6L,
    val minutesSpent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserLearningProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lessonId: String = "",
    val moduleSlug: String = "",
    val completed: Boolean = false,
    val attemptsCount: Long = 0L,
    val lastScore: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VocabularyEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val word: String = "",
    val phonetic: String = "",
    val meaning: String = "",
    val level: String = "Intermediate",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val exampleSentence: String = "",
    val conversationExample: String = "",
    val collocations: List<String> = emptyList(),
    val personalNote: String = "",
    val masteryLevel: Long = 0L,
    val repetitions: Long = 0L,
    val nextReviewEpochDay: Long = 0L,
    val usageAssessed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SpeakingSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moduleType: String = "SPEAKING_LAB",
    val promptTitle: String = "",
    val durationSeconds: Long = 0L,
    val localMediaPath: String = "",
    val mediaType: String = "audio/mp4",
    val feedbackId: String = "",
    val transcript: String = "",
    val overallAssessment: String = "",
    val strengths: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val pronunciationNotes: List<String> = emptyList(),
    val betterAlternatives: List<String> = emptyList(),
    val fillerWordsDetected: List<String> = emptyList(),
    val repeatedExpressions: List<String> = emptyList(),
    val improvementSuggestions: List<String> = emptyList(),
    val recommendedExercises: List<String> = emptyList(),
    val pronunciationAssessable: Boolean = true,
    val articulationClarity: String = "",
    val thoughtOrganization: String = "",
    val nextRecommendedActivity: String = "",
    val confidenceLevel: String = "Medium",
    val audioQualityNote: String = "",
    val fluencyScore: Long = 0L,
    val pronunciationScore: Long = 0L,
    val vocabularyScore: Long = 0L,
    val grammarScore: Long = 0L,
    val articulationScore: Long = 0L,
    val wordsPerMinute: Long = 0L,
    val sourceVideoUrl: String = "",
    val evaluationStatus: String = "COMPLETED",
    val isPartialEvaluation: Boolean = false,
    val evaluatedDimensionKeys: List<String> = emptyList(),
    val unavailableDimensionKeys: List<String> = emptyList(),
    val evidenceSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SpeechFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val transcript: String = "",
    val strengths: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val recordingExamples: List<String> = emptyList(),
    val betterAlternatives: List<String> = emptyList(),
    val fillerWords: List<String> = emptyList(),
    val repeatedExpressions: List<String> = emptyList(),
    val specificExercises: List<String> = emptyList(),
    val pronunciationAssessable: Boolean = false,
    val pronunciationNote: String = "",
    val articulationClarity: String = "",
    val thoughtOrganization: String = "",
    val nextRecommendedActivity: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConversationSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scenario: String = "",
    val difficultyLevel: String = "Intermediate",
    val turnCount: Long = 0L,
    val summaryFeedback: String = "",
    val transcriptPreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningModule(
    @DocumentId val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val orderIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val moduleSlug: String = "",
    val techniqueId: String = "",
    val title: String = "",
    val objective: String = "",
    val sourceVideoUrl: String = "",
    val timestampLabel: String = "",
    val explanation: String = "",
    val instructions: List<String> = emptyList(),
    val example: String = "",
    val practicePrompt: String = "",
    val nextLessonId: String = "",
    val isCreatorDerived: Boolean = true,
    val status: String = "PUBLISHED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunicationExercise(
    @DocumentId val id: String = "",
    val moduleSlug: String = "",
    val lessonId: String = "",
    val title: String = "",
    val instructions: String = "",
    val difficulty: String = "Intermediate",
    val durationSeconds: Long = 60L,
    val isAiSupplement: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningChallenge(
    @DocumentId val id: String = "",
    val title: String = "",
    val challengeType: String = "DAILY",
    val durationSeconds: Long = 60L,
    val prompt: String = "",
    val frameworkHint: String = "",
    val sourceReference: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RealWorldTaskRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskTitle: String = "",
    val sourceVideoUrl: String = "",
    val completed: Boolean = false,
    val reflection: String = "",
    val linkedSessionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "ADMIN",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
