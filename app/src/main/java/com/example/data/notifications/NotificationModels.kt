package com.example.data.notifications

import kotlinx.serialization.Serializable

/**
 * Severity levels for in-app alerts displayed inside the Notification Drawer.
 */
@Serializable
enum class AlertSeverity(val label: String) {
    INFO("Info"),
    SUCCESS("Success"),
    REMINDER("Reminder"),
    WARNING("Action Needed"),
    URGENT("Important")
}

/**
 * Strictly non-update alert categories supported by the Notification Management System.
 * Software/APK update notifications are intentionally excluded so the notification center
 * never interferes with the dedicated App Update system.
 */
@Serializable
enum class AppAlertCategory(val label: String, val defaultRoute: String) {
    DAILY_REMINDER("Daily Reminder", "daily_practice"),
    STREAK("Streak Alert", "daily_practice"),
    COACHING_FEEDBACK("Coach Feedback", "speaking_lab"),
    VOCABULARY("Vocabulary", "vocabulary"),
    ACHIEVEMENT("Milestone", "analytics"),
    PROGRESS("Progress", "analytics"),
    ANNOUNCEMENT("Announcement", "dashboard"),
    ACCOUNT("Account", "profile")
}

/**
 * Persistent alert record stored by [NotificationManagerRepository].
 */
@Serializable
data class StoredAlert(
    val id: String,
    val title: String,
    val message: String,
    val category: AppAlertCategory,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val timestampMs: Long,
    val isRead: Boolean = false,
    val isPinned: Boolean = false,
    val actionLabel: String = "",
    val actionRoute: String = "",
    val dedupKey: String = "",
    val sourceTag: String = "app"
)

/**
 * User-configurable preferences controlling which alert categories are stored & surfaced.
 */
@Serializable
data class AlertPreferences(
    val dailyReminders: Boolean = true,
    val streakAlerts: Boolean = true,
    val coachingFeedback: Boolean = true,
    val vocabularyReminders: Boolean = true,
    val achievementsAndProgress: Boolean = true,
    val announcements: Boolean = true,
    val accountAlerts: Boolean = true
) {
    fun isEnabledFor(category: AppAlertCategory): Boolean = when (category) {
        AppAlertCategory.DAILY_REMINDER -> dailyReminders
        AppAlertCategory.STREAK -> streakAlerts
        AppAlertCategory.COACHING_FEEDBACK -> coachingFeedback
        AppAlertCategory.VOCABULARY -> vocabularyReminders
        AppAlertCategory.ACHIEVEMENT, AppAlertCategory.PROGRESS -> achievementsAndProgress
        AppAlertCategory.ANNOUNCEMENT -> announcements
        AppAlertCategory.ACCOUNT -> accountAlerts
    }
}

/**
 * Filter tabs available in the Notification UI Drawer.
 */
enum class NotificationDrawerFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    PINNED("Pinned"),
    COACHING("Coaching"),
    REMINDERS("Reminders")
}

/**
 * Snapshot of the persisted Notification Management System state.
 */
@Serializable
data class NotificationStoreState(
    val userId: String = "",
    val alerts: List<StoredAlert> = emptyList(),
    val preferences: AlertPreferences = AlertPreferences(),
    val lastSyncedAtMs: Long = 0L
) {
    val unreadCount: Int
        get() = alerts.count { !it.isRead }

    val pinnedCount: Int
        get() = alerts.count { it.isPinned }

    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * Isolation boundary ensuring the Notification Management System NEVER ingests, stores,
 * or routes to App Update / OTA Release payloads. This keeps the bell-icon drawer 100%
 * independent from [com.example.data.update.ReleaseUpdateRepository] and `app_updates`.
 */
object UpdateSystemIsolationGuard {
    private val blockedRoutePrefixes = listOf(
        "app_updates",
        "ota_update",
        "release_update",
        "update_installer"
    )

    private val blockedSourceTags = setOf(
        "update_system",
        "ota_updater",
        "release_repository",
        "github_release"
    )

    private val blockedKeywords = listOf(
        "apk download",
        "install update",
        "app update available",
        "new version available",
        "ota release"
    )

    fun isUpdateSystemPayload(
        title: String,
        message: String,
        actionRoute: String,
        sourceTag: String
    ): Boolean {
        val normalizedRoute = actionRoute.trim().lowercase()
        if (blockedRoutePrefixes.any { normalizedRoute == it || normalizedRoute.startsWith("$it/") }) {
            return true
        }
        val normalizedSource = sourceTag.trim().lowercase()
        if (normalizedSource in blockedSourceTags) {
            return true
        }
        val combinedText = "${title.lowercase()} ${message.lowercase()}"
        return blockedKeywords.any { combinedText.contains(it) }
    }

    fun sanitizeRoute(route: String, fallback: String): String {
        val clean = route.trim()
        if (clean.isBlank()) return fallback
        val lower = clean.lowercase()
        return if (blockedRoutePrefixes.any { lower == it || lower.startsWith("$it/") }) {
            fallback
        } else {
            clean
        }
    }
}
