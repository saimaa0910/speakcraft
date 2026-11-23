package com.example.speech.content

import com.example.speech.feedback.TipPackLoadResult
import com.example.speech.feedback.TipPackLoader
import com.example.speech.feedback.TipPackOptions
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.AttemptInput
import com.example.speech.pipeline.IssueOverride
import com.example.speech.pipeline.IssueOverrideEntry
import com.example.speech.pipeline.IssueOverrides
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.rules.IssueCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Content build validation for the shipped pack `app/src/main/assets/content/` (ARC-03 §6; rules V-01…V-22).
 * The pack is the curriculum re-expressed in SpeakCraft's own words (D-08). Every item is `PENDING_REVIEW`
 * until a human reviewer adds the specific source video and signs off, so release builds keep the built-in
 * tips (V-10) while debug builds use the pack.
 */
class ContentPackAssetsTest {

    private val moduleDir: File = File(System.getProperty("user.dir") ?: ".").let { cwd ->
        if (File(cwd, "src/main/assets").exists()) cwd else File(cwd, "app")
    }
    private val contentDir = File(moduleDir, "src/main/assets/content")
    private val repoRoot: File = moduleDir.absoluteFile.parentFile

    private val blocklist: Set<String> by lazy {
        File(repoRoot, "content/branding-blocklist.txt").readLines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
    }

    private fun objects(file: File): List<JsonObject> =
        Json.parseToJsonElement(file.readText()).jsonArray.map { it.jsonObject }

    private val techniques by lazy { objects(File(contentDir, "techniques.json")) }
    private val tips by lazy { objects(File(contentDir, "tips.json")) }
    private val activities by lazy {
        File(contentDir, "activities").listFiles { f -> f.name.endsWith(".json") }!!.sortedBy { it.name }.flatMap { objects(it) }
    }

    private val templates = setOf("TPL-READ", "TPL-REPEAT", "TPL-ANSWER", "TPL-STRUCT", "TPL-STORY")
    private val allTemplates = templates + setOf("TPL-ROLE", "TPL-WORD-SAY", "TPL-WORD-USE", "TPL-WORD-OWN", "TPL-WORD-RECALL")
    private val tracks = setOf("TRK-EVERYDAY", "TRK-WORK", "TRK-INTERVIEW", "TRK-PRESENT", "TRK-CLEAR")
    private val levels = listOf("L1", "L2", "L3", "L4", "L5")
    private val targetMetricKeys = setOf("M-WPM", "M-PAUSE", "M-FILL", "M-LEX", "M-HEDGE", "M-LEN", "M-STRUCT", "M-MATCH", "M-LEVEL")
    private val idPattern = Regex("^[a-z0-9][a-z0-9-]{1,63}$")
    private val commonKeys = setOf("id", "status", "introducedIn", "retiredIn", "provenance", "licenceStatus", "attributionText")
    private val techniqueKeys = commonKeys + setOf(
        "name", "summary", "whyItWorks", "steps", "tipIds", "relatedActivityIds", "targetMetrics", "selfCheckItems",
        "issueOverrides", "trackIds", "levelMin", "levelMax"
    )
    private val activityKeys = commonKeys + setOf(
        "trackIds", "templateId", "levelMin", "levelMax", "title", "goal", "prompt", "referenceText", "targetRange",
        "framework", "expectedElements", "prepSeconds", "maxSeconds", "targetWordIds", "techniqueIds", "general",
        "estMinutes", "tags", "roleplay", "issueOverrides"
    )
    private val provenanceKeys = setOf(
        "sourceVideoId", "sourceUrl", "timestampStart", "timestampEnd", "techniqueNameAsTaught", "verifiedBy", "verifiedOn", "notes"
    )

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()

    private fun check(problems: MutableList<String>, condition: Boolean, message: () -> String) {
        if (!condition) problems += message()
    }

    private fun assertNoProblems(problems: List<String>) =
        assertTrue("${problems.size} problems:\n" + problems.take(40).joinToString("\n"), problems.isEmpty())

    private fun commonChecks(o: JsonObject, allowed: Set<String>, problems: MutableList<String>) {
        val id = o.str("id") ?: "?"
        (o.keys - allowed).forEach { problems += "$id: unknown key $it (V-01/V-13)" }
        check(problems, idPattern.matches(id)) { "$id: id format (V-21)" }
        check(problems, o.str("status") == "ACTIVE" || o.str("status") == "RETIRED") { "$id: status" }
        check(problems, (o["introducedIn"] as? JsonPrimitive)?.intOrNull?.let { it >= 1 } == true) { "$id: introducedIn" }
        check(problems, o["attributionText"] == JsonNull) { "$id: attributionText must be null (V-11)" }
        val licence = o.str("licenceStatus")
        check(problems, licence in setOf("ORIGINAL", "ORIGINAL_REEXPRESSION", "PENDING_REVIEW")) { "$id: licenceStatus $licence (V-11)" }
        (o["provenance"] as? JsonObject)?.let { p ->
            (p.keys - provenanceKeys).forEach { problems += "$id: unknown provenance key $it" }
        } ?: check(problems, licence == "PENDING_REVIEW") { "$id: provenance required" }
        if (licence == "PENDING_REVIEW") {
            // Sources are never invented: a pending item carries no link until a reviewer adds one.
            val p = o["provenance"] as? JsonObject
            check(problems, p?.get("sourceUrl") == null && p?.get("sourceVideoId") == null) { "$id: pending item must not carry a source link" }
        }
    }

    private fun levelsValid(o: JsonObject): Boolean {
        val min = levels.indexOf(o.str("levelMin"))
        val max = levels.indexOf(o.str("levelMax"))
        return min >= 0 && max >= min
    }

    private fun overrides(o: JsonObject, problems: MutableList<String>): List<IssueOverrideEntry> {
        val id = o.str("id") ?: "?"
        return (o["issueOverrides"] as? JsonArray).orEmpty().mapNotNull { e ->
            val entry = e.jsonObject
            (entry.keys - setOf("issueCode", "action", "band")).forEach { problems += "$id: unknown issueOverrides key $it" }
            val code = IssueCode.entries.firstOrNull { it.name == entry.str("issueCode") }
            val band = entry["band"] as? JsonObject
            when {
                code == null -> { problems += "$id: unknown issueCode ${entry.str("issueCode")} (V-22)"; null }
                entry.str("action") == "SUPPRESS" && band == null -> IssueOverrideEntry(code, IssueOverride.Suppress)
                entry.str("action") == "REBAND" && band != null -> {
                    val min = (band["min"] as? JsonPrimitive)?.doubleOrNull
                    val max = (band["max"] as? JsonPrimitive)?.doubleOrNull
                    if (min == null || max == null || min >= max) { problems += "$id: REBAND band invalid (V-22)"; null }
                    else IssueOverrideEntry(code, IssueOverride.Reband(min, max))
                }
                else -> { problems += "$id: issueOverrides action/band mismatch (V-22)"; null }
            }
        }.also { entries -> problems += IssueOverrides.validate(id, entries) }
    }

    @Test
    fun tcContent001_tips_loadInDebug_andReleaseKeepsBuiltInTipsOnlyBecauseOfPendingReview() {
        val json = File(contentDir, "tips.json").readText()
        val ids = techniques.mapNotNull { it.str("id") }.toSet()
        val debug = TipPackLoader.load(json, TipPackOptions(false, ids, allTemplates, blocklist))
        assertTrue("debug load: $debug", debug is TipPackLoadResult.Loaded)
        assertEquals(tips.count { it.str("status") == "ACTIVE" }, (debug as TipPackLoadResult.Loaded).tips.size)

        val release = TipPackLoader.load(json, TipPackOptions(true, ids, allTemplates, blocklist))
        if (release is TipPackLoadResult.Rejected) {
            val other = release.violations.filter { it.rule != "V-10" }
            assertTrue("release violations other than V-10: $other", other.isEmpty())
        }
    }

    @Test
    fun tcContent002_techniques() {
        val problems = mutableListOf<String>()
        val tipById = tips.associateBy { it.str("id") }
        val activityById = activities.associateBy { it.str("id") }
        assertEquals("one record per curriculum technique", 15, techniques.size)
        for (t in techniques) {
            val id = t.str("id")!!
            commonChecks(t, techniqueKeys, problems)
            check(problems, (t.str("name")?.length ?: 0) in 1..60) { "$id: name 1–60" }
            check(problems, (t.str("summary")?.length ?: 0) in 1..140) { "$id: summary 1–140" }
            check(problems, (t.str("whyItWorks")?.length ?: 0) in 1..400) { "$id: whyItWorks 1–400" }
            val steps = t.strings("steps")
            check(problems, steps.size in 2..7 && steps.all { it.length in 1..200 }) { "$id: steps 2–7, each ≤ 200" }
            val tipIds = t.strings("tipIds")
            check(problems, tipIds.isNotEmpty() && tipIds.all { it in tipById }) { "$id: tipIds must exist (V-03)" }
            check(problems, tipIds.any { tid ->
                tipById[tid]?.let { it.str("techniqueId") == id && (it["triggers"] as? JsonArray)?.isEmpty() == true } == true
            }) { "$id: needs a general tip in tipIds (V-15)" }
            val related = t.strings("relatedActivityIds")
            check(problems, related.isNotEmpty()) { "$id: relatedActivityIds ≥ 1" }
            related.forEach { aid ->
                check(problems, activityById[aid]?.strings("techniqueIds")?.contains(id) == true) { "$id: activity $aid must list it back (V-03)" }
            }
            check(problems, t.strings("targetMetrics").all { it in targetMetricKeys }) { "$id: targetMetrics (V-17)" }
            val selfChecks = (t["selfCheckItems"] as? JsonArray).orEmpty().map { it.jsonObject }
            check(problems, selfChecks.size <= 6) { "$id: ≤ 6 self-check items" }
            selfChecks.forEach { s ->
                check(problems, s.keys == setOf("id", "text") && idPattern.matches(s.str("id") ?: "") && (s.str("text")?.length ?: 0) in 1..120) {
                    "$id: self-check item ${s.str("id")}"
                }
            }
            check(problems, t.strings("trackIds").let { it.isNotEmpty() && it.all { k -> k in tracks } }) { "$id: trackIds" }
            check(problems, levelsValid(t)) { "$id: levels (V-05)" }
            overrides(t, problems)
        }
        assertNoProblems(problems)
    }

    @Test
    fun tcContent003_activities() {
        val problems = mutableListOf<String>()
        val techniqueIds = techniques.mapNotNull { it.str("id") }.toSet()
        assertTrue("activities present", activities.size >= techniques.size)
        for (a in activities) {
            val id = a.str("id")!!
            commonChecks(a, activityKeys, problems)
            val template = a.str("templateId")
            check(problems, template in templates) { "$id: templateId $template" }
            check(problems, (a.str("title")?.length ?: 0) in 1..60) { "$id: title 1–60" }
            check(problems, (a.str("goal")?.length ?: 0) in 1..140) { "$id: goal 1–140" }
            check(problems, (a.str("prompt")?.length ?: 0) in 1..600) { "$id: prompt 1–600" }
            check(problems, a.strings("trackIds").let { it.isNotEmpty() && it.all { k -> k in tracks } }) { "$id: trackIds" }
            check(problems, levelsValid(a)) { "$id: levels (V-05)" }
            when (template) {
                "TPL-READ", "TPL-REPEAT" -> {
                    check(problems, (a.str("referenceText")?.length ?: 0) in 1..400) { "$id: referenceText 1–400 (V-04)" }
                    check(problems, a["targetRange"] == null && a["framework"] == null) { "$id: READ/REPEAT has no targetRange/framework" }
                }
                else -> {
                    val range = a["targetRange"] as? JsonObject
                    val unit = range?.str("unit")
                    val min = (range?.get("min") as? JsonPrimitive)?.intOrNull
                    val max = (range?.get("max") as? JsonPrimitive)?.intOrNull
                    check(problems, unit in setOf("WORDS", "SECONDS") && min != null && max != null && min in 1 until max) { "$id: targetRange (V-04)" }
                    check(problems, a["referenceText"] == null) { "$id: referenceText only for READ/REPEAT" }
                }
            }
            if (template == "TPL-STRUCT") {
                val expected = when (a.str("framework")) {
                    "STAR" -> listOf("Situation", "Task", "Action", "Result")
                    "PREP" -> listOf("Point", "Reason", "Example", "Point_Restate")
                    else -> null
                }
                check(problems, expected != null && a.strings("expectedElements") == expected) { "$id: framework/expectedElements (V-04)" }
            } else {
                check(problems, a["framework"] == null && a["expectedElements"] == null) { "$id: framework only for TPL-STRUCT" }
            }
            val general = (a["general"] as? JsonPrimitive)?.booleanOrNull == true
            val linked = a.strings("techniqueIds")
            check(problems, general || linked.isNotEmpty()) { "$id: needs a technique or general (V-09)" }
            check(problems, linked.all { it in techniqueIds }) { "$id: unknown techniqueIds $linked (V-03)" }
            check(problems, (a["estMinutes"] as? JsonPrimitive)?.intOrNull in 1..10) { "$id: estMinutes 1–10" }
            check(problems, a.strings("tags").size <= 8) { "$id: ≤ 8 tags" }
            overrides(a, problems)
        }
        assertNoProblems(problems)
    }

    @Test
    fun tcContent004_displayedText_hasNoBrandingLinksOrPlaceholders() {
        val problems = mutableListOf<String>()
        val placeholder = Regex("\\b(TODO|TBD|FIXME|lorem)\\b", RegexOption.IGNORE_CASE)
        val html = Regex("<\\s*/?\\s*[a-zA-Z][^>]*>")
        val blocked = blocklist.map { term -> term to Regex("(?i)(^|[^\\p{L}\\p{N}])${Regex.escape(term)}($|[^\\p{L}\\p{N}])") }

        fun displayed(id: String, field: String, text: String) {
            check(problems, text == text.trim()) { "$id.$field: not trimmed (V-18)" }
            check(problems, !placeholder.containsMatchIn(text)) { "$id.$field: placeholder text (V-18)" }
            check(problems, !html.containsMatchIn(text)) { "$id.$field: HTML (V-18)" }
            check(problems, !text.contains("http", ignoreCase = true) && !text.contains("youtube", ignoreCase = true)) { "$id.$field: link in displayed text" }
            blocked.forEach { (term, regex) -> check(problems, !regex.containsMatchIn(text)) { "$id.$field: blocked term '$term' (V-14)" } }
        }

        techniques.forEach { t ->
            val id = t.str("id")!!
            listOf("name", "summary", "whyItWorks").forEach { f -> t.str(f)?.let { displayed(id, f, it) } }
            t.strings("steps").forEachIndexed { i, s -> displayed(id, "steps[$i]", s) }
            (t["selfCheckItems"] as? JsonArray).orEmpty().forEach { s -> displayed(id, "selfCheck", s.jsonObject.str("text")!!) }
        }
        activities.forEach { a ->
            val id = a.str("id")!!
            listOf("title", "goal", "prompt", "referenceText").forEach { f -> a.str(f)?.let { displayed(id, f, it) } }
            a.strings("tags").forEach { displayed(id, "tags", it) }
        }
        tips.forEach { t -> displayed(t.str("id")!!, "text", t.str("text")!!) }
        assertNoProblems(problems)
    }

    @Test
    fun tcContent005_idsUniqueAcrossAllItemTypes() {
        val ids = techniques.map { it.str("id")!! } + activities.map { it.str("id")!! } + tips.map { it.str("id")!! } +
            techniques.flatMap { t -> (t["selfCheckItems"] as? JsonArray).orEmpty().map { it.jsonObject.str("id")!! } }
        val duplicates = ids.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate ids (V-02): $duplicates", duplicates.isEmpty())
    }

    @Test
    fun tcContent006_manifest_countsAndChecksums() {
        val manifest = Json.parseToJsonElement(File(contentDir, "manifest.json").readText()).jsonObject
        assertEquals("speakcraft-core", manifest.str("packId"))
        assertEquals(1, (manifest["schemaVersion"] as JsonPrimitive).intOrNull)
        val counts = manifest["counts"]!!.jsonObject
        assertEquals(techniques.size, counts["techniques"]!!.jsonObject["total"]!!.jsonPrimitive.intOrNull)
        assertEquals(activities.size, counts["activities"]!!.jsonObject["total"]!!.jsonPrimitive.intOrNull)
        assertEquals(tips.size, counts["tips"]!!.jsonObject["total"]!!.jsonPrimitive.intOrNull)
        val listed = manifest["files"]!!.jsonArray.map { it.jsonObject }
        val onDisk = contentDir.walkTopDown().filter { it.isFile && it.name != "manifest.json" }
            .map { it.relativeTo(contentDir).invariantSeparatorsPath }.toSet()
        assertEquals("manifest lists every pack file (V-19)", onDisk, listed.map { it.str("path")!! }.toSet())
        listed.forEach { f ->
            val bytes = File(contentDir, f.str("path")!!).readBytes()
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals("checksum of ${f.str("path")} (V-19)", f.str("sha256"), sha)
        }
    }

    @Test
    fun tcContent007_endToEnd_techniqueOverridesAndTipsReachFeedback() {
        val catalog = (TipPackLoader.load(File(contentDir, "tips.json").readText(), TipPackOptions(releaseBuild = false))
            as TipPackLoadResult.Loaded).tips
        val problems = mutableListOf<String>()
        for (t in techniques) {
            val id = t.str("id")!!
            val resolved = IssueOverrides.resolve(activity = emptyList(), techniques = listOf(overrides(t, problems)))
            val input = AttemptInput(
                attemptId = "att-$id",
                templateId = "TPL-ANSWER",
                durationMs = 20_000L,
                voicedDurationMs = 15_000L,
                transcript = "um so i think we basically moved the launch because uh the testing found two problems " +
                    "and we wanted to fix them before customers saw them which took about a week",
                pauseCount = 8,
                activeFrameRmsList = List(50) { 0.1 + (it % 5) * 0.01 },
                noiseFloorRms = 0.005,
                techniqueIds = listOf(id),
                issueOverrides = resolved
            )
            val r = LocalIntelligencePipeline.evaluate(input, catalog) as AttemptEvaluationResult.Valid
            IssueOverrides.suppressed(resolved).forEach { code ->
                check(problems, r.issues.none { it.code == code }) { "$id: suppressed $code still raised" }
            }
            check(problems, r.feedback.tips.isNotEmpty() && r.feedback.tips.all { it.tipId.startsWith("tip-") }) {
                "$id: feedback tips must come from the pack, got ${r.feedback.tips.map { it.tipId }}"
            }

            // A clean attempt gets exactly the practised technique's general tip (ARC-03 §6.9 step 4).
            val clean = LocalIntelligencePipeline.evaluate(
                input.copy(transcript = cleanAnswer, pauseCount = 0, issueOverrides = emptyMap()), catalog
            ) as AttemptEvaluationResult.Valid
            check(problems, clean.issues.isEmpty()) { "$id: clean attempt raised ${clean.issues.map { it.code }}" }
            val tip = clean.feedback.tips.singleOrNull()
            check(problems, tip != null && tip.techniqueId == id && tip.tipId in t.strings("tipIds")) {
                "$id: clean attempt should get this technique's general tip, got ${clean.feedback.tips.map { it.tipId }}"
            }
        }
        assertNoProblems(problems)
    }

    /** 41 varied words in 15 s of speech (164 wpm, inside the noise band), no fillers, hedges or long pauses. */
    private val cleanAnswer = "our team planned the spring launch carefully because customers asked for faster reports " +
        "clearer screens better search simple exports reliable alerts friendly onboarding useful dashboards secure " +
        "sharing quick setup offline notes calm reminders weekly summaries smart filters bright themes gentle sounds"
}

private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
