package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.ContractorMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractorMaterialDao {
    @Query("SELECT * FROM contractor_materials WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllMaterials(): Flow<List<ContractorMaterialEntity>>

    @Query("SELECT * FROM contractor_materials")
    suspend fun getAllMaterialsRaw(): List<ContractorMaterialEntity>

    @Query("SELECT * FROM contractor_materials WHERE syncId = :syncId LIMIT 1")
    suspend fun getMaterialBySyncId(syncId: String): ContractorMaterialEntity?

    @Query("SELECT * FROM contractor_materials WHERE id = :id AND isDeleted = 0")
    fun getMaterialById(id: Long): Flow<ContractorMaterialEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: ContractorMaterialEntity): Long

    @Update
    suspend fun updateMaterial(material: ContractorMaterialEntity)

    @Query("UPDATE contractor_materials SET isActive = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setMaterialActiveStatus(id: Long, active: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE contractor_materials SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMaterial(id: Long, deletedAt: Long = System.currentTimeMillis())
}
