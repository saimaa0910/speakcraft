package com.example

import com.example.data.model.CurriculumCatalog
import com.example.data.model.FeatureTutorialRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces Stage B Sprint 2 Task T-04.1.1 (ADR-011, D-08, ISS-05, FND-UI-03, FND-DOM-09):
 * 1. YouTube Intelligence client and screen are removed from the codebase and classpath.
 * 2. Zero creator branding strings or handles exist anywhere under app/src/main/.
 * 3. All 15 bundled catalogue items (12 curated lessons + 3 editorial drafts) remain intact
 *    in CurriculumCatalog for EP-09 re-expression, with zero channel-root YouTube URLs.
 * 4. CI security-static workflow guards against reintroduction of creator branding or YouTube classes.
 */
class YouTubeRemovalAndBrandingPolicyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, ".github").exists()) cwd else cwd.parentFile ?: cwd
    }

    @Test
    fun `youtube intelligence client and screen are absent from source tree and classpath`() {
        val root = repoRoot()
        assertFalse(
            "YouTubeIntelligenceClient.kt must be deleted",
            File(root, "app/src/main/java/com/example/data/youtube/YouTubeIntelligenceClient.kt").exists()
        )
        assertFalse(
            "YouTubeIntelligenceScreen.kt must be deleted",
            File(root, "app/src/main/java/com/example/ui/screens/YouTubeIntelligenceScreen.kt").exists()
        )

        val removedClasses = listOf(
            "com.example.data.youtube.YouTubeIntelligenceClient",
            "com.example.ui.screens.YouTubeIntelligenceScreenKt",
            "com.example.data.model.YouTubeSource",
            "com.example.data.model.ExtractedTechnique",
            "com.example.data.ai.ExtractedYouTubeBundle"
        )
        removedClasses.forEach { fqcn ->
            val loaded = runCatching { Class.forName(fqcn) }.isSuccess
            assertFalse("Removed class $fqcn must not exist on classpath", loaded)
        }
    }

    @Test
    fun `app src main contains zero creator branding strings or youtube intelligence symbols`() {
        val mainDir = File(repoRoot(), "app/src/main")
        assertTrue("app/src/main must exist", mainDir.isDirectory)

        val prohibited = Regex("""vinh|giang|askvinh|YouTubeIntelligenceClient|YouTubeIntelligenceScreen""", RegexOption.IGNORE_CASE)
        val violations = mutableListOf<String>()

        mainDir.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java", "xml", "json", "txt") }
            .forEach { file ->
                file.readLines().forEachIndexed { idx, line ->
                    if (prohibited.containsMatchIn(line)) {
                        val rel = file.relativeTo(repoRoot()).path
                        violations += "$rel:${idx + 1}: ${line.trim()}"
                    }
                }
            }

        assertTrue(
            "Prohibited creator branding or YouTube Intelligence references found in app/src/main:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `bundled curriculum catalog preserves all 15 items for EP-09 with zero creator URLs`() {
        assertEquals(
            "D-08 requires preserving all 15 bundled technique seeds for EP-09 re-expression",
            15,
            CurriculumCatalog.curatedTechniqueCatalog.size
        )
        assertEquals(
            "All 12 curated lessons must remain intact",
            12,
            CurriculumCatalog.lessons.size
        )
        assertEquals(15, CurriculumCatalog.contentModules.size)
        assertEquals(10, CurriculumCatalog.speakingChallenges.size)
        assertEquals(3, CurriculumCatalog.shadowingSegments.size)
        assertEquals(6, CurriculumCatalog.publicSpeakingModules.size)
        assertEquals(7, CurriculumCatalog.structuredPrograms.size)

        CurriculumCatalog.curatedTechniqueCatalog.forEach { seed ->
            assertFalse(
                "Seed ${seed.id} must not point to YouTube URLs before EP-09 verification",
                seed.sourceVideoUrl.contains("youtube.com", ignoreCase = true)
            )
        }
        CurriculumCatalog.shadowingSegments.forEach { segment ->
            assertFalse(
                "Shadowing segment ${segment.id} must not point to YouTube URLs before EP-09",
                segment.sourceVideoUrl.contains("youtube.com", ignoreCase = true)
            )
        }
        CurriculumCatalog.publicSpeakingModules.forEach { module ->
            assertFalse(
                "Public speaking module ${module.id} must not point to YouTube URLs before EP-09",
                module.sourceVideoUrl.contains("youtube.com", ignoreCase = true)
            )
        }
        assertFalse(
            "youtube_library must be removed from FeatureTutorialRegistry",
            FeatureTutorialRegistry.allTutorials.any { it.featureKey == "youtube_library" }
        )
    }

    @Test
    fun `ci security-static enforces ADR-011 creator branding and youtube removal check`() {
        val ciContent = File(repoRoot(), ".github/workflows/ci.yml").readText()
        assertTrue(
            "ci.yml must include ADR-011 Check #6 guarding app/src/main",
            ciContent.contains("ADR-011") &&
                ciContent.contains("YouTubeIntelligenceClient") &&
                ciContent.contains("app/src/main")
        )
    }
}
