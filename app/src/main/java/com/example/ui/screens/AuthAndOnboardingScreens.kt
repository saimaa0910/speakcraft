package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.StepProgressIndicator
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import kotlin.math.roundToInt

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = MaterialTheme.speakCraftColors
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = { isCheckingSession = false },
            scope = scope
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
            val heroSection: @Composable (Modifier) -> Unit = { secModifier ->
                Column(
                    modifier = secModifier,
                    horizontalAlignment = if (tier.isExpandedTablet) Alignment.Start else Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_onboarding_stage),
                        contentDescription = "SpeakCraft coaching stage illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (tier.isExpandedTablet) 260.dp else 220.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(SpeakCraftSpacing.xl))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = "SpeakCraft logo icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            "SpeakCraft",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    Spacer(Modifier.height(SpeakCraftSpacing.sm))
                    Text(
                        text = "AI Communication Coach grounded in Vinh Giang (@askvinh) frameworks",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = if (tier.isExpandedTablet) TextAlign.Start else TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val authCardSection: @Composable (Modifier) -> Unit = { secModifier ->
                Column(
                    modifier = secModifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCheckingSession) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.testTag("auth_checking_session_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Checking existing Google session...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(SpeakCraftSpacing.md))
                    }
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(SpeakCraftSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("What's inside:", style = MaterialTheme.typography.labelLarge)
                            AuthFeatureRow(
                                icon = Icons.Default.PlayCircle,
                                tint = ext.info,
                                bg = ext.infoContainer,
                                text = "Live YouTube @askvinh intelligence & source-attributed lessons"
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.GraphicEq,
                                tint = MaterialTheme.colorScheme.primary,
                                bg = MaterialTheme.colorScheme.primaryContainer,
                                text = "Multimodal AI speech & 3-Stage Video Audit feedback"
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.ChatBubble,
                                tint = ext.indigo,
                                bg = ext.indigoContainer,
                                text = "13 Conversation Simulator roles, Pronunciation & Shadowing Labs"
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.School,
                                tint = ext.success,
                                bg = ext.successContainer,
                                text = "30/60/90-day programs & evidence-based analytics"
                            )
                        }
                    }
                    Spacer(Modifier.height(SpeakCraftSpacing.md))
                    FeatureGuideStrip(featureKey = "auth")
                    Spacer(Modifier.height(SpeakCraftSpacing.xl))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = SpeakCraftSpacing.md)
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
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SpeakCraftSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (tier.isExpandedTablet) {
                    Row(
                        modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        heroSection(Modifier.weight(1f))
                        authCardSection(Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = SpeakCraftSpacing.maxDialogContentWidth),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        heroSection(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(SpeakCraftSpacing.lg))
                        authCardSection(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bg,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    var currentStep by remember { mutableIntStateOf(1) }
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

    fun applyLevelPresets(selectedLevel: String) {
        level = selectedLevel
        when (selectedLevel) {
            "Beginner" -> {
                fluency = 42f; pronunciation = 45f; vocab = 48f; grammar = 50f
                articulation = 40f; listening = 50f; confidence = 38f
            }
            "Intermediate" -> {
                fluency = 60f; pronunciation = 62f; vocab = 64f; grammar = 68f
                articulation = 58f; listening = 65f; confidence = 55f
            }
            "Upper-Intermediate" -> {
                fluency = 74f; pronunciation = 75f; vocab = 76f; grammar = 78f
                articulation = 72f; listening = 78f; confidence = 70f
            }
            "Advanced" -> {
                fluency = 86f; pronunciation = 85f; vocab = 88f; grammar = 90f
                articulation = 84f; listening = 88f; confidence = 82f
            }
        }
    }

    BackHandler(enabled = currentStep > 1) {
        currentStep = (currentStep - 1).coerceAtLeast(1)
    }

    val goalPresets = listOf(
        "Speak smoothly in interviews without filler words",
        "Master vocal variety, pacing, and pauses",
        "Present concisely to managers and teams",
        "Think in English without translating mentally"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
            val wizardMaxWidth = if (tier.isExpandedTablet) 760.dp else 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SpeakCraftSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = wizardMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
                ) {
                    StepProgressIndicator(
                        currentStep = currentStep,
                        totalSteps = 3,
                        stepTitle = when (currentStep) {
                            1 -> "Identity & Speaking Level"
                            2 -> "Coaching Goal & Cadence"
                            else -> "Baseline Calibration"
                        }
                    )
                    FeatureGuideStrip(featureKey = "onboarding")

                    AnimatedContent(targetState = currentStep, label = "onboarding_step") { step ->
                        when (step) {
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) {
                                    Text(
                                        "Welcome to SpeakCraft",
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        "Let's tailor your coaching curriculum and @askvinh 30-Day Communication Challenge baseline.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Your Name") },
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                                    )
                                    Text("Current English Speaking Level", style = MaterialTheme.typography.labelLarge)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                                    ) {
                                        listOf("Beginner", "Intermediate", "Upper-Intermediate", "Advanced").forEach { tierName ->
                                            FilterChip(
                                                selected = level == tierName,
                                                onClick = { applyLevelPresets(tierName) },
                                                label = { Text(tierName) },
                                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            )
                                        }
                                    }
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    ) {
                                        Text(
                                            text = "Selecting a level pre-fills realistic skill baselines you can fine-tune in Step 3.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(SpeakCraftSpacing.md)
                                        )
                                    }
                                }
                            }
                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) {
                                    Text(
                                        "Set Your Goal & Rhythm",
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        "Choose a focus area or write your own communication target.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = goal,
                                        onValueChange = { goal = it },
                                        label = { Text("Primary Communication Goal") },
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth().testTag("onboarding_goal_input")
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                                    ) {
                                        goalPresets.forEach { preset ->
                                            FilterChip(
                                                selected = goal == preset,
                                                onClick = { goal = preset },
                                                label = { Text(preset) }
                                            )
                                        }
                                    }
                                    Text("Daily Practice Commitment", style = MaterialTheme.typography.labelLarge)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                                    ) {
                                        listOf(10, 15, 25, 45).forEach { mins ->
                                            FilterChip(
                                                selected = dailyMins.roundToInt() == mins,
                                                onClick = { dailyMins = mins.toFloat() },
                                                label = { Text("${mins}m / day") },
                                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            )
                                        }
                                    }
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                                    ) {
                                        Column(modifier = Modifier.padding(SpeakCraftSpacing.md)) {
                                            AssessmentSlider(
                                                label = "Daily Practice Goal",
                                                valueDisplay = "${dailyMins.roundToInt()} min",
                                                value = dailyMins,
                                                range = 5f..60f
                                            ) { dailyMins = it }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) {
                                    Text(
                                        "Calibrate Your Skill Baseline",
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        "Adjust your starting self-assessment. SpeakCraft updates these automatically from real AI speech scores.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(SpeakCraftSpacing.lg),
                                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                                        ) {
                                            AssessmentSlider("Self-Rated Fluency", "${fluency.roundToInt()}%", fluency, 20f..95f) { fluency = it }
                                            AssessmentSlider("Pronunciation Clarity", "${pronunciation.roundToInt()}%", pronunciation, 20f..95f) { pronunciation = it }
                                            AssessmentSlider("Active Vocabulary", "${vocab.roundToInt()}%", vocab, 20f..95f) { vocab = it }
                                            AssessmentSlider("Spoken Grammar", "${grammar.roundToInt()}%", grammar, 20f..95f) { grammar = it }
                                            AssessmentSlider("Articulation & Structure", "${articulation.roundToInt()}%", articulation, 20f..95f) { articulation = it }
                                            AssessmentSlider("Listening Comprehension", "${listening.roundToInt()}%", listening, 20f..95f) { listening = it }
                                            AssessmentSlider("Speaking Confidence (Self-Assessment)", "${confidence.roundToInt()}%", confidence, 10f..95f) { confidence = it }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("onboarding_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Back")
                            }
                        }
                        if (currentStep < 3) {
                            Button(
                                onClick = { currentStep++ },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("onboarding_next_button")
                            ) {
                                Text("Continue")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        } else {
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
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("complete_onboarding_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Start My Coaching Plan")
                            }
                        }
                    }
                    Spacer(Modifier.height(SpeakCraftSpacing.xl))
                }
            }
        }
    }
}

@Composable
private fun AssessmentSlider(
    label: String,
    valueDisplay: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
