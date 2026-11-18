package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.ProgramEnrollment
import com.example.data.model.VocabSeed
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldSuccess
import kotlin.math.roundToInt

@Composable
fun DailyPracticeScreen(
    todayDateKey: String,
    todayActivity: DailyActivity?,
    todaysWords: List<VocabSeed>,
    onToggleStep: (stepId: String, reflection: String, confidence: Long) -> Unit,
    onSaveReflection: (reflection: String, confidence: Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    val completedIds = todayActivity?.completedStepIds.orEmpty()
    var reflection by remember(todayActivity?.reflectionText) {
        mutableStateOf(todayActivity?.reflectionText.orEmpty())
    }
    var confidence by remember(todayActivity?.confidenceRating) {
        mutableFloatStateOf((todayActivity?.confidenceRating ?: 6L).toFloat())
    }

    val steps = CurriculumCatalog.dailySteps
    val progress = (completedIds.size.toFloat() / steps.size.coerceAtLeast(1)).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Daily Communication Practice",
                    subtitle = "$todayDateKey • ${completedIds.size}/${steps.size} activities completed"
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = EmeraldSuccess
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Today's 3 Target Words (Daily Rotation)", style = MaterialTheme.typography.titleMedium)
                        todaysWords.forEach { w ->
                            Text("• ${w.word} (${w.phonetic}): ${w.meaning}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            items(steps, key = { it.id }) { step ->
                val isDone = completedIds.contains(step.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleStep(step.id, reflection, confidence.roundToInt().toLong()) },
                            modifier = Modifier.testTag("daily_step_toggle_${step.id}")
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isDone) "Completed ${step.title}" else "Mark ${step.title} complete",
                                tint = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${step.stepNumber}. ${step.title}", style = MaterialTheme.typography.titleMedium)
                            Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (step.actionRoute != null) {
                            OutlinedButton(onClick = { onNavigate(step.actionRoute) }) {
                                Text("Open")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Daily Self-Reflection & Confidence Check", style = MaterialTheme.typography.titleMedium)
                        Text("Self-rated confidence today: ${confidence.roundToInt()}/10", style = MaterialTheme.typography.bodySmall)
                        Slider(value = confidence, onValueChange = { confidence = it }, valueRange = 1f..10f)
                        OutlinedTextField(
                            value = reflection,
                            onValueChange = { reflection = it },
                            label = { Text("What felt stronger today? What @askvinh lever will you focus on tomorrow?") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("daily_reflection_input")
                        )
                        Button(
                            onClick = { onSaveReflection(reflection, confidence.roundToInt().toLong()) },
                            modifier = Modifier.fillMaxWidth().testTag("save_daily_reflection_button")
                        ) {
                            Text("Save Daily Reflection")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningPathsScreen(
    enrollments: List<ProgramEnrollment>,
    onEnroll: (programId: String, title: String) -> Unit,
    onToggleDay: (programId: String, dayNumber: Long, totalDays: Int) -> Unit
) {
    val programs = CurriculumCatalog.structuredPrograms
    var expandedProgramId by remember { mutableStateOf("30_day_challenge") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Structured Learning Programs",
                    subtitle = "30/60/90-day tracks & specialized @askvinh skill curricula"
                )
            }

            items(programs, key = { it.id }) { program ->
                val enrollment = enrollments.firstOrNull { it.programId == program.id }
                val completedDays = enrollment?.completedDays.orEmpty()
                val isExpanded = expandedProgramId == program.id

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(program.title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${program.durationDays} Days • ${program.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (enrollment == null) {
                                Button(
                                    onClick = { onEnroll(program.id, program.title) },
                                    modifier = Modifier.testTag("enroll_${program.id}")
                                ) {
                                    Text("Enroll")
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    expandedProgramId = if (isExpanded) "" else program.id
                                }) {
                                    Text(if (isExpanded) "Hide Plan" else "${completedDays.size}/${program.durationDays}d")
                                }
                            }
                        }

                        if (enrollment != null) {
                            LinearProgressIndicator(
                                progress = { (completedDays.size.toFloat() / program.durationDays).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = EmeraldSuccess
                            )
                        }

                        if (isExpanded || (enrollment != null && program.id == "30_day_challenge")) {
                            var dayCounter = 1L
                            program.weeks.forEach { week ->
                                Text(week.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Focus: ${week.focusAreas.joinToString(" • ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                week.dailyDrills.forEach { drillText ->
                                    val dNum = dayCounter++
                                    val checked = completedDays.contains(dNum)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (enrollment != null) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { onToggleDay(program.id, dNum, program.durationDays) }
                                            )
                                        }
                                        Text(drillText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
