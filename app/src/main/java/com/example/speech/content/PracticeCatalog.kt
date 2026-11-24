package com.example.speech.content

import com.example.speech.feedback.SelfCheckItem
import com.example.speech.pipeline.IssueOverride
import com.example.speech.pipeline.IssueOverrideEntry
import com.example.speech.pipeline.IssueOverrides
import com.example.speech.rules.IssueCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** A teachable technique from the content pack (ARC-03 §6.8). */
data class PracticeTechnique(
    val id: String,
    val name: String,
    val summary: String,
    val whyItWorks: String,
    val steps: List<String>,
    val activityIds: List<String>,
    val selfChecks: List<SelfCheckItem>,
    val overrides: List<IssueOverrideEntry>,
    val trackIds: List<String>,
    val pendingReview: Boolean
)

/** A speaking activity: a prompt practised through one evaluation template (ARC-03 §6.7). */
data class PracticeActivity(
    val id: String,
    val title: String,
    val goal: String,
    val prompt: String,
    val templateId: String,
    val referenceText: String? = null,
    val targetMinWords: Int? = null,
    val targetMaxWords: Int? = null,
    val framework: String? = null,
    val techniqueIds: List<String> = emptyList(),
    val overrides: List<IssueOverrideEntry> = emptyList(),
    val prepSeconds: Int? = null,
    val maxSeconds: Int,
    val estMinutes: Int = 2,
    val trackIds: List<String> = emptyList(),
    val pendingReview: Boolean = false,
    /** Built into the app (written from scratch), not from the content pack. */
    val builtIn: Boolean = false
) {
    val isReadAloud: Boolean get() = templateId == "TPL-READ" || templateId == "TPL-REPEAT"
}

/** Everything the practice screens need, with the content pack's precedence rules applied. */
class PracticeCatalog(
    val techniques: List<PracticeTechnique>,
    val activities: List<PracticeActivity>
) {
    private val techniqueById = techniques.associateBy { it.id }
    private val activityById = activities.associateBy { it.id }

    fun technique(id: String): PracticeTechnique? = techniqueById[id]
    fun activity(id: String): PracticeActivity? = activityById[id]

    fun activitiesFor(techniqueId: String): List<PracticeActivity> =
        activities.filter { techniqueId in it.techniqueIds }

    val quickPractice: List<PracticeActivity> get() = activities.filter { it.builtIn }

    /** Overrides for one attempt: activity beats technique, SUPPRESS beats REBAND, widest band wins (ARC-03 §6.8). */
    fun overridesFor(activity: PracticeActivity): Map<IssueCode, IssueOverride> =
        IssueOverrides.resolve(activity.overrides, activity.techniqueIds.mapNotNull { techniqueById[it]?.overrides })

    /** Self-check items of the practised techniques (things audio cannot measure; stored as self-reported). */
    fun selfChecksFor(activity: PracticeActivity): List<SelfCheckItem> =
        activity.techniqueIds.mapNotNull { techniqueById[it] }.flatMap { it.selfChecks }.distinctBy { it.id }

    /** The next activity of the same technique, for "Try the next one". */
    fun nextAfter(activity: PracticeActivity): PracticeActivity? {
        val siblings = activity.techniqueIds.firstOrNull()?.let { activitiesFor(it) }
            ?: activities.filter { it.builtIn == activity.builtIn }
        val i = siblings.indexOfFirst { it.id == activity.id }
        return if (i >= 0 && i + 1 < siblings.size) siblings[i + 1] else null
    }

    companion object {
        /** Default recording limits per template (seconds). */
        fun defaultMaxSeconds(templateId: String): Int = when (templateId) {
            "TPL-READ", "TPL-REPEAT" -> 60
            "TPL-STRUCT" -> 120
            "TPL-STORY" -> 150
            else -> 90
        }
    }
}

/**
 * Parses the content pack (`techniques.json` + `activities/trk-*.json`) for the practice screens. The pack is
 * validated in the build (`ContentPackAssetsTest`); parsing here is defensive and skips anything malformed.
 *
 * [includePendingReview] is false in release builds: items still `PENDING_REVIEW` never ship there (V-10).
 */
object PracticeCatalogParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(techniquesJson: String?, activityJsons: List<String>, includePendingReview: Boolean): PracticeCatalog {
        val allowed: (JsonObject) -> Boolean = { o ->
            o.str("status") == "ACTIVE" && (includePendingReview || o.str("licenceStatus") != "PENDING_REVIEW")
        }
        val techniques = techniquesJson?.let { array(it) }.orEmpty().filter(allowed).mapNotNull { runCatching { technique(it) }.getOrNull() }
        val techniqueIds = techniques.map { it.id }.toSet()
        val activities = activityJsons.flatMap { array(it) }.filter(allowed)
            .mapNotNull { runCatching { activity(it) }.getOrNull() }
            .map { a -> a.copy(techniqueIds = a.techniqueIds.filter { it in techniqueIds }) }
            .sortedBy { it.id }
        return PracticeCatalog(techniques, BuiltInPractice.ACTIVITIES + activities)
    }

    private fun array(text: String): List<JsonObject> =
        runCatching { json.parseToJsonElement(text).jsonArray.map { it.jsonObject } }.getOrDefault(emptyList())

    private fun technique(o: JsonObject) = PracticeTechnique(
        id = o.str("id")!!,
        name = o.str("name")!!,
        summary = o.str("summary").orEmpty(),
        whyItWorks = o.str("whyItWorks").orEmpty(),
        steps = o.strings("steps"),
        activityIds = o.strings("relatedActivityIds"),
        selfChecks = (o["selfCheckItems"] as? JsonArray).orEmpty().mapNotNull { e ->
            val s = e.jsonObject
            val id = s.str("id") ?: return@mapNotNull null
            SelfCheckItem(id, s.str("text") ?: return@mapNotNull null)
        },
        overrides = overrides(o),
        trackIds = o.strings("trackIds"),
        pendingReview = o.str("licenceStatus") == "PENDING_REVIEW"
    )

    private fun activity(o: JsonObject): PracticeActivity {
        val template = o.str("templateId")!!
        val range = o["targetRange"] as? JsonObject
        val wordsRange = range?.takeIf { it.str("unit") == "WORDS" }
        val secondsMax = range?.takeIf { it.str("unit") == "SECONDS" }?.int("max")
        val maxSeconds = o.int("maxSeconds")
            ?: secondsMax?.let { it + 15 }
            ?: PracticeCatalog.defaultMaxSeconds(template)
        return PracticeActivity(
            id = o.str("id")!!,
            title = o.str("title")!!,
            goal = o.str("goal").orEmpty(),
            prompt = o.str("prompt")!!,
            templateId = template,
            referenceText = o.str("referenceText"),
            targetMinWords = wordsRange?.int("min"),
            targetMaxWords = wordsRange?.int("max"),
            framework = o.str("framework"),
            techniqueIds = o.strings("techniqueIds"),
            overrides = overrides(o),
            prepSeconds = o.int("prepSeconds"),
            maxSeconds = maxSeconds,
            estMinutes = o.int("estMinutes") ?: 2,
            trackIds = o.strings("trackIds"),
            pendingReview = o.str("licenceStatus") == "PENDING_REVIEW"
        )
    }

    private fun overrides(o: JsonObject): List<IssueOverrideEntry> =
        (o["issueOverrides"] as? JsonArray).orEmpty().mapNotNull { e ->
            val entry = e.jsonObject
            val code = IssueCode.entries.firstOrNull { it.name == entry.str("issueCode") } ?: return@mapNotNull null
            when (entry.str("action")) {
                "SUPPRESS" -> IssueOverrideEntry(code, IssueOverride.Suppress)
                "REBAND" -> {
                    val band = entry["band"] as? JsonObject ?: return@mapNotNull null
                    val min = (band["min"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
                    val max = (band["max"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
                    if (min < max) IssueOverrideEntry(code, IssueOverride.Reband(min, max)) else null
                }
                else -> null
            }
        }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
    private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> = this ?: emptyList()
}

/**
 * Quick practice activities written from scratch for SpeakCraft (licence ORIGINAL; no channel-derived content),
 * so every build — including release builds before the content pack is reviewed — has something to practise.
 */
object BuiltInPractice {
    val ACTIVITIES: List<PracticeActivity> = listOf(
        PracticeActivity(
            id = "quick-introduce-yourself",
            title = "Introduce yourself",
            goal = "Give a clear one-minute introduction.",
            prompt = "Introduce yourself as if you were meeting a new colleague: who you are, what you do, and one thing you enjoy outside work.",
            templateId = "TPL-ANSWER",
            targetMinWords = 80, targetMaxWords = 160, maxSeconds = 75, prepSeconds = 10, estMinutes = 2,
            trackIds = listOf("TRK-EVERYDAY", "TRK-WORK"), builtIn = true
        ),
        PracticeActivity(
            id = "quick-explain-your-work",
            title = "Explain your work simply",
            goal = "Explain something you know well to a listener who does not.",
            prompt = "Explain what you do at work or in your studies to a friend who knows nothing about it. Use one everyday comparison.",
            templateId = "TPL-ANSWER",
            targetMinWords = 90, targetMaxWords = 180, maxSeconds = 90, prepSeconds = 15, estMinutes = 3,
            trackIds = listOf("TRK-WORK", "TRK-CLEAR"), builtIn = true
        ),
        PracticeActivity(
            id = "quick-opinion-prep",
            title = "Share an opinion with a clear structure",
            goal = "Give your point, a reason, an example and your point again.",
            prompt = "Is it better to work from home or from an office? Give your point, the main reason, a short example, then restate your point.",
            templateId = "TPL-STRUCT", framework = "PREP",
            targetMinWords = 90, targetMaxWords = 170, maxSeconds = 90, prepSeconds = 20, estMinutes = 3,
            trackIds = listOf("TRK-WORK", "TRK-INTERVIEW"), builtIn = true
        ),
        PracticeActivity(
            id = "quick-story-small-win",
            title = "Tell a short story",
            goal = "Tell a story with a beginning, a turning point and an ending.",
            prompt = "Tell the story of a small win you had recently: where you were, what went wrong or got hard, and how it turned out.",
            templateId = "TPL-STORY",
            targetMinWords = 120, targetMaxWords = 250, maxSeconds = 120, prepSeconds = 20, estMinutes = 3,
            trackIds = listOf("TRK-PRESENT", "TRK-EVERYDAY"), builtIn = true
        ),
        PracticeActivity(
            id = "quick-read-aloud",
            title = "Read aloud clearly",
            goal = "Read a short passage at a steady pace with clear endings.",
            prompt = "Read the passage aloud at a comfortable pace. Pause briefly at each full stop.",
            templateId = "TPL-READ",
            referenceText = "Every morning the small café on the corner opens at seven. The owner sets out the chairs, " +
                "turns on the coffee machine and writes the day's special on a board by the door. By eight, the first " +
                "regulars arrive, each with a favourite seat and a familiar order.",
            maxSeconds = 60, estMinutes = 1,
            trackIds = listOf("TRK-CLEAR", "TRK-EVERYDAY"), builtIn = true
        )
    )
}
