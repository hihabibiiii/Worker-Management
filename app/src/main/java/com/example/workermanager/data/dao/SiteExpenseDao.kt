package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.SiteExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteExpenseDao {
    @Query("SELECT * FROM site_expenses WHERE isDeleted = 0 ORDER BY expenseDate DESC, id DESC")
    fun getAllExpenses(): Flow<List<SiteExpenseEntity>>

    @Query("SELECT * FROM site_expenses")
    suspend fun getAllExpensesRaw(): List<SiteExpenseEntity>

    @Query("SELECT * FROM site_expenses WHERE id = :id AND isDeleted = 0")
    fun getExpenseById(id: Long): Flow<SiteExpenseEntity?>

    @Query("SELECT * FROM site_expenses WHERE siteId = :siteId AND isDeleted = 0 ORDER BY expenseDate DESC, id DESC")
    fun getExpensesBySite(siteId: Long): Flow<List<SiteExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SiteExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: SiteExpenseEntity)

    @Query("UPDATE site_expenses SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteExpense(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE site_expenses SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreExpense(id: Long)
}
