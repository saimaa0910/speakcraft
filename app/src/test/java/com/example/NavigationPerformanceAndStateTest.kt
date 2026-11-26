package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.test.core.app.ApplicationProvider
import com.example.data.activity.ActivityEngine
import com.example.data.activity.UniversalEvaluationEngine
import com.example.data.learning.LearningProgressManager
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathPersistenceRepository
import com.example.data.model.UserProfile
import com.example.data.notifications.NotificationManagerRepository
import com.example.ui.TabGraphRoutes
import com.example.ui.navigateAcrossTabs
import com.example.ui.screens.LearningPathDetailScreen
import com.example.ui.screens.LearningPathsOverviewScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.tabGraphForRoute
import com.example.ui.tabStartRouteForGraph
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.ActivityEngineViewModel
import com.example.ui.viewmodel.ActivitySessionStage
import com.example.ui.viewmodel.LearningPathsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationPerformanceAndStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabGraphMapping_mapsPrimarySecondaryAndNestedRoutesCorrectly() {
        assertEquals(TabGraphRoutes.DASHBOARD, tabGraphForRoute("dashboard"))
        assertEquals(TabGraphRoutes.DAILY_PRACTICE, tabGraphForRoute("daily_practice"))
        assertEquals(TabGraphRoutes.LEARNING_PATHS, tabGraphForRoute("learning_paths"))
        assertEquals(TabGraphRoutes.LEARNING_PATHS, tabGraphForRoute("learning_path_detail/prog_core_foundations"))
        assertEquals(TabGraphRoutes.LEARNING_PATHS, tabGraphForRoute("learning_path_lesson/prog_core_foundations/2"))
        assertEquals(TabGraphRoutes.LEARNING_PATHS, tabGraphForRoute("learning_path_assessment/prog_core_foundations/m2"))
        assertEquals(TabGraphRoutes.LEARNING_PATHS, tabGraphForRoute("lesson_detail/les_01"))
        assertEquals(TabGraphRoutes.ACTIVITIES, tabGraphForRoute("activity_library"))
        assertEquals(TabGraphRoutes.ACTIVITIES, tabGraphForRoute("activity/m01_daily_english_speaking"))
        assertEquals(TabGraphRoutes.COACH_PRACTICE, tabGraphForRoute("coach_practice"))
        assertEquals(TabGraphRoutes.SPEAKING_LAB, tabGraphForRoute("speaking_lab"))
        assertEquals(TabGraphRoutes.SPEAKING_LAB, tabGraphForRoute("pronunciation"))
        assertEquals(TabGraphRoutes.SPEAKING_LAB, tabGraphForRoute("challenges"))
        assertEquals(TabGraphRoutes.VOCABULARY, tabGraphForRoute("vocabulary"))
        assertEquals(TabGraphRoutes.AI_MENTOR, tabGraphForRoute("ai_mentor"))
        assertEquals(TabGraphRoutes.ANALYTICS, tabGraphForRoute("analytics"))
        assertEquals(TabGraphRoutes.PROFILE, tabGraphForRoute("profile"))
        assertEquals(TabGraphRoutes.SETTINGS, tabGraphForRoute("settings"))
        assertEquals(TabGraphRoutes.SETTINGS, tabGraphForRoute("admin_console"))

        assertEquals("learning_paths", tabStartRouteForGraph(TabGraphRoutes.LEARNING_PATHS))
        assertEquals("vocabulary", tabStartRouteForGraph(TabGraphRoutes.VOCABULARY))
        assertEquals("activity_library", tabStartRouteForGraph(TabGraphRoutes.ACTIVITIES))
        assertEquals("dashboard", tabStartRouteForGraph(TabGraphRoutes.DASHBOARD))
    }

    @Test
    fun nestedTabBackStack_learningPathsModule2AndVocabularyTab_preservedAcrossSwitchesAndReTapPopsToRoot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LearningPathPersistenceRepository(
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
        val lpVm = LearningPathsViewModel(
            persistenceRepository = repo,
            computeDispatcher = Dispatchers.Unconfined
        )
        val program = LearningPathCurriculumRegistry.allPrograms.first()
        val module2 = program.modules[1]
        lateinit var currentRouteHolder: () -> String
        lateinit var navControllerRef: NavHostController

        composeRule.setContent {
            val navController = rememberNavController()
            navControllerRef = navController
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route ?: "dashboard"
            currentRouteHolder = { currentRoute }

            LaunchedEffect(Unit) {
                lpVm.syncAccountAndEnrollments(
                    userId = "nav_test_user",
                    profile = UserProfile(displayName = "Test", communicationLevel = "Intermediate"),
                    remoteEnrollments = emptyList()
                )
            }

            val uiState by lpVm.uiState.collectAsStateWithLifecycle()

            SpeakCraftTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row {
                        TextButton(onClick = { navigateAcrossTabs(navController, "dashboard") }) {
                            Text("TabHome")
                        }
                        TextButton(onClick = { navigateAcrossTabs(navController, "learning_paths") }) {
                            Text("TabLearn")
                        }
                        TextButton(onClick = { navigateAcrossTabs(navController, "vocabulary") }) {
                            Text("TabVocab")
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = TabGraphRoutes.DASHBOARD,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                        modifier = Modifier.weight(1f)
                    ) {
                        navigation(startDestination = "dashboard", route = TabGraphRoutes.DASHBOARD) {
                            composable("dashboard") {
                                Box { Text("Home Content") }
                            }
                        }
                        navigation(startDestination = "learning_paths", route = TabGraphRoutes.LEARNING_PATHS) {
                            composable("learning_paths") {
                                LearningPathsOverviewScreen(
                                    uiState = uiState,
                                    onSearchChange = { lpVm.setSearchQuery(it) },
                                    onCategoryFilterChange = { lpVm.setCategoryFilter(it) },
                                    onPreviewProgram = { lpVm.openPreview(it) },
                                    onEnrollProgram = { lpVm.enrollInProgram(it) },
                                    onTogglePauseProgram = { lpVm.togglePauseProgram(it) },
                                    onUnenrollProgram = { lpVm.unenrollProgram(it) },
                                    onOpenProgramDetail = { pid ->
                                        navigateAcrossTabs(navController, "learning_path_detail/$pid")
                                    },
                                    onOpenLesson = { pid, day ->
                                        navigateAcrossTabs(navController, "learning_path_lesson/$pid/$day")
                                    },
                                    onDismissBanner = {}
                                )
                            }
                            composable(
                                route = "learning_path_detail/{programId}",
                                arguments = listOf(navArgument("programId") { type = NavType.StringType })
                            ) { entry ->
                                val pid = entry.arguments?.getString("programId").orEmpty()
                                LearningPathDetailScreen(
                                    programId = pid,
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                    onEnroll = { lpVm.enrollInProgram(it) },
                                    onTogglePause = { lpVm.togglePauseProgram(it) },
                                    onOpenLesson = { p, d ->
                                        navigateAcrossTabs(navController, "learning_path_lesson/$p/$d")
                                    },
                                    onToggleDayCompleteQuick = { p, d -> lpVm.toggleDayCompletionQuick(p, d) },
                                    onOpenAssessment = { p, m ->
                                        navigateAcrossTabs(navController, "learning_path_assessment/$p/$m")
                                    },
                                    onOpenProgram = { target ->
                                        navigateAcrossTabs(navController, "learning_path_detail/$target")
                                    }
                                )
                            }
                        }
                        navigation(startDestination = "vocabulary", route = TabGraphRoutes.VOCABULARY) {
                            composable("vocabulary") {
                                VocabularyScreen(
                                    entries = emptyList(),
                                    todayEpochDay = 20_000L,
                                    vocabEvalState = null,
                                    onSpeakWord = {},
                                    onReviewCard = { _, _, _ -> },
                                    onEvaluateUsage = { _, _, _ -> },
                                    onSaveCustomWord = { _, _, _, _, _ -> },
                                    onClearEval = {}
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Switch from Home -> Learning Paths
        composeRule.onNodeWithText("TabLearn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("learning_paths_overview").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Open first program's detail screen
        composeRule.onNodeWithTag("learning_paths_overview")
            .performScrollToNode(hasTestTag("lp_explore_btn_${program.programId}"))
        composeRule.onNodeWithTag("lp_explore_btn_${program.programId}").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("lp_detail_screen").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("learning_path_detail/{programId}", currentRouteHolder())

        // 3. Expand Module 2 and confirm its first lesson row is visible
        composeRule.onNodeWithTag("lp_detail_screen")
            .performScrollToNode(hasTestTag("lp_module_card_${module2.moduleId}"))
        composeRule.onNodeWithText(module2.title).performClick()
        composeRule.onNodeWithTag("lp_detail_screen")
            .performScrollToNode(hasTestTag("lp_lesson_row_${module2.dayStart}"))
        composeRule.onNodeWithTag("lp_lesson_row_${module2.dayStart}").assertIsDisplayed()

        // 4. Switch to Vocabulary Builder and pick the Flashcards sub-tab
        composeRule.onNodeWithText("TabVocab").performClick()
        composeRule.onNodeWithText("Flashcards").performClick()
        composeRule.onNodeWithTag("vocab_flashcard").assertIsDisplayed()
        assertEquals("vocabulary", currentRouteHolder())

        // 5. Switch back to Learning Paths -> must return to Program Detail with Module 2 STILL expanded!
        composeRule.onNodeWithText("TabLearn").performClick()
        composeRule.onNodeWithTag("lp_detail_screen").assertIsDisplayed()
        assertEquals("learning_path_detail/{programId}", currentRouteHolder())
        composeRule.onNodeWithTag("lp_lesson_row_${module2.dayStart}").assertIsDisplayed()

        // 6. Switch back to Vocabulary -> Flashcards sub-tab is STILL preserved!
        composeRule.onNodeWithText("TabVocab").performClick()
        composeRule.onNodeWithTag("vocab_flashcard").assertIsDisplayed()
        assertEquals("vocabulary", navControllerRef.currentBackStackEntry?.destination?.route)

        // 7. Return to Home (dashboard), then back to Learning Paths -> Module 2 STILL expanded!
        composeRule.onNodeWithText("TabHome").performClick()
        composeRule.onNodeWithText("Home Content").assertIsDisplayed()
        assertEquals("dashboard", navControllerRef.currentBackStackEntry?.destination?.route)

        composeRule.onNodeWithText("TabLearn").performClick()
        composeRule.onNodeWithTag("lp_detail_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("lp_lesson_row_${module2.dayStart}").assertIsDisplayed()
        assertEquals("learning_path_detail/{programId}", currentRouteHolder())
        assertEquals("learning_path_detail/{programId}", navControllerRef.currentBackStackEntry?.destination?.route)

        // 8. Re-tap active TabLearn -> pops cleanly to Learning Paths overview
        composeRule.onNodeWithText("TabLearn").performClick()
        composeRule.onNodeWithTag("learning_paths_overview").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals("learning_paths", navControllerRef.currentBackStackEntry?.destination?.route)
        assertEquals("learning_paths", currentRouteHolder())
    }

    @Test
    fun activityEngineViewModel_loadActivity_preservesUnfinishedSessionUnlessForceReset() {
        val engine = ActivityEngine(
            evaluationEngine = UniversalEvaluationEngine(aiClientOverride = { _, _, _, _ -> null })
        )
        val vm = ActivityEngineViewModel(
            activityRepository = null,
            activityEngine = engine,
            audioRecorder = null,
            userId = "preserve_user",
            computeDispatcher = Dispatchers.Unconfined
        )

        vm.loadActivity("m01_daily_english_speaking")
        vm.startActivity()
        vm.updateTextResponse("Practicing structured executive speaking cleanly.")
        vm.updateConfidenceSelfRating(84)

        // Simulate tab restore re-running loadActivity for the same activityId
        vm.loadActivity("m01_daily_english_speaking")
        val preserved = vm.activeState.value
        assertTrue(preserved.stage is ActivitySessionStage.Active)
        assertEquals("Practicing structured executive speaking cleanly.", preserved.textResponse)
        assertEquals(84, preserved.confidenceSelfRating)

        // Explicit retry forces a clean reset
        vm.retryCurrentActivity()
        val reset = vm.activeState.value
        assertTrue(reset.stage is ActivitySessionStage.Active)
        assertEquals("", reset.textResponse)
        assertEquals(70, reset.confidenceSelfRating)
    }

    @Test
    fun asyncRepositoriesAndLearningPathsSync_loadOffMainAndDeduplicateCleanly() = runTest {
        val tempRoot = File.createTempFile("nav_perf_state", "").apply {
            delete()
            mkdirs()
        }
        try {
            val lpDir = File(tempRoot, "lp")
            val notifDir = File(tempRoot, "notif")
            val progressManager = LearningProgressManager(
                storageDir = lpDir,
                persistScope = this,
                ioDispatcher = Dispatchers.Unconfined
            )
            val notificationManager = NotificationManagerRepository(
                customStorageDir = notifDir,
                persistScope = this,
                ioDispatcher = Dispatchers.Unconfined
            )
            val profile = UserProfile(
                displayName = "Ava",
                communicationLevel = "Intermediate",
                primaryGoal = "Executive Presence",
                dailyGoalMinutes = 20L
            )

            progressManager.loadForUserAsync("user_async_1", profile, emptyList())
            progressManager.loadForUserAsync("user_async_1", profile, emptyList())
            assertEquals("user_async_1", progressManager.state.value.userId)

            notificationManager.loadForUserAsync("user_async_1", profile, sessionsCount = 2, streakDays = 3, nowMs = 1_700_000_000_000L)
            notificationManager.loadForUserAsync("user_async_1", profile, sessionsCount = 2, streakDays = 3, nowMs = 1_700_000_000_000L)
            assertEquals("user_async_1", notificationManager.state.value.userId)
            assertTrue(notificationManager.state.value.alerts.isNotEmpty())

            val context = ApplicationProvider.getApplicationContext<Context>()
            val lpVm = LearningPathsViewModel(
                persistenceRepository = LearningPathPersistenceRepository(context, Dispatchers.Unconfined),
                computeDispatcher = Dispatchers.Unconfined
            )
            lpVm.syncAccountAndEnrollments("user_async_1", profile, emptyList())
            lpVm.syncAccountAndEnrollments("user_async_1", profile, emptyList())
            assertEquals(7, lpVm.uiState.value.programStats.size)
            assertEquals(7, lpVm.uiState.value.programs.size)
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
