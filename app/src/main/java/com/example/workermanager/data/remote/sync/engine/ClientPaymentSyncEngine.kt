package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.ClientPaymentDao
import com.example.workermanager.data.dao.ContractDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.ClientPaymentDTO
import com.example.workermanager.data.remote.sync.mapper.ClientPaymentMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ClientPaymentSyncEngine {
    private const val TAG = "ClientPaymentSyncEngine"

    suspend fun syncUserClientPayments(clientPaymentDao: ClientPaymentDao, contractDao: ContractDao): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val cPayRef = FirestoreProvider.getUserDocumentRef().collection("clientPayments")

            val cloudSnapshot = cPayRef.get().await()
            val cloudPayments = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(ClientPaymentDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = clientPaymentDao.getAllClientPaymentsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudPayments) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedContractId = if (cloudDto.contractSyncId.isNotBlank()) {
                    contractDao.getContractBySyncId(cloudDto.contractSyncId)?.id
                } else null

                if (resolvedContractId == null) continue

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = ClientPaymentMapper.toEntity(cloudDto, localContractId = resolvedContractId).copy(isPendingSync = false)
                    clientPaymentDao.insertPayment(entity)
                    Log.d(TAG, "Pulled client payment: ${cloudDto.amount} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = ClientPaymentMapper.toEntity(cloudDto, localId = existingLocal.id, localContractId = resolvedContractId).copy(isPendingSync = false)
                    clientPaymentDao.updatePayment(updatedEntity)
                    Log.d(TAG, "Updated local client payment: ${cloudDto.amount} (${cloudDto.syncId})")
                }
            }

            val eligible = clientPaymentDao.getAllClientPaymentsRaw().filter { p ->
                (p.syncOrigin == SyncOrigin.NEW_LOCAL || p.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                p.syncId.isNotBlank() && p.isPendingSync
            }

            val cloudMap = cloudPayments.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val contractSyncId = contractDao.getAllContractsRaw().find { it.id == localItem.contractId }?.syncId ?: ""
                    if (contractSyncId.isBlank()) continue

                    val dto = ClientPaymentMapper.toDTO(localItem, contractSyncId = contractSyncId)
                    cPayRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        clientPaymentDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged client payment tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        clientPaymentDao.updatePayment(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed client payment: ${localItem.amount} (${localItem.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing client payments", e)
            Result.failure(e)
        }
    }
}
