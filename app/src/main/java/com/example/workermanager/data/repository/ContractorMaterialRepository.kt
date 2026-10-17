package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ContractorMaterialDao
import com.example.workermanager.data.dao.ContractorMaterialTransactionDao
import com.example.workermanager.data.entity.ContractorMaterialEntity
import com.example.workermanager.data.entity.ContractorMaterialTransactionEntity
import kotlinx.coroutines.flow.Flow

class ContractorMaterialRepository(
    private val materialDao: ContractorMaterialDao,
    private val transactionDao: ContractorMaterialTransactionDao
) {
    val allMaterials: Flow<List<ContractorMaterialEntity>> = materialDao.getAllMaterials()
    val allTransactions: Flow<List<ContractorMaterialTransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBySite(siteId: Long): Flow<List<ContractorMaterialTransactionEntity>> =
        transactionDao.getTransactionsBySite(siteId)

    fun getTransactionsByMaterial(materialId: Long): Flow<List<ContractorMaterialTransactionEntity>> =
        transactionDao.getTransactionsByMaterial(materialId)

    suspend fun insertMaterial(material: ContractorMaterialEntity): Long =
        materialDao.insertMaterial(material)

    suspend fun updateMaterial(material: ContractorMaterialEntity) =
        materialDao.updateMaterial(material)

    suspend fun deactivateMaterial(id: Long) =
        materialDao.setMaterialActiveStatus(id, active = false)

    suspend fun softDeleteMaterial(id: Long) =
        materialDao.softDeleteMaterial(id)

    suspend fun insertTransaction(transaction: ContractorMaterialTransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: ContractorMaterialTransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(id: Long) =
        transactionDao.softDeleteTransaction(id)
}
