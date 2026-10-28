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

    @Query("UPDATE materials SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMaterial(id: Long, deletedAt: Long = System.currentTimeMillis())
}
