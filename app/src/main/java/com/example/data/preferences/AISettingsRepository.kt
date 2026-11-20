package com.example.data.preferences

import com.example.data.ai.local.LocalExecutionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class RecognitionEnginePreference(val label: String) {
    AUTO("Automatic (On-Device Preferred)"),
    ANDROID_OFFLINE("Android SpeechRecognizer (On-Device)"),
    LOCAL_WHISPER("Local Whisper / Moonshine TFLite")
}

/**
 * Single centralized snapshot of all Local AI, Cloud AI, Speech & Transcription,
 * Video Intelligence, and Privacy controls across SpeakCraft.
 *
 * In Stage 1 (Zero-Cloud-AI Policy, [LocalExecutionPolicy.POLICY] == "LOCAL_ONLY"),
 * [cloudAiSupportedInBuild] is false and [effectiveCloudAiEnabled] is always false
 * so Cloud AI cannot be silently invoked anywhere in the app.
 */
data class AISettings(
    val localAiEnabled: Boolean = true,
    val cloudAiEnabled: Boolean = false,
    val cloudAiSupportedInBuild: Boolean = !LocalExecutionPolicy.isLocalOnly(),
    val cloudAiModelTier: String = "balanced",
    val liveTranscriptionEnabled: Boolean = false,
    val localSpeechRecognitionEnabled: Boolean = true,
    val localAudioAnalysisEnabled: Boolean = true,
    val localVideoAnalysisEnabled: Boolean = true,
    val selectedRecognitionLanguage: String = "en-US",
    val selectedRecognitionEngine: RecognitionEnginePreference = RecognitionEnginePreference.AUTO
) {
    val effectiveCloudAiEnabled: Boolean
        get() = cloudAiSupportedInBuild && cloudAiEnabled

    val audioVideoUploadedToCloud: Boolean = false
}

/**
 * Single source of truth for all AI & Intelligence settings in SpeakCraft.
 * Wraps [AppPreferencesRepository] so there is only ever one DataStore instance.
 */
class AISettingsRepository(
    private val preferencesRepository: AppPreferencesRepository
) {
    val aiSettingsFlow: Flow<AISettings> = preferencesRepository.preferencesFlow.map { prefs ->
        val enginePref = RecognitionEnginePreference.entries.firstOrNull {
            it.name == prefs.selectedRecognitionEngine
        } ?: RecognitionEnginePreference.AUTO
        val cloudSupported = !LocalExecutionPolicy.isLocalOnly()
        AISettings(
            localAiEnabled = prefs.localAiEnabled,
            cloudAiEnabled = if (cloudSupported) prefs.cloudAiEnabled else false,
            cloudAiSupportedInBuild = cloudSupported,
            cloudAiModelTier = prefs.cloudAiModelTier,
            liveTranscriptionEnabled = prefs.liveTranscriptionEnabled,
            localSpeechRecognitionEnabled = prefs.localSpeechRecognitionEnabled,
            localAudioAnalysisEnabled = prefs.localAudioAnalysisEnabled,
            localVideoAnalysisEnabled = prefs.localVideoAnalysisEnabled,
            selectedRecognitionLanguage = prefs.selectedRecognitionLanguage,
            selectedRecognitionEngine = enginePref
        )
    }

    suspend fun setLocalAiEnabled(enabled: Boolean) {
        preferencesRepository.setLocalAiEnabled(enabled)
    }

    /**
     * Enables or disables Cloud AI centrally.
     * In Stage 1 ([LocalExecutionPolicy.isLocalOnly] == true), Cloud AI is unavailable
     * by policy, so attempts to enable it are rejected safely and persisted as false.
     */
    suspend fun setCloudAiEnabled(enabled: Boolean): Result<Boolean> {
        if (LocalExecutionPolicy.isLocalOnly()) {
            preferencesRepository.setCloudAiEnabled(false)
            return if (enabled) {
                Result.failure(
                    IllegalStateException(
                        "Cloud AI Coach is unavailable under Stage 1 Zero-Cloud-AI Policy. Local Intelligence handles all coaching on-device."
                    )
                )
            } else {
                Result.success(false)
            }
        }
        preferencesRepository.setCloudAiEnabled(enabled)
        return Result.success(enabled)
    }

    suspend fun setCloudAiModelTier(tier: String) {
        preferencesRepository.setCloudAiModelTier(tier)
    }

    suspend fun setLiveTranscriptionEnabled(enabled: Boolean) {
        preferencesRepository.setLiveTranscriptionEnabled(enabled)
    }

    suspend fun setLocalSpeechRecognitionEnabled(enabled: Boolean) {
        preferencesRepository.setLocalSpeechRecognitionEnabled(enabled)
    }

    suspend fun setLocalAudioAnalysisEnabled(enabled: Boolean) {
        preferencesRepository.setLocalAudioAnalysisEnabled(enabled)
    }

    suspend fun setLocalVideoAnalysisEnabled(enabled: Boolean) {
        preferencesRepository.setLocalVideoAnalysisEnabled(enabled)
    }

    suspend fun setSelectedRecognitionLanguage(languageTag: String) {
        preferencesRepository.setSelectedRecognitionLanguage(languageTag)
    }

    suspend fun setSelectedRecognitionEngine(engine: RecognitionEnginePreference) {
        preferencesRepository.setSelectedRecognitionEngine(engine.name)
    }
}
