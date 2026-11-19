package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemePreference
import com.example.data.update.ApkIntegrityVerifier
import com.example.data.update.SemVer
import com.example.data.update.SignatureCompatibility
import com.example.data.update.UpdateBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SemVerAndUpdateTest {

    @Test
    fun `semver parses and compares versions including pre-releases`() {
        val v100 = SemVer.parse("1.0.0")!!
        val v110 = SemVer.parse("v1.1.0")!!
        val v110Beta = SemVer.parse("1.1.0-beta.1")!!

        assertEquals(SemVer(1, 0, 0), v100)
        assertTrue(v110 > v100)
        assertTrue(v110 > v110Beta)
        assertTrue(v110Beta > v100)
        assertTrue(SemVer.isNewer("1.2.0", "1.0.0"))
        assertFalse(SemVer.isNewer("1.0.0", "1.0.0"))
        assertTrue(SemVer.isBelowMinimum("1.0.0", "1.1.0"))
        assertFalse(SemVer.isBelowMinimum("1.1.0", "1.1.0"))
    }

    @Test
    fun `sha256 verifier accepts valid checksum and rejects tampered file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "test_release_artifact.bin")
        tempFile.writeText("SpeakCraft verified APK payload v1.1.0")

        val sha = ApkIntegrityVerifier.computeSha256(tempFile)
        assertEquals(64, sha.length)
        assertTrue(ApkIntegrityVerifier.verifySha256(tempFile, sha))

        // Tamper file content
        tempFile.writeText("Tampered payload")
        assertFalse(ApkIntegrityVerifier.verifySha256(tempFile, sha))
        assertFalse(ApkIntegrityVerifier.verifySha256(tempFile, "not-a-valid-sha"))
        tempFile.delete()
    }

    @Test
    fun `backup manager creates pre-update snapshot and detects interrupted update`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = UpdateBackupManager(context)
        manager.clearRecoveryMarker()

        val prefs = AppPreferences(
            themeMode = ThemePreference.DARK,
            ttsRate = 1.05f,
            updateChannel = "beta"
        )
        val result = manager.createPreUpdateBackup(
            prefs = prefs,
            currentVersionName = "1.0.0",
            currentVersionCode = 1L,
            targetVersionName = "1.2.0"
        )
        assertTrue(result.isSuccess)
        val snap = result.getOrThrow()
        assertTrue(snap.fileName.startsWith("speakcraft_backup_1.0.0_"))
        assertTrue(manager.listBackups().any { it.fileName == snap.fileName })

        // Still running v1.0.0 -> interrupted warning appears
        val warning = manager.checkInterruptedUpdateMarker("1.0.0")
        assertNotNull(warning)
        assertTrue(warning!!.contains("v1.0.0"))
        assertTrue(warning.contains("v1.2.0"))

        // Once running target v1.2.0 -> marker clears cleanly
        assertNull(manager.checkInterruptedUpdateMarker("1.2.0"))
    }

    @Test
    fun `release evaluation respects stable vs beta channel and rejects non-https download url`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = UpdateBackupManager(context)
        val repo = com.example.data.update.ReleaseUpdateRepository(context, manager)

        val stableRelease = com.example.data.model.ApplicationRelease(
            id = "1_1_0",
            versionCode = 2L,
            versionName = "1.1.0",
            channel = "stable",
            downloadUrl = "https://example.com/speakcraft-1.1.0.apk",
            sha256Checksum = "a".repeat(64),
            enabled = true
        )
        val betaRelease = com.example.data.model.ApplicationRelease(
            id = "1_2_0_beta_1",
            versionCode = 3L,
            versionName = "1.2.0-beta.1",
            channel = "beta",
            downloadUrl = "http://insecure.example.com/speakcraft-beta.apk",
            sha256Checksum = "b".repeat(64),
            enabled = true
        )
        val disabledHigherRelease = com.example.data.model.ApplicationRelease(
            id = "2_0_0",
            versionCode = 10L,
            versionName = "2.0.0",
            channel = "stable",
            downloadUrl = "https://example.com/speakcraft-2.0.0.apk",
            sha256Checksum = "c".repeat(64),
            enabled = false
        )

        val all = listOf(stableRelease, betaRelease, disabledHigherRelease)

        val stableEval = repo.evaluateReleases(all, "stable")
        assertTrue(stableEval.isUpdateAvailable)
        assertEquals("1.1.0", stableEval.latestRelease?.versionName)

        val betaEval = repo.evaluateReleases(all, "beta")
        assertTrue(betaEval.isUpdateAvailable)
        assertEquals("1.2.0-beta.1", betaEval.latestRelease?.versionName)

        // Attempting download on HTTP (non-HTTPS) must fail safely
        val dlResult = repo.downloadAndVerifyRelease(betaRelease, AppPreferences(updateChannel = "beta"))
        assertTrue(dlResult.isFailure)
        assertTrue(repo.downloadState.value is com.example.data.update.UpdateDownloadState.Failed)
    }

    @Test
    fun `previous apk 1_0_0 receives 1_1_0-beta_1 only through beta channel and current build is 1_1_0-beta_1`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = UpdateBackupManager(context)
        val repo = com.example.data.update.ReleaseUpdateRepository(context, manager)

        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("1.1.0-beta.1", BuildConfig.VERSION_NAME)
        assertEquals("beta", BuildConfig.BUILD_CHANNEL)

        val currentBetaRelease = com.example.data.model.ApplicationRelease(
            id = "rel_1_1_0_beta_1_2",
            versionCode = 2L,
            versionName = "1.1.0-beta.1",
            channel = "beta",
            downloadUrl = "https://example.com/speakcraft_1.1.0-beta.1.apk",
            sha256Checksum = "d".repeat(64),
            minSupportedVersion = "1.0.0",
            enabled = true
        )

        // Simulating previous APK v1.0.0 (build 1) checking Stable channel -> beta update excluded, prompt mentions Beta
        val prevOnStable = repo.evaluateReleases(
            releases = listOf(currentBetaRelease),
            channel = "stable",
            currentName = "1.0.0",
            currentCode = 1L
        )
        assertFalse(prevOnStable.isUpdateAvailable)
        assertTrue(prevOnStable.statusSummary.contains("Beta", ignoreCase = true))

        // Simulating previous APK v1.0.0 (build 1) checking Beta channel -> v1.1.0-beta.1 offered cleanly
        val prevOnBeta = repo.evaluateReleases(
            releases = listOf(currentBetaRelease),
            channel = "beta",
            currentName = "1.0.0",
            currentCode = 1L
        )
        assertTrue(prevOnBeta.isUpdateAvailable)
        assertEquals("1.1.0-beta.1", prevOnBeta.latestRelease?.versionName)
        assertEquals("beta", prevOnBeta.latestRelease?.channel)

        // Current installed v1.1.0-beta.1 (build 2) checking Beta channel -> already up to date
        val currentOnBeta = repo.evaluateReleases(listOf(currentBetaRelease), "beta")
        assertFalse(currentOnBeta.isUpdateAvailable)
    }

    @Test
    fun `semver orders multi-digit beta pre-releases numerically`() {
        val beta2 = SemVer.parse("1.1.0-beta.2")!!
        val beta10 = SemVer.parse("1.1.0-beta.10")!!
        assertTrue(beta10 > beta2)
        assertTrue(SemVer.isNewer("1.1.0-beta.10", "1.1.0-beta.2"))
    }

    @Test
    fun `github release client parses apk asset, sha256, channel and merges with firestore releases`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = UpdateBackupManager(context)
        val repo = com.example.data.update.ReleaseUpdateRepository(context, manager)

        assertEquals(
            "octocat/speakcraft",
            com.example.data.update.GitHubReleaseClient.normalizeRepoSlug("https://github.com/octocat/speakcraft.git")
        )

        val sha = "e".repeat(64)
        val json = """
            [
              {
                "tag_name": "v1.1.0-beta.2-build.3",
                "name": "SpeakCraft v1.1.0-beta.2",
                "draft": false,
                "prerelease": true,
                "published_at": "2026-11-18T10:00:00Z",
                "body": "VersionCode: 3\nVersionName: 1.1.0-beta.2\nChannel: beta\nMinSupportedVersion: 1.0.0\nSHA256: $sha\n- Upgraded launch animation\n- Beta OTA check",
                "assets": [
                  {
                    "name": "speakcraft_1.1.0-beta.2.apk",
                    "size": 12345678,
                    "browser_download_url": "https://github.com/octocat/speakcraft/releases/download/v1.1.0-beta.2-build.3/speakcraft_1.1.0-beta.2.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val ghReleases = com.example.data.update.GitHubReleaseClient.parseReleases(json)
        assertEquals(1, ghReleases.size)
        val parsed = ghReleases.first()
        assertEquals("1.1.0-beta.2", parsed.versionName)
        assertEquals(3L, parsed.versionCode)
        assertEquals("beta", parsed.channel)
        assertEquals(sha, parsed.sha256Checksum)
        assertEquals(2, parsed.features.size)

        val merged = repo.mergeReleases(firestoreReleases = emptyList(), githubReleases = ghReleases)
        val evalFromV100 = repo.evaluateReleases(merged, "beta", currentName = "1.0.0", currentCode = 1L)
        assertTrue(evalFromV100.isUpdateAvailable)
        assertEquals("1.1.0-beta.2", evalFromV100.latestRelease?.versionName)
    }

    @Test
    fun `certificate compatibility detects debug vs release signing conflict even when expected cert matches release`() {
        val debugCert = "3f7d14b32df7635fe1b48db5d09857df02a000e2204568323d9a60025fa75f9f"
        val releaseCert = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"
        val tamperedCert = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        // Matching installed & candidate certs -> Compatible
        val sameKey = ApkIntegrityVerifier.evaluateCertificateCompatibility(
            installedCerts = listOf(releaseCert),
            candidateCerts = listOf(releaseCert.uppercase()),
            expectedCertSha256 = releaseCert
        )
        assertTrue(sameKey is SignatureCompatibility.Compatible)

        // Installed app signed with debug keystore, downloaded release APK signed with upload key
        // Even though expectedCertSha256 matches releaseCert, Android PackageManager will reject with
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE -> must return MismatchWithInstalled!
        val conflict = ApkIntegrityVerifier.evaluateCertificateCompatibility(
            installedCerts = listOf(debugCert),
            candidateCerts = listOf(releaseCert),
            expectedCertSha256 = releaseCert
        )
        assertTrue(conflict is SignatureCompatibility.MismatchWithInstalled)
        val mismatch = conflict as SignatureCompatibility.MismatchWithInstalled
        assertEquals(debugCert, mismatch.installedCertSha256)
        assertEquals(releaseCert, mismatch.targetCertSha256)

        // Tampered candidate cert does not match expected metadata cert -> MismatchWithExpected
        val tampered = ApkIntegrityVerifier.evaluateCertificateCompatibility(
            installedCerts = listOf(releaseCert),
            candidateCerts = listOf(tamperedCert),
            expectedCertSha256 = releaseCert
        )
        assertTrue(tampered is SignatureCompatibility.MismatchWithExpected)

        assertEquals("3F7D14B3…5FA75F9F", ApkIntegrityVerifier.formatShortFingerprint(debugCert))
    }
}


