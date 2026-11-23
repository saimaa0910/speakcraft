package com.example.speech.recognition

import java.util.Locale

/**
 * Text assembler and stitcher for continuous speech recognition across restart boundaries
 * (ARC-05 §4.2, T-05.1, fixes FND-AI-04 duplication).
 *
 * Automatically detects and de-duplicates word overlaps between consecutive segments.
 */
object SegmentStitcher {

    /**
     * Stitches an incoming segment onto the existing accumulated transcript, removing
     * any overlapping words at the boundary.
     *
     * @param existingAccumulated Text accumulated so far
     * @param newSegmentText New segment text to append
     * @return Stitched and de-duplicated transcript
     */
    fun stitch(existingAccumulated: String, newSegmentText: String): String {
        val base = existingAccumulated.trim()
        val incoming = newSegmentText.trim()

        if (base.isEmpty()) return incoming
        if (incoming.isEmpty()) return base

        // Tokenize words
        val baseWords = base.split(Regex("""\s+"""))
        val incomingWords = incoming.split(Regex("""\s+"""))

        // Find the maximum word overlap between the end of base and start of incoming
        val maxOverlapCheck = minOf(baseWords.size, incomingWords.size, 10) // check up to 10 overlapping words
        var bestOverlapCount = 0

        for (k in maxOverlapCheck downTo 1) {
            val baseSlice = baseWords.takeLast(k)
            val incomingSlice = incomingWords.take(k)

            val matches = (0 until k).all { i ->
                normalizeWord(baseSlice[i]) == normalizeWord(incomingSlice[i])
            }

            if (matches) {
                bestOverlapCount = k
                break
            }
        }

        return if (bestOverlapCount > 0) {
            // Drop overlapping prefix words from incoming
            val nonOverlappingIncoming = incomingWords.drop(bestOverlapCount)
            if (nonOverlappingIncoming.isEmpty()) {
                base
            } else {
                "$base ${nonOverlappingIncoming.joinToString(" ")}"
            }
        } else {
            "$base $incoming"
        }
    }

    /**
     * Normalizes a token for comparison (lowercased, stripped of common punctuation).
     */
    fun normalizeWord(token: String): String {
        return token.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
    }
}
