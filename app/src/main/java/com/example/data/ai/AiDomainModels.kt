package com.example.data.ai

data class VocabEvaluationResult(
    val usedCorrectly: Boolean,
    val naturalnessScore: Int,
    val feedback: String,
    val improvedExample: String
)
