package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.data.database.AppDatabase
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.dto.*
import com.example.workermanager.data.remote.sync.mapper.SiteMapper
import com.example.workermanager.data.remote.sync.mapper.WorkerMapper
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object LegacySyncBootstrapper {
    private const val TAG = "LegacySyncBootstrapper"

    fun normalizeName(name: String?): String {
        return name?.trim()?.replace("\\s+".toRegex(), " ")?.lowercase() ?: ""
    }

    fun normalizePhone(phone: String): String {
        return phone.filter { it.isDigit() }
    }

    fun isWorkerMatch(worker: WorkerEntity, cloudDto: WorkerDTO): Boolean {
        val localName = normalizeName(worker.name)
        val cloudName = normalizeName(cloudDto.name)
        if (localName.isBlank() || localName != cloudName) {
            return false
        }

        val localPhone = normalizePhone(worker.phone)
        val cloudPhone = normalizePhone(cloudDto.phone)

        // Conservative identity matching:
        // - Phone must be non-blank and normalized.
        // - Name must also match after normalization.
        // - Worker type alone must never establish identity.
        // - If phone number is missing in either record, do not guess.
        if (localPhone.isBlank() || cloudPhone.isBlank()) {
            return false
        }

        return localPhone == cloudPhone
    }

    fun isSiteMatch(localSite: SiteEntity, cloudDto: SiteDTO): Boolean {
        val localName = normalizeName(localSite.name)
        val cloudName = normalizeName(cloudDto.name)
        if (localName.isBlank() || localName != cloudName) {
            return false
        }

        val localAddress = normalizeName(localSite.address)
        val cloudAddress = normalizeName(cloudDto.address)

        val localManager = normalizeName(localSite.manager)
        val cloudManager = normalizeName(cloudDto.manager)

        val localStartDate = localSite.startDate.toString().trim()
        val cloudStartDate = cloudDto.startDate.trim()

        // Any explicit conflict in attributes immediately invalidates the match
        if (localAddress.isNotBlank() && cloudAddress.isNotBlank() && localAddress != cloudAddress) {
            return false
        }
        if (localManager.isNotBlank() && cloudManager.isNotBlank() && localManager != cloudManager) {
            return false
        }
        if (localStartDate.isNotBlank() && cloudStartDate.isNotBlank() && localStartDate != cloudStartDate) {
            return false
        }

        // Secondary matching attributes: at least one strong secondary attribute must match
        val addressMatches = localAddress.isNotBlank() && localAddress == cloudAddress
        val managerMatches = localManager.isNotBlank() && localManager == cloudManager
        val startDateMatches = localStartDate.isNotBlank() && localStartDate == cloudStartDate

        // Name-only matching must NOT automatically establish identity
        return addressMatches || managerMatches || startDateMatches
    }

    suspend fun hasAnyLegacyRecords(database: AppDatabase): Boolean {
        if (database.siteDao().getAllSitesRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.workerDao().getAllWorkersRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.materialDao().getAllMaterialsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.contractorMaterialDao().getAllMaterialsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.contractDao().getAllContractsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.attendanceDao().getAllAttendanceRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.paymentDao().getAllPaymentsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.clientPaymentDao().getAllClientPaymentsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.siteAttendanceDao().getAllSiteAttendance().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.siteExpenseDao().getAllExpensesRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.siteExpensePaymentDao().getAllPaymentsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.materialTransactionDao().getAllTransactionsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.contractorMaterialTransactionDao().getAllTransactionsRaw().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        if (database.workerSiteHistoryDao().getAllHistory().any { it.syncOrigin == SyncOrigin.LEGACY || it.syncId.isBlank() }) return true
        return false
    }

    suspend fun bootstrapLegacyRecords(database: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = FirestoreProvider.requireAuthenticatedUid()
            if (com.example.workermanager.data.remote.sync.SyncCursorManager.hasBootstrappedLegacy(uid)) {
                Log.d(TAG, "Legacy records already bootstrapped for user: $uid. Skipping.")
                return@withContext Result.success(0)
            }

            if (!hasAnyLegacyRecords(database)) {
                Log.d(TAG, "No legacy records found in local database for user: $uid. Marking bootstrap complete.")
                com.example.workermanager.data.remote.sync.SyncCursorManager.setBootstrappedLegacy(uid, true)
                return@withContext Result.success(0)
            }

            Log.d(TAG, "Starting Legacy Sync Bootstrap for user: $uid")
            var totalBootstrapped = 0

            val userRef = FirestoreProvider.getUserDocumentRef()
            val matchedCloudSyncIds = mutableSetOf<String>()

            // 1. Sites
            val sitesCol = userRef.collection("sites")
            val cloudSites = fetchCloudCollection(sitesCol) { SiteMapper.fromSnapshot(it) }
            val localSites = database.siteDao().getAllSitesRaw()
            for (site in localSites) {
                if (site.syncOrigin == SyncOrigin.LEGACY || site.syncId.isBlank()) {
                    val matchingCandidates = cloudSites.filter { 
                        it.syncId !in matchedCloudSyncIds &&
                        isSiteMatch(site, it) 
                    }
                    if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        val updated = site.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                        database.siteDao().updateSite(updated)
                        totalBootstrapped++
                    } else {
                        // Unresolved Site identity remains pending.
                        // Never create a duplicate cloud Site as a fallback.
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous site match for '${site.name}' (${matchingCandidates.size} candidates). Preserving local record as pending.")
                        } else {
                            Log.w(TAG, "Cannot establish stable cloud identity for legacy site '${site.name}'. Preserving local record as pending.")
                        }
                    }
                }
            }

            // 2. Materials
            val materialsCol = userRef.collection("materials")
            val cloudMaterials = fetchCloudCollection(materialsCol) { it.toObject(MaterialDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localMaterials = database.materialDao().getAllMaterialsRaw()
            for (mat in localMaterials) {
                if (mat.syncOrigin == SyncOrigin.LEGACY || mat.syncId.isBlank()) {
                    val matchingCandidates = cloudMaterials.filter { 
                        it.syncId !in matchedCloudSyncIds &&
                        it.name.trim().equals(mat.name.trim(), ignoreCase = true) && 
                        it.category.equals(mat.category.name, ignoreCase = true)
                    }
                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        mat.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous material match for '${mat.name}'. Treating as separate record.")
                        }
                        mat.copy(
                            syncId = if (mat.syncId.isNotBlank()) mat.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (mat.updatedAt <= 0) (if (mat.createdAt > 0) mat.createdAt else System.currentTimeMillis()) else mat.updatedAt
                        )
                    }
                    database.materialDao().updateMaterial(updated)
                    totalBootstrapped++
                }
            }

            // 3. Contractor Materials
            val cMatCol = userRef.collection("contractorMaterials")
            val cloudCMat = fetchCloudCollection(cMatCol) { it.toObject(ContractorMaterialDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localCMat = database.contractorMaterialDao().getAllMaterialsRaw()
            for (cMat in localCMat) {
                if (cMat.syncOrigin == SyncOrigin.LEGACY || cMat.syncId.isBlank()) {
                    val matchingCandidates = cloudCMat.filter { 
                        it.syncId !in matchedCloudSyncIds &&
                        it.name.trim().equals(cMat.name.trim(), ignoreCase = true) 
                    }
                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        cMat.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous contractor material match for '${cMat.name}'. Treating as separate record.")
                        }
                        cMat.copy(
                            syncId = if (cMat.syncId.isNotBlank()) cMat.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (cMat.updatedAt <= 0) (if (cMat.createdAt > 0) cMat.createdAt else System.currentTimeMillis()) else cMat.updatedAt
                        )
                    }
                    database.contractorMaterialDao().updateMaterial(updated)
                    totalBootstrapped++
                }
            }

            // 4. Workers
            val workersCol = userRef.collection("workers")
            val cloudWorkers = fetchCloudCollection(workersCol) { WorkerMapper.fromSnapshot(it) }
            val localWorkers = database.workerDao().getAllWorkersRaw()
            for (worker in localWorkers) {
                if (worker.syncOrigin == SyncOrigin.LEGACY || worker.syncId.isBlank()) {
                    val matchingCandidates = cloudWorkers.filter {
                        it.syncId !in matchedCloudSyncIds && isWorkerMatch(worker, it)
                    }
                    if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        val updated = worker.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                        database.workerDao().updateWorker(updated)
                        totalBootstrapped++
                    } else {
                        // If phone number is missing or multiple candidates match, do not guess.
                        // Preserve the local record and isPendingSync = true.
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous worker match for '${worker.name}' (${matchingCandidates.size} candidates). Preserving local record as pending.")
                        } else {
                            Log.w(TAG, "Cannot establish stable cloud identity for legacy worker '${worker.name}'. Preserving local record as pending.")
                        }
                    }
                }
            }

            // Refresh sites & workers maps with latest syncIds for child relationship mapping
            val siteIdToSyncId = database.siteDao().getAllSitesRaw().associate { it.id to it.syncId }
            val workerIdToSyncId = database.workerDao().getAllWorkersRaw().associate { it.id to it.syncId }

            // 5. Contracts
            val contractsCol = userRef.collection("contracts")
            val cloudContracts = fetchCloudCollection(contractsCol) { it.toObject(ContractDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localContracts = database.contractDao().getAllContractsRaw()
            for (contract in localContracts) {
                if (contract.syncOrigin == SyncOrigin.LEGACY || contract.syncId.isBlank()) {
                    val siteSyncId = contract.siteId?.let { siteIdToSyncId[it] }
                    val matchingCandidates = cloudContracts.filter {
                        it.syncId !in matchedCloudSyncIds &&
                        it.clientName.trim().equals(contract.clientName.trim(), ignoreCase = true) &&
                        it.startDate == contract.startDate.toString() &&
                        (siteSyncId == null || it.siteSyncId.isNullOrBlank() || it.siteSyncId == siteSyncId)
                    }
                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        contract.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous contract match for '${contract.clientName}'. Treating as separate record.")
                        }
                        contract.copy(
                            syncId = if (contract.syncId.isNotBlank()) contract.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (contract.updatedAt <= 0) System.currentTimeMillis() else contract.updatedAt
                        )
                    }
                    database.contractDao().updateContract(updated)
                    totalBootstrapped++
                }
            }

            val contractIdToSyncId = database.contractDao().getAllContractsRaw().associate { it.id to it.syncId }

            // 6. Attendance
            val attCol = userRef.collection("attendance")
            val cloudAtt = fetchCloudCollection(attCol) { it.toObject(AttendanceDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localAtt = database.attendanceDao().getAllAttendanceRaw()
            for (att in localAtt) {
                if (att.syncOrigin == SyncOrigin.LEGACY || att.syncId.isBlank()) {
                    val wSync = workerIdToSyncId[att.workerId] ?: ""
                    val sSync = siteIdToSyncId[att.siteId] ?: ""
                    val matchingCandidates = if (wSync.isNotBlank() && sSync.isNotBlank()) {
                        cloudAtt.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.workerSyncId == wSync && it.siteSyncId == sSync && it.date == att.date.toString()
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        att.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous attendance match for worker=$wSync, site=$sSync, date=${att.date}.")
                        }
                        att.copy(
                            syncId = if (att.syncId.isNotBlank()) att.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (att.updatedAt <= 0) (if (att.createdAt > 0) att.createdAt else System.currentTimeMillis()) else att.updatedAt
                        )
                    }
                    database.attendanceDao().updateAttendance(updated)
                    totalBootstrapped++
                }
            }

            // 7. Payments
            val payCol = userRef.collection("payments")
            val cloudPay = fetchCloudCollection(payCol) { it.toObject(PaymentDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localPay = database.paymentDao().getAllPaymentsRaw()
            for (pay in localPay) {
                if (pay.syncOrigin == SyncOrigin.LEGACY || pay.syncId.isBlank()) {
                    val wSync = workerIdToSyncId[pay.workerId] ?: ""
                    val matchingCandidates = if (wSync.isNotBlank()) {
                        cloudPay.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.workerSyncId == wSync && it.date == pay.date.toString() && Math.abs(it.amount - pay.amount) < 0.01
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        pay.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous payment match for worker=$wSync, date=${pay.date}, amount=${pay.amount}.")
                        }
                        pay.copy(
                            syncId = if (pay.syncId.isNotBlank()) pay.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (pay.updatedAt <= 0) (if (pay.createdAt > 0) pay.createdAt else System.currentTimeMillis()) else pay.updatedAt
                        )
                    }
                    database.paymentDao().updatePayment(updated)
                    totalBootstrapped++
                }
            }

            // 8. Client Payments
            val cPayCol = userRef.collection("clientPayments")
            val cloudCPay = fetchCloudCollection(cPayCol) { it.toObject(ClientPaymentDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localCPay = database.clientPaymentDao().getAllClientPaymentsRaw()
            for (cPay in localCPay) {
                if (cPay.syncOrigin == SyncOrigin.LEGACY || cPay.syncId.isBlank()) {
                    val cSync = contractIdToSyncId[cPay.contractId] ?: ""
                    val matchingCandidates = if (cSync.isNotBlank()) {
                        cloudCPay.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.contractSyncId == cSync && it.paymentDate == cPay.paymentDate.toString() && Math.abs(it.amount - cPay.amount) < 0.01
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        cPay.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous client payment match for contract=$cSync, date=${cPay.paymentDate}, amount=${cPay.amount}.")
                        }
                        cPay.copy(
                            syncId = if (cPay.syncId.isNotBlank()) cPay.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (cPay.updatedAt <= 0) (if (cPay.createdAt > 0) cPay.createdAt else System.currentTimeMillis()) else cPay.updatedAt
                        )
                    }
                    database.clientPaymentDao().updatePayment(updated)
                    totalBootstrapped++
                }
            }

            // 9. Worker Site History
            val histCol = userRef.collection("workerSiteHistory")
            val cloudHist = fetchCloudCollection(histCol) { it.toObject(WorkerSiteHistoryDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localHist = database.workerSiteHistoryDao().getAllHistory()
            for (hist in localHist) {
                if (hist.syncOrigin == SyncOrigin.LEGACY || hist.syncId.isBlank()) {
                    val wSync = workerIdToSyncId[hist.workerId] ?: ""
                    val sSync = siteIdToSyncId[hist.siteId] ?: ""
                    val matchingCandidates = if (wSync.isNotBlank() && sSync.isNotBlank()) {
                        cloudHist.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.workerSyncId == wSync && it.siteSyncId == sSync && it.startDate == hist.startDate.toString()
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        hist.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous worker site history match for worker=$wSync, site=$sSync, startDate=${hist.startDate}.")
                        }
                        hist.copy(
                            syncId = if (hist.syncId.isNotBlank()) hist.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (hist.updatedAt <= 0) (if (hist.createdAt > 0) hist.createdAt else System.currentTimeMillis()) else hist.updatedAt
                        )
                    }
                    database.workerSiteHistoryDao().update(updated)
                    totalBootstrapped++
                }
            }

            // 10. Site Attendance
            val sAttCol = userRef.collection("siteAttendance")
            val cloudSAtt = fetchCloudCollection(sAttCol) { it.toObject(SiteAttendanceDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localSAtt = database.siteAttendanceDao().getAllSiteAttendance()
            for (sAtt in localSAtt) {
                if (sAtt.syncOrigin == SyncOrigin.LEGACY || sAtt.syncId.isBlank()) {
                    val sSync = siteIdToSyncId[sAtt.siteId] ?: ""
                    val matchingCandidates = if (sSync.isNotBlank()) {
                        cloudSAtt.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.siteSyncId == sSync && it.date == sAtt.date.toString()
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        sAtt.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous site attendance match for site=$sSync, date=${sAtt.date}.")
                        }
                        sAtt.copy(
                            syncId = if (sAtt.syncId.isNotBlank()) sAtt.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (sAtt.updatedAt <= 0) System.currentTimeMillis() else sAtt.updatedAt
                        )
                    }
                    database.siteAttendanceDao().insertSiteAttendance(updated)
                    totalBootstrapped++
                }
            }

            // 11. Site Expenses
            val expCol = userRef.collection("siteExpenses")
            val cloudExp = fetchCloudCollection(expCol) { it.toObject(SiteExpenseDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localExp = database.siteExpenseDao().getAllExpensesRaw()
            for (exp in localExp) {
                if (exp.syncOrigin == SyncOrigin.LEGACY || exp.syncId.isBlank()) {
                    val sSync = siteIdToSyncId[exp.siteId] ?: ""
                    val matchingCandidates = if (sSync.isNotBlank()) {
                        cloudExp.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.siteSyncId == sSync && it.expenseDate == exp.expenseDate.toString() && Math.abs(it.amount - exp.amount) < 0.01
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        exp.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous site expense match for site=$sSync, date=${exp.expenseDate}, amount=${exp.amount}.")
                        }
                        exp.copy(
                            syncId = if (exp.syncId.isNotBlank()) exp.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (exp.updatedAt <= 0) (if (exp.createdAt > 0) exp.createdAt else System.currentTimeMillis()) else exp.updatedAt
                        )
                    }
                    database.siteExpenseDao().updateExpense(updated)
                    totalBootstrapped++
                }
            }

            val expenseIdToSyncId = database.siteExpenseDao().getAllExpensesRaw().associate { it.id to it.syncId }

            // 12. Site Expense Payments
            val expPayCol = userRef.collection("siteExpensePayments")
            val cloudExpPay = fetchCloudCollection(expPayCol) { it.toObject(SiteExpensePaymentDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localExpPay = database.siteExpensePaymentDao().getAllPaymentsRaw()
            for (expPay in localExpPay) {
                if (expPay.syncOrigin == SyncOrigin.LEGACY || expPay.syncId.isBlank()) {
                    val eSync = expenseIdToSyncId[expPay.expenseId] ?: ""
                    val matchingCandidates = if (eSync.isNotBlank()) {
                        cloudExpPay.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.expenseSyncId == eSync && it.paymentDate == expPay.paymentDate.toString() && Math.abs(it.amount - expPay.amount) < 0.01
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        expPay.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous site expense payment match for expense=$eSync, date=${expPay.paymentDate}, amount=${expPay.amount}.")
                        }
                        expPay.copy(
                            syncId = if (expPay.syncId.isNotBlank()) expPay.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (expPay.updatedAt <= 0) (if (expPay.createdAt > 0) expPay.createdAt else System.currentTimeMillis()) else expPay.updatedAt
                        )
                    }
                    database.siteExpensePaymentDao().updatePayment(updated)
                    totalBootstrapped++
                }
            }

            val matIdToSyncId = database.materialDao().getAllMaterialsRaw().associate { it.id to it.syncId }

            // 13. Material Transactions
            val matTxCol = userRef.collection("materialTransactions")
            val cloudMatTx = fetchCloudCollection(matTxCol) { it.toObject(MaterialTransactionDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localMatTx = database.materialTransactionDao().getAllTransactionsRaw()
            for (tx in localMatTx) {
                if (tx.syncOrigin == SyncOrigin.LEGACY || tx.syncId.isBlank()) {
                    val mSync = matIdToSyncId[tx.materialId] ?: ""
                    val sSync = siteIdToSyncId[tx.siteId] ?: ""
                    val matchingCandidates = if (mSync.isNotBlank() && sSync.isNotBlank()) {
                        cloudMatTx.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.materialSyncId == mSync && it.siteSyncId == sSync && it.transactionDate == tx.transactionDate.toString() && Math.abs(it.quantity - tx.quantity) < 0.001
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        tx.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous material transaction match for material=$mSync, site=$sSync, date=${tx.transactionDate}, qty=${tx.quantity}.")
                        }
                        tx.copy(
                            syncId = if (tx.syncId.isNotBlank()) tx.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (tx.updatedAt <= 0) (if (tx.createdAt > 0) tx.createdAt else System.currentTimeMillis()) else tx.updatedAt
                        )
                    }
                    database.materialTransactionDao().updateTransaction(updated)
                    totalBootstrapped++
                }
            }

            val cMatIdToSyncId = database.contractorMaterialDao().getAllMaterialsRaw().associate { it.id to it.syncId }

            // 14. Contractor Material Transactions
            val cMatTxCol = userRef.collection("contractorMaterialTransactions")
            val cloudCMatTx = fetchCloudCollection(cMatTxCol) { it.toObject(ContractorMaterialTransactionDTO::class.java)?.apply { if (syncId.isBlank()) syncId = it.id } }
            val localCMatTx = database.contractorMaterialTransactionDao().getAllTransactionsRaw()
            for (tx in localCMatTx) {
                if (tx.syncOrigin == SyncOrigin.LEGACY || tx.syncId.isBlank()) {
                    val mSync = cMatIdToSyncId[tx.materialId] ?: ""
                    val matchingCandidates = if (mSync.isNotBlank()) {
                        cloudCMatTx.filter {
                            it.syncId !in matchedCloudSyncIds &&
                            it.materialSyncId == mSync && it.transactionDate == tx.transactionDate.toString() && Math.abs(it.quantity - tx.quantity) < 0.001
                        }
                    } else emptyList()

                    val updated = if (matchingCandidates.size == 1) {
                        val cloudMatch = matchingCandidates.first()
                        matchedCloudSyncIds.add(cloudMatch.syncId)
                        tx.copy(
                            syncId = cloudMatch.syncId,
                            syncOrigin = SyncOrigin.CLOUD_SYNCED,
                            isPendingSync = false
                        )
                    } else {
                        if (matchingCandidates.size > 1) {
                            Log.w(TAG, "Ambiguous contractor material transaction match for material=$mSync, date=${tx.transactionDate}, qty=${tx.quantity}.")
                        }
                        tx.copy(
                            syncId = if (tx.syncId.isNotBlank()) tx.syncId else UUID.randomUUID().toString(),
                            syncOrigin = SyncOrigin.NEW_LOCAL,
                            isPendingSync = true,
                            updatedAt = if (tx.updatedAt <= 0) (if (tx.createdAt > 0) tx.createdAt else System.currentTimeMillis()) else tx.updatedAt
                        )
                    }
                    database.contractorMaterialTransactionDao().updateTransaction(updated)
                    totalBootstrapped++
                }
            }

            Log.d(TAG, "Legacy Sync Bootstrap finished. Total records upgraded for sync: $totalBootstrapped")
            com.example.workermanager.data.remote.sync.SyncCursorManager.setBootstrappedLegacy(uid, true)
            Result.success(totalBootstrapped)
        } catch (e: Exception) {
            Log.e(TAG, "Error in LegacySyncBootstrapper, aborting bootstrap without mutating unprocessed records", e)
            Result.failure(e)
        }
    }

    internal suspend fun <T> fetchCloudCollection(
        collection: CollectionReference,
        pageSize: Long = 100L,
        transform: (DocumentSnapshot) -> T?
    ): List<T> {
        val results = mutableListOf<T>()
        var lastSnapshot: DocumentSnapshot? = null
        while (true) {
            var query: Query = collection.limit(pageSize)
            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }
            val pageSnapshot = query.get().await()
            if (pageSnapshot.isEmpty) {
                break
            }
            for (doc in pageSnapshot.documents) {
                val item = transform(doc)
                if (item != null) {
                    results.add(item)
                }
            }
            if (pageSnapshot.size() < pageSize) {
                break
            }
            lastSnapshot = pageSnapshot.documents.last()
        }
        return results
    }
}
