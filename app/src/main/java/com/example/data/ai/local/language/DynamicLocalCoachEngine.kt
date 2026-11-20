package com.example.data.ai.local.language

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dynamic, deep on-device communication intelligence engine.
 *
 * Unlike static boilerplate templates, this engine analyzes:
 * - What the user actually talked about (topic/key-phrase extraction)
 * - Communication intent (persuasion, storytelling, explanation, interview, reflection, conversation)
 * - Discourse structure (opening hook, concrete specificity, transitions, closing punch)
 * - Power language vs. hedging/weak qualifiers ("kind of", "maybe", "just", "very good", "things")
 * - Sentence rhythm variety & run-on detection
 * - Real in-context Before -> After rewrites of the user's OWN spoken sentences
 *
 * 100% local, zero network calls, deterministic and fast (<5ms).
 */
object DynamicLocalCoachEngine {

    enum class SpeechIntent(val label: String) {
        STORYTELLING("storytelling"),
        PERSUASION("persuasion"),
        EXPLANATION("explanation"),
        INTERVIEW("interview answer"),
        REFLECTION("personal reflection"),
        CONVERSATION("conversational response")
    }

    data class DiscourseProfile(
        val intent: SpeechIntent,
        val keyTopics: List<String>,
        val hedgesDetected: List<String>,
        val vaguePhrasesDetected: List<String>,
        val hasOpeningAnchor: Boolean,
        val hasConcreteExample: Boolean,
        val hasTransitionSignals: Boolean,
        val hasStrongClosing: Boolean,
        val sentenceLengths: List<Int>,
        val rhythmVarietyScore: Int, // 0..100
        val longestSentence: String
    )

    data class SentenceRewrite(
        val original: String,
        val improved: String,
        val why: String
    )

    data class DynamicCoachInsights(
        val discourse: DiscourseProfile,
        val rewrites: List<SentenceRewrite>,
        val strengths: List<String>,
        val focusAreas: List<String>,
        val suggestions: List<String>,
        val narrativeSummary: String,
        val nextMicroDrill: String
    )

    private val stopWords = setOf(
        "the", "and", "for", "that", "with", "this", "from", "have", "they", "were", "been",
        "will", "would", "could", "should", "about", "there", "their", "what", "when", "where",
        "which", "while", "into", "your", "just", "very", "really", "like", "know", "think",
        "want", "going", "actually", "basically", "literally", "kind", "sort", "stuff", "thing",
        "things", "make", "made", "some", "more", "most", "much", "many", "also", "then", "than",
        "them", "these", "those", "here", "only", "over", "such", "even", "back", "after", "first",
        "well", "good", "great", "because", "through", "being", "doing", "said", "says", "tell"
    )

    private val hedgePatterns: List<Pair<Regex, String>> = listOf(
        Regex("\\bi\\s+kind\\s+of\\s+think\\b", RegexOption.IGNORE_CASE) to "I believe",
        Regex("\\bi\\s+sort\\s+of\\s+think\\b", RegexOption.IGNORE_CASE) to "I believe",
        Regex("\\bkind\\s+of\\b", RegexOption.IGNORE_CASE) to "",
        Regex("\\bsort\\s+of\\b", RegexOption.IGNORE_CASE) to "",
        Regex("\\bi\\s+guess\\b", RegexOption.IGNORE_CASE) to "I believe",
        Regex("\\bjust\\s+wanted\\s+to\\s+say\\b", RegexOption.IGNORE_CASE) to "want to highlight",
        Regex("\\bi\\s+think\\s+maybe\\b", RegexOption.IGNORE_CASE) to "I recommend",
        Regex("\\bmaybe\\s+we\\s+could\\s+just\\b", RegexOption.IGNORE_CASE) to "we should",
        Regex("\\bhonestly\\s+i\\s+don'?t\\s+know\\s+if\\b", RegexOption.IGNORE_CASE) to "one key question is whether",
        Regex("\\bbasically\\b", RegexOption.IGNORE_CASE) to "",
        Regex("\\bactually\\b", RegexOption.IGNORE_CASE) to "",
        Regex("\\bliterally\\b", RegexOption.IGNORE_CASE) to ""
    )

    private val vagueUpgrades: List<Pair<Regex, String>> = listOf(
        Regex("\\bvery\\s+good\\b", RegexOption.IGNORE_CASE) to "compelling",
        Regex("\\breally\\s+good\\b", RegexOption.IGNORE_CASE) to "effective",
        Regex("\\bvery\\s+important\\b", RegexOption.IGNORE_CASE) to "essential",
        Regex("\\breally\\s+important\\b", RegexOption.IGNORE_CASE) to "critical",
        Regex("\\bvery\\s+big\\b", RegexOption.IGNORE_CASE) to "substantial",
        Regex("\\breally\\s+hard\\b", RegexOption.IGNORE_CASE) to "challenging",
        Regex("\\ba\\s+lot\\s+of\\b", RegexOption.IGNORE_CASE) to "many",
        Regex("\\bvery\\s+fast\\b", RegexOption.IGNORE_CASE) to "rapid",
        Regex("\\bvery\\s+bad\\b", RegexOption.IGNORE_CASE) to "costly",
        Regex("\\bgood\\s+things\\b", RegexOption.IGNORE_CASE) to "meaningful results",
        Regex("\\bdo\\s+things\\b", RegexOption.IGNORE_CASE) to "deliver outcomes"
    )

    private val fillerCleanupRegex = Regex(
        "\\b(um+|uh+|er+|ah+|you know|i mean|like,\\s*)\\b",
        RegexOption.IGNORE_CASE
    )

    fun analyze(
        evidence: LocalFeedbackEvidence,
        previousSessionDelta: Int? = null
    ): DynamicCoachInsights {
        val transcript = evidence.transcript.trim()
        val sentences = splitSentences(transcript)
        val discourse = buildDiscourseProfile(transcript, sentences, evidence.exerciseTitle)
        val rewrites = buildDynamicRewrites(transcript, sentences, evidence)
        val strengths = buildDynamicStrengths(evidence, discourse, sentences)
        val focusAreas = buildDynamicFocusAreas(evidence, discourse)
        val suggestions = buildDynamicSuggestions(evidence, discourse, rewrites)
        val narrative = buildCoachNarrative(evidence, discourse, rewrites, previousSessionDelta)
        val nextDrill = buildTailoredMicroDrill(evidence, discourse, rewrites)

        return DynamicCoachInsights(
            discourse = discourse,
            rewrites = rewrites,
            strengths = strengths,
            focusAreas = focusAreas,
            suggestions = suggestions,
            narrativeSummary = narrative,
            nextMicroDrill = nextDrill
        )
    }

    /**
     * Analyzes any standalone written or spoken draft pasted into Mentor chat or Practice Lab,
     * returning concrete sentence rewrites, grammar polish, and delivery structure guidance.
     */
    fun analyzeUserDraft(
        text: String,
        topicHint: String = "Communication Draft"
    ): DynamicCoachInsights {
        val clean = text.trim()
        val lang = LocalLanguageAnalysisEngine.analyze(clean)
        val wordCount = LocalLanguageAnalysisEngine.tokenizeWords(clean).size
        val evidence = com.example.data.ai.local.language.LocalFeedbackEvidence(
            evaluatorName = "MentorDraftAnalyzer",
            exerciseTitle = topicHint,
            transcript = clean,
            expectedText = null,
            wordCount = wordCount,
            speechDurationSeconds = (wordCount / 2.3).coerceAtLeast(3.0),
            wordsPerMinute = if (wordCount >= 10) 140 else 0,
            fillerCount = 0,
            detectedFillers = emptyList(),
            pauseCount = 0,
            meanPauseDurationMs = 0.0,
            grammarIssues = lang.grammarIssues,
            vocabularyAnalysis = lang.vocabularyAnalysis,
            audioFeatures = null,
            faceAnalysis = null,
            postureAnalysis = null,
            gestureAnalysis = null,
            limitationNotices = emptyList()
        )
        return analyze(evidence, previousSessionDelta = null)
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 4 }
    }

    private fun buildDiscourseProfile(
        transcript: String,
        sentences: List<String>,
        exerciseTitle: String
    ): DiscourseProfile {
        val lower = transcript.lowercase()
        val words = LocalLanguageAnalysisEngine.tokenizeWords(transcript)

        // 1. Key topic extraction (top meaningful content words & bigrams)
        val freq = linkedMapOf<String, Int>()
        for (w in words) {
            if (w.length >= 4 && w !in stopWords) {
                freq[w] = (freq[w] ?: 0) + 1
            }
        }
        val keyTopics = freq.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .take(3)
            .map { it.key }

        // 2. Speech intent classification
        val titleLower = exerciseTitle.lowercase()
        val intent = when {
            titleLower.contains("story") || Regex("\\b(one day|last year|remember when|suddenly|happened|felt)\\b").containsMatchIn(lower) ->
                SpeechIntent.STORYTELLING
            titleLower.contains("interview") || Regex("\\b(experience|my role|team|managed|achieved|strength|weakness|project)\\b").containsMatchIn(lower) ->
                SpeechIntent.INTERVIEW
            titleLower.contains("pitch") || titleLower.contains("present") || Regex("\\b(recommend|should|must|benefit|impact|therefore|propose)\\b").containsMatchIn(lower) ->
                SpeechIntent.PERSUASION
            Regex("\\b(first|second|step|works by|means that|reason is|for example)\\b").containsMatchIn(lower) ->
                SpeechIntent.EXPLANATION
            titleLower.contains("reflect") || Regex("\\b(learned|realized|my goal|improve|habit|grateful)\\b").containsMatchIn(lower) ->
                SpeechIntent.REFLECTION
            else -> SpeechIntent.CONVERSATION
        }

        // 3. Hedges & vague phrases
        val hedges = buildList {
            for ((regex, _) in hedgePatterns) {
                regex.find(transcript)?.value?.lowercase()?.let { add(it) }
            }
        }.distinct()

        val vague = buildList {
            for ((regex, _) in vagueUpgrades) {
                regex.find(transcript)?.value?.lowercase()?.let { add(it) }
            }
        }.distinct()

        // 4. Discourse signals
        val firstSentence = sentences.firstOrNull().orEmpty()
        val lastSentence = sentences.lastOrNull().orEmpty()
        val hasOpeningAnchor = firstSentence.length >= 16 &&
            !firstSentence.lowercase().startsWith("um") &&
            !firstSentence.lowercase().startsWith("uh") &&
            !firstSentence.lowercase().startsWith("so like")

        val hasConcreteExample = Regex(
            "\\b(for example|for instance|such as|specifically|last week|yesterday|percent|%|\\d+)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(transcript)

        val hasTransitionSignals = Regex(
            "\\b(first|second|finally|however|therefore|because|as a result|on the other hand|moreover|in addition|that's why)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(transcript)

        val hasStrongClosing = sentences.size >= 2 &&
            lastSentence.split(Regex("\\s+")).size in 5..24 &&
            !Regex("\\b(yeah|so yeah|that's it|i guess|whatever|stuff like that)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(lastSentence)

        // 5. Sentence length rhythm variety
        val lengths = sentences.map { LocalLanguageAnalysisEngine.tokenizeWords(it).size }.filter { it > 0 }
        val rhythmVariety = if (lengths.size >= 2) {
            val mean = lengths.average()
            val avgDev = lengths.map { abs(it - mean) }.average()
            ((avgDev / 6.0) * 100.0).roundToInt().coerceIn(20, 100)
        } else 55

        val longest = sentences.maxByOrNull { LocalLanguageAnalysisEngine.tokenizeWords(it).size }.orEmpty()

        return DiscourseProfile(
            intent = intent,
            keyTopics = keyTopics,
            hedgesDetected = hedges,
            vaguePhrasesDetected = vague,
            hasOpeningAnchor = hasOpeningAnchor,
            hasConcreteExample = hasConcreteExample,
            hasTransitionSignals = hasTransitionSignals,
            hasStrongClosing = hasStrongClosing,
            sentenceLengths = lengths,
            rhythmVarietyScore = rhythmVariety,
            longestSentence = longest
        )
    }

    private fun buildDynamicRewrites(
        transcript: String,
        sentences: List<String>,
        evidence: LocalFeedbackEvidence
    ): List<SentenceRewrite> = buildList {
        if (transcript.isBlank()) return@buildList

        // 1. Real in-context grammar & fluency upgrades from user's own sentences
        for (issue in evidence.grammarIssues.take(2)) {
            val hostSentence = sentences.firstOrNull {
                it.contains(issue.originalText, ignoreCase = true)
            }
            if (hostSentence != null && hostSentence.length <= 180) {
                val upgraded = cleanSpaces(
                    hostSentence.replace(issue.originalText, issue.correctedText, ignoreCase = true)
                )
                if (!upgraded.equals(hostSentence, ignoreCase = true)) {
                    add(
                        SentenceRewrite(
                            original = hostSentence,
                            improved = upgraded,
                            why = "${issue.issueType}: ${issue.explanation}"
                        )
                    )
                }
            } else {
                add(
                    SentenceRewrite(
                        original = issue.originalText,
                        improved = issue.correctedText,
                        why = issue.explanation
                    )
                )
            }
        }

        // 2. Rewrite user's own sentences that contain fillers, hedges, or vague wording
        for (sentence in sentences) {
            if (size >= 3) break
            if (any { it.original.equals(sentence, ignoreCase = true) }) continue

            var candidate = sentence
            val reasons = mutableListOf<String>()

            if (fillerCleanupRegex.containsMatchIn(candidate)) {
                candidate = fillerCleanupRegex.replace(candidate, " ")
                reasons += "removed filler hesitation"
            }
            for ((regex, replacement) in hedgePatterns) {
                if (regex.containsMatchIn(candidate)) {
                    candidate = regex.replace(candidate, replacement)
                    reasons += "replaced soft hedging with direct ownership"
                }
            }
            for ((regex, replacement) in vagueUpgrades) {
                if (regex.containsMatchIn(candidate)) {
                    candidate = regex.replace(candidate, replacement)
                    reasons += "upgraded vague phrasing to sharper vocabulary"
                }
            }

            // Split long run-on sentence (>24 words) at natural conjunction for cleaner breathing
            val candidateWords = LocalLanguageAnalysisEngine.tokenizeWords(candidate)
            if (candidateWords.size > 24) {
                val splitMatch = Regex("\\s+(and|but|so|because)\\s+", RegexOption.IGNORE_CASE)
                    .findAll(candidate)
                    .minByOrNull { abs(it.range.first - candidate.length / 2) }
                if (splitMatch != null) {
                    val left = candidate.substring(0, splitMatch.range.first).trim().trimEnd(',', ';')
                    val right = candidate.substring(splitMatch.range.last + 1).trim()
                        .replaceFirstChar { it.uppercase() }
                    if (left.length >= 12 && right.length >= 12) {
                        candidate = "$left. [pause] $right"
                        reasons += "broke a ${candidateWords.size}-word run-on into two crisp beats"
                    }
                }
            }

            candidate = polishSentenceCapitalization(cleanSpaces(candidate))
            if (reasons.isNotEmpty() && !candidate.equals(sentence, ignoreCase = true) && candidate.length >= 8) {
                add(
                    SentenceRewrite(
                        original = sentence,
                        improved = candidate,
                        why = reasons.distinct().joinToString(" and ").replaceFirstChar { it.uppercase() } + "."
                    )
                )
            }
        }

        // 3. If speech was already clean, still elevate the user's actual key sentence with pause/emphasis coaching
        if (isEmpty() && sentences.isNotEmpty()) {
            val anchor = sentences.first()
            val words = anchor.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 6) {
                val breakIdx = (words.size / 2).coerceIn(2, words.size - 2)
                val paced = words.take(breakIdx).joinToString(" ") +
                    " [1s pause] " +
                    words.drop(breakIdx).joinToString(" ")
                add(
                    SentenceRewrite(
                        original = anchor,
                        improved = paced,
                        why = "Adding a deliberate 1-second pause mid-thought amplifies emphasis and vocal authority."
                    )
                )
            }
        }
    }

    private fun buildDynamicStrengths(
        evidence: LocalFeedbackEvidence,
        discourse: DiscourseProfile,
        sentences: List<String>
    ): List<String> = buildList {
        val topicHint = discourse.keyTopics.firstOrNull()?.let { " around \"$it\"" }.orEmpty()

        if (discourse.hasOpeningAnchor && sentences.isNotEmpty()) {
            add("You launched cleanly into your ${discourse.intent.label}$topicHint without stalling at the start.")
        }
        if (discourse.hasConcreteExample) {
            add("Strong specificity — grounding your point with concrete detail makes listeners trust and remember it.")
        }
        if (discourse.hasTransitionSignals) {
            add("Logical flow — your transition cues guided the listener cleanly from one idea to the next.")
        }
        if (discourse.hasStrongClosing) {
            add("Confident finish — you landed your closing thought cleanly instead of trailing off.")
        }
        if (discourse.sentenceLengths.size >= 3 && discourse.rhythmVarietyScore >= 45) {
            add("Engaging sentence rhythm — mixing shorter and longer lines kept your delivery lively.")
        }
        if (evidence.wordsPerMinute in 120..168) {
            add("Ideal conversational tempo (${evidence.wordsPerMinute} WPM) — fast enough to hold energy, calm enough to absorb.")
        }
        if (evidence.fillerCount == 0 && evidence.wordCount >= 12) {
            add("Crisp, filler-free delivery — your pauses felt intentional rather than hesitant.")
        }
        if (evidence.grammarIssues.isEmpty() && evidence.wordCount >= 12) {
            add("Natural grammatical control across your ${sentences.size.coerceAtLeast(1)} spoken thought(s).")
        }
        evidence.vocabularyAnalysis?.let { vocab ->
            if (vocab.vocabularyDiversityScore >= 58) {
                add("Expressive word choice (${vocab.vocabularyDiversityScore}% unique vocabulary) with minimal repetition.")
            }
        }
    }

    private fun buildDynamicFocusAreas(
        evidence: LocalFeedbackEvidence,
        discourse: DiscourseProfile
    ): List<String> = buildList {
        if (discourse.hedgesDetected.isNotEmpty()) {
            val samples = discourse.hedgesDetected.take(3).joinToString(", ") { "\"$it\"" }
            add("Soft hedging ($samples) diluted your authority — state your point directly first.")
        }
        if (discourse.vaguePhrasesDetected.isNotEmpty()) {
            val samples = discourse.vaguePhrasesDetected.take(2).joinToString(", ") { "\"$it\"" }
            add("Swap generic wording ($samples) for sharper, more concrete words.")
        }
        if (!discourse.hasConcreteExample && evidence.wordCount >= 22) {
            val topic = discourse.keyTopics.firstOrNull() ?: "your main idea"
            add("Add one quick real-world example or number to make \"$topic\" immediately vivid.")
        }
        val longestWords = LocalLanguageAnalysisEngine.tokenizeWords(discourse.longestSentence).size
        if (longestWords > 26) {
            add("One sentence stretched to $longestWords words — split long ideas into 2 shorter beats with a breath pause.")
        }
        if (!discourse.hasStrongClosing && evidence.wordCount >= 20 && discourse.sentenceLengths.size >= 2) {
            add("Tighten your ending — finish on a single decisive takeaway line with downward vocal inflection.")
        }
    }

    private fun buildDynamicSuggestions(
        evidence: LocalFeedbackEvidence,
        discourse: DiscourseProfile,
        rewrites: List<SentenceRewrite>
    ): List<String> = buildList {
        rewrites.firstOrNull()?.let { rw ->
            add("Read the upgraded line aloud twice: \"${rw.improved.take(120)}\" — notice how much cleaner it lands.")
        }
        if (discourse.hedgesDetected.isNotEmpty()) {
            add("Record one 30-second take banning \"${discourse.hedgesDetected.first()}\" — replace it with a half-second silent breath.")
        }
        if (!discourse.hasConcreteExample && evidence.wordCount >= 18) {
            add("Use the 1-2-1 formula: 1 clear claim, 1 specific example from real life, 1 crisp closing line.")
        }
        if (evidence.wordsPerMinute > 175) {
            add("Pause for one full beat right before your most important noun — it immediately makes you sound calmer and more authoritative.")
        }
    }

    private fun buildCoachNarrative(
        evidence: LocalFeedbackEvidence,
        discourse: DiscourseProfile,
        rewrites: List<SentenceRewrite>,
        previousSessionDelta: Int?
    ): String {
        if (evidence.transcript.isBlank()) {
            val loudness = evidence.audioFeatures?.relativeLoudness?.roundToInt()
            val paceInfo = if (evidence.wordsPerMinute > 0) " at roughly ${evidence.wordsPerMinute} WPM" else ""
            val volumeCue = when {
                evidence.audioFeatures?.clippingDetected == true ->
                    "Your mic picked up slight clipping — stepping back about 15 cm will instantly make your consonants sound cleaner."
                loudness != null && loudness < -34 ->
                    "Your vocal projection came in quite soft (${loudness} dBFS) — take a deeper belly breath before your opening line so your voice carries effortlessly."
                loudness != null ->
                    "Your vocal projection sounded steady (${loudness} dBFS)$paceInfo."
                else ->
                    "I captured your ${evidence.speechDurationSeconds.roundToInt()}s audio session for vocal delivery analysis."
            }
            return "$volumeCue To unlock sentence-by-sentence rewrites and word choice coaching on your next take, speak with Live Transcript enabled."
        }

        val topicPhrase = when (discourse.keyTopics.size) {
            0 -> "your ${discourse.intent.label}"
            1 -> "your point on \"${discourse.keyTopics[0]}\""
            else -> "your ${discourse.intent.label} around \"${discourse.keyTopics[0]}\" and \"${discourse.keyTopics[1]}\""
        }

        val opener = when {
            evidence.fillerCount == 0 && evidence.grammarIssues.isEmpty() && evidence.wordsPerMinute in 120..170 ->
                "Really crisp take on $topicPhrase — your pacing (${evidence.wordsPerMinute} WPM) and phrasing already sound poised and easy to follow."
            discourse.hasConcreteExample && discourse.hasOpeningAnchor ->
                "You built strong momentum in $topicPhrase by opening cleanly and backing it up with real detail."
            evidence.wordsPerMinute > 180 ->
                "Your energy on $topicPhrase comes through clearly, though at ${evidence.wordsPerMinute} WPM your best ideas are racing past the listener."
            evidence.fillerCount >= 3 ->
                "Your core idea in $topicPhrase is solid, and clearing out those ${evidence.fillerCount} filler pauses will instantly make you sound twice as confident."
            evidence.grammarIssues.isNotEmpty() ->
                "Your message in $topicPhrase came across clearly, and polishing ${evidence.grammarIssues.size} quick phrasing detail(s) will make it sound effortless."
            else ->
                "Solid run on $topicPhrase (${evidence.wordCount} words) — you kept your message focused and approachable."
        }

        val topLeverage = when {
            evidence.grammarIssues.isNotEmpty() -> {
                val issue = evidence.grammarIssues.first()
                "Your highest-leverage tweak: shift \"${issue.originalText}\" to \"${issue.correctedText}\" so your phrasing lands with executive polish."
            }
            evidence.fillerCount >= 2 -> {
                val topFiller = evidence.detectedFillers.firstOrNull()
                    ?.replace(Regex("\\s*\\(\\d+\\)"), "") ?: "filler words"
                "Next time you feel \"$topFiller\" coming, close your lips for one quiet second — silence signals authority."
            }
            discourse.hedgesDetected.isNotEmpty() ->
                "Watch for softeners like \"${discourse.hedgesDetected.first()}\" — dropping them turns a tentative opinion into a clear recommendation."
            evidence.wordsPerMinute > 175 ->
                "Try slowing down by ~15% on your key takeaway sentence so listeners actually feel the weight of your conclusion."
            !discourse.hasConcreteExample && evidence.wordCount >= 20 ->
                "To elevate this from good to memorable, drop in one specific moment, metric, or example."
            rewrites.isNotEmpty() ->
                "Try one quick second take using the tightened phrasing below — adding a deliberate beat before your main point will make it pop."
            else ->
                "Next level up: add vocal contrast by lifting your energy slightly on the setup and slowing down cleanly on your final line."
        }

        val deltaNote = when {
            previousSessionDelta == null -> ""
            previousSessionDelta >= 4 -> " You jumped +$previousSessionDelta points over your previous session — real progress showing up."
            previousSessionDelta <= -4 -> " Your score dipped ${abs(previousSessionDelta)} points from last time — totally normal when stretching on a new prompt."
            else -> ""
        }

        return "$opener $topLeverage$deltaNote".trim()
    }

    private fun buildTailoredMicroDrill(
        evidence: LocalFeedbackEvidence,
        discourse: DiscourseProfile,
        rewrites: List<SentenceRewrite>
    ): String {
        val topic = discourse.keyTopics.firstOrNull()
        return when {
            evidence.fillerCount >= 2 -> {
                val filler = evidence.detectedFillers.firstOrNull()?.replace(Regex("\\s*\\(\\d+\\)"), "") ?: "um"
                "60s Silent-Pause Retake: Re-explain ${topic?.let { "\"$it\"" } ?: "this exact point"} while replacing every \"$filler\" with a calm 1-second breath."
            }
            discourse.hedgesDetected.isNotEmpty() ->
                "30s Executive Retake: Deliver your core point on ${topic?.let { "\"$it\"" } ?: "this topic"} in 3 direct sentences with zero \"${discourse.hedgesDetected.first()}\"."
            evidence.wordsPerMinute > 175 ->
                "Slow-Landing Drill: Repeat your last two sentences at ~140 WPM, pausing 1 full second before the final takeaway."
            evidence.grammarIssues.isNotEmpty() ->
                "Precision Echo: Say \"${evidence.grammarIssues.first().correctedText}\" aloud 3 times inside your own sentence so it becomes automatic."
            rewrites.isNotEmpty() ->
                "Upgraded Delivery Take: Record a 45-second take opening with the polished line from your Rewrites tab."
            !discourse.hasConcreteExample && topic != null ->
                "Story Anchor Drill: Explain \"$topic\" again in 45 seconds, starting with \"Last time I saw this happen...\""
            else ->
                "Vocal Melody Upgrade: Re-record for 45 seconds using a warm rise on your opening question and a crisp downward finish."
        }
    }

    private fun cleanSpaces(s: String): String =
        s.replace(Regex("\\s+([,.!?])"), "$1")
            .replace(Regex(",\\s*,"), ",")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    private fun polishSentenceCapitalization(s: String): String {
        val trimmed = s.trimStart(',', ' ', ';', '-')
        return trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
