package com.example.workermanager.data.remote.sync.engine

import android.util.Log
import com.example.workermanager.UserSession
import com.example.workermanager.data.remote.FirestoreProvider
import com.example.workermanager.data.remote.sync.SyncStatusManager
import com.example.workermanager.data.remote.sync.SyncStatusState
import com.example.workermanager.data.remote.sync.dto.*
import com.example.workermanager.data.remote.sync.mapper.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreRealtimeSyncManager {
    private const val TAG = "RealtimeSyncManager"
    private val activeRegistrations = mutableListOf<ListenerRegistration>()

    fun startListening(userSession: UserSession) {
        stopListening()

        val uid = userSession.userId
        if (uid.isBlank()) return

        Log.d(TAG, "Starting real-time Firestore listeners for user: $uid")
        SyncStatusManager.updateStatus(SyncStatusState.Syncing)

        val userRef = FirestoreProvider.getUserDocumentRef()
        val db = userSession.database
        val scope = userSession.sessionScope

        // 1. Sites Listener
        val sitesReg = userRef.collection("sites").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in sites snapshot listener", error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(SiteDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.siteDao().getSiteBySyncId(dto.syncId)
                        if (existing == null) {
                            if (!dto.isDeleted) db.siteDao().insertSite(SiteMapper.toEntity(dto))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.siteDao().updateSite(SiteMapper.toEntity(dto, localId = existing.id))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(sitesReg)

        // 2. Materials Listener
        val materialsReg = userRef.collection("materials").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(MaterialDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.materialDao().getMaterialBySyncId(dto.syncId)
                        if (existing == null) {
                            if (!dto.isDeleted) db.materialDao().insertMaterial(MaterialMapper.toEntity(dto))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.materialDao().updateMaterial(MaterialMapper.toEntity(dto, localId = existing.id))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(materialsReg)

        // 3. Contractor Materials Listener
        val cMatReg = userRef.collection("contractorMaterials").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(ContractorMaterialDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.contractorMaterialDao().getMaterialBySyncId(dto.syncId)
                        if (existing == null) {
                            if (!dto.isDeleted) db.contractorMaterialDao().insertMaterial(ContractorMaterialMapper.toEntity(dto))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.contractorMaterialDao().updateMaterial(ContractorMaterialMapper.toEntity(dto, localId = existing.id))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(cMatReg)

        // 4. Workers Listener
        val workersReg = userRef.collection("workers").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(WorkerDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.workerDao().getWorkerBySyncId(dto.syncId)
                        val localSiteId = if (!dto.siteSyncId.isNullOrBlank()) db.siteDao().getSiteBySyncId(dto.siteSyncId!!)?.id else null
                        if (existing == null) {
                            if (!dto.isDeleted) db.workerDao().insertWorker(WorkerMapper.toEntity(dto, localSiteId = localSiteId))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.workerDao().updateWorker(WorkerMapper.toEntity(dto, localId = existing.id, localSiteId = localSiteId))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(workersReg)

        // 5. Contracts Listener
        val contractsReg = userRef.collection("contracts").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(ContractDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.contractDao().getContractBySyncId(dto.syncId)
                        val localSiteId = if (!dto.siteSyncId.isNullOrBlank()) db.siteDao().getSiteBySyncId(dto.siteSyncId!!)?.id else null
                        if (existing == null) {
                            if (!dto.isDeleted) db.contractDao().insertContract(ContractMapper.toEntity(dto, localSiteId = localSiteId))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.contractDao().updateContract(ContractMapper.toEntity(dto, localId = existing.id, localSiteId = localSiteId))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(contractsReg)

        // 6. Attendance Listener
        val attReg = userRef.collection("attendance").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(AttendanceDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.attendanceDao().getAttendanceBySyncId(dto.syncId)
                        val localWorkerId = if (dto.workerSyncId.isNotBlank()) db.workerDao().getWorkerBySyncId(dto.workerSyncId)?.id else null
                        val localSiteId = if (dto.siteSyncId.isNotBlank()) db.siteDao().getSiteBySyncId(dto.siteSyncId)?.id else null

                        if (localWorkerId == null || localSiteId == null) continue

                        if (existing == null) {
                            db.attendanceDao().insertAttendance(AttendanceMapper.toEntity(dto, localWorkerId = localWorkerId, localSiteId = localSiteId))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.attendanceDao().updateAttendance(AttendanceMapper.toEntity(dto, localId = existing.id, localWorkerId = localWorkerId, localSiteId = localSiteId))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(attReg)

        // 7. Payments Listener
        val payReg = userRef.collection("payments").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(PaymentDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.paymentDao().getPaymentBySyncId(dto.syncId)
                        val localWorkerId = if (dto.workerSyncId.isNotBlank()) db.workerDao().getWorkerBySyncId(dto.workerSyncId)?.id else null
                        val localSiteId = if (dto.siteSyncId.isNotBlank()) db.siteDao().getSiteBySyncId(dto.siteSyncId)?.id else null

                        if (localWorkerId == null || localSiteId == null) continue

                        if (existing == null) {
                            if (!dto.isDeleted) db.paymentDao().insertPayment(PaymentMapper.toEntity(dto, localWorkerId = localWorkerId, localSiteId = localSiteId))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.paymentDao().updatePayment(PaymentMapper.toEntity(dto, localId = existing.id, localWorkerId = localWorkerId, localSiteId = localSiteId))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(payReg)

        // 8. Site Expenses Listener
        val expensesReg = userRef.collection("siteExpenses").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val dto = doc.toObject(SiteExpenseDTO::class.java)?.apply { if (syncId.isBlank()) syncId = doc.id } ?: continue
                        val existing = db.siteExpenseDao().getExpenseBySyncId(dto.syncId)
                        val localSiteId = if (dto.siteSyncId.isNotBlank()) db.siteDao().getSiteBySyncId(dto.siteSyncId)?.id else null

                        if (localSiteId == null) continue

                        if (existing == null) {
                            if (!dto.isDeleted) db.siteExpenseDao().insertExpense(SiteExpenseMapper.toEntity(dto, localSiteId = localSiteId))
                        } else if (dto.updatedAt > existing.updatedAt) {
                            db.siteExpenseDao().updateExpense(SiteExpenseMapper.toEntity(dto, localId = existing.id, localSiteId = localSiteId))
                        }
                    }
                    SyncStatusManager.updateStatus(SyncStatusState.Synced)
                }
            }
        }
        activeRegistrations.add(expensesReg)

        Log.d(TAG, "Real-time snapshot listeners attached successfully.")
    }

    fun stopListening() {
        if (activeRegistrations.isNotEmpty()) {
            Log.d(TAG, "Detaching ${activeRegistrations.size} active Firestore listeners...")
            for (reg in activeRegistrations) {
                try {
                    reg.remove()
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing listener", e)
                }
            }
            activeRegistrations.clear()
        }
    }
}
