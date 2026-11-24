package com.example.speech.pipeline

import com.example.speech.rules.IssueCode

/**
 * Pairs a detected issue with its relevant transcript span for inline annotation (ARC-05 §8.4, v1.5.0).
 * Each insight highlights the exact words in the transcript that triggered a rule or detection.
 */
data class TranscriptInsight(
    val issueCode: IssueCode,
    val spanStart: Int,          // character offset in transcript
    val spanEnd: Int,            // character offset in transcript
    val highlightedText: String, // the actual words
    val issueTitle: String,      // from IssueCode.title
    val suggestion: String,      // actionable tip
    val severity: InsightSeverity
)

/**
 * Severity classification for transcript insights.
 */
enum class InsightSeverity { ERROR, WARNING, TIP }
