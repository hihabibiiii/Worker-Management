package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers")
    suspend fun getAllWorkersRaw(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :workerId")
    fun getWorkerById(workerId: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE currentSiteId = :siteId AND isDeleted = 0 ORDER BY name ASC")
    fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("UPDATE workers SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteWorker(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE workers SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreWorker(id: Long)

    @Query("SELECT * FROM workers WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedWorkers(): Flow<List<WorkerEntity>>

    @Query("DELETE FROM workers WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteWorker(id: Long)

    @Query("DELETE FROM workers WHERE isDeleted = 1")
    suspend fun emptyWorkerTrash()

    @Query("SELECT * FROM workers WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')")
    fun searchWorkers(query: String): Flow<List<WorkerEntity>>
    
    @Query("SELECT * FROM workers WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC")
    fun getActiveWorkers(): Flow<List<WorkerEntity>>
}
