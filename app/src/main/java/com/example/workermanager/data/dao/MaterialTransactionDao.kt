package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.MaterialTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialTransactionDao {
    @Query("SELECT * FROM material_transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getAllTransactions(): Flow<List<MaterialTransactionEntity>>

    @Query("SELECT * FROM material_transactions")
    suspend fun getAllTransactionsRaw(): List<MaterialTransactionEntity>

    @Query("SELECT * FROM material_transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getTransactionBySyncId(syncId: String): MaterialTransactionEntity?

    @Query("SELECT * FROM material_transactions WHERE siteId = :siteId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsBySite(siteId: Long): Flow<List<MaterialTransactionEntity>>

    @Query("SELECT * FROM material_transactions WHERE materialId = :materialId AND isDeleted = 0 ORDER BY transactionDate DESC, id DESC")
    fun getTransactionsByMaterial(materialId: Long): Flow<List<MaterialTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MaterialTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: MaterialTransactionEntity)

    @Query("UPDATE material_transactions SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE material_transactions SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreTransaction(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM material_transactions WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
