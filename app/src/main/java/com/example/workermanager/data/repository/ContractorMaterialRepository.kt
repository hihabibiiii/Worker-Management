package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ContractorMaterialDao
import com.example.workermanager.data.dao.ContractorMaterialTransactionDao
import com.example.workermanager.data.entity.ContractorMaterialEntity
import com.example.workermanager.data.entity.ContractorMaterialTransactionEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class ContractorMaterialRepository(
    private val materialDao: ContractorMaterialDao,
    private val transactionDao: ContractorMaterialTransactionDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allMaterials: Flow<List<ContractorMaterialEntity>> = materialDao.getAllMaterials()
    val allTransactions: Flow<List<ContractorMaterialTransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBySite(siteId: Long): Flow<List<ContractorMaterialTransactionEntity>> =
        transactionDao.getTransactionsBySite(siteId)

    fun getTransactionsByMaterial(materialId: Long): Flow<List<ContractorMaterialTransactionEntity>> =
        transactionDao.getTransactionsByMaterial(materialId)

    suspend fun insertMaterial(material: ContractorMaterialEntity): Long {
        val id = materialDao.insertMaterial(material)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateMaterial(material: ContractorMaterialEntity) {
        val updated = material.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (material.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else material.syncOrigin,
            syncId = if (material.syncId.isBlank()) java.util.UUID.randomUUID().toString() else material.syncId
        )
        materialDao.updateMaterial(updated)
        onDataChanged?.invoke()
    }

    suspend fun deactivateMaterial(id: Long) {
        materialDao.setMaterialActiveStatus(id, active = false)
        onDataChanged?.invoke()
    }

    suspend fun softDeleteMaterial(id: Long) {
        materialDao.softDeleteMaterial(id)
        onDataChanged?.invoke()
    }

    suspend fun restoreMaterial(id: Long) {
        materialDao.restoreMaterial(id)
        onDataChanged?.invoke()
    }

    suspend fun permanentlyDeleteMaterial(id: Long) {
        val item = materialDao.getAllMaterialsRaw().find { it.id == id }
        if (item != null && item.syncId.isNotBlank()) {
            materialDao.markPendingPermanentDelete(id)
        } else {
            materialDao.permanentlyDeleteMaterial(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyMaterialTrash() {
        val deleted = materialDao.getAllMaterialsRaw().filter { it.isDeleted }
        for (item in deleted) {
            if (item.syncId.isNotBlank()) {
                materialDao.markPendingPermanentDelete(item.id)
            } else {
                materialDao.permanentlyDeleteMaterial(item.id)
            }
        }
        onDataChanged?.invoke()
    }

    suspend fun insertTransaction(transaction: ContractorMaterialTransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateTransaction(transaction: ContractorMaterialTransactionEntity) {
        val updated = transaction.copy(
            updatedAt = System.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (transaction.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else transaction.syncOrigin,
            syncId = if (transaction.syncId.isBlank()) java.util.UUID.randomUUID().toString() else transaction.syncId
        )
        transactionDao.updateTransaction(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.softDeleteTransaction(id)
        onDataChanged?.invoke()
    }
}
