package com.example.data.notifications

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.data.learning.AdminAnnouncement
import com.example.data.learning.NotificationCategory
import com.example.data.learning.UserNotification
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Dedicated Notification Management System responsible for storing, deduplicating,
 * filtering, persisting, and managing in-app user alerts.
 *
 * Isolation guarantee:
 * - Persists into its own `notification_store/alerts_<uid>.json` directory.
 * - Never reads, writes, or modifies `AppPreferencesRepository` or any app-update payloads.
 * - Rejects update-system payloads via [UpdateSystemIsolationGuard].
 */
class NotificationManagerRepository(
    context: Context? = null,
    private val customStorageDir: File? = null
) {
    private val storageDir: File? = customStorageDir
        ?: context?.applicationContext?.filesDir?.let { File(it, "notification_store") }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state = MutableStateFlow(NotificationStoreState())
    val state: StateFlow<NotificationStoreState> = _state.asStateFlow()

    private val lock = Any()
    private var loadedUserId: String? = null

    /**
     * Loads persisted alerts for [userId] and ensures contextual daily/welcome alerts exist
     * without interfering with existing stored alerts.
     */
    fun loadForUser(
        userId: String,
        profile: UserProfile? = null,
        sessionsCount: Int = 0,
        streakDays: Int = 0,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val safeUid = userId.ifBlank { "local_guest" }
        synchronized(lock) {
            var current = if (loadedUserId == safeUid && _state.value.userId == safeUid) {
                _state.value
            } else {
                val restored = readFromDisk(safeUid)?.let { sanitizeSnapshot(it) }
                loadedUserId = safeUid
                restored ?: NotificationStoreState(userId = safeUid, lastSyncedAtMs = nowMs)
            }

            val dayKey = dateKey(nowMs)
            val displayName = profile?.displayName?.trim().orEmpty().ifBlank { "Speaker" }
            val dailyTargetMins = (profile?.dailyGoalMinutes ?: 15L).coerceIn(5L, 90L)

            if (current.alerts.isEmpty()) {
                current = appendAlertLocked(
                    snapshot = current,
                    title = "Welcome to SpeakCraft Alerts, $displayName",
                    message = "Your practice reminders, coaching highlights, vocabulary reviews, and streak alerts appear here.",
                    category = AppAlertCategory.ACCOUNT,
                    severity = AlertSeverity.INFO,
                    actionLabel = "View Profile",
                    actionRoute = "profile",
                    dedupKey = "welcome_alerts_$safeUid",
                    isPinned = false,
                    sourceTag = "onboarding",
                    nowMs = nowMs
                )
            }

            current = appendAlertLocked(
                snapshot = current,
                title = "Today's $dailyTargetMins-min speaking plan is ready",
                message = if (sessionsCount == 0) {
                    "Complete your first quick speaking drill to establish your personal voice baseline."
                } else {
                    "Keep building momentum with a focused drill matched to your goals."
                },
                category = AppAlertCategory.DAILY_REMINDER,
                severity = AlertSeverity.REMINDER,
                actionLabel = "Start Practice",
                actionRoute = "daily_practice",
                dedupKey = "daily_plan_$dayKey",
                isPinned = false,
                sourceTag = "scheduler",
                nowMs = nowMs
            )

            if (streakDays >= 2) {
                current = appendAlertLocked(
                    snapshot = current,
                    title = "Protect your $streakDays-day streak",
                    message = "Log one short speaking or vocabulary session today to keep your streak alive.",
                    category = AppAlertCategory.STREAK,
                    severity = AlertSeverity.WARNING,
                    actionLabel = "Quick Drill",
                    actionRoute = "speaking_lab",
                    dedupKey = "streak_protect_${dayKey}_$streakDays",
                    isPinned = false,
                    sourceTag = "streak_engine",
                    nowMs = nowMs
                )
            }

            commitLocked(current.copy(userId = safeUid, lastSyncedAtMs = nowMs))
        }
    }

    /**
     * Posts and persists a new alert into the Notification Management System.
     *
     * @return `true` if the alert was stored, or `false` if blocked by [UpdateSystemIsolationGuard],
     * disabled by user preferences, or deduplicated.
     */
    fun postAlert(
        title: String,
        message: String,
        category: AppAlertCategory,
        severity: AlertSeverity = AlertSeverity.INFO,
        actionLabel: String = "",
        actionRoute: String = category.defaultRoute,
        dedupKey: String = "",
        isPinned: Boolean = false,
        sourceTag: String = "app",
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTitle.isBlank() || cleanMessage.isBlank()) return false

        if (UpdateSystemIsolationGuard.isUpdateSystemPayload(cleanTitle, cleanMessage, actionRoute, sourceTag)) {
            AppLogger.i(TAG, "Blocked update-system payload from entering NotificationManagerRepository: $cleanTitle")
            return false
        }

        synchronized(lock) {
            val before = _state.value
            val after = appendAlertLocked(
                snapshot = before,
                title = cleanTitle,
                message = cleanMessage,
                category = category,
                severity = severity,
                actionLabel = actionLabel.trim(),
                actionRoute = UpdateSystemIsolationGuard.sanitizeRoute(actionRoute, category.defaultRoute),
                dedupKey = dedupKey.trim(),
                isPinned = isPinned,
                sourceTag = sourceTag.trim().ifBlank { "app" },
                nowMs = nowMs
            )
            if (after === before || after.alerts.size == before.alerts.size && after.alerts == before.alerts) {
                return false
            }
            commitLocked(after.copy(lastSyncedAtMs = nowMs))
            return true
        }
    }

    /**
     * Bridges learning-progress notifications into the dedicated Notification Management System
     * without duplicating items or overwriting read/dismissed user state.
     */
    fun syncFromLearningNotifications(items: List<UserNotification>) {
        if (items.isEmpty()) return
        synchronized(lock) {
            var next = _state.value
            val existingKeys = next.alerts.mapNotNull { it.dedupKey.takeIf(String::isNotBlank) }.toMutableSet()
            var changed = false

            items.sortedBy { it.timestampMs }.forEach { item ->
                val mappedCategory = mapLearningCategory(item.category)
                val mappedSeverity = mapLearningSeverity(item.category)
                val dedup = item.dedupKey.ifBlank { "learning_${item.id}" }

                if (dedup in existingKeys) return@forEach
                if (UpdateSystemIsolationGuard.isUpdateSystemPayload(item.title, item.body, item.targetRoute, "learning")) {
                    return@forEach
                }
                if (!next.preferences.isEnabledFor(mappedCategory)) return@forEach

                val sanitizedRoute = UpdateSystemIsolationGuard.sanitizeRoute(
                    item.targetRoute,
                    mappedCategory.defaultRoute
                )
                val stored = StoredAlert(
                    id = item.id.ifBlank { UUID.randomUUID().toString() },
                    title = item.title.trim(),
                    message = item.body.trim(),
                    category = mappedCategory,
                    severity = mappedSeverity,
                    timestampMs = item.timestampMs,
                    isRead = item.isRead,
                    isPinned = false,
                    actionLabel = defaultActionLabel(mappedCategory),
                    actionRoute = sanitizedRoute,
                    dedupKey = dedup,
                    sourceTag = "learning_progress"
                )
                next = next.copy(alerts = sortAlerts(listOf(stored) + next.alerts).take(MAX_STORED_ALERTS))
                existingKeys.add(dedup)
                changed = true
            }

            if (changed) {
                commitLocked(next.copy(lastSyncedAtMs = System.currentTimeMillis()))
            }
        }
    }

    /**
     * Ingests an active workspace/admin announcement if it is NOT an app-update notice.
     */
    fun ingestAnnouncement(announcement: AdminAnnouncement, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!announcement.active) return false
        return postAlert(
            title = announcement.title,
            message = announcement.message,
            category = AppAlertCategory.ANNOUNCEMENT,
            severity = AlertSeverity.INFO,
            actionLabel = "Explore",
            actionRoute = announcement.targetRoute.ifBlank { "dashboard" },
            dedupKey = "announcement_${announcement.id}",
            sourceTag = "announcement",
            nowMs = nowMs
        )
    }

    /**
     * Stores a coaching feedback alert after a speaking practice session finishes.
     */
    fun recordSessionCompletedAlert(
        sessionId: String,
        promptTitle: String,
        overallScore: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val label = promptTitle.trim().ifBlank { "Speaking Practice" }
        val severity = when {
            overallScore >= 80 -> AlertSeverity.SUCCESS
            overallScore >= 60 -> AlertSeverity.INFO
            else -> AlertSeverity.REMINDER
        }
        return postAlert(
            title = "Coach review ready: $label",
            message = "Scored $overallScore/100. Open Speaking Lab to review your tailored rewrites and next 60-second micro-drill.",
            category = AppAlertCategory.COACHING_FEEDBACK,
            severity = severity,
            actionLabel = "View Feedback",
            actionRoute = "speaking_lab",
            dedupKey = "session_feedback_$sessionId",
            sourceTag = "local_coach",
            nowMs = nowMs
        )
    }

    fun markAsRead(alertId: String) {
        synchronized(lock) {
            val current = _state.value
            val updated = current.alerts.map { if (it.id == alertId) it.copy(isRead = true) else it }
            if (updated != current.alerts) {
                commitLocked(current.copy(alerts = updated))
            }
        }
    }

    fun markAsUnread(alertId: String) {
        synchronized(lock) {
            val current = _state.value
            val updated = current.alerts.map { if (it.id == alertId) it.copy(isRead = false) else it }
            if (updated != current.alerts) {
                commitLocked(current.copy(alerts = updated))
            }
        }
    }

    fun togglePinned(alertId: String) {
        synchronized(lock) {
            val current = _state.value
            val updated = sortAlerts(
                current.alerts.map { if (it.id == alertId) it.copy(isPinned = !it.isPinned) else it }
            )
            if (updated != current.alerts) {
                commitLocked(current.copy(alerts = updated))
            }
        }
    }

    fun markAllAsRead() {
        synchronized(lock) {
            val current = _state.value
            if (current.alerts.none { !it.isRead }) return
            commitLocked(current.copy(alerts = current.alerts.map { it.copy(isRead = true) }))
        }
    }

    fun dismissAlert(alertId: String) {
        synchronized(lock) {
            val current = _state.value
            val updated = current.alerts.filterNot { it.id == alertId }
            if (updated.size != current.alerts.size) {
                commitLocked(current.copy(alerts = updated))
            }
        }
    }

    fun clearReadAlerts() {
        synchronized(lock) {
            val current = _state.value
            val updated = current.alerts.filter { !it.isRead || it.isPinned }
            if (updated.size != current.alerts.size) {
                commitLocked(current.copy(alerts = updated))
            }
        }
    }

    fun clearAllAlerts() {
        synchronized(lock) {
            val current = _state.value
            if (current.alerts.isEmpty()) return
            commitLocked(current.copy(alerts = emptyList()))
        }
    }

    fun updatePreferences(updated: AlertPreferences) {
        synchronized(lock) {
            commitLocked(_state.value.copy(preferences = updated))
        }
    }

    private fun appendAlertLocked(
        snapshot: NotificationStoreState,
        title: String,
        message: String,
        category: AppAlertCategory,
        severity: AlertSeverity,
        actionLabel: String,
        actionRoute: String,
        dedupKey: String,
        isPinned: Boolean,
        sourceTag: String,
        nowMs: Long
    ): NotificationStoreState {
        if (!snapshot.preferences.isEnabledFor(category)) return snapshot
        if (dedupKey.isNotBlank() && snapshot.alerts.any { it.dedupKey == dedupKey }) {
            return snapshot
        }
        val item = StoredAlert(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            category = category,
            severity = severity,
            timestampMs = nowMs,
            isRead = false,
            isPinned = isPinned,
            actionLabel = actionLabel.ifBlank { defaultActionLabel(category) },
            actionRoute = UpdateSystemIsolationGuard.sanitizeRoute(actionRoute, category.defaultRoute),
            dedupKey = dedupKey,
            sourceTag = sourceTag
        )
        val merged = sortAlerts(listOf(item) + snapshot.alerts).take(MAX_STORED_ALERTS)
        return snapshot.copy(alerts = merged)
    }

    private fun sanitizeSnapshot(snapshot: NotificationStoreState): NotificationStoreState {
        val cleanAlerts = snapshot.alerts.filterNot { alert ->
            UpdateSystemIsolationGuard.isUpdateSystemPayload(
                title = alert.title,
                message = alert.message,
                actionRoute = alert.actionRoute,
                sourceTag = alert.sourceTag
            )
        }
        return snapshot.copy(alerts = sortAlerts(cleanAlerts))
    }

    private fun sortAlerts(alerts: List<StoredAlert>): List<StoredAlert> =
        alerts.sortedWith(
            compareByDescending<StoredAlert> { it.isPinned }
                .thenByDescending { it.timestampMs }
        )

    private fun mapLearningCategory(category: NotificationCategory): AppAlertCategory = when (category) {
        NotificationCategory.DAILY_PRACTICE_REMINDER -> AppAlertCategory.DAILY_REMINDER
        NotificationCategory.STREAK_ALERT, NotificationCategory.STREAK_MILESTONE -> AppAlertCategory.STREAK
        NotificationCategory.VOCABULARY_REVIEW -> AppAlertCategory.VOCABULARY
        NotificationCategory.LESSON_PROGRESS, NotificationCategory.WEEKLY_SUMMARY -> AppAlertCategory.PROGRESS
        NotificationCategory.ACHIEVEMENT_UNLOCKED -> AppAlertCategory.ACHIEVEMENT
        NotificationCategory.PRACTICE_FEEDBACK -> AppAlertCategory.COACHING_FEEDBACK
        NotificationCategory.ADMIN_ANNOUNCEMENT -> AppAlertCategory.ANNOUNCEMENT
        NotificationCategory.ACCOUNT_SECURITY -> AppAlertCategory.ACCOUNT
    }

    private fun mapLearningSeverity(category: NotificationCategory): AlertSeverity = when (category) {
        NotificationCategory.STREAK_ALERT -> AlertSeverity.WARNING
        NotificationCategory.ACCOUNT_SECURITY -> AlertSeverity.URGENT
        NotificationCategory.ACHIEVEMENT_UNLOCKED, NotificationCategory.STREAK_MILESTONE -> AlertSeverity.SUCCESS
        NotificationCategory.DAILY_PRACTICE_REMINDER, NotificationCategory.VOCABULARY_REVIEW -> AlertSeverity.REMINDER
        else -> AlertSeverity.INFO
    }

    private fun defaultActionLabel(category: AppAlertCategory): String = when (category) {
        AppAlertCategory.DAILY_REMINDER -> "Start Plan"
        AppAlertCategory.STREAK -> "Keep Streak"
        AppAlertCategory.COACHING_FEEDBACK -> "Open Lab"
        AppAlertCategory.VOCABULARY -> "Review Words"
        AppAlertCategory.ACHIEVEMENT, AppAlertCategory.PROGRESS -> "View Progress"
        AppAlertCategory.ANNOUNCEMENT -> "Open"
        AppAlertCategory.ACCOUNT -> "View Profile"
    }

    private fun dateKey(nowMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

    private fun commitLocked(snapshot: NotificationStoreState) {
        _state.update { snapshot }
        writeToDisk(snapshot)
    }

    private fun fileForUser(uid: String): File? {
        val dir = storageDir ?: return null
        val safe = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "local_guest" }
        return File(dir, "alerts_$safe.json")
    }

    private fun readFromDisk(uid: String): NotificationStoreState? {
        val file = fileForUser(uid) ?: return null
        if (!file.exists()) return null
        return try {
            json.decodeFromString<NotificationStoreState>(file.readText())
        } catch (err: Throwable) {
            AppLogger.w(TAG, "Failed reading persisted alerts, starting fresh: ${err.message}")
            null
        }
    }

    private fun writeToDisk(snapshot: NotificationStoreState) {
        val file = fileForUser(snapshot.userId) ?: return
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(snapshot))
            if (!temp.renameTo(file)) {
                file.writeText(json.encodeToString(snapshot))
                temp.delete()
            }
        } catch (err: Throwable) {
            AppLogger.w(TAG, "Failed writing persisted alerts: ${err.message}")
        }
    }

    companion object {
        private const val TAG = "NotificationManagerRepo"
        private const val MAX_STORED_ALERTS = 150

        fun filterAlerts(
            alerts: List<StoredAlert>,
            filter: NotificationDrawerFilter
        ): List<StoredAlert> = when (filter) {
            NotificationDrawerFilter.ALL -> alerts
            NotificationDrawerFilter.UNREAD -> alerts.filter { !it.isRead }
            NotificationDrawerFilter.PINNED -> alerts.filter { it.isPinned }
            NotificationDrawerFilter.COACHING -> alerts.filter {
                it.category == AppAlertCategory.COACHING_FEEDBACK ||
                    it.category == AppAlertCategory.ACHIEVEMENT ||
                    it.category == AppAlertCategory.PROGRESS
            }
            NotificationDrawerFilter.REMINDERS -> alerts.filter {
                it.category == AppAlertCategory.DAILY_REMINDER ||
                    it.category == AppAlertCategory.STREAK ||
                    it.category == AppAlertCategory.VOCABULARY
            }
        }
    }
}
