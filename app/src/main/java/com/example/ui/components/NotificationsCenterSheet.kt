package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.learning.NotificationCategory
import com.example.data.learning.NotificationPreferences
import com.example.data.learning.UserNotification
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

/**
 * In-app Notifications & Alerts center triggered from the top bar bell icon.
 * Strictly surfaces user learning alerts, streak warnings, practice reminders,
 * vocabulary reviews, achievements, and account notices — NEVER app update prompts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterSheet(
    notifications: List<UserNotification>,
    preferences: NotificationPreferences,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onUpdatePreferences: (NotificationPreferences) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf("All") }
    var showAlertPrefs by remember { mutableStateOf(false) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val filtered = remember(notifications, selectedFilter) {
        val sorted = notifications.sortedByDescending { it.timestampMs }
        when (selectedFilter) {
            "Unread" -> sorted.filter { !it.isRead }
            "Alerts" -> sorted.filter {
                it.category == NotificationCategory.STREAK_ALERT ||
                    it.category == NotificationCategory.DAILY_PRACTICE_REMINDER ||
                    it.category == NotificationCategory.VOCABULARY_REVIEW ||
                    it.category == NotificationCategory.ACCOUNT_SECURITY
            }
            "Progress" -> sorted.filter {
                it.category == NotificationCategory.STREAK_MILESTONE ||
                    it.category == NotificationCategory.ACHIEVEMENT_UNLOCKED ||
                    it.category == NotificationCategory.LESSON_PROGRESS ||
                    it.category == NotificationCategory.PRACTICE_FEEDBACK ||
                    it.category == NotificationCategory.WEEKLY_SUMMARY
            }
            else -> sorted
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("notifications_center_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Notifications & Alerts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread alert(s)" else "You're all caught up",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("notifications_mark_all_read_button")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark read")
                        }
                    }
                    IconButton(
                        onClick = { showAlertPrefs = !showAlertPrefs },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("notifications_prefs_toggle_button")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Alert preferences",
                            tint = if (showAlertPrefs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showAlertPrefs) {
                AlertPreferencesPanel(
                    preferences = preferences,
                    onUpdatePreferences = onUpdatePreferences
                )
            }

            // Filter Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Unread", "Alerts", "Progress")) { filter ->
                    val count = when (filter) {
                        "Unread" -> unreadCount
                        else -> null
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(if (count != null && count > 0) "$filter ($count)" else filter)
                        },
                        modifier = Modifier.testTag("notifications_filter_${filter.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (selectedFilter == "Unread") "No unread alerts" else "No notifications yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Daily reminders, streak alerts, vocabulary reviews, and coaching milestones appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        NotificationAlertRow(
                            notification = item,
                            onClick = {
                                onMarkRead(item.id)
                                val route = item.targetRoute.ifBlank { "dashboard" }
                                onDismiss()
                                onOpenRoute(route)
                            },
                            onDismissItem = { onDeleteNotification(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAlertRow(
    notification: UserNotification,
    onClick: () -> Unit,
    onDismissItem: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val (icon, accentBg, accentTint) = styleForNotificationCategory(notification.category)
    val isUnread = !notification.isRead

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (isUnread) 1.dp else 0.75.dp,
            color = if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = notification.category.label,
                        tint = accentTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = notification.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentTint,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• ${formatRelativeTimestamp(notification.timestampMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDismissItem,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AlertPreferencesPanel(
    preferences: NotificationPreferences,
    onUpdatePreferences: (NotificationPreferences) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Alert Preferences",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            AlertToggleRow(
                label = "Daily Practice Reminders",
                checked = preferences.dailyPracticeReminders,
                onCheckedChange = { onUpdatePreferences(preferences.copy(dailyPracticeReminders = it)) }
            )
            AlertToggleRow(
                label = "Streak Protection Alerts",
                checked = preferences.streakAlerts,
                onCheckedChange = { onUpdatePreferences(preferences.copy(streakAlerts = it)) }
            )
            AlertToggleRow(
                label = "Vocabulary Review Due",
                checked = preferences.vocabularyReviewReminders,
                onCheckedChange = { onUpdatePreferences(preferences.copy(vocabularyReviewReminders = it)) }
            )
            AlertToggleRow(
                label = "Coaching Feedback & Milestones",
                checked = preferences.lessonAndFeedbackAlerts,
                onCheckedChange = { onUpdatePreferences(preferences.copy(lessonAndFeedbackAlerts = it)) }
            )
        }
    }
}

@Composable
private fun AlertToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun styleForNotificationCategory(category: NotificationCategory): Triple<ImageVector, Color, Color> {
    val ext = MaterialTheme.speakCraftColors
    return when (category) {
        NotificationCategory.DAILY_PRACTICE_REMINDER ->
            Triple(Icons.Default.Schedule, ext.pastelLavenderBg, ext.pastelLavenderInk)
        NotificationCategory.STREAK_ALERT ->
            Triple(Icons.Default.LocalFireDepartment, ext.pastelGoldBg, ext.pastelGoldInk)
        NotificationCategory.STREAK_MILESTONE ->
            Triple(Icons.Default.EmojiEvents, ext.pastelPeachBg, ext.pastelPeachInk)
        NotificationCategory.VOCABULARY_REVIEW ->
            Triple(Icons.Default.Book, ext.pastelSkyBg, ext.pastelSkyInk)
        NotificationCategory.LESSON_PROGRESS ->
            Triple(Icons.Default.School, ext.pastelMintBg, ext.pastelMintInk)
        NotificationCategory.ACHIEVEMENT_UNLOCKED ->
            Triple(Icons.Default.AutoAwesome, ext.pastelPeachBg, ext.pastelPeachInk)
        NotificationCategory.WEEKLY_SUMMARY ->
            Triple(Icons.Default.Insights, ext.pastelSkyBg, ext.pastelSkyInk)
        NotificationCategory.PRACTICE_FEEDBACK ->
            Triple(Icons.Default.CheckCircle, ext.pastelMintBg, ext.pastelMintInk)
        NotificationCategory.ADMIN_ANNOUNCEMENT ->
            Triple(Icons.Default.Campaign, ext.pastelLavenderBg, ext.pastelLavenderInk)
        NotificationCategory.ACCOUNT_SECURITY ->
            Triple(Icons.Default.Security, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
    }
}

private fun formatRelativeTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Recently"
    val deltaMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val mins = deltaMs / 60_000L
    val hours = mins / 60L
    val days = hours / 24L
    return when {
        mins < 1L -> "Just now"
        mins < 60L -> "${mins}m ago"
        hours < 24L -> "${hours}h ago"
        days == 1L -> "Yesterday"
        else -> "${days}d ago"
    }
}
