package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE siteId = :siteId ORDER BY date DESC")
    fun getPaymentsBySite(siteId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE workerId = :workerId ORDER BY date DESC")
    fun getPaymentsByWorker(workerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE date BETWEEN :startDate AND :endDate")
    fun getPaymentsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("SELECT SUM(amount) FROM payments WHERE workerId = :workerId")
    fun getTotalPaidByWorker(workerId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE siteId = :siteId")
    fun getTotalPaidBySite(siteId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments")
    fun getTotalPaid(): Flow<Double?>
}
