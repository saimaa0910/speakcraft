package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies Stage B Sprint 1 Task T-01.2.1 (ENG-01 §4.2, ISS-08, CL-20):
 * - PR CI workflow `.github/workflows/ci.yml` exists and runs on pull_request + push to main.
 * - CI runs debug build, release Kotlin compile, unit/Robolectric tests, and lint with zero secrets.
 * - CI enforces static security checks and never uses `pull_request_target`.
 * - Version catalog maps `junit:junit` to `version.ref = "junit"` (4.13.2), not AndroidX `junitVersion` (1.3.0).
 */
class CiWorkflowPolicyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, ".github").exists()) cwd else cwd.parentFile ?: cwd
    }

    @Test
    fun `ci workflow exists and triggers on pull_request and push to main`() {
        val ciFile = File(repoRoot(), ".github/workflows/ci.yml")
        assertTrue("ci.yml must exist under .github/workflows/", ciFile.exists())
        val content = ciFile.readText()

        assertTrue("ci.yml must trigger on pull_request", content.contains("pull_request:"))
        assertTrue("ci.yml must trigger on push to main", content.contains("push:") && content.contains("- main"))
        assertTrue("ci.yml must use read-only permissions", content.contains("permissions:\n  contents: read"))
        assertTrue("ci.yml must cancel superseded runs", content.contains("cancel-in-progress: true"))
    }

    @Test
    fun `ci workflow runs build, unit tests, and lint without secrets or pull_request_target`() {
        val content = File(repoRoot(), ".github/workflows/ci.yml").readText()

        assertTrue("ci.yml must run assembleDebug", content.contains(":app:assembleDebug"))
        assertTrue("ci.yml must compile release Kotlin", content.contains(":app:compileReleaseKotlin"))
        assertTrue("ci.yml must run testDebugUnitTest", content.contains(":app:testDebugUnitTest"))
        assertTrue("ci.yml must run lintDebug", content.contains(":app:lintDebug"))
        assertTrue("ci.yml must upload test and lint reports", content.contains("actions/upload-artifact@v4"))
        assertTrue("ci.yml must include static security checks job", content.contains("security-static:"))

        assertFalse(
            "ci.yml must never declare pull_request_target trigger",
            Regex("""(?m)^\s*pull_request_target\s*:""").containsMatchIn(content)
        )
        assertFalse(
            "ci.yml must not reference GitHub secrets",
            Regex("""\$\{\{\s*secrets\.""").containsMatchIn(content)
        )
    }

    @Test
    fun `version catalog maps junit to 4_13_2 version ref instead of junitVersion`() {
        val toml = File(repoRoot(), "gradle/libs.versions.toml").readText()
        assertTrue(
            "libs.versions.toml must map junit library to version.ref = \"junit\"",
            toml.contains("""junit = { group = "junit", name = "junit", version.ref = "junit" }""")
        )
        assertFalse(
            "libs.versions.toml must not map junit:junit to AndroidX junitVersion",
            toml.contains("""junit = { group = "junit", name = "junit", version.ref = "junitVersion" }""")
        )
    }

    @Test
    fun `ci build is resilient on clean checkout without gitignored debug keystore or google-services`() {
        val ciContent = File(repoRoot(), ".github/workflows/ci.yml").readText()
        assertTrue(
            "ci.yml must prepare ephemeral debug.keystore for clean checkout assembleDebug",
            ciContent.contains("debug.keystore.base64") && ciContent.contains("rm -f debug.keystore")
        )

        val authHelper = File(repoRoot(), "app/src/main/java/com/example/data/firebase/AuthHelper.kt").readText()
        assertFalse(
            "AuthHelper.kt must not hard-reference R.string.default_web_client_id (breaks secret-free CI)",
            authHelper.contains("R.string.default_web_client_id")
        )
        assertTrue(
            "AuthHelper.kt must resolve default_web_client_id via runtime getIdentifier lookup",
            authHelper.contains("""getIdentifier("default_web_client_id", "string"""")
        )

        val appGradle = File(repoRoot(), "app/build.gradle.kts").readText()
        assertTrue(
            "app/build.gradle.kts must set minSdk = 26 (D-14, resolves java.time NewApi lint errors)",
            Regex("""minSdk\s*=\s*26\b""").containsMatchIn(appGradle)
        )
    }

    @Test
    fun `contributing guide documents secret-free local build, test, and lint workflow`() {
        val contributing = File(repoRoot(), "CONTRIBUTING.md")
        assertTrue("CONTRIBUTING.md must exist at repository root (T-01.1.2)", contributing.exists())
        val content = contributing.readText()
        assertTrue(
            "CONTRIBUTING.md must document googleServices.missing.passthrough",
            content.contains("googleServices.missing.passthrough")
        )
        assertTrue("CONTRIBUTING.md must document assembleDebug", content.contains(":app:assembleDebug"))
        assertTrue("CONTRIBUTING.md must document testDebugUnitTest", content.contains(":app:testDebugUnitTest"))
        assertTrue("CONTRIBUTING.md must document lintDebug", content.contains(":app:lintDebug"))
        assertTrue("CONTRIBUTING.md must document minSdk 26", content.contains("26"))
    }
}
