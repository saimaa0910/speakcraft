package com.example.data.ai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.core.logging.AppLogger
import com.example.data.model.VocabSeed

private const val TAG = "PhoneGeminiBridge"

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
 * Outcome of attempting to launch or send a prompt to the Gemini app on the phone.
 */
data class GeminiLaunchResult(
    val launched: Boolean,
    val targetUsed: String,
    val copiedToClipboard: Boolean,
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
 * Android Intents, App Actions, Deep Links, and text sharing — keeping SpeakCraft 100% free of
 * embedded Cloud AI SDKs or API keys while letting users collaborate seamlessly with Gemini on phone.
 */
object PhoneGeminiBridge {

    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    const val AICORE_PACKAGE = "com.google.android.aicore"
    private const val PLAY_STORE_GEMINI_URI = "market://details?id=$GEMINI_PACKAGE"
    private const val WEB_STORE_GEMINI_URI = "https://play.google.com/store/apps/details?id=$GEMINI_PACKAGE"

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
                geminiInstalled -> append("Google Gemini app is installed on your phone. You can send coaching prompts, vocabulary drills, and roleplay topics directly to Gemini, or share drafts from Gemini back into SpeakCraft.")
                assistOk || googleInstalled -> append("Standalone Gemini app is not installed yet, so prompts will route via your phone's default Assistant / Share integration.")
                else -> append("Install the Google Gemini app from Google Play to exchange prompts directly from SpeakCraft.")
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

        // 1. Standalone Google Gemini app
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

        // 2. Phone's default Digital Assistant / Gemini overlay
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

        // 3. Google App fallback
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

        // 4. Offer Play Store to install Gemini
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
        val safePrompt = promptText.trim().take(2000)
        if (safePrompt.isBlank()) {
            return openGeminiApp(context)
        }

        val copied = copyToClipboard(context, title, safePrompt)
        val pm = context.packageManager

        // 1. Targeted ACTION_SEND directly to the Gemini app package
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

            // Gemini installed, ACTION_SEND not handled directly -> launch Gemini with prompt copied
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

        // 2. Try Google App direct share fallback or system Share chooser
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
     * Parses inbound deep links (`speakcraft://...`) and shared text (`ACTION_SEND` from Gemini app)
     * received by `MainActivity`. Only whitelisted internal SpeakCraft routes are accepted.
     */
    fun parseInboundIntent(intent: Intent?): InboundGeminiLink? {
        if (intent == null) return null

        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.take(1500)
            if (!shared.isNullOrBlank()) {
                return InboundGeminiLink(
                    targetRoute = "ai_mentor",
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
                val prompt = uri.getQueryParameter("prompt")?.trim()?.take(1200)
                    ?: uri.getQueryParameter("topic")?.trim()?.take(1200)
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
    // Privacy-Safe Coaching Prompt Builders (explicitly triggered by user)
    // -----------------------------------------------------------------------

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
