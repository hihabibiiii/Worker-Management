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

    fun getDeletedAttendance(): Flow<List<AttendanceEntity>> =
        attendanceDao.getDeletedAttendance()

    fun getSiteAttendance(siteId: Long): Flow<List<com.example.workermanager.data.entity.SiteAttendanceEntity>> =
        siteAttendanceDao.getSiteAttendance(siteId)

    fun getSiteAttendanceByDate(siteId: Long, date: LocalDate): Flow<com.example.workermanager.data.entity.SiteAttendanceEntity?> =
        kotlinx.coroutines.flow.flow {
            emit(siteAttendanceDao.getSiteAttendanceByDate(siteId, date))
        }

    suspend fun insertAttendance(attendance: AttendanceEntity) {
        val existing = attendanceDao.getAttendanceByWorkerSiteAndDate(attendance.workerId, attendance.siteId, attendance.date)
        if (existing != null) {
            val toUpdate = attendance.copy(
                id = existing.id,
                syncId = if (existing.syncId.isNotBlank()) existing.syncId else attendance.syncId,
                isDeleted = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis(),
                isPendingSync = true,
                syncOrigin = if (existing.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else existing.syncOrigin
            )
            attendanceDao.updateAttendance(toUpdate)
        } else {
            attendanceDao.insertAttendance(
                attendance.copy(
                    isDeleted = false,
                    deletedAt = null,
                    updatedAt = System.currentTimeMillis(),
                    isPendingSync = true
                )
            )
        }
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    suspend fun updateAttendance(attendance: AttendanceEntity) {
        val updated = attendance.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (attendance.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else attendance.syncOrigin,
            syncId = if (attendance.syncId.isBlank()) java.util.UUID.randomUUID().toString() else attendance.syncId
        )
        attendanceDao.updateAttendance(updated)
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    suspend fun deleteAttendance(attendance: AttendanceEntity) {
        attendanceDao.softDeleteAttendance(attendance.id)
        recalculateSiteAttendance(attendance.siteId, attendance.date)
        onDataChanged?.invoke()
    }

    suspend fun restoreAttendance(id: Long) {
        attendanceDao.restoreAttendance(id)
        val restored = attendanceDao.getAllAttendanceRaw().find { it.id == id }
        if (restored != null) {
            recalculateSiteAttendance(restored.siteId, restored.date)
        }
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
