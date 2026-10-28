package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.AttendanceDao
import com.example.workermanager.data.entity.AttendanceEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val siteAttendanceDao: com.example.workermanager.data.dao.SiteAttendanceDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allAttendance: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendance()

    fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByDate(date)
    
    fun getAttendanceBySiteAndDate(siteId: Long, date: LocalDate): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceBySiteAndDate(siteId, date)

    fun getAttendanceByWorker(workerId: Long): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceByWorker(workerId)

    fun getAttendanceBySite(siteId: Long): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceBySite(siteId)

    fun getTotalEarnedByWorker(workerId: Long): Flow<Double?> = 
        attendanceDao.getTotalEarnedByWorker(workerId)

    fun getTotalEarned(): Flow<Double?> = 
        attendanceDao.getTotalEarned()

    fun getTotalEarnedBySite(siteId: Long): Flow<Double?> = 
        attendanceDao.getTotalEarnedBySite(siteId)

    fun getAttendanceInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceInRange(startDate, endDate)

    fun getTodayTotalWage(date: LocalDate): Flow<Double?> = 
        attendanceDao.getTodayTotalWage(date)

    fun getSiteWorkingDays(siteId: Long): Flow<Int> =
        attendanceDao.getSiteWorkingDays(siteId)

    fun getTotalSiteWorkingDays(): Flow<Int> =
        attendanceDao.getTotalSiteWorkingDays()

    fun getSiteAttendance(siteId: Long): Flow<List<com.example.workermanager.data.entity.SiteAttendanceEntity>> =
        siteAttendanceDao.getSiteAttendance(siteId)

    fun getSiteAttendanceByDate(siteId: Long, date: LocalDate): Flow<com.example.workermanager.data.entity.SiteAttendanceEntity?> =
        kotlinx.coroutines.flow.flow {
            emit(siteAttendanceDao.getSiteAttendanceByDate(siteId, date))
        }

    suspend fun insertAttendance(attendance: AttendanceEntity) {
        attendanceDao.insertAttendance(attendance)
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    suspend fun updateAttendance(attendance: AttendanceEntity) {
        val updated = if (attendance.syncOrigin != SyncOrigin.LEGACY) {
            attendance.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true)
        } else attendance
        attendanceDao.updateAttendance(updated)
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    suspend fun deleteAttendance(attendance: AttendanceEntity) {
        attendanceDao.deleteAttendance(attendance)
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    private suspend fun recalculateSiteAttendance(siteId: Long, date: LocalDate) {
        val workerAttendances = attendanceDao.getAttendanceListBySiteAndDate(siteId, date)
        val isPresent = workerAttendances.any { 
            it.status == com.example.workermanager.data.entity.AttendanceStatus.PRESENT || 
            it.status == com.example.workermanager.data.entity.AttendanceStatus.HALF_DAY ||
            it.status == com.example.workermanager.data.entity.AttendanceStatus.OVERTIME
        }
        
        val existing = siteAttendanceDao.getSiteAttendanceByDate(siteId, date)
        if (existing != null) {
            val updated = if (existing.syncOrigin != SyncOrigin.LEGACY) {
                existing.copy(isPresent = isPresent, updatedAt = System.currentTimeMillis(), isPendingSync = true)
            } else existing.copy(isPresent = isPresent)
            siteAttendanceDao.insertSiteAttendance(updated)
        } else {
            siteAttendanceDao.insertSiteAttendance(
                com.example.workermanager.data.entity.SiteAttendanceEntity(
                    siteId = siteId,
                    date = date,
                    isPresent = isPresent
                )
            )
        }
    }
}
