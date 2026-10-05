package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.PaymentDao
import com.example.workermanager.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PaymentRepository(private val paymentDao: PaymentDao) {
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    fun getPaymentsBySite(siteId: Long): Flow<List<PaymentEntity>> = 
        paymentDao.getPaymentsBySite(siteId)

    fun getPaymentsByWorker(workerId: Long): Flow<List<PaymentEntity>> = 
        paymentDao.getPaymentsByWorker(workerId)

    fun getTotalPaidByWorker(workerId: Long): Flow<Double?> = 
        paymentDao.getTotalPaidByWorker(workerId)

    fun getTotalPaidBySite(siteId: Long): Flow<Double?> = 
        paymentDao.getTotalPaidBySite(siteId)

    fun getTotalPaid(): Flow<Double?> = paymentDao.getTotalPaid()

    suspend fun insertPayment(payment: PaymentEntity) = paymentDao.insertPayment(payment)
    suspend fun updatePayment(payment: PaymentEntity) = paymentDao.updatePayment(payment)
    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.deletePayment(payment)
}
