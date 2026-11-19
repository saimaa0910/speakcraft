package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.DailyPracticePlanRecord
import com.example.data.activity.SavedActivityRecord
import com.example.data.activity.UserActivityProgress
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
import java.util.UUID

class ActivityFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun sanitizeId(raw: String, maxLen: Int = 120): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "item" }.take(maxLen)

    private fun <T> wrapStreamError(
        source: Flow<T>,
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = source.catch { error ->
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

    // ==================== REALTIME OBSERVATION STREAMS ====================

    fun observeActivityProgress(userId: String): Flow<List<UserActivityProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("activityProgress")
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(UserActivityProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                uid, OperationType.LIST, "users/$uid/activityProgress"
            )
        )
    }

    fun observeActivityAttempts(userId: String): Flow<List<ActivityAttemptRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("activityAttempts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(200)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(ActivityAttemptRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                uid, OperationType.LIST, "users/$uid/activityAttempts"
            )
        )
    }

    fun observeSavedActivities(userId: String): Flow<List<SavedActivityRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("savedActivities")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(SavedActivityRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    },
                uid, OperationType.LIST, "users/$uid/savedActivities"
            )
        )
    }

    fun observeDailyPracticePlan(userId: String, dateKey: String): Flow<DailyPracticePlanRecord?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val docId = sanitizeId(dateKey, 32)
        emitAll(
            wrapStreamError(
                db.collection("users").document(uid).collection("dailyPracticePlans").document(docId)
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) {
                            snap.toObject(DailyPracticePlanRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        } else null
                    },
                uid, OperationType.GET, "users/$uid/dailyPracticePlans/$docId"
            )
        )
    }

    // ==================== MUTATIONS ====================

    suspend fun saveActivityProgress(
        progress: UserActivityProgress,
        isUpdate: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        val docId = sanitizeId(progress.activityId)
        val ref = db.collection("users").document(uid).collection("activityProgress").document(docId)

        if (isUpdate) {
            val updateMap = mapOf(
                "skillCategory" to progress.skillCategory.take(64),
                "completed" to progress.completed,
                "attemptsCount" to progress.attemptsCount.coerceIn(0L, 10000L),
                "bestObjectiveScore" to progress.bestObjectiveScore.coerceIn(0L, 100L),
                "latestObjectiveScore" to progress.latestObjectiveScore.coerceIn(0L, 100L),
                "latestConfidenceSelfRating" to progress.latestConfidenceSelfRating.coerceIn(0L, 100L),
                "totalPracticeSeconds" to progress.totalPracticeSeconds.coerceIn(0L, 10_000_000L),
                "lastPracticedDateKey" to progress.lastPracticedDateKey.take(20),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.update(updateMap).await()
        } else {
            val createMap = mapOf(
                "userId" to uid,
                "activityId" to docId,
                "moduleId" to sanitizeId(progress.moduleId, 80),
                "skillCategory" to progress.skillCategory.take(64),
                "completed" to progress.completed,
                "attemptsCount" to progress.attemptsCount.coerceIn(0L, 10000L),
                "bestObjectiveScore" to progress.bestObjectiveScore.coerceIn(0L, 100L),
                "latestObjectiveScore" to progress.latestObjectiveScore.coerceIn(0L, 100L),
                "latestConfidenceSelfRating" to progress.latestConfidenceSelfRating.coerceIn(0L, 100L),
                "totalPracticeSeconds" to progress.totalPracticeSeconds.coerceIn(0L, 10_000_000L),
                "lastPracticedDateKey" to progress.lastPracticedDateKey.take(20),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(createMap).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/activityProgress/${progress.activityId}")
        Result.failure(e)
    }

    suspend fun recordActivityAttempt(attempt: ActivityAttemptRecord): Result<String> = try {
        val uid = requireUserId()
        val attemptId = "att_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ref = db.collection("users").document(uid).collection("activityAttempts").document(attemptId)

        val payload = mapOf(
            "userId" to uid,
            "activityId" to sanitizeId(attempt.activityId),
            "moduleId" to sanitizeId(attempt.moduleId, 80),
            "skillCategory" to attempt.skillCategory.take(64),
            "exerciseType" to attempt.exerciseType.take(64),
            "evaluatorName" to attempt.evaluatorName.take(64),
            "dateKey" to attempt.dateKey.take(20),
            "durationSeconds" to attempt.durationSeconds.coerceIn(0L, 14400L),
            "objectiveScore" to attempt.objectiveScore.coerceIn(0L, 100L),
            "confidenceSelfRating" to attempt.confidenceSelfRating.coerceIn(0L, 100L),
            "passed" to attempt.passed,
            "aiEvaluated" to attempt.aiEvaluated,
            "responseSummary" to attempt.responseSummary.take(600),
            "whatWentWell" to attempt.whatWentWell.take(5).map { it.take(300) },
            "specificMistakes" to attempt.specificMistakes.take(5).map { it.take(300) },
            "improvementSuggestions" to attempt.improvementSuggestions.take(5).map { it.take(300) },
            "nextPracticeTask" to attempt.nextPracticeTask.take(300),
            "recordedMediaPath" to attempt.recordedMediaPath.take(400),
            "wordsPerMinute" to attempt.wordsPerMinute.coerceIn(0L, 400L),
            "fillerCount" to attempt.fillerCount.coerceIn(0L, 500L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(payload).await()
        Result.success(attemptId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/activityAttempts")
        Result.failure(e)
    }

    suspend fun toggleSavedActivity(
        activityId: String,
        moduleId: String,
        title: String,
        currentlySaved: Boolean,
        note: String = ""
    ): Result<Boolean> = try {
        val uid = requireUserId()
        val docId = sanitizeId(activityId)
        val ref = db.collection("users").document(uid).collection("savedActivities").document(docId)
        if (currentlySaved) {
            ref.delete().await()
            Result.success(false)
        } else {
            val payload = mapOf(
                "userId" to uid,
                "activityId" to docId,
                "moduleId" to sanitizeId(moduleId, 80),
                "title" to title.take(140),
                "note" to note.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/savedActivities/$activityId")
        Result.failure(e)
    }

    suspend fun saveDailyPracticePlan(
        record: DailyPracticePlanRecord,
        isUpdate: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        val docId = sanitizeId(record.dateKey, 32)
        val ref = db.collection("users").document(uid).collection("dailyPracticePlans").document(docId)

        if (isUpdate) {
            val updateMap = mapOf(
                "targetMinutes" to record.targetMinutes.coerceIn(10L, 120L),
                "plannedActivityIds" to record.plannedActivityIds.take(20).map { sanitizeId(it) },
                "completedActivityIds" to record.completedActivityIds.take(20).map { sanitizeId(it) },
                "pinnedActivityIds" to record.pinnedActivityIds.take(20).map { sanitizeId(it) },
                "personalizationSummary" to record.personalizationSummary.take(400),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.update(updateMap).await()
        } else {
            val createMap = mapOf(
                "userId" to uid,
                "dateKey" to docId,
                "targetMinutes" to record.targetMinutes.coerceIn(10L, 120L),
                "plannedActivityIds" to record.plannedActivityIds.take(20).map { sanitizeId(it) },
                "completedActivityIds" to record.completedActivityIds.take(20).map { sanitizeId(it) },
                "pinnedActivityIds" to record.pinnedActivityIds.take(20).map { sanitizeId(it) },
                "personalizationSummary" to record.personalizationSummary.take(400),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(createMap).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/dailyPracticePlans/${record.dateKey}")
        Result.failure(e)
    }
}
