package com.example.data.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "speakcraft_prefs")

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val themeMode: ThemePreference = ThemePreference.SYSTEM,
    val ttsRate: Float = 1.0f,
    val youtubeApiKey: String = "",
    val updateChannel: String = "stable",
    val lastUpdateCheckMs: Long = 0L,
    val dismissedReleaseVersion: String = "",
    val lastVerifiedVersionCode: Long = 0L,
    val completedTutorialKeys: Set<String> = emptySet(),
    val firstUseTutorialsEnabled: Boolean = true,
    val hasCompletedInitialLaunch: Boolean = false,
    val githubReleaseRepo: String = "",
    val fontScalePreference: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val cloudAiEnabled: Boolean = false,
    val cloudAiModelTier: String = "balanced",
    val voiceAutoSpeakEnabled: Boolean = true
)

private object KeystoreCipherHelper {
    private const val KEY_ALIAS = "speakcraft_yt_api_key_aes"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getOrCreateKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            generator.init(spec)
            generator.generateKey()
        }
    }.getOrNull()

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val key = getOrCreateKey() ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            "enc:${Base64.encodeToString(combined, Base64.NO_WRAP)}"
        }.getOrDefault("")
    }

    fun decrypt(stored: String): String {
        if (stored.isBlank()) return ""
        if (!stored.startsWith("enc:")) return stored // legacy fallback read-only
        val key = getOrCreateKey() ?: return ""
        return runCatching {
            val combined = Base64.decode(stored.removePrefix("enc:"), Base64.NO_WRAP)
            val ivLen = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val cipherBytes = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }
}

class AppPreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val ttsRateKey = floatPreferencesKey("tts_rate")
    private val ytKey = stringPreferencesKey("youtube_api_key")
    private val updateChannelKey = stringPreferencesKey("update_channel")
    private val lastCheckKey = longPreferencesKey("last_update_check_ms")
    private val dismissedReleaseKey = stringPreferencesKey("dismissed_release_version")
    private val verifiedVersionCodeKey = longPreferencesKey("last_verified_version_code")
    private val completedTutorialsKey = stringSetPreferencesKey("completed_tutorial_keys")
    private val firstUseTutorialsEnabledKey = booleanPreferencesKey("first_use_tutorials_enabled")
    private val initialLaunchCompletedKey = booleanPreferencesKey("initial_launch_completed")
    private val githubReleaseRepoKey = stringPreferencesKey("github_release_repo")
    private val fontScaleKey = floatPreferencesKey("font_scale_preference")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val cloudAiEnabledKey = booleanPreferencesKey("cloud_ai_enabled")
    private val cloudAiModelTierKey = stringPreferencesKey("cloud_ai_model_tier")
    private val voiceAutoSpeakKey = booleanPreferencesKey("voice_auto_speak_enabled")

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val theme = enumValues<ThemePreference>().firstOrNull { it.name == prefs[themeKey] }
            ?: ThemePreference.SYSTEM
        val rate = prefs[ttsRateKey] ?: 1.0f
        val apiKey = KeystoreCipherHelper.decrypt(prefs[ytKey].orEmpty())
        val defaultChannel = com.example.BuildConfig.BUILD_CHANNEL.takeIf { it in listOf("stable", "beta") } ?: "stable"
        val channel = prefs[updateChannelKey]?.takeIf { it in listOf("stable", "beta") } ?: defaultChannel
        val defaultRepo = com.example.BuildConfig.GITHUB_RELEASE_REPO
        val storedRepo = prefs[githubReleaseRepoKey].orEmpty().ifBlank { defaultRepo }
        val modelTier = prefs[cloudAiModelTierKey]?.takeIf { it in listOf("balanced", "deep", "fast") } ?: "balanced"
        AppPreferences(
            themeMode = theme,
            ttsRate = rate,
            youtubeApiKey = apiKey,
            updateChannel = channel,
            lastUpdateCheckMs = prefs[lastCheckKey] ?: 0L,
            dismissedReleaseVersion = prefs[dismissedReleaseKey].orEmpty(),
            lastVerifiedVersionCode = prefs[verifiedVersionCodeKey] ?: 0L,
            completedTutorialKeys = prefs[completedTutorialsKey] ?: emptySet(),
            firstUseTutorialsEnabled = prefs[firstUseTutorialsEnabledKey] ?: true,
            hasCompletedInitialLaunch = prefs[initialLaunchCompletedKey] ?: false,
            githubReleaseRepo = storedRepo,
            fontScalePreference = (prefs[fontScaleKey] ?: 1.0f).coerceIn(0.85f, 1.3f),
            reducedMotion = prefs[reducedMotionKey] ?: false,
            cloudAiEnabled = prefs[cloudAiEnabledKey] ?: false,
            cloudAiModelTier = modelTier,
            voiceAutoSpeakEnabled = prefs[voiceAutoSpeakKey] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemePreference) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setTtsRate(rate: Float) {
        context.dataStore.edit { it[ttsRateKey] = rate.coerceIn(0.6f, 1.4f) }
    }

    suspend fun setYoutubeApiKey(key: String) {
        val trimmed = key.trim()
        val encrypted = if (trimmed.isBlank()) "" else KeystoreCipherHelper.encrypt(trimmed)
        context.dataStore.edit { it[ytKey] = encrypted }
    }

    suspend fun setUpdateChannel(channel: String) {
        val safe = if (channel == "beta") "beta" else "stable"
        context.dataStore.edit { it[updateChannelKey] = safe }
    }

    suspend fun recordUpdateCheck(timestampMs: Long) {
        context.dataStore.edit { it[lastCheckKey] = timestampMs }
    }

    suspend fun dismissReleaseBanner(versionName: String) {
        context.dataStore.edit { it[dismissedReleaseKey] = versionName }
    }

    suspend fun recordVerifiedVersionCode(versionCode: Long) {
        context.dataStore.edit { it[verifiedVersionCodeKey] = versionCode }
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

    suspend fun setGithubReleaseRepo(repoSlug: String) {
        context.dataStore.edit { prefs ->
            prefs[githubReleaseRepoKey] = com.example.data.update.GitHubReleaseClient.normalizeRepoSlug(repoSlug)
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

    suspend fun setCloudAiEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[cloudAiEnabledKey] = enabled
        }
    }

    suspend fun setCloudAiModelTier(tier: String) {
        val safe = tier.takeIf { it in listOf("balanced", "deep", "fast") } ?: "balanced"
        context.dataStore.edit { prefs ->
            prefs[cloudAiModelTierKey] = safe
        }
    }

    suspend fun setVoiceAutoSpeakEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[voiceAutoSpeakKey] = enabled
        }
    }
}
