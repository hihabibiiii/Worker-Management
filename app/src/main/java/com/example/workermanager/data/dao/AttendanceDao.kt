package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceRaw(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId AND date = :date")
    fun getAttendanceBySiteAndDate(siteId: Long, date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId AND date = :date")
    suspend fun getAttendanceListBySiteAndDate(siteId: Long, date: LocalDate): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceByWorker(workerId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId ORDER BY date DESC")
    fun getAttendanceBySite(siteId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate")
    fun getAttendanceInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("SELECT SUM(amount) FROM attendance")
    fun getTotalEarned(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM attendance WHERE workerId = :workerId")
    fun getTotalEarnedByWorker(workerId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM attendance WHERE siteId = :siteId")
    fun getTotalEarnedBySite(siteId: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM attendance WHERE date = :date")
    fun getTodayTotalWage(date: LocalDate): Flow<Double?>

    @Query("SELECT COUNT(DISTINCT date) FROM attendance WHERE siteId = :siteId AND status IN ('PRESENT', 'HALF_DAY', 'OVERTIME')")
    fun getSiteWorkingDays(siteId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT siteId, date FROM attendance WHERE status IN ('PRESENT', 'HALF_DAY', 'OVERTIME'))")
    fun getTotalSiteWorkingDays(): Flow<Int>
}
