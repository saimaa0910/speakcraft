package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.example.ui.components.AnimatedThinkingDots
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.viewmodel.InteractiveCoachViewModel

private fun bubbleShapeFor(isUser: Boolean): RoundedCornerShape = if (isUser) {
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
} else {
    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
}

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
    var pendingScenarioSwitch by remember { mutableStateOf<String?>(null) }

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

    pendingScenarioSwitch?.let { targetScenario ->
        AlertDialog(
            onDismissRequest = { pendingScenarioSwitch = null },
            title = { Text("Switch Conversation Scenario?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Switching to \"$targetScenario\" will start a fresh roleplay and clear the current conversation turns.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coachVm.startScenario(targetScenario, userLevel)
                        pendingScenarioSwitch = null
                    }
                ) { Text("Switch Scenario") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingScenarioSwitch = null }) {
                    Text("Keep Practicing")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val chatStreamAndComposer: @Composable (Modifier) -> Unit = { colModifier ->
            Column(
                modifier = colModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                ) {
                    items(turns) { turn ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = bubbleShapeFor(turn.isUser),
                                color = if (turn.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                tonalElevation = if (turn.isUser) 0.dp else 1.dp,
                                border = BorderStroke(
                                    1.dp,
                                    if (turn.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.fillMaxWidth(0.86f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (turn.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (turn.isUser) Icons.Default.Person else Icons.Default.RecordVoiceOver,
                                                    contentDescription = null,
                                                    tint = if (turn.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (turn.isUser) "You" else "AI Partner ($selectedScenario)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (turn.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    if (isPartnerTyping) {
                        item {
                            AnimatedThinkingDots(label = "Partner is responding...")
                        }
                    }

                    if (!tier.isExpandedTablet) {
                        item {
                            when (val debrief = coachDebrief) {
                                is UiState.Loading -> CircularProgressIndicator()
                                is UiState.Error -> ErrorRecoveryCard(debrief.message)
                                is UiState.Success -> {
                                    Card(
                                        shape = MaterialTheme.shapes.large,
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
                }

                if (voiceStatus != null) {
                    Text(voiceStatus.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                            modifier = Modifier
                                .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("conversation_voice_button")
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
                            shape = MaterialTheme.shapes.medium,
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
                            modifier = Modifier
                                .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("conversation_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (!tier.isExpandedTablet) {
                    OutlinedButton(
                        onClick = { coachVm.requestCoachCheckInAndSave(userLevel) },
                        enabled = turns.size >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("conversation_checkin_button")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("End & Get Coach Check-In Feedback")
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth)
                .padding(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(
                title = "Conversation Simulator",
                subtitle = "Adaptive $userLevel dialogue • Natural flow without mid-chat interruptions"
            )
            FeatureGuideStrip(featureKey = "conversation")

            if (tier.isExpandedTablet) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                    verticalAlignment = Alignment.Top
                ) {
                    chatStreamAndComposer(Modifier.weight(1.35f).fillMaxSize())
                    Column(
                        modifier = Modifier.weight(0.65f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Roleplay Scenarios", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(CurriculumCatalog.conversationScenarios) { scenario ->
                                FilterChip(
                                    selected = selectedScenario == scenario,
                                    onClick = {
                                        if (scenario != selectedScenario) {
                                            if (turns.size >= 2) {
                                                pendingScenarioSwitch = scenario
                                            } else {
                                                coachVm.startScenario(scenario, userLevel)
                                            }
                                        }
                                    },
                                    label = { Text(scenario) },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                )
                            }
                            item {
                                when (val debrief = coachDebrief) {
                                    is UiState.Loading -> CircularProgressIndicator()
                                    is UiState.Error -> ErrorRecoveryCard(debrief.message)
                                    is UiState.Success -> {
                                        Card(
                                            shape = MaterialTheme.shapes.large,
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
                        OutlinedButton(
                            onClick = { coachVm.requestCoachCheckInAndSave(userLevel) },
                            enabled = turns.size >= 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("conversation_checkin_button")
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("End & Get Coach Check-In Feedback")
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                    items(CurriculumCatalog.conversationScenarios) { scenario ->
                        FilterChip(
                            selected = selectedScenario == scenario,
                            onClick = {
                                if (scenario != selectedScenario) {
                                    if (turns.size >= 2) {
                                        pendingScenarioSwitch = scenario
                                    } else {
                                        coachVm.startScenario(scenario, userLevel)
                                    }
                                }
                            },
                            label = { Text(scenario) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
                chatStreamAndComposer(Modifier.weight(1f).fillMaxWidth())
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
    val mentorMessages by coachVm.mentorMessages.collectAsStateWithLifecycle()
    val isResponding by coachVm.isMentorResponding.collectAsStateWithLifecycle()
    val mentorError by coachVm.mentorError.collectAsStateWithLifecycle()

    var question by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val quickPrompts = listOf(
        "Create my personalized 7-day improvement plan",
        "Identify my recurring speaking mistakes & @askvinh fixes",
        "Recommend my next @askvinh lesson based on my scores",
        "How do I stop saying 'um' in interviews?"
    )

    if (showClearConfirm) {
        ConfirmDestructiveDialog(
            title = "Clear Mentor Thread?",
            message = "This removes your saved multi-turn AI Mentor chat history from this device.",
            confirmLabel = "Clear Thread",
            onConfirm = {
                coachVm.clearMentorThread()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth)
                .padding(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(R.drawable.img_mentor_avatar),
                            contentDescription = "SpeakCraft AI Mentor emblem",
                            modifier = Modifier.size(50.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Personalized AI Mentor", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Grounded in @askvinh • ${sessions.size} sessions synced",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (mentorMessages.size > 1) {
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("mentor_clear_thread_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
            }

            FeatureGuideStrip(featureKey = "ai_mentor")

            LazyRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                items(quickPrompts) { prompt ->
                    FilterChip(
                        selected = false,
                        enabled = !isResponding,
                        onClick = {
                            coachVm.askMentor(
                                profile = profile,
                                sessions = sessions,
                                feedbackList = feedbackList,
                                question = prompt
                            )
                        },
                        label = { Text(prompt) },
                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(mentorMessages, key = { idx, msg -> "${idx}_${msg.isUser}_${msg.text.hashCode()}" }) { idx, turn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = bubbleShapeFor(turn.isUser),
                            color = if (turn.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (turn.isUser) 0.dp else 1.dp,
                            border = BorderStroke(
                                1.dp,
                                if (turn.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .testTag("mentor_chat_bubble_$idx")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (turn.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (turn.isUser) Icons.Default.Person else Icons.Default.Psychology,
                                                contentDescription = null,
                                                tint = if (turn.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (turn.isUser) "You" else "SpeakCraft AI Mentor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (turn.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(turn.text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                if (isResponding) {
                    item {
                        AnimatedThinkingDots(label = "Reviewing your scores and @askvinh curriculum...")
                    }
                }

                if (mentorError != null) {
                    item {
                        ErrorRecoveryCard(mentorError.orEmpty())
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                ) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Ask your communication mentor...") },
                        shape = MaterialTheme.shapes.medium,
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
                        enabled = !isResponding && question.isNotBlank(),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 54.dp, minHeight = 54.dp)
                            .testTag("mentor_ask_button")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "Ask mentor")
                    }
                }
            }
        }
    }
}
