package com.example

import com.example.data.ai.GeminiCoachService
import com.example.data.ai.local.LocalExecutionPolicy
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ZeroCloudAiNetworkBoundaryTest {

    @Test
    fun zero_cloud_policy_is_strictly_enforced() {
        assertEquals("LOCAL_ONLY", LocalExecutionPolicy.POLICY)
        LocalExecutionPolicy.verifyLocalOnly()
    }

    @Test
    fun network_interceptor_throws_ioexception_on_any_cloud_ai_endpoint() {
        // Test client with interceptor and a mock terminal chain
        val blockedEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent",
            "https://firebaseml.googleapis.com/v1/projects/speakcraft/models",
            "https://api.openai.com/v1/chat/completions",
            "https://api.anthropic.com/v1/messages",
            "https://api.groq.com/openai/v1/chat/completions",
            "https://huggingface.co/api/inference"
        )

        val interceptor = LocalExecutionPolicy.aiEndpointBlockingInterceptor

        blockedEndpoints.forEach { endpoint ->
            val request = Request.Builder().url(endpoint).build()
            val fakeChain = object : Interceptor.Chain {
                override fun request(): Request = request
                override fun proceed(request: Request): Response {
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                override fun connection() = null
                override fun call() = throw UnsupportedOperationException()
                override fun connectTimeoutMillis() = 1000
                override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun readTimeoutMillis() = 1000
                override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun writeTimeoutMillis() = 1000
                override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            }

            val exception = assertThrows(IOException::class.java) {
                interceptor.intercept(fakeChain)
            }
            assertTrue(
                "Exception message must mention LOCAL_ONLY policy violation",
                exception.message?.contains("LOCAL_ONLY policy violation") == true
            )
        }
    }

    @Test
    fun deprecated_gemini_coach_service_strictly_throws_unsupported_operation() = runTest {
        val deprecatedService = GeminiCoachService()

        assertThrows(UnsupportedOperationException::class.java) {
            runTest {
                deprecatedService.analyzeSpeechRecording(
                    mediaBytes = ByteArray(10),
                    mimeType = "audio/mp4",
                    moduleType = "test",
                    promptTitle = "test",
                    durationSeconds = 10,
                    previousSession = null
                )
            }
        }

        assertThrows(UnsupportedOperationException::class.java) {
            runTest {
                deprecatedService.evaluateVocabularyUsage("word", "meaning", "sentence")
            }
        }

        assertThrows(UnsupportedOperationException::class.java) {
            runTest {
                deprecatedService.generateConversationTurn("scenario", "level", emptyList(), "msg")
            }
        }

        assertThrows(UnsupportedOperationException::class.java) {
            runTest {
                deprecatedService.generateConversationCheckIn("scenario", "transcript")
            }
        }
    }
}
