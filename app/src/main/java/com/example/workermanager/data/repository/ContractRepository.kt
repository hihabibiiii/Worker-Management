package com.example.workermanager.data.repository

import com.example.workermanager.data.dao.ContractDao
import com.example.workermanager.data.entity.ContractEntity
import kotlinx.coroutines.flow.Flow

class ContractRepository(private val contractDao: ContractDao) {
    val allContracts: Flow<List<ContractEntity>> = contractDao.getAllContracts()

    fun getContractById(id: Long): Flow<ContractEntity?> = contractDao.getContractById(id)

    fun getContractsBySite(siteId: Long): Flow<List<ContractEntity>> = contractDao.getContractsBySite(siteId)

    suspend fun insertContract(contract: ContractEntity): Long = contractDao.insertContract(contract)

    suspend fun updateContract(contract: ContractEntity) = contractDao.updateContract(contract)

    suspend fun deleteContract(contract: ContractEntity) = contractDao.deleteContract(contract)
}
