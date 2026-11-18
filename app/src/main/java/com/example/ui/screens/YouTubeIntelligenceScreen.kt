package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurriculumCatalog
import com.example.data.model.ExtractedTechnique
import com.example.data.model.YouTubeSource
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LessonAttributionBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AmberSpotlight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.InteractiveCoachViewModel

@Composable
fun YouTubeIntelligenceScreen(
    coachVm: InteractiveCoachViewModel,
    sources: List<YouTubeSource>,
    userTechniques: List<ExtractedTechnique>,
    sharedTechniques: List<ExtractedTechnique>,
    youtubeApiKey: String
) {
    val uriHandler = LocalUriHandler.current
    val auditState by coachVm.ytAuditState.collectAsStateWithLifecycle()

    val mergedTechniques = remember(userTechniques, sharedTechniques) {
        (userTechniques + sharedTechniques).distinctBy { it.id.ifBlank { it.title } }
    }

    var urlInput by remember { mutableStateOf("https://www.youtube.com/@askvinh") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDifficulty by remember { mutableStateOf("All") }

    val filteredTechniques = remember(mergedTechniques, searchQuery, selectedCategory, selectedDifficulty) {
        mergedTechniques.filter { tech ->
            val matchesCat = selectedCategory == "All" || tech.category.equals(selectedCategory, ignoreCase = true)
            val matchesDiff = selectedDifficulty == "All" || tech.difficulty.equals(selectedDifficulty, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                tech.title.contains(searchQuery, ignoreCase = true) ||
                tech.creatorAdvice.contains(searchQuery, ignoreCase = true) ||
                tech.practicalExercise.contains(searchQuery, ignoreCase = true) ||
                tech.sourceVideoTitle.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesDiff && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "YouTube Channel Intelligence",
                    subtitle = "Inspect @askvinh or any channel/playlist/video • ${sources.size} audited videos, ${mergedTechniques.size} techniques"
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("YouTube Channel, Playlist, or Video URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("youtube_url_input")
                        )
                        Button(
                            onClick = { coachVm.auditYouTubeUrl(urlInput, youtubeApiKey) },
                            enabled = !auditState.isAuditing,
                            modifier = Modifier.fillMaxWidth().testTag("youtube_audit_button")
                        ) {
                            if (auditState.isAuditing) {
                                CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (auditState.isAuditing) "Analyzing Feed & Captions..." else "Inspect & Extract Lessons")
                        }
                        if (auditState.statusSummary.isNotBlank()) {
                            Text(auditState.statusSummary, style = MaterialTheme.typography.bodySmall, color = AcousticTeal)
                        }
                        if (auditState.error != null) {
                            Text(auditState.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (sources.isNotEmpty()) {
                item {
                    SectionHeader("Video Extraction Audit", "Published dates, durations & live caption status")
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sources, key = { it.id.ifBlank { it.videoId } }) { src ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.width(250.dp).clickable { uriHandler.openUri(src.videoUrl) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(src.title, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                                    val metaLine = buildString {
                                        append("${src.extractionStatus} • ${src.category}")
                                        if (src.durationSeconds > 0L) append(" • ${src.durationSeconds / 60}m${src.durationSeconds % 60}s")
                                    }
                                    Text(
                                        text = metaLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (src.extractionStatus) {
                                            "EXTRACTED" -> EmeraldSuccess
                                            "NO_CAPTIONS" -> AmberSpotlight
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                    if (src.publishedDate.isNotBlank() || src.playlistTitle.isNotBlank()) {
                                        Text(
                                            text = listOfNotNull(
                                                src.publishedDate.takeIf { it.isNotBlank() },
                                                src.playlistTitle.takeIf { it.isNotBlank() }
                                            ).joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    if (src.statusDetail.isNotBlank()) {
                                        Text(src.statusDetail, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Searchable Knowledge Base", "${filteredTechniques.size} techniques • Creator Advice vs AI Supplement")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search frameworks, drills, or @askvinh topics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("youtube_search_input")
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("All", "Beginner", "Intermediate", "Advanced")) { diff ->
                        FilterChip(selected = selectedDifficulty == diff, onClick = { selectedDifficulty = diff }, label = { Text(diff) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("All") + CurriculumCatalog.allCategories) { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                    }
                }
            }

            if (filteredTechniques.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No matching techniques",
                        message = "Clear your category filter or search term to inspect all @askvinh techniques."
                    )
                }
            } else {
                items(filteredTechniques, key = { it.id.ifBlank { it.title } }) { tech ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                LessonAttributionBadge(
                                    isCreatorDerived = tech.isCreatorDerived,
                                    sourceVerified = tech.timestampLabel != "Pending"
                                )
                                Text("${tech.suggestedDurationMinutes}m • ${tech.timestampLabel}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(tech.title, style = MaterialTheme.typography.titleLarge)
                            Text("${tech.category} • ${tech.difficulty}", style = MaterialTheme.typography.labelSmall, color = AcousticTeal)

                            if (tech.originalContext.isNotBlank()) {
                                Text("Context: ${tech.originalContext}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text("CREATOR ADVICE (Attributed):", style = MaterialTheme.typography.labelSmall, color = AmberSpotlight)
                            Text(tech.creatorAdvice, style = MaterialTheme.typography.bodyMedium)

                            if (tech.aiExplanation.isNotBlank()) {
                                Text("AI COACHING SUPPLEMENT:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(tech.aiExplanation, style = MaterialTheme.typography.bodySmall)
                            }

                            if (tech.instructions.isNotBlank()) {
                                Text("Instructions:\n${tech.instructions}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Practical Exercise: ${tech.practicalExercise}", style = MaterialTheme.typography.bodyMedium)
                            if (tech.expectedOutcome.isNotBlank()) {
                                Text("Expected Outcome: ${tech.expectedOutcome}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                            }

                            if (tech.commonMistake.isNotBlank()) {
                                Text("Mistake -> Fix: ${tech.commonMistake} → ${tech.correction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri(tech.sourceVideoUrl) }
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open source video", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Source: ${tech.sourceVideoTitle}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
