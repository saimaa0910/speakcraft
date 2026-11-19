package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.ApplicationRelease
import com.example.data.model.ApplicationUpdateEvent
import com.example.data.model.ExtractedTechnique
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.data.model.YouTubeSource
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.update.BackupSnapshotInfo
import com.example.data.update.GitHubReleaseClient
import com.example.data.update.ReleaseUpdateRepository
import com.example.data.update.UpdateBackupManager
import com.example.data.update.UpdateDownloadState
import com.example.data.update.UpdateEvaluation
import com.example.data.youtube.YouTubeIntelligenceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdateAndAdminViewModel(
    private val repository: SpeakCraftRepository,
    private val updateRepository: ReleaseUpdateRepository,
    private val backupManager: UpdateBackupManager,
    private val prefsRepo: AppPreferencesRepository,
    private val youtubeClient: YouTubeIntelligenceClient,
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    private val currentUserId: String,
    private val githubReleaseClient: GitHubReleaseClient = GitHubReleaseClient()
) : ViewModel() {

    @Deprecated("Use constructor with LocalIntelligenceOrchestrator")
    constructor(
        repository: SpeakCraftRepository,
        updateRepository: ReleaseUpdateRepository,
        backupManager: UpdateBackupManager,
        prefsRepo: AppPreferencesRepository,
        youtubeClient: YouTubeIntelligenceClient,
        geminiCoach: com.example.data.ai.GeminiCoachService,
        currentUserId: String,
        githubReleaseClient: GitHubReleaseClient = GitHubReleaseClient()
    ) : this(
        repository = repository,
        updateRepository = updateRepository,
        backupManager = backupManager,
        prefsRepo = prefsRepo,
        youtubeClient = youtubeClient,
        localOrchestrator = LocalIntelligenceOrchestrator(),
        currentUserId = currentUserId,
        githubReleaseClient = githubReleaseClient
    )

    val enabledReleases: StateFlow<List<ApplicationRelease>> =
        repository.observeEnabledReleases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminReleases: StateFlow<List<ApplicationRelease>> =
        repository.observeAdminReleases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val moderationTechniques: StateFlow<List<ExtractedTechnique>> =
        repository.observePendingModerationTechniques()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminUpdateEvents: StateFlow<List<ApplicationUpdateEvent>> =
        repository.observeAllUpdateEventsForAdmin()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userUpdateEvents: StateFlow<List<ApplicationUpdateEvent>> =
        repository.observeUserUpdateEvents(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloadState: StateFlow<UpdateDownloadState> = updateRepository.downloadState

    private val _checkedReleasesOverride = MutableStateFlow<List<ApplicationRelease>?>(null)

    val updateEvaluation: StateFlow<UpdateEvaluation> = combine(
        enabledReleases,
        _checkedReleasesOverride,
        prefsRepo.preferencesFlow
    ) { observed, checkedOverride, prefs ->
        val source = checkedOverride ?: observed
        updateRepository.evaluateReleases(source, prefs.updateChannel)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        updateRepository.evaluateReleases(emptyList(), "stable")
    )

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupSnapshotInfo>>(backupManager.listBackups())
    val backups: StateFlow<List<BackupSnapshotInfo>> = _backups.asStateFlow()

    private val _interruptedWarning = MutableStateFlow<String?>(
        backupManager.checkInterruptedUpdateMarker(updateRepository.currentVersionName())
    )
    val interruptedWarning: StateFlow<String?> = _interruptedWarning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _adminSyncBusy = MutableStateFlow(false)
    val adminSyncBusy: StateFlow<Boolean> = _adminSyncBusy.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissInterruptedWarning() {
        backupManager.clearRecoveryMarker()
        _interruptedWarning.value = null
    }

    private suspend fun refreshMergedReleases(githubRepo: String): List<ApplicationRelease> {
        val firestoreFetched = repository.fetchEnabledReleasesOnce().getOrNull().orEmpty()
        val githubFetched = if (githubRepo.isNotBlank()) {
            githubReleaseClient.fetchReleases(githubRepo).getOrNull().orEmpty()
        } else {
            emptyList()
        }
        val combined = updateRepository.mergeReleases(
            firestoreReleases = firestoreFetched.ifEmpty { enabledReleases.value },
            githubReleases = githubFetched
        )
        if (combined.isNotEmpty()) {
            _checkedReleasesOverride.value = combined
        }
        return combined.ifEmpty { _checkedReleasesOverride.value ?: enabledReleases.value }
    }

    fun setGithubReleaseRepo(repoSlug: String, currentPrefs: AppPreferences) {
        viewModelScope.launch {
            val normalized = GitHubReleaseClient.normalizeRepoSlug(repoSlug)
            prefsRepo.setGithubReleaseRepo(normalized)
            if (normalized.isNotBlank()) {
                checkForUpdatesManual(currentPrefs.copy(githubReleaseRepo = normalized))
            } else {
                _statusMessage.value = "Cleared custom GitHub release repository."
            }
        }
    }

    fun selectUpdateChannel(channel: String, currentPrefs: AppPreferences) {
        val safeChannel = if (channel == "beta") "beta" else "stable"
        val currentDl = downloadState.value
        if (currentDl is UpdateDownloadState.VerifiedReady ||
            currentDl is UpdateDownloadState.Failed ||
            currentDl is UpdateDownloadState.SigningConflict
        ) {
            updateRepository.resetDownloadState()
        }
        viewModelScope.launch {
            prefsRepo.setUpdateChannel(safeChannel)
            _isCheckingForUpdates.value = true
            try {
                val now = System.currentTimeMillis()
                prefsRepo.recordUpdateCheck(now)
                val candidateReleases = refreshMergedReleases(currentPrefs.githubReleaseRepo)
                val eval = updateRepository.evaluateReleases(candidateReleases, safeChannel)
                _statusMessage.value = if (eval.isUpdateAvailable && eval.latestRelease != null) {
                    "Switched to ${safeChannel.replaceFirstChar { it.uppercase() }} channel — v${eval.latestRelease.versionName} available."
                } else {
                    "Update channel set to ${safeChannel.replaceFirstChar { it.uppercase() }}."
                }
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    fun refreshReleasesSilently(prefs: AppPreferences) {
        if (prefs.githubReleaseRepo.isBlank()) return
        viewModelScope.launch {
            runCatching {
                refreshMergedReleases(prefs.githubReleaseRepo)
            }
        }
    }

    fun checkForUpdatesManual(prefs: AppPreferences) {
        if (_isCheckingForUpdates.value) return
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            try {
                val now = System.currentTimeMillis()
                prefsRepo.recordUpdateCheck(now)
                val candidateReleases = refreshMergedReleases(prefs.githubReleaseRepo)
                val eval = updateRepository.evaluateReleases(candidateReleases, prefs.updateChannel)
                val latest = eval.latestRelease
                repository.logUpdateEvent(
                    fromVersion = eval.currentVersionName,
                    toVersion = latest?.versionName ?: eval.currentVersionName,
                    channel = prefs.updateChannel,
                    status = "CHECKED",
                    detail = eval.statusSummary
                )
                if (eval.isUpdateAvailable && latest != null) {
                    if (eval.hasSigningConflictWithInstalled) {
                        _statusMessage.value = "v${latest.versionName} uses a different signing key — open the direct APK link for a one-time clean reinstall."
                    } else {
                        val currentDl = downloadState.value
                        if (currentDl !is UpdateDownloadState.Downloading &&
                            currentDl !is UpdateDownloadState.Verifying &&
                            currentDl !is UpdateDownloadState.VerifiedReady
                        ) {
                            _statusMessage.value = "Update v${latest.versionName} found — downloading & verifying..."
                            performDownloadAndVerify(latest, prefs)
                        } else {
                            _statusMessage.value = "Update v${latest.versionName} is ready."
                        }
                    }
                } else {
                    _statusMessage.value = eval.statusSummary
                }
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    fun downloadAndVerifyRelease(release: ApplicationRelease, prefs: AppPreferences) {
        viewModelScope.launch {
            performDownloadAndVerify(release, prefs)
        }
    }

    private suspend fun performDownloadAndVerify(release: ApplicationRelease, prefs: AppPreferences) {
        val res = updateRepository.downloadAndVerifyRelease(release, prefs)
        _backups.value = backupManager.listBackups()
        res.onSuccess {
            repository.logUpdateEvent(
                fromVersion = updateRepository.currentVersionName(),
                toVersion = release.versionName,
                channel = release.channel,
                status = "VERIFIED",
                detail = "SHA-256 checksum & signature check completed."
            )
            _statusMessage.value = "Verified v${release.versionName}. Ready to install."
        }.onFailure { err ->
            repository.logUpdateEvent(
                fromVersion = updateRepository.currentVersionName(),
                toVersion = release.versionName,
                channel = release.channel,
                status = "FAILED",
                detail = err.message ?: "Download verification failed."
            )
            _statusMessage.value = err.message ?: "Update verification failed."
        }
    }

    fun launchInstall(release: ApplicationRelease) {
        viewModelScope.launch {
            val state = downloadState.value
            val apkFile = (state as? UpdateDownloadState.VerifiedReady)?.apkFile
            val res = updateRepository.launchVerifiedInstallerOrReleasePage(apkFile, release.downloadUrl)
            res.onSuccess { msg ->
                repository.logUpdateEvent(
                    fromVersion = updateRepository.currentVersionName(),
                    toVersion = release.versionName,
                    channel = release.channel,
                    status = "INSTALL_LAUNCHED",
                    detail = msg
                )
                _statusMessage.value = msg
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to launch installer."
            }
        }
    }

    fun openDirectDownloadLink(release: ApplicationRelease) {
        viewModelScope.launch {
            val res = updateRepository.openDirectDownloadLink(release.downloadUrl)
            res.onSuccess { msg ->
                repository.logUpdateEvent(
                    fromVersion = updateRepository.currentVersionName(),
                    toVersion = release.versionName,
                    channel = release.channel,
                    status = "INSTALL_LAUNCHED",
                    detail = msg
                )
                _statusMessage.value = msg
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to open direct download link."
            }
        }
    }

    fun createManualBackup(prefs: AppPreferences) {
        viewModelScope.launch {
            val res = backupManager.createPreUpdateBackup(
                prefs = prefs,
                currentVersionName = updateRepository.currentVersionName(),
                currentVersionCode = updateRepository.currentVersionCode(),
                targetVersionName = updateRepository.currentVersionName()
            )
            backupManager.clearRecoveryMarker() // manual snapshot doesn't indicate an interrupted update
            _backups.value = backupManager.listBackups()
            _statusMessage.value = res.fold(
                onSuccess = { "Created local backup snapshot ${it.fileName}." },
                onFailure = { "Backup failed: ${it.message}" }
            )
        }
    }

    fun restoreBackup(fileName: String) {
        viewModelScope.launch {
            val res = backupManager.restoreBackupSnapshot(fileName)
            res.onSuccess { restored ->
                prefsRepo.setThemeMode(restored.themeMode)
                prefsRepo.setTtsRate(restored.ttsRate)
                prefsRepo.setUpdateChannel(restored.updateChannel)
                _statusMessage.value = "Restored preferences from ${restored.fileName} (v${restored.appVersionName})."
            }.onFailure { err ->
                _statusMessage.value = "Restore failed: ${err.message}"
            }
        }
    }

    fun resetDownloadState() {
        updateRepository.resetDownloadState()
    }

    // ==================== ADMIN CONSOLE ACTIONS ====================

    fun adminSyncYouTubeChannel(url: String, apiKey: String, autoPublish: Boolean) {
        viewModelScope.launch {
            _adminSyncBusy.value = true
            val auditRes = youtubeClient.inspectUrl(url, apiKey)
            auditRes.onFailure { err ->
                _statusMessage.value = "Sync failed: ${err.message}"
            }.onSuccess { audit ->
                var savedCount = 0
                audit.videos.forEach { video ->
                    val techniques = if (video.extractionStatus != "INACCESSIBLE") {
                        localOrchestrator.extractYouTubeLessons(
                            videoId = video.videoId,
                            videoTitle = video.title,
                            videoUrl = video.videoUrl,
                            description = video.description,
                            transcriptText = video.transcript
                        ).getOrNull()?.techniques.orEmpty()
                    } else emptyList()

                    savedCount += techniques.size
                    repository.adminSyncVideoAndTechniques(
                        source = YouTubeSource(
                            videoId = video.videoId,
                            channelHandle = video.channelHandle,
                            playlistTitle = video.playlistTitle,
                            publishedDate = video.publishedDate,
                            durationSeconds = video.durationSeconds,
                            title = video.title,
                            videoUrl = video.videoUrl,
                            thumbnailUrl = video.thumbnailUrl,
                            extractionStatus = video.extractionStatus,
                            statusDetail = video.statusDetail,
                            transcriptAvailable = video.transcript != null
                        ),
                        segments = video.segments,
                        techniques = techniques,
                        moderationStatus = if (autoPublish) "PUBLISHED" else "PENDING_REVIEW"
                    )
                }
                _statusMessage.value = "Synced ${audit.videos.size} videos and queued $savedCount techniques."
            }
            _adminSyncBusy.value = false
        }
    }

    fun moderateTechnique(techniqueId: String, status: String) {
        viewModelScope.launch {
            val res = repository.adminModerateTechnique(techniqueId, status)
            _statusMessage.value = if (res.isSuccess) "Technique set to $status." else res.exceptionOrNull()?.message
        }
    }

    fun saveSharedLesson(lesson: Lesson) {
        viewModelScope.launch {
            val res = repository.adminSaveSharedLesson(lesson)
            _statusMessage.value = if (res.isSuccess) "Shared lesson '${lesson.title}' saved." else res.exceptionOrNull()?.message
        }
    }

    fun saveSharedChallenge(challenge: LearningChallenge) {
        viewModelScope.launch {
            val res = repository.adminSaveSharedChallenge(challenge)
            _statusMessage.value = if (res.isSuccess) "Challenge '${challenge.title}' saved." else res.exceptionOrNull()?.message
        }
    }

    fun publishRelease(release: ApplicationRelease) {
        viewModelScope.launch {
            val res = repository.adminPublishRelease(release)
            _statusMessage.value = if (res.isSuccess) {
                "Published release v${release.versionName} (${release.channel})."
            } else {
                res.exceptionOrNull()?.message ?: "Release validation failed (check HTTPS & 64-char SHA-256)."
            }
        }
    }

    fun toggleReleaseEnabled(release: ApplicationRelease) {
        viewModelScope.launch {
            val res = repository.adminToggleReleaseEnabled(release)
            _statusMessage.value = if (res.isSuccess) {
                "Release v${release.versionName} ${if (!release.enabled) "enabled" else "disabled"}."
            } else {
                res.exceptionOrNull()?.message
            }
        }
    }
}
