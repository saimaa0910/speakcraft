package com.example.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class RecognitionEnginePreference(val label: String) {
    AUTO("Automatic (On-Device Preferred)"),
    ANDROID_OFFLINE("Android SpeechRecognizer (On-Device)"),

    /**
     * Retired: the app-managed Whisper/Moonshine recogniser was fabricated and is removed (FND-AI-03). Kept only
     * so stored preferences still parse; it is read as [AUTO] ([fromStored]) and never offered.
     */
    LOCAL_WHISPER("Automatic (On-Device Preferred)");

    companion object {
        /** Choices offered in Settings. */
        val SELECTABLE: List<RecognitionEnginePreference> = listOf(AUTO, ANDROID_OFFLINE)

        /** Parses a stored preference; unknown values and the retired [LOCAL_WHISPER] behave as [AUTO]. */
        fun fromStored(name: String?): RecognitionEnginePreference =
            when (entries.firstOrNull { it.name == name }) {
                ANDROID_OFFLINE -> ANDROID_OFFLINE
                else -> AUTO
            }
    }
}

/**
 * Single centralized snapshot of all on-device Local Intelligence, Speech & Transcription,
 * Video Intelligence, and Privacy controls across SpeakCraft (ADR-003: Zero Cloud AI).
 */
data class AISettings(
    val localAiEnabled: Boolean = true,
    val liveTranscriptionEnabled: Boolean = false,
    val localSpeechRecognitionEnabled: Boolean = true,
    val localAudioAnalysisEnabled: Boolean = true,
    val localVideoAnalysisEnabled: Boolean = true,
    val selectedRecognitionLanguage: String = "en-US",
    val selectedRecognitionEngine: RecognitionEnginePreference = RecognitionEnginePreference.AUTO
) {
    val audioVideoUploadedToCloud: Boolean = false
}

/**
 * Single source of truth for all on-device Intelligence settings in SpeakCraft.
 * Wraps [AppPreferencesRepository] so there is only ever one DataStore instance.
 */
class AISettingsRepository(
    private val preferencesRepository: AppPreferencesRepository
) {
    val aiSettingsFlow: Flow<AISettings> = preferencesRepository.preferencesFlow.map { prefs ->
        val enginePref = RecognitionEnginePreference.fromStored(prefs.selectedRecognitionEngine)
        AISettings(
            localAiEnabled = prefs.localAiEnabled,
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
        preferencesRepository.setSelectedRecognitionEngine(RecognitionEnginePreference.fromStored(engine.name).name)
    }
}
