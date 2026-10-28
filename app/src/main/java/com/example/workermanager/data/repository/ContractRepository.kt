package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ContractDao
import com.example.workermanager.data.entity.ContractEntity
import com.example.workermanager.data.entity.SyncOrigin
import kotlinx.coroutines.flow.Flow

class ContractRepository(
    private val contractDao: ContractDao,
    private val onDataChanged: (() -> Unit)? = null
) {
    val allContracts: Flow<List<ContractEntity>> = contractDao.getAllContracts()

    fun getContractById(id: Long): Flow<ContractEntity?> = contractDao.getContractById(id)

    fun getContractsBySite(siteId: Long): Flow<List<ContractEntity>> = contractDao.getContractsBySite(siteId)

    suspend fun insertContract(contract: ContractEntity): Long {
        val id = contractDao.insertContract(contract)
        onDataChanged?.invoke()
        return id
    }

    suspend fun updateContract(contract: ContractEntity) {
        val updated = if (contract.syncOrigin != SyncOrigin.LEGACY) {
            contract.copy(updatedAt = System.currentTimeMillis(), isPendingSync = true)
        } else contract
        contractDao.updateContract(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteContract(contract: ContractEntity) {
        contractDao.softDeleteContract(contract.id)
        onDataChanged?.invoke()
    }

    suspend fun restoreContract(id: Long) {
        contractDao.restoreContract(id)
        onDataChanged?.invoke()
    }

    fun getDeletedContracts(): Flow<List<ContractEntity>> = contractDao.getDeletedContracts()

    suspend fun permanentlyDeleteContract(id: Long) {
        val item = contractDao.getAllContractsRaw().find { it.id == id }
        if (item != null && item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
            contractDao.markPendingPermanentDelete(id)
        } else {
            contractDao.permanentlyDeleteContract(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyContractTrash() {
        val deleted = contractDao.getAllContractsRaw().filter { it.isDeleted }
        for (item in deleted) {
            if (item.syncOrigin != SyncOrigin.LEGACY && item.syncId.isNotBlank()) {
                contractDao.markPendingPermanentDelete(item.id)
            } else {
                contractDao.permanentlyDeleteContract(item.id)
            }
        }
        onDataChanged?.invoke()
    }
}
