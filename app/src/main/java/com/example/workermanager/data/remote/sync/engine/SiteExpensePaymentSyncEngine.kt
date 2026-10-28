package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteExpenseDao
import com.example.workermanager.data.dao.SiteExpensePaymentDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.SiteExpensePaymentDTO
import com.example.workermanager.data.remote.sync.mapper.SiteExpensePaymentMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SiteExpensePaymentSyncEngine {
    private const val TAG = "SiteExpensePaymentSyncEngine"

    suspend fun syncSiteExpensePayments(
        expensePaymentDao: SiteExpensePaymentDao,
        expenseDao: SiteExpenseDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val expPayRef = FirestoreProvider.getUserDocumentRef().collection("siteExpensePayments")

            val cloudSnapshot = expPayRef.get().await()
            val cloudPayments = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SiteExpensePaymentDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = expensePaymentDao.getAllPaymentsRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudPayments) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedExpenseId = if (cloudDto.expenseSyncId.isNotBlank()) expenseDao.getExpenseBySyncId(cloudDto.expenseSyncId)?.id else null
                if (resolvedExpenseId == null) continue

                if (existingLocal == null) {
                    if (cloudDto.isDeleted) continue
                    val entity = SiteExpensePaymentMapper.toEntity(cloudDto, localExpenseId = resolvedExpenseId).copy(isPendingSync = false)
                    expensePaymentDao.insertPayment(entity)
                    Log.d(TAG, "Pulled expense payment: ${cloudDto.amount} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = SiteExpensePaymentMapper.toEntity(cloudDto, localId = existingLocal.id, localExpenseId = resolvedExpenseId).copy(isPendingSync = false)
                    expensePaymentDao.updatePayment(updatedEntity)
                    Log.d(TAG, "Updated expense payment: ${cloudDto.amount} (${cloudDto.syncId})")
                }
            }

            val eligible = expensePaymentDao.getAllPaymentsRaw().filter { ep ->
                (ep.syncOrigin == SyncOrigin.NEW_LOCAL || ep.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                ep.syncId.isNotBlank() && ep.isPendingSync
            }

            val cloudMap = cloudPayments.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val expenseSyncId = expenseDao.getAllExpensesRaw().find { it.id == localItem.expenseId }?.syncId ?: ""
                    if (expenseSyncId.isBlank()) continue

                    val dto = SiteExpensePaymentMapper.toDTO(localItem, expenseSyncId = expenseSyncId)
                    expPayRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    if (localItem.deletedAt == -1L) {
                        expensePaymentDao.deleteBySyncId(localItem.syncId)
                        Log.d(TAG, "Permanently purged expense payment tombstone after cloud ack: ${localItem.syncId}")
                    } else {
                        expensePaymentDao.updatePayment(localItem.copy(isPendingSync = false))
                        Log.d(TAG, "Pushed expense payment: ${localItem.amount} (${localItem.syncId})")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing site expense payments", e)
            Result.failure(e)
        }
    }
}
