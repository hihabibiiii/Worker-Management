package com.example.workermanager.data.repository

import com.example.workermanager.data.billing.CreationQuotaPolicy
import com.example.workermanager.data.billing.ProAccessLevel
import com.example.workermanager.data.dao.WorkerDao
import com.example.workermanager.data.dao.WorkerSiteHistoryDao
import com.example.workermanager.data.entity.SyncOrigin
import com.example.workermanager.data.entity.WorkerEntity
import com.example.workermanager.data.entity.WorkerSiteHistoryEntity
import com.example.workermanager.data.remote.sync.MonotonicClock
import com.example.workermanager.data.remote.sync.RecordRevisionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class WorkerRepository(
    private val workerDao: WorkerDao,
    private val historyDao: WorkerSiteHistoryDao,
    private val siteDao: com.example.workermanager.data.dao.SiteDao? = null,
    private val onDataChanged: (() -> Unit)? = null,
    private val proAccessLevelProvider: (() -> ProAccessLevel)? = null
) {
    val allWorkers: Flow<List<WorkerEntity>> = workerDao.getAllWorkers()
    val activeWorkers: Flow<List<WorkerEntity>> = workerDao.getActiveWorkers()

    fun getWorkerById(id: Long): Flow<WorkerEntity?> = workerDao.getWorkerById(id)
    fun getWorkersBySite(siteId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersBySite(siteId)
    fun searchWorkers(query: String): Flow<List<WorkerEntity>> = workerDao.searchWorkers(query)

    suspend fun canCreateWorker(): Boolean {
        val level = proAccessLevelProvider?.invoke() ?: ProAccessLevel.FREE
        val activeCount = workerDao.getAllWorkersRaw().count { !it.isDeleted }
        return CreationQuotaPolicy.canCreateWorker(activeCount, level)
    }

    suspend fun insertWorker(worker: WorkerEntity, siteId: Long?): Long {
        val level = proAccessLevelProvider?.invoke() ?: ProAccessLevel.FREE
        val activeCount = workerDao.getAllWorkersRaw().count { !it.isDeleted }
        CreationQuotaPolicy.validateWorkerCreation(activeCount, level)
        val workerId = workerDao.insertWorker(worker)
        if (siteId != null) {
            val history = WorkerSiteHistoryEntity(
                workerId = workerId,
                siteId = siteId,
                startDate = LocalDate.now()
            )
            historyDao.insert(history)
            workerDao.updateWorker(worker.copy(id = workerId, currentSiteId = siteId))
        }
        onDataChanged?.invoke()
        return workerId
    }

    suspend fun updateWorker(worker: WorkerEntity) {
        val updated = worker.copy(
            updatedAt = MonotonicClock.currentTimeMillis(),
            isPendingSync = true,
            syncOrigin = if (worker.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else worker.syncOrigin,
            syncId = if (worker.syncId.isBlank()) java.util.UUID.randomUUID().toString() else worker.syncId
        )
        RecordRevisionManager.recordLocalEdit(updated.syncId)
        workerDao.updateWorker(updated)
        onDataChanged?.invoke()
    }

    suspend fun deleteWorker(worker: WorkerEntity) {
        if (worker.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(worker.syncId)
        }
        workerDao.softDeleteWorker(worker.id)
        onDataChanged?.invoke()
    }

    suspend fun restoreWorker(id: Long) {
        val worker = workerDao.getAllWorkersRaw().find { it.id == id }
        if (worker != null && worker.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(worker.syncId)
        }
        workerDao.restoreWorker(id)
        onDataChanged?.invoke()
    }

    fun getDeletedWorkers(): Flow<List<WorkerEntity>> = workerDao.getDeletedWorkers()

    suspend fun permanentlyDeleteWorker(id: Long) {
        val worker = workerDao.getAllWorkersRaw().find { it.id == id }
        if (worker != null && worker.syncId.isNotBlank()) {
            RecordRevisionManager.recordLocalEdit(worker.syncId)
            workerDao.markPendingPermanentDelete(id)
        } else {
            workerDao.permanentlyDeleteWorker(id)
        }
        onDataChanged?.invoke()
    }

    suspend fun emptyWorkerTrash() {
        val deleted = workerDao.getAllWorkersRaw().filter { it.isDeleted }
        for (worker in deleted) {
            if (worker.syncId.isNotBlank()) {
                workerDao.markPendingPermanentDelete(worker.id)
            } else {
                workerDao.permanentlyDeleteWorker(worker.id)
            }
        }
        onDataChanged?.invoke()
    }

    suspend fun transferWorker(workerId: Long, newSiteId: Long, transferDate: LocalDate) {
        // 1. Close current history
        historyDao.closeCurrentHistory(workerId, transferDate.minusDays(1))
        
        // 2. Create new history
        val newHistory = WorkerSiteHistoryEntity(
            workerId = workerId,
            siteId = newSiteId,
            startDate = transferDate
        )
        historyDao.insert(newHistory)
        
        // 3. Update worker currentSiteId
        val worker = workerDao.getWorkerById(workerId).first()
        worker?.let {
            val updated = it.copy(
                currentSiteId = newSiteId,
                updatedAt = System.currentTimeMillis(),
                isPendingSync = true,
                syncOrigin = if (it.syncOrigin == SyncOrigin.LEGACY) SyncOrigin.NEW_LOCAL else it.syncOrigin,
                syncId = if (it.syncId.isBlank()) java.util.UUID.randomUUID().toString() else it.syncId
            )
            workerDao.updateWorker(updated)
        }
        onDataChanged?.invoke()
    }

    fun getWorkerSiteHistory(workerId: Long): Flow<List<WorkerSiteHistoryEntity>> = 
        historyDao.getHistoryForWorker(workerId)

    fun getWorkersForSiteOnDate(siteId: Long, date: LocalDate): Flow<List<WorkerSiteHistoryEntity>> =
        historyDao.getWorkersForSiteOnDate(siteId, date)

    suspend fun getSiteIdForWorkerOnDate(workerId: Long, date: LocalDate): Long? =
        historyDao.getSiteIdForWorkerOnDate(workerId, date)
}
