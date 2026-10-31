package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.MaterialDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.MaterialDTO
import com.example.workermanager.data.remote.sync.mapper.MaterialMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object MaterialSyncEngine {
    private const val TAG = "MaterialSyncEngine"

    suspend fun syncUserMaterials(materialDao: MaterialDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val materialsRef = FirestoreProvider.getUserDocumentRef().collection("materials")

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = materialsRef,
                userId = uid,
                collectionName = "materials"
            ) { doc ->
                doc.toObject(MaterialDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }
            val cloudMaterials = pullResult.items

            val allLocalMaterials = materialDao.getAllMaterialsRaw()
            val localMaterialsMap = allLocalMaterials.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudMaterials) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMaterialsMap[cloudDto.syncId]

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = MaterialMapper.toEntity(cloudDto).copy(isPendingSync = false)
                    materialDao.insertMaterial(entity)
                    Log.d(TAG, "Pulled new material from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = MaterialMapper.toEntity(cloudDto, localId = existingLocal.id).copy(isPendingSync = false)
                    materialDao.updateMaterial(updatedEntity)
                    Log.d(TAG, "Updated local material from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligibleMaterials = materialDao.getAllMaterialsRaw().filter { mat ->
                (mat.syncOrigin == SyncOrigin.NEW_LOCAL || mat.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                mat.syncId.isNotBlank() && mat.isPendingSync
            }

            val cloudMaterialsMap = cloudMaterials.associateBy { it.syncId }

            for (localMaterial in eligibleMaterials) {
                val cloudDto = cloudMaterialsMap[localMaterial.syncId]
                if (cloudDto == null || localMaterial.updatedAt >= cloudDto.updatedAt) {
                    val dto = MaterialMapper.toDTO(localMaterial)
                    materialsRef.document(localMaterial.syncId).set(dto, SetOptions.merge()).await()
                    if (localMaterial.deletedAt == -1L) {
                        materialDao.deleteBySyncId(localMaterial.syncId)
                        Log.d(TAG, "Permanently purged material tombstone after cloud ack: ${localMaterial.syncId}")
                    } else {
                        materialDao.updateMaterial(localMaterial.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed local material to cloud: ${localMaterial.name} (${localMaterial.syncId})")
                    }
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "materials", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user materials", e)
            Result.failure(e)
        }
    }
}
