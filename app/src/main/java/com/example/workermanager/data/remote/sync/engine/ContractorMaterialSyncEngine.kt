package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.ContractorMaterialDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.FirestoreSyncHelper
import com.example.workermanager.data.remote.sync.SyncCursorManager
import com.example.workermanager.data.remote.sync.dto.ContractorMaterialDTO
import com.example.workermanager.data.remote.sync.mapper.ContractorMaterialMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ContractorMaterialSyncEngine {
    private const val TAG = "ContractorMaterialSyncEngine"

    suspend fun syncUserContractorMaterials(contractorMaterialDao: ContractorMaterialDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            val cMatRef = FirestoreProvider.getUserDocumentRef().collection("contractorMaterials")

            // 1. Paginated Incremental Pull (Cloud -> Local)
            val pullResult = FirestoreSyncHelper.pullIncremental(
                collectionRef = cMatRef,
                userId = uid,
                collectionName = "contractorMaterials"
            ) { doc ->
                doc.toObject(ContractorMaterialDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }
            val cloudMaterials = pullResult.items

            val allLocalMaterials = contractorMaterialDao.getAllMaterialsRaw()
            val localMaterialsMap = allLocalMaterials.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudMaterials) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMaterialsMap[cloudDto.syncId]

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = ContractorMaterialMapper.toEntity(cloudDto).copy(isPendingSync = false)
                    contractorMaterialDao.insertMaterial(entity)
                    Log.d(TAG, "Pulled new contractor material from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = ContractorMaterialMapper.toEntity(cloudDto, localId = existingLocal.id).copy(isPendingSync = false)
                    contractorMaterialDao.updateMaterial(updatedEntity)
                    Log.d(TAG, "Updated local contractor material from cloud: ${cloudDto.name} (${cloudDto.syncId})")
                }
            }

            // 2. Outbound Push (Local -> Cloud)
            val eligibleMaterials = contractorMaterialDao.getAllMaterialsRaw().filter { mat ->
                (mat.syncOrigin == SyncOrigin.NEW_LOCAL || mat.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                mat.syncId.isNotBlank() && mat.isPendingSync
            }

            val cloudMaterialsMap = cloudMaterials.associateBy { it.syncId }

            for (localMaterial in eligibleMaterials) {
                val cloudDto = cloudMaterialsMap[localMaterial.syncId]
                if (cloudDto == null || localMaterial.updatedAt >= cloudDto.updatedAt) {
                    val dto = ContractorMaterialMapper.toDTO(localMaterial)
                    cMatRef.document(localMaterial.syncId).set(dto, SetOptions.merge()).await()
                    if (localMaterial.deletedAt == -1L) {
                        contractorMaterialDao.deleteBySyncId(localMaterial.syncId)
                        Log.d(TAG, "Permanently purged contractor material tombstone after cloud ack: ${localMaterial.syncId}")
                    } else {
                        contractorMaterialDao.updateMaterial(localMaterial.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed local contractor material to cloud: ${localMaterial.name} (${localMaterial.syncId})")
                    }
                }
            }

            // 3. Save sync cursor high-water mark
            SyncCursorManager.setLastSyncTimestamp(uid, "contractorMaterials", pullResult.highestUpdatedAt)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing contractor materials", e)
            Result.failure(e)
        }
    }
}
