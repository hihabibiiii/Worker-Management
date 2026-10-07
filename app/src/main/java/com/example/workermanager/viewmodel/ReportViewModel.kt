package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.ClientPaymentRepository
import com.example.workermanager.data.repository.ContractRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

class ReportViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val monthlyReport = combine(
        _selectedSiteId,
        _selectedMonth
    ) { siteId, month ->
        Pair(siteId, month)
    }.flatMapLatest { (siteId, month) ->
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()
        
        val workersFlow = if (siteId == null) workerRepository.allWorkers 
                         else workerRepository.getWorkersBySite(siteId)
        
        val attendanceFlow = if (siteId == null) attendanceRepository.getAttendanceInRange(startDate, endDate)
                             else attendanceRepository.getAttendanceInRange(startDate, endDate).map { list ->
                                 list.filter { it.siteId == siteId }
                             }
        
        val paymentsFlow = if (siteId == null) paymentRepository.allPayments
                          else paymentRepository.getPaymentsBySite(siteId)

        combine(workersFlow, attendanceFlow, paymentsFlow) { workers, attendanceList, allPayments ->
            workers.map { worker ->
                val workerAttendance = attendanceList.filter { it.workerId == worker.id }
                val workerPayments = allPayments.filter { 
                    it.workerId == worker.id && 
                    !it.date.isBefore(startDate) && !it.date.isAfter(endDate) 
                }
                
                WorkerMonthlyReport(
                    workerName = worker.name,
                    presentDays = workerAttendance.count { it.status == AttendanceStatus.PRESENT },
                    halfDays = workerAttendance.count { it.status == AttendanceStatus.HALF_DAY },
                    absentDays = workerAttendance.count { it.status == AttendanceStatus.ABSENT },
                    overtimeHours = workerAttendance.sumOf { it.overtimeHours },
                    totalEarned = workerAttendance.sumOf { it.amount },
                    totalPaid = workerPayments.sumOf { it.amount }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val contractReport = _selectedSiteId.flatMapLatest { siteId ->
        val contractsFlow = if (siteId == null) contractRepository.allContracts
                           else contractRepository.getContractsBySite(siteId)
        
        contractsFlow.flatMapLatest { contracts ->
            if (contracts.isEmpty()) flowOf(emptyList<ContractReportModel>())
            else {
                val flows = contracts.map { contract ->
                    clientPaymentRepository.getTotalReceivedByContract(contract.id).map { received ->
                        ContractReportModel(
                            clientName = contract.clientName,
                            siteId = contract.siteId,
                            contractAmount = contract.totalAmount,
                            totalReceived = received ?: 0.0,
                            remaining = contract.totalAmount - (received ?: 0.0)
                        )
                    }
                }
                combine(flows) { it.toList() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun updateSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }
}

data class WorkerMonthlyReport(
    val workerName: String,
    val presentDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val overtimeHours: Double,
    val totalEarned: Double,
    val totalPaid: Double
) {
    val pendingBalance: Double = totalEarned - totalPaid
}

data class ContractReportModel(
    val clientName: String,
    val siteId: Long?,
    val contractAmount: Double,
    val totalReceived: Double,
    val remaining: Double
)

class ReportViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(workerRepository, attendanceRepository, paymentRepository, contractRepository, clientPaymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
