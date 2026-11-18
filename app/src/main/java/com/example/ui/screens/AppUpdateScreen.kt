package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApplicationRelease
import com.example.data.model.ApplicationUpdateEvent
import com.example.data.preferences.AppPreferences
import com.example.data.update.BackupSnapshotInfo
import com.example.data.update.UpdateDownloadState
import com.example.data.update.UpdateEvaluation
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppUpdateScreen(
    vm: UpdateAndAdminViewModel,
    prefs: AppPreferences,
    onSetUpdateChannel: (String) -> Unit,
    onSetGithubReleaseRepo: (String) -> Unit = {}
) {
    val ext = MaterialTheme.speakCraftColors
    val evaluation by vm.updateEvaluation.collectAsStateWithLifecycle()
    val downloadState by vm.downloadState.collectAsStateWithLifecycle()
    val isCheckingForUpdates by vm.isCheckingForUpdates.collectAsStateWithLifecycle()
    val backups by vm.backups.collectAsStateWithLifecycle()
    val interruptedWarning by vm.interruptedWarning.collectAsStateWithLifecycle()
    val events by vm.userUpdateEvents.collectAsStateWithLifecycle()

    var pendingRestoreBackup by remember { mutableStateOf<BackupSnapshotInfo?>(null) }

    val dateFormatter = rememberDateFormatter()
    val lastCheckText = if (prefs.lastUpdateCheckMs > 0L) {
        dateFormatter.format(Date(prefs.lastUpdateCheckMs))
    } else "On app launch"

    pendingRestoreBackup?.let { backup ->
        ConfirmDestructiveDialog(
            title = "Restore Snapshot Backup?",
            message = "This restores your theme, voice speed, and update channel preferences from \"${backup.fileName}\" (v${backup.appVersionName}).",
            confirmLabel = "Restore",
            onConfirm = {
                vm.restoreBackup(backup.fileName)
                pendingRestoreBackup = null
            },
            onDismiss = { pendingRestoreBackup = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)
        val latest = evaluation.latestRelease

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Application Updates & Backups",
                    subtitle = "One-tap check & automatic verified download with rollback-safe snapshots"
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "app_updates")
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
                        accentColor = ext.info,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Update Channel",
                        value = prefs.updateChannel.replaceFirstChar { it.uppercase() },
                        delta = "Checked: $lastCheckText",
                        accentColor = ext.success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            UpdateChannelCard(
                                prefs = prefs,
                                evaluation = evaluation,
                                isChecking = isCheckingForUpdates,
                                onSetUpdateChannel = onSetUpdateChannel,
                                onSetGithubReleaseRepo = onSetGithubReleaseRepo,
                                onCheckNow = { vm.checkForUpdatesManual(prefs) }
                            )
                            LatestReleaseSection(
                                latest = latest,
                                prefs = prefs,
                                evaluation = evaluation,
                                downloadState = downloadState,
                                onDownload = { rel -> vm.downloadAndVerifyRelease(rel, prefs) },
                                onInstall = { rel -> vm.launchInstall(rel) },
                                onResetDownload = { vm.resetDownloadState() }
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BackupManagerCard(
                                backups = backups,
                                onCreateBackup = { vm.createManualBackup(prefs) },
                                onRequestRestore = { pendingRestoreBackup = it }
                            )
                            if (events.isNotEmpty()) {
                                SectionHeader("Update Activity History", "Audit trail of checks, verifications & installs")
                                events.take(8).forEach { ev ->
                                    UpdateEventCard(ev)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    UpdateChannelCard(
                        prefs = prefs,
                        evaluation = evaluation,
                        isChecking = isCheckingForUpdates,
                        onSetUpdateChannel = onSetUpdateChannel,
                        onSetGithubReleaseRepo = onSetGithubReleaseRepo,
                        onCheckNow = { vm.checkForUpdatesManual(prefs) }
                    )
                }

                item {
                    LatestReleaseSection(
                        latest = latest,
                        prefs = prefs,
                        evaluation = evaluation,
                        downloadState = downloadState,
                        onDownload = { rel -> vm.downloadAndVerifyRelease(rel, prefs) },
                        onInstall = { rel -> vm.launchInstall(rel) },
                        onResetDownload = { vm.resetDownloadState() }
                    )
                }

                item {
                    BackupManagerCard(
                        backups = backups,
                        onCreateBackup = { vm.createManualBackup(prefs) },
                        onRequestRestore = { pendingRestoreBackup = it }
                    )
                }

                if (events.isNotEmpty()) {
                    item {
                        SectionHeader("Update Activity History", "Audit trail of checks, verifications & installs")
                    }
                    items(events.take(8)) { ev ->
                        UpdateEventCard(ev)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateChannelCard(
    prefs: AppPreferences,
    evaluation: UpdateEvaluation,
    isChecking: Boolean,
    onSetUpdateChannel: (String) -> Unit,
    onSetGithubReleaseRepo: (String) -> Unit,
    onCheckNow: () -> Unit
) {
    var repoDraft by remember(prefs.githubReleaseRepo) { mutableStateOf(prefs.githubReleaseRepo) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Release Channel & Check for Updates", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                listOf("stable", "beta").forEach { channel ->
                    FilterChip(
                        selected = prefs.updateChannel == channel,
                        onClick = { onSetUpdateChannel(channel) },
                        label = {
                            Text(
                                if (channel == "beta") "Beta (Early Access)" else "Stable"
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("update_channel_chip_$channel")
                    )
                }
            }
            Text(
                if (prefs.updateChannel == "beta") {
                    "Beta channel enabled — installs early-access beta updates (including v1.1.0-beta.1 builds) as well as stable releases."
                } else {
                    "Stable channel enabled — installs production releases only. Switch to Beta above to check and install beta updates."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(evaluation.statusSummary, style = MaterialTheme.typography.bodySmall)
            Text(
                "Checks Firestore applicationReleases (used by all previous APKs including v1.0.0) and your configured GitHub Releases repository, then downloads & SHA-256 verifies any newer APK.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = repoDraft,
                    onValueChange = { repoDraft = it },
                    label = { Text("GitHub Repo (owner/repo, optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("github_release_repo_input")
                )
                OutlinedButton(
                    onClick = { onSetGithubReleaseRepo(repoDraft) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("save_github_release_repo_button")
                ) {
                    Text("Sync")
                }
            }
            Button(
                onClick = onCheckNow,
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    .testTag("check_for_updates_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Checking & Downloading...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Check for Updates Now")
                }
            }
        }
    }
}

@Composable
private fun LatestReleaseSection(
    latest: ApplicationRelease?,
    prefs: AppPreferences,
    evaluation: UpdateEvaluation,
    downloadState: UpdateDownloadState,
    onDownload: (ApplicationRelease) -> Unit,
    onInstall: (ApplicationRelease) -> Unit,
    onResetDownload: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    if (latest == null) {
        EmptyStateCard(
            title = "You're on SpeakCraft v${evaluation.currentVersionName} (Build ${evaluation.currentVersionCode})",
            message = "Active channel: ${prefs.updateChannel.replaceFirstChar { it.uppercase() }}. No published remote release found yet. Push to your GitHub repo with the CI workflow configured (FIREBASE_SERVICE_ACCOUNT_JSON secret) or publish in Admin Console -> Releases so previous APKs (v1.0.0) detect updates automatically."
        )
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.dp,
                if (evaluation.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Text("New Features", style = MaterialTheme.typography.labelLarge, color = ext.info)
                    latest.features.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (latest.bugFixes.isNotEmpty()) {
                    Text("Bug Fixes", style = MaterialTheme.typography.labelLarge)
                    latest.bugFixes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (latest.securityImprovements.isNotEmpty()) {
                    Text("Security Improvements", style = MaterialTheme.typography.labelLarge, color = ext.success)
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = ext.success)
                        Text(
                            "SHA-256: ${latest.sha256Checksum.take(16)}…${latest.sha256Checksum.takeLast(8)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                when (downloadState) {
                    is UpdateDownloadState.Idle -> {
                        Button(
                            onClick = { onDownload(latest) },
                            enabled = evaluation.isUpdateAvailable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("download_update_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (evaluation.isUpdateAvailable) "Download & Verify v${latest.versionName}" else "Current Version Already Installed")
                        }
                    }
                    is UpdateDownloadState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(progress = { downloadState.progressFraction }, modifier = Modifier.fillMaxWidth())
                            Text("Downloading ${(downloadState.progressFraction * 100).toInt()}% (${downloadState.bytesRead / 1024} KB)...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is UpdateDownloadState.Verifying -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Verifying SHA-256 checksum & APK signing certificate...", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateDownloadState.VerifiedReady -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ext.success)
                            Text(
                                "Verified (${if (downloadState.signatureVerified) "Checksum + Signing Cert OK" else "Checksum OK"})",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.success
                            )
                        }
                        Button(
                            onClick = { onInstall(latest) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("install_verified_update_button")
                        ) {
                            Text("Launch Verified Installer")
                        }
                    }
                    is UpdateDownloadState.Failed -> {
                        ErrorRecoveryCard(downloadState.reason) { onResetDownload() }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupManagerCard(
    backups: List<BackupSnapshotInfo>,
    onCreateBackup: () -> Unit,
    onRequestRestore: (BackupSnapshotInfo) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Pre-Update Data Backups", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "SpeakCraft automatically snapshots local preferences & recordings metadata before every update. Cloud user progress stays protected in Firestore.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onCreateBackup,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    .testTag("create_backup_button")
            ) {
                Text("Create Manual Snapshot Backup Now")
            }
            backups.take(4).forEach { b ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("v${b.appVersionName} • ${b.recordingsCount} clips indexed", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${b.fileName} (${b.sizeBytes} B)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onRequestRestore(b) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("restore_backup_button_${b.fileName}")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateEventCard(ev: ApplicationUpdateEvent) {
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

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.US)
    }
}
