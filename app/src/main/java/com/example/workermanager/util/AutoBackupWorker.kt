package com.example.workermanager.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.workermanager.WorkerApplication
import com.example.workermanager.data.preference.AutoBackupPreferenceManager

class AutoBackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = appContext.applicationContext as WorkerApplication
            val prefManager = AutoBackupPreferenceManager(appContext)

            if (!prefManager.isAutoBackupEnabled.value) {
                return Result.success()
            }

            val success = BackupManager.performAutoBackup(appContext, app.database, prefManager)
            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkCraftAutoBackup", "AutoBackupWorker failed", e)
            Result.retry()
        }
    }
}
