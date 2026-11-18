package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.RecommendedNextAction
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.data.update.UpdateEvaluation
import com.example.ui.components.MetricTile
import com.example.ui.components.ScoreTrendSparkline
import com.example.ui.components.SectionHeader
import com.example.ui.components.UpdateBannerCard
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AmberGoldBright
import com.example.ui.theme.AmberSpotlight
import com.example.ui.theme.EmeraldSuccess

data class ModuleTileSpec(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private fun iconForModuleSlug(slug: String): ImageVector = when (slug) {
    "articulation_lab" -> Icons.Default.GraphicEq
    "voice_training" -> Icons.Default.RecordVoiceOver
    "daily_practice" -> Icons.Default.CheckCircle
    "vocabulary" -> Icons.Default.Book
    "conversation" -> Icons.Default.ChatBubble
    "listening_lab" -> Icons.Default.Headphones
    "public_speaking" -> Icons.Default.Campaign
    "storytelling" -> Icons.Default.AutoStories
    "body_language" -> Icons.Default.Gesture
    "confidence" -> Icons.Default.SelfImprovement
    "thinking_articulation" -> Icons.Default.Spellcheck
    "professional" -> Icons.Default.BusinessCenter
    "influence" -> Icons.Default.VerifiedUser
    "record_review" -> Icons.Default.Mic
    "challenges" -> Icons.Default.EmojiEvents
    else -> Icons.Default.School
}

@Composable
fun DashboardScreen(
    profile: UserProfile?,
    sessions: List<SpeakingSession>,
    vocabulary: List<VocabularyEntry>,
    dailyActivities: List<DailyActivity>,
    learningProgress: List<UserLearningProgress>,
    realWorldTasks: List<RealWorldTaskRecord>,
    recommendedNextAction: RecommendedNextAction,
    updateEvaluation: UpdateEvaluation?,
    showUpdateBanner: Boolean,
    isAdmin: Boolean,
    todayDateKey: String,
    onDismissUpdateBanner: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    var reportPeriod by remember { mutableStateOf("Weekly") }

    val overallScore = remember(profile) {
        if (profile == null) 62 else {
            ((profile.fluencyScore + profile.pronunciationScore + profile.vocabularyScore +
                profile.grammarScore + profile.articulationScore + profile.listeningScore) / 6).toInt()
        }
    }
    val todayDoneCount = dailyActivities.firstOrNull { it.dateKey == todayDateKey }?.completedStepIds?.size ?: 0
    val completedLessons = learningProgress.count { it.completed }
    val pendingTasksCount = (CurriculumCatalog.realWorldTaskSeeds.size - realWorldTasks.count { it.completed }).coerceAtLeast(0)
    val practiceHours = String.format("%.1fh", (profile?.totalPracticeMinutes ?: 0L) / 60.0)

    val assessablePronScores = remember(sessions, profile) {
        val list = sessions.filter { it.pronunciationAssessable }.take(8).reversed().map { it.pronunciationScore.toInt() }
        if (list.isEmpty()) listOf((profile?.pronunciationScore ?: 65L).toInt()) else list
    }

    val fluencyDelta = remember(sessions) {
        if (sessions.size >= 2) {
            val latest = sessions.first().fluencyScore
            val oldest = sessions.last().fluencyScore
            val diff = latest - oldest
            if (diff >= 0) "+$diff pts" else "$diff pts"
        } else "Baseline"
    }

    val all15Modules = remember {
        CurriculumCatalog.contentModules.map { mod ->
            ModuleTileSpec(mod.route, mod.title, mod.subtitle, iconForModuleSlug(mod.slug))
        }
    }

    val utilityTools = remember(isAdmin) {
        buildList {
            add(ModuleTileSpec("learning_paths", "30/60/90 Plans", "Structured tracks", Icons.Default.School))
            add(ModuleTileSpec("real_world_tasks", "Field Tasks", "Real-world log", Icons.AutoMirrored.Filled.DirectionsRun))
            add(ModuleTileSpec("youtube_library", "@askvinh Library", "Channel & transcripts", Icons.Default.PlayCircle))
            add(ModuleTileSpec("analytics", "Analytics", "Mistakes & trends", Icons.Default.Analytics))
            add(ModuleTileSpec("ai_mentor", "AI Mentor", "Personal coach chat", Icons.Default.Psychology))
            add(ModuleTileSpec("app_updates", "App Updates", "Version & backups", Icons.Default.SystemUpdate))
            if (isAdmin) {
                add(ModuleTileSpec("admin_console", "Admin Studio", "RBAC & Releases", Icons.Default.AdminPanelSettings))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 880.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showUpdateBanner && updateEvaluation?.latestRelease != null && updateEvaluation.isUpdateAvailable) {
                item {
                    val rel = updateEvaluation.latestRelease
                    UpdateBannerCard(
                        versionName = rel.versionName,
                        mandatory = updateEvaluation.isMandatory,
                        onOpenUpdater = { onNavigate("app_updates") },
                        onDismiss = { onDismissUpdateBanner(rel.versionName) }
                    )
                }
            }

            // Hero Action-First Learning Plan
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_coach_hero),
                        contentDescription = "SpeakCraft executive coaching stage",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x330F172A), Color(0xEB0F172A))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AmberGoldBright.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "${recommendedNextAction.badge} • ${profile?.streakDays ?: 1}d Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFFBEB),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(
                                text = "Overall $overallScore/100",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGoldBright
                            )
                        }
                        Column {
                            Text(
                                text = "Next Step: ${recommendedNextAction.title}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = recommendedNextAction.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigate(recommendedNextAction.targetRoute) },
                                    modifier = Modifier.testTag("recommended_action_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(recommendedNextAction.ctaLabel)
                                }
                                OutlinedButton(
                                    onClick = { onNavigate("speaking_lab") }
                                ) {
                                    Text("Quick Record", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Key Metrics Summary
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Practice Hours", practiceHours, "${profile?.dailyGoalMinutes ?: 15}m/day goal", AmberSpotlight, Modifier.weight(1f))
                        MetricTile("Words Learned", "${vocabulary.size.coerceAtLeast((profile?.wordsLearnedCount ?: 0L).toInt())}", "Active notebook", AcousticTeal, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Recordings", "${sessions.size}", "Fluency $fluencyDelta", EmeraldSuccess, Modifier.weight(1f))
                        MetricTile("Today & Lessons", "$todayDoneCount/9", "$completedLessons lessons • $pendingTasksCount tasks", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
            }

            // Recent Recordings Highlight
            if (sessions.isNotEmpty()) {
                val latest = sessions.first()
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Latest Recording Review", style = MaterialTheme.typography.labelLarge, color = AcousticTeal)
                                Text("${latest.wordsPerMinute} WPM", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(latest.promptTitle, style = MaterialTheme.typography.titleMedium)
                            Text(latest.overallAssessment, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }

            // Trend Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pronunciation & Clarity Trend", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "$reportPeriod Performance • ${sessions.size} sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Weekly", "Monthly").forEach { period ->
                                    FilterChip(
                                        selected = reportPeriod == period,
                                        onClick = { reportPeriod = period },
                                        label = { Text(period) }
                                    )
                                }
                            }
                        }
                        ScoreTrendSparkline(scores = assessablePronScores, accentColor = AcousticTeal)
                    }
                }
            }

            // 15 Content Modules Directory
            item {
                SectionHeader(
                    title = "15 Communication Modules",
                    subtitle = "Structured @askvinh training labs, lessons & challenges"
                )
            }

            item {
                ModuleGrid(items = all15Modules, onNavigate = onNavigate)
            }

            // Coaching Tools, Intelligence & Releases
            item {
                SectionHeader(
                    title = "Intelligence, Analytics & System",
                    subtitle = "30/60/90 plans, YouTube ingestion & release updater"
                )
            }

            item {
                ModuleGrid(items = utilityTools, onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun ModuleGrid(
    items: List<ModuleTileSpec>,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { mod ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onNavigate(mod.route) }
                            .testTag("module_card_${mod.route}"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        mod.icon,
                                        contentDescription = mod.title,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(mod.title, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    mod.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
