package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.activity.ActivityAnalyticsSnapshot
import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityDifficulty
import com.example.data.activity.ActivityDurationFilter
import com.example.data.activity.ActivityEngine
import com.example.data.activity.ActivityEvaluationResult
import com.example.data.activity.ActivityModuleDefinition
import com.example.data.activity.ActivityProgressAnalyticsEngine
import com.example.data.activity.ActivitySearchFilter
import com.example.data.activity.ActivitySubmissionOutcome
import com.example.data.activity.ActivityUserResponse
import com.example.data.activity.DailyPracticeEngine
import com.example.data.activity.DailyPracticePlan
import com.example.data.activity.DailyPracticePlanRecord
import com.example.data.activity.DialogueTurn
import com.example.data.activity.ExerciseTemplateType
import com.example.data.activity.LearningPathProgressSummary
import com.example.data.activity.LearningPathsEngine
import com.example.data.activity.LoadedActivityResult
import com.example.data.activity.SavedActivityRecord
import com.example.data.activity.UserActivityProgress
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.ActivityFirestoreRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class ActivitySessionStage {
    data object Ready : ActivitySessionStage()
    data object Active : ActivitySessionStage()
    data object Evaluating : ActivitySessionStage()
    data class Completed(
        val outcome: ActivitySubmissionOutcome,
        val persistedToCloud: Boolean
    ) : ActivitySessionStage()
    data class Invalid(val activityId: String, val errors: List<String>) : ActivitySessionStage()
}

data class ActiveActivityUiState(
    val activity: ActivityDefinition? = null,
    val module: ActivityModuleDefinition? = null,
    val stage: ActivitySessionStage = ActivitySessionStage.Ready,
    val elapsedSeconds: Int = 0,
    val textResponse: String = "",
    val recordedAudioPath: String = "",
    val recordedVideoPath: String = "",
    val completedChecklistItems: Set<String> = emptySet(),
    val dialogueTurns: List<DialogueTurn> = emptyList(),
    val dialogueReplyLoading: Boolean = false,
    val confidenceSelfRating: Int = 70,
    val confidenceRatingTouched: Boolean = false,
    val validationError: String? = null
)

class ActivityEngineViewModel(
    private val activityRepository: ActivityFirestoreRepository?,
    private val activityEngine: ActivityEngine,
    private val localOrchestrator: LocalIntelligenceOrchestrator? = null,
    val audioRecorder: AudioRecorderManager?,
    private val userId: String,
    @Suppress("UNUSED_PARAMETER", "DEPRECATION") geminiCoach: com.example.data.ai.GeminiCoachService? = null
) : ViewModel() {

    val todayDateKey: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Local shadow state enables instant UI updates and full offline/test resilience
    private val _localProgress = MutableStateFlow<List<UserActivityProgress>>(emptyList())
    private val _localAttempts = MutableStateFlow<List<ActivityAttemptRecord>>(emptyList())
    private val _localSaved = MutableStateFlow<List<SavedActivityRecord>>(emptyList())
    private val _localDailyRecord = MutableStateFlow<DailyPracticePlanRecord?>(null)

    private val _selectedDailyMinutes = MutableStateFlow(20)
    val selectedDailyMinutes: StateFlow<Int> = _selectedDailyMinutes.asStateFlow()

    private val _customDailyPlan = MutableStateFlow<DailyPracticePlan?>(null)

    private val _searchFilter = MutableStateFlow(ActivitySearchFilter())
    val searchFilter: StateFlow<ActivitySearchFilter> = _searchFilter.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _activeState = MutableStateFlow(ActiveActivityUiState())
    val activeState: StateFlow<ActiveActivityUiState> = _activeState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var timerJob: Job? = null

    val progressList: StateFlow<List<UserActivityProgress>> = (
        activityRepository?.observeActivityProgress(userId)
            ?.catch { err ->
                val fsErr = generateSequence(err) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && err is CancellationException) throw err
                emit(_localProgress.value)
            }
            ?: _localProgress
        ).combine(_localProgress) { remote, local ->
            if (remote.isEmpty() && local.isNotEmpty()) local
            else {
                val merged = LinkedHashMap<String, UserActivityProgress>()
                local.forEach { merged[it.activityId] = it }
                remote.forEach { merged[it.activityId] = it }
                merged.values.toList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attemptHistory: StateFlow<List<ActivityAttemptRecord>> = (
        activityRepository?.observeActivityAttempts(userId)
            ?.catch { err ->
                val fsErr = generateSequence(err) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && err is CancellationException) throw err
                emit(_localAttempts.value)
            }
            ?: _localAttempts
        ).combine(_localAttempts) { remote, local ->
            if (remote.isEmpty()) local else remote
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedActivities: StateFlow<List<SavedActivityRecord>> = (
        activityRepository?.observeSavedActivities(userId)
            ?.catch { err ->
                val fsErr = generateSequence(err) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && err is CancellationException) throw err
                emit(_localSaved.value)
            }
            ?: _localSaved
        ).combine(_localSaved) { remote, local ->
            if (remote.isEmpty() && local.isNotEmpty()) local else remote
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyPlanRecord: StateFlow<DailyPracticePlanRecord?> = (
        activityRepository?.observeDailyPracticePlan(userId, todayDateKey)
            ?.catch { err ->
                val fsErr = generateSequence(err) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && err is CancellationException) throw err
                emit(_localDailyRecord.value)
            }
            ?: _localDailyRecord
        ).combine(_localDailyRecord) { remote, local ->
            remote ?: local
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val analyticsSnapshot: StateFlow<ActivityAnalyticsSnapshot> = combine(
        progressList,
        attemptHistory
    ) { progress, attempts ->
        ActivityProgressAnalyticsEngine.computeSnapshot(progress, attempts, todayDateKey)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ActivityProgressAnalyticsEngine.computeSnapshot(emptyList(), emptyList(), todayDateKey)
    )

    val learningPathSummaries: StateFlow<List<LearningPathProgressSummary>> = progressList
        .combine(_localProgress) { p, _ -> LearningPathsEngine.evaluateAllPaths(p) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LearningPathsEngine.evaluateAllPaths(emptyList())
        )

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    // ==================== ACTIVITY LOADING & LIFECYCLE ====================

    fun loadActivity(activityId: String) {
        stopTimer()
        audioRecorder?.stopPlayback()
        if (audioRecorder?.isRecording?.value == true) {
            audioRecorder.stopRecording()
        }
        when (val result = activityEngine.loadActivity(activityId)) {
            is LoadedActivityResult.Invalid -> {
                _activeState.value = ActiveActivityUiState(
                    activity = null,
                    module = null,
                    stage = ActivitySessionStage.Invalid(result.activityId, result.errors)
                )
            }
            is LoadedActivityResult.Valid -> {
                val openingTurns = result.activity.interactionConfiguration.dialogueOpeningLine
                    .takeIf { it.isNotBlank() }
                    ?.let { listOf(DialogueTurn(role = "model", text = it)) }
                    ?: emptyList()
                _activeState.value = ActiveActivityUiState(
                    activity = result.activity,
                    module = result.module,
                    stage = ActivitySessionStage.Ready,
                    elapsedSeconds = 0,
                    dialogueTurns = openingTurns
                )
            }
        }
    }

    fun startActivity() {
        val current = _activeState.value
        if (current.activity == null) return
        _activeState.update { it.copy(stage = ActivitySessionStage.Active, validationError = null) }
        startTimer()
    }

    fun retryCurrentActivity() {
        val id = _activeState.value.activity?.activityId ?: return
        loadActivity(id)
        startActivity()
    }

    fun updateTextResponse(value: String) {
        _activeState.update { it.copy(textResponse = value, validationError = null) }
    }

    fun toggleChecklistItem(item: String) {
        _activeState.update { state ->
            val updated = state.completedChecklistItems.toMutableSet()
            if (!updated.add(item)) updated.remove(item)
            state.copy(completedChecklistItems = updated, validationError = null)
        }
    }

    fun updateConfidenceSelfRating(rating: Int) {
        _activeState.update {
            it.copy(
                confidenceSelfRating = rating.coerceIn(1, 100),
                confidenceRatingTouched = true
            )
        }
    }

    fun startAudioRecording(): Boolean {
        val recorder = audioRecorder ?: return false
        if (_activeState.value.stage is ActivitySessionStage.Ready) {
            startActivity()
        }
        return recorder.startRecording(viewModelScope)
    }

    fun stopAudioRecording() {
        val file = audioRecorder?.stopRecording()
        if (file != null) {
            _activeState.update {
                it.copy(
                    recordedAudioPath = file.absolutePath,
                    elapsedSeconds = maxOf(it.elapsedSeconds, audioRecorder?.elapsedSeconds?.value ?: 0),
                    validationError = null
                )
            }
        }
    }

    fun attachRecordedVideo(file: File, durationSec: Int) {
        if (_activeState.value.stage is ActivitySessionStage.Ready) {
            startActivity()
        }
        _activeState.update {
            it.copy(
                recordedVideoPath = file.absolutePath,
                elapsedSeconds = maxOf(it.elapsedSeconds, durationSec),
                validationError = null
            )
        }
    }

    fun sendDialogueMessage(userMessage: String, userLevel: String) {
        val clean = userMessage.trim()
        val activity = _activeState.value.activity ?: return
        if (clean.isBlank()) return
        if (_activeState.value.stage is ActivitySessionStage.Ready) {
            startActivity()
        }
        val historyBefore = _activeState.value.dialogueTurns
        val withUser = historyBefore + DialogueTurn("user", clean)
        _activeState.update {
            it.copy(dialogueTurns = withUser, dialogueReplyLoading = true, validationError = null)
        }

        viewModelScope.launch {
            val scenario = "${activity.title}: ${activity.interactionConfiguration.dialoguePartnerRole.ifBlank { activity.description }}"
            val pairs = historyBefore.map { it.role to it.text }
            val reply = localOrchestrator?.generateConversationTurn(scenario, userLevel, pairs, clean)?.getOrNull()
                ?: "That makes sense! Could you share one specific example or next step?"
            _activeState.update { state ->
                state.copy(
                    dialogueTurns = state.dialogueTurns + DialogueTurn("model", reply),
                    dialogueReplyLoading = false
                )
            }
        }
    }

    fun submitActiveResponse() {
        val state = _activeState.value
        val activity = state.activity ?: return
        if (audioRecorder?.isRecording?.value == true) {
            stopAudioRecording()
        }
        val freshState = _activeState.value
        val audioBytes = freshState.recordedAudioPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { File(path).takeIf { it.exists() && it.length() > 0L }?.readBytes() }.getOrNull() }

        val userResponse = ActivityUserResponse(
            activityId = activity.activityId,
            textResponse = freshState.textResponse,
            recordedAudioPath = freshState.recordedAudioPath,
            recordedVideoPath = freshState.recordedVideoPath,
            mediaBytes = audioBytes,
            mediaMimeType = if (audioBytes != null) "audio/mp4" else "",
            durationSeconds = freshState.elapsedSeconds.coerceAtLeast(10),
            completedChecklistItems = freshState.completedChecklistItems.toList(),
            dialogueTurns = freshState.dialogueTurns,
            confidenceSelfRating = if (freshState.confidenceRatingTouched) freshState.confidenceSelfRating else null
        )

        val validationMessage = activityEngine.validateUserSubmission(activity, userResponse)
        if (validationMessage != null) {
            _activeState.update { it.copy(validationError = validationMessage) }
            return
        }

        stopTimer()
        _activeState.update { it.copy(stage = ActivitySessionStage.Evaluating, validationError = null) }

        viewModelScope.launch {
            val existingProgress = progressList.value.firstOrNull { it.activityId == activity.activityId }
            val outcome = activityEngine.evaluateAndBuildOutcome(
                userId = userId,
                activity = activity,
                response = userResponse,
                existingProgress = existingProgress,
                allProgress = progressList.value,
                recentAttempts = attemptHistory.value,
                todayDateKey = todayDateKey
            )

            // Update local shadow immediately
            _localProgress.update { current ->
                current.filterNot { it.activityId == activity.activityId } + outcome.updatedProgress
            }
            _localAttempts.update { current ->
                listOf(outcome.attemptRecord) + current
            }

            var persisted = false
            if (activityRepository != null) {
                val pRes = activityRepository.saveActivityProgress(
                    progress = outcome.updatedProgress,
                    isUpdate = existingProgress != null
                )
                val aRes = activityRepository.recordActivityAttempt(outcome.attemptRecord)
                persisted = pRes.isSuccess && aRes.isSuccess
                if (!persisted) {
                    _statusBanner.value = "Saved locally; cloud sync will retry when online."
                }
            }

            _activeState.update {
                it.copy(
                    stage = ActivitySessionStage.Completed(outcome, persisted)
                )
            }
        }
    }

    // ==================== BOOKMARKS, SEARCH & DAILY PLAN ====================

    fun toggleSavedActivity(activity: ActivityDefinition) {
        val currentlySaved = savedActivities.value.any { it.activityId == activity.activityId }
        viewModelScope.launch {
            if (currentlySaved) {
                _localSaved.update { list -> list.filterNot { it.activityId == activity.activityId } }
            } else {
                _localSaved.update { list ->
                    listOf(
                        SavedActivityRecord(
                            id = activity.activityId,
                            userId = userId,
                            activityId = activity.activityId,
                            moduleId = activity.moduleId,
                            title = activity.title
                        )
                    ) + list.filterNot { it.activityId == activity.activityId }
                }
            }
            val result = activityRepository?.toggleSavedActivity(
                activityId = activity.activityId,
                moduleId = activity.moduleId,
                title = activity.title,
                currentlySaved = currentlySaved
            )
            _statusBanner.value = when {
                result == null || result.isSuccess -> if (currentlySaved) "Removed from Saved Activities" else "Saved '${activity.title}'"
                else -> "Updated locally; cloud bookmark sync pending."
            }
        }
    }

    fun selectDailyPlanMinutes(minutes: Int, profile: UserProfile?) {
        _selectedDailyMinutes.value = minutes
        _customDailyPlan.value = null
        val plan = buildEffectiveDailyPlan(profile)
        persistDailyPlan(plan)
    }

    fun buildEffectiveDailyPlan(profile: UserProfile?): DailyPracticePlan {
        val existingCustom = _customDailyPlan.value
        val completedToday = attemptHistory.value
            .filter { it.dateKey == todayDateKey }
            .map { it.activityId }
            .toSet()

        if (existingCustom != null && existingCustom.targetMinutes == _selectedDailyMinutes.value) {
            return existingCustom.copy(
                blocks = existingCustom.blocks.map { blk ->
                    blk.copy(completed = blk.activityId in completedToday)
                }
            )
        }

        val savedRecord = dailyPlanRecord.value
        val pinnedIds = savedRecord?.pinnedActivityIds.orEmpty()
        return DailyPracticeEngine.generatePlan(
            dateKey = todayDateKey,
            targetMinutes = _selectedDailyMinutes.value,
            profile = profile,
            progressList = progressList.value,
            recentAttempts = attemptHistory.value,
            pinnedActivityIds = pinnedIds,
            completedTodayIds = completedToday
        )
    }

    fun swapDailyPlanActivity(activityIdToReplace: String, profile: UserProfile?) {
        val current = buildEffectiveDailyPlan(profile)
        val swapped = DailyPracticeEngine.swapBlockInPlan(
            currentPlan = current,
            activityIdToReplace = activityIdToReplace,
            recentAttempts = attemptHistory.value
        )
        _customDailyPlan.value = swapped
        persistDailyPlan(swapped)
        _statusBanner.value = "Swapped activity in today's ${swapped.targetMinutes}-min plan."
    }

    private fun persistDailyPlan(plan: DailyPracticePlan) {
        val isUpdate = dailyPlanRecord.value != null
        val record = DailyPracticePlanRecord(
            id = plan.dateKey,
            userId = userId,
            dateKey = plan.dateKey,
            targetMinutes = plan.targetMinutes.toLong(),
            plannedActivityIds = plan.blocks.map { it.activityId },
            completedActivityIds = plan.blocks.filter { it.completed }.map { it.activityId },
            pinnedActivityIds = plan.blocks.filter { it.pinned }.map { it.activityId },
            personalizationSummary = plan.personalizationSummary
        )
        _localDailyRecord.value = record
        viewModelScope.launch {
            activityRepository?.saveDailyPracticePlan(record, isUpdate)
        }
    }

    fun updateSearchQuery(query: String) {
        _currentPage.value = 1
        _searchFilter.update { it.copy(query = query) }
    }

    fun updateModuleFilter(moduleId: String?) {
        _currentPage.value = 1
        _searchFilter.update { it.copy(moduleId = moduleId) }
    }

    fun updateDifficultyFilter(difficulty: ActivityDifficulty?) {
        _currentPage.value = 1
        _searchFilter.update { it.copy(difficulty = difficulty) }
    }

    fun updateDurationFilter(duration: ActivityDurationFilter) {
        _currentPage.value = 1
        _searchFilter.update { it.copy(durationFilter = duration) }
    }

    fun updateExerciseTypeFilter(type: ExerciseTemplateType?) {
        _currentPage.value = 1
        _searchFilter.update { it.copy(exerciseType = type) }
    }

    fun clearAllFilters() {
        _currentPage.value = 1
        _searchFilter.value = ActivitySearchFilter()
    }

    fun loadMoreCatalogueItems() {
        _currentPage.update { it + 1 }
    }

    fun recommendedActivities(profile: UserProfile?): List<ActivityDefinition> =
        activityEngine.recommendNextActivities(
            currentActivity = null,
            allProgress = progressList.value,
            recentAttempts = attemptHistory.value,
            profile = profile,
            limit = 6
        )

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _activeState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
