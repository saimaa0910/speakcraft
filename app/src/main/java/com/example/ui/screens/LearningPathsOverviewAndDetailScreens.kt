package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathLesson
import com.example.data.learningpaths.LearningPathModule
import com.example.data.learningpaths.LearningPathProgram
import com.example.data.learningpaths.LearningPathRecommendationEngine
import com.example.data.learningpaths.PathDifficultyTier
import com.example.data.learningpaths.ProgramRecommendation
import com.example.data.learningpaths.ProgramUserStats
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.LearningPathsUiState

/**
 * Hub screen displaying all 7 complete Learning Programs with personalized recommendations,
 * search & filters, pre-enroll syllabus preview, enrollment lifecycle controls, and deep navigation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningPathsOverviewScreen(
    uiState: LearningPathsUiState,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onPreviewProgram: (String?) -> Unit,
    onEnrollProgram: (String) -> Unit,
    onTogglePauseProgram: (String) -> Unit,
    onUnenrollProgram: (String) -> Unit,
    onOpenProgramDetail: (String) -> Unit,
    onOpenLesson: (String, Int) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    var unenrollConfirmProgram by remember { mutableStateOf<LearningPathProgram?>(null) }

    val filters = listOf("All", "Enrolled", "Completed", "Foundational", "Intermediate", "Advanced", "Specialized")

    val filteredPrograms = remember(
        uiState.programs,
        uiState.selectedCategoryFilter,
        uiState.searchQuery,
        uiState.programStats
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        uiState.programs.filter { program ->
            val stats = uiState.programStats[program.programId]
            val matchesFilter = when (uiState.selectedCategoryFilter) {
                "Enrolled" -> stats?.enrolled == true
                "Completed" -> stats?.programCompleted == true
                "Foundational" -> program.difficultyTier == PathDifficultyTier.FOUNDATIONAL
                "Intermediate" -> program.difficultyTier == PathDifficultyTier.INTERMEDIATE
                "Advanced" -> program.difficultyTier == PathDifficultyTier.ADVANCED
                "Specialized" -> program.programId in setOf("vocab_challenge", "pronunciation_program", "public_speaking_program", "interview_program")
                else -> true
            }
            val matchesQuery = q.isBlank() ||
                program.title.lowercase().contains(q) ||
                program.subtitle.lowercase().contains(q) ||
                program.overview.lowercase().contains(q) ||
                program.primarySkillFocus.any { it.lowercase().contains(q) } ||
                program.categoryTag.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    val totalCompletedDays = uiState.programStats.values.sumOf { it.completedDaysCount }
    val activeProgramsCount = uiState.programStats.values.count { it.enrolled && !it.programCompleted }
    val completedProgramsCount = uiState.programStats.values.count { it.programCompleted }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("learning_paths_overview"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FeatureGuideStrip(featureKey = "learning_paths")
        }

        if (!uiState.bannerMessage.isNullOrBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ext.pastelMintBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ext.pastelMintInk
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = uiState.bannerMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.pastelMintInk,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissBanner) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ext.pastelMintInk)
                        }
                    }
                }
            }
        }

        // Hub Hero Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.studioConsoleSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = ext.onStudioConsole,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "7 Complete Learning Programs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.onStudioConsole
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "280 interactive daily lessons & 28 graded assessments running 100% offline with instant transcript highlights and coaching feedback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onStudioConsole.copy(alpha = 0.86f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBadgePill(
                            label = "Active Tracks",
                            value = "$activeProgramsCount",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgePill(
                            label = "Days Completed",
                            value = "$totalCompletedDays / 280",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgePill(
                            label = "Graduated",
                            value = "$completedProgramsCount / 7",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Top Personalized Recommendations Card
        if (uiState.recommendations.isNotEmpty() && uiState.searchQuery.isBlank() && uiState.selectedCategoryFilter == "All") {
            item {
                RecommendationSpotlightCard(
                    recommendations = uiState.recommendations,
                    programStats = uiState.programStats,
                    onPreview = onPreviewProgram,
                    onOpenProgramDetail = onOpenProgramDetail,
                    onOpenLesson = onOpenLesson,
                    onEnroll = onEnrollProgram
                )
            }
        }

        // Search & Filter Controls
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lp_search_input"),
                placeholder = { Text("Search programs, topics, or skills...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { chip ->
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == chip,
                        onClick = { onCategoryFilterChange(chip) },
                        label = { Text(chip) },
                        modifier = Modifier.testTag("lp_filter_$chip")
                    )
                }
            }
        }

        if (filteredPrograms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching learning programs found.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = {
                            onSearchChange("")
                            onCategoryFilterChange("All")
                        }) {
                            Text("Show all 7 programs")
                        }
                    }
                }
            }
        } else {
            items(filteredPrograms, key = { it.programId }) { program ->
                val stats = uiState.programStats[program.programId]
                    ?: LearningPathRecommendationEngine.computeProgramStats(program, null)
                val unmetPrereqs = LearningPathRecommendationEngine.unmetPrerequisites(
                    program = program,
                    allPrograms = uiState.programs,
                    userState = uiState.userState
                )
                ProgramCatalogCard(
                    program = program,
                    stats = stats,
                    unmetPrereqs = unmetPrereqs,
                    adviceText = LearningPathRecommendationEngine.nextActionAdvice(
                        program = program,
                        enrollment = uiState.userState.enrollments[program.programId]
                    ),
                    onPreview = { onPreviewProgram(program.programId) },
                    onEnroll = { onEnrollProgram(program.programId) },
                    onContinue = { onOpenLesson(program.programId, stats.nextDayNumber) },
                    onOpenSyllabus = { onOpenProgramDetail(program.programId) },
                    onTogglePause = { onTogglePauseProgram(program.programId) },
                    onRequestUnenroll = { unenrollConfirmProgram = program }
                )
            }
        }
    }

    // Preview Syllabus Dialog BEFORE Enrolling
    val previewProgram = uiState.previewProgramId?.let { LearningPathCurriculumRegistry.getProgram(it) }
    if (previewProgram != null) {
        val previewStats = uiState.programStats[previewProgram.programId]
        ProgramPreviewDialog(
            program = previewProgram,
            isEnrolled = previewStats?.enrolled == true,
            onDismiss = { onPreviewProgram(null) },
            onEnroll = { onEnrollProgram(previewProgram.programId) },
            onOpenSyllabus = {
                onPreviewProgram(null)
                onOpenProgramDetail(previewProgram.programId)
            }
        )
    }

    // Confirm Unenroll Dialog (protects against accidental progress loss)
    unenrollConfirmProgram?.let { target ->
        AlertDialog(
            onDismissRequest = { unenrollConfirmProgram = null },
            title = { Text("Leave '${target.title}'?") },
            text = {
                Text("This clears your active enrollment and local day records for this path. You can also pause instead to keep every score saved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnenrollProgram(target.programId)
                        unenrollConfirmProgram = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave Path")
                }
            },
            dismissButton = {
                TextButton(onClick = { unenrollConfirmProgram = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatBadgePill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.speakCraftColors.onStudioConsole
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.speakCraftColors.onStudioConsole.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun RecommendationSpotlightCard(
    recommendations: List<ProgramRecommendation>,
    programStats: Map<String, ProgramUserStats>,
    onPreview: (String) -> Unit,
    onOpenProgramDetail: (String) -> Unit,
    onOpenLesson: (String, Int) -> Unit,
    onEnroll: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val top = recommendations.first()
    val stats = programStats[top.program.programId]

    Card(
        colors = CardDefaults.cardColors(containerColor = ext.pastelLavenderBg),
        border = BorderStroke(1.dp, ext.indigo.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ext.pastelLavenderInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "RECOMMENDED FOR YOU · ${top.priorityTag.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.pastelLavenderInk
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = top.program.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.pastelLavenderInk
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = top.reason,
                style = MaterialTheme.typography.bodySmall,
                color = ext.pastelLavenderInk.copy(alpha = 0.88f)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stats?.enrolled == true) {
                    Button(onClick = { onOpenLesson(top.program.programId, stats.nextDayNumber) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Continue Day ${stats.nextDayNumber}")
                    }
                    OutlinedButton(onClick = { onOpenProgramDetail(top.program.programId) }) {
                        Text("Syllabus")
                    }
                } else {
                    Button(onClick = { onEnroll(top.program.programId) }) {
                        Text("Start ${top.program.durationDays}-Day Path")
                    }
                    OutlinedButton(onClick = { onPreview(top.program.programId) }) {
                        Text("Preview Syllabus")
                    }
                }
            }
            if (recommendations.size > 1) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = ext.pastelLavenderInk.copy(alpha = 0.14f))
                Spacer(Modifier.height(8.dp))
                recommendations.drop(1).forEach { alt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenProgramDetail(alt.program.programId) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${alt.program.title} (${alt.program.durationDays}d)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ext.pastelLavenderInk,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = alt.priorityTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.pastelLavenderInk.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramCatalogCard(
    program: LearningPathProgram,
    stats: ProgramUserStats,
    unmetPrereqs: List<LearningPathProgram>,
    adviceText: String,
    onPreview: () -> Unit,
    onEnroll: () -> Unit,
    onContinue: () -> Unit,
    onOpenSyllabus: () -> Unit,
    onTogglePause: () -> Unit,
    onRequestUnenroll: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val containerColor = when {
        stats.programCompleted -> ext.pastelMintBg.copy(alpha = 0.6f)
        stats.enrolled && !stats.paused -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lp_program_card_${program.programId}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (stats.enrolled) 1.5.dp else 1.dp,
            color = if (stats.programCompleted) ext.success else ext.cardBorderSubtle
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SmallPillTag(
                            text = "${program.durationDays} Days · ${program.modules.size} Modules",
                            bg = ext.pastelSkyBg,
                            fg = ext.pastelSkyInk
                        )
                        SmallPillTag(
                            text = program.difficultyTier.label,
                            bg = ext.pastelPeriwinkleBg,
                            fg = ext.pastelPeriwinkleInk
                        )
                        if (stats.programCompleted) {
                            SmallPillTag(
                                text = "Completed",
                                bg = ext.pastelMintBg,
                                fg = ext.pastelMintInk
                            )
                        } else if (stats.paused) {
                            SmallPillTag(
                                text = "Paused",
                                bg = ext.pastelPeachBg,
                                fg = ext.pastelPeachInk
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = program.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = program.overview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                program.primarySkillFocus.forEach { focus ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = focus,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (unmetPrereqs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Helpful first: ${unmetPrereqs.joinToString(", ") { it.title }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.warning
                )
            }

            if (stats.enrolled) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { stats.completionFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stats.completedDaysCount}/${stats.totalDays} days · ${stats.passedModulesCount}/${stats.totalModulesCount} assessments",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stats.averageLessonScore > 0) {
                        Text(
                            text = "Avg ${stats.averageLessonScore}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.success
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = adviceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "~${program.estimatedDailyMinutes} min/day · ${program.recommendedLevelRange}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!stats.enrolled) {
                    Button(
                        onClick = onEnroll,
                        modifier = Modifier.testTag("enroll_${program.programId}")
                    ) {
                        Text("Enroll Now")
                    }
                    OutlinedButton(
                        onClick = onPreview,
                        modifier = Modifier.testTag("lp_preview_btn_${program.programId}")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Preview Syllabus")
                    }
                    TextButton(
                        onClick = onOpenSyllabus,
                        modifier = Modifier.testTag("lp_explore_btn_${program.programId}")
                    ) {
                        Text("Inspect Days")
                    }
                } else {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.testTag("lp_continue_btn_${program.programId}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (stats.programCompleted) "Review Day ${stats.nextDayNumber}" else "Continue Day ${stats.nextDayNumber}")
                    }
                    OutlinedButton(
                        onClick = onOpenSyllabus,
                        modifier = Modifier.testTag("lp_syllabus_btn_${program.programId}")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Syllabus")
                    }
                    TextButton(onClick = onTogglePause) {
                        Icon(
                            if (stats.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (stats.paused) "Resume" else "Pause")
                    }
                    TextButton(onClick = onRequestUnenroll) {
                        Text("Leave", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramPreviewDialog(
    program: LearningPathProgram,
    isEnrolled: Boolean,
    onDismiss: () -> Unit,
    onEnroll: () -> Unit,
    onOpenSyllabus: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${program.durationDays} Days · ${program.difficultyTier.label} · ~${program.estimatedDailyMinutes} min/day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("lp_preview_dialog_${program.programId}"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(program.overview, style = MaterialTheme.typography.bodySmall)
                Text("Intended Audience", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(program.targetAudience, style = MaterialTheme.typography.bodySmall)

                Text("Learning Objectives", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                program.learningObjectives.forEach { obj ->
                    Text("• $obj", style = MaterialTheme.typography.bodySmall)
                }

                Text("Modules (${program.modules.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                program.modules.forEach { mod ->
                    Text(
                        "• ${mod.title} (${mod.lessons.size} lessons + checkpoint)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text("Completion Requirements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(program.completionCriteria.description, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (!isEnrolled) {
                Button(
                    onClick = onEnroll,
                    modifier = Modifier.testTag("lp_preview_enroll_btn")
                ) {
                    Text("Enroll & Start Day 1")
                }
            } else {
                Button(onClick = onOpenSyllabus) {
                    Text("Open Syllabus")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenSyllabus) {
                    Text("Browse Days")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

// =============================================================================
// PROGRAM DETAIL & FULL MODULE/DAY SYLLABUS SCREEN
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningPathDetailScreen(
    programId: String,
    uiState: LearningPathsUiState,
    onBack: () -> Unit,
    onEnroll: (String) -> Unit,
    onTogglePause: (String) -> Unit,
    onOpenLesson: (String, Int) -> Unit,
    onToggleDayCompleteQuick: (String, Int) -> Unit,
    onOpenAssessment: (String, String) -> Unit,
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val program = LearningPathCurriculumRegistry.getProgram(programId)
    if (program == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Learning path not found.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBack) { Text("Back to Programs") }
            }
        }
        return
    }

    val ext = MaterialTheme.speakCraftColors
    val enrollment = uiState.userState.enrollments[programId]
    val stats = uiState.programStats[programId]
        ?: LearningPathRecommendationEngine.computeProgramStats(program, enrollment)
    val unmetPrereqs = LearningPathRecommendationEngine.unmetPrerequisites(
        program = program,
        allPrograms = uiState.programs,
        userState = uiState.userState
    )
    val followOnPrograms = if (stats.programCompleted) {
        LearningPathRecommendationEngine.recommendFollowOnPrograms(
            completedProgram = program,
            allPrograms = uiState.programs,
            userState = uiState.userState
        )
    } else emptyList()

    var expandedModuleId by remember(programId) {
        mutableStateOf(
            program.modules.firstOrNull { mod ->
                stats.nextDayNumber in mod.dayStart..mod.dayEnd
            }?.moduleId ?: program.modules.firstOrNull()?.moduleId
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lp_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("All Programs")
                }
            }
        }

        // Program Header & Objectives Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ext.studioConsoleSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "${program.durationDays}-Day Curriculum · ${program.difficultyTier.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.onStudioConsole.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ext.onStudioConsole
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = program.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onStudioConsole.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { stats.completionFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${stats.completedDaysCount}/${stats.totalDays} Days Done",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.onStudioConsole,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stats.passedModulesCount}/${stats.totalModulesCount} Assessments",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.onStudioConsole
                        )
                        if (stats.averageLessonScore > 0) {
                            Text(
                                text = "Avg Score: ${stats.averageLessonScore}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.onStudioConsole
                            )
                        }
                        if (stats.totalPracticeSeconds > 0) {
                            Text(
                                text = "${(stats.totalPracticeSeconds / 60).coerceAtLeast(1)}m practiced",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.onStudioConsole
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!stats.enrolled) {
                            Button(onClick = { onEnroll(program.programId) }) {
                                Text("Enroll & Start Day 1")
                            }
                        } else {
                            Button(onClick = { onOpenLesson(program.programId, stats.nextDayNumber) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Practice Day ${stats.nextDayNumber}")
                            }
                            OutlinedButton(
                                onClick = { onTogglePause(program.programId) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.onStudioConsole),
                                border = BorderStroke(1.dp, ext.onStudioConsole.copy(alpha = 0.5f))
                            ) {
                                Text(if (stats.paused) "Resume Track" else "Pause Track")
                            }
                        }
                    }
                }
            }
        }

        // Learning Objectives & Completion Criteria
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Program Learning Objectives",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    program.learningObjectives.forEach { obj ->
                        Text("• $obj", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Graduation Criteria: ${program.completionCriteria.description}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Advisory Prerequisites Banner
        if (unmetPrereqs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ext.pastelPeachBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Recommended Foundation (Advisory)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ext.pastelPeachInk
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Completing ${unmetPrereqs.joinToString(" or ") { it.title }} beforehand accelerates results, though every lesson stays open now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.pastelPeachInk
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            unmetPrereqs.forEach { pre ->
                                TextButton(onClick = { onOpenProgram(pre.programId) }) {
                                    Text("View ${pre.title}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Graduated Celebration & Follow-on Recommendations
        if (stats.programCompleted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ext.pastelMintBg),
                    border = BorderStroke(1.5.dp, ext.success),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ext.pastelMintInk)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Program Completed — Mastery Earned!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.pastelMintInk
                            )
                        }
                        Text(
                            text = "Average Score: ${stats.averageLessonScore}% · ${stats.passedModulesCount}/${stats.totalModulesCount} Assessments Passed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.pastelMintInk
                        )
                        if (followOnPrograms.isNotEmpty()) {
                            Text(
                                text = "Recommended Next Learning Path:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ext.pastelMintInk
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                followOnPrograms.forEach { nextProg ->
                                    Button(onClick = { onOpenProgram(nextProg.programId) }) {
                                        Text(nextProg.title)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modules Accordion List (All Modules + Individual Virtualized Lessons 1..N + Module Checkpoints)
        program.modules.forEach { module ->
            val moduleCompletedDays = module.lessons.count { lesson ->
                enrollment?.completedDays?.contains(lesson.dayNumber) == true
            }
            val moduleAssessmentRec = enrollment?.moduleAssessments?.get(module.moduleId)
            val isExpanded = expandedModuleId == module.moduleId
            val assessmentPassed = moduleAssessmentRec?.passed == true
            val bestAssessmentScore = moduleAssessmentRec?.bestCombinedScore ?: 0

            item(key = "${module.moduleId}_header") {
                ModuleHeaderCard(
                    module = module,
                    completedCount = moduleCompletedDays,
                    assessmentPassed = assessmentPassed,
                    bestAssessmentScore = bestAssessmentScore,
                    expanded = isExpanded,
                    onToggleExpand = {
                        expandedModuleId = if (isExpanded) null else module.moduleId
                    }
                )
            }

            if (isExpanded) {
                item(key = "${module.moduleId}_objectives") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Module Objectives:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            module.learningObjectives.forEach { obj ->
                                Text("• $obj", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                items(module.lessons, key = { it.lessonId }) { lesson ->
                    val rec = enrollment?.dayRecords?.get(lesson.dayNumber)
                    val isDone = enrollment?.completedDays?.contains(lesson.dayNumber) == true
                    LessonRowCard(
                        lesson = lesson,
                        completed = isDone,
                        bestScore = rec?.bestScore ?: 0,
                        attempts = rec?.attempts ?: 0,
                        onOpen = { onOpenLesson(program.programId, lesson.dayNumber) },
                        onToggleQuick = { onToggleDayCompleteQuick(program.programId, lesson.dayNumber) }
                    )
                }

                item(key = "${module.moduleId}_assessment") {
                    ModuleAssessmentLauncherCard(
                        module = module,
                        assessmentPassed = assessmentPassed,
                        bestAssessmentScore = bestAssessmentScore,
                        onOpenAssessment = { onOpenAssessment(program.programId, module.moduleId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleHeaderCard(
    module: LearningPathModule,
    completedCount: Int,
    assessmentPassed: Boolean,
    bestAssessmentScore: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lp_module_card_${module.moduleId}"),
        border = BorderStroke(
            width = 1.dp,
            color = if (assessmentPassed) ext.success else ext.cardBorderSubtle
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallPillTag(
                        text = "Days ${module.dayStart}-${module.dayEnd}",
                        bg = ext.pastelSkyBg,
                        fg = ext.pastelSkyInk
                    )
                    SmallPillTag(
                        text = "$completedCount/${module.lessons.size} Lessons",
                        bg = if (completedCount == module.lessons.size) ext.pastelMintBg else ext.pastelPeriwinkleBg,
                        fg = if (completedCount == module.lessons.size) ext.pastelMintInk else ext.pastelPeriwinkleInk
                    )
                    if (assessmentPassed) {
                        SmallPillTag(
                            text = "Assessment Passed ($bestAssessmentScore%)",
                            bg = ext.pastelMintBg,
                            fg = ext.pastelMintInk
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse module" else "Expand module"
            )
        }
    }
}

@Composable
private fun ModuleAssessmentLauncherCard(
    module: LearningPathModule,
    assessmentPassed: Boolean,
    bestAssessmentScore: Int,
    onOpenAssessment: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (module.assessment.isCapstone) ext.pastelLavenderBg else ext.pastelGoldBg
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val titleColor = if (module.assessment.isCapstone) ext.pastelLavenderInk else ext.pastelGoldInk
            Text(
                text = if (module.assessment.isCapstone) "FINAL PROGRAM CAPSTONE" else "MODULE CHECKPOINT ASSESSMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = module.assessment.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${module.assessment.overview} Pass >= ${module.assessment.passingScoreThreshold}%.",
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.88f)
            )
            if (bestAssessmentScore > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Best Score: $bestAssessmentScore% (${if (assessmentPassed) "Passed" else "Retake Recommended"})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenAssessment,
                modifier = Modifier.testTag("lp_open_assessment_${module.moduleId}")
            ) {
                Text(if (assessmentPassed) "Review / Retake Assessment" else "Start Assessment")
            }
        }
    }
}

@Composable
private fun LessonRowCard(
    lesson: LearningPathLesson,
    completed: Boolean,
    bestScore: Int,
    attempts: Int,
    onOpen: () -> Unit,
    onToggleQuick: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .testTag("lp_lesson_row_${lesson.dayNumber}"),
        color = if (completed) ext.pastelMintBg.copy(alpha = 0.48f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (completed) ext.success.copy(alpha = 0.45f) else ext.cardBorderSubtle),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (completed) ext.success else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "D${lesson.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) ext.onSuccess else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${lesson.exerciseType.label} · ${lesson.estimatedMinutes} min" +
                        if (attempts > 0) " · Best $bestScore%" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleQuick) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (completed) "Mark day incomplete" else "Mark day complete",
                    tint = if (completed) ext.success else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
internal fun SmallPillTag(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
