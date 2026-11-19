package com.example.data.ai.local.language

import android.content.Context
import com.example.data.ai.local.OnDeviceModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stage 1.2 On-Device Feedback Generation Engine.
 *
 * Fully deterministic and lightweight: delegates directly to [LocalFeedbackEngine]
 * which orchestrates the 7 specialized sub-engines (Grammar, Fluency, Vocabulary,
 * Pronunciation, Confidence, Structure, Improvement).
 *
 * **Stage 1.2 Invariant:** Zero LLM dependencies. No Phi-3.5 Mini, Gemma, or Qwen.
 */
class LocalFeedbackGenerationEngine(
    @Suppress("unused") private val context: Context? = null,
    @Suppress("unused") private val modelManager: OnDeviceModelManager? = null
) {

    /**
     * Generates all 6 structured feedback sections from [evidence].
     */
    suspend fun generateFeedback(evidence: LocalFeedbackEvidence): StructuredFeedback =
        withContext(Dispatchers.Default) {
            LocalFeedbackEngine.buildFeedback(evidence)
        }

    /**
     * Releases any held resources (no-op in Stage 1.2 lightweight deterministic mode).
     */
    fun release() {
        // No native inference sessions to release in lightweight deterministic mode
    }
}
