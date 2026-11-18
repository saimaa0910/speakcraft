package com.example.data.update

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String = ""
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        // Stable release (empty preRelease) is newer than a pre-release on same X.Y.Z
        if (preRelease.isBlank() && other.preRelease.isNotBlank()) return 1
        if (preRelease.isNotBlank() && other.preRelease.isBlank()) return -1
        return preRelease.compareTo(other.preRelease, ignoreCase = true)
    }

    override fun toString(): String =
        if (preRelease.isBlank()) "$major.$minor.$patch" else "$major.$minor.$patch-$preRelease"

    companion object {
        fun parse(raw: String): SemVer? {
            val cleaned = raw.trim().removePrefix("v").removePrefix("V")
            if (cleaned.isBlank()) return null
            val dashParts = cleaned.split("-", limit = 2)
            val core = dashParts[0].split(".")
            if (core.isEmpty()) return null
            val major = core.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = core.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = core.getOrNull(2)?.toIntOrNull() ?: 0
            val pre = dashParts.getOrNull(1).orEmpty()
            return SemVer(major, minor, patch, pre)
        }

        fun isNewer(candidateVersion: String, currentVersion: String): Boolean {
            val candidate = parse(candidateVersion) ?: return false
            val current = parse(currentVersion) ?: return false
            return candidate > current
        }

        fun isBelowMinimum(currentVersion: String, minSupportedVersion: String): Boolean {
            val current = parse(currentVersion) ?: return false
            val minVer = parse(minSupportedVersion) ?: return false
            return current < minVer
        }
    }
}
