package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.MaterialDao
import com.example.workermanager.data.dao.MaterialTransactionDao
import com.example.workermanager.data.entity.MaterialEntity
import com.example.workermanager.data.entity.MaterialTransactionEntity
import com.example.workermanager.data.entity.MaterialTransactionType
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MaterialRepository(
    private val materialDao: MaterialDao,
    private val transactionDao: MaterialTransactionDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allMaterials: Flow<List<MaterialEntity>> = materialDao.getAllMaterials()
    val allTransactions: Flow<List<MaterialTransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBySite(siteId: Long): Flow<List<MaterialTransactionEntity>> =
        transactionDao.getTransactionsBySite(siteId)

    suspend fun insertMaterial(material: MaterialEntity): Long {
        val id = materialDao.insertMaterial(material)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateMaterial(material: MaterialEntity) {
        val updated = if (material.syncOrigin != SyncOrigin.LEGACY) {
            material.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true)
        } else material
        materialDao.updateMaterial(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteMaterial(id: Long) {
        materialDao.softDeleteMaterial(id)
        onDataChanged?.invoke()
    }

    suspend fun permanentlyDeleteMaterial(id: Long) {
        val item = materialDao.getAllMaterialsRaw().find { it.id == id }
        if (item != null && item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
            materialDao.markPendingPermanentDelete(id)
        } else {
            materialDao.permanentlyDeleteMaterial(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyMaterialTrash() {
        val deleted = materialDao.getAllMaterialsRaw().filter { it.isDeleted }
        for (item in deleted) {
            if (item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
                materialDao.markPendingPermanentDelete(item.id)
            } else {
                materialDao.permanentlyDeleteMaterial(item.id)
            }
        }
        onDataChanged?.invoke()
    }

    suspend fun insertTransaction(transaction: MaterialTransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateTransaction(transaction: MaterialTransactionEntity) {
        val updated = if (transaction.syncOrigin != SyncOrigin.LEGACY) {
            transaction.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true)
        } else transaction
        transactionDao.updateTransaction(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.softDeleteTransaction(id)
        onDataChanged?.invoke()
    }

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
        onDataChanged?.invoke()
    }
}
