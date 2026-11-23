package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumCatalog
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.viewmodel.UpdateAndAdminViewModel

@Composable
fun AdminDashboardScreen(
    vm: UpdateAndAdminViewModel,
    isAdmin: Boolean,
    currentUserId: String
) {
    if (!isAdmin) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.widthIn(max = 520.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = "Admin restricted", tint = MaterialTheme.colorScheme.error)
                        Text("Admin Access Restricted", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "This console enforces Firestore RBAC. Administrative permissions require an active record at adminUsers/$currentUserId provisioned server-side.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    var lessonTitle by remember { mutableStateOf("") }
    var lessonObj by remember { mutableStateOf("") }
    var lessonSlug by remember { mutableStateOf(CurriculumCatalog.contentModules.first().slug) }
    var chalTitle by remember { mutableStateOf("") }
    var chalPrompt by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                FeatureGuideStrip(featureKey = "admin_console", modifier = Modifier.widthIn(max = containerMaxWidth))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("Shared Curriculum Catalog", "Publish curated lessons and speaking challenges")
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Publish Shared Lesson", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = lessonSlug,
                                    onValueChange = { lessonSlug = it },
                                    label = { Text("Module Slug (e.g. storytelling)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = lessonTitle,
                                    onValueChange = { lessonTitle = it },
                                    label = { Text("Lesson Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = lessonObj,
                                    onValueChange = { lessonObj = it },
                                    label = { Text("Objective & Explanation") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (lessonTitle.isNotBlank()) {
                                            vm.saveSharedLesson(
                                                Lesson(
                                                    id = "shared_${System.currentTimeMillis()}",
                                                    moduleSlug = lessonSlug.trim(),
                                                    title = lessonTitle.trim(),
                                                    objective = lessonObj.trim().ifBlank { "Communication practice" },
                                                    sourceVideoUrl = "",
                                                    timestampLabel = "Curated",
                                                    explanation = lessonObj.trim(),
                                                    instructions = listOf("Study the framework", "Record a 60s practice clip"),
                                                    practicePrompt = lessonObj.trim(),
                                                    status = "PUBLISHED"
                                                )
                                            )
                                            lessonTitle = ""
                                            lessonObj = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Publish Lesson") }
                            }
                        }
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Publish Shared Speaking Challenge", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = chalTitle,
                                    onValueChange = { chalTitle = it },
                                    label = { Text("Challenge Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = chalPrompt,
                                    onValueChange = { chalPrompt = it },
                                    label = { Text("Speaking Prompt") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (chalTitle.isNotBlank() && chalPrompt.isNotBlank()) {
                                            vm.saveSharedChallenge(
                                                LearningChallenge(
                                                    id = "shared_chal_${System.currentTimeMillis()}",
                                                    title = chalTitle.trim(),
                                                    challengeType = "DAILY",
                                                    durationSeconds = 60L,
                                                    prompt = chalPrompt.trim(),
                                                    frameworkHint = "Clarity & intentional pauses",
                                                    sourceReference = "Admin Curated",
                                                    active = true
                                                )
                                            )
                                            chalTitle = ""
                                            chalPrompt = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Publish Challenge") }
                            }
                        }
                    }
                }
            }
        }
    }
}
