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
    data class SigningConflict(
        val installedCertSha256: String,
        val releaseCertSha256: String,
        val reason: String
    ) : UpdateDownloadState
    data class Failed(val reason: String) : UpdateDownloadState
}

data class UpdateEvaluation(
    val currentVersionName: String,
    val currentVersionCode: Long,
    val latestRelease: ApplicationRelease?,
    val isUpdateAvailable: Boolean,
    val isMandatory: Boolean,
    val statusSummary: String,
    val hasSigningConflictWithInstalled: Boolean = false,
    val installedCertFingerprint: String = "",
    val releaseCertFingerprint: String = ""
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
        channel: String,
        currentName: String = currentVersionName(),
        currentCode: Long = currentVersionCode()
    ): UpdateEvaluation {
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

        val newestBetaOnly = if (channel != "beta") {
            releases.filter {
                it.enabled &&
                    it.channel == "beta" &&
                    it.minSdk <= android.os.Build.VERSION.SDK_INT.toLong()
            }.maxWithOrNull { a, b ->
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
        } else null

        val hasNewer = newest != null && (
            SemVer.isNewer(newest.versionName, currentName) || newest.versionCode > currentCode
            )
        val hasNewerBeta = newestBetaOnly != null && (
            SemVer.isNewer(newestBetaOnly.versionName, currentName) || newestBetaOnly.versionCode > currentCode
            )
        val forcedByMinVer = newest != null &&
            newest.minSupportedVersion.isNotBlank() &&
            SemVer.isBelowMinimum(currentName, newest.minSupportedVersion)
        val mandatory = hasNewer && ((newest?.mandatory == true) || forcedByMinVer)

        val certCompat = if (hasNewer && newest != null) {
            ApkIntegrityVerifier.checkInstalledCertificateCompatibility(context, newest.signingCertSha256)
        } else {
            SignatureCompatibility.Unknown
        }
        val signingConflict = certCompat as? SignatureCompatibility.MismatchWithInstalled

        val summary = when {
            newest == null && hasNewerBeta ->
                "You are on SpeakCraft v$currentName (build $currentCode). Beta update v${newestBetaOnly!!.versionName} is available — switch to Beta channel to install."
            newest == null ->
                "You are on SpeakCraft v$currentName (build $currentCode). No newer published releases on '$channel' channel."
            hasNewer && signingConflict != null ->
                "Update v${newest.versionName} uses a different signing key than your installed build — a one-time clean reinstall is required."
            hasNewer ->
                "Update available: v${newest.versionName} (build ${newest.versionCode}) on ${newest.channel} channel."
            currentCode > newest.versionCode ->
                "SpeakCraft v$currentName (build $currentCode) is ahead of the latest $channel release (v${newest.versionName})."
            hasNewerBeta ->
                "Latest $channel release v${newest.versionName} installed. Beta v${newestBetaOnly!!.versionName} is available on Beta channel."
            else ->
                "Up to date — SpeakCraft v$currentName (build $currentCode) is the latest $channel release."
        }

        return UpdateEvaluation(
            currentVersionName = currentName,
            currentVersionCode = currentCode,
            latestRelease = newest,
            isUpdateAvailable = hasNewer,
            isMandatory = mandatory,
            statusSummary = summary,
            hasSigningConflictWithInstalled = signingConflict != null,
            installedCertFingerprint = signingConflict?.installedCertSha256.orEmpty(),
            releaseCertFingerprint = signingConflict?.targetCertSha256.orEmpty()
        )
    }

    fun mergeReleases(
        firestoreReleases: List<ApplicationRelease>,
        githubReleases: List<ApplicationRelease>
    ): List<ApplicationRelease> {
        val merged = linkedMapOf<String, ApplicationRelease>()
        (firestoreReleases + githubReleases).forEach { rel ->
            if (rel.enabled && rel.downloadUrl.startsWith("https://")) {
                val normalizedVer = SemVer.parse(rel.versionName)?.toString() ?: rel.versionName.trim()
                val key = "${normalizedVer}_${rel.versionCode}_${rel.channel}"
                merged.putIfAbsent(key, rel)
            }
        }
        return merged.values.toList()
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

            // Pre-flight signature compatibility check so we never waste bandwidth or hit Android's
            // generic INSTALL_FAILED_UPDATE_INCOMPATIBLE package conflict error.
            val preCheck = ApkIntegrityVerifier.checkInstalledCertificateCompatibility(
                context = context,
                releaseCertSha256 = release.signingCertSha256
            )
            if (preCheck is SignatureCompatibility.MismatchWithInstalled) {
                val msg = buildSigningConflictMessage(preCheck.installedCertSha256, preCheck.targetCertSha256)
                _downloadState.value = UpdateDownloadState.SigningConflict(
                    installedCertSha256 = preCheck.installedCertSha256,
                    releaseCertSha256 = preCheck.targetCertSha256,
                    reason = msg
                )
                return@withContext Result.failure(SecurityException(msg))
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
            val compat = ApkIntegrityVerifier.verifyDownloadedApkCompatibility(
                context = context,
                apkFile = targetFile,
                expectedCertSha256 = release.signingCertSha256
            )
            when (compat) {
                is SignatureCompatibility.MismatchWithInstalled -> {
                    targetFile.delete()
                    val msg = buildSigningConflictMessage(compat.installedCertSha256, compat.targetCertSha256)
                    _downloadState.value = UpdateDownloadState.SigningConflict(
                        installedCertSha256 = compat.installedCertSha256,
                        releaseCertSha256 = compat.targetCertSha256,
                        reason = msg
                    )
                    return@withContext Result.failure(SecurityException(msg))
                }
                is SignatureCompatibility.MismatchWithExpected -> {
                    targetFile.delete()
                    val msg = "APK signing certificate does not match the published release metadata. Installation aborted."
                    _downloadState.value = UpdateDownloadState.Failed(msg)
                    return@withContext Result.failure(SecurityException(msg))
                }
                is SignatureCompatibility.Compatible -> {
                    _downloadState.value = UpdateDownloadState.VerifiedReady(
                        apkFile = targetFile,
                        sha256 = actualHash,
                        signatureVerified = true
                    )
                }
                SignatureCompatibility.Unknown -> {
                    if (release.signingCertSha256.isNotBlank()) {
                        targetFile.delete()
                        val msg = "Could not verify APK signing certificate. Installation aborted."
                        _downloadState.value = UpdateDownloadState.Failed(msg)
                        return@withContext Result.failure(SecurityException(msg))
                    }
                    _downloadState.value = UpdateDownloadState.VerifiedReady(
                        apkFile = targetFile,
                        sha256 = actualHash,
                        signatureVerified = false
                    )
                }
            }
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

    fun openDirectDownloadLink(downloadUrl: String): Result<String> = runCatching {
        val safeUrl = downloadUrl.trim()
        require(safeUrl.startsWith("https://")) { "Direct APK download link must use HTTPS." }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
        "Opened direct APK download link in browser."
    }

    fun resetDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    companion object {
        fun buildSigningConflictMessage(installedCertSha256: String, releaseCertSha256: String): String {
            val installedShort = ApkIntegrityVerifier.formatShortFingerprint(installedCertSha256)
            val releaseShort = ApkIntegrityVerifier.formatShortFingerprint(releaseCertSha256)
            return "Signing certificate mismatch detected before install (installed: $installedShort, update: $releaseShort). " +
                "Android prevents overwriting an app signed with a different keystore (for example, switching from a Debug build to a Release build). " +
                "Your progress is safely synced in Firebase — download the APK via browser, uninstall the old build once, and install the new release. Future updates signed with the same key will install in-place."
        }
    }
}
