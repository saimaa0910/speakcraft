package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.ui.components.CloudSyncWarningBanner
import com.example.ui.components.CoachEvaluationStatusCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.LiveTranscriptPanel
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.SpeakingAndLabsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationLabScreen(
    speakingVm: SpeakingAndLabsViewModel,
    ttsHelper: TtsAndSpeechHelper,
    pastSessions: List<SpeakingSession>,
    initialFocus: String = "pronunciation",
    onOpenAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val drills = CurriculumCatalog.pronunciationDrills
    val categories = listOf("All") + drills.map { it.category }.distinct()
    val defaultCat = if (initialFocus == "articulation" && categories.contains("Difficult Sounds")) {
        "Difficult Sounds"
    } else "All"
    var selectedCategory by remember(initialFocus) { mutableStateOf(defaultCat) }
    var activeDrill by remember(initialFocus) {
        mutableStateOf(drills.firstOrNull { defaultCat == "All" || it.category == defaultCat } ?: drills.first())
    }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    val headerTitle = if (initialFocus == "articulation") {
        "Articulation & Enunciation Lab"
    } else {
        "Phonetic & Intonation Lab"
    }
    val headerSubtitle = if (initialFocus == "articulation") {
        "Crisp consonant clarity, tongue placement, linking & enunciation drills"
    } else {
        "Minimal pairs, IPA contrasts, word/sentence stress & downward authority intonation"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)
        val filteredDrills = drills.filter { selectedCategory == "All" || it.category == selectedCategory }

        val activeDrillCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                    Text(
                        "1. Listen carefully -> 2. Record aloud -> 3. Compare with AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${activeDrill.title} • ${activeDrill.targetIpa}", style = MaterialTheme.typography.titleLarge)
                    Text(activeDrill.contrastOrFocus, style = MaterialTheme.typography.labelMedium, color = ext.info)
                    Text("Practice Phrase: \"${activeDrill.practicePhrase}\"", style = MaterialTheme.typography.bodyLarge)
                    Text("Tip: ${activeDrill.tip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        OutlinedButton(
                            onClick = { ttsHelper.speak(activeDrill.practicePhrase, 0.72f) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Listen 0.75x")
                        }
                        OutlinedButton(
                            onClick = { ttsHelper.speak(activeDrill.practicePhrase, 1.0f) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Listen 1.0x")
                        }
                    }

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)
                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = {
                                if (isRecording) speakingVm.stopRecording()
                                else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("pron_record_button")
                        ) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRecording) "Stop" else "Record")
                        }
                        OutlinedButton(
                            onClick = {
                                speakingVm.submitForAiAnalysis(
                                    moduleType = "PRONUNCIATION",
                                    promptTitle = activeDrill.title,
                                    previousSession = pastSessions.firstOrNull(),
                                    extraInstructions = "Compare pronunciation strictly against target phrase: '${activeDrill.practicePhrase}' focusing on ${activeDrill.targetIpa}."
                                )
                            },
                            enabled = !isRecording,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text("Compare AI") }
                    }
                }
            }
        }

        val feedbackSection: @Composable () -> Unit = {
            if (cloudSyncError != null) {
                CloudSyncWarningBanner(cloudSyncError.orEmpty()) { speakingVm.retryCloudSync() }
            }
            if (activeModule == "PRONUNCIATION") {
                when (val s = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(150)
                    is UiState.Error -> {
                        val rep = evaluationReport
                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                            CoachEvaluationStatusCard(
                                report = rep,
                                onRecordAgain = {
                                    speakingVm.recordAgain()
                                    speakingVm.startRecording(null)
                                },
                                onCancel = { speakingVm.cancelEvaluation() }
                            )
                        } else {
                            ErrorRecoveryCard(s.message)
                        }
                    }
                    is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader(headerTitle, headerSubtitle)
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(
                    featureKey = if (initialFocus == "articulation") "articulation_lab" else "pronunciation"
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            activeDrillCard()
                            feedbackSection()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Select Practice Drill (${filteredDrills.size})", style = MaterialTheme.typography.titleMedium)
                            filteredDrills.forEach { drill ->
                                val isSelected = drill.id == activeDrill.id
                                Card(
                                    onClick = { activeDrill = drill; speakingVm.clearAnalysis() },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text("${drill.category}: ${drill.title}", style = MaterialTheme.typography.titleMedium)
                                        Text(drill.practicePhrase, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { activeDrillCard() }
                if (cloudSyncError != null || activeModule == "PRONUNCIATION") {
                    item { Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) { feedbackSection() } }
                }
                items(filteredDrills, key = { it.id }) { drill ->
                    val isSelected = drill.id == activeDrill.id
                    Card(
                        onClick = { activeDrill = drill; speakingVm.clearAnalysis() },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${drill.category}: ${drill.title}", style = MaterialTheme.typography.titleMedium)
                            Text(drill.practicePhrase, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListeningShadowingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    ttsHelper: TtsAndSpeechHelper,
    pastSessions: List<SpeakingSession>,
    onOpenAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val uriHandler = LocalUriHandler.current
    val segments = CurriculumCatalog.shadowingSegments
    var activeSegment by remember { mutableStateOf(segments.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val shadowCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(activeSegment.title, style = MaterialTheme.typography.titleLarge)
                    Text("Focus: ${activeSegment.focusSkill} • Target ${activeSegment.pacingTargetWpm} WPM", style = MaterialTheme.typography.labelMedium, color = ext.info)
                    Text(activeSegment.scriptWithMarkers, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        OutlinedButton(
                            onClick = { ttsHelper.speak(activeSegment.plainScript, 0.92f) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Play Reference")
                        }
                        if (activeSegment.sourceVideoUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { uriHandler.openUri(activeSegment.sourceVideoUrl) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            ) {
                                Text("Watch Original Lesson")
                            }
                        }
                    }

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)
                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = {
                                if (isRecording) speakingVm.stopRecording()
                                else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text(if (isRecording) "Stop Shadowing" else "Shadow & Repeat") }
                        OutlinedButton(
                            onClick = {
                                speakingVm.submitForAiAnalysis(
                                    moduleType = "SHADOWING",
                                    promptTitle = activeSegment.title,
                                    previousSession = pastSessions.firstOrNull(),
                                    extraInstructions = "Compare timing, WPM (target ${activeSegment.pacingTargetWpm}), pause placement, and pronunciation against reference transcript: '${activeSegment.plainScript}'."
                                )
                            },
                            enabled = !isRecording,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text("Score Shadow") }
                    }
                }
            }
        }

        val feedbackSection: @Composable () -> Unit = {
            if (cloudSyncError != null) {
                CloudSyncWarningBanner(cloudSyncError.orEmpty()) { speakingVm.retryCloudSync() }
            }
            if (activeModule == "SHADOWING") {
                when (val s = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(150)
                    is UiState.Error -> {
                        val rep = evaluationReport
                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                            CoachEvaluationStatusCard(
                                report = rep,
                                onRecordAgain = {
                                    speakingVm.recordAgain()
                                    speakingVm.startRecording(null)
                                },
                                onCancel = { speakingVm.cancelEvaluation() }
                            )
                        } else {
                            ErrorRecoveryCard(s.message)
                        }
                    }
                    is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader("Active Listening & Shadowing", "4 Ways of Listening + match natural pacing & silent pauses")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "listening_lab")
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            shadowCard()
                            feedbackSection()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Shadowing Segments", style = MaterialTheme.typography.titleMedium)
                            segments.forEach { seg ->
                                val isSelected = seg.id == activeSegment.id
                                Card(
                                    onClick = { activeSegment = seg; speakingVm.clearAnalysis() },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(seg.title, style = MaterialTheme.typography.titleMedium)
                                        Text(seg.sourceVideoTitle, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { shadowCard() }
                if (cloudSyncError != null || activeModule == "SHADOWING") {
                    item { Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) { feedbackSection() } }
                }
                items(segments, key = { it.id }) { seg ->
                    val isSelected = seg.id == activeSegment.id
                    Card(
                        onClick = { activeSegment = seg; speakingVm.clearAnalysis() },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(seg.title, style = MaterialTheme.typography.titleMedium)
                            Text(seg.sourceVideoTitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarSpeakingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>,
    onOpenAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val lessons = CurriculumCatalog.grammarLessons
    var activeLesson by remember { mutableStateOf(lessons.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val grammarCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                    Text(activeLesson.topic, style = MaterialTheme.typography.titleLarge)
                    Text(activeLesson.ruleSummary, style = MaterialTheme.typography.bodyLarge)
                    Text("Common Mistake: \"${activeLesson.commonConversationalMistake}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Text("Natural Fix: \"${activeLesson.naturalCorrection}\"", style = MaterialTheme.typography.bodySmall, color = ext.success)
                    Text("Speaking Drill: ${activeLesson.speakingPrompt}", style = MaterialTheme.typography.bodyMedium)

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)
                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = {
                                if (isRecording) speakingVm.stopRecording()
                                else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text(if (isRecording) "Stop" else "Speak Drill") }
                        OutlinedButton(
                            onClick = {
                                speakingVm.submitForAiAnalysis(
                                    moduleType = "GRAMMAR_SPEAKING",
                                    promptTitle = activeLesson.topic,
                                    previousSession = pastSessions.firstOrNull(),
                                    extraInstructions = "Evaluate grammatical accuracy and thought organization for: ${activeLesson.topic}. Rule: ${activeLesson.ruleSummary}"
                                )
                            },
                            enabled = !isRecording,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text("Check Grammar") }
                    }
                }
            }
        }

        val feedbackSection: @Composable () -> Unit = {
            if (cloudSyncError != null) {
                CloudSyncWarningBanner(cloudSyncError.orEmpty()) { speakingVm.retryCloudSync() }
            }
            if (activeModule == "GRAMMAR_SPEAKING") {
                when (val s = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(150)
                    is UiState.Error -> {
                        val rep = evaluationReport
                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                            CoachEvaluationStatusCard(
                                report = rep,
                                onRecordAgain = {
                                    speakingVm.recordAgain()
                                    speakingVm.startRecording(null)
                                },
                                onCancel = { speakingVm.cancelEvaluation() }
                            )
                        } else {
                            ErrorRecoveryCard(s.message)
                        }
                    }
                    is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader("Thinking & Articulation / Grammar", "Structure spontaneous thoughts cleanly via PREP, CLEAR & spoken grammar")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "grammar_speaking")
            }
            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            grammarCard()
                            feedbackSection()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Thinking & Grammar Frameworks", style = MaterialTheme.typography.titleMedium)
                            lessons.forEach { lesson ->
                                val isSelected = lesson.id == activeLesson.id
                                Card(
                                    onClick = { activeLesson = lesson; speakingVm.clearAnalysis() },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(lesson.topic, style = MaterialTheme.typography.titleMedium)
                                        Text(lesson.ruleSummary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { grammarCard() }
                if (cloudSyncError != null || activeModule == "GRAMMAR_SPEAKING") {
                    item { Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) { feedbackSection() } }
                }
                items(lessons, key = { it.id }) { lesson ->
                    val isSelected = lesson.id == activeLesson.id
                    Card(
                        onClick = { activeLesson = lesson; speakingVm.clearAnalysis() },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(lesson.topic, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.ruleSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicSpeakingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>,
    onOpenAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val modules = CurriculumCatalog.publicSpeakingModules
    var activeModule by remember { mutableStateOf(modules.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeAnalysisModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val rehearsalCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                    Text(activeModule.title, style = MaterialTheme.typography.titleLarge)
                    Text("Framework: ${activeModule.creatorFramework}", style = MaterialTheme.typography.labelMedium, color = ext.info)
                    Text(activeModule.keyPrinciple, style = MaterialTheme.typography.bodyLarge)
                    Text("Simulated Assignment: ${activeModule.rehearsalPrompt}", style = MaterialTheme.typography.bodyMedium)

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)
                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = {
                                if (isRecording) speakingVm.stopRecording()
                                else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text(if (isRecording) "Stop Delivery" else "Record Rehearsal") }
                        OutlinedButton(
                            onClick = {
                                speakingVm.submitForAiAnalysis(
                                    moduleType = "PRESENTATION",
                                    promptTitle = activeModule.title,
                                    previousSession = pastSessions.firstOrNull(),
                                    extraInstructions = "Evaluate presentation delivery, opening hook, vocal modulation, and structure using ${activeModule.creatorFramework}."
                                )
                            },
                            enabled = !isRecording,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) { Text("Get Presentation AI") }
                    }
                }
            }
        }

        val feedbackSection: @Composable () -> Unit = {
            if (cloudSyncError != null) {
                CloudSyncWarningBanner(cloudSyncError.orEmpty()) { speakingVm.retryCloudSync() }
            }
            if (activeAnalysisModule == "PRESENTATION") {
                when (val s = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(150)
                    is UiState.Error -> {
                        val rep = evaluationReport
                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                            CoachEvaluationStatusCard(
                                report = rep,
                                onRecordAgain = {
                                    speakingVm.recordAgain()
                                    speakingVm.startRecording(null)
                                },
                                onCancel = { speakingVm.cancelEvaluation() }
                            )
                        } else {
                            ErrorRecoveryCard(s.message)
                        }
                    }
                    is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader("Public Speaking & Presentation Training", "Hooks, PREP structure, vocal modulation & stage closing")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "public_speaking")
            }
            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            rehearsalCard()
                            feedbackSection()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Presentation Modules", style = MaterialTheme.typography.titleMedium)
                            modules.forEach { mod ->
                                val isSelected = mod.id == activeModule.id
                                Card(
                                    onClick = { activeModule = mod; speakingVm.clearAnalysis() },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(mod.title, style = MaterialTheme.typography.titleMedium)
                                        Text(mod.keyPrinciple, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { rehearsalCard() }
                if (cloudSyncError != null || activeAnalysisModule == "PRESENTATION") {
                    item { Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) { feedbackSection() } }
                }
                items(modules, key = { it.id }) { mod ->
                    val isSelected = mod.id == activeModule.id
                    Card(
                        onClick = { activeModule = mod; speakingVm.clearAnalysis() },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(mod.title, style = MaterialTheme.typography.titleMedium)
                            Text(mod.keyPrinciple, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealWorldTasksScreen(
    records: List<RealWorldTaskRecord>,
    onSaveTask: (taskId: String, title: String, completed: Boolean, reflection: String) -> Unit
) {
    val seeds = CurriculumCatalog.realWorldTaskSeeds
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader("Real-World Communication Tasks", "Apply speaking techniques outside the app and log your reflection")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "real_world_tasks")
            }
            if (tier.isExpandedTablet) {
                items(seeds.indices.chunked(2), key = { row -> row.joinToString("_") }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md),
                        verticalAlignment = Alignment.Top
                    ) {
                        pair.forEach { idx ->
                            val taskTitle = seeds[idx]
                            val taskId = "rw_task_$idx"
                            val saved = records.firstOrNull { it.id == taskId || it.taskTitle == taskTitle }
                            RealWorldTaskCard(
                                taskId = taskId,
                                taskTitle = taskTitle,
                                saved = saved,
                                onSaveTask = onSaveTask,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(seeds.indices.toList()) { idx ->
                    val taskTitle = seeds[idx]
                    val taskId = "rw_task_$idx"
                    val saved = records.firstOrNull { it.id == taskId || it.taskTitle == taskTitle }
                    RealWorldTaskCard(
                        taskId = taskId,
                        taskTitle = taskTitle,
                        saved = saved,
                        onSaveTask = onSaveTask,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RealWorldTaskCard(
    taskId: String,
    taskTitle: String,
    saved: RealWorldTaskRecord?,
    onSaveTask: (taskId: String, title: String, completed: Boolean, reflection: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    var completed by remember(saved?.completed) { mutableStateOf(saved?.completed ?: false) }
    var reflection by remember(saved?.reflection) { mutableStateOf(saved?.reflection.orEmpty()) }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (completed) ext.success.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (completed) 1.5.dp else 1.dp,
            if (completed) ext.success.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = completed,
                    onCheckedChange = {
                        completed = it
                        onSaveTask(taskId, taskTitle, it, reflection)
                    }
                )
                Text(taskTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (completed) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = ext.success)
                }
            }
            OutlinedTextField(
                value = reflection,
                onValueChange = { reflection = it },
                label = { Text("What happened? What would you refine next time?") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { onSaveTask(taskId, taskTitle, completed, reflection) },
                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Save Reflection")
            }
        }
    }
}
