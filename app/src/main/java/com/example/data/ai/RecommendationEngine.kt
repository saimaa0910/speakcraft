package com.example.data.ai

import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.SpeakingSession
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile

data class RecommendedNextAction(
    val title: String,
    val subtitle: String,
    val reason: String,
    val targetRoute: String,
    val badge: String,
    val ctaLabel: String
)

object RecommendationEngine {

    fun recommendNextAction(
        profile: UserProfile?,
        sessions: List<SpeakingSession>,
        learningProgress: List<UserLearningProgress>,
        todayActivity: DailyActivity?
    ): RecommendedNextAction {
        val completedTodayCount = todayActivity?.completedStepIds?.size ?: 0
        val latest = sessions.firstOrNull()

        // 1. If recent speech feedback flagged specific actionable mistakes or repeated fillers, target them directly
        if (latest != null) {
            val allMistakesText = (latest.mistakes + latest.fillerWordsDetected + latest.repeatedExpressions)
                .joinToString(" ").lowercase()

            if (latest.fillerWordsDetected.size >= 3 || allMistakesText.contains("filler") || allMistakesText.contains("um")) {
                return RecommendedNextAction(
                    title = "Zero-Filler 60s Sprint",
                    subtitle = "Replace vocal fillers with clean 1-second pauses",
                    reason = "Your last recording detected ${latest.fillerWordsDetected.size.coerceAtLeast(1)} filler patterns.",
                    targetRoute = "challenges",
                    badge = "Mistake Focus",
                    ctaLabel = "Start 60s Sprint"
                )
            }
            if (latest.pronunciationAssessable && latest.pronunciationScore in 1..69) {
                return RecommendedNextAction(
                    title = "Articulation & Clarity Drills",
                    subtitle = "Minimal pairs & final consonant release",
                    reason = "Your recent pronunciation clarity scored ${latest.pronunciationScore}/100.",
                    targetRoute = "articulation_lab",
                    badge = "Clarity Focus",
                    ctaLabel = "Open Articulation Lab"
                )
            }
            if (latest.articulationScore in 1..69 || allMistakesText.contains("structure") || allMistakesText.contains("ramble")) {
                return RecommendedNextAction(
                    title = "PREP Impromptu Framework",
                    subtitle = "Point -> Reason -> Example -> Point in 60s",
                    reason = "Sharpen thought organization based on your latest session (${latest.articulationScore}/100).",
                    targetRoute = "lesson_detail/lesson_tech_prep",
                    badge = "Structure Focus",
                    ctaLabel = "Practice PREP Lesson"
                )
            }
        }

        // 2. If user hasn't completed core verified foundational lessons yet
        val completedLessonIds = learningProgress.filter { it.completed }.map { it.lessonId }.toSet()
        val nextLesson = CurriculumCatalog.lessons.firstOrNull { it.sourceVerified && it.id !in completedLessonIds }
        if (sessions.isEmpty() && nextLesson != null) {
            return RecommendedNextAction(
                title = nextLesson.title,
                subtitle = nextLesson.objective,
                reason = "Build your core communication foundation and record your first baseline.",
                targetRoute = "lesson_detail/${nextLesson.id}",
                badge = "Foundation Lesson",
                ctaLabel = "Start Lesson"
            )
        }

        // 3. Lowest profile score routing
        if (profile != null) {
            val lowestMetric = listOf(
                "fluency" to profile.fluencyScore,
                "articulation" to profile.articulationScore,
                "vocabulary" to profile.vocabularyScore,
                "listening" to profile.listeningScore
            ).minByOrNull { it.second }

            when (lowestMetric?.first) {
                "articulation" -> return RecommendedNextAction(
                    title = "5 Vocal Foundations Warm-Up",
                    subtitle = "Modulate Rate, Pause, Melody, Volume & Character",
                    reason = "Articulation is currently your highest-leverage growth score (${profile.articulationScore}/100).",
                    targetRoute = "voice_training",
                    badge = "Vocal Lever",
                    ctaLabel = "Train Voice"
                )
                "vocabulary" -> return RecommendedNextAction(
                    title = "Active Vocabulary & Usage Lab",
                    subtitle = "Practice today's words in natural sentences",
                    reason = "Strengthen expressive precision (Vocabulary ${profile.vocabularyScore}/100).",
                    targetRoute = "vocabulary",
                    badge = "Word Power",
                    ctaLabel = "Practice Words"
                )
                "listening" -> return RecommendedNextAction(
                    title = "Active Listening & Shadowing",
                    subtitle = "Calibrate cadence & 4 Ways of Listening",
                    reason = "Boost listening comprehension & rhythm (${profile.listeningScore}/100).",
                    targetRoute = "listening_lab",
                    badge = "Listening Focus",
                    ctaLabel = "Start Shadowing"
                )
            }
        }

        // 4. Daily routine fallback if today isn't complete yet
        if (completedTodayCount < 4) {
            return RecommendedNextAction(
                title = "Today's 9-Step Speaking Routine",
                subtitle = "$completedTodayCount/9 steps completed today",
                reason = "Keep your daily communication streak moving forward.",
                targetRoute = "daily_practice",
                badge = "Daily Plan",
                ctaLabel = "Continue Routine"
            )
        }

        // 5. Default Record & Review nudge
        return RecommendedNextAction(
            title = "Record & 3-Stage Audit",
            subtitle = "Capture 60 seconds and benchmark against your prior clip",
            reason = "Consistent self-audit compounds vocal confidence fastest.",
            targetRoute = "speaking_lab",
            badge = "Speaking Lab",
            ctaLabel = "Record Now"
        )
    }
}
