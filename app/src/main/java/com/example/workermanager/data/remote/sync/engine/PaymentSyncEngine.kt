package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.PaymentDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.PaymentDTO
import com.example.workermanager.data.remote.sync.mapper.PaymentMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PaymentSyncEngine {
    private const val TAG = "PaymentSyncEngine"

    suspend fun syncUserPayments(
        paymentDao: PaymentDao,
        workerDao: WorkerDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val payRef = FirestoreProvider.getUserDocumentRef().collection("payments")

            val cloudSnapshot = payRef.get().await()
            val cloudPayments = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(PaymentDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = paymentDao.getAllPaymentsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudPayments) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedWorkerId = if (cloudDto.workerSyncId.isNotBlank()) workerDao.getWorkerBySyncId(cloudDto.workerSyncId)?.id else null
                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null

                if (resolvedWorkerId == null || resolvedSiteId == null) continue

                if (existingLocal == null) {
                    val entity = PaymentMapper.toEntity(
                        cloudDto,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    paymentDao.insertPayment(entity)
                    Log.d(TAG, "Pulled payment: ${cloudDto.amount} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = PaymentMapper.toEntity(
                        cloudDto,
                        localId = existingLocal.id,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    paymentDao.updatePayment(updatedEntity)
                    Log.d(TAG, "Updated payment: ${cloudDto.amount} (${cloudDto.syncId})")
                }
            }

            val eligible = paymentDao.getAllPaymentsRaw().filter { p ->
                (p.syncOrigin == SyncOrigin.NEW_LOCAL || p.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                p.syncId.isNotBlank() && p.isPendingSync
            }

            val cloudMap = cloudPayments.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val workerSyncId = workerDao.getAllWorkersRaw().find { it.id == localItem.workerId }?.syncId ?: ""
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    if (workerSyncId.isBlank() || siteSyncId.isBlank()) continue

                    val dto = PaymentMapper.toDTO(localItem, workerSyncId = workerSyncId, siteSyncId = siteSyncId)
                    payRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    paymentDao.updatePayment(localItem.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed payment: ${localItem.amount} (${localItem.syncId})")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing payments", e)
            Result.failure(e)
        }
    }
}
