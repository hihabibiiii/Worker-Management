package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.AttendanceDao
import com.example.workermanager.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
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

    fun getTotalEarnedBySite(siteId: Long): Flow<Double?> = 
        attendanceDao.getTotalEarnedBySite(siteId)

    fun getAttendanceInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceInRange(startDate, endDate)

    fun getTodayTotalWage(date: LocalDate): Flow<Double?> = 
        attendanceDao.getTodayTotalWage(date)

    suspend fun insertAttendance(attendance: AttendanceEntity) = 
        attendanceDao.insertAttendance(attendance)

    suspend fun updateAttendance(attendance: AttendanceEntity) = 
        attendanceDao.updateAttendance(attendance)
}
