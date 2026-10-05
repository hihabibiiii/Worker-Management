package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.first

class SettingsViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    suspend fun getAllData(): Triple<List<com.example.workermanager.data.entity.WorkerEntity>, List<com.example.workermanager.data.entity.AttendanceEntity>, List<com.example.workermanager.data.entity.PaymentEntity>> {
        val workers = workerRepository.allWorkers.first()
        val attendance = attendanceRepository.allAttendance.first()
        val payments = paymentRepository.allPayments.first()
        return Triple(workers, attendance, payments)
    }
}

class SettingsViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(workerRepository, attendanceRepository, paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
