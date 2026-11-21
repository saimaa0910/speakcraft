package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies Stage B Sprint 1 Task T-01.3.1 (ENG-01 §4.4, FND-SEC-02, ADR-004):
 * - Legacy push-to-release workflow is removed.
 * - Release workflow is strictly tag-driven (no branch push triggers).
 * - Release workflow builds only release-signed artefacts with zero debug keystore fallback.
 * - Release workflow runs unit tests before building and cleans up credentials in always().
 */
class ReleaseWorkflowPolicyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, ".github").exists()) cwd else cwd.parentFile ?: cwd
    }

    private fun workflowsDir(): File = File(repoRoot(), ".github/workflows")

    @Test
    fun `legacy release_on_push workflow is removed`() {
        val legacy = File(workflowsDir(), "release_on_push.yml")
        assertFalse(
            "Legacy release_on_push.yml must be deleted so pushes to main/beta never publish releases",
            legacy.exists()
        )
    }

    @Test
    fun `release workflow triggers strictly on v-semver tags and never on branch push`() {
        val workflow = File(workflowsDir(), "release.yml")
        assertTrue("release.yml must exist under .github/workflows/", workflow.exists())
        val content = workflow.readText()

        assertTrue("release.yml must trigger on v*.*.* tags", content.contains("\"v*.*.*\""))
        assertFalse("release.yml must not trigger on branch pushes", Regex("""(?m)^\s*branches:\s*$""").containsMatchIn(content))
        assertFalse("release.yml must not expose workflow_dispatch debug signing", content.contains("signing_mode"))
    }

    @Test
    fun `release workflow enforces release-only signing with zero debug fallback and no OTA Firestore publish`() {
        val content = File(workflowsDir(), "release.yml").readText()

        assertTrue("release.yml must build release APK and AAB", content.contains(":app:assembleRelease :app:bundleRelease"))
        assertTrue("release.yml must verify APK signature", content.contains("apksigner") && content.contains("verify --print-certs"))
        assertTrue("release.yml must guard against debuggable release APK", content.contains("application-debuggable"))
        assertTrue("release.yml must run unit tests before release packaging", content.contains(":app:testDebugUnitTest"))
        assertTrue("release.yml must create draft releases first", content.contains("draft: true"))
        assertTrue("release.yml must use least-privilege workflow permissions", content.contains("permissions:\n  contents: read"))
        assertTrue("release.yml must clean up secrets in always()", content.contains("if: always()") && content.contains("rm -f"))

        assertFalse("release.yml must never reference DEBUG_KEYSTORE_BASE64", content.contains("DEBUG_KEYSTORE_BASE64"))
        assertFalse("release.yml must never fall back to assembleDebug", content.contains("assembleDebug"))
        assertFalse("release.yml must never reference debug.keystore", content.contains("debug.keystore"))
        assertFalse("release.yml must not install unpinned firebase-admin or publish applicationReleases", content.contains("firebase-admin"))
        assertFalse("release.yml must not publish to retired applicationReleases", content.contains("applicationReleases"))
    }
}
