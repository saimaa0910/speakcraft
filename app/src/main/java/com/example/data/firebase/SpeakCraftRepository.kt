package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.ApplicationRelease
import com.example.data.model.ApplicationUpdateEvent
import com.example.data.model.ConversationSession
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DailyActivity
import com.example.data.model.ExtractedTechnique
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.data.model.ProgramEnrollment
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.TranscriptSegment
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.example.data.model.YouTubeSource
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File

class SpeakCraftRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String, maxLen: Int = 120): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "item" }.take(maxLen)

    private fun <T> wrapStreamError(
        flow: Flow<T>,
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = flow.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // ==================== USER STREAMS ====================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).snapshots().map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                },
                uid, OperationType.GET, "users/$uid"
            )
        )
    }

    fun observeSpeakingSessions(userId: String): Flow<List<SpeakingSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("speakingSessions")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { it.toObjects(SpeakingSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/speakingSessions"
            )
        )
    }

    fun observeSpeechFeedback(userId: String): Flow<List<SpeechFeedback>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("speechFeedback")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { it.toObjects(SpeechFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/speechFeedback"
            )
        )
    }

    fun observeVocabulary(userId: String): Flow<List<VocabularyEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("vocabulary")
                    .snapshots()
                    .map { it.toObjects(VocabularyEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/vocabulary"
            )
        )
    }

    fun observeDailyActivities(userId: String): Flow<List<DailyActivity>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("dailyActivities")
                    .snapshots()
                    .map { it.toObjects(DailyActivity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/dailyActivities"
            )
        )
    }

    fun observeLearningProgress(userId: String): Flow<List<UserLearningProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("learningProgress")
                    .snapshots()
                    .map { it.toObjects(UserLearningProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/learningProgress"
            )
        )
    }

    fun observeEnrollments(userId: String): Flow<List<ProgramEnrollment>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("enrollments")
                    .snapshots()
                    .map { it.toObjects(ProgramEnrollment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/enrollments"
            )
        )
    }

    fun observeYouTubeSources(userId: String): Flow<List<YouTubeSource>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("youtubeSources")
                    .snapshots()
                    .map { it.toObjects(YouTubeSource::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/youtubeSources"
            )
        )
    }

    fun observeExtractedTechniques(userId: String): Flow<List<ExtractedTechnique>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("extractedTechniques")
                    .snapshots()
                    .map { it.toObjects(ExtractedTechnique::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/extractedTechniques"
            )
        )
    }

    fun observeConversationSessions(userId: String): Flow<List<ConversationSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("conversationSessions")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { it.toObjects(ConversationSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/conversationSessions"
            )
        )
    }

    fun observeRealWorldTasks(userId: String): Flow<List<RealWorldTaskRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("realWorldTasks")
                    .snapshots()
                    .map { it.toObjects(RealWorldTaskRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) },
                uid, OperationType.LIST, "users/$uid/realWorldTasks"
            )
        )
    }

    // ==================== SHARED CATALOG, RELEASES & RBAC ====================

    fun observeIsAdmin(userId: String): Flow<Boolean> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null || uid != userId) {
            emit(false)
            return@flow
        }
        emitAll(
            db.collection("adminUsers").document(uid)
                .snapshots()
                .map { snap ->
                    snap.exists() && (snap.toObject(AdminUser::class.java)?.active == true)
                }
                .catch { emit(false) }
        )
    }

    fun observePublishedSharedTechniques(): Flow<List<ExtractedTechnique>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("extractedTechniques")
                .whereEqualTo("moderationStatus", "PUBLISHED")
                .snapshots()
                .map { it.toObjects(ExtractedTechnique::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observePublishedSharedLessons(): Flow<List<Lesson>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("lessons")
                .whereEqualTo("status", "PUBLISHED")
                .snapshots()
                .map { it.toObjects(Lesson::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observeSharedChallenges(): Flow<List<LearningChallenge>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("learningChallenges")
                .snapshots()
                .map { it.toObjects(LearningChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observeEnabledReleases(): Flow<List<ApplicationRelease>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("applicationReleases")
                .whereEqualTo("enabled", true)
                .snapshots()
                .map { it.toObjects(ApplicationRelease::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    suspend fun fetchEnabledReleasesOnce(): Result<List<ApplicationRelease>> = runCatching {
        if (auth.currentUser == null) return@runCatching emptyList()
        val snap = runCatching {
            db.collection("applicationReleases")
                .whereEqualTo("enabled", true)
                .get(Source.SERVER)
                .await()
        }.getOrElse {
            db.collection("applicationReleases")
                .whereEqualTo("enabled", true)
                .get()
                .await()
        }
        snap.toObjects(ApplicationRelease::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    fun observeAdminReleases(): Flow<List<ApplicationRelease>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("applicationReleases")
                .orderBy("versionCode", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(ApplicationRelease::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observePendingModerationTechniques(): Flow<List<ExtractedTechnique>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("extractedTechniques")
                .snapshots()
                .map { it.toObjects(ExtractedTechnique::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observeUserUpdateEvents(userId: String): Flow<List<ApplicationUpdateEvent>> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null || uid != userId) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("applicationUpdateEvents")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { it.toObjects(ApplicationUpdateEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    fun observeAllUpdateEventsForAdmin(): Flow<List<ApplicationUpdateEvent>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("applicationUpdateEvents")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { it.toObjects(ApplicationUpdateEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(emptyList()) }
        )
    }

    // ==================== USER MUTATIONS ====================

    suspend fun createOrUpdateProfile(
        displayName: String,
        level: String,
        primaryGoal: String,
        dailyGoalMinutes: Long,
        fluency: Long,
        pronunciation: Long,
        vocabulary: Long,
        grammar: Long,
        articulation: Long,
        listening: Long,
        confidenceSelf: Long
    ): Result<Unit> = runMutation(OperationType.WRITE, "users") { uid ->
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        val safeName = displayName.trim().ifBlank { "Speaker" }.take(100)
        val safeGoal = primaryGoal.trim().ifBlank { "Speak with vocal authority" }.take(200)
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "communicationLevel" to level,
                "primaryGoal" to safeGoal,
                "onboardingCompleted" to true,
                "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(5L, 240L),
                "streakDays" to 1L,
                "lastPracticeDate" to "",
                "totalPracticeMinutes" to 0L,
                "wordsLearnedCount" to 0L,
                "recordingsCount" to 0L,
                "completedExercisesCount" to 0L,
                "fluencyScore" to fluency.coerceIn(0L, 100L),
                "pronunciationScore" to pronunciation.coerceIn(0L, 100L),
                "vocabularyScore" to vocabulary.coerceIn(0L, 100L),
                "grammarScore" to grammar.coerceIn(0L, 100L),
                "articulationScore" to articulation.coerceIn(0L, 100L),
                "listeningScore" to listening.coerceIn(0L, 100L),
                "confidenceSelfRating" to confidenceSelf.coerceIn(0L, 100L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            docRef.update(
                mapOf(
                    "displayName" to safeName,
                    "communicationLevel" to level,
                    "primaryGoal" to safeGoal,
                    "onboardingCompleted" to true,
                    "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(5L, 240L),
                    "fluencyScore" to fluency.coerceIn(0L, 100L),
                    "pronunciationScore" to pronunciation.coerceIn(0L, 100L),
                    "vocabularyScore" to vocabulary.coerceIn(0L, 100L),
                    "grammarScore" to grammar.coerceIn(0L, 100L),
                    "articulationScore" to articulation.coerceIn(0L, 100L),
                    "listeningScore" to listening.coerceIn(0L, 100L),
                    "confidenceSelfRating" to confidenceSelf.coerceIn(0L, 100L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun seedAskVinhKnowledgeBaseIfEmpty(): Result<Unit> = runMutation(OperationType.WRITE, "extractedTechniques") { uid ->
        val existingTechs = db.collection("users").document(uid).collection("extractedTechniques").limit(1).get().await()
        if (existingTechs.isEmpty) {
            val batch = db.batch()
            val seenVideos = mutableSetOf<String>()
            CurriculumCatalog.askVinhKnowledgeBase.forEach { seed ->
                val safeTechId = sanitizeId(seed.id)
                val techRef = db.collection("users").document(uid).collection("extractedTechniques").document(safeTechId)
                batch.set(
                    techRef,
                    mapOf(
                        "userId" to uid,
                        "videoId" to seed.videoId.take(64),
                        "title" to seed.title.take(250),
                        "category" to seed.category.take(100),
                        "difficulty" to seed.difficulty,
                        "creatorAdvice" to seed.creatorAdvice.take(2000),
                        "aiExplanation" to seed.aiExplanation.take(2000),
                        "originalContext" to seed.originalContext.take(1000),
                        "instructions" to seed.instructions.take(2500),
                        "practicalExercise" to seed.practicalExercise.take(2000),
                        "expectedOutcome" to seed.expectedOutcome.take(800),
                        "suggestedDurationMinutes" to seed.suggestedDurationMinutes.coerceIn(1L, 180L),
                        "commonMistake" to seed.commonMistake.take(1000),
                        "correction" to seed.correction.take(1000),
                        "sourceVideoTitle" to seed.sourceVideoTitle.take(300),
                        "sourceVideoUrl" to seed.sourceVideoUrl.take(500),
                        "timestampLabel" to seed.timestampLabel.take(40),
                        "isCreatorDerived" to seed.isCreatorDerived,
                        "moderationStatus" to if (seed.verificationStatus == "VERIFIED") "PUBLISHED" else "PENDING_REVIEW",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val safeVideoId = sanitizeId(seed.videoId, 100)
                if (seenVideos.add(safeVideoId)) {
                    val srcRef = db.collection("users").document(uid).collection("youtubeSources").document(safeVideoId)
                    val status = if (seed.verificationStatus == "VERIFIED") "EXTRACTED" else "PENDING"
                    batch.set(
                        srcRef,
                        mapOf(
                            "userId" to uid,
                            "videoId" to safeVideoId,
                            "channelHandle" to "@askvinh",
                            "title" to seed.sourceVideoTitle.take(300),
                            "videoUrl" to seed.sourceVideoUrl.take(500),
                            "mainTopic" to seed.title.take(300),
                            "category" to seed.category.take(100),
                            "difficulty" to seed.difficulty,
                            "extractionStatus" to status,
                            "statusDetail" to if (status == "EXTRACTED") {
                                "Verified @askvinh public framework catalog"
                            } else {
                                "Awaiting live caption ingestion in YouTube Learning Library"
                            },
                            "transcriptAvailable" to (status == "EXTRACTED"),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
        }
    }

    suspend fun enrollInProgram(programId: String, programTitle: String): Result<Unit> =
        runMutation(OperationType.WRITE, "enrollments/$programId") { uid ->
            val safeId = sanitizeId(programId, 100)
            val docRef = db.collection("users").document(uid).collection("enrollments").document(safeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "programId" to safeId,
                        "programTitle" to programTitle.take(200),
                        "currentDay" to 1L,
                        "completedDays" to emptyList<Long>(),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun toggleProgramDay(programId: String, dayNumber: Long, totalDays: Int): Result<Unit> =
        runMutation(OperationType.UPDATE, "enrollments/$programId") { uid ->
            val safeId = sanitizeId(programId, 100)
            val docRef = db.collection("users").document(uid).collection("enrollments").document(safeId)
            val snap = docRef.get().await()
            val currentList = (snap.get("completedDays") as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
            val updatedList = if (currentList.contains(dayNumber)) currentList - dayNumber else (currentList + dayNumber).distinct().sorted()
            val nextDay = ((updatedList.maxOrNull() ?: 0L) + 1L).coerceIn(1L, totalDays.toLong().coerceAtLeast(1L))
            docRef.update(
                mapOf(
                    "completedDays" to updatedList,
                    "currentDay" to nextDay,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun saveDailyActivity(
        dateKey: String,
        completedSteps: List<String>,
        reflection: String,
        confidenceRating: Long,
        minutesSpent: Long
    ): Result<Unit> = runMutation(OperationType.WRITE, "dailyActivities/$dateKey") { uid ->
        val safeDate = sanitizeId(dateKey, 20)
        val docRef = db.collection("users").document(uid).collection("dailyActivities").document(safeDate)
        val snap = docRef.get().await()
        val prevDayMinutes = if (snap.exists()) (snap.getLong("minutesSpent") ?: 0L) else 0L
        val clampedMinutes = minutesSpent.coerceIn(0L, 600L)
        val minutesDelta = StreakAndAnalyticsHelper.computeMinutesDelta(prevDayMinutes, clampedMinutes)
        val cleanSteps = completedSteps.distinct().take(20)
        val payload = mutableMapOf<String, Any>(
            "completedStepIds" to cleanSteps,
            "confidenceRating" to confidenceRating.coerceIn(1L, 10L),
            "minutesSpent" to clampedMinutes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (reflection.isNotBlank()) {
            payload["reflectionText"] = reflection.take(2000)
        }
        if (!snap.exists()) {
            payload["userId"] = uid
            payload["dateKey"] = safeDate
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }

        val userRef = db.collection("users").document(uid)
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
            val prevLast = userSnap.getString("lastPracticeDate").orEmpty()
            val prevStreak = userSnap.getLong("streakDays") ?: 0L
            val newStreak = StreakAndAnalyticsHelper.computeUpdatedStreak(
                prevLastDateKey = prevLast,
                todayDateKey = safeDate,
                prevStreak = prevStreak,
                hasCompletedSteps = cleanSteps.isNotEmpty()
            )
            val updates = mutableMapOf<String, Any>(
                "lastPracticeDate" to safeDate,
                "streakDays" to newStreak,
                "confidenceSelfRating" to (confidenceRating * 10L).coerceIn(10L, 100L),
                "completedExercisesCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (minutesDelta > 0L) {
                updates["totalPracticeMinutes"] = FieldValue.increment(minutesDelta)
            }
            userRef.update(updates).await()
        }
    }

    suspend fun toggleLessonProgress(
        lessonId: String,
        moduleSlug: String,
        completed: Boolean,
        notes: String = "",
        score: Long? = null
    ): Result<Unit> = runMutation(OperationType.WRITE, "learningProgress/$lessonId") { uid ->
        val safeId = sanitizeId(lessonId, 120)
        val docRef = db.collection("users").document(uid).collection("learningProgress").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "lessonId" to safeId,
                "moduleSlug" to moduleSlug.take(100),
                "completed" to completed,
                "attemptsCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (score != null) payload["lastScore"] = score.coerceIn(0L, 100L)
            if (notes.isNotBlank()) payload["notes"] = notes.take(2000)
            docRef.set(payload).await()
        } else {
            val prevAttempts = snap.getLong("attemptsCount") ?: 0L
            val updates = mutableMapOf<String, Any>(
                "completed" to completed,
                "attemptsCount" to (prevAttempts + 1L).coerceAtMost(10000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (score != null) updates["lastScore"] = score.coerceIn(0L, 100L)
            if (notes.isNotBlank()) updates["notes"] = notes.take(2000)
            docRef.update(updates).await()
        }
    }

    suspend fun upsertVocabularyEntry(
        wordId: String,
        word: String,
        phonetic: String,
        meaning: String,
        level: String,
        exampleSentence: String,
        collocations: List<String>,
        personalNote: String,
        masteryLevel: Long,
        repetitions: Long,
        nextReviewEpochDay: Long,
        usageAssessed: Boolean
    ): Result<Unit> = runMutation(OperationType.WRITE, "vocabulary/$wordId") { uid ->
        val safeId = sanitizeId(wordId, 100)
        val docRef = db.collection("users").document(uid).collection("vocabulary").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "word" to word.take(80),
                "phonetic" to phonetic.take(120).ifBlank { null },
                "meaning" to meaning.take(600),
                "level" to if (level in listOf("Beginner", "Intermediate", "Advanced")) level else "Intermediate",
                "exampleSentence" to exampleSentence.take(600).ifBlank { null },
                "collocations" to collocations.take(15),
                "personalNote" to personalNote.take(1000).ifBlank { null },
                "masteryLevel" to masteryLevel.coerceIn(0L, 5L),
                "repetitions" to repetitions.coerceIn(0L, 1000L),
                "nextReviewEpochDay" to nextReviewEpochDay.coerceAtLeast(0L),
                "usageAssessed" to usageAssessed,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "wordsLearnedCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val updates = mapOf(
                "personalNote" to personalNote.take(1000).ifBlank { null },
                "masteryLevel" to masteryLevel.coerceIn(0L, 5L),
                "repetitions" to repetitions.coerceIn(0L, 1000L),
                "nextReviewEpochDay" to nextReviewEpochDay.coerceAtLeast(0L),
                "usageAssessed" to usageAssessed,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.update(updates).await()
        }
    }

    suspend fun saveSpeakingSession(session: SpeakingSession): Result<String> =
        runMutationWithReturn(OperationType.CREATE, "speakingSessions") { uid ->
            val sessionRef = db.collection("users").document(uid).collection("speakingSessions").document()
            val feedbackRef = db.collection("users").document(uid).collection("speechFeedback").document()
            val safeModule = if (session.moduleType in listOf(
                    "SPEAKING_LAB", "DAILY_CHALLENGE", "PRONUNCIATION", "SHADOWING", "GRAMMAR_SPEAKING", "PRESENTATION", "LESSON_PRACTICE"
                )
            ) session.moduleType else "SPEAKING_LAB"

            val batch = db.batch()
            val sessionPayload = mapOf(
                "userId" to uid,
                "moduleType" to safeModule,
                "promptTitle" to session.promptTitle.take(300).ifBlank { "Speaking Practice" },
                "durationSeconds" to session.durationSeconds.coerceIn(0L, 3600L),
                "localMediaPath" to session.localMediaPath.take(600).ifBlank { null },
                "mediaType" to session.mediaType.take(64).ifBlank { null },
                "feedbackId" to feedbackRef.id,
                "transcript" to session.transcript.take(15000).ifBlank { null },
                "overallAssessment" to session.overallAssessment.take(3000).ifBlank { "Assessment recorded." },
                "strengths" to session.strengths.take(15).map { it.take(500) },
                "mistakes" to session.mistakes.take(20).map { it.take(500) },
                "pronunciationNotes" to session.pronunciationNotes.take(20).map { it.take(500) },
                "betterAlternatives" to session.betterAlternatives.take(15).map { it.take(500) },
                "fillerWordsDetected" to session.fillerWordsDetected.take(25).map { it.take(500) },
                "repeatedExpressions" to session.repeatedExpressions.take(20).map { it.take(500) },
                "improvementSuggestions" to session.improvementSuggestions.take(15).map { it.take(500) },
                "recommendedExercises" to session.recommendedExercises.take(15).map { it.take(500) },
                "pronunciationAssessable" to session.pronunciationAssessable,
                "articulationClarity" to session.articulationClarity.take(800).ifBlank { null },
                "thoughtOrganization" to session.thoughtOrganization.take(800).ifBlank { null },
                "nextRecommendedActivity" to session.nextRecommendedActivity.take(400).ifBlank { null },
                "confidenceLevel" to if (session.confidenceLevel in listOf("High", "Medium", "Low")) session.confidenceLevel else "Medium",
                "audioQualityNote" to session.audioQualityNote.take(500).ifBlank { null },
                "fluencyScore" to session.fluencyScore.coerceIn(0L, 100L),
                "pronunciationScore" to session.pronunciationScore.coerceIn(0L, 100L),
                "vocabularyScore" to session.vocabularyScore.coerceIn(0L, 100L),
                "grammarScore" to session.grammarScore.coerceIn(0L, 100L),
                "articulationScore" to session.articulationScore.coerceIn(0L, 100L),
                "wordsPerMinute" to session.wordsPerMinute.coerceIn(0L, 400L),
                "sourceVideoUrl" to session.sourceVideoUrl.take(500).ifBlank { null },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            batch.set(sessionRef, sessionPayload)

            val fbPayload = mapOf(
                "userId" to uid,
                "sessionId" to sessionRef.id,
                "transcript" to session.transcript.take(15000).ifBlank { null },
                "strengths" to session.strengths.take(15).map { it.take(500) },
                "mistakes" to session.mistakes.take(20).map { it.take(500) },
                "recordingExamples" to session.pronunciationNotes.filter { it.startsWith("Quoted examples:") }.take(15),
                "betterAlternatives" to session.betterAlternatives.take(15).map { it.take(500) },
                "fillerWords" to session.fillerWordsDetected.take(25).map { it.take(500) },
                "repeatedExpressions" to session.repeatedExpressions.take(20).map { it.take(500) },
                "specificExercises" to session.recommendedExercises.take(15).map { it.take(500) },
                "pronunciationAssessable" to session.pronunciationAssessable,
                "pronunciationNote" to session.pronunciationNotes.joinToString("; ").take(800).ifBlank { null },
                "articulationClarity" to session.articulationClarity.take(800).ifBlank { null },
                "thoughtOrganization" to session.thoughtOrganization.take(800).ifBlank { null },
                "nextRecommendedActivity" to session.nextRecommendedActivity.take(400).ifBlank { null },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            batch.set(feedbackRef, fbPayload)
            batch.commit().await()

            // Rolling weighted skill update so single outliers don't overwrite steady progress
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (snap.exists()) {
                val mins = (session.durationSeconds / 60L).coerceAtLeast(1L)
                fun blend(oldVal: Long?, newVal: Long): Long {
                    val base = oldVal ?: newVal
                    return ((base * 3L + newVal * 2L) / 5L).coerceIn(0L, 100L)
                }
                val newPron = if (session.pronunciationAssessable) {
                    blend(snap.getLong("pronunciationScore"), session.pronunciationScore)
                } else {
                    (snap.getLong("pronunciationScore") ?: 60L).coerceIn(0L, 100L)
                }
                userRef.update(
                    mapOf(
                        "recordingsCount" to FieldValue.increment(1L),
                        "completedExercisesCount" to FieldValue.increment(1L),
                        "totalPracticeMinutes" to FieldValue.increment(mins),
                        "fluencyScore" to blend(snap.getLong("fluencyScore"), session.fluencyScore),
                        "pronunciationScore" to newPron,
                        "vocabularyScore" to blend(snap.getLong("vocabularyScore"), session.vocabularyScore),
                        "grammarScore" to blend(snap.getLong("grammarScore"), session.grammarScore),
                        "articulationScore" to blend(snap.getLong("articulationScore"), session.articulationScore),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            sessionRef.id
        }

    suspend fun deleteSpeakingSession(sessionId: String, feedbackId: String = "", localMediaPath: String = ""): Result<Unit> =
        runMutation(OperationType.DELETE, "speakingSessions/$sessionId") { uid ->
            val safeId = sanitizeId(sessionId, 128)
            val batch = db.batch()
            batch.delete(db.collection("users").document(uid).collection("speakingSessions").document(safeId))
            if (feedbackId.isNotBlank()) {
                batch.delete(db.collection("users").document(uid).collection("speechFeedback").document(sanitizeId(feedbackId, 128)))
            }
            batch.commit().await()
            if (localMediaPath.isNotBlank()) {
                runCatching { File(localMediaPath).takeIf { it.exists() }?.delete() }
            }
        }

    suspend fun saveConversationSession(
        scenario: String,
        difficultyLevel: String,
        turnCount: Long,
        summaryFeedback: String,
        transcriptPreview: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "conversationSessions") { uid ->
        val docRef = db.collection("users").document(uid).collection("conversationSessions").document()
        val payload = mapOf(
            "userId" to uid,
            "scenario" to scenario.take(120),
            "difficultyLevel" to difficultyLevel.take(60),
            "turnCount" to turnCount.coerceIn(0L, 500L),
            "summaryFeedback" to summaryFeedback.take(4000).ifBlank { null },
            "transcriptPreview" to transcriptPreview.take(4000).ifBlank { null },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        docRef.set(payload).await()
    }

    suspend fun saveYouTubeAnalysis(
        source: YouTubeSource,
        techniques: List<ExtractedTechnique>
    ): Result<Unit> = runMutation(OperationType.WRITE, "youtubeSources/${source.videoId}") { uid ->
        val safeSourceId = sanitizeId(source.videoId, 100)
        val batch = db.batch()
        val srcRef = db.collection("users").document(uid).collection("youtubeSources").document(safeSourceId)
        val srcPayload = mapOf(
            "userId" to uid,
            "videoId" to source.videoId.take(64),
            "channelHandle" to source.channelHandle.take(100).ifBlank { null },
            "playlistTitle" to source.playlistTitle.take(200).ifBlank { null },
            "publishedDate" to source.publishedDate.take(40).ifBlank { null },
            "durationSeconds" to source.durationSeconds.coerceIn(0L, 86400L),
            "title" to source.title.take(300).ifBlank { "Untitled Video" },
            "videoUrl" to source.videoUrl.take(500),
            "thumbnailUrl" to source.thumbnailUrl.take(500).ifBlank { null },
            "mainTopic" to source.mainTopic.take(300).ifBlank { null },
            "category" to source.category.take(100).ifBlank { "Public Speaking" },
            "difficulty" to if (source.difficulty in listOf("Beginner", "Intermediate", "Advanced")) source.difficulty else "Intermediate",
            "extractionStatus" to if (source.extractionStatus in listOf("EXTRACTED", "NO_CAPTIONS", "INACCESSIBLE", "PENDING")) source.extractionStatus else "EXTRACTED",
            "statusDetail" to source.statusDetail.take(500).ifBlank { null },
            "transcriptAvailable" to source.transcriptAvailable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        batch.set(srcRef, srcPayload)

        techniques.forEachIndexed { idx, tech ->
            val techId = sanitizeId("${safeSourceId}_t$idx", 120)
            val techRef = db.collection("users").document(uid).collection("extractedTechniques").document(techId)
            val techPayload = mapOf(
                "userId" to uid,
                "videoId" to source.videoId.take(64),
                "title" to tech.title.take(250).ifBlank { source.title.take(250) },
                "category" to tech.category.take(100).ifBlank { source.category.take(100) },
                "difficulty" to if (tech.difficulty in listOf("Beginner", "Intermediate", "Advanced")) tech.difficulty else "Intermediate",
                "creatorAdvice" to tech.creatorAdvice.take(2000).ifBlank { "Extracted from video." },
                "aiExplanation" to tech.aiExplanation.take(2000).ifBlank { null },
                "originalContext" to tech.originalContext.take(1000).ifBlank { null },
                "instructions" to tech.instructions.take(2500).ifBlank { null },
                "practicalExercise" to tech.practicalExercise.take(2000).ifBlank { "Practice aloud for 60 seconds." },
                "expectedOutcome" to tech.expectedOutcome.take(800).ifBlank { null },
                "suggestedDurationMinutes" to tech.suggestedDurationMinutes.coerceIn(1L, 180L),
                "commonMistake" to tech.commonMistake.take(1000).ifBlank { null },
                "correction" to tech.correction.take(1000).ifBlank { null },
                "sourceVideoTitle" to tech.sourceVideoTitle.take(300).ifBlank { source.title.take(300) },
                "sourceVideoUrl" to tech.sourceVideoUrl.take(500).ifBlank { source.videoUrl.take(500) },
                "timestampLabel" to tech.timestampLabel.take(40).ifBlank { null },
                "isCreatorDerived" to tech.isCreatorDerived,
                "moderationStatus" to "PUBLISHED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            batch.set(techRef, techPayload)
        }
        batch.commit().await()
    }

    suspend fun toggleRealWorldTask(
        taskId: String,
        taskTitle: String,
        completed: Boolean,
        reflection: String
    ): Result<Unit> = runMutation(OperationType.WRITE, "realWorldTasks/$taskId") { uid ->
        val safeId = sanitizeId(taskId, 100)
        val docRef = db.collection("users").document(uid).collection("realWorldTasks").document(safeId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "taskTitle" to taskTitle.take(300),
                "sourceVideoUrl" to "https://www.youtube.com/@askvinh",
                "completed" to completed,
                "reflection" to reflection.take(2000).ifBlank { null },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "completed" to completed,
                "reflection" to reflection.take(2000).ifBlank { null },
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.update(updates).await()
        }
    }

    suspend fun clearSpeakingHistory(): Result<Unit> =
        runMutation(OperationType.DELETE, "speakingSessions") { uid ->
            val snaps = db.collection("users").document(uid).collection("speakingSessions").get().await()
            val fbSnaps = db.collection("users").document(uid).collection("speechFeedback").get().await()
            val batch = db.batch()
            snaps.documents.forEach { doc ->
                doc.getString("localMediaPath")?.takeIf { it.isNotBlank() }?.let { path ->
                    runCatching { File(path).takeIf { it.exists() }?.delete() }
                }
                batch.delete(doc.reference)
            }
            fbSnaps.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }

    suspend fun logUpdateEvent(
        fromVersion: String,
        toVersion: String,
        channel: String,
        status: String,
        detail: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "applicationUpdateEvents") { uid ->
        val docRef = db.collection("applicationUpdateEvents").document()
        val safeStatus = if (status in listOf("CHECKED", "DOWNLOADED", "VERIFIED", "INSTALL_LAUNCHED", "INSTALLED", "FAILED", "ROLLED_BACK")) status else "CHECKED"
        db.collection("applicationUpdateEvents").document(docRef.id).set(
            mapOf(
                "userId" to uid,
                "fromVersion" to fromVersion.take(32),
                "toVersion" to toVersion.take(32),
                "channel" to (if (channel == "beta") "beta" else "stable"),
                "status" to safeStatus,
                "detail" to detail.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // ==================== ADMIN CONSOLE MUTATIONS (GATED BY adminUsers/{uid}) ====================

    suspend fun adminSyncVideoAndTechniques(
        source: YouTubeSource,
        segments: List<TranscriptSegment>,
        techniques: List<ExtractedTechnique>,
        moderationStatus: String = "PENDING_REVIEW"
    ): Result<Unit> = runMutation(OperationType.WRITE, "adminSync/${source.videoId}") { _ ->
        val safeVid = sanitizeId(source.videoId, 100)
        val batch = db.batch()

        val videoRef = db.collection("youtubeVideos").document(safeVid)
        batch.set(
            videoRef,
            mapOf(
                "videoId" to source.videoId.take(64),
                "channelId" to source.channelHandle.take(100),
                "playlistTitle" to source.playlistTitle.take(200),
                "title" to source.title.take(300),
                "publishedDate" to source.publishedDate.take(40),
                "durationSeconds" to source.durationSeconds.coerceIn(0L, 86400L),
                "videoUrl" to source.videoUrl.take(500),
                "thumbnailUrl" to source.thumbnailUrl.take(500),
                "transcriptAvailable" to source.transcriptAvailable,
                "processingStatus" to source.extractionStatus,
                "statusDetail" to source.statusDetail.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (segments.isNotEmpty()) {
            val transcriptRef = db.collection("videoTranscripts").document(safeVid)
            batch.set(
                transcriptRef,
                mapOf(
                    "videoId" to source.videoId.take(64),
                    "languageCode" to "en",
                    "segmentSummaries" to segments.take(40).map { "[${it.timestampLabel}] ${it.text}".take(240) },
                    "attributionNotice" to "Timestamps & excerpts grounded in creator captions.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        techniques.forEachIndexed { idx, tech ->
            val techId = sanitizeId("${safeVid}_admin_$idx", 120)
            val techRef = db.collection("extractedTechniques").document(techId)
            batch.set(
                techRef,
                mapOf(
                    "videoId" to source.videoId.take(64),
                    "title" to tech.title.take(250),
                    "category" to tech.category.take(100),
                    "difficulty" to tech.difficulty,
                    "creatorAdvice" to tech.creatorAdvice.take(2000),
                    "aiExplanation" to tech.aiExplanation.take(2000),
                    "originalContext" to tech.originalContext.take(1000),
                    "instructions" to tech.instructions.take(2500),
                    "practicalExercise" to tech.practicalExercise.take(2000),
                    "expectedOutcome" to tech.expectedOutcome.take(800),
                    "suggestedDurationMinutes" to tech.suggestedDurationMinutes.coerceIn(1L, 180L),
                    "commonMistake" to tech.commonMistake.take(1000),
                    "correction" to tech.correction.take(1000),
                    "sourceVideoTitle" to tech.sourceVideoTitle.take(300),
                    "sourceVideoUrl" to tech.sourceVideoUrl.take(500),
                    "timestampLabel" to tech.timestampLabel.take(40),
                    "isCreatorDerived" to tech.isCreatorDerived,
                    "moderationStatus" to moderationStatus,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun adminModerateTechnique(techniqueId: String, newStatus: String): Result<Unit> =
        runMutation(OperationType.UPDATE, "extractedTechniques/$techniqueId") { _ ->
            val safeId = sanitizeId(techniqueId, 120)
            val allowed = if (newStatus in listOf("PENDING_REVIEW", "APPROVED", "PUBLISHED", "REJECTED")) newStatus else "PENDING_REVIEW"
            db.collection("extractedTechniques").document(safeId).update(
                mapOf(
                    "moderationStatus" to allowed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun adminSaveSharedLesson(lesson: Lesson): Result<Unit> =
        runMutation(OperationType.WRITE, "lessons/${lesson.id}") { _ ->
            val safeId = sanitizeId(lesson.id.ifBlank { "lesson_${System.currentTimeMillis()}" }, 120)
            db.collection("lessons").document(safeId).set(
                mapOf(
                    "moduleSlug" to lesson.moduleSlug.take(100),
                    "techniqueId" to lesson.techniqueId.take(120),
                    "title" to lesson.title.take(250),
                    "objective" to lesson.objective.take(600),
                    "sourceVideoUrl" to lesson.sourceVideoUrl.take(500),
                    "timestampLabel" to lesson.timestampLabel.take(40),
                    "explanation" to lesson.explanation.take(3000),
                    "instructions" to lesson.instructions.take(15),
                    "example" to lesson.example.take(1500),
                    "practicePrompt" to lesson.practicePrompt.take(1500),
                    "nextLessonId" to lesson.nextLessonId.take(120),
                    "isCreatorDerived" to lesson.isCreatorDerived,
                    "status" to if (lesson.status in listOf("DRAFT", "PUBLISHED")) lesson.status else "PUBLISHED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun adminSaveSharedChallenge(challenge: LearningChallenge): Result<Unit> =
        runMutation(OperationType.WRITE, "learningChallenges/${challenge.id}") { _ ->
            val safeId = sanitizeId(challenge.id.ifBlank { "chal_${System.currentTimeMillis()}" }, 120)
            db.collection("learningChallenges").document(safeId).set(
                mapOf(
                    "title" to challenge.title.take(200),
                    "challengeType" to if (challenge.challengeType == "WEEKLY") "WEEKLY" else "DAILY",
                    "durationSeconds" to challenge.durationSeconds.coerceIn(15L, 600L),
                    "prompt" to challenge.prompt.take(1000),
                    "frameworkHint" to challenge.frameworkHint.take(400),
                    "sourceReference" to challenge.sourceReference.take(300),
                    "active" to challenge.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun adminPublishRelease(release: ApplicationRelease): Result<Unit> =
        runMutation(OperationType.WRITE, "applicationReleases") { _ ->
            val safeId = sanitizeId(release.id.ifBlank { "rel_${release.versionName.replace('.', '_')}_${release.versionCode}" }, 120)
            val docRef = db.collection("applicationReleases").document(safeId)
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "versionName" to release.versionName.trim(),
                "versionCode" to release.versionCode.coerceAtLeast(1L),
                "channel" to if (release.channel == "beta") "beta" else "stable",
                "releaseDate" to release.releaseDate.take(40),
                "releaseNotes" to release.releaseNotes.take(4000),
                "features" to release.features.take(30).map { it.take(500) },
                "bugFixes" to release.bugFixes.take(30).map { it.take(500) },
                "securityImprovements" to release.securityImprovements.take(30).map { it.take(500) },
                "downloadUrl" to release.downloadUrl.trim().take(600),
                "installerFileName" to release.installerFileName.take(200),
                "fileSizeBytes" to release.fileSizeBytes.coerceAtLeast(0L),
                "sha256Checksum" to release.sha256Checksum.trim().lowercase(),
                "signingCertSha256" to release.signingCertSha256.trim().lowercase(),
                "minSupportedVersion" to release.minSupportedVersion.trim(),
                "minSdk" to release.minSdk.coerceIn(21L, 40L),
                "mandatory" to release.mandatory,
                "enabled" to release.enabled,
                "supportedOs" to release.supportedOs.take(10),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
        }

    suspend fun adminToggleReleaseEnabled(release: ApplicationRelease): Result<Unit> =
        adminPublishRelease(release.copy(enabled = !release.enabled))

    private suspend fun runMutation(
        op: OperationType,
        pathSuffix: String,
        block: suspend (String) -> Unit
    ): Result<Unit> = try {
        val uid = requireUserId()
        block(uid)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        handleFirestoreError(e, op, "users/$uid/$pathSuffix")
        Result.failure(e)
    }

    private suspend fun <T> runMutationWithReturn(
        op: OperationType,
        pathSuffix: String,
        block: suspend (String) -> T
    ): Result<T> = try {
        val uid = requireUserId()
        Result.success(block(uid))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        handleFirestoreError(e, op, "users/$uid/$pathSuffix")
        Result.failure(e)
    }
}
