package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.MaterialDao
import com.example.workermanager.data.dao.MaterialTransactionDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.MaterialTransactionDTO
import com.example.workermanager.data.remote.sync.mapper.MaterialTransactionMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object MaterialTransactionSyncEngine {
    private const val TAG = "MaterialTransactionSyncEngine"

    suspend fun syncMaterialTransactions(
        transactionDao: MaterialTransactionDao,
        materialDao: MaterialDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val matTxRef = FirestoreProvider.getUserDocumentRef().collection("materialTransactions")

            val cloudSnapshot = matTxRef.get().await()
            val cloudTx = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(MaterialTransactionDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = transactionDao.getAllTransactionsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudTx) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedMaterialId = if (cloudDto.materialSyncId.isNotBlank()) materialDao.getMaterialBySyncId(cloudDto.materialSyncId)?.id else null
                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null

                if (resolvedMaterialId == null || resolvedSiteId == null) continue

                val resolvedTargetSiteId = if (!cloudDto.targetSiteSyncId.isNullOrBlank()) siteDao.getSiteBySyncId(cloudDto.targetSiteSyncId!!)?.id else null

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = MaterialTransactionMapper.toEntity(
                        cloudDto,
                        localMaterialId = resolvedMaterialId,
                        localSiteId = resolvedSiteId,
                        localTargetSiteId = resolvedTargetSiteId
                    ).copy(isPendingSync = false)
                    transactionDao.insertTransaction(entity)
                    Log.d(TAG, "Pulled material transaction: (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = MaterialTransactionMapper.toEntity(
                        cloudDto,
                        localId = existingLocal.id,
                        localMaterialId = resolvedMaterialId,
                        localSiteId = resolvedSiteId,
                        localTargetSiteId = resolvedTargetSiteId
                    ).copy(isPendingSync = false)
                    transactionDao.updateTransaction(updatedEntity)
                    Log.d(TAG, "Updated material transaction: (${cloudDto.syncId})")
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
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    val targetSiteSyncId = if (localItem.targetSiteId != null) siteDao.getAllSitesRaw().find { it.id == localItem.targetSiteId }?.syncId else null

                    if (matSyncId.isBlank() || siteSyncId.isBlank()) continue

                    val dto = MaterialTransactionMapper.toDTO(
                        localItem,
                        materialSyncId = matSyncId,
                        siteSyncId = siteSyncId,
                        targetSiteSyncId = targetSiteSyncId
                    )
                    matTxRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        transactionDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged material transaction tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        transactionDao.updateTransaction(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed material transaction: (${localItem.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing material transactions", e)
            Result.failure(e)
        }
    }
}
