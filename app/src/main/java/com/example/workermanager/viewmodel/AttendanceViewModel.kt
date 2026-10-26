package com.example.workermanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.AttendanceStatus
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.repository.AttendanceRepository
import com.example.workermanager.data.repository.WorkerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AttendanceViewModel(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedSiteId = MutableStateFlow<Long?>(null)
    val selectedSiteId: StateFlow<Long?> = _selectedSiteId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceForSelectedDate: StateFlow<List<AttendanceEntity>> = _selectedDate
        .flatMapLatest { date -> attendanceRepository.getAttendanceByDate(date) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workersWithAttendance: StateFlow<List<WorkerAttendance>> = combine(
        _selectedSiteId,
        _selectedDate
    ) { siteId, date ->
        Pair(siteId, date)
    }.flatMapLatest { (siteId, date) ->
        if (siteId == null) {
            combine(
                workerRepository.activeWorkers,
                attendanceRepository.getAttendanceByDate(date)
            ) { workers, attendanceList ->
                workers.map { worker ->
                    val attendance = attendanceList.find { it.workerId == worker.id }
                    WorkerAttendance(worker, attendance)
                }
            }
        } else {
            combine(
                workerRepository.allWorkers,
                workerRepository.getWorkersForSiteOnDate(siteId, date),
                attendanceRepository.getAttendanceBySiteAndDate(siteId, date)
            ) { allWorkers, history, attendanceList ->
                val workerIds = history.map { it.workerId }.toSet()
                allWorkers.filter { it.id in workerIds }.map { worker ->
                    val attendance = attendanceList.find { it.workerId == worker.id }
                    WorkerAttendance(worker, attendance)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSelectedDate(date: LocalDate) {
        if (!date.isAfter(LocalDate.now())) {
            _selectedDate.value = date
        }
    }

    fun updateSelectedSite(siteId: Long?) {
        _selectedSiteId.value = siteId
    }

    fun markAttendance(
        worker: WorkerEntity,
        status: AttendanceStatus,
        overtimeHours: Double = 0.0,
        note: String = ""
    ) {
        val targetDate = _selectedDate.value
        if (targetDate.isAfter(LocalDate.now())) {
            return
        }
        val siteId = worker.currentSiteId ?: return
        viewModelScope.launch {
            val overtimeAmount = overtimeHours * worker.overtimeRate
            val baseAmount = when (status) {
                AttendanceStatus.PRESENT -> worker.dailyRate
                AttendanceStatus.HALF_DAY -> worker.dailyRate / 2.0
                AttendanceStatus.ABSENT -> 0.0
                AttendanceStatus.OVERTIME -> 0.0
            }

            val totalAmount = baseAmount + overtimeAmount

            val existingList = attendanceRepository.getAttendanceByDate(targetDate).firstOrNull() ?: emptyList()
            val existing = existingList.find { it.workerId == worker.id && it.siteId == siteId }

            val attendance = if (existing != null) {
                existing.copy(
                    status = status,
                    overtimeHours = overtimeHours,
                    overtimeAmount = overtimeAmount,
                    amount = totalAmount,
                    note = note
                )
            } else {
                AttendanceEntity(
                    workerId = worker.id,
                    siteId = siteId,
                    date = targetDate,
                    status = status,
                    overtimeHours = overtimeHours,
                    overtimeAmount = overtimeAmount,
                    amount = totalAmount,
                    note = note
                )
            }
            attendanceRepository.insertAttendance(attendance)
        }
    }

    fun getAttendanceByWorker(workerId: Long): Flow<List<AttendanceEntity>> {
        return attendanceRepository.getAttendanceByWorker(workerId)
    }

    fun getAttendanceBySite(siteId: Long): Flow<List<AttendanceEntity>> {
        return attendanceRepository.getAttendanceBySite(siteId)
    }

    fun getSiteAttendance(siteId: Long): Flow<List<com.example.workermanager.data.entity.SiteAttendanceEntity>> {
        return attendanceRepository.getSiteAttendance(siteId)
    }

    fun getSiteAttendanceForDate(siteId: Long, date: LocalDate): Flow<com.example.workermanager.data.entity.SiteAttendanceEntity?> {
        return attendanceRepository.getSiteAttendanceByDate(siteId, date)
    }
}

data class WorkerAttendance(
    val worker: WorkerEntity,
    val attendance: AttendanceEntity?
)

class AttendanceViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(workerRepository, attendanceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
