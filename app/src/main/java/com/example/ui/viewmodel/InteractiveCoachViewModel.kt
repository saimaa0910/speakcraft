package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiCoachService
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.YouTubeSource
import com.example.data.youtube.YouTubeIntelligenceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

data class YouTubeAuditUiState(
    val isAuditing: Boolean = false,
    val statusSummary: String = "",
    val error: String? = null
)

class InteractiveCoachViewModel(
    private val repository: SpeakCraftRepository,
    private val geminiCoach: GeminiCoachService,
    private val youtubeClient: YouTubeIntelligenceClient,
    private val appContext: Context? = null,
    private val currentUserId: String = "default"
) : ViewModel() {

    private val defaultMentorGreeting = ChatTurn(
        isUser = false,
        text = "Ask me anything about your speaking scores, recurring mistakes, or @askvinh techniques."
    )

    private val _selectedScenario = MutableStateFlow("Job interviews")
    val selectedScenario: StateFlow<String> = _selectedScenario.asStateFlow()

    private val _chatTurns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val chatTurns: StateFlow<List<ChatTurn>> = _chatTurns.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _coachDebrief = MutableStateFlow<UiState<String>?>(null)
    val coachDebrief: StateFlow<UiState<String>?> = _coachDebrief.asStateFlow()

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

    private val _ytAuditState = MutableStateFlow(YouTubeAuditUiState())
    val ytAuditState: StateFlow<YouTubeAuditUiState> = _ytAuditState.asStateFlow()

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
        _selectedScenario.value = scenario
        _coachDebrief.value = null
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
        val currentHistory = _chatTurns.value
        _chatTurns.value = currentHistory + ChatTurn(isUser = true, text = trimmed)

        viewModelScope.launch {
            _isPartnerTyping.value = true
            val historyPairs = currentHistory.map { (if (it.isUser) "user" else "model") to it.text }
            val res = geminiCoach.sendConversationTurn(_selectedScenario.value, userLevel, historyPairs, trimmed)
            _isPartnerTyping.value = false
            res.onSuccess { reply ->
                _chatTurns.value = _chatTurns.value + ChatTurn(isUser = false, text = reply)
                onPartnerReply(reply)
            }.onFailure { err ->
                _chatTurns.value = _chatTurns.value + ChatTurn(isUser = false, text = "[Connection issue: ${err.localizedMessage}]")
            }
        }
    }

    fun requestCoachCheckInAndSave(userLevel: String) {
        val turns = _chatTurns.value
        if (turns.size < 2) return
        viewModelScope.launch {
            _coachDebrief.value = UiState.Loading
            val transcript = turns.joinToString("\n") { (if (it.isUser) "Learner: " else "Partner: ") + it.text }
            val res = geminiCoach.generateConversationCheckIn(_selectedScenario.value, transcript)
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
        val priorHistory = _mentorMessages.value
        val withUser = priorHistory + ChatTurn(isUser = true, text = trimmed)
        _mentorMessages.value = withUser
        persistMentorHistory(withUser)

        val recentContextSnippet = priorHistory.takeLast(4).joinToString("\n") {
            (if (it.isUser) "User: " else "Mentor: ") + it.text.take(300)
        }
        val contextualQuestion = if (priorHistory.size > 1) {
            "Recent chat context:\n$recentContextSnippet\n\nFollow-up question: $trimmed"
        } else trimmed

        viewModelScope.launch {
            _isMentorResponding.value = true
            _mentorError.value = null
            _mentorAnswer.value = UiState.Loading
            val sb = StringBuilder()
            val placeholder = withUser + ChatTurn(isUser = false, text = "")
            try {
                withTimeout(60_000L) {
                    geminiCoach.askMentorStream(profile, sessions, feedbackList, contextualQuestion).collect { chunk ->
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

    fun auditYouTubeUrl(url: String, youtubeApiKey: String) {
        viewModelScope.launch {
            _ytAuditState.value = YouTubeAuditUiState(
                isAuditing = true,
                statusSummary = "Inspecting channel/playlist/video metadata & public captions..."
            )
            val auditRes = youtubeClient.inspectUrl(url, youtubeApiKey)
            auditRes.onFailure { err ->
                _ytAuditState.value = YouTubeAuditUiState(
                    isAuditing = false,
                    error = err.localizedMessage ?: "Could not inspect YouTube URL."
                )
            }.onSuccess { audit ->
                var extractedCount = 0
                audit.videos.forEachIndexed { idx, video ->
                    _ytAuditState.value = YouTubeAuditUiState(
                        isAuditing = true,
                        statusSummary = "Analyzing video ${idx + 1}/${audit.videos.size}: ${video.title.take(40)}..."
                    )
                    if (video.extractionStatus == "INACCESSIBLE") {
                        repository.saveYouTubeAnalysis(
                            source = YouTubeSource(
                                videoId = video.videoId,
                                channelHandle = video.channelHandle,
                                playlistTitle = video.playlistTitle,
                                publishedDate = video.publishedDate,
                                durationSeconds = video.durationSeconds,
                                title = video.title,
                                videoUrl = video.videoUrl,
                                thumbnailUrl = video.thumbnailUrl,
                                mainTopic = "Inaccessible",
                                category = "Public Speaking",
                                difficulty = "Intermediate",
                                extractionStatus = "INACCESSIBLE",
                                statusDetail = video.statusDetail,
                                transcriptAvailable = false
                            ),
                            techniques = emptyList()
                        )
                    } else {
                        val bundleRes = geminiCoach.extractYouTubeLessons(
                            videoId = video.videoId,
                            videoTitle = video.title,
                            videoUrl = video.videoUrl,
                            description = video.description,
                            transcriptText = video.transcript
                        )
                        bundleRes.onSuccess { bundle ->
                            extractedCount += bundle.techniques.size
                            repository.saveYouTubeAnalysis(
                                source = YouTubeSource(
                                    videoId = video.videoId,
                                    channelHandle = video.channelHandle,
                                    playlistTitle = video.playlistTitle,
                                    publishedDate = video.publishedDate,
                                    durationSeconds = video.durationSeconds,
                                    title = video.title,
                                    videoUrl = video.videoUrl,
                                    thumbnailUrl = video.thumbnailUrl,
                                    mainTopic = bundle.mainTopic,
                                    category = bundle.category,
                                    difficulty = bundle.difficulty,
                                    extractionStatus = video.extractionStatus,
                                    statusDetail = video.statusDetail,
                                    transcriptAvailable = video.transcript != null
                                ),
                                techniques = bundle.techniques
                            )
                        }
                    }
                }
                _ytAuditState.value = YouTubeAuditUiState(
                    isAuditing = false,
                    statusSummary = "${audit.statusNote} Saved $extractedCount source-attributed techniques."
                )
            }
        }
    }
}
