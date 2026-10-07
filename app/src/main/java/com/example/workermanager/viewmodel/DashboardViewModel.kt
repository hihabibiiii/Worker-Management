package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.ClientPaymentRepository
import com.example.workermanager.data.repository.ContractRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardStats(
    val presentWorkers: Int = 0,
    val halfDayWorkers: Int = 0,
    val absentWorkers: Int = 0,
    val overtimeWorkers: Int = 0,
    val totalAdvance: Double = 0.0,
    val todayEarned: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0,
    // Contract Stats
    val totalContracts: Int = 0,
    val totalContractReceivable: Double = 0.0,
    val totalContractReceived: Double = 0.0,
    val totalContractRemaining: Double = 0.0
)

class DashboardViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val stats: StateFlow<DashboardStats> = _selectedSiteId.flatMapLatest { siteId ->
        val attendanceFlow = if (siteId == null) attendanceRepository.getAttendanceByDate(LocalDate.now())
                            else attendanceRepository.getAttendanceBySiteAndDate(siteId, LocalDate.now())
        
        val workerPaymentsFlow = if (siteId == null) paymentRepository.allPayments
                                else paymentRepository.getPaymentsBySite(siteId)
        
        val historyAttendanceFlow = if (siteId == null) attendanceRepository.getAttendanceInRange(LocalDate.of(2000, 1, 1), LocalDate.now())
                                   else attendanceRepository.getAttendanceBySite(siteId)
        
        val contractsFlow = if (siteId == null) contractRepository.allContracts
                           else contractRepository.getContractsBySite(siteId)

        val contractReceivedFlow = contractsFlow.flatMapLatest { contracts ->
            if (contracts.isEmpty()) flowOf(0.0)
            else {
                val flows = contracts.map { clientPaymentRepository.getTotalReceivedByContract(it.id) }
                combine(flows) { it.sumOf { amt -> amt ?: 0.0 } }
            }
        }

        combine(
            attendanceFlow,
            workerPaymentsFlow,
            historyAttendanceFlow,
            contractsFlow,
            contractReceivedFlow
        ) { attendance, allPayments, historyAttendance, contracts, receivedAmt ->
            val earned = historyAttendance.sumOf { it.amount }
            val paid = allPayments.sumOf { it.amount }
            val totalContractsAmt = contracts.sumOf { it.totalAmount }
            
            DashboardStats(
                presentWorkers = attendance.count { it.status == AttendanceStatus.PRESENT },
                halfDayWorkers = attendance.count { it.status == AttendanceStatus.HALF_DAY },
                absentWorkers = attendance.count { it.status == AttendanceStatus.ABSENT },
                overtimeWorkers = attendance.count { it.status == AttendanceStatus.OVERTIME },
                totalAdvance = allPayments.filter { it.paymentType == PaymentType.ADVANCE }.sumOf { it.amount },
                todayEarned = attendance.sumOf { it.amount },
                totalEarned = earned,
                totalPaid = paid,
                totalPending = earned - paid,
                totalContracts = contracts.size,
                totalContractReceivable = totalContractsAmt,
                totalContractReceived = receivedAmt,
                totalContractRemaining = totalContractsAmt - receivedAmt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardStats())

    fun updateSelectedSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }
}

class DashboardViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(workerRepository, attendanceRepository, paymentRepository, contractRepository, clientPaymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
