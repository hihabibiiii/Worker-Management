package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.ContractorMaterialDao
import com.example.workermanager.data.dao.ContractorMaterialTransactionDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.ContractorMaterialTransactionDTO
import com.example.workermanager.data.remote.sync.mapper.ContractorMaterialTransactionMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ContractorMaterialTransactionSyncEngine {
    private const val TAG = "ContractorMaterialTransactionSyncEngine"

    suspend fun syncContractorMaterialTransactions(
        transactionDao: ContractorMaterialTransactionDao,
        materialDao: ContractorMaterialDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val cMatTxRef = FirestoreProvider.getUserDocumentRef().collection("contractorMaterialTransactions")

            val cloudSnapshot = cMatTxRef.get().await()
            val cloudTx = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(ContractorMaterialTransactionDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = transactionDao.getAllTransactionsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudTx) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedMaterialId = if (cloudDto.materialSyncId.isNotBlank()) materialDao.getMaterialBySyncId(cloudDto.materialSyncId)?.id else null
                val resolvedSiteId = if (!cloudDto.siteSyncId.isNullOrBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId!!)?.id else null

                if (resolvedMaterialId == null) continue

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = ContractorMaterialTransactionMapper.toEntity(
                        cloudDto,
                        localMaterialId = resolvedMaterialId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    transactionDao.insertTransaction(entity)
                    Log.d(TAG, "Pulled contractor material transaction: (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = ContractorMaterialTransactionMapper.toEntity(
                        cloudDto,
                        localId = existingLocal.id,
                        localMaterialId = resolvedMaterialId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    transactionDao.updateTransaction(updatedEntity)
                    Log.d(TAG, "Updated contractor material transaction: (${cloudDto.syncId})")
                }
            }

            val eligible = transactionDao.getAllTransactionsRaw().filter { tx ->
                (tx.syncOrigin == SyncOrigin.NEW_LOCAL || tx.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                tx.syncId.isNotBlank() && tx.isPendingSync
            }

            val cloudMap = cloudTx.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val matSyncId = materialDao.getAllMaterialsRaw().find { it.id == localItem.materialId }?.syncId ?: ""
                    val siteSyncId = if (localItem.siteId != null) siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId else null

                    if (matSyncId.isBlank()) continue

                    val dto = ContractorMaterialTransactionMapper.toDTO(
                        localItem,
                        materialSyncId = matSyncId,
                        siteSyncId = siteSyncId
                    )
                    cMatTxRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        transactionDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged contractor material tx tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        transactionDao.updateTransaction(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed contractor material transaction: (${localItem.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing contractor material transactions", e)
            Result.failure(e)
        }
    }
}
