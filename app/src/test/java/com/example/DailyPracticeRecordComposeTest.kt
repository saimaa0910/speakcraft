package com.example

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.learning.LearningProgressManager
import com.example.ui.screens.DailyPracticeScreen
import com.example.ui.theme.SpeakCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Daily Practice: the current task set, "Mark done", the next set straight away, and the daily record. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class DailyPracticeRecordComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var today = "2026-11-24"
    private var tick = 1_764_000_000_000L
    private val manager = LearningProgressManager(
        storageDir = null,
        clockMs = { tick += 60_000L; tick },
        todayKeyProvider = { today }
    )

    @Test
    fun current_set_mark_done_next_set_and_todays_record_with_previous_days() {
        // Yesterday: one task done, so a previous day exists in the record.
        manager.loadForUser("ui_daily_user")
        manager.completeDailyTask(manager.state.value.dailyPlan!!.tasks.last().id)
        today = "2026-11-25"
        manager.loadForUser("ui_daily_user")

        composeRule.setContent {
            val snapshot by manager.state.collectAsState()
            val pkg by manager.dailyPackage.collectAsState()
            SpeakCraftTheme {
                DailyPracticeScreen(
                    todayDateKey = today,
                    todayActivity = null,
                    todaysWords = emptyList(),
                    dailyPackage = pkg,
                    onToggleStep = { _, _, _ -> },
                    onSaveReflection = { _, _ -> },
                    onNavigate = {},
                    dailyTasks = snapshot.dailyPlan?.tasks,
                    setNumberToday = snapshot.dailyRotation.setIndexToday + 1,
                    dailyRecords = snapshot.dailyRecords,
                    onCompleteTask = { manager.completeDailyTask(it) }
                )
            }
        }

        val list = composeRule.onNodeWithTag("daily_practice_list")
        val firstTask = manager.state.value.dailyPlan!!.tasks.first()
        list.performScrollToNode(hasTestTag("daily_task_done_${firstTask.id}"))
        composeRule.onNodeWithText("Set 1").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_task_done_${firstTask.id}").performClick()
        composeRule.waitForIdle()

        // Today's record shows the task under its exercise group, as self-reported.
        list.performScrollToNode(hasTestTag("daily_record_card"))
        composeRule.onNodeWithText("Today's record").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("daily_record_group_daily_vocabulary"))
        composeRule.onNodeWithTag("daily_record_group_daily_vocabulary").assertIsDisplayed()

        // Finishing the rest of the set brings the next set immediately.
        manager.state.value.dailyPlan!!.tasks.filter { !it.isCompleted }.forEach { manager.completeDailyTask(it.id) }
        composeRule.waitForIdle()
        assertEquals("the next set started", 1, manager.state.value.dailyRotation.setIndexToday)
        list.performScrollToNode(hasTestTag("daily_task_set_number"))
        composeRule.onNodeWithTag("daily_task_set_number").assertTextEquals("Set 2")

        // Previous days are one tap away.
        list.performScrollToNode(hasTestTag("daily_record_day_2026-11-24"))
        composeRule.onNodeWithTag("daily_record_day_2026-11-24").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Record for 2026-11-24").assertIsDisplayed()
    }
}
