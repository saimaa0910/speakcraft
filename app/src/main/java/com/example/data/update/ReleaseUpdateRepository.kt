package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.ApplicationRelease
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "ReleaseUpdateRepo"

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progressFraction: Float, val bytesRead: Long, val totalBytes: Long) : UpdateDownloadState
    data object Verifying : UpdateDownloadState
    data class VerifiedReady(val apkFile: File, val sha256: String, val signatureVerified: Boolean) : UpdateDownloadState
    data class Failed(val reason: String) : UpdateDownloadState
}

data class UpdateEvaluation(
    val currentVersionName: String,
    val currentVersionCode: Long,
    val latestRelease: ApplicationRelease?,
    val isUpdateAvailable: Boolean,
    val isMandatory: Boolean,
    val statusSummary: String
)

class ReleaseUpdateRepository(
    private val context: Context,
    private val backupManager: UpdateBackupManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Long = BuildConfig.VERSION_CODE.toLong()

    fun evaluateReleases(
        releases: List<ApplicationRelease>,
        channel: String
    ): UpdateEvaluation {
        val currentName = currentVersionName()
        val currentCode = currentVersionCode()

        val eligible = releases.filter { rel ->
            rel.enabled &&
                (channel == "beta" || rel.channel == "stable") &&
                rel.minSdk <= android.os.Build.VERSION.SDK_INT.toLong()
        }

        val newest = eligible.maxWithOrNull { a, b ->
            val va = SemVer.parse(a.versionName)
            val vb = SemVer.parse(b.versionName)
            when {
                va != null && vb != null -> {
                    val cmp = va.compareTo(vb)
                    if (cmp != 0) cmp else a.versionCode.compareTo(b.versionCode)
                }
                else -> a.versionCode.compareTo(b.versionCode)
            }
        }

        val hasNewer = newest != null && (
            SemVer.isNewer(newest.versionName, currentName) || newest.versionCode > currentCode
            )
        val forcedByMinVer = newest != null &&
            newest.minSupportedVersion.isNotBlank() &&
            SemVer.isBelowMinimum(currentName, newest.minSupportedVersion)
        val mandatory = hasNewer && ((newest?.mandatory == true) || forcedByMinVer)

        val summary = when {
            newest == null -> "You are on SpeakCraft v$currentName (build $currentCode). No newer published releases on '$channel' channel."
            hasNewer -> "Update available: v${newest.versionName} (build ${newest.versionCode}) on ${newest.channel} channel."
            else -> "Up to date — SpeakCraft v$currentName (build $currentCode) is the latest $channel release."
        }

        return UpdateEvaluation(
            currentVersionName = currentName,
            currentVersionCode = currentCode,
            latestRelease = if (hasNewer) newest else newest,
            isUpdateAvailable = hasNewer,
            isMandatory = mandatory,
            statusSummary = summary
        )
    }

    suspend fun downloadAndVerifyRelease(
        release: ApplicationRelease,
        prefs: AppPreferences
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = release.downloadUrl.trim()
            if (!url.startsWith("https://")) {
                val msg = "Release download URL must use HTTPS for integrity protection."
                _downloadState.value = UpdateDownloadState.Failed(msg)
                return@withContext Result.failure(IllegalArgumentException(msg))
            }

            // Create automatic pre-update snapshot before downloading/installing
            backupManager.createPreUpdateBackup(
                prefs = prefs,
                currentVersionName = currentVersionName(),
                currentVersionCode = currentVersionCode(),
                targetVersionName = release.versionName
            )

            val updatesDir = File(context.filesDir, "updates").apply { mkdirs() }
            val safeFileName = release.installerFileName
                .ifBlank { "speakcraft_${release.versionName}.apk" }
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(updatesDir, safeFileName)
            val tempFile = File(updatesDir, "$safeFileName.part")
            if (tempFile.exists()) tempFile.delete()

            _downloadState.value = UpdateDownloadState.Downloading(0f, 0L, release.fileSizeBytes)

            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed with HTTP ${response.code}."
                    _downloadState.value = UpdateDownloadState.Failed(msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                val body = response.body ?: run {
                    val msg = "Empty release response body."
                    _downloadState.value = UpdateDownloadState.Failed(msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                val total = body.contentLength().takeIf { it > 0L } ?: release.fileSizeBytes.coerceAtLeast(1L)
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesCopied = 0L
                        var read = input.read(buffer)
                        while (read >= 0) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            val fraction = (bytesCopied.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            _downloadState.value = UpdateDownloadState.Downloading(fraction, bytesCopied, total)
                            read = input.read(buffer)
                        }
                        output.flush()
                    }
                }
            }

            _downloadState.value = UpdateDownloadState.Verifying
            if (!ApkIntegrityVerifier.verifySha256(tempFile, release.sha256Checksum)) {
                tempFile.delete()
                val msg = "SHA-256 integrity verification failed. Downloaded file discarded for your safety."
                _downloadState.value = UpdateDownloadState.Failed(msg)
                return@withContext Result.failure(SecurityException(msg))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val actualHash = ApkIntegrityVerifier.computeSha256(targetFile)
            val sigOk = ApkIntegrityVerifier.verifyDownloadedApkSignature(
                context = context,
                apkFile = targetFile,
                expectedCertSha256 = release.signingCertSha256
            )
            if (release.signingCertSha256.isNotBlank() && !sigOk) {
                targetFile.delete()
                val msg = "APK signing certificate mismatch. Installation aborted."
                _downloadState.value = UpdateDownloadState.Failed(msg)
                return@withContext Result.failure(SecurityException(msg))
            }

            _downloadState.value = UpdateDownloadState.VerifiedReady(
                apkFile = targetFile,
                sha256 = actualHash,
                signatureVerified = sigOk
            )
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download/verify error", e)
            val msg = e.message ?: "Unable to download release package."
            _downloadState.value = UpdateDownloadState.Failed(msg)
            Result.failure(e)
        }
    }

    fun launchVerifiedInstallerOrReleasePage(
        apkFile: File?,
        fallbackReleaseUrl: String
    ): Result<String> = runCatching {
        if (apkFile != null && apkFile.exists() && apkFile.name.endsWith(".apk", ignoreCase = true)) {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (installIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(installIntent)
                return@runCatching "Opened system package installer for verified APK."
            }
        }
        // Play-safe browser / Play Store fallback
        val safeUrl = fallbackReleaseUrl.ifBlank { "https://play.google.com/store/apps/details?id=${context.packageName}" }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
        "Opened official release page in browser."
    }

    fun resetDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
    }
}
