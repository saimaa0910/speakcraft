package com.example

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Initializes FirebaseApp for Robolectric unit tests, falling back to deterministic
 * synthetic FirebaseOptions when `app/google-services.json` is absent (such as on
 * secret-free PR CI runners).
 */
internal fun ensureTestFirebaseApp(context: Context): FirebaseApp {
    FirebaseApp.getApps(context).firstOrNull()?.let { return it }
    FirebaseApp.initializeApp(context)?.let { return it }
    val fallbackOptions = FirebaseOptions.Builder()
        .setApplicationId("1:000000000000:android:speakcrafttest")
        .setApiKey("test-api-key-not-used-in-unit-tests")
        .setProjectId("demo-speakcraft-test")
        .build()
    return FirebaseApp.initializeApp(context, fallbackOptions)
}
