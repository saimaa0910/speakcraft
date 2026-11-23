package com.example.speech.feedback

import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Content-pack coach tips (ARC-03 §6.9, validation V-01…V-18; SRS-FR-EVAL-016; D-08): strict loading,
 * licence and link rules, coverage, and deterministic selection with structured triggers.
 */
class TipPackLoaderTest {

    private val provOriginal = """{"verifiedBy":"content-reviewer-02","verifiedOn":"2026-11-20"}"""
    private val provReexpression = """{"sourceVideoId":"AbCdEfGhIjK","sourceUrl":"https://www.youtube.com/watch?v=AbCdEfGhIjK&t=75s",""" +
        """"timestampStart":75,"timestampEnd":142,"techniqueNameAsTaught":"internal only","verifiedBy":"content-reviewer-02","verifiedOn":"2026-11-20"}"""

    private fun tip(
        id: String,
        techniqueId: String?,
        triggers: String,
        priority: Int = 10,
        licence: String = "ORIGINAL",
        provenance: String? = provOriginal,
        status: String = "ACTIVE",
        retiredIn: Int? = null,
        text: String = "Short original advice written for this tip.",
        attribution: String = "null",
        extra: String = ""
    ): String {
        val tech = techniqueId?.let { "\"$it\"" } ?: "null"
        val prov = provenance?.let { ""","provenance":$it""" } ?: ""
        val retired = retiredIn?.let { ""","retiredIn":$it""" } ?: ""
        return """{"id":"$id","status":"$status","introducedIn":1$retired,"techniqueId":$tech,"text":"$text",""" +
            """"triggers":$triggers,"priority":$priority$prov,"licenceStatus":"$licence","attributionText":$attribution$extra}"""
    }

    private fun issueTips(except: Set<IssueCode> = emptySet()): List<String> = IssueCode.entries.filterNot { it in except }.map {
        tip("tip-${it.name.lowercase().replace('_', '-')}", null, """[{"issueCode":"${it.name}"}]""")
    }

    private val reexpressionTip = tip(
        "tip-pause-answer", "pause-power", """[{"issueCode":"ISSUE_PAUSES_LONG","templateId":"TPL-ANSWER"}]""",
        priority = 1, licence = "ORIGINAL_REEXPRESSION", provenance = provReexpression
    )
    private val globalGeneral = tip("tip-general-global", null, "[]", priority = 50)
    private val techniqueGeneral = tip("tip-pause-general", "pause-power", "[]", priority = 20)

    private fun pack(vararg records: String) = records.joinToString(",", "[", "]")
    private fun validRecords() = issueTips() + listOf(reexpressionTip, globalGeneral, techniqueGeneral)
    private val debug = TipPackOptions(releaseBuild = false)
    private val release = TipPackOptions(releaseBuild = true, knownTechniqueIds = setOf("pause-power"))

    private fun loaded(json: String, options: TipPackOptions = release) =
        TipPackLoader.load(json, options) as? TipPackLoadResult.Loaded
            ?: throw AssertionError("expected Loaded, got ${TipPackLoader.load(json, options)}")

    private fun violations(json: String, options: TipPackOptions = release): List<ContentViolation> =
        (TipPackLoader.load(json, options) as? TipPackLoadResult.Rejected)?.violations
            ?: throw AssertionError("expected Rejected")

    @Test
    fun tcPack001_validPack_loads_withOutboundLinkOnlyForReexpressedTips() {
        val result = loaded(pack(*validRecords().toTypedArray()))
        assertEquals(IssueCode.entries.size + 3, result.tips.size)
        assertTrue(result.pendingReviewTipIds.isEmpty())
        val reexpressed = result.tips.single { it.tipId == "tip-pause-answer" }
        assertEquals("https://www.youtube.com/watch?v=AbCdEfGhIjK&t=75s", reexpressed.sourceVideoUrl)
        assertEquals(TipPackLoader.PACK_TIP_TITLE, reexpressed.title)
        assertNull(result.tips.single { it.tipId == "tip-general-global" }.sourceVideoUrl)
    }

    @Test
    fun tcPack002_unknownKeys_includingTranscriptFields_areRejected() {
        val withTranscript = tip("tip-extra", null, "[]", extra = ""","transcript":"verbatim words"""")
        val v = violations(pack(*(validRecords() + withTranscript).toTypedArray()))
        assertEquals("V-01", v.single().rule)
    }

    @Test
    fun tcPack003_pendingReview_blockedInRelease_allowedAndReportedInDebug() {
        val pending = tip("tip-pending", null, """[{"templateId":"TPL-READ"}]""", licence = "PENDING_REVIEW", provenance = null)
        val json = pack(*(validRecords() + pending).toTypedArray())
        assertTrue(violations(json).any { it.rule == "V-10" && it.itemId == "tip-pending" })
        assertEquals(setOf("tip-pending"), loaded(json, debug).pendingReviewTipIds)
    }

    @Test
    fun tcPack004_reexpressedTip_needsASpecificVideoLink() {
        val channel = provReexpression.replace("https://www.youtube.com/watch?v=AbCdEfGhIjK&t=75s", "https://www.youtube.com/@somechannel")
        val bad = tip("tip-bad-link", null, """[{"templateId":"TPL-READ"}]""", licence = "ORIGINAL_REEXPRESSION", provenance = channel)
        assertTrue(violations(pack(*(validRecords() + bad).toTypedArray())).any { it.rule == "V-12" })

        assertTrue(TipPackLoader.isSpecificVideoUrl("https://www.youtube.com/watch?v=AbCdEfGhIjK", "AbCdEfGhIjK"))
        assertTrue(TipPackLoader.isSpecificVideoUrl("https://youtu.be/AbCdEfGhIjK?t=30", "AbCdEfGhIjK"))
        assertTrue(TipPackLoader.isSpecificVideoUrl("https://www.youtube.com/shorts/AbCdEfGhIjK", "AbCdEfGhIjK"))
        assertFalse(TipPackLoader.isSpecificVideoUrl("https://www.youtube.com/watch?v=ZZZZZZZZZZZ", "AbCdEfGhIjK"))
        assertFalse(TipPackLoader.isSpecificVideoUrl("https://www.youtube.com/playlist?list=PL123", "AbCdEfGhIjK"))
        assertFalse(TipPackLoader.isSpecificVideoUrl("https://www.youtube.com/channel/UC123", "AbCdEfGhIjK"))
    }

    @Test
    fun tcPack005_licenceRules_attributionAndLicensed() {
        val attributed = tip("tip-attributed", null, """[{"templateId":"TPL-READ"}]""", attribution = "\"Credit line\"")
        val licensed = tip("tip-licensed", null, """[{"templateId":"TPL-READ"}]""", licence = "LICENSED")
        val v = violations(pack(*(validRecords() + attributed + licensed).toTypedArray()))
        assertTrue(v.any { it.rule == "V-11" && it.itemId == "tip-attributed" })
        assertTrue(v.any { it.rule == "V-11" && it.itemId == "tip-licensed" })
    }

    @Test
    fun tcPack006_coverage_everyIssueAndGeneralTips() {
        val missingQuiet = issueTips(except = setOf(IssueCode.ISSUE_TOO_QUIET)) + listOf(globalGeneral, techniqueGeneral)
        assertTrue(violations(pack(*missingQuiet.toTypedArray())).any { it.rule == "V-15" && "ISSUE_TOO_QUIET" in it.message })

        val noGlobal = issueTips() + techniqueGeneral
        assertTrue(violations(pack(*noGlobal.toTypedArray())).any { it.rule == "V-15" && "global" in it.message })

        val noTechniqueGeneral = issueTips() + globalGeneral
        assertTrue(violations(pack(*noTechniqueGeneral.toTypedArray())).any { it.rule == "V-15" && it.itemId == "pause-power" })
    }

    @Test
    fun tcPack007_triggerReferences_andTextHygiene() {
        val records = validRecords() + listOf(
            tip("tip-bad-band", null, """[{"metricKey":"M-FILL","band":"HIGH"}]"""),
            tip("tip-bad-issue", null, """[{"issueCode":"ISSUE_NOT_REAL"}]"""),
            tip("tip-empty-trigger", null, "[{}]"),
            tip("tip-html", null, """[{"templateId":"TPL-READ"}]""", text = "Use <b>bold</b> pauses."),
            tip("tip-todo", null, """[{"templateId":"TPL-READ"}]""", text = "TODO write this.")
        )
        val v = violations(pack(*records.toTypedArray()))
        assertTrue(v.any { it.rule == "V-16" && it.itemId == "tip-bad-band" })
        assertTrue(v.any { it.rule == "V-16" && it.itemId == "tip-bad-issue" })
        assertTrue(v.any { it.rule == "V-01" && it.itemId == "tip-empty-trigger" })
        assertTrue(v.any { it.rule == "V-18" && it.itemId == "tip-html" })
        assertTrue(v.any { it.rule == "V-18" && it.itemId == "tip-todo" })

        val goodBand = tip("tip-good-band", null, """[{"metricKey":"M-FILL","band":"Frequent"}]""")
        loaded(pack(*(validRecords() + goodBand).toTypedArray()))
    }

    @Test
    fun tcPack008_brandingBlocklist() {
        val named = tip("tip-named", null, """[{"templateId":"TPL-READ"}]""", text = "As Examplecoach says, slow down.")
        val options = release.copy(brandingBlocklist = setOf("Examplecoach"))
        assertTrue(violations(pack(*(validRecords() + named).toTypedArray()), options).any { it.rule == "V-14" })
    }

    @Test
    fun tcPack009_retiredTips_stayValidButAreNeverSelected() {
        val retired = tip("tip-old", null, """[{"issueCode":"ISSUE_PACE_FAST"}]""", status = "RETIRED", retiredIn = 2, priority = 1)
        val result = loaded(pack(*(validRecords() + retired).toTypedArray()))
        assertTrue(result.tips.none { it.tipId == "tip-old" })
    }

    @Test
    fun tcPack010_selection_usesMostSpecificTrigger_andReportsTheIssueAnswered() {
        val catalog = loaded(pack(*validRecords().toTypedArray())).tips
        val pauses = DetectedIssue(IssueCode.ISSUE_PAUSES_LONG, "6 long pauses", "Plan your points.")
        val fillers = DetectedIssue(IssueCode.ISSUE_FILLERS_HIGH, "8 / min", "Pause instead.")
        val tips = TipSelector.select(
            attemptId = "att-pack",
            issues = listOf(fillers, pauses),
            metrics = emptyMap(),
            techniqueIds = listOf("pause-power"),
            templateId = "TPL-ANSWER",
            catalog = catalog
        )
        assertEquals("tip-issue-fillers-high", tips[0].tipId)
        assertTrue(tips[0].isFocusTip)
        assertEquals(IssueCode.ISSUE_FILLERS_HIGH, tips[0].forIssue)
        // The re-expressed pause tip (issue + template trigger, priority 1) ranks before the generic pause tip.
        assertEquals("tip-pause-answer", tips[1].tipId)
        assertEquals(IssueCode.ISSUE_PAUSES_LONG, tips[1].forIssue)
        assertEquals("https://www.youtube.com/watch?v=AbCdEfGhIjK&t=75s", tips[1].sourceVideoUrl)
        assertEquals(3, tips.size)

        // On another template the combined trigger does not match (all keys must match).
        val elsewhere = TipSelector.select("att-pack", listOf(pauses), emptyMap(), listOf("pause-power"), "TPL-READ", catalog = catalog)
        assertTrue(elsewhere.none { it.tipId == "tip-pause-answer" })
    }

    @Test
    fun tcPack011_noIssue_techniqueGeneralTip_elseGlobalGeneralTip() {
        val catalog = loaded(pack(*validRecords().toTypedArray())).tips
        val forTechnique = TipSelector.select("att-calm", emptyList(), emptyMap(), listOf("pause-power"), "TPL-ANSWER", catalog = catalog)
        assertEquals(listOf("tip-pause-general"), forTechnique.map { it.tipId })
        val general = TipSelector.select("att-calm", emptyList(), emptyMap(), emptyList(), "TPL-ANSWER", catalog = catalog)
        assertEquals(listOf("tip-general-global"), general.map { it.tipId })
        assertNull(general.single().forIssue)
    }

    @Test
    fun tcPack012_bandLabels_coverEveryBandTheMetricEngineProduces() {
        val inputs = listOf(
            "" to null,
            "um so i think maybe we sort of just went there" to null,
            List(60) { "word$it" }.joinToString(" ") to "STAR",
            "the situation was hard my task was clear i decided to act as a result we won" to "STAR",
            List(10) { "same" }.joinToString(" ") to "PREP"
        )
        val seen = mutableMapOf<SpeechMetricKey, MutableSet<String>>()
        for ((transcript, framework) in inputs) {
            for (voicedMs in listOf(2_000L, 15_000L, 60_000L)) {
                MetricEngine.computeMetrics(
                    durationMs = 20_000L,
                    voicedDurationMs = voicedMs,
                    transcript = transcript,
                    pauseCount = 3,
                    activeFrameRmsList = listOf(0.01, 0.2, 0.05, 0.3),
                    referenceText = "we went there",
                    targetWord = "went",
                    targetMinWords = 20,
                    framework = framework,
                    grammarHitCount = if (transcript.isEmpty()) 0 else 1
                ).values.forEach { seen.getOrPut(it.key) { mutableSetOf() } += it.band }
            }
        }
        seen.forEach { (key, bands) ->
            val known = MetricEngine.BAND_LABELS[key].orEmpty()
            assertTrue("$key produced $bands, BAND_LABELS has $known", known.containsAll(bands))
        }
    }
}
