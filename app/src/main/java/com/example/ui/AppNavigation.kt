package com.example.ui

import com.example.appContainer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.data.AppContainer
import com.example.data.activity.ActivityCatalogue
import com.example.data.ai.InboundGeminiLink
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.learning.LearningLevel
import com.example.data.learning.LearningProgressManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.FeatureTutorialRegistry
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.notifications.NotificationManagerRepository
import com.example.data.preferences.AISettings
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.TutorialState
import com.example.ui.components.FeatureTutorialDialog
import com.example.ui.components.LaunchExperienceViewModel
import com.example.ui.components.NotificationDrawer
import com.example.ui.components.SpeakCraftLaunchOverlay
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.components.TutorialOverlayStep
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.screens.ActivityDiscoveryScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.ConversationSimulatorScreen
import com.example.ui.screens.DailyPracticeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GrammarSpeakingScreen
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.ui.components.ActiveLearningPathBannerUi
import com.example.ui.screens.LearningPathAssessmentScreen
import com.example.ui.screens.LearningPathDetailScreen
import com.example.ui.screens.LearningPathLessonScreen
import com.example.ui.screens.LearningPathsOverviewScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.ListeningShadowingScreen
import com.example.ui.screens.ModuleLessonsScreen
import com.example.ui.screens.OnboardingAssessmentScreen
import com.example.ui.screens.PronunciationLabScreen
import com.example.ui.screens.PublicSpeakingScreen
import com.example.ui.screens.RealWorldTasksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.screens.VoiceTrainingLabScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.theme.speakCraftColors
import com.example.ui.screens.CoachPracticeRoute
import com.example.ui.viewmodel.ActivityEngineViewModel
import com.example.ui.viewmodel.CoachPracticeViewModel
import com.example.ui.viewmodel.OnlineConsent
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.LearningPathsViewModel
import com.example.ui.viewmodel.MainCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal object TabGraphRoutes {
    const val DASHBOARD = "tab_dashboard"
    const val DAILY_PRACTICE = "tab_daily_practice"
    const val LEARNING_PATHS = "tab_learning_paths"
    const val ACTIVITIES = "tab_activities"
    const val COACH_PRACTICE = "tab_coach_practice"
    const val SPEAKING_LAB = "tab_speaking_lab"
    const val VOCABULARY = "tab_vocabulary"
    const val AI_MENTOR = "tab_ai_mentor"
    const val ANALYTICS = "tab_analytics"
    const val PROFILE = "tab_profile"
    const val SETTINGS = "tab_settings"
}

internal fun tabGraphForRoute(route: String): String {
    val clean = route.substringBefore('?').trim()
    return when {
        clean == "dashboard" -> TabGraphRoutes.DASHBOARD
        clean == "daily_practice" -> TabGraphRoutes.DAILY_PRACTICE
        clean == "learning_paths" ||
            clean.startsWith("learning_path_detail") ||
            clean.startsWith("learning_path_lesson") ||
            clean.startsWith("learning_path_assessment") ||
            clean.startsWith("module_lessons") ||
            clean.startsWith("lesson_detail") -> TabGraphRoutes.LEARNING_PATHS
        clean == "activity_library" ||
            clean.startsWith("activity_library/") ||
            clean.startsWith("activity/") -> TabGraphRoutes.ACTIVITIES
        clean == "coach_practice" -> TabGraphRoutes.COACH_PRACTICE
        clean == "speaking_lab" ||
            clean == "challenges" ||
            clean == "articulation_lab" ||
            clean == "pronunciation" ||
            clean == "voice_training" ||
            clean == "conversation" ||
            clean == "listening_lab" ||
            clean == "grammar_speaking" ||
            clean == "public_speaking" ||
            clean == "real_world_tasks" -> TabGraphRoutes.SPEAKING_LAB
        clean == "vocabulary" -> TabGraphRoutes.VOCABULARY
        clean == "ai_mentor" -> TabGraphRoutes.AI_MENTOR
        clean == "analytics" -> TabGraphRoutes.ANALYTICS
        clean == "profile" || clean == "profile_recordings" -> TabGraphRoutes.PROFILE
        clean == "settings" || clean == "admin_console" -> TabGraphRoutes.SETTINGS
        else -> TabGraphRoutes.DASHBOARD
    }
}

internal fun tabStartRouteForGraph(tabGraph: String): String = when (tabGraph) {
    TabGraphRoutes.DASHBOARD -> "dashboard"
    TabGraphRoutes.DAILY_PRACTICE -> "daily_practice"
    TabGraphRoutes.LEARNING_PATHS -> "learning_paths"
    TabGraphRoutes.ACTIVITIES -> "activity_library"
    TabGraphRoutes.COACH_PRACTICE -> "coach_practice"
    TabGraphRoutes.SPEAKING_LAB -> "speaking_lab"
    TabGraphRoutes.VOCABULARY -> "vocabulary"
    TabGraphRoutes.AI_MENTOR -> "ai_mentor"
    TabGraphRoutes.ANALYTICS -> "analytics"
    TabGraphRoutes.PROFILE -> "profile"
    TabGraphRoutes.SETTINGS -> "settings"
    else -> "dashboard"
}

/**
 * Unified multi-back-stack router for all SpeakCraft tabs and drill-downs:
 * - Switching to another tab graph saves the current tab's back stack & UI state and restores the target tab's saved stack.
 * - Re-tapping an already active tab pops cleanly to that tab's start destination (no-op if already at root).
 * - Pushing a child route inside the same tab adds normally onto that tab's preserved stack.
 */
internal fun navigateAcrossTabs(navController: NavHostController, route: String) {
    val cleanRoute = route.trim()
    if (cleanRoute.isBlank()) return
    val targetGraph = tabGraphForRoute(cleanRoute)
    val targetStartRoute = tabStartRouteForGraph(targetGraph)
    val currentDest = navController.currentBackStackEntry?.destination
    val currentGraph = currentDest?.hierarchy
        ?.firstOrNull { it.route?.startsWith("tab_") == true }
        ?.route
    val currentRoutePattern = currentDest?.route

    if (currentGraph == targetGraph) {
        if (cleanRoute == targetStartRoute) {
            if (currentRoutePattern != targetStartRoute) {
                navController.popBackStack(targetStartRoute, inclusive = false)
            }
            return
        }
        navController.navigate(cleanRoute) {
            launchSingleTop = true
        }
        return
    }

    navController.navigate(targetGraph) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    if (cleanRoute != targetStartRoute) {
        val restoredRouteNow = navController.currentBackStackEntry?.destination?.route
        val restoredArgsNow = navController.currentBackStackEntry?.arguments
        val alreadyAtExactTarget = when {
            cleanRoute.startsWith("learning_path_detail/") ->
                restoredRouteNow == "learning_path_detail/{programId}" &&
                    restoredArgsNow?.getString("programId") == cleanRoute.removePrefix("learning_path_detail/")
            cleanRoute.startsWith("learning_path_lesson/") -> {
                val parts = cleanRoute.removePrefix("learning_path_lesson/").split("/")
                restoredRouteNow == "learning_path_lesson/{programId}/{dayNumber}" &&
                    parts.size == 2 &&
                    restoredArgsNow?.getString("programId") == parts[0] &&
                    restoredArgsNow?.getInt("dayNumber")?.toString() == parts[1]
            }
            cleanRoute.startsWith("activity/") ->
                restoredRouteNow == "activity/{activityId}" &&
                    restoredArgsNow?.getString("activityId") == cleanRoute.removePrefix("activity/")
            else -> restoredRouteNow == cleanRoute
        }
        if (!alreadyAtExactTarget) {
            navController.navigate(cleanRoute) {
                launchSingleTop = true
            }
        }
    }
}

private val compactPrimaryRoutes = setOf(
    "dashboard",
    "daily_practice",
    "coach_practice",
    "speaking_lab",
    "ai_mentor"
)

private val sidebarPrimaryRoutes = setOf(
    "dashboard",
    "activity_library",
    "daily_practice",
    "ai_mentor",
    "learning_paths",
    "coach_practice",
    "speaking_lab",
    "vocabulary",
    "analytics",
    "settings"
)

private data class PrimaryNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryNavItems = listOf(
    PrimaryNavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    PrimaryNavItem("daily_practice", "Daily", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    PrimaryNavItem("coach_practice", "Coach", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    PrimaryNavItem("speaking_lab", "Record", Icons.Filled.Mic, Icons.Outlined.Mic),
    PrimaryNavItem("ai_mentor", "Mentor", Icons.Filled.Psychology, Icons.Outlined.Psychology)
)

private val routeTitles = mapOf(
    "dashboard" to "SpeakCraft",
    "daily_practice" to "Daily Practice",
    "learning_paths" to "Learning Paths",
    "vocabulary" to "Vocabulary Builder",
    "speaking_lab" to "Record & Review",
    "coach_practice" to "Coach Practice",
    "challenges" to "Challenges",
    "pronunciation" to "Phonetic & Intonation Lab",
    "articulation_lab" to "Articulation & Enunciation Lab",
    "voice_training" to "Voice Training Lab",
    "conversation" to "Conversation Mastery",
    "listening_lab" to "Active Listening",
    "grammar_speaking" to "Thinking & Articulation",
    "public_speaking" to "Public Speaking",
    "real_world_tasks" to "Real-World Tasks",
    "analytics" to "Progress Analytics",
    "ai_mentor" to "AI Mentor",
    "settings" to "Settings",
    "profile" to "My Profile",
    "profile_recordings" to "My Recordings",
    "admin_console" to "Admin Console",
    "activity_library" to "Activity Engine (150)"
)

@Composable
fun SpeakCraftRoot(
    auth: FirebaseAuth = Firebase.auth,
    launchVm: LaunchExperienceViewModel = viewModel(),
    inboundGeminiLink: InboundGeminiLink? = null,
    onConsumeInboundGeminiLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val prefsSnapshot by remember(container) {
        container.preferencesRepository.preferencesFlow.map { it to true }
    }.collectAsStateWithLifecycle(initialValue = AppPreferences() to false)
    val (prefs, prefsLoaded) = prefsSnapshot
    val darkTheme = when (prefs.themeMode) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val reducedMotion = rememberReducedMotionEnabled()
    val launchVisible by launchVm.isOverlayVisible.collectAsStateWithLifecycle()
    val latchedCadence by launchVm.latchedCadence.collectAsStateWithLifecycle()

    LaunchedEffect(prefsLoaded, reducedMotion) {
        if (prefsLoaded) {
            val firstLaunch = !prefs.hasCompletedInitialLaunch
            launchVm.latchCadenceIfNeeded(
                isFirstLaunch = firstLaunch,
                reducedMotion = reducedMotion
            )
            if (firstLaunch) {
                container.preferencesRepository.markInitialLaunchCompleted()
            }
        }
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    SpeakCraftTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            val user = currentUser
            if (user == null) {
                AuthScreen(onAuthSuccess = {})
            } else {
                key(user.uid) {
                    AuthenticatedAppShell(
                        userId = user.uid,
                        userDisplayName = user.displayName.orEmpty(),
                        userEmail = user.email.orEmpty(),
                        prefs = prefs,
                        prefsLoaded = prefsLoaded,
                        container = container,
                        inboundGeminiLink = inboundGeminiLink,
                        onConsumeInboundGeminiLink = onConsumeInboundGeminiLink
                    )
                }
            }

            if (launchVisible) {
                val activeCadence = latchedCadence
                if (activeCadence == null) {
                    // Seamless Stage 0 hold for the few milliseconds DataStore loads prefs
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("launch_animation_settling_bg")
                    )
                } else {
                    SpeakCraftLaunchOverlay(
                        visible = true,
                        cadence = activeCadence,
                        reducedMotion = reducedMotion,
                        onStageChanged = { stage -> launchVm.updateStage(stage) },
                        onFinished = { launchVm.markCompleted() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    userId: String,
    userDisplayName: String,
    userEmail: String,
    prefs: AppPreferences,
    prefsLoaded: Boolean,
    container: AppContainer,
    inboundGeminiLink: InboundGeminiLink? = null,
    onConsumeInboundGeminiLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val ttsHelper = remember { TtsAndSpeechHelper(context.applicationContext) }
    val prefsRepo: AppPreferencesRepository = container.preferencesRepository
    val tutorialStateManager = container.tutorialStateManager
    val tutorialSnapshot by remember(tutorialStateManager, userId) {
        tutorialStateManager.observeTutorialState(userId).map { it to true }
    }.collectAsStateWithLifecycle(initialValue = TutorialState(userId = userId) to false)
    val (tutorialState, tutorialStateLoaded) = tutorialSnapshot

    DisposableEffect(Unit) {
        onDispose { ttsHelper.release() }
    }

    val mainVm: MainCoachViewModel = viewModel(
        key = "main_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                MainCoachViewModel(
                    repository = container.repository,
                    localOrchestrator = container.localIntelligenceOrchestrator,
                    currentUserId = userId,
                    hybridCoordinator = container.hybridAiCoordinator,
                    learningProgressManager = container.learningProgressManager
                )
            }
        }
    )

    val speakingVm: SpeakingAndLabsViewModel = viewModel(
        key = "speak_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                SpeakingAndLabsViewModel(
                    repository = container.repository,
                    audioManager = container.audioRecorderManager,
                    aiSettingsRepo = container.aiSettingsRepository,
                    liveTranscriptionManager = container.liveTranscriptionManager,
                    capabilityManager = container.intelligenceCapabilityManager,
                    tipCatalog = { container.tipCatalog }
                )
            }
        }
    )

    val coachVm: InteractiveCoachViewModel = viewModel(
        key = "coach_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                InteractiveCoachViewModel(
                    repository = container.repository,
                    localOrchestrator = container.localIntelligenceOrchestrator,
                    appContext = context.applicationContext,
                    currentUserId = userId,
                    hybridCoordinator = container.hybridAiCoordinator,
                    prefsRepo = container.preferencesRepository
                )
            }
        }
    )

    val updateAdminVm: UpdateAndAdminViewModel = viewModel(
        key = "update_admin_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                UpdateAndAdminViewModel(
                    repository = container.repository
                )
            }
        }
    )

    val activityVm: ActivityEngineViewModel = viewModel(
        key = "activity_engine_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                ActivityEngineViewModel(
                    activityRepository = container.activityRepository,
                    activityEngine = container.activityEngine,
                    localOrchestrator = container.localIntelligenceOrchestrator,
                    audioRecorder = container.audioRecorderManager,
                    userId = userId,
                    hybridCoordinator = container.hybridAiCoordinator,
                    learningProgress = container.learningProgressManager
                )
            }
        }
    )

    val learningPathsVm: LearningPathsViewModel = viewModel(
        key = "learning_paths_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                LearningPathsViewModel(
                    persistenceRepository = container.learningPathPersistenceRepository,
                    speakCraftRepository = container.repository,
                    activityRepository = container.activityRepository,
                    learningProgressManager = container.learningProgressManager,
                    audioManager = container.audioRecorderManager,
                    liveTranscriptionManager = container.liveTranscriptionManager,
                    ttsHelper = ttsHelper,
                    aiSettingsRepository = container.aiSettingsRepository,
                    dateKeyProvider = container.dateKeys
                )
            }
        }
    )
    val coachPracticeVm: CoachPracticeViewModel = viewModel(
        key = "coach_practice_$userId",
        factory = viewModelFactory {
            initializer {
                val consentStore = container.onlineTranscriptionConsent
                CoachPracticeViewModel(
                    loadCatalog = { container.practiceCatalog },
                    engine = container.phonePracticeEngine,
                    consent = object : OnlineConsent {
                        override val granted: Boolean get() = consentStore.hasConsented
                        override fun set(granted: Boolean) {
                            if (granted) consentStore.grant() else consentStore.revoke()
                        }
                    },
                    // Each finished attempt -> daily record + techniques pillar + technique daily task
                    onAttemptCompleted = { completion ->
                        container.learningProgressManager.recordPracticeCompletion(completion)
                    }
                )
            }
        }
    )

    val profileState by mainVm.userProfile.collectAsStateWithLifecycle()
    val isAdmin by mainVm.isAdmin.collectAsStateWithLifecycle()
    val actionMessage by mainVm.actionMessage.collectAsStateWithLifecycle()
    val updateStatusMessage by updateAdminVm.statusMessage.collectAsStateWithLifecycle()
    val activityStatusBanner by activityVm.statusBanner.collectAsStateWithLifecycle()

    val learningProgressManager = container.learningProgressManager
    val notificationManager = container.notificationManager
    val unreadNotificationsCount by remember(notificationManager) {
        notificationManager.state.map { it.unreadCount }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = 0)

    val aiSettingsRepo = container.aiSettingsRepository
    val capabilityManager = container.intelligenceCapabilityManager
    val reducedMotion = rememberReducedMotionEnabled()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainVm.clearActionMessage()
        }
    }
    LaunchedEffect(updateStatusMessage) {
        val msg = updateStatusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            updateAdminVm.clearStatusMessage()
        }
    }
    LaunchedEffect(activityStatusBanner) {
        val msg = activityStatusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            activityVm.clearStatusBanner()
        }
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("profile_loading_indicator"),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Preparing your speaking studio...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Could not load profile: ${pState.message}")
            }
        }
        is UiState.Success -> {
            val profile = pState.data
            if (profile == null || !profile.onboardingCompleted) {
                OnboardingAssessmentScreen(
                    defaultDisplayName = userDisplayName,
                    onComplete = { name, level, goal, mins, flu, pron, voc, gram, art, list, conf ->
                        mainVm.completeOnboardingAssessment(name, level, goal, mins, flu, pron, voc, gram, art, list, conf)
                    }
                )
            } else {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentDest = backStack?.destination
                val currentRoute = currentDest?.route ?: "dashboard"
                val currentTabGraph = currentDest?.hierarchy
                    ?.firstOrNull { it.route?.startsWith("tab_") == true }
                    ?.route
                    ?: tabGraphForRoute(currentRoute)
                val currentSlugArg = backStack?.arguments?.getString("slug")
                val currentLessonIdArg = backStack?.arguments?.getString("lessonId")
                val currentActivityIdArg = backStack?.arguments?.getString("activityId")
                val currentProgramIdArg = backStack?.arguments?.getString("programId")
                val currentDayNumArg = backStack?.arguments?.getInt("dayNumber") ?: 0
                val currentPathModArg = backStack?.arguments?.getString("moduleId")

                val topBarSharedLessons = if (currentRoute.startsWith("lesson_detail")) {
                    mainVm.publishedSharedLessons.collectAsStateWithLifecycle().value
                } else {
                    emptyList()
                }
                val topBarTitle = when {
                    currentRoute.startsWith("module_lessons") && !currentSlugArg.isNullOrBlank() ->
                        CurriculumCatalog.contentModules.firstOrNull { it.slug == currentSlugArg }?.title ?: "Module Lessons"
                    currentRoute.startsWith("lesson_detail") && !currentLessonIdArg.isNullOrBlank() ->
                        CurriculumCatalog.lessons.firstOrNull { it.id == currentLessonIdArg }?.title
                            ?: topBarSharedLessons.firstOrNull { it.id == currentLessonIdArg }?.title
                            ?: "Lesson Detail"
                    currentRoute.startsWith("learning_path_detail") && !currentProgramIdArg.isNullOrBlank() ->
                        LearningPathCurriculumRegistry.getProgram(currentProgramIdArg)?.title ?: "Program Syllabus"
                    currentRoute.startsWith("learning_path_lesson") && !currentProgramIdArg.isNullOrBlank() && currentDayNumArg > 0 ->
                        LearningPathCurriculumRegistry.getLesson(currentProgramIdArg, currentDayNumArg)?.let {
                            "Day ${it.dayNumber}: ${it.title}"
                        } ?: "Daily Lesson"
                    currentRoute.startsWith("learning_path_assessment") && !currentProgramIdArg.isNullOrBlank() && !currentPathModArg.isNullOrBlank() ->
                        LearningPathCurriculumRegistry.getModule(currentProgramIdArg, currentPathModArg)?.assessment?.title
                            ?: "Module Assessment"
                    currentRoute.startsWith("activity_library") -> "Activity Engine (150)"
                    currentRoute.startsWith("activity/") && !currentActivityIdArg.isNullOrBlank() ->
                        ActivityCatalogue.getActivityById(currentActivityIdArg)?.title ?: "Activity Studio"
                    else -> routeTitles[currentRoute] ?: "SpeakCraft"
                }

                var showFeatureGuideDialog by remember { mutableStateOf(false) }
                var showNotificationDrawer by remember { mutableStateOf(false) }
                var manualTourSteps by remember { mutableStateOf<List<TutorialOverlayStep>?>(null) }
                var manualTourTitle by remember { mutableStateOf("Core Features Guided Tour") }
                var pendingGeminiDraft by remember { mutableStateOf<String?>(null) }
                var pendingStudioGeminiFeedback by remember { mutableStateOf<String?>(null) }

                SyncEffectsHost(
                    userId = userId,
                    profile = profile,
                    mainVm = mainVm,
                    learningPathsVm = learningPathsVm,
                    learningProgressManager = learningProgressManager,
                    notificationManager = notificationManager
                )

                val navigateTo: (String) -> Unit = remember(navController) {
                    { route -> navigateAcrossTabs(navController, route) }
                }

                LaunchedEffect(inboundGeminiLink) {
                    val link = inboundGeminiLink ?: return@LaunchedEffect
                    val targetRoute = link.targetRoute
                    if (!targetRoute.isNullOrBlank()) {
                        navigateTo(targetRoute)
                    }
                    if (!link.sharedText.isNullOrBlank()) {
                        if (targetRoute == "speaking_lab") {
                            pendingStudioGeminiFeedback = link.sharedText
                        } else {
                            pendingGeminiDraft = link.sharedText
                        }
                    }
                    onConsumeInboundGeminiLink()
                }

                if (showFeatureGuideDialog) {
                    FeatureTutorialDialog(
                        initialTutorial = FeatureTutorialRegistry.forRoute(currentRoute),
                        onDismiss = { showFeatureGuideDialog = false },
                        onNavigateToFeature = { route -> navigateTo(route) },
                        onStartInteractiveTour = {
                            manualTourTitle = "Core Features Guided Tour"
                            manualTourSteps = TutorialOverlayCatalog.coreWelcomeTourSteps()
                        }
                    )
                }

                val showCoreTour = prefsLoaded &&
                    tutorialStateLoaded &&
                    !showFeatureGuideDialog &&
                    manualTourSteps == null &&
                    currentRoute == "dashboard" &&
                    tutorialState.shouldShowTutorial(TutorialOverlayCatalog.CORE_TOUR_KEY)

                val showModuleSpotlight = prefsLoaded &&
                    tutorialStateLoaded &&
                    !showFeatureGuideDialog &&
                    manualTourSteps == null &&
                    currentRoute in TutorialOverlayCatalog.coreModuleFirstUseRoutes &&
                    tutorialState.shouldShowTutorial(currentRoute)

                val activeOverlaySteps: List<TutorialOverlayStep> = when {
                    manualTourSteps != null -> manualTourSteps.orEmpty()
                    showCoreTour -> TutorialOverlayCatalog.coreWelcomeTourSteps()
                    showModuleSpotlight -> listOf(TutorialOverlayCatalog.singleModuleStep(currentRoute))
                    else -> emptyList()
                }
                val activeOverlayVisible = activeOverlaySteps.isNotEmpty()
                val activeOverlayTitle = when {
                    manualTourSteps != null -> manualTourTitle
                    showCoreTour -> "Welcome to SpeakCraft"
                    else -> "Module Quick Guide"
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useSidebar = maxWidth >= 840.dp
                    val useRail = maxWidth >= 600.dp && !useSidebar
                    val isChromeRoot = if (useSidebar) {
                        currentRoute in sidebarPrimaryRoutes
                    } else {
                        currentRoute in compactPrimaryRoutes
                    }
                    val userInitial = profile.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                    val ext = MaterialTheme.speakCraftColors

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useSidebar) {
                            SpeakCraftIndigoSidebar(
                                currentTabGraph = currentTabGraph,
                                onNavigate = navigateTo
                            )
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.background,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                Column {
                                    TopAppBar(
                                        windowInsets = WindowInsets.statusBars,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            titleContentColor = MaterialTheme.colorScheme.onBackground
                                        ),
                                        title = {
                                            if (currentRoute == "dashboard" && !useSidebar) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Filled.RecordVoiceOver,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(19.dp)
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text(
                                                            topBarTitle,
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Communication Improvement",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else if (currentRoute != "dashboard") {
                                                Text(
                                                    text = topBarTitle,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    maxLines = 1
                                                )
                                            }
                                        },
                                        navigationIcon = {
                                            if (!isChromeRoot) {
                                                IconButton(
                                                    onClick = {
                                                        if (!navController.navigateUp()) {
                                                            navigateTo("dashboard")
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                                }
                                            }
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = { showNotificationDrawer = !showNotificationDrawer },
                                                modifier = Modifier
                                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("notifications_bell_button")
                                            ) {
                                                if (unreadNotificationsCount > 0) {
                                                    BadgedBox(
                                                        badge = {
                                                            Badge {
                                                                Text(
                                                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString()
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Notifications,
                                                            contentDescription = "$unreadNotificationsCount unread notifications and alerts"
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        Icons.Default.Notifications,
                                                        contentDescription = "Notifications and alerts"
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { showFeatureGuideDialog = true },
                                                modifier = Modifier
                                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("feature_tutorial_topbar_button")
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.HelpOutline,
                                                    contentDescription = "Feature guide & what improves",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isAdmin) {
                                                IconButton(onClick = { navigateTo("admin_console") }) {
                                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console")
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 10.dp)
                                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                    .clip(CircleShape)
                                                    .clickable { navigateTo("profile") }
                                                    .testTag("user_profile_topbar_button"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = userInitial,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                }
                            },
                            bottomBar = {
                                if (!useRail && !useSidebar) {
                                    Column {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 2.dp,
                                            windowInsets = WindowInsets.navigationBars
                                        ) {
                                            primaryNavItems.forEach { item ->
                                                val itemGraph = tabGraphForRoute(item.route)
                                                val isSelected = currentTabGraph == itemGraph
                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = { navigateTo(item.route) },
                                                    icon = {
                                                        Icon(
                                                            if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                            contentDescription = item.label
                                                        )
                                                    },
                                                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                if (useRail) {
                                    NavigationRail(
                                        containerColor = ext.sidebarTop,
                                        header = {
                                            Spacer(Modifier.size(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = ext.sidebarActiveStart,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.RecordVoiceOver,
                                                        contentDescription = "SpeakCraft Studio",
                                                        tint = ext.onHeroTitle,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.size(8.dp))
                                        }
                                    ) {
                                        primaryNavItems.forEach { item ->
                                            val itemGraph = tabGraphForRoute(item.route)
                                            val isSelected = currentTabGraph == itemGraph
                                            NavigationRailItem(
                                                selected = isSelected,
                                                onClick = { navigateTo(item.route) },
                                                icon = {
                                                    Icon(
                                                        if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = item.label
                                                    )
                                                },
                                                label = { Text(item.label) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = ext.onHeroTitle,
                                                    selectedTextColor = ext.onHeroTitle,
                                                    unselectedIconColor = ext.sidebarTextIdle,
                                                    unselectedTextColor = ext.sidebarTextIdle,
                                                    indicatorColor = ext.sidebarActiveStart
                                                )
                                            )
                                        }
                                    }
                                }
                            NavHost(
                                navController = navController,
                                startDestination = TabGraphRoutes.DASHBOARD,
                                enterTransition = {
                                    if (initialState.destination.parent?.route != targetState.destination.parent?.route || reducedMotion) {
                                        EnterTransition.None
                                    } else {
                                        fadeIn(tween(110))
                                    }
                                },
                                exitTransition = {
                                    if (initialState.destination.parent?.route != targetState.destination.parent?.route || reducedMotion) {
                                        ExitTransition.None
                                    } else {
                                        fadeOut(tween(90))
                                    }
                                },
                                popEnterTransition = {
                                    if (initialState.destination.parent?.route != targetState.destination.parent?.route || reducedMotion) {
                                        EnterTransition.None
                                    } else {
                                        fadeIn(tween(110))
                                    }
                                },
                                popExitTransition = {
                                    if (initialState.destination.parent?.route != targetState.destination.parent?.route || reducedMotion) {
                                        ExitTransition.None
                                    } else {
                                        fadeOut(tween(90))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                navigation(startDestination = "dashboard", route = TabGraphRoutes.DASHBOARD) {
                                    composable("dashboard") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val vocabState by mainVm.vocabularyEntries.collectAsStateWithLifecycle()
                                        val dailyState by mainVm.dailyActivities.collectAsStateWithLifecycle()
                                        val learningProgressState by mainVm.learningProgress.collectAsStateWithLifecycle()
                                        val tasksState by mainVm.realWorldTasks.collectAsStateWithLifecycle()
                                        val recommendedNextAction by mainVm.recommendedNextAction.collectAsStateWithLifecycle()
                                        val learningSnapshot by learningProgressManager.state.collectAsStateWithLifecycle()
                                        val activePathBanner by remember(learningPathsVm) {
                                            learningPathsVm.uiState
                                                .map { state ->
                                                    state.programs.firstNotNullOfOrNull { prog ->
                                                        val st = state.programStats[prog.programId]
                                                        if (st != null && st.enrolled && !st.paused && !st.programCompleted) {
                                                            ActiveLearningPathBannerUi(
                                                                programId = prog.programId,
                                                                programTitle = prog.title,
                                                                completedDaysCount = st.completedDaysCount,
                                                                totalDays = st.totalDays,
                                                                nextDayNumber = st.nextDayNumber,
                                                                nextLessonTitle = st.nextLesson?.title ?: "Continue Module",
                                                                completionFraction = st.completionFraction
                                                            )
                                                        } else null
                                                    }
                                                }
                                                .distinctUntilChanged()
                                        }.collectAsStateWithLifecycle(initialValue = null)

                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        val vocabEntries = (vocabState as? UiState.Success)?.data.orEmpty()
                                        val dailyList = (dailyState as? UiState.Success)?.data.orEmpty()
                                        val learningProgress = (learningProgressState as? UiState.Success)?.data.orEmpty()
                                        val realWorldList = (tasksState as? UiState.Success)?.data.orEmpty()
                                        val lpRecordingsCount by remember(learningPathsVm) {
                                            learningPathsVm.uiState
                                                .map { state ->
                                                    state.userState.enrollments.values.sumOf { enr ->
                                                        enr.dayRecords.values.count { !it.lastAudioPath.isNullOrBlank() }
                                                    }
                                                }
                                                .distinctUntilChanged()
                                        }.collectAsStateWithLifecycle(initialValue = 0)

                                        DashboardScreen(
                                            profile = profile,
                                            sessions = sessions,
                                            vocabulary = vocabEntries,
                                            dailyActivities = dailyList,
                                            learningProgress = learningProgress,
                                            realWorldTasks = realWorldList,
                                            recommendedNextAction = recommendedNextAction,
                                            isAdmin = isAdmin,
                                            todayDateKey = mainVm.todayDateKey,
                                            onNavigate = navigateTo,
                                            learningSnapshot = learningSnapshot,
                                            onCompleteDailyTask = { taskId ->
                                                learningProgressManager.completeDailyTask(taskId)
                                            },
                                            onOpenProfile = { navigateTo("profile") },
                                            onQuickAskMentor = { promptText ->
                                                val feedbackList = (mainVm.speechFeedback.value as? UiState.Success)?.data.orEmpty()
                                                coachVm.askMentor(
                                                    profile = profile,
                                                    sessions = sessions,
                                                    feedbackList = feedbackList,
                                                    question = promptText
                                                )
                                                navigateTo("ai_mentor")
                                            },
                                            activePathSummary = activePathBanner,
                                            extraRecordingsCount = lpRecordingsCount
                                        )
                                    }
                                }

                                navigation(startDestination = "daily_practice", route = TabGraphRoutes.DAILY_PRACTICE) {
                                    composable("daily_practice") {
                                        val dailyState by mainVm.dailyActivities.collectAsStateWithLifecycle()
                                        val dailyPackage by mainVm.dailyCurriculumPackage.collectAsStateWithLifecycle()
                                        val learningSnapshot by learningProgressManager.state.collectAsStateWithLifecycle()
                                        val dailyList = (dailyState as? UiState.Success)?.data.orEmpty()
                                        val todayActivity = dailyList.firstOrNull { it.dateKey == mainVm.todayDateKey }
                                        val userLevel = remember(profile.communicationLevel) {
                                            LearningLevel.fromLabel(profile.communicationLevel)
                                        }
                                        DailyPracticeScreen(
                                            todayDateKey = mainVm.todayDateKey,
                                            todayActivity = todayActivity,
                                            todaysWords = if (dailyPackage.dailyWords.isNotEmpty()) dailyPackage.dailyWords else mainVm.dailyVocabularyBatch,
                                            userLevel = userLevel,
                                            dailyPackage = dailyPackage,
                                            onStartActivity = { activityId -> navigateTo("activity/$activityId") },
                                            onSpeakText = { text -> ttsHelper.speak(text, prefs.ttsRate) },
                                            onToggleStep = { id, refl, conf -> mainVm.toggleDailyStep(id, todayActivity, refl, conf) },
                                            onSaveReflection = { refl, conf -> mainVm.saveDailyReflection(todayActivity, refl, conf) },
                                            onNavigate = navigateTo,
                                            dailyTasks = learningSnapshot.dailyPlan
                                                ?.takeIf { it.dateKey == dailyPackage.dateKey }
                                                ?.tasks,
                                            setNumberToday = learningSnapshot.dailyRotation
                                                .takeIf { it.dateKey == dailyPackage.dateKey }
                                                ?.let { it.setIndexToday + 1 } ?: 1,
                                            dailyRecords = learningSnapshot.dailyRecords,
                                            onCompleteTask = { taskId -> learningProgressManager.completeDailyTask(taskId) }
                                        )
                                    }
                                }

                                navigation(startDestination = "learning_paths", route = TabGraphRoutes.LEARNING_PATHS) {
                                    composable("learning_paths") {
                                        val learningPathsUiState by learningPathsVm.uiState.collectAsStateWithLifecycle()
                                        LearningPathsOverviewScreen(
                                            uiState = learningPathsUiState,
                                            onSearchChange = { learningPathsVm.setSearchQuery(it) },
                                            onCategoryFilterChange = { learningPathsVm.setCategoryFilter(it) },
                                            onPreviewProgram = { learningPathsVm.openPreview(it) },
                                            onEnrollProgram = { learningPathsVm.enrollInProgram(it) },
                                            onTogglePauseProgram = { learningPathsVm.togglePauseProgram(it) },
                                            onUnenrollProgram = { learningPathsVm.unenrollProgram(it) },
                                            onOpenProgramDetail = { pid -> navigateTo("learning_path_detail/$pid") },
                                            onOpenLesson = { pid, day -> navigateTo("learning_path_lesson/$pid/$day") },
                                            onDismissBanner = { learningPathsVm.dismissBanner() }
                                        )
                                    }
                                    composable(
                                        route = "learning_path_detail/{programId}",
                                        arguments = listOf(navArgument("programId") { type = NavType.StringType })
                                    ) { entry ->
                                        val programId = entry.arguments?.getString("programId").orEmpty()
                                        val learningPathsUiState by learningPathsVm.uiState.collectAsStateWithLifecycle()
                                        LearningPathDetailScreen(
                                            programId = programId,
                                            uiState = learningPathsUiState,
                                            onBack = { navController.popBackStack() },
                                            onEnroll = { learningPathsVm.enrollInProgram(it) },
                                            onTogglePause = { learningPathsVm.togglePauseProgram(it) },
                                            onOpenLesson = { pid, day -> navigateTo("learning_path_lesson/$pid/$day") },
                                            onToggleDayCompleteQuick = { pid, day -> learningPathsVm.toggleDayCompletionQuick(pid, day) },
                                            onOpenAssessment = { pid, modId -> navigateTo("learning_path_assessment/$pid/$modId") },
                                            onOpenProgram = { targetPid -> navigateTo("learning_path_detail/$targetPid") }
                                        )
                                    }
                                    composable(
                                        route = "learning_path_lesson/{programId}/{dayNumber}",
                                        arguments = listOf(
                                            navArgument("programId") { type = NavType.StringType },
                                            navArgument("dayNumber") { type = NavType.IntType }
                                        )
                                    ) { entry ->
                                        val programId = entry.arguments?.getString("programId").orEmpty()
                                        val dayNumber = entry.arguments?.getInt("dayNumber") ?: 1
                                        val learningPathsUiState by learningPathsVm.uiState.collectAsStateWithLifecycle()
                                        LearningPathLessonScreen(
                                            programId = programId,
                                            dayNumber = dayNumber,
                                            uiState = learningPathsUiState,
                                            onPrepareLesson = { pid, day -> learningPathsVm.prepareLessonSession(pid, day) },
                                            onBackToSyllabus = { navController.popBackStack() },
                                            onNavigateDay = { targetDay ->
                                                navController.navigate("learning_path_lesson/$programId/$targetDay") {
                                                    popUpTo("learning_path_lesson/{programId}/{dayNumber}") { inclusive = true }
                                                }
                                            },
                                            onOpenAssessment = { pid, modId -> navigateTo("learning_path_assessment/$pid/$modId") },
                                            onOpenActivityStudio = { activityId -> navigateTo("activity/$activityId") },
                                            onOpenLabRoute = { labRoute -> navigateTo(labRoute) },
                                            onUpdateDraftText = { learningPathsVm.updateDraftResponseText(it) },
                                            onSelectQuizOption = { qId, opt -> learningPathsVm.selectQuizOption(qId, opt) },
                                            onToggleChecklistItem = { learningPathsVm.toggleChecklistItem(it) },
                                            onSetConfidence = { learningPathsVm.setConfidenceRating(it) },
                                            onUpdateReflectionNote = { learningPathsVm.updateReflectionNote(it) },
                                            onSaveReflectionNote = { learningPathsVm.saveReflectionNoteNow(programId, dayNumber) },
                                            onPlayModelTts = { learningPathsVm.playModelScript(it) },
                                            onStopModelTts = { learningPathsVm.stopModelAudio() },
                                            onStartOrResumeRecording = { learningPathsVm.startOrResumeLessonRecording() },
                                            onPauseRecording = { learningPathsVm.pauseLessonRecording() },
                                            onStopRecording = { learningPathsVm.stopLessonRecording() },
                                            onTogglePlayback = { learningPathsVm.toggleRecordedPlayback() },
                                            onSubmitForEvaluation = { learningPathsVm.submitAndEvaluateLesson(programId, dayNumber) }
                                        )
                                    }
                                    composable(
                                        route = "learning_path_assessment/{programId}/{moduleId}",
                                        arguments = listOf(
                                            navArgument("programId") { type = NavType.StringType },
                                            navArgument("moduleId") { type = NavType.StringType }
                                        )
                                    ) { entry ->
                                        val programId = entry.arguments?.getString("programId").orEmpty()
                                        val moduleId = entry.arguments?.getString("moduleId").orEmpty()
                                        val learningPathsUiState by learningPathsVm.uiState.collectAsStateWithLifecycle()
                                        LearningPathAssessmentScreen(
                                            programId = programId,
                                            moduleId = moduleId,
                                            uiState = learningPathsUiState,
                                            onPrepareAssessment = { pid, mId -> learningPathsVm.prepareModuleAssessment(pid, mId) },
                                            onSelectScenarioOption = { qId, idx -> learningPathsVm.selectAssessmentOption(qId, idx) },
                                            onUpdatePracticalText = { learningPathsVm.updateAssessmentPracticalText(it) },
                                            onSetConfidence = { learningPathsVm.setAssessmentConfidence(it) },
                                            onSubmitAssessment = { learningPathsVm.submitModuleAssessment(programId, moduleId) },
                                            onBackToSyllabus = { navController.popBackStack() }
                                        )
                                    }
                                    composable(
                                        route = "module_lessons/{slug}",
                                        arguments = listOf(navArgument("slug") { type = NavType.StringType })
                                    ) { entry ->
                                        val slug = entry.arguments?.getString("slug").orEmpty()
                                        val sharedLessons by mainVm.publishedSharedLessons.collectAsStateWithLifecycle()
                                        val learningProgressState by mainVm.learningProgress.collectAsStateWithLifecycle()
                                        val learningProgress = (learningProgressState as? UiState.Success)?.data.orEmpty()
                                        ModuleLessonsScreen(
                                            moduleSlug = slug,
                                            sharedLessons = sharedLessons,
                                            learningProgress = learningProgress,
                                            onOpenLesson = { lessonId -> navigateTo("lesson_detail/$lessonId") }
                                        )
                                    }
                                    composable(
                                        route = "lesson_detail/{lessonId}",
                                        arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                                    ) { entry ->
                                        val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                                        val sharedLessons by mainVm.publishedSharedLessons.collectAsStateWithLifecycle()
                                        val learningProgressState by mainVm.learningProgress.collectAsStateWithLifecycle()
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val learningProgress = (learningProgressState as? UiState.Success)?.data.orEmpty()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        LessonDetailScreen(
                                            lessonId = lessonId,
                                            sharedLessons = sharedLessons,
                                            learningProgress = learningProgress,
                                            speakingVm = speakingVm,
                                            pastSessions = sessions,
                                            onToggleCompleted = { id, slug, completed, notes ->
                                                mainVm.toggleLessonCompleted(id, slug, completed, notes)
                                            },
                                            onNavigateToLesson = { nextId -> navigateTo("lesson_detail/$nextId") }
                                        )
                                    }
                                }

                                navigation(startDestination = "vocabulary", route = TabGraphRoutes.VOCABULARY) {
                                    composable("vocabulary") {
                                        val vocabState by mainVm.vocabularyEntries.collectAsStateWithLifecycle()
                                        val vocabEvalState by mainVm.vocabEvalState.collectAsStateWithLifecycle()
                                        val vocabBuilderState by mainVm.vocabBuilderState.collectAsStateWithLifecycle()
                                        val vocabEntries = (vocabState as? UiState.Success)?.data.orEmpty()
                                        val entriesById = remember(vocabEntries) { vocabEntries.associateBy { it.id } }
                                        VocabularyScreen(
                                            entries = vocabEntries,
                                            todayEpochDay = mainVm.todayEpochDay,
                                            vocabEvalState = vocabEvalState,
                                            onSpeakWord = { txt -> ttsHelper.speak(txt, prefs.ttsRate) },
                                            onReviewCard = { seed, existing, ok -> mainVm.reviewVocabularyCard(seed, existing, ok) },
                                            onEvaluateUsage = { seed, existing, sent -> mainVm.evaluateVocabUsage(seed, existing, sent) },
                                            onSaveCustomWord = { w, m, l, e, n -> mainVm.saveCustomVocabularyWord(w, m, l, e, n) },
                                            onClearEval = { mainVm.clearVocabEval() },
                                            userLevel = vocabBuilderState.userLevel,
                                            dailyVocabWords = vocabBuilderState.dailyWords,
                                            completedDailyWordIds = vocabBuilderState.completedWordIds,
                                            dailyWordScores = vocabBuilderState.wordScores,
                                            learnedWordIds = vocabBuilderState.learnedWordIds,
                                            weeklyEligibility = vocabBuilderState.weeklyEligibility,
                                            learnedWordsForAssessment = vocabBuilderState.learnedWordsForAssessment,
                                            ttsHelper = ttsHelper,
                                            onAssessSpokenWord = { seed, existing, transcript, alts, conf ->
                                                mainVm.assessDailyWordSpeaking(seed, existing, transcript, alts, conf)
                                            },
                                            onCompleteWeeklyAssessment = { record, perWordPassed ->
                                                mainVm.submitWeeklyVocabAssessment(record, perWordPassed, entriesById)
                                            }
                                        )
                                    }
                                }

                                navigation(startDestination = "coach_practice", route = TabGraphRoutes.COACH_PRACTICE) {
                                    composable("coach_practice") {
                                        CoachPracticeRoute(
                                            vm = coachPracticeVm,
                                            cameraHost = container.phonePracticeEngine,
                                            onSpeak = { txt -> ttsHelper.speak(txt, prefs.ttsRate) }
                                        )
                                    }
                                }

                                navigation(startDestination = "speaking_lab", route = TabGraphRoutes.SPEAKING_LAB) {
                                    composable("speaking_lab") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        SpeakingLabScreen(
                                            speakingVm = speakingVm,
                                            pastSessions = sessions,
                                            onDeleteSession = { sess -> mainVm.deleteSpeakingSession(sess) },
                                            onOpenAiSettings = { navigateTo("settings") },
                                            incomingGeminiFeedback = pendingStudioGeminiFeedback,
                                            onConsumeIncomingGeminiFeedback = { pendingStudioGeminiFeedback = null },
                                            onOpenProfileRecordings = { navigateTo("profile_recordings") }
                                        )
                                    }
                                    composable("challenges") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sharedChallenges by mainVm.sharedChallenges.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        ChallengesScreen(
                                            speakingVm = speakingVm,
                                            sharedChallenges = sharedChallenges,
                                            pastSessions = sessions,
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("articulation_lab") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        PronunciationLabScreen(
                                            speakingVm = speakingVm,
                                            ttsHelper = ttsHelper,
                                            pastSessions = sessions,
                                            initialFocus = "articulation",
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("pronunciation") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        PronunciationLabScreen(
                                            speakingVm = speakingVm,
                                            ttsHelper = ttsHelper,
                                            pastSessions = sessions,
                                            initialFocus = "pronunciation",
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("voice_training") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        VoiceTrainingLabScreen(speakingVm = speakingVm, ttsHelper = ttsHelper, pastSessions = sessions)
                                    }
                                    composable("conversation") {
                                        ConversationSimulatorScreen(
                                            coachVm = coachVm,
                                            ttsHelper = ttsHelper,
                                            userLevel = profile.communicationLevel,
                                            ttsRate = prefs.ttsRate
                                        )
                                    }
                                    composable("listening_lab") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        ListeningShadowingScreen(
                                            speakingVm = speakingVm,
                                            ttsHelper = ttsHelper,
                                            pastSessions = sessions,
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("grammar_speaking") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        GrammarSpeakingScreen(
                                            speakingVm = speakingVm,
                                            pastSessions = sessions,
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("public_speaking") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        PublicSpeakingScreen(
                                            speakingVm = speakingVm,
                                            pastSessions = sessions,
                                            onOpenAiSettings = { navigateTo("settings") }
                                        )
                                    }
                                    composable("real_world_tasks") {
                                        val tasksState by mainVm.realWorldTasks.collectAsStateWithLifecycle()
                                        val realWorldList = (tasksState as? UiState.Success)?.data.orEmpty()
                                        RealWorldTasksScreen(
                                            records = realWorldList,
                                            onSaveTask = { id, title, done, refl -> mainVm.toggleRealWorldTask(id, title, done, refl) }
                                        )
                                    }
                                }

                                navigation(startDestination = "analytics", route = TabGraphRoutes.ANALYTICS) {
                                    composable("analytics") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val dailyState by mainVm.dailyActivities.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        val dailyList = (dailyState as? UiState.Success)?.data.orEmpty()
                                        AnalyticsScreen(
                                            profile = profile,
                                            sessions = sessions,
                                            dailyActivities = dailyList,
                                            onNavigateToMentor = { navigateTo("ai_mentor") }
                                        )
                                    }
                                }

                                navigation(startDestination = "ai_mentor", route = TabGraphRoutes.AI_MENTOR) {
                                    composable("ai_mentor") {
                                        val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
                                        val feedbackState by mainVm.speechFeedback.collectAsStateWithLifecycle()
                                        val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                                        val feedbackList = (feedbackState as? UiState.Success)?.data.orEmpty()
                                        AiMentorScreen(
                                            coachVm = coachVm,
                                            profile = profile,
                                            sessions = sessions,
                                            feedbackList = feedbackList,
                                            ttsHelper = ttsHelper,
                                            initialDraftFromGemini = pendingGeminiDraft,
                                            onConsumeInitialDraft = { pendingGeminiDraft = null }
                                        )
                                    }
                                }

                                navigation(startDestination = "settings", route = TabGraphRoutes.SETTINGS) {
                                    composable("settings") {
                                        val aiSettings by aiSettingsRepo.aiSettingsFlow.collectAsStateWithLifecycle(initialValue = AISettings())
                                        val capabilityMatrix by remember(capabilityManager, aiSettingsRepo) {
                                            capabilityManager.observeMatrix(aiSettingsRepo)
                                        }.collectAsStateWithLifecycle(initialValue = capabilityManager.computeMatrix(AISettings()))
                                        SettingsScreen(
                                            prefs = prefs,
                                            userEmail = userEmail,
                                            isAdmin = isAdmin,
                                            onSetTheme = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                                            onSetTtsRate = { rate -> scope.launch { prefsRepo.setTtsRate(rate) } },
                                            onOpenAdminConsole = { navigateTo("admin_console") },
                                            onClearHistory = { mainVm.clearSpeakingHistory() },
                                            onSignOut = { signOut(credentialManager, scope) },
                                            firstUseTutorialsEnabled = tutorialState.firstUseTutorialsEnabled,
                                            onToggleFirstUseTutorials = { enabled ->
                                                scope.launch { tutorialStateManager.setFirstUseTutorialsEnabled(userId, enabled) }
                                            },
                                            onReplayTutorials = {
                                                scope.launch {
                                                    tutorialStateManager.resetUserTutorials(userId)
                                                    navigateTo("dashboard")
                                                }
                                            },
                                            aiSettings = aiSettings,
                                            capabilityMatrix = capabilityMatrix,
                                            onSetLocalAiEnabled = { enabled ->
                                                scope.launch { aiSettingsRepo.setLocalAiEnabled(enabled) }
                                            },
                                            onSetLiveTranscriptionEnabled = { enabled ->
                                                scope.launch { aiSettingsRepo.setLiveTranscriptionEnabled(enabled) }
                                            },
                                            onSetLocalSpeechRecEnabled = { enabled ->
                                                scope.launch { aiSettingsRepo.setLocalSpeechRecognitionEnabled(enabled) }
                                            },
                                            onSetLocalAudioAnalysisEnabled = { enabled ->
                                                scope.launch { aiSettingsRepo.setLocalAudioAnalysisEnabled(enabled) }
                                            },
                                            onSetLocalVideoAnalysisEnabled = { enabled ->
                                                scope.launch { aiSettingsRepo.setLocalVideoAnalysisEnabled(enabled) }
                                            },
                                            onSetRecognitionLanguage = { lang ->
                                                scope.launch { aiSettingsRepo.setSelectedRecognitionLanguage(lang) }
                                            },
                                            onSetRecognitionEngine = { engine ->
                                                scope.launch { aiSettingsRepo.setSelectedRecognitionEngine(engine) }
                                            }
                                        )
                                    }
                                    composable("admin_console") {
                                        AdminDashboardScreen(
                                            vm = updateAdminVm,
                                            isAdmin = isAdmin,
                                            currentUserId = userId
                                        )
                                    }
                                }

                                navigation(startDestination = "profile", route = TabGraphRoutes.PROFILE) {
                                    composable("profile") {
                                        ProfileRouteHost(
                                            initialTab = 0,
                                            profile = profile,
                                            userEmail = userEmail,
                                            mainVm = mainVm,
                                            learningPathsVm = learningPathsVm,
                                            learningProgressManager = learningProgressManager,
                                            audioManager = container.audioRecorderManager,
                                            onSaveProfile = { name, level, goal, mins ->
                                                mainVm.updateUserProfile(name, level, goal, mins, profile)
                                            },
                                            onOpenNotifications = { showNotificationDrawer = true },
                                            onNavigate = navigateTo,
                                            onSignOut = { signOut(credentialManager, scope) }
                                        )
                                    }
                                    composable("profile_recordings") {
                                        ProfileRouteHost(
                                            initialTab = 1,
                                            profile = profile,
                                            userEmail = userEmail,
                                            mainVm = mainVm,
                                            learningPathsVm = learningPathsVm,
                                            learningProgressManager = learningProgressManager,
                                            audioManager = container.audioRecorderManager,
                                            onSaveProfile = { name, level, goal, mins ->
                                                mainVm.updateUserProfile(name, level, goal, mins, profile)
                                            },
                                            onOpenNotifications = { showNotificationDrawer = true },
                                            onNavigate = navigateTo,
                                            onSignOut = { signOut(credentialManager, scope) }
                                        )
                                    }
                                }

                                navigation(startDestination = "activity_library", route = TabGraphRoutes.ACTIVITIES) {
                                    composable("activity_library") {
                                        ActivityDiscoveryScreen(
                                            vm = activityVm,
                                            profile = profile,
                                            onOpenActivity = { id -> navigateTo("activity/$id") }
                                        )
                                    }
                                    composable(
                                        route = "activity_library/{moduleId}",
                                        arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
                                    ) { entry ->
                                        val modId = entry.arguments?.getString("moduleId")
                                        ActivityDiscoveryScreen(
                                            vm = activityVm,
                                            profile = profile,
                                            initialModuleId = modId,
                                            onOpenActivity = { id -> navigateTo("activity/$id") }
                                        )
                                    }
                                    composable(
                                        route = "activity/{activityId}",
                                        arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                                    ) { entry ->
                                        val actId = entry.arguments?.getString("activityId").orEmpty()
                                        ActivityScreen(
                                            activityId = actId,
                                            vm = activityVm,
                                            ttsHelper = ttsHelper,
                                            userLevel = profile.communicationLevel,
                                            onOpenNextActivity = { nextId -> navigateTo("activity/$nextId") },
                                            onBackToCatalogue = { navigateTo("activity_library") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }

                    TutorialOverlay(
                        visible = activeOverlayVisible,
                        steps = activeOverlaySteps,
                        tourTitle = activeOverlayTitle,
                        tourSubtitle = if (activeOverlaySteps.size > 1) {
                            "Core features walkthrough • highlight benefits & usage instructions"
                        } else {
                            "First visit spotlight • what this module improves & how to practice"
                        },
                        allowDisableFutureOverlays = true,
                        onDismiss = { dontShowAgain ->
                            if (manualTourSteps != null) {
                                manualTourSteps = null
                                if (dontShowAgain) {
                                    scope.launch { tutorialStateManager.setFirstUseTutorialsEnabled(userId, false) }
                                }
                            } else {
                                val completedKey = if (showCoreTour) {
                                    TutorialOverlayCatalog.CORE_TOUR_KEY
                                } else {
                                    currentRoute
                                }
                                scope.launch {
                                    tutorialStateManager.markFeatureCompleted(userId, completedKey)
                                    if (dontShowAgain) tutorialStateManager.setFirstUseTutorialsEnabled(userId, false)
                                }
                            }
                        },
                        onComplete = { dontShowAgain ->
                            if (manualTourSteps != null) {
                                manualTourSteps = null
                                if (dontShowAgain) {
                                    scope.launch { tutorialStateManager.setFirstUseTutorialsEnabled(userId, false) }
                                }
                            } else {
                                val completedKey = if (showCoreTour) {
                                    TutorialOverlayCatalog.CORE_TOUR_KEY
                                } else {
                                    currentRoute
                                }
                                scope.launch {
                                    tutorialStateManager.markFeatureCompleted(userId, completedKey)
                                    if (dontShowAgain) tutorialStateManager.setFirstUseTutorialsEnabled(userId, false)
                                }
                            }
                        },
                        onTryModule = { targetRoute ->
                            if (manualTourSteps != null) {
                                manualTourSteps = null
                            } else {
                                scope.launch {
                                    tutorialStateManager.markFeaturesCompleted(
                                        userId = userId,
                                        featureKeys = setOf(TutorialOverlayCatalog.CORE_TOUR_KEY, targetRoute)
                                    )
                                }
                            }
                            navigateTo(targetRoute)
                        }
                    )

                    NotificationDrawerHost(
                        visible = showNotificationDrawer,
                        notificationManager = notificationManager,
                        onDismiss = { showNotificationDrawer = false },
                        onOpenRoute = { route -> navigateTo(route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRouteHost(
    initialTab: Int,
    profile: UserProfile,
    userEmail: String,
    mainVm: MainCoachViewModel,
    learningPathsVm: LearningPathsViewModel,
    learningProgressManager: LearningProgressManager,
    audioManager: com.example.data.media.AudioRecorderManager,
    onSaveProfile: (displayName: String, level: String, primaryGoal: String, dailyGoalMinutes: Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
    val vocabState by mainVm.vocabularyEntries.collectAsStateWithLifecycle()
    val learningSnapshot by learningProgressManager.state.collectAsStateWithLifecycle()
    val learningPathsState by remember(learningPathsVm) {
        learningPathsVm.uiState.map { it.userState }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = com.example.data.learningpaths.LearningPathsUserState())
    val isPlaying by audioManager.isPlaying.collectAsStateWithLifecycle()
    val playingPath by audioManager.playingFilePath.collectAsStateWithLifecycle()

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val vocabEntries = (vocabState as? UiState.Success)?.data.orEmpty()

    UserProfileScreen(
        profile = profile,
        userEmail = userEmail,
        sessions = sessions,
        vocabulary = vocabEntries,
        learningSnapshot = learningSnapshot,
        onSaveProfile = onSaveProfile,
        onOpenNotifications = onOpenNotifications,
        onNavigate = onNavigate,
        onSignOut = onSignOut,
        initialTab = initialTab,
        learningPathsState = learningPathsState,
        playingMediaPath = if (isPlaying) playingPath else null,
        onTogglePlayMedia = { path ->
            if (isPlaying && playingPath == path) {
                audioManager.stopPlayback()
            } else {
                audioManager.playFile(path, muted = false)
            }
        },
        onStopPlayback = { audioManager.stopPlayback() },
        onDeleteRecordedSession = { rec ->
            when {
                rec.backedSpeakingSession != null -> mainVm.deleteSpeakingSession(rec.backedSpeakingSession)
                rec.backedPracticeRecordingId != null -> {
                    learningProgressManager.deletePracticeRecording(rec.backedPracticeRecordingId)
                    audioManager.deleteFile(rec.localMediaPath)
                }
                rec.backedLearningPathKey != null -> {
                    val (progId, day) = rec.backedLearningPathKey
                    learningPathsVm.clearDayAudioRecording(progId, day)
                }
                rec.backedActivityAttemptId != null -> {
                    learningProgressManager.deleteActivityAttemptMedia(rec.backedActivityAttemptId)
                    audioManager.deleteFile(rec.localMediaPath)
                }
            }
        }
    )
}

@Composable
private fun SyncEffectsHost(
    userId: String,
    profile: UserProfile,
    mainVm: MainCoachViewModel,
    learningPathsVm: LearningPathsViewModel,
    learningProgressManager: LearningProgressManager,
    notificationManager: NotificationManagerRepository
) {
    val enrollState by mainVm.enrollments.collectAsStateWithLifecycle()
    val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
    val activeAnnouncements by mainVm.activeAnnouncements.collectAsStateWithLifecycle()
    val learningNotifications by remember(learningProgressManager) {
        learningProgressManager.state.map { it.notifications }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val learningStreakDays by remember(learningProgressManager) {
        learningProgressManager.state.map { it.streak.currentStreakDays }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = 0)

    val enrollments = (enrollState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    var baselineSessionCount by remember(userId) { mutableStateOf<Int?>(null) }

    LaunchedEffect(userId, profile, enrollments) {
        learningPathsVm.syncAccountAndEnrollments(
            userId = userId,
            profile = profile,
            remoteEnrollments = enrollments
        )
    }

    LaunchedEffect(userId, profile, sessions.size, sessions.firstOrNull()?.id, learningStreakDays) {
        learningProgressManager.loadForUserAsync(
            userId = userId,
            profile = profile,
            recentSessions = sessions
        )
        notificationManager.loadForUserAsync(
            userId = userId,
            profile = profile,
            sessionsCount = sessions.size,
            streakDays = maxOf(
                profile.streakDays.toInt(),
                learningStreakDays
            )
        )
        val priorCount = baselineSessionCount
        if (priorCount != null && sessions.size > priorCount) {
            val newest = sessions.firstOrNull()
            if (newest != null && newest.evaluationStatus == "COMPLETED") {
                val dimValues = buildList {
                    val keys = newest.evaluatedDimensionKeys
                    if (keys.isEmpty() || "fluency" in keys) add(newest.fluencyScore)
                    if (keys.isEmpty() || "pronunciation" in keys) add(newest.pronunciationScore)
                    if (keys.isEmpty() || "vocabulary" in keys) add(newest.vocabularyScore)
                    if (keys.isEmpty() || "grammar" in keys) add(newest.grammarScore)
                    if ("delivery" in keys || "articulation" in keys) add(newest.articulationScore)
                }
                if (dimValues.isNotEmpty()) {
                    val avg = (dimValues.sum() / dimValues.size).toInt()
                    notificationManager.recordSessionCompletedAlert(
                        sessionId = newest.id.ifBlank { "${newest.promptTitle}_${sessions.size}" },
                        promptTitle = newest.promptTitle,
                        overallScore = avg
                    )
                }
            }
        }
        baselineSessionCount = sessions.size
    }

    LaunchedEffect(learningNotifications) {
        notificationManager.syncFromLearningNotifications(learningNotifications)
    }

    LaunchedEffect(activeAnnouncements) {
        activeAnnouncements.forEach { ann ->
            learningProgressManager.ingestAdminAnnouncement(ann)
            notificationManager.ingestAnnouncement(ann)
        }
    }
}

@Composable
private fun NotificationDrawerHost(
    visible: Boolean,
    notificationManager: NotificationManagerRepository,
    onDismiss: () -> Unit,
    onOpenRoute: (String) -> Unit
) {
    val notificationStoreState by notificationManager.state.collectAsStateWithLifecycle()
    NotificationDrawer(
        visible = visible,
        alerts = notificationStoreState.alerts,
        preferences = notificationStoreState.preferences,
        onDismiss = onDismiss,
        onMarkRead = { id -> notificationManager.markAsRead(id) },
        onMarkUnread = { id -> notificationManager.markAsUnread(id) },
        onTogglePin = { id -> notificationManager.togglePinned(id) },
        onMarkAllRead = { notificationManager.markAllAsRead() },
        onDismissAlert = { id -> notificationManager.dismissAlert(id) },
        onClearRead = { notificationManager.clearReadAlerts() },
        onUpdatePreferences = { updated -> notificationManager.updatePreferences(updated) },
        onOpenRoute = onOpenRoute
    )
}

private data class SidebarDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun SpeakCraftIndigoSidebar(
    currentTabGraph: String,
    onNavigate: (String) -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    val destinations = remember {
        listOf(
            SidebarDestination("dashboard", "Home", Icons.Default.Home),
            SidebarDestination("activity_library", "150 Activities", Icons.Default.School),
            SidebarDestination("daily_practice", "Practice", Icons.Default.TrackChanges),
            SidebarDestination("ai_mentor", "AI Mentor", Icons.Default.Psychology),
            SidebarDestination("learning_paths", "Learn", Icons.Default.MenuBook),
            SidebarDestination("coach_practice", "Coach Practice", Icons.Default.RecordVoiceOver),
            SidebarDestination("speaking_lab", "Record & Review", Icons.Default.Mic),
            SidebarDestination("vocabulary", "Tools", Icons.Default.GridView),
            SidebarDestination("analytics", "Progress", Icons.Default.Analytics),
            SidebarDestination("settings", "Settings", Icons.Default.Settings)
        )
    }

    Box(
        modifier = Modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ext.sidebarTop, ext.sidebarBottom)
                )
            )
            .testTag("speakcraft_indigo_sidebar")
    ) {
        // Decorative violet studio wave near bottom of sidebar
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(210.dp)
        ) {
            val wave1 = Path().apply {
                moveTo(0f, size.height * 0.25f)
                cubicTo(
                    size.width * 0.35f, size.height * 0.05f,
                    size.width * 0.7f, size.height * 0.45f,
                    size.width, size.height * 0.22f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = wave1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ext.sidebarWaveAccent.copy(alpha = 0.28f),
                        ext.sidebarActiveStart.copy(alpha = 0.08f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Brand Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.sidebarActiveStart.copy(alpha = 0.32f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = "SpeakCraft Logo",
                                tint = ext.onHeroTitle,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "SpeakCraft",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ext.onHeroTitle
                    )
                    Text(
                        text = "Communication Improvement",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.sidebarTextIdle.copy(alpha = 0.8f)
                    )
                }

                // Navigation Destinations
                destinations.forEach { dest ->
                    val isSelected = currentTabGraph == tabGraphForRoute(dest.route)
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ext.sidebarActiveStart, ext.sidebarActiveEnd)
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onNavigate(dest.route) }
                        .padding(horizontal = 14.dp, vertical = 11.dp)

                    Row(
                        modifier = itemModifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = if (isSelected) ext.onHeroTitle else ext.sidebarTextIdle,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) ext.onHeroTitle else ext.sidebarTextIdle,
                            maxLines = 1
                        )
                    }
                }
            }

            // Footer Studio Privacy & On-Device Badge (links to Settings)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ext.onHeroTitle.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ext.onHeroBadge,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "On-Device Studio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.onHeroTitle
                        )
                    }
                    Text(
                        text = "v${com.example.BuildConfig.VERSION_NAME} • Local speech analysis & privacy controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.sidebarTextIdle
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ext.onHeroTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onNavigate("settings") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Settings",
                                style = MaterialTheme.typography.labelLarge,
                                color = ext.sidebarTop
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ext.sidebarTop,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

