package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.notifications.AlertPreferences
import com.example.data.notifications.AlertSeverity
import com.example.data.notifications.AppAlertCategory
import com.example.data.notifications.NotificationDrawerFilter
import com.example.data.notifications.NotificationManagerRepository
import com.example.data.notifications.StoredAlert
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

/**
 * Dedicated right-edge slide-in Notification UI Drawer triggered by the top-bar bell icon.
 * Displays stored user alerts, category filters, bulk actions, pinning, read/unread state,
 * and alert preference toggles while remaining strictly isolated from the App Update system.
 */
@Composable
fun NotificationDrawer(
    visible: Boolean,
    alerts: List<StoredAlert>,
    preferences: AlertPreferences,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismissAlert: (String) -> Unit,
    onClearRead: () -> Unit,
    onUpdatePreferences: (AlertPreferences) -> Unit,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    var selectedFilter by remember { mutableStateOf(NotificationDrawerFilter.ALL) }
    var showPreferences by remember { mutableStateOf(false) }

    val unreadCount = remember(visible, alerts) {
        if (!visible) 0 else alerts.count { !it.isRead }
    }
    val hasClearableRead = remember(visible, alerts) {
        if (!visible) false else alerts.any { it.isRead && !it.isPinned }
    }
    val filteredAlerts = remember(visible, alerts, selectedFilter) {
        if (!visible) emptyList() else NotificationManagerRepository.filterAlerts(alerts, selectedFilter)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Dimmed backdrop scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("notification_drawer_scrim")
            )
        }

        // Slide-over right-edge Notification Drawer panel
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> fullWidth }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> fullWidth }
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .testTag("notification_drawer_panel"),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (unreadCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "$unreadCount new",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Practice alerts, coaching & streaks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showPreferences = !showPreferences },
                                modifier = Modifier
                                    .defaultMinSize(
                                        minWidth = SpeakCraftSpacing.minTouchTarget,
                                        minHeight = SpeakCraftSpacing.minTouchTarget
                                    )
                                    .testTag("notification_drawer_prefs_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Alert preferences",
                                    tint = if (showPreferences) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .defaultMinSize(
                                        minWidth = SpeakCraftSpacing.minTouchTarget,
                                        minHeight = SpeakCraftSpacing.minTouchTarget
                                    )
                                    .testTag("notification_drawer_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close notifications drawer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NotificationDrawerFilter.entries) { filter ->
                            val chipLabel = when (filter) {
                                NotificationDrawerFilter.UNREAD ->
                                    if (unreadCount > 0) "Unread ($unreadCount)" else "Unread"
                                else -> filter.label
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(chipLabel) },
                                modifier = Modifier.testTag("notification_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    // Bulk management bar
                    if (unreadCount > 0 || hasClearableRead) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (unreadCount > 0) {
                                TextButton(
                                    onClick = onMarkAllRead,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("notification_drawer_mark_all_read")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mark all read")
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            if (hasClearableRead) {
                                TextButton(
                                    onClick = onClearRead,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("notification_drawer_clear_read")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clear read")
                                }
                            }
                        }
                    }

                    // Collapsible Alert Preferences
                    AnimatedVisibility(visible = showPreferences) {
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)) {
                            DrawerAlertPreferencesCard(
                                preferences = preferences,
                                onUpdatePreferences = onUpdatePreferences
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    // Alerts Feed or Empty State
                    if (filteredAlerts.isEmpty()) {
                        DrawerEmptyState(
                            filter = selectedFilter,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("notification_drawer_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                        ) {
                            items(filteredAlerts, key = { it.id }) { alert ->
                                StoredAlertCard(
                                    alert = alert,
                                    onOpen = {
                                        onMarkRead(alert.id)
                                        onDismiss()
                                        if (alert.actionRoute.isNotBlank()) {
                                            onOpenRoute(alert.actionRoute)
                                        }
                                    },
                                    onToggleRead = {
                                        if (alert.isRead) onMarkUnread(alert.id) else onMarkRead(alert.id)
                                    },
                                    onTogglePin = { onTogglePin(alert.id) },
                                    onDismissAlert = { onDismissAlert(alert.id) }
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
private fun DrawerEmptyState(
    filter: NotificationDrawerFilter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = when (filter) {
                NotificationDrawerFilter.UNREAD -> "You're all caught up"
                NotificationDrawerFilter.PINNED -> "No pinned alerts"
                NotificationDrawerFilter.COACHING -> "No coaching alerts yet"
                NotificationDrawerFilter.REMINDERS -> "No active reminders"
                NotificationDrawerFilter.ALL -> "No notifications yet"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Practice reminders, streak alerts, vocabulary reviews, and coaching summaries will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StoredAlertCard(
    alert: StoredAlert,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onTogglePin: () -> Unit,
    onDismissAlert: () -> Unit
) {
    val (icon, iconBg, iconTint) = categoryVisuals(alert.category)
    val cardColor = if (alert.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
    }
    val borderColor = when {
        alert.isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        !alert.isRead -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("stored_alert_card_${alert.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = alert.category.label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!alert.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (alert.isRead) FontWeight.Medium else FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = alert.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (alert.severity == AlertSeverity.WARNING || alert.severity == AlertSeverity.URGENT) {
                            SeverityBadge(severity = alert.severity)
                        }
                        Text(
                            text = "• ${formatDrawerRelativeTime(alert.timestampMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alert.actionRoute.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = alert.actionLabel.ifBlank { "Open" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = SpeakCraftSpacing.minTouchTarget,
                                minHeight = SpeakCraftSpacing.minTouchTarget
                            )
                            .testTag("alert_pin_${alert.id}")
                    ) {
                        Icon(
                            imageVector = if (alert.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (alert.isPinned) "Unpin alert" else "Pin alert",
                            tint = if (alert.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleRead,
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = SpeakCraftSpacing.minTouchTarget,
                                minHeight = SpeakCraftSpacing.minTouchTarget
                            )
                            .testTag("alert_toggle_read_${alert.id}")
                    ) {
                        Icon(
                            imageVector = if (alert.isRead) {
                                Icons.Default.MarkEmailUnread
                            } else {
                                Icons.Default.MarkEmailRead
                            },
                            contentDescription = if (alert.isRead) "Mark as unread" else "Mark as read",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismissAlert,
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = SpeakCraftSpacing.minTouchTarget,
                                minHeight = SpeakCraftSpacing.minTouchTarget
                            )
                            .testTag("alert_dismiss_${alert.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss alert",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: AlertSeverity) {
    val ext = MaterialTheme.speakCraftColors
    val (bg, fg) = when (severity) {
        AlertSeverity.URGENT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        AlertSeverity.WARNING -> ext.warningContainer to ext.warning
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun DrawerAlertPreferencesCard(
    preferences: AlertPreferences,
    onUpdatePreferences: (AlertPreferences) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_drawer_prefs_panel")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Alert Preferences",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            DrawerPreferenceSwitch(
                label = "Daily Practice Reminders",
                checked = preferences.dailyReminders,
                onCheckedChange = { onUpdatePreferences(preferences.copy(dailyReminders = it)) }
            )
            DrawerPreferenceSwitch(
                label = "Streak Protection Alerts",
                checked = preferences.streakAlerts,
                onCheckedChange = { onUpdatePreferences(preferences.copy(streakAlerts = it)) }
            )
            DrawerPreferenceSwitch(
                label = "Coaching Feedback Ready",
                checked = preferences.coachingFeedback,
                onCheckedChange = { onUpdatePreferences(preferences.copy(coachingFeedback = it)) }
            )
            DrawerPreferenceSwitch(
                label = "Vocabulary Review Due",
                checked = preferences.vocabularyReminders,
                onCheckedChange = { onUpdatePreferences(preferences.copy(vocabularyReminders = it)) }
            )
            DrawerPreferenceSwitch(
                label = "Milestones & Announcements",
                checked = preferences.achievementsAndProgress && preferences.announcements,
                onCheckedChange = {
                    onUpdatePreferences(
                        preferences.copy(
                            achievementsAndProgress = it,
                            announcements = it
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun DrawerPreferenceSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun categoryVisuals(category: AppAlertCategory): Triple<ImageVector, Color, Color> {
    val ext = MaterialTheme.speakCraftColors
    return when (category) {
        AppAlertCategory.DAILY_REMINDER ->
            Triple(Icons.Default.Schedule, ext.pastelLavenderBg, ext.pastelLavenderInk)
        AppAlertCategory.STREAK ->
            Triple(Icons.Default.LocalFireDepartment, ext.pastelGoldBg, ext.pastelGoldInk)
        AppAlertCategory.COACHING_FEEDBACK ->
            Triple(Icons.Default.CheckCircle, ext.pastelMintBg, ext.pastelMintInk)
        AppAlertCategory.VOCABULARY ->
            Triple(Icons.Default.Book, ext.pastelSkyBg, ext.pastelSkyInk)
        AppAlertCategory.ACHIEVEMENT ->
            Triple(Icons.Default.EmojiEvents, ext.pastelPeachBg, ext.pastelPeachInk)
        AppAlertCategory.PROGRESS ->
            Triple(Icons.Default.Insights, ext.pastelSkyBg, ext.pastelSkyInk)
        AppAlertCategory.ANNOUNCEMENT ->
            Triple(Icons.Default.Campaign, ext.pastelLavenderBg, ext.pastelLavenderInk)
        AppAlertCategory.ACCOUNT ->
            Triple(Icons.Default.Security, ext.pastelPeriwinkleBg, ext.pastelPeriwinkleInk)
    }
}

private fun formatDrawerRelativeTime(timestampMs: Long): String {
    val diffMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    return when {
        mins < 1L -> "Just now"
        mins < 60L -> "${mins}m ago"
        mins < 1_440L -> "${mins / 60L}h ago"
        mins < 2_880L -> "Yesterday"
        else -> "${mins / 1_440L}d ago"
    }
}
