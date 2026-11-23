package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.learning.AdminAnnouncement
import com.example.data.model.UserProfile
import com.example.data.notifications.AlertPreferences
import com.example.data.notifications.AlertSeverity
import com.example.data.notifications.AppAlertCategory
import com.example.data.notifications.NotificationDrawerFilter
import com.example.data.notifications.NotificationManagerRepository
import com.example.data.notifications.StoredAlert
import com.example.data.notifications.UpdateSystemIsolationGuard
import com.example.ui.components.NotificationDrawer
import com.example.ui.theme.SpeakCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationManagementSystemTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun tempStoreDir(): File =
        File(System.getProperty("java.io.tmpdir"), "speakcraft_notif_test_${UUID.randomUUID()}").apply {
            mkdirs()
        }

    @Test
    fun `repository stores persists deduplicates pins and clears alerts cleanly`() {
        val dir = tempStoreDir()
        try {
            val repo = NotificationManagerRepository(customStorageDir = dir)
            repo.loadForUser(
                userId = "user_alpha",
                profile = UserProfile(userId = "user_alpha", displayName = "Sruthi", dailyGoalMinutes = 20L),
                sessionsCount = 2,
                streakDays = 4,
                nowMs = 1_700_000_000_000L
            )
            val initialCount = repo.state.value.alerts.size
            assertTrue(initialCount >= 3)

            // Post a custom alert & verify dedup
            val posted = repo.postAlert(
                title = "Pronunciation Milestone",
                message = "Your clarity reached 88% in today's drill.",
                category = AppAlertCategory.ACHIEVEMENT,
                severity = AlertSeverity.SUCCESS,
                actionLabel = "See Score",
                actionRoute = "analytics",
                dedupKey = "pron_milestone_88",
                nowMs = 1_700_000_050_000L
            )
            assertTrue(posted)
            val duplicatePosted = repo.postAlert(
                title = "Pronunciation Milestone",
                message = "Duplicate should be ignored.",
                category = AppAlertCategory.ACHIEVEMENT,
                dedupKey = "pron_milestone_88",
                nowMs = 1_700_000_060_000L
            )
            assertFalse(duplicatePosted)

            val created = repo.state.value.alerts.first { it.dedupKey == "pron_milestone_88" }
            repo.togglePinned(created.id)
            assertTrue(repo.state.value.alerts.first().id == created.id)
            assertTrue(repo.state.value.alerts.first().isPinned)

            repo.markAllAsRead()
            assertEquals(0, repo.state.value.unreadCount)

            repo.markAsUnread(created.id)
            assertEquals(1, repo.state.value.unreadCount)

            // Clear read keeps unread & pinned alerts
            repo.clearReadAlerts()
            assertEquals(1, repo.state.value.alerts.size)
            assertEquals(created.id, repo.state.value.alerts.first().id)

            // Reload from disk in a brand-new repository instance
            val reloaded = NotificationManagerRepository(customStorageDir = dir)
            reloaded.loadForUser(
                userId = "user_alpha",
                profile = UserProfile(userId = "user_alpha", displayName = "Sruthi"),
                nowMs = 1_700_000_000_000L
            )
            assertTrue(reloaded.state.value.alerts.any { it.dedupKey == "pron_milestone_88" && it.isPinned })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `notification system strictly blocks update system payloads and sanitizes update routes`() {
        val dir = tempStoreDir()
        try {
            val repo = NotificationManagerRepository(customStorageDir = dir)
            repo.loadForUser(userId = "user_iso")
            val beforeCount = repo.state.value.alerts.size

            // Direct update route
            assertFalse(
                repo.postAlert(
                    title = "Version 2.0 Ready",
                    message = "Tap to open updater",
                    category = AppAlertCategory.ANNOUNCEMENT,
                    actionRoute = "app_updates"
                )
            )
            // Update source tag
            assertFalse(
                repo.postAlert(
                    title = "System Notice",
                    message = "Download latest package",
                    category = AppAlertCategory.ACCOUNT,
                    sourceTag = "update_system"
                )
            )
            // Update keyword in announcement
            assertFalse(
                repo.ingestAnnouncement(
                    AdminAnnouncement(
                        id = "ann_upd",
                        title = "App update available",
                        message = "Please install update v2.0 now.",
                        active = true
                    )
                )
            )
            assertEquals(beforeCount, repo.state.value.alerts.size)
            assertEquals("dashboard", UpdateSystemIsolationGuard.sanitizeRoute("app_updates", "dashboard"))
            assertEquals("speaking_lab", UpdateSystemIsolationGuard.sanitizeRoute("speaking_lab", "dashboard"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `preferences toggle suppresses disabled alert categories`() {
        val dir = tempStoreDir()
        try {
            val repo = NotificationManagerRepository(customStorageDir = dir)
            repo.loadForUser(userId = "user_pref")
            repo.updatePreferences(AlertPreferences(streakAlerts = false))

            val stored = repo.postAlert(
                title = "Streak check",
                message = "Keep your streak going today.",
                category = AppAlertCategory.STREAK,
                dedupKey = "streak_muted_test"
            )
            assertFalse(stored)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `notification drawer renders alerts supports filtering preferences pin and navigation`() {
        var markedReadId = ""
        var pinnedId = ""
        var dismissedId = ""
        var navigatedRoute = ""
        var markAllCalled = false
        var drawerClosed = false

        val alerts = listOf(
            StoredAlert(
                id = "a1",
                title = "Coach review ready: Interview Prep",
                message = "Scored 84/100. View your tailored rewrites.",
                category = AppAlertCategory.COACHING_FEEDBACK,
                severity = AlertSeverity.SUCCESS,
                timestampMs = System.currentTimeMillis() - 120_000L,
                isRead = false,
                actionLabel = "View Feedback",
                actionRoute = "speaking_lab"
            ),
            StoredAlert(
                id = "a2",
                title = "Protect your 5-day streak",
                message = "Complete one quick drill today.",
                category = AppAlertCategory.STREAK,
                severity = AlertSeverity.WARNING,
                timestampMs = System.currentTimeMillis() - 600_000L,
                isRead = true,
                isPinned = true,
                actionLabel = "Quick Drill",
                actionRoute = "daily_practice"
            )
        )

        composeRule.setContent {
            SpeakCraftTheme {
                NotificationDrawer(
                    visible = true,
                    alerts = alerts,
                    preferences = AlertPreferences(),
                    onDismiss = { drawerClosed = true },
                    onMarkRead = { markedReadId = it },
                    onMarkUnread = {},
                    onTogglePin = { pinnedId = it },
                    onMarkAllRead = { markAllCalled = true },
                    onDismissAlert = { dismissedId = it },
                    onClearRead = {},
                    onUpdatePreferences = {},
                    onOpenRoute = { navigatedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("notification_drawer_panel").assertIsDisplayed()
        composeRule.onNodeWithText("Coach review ready: Interview Prep").assertIsDisplayed()

        // Toggle preferences panel
        composeRule.onNodeWithTag("notification_drawer_prefs_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_drawer_prefs_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_drawer_prefs_toggle").performClick()
        composeRule.waitForIdle()

        // Mark all read
        composeRule.onNodeWithTag("notification_drawer_mark_all_read").performClick()
        assertTrue(markAllCalled)

        // Pin & dismiss controls
        composeRule.onNodeWithTag("alert_pin_a1").performClick()
        assertEquals("a1", pinnedId)
        composeRule.onNodeWithTag("alert_dismiss_a1").performClick()
        assertEquals("a1", dismissedId)

        // Filter to Pinned
        composeRule.onNodeWithTag("notification_filter_${NotificationDrawerFilter.PINNED.name.lowercase()}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Protect your 5-day streak").assertIsDisplayed()

        // Tap alert card -> opens route, marks read, closes drawer
        composeRule.onNodeWithTag("stored_alert_card_a2").performClick()
        assertEquals("a2", markedReadId)
        assertEquals("daily_practice", navigatedRoute)
        assertTrue(drawerClosed)
    }
}
