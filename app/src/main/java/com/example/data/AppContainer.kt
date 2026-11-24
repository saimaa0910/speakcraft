package com.example.data

import android.content.Context
import com.example.R
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.local.DeviceAdaptiveModelInstaller
import com.example.data.ai.local.DeviceCapabilityManager
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.OfflineAIAvailabilityManager
import com.example.data.ai.local.OnDeviceModelManager
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.preferences.AISettingsRepository
import com.example.core.common.dispatch.DispatcherProvider
import com.example.core.common.dispatch.StandardDispatcherProvider
import com.example.core.common.time.Clock
import com.example.core.common.time.DateKeyProvider
import com.example.core.common.time.SystemClock
import com.example.core.common.time.SystemDateKeyProvider
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.TutorialStateManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Core utilities (T-02.1.2)
    val clock: Clock by lazy { SystemClock }
    val dateKeys: DateKeyProvider by lazy { SystemDateKeyProvider(clock) }
    val dispatchers: DispatcherProvider by lazy { StandardDispatcherProvider }
    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + dispatchers.default) }

    // Local intelligence engine (EP-06/EP-07, ARC-05; video ARC-07): one recording at a time app-wide.
    val recordingArbiter: com.example.speech.session.RecordingArbiter by lazy { com.example.speech.session.RecordingArbiter() }
    /** Coach tips: content-pack tips when `assets/content/tips.json` is present and valid, else built-in (ARC-03 §6.9). */
    val tipCatalog: List<com.example.speech.feedback.TipDefinition> by lazy {
        com.example.speech.android.AssetTipCatalog.load(appContext)
    }
    val speechAttemptRunner: com.example.speech.session.SpeechAttemptRunner by lazy {
        // The catalogue is read lazily on the thread that evaluates the attempt, never at app start.
        com.example.speech.session.SpeechAttemptRunner(recordingArbiter, tipCatalog = { tipCatalog })
    }
    val onlineTranscriptionConsent: com.example.speech.android.OnlineTranscriptionConsentStore by lazy {
        com.example.speech.android.OnlineTranscriptionConsentStore(appContext)
    }
    val languagePackManager: com.example.speech.android.LanguagePackManager by lazy {
        com.example.speech.android.LanguagePackManager(appContext)
    }
    /** Coach practice on the phone: microphone, recogniser and camera wired to the engine (EP-08). */
    val phonePracticeEngine: com.example.speech.android.PhonePracticeEngine by lazy {
        com.example.speech.android.PhonePracticeEngine(appContext, speechAttemptRunner)
    }
    /** Practice catalogue (content pack + built-in quick practice); read on first use, off the main thread. */
    val practiceCatalog: com.example.speech.content.PracticeCatalog by lazy {
        com.example.speech.android.AssetPracticeCatalog.load(appContext)
    }

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

    val deviceAdaptiveModelInstaller: DeviceAdaptiveModelInstaller by lazy {
        DeviceAdaptiveModelInstaller(
            context = appContext,
            modelManager = onDeviceModelManager,
            capabilityManager = deviceCapabilityManager,
            preferencesRepository = preferencesRepository,
            aiSettingsRepository = aiSettingsRepository
        )
    }

    val localIntelligenceOrchestrator: LocalIntelligenceOrchestrator by lazy {
        LocalIntelligenceOrchestrator(appContext, onDeviceModelManager, deviceCapabilityManager)
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
            prefsRepo = preferencesRepository,
            appContext = appContext,
            modelManager = onDeviceModelManager,
            aiSettingsRepo = aiSettingsRepository
        )
    }

    val audioRecorderManager: AudioRecorderManager by lazy {
        AudioRecorderManager(appContext)
    }

    val tutorialStateManager: TutorialStateManager by lazy {
        TutorialStateManager(appContext)
    }

    val activityRepository: com.example.data.firebase.ActivityFirestoreRepository by lazy {
        com.example.data.firebase.ActivityFirestoreRepository(firestore)
    }

    val universalEvaluationEngine: com.example.data.activity.UniversalEvaluationEngine by lazy {
        com.example.data.activity.UniversalEvaluationEngine()
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
