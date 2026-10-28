package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.dao.WorkerSiteHistoryDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.WorkerSiteHistoryDTO
import com.example.workermanager.data.remote.sync.mapper.WorkerSiteHistoryMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object WorkerSiteHistorySyncEngine {
    private const val TAG = "WorkerSiteHistorySyncEngine"

    suspend fun syncWorkerSiteHistory(
        historyDao: WorkerSiteHistoryDao,
        workerDao: WorkerDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val historyRef = FirestoreProvider.getUserDocumentRef().collection("workerSiteHistory")

            val cloudSnapshot = historyRef.get().await()
            val cloudHistory = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkerSiteHistoryDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = historyDao.getAllHistory()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudHistory) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedWorkerId = if (cloudDto.workerSyncId.isNotBlank()) workerDao.getWorkerBySyncId(cloudDto.workerSyncId)?.id else null
                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null

                if (resolvedWorkerId == null || resolvedSiteId == null) continue

                if (existingLocal == null) {
                    val entity = WorkerSiteHistoryMapper.toEntity(
                        cloudDto,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    historyDao.insert(entity)
                    Log.d(TAG, "Pulled worker site history: (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = WorkerSiteHistoryMapper.toEntity(
                        cloudDto,
                        localId = existingLocal.id,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    historyDao.update(updatedEntity)
                    Log.d(TAG, "Updated worker site history: (${cloudDto.syncId})")
                }
            }

            val eligible = historyDao.getAllHistory().filter { h ->
                (h.syncOrigin == SyncOrigin.NEW_LOCAL || h.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                h.syncId.isNotBlank() && h.isPendingSync
            }

            val cloudMap = cloudHistory.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val workerSyncId = workerDao.getAllWorkersRaw().find { it.id == localItem.workerId }?.syncId ?: ""
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    if (workerSyncId.isBlank() || siteSyncId.isBlank()) continue

                    val dto = WorkerSiteHistoryMapper.toDTO(localItem, workerSyncId = workerSyncId, siteSyncId = siteSyncId)
                    historyRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    historyDao.update(localItem.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed worker site history: (${localItem.syncId})")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing worker site history", e)
            Result.failure(e)
        }
    }
}
