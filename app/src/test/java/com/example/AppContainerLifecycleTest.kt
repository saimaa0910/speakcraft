package com.example

import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies Stage B Sprint 3 Task T-02.1.1 (FND-ARCH-02, ARC-01 §2, §7, CMP-01, CMP-02):
 * - SpeakCraftApplication owns the process-scoped AppContainer.
 * - AppContainer is not recreated across Activity configuration changes (device rotation).
 * - AppContainer singletons remain persistent and stable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SpeakCraftApplication::class)
class AppContainerLifecycleTest {

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<SpeakCraftApplication>()
        ensureTestFirebaseApp(app)
    }

    @Test
    fun `application owns initialized process-scoped AppContainer`() {
        val app = ApplicationProvider.getApplicationContext<SpeakCraftApplication>()
        assertNotNull("Application must not be null", app)
        assertNotNull("container must be initialized in onCreate()", app.container)

        val contextContainer = app.appContainer
        assertSame("Context extension appContainer must return Application container", app.container, contextContainer)
    }

    @Test
    fun `activity recreation and rotation does not recreate AppContainer`() {
        val app = ApplicationProvider.getApplicationContext<SpeakCraftApplication>()
        val initialContainer = app.container

        // Create and setup activity using standard ComponentActivity to verify activity-lifecycle stability
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val activityContainer = activity.appContainer
        assertSame("Activity must use Application-scoped container", initialContainer, activityContainer)

        // Simulate configuration change / rotation
        controller.recreate()
        val recreatedActivity = controller.get()
        val recreatedContainer = recreatedActivity.appContainer

        assertSame(
            "Device rotation must not recreate AppContainer (fixes FND-ARCH-02)",
            initialContainer,
            recreatedContainer
        )
    }

    @Test
    fun `AppContainer singletons are lazily initialized and remain stable`() {
        val app = ApplicationProvider.getApplicationContext<SpeakCraftApplication>()
        val container = app.container

        val prefs1 = container.preferencesRepository
        val prefs2 = container.preferencesRepository
        assertSame("preferencesRepository must be a singleton within container", prefs1, prefs2)

        val clock1 = container.clock
        val clock2 = container.clock
        assertSame("clock must be a singleton within container", clock1, clock2)

        val dateKeys1 = container.dateKeys
        val dateKeys2 = container.dateKeys
        assertSame("dateKeys must be a singleton within container", dateKeys1, dateKeys2)

        val dispatchers1 = container.dispatchers
        val dispatchers2 = container.dispatchers
        assertSame("dispatchers must be a singleton within container", dispatchers1, dispatchers2)

        assertTrue("clock must report positive epoch millis", container.clock.nowMillis() > 0L)
        assertTrue("dateKeys must match YYYY-MM-DD pattern", container.dateKeys.todayDateKey().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}
