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

    @Query("SELECT * FROM site_expenses WHERE syncId = :syncId LIMIT 1")
    suspend fun getExpenseBySyncId(syncId: String): SiteExpenseEntity?

    @Query("SELECT * FROM site_expenses WHERE id = :id AND isDeleted = 0")
    fun getExpenseById(id: Long): Flow<SiteExpenseEntity?>

    @Query("SELECT * FROM site_expenses WHERE siteId = :siteId AND isDeleted = 0 ORDER BY expenseDate DESC, id DESC")
    fun getExpensesBySite(siteId: Long): Flow<List<SiteExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SiteExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: SiteExpenseEntity)

    @Query("UPDATE site_expenses SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteExpense(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE site_expenses SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun restoreExpense(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM site_expenses WHERE isDeleted = 1 AND deletedAt > 0 ORDER BY deletedAt DESC")
    fun getDeletedExpenses(): Flow<List<SiteExpenseEntity>>

    @Query("UPDATE site_expenses SET deletedAt = -1, updatedAt = :updatedAt, isPendingSync = 1 WHERE id = :id")
    suspend fun markPendingPermanentDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM site_expenses WHERE id = :id AND isDeleted = 1")
    suspend fun permanentlyDeleteExpense(id: Long)

    @Query("DELETE FROM site_expenses WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM site_expenses WHERE isDeleted = 1 AND syncOrigin = 0")
    suspend fun emptyExpenseTrash()
}
