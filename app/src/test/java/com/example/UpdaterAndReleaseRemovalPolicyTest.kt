package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Policy and regression suite enforcing Sprint 2 Task 2 (T-04.1.3 / ADR-004 / D-03):
 * - In-app APK updater, GitHub release client, backup manager, update screens/dialogs, and
 *   Firestore release publish script are completely removed.
 * - Zero APK install intents, REQUEST_INSTALL_PACKAGES permissions, or GitHub Releases API calls
 *   exist anywhere in app/src/main.
 * - Firestore rules and client repositories no longer expose applicationReleases or
 *   applicationUpdateEvents collections.
 * - CI security-static Check #7 enforces ongoing absence.
 */
class UpdaterAndReleaseRemovalPolicyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, ".github").exists()) cwd else cwd.parentFile ?: cwd
    }

    @Test
    fun `retired updater classes, screens, dialogs, FileProvider paths, and publish script are deleted`() {
        val root = repoRoot()
        val deletedPaths = listOf(
            "app/src/main/java/com/example/data/update",
            "app/src/main/java/com/example/data/update/GitHubReleaseClient.kt",
            "app/src/main/java/com/example/data/update/ReleaseUpdateRepository.kt",
            "app/src/main/java/com/example/data/update/UpdateBackupManager.kt",
            "app/src/main/java/com/example/data/update/ApkIntegrityVerifier.kt",
            "app/src/main/java/com/example/data/update/SemVer.kt",
            "app/src/main/java/com/example/ui/screens/AppUpdateScreen.kt",
            "app/src/main/java/com/example/ui/components/UpdateAvailableDialog.kt",
            "app/src/main/res/xml/file_paths.xml",
            "scripts/publish_firestore_release.mjs",
            "scripts/publish_firestore_release.test.mjs"
        )
        deletedPaths.forEach { rel ->
            assertFalse("Retired artifact must stay deleted: $rel", File(root, rel).exists())
        }
    }

    @Test
    fun `app main source and manifest contain zero install intents, GitHub Releases API calls, or updater symbols`() {
        val mainDir = File(repoRoot(), "app/src/main")
        assertTrue("app/src/main must exist", mainDir.exists())

        val forbiddenPattern = Regex(
            """REQUEST_INSTALL_PACKAGES|ACTION_INSTALL_PACKAGE|vnd\.android\.package-archive|api\.github\.com/repos|GitHubReleaseClient|ReleaseUpdateRepository|UpdateBackupManager|AppUpdateScreen|UpdateAvailableDialog|ApplicationRelease|ApplicationUpdateEvent|applicationReleases|applicationUpdateEvents"""
        )

        val violations = mutableListOf<String>()
        mainDir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .forEach { file ->
                val text = file.readText()
                val match = forbiddenPattern.find(text)
                if (match != null) {
                    violations += "${file.relativeTo(repoRoot()).path}: matched '${match.value}'"
                }
            }

        assertTrue(
            "Found prohibited in-app updater / install-intent / release-collection references:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `firestore rules remove applicationReleases and applicationUpdateEvents so they default-deny`() {
        val rules = File(repoRoot(), "firestore.rules").readText()
        assertFalse("firestore.rules must not define applicationReleases rules", rules.contains("applicationReleases"))
        assertFalse("firestore.rules must not define applicationUpdateEvents rules", rules.contains("applicationUpdateEvents"))
        assertFalse("firestore.rules must not retain unused isValidRelease helper", rules.contains("isValidRelease"))
        assertFalse("firestore.rules must not retain unused isValidUpdateEvent helper", rules.contains("isValidUpdateEvent"))

        val rulesTest = File(repoRoot(), "firestore.test.js").readText()
        assertTrue(
            "firestore.test.js must verify retired applicationReleases is denied for admin",
            rulesTest.contains("""assertFails(adminDb.collection("applicationReleases")""")
        )
        assertTrue(
            "firestore.test.js must verify retired applicationUpdateEvents is denied for admin",
            rulesTest.contains("""assertFails(adminDb.collection("applicationUpdateEvents")""")
        )
    }

    @Test
    fun `build config removes GITHUB_RELEASE_REPO and ci workflow enforces ADR-004 check`() {
        val root = repoRoot()
        val appGradle = File(root, "app/build.gradle.kts").readText()
        assertFalse("app/build.gradle.kts must not emit GITHUB_RELEASE_REPO BuildConfig", appGradle.contains("GITHUB_RELEASE_REPO"))

        val manifest = File(root, "app/src/main/AndroidManifest.xml").readText()
        assertFalse("AndroidManifest.xml must not request REQUEST_INSTALL_PACKAGES", manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse("AndroidManifest.xml must not retain unused updater FileProvider", manifest.contains("FileProvider"))

        val ciYml = File(root, ".github/workflows/ci.yml").readText()
        assertTrue("ci.yml must enforce ADR-004 check for publish script", ciYml.contains("scripts/publish_firestore_release.mjs"))
        assertTrue("ci.yml must enforce ADR-004 check for install intents and GitHub API", ciYml.contains("REQUEST_INSTALL_PACKAGES") && ciYml.contains("api\\.github\\.com/repos"))
        assertTrue("ci.yml must enforce ADR-004 check for retired release collections", ciYml.contains("applicationReleases") && ciYml.contains("applicationUpdateEvents"))
    }
}
