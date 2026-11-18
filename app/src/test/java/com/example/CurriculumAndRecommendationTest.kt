package com.example

import com.example.data.ai.RecommendationEngine
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.SpeakingSession
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumAndRecommendationTest {

    @Test
    fun `catalog exposes all 15 communication modules with unique slugs`() {
        val modules = CurriculumCatalog.contentModules
        assertEquals(15, modules.size)
        assertEquals(15, modules.map { it.slug }.toSet().size)
        val expectedSlugs = setOf(
            "articulation_lab", "voice_training", "daily_practice", "vocabulary",
            "conversation", "listening_lab", "public_speaking", "storytelling",
            "body_language", "confidence", "thinking_articulation", "professional",
            "influence", "record_review", "challenges"
        )
        assertEquals(expectedSlugs, modules.map { it.slug }.toSet())
    }

    @Test
    fun `unverified frameworks stay marked PENDING_TRANSCRIPT and lessons remain source grounded`() {
        val pending = CurriculumCatalog.askVinhKnowledgeBase.filter { it.verificationStatus == "PENDING_TRANSCRIPT" }
        assertTrue(pending.isNotEmpty())
        pending.forEach { seed ->
            assertFalse("Pending item ${seed.id} must not claim creator-verified transcript", seed.isCreatorDerived)
            assertTrue(seed.creatorAdvice.contains("transcript", ignoreCase = true))
        }

        val verifiedLessons = CurriculumCatalog.lessons.filter { it.sourceVerified }
        assertTrue(verifiedLessons.isNotEmpty())
        verifiedLessons.forEach { lesson ->
            assertTrue(lesson.sourceVideoUrl.startsWith("https://www.youtube.com/"))
            assertTrue(lesson.instructions.isNotEmpty())
        }
    }

    @Test
    fun `recommendation engine targets recurring fillers and low pronunciation cleanly`() {
        val profile = UserProfile(userId = "u1", fluencyScore = 72L, pronunciationScore = 75L, articulationScore = 74L)

        val fillerSession = SpeakingSession(
            id = "s1",
            fillerWordsDetected = listOf("um", "like", "you know"),
            fluencyScore = 68L,
            pronunciationAssessable = false,
            pronunciationScore = 0L,
            articulationScore = 75L
        )
        val fillerRec = RecommendationEngine.recommendNextAction(profile, listOf(fillerSession), emptyList(), null)
        assertEquals("challenges", fillerRec.targetRoute)

        val pronSession = SpeakingSession(
            id = "s2",
            fillerWordsDetected = emptyList(),
            pronunciationAssessable = true,
            pronunciationScore = 58L,
            articulationScore = 76L
        )
        val pronRec = RecommendationEngine.recommendNextAction(profile, listOf(pronSession), emptyList(), null)
        assertEquals("articulation_lab", pronRec.targetRoute)

        val prepSession = SpeakingSession(
            id = "s3",
            fillerWordsDetected = emptyList(),
            pronunciationAssessable = false,
            articulationScore = 60L
        )
        val prepRec = RecommendationEngine.recommendNextAction(profile, listOf(prepSession), emptyList(), null)
        assertEquals("lesson_detail/lesson_vinh_prep", prepRec.targetRoute)
    }

    @Test
    fun `recommendation engine suggests foundation lesson when user has no sessions`() {
        val rec = RecommendationEngine.recommendNextAction(
            profile = UserProfile(userId = "u2"),
            sessions = emptyList(),
            learningProgress = emptyList<UserLearningProgress>(),
            todayActivity = DailyActivity(dateKey = "2026-11-18")
        )
        assertTrue(rec.targetRoute.startsWith("lesson_detail/"))
        assertEquals("Foundation Lesson", rec.badge)
    }
}
