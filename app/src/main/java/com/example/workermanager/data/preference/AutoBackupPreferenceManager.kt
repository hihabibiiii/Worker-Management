package com.example.workermanager.data.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workermanager.util.AutoBackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class AutoBackupPreferenceManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("workcraft_autobackup_prefs", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_ENABLED)) {
            prefs.edit().putBoolean(KEY_ENABLED, true).apply()
            scheduleWorkManager()
        }
    }

    private val _isAutoBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled

    private val _backupUri = MutableStateFlow(prefs.getString(KEY_URI, null))
    val backupUri: StateFlow<String?> = _backupUri

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_TIME, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime

    private val _lastBackupStatus = MutableStateFlow(prefs.getString(KEY_LAST_STATUS, "Never") ?: "Never")
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isAutoBackupEnabled.value = enabled

        if (enabled) {
            scheduleWorkManager()
        } else {
            cancelWorkManager()
        }
    }

    fun setBackupUri(uriString: String?) {
        prefs.edit().putString(KEY_URI, uriString).apply()
        _backupUri.value = uriString
    }

    fun updateBackupStatus(time: Long, status: String) {
        prefs.edit()
            .putLong(KEY_LAST_TIME, time)
            .putString(KEY_LAST_STATUS, status)
            .apply()
        _lastBackupTime.value = time
        _lastBackupStatus.value = status
    }

    fun scheduleWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    fun cancelWorkManager() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "WorkCraftAutoBackupWork"
        private const val KEY_ENABLED = "key_auto_backup_enabled"
        private const val KEY_URI = "key_auto_backup_uri"
        private const val KEY_LAST_TIME = "key_last_backup_time"
        private const val KEY_LAST_STATUS = "key_last_backup_status"
    }
}
