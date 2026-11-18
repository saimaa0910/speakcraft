package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tutorialStateDataStore by preferencesDataStore(name = "speakcraft_tutorial_state")

data class TutorialState(
    val userId: String = "",
    val completedFeatures: Set<String> = emptySet(),
    val firstUseTutorialsEnabled: Boolean = true
) {
    fun hasCompleted(featureKey: String): Boolean =
        completedFeatures.contains(featureKey)

    fun shouldShowTutorial(featureKey: String): Boolean =
        firstUseTutorialsEnabled && !completedFeatures.contains(featureKey)
}

class TutorialStateManager(private val context: Context) {

    private fun completedKey(userId: String) =
        stringSetPreferencesKey("completed_tutorials_${sanitizeUserId(userId)}")

    private fun enabledKey(userId: String) =
        booleanPreferencesKey("first_use_tutorials_enabled_${sanitizeUserId(userId)}")

    private fun sanitizeUserId(userId: String): String =
        userId.trim().ifBlank { "anonymous" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    fun observeTutorialState(userId: String): Flow<TutorialState> {
        val cKey = completedKey(userId)
        val eKey = enabledKey(userId)
        return context.tutorialStateDataStore.data.map { prefs ->
            TutorialState(
                userId = userId,
                completedFeatures = prefs[cKey] ?: emptySet(),
                firstUseTutorialsEnabled = prefs[eKey] ?: true
            )
        }
    }

    suspend fun markFeatureCompleted(userId: String, featureKey: String) {
        val cleanFeature = featureKey.trim()
        if (cleanFeature.isBlank()) return
        val cKey = completedKey(userId)
        context.tutorialStateDataStore.edit { prefs ->
            val current = prefs[cKey] ?: emptySet()
            prefs[cKey] = current + cleanFeature
        }
    }

    suspend fun markFeaturesCompleted(userId: String, featureKeys: Set<String>) {
        val cleaned = featureKeys.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (cleaned.isEmpty()) return
        val cKey = completedKey(userId)
        context.tutorialStateDataStore.edit { prefs ->
            val current = prefs[cKey] ?: emptySet()
            prefs[cKey] = current + cleaned
        }
    }

    suspend fun setFirstUseTutorialsEnabled(userId: String, enabled: Boolean) {
        val eKey = enabledKey(userId)
        context.tutorialStateDataStore.edit { prefs ->
            prefs[eKey] = enabled
        }
    }

    suspend fun resetUserTutorials(userId: String) {
        val cKey = completedKey(userId)
        val eKey = enabledKey(userId)
        context.tutorialStateDataStore.edit { prefs ->
            prefs[cKey] = emptySet()
            prefs[eKey] = true
        }
    }
}
