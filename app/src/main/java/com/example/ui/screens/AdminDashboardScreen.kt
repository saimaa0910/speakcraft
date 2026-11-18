package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApplicationRelease
import com.example.data.model.CurriculumCatalog
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    vm: UpdateAndAdminViewModel,
    isAdmin: Boolean,
    currentUserId: String,
    youtubeApiKey: String
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val syncBusy by vm.adminSyncBusy.collectAsStateWithLifecycle()
    val techniques by vm.moderationTechniques.collectAsStateWithLifecycle()
    val releases by vm.adminReleases.collectAsStateWithLifecycle()
    val updateEvents by vm.adminUpdateEvents.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.widthIn(max = 880.dp)) {
            listOf("YouTube Sync", "Moderation", "Releases", "Telemetry").forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            when (selectedTab) {
                0 -> {
                    var channelUrl by remember { mutableStateOf("https://www.youtube.com/@askvinh") }
                    var autoPublish by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader("YouTube Content Sync Console", "Ingest @askvinh feeds & queue techniques for moderation")
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = channelUrl,
                                        onValueChange = { channelUrl = it },
                                        label = { Text("Channel / Playlist / Video URL") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = autoPublish, onCheckedChange = { autoPublish = it })
                                        Text("Publish immediately (otherwise queue as PENDING_REVIEW)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = { vm.adminSyncYouTubeChannel(channelUrl, youtubeApiKey, autoPublish) },
                                        enabled = !syncBusy,
                                        modifier = Modifier.fillMaxWidth().testTag("admin_sync_youtube_button")
                                    ) {
                                        if (syncBusy) CircularProgressIndicator(strokeWidth = 2.dp)
                                        else Icon(Icons.Default.Sync, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (syncBusy) "Syncing Channel..." else "Sync to Shared Catalog")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    var lessonTitle by remember { mutableStateOf("") }
                    var lessonObj by remember { mutableStateOf("") }
                    var lessonSlug by remember { mutableStateOf(CurriculumCatalog.contentModules.first().slug) }
                    var chalTitle by remember { mutableStateOf("") }
                    var chalPrompt by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader("AI Extraction Moderation Queue", "${techniques.size} shared catalog entries")
                        }
                        if (techniques.isEmpty()) {
                            item {
                                EmptyStateCard("Queue is empty", "Sync YouTube videos in the YouTube Sync tab to queue extracted techniques for review.")
                            }
                        } else {
                            items(techniques, key = { it.id.ifBlank { it.title } }) { tech ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${tech.moderationStatus} • ${tech.category}", style = MaterialTheme.typography.labelSmall, color = AcousticTeal)
                                        Text(tech.title, style = MaterialTheme.typography.titleMedium)
                                        Text("Creator Advice: ${tech.creatorAdvice}", style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { vm.moderateTechnique(tech.id, "PUBLISHED") }) { Text("Publish") }
                                            OutlinedButton(onClick = { vm.moderateTechnique(tech.id, "APPROVED") }) { Text("Approve") }
                                            OutlinedButton(onClick = { vm.moderateTechnique(tech.id, "REJECTED") }) { Text("Reject") }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Publish Shared Lesson", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = lessonSlug, onValueChange = { lessonSlug = it }, label = { Text("Module Slug (e.g. storytelling)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = lessonTitle, onValueChange = { lessonTitle = it }, label = { Text("Lesson Title") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = lessonObj, onValueChange = { lessonObj = it }, label = { Text("Objective & Explanation") }, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            if (lessonTitle.isNotBlank()) {
                                                vm.saveSharedLesson(
                                                    Lesson(
                                                        id = "shared_${System.currentTimeMillis()}",
                                                        moduleSlug = lessonSlug.trim(),
                                                        title = lessonTitle.trim(),
                                                        objective = lessonObj.trim().ifBlank { "Communication practice" },
                                                        sourceVideoUrl = "https://www.youtube.com/@askvinh",
                                                        timestampLabel = "Curated",
                                                        explanation = lessonObj.trim(),
                                                        instructions = listOf("Study the framework", "Record a 60s practice clip"),
                                                        practicePrompt = lessonObj.trim(),
                                                        status = "PUBLISHED"
                                                    )
                                                )
                                                lessonTitle = ""; lessonObj = ""
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
                                    OutlinedTextField(value = chalTitle, onValueChange = { chalTitle = it }, label = { Text("Challenge Title") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = chalPrompt, onValueChange = { chalPrompt = it }, label = { Text("Speaking Prompt") }, modifier = Modifier.fillMaxWidth())
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
                                                        frameworkHint = "@askvinh clarity & pauses",
                                                        sourceReference = "Admin Curated",
                                                        active = true
                                                    )
                                                )
                                                chalTitle = ""; chalPrompt = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Publish Challenge") }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    var versionName by remember { mutableStateOf("1.1.0") }
                    var versionCode by remember { mutableStateOf("2") }
                    var channel by remember { mutableStateOf("stable") }
                    var releaseNotes by remember { mutableStateOf("") }
                    var downloadUrl by remember { mutableStateOf("https://") }
                    var sha256 by remember { mutableStateOf("") }
                    var minSupported by remember { mutableStateOf("1.0.0") }
                    var mandatory by remember { mutableStateOf(false) }
                    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader("Application Release Manager", "Publish SHA-256 verified HTTPS updates & toggle rollout")
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Publish New Release", style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = versionName, onValueChange = { versionName = it }, label = { Text("SemVer (X.Y.Z)") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = versionCode, onValueChange = { versionCode = it }, label = { Text("Build #") }, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("stable", "beta").forEach { ch ->
                                            FilterChip(selected = channel == ch, onClick = { channel = ch }, label = { Text(ch) })
                                        }
                                    }
                                    OutlinedTextField(value = downloadUrl, onValueChange = { downloadUrl = it }, label = { Text("HTTPS Installer URL (https://...)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = sha256, onValueChange = { sha256 = it }, label = { Text("SHA-256 Checksum (64 hex chars)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = minSupported, onValueChange = { minSupported = it }, label = { Text("Min Supported Version (X.Y.Z)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = releaseNotes, onValueChange = { releaseNotes = it }, label = { Text("Release Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = mandatory, onCheckedChange = { mandatory = it })
                                        Text("Mandatory update", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = {
                                            vm.publishRelease(
                                                ApplicationRelease(
                                                    versionName = versionName.trim(),
                                                    versionCode = versionCode.toLongOrNull() ?: 2L,
                                                    channel = channel,
                                                    releaseDate = todayIso,
                                                    releaseNotes = releaseNotes.ifBlank { "SpeakCraft update v$versionName" },
                                                    downloadUrl = downloadUrl.trim(),
                                                    installerFileName = "speakcraft_${versionName.trim()}.apk",
                                                    sha256Checksum = sha256.trim().lowercase(),
                                                    minSupportedVersion = minSupported.trim(),
                                                    mandatory = mandatory,
                                                    enabled = true
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("admin_publish_release_button")
                                    ) {
                                        Icon(Icons.Default.Publish, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Publish Verified Release")
                                    }
                                }
                            }
                        }

                        items(releases, key = { it.id.ifBlank { it.versionName + it.versionCode } }) { rel ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("v${rel.versionName} (build ${rel.versionCode}) • ${rel.channel}", style = MaterialTheme.typography.titleMedium)
                                        Text(if (rel.enabled) "Enabled • ${rel.releaseDate}" else "Disabled", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { vm.toggleReleaseEnabled(rel) }) {
                                        Text(if (rel.enabled) "Disable" else "Enable")
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val failures = updateEvents.count { it.status == "FAILED" }
                    val verified = updateEvents.count { it.status == "VERIFIED" || it.status == "INSTALL_LAUNCHED" }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader("System Health & Update Telemetry", "Monitor release integrity & client update funnel")
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile("Published Releases", "${releases.size}", "${releases.count { it.enabled }} active", AcousticTeal, Modifier.weight(1f))
                                MetricTile("Verified Installs", "$verified", "$failures failures logged", EmeraldSuccess, Modifier.weight(1f))
                            }
                        }
                        items(updateEvents.take(25)) { ev ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${ev.status} • v${ev.fromVersion} -> v${ev.toVersion} (${ev.channel})", style = MaterialTheme.typography.labelLarge)
                                    if (ev.detail.isNotBlank()) Text(ev.detail, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
