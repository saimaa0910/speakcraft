package com.example.data.learning

import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityDifficulty
import com.example.data.activity.ActivitySkillCategory
import com.example.data.activity.LearningPathsEngine
import com.example.data.activity.TechniqueMetadata
import com.example.data.model.CurriculumCatalog
import com.example.data.model.UserProfile
import com.example.data.model.VocabSeed
import kotlin.math.abs

data class DailyPracticeSentence(
    val id: String,
    val text: String,
    val focusWordOrTopic: String,
    val level: LearningLevel,
    val phoneticPrompt: String = "",
    val grammarFocus: String = "",
    val speakingTip: String = ""
)

data class DailyTechniqueFocus(
    val techniqueId: String,
    val title: String,
    val summary: String,
    val coreRule: String,
    val category: String,
    val suggestedDailyAction: String
)

data class DailyMissionTask(
    val id: String,
    val title: String,
    val description: String,
    val level: LearningLevel,
    val contextCategory: String, // Social, Meeting, Presentation, Self-Reflection
    val estimatedMinutes: Int = 5,
    val isCompleted: Boolean = false
)

data class DailyActivityItem(
    val activity: ActivityDefinition,
    val reason: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false
)

data class DailyAdaptiveCurriculumPackage(
    val dateKey: String,
    val userLevel: LearningLevel,
    val headlineTheme: String,
    val targetMinutes: Int,
    val dailyWords: List<VocabSeed>,
    val dailySentences: List<DailyPracticeSentence>,
    val dailyActivities: List<DailyActivityItem>,
    val dailyTechnique: DailyTechniqueFocus,
    val dailyMission: DailyMissionTask,
    val dailyTasks: List<DailyPracticeTask>,
    /** 0 for the first set of the day; +1 each time the user completes a whole set that day. */
    val setIndexToday: Int = 0,
    /** The user's position in the daily-set sequence that produced this set. */
    val sequencePosition: Int = 0
) {
    /** The content items of this set (persisted so the same set is rebuilt after a restart). */
    fun items(): DailySetItems = DailySetItems(
        level = userLevel,
        wordIds = dailyWords.map { it.id },
        sentenceIds = dailySentences.map { it.id },
        activityIds = dailyActivities.map { it.activity.activityId },
        techniqueId = dailyTechnique.techniqueId,
        missionId = dailyMission.id
    )
}

/**
 * Generates level-calibrated daily content that deterministically changes each day:
 * - Level-matched Vocabulary Words (with meanings, phonetics, example sentences)
 * - Level-matched Spoken Sentences (with pronunciation hints & grammar tips)
 * - Level-matched Activities & Exercises (from ActivityCatalogue rotating daily)
 * - Daily Technique of the Day (core communication rule & action cue)
 * - Daily Real-World Communication Mission (contextual challenge for today)
 *
 * Each set is chosen from the date plus the user's sequence position, avoiding recently completed items;
 * [DailySetRotation] advances the position when a set is completed, so the next set appears immediately.
 */
object DailyAdaptiveCurriculumEngine {

    val levelSentenceBank: Map<LearningLevel, List<DailyPracticeSentence>> = mapOf(
        LearningLevel.BEGINNER to listOf(
            DailyPracticeSentence(
                id = "sent_beg_01",
                text = "Every morning, I plan three clear priorities before checking my messages.",
                focusWordOrTopic = "Daily Habits",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Focus on crisp /p/ in 'plan' and clear /t/ in 'priorities'.",
                grammarFocus = "Simple Present for habitual actions",
                speakingTip = "Take a calm breath before the sentence; avoid rushing the ending."
            ),
            DailyPracticeSentence(
                id = "sent_beg_02",
                text = "I always take a calm breath before answering an unexpected question.",
                focusWordOrTopic = "Poise",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Lengthen the vowel in 'calm' /kɑːm/ and release 'breath' cleanly.",
                grammarFocus = "Adverb of frequency 'always' before main verb",
                speakingTip = "Hold a silent pause right after 'breath' to model composure."
            ),
            DailyPracticeSentence(
                id = "sent_beg_03",
                text = "She explained the new schedule clearly, and everyone understood immediately.",
                focusWordOrTopic = "Clarity",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Emphasize 'clearly' and articulate the /d/ in 'understood'.",
                grammarFocus = "Past Simple narrative coordination with 'and'",
                speakingTip = "Keep your pitch level; finish 'immediately' with downward inflection."
            ),
            DailyPracticeSentence(
                id = "sent_beg_04",
                text = "Let's meet on Monday at nine o'clock in the main conference room.",
                focusWordOrTopic = "Scheduling",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Contrast 'on Monday' with 'at nine' and 'in the room'.",
                grammarFocus = "Prepositions: on + day, at + time, in + place",
                speakingTip = "Group into three smooth rhythmic chunks: on Monday / at nine / in the room."
            ),
            DailyPracticeSentence(
                id = "sent_beg_05",
                text = "One of my biggest strengths is listening carefully before I speak.",
                focusWordOrTopic = "Self-Introduction",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Stress 'biggest strengths' and 'listening carefully'.",
                grammarFocus = "Singular verb 'is' with 'One of my...'",
                speakingTip = "Deliver with warm eye contact and open facial expression."
            ),
            DailyPracticeSentence(
                id = "sent_beg_06",
                text = "Could you please explain that again so I can take clear notes?",
                focusWordOrTopic = "Polite Clarification",
                level = LearningLevel.BEGINNER,
                phoneticPrompt = "Link 'Could you' naturally as /kʊdʒuː/.",
                grammarFocus = "Modal auxiliary 'could' for courteous workplace requests",
                speakingTip = "End with a gentle, friendly rise in pitch to invite cooperation."
            )
        ),
        LearningLevel.ELEMENTARY to listOf(
            DailyPracticeSentence(
                id = "sent_elem_01",
                text = "Although the project had a tight deadline, our team completed every deliverable on time.",
                focusWordOrTopic = "Contrast & Accomplishment",
                level = LearningLevel.ELEMENTARY,
                phoneticPrompt = "Smooth transition across the comma after 'deadline'.",
                grammarFocus = "Complex sentence with concessive clause 'Although...'",
                speakingTip = "Pause briefly at the comma to let listeners register the contrast."
            ),
            DailyPracticeSentence(
                id = "sent_elem_02",
                text = "When presenting to a group, keeping your gestures open builds trust immediately.",
                focusWordOrTopic = "Nonverbal Presence",
                level = LearningLevel.ELEMENTARY,
                phoneticPrompt = "Notice open vowel in 'open' /ˈoʊ.pən/ and crisp /t/ in 'trust'.",
                grammarFocus = "Gerund subject 'keeping your gestures open'",
                speakingTip = "Coordinate hands in the Power Zone as you speak the word 'open'."
            ),
            DailyPracticeSentence(
                id = "sent_elem_03",
                text = "Yesterday I walked into the review while everyone was discussing the new layout.",
                focusWordOrTopic = "Storytelling Scene",
                level = LearningLevel.ELEMENTARY,
                phoneticPrompt = "Pronounce 'walked' as /wɔːkt/ (voiceless -ed).",
                grammarFocus = "Past Simple interrupted by Past Continuous",
                speakingTip = "Lower your pitch slightly on 'discussing the new layout' to create scene depth."
            ),
            DailyPracticeSentence(
                id = "sent_elem_04",
                text = "I recommend breaking complex workflows into three straightforward steps.",
                focusWordOrTopic = "Problem Solving",
                level = LearningLevel.ELEMENTARY,
                phoneticPrompt = "Clear enunciation of 'straightforward' /streɪtˈfɔːr.wərd/.",
                grammarFocus = "Verb 'recommend' + gerund 'breaking'",
                speakingTip = "Use your fingers to count 'three straightforward steps' for visual emphasis."
            ),
            DailyPracticeSentence(
                id = "sent_elem_05",
                text = "Active listening helped us demystify the customer's real underlying concern.",
                focusWordOrTopic = "Demystify",
                level = LearningLevel.ELEMENTARY,
                phoneticPrompt = "Stress on the second syllable of 'demystify' /diːˈmɪs.tə.faɪ/.",
                grammarFocus = "Collocation: demystify a concern/problem",
                speakingTip = "Emphasize 'underlying concern' with deliberate vocal warmth."
            )
        ),
        LearningLevel.INTERMEDIATE to listOf(
            DailyPracticeSentence(
                id = "sent_inter_01",
                text = "The core insight I want to highlight is that simplicity consistently drives adoption.",
                focusWordOrTopic = "PREP: Point",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Resonant stress on 'core insight' and 'simplicity'.",
                grammarFocus = "Noun clause complement: 'is that [clause]'",
                speakingTip = "State the conclusion right upfront without hedging preamble."
            ),
            DailyPracticeSentence(
                id = "sent_inter_02",
                text = "The reason this matters is that automated testing saves ten engineering hours every week.",
                focusWordOrTopic = "PREP: Reason",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Clean /z/ in 'reason' and 'saves'.",
                grammarFocus = "Causal reasoning clause with quantified evidence",
                speakingTip = "Slow down by twenty percent as you deliver the specific number 'ten hours'."
            ),
            DailyPracticeSentence(
                id = "sent_inter_03",
                text = "For example, when we eliminated redundant sync meetings, sprint delivery accelerated.",
                focusWordOrTopic = "PREP: Example",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Crisp consonants in 'redundant' and 'sprint delivery'.",
                grammarFocus = "Signposting transition 'For example' followed by time clause",
                speakingTip = "Paint a vivid mental picture of the turning point."
            ),
            DailyPracticeSentence(
                id = "sent_inter_04",
                text = "If we tightened the opening hook, the audience would engage within the first ten seconds.",
                focusWordOrTopic = "Presentation Strategy",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Natural reduction in 'would engage' /wʊd ɪnˈɡeɪdʒ/.",
                grammarFocus = "Second conditional for diplomatic, persuasive recommendations",
                speakingTip = "Hold a one-second pause after 'hook' before resolving the sentence."
            ),
            DailyPracticeSentence(
                id = "sent_inter_05",
                text = "Let me articulate the clear trade-offs between rapid release speed and maintainability.",
                focusWordOrTopic = "Articulate",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Verb pronunciation of 'articulate' /ɑːrˈtɪk.jə.leɪt/.",
                grammarFocus = "Parallel noun pairing 'speed and maintainability'",
                speakingTip = "Use two distinct hand positions to represent each side of the trade-off."
            ),
            DailyPracticeSentence(
                id = "sent_inter_06",
                text = "Rather than responding defensively, we listened attentively and welcomed the criticism.",
                focusWordOrTopic = "Poise Under Fire",
                level = LearningLevel.INTERMEDIATE,
                phoneticPrompt = "Emphasize 'listened attentively' and 'welcomed'.",
                grammarFocus = "Prepositional contrast 'Rather than [gerund]...'",
                speakingTip = "End with calm downward pitch to convey emotional maturity."
            )
        ),
        LearningLevel.UPPER_INTERMEDIATE to listOf(
            DailyPracticeSentence(
                id = "sent_upper_01",
                text = "While I endorse the ambitious strategic roadmap, we must balance it against near-term capacity.",
                focusWordOrTopic = "Diplomatic Alignment",
                level = LearningLevel.UPPER_INTERMEDIATE,
                phoneticPrompt = "Nuanced stress on 'endorse' and 'near-term capacity'.",
                grammarFocus = "Concessive balance with 'While...'",
                speakingTip = "Deliver the concession warmly and the boundary decisively."
            ),
            DailyPracticeSentence(
                id = "sent_upper_02",
                text = "A pivotal factor in accelerating cross-functional alignment was transparent async communication.",
                focusWordOrTopic = "Pivotal",
                level = LearningLevel.UPPER_INTERMEDIATE,
                phoneticPrompt = "Crisp short /ɪ/ in 'pivotal' /ˈpɪv.ə.təl/.",
                grammarFocus = "Gerund complement in nominalized subject structure",
                speakingTip = "Slow your cadence on 'transparent async communication' for lasting impact."
            ),
            DailyPracticeSentence(
                id = "sent_upper_03",
                text = "Varying your vocal melody and strategic pacing prevents monotony during technical presentations.",
                focusWordOrTopic = "Vocal Modulation",
                level = LearningLevel.UPPER_INTERMEDIATE,
                phoneticPrompt = "Clear contrast between 'vocal melody' and 'strategic pacing'.",
                grammarFocus = "Compound gerund subject with singular agreement 'prevents'",
                speakingTip = "Inflect your own voice musically while speaking this sentence."
            ),
            DailyPracticeSentence(
                id = "sent_upper_04",
                text = "Ending declarative statements with a grounded downward tone reinforces professional authority.",
                focusWordOrTopic = "Vocal Image",
                level = LearningLevel.UPPER_INTERMEDIATE,
                phoneticPrompt = "Decisive descent on 'professional authority'.",
                grammarFocus = "Zero-uptalk declarative cadence",
                speakingTip = "Ensure your pitch steps downward on the final two syllables."
            )
        ),
        LearningLevel.ADVANCED to listOf(
            DailyPracticeSentence(
                id = "sent_adv_01",
                text = "To synthesize our findings, we face two competing imperatives: market velocity versus architectural resilience.",
                focusWordOrTopic = "Synthesize",
                level = LearningLevel.ADVANCED,
                phoneticPrompt = "Crisp sibilant in 'synthesize' /ˈsɪn.θə.saɪz/ and 'resilience' /rɪˈzɪl.jəns/.",
                grammarFocus = "Infinitive of purpose + colon expansion",
                speakingTip = "Frame with executive presence: steady eye contact and silent framing pauses."
            ),
            DailyPracticeSentence(
                id = "sent_adv_02",
                text = "True executive gravitas is not demonstrated by volume; it is forged through intentional silence and unshakeable poise.",
                focusWordOrTopic = "Gravitas",
                level = LearningLevel.ADVANCED,
                phoneticPrompt = "Latinate loanword 'gravitas' /ˈɡræv.ɪ.tɑːs/.",
                grammarFocus = "Antithetical parallel structure 'not by X; forged through Y'",
                speakingTip = "Hold a full 1.5-second silent pause across the semicolon."
            ),
            DailyPracticeSentence(
                id = "sent_adv_03",
                text = "By honoring the underlying nuances of stakeholder sentiment, we transformed skepticism into enduring sponsorship.",
                focusWordOrTopic = "Nuance",
                level = LearningLevel.ADVANCED,
                phoneticPrompt = "Pronounce 'nuances' /ˈnuː.ɑːns.ɪz/ with French nasal trace.",
                grammarFocus = "Participial cause-and-effect construction",
                speakingTip = "Speak with deep diaphragmatic resonance and measured tempo."
            ),
            DailyPracticeSentence(
                id = "sent_adv_04",
                text = "A resonant vision connects rigorous data architecture with the lived human experiences of our community.",
                focusWordOrTopic = "Resonant",
                level = LearningLevel.ADVANCED,
                phoneticPrompt = "Warm open vowels in 'resonant' /ˈrez.ən.ənt/ and 'rigorous'.",
                grammarFocus = "Abstract metaphorical bridge between data and empathy",
                speakingTip = "Anchor your hands at chest height and gesture outward on 'community'."
            )
        )
    )

    val levelMissionBank: Map<LearningLevel, List<DailyMissionTask>> = mapOf(
        LearningLevel.BEGINNER to listOf(
            DailyMissionTask("m_beg_01", "The 1-Second Silent Pause", "In your next conversation, hold a 1-second silent pause before replying instead of saying 'um'.", LearningLevel.BEGINNER, "Social", 3),
            DailyMissionTask("m_beg_02", "Describe Your Day in 60s", "Record 60 seconds aloud describing 3 specific tasks you completed today in clear English.", LearningLevel.BEGINNER, "Self-Reflection", 4),
            DailyMissionTask("m_beg_03", "FORD Opener Challenge", "Ask a colleague or friend about their family, occupation, recreation, or plans using the FORD method.", LearningLevel.BEGINNER, "Social", 5),
            DailyMissionTask("m_beg_04", "Downward Sentence Finisher", "End 3 statements today with decisive downward inflection without trailing off or uptalk.", LearningLevel.BEGINNER, "Workplace", 3)
        ),
        LearningLevel.ELEMENTARY to listOf(
            DailyMissionTask("m_elem_01", "3-Layer Follow-up Inquiry", "Ask 3 consecutive follow-up questions in a real conversation without shifting the focus to yourself.", LearningLevel.ELEMENTARY, "Social", 5),
            DailyMissionTask("m_elem_02", "Power-Zone Posture Check", "Maintain open hands in the navel-to-chest zone during your next video call or meeting update.", LearningLevel.ELEMENTARY, "Meeting", 5),
            DailyMissionTask("m_elem_03", "Sensory Anecdote Reliving", "Tell a 60-second micro-story about a recent experience including one sensory sound or sight.", LearningLevel.ELEMENTARY, "Storytelling", 4)
        ),
        LearningLevel.INTERMEDIATE to listOf(
            DailyMissionTask("m_inter_01", "The Impromptu PREP Sprint", "Answer an impromptu question or give a meeting update using Point -> Reason -> Example -> Point.", LearningLevel.INTERMEDIATE, "Meeting", 5),
            DailyMissionTask("m_inter_02", "BLUF 45-Second Briefing", "Deliver the Bottom Line Up Front within the first 15 seconds of a workplace explanation.", LearningLevel.INTERMEDIATE, "Workplace", 4),
            DailyMissionTask("m_inter_03", "Reflective Listening Mirror", "Summarize someone else's point in your own words ('If I heard you right, your main priority is...') before proposing ideas.", LearningLevel.INTERMEDIATE, "Meeting", 4),
            DailyMissionTask("m_inter_04", "Zero-Filler Speech Sprint", "Record a 90-second response with zero filler words using silent breath resets.", LearningLevel.INTERMEDIATE, "Self-Reflection", 5)
        ),
        LearningLevel.UPPER_INTERMEDIATE to listOf(
            DailyMissionTask("m_upper_01", "Tactful Constructive Dissent", "Express disagreement in a discussion by framing it constructively ('I appreciate that perspective, and another angle is...').", LearningLevel.UPPER_INTERMEDIATE, "Meeting", 5),
            DailyMissionTask("m_upper_02", "Vocal Melody Contrast Sprint", "Deliver a presentation segment intentionally alternating brisk setup pace with a slow, resonant punchline.", LearningLevel.UPPER_INTERMEDIATE, "Presentation", 6),
            DailyMissionTask("m_upper_03", "Nonverbal Room-Reading Pass", "Observe nonverbal cues in a live meeting and calibrate your speaking pace to match listener energy.", LearningLevel.UPPER_INTERMEDIATE, "Meeting", 5)
        ),
        LearningLevel.ADVANCED to listOf(
            DailyMissionTask("m_adv_01", "Executive Synthesis Briefing", "Distill a multi-hour discussion or complex document into a 2-minute strategic takeaway.", LearningLevel.ADVANCED, "Presentation", 5),
            DailyMissionTask("m_adv_02", "Poise Under Pressure", "Respond to a challenging question or critique with a 2-second silent pause, steady eye contact, and zero defensive posture.", LearningLevel.ADVANCED, "Meeting", 5),
            DailyMissionTask("m_adv_03", "Inspirational Narrative Capstone", "Deliver a sensory story that bridges technical execution with user empathy in under 90 seconds.", LearningLevel.ADVANCED, "Presentation", 6)
        )
    )

    private fun safeSeed(seedKey: String): Int {
        val hash = seedKey.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else abs(hash)
    }

    /**
     * Seed key of one daily set: the date alone for sequence position 0 (so a date always maps to the same
     * first set), otherwise date + position, so every completed set leads to a different next set.
     */
    fun seedKeyFor(dateKey: String, sequencePosition: Int): String =
        if (sequencePosition <= 0) dateKey else "$dateKey#$sequencePosition"

    /** Key used in task ids: unique per day and per set of that day. */
    fun setKeyFor(dateKey: String, setIndexToday: Int): String =
        if (setIndexToday <= 0) dateKey else "$dateKey-s${setIndexToday + 1}"

    private fun <T> rotate(list: List<T>, start: Int): List<T> =
        if (list.isEmpty()) list else List(list.size) { i -> list[Math.floorMod(start + i, list.size)] }

    /**
     * Picks [count] items in rotation order, skipping [recent] ids (most recent first). When the primary pool
     * runs out it continues with the [secondary] pool (adjacent level), and only then re-uses the least recently
     * used items. With no recent ids this is exactly "the first [count] items of the rotation".
     */
    private fun <T> pickAvoidingRecent(
        primary: List<T>,
        secondary: List<T>,
        count: Int,
        recent: List<String>,
        idOf: (T) -> String
    ): List<T> {
        val recentSet = recent.toSet()
        val picked = LinkedHashMap<String, T>()
        fun take(source: List<T>, allowRecent: Boolean) {
            for (item in source) {
                if (picked.size >= count) return
                val key = idOf(item)
                if (!allowRecent && key in recentSet) continue
                picked.putIfAbsent(key, item)
            }
        }
        take(primary, allowRecent = false)
        if (recentSet.isNotEmpty()) {
            take(secondary, allowRecent = false)
            if (picked.size < count) {
                val leastRecentFirst = (primary + secondary).distinctBy(idOf)
                    .sortedByDescending { recent.indexOf(idOf(it)) }
                take(leastRecentFirst, allowRecent = true)
            }
        }
        return picked.values.toList()
    }

    private fun adjacentLevels(level: LearningLevel): List<LearningLevel> =
        LearningLevel.entries.filter { abs(it.rank - level.rank) == 1 }

    private fun levelWordPool(level: LearningLevel): List<VocabSeed> {
        val bank = CurriculumCatalog.vocabularyBank
        val targetLevelLabel = level.label.lowercase()
        return bank.filter { it.level.lowercase() == targetLevelLabel }
            .ifEmpty {
                // Smooth fallback to adjacent level words
                when (level) {
                    LearningLevel.BEGINNER, LearningLevel.ELEMENTARY ->
                        bank.filter { it.level.equals("Beginner", true) || it.level.equals("Elementary", true) }
                    LearningLevel.INTERMEDIATE ->
                        bank.filter { it.level.equals("Intermediate", true) || it.level.equals("Elementary", true) }
                    else ->
                        bank.filter { it.level.equals("Upper Intermediate", true) || it.level.equals("Advanced", true) }
                }
            }.ifEmpty { bank }
    }

    /**
     * Retrieves daily vocabulary words calibrated to user level and rotating daily.
     */
    fun getDailyWords(dateKey: String, level: LearningLevel, count: Int = 3): List<VocabSeed> =
        selectWords(dateKey, level, count, emptyList())

    private fun selectWords(seedKey: String, level: LearningLevel, count: Int, recent: List<String>): List<VocabSeed> {
        val primary = levelWordPool(level)
        if (primary.isEmpty()) return emptyList()
        val n = count.coerceIn(1, primary.size)
        val seed = safeSeed(seedKey)
        val adjacentLabels = adjacentLevels(level).map { it.label.lowercase() }.toSet()
        val secondary = CurriculumCatalog.vocabularyBank.filter { it.level.lowercase() in adjacentLabels }
        return pickAvoidingRecent(rotate(primary, seed), rotate(secondary, seed), n, recent) { it.id }
    }

    /**
     * Retrieves daily practice sentences calibrated to user level and rotating daily.
     */
    fun getDailySentences(dateKey: String, level: LearningLevel, count: Int = 3): List<DailyPracticeSentence> =
        selectSentences(dateKey, level, count, emptyList())

    private fun selectSentences(
        seedKey: String,
        level: LearningLevel,
        count: Int,
        recent: List<String>
    ): List<DailyPracticeSentence> {
        val pool = levelSentenceBank[level]
            ?: levelSentenceBank[LearningLevel.INTERMEDIATE]
            ?: emptyList()
        if (pool.isEmpty()) return emptyList()
        val start = Math.floorMod(safeSeed(seedKey) * 31, pool.size)
        val n = count.coerceIn(1, pool.size)
        val secondary = adjacentLevels(level).flatMap { levelSentenceBank[it].orEmpty() }
        return pickAvoidingRecent(rotate(pool, start), rotate(secondary, start), n, recent) { it.id }
    }

    /**
     * Retrieves daily activities calibrated to user level and rotating daily.
     */
    fun getDailyActivities(
        dateKey: String,
        level: LearningLevel,
        profile: UserProfile? = null,
        completedTodayIds: Set<String> = emptySet(),
        count: Int = 3
    ): List<DailyActivityItem> = selectActivities(dateKey, level, completedTodayIds, count, emptyList())

    private fun selectActivities(
        seedKey: String,
        level: LearningLevel,
        completedTodayIds: Set<String>,
        count: Int,
        recent: List<String>
    ): List<DailyActivityItem> {
        val allActivities = ActivityCatalogue.allActivities
        val desiredDifficulty = desiredDifficultyFor(level)

        // Candidates matching desired difficulty or adjacent
        val base = allActivities.filter { act ->
            act.activityId !in completedTodayIds &&
                (act.difficulty == desiredDifficulty || abs(act.difficulty.rank - desiredDifficulty.rank) <= 1)
        }.ifEmpty { allActivities }
        // Skip recently completed activities while enough others remain
        val recentSet = recent.toSet()
        val fresh = base.filter { it.activityId !in recentSet }
        val candidates = if (fresh.size >= count) fresh else base

        val seed = safeSeed(seedKey)
        val selected = mutableListOf<DailyActivityItem>()
        val usedCategories = mutableSetOf<ActivitySkillCategory>()

        // Rotate through distinct skill pillars
        val sortedCandidates = candidates.sortedBy { act ->
            ((act.activityId.hashCode() xor seed) and 0x7FFFFFFF) % 100
        }

        for (act in sortedCandidates) {
            if (selected.size >= count) break
            if (selected.any { it.activity.activityId == act.activityId }) continue
            // Prefer diverse skill categories
            if (act.skillCategory in usedCategories && selected.size < count - 1) continue
            usedCategories.add(act.skillCategory)
            selected += activityItem(act, level, completedTodayIds)
        }
        // Small candidate pools: fill without the diversity preference
        for (act in sortedCandidates) {
            if (selected.size >= count) break
            if (selected.any { it.activity.activityId == act.activityId }) continue
            selected += activityItem(act, level, completedTodayIds)
        }
        return selected
    }

    private fun desiredDifficultyFor(level: LearningLevel): ActivityDifficulty = when (level) {
        LearningLevel.BEGINNER, LearningLevel.ELEMENTARY -> ActivityDifficulty.BEGINNER
        LearningLevel.INTERMEDIATE -> ActivityDifficulty.INTERMEDIATE
        else -> ActivityDifficulty.ADVANCED
    }

    private fun activityItem(
        act: ActivityDefinition,
        level: LearningLevel,
        completedTodayIds: Set<String> = emptySet()
    ): DailyActivityItem {
        val reason = when (act.skillCategory) {
            ActivitySkillCategory.ENGLISH_FLUENCY -> "Daily ${level.label} spontaneous speaking workout"
            ActivitySkillCategory.PRONUNCIATION -> "Targeted articulation & sound precision drill"
            ActivitySkillCategory.VOICE_MASTERY -> "Vocal melody, breath support & pause training"
            ActivitySkillCategory.SPEAKING_CLARITY -> "Concise structure & PREP framework practice"
            ActivitySkillCategory.CONVERSATION -> "Real-world conversational agility simulation"
            ActivitySkillCategory.STORYTELLING -> "Sensory storytelling & narrative progression"
            ActivitySkillCategory.PUBLIC_SPEAKING -> "High-impact delivery & scriptless poise"
            ActivitySkillCategory.PROFESSIONAL -> "Executive communication & workplace clarity"
            else -> "Core communication habit builder"
        }
        return DailyActivityItem(
            activity = act,
            reason = reason,
            durationMinutes = act.duration,
            isCompleted = act.activityId in completedTodayIds
        )
    }

    private fun appropriateTechniques(level: LearningLevel): List<TechniqueMetadata> {
        val catalog = LearningPathsEngine.techniquesCatalog.values.toList()
        return when (level) {
            LearningLevel.BEGINNER, LearningLevel.ELEMENTARY ->
                catalog.filter { it.id in listOf("calm-clear-speaking", "articulation-warmup", "conversation-threads", "pause-not-filler") }
            LearningLevel.INTERMEDIATE ->
                catalog.filter { it.id in listOf("prep-answer", "explain-complex-ideas", "active-listening", "relive-the-story", "purposeful-gestures") }
            else ->
                catalog.filter { it.id in listOf("three-channels", "warm-and-assured", "vocal-variety", "adapt-to-listener-style") }
        }.ifEmpty { catalog }
    }

    private fun techniqueFocus(selected: TechniqueMetadata): DailyTechniqueFocus {
        val action = when (selected.id) {
            "prep-answer" -> "Use Point -> Reason -> Example -> Point in your next meeting or challenge response."
            "pause-not-filler" -> "Hold silent 1-second pauses whenever formulating your next thought."
            "calm-clear-speaking" -> "Breathe low into your diaphragm before speaking; pace yourself at 140 WPM."
            "articulation-warmup" -> "Exaggerate lip and jaw movements during your morning warm-up."
            "active-listening" -> "Summarize the other person's main point in one sentence before proposing your view."
            "relive-the-story" -> "Include sensory sights, sounds, and direct quotes in an everyday anecdote."
            "purposeful-gestures" -> "Keep your hands active in the Power Zone between your navel and chest."
            "three-channels" -> "Synchronize your vocal warmth, posture, and word choice for unified gravitas."
            else -> "Apply this core communication lever throughout today's conversations."
        }
        return DailyTechniqueFocus(
            techniqueId = selected.id,
            title = selected.name,
            summary = selected.summary,
            coreRule = selected.coreRule,
            category = selected.category,
            suggestedDailyAction = action
        )
    }

    /**
     * Retrieves daily technique of the day calibrated to user level and rotating daily.
     */
    fun getDailyTechnique(dateKey: String, level: LearningLevel): DailyTechniqueFocus =
        selectTechnique(dateKey, level, emptyList())

    private fun selectTechnique(seedKey: String, level: LearningLevel, recent: List<String>): DailyTechniqueFocus {
        val appropriate = appropriateTechniques(level)
        val rotated = rotate(appropriate, safeSeed(seedKey) % appropriate.size)
        val selected = pickAvoidingRecent(rotated, emptyList(), 1, recent) { it.id }.first()
        return techniqueFocus(selected)
    }

    /**
     * Retrieves daily real-world mission calibrated to user level and rotating daily.
     */
    fun getDailyMission(dateKey: String, level: LearningLevel): DailyMissionTask =
        selectMission(dateKey, level, emptyList())

    private fun selectMission(seedKey: String, level: LearningLevel, recent: List<String>): DailyMissionTask {
        val missions = levelMissionBank[level]
            ?: levelMissionBank[LearningLevel.INTERMEDIATE]
            ?: emptyList()

        if (missions.isEmpty()) {
            return DailyMissionTask(
                id = "m_default_$seedKey",
                title = "1-Second Silent Pause",
                description = "Hold a deliberate pause before answering today.",
                level = level,
                contextCategory = "Workplace"
            )
        }
        val rotated = rotate(missions, safeSeed(seedKey) % missions.size)
        return pickAvoidingRecent(rotated, emptyList(), 1, recent) { it.id }.first()
    }

    private fun themeFor(level: LearningLevel): String = when (level) {
        LearningLevel.BEGINNER -> "Foundation, Clear Sentences & Calm Breathing"
        LearningLevel.ELEMENTARY -> "Smooth Phrasing, Open Body Language & Everyday Conversations"
        LearningLevel.INTERMEDIATE -> "Structured PREP Explanations & Impromptu Speaking Poise"
        LearningLevel.UPPER_INTERMEDIATE -> "Executive Cadence, Influence & Diplomatic Clarity"
        LearningLevel.ADVANCED -> "Executive Gravitas, Strategic Nuance & Compelling Presence"
    }

    /**
     * Generates a complete daily curriculum package combining:
     * - Words
     * - Sentences
     * - Activities
     * - Technique of the Day
     * - Real-World Mission
     * - Daily Tasks
     *
     * [sequencePosition] is the user's position in the daily-set sequence (it advances each time a whole set
     * is completed) and [recentItemIds] are recently completed items to avoid, most recent first. With the
     * defaults the package depends on the date alone.
     */
    fun generateDailyPackage(
        dateKey: String,
        level: LearningLevel,
        profile: UserProfile? = null,
        completedTodayIds: Set<String> = emptySet(),
        targetMinutes: Int = 20,
        sequencePosition: Int = 0,
        setIndexToday: Int = 0,
        recentItemIds: List<String> = emptyList()
    ): DailyAdaptiveCurriculumPackage {
        val seedKey = seedKeyFor(dateKey, sequencePosition)
        return assemblePackage(
            dateKey = dateKey,
            level = level,
            targetMinutes = targetMinutes,
            setIndexToday = setIndexToday,
            sequencePosition = sequencePosition,
            words = selectWords(seedKey, level, 3, recentItemIds),
            sentences = selectSentences(seedKey, level, 3, recentItemIds),
            activities = selectActivities(seedKey, level, completedTodayIds, 3, recentItemIds),
            technique = selectTechnique(seedKey, level, recentItemIds),
            mission = selectMission(seedKey, level, recentItemIds)
        )
    }

    /**
     * Rebuilds a stored daily set from its item ids (so the same set survives restarts and content order).
     * Returns null when an item no longer exists; the caller then generates a fresh set.
     */
    fun rebuildPackage(
        dateKey: String,
        items: DailySetItems,
        setIndexToday: Int,
        sequencePosition: Int,
        targetMinutes: Int = 20
    ): DailyAdaptiveCurriculumPackage? {
        val level = items.level
        val wordsById = CurriculumCatalog.vocabularyBank.associateBy { it.id }
        val sentencesById = levelSentenceBank.values.flatten().associateBy { it.id }
        val missionsById = levelMissionBank.values.flatten().associateBy { it.id }
        val words = items.wordIds.map { wordsById[it] ?: return null }
        val sentences = items.sentenceIds.map { sentencesById[it] ?: return null }
        val activities = items.activityIds.map { id ->
            activityItem(ActivityCatalogue.getActivityById(id) ?: return null, level)
        }
        val technique = LearningPathsEngine.techniquesCatalog[items.techniqueId]?.let { techniqueFocus(it) } ?: return null
        val mission = missionsById[items.missionId] ?: return null
        return assemblePackage(dateKey, level, targetMinutes, setIndexToday, sequencePosition, words, sentences, activities, technique, mission)
    }

    private fun assemblePackage(
        dateKey: String,
        level: LearningLevel,
        targetMinutes: Int,
        setIndexToday: Int,
        sequencePosition: Int,
        words: List<VocabSeed>,
        sentences: List<DailyPracticeSentence>,
        activities: List<DailyActivityItem>,
        technique: DailyTechniqueFocus,
        mission: DailyMissionTask
    ): DailyAdaptiveCurriculumPackage {
        val setKey = setKeyFor(dateKey, setIndexToday)

        // Generate matching daily practice tasks
        val tasks = mutableListOf<DailyPracticeTask>()
        tasks += DailyPracticeTask(
            id = "task_vocab_$setKey",
            title = "Learn 3 ${level.label} Words (${words.joinToString(", ") { it.word }})",
            description = "Study meanings, phonetics, and collocations; practice speaking each word.",
            category = "Vocabulary",
            durationMinutes = 5,
            targetRoute = "vocabulary",
            skillFocus = "Vocabulary",
            itemIds = words.map { it.id }
        )
        tasks += DailyPracticeTask(
            id = "task_sentence_$setKey",
            title = "Speak 3 Spoken Sentences Aloud",
            description = "Practice level-calibrated sentence rhythm, pronunciation cues, and grammar focus.",
            category = "Speaking",
            durationMinutes = 5,
            targetRoute = "grammar_speaking",
            skillFocus = "Fluency",
            itemIds = sentences.map { it.id }
        )
        activities.forEach { actItem ->
            val actId = actItem.activity.activityId
            tasks += DailyPracticeTask(
                id = "task_act_${setKey}_$actId",
                title = "Drill: ${actItem.activity.title}",
                description = actItem.reason,
                category = actItem.activity.skillCategory.label,
                durationMinutes = actItem.durationMinutes,
                targetRoute = "activity/$actId",
                skillFocus = actItem.activity.skillCategory.label,
                isCompleted = actItem.isCompleted,
                linkedActivityId = actId,
                linkedTechniqueIds = LearningPathsEngine.activityToTechniques[actId].orEmpty(),
                itemIds = listOf(actId)
            )
        }
        tasks += DailyPracticeTask(
            id = "task_technique_$setKey",
            title = "Apply Technique: ${technique.title}",
            description = technique.suggestedDailyAction,
            category = "Technique",
            durationMinutes = 3,
            targetRoute = "coach_practice",
            skillFocus = technique.category,
            linkedTechniqueIds = listOf(technique.techniqueId),
            itemIds = listOf(technique.techniqueId)
        )
        tasks += DailyPracticeTask(
            id = "task_mission_$setKey",
            title = "Mission: ${mission.title}",
            description = mission.description,
            category = "Mission",
            durationMinutes = mission.estimatedMinutes,
            targetRoute = "daily_practice",
            skillFocus = "Confidence",
            itemIds = listOf(mission.id)
        )

        return DailyAdaptiveCurriculumPackage(
            dateKey = dateKey,
            userLevel = level,
            headlineTheme = themeFor(level),
            targetMinutes = targetMinutes,
            dailyWords = words,
            dailySentences = sentences,
            dailyActivities = activities,
            dailyTechnique = technique,
            dailyMission = mission,
            dailyTasks = tasks,
            setIndexToday = setIndexToday,
            sequencePosition = sequencePosition
        )
    }
}
