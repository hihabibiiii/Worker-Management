package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.workermanager.data.preference.ThemeMode
import com.example.workermanager.data.preference.ThemePreferenceManager
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class SettingsViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferenceManager.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themePreferenceManager.setThemeMode(mode)
    }

    suspend fun getAllData(): Triple<List<com.example.workermanager.data.entity.WorkerEntity>, List<com.example.workermanager.data.entity.AttendanceEntity>, List<com.example.workermanager.data.entity.PaymentEntity>> {
        val workers = workerRepository.allWorkers.first()
        val attendance = attendanceRepository.allAttendance.first()
        val payments = paymentRepository.allPayments.first()
        return Triple(workers, attendance, payments)
    }

    suspend fun clearAllData(database: com.example.workermanager.data.database.AppDatabase): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                database.clearAllTables()
                true
            } catch (e: Exception) {
                android.util.Log.e("WorkCraftDB", "Error clearing database tables", e)
                false
            }
        }
    }
}

class SettingsViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(workerRepository, attendanceRepository, paymentRepository, themePreferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
