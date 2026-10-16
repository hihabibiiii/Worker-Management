package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.MaterialDao
import com.example.workermanager.data.dao.MaterialTransactionDao
import com.example.workermanager.data.entity.MaterialEntity
import com.example.workermanager.data.entity.MaterialTransactionEntity
import com.example.workermanager.data.entity.MaterialTransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MaterialRepository(
    private val materialDao: MaterialDao,
    private val transactionDao: MaterialTransactionDao
) {
    val allMaterials: Flow<List<MaterialEntity>> = materialDao.getAllMaterials()
    val allTransactions: Flow<List<MaterialTransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBySite(siteId: Long): Flow<List<MaterialTransactionEntity>> =
        transactionDao.getTransactionsBySite(siteId)

    suspend fun insertMaterial(material: MaterialEntity): Long = materialDao.insertMaterial(material)

    suspend fun updateMaterial(material: MaterialEntity) = materialDao.updateMaterial(material)

    suspend fun deleteMaterial(id: Long) = materialDao.softDeleteMaterial(id)

    suspend fun insertTransaction(transaction: MaterialTransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: MaterialTransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) = transactionDao.softDeleteTransaction(id)

    suspend fun performTransfer(
        materialId: Long,
        sourceSiteId: Long,
        destinationSiteId: Long,
        quantity: Double,
        unit: String,
        transferDate: LocalDate,
        notes: String
    ) {
        val outTx = MaterialTransactionEntity(
            materialId = materialId,
            siteId = sourceSiteId,
            targetSiteId = destinationSiteId,
            transactionType = MaterialTransactionType.TRANSFER_OUT,
            transactionDate = transferDate,
            quantity = quantity,
            unit = unit,
            notes = notes
        )
        val inTx = MaterialTransactionEntity(
            materialId = materialId,
            siteId = destinationSiteId,
            targetSiteId = sourceSiteId,
            transactionType = MaterialTransactionType.TRANSFER_IN,
            transactionDate = transferDate,
            quantity = quantity,
            unit = unit,
            notes = notes
        )
        transactionDao.insertTransaction(outTx)
        transactionDao.insertTransaction(inTx)
    }
}
