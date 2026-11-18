package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.InteractiveCoachViewModel

@Composable
fun ConversationSimulatorScreen(
    coachVm: InteractiveCoachViewModel,
    ttsHelper: TtsAndSpeechHelper,
    userLevel: String,
    ttsRate: Float
) {
    val context = LocalContext.current
    val selectedScenario by coachVm.selectedScenario.collectAsStateWithLifecycle()
    val turns by coachVm.chatTurns.collectAsStateWithLifecycle()
    val isPartnerTyping by coachVm.isPartnerTyping.collectAsStateWithLifecycle()
    val coachDebrief by coachVm.coachDebrief.collectAsStateWithLifecycle()
    val isDictating by ttsHelper.isListening.collectAsStateWithLifecycle()

    var messageInput by remember { mutableStateOf("") }
    var voiceStatus by remember { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            ttsHelper.startDictation(
                onResult = { spoken -> messageInput = spoken },
                onError = { err -> voiceStatus = err }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (turns.isEmpty()) {
            coachVm.startScenario(selectedScenario, userLevel)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(
                title = "Conversation Simulator",
                subtitle = "Adaptive $userLevel dialogue • Natural flow without mid-chat interruptions"
            )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CurriculumCatalog.conversationScenarios) { scenario ->
                FilterChip(
                    selected = selectedScenario == scenario,
                    onClick = { coachVm.startScenario(scenario, userLevel) },
                    label = { Text(scenario) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(turns) { turn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (turn.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(0.86f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (turn.isUser) "You" else "AI Partner ($selectedScenario)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (isPartnerTyping) {
                item { Text("Partner is responding...", style = MaterialTheme.typography.bodySmall) }
            }

            item {
                when (val debrief = coachDebrief) {
                    is UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> Text(debrief.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Coach Check-In Summary", style = MaterialTheme.typography.titleMedium)
                                Text(debrief.data, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    null -> Unit
                }
            }
        }

        if (voiceStatus != null) {
            Text(voiceStatus.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = {
                    voiceStatus = null
                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (ok) {
                        ttsHelper.startDictation(
                            onResult = { spoken -> messageInput = spoken },
                            onError = { err -> voiceStatus = err }
                        )
                    } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.testTag("conversation_voice_button")
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Speak message",
                    tint = if (isDictating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Reply naturally...") },
                modifier = Modifier.weight(1f).testTag("conversation_text_input")
            )
            IconButton(
                onClick = {
                    val msg = messageInput
                    messageInput = ""
                    coachVm.sendConversationMessage(msg, userLevel) { reply ->
                        ttsHelper.speak(reply, ttsRate)
                    }
                },
                modifier = Modifier.testTag("conversation_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
            }
        }

        OutlinedButton(
            onClick = { coachVm.requestCoachCheckInAndSave(userLevel) },
            enabled = turns.size >= 2,
            modifier = Modifier.fillMaxWidth().testTag("conversation_checkin_button")
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("End & Get Coach Check-In Feedback")
        }
        }
    }
}

@Composable
fun AiMentorScreen(
    coachVm: InteractiveCoachViewModel,
    profile: UserProfile?,
    sessions: List<SpeakingSession>,
    feedbackList: List<SpeechFeedback> = emptyList()
) {
    val mentorState by coachVm.mentorAnswer.collectAsStateWithLifecycle()
    var question by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Create my personalized 7-day improvement plan",
        "Identify my recurring speaking mistakes & @askvinh fixes",
        "Recommend my next @askvinh lesson based on my scores",
        "How do I stop saying 'um' in interviews?"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.img_mentor_avatar),
                    contentDescription = "SpeakCraft AI Mentor emblem",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Personalized AI Mentor", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Grounded in @askvinh • Context-aware of your ${sessions.size} recordings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            coachVm.askMentor(
                                profile = profile,
                                sessions = sessions,
                                feedbackList = feedbackList,
                                question = prompt
                            )
                        },
                        label = { Text(prompt) }
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Ask your communication mentor...") },
                    modifier = Modifier.weight(1f).testTag("mentor_question_input")
                )
                Button(
                    onClick = {
                        val q = question
                        question = ""
                        coachVm.askMentor(
                            profile = profile,
                            sessions = sessions,
                            feedbackList = feedbackList,
                            question = q
                        )
                    },
                    modifier = Modifier.testTag("mentor_ask_button")
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = "Ask mentor")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (val s = mentorState) {
                        is UiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                Text("Reviewing your scores and @askvinh curriculum...")
                            }
                        }
                        is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> Text(s.data, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        }
    }
}
