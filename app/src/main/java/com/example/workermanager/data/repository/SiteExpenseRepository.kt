package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.SiteExpenseDao
import com.example.workermanager.data.dao.SiteExpensePaymentDao
import com.example.workermanager.data.entity.SiteExpenseEntity
import com.example.workermanager.data.entity.SiteExpensePaymentEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class SiteExpenseRepository(
    private val expenseDao: SiteExpenseDao,
    private val paymentDao: SiteExpensePaymentDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allExpenses: Flow<List<SiteExpenseEntity>> = expenseDao.getAllExpenses()
    val allPayments: Flow<List<SiteExpensePaymentEntity>> = paymentDao.getAllPayments()

    fun getExpensesBySite(siteId: Long): Flow<List<SiteExpenseEntity>> = expenseDao.getExpensesBySite(siteId)

    fun getPaymentsForExpense(expenseId: Long): Flow<List<SiteExpensePaymentEntity>> = paymentDao.getPaymentsForExpense(expenseId)

    suspend fun insertExpense(expense: SiteExpenseEntity): Long {
        val id = expenseDao.insertExpense(expense)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateExpense(expense: SiteExpenseEntity) {
        val updated = expense.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (expense.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else expense.syncOrigin,
            syncId = if (expense.syncId.isBlank()) java.util.UUID.randomUUID().toString() else expense.syncId
        )
        expenseDao.updateExpense(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteExpense(id: Long) {
        expenseDao.softDeleteExpense(id)
        onDataChanged?.invoke()
    }

    suspend fun insertPayment(payment: SiteExpensePaymentEntity): Long {
        val id = paymentDao.insertPayment(payment)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updatePayment(payment: SiteExpensePaymentEntity) {
        val updated = payment.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (payment.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else payment.syncOrigin,
            syncId = if (payment.syncId.isBlank()) java.util.UUID.randomUUID().toString() else payment.syncId
        )
        paymentDao.updatePayment(updated)
        onDataChanged?.invoke()
    }

    suspend fun deletePayment(id: Long) {
        paymentDao.softDeletePayment(id)
        onDataChanged?.invoke()
    }
}
