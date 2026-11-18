package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppContainer
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.FeatureTutorialRegistry
import com.example.data.model.UiState
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.TutorialState
import com.example.ui.components.FeatureTutorialDialog
import com.example.ui.components.LaunchExperienceViewModel
import com.example.ui.components.SpeakCraftLaunchOverlay
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.components.TutorialOverlayStep
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppUpdateScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.ConversationSimulatorScreen
import com.example.ui.screens.DailyPracticeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GrammarSpeakingScreen
import com.example.ui.screens.LearningPathsScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.ListeningShadowingScreen
import com.example.ui.screens.ModuleLessonsScreen
import com.example.ui.screens.OnboardingAssessmentScreen
import com.example.ui.screens.PronunciationLabScreen
import com.example.ui.screens.PublicSpeakingScreen
import com.example.ui.screens.RealWorldTasksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.screens.VoiceTrainingLabScreen
import com.example.ui.screens.YouTubeIntelligenceScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.MainCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class PrimaryNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryNavItems = listOf(
    PrimaryNavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    PrimaryNavItem("daily_practice", "Daily", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    PrimaryNavItem("speaking_lab", "Record", Icons.Filled.Mic, Icons.Outlined.Mic),
    PrimaryNavItem("youtube_library", "@askvinh", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    PrimaryNavItem("ai_mentor", "Mentor", Icons.Filled.Psychology, Icons.Outlined.Psychology)
)

private val routeTitles = mapOf(
    "dashboard" to "SpeakCraft",
    "daily_practice" to "Daily Practice",
    "learning_paths" to "Learning Paths",
    "vocabulary" to "Vocabulary Builder",
    "speaking_lab" to "Record & Review",
    "challenges" to "Challenges",
    "pronunciation" to "Phonetic & Intonation Lab",
    "articulation_lab" to "Articulation & Enunciation Lab",
    "voice_training" to "Voice Training Lab",
    "conversation" to "Conversation Mastery",
    "listening_lab" to "Active Listening",
    "grammar_speaking" to "Thinking & Articulation",
    "public_speaking" to "Public Speaking",
    "real_world_tasks" to "Real-World Tasks",
    "youtube_library" to "YouTube Intelligence",
    "analytics" to "Progress Analytics",
    "ai_mentor" to "AI Mentor",
    "settings" to "Settings",
    "app_updates" to "App Updates & Backups",
    "admin_console" to "Admin Console"
)

@Composable
fun SpeakCraftRoot(
    auth: FirebaseAuth = Firebase.auth,
    launchVm: LaunchExperienceViewModel = viewModel()
) {
    val context = LocalContext.current
    val container = remember(context.applicationContext) { AppContainer(context.applicationContext) }
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
                        container = container
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
    container: AppContainer
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

    LaunchedEffect(userId) {
        prefsRepo.recordUpdateCheck(System.currentTimeMillis())
    }

    val mainVm: MainCoachViewModel = viewModel(
        key = "main_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                MainCoachViewModel(container.repository, container.geminiCoachService, userId)
            }
        }
    )

    val speakingVm: SpeakingAndLabsViewModel = viewModel(
        key = "speak_$userId",
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                SpeakingAndLabsViewModel(
                    container.repository,
                    container.geminiCoachService,
                    container.audioRecorderManager
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
                    geminiCoach = container.geminiCoachService,
                    youtubeClient = container.youtubeClient,
                    appContext = context.applicationContext,
                    currentUserId = userId
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
                    repository = container.repository,
                    updateRepository = container.releaseUpdateRepository,
                    backupManager = container.backupManager,
                    prefsRepo = container.preferencesRepository,
                    youtubeClient = container.youtubeClient,
                    geminiCoach = container.geminiCoachService,
                    currentUserId = userId,
                    githubReleaseClient = container.githubReleaseClient
                )
            }
        }
    )

    val profileState by mainVm.userProfile.collectAsStateWithLifecycle()
    val sessionsState by mainVm.speakingSessions.collectAsStateWithLifecycle()
    val feedbackState by mainVm.speechFeedback.collectAsStateWithLifecycle()
    val vocabState by mainVm.vocabularyEntries.collectAsStateWithLifecycle()
    val dailyState by mainVm.dailyActivities.collectAsStateWithLifecycle()
    val learningProgressState by mainVm.learningProgress.collectAsStateWithLifecycle()
    val enrollState by mainVm.enrollments.collectAsStateWithLifecycle()
    val tasksState by mainVm.realWorldTasks.collectAsStateWithLifecycle()
    val ytSourcesState by mainVm.youtubeSources.collectAsStateWithLifecycle()
    val techState by mainVm.extractedTechniques.collectAsStateWithLifecycle()
    val sharedTechniques by mainVm.publishedSharedTechniques.collectAsStateWithLifecycle()
    val sharedLessons by mainVm.publishedSharedLessons.collectAsStateWithLifecycle()
    val sharedChallenges by mainVm.sharedChallenges.collectAsStateWithLifecycle()
    val isAdmin by mainVm.isAdmin.collectAsStateWithLifecycle()
    val recommendedNextAction by mainVm.recommendedNextAction.collectAsStateWithLifecycle()
    val actionMessage by mainVm.actionMessage.collectAsStateWithLifecycle()
    val vocabEvalState by mainVm.vocabEvalState.collectAsStateWithLifecycle()

    val updateEvaluation by updateAdminVm.updateEvaluation.collectAsStateWithLifecycle()
    val updateStatusMessage by updateAdminVm.statusMessage.collectAsStateWithLifecycle()

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
                val currentRoute = backStack?.destination?.route ?: "dashboard"
                val currentSlugArg = backStack?.arguments?.getString("slug")
                val currentLessonIdArg = backStack?.arguments?.getString("lessonId")
                val isPrimaryRoute = primaryNavItems.any { it.route == currentRoute }

                val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                val feedbackList = (feedbackState as? UiState.Success)?.data.orEmpty()
                val vocabEntries = (vocabState as? UiState.Success)?.data.orEmpty()
                val dailyList = (dailyState as? UiState.Success)?.data.orEmpty()
                val learningProgress = (learningProgressState as? UiState.Success)?.data.orEmpty()
                val enrollments = (enrollState as? UiState.Success)?.data.orEmpty()
                val realWorldList = (tasksState as? UiState.Success)?.data.orEmpty()
                val ytSources = (ytSourcesState as? UiState.Success)?.data.orEmpty()
                val userTechniques = (techState as? UiState.Success)?.data.orEmpty()

                val latestReleaseVer = updateEvaluation.latestRelease?.versionName.orEmpty()
                val showUpdateBanner = updateEvaluation.isUpdateAvailable &&
                    (updateEvaluation.isMandatory || latestReleaseVer != prefs.dismissedReleaseVersion)

                val topBarTitle = when {
                    currentRoute.startsWith("module_lessons") && !currentSlugArg.isNullOrBlank() ->
                        CurriculumCatalog.contentModules.firstOrNull { it.slug == currentSlugArg }?.title ?: "Module Lessons"
                    currentRoute.startsWith("lesson_detail") && !currentLessonIdArg.isNullOrBlank() ->
                        CurriculumCatalog.lessons.firstOrNull { it.id == currentLessonIdArg }?.title
                            ?: sharedLessons.firstOrNull { it.id == currentLessonIdArg }?.title
                            ?: "Lesson Detail"
                    else -> routeTitles[currentRoute] ?: "SpeakCraft"
                }

                var showFeatureGuideDialog by remember { mutableStateOf(false) }
                var manualTourSteps by remember { mutableStateOf<List<TutorialOverlayStep>?>(null) }
                var manualTourTitle by remember { mutableStateOf("Core Features Guided Tour") }

                val navigateTo: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        if (primaryNavItems.any { it.route == route }) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
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
                                        if (currentRoute == "dashboard") {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.RecordVoiceOver,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(topBarTitle, style = MaterialTheme.typography.titleLarge)
                                                    Text(
                                                        text = "AI Communication Studio",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = topBarTitle,
                                                style = MaterialTheme.typography.titleLarge,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        if (!isPrimaryRoute) {
                                            IconButton(onClick = { navController.navigateUp() }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { showFeatureGuideDialog = true },
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .testTag("feature_tutorial_topbar_button")
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = "Feature guide & what improves",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (isAdmin) {
                                            IconButton(onClick = { navigateTo("admin_console") }) {
                                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console")
                                            }
                                        }
                                        IconButton(onClick = { navigateTo("app_updates") }) {
                                            if (updateEvaluation.isUpdateAvailable) {
                                                BadgedBox(badge = { Badge() }) {
                                                    Icon(Icons.Default.SystemUpdate, contentDescription = "Updates available")
                                                }
                                            } else {
                                                Icon(Icons.Default.SystemUpdate, contentDescription = "App updates")
                                            }
                                        }
                                        IconButton(onClick = { navigateTo("settings") }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            }
                        },
                        bottomBar = {
                            if (!useRail) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 2.dp,
                                        windowInsets = WindowInsets.navigationBars
                                    ) {
                                        primaryNavItems.forEach { item ->
                                            val isSelected = currentRoute == item.route
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
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        Spacer(Modifier.size(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.RecordVoiceOver,
                                                    contentDescription = "SpeakCraft Studio",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.size(8.dp))
                                    }
                                ) {
                                    primaryNavItems.forEach { item ->
                                        val isSelected = currentRoute == item.route
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
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                enterTransition = { fadeIn(tween(180)) },
                                exitTransition = { fadeOut(tween(140)) },
                                popEnterTransition = { fadeIn(tween(180)) },
                                popExitTransition = { fadeOut(tween(140)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        profile = profile,
                                        sessions = sessions,
                                        vocabulary = vocabEntries,
                                        dailyActivities = dailyList,
                                        learningProgress = learningProgress,
                                        realWorldTasks = realWorldList,
                                        recommendedNextAction = recommendedNextAction,
                                        updateEvaluation = updateEvaluation,
                                        showUpdateBanner = showUpdateBanner,
                                        isAdmin = isAdmin,
                                        todayDateKey = mainVm.todayDateKey,
                                        onDismissUpdateBanner = { ver ->
                                            scope.launch { prefsRepo.dismissReleaseBanner(ver) }
                                        },
                                        onNavigate = navigateTo
                                    )
                                }
                                composable("daily_practice") {
                                    val todayActivity = dailyList.firstOrNull { it.dateKey == mainVm.todayDateKey }
                                    DailyPracticeScreen(
                                        todayDateKey = mainVm.todayDateKey,
                                        todayActivity = todayActivity,
                                        todaysWords = mainVm.dailyVocabularyBatch,
                                        onToggleStep = { id, refl, conf -> mainVm.toggleDailyStep(id, todayActivity, refl, conf) },
                                        onSaveReflection = { refl, conf -> mainVm.saveDailyReflection(todayActivity, refl, conf) },
                                        onNavigate = navigateTo
                                    )
                                }
                                composable("learning_paths") {
                                    LearningPathsScreen(
                                        enrollments = enrollments,
                                        onEnroll = { id, title -> mainVm.enrollProgram(id, title) },
                                        onToggleDay = { id, day, total -> mainVm.toggleProgramDay(id, day, total) }
                                    )
                                }
                                composable("vocabulary") {
                                    VocabularyScreen(
                                        entries = vocabEntries,
                                        todayEpochDay = mainVm.todayEpochDay,
                                        vocabEvalState = vocabEvalState,
                                        onSpeakWord = { txt -> ttsHelper.speak(txt, prefs.ttsRate) },
                                        onReviewCard = { seed, existing, ok -> mainVm.reviewVocabularyCard(seed, existing, ok) },
                                        onEvaluateUsage = { seed, existing, sent -> mainVm.evaluateVocabUsage(seed, existing, sent) },
                                        onSaveCustomWord = { w, m, l, e, n -> mainVm.saveCustomVocabularyWord(w, m, l, e, n) },
                                        onClearEval = { mainVm.clearVocabEval() }
                                    )
                                }
                                composable("speaking_lab") {
                                    SpeakingLabScreen(
                                        speakingVm = speakingVm,
                                        pastSessions = sessions,
                                        onDeleteSession = { sess -> mainVm.deleteSpeakingSession(sess) }
                                    )
                                }
                                composable("challenges") {
                                    ChallengesScreen(
                                        speakingVm = speakingVm,
                                        sharedChallenges = sharedChallenges,
                                        pastSessions = sessions
                                    )
                                }
                                composable("articulation_lab") {
                                    PronunciationLabScreen(
                                        speakingVm = speakingVm,
                                        ttsHelper = ttsHelper,
                                        pastSessions = sessions,
                                        initialFocus = "articulation"
                                    )
                                }
                                composable("pronunciation") {
                                    PronunciationLabScreen(
                                        speakingVm = speakingVm,
                                        ttsHelper = ttsHelper,
                                        pastSessions = sessions,
                                        initialFocus = "pronunciation"
                                    )
                                }
                                composable("voice_training") {
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
                                    ListeningShadowingScreen(speakingVm = speakingVm, ttsHelper = ttsHelper, pastSessions = sessions)
                                }
                                composable("grammar_speaking") {
                                    GrammarSpeakingScreen(speakingVm = speakingVm, pastSessions = sessions)
                                }
                                composable("public_speaking") {
                                    PublicSpeakingScreen(speakingVm = speakingVm, pastSessions = sessions)
                                }
                                composable("real_world_tasks") {
                                    RealWorldTasksScreen(
                                        records = realWorldList,
                                        onSaveTask = { id, title, done, refl -> mainVm.toggleRealWorldTask(id, title, done, refl) }
                                    )
                                }
                                composable(
                                    route = "module_lessons/{slug}",
                                    arguments = listOf(navArgument("slug") { type = NavType.StringType })
                                ) { entry ->
                                    val slug = entry.arguments?.getString("slug").orEmpty()
                                    ModuleLessonsScreen(
                                        moduleSlug = slug,
                                        sharedLessons = sharedLessons,
                                        learningProgress = learningProgress,
                                        onOpenLesson = { lessonId -> navigateTo("lesson_detail/$lessonId") },
                                        onOpenYouTubeLibrary = { navigateTo("youtube_library") }
                                    )
                                }
                                composable(
                                    route = "lesson_detail/{lessonId}",
                                    arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                                ) { entry ->
                                    val lessonId = entry.arguments?.getString("lessonId").orEmpty()
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
                                composable("youtube_library") {
                                    YouTubeIntelligenceScreen(
                                        coachVm = coachVm,
                                        sources = ytSources,
                                        userTechniques = userTechniques,
                                        sharedTechniques = sharedTechniques,
                                        youtubeApiKey = prefs.youtubeApiKey
                                    )
                                }
                                composable("analytics") {
                                    AnalyticsScreen(
                                        profile = profile,
                                        sessions = sessions,
                                        dailyActivities = dailyList,
                                        onNavigateToMentor = { navigateTo("ai_mentor") }
                                    )
                                }
                                composable("ai_mentor") {
                                    AiMentorScreen(
                                        coachVm = coachVm,
                                        profile = profile,
                                        sessions = sessions,
                                        feedbackList = feedbackList
                                    )
                                }
                                composable("app_updates") {
                                    AppUpdateScreen(
                                        vm = updateAdminVm,
                                        prefs = prefs,
                                        onSetUpdateChannel = { ch -> updateAdminVm.selectUpdateChannel(ch, prefs) },
                                        onSetGithubReleaseRepo = { repo -> updateAdminVm.setGithubReleaseRepo(repo, prefs) }
                                    )
                                }
                                composable("admin_console") {
                                    AdminDashboardScreen(
                                        vm = updateAdminVm,
                                        isAdmin = isAdmin,
                                        currentUserId = userId,
                                        youtubeApiKey = prefs.youtubeApiKey
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        prefs = prefs,
                                        userEmail = userEmail,
                                        isAdmin = isAdmin,
                                        updateStatusSummary = updateEvaluation.statusSummary,
                                        onSetTheme = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                                        onSetTtsRate = { rate -> scope.launch { prefsRepo.setTtsRate(rate) } },
                                        onSetYoutubeKey = { key -> scope.launch { prefsRepo.setYoutubeApiKey(key) } },
                                        onSetUpdateChannel = { ch -> updateAdminVm.selectUpdateChannel(ch, prefs) },
                                        onOpenUpdater = {
                                            updateAdminVm.checkForUpdatesManual(prefs)
                                            navigateTo("app_updates")
                                        },
                                        onCreateBackupNow = { updateAdminVm.createManualBackup(prefs) },
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
                                        }
                                    )
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
                }
            }
        }
    }
}
