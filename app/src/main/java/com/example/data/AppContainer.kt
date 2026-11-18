package com.example.data

import android.content.Context
import com.example.R
import com.example.data.ai.GeminiCoachService
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
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

    val geminiCoachService: GeminiCoachService by lazy {
        GeminiCoachService()
    }

    val youtubeClient: YouTubeIntelligenceClient by lazy {
        YouTubeIntelligenceClient()
    }

    val audioRecorderManager: AudioRecorderManager by lazy {
        AudioRecorderManager(appContext)
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(appContext)
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
}
