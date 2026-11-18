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
import androidx.compose.material.icons.Icons
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
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemePreference
import com.example.ui.components.MetricTile
import com.example.ui.components.ScoreTrendSparkline
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkillBreakdownBars
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AmberSpotlight
import com.example.ui.theme.EmeraldSuccess

@Composable
fun AnalyticsScreen(
    profile: UserProfile?,
    sessions: List<SpeakingSession>,
    dailyActivities: List<DailyActivity>,
    onNavigateToMentor: () -> Unit
) {
    var period by remember { mutableStateOf("Weekly") }

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
    val recurringMistakes = remember(sessions) {
        sessions.flatMap { it.mistakes + it.fillerWordsDetected + it.repeatedExpressions }.distinct().take(8)
    }
    val trendScores = remember(sessions, profile) {
        val list = sessions.take(10).reversed().map {
            if (it.pronunciationAssessable) {
                ((it.fluencyScore + it.pronunciationScore + it.articulationScore) / 3).toInt()
            } else {
                ((it.fluencyScore + it.articulationScore) / 2).toInt()
            }
        }
        if (list.isEmpty()) listOf((profile?.fluencyScore ?: 62L).toInt()) else list
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader("Progress Analytics", "Comparable speech metrics separated from subjective confidence")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly", "Monthly").forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p) })
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Strongest Skill", strongest?.first ?: "Grammar", "${strongest?.second ?: 68}/100", EmeraldSuccess, Modifier.weight(1f))
                    MetricTile("Weakest Skill", weakest?.first ?: "Articulation", "${weakest?.second ?: 58}/100", AmberSpotlight, Modifier.weight(1f))
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("$period Composite Speech Trend", style = MaterialTheme.typography.titleMedium)
                        ScoreTrendSparkline(scores = trendScores, accentColor = AcousticTeal)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Objective Speech Skills (Rolling Average)", style = MaterialTheme.typography.titleMedium)
                        SkillBreakdownBars(metrics = objectiveSkills)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Subjective Self-Assessment & Consistency (Separate Metric)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Self-Rated Confidence: ${profile?.confidenceSelfRating ?: 55}/100 • Consistency: ${dailyActivities.size} active practice days (${profile?.streakDays ?: 1}d streak).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recurring Mistakes & Next Steps", style = MaterialTheme.typography.titleMedium)
                        if (recurringMistakes.isEmpty()) {
                            Text("Record 1–2 sessions in Record & Review so SpeakCraft can detect recurring fillers or pronunciation gaps.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            recurringMistakes.forEach { m -> Text("• $m", style = MaterialTheme.typography.bodyMedium) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = onNavigateToMentor, modifier = Modifier.fillMaxWidth()) {
                            Text("Ask AI Mentor for Custom Weekly Plan")
                        }
                    }
                }
            }
        }
    }
}

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
    onSignOut: () -> Unit
) {
    var apiKeyDraft by remember(prefs.youtubeApiKey) { mutableStateOf(prefs.youtubeApiKey) }
    var ttsSlider by remember(prefs.ttsRate) { mutableFloatStateOf(prefs.ttsRate) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader("Settings & Privacy", "Signed in as $userEmail • SpeakCraft v${BuildConfig.VERSION_NAME}")
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Software Updates & Backups", style = MaterialTheme.typography.titleMedium)
                        Text(updateStatusSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("stable", "beta").forEach { ch ->
                                FilterChip(
                                    selected = prefs.updateChannel == ch,
                                    onClick = { onSetUpdateChannel(ch) },
                                    label = { Text("${ch.replaceFirstChar { it.uppercase() }} Channel") }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenUpdater,
                                modifier = Modifier.weight(1f).testTag("settings_check_updates_button")
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Check for Updates")
                            }
                            OutlinedButton(
                                onClick = onCreateBackupNow,
                                modifier = Modifier.weight(1f).testTag("settings_backup_button")
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Backup Data")
                            }
                        }
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = onOpenAdminConsole,
                                modifier = Modifier.fillMaxWidth().testTag("settings_admin_console_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open Admin & Content Studio")
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
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreference.entries.forEach { mode ->
                                FilterChip(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSetTheme(mode) },
                                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        OutlinedButton(onClick = { onSetYoutubeKey(apiKeyDraft) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Encrypt & Save YouTube Key")
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
                        Text("Privacy & Account", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                        ) {
                            Text("Delete Speaking Session History")
                        }
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }
}
