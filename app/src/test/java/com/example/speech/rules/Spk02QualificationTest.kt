package com.example.speech.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * SPK-02 qualification (ARC-05 §8.1, SRS-FR-EVAL-006, NFR-QUAL-003): measures every candidate rule
 * on [Spk02Corpus] and checks that [RuleQualificationRegistry] records exactly the measured result.
 * A registry entry that does not match a fresh measurement — or a qualifying rule missing from the
 * registry — fails the build, so qualifications cannot drift from the evidence.
 */
class Spk02QualificationTest {

    private fun round3(v: Double) = String.format(Locale.ROOT, "%.3f", v)

    @Test
    fun tcSpk02_001_corpusMeetsMinimumSizePerRule() {
        for (rule in RuleEngine.CANDIDATE_RULES) {
            val corpus = Spk02Corpus.CORPUS[rule.id]
            assertNotNull("no corpus for ${rule.id}", corpus)
            assertTrue("${rule.id} positives", corpus!!.count { it.shouldFire } >= RuleQualificationHarness.MIN_POSITIVES)
            assertTrue("${rule.id} negatives", corpus.count { !it.shouldFire } >= RuleQualificationHarness.MIN_NEGATIVES)
        }
    }

    @Test
    fun tcSpk02_002_registryMatchesFreshMeasurement() {
        val measured = RuleEngine.CANDIDATE_RULES.map { RuleQualificationHarness.evaluate(it, Spk02Corpus.CORPUS.getValue(it.id)) }
        val summary = measured.joinToString("; ") {
            "${it.ruleId} tp=${it.truePositives} fp=${it.falsePositives} fn=${it.falseNegatives} tn=${it.trueNegatives} " +
                "precision=${it.precision?.let(::round3)} recall=${it.recall?.let(::round3)} qualifies=${it.qualifies}"
        }
        for (report in measured) {
            val entry = RuleQualificationRegistry.QUALIFIED[report.ruleId]
            if (report.qualifies) {
                assertNotNull("qualifying rule missing from registry. Measured: $summary", entry)
                assertEquals("precision for ${report.ruleId}. Measured: $summary", round3(report.precision!!), round3(entry!!.precision))
                assertEquals(report.positives, entry.positives)
                assertEquals(report.negatives, entry.negatives)
                assertEquals(Spk02Corpus.VERSION, entry.corpusVersion)
            } else {
                assertNull("non-qualifying rule must not be registered. Measured: $summary", entry)
            }
        }
    }
}
