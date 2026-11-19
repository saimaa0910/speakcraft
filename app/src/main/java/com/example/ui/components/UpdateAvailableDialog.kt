package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ApplicationRelease
import com.example.data.update.ApkIntegrityVerifier
import com.example.data.update.UpdateEvaluation
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

/**
 * Modal dialog triggered when Firestore (or merged GitHub Releases) detects a newer SpeakCraft
 * version. Displays release notes, version jump, optional signing-key conflict guidance, and a
 * direct HTTPS APK download link alongside the in-app verified updater.
 */
@Composable
fun UpdateAvailableDialog(
    evaluation: UpdateEvaluation,
    release: ApplicationRelease,
    onDismiss: () -> Unit,
    onInstallViaUpdater: () -> Unit,
    onOpenDirectDownload: ((String) -> Unit)? = null
) {
    val ext = MaterialTheme.speakCraftColors
    val uriHandler = LocalUriHandler.current
    val hasDirectUrl = release.downloadUrl.trim().startsWith("https://")
    val openDirectLink: () -> Unit = {
        val url = release.downloadUrl.trim()
        if (url.startsWith("https://")) {
            if (onOpenDirectDownload != null) {
                onOpenDirectDownload(url)
            } else {
                uriHandler.openUri(url)
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!evaluation.isMandatory) onDismiss()
        },
        modifier = Modifier.testTag("update_available_dialog"),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (evaluation.hasSigningConflictWithInstalled) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (evaluation.hasSigningConflictWithInstalled) {
                            Icons.Default.Warning
                        } else {
                            Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = if (evaluation.hasSigningConflictWithInstalled) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Update Available: v${release.versionName}",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = release.channel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "v${evaluation.currentVersionName} → v${release.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (evaluation.isMandatory) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "MANDATORY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val metaParts = buildList {
                    if (release.releaseDate.isNotBlank()) add("Released ${release.releaseDate}")
                    add("Build ${release.versionCode}")
                    if (release.fileSizeBytes > 0L) {
                        val sizeMb = release.fileSizeBytes / (1024.0 * 1024.0)
                        add(if (sizeMb >= 1.0) String.format("%.1f MB", sizeMb) else "${release.fileSizeBytes / 1024} KB")
                    }
                }
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (evaluation.hasSigningConflictWithInstalled) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_dialog_signing_conflict_notice")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Clean Reinstall Needed (Different Signing Key)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Installed key: ${ApkIntegrityVerifier.formatShortFingerprint(evaluation.installedCertFingerprint)} • Release key: ${ApkIntegrityVerifier.formatShortFingerprint(evaluation.releaseCertFingerprint)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Android blocks overwriting an app when switching from a Debug keystore to a Release keystore. Your progress is saved in Firebase — download the APK link below, uninstall the current build once, then install the new APK.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (release.releaseNotes.isNotBlank()) {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = release.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("update_dialog_release_notes")
                    )
                }

                if (release.features.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        release.features.take(5).forEach { feature ->
                            Text(
                                text = "• $feature",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasDirectUrl) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .clickable(onClick = openDirectLink)
                            .testTag("update_dialog_direct_download_link")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open direct APK download link",
                                tint = ext.info,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct APK Download Link",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.info
                                )
                                Text(
                                    text = release.downloadUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (evaluation.hasSigningConflictWithInstalled && hasDirectUrl) {
                Button(
                    onClick = openDirectLink,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("update_dialog_confirm_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download APK Link")
                }
            } else {
                Button(
                    onClick = onInstallViaUpdater,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("update_dialog_confirm_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download & Install")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!evaluation.hasSigningConflictWithInstalled && hasDirectUrl) {
                    OutlinedButton(
                        onClick = openDirectLink,
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("update_dialog_browser_download_button")
                    ) {
                        Text("Direct Link")
                    }
                }
                if (!evaluation.isMandatory) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("update_dialog_dismiss_button")
                    ) {
                        Text("Later")
                    }
                }
            }
        }
    )
}
