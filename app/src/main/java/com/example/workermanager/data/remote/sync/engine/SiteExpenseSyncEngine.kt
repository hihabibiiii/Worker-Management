package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.dao.SiteDao
import com.example.workermanager.data.dao.SiteExpenseDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.SiteExpenseDTO
import com.example.workermanager.data.remote.sync.mapper.SiteExpenseMapper
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SiteExpenseSyncEngine {
    private const val TAG = "SiteExpenseSyncEngine"

    suspend fun syncSiteExpenses(
        expenseDao: SiteExpenseDao,
        siteDao: SiteDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirestoreProvider.requireAuthenticatedUid()
            val expRef = FirestoreProvider.getUserDocumentRef().collection("siteExpenses")

            val cloudSnapshot = expRef.get().await()
            val cloudExpenses = cloudSnapshot.documents.mapNotNull { doc ->
                doc.toObject(SiteExpenseDTO::class.java)?.apply {
                    if (syncId.isBlank()) syncId = doc.id
                }
            }

            val allLocal = expenseDao.getAllExpensesRaw()
            val localMap = allLocal.filter { it.syncId.isNotBlank() }.associateBy { it.syncId }

            for (cloudDto in cloudExpenses) {
                if (cloudDto.syncId.isBlank()) continue
                val existingLocal = localMap[cloudDto.syncId]

                val resolvedSiteId = if (cloudDto.siteSyncId.isNotBlank()) siteDao.getSiteBySyncId(cloudDto.siteSyncId)?.id else null
                if (resolvedSiteId == null) continue

                if (existingLocal == null) {
                    val entity = SiteExpenseMapper.toEntity(cloudDto, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    expenseDao.insertExpense(entity)
                    Log.d(TAG, "Pulled expense: ${cloudDto.description} (${cloudDto.syncId})")
                } else if (cloudDto.updatedAt > existingLocal.updatedAt) {
                    val updatedEntity = SiteExpenseMapper.toEntity(cloudDto, localId = existingLocal.id, localSiteId = resolvedSiteId).copy(isPendingSync = false)
                    expenseDao.updateExpense(updatedEntity)
                    Log.d(TAG, "Updated expense: ${cloudDto.description} (${cloudDto.syncId})")
                }
            }

            val eligible = expenseDao.getAllExpensesRaw().filter { e ->
                (e.syncOrigin == SyncOrigin.NEW_LOCAL || e.syncOrigin == SyncOrigin.CLOUD_SYNCED) &&
                e.syncId.isNotBlank() && e.isPendingSync
            }

            val cloudMap = cloudExpenses.associateBy { it.syncId }

            for (localItem in eligible) {
                val cloudDto = cloudMap[localItem.syncId]
                if (cloudDto == null || localItem.updatedAt > cloudDto.updatedAt) {
                    val siteSyncId = siteDao.getAllSitesRaw().find { it.id == localItem.siteId }?.syncId ?: ""
                    if (siteSyncId.isBlank()) continue

                    val dto = SiteExpenseMapper.toDTO(localItem, siteSyncId = siteSyncId)
                    expRef.document(localItem.syncId).set(dto, SetOptions.merge()).await()
                    expenseDao.updateExpense(localItem.copy(isPendingSync = false))
                    Log.d(TAG, "Pushed expense: ${localItem.description} (${localItem.syncId})")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing site expenses", e)
            Result.failure(e)
        }
    }
}
