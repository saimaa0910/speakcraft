package com.example.speech.word

import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordStageEvaluatorTest {

    @Test
    fun tcWord001_sayIt_exactMatch_scores100AndPasses() {
        val result = WordStageEvaluator.evaluateSayIt(
            targetWord = "collaborate",
            acceptedInflections = listOf("collaborates", "collaborating", "collaborated"),
            transcript = "collaborate"
        )

        assertEquals(100, result.score)
        assertTrue(result.passed)
        assertTrue(result.unmet.isEmpty())
        assertNotNull(result.caveat)
    }

    @Test
    fun tcWord002_sayIt_inflectionMatch_scores90AndPasses() {
        val result = WordStageEvaluator.evaluateSayIt(
            targetWord = "collaborate",
            acceptedInflections = listOf("collaborates", "collaborating", "collaborated"),
            transcript = "we are collaborating"
        )

        assertEquals(90, result.score)
        assertTrue(result.passed)
        assertTrue(result.unmet.isEmpty())
    }

    @Test
    fun tcWord003_sayIt_nearMatch_scoresBetween75And89AndPasses() {
        // "colaborate" missing one 'l': Levenshtein distance 1 from 11 chars (similarity 10/11 = 0.909)
        val result = WordStageEvaluator.evaluateSayIt(
            targetWord = "collaborate",
            acceptedInflections = emptyList(),
            transcript = "colaborate"
        )

        assertTrue(result.score in 75..89)
        assertTrue(result.passed)
        assertTrue(result.unmet.isEmpty())
    }

    @Test
    fun tcWord004_sayIt_wrongForm_scoresUnder75AndFailsWithIssueWrongForm() {
        // "elaborate" vs "collaborate" (similarity ~0.72)
        val result = WordStageEvaluator.evaluateSayIt(
            targetWord = "collaborate",
            acceptedInflections = emptyList(),
            transcript = "elaborate"
        )

        assertTrue(result.score < 75)
        assertFalse(result.passed)
        assertTrue(result.unmet.contains(StageRequirement.TARGET_PRESENT))
        assertEquals(1, result.issues.size)
        assertEquals(IssueCode.ISSUE_WRONG_FORM, result.issues[0].code)
    }

    @Test
    fun tcWord005_sayIt_completelyDifferent_scores0AndFailsWithTargetWordMissing() {
        val result = WordStageEvaluator.evaluateSayIt(
            targetWord = "collaborate",
            acceptedInflections = emptyList(),
            transcript = "banana apple orange"
        )

        assertEquals(0, result.score)
        assertFalse(result.passed)
        assertTrue(result.unmet.contains(StageRequirement.TARGET_PRESENT))
        assertEquals(1, result.issues.size)
        assertEquals(IssueCode.ISSUE_TARGET_WORD_MISSING, result.issues[0].code)
    }

    @Test
    fun tcWord006_useIt_bothSentencesPassing_passes() {
        val refSentences = listOf(
            "we need to collaborate on the project",
            "effective teams collaborate across departments"
        )
        val transcripts = listOf(
            "we need to collaborate on the project",
            "effective teams collaborate across departments"
        )

        val result = WordStageEvaluator.evaluateUseIt(refSentences, transcripts)
        assertEquals(100, result.score)
        assertTrue(result.passed)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun tcWord007_useIt_oneSentenceFailing_fails() {
        val refSentences = listOf(
            "we need to collaborate on the project",
            "effective teams collaborate across departments"
        )
        val transcripts = listOf(
            "we need to collaborate on the project",
            "teams work together" // match < 75%
        )

        val result = WordStageEvaluator.evaluateUseIt(refSentences, transcripts)
        assertTrue(result.score < 75)
        assertFalse(result.passed)
        assertTrue(result.unmet.contains(StageRequirement.BOTH_SENTENCES))
        assertEquals(1, result.issues.size)
        assertEquals(IssueCode.ISSUE_WORD_NOT_RECOGNISED, result.issues[0].code)
    }

    @Test
    fun tcWord008_makeYourOwn_allConditionsMet_scores100AndPasses() {
        val result = WordStageEvaluator.evaluateMakeYourOwn(
            targetWord = "convey",
            acceptedInflections = listOf("conveys", "conveying", "conveyed"),
            transcript = "I wanted to convey the core architectural concept clearly",
            grammarHitCount = 0
        )

        assertEquals(100, result.score)
        assertTrue(result.passed)
        assertTrue(result.unmet.isEmpty())
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun tcWord009_makeYourOwn_missingTarget_fails() {
        val result = WordStageEvaluator.evaluateMakeYourOwn(
            targetWord = "convey",
            acceptedInflections = listOf("conveys", "conveying", "conveyed"),
            transcript = "I wanted to explain the core architectural concept clearly",
            grammarHitCount = 0
        )

        assertFalse(result.passed)
        assertTrue(result.unmet.contains(StageRequirement.TARGET_PRESENT))
        assertEquals(1, result.issues.size)
        assertEquals(IssueCode.ISSUE_TARGET_WORD_MISSING, result.issues[0].code)
    }

    @Test
    fun tcWord010_makeYourOwn_tooShort_fails() {
        val result = WordStageEvaluator.evaluateMakeYourOwn(
            targetWord = "convey",
            acceptedInflections = emptyList(),
            transcript = "I convey it", // 3 words < 5
            grammarHitCount = 0
        )

        assertFalse(result.passed)
        assertTrue(result.unmet.contains(StageRequirement.MIN_WORDS))
        assertEquals(1, result.issues.size)
        assertEquals(IssueCode.ISSUE_TOO_SHORT, result.issues[0].code)
    }
}
