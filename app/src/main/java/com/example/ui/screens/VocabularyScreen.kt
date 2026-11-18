package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
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
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.EmeraldSuccess

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
    var selectedTab by remember { mutableIntStateOf(0) }
    var levelFilter by remember { mutableStateOf("All") }
    val bank = CurriculumCatalog.vocabularyBank
    val filteredBank = remember(levelFilter) {
        if (levelFilter == "All") bank else bank.filter { it.level == levelFilter }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TabRow(selectedTabIndex = selectedTab) {
            listOf("Words", "Flashcards", "AI Quiz", "Notebook").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
                when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("Vocabulary Builder", "Meaning, IPA, collocations & real conversation usage")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Beginner", "Intermediate", "Advanced").forEach { lvl ->
                                FilterChip(selected = levelFilter == lvl, onClick = { levelFilter = lvl }, label = { Text(lvl) })
                            }
                        }
                    }
                    items(filteredBank, key = { it.id }) { seed ->
                        val saved = entries.firstOrNull { it.word.equals(seed.word, ignoreCase = true) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${seed.word}  •  ${seed.level}", style = MaterialTheme.typography.titleLarge)
                                        Text(seed.phonetic, style = MaterialTheme.typography.labelSmall, color = AcousticTeal)
                                    }
                                    IconButton(onClick = { onSpeakWord("${seed.word}. ${seed.exampleSentence}") }) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce ${seed.word}")
                                    }
                                }
                                Text(seed.meaning, style = MaterialTheme.typography.bodyLarge)
                                Text("Synonyms: ${seed.synonyms.joinToString(", ")} | Antonyms: ${seed.antonyms.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                Text("Collocations: ${seed.collocations.joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("Example: \"${seed.exampleSentence}\"", style = MaterialTheme.typography.bodyMedium)
                                Text("Conversation: ${seed.conversationExample}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (saved != null) {
                                    Text("Mastery: ${saved.masteryLevel}/5 • Reps: ${saved.repetitions}", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                }
                            }
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
                val currentCard = dueCards[cardIndex % dueCards.size]
                val existing = entries.firstOrNull { it.word.equals(currentCard.word, ignoreCase = true) }

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader("Spaced Repetition Deck", "${dueCards.size} cards due today • Tap card to reveal")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clickable { flipped = !flipped }
                            .testTag("vocab_flashcard"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(22.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(currentCard.word, style = MaterialTheme.typography.displayLarge)
                            Text(currentCard.phonetic, style = MaterialTheme.typography.labelSmall, color = AcousticTeal)
                            Spacer(Modifier.height(12.dp))
                            if (flipped) {
                                Text(currentCard.meaning, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(8.dp))
                                Text("\"${currentCard.exampleSentence}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Tap to reveal meaning & example", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.weight(1f)
                        ) { Text("Review Again (1d)") }
                        Button(
                            onClick = {
                                onReviewCard(currentCard, existing, true)
                                flipped = false
                                cardIndex = (cardIndex + 1) % dueCards.size
                            },
                            modifier = Modifier.weight(1f)
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("Contextual Usage Assessment", "Use the word naturally in a real conversation—not rote memorization")
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Target Word: ${target.word}", style = MaterialTheme.typography.titleLarge)
                                    OutlinedButton(onClick = {
                                        targetIdx = (targetIdx + 1) % bank.size
                                        sentenceInput = ""
                                        onClearEval()
                                    }) { Text("Next Word") }
                                }
                                Text(target.meaning, style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = sentenceInput,
                                    onValueChange = { sentenceInput = it },
                                    label = { Text("Write a realistic workplace or daily sentence using '${target.word}'") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth().testTag("vocab_quiz_input")
                                )
                                Button(
                                    onClick = { onEvaluateUsage(target, existing, sentenceInput) },
                                    modifier = Modifier.fillMaxWidth().testTag("vocab_quiz_submit")
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
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (r.usedCorrectly) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("Personal Vocabulary Notebook", "${entries.size} saved entries")
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = customWord, onValueChange = { customWord = it }, label = { Text("Word or Phrase") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = customMeaning, onValueChange = { customMeaning = it }, label = { Text("Meaning") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = customExample, onValueChange = { customExample = it }, label = { Text("Example Sentence") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = customNote, onValueChange = { customNote = it }, label = { Text("Personal Usage Note") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        onSaveCustomWord(customWord, customMeaning, "Intermediate", customExample, customNote)
                                        customWord = ""; customMeaning = ""; customExample = ""; customNote = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add to Personal Notebook")
                                }
                            }
                        }
                    }
                    items(entries, key = { it.id.ifBlank { it.word } }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.word, style = MaterialTheme.typography.titleMedium)
                                    Text("Mastery ${item.masteryLevel}/5", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                }
                                Text(item.meaning, style = MaterialTheme.typography.bodyMedium)
                                if (item.exampleSentence.isNotBlank()) Text("\"${item.exampleSentence}\"", style = MaterialTheme.typography.bodySmall)
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
