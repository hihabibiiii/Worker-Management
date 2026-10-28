package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.PaymentDao
import com.example.workermanager.data.entity.PaymentEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val onDataChanged: (() -> Unit)? = null
) {
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

    suspend fun insertPayment(payment: PaymentEntity) {
        paymentDao.insertPayment(payment)
        onDataChanged?.invoke()
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        val updated = if (payment.syncOrigin != SyncOrigin.LEGACY) {
            payment.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true)
        } else payment
        paymentDao.updatePayment(updated)
        onDataChanged?.invoke()
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        paymentDao.softDeletePayment(payment.id)
        onDataChanged?.invoke()
    }

    suspend fun restorePayment(id: Long) {
        paymentDao.restorePayment(id)
        onDataChanged?.invoke()
    }

    fun getDeletedPayments(): Flow<List<PaymentEntity>> = paymentDao.getDeletedPayments()

    suspend fun permanentlyDeletePayment(id: Long) {
        val item = paymentDao.getAllPaymentsRaw().find { it.id == id }
        if (item != null && item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
            paymentDao.markPendingPermanentDelete(id)
        } else {
            paymentDao.permanentlyDeletePayment(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyPaymentTrash() {
        val deleted = paymentDao.getAllPaymentsRaw().filter { it.isDeleted }
        for (item in deleted) {
            if (item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
                paymentDao.markPendingPermanentDelete(item.id)
            } else {
                paymentDao.permanentlyDeletePayment(item.id)
            }
        }
        onDataChanged?.invoke()
    }
}
