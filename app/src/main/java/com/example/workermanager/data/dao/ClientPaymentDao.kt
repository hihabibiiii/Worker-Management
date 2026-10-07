package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.ClientPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientPaymentDao {
    @Query("SELECT * FROM client_payments WHERE contractId = :contractId ORDER BY paymentDate DESC")
    fun getPaymentsByContract(contractId: Long): Flow<List<ClientPaymentEntity>>

    @Query("SELECT SUM(amount) FROM client_payments WHERE contractId = :contractId")
    fun getTotalReceivedByContract(contractId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM client_payments")
    fun getTotalReceivedAll(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: ClientPaymentEntity)

    @Update
    suspend fun updatePayment(payment: ClientPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: ClientPaymentEntity)
}
