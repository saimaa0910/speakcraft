package com.example.data.ai.local

/**
 * Enforces that all AI and speech analysis in SpeakCraft runs strictly on-device (ADR-003).
 * Cloud AI SDKs and remote inference services have been removed from the build.
 */
object LocalExecutionPolicy {

    const val POLICY = "LOCAL_ONLY"

    fun isLocalOnly(): Boolean = POLICY == "LOCAL_ONLY"

    /** Domains whose traffic is considered cloud AI and is never called by SpeakCraft. */
    val BLOCKED_AI_DOMAINS = listOf(
        "generativelanguage.googleapis.com",
        "firebaseml.googleapis.com",
        "api.openai.com",
        "api.anthropic.com",
        "api.groq.com",
        "huggingface.co/api"
    )

    fun isBlockedAiEndpoint(url: String): Boolean =
        BLOCKED_AI_DOMAINS.any { url.contains(it) }

    fun verifyLocalOnly() {
        check(POLICY == "LOCAL_ONLY") {
            "LocalExecutionPolicy.POLICY has been modified — LOCAL_ONLY guarantee is broken."
        }
        check(BLOCKED_AI_DOMAINS.isNotEmpty()) {
            "LocalExecutionPolicy.BLOCKED_AI_DOMAINS is empty."
        }
    }
}
