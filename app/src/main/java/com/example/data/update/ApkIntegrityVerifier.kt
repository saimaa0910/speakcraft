package com.example.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

sealed interface SignatureCompatibility {
    data class Compatible(val certSha256: String) : SignatureCompatibility
    data class MismatchWithInstalled(
        val installedCertSha256: String,
        val targetCertSha256: String
    ) : SignatureCompatibility
    data class MismatchWithExpected(
        val expectedCertSha256: String,
        val actualCertSha256: String
    ) : SignatureCompatibility
    data object Unknown : SignatureCompatibility
}

object ApkIntegrityVerifier {

    fun computeSha256(file: File): String {
        if (!file.exists()) return ""
        return file.inputStream().use { computeSha256(it) }
    }

    fun computeSha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead = input.read(buffer)
        while (bytesRead >= 0) {
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
            }
            bytesRead = input.read(buffer)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifySha256(file: File, expectedSha256: String): Boolean {
        val expected = normalizeHexDigest(expectedSha256)
        if (expected.length != 64 || !expected.all { it in '0'..'9' || it in 'a'..'f' }) {
            return false
        }
        val actual = computeSha256(file).lowercase()
        return constantTimeEquals(actual, expected)
    }

    fun normalizeHexDigest(raw: String): String =
        raw.trim().replace(":", "").lowercase()

    fun isValidSha256Hex(raw: String): Boolean {
        val normalized = normalizeHexDigest(raw)
        return normalized.length == 64 && normalized.all { it in '0'..'9' || it in 'a'..'f' }
    }

    fun formatShortFingerprint(sha256: String): String {
        val normalized = normalizeHexDigest(sha256).uppercase()
        if (normalized.length < 16) return normalized
        return "${normalized.take(8)}…${normalized.takeLast(8)}"
    }

    /**
     * Pure certificate compatibility evaluation between the installed app's signing certificates,
     * a candidate APK's signing certificates (or advertised release certificate), and an optional
     * expected release certificate digest.
     */
    fun evaluateCertificateCompatibility(
        installedCerts: List<String>,
        candidateCerts: List<String>,
        expectedCertSha256: String = ""
    ): SignatureCompatibility {
        val normalizedInstalled = installedCerts.map(::normalizeHexDigest).filter(::isValidSha256Hex)
        val normalizedCandidate = candidateCerts.map(::normalizeHexDigest).filter(::isValidSha256Hex)
        val normalizedExpected = normalizeHexDigest(expectedCertSha256)

        if (normalizedCandidate.isEmpty()) {
            return SignatureCompatibility.Unknown
        }

        // 1. Verify downloaded/candidate APK matches the release metadata's advertised cert if provided.
        if (normalizedExpected.isNotBlank()) {
            if (!isValidSha256Hex(normalizedExpected) ||
                normalizedCandidate.none { constantTimeEquals(it, normalizedExpected) }
            ) {
                return SignatureCompatibility.MismatchWithExpected(
                    expectedCertSha256 = normalizedExpected,
                    actualCertSha256 = normalizedCandidate.first()
                )
            }
        }

        // 2. Always verify against the currently installed app's signing certificate so Android
        // PackageInstaller won't fail with INSTALL_FAILED_UPDATE_INCOMPATIBLE.
        if (normalizedInstalled.isNotEmpty()) {
            val matched = normalizedCandidate.firstOrNull { candidate ->
                normalizedInstalled.any { installed -> constantTimeEquals(installed, candidate) }
            }
            if (matched == null) {
                return SignatureCompatibility.MismatchWithInstalled(
                    installedCertSha256 = normalizedInstalled.first(),
                    targetCertSha256 = normalizedCandidate.first()
                )
            }
            return SignatureCompatibility.Compatible(matched)
        }

        return SignatureCompatibility.Compatible(normalizedCandidate.first())
    }

    /**
     * Pre-download check comparing the release's advertised signing certificate SHA-256 against
     * the currently installed SpeakCraft APK's signing certificate.
     */
    fun checkInstalledCertificateCompatibility(
        context: Context,
        releaseCertSha256: String
    ): SignatureCompatibility {
        val normalizedRelease = normalizeHexDigest(releaseCertSha256)
        if (!isValidSha256Hex(normalizedRelease)) {
            return SignatureCompatibility.Unknown
        }
        val installedCerts = getInstalledAppSigningCertSha256(context)
        if (installedCerts.isEmpty()) {
            return SignatureCompatibility.Unknown
        }
        return evaluateCertificateCompatibility(
            installedCerts = installedCerts,
            candidateCerts = listOf(normalizedRelease),
            expectedCertSha256 = normalizedRelease
        )
    }

    fun verifyDownloadedApkCompatibility(
        context: Context,
        apkFile: File,
        expectedCertSha256: String = ""
    ): SignatureCompatibility {
        if (!apkFile.exists()) return SignatureCompatibility.Unknown
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return SignatureCompatibility.Unknown
            val installedCerts = getInstalledAppSigningCertSha256(context)
            val apkCerts = extractCertSha256List(pkgInfo)
            evaluateCertificateCompatibility(
                installedCerts = installedCerts,
                candidateCerts = apkCerts,
                expectedCertSha256 = expectedCertSha256
            )
        } catch (_: Exception) {
            SignatureCompatibility.Unknown
        }
    }

    fun verifyDownloadedApkSignature(context: Context, apkFile: File, expectedCertSha256: String = ""): Boolean {
        return when (verifyDownloadedApkCompatibility(context, apkFile, expectedCertSha256)) {
            is SignatureCompatibility.Compatible -> true
            else -> false
        }
    }

    fun getInstalledAppSigningCertSha256(context: Context): List<String> {
        return try {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            extractCertSha256List(pkgInfo)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractCertSha256List(pkgInfo: android.content.pm.PackageInfo): List<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
            } else emptyArray()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures ?: emptyArray()
        }
        return signatures.orEmpty().map { sig ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
