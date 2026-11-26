package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.TextButton
import com.example.data.learning.DailyActivityItem
import com.example.data.learning.DailyAdaptiveCurriculumEngine
import com.example.data.learning.DailyAdaptiveCurriculumPackage
import com.example.data.learning.DailyMissionTask
import com.example.data.learning.DailyPracticeSentence
import com.example.data.learning.DailyPracticeTask
import com.example.data.learning.DailyRecordBook
import com.example.data.learning.DailyRecordEntry
import com.example.data.learning.DailyRecordOutcome
import com.example.data.learning.DailyTechniqueFocus
import com.example.data.learning.LearningLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.model.VocabSeed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.example.data.activity.ActivityDifficulty
import com.example.data.activity.LearningPathProgressSummary
import com.example.data.activity.LearningPathsEngine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.ProgramEnrollment
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import kotlin.math.roundToInt

@Composable
fun DailyPracticeScreen(
    todayDateKey: String,
    todayActivity: DailyActivity?,
    todaysWords: List<VocabSeed>,
    userLevel: LearningLevel = LearningLevel.INTERMEDIATE,
    dailyPackage: DailyAdaptiveCurriculumPackage? = null,
    onStartActivity: (String) -> Unit = {},
    onSpeakText: ((String) -> Unit)? = null,
    onToggleStep: (stepId: String, reflection: String, confidence: Long) -> Unit,
    onSaveReflection: (reflection: String, confidence: Long) -> Unit,
    onNavigate: (String) -> Unit,
    /** The current daily task set with completion state (falls back to the package's tasks). */
    dailyTasks: List<DailyPracticeTask>? = null,
    /** 1 for the first set of the day; +1 each time a whole set was completed today. */
    setNumberToday: Int = 1,
    /** Daily record: dateKey -> completed tasks / steps, from the local learning state. */
    dailyRecords: Map<String, List<DailyRecordEntry>> = emptyMap(),
    onCompleteTask: (taskId: String) -> Unit = {}
) {
    val completedIds = todayActivity?.completedStepIds.orEmpty()
    var reflection by rememberSaveable(todayActivity?.reflectionText) {
        mutableStateOf(todayActivity?.reflectionText.orEmpty())
    }
    var confidence by rememberSaveable(todayActivity?.confidenceRating) {
        mutableFloatStateOf((todayActivity?.confidenceRating ?: 6L).toFloat())
    }

    val effectivePackage = dailyPackage ?: remember(todayDateKey, userLevel) {
        DailyAdaptiveCurriculumEngine.generateDailyPackage(
            dateKey = todayDateKey,
            level = userLevel,
            completedTodayIds = completedIds.toSet()
        )
    }

    val displayWords = if (effectivePackage.dailyWords.isNotEmpty()) effectivePackage.dailyWords else todaysWords
    val steps = CurriculumCatalog.dailySteps
    val progress = (completedIds.size.toFloat() / steps.size.coerceAtLeast(1)).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val themeBanner: @Composable () -> Unit = {
            DailyCurriculumThemeBanner(packageInfo = effectivePackage)
        }

        val targetWordsCard: @Composable () -> Unit = {
            DailyTargetWordsCard(
                words = displayWords,
                userLevel = effectivePackage.userLevel,
                onSpeakWord = onSpeakText,
                onNavigate = onNavigate
            )
        }

        val spokenSentencesCard: @Composable () -> Unit = {
            DailySpokenSentencesCard(
                sentences = effectivePackage.dailySentences,
                userLevel = effectivePackage.userLevel,
                onSpeakText = onSpeakText,
                onNavigate = onNavigate
            )
        }

        val activitiesCard: @Composable () -> Unit = {
            DailyActivitiesCard(
                activities = effectivePackage.dailyActivities,
                userLevel = effectivePackage.userLevel,
                onStartActivity = onStartActivity
            )
        }

        val techniqueCard: @Composable () -> Unit = {
            DailyTechniqueCard(technique = effectivePackage.dailyTechnique)
        }

        val missionCard: @Composable () -> Unit = {
            DailyMissionCard(mission = effectivePackage.dailyMission)
        }

        val reflectionCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Self-Reflection & Confidence Check", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${confidence.roundToInt()}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text("Self-rated confidence today: ${confidence.roundToInt()}/10", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = confidence, onValueChange = { confidence = it }, valueRange = 1f..10f)
                    OutlinedTextField(
                        value = reflection,
                        onValueChange = { reflection = it },
                        label = { Text("What felt stronger today? What speaking lever will you focus on tomorrow?") },
                        minLines = 3,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().testTag("daily_reflection_input")
                    )
                    Button(
                        onClick = { onSaveReflection(reflection, confidence.roundToInt().toLong()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("save_daily_reflection_button")
                    ) {
                        Text("Save Daily Reflection")
                    }
                }
            }
        }

        val currentTasks = dailyTasks ?: effectivePackage.dailyTasks
        val taskSetCard: @Composable () -> Unit = {
            DailyTaskSetCard(
                tasks = currentTasks,
                setNumber = setNumberToday,
                onCompleteTask = onCompleteTask,
                onOpenRoute = onNavigate
            )
        }

        val recordCard: @Composable () -> Unit = {
            DailyRecordCard(records = dailyRecords, todayDateKey = todayDateKey)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth).testTag("daily_practice_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Daily Communication Practice",
                    subtitle = "$todayDateKey • Level: ${effectivePackage.userLevel.label} • ${completedIds.size}/${steps.size} routine tasks completed"
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = EmeraldSuccess,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            item {
                FeatureGuideStrip(featureKey = "daily_practice")
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            taskSetCard()
                            spokenSentencesCard()
                            activitiesCard()
                            steps.forEach { step ->
                                val isDone = completedIds.contains(step.id)
                                DailyStepCard(
                                    step = step,
                                    isDone = isDone,
                                    onToggle = { onToggleStep(step.id, reflection, confidence.roundToInt().toLong()) },
                                    onNavigate = onNavigate
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(0.85f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            themeBanner()
                            recordCard()
                            targetWordsCard()
                            techniqueCard()
                            missionCard()
                            reflectionCard()
                        }
                    }
                }
            } else {
                item { themeBanner() }
                item { taskSetCard() }
                item { recordCard() }
                item { spokenSentencesCard() }
                item { targetWordsCard() }
                item { activitiesCard() }
                item { techniqueCard() }
                item { missionCard() }
                items(steps, key = { it.id }) { step ->
                    val isDone = completedIds.contains(step.id)
                    DailyStepCard(
                        step = step,
                        isDone = isDone,
                        onToggle = { onToggleStep(step.id, reflection, confidence.roundToInt().toLong()) },
                        onNavigate = onNavigate
                    )
                }
                item { reflectionCard() }
            }
        }
    }
}

@Composable
private fun DailyCurriculumThemeBanner(packageInfo: DailyAdaptiveCurriculumPackage) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "${packageInfo.userLevel.label} Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        "Target: ~${packageInfo.targetMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                packageInfo.headlineTheme,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Adaptive daily curriculum calibrated to your level. Words, sentences, drills, technique and mission change as soon as you finish the current set, and again each new day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun DailyTargetWordsCard(
    words: List<VocabSeed>,
    userLevel: LearningLevel,
    onSpeakWord: ((String) -> Unit)?,
    onNavigate: (String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Daily Target Vocabulary", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        userLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                "Words calibrated to your current proficiency level. Part of the current daily set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            words.forEach { w ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(w.word, style = MaterialTheme.typography.titleSmall)
                                if (w.phonetic.isNotBlank()) {
                                    Text(w.phonetic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(w.meaning, style = MaterialTheme.typography.bodySmall)
                            if (w.exampleSentence.isNotBlank()) {
                                Text(
                                    "\"${w.exampleSentence}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (onSpeakWord != null) {
                            IconButton(onClick = { onSpeakWord(w.word) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Listen to ${w.word}", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { onNavigate("vocabulary") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Practice In Vocabulary Lab")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun DailySpokenSentencesCard(
    sentences: List<DailyPracticeSentence>,
    userLevel: LearningLevel,
    onSpeakText: ((String) -> Unit)?,
    onNavigate: (String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Today's Spoken Sentences", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        userLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                "Read aloud using the phonetic and grammar cues below. Part of the current daily set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            sentences.forEach { sent ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    sent.focusWordOrTopic,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (onSpeakText != null) {
                                IconButton(onClick = { onSpeakText(sent.text) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text(
                            "\"${sent.text}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (sent.grammarFocus.isNotBlank()) {
                            Text(
                                "Grammar: ${sent.grammarFocus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sent.speakingTip.isNotBlank()) {
                            Text(
                                "Tip: ${sent.speakingTip}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { onNavigate("grammar_speaking") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Practice Grammar & Speaking Aloud")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun DailyActivitiesCard(
    activities: List<DailyActivityItem>,
    userLevel: LearningLevel,
    onStartActivity: (String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Daily Practice Activities", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "${activities.size} Drills",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                "Curated exercises from the 150-activity catalogue, chosen for ${userLevel.label} learners in the current daily set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            activities.forEach { item ->
                val act = item.activity
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(act.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "${item.durationMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Text(
                                    act.skillCategory.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Text(
                                    act.difficulty.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(item.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { onStartActivity(act.activityId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Text("Start Drill (${act.exerciseType.name.lowercase().replace('_', ' ')})")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTechniqueCard(technique: DailyTechniqueFocus) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Technique of the Day", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        technique.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(technique.title, style = MaterialTheme.typography.titleSmall)
            Text(technique.summary, style = MaterialTheme.typography.bodySmall)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Core Rule: ${technique.coreRule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Daily Action: ${technique.suggestedDailyAction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DailyMissionCard(mission: DailyMissionTask) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Daily Real-World Mission", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "${mission.contextCategory} • ~${mission.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(mission.title, style = MaterialTheme.typography.titleSmall)
            Text(mission.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyStepCard(
    step: com.example.data.model.DailyStepDefinition,
    isDone: Boolean,
    onToggle: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) ext.successContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDone) ext.success.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                    .testTag("daily_step_toggle_${step.id}")
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
                OutlinedButton(
                    onClick = { onNavigate(step.actionRoute) },
                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                ) {
                    Text("Open")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun LearningPathsScreen(
    enrollments: List<ProgramEnrollment>,
    onEnroll: (programId: String, title: String) -> Unit,
    onToggleDay: (programId: String, dayNumber: Long, totalDays: Int) -> Unit,
    pathSummaries: List<LearningPathProgressSummary> = emptyList(),
    onOpenActivity: (activityId: String) -> Unit = {}
) {
    val programs = CurriculumCatalog.structuredPrograms
    var expandedProgramId by rememberSaveable { mutableStateOf("30_day_challenge") }
    var selectedMainTab by rememberSaveable { mutableIntStateOf(0) } // 0: 10 Goal-Driven Paths, 1: Structured Programs
    var selectedDifficultyFilter by rememberSaveable { mutableStateOf<ActivityDifficulty?>(null) }

    val effectiveSummaries = remember(pathSummaries) {
        if (pathSummaries.isNotEmpty()) pathSummaries
        else LearningPathsEngine.evaluateAllPaths(emptyList())
    }

    val filteredSummaries = remember(effectiveSummaries, selectedDifficultyFilter) {
        if (selectedDifficultyFilter == null) effectiveSummaries
        else effectiveSummaries.filter { it.path.difficulty == selectedDifficultyFilter }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Learning Paths & Programs",
                    subtitle = "10 goal-driven 4-pillar communication curricula & structured challenge tracks"
                )
                Spacer(Modifier.height(8.dp))
                FeatureGuideStrip(featureKey = "learning_paths")
                Spacer(Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedMainTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedMainTab == 0,
                        onClick = { selectedMainTab = 0 },
                        text = { Text("10 Learning Paths") }
                    )
                    Tab(
                        selected = selectedMainTab == 1,
                        onClick = { selectedMainTab = 1 },
                        text = { Text("Structured Programs") }
                    )
                }
            }

            if (selectedMainTab == 0) {
                // TAB 0: 10 PERSONALIZED LEARNING PATHS (4 PILLARS)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDifficultyFilter == null,
                            onClick = { selectedDifficultyFilter = null },
                            label = { Text("All (${effectiveSummaries.size})") }
                        )
                        FilterChip(
                            selected = selectedDifficultyFilter == ActivityDifficulty.BEGINNER,
                            onClick = { selectedDifficultyFilter = ActivityDifficulty.BEGINNER },
                            label = { Text("Beginner") }
                        )
                        FilterChip(
                            selected = selectedDifficultyFilter == ActivityDifficulty.INTERMEDIATE,
                            onClick = { selectedDifficultyFilter = ActivityDifficulty.INTERMEDIATE },
                            label = { Text("Intermediate") }
                        )
                        FilterChip(
                            selected = selectedDifficultyFilter == ActivityDifficulty.ADVANCED,
                            onClick = { selectedDifficultyFilter = ActivityDifficulty.ADVANCED },
                            label = { Text("Advanced") }
                        )
                    }
                }

                items(filteredSummaries, key = { it.path.pathId }) { summary ->
                    ComprehensiveLearningPathCard(
                        summary = summary,
                        onOpenActivity = onOpenActivity
                    )
                }
            } else {
                // TAB 1: 30/60/90-DAY STRUCTURED PROGRAMS
                items(programs, key = { it.id }) { program ->
                    val enrollment = enrollments.firstOrNull { it.programId == program.id }
                    val completedDays = enrollment?.completedDays.orEmpty()
                    val isExpanded = expandedProgramId == program.id

                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            .testTag("enroll_${program.id}")
                                    ) {
                                        Text("Enroll")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            expandedProgramId = if (isExpanded) "" else program.id
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    ) {
                                        Text(if (isExpanded) "Hide Plan" else "${completedDays.size}/${program.durationDays}d")
                                    }
                                }
                            }

                            if (enrollment != null) {
                                LinearProgressIndicator(
                                    progress = { (completedDays.size.toFloat() / program.durationDays).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                    color = EmeraldSuccess,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
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
}

private fun formatTimeOfDay(epochMs: Long): String =
    if (epochMs <= 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

/**
 * The current daily task set. Tasks complete automatically from the matching practice (an evaluated drill,
 * a valid Coach Practice attempt, a vocabulary review …) or by "Mark done". When the last one is done the
 * next set appears straight away.
 */
@Composable
private fun DailyTaskSetCard(
    tasks: List<DailyPracticeTask>,
    setNumber: Int,
    onCompleteTask: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val doneCount = tasks.count { it.isCompleted }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("daily_task_set_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Today's tasks", style = MaterialTheme.typography.titleMedium)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "Set $setNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp).testTag("daily_task_set_number")
                    )
                }
            }
            Text(
                if (setNumber > 1) {
                    "You finished ${setNumber - 1} set${if (setNumber > 2) "s" else ""} today — here is a fresh one. $doneCount/${tasks.size} done."
                } else {
                    "$doneCount/${tasks.size} done. Finish them all and the next set appears straight away."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = EmeraldSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            tasks.forEach { task ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (task.isCompleted) ext.successContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().testTag("daily_task_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (task.isCompleted) "Done" else "Not done yet",
                            tint = if (task.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (task.isCompleted) "${task.category} • done at ${formatTimeOfDay(task.completedAtMs)}"
                                else "${task.category} • ~${task.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!task.isCompleted) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (task.targetRoute.isNotBlank() && task.targetRoute != "daily_practice") {
                                    TextButton(onClick = { onOpenRoute(task.targetRoute) }) { Text("Start") }
                                }
                                OutlinedButton(
                                    onClick = { onCompleteTask(task.id) },
                                    modifier = Modifier.testTag("daily_task_done_${task.id}")
                                ) { Text("Mark done") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The daily record: for each exercise / activity, which tasks or steps were completed, when, and the result
 * exactly as recorded (score, pass, inconclusive or self-reported). Earlier days are one tap away.
 */
@Composable
private fun DailyRecordCard(
    records: Map<String, List<DailyRecordEntry>>,
    todayDateKey: String
) {
    val days = remember(records, todayDateKey) {
        (listOf(todayDateKey) + DailyRecordBook.daysWithRecords(records)).distinct().take(30)
    }
    var selectedDay by rememberSaveable(todayDateKey) { mutableStateOf(todayDateKey) }
    val groups = remember(records, selectedDay) { DailyRecordBook.groupsFor(records, selectedDay) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth().testTag("daily_record_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (selectedDay == todayDateKey) "Today's record" else "Record for $selectedDay",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (days.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { day ->
                        FilterChip(
                            selected = day == selectedDay,
                            onClick = { selectedDay = day },
                            label = { Text(if (day == todayDateKey) "Today" else day) },
                            modifier = Modifier.testTag("daily_record_day_$day")
                        )
                    }
                }
            }
            if (groups.isEmpty()) {
                Text(
                    "Nothing recorded for this day yet. Completed tasks, activity drills, Coach Practice attempts and lessons appear here with their results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            groups.forEach { group ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().testTag("daily_record_group_${group.groupId}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.groupTitle, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)) {
                                Text(
                                    group.kind.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        group.entries.forEach { entry ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    formatTimeOfDay(entry.timestampMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.stepTitle, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        DailyRecordBook.resultText(entry) +
                                            if (entry.outcome != DailyRecordOutcome.INCONCLUSIVE && entry.detail.isNotBlank()) " • ${entry.detail}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (entry.outcome) {
                                            DailyRecordOutcome.PASSED -> EmeraldSuccess
                                            DailyRecordOutcome.INCONCLUSIVE -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
