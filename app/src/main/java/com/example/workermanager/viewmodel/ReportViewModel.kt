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
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class ReportType {
    WEEKLY, MONTHLY, YEARLY
}

data class MonthSummaryModel(
    val monthName: String,
    val yearMonth: YearMonth,
    val presentDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val overtimeHours: Double,
    val totalEarned: Double,
    val totalPaid: Double
) {
    val pendingBalance: Double = totalEarned - totalPaid
}

data class YearlySummaryReport(
    val year: Int = Year.now().value,
    val totalPresent: Int = 0,
    val totalHalfDay: Int = 0,
    val totalAbsent: Int = 0,
    val totalOvertimeHours: Double = 0.0,
    val totalWorkerPaid: Double = 0.0,
    val salaryPaid: Double = 0.0,
    val advancePaid: Double = 0.0,
    val bonusPaid: Double = 0.0,
    val otherPaymentsPaid: Double = 0.0,
    val totalContractReceived: Double = 0.0,
    val monthSummaries: List<MonthSummaryModel> = emptyList()
)

class ReportViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val contractRepository: ContractRepository,
    private val clientPaymentRepository: ClientPaymentRepository
) : ViewModel() {

    private val _selectedReportType = MutableStateFlow(ReportType.WEEKLY)
    val selectedReportType: StateFlow<ReportType> = _selectedReportType

    private val _selectedWeekStartDate = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    val selectedWeekStartDate: StateFlow<LocalDate> = _selectedWeekStartDate

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Year.now())
    val selectedYear: StateFlow<Year> = _selectedYear

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weeklyReport = combine(
        _selectedSiteId,
        _selectedWeekStartDate
    ) { siteId, monday ->
        Pair(siteId, monday)
    }.flatMapLatest { (siteId, monday) ->
        val startDate = monday
        val endDate = monday.plusDays(6)
        
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
                    phone = worker.phone,
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
                    phone = worker.phone,
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
    val yearlySummaryReport = combine(
        _selectedSiteId,
        _selectedYear
    ) { siteId, year ->
        Pair(siteId, year)
    }.flatMapLatest { (siteId, year) ->
        val startDate = LocalDate.of(year.value, 1, 1)
        val endDate = LocalDate.of(year.value, 12, 31)

        val attendanceFlow = if (siteId == null) attendanceRepository.getAttendanceInRange(startDate, endDate)
                             else attendanceRepository.getAttendanceInRange(startDate, endDate).map { list ->
                                 list.filter { it.siteId == siteId }
                             }

        val paymentsFlow = if (siteId == null) paymentRepository.allPayments
                          else paymentRepository.getPaymentsBySite(siteId)

        combine(attendanceFlow, paymentsFlow) { attendanceList, allPayments ->
            val yearlyPayments = allPayments.filter { it.date.year == year.value }

            val totalPresent = attendanceList.count { it.status == AttendanceStatus.PRESENT }
            val totalHalfDay = attendanceList.count { it.status == AttendanceStatus.HALF_DAY }
            val totalAbsent = attendanceList.count { it.status == AttendanceStatus.ABSENT }
            val totalOvertimeHours = attendanceList.sumOf { it.overtimeHours }

            val totalWorkerPaid = yearlyPayments.sumOf { it.amount }
            val salaryPaid = yearlyPayments.filter { it.paymentType == PaymentType.SALARY }.sumOf { it.amount }
            val advancePaid = yearlyPayments.filter { it.paymentType == PaymentType.ADVANCE }.sumOf { it.amount }
            val bonusPaid = yearlyPayments.filter { it.paymentType == PaymentType.BONUS }.sumOf { it.amount }
            val otherPaymentsPaid = totalWorkerPaid - salaryPaid - advancePaid - bonusPaid

            val monthSummaries = (1..12).map { monthNum ->
                val ym = YearMonth.of(year.value, monthNum)
                val monthAtt = attendanceList.filter { it.date.monthValue == monthNum }
                val monthPay = yearlyPayments.filter { it.date.monthValue == monthNum }

                MonthSummaryModel(
                    monthName = ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    yearMonth = ym,
                    presentDays = monthAtt.count { it.status == AttendanceStatus.PRESENT },
                    halfDays = monthAtt.count { it.status == AttendanceStatus.HALF_DAY },
                    absentDays = monthAtt.count { it.status == AttendanceStatus.ABSENT },
                    overtimeHours = monthAtt.sumOf { it.overtimeHours },
                    totalEarned = monthAtt.sumOf { it.amount },
                    totalPaid = monthPay.sumOf { it.amount }
                )
            }

            YearlySummaryReport(
                year = year.value,
                totalPresent = totalPresent,
                totalHalfDay = totalHalfDay,
                totalAbsent = totalAbsent,
                totalOvertimeHours = totalOvertimeHours,
                totalWorkerPaid = totalWorkerPaid,
                salaryPaid = salaryPaid,
                advancePaid = advancePaid,
                bonusPaid = bonusPaid,
                otherPaymentsPaid = otherPaymentsPaid,
                totalContractReceived = 0.0,
                monthSummaries = monthSummaries
            )
        }
    }.flatMapLatest { summaryWithoutClient ->
        clientPaymentRepository.getTotalReceivedAll().map { totalReceived ->
            summaryWithoutClient.copy(totalContractReceived = totalReceived ?: 0.0)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, YearlySummaryReport())

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

    fun updateReportType(type: ReportType) {
        _selectedReportType.value = type
    }

    fun updateWeek(monday: LocalDate) {
        _selectedWeekStartDate.value = monday
    }

    fun updateMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun updateYear(year: Year) {
        _selectedYear.value = year
    }

    fun updateSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }
}

data class WorkerMonthlyReport(
    val workerName: String,
    val phone: String,
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
