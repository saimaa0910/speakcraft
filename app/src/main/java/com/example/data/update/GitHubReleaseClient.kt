package com.example.data.update

import com.example.data.model.ApplicationRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class GitHubReleaseClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchReleases(repoInput: String): Result<List<ApplicationRelease>> = withContext(Dispatchers.IO) {
        runCatching {
            val slug = normalizeRepoSlug(repoInput)
            if (slug.isBlank()) return@runCatching emptyList()

            val apiUrl = "https://api.github.com/repos/$slug/releases?per_page=15"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SpeakCraft-Android-Updater")
                .build()

            val rawJson = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("GitHub releases check failed (HTTP ${response.code}).")
                }
                response.body?.string().orEmpty()
            }

            val parsed = parseReleasesJson(rawJson)
            // Backfill SHA-256 from sidecar .sha256 asset if not inline in release notes body
            parsed.map { item ->
                if (item.release.sha256Checksum.length == 64 || item.shaSidecarUrl.isBlank()) {
                    item.release
                } else {
                    val sidecarSha = fetchSidecarSha256(item.shaSidecarUrl)
                    if (sidecarSha.length == 64) {
                        item.release.copy(sha256Checksum = sidecarSha)
                    } else {
                        item.release
                    }
                }
            }.filter { it.sha256Checksum.length == 64 && it.downloadUrl.startsWith("https://") }
        }
    }

    private fun fetchSidecarSha256(url: String): String = runCatching {
        if (!url.startsWith("https://")) return ""
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SpeakCraft-Android-Updater")
            .build()
        val text = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) "" else resp.body?.string().orEmpty()
        }
        Regex("""\b([a-fA-F0-9]{64})\b""").find(text)?.groupValues?.get(1)?.lowercase().orEmpty()
    }.getOrDefault("")

    private data class ParsedReleaseEntry(
        val release: ApplicationRelease,
        val shaSidecarUrl: String
    )

    companion object {
        fun normalizeRepoSlug(raw: String): String {
            val trimmed = raw.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removeSuffix(".git")
                .trim('/')
            val parts = trimmed.split("/").filter { it.isNotBlank() }
            if (parts.size < 2) return ""
            val candidate = "${parts[0]}/${parts[1]}"
            return if (candidate.matches(Regex("^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$"))) candidate else ""
        }

        fun parseReleases(rawJson: String): List<ApplicationRelease> =
            parseReleasesJson(rawJson).map { it.release }

        private fun parseReleasesJson(rawJson: String): List<ParsedReleaseEntry> {
            if (rawJson.isBlank()) return emptyList()
            val array = JSONArray(rawJson)
            val entries = mutableListOf<ParsedReleaseEntry>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optBoolean("draft", false)) continue

                val tagName = obj.optString("tag_name").trim()
                val body = obj.optString("body").trim()
                val prerelease = obj.optBoolean("prerelease", false)
                val publishedAt = obj.optString("published_at").take(10)
                val assets = obj.optJSONArray("assets") ?: continue

                var apkUrl = ""
                var apkName = ""
                var apkSize = 0L
                var shaSidecarUrl = ""

                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j) ?: continue
                    val name = asset.optString("name").trim()
                    val dlUrl = asset.optString("browser_download_url").trim()
                    if (!dlUrl.startsWith("https://")) continue
                    if (name.endsWith(".apk", ignoreCase = true) && apkUrl.isBlank()) {
                        apkUrl = dlUrl
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                    } else if (name.endsWith(".sha256", ignoreCase = true) && shaSidecarUrl.isBlank()) {
                        shaSidecarUrl = dlUrl
                    }
                }
                if (apkUrl.isBlank()) continue

                val cleanVersion = extractVersionName(tagName, body) ?: continue
                val versionCode = extractVersionCode(tagName, body)
                val sha256 = extractSha256(body)
                val certSha256 = extractSigningCertSha256(body)
                val minSupported = extractMinSupportedVersion(body)
                val mandatory = Regex("""(?im)^mandatory\s*:\s*true\b""").containsMatchIn(body)
                val channel = extractChannel(body, prerelease, cleanVersion)
                val features = extractFeatureBullets(body)

                entries.add(
                    ParsedReleaseEntry(
                        release = ApplicationRelease(
                            id = "gh_${cleanVersion.replace('.', '_')}_$versionCode",
                            versionName = cleanVersion,
                            versionCode = versionCode,
                            channel = channel,
                            releaseDate = publishedAt.ifBlank { "Latest" },
                            releaseNotes = body.ifBlank { obj.optString("name").ifBlank { "SpeakCraft $cleanVersion" } },
                            features = features,
                            downloadUrl = apkUrl,
                            installerFileName = apkName,
                            fileSizeBytes = apkSize,
                            sha256Checksum = sha256,
                            signingCertSha256 = certSha256,
                            minSupportedVersion = minSupported,
                            minSdk = 24L,
                            mandatory = mandatory,
                            enabled = true
                        ),
                        shaSidecarUrl = shaSidecarUrl
                    )
                )
            }
            return entries
        }

        private fun extractVersionName(tagName: String, body: String): String? {
            val fromBody = Regex("""(?im)^versionname\s*:\s*([0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.]+)?)""")
                .find(body)?.groupValues?.get(1)
            if (!fromBody.isNullOrBlank()) return fromBody

            val cleanedTag = tagName.removePrefix("v").removePrefix("V")
                .replace(Regex("""-build\.\d+$"""), "")
            return if (cleanedTag.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(-[a-zA-Z0-9.]+)?$"))) {
                cleanedTag
            } else {
                null
            }
        }

        private fun extractVersionCode(tagName: String, body: String): Long {
            val fromBody = Regex("""(?im)^versioncode\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toLongOrNull()
            if (fromBody != null && fromBody > 0L) return fromBody
            val fromTag = Regex("""-build\.(\d+)$""").find(tagName)?.groupValues?.get(1)?.toLongOrNull()
            return fromTag ?: 1L
        }

        private fun extractSha256(body: String): String =
            Regex("""(?im)^sha-?256\s*:\s*([a-fA-F0-9]{64})\b""")
                .find(body)?.groupValues?.get(1)?.lowercase().orEmpty()

        private fun extractSigningCertSha256(body: String): String =
            Regex("""(?im)^signingcertsha256\s*:\s*([a-fA-F0-9:]{64,95})\b""")
                .find(body)?.groupValues?.get(1)?.replace(":", "")?.lowercase().orEmpty()

        private fun extractMinSupportedVersion(body: String): String =
            Regex("""(?im)^minsupportedversion\s*:\s*([0-9]+\.[0-9]+\.[0-9]+)\b""")
                .find(body)?.groupValues?.get(1) ?: "1.0.0"

        private fun extractChannel(body: String, prerelease: Boolean, versionName: String): String {
            val explicit = Regex("""(?im)^channel\s*:\s*(stable|beta)\b""").find(body)?.groupValues?.get(1)?.lowercase()
            if (explicit != null) return explicit
            if (prerelease || versionName.contains("-")) return "beta"
            return "stable"
        }

        private fun extractFeatureBullets(body: String): List<String> =
            body.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("* ") }
                .map { it.drop(2).trim() }
                .filter { it.isNotBlank() }
                .take(10)
                .toList()
    }
}
