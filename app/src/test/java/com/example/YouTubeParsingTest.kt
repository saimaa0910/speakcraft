package com.example

import com.example.data.youtube.YouTubeIntelligenceClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeParsingTest {

    private val client = YouTubeIntelligenceClient()

    @Test
    fun `url extractors handle watch shorts youtu_be playlist and channel URLs`() {
        assertEquals("dQw4w9WgXcQ", client.extractSingleVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("abcDEF12345", client.extractSingleVideoId("https://youtu.be/abcDEF12345?t=30"))
        assertEquals("xyz98765432", client.extractSingleVideoId("https://www.youtube.com/shorts/xyz98765432"))
        assertNull(client.extractSingleVideoId("https://www.youtube.com/@askvinh"))

        assertEquals("PL12345_ABC", client.extractPlaylistId("https://www.youtube.com/playlist?list=PL12345_ABC"))
        assertEquals("@askvinh", client.extractHandleLabel("https://www.youtube.com/@askvinh"))
        assertEquals("UC1234567890123456789012", client.extractChannelIdFromUrl("https://www.youtube.com/channel/UC1234567890123456789012"))
    }

    @Test
    fun `rss parser extracts playlist title video id published date and description`() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/">
              <title>Ask Vinh - Vocal Mastery</title>
              <entry>
                <yt:videoId>vid00123456</yt:videoId>
                <title>3 Vocal Foundations</title>
                <published>2026-04-12T10:00:00+00:00</published>
                <media:description>Practice rate, volume, and pauses.</media:description>
              </entry>
            </feed>
        """.trimIndent()

        val (feedTitle, entries) = client.parseRssFeedXml(sampleRss)
        assertEquals("Ask Vinh - Vocal Mastery", feedTitle)
        assertEquals(1, entries.size)
        assertEquals("vid00123456", entries.first().videoId)
        assertEquals("3 Vocal Foundations", entries.first().title)
        assertEquals("2026-04-12", entries.first().publishedDate)
        assertTrue(entries.first().description.contains("Practice rate"))
    }

    @Test
    fun `timedtext parser preserves timestamped segments and decodes entities`() {
        val timedText = """
            <?xml version="1.0" encoding="utf-8" ?>
            <transcript>
              <text start="5.2" dur="3.1">Hello &amp; welcome to today&#39;s lesson.</text>
              <text start="75.0" dur="4.0">&quot;Silence&quot; creates gravity before key points.</text>
            </transcript>
        """.trimIndent()

        val segments = client.parseTimedTextSegments(timedText)
        assertEquals(2, segments.size)
        assertEquals("00:05", segments[0].timestampLabel)
        assertEquals("Hello & welcome to today's lesson.", segments[0].text)
        assertEquals("01:15", segments[1].timestampLabel)
        assertEquals("\"Silence\" creates gravity before key points.", segments[1].text)

        val formatted = client.formatTranscriptFromSegments(segments)
        assertTrue(formatted.contains("[00:05] Hello & welcome"))
        assertTrue(formatted.contains("[01:15] \"Silence\" creates gravity"))
    }
}
