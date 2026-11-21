package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppPreferencesRepository
import com.example.ui.components.LaunchCadence
import com.example.ui.components.LaunchExperienceViewModel
import com.example.ui.components.LaunchStage
import com.example.ui.components.LaunchTimelineSpec
import com.example.ui.components.SpeakCraftLaunchOverlay
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.SpeakCraftTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SpeakCraftLaunchAnimationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `timeline spec progresses deliberately through all 4 stages and completes`() {
        assertTrue(LaunchCadence.FIRST_LAUNCH.durationMs >= 2600)
        assertTrue(LaunchCadence.RETURNING_LAUNCH.durationMs >= 1800)
        assertTrue(LaunchCadence.REDUCED_MOTION.durationMs >= 500)

        val stage1 = LaunchTimelineSpec.evaluate(0.05f, LaunchCadence.FIRST_LAUNCH)
        assertEquals(LaunchStage.INITIAL_APPEARANCE, stage1.stage)
        assertTrue(stage1.backgroundGlowAlpha > 0f)
        assertEquals(0f, stage1.emblemAlpha, 0.001f)

        val stage2 = LaunchTimelineSpec.evaluate(0.28f, LaunchCadence.FIRST_LAUNCH)
        assertEquals(LaunchStage.BRAND_REVEAL, stage2.stage)
        assertTrue(stage2.emblemAlpha > 0.5f)
        assertTrue(stage2.emblemScale in 0.78f..1.06f)
        assertTrue(stage2.emblemArcSweep > 0f)
        assertTrue(stage2.orbitalAngleDeg > 0f)
        assertTrue(stage2.waveReveal > 0f)

        val stage3 = LaunchTimelineSpec.evaluate(0.75f, LaunchCadence.FIRST_LAUNCH)
        assertEquals(LaunchStage.IDENTITY_REVEAL, stage3.stage)
        assertTrue(stage3.eyebrowAlpha > 0.9f)
        assertEquals(1f, stage3.wordmarkAlpha, 0.001f)
        assertTrue(stage3.wordmarkLetterSpacingSp <= 0f)
        assertTrue(stage3.taglineAlpha > 0.7f)
        assertTrue(stage3.dividerSweep > 0.9f)

        val stage4 = LaunchTimelineSpec.evaluate(0.94f, LaunchCadence.FIRST_LAUNCH)
        assertEquals(LaunchStage.FINAL_TRANSITION, stage4.stage)
        assertTrue(stage4.exitAlpha < 1f)
        assertTrue(stage4.exitScale > 1f)

        val done = LaunchTimelineSpec.evaluate(1.0f, LaunchCadence.FIRST_LAUNCH)
        assertEquals(LaunchStage.COMPLETED, done.stage)
        assertEquals(0f, done.exitAlpha, 0.001f)
    }

    @Test
    fun `first launch vs returning launch preferences select distinct deliberate cadences`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = AppPreferencesRepository(ctx)
        val initialPrefs = prefsRepo.preferencesFlow.first()

        val firstVm = LaunchExperienceViewModel()
        firstVm.latchCadenceIfNeeded(isFirstLaunch = true, reducedMotion = false)
        assertEquals(LaunchCadence.FIRST_LAUNCH, firstVm.latchedCadence.value)

        prefsRepo.markInitialLaunchCompleted()
        val updatedPrefs = prefsRepo.preferencesFlow.first()
        assertTrue(updatedPrefs.hasCompletedInitialLaunch || !initialPrefs.hasCompletedInitialLaunch)

        val returningVm = LaunchExperienceViewModel()
        returningVm.latchCadenceIfNeeded(
            isFirstLaunch = !updatedPrefs.hasCompletedInitialLaunch,
            reducedMotion = false
        )
        assertEquals(LaunchCadence.RETURNING_LAUNCH, returningVm.latchedCadence.value)
        assertTrue(LaunchCadence.RETURNING_LAUNCH.durationMs < LaunchCadence.FIRST_LAUNCH.durationMs)
    }

    @Test
    fun `returning from background and configuration changes never replay completed launch`() {
        val vm = LaunchExperienceViewModel()
        vm.latchCadenceIfNeeded(isFirstLaunch = true, reducedMotion = false)
        assertEquals(LaunchCadence.FIRST_LAUNCH, vm.latchedCadence.value)
        assertTrue(vm.isOverlayVisible.value)

        vm.latchCadenceIfNeeded(isFirstLaunch = false, reducedMotion = false)
        assertEquals(LaunchCadence.FIRST_LAUNCH, vm.latchedCadence.value)

        vm.markCompleted()
        assertFalse(vm.isOverlayVisible.value)
        assertEquals(LaunchStage.COMPLETED, vm.currentStage.value)

        vm.latchCadenceIfNeeded(isFirstLaunch = true, reducedMotion = false)
        assertFalse(vm.isOverlayVisible.value)
    }

    @Test
    fun `first launch overlay advances smoothly through brand and identity stages into signed-in content`() {
        composeRule.mainClock.autoAdvance = false
        val vm = LaunchExperienceViewModel()
        vm.latchCadenceIfNeeded(isFirstLaunch = true, reducedMotion = false)

        composeRule.setContent {
            val visible by vm.isOverlayVisible.collectAsStateWithLifecycle()
            SpeakCraftTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Signed-in Dashboard Ready")
                    SpeakCraftLaunchOverlay(
                        visible = visible,
                        cadence = LaunchCadence.FIRST_LAUNCH,
                        reducedMotion = false,
                        onStageChanged = { vm.updateStage(it) },
                        onFinished = { vm.markCompleted() }
                    )
                }
            }
        }

        // Advance into settled Stage 3 identity reveal (~78% of 2900ms -> 2260ms)
        composeRule.mainClock.advanceTimeBy(2260)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("launch_animation_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_emblem").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_sound_wave").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()
        composeRule.onNodeWithText("VOICE & COMMUNICATION STUDIO").assertIsDisplayed()
        composeRule.onNodeWithText("Speak with Confidence. Communicate with Impact.").assertIsDisplayed()

        // Complete Stage 4 transition cleanly (2900ms total)
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.waitForIdle()

        assertFalse(vm.isOverlayVisible.value)
        assertEquals(LaunchStage.COMPLETED, vm.currentStage.value)
        composeRule.onNodeWithTag("launch_animation_overlay").assertDoesNotExist()
        composeRule.onNodeWithText("Signed-in Dashboard Ready").assertIsDisplayed()
    }

    @Test
    fun `signed-out flow renders AuthScreen underneath without blocking and reveals it on completion`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        composeRule.mainClock.autoAdvance = false
        val vm = LaunchExperienceViewModel()
        vm.latchCadenceIfNeeded(isFirstLaunch = false, reducedMotion = false)

        composeRule.setContent {
            val visible by vm.isOverlayVisible.collectAsStateWithLifecycle()
            SpeakCraftTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AuthScreen(onAuthSuccess = {})
                    SpeakCraftLaunchOverlay(
                        visible = visible,
                        cadence = LaunchCadence.RETURNING_LAUNCH,
                        reducedMotion = false,
                        onStageChanged = { vm.updateStage(it) },
                        onFinished = { vm.markCompleted() }
                    )
                }
            }
        }

        // Mid-returning launch (1580ms of 2100ms): settled brand identity visible
        composeRule.mainClock.advanceTimeBy(1580)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launch_animation_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()

        // Finish returning launch (2100ms total)
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launch_animation_overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("google_sign_in_button").assertExists()
    }

    @Test
    fun `light and dark themes render crisp visible brand lockup and respect reduced motion and vector fallback`() {
        composeRule.mainClock.autoAdvance = false
        var isDark by mutableStateOf(false)
        var finishedCount = 0

        composeRule.setContent {
            SpeakCraftTheme(darkTheme = isDark) {
                SpeakCraftLaunchOverlay(
                    visible = true,
                    cadence = LaunchCadence.REDUCED_MOTION,
                    reducedMotion = true,
                    forceVectorFallback = true,
                    onFinished = { finishedCount++ }
                )
            }
        }

        // Light theme with reduced motion & native crest
        composeRule.mainClock.advanceTimeBy(220)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launch_animation_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_vector_fallback_crest").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_tagline").assertIsDisplayed()

        // Switch to Dark theme
        isDark = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launch_animation_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_vector_fallback_crest").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()

        // Complete reduced motion duration (600ms)
        composeRule.mainClock.advanceTimeBy(450)
        composeRule.waitForIdle()
        assertTrue(finishedCount >= 1)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h568dp")
    fun `launch overlay scales emblem cleanly on compact 320dp phone screen`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SpeakCraftTheme(darkTheme = true) {
                SpeakCraftLaunchOverlay(
                    visible = true,
                    cadence = LaunchCadence.FIRST_LAUNCH,
                    reducedMotion = false,
                    onFinished = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(2260)
        composeRule.waitForIdle()

        val compactBounds = composeRule.onNodeWithTag("launch_brand_emblem").assertIsDisplayed().getBoundsInRoot()
        val compactWidth = compactBounds.right - compactBounds.left
        assertTrue("Expected compact phone emblem <= 108.dp, got $compactWidth", compactWidth <= 108.dp)
        composeRule.onNodeWithTag("launch_sound_wave").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_tagline").assertIsDisplayed()
    }

    @Test
    fun `launch overlay scales emblem cleanly on standard 412dp phone screen`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SpeakCraftTheme(darkTheme = true) {
                SpeakCraftLaunchOverlay(
                    visible = true,
                    cadence = LaunchCadence.FIRST_LAUNCH,
                    reducedMotion = false,
                    onFinished = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(2260)
        composeRule.waitForIdle()

        val standardBounds = composeRule.onNodeWithTag("launch_brand_emblem").assertIsDisplayed().getBoundsInRoot()
        val standardWidth = standardBounds.right - standardBounds.left
        assertTrue("Expected standard phone emblem in 120..128.dp, got $standardWidth", standardWidth in 120.dp..128.dp)
        composeRule.onNodeWithTag("launch_sound_wave").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_tagline").assertIsDisplayed()
    }
}
