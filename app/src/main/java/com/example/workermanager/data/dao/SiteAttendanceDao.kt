package com.example.workermanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workermanager.data.entity.SiteAttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SiteAttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiteAttendance(siteAttendance: SiteAttendanceEntity)

    @Query("SELECT * FROM site_attendance WHERE siteId = :siteId ORDER BY date DESC")
    fun getSiteAttendance(siteId: Long): Flow<List<SiteAttendanceEntity>>

    @Query("SELECT * FROM site_attendance WHERE siteId = :siteId AND date = :date LIMIT 1")
    suspend fun getSiteAttendanceByDate(siteId: Long, date: LocalDate): SiteAttendanceEntity?

    @Query("SELECT * FROM site_attendance WHERE date = :date")
    fun getAllSiteAttendanceByDate(date: LocalDate): Flow<List<SiteAttendanceEntity>>

    @Query("SELECT * FROM site_attendance")
    suspend fun getAllSiteAttendance(): List<SiteAttendanceEntity>

    @Query("SELECT * FROM site_attendance WHERE syncId = :syncId LIMIT 1")
    suspend fun getSiteAttendanceBySyncId(syncId: String): SiteAttendanceEntity?
}
