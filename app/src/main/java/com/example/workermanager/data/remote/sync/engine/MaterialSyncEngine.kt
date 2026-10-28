package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.MaterialDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
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
            FirestoreProvider.requireAuthenticatedUid()
            val materialsRef = FirestoreProvider.getUserDocumentRef().collection("materials")

            // 1. Down-sync (Pull Cloud -> Local)
            val cloudSnapshot = materialsRef.get().await()
            val cloudMaterials = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(MaterialDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocalMaterials = materialDao.getAllMaterialsRaw()
            val localMaterialsMap = allLocalMaterials.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudMaterials) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMaterialsMap[cloudDto.syncId]

                if (existingLocal == null) {
                    val entity = MaterialMapper.toEntity(cloudDto)
                    materialDao.insertMaterial(entity)
                    Log.d(TAG, "Pulled new material from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = MaterialMapper.toEntity(cloudDto, localId = existingLocal.id)
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
                if (cloudDto == null || localMaterial.updatedAt > cloudDto.updatedAt) {
                    val dto = MaterialMapper.toDTO(localMaterial)
                    materialsRef.document(localMaterial.syncId).set(dto, SetOptions.merge()).await()
                    materialDao.updateMaterial(localMaterial.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed local material to cloud: ${localMaterial.name} (${localMaterial.syncId})")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user materials", e)
            Result.failure(e)
        }
    }
}
