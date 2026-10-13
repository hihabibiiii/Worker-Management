package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsRaw(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE siteId = :siteId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsBySite(siteId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE workerId = :workerId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsByWorker(workerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0")
    fun getPaymentsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeletePayment(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE payments SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restorePayment(id: Long)

    @Query("SELECT * FROM payments WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedPayments(): Flow<List<PaymentEntity>>

    @Query("DELETE FROM payments WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeletePayment(id: Long)

    @Query("DELETE FROM payments WHERE isDeleted = 1")
    suspend fun emptyPaymentTrash()

    @Query("SELECT SUM(amount) FROM payments WHERE workerId = :workerId AND isDeleted = 0")
    fun getTotalPaidByWorker(workerId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE siteId = :siteId AND isDeleted = 0")
    fun getTotalPaidBySite(siteId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE isDeleted = 0")
    fun getTotalPaid(): Flow<Double?>
}
