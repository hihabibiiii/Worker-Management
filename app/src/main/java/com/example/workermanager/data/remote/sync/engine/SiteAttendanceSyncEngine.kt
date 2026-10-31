package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteAttendanceDao
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.SiteAttendanceDTO
import com.example.workermanager.data.remote.sync.mapper.SiteAttendanceMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SiteAttendanceSyncEngine {
    private const val TAG = "SiteAttendanceSyncEngine"

    suspend fun syncSiteAttendance(
        siteAttendanceDao: SiteAttendanceDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val siteAttRef = FirestoreProvider.getUserDocumentRef().collection("siteAttendance")

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = siteAttRef,
                userId = uid,
                collectionName = "siteAttendance"
            ) { doc ->
                doc.toObject(SiteAttendanceDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }
            val cloudSiteAttendance = pullResult.items

            val allLocal = siteAttendanceDao.getAllSiteAttendance()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudSiteAttendance) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null
                if (resolvedSiteId == null) continue

                if (existingLocal == null) {
                    val entity = SiteAttendanceMapper.toEntity(cloudDto, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    siteAttendanceDao.insertSiteAttendance(entity)
                    Log.d(TAG, "Pulled site attendance: (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = SiteAttendanceMapper.toEntity(cloudDto, localId = existingLocal.id, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    siteAttendanceDao.insertSiteAttendance(updatedEntity)
                    Log.d(TAG, "Updated site attendance: (${cloudDto.syncId})")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligible = siteAttendanceDao.getAllSiteAttendance().filter { sa ->
                (sa.syncOrigin == SyncOrigin.NEW_LOCAL || sa.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                sa.syncId.isNotBlank() && sa.isPendingSync
            }

            val cloudMap = cloudSiteAttendance.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt >= cloudDto.updatedAt) {
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    if (siteSyncId.isBlank()) continue

                    val dto = SiteAttendanceMapper.toDTO(localItem, siteSyncId = siteSyncId)
                    siteAttRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    siteAttendanceDao.insertSiteAttendance(localItem.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed site attendance: (${localItem.syncId})")
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "siteAttendance", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing site attendance", e)
            Result.failure(e)
        }
    }
}
