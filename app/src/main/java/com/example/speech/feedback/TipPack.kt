package com.example.speech.feedback

import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One trigger of a content-pack tip (ARC-03 §6.9). Every key that is present must match (logical AND);
 * a tip matches when any of its triggers matches.
 */
data class TipTrigger(
    val issueCode: IssueCode? = null,
    val metricKey: SpeechMetricKey? = null,
    val band: String? = null,
    val templateId: String? = null,
    val techniqueId: String? = null
) {
    /** ARC-05 §8.4 ranking: the trigger counts at the level of its most specific key. */
    val specificity: Int
        get() = when {
            issueCode != null -> 4
            metricKey != null -> 3
            techniqueId != null -> 2
            templateId != null -> 1
            else -> 0
        }

    fun matches(
        issues: List<DetectedIssue>,
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        techniqueIds: List<String>,
        attemptTemplateId: String?
    ): Boolean =
        (issueCode == null || issues.any { it.code == issueCode }) &&
            (metricKey == null || metrics[metricKey]?.band.equals(band, ignoreCase = true)) &&
            (techniqueId == null || techniqueId in techniqueIds) &&
            (templateId == null || templateId.equals(attemptTemplateId, ignoreCase = true))
}

/** A content validation failure (ARC-03 §6.12 rule ids V-01…V-22). */
data class ContentViolation(val rule: String, val itemId: String?, val message: String)

/**
 * Build context for loading `tips.json`.
 * @param releaseBuild release builds reject `PENDING_REVIEW` tips (V-10); debug builds keep them and report their ids.
 * @param knownTechniqueIds technique ids in the pack; when given, tip references are checked (V-03/V-16) and every
 *   technique needs a general tip (V-15).
 * @param knownTemplateIds template ids; when null, template references only need the `TPL-` form.
 * @param brandingBlocklist names and handles that must not appear in displayed text (V-14).
 */
data class TipPackOptions(
    val releaseBuild: Boolean,
    val knownTechniqueIds: Set<String>? = null,
    val knownTemplateIds: Set<String>? = null,
    val brandingBlocklist: Set<String> = emptySet()
)

sealed class TipPackLoadResult {
    /** Active tips, ready for [TipSelector.select]; retired tips are kept out of selection (SRS-BR-026). */
    data class Loaded(val tips: List<TipDefinition>, val pendingReviewTipIds: Set<String>) : TipPackLoadResult()

    /** The pack failed validation; callers keep the built-in catalogue ([TipSelector.CATALOG]). */
    data class Rejected(val violations: List<ContentViolation>) : TipPackLoadResult()
}

/**
 * Loads and validates the content pack's `tips.json` (ARC-03 §6.1, §6.9; SRS-FR-EVAL-016), so coach tips
 * come from reviewed curriculum content instead of code. Parsing is strict: unknown keys fail (V-01), which
 * also rejects any transcript, caption or quote field (V-13). Any violation rejects the whole file, so a
 * partly valid pack never reaches users.
 */
object TipPackLoader {

    /** Display title for pack tips: the schema has no tip title (ARC-03 §6.9), so a neutral label is used. */
    const val PACK_TIP_TITLE = "Coach tip"

    private val json = Json { ignoreUnknownKeys = false; isLenient = false }

    private val ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,63}$")
    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")
    private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val TEMPLATE_PATTERN = Regex("^TPL-[A-Z0-9-]+$")
    private val PLACEHOLDER_PATTERN = Regex("\\b(TODO|TBD|FIXME|lorem)\\b", RegexOption.IGNORE_CASE)
    private val HTML_PATTERN = Regex("<\\s*/?\\s*[a-zA-Z][^>]*>")
    private val LICENCE_STATUSES = setOf("ORIGINAL", "ORIGINAL_REEXPRESSION", "LICENSED", "PENDING_REVIEW")
    private const val MAX_TEXT = 240

    fun load(tipsJson: String, options: TipPackOptions): TipPackLoadResult {
        val records = try {
            json.decodeFromString(ListSerializer(TipRecordJson.serializer()), tipsJson)
        } catch (e: SerializationException) {
            return TipPackLoadResult.Rejected(listOf(ContentViolation("V-01", null, "tips.json: ${e.message}")))
        } catch (e: IllegalArgumentException) {
            return TipPackLoadResult.Rejected(listOf(ContentViolation("V-01", null, "tips.json: ${e.message}")))
        }

        val violations = mutableListOf<ContentViolation>()
        records.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
            violations += ContentViolation("V-02", it, "duplicate tip id")
        }
        val tips = records.mapNotNull { record -> validateAndConvert(record, options, violations) }
        checkCoverage(records, tips, options, violations)

        if (violations.isNotEmpty()) return TipPackLoadResult.Rejected(violations)
        val active = records.filter { it.status == "ACTIVE" }.map { it.id }.toSet()
        return TipPackLoadResult.Loaded(
            tips = tips.filter { it.tipId in active },
            pendingReviewTipIds = records.filter { it.licenceStatus == "PENDING_REVIEW" && it.id in active }
                .map { it.id }.toSet()
        )
    }

    private fun validateAndConvert(
        r: TipRecordJson,
        options: TipPackOptions,
        out: MutableList<ContentViolation>
    ): TipDefinition? {
        val before = out.size
        fun fail(rule: String, message: String) {
            out += ContentViolation(rule, r.id, message)
        }

        // V-01 schema conformance
        if (!ID_PATTERN.matches(r.id)) fail("V-01", "id must match ${ID_PATTERN.pattern}")
        if (r.status != "ACTIVE" && r.status != "RETIRED") fail("V-01", "status must be ACTIVE or RETIRED")
        if (r.introducedIn < 1) fail("V-01", "introducedIn must be ≥ 1")
        if (r.retiredIn != null && (r.status != "RETIRED" || r.retiredIn < r.introducedIn)) {
            fail("V-01", "retiredIn needs status RETIRED and must not precede introducedIn")
        }
        if (r.text.isBlank() || r.text.length > MAX_TEXT) fail("V-01", "text must be 1–$MAX_TEXT characters")
        if (r.priority !in 1..100) fail("V-01", "priority must be 1–100")
        if (r.licenceStatus !in LICENCE_STATUSES) fail("V-01", "unknown licenceStatus ${r.licenceStatus}")

        // V-18 text hygiene
        if (r.text != r.text.trim()) fail("V-18", "text must be trimmed")
        if (PLACEHOLDER_PATTERN.containsMatchIn(r.text)) fail("V-18", "text contains placeholder wording")
        if (HTML_PATTERN.containsMatchIn(r.text)) fail("V-18", "text must not contain HTML")

        // V-14 no creator name, handle or branding in displayed text
        options.brandingBlocklist.filter { it.isNotBlank() }.forEach { term ->
            if (Regex("(?i)(^|[^\\p{L}\\p{N}])${Regex.escape(term.trim())}($|[^\\p{L}\\p{N}])").containsMatchIn(r.text)) {
                fail("V-14", "text contains a blocked name or handle")
            }
        }

        // V-10, V-11, V-12 licence and provenance (D-08)
        validateLicence(r, options, ::fail)

        // V-03 / V-16 references
        if (r.techniqueId != null && options.knownTechniqueIds != null && r.techniqueId !in options.knownTechniqueIds) {
            fail("V-03", "unknown techniqueId ${r.techniqueId}")
        }
        val triggers = r.triggers.mapIndexedNotNull { i, t -> convertTrigger(t, i, options, ::fail) }

        if (out.size > before) return null
        return TipDefinition(
            tipId = r.id,
            techniqueId = r.techniqueId,
            title = PACK_TIP_TITLE,
            text = r.text,
            priority = r.priority,
            sourceVideoUrl = if (r.licenceStatus == "ORIGINAL_REEXPRESSION") r.provenance?.sourceUrl else null,
            triggers = triggers
        )
    }

    private fun validateLicence(r: TipRecordJson, options: TipPackOptions, fail: (String, String) -> Unit) {
        if (r.attributionText != null) fail("V-11", "attributionText must be null in R1")
        when (r.licenceStatus) {
            "LICENSED" -> fail("V-11", "LICENSED content is not allowed in R1")
            "PENDING_REVIEW" -> if (options.releaseBuild) fail("V-10", "PENDING_REVIEW tips cannot ship in a release build")
        }
        if (r.licenceStatus == "PENDING_REVIEW") return
        val p = r.provenance
        if (p == null) {
            fail("V-11", "provenance with verifiedBy and verifiedOn is required")
            return
        }
        if (p.verifiedBy.isNullOrBlank() || p.verifiedBy.length > 40) fail("V-11", "provenance.verifiedBy is required (≤ 40)")
        if (p.verifiedOn == null || !DATE_PATTERN.matches(p.verifiedOn)) fail("V-11", "provenance.verifiedOn must be YYYY-MM-DD")
        if (r.licenceStatus != "ORIGINAL_REEXPRESSION") return

        val videoId = p.sourceVideoId
        if (videoId == null || !VIDEO_ID_PATTERN.matches(videoId)) fail("V-11", "provenance.sourceVideoId must be an 11-character video id")
        if (p.timestampStart == null || p.timestampStart < 0) fail("V-11", "provenance.timestampStart is required (≥ 0)")
        if (p.timestampEnd != null && p.timestampStart != null && p.timestampEnd <= p.timestampStart) {
            fail("V-11", "provenance.timestampEnd must be after timestampStart")
        }
        val url = p.sourceUrl
        if (url == null) {
            fail("V-11", "provenance.sourceUrl is required")
        } else if (videoId != null && !isSpecificVideoUrl(url, videoId)) {
            fail("V-12", "sourceUrl must be a specific video link for $videoId (no channel, handle or playlist links)")
        }
    }

    /** V-12: a specific video, `watch?v=`, `youtu.be/` or `shorts/`, with an optional `t=` start, matching [videoId]. */
    fun isSpecificVideoUrl(url: String, videoId: String): Boolean {
        if (!VIDEO_ID_PATTERN.matches(videoId)) return false
        val id = Regex.escape(videoId)
        val t = "t=\\d+s?"
        return Regex(
            "^https://(?:www\\.youtube\\.com/watch\\?v=$id(?:&$t)?" +
                "|youtu\\.be/$id(?:\\?$t)?" +
                "|www\\.youtube\\.com/shorts/$id(?:\\?$t)?)$"
        ).matches(url)
    }

    private fun convertTrigger(
        t: TipTriggerJson,
        index: Int,
        options: TipPackOptions,
        fail: (String, String) -> Unit
    ): TipTrigger? {
        val where = "triggers[$index]"
        if (listOf(t.issueCode, t.metricKey, t.band, t.templateId, t.techniqueId).all { it == null }) {
            fail("V-01", "$where needs at least one key")
            return null
        }
        if ((t.metricKey == null) != (t.band == null)) {
            fail("V-01", "$where: metricKey and band go together")
            return null
        }
        val issue = t.issueCode?.let { code ->
            IssueCode.entries.firstOrNull { it.name == code } ?: run {
                fail("V-16", "$where: unknown issueCode $code")
                return null
            }
        }
        val metric = t.metricKey?.let { code ->
            SpeechMetricKey.entries.firstOrNull { it.code == code } ?: run {
                fail("V-16", "$where: unknown metricKey $code")
                return null
            }
        }
        if (metric != null) {
            val bands = MetricEngine.BAND_LABELS[metric].orEmpty()
            if (bands.none { it.equals(t.band, ignoreCase = true) }) {
                fail("V-16", "$where: band '${t.band}' is not a band of ${metric.code} (${bands.joinToString()})")
                return null
            }
        }
        if (t.templateId != null) {
            val known = options.knownTemplateIds
            if ((known != null && t.templateId !in known) || (known == null && !TEMPLATE_PATTERN.matches(t.templateId))) {
                fail("V-16", "$where: unknown templateId ${t.templateId}")
                return null
            }
        }
        if (t.techniqueId != null && options.knownTechniqueIds != null && t.techniqueId !in options.knownTechniqueIds) {
            fail("V-16", "$where: unknown techniqueId ${t.techniqueId}")
            return null
        }
        return TipTrigger(issue, metric, t.band, t.templateId, t.techniqueId)
    }

    /** V-15: every issue code has an active tip, there is a global general tip, and every technique has a general tip. */
    private fun checkCoverage(
        records: List<TipRecordJson>,
        tips: List<TipDefinition>,
        options: TipPackOptions,
        out: MutableList<ContentViolation>
    ) {
        if (tips.size != records.size) return // per-record failures already reported
        val active = records.filter { it.status == "ACTIVE" }.map { it.id }.toSet()
        val activeTips = tips.filter { it.tipId in active }
        val coveredIssues = activeTips.flatMap { it.triggers.orEmpty() }.mapNotNull { it.issueCode }.toSet()
        IssueCode.entries.filterNot { it in coveredIssues }.forEach {
            out += ContentViolation("V-15", null, "no active tip has an issueCode trigger for ${it.name}")
        }
        val generals = activeTips.filter { it.triggers.isNullOrEmpty() }
        if (generals.none { it.techniqueId == null }) {
            out += ContentViolation("V-15", null, "no active global general tip (techniqueId null, no triggers)")
        }
        options.knownTechniqueIds?.forEach { technique ->
            if (generals.none { it.techniqueId == technique }) {
                out += ContentViolation("V-15", technique, "technique has no active general tip")
            }
        }
    }
}

@Serializable
internal data class TipRecordJson(
    val id: String,
    val status: String,
    val introducedIn: Int,
    val retiredIn: Int? = null,
    val techniqueId: String?,
    val text: String,
    val triggers: List<TipTriggerJson>,
    val priority: Int,
    val provenance: ProvenanceJson? = null,
    val licenceStatus: String,
    val attributionText: String?
)

@Serializable
internal data class TipTriggerJson(
    val issueCode: String? = null,
    val metricKey: String? = null,
    val band: String? = null,
    val templateId: String? = null,
    val techniqueId: String? = null
)

@Serializable
internal data class ProvenanceJson(
    val sourceVideoId: String? = null,
    val sourceUrl: String? = null,
    val timestampStart: Int? = null,
    val timestampEnd: Int? = null,
    val techniqueNameAsTaught: String? = null,
    val verifiedBy: String? = null,
    val verifiedOn: String? = null,
    val notes: String? = null
)
