package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.ai.StudioTaskType
import com.example.data.learning.DeviceMediaFile
import com.example.data.learning.PersistedLearningSnapshot
import com.example.data.learning.RecordingSourceCategory
import com.example.data.learning.UnifiedRecordedSession
import com.example.data.learning.UnifiedRecordingsCatalog
import com.example.data.learningpaths.LearningPathsUserState
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkillBreakdownBars
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val profileLevels = listOf(
    "Beginner",
    "Elementary",
    "Intermediate",
    "Upper Intermediate",
    "Advanced"
)

private val profileGoals = listOf(
    "Daily English Fluency",
    "Speaking Confidence",
    "Pronunciation Improvement",
    "Job Interviews",
    "Public Speaking",
    "Workplace Communication",
    "Conversation Practice",
    "Vocabulary Building"
)

private val dailyMinuteChoices = listOf(5L, 10L, 15L, 20L, 30L, 45L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    profile: UserProfile,
    userEmail: String,
    sessions: List<SpeakingSession>,
    vocabulary: List<VocabularyEntry>,
    learningSnapshot: PersistedLearningSnapshot?,
    onSaveProfile: (displayName: String, level: String, primaryGoal: String, dailyGoalMinutes: Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    initialTab: Int = 0,
    learningPathsState: LearningPathsUserState? = null,
    deviceMediaFiles: List<DeviceMediaFile> = emptyList(),
    playingMediaPath: String? = null,
    onTogglePlayMedia: ((String) -> Unit)? = null,
    onStopPlayback: () -> Unit = {},
    onDeleteRecordedSession: ((UnifiedRecordedSession) -> Unit)? = null
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var nameDraft by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var levelDraft by rememberSaveable(profile.communicationLevel) {
        mutableStateOf(profile.communicationLevel.ifBlank { "Intermediate" })
    }
    var goalDraft by rememberSaveable(profile.primaryGoal) {
        mutableStateOf(profile.primaryGoal.ifBlank { "Daily English Fluency" })
    }
    var minutesDraft by rememberSaveable(profile.dailyGoalMinutes) {
        mutableLongStateOf(profile.dailyGoalMinutes.coerceIn(5L, 60L))
    }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val allRecordings = remember(sessions, learningSnapshot, learningPathsState, deviceMediaFiles) {
        UnifiedRecordingsCatalog.collectAll(
            speakingSessions = sessions,
            learningSnapshot = learningSnapshot,
            learningPathsState = learningPathsState,
            deviceMediaFiles = deviceMediaFiles
        )
    }

    var localPlayingPath by remember { mutableStateOf<String?>(null) }
    var localPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                localPlayer?.stop()
                localPlayer?.release()
            }
            localPlayer = null
        }
    }

    val effectivePlayingPath = if (onTogglePlayMedia != null) playingMediaPath else localPlayingPath
    fun toggleAudioPlay(path: String) {
        if (onTogglePlayMedia != null) {
            onTogglePlayMedia(path)
            return
        }
        val current = localPlayer
        if (localPlayingPath == path && current != null) {
            runCatching {
                current.stop()
                current.release()
            }
            localPlayer = null
            localPlayingPath = null
            return
        }
        runCatching {
            current?.stop()
            current?.release()
        }
        localPlayer = null
        localPlayingPath = null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    localPlayingPath = null
                    runCatching { it.release() }
                    localPlayer = null
                }
                start()
            }
            localPlayer = mp
            localPlayingPath = path
        }
    }

    var pendingDeleteRecording by remember { mutableStateOf<UnifiedRecordedSession?>(null) }
    var activeVideoRecording by remember { mutableStateOf<UnifiedRecordedSession?>(null) }
    var recordingsFilter by rememberSaveable { mutableStateOf(RecordingSourceCategory.ALL) }
    var recordingsSearch by rememberSaveable { mutableStateOf("") }
    var geminiStatusMessage by remember { mutableStateOf<String?>(null) }

    pendingDeleteRecording?.let { item ->
        ConfirmDestructiveDialog(
            title = "Delete Recording?",
            message = "This permanently removes \"${item.title}\" and its local audio or video clip.",
            confirmLabel = "Delete",
            onConfirm = {
                if (effectivePlayingPath == item.localMediaPath) {
                    if (onTogglePlayMedia != null) onStopPlayback() else toggleAudioPlay(item.localMediaPath)
                }
                onDeleteRecordedSession?.invoke(item)
                pendingDeleteRecording = null
            },
            onDismiss = { pendingDeleteRecording = null }
        )
    }

    activeVideoRecording?.let { videoItem ->
        AlertDialog(
            onDismissRequest = { activeVideoRecording = null },
            title = { Text(videoItem.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                val clipFile = File(videoItem.localMediaPath)
                if (!clipFile.exists() || clipFile.length() <= 0L) {
                    Text(
                        "Video clip file is no longer available on device storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.fromFile(clipFile))
                                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                                setOnPreparedListener {
                                    it.isLooping = false
                                    start()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeVideoRecording = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSignOutConfirm) {
        ConfirmDestructiveDialog(
            title = "Sign Out of SpeakCraft?",
            message = "Your cloud progress is safely stored in Firebase. Sign back in anytime with Google.",
            confirmLabel = "Sign Out",
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }

    val displayName = profile.displayName.ifBlank { "Speaker" }
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "S" }
    }
    val overallScore = remember(profile) {
        ((profile.fluencyScore + profile.pronunciationScore + profile.vocabularyScore +
            profile.grammarScore + profile.articulationScore + profile.listeningScore) / 6).toInt()
    }
    val totalMinutes = maxOf(
        profile.totalPracticeMinutes.toInt(),
        learningSnapshot?.totalPracticeMinutes ?: 0
    )
    val streakDays = maxOf(
        profile.streakDays.toInt(),
        learningSnapshot?.streak?.currentStreakDays ?: 0
    )
    val wordsMastered = maxOf(
        vocabulary.size,
        profile.wordsLearnedCount.toInt(),
        learningSnapshot?.wordProgress?.values?.count { it.isCompleted } ?: 0
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = containerMaxWidth)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    modifier = Modifier.testTag("profile_tab_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recordings (${allRecordings.size})") },
                    modifier = Modifier.testTag("profile_tab_recordings")
                )
            }

            if (selectedTab == 1) {
                val filteredRecordings = remember(allRecordings, recordingsFilter, recordingsSearch) {
                    val q = recordingsSearch.trim().lowercase()
                    allRecordings.filter { item ->
                        val catMatch = recordingsFilter == RecordingSourceCategory.ALL || item.sourceCategory == recordingsFilter
                        val textMatch = q.isBlank() ||
                            item.title.lowercase().contains(q) ||
                            item.sourceLabel.lowercase().contains(q) ||
                            item.summary.lowercase().contains(q) ||
                            item.transcript.lowercase().contains(q)
                        catMatch && textMatch
                    }
                }
                val videoCount = remember(allRecordings) { allRecordings.count { it.isVideo } }
                val audioCount = allRecordings.size - videoCount
                val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("profile_recordings_tab_content"),
                    contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "All Recorded Sessions",
                            subtitle = "${allRecordings.size} total • $audioCount audio, $videoCount video across all SpeakCraft modules"
                        )
                    }

                    if (!geminiStatusMessage.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = geminiStatusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = recordingsSearch,
                            onValueChange = { recordingsSearch = it },
                            placeholder = { Text("Search recordings by title, module, or transcript...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (recordingsSearch.isNotBlank()) {
                                    IconButton(onClick = { recordingsSearch = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_recordings_search_input")
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecordingSourceCategory.values().forEach { cat ->
                                FilterChip(
                                    selected = recordingsFilter == cat,
                                    onClick = { recordingsFilter = cat },
                                    label = { Text(cat.label) },
                                    modifier = Modifier.testTag("profile_recordings_filter_${cat.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredRecordings.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().testTag("profile_recordings_empty")) {
                                EmptyStateCard(
                                    title = if (allRecordings.isEmpty()) "No saved recordings yet" else "No matching recordings",
                                    message = if (allRecordings.isEmpty()) {
                                        "Your saved audio and video clips from Record & Review Studio, Coach Practice, Learning Paths, 150 Activities, and Speaking Labs will appear here."
                                    } else {
                                        "Try clearing your search or choosing All recordings."
                                    },
                                    ctaLabel = if (allRecordings.isEmpty()) "Open Record & Review" else "Show All",
                                    onCta = {
                                        if (allRecordings.isEmpty()) {
                                            onNavigate("speaking_lab")
                                        } else {
                                            recordingsFilter = RecordingSourceCategory.ALL
                                            recordingsSearch = ""
                                        }
                                    },
                                    icon = Icons.Default.Mic
                                )
                            }
                        }
                    } else {
                        items(filteredRecordings, key = { it.key }) { rec ->
                            val isThisPlaying = effectivePlayingPath == rec.localMediaPath && rec.localMediaPath.isNotBlank()
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_recording_card_${rec.key}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = rec.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ext.infoContainer
                                                ) {
                                                    Text(
                                                        text = rec.sourceLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ext.info,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (rec.isVideo) ext.pastelLavenderBg else ext.pastelSkyBg
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (rec.isVideo) Icons.Default.Videocam else Icons.Default.Mic,
                                                            contentDescription = null,
                                                            tint = if (rec.isVideo) ext.pastelLavenderInk else ext.pastelSkyInk,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = if (rec.isVideo) "Video" else "Audio",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (rec.isVideo) ext.pastelLavenderInk else ext.pastelSkyInk
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (rec.localMediaPath.isNotBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        if (rec.isVideo) {
                                                            activeVideoRecording = rec
                                                        } else {
                                                            toggleAudioPlay(rec.localMediaPath)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .defaultMinSize(
                                                            minWidth = SpeakCraftSpacing.minTouchTarget,
                                                            minHeight = SpeakCraftSpacing.minTouchTarget
                                                        )
                                                        .testTag("profile_play_recording_${rec.key}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = if (isThisPlaying) "Stop playback" else "Play recording",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            if (onDeleteRecordedSession != null) {
                                                IconButton(
                                                    onClick = { pendingDeleteRecording = rec },
                                                    modifier = Modifier
                                                        .defaultMinSize(
                                                            minWidth = SpeakCraftSpacing.minTouchTarget,
                                                            minHeight = SpeakCraftSpacing.minTouchTarget
                                                        )
                                                        .testTag("profile_delete_recording_${rec.key}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete recording",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val metaParts = buildList {
                                        if (rec.durationSeconds > 0) {
                                            add(String.format(Locale.US, "%02d:%02d", rec.durationSeconds / 60, rec.durationSeconds % 60))
                                        }
                                        if (rec.timestampMs > 0L) {
                                            add(dateFmt.format(Date(rec.timestampMs)))
                                        }
                                    }
                                    if (metaParts.isNotEmpty()) {
                                        Text(
                                            text = metaParts.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (rec.badges.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rec.badges.forEach { badge ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                ) {
                                                    Text(
                                                        text = badge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    rec.comparisonNote?.let { note ->
                                        Surface(
                                            color = (if (rec.comparisonPositive) ext.success else MaterialTheme.colorScheme.error).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (rec.comparisonPositive) ext.success else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = rec.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (rec.transcript.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "\"${rec.transcript.take(220)}${if (rec.transcript.length > 220) "…" else ""}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    val supportsGeminiExport = rec.backedSpeakingSession != null ||
                                        rec.backedSpeakingRecordingId != null ||
                                        rec.sourceCategory == RecordingSourceCategory.SPEAKING_STUDIO ||
                                        rec.sourceCategory == RecordingSourceCategory.SPEAKING_LABS
                                    if (supportsGeminiExport && (rec.localMediaPath.isNotBlank() || rec.title.isNotBlank())) {
                                        OutlinedButton(
                                            onClick = {
                                                val file = rec.localMediaPath.takeIf { it.isNotBlank() }?.let(::File)
                                                val inferredTask = if (rec.title.startsWith("Daily Video", ignoreCase = true)) {
                                                    StudioTaskType.DAILY_VIDEO
                                                } else {
                                                    StudioTaskType.SELECT_TOPIC
                                                }
                                                val res = PhoneGeminiBridge.sendStudioRecordingToGeminiApp(
                                                    context = context,
                                                    mediaFile = file,
                                                    isVideo = rec.isVideo,
                                                    taskType = inferredTask,
                                                    taskTitle = rec.title,
                                                    transcript = rec.transcript,
                                                    durationSeconds = rec.durationSeconds
                                                )
                                                geminiStatusMessage = res.userMessage
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Send Saved Clip to Gemini App")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Hero Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ext.sidebarTop, ext.sidebarActiveStart)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ext.onHeroTitle.copy(alpha = 0.2f),
                                    border = BorderStroke(2.dp, ext.onHeroTitle.copy(alpha = 0.7f)),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ext.onHeroTitle
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onHeroTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (userEmail.isNotBlank()) {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.sidebarTextIdle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ext.onHeroTitle.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = profile.communicationLevel.ifBlank { "Intermediate" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.onHeroTitle,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ext.onHeroTitle.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "Score $overallScore/100",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.onHeroTitle,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Focus Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${profile.primaryGoal.ifBlank { "Daily English Fluency" }} • ${profile.dailyGoalMinutes} min/day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("profile_edit_toggle_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isEditing) "Close" else "Edit Profile")
                            }
                        }
                    }
                }
            }

            // Inline Profile Editor
            if (isEditing) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("profile_editor_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Update Your Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            Text("Communication Level", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileLevels.forEach { lvl ->
                                    FilterChip(
                                        selected = levelDraft.equals(lvl, ignoreCase = true),
                                        onClick = { levelDraft = lvl },
                                        label = { Text(lvl) }
                                    )
                                }
                            }
                            Text("Primary Goal", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileGoals.forEach { goal ->
                                    FilterChip(
                                        selected = goalDraft.equals(goal, ignoreCase = true),
                                        onClick = { goalDraft = goal },
                                        label = { Text(goal) }
                                    )
                                }
                            }
                            Text("Daily Practice Target", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dailyMinuteChoices.forEach { mins ->
                                    FilterChip(
                                        selected = minutesDraft == mins,
                                        onClick = { minutesDraft = mins },
                                        label = { Text("$mins min") }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        nameDraft = profile.displayName
                                        levelDraft = profile.communicationLevel.ifBlank { "Intermediate" }
                                        goalDraft = profile.primaryGoal.ifBlank { "Daily English Fluency" }
                                        minutesDraft = profile.dailyGoalMinutes.coerceIn(5L, 60L)
                                        isEditing = false
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        onSaveProfile(
                                            nameDraft.trim().ifBlank { displayName },
                                            levelDraft,
                                            goalDraft,
                                            minutesDraft
                                        )
                                        isEditing = false
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("profile_save_button")
                                ) {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }

            // Stats Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Active Streak",
                            value = "$streakDays days",
                            bg = ext.pastelGoldBg,
                            ink = ext.pastelGoldInk,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            icon = Icons.Default.Schedule,
                            label = "Practice Time",
                            value = String.format("%.1f hrs", totalMinutes / 60.0),
                            bg = ext.pastelLavenderBg,
                            ink = ext.pastelLavenderInk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            icon = Icons.Default.Mic,
                            label = "Recordings",
                            value = "${allRecordings.size}",
                            bg = ext.pastelSkyBg,
                            ink = ext.pastelSkyInk,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("profile_recordings_stat_card")
                        )
                        ProfileStatCard(
                            icon = Icons.Default.School,
                            label = "Words Learned",
                            value = "$wordsMastered",
                            bg = ext.pastelMintBg,
                            ink = ext.pastelMintInk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6-Skill Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Communication Skill Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SkillBreakdownBars(
                            metrics = listOf(
                                "Fluency" to profile.fluencyScore.toInt(),
                                "Pronunciation" to profile.pronunciationScore.toInt(),
                                "Vocabulary" to profile.vocabularyScore.toInt(),
                                "Grammar" to profile.grammarScore.toInt(),
                                "Articulation" to profile.articulationScore.toInt(),
                                "Listening" to profile.listeningScore.toInt()
                            )
                        )
                    }
                }
            }

            // Quick Links
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        ProfileShortcutRow(
                            icon = Icons.Default.Mic,
                            title = "Saved Recordings (${allRecordings.size})",
                            subtitle = "View & play all audio and video clips across the app",
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("profile_overview_recordings_link")
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.Default.BarChart,
                            title = "Progress Analytics",
                            subtitle = "Detailed trends, scores & milestones",
                            onClick = { onNavigate("analytics") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications & Alerts",
                            subtitle = "Practice reminders, streak alerts & preferences",
                            onClick = onOpenNotifications
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.Default.Settings,
                            title = "App Settings",
                            subtitle = "Theme, voice speed, tutorials & privacy",
                            onClick = { onNavigate("settings") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ProfileShortcutRow(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "Sign Out",
                            subtitle = "Switch Google account safely",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showSignOutConfirm = true }
                        )
                    }
                }
            }
        }
        }
    }
}
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    bg: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
