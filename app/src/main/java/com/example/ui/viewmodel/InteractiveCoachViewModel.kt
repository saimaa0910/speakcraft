package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

class InteractiveCoachViewModel(
    private val repository: SpeakCraftRepository,
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val appContext: Context? = null,
    private val currentUserId: String = "default",
    private val hybridCoordinator: HybridAiCoachCoordinator? = null,
    private val prefsRepo: AppPreferencesRepository? = null
) : ViewModel() {

    private val effectiveCoordinator: HybridAiCoachCoordinator by lazy {
        hybridCoordinator ?: HybridAiCoachCoordinator(
            localOrchestrator = localOrchestrator,
            prefsRepo = prefsRepo,
            appContext = appContext
        )
    }

    private val defaultMentorGreeting = ChatTurn(
        isUser = false,
        text = "Ask me anything about your speaking scores, recurring mistakes, or paste any sentence for a line-by-line rewrite."
    )

    private val _selectedScenario = MutableStateFlow("Job interviews")
    val selectedScenario: StateFlow<String> = _selectedScenario.asStateFlow()

    private val _chatTurns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val chatTurns: StateFlow<List<ChatTurn>> = _chatTurns.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _conversationError = MutableStateFlow<String?>(null)
    val conversationError: StateFlow<String?> = _conversationError.asStateFlow()

    private val _voiceLiveActive = MutableStateFlow(false)
    val voiceLiveActive: StateFlow<Boolean> = _voiceLiveActive.asStateFlow()

    private val _voiceLiveStatus = MutableStateFlow("Ready for Voice Call")
    val voiceLiveStatus: StateFlow<String> = _voiceLiveStatus.asStateFlow()

    private val _voiceAutoSpeak = MutableStateFlow(true)
    val voiceAutoSpeak: StateFlow<Boolean> = _voiceAutoSpeak.asStateFlow()

    private val _coachDebrief = MutableStateFlow<UiState<String>?>(null)
    val coachDebrief: StateFlow<UiState<String>?> = _coachDebrief.asStateFlow()

    private val _selectedMentorRole = MutableStateFlow("mentor")
    val selectedMentorRole: StateFlow<String> = _selectedMentorRole.asStateFlow()

    private val _mentorMessages = MutableStateFlow(loadPersistedMentorHistory())
    val mentorMessages: StateFlow<List<ChatTurn>> = _mentorMessages.asStateFlow()

    private val _isMentorResponding = MutableStateFlow(false)
    val isMentorResponding: StateFlow<Boolean> = _isMentorResponding.asStateFlow()

    private val _mentorError = MutableStateFlow<String?>(null)
    val mentorError: StateFlow<String?> = _mentorError.asStateFlow()

    private val _mentorAnswer = MutableStateFlow<UiState<String>>(
        UiState.Success(defaultMentorGreeting.text)
    )
    val mentorAnswer: StateFlow<UiState<String>> = _mentorAnswer.asStateFlow()

    private var lastQuestionText: String = ""
    private var lastProfileSnapshot: UserProfile? = null
    private var lastSessionsSnapshot: List<SpeakingSession> = emptyList()
    private var lastFeedbackSnapshot: List<SpeechFeedback> = emptyList()

    private var lastConversationMsg: String = ""
    private var lastConversationLevel: String = "Intermediate"

    fun setMentorRole(roleKey: String) {
        _selectedMentorRole.value = roleKey
    }

    fun setVoiceAutoSpeak(enabled: Boolean) {
        _voiceAutoSpeak.value = enabled
        viewModelScope.launch {
            prefsRepo?.setVoiceAutoSpeakEnabled(enabled)
        }
    }

    private fun mentorHistoryFile(): File? {
        val ctx = appContext ?: return null
        val safeUid = currentUserId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "default" }
        return File(ctx.filesDir, "mentor_history_$safeUid.json")
    }

    private fun loadPersistedMentorHistory(): List<ChatTurn> {
        val file = mentorHistoryFile()
        if (file == null || !file.exists()) return listOf(defaultMentorGreeting)
        return runCatching {
            val arr = JSONArray(file.readText())
            val list = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val txt = obj.optString("text")
                if (txt.isBlank()) null else ChatTurn(isUser = obj.optBoolean("isUser", false), text = txt)
            }
            list.ifEmpty { listOf(defaultMentorGreeting) }
        }.getOrElse { listOf(defaultMentorGreeting) }
    }

    private fun persistMentorHistory(turns: List<ChatTurn>) {
        val file = mentorHistoryFile() ?: return
        runCatching {
            val arr = JSONArray()
            turns.takeLast(40).forEach { turn ->
                arr.put(JSONObject().apply {
                    put("isUser", turn.isUser)
                    put("text", turn.text)
                })
            }
            file.writeText(arr.toString())
        }
    }

    fun startScenario(scenario: String, userLevel: String) {
        stopVoiceLiveSession()
        _selectedScenario.value = scenario
        _coachDebrief.value = null
        _conversationError.value = null
        val greeting = when (scenario) {
            "Job interviews" -> "Thanks for coming in today. To kick things off, walk me through a challenging project you led or contributed to."
            "Handling disagreements" -> "I hear your perspective, though I still think shipping the simpler version on Friday makes more sense. What makes you push for the redesign?"
            "Talking with managers" -> "Hey, glad we caught up for our 1-on-1. What's top of mind for you this week?"
            else -> "Hi! Great to connect for this $scenario practice ($userLevel pace). How would you like to open?"
        }
        _chatTurns.value = listOf(ChatTurn(isUser = false, text = greeting))
    }

    fun sendConversationMessage(
        message: String,
        userLevel: String,
        onPartnerReply: (String) -> Unit
    ) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return
        lastConversationMsg = trimmed
        lastConversationLevel = userLevel
        _conversationError.value = null

        val currentHistory = _chatTurns.value
        _chatTurns.value = currentHistory + ChatTurn(isUser = true, text = trimmed)

        viewModelScope.launch {
            _isPartnerTyping.value = true
            val historyPairs = currentHistory.map { (if (it.isUser) "user" else "model") to it.text }
            val res = effectiveCoordinator.generateConversationTurn(_selectedScenario.value, userLevel, historyPairs, trimmed)
            _isPartnerTyping.value = false
            res.onSuccess { reply ->
                _chatTurns.value = _chatTurns.value + ChatTurn(isUser = false, text = reply)
                onPartnerReply(reply)
            }.onFailure { err ->
                _conversationError.value = err.localizedMessage ?: "Conversation turn could not be generated. Please retry."
            }
        }
    }

    fun retryLastConversationMessage(onPartnerReply: (String) -> Unit) {
        if (lastConversationMsg.isBlank()) return
        _conversationError.value = null
        val historyWithoutRetryDup = _chatTurns.value
        viewModelScope.launch {
            _isPartnerTyping.value = true
            val historyPairs = historyWithoutRetryDup.dropLastWhile { it.isUser }
                .map { (if (it.isUser) "user" else "model") to it.text }
            val res = effectiveCoordinator.generateConversationTurn(
                _selectedScenario.value,
                lastConversationLevel,
                historyPairs,
                lastConversationMsg
            )
            _isPartnerTyping.value = false
            res.onSuccess { reply ->
                _chatTurns.value = _chatTurns.value + ChatTurn(isUser = false, text = reply)
                onPartnerReply(reply)
            }.onFailure { err ->
                _conversationError.value = err.localizedMessage ?: "Retry failed."
            }
        }
    }

    fun setLocalVoiceCallState(active: Boolean, status: String) {
        _voiceLiveActive.value = active
        _voiceLiveStatus.value = status
    }

    fun stopVoiceLiveSession() {
        _voiceLiveActive.value = false
        _voiceLiveStatus.value = "Ready for Voice Call"
    }

    fun requestCoachCheckInAndSave(userLevel: String) {
        val turns = _chatTurns.value
        if (turns.size < 2) return
        viewModelScope.launch {
            _coachDebrief.value = UiState.Loading
            val transcript = turns.joinToString("\n") { (if (it.isUser) "Learner: " else "Partner: ") + it.text }
            val res = effectiveCoordinator.generateConversationCheckIn(_selectedScenario.value, transcript)
            res.onSuccess { feedback ->
                _coachDebrief.value = UiState.Success(feedback)
                repository.saveConversationSession(
                    scenario = _selectedScenario.value,
                    difficultyLevel = userLevel,
                    turnCount = turns.size.toLong(),
                    summaryFeedback = feedback,
                    transcriptPreview = transcript
                )
            }.onFailure { err ->
                _coachDebrief.value = UiState.Error(err.localizedMessage ?: "Failed generating coaching summary.")
            }
        }
    }

    fun askMentor(
        profile: UserProfile?,
        sessions: List<SpeakingSession>,
        feedbackList: List<SpeechFeedback> = emptyList(),
        question: String
    ) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _isMentorResponding.value) return
        lastQuestionText = trimmed
        lastProfileSnapshot = profile
        lastSessionsSnapshot = sessions
        lastFeedbackSnapshot = feedbackList

        val priorHistory = _mentorMessages.value
        val withUser = priorHistory + ChatTurn(isUser = true, text = trimmed)
        _mentorMessages.value = withUser
        persistMentorHistory(withUser)

        val historyPairs = priorHistory
            .drop(1) // skip static greeting
            .takeLast(10)
            .map { (if (it.isUser) "user" else "model") to it.text }

        val recentContextSnippet = priorHistory.takeLast(4).joinToString("\n") {
            (if (it.isUser) "User: " else "Mentor: ") + it.text.take(300)
        }
        val contextualQuestion = if (priorHistory.size > 1) {
            "Recent chat context:\n$recentContextSnippet\n\nFollow-up question: $trimmed"
        } else trimmed

        runMentorStreamRequest(
            withUser = withUser,
            historyPairs = historyPairs,
            questionText = contextualQuestion,
            profile = profile,
            sessions = sessions,
            feedbackList = feedbackList
        )
    }

    fun retryLastMentorQuestion() {
        if (lastQuestionText.isBlank() || _isMentorResponding.value) return
        val current = _mentorMessages.value
        val withUser = if (current.lastOrNull()?.isUser == true) {
            current
        } else {
            current + ChatTurn(isUser = true, text = lastQuestionText)
        }
        val historyPairs = withUser.dropLast(1).drop(1).takeLast(10)
            .map { (if (it.isUser) "user" else "model") to it.text }
        runMentorStreamRequest(
            withUser = withUser,
            historyPairs = historyPairs,
            questionText = lastQuestionText,
            profile = lastProfileSnapshot,
            sessions = lastSessionsSnapshot,
            feedbackList = lastFeedbackSnapshot
        )
    }

    private fun runMentorStreamRequest(
        withUser: List<ChatTurn>,
        historyPairs: List<Pair<String, String>>,
        questionText: String,
        profile: UserProfile?,
        sessions: List<SpeakingSession>,
        feedbackList: List<SpeechFeedback>
    ) {
        viewModelScope.launch {
            _isMentorResponding.value = true
            _mentorError.value = null
            _mentorAnswer.value = UiState.Loading
            val sb = StringBuilder()
            try {
                withTimeout(60_000L) {
                    effectiveCoordinator.askMentorStream(
                        profile = profile,
                        recentSessions = sessions,
                        recentFeedback = feedbackList,
                        history = historyPairs,
                        userQuestion = questionText,
                        roleKey = _selectedMentorRole.value
                    ).collect { chunk ->
                        sb.append(chunk)
                        if (sb.isNotBlank()) {
                            val partial = sb.toString()
                            _mentorAnswer.value = UiState.Success(partial)
                            _mentorMessages.value = withUser + ChatTurn(isUser = false, text = partial)
                        }
                    }
                }
                if (sb.isBlank()) {
                    _mentorError.value = "Mentor returned an empty response."
                    _mentorAnswer.value = UiState.Error("Mentor returned an empty response.")
                    _mentorMessages.value = withUser
                } else {
                    val finalized = withUser + ChatTurn(isUser = false, text = sb.toString())
                    _mentorMessages.value = finalized
                    persistMentorHistory(finalized)
                }
            } catch (e: TimeoutCancellationException) {
                _mentorError.value = "Mentor timed out after 60 seconds. Please try again."
                _mentorAnswer.value = UiState.Error("Mentor timed out after 60 seconds. Please try again.")
                _mentorMessages.value = withUser
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Mentor failed to respond."
                _mentorError.value = msg
                _mentorAnswer.value = UiState.Error(msg)
                _mentorMessages.value = withUser
            } finally {
                _isMentorResponding.value = false
            }
        }
    }

    fun clearMentorThread() {
        val reset = listOf(defaultMentorGreeting)
        _mentorMessages.value = reset
        _mentorError.value = null
        _mentorAnswer.value = UiState.Success(defaultMentorGreeting.text)
        persistMentorHistory(reset)
    }

    override fun onCleared() {
        stopVoiceLiveSession()
        super.onCleared()
    }
}
