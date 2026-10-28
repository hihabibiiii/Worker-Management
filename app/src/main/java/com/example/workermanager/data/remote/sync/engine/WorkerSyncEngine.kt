package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.WorkerDTO
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object WorkerSyncEngine {
    private const val TAG = "WorkerSyncEngine"

    suspend fun syncUserWorkers(workerDao: WorkerDao, siteDao: SiteDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val workersRef = FirestoreProvider.getUserDocumentRef().collection("workers")

            // 1. Down-sync (Pull Cloud -> Local)
            val cloudSnapshot = workersRef.get().await()
            val cloudWorkers = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkerDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocalWorkers = workerDao.getAllWorkersRaw()
            val localWorkersMap = allLocalWorkers.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudWorkers) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localWorkersMap[cloudDto.syncId]

                val resolvedSiteId = if (!cloudDto.siteSyncId.isNullOrBlank()) {
                    siteDao.getSiteBySyncId(cloudDto.siteSyncId!!)?.id
                } else null

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = WorkerMapper.toEntity(cloudDto, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    workerDao.insertWorker(entity)
                    Log.d(TAG, "Pulled new worker from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = WorkerMapper.toEntity(cloudDto, localId = existingLocal.id, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    workerDao.updateWorker(updatedEntity)
                    Log.d(TAG, "Updated local worker from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligibleWorkers = workerDao.getAllWorkersRaw().filter { worker ->
                (worker.syncOrigin == SyncOrigin.NEW_LOCAL || worker.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                worker.syncId.isNotBlank() && worker.isPendingSync
            }

            val cloudWorkersMap = cloudWorkers.associateBy { it.syncId }

            for (localWorker in eligibleWorkers) {
                val cloudDto = cloudWorkersMap[localWorker.syncId]
                if (cloudDto == null || localWorker.updatedAt > cloudDto.updatedAt) {
                    val siteSyncId = if (localWorker.currentSiteId != null) {
                        siteDao.getAllSitesRaw().find { it.id == localWorker.currentSiteId }?.syncId
                    } else null

                    val dto = WorkerMapper.toDTO(localWorker, siteSyncId = siteSyncId)
                    workersRef.document(localWorker.syncId).set(dto, SetOptions.merge()).await()
                    if (localWorker.deletedAt == -1L) {
                        workerDao.deleteBySyncId(localWorker.syncId)
                        Log.d(TAG, "Permanently purged worker tombstone after cloud ack: ${localWorker.syncId}")
                    } else {
                        workerDao.updateWorker(localWorker.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed local worker to cloud: ${localWorker.name} (${localWorker.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user workers", e)
            Result.failure(e)
        }
    }
}
