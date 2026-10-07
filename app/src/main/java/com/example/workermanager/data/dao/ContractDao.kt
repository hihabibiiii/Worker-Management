package com.example.workermanager.data.dao

import androidx.room.*
import com.example.workermanager.data.entity.ContractEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts ORDER BY startDate DESC")
    fun getAllContracts(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE id = :id")
    fun getContractById(id: Long): Flow<ContractEntity?>

    @Query("SELECT * FROM contracts WHERE siteId = :siteId")
    fun getContractsBySite(siteId: Long): Flow<List<ContractEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity): Long

    @Update
    suspend fun updateContract(contract: ContractEntity)

    @Delete
    suspend fun deleteContract(contract: ContractEntity)
}
