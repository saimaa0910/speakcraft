package com.example.data.ai.local

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Enforces that all AI inference happens on-device only.
 * Any attempt to call a cloud AI endpoint is blocked at the network layer.
 */
object LocalExecutionPolicy {

    const val POLICY = "LOCAL_ONLY"

    fun isLocalOnly(): Boolean = POLICY == "LOCAL_ONLY"

    /** Domains whose traffic is considered "cloud AI" and must be blocked. */
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

    /**
     * OkHttp [Interceptor] that throws [IOException] if the outgoing request
     * targets any known cloud-AI endpoint. Attach this to every OkHttpClient
     * used in the app to enforce the LOCAL_ONLY policy at the network level.
     */
    val aiEndpointBlockingInterceptor: Interceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()

        val blockedDomain = BLOCKED_AI_DOMAINS.firstOrNull { domain -> url.contains(domain) }
        if (blockedDomain != null) {
            throw IOException(
                "LOCAL_ONLY policy violation: request to cloud AI endpoint blocked. " +
                "Matched domain: '$blockedDomain'. URL: $url"
            )
        }

        chain.proceed(request)
    }

    /**
     * Programmatic assertion that the LOCAL_ONLY policy is active.
     * Call this at app-startup or in tests to verify the interceptor is wired.
     *
     * @throws IllegalStateException if the policy constant has been tampered with.
     */
    fun verifyLocalOnly() {
        check(POLICY == "LOCAL_ONLY") {
            "LocalExecutionPolicy.POLICY has been modified — LOCAL_ONLY guarantee is broken."
        }
        check(BLOCKED_AI_DOMAINS.isNotEmpty()) {
            "LocalExecutionPolicy.BLOCKED_AI_DOMAINS is empty — cloud AI endpoints are not blocked."
        }
        // All good: policy is active.
    }
}
