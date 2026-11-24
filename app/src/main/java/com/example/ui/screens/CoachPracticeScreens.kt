package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speech.android.PracticeCameraHost
import com.example.speech.content.PracticeActivity
import com.example.speech.content.PracticeCatalog
import com.example.speech.content.PracticeTechnique
import com.example.speech.feedback.CoachTip
import com.example.speech.feedback.SelfCheckItem
import com.example.speech.metrics.MetricMethodology
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.recognition.RecognitionError
import com.example.speech.session.PracticeResult
import com.example.speech.tier.SpeechTier
import com.example.speech.video.VideoEvaluation
import com.example.ui.components.TranscriptDisplayCard
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.CoachPracticeUiState
import com.example.ui.viewmodel.CoachPracticeViewModel
import com.example.ui.viewmodel.PracticeSessionState
import java.io.File
import java.util.Locale

/**
 * Coach practice (EP-08 slice): choose an activity, speak, and get feedback from the local intelligence engine on
 * the phone — strengths, the one thing to work on, mistakes with evidence, coach tips, transcript highlights,
 * body-language checks (camera, optional) and local replay. Nothing is uploaded.
 */
@Composable
fun CoachPracticeRoute(
    vm: CoachPracticeViewModel,
    cameraHost: PracticeCameraHost?,
    onSpeak: (String) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val activity = state.activity
    val technique = state.selectedTechniqueId?.let { state.catalog?.technique(it) }

    when {
        activity != null -> {
            BackHandler { vm.closeActivity() }
            when (val session = state.session) {
                is PracticeSessionState.Done -> PracticeResultScreen(
                    state = state,
                    activity = activity,
                    result = session.result,
                    onAnswerSelfCheck = vm::answerSelfCheck,
                    onTryAgain = vm::tryAgain,
                    onNext = state.catalog?.nextAfter(activity)?.let { next -> { vm.openActivity(next) } },
                    onBack = vm::closeActivity
                )
                else -> PracticeSessionScreen(state = state, activity = activity, vm = vm, cameraHost = cameraHost, onSpeak = onSpeak)
            }
        }
        technique != null -> {
            BackHandler { vm.openTechnique(null) }
            TechniqueScreen(
                technique = technique,
                activities = state.catalog?.activitiesFor(technique.id).orEmpty(),
                onOpenActivity = vm::openActivity,
                onBack = { vm.openTechnique(null) }
            )
        }
        else -> CoachPracticeHome(state = state, onOpenActivity = vm::openActivity, onOpenTechnique = { vm.openTechnique(it) })
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------------------------------------------

@Composable
fun CoachPracticeHome(
    state: CoachPracticeUiState,
    onOpenActivity: (PracticeActivity) -> Unit,
    onOpenTechnique: (String) -> Unit
) {
    val catalog = state.catalog
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("coach_practice_home"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Practise and get instant feedback", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Speak for a minute and your phone analyses your pace, pauses, filler words, structure and more. " +
                            "Recordings and results stay on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.tier?.let { TierLine(it) }
                }
            }
        }
        if (catalog == null) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            return@LazyColumn
        }
        item { SectionTitle("Quick practice") }
        items(catalog.quickPractice, key = { it.id }) { a -> ActivityRow(a, onClick = { onOpenActivity(a) }) }
        if (catalog.techniques.isNotEmpty()) {
            item { SectionTitle("Techniques") }
            items(catalog.techniques, key = { it.id }) { t ->
                Card(onClick = { onOpenTechnique(t.id) }, modifier = Modifier.fillMaxWidth().testTag("technique_${t.id}")) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (t.pendingReview) PendingReviewBadge()
                        }
                        Text(t.summary, style = MaterialTheme.typography.bodySmall)
                        Text("${catalog.activitiesFor(t.id).size} activities", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TechniqueScreen(
    technique: PracticeTechnique,
    activities: List<PracticeActivity>,
    onOpenActivity: (PracticeActivity) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("technique_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackRow(onBack) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(technique.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (technique.pendingReview) PendingReviewBadge()
            }
        }
        item { Text(technique.summary, style = MaterialTheme.typography.bodyLarge) }
        if (technique.whyItWorks.isNotBlank()) {
            item {
                InfoCard(title = "Why it works") { Text(technique.whyItWorks, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (technique.steps.isNotEmpty()) {
            item {
                InfoCard(title = "How to do it") {
                    technique.steps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        item { SectionTitle("Practise it") }
        items(activities, key = { it.id }) { a -> ActivityRow(a, onClick = { onOpenActivity(a) }) }
    }
}

@Composable
private fun ActivityRow(activity: PracticeActivity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("activity_${activity.id}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(activity.goal, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${templateLabel(activity.templateId)} · up to ${formatSeconds(activity.maxSeconds * 1000L)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (activity.pendingReview) PendingReviewBadge()
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Session
// ---------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeSessionScreen(
    state: CoachPracticeUiState,
    activity: PracticeActivity,
    vm: CoachPracticeViewModel,
    cameraHost: PracticeCameraHost?,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val session = state.session
    val recording = session as? PracticeSessionState.Recording
    val busy = recording != null || session is PracticeSessionState.Evaluating

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setCameraOn(granted)
    }
    fun hasPermission(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("practice_session"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { if (!busy) BackRow(vm::closeActivity) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(activity.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (activity.pendingReview) PendingReviewBadge()
            }
        }
        item {
            InfoCard(title = activity.goal.ifBlank { "Your task" }) {
                Text(activity.prompt, style = MaterialTheme.typography.bodyLarge)
                activity.framework?.let { fw ->
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        frameworkSteps(fw).forEach { step -> AssistChip(onClick = {}, label = { Text(step) }) }
                    }
                }
                if (activity.targetMinWords != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Aim for about ${activity.targetMinWords}–${activity.targetMaxWords ?: activity.targetMinWords + 60} words.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        activity.referenceText?.let { text ->
            item {
                InfoCard(title = if (activity.templateId == "TPL-REPEAT") "Listen, then repeat" else "Read this aloud") {
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                    if (activity.templateId == "TPL-REPEAT") {
                        TextButton(onClick = { onSpeak(text) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play the model")
                        }
                    }
                }
            }
        }
        if (!busy) {
            item {
                OptionRow(
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    title = "Body language with camera",
                    subtitle = "Checks framing, eye line and posture on the phone. Optional.",
                    checked = state.cameraOn,
                    enabled = cameraHost != null,
                    onChange = { on ->
                        if (on && !hasPermission(Manifest.permission.CAMERA)) cameraLauncher.launch(Manifest.permission.CAMERA)
                        else vm.setCameraOn(on)
                    }
                )
            }
            if (state.tier == SpeechTier.T3B_AUDIO_ONLY || state.allowOnline) {
                item {
                    OptionRow(
                        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null) },
                        title = "Online transcription",
                        subtitle = "This phone has no offline English recogniser. With this on, your speech is sent to your phone's " +
                            "speech service to get words; without it you get audio-only feedback.",
                        checked = state.allowOnline,
                        enabled = true,
                        onChange = vm::setAllowOnline
                    )
                }
            }
        }
        if (state.cameraOn && cameraHost != null && session !is PracticeSessionState.Evaluating) {
            item { CameraPreview(cameraHost, onReady = vm::onCameraReady) }
        }
        item { state.tier?.let { TierLine(it) } }
        item {
            when (session) {
                is PracticeSessionState.Recording -> RecordingControls(session, onStop = vm::stopRecording, onCancel = vm::cancelRecording)
                PracticeSessionState.Evaluating -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Analysing on your phone…", style = MaterialTheme.typography.titleMedium)
                }
                is PracticeSessionState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(session.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = vm::tryAgain) { Text("Try again") }
                }
                else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    activity.prepSeconds?.let {
                        Text("Take up to ${it}s to think, then press record.", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    FilledIconButton(
                        onClick = {
                            if (hasPermission(Manifest.permission.RECORD_AUDIO)) vm.startRecording()
                            else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.size(84.dp).testTag("record_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Icon(Icons.Default.Mic, contentDescription = "Start recording", modifier = Modifier.size(40.dp)) }
                    Spacer(Modifier.height(6.dp))
                    Text("Tap to record (up to ${formatSeconds(activity.maxSeconds * 1000L)})", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RecordingControls(rec: PracticeSessionState.Recording, onStop: () -> Unit, onCancel: () -> Unit) {
    val ext = MaterialTheme.speakCraftColors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${formatSeconds(rec.elapsedMs)} / ${formatSeconds(rec.limitMs)}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("recording_timer")
        )
        if (rec.nearLimit) {
            Text("About ${((rec.limitMs - rec.elapsedMs).coerceAtLeast(0) / 1000)} seconds left — start wrapping up.", color = ext.warning)
        }
        Spacer(Modifier.height(12.dp))
        FilledIconButton(
            onClick = onStop,
            modifier = Modifier.size(84.dp).testTag("stop_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ext.recordingPulse)
        ) { Icon(Icons.Default.Stop, contentDescription = "Stop and get feedback", modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(6.dp))
        Text("Tap to finish and get feedback", style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = onCancel) { Text("Discard this attempt") }
    }
}

@Composable
private fun CameraPreview(cameraHost: PracticeCameraHost, onReady: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var status by remember { mutableStateOf("Starting camera…") }
    LaunchedEffect(cameraHost) {
        val ok = cameraHost.bindCamera(lifecycleOwner, previewView.surfaceProvider)
        status = if (ok) "" else "The camera could not start on this phone."
        onReady(ok)
    }
    DisposableEffect(cameraHost) { onDispose { cameraHost.unbindCamera() } }
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (status.isNotEmpty()) Text(status, modifier = Modifier.align(Alignment.Center).padding(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Result
// ---------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeResultScreen(
    state: CoachPracticeUiState,
    activity: PracticeActivity,
    result: PracticeResult,
    onAnswerSelfCheck: (String, String) -> Unit,
    onTryAgain: () -> Unit,
    onNext: (() -> Unit)?,
    onBack: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val evaluation = result.outcome.evaluation
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("practice_result"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackRow(onBack) }
        item { Text(activity.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (result.switchedToCompatibilityMode) {
            item {
                Banner("This phone's recogniser did not accept our audio stream, so we switched to compatibility mode. Please record again.")
            }
        }
        result.outcome.recognitionError?.let { err -> item { Banner(recognitionAdvice(err)) } }

        when (evaluation) {
            is AttemptEvaluationResult.Inconclusive -> {
                item {
                    InfoCard(title = "We couldn't evaluate this attempt") {
                        Text(evaluation.reason.userMessage, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(evaluation.advice.advice, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            is AttemptEvaluationResult.Valid -> {
                val fb = evaluation.feedback
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = ext.successContainer), modifier = Modifier.testTag("strength_card")) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ext.success)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(fb.strength.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(fb.strength.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (evaluation.headlineMetrics.isNotEmpty()) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            evaluation.headlineMetrics.forEach { m ->
                                MetricTile(m.key.displayName, m.displayValue, m.band, m.key.methodology)
                            }
                        }
                    }
                }
                item {
                    InfoCard(title = "Focus for next time") {
                        Text(fb.focus.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(fb.focus.advice, style = MaterialTheme.typography.bodyMedium)
                        fb.focus.evidenceText?.let { Text("What we heard: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (evaluation.issues.isNotEmpty()) {
                    item { SectionTitle("What to improve") }
                    items(evaluation.issues) { issue ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(issue.code.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    LabelChip(issue.code.methodology)
                                }
                                if (issue.evidenceText.isNotBlank()) Text("Evidence: ${issue.evidenceText}", style = MaterialTheme.typography.bodySmall)
                                Text(issue.suggestion, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (fb.tips.isNotEmpty()) {
                    item { SectionTitle("Coach tips") }
                    items(fb.tips, key = { it.tipId }) { tip -> TipCard(tip) }
                }
                if (evaluation.transcriptText.isNotBlank()) {
                    item {
                        TranscriptDisplayCard(
                            transcript = evaluation.transcriptText,
                            insights = evaluation.transcriptInsights,
                            summary = fb.transcriptInsightSummary
                        )
                    }
                } else if (result.tier == SpeechTier.T3B_AUDIO_ONLY) {
                    item {
                        InfoCard(title = "Audio-only feedback") {
                            Text("This phone has no offline recogniser, so feedback covers timing, pauses and loudness — not words.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                (evaluation.video as? VideoEvaluation.Valid)?.let { video ->
                    item {
                        InfoCard(title = "Body language (estimated)") {
                            video.metrics.values.forEach { v ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(v.key.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text("${v.displayValue} · ${v.band}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                val selfChecks = (state.catalog?.selfChecksFor(activity).orEmpty() + fb.selfCheckItems).distinctBy { it.id }
                if (selfChecks.isNotEmpty()) {
                    item { SectionTitle("Check yourself") }
                    items(selfChecks, key = { it.id }) { item -> SelfCheckRow(item, state.selfCheckAnswers[item.id], onAnswerSelfCheck) }
                }
            }
        }
        result.replayFile?.takeIf { it.exists() }?.let { file ->
            item { ReplayCard(file, result.replayHasVideo) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTryAgain, modifier = Modifier.testTag("try_again")) { Text("Try again") }
                onNext?.let { OutlinedButton(onClick = it) { Text("Next activity") } }
            }
        }
        item {
            Text(
                "Measured = counted from your audio. Estimated = computed by on-device rules and may be wrong. " +
                    "Evaluation version ${evaluation.evaluationVersion}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipCard(tip: CoachTip) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (tip.isFocusTip) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(tip.text, style = MaterialTheme.typography.bodyMedium)
                tip.sourceVideoUrl?.let { url ->
                    // Outbound link to the specific source lesson (ARC-03 §6.4): opens the browser or video app, no embedding.
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }) {
                        Text("Watch the original lesson")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfCheckRow(item: SelfCheckItem, answer: String?, onAnswer: (String, String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.text, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("YES" to "Yes", "PARTLY" to "Partly", "NO" to "Not yet").forEach { (code, label) ->
                    FilterChip(selected = answer == code, onClick = { onAnswer(item.id, code) }, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
private fun ReplayCard(file: File, hasVideo: Boolean) {
    InfoCard(title = if (hasVideo) "Watch your attempt" else "Listen to your attempt") {
        if (hasVideo) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(file))
                        setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                        setOnPreparedListener { it.isLooping = false; seekTo(1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
            )
        } else {
            AudioPlayer(file)
        }
    }
}

@Composable
private fun AudioPlayer(file: File) {
    var playing by remember { mutableStateOf(false) }
    val player = remember(file) { runCatching { MediaPlayer().apply { setDataSource(file.absolutePath); prepare() } }.getOrNull() }
    DisposableEffect(player) { onDispose { player?.release() } }
    if (player == null) {
        Text("This recording can't be played on this phone.")
        return
    }
    DisposableEffect(player) {
        player.setOnCompletionListener { playing = false }
        onDispose { }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = {
            if (playing) { player.pause(); playing = false } else { player.start(); playing = true }
        }) { Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = if (playing) "Pause" else "Play") }
        Spacer(Modifier.width(10.dp))
        Text(formatSeconds(player.duration.toLong()), style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Small pieces
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun MetricTile(name: String, value: String, band: String, methodology: MetricMethodology) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(name, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$band · ${methodologyLabel(methodology)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LabelChip(methodology: MetricMethodology) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(methodologyLabel(methodology), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun PendingReviewBadge() {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text("Pending review", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun TierLine(tier: SpeechTier) {
    val text = when (tier) {
        SpeechTier.T1_FULL_ON_DEVICE -> "On-device transcription · replay · voice analysis"
        SpeechTier.T2_ON_DEVICE_MIC -> "On-device transcription (no audio replay on this phone)"
        SpeechTier.T3A_ONLINE_CONSENTED -> "Online transcription (you agreed) · no audio replay"
        SpeechTier.T3B_AUDIO_ONLY -> "Audio-only feedback (no offline recogniser on this phone)"
    }
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("tier_line"))
}

@Composable
private fun OptionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun Banner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WarningAmber, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
}

private fun methodologyLabel(m: MetricMethodology) = if (m == MetricMethodology.MEASURED) "Measured" else "Estimated"

private fun templateLabel(templateId: String) = when (templateId) {
    "TPL-READ" -> "Read aloud"
    "TPL-REPEAT" -> "Listen & repeat"
    "TPL-STRUCT" -> "Structured answer"
    "TPL-STORY" -> "Story"
    else -> "Answer"
}

private fun frameworkSteps(framework: String) = when (framework.uppercase(Locale.ROOT)) {
    "STAR" -> listOf("Situation", "Task", "Action", "Result")
    "PREP" -> listOf("Point", "Reason", "Example", "Point again")
    else -> emptyList()
}

private fun recognitionAdvice(error: RecognitionError): String = when (error) {
    RecognitionError.LanguageUnavailable -> "The offline English speech pack isn't installed. Install it in your phone's speech settings for word-level feedback."
    RecognitionError.Network -> "Online transcription needs a connection. Your timing and pause feedback is still shown."
    RecognitionError.Permission -> "Microphone permission was turned off during recording."
    RecognitionError.Busy -> "The phone's recogniser was busy. Try again in a moment."
    else -> "Speech recognition had a problem (${error.message}). Feedback uses what could be measured."
}

internal fun formatSeconds(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
}
