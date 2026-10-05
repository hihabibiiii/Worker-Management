package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.PaymentType
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.PaymentRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    val totalPending: Double = 0.0
)

class DashboardViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val stats: StateFlow<DashboardStats> = _selectedSiteId.flatMapLatest { siteId ->
        if (siteId == null) {
            combine(
                attendanceRepository.getAttendanceByDate(LocalDate.now()),
                paymentRepository.allPayments,
                attendanceRepository.getAttendanceInRange(LocalDate.of(2000, 1, 1), LocalDate.now())
            ) { attendance, allPayments, historyAttendance ->
                DashboardStats(
                    presentWorkers = attendance.count { it.status == AttendanceStatus.PRESENT },
                    halfDayWorkers = attendance.count { it.status == AttendanceStatus.HALF_DAY },
                    absentWorkers = attendance.count { it.status == AttendanceStatus.ABSENT },
                    overtimeWorkers = attendance.count { it.status == AttendanceStatus.OVERTIME },
                    totalAdvance = allPayments.filter { it.paymentType == PaymentType.ADVANCE }.sumOf { it.amount },
                    todayEarned = attendance.sumOf { it.amount },
                    totalEarned = historyAttendance.sumOf { it.amount },
                    totalPaid = allPayments.sumOf { it.amount },
                    totalPending = historyAttendance.sumOf { it.amount } - allPayments.sumOf { it.amount }
                )
            }
        } else {
            combine(
                attendanceRepository.getAttendanceBySiteAndDate(siteId, LocalDate.now()),
                paymentRepository.getPaymentsBySite(siteId),
                attendanceRepository.getAttendanceBySite(siteId)
            ) { attendance, sitePayments, historyAttendance ->
                val earned = historyAttendance.sumOf { it.amount }
                val paid = sitePayments.sumOf { it.amount }
                DashboardStats(
                    presentWorkers = attendance.count { it.status == AttendanceStatus.PRESENT },
                    halfDayWorkers = attendance.count { it.status == AttendanceStatus.HALF_DAY },
                    absentWorkers = attendance.count { it.status == AttendanceStatus.ABSENT },
                    overtimeWorkers = attendance.count { it.status == AttendanceStatus.OVERTIME },
                    totalAdvance = sitePayments.filter { it.paymentType == PaymentType.ADVANCE }.sumOf { it.amount },
                    todayEarned = attendance.sumOf { it.amount },
                    totalEarned = earned,
                    totalPaid = paid,
                    totalPending = earned - paid
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardStats())

    fun updateSelectedSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }
}

class DashboardViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(workerRepository, attendanceRepository, paymentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
