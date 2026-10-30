package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.ClientPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientPaymentDao {
    @Query("SELECT * FROM client_payments WHERE contractId = :contractId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun getPaymentsByContract(contractId: Long): Flow<List<ClientPaymentEntity>>

    @Query("SELECT * FROM client_payments")
    suspend fun getAllClientPaymentsRaw(): List<ClientPaymentEntity>

    @Query("SELECT * FROM client_payments WHERE syncId = :syncId LIMIT 1")
    suspend fun getClientPaymentBySyncId(syncId: String): ClientPaymentEntity?

    @Query("SELECT SUM(amount) FROM client_payments WHERE contractId = :contractId AND isDeleted = 0")
    fun getTotalReceivedByContract(contractId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM client_payments WHERE isDeleted = 0")
    fun getTotalReceivedAll(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: ClientPaymentEntity)

    @Update
    suspend fun updatePayment(payment: ClientPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: ClientPaymentEntity)

    @Query("UPDATE client_payments SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteClientPayment(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE client_payments SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreClientPayment(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM client_payments WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedClientPayments(): Flow<List<ClientPaymentEntity>>

    @Query("UPDATE client_payments SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM client_payments WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteClientPayment(id: Long)

    @Query("DELETE FROM client_payments WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM client_payments WHERE isDeleted = 1 AND syncOrigin = 0")
    suspend fun emptyClientPaymentTrash()
}
