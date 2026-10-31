package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.AttendanceDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.AttendanceDTO
import com.example.workermanager.data.remote.sync.mapper.AttendanceMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AttendanceSyncEngine {
    private const val TAG = "AttendanceSyncEngine"

    suspend fun syncUserAttendance(
        attendanceDao: AttendanceDao,
        workerDao: WorkerDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val attRef = FirestoreProvider.getUserDocumentRef().collection("attendance")

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = attRef,
                userId = uid,
                collectionName = "attendance"
            ) { doc ->
                doc.toObject(AttendanceDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }
            val cloudAttendance = pullResult.items

            val allLocal = attendanceDao.getAllAttendanceRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudAttendance) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedWorkerId = if (cloudDto.workerSyncId.isNotBlank()) workerDao.getWorkerBySyncId(cloudDto.workerSyncId)?.id else null
                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null

                if (resolvedWorkerId == null || resolvedSiteId == null) continue

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = AttendanceMapper.toEntity(
                        cloudDto,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    attendanceDao.insertAttendance(entity)
                    Log.d(TAG, "Pulled attendance: ${cloudDto.date} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = AttendanceMapper.toEntity(
                        cloudDto,
                        localId = existingLocal.id,
                        localWorkerId = resolvedWorkerId,
                        localSiteId = resolvedSiteId
                    ).copy(isPendingSync = false)
                    attendanceDao.updateAttendance(updatedEntity)
                    Log.d(TAG, "Updated attendance: ${cloudDto.date} (${cloudDto.syncId}) isDeleted=${cloudDto.isDeleted}")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligible = attendanceDao.getAllAttendanceRaw().filter { a ->
                (a.syncOrigin == SyncOrigin.NEW_LOCAL || a.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                a.syncId.isNotBlank() && a.isPendingSync
            }

            val cloudMap = cloudAttendance.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt >= cloudDto.updatedAt) {
                    val workerSyncId = workerDao.getAllWorkersRaw().find { it.id == localItem.workerId }?.syncId ?: ""
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    if (workerSyncId.isBlank() || siteSyncId.isBlank()) continue

                    val dto = AttendanceMapper.toDTO(localItem, workerSyncId = workerSyncId, siteSyncId = siteSyncId)
                    attRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        attendanceDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged attendance tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        attendanceDao.updateAttendance(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed attendance: ${localItem.date} (${localItem.syncId}) isDeleted=${localItem.isDeleted}")
                    }
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "attendance", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing attendance", e)
            Result.failure(e)
        }
    }
}
