package com.example.speech.content

import com.example.speech.pipeline.IssueOverride
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The practice catalogue built from the shipped content pack plus built-in quick practice (EP-08). */
class PracticeCatalogParserTest {

    private val contentDir = File(System.getProperty("user.dir") ?: ".").let { cwd ->
        if (File(cwd, "src/main/assets").exists()) cwd else File(cwd, "app")
    }.let { File(it, "src/main/assets/content") }

    private fun load(includePending: Boolean): PracticeCatalog = PracticeCatalogParser.parse(
        techniquesJson = File(contentDir, "techniques.json").readText(),
        activityJsons = File(contentDir, "activities").listFiles()!!.filter { it.extension == "json" }.sortedBy { it.name }.map { it.readText() },
        includePendingReview = includePending
    )

    @Test
    fun tcCat001_debugBuild_showsTheWholePackWithPendingBadges() {
        val c = load(includePending = true)
        assertEquals(15, c.techniques.size)
        assertEquals(86 + BuiltInPractice.ACTIVITIES.size, c.activities.size)
        assertTrue(c.techniques.all { it.pendingReview })
        assertTrue(c.techniques.all { c.activitiesFor(it.id).isNotEmpty() })
        assertEquals(BuiltInPractice.ACTIVITIES.map { it.id }, c.quickPractice.map { it.id })
    }

    @Test
    fun tcCat002_releaseBuild_leavesOutPendingReviewItems_butKeepsQuickPractice() {
        val c = load(includePending = false)
        assertTrue(c.techniques.isEmpty())
        assertEquals(BuiltInPractice.ACTIVITIES.map { it.id }, c.activities.map { it.id })
    }

    @Test
    fun tcCat003_techniqueOverridesReachTheAttempt() {
        val c = load(includePending = true)
        val pauseActivity = c.activitiesFor("pause-not-filler").first()
        assertEquals(IssueOverride.Suppress, c.overridesFor(pauseActivity)[IssueCode.ISSUE_PAUSES_LONG])
        assertTrue(c.selfChecksFor(pauseActivity).isNotEmpty())
    }

    @Test
    fun tcCat004_activitiesCarryWhatTheirTemplateNeeds() {
        val c = load(includePending = true)
        c.activities.forEach { a ->
            assertTrue("${a.id} limit", a.maxSeconds in 20..300)
            when (a.templateId) {
                "TPL-READ", "TPL-REPEAT" -> assertNotNull("${a.id} reference text", a.referenceText)
                "TPL-STRUCT" -> assertTrue("${a.id} framework", a.framework == "PREP" || a.framework == "STAR")
            }
        }
        val first = c.activitiesFor("prep-answer").first()
        assertNotNull(c.nextAfter(first))
    }

    @Test
    fun tcCat005_malformedContent_isSkippedNotFatal() {
        val c = PracticeCatalogParser.parse("not json", listOf("[{\"id\":\"x\"}]", "{}"), includePendingReview = true)
        assertTrue(c.techniques.isEmpty())
        assertEquals(BuiltInPractice.ACTIVITIES.size, c.activities.size)
    }
}
