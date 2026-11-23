package com.example

import android.app.Application
import android.content.Context
import com.example.data.AppContainer

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
    }
}

/**
 * Safe accessor for the process-scoped [AppContainer].
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as? SpeakCraftApplication)?.container
        ?: AppContainer(applicationContext)
