package com.example.workermanager.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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

            if (currentUser == null || currentUser.uid.isBlank()) {
                return Result.success()
            }

            val userSession = app.userSessionManager.getOrCreateSession(currentUser.uid)
            val syncResult = MasterSyncEngine.performFullSync(userSession.database)

            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSyncWorker", "Error executing CloudSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME_ONETIME = "WorkCraftCloudSyncOneTime"

        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
