package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "speakcraft_prefs")

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val themeMode: ThemePreference = ThemePreference.SYSTEM,
    val ttsRate: Float = 1.0f,
    val completedTutorialKeys: Set<String> = emptySet(),
    val firstUseTutorialsEnabled: Boolean = true,
    val hasCompletedInitialLaunch: Boolean = false,
    val fontScalePreference: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val voiceAutoSpeakEnabled: Boolean = true,
    val localAiEnabled: Boolean = true,
    val liveTranscriptionEnabled: Boolean = false,
    val localSpeechRecognitionEnabled: Boolean = true,
    val localAudioAnalysisEnabled: Boolean = true,
    val localVideoAnalysisEnabled: Boolean = true,
    val selectedRecognitionLanguage: String = "en-US",
    val selectedRecognitionEngine: String = "AUTO"
)

class AppPreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val ttsRateKey = floatPreferencesKey("tts_rate")
    private val completedTutorialsKey = stringSetPreferencesKey("completed_tutorial_keys")
    private val firstUseTutorialsEnabledKey = booleanPreferencesKey("first_use_tutorials_enabled")
    private val initialLaunchCompletedKey = booleanPreferencesKey("initial_launch_completed")
    private val fontScaleKey = floatPreferencesKey("font_scale_preference")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val voiceAutoSpeakKey = booleanPreferencesKey("voice_auto_speak_enabled")
    private val localAiEnabledKey = booleanPreferencesKey("ai_local_enabled")
    private val liveTranscriptionEnabledKey = booleanPreferencesKey("ai_live_transcription_enabled")
    private val localSpeechRecEnabledKey = booleanPreferencesKey("ai_local_speech_recognition_enabled")
    private val localAudioAnalysisEnabledKey = booleanPreferencesKey("ai_local_audio_analysis_enabled")
    private val localVideoAnalysisEnabledKey = booleanPreferencesKey("ai_local_video_analysis_enabled")
    private val recognitionLanguageKey = stringPreferencesKey("ai_recognition_language")
    private val recognitionEngineKey = stringPreferencesKey("ai_recognition_engine")

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val theme = enumValues<ThemePreference>().firstOrNull { it.name == prefs[themeKey] }
            ?: ThemePreference.SYSTEM
        val rate = prefs[ttsRateKey] ?: 1.0f
        val recLang = prefs[recognitionLanguageKey]?.takeIf { it.isNotBlank() } ?: "en-US"
        val recEngine = prefs[recognitionEngineKey]?.takeIf {
            it in listOf("AUTO", "ANDROID_OFFLINE", "LOCAL_WHISPER")
        } ?: "AUTO"
        AppPreferences(
            themeMode = theme,
            ttsRate = rate,
            completedTutorialKeys = prefs[completedTutorialsKey] ?: emptySet(),
            firstUseTutorialsEnabled = prefs[firstUseTutorialsEnabledKey] ?: true,
            hasCompletedInitialLaunch = prefs[initialLaunchCompletedKey] ?: false,
            fontScalePreference = (prefs[fontScaleKey] ?: 1.0f).coerceIn(0.85f, 1.3f),
            reducedMotion = prefs[reducedMotionKey] ?: false,
            voiceAutoSpeakEnabled = prefs[voiceAutoSpeakKey] ?: true,
            localAiEnabled = prefs[localAiEnabledKey] ?: true,
            liveTranscriptionEnabled = prefs[liveTranscriptionEnabledKey] ?: false,
            localSpeechRecognitionEnabled = prefs[localSpeechRecEnabledKey] ?: true,
            localAudioAnalysisEnabled = prefs[localAudioAnalysisEnabledKey] ?: true,
            localVideoAnalysisEnabled = prefs[localVideoAnalysisEnabledKey] ?: true,
            selectedRecognitionLanguage = recLang,
            selectedRecognitionEngine = recEngine
        )
    }

    suspend fun setThemeMode(mode: ThemePreference) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setTtsRate(rate: Float) {
        context.dataStore.edit { it[ttsRateKey] = rate.coerceIn(0.6f, 1.4f) }
    }

    suspend fun markTutorialCompleted(key: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[completedTutorialsKey] ?: emptySet()
            prefs[completedTutorialsKey] = current + key
        }
    }

    suspend fun markTutorialsCompleted(keys: Set<String>) {
        if (keys.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[completedTutorialsKey] ?: emptySet()
            prefs[completedTutorialsKey] = current + keys
        }
    }

    suspend fun setFirstUseTutorialsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[firstUseTutorialsEnabledKey] = enabled }
    }

    suspend fun resetAllTutorials() {
        context.dataStore.edit { prefs ->
            prefs[completedTutorialsKey] = emptySet()
            prefs[firstUseTutorialsEnabledKey] = true
        }
    }

    suspend fun markInitialLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[initialLaunchCompletedKey] = true
        }
    }

    suspend fun setFontScalePreference(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[fontScaleKey] = scale.coerceIn(0.85f, 1.3f)
        }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[reducedMotionKey] = enabled
        }
    }

    suspend fun setVoiceAutoSpeakEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[voiceAutoSpeakKey] = enabled
        }
    }

    suspend fun setLocalAiEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[localAiEnabledKey] = enabled
        }
    }

    suspend fun setLiveTranscriptionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[liveTranscriptionEnabledKey] = enabled
        }
    }

    suspend fun setLocalSpeechRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[localSpeechRecEnabledKey] = enabled
        }
    }

    suspend fun setLocalAudioAnalysisEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[localAudioAnalysisEnabledKey] = enabled
        }
    }

    suspend fun setLocalVideoAnalysisEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[localVideoAnalysisEnabledKey] = enabled
        }
    }

    suspend fun setSelectedRecognitionLanguage(languageTag: String) {
        val safe = languageTag.trim().ifBlank { "en-US" }
        context.dataStore.edit { prefs ->
            prefs[recognitionLanguageKey] = safe
        }
    }

    suspend fun setSelectedRecognitionEngine(engine: String) {
        val safe = engine.takeIf { it in listOf("AUTO", "ANDROID_OFFLINE", "LOCAL_WHISPER") } ?: "AUTO"
        context.dataStore.edit { prefs ->
            prefs[recognitionEngineKey] = safe
        }
    }
}
