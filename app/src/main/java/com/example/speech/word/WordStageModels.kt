package com.example.speech.word

import com.example.speech.rules.DetectedIssue

/**
 * Stages of word vocabulary mastery (ARC-05 §9, SRS-FR-WORD-001).
 */
enum class WordStage(val stageNumber: Int, val title: String) {
    SEE_IT(1, "See it"),
    SAY_IT(2, "Say it"),
    USE_IT(3, "Use it"),
    MAKE_YOUR_OWN(4, "Make your own")
}

/**
 * Requirements enforced during Stage 4 "Make your own" (ARC-05 §9).
 */
enum class StageRequirement {
    TARGET_PRESENT,
    MIN_WORDS,
    GRAMMAR_HITS,
    COMPOSITE_SCORE,
    BOTH_SENTENCES
}

/**
 * Result of evaluating a vocabulary stage attempt (CMP-36).
 */
data class WordStageResult(
    val stage: WordStage,
    val score: Int,
    val threshold: Int,
    val passed: Boolean,
    val unmet: Set<StageRequirement> = emptySet(),
    val issues: List<DetectedIssue> = emptyList(),
    val caveat: String? = null
)
