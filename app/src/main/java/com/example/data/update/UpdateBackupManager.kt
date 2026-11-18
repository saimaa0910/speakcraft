package com.example.data.update

import android.content.Context
import com.example.data.preferences.AppPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupSnapshotInfo(
    val fileName: String,
    val createdAtMs: Long,
    val appVersionName: String,
    val appVersionCode: Long,
    val recordingsCount: Int,
    val sizeBytes: Long
)

class UpdateBackupManager(private val context: Context) {

    fun backupsDir(): File = File(context.filesDir, "backups").apply { mkdirs() }
    private fun recoveryMarkerFile(): File = File(backupsDir(), "pending_update_marker.json")

    fun createPreUpdateBackup(
        prefs: AppPreferences,
        currentVersionName: String,
        currentVersionCode: Long,
        targetVersionName: String
    ): Result<BackupSnapshotInfo> = runCatching {
        val recordingsDir = File(context.filesDir, "recordings")
        val recordings = recordingsDir.listFiles()?.map { it.name }.orEmpty()
        val now = System.currentTimeMillis()
        val fileName = "speakcraft_backup_${currentVersionName}_$now.json"
        val outFile = File(backupsDir(), fileName)

        val json = JSONObject().apply {
            put("createdAtMs", now)
            put("appVersionName", currentVersionName)
            put("appVersionCode", currentVersionCode)
            put("targetVersionName", targetVersionName)
            put("themeMode", prefs.themeMode.name)
            put("ttsRate", prefs.ttsRate.toDouble())
            put("updateChannel", prefs.updateChannel)
            put("recordingsFiles", JSONArray(recordings))
        }
        outFile.writeText(json.toString(2))

        // Write rollback recovery marker before handing off to installer
        recoveryMarkerFile().writeText(
            JSONObject().apply {
                put("backupFile", outFile.absolutePath)
                put("fromVersion", currentVersionName)
                put("targetVersion", targetVersionName)
                put("startedAtMs", now)
            }.toString()
        )

        BackupSnapshotInfo(
            fileName = outFile.name,
            createdAtMs = now,
            appVersionName = currentVersionName,
            appVersionCode = currentVersionCode,
            recordingsCount = recordings.size,
            sizeBytes = outFile.length()
        )
    }

    fun listBackups(): List<BackupSnapshotInfo> {
        val files = backupsDir().listFiles { _, name ->
            name.startsWith("speakcraft_backup_") && name.endsWith(".json")
        } ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.mapNotNull { file ->
            runCatching {
                val json = JSONObject(file.readText())
                val arr = json.optJSONArray("recordingsFiles") ?: JSONArray()
                BackupSnapshotInfo(
                    fileName = file.name,
                    createdAtMs = json.optLong("createdAtMs", file.lastModified()),
                    appVersionName = json.optString("appVersionName", "1.0.0"),
                    appVersionCode = json.optLong("appVersionCode", 1L),
                    recordingsCount = arr.length(),
                    sizeBytes = file.length()
                )
            }.getOrNull()
        }
    }

    fun checkInterruptedUpdateMarker(currentVersionName: String): String? {
        val marker = recoveryMarkerFile()
        if (!marker.exists()) return null
        return runCatching {
            val json = JSONObject(marker.readText())
            val fromVer = json.optString("fromVersion")
            val targetVer = json.optString("targetVersion")
            if (targetVer.isNotBlank() && SemVer.parse(currentVersionName) == SemVer.parse(targetVer)) {
                marker.delete()
                null
            } else {
                "Previous update from v$fromVer to v$targetVer was not completed. Your local backup and Firestore cloud progress remain intact."
            }
        }.getOrNull()
    }

    fun clearRecoveryMarker() {
        runCatching { recoveryMarkerFile().delete() }
    }

    fun readBackupSummary(fileName: String): String {
        val target = File(backupsDir(), fileName)
        if (!target.exists()) return "Backup not found."
        return runCatching {
            val json = JSONObject(target.readText())
            "Backup v${json.optString("appVersionName")} • Channel: ${json.optString("updateChannel")} • Recordings indexed: ${json.optJSONArray("recordingsFiles")?.length() ?: 0}"
        }.getOrElse { "Unable to parse backup snapshot." }
    }
}
