package com.example

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathPersistenceRepository
import com.example.data.model.UserProfile
import com.example.ui.components.ActiveLearningPathBannerUi
import com.example.ui.components.ActiveLearningPathDashboardCard
import com.example.ui.screens.LearningPathAssessmentScreen
import com.example.ui.screens.LearningPathDetailScreen
import com.example.ui.screens.LearningPathLessonScreen
import com.example.ui.screens.LearningPathsOverviewScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.LearningPathsViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class LearningPathsUiComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var repo: LearningPathPersistenceRepository
    private lateinit var vm: LearningPathsViewModel
    private val testUid = "ui_test_user_${System.currentTimeMillis()}"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = LearningPathPersistenceRepository(
            context = context,
            ioDispatcher = Dispatchers.Main.immediate
        )
        vm = LearningPathsViewModel(
            persistenceRepository = repo,
            computeDispatcher = Dispatchers.Main.immediate
        )
        vm.syncAccountAndEnrollments(
            userId = testUid,
            profile = UserProfile(
                userId = testUid,
                displayName = "Alex",
                communicationLevel = "Intermediate",
                primaryGoal = "Job Interviews"
            ),
            remoteEnrollments = emptyList()
        )
        composeRule.waitForIdle()
    }

    @Test
    fun overview_displays_all_7_programs_supports_preview_before_enroll_and_enrollment() {
        var openedDetailId = ""
        composeRule.setContent {
            val uiState by vm.uiState.collectAsState()
            SpeakCraftTheme {
                LearningPathsOverviewScreen(
                    uiState = uiState,
                    onSearchChange = vm::setSearchQuery,
                    onCategoryFilterChange = vm::setCategoryFilter,
                    onPreviewProgram = vm::openPreview,
                    onEnrollProgram = vm::enrollInProgram,
                    onTogglePauseProgram = vm::togglePauseProgram,
                    onUnenrollProgram = vm::unenrollProgram,
                    onOpenProgramDetail = { openedDetailId = it },
                    onOpenLesson = { _, _ -> },
                    onDismissBanner = vm::dismissBanner
                )
            }
        }

        val listTag = "learning_paths_overview"
        composeRule.onNodeWithTag(listTag).assertIsDisplayed()
        composeRule.onNodeWithText("7 Complete Learning Programs").assertIsDisplayed()

        // Scroll LazyColumn down to Specialized program card and preview BEFORE enrolling
        composeRule.onNodeWithTag(listTag)
            .performScrollToNode(hasTestTag("lp_preview_btn_pronunciation_program"))
        composeRule.onNodeWithTag("lp_program_card_pronunciation_program").assertIsDisplayed()
        composeRule.onNodeWithTag("lp_preview_btn_pronunciation_program").performClick()

        composeRule.onNodeWithTag("lp_preview_dialog_pronunciation_program").assertIsDisplayed()
        composeRule.onNodeWithTag("lp_preview_enroll_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            vm.uiState.value.userState.enrollments.containsKey("pronunciation_program")
        }
        assertTrue(vm.uiState.value.userState.enrollments.containsKey("pronunciation_program"))

        // Filter chip filters cleanly down to Enrolled
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_filter_Enrolled"))
        composeRule.onNodeWithTag("lp_filter_Enrolled").performClick()
        composeRule.onNodeWithTag(listTag)
            .performScrollToNode(hasTestTag("lp_syllabus_btn_pronunciation_program"))
        composeRule.onNodeWithTag("lp_syllabus_btn_pronunciation_program").performClick()
        assertEquals("pronunciation_program", openedDetailId)
    }

    @Test
    fun detail_screen_renders_objectives_all_modules_lessons_and_checkpoints() {
        var openedDay = -1
        var openedAssessmentModule = ""

        composeRule.setContent {
            val uiState by vm.uiState.collectAsState()
            SpeakCraftTheme {
                LearningPathDetailScreen(
                    programId = "vocab_challenge",
                    uiState = uiState,
                    onBack = {},
                    onEnroll = vm::enrollInProgram,
                    onTogglePause = vm::togglePauseProgram,
                    onOpenLesson = { _, day -> openedDay = day },
                    onToggleDayCompleteQuick = vm::toggleDayCompletionQuick,
                    onOpenAssessment = { _, mod -> openedAssessmentModule = mod },
                    onOpenProgram = {}
                )
            }
        }

        val listTag = "lp_detail_screen"
        composeRule.onNodeWithTag(listTag).assertIsDisplayed()
        composeRule.onNodeWithText("Daily Vocabulary Challenge").assertIsDisplayed()
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasText("Program Learning Objectives"))
        composeRule.onNodeWithText("Program Learning Objectives").assertIsDisplayed()

        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_lesson_row_1"))
        composeRule.onNodeWithTag("lp_lesson_row_1").performClick()
        assertEquals(1, openedDay)

        composeRule.onNodeWithTag(listTag)
            .performScrollToNode(hasTestTag("lp_open_assessment_vocab_challenge_mod_1"))
        composeRule.onNodeWithTag("lp_open_assessment_vocab_challenge_mod_1").performClick()
        assertEquals("vocab_challenge_mod_1", openedAssessmentModule)
    }

    @Test
    fun lesson_screen_evaluates_spoken_draft_and_renders_transcript_feedback() {
        composeRule.setContent {
            val uiState by vm.uiState.collectAsState()
            SpeakCraftTheme {
                LearningPathLessonScreen(
                    programId = "30_day_challenge",
                    dayNumber = 6,
                    uiState = uiState,
                    onPrepareLesson = vm::prepareLessonSession,
                    onBackToSyllabus = {},
                    onNavigateDay = {},
                    onOpenAssessment = { _, _ -> },
                    onOpenActivityStudio = {},
                    onOpenLabRoute = {},
                    onUpdateDraftText = vm::updateDraftResponseText,
                    onSelectQuizOption = vm::selectQuizOption,
                    onToggleChecklistItem = vm::toggleChecklistItem,
                    onSetConfidence = vm::setConfidenceRating,
                    onUpdateReflectionNote = vm::updateReflectionNote,
                    onSaveReflectionNote = { vm.saveReflectionNoteNow("30_day_challenge", 6) },
                    onPlayModelTts = {},
                    onStopModelTts = {},
                    onStartOrResumeRecording = {},
                    onPauseRecording = {},
                    onStopRecording = {},
                    onTogglePlayback = {},
                    onSubmitForEvaluation = { vm.submitAndEvaluateLesson("30_day_challenge", 6) }
                )
            }
        }

        val listTag = "lp_lesson_screen"
        composeRule.onNodeWithTag(listTag).assertIsDisplayed()
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_lesson_response_input"))
        composeRule.onNodeWithTag("lp_lesson_response_input")
            .performTextInput("Um last week I simplified our morning sync agenda and paused silently before stating priorities, which gave our whole squad much clearer clarity instead of rushing.")

        val day6 = LearningPathCurriculumRegistry.getLesson("30_day_challenge", 6)!!
        val qId = day6.quizQuestions.first().questionId
        val ansIdx = day6.quizQuestions.first().correctIndex
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_quiz_${qId}_$ansIdx"))
        composeRule.onNodeWithTag("lp_quiz_${qId}_$ansIdx").performClick()

        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_lesson_submit_btn"))
        composeRule.onNodeWithTag("lp_lesson_submit_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.uiState.value.activeFeedback != null
        }

        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_feedback_score_card"))
        composeRule.onNodeWithTag("lp_feedback_score_card").assertIsDisplayed()
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasText("What Went Well", substring = true))
        composeRule.onNodeWithText("What Went Well", substring = true).assertIsDisplayed()
    }

    @Test
    fun assessment_screen_grades_capstone_and_displays_combined_score() {
        val mod = LearningPathCurriculumRegistry.getModule("interview_program", "interview_program_mod_3")!!
        val spec = mod.assessment

        composeRule.setContent {
            val uiState by vm.uiState.collectAsState()
            SpeakCraftTheme {
                LearningPathAssessmentScreen(
                    programId = "interview_program",
                    moduleId = mod.moduleId,
                    uiState = uiState,
                    onPrepareAssessment = vm::prepareModuleAssessment,
                    onSelectScenarioOption = vm::selectAssessmentOption,
                    onUpdatePracticalText = vm::updateAssessmentPracticalText,
                    onSetConfidence = vm::setAssessmentConfidence,
                    onSubmitAssessment = { vm.submitModuleAssessment("interview_program", mod.moduleId) },
                    onBackToSyllabus = {}
                )
            }
        }

        val listTag = "lp_assessment_screen"
        composeRule.onNodeWithTag(listTag).assertIsDisplayed()
        spec.scenarioQuestions.forEach { q ->
            composeRule.onNodeWithTag(listTag)
                .performScrollToNode(hasTestTag("lp_assess_q_${q.questionId}_${q.correctIndex}"))
            composeRule.onNodeWithTag("lp_assess_q_${q.questionId}_${q.correctIndex}").performClick()
        }
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_assessment_practical_input"))
        composeRule.onNodeWithTag("lp_assessment_practical_input")
            .performTextInput("Bringing seven years building reliable mobile systems, I clarify ambiguous goals early, listen calmly under pushback, and proved this by reducing customer onboarding attrition forty percent across distributed teams.")

        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_assessment_submit_btn"))
        composeRule.onNodeWithTag("lp_assessment_submit_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.uiState.value.activeAssessmentResult != null
        }

        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag("lp_assessment_result_card"))
        composeRule.onNodeWithTag("lp_assessment_result_card").assertIsDisplayed()
    }

    @Test
    fun dashboard_active_path_card_triggers_resume_and_browse_cleanly() {
        var resumedProgram = ""
        var resumedDay = -1
        var browsed = false

        composeRule.setContent {
            SpeakCraftTheme {
                androidx.compose.foundation.layout.Column {
                    ActiveLearningPathDashboardCard(
                        activeBanner = null,
                        onResumeLesson = { _, _ -> },
                        onExploreAllPaths = { browsed = true }
                    )
                    ActiveLearningPathDashboardCard(
                        activeBanner = ActiveLearningPathBannerUi(
                            programId = "30_day_challenge",
                            programTitle = "30-Day Communication Challenge",
                            completedDaysCount = 5,
                            totalDays = 30,
                            nextDayNumber = 6,
                            nextLessonTitle = "Deliberate Pace & Micro-Pauses",
                            completionFraction = 5f / 30f
                        ),
                        onResumeLesson = { p, d ->
                            resumedProgram = p
                            resumedDay = d
                        },
                        onExploreAllPaths = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("280 Guided Lessons & Capstones").assertIsDisplayed()
        composeRule.onNodeWithText("Browse").performClick()
        assertTrue(browsed)

        composeRule.onNodeWithText("30-Day Communication Challenge").assertIsDisplayed()
        composeRule.onNodeWithText("Day 6").performClick()
        assertEquals("30_day_challenge", resumedProgram)
        assertEquals(6, resumedDay)
    }
}
