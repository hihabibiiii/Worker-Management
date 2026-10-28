package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.WorkerSiteHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkerSiteHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WorkerSiteHistoryEntity)

    @Update
    suspend fun update(history: WorkerSiteHistoryEntity)

    @Query("SELECT * FROM worker_site_history WHERE workerId = :workerId ORDER BY startDate DESC")
    fun getHistoryForWorker(workerId: Long): Flow<List<WorkerSiteHistoryEntity>>

    @Query("SELECT * FROM worker_site_history WHERE workerId = :workerId AND endDate IS NULL LIMIT 1")
    suspend fun getCurrentHistoryForWorker(workerId: Long): WorkerSiteHistoryEntity?

    @Query("SELECT * FROM worker_site_history WHERE siteId = :siteId AND (endDate IS NULL OR :date <= endDate) AND startDate <= :date")
    fun getWorkersForSiteOnDate(siteId: Long, date: LocalDate): Flow<List<WorkerSiteHistoryEntity>>
    
    @Query("SELECT * FROM worker_site_history WHERE (endDate IS NULL OR :date <= endDate) AND startDate <= :date")
    fun getHistoryForAllSitesOnDate(date: LocalDate): Flow<List<WorkerSiteHistoryEntity>>

    @Query("UPDATE worker_site_history SET endDate = :endDate WHERE workerId = :workerId AND endDate IS NULL")
    suspend fun closeCurrentHistory(workerId: Long, endDate: LocalDate)
    
    @Query("SELECT siteId FROM worker_site_history WHERE workerId = :workerId AND startDate <= :date AND (endDate IS NULL OR endDate >= :date) LIMIT 1")
    suspend fun getSiteIdForWorkerOnDate(workerId: Long, date: LocalDate): Long?

    @Query("SELECT * FROM worker_site_history")
    suspend fun getAllHistory(): List<WorkerSiteHistoryEntity>

    @Query("SELECT * FROM worker_site_history WHERE syncId = :syncId LIMIT 1")
    suspend fun getHistoryBySyncId(syncId: String): WorkerSiteHistoryEntity?
}
