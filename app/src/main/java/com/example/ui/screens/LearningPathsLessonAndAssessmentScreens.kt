package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathFeedback
import com.example.ui.components.TranscriptDisplayCard
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.LearningPathsUiState
import kotlin.math.roundToInt

/**
 * Interactive daily lesson workspace supporting theory study, before/after contrast inspection,
 * TTS model listening, offline recording + live transcription, editable transcript/writing practice,
 * knowledge checks, inline transcript mistake highlights, rubric scoring, and local reflection notes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningPathLessonScreen(
    programId: String,
    dayNumber: Int,
    uiState: LearningPathsUiState,
    onPrepareLesson: (String, Int) -> Unit,
    onBackToSyllabus: () -> Unit,
    onNavigateDay: (Int) -> Unit,
    onOpenAssessment: (String, String) -> Unit,
    onOpenActivityStudio: (String) -> Unit,
    onOpenLabRoute: (String) -> Unit,
    onUpdateDraftText: (String) -> Unit,
    onSelectQuizOption: (String, Int) -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onSetConfidence: (Int) -> Unit,
    onUpdateReflectionNote: (String) -> Unit,
    onSaveReflectionNote: () -> Unit,
    onPlayModelTts: (String) -> Unit,
    onStopModelTts: () -> Unit,
    onStartOrResumeRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSubmitForEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(programId, dayNumber) {
        onPrepareLesson(programId, dayNumber)
    }

    val program = LearningPathCurriculumRegistry.getProgram(programId)
    val lesson = LearningPathCurriculumRegistry.getLesson(programId, dayNumber)
    if (program == null || lesson == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lesson Day $dayNumber could not be loaded.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBackToSyllabus) { Text("Return to Syllabus") }
            }
        }
        return
    }

    val ext = MaterialTheme.speakCraftColors
    val module = LearningPathCurriculumRegistry.getModule(programId, lesson.moduleId)
    val enrollment = uiState.userState.enrollments[programId]
    val dayCompleted = enrollment?.completedDays?.contains(dayNumber) == true
    val wordCount = LocalLanguageAnalysisEngine.tokenizeWords(uiState.draftResponseText).size
    val feedback = uiState.activeFeedback

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lp_lesson_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navigation Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackToSyllabus) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Syllabus")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onNavigateDay(dayNumber - 1) },
                        enabled = dayNumber > 1
                    ) {
                        Text("Prev")
                    }
                    OutlinedButton(
                        onClick = { onNavigateDay(dayNumber + 1) },
                        enabled = dayNumber < program.durationDays
                    ) {
                        Text("Next")
                    }
                }
            }
        }

        // Lesson Banner Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.studioConsoleSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallPillTag(
                            text = "Day ${lesson.dayNumber} of ${program.durationDays}",
                            bg = MaterialTheme.colorScheme.primaryContainer,
                            fg = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        SmallPillTag(
                            text = lesson.exerciseType.label,
                            bg = ext.pastelSkyBg,
                            fg = ext.pastelSkyInk
                        )
                        SmallPillTag(
                            text = "${lesson.estimatedMinutes} min",
                            bg = ext.pastelPeriwinkleBg,
                            fg = ext.pastelPeriwinkleInk
                        )
                        if (dayCompleted) {
                            SmallPillTag(
                                text = "Completed",
                                bg = ext.pastelMintBg,
                                fg = ext.pastelMintInk
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ext.onStudioConsole
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lesson.learningObjective,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onStudioConsole.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // 1. Concept Explanation & Takeaways
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Learn the Core Technique",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(lesson.coreConceptExplanation, style = MaterialTheme.typography.bodyMedium)
                    lesson.keyTakeaways.forEach { point ->
                        Text("• $point", style = MaterialTheme.typography.bodySmall)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lesson.focusKeywords.forEach { kw ->
                            SmallPillTag(kw, ext.pastelLavenderBg, ext.pastelLavenderInk)
                        }
                    }
                }
            }
        }

        // Before vs After Contrast + TTS Model Playback
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ext.cardBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Before vs After Model Contrast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = ext.pastelRoseBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = lesson.exampleContrast.beforeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ext.pastelRoseInk
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = lesson.exampleContrast.weakExample,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.pastelRoseInk
                            )
                        }
                    }
                    Surface(
                        color = ext.pastelMintBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = lesson.exampleContrast.afterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ext.pastelMintInk
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = lesson.exampleContrast.strongExample,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.pastelMintInk
                            )
                        }
                    }
                    Text(
                        text = "Why it works: ${lesson.exampleContrast.whyItWorks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lesson.ttsModelScript.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = "Model Practice Script:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "\"${lesson.ttsModelScript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPlayModelTts(lesson.ttsModelScript) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Listen to Model")
                            }
                            TextButton(onClick = onStopModelTts) {
                                Text("Stop Audio")
                            }
                        }
                    }
                }
            }
        }

        // 2. Practice Prompt, Checklist & Speaking / Text Workspace
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Practice Workflow",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SmallPillTag(
                            text = "$wordCount / ${lesson.targetWordCountMin}+ words",
                            bg = if (wordCount >= lesson.targetWordCountMin) ext.pastelMintBg else ext.pastelGoldBg,
                            fg = if (wordCount >= lesson.targetWordCountMin) ext.pastelMintInk else ext.pastelGoldInk
                        )
                    }

                    Text(
                        text = lesson.exercisePrompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Guided Practice Checklist
                    lesson.practiceChecklist.forEachIndexed { idx, step ->
                        val checked = uiState.draftChecklistIndices.contains(idx)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleChecklistItem(idx) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleChecklistItem(idx) }
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Audio Capture Studio Strip (Works cleanly with optional mic/live transcription)
                    Surface(
                        color = ext.pastelSkyBg.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.isRecordingAudio) {
                                        if (uiState.isRecordingPaused) "Recording Paused (${uiState.recordingSeconds}s)"
                                        else "Recording Spoken Response (${uiState.recordingSeconds}s)"
                                    } else if (!uiState.attachedAudioPath.isNullOrBlank()) {
                                        "Audio Attached (${uiState.recordingSeconds}s recorded)"
                                    } else {
                                        "Optional On-Device Voice Recording"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ext.pastelSkyInk
                                )
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!uiState.isRecordingAudio || uiState.isRecordingPaused) {
                                    Button(onClick = onStartOrResumeRecording) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (uiState.isRecordingPaused) "Resume Mic" else "Record Speech")
                                    }
                                } else {
                                    OutlinedButton(onClick = onPauseRecording) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pause")
                                    }
                                }
                                if (uiState.isRecordingAudio) {
                                    Button(onClick = onStopRecording) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Stop")
                                    }
                                }
                                if (!uiState.attachedAudioPath.isNullOrBlank() && !uiState.isRecordingAudio) {
                                    OutlinedButton(onClick = onTogglePlayback) {
                                        Icon(
                                            if (uiState.isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (uiState.isPlayingAudio) "Stop Playback" else "Play Recording")
                                    }
                                }
                            }

                            if (uiState.liveTranscriptionState.displayText.isNotBlank()) {
                                Text(
                                    text = "Live Transcript: ${uiState.liveTranscriptionState.displayText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.pastelSkyInk
                                )
                            }
                        }
                    }

                    // Transcript / Written Response Input
                    OutlinedTextField(
                        value = uiState.draftResponseText,
                        onValueChange = onUpdateDraftText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .testTag("lp_lesson_response_input"),
                        label = { Text("Your Spoken Transcript or Practice Draft") },
                        placeholder = {
                            Text("Speak aloud while recording or type your natural ${lesson.targetWordCountMin}+ word response here for instant coaching feedback...")
                        }
                    )

                    // Linked Activity Engine & Labs Deep Links
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(onClick = { onOpenActivityStudio(lesson.linkedActivityId) }) {
                            Text("Practice in Full Studio")
                        }
                        if (!lesson.linkedLabRoute.isNullOrBlank()) {
                            TextButton(onClick = { onOpenLabRoute(lesson.linkedLabRoute) }) {
                                Text("Open Skill Lab")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick Knowledge Check Quiz
                    if (lesson.quizQuestions.isNotEmpty()) {
                        Text(
                            text = "Quick Concept Check",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        lesson.quizQuestions.forEach { q ->
                            Text(
                                text = q.prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            q.options.forEachIndexed { idx, opt ->
                                val selected = uiState.draftQuizAnswers[q.questionId] == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectQuizOption(q.questionId, idx) }
                                        .testTag("lp_quiz_${q.questionId}_$idx"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { onSelectQuizOption(q.questionId, idx) }
                                    )
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (feedback != null) {
                                Surface(
                                    color = ext.pastelLavenderBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Answer Note: ${q.explanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.pastelLavenderInk,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Self-Confidence Slider (strictly separated from objective score)
                    HorizontalDivider()
                    Text(
                        text = "Self-Rated Speaking Confidence: ${uiState.draftConfidenceRating} / 5 (tracked separately from objective score)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = uiState.draftConfidenceRating.toFloat(),
                        onValueChange = { onSetConfidence(it.roundToInt()) },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        }

        item {
            Button(
                onClick = onSubmitForEvaluation,
                enabled = !uiState.isEvaluating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lp_lesson_submit_btn")
            ) {
                if (uiState.isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Evaluating Response...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Analyze & Score My Response")
                }
            }
        }

        // 3. Feedback, Inline Transcript Highlights & Adaptive Navigation
        if (feedback != null) {
            item {
                LessonEvaluationFeedbackBlock(
                    feedback = feedback,
                    hasTranscriptText = uiState.draftResponseText.isNotBlank(),
                    draftText = uiState.draftResponseText
                )
            }

            item {
                val isLastDayInModule = module?.dayEnd == dayNumber
                val modulePassed = enrollment?.moduleAssessments?.get(lesson.moduleId)?.passed == true
                Card(
                    colors = CardDefaults.cardColors(containerColor = ext.pastelLavenderBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Recommended Next Step",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.pastelLavenderInk
                        )
                        Text(
                            text = feedback.recommendedNextStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.pastelLavenderInk
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isLastDayInModule && !modulePassed) {
                                Button(onClick = { onOpenAssessment(programId, module.moduleId) }) {
                                    Text(if (module.assessment.isCapstone) "Take Final Capstone" else "Take Module Checkpoint")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else if (dayNumber < program.durationDays) {
                                Button(onClick = { onNavigateDay(dayNumber + 1) }) {
                                    Text("Continue Day ${dayNumber + 1}")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else if (module != null) {
                                Button(onClick = { onOpenAssessment(programId, module.moduleId) }) {
                                    Text("Open Final Capstone")
                                }
                            }
                            OutlinedButton(onClick = onBackToSyllabus) {
                                Text("Back to Syllabus")
                            }
                        }
                    }
                }
            }
        }

        // 4. Persistent Offline Reflection Note
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Personal Reflection Note (Saved Locally)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = uiState.draftReflectionNote,
                        onValueChange = onUpdateReflectionNote,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lp_lesson_note_input"),
                        placeholder = { Text("Write one key takeaway or tweak to carry into tomorrow...") }
                    )
                    OutlinedButton(onClick = onSaveReflectionNote) {
                        Text("Save Reflection Note")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonEvaluationFeedbackBlock(
    feedback: LearningPathFeedback,
    hasTranscriptText: Boolean,
    draftText: String
) {
    val ext = MaterialTheme.speakCraftColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Score Headline Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (feedback.passed) ext.pastelMintBg else ext.pastelPeachBg
            ),
            border = BorderStroke(1.dp, if (feedback.passed) ext.success else ext.warning),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.testTag("lp_feedback_score_card")
        ) {
            val fg = if (feedback.passed) ext.pastelMintInk else ext.pastelPeachInk
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (feedback.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = fg
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (feedback.passed) "Lesson Target Passed" else "Polish & Retry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                    Text(
                        text = "${feedback.overallScore}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (feedback.quizTotalCount > 0) {
                        Text(
                            text = "Concept Check: ${feedback.quizCorrectCount}/${feedback.quizTotalCount} (${feedback.quizScorePercent}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = fg
                        )
                    }
                    Text(
                        text = "Confidence: ${feedback.confidenceSelfRating}/5 (Self-Rated)",
                        style = MaterialTheme.typography.labelMedium,
                        color = fg
                    )
                }

                Text(
                    text = feedback.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg
                )
                Text(
                    text = feedback.evidenceNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.8f)
                )
            }
        }

        // Real-Time Inline Transcript Mistake Highlighter (Reuses shared TranscriptDisplayCard)
        if (hasTranscriptText) {
            TranscriptDisplayCard(
                transcript = draftText,
                insights = feedback.transcriptInsights,
                summary = feedback.transcriptSummary
            )
        }

        // Rubric Breakdown
        if (feedback.rubricBreakdown.isNotEmpty()) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Objective Rubric Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    feedback.rubricBreakdown.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("${item.scoreOutOf100}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { item.scoreOutOf100 / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(item.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // What Went Well
        if (feedback.whatWentWell.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.pastelMintBg.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("What Went Well", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.pastelMintInk)
                    feedback.whatWentWell.forEach { line ->
                        Text("✓ $line", style = MaterialTheme.typography.bodySmall, color = ext.pastelMintInk)
                    }
                }
            }
        }

        // Mistakes & Gaps
        if (feedback.mistakesFound.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.pastelRoseBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mistakes & Gaps Spotted", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.pastelRoseInk)
                    feedback.mistakesFound.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall, color = ext.pastelRoseInk)
                    }
                }
            }
        }

        // Corrected Examples
        if (feedback.correctedExamples.isNotEmpty()) {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suggested Phrasing Upgrades", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    feedback.correctedExamples.forEach { fix ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Before: ${fix.original}", style = MaterialTheme.typography.bodySmall)
                                Text("Improved: ${fix.improved}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ext.success)
                                Text("Why: ${fix.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Actionable Coach Tips
        if (feedback.coachTips.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.pastelSkyBg.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Actionable Coach Tips", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.pastelSkyInk)
                    feedback.coachTips.forEach { tip ->
                        Text("→ $tip", style = MaterialTheme.typography.bodySmall, color = ext.pastelSkyInk)
                    }
                }
            }
        }
    }
}

// =============================================================================
// MODULE CHECKPOINT & FINAL PROGRAM CAPSTONE ASSESSMENT SCREEN
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningPathAssessmentScreen(
    programId: String,
    moduleId: String,
    uiState: LearningPathsUiState,
    onPrepareAssessment: (String, String) -> Unit,
    onSelectScenarioOption: (String, Int) -> Unit,
    onUpdatePracticalText: (String) -> Unit,
    onSetConfidence: (Int) -> Unit,
    onSubmitAssessment: () -> Unit,
    onBackToSyllabus: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(programId, moduleId) {
        onPrepareAssessment(programId, moduleId)
    }

    val program = LearningPathCurriculumRegistry.getProgram(programId)
    val module = LearningPathCurriculumRegistry.getModule(programId, moduleId)
    if (program == null || module == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Module assessment could not be loaded.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBackToSyllabus) { Text("Back to Syllabus") }
            }
        }
        return
    }

    val spec = module.assessment
    val ext = MaterialTheme.speakCraftColors
    val existingRecord = uiState.userState.enrollments[programId]?.moduleAssessments?.get(moduleId)
    val evalResult = uiState.activeAssessmentResult
    val wordCount = LocalLanguageAnalysisEngine.tokenizeWords(uiState.assessmentPracticalText).size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lp_assessment_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBackToSyllabus) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back to ${program.title}")
            }
        }

        // Assessment Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.studioConsoleSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (spec.isCapstone) Icons.Default.Star else Icons.Default.School,
                            contentDescription = null,
                            tint = ext.onStudioConsole
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (spec.isCapstone) "FINAL PROGRAM CAPSTONE" else "MODULE CHECKPOINT ASSESSMENT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.onStudioConsole.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ext.onStudioConsole
                    )
                    Text(
                        text = spec.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onStudioConsole.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Pass Threshold: ${spec.passingScoreThreshold}% (40% Scenario Judgment + 60% Practical Performance)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.onStudioConsole.copy(alpha = 0.8f)
                    )
                    if (existingRecord != null && existingRecord.attempts > 0) {
                        Text(
                            text = "Previous Best: ${existingRecord.bestCombinedScore}% (${if (existingRecord.passed) "PASSED" else "Not Yet Passed"})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.onStudioConsole
                        )
                    }
                }
            }
        }

        // Part 1: Scenario Judgment Questions
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Part 1: Scenario Judgment (${spec.scenarioQuestions.size} Questions)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    spec.scenarioQuestions.forEachIndexed { qIdx, q ->
                        Text(
                            text = "${qIdx + 1}. ${q.prompt}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        q.options.forEachIndexed { oIdx, opt ->
                            val selected = uiState.assessmentScenarioAnswers[q.questionId] == oIdx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectScenarioOption(q.questionId, oIdx) }
                                    .testTag("lp_assess_q_${q.questionId}_$oIdx"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectScenarioOption(q.questionId, oIdx) }
                                )
                                Text(opt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                        if (evalResult != null) {
                            val isCorrect = uiState.assessmentScenarioAnswers[q.questionId] == q.correctIndex
                            Surface(
                                color = if (isCorrect) ext.pastelMintBg else ext.pastelPeachBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${if (isCorrect) "Correct" else "Review"}: ${q.explanation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCorrect) ext.pastelMintInk else ext.pastelPeachInk,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (qIdx < spec.scenarioQuestions.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        // Part 2: Practical Performance Task
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Part 2: Practical Demonstration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SmallPillTag(
                            text = "$wordCount / ${spec.minWords}+ words",
                            bg = if (wordCount >= spec.minWords) ext.pastelMintBg else ext.pastelGoldBg,
                            fg = if (wordCount >= spec.minWords) ext.pastelMintInk else ext.pastelGoldInk
                        )
                    }
                    Text(spec.practicalTaskPrompt, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spec.expectedElements.forEach { elem ->
                            SmallPillTag(elem, ext.pastelPeriwinkleBg, ext.pastelPeriwinkleInk)
                        }
                    }
                    OutlinedTextField(
                        value = uiState.assessmentPracticalText,
                        onValueChange = onUpdatePracticalText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .testTag("lp_assessment_practical_input"),
                        label = { Text("Your Complete Assessment Response") },
                        placeholder = { Text("Deliver a cohesive, structured response hitting all required elements above...") }
                    )
                    Text(
                        text = "Confidence Self-Rating: ${uiState.assessmentConfidenceRating} / 5",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = uiState.assessmentConfidenceRating.toFloat(),
                        onValueChange = { onSetConfidence(it.roundToInt()) },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        }

        item {
            Button(
                onClick = onSubmitAssessment,
                enabled = !uiState.isGradingAssessment,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lp_assessment_submit_btn")
            ) {
                if (uiState.isGradingAssessment) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Grading Assessment...")
                } else {
                    Text(if (spec.isCapstone) "Submit Final Capstone" else "Submit Checkpoint Assessment")
                }
            }
        }

        // Assessment Graded Result
        if (evalResult != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (evalResult.passed) ext.pastelMintBg else ext.pastelPeachBg
                    ),
                    border = BorderStroke(1.5.dp, if (evalResult.passed) ext.success else ext.warning),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("lp_assessment_result_card")
                ) {
                    val fg = if (evalResult.passed) ext.pastelMintInk else ext.pastelPeachInk
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (evalResult.passed) {
                                if (spec.isCapstone) "CAPSTONE GRADUATION PASSED — ${evalResult.combinedScore}%"
                                else "MODULE CHECKPOINT PASSED — ${evalResult.combinedScore}%"
                            } else {
                                "SCORE ${evalResult.combinedScore}% — RETAKE RECOMMENDED (${spec.passingScoreThreshold}%+ REQUIRED)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                        Text(
                            text = "Scenario Quiz: ${evalResult.quizScorePercent}% · Practical Demonstration: ${evalResult.practicalScorePercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = fg
                        )
                    }
                }
            }

            item {
                LessonEvaluationFeedbackBlock(
                    feedback = evalResult.feedback,
                    hasTranscriptText = uiState.assessmentPracticalText.isNotBlank(),
                    draftText = uiState.assessmentPracticalText
                )
            }

            item {
                Button(
                    onClick = onBackToSyllabus,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (evalResult.passed) "Continue Learning Path Syllabus" else "Return to Syllabus")
                }
            }
        }
    }
}
