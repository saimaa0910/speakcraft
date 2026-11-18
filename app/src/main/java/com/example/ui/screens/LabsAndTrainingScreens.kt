package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.SpeakingAndLabsViewModel

@Composable
fun PronunciationLabScreen(
    speakingVm: SpeakingAndLabsViewModel,
    ttsHelper: TtsAndSpeechHelper,
    pastSessions: List<SpeakingSession>
) {
    val context = LocalContext.current
    val drills = CurriculumCatalog.pronunciationDrills
    val categories = listOf("All") + drills.map { it.category }.distinct()
    var selectedCategory by remember { mutableStateOf("All") }
    var activeDrill by remember { mutableStateOf(drills.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Articulation & Pronunciation Lab", "Minimal pairs, IPA sounds, word/sentence stress, intonation & linking")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${activeDrill.title} • ${activeDrill.targetIpa}", style = MaterialTheme.typography.titleLarge)
                        Text(activeDrill.contrastOrFocus, style = MaterialTheme.typography.labelMedium, color = AcousticTeal)
                        Text("Practice Phrase: \"${activeDrill.practicePhrase}\"", style = MaterialTheme.typography.bodyLarge)
                        Text("Tip: ${activeDrill.tip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { ttsHelper.speak(activeDrill.practicePhrase, 0.72f) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Listen 0.75x")
                            }
                            OutlinedButton(onClick = { ttsHelper.speak(activeDrill.practicePhrase, 1.0f) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Listen 1.0x")
                            }
                        }

                        LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) speakingVm.stopRecording()
                                    else {
                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("pron_record_button")
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
                                enabled = !isRecording && recordedPath != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Compare AI") }
                        }
                    }
                }
            }

            if (activeModule == "PRONUNCIATION") {
                item {
                    when (val s = analysisState) {
                        is UiState.Loading -> ShimmerSkeletonCard(150)
                        is UiState.Error -> ErrorRecoveryCard(s.message)
                        is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull())
                        null -> Unit
                    }
                }
            }

            items(drills.filter { selectedCategory == "All" || it.category == selectedCategory }, key = { it.id }) { drill ->
                Card(
                    onClick = { activeDrill = drill; speakingVm.clearAnalysis() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

@Composable
fun ListeningShadowingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    ttsHelper: TtsAndSpeechHelper,
    pastSessions: List<SpeakingSession>
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val segments = CurriculumCatalog.shadowingSegments
    var activeSegment by remember { mutableStateOf(segments.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Active Listening & Shadowing", "4 Ways of Listening + match @askvinh pacing & silent pauses")
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(activeSegment.title, style = MaterialTheme.typography.titleLarge)
                        Text("Focus: ${activeSegment.focusSkill} • Target ${activeSegment.pacingTargetWpm} WPM", style = MaterialTheme.typography.labelMedium, color = AcousticTeal)
                        Text(activeSegment.scriptWithMarkers, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { ttsHelper.speak(activeSegment.plainScript, 0.92f) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Play Reference")
                            }
                            OutlinedButton(onClick = { uriHandler.openUri(activeSegment.sourceVideoUrl) }, modifier = Modifier.weight(1f)) {
                                Text("Watch @askvinh")
                            }
                        }

                        LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) speakingVm.stopRecording()
                                    else {
                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f)
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
                                enabled = !isRecording && recordedPath != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Score Shadow") }
                        }
                    }
                }
            }

            if (activeModule == "SHADOWING") {
                item {
                    when (val s = analysisState) {
                        is UiState.Loading -> ShimmerSkeletonCard(150)
                        is UiState.Error -> ErrorRecoveryCard(s.message)
                        is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull())
                        null -> Unit
                    }
                }
            }

            items(segments, key = { it.id }) { seg ->
                Card(
                    onClick = { activeSegment = seg; speakingVm.clearAnalysis() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

@Composable
fun GrammarSpeakingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>
) {
    val context = LocalContext.current
    val lessons = CurriculumCatalog.grammarLessons
    var activeLesson by remember { mutableStateOf(lessons.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Thinking & Articulation / Grammar", "Structure spontaneous thoughts cleanly via PREP, CLEAR & spoken grammar")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(activeLesson.topic, style = MaterialTheme.typography.titleLarge)
                        Text(activeLesson.ruleSummary, style = MaterialTheme.typography.bodyLarge)
                        Text("Common Mistake: \"${activeLesson.commonConversationalMistake}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text("Natural Fix: \"${activeLesson.naturalCorrection}\"", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                        Text("Speaking Drill: ${activeLesson.speakingPrompt}", style = MaterialTheme.typography.bodyMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) speakingVm.stopRecording()
                                    else {
                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f)
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
                                enabled = !isRecording && recordedPath != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Check Grammar") }
                        }
                    }
                }
            }
            if (activeModule == "GRAMMAR_SPEAKING") {
                item {
                    when (val s = analysisState) {
                        is UiState.Loading -> ShimmerSkeletonCard(150)
                        is UiState.Error -> ErrorRecoveryCard(s.message)
                        is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull())
                        null -> Unit
                    }
                }
            }
            items(lessons, key = { it.id }) { lesson ->
                Card(
                    onClick = { activeLesson = lesson; speakingVm.clearAnalysis() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

@Composable
fun PublicSpeakingScreen(
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>
) {
    val context = LocalContext.current
    val modules = CurriculumCatalog.publicSpeakingModules
    var activeModule by remember { mutableStateOf(modules.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val activeAnalysisModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Public Speaking & Presentation Training", "Hooks, PREP structure, vocal modulation & stage closing")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(activeModule.title, style = MaterialTheme.typography.titleLarge)
                        Text("Framework: ${activeModule.creatorFramework}", style = MaterialTheme.typography.labelMedium, color = AcousticTeal)
                        Text(activeModule.keyPrinciple, style = MaterialTheme.typography.bodyLarge)
                        Text("Simulated Assignment: ${activeModule.rehearsalPrompt}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) speakingVm.stopRecording()
                                    else {
                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f)
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
                                enabled = !isRecording && recordedPath != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("Get Presentation AI") }
                        }
                    }
                }
            }
            if (activeAnalysisModule == "PRESENTATION") {
                item {
                    when (val s = analysisState) {
                        is UiState.Loading -> ShimmerSkeletonCard(150)
                        is UiState.Error -> ErrorRecoveryCard(s.message)
                        is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull())
                        null -> Unit
                    }
                }
            }
            items(modules, key = { it.id }) { mod ->
                Card(
                    onClick = { activeModule = mod; speakingVm.clearAnalysis() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

@Composable
fun RealWorldTasksScreen(
    records: List<RealWorldTaskRecord>,
    onSaveTask: (taskId: String, title: String, completed: Boolean, reflection: String) -> Unit
) {
    val seeds = CurriculumCatalog.realWorldTaskSeeds
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("Real-World Communication Tasks", "Apply @askvinh techniques outside the app and log your reflection")
            }
            items(seeds.indices.toList()) { idx ->
                val taskTitle = seeds[idx]
                val taskId = "rw_task_$idx"
                val saved = records.firstOrNull { it.id == taskId || it.taskTitle == taskTitle }
                var completed by remember(saved?.completed) { mutableStateOf(saved?.completed ?: false) }
                var reflection by remember(saved?.reflection) { mutableStateOf(saved?.reflection.orEmpty()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = EmeraldSuccess)
                            }
                        }
                        OutlinedTextField(
                            value = reflection,
                            onValueChange = { reflection = it },
                            label = { Text("What happened? What would you refine next time?") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = { onSaveTask(taskId, taskTitle, completed, reflection) }) {
                            Text("Save Reflection")
                        }
                    }
                }
            }
        }
    }
}
