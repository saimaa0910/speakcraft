package com.example.ui.viewmodel

import com.example.core.logging.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.language.LocalVocabStageEvaluation
import com.example.data.ai.RecommendationEngine
import com.example.data.ai.RecommendedNextAction
import com.example.data.ai.VocabEvaluationResult
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.learning.DailyAdaptiveCurriculumEngine
import com.example.data.learning.DailyAdaptiveCurriculumPackage
import com.example.data.learning.LearningLevel
import com.example.data.learning.LearningProgressManager
import com.example.data.learning.PersistedLearningSnapshot
import com.example.data.learning.VocabularyMasteryEngine
import com.example.data.learning.WeeklyAssessmentEligibility
import com.example.data.learning.WeeklyVocabularyAssessmentRecord
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
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
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainCoachVM"
private const val STOP_TIMEOUT_MS = 5000L

data class DailyVocabBuilderUiState(
    val userLevel: LearningLevel = LearningLevel.INTERMEDIATE,
    val dailyWords: List<VocabSeed> = emptyList(),
    val completedWordIds: Set<String> = emptySet(),
    val wordScores: Map<String, Int> = emptyMap(),
    val lastTranscripts: Map<String, String> = emptyMap(),
    val learnedWordIds: Set<String> = emptySet(),
    val weeklyEligibility: WeeklyAssessmentEligibility = WeeklyAssessmentEligibility(),
    val learnedWordsForAssessment: List<VocabSeed> = emptyList()
)

class MainCoachViewModel(
    private val repository: SpeakCraftRepository,
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val currentUserId: String,
    private val hybridCoordinator: HybridAiCoachCoordinator? = null,
    /** Local learning state: daily set, daily record and learning-path progress (survives restarts). */
    private val learningProgressManager: LearningProgressManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val effectiveCoordinator: HybridAiCoachCoordinator by lazy {
        hybridCoordinator ?: HybridAiCoachCoordinator(localOrchestrator = localOrchestrator)
    }

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
                AppLogger.w(TAG, "Error observing $label", root)
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

    val publishedSharedLessons: StateFlow<List<Lesson>> =
        repository.observePublishedSharedLessons()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val sharedChallenges: StateFlow<List<LearningChallenge>> =
        repository.observeSharedChallenges()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminRole: StateFlow<com.example.data.learning.AdminRole> =
        repository.observeAdminRole(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), com.example.data.learning.AdminRole.NONE)

    val isAdmin: StateFlow<Boolean> =
        adminRole.map { it != com.example.data.learning.AdminRole.NONE }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val activeAnnouncements: StateFlow<List<com.example.data.learning.AdminAnnouncement>> =
        repository.observeActiveAnnouncements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

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
    }.flowOn(computeDispatcher).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        RecommendationEngine.recommendNextAction(null, emptyList(), emptyList(), null)
    )

    val currentLearningLevel: StateFlow<LearningLevel> = userProfile.map { uiState ->
        val profile = (uiState as? UiState.Success)?.data
        LearningLevel.fromLabel(profile?.communicationLevel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LearningLevel.INTERMEDIATE)

    /**
     * Today's current daily set. Comes from the local learning state (completion-driven: the next set appears
     * as soon as the current one is finished); the date-only package is the fallback before that state loads.
     */
    val dailyCurriculumPackage: StateFlow<DailyAdaptiveCurriculumPackage> = combine(
        currentLearningLevel,
        userProfile,
        dailyActivities,
        learningProgressManager?.dailyPackage ?: flowOf(null)
    ) { level, pState, dState, currentSet ->
        val profile = (pState as? UiState.Success)?.data
        val dailyList = (dState as? UiState.Success)?.data.orEmpty()
        val today = dailyList.firstOrNull { it.dateKey == todayDateKey }
        currentSet?.takeIf { it.dateKey == todayDateKey } ?: DailyAdaptiveCurriculumEngine.generateDailyPackage(
            dateKey = todayDateKey,
            level = level,
            profile = profile,
            completedTodayIds = today?.completedStepIds.orEmpty().toSet()
        )
    }.flowOn(computeDispatcher).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DailyAdaptiveCurriculumEngine.generateDailyPackage(todayDateKey, LearningLevel.INTERMEDIATE)
    )

    val dailyVocabularyBatch: List<VocabSeed>
        get() {
            val level = currentLearningLevel.value
            val snap = learningProgressManager?.state?.value
            if (snap != null) {
                val wordsById = CurriculumCatalog.vocabularyBank.associateBy { it.id }
                val ids = snap.dailyVocabulary?.takeIf { it.dateKey == todayDateKey && it.level == level }?.wordIds.orEmpty()
                val resolved = ids.mapNotNull { wordsById[it] }
                if (resolved.size == VocabularyMasteryEngine.DAILY_WORD_COUNT) return resolved
            }
            return VocabularyMasteryEngine.ensureDailyVocabularyWords(
                bank = CurriculumCatalog.vocabularyBank,
                level = level,
                todayDateKey = todayDateKey
            ).first.wordIds.mapNotNull { id -> CurriculumCatalog.vocabularyBank.firstOrNull { it.id == id } }
        }

    val vocabBuilderState: StateFlow<DailyVocabBuilderUiState> = combine(
        currentLearningLevel,
        vocabularyEntries,
        learningProgressManager?.state ?: flowOf(PersistedLearningSnapshot())
    ) { level, vState, snap ->
        val entries = (vState as? UiState.Success)?.data.orEmpty()
        val wordsById = CurriculumCatalog.vocabularyBank.associateBy { it.id }
        val completedAllTime = snap.learnedVocabularyLog.map { it.wordId }.toSet() +
            snap.wordProgress.filterValues { it.isCompleted || it.stage2Passed }.keys
        val (assignment, _) = VocabularyMasteryEngine.ensureDailyVocabularyWords(
            bank = CurriculumCatalog.vocabularyBank,
            level = level,
            todayDateKey = todayDateKey,
            shownWordIds = snap.shownVocabularyWordIds,
            completedAllTimeIds = completedAllTime,
            existingAssignment = snap.dailyVocabulary,
            previousDaysCount = snap.vocabularyActiveDateKeys.size
        )
        val dailyWords = assignment.wordIds.mapNotNull { wordsById[it] }
        val eligibility = VocabularyMasteryEngine.computeWeeklyEligibility(
            learnedLog = snap.learnedVocabularyLog,
            wordProgress = snap.wordProgress,
            weeklyAssessments = snap.weeklyAssessments,
            firstVocabEpochDay = snap.firstVocabularyEpochDay,
            vocabularyActiveDateKeys = snap.vocabularyActiveDateKeys,
            todayEpochDay = todayEpochDay,
            fallbackEntries = entries
        )
        val learnedSeeds = VocabularyMasteryEngine.resolveLearnedSeeds(
            learnedWordIds = eligibility.learnedWordIdsInCycle,
            bank = CurriculumCatalog.vocabularyBank,
            entries = entries
        )
        val entryLearnedIds = entries.filter { it.usageAssessed || it.masteryLevel > 0L }.map { it.id }.toSet()
        DailyVocabBuilderUiState(
            userLevel = level,
            dailyWords = dailyWords,
            completedWordIds = assignment.completedWordIds,
            wordScores = assignment.wordScores,
            lastTranscripts = assignment.lastTranscripts,
            learnedWordIds = completedAllTime + entryLearnedIds,
            weeklyEligibility = eligibility,
            learnedWordsForAssessment = learnedSeeds
        )
    }.flowOn(computeDispatcher).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DailyVocabBuilderUiState()
    )

    init {
        if (learningProgressManager != null) {
            viewModelScope.launch {
                currentLearningLevel.collect { level ->
                    if (learningProgressManager.state.value.userId.isNotBlank()) {
                        withContext(ioDispatcher) {
                            learningProgressManager.ensureDailyVocabulary(level)
                        }
                    }
                }
            }
        }
    }

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _vocabEvalState = MutableStateFlow<UiState<VocabEvaluationResult>?>(null)
    val vocabEvalState: StateFlow<UiState<VocabEvaluationResult>?> = _vocabEvalState.asStateFlow()

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
                learningProgressManager?.updateLearningPreferences(
                    currentLevel = LearningLevel.fromLabel(level),
                    dailyTargetMinutes = dailyGoalMinutes.toInt()
                )
                repository.enrollInProgram("30_day_challenge", "30-Day Communication Challenge")
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun updateUserProfile(
        displayName: String,
        level: String,
        primaryGoal: String,
        dailyGoalMinutes: Long,
        existing: UserProfile
    ) {
        viewModelScope.launch {
            val res = repository.createOrUpdateProfile(
                displayName = displayName.trim().ifBlank { existing.displayName },
                level = level,
                primaryGoal = primaryGoal,
                dailyGoalMinutes = dailyGoalMinutes.coerceIn(5L, 120L),
                fluency = existing.fluencyScore,
                pronunciation = existing.pronunciationScore,
                vocabulary = existing.vocabularyScore,
                grammar = existing.grammarScore,
                articulation = existing.articulationScore,
                listening = existing.listeningScore,
                confidenceSelf = existing.confidenceSelfRating
            )
            if (res.isSuccess) {
                learningProgressManager?.updateLearningPreferences(
                    currentLevel = LearningLevel.fromLabel(level),
                    dailyTargetMinutes = dailyGoalMinutes.toInt()
                )
                _actionMessage.value = "Profile updated!"
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
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
            if (stepId !in existing) {
                val title = CurriculumCatalog.dailySteps.firstOrNull { it.id == stepId }?.title ?: stepId
                learningProgressManager?.recordDailyRoutineStep(stepId, title)
            }
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
            learningProgressManager?.recordDailyRoutineStep("reflection", "Daily self-reflection saved")
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
            if (completed) {
                val title = CurriculumCatalog.lessons.firstOrNull { it.id == lessonId }?.title
                    ?: publishedSharedLessons.value.firstOrNull { it.id == lessonId }?.title
                    ?: lessonId
                learningProgressManager?.recordLessonCompleted(lessonId, title, score?.toInt())
            }
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
        learningProgressManager?.recordFlashcardReview(seed.id, seed.word, qualityGood)
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

    /**
     * Evaluates a user's spoken pronunciation of [seed] and marks the word Done ONLY when spoken correctly.
     */
    fun assessDailyWordSpeaking(
        seed: VocabSeed,
        existingEntry: VocabularyEntry?,
        transcript: String,
        alternatives: List<String> = emptyList(),
        confidence: Float = 0.85f
    ): LocalVocabStageEvaluation {
        val eval = learningProgressManager?.recordVocabularyWordSpokenAssessment(
            seed = seed,
            recognizedText = transcript,
            alternatives = alternatives,
            asrConfidence = confidence
        ) ?: VocabularyMasteryEngine.evaluateSpokenWordAttempt(
            seed = seed,
            transcript = transcript,
            alternatives = alternatives,
            asrConfidence = confidence
        )

        if (eval.passed) {
            viewModelScope.launch {
                repository.upsertVocabularyEntry(
                    wordId = seed.id,
                    word = seed.word,
                    phonetic = seed.phonetic,
                    meaning = seed.meaning,
                    level = seed.level,
                    exampleSentence = seed.exampleSentence,
                    collocations = seed.collocations,
                    personalNote = existingEntry?.personalNote.orEmpty(),
                    masteryLevel = ((existingEntry?.masteryLevel ?: 0L) + 1L).coerceAtLeast(1L).coerceAtMost(5L),
                    repetitions = (existingEntry?.repetitions ?: 0L) + 1L,
                    nextReviewEpochDay = todayEpochDay + 3L,
                    usageAssessed = true
                )
            }
        }
        return eval
    }

    /**
     * Persists a completed 7-day cumulative AI Quiz spoken assessment and updates spaced repetition.
     */
    fun submitWeeklyVocabAssessment(
        result: WeeklyVocabularyAssessmentRecord,
        perWordPassed: Map<String, Boolean>,
        entriesByWordId: Map<String, VocabularyEntry> = emptyMap()
    ) {
        learningProgressManager?.recordWeeklyVocabularyAssessment(result, perWordPassed)
        val bankById = CurriculumCatalog.vocabularyBank.associateBy { it.id }
        viewModelScope.launch {
            perWordPassed.forEach { (wordId, passed) ->
                val seed = bankById[wordId] ?: return@forEach
                val existing = entriesByWordId[wordId]
                val nextMastery = if (passed) ((existing?.masteryLevel ?: 1L) + 1L).coerceAtMost(5L) else (existing?.masteryLevel ?: 1L).coerceAtLeast(0L)
                val nextDays = if (passed) 7L else 1L
                repository.upsertVocabularyEntry(
                    wordId = seed.id,
                    word = seed.word,
                    phonetic = seed.phonetic,
                    meaning = seed.meaning,
                    level = seed.level,
                    exampleSentence = seed.exampleSentence,
                    collocations = seed.collocations,
                    personalNote = existing?.personalNote.orEmpty(),
                    masteryLevel = nextMastery,
                    repetitions = (existing?.repetitions ?: 0L) + if (passed) 1L else 0L,
                    nextReviewEpochDay = todayEpochDay + nextDays,
                    usageAssessed = existing?.usageAssessed ?: passed
                )
            }
            _actionMessage.value = "7-Day Spoken Assessment complete: ${result.scorePercent}% (${result.passedWordIds.size}/${result.assessedWordIds.size} words mastered)"
        }
    }

    fun evaluateVocabUsage(seed: VocabSeed, existingEntry: VocabularyEntry?, userSentence: String) {
        if (userSentence.isBlank()) {
            _actionMessage.value = "Speak a sentence using '${seed.word}' first."
            return
        }
        viewModelScope.launch {
            _vocabEvalState.value = UiState.Loading
            val eval = effectiveCoordinator.evaluateVocabularyUsage(
                word = seed.word,
                meaning = seed.meaning,
                userSentence = userSentence,
                difficulty = seed.level.ifBlank { "Intermediate" }
            )
            eval.onSuccess { result ->
                _vocabEvalState.value = UiState.Success(result)
                learningProgressManager?.recordVocabularyUsageCheck(seed.id, seed.word, result.usedCorrectly, result.naturalnessScore)
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
        if (completed) learningProgressManager?.recordRealWorldTask(taskId, title)
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

    fun deleteAccountAndAllData(onDeleted: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteCurrentUserAccountAndData()
            res.onSuccess {
                _actionMessage.value = "Account and all associated data deleted."
                onDeleted()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Account deletion failed. Please sign out, sign back in, and retry."
                _actionMessage.value = msg
                onFailure(msg)
            }
        }
    }
}
