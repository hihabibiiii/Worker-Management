package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials")
    suspend fun getAllMaterialsRaw(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE syncId = :syncId LIMIT 1")
    suspend fun getMaterialBySyncId(syncId: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE id = :id AND isDeleted = 0")
    fun getMaterialById(id: Long): Flow<MaterialEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity): Long

    @Update
    suspend fun updateMaterial(material: MaterialEntity)

    @Query("UPDATE materials SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteMaterial(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE materials SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreMaterial(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM materials WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedMaterials(): Flow<List<MaterialEntity>>

    @Query("UPDATE materials SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM materials WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteMaterial(id: Long)

    @Query("DELETE FROM materials WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM materials WHERE isDeleted = 1 AND syncOrigin = 0")
    suspend fun emptyMaterialTrash()
}
