package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceRaw(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE syncId = :syncId LIMIT 1")
    suspend fun getAttendanceBySyncId(syncId: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND siteId = :siteId AND date = :date LIMIT 1")
    suspend fun getAttendanceByWorkerSiteAndDate(workerId: Long, siteId: Long, date: LocalDate): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date = :date AND isDeleted = 0")
    fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId AND date = :date AND isDeleted = 0")
    fun getAttendanceBySiteAndDate(siteId: Long, date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId AND date = :date AND isDeleted = 0")
    suspend fun getAttendanceListBySiteAndDate(siteId: Long, date: LocalDate): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND isDeleted = 0 ORDER BY date DESC")
    fun getAttendanceByWorker(workerId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE siteId = :siteId AND isDeleted = 0 ORDER BY date DESC")
    fun getAttendanceBySite(siteId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0")
    fun getAttendanceInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("UPDATE attendance SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteAttendance(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE attendance SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreAttendance(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM attendance WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedAttendance(): Flow<List<AttendanceEntity>>

    @Query("UPDATE attendance SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM attendance WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteAttendance(id: Long)

    @Query("DELETE FROM attendance WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT SUM(amount) FROM attendance WHERE isDeleted = 0")
    fun getTotalEarned(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM attendance WHERE workerId = :workerId AND isDeleted = 0")
    fun getTotalEarnedByWorker(workerId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM attendance WHERE siteId = :siteId AND isDeleted = 0")
    fun getTotalEarnedBySite(siteId: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM attendance WHERE date = :date AND isDeleted = 0")
    fun getTodayTotalWage(date: LocalDate): Flow<Double?>

    @Query("SELECT COUNT(DISTINCT date) FROM attendance WHERE siteId = :siteId AND isDeleted = 0 AND status IN ('PRESENT', 'HALF_DAY', 'OVERTIME')")
    fun getSiteWorkingDays(siteId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT siteId, date FROM attendance WHERE isDeleted = 0 AND status IN ('PRESENT', 'HALF_DAY', 'OVERTIME'))")
    fun getTotalSiteWorkingDays(): Flow<Int>
}
