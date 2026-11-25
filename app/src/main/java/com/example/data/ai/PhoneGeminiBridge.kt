package com.example.data.ai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.core.logging.AppLogger
import com.example.data.model.VocabSeed
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private const val TAG = "PhoneGeminiBridge"
private const val PREFS_NAME = "speakcraft_phone_gemini_bridge"
private const val KEY_PENDING_HANDOFF = "pending_studio_handoff_json"
private const val KEY_STUDIO_FEEDBACK_LIST = "studio_gemini_feedback_list_json"
private const val OUTBOUND_STUDIO_PROMPT_MARKER = "SpeakCraft Record & Review Studio"
private const val HANDOFF_WINDOW_MS = 12 * 60 * 60 * 1000L

enum class StudioTaskType(val label: String, val badge: String) {
    DAILY_VIDEO("1. Daily Video Recording", "Daily Free Speak"),
    SELECT_TOPIC("2. Select Topics", "Topic Drill")
}

@Serializable
data class PendingStudioGeminiHandoff(
    val targetRoute: String = "speaking_lab",
    val taskTypeName: String = StudioTaskType.DAILY_VIDEO.name,
    val taskTitle: String = "",
    val mediaLabel: String = "",
    val isVideo: Boolean = false,
    val sentAtMs: Long = 0L
)

@Serializable
data class StudioGeminiFeedbackEntry(
    val id: String = UUID.randomUUID().toString(),
    val taskTypeLabel: String = StudioTaskType.DAILY_VIDEO.label,
    val taskTitle: String = "",
    val mediaLabel: String = "",
    val isVideo: Boolean = false,
    val feedbackText: String = "",
    val source: String = "Gemini App",
    val receivedAtMs: Long = 0L
)

/**
 * Snapshot of Gemini app & Android Assistant integration available on this phone.
 */
data class GeminiAppConnectionStatus(
    val geminiAppInstalled: Boolean = false,
    val googleAppInstalled: Boolean = false,
    val assistantVoiceAvailable: Boolean = false,
    val aiCoreServiceInstalled: Boolean = false,
    val shareTargetEnabled: Boolean = true,
    val statusLabel: String = "Checking phone Gemini integration...",
    val detailMessage: String = ""
) {
    val isAnyGeminiPathAvailable: Boolean
        get() = geminiAppInstalled || assistantVoiceAvailable || googleAppInstalled
}

/**
 * Outcome of attempting to launch or send a prompt/media clip to the Gemini app on the phone.
 */
data class GeminiLaunchResult(
    val launched: Boolean,
    val targetUsed: String,
    val copiedToClipboard: Boolean,
    val mediaAttached: Boolean = false,
    val userMessage: String
)

/**
 * Inbound deep-link or shared text received by SpeakCraft from the Gemini app or Google Assistant.
 */
data class InboundGeminiLink(
    val targetRoute: String? = null,
    val sharedText: String? = null,
    val launchNonce: Long = 0L
)

/**
 * Connects SpeakCraft directly with the Google Gemini app installed on the user's phone via
 * Android Intents, MediaStore URIs, Deep Links, and Share Targets — keeping SpeakCraft 100% free of
 * embedded Cloud AI SDKs or API keys while letting users submit recordings and receive Gemini feedback.
 */
object PhoneGeminiBridge {

    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    const val AICORE_PACKAGE = "com.google.android.aicore"
    private const val PLAY_STORE_GEMINI_URI = "market://details?id=$GEMINI_PACKAGE"
    private const val WEB_STORE_GEMINI_URI = "https://play.google.com/store/apps/details?id=$GEMINI_PACKAGE"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val ALLOWED_ROUTES = setOf(
        "dashboard",
        "daily_practice",
        "vocabulary",
        "coach_practice",
        "speaking_lab",
        "conversation",
        "ai_mentor",
        "learning_paths",
        "settings"
    )

    fun inspectConnectionStatus(context: Context): GeminiAppConnectionStatus {
        val pm = context.packageManager
        val geminiInstalled = isPackageInstalled(pm, GEMINI_PACKAGE)
        val googleInstalled = isPackageInstalled(pm, GOOGLE_APP_PACKAGE)
        val aiCoreInstalled = isPackageInstalled(pm, AICORE_PACKAGE)
        val assistOk = canResolveIntent(pm, Intent(Intent.ACTION_ASSIST)) ||
            canResolveIntent(pm, Intent(Intent.ACTION_VOICE_COMMAND))

        val statusLabel = when {
            geminiInstalled -> "Gemini App Connected (Installed on Phone)"
            assistOk || googleInstalled -> "Google Assistant / Gemini Voice Ready"
            else -> "Gemini App Not Detected"
        }

        val detail = buildString {
            when {
                geminiInstalled -> append("Google Gemini app is installed on your phone. You can send coaching prompts, recorded videos/audio, and roleplay topics directly to Gemini, or share feedback from Gemini back into SpeakCraft.")
                assistOk || googleInstalled -> append("Standalone Gemini app is not installed yet, so prompts and clips will route via your phone's default Assistant / Share integration.")
                else -> append("Install the Google Gemini app from Google Play to exchange prompts and recording reviews directly from SpeakCraft.")
            }
            if (aiCoreInstalled) {
                append(" Android System AICore runtime is also present on this device.")
            }
        }

        return GeminiAppConnectionStatus(
            geminiAppInstalled = geminiInstalled,
            googleAppInstalled = googleInstalled,
            assistantVoiceAvailable = assistOk,
            aiCoreServiceInstalled = aiCoreInstalled,
            shareTargetEnabled = true,
            statusLabel = statusLabel,
            detailMessage = detail
        )
    }

    /**
     * Opens the Google Gemini app directly on the phone (or falls back to Voice Assistant / Play Store).
     */
    fun openGeminiApp(context: Context): GeminiLaunchResult {
        val pm = context.packageManager

        pm.getLaunchIntentForPackage(GEMINI_PACKAGE)?.let { launchIntent ->
            prepareActivityFlags(context, launchIntent)
            if (startSafely(context, launchIntent)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = "Gemini App",
                    copiedToClipboard = false,
                    userMessage = "Opened Google Gemini app on your phone."
                )
            }
        }

        for (action in listOf(Intent.ACTION_VOICE_COMMAND, Intent.ACTION_ASSIST)) {
            val assistIntent = Intent(action).also { prepareActivityFlags(context, it) }
            if (canResolveIntent(pm, assistIntent) && startSafely(context, assistIntent)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = "Phone Assistant",
                    copiedToClipboard = false,
                    userMessage = "Opened your phone's Assistant / Gemini voice overlay."
                )
            }
        }

        pm.getLaunchIntentForPackage(GOOGLE_APP_PACKAGE)?.let { googleLaunch ->
            prepareActivityFlags(context, googleLaunch)
            if (startSafely(context, googleLaunch)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = "Google App",
                    copiedToClipboard = false,
                    userMessage = "Opened Google app."
                )
            }
        }

        return openGeminiPlayStore(context)
    }

    /**
     * Launches Gemini Live / voice assistant directly for hands-free spoken interaction.
     */
    fun launchGeminiLiveVoice(context: Context): GeminiLaunchResult {
        val pm = context.packageManager
        val geminiInstalled = isPackageInstalled(pm, GEMINI_PACKAGE)

        val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            if (geminiInstalled) setPackage(GEMINI_PACKAGE)
            prepareActivityFlags(context, this)
        }
        if (canResolveIntent(pm, voiceIntent) && startSafely(context, voiceIntent)) {
            return GeminiLaunchResult(
                launched = true,
                targetUsed = if (geminiInstalled) "Gemini Voice" else "Voice Assistant",
                copiedToClipboard = false,
                userMessage = "Launched Gemini voice assistant on your phone."
            )
        }

        val assistIntent = Intent(Intent.ACTION_ASSIST).also {
            if (geminiInstalled) it.setPackage(GEMINI_PACKAGE)
            prepareActivityFlags(context, it)
        }
        if (canResolveIntent(pm, assistIntent) && startSafely(context, assistIntent)) {
            return GeminiLaunchResult(
                launched = true,
                targetUsed = "Assistant Overlay",
                copiedToClipboard = false,
                userMessage = "Launched Gemini assistant overlay."
            )
        }

        return openGeminiApp(context)
    }

    /**
     * Sends [promptText] explicitly triggered by the user to the Google Gemini app installed on the phone.
     * Also copies [promptText] to the clipboard as a backup so users can paste in 1 tap if an app build opens the launcher.
     */
    fun sendPromptToGeminiApp(
        context: Context,
        promptText: String,
        title: String = "Practice with Gemini"
    ): GeminiLaunchResult {
        val safePrompt = promptText.trim().take(2500)
        if (safePrompt.isBlank()) {
            return openGeminiApp(context)
        }

        val copied = copyToClipboard(context, title, safePrompt)
        val pm = context.packageManager

        if (isPackageInstalled(pm, GEMINI_PACKAGE)) {
            val directShare = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, safePrompt)
                setPackage(GEMINI_PACKAGE)
                prepareActivityFlags(context, this)
            }
            if (canResolveIntent(pm, directShare) && startSafely(context, directShare)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = "Gemini App Share",
                    copiedToClipboard = copied,
                    userMessage = "Sent prompt to Google Gemini app on your phone."
                )
            }

            pm.getLaunchIntentForPackage(GEMINI_PACKAGE)?.let { launchIntent ->
                prepareActivityFlags(context, launchIntent)
                if (startSafely(context, launchIntent)) {
                    return GeminiLaunchResult(
                        launched = true,
                        targetUsed = "Gemini App",
                        copiedToClipboard = copied,
                        userMessage = if (copied) {
                            "Opened Gemini app — prompt copied to clipboard so you can paste right away."
                        } else {
                            "Opened Gemini app on your phone."
                        }
                    )
                }
            }
        }

        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, safePrompt)
            },
            title
        ).also { prepareActivityFlags(context, it) }

        if (canResolveIntent(pm, chooser) && startSafely(context, chooser)) {
            return GeminiLaunchResult(
                launched = true,
                targetUsed = "App Share Chooser",
                copiedToClipboard = copied,
                userMessage = if (copied) {
                    "Choose Gemini in the share sheet (prompt also copied to clipboard)."
                } else {
                    "Choose Gemini from the share sheet."
                }
            )
        }

        return openGeminiApp(context)
    }

    /**
     * Submits a Record & Review Studio video or audio recording (from Task 1: Daily Video Recording or
     * Task 2: Select Topics) along with structured coaching prompt & transcript directly to the Gemini app
     * installed on the user's phone, and records a pending return route back into `speaking_lab`.
     */
    fun sendStudioRecordingToGeminiApp(
        context: Context,
        mediaFile: File?,
        isVideo: Boolean,
        taskType: StudioTaskType,
        taskTitle: String,
        transcript: String = "",
        durationSeconds: Int = 0
    ): GeminiLaunchResult {
        val cleanTitle = taskTitle.trim().ifBlank {
            if (taskType == StudioTaskType.DAILY_VIDEO) "Daily Video Recording (Open Speak)" else "Selected Topic Recording"
        }
        val prompt = buildStudioReviewPrompt(
            taskType = taskType,
            taskTitle = cleanTitle,
            isVideo = isVideo,
            transcript = transcript,
            durationSeconds = durationSeconds
        )
        val copied = copyToClipboard(context, "SpeakCraft Studio Review", prompt)
        val mediaLabel = buildString {
            append(if (isVideo) "Video" else "Audio")
            if (durationSeconds > 0) append(" (${durationSeconds}s)")
        }

        savePendingStudioHandoff(
            context,
            PendingStudioGeminiHandoff(
                targetRoute = "speaking_lab",
                taskTypeName = taskType.name,
                taskTitle = cleanTitle,
                mediaLabel = mediaLabel,
                isVideo = isVideo,
                sentAtMs = System.currentTimeMillis()
            )
        )

        val validFile = mediaFile?.takeIf { it.exists() && it.length() > 0L }
        val mimeType = resolveMediaMimeType(validFile, isVideo)
        val sharedUri = validFile?.let { exportMediaToMediaStore(context, it, isVideo, mimeType, cleanTitle) }
        val pm = context.packageManager
        val geminiInstalled = isPackageInstalled(pm, GEMINI_PACKAGE)

        if (sharedUri != null) {
            val directMediaIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, sharedUri)
                putExtra(Intent.EXTRA_SUBJECT, "SpeakCraft ${taskType.label}: $cleanTitle")
                putExtra(Intent.EXTRA_TEXT, prompt)
                clipData = ClipData.newUri(context.contentResolver, "SpeakCraft Recording", sharedUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (geminiInstalled) setPackage(GEMINI_PACKAGE)
                prepareActivityFlags(context, this)
            }
            if (canResolveIntent(pm, directMediaIntent) && startSafely(context, directMediaIntent)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = if (geminiInstalled) "Gemini App Media Share" else "Media Share",
                    copiedToClipboard = copied,
                    mediaAttached = true,
                    userMessage = "Submitted your ${if (isVideo) "video" else "audio"} & coaching prompt to Gemini app. Copy or Share Gemini's reply back here!"
                )
            }

            val chooserMedia = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, sharedUri)
                    putExtra(Intent.EXTRA_SUBJECT, "SpeakCraft ${taskType.label}: $cleanTitle")
                    putExtra(Intent.EXTRA_TEXT, prompt)
                    clipData = ClipData.newUri(context.contentResolver, "SpeakCraft Recording", sharedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Submit Recording to Gemini App"
            ).also { prepareActivityFlags(context, it) }

            if (canResolveIntent(pm, chooserMedia) && startSafely(context, chooserMedia)) {
                return GeminiLaunchResult(
                    launched = true,
                    targetUsed = "Media Share Chooser",
                    copiedToClipboard = copied,
                    mediaAttached = true,
                    userMessage = "Choose Gemini to submit your ${if (isVideo) "video" else "audio"} (review prompt also copied to clipboard). Share or paste Gemini's reply back here!"
                )
            }
        }

        // Fallback if the device only routes text/plain Intents cleanly to Gemini
        val textRes = sendPromptToGeminiApp(
            context = context,
            promptText = prompt,
            title = "SpeakCraft ${taskType.label}: $cleanTitle"
        )
        return textRes.copy(
            mediaAttached = sharedUri != null,
            userMessage = buildString {
                append(textRes.userMessage)
                if (sharedUri != null) {
                    append(" Clip saved to gallery for attachment if needed.")
                }
                append(" Copy or Share Gemini's feedback back into Studio!")
            }
        )
    }

    /**
     * Parses inbound deep links (`speakcraft://...`) and shared text (`ACTION_SEND` from Gemini app)
     * received by `MainActivity`. If a recent Record & Review Studio submission is waiting for
     * feedback, shared text from Gemini routes directly back to `"speaking_lab"`.
     */
    fun parseInboundIntent(
        intent: Intent?,
        context: Context? = null
    ): InboundGeminiLink? {
        if (intent == null) return null

        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.take(3500)
            if (!shared.isNullOrBlank()) {
                val pendingStudio = context?.let { getPendingStudioHandoff(it) }
                val routeToStudio = pendingStudio != null &&
                    pendingStudio.targetRoute == "speaking_lab" &&
                    (System.currentTimeMillis() - pendingStudio.sentAtMs) in 0L..HANDOFF_WINDOW_MS
                if (routeToStudio) {
                    clearPendingStudioHandoff(context!!)
                }
                return InboundGeminiLink(
                    targetRoute = if (routeToStudio) "speaking_lab" else "ai_mentor",
                    sharedText = shared,
                    launchNonce = System.currentTimeMillis()
                )
            }
        }

        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null && uri.scheme.equals("speakcraft", ignoreCase = true)) {
                val rawRoute = uri.host?.takeIf { it.isNotBlank() }
                    ?: uri.pathSegments?.firstOrNull()
                    ?: "dashboard"
                val route = when (rawRoute.lowercase()) {
                    "app", "home" -> uri.pathSegments?.firstOrNull()?.lowercase() ?: "dashboard"
                    else -> rawRoute.lowercase()
                }
                val safeRoute = if (route in ALLOWED_ROUTES) route else "dashboard"
                val prompt = uri.getQueryParameter("feedback")?.trim()?.take(3500)
                    ?: uri.getQueryParameter("prompt")?.trim()?.take(1500)
                    ?: uri.getQueryParameter("topic")?.trim()?.take(1500)
                return InboundGeminiLink(
                    targetRoute = safeRoute,
                    sharedText = prompt?.takeIf { it.isNotBlank() },
                    launchNonce = System.currentTimeMillis()
                )
            }
        }
        return null
    }

    // -----------------------------------------------------------------------
    // Studio Pending Handoff & Returned Gemini Feedback Persistence (Local)
    // -----------------------------------------------------------------------

    fun savePendingStudioHandoff(context: Context, handoff: PendingStudioGeminiHandoff) {
        runCatching {
            prefs(context).edit()
                .putString(KEY_PENDING_HANDOFF, json.encodeToString(handoff))
                .apply()
        }.onFailure { AppLogger.w(TAG, "Unable to save pending studio handoff: ${it.message}") }
    }

    fun getPendingStudioHandoff(context: Context): PendingStudioGeminiHandoff? = runCatching {
        val raw = prefs(context).getString(KEY_PENDING_HANDOFF, null) ?: return null
        val decoded = json.decodeFromString(PendingStudioGeminiHandoff.serializer(), raw)
        if ((System.currentTimeMillis() - decoded.sentAtMs) > HANDOFF_WINDOW_MS) null else decoded
    }.getOrNull()

    fun clearPendingStudioHandoff(context: Context) {
        runCatching {
            prefs(context).edit().remove(KEY_PENDING_HANDOFF).apply()
        }
    }

    fun loadStudioFeedbackHistory(context: Context): List<StudioGeminiFeedbackEntry> = runCatching {
        val raw = prefs(context).getString(KEY_STUDIO_FEEDBACK_LIST, null) ?: return emptyList()
        json.decodeFromString<List<StudioGeminiFeedbackEntry>>(raw)
    }.getOrDefault(emptyList())

    fun saveStudioFeedbackEntry(
        context: Context,
        entry: StudioGeminiFeedbackEntry
    ): List<StudioGeminiFeedbackEntry> {
        val current = loadStudioFeedbackHistory(context)
        val cleanText = entry.feedbackText.trim()
        if (cleanText.isBlank()) return current
        val normalized = entry.copy(
            feedbackText = cleanText,
            receivedAtMs = entry.receivedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        val updated = (listOf(normalized) + current.filterNot {
            it.feedbackText.equals(cleanText, ignoreCase = true) &&
                it.taskTitle.equals(normalized.taskTitle, ignoreCase = true)
        }).take(25)
        runCatching {
            prefs(context).edit()
                .putString(KEY_STUDIO_FEEDBACK_LIST, json.encodeToString(updated))
                .apply()
        }.onFailure { AppLogger.w(TAG, "Unable to persist studio Gemini feedback: ${it.message}") }
        clearPendingStudioHandoff(context)
        return updated
    }

    fun readGeminiFeedbackFromClipboard(context: Context): String? = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim() ?: return null
        if (text.isBlank()) return null
        if (text.contains(OUTBOUND_STUDIO_PROMPT_MARKER)) return null
        text.take(3500)
    }.getOrNull()

    // -----------------------------------------------------------------------
    // Privacy-Safe Coaching Prompt Builders (explicitly triggered by user)
    // -----------------------------------------------------------------------

    fun buildStudioReviewPrompt(
        taskType: StudioTaskType,
        taskTitle: String,
        isVideo: Boolean,
        transcript: String,
        durationSeconds: Int = 0
    ): String {
        val mediaWord = if (isVideo) "video recording" else "audio recording"
        val cleanTitle = taskTitle.trim().ifBlank {
            if (taskType == StudioTaskType.DAILY_VIDEO) "Daily Free Video Recording" else "Selected Topic Recording"
        }
        val cleanTranscript = transcript.trim()
        return buildString {
            append("$OUTBOUND_STUDIO_PROMPT_MARKER — Please review my $mediaWord.\n")
            when (taskType) {
                StudioTaskType.DAILY_VIDEO -> {
                    append("Task: 1. Daily Video Recording (open free speaking — user spoke about whatever they wanted today).\n")
                    if (cleanTitle != "Daily Video Recording (Open Speak)" && cleanTitle != "Daily Free Video Recording") {
                        append("Topic / Note: $cleanTitle\n")
                    }
                }
                StudioTaskType.SELECT_TOPIC -> {
                    append("Task: 2. Selected Topic Practice\n")
                    append("Topic Prompt: $cleanTitle\n")
                }
            }
            if (durationSeconds > 0) {
                append("Duration: ${durationSeconds}s\n")
            }
            if (cleanTranscript.isNotBlank()) {
                append("\nSpoken Transcript:\n\"$cleanTranscript\"\n")
            } else {
                append("\n(Listen/watch the attached $mediaWord directly — live transcript was not captured.)\n")
            }
            append("\nPlease give clear, structured coaching feedback covering:\n")
            append("1. Top Strengths (delivery, body language/presence if video, pacing, tone)\n")
            append("2. Specific Mistakes or Habits to Fix\n")
            append("3. Clarity, Conciseness & Structure Tips\n")
            append("4. Stronger Rewritten Take (30-45s example I can practice next)")
        }
    }

    fun buildMentorPrompt(
        userQuestionOrDraft: String,
        userLevel: String
    ): String {
        val topic = userQuestionOrDraft.trim()
        return if (topic.isBlank()) {
            "Hi Gemini, I'm practicing spoken English and communication at $userLevel level in SpeakCraft. " +
                "Give me a realistic 60-second impromptu speaking prompt using the PREP framework (Point, Reason, Example, Point) and coach me."
        } else {
            "Hi Gemini, I'm practicing communication at $userLevel level. Help me coach and refine this topic or draft for spoken delivery:\n\"$topic\""
        }
    }

    fun buildConversationRoleplayPrompt(
        scenario: String,
        userLevel: String
    ): String =
        "Hi Gemini, let's practice a spoken conversation roleplay for \"$scenario\" at a $userLevel communication level. " +
            "Play the partner, ask one short realistic question at a time, and give concise feedback on my clarity."

    fun buildVocabularyPrompt(
        seed: VocabSeed,
        userLevel: String
    ): String =
        "Hi Gemini, I'm learning the word \"${seed.word}\" (${seed.phonetic}: ${seed.meaning}) at $userLevel level. " +
            "Give me 2 natural spoken conversation examples and quiz me on using it naturally."

    private fun resolveMediaMimeType(file: File?, isVideo: Boolean): String {
        if (isVideo) return "video/mp4"
        return when (file?.extension?.lowercase()) {
            "wav" -> "audio/wav"
            "m4a", "mp4" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            else -> "audio/mp4"
        }
    }

    private fun exportMediaToMediaStore(
        context: Context,
        sourceFile: File,
        isVideo: Boolean,
        mimeType: String,
        title: String
    ): Uri? = runCatching {
        val resolver = context.contentResolver
        val ext = sourceFile.extension.takeIf { it.isNotBlank() } ?: if (isVideo) "mp4" else "m4a"
        val safeName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(24).ifBlank { "recording" }
        val fileName = "speakcraft_${safeName}_${System.currentTimeMillis()}.$ext"
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val baseDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/SpeakCraft")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: return null
        val copied = resolver.openOutputStream(uri)?.use { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
        }
        if (copied == null || copied <= 0L) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finalizeValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, finalizeValues, null, null)
        }
        uri
    }.onFailure { err ->
        AppLogger.w(TAG, "MediaStore share export skipped: ${err.message}")
    }.getOrNull()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun openGeminiPlayStore(context: Context): GeminiLaunchResult {
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_GEMINI_URI)).also {
            prepareActivityFlags(context, it)
        }
        if (canResolveIntent(context.packageManager, storeIntent) && startSafely(context, storeIntent)) {
            return GeminiLaunchResult(
                launched = true,
                targetUsed = "Play Store",
                copiedToClipboard = false,
                userMessage = "Gemini app isn't installed on this phone yet — opened Google Play Store."
            )
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_STORE_GEMINI_URI)).also {
            prepareActivityFlags(context, it)
        }
        val webLaunched = canResolveIntent(context.packageManager, webIntent) && startSafely(context, webIntent)
        return GeminiLaunchResult(
            launched = webLaunched,
            targetUsed = if (webLaunched) "Play Store Web" else "Unavailable",
            copiedToClipboard = false,
            userMessage = if (webLaunched) {
                "Opened Google Play listing for Google Gemini."
            } else {
                "Could not find Gemini app or Google Play on this device."
            }
        )
    }

    private fun copyToClipboard(context: Context, label: String, text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    }.getOrDefault(false)

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        true
    }.getOrDefault(false)

    private fun canResolveIntent(pm: PackageManager, intent: Intent): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())) != null
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }
    }.getOrDefault(false)

    private fun prepareActivityFlags(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun startSafely(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        AppLogger.w(TAG, "Unable to launch phone Gemini intent: ${t.message}")
        false
    }
}
