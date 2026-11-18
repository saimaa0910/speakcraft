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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppPreferences
import com.example.data.update.UpdateDownloadState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppUpdateScreen(
    vm: UpdateAndAdminViewModel,
    prefs: AppPreferences,
    onSetUpdateChannel: (String) -> Unit
) {
    val evaluation by vm.updateEvaluation.collectAsStateWithLifecycle()
    val downloadState by vm.downloadState.collectAsStateWithLifecycle()
    val backups by vm.backups.collectAsStateWithLifecycle()
    val interruptedWarning by vm.interruptedWarning.collectAsStateWithLifecycle()
    val events by vm.userUpdateEvents.collectAsStateWithLifecycle()

    val dateFormatter = rememberDateFormatter()
    val lastCheckText = if (prefs.lastUpdateCheckMs > 0L) {
        dateFormatter.format(Date(prefs.lastUpdateCheckMs))
    } else "On app launch"

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Application Updates & Backups",
                    subtitle = "Semantic versioning, SHA-256 integrity checks & rollback-safe backups"
                )
            }

            if (interruptedWarning != null) {
                item {
                    ErrorRecoveryCard(
                        message = interruptedWarning.orEmpty(),
                        onRetry = { vm.dismissInterruptedWarning() }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Installed Version",
                        value = "v${evaluation.currentVersionName}",
                        delta = "Build ${evaluation.currentVersionCode}",
                        accentColor = AcousticTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Update Channel",
                        value = prefs.updateChannel.replaceFirstChar { it.uppercase() },
                        delta = "Checked: $lastCheckText",
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Release Channel & Manual Check", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("stable", "beta").forEach { channel ->
                                FilterChip(
                                    selected = prefs.updateChannel == channel,
                                    onClick = { onSetUpdateChannel(channel) },
                                    label = { Text(channel.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        Text(evaluation.statusSummary, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { vm.checkForUpdatesManual(prefs) },
                            modifier = Modifier.fillMaxWidth().testTag("check_for_updates_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Check for Updates Now")
                        }
                    }
                }
            }

            val latest = evaluation.latestRelease
            item {
                if (latest == null) {
                    EmptyStateCard(
                        title = "You're on SpeakCraft v${evaluation.currentVersionName}",
                        message = "No remote releases are published on the '${prefs.updateChannel}' channel yet. When an administrator publishes an update, its changelog and verified SHA-256 package will appear here."
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (evaluation.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Release v${latest.versionName} (${latest.channel})", style = MaterialTheme.typography.titleLarge)
                                }
                                if (latest.mandatory) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                                        Text("Mandatory", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Text(
                                "Released ${latest.releaseDate} • Min Supported v${latest.minSupportedVersion} • ${latest.fileSizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(latest.releaseNotes, style = MaterialTheme.typography.bodyMedium)

                            if (latest.features.isNotEmpty()) {
                                Text("New Features", style = MaterialTheme.typography.labelLarge, color = AcousticTeal)
                                latest.features.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            if (latest.bugFixes.isNotEmpty()) {
                                Text("Bug Fixes", style = MaterialTheme.typography.labelLarge)
                                latest.bugFixes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            if (latest.securityImprovements.isNotEmpty()) {
                                Text("Security Improvements", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                                latest.securityImprovements.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess)
                                    Text(
                                        "SHA-256: ${latest.sha256Checksum.take(16)}…${latest.sha256Checksum.takeLast(8)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            when (val dl = downloadState) {
                                is UpdateDownloadState.Idle -> {
                                    Button(
                                        onClick = { vm.downloadAndVerifyRelease(latest, prefs) },
                                        enabled = evaluation.isUpdateAvailable,
                                        modifier = Modifier.fillMaxWidth().testTag("download_update_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (evaluation.isUpdateAvailable) "Download & Verify v${latest.versionName}" else "Current Version Already Installed")
                                    }
                                }
                                is UpdateDownloadState.Downloading -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LinearProgressIndicator(progress = { dl.progressFraction }, modifier = Modifier.fillMaxWidth())
                                        Text("Downloading ${(dl.progressFraction * 100).toInt()}% (${dl.bytesRead / 1024} KB)...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                is UpdateDownloadState.Verifying -> {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text("Verifying SHA-256 checksum & APK signing certificate...", style = MaterialTheme.typography.bodySmall)
                                }
                                is UpdateDownloadState.VerifiedReady -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                        Text(
                                            "Verified (${if (dl.signatureVerified) "Checksum + Signing Cert OK" else "Checksum OK"})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Button(
                                        onClick = { vm.launchInstall(latest) },
                                        modifier = Modifier.fillMaxWidth().testTag("install_verified_update_button")
                                    ) {
                                        Text("Launch Verified Installer")
                                    }
                                }
                                is UpdateDownloadState.Failed -> {
                                    ErrorRecoveryCard(dl.reason) { vm.resetDownloadState() }
                                }
                            }
                        }
                    }
                }
            }

            // Pre-Update Backup Manager
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Pre-Update Data Backups", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "SpeakCraft automatically snapshots local preferences & recordings metadata before every update. Cloud user progress stays protected in Firestore.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = { vm.createManualBackup(prefs) },
                            modifier = Modifier.fillMaxWidth().testTag("create_backup_button")
                        ) {
                            Text("Create Manual Snapshot Backup Now")
                        }
                        backups.take(4).forEach { b ->
                            Text(
                                "• ${b.fileName} • v${b.appVersionName} (${b.recordingsCount} recordings indexed, ${b.sizeBytes} B)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (events.isNotEmpty()) {
                item {
                    SectionHeader("Update Activity History", "Audit trail of checks, verifications & installs")
                }
                items(events.take(8)) { ev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${ev.status}: v${ev.fromVersion} -> v${ev.toVersion} (${ev.channel})", style = MaterialTheme.typography.labelLarge)
                            if (ev.detail.isNotBlank()) Text(ev.detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return androidx.compose.runtime.remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.US)
    }
}
