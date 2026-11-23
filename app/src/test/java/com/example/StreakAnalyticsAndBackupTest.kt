package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SpeakingSession
import com.example.data.model.StreakAndAnalyticsHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StreakAnalyticsAndBackupTest {

    @Test
    fun `streak helper increments only on consecutive day and resets after multi-day gap`() {
        // Same day preserves existing streak
        assertEquals(
            4L,
            StreakAndAnalyticsHelper.computeUpdatedStreak(
                prevLastDateKey = "2026-11-18",
                todayDateKey = "2026-11-18",
                prevStreak = 4L,
                hasCompletedSteps = true
            )
        )
        // Consecutive next day increments streak
        assertEquals(
            5L,
            StreakAndAnalyticsHelper.computeUpdatedStreak(
                prevLastDateKey = "2026-11-18",
                todayDateKey = "2026-11-19",
                prevStreak = 4L,
                hasCompletedSteps = true
            )
        )
        // 2-day gap resets streak to 1
        assertEquals(
            1L,
            StreakAndAnalyticsHelper.computeUpdatedStreak(
                prevLastDateKey = "2026-11-15",
                todayDateKey = "2026-11-18",
                prevStreak = 9L,
                hasCompletedSteps = true
            )
        )
        // Incomplete steps keep existing streak unchanged
        assertEquals(
            3L,
            StreakAndAnalyticsHelper.computeUpdatedStreak(
                prevLastDateKey = "2026-11-18",
                todayDateKey = "2026-11-19",
                prevStreak = 3L,
                hasCompletedSteps = false
            )
        )
    }

    @Test
    fun `minutes delta never double counts or returns negative`() {
        assertEquals(5L, StreakAndAnalyticsHelper.computeMinutesDelta(10L, 15L))
        assertEquals(0L, StreakAndAnalyticsHelper.computeMinutesDelta(15L, 10L))
    }

    @Test
    fun `filterSessionsByPeriod filters Daily Weekly and Monthly accurately`() {
        val nowMs = 1_763_500_000_000L
        val sessionToday = SpeakingSession(
            id = "s1",
            promptTitle = "Today",
            createdAt = Timestamp(Date(nowMs - TimeUnit.HOURS.toMillis(3)))
        )
        val session4DaysAgo = SpeakingSession(
            id = "s2",
            promptTitle = "4d",
            createdAt = Timestamp(Date(nowMs - TimeUnit.DAYS.toMillis(4)))
        )
        val session20DaysAgo = SpeakingSession(
            id = "s3",
            promptTitle = "20d",
            createdAt = Timestamp(Date(nowMs - TimeUnit.DAYS.toMillis(20)))
        )
        val session45DaysAgo = SpeakingSession(
            id = "s4",
            promptTitle = "45d",
            createdAt = Timestamp(Date(nowMs - TimeUnit.DAYS.toMillis(45)))
        )
        val all = listOf(sessionToday, session4DaysAgo, session20DaysAgo, session45DaysAgo)

        assertEquals(listOf("s1"), StreakAndAnalyticsHelper.filterSessionsByPeriod(all, "Daily", nowMs).map { it.id })
        assertEquals(listOf("s1", "s2"), StreakAndAnalyticsHelper.filterSessionsByPeriod(all, "Weekly", nowMs).map { it.id })
        assertEquals(listOf("s1", "s2", "s3"), StreakAndAnalyticsHelper.filterSessionsByPeriod(all, "Monthly", nowMs).map { it.id })
    }

    @Test
    fun `feature tutorial registry covers all 22 active features with steps and improvement benefits`() {
        val requiredKeys = listOf(
            "auth", "onboarding", "dashboard", "daily_practice", "learning_paths",
            "vocabulary", "speaking_lab", "challenges", "articulation_lab", "pronunciation",
            "voice_training", "conversation", "listening_lab", "grammar_speaking",
            "public_speaking", "real_world_tasks", "module_lessons", "lesson_detail",
            "analytics", "ai_mentor", "settings", "admin_console"
        )
        val registryKeys = com.example.data.model.FeatureTutorialRegistry.allTutorials.map { it.featureKey }.toSet()
        assertFalse("Removed youtube_library tutorial must not exist", registryKeys.contains("youtube_library"))
        assertFalse("Removed app_updates tutorial must not exist", registryKeys.contains("app_updates"))
        requiredKeys.forEach { key ->
            assertTrue("Missing tutorial for $key", registryKeys.contains(key))
        }
        com.example.data.model.FeatureTutorialRegistry.allTutorials.forEach { tutorial ->
            assertTrue("Empty steps for ${tutorial.featureKey}", tutorial.howToUseSteps.isNotEmpty())
            assertTrue("Empty whatImproves for ${tutorial.featureKey}", tutorial.whatImproves.isNotEmpty())
            assertTrue("Empty coachTip for ${tutorial.featureKey}", tutorial.coachTip.isNotBlank())
        }
        assertEquals("module_lessons", com.example.data.model.FeatureTutorialRegistry.forRoute("module_lessons/storytelling").featureKey)
        assertEquals("lesson_detail", com.example.data.model.FeatureTutorialRegistry.forRoute("lesson_detail/lesson_1").featureKey)
    }

    @Test
    fun `tutorial overlay catalog covers speaking lab and ai mentor and prefs persist completion`() = runBlocking {
        val coreSteps = com.example.ui.components.TutorialOverlayCatalog.coreWelcomeTourSteps()
        assertTrue(coreSteps.any { it.featureKey == "speaking_lab" && it.benefits.isNotEmpty() && it.usageSteps.isNotEmpty() })
        assertTrue(coreSteps.any { it.featureKey == "ai_mentor" && it.benefits.isNotEmpty() && it.usageSteps.isNotEmpty() })

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.AppPreferencesRepository(ctx)
        repo.resetAllTutorials()
        repo.markTutorialCompleted("speaking_lab")
        repo.setFirstUseTutorialsEnabled(false)
        val snapshot = repo.preferencesFlow.first()
        assertTrue(snapshot.completedTutorialKeys.contains("speaking_lab"))
        assertEquals(false, snapshot.firstUseTutorialsEnabled)

        repo.resetAllTutorials()
        val afterReset = repo.preferencesFlow.first()
        assertTrue(afterReset.completedTutorialKeys.isEmpty())
        assertEquals(true, afterReset.firstUseTutorialsEnabled)
    }

    @Test
    fun `tutorial state manager isolates completed features and toggles per userId`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.data.preferences.TutorialStateManager(ctx)
        val userA = "user_alpha_test"
        val userB = "user_beta_test"

        manager.resetUserTutorials(userA)
        manager.resetUserTutorials(userB)

        val initialA = manager.observeTutorialState(userA).first()
        val initialB = manager.observeTutorialState(userB).first()
        assertTrue(initialA.shouldShowTutorial("speaking_lab"))
        assertTrue(initialA.shouldShowTutorial("ai_mentor"))
        assertTrue(initialB.shouldShowTutorial("speaking_lab"))

        // Completing Speaking Lab for User A hides it for User A only
        manager.markFeatureCompleted(userA, "speaking_lab")
        val updatedA = manager.observeTutorialState(userA).first()
        val untouchedB = manager.observeTutorialState(userB).first()

        assertTrue(updatedA.hasCompleted("speaking_lab"))
        assertEquals(false, updatedA.shouldShowTutorial("speaking_lab"))
        assertTrue(updatedA.shouldShowTutorial("ai_mentor"))

        // User B still sees Speaking Lab first-use tutorial
        assertEquals(false, untouchedB.hasCompleted("speaking_lab"))
        assertTrue(untouchedB.shouldShowTutorial("speaking_lab"))

        // Disabling first-use tutorials for User B leaves User A unaffected
        manager.setFirstUseTutorialsEnabled(userB, false)
        val disabledB = manager.observeTutorialState(userB).first()
        assertEquals(false, disabledB.firstUseTutorialsEnabled)
        assertEquals(false, disabledB.shouldShowTutorial("ai_mentor"))
        assertTrue(manager.observeTutorialState(userA).first().firstUseTutorialsEnabled)

        // Resetting User B restores User B without clearing User A
        manager.resetUserTutorials(userB)
        val resetB = manager.observeTutorialState(userB).first()
        assertTrue(resetB.firstUseTutorialsEnabled)
        assertTrue(resetB.shouldShowTutorial("speaking_lab"))
        assertTrue(manager.observeTutorialState(userA).first().hasCompleted("speaking_lab"))
    }
}

