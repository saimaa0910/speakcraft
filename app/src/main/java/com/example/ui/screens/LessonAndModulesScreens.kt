package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.evaluation.LabPracticeTarget
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.Lesson
import com.example.data.model.LessonDefinition
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.data.model.UserLearningProgress
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.LessonAttributionBadge
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import kotlin.math.roundToInt

@Composable
fun ModuleLessonsScreen(
    moduleSlug: String,
    sharedLessons: List<Lesson>,
    learningProgress: List<UserLearningProgress>,
    onOpenLesson: (String) -> Unit
) {
    val moduleMeta = CurriculumCatalog.contentModules.firstOrNull { it.slug == moduleSlug }
    val localLessons = remember(moduleSlug, sharedLessons) {
        val builtIn = CurriculumCatalog.lessons.filter { it.moduleSlug == moduleSlug }
        val fromCloud = sharedLessons.filter { it.moduleSlug == moduleSlug }.map { l ->
            LessonDefinition(
                id = l.id,
                moduleSlug = l.moduleSlug,
                title = l.title,
                objective = l.objective,
                sourceVideoTitle = "Curated Lesson",
                sourceVideoUrl = l.sourceVideoUrl,
                timestampLabel = l.timestampLabel,
                explanation = l.explanation,
                instructions = l.instructions,
                example = l.example,
                practicePrompt = l.practicePrompt,
                nextLessonId = l.nextLessonId.ifBlank { null },
                isCreatorDerived = l.isCreatorDerived,
                sourceVerified = true
            )
        }
        (builtIn + fromCloud).distinctBy { it.id }
    }
    val pendingSeeds = remember(moduleSlug) {
        CurriculumCatalog.curatedTechniqueCatalog.filter {
            it.moduleSlug == moduleSlug && it.verificationStatus == "PENDING_TRANSCRIPT"
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = moduleMeta?.title ?: "Module Lessons",
                    subtitle = moduleMeta?.subtitle ?: "Structured communication curriculum"
                )
                Spacer(Modifier.height(8.dp))
                FeatureGuideStrip(featureKey = "module_lessons")
            }

            if (localLessons.isEmpty() && pendingSeeds.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No lessons published in this module yet",
                        message = "Check back as new curated communication lessons are added to this module."
                    )
                }
            }

            if (tier.isExpandedTablet) {
                items(localLessons.chunked(2), key = { row -> row.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        pair.forEach { lesson ->
                            val prog = learningProgress.firstOrNull { it.lessonId == lesson.id }
                            val done = prog?.completed == true
                            ModuleLessonCard(
                                lesson = lesson,
                                done = done,
                                onOpenLesson = onOpenLesson,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(localLessons, key = { it.id }) { lesson ->
                    val prog = learningProgress.firstOrNull { it.lessonId == lesson.id }
                    val done = prog?.completed == true
                    ModuleLessonCard(
                        lesson = lesson,
                        done = done,
                        onOpenLesson = onOpenLesson,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (pendingSeeds.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Upcoming Editorial Drafts",
                        subtitle = "Curriculum modules currently being authored"
                    )
                }
                items(pendingSeeds, key = { it.id }) { seed ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LessonAttributionBadge(isCreatorDerived = false, sourceVerified = false)
                            Text(seed.title, style = MaterialTheme.typography.titleMedium)
                            Text(seed.creatorAdvice, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleLessonCard(
    lesson: LessonDefinition,
    done: Boolean,
    onOpenLesson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onOpenLesson(lesson.id) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (done) EmeraldSuccess.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (done) 1.5.dp else 1.dp,
            if (done) EmeraldSuccess.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.testTag("lesson_card_${lesson.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LessonAttributionBadge(lesson.isCreatorDerived, lesson.sourceVerified)
                Icon(
                    if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (done) "Completed" else "Not completed",
                    tint = if (done) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(lesson.title, style = MaterialTheme.typography.titleLarge)
            Text(lesson.objective, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Source: ${lesson.sourceVideoTitle} • ${lesson.timestampLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = AcousticTeal
            )
        }
    }
}

@Composable
fun LessonDetailScreen(
    lessonId: String,
    sharedLessons: List<Lesson>,
    learningProgress: List<UserLearningProgress>,
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>,
    onToggleCompleted: (lessonId: String, moduleSlug: String, completed: Boolean, notes: String) -> Unit,
    onNavigateToLesson: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val lesson = remember(lessonId, sharedLessons) {
        CurriculumCatalog.lessons.firstOrNull { it.id == lessonId }
            ?: sharedLessons.firstOrNull { it.id == lessonId }?.let { l ->
                LessonDefinition(
                    id = l.id,
                    moduleSlug = l.moduleSlug,
                    title = l.title,
                    objective = l.objective,
                    sourceVideoTitle = "Curated Lesson",
                    sourceVideoUrl = l.sourceVideoUrl,
                    timestampLabel = l.timestampLabel,
                    explanation = l.explanation,
                    instructions = l.instructions,
                    example = l.example,
                    practicePrompt = l.practicePrompt,
                    nextLessonId = l.nextLessonId.ifBlank { null },
                    isCreatorDerived = l.isCreatorDerived,
                    sourceVerified = true
                )
            }
    }

    if (lesson == null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard("Lesson not found", "Select another lesson from the module directory.")
        }
        return
    }

    val progressRecord = learningProgress.firstOrNull { it.lessonId == lesson.id }
    val isCompleted = progressRecord?.completed == true
    var notesDraft by remember(progressRecord?.notes) { mutableStateOf(progressRecord?.notes.orEmpty()) }

    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val isPaused by speakingVm.audioManager.isPaused.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val elapsed by speakingVm.audioManager.elapsedSeconds.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val breakdownCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Technique Breakdown", style = MaterialTheme.typography.titleMedium)
                    Text(lesson.explanation, style = MaterialTheme.typography.bodyMedium)
                    Text("Step-by-Step Instructions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    lesson.instructions.forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = lesson.example,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
            }
        }

        val practiceCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Practice Prompt", style = MaterialTheme.typography.titleMedium)
                    Text(lesson.practicePrompt, style = MaterialTheme.typography.bodyLarge)
                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording, isPaused = isPaused)
                    Text("Captured: ${elapsed}s", style = MaterialTheme.typography.labelMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isRecording) {
                                    speakingVm.stopRecording()
                                } else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                            modifier = Modifier.weight(1f).testTag("lesson_record_button")
                        ) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRecording) "Stop" else "Record")
                        }
                        if (isRecording) {
                            OutlinedButton(
                                onClick = { if (isPaused) speakingVm.resumeRecording() else speakingVm.pauseRecording() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPaused) "Resume" else "Pause")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    speakingVm.submitForAiAnalysis(
                                        moduleType = "LESSON_PRACTICE",
                                        promptTitle = lesson.title,
                                        practiceTarget = LabPracticeTarget.fromPrompt(lesson.title, lesson.practicePrompt)
                                    )
                                },
                                enabled = recordedPath != null,
                                modifier = Modifier.weight(1f).testTag("lesson_analyze_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Get AI Feedback")
                            }
                        }
                    }
                }
            }
        }

        val feedbackCardSection: @Composable () -> Unit = {
            if (activeModule == "LESSON_PRACTICE") {
                when (val st = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(150)
                    is UiState.Error -> ErrorRecoveryCard(st.message)
                    is UiState.Success -> FeedbackReportCard(st.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        val completionCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Completion & Takeaways", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = notesDraft,
                        onValueChange = { notesDraft = it },
                        label = { Text("Your key takeaway from practicing this lesson") },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onToggleCompleted(lesson.id, lesson.moduleSlug, !isCompleted, notesDraft)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("lesson_complete_toggle")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isCompleted) "Completed ✓" else "Mark Complete")
                        }
                        if (!lesson.nextLessonId.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    speakingVm.clearAnalysis()
                                    onNavigateToLesson(lesson.nextLessonId)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("next_lesson_button")
                            ) {
                                Text("Next Lesson")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LessonAttributionBadge(lesson.isCreatorDerived, lesson.sourceVerified)
                    Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
                    Text("Objective: ${lesson.objective}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (lesson.sourceVideoUrl.isNotBlank()) {
                        OutlinedButton(onClick = { uriHandler.openUri(lesson.sourceVideoUrl) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Watch source video")
                            Spacer(Modifier.width(6.dp))
                            Text("${lesson.sourceVideoTitle} • ${lesson.timestampLabel}")
                        }
                    }
                    FeatureGuideStrip(featureKey = "lesson_detail")
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
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            breakdownCard()
                            completionCard()
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            practiceCard()
                            feedbackCardSection()
                        }
                    }
                }
            } else {
                item { breakdownCard() }
                item { practiceCard() }
                if (activeModule == "LESSON_PRACTICE") {
                    item { feedbackCardSection() }
                }
                item { completionCard() }
            }
        }
    }
}

@Composable
fun VoiceTrainingLabScreen(
    speakingVm: SpeakingAndLabsViewModel,
    ttsHelper: TtsAndSpeechHelper,
    pastSessions: List<SpeakingSession>
) {
    val context = LocalContext.current
    var targetWpm by remember { mutableFloatStateOf(130f) }
    var pauseBeats by remember { mutableFloatStateOf(2f) }
    var activeLever by remember { mutableStateOf("Rate & Intentional Pauses") }

    val leverScripts = remember {
        mapOf(
            "Rate & Intentional Pauses" to "Speed builds urgency when listing ideas — then slow down completely before delivering your main takeaway. [PAUSE 1s] That contrast makes people listen.",
            "Melody & Pitch Range" to "Instead of locking every sentence into one flat note, lift slightly on curious questions and drop downward cleanly on decisive recommendations.",
            "Volume & Projection" to "Breathe deep into your lower ribs, drop your shoulders, and project toward the back row without straining your throat.",
            "Character & Warmth" to "Deliver data clearly, then shift into warm conversational empathy when describing how a decision impacts the people around you."
        )
    }
    val script = leverScripts[activeLever].orEmpty()

    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(null)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val drillCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vocal Lever Focus: $activeLever", style = MaterialTheme.typography.titleMedium, color = AcousticTeal)
                    Text(script, style = MaterialTheme.typography.bodyLarge)

                    Text("Target Pace: ${targetWpm.roundToInt()} WPM • Pause Hold: ${pauseBeats.roundToInt()}s", style = MaterialTheme.typography.labelMedium)
                    Slider(value = targetWpm, onValueChange = { targetWpm = it }, valueRange = 100f..175f)
                    Slider(value = pauseBeats, onValueChange = { pauseBeats = it }, valueRange = 1f..4f, steps = 2)

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { ttsHelper.speak(script, (targetWpm / 140f).coerceIn(0.75f, 1.25f)) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Hear Reference")
                        }
                        Button(
                            onClick = {
                                if (isRecording) speakingVm.stopRecording()
                                else {
                                    val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (ok) speakingVm.startRecording(null) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRecording) "Stop" else "Practice Drill")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            speakingVm.submitForAiAnalysis(
                                moduleType = "SPEAKING_LAB",
                                promptTitle = "Voice Training: $activeLever",
                                practiceTarget = LabPracticeTarget.readAloud(script)
                            )
                        },
                        enabled = !isRecording && recordedPath != null,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    ) {
                        Text("Evaluate Vocal Foundations With AI")
                    }
                }
            }
        }

        val feedbackSection: @Composable () -> Unit = {
            if (activeModule == "SPEAKING_LAB") {
                when (val s = analysisState) {
                    is UiState.Loading -> ShimmerSkeletonCard(140)
                    is UiState.Error -> ErrorRecoveryCard(s.message)
                    is UiState.Success -> FeedbackReportCard(s.data, pastSessions.firstOrNull(), evaluationReport)
                    null -> Unit
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Voice Training Lab",
                    subtitle = "5 Vocal Foundations: Rate, Pause, Melody, Volume & Character"
                )
                Spacer(Modifier.height(8.dp))
                FeatureGuideStrip(featureKey = "voice_training")
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            drillCard()
                            feedbackSection()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Vocal Foundations", style = MaterialTheme.typography.titleMedium)
                            leverScripts.keys.forEach { lever ->
                                val isSelected = lever == activeLever
                                Card(
                                    onClick = { activeLever = lever; speakingVm.clearAnalysis() },
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
                                        Text(lever, style = MaterialTheme.typography.titleMedium)
                                        Text(leverScripts[lever].orEmpty(), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item { drillCard() }
                if (activeModule == "SPEAKING_LAB") {
                    item { feedbackSection() }
                }
                items(leverScripts.keys.toList()) { lever ->
                    val isSelected = lever == activeLever
                    Card(
                        onClick = { activeLever = lever; speakingVm.clearAnalysis() },
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
                            Text(lever, style = MaterialTheme.typography.titleMedium)
                            Text(leverScripts[lever].orEmpty(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
