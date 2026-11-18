package com.example.data.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    val lastVerifiedVersionCode: Long = 0L
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

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val theme = enumValues<ThemePreference>().firstOrNull { it.name == prefs[themeKey] }
            ?: ThemePreference.SYSTEM
        val rate = prefs[ttsRateKey] ?: 1.0f
        val apiKey = KeystoreCipherHelper.decrypt(prefs[ytKey].orEmpty())
        val channel = prefs[updateChannelKey]?.takeIf { it in listOf("stable", "beta") } ?: "stable"
        AppPreferences(
            themeMode = theme,
            ttsRate = rate,
            youtubeApiKey = apiKey,
            updateChannel = channel,
            lastUpdateCheckMs = prefs[lastCheckKey] ?: 0L,
            dismissedReleaseVersion = prefs[dismissedReleaseKey].orEmpty(),
            lastVerifiedVersionCode = prefs[verifiedVersionCodeKey] ?: 0L
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
}
