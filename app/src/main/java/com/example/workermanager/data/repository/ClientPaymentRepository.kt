package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ClientPaymentDao
import com.example.workermanager.data.entity.ClientPaymentEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class ClientPaymentRepository(
    private val clientPaymentDao: ClientPaymentDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    fun getPaymentsByContract(contractId: Long): Flow<List<ClientPaymentEntity>> = 
        clientPaymentDao.getPaymentsByContract(contractId)

    fun getTotalReceivedByContract(contractId: Long): Flow<Double?> = 
        clientPaymentDao.getTotalReceivedByContract(contractId)

    fun getTotalReceivedAll(): Flow<Double?> = clientPaymentDao.getTotalReceivedAll()

    suspend fun insertPayment(payment: ClientPaymentEntity) {
        clientPaymentDao.insertPayment(payment)
        onDataChanged?.invoke()
    }

    suspend fun updatePayment(payment: ClientPaymentEntity) {
        val updated = payment.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (payment.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else payment.syncOrigin,
            syncId = if (payment.syncId.isBlank()) java.util.UUID.randomUUID().toString() else payment.syncId
        )
        clientPaymentDao.updatePayment(updated)
        onDataChanged?.invoke()
    }

    suspend fun deletePayment(payment: ClientPaymentEntity) {
        clientPaymentDao.softDeleteClientPayment(payment.id)
        onDataChanged?.invoke()
    }

    suspend fun restoreClientPayment(id: Long) {
        clientPaymentDao.restoreClientPayment(id)
        onDataChanged?.invoke()
    }

    fun getDeletedClientPayments(): Flow<List<ClientPaymentEntity>> = clientPaymentDao.getDeletedClientPayments()

    suspend fun permanentlyDeleteClientPayment(id: Long) {
        val item = clientPaymentDao.getAllClientPaymentsRaw().find { it.id == id }
        if (item != null && item.syncId.isNotBlank()) {
            clientPaymentDao.markPendingPermanentDelete(id)
        } else {
            clientPaymentDao.permanentlyDeleteClientPayment(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyClientPaymentTrash() {
        val deleted = clientPaymentDao.getAllClientPaymentsRaw().filter { it.isDeleted }
        for (item in deleted) {
            if (item.syncId.isNotBlank()) {
                clientPaymentDao.markPendingPermanentDelete(item.id)
            } else {
                clientPaymentDao.permanentlyDeleteClientPayment(item.id)
            }
        }
        onDataChanged?.invoke()
    }
}
