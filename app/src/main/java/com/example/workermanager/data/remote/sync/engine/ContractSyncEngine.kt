package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.ContractDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.ContractDTO
import com.example.workermanager.data.remote.sync.mapper.ContractMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ContractSyncEngine {
    private const val TAG = "ContractSyncEngine"

    suspend fun syncUserContracts(contractDao: ContractDao, siteDao: SiteDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val contractsRef = FirestoreProvider.getUserDocumentRef().collection("contracts")

            // 1. Down-sync (Pull Cloud -> Local)
            val cloudSnapshot = contractsRef.get().await()
            val cloudContracts = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(ContractDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = contractDao.getAllContractsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudContracts) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedSiteId = if (!cloudDto.siteSyncId.isNullOrBlank()) {
                    siteDao.getSiteBySyncId(cloudDto.siteSyncId!!)?.id
                } else null

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = ContractMapper.toEntity(cloudDto, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    contractDao.insertContract(entity)
                    Log.d(TAG, "Pulled new contract: ${cloudDto.clientName} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = ContractMapper.toEntity(cloudDto, localId = existingLocal.id, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    contractDao.updateContract(updatedEntity)
                    Log.d(TAG, "Updated local contract: ${cloudDto.clientName} (${cloudDto.syncId})")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligible = contractDao.getAllContractsRaw().filter { c ->
                (c.syncOrigin == SyncOrigin.NEW_LOCAL || c.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                c.syncId.isNotBlank() && c.isPendingSync
            }

            val cloudMap = cloudContracts.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val siteSyncId = if (localItem.siteId != null) {
                        siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId
                    } else null

                    val dto = ContractMapper.toDTO(localItem, siteSyncId = siteSyncId)
                    contractsRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        contractDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged contract tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        contractDao.updateContract(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed contract: ${localItem.clientName} (${localItem.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing contracts", e)
            Result.failure(e)
        }
    }
}
