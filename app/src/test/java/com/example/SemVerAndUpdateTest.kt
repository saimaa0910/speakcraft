package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemePreference
import com.example.data.update.ApkIntegrityVerifier
import com.example.data.update.SemVer
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
}
