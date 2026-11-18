package com.example.data.youtube

import android.util.Log
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

private const val TAG = "YouTubeIntelClient"

data class FetchedYouTubeVideo(
    val videoId: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val description: String,
    val channelHandle: String,
    val channelId: String = "",
    val playlistId: String = "",
    val playlistTitle: String = "",
    val publishedDate: String = "",
    val durationSeconds: Long = 0L,
    val transcript: String? = null,
    val segments: List<TranscriptSegment> = emptyList(),
    val extractionStatus: String,
    val statusDetail: String
)

data class ChannelAuditResult(
    val sourceInput: String,
    val resolvedChannelId: String?,
    val playlistId: String? = null,
    val playlistTitle: String = "",
    val videosFoundCount: Int,
    val videos: List<FetchedYouTubeVideo>,
    val inaccessibleCount: Int,
    val statusNote: String
)

data class RssVideoEntry(
    val videoId: String,
    val title: String,
    val description: String,
    val publishedDate: String = ""
)

class YouTubeIntelligenceClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun inspectUrl(rawUrl: String, youtubeApiKey: String = ""): Result<ChannelAuditResult> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = rawUrl.trim()
                if (trimmed.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Please enter a valid YouTube URL."))
                }

                // 1. Single video URL (watch?v=, youtu.be/, shorts/)
                val singleVideoId = extractSingleVideoId(trimmed)
                val playlistId = extractPlaylistId(trimmed)
                if (singleVideoId != null && playlistId == null) {
                    val fetched = fetchSingleVideoDetails(singleVideoId, "@youtube")
                    val inaccessible = if (fetched.extractionStatus == "INACCESSIBLE") 1 else 0
                    return@withContext Result.success(
                        ChannelAuditResult(
                            sourceInput = trimmed,
                            resolvedChannelId = fetched.channelId.ifBlank { null },
                            videosFoundCount = 1,
                            videos = listOf(fetched),
                            inaccessibleCount = inaccessible,
                            statusNote = "Inspected individual video ($singleVideoId): ${fetched.extractionStatus}"
                        )
                    )
                }

                // 2. Playlist URL
                if (playlistId != null) {
                    val (feedTitle, rssEntries) = fetchRssFeedWithTitle("https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId")
                    return@withContext enrichEntries(
                        sourceInput = trimmed,
                        channelId = null,
                        playlistId = playlistId,
                        playlistTitle = feedTitle,
                        handle = "@playlist",
                        entries = rssEntries
                    )
                }

                // 3. Channel URL (/channel/UC... or /@handle)
                val channelId = extractChannelIdFromUrl(trimmed) ?: resolveHandleToChannelId(trimmed)
                if (channelId != null) {
                    val handle = extractHandleLabel(trimmed)
                    if (youtubeApiKey.isNotBlank()) {
                        val apiVideos = fetchChannelViaDataApi(channelId, youtubeApiKey, trimmed)
                        if (apiVideos.isNotEmpty()) {
                            return@withContext enrichEntries(
                                sourceInput = trimmed,
                                channelId = channelId,
                                playlistId = null,
                                playlistTitle = "Channel Uploads ($handle)",
                                handle = handle,
                                entries = apiVideos
                            )
                        }
                    }
                    val (feedTitle, rssEntries) = fetchRssFeedWithTitle("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId")
                    return@withContext enrichEntries(
                        sourceInput = trimmed,
                        channelId = channelId,
                        playlistId = null,
                        playlistTitle = feedTitle.ifBlank { "Channel Feed ($handle)" },
                        handle = handle,
                        entries = rssEntries
                    )
                }

                Result.failure(IllegalStateException("Could not resolve a YouTube channel, playlist, or video ID from: $trimmed"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed inspecting YouTube URL: $rawUrl", e)
                Result.failure(e)
            }
        }

    private suspend fun enrichEntries(
        sourceInput: String,
        channelId: String?,
        playlistId: String?,
        playlistTitle: String,
        handle: String,
        entries: List<RssVideoEntry>
    ): Result<ChannelAuditResult> {
        if (entries.isEmpty()) {
            return Result.failure(IllegalStateException("No accessible public videos returned from feed for $sourceInput."))
        }
        val limited = entries.take(15)
        val semaphore = Semaphore(4)
        val detailed = coroutineScope {
            limited.map { entry ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        fetchSingleVideoDetails(
                            videoId = entry.videoId,
                            channelHandle = handle,
                            channelId = channelId.orEmpty(),
                            playlistId = playlistId.orEmpty(),
                            playlistTitle = playlistTitle,
                            fallbackTitle = entry.title,
                            fallbackDesc = entry.description,
                            fallbackPublishedDate = entry.publishedDate
                        )
                    }
                }
            }.awaitAll()
        }
        val inaccessible = detailed.count { it.extractionStatus == "INACCESSIBLE" }
        val extracted = detailed.count { it.transcript != null }
        val noCaptions = detailed.count { it.extractionStatus == "NO_CAPTIONS" }
        return Result.success(
            ChannelAuditResult(
                sourceInput = sourceInput,
                resolvedChannelId = channelId,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                videosFoundCount = entries.size,
                videos = detailed,
                inaccessibleCount = inaccessible,
                statusNote = "Found ${entries.size} feed items; audited top ${detailed.size} ($extracted with captions, $noCaptions without captions, $inaccessible inaccessible)."
            )
        )
    }

    private fun fetchSingleVideoDetails(
        videoId: String,
        channelHandle: String,
        channelId: String = "",
        playlistId: String = "",
        playlistTitle: String = "",
        fallbackTitle: String = "",
        fallbackDesc: String = "",
        fallbackPublishedDate: String = ""
    ): FetchedYouTubeVideo {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val html = executeGet(watchUrl)
        if (html == null) {
            return FetchedYouTubeVideo(
                videoId = videoId,
                title = fallbackTitle.ifBlank { "Video $videoId" },
                videoUrl = watchUrl,
                thumbnailUrl = thumbUrl,
                description = fallbackDesc,
                channelHandle = channelHandle,
                channelId = channelId,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                publishedDate = fallbackPublishedDate,
                durationSeconds = 0L,
                transcript = null,
                segments = emptyList(),
                extractionStatus = "INACCESSIBLE",
                statusDetail = "Network request to watch page failed or blocked."
            )
        }

        val title = extractMetaTitle(html) ?: fallbackTitle.ifBlank { "YouTube Video $videoId" }
        val desc = extractShortDescription(html) ?: fallbackDesc
        val publishedDate = extractPublishedDate(html) ?: fallbackPublishedDate
        val durationSeconds = extractDurationSeconds(html)
        val resolvedCid = channelId.ifBlank { extractChannelIdFromHtml(html).orEmpty() }
        val captionUrl = extractCaptionTrackUrl(html)
        if (captionUrl == null) {
            return FetchedYouTubeVideo(
                videoId = videoId,
                title = title,
                videoUrl = watchUrl,
                thumbnailUrl = thumbUrl,
                description = desc,
                channelHandle = channelHandle,
                channelId = resolvedCid,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                publishedDate = publishedDate,
                durationSeconds = durationSeconds,
                transcript = null,
                segments = emptyList(),
                extractionStatus = "NO_CAPTIONS",
                statusDetail = "Video accessible, but public timedtext captions were unavailable."
            )
        }

        val captionXml = executeGet(captionUrl)
        val segments = if (captionXml != null) parseTimedTextSegments(captionXml) else emptyList()
        val formattedTranscript = formatTranscriptFromSegments(segments)
        return if (formattedTranscript.isNotBlank()) {
            FetchedYouTubeVideo(
                videoId = videoId,
                title = title,
                videoUrl = watchUrl,
                thumbnailUrl = thumbUrl,
                description = desc,
                channelHandle = channelHandle,
                channelId = resolvedCid,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                publishedDate = publishedDate,
                durationSeconds = durationSeconds,
                transcript = formattedTranscript,
                segments = segments,
                extractionStatus = "EXTRACTED",
                statusDetail = "Title, duration, metadata, and ${segments.size} timestamped caption segments extracted."
            )
        } else {
            FetchedYouTubeVideo(
                videoId = videoId,
                title = title,
                videoUrl = watchUrl,
                thumbnailUrl = thumbUrl,
                description = desc,
                channelHandle = channelHandle,
                channelId = resolvedCid,
                playlistId = playlistId,
                playlistTitle = playlistTitle,
                publishedDate = publishedDate,
                durationSeconds = durationSeconds,
                transcript = null,
                segments = emptyList(),
                extractionStatus = "NO_CAPTIONS",
                statusDetail = "Caption track present but returned empty timedtext."
            )
        }
    }

    private fun resolveHandleToChannelId(url: String): String? {
        val normalized = if (url.startsWith("http")) url else "https://www.youtube.com/${url.trimStart('/')}"
        val html = executeGet(normalized) ?: return null
        return extractChannelIdFromHtml(html)
    }

    private fun extractChannelIdFromHtml(html: String): String? {
        val patterns = listOf(
            Regex("\"channelId\":\"(UC[a-zA-Z0-9_-]{20,})\""),
            Regex("\"externalId\":\"(UC[a-zA-Z0-9_-]{20,})\""),
            Regex("https://www\\.youtube\\.com/channel/(UC[a-zA-Z0-9_-]{20,})")
        )
        for (regex in patterns) {
            val match = regex.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun fetchChannelViaDataApi(channelId: String, apiKey: String, sourceLabel: String): List<RssVideoEntry> {
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$channelId&maxResults=15&order=date&type=video&key=$apiKey"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { i ->
                val item = items.getJSONObject(i)
                val vid = item.optJSONObject("id")?.optString("videoId").orEmpty()
                val snippet = item.optJSONObject("snippet")
                val title = snippet?.optString("title").orEmpty()
                val desc = snippet?.optString("description").orEmpty()
                val pub = snippet?.optString("publishedAt").orEmpty().take(10)
                if (vid.isNotBlank()) RssVideoEntry(vid, title, desc, pub) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube Data API parse fallback for $sourceLabel: ${e.message}")
            emptyList()
        }
    }

    private fun fetchRssFeedWithTitle(feedUrl: String): Pair<String, List<RssVideoEntry>> {
        val xml = executeGet(feedUrl) ?: return "" to emptyList()
        return parseRssFeedXml(xml)
    }

    internal fun parseRssFeedXml(xml: String): Pair<String, List<RssVideoEntry>> {
        val results = mutableListOf<RssVideoEntry>()
        var feedTitle = ""
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var inEntry = false
            var currentId = ""
            var currentTitle = ""
            var currentDesc = ""
            var currentPublished = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "entry") {
                            inEntry = true
                            currentId = ""
                            currentTitle = ""
                            currentDesc = ""
                            currentPublished = ""
                        } else if (!inEntry && name == "title" && feedTitle.isBlank()) {
                            feedTitle = parser.nextText().trim()
                        } else if (inEntry) {
                            when (name) {
                                "yt:videoId", "videoId" -> currentId = parser.nextText()
                                "title" -> currentTitle = parser.nextText()
                                "published" -> currentPublished = parser.nextText().take(10)
                                "media:description", "description" -> currentDesc = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "entry" && inEntry) {
                            if (currentId.isNotBlank()) {
                                results.add(
                                    RssVideoEntry(
                                        videoId = currentId.trim(),
                                        title = currentTitle.trim(),
                                        description = currentDesc.trim(),
                                        publishedDate = currentPublished.trim()
                                    )
                                )
                            }
                            inEntry = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "RSS parse error: ${e.message}")
        }
        return feedTitle to results
    }

    internal fun extractCaptionTrackUrl(html: String): String? {
        val regex = Regex("\"captionTracks\":(\\[.*?\\])")
        val match = regex.find(html) ?: return null
        return try {
            val arr = JSONArray(match.groupValues[1])
            if (arr.length() == 0) return null
            var chosenUrl: String? = null
            for (i in 0 until arr.length()) {
                val track = arr.getJSONObject(i)
                val lang = track.optString("languageCode")
                val url = track.optString("baseUrl")
                if (lang.startsWith("en") && url.isNotBlank()) {
                    chosenUrl = url
                    break
                }
                if (chosenUrl == null && url.isNotBlank()) chosenUrl = url
            }
            chosenUrl?.replace("\\u0026", "&")
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseTimedTextSegments(xml: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "text") {
                    val startSec = parser.getAttributeValue(null, "start")?.toDoubleOrNull() ?: 0.0
                    val durSec = parser.getAttributeValue(null, "dur")?.toDoubleOrNull() ?: 0.0
                    val mins = (startSec / 60).toInt()
                    val secs = (startSec % 60).toInt()
                    val label = "%02d:%02d".format(mins, secs)
                    val text = parser.nextText()
                        .replace("&#39;", "'")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .replace("\n", " ")
                        .trim()
                    if (text.isNotBlank()) {
                        segments.add(
                            TranscriptSegment(
                                startSeconds = startSec,
                                durationSeconds = durSec,
                                timestampLabel = label,
                                text = text
                            )
                        )
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            // Ignore partial XML stream tail issues
        }
        return segments
    }

    internal fun formatTranscriptFromSegments(segments: List<TranscriptSegment>): String {
        return segments.joinToString(" ") { "[${it.timestampLabel}] ${it.text}" }.trim()
    }

    private fun extractMetaTitle(html: String): String? {
        val regex = Regex("<meta name=\"title\" content=\"(.*?)\">")
        return regex.find(html)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.replace("&#39;", "'")
            ?.replace("&quot;", "\"")
    }

    private fun extractShortDescription(html: String): String? {
        val regex = Regex("\"shortDescription\":\"((?:\\\\.|[^\"\\\\])*)\"")
        val raw = regex.find(html)?.groupValues?.getOrNull(1) ?: return null
        return raw.replace("\\n", "\n").replace("\\\"", "\"")
    }

    private fun extractPublishedDate(html: String): String? {
        val patterns = listOf(
            Regex("\"publishDate\":\"([0-9]{4}-[0-9]{2}-[0-9]{2})"),
            Regex("<meta itemprop=\"datePublished\" content=\"([0-9]{4}-[0-9]{2}-[0-9]{2})")
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.getOrNull(1) }
    }

    private fun extractDurationSeconds(html: String): Long {
        val regex = Regex("\"lengthSeconds\":\"(\\d+)\"")
        return regex.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    }

    internal fun extractSingleVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})")
        )
        return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
    }

    internal fun extractPlaylistId(url: String): String? {
        return Regex("[?&]list=([a-zA-Z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)
    }

    internal fun extractChannelIdFromUrl(url: String): String? {
        return Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{20,})").find(url)?.groupValues?.getOrNull(1)
    }

    internal fun extractHandleLabel(url: String): String {
        val match = Regex("@([a-zA-Z0-9_.-]+)").find(url)
        return if (match != null) "@${match.groupValues[1]}" else "@youtube"
    }

    private fun executeGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
