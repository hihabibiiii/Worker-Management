package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :workerId")
    fun getWorkerById(workerId: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE currentSiteId = :siteId ORDER BY name ASC")
    fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("SELECT * FROM workers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchWorkers(query: String): Flow<List<WorkerEntity>>
    
    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveWorkers(): Flow<List<WorkerEntity>>
}
