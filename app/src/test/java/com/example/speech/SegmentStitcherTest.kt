package com.example.speech

import com.example.speech.recognition.SegmentStitcher
import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentStitcherTest {

    @Test
    fun `stitch handles empty inputs gracefully`() {
        assertEquals("hello", SegmentStitcher.stitch("", "hello"))
        assertEquals("world", SegmentStitcher.stitch("world", ""))
        assertEquals("", SegmentStitcher.stitch("", ""))
    }

    @Test
    fun `stitch joins disjoint segments with a space`() {
        val base = "The quick brown fox"
        val incoming = "jumps over the lazy dog"
        val expected = "The quick brown fox jumps over the lazy dog"

        assertEquals(expected, SegmentStitcher.stitch(base, incoming))
    }

    @Test
    fun `stitch removes overlapping words at segment boundary (fixes FND-AI-04)`() {
        val base = "we will focus on delivering value"
        val incoming = "delivering value to our users"
        val expected = "we will focus on delivering value to our users"

        assertEquals(expected, SegmentStitcher.stitch(base, incoming))
    }

    @Test
    fun `stitch handles single overlapping word with punctuation and case difference`() {
        val base = "I had an interview today,"
        val incoming = "Today was very challenging"
        val expected = "I had an interview today, was very challenging"

        assertEquals(expected, SegmentStitcher.stitch(base, incoming))
    }

    @Test
    fun `stitch handles identical duplicate segment without repetition`() {
        val base = "good morning everyone"
        val incoming = "good morning everyone"

        assertEquals("good morning everyone", SegmentStitcher.stitch(base, incoming))
    }

    @Test
    fun `stitch handles multi-word overlap up to threshold`() {
        val base = "one two three four five six seven eight"
        val incoming = "five six seven eight nine ten"
        val expected = "one two three four five six seven eight nine ten"

        assertEquals(expected, SegmentStitcher.stitch(base, incoming))
    }
}
