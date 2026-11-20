package com.example.data

import android.content.Context
import com.example.R
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.local.DeviceCapabilityManager
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.OfflineAIAvailabilityManager
import com.example.data.ai.local.OnDeviceModelManager
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.TutorialStateManager
import com.example.data.update.ReleaseUpdateRepository
import com.example.data.update.UpdateBackupManager
import com.example.data.youtube.YouTubeIntelligenceClient
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }

    val repository: SpeakCraftRepository by lazy {
        SpeakCraftRepository(firestore)
    }

    val onDeviceModelManager: OnDeviceModelManager by lazy { OnDeviceModelManager(appContext) }
    val deviceCapabilityManager: DeviceCapabilityManager by lazy { DeviceCapabilityManager(appContext) }
    val offlineAIAvailabilityManager: OfflineAIAvailabilityManager by lazy {
        OfflineAIAvailabilityManager(appContext, onDeviceModelManager, deviceCapabilityManager)
    }

    val localIntelligenceOrchestrator: LocalIntelligenceOrchestrator by lazy {
        LocalIntelligenceOrchestrator(appContext, onDeviceModelManager, deviceCapabilityManager)
    }

    val firebaseAiLogicService: com.example.data.ai.cloud.FirebaseAiLogicService by lazy {
        com.example.data.ai.cloud.FirebaseAiLogicService()
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(appContext)
    }

    val aiSettingsRepository: AISettingsRepository by lazy {
        AISettingsRepository(preferencesRepository)
    }

    val intelligenceCapabilityManager: IntelligenceCapabilityManager by lazy {
        IntelligenceCapabilityManager(
            context = appContext,
            modelManager = onDeviceModelManager,
            offlineAvailabilityManager = offlineAIAvailabilityManager
        )
    }

    val liveTranscriptionManager: LiveTranscriptionManager by lazy {
        LiveTranscriptionManager(
            context = appContext,
            capabilityManager = intelligenceCapabilityManager
        )
    }

    val hybridAiCoordinator: HybridAiCoachCoordinator by lazy {
        HybridAiCoachCoordinator(
            localOrchestrator = localIntelligenceOrchestrator,
            cloudService = firebaseAiLogicService,
            prefsRepo = preferencesRepository,
            appContext = appContext,
            modelManager = onDeviceModelManager,
            aiSettingsRepo = aiSettingsRepository
        )
    }

    val youtubeClient: YouTubeIntelligenceClient by lazy {
        YouTubeIntelligenceClient()
    }

    val audioRecorderManager: AudioRecorderManager by lazy {
        AudioRecorderManager(appContext)
    }

    val tutorialStateManager: TutorialStateManager by lazy {
        TutorialStateManager(appContext)
    }

    val backupManager: UpdateBackupManager by lazy {
        UpdateBackupManager(appContext)
    }

    val releaseUpdateRepository: ReleaseUpdateRepository by lazy {
        ReleaseUpdateRepository(appContext, backupManager)
    }

    val githubReleaseClient: com.example.data.update.GitHubReleaseClient by lazy {
        com.example.data.update.GitHubReleaseClient()
    }

    val activityRepository: com.example.data.firebase.ActivityFirestoreRepository by lazy {
        com.example.data.firebase.ActivityFirestoreRepository(firestore)
    }

    val universalEvaluationEngine: com.example.data.activity.UniversalEvaluationEngine by lazy {
        com.example.data.activity.UniversalEvaluationEngine(
            aiClientOverride = { activity, response, strategy, metrics ->
                hybridAiCoordinator.evaluateUniversalActivityIfCloud(activity, response, strategy, metrics)
            }
        )
    }

    val activityEngine: com.example.data.activity.ActivityEngine by lazy {
        com.example.data.activity.ActivityEngine(universalEvaluationEngine)
    }

    val learningProgressManager: com.example.data.learning.LearningProgressManager by lazy {
        com.example.data.learning.LearningProgressManager(appContext)
    }

    val notificationManager: com.example.data.notifications.NotificationManagerRepository by lazy {
        com.example.data.notifications.NotificationManagerRepository(appContext)
    }
}
