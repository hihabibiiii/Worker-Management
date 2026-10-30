package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.SiteExpensePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteExpensePaymentDao {
    @Query("SELECT * FROM site_expense_payments WHERE isDeleted = 0 ORDER BY paymentDate DESC, id DESC")
    fun getAllPayments(): Flow<List<SiteExpensePaymentEntity>>

    @Query("SELECT * FROM site_expense_payments")
    suspend fun getAllPaymentsRaw(): List<SiteExpensePaymentEntity>

    @Query("SELECT * FROM site_expense_payments WHERE syncId = :syncId LIMIT 1")
    suspend fun getPaymentBySyncId(syncId: String): SiteExpensePaymentEntity?

    @Query("SELECT * FROM site_expense_payments WHERE expenseId = :expenseId AND isDeleted = 0 ORDER BY paymentDate DESC, id DESC")
    fun getPaymentsForExpense(expenseId: Long): Flow<List<SiteExpensePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SiteExpensePaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: SiteExpensePaymentEntity)

    @Query("UPDATE site_expense_payments SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeletePayment(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE site_expense_payments SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restorePayment(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM site_expense_payments WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
