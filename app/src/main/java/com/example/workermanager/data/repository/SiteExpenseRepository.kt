package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.SiteExpenseDao
import com.example.workermanager.data.dao.SiteExpensePaymentDao
import com.example.workermanager.data.entity.SiteExpenseEntity
import com.example.workermanager.data.entity.SiteExpensePaymentEntity
import kotlinx.coroutines.flow.Flow

class SiteExpenseRepository(
    private val expenseDao: SiteExpenseDao,
    private val paymentDao: SiteExpensePaymentDao
) {
    val allExpenses: Flow<List<SiteExpenseEntity>> = expenseDao.getAllExpenses()
    val allPayments: Flow<List<SiteExpensePaymentEntity>> = paymentDao.getAllPayments()

    fun getExpensesBySite(siteId: Long): Flow<List<SiteExpenseEntity>> = expenseDao.getExpensesBySite(siteId)

    fun getPaymentsForExpense(expenseId: Long): Flow<List<SiteExpensePaymentEntity>> = paymentDao.getPaymentsForExpense(expenseId)

    suspend fun insertExpense(expense: SiteExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: SiteExpenseEntity) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(id: Long) = expenseDao.softDeleteExpense(id)

    suspend fun insertPayment(payment: SiteExpensePaymentEntity): Long = paymentDao.insertPayment(payment)

    suspend fun updatePayment(payment: SiteExpensePaymentEntity) = paymentDao.updatePayment(payment)

    suspend fun deletePayment(id: Long) = paymentDao.softDeletePayment(id)
}
