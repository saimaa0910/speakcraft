package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.VocabEvaluationResult
import com.example.data.model.CurriculumCatalog
import com.example.data.model.UiState
import com.example.data.model.VocabSeed
import com.example.data.model.VocabularyEntry
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

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
    onClearEval: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    var selectedTab by remember { mutableIntStateOf(0) }
    var levelFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val bank = CurriculumCatalog.vocabularyBank
    val filteredBank = remember(levelFilter, searchQuery) {
        val q = searchQuery.trim()
        bank.filter { seed ->
            val levelMatch = levelFilter == "All" || seed.level == levelFilter
            val queryMatch = q.isBlank() ||
                seed.word.contains(q, ignoreCase = true) ||
                seed.meaning.contains(q, ignoreCase = true)
            levelMatch && queryMatch
        }
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
                        onClick = { selectedTab = idx },
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
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                            ) {
                                item {
                                    SectionHeader("Vocabulary Builder", "Meaning, IPA, collocations & real conversation usage")
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
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                                    ) {
                                        listOf("All", "Beginner", "Intermediate", "Advanced").forEach { lvl ->
                                            FilterChip(
                                                selected = levelFilter == lvl,
                                                onClick = { levelFilter = lvl },
                                                label = { Text(lvl) },
                                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
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
                                            onCta = { searchQuery = ""; levelFilter = "All" }
                                        )
                                    }
                                } else if (tier.isExpandedTablet) {
                                    val rows = filteredBank.chunked(2)
                                    items(rows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            pair.forEach { seed ->
                                                val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                                VocabSeedCard(
                                                    seed = seed,
                                                    saved = saved,
                                                    onSpeakWord = onSpeakWord,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(filteredBank, key = { it.id }) { seed ->
                                        val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                        VocabSeedCard(
                                            seed = seed,
                                            saved = saved,
                                            onSpeakWord = onSpeakWord,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            var cardIndex by remember { mutableIntStateOf(0) }
                            var flipped by remember { mutableStateOf(false) }
                            val dueCards = remember(entries, todayEpochDay) {
                                bank.filter { seed ->
                                    val existing = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                                    existing == null || existing.nextReviewEpochDay <= todayEpochDay
                                }.ifEmpty { bank }
                            }
                            val safePosition = cardIndex % dueCards.size
                            val currentCard = dueCards[safePosition]
                            val existing = entries.firstOrNull { it.word.equals(currentCard.word, ignoreCase = true) }

                            Column(
                                modifier = Modifier.fillMaxSize().padding(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg)
                            ) {
                                SectionHeader(
                                    "Spaced Repetition Deck",
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
                                                    currentCard.phonetic,
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
                            var targetIdx by remember { mutableIntStateOf(0) }
                            val target = bank[targetIdx % bank.size]
                            val existing = entries.firstOrNull { it.word.equals(target.word, ignoreCase = true) }
                            var sentenceInput by remember { mutableStateOf("") }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                            ) {
                                item {
                                    SectionHeader("Contextual Usage Assessment", "Use the word naturally in a real conversation—not rote memorization")
                                }
                                item {
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                                    ) {
                                        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Target Word: ${target.word}", style = MaterialTheme.typography.titleLarge)
                                                OutlinedButton(
                                                    onClick = {
                                                        targetIdx = (targetIdx + 1) % bank.size
                                                        sentenceInput = ""
                                                        onClearEval()
                                                    },
                                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                ) { Text("Next Word") }
                                            }
                                            Text(target.meaning, style = MaterialTheme.typography.bodyMedium)
                                            OutlinedTextField(
                                                value = sentenceInput,
                                                onValueChange = { sentenceInput = it },
                                                label = { Text("Write a realistic workplace or daily sentence using '${target.word}'") },
                                                minLines = 2,
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.fillMaxWidth().testTag("vocab_quiz_input")
                                            )
                                            Button(
                                                onClick = { onEvaluateUsage(target, existing, sentenceInput) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                    .testTag("vocab_quiz_submit")
                                            ) { Text("Evaluate Conversational Usage") }
                                        }
                                    }
                                }
                                item {
                                    when (vocabEvalState) {
                                        is UiState.Loading -> CircularProgressIndicator()
                                        is UiState.Error -> Text(vocabEvalState.message, color = MaterialTheme.colorScheme.error)
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
                                                        text = if (r.usedCorrectly) "Natural Usage (${r.naturalnessScore}/100)" else "Needs Adjustment (${r.naturalnessScore}/100)",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(r.feedback, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Polished Example: \"${r.improvedExample}\"", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                        null -> Unit
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
                                                    onSaveCustomWord(customWord, customMeaning, "Intermediate", customExample, customNote)
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
private fun VocabSeedCard(
    seed: VocabSeed,
    saved: VocabularyEntry?,
    onSpeakWord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${seed.word}  •  ${seed.level}", style = MaterialTheme.typography.titleLarge)
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
