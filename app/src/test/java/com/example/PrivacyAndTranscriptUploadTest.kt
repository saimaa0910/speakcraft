package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.data.activity.ActivityAttemptRecord
import com.example.data.firebase.ActivityFirestoreRepository
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.SpeakingSession
import com.example.data.preferences.AppPreferences
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SpeakCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies Stage B Sprint 1 Task T-04.2.1 (ISS-03, FND-SEC-05, SEC-REQ-10):
 * - Firestore payloads for speakingSessions, speechFeedback, conversationSessions, and
 *   activityAttempts never upload raw speech transcripts or device-local media file paths.
 * - Settings screen privacy statement accurately matches actual storage and speech behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class PrivacyAndTranscriptUploadTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleTranscript = "PRIVATE_SENSITIVE_SPOKEN_TRANSCRIPT_DO_NOT_UPLOAD"
    private val sampleMediaPath = "/data/user/0/com.example/files/speaking_recording_42.m4a"

    private fun sampleSpeakingSession(): SpeakingSession = SpeakingSession(
        moduleType = "SPEAKING_LAB",
        promptTitle = "2-Minute Product Pitch",
        durationSeconds = 95L,
        localMediaPath = sampleMediaPath,
        mediaType = "audio/mp4",
        transcript = sampleTranscript,
        overallAssessment = "Clear structure with steady pacing.",
        strengths = listOf("Strong opening hook"),
        mistakes = listOf("Rushed conclusion"),
        pronunciationNotes = listOf("Clear vowel endings"),
        betterAlternatives = listOf("Pause briefly before key metrics"),
        fillerWordsDetected = listOf("um (2)"),
        repeatedExpressions = listOf("basically (2)"),
        improvementSuggestions = listOf("Slow down on final sentence"),
        recommendedExercises = listOf("1-minute summary drill"),
        pronunciationAssessable = true,
        articulationClarity = "Clear",
        thoughtOrganization = "Structured",
        nextRecommendedActivity = "Pause Control Drill",
        confidenceLevel = "High",
        audioQualityNote = "Good signal",
        fluencyScore = 84L,
        pronunciationScore = 80L,
        vocabularyScore = 86L,
        grammarScore = 82L,
        articulationScore = 85L,
        wordsPerMinute = 132L,
        evaluationStatus = "COMPLETED",
        isPartialEvaluation = false,
        evaluatedDimensionKeys = listOf("fluency", "vocabulary"),
        unavailableDimensionKeys = emptyList(),
        evidenceSummary = "95s audio, local evaluation"
    )

    @Test
    fun `speakingSessions payload never uploads transcript or localMediaPath`() {
        val payload = SpeakCraftRepository.buildSpeakingSessionPayload(
            uid = "uid-123",
            feedbackId = "fb-456",
            session = sampleSpeakingSession(),
            timestamp = "SERVER_TS"
        )

        assertFalse("speakingSessions must not contain transcript", payload.containsKey("transcript"))
        assertFalse("speakingSessions must not contain localMediaPath", payload.containsKey("localMediaPath"))
        assertFalse(
            "speakingSessions values must not leak raw transcript text",
            payload.values.any { it.toString().contains(sampleTranscript) }
        )
        assertFalse(
            "speakingSessions values must not leak device media file paths",
            payload.values.any { it.toString().contains(sampleMediaPath) }
        )

        // Core progress metadata remains intact
        assertEquals("uid-123", payload["userId"])
        assertEquals("fb-456", payload["feedbackId"])
        assertEquals("SPEAKING_LAB", payload["moduleType"])
        assertEquals(84L, payload["fluencyScore"])
        assertEquals(132L, payload["wordsPerMinute"])
        assertEquals("COMPLETED", payload["evaluationStatus"])
    }

    @Test
    fun `speechFeedback payload never uploads transcript`() {
        val payload = SpeakCraftRepository.buildSpeechFeedbackPayload(
            uid = "uid-123",
            sessionId = "sess-789",
            session = sampleSpeakingSession(),
            timestamp = "SERVER_TS"
        )

        assertFalse("speechFeedback must not contain transcript", payload.containsKey("transcript"))
        assertFalse(
            "speechFeedback values must not leak raw transcript text",
            payload.values.any { it.toString().contains(sampleTranscript) }
        )

        assertEquals("uid-123", payload["userId"])
        assertEquals("sess-789", payload["sessionId"])
        assertTrue((payload["strengths"] as List<*>).contains("Strong opening hook"))
    }

    @Test
    fun `conversationSessions payload never uploads transcriptPreview`() {
        val payload = SpeakCraftRepository.buildConversationSessionPayload(
            uid = "uid-123",
            scenario = "Job Interview",
            difficultyLevel = "Intermediate",
            turnCount = 6L,
            summaryFeedback = "Concise answers with good follow-up.",
            timestamp = "SERVER_TS"
        )

        assertFalse("conversationSessions must not contain transcriptPreview", payload.containsKey("transcriptPreview"))
        assertFalse("conversationSessions must not contain transcript", payload.containsKey("transcript"))
        assertEquals("uid-123", payload["userId"])
        assertEquals("Job Interview", payload["scenario"])
        assertEquals(6L, payload["turnCount"])
    }

    @Test
    fun `activityAttempts payload never uploads recordedMediaPath`() {
        val attempt = ActivityAttemptRecord(
            activityId = "act_01",
            moduleId = "mod_fluency",
            skillCategory = "Fluency",
            exerciseType = "SPEAKING_PROMPT",
            evaluatorName = "LocalSpeechEvaluator",
            dateKey = "2026-11-21",
            durationSeconds = 60L,
            objectiveScore = 88L,
            confidenceSelfRating = 75L,
            passed = true,
            aiEvaluated = false,
            responseSummary = "Delivered structured 60-second answer.",
            recordedMediaPath = sampleMediaPath,
            wordsPerMinute = 128L,
            fillerCount = 1L
        )

        val payload = ActivityFirestoreRepository.buildActivityAttemptPayload(
            uid = "uid-123",
            attempt = attempt,
            timestamp = "SERVER_TS"
        )

        assertFalse("activityAttempts must not contain recordedMediaPath", payload.containsKey("recordedMediaPath"))
        assertFalse(
            "activityAttempts values must not leak device media file paths",
            payload.values.any { it.toString().contains(sampleMediaPath) }
        )
        assertEquals("uid-123", payload["userId"])
        assertEquals("act_01", payload["activityId"])
        assertEquals(88L, payload["objectiveScore"])
    }

    @Test
    fun `SettingsScreen privacy statement accurately reflects on-device transcripts and Firestore metadata`() {
        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(),
                    userEmail = "speaker@speakcraft.app",
                    isAdmin = false,
                    updateStatusSummary = "Up to date",
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onSetUpdateChannel = {},
                    onOpenUpdater = {},
                    onCreateBackupNow = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {}
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_ai_privacy_statement"))
        composeRule.onNodeWithTag("settings_ai_privacy_statement").assertIsDisplayed()
        composeRule.onNodeWithText("transcripts stay strictly on this device", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("never uploaded to Firestore or any Cloud AI service", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("device file paths", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("system speech provider", substring = true).assertIsDisplayed()
    }
}
