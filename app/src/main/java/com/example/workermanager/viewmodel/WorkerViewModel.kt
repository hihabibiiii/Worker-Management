package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.*
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkerViewModel(
    private val repository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val workers: StateFlow<List<WorkerEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allWorkers else repository.searchWorkers(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addWorker(
        name: String,
        phone: String,
        workerType: WorkerType,
        dailyRate: Double,
        overtimeRate: Double,
        joiningDate: LocalDate,
        siteId: Long?,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertWorker(
                WorkerEntity(
                    name = name,
                    phone = phone,
                    workerType = workerType,
                    dailyRate = dailyRate,
                    overtimeRate = overtimeRate,
                    joiningDate = joiningDate,
                    notes = notes
                ),
                siteId
            )
        }
    }

    fun updateWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            repository.updateWorker(worker)
        }
    }

    fun deleteWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
        }
    }

    fun getWorkerById(id: Long): Flow<WorkerEntity?> = repository.getWorkerById(id)

    fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>> = repository.getWorkersBySite(siteId)

    fun getWorkerSiteHistory(workerId: Long) = repository.getWorkerSiteHistory(workerId)

    fun transferWorker(workerId: Long, newSiteId: Long, transferDate: LocalDate) {
        viewModelScope.launch {
            repository.transferWorker(workerId, newSiteId, transferDate)
        }
    }

    fun updatePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            paymentRepository.updatePayment(payment)
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            paymentRepository.deletePayment(payment)
        }
    }

    fun getWorkerStats(workerId: Long) = combine(
        attendanceRepository.getAttendanceByWorker(workerId),
        paymentRepository.getPaymentsByWorker(workerId)
    ) { attendance, payments ->
        val present = attendance.count { it.status == AttendanceStatus.PRESENT }
        val halfDay = attendance.count { it.status == AttendanceStatus.HALF_DAY }
        val absent = attendance.count { it.status == AttendanceStatus.ABSENT }
        val overtimeHours = attendance.sumOf { it.overtimeHours }
        
        val totalEarned = attendance.sumOf { it.amount }
        val totalPaid = payments.sumOf { it.amount }
        
        WorkerStats(
            presentDays = present,
            halfDays = halfDay,
            absentDays = absent,
            overtimeHours = overtimeHours,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            pendingBalance = totalEarned - totalPaid
        )
    }
}

data class WorkerStats(
    val presentDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val overtimeHours: Double,
    val totalEarned: Double,
    val totalPaid: Double,
    val pendingBalance: Double
)

class WorkerViewModelFactory(
    private val repository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerViewModel(repository, attendanceRepository, paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
