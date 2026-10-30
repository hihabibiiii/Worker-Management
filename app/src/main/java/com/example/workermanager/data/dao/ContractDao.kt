package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.ContractEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getAllContracts(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts")
    suspend fun getAllContractsRaw(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE syncId = :syncId LIMIT 1")
    suspend fun getContractBySyncId(syncId: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE id = :id")
    fun getContractById(id: Long): Flow<ContractEntity?>

    @Query("SELECT * FROM contracts WHERE siteId = :siteId AND isDeleted = 0")
    fun getContractsBySite(siteId: Long): Flow<List<ContractEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity): Long

    @Update
    suspend fun updateContract(contract: ContractEntity)

    @Delete
    suspend fun deleteContract(contract: ContractEntity)

    @Query("UPDATE contracts SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteContract(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE contracts SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreContract(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM contracts WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedContracts(): Flow<List<ContractEntity>>

    @Query("UPDATE contracts SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM contracts WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteContract(id: Long)

    @Query("DELETE FROM contracts WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM contracts WHERE isDeleted = 1 AND syncOrigin = 0")
    suspend fun emptyContractTrash()
}
