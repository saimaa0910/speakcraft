package com.example

import android.app.Application
import android.content.Context
import com.example.data.AppContainer
import kotlinx.coroutines.launch

/**
 * Custom Application class establishing the process-scoped dependency graph (T-02.1.1, CMP-01, ADR-001).
 *
 * Owns the process-scoped [AppContainer], ensuring singletons are created once per process
 * lifecycle and never recreated or leaked across configuration changes (device rotation, FND-ARCH-02).
 */
class SpeakCraftApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Automatically provision and install device-suitable lightweight AI models on install / startup
        container.appScope.launch(container.dispatchers.io) {
            runCatching {
                container.deviceAdaptiveModelInstaller.autoInstallOnFirstLaunch()
            }
        }
    }
}

/**
 * Safe accessor for the process-scoped [AppContainer].
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as? SpeakCraftApplication)?.container
        ?: AppContainer(applicationContext)
