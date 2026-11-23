package com.example.speech.feedback

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import kotlin.math.abs

/**
 * Deterministic coach tip catalog entry (ARC-05 §8.4, PRD-FR-056, SRS-FR-EVAL-016).
 * Follows D-08: original phrasing, own examples, no creator likeness or branding, link out to lessons.
 */
data class TipDefinition(
    val tipId: String,
    val techniqueId: String,
    val title: String,
    val text: String,
    val triggerIssueCode: IssueCode? = null,
    val triggerMetricKey: SpeechMetricKey? = null,
    val triggerMetricBand: String? = null,
    val triggerTemplate: String? = null,
    val priority: Int = 1,
    val sourceVideoUrl: String? = null
)

/**
 * Deterministic Tip Selector implementing ARC-05 §8.4 (CMP-75).
 *
 * Selection Rules:
 * 1. Candidates match by trigger: issue code > metric band > technique id > template id.
 * 2. Ranked by trigger specificity (4 > 3 > 2 > 1), then priority (1 > 2 > ...), then stable hash of (tipId + attemptId).
 * 3. Focus tip = top tip for highest priority issue.
 * 4. Up to [maxExtra] (default 2) additional tips cover subsequent issues or the activity's technique.
 * 5. If no issue detected: returns 1 general technique tip + "Our checks didn't find issues in this attempt".
 */
object TipSelector {

    val CATALOG: List<TipDefinition> = listOf(
        // Quiet / level issues
        TipDefinition(
            tipId = "TIP-LEVEL-01",
            techniqueId = "TECH-MIC-PROXIMITY",
            title = "Microphone Positioning",
            text = "Keep your phone at a consistent distance of about 15–20 cm (6–8 inches). Avoid shifting back and forth while speaking.",
            triggerIssueCode = IssueCode.ISSUE_TOO_QUIET,
            priority = 1
        ),
        // Word not recognised
        TipDefinition(
            tipId = "TIP-WORD-RECOG-01",
            techniqueId = "TECH-ARTICULATION",
            title = "Crisp Consonants",
            text = "Over-articulate final consonants (t, d, k, p). In English, the ends of words carry crucial grammatical meaning.",
            triggerIssueCode = IssueCode.ISSUE_WORD_NOT_RECOGNISED,
            priority = 1,
            sourceVideoUrl = "https://youtube.com/watch?v=speech_consonants_drill"
        ),
        // Target word missing or wrong form
        TipDefinition(
            tipId = "TIP-TARGET-01",
            techniqueId = "TECH-VOCAB-ANCHOR",
            title = "Anchor Your Key Term",
            text = "Decide on your sentence structure before you start so the target word naturally fits as your subject or main verb.",
            triggerIssueCode = IssueCode.ISSUE_TARGET_WORD_MISSING,
            priority = 1
        ),
        TipDefinition(
            tipId = "TIP-WRONG-FORM-01",
            techniqueId = "TECH-VOCAB-INFLECTION",
            title = "Match the Verb Tense",
            text = "Check whether the prompt asks for past, present, or continuous form. Practice saying the specific form aloud once before recording.",
            triggerIssueCode = IssueCode.ISSUE_WRONG_FORM,
            priority = 1
        ),
        // Structure issues
        TipDefinition(
            tipId = "TIP-STRUCT-STAR-01",
            techniqueId = "TECH-FRAMEWORK-STAR",
            title = "State the Result Clearly",
            text = "End your story with the concrete outcome: What changed because of what you did? Use signposts like 'As a result' or 'Ultimately'.",
            triggerIssueCode = IssueCode.ISSUE_STRUCT_ELEMENT_MISSING,
            priority = 1,
            sourceVideoUrl = "https://youtube.com/watch?v=star_framework_mastery"
        ),
        // Filler words
        TipDefinition(
            tipId = "TIP-FILLERS-PAUSE-01",
            techniqueId = "TECH-SILENT-PAUSE",
            title = "The Power of the Silent Pause",
            text = "Whenever you feel 'um' or 'like' coming on, close your lips and pause for one second. Silence conveys confidence and authority.",
            triggerIssueCode = IssueCode.ISSUE_FILLERS_HIGH,
            priority = 1,
            sourceVideoUrl = "https://youtube.com/watch?v=master_the_silent_pause"
        ),
        // Fast pace
        TipDefinition(
            tipId = "TIP-PACE-FAST-01",
            techniqueId = "TECH-PACE-MODULATION",
            title = "Slow Down to 130 WPM",
            text = "Breathe between sentences and stretch your stressed vowel sounds. Giving listeners space to digest your words makes you sound thoughtful.",
            triggerIssueCode = IssueCode.ISSUE_PACE_FAST,
            priority = 1
        ),
        // Slow pace
        TipDefinition(
            tipId = "TIP-PACE-SLOW-01",
            techniqueId = "TECH-CHUNK-SPEAKING",
            title = "Speak in Thought Chunks",
            text = "Group 3 to 5 words together smoothly rather than pronouncing words individually. Think of phrases as musical measures.",
            triggerIssueCode = IssueCode.ISSUE_PACE_SLOW,
            priority = 1
        ),
        // Pauses
        TipDefinition(
            tipId = "TIP-PAUSES-01",
            techniqueId = "TECH-TRAIN-OF-THOUGHT",
            title = "Pre-visualize Your Next Point",
            text = "Before you begin your next sentence, picture the main noun or verb. This eliminates awkward hesitations mid-clause.",
            triggerIssueCode = IssueCode.ISSUE_PAUSES_LONG,
            priority = 1
        ),
        // Too short
        TipDefinition(
            tipId = "TIP-TOO-SHORT-01",
            techniqueId = "TECH-RULE-OF-THREE",
            title = "Expand with One Concrete Example",
            text = "After stating your main idea, add: 'For instance...' followed by a 1-sentence real situation to build depth.",
            triggerIssueCode = IssueCode.ISSUE_TOO_SHORT,
            priority = 1
        ),
        // Grammar check
        TipDefinition(
            tipId = "TIP-GRAMMAR-01",
            techniqueId = "TECH-PRECISION-HABITS",
            title = "Third-Person Agreement Habit",
            text = "Remember that singular subjects take singular verbs: 'he does', 'she has', 'it goes'. Re-read the corrected sentence aloud 3 times.",
            triggerIssueCode = IssueCode.ISSUE_GRAMMAR_POSSIBLE,
            priority = 1
        ),
        // Hedging
        TipDefinition(
            tipId = "TIP-HEDGING-01",
            techniqueId = "TECH-DIRECT-ASSERTION",
            title = "Drop Weakening Softeners",
            text = "Replace phrases like 'I kind of think maybe' with direct statements: 'My recommendation is...' or 'In my view...'.",
            triggerIssueCode = IssueCode.ISSUE_HEDGING_HIGH,
            priority = 1
        ),
        // Low variety
        TipDefinition(
            tipId = "TIP-VARIETY-01",
            techniqueId = "TECH-SYNONYM-PALETTE",
            title = "Expand Your Descriptive Vocabulary",
            text = "Notice repeating words like 'good', 'problem', or 'nice'. Substitute with precise terms like 'effective', 'bottleneck', or 'valuable'.",
            triggerIssueCode = IssueCode.ISSUE_LOW_VARIETY,
            priority = 1
        ),
        // General technique fallbacks
        TipDefinition(
            tipId = "TIP-TECH-GENERAL-01",
            techniqueId = "TECH-DELIVERY-GENERAL",
            title = "Vocal Resonance & Posture",
            text = "Stand or sit tall with an open chest. Speaking from your diaphragm creates warm, natural authority in any conversation.",
            priority = 3,
            sourceVideoUrl = "https://youtube.com/watch?v=vocal_resonance_foundation"
        ),
        TipDefinition(
            tipId = "TIP-TECH-GENERAL-02",
            techniqueId = "TECH-CONFIDENT-CLOSING",
            title = "Land Your Final Sentence",
            text = "Avoid trailing off with 'so yeah' or 'that's it'. Deliver your closing sentence with downward inflection and hold a calm pause.",
            priority = 3
        )
    )

    /**
     * Deterministically selects coach tips for an attempt.
     */
    fun select(
        attemptId: String,
        issues: List<DetectedIssue>,
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        techniqueIds: List<String> = emptyList(),
        templateId: String? = null,
        maxExtra: Int = 2
    ): List<CoachTip> {
        val selectedTips = mutableListOf<CoachTip>()
        val chosenTipIds = mutableSetOf<String>()

        // 1. If issues exist, find candidate tips
        val rankedCandidates = CATALOG.mapNotNull { tip ->
            val specificity = when {
                tip.triggerIssueCode != null && issues.any { it.code == tip.triggerIssueCode } -> 4
                tip.triggerMetricKey != null && metrics[tip.triggerMetricKey]?.band == tip.triggerMetricBand -> 3
                techniqueIds.contains(tip.techniqueId) -> 2
                tip.triggerTemplate != null && tip.triggerTemplate.equals(templateId, ignoreCase = true) -> 1
                tip.triggerIssueCode == null && tip.triggerMetricKey == null && tip.triggerTemplate == null -> 0 // Fallback
                else -> -1
            }

            if (specificity >= 0) {
                // Stable deterministic hash of tipId + attemptId to vary phrasing without cloud AI
                val stableHash = abs((tip.tipId + attemptId).hashCode()) % 1000
                CandidateTip(tip, specificity, tip.priority, stableHash)
            } else null
        }.sortedWith(
            compareByDescending<CandidateTip> { it.specificity }
                .thenBy { it.priority }
                .thenBy { it.stableHash }
        )

        // ARC-05 §8.4 Step 4 & CMP-75: If no issue detected, return exactly one general tip
        if (issues.isEmpty()) {
            val generalCandidate = rankedCandidates.firstOrNull { it.tip.techniqueId in techniqueIds }
                ?: rankedCandidates.firstOrNull { it.specificity >= 0 }
                ?: CandidateTip(CATALOG.first { it.techniqueId == "TECH-DELIVERY-GENERAL" }, 0, 3, 0)

            return listOf(
                CoachTip(
                    tipId = generalCandidate.tip.tipId,
                    techniqueId = generalCandidate.tip.techniqueId,
                    title = generalCandidate.tip.title,
                    text = generalCandidate.tip.text,
                    forIssue = null,
                    isFocusTip = true,
                    sourceVideoUrl = generalCandidate.tip.sourceVideoUrl
                )
            )
        }

        // 2. Select focus tip for the highest priority issue
        val highestPriorityIssue = issues.first()
        val focusMatch = rankedCandidates.firstOrNull { it.tip.triggerIssueCode == highestPriorityIssue.code }
            ?: rankedCandidates.firstOrNull()

        if (focusMatch != null) {
            selectedTips.add(
                CoachTip(
                    tipId = focusMatch.tip.tipId,
                    techniqueId = focusMatch.tip.techniqueId,
                    title = focusMatch.tip.title,
                    text = focusMatch.tip.text,
                    forIssue = focusMatch.tip.triggerIssueCode,
                    isFocusTip = true,
                    sourceVideoUrl = focusMatch.tip.sourceVideoUrl
                )
            )
            chosenTipIds.add(focusMatch.tip.tipId)
        }

        // 3. Select up to [maxExtra] additional tips covering other issues or general technique
        for (candidate in rankedCandidates) {
            if (selectedTips.size >= 1 + maxExtra) break
            if (candidate.tip.tipId in chosenTipIds) continue

            selectedTips.add(
                CoachTip(
                    tipId = candidate.tip.tipId,
                    techniqueId = candidate.tip.techniqueId,
                    title = candidate.tip.title,
                    text = candidate.tip.text,
                    forIssue = candidate.tip.triggerIssueCode,
                    isFocusTip = false,
                    sourceVideoUrl = candidate.tip.sourceVideoUrl
                )
            )
            chosenTipIds.add(candidate.tip.tipId)
        }

        return selectedTips
    }

    private data class CandidateTip(
        val tip: TipDefinition,
        val specificity: Int,
        val priority: Int,
        val stableHash: Int
    )
}
