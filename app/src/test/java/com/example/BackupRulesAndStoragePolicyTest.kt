package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.media.AudioRecorderManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Enforces Stage B Sprint 2 Task T-04.2.2 (ADR-015, D-23, SEC-REQ-16, SEC-REQ-17, GAP-04, GAP-20):
 * - Backup and data extraction XML rules enforce strict include-only semantics for cloud backup and D2D transfer.
 * - Sensitive databases (`sensitive.db`) and audio recordings (`recordings`) are explicitly excluded.
 * - `AudioRecorderManager.recordingsDir()` resides strictly under `context.noBackupFilesDir`.
 * - Merged manifest links both `dataExtractionRules` and `fullBackupContent` with zero external storage permissions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRulesAndStoragePolicyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, ".github").exists()) cwd else cwd.parentFile ?: cwd
    }

    @Test
    fun `data_extraction_rules enforces include-only learning db and preferences and excludes sensitive items`() {
        val file = File(repoRoot(), "app/src/main/res/xml/data_extraction_rules.xml")
        assertTrue("data_extraction_rules.xml must exist", file.exists())
        val xml = file.readText()

        assertTrue("Must declare cloud-backup section", xml.contains("<cloud-backup>"))
        assertTrue("Must declare device-transfer section", xml.contains("<device-transfer>"))

        // Cloud-backup includes and excludes
        assertTrue("Cloud backup must include learning.db", xml.contains("""<include domain="database" path="learning.db""""))
        assertTrue("Cloud backup must include datastore preferences", xml.contains("""<include domain="file" path="datastore/settings.preferences_pb""""))
        assertTrue("Cloud backup must include speakcraft_prefs", xml.contains("""<include domain="file" path="datastore/speakcraft_prefs.preferences_pb""""))
        assertTrue("Cloud backup must explicitly exclude sensitive.db", xml.contains("""<exclude domain="database" path="sensitive.db""""))
        assertTrue("Cloud backup must explicitly exclude recordings", xml.contains("""<exclude domain="file" path="recordings""""))

        // Device-transfer includes and excludes
        assertTrue("Device transfer must include learning.db", xml.contains("""<include domain="database" path="learning.db""""))
        assertTrue("Device transfer must explicitly exclude sensitive.db", xml.contains("""<exclude domain="database" path="sensitive.db""""))
        assertTrue("Device transfer must explicitly exclude recordings", xml.contains("""<exclude domain="file" path="recordings""""))

        // Prohibited inclusions
        assertFalse("Must never include recordings in backup", xml.contains("""<include domain="file" path="recordings""""))
        assertFalse("Must never include sensitive.db in backup", xml.contains("""<include domain="database" path="sensitive.db""""))
    }

    @Test
    fun `backup_rules for Android 11 and lower enforces include-only and excludes sensitive items`() {
        val file = File(repoRoot(), "app/src/main/res/xml/backup_rules.xml")
        assertTrue("backup_rules.xml must exist", file.exists())
        val xml = file.readText()

        assertTrue("Must declare full-backup-content", xml.contains("<full-backup-content>"))
        assertTrue("Full backup must include learning.db", xml.contains("""<include domain="database" path="learning.db""""))
        assertTrue("Full backup must include settings preferences", xml.contains("""<include domain="file" path="datastore/settings.preferences_pb""""))
        assertTrue("Full backup must include speakcraft preferences", xml.contains("""<include domain="file" path="datastore/speakcraft_prefs.preferences_pb""""))
        assertTrue("Full backup must explicitly exclude sensitive.db", xml.contains("""<exclude domain="database" path="sensitive.db""""))
        assertTrue("Full backup must explicitly exclude recordings", xml.contains("""<exclude domain="file" path="recordings""""))

        assertFalse("Must not include recordings", xml.contains("""<include domain="file" path="recordings""""))
        assertFalse("Must not include sensitive.db", xml.contains("""<include domain="database" path="sensitive.db""""))
    }

    @Test
    fun `AudioRecorderManager stores recordings strictly inside noBackupFilesDir`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AudioRecorderManager(context)

        val dir = manager.recordingsDir()
        assertTrue("Recordings directory must exist or be created", dir.exists())
        assertTrue(
            "Recordings directory (${dir.absolutePath}) must reside inside noBackupFilesDir (${context.noBackupFilesDir.absolutePath})",
            dir.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath)
        )
        assertFalse(
            "Recordings directory must not reside under filesDir (which is subject to backup)",
            dir.canonicalPath.startsWith(context.filesDir.canonicalPath)
        )
        assertEquals("recordings", dir.name)
    }

    @Test
    fun `AndroidManifest declares backup rules and zero external storage permissions`() {
        val manifest = File(repoRoot(), "app/src/main/AndroidManifest.xml").readText()

        assertTrue("Manifest must declare allowBackup", manifest.contains("""android:allowBackup="true""""))
        assertTrue("Manifest must link dataExtractionRules", manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        assertTrue("Manifest must link fullBackupContent", manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))

        assertFalse("Manifest must not request READ_EXTERNAL_STORAGE", manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse("Manifest must not request WRITE_EXTERNAL_STORAGE", manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse("Manifest must not request MANAGE_EXTERNAL_STORAGE", manifest.contains("MANAGE_EXTERNAL_STORAGE"))
    }
}
