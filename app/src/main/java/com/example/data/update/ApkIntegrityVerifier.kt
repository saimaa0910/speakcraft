package com.example.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

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
        val expected = expectedSha256.trim().lowercase()
        if (expected.length != 64 || !expected.all { it in '0'..'9' || it in 'a'..'f' }) {
            return false
        }
        val actual = computeSha256(file).lowercase()
        return constantTimeEquals(actual, expected)
    }

    fun verifyDownloadedApkSignature(context: Context, apkFile: File, expectedCertSha256: String = ""): Boolean {
        if (!apkFile.exists()) return false
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            val installedCerts = getInstalledAppSigningCertSha256(context)
            val apkCerts = extractCertSha256List(pkgInfo)
            if (apkCerts.isEmpty()) return false

            if (expectedCertSha256.isNotBlank()) {
                val target = expectedCertSha256.trim().lowercase()
                apkCerts.any { constantTimeEquals(it.lowercase(), target) }
            } else if (installedCerts.isNotEmpty()) {
                apkCerts.any { cert -> installedCerts.any { constantTimeEquals(it, cert) } }
            } else {
                true
            }
        } catch (_: Exception) {
            false
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
