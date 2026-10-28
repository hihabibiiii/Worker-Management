package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.SiteDTO
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SiteSyncEngine {
    private const val TAG = "SiteSyncEngine"

    suspend fun syncUserSites(siteDao: SiteDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val sitesRef = FirestoreProvider.getUserDocumentRef().collection("sites")

            // 1. Cloud Inspection & Down-Sync (Pull First Anti-Wipe Safeguard)
            val cloudSnapshot = sitesRef.get().await()
            val cloudSites = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SiteDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocalSites = siteDao.getAllSitesRaw()
            val localSitesMap = allLocalSites.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            // Process Cloud -> Local Pull
            for (cloudDto in cloudSites) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localSitesMap[cloudDto.syncId]

                if (existingLocal == null) {
                    // New cloud record -> Insert into local Room
                    val entity = SiteMapper.toEntity(cloudDto).copy(isPendingSync = false)
                    siteDao.insertSite(entity)
                    Log.d(TAG, "Pulled new site from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    // Cloud record is newer -> Update local Room
                    val updatedEntity = SiteMapper.toEntity(cloudDto, localId = existingLocal.id).copy(isPendingSync = false)
                    siteDao.updateSite(updatedEntity)
                    Log.d(TAG, "Updated local site from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                }
            }

            // 2. Local -> Cloud Outbound Push (ONLY NEW_LOCAL or CLOUD_SYNCED with isPendingSync == true)
            val eligibleLocalSites = siteDao.getAllSitesRaw().filter { site ->
                (site.syncOrigin == SyncOrigin.NEW_LOCAL || site.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                site.syncId.isNotBlank() && site.isPendingSync
            }

            val cloudSitesMap = cloudSites.associateBy { it.syncId }

            for (localSite in eligibleLocalSites) {
                val cloudDto = cloudSitesMap[localSite.syncId]
                if (cloudDto == null || localSite.updatedAt > cloudDto.updatedAt) {
                    val dto = SiteMapper.toDTO(localSite)
                    sitesRef.document(localSite.syncId).set(dto, SetOptions.merge()).await()
                    siteDao.updateSite(localSite.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed local site to cloud: ${localSite.name} (${localSite.syncId})")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user sites", e)
            Result.failure(e)
        }
    }
}
