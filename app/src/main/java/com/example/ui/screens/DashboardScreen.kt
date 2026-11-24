package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.RecommendedNextAction
import com.example.data.learning.PersistedLearningSnapshot
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.FeatureTutorialRegistry
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeatureTutorialDialog
import com.example.ui.components.InlineAiMentorCard
import com.example.ui.components.LearningTrackProgressSpec
import com.example.ui.components.LearningTracksStrip
import com.example.ui.components.QuickAccessPastelGrid
import com.example.ui.components.ScoreTrendSparkline
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpotlightHeroCard
import com.example.ui.components.StreakHighlightCard
import com.example.ui.components.StudioMetricCard
import com.example.ui.components.TodaysPracticePlanCard
import com.example.ui.components.TodaysProgressRingCard
import com.example.ui.components.WeeklyGoalCard
import com.example.ui.components.WelcomeBackBanner
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
    isAdmin: Boolean,
    todayDateKey: String,
    onNavigate: (String) -> Unit,
    learningSnapshot: PersistedLearningSnapshot? = null,
    reducedMotion: Boolean = false,
    onCompleteDailyTask: (String) -> Unit = {},
    onOpenProfile: () -> Unit = { onNavigate("settings") },
    onQuickAskMentor: (String) -> Unit = { onNavigate("ai_mentor") }
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
    val personalizedPlan = learningSnapshot?.dailyPlan
    val streakState = learningSnapshot?.streak
    val todayActivity = dailyActivities.firstOrNull { it.dateKey == todayDateKey }
    val totalDailyTasks = personalizedPlan?.totalCount?.coerceAtLeast(1) ?: 6
    val todayDoneCount = personalizedPlan?.completedCount
        ?: (todayActivity?.completedStepIds?.size ?: 0).coerceAtMost(totalDailyTasks)
    val completedLessons = learningProgress.count { it.completed }
    val totalCoreLessons = CurriculumCatalog.lessons.size.coerceAtLeast(9)
    val totalPracticeMins = maxOf(
        (profile?.totalPracticeMinutes ?: 0L).toInt(),
        learningSnapshot?.totalPracticeMinutes ?: 0
    )
    val practiceHoursFormatted = String.format("%.1f h", totalPracticeMins / 60.0)
    val masteredWordsCount = learningSnapshot?.wordProgress?.values?.count { it.isCompleted } ?: 0
    val wordsLearnedCount = maxOf(
        vocabulary.size,
        (profile?.wordsLearnedCount ?: 0L).toInt(),
        masteredWordsCount
    )
    val activeDaysThisWeek = remember(dailyActivities, streakState) {
        streakState?.weeklyCompletionMap?.values?.count { it }
            ?: dailyActivities.count { it.completedStepIds.isNotEmpty() }.coerceIn(0, 5)
    }
    val learningTrackLabel = learningSnapshot?.currentLevel?.label
        ?: profile?.communicationLevel?.ifBlank { "Foundation" }
        ?: "Foundation"
    val learningDay = ((streakState?.completedDateKeys?.size ?: 0) + 1)
        .coerceAtLeast((profile?.streakDays ?: 1L).toInt())
    val personalizedCue = remember(personalizedPlan, streakState, sessions.size) {
        val targetMin = personalizedPlan?.targetMinutes ?: 15
        when {
            personalizedPlan?.isRecoveryPlan == true ->
                "Welcome back! Your gentle $targetMin-minute recovery session is ready."
            streakState?.todayRequirementMet == true ->
                "Daily goal complete! Explore a quick speaking drill or review flashcards."
            sessions.isNotEmpty() ->
                "Ready to become a more confident communicator? Your next $targetMin-min session is ready."
            else ->
                "Your personalized $targetMin-minute communication practice plan is ready."
        }
    }

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

    val learningTracks = remember(learningProgress) {
        val completedIds = learningProgress.filter { it.completed }.map { it.lessonId }.toSet()
        fun pctForSlugs(slugs: Set<String>, fallback: Int): Int {
            val trackLessons = CurriculumCatalog.lessons.filter { it.moduleSlug in slugs }
            if (trackLessons.isEmpty()) return fallback
            val done = trackLessons.count { it.id in completedIds }
            return ((done * 100f) / trackLessons.size).toInt().coerceIn(0, 100)
        }
        listOf(
            LearningTrackProgressSpec(
                title = "Foundation",
                subtitle = "Build core communication skills",
                percent = pctForSlugs(setOf("daily_practice", "vocabulary", "thinking_articulation"), 0),
                icon = Icons.Default.Star,
                hubFilter = "All",
                swatchIndex = 0
            ),
            LearningTrackProgressSpec(
                title = "Speaking & Story",
                subtitle = "Tell clear and engaging stories",
                percent = pctForSlugs(setOf("public_speaking", "storytelling", "body_language", "confidence", "record_review"), 0),
                icon = Icons.Default.AutoStories,
                hubFilter = "Speaking & Story",
                swatchIndex = 1
            ),
            LearningTrackProgressSpec(
                title = "Workplace & Influence",
                subtitle = "Communicate with impact at work",
                percent = pctForSlugs(setOf("conversation", "professional", "influence", "challenges"), 0),
                icon = Icons.Default.BusinessCenter,
                hubFilter = "Workplace & Influence",
                swatchIndex = 2
            ),
            LearningTrackProgressSpec(
                title = "Voice & Clarity",
                subtitle = "Improve tone, pace and clarity",
                percent = pctForSlugs(setOf("articulation_lab", "voice_training", "listening_lab"), 0),
                icon = Icons.Default.GraphicEq,
                hubFilter = "Voice & Clarity",
                swatchIndex = 3
            )
        )
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
            add(ModuleTileSpec("coach_practice", "Coach Practice", "Speak & get instant on-device feedback", Icons.Default.RecordVoiceOver, "Tools"))
            add(ModuleTileSpec("activity_library", "Activity Engine", "150 drills • 10 modules • 10 paths", Icons.Default.School, "Tools"))
            add(ModuleTileSpec("learning_paths", "30/60/90 Plans", "Structured tracks", Icons.Default.School, "Tools"))
            add(ModuleTileSpec("real_world_tasks", "Field Tasks", "Real-world log", Icons.AutoMirrored.Filled.DirectionsRun, "Tools"))
            add(ModuleTileSpec("analytics", "Analytics", "Mistakes & trends", Icons.Default.Analytics, "Tools"))
            add(ModuleTileSpec("ai_mentor", "AI Mentor", "Personal coach chat", Icons.Default.Psychology, "Tools"))
            if (isAdmin) {
                add(ModuleTileSpec("admin_console", "Admin Studio", "RBAC & Catalog", Icons.Default.AdminPanelSettings, "Tools"))
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = if (tier.isExpandedTablet) 1280.dp else SpeakCraftSpacing.contentMaxWidthFor(tier)
        val moduleColumns = SpeakCraftSpacing.moduleColumnsFor(maxWidth)
        val displayName = profile?.displayName?.ifBlank { "Speaker" } ?: "Speaker"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth)
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
        ) {
            // Single Clean Animated Welcome-Back Greeting + Search Bar
            item {
                DashboardStudioHeader(
                    displayName = displayName,
                    query = moduleQuery,
                    onQueryChange = { moduleQuery = it },
                    wideLayout = tier.isTabletOrWide,
                    reducedMotion = reducedMotion
                )
            }

            // Instant search results preview when user types in top search bar
            if (moduleQuery.isNotBlank()) {
                item {
                    if (filteredModules.isEmpty()) {
                        EmptyStateCard(
                            title = "No matching modules",
                            message = "Try a different keyword or clear the search bar.",
                            ctaLabel = "Clear Search",
                            onCta = { moduleQuery = "" }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                title = "Search Results (${filteredModules.size})",
                                subtitle = "Matching lessons, drills & coaching tools"
                            )
                            ModuleGrid(
                                items = filteredModules,
                                columns = moduleColumns,
                                onNavigate = onNavigate
                            )
                        }
                    }
                }
            }

            if (tier.isExpandedTablet) {
                // Expanded Tablet / Studio Layout mirroring the reference screenshot
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(2.35f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                                verticalAlignment = Alignment.Top
                            ) {
                                SpotlightHeroCard(
                                    profile = profile,
                                    recommendedNextAction = recommendedNextAction,
                                    overallScore = overallScore,
                                    onNavigate = onNavigate,
                                    modifier = Modifier.weight(1.25f),
                                    learningDay = learningDay,
                                    reducedMotion = reducedMotion
                                )
                                TodaysPracticePlanCard(
                                    todayActivity = todayActivity,
                                    onNavigate = onNavigate,
                                    modifier = Modifier.weight(1f),
                                    personalizedPlan = personalizedPlan,
                                    onCompleteDailyTask = if (personalizedPlan != null) onCompleteDailyTask else null
                                )
                            }

                            // 4-up KPI Metric Strip
                            DashboardKpiStrip(
                                practiceHours = practiceHoursFormatted,
                                wordsLearned = "$wordsLearnedCount",
                                recordings = "${sessions.size}",
                                lessonsCompleted = "$completedLessons / $totalCoreLessons",
                                fourUpRow = true
                            )
                        }

                        // Right Companion Column: Streak + Progress Ring + Weekly Goal
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            StreakHighlightCard(
                                streakDays = profile?.streakDays ?: 1L,
                                streakState = streakState
                            )
                            TodaysProgressRingCard(
                                completedTasks = todayDoneCount,
                                totalTasks = totalDailyTasks,
                                onNavigate = onNavigate
                            )
                            WeeklyGoalCard(
                                activeDaysThisWeek = activeDaysThisWeek,
                                targetDays = 5,
                                weeklyCompletionMap = streakState?.weeklyCompletionMap
                            )
                        }
                    }
                }

                // Second Studio Band: Inline AI Mentor + Quick Access Pastel Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        InlineAiMentorCard(
                            onOpenMentor = { onNavigate("ai_mentor") },
                            modifier = Modifier.weight(1.0f),
                            onAskPrompt = onQuickAskMentor
                        )
                        QuickAccessPastelGrid(
                            columns = 4,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1.18f)
                        )
                    }
                }

                // Third Studio Band: My Learning Tracks 4-up strip
                item {
                    LearningTracksStrip(
                        tracks = learningTracks,
                        columns = 4,
                        onViewAll = { onNavigate("learning_paths") },
                        onSelectTrack = { hub -> selectedHubGroup = hub }
                    )
                }
            } else {
                // Compact Phone / Medium Layout preserving every visual card with clean vertical flow
                item {
                    SpotlightHeroCard(
                        profile = profile,
                        recommendedNextAction = recommendedNextAction,
                        overallScore = overallScore,
                        onNavigate = onNavigate,
                        learningDay = learningDay,
                        reducedMotion = reducedMotion
                    )
                }

                item {
                    StreakHighlightCard(
                        streakDays = profile?.streakDays ?: 1L,
                        streakState = streakState
                    )
                }

                item {
                    TodaysPracticePlanCard(
                        todayActivity = todayActivity,
                        onNavigate = onNavigate,
                        personalizedPlan = personalizedPlan,
                        onCompleteDailyTask = if (personalizedPlan != null) onCompleteDailyTask else null
                    )
                }

                item {
                    DashboardKpiStrip(
                        practiceHours = practiceHoursFormatted,
                        wordsLearned = "$wordsLearnedCount",
                        recordings = "${sessions.size}",
                        lessonsCompleted = "$completedLessons / $totalCoreLessons",
                        fourUpRow = tier.isTabletOrWide
                    )
                }

                item {
                    if (tier.isTabletOrWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            TodaysProgressRingCard(
                                completedTasks = todayDoneCount,
                                totalTasks = totalDailyTasks,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                            WeeklyGoalCard(
                                activeDaysThisWeek = activeDaysThisWeek,
                                targetDays = 5,
                                modifier = Modifier.weight(1f),
                                weeklyCompletionMap = streakState?.weeklyCompletionMap
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) {
                            TodaysProgressRingCard(
                                completedTasks = todayDoneCount,
                                totalTasks = totalDailyTasks,
                                onNavigate = onNavigate
                            )
                            WeeklyGoalCard(
                                activeDaysThisWeek = activeDaysThisWeek,
                                targetDays = 5,
                                weeklyCompletionMap = streakState?.weeklyCompletionMap
                            )
                        }
                    }
                }

                item {
                    InlineAiMentorCard(
                        onOpenMentor = { onNavigate("ai_mentor") },
                        onAskPrompt = onQuickAskMentor
                    )
                }

                item {
                    QuickAccessPastelGrid(
                        columns = if (tier.isTabletOrWide) 4 else 2,
                        onNavigate = onNavigate
                    )
                }

                item {
                    LearningTracksStrip(
                        tracks = learningTracks,
                        columns = if (tier.isTabletOrWide) 4 else 2,
                        onViewAll = { onNavigate("learning_paths") },
                        onSelectTrack = { hub -> selectedHubGroup = hub }
                    )
                }
            }

            // Recent Recordings & Pronunciation Trend
            if (tier.isExpandedTablet && sessions.isNotEmpty()) {
                val latest = sessions.first()
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        LatestRecordingSummaryCard(
                            latest = latest,
                            onOpenSpeakingLab = { onNavigate("speaking_lab") },
                            modifier = Modifier.weight(1f)
                        )
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
                if (sessions.isNotEmpty()) {
                    val latest = sessions.first()
                    item {
                        LatestRecordingSummaryCard(
                            latest = latest,
                            onOpenSpeakingLab = { onNavigate("speaking_lab") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    DashboardTrendCard(
                        reportPeriod = reportPeriod,
                        filteredCount = filteredSessions.size,
                        scores = assessablePronScores,
                        onSelectPeriod = { reportPeriod = it }
                    )
                }
            }

            // Interactive Tutorials Strip + Browse All 24 Feature Guides
            item {
                Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                    FeatureGuideStrip(featureKey = "dashboard")
                    OutlinedButton(
                        onClick = { showAllTutorials = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("browse_all_tutorials_button")
                    ) {
                        Text("Explore Tutorials for All 22 Features & Benefits")
                    }
                }
            }

            // Full Category-Filtered Communication Modules & Tools Directory
            if (moduleQuery.isBlank()) {
                item {
                    SectionHeader(
                        title = "All Communication Modules & Tools",
                        subtitle = "Browse all 15 speaking modules & coaching utilities"
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
private fun DashboardStudioHeader(
    displayName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    wideLayout: Boolean,
    reducedMotion: Boolean = false
) {
    val searchField: @Composable (Modifier) -> Unit = { mod ->
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search lessons, drills, tools...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = mod.testTag("dashboard_module_search")
        )
    }

    if (wideLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WelcomeBackBanner(
                displayName = displayName,
                reducedMotion = reducedMotion,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            searchField(Modifier.widthIn(min = 260.dp, max = 360.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WelcomeBackBanner(
                displayName = displayName,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxWidth()
            )
            searchField(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DashboardKpiStrip(
    practiceHours: String,
    wordsLearned: String,
    recordings: String,
    lessonsCompleted: String,
    fourUpRow: Boolean
) {
    val ext = MaterialTheme.speakCraftColors
    if (fourUpRow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudioMetricCard(
                label = "Practice Hours",
                value = practiceHours,
                pillText = "Weekly Goal: 5 h",
                icon = Icons.Default.HourglassTop,
                iconBg = ext.pastelLavenderBg,
                iconTint = ext.pastelLavenderInk,
                pillBg = ext.pastelLavenderBg,
                pillInk = ext.pastelLavenderInk,
                modifier = Modifier.weight(1f)
            )
            StudioMetricCard(
                label = "Words Learned",
                value = wordsLearned,
                pillText = "↑ Start learning",
                icon = Icons.Default.BarChart,
                iconBg = ext.pastelSkyBg,
                iconTint = ext.pastelSkyInk,
                pillBg = ext.pastelMintBg,
                pillInk = ext.pastelMintInk,
                modifier = Modifier.weight(1f)
            )
            StudioMetricCard(
                label = "Recordings",
                value = recordings,
                pillText = "↑ Improve fluency",
                icon = Icons.Default.Mic,
                iconBg = ext.pastelPeriwinkleBg,
                iconTint = ext.pastelPeriwinkleInk,
                pillBg = ext.pastelMintBg,
                pillInk = ext.pastelMintInk,
                modifier = Modifier.weight(1f)
            )
            StudioMetricCard(
                label = "Lessons Completed",
                value = lessonsCompleted,
                pillText = "Today's Goal",
                icon = Icons.Default.TrackChanges,
                iconBg = ext.pastelCoralBg,
                iconTint = ext.pastelCoralInk,
                pillBg = ext.pastelPeachBg,
                pillInk = ext.pastelPeachInk,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMetricCard(
                    label = "Practice Hours",
                    value = practiceHours,
                    pillText = "Weekly Goal: 5 h",
                    icon = Icons.Default.HourglassTop,
                    iconBg = ext.pastelLavenderBg,
                    iconTint = ext.pastelLavenderInk,
                    pillBg = ext.pastelLavenderBg,
                    pillInk = ext.pastelLavenderInk,
                    modifier = Modifier.weight(1f)
                )
                StudioMetricCard(
                    label = "Words Learned",
                    value = wordsLearned,
                    pillText = "↑ Start learning",
                    icon = Icons.Default.BarChart,
                    iconBg = ext.pastelSkyBg,
                    iconTint = ext.pastelSkyInk,
                    pillBg = ext.pastelMintBg,
                    pillInk = ext.pastelMintInk,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMetricCard(
                    label = "Recordings",
                    value = recordings,
                    pillText = "↑ Improve fluency",
                    icon = Icons.Default.Mic,
                    iconBg = ext.pastelPeriwinkleBg,
                    iconTint = ext.pastelPeriwinkleInk,
                    pillBg = ext.pastelMintBg,
                    pillInk = ext.pastelMintInk,
                    modifier = Modifier.weight(1f)
                )
                StudioMetricCard(
                    label = "Lessons Completed",
                    value = lessonsCompleted,
                    pillText = "Today's Goal",
                    icon = Icons.Default.TrackChanges,
                    iconBg = ext.pastelCoralBg,
                    iconTint = ext.pastelCoralInk,
                    pillBg = ext.pastelPeachBg,
                    pillInk = ext.pastelPeachInk,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LatestRecordingSummaryCard(
    latest: SpeakingSession,
    onOpenSpeakingLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        onClick = onOpenSpeakingLab,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ext.infoContainer
                ) {
                    Text(
                        "Latest Recording Review",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.info,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${latest.wordsPerMinute} WPM • Fluency ${latest.fluencyScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Speaking Lab",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(latest.promptTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                latest.overallAssessment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
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
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
            ScoreTrendSparkline(scores = scores, accentColor = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ModuleGrid(
    items: List<ModuleTileSpec>,
    columns: Int = 2,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val colCount = columns.coerceIn(1, 3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(colCount).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { mod ->
                    val (badgeBg, iconTint) = when (mod.hubGroup) {
                        "Voice & Clarity" -> ext.pastelMintBg to ext.pastelMintInk
                        "Speaking & Story" -> ext.pastelLavenderBg to ext.pastelLavenderInk
                        "Workplace & Influence" -> ext.pastelSkyBg to ext.pastelSkyInk
                        else -> ext.pastelPeachBg to ext.pastelPeachInk
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onNavigate(mod.route) }
                            .testTag("module_card_${mod.route}"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        mod.icon,
                                        contentDescription = mod.title,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mod.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
