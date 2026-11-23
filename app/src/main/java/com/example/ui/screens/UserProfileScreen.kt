package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.learning.PersistedLearningSnapshot
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.SkillBreakdownBars
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

private val profileLevels = listOf(
    "Beginner",
    "Elementary",
    "Intermediate",
    "Upper Intermediate",
    "Advanced"
)

private val profileGoals = listOf(
    "Daily English Fluency",
    "Speaking Confidence",
    "Pronunciation Improvement",
    "Job Interviews",
    "Public Speaking",
    "Workplace Communication",
    "Conversation Practice",
    "Vocabulary Building"
)

private val dailyMinuteChoices = listOf(5L, 10L, 15L, 20L, 30L, 45L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    profile: UserProfile,
    userEmail: String,
    sessions: List<SpeakingSession>,
    vocabulary: List<VocabularyEntry>,
    learningSnapshot: PersistedLearningSnapshot?,
    onSaveProfile: (displayName: String, level: String, primaryGoal: String, dailyGoalMinutes: Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    var isEditing by remember { mutableStateOf(false) }
    var nameDraft by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var levelDraft by remember(profile.communicationLevel) {
        mutableStateOf(profile.communicationLevel.ifBlank { "Intermediate" })
    }
    var goalDraft by remember(profile.primaryGoal) {
        mutableStateOf(profile.primaryGoal.ifBlank { "Daily English Fluency" })
    }
    var minutesDraft by remember(profile.dailyGoalMinutes) {
        mutableLongStateOf(profile.dailyGoalMinutes.coerceIn(5L, 60L))
    }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        ConfirmDestructiveDialog(
            title = "Sign Out of SpeakCraft?",
            message = "Your cloud progress is safely stored in Firebase. Sign back in anytime with Google.",
            confirmLabel = "Sign Out",
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }

    val displayName = profile.displayName.ifBlank { "Speaker" }
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "S" }
    }
    val overallScore = remember(profile) {
        ((profile.fluencyScore + profile.pronunciationScore + profile.vocabularyScore +
            profile.grammarScore + profile.articulationScore + profile.listeningScore) / 6).toInt()
    }
    val totalMinutes = maxOf(
        profile.totalPracticeMinutes.toInt(),
        learningSnapshot?.totalPracticeMinutes ?: 0
    )
    val streakDays = maxOf(
        profile.streakDays.toInt(),
        learningSnapshot?.streak?.currentStreakDays ?: 0
    )
    val wordsMastered = maxOf(
        vocabulary.size,
        profile.wordsLearnedCount.toInt(),
        learningSnapshot?.wordProgress?.values?.count { it.isCompleted } ?: 0
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ext.sidebarTop, ext.sidebarActiveStart)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ext.onHeroTitle.copy(alpha = 0.2f),
                                    border = BorderStroke(2.dp, ext.onHeroTitle.copy(alpha = 0.7f)),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ext.onHeroTitle
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onHeroTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (userEmail.isNotBlank()) {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.sidebarTextIdle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ext.onHeroTitle.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = profile.communicationLevel.ifBlank { "Intermediate" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.onHeroTitle,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ext.onHeroTitle.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "Score $overallScore/100",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.onHeroTitle,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Focus Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${profile.primaryGoal.ifBlank { "Daily English Fluency" }} • ${profile.dailyGoalMinutes} min/day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("profile_edit_toggle_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isEditing) "Close" else "Edit Profile")
                            }
                        }
                    }
                }
            }

            // Inline Profile Editor
            if (isEditing) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("profile_editor_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Update Your Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            Text("Communication Level", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileLevels.forEach { lvl ->
                                    FilterChip(
                                        selected = levelDraft.equals(lvl, ignoreCase = true),
                                        onClick = { levelDraft = lvl },
                                        label = { Text(lvl) }
                                    )
                                }
                            }
                            Text("Primary Goal", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileGoals.forEach { goal ->
                                    FilterChip(
                                        selected = goalDraft.equals(goal, ignoreCase = true),
                                        onClick = { goalDraft = goal },
                                        label = { Text(goal) }
                                    )
                                }
                            }
                            Text("Daily Practice Target", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dailyMinuteChoices.forEach { mins ->
                                    FilterChip(
                                        selected = minutesDraft == mins,
                                        onClick = { minutesDraft = mins },
                                        label = { Text("$mins min") }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        nameDraft = profile.displayName
                                        levelDraft = profile.communicationLevel.ifBlank { "Intermediate" }
                                        goalDraft = profile.primaryGoal.ifBlank { "Daily English Fluency" }
                                        minutesDraft = profile.dailyGoalMinutes.coerceIn(5L, 60L)
                                        isEditing = false
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        onSaveProfile(
                                            nameDraft.trim().ifBlank { displayName },
                                            levelDraft,
                                            goalDraft,
                                            minutesDraft
                                        )
                                        isEditing = false
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("profile_save_button")
                                ) {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }

            // Stats Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Active Streak",
                            value = "$streakDays days",
                            bg = ext.pastelGoldBg,
                            ink = ext.pastelGoldInk,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            icon = Icons.Default.Schedule,
                            label = "Practice Time",
                            value = String.format("%.1f hrs", totalMinutes / 60.0),
                            bg = ext.pastelLavenderBg,
                            ink = ext.pastelLavenderInk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            icon = Icons.Default.Mic,
                            label = "Recordings",
                            value = "${sessions.size}",
                            bg = ext.pastelSkyBg,
                            ink = ext.pastelSkyInk,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            icon = Icons.Default.School,
                            label = "Words Learned",
                            value = "$wordsMastered",
                            bg = ext.pastelMintBg,
                            ink = ext.pastelMintInk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6-Skill Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Communication Skill Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SkillBreakdownBars(
                            metrics = listOf(
                                "Fluency" to profile.fluencyScore.toInt(),
                                "Pronunciation" to profile.pronunciationScore.toInt(),
                                "Vocabulary" to profile.vocabularyScore.toInt(),
                                "Grammar" to profile.grammarScore.toInt(),
                                "Articulation" to profile.articulationScore.toInt(),
                                "Listening" to profile.listeningScore.toInt()
                            )
                        )
                    }
                }
            }

            // Quick Links
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        ProfileShortcutRow(
                            icon = Icons.Default.BarChart,
                            title = "Progress Analytics",
                            subtitle = "Detailed trends, scores & milestones",
                            onClick = { onNavigate("analytics") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications & Alerts",
                            subtitle = "Practice reminders, streak alerts & preferences",
                            onClick = onOpenNotifications
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.Default.Settings,
                            title = "App Settings",
                            subtitle = "Theme, voice speed, tutorials & privacy",
                            onClick = { onNavigate("settings") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "Sign Out",
                            subtitle = "Switch Google account safely",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showSignOutConfirm = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    bg: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
