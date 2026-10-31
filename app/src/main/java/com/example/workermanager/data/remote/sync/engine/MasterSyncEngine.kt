package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.SyncStatusManager
import com.example.workermanager.data.remote.sync.SyncStatusState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

object MasterSyncEngine {
    private const val TAG = "MasterSyncEngine"
    internal val syncMutex = Mutex()

    suspend fun performFullSync(database: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Sync is already in progress. Skipping duplicate concurrent request.")
            return@withContext Result.success(Unit)
        }

        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            Log.d(TAG, "Starting full Cloud Sync for user: $uid")
            SyncStatusManager.updateStatus(SyncStatusState.Syncing)

            // Bootstrap legacy records (idempotent: only processes un-synced legacy records)
            val bootstrapRes = LegacySyncBootstrapper.bootstrapLegacyRecords(database)
            if (bootstrapRes.isFailure) {
                val err = bootstrapRes.exceptionOrNull() ?: Exception("Legacy sync bootstrap failed")
                Log.e(TAG, "Legacy sync bootstrap failed, aborting full sync: ${err.message}", err)
                return@withContext Result.failure(err)
            }

            // Level 1: Independent Parent Entities
            val res1 = SiteSyncEngine.syncUserSites(database.siteDao())
            if (res1.isFailure) return@withContext Result.failure(res1.exceptionOrNull() ?: Exception("Sites sync failed"))

            val res2 = MaterialSyncEngine.syncUserMaterials(database.materialDao())
            if (res2.isFailure) return@withContext Result.failure(res2.exceptionOrNull() ?: Exception("Materials sync failed"))

            val res3 = ContractorMaterialSyncEngine.syncUserContractorMaterials(database.contractorMaterialDao())
            if (res3.isFailure) return@withContext Result.failure(res3.exceptionOrNull() ?: Exception("Contractor Materials sync failed"))

            // Level 2: Parent-Dependent Entities
            val res4 = WorkerSyncEngine.syncUserWorkers(database.workerDao(), database.siteDao())
            if (res4.isFailure) return@withContext Result.failure(res4.exceptionOrNull() ?: Exception("Workers sync failed"))

            val res5 = ContractSyncEngine.syncUserContracts(database.contractDao(), database.siteDao())
            if (res5.isFailure) return@withContext Result.failure(res5.exceptionOrNull() ?: Exception("Contracts sync failed"))

            // Level 3: Child Records & Transactions
            val res6 = WorkerSiteHistorySyncEngine.syncWorkerSiteHistory(database.workerSiteHistoryDao(), database.workerDao(), database.siteDao())
            if (res6.isFailure) return@withContext Result.failure(res6.exceptionOrNull() ?: Exception("Worker Site History sync failed"))

            val res7 = AttendanceSyncEngine.syncUserAttendance(database.attendanceDao(), database.workerDao(), database.siteDao())
            if (res7.isFailure) return@withContext Result.failure(res7.exceptionOrNull() ?: Exception("Attendance sync failed"))

            val res8 = PaymentSyncEngine.syncUserPayments(database.paymentDao(), database.workerDao(), database.siteDao())
            if (res8.isFailure) return@withContext Result.failure(res8.exceptionOrNull() ?: Exception("Payments sync failed"))

            val res9 = ClientPaymentSyncEngine.syncUserClientPayments(database.clientPaymentDao(), database.contractDao())
            if (res9.isFailure) return@withContext Result.failure(res9.exceptionOrNull() ?: Exception("Client Payments sync failed"))

            val res10 = SiteAttendanceSyncEngine.syncSiteAttendance(database.siteAttendanceDao(), database.siteDao())
            if (res10.isFailure) return@withContext Result.failure(res10.exceptionOrNull() ?: Exception("Site Attendance sync failed"))

            val res11 = SiteExpenseSyncEngine.syncSiteExpenses(database.siteExpenseDao(), database.siteDao())
            if (res11.isFailure) return@withContext Result.failure(res11.exceptionOrNull() ?: Exception("Site Expenses sync failed"))

            val res12 = SiteExpensePaymentSyncEngine.syncSiteExpensePayments(database.siteExpensePaymentDao(), database.siteExpenseDao())
            if (res12.isFailure) return@withContext Result.failure(res12.exceptionOrNull() ?: Exception("Site Expense Payments sync failed"))

            val res13 = MaterialTransactionSyncEngine.syncMaterialTransactions(database.materialTransactionDao(), database.materialDao(), database.siteDao())
            if (res13.isFailure) return@withContext Result.failure(res13.exceptionOrNull() ?: Exception("Material Transactions sync failed"))

            val res14 = ContractorMaterialTransactionSyncEngine.syncContractorMaterialTransactions(database.contractorMaterialTransactionDao(), database.contractorMaterialDao(), database.siteDao())
            if (res14.isFailure) return@withContext Result.failure(res14.exceptionOrNull() ?: Exception("Contractor Material Transactions sync failed"))

            Log.d(TAG, "Full Cloud Sync completed successfully for all 14 entities for user: $uid")
            SyncStatusManager.updateStatus(SyncStatusState.Synced)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "MasterSyncEngine failed", e)
            SyncStatusManager.updateStatus(SyncStatusState.Error(e.message ?: "Sync failed"))
            Result.failure(e)
        } finally {
            syncMutex.unlock()
        }
    }
}
