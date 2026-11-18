package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.DailyActivity
import com.example.data.model.SpeakingSession
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
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
        if (filteredSessions.isEmpty()) 0 else {
            (filteredSessions.sumOf { it.wordsPerMinute } / filteredSessions.size).toInt()
        }
    }
    val recurringMistakes = remember(filteredSessions) {
        filteredSessions.flatMap { it.mistakes + it.fillerWordsDetected + it.repeatedExpressions }.distinct().take(8)
    }
    val trendScores = remember(filteredSessions, profile) {
        val list = filteredSessions.take(10).reversed().map {
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
                        MetricTile("Strongest Skill", strongest?.first ?: "Grammar", "${strongest?.second ?: 68}/100", ext.success, Modifier.weight(1f))
                        MetricTile("Weakest Skill", weakest?.first ?: "Articulation", "${weakest?.second ?: 58}/100", ext.warning, Modifier.weight(1f))
                        MetricTile("$period Sessions", "${filteredSessions.size}", "Total ${sessions.size} recorded", ext.info, Modifier.weight(1f))
                        MetricTile("$period Pace Avg", if (avgWpm > 0) "$avgWpm WPM" else "—", "Target 120–155 WPM", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Strongest Skill", strongest?.first ?: "Grammar", "${strongest?.second ?: 68}/100", ext.success, Modifier.weight(1f))
                        MetricTile("Weakest Skill", weakest?.first ?: "Articulation", "${weakest?.second ?: 58}/100", ext.warning, Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("$period Sessions", "${filteredSessions.size}", "Total ${sessions.size} recorded", ext.info, Modifier.weight(1f))
                        MetricTile("$period Pace Avg", if (avgWpm > 0) "$avgWpm WPM" else "—", "Target 120–155 WPM", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
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
            onCta = if (period != "Monthly") onSwitchToMonthly else null
        )
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Subjective Self-Assessment & Consistency (Separate Metric)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Self-Rated Confidence: ${profile?.confidenceSelfRating ?: 55}/100 • Consistency: $dailyActivitiesCount active practice days (${profile?.streakDays ?: 1}d streak).",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AnalyticsRecurringMistakesCard(
    period: String,
    recurringMistakes: List<String>,
    onNavigateToMentor: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recurring Mistakes ($period) & Next Steps", style = MaterialTheme.typography.titleMedium)
            if (recurringMistakes.isEmpty()) {
                Text("Record 1–2 sessions in Record & Review so SpeakCraft can detect recurring fillers or pronunciation gaps in this window.", style = MaterialTheme.typography.bodySmall)
            } else {
                recurringMistakes.forEach { m -> Text("• $m", style = MaterialTheme.typography.bodyMedium) }
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onNavigateToMentor,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Ask AI Mentor for Custom Weekly Plan")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    userEmail: String,
    isAdmin: Boolean,
    updateStatusSummary: String,
    onSetTheme: (ThemePreference) -> Unit,
    onSetTtsRate: (Float) -> Unit,
    onSetYoutubeKey: (String) -> Unit,
    onSetUpdateChannel: (String) -> Unit,
    onOpenUpdater: () -> Unit,
    onCreateBackupNow: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onClearHistory: () -> Unit,
    onSignOut: () -> Unit,
    firstUseTutorialsEnabled: Boolean = prefs.firstUseTutorialsEnabled,
    onToggleFirstUseTutorials: (Boolean) -> Unit = {},
    onReplayTutorials: () -> Unit = {}
) {
    var apiKeyDraft by remember(prefs.youtubeApiKey) { mutableStateOf(prefs.youtubeApiKey) }
    var ttsSlider by remember(prefs.ttsRate) { mutableFloatStateOf(prefs.ttsRate) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showClearHistoryConfirm) {
        ConfirmDestructiveDialog(
            title = "Delete Speaking Session History?",
            message = "This permanently deletes all saved speaking recordings and AI diagnostic summaries from your account.",
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

        val updatesCard: @Composable () -> Unit = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Software Updates & Backups", style = MaterialTheme.typography.titleMedium)
                    Text(updateStatusSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        listOf("stable", "beta").forEach { ch ->
                            FilterChip(
                                selected = prefs.updateChannel == ch,
                                onClick = { onSetUpdateChannel(ch) },
                                label = { Text("${ch.replaceFirstChar { it.uppercase() }} Channel") },
                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = onOpenUpdater,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("settings_check_updates_button")
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Check for Updates")
                        }
                        OutlinedButton(
                            onClick = onCreateBackupNow,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("settings_backup_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Backup Data")
                        }
                    }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
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

        val integrationsCard: @Composable () -> Unit = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Integrations & Keystore Protection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• AI Coaching: Active via Firebase AI Logic (Gemini 3.8 Flash, no API key needed).\n" +
                            "• YouTube Channel Audit: Uses live public RSS feeds & timedtext captions automatically. Optional YouTube Data API v3 key is encrypted at rest with Android Keystore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text("Optional YouTube Data API v3 Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { onSetYoutubeKey(apiKeyDraft) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    ) {
                        Text("Encrypt & Save YouTube Key")
                    }
                }
            }
        }

        val privacyCard: @Composable () -> Unit = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacy & Account", style = MaterialTheme.typography.titleMedium)
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
                            updatesCard()
                            integrationsCard()
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            appearanceAndTutorialsCard()
                            privacyCard()
                        }
                    }
                }
            } else {
                item { updatesCard() }
                item { appearanceAndTutorialsCard() }
                item { integrationsCard() }
                item { privacyCard() }
            }
        }
    }
}
