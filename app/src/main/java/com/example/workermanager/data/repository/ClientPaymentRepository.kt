package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ClientPaymentDao
import com.example.workermanager.data.entity.ClientPaymentEntity
import kotlinx.coroutines.flow.Flow

class ClientPaymentRepository(private val clientPaymentDao: ClientPaymentDao) {
    fun getPaymentsByContract(contractId: Long): Flow<List<ClientPaymentEntity>> = 
        clientPaymentDao.getPaymentsByContract(contractId)

    fun getTotalReceivedByContract(contractId: Long): Flow<Double?> = 
        clientPaymentDao.getTotalReceivedByContract(contractId)

    fun getTotalReceivedAll(): Flow<Double?> = clientPaymentDao.getTotalReceivedAll()

    suspend fun insertPayment(payment: ClientPaymentEntity) = clientPaymentDao.insertPayment(payment)

    suspend fun updatePayment(payment: ClientPaymentEntity) = clientPaymentDao.updatePayment(payment)

    suspend fun deletePayment(payment: ClientPaymentEntity) = clientPaymentDao.softDeleteClientPayment(payment.id)

    suspend fun restoreClientPayment(id: Long) = clientPaymentDao.restoreClientPayment(id)

    fun getDeletedClientPayments(): Flow<List<ClientPaymentEntity>> = clientPaymentDao.getDeletedClientPayments()

    suspend fun permanentlyDeleteClientPayment(id: Long) = clientPaymentDao.permanentlyDeleteClientPayment(id)

    suspend fun emptyClientPaymentTrash() = clientPaymentDao.emptyClientPaymentTrash()
}
