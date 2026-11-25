package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.ai.CapabilityStatus
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.IntelligenceCapabilityMatrix
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.model.DailyActivity
import com.example.data.model.SpeakingSession
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.UserProfile
import com.example.data.preferences.AISettings
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.RecognitionEnginePreference
import com.example.data.preferences.ThemePreference
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.MetricTile
import com.example.ui.components.ScoreTrendSparkline
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkillBreakdownBars
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    profile: UserProfile?,
    sessions: List<SpeakingSession>,
    dailyActivities: List<DailyActivity>,
    onNavigateToMentor: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    var period by remember { mutableStateOf("Weekly") }

    val filteredSessions = remember(sessions, period) {
        StreakAndAnalyticsHelper.filterSessionsByPeriod(sessions, period)
    }

    val objectiveSkills = remember(profile) {
        listOf(
            "Fluency" to (profile?.fluencyScore ?: 62L).toInt(),
            "Pronunciation" to (profile?.pronunciationScore ?: 65L).toInt(),
            "Vocabulary" to (profile?.vocabularyScore ?: 60L).toInt(),
            "Grammar" to (profile?.grammarScore ?: 68L).toInt(),
            "Articulation" to (profile?.articulationScore ?: 58L).toInt(),
            "Listening" to (profile?.listeningScore ?: 66L).toInt()
        )
    }
    val strongest = objectiveSkills.maxByOrNull { it.second }
    val weakest = objectiveSkills.minByOrNull { it.second }
    val avgWpm = remember(filteredSessions) {
        // Only sessions where pace was measured (audio-only attempts have none).
        val measured = filteredSessions.filter { it.wordsPerMinute > 0L }
        if (measured.isEmpty()) 0 else {
            (measured.sumOf { it.wordsPerMinute } / measured.size).toInt()
        }
    }
    val recurringMistakes = remember(filteredSessions) {
        filteredSessions.flatMap { it.mistakes + it.fillerWordsDetected + it.repeatedExpressions }.distinct().take(8)
    }
    val trendScores = remember(filteredSessions, profile) {
        // Engine-evaluated sessions carry no legacy skill scores, so they are not part of this score trend.
        val list = filteredSessions.filterNot { LabAttemptEvaluator.isEngineEvaluated(it) }.take(10).reversed().map {
            if (it.pronunciationAssessable) {
                ((it.fluencyScore + it.pronunciationScore + it.articulationScore) / 3).toInt()
            } else {
                ((it.fluencyScore + it.articulationScore) / 2).toInt()
            }
        }
        if (list.isEmpty()) listOf((profile?.fluencyScore ?: 62L).toInt()) else list
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader("Progress Analytics", "Comparable speech metrics separated from subjective confidence")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "analytics")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                ) {
                    listOf("Daily", "Weekly", "Monthly").forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
            }

            if (tier.isTabletOrWide) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Strongest Skill", strongest?.first ?: "Grammar", "${strongest?.second ?: 68}/100", ext.success, Modifier.weight(1f), Icons.Default.EmojiEvents)
                        MetricTile("Weakest Skill", weakest?.first ?: "Articulation", "${weakest?.second ?: 58}/100", ext.warning, Modifier.weight(1f), Icons.Default.TrendingUp)
                        MetricTile("$period Sessions", "${filteredSessions.size}", "Total ${sessions.size} recorded", ext.info, Modifier.weight(1f), Icons.Default.Mic)
                        MetricTile("$period Pace Avg", if (avgWpm > 0) "$avgWpm WPM" else "—", "Target 120–155 WPM", MaterialTheme.colorScheme.primary, Modifier.weight(1f), Icons.Default.Speed)
                    }
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Strongest Skill", strongest?.first ?: "Grammar", "${strongest?.second ?: 68}/100", ext.success, Modifier.weight(1f), Icons.Default.EmojiEvents)
                        MetricTile("Weakest Skill", weakest?.first ?: "Articulation", "${weakest?.second ?: 58}/100", ext.warning, Modifier.weight(1f), Icons.Default.TrendingUp)
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("$period Sessions", "${filteredSessions.size}", "Total ${sessions.size} recorded", ext.info, Modifier.weight(1f), Icons.Default.Mic)
                        MetricTile("$period Pace Avg", if (avgWpm > 0) "$avgWpm WPM" else "—", "Target 120–155 WPM", MaterialTheme.colorScheme.primary, Modifier.weight(1f), Icons.Default.Speed)
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
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AnalyticsTrendCard(
                                period = period,
                                filteredSessions = filteredSessions,
                                trendScores = trendScores,
                                onSwitchToMonthly = { period = "Monthly" }
                            )
                            AnalyticsRecurringMistakesCard(
                                period = period,
                                recurringMistakes = recurringMistakes,
                                onNavigateToMentor = onNavigateToMentor
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AnalyticsObjectiveSkillsCard(objectiveSkills = objectiveSkills)
                            AnalyticsSubjectiveConfidenceCard(
                                profile = profile,
                                dailyActivitiesCount = dailyActivities.size
                            )
                        }
                    }
                }
            } else {
                item {
                    AnalyticsTrendCard(
                        period = period,
                        filteredSessions = filteredSessions,
                        trendScores = trendScores,
                        onSwitchToMonthly = { period = "Monthly" }
                    )
                }
                item {
                    AnalyticsObjectiveSkillsCard(objectiveSkills = objectiveSkills)
                }
                item {
                    AnalyticsSubjectiveConfidenceCard(
                        profile = profile,
                        dailyActivitiesCount = dailyActivities.size
                    )
                }
                item {
                    AnalyticsRecurringMistakesCard(
                        period = period,
                        recurringMistakes = recurringMistakes,
                        onNavigateToMentor = onNavigateToMentor
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTrendCard(
    period: String,
    filteredSessions: List<SpeakingSession>,
    trendScores: List<Int>,
    onSwitchToMonthly: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    if (filteredSessions.isEmpty()) {
        EmptyStateCard(
            title = "No sessions in $period window",
            message = "Switch to Monthly or record a fresh session in Record & Review to track your $period trend.",
            ctaLabel = if (period != "Monthly") "Switch to Monthly" else null,
            onCta = if (period != "Monthly") onSwitchToMonthly else null,
            icon = Icons.Default.TrendingUp
        )
    } else {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$period Composite Speech Trend (${filteredSessions.size} sessions)", style = MaterialTheme.typography.titleMedium)
                ScoreTrendSparkline(scores = trendScores, accentColor = ext.info)
            }
        }
    }
}

@Composable
private fun AnalyticsObjectiveSkillsCard(objectiveSkills: List<Pair<String, Int>>) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Objective Speech Skills (Rolling Average)", style = MaterialTheme.typography.titleMedium)
            SkillBreakdownBars(metrics = objectiveSkills)
        }
    }
}

@Composable
private fun AnalyticsSubjectiveConfidenceCard(
    profile: UserProfile?,
    dailyActivitiesCount: Int
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Subjective Self-Assessment & Consistency (Separate Metric)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Self-Rated Confidence: ${profile?.confidenceSelfRating ?: 55}/100 • Consistency: $dailyActivitiesCount active practice days (${profile?.streakDays ?: 1}d streak).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalyticsRecurringMistakesCard(
    period: String,
    recurringMistakes: List<String>,
    onNavigateToMentor: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recurring Mistakes ($period) & Next Steps", style = MaterialTheme.typography.titleMedium)
            if (recurringMistakes.isEmpty()) {
                Text(
                    "Record 1–2 sessions in Record & Review so SpeakCraft can detect recurring fillers or pronunciation gaps in this window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recurringMistakes.forEach { m ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ext.warningContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = m,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.onWarning,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onNavigateToMentor,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Ask AI Mentor for Custom Weekly Plan")
            }
        }
    }
}

@Composable
private fun SettingsCardHeader(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bg,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    userEmail: String,
    isAdmin: Boolean,
    onSetTheme: (ThemePreference) -> Unit,
    onSetTtsRate: (Float) -> Unit,
    onOpenAdminConsole: () -> Unit,
    onClearHistory: () -> Unit,
    onSignOut: () -> Unit,
    firstUseTutorialsEnabled: Boolean = prefs.firstUseTutorialsEnabled,
    onToggleFirstUseTutorials: (Boolean) -> Unit = {},
    onReplayTutorials: () -> Unit = {},
    aiSettings: AISettings = AISettings(
        localAiEnabled = prefs.localAiEnabled,
        liveTranscriptionEnabled = prefs.liveTranscriptionEnabled,
        localSpeechRecognitionEnabled = prefs.localSpeechRecognitionEnabled,
        localAudioAnalysisEnabled = prefs.localAudioAnalysisEnabled,
        localVideoAnalysisEnabled = prefs.localVideoAnalysisEnabled,
        selectedRecognitionLanguage = prefs.selectedRecognitionLanguage,
        selectedRecognitionEngine = RecognitionEnginePreference.fromStored(prefs.selectedRecognitionEngine)
    ),
    capabilityMatrix: IntelligenceCapabilityMatrix = IntelligenceCapabilityManager().computeMatrix(aiSettings),
    onSetLocalAiEnabled: (Boolean) -> Unit = {},
    onSetLiveTranscriptionEnabled: (Boolean) -> Unit = {},
    /** No longer used: it controlled only the removed local file recogniser (FND-AI-03). Kept for call sites. */
    onSetLocalSpeechRecEnabled: (Boolean) -> Unit = {},
    onSetLocalAudioAnalysisEnabled: (Boolean) -> Unit = {},
    onSetLocalVideoAnalysisEnabled: (Boolean) -> Unit = {},
    onSetRecognitionLanguage: (String) -> Unit = {},
    onSetRecognitionEngine: (RecognitionEnginePreference) -> Unit = {}
) {
    val ext = MaterialTheme.speakCraftColors
    var ttsSlider by remember(prefs.ttsRate) { mutableFloatStateOf(prefs.ttsRate) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showClearHistoryConfirm) {
        ConfirmDestructiveDialog(
            title = "Delete Speaking Session History?",
            message = "This permanently deletes your saved speaking session summaries from your account and removes any linked recordings stored on this device.",
            confirmLabel = "Delete History",
            onConfirm = {
                onClearHistory()
                showClearHistoryConfirm = false
            },
            onDismiss = { showClearHistoryConfirm = false }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val appInfoCard: @Composable () -> Unit = {
            Card(
                modifier = Modifier.testTag("settings_app_info_card"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCardHeader(
                        icon = Icons.Default.Info,
                        tint = ext.info,
                        bg = ext.infoContainer,
                        title = "App Version & Distribution"
                    )
                    Text(
                        "SpeakCraft v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "App updates are delivered through official release distribution channels. SpeakCraft does not download or install APK packages in-app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("settings_admin_console_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Admin & Content Studio")
                        }
                    }
                }
            }
        }

        val appearanceAndTutorialsCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCardHeader(
                        icon = Icons.Default.Palette,
                        tint = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        title = "Appearance"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        ThemePreference.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { onSetTheme(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            )
                        }
                    }

                    Text("Reference Voice Speed: ${String.format("%.2fx", ttsSlider)}", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = ttsSlider,
                        onValueChange = { ttsSlider = it },
                        onValueChangeFinished = { onSetTtsRate(ttsSlider) },
                        valueRange = 0.65f..1.35f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("First-Use Feature Overlays", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Show guided usage & benefit tips when visiting Speaking Lab, AI Mentor, and core tools for the first time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = firstUseTutorialsEnabled,
                            onCheckedChange = onToggleFirstUseTutorials,
                            modifier = Modifier.testTag("settings_first_use_tutorials_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onReplayTutorials,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("settings_replay_tutorials_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Replay Welcome & Module Tours")
                    }
                }
            }
        }

        val aiCoreCard: @Composable () -> Unit = {
            Card(
                modifier = Modifier.testTag("settings_ai_intelligence_section"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsCardHeader(
                        icon = Icons.Default.Psychology,
                        tint = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        title = "AI & Intelligence"
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.infoContainer
                    ) {
                        Text(
                            text = "Active Engine: On-Device Local Intelligence (Zero-Cloud-AI Policy)",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.info,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("settings_ai_engine_status")
                        )
                    }

                    // A. Local Intelligence
                    Text("A. Local Intelligence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Local Intelligence Engine", style = MaterialTheme.typography.titleSmall)
                                CapabilityStatusPill(capabilityMatrix.localAi.status)
                            }
                            Text(
                                capabilityMatrix.localAi.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiSettings.localAiEnabled,
                            onCheckedChange = onSetLocalAiEnabled,
                            modifier = Modifier.testTag("settings_local_ai_switch")
                        )
                    }
                }
            }
        }

        val aiSpeechControlsCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // B. Speech & Transcription
                    Text("B. Speech & Transcription", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Live Transcription", style = MaterialTheme.typography.titleSmall)
                                CapabilityStatusPill(capabilityMatrix.liveTranscription.status)
                            }
                            Text(
                                "Display recognized words while you are speaking during practice.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (aiSettings.liveTranscriptionEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aiSettings.liveTranscriptionEnabled) ext.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiSettings.liveTranscriptionEnabled,
                            onCheckedChange = onSetLiveTranscriptionEnabled,
                            modifier = Modifier.testTag("settings_live_transcription_switch")
                        )
                    }

                    // Status only: transcripts come from the device's own recogniser; the app ships none (FND-AI-03).
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("settings_on_device_speech_status")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("On-Device Speech Recognition", style = MaterialTheme.typography.titleSmall)
                            CapabilityStatusPill(capabilityMatrix.localSpeechRecognition.status)
                        }
                        Text(
                            capabilityMatrix.localSpeechRecognition.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Acoustic Audio DSP Analysis", style = MaterialTheme.typography.titleSmall)
                                CapabilityStatusPill(capabilityMatrix.localAudioAnalysis.status)
                            }
                            Text(
                                capabilityMatrix.localAudioAnalysis.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiSettings.localAudioAnalysisEnabled,
                            onCheckedChange = onSetLocalAudioAnalysisEnabled,
                            modifier = Modifier.testTag("settings_local_audio_switch")
                        )
                    }
                }
            }
        }

        val aiSpeechDiagnosticsCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recognition Engine", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        RecognitionEnginePreference.SELECTABLE.forEach { pref ->
                            FilterChip(
                                selected = aiSettings.selectedRecognitionEngine == pref,
                                onClick = { onSetRecognitionEngine(pref) },
                                label = { Text(pref.label) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("settings_rec_engine_${pref.name}")
                            )
                        }
                    }

                    Text("Recognition Language", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        listOf("en-US", "en-GB", "en-AU", "en-IN").forEach { lang ->
                            FilterChip(
                                selected = aiSettings.selectedRecognitionLanguage == lang,
                                onClick = { onSetRecognitionLanguage(lang) },
                                label = { Text(lang) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("settings_rec_lang_$lang")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_device_adaptive_status")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Device-Adaptive AI Provisioning",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ext.pastelMintBg
                                ) {
                                    Text(
                                        text = capabilityMatrix.deviceProfile.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.pastelMintInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = capabilityMatrix.deviceModelSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_speech_diagnostics")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Speech & Transcription Diagnostics", style = MaterialTheme.typography.labelLarge)
                            Text("Recognition Engine: ${capabilityMatrix.activeRecognitionEngineLabel}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Offline Availability: ${if (capabilityMatrix.offlineSpeechAvailable) "Verified Offline" else "Unavailable / Unverified"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Microphone Permission: ${if (capabilityMatrix.hasRecordAudioPermission) "Granted" else "Required (Grant in Recording)"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Recognition Status: ${capabilityMatrix.liveTranscription.status.label} — ${capabilityMatrix.liveTranscription.detail}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("On-Device Speech: ${capabilityMatrix.onDeviceSpeechStatus}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        val aiVideoPrivacyCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // C. Video Intelligence
                    Text("C. Video Intelligence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Camera & Body Language Analysis", style = MaterialTheme.typography.titleSmall)
                                CapabilityStatusPill(capabilityMatrix.localVideoAnalysis.status)
                            }
                            Text(
                                capabilityMatrix.localVideoAnalysis.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Camera Permission: ${if (capabilityMatrix.hasCameraPermission) "Granted" else "Required only for Video Mode"} • ${capabilityMatrix.mediaPipeStatus}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiSettings.localVideoAnalysisEnabled,
                            onCheckedChange = onSetLocalVideoAnalysisEnabled,
                            modifier = Modifier.testTag("settings_local_video_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // D. Privacy
                    Text("D. Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.successContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_ai_privacy_statement")
                    ) {
                        Text(
                            text = "Speaking audio, video recordings, device file paths, and speech or conversation transcripts stay strictly on this device and are never uploaded to Firestore or any Cloud AI service. Firebase Authentication and Firestore store only your account identity, practice scores, coaching summaries, vocabulary progress, and session metadata. Note: when live speech recognition is enabled and an offline language pack is unavailable, Android's system speech provider may process audio under your device OS settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        val integrationsCard: @Composable () -> Unit = {
            val context = LocalContext.current
            var geminiStatus by remember(context) {
                mutableStateOf(PhoneGeminiBridge.inspectConnectionStatus(context))
            }
            var geminiActionMessage by remember { mutableStateOf<String?>(null) }
            var studioPromptDraft by remember(context) {
                mutableStateOf(PhoneGeminiBridge.getEffectiveStudioGeminiPrompt(context))
            }
            var isCustomStudioPrompt by remember(context) {
                mutableStateOf(PhoneGeminiBridge.hasCustomStudioGeminiPrompt(context))
            }
            var studioPromptStatus by remember { mutableStateOf<String?>(null) }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCardHeader(
                        icon = Icons.Default.Key,
                        tint = ext.success,
                        bg = ext.successContainer,
                        title = "On-Device Engine Status"
                    )
                    Text(
                        "• AI Engine: Stage 1 On-Device Local Intelligence (audio DSP, MediaPipe landmarks, and speech recognition — zero cloud AI uploads).\n" +
                            "• Curriculum Library: Bundled communication frameworks and structured speaking drills available offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_phone_gemini_card"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsCardHeader(
                            icon = Icons.Default.AutoAwesome,
                            tint = if (geminiStatus.isAnyGeminiPathAvailable) ext.success else MaterialTheme.colorScheme.primary,
                            bg = if (geminiStatus.isAnyGeminiPathAvailable) ext.successContainer else MaterialTheme.colorScheme.primaryContainer,
                            title = "Phone Gemini App Connection"
                        )
                        Text(
                            text = geminiStatus.statusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = geminiStatus.detailMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (geminiStatus.geminiAppInstalled) ext.successContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (geminiStatus.geminiAppInstalled) "Gemini App: Installed" else "Gemini App: Not Installed",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (geminiStatus.geminiAppInstalled) ext.success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (geminiStatus.assistantVoiceAvailable) ext.successContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (geminiStatus.assistantVoiceAvailable) "Voice Overlay: Ready" else "Share Handoff: Ready",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (geminiStatus.assistantVoiceAvailable) ext.success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (geminiStatus.aiCoreServiceInstalled) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = ext.successContainer
                                ) {
                                    Text(
                                        text = "Android AICore: Active",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.success
                                    )
                                }
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    geminiStatus = PhoneGeminiBridge.inspectConnectionStatus(context)
                                    val result = PhoneGeminiBridge.openGeminiApp(context)
                                    geminiActionMessage = result.userMessage
                                },
                                modifier = Modifier.testTag("settings_open_gemini_app_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (geminiStatus.geminiAppInstalled) "Open Gemini App" else "Connect / Install Gemini")
                            }
                            OutlinedButton(
                                onClick = {
                                    geminiStatus = PhoneGeminiBridge.inspectConnectionStatus(context)
                                    val result = PhoneGeminiBridge.launchGeminiLiveVoice(context)
                                    geminiActionMessage = result.userMessage
                                },
                                modifier = Modifier.testTag("settings_launch_gemini_voice_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Launch Gemini Voice")
                            }
                        }
                        if (!geminiActionMessage.isNullOrBlank()) {
                            Text(
                                text = geminiActionMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Share text or responses directly from your phone's Gemini app into SpeakCraft (via Android Share or speakcraft:// links) to practice them in AI Mentor.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_studio_gemini_prompt_section"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Record & Review Gemini Prompt",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isCustomStudioPrompt) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = if (isCustomStudioPrompt) "Custom" else "Default",
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("settings_studio_gemini_prompt_badge"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCustomStudioPrompt) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            Text(
                                text = "Customize the coaching instructions sent with your Daily Video or Selected Topic recordings in Record & Review Studio. Task, topic, duration, and transcript are attached automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhoneGeminiBridge.STUDIO_GEMINI_PROMPT_PRESETS.forEachIndexed { index, preset ->
                                    val selected = studioPromptDraft.trim() == preset.instructions.trim()
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            studioPromptDraft = preset.instructions
                                            studioPromptStatus = "Loaded \"${preset.label}\" preset — tap Save Prompt to apply."
                                        },
                                        label = { Text(preset.label) },
                                        modifier = Modifier.testTag("settings_studio_gemini_preset_$index")
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = studioPromptDraft,
                                onValueChange = { updated ->
                                    studioPromptDraft = updated.take(1500)
                                    studioPromptStatus = null
                                },
                                label = { Text("Coaching prompt sent to Gemini") },
                                placeholder = { Text("Tell Gemini what feedback you want on your recording...") },
                                supportingText = {
                                    Text("${studioPromptDraft.length}/1500 • Optional tags: {task}, {topic}, {duration}, {media}")
                                },
                                minLines = 4,
                                maxLines = 8,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_studio_gemini_prompt_input")
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val savedCustom = PhoneGeminiBridge.saveCustomStudioGeminiPrompt(
                                            context = context,
                                            prompt = studioPromptDraft
                                        )
                                        studioPromptDraft = PhoneGeminiBridge.getEffectiveStudioGeminiPrompt(context)
                                        isCustomStudioPrompt = savedCustom
                                        studioPromptStatus = if (savedCustom) {
                                            "Saved custom Record & Review Gemini prompt."
                                        } else {
                                            "Using standard default Record & Review Gemini prompt."
                                        }
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("settings_studio_gemini_prompt_save_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Prompt")
                                }

                                OutlinedButton(
                                    onClick = {
                                        PhoneGeminiBridge.resetCustomStudioGeminiPrompt(context)
                                        studioPromptDraft = PhoneGeminiBridge.DEFAULT_STUDIO_GEMINI_PROMPT
                                        isCustomStudioPrompt = false
                                        studioPromptStatus = "Restored default Record & Review Gemini prompt."
                                    },
                                    enabled = isCustomStudioPrompt ||
                                        studioPromptDraft.trim() != PhoneGeminiBridge.DEFAULT_STUDIO_GEMINI_PROMPT.trim(),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("settings_studio_gemini_prompt_reset_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reset Default")
                                }
                            }

                            if (!studioPromptStatus.isNullOrBlank()) {
                                Text(
                                    text = studioPromptStatus.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("settings_studio_gemini_prompt_status")
                                )
                            }
                        }
                    }
                }
            }
        }

        val privacyCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCardHeader(
                        icon = Icons.Default.Security,
                        tint = MaterialTheme.colorScheme.error,
                        bg = MaterialTheme.colorScheme.errorContainer,
                        title = "Privacy & Account"
                    )
                    OutlinedButton(
                        onClick = { showClearHistoryConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("clear_history_button")
                    ) {
                        Text("Delete Speaking Session History")
                    }
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("sign_out_button")
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader("Settings & Privacy", "Signed in as $userEmail • SpeakCraft v${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "settings")
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
                            appInfoCard()
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            appearanceAndTutorialsCard()
                        }
                    }
                }
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
                            aiCoreCard()
                            aiVideoPrivacyCard()
                            integrationsCard()
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            aiSpeechControlsCard()
                            aiSpeechDiagnosticsCard()
                            privacyCard()
                        }
                    }
                }
            } else {
                item { aiCoreCard() }
                item { aiSpeechControlsCard() }
                item { aiSpeechDiagnosticsCard() }
                item { aiVideoPrivacyCard() }
                item { appInfoCard() }
                item { appearanceAndTutorialsCard() }
                item { integrationsCard() }
                item { privacyCard() }
            }
        }
    }
}

@Composable
private fun CapabilityStatusPill(status: CapabilityStatus) {
    val ext = MaterialTheme.speakCraftColors
    val (bg, fg) = when (status) {
        CapabilityStatus.ENABLED, CapabilityStatus.AVAILABLE -> ext.successContainer to ext.success
        CapabilityStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        CapabilityStatus.PERMISSION_REQUIRED, CapabilityStatus.MODEL_REQUIRED -> ext.warningContainer to ext.onWarning
        CapabilityStatus.UNAVAILABLE, CapabilityStatus.INITIALIZATION_FAILED ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

