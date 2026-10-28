package com.example.workermanager.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workermanager.WorkerApplication
import com.example.workermanager.data.remote.sync.engine.MasterSyncEngine
import com.google.firebase.auth.FirebaseAuth

class CloudSyncWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = appContext.applicationContext as WorkerApplication
            val currentUser = FirebaseAuth.getInstance().currentUser
            val targetUserId = inputData.getString(KEY_TARGET_USER_ID)

            if (targetUserId.isNullOrBlank()) {
                Log.w(TAG, "CloudSyncWorker request missing $KEY_TARGET_USER_ID. Aborting execution safely without fallback.")
                return Result.success()
            }

            if (currentUser == null || currentUser.uid != targetUserId) {
                Log.w(TAG, "Current authenticated user (${currentUser?.uid}) does not match target user ID ($targetUserId). Aborting.")
                return Result.success()
            }

            val userSession = app.userSessionManager.getActiveSession(targetUserId)
            if (userSession == null) {
                Log.w(TAG, "Active session for user $targetUserId not found. Aborting sync.")
                return Result.success()
            }

            val syncResult = MasterSyncEngine.performFullSync(userSession.database)

            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing CloudSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CloudSyncWorker"
        const val KEY_TARGET_USER_ID = "key_target_user_id"
        private const val WORK_NAME_PREFIX = "WorkCraftCloudSync_"

        fun scheduleImmediateSync(context: Context) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null || currentUser.uid.isBlank()) return

            val targetUid = currentUser.uid
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(KEY_TARGET_USER_ID to targetUid)

            val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            val uniqueWorkName = "$WORK_NAME_PREFIX$targetUid"

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
            )
        }
    }
}
