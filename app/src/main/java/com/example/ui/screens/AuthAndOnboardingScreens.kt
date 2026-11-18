package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import kotlin.math.roundToInt

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.img_onboarding_stage),
                    contentDescription = "SpeakCraft coaching stage illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = "SpeakCraft logo icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("SpeakCraft", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AI Communication Coach grounded in Vinh Giang (@askvinh) frameworks",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What's inside:", style = MaterialTheme.typography.labelLarge)
                        Text("• Live YouTube @askvinh intelligence & source-attributed lessons", style = MaterialTheme.typography.bodyMedium)
                        Text("• Multimodal AI speech & 3-Stage Video Audit feedback", style = MaterialTheme.typography.bodyMedium)
                        Text("• 13 Conversation Simulator roles, Pronunciation & Shadowing Labs", style = MaterialTheme.typography.bodyMedium)
                        Text("• 30/60/90-day programs & evidence-based analytics", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingAssessmentScreen(
    defaultDisplayName: String,
    onComplete: (
        displayName: String,
        level: String,
        goal: String,
        dailyMinutes: Long,
        fluency: Long,
        pronunciation: Long,
        vocabulary: Long,
        grammar: Long,
        articulation: Long,
        listening: Long,
        confidence: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf(defaultDisplayName.ifBlank { "Speaker" }) }
    var level by remember { mutableStateOf("Intermediate") }
    var goal by remember { mutableStateOf("Master vocal variety, eliminate filler words, and speak persuasively") }
    var dailyMins by remember { mutableFloatStateOf(15f) }
    var fluency by remember { mutableFloatStateOf(60f) }
    var pronunciation by remember { mutableFloatStateOf(62f) }
    var vocab by remember { mutableFloatStateOf(64f) }
    var grammar by remember { mutableFloatStateOf(68f) }
    var articulation by remember { mutableFloatStateOf(58f) }
    var listening by remember { mutableFloatStateOf(65f) }
    var confidence by remember { mutableFloatStateOf(55f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Initial Communication Assessment", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Calibrate your SpeakCraft baseline and enroll in the @askvinh 30-Day Communication Challenge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
            )

            Text("Current English Speaking Level", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beginner", "Intermediate", "Upper-Intermediate", "Advanced").forEach { tier ->
                    FilterChip(
                        selected = level == tier,
                        onClick = { level = tier },
                        label = { Text(tier.replace("Upper-Intermediate", "Upper-Int")) }
                    )
                }
            }

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Primary Communication Goal") },
                modifier = Modifier.fillMaxWidth().testTag("onboarding_goal_input")
            )

            AssessmentSlider("Daily Practice Goal: ${dailyMins.roundToInt()} min", dailyMins, 5f..60f) { dailyMins = it }
            AssessmentSlider("Self-Rated Fluency: ${fluency.roundToInt()}%", fluency, 20f..95f) { fluency = it }
            AssessmentSlider("Pronunciation Clarity: ${pronunciation.roundToInt()}%", pronunciation, 20f..95f) { pronunciation = it }
            AssessmentSlider("Active Vocabulary: ${vocab.roundToInt()}%", vocab, 20f..95f) { vocab = it }
            AssessmentSlider("Spoken Grammar: ${grammar.roundToInt()}%", grammar, 20f..95f) { grammar = it }
            AssessmentSlider("Articulation & Structure: ${articulation.roundToInt()}%", articulation, 20f..95f) { articulation = it }
            AssessmentSlider("Listening Comprehension: ${listening.roundToInt()}%", listening, 20f..95f) { listening = it }
            AssessmentSlider("Speaking Confidence (Self-Assessment): ${confidence.roundToInt()}%", confidence, 10f..95f) { confidence = it }

            Button(
                onClick = {
                    onComplete(
                        name,
                        level,
                        goal,
                        dailyMins.roundToInt().toLong(),
                        fluency.roundToInt().toLong(),
                        pronunciation.roundToInt().toLong(),
                        vocab.roundToInt().toLong(),
                        grammar.roundToInt().toLong(),
                        articulation.roundToInt().toLong(),
                        listening.roundToInt().toLong(),
                        confidence.roundToInt().toLong()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_onboarding_button")
            ) {
                Text("Start My Coaching Plan")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AssessmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
