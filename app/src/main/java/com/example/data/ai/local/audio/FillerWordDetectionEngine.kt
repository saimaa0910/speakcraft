package com.example.data.ai.local.audio

/**
 * Result of a filler-word analysis pass.
 *
 * @param detectedFillers       Map of filler word/phrase → occurrence count.
 * @param totalFillerCount      Sum of all occurrence counts.
 * @param fillerFrequencyPerMinute  Fillers per minute, based on total word count
 *                              (approximating 125 wpm if word count is 0).
 * @param transcript            The original transcript that was analysed.
 * @param wordCount             Total word count of the transcript.
 */
data class FillerDetectionResult(
    val detectedFillers: Map<String, Int>,
    val totalFillerCount: Int,
    val fillerFrequencyPerMinute: Double,
    val transcript: String,
    val wordCount: Int
)

/**
 * Detects filler words and disfluency phrases in a transcript using word-boundary
 * regex matching.  All processing is local — no cloud calls.
 *
 * Rules:
 * - Single-word fillers use `\b` boundaries so "like" does NOT match inside "likely".
 * - Multi-word fillers ("you know", "sort of") are matched as exact phrases with
 *   flexible whitespace between tokens.
 * - Matching is case-insensitive.
 */
object FillerWordDetectionEngine {

    val defaultFillerWords: Set<String> = setOf(
        "um", "uh", "like", "basically", "actually",
        "you know", "sort of", "kind of", "right",
        "okay so", "i mean"
    )

    /**
     * Analyse [transcript] for occurrences of [defaultFillerWords] ∪ [customFillers].
     *
     * @param transcript    Raw transcript text to analyse.
     * @param customFillers Additional filler words/phrases to detect alongside defaults.
     * @return [FillerDetectionResult] with per-filler counts and aggregate stats.
     */
    fun detect(
        transcript: String,
        customFillers: Set<String> = emptySet()
    ): FillerDetectionResult {
        val allFillers = defaultFillerWords + customFillers

        // Sort longest-first so multi-word phrases match before their component words
        val sortedFillers = allFillers.sortedByDescending { it.length }

        val counts = mutableMapOf<String, Int>()

        for (filler in sortedFillers) {
            val pattern = buildPattern(filler)
            val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE))
            val matchCount = regex.findAll(transcript).count()
            if (matchCount > 0) {
                counts[filler] = matchCount
            }
        }

        val totalFillerCount = counts.values.sum()

        val wordCount = if (transcript.isBlank()) 0
        else transcript.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

        // Estimate speaking time: assume 125 wpm if we have no better data
        val estimatedMinutes = if (wordCount > 0) wordCount / 125.0 else 1.0
        val fillerFrequencyPerMinute = if (estimatedMinutes > 0)
            totalFillerCount / estimatedMinutes else 0.0

        return FillerDetectionResult(
            detectedFillers = counts.toMap(),
            totalFillerCount = totalFillerCount,
            fillerFrequencyPerMinute = fillerFrequencyPerMinute,
            transcript = transcript,
            wordCount = wordCount
        )
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * Build a regex pattern for [filler]:
     * - Multi-word: wrap with `\b`, join tokens with `\s+`.
     * - Single-word: simple `\b<word>\b`.
     */
    private fun buildPattern(filler: String): String {
        val escaped = filler.trim()
            .split(Regex("\\s+"))
            .joinToString("""\s+""") { Regex.escape(it) }
        return """\b$escaped\b"""
    }
}
