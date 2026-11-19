package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityDifficulty
import com.example.data.activity.ActivityDurationFilter
import com.example.data.activity.DailyPracticeEngine
import com.example.data.activity.ExerciseTemplateType
import com.example.data.activity.UserActivityProgress
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.ActivityEngineViewModel

enum class ActivityHubTab(val label: String) {
    CATALOGUE("All 150"),
    DAILY_PLAN("Daily Plan"),
    PATHS("10 Paths"),
    SAVED_RECENT("Saved & Recent"),
    ANALYTICS("Analytics")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityDiscoveryScreen(
    vm: ActivityEngineViewModel,
    profile: UserProfile?,
    initialModuleId: String? = null,
    initialTab: ActivityHubTab = ActivityHubTab.CATALOGUE,
    onOpenActivity: (String) -> Unit
) {
    LaunchedEffect(initialModuleId) {
        if (!initialModuleId.isNullOrBlank()) {
            vm.updateModuleFilter(initialModuleId)
        }
    }

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val filter by vm.searchFilter.collectAsStateWithLifecycle()
    val currentPage by vm.currentPage.collectAsStateWithLifecycle()
    val progressList by vm.progressList.collectAsStateWithLifecycle()
    val attempts by vm.attemptHistory.collectAsStateWithLifecycle()
    val savedList by vm.savedActivities.collectAsStateWithLifecycle()
    val selectedMinutes by vm.selectedDailyMinutes.collectAsStateWithLifecycle()
    val analytics by vm.analyticsSnapshot.collectAsStateWithLifecycle()
    val pathSummaries by vm.learningPathSummaries.collectAsStateWithLifecycle()

    val progressById = remember(progressList) { progressList.associateBy { it.activityId } }
    val savedIds = remember(savedList) { savedList.map { it.activityId }.toSet() }
    val matchedActivities = remember(filter) { ActivityCatalogue.searchAndFilter(filter) }
    val paginatedActivities = remember(matchedActivities, currentPage) {
        ActivityCatalogue.paginate(matchedActivities, currentPage, pageSize = 20)
    }
    val dailyPlan = remember(selectedMinutes, profile, progressList, attempts) {
        vm.buildEffectiveDailyPlan(profile)
    }
    val recommended = remember(profile, progressList, attempts) {
        vm.recommendedActivities(profile)
    }

    val ext = MaterialTheme.speakCraftColors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("activity_discovery_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = if (tier.isExpandedTablet) 1180.dp else SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth)
                .testTag("activity_discovery_list"),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            // Engine Header Summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SpeakCraft Universal Activity Engine",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "150 communication activities • 10 curriculum modules • 25 reusable templates • 9 AI evaluators",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryStatBadge("Completed", "${analytics.totalActivitiesCompleted}/150", Modifier.weight(1f))
                            SummaryStatBadge("Practice", "${analytics.totalPracticeMinutes}m", Modifier.weight(1f))
                            SummaryStatBadge("Streak", "${analytics.currentConsistencyStreak}d", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Hub Navigation Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityHubTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("activity_hub_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            when (selectedTab) {
                ActivityHubTab.CATALOGUE -> {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = filter.query,
                            onValueChange = { vm.updateSearchQuery(it) },
                            placeholder = { Text("Search 150 activities by name, skill, or objective...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (filter.query.isNotBlank()) {
                                    IconButton(onClick = { vm.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activity_search_input")
                        )
                    }

                    // Module Filter Strip (All + 10 Modules)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Curriculum Modules (10)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filter.moduleId == null,
                                    onClick = { vm.updateModuleFilter(null) },
                                    label = { Text("All Modules (150)") },
                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                )
                                ActivityCatalogue.modules.forEach { mod ->
                                    FilterChip(
                                        selected = filter.moduleId == mod.moduleId,
                                        onClick = { vm.updateModuleFilter(if (filter.moduleId == mod.moduleId) null else mod.moduleId) },
                                        label = { Text("${mod.order}. ${mod.title}") },
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            .testTag("module_filter_${mod.moduleId}")
                                    )
                                }
                            }
                        }
                    }

                    // Difficulty, Duration & Exercise Type Filters
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ActivityDifficulty.entries.forEach { diff ->
                                    FilterChip(
                                        selected = filter.difficulty == diff,
                                        onClick = { vm.updateDifficultyFilter(if (filter.difficulty == diff) null else diff) },
                                        label = { Text(diff.label) },
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            .testTag("difficulty_filter_${diff.name.lowercase()}")
                                    )
                                }
                                ActivityDurationFilter.entries.filter { it != ActivityDurationFilter.ALL }.forEach { dur ->
                                    FilterChip(
                                        selected = filter.durationFilter == dur,
                                        onClick = {
                                            vm.updateDurationFilter(
                                                if (filter.durationFilter == dur) ActivityDurationFilter.ALL else dur
                                            )
                                        },
                                        label = { Text(dur.label) },
                                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = filter.exerciseType == null,
                                    onClick = { vm.updateExerciseTypeFilter(null) },
                                    label = { Text("All 25 Templates") },
                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                )
                                ExerciseTemplateType.entries.forEach { type ->
                                    FilterChip(
                                        selected = filter.exerciseType == type,
                                        onClick = { vm.updateExerciseTypeFilter(if (filter.exerciseType == type) null else type) },
                                        label = { Text(type.displayName) },
                                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    )
                                }
                            }
                        }
                    }

                    // Results Count Header
                    item {
                        SectionHeader(
                            title = "Activities (${matchedActivities.size})",
                            subtitle = "Showing ${paginatedActivities.size} of ${matchedActivities.size} matching activities",
                            actionLabel = if (filter != com.example.data.activity.ActivitySearchFilter()) "Reset" else null,
                            onAction = { vm.clearAllFilters() }
                        )
                    }

                    if (matchedActivities.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No matching activities",
                                message = "Try clearing one of your difficulty, duration, or module filters.",
                                ctaLabel = "Reset Filters",
                                onCta = { vm.clearAllFilters() }
                            )
                        }
                    } else {
                        items(paginatedActivities, key = { it.activityId }) { activity ->
                            ActivityCatalogueRowCard(
                                activity = activity,
                                progress = progressById[activity.activityId],
                                isSaved = activity.activityId in savedIds,
                                onToggleSave = { vm.toggleSavedActivity(activity) },
                                onOpen = { onOpenActivity(activity.activityId) }
                            )
                        }

                        if (paginatedActivities.size < matchedActivities.size) {
                            item {
                                OutlinedButton(
                                    onClick = { vm.loadMoreCatalogueItems() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("activity_load_more_button")
                                ) {
                                    Text("Load More (${matchedActivities.size - paginatedActivities.size} remaining)")
                                }
                            }
                        }
                    }
                }

                ActivityHubTab.DAILY_PLAN -> {
                    item {
                        SectionHeader(
                            title = "Personalized Daily Practice Engine",
                            subtitle = dailyPlan.personalizationSummary
                        )
                    }
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DailyPracticeEngine.supportedDurationsMinutes.forEach { mins ->
                                FilterChip(
                                    selected = selectedMinutes == mins,
                                    onClick = { vm.selectDailyPlanMinutes(mins, profile) },
                                    label = { Text("$mins min") },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("daily_duration_${mins}m")
                                )
                            }
                        }
                    }
                    items(dailyPlan.blocks, key = { it.activityId }) { block ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenActivity(block.activityId) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = ext.pastelLavenderBg, shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                block.pillar.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.pastelLavenderInk,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text("${block.durationMinutes} min • ${block.difficulty.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (block.completed) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = ext.success, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(block.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { vm.swapDailyPlanActivity(block.activityId, profile) },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("daily_swap_${block.activityId}")
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap activity")
                                }
                            }
                        }
                    }
                }

                ActivityHubTab.PATHS -> {
                    item {
                        SectionHeader(
                            title = "10 Personalized Learning Paths",
                            subtitle = "Follow structured milestones or jump directly into any activity non-linearly"
                        )
                    }
                    items(pathSummaries, key = { it.path.pathId }) { summary ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("learning_path_card_${summary.path.pathId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(summary.path.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${summary.path.difficulty.label} • ${summary.completedCount}/${summary.totalCount} completed",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text("${summary.percentComplete}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(summary.path.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LinearProgressIndicator(
                                    progress = { summary.percentComplete / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                if (summary.unlockedMilestones.isNotEmpty()) {
                                    Text(
                                        "Unlocked Milestones: ${summary.unlockedMilestones.joinToString(" • ") { it.title }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.success
                                    )
                                }
                                val nextId = summary.nextRecommendedActivityId ?: summary.path.orderedActivityIds.first()
                                Button(
                                    onClick = { onOpenActivity(nextId) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                ) {
                                    Text(if (summary.completedCount == 0) "Start Path" else "Continue Path")
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                ActivityHubTab.SAVED_RECENT -> {
                    item {
                        SectionHeader("Recommended For You", "Personalized next drills based on your progress")
                    }
                    items(recommended, key = { "rec_${it.activityId}" }) { act ->
                        ActivityCatalogueRowCard(
                            activity = act,
                            progress = progressById[act.activityId],
                            isSaved = act.activityId in savedIds,
                            onToggleSave = { vm.toggleSavedActivity(act) },
                            onOpen = { onOpenActivity(act.activityId) }
                        )
                    }
                    item {
                        SectionHeader("Saved Activities (${savedList.size})", "Your bookmarked favorites")
                    }
                    if (savedList.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No saved activities yet",
                                message = "Tap the bookmark icon on any activity to keep it in your quick-practice list.",
                                ctaLabel = "Explore Catalogue",
                                onCta = { selectedTab = ActivityHubTab.CATALOGUE }
                            )
                        }
                    } else {
                        items(savedList, key = { "saved_${it.activityId}" }) { saved ->
                            val def = ActivityCatalogue.getActivityById(saved.activityId)
                            if (def != null) {
                                ActivityCatalogueRowCard(
                                    activity = def,
                                    progress = progressById[def.activityId],
                                    isSaved = true,
                                    onToggleSave = { vm.toggleSavedActivity(def) },
                                    onOpen = { onOpenActivity(def.activityId) }
                                )
                            }
                        }
                    }
                }

                ActivityHubTab.ANALYTICS -> {
                    item {
                        SectionHeader(
                            title = "Activity Progress & Analytics",
                            subtitle = "Objective rubric performance tracked separately from self-assessed confidence"
                        )
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SummaryStatBadge("Avg Objective", "${analytics.averageObjectiveScore}/100", Modifier.weight(1f))
                                    SummaryStatBadge(
                                        "Avg Confidence",
                                        analytics.averageConfidenceSelfRating?.let { "$it/100" } ?: "Not rated",
                                        Modifier.weight(1f)
                                    )
                                    SummaryStatBadge("Weekly Runs", "${analytics.weeklyAttemptsCount}", Modifier.weight(1f))
                                }
                                Text(
                                    "Active Days (Last 7d): ${analytics.activeDaysLast7}/7 • Recordings Logged: ${analytics.recordingsCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (analytics.improvementAreas.isNotEmpty()) {
                                    Text(
                                        "Focus Improvement Areas: ${analytics.improvementAreas.joinToString(", ") { it.label }}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.warning
                                    )
                                }
                                if (analytics.achievedMilestones.isNotEmpty()) {
                                    Text(
                                        "Milestones: ${analytics.achievedMilestones.joinToString(" • ")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.success
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionHeader("Module Completion (10 Modules)")
                    }
                    items(analytics.moduleStats, key = { it.module.moduleId }) { stat ->
                        val ratio = if (stat.totalCount > 0) stat.completedCount.toFloat() / stat.totalCount else 0f
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable {
                                vm.updateModuleFilter(stat.module.moduleId)
                                selectedTab = ActivityHubTab.CATALOGUE
                            }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stat.module.order}. ${stat.module.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${stat.completedCount}/${stat.totalCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityCatalogueRowCard(
    activity: ActivityDefinition,
    progress: UserActivityProgress?,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onOpen: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("activity_item_${activity.activityId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(activity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (progress?.completed == true) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = ext.success,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    activity.learningObjective,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    "${activity.skillCategory.label} • ${activity.difficulty.label} • ${activity.duration}m • ${activity.exerciseType.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove bookmark" else "Save activity",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
