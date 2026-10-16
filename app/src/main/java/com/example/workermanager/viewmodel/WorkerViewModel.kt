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

    private val _selectedSiteFilterId = MutableStateFlow<Long?>(null)
    val selectedSiteFilterId: StateFlow<Long?> = _selectedSiteFilterId

    val workers: StateFlow<List<WorkerEntity>> = combine(
        _searchQuery,
        _selectedSiteFilterId,
        repository.allWorkers
    ) { query, siteFilterId, allWorkersList ->
        var list = allWorkersList
        if (siteFilterId != null) {
            list = list.filter { it.currentSiteId == siteFilterId }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSiteFilter(siteId: Long?) {
        _selectedSiteFilterId.value = siteId
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

    fun updateWorkerPhoto(workerId: Long, photoPath: String?) {
        viewModelScope.launch {
            val worker = repository.getWorkerById(workerId).firstOrNull()
            if (worker != null) {
                repository.updateWorker(worker.copy(photoPath = photoPath))
            }
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
        val totalAdvance = payments.filter { it.paymentType == PaymentType.ADVANCE }.sumOf { it.amount }
        
        WorkerStats(
            presentDays = present,
            halfDays = halfDay,
            absentDays = absent,
            overtimeHours = overtimeHours,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            totalAdvance = totalAdvance,
            pendingBalance = totalEarned - totalPaid
        )
    }
}

data class WorkerStats(
    val presentDays: Int = 0,
    val halfDays: Int = 0,
    val absentDays: Int = 0,
    val overtimeHours: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val pendingBalance: Double = 0.0
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
