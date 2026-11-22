package com.example

import com.example.data.ai.local.LocalExecutionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * TC-SEC-010 / ADR-003 (T-04.1.2):
 * Verifies at classpath and build-configuration level that Cloud AI SDKs (`firebase-ai`),
 * `FirebaseAiLogicService`, `GeminiCoachService`, and `cloud_ai_*` preference keys are
 * completely removed from the application.
 */
class ZeroCloudAiNetworkBoundaryTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "gradle").exists()) cwd else cwd.parentFile ?: cwd
    }

    @Test
    fun zero_cloud_policy_is_strictly_enforced() {
        assertEquals("LOCAL_ONLY", LocalExecutionPolicy.POLICY)
        LocalExecutionPolicy.verifyLocalOnly()
    }

    @Test
    fun firebase_ai_sdk_is_absent_from_runtime_classpath() {
        val prohibitedClasses = listOf(
            "com.google.firebase.ai.FirebaseAI",
            "com.google.firebase.ai.GenerativeModel",
            "com.example.data.ai.cloud.FirebaseAiLogicService",
            "com.example.data.ai.GeminiCoachService"
        )
        prohibitedClasses.forEach { fqcn ->
            val loaded = runCatching { Class.forName(fqcn) }.isSuccess
            assertFalse("Prohibited Cloud AI class must NOT be on runtime classpath: $fqcn", loaded)
        }
    }

    @Test
    fun build_config_and_preferences_contain_no_cloud_ai_dependencies_or_keys() {
        val appGradle = File(repoRoot(), "app/build.gradle.kts").readText()
        val libsToml = File(repoRoot(), "gradle/libs.versions.toml").readText()
        val prefsRepo = File(repoRoot(), "app/src/main/java/com/example/data/preferences/AppPreferencesRepository.kt").readText()
        val aiSettingsRepo = File(repoRoot(), "app/src/main/java/com/example/data/preferences/AISettingsRepository.kt").readText()

        assertFalse("app/build.gradle.kts must not depend on firebase.ai", appGradle.contains("firebase.ai"))
        assertFalse("libs.versions.toml must not define firebase-ai", libsToml.contains("firebase-ai"))
        assertFalse("AppPreferencesRepository must not retain cloud_ai_enabled key", prefsRepo.contains("cloud_ai_enabled"))
        assertFalse("AppPreferencesRepository must not retain cloud_ai_model_tier key", prefsRepo.contains("cloud_ai_model_tier"))
        assertFalse("AISettingsRepository must not expose cloudAiEnabled", aiSettingsRepo.contains("cloudAiEnabled"))

        val ciYml = File(repoRoot(), ".github/workflows/ci.yml").readText()
        assertTrue(
            "ci.yml must enforce firebase-ai absence check",
            ciYml.contains("firebase[-.]ai")
        )
    }
}
