package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.ai.InboundGeminiLink
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.firebase.configureAppCheck
import com.example.ui.SpeakCraftRoot

class MainActivity : ComponentActivity() {
    private var inboundGeminiLink by mutableStateOf<InboundGeminiLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)
        inboundGeminiLink = PhoneGeminiBridge.parseInboundIntent(intent, this)

        enableEdgeToEdge()
        setContent {
            SpeakCraftRoot(
                inboundGeminiLink = inboundGeminiLink,
                onConsumeInboundGeminiLink = { inboundGeminiLink = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inboundGeminiLink = PhoneGeminiBridge.parseInboundIntent(intent, this)
    }
}
