package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.RecommendationEngine
import com.example.data.ai.RecommendedNextAction
import com.example.data.ai.VocabEvaluationResult
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.ExtractedTechnique
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.data.model.ProgramEnrollment
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UiState
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabSeed
import com.example.data.model.VocabularyEntry
import com.example.data.model.YouTubeSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainCoachVM"
private const val STOP_TIMEOUT_MS = 5000L

class MainCoachViewModel(
    private val repository: SpeakCraftRepository,
    private val geminiCoach: GeminiCoachService,
    private val currentUserId: String
) : ViewModel() {

    val todayDateKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayEpochDay: Long
        get() = System.currentTimeMillis() / 86_400_000L

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val speakingSessions: StateFlow<UiState<List<SpeakingSession>>> =
        repository.observeSpeakingSessions(currentUserId).toUiStateFlow("speaking sessions")

    val speechFeedback: StateFlow<UiState<List<SpeechFeedback>>> =
        repository.observeSpeechFeedback(currentUserId).toUiStateFlow("speech feedback")

    val vocabularyEntries: StateFlow<UiState<List<VocabularyEntry>>> =
        repository.observeVocabulary(currentUserId).toUiStateFlow("vocabulary")

    val dailyActivities: StateFlow<UiState<List<DailyActivity>>> =
        repository.observeDailyActivities(currentUserId).toUiStateFlow("daily activities")

    val learningProgress: StateFlow<UiState<List<UserLearningProgress>>> =
        repository.observeLearningProgress(currentUserId).toUiStateFlow("learning progress")

    val enrollments: StateFlow<UiState<List<ProgramEnrollment>>> =
        repository.observeEnrollments(currentUserId).toUiStateFlow("enrollments")

    val realWorldTasks: StateFlow<UiState<List<RealWorldTaskRecord>>> =
        repository.observeRealWorldTasks(currentUserId).toUiStateFlow("real-world tasks")

    val youtubeSources: StateFlow<UiState<List<YouTubeSource>>> =
        repository.observeYouTubeSources(currentUserId).toUiStateFlow("YouTube sources")

    val extractedTechniques: StateFlow<UiState<List<ExtractedTechnique>>> =
        repository.observeExtractedTechniques(currentUserId).toUiStateFlow("extracted techniques")

    val publishedSharedTechniques: StateFlow<List<ExtractedTechnique>> =
        repository.observePublishedSharedTechniques()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publishedSharedLessons: StateFlow<List<Lesson>> =
        repository.observePublishedSharedLessons()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val sharedChallenges: StateFlow<List<LearningChallenge>> =
        repository.observeSharedChallenges()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val isAdmin: StateFlow<Boolean> =
        repository.observeIsAdmin(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val recommendedNextAction: StateFlow<RecommendedNextAction> = combine(
        userProfile,
        speakingSessions,
        learningProgress,
        dailyActivities
    ) { pState, sState, lpState, dState ->
        val profile = (pState as? UiState.Success)?.data
        val sessions = (sState as? UiState.Success)?.data.orEmpty()
        val progress = (lpState as? UiState.Success)?.data.orEmpty()
        val dailyList = (dState as? UiState.Success)?.data.orEmpty()
        val today = dailyList.firstOrNull { it.dateKey == todayDateKey }
        RecommendationEngine.recommendNextAction(profile, sessions, progress, today)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        RecommendationEngine.recommendNextAction(null, emptyList(), emptyList(), null)
    )

    val dailyVocabularyBatch: List<VocabSeed> by lazy {
        val bank = CurriculumCatalog.vocabularyBank
        if (bank.size <= 3) bank
        else {
            val offset = ((todayEpochDay % bank.size).toInt()).coerceAtLeast(0)
            (0 until 3).map { i -> bank[(offset + i) % bank.size] }
        }
    }

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _vocabEvalState = MutableStateFlow<UiState<VocabEvaluationResult>?>(null)
    val vocabEvalState: StateFlow<UiState<VocabEvaluationResult>?> = _vocabEvalState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedAskVinhKnowledgeBaseIfEmpty()
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearVocabEval() {
        _vocabEvalState.value = null
    }

    fun completeOnboardingAssessment(
        displayName: String,
        level: String,
        primaryGoal: String,
        dailyGoalMinutes: Long,
        fluency: Long,
        pronunciation: Long,
        vocabulary: Long,
        grammar: Long,
        articulation: Long,
        listening: Long,
        confidenceSelf: Long
    ) {
        viewModelScope.launch {
            val res = repository.createOrUpdateProfile(
                displayName = displayName,
                level = level,
                primaryGoal = primaryGoal,
                dailyGoalMinutes = dailyGoalMinutes,
                fluency = fluency,
                pronunciation = pronunciation,
                vocabulary = vocabulary,
                grammar = grammar,
                articulation = articulation,
                listening = listening,
                confidenceSelf = confidenceSelf
            )
            if (res.isSuccess) {
                repository.enrollInProgram("30_day_challenge", "30-Day Communication Challenge")
                repository.seedAskVinhKnowledgeBaseIfEmpty()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun toggleDailyStep(
        stepId: String,
        currentActivity: DailyActivity?,
        reflectionText: String,
        confidenceRating: Long
    ) {
        viewModelScope.launch {
            val existing = currentActivity?.completedStepIds.orEmpty()
            val updated = if (existing.contains(stepId)) existing - stepId else existing + stepId
            val mins = (updated.size * 3L).coerceAtLeast(3L)
            val res = repository.saveDailyActivity(
                dateKey = todayDateKey,
                completedSteps = updated,
                reflection = reflectionText,
                confidenceRating = confidenceRating,
                minutesSpent = mins
            )
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed updating daily step"
            }
        }
    }

    fun saveDailyReflection(
        currentActivity: DailyActivity?,
        reflectionText: String,
        confidenceRating: Long
    ) {
        viewModelScope.launch {
            val existing = (currentActivity?.completedStepIds.orEmpty() + "reflection").distinct()
            val res = repository.saveDailyActivity(
                dateKey = todayDateKey,
                completedSteps = existing,
                reflection = reflectionText,
                confidenceRating = confidenceRating,
                minutesSpent = (existing.size * 3L).coerceAtLeast(5L)
            )
            _actionMessage.value = if (res.isSuccess) {
                "Daily reflection saved!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save reflection"
            }
        }
    }

    fun toggleLessonCompleted(
        lessonId: String,
        moduleSlug: String,
        completed: Boolean,
        notes: String = "",
        score: Long? = null
    ) {
        viewModelScope.launch {
            val res = repository.toggleLessonProgress(lessonId, moduleSlug, completed, notes, score)
            _actionMessage.value = if (res.isSuccess) {
                if (completed) "Lesson marked completed!" else "Lesson progress updated"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not update lesson progress"
            }
        }
    }

    fun enrollProgram(programId: String, title: String) {
        viewModelScope.launch {
            val res = repository.enrollInProgram(programId, title)
            _actionMessage.value = if (res.isSuccess) "Enrolled in $title" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun toggleProgramDay(programId: String, dayNumber: Long, totalDays: Int) {
        viewModelScope.launch {
            val res = repository.toggleProgramDay(programId, dayNumber, totalDays)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not update program day"
            }
        }
    }

    fun reviewVocabularyCard(seed: VocabSeed, existingEntry: VocabularyEntry?, qualityGood: Boolean) {
        viewModelScope.launch {
            val prevReps = existingEntry?.repetitions ?: 0L
            val prevMastery = existingEntry?.masteryLevel ?: 0L
            val newReps = if (qualityGood) prevReps + 1L else 0L
            val newMastery = if (qualityGood) (prevMastery + 1L).coerceAtMost(5L) else (prevMastery - 1L).coerceAtLeast(0L)
            val intervalDays = when {
                !qualityGood -> 1L
                newReps <= 1L -> 1L
                newReps == 2L -> 3L
                else -> (newReps * 3L).coerceAtMost(21L)
            }
            val res = repository.upsertVocabularyEntry(
                wordId = seed.id,
                word = seed.word,
                phonetic = seed.phonetic,
                meaning = seed.meaning,
                level = seed.level,
                exampleSentence = seed.exampleSentence,
                collocations = seed.collocations,
                personalNote = existingEntry?.personalNote.orEmpty(),
                masteryLevel = newMastery,
                repetitions = newReps,
                nextReviewEpochDay = todayEpochDay + intervalDays,
                usageAssessed = existingEntry?.usageAssessed ?: false
            )
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save review"
            }
        }
    }

    fun saveCustomVocabularyWord(
        word: String,
        meaning: String,
        level: String,
        example: String,
        note: String
    ) {
        if (word.isBlank() || meaning.isBlank()) {
            _actionMessage.value = "Word and meaning are required."
            return
        }
        viewModelScope.launch {
            val safeId = "custom_${word.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
            val res = repository.upsertVocabularyEntry(
                wordId = safeId,
                word = word.trim(),
                phonetic = "",
                meaning = meaning.trim(),
                level = level,
                exampleSentence = example.trim(),
                collocations = emptyList(),
                personalNote = note.trim(),
                masteryLevel = 1L,
                repetitions = 1L,
                nextReviewEpochDay = todayEpochDay + 1L,
                usageAssessed = false
            )
            _actionMessage.value = if (res.isSuccess) "Saved '$word' to notebook" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun evaluateVocabUsage(seed: VocabSeed, existingEntry: VocabularyEntry?, userSentence: String) {
        if (userSentence.isBlank()) {
            _actionMessage.value = "Enter a sentence using '${seed.word}' first."
            return
        }
        viewModelScope.launch {
            _vocabEvalState.value = UiState.Loading
            val eval = geminiCoach.evaluateVocabularyUsage(seed.word, seed.meaning, userSentence)
            eval.onSuccess { result ->
                _vocabEvalState.value = UiState.Success(result)
                if (result.usedCorrectly) {
                    repository.upsertVocabularyEntry(
                        wordId = seed.id,
                        word = seed.word,
                        phonetic = seed.phonetic,
                        meaning = seed.meaning,
                        level = seed.level,
                        exampleSentence = seed.exampleSentence,
                        collocations = seed.collocations,
                        personalNote = existingEntry?.personalNote.orEmpty(),
                        masteryLevel = ((existingEntry?.masteryLevel ?: 0L) + 1L).coerceAtMost(5L),
                        repetitions = (existingEntry?.repetitions ?: 0L) + 1L,
                        nextReviewEpochDay = todayEpochDay + 4L,
                        usageAssessed = true
                    )
                }
            }.onFailure { err ->
                _vocabEvalState.value = UiState.Error(err.localizedMessage ?: "Evaluation failed")
            }
        }
    }

    fun toggleRealWorldTask(taskId: String, title: String, completed: Boolean, reflection: String) {
        viewModelScope.launch {
            val res = repository.toggleRealWorldTask(taskId, title, completed, reflection)
            _actionMessage.value = if (res.isSuccess) {
                if (completed) "Task marked completed!" else "Task updated"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed saving task"
            }
        }
    }

    fun deleteSpeakingSession(session: SpeakingSession) {
        if (session.id.isBlank()) return
        viewModelScope.launch {
            val res = repository.deleteSpeakingSession(session.id, session.feedbackId, session.localMediaPath)
            _actionMessage.value = if (res.isSuccess) {
                "Recording deleted."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed deleting recording."
            }
        }
    }

    fun clearSpeakingHistory() {
        viewModelScope.launch {
            val res = repository.clearSpeakingHistory()
            _actionMessage.value = if (res.isSuccess) "Speaking recordings history cleared." else res.exceptionOrNull()?.localizedMessage
        }
    }
}
