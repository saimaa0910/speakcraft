package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.ConversationSession
import com.example.data.model.DailyActivity
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import com.example.data.model.ProgramEnrollment
import com.example.data.model.RealWorldTaskRecord
import com.example.data.model.SpeakingSession
import com.example.data.model.SpeechFeedback
import com.example.data.model.StreakAndAnalyticsHelper
import com.example.data.model.UserLearningProgress
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
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

    fun observeIsAdmin(userId: String): Flow<Boolean> =
        observeAdminRole(userId).map { it != com.example.data.learning.AdminRole.NONE }

    /**
     * Verifies admin role from BOTH Firebase Auth custom claims (request.auth.token.admin / role)
     * and server-provisioned adminUsers/{uid} document. Never trusts client-writable fields.
     */
    fun observeAdminRole(userId: String): Flow<com.example.data.learning.AdminRole> = flow {
        val user = auth.currentUser
        val uid = user?.uid
        if (uid == null || uid != userId) {
            emit(com.example.data.learning.AdminRole.NONE)
            return@flow
        }
        val claimRole = runCatching {
            val token = user.getIdToken(false).await()
            val claims = token.claims
            val isClaimAdmin = claims["admin"] == true
            val roleStr = claims["role"] as? String
            when {
                !roleStr.isNullOrBlank() -> com.example.data.learning.AdminRole.fromString(roleStr)
                isClaimAdmin -> com.example.data.learning.AdminRole.SUPER_ADMIN
                else -> com.example.data.learning.AdminRole.NONE
            }
        }.getOrDefault(com.example.data.learning.AdminRole.NONE)

        emitAll(
            db.collection("adminUsers").document(uid)
                .snapshots()
                .map { snap ->
                    val docAdmin = snap.exists() && (snap.toObject(AdminUser::class.java)?.active == true)
                    val docRole = if (docAdmin) {
                        com.example.data.learning.AdminRole.fromString(snap.getString("role") ?: "SUPER_ADMIN")
                    } else com.example.data.learning.AdminRole.NONE
                    when {
                        claimRole != com.example.data.learning.AdminRole.NONE -> claimRole
                        else -> docRole
                    }
                }
                .catch { emit(claimRole) }
        )
    }

    fun observeActiveAnnouncements(): Flow<List<com.example.data.learning.AdminAnnouncement>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("adminAnnouncements")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.documents.map { doc ->
                        com.example.data.learning.AdminAnnouncement(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            message = doc.getString("message").orEmpty(),
                            targetAudience = doc.getString("targetAudience") ?: "ALL",
                            targetRoute = doc.getString("targetRoute") ?: "dashboard",
                            createdBy = doc.getString("createdBy").orEmpty(),
                            createdAtMs = doc.getLong("createdAtMs") ?: 0L,
                            active = doc.getBoolean("active") ?: true
                        )
                    }.sortedByDescending { it.createdAtMs }
                }
                .catch { emit(emptyList()) }
        )
    }

    fun observeAdminAuditLogs(): Flow<List<com.example.data.learning.AdminAuditRecord>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("adminAuditLogs")
                .orderBy("timestampMs", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.documents.map { doc ->
                        com.example.data.learning.AdminAuditRecord(
                            id = doc.id,
                            adminUid = doc.getString("adminUid").orEmpty(),
                            adminRole = doc.getString("adminRole").orEmpty(),
                            action = doc.getString("action").orEmpty(),
                            targetPath = doc.getString("targetPath").orEmpty(),
                            detail = doc.getString("detail").orEmpty(),
                            timestampMs = doc.getLong("timestampMs") ?: 0L
                        )
                    }
                }
                .catch { emit(emptyList()) }
        )
    }

    fun observeAdminUsersList(): Flow<List<AdminUser>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("adminUsers")
                .snapshots()
                .map { it.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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
            } else {
                docRef.update(
                    mapOf(
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun toggleProgramDay(programId: String, dayNumber: Long, totalDays: Int): Result<Unit> =
        runMutation(OperationType.UPDATE, "enrollments/$programId") { uid ->
            val safeId = sanitizeId(programId, 100)
            val safeTotal = totalDays.toLong().coerceIn(1L, 180L)
            val safeDay = dayNumber.coerceIn(1L, safeTotal)
            val docRef = db.collection("users").document(uid).collection("enrollments").document(safeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initialList = listOf(safeDay)
                val nextDay = (safeDay + 1L).coerceIn(1L, safeTotal)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "programId" to safeId,
                        "programTitle" to safeId,
                        "currentDay" to nextDay,
                        "completedDays" to initialList,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val currentList = (snap.get("completedDays") as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
                val updatedList = if (currentList.contains(safeDay)) currentList - safeDay else (currentList + safeDay).distinct().sorted().take(180)
                val nextDay = ((1L..safeTotal).firstOrNull { !updatedList.contains(it) } ?: safeTotal)
                docRef.update(
                    mapOf(
                        "completedDays" to updatedList,
                        "currentDay" to nextDay,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun syncProgramEnrollment(
        programId: String,
        programTitle: String,
        currentDay: Long,
        completedDays: List<Long>,
        active: Boolean
    ): Result<Unit> =
        runMutation(OperationType.WRITE, "enrollments/$programId") { uid ->
            val safeId = sanitizeId(programId, 100)
            val safeCurrentDay = currentDay.coerceIn(1L, 180L)
            val safeCompleted = completedDays.filter { it in 1L..180L }.distinct().sorted().take(180)
            val docRef = db.collection("users").document(uid).collection("enrollments").document(safeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "programId" to safeId,
                        "programTitle" to programTitle.take(200),
                        "currentDay" to safeCurrentDay,
                        "completedDays" to safeCompleted,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "currentDay" to safeCurrentDay,
                        "completedDays" to safeCompleted,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun setProgramActive(programId: String, active: Boolean): Result<Unit> =
        runMutation(OperationType.UPDATE, "enrollments/$programId") { uid ->
            val safeId = sanitizeId(programId, 100)
            val docRef = db.collection("users").document(uid).collection("enrollments").document(safeId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
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
                "level" to if (level in listOf("Beginner", "Elementary", "Intermediate", "Upper Intermediate", "Advanced")) level else "Intermediate",
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

    suspend fun saveSpeakingSession(session: SpeakingSession): Result<String> {
        if (session.evaluationStatus != "COMPLETED") {
            return Result.failure(
                IllegalStateException("Cannot save an unscored or failed evaluation session (${session.evaluationStatus}).")
            )
        }
        return runMutationWithReturn(OperationType.CREATE, "speakingSessions") { uid ->
            val sessionRef = db.collection("users").document(uid).collection("speakingSessions").document()
            val feedbackRef = db.collection("users").document(uid).collection("speechFeedback").document()

            val batch = db.batch()
            val sessionPayload = buildSpeakingSessionPayload(
                uid = uid,
                feedbackId = feedbackRef.id,
                session = session
            )
            batch.set(sessionRef, sessionPayload)

            val fbPayload = buildSpeechFeedbackPayload(
                uid = uid,
                sessionId = sessionRef.id,
                session = session
            )
            batch.set(feedbackRef, fbPayload)
            batch.commit().await()

            // Rolling weighted skill update ONLY for dimensions actually evaluated with valid evidence
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (snap.exists()) {
                val mins = (session.durationSeconds / 60L).coerceAtLeast(1L)
                val evaluatedKeys = session.evaluatedDimensionKeys.toSet()
                val legacyAll = evaluatedKeys.isEmpty()

                fun blendIfEvaluated(key: String, field: String, newVal: Long): Long? {
                    if (!legacyAll && key !in evaluatedKeys) return null
                    if (newVal <= 0L) return null
                    val oldVal = snap.getLong(field)
                    val base = oldVal ?: newVal
                    return ((base * 3L + newVal * 2L) / 5L).coerceIn(0L, 100L)
                }

                val updates = mutableMapOf<String, Any>(
                    "recordingsCount" to FieldValue.increment(1L),
                    "completedExercisesCount" to FieldValue.increment(1L),
                    "totalPracticeMinutes" to FieldValue.increment(mins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                blendIfEvaluated("fluency", "fluencyScore", session.fluencyScore)?.let { updates["fluencyScore"] = it }
                if (session.pronunciationAssessable) {
                    blendIfEvaluated("pronunciation", "pronunciationScore", session.pronunciationScore)?.let {
                        updates["pronunciationScore"] = it
                    }
                }
                blendIfEvaluated("vocabulary", "vocabularyScore", session.vocabularyScore)?.let { updates["vocabularyScore"] = it }
                blendIfEvaluated("grammar", "grammarScore", session.grammarScore)?.let { updates["grammarScore"] = it }
                blendIfEvaluated("delivery", "articulationScore", session.articulationScore)?.let { updates["articulationScore"] = it }

                userRef.update(updates).await()
            }
            sessionRef.id
        }
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

    @Suppress("UNUSED_PARAMETER")
    suspend fun saveConversationSession(
        scenario: String,
        difficultyLevel: String,
        turnCount: Long,
        summaryFeedback: String,
        transcriptPreview: String = ""
    ): Result<Unit> = runMutation(OperationType.CREATE, "conversationSessions") { uid ->
        val docRef = db.collection("users").document(uid).collection("conversationSessions").document()
        // T-04.2.1 (FND-SEC-05 / SEC-REQ-10): transcriptPreview is intentionally not uploaded to Firestore.
        val payload = buildConversationSessionPayload(
            uid = uid,
            scenario = scenario,
            difficultyLevel = difficultyLevel,
            turnCount = turnCount,
            summaryFeedback = summaryFeedback
        )
        docRef.set(payload).await()
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

    // ==================== ADMIN CONSOLE MUTATIONS (GATED BY adminUsers/{uid}) ====================

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

    suspend fun adminPublishAnnouncement(
        title: String,
        message: String,
        targetAudience: String = "ALL",
        targetRoute: String = "dashboard",
        adminRole: String = "SUPER_ADMIN"
    ): Result<Unit> = runMutation(OperationType.WRITE, "adminAnnouncements") { uid ->
        val docRef = db.collection("adminAnnouncements").document()
        val now = System.currentTimeMillis()
        docRef.set(
            mapOf(
                "title" to title.trim().take(160),
                "message" to message.trim().take(1200),
                "targetAudience" to targetAudience.take(40),
                "targetRoute" to targetRoute.take(80),
                "createdBy" to uid,
                "createdAtMs" to now,
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        adminLogAuditAction(adminRole, "PUBLISH_ANNOUNCEMENT", "adminAnnouncements/${docRef.id}", title.take(120))
    }

    suspend fun adminLogAuditAction(
        adminRole: String,
        action: String,
        targetPath: String,
        detail: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "adminAuditLogs") { uid ->
        val docRef = db.collection("adminAuditLogs").document()
        docRef.set(
            mapOf(
                "adminUid" to uid,
                "adminRole" to adminRole.take(40),
                "action" to action.take(120),
                "targetPath" to targetPath.take(200),
                "detail" to detail.take(1000),
                "timestampMs" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun adminUpsertRole(
        targetUserId: String,
        role: String,
        active: Boolean,
        actorRole: String
    ): Result<Unit> = runMutation(OperationType.WRITE, "adminUsers/$targetUserId") { _ ->
        val safeUid = sanitizeId(targetUserId, 128)
        val safeRole = if (role in listOf("SUPER_ADMIN", "CONTENT_ADMIN", "MODERATOR")) role else "MODERATOR"
        db.collection("adminUsers").document(safeUid).set(
            mapOf(
                "userId" to safeUid,
                "role" to safeRole,
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        adminLogAuditAction(actorRole, "UPDATE_ADMIN_ROLE", "adminUsers/$safeUid", "role=$safeRole active=$active")
    }

    /**
     * Deletes all user-owned Firestore subcollections, local media recordings, and the user's
     * root profile document, then attempts to delete the Firebase Authentication account.
     * Returns an actionable failure if Firebase requires recent re-authentication.
     */
    suspend fun deleteCurrentUserAccountAndData(): Result<Unit> = runMutation(OperationType.DELETE, "account_deletion") { uid ->
        val userDoc = db.collection("users").document(uid)
        val subcollections = listOf(
            "speakingSessions", "speechFeedback", "vocabulary", "dailyActivities",
            "learningProgress", "enrollments", "youtubeSources", "extractedTechniques",
            "conversationSessions", "realWorldTasks", "activityProgress", "activityAttempts",
            "savedActivities", "dailyPracticePlans", "notifications", "learningEvents",
            "vocabularyProgress", "quizAttempts", "learningSnapshots"
        )
        for (sub in subcollections) {
            val snap = runCatching { userDoc.collection(sub).get().await() }.getOrNull() ?: continue
            if (snap.isEmpty) continue
            snap.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    doc.getString("localMediaPath")?.takeIf { it.isNotBlank() }?.let { path ->
                        runCatching { File(path).takeIf { it.exists() }?.delete() }
                    }
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        }
        runCatching { userDoc.delete().await() }
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No authenticated user found.")
        currentUser.delete().await()
    }

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

    companion object {
        private val ALLOWED_SPEAKING_MODULES = setOf(
            "SPEAKING_LAB",
            "DAILY_CHALLENGE",
            "PRONUNCIATION",
            "SHADOWING",
            "GRAMMAR_SPEAKING",
            "PRESENTATION",
            "LESSON_PRACTICE"
        )

        private val ALLOWED_CONFIDENCE_LEVELS = setOf("High", "Medium", "Low")

        /**
         * Builds the Firestore payload for `speakingSessions`.
         *
         * T-04.2.1 (FND-SEC-05 / SEC-REQ-10): Intentionally omits `transcript` and `localMediaPath`
         * so raw speech transcripts and on-device file paths never leave the device.
         *
         * T-04.2.4 (GAP-20 / ISS-01): Strictly conforms to `firestore.rules` hasOnly allowlist and
         * normalizes `confidenceLevel` to ['High', 'Medium', 'Low'] so speaking-session saves succeed.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun buildSpeakingSessionPayload(
            uid: String,
            feedbackId: String,
            session: SpeakingSession,
            timestamp: Any = FieldValue.serverTimestamp()
        ): Map<String, Any> {
            val safeModule = if (session.moduleType in ALLOWED_SPEAKING_MODULES) {
                session.moduleType
            } else {
                "SPEAKING_LAB"
            }
            val safeConfidence = when (session.confidenceLevel) {
                "High" -> "High"
                "Low", "Low (Partial)" -> "Low"
                else -> "Medium"
            }
            return mapOf(
                "userId" to uid,
                "moduleType" to safeModule,
                "promptTitle" to session.promptTitle.take(300).ifBlank { "Speaking Practice" },
                "durationSeconds" to session.durationSeconds.coerceIn(0L, 3600L),
                "mediaType" to session.mediaType.take(64).ifBlank { null },
                "feedbackId" to feedbackId,
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
                "confidenceLevel" to safeConfidence,
                "audioQualityNote" to session.audioQualityNote.take(500).ifBlank { null },
                "fluencyScore" to session.fluencyScore.coerceIn(0L, 100L),
                "pronunciationScore" to session.pronunciationScore.coerceIn(0L, 100L),
                "vocabularyScore" to session.vocabularyScore.coerceIn(0L, 100L),
                "grammarScore" to session.grammarScore.coerceIn(0L, 100L),
                "articulationScore" to session.articulationScore.coerceIn(0L, 100L),
                "wordsPerMinute" to session.wordsPerMinute.coerceIn(0L, 400L),
                "sourceVideoUrl" to session.sourceVideoUrl.take(500).ifBlank { null },
                "createdAt" to timestamp,
                "updatedAt" to timestamp
            ).filterValues { it != null } as Map<String, Any>
        }

        /**
         * Builds the Firestore payload for `speechFeedback`.
         *
         * T-04.2.1 (FND-SEC-05 / SEC-REQ-10): Intentionally omits `transcript`.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun buildSpeechFeedbackPayload(
            uid: String,
            sessionId: String,
            session: SpeakingSession,
            timestamp: Any = FieldValue.serverTimestamp()
        ): Map<String, Any> {
            return mapOf(
                "userId" to uid,
                "sessionId" to sessionId,
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
                "createdAt" to timestamp,
                "updatedAt" to timestamp
            ).filterValues { it != null } as Map<String, Any>
        }

        /**
         * Builds the Firestore payload for `conversationSessions`.
         *
         * T-04.2.1 (FND-SEC-05 / SEC-REQ-10): Intentionally omits `transcriptPreview`.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun buildConversationSessionPayload(
            uid: String,
            scenario: String,
            difficultyLevel: String,
            turnCount: Long,
            summaryFeedback: String,
            timestamp: Any = FieldValue.serverTimestamp()
        ): Map<String, Any> {
            return mapOf(
                "userId" to uid,
                "scenario" to scenario.take(120),
                "difficultyLevel" to difficultyLevel.take(60),
                "turnCount" to turnCount.coerceIn(0L, 500L),
                "summaryFeedback" to summaryFeedback.take(4000).ifBlank { null },
                "createdAt" to timestamp,
                "updatedAt" to timestamp
            ).filterValues { it != null } as Map<String, Any>
        }
    }
}
