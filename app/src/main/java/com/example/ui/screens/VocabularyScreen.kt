package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.VocabEvaluationResult
import com.example.data.ai.local.language.LocalVocabStageEvaluation
import com.example.data.learning.LearningLevel
import com.example.data.learning.VocabularyMasteryEngine
import com.example.data.learning.WeeklyAssessmentEligibility
import com.example.data.learning.WeeklySpokenAnswerResult
import com.example.data.learning.WeeklySpokenQuestion
import com.example.data.learning.WeeklyVocabularyAssessmentRecord
import com.example.data.media.LiveSpeechCaptureResult
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.UiState
import com.example.data.model.VocabSeed
import com.example.data.model.VocabularyEntry
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import kotlin.math.roundToInt

private sealed interface VocabSpeechTarget {
    val key: String

    data class DailyOrLibraryWord(val seed: VocabSeed) : VocabSpeechTarget {
        override val key: String get() = "word_${seed.id}"
    }

    data class AiQuizSentence(val seed: VocabSeed) : VocabSpeechTarget {
        override val key: String get() = "quiz_${seed.id}"
    }

    data class WeeklyQuestion(val question: WeeklySpokenQuestion) : VocabSpeechTarget {
        override val key: String get() = "weekly_${question.id}"
    }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabularyScreen(
    entries: List<VocabularyEntry>,
    todayEpochDay: Long,
    vocabEvalState: UiState<VocabEvaluationResult>?,
    onSpeakWord: (String) -> Unit,
    onReviewCard: (VocabSeed, VocabularyEntry?, Boolean) -> Unit,
    onEvaluateUsage: (VocabSeed, VocabularyEntry?, String) -> Unit,
    onSaveCustomWord: (word: String, meaning: String, level: String, example: String, note: String) -> Unit,
    onClearEval: () -> Unit,
    userLevel: LearningLevel = LearningLevel.INTERMEDIATE,
    dailyVocabWords: List<VocabSeed> = emptyList(),
    completedDailyWordIds: Set<String> = emptySet(),
    dailyWordScores: Map<String, Int> = emptyMap(),
    learnedWordIds: Set<String> = emptySet(),
    weeklyEligibility: WeeklyAssessmentEligibility = WeeklyAssessmentEligibility(),
    learnedWordsForAssessment: List<VocabSeed> = emptyList(),
    ttsHelper: TtsAndSpeechHelper? = null,
    onAssessSpokenWord: (
        seed: VocabSeed,
        existing: VocabularyEntry?,
        transcript: String,
        alternatives: List<String>,
        confidence: Float
    ) -> LocalVocabStageEvaluation = { seed, _, txt, alts, conf ->
        VocabularyMasteryEngine.evaluateSpokenWordAttempt(seed, txt, alts, conf)
    },
    onCompleteWeeklyAssessment: (WeeklyVocabularyAssessmentRecord, Map<String, Boolean>) -> Unit = { _, _ -> },
    onStartSpeechCaptureOverride: (
        (onPartial: (String) -> Unit, onResult: (LiveSpeechCaptureResult) -> Unit, onError: (String) -> Unit) -> Unit
    )? = null
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    var selectedTab by remember { mutableIntStateOf(0) }
    var levelFilter by remember(userLevel) { mutableStateOf(userLevel.label) }
    var searchQuery by remember { mutableStateOf("") }

    val bank = CurriculumCatalog.vocabularyBank
    val effectiveDailyWords = remember(dailyVocabWords, userLevel, todayEpochDay, bank) {
        if (dailyVocabWords.isNotEmpty()) dailyVocabWords
        else {
            val ids = VocabularyMasteryEngine.ensureDailyVocabularyWords(
                bank = bank,
                level = userLevel,
                todayDateKey = todayEpochDay.toString()
            ).first.wordIds
            ids.mapNotNull { id -> bank.firstOrNull { it.id == id } }
        }
    }

    var localDoneIds by remember(todayEpochDay) { mutableStateOf<Set<String>>(emptySet()) }
    var localWordEvals by remember(todayEpochDay) { mutableStateOf<Map<String, LocalVocabStageEvaluation>>(emptyMap()) }

    val effectiveDoneTodayIds = completedDailyWordIds + localDoneIds
    val effectiveLearnedIds = learnedWordIds + effectiveDoneTodayIds

    // Resolve learned seeds fallback when tests pass learnedWordIds/entries without external ViewModel pool
    val effectiveLearnedForAssessment = remember(learnedWordsForAssessment, effectiveLearnedIds, entries) {
        if (learnedWordsForAssessment.isNotEmpty()) {
            learnedWordsForAssessment
        } else {
            val resolvedIds = (effectiveLearnedIds + entries.filter { it.usageAssessed || it.masteryLevel > 0L }.map { it.id }).distinct()
            VocabularyMasteryEngine.resolveLearnedSeeds(resolvedIds, bank, entries)
        }
    }

    // AI Quiz states (speak-only)
    var spokenQuizSentence by remember { mutableStateOf("") }
    var activeWeeklyQuizMode by remember { mutableStateOf(false) }
    var weeklyQuestions by remember { mutableStateOf<List<WeeklySpokenQuestion>>(emptyList()) }
    var weeklyQuestionIdx by remember { mutableIntStateOf(0) }
    var weeklyAnswersByQId by remember { mutableStateOf<Map<String, WeeklySpokenAnswerResult>>(emptyMap()) }
    var weeklyAssessmentCompleted by remember { mutableStateOf(false) }
    var weeklyAssessmentRecord by remember { mutableStateOf<WeeklyVocabularyAssessmentRecord?>(null) }

    // Live Speech Recognition state
    var pendingSpeechTarget by remember { mutableStateOf<VocabSpeechTarget?>(null) }
    var activeSpeechTarget by remember { mutableStateOf<VocabSpeechTarget?>(null) }
    var activePartialText by remember { mutableStateOf("") }
    var speechCaptureError by remember { mutableStateOf<String?>(null) }
    var errorTargetKey by remember { mutableStateOf<String?>(null) }
    var isCaptureActive by remember { mutableStateOf(false) }

    val helperListening = ttsHelper?.isListening?.collectAsStateWithLifecycle()?.value ?: false
    val helperPartial = ttsHelper?.partialTranscript?.collectAsStateWithLifecycle()?.value.orEmpty()
    val helperRms = ttsHelper?.rmsDb?.collectAsStateWithLifecycle()?.value ?: 0f
    val isCurrentlyListening = isCaptureActive || helperListening

    DisposableEffect(ttsHelper) {
        onDispose { ttsHelper?.stopDictation() }
    }

    fun launchSpeechFor(target: VocabSpeechTarget) {
        speechCaptureError = null
        errorTargetKey = null
        activeSpeechTarget = target
        activePartialText = ""
        isCaptureActive = true

        val handleFinal: (LiveSpeechCaptureResult) -> Unit = { result ->
            isCaptureActive = false
            activeSpeechTarget = null
            activePartialText = ""
            speechCaptureError = null
            errorTargetKey = null
            when (target) {
                is VocabSpeechTarget.DailyOrLibraryWord -> {
                    val existing = entries.firstOrNull { it.word.equals(target.seed.word, ignoreCase = true) }
                    val eval = onAssessSpokenWord(
                        target.seed,
                        existing,
                        result.transcript,
                        result.alternatives,
                        result.confidence.takeIf { it > 0f } ?: 0.85f
                    )
                    localWordEvals = localWordEvals + (target.seed.id to eval)
                    if (eval.passed) {
                        localDoneIds = localDoneIds + target.seed.id
                    }
                }
                is VocabSpeechTarget.AiQuizSentence -> {
                    spokenQuizSentence = result.transcript
                    val existing = entries.firstOrNull { it.word.equals(target.seed.word, ignoreCase = true) }
                    onEvaluateUsage(target.seed, existing, result.transcript)
                }
                is VocabSpeechTarget.WeeklyQuestion -> {
                    val res = VocabularyMasteryEngine.evaluateWeeklyQuestionResponse(
                        question = target.question,
                        spokenTranscript = result.transcript,
                        alternatives = result.alternatives,
                        confidence = result.confidence.takeIf { it > 0f } ?: 0.85f
                    )
                    weeklyAnswersByQId = weeklyAnswersByQId + (target.question.id to res)
                }
            }
        }

        val handleError: (String) -> Unit = { err ->
            isCaptureActive = false
            activeSpeechTarget = null
            activePartialText = ""
            speechCaptureError = err
            errorTargetKey = target.key
        }

        if (onStartSpeechCaptureOverride != null) {
            onStartSpeechCaptureOverride(
                { partial -> activePartialText = partial },
                handleFinal,
                handleError
            )
            return
        }

        val helper = ttsHelper
        if (helper == null) {
            handleError("Microphone speech capture requires an active device speech recognizer.")
            return
        }
        helper.startLiveSpeechCapture(
            preferOffline = true,
            maxResults = 5,
            onPartial = { partial -> activePartialText = partial },
            onFinalResult = handleFinal,
            onError = handleError
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingSpeechTarget
        pendingSpeechTarget = null
        if (granted && pending != null) {
            launchSpeechFor(pending)
        } else if (!granted && pending != null) {
            speechCaptureError = "Microphone permission is required for spoken vocabulary assessment."
            errorTargetKey = pending.key
        }
    }

    fun requestSpeak(target: VocabSpeechTarget) {
        if (isCurrentlyListening && activeSpeechTarget?.key == target.key) {
            ttsHelper?.stopDictation()
            isCaptureActive = false
            activeSpeechTarget = null
            return
        }
        if (onStartSpeechCaptureOverride != null) {
            launchSpeechFor(target)
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchSpeechFor(target)
        } else {
            pendingSpeechTarget = target
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val filteredBank = remember(levelFilter, searchQuery) {
        val q = searchQuery.trim()
        bank.filter { seed ->
            val levelMatch = levelFilter == "All" || seed.level.equals(levelFilter, ignoreCase = true)
            val queryMatch = q.isBlank() ||
                seed.word.contains(q, ignoreCase = true) ||
                seed.meaning.contains(q, ignoreCase = true)
            levelMatch && queryMatch
        }
    }

    val levelQuizPool = remember(userLevel, effectiveDailyWords, bank) {
        val levelSpecific = bank.filter { it.level.equals(userLevel.label, ignoreCase = true) }
        (effectiveDailyWords + levelSpecific).distinctBy { it.id }.ifEmpty { bank }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                listOf("Words", "Flashcards", "AI Quiz", "Notebook").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = {
                            ttsHelper?.stopDictation()
                            isCaptureActive = false
                            activeSpeechTarget = null
                            selectedTab = idx
                        },
                        text = { Text(label) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpeakCraftSpacing.lg, vertical = SpeakCraftSpacing.xs),
                contentAlignment = Alignment.TopCenter
            ) {
                FeatureGuideStrip(
                    featureKey = "vocabulary",
                    modifier = Modifier.widthIn(max = containerMaxWidth)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth)) {
                    when (selectedTab) {
                        0 -> {
                            val dailyDoneCount = effectiveDailyWords.count { it.id in effectiveDoneTodayIds }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().testTag("vocabulary_words_list"),
                                contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                            ) {
                                // 1. Daily 3 Non-Repeating Words (Level-Calibrated) + Spoken Assessment to Mark Done
                                item {
                                    DailyThreeWordsHeaderCard(
                                        userLevel = userLevel,
                                        doneCount = dailyDoneCount,
                                        totalCount = effectiveDailyWords.size
                                    )
                                }
                                items(effectiveDailyWords, key = { "daily_${it.id}" }) { seed ->
                                    val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                    val isDone = seed.id in effectiveDoneTodayIds
                                    val bestScore = localWordEvals[seed.id]?.score ?: dailyWordScores[seed.id]
                                    val targetKey = "word_${seed.id}"
                                    DailyWordAssessmentCard(
                                        seed = seed,
                                        saved = saved,
                                        isDone = isDone,
                                        evalResult = localWordEvals[seed.id],
                                        lastScore = bestScore,
                                        isListening = isCurrentlyListening && activeSpeechTarget?.key == targetKey,
                                        livePartialText = if (activeSpeechTarget?.key == targetKey) {
                                            activePartialText.ifBlank { helperPartial }
                                        } else "",
                                        rmsDb = helperRms,
                                        errorMessage = if (errorTargetKey == targetKey) speechCaptureError else null,
                                        onSpeakReference = { onSpeakWord("${seed.word}. ${seed.exampleSentence}") },
                                        onStartSpeakAssessment = {
                                            requestSpeak(VocabSpeechTarget.DailyOrLibraryWord(seed))
                                        }
                                    )
                                }

                                // 2. Level-Matched Vocabulary Library
                                item {
                                    Spacer(Modifier.height(SpeakCraftSpacing.xs))
                                    SectionHeader(
                                        "Level Vocabulary Library",
                                        "Filtered for ${userLevel.label} level • Switch filters anytime"
                                    )
                                    Spacer(Modifier.height(SpeakCraftSpacing.sm))
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search vocabulary words or meanings...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth().testTag("vocab_search_input")
                                    )
                                    Spacer(Modifier.height(SpeakCraftSpacing.sm))
                                    val filterOptions = remember(userLevel) {
                                        listOf(
                                            userLevel.label,
                                            "All",
                                            "Beginner",
                                            "Elementary",
                                            "Intermediate",
                                            "Upper Intermediate",
                                            "Advanced"
                                        ).distinct()
                                    }
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                                    ) {
                                        filterOptions.forEach { lvl ->
                                            val labelText = if (lvl == userLevel.label) "$lvl (Your Level)" else lvl
                                            FilterChip(
                                                selected = levelFilter == lvl,
                                                onClick = { levelFilter = lvl },
                                                label = { Text(labelText) },
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                    .testTag("vocab_level_chip_${lvl.lowercase().replace(" ", "_")}")
                                            )
                                        }
                                    }
                                }
                                if (filteredBank.isEmpty()) {
                                    item {
                                        EmptyStateCard(
                                            title = "No matching words",
                                            message = "Clear your search or choose All levels to explore the full bank.",
                                            ctaLabel = "Reset Filters",
                                            onCta = { searchQuery = ""; levelFilter = userLevel.label }
                                        )
                                    }
                                } else if (tier.isExpandedTablet) {
                                    val rows = filteredBank.chunked(2)
                                    items(rows, key = { row -> row.joinToString("_") { "lib_${it.id}" } }) { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            pair.forEach { seed ->
                                                val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                                val isLearned = seed.id in effectiveLearnedIds || saved?.usageAssessed == true
                                                val targetKey = "word_${seed.id}"
                                                VocabSeedCard(
                                                    seed = seed,
                                                    saved = saved,
                                                    isAssessedDone = isLearned,
                                                    isListening = isCurrentlyListening && activeSpeechTarget?.key == targetKey,
                                                    evalResult = localWordEvals[seed.id],
                                                    onSpeakWord = onSpeakWord,
                                                    onSpeakAssessment = {
                                                        requestSpeak(VocabSpeechTarget.DailyOrLibraryWord(seed))
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(filteredBank, key = { "lib_${it.id}" }) { seed ->
                                        val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                        val isLearned = seed.id in effectiveLearnedIds || saved?.usageAssessed == true
                                        val targetKey = "word_${seed.id}"
                                        VocabSeedCard(
                                            seed = seed,
                                            saved = saved,
                                            isAssessedDone = isLearned,
                                            isListening = isCurrentlyListening && activeSpeechTarget?.key == targetKey,
                                            evalResult = localWordEvals[seed.id],
                                            onSpeakWord = onSpeakWord,
                                            onSpeakAssessment = {
                                                requestSpeak(VocabSpeechTarget.DailyOrLibraryWord(seed))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            var cardIndex by remember { mutableIntStateOf(0) }
                            var flipped by remember { mutableStateOf(false) }
                            val dueCards = remember(entries, todayEpochDay, userLevel) {
                                val levelFirst = bank.sortedByDescending { it.level.equals(userLevel.label, ignoreCase = true) }
                                levelFirst.filter { seed ->
                                    val existing = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                    existing == null || existing.nextReviewEpochDay <= todayEpochDay
                                }.ifEmpty { levelFirst }
                            }
                            val safePosition = cardIndex % dueCards.size
                            val currentCard = dueCards[safePosition]
                            val existing = entries.firstOrNull { it.word.equals(currentCard.word, ignoreCase = true) }

                            Column(
                                modifier = Modifier.fillMaxSize().padding(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
                            ) {
                                SectionHeader(
                                    "Spaced Repetition Deck (${userLevel.label})",
                                    "Card ${safePosition + 1} of ${dueCards.size} due today • Tap card to reveal"
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(270.dp)
                                        .clickable { flipped = !flipped }
                                        .testTag("vocab_flashcard"),
                                    shape = MaterialTheme.shapes.large,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                                ) {
                                    AnimatedContent(targetState = Pair(currentCard.id, flipped), label = "flashcard_reveal") { (_, isFlipped) ->
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(22.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(currentCard.word, style = MaterialTheme.typography.displayLarge)
                                            Spacer(Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = ext.infoContainer
                                            ) {
                                                Text(
                                                    "${currentCard.phonetic} • ${currentCard.level}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ext.info,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(14.dp))
                                            if (isFlipped) {
                                                Text(currentCard.meaning, style = MaterialTheme.typography.bodyLarge)
                                                Spacer(Modifier.height(8.dp))
                                                Text("\"${currentCard.exampleSentence}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                Text("Tap to reveal meaning & example", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onReviewCard(currentCard, existing, false)
                                            flipped = false
                                            cardIndex = (cardIndex + 1) % dueCards.size
                                        },
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    ) { Text("Review Again (1d)") }
                                    Button(
                                        onClick = {
                                            onReviewCard(currentCard, existing, true)
                                            flipped = false
                                            cardIndex = (cardIndex + 1) % dueCards.size
                                        },
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    ) { Text("Mastered Next") }
                                }
                            }
                        }

                        2 -> {
                            var targetIdx by remember(userLevel) { mutableIntStateOf(0) }
                            val target = levelQuizPool[targetIdx % levelQuizPool.size]
                            val existing = entries.firstOrNull { it.word.equals(target.word, ignoreCase = true) }

                            // Auto-reset weekly mode cleanly if user switched eligibility
                            LaunchedEffect(weeklyEligibility.cycleNumber) {
                                activeWeeklyQuizMode = false
                                weeklyAssessmentCompleted = false
                                weeklyAssessmentRecord = null
                                weeklyAnswersByQId = emptyMap()
                                weeklyQuestionIdx = 0
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize().testTag("ai_quiz_list"),
                                contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                            ) {
                                item {
                                    SectionHeader(
                                        "Spoken AI Vocabulary Quiz",
                                        "Speak-only assessment calibrated for ${userLevel.label} • Plus 7-day cumulative review"
                                    )
                                }

                                // After-7-Days Cumulative Assessment Card (Locked Countdown OR Unlocked Review)
                                item {
                                    WeeklySevenDayAssessmentBanner(
                                        eligibility = weeklyEligibility,
                                        learnedSeeds = effectiveLearnedForAssessment,
                                        isInWeeklyMode = activeWeeklyQuizMode,
                                        onStartWeeklyAssessment = {
                                            val questions = VocabularyMasteryEngine.buildWeeklyAssessmentQuestions(
                                                learnedSeeds = effectiveLearnedForAssessment,
                                                seed = todayEpochDay
                                            )
                                            weeklyQuestions = questions
                                            weeklyQuestionIdx = 0
                                            weeklyAnswersByQId = emptyMap()
                                            weeklyAssessmentCompleted = false
                                            weeklyAssessmentRecord = null
                                            activeWeeklyQuizMode = questions.isNotEmpty()
                                        },
                                        onExitWeeklyAssessment = {
                                            ttsHelper?.stopDictation()
                                            isCaptureActive = false
                                            activeSpeechTarget = null
                                            activeWeeklyQuizMode = false
                                        }
                                    )
                                }

                                if (activeWeeklyQuizMode && weeklyQuestions.isNotEmpty()) {
                                    if (weeklyAssessmentCompleted && weeklyAssessmentRecord != null) {
                                        item {
                                            WeeklyAssessmentSummaryCard(
                                                record = weeklyAssessmentRecord!!,
                                                answers = weeklyAnswersByQId.values.toList(),
                                                onDone = {
                                                    activeWeeklyQuizMode = false
                                                    weeklyAssessmentCompleted = false
                                                }
                                            )
                                        }
                                    } else {
                                        val currentQ = weeklyQuestions[weeklyQuestionIdx.coerceIn(0, weeklyQuestions.lastIndex)]
                                        val currentAns = weeklyAnswersByQId[currentQ.id]
                                        val targetKey = "weekly_${currentQ.id}"
                                        item {
                                            WeeklySpokenQuestionCard(
                                                questionIndex = weeklyQuestionIdx,
                                                totalQuestions = weeklyQuestions.size,
                                                question = currentQ,
                                                answerResult = currentAns,
                                                isListening = isCurrentlyListening && activeSpeechTarget?.key == targetKey,
                                                livePartialText = if (activeSpeechTarget?.key == targetKey) {
                                                    activePartialText.ifBlank { helperPartial }
                                                } else "",
                                                rmsDb = helperRms,
                                                errorMessage = if (errorTargetKey == targetKey) speechCaptureError else null,
                                                onPlayClue = { onSpeakWord(currentQ.audioPromptText) },
                                                onSpeakAnswer = {
                                                    requestSpeak(VocabSpeechTarget.WeeklyQuestion(currentQ))
                                                },
                                                onNextOrFinish = {
                                                    if (weeklyQuestionIdx < weeklyQuestions.lastIndex) {
                                                        weeklyQuestionIdx++
                                                    } else {
                                                        val answers = weeklyQuestions.mapNotNull { weeklyAnswersByQId[it.id] }
                                                        val assessedIds = weeklyQuestions.map { it.seed.id }.distinct()
                                                        val perWordPassed = assessedIds.associateWith { wId ->
                                                            answers.any { it.wordId == wId && it.passed }
                                                        }
                                                        val passedIds = perWordPassed.filterValues { it }.keys.toList()
                                                        val scorePct = if (assessedIds.isEmpty()) 0 else {
                                                            ((passedIds.size.toFloat() / assessedIds.size) * 100f).roundToInt().coerceIn(0, 100)
                                                        }
                                                        val record = WeeklyVocabularyAssessmentRecord(
                                                            id = "weekly_c${weeklyEligibility.cycleNumber}_${System.currentTimeMillis()}",
                                                            completedEpochDay = todayEpochDay,
                                                            cycleNumber = weeklyEligibility.cycleNumber,
                                                            assessedWordIds = assessedIds,
                                                            passedWordIds = passedIds,
                                                            scorePercent = scorePct
                                                        )
                                                        weeklyAssessmentRecord = record
                                                        weeklyAssessmentCompleted = true
                                                        onCompleteWeeklyAssessment(record, perWordPassed)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Daily Spoken AI Quiz (SPEAK option, NOT text input!)
                                    val quizTargetKey = "quiz_${target.id}"
                                    val isListeningQuiz = isCurrentlyListening && activeSpeechTarget?.key == quizTargetKey

                                    item {
                                        Card(
                                            shape = MaterialTheme.shapes.large,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                            modifier = Modifier.testTag("vocab_quiz_speak_panel")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(SpeakCraftSpacing.lg),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Target Word: ${target.word}", style = MaterialTheme.typography.titleLarge)
                                                        Text(
                                                            "${target.phonetic} • ${target.level} Level",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(onClick = { onSpeakWord("${target.word}. ${target.meaning}") }) {
                                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to ${target.word}", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                ttsHelper?.stopDictation()
                                                                isCaptureActive = false
                                                                activeSpeechTarget = null
                                                                targetIdx = (targetIdx + 1) % levelQuizPool.size
                                                                spokenQuizSentence = ""
                                                                speechCaptureError = null
                                                                onClearEval()
                                                            },
                                                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                        ) { Text("Next Word") }
                                                    }
                                                }

                                                Text(target.meaning, style = MaterialTheme.typography.bodyMedium)

                                                // Voice-Only Prompt Card (Replaces OutlinedTextField)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                            Text(
                                                                "Speak a realistic sentence using \"${target.word}\"",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                        Text(
                                                            "Voice-only AI quiz: tap Speak below and say a natural workplace or daily sentence aloud.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )

                                                        if (isListeningQuiz) {
                                                            ListeningLiveMeterCard(
                                                                prompt = "Listening... Say a full sentence containing \"${target.word}\"",
                                                                partialText = activePartialText.ifBlank { helperPartial },
                                                                rmsDb = helperRms
                                                            )
                                                        }

                                                        if (errorTargetKey == quizTargetKey && !speechCaptureError.isNullOrBlank()) {
                                                            Text(
                                                                speechCaptureError.orEmpty(),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }

                                                        Button(
                                                            onClick = {
                                                                requestSpeak(VocabSpeechTarget.AiQuizSentence(target))
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isListeningQuiz) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                                .testTag("vocab_quiz_speak_button")
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isListeningQuiz) Icons.Default.Stop else Icons.Default.Mic,
                                                                contentDescription = null
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(
                                                                if (isListeningQuiz) "Stop Listening"
                                                                else if (spokenQuizSentence.isBlank()) "Speak Your Sentence"
                                                                else "Speak a New Sentence"
                                                            )
                                                        }
                                                    }
                                                }

                                                if (spokenQuizSentence.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("vocab_quiz_spoken_transcript")
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                "What You Spoke:",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                "\"$spokenQuizSentence\"",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }

                                                    OutlinedButton(
                                                        onClick = { onEvaluateUsage(target, existing, spokenQuizSentence) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                            .testTag("vocab_quiz_submit")
                                                    ) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Re-Evaluate Spoken Usage")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        when (vocabEvalState) {
                                            is UiState.Loading -> CircularProgressIndicator()
                                            is UiState.Error -> ErrorRecoveryCard(
                                                message = vocabEvalState.message,
                                                onRetry = {
                                                    if (spokenQuizSentence.isNotBlank()) {
                                                        onEvaluateUsage(target, existing, spokenQuizSentence)
                                                    } else {
                                                        requestSpeak(VocabSpeechTarget.AiQuizSentence(target))
                                                    }
                                                }
                                            )
                                            is UiState.Success -> {
                                                val r = vocabEvalState.data
                                                Card(
                                                    shape = MaterialTheme.shapes.large,
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (r.usedCorrectly) ext.successContainer else MaterialTheme.colorScheme.errorContainer
                                                    )
                                                ) {
                                                    Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = if (r.usedCorrectly) "Natural Spoken Usage (${r.naturalnessScore}/100)" else "Needs Adjustment (${r.naturalnessScore}/100)",
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        Text(r.feedback, style = MaterialTheme.typography.bodyMedium)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                "Polished Example: \"${r.improvedExample}\"",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            IconButton(onClick = { onSpeakWord(r.improvedExample) }) {
                                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to polished example")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            null -> Unit
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            var customWord by remember { mutableStateOf("") }
                            var customMeaning by remember { mutableStateOf("") }
                            var customExample by remember { mutableStateOf("") }
                            var customNote by remember { mutableStateOf("") }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                            ) {
                                item {
                                    SectionHeader("Personal Vocabulary Notebook", "${entries.size} saved entries")
                                    Spacer(Modifier.height(SpeakCraftSpacing.sm))
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = customWord, onValueChange = { customWord = it }, label = { Text("Word or Phrase") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = customMeaning, onValueChange = { customMeaning = it }, label = { Text("Meaning") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = customExample, onValueChange = { customExample = it }, label = { Text("Example Sentence") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = customNote, onValueChange = { customNote = it }, label = { Text("Personal Usage Note") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                                            Button(
                                                onClick = {
                                                    onSaveCustomWord(customWord, customMeaning, userLevel.label, customExample, customNote)
                                                    customWord = ""; customMeaning = ""; customExample = ""; customNote = ""
                                                },
                                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Add to Personal Notebook")
                                            }
                                        }
                                    }
                                }
                                if (tier.isExpandedTablet) {
                                    items(entries.chunked(2), key = { row -> row.joinToString("_") { it.id.ifBlank { it.word } } }) { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            pair.forEach { item ->
                                                NotebookEntryCard(item = item, modifier = Modifier.weight(1f))
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(entries, key = { it.id.ifBlank { it.word } }) { item ->
                                        NotebookEntryCard(item = item, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyThreeWordsHeaderCard(
    userLevel: LearningLevel,
    doneCount: Int,
    totalCount: Int
) {
    val ext = MaterialTheme.speakCraftColors
    val allDone = totalCount > 0 && doneCount >= totalCount
    val progress = if (totalCount == 0) 0f else (doneCount.toFloat() / totalCount).coerceIn(0f, 1f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) ext.successContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (allDone) EmeraldSuccess.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = Modifier.testTag("daily_three_words_header")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's $totalCount Daily Words",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            userLevel.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (allDone) ext.successContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            "$doneCount/$totalCount Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (allDone) ext.success else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                if (allDone) {
                    "All $totalCount non-repeating words assessed and marked Done today! 3 new ${userLevel.label} words unlock tomorrow."
                } else {
                    "3 fresh non-repeating ${userLevel.label} words for today. Speak each word clearly in the assessment to mark it Done."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = EmeraldSuccess,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun DailyWordAssessmentCard(
    seed: VocabSeed,
    saved: VocabularyEntry?,
    isDone: Boolean,
    evalResult: LocalVocabStageEvaluation?,
    lastScore: Int?,
    isListening: Boolean,
    livePartialText: String,
    rmsDb: Float,
    errorMessage: String?,
    onSpeakReference: () -> Unit,
    onStartSpeakAssessment: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = Modifier.fillMaxWidth().testTag("daily_vocab_card_${seed.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) ext.successContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isDone) EmeraldSuccess.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(seed.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDone) ext.successContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isDone) "Done" else "Pending assessment",
                                    tint = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isDone) {
                                        if (lastScore != null && lastScore > 0) "Done • $lastScore%" else "Done"
                                    } else "Speak to Complete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDone) ext.success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = ext.infoContainer) {
                            Text(
                                "${seed.phonetic} • ${seed.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.info,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (saved != null && saved.masteryLevel > 0L) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    "Mastery ${saved.masteryLevel}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onSpeakReference,
                    modifier = Modifier.defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to ${seed.word}", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Text(seed.meaning, style = MaterialTheme.typography.bodyMedium)
            if (seed.collocations.isNotEmpty()) {
                Text("Collocations: ${seed.collocations.joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("Example: \"${seed.exampleSentence}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (isListening) {
                ListeningLiveMeterCard(
                    prompt = "Say \"${seed.word}\" clearly aloud...",
                    partialText = livePartialText,
                    rmsDb = rmsDb
                )
            }

            if (evalResult != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (evalResult.passed) ext.successContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().testTag("daily_vocab_eval_${seed.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (evalResult.passed) {
                                "Spoken Assessment Passed (${evalResult.score}/100) — Marked Done!"
                            } else {
                                "Not Marked Done Yet (${evalResult.score}/100)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (evalResult.passed) ext.success else MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (evalResult.recognizedText.isNotBlank()) {
                            Text(
                                "Heard: \"${evalResult.recognizedText}\"",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(evalResult.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (isDone && !isListening) {
                OutlinedButton(
                    onClick = onStartSpeakAssessment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("speak_assess_word_${seed.id}")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Speak Again to Practice")
                }
            } else {
                Button(
                    onClick = onStartSpeakAssessment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("speak_assess_word_${seed.id}")
                ) {
                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isListening) "Stop Listening" else "Speak Word to Mark Done")
                }
            }
        }
    }
}

@Composable
private fun ListeningLiveMeterCard(
    prompt: String,
    partialText: String,
    rmsDb: Float
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(prompt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            val normalizedRms = ((rmsDb + 2f) / 12f).coerceIn(0.1f, 1f)
            LinearProgressIndicator(
                progress = { normalizedRms },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
            )
            if (partialText.isNotBlank()) {
                Text("Heard so far: \"$partialText\"", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklySevenDayAssessmentBanner(
    eligibility: WeeklyAssessmentEligibility,
    learnedSeeds: List<VocabSeed>,
    isInWeeklyMode: Boolean,
    onStartWeeklyAssessment: () -> Unit,
    onExitWeeklyAssessment: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    if (eligibility.isDueNow && learnedSeeds.isNotEmpty()) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = ext.successContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("weekly_assessment_unlocked_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EmeraldSuccess)
                        Text(
                            "7-Day Spoken Assessment Ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = ext.successContainer) {
                        Text(
                            "Day 7+ Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    "7 days complete! Test your retention of the ${learnedSeeds.size} word${if (learnedSeeds.size == 1) "" else "s"} you learnt by speaking aloud.",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    learnedSeeds.take(6).forEach { w ->
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                            Text(
                                w.word,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (learnedSeeds.size > 6) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                            Text(
                                "+${learnedSeeds.size - 6} more",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (isInWeeklyMode) {
                    OutlinedButton(
                        onClick = onExitWeeklyAssessment,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    ) {
                        Text("Back to Daily Spoken Quiz")
                    }
                } else {
                    Button(
                        onClick = onStartWeeklyAssessment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("start_7day_assessment_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Start 7-Day Assessment (${minOf(learnedSeeds.size, VocabularyMasteryEngine.MAX_WEEKLY_QUESTIONS)} Words)")
                    }
                }
            }
        }
    } else {
        val currentDay = maxOf(1, maxOf(eligibility.daysElapsed, eligibility.distinctActiveDays).coerceAtMost(7))
        val progress = (maxOf(eligibility.daysElapsed, eligibility.distinctActiveDays).coerceIn(0, 7) / 7f)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("weekly_assessment_countdown_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("After 7 Days: Learned Words Assessment", style = MaterialTheme.typography.titleSmall)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            if (eligibility.isUnlocked) "Speak 1+ Word to Unlock" else "${eligibility.daysRemaining}d left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    if (eligibility.isUnlocked) {
                        "7 days reached! Pass at least 1 spoken word assessment in the Words tab to launch your 7-day quiz."
                    } else {
                        "Day $currentDay of 7 • ${learnedSeeds.size} word${if (learnedSeeds.size == 1) "" else "s"} learnt so far. After 7 days, AI Quiz unlocks a speak-only test of all words you learnt."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                eligibility.latestAssessment?.let { last ->
                    Text(
                        "Last 7-day review: ${last.scorePercent}% (${last.passedWordIds.size}/${last.assessedWordIds.size} mastered)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklySpokenQuestionCard(
    questionIndex: Int,
    totalQuestions: Int,
    question: WeeklySpokenQuestion,
    answerResult: WeeklySpokenAnswerResult?,
    isListening: Boolean,
    livePartialText: String,
    rmsDb: Float,
    errorMessage: String?,
    onPlayClue: () -> Unit,
    onSpeakAnswer: () -> Unit,
    onNextOrFinish: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("weekly_question_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        question.mode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    "Question ${questionIndex + 1} of $totalQuestions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { (questionIndex + 1).toFloat() / totalQuestions.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )

            Text(question.prompt, style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPlayClue) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear clue aloud", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (isListening) {
                ListeningLiveMeterCard(
                    prompt = "Listening... Speak your answer aloud",
                    partialText = livePartialText,
                    rmsDb = rmsDb
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSpeakAnswer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    .testTag("weekly_quiz_speak_button")
            ) {
                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isListening -> "Stop Listening"
                        answerResult == null -> "Speak Answer"
                        else -> "Speak Again"
                    }
                )
            }

            if (answerResult != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (answerResult.passed) ext.successContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (answerResult.passed) "Passed (${answerResult.score}/100)" else "Needs Review (${answerResult.score}/100)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (answerResult.transcript.isNotBlank()) {
                            Text("Spoken: \"${answerResult.transcript}\"", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(answerResult.feedback, style = MaterialTheme.typography.bodySmall)
                        if (answerResult.improvedExample.isNotBlank()) {
                            Text("Model Example: \"${answerResult.improvedExample}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Button(
                    onClick = onNextOrFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("weekly_quiz_next_button")
                ) {
                    Text(if (questionIndex < totalQuestions - 1) "Next Question" else "Finish 7-Day Assessment")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyAssessmentSummaryCard(
    record: WeeklyVocabularyAssessmentRecord,
    answers: List<WeeklySpokenAnswerResult>,
    onDone: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (record.scorePercent >= 70) ext.successContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("weekly_assessment_summary_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EmeraldSuccess)
                Text("7-Day Spoken Assessment Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Score: ${record.scorePercent}% • ${record.passedWordIds.size} of ${record.assessedWordIds.size} words spoken accurately",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                answers.forEach { ans ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (ans.passed) ext.successContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "${ans.word} ${if (ans.passed) "✓" else "↻"}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                "Missed words have been scheduled automatically into your Spaced Repetition Flashcards deck.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun VocabSeedCard(
    seed: VocabSeed,
    saved: VocabularyEntry?,
    isAssessedDone: Boolean = false,
    isListening: Boolean = false,
    evalResult: LocalVocabStageEvaluation? = null,
    onSpeakWord: (String) -> Unit,
    onSpeakAssessment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAssessedDone) EmeraldSuccess.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${seed.word}  •  ${seed.level}", style = MaterialTheme.typography.titleLarge)
                        if (isAssessedDone) {
                            Surface(shape = RoundedCornerShape(8.dp), color = ext.successContainer) {
                                Text(
                                    "Done ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.success,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ext.infoContainer
                    ) {
                        Text(
                            seed.phonetic,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.info,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { onSpeakWord("${seed.word}. ${seed.exampleSentence}") },
                    modifier = Modifier.defaultMinSize(
                        minWidth = SpeakCraftSpacing.minTouchTarget,
                        minHeight = SpeakCraftSpacing.minTouchTarget
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce ${seed.word}", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(seed.meaning, style = MaterialTheme.typography.bodyLarge)
            Text("Synonyms: ${seed.synonyms.joinToString(", ")} | Antonyms: ${seed.antonyms.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Text("Collocations: ${seed.collocations.joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("Example: \"${seed.exampleSentence}\"", style = MaterialTheme.typography.bodyMedium)
            Text("Conversation: ${seed.conversationExample}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (evalResult != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (evalResult.passed) ext.successContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (evalResult.passed) "Passed (${evalResult.score}/100): \"${evalResult.recognizedText}\"" else "${evalResult.score}/100: ${evalResult.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (saved != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ext.successContainer
                    ) {
                        Text(
                            "Mastery: ${saved.masteryLevel}/5 • Reps: ${saved.repetitions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                OutlinedButton(
                    onClick = onSpeakAssessment,
                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isListening) "Stop" else if (isAssessedDone) "Speak Again" else "Speak & Mark Done")
                }
            }
        }
    }
}

@Composable
private fun NotebookEntryCard(
    item: VocabularyEntry,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.word, style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ext.successContainer
                ) {
                    Text(
                        "Mastery ${item.masteryLevel}/5",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.success,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(item.meaning, style = MaterialTheme.typography.bodyMedium)
            if (item.exampleSentence.isNotBlank()) Text("\"${item.exampleSentence}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
