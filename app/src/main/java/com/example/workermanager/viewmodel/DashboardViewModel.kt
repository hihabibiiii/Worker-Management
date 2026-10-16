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
import com.example.workermanager.data.repository.SiteExpenseRepository
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
    val totalContractRemaining: Double = 0.0,
    // Site Expense Stats
    val todayExpense: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalExpensePaid: Double = 0.0,
    val totalExpenseRemain: Double = 0.0
)

class DashboardViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository,
    private val siteExpenseRepository: SiteExpenseRepository
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

        val siteExpensesFlow = if (siteId == null) siteExpenseRepository.allExpenses
                              else siteExpenseRepository.getExpensesBySite(siteId)

        val siteExpensePaymentsFlow = siteExpenseRepository.allPayments

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
            contractReceivedFlow,
            siteExpensesFlow,
            siteExpensePaymentsFlow
        ) { flowsArray ->
            @Suppress("UNCHECKED_CAST")
            val attendance = flowsArray[0] as List<com.example.workermanager.data.entity.AttendanceEntity>
            @Suppress("UNCHECKED_CAST")
            val allPayments = flowsArray[1] as List<com.example.workermanager.data.entity.PaymentEntity>
            @Suppress("UNCHECKED_CAST")
            val historyAttendance = flowsArray[2] as List<com.example.workermanager.data.entity.AttendanceEntity>
            @Suppress("UNCHECKED_CAST")
            val contracts = flowsArray[3] as List<com.example.workermanager.data.entity.ContractEntity>
            val receivedAmt = flowsArray[4] as Double
            @Suppress("UNCHECKED_CAST")
            val siteExpenses = flowsArray[5] as List<com.example.workermanager.data.entity.SiteExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val siteExpensePayments = flowsArray[6] as List<com.example.workermanager.data.entity.SiteExpensePaymentEntity>

            val today = LocalDate.now()
            val earned = historyAttendance.sumOf { it.amount }
            val paid = allPayments.sumOf { it.amount }
            val totalContractsAmt = contracts.sumOf { it.totalAmount }

            // Expense calculations
            val activeExpIds = siteExpenses.map { it.id }.toSet()
            val linkedPayments = siteExpensePayments.filter { activeExpIds.contains(it.expenseId) }

            val todayExp = siteExpenses.filter { it.expenseDate == today }.sumOf { it.amount }
            val totalExp = siteExpenses.sumOf { it.amount }
            val totalExpPaid = linkedPayments.sumOf { it.amount }
            val totalExpRemain = (totalExp - totalExpPaid).coerceAtLeast(0.0)

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
                totalContractRemaining = totalContractsAmt - receivedAmt,
                todayExpense = todayExp,
                totalExpense = totalExp,
                totalExpensePaid = totalExpPaid,
                totalExpenseRemain = totalExpRemain
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardStats())

    val totalSiteWorkingDays: StateFlow<Int> = attendanceRepository.getTotalSiteWorkingDays()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun updateSelectedSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }

    fun getSiteWorkingDays(siteId: Long) = attendanceRepository.getSiteWorkingDays(siteId)
}

class DashboardViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository,
    private val siteExpenseRepository: SiteExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                workerRepository,
                attendanceRepository,
                paymentRepository,
                contractRepository,
                clientPaymentRepository,
                siteExpenseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
