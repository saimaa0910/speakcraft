package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.RecommendedNextAction
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.FeatureTutorialRegistry
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.data.update.UpdateEvaluation
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeatureTutorialDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.ScoreTrendSparkline
import com.example.ui.components.SectionHeader
import com.example.ui.components.UpdateBannerCard
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

data class ModuleTileSpec(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val hubGroup: String = "Tools"
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

private fun hubGroupForSlug(slug: String): String = when (slug) {
    "articulation_lab", "voice_training", "listening_lab" -> "Voice & Clarity"
    "public_speaking", "storytelling", "body_language", "confidence", "record_review" -> "Speaking & Story"
    "daily_practice", "vocabulary", "conversation", "thinking_articulation", "professional", "influence", "challenges" -> "Workplace & Influence"
    else -> "Tools"
}

@OptIn(ExperimentalLayoutApi::class)
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
    val ext = MaterialTheme.speakCraftColors
    var reportPeriod by remember { mutableStateOf("Weekly") }
    var moduleQuery by remember { mutableStateOf("") }
    var selectedHubGroup by remember { mutableStateOf("All") }
    var showAllTutorials by remember { mutableStateOf(false) }

    if (showAllTutorials) {
        FeatureTutorialDialog(
            initialTutorial = FeatureTutorialRegistry.forRoute("dashboard"),
            onDismiss = { showAllTutorials = false },
            onNavigateToFeature = onNavigate
        )
    }

    val overallScore = remember(profile) {
        if (profile == null) 62 else {
            ((profile.fluencyScore + profile.pronunciationScore + profile.vocabularyScore +
                profile.grammarScore + profile.articulationScore + profile.listeningScore) / 6).toInt()
        }
    }
    val todayActivity = dailyActivities.firstOrNull { it.dateKey == todayDateKey }
    val todayDoneCount = todayActivity?.completedStepIds?.size ?: 0
    val completedLessons = learningProgress.count { it.completed }
    val pendingTasksCount = (CurriculumCatalog.realWorldTaskSeeds.size - realWorldTasks.count { it.completed }).coerceAtLeast(0)
    val practiceHours = String.format("%.1fh", (profile?.totalPracticeMinutes ?: 0L) / 60.0)

    val filteredSessions = remember(sessions, reportPeriod) {
        StreakAndAnalyticsHelper.filterSessionsByPeriod(sessions, reportPeriod)
    }

    val assessablePronScores = remember(filteredSessions, profile) {
        val list = filteredSessions
            .filter { it.pronunciationAssessable }
            .take(8)
            .reversed()
            .map { it.pronunciationScore.toInt() }
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

    val allModulesAndTools = remember(isAdmin) {
        val core15 = CurriculumCatalog.contentModules.map { mod ->
            ModuleTileSpec(
                route = mod.route,
                title = mod.title,
                subtitle = mod.subtitle,
                icon = iconForModuleSlug(mod.slug),
                hubGroup = hubGroupForSlug(mod.slug)
            )
        }
        val utilityTools = buildList {
            add(ModuleTileSpec("learning_paths", "30/60/90 Plans", "Structured tracks", Icons.Default.School, "Tools"))
            add(ModuleTileSpec("real_world_tasks", "Field Tasks", "Real-world log", Icons.AutoMirrored.Filled.DirectionsRun, "Tools"))
            add(ModuleTileSpec("youtube_library", "@askvinh Library", "Channel & transcripts", Icons.Default.PlayCircle, "Tools"))
            add(ModuleTileSpec("analytics", "Analytics", "Mistakes & trends", Icons.Default.Analytics, "Tools"))
            add(ModuleTileSpec("ai_mentor", "AI Mentor", "Personal coach chat", Icons.Default.Psychology, "Tools"))
            add(ModuleTileSpec("app_updates", "App Updates", "Version & backups", Icons.Default.SystemUpdate, "Tools"))
            if (isAdmin) {
                add(ModuleTileSpec("admin_console", "Admin Studio", "RBAC & Releases", Icons.Default.AdminPanelSettings, "Tools"))
            }
        }
        core15 + utilityTools
    }

    val filteredModules = remember(allModulesAndTools, moduleQuery, selectedHubGroup) {
        val q = moduleQuery.trim()
        allModulesAndTools.filter { mod ->
            val groupMatch = selectedHubGroup == "All" || mod.hubGroup == selectedHubGroup
            val queryMatch = q.isBlank() ||
                mod.title.contains(q, ignoreCase = true) ||
                mod.subtitle.contains(q, ignoreCase = true)
            groupMatch && queryMatch
        }
    }

    val quickHubs = remember {
        listOf(
            ModuleTileSpec("learning_paths", "30/60/90 Plans", "Programs", Icons.Default.School),
            ModuleTileSpec("vocabulary", "Vocabulary", "Flashcards", Icons.Default.Book),
            ModuleTileSpec("conversation", "Simulate", "13 roles", Icons.Default.ChatBubble),
            ModuleTileSpec("analytics", "Analytics", "Skill trends", Icons.Default.Analytics)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)
        val moduleColumns = SpeakCraftSpacing.moduleColumnsFor(maxWidth)

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
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

            if (tier.isExpandedTablet) {
                // Expanded Tablet: Hero Banner side-by-side with Routine Progress & Quick Hubs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1.25f)) {
                            DashboardHeroCard(
                                profile = profile,
                                recommendedNextAction = recommendedNextAction,
                                overallScore = overallScore,
                                onNavigate = onNavigate
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            DashboardRoutineCard(
                                todayDoneCount = todayDoneCount,
                                quickHubs = quickHubs,
                                onNavigate = onNavigate
                            )
                            FeatureGuideStrip(featureKey = "dashboard")
                            OutlinedButton(
                                onClick = { showAllTutorials = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("browse_all_tutorials_button")
                            ) {
                                Text("Explore Tutorials for All 24 Features & Benefits")
                            }
                        }
                    }
                }
            } else {
                // Phone / Compact: Hero Action-First Learning Plan
                item {
                    DashboardHeroCard(
                        profile = profile,
                        recommendedNextAction = recommendedNextAction,
                        overallScore = overallScore,
                        onNavigate = onNavigate
                    )
                }

                // Today's Momentum Bar + Core 4 Hub Shortcuts
                item {
                    DashboardRoutineCard(
                        todayDoneCount = todayDoneCount,
                        quickHubs = quickHubs,
                        onNavigate = onNavigate
                    )
                }

                // Dashboard Tutorial + Browse All 24 Guides
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        FeatureGuideStrip(featureKey = "dashboard")
                        OutlinedButton(
                            onClick = { showAllTutorials = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("browse_all_tutorials_button")
                        ) {
                            Text("Explore Tutorials for All 24 Features & Benefits")
                        }
                    }
                }
            }

            // Key Metrics Summary: 4-in-a-row on Tablet, 2x2 on Phone
            item {
                val wordsValue = "${vocabulary.size.coerceAtLeast((profile?.wordsLearnedCount ?: 0L).toInt())}"
                if (tier.isTabletOrWide) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Practice Hours", practiceHours, "${profile?.dailyGoalMinutes ?: 15}m/day goal", ext.warning, Modifier.weight(1f))
                        MetricTile("Words Learned", wordsValue, "Active notebook", ext.info, Modifier.weight(1f))
                        MetricTile("Recordings", "${sessions.size}", "Fluency $fluencyDelta", ext.success, Modifier.weight(1f))
                        MetricTile("Today & Lessons", "$todayDoneCount/9", "$completedLessons lessons • $pendingTasksCount tasks", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile("Practice Hours", practiceHours, "${profile?.dailyGoalMinutes ?: 15}m/day goal", ext.warning, Modifier.weight(1f))
                            MetricTile("Words Learned", wordsValue, "Active notebook", ext.info, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile("Recordings", "${sessions.size}", "Fluency $fluencyDelta", ext.success, Modifier.weight(1f))
                            MetricTile("Today & Lessons", "$todayDoneCount/9", "$completedLessons lessons • $pendingTasksCount tasks", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        }
                    }
                }
            }

            if (tier.isExpandedTablet && sessions.isNotEmpty()) {
                val latest = sessions.first()
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Latest Recording Review", style = MaterialTheme.typography.labelLarge, color = ext.info)
                                    Text("${latest.wordsPerMinute} WPM", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(latest.promptTitle, style = MaterialTheme.typography.titleMedium)
                                Text(latest.overallAssessment, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            }
                        }
                        DashboardTrendCard(
                            reportPeriod = reportPeriod,
                            filteredCount = filteredSessions.size,
                            scores = assessablePronScores,
                            onSelectPeriod = { reportPeriod = it },
                            modifier = Modifier.weight(1.25f)
                        )
                    }
                }
            } else {
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
                                    Text("Latest Recording Review", style = MaterialTheme.typography.labelLarge, color = ext.info)
                                    Text("${latest.wordsPerMinute} WPM", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(latest.promptTitle, style = MaterialTheme.typography.titleMedium)
                                Text(latest.overallAssessment, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }

                // Filtered Trend Card
                item {
                    DashboardTrendCard(
                        reportPeriod = reportPeriod,
                        filteredCount = filteredSessions.size,
                        scores = assessablePronScores,
                        onSelectPeriod = { reportPeriod = it }
                    )
                }
            }

            // Searchable + Category-Filtered Skill Modules & Tools Directory
            item {
                SectionHeader(
                    title = "Communication Modules & Tools",
                    subtitle = "Filter by skill focus or search all 15 @askvinh modules & utilities"
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                OutlinedTextField(
                    value = moduleQuery,
                    onValueChange = { moduleQuery = it },
                    placeholder = { Text("Search modules (e.g., voice, interview, storytelling)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (moduleQuery.isNotBlank()) {
                            IconButton(onClick = { moduleQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_module_search")
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                ) {
                    listOf("All", "Voice & Clarity", "Speaking & Story", "Workplace & Influence", "Tools").forEach { group ->
                        FilterChip(
                            selected = selectedHubGroup == group,
                            onClick = { selectedHubGroup = group },
                            label = { Text(group) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
            }

            item {
                if (filteredModules.isEmpty()) {
                    EmptyStateCard(
                        title = "No matching modules",
                        message = "Try a different keyword or reset the category filter.",
                        ctaLabel = "Show All Modules",
                        onCta = {
                            moduleQuery = ""
                            selectedHubGroup = "All"
                        }
                    )
                } else {
                    ModuleGrid(
                        items = filteredModules,
                        columns = moduleColumns,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    profile: UserProfile?,
    recommendedNextAction: RecommendedNextAction,
    overallScore: Int,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
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
                        colors = listOf(ext.heroOverlayStart, ext.heroOverlayEnd)
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
                    color = ext.warning.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "${recommendedNextAction.badge} • ${profile?.streakDays ?: 1}d Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.onHeroBadge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Overall $overallScore/100",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.onHeroBadge
                )
            }
            Column {
                Text(
                    text = "Next Step: ${recommendedNextAction.title}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ext.onHeroTitle
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = recommendedNextAction.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.onHeroSubtitle,
                    maxLines = 2
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onNavigate(recommendedNextAction.targetRoute) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("recommended_action_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(recommendedNextAction.ctaLabel)
                    }
                    OutlinedButton(
                        onClick = { onNavigate("speaking_lab") },
                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
                        border = BorderStroke(1.dp, ext.onHeroSubtitle.copy(alpha = 0.65f))
                    ) {
                        Text("Quick Record", color = ext.onHeroTitle)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRoutineCard(
    todayDoneCount: Int,
    quickHubs: List<ModuleTileSpec>,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(SpeakCraftSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Routine Progress", style = MaterialTheme.typography.labelLarge)
                Text(
                    "$todayDoneCount / 9 steps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { (todayDoneCount / 9f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = ext.success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
            ) {
                quickHubs.forEach { hub ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onNavigate(hub.route) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                hub.icon,
                                contentDescription = hub.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                hub.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTrendCard(
    reportPeriod: String,
    filteredCount: Int,
    scores: List<Int>,
    onSelectPeriod: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pronunciation & Clarity Trend", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$reportPeriod Window • $filteredCount sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Weekly", "Monthly").forEach { period ->
                        FilterChip(
                            selected = reportPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text(period) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
            }
            ScoreTrendSparkline(scores = scores, accentColor = ext.info)
        }
    }
}

@Composable
private fun ModuleGrid(
    items: List<ModuleTileSpec>,
    columns: Int = 2,
    onNavigate: (String) -> Unit
) {
    val colCount = columns.coerceIn(1, 3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(colCount).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { mod ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
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
                repeat((colCount - rowItems.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
